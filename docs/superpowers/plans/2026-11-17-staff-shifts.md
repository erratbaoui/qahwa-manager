# Staff Shifts & Cash Settlement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every order belongs to a named waiter's shift. The owner settles each shift (expected cash vs counted cash, with the difference kept forever), and only the owner, behind a PIN, can cancel orders, edit the menu, prices and stock, see revenue, manage staff and settle.

**Architecture:** Room v3 adds `staff`, `shifts`, `shift_left_open` and `owner_credentials`, plus shift columns on `orders`. Multi-row rules (one active shift; the end-of-shift snapshot; settlement) live in DAO `@Transaction` methods. `OwnerAuthRepository` holds the PIN (PBKDF2 hash, persisted lockout). `OwnerSession` is an in-memory singleton for owner mode, and `OwnerGate` is a Compose helper that asks for the PIN before running an owner-only action. ViewModels also refuse owner-only actions when owner mode is off.

**Tech Stack:** Kotlin 2.3, Jetpack Compose M3, Navigation 3, Hilt, Room 2.8 (KSP), kotlinx-coroutines, JUnit4 + kotlinx-coroutines-test, Room `MigrationTestHelper`.

**Spec:** `docs/superpowers/specs/2026-11-17-staff-shifts-design.md` (read it first; this plan argues from it).

## Global Constraints

- Branch `feat/staff-shifts`. Before Task 1, **rebase it onto `main` after the release-signing PR has merged** (it only holds docs commits). Never push to `main`.
- Build and test with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. Unit: `./gradlew :app:testDebugUnitTest`. Instrumented: `./gradlew :app:connectedDebugAndroidTest` on emulator `qahwa-api30` or `qahwa-api34` (never the `flow-*` AVDs). With AGP 9.5, read results from `app/build/outputs/androidTest-results/connected/debug/*.xml`.
- No new dependencies and no version bumps (`kotlinx-coroutines-test` must not pass the resolved coroutines-core).
- minSdk 24: PIN hashing uses `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")`, 20 000 iterations, 256-bit key, 16-byte salt from `SecureRandom`, hex-encoded. Compare with `MessageDigest.isEqual`. Hash off the main thread (`Dispatchers.Default`).
- Money is `Long` centimes. Typed amounts go through `parseMadToCentimes` (`util/Money.kt`).
- Every new string goes in `values/`, `values-fr/` and `values-ar/strings.xml` (`StringResourcesTest`). New screens must render right-to-left in Arabic. Match the tone of the existing translations.
- UI uses the existing `QahwaComponents` (`QahwaCard`, `QahwaPrimaryButton`, `QahwaDestructiveButton`, `QahwaTextField`, `QahwaConfirmDialog`, `QahwaPageHeader`, `QahwaCurrencyText`, `QahwaEmptyState`, `QahwaSectionHeader`) and `QahwaTheme.colors`. No new colors.
- PIN = exactly 4 digits. Lockout: 5 free wrong tries in a row, then 60 s, doubling with each further failure, capped at 1 h. Owner mode idle timeout: 120 000 ms. Recovery code: 8 characters from `ABCDEFGHJKMNPQRSTUVWXYZ23456789`, shown as `XXXX-XXXX`.
- **Owner-gate rule for UI:** wrap the *final* action (save, delete, adjust, confirm), not the opening of a form. If owner mode expired while a form was open, the PIN is asked again and the form isn't lost.
- Commit after each task with a Conventional Commits message ending in `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **App killed or tablet restarted mid-shift.** The shift is still active (it's in the DB), and owner mode is **off** (a new `OwnerSession` starts locked). Tested in Task 4 (`newSession_startsLocked`) and Task 5 (`activeShift_survivesNewRepositoryInstance`).
2. **Double taps on Start shift, Take over, End shift or Confirm settlement.** Exactly one shift is created or ended and exactly one settlement is saved. Tested in Task 5 (`start_concurrentCallsCreateOneShift`, `settle_secondCallIsRefused`) and Task 9 (`settle_ignoresSecondTapWhileSaving`).
3. **Counted cash typed as `610,50`, `610.5`, blank or `abc`.** Valid input parses to centimes. Invalid input disables Confirm; it never settles as 0. Tested in Task 9 (`countedInput_frenchCommaParses`, `settle_refusesInvalidInput`).
4. **Lockout across restarts, and a correct PIN typed during lockout.** The lockout persists, and a correct PIN during it still returns `LockedOut`. Tested in Task 3 (`lockout_persistsAcrossRepositoryInstances`, `correctPinDuringLockout_isRefused`).
5. **Owner mode expiring between opening a form and saving it.** The save asks for the PIN and then runs exactly once. Tested in Task 4 (`gate_runsPendingActionOnceAfterVerify`).

---

### Task 1: Database v3 schema and migration 2→3

**Files:**
- Create: `app/src/main/kotlin/com/example/qahwamanager/data/model/Staff.kt`, `Shift.kt`, `ShiftLeftOpen.kt`, `OwnerCredentials.kt`
- Modify: `data/model/Order.kt`, `data/db/Converters.kt`, `data/db/QahwaDatabase.kt`, `data/db/Migrations.kt`
- Generated: `app/schemas/com.example.qahwamanager.data.db.QahwaDatabase/3.json` (commit it)
- Test: `app/src/androidTest/kotlin/com/example/qahwamanager/data/db/MigrationTest.kt`

**Interfaces:**
- Produces:
  - `Staff(id: Long = 0, name: String, isOwner: Boolean = false, isActive: Boolean = true, createdAt: Long)`, table `staff`.
  - `enum class ShiftStatus { ACTIVE, ENDED, SETTLED }`.
  - `Shift(id: Long = 0, staffId: Long, status: ShiftStatus = ShiftStatus.ACTIVE, startedAt: Long, endedAt: Long? = null, endedByTakeover: Boolean = false, settledAt: Long? = null, expectedCentimes: Long? = null, countedCentimes: Long? = null, failedOwnerPinAttempts: Int = 0)`, table `shifts`, FK `staffId → staff.id`, indices `staffId` and `status`.
  - `val Shift.differenceCentimes: Long?` = `countedCentimes - expectedCentimes` when both are non-null, otherwise null.
  - `ShiftLeftOpen(shiftId: Long, orderId: Long, totalCentimes: Long)`, table `shift_left_open`, primary key `(shiftId, orderId)`, FKs to `shifts.id` and `orders.id`, index `orderId`.
  - `OwnerCredentials(id: Int = 1, pinHash: String, pinSalt: String, recoveryHash: String, recoverySalt: String, failedAttemptsInRow: Int = 0, lockedUntil: Long = 0)`, table `owner_credentials`, a single row with id 1.
  - `Order` gains `openedShiftId: Long? = null, paidShiftId: Long? = null, paidAt: Long? = null, cancelledAt: Long? = null, cancelledShiftId: Long? = null`, with indices `openedShiftId` and `paidShiftId`. **No foreign keys on these columns**, because they're added with `ALTER TABLE`.
  - `MIGRATION_2_3`. `QahwaDatabase` becomes version 3 and adds the migration to the builder.

- [ ] **Step 1: Write the failing migration test** `migrate2To3_keepsExistingDataAndAddsShiftTables`. Create the v2 DB with: menu item id 1 (850 centimes), inventory item id 1, order id 10 (`PAID`, total 1729, createdAt 1000), order id 11 (`OPEN`), and an order item under 10. Migrate with `helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)`. Assert:
  - Every v2 value reads back unchanged.
  - `openedShiftId`, `paidShiftId`, `paidAt`, `cancelledAt` and `cancelledShiftId` are NULL for orders 10 and 11.
  - `SELECT COUNT(*)` from each of `staff`, `shifts`, `shift_left_open` and `owner_credentials` is 0.

- [ ] **Step 2: Run it and see it fail.** Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.qahwamanager.data.db.MigrationTest`. Expected: a compile failure (`MIGRATION_2_3` is unresolved).

