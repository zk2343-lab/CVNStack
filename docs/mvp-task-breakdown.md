# Iteration 1 MVP — task breakdown

Team: **CVNStack**

| Person | UNI | GitHub |
| --- | --- | --- |
| Zengxiaoran Kang | zk2343 | [zk2343-lab](https://github.com/zk2343-lab) |
| Ding Wang | dw3258 | [wdingding0924-ux](https://github.com/wdingding0924-ux) |
| Yoongu Kang | yk3193 | [ygkang-cu](https://github.com/ygkang-cu) |

Board: <https://github.com/users/zk2343-lab/projects/1/views/1>

Requirements: [iteration-1-guidelines.md](iteration-1-guidelines.md)

Each row below is one pull request. The author opens the pull request; one of the other two reviews it. Do not push feature work straight to `main` once branch protection is on.

## Decisions for this iteration

These are the contracts for the first demo. Departures from the proposal are called out here and must be repeated in the README. The event broker and webhook worker are the only architecture boxes deferred past this iteration.

1. **Runtime.** Java 21, Spring Boot 3, Maven. One deployable service (`waitlist-app`) plus shared libraries. The event broker and webhook worker stay out of this iteration.
2. **Modules.** Shared code is its own Maven modules, not a package inside the app. `waitlist-domain` and `waitlist-api` have no JDBC and no Redis. `waitlist-persistence` and `waitlist-cache` depend on the domain. Only `waitlist-app` depends on all four.
3. **Database.** PostgreSQL is the source of truth for waitlists and entries. Schema changes go through Flyway.
4. **Cache.** Redis holds derived state only: the waiting-order sorted set, the serve-next lock, and a read cache of entry payloads. It must be rebuildable from PostgreSQL.
5. **Consistency.** A mutation commits to PostgreSQL first, then updates Redis. If the Redis update fails, delete that waitlist's rank key and the entry cache key so the next read rebuilds from the database. Never answer a rank or a cached entry that the database has already changed. `serve-next` claims an entry with a lock, a conditional `WAITING → CALLED` update, and then the Redis removal.
6. **GCP.** Cloud SQL for PostgreSQL, Memorystore for Redis, and Cloud Run for the app, created as one stack. Local `docker-compose.yml` runs the same two stores so development does not require GCP. The app reads host, port, and credentials from configuration, not from hardcoded addresses.
7. **Priority.** Integer from 1 to 5. **5 is the most urgent.** Default is 1. The proposal's create call used a boolean `prioritized`, and its update call used an integer tier. One integer scale is what ranking needs. Clients that only have a boolean can send `5` for priority and `1` otherwise.
8. **Order.** Among `WAITING` entries, higher priority comes first. Equal priority is ordered by earlier insertion time. Position starts at 1.
9. **Wait estimate.** `entriesAhead * processingTimePerEntryMinutes`. Use the waitlist's configured processing time. Do not implement rolling completion history yet. When an entry is not `WAITING`, position and estimate are null.
10. **Status.** `WAITING` → `CALLED` → `SERVED`, and `WAITING` or `CALLED` → `CANCELLED`. `SERVED` and `CANCELLED` are terminal. Priority may change only while the entry is `WAITING` or `CALLED`. `POST /serve-next` is the normal way to move the head of the line to `CALLED`. `PATCH` may also set `CALLED`.
11. **Identity and duplicates.** Waitlist names are unique. A second create with the same name returns 409. Entry names are not unique: two adds with the same name create two entries. The proposal asked for create calls to reject an identical retry. We only do that for waitlist names in this iteration, because entries have no idempotency key.
12. **Empty serve.** `POST /serve-next` on a waitlist with nobody `WAITING` returns **204** and changes nothing.
13. **Removal.** `DELETE` drops the entry. A second delete returns 404. Positions of the remaining waiting entries move up.

### Status codes

| Code | When |
| --- | --- |
| 200 | Create, add, get, serve (when someone was called), update, or delete succeeded |
| 204 | Serve-next found nobody waiting |
| 400 | Invalid field, empty update, or illegal status transition |
| 404 | Waitlist or entry is missing, or the entry is not on that waitlist |
| 409 | Waitlist name already exists |
| 422 | Adding an entry would exceed the waitlist's maximum length |

Error body:

```json
{ "error": "NOT_FOUND", "message": "Waitlist or entry does not exist" }
```

Use `NOT_FOUND`, `INVALID_REQUEST`, `INVALID_STATE_TRANSITION`, `CONFLICT`, and `CAPACITY_EXCEEDED` as the `error` values.

### Call order

A correct session looks like this:

1. `POST /waitlists`
2. `POST /waitlists/{id}/entries` one or more times
3. `GET /waitlists/{id}/entries/{entryId}` any time after the entry exists
4. `POST /waitlists/{id}/serve-next` when a server is free
5. `PATCH` that entry to `SERVED` or `CANCELLED`
6. `DELETE` is allowed whenever the entry still exists

Calls that must fail are part of the API tests: get or serve before create, add when the list is full, patch `SERVED` back to `WAITING`, delete twice, create a waitlist whose name is taken.

## Layout to create first

Agree this tree in the scaffold pull request so the other two branches are not guessing package names.

```
pom.xml                      # parent reactor
config/checkstyle/checkstyle.xml
docker-compose.yml           # Postgres + Redis for local runs
infra/gcp/                   # Cloud SQL, Memorystore, Cloud Run
reports/checkstyle/
reports/jacoco/
modules/waitlist-domain/     # entities, ranking, estimates, state machine, repository ports
modules/waitlist-api/        # request and response DTOs, error codes
modules/waitlist-persistence/# Flyway and PostgreSQL adapters
modules/waitlist-cache/      # Redis ranks, locks, entry cache
modules/waitlist-app/        # Spring Boot controllers and use cases
```

Allowed dependencies point inward only: domain and api do not depend on persistence, cache, or the app. Unit tests of use cases mock the repository and cache ports. Testcontainers cover the real stores.

Header comment on every non-trivial type, including tests: purpose, and `AI-assisted` if a model generated or debugged the file.

## Who does what

| Owner | Focus |
| --- | --- |
| Zengxiaoran Kang | Application skeleton, shared-module reactor, create-waitlist, serve-next, GCP stack, Checkstyle, README coordination, AI log, release tag and branch protection |
| Ding Wang | Shared domain module, PostgreSQL module, DB/cache consistency, add-entry, update-entry, and the unit tests for that logic |
| Yoongu Kang | Shared API module and error contract, Redis module, get-entry, delete-entry, the REST Assured suite, and the coverage report |

The shared-module reactor lands right after the scaffold. Domain and API modules start from that. Endpoint work starts when both modules are on `main`. PostgreSQL and Redis start once the domain ports exist, and the consistency pull request is what switches the running service off any temporary fake.

## Issues

Titles match the GitHub issues on the project board.

### 1. [Scaffold the Spring Boot project](https://github.com/zk2343-lab/CVNStack/issues/1)

**Owner:** Zengxiaoran Kang  
**Blocks:** everything else

- Maven project, Java 21, Spring Boot 3. A single bootable module is enough here; the split into shared modules is issue 17.
- `GET /health` returns 200 so we can show the process is up before the waitlist API exists.
- JUnit 5, Mockito, REST Assured, JaCoCo, and Checkstyle plugins declared in the parent `pom.xml`. A single Checkstyle run may still fail until later code exists; the plugin has to be wired.
- `.gitignore` for `target/`, IDE files, and `.env`.
- README stub that says how to build and run, and points at this breakdown. Full API documentation is a later issue.
- Do not add a production in-memory store. Fakes belong in unit tests only.

### 2. [Shared API module and error responses](https://github.com/zk2343-lab/CVNStack/issues/2)

**Owner:** Yoongu Kang  
**Depends on:** 1, 17

- Error codes and the `{ error, message }` DTO live in `modules/waitlist-api`, so a later client can depend on the same contract.
- `@ControllerAdvice` in `waitlist-app` maps validation failures, missing resources, conflicts, capacity, and illegal transitions onto the status table above.
- Unit test the handler with `MockMvc` or a direct call. No endpoint logic in this pull request.

### 3. [Shared domain module](https://github.com/zk2343-lab/CVNStack/issues/3)

**Owner:** Ding Wang  
**Depends on:** 1, 17

- `Waitlist`, `Entry`, `EntryStatus` in `modules/waitlist-domain`. No Spring, JDBC, or Redis in this module.
- Ranking: priority descending, then insertion time ascending, only among `WAITING` entries.
- Wait estimate from configured processing time. No history window.
- State-machine method that accepts or rejects a transition. Do not bury the rules in a controller.
- Repository and cache ports (interfaces) only. PostgreSQL and Redis implementations are issues 18 and 19. Waitlist name uniqueness is part of the repository port.
- Unit tests for ranking, estimate, and transitions. Mock the clock if tests would otherwise depend on `Instant.now()`.

### 4. [Create a waitlist — `POST /waitlists`](https://github.com/zk2343-lab/CVNStack/issues/4)

**Owner:** Zengxiaoran Kang  
**Depends on:** 2, 3

Body: `name`, `maxLength` (positive integer), `processingTimePerEntryMinutes` (positive integer).  
Success: 200 and the new id, plus the settings that were stored.  
Failures: 400 for invalid input, 409 when the name exists.  
Side effect on success: one new waitlist and no entries. Failure writes nothing.  
Unit-test the service with a mock repository.

### 5. [Add an entry — `POST /waitlists/{id}/entries`](https://github.com/zk2343-lab/CVNStack/issues/5)

**Owner:** Ding Wang  
**Depends on:** 2, 3

Body: `name` (non-blank), `priority` (1–5, default 1).  
Success: 200 with entry id, status `WAITING`, position, and `estimatedWaitTimeMinutes`.  
Failures: 404 unknown waitlist, 400 invalid body, 422 at capacity. Count only entries that still occupy a slot (`WAITING` and `CALLED`) toward `maxLength`.  
Side effect: one new waiting entry. Later positions follow the ranking rules.  
Unit-test the service with a mock repository, including the capacity failure.

### 6. [Read an entry — `GET /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/6)

**Owner:** Yoongu Kang  
**Depends on:** 2, 3

No body. Success: 200 with `waitlistId`, `entryId`, `status`, `priority`, `position`, `estimatedWaitTimeMinutes`.  
Position and estimate are null unless status is `WAITING`.  
404 if the waitlist or entry is missing, or the entry belongs to another waitlist.  
No writes. Two identical gets both succeed and can disagree if a mutation landed between them.  
Unit-test position math through the service, with a mock repository.

### 7. [Serve the next entry — `POST /waitlists/{id}/serve-next`](https://github.com/zk2343-lab/CVNStack/issues/7)

**Owner:** Zengxiaoran Kang  
**Depends on:** 4, 5

No body. Selects the first `WAITING` entry, sets `CALLED`, stores the call time, and returns 200 with id, status, and priority.  
204 when nobody is waiting. 404 when the waitlist does not exist.  
The selection and the status change are one step so two overlapping calls cannot claim the same entry. The lock and the conditional database update that make that true across processes are issue 20. This pull request defines the use case and tests it with a mock port.  
Side effect: that entry leaves the waiting order, so later estimates shrink. An empty or missing waitlist writes nothing.  
Unit-test tie-breaking and the empty-queue case with a mock repository.

### 8. [Update an entry — `PATCH /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/8)

**Owner:** Ding Wang  
**Depends on:** 5

Body may include `status`, `priority`, or both. At least one is required.  
Success: 200 with the entry, position and estimate if it is still `WAITING`, and `updatedAt`.  
400 for an empty body, a priority outside 1–5, or a rejected transition (for example `SERVED` → `WAITING`).  
404 for a missing waitlist or entry.  
Setting the same status or priority again returns 200 and does not invent a new transition. A second patch into an already terminal state returns 400.  
Priority changes reorder waiting entries. Moving to `SERVED` or `CANCELLED` clears position and estimate. Do not update rolling averages in this iteration.  
Unit-test the allowed and rejected transitions.

### 9. [Remove an entry — `DELETE /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/9)

**Owner:** Yoongu Kang  
**Depends on:** 5

Success: 200 with `{ "message": "Entry successfully removed", "entryId", "waitlistId" }`.  
404 if either id is unknown or the entry was already removed.  
Side effect: active depth drops, and waiting entries behind it move up. Failure writes nothing.  
Unit-test the service with a mock repository.

### 10. [API test suite](https://github.com/zk2343-lab/CVNStack/issues/10)

**Owner:** Yoongu Kang  
**Depends on:** 4–9 and 20 (the collection can be sketched earlier against this document)

REST Assured tests under the app module, run by `mvn test`, against PostgreSQL and Redis via Testcontainers. Do not point this suite at an in-memory fake.

Cover at least:

- Happy path in call order: create waitlist, add a normal entry and a priority-5 entry, get both, serve-next (the priority-5 entry comes out first), patch it to `SERVED`, delete the other.
- Same-priority entries come out in insertion order.
- 409 duplicate waitlist name, 422 full list, 400 bad priority and illegal transition, 404 missing ids, 204 serve on an empty list, 404 on a second delete.

Fix the product bugs these tests find. Leave a note in the pull request for any failure that is still open.

### 11. [Checkstyle clean report](https://github.com/zk2343-lab/CVNStack/issues/11)

**Owner:** Zengxiaoran Kang  
**Depends on:** the code that will be tagged

- Ruleset file at `config/checkstyle/checkstyle.xml`, based on Google Java Style, named as such in the README.
- `mvn checkstyle:check` passes on main application and test sources.
- Commit the text or XML report under `reports/checkstyle/`. The committed report must match the tagged commit.

### 12. [Branch coverage report](https://github.com/zk2343-lab/CVNStack/issues/12)

**Owner:** Yoongu Kang, with gaps fixed by whoever owns the uncovered code  
**Depends on:** 10

- JaCoCo configured in `pom.xml` for branch coverage.
- Commit the report under `reports/jacoco/`.
- Branch coverage at least 55% on the tagged tree. If it is short, add tests on the thin spots (state machine and error paths first) rather than excluding packages to move the number.

### 13. [README and API documentation](https://github.com/zk2343-lab/CVNStack/issues/13)

**Owner:** Zengxiaoran Kang coordinates. Each person documents the endpoints they implemented.  
**Depends on:** 4–9, 20, 21

The README must include:

- Build, run, and test commands, including the style check and the coverage report.
- Tool names: Checkstyle (Google Java Style), JUnit 5, Mockito, REST Assured, JaCoCo, and the paths of their config.
- The six endpoints: inputs, outputs, side effects, status codes, and the call-order rules in this file.
- One correct walkthrough a client could follow (hospital, warehouse, or call center — pick one and stick to it).
- At least three bad sequences and the status code each one gets.
- How to run against `docker-compose.yml`, and how to point the same configuration at Cloud SQL and Memorystore.
- The consistency rules in this file: database first, cache rebuilt on failure, what Redis is allowed to store.
- A statement that no third-party source is vendored; libraries come from Maven.
- Link to the project board.
- The deviations listed at the top of this file.

### 14. [AI usage log and in-code marks](https://github.com/zk2343-lab/CVNStack/issues/14)

**Owner:** Zengxiaoran Kang  
**Ongoing; finish before the tag**

- Add `docs/ai-usage.md`: tool, source (URL or product), purpose, representative prompts, and **token total per tool**.
- Header-comment every file a model generated or debugged, tests included.
- This planning pair (`iteration-1-guidelines.md`, `mvp-task-breakdown.md`) was drafted with Cursor using Grok 4.7 on 2026-10-16. Record that session's token total once Cursor's usage view shows it.

### 15. [Branch protection and the iteration-1 tag](https://github.com/zk2343-lab/CVNStack/issues/15)

**Owner:** Zengxiaoran Kang  
**Depends on:** 11, 12, 13, 14, 21

- Protect `main`: pull request required, at least one review, no force-push.
- Confirm the style report is clean and the coverage report is at least 55% on that commit.
- Tag `iteration-1` (annotated) on that commit. A GitHub Release with the same name is optional.
- Do not move the tag after the deadline.

### 17. [Establish shared Maven modules](https://github.com/zk2343-lab/CVNStack/issues/17)

**Owner:** Zengxiaoran Kang  
**Depends on:** 1

Create the reactor and move the boot application into `modules/waitlist-app`. Add the other four modules with the dependency arrows in the layout section. `mvn test` from the repository root builds every module. This pull request can leave domain types and SQL empty; issues 2, 3, 18, and 19 fill them. No production in-memory repository.

### 18. [PostgreSQL schema and repository](https://github.com/zk2343-lab/CVNStack/issues/18)

**Owner:** Ding Wang  
**Depends on:** 3, 17

- Flyway migrations for waitlist and entry tables, including status, priority, insertion time, call time, and update time.
- `waitlist-persistence` implements the repository ports from the domain module.
- Waitlist name uniqueness is enforced in the database.
- Integration test with Testcontainers: save, read, and the 409 path. No Redis in this pull request.

### 19. [Redis cache for ranks, locks, and reads](https://github.com/zk2343-lab/CVNStack/issues/19)

**Owner:** Yoongu Kang  
**Depends on:** 3, 17

- `waitlist-cache` implements the cache port.
- Waiting order is a sorted set per waitlist. Score puts a higher priority first and, within a priority, an earlier insertion time first. Document the score formula next to the code.
- A lock key for serve-next, and a per-entry read cache.
- Integration test with Testcontainers against Redis only. Rebuilding from PostgreSQL is issue 20.

### 20. [Keep PostgreSQL and Redis consistent](https://github.com/zk2343-lab/CVNStack/issues/20)

**Owner:** Ding Wang  
**Depends on:** 18, 19

Implement the consistency rules in decision 5, and switch `waitlist-app` onto these adapters. After this pull request the running service has no in-memory store.

- Add, patch, delete, and serve-next commit the database row, then update Redis.
- A missing rank key or entry key is rebuilt from PostgreSQL before the response is computed.
- Two overlapping serve-next calls on one waitlist return two different entries, or one entry and a 204.
- Tests use both Testcontainers. Yoongu reviews, since the Redis operations are his module.

### 21. [Integrate the service with GCP](https://github.com/zk2343-lab/CVNStack/issues/21)

**Owner:** Zengxiaoran Kang  
**Depends on:** 17, 18, 19

Create Cloud SQL (PostgreSQL), Memorystore (Redis), and Cloud Run in one stack under `infra/gcp/`, plus `docker-compose.yml` for the same two stores locally. Database password comes from Secret Manager in GCP and from the compose env file locally. The app configuration names are the same in both places.

Document project, region, and the commands to deploy in `docs/gcp.md`. Do not add the event broker here.

The issue is done when either Cloud Run is serving `/health` against those two managed stores, or the GCP project cannot be billed yet and the pull request says so. In that case compose must still boot the app against PostgreSQL and Redis, and the stack definition stays in the repo for the team to apply later.

## Suggested order

```
1 Scaffold (Zengxiaoran)
        |
        v
17 Shared modules (Zengxiaoran)
        |-----------------------------|
        v                             v
2 API module (Yoongu)          3 Domain module (Ding)
        |                             |
        +--------------+--------------+
                       v
     4 Create    5 Add entry    6 Get     and, once 3 is in:
                       |                 18 Postgres (Ding)
     7 Serve-next  8 Patch  9 Delete     19 Redis (Yoongu)
                       |                      |
                       |                      v
                       |              20 Consistency (Ding)
                       |                      |
                       +----------+-----------+
                                  v
                    21 GCP (Zengxiaoran)   10 API suite (Yoongu)
                                  |
                    11 Checkstyle  12 Coverage  13 README  14 AI log
                                  v
                        15 Protect main + tag
```

Issues 18 and 19 can proceed in parallel with the endpoint pull requests. Issue 10 waits until the endpoints and the consistent store are both on `main`. Issue 15 waits until 11–14 and 21 are on `main`.

---

*Drafted with Cursor (Grok 4.7) on 2026-10-16 from the CVNStack proposal and the iteration-1 brief. Team members own the issues after they are filed.*
