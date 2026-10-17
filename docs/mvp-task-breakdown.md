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
| Zengxiaoran Kang | Platform path: scaffold, shared contracts, error handler, GCP and Compose, app wiring, Checkstyle, coverage report, README, AI log, release tag |
| Ding Wang | Write path: domain rules, PostgreSQL, create-waitlist, add-entry, update-entry, delete-entry, and the tests for that path |
| Yoongu Kang | Read path: Redis, get-entry, serve-next, and the API tests for that path |

After issue 17, Ding and Yoongu do not wait on each other. Each path owns its module and the endpoints on that module. The only shared gate is issue 17 (entities, port interfaces, and the pure ordering, estimate, and transition functions). Issue 20 is the one place the two stores are wired together, and issue 21 can start from the scaffold.

## Issues

Titles match the GitHub issues on the project board.

### 1. [Scaffold the Spring Boot project](https://github.com/zk2343-lab/CVNStack/issues/1)

**Owner:** Zengxiaoran Kang  
**Blocks:** issue 17 only. GCP (issue 21) can start from this scaffold.

- Maven project, Java 21, Spring Boot 3. A single bootable module is enough here; the split into shared modules is issue 17.
- `GET /health` returns 200 so we can show the process is up before the waitlist API exists.
- JUnit 5, Mockito, REST Assured, JaCoCo, and Checkstyle plugins declared in the parent `pom.xml`. A single Checkstyle run may still fail until later code exists; the plugin has to be wired.
- `.gitignore` for `target/`, IDE files, and `.env`.
- README stub that says how to build and run, and points at this breakdown. Full API documentation is a later issue.
- Do not add a production in-memory store. Fakes belong in unit tests only.

### 2. [Shared API module and error responses](https://github.com/zk2343-lab/CVNStack/issues/2)

**Owner:** Zengxiaoran Kang  
**Depends on:** 17

This is part of the platform path. It does not block Ding's or Yoongu's endpoints. The error DTO itself is created in issue 17 so controllers can compile first.

- Error codes and the `{ error, message }` DTO live in `modules/waitlist-api`, so a later client can depend on the same contract.
- `@ControllerAdvice` in `waitlist-app` maps validation failures, missing resources, conflicts, capacity, and illegal transitions onto the status table above.
- Unit test the handler with `MockMvc` or a direct call. No endpoint logic in this pull request.

### 3. [Shared domain module](https://github.com/zk2343-lab/CVNStack/issues/3)

**Owner:** Ding Wang  
**Depends on:** 17

Write path. Yoongu's read path does not wait on this issue. Entities and port interfaces arrive in issue 17; this pull request adds the write-side rules those ports rely on.

- Use the ordering, estimate, and transition functions from issue 17. Do not add a second copy.
- Capacity: only `WAITING` and `CALLED` entries count toward `maxLength`.
- Waitlist name uniqueness is enforced through the repository port.
- No Spring, JDBC, or Redis in this module. Issues 18 and 19 implement the ports.
- Unit-test the capacity rule and a rejected call that goes through the transition function.

### 4. [Create a waitlist — `POST /waitlists`](https://github.com/zk2343-lab/CVNStack/issues/4)

**Owner:** Ding Wang  
**Depends on:** 3, 18

Write path, same owner as the domain rules and PostgreSQL. Include this endpoint's REST Assured cases in this pull request.

Body: `name`, `maxLength` (positive integer), `processingTimePerEntryMinutes` (positive integer).  
Success: 200 and the new id, plus the settings that were stored.  
Failures: 400 for invalid input, 409 when the name exists.  
Side effect on success: one new waitlist and no entries. Failure writes nothing.  
Unit-test the service with a mock repository.

### 5. [Add an entry — `POST /waitlists/{id}/entries`](https://github.com/zk2343-lab/CVNStack/issues/5)

**Owner:** Ding Wang  
**Depends on:** 3, 18

Write path. Include this endpoint's REST Assured cases in this pull request.

Body: `name` (non-blank), `priority` (1–5, default 1).  
Success: 200 with entry id, status `WAITING`, position, and `estimatedWaitTimeMinutes`.  
Failures: 404 unknown waitlist, 400 invalid body, 422 at capacity. Count only entries that still occupy a slot (`WAITING` and `CALLED`) toward `maxLength`.  
Side effect: one new waiting entry. Later positions follow the ranking rules.  
Unit-test the service with a mock repository, including the capacity failure.

### 6. [Read an entry — `GET /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/6)

**Owner:** Yoongu Kang  
**Depends on:** 19