- [ ] **Step 3: Add the entities, the `ShiftStatus` converters and the DB version bump**, then build once (`./gradlew :app:kspDebugKotlin`) to generate `3.json`.

- [ ] **Step 4: Write `MIGRATION_2_3` in `Migrations.kt`.** Copy each `CREATE TABLE` / `CREATE INDEX` statement exactly from `3.json`. Add the five order columns with `ALTER TABLE orders ADD COLUMN <name> INTEGER`, then create `index_orders_openedShiftId` and `index_orders_paidShiftId`. Add a KDoc in the style of `MIGRATION_1_2` explaining that it's additive.

- [ ] **Step 5: Run the migration test.** Expected: both migration tests pass (1→2 and 2→3). Check the XML result.

- [ ] **Step 6: Run the unit tests** (`./gradlew :app:testDebugUnitTest`). Expected: pass, because the new `Order` fields have defaults.

- [ ] **Step 7: Commit.** `feat(db): Add staff, shifts and owner credentials tables (schema v3)`

---

### Task 2: DAOs for staff, shifts and owner; shift-aware order writes; test fakes; DI

**Files:**
- Create: `data/db/StaffDao.kt`, `data/db/ShiftDao.kt`, `data/db/OwnerDao.kt`, `data/model/ShiftWithStaff.kt`
- Modify: `data/db/OrderDao.kt`, `data/db/QahwaDatabase.kt` (abstract DAO getters), `di/AppModule.kt` (providers)
- Modify: `app/src/test/kotlin/com/example/qahwamanager/testing/Fakes.kt`
- Test: `app/src/androidTest/.../data/db/ShiftDaoTest.kt` (new), `OrderDaoTest.kt` (extend)

