<!-- AI-assisted. -->

# AI usage

This is the only AI usage record in the repository. When a task is completed, append one entry under the tool that did the work. A file the model generated or debugged is marked `AI-assisted` in its header. Prompts and token totals are not copied into that file.

Token totals come from the product's usage screen. If the screen has not been read, write **not yet read from Cursor usage** and do not estimate. When it is read, replace that phrase with the number and the date.

## Cursor

| | |
| --- | --- |
| Tool | Cursor desktop agent |
| Source | <https://cursor.com> |
| Model | Grok 4.7 (high); Composer default on later turns |
| Token total | **13,991,013** (input 739,466 + output 192,603 + cache read 13,058,944), read from Cursor usage API on 2026-10-16 for the three chats below. Request cost about $9.12 of included Pro usage. |

### Iteration plan

- **Completed.** 2026-10-16
- **Purpose.** Draft the iteration requirements and the task split.
- **Prompt.** The course first-iteration brief and the CVNStack proposal. The original chat text was not stored.
- **Files.** `docs/iteration-1-guidelines.md`, `docs/mvp-task-breakdown.md` (later removed; content moved to GitHub issues and agent skills)
- **Tokens.** About **6,368,067** (input 231,680 + output 92,995 + cache read 6,043,392). Included in the Cursor total above.

### Issue 1 — Scaffold the Spring Boot project

