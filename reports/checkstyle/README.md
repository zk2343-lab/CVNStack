<!-- AI-assisted. -->

# Checkstyle report

**Result: clean.** Zero violations on the current `main` tree.

| | |
| --- | --- |
| Generated | 2026-10-25 |
| Tree | `e1953c7` (`origin/main` at generation time) |
| Command | `mvn checkstyle:checkstyle checkstyle:check` (JDK 21) |
| Ruleset | [`config/checkstyle/checkstyle.xml`](../../config/checkstyle/checkstyle.xml) (Google Java Style: 2-space indent, 100-char lines) |
| Plugin | `maven-checkstyle-plugin` 3.6.0 / Checkstyle 10.26.1 |
| Sources | `src/main/java` and `src/test/java` (77 `.java` files) |
| XML | [`cvnstack-checkstyle-result.xml`](cvnstack-checkstyle-result.xml) |

`mvn checkstyle:check` exited 0. Checkstyle 10 writes an empty `<checkstyle>` document when every file is clean; that XML is the official plugin output, not a stub.

## Files scanned

### `src/main/java` (47)

- `com.cvnstack.WaitlistApplication`
- `com.cvnstack.controller`: `CreateEntryController`, `CreateWaitlistController`, `DeleteEntryController`, `EntryQueryController`, `HealthController`, `PatchEntryController`, `ServeNextController`, `StoreSnapshotController`, `WaitlistQueryController`
- `com.cvnstack.error`: `ApiException`, `ErrorCode`, `GlobalExceptionHandler`
- `com.cvnstack.model`: `Entry`, `EntryStatus`, `EntryTransitions`, `WaitEstimate`, `WaitingOrder`, `Waitlist`
- `com.cvnstack.model.request`: `CreateEntryRequest`, `CreateWaitlistRequest`, `PatchEntryRequest`
- `com.cvnstack.model.response`: `DeleteEntryResponse`, `EntryResponse`, `ErrorResponse`, `HealthResponse`, `PatchEntryResponse`, `ServeNextResponse`, `StoreSnapshotResponse`, `WaitlistDetailResponse`, `WaitlistResponse`
- `com.cvnstack.service`: `CacheConsistencyService`, `CacheStorage`, `CreateEntryService`, `CreateWaitlistService`, `DeleteEntryService`, `EntryQueryService`, `EntryRulesService`, `HealthService`, `PatchEntryService`, `PostgresWaitlistStorage`, `RedisCacheStorage`, `ServeNextService`, `StoreSnapshotService`, `WaitlistQueryService`, `WaitlistRulesService`, `WaitlistStorage`

### `src/test/java` (30)

- `com.cvnstack.controller`: `CreateEntryControllerTest`, `CreateWaitlistControllerTest`, `DeleteEntryControllerTest`, `EntryQueryControllerTest`, `HealthControllerTest`, `PatchEntryControllerTest`, `ReadPathApiTest`, `ServeNextControllerTest`, `WaitlistQueryControllerTest`
- `com.cvnstack.error`: `ErrorProbeController`, `GlobalExceptionHandlerTest`
- `com.cvnstack.model`: `EntryTest`, `EntryTransitionsTest`, `WaitEstimateTest`, `WaitingOrderTest`, `WaitlistTest`
- `com.cvnstack.service`: `CacheConsistencyServiceTest`, `CreateEntryServiceTest`, `CreateWaitlistServiceTest`, `DeleteEntryServiceTest`, `EntryQueryServiceTest`, `EntryRulesServiceTest`, `PatchEntryServiceTest`, `PostgresWaitlistStorageTest`, `RedisCacheStorageTest`, `RedisCacheStorageUnitTest`, `ServeNextServiceTest`, `StoreConsistencyTest`, `WaitlistQueryServiceTest`, `WaitlistRulesServiceTest`

Regenerate with `mvn checkstyle:checkstyle` and replace `cvnstack-checkstyle-result.xml`.