**Interfaces:**
- Produces:
  - `data class ShiftWithStaff(@Embedded val shift: Shift, val staffName: String)`. Queries select `shifts.*, staff.name AS staffName` with a JOIN.
  - `class ShiftAlreadyActiveException : IllegalStateException()`
  - `interface StaffDao`:
    - `observeActive(): Flow<List<Staff>>` (`isActive = 1`, owner first, then by name)
    - `observeAll(): Flow<List<Staff>>`
    - `suspend getById(id: Long): Staff?`
    - `suspend insert(staff: Staff): Long`
    - `suspend update(staff: Staff)`
  - `abstract class ShiftDao`:
    - `suspend getActive(): Shift?`
    - `observeActive(): Flow<ShiftWithStaff?>`
    - `observeById(id: Long): Flow<ShiftWithStaff?>`
    - `observeToSettle(): Flow<List<ShiftWithStaff>>` (status ENDED, `endedAt` ascending)
    - `observeSettled(staffId: Long?): Flow<List<ShiftWithStaff>>` (status SETTLED, `settledAt` descending; null means all staff)
    - `observeExpectedCentimes(shiftId: Long): Flow<Long>` and `suspend expectedCentimes(shiftId: Long): Long`. Both are `COALESCE(SUM(totalCentimes), 0)` over orders with `paidShiftId = :shiftId AND status = 'PAID'`.
    - `observeLeftOpen(shiftId: Long): Flow<List<ShiftLeftOpen>>`
    - `suspend recordFailedOwnerPinOnActive(): Int`
    - `@Transaction open suspend startShift(staffId: Long, now: Long): Long`. Throws `ShiftAlreadyActiveException` when a shift is active.
    - `@Transaction open suspend endActiveShift(now: Long, takeover: Boolean): Long?`. Snapshots every `OPEN` order into `shift_left_open` with its current total, sets the shift to ENDED with `endedAt` and `endedByTakeover`, and returns the shift id. Returns null if no shift is active.
    - `@Transaction open suspend takeOver(newStaffId: Long, now: Long): Long`. Calls `endActiveShift(now, takeover = true)`, then `startShift(newStaffId, now)`.
    - `@Transaction open suspend settle(shiftId: Long, countedCentimes: Long, now: Long): Boolean`. Copies `expectedCentimes(shiftId)` onto the shift along with counted and `settledAt`, and sets status SETTLED, only where status is ENDED. Returns whether a row changed.
  - `abstract class OwnerDao`:
    - `observeExists(): Flow<Boolean>`
    - `suspend getCredentials(): OwnerCredentials?`
    - `@Upsert suspend upsertCredentials(c: OwnerCredentials)`
    - `@Insert suspend insertStaff(staff: Staff): Long`
    - `@Transaction open suspend createOwner(owner: Staff, credentials: OwnerCredentials): Long`. Inserts both and returns the staff id.
  - `OrderDao` adds:
    - `suspend markPaidIfOpen(id: Long, shiftId: Long, now: Long): Int`. Sets PAID, `paidShiftId` and `paidAt`, only where status is OPEN.
    - `suspend cancelIfOpen(id: Long, shiftId: Long?, now: Long): Int`. Sets CANCELLED, `cancelledAt` and `cancelledShiftId`, only where status is OPEN.
    - `observePaidInShift(shiftId: Long): Flow<List<Order>>`
    - `observeCancelledForShift(shiftId: Long): Flow<List<Order>>`. Matches `status = 'CANCELLED' AND COALESCE(openedShiftId, cancelledShiftId) = :shiftId`.
    - `updateStatusIfOpen` **stays for now**; Task 6 removes it.
  - Fakes (`Fakes.kt`): `FakeStaffDao`, `FakeShiftDao(orderDao: FakeOrderDao, staffDao: FakeStaffDao)` and `FakeOwnerDao(staffDao: FakeStaffDao)`. Each mirrors the SQL semantics above. `FakeShiftDao` serializes its `@Transaction` methods with a `Mutex` and uses `IO_DELAY_MS`, so concurrent calls interleave the way they do against Room. `FakeOrderDao` implements the new order methods.
  - `AppModule` provides `StaffDao`, `ShiftDao` and `OwnerDao`.

- [ ] **Step 1: Write the failing instrumented tests** in `ShiftDaoTest` (in-memory DB, as in `OrderDaoTest`):
  - `startShift_whileActive_throws`: start for staff A, then expect `ShiftAlreadyActiveException` when starting for staff B.
  - `endActiveShift_snapshotsOpenOrders`: two OPEN orders (500, 700) and one PAID. After ending, `shift_left_open` holds exactly the two open order ids with 500 and 700. The shift is ENDED, `endedByTakeover` is false, and `getActive()` is null.
  - `expectedCentimes_sumsOnlyOrdersPaidInThatShift`: 800 and 1200 paid in shift 1, 500 paid in shift 2, and one CANCELLED with `openedShiftId` = 1. Expect 2000 for shift 1.
  - `settle_copiesExpectedAndRefusesSecondCall`: an ENDED shift with 2000 expected, `settle(id, 1700, now)` returns true and stores expected 2000, counted 1700 and SETTLED. Then mark another order paid with that `paidShiftId` and call settle again: it returns false, and the stored expected is still 2000.
  - `takeOver_endsOldShiftFlaggedAndStartsNew`.
  - `OrderDaoTest.cancelledForShift_fallsBackToCancelledShiftId`: an order with null `openedShiftId`, cancelled with `shiftId` 3, appears in `observeCancelledForShift(3)`.

- [ ] **Step 2: Run them and see them fail** (compile errors: unresolved DAOs).

- [ ] **Step 3: Implement the DAOs, `ShiftWithStaff`, the DB getters and the `AppModule` providers.**

- [ ] **Step 4: Run the instrumented tests.** Expected: all pass, including the existing `OrderDaoTest` and `InventoryDaoTest`.

- [ ] **Step 5: Add the fakes to `Fakes.kt`, then run** `./gradlew :app:testDebugUnitTest`. Expected: pass (the fakes compile, and existing tests are unchanged).

- [ ] **Step 6: Commit.** `feat(db): Add staff, shift and owner DAOs with transactional shift rules`

---

### Task 3: Owner PIN, recovery code and lockout

**Files:**
- Create: `security/PinHasher.kt`, `security/RecoveryCode.kt`, `security/PinLockout.kt`, `data/repository/OwnerAuthRepository.kt`
- Test: `app/src/test/.../security/PinHasherTest.kt`, `RecoveryCodeTest.kt`, `PinLockoutTest.kt`, `data/repository/OwnerAuthRepositoryTest.kt`

**Interfaces:**
- Consumes: `OwnerDao` and `ShiftDao.recordFailedOwnerPinOnActive()` from Task 2, and `Clock`.
- Produces:
  - `object PinHasher`: `fun newSalt(): String`, `fun hash(secret: String, saltHex: String): String`, `fun matches(secret: String, saltHex: String, expectedHex: String): Boolean`.
  - `fun isValidPin(pin: String): Boolean` (exactly 4 ASCII digits).
  - `object RecoveryCode`: `fun generate(random: SecureRandom = SecureRandom()): String` (8 chars), `fun normalize(input: String): String` (uppercase; strip spaces and `-`), `fun format(code: String): String` (`"ABCD-EFGH"`).
  - `object PinLockout`: `const val FREE_ATTEMPTS = 5`, `fun lockDurationMillis(failuresInRow: Int): Long`.
  - `sealed interface PinCheck { data object Ok; data class Wrong(val attemptsBeforeLock: Int); data class LockedOut(val untilMillis: Long) }`
  - `class OwnerAuthRepository @Inject constructor(ownerDao: OwnerDao, shiftDao: ShiftDao, clock: Clock)`:
    - `fun observeOwnerExists(): Flow<Boolean>`
    - `suspend fun setUpOwner(name: String, pin: String): String`. Returns the raw recovery code. Throws `IllegalArgumentException` for a blank name or an invalid PIN. Throws `IllegalStateException` if an owner already exists.
    - `suspend fun checkPin(pin: String): PinCheck`
    - `suspend fun resetPinWithRecoveryCode(code: String, newPin: String): String?`. Returns the new recovery code, or null when the code is wrong. A wrong code counts as a failed attempt.
    - `suspend fun changePin(currentPin: String, newPin: String): PinCheck`. On `Ok`, the PIN is changed.