Read path, same owner as Redis. Does not wait on the write-path pull requests. Include this endpoint's REST Assured cases here; issue 10 collects the read-path suite.

No body. Success: 200 with `waitlistId`, `entryId`, `status`, `priority`, `position`, `estimatedWaitTimeMinutes`.  
Position and estimate are null unless status is `WAITING`.  
404 if the waitlist or entry is missing, or the entry belongs to another waitlist.  
No writes. Two identical gets both succeed and can disagree if a mutation landed between them.  
Unit-test position math through the service, with a mock repository.

### 7. [Serve the next entry — `POST /waitlists/{id}/serve-next`](https://github.com/zk2343-lab/CVNStack/issues/7)

**Owner:** Yoongu Kang  
**Depends on:** 19

Read path, with the Redis lock. Does not wait on create-waitlist or add-entry. Unit-test the claim through the cache port. Issue 20 is the later proof against PostgreSQL and Redis together.

No body. Selects the first `WAITING` entry, sets `CALLED`, stores the call time, and returns 200 with id, status, and priority.  
204 when nobody is waiting. 404 when the waitlist does not exist.  
The selection and the status change are one step so two overlapping calls cannot claim the same entry.  
Side effect: that entry leaves the waiting order, so later estimates shrink. An empty or missing waitlist writes nothing.  
Unit-test tie-breaking and the empty-queue case with a mock repository.

### 8. [Update an entry — `PATCH /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/8)

**Owner:** Ding Wang  
**Depends on:** 5

Write path. Include this endpoint's REST Assured cases in this pull request.

Body may include `status`, `priority`, or both. At least one is required.  
Success: 200 with the entry, position and estimate if it is still `WAITING`, and `updatedAt`.  
400 for an empty body, a priority outside 1–5, or a rejected transition (for example `SERVED` → `WAITING`).  
404 for a missing waitlist or entry.  
Setting the same status or priority again returns 200 and does not invent a new transition. A second patch into an already terminal state returns 400.  
Priority changes reorder waiting entries. Moving to `SERVED` or `CANCELLED` clears position and estimate. Do not update rolling averages in this iteration.  
Unit-test the allowed and rejected transitions.

### 9. [Remove an entry — `DELETE /waitlists/{id}/entries/{entryId}`](https://github.com/zk2343-lab/CVNStack/issues/9)

**Owner:** Ding Wang  
**Depends on:** 5

Write path, with add-entry. Call the cache port to drop the member; unit tests use a fake cache. Include this endpoint's REST Assured cases in this pull request.

Success: 200 with `{ "message": "Entry successfully removed", "entryId", "waitlistId" }`.  
404 if either id is unknown or the entry was already removed.  
Side effect: active depth drops, and waiting entries behind it move up. Failure writes nothing.  
Unit-test the service with a mock repository.

### 10. [API test suite](https://github.com/zk2343-lab/CVNStack/issues/10)

**Owner:** Yoongu Kang  
**Depends on:** 6, 7

Read-path suite only: get-entry and serve-next, against Redis via Testcontainers. Ding covers create, add, update, and delete in those pull requests. The cross-store scenario lives in issue 20. Do not point this suite at an in-memory fake.

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

**Owner:** Zengxiaoran Kang. Whoever owns an uncovered branch adds the missing test.  
**Depends on:** 4, 5, 6, 7, 8, 9, 10, 20

- JaCoCo configured in `pom.xml` for branch coverage.
- Commit the report under `reports/jacoco/`.
- Branch coverage at least 55% on the tagged tree. If it is short, add tests on the thin spots (state machine and error paths first) rather than excluding packages to move the number.

### 13. [README and API documentation](https://github.com/zk2343-lab/CVNStack/issues/13)

**Owner:** Zengxiaoran Kang coordinates. Each person documents the endpoints they implemented.  
**Depends on:** nothing at the start. Build, run, test, Compose, and GCP sections can be written with issue 21. Each endpoint owner adds their section in the endpoint pull request.

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
**Depends on:** 11, 12, 13, 14, 20, 21

- Protect `main`: pull request required, at least one review, no force-push.
- Confirm the style report is clean and the coverage report is at least 55% on that commit.
- Tag `iteration-1` (annotated) on that commit. A GitHub Release with the same name is optional.
- Do not move the tag after the deadline.

### 17. [Establish shared Maven modules](https://github.com/zk2343-lab/CVNStack/issues/17)

**Owner:** Zengxiaoran Kang  
**Depends on:** 1

This is the only pull request Ding and Yoongu wait on.

