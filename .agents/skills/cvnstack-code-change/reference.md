<!-- AI-assisted. -->

# CVNStack change reference

Source of truth for a task is the GitHub issue. This file holds the shared contracts: modules, status codes, and stores.

## Modules

| Module | Package | May depend on |
| --- | --- | --- |
| `modules/waitlist-domain` | `edu.columbia.cvnstack.domain` | nothing in this repo |
| `modules/waitlist-api` | `edu.columbia.cvnstack.api` | nothing in this repo |
| `modules/waitlist-persistence` | `edu.columbia.cvnstack.persistence` | domain |
| `modules/waitlist-cache` | `edu.columbia.cvnstack.cache` | domain |
| `modules/waitlist-app` | `edu.columbia.cvnstack` | all four, once they exist |

Issue 1 creates only `waitlist-app` and the parent `pom.xml`. Issue 17 adds the other four. Do not create them early.

`edu.columbia.cvnstack.health` is the liveness probe inside the app. Do not put waitlist types there.

## Stores

PostgreSQL is the source of truth. Schema changes go through Flyway. Redis holds the waiting-order sorted set, the serve-next lock, and a read cache of entry payloads. A mutation commits to PostgreSQL first, then updates Redis. If the Redis update fails, delete that waitlist's rank key and the entry cache key so the next read rebuilds from the database.

Local stores come from `docker-compose.yml` (issue 21). The app reads host, port, and credentials from configuration. Do not hardcode them.

## HTTP

| Code | `error` value | When |
| --- | --- | --- |
| 200 | | The call succeeded |
| 204 | | Serve-next found nobody waiting |
| 400 | `INVALID_REQUEST` or `INVALID_STATE_TRANSITION` | Bad field, empty update, or illegal transition |
| 404 | `NOT_FOUND` | Unknown waitlist or entry |
| 409 | `CONFLICT` | Waitlist name already exists |
| 422 | `CAPACITY_EXCEEDED` | The add would pass `maxLength` |

Error body: `{ "error": "NOT_FOUND", "message": "..." }`.

Priority is an integer 1–5. **5 is the most urgent.** Default is 1. Among `WAITING` entries, higher priority comes first, then earlier insertion time. Position starts at 1. Estimate is `entriesAhead * processingTimePerEntryMinutes`. Position and estimate are null unless the entry is `WAITING`.

`WAITING` → `CALLED` → `SERVED`. `WAITING` or `CALLED` → `CANCELLED`. `SERVED` and `CANCELLED` are terminal.

## Issue fences

- Issue 1: parent POM, one bootable module, `GET /health` returns 200, Checkstyle file, `.gitignore`, README build/run. No database, no Redis, no in-memory repository.
- Issue 17: entities, ports, error DTO, pure ordering, estimate, and transitions.
- Write path (Ding Wang): issues 3, 18, 4, 5, 8, 9.
- Read path (Yoongu Kang): issues 19, 6, 7, 10.
- Do not implement another owner's endpoints in your pull request.
