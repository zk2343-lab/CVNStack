<!-- AI-assisted. -->

# CVNStack

Waitlist Management Service for COMS W4156. Client applications create a waitlist, add prioritized entries, and move those entries from waiting to called, served, or cancelled. The API contract follows the team proposal (*Waitlist Management Service — CVNStack Proposal*) with the MVP deviations listed below.

Team: Zengxiaoran Kang (zk2343-lab), Ding Wang (wdingding0924-ux), Yoongu Kang (ygkang-cu).

Tasks live on the project board: <https://github.com/users/zk2343-lab/projects/1/views/1>

No third-party source is copied into this repository. Libraries come from Maven Central.

## Prerequisites

Install JDK 21 and Maven 3.9 or newer. `java -version` must report 21. If `mvn -version` shows a different Java, set `JAVA_HOME` to the JDK 21 home and run Maven again.

Tests also require a running Docker-compatible runtime. Testcontainers starts an isolated
`redis:7.4-alpine` container and removes it after testing. No separately installed Redis server
is needed for tests. Tests are not skipped when Docker is unavailable.

With rootless Docker on Linux, set these in the shell that runs Maven:

```bash
export DOCKER_HOST="unix:///run/user/$(id -u)/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/run/user/$(id -u)/docker.sock"
```

```bash
git clone https://github.com/zk2343-lab/CVNStack.git
cd CVNStack
```

## Build, test, style, coverage

| Goal | Command | Tool / path |
| --- | --- | --- |
| Compile and package | `mvn -DskipTests package` | Maven |
| Unit and API tests | `mvn test` | JUnit 5, Mockito, REST Assured |
| Style check | `mvn checkstyle:check` | Checkstyle, `config/checkstyle/checkstyle.xml` (Google Java Style) |
| Tests + style + coverage | `mvn verify` | also JaCoCo under `target/site/jacoco/` |
| Clean Checkstyle report | committed under `reports/checkstyle/` | regenerated with `mvn checkstyle:check` |

```bash
mvn test
mvn checkstyle:check
mvn verify
```

Runnable jar after package: `target/cvnstack-0.1.0-SNAPSHOT.jar`.

## Start

```bash
mvn spring-boot:run
```

Or after a package:

```bash
java -jar target/cvnstack-0.1.0-SNAPSHOT.jar
```

The process listens on port 8080. `GET /health` returns 200 and `{"status":"UP"}`. Stop with Ctrl-C.

