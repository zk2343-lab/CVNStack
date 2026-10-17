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

These are intentional departures from the proposal so the first demo does not depend on infrastructure we do not need yet. Say so in the README.

1. **Runtime.** Java 21, Spring Boot 3, Maven. One process. No PostgreSQL, Redis, or event broker in this iteration. Those stay in the proposal as the later deployment shape.
2. **Storage.** An in-memory repository behind an interface. The interface is the only thing controllers and services depend on, so a database can replace the map later without rewriting the API.
3. **Priority.** Integer from 1 to 5. **5 is the most urgent.** Default is 1. The proposal's create call used a boolean `prioritized`, and its update call used an integer tier. One integer scale is what ranking needs. Clients that only have a boolean can send `5` for priority and `1` otherwise.
4. **Order.** Among `WAITING` entries, higher priority comes first. Equal priority is ordered by earlier insertion time. Position starts at 1.
5. **Wait estimate.** `entriesAhead * processingTimePerEntryMinutes`. Use the waitlist's configured processing time. Do not implement rolling completion history yet. When an entry is not `WAITING`, position and estimate are null.
6. **Status.** `WAITING` → `CALLED` → `SERVED`, and `WAITING` or `CALLED` → `CANCELLED`. `SERVED` and `CANCELLED` are terminal. Priority may change only while the entry is `WAITING` or `CALLED`. `POST /serve-next` is the normal way to move the head of the line to `CALLED`. `PATCH` may also set `CALLED`.
7. **Identity and duplicates.** Waitlist names are unique. A second create with the same name returns 409. Entry names are not unique: two adds with the same name create two entries. The proposal asked for create calls to reject an identical retry. We only do that for waitlist names in this iteration, because entries have no idempotency key.
8. **Empty serve.** `POST /serve-next` on a waitlist with nobody `WAITING` returns **204** and changes nothing.
9. **Removal.** `DELETE` drops the entry. A second delete returns 404. Positions of the remaining waiting entries move up.

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
pom.xml
config/checkstyle/checkstyle.xml
reports/checkstyle/          # committed clean report, produced before the tag
reports/jacoco/              # committed branch-coverage report
src/main/java/edu/columbia/cvnstack/waitlist/
  WaitlistApplication.java
  api/                       # controllers, exception handler
  api/dto/                   # request and response records
  domain/                    # Waitlist, Entry, EntryStatus
  service/                   # use cases, ranking, wait estimate, state machine
  repository/                # interfaces plus the in-memory implementation
src/test/java/edu/columbia/cvnstack/waitlist/
  domain/
  service/                   # Mockito doubles for the repositories
  api/                       # REST Assured
```

Header comment on every non-trivial type, including tests: purpose, and `AI-assisted` if a model generated or debugged the file.

## Who does what

| Owner | Focus |
| --- | --- |
| Zengxiaoran Kang | Application skeleton, create-waitlist, serve-next, Checkstyle, README coordination, AI log, release tag and branch protection |
| Ding Wang | Domain model, ranking, wait estimate, state machine, in-memory store, add-entry, update-entry, and the unit tests for that logic |
| Yoongu Kang | Error contract, get-entry, delete-entry, the REST Assured suite, and the coverage report |

Ding's domain pull request and Yoongu's error-handler pull request both start as soon as the scaffold is merged. Endpoint work starts when both of those are on `main`.

## Issues

Titles match the GitHub issues on the project board.

### 1. [Scaffold the Spring Boot project](https://github.com/zk2343-lab/CVNStack/issues/1)

**Owner:** Zengxiaoran Kang  
**Blocks:** everything else

- Maven project, Java 21, Spring Boot 3, package tree above.
- `GET /health` returns 200 so we can show the process is up before the waitlist API exists.
- JUnit 5, Mockito, REST Assured, JaCoCo, and Checkstyle plugins declared in `pom.xml`. A single Checkstyle run may still fail until later code exists; the plugin has to be wired.
- `.gitignore` for `target/`, IDE files, and `.env`.
- README stub that says how to build and run, and points at this breakdown. Full API documentation is a later issue.

### 2. [Shared error responses](https://github.com/zk2343-lab/CVNStack/issues/2)

**Owner:** Yoongu Kang  
**Depends on:** 1

- `@ControllerAdvice` that maps validation failures, missing resources, conflicts, capacity, and illegal transitions onto the status table above.
- DTO for `{ error, message }`.
- Unit test the handler with `MockMvc` or a direct call. No endpoint logic in this pull request.

### 3. [Domain model and in-memory store](https://github.com/zk2343-lab/CVNStack/issues/3)

**Owner:** Ding Wang  
**Depends on:** 1

- `Waitlist`, `Entry`, `EntryStatus`.
- Ranking: priority descending, then insertion time ascending, only among `WAITING` entries.
- Wait estimate from configured processing time. No history window.
- State-machine method that accepts or rejects a transition. Do not bury the rules in a controller.
- Repository interfaces and a thread-safe in-memory implementation. Waitlist name uniqueness lives here or in the service, consistently.
- Unit tests for ranking, estimate, transitions, and the repository. Mock the clock if tests would otherwise depend on `Instant.now()`.

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
The selection and the status change are one synchronized step so two overlapping calls cannot claim the same entry.  
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
**Depends on:** 4–9 (the collection can be sketched earlier against this document)

REST Assured tests under `src/test/java/.../api`, run by `mvn test`.

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
**Depends on:** 4–9

The README must include:

- Build, run, and test commands, including the style check and the coverage report.
- Tool names: Checkstyle (Google Java Style), JUnit 5, Mockito, REST Assured, JaCoCo, and the paths of their config.
- The six endpoints: inputs, outputs, side effects, status codes, and the call-order rules in this file.
- One correct walkthrough a client could follow (hospital, warehouse, or call center — pick one and stick to it).
- At least three bad sequences and the status code each one gets.
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
**Last**

- Protect `main`: pull request required, at least one review, no force-push.
- Confirm the style report is clean and the coverage report is at least 55% on that commit.
- Tag `iteration-1` (annotated) on that commit. A GitHub Release with the same name is optional.
- Do not move the tag after the deadline.

## Suggested order

```
1 Scaffold (Zengxiaoran)
        |--------------------------|
        v                          v
2 Errors (Yoongu)          3 Domain + store (Ding)
        |                          |
        +------------+-------------+
                     v
        4 Create (Zengxiaoran)   5 Add entry (Ding)   6 Get entry (Yoongu)
                     |                |                      |
                     v                v                      v
              7 Serve-next     8 Patch entry          9 Delete entry
                     \               |                      /
                      \--------------+---------------------/
                                     v
                              10 API suite (Yoongu)
                                     v
                    11 Checkstyle    12 Coverage    13 README    14 AI log
                                     \              |            /
                                      \-------------+-----------/
                                                    v
                                          15 Protect main + tag
```

Issues 11–14 can proceed as soon as the endpoints they describe exist. Issue 15 waits until 11–14 are on `main`.

---

*Drafted with Cursor (Grok 4.7) on 2026-10-16 from the CVNStack proposal and the iteration-1 brief. Team members own the issues after they are filed.*