- [ ] **Step 1: Write the failing tests.**
  - `PinHasherTest`:
    - `sameSecretAndSalt_sameHash`
    - `differentSalt_differentHash`
    - `matches_rejectsWrongPin`
    - `hash_isHex64Chars` (256 bits)
  - `RecoveryCodeTest`:
    - `generate_is8CharsFromAlphabet`
    - `normalize_acceptsLowercaseAndDash` (`"abcd-efgh "` gives `"ABCDEFGH"`)
    - `format_insertsDash`
  - `PinLockoutTest`: `lockDurationMillis` for 0..4 is 0, for 5 is 60 000, for 6 is 120 000, for 7 is 240 000, and for 20 is 3 600 000.
  - `OwnerAuthRepositoryTest`, using the fakes and `testClock`:
    - `setUpOwner_createsOwnerStaffAndStoresNoPlainPin`: the owner exists, the staff row has `isOwner` true, and the credentials don't contain `"1234"`.
    - `checkPin_correct_isOkAndResetsFailures`
    - `checkPin_wrongFiveTimes_locksForOneMinute`: the fifth result is `LockedOut(now + 60_000)`, and the fourth is `Wrong(1)`.
    - `correctPinDuringLockout_isRefused`
    - `lockout_persistsAcrossRepositoryInstances`: a new repository over the same fake DAOs is still locked.
    - `lockout_endsAfterDuration`: after the clock advances 60 s, the correct PIN gives `Ok`.
    - `wrongPin_recordsAttemptOnActiveShift`: with an active shift, its `failedOwnerPinAttempts` becomes 1. With no shift, the call still counts toward the lockout.
    - `recoveryCode_resetsPinAndRotatesCode`: the old code no longer works, the new PIN works, and the old PIN fails.
    - `wrongRecoveryCode_countsAsFailure`
    - `changePin_requiresCurrentPin`

- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.example.qahwamanager.security.*' --tests '*OwnerAuthRepositoryTest'`. Expected: compile failure.

- [ ] **Step 3: Implement the four files,** following Global Constraints for the algorithm, iterations, encoding and lockout values.

- [ ] **Step 4: Run the same command.** Expected: PASS.

- [ ] **Step 5: Commit.** `feat(auth): Add owner PIN hashing, recovery code and persisted lockout`

---

### Task 4: Owner mode session and the owner gate

**Files:**
- Create: `security/OwnerSession.kt`, `ui/auth/OwnerGate.kt`
- Modify: `MainActivity.kt` (inject `OwnerSession`; touch, lock on stop, idle timer)
- Test: `app/src/test/.../security/OwnerSessionTest.kt`, `app/src/test/.../ui/auth/OwnerGateTest.kt`

**Interfaces:**
- Produces:
  - `@Singleton class OwnerSession @Inject constructor(clock: Clock)`:
    - `companion object { const val IDLE_TIMEOUT_MS = 120_000L }`
    - `val isOwnerMode: StateFlow<Boolean>`
    - `fun unlock()`, `fun lock()`, `fun touch()`
    - `suspend fun enforceIdleTimeout(): Nothing`. While unlocked, it waits until `lastTouch + IDLE_TIMEOUT_MS`, then locks if there was no newer touch.
  - `@Stable class OwnerGate(session: OwnerSession)`:
    - `val pending: (() -> Unit)?`, backed by `mutableStateOf`
    - `fun require(action: () -> Unit)`. Runs `action` now in owner mode; otherwise stores it as `pending`.
    - `fun onVerified()`. Unlocks the session, runs `pending` once and clears it.
    - `fun onDismissed()`. Clears `pending`.
  - `val LocalOwnerGate = staticCompositionLocalOf<OwnerGate> { error("OwnerGate not provided") }`
  - In `MainActivity`:
    - `@Inject lateinit var ownerSession: OwnerSession`
    - `dispatchTouchEvent` calls `ownerSession.touch()` before `super`
    - `onStop` calls `ownerSession.lock()` unless `isChangingConfigurations`
    - `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ownerSession.enforceIdleTimeout() } }`

- [ ] **Step 1: Write the failing tests.**
  - `OwnerSessionTest`, with virtual time and `testClock`, running `enforceIdleTimeout` in `backgroundScope`:
    - `newSession_startsLocked`
    - `unlock_thenIdle119s_stillOwner`
    - `unlock_thenIdle120s_locks`
    - `touchAt60s_extendsTo180s`: owner mode holds at 179 s and is locked at 180 s.
    - `lock_isImmediate`
  - `OwnerGateTest`:
    - `require_inOwnerMode_runsNow`
    - `require_locked_storesPending_andDoesNotRun`
    - `gate_runsPendingActionOnceAfterVerify`: the action counter is 1 after `onVerified()`, and a second `onVerified()` leaves it at 1. The session is unlocked.
    - `onDismissed_dropsPending`

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement `OwnerSession`, `OwnerGate` and the `MainActivity` hooks.**

- [ ] **Step 4: Run the tests.** Expected: PASS. Also run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL (the Hilt graph is valid).

- [ ] **Step 5: Commit.** `feat(auth): Add in-memory owner mode with idle timeout and owner gate`

---

### Task 5: Staff and shift repositories