Waitlist routes land in issues #4–#9. The issue #6 entry lookup handler is implemented.
Actual entry requests require a `WaitlistStorage` implementation (issue #18).
`/health` still works without it; tests provide a mock repository.

## Layout

Single Maven module. Flat packages under `com.cvnstack`:

```text
pom.xml
README.md
.gitignore
config/checkstyle/checkstyle.xml
reports/checkstyle/
src/
  main/
    java/com/cvnstack/
      WaitlistApplication.java
      controller/  # HTTP API request handlers (ResponseEntity)
      service/     # app services, WaitlistStorage / CacheStorage
      model/       # entities + modeling rules
        response/  # HealthResponse, ErrorResponse, later API bodies
      error/       # ErrorCode, ApiException, ControllerAdvice
    resources/
      application.properties
  test/
    java/com/cvnstack/
      controller/
      service/
      model/
      error/
```

There is no production in-memory store. Mocks and fakes stay in unit tests.
`RedisCacheStorageTest` uses an actual Redis container. PostgreSQL tests arrive with issue #18.

### Quality and data tools (names and config)

| Tool | Role | Where configured |
| --- | --- | --- |
| Checkstyle | Google Java Style (2-space indent, 100-char lines) | `config/checkstyle/checkstyle.xml`, root `pom.xml` plugin |
| JUnit 5 | Unit tests | root `pom.xml` dependency |
| Mockito | Mocks in unit tests | root `pom.xml` dependency |
| REST Assured | HTTP API tests | root `pom.xml` dependency |
| JaCoCo | Coverage on `mvn verify` | root `pom.xml` plugin |
| Flyway | Schema migrations | arrives with issue #18 (PostgreSQL) |
| Testcontainers | Isolated Redis in tests; PostgreSQL later | root `pom.xml`, `RedisCacheStorageTest` |

## Local stores and GCP

PostgreSQL is the source of truth. Redis holds the waiting-order sorted set, the serve-next lock, and a read cache of entry payloads.

### Redis storage (issue #19)

`service.RedisCacheStorage` implements the existing `CacheStorage` interface. It does not
implement HTTP routes or read from PostgreSQL. Database/cache coordination remains issue #20.

- `replaceWaitingOrder` receives ids already sorted by `WaitingOrder`: higher priority first,
  then earlier insertion. The Sorted Set score is the zero-based index in that list.
  Redis reads ascending scores; public positions still start at 1.
- The ready key stores the expected waiting count. This distinguishes an uncached queue from a
  cached empty queue and detects a missing rank key.
- Entry Hashes store `id`, `waitlistId`, `name`, `priority`, `status`, and `insertedAt`.
  Position and wait estimates are not cached inside the entry payload.
- `deleteEntry` removes only the entry payload. The caller separately replaces or invalidates
  waiting order when a mutation changes it.
- `invalidateWaitlist` removes that waitlist's order and all its cached entries, not its lock
  or another waitlist's data. A per-waitlist Set tracks entry cache keys; no global key scan is used.
- Keys start with `cvnstack:waitlist:<encoded-id>:`. Each id component uses URL-safe Base64
  without padding, so ids containing separators do not collide.

Configure the application through Spring properties or their environment-variable equivalents:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Serve locks have a unique owner token and expire after 30 seconds. Release checks the
token atomically before deleting the lock. Acquire and release must use the same adapter instance
and thread, normally in a `try` / `finally` block. Locks are not reentrant or automatically renewed.
Contention returns `false`; Redis failures propagate to the caller rather than returning stale data.
A lock can expire during a long operation, so issue #20 must still use a conditional database
update to prevent claiming an entry twice.

Lua scripts keep related Redis changes atomic. References:
[Spring Data Redis scripting](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html),
[Redis lock ownership](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/),
[Sorted Set scores](https://redis.io/docs/latest/commands/zadd/).

### Consistency rules

1. A mutation commits to PostgreSQL first, then updates Redis.
2. If the Redis update fails, delete that waitlist's rank key and the entry cache key so the next read rebuilds from the database.
3. Redis must not be the only copy of waitlist or entry data.

### docker-compose (issue #21)

When `docker-compose.yml` is added, start local PostgreSQL and Redis with:

```bash
docker compose up -d
```

Point the app at those containers through configuration (do not hardcode hosts in code), for example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cvnstack
spring.datasource.username=cvnstack
spring.datasource.password=cvnstack
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Cloud SQL and Memorystore (issue #21)

Use the same property keys. Replace host, port, database name, and credentials with the Cloud SQL instance and Memorystore endpoint for the deployed environment (private IP or Cloud SQL Auth Proxy as chosen in that issue). No code change should be required beyond configuration.

## Domain rules (MVP)

- Priority is an integer **1–5**. **5 is most urgent.** Default is **1**.
- Among `WAITING` entries, higher priority comes first, then earlier insertion time.
- Position starts at **1**. Estimate is `entriesAhead * processingTimePerEntryMinutes`.
- Position and estimate are **null** unless the entry is `WAITING`.
- Status: `WAITING` → `CALLED` → `SERVED`. `WAITING` or `CALLED` → `CANCELLED`. `SERVED` and `CANCELLED` are terminal.
- Priority changes only while `WAITING` or `CALLED`.
- Waitlist names are unique.

## HTTP status and error body

Error body shape: `{ "error": "...", "message": "..." }` (`model.response.ErrorResponse`). Controllers return Spring `ResponseEntity`.

| Code | `error` value | When |
| --- | --- | --- |
| 200 | | Success |
| 204 | | Serve-next found nobody waiting |
| 400 | `INVALID_REQUEST` or `INVALID_STATE_TRANSITION` | Bad field, empty update, or illegal status change |
| 404 | `NOT_FOUND` | Missing waitlist or entry |
| 409 | `CONFLICT` | Waitlist name already exists |
| 422 | `CAPACITY_EXCEEDED` | Add would exceed `maxLength` |

## API endpoints

These six routes match the proposal. Implementation issues are noted.
Routes that are not implemented return Spring's default 404, not the waitlist `NOT_FOUND` body.

### `POST /waitlists` (issue #4)

- **Input:** JSON with waitlist `name`, `maxLength`, and `processingTimePerEntryMinutes`.
- **Success:** 200 with the assigned waitlist id (and settings as implemented).
- **Side effects:** Inserts the waitlist in PostgreSQL; may seed Redis structures.
- **Errors:** 400 invalid fields; 409 name already exists.

### `POST /waitlists/{id}/entries` (issue #5)

- **Input:** path `id`; JSON with entry `name` and optional `priority` (1–5, default 1).
- **Success:** 200 with entry id, status `WAITING`, position, and `estimatedWaitTimeMinutes`.
- **Side effects:** Inserts the entry; updates waiting order in Redis after PostgreSQL commit.
- **Errors:** 404 unknown waitlist; 400 invalid fields; 422 capacity exceeded.
- **Idempotency:** entry creates are **not** idempotent (see deviations).

### `GET /waitlists/{id}/entries/{entryId}` (issue #6)

- **Input:** path `id`, `entryId`. No body.
- **Success:** 200 with `waitlistId`, `entryId`, `status`, `priority`, `position`, `estimatedWaitTimeMinutes` (`position` / estimate null when not `WAITING`).
- **Side effects:** none. This handler does not write to the repository or Redis.
- **Errors:** 404 waitlist or entry missing, or entry on another waitlist.
- **Implementation:** uses `EntryRulesService` and `WaitEstimate`. Redis/database coordination
  remains issue #20. Repository-backed use requires issue #18; the current tests use mocks.
  Repository injection is [lazy](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/context/annotation/Lazy.html)
  so `/health` can still start before the storage implementation lands.

### `POST /waitlists/{id}/serve-next` (issue #7)

- **Input:** path `id`. No body.
- **Success:** 200 with selected entry as `CALLED`; **204** when nobody is waiting.
- **Side effects:** `WAITING` → `CALLED` under a Redis serve lock; waiting order updated after PostgreSQL commit.
- **Errors:** 404 unknown waitlist.

### `PATCH /waitlists/{id}/entries/{entryId}` (issue #8)

- **Input:** path ids; JSON with optional `status` and/or `priority`. Empty body is 400 `INVALID_REQUEST`.
- **Success:** 200 with updated entry, position/estimate when still waiting.
- **Side effects:** status/priority persisted; rank rebuilt when waiting order changes.
- **Errors:** 404 missing waitlist/entry; 400 `INVALID_STATE_TRANSITION` or `INVALID_REQUEST`.

### `DELETE /waitlists/{id}/entries/{entryId}` (issue #9)

- **Input:** path ids.
- **Success:** 200 confirming removal.
- **Side effects:** entry removed; waiting positions for remaining entries shift.
- **Errors:** 404 if waitlist/entry missing or already removed.

### Liveness

- `GET /health` → 200 `{"status":"UP"}`.

## Call order

### Correct walkthrough

1. `POST /waitlists` — create a waitlist.
2. `POST /waitlists/{id}/entries` — add one or more entries (higher priority optional).
3. `GET /waitlists/{id}/entries/{entryId}` — read position and estimate.
4. `POST /waitlists/{id}/serve-next` — call the next waiting entry.
5. `PATCH /waitlists/{id}/entries/{entryId}` — mark `SERVED` (or `CANCELLED`).
6. Optionally `DELETE` an entry that should leave the list without being served.

### Bad sequences (expected status)

1. `POST /waitlists/{id}/entries` before the waitlist exists → **404** `NOT_FOUND`.
2. `PATCH` `SERVED` → `WAITING` → **400** `INVALID_STATE_TRANSITION`.
3. `POST /waitlists` twice with the same name → second call **409** `CONFLICT`.
4. Add entries until `maxLength`, then add another → **422** `CAPACITY_EXCEEDED`.

## Deviations from the written proposal

| Proposal idea | MVP behavior |
| --- | --- |
| Boolean `prioritized` on add | Integer priority **1–5** (default 1) |
| Rolling / completion-history wait estimates | Fixed `entriesAhead * processingTimePerEntryMinutes` only |
| Duplicate entry create as conflict | Entry creates are **not** idempotent |
| Event broker + webhook worker | **No** event broker in this iteration |
| Empty PATCH as `MALFORMED_REQUEST` | **400** `INVALID_REQUEST` |

## AI-assisted work

Coding agents follow [AGENTS.md](AGENTS.md) and `.agents/skills/`. Usage is recorded in [docs/ai-usage.md](docs/ai-usage.md). A file a model wrote or debugged is marked `AI-assisted` in its header.
