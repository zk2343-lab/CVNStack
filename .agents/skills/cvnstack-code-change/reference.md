<!-- AI-assisted. -->

# CVNStack change reference

Source of truth for a task is the GitHub issue. This file holds the shared contracts: packages, status codes, and stores.

## Layout

Single Maven module. Flat packages under `com.cvnstack`:

| Package | Holds |
| --- | --- |
| `(root)` | `WaitlistApplication` |
| `controller` | HTTP API handlers returning `ResponseEntity` |
| `service` | Application services and storage ports |
| `model` | Entities and modeling rules (order, estimate, transitions) |
| `model.response` | Response DTOs (`HealthResponse`, `ErrorResponse`, …) |
| `error` | `ErrorCode`, `ApiException`, `@ControllerAdvice` |

Configuration lives in `src/main/resources/` (no `config` Java package).
Services reference model classes and `ErrorCode` through `ApiException`.

## Stores

PostgreSQL is the source of truth. Schema changes go through Flyway. Redis holds the waiting-order sorted set, the serve-next lock, and a read cache of entry payloads. A mutation commits to PostgreSQL first, then updates Redis. If the Redis update fails, delete that waitlist's rank key and the entry cache key so the next read rebuilds from the database.

Local stores come from `docker-compose.yml` (issue 21). The app reads host, port, and credentials from configuration. Do not hardcode them.

Storage ports: `WaitlistStorage` and `CacheStorage` in `service` (impls in #18 / #19).

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

- Issue 1: bootable app, `GET /health` returns 200, Checkstyle file, `.gitignore`, README build/run. No database, no Redis, no in-memory repository.
- Issue 17: model types, storage ports, error types, pure ordering, estimate, and transitions.
- Write path (Ding Wang): issues 3, 18, 4, 5, 8, 9.
- Read path (Yoongu Kang): issues 19, 6, 7, 10.
- Do not implement another owner's endpoints in your pull request.