Create the reactor and move the boot application into `modules/waitlist-app`. Add the other four modules with the dependency arrows in the layout section. `mvn test` from the repository root builds every module. No production in-memory repository.

Land these types here, with unit tests, so the write path and the read path can compile without another shared pull request:

- `Waitlist`, `Entry`, `EntryStatus`
- repository port and cache port
- error DTO and error codes
- pure ordering (higher priority, then earlier insertion time), wait estimate, and the allowed status transitions

Issue 3 adds write-side policy on top of those types. Issue 18 and issue 19 implement the ports. They do not wait on issue 3.

### 18. [PostgreSQL schema and repository](https://github.com/zk2343-lab/CVNStack/issues/18)

**Owner:** Ding Wang  
**Depends on:** 17

Write path. Starts as soon as the ports exist. No dependency on Redis or on issue 3.

- Flyway migrations for waitlist and entry tables, including status, priority, insertion time, call time, and update time.
- `waitlist-persistence` implements the repository port.
- Queries that need waiting order use `priority DESC, inserted_at ASC`.
- Waitlist name uniqueness is enforced in the database.
- Integration test with Testcontainers: save, read, and the 409 path. No Redis client in this pull request.

### 19. [Redis cache for ranks, locks, and reads](https://github.com/zk2343-lab/CVNStack/issues/19)

**Owner:** Yoongu Kang  
**Depends on:** 17

Read path. Starts as soon as the ports exist. No dependency on PostgreSQL or on issue 3.

- `waitlist-cache` implements the cache port.
- Waiting order is a sorted set per waitlist. Score puts a higher priority first and, within a priority, an earlier insertion time first. Use the same ordering rules as issue 17, and document the score next to the code.
- A lock key for serve-next, and a per-entry read cache.
- If the rank key is missing, rebuild it from the repository port. Tests pass a fake repository.
- Integration test with Testcontainers against Redis only.

### 20. [Keep PostgreSQL and Redis consistent](https://github.com/zk2343-lab/CVNStack/issues/20)

**Owner:** Zengxiaoran Kang  
**Depends on:** 18, 19

Platform path, and the only pull request that needs both stores. Ding's write services already call the cache port with a fake. Yoongu's cache already rebuilds from a fake repository. This pull request connects the real adapters in `waitlist-app`.

- Spring wires PostgreSQL and Redis. No production in-memory store remains.
- One Testcontainers test runs both stores: add a priority entry and a normal entry, serve-next returns the priority entry, and a second overlapping serve-next does not return the same entry.
- Ding and Yoongu both review.

### 21. [Integrate the service with GCP](https://github.com/zk2343-lab/CVNStack/issues/21)

**Owner:** Zengxiaoran Kang  
**Depends on:** 1

Platform path. Terraform, Compose, and `/health` on Cloud Run do not wait for the repositories. Use the configuration names from this file so issue 18 and issue 19 can drop in later.

Create Cloud SQL (PostgreSQL), Memorystore (Redis), and Cloud Run in one stack under `infra/gcp/`, plus `docker-compose.yml` for the same two stores locally. Database password comes from Secret Manager in GCP and from the compose env file locally. The app configuration names are the same in both places.

Document project, region, and the commands to deploy in `docs/gcp.md`. Do not add the event broker here.

The issue is done when either Cloud Run is serving `/health` against those two managed stores, or the GCP project cannot be billed yet and the pull request says so. In that case compose must still boot the app against PostgreSQL and Redis, and the stack definition stays in the repo for the team to apply later.

## Suggested order

```
1 Scaffold (Zengxiaoran) -----> 21 GCP and Compose (Zengxiaoran)
        |
        v
17 Contracts (Zengxiaoran)
        |-----------------------------|
        v                             v
Ding, write path               Yoongu, read path
  3 domain rules                 19 Redis
  18 PostgreSQL                  6 Get entry
  4 Create waitlist              7 Serve-next
  5 Add entry                    10 Read-path API tests
  8 Patch entry
  9 Delete entry
        |                             |
        +--------------+--------------+
                       v
        2 Error handler (Zengxiaoran, any time after 17)
        20 Wire both stores (Zengxiaoran)
                       v
        11 Checkstyle   12 Coverage   13 README   14 AI log
                       v
              15 Protect main + tag
```

Ding's list and Yoongu's list run side by side after issue 17. Issue 2 and issue 21 are not on those lists. Issue 20 is the one join. Issue 15 waits until 11–14, 20, and 21 are on `main`.

---

*Drafted with Cursor (Grok 4.7) on 2026-10-16 from the CVNStack proposal and the iteration-1 brief. Team members own the issues after they are filed.*