- **Completed.** 2026-10-16
- **Purpose.** Bootable Java 21 / Spring Boot 3 module with `GET /health`, parent test and quality plugins, Checkstyle ruleset, and agent skills that keep later edits on this log.
- **Prompts.**

  > can you help me to work on https://github.com/zk2343-lab/CVNStack/issues/1 While working on it, build ai skill references so team can have consistent guildine while using agent to work on code cahnge - don't add cursoragent as collaborator or leave any stupid chace, and instead follow Iteration 1 Guidelines' AI documentation.

  > also have work in drfat (don't pr or release just yet) I will review the work before commit

  > usage and of ai should be consolidated in one place (i am observing the AI generated referneces in many places across repo) and documented on each tasks upon completion. squash to one commit ahead of mainline with one short messagae

  > does this complhy to instruction? do we need to comment ai usage inline???? or it is fine as long as we have one place to document the suage. Additionally, I hope this experience can be streamlined and applied across all kinds of ai agent coders(cursor, copilot, etc). the idea is to add in skill so those ai code detect and apply same strategy

- **Files.** `pom.xml`, `modules/waitlist-app/`, `config/checkstyle/checkstyle.xml`, `.gitignore`, `README.md`, `AGENTS.md`, `.agents/skills/`, this file.
- **Tokens.** About **6,852,019** (input 320,315 + output 84,984 + cache read 6,446,720). Included in the Cursor total above.

### Remove the planning docs

- **Completed.** 2026-10-16
- **Purpose.** Drop the iteration guidelines and the task breakdown now that the agent skills and the GitHub project issues hold that material, and put build and start commands in the README.
- **Prompt.**

  > lets remove ./doc?

  > The instructions were added to agent skill, the tasks were added to project dashboard as issues.

  > Also add how to build and start service in README.md

- **Files.** Deleted `docs/iteration-1-guidelines.md` and `docs/mvp-task-breakdown.md`. Updated `README.md`, `AGENTS.md`, `.agents/skills/cvnstack-code-change/SKILL.md`, `.agents/skills/cvnstack-code-change/reference.md`, and this file.
- **Tokens.** About **770,927** (input 187,471 + output 14,624 + cache read 568,832), and still rising for later turns. Included in the Cursor total above.

### Clean commit identity on the issue 1 PR

- **Completed.** 2026-10-16
- **Purpose.** Stop agents and personal accounts from appearing on commits or pull requests; require the school GitHub identity; rewrite the issue 1 commit without a Cursor co-author trailer.
- **Prompt.**

  > On Pull request, it shows shalloe99cursoragent shalloe99 and cursoragent committed 2 minutes ago

  > i want only the author itself, no cursor or other ai agent mentioned as particiapnts (update in agent skill). and use school account when working on school project. clean out history for this bad one and resubmit

- **Files.** `AGENTS.md`, `.agents/skills/cvnstack-ai-documentation/SKILL.md`, `.agents/skills/cvnstack-ai-documentation/reference.md`, `.agents/skills/cvnstack-code-change/SKILL.md`, this file.
- **Tokens.** Included in the Cursor total above. Not re-read for this turn alone.

### Issues 11, 17, 2, 13 — modules, errors, Checkstyle, README

- **Completed.** 2026-10-17 (local branch; awaiting sign-off before commit)
- **Purpose.** Shared Maven modules with domain types/ports and pure ranking/estimate/transitions; API error codes, `ErrorResponse`, and `@ControllerAdvice`; clean Checkstyle reports under `reports/checkstyle/`; README/API docs aligned with the CVNStack proposal and MVP deviations.
- **Prompt.**

  > pull in latest change, work in a seperate local feature rbanch https://github.com/zk2343-lab/CVNStack/issues/11 https://github.com/zk2343-lab/CVNStack/issues/17 and https://github.com/zk2343-lab/CVNStack/issues/2
  > https://github.com/zk2343-lab/CVNStack/issues/13
  >
  > Remmeber to reference from our project design file Waitlist-Management-Service-CVNStack-Proposal (1).pdf . don't commit your changes - wait for my final sign off
- **Files.** `pom.xml`, `README.md`, `src/main/java/com/cvnstack/{service,model,error}/`, `src/test/...`, `reports/checkstyle/`, `.agents/skills/cvnstack-code-change/`, `AGENTS.md`, this file.
- **Tokens.** not yet read from the product usage screen

## Codex

| | |
| --- | --- |
| Tool | OpenAI Codex |
| Source | https://openai.com/codex/ |
| Model | `gpt-6.1-sol`, recorded in the local session log |
| Token total | **27,942,956** across recorded #19 and #6 snapshots; #7 and later usage not yet read from the product usage screen |

### Issue 19 - Redis cache for ranks, locks, and reads

- **Completed.** Local implementation and testing. Simplified and documented.
- **Purpose.** Implement the existing `CacheStorage` interface with Redis waiting order, entry caching, and serve-next locks. Add Redis container tests and unit tests. Prepare the local JDK 21 and Docker environment.
- **Prompt.**

  > Review the existing base code against the requirements and TODOs in issue #19. Check the existing CacheStorage interface and whether the implementation follows the intended direction. Discuss the changes with me before committing work, implement the approved work, and verify it with tests. Use standard Java where it keeps the code clear and efficient.

- **Files.** `src/main/java/com/cvnstack/service/RedisCacheStorage.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageTest.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageUnitTest.java`, `pom.xml`, `README.md`, this file.
- **Verification.** `mvn clean verify`: 49 tests passed, none skipped, and zero Checkstyle violations after removing the optional lock-duration setting.
- **Tokens.** Included in the Codex total above. It includes #19 preparation, environment setup, related status checks, implementation, tests, explanations, and revisions.
  | Recorded counter | Tokens |
  | --- | ---: |
  | Input, including cached input | 21,923,834 |
  | Cached input, already included above | 21,125,120 |
  | Output, including reasoning | 91,642 |
  | Total: input + output | **22,015,476** |

### Issue 6 - Read an entry

- **Completed.** Local implementation and testing.
- **Purpose.** Implement the read-only entry lookup using the existing storage port and ranking rules. Add service and HTTP tests with a mock repository. Keep `/health` working before the PostgreSQL implementation lands.
- **Prompt.**

  > Review the existing code and requirements for issue #6. Implement the entry lookup on a separate branch based on #19. Reuse the shared rules, write tests with a mock repository. Merge the latest main, preserve both task records when resolving the document conflict, and verify before committing and pushing.

- **Files.** `src/main/java/com/cvnstack/controller/EntryQueryController.java`, `src/main/java/com/cvnstack/service/EntryQueryService.java`, `src/main/java/com/cvnstack/model/response/EntryResponse.java`, `src/test/java/com/cvnstack/service/EntryQueryServiceTest.java`, `src/test/java/com/cvnstack/controller/EntryQueryControllerTest.java`, `README.md`, this file.
- **Verification.** Service tests first failed against an empty implementation. After implementation, `mvn clean verify` passed all 59 tests. After merging main on 2026-10-23, all 78 tests passed with none skipped and zero Checkstyle violations. HTTP tests use a mock repository, not PostgreSQL.
- **Tokens.** **5,927,480** (input 5,910,734, output 16,746; cached input 5,863,168 is already included). Summed 21 unique local usage records for this chat from 2026-10-19 13:12 through 13:32 UTC. Includes preparation and implementation. Later responses are outside this snapshot. Included in the Codex total above.

### Issue 7 - Serve the next entry

- **Completed.** 2026-10-21. Local implementation and testing. Database and lock integration remain in #18 and #20.
- **Purpose.** Add the serve-next handler and tests, a stored call timestamp, and the conditional-claim storage contract. Preserve call timestamps in Redis.
- **Prompt.**

  > Implement issue #7 on a separate branch, following the proposal and issue requirements. Write tests first. Add the shared fields and function declarations needed for the operation, while leaving the PostgreSQL implementation and cross-store integration to #18 and #20. Verify the changes before committing or pushing.

- **Files.** `src/main/java/com/cvnstack/controller/ServeNextController.java`, `src/main/java/com/cvnstack/service/ServeNextService.java`, `src/main/java/com/cvnstack/model/response/ServeNextResponse.java`, `src/main/java/com/cvnstack/model/Entry.java`, `src/main/java/com/cvnstack/service/WaitlistStorage.java`, `src/main/java/com/cvnstack/service/RedisCacheStorage.java`, `src/test/java/com/cvnstack/controller/ServeNextControllerTest.java`, `src/test/java/com/cvnstack/service/ServeNextServiceTest.java`, `src/test/java/com/cvnstack/model/EntryTest.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageTest.java`, `README.md`, this file.
- **Verification.** `mvn clean verify`: 68 tests passed, none skipped, and zero Checkstyle violations. Service and HTTP tests mock the storage ports; Redis payload tests use a real container. Concurrent requests against both stores remain in #20.
- **Tokens.** not yet read from the product usage screen.

### Issue 18 - PostgreSQL schema and repository

- **Completed.** 2026-10-22. Local implementation and testing.
- **Tool / source.** Codex, https://openai.com/codex/.
- **Purpose.** Implement PostgreSQL persistence through the existing storage port with JdbcTemplate, a Flyway initial schema, database-owned update timestamps, ID-based waitlist upsert, insert-only entries, scoped deletion, conditional calling, and atomic capacity enforcement. Preserve the existing single module and Redis behavior.
- **Prompt.**

  > Implement Issue #18: PostgreSQL schema and repository for the waitlist service. Implement the PostgreSQL persistence layer only using Spring JdbcTemplate, PostgreSQL, Flyway, and real PostgreSQL Testcontainers tests. Keep application services dependent on WaitlistStorage. Create both tables with UUID IDs, the specified constraints and indexes, and add Entry.updatedAt using PostgreSQL CURRENT_TIMESTAMP. Implement ID-based waitlist upsert, insert-only saveEntry, transactional capacity check plus insert counting WAITING and CALLED, conditional callEntryIfWaiting, boolean scoped deleteEntry, and unordered entry listing. Map only recognized database failures, including the named waitlist-name unique constraint to conflict. Configure Spring datasource wiring and add persistence tests. Do not add Redis behavior, JPA, cascade deletes, derived columns, unrelated validation changes, or agent Git metadata. Follow repository header and AI usage requirements.

- **Files.** `pom.xml`, `src/main/java/com/cvnstack/model/Entry.java`, `src/main/java/com/cvnstack/service/WaitlistStorage.java`, `src/main/java/com/cvnstack/service/PostgresWaitlistStorage.java`, `src/main/resources/application.properties`, `src/main/resources/db/migration/V1__create_waitlist_schema.sql`, `src/test/java/com/cvnstack/service/PostgresWaitlistStorageTest.java`, `src/test/java/com/cvnstack/model/EntryTest.java`, `src/test/java/com/cvnstack/controller/HealthControllerTest.java`, `src/test/java/com/cvnstack/error/GlobalExceptionHandlerTest.java`, `src/test/resources/application.properties`, this file.
- **Verification.** `mvn test`: 87 tests passed, none failed or skipped, including eight PostgreSQL integration tests and a competing capacity insert test. Flyway created a clean schema and applied no migrations on a repeat run. `mvn checkstyle:check`: zero violations. `git diff --check`: clean.
- **Tokens (user-requested estimate).** Approximately **86,300** for Issue #18, using the context occupancy as a proxy at the user's request; this is not a measured cumulative token total. Actual usage: not yet read from the product usage screen.
- **Source.** User-provided Codex CLI local check, not a product usage-screen reading: `Context window: 84% left (86.3K used / 475K)`. This estimate is not included in the measured Codex token total above.
- **Review follow-up (2026-10-23).** Extended the authorized scope to preserve `Entry.updatedAt` in Redis payload writes and reads. Older payloads without the field remain readable; replacing a payload updates or clears the timestamp without generating a new one in Redis.
- **Follow-up prompt.**

  > I want to make the Redis change here to make the redis storage consistent with the entry model change.

- **Additional files.** `src/main/java/com/cvnstack/service/RedisCacheStorage.java`, `src/test/java/com/cvnstack/service/RedisCacheStorageTest.java`, this file.
- **UUID review follow-up (2026-10-23).** Added `ApiException.badRequest` and localized UUID parsing in PostgreSQL storage. Only UUID parse failures become HTTP 400 / INVALID_REQUEST; valid missing rows retain empty storage results and existing service 404 handling. Redis exception handling and the global exception handler were unchanged.
- **UUID follow-up prompt.**

  > Implement focused malformed-UUID handling for the PostgreSQL storage paths. A syntactically invalid UUID string should result in 400 Bad Request. A syntactically valid UUID that does not exist should continue to result in 404 Not Found through the existing service logic. Do not add a blanket/global mapping from all IllegalArgumentException to 400. Add ApiException.badRequest, a private UUID parsing helper, replace relevant direct UUID parsing calls, and add focused tests without changing Redis exception handling.

- **Additional UUID files.** `src/main/java/com/cvnstack/error/ApiException.java`, `src/main/java/com/cvnstack/service/PostgresWaitlistStorage.java`, `src/test/java/com/cvnstack/service/PostgresWaitlistStorageTest.java`, this file.
- **UUID follow-up verification.** `mvn test checkstyle:check`: all 93 tests passed, none failed or skipped; zero Checkstyle violations. Three added PostgreSQL integration tests cover malformed waitlist IDs, malformed entry IDs, existing handler HTTP 400 mapping, and valid missing UUIDs producing service 404 responses. Additional token usage: not yet read from the product usage screen.
- **Follow-up verification.** `mvn -Dtest=RedisCacheStorageTest,RedisCacheStorageUnitTest test checkstyle:check`: 28 tests passed, none failed or skipped; zero Checkstyle violations. Three added integration tests cover distinct timestamps at nanosecond precision, replacement and clearing of the cached write timestamp, and legacy payloads without `updatedAt`. Additional token usage has not yet been read from the product usage screen; the earlier estimate is unchanged.

### Issue 4 - Create a waitlist

- **Completed.** 2026-10-23. Local implementation and testing.
- **Tool / source.** Codex, https://openai.com/codex/.
- **Purpose.** Implement POST /waitlists using request/response records, a dedicated creation service, server-generated UUIDs, and the existing PostgreSQL storage port. Validate nonblank names and positive settings, return stored settings with HTTP 200, and propagate duplicate-name conflicts as HTTP 409. Require positive processing time in Waitlist and preserve database constraint coverage through direct SQL. Include service, model, and REST Assured tests.
- **Prompt.**

  > Implement Issue #4: POST /waitlists — create a waitlist. Add CreateWaitlistRequest, WaitlistResponse without updatedAt, a dedicated CreateWaitlistService returning the persisted result, and a controller returning ResponseEntity.ok. Validate input with ApiException.badRequest, enforce positive processing time in Waitlist, propagate storage conflicts, create no entries, and add no Redis caching. Add mocked-storage service tests, model invariant tests, and REST Assured endpoint tests for success, invalid input without persistence, and duplicate-name conflicts.

- **Files.** `src/main/java/com/cvnstack/model/request/CreateWaitlistRequest.java`, `src/main/java/com/cvnstack/model/response/WaitlistResponse.java`, `src/main/java/com/cvnstack/service/CreateWaitlistService.java`, `src/main/java/com/cvnstack/controller/CreateWaitlistController.java`, `src/main/java/com/cvnstack/model/Waitlist.java`, `src/test/java/com/cvnstack/service/CreateWaitlistServiceTest.java`, `src/test/java/com/cvnstack/controller/CreateWaitlistControllerTest.java`, `src/test/java/com/cvnstack/model/WaitlistTest.java`, `src/test/java/com/cvnstack/service/PostgresWaitlistStorageTest.java`, this file.
- **Verification.** Java 21 `mvn test checkstyle:check` passed, with zero Checkstyle violations. Includes PostgreSQL and Redis integration tests; new HTTP tests mock storage and verify no cache or entry operations.
- **Tokens.** **3,192,037** cumulative total since Issue #4 began (3,175,788 input + 16,249 output), including the request DTO organization follow-up. Cached input: 3,099,904, already included in input. Cache-write input: 0. Reasoning output: 708, already included in output. Replaces the earlier snapshot; do not add the two totals together.
- **Usage source.** User-provided local usage record, supplied on 2026-10-23; not a product usage-screen reading. Product usage: not yet read from the product usage screen. This snapshot excludes subsequent turns.
- **Request DTO organization follow-up (2026-10-23).** Moved CreateWaitlistRequest into `com.cvnstack.model.request` and updated controller, service, and service-test imports. Fields, validation, and endpoint behavior are unchanged.
- **Follow-up prompt.**

  > Move CreateWaitlistRequest from com.cvnstack.model to com.cvnstack.model.request, update all production and test imports, and run relevant tests. Keep the refactor organizational without changing fields, validation, endpoint behavior, domain models, or response DTOs.

- **Follow-up verification.** Java 21 `mvn -Dtest=CreateWaitlistServiceTest,CreateWaitlistControllerTest test checkstyle:check`: all 24 tests passed, none skipped; zero Checkstyle violations. No imports reference the previous request package.
### Issue 5 - POST /waitlists/{id}/entries - add an entry

- **Completed.** 2026-10-23.
- **Tool / source.** Codex, https://openai.com/codex/.
- **Purpose.** Add the create-entry request DTO, controller, and dedicated service. Validate before storage access, default priority to 1, use the atomic capacity insert and persisted entry, calculate waiting information through shared rules, and update entry payload and ranked ID caches explicitly. Keep Issue #20 coordination out of scope.
- **Prompt.**

  > Implement GitHub Issue #5: `POST /waitlists/{id}/entries — add an entry`.
  > Work only on Issue #5. Follow the existing project structure and patterns. Do not implement Issue #20 PostgreSQL/Redis consistency coordination in this PR.
  > Do NOT put `waitlistId` in the request DTO. It already comes from the URL path.
  > Use `Integer` rather than primitive `int` so omission can be distinguished and mapped to default priority `1`.
  > Use the returned persisted entry for all subsequent work.
  > Do not implement ranking logic again inside the new service.
  > Include this endpoint's REST Assured tests in this PR, as required by Issue #5.
  > Run the relevant unit tests and REST Assured/controller tests and fix any regressions caused by this implementation.

- **Files.** `src/main/java/com/cvnstack/model/request/CreateEntryRequest.java`, `src/main/java/com/cvnstack/controller/CreateEntryController.java`, `src/main/java/com/cvnstack/service/CreateEntryService.java`, `src/test/java/com/cvnstack/service/CreateEntryServiceTest.java`, `src/test/java/com/cvnstack/controller/CreateEntryControllerTest.java`, this file.
- **Verification.** Initial full suite: `mvn test checkstyle:check` passed 121 tests, none failed or skipped, with zero Checkstyle violations, including existing PostgreSQL and Redis Testcontainers tests. Final focused verification: `mvn -Dtest=CreateEntryServiceTest,*ControllerTest,GlobalExceptionHandlerTest test checkstyle:check` passed 44 tests, none failed or skipped, with zero Checkstyle violations. `git diff --check`: clean. Malformed/unreadable JSON uses Spring default HTTP 400; valid JSON with invalid fields retains application INVALID_REQUEST validation.
- **Tokens.** **2,195,037** total for Issue #5, from the user-provided local usage record (not a product usage-screen reading). Input: 2,183,896; cached input: 2,110,592; cache-write input: 0; output: 11,141; reasoning output: 635. Cached input and reasoning output are reported subcategories, not additional tokens added to the total.
### Issue 8 - PATCH /waitlists/{waitlistId}/entries/{entryId}

- **Completed.** 2026-10-24. Local implementation and testing.
- **Tool / source.** Codex, https://openai.com/codex/.
- **Purpose.** Implement partial status and priority updates with same-value no-ops, persisted-state validation, preserved call timestamps, database-owned write timestamps, derived PostgreSQL-backed ranking, and conditional updates that re-read and re-validate on contention. No Redis coordination was added.
- **Prompt.**

  > Implement GitHub Issue #8: `PATCH /waitlists/{waitlistId}/entries/{entryId}`. Keep this PR focused on PATCH behavior and PostgreSQL-backed correctness. Do not add new Redis coordination logic in this PR. Redis/cache synchronization remains part of Issue #20.

  The detailed request also specified nullable request fields without defaults, a dedicated response with updatedAt and nullable waiting information, validation before reads, no writes for repeated values, existing transition and priority rules, calledAt preservation, a conditional SQL update comparing observed status and priority, database CURRENT_TIMESTAMP and RETURNING, re-reading after conditional-update failure, PostgreSQL-derived positions and estimates, service unit tests, and REST Assured coverage through the real controller and service with mocked storage.
- **Files.** `src/main/java/com/cvnstack/controller/PatchEntryController.java`, `src/main/java/com/cvnstack/model/request/PatchEntryRequest.java`, `src/main/java/com/cvnstack/model/response/PatchEntryResponse.java`, `src/main/java/com/cvnstack/service/PatchEntryService.java`, `src/main/java/com/cvnstack/service/WaitlistStorage.java`, `src/main/java/com/cvnstack/service/PostgresWaitlistStorage.java`, `src/test/java/com/cvnstack/controller/PatchEntryControllerTest.java`, `src/test/java/com/cvnstack/service/PatchEntryServiceTest.java`, `src/test/java/com/cvnstack/service/PostgresWaitlistStorageTest.java`, this file.
- **Verification.** `mvn test checkstyle:check`: 163 tests passed, none failed or skipped; zero Checkstyle violations. Includes 22 PATCH service cases, 15 PATCH REST Assured cases, and two added real PostgreSQL tests for conditional updates and competing writers. `git diff --check`: clean.
- **Tokens.** **1,066,853** total for Issue #8, from the user-provided local usage record (not a product usage-screen reading). Input: 1,056,619; cached input: 1,000,832; cache-write input: 0; output: 10,234; reasoning output: 265. Cached input and reasoning output are reported subcategories, not additional tokens added to the total.
- **Integer-input follow-up (2026-10-24).** Audited all controller request bodies and configuration before editing. Only `CreateEntryRequest.priority` and `PatchEntryRequest.priority` accept integer JSON input in this checkout. Reproduced default truncation with the project's Spring Jackson builder: 4.5 became 4 and 5.9 became 5. No custom mapper or coercion configuration exists. `CreateWaitlistRequest` and its endpoint are absent, so no waitlist-creation API tests were added. No endpoint intentionally depends on fractional truncation.
- **Follow-up prompt.**

  > Audit all integer HTTP request fields and existing Jackson configuration before changing anything. If globally safe, reject floating-point JSON numbers for integer targets using centralized Spring Boot configuration. Keep unrelated coercions and service business-range validation unchanged. Add REST Assured tests for every affected existing endpoint, verify rejection before persistence, and confirm valid integer input still succeeds.

- **Follow-up implementation.** Added one `Jackson2ObjectMapperBuilderCustomizer` bean in the existing application configuration to disable `DeserializationFeature.ACCEPT_FLOAT_AS_INT`. This applies in production and HTTP tests without duplicating settings in the test application properties. Decimal and exponent forms, including mathematically whole values such as 5.0 and 5e0, are rejected for integer targets. Numeric-string behavior remains unchanged and is covered by API compatibility cases.
- **Additional files.** `src/main/java/com/cvnstack/WaitlistApplication.java`, `src/test/java/com/cvnstack/controller/CreateEntryControllerTest.java`, `src/test/java/com/cvnstack/controller/PatchEntryControllerTest.java`, this file.
- **Follow-up verification.** `mvn test checkstyle:check`: 172 tests passed, none failed or skipped, and zero Checkstyle violations. After adding numeric-string compatibility cases, final `mvn -Dtest=CreateEntryControllerTest,PatchEntryControllerTest test checkstyle:check`: 42 endpoint cases passed, none failed or skipped, and zero Checkstyle violations. Eight floating-point rejection cases verify HTTP 400 with no storage or cache interaction. Normal POST priority 4 and PATCH priority 5 return HTTP 200. Existing service range checks remain unchanged. `git diff --check`: clean.
- **Additional tokens.** not yet read from the product usage screen; the earlier user-provided local usage record is unchanged.

### Issue 9 - DELETE /waitlists/{waitlistId}/entries/{entryId} - remove an entry

- **Completed.** 2026-10-24.
- **Tool / source.** Codex, https://openai.com/codex/.
- **Purpose.** Implement the delete-entry controller, dedicated response DTO, and service using the PostgreSQL storage delete result as the entry existence check. Return HTTP 200 on removal and 404 for missing waitlists, missing entries, and repeated deletes. Keep Redis coordination and ranking outside this issue.
- **Prompt.**

  > Implement GitHub Issue #9: `DELETE /waitlists/{waitlistId}/entries/{entryId}`. Keep this PR focused on remove-entry behavior only. Do not implement PostgreSQL/Redis coordination from Issue #20 in this PR. Use a dedicated DeleteEntryResponse with message "Entry successfully removed", entryId, and waitlistId. Verify the waitlist exists, delete directly through storage.deleteEntry, and map false to 404. Do not look up the entry first, calculate ranking, or update Redis. Add service unit tests and REST Assured tests through the real controller and service with mocked storage for success, missing waitlist, missing entry, and a second delete. Add purpose and AI-assisted comments and run relevant tests.

- **Files.** `src/main/java/com/cvnstack/controller/DeleteEntryController.java`, `src/main/java/com/cvnstack/model/response/DeleteEntryResponse.java`, `src/main/java/com/cvnstack/service/DeleteEntryService.java`, `src/test/java/com/cvnstack/controller/DeleteEntryControllerTest.java`, `src/test/java/com/cvnstack/service/DeleteEntryServiceTest.java`, this file.
- **Verification.** `mvn test checkstyle:check`: 133 tests passed, none failed or skipped; zero Checkstyle violations. Includes five new service tests and four new REST Assured tests, plus existing PostgreSQL and Redis integration tests. Storage failures propagate unchanged. `git diff --check`: clean.
- **Tokens.** **558,484** total for Issue #9, from the user-provided local usage record (not a product usage-screen reading). Input: 553,175; cached input: 516,608; cache-write input: 0; output: 5,309; reasoning output: 57. Cached input and reasoning output are reported subcategories, not additional tokens added to the total.

### Issue 10 - Read-path API tests

- **Completed.** 2026-10-23. Local read-path tests. Write-route cases and the remaining #20 integration are not included.
- **Purpose.** Test GET entry and serve-next through HTTP with real PostgreSQL and Redis containers. Keep the existing mock-based tests and production code unchanged.
- **Prompt.**

  > Start issue #10 on a separate branch from the latest main. Reuse the existing test structure and add REST Assured tests with actual PostgreSQL and Redis, not mocked storage. Prepare test data through the repository because the write endpoints are not implemented yet. Verify GET, serve-next, database changes, and cache invalidation. Leave the remaining write-route and #20 cases separate. Run the full checks before committing or pushing. Commit the local work, merge the latest main while preserving both task records, and rerun the full checks.

  > Resolve the conflict in PR #32 after the DELETE changes merge into main. Keep both task records and run the full checks before pushing.

- **Files.** `src/test/java/com/cvnstack/controller/ReadPathApiTest.java`, `README.md`, this file.
- **Verification.** The new class passed all 13 test executions. `mvn clean verify`: all 106 tests passed on the original base. After merging main on 2026-10-24, all 137 tests passed, none failed or skipped, with zero Checkstyle violations. No production changes were needed for the tests.
- **Merge follow-up.** 2026-10-24: preserved the #9 and #10 records when merging main after PR #33. `mvn clean verify`: all 146 tests passed, none failed or skipped; zero Checkstyle violations.
- **Tokens.** not yet read from the product usage screen.