**Files:**
- Create: `data/repository/StaffRepository.kt`, `data/repository/ShiftRepository.kt`
- Test: `app/src/test/.../data/repository/StaffRepositoryTest.kt`, `ShiftRepositoryTest.kt`

**Interfaces:**
- Consumes: the Task 2 DAOs and `Clock`.
- Produces:
  - `class StaffRepository @Inject constructor(staffDao: StaffDao, clock: Clock)`:
    - `observeActive()`, `observeAll()`
    - `suspend addWaiter(name: String): Long`. Trims the name; throws `IllegalArgumentException` if it's blank.
    - `suspend rename(id: Long, name: String)`, with the same blank rule.
    - `suspend setActive(id: Long, active: Boolean)`. Throws `IllegalArgumentException` when deactivating the owner.
  - `data class ShiftReport(val shift: ShiftWithStaff, val expectedCentimes: Long, val paidOrders: List<Order>, val cancelledOrders: List<Order>, val leftOpen: List<ShiftLeftOpen>)`. While the shift isn't SETTLED, `expectedCentimes` is the live sum; once it's SETTLED, it's the stored value.
  - `class ShiftRepository @Inject constructor(shiftDao: ShiftDao, orderDao: OrderDao, clock: Clock)`:
    - `observeActive(): Flow<ShiftWithStaff?>`
    - `observeCollectedCentimes(shiftId: Long): Flow<Long>`
    - `suspend activeShiftId(): Long?`
    - `suspend start(staffId: Long): Long`
    - `suspend endActive(): Long?`
    - `suspend takeOver(newStaffId: Long): Long`
    - `suspend settle(shiftId: Long, countedCentimes: Long): Boolean`
    - `observeToSettle()`, `observeHistory(staffId: Long?)`
    - `observeReport(shiftId: Long): Flow<ShiftReport?>`

- [ ] **Step 1: Write the failing tests** (fakes, `testClock`):
  - `StaffRepositoryTest`:
    - `addWaiter_trimsName`
    - `addWaiter_blank_throws`
    - `deactivateOwner_throws`
    - `deactivatedWaiter_leavesActiveList_keepsInAll`
  - `ShiftRepositoryTest`:
    - `start_concurrentCallsCreateOneShift`: two `async { runCatching { start(...) } }`. Exactly one succeeds, and the fake holds one ACTIVE shift.
    - `activeShift_survivesNewRepositoryInstance`
    - `expected_countsOrdersPaidAfterMidnightInSameShift`: shift started at 23:00, orders paid at 23:30 and 00:30. Expected is the sum of both.
    - `endActive_leavesOpenOrdersOutOfExpected`: one paid (800) and one open (500). Expected is 800, and `leftOpen` is `[500]`.
    - `leftOpenOrderPaidLater_countsForTheShiftThatCollectsIt`: Karim's shift ends with order X open. In Youssef's shift X is marked paid. Youssef's expected includes X, Karim's report still lists X as left open, and Karim's expected excludes it.
    - `takeOver_flagsOldShiftAndStartsNew`
    - `settle_snapshotsAndComputesDifference`: expected 64 000, counted 61 000, so `differenceCentimes` is -3000.
    - `settle_secondCallIsRefused`
    - `settle_activeShift_isRefused`
    - `report_settledShiftKeepsStoredExpected`

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement both repositories.** `observeReport` combines `observeById`, `observeExpectedCentimes`, `observePaidInShift`, `observeCancelledForShift` and `observeLeftOpen`.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `feat(shifts): Add staff and shift repositories`

---

### Task 6: Orders belong to shifts; cancel is owner-only

**Files:**
- Modify: `data/repository/OrderRepository.kt`, `data/db/OrderDao.kt` (remove `updateStatusIfOpen`), `ui/orders/OrdersViewModel.kt`, `testing/Fakes.kt`
- Test: `app/src/test/.../ui/orders/OrdersViewModelTest.kt`, plus any other test that builds `OrderRepository`

**Interfaces:**
- Consumes: `ShiftRepository` (Task 5), `OwnerSession` (Task 4).
- Produces:
  - `OrderRepository(orderDao, orderItemDao, clock: Clock)`:
    - `suspend createOrder(tableNumber: String, items: List<OrderItem>, shiftId: Long): Long` (sets `openedShiftId`)
    - `suspend markPaid(orderId: Long, shiftId: Long): Boolean`
    - `suspend cancel(orderId: Long, activeShiftId: Long?): Boolean`
    - `updateOrderStatus` is removed.
  - `OrdersViewModel(orderRepository, menuRepository, shiftRepository, ownerSession)`:
    - adds `val activeShift: StateFlow<ShiftWithStaff?>`
    - `placeOrder` and `markAsPaid` do nothing when no shift is active
    - `cancelOrder` does nothing unless `ownerSession.isOwnerMode.value`
    - all other signatures are unchanged

- [ ] **Step 1: Update `OrdersViewModelTest` setup** to build the new constructor with fakes and an `OwnerSession(testClock)`, starting a shift in `@Before` unless a test says otherwise. Add:
  - `placeOrder_stampsOpenedShift`
  - `placeOrder_withoutActiveShift_doesNothing`: no order is stored, and the cart is kept.
  - `markAsPaid_stampsPaidShiftAndTime`
  - `markAsPaid_withoutActiveShift_doesNothing`
  - `cancelOrder_locked_doesNothing`: the order stays OPEN.
  - `cancelOrder_ownerMode_cancelsAndStamps`

  Keep the existing tests passing unchanged in intent.

- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest`. Expected: the new tests fail.

- [ ] **Step 3: Implement it. Fix every other compile site** that passed the old constructor or called `updateOrderStatus` (Hilt injects the new parameters automatically).

- [ ] **Step 4: Run the unit tests and the instrumented `OrderDaoTest`.** Expected: PASS.

- [ ] **Step 5: Commit.** `feat(orders): Stamp orders with their shift and require owner mode to cancel`

---

### Task 7: First-run owner setup, PIN dialog, recovery, change PIN, owner banner

**Files:**
- Create: `ui/root/RootViewModel.kt`, `ui/auth/OwnerSetupScreen.kt`, `ui/auth/OwnerPinDialog.kt`, `ui/auth/OwnerPinViewModel.kt`, `ui/auth/ChangePinDialog.kt`, `ui/auth/OwnerModeBanner.kt`
- Modify: `MainActivity.kt` (root switch), `Navigation.kt` (provide `LocalOwnerGate`, host the dialog, show the banner), and `res/values{,-fr,-ar}/strings.xml`
- Test: `app/src/test/.../ui/root/RootViewModelTest.kt`, `app/src/test/.../ui/auth/OwnerPinViewModelTest.kt`

**Interfaces:**
- Consumes: `OwnerAuthRepository` (Task 3), `OwnerSession` and `OwnerGate` (Task 4).
- Produces:
  - `RootViewModel(ownerAuthRepository)`:
    - `val screen: StateFlow<RootScreen>`, where `enum class RootScreen { LOADING, SETUP, APP }`
    - `val recoveryCodeToShow: StateFlow<String?>`
    - `fun setUp(name: String, pin: String, pinConfirm: String): SetupError?`, where `enum class SetupError { BLANK_NAME, INVALID_PIN, PIN_MISMATCH }`
    - `fun acknowledgeRecoveryCode()`
    - SETUP stays the screen while `recoveryCodeToShow != null`, even though the owner already exists.
  - `OwnerPinViewModel(ownerAuthRepository, ownerSession)`:
    - `val state: StateFlow<PinUiState>`, where `PinUiState(error: PinCheck?, busy: Boolean)`
    - `fun submit(pin: String, onOk: () -> Unit)`
    - `fun resetWithCode(code: String, newPin: String, newPinConfirm: String, onNewCode: (String) -> Unit)`
    - `fun changePin(current: String, new: String, confirm: String, onDone: () -> Unit)`. Changing the PIN keeps the existing recovery code.
  - `@Composable fun OwnerPinDialog(onVerified: () -> Unit, onDismiss: () -> Unit)`: a 4-dot PIN pad with digits 0–9 and backspace. It shows "Wrong PIN, N tries left" or "Locked, try again in m:ss" (live countdown), plus a *Forgot PIN?* link that switches to the recovery form.
  - `@Composable fun OwnerModeBanner(onLock: () -> Unit)`, shown at the top of `MainNavigation` while in owner mode.
  - Copy (English; translate into FR and AR): `owner_setup_title` "Set up the owner", `owner_name` "Your name", `create_pin` "Choose a 4-digit PIN", `confirm_pin` "Enter the PIN again", `recovery_code_title` "Write down this recovery code", `recovery_code_message` "If you forget your PIN, this code is the only way back in. Keep it somewhere safe, not on this tablet.", `recovery_code_done` "I've written it down", `enter_owner_pin` "Owner PIN", `wrong_pin_tries_left` (plural, %d), `pin_locked_until` "Too many tries. Try again in %1$s", `forgot_pin` "Forgot PIN?", `recovery_code` "Recovery code", `new_pin` "New PIN", `change_pin` "Change PIN", `current_pin` "Current PIN", `owner_mode` "Owner mode", `lock` "Lock", `pins_dont_match` "The PINs don't match", `pin_must_be_4_digits` "The PIN must be 4 digits", `name_required` "Enter a name".

- [ ] **Step 1: Write the failing VM tests.**
  - `RootViewModelTest`:
    - `noOwner_showsSetup`
    - `setUp_pinMismatch_returnsError`
    - `setUp_success_keepsSetupUntilCodeAcknowledged`: right after setup the screen is SETUP and the code is non-null; after `acknowledgeRecoveryCode()` the screen is APP.
    - `existingOwner_showsApp`
  - `OwnerPinViewModelTest`:
    - `submit_correct_unlocksSessionAndCallsOk`
    - `submit_wrong_setsWrongError_keepsLocked`
    - `resetWithCode_mismatch_doesNotCallRepo`

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement the VMs and composables.** In `MainActivity`, `setContent` switches on `RootViewModel.screen`:
  - LOADING: a plain background
  - SETUP: `OwnerSetupScreen`
  - APP: `MainNavigation(ownerSession)`

  `MainNavigation` creates `remember { OwnerGate(ownerSession) }`, provides `LocalOwnerGate`, and shows `OwnerPinDialog` while `gate.pending != null` (calling `gate::onVerified` / `gate::onDismissed`).

- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS, including `StringResourcesTest`.

- [ ] **Step 5: Manual check on `qahwa-api34`.** On a fresh install the setup screen appears. Set it up, see the code, acknowledge, and the app opens. Repeat in Arabic and confirm the layout is RTL. Take a screenshot of each step for the final report.

- [ ] **Step 6: Commit.** `feat(auth): Add owner setup, PIN dialog with recovery, and owner mode banner`

---

### Task 8: Waiter shift flow: start, take over, collected so far, end shift

**Files:**
- Create: `ui/shift/ShiftViewModel.kt`, `ui/shift/ShiftCard.kt`, `ui/shift/EndShiftScreen.kt`
- Modify:
  - `ui/home/HomeScreen.kt`: the shift card goes above the stats. The revenue stat shows only in owner mode.
  - `ui/orders/NewOrderScreen.kt`: show `ShiftCard` instead of the cart when no shift is active.
  - `ui/orders/OrdersListScreen.kt`: same for the New order button.
  - `ui/orders/OrderDetailScreen.kt`: *Cancel order* goes through `LocalOwnerGate.current.require { … }`. *Mark as paid* is disabled when no shift is active, with the `no_active_shift` hint.
  - `NavigationKeys.kt` (+ `EndShift`), `Navigation.kt` (entry), and strings ×3
- Test: `app/src/test/.../ui/shift/ShiftViewModelTest.kt`

**Interfaces:**
- Consumes: `ShiftRepository`, `StaffRepository` (Task 5), `OrderRepository` (Task 6), `OwnerSession` (Task 4).
- Produces:
  - `ShiftViewModel`:
    - `activeShift: StateFlow<ShiftWithStaff?>`
    - `activeStaff: StateFlow<List<Staff>>`
    - `openOrders: StateFlow<List<Order>>`
    - `collectedCentimes: StateFlow<Long>` (0 when no shift is active)
    - `isOwnerMode: StateFlow<Boolean>`
    - `fun start(staffId: Long)`, `fun takeOver(staffId: Long)`, `fun endShift(onDone: () -> Unit)`, `fun markPaid(orderId: Long)`
    - All writes go through one save guard, like `OrdersViewModel.save`.
  - `@Composable fun ShiftCard(onEndShift: () -> Unit)`:
    - With no shift: "Start shift" plus a name button for each active staff member.
    - With a shift: "Karim · since 08:00 · collected 340.00 MAD", an *End shift* button, and a *Not Karim? Take over* link. The link opens a name picker, then a confirm dialog: "Karim's shift will end now. His N open tables will be listed as left open."
  - `@Serializable data object EndShift : NavKey`
  - `EndShiftScreen(onBack)`: lists the open orders, each with *Mark paid*, then *End shift*. With open orders remaining, *End shift* asks for confirmation: "N tables (X MAD) will be left open for the owner."
  - Copy (English; ×3): `start_shift` "Start shift", `who_is_working` "Who's working?", `shift_since` "%1$s · since %2$s", `collected_so_far` "Collected so far", `end_shift` "End shift", `take_over` "Not %1$s? Take over", `take_over_confirm` (plural, %1$s name, %2$d tables), `collect_open_tables` "Collect from these tables first", `leave_open_confirm` (plural, %1$d tables, %2$s amount), `no_active_shift` "Start a shift to take orders".

- [ ] **Step 1: Write the failing tests** in `ShiftViewModelTest`:
  - `start_thenActiveShiftShowsStaffName`
  - `start_doubleTap_createsOneShift`
  - `collected_tracksPaidOrdersInShift`: mark two orders paid (800 and 1200) and expect 2000.
  - `endShift_withOpenOrders_recordsLeftOpenAndCallsDone`
  - `takeOver_endsPreviousAndStartsNew`

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement the VM, the card, the screen and the screen changes listed above.**

- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.

- [ ] **Step 5: Manual check on the emulator:**
  - Start a shift and place two orders. Pay one; *collected so far* updates.
  - Cancel on the other asks for the PIN.
  - End shift asks for confirmation about the open table.
  - Start the next shift. Take over from a third shift.
  - Kill the app (`adb shell am kill com.qahwa.manager`) and relaunch: the shift is still active, and owner mode is off.

- [ ] **Step 6: Commit.** `feat(shifts): Add waiter shift card, take-over and end-of-shift collection`

---

### Task 9: Owner area: Owner tab, shifts to settle, settlement, history, staff

**Files:**
- Create: `ui/owner/OwnerHomeScreen.kt`, `ui/owner/ShiftsScreen.kt`, `ui/owner/ShiftsViewModel.kt`, `ui/owner/ShiftReportScreen.kt`, `ui/owner/ShiftReportViewModel.kt`, `ui/owner/StaffScreen.kt`, `ui/owner/StaffViewModel.kt`
- Modify:
  - `NavigationKeys.kt`: add `Owner`, `Shifts`, `ShiftReport(shiftId: Long)` and `StaffList`. `Sales` stays as a pushed entry.
  - `Navigation.kt`:
    - The fifth tab becomes `Owner`, with the `Icons.Default.AdminPanelSettings` icon and the string `owner`. Clicking it goes through `gate.require`.
    - Add a `LaunchedEffect` keyed on owner mode. When owner mode turns off while the back stack root is `Owner`, it resets the stack to `Home`.
  - Strings ×3
- Test: `app/src/test/.../ui/owner/ShiftsViewModelTest.kt`, `ShiftReportViewModelTest.kt`, `StaffViewModelTest.kt`

**Interfaces:**
- Consumes: `ShiftRepository`, `StaffRepository`, `OwnerSession`, `OwnerGate`, `ChangePinDialog`, the existing `SalesScreen`.
- Produces:
  - `OwnerHomeScreen(onSales, onShifts, onStaff)`: rows for *Sales*, *Shifts* (with a badge showing the to-settle count), *Staff* and *Change PIN* (opens `ChangePinDialog`).
  - `ShiftsViewModel(shiftRepository, staffRepository)`:
    - `toSettle: StateFlow<List<ShiftWithStaff>>`
    - `history: StateFlow<List<ShiftWithStaff>>`
    - `staff: StateFlow<List<Staff>>`
    - `selectedStaffId: StateFlow<Long?>`, `fun selectStaff(id: Long?)`
    - `totalDifferenceCentimes: StateFlow<Long>` (the sum of `differenceCentimes` over `history`)
  - `ShiftReportViewModel(shiftRepository, ownerSession)`:
    - `fun report(shiftId: Long): Flow<ShiftReport?>`
    - `countedInput: StateFlow<String>`, `fun setCountedInput(text: String)`
    - `countedCentimes: StateFlow<Long?>` (`parseMadToCentimes`)
    - `isSaving: StateFlow<Boolean>`
    - `fun settle(shiftId: Long, onDone: () -> Unit)`. Does nothing if `countedCentimes` is null, owner mode is off, or a save is in flight.
  - `ShiftReportScreen(shiftId, onBack)`:
    - Shows the spec's settlement layout: orders collected, expected, the counted field, Short/Over/Exact, cancelled count and sum, left open (with each table and amount), wrong owner-PIN attempts, and "Ended by takeover" when it applies.
    - For an ENDED shift the counted field and *Confirm settlement* are enabled. *Confirm settlement* goes through `gate.require`.
    - For a SETTLED shift everything is read-only.
    - Below that, the list of collected orders.
  - `StaffViewModel(staffRepository, ownerSession)`: `staff`, `fun add(name)`, `fun rename(id, name)`, `fun setActive(id, active)`. These do nothing unless in owner mode and surface `IllegalArgumentException` as `error: StateFlow<StaffError?>`.
  - Copy (English; ×3): `owner` "Owner", `shifts` "Shifts", `to_settle` "To settle", `history` "History", `all_staff` "Everyone", `settle_title` "Settle: %1$s", `orders_collected` "Orders collected", `expected_cash` "Expected cash", `counted_cash` "Counted", `short_by` "Short %1$s", `over_by` "Over %1$s", `exact` "Exact", `confirm_settlement` "Confirm settlement", `cancelled_by_owner` "Cancelled (owner)", `left_open` "Left open", `wrong_pin_attempts` "Wrong owner-PIN attempts", `ended_by_takeover` "Ended by takeover", `staff` "Staff", `add_waiter` "Add waiter", `rename` "Rename", `deactivate` "Deactivate", `reactivate` "Reactivate", `running_difference` "Total difference", `nothing_to_settle` "No shifts waiting to be settled".

- [ ] **Step 1: Write the failing tests.**
  - `ShiftsViewModelTest`:
    - `toSettle_oldestFirst`
    - `history_filterByStaff`
    - `totalDifference_sumsFilteredHistory` (−3000 and +500 give −2500)
  - `ShiftReportViewModelTest`:
    - `countedInput_frenchCommaParses` (`"610,50"` gives 61050)
    - `settle_refusesInvalidInput` (`""` and `"abc"` leave the shift ENDED)
    - `settle_locked_doesNothing`
    - `settle_ownerMode_settlesAndCallsDone`
    - `settle_ignoresSecondTapWhileSaving`
  - `StaffViewModelTest`:
    - `add_locked_doesNothing`
    - `add_blank_setsError`
    - `deactivateOwner_setsError`

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement the VMs, the screens and the navigation changes.**

- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.

- [ ] **Step 5: Manual check on the emulator:**
  - Tapping the Owner tab asks for the PIN.
  - Settle an ended shift with `610,50`. Short/Over is correct and it moves to History.
  - Filter History by waiter.
  - Add, rename and deactivate a waiter. The deactivated waiter disappears from Start shift.
  - Lock owner mode while on an Owner screen and you return to Home.
  - Wait 2 minutes idle in owner mode and it locks itself.
  - Check everything in Arabic RTL.

- [ ] **Step 6: Commit.** `feat(owner): Add owner tab with shift settlement, history and staff management`

---

### Task 10: Menu and inventory edits are owner-only

**Files:**
- Modify: `ui/menu/MenuViewModel.kt`, `ui/menu/MenuScreen.kt`, `ui/inventory/InventoryViewModel.kt`, `ui/inventory/InventoryScreen.kt`
- Test: `app/src/test/.../ui/menu/MenuViewModelTest.kt` (new), `app/src/test/.../ui/inventory/InventoryViewModelTest.kt` (extend)

**Interfaces:**
- Consumes: `OwnerSession`, `LocalOwnerGate`.
- Produces:
  - `MenuViewModel(menuRepository, ownerSession)`: `addItem`, `updateItem` and `deleteItem` do nothing unless in owner mode. `toggleAvailability` stays open to everyone.
  - `InventoryViewModel(inventoryRepository, ownerSession)`: `addItem`, `updateItem`, `adjustQuantity` and `deleteItem` do nothing unless in owner mode.
  - UI: the add/save button in each dialog, delete confirms and quantity steppers call `gate.require { viewModel.x(...) }`. The availability switch doesn't.

- [ ] **Step 1: Write the failing tests.**
  - `MenuViewModelTest`:
    - `toggleAvailability_locked_works`
    - `addItem_locked_doesNothing`
    - `addItem_ownerMode_adds`
    - `deleteItem_locked_doesNothing`
  - `InventoryViewModelTest`:
    - `adjustQuantity_locked_doesNothing`
    - `adjustQuantity_ownerMode_adjusts`

  Update the existing inventory tests to unlock the session first.

- [ ] **Step 2: Run them and see them fail.**

- [ ] **Step 3: Implement the VM and UI changes.**

- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest`. Expected: PASS.

- [ ] **Step 5: Commit.** `feat(auth): Make menu and stock edits owner-only, keep availability toggle open`

---

### Task 11: Full verification and README

**Files:**
- Modify: `README.md` (features: "Staff, shifts & cash settlement"; how owner mode works; known limits from the spec)

- [ ] **Step 1: Run every unit test.** `./gradlew :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run every instrumented test on `qahwa-api30`.** `./gradlew :app:connectedDebugAndroidTest`. Expected: every test in the XML results passes (0 failures).

- [ ] **Step 3: Run the spec's manual checklist on `qahwa-api34`.** Include an upgrade test: install the current `main` debug build, create orders, then install this branch's debug build over it. The owner setup appears, and the old orders are still in Sales. Then go through fresh install, a full shift (start → orders → end → settle), wrong-PIN lockout (5 tries, 1-minute wait shown), the recovery code, and Arabic RTL on every new screen. Save screenshots in the scratchpad and list them in the final report.

- [ ] **Step 4: Update the README, then commit.** `docs: Document staff shifts, owner mode and known limits`

- [ ] **Step 5: Push the branch and open a PR** to `main` (`gh pr create`). The PR body summarizes the changes, the manual-test evidence and the known limits, and ends with the attribution line. **The user reviews and merges.**
