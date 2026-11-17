# Staff shifts & cash settlement — design

Date: 2026-11-17 · Status: draft for approval · Branch: `feat/staff-shifts`

## Why

Qahwa Manager works today as a single-person POS: anyone holding the tablet can take orders,
mark them paid, cancel them, change prices and see the day's revenue, and nothing records who
did what. That makes the app hard to sell. A café owner's main worry is cash going missing
between the customer and the till, and the app does nothing about it.

This feature is the first of three that should make the app sellable (staff & cash control,
then receipt printing, then backup). It gives the owner one number per shift: **how much cash
the waiter should hand over, how much they did hand over, and the difference**, kept as a history
per waiter.

## How the café works (facts from the owner's side, as told by the user)

1. The café has **one tablet**, shared at the counter.
2. There is **one waiter per shift**.
3. Each waiter collects cash from their own tables and **settles with the owner at the end of
   the shift**.
4. At the end of the shift the waiter **collects from every table still open**, even if the
   customers stay seated. Anything those customers order afterwards is a new order for the next
   waiter.

Everything else below is a proposal from the design conversation, approved by the user as a
whole ("yes, write the design"). Calls made only while writing this document are marked
*(builder's call)* or *(my call)*.

## Before this work: one permanent signing key (confirmed by the user, do first)

The release workflow (`.github/workflows/release.yml`) runs `assembleDebug` on a fresh CI
machine, so every release is signed with a new throwaway debug key. v1.0.0 and v1.0.1-53fbea2
have different certificates (SHA-256 `1ad7bedf…` and `e80e0f11…`). Android refuses to install a
version over one signed with a different key, so every update so far has meant an uninstall,
which wipes the café's data. No café can be sold an app that can't update.

- The user creates one release keystore and keeps it safe, backed up off the laptop. If it's
  lost, no future update can install over an existing install.
- The keystore and its passwords go into GitHub secrets. The workflow builds a **signed release
  APK** from them.
- The release build has R8 minification switched on (`isMinifyEnabled = true`), and CI has never
  built it. The build must be checked on an emulator, because Room, Hilt and kotlinx.serialization
  can break under shrinking.
- This is a separate small change on its own branch and PR, merged before the shifts work. It
  touches the `main` release workflow, so the user reviews it before merge.
- Test devices with a 1.0 build need one last uninstall to switch to the permanent key.

## Goals

- Every order belongs to a shift, and every shift belongs to one named waiter.
- At the end of a shift the owner sees the expected cash, types the counted cash, and the
  difference is saved permanently.
- The owner can look back at each waiter's history of settlements and spot patterns.
- Only the owner can do the things that make cash disappear or hide it: cancel orders, change
  the menu and prices, edit stock, see café-wide revenue, manage staff and settle shifts.
- The waiter's work during service gets **no slower** than today.

## Non-goals (this project)

- Receipt printing and backup. They're the next two projects and get their own designs.
- Cloud sync, multiple tablets, the owner viewing sales remotely.
- Waiter PINs (see Approaches).
- A starting float (change given to the waiter at the start of a shift). Waiters bring their own.
- Comparing stock used against sales (would catch orders never typed in). That's a later feature.
- Editing an order's items after it's placed. That isn't possible today and stays that way.
- Kiosk lockdown. A waiter could still clear the app's data from Android settings; this is a
  known limit (see Known limits).

## Anti-goals (failure even if it "works")

- The waiter has to type a PIN or tap extra screens to take or collect an order.
- The café is **blocked** because the owner isn't there: the next waiter must always be able to
  start, even if the previous shift hasn't been settled.
- Upgrading from 1.0 loses or changes any existing order, menu item or stock item.
- The owner gets locked out of their own data for good because they forgot their PIN.

## Constraints

- Offline only, single device, no new permissions.
- Room database upgrade **v2 → v3**, additive only (new tables, new nullable columns).
  Export `app/schemas/.../3.json` and add a 2→3 case to `MigrationTest`.
- Money stays in integer centimes (`Long`). The counted-cash field reuses `DecimalInput` / `Money`.
- All new text exists in **English, French and Arabic**, and the screens work right-to-left.
  `StringResourcesTest` fails the build otherwise.
- minSdk 24. PIN hashing uses `javax.crypto` (PBKDF2), which works on API 24. No new crypto
  libraries.
- The look follows the existing `QahwaComponents` and both themes.
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. **Never push to `main`.** Every push
  to `main` builds an APK and publishes a public GitHub Release (commit 53fbea2).

## Approaches considered

**How identity works on a shared tablet.** Before we knew there was one waiter per shift:

1. *PIN on every money action:* a number pad on each *Place order* / *Mark paid*; the PIN says
   who did it. The most secure option, but it only guards against a second waiter acting under
   someone else's name.
2. *Log in once, auto-lock after ~1 min idle:* faster, but on a busy tablet people act in each
   other's sessions.
3. *Tap your name, no PIN:* fastest, but proves nothing on its own.

Once the user said **one waiter per shift**, the "someone else acts under your name" risk mostly
went away, and the chosen design became **shifts with an owner-only PIN**. The waiter picks their
name once to start the shift (option 3's speed). The only PIN guards the owner's actions, which
is where the remaining theft trick lives: ring up an order, pocket the cash, cancel it.

**Tables still open when a shift ends.** Options were: hand them to the next waiter, block
settlement until they're paid, or leave them alone. The user's answer settled it: the waiter
collects from every open table at the end of the shift. The design follows that, with an escape
hatch for the case where a table really can't be collected (see "Ending a shift").

## The design

### People and the owner PIN

- **First launch** (fresh install, or first launch after upgrading from 1.0) opens an **owner
  setup** screen: the owner's name, then a 4-digit PIN entered twice. The app then shows a
  **recovery code** once (8 characters, *my call*) and asks the owner to write it down. Nothing
  else in the app is usable until setup is done.
- The owner appears in the staff list like a waiter, so they can **start a shift themselves** on
  days they run the café alone.
- **Staff screen** (owner only): add a waiter (name only), rename, deactivate. Deactivated
  people disappear from the start-shift list but keep their history. The owner's own entry can't
  be deactivated. Nobody is ever deleted.
- **PIN storage:** salted PBKDF2 hash, never the PIN itself. Same for the recovery code. Where it
  lives (a DB table or SharedPreferences) is the *builder's call*.
- **Wrong PIN:** after 5 wrong tries in a row, the PIN pad locks for 1 minute, doubling on each
  further failure (*my call*; exact numbers are the builder's call). The lockout survives the app
  being killed. **Every failed owner-PIN attempt is counted on the active shift** and shown in
  that shift's report. A waiter guessing the owner's PIN is itself a theft signal. Failed
  attempts with no shift running still count toward the lockout.
- **Hash algorithm:** `PBKDF2WithHmacSHA1` with a high iteration count, because the SHA-256
  variant needs API 26 and minSdk is 24 *(my call)*.
- **Forgot PIN:** "Forgot PIN?" on the PIN pad → enter recovery code → set a new PIN → a new
  recovery code is shown.
- **Change PIN:** in the owner area.

### Owner mode

- Entering the owner PIN turns on **owner mode**: a visible banner ("Owner mode · Lock") at the
  top of every screen.
- Owner mode is held **in memory only** and ends when the app goes to the background, after
  2 minutes without a touch, or when the owner taps *Lock* (*my call* on the 2 minutes).
- Any owner-only action tapped outside owner mode opens the PIN pad, and on success does the
  action and turns owner mode on.

### Who can do what

| Area | Waiter (no PIN) | Owner (owner mode) |
|---|---|---|
| Home | current shift card: waiter name, start time, orders collected, **cash collected so far**; open tables; low-stock banner | plus today's café revenue |
| Orders | place orders, mark paid, view orders | plus **cancel an order** |
| Menu | view, **toggle an item available/unavailable** (it ran out) | add, edit, delete, change prices |
| Inventory | view stock and low-stock warnings | add, edit, delete, adjust quantities |
| Owner tab (replaces *Sales*) | — (tapping it asks for the PIN) | Sales (as today), Shifts, Staff, Change PIN |

Calls in this table that weren't discussed with the user: waiters can toggle availability; stock
edits are owner-only; the *Sales* tab becomes an *Owner* tab holding Sales, Shifts and Staff, so
the bottom bar stays at five tabs. The owner gate goes in the tab click handler in
`Navigation.kt`, which today just clears the back stack and adds the tab.

### Shifts

A shift is **Active → Ended → Settled**. It is stored in the database, so it survives the app
being killed or the tablet restarting.

- **Only one Active shift at a time**, enforced in the database layer (a DAO `@Transaction`), not
  just the UI.
- **Starting:** with no active shift, Home, Orders and New Order show a *Start shift* card: tap
  your name from the active staff list. No PIN.
- **Placing an order or marking one paid requires an active shift.** With no shift active, those
  actions show the *Start shift* card instead.
- **Ending a shift** (waiter, no PIN). The *End shift* screen lists every open order first, with
  *Mark paid* on each, because the waiter collects from all of them. When none are left, *End
  shift* ends it. The next waiter can start right away; settling can happen later.
  - *Escape hatch (confirmed by the user):* if a table can't be collected (a customer left
    without paying, say) and the owner isn't there to cancel it, the waiter can still end the
    shift after a confirmation. Those orders stay open and appear on the settlement as **"left
    open"** for the owner to deal with. They don't count toward the waiter's expected cash. The
    owner then cancels them (the café's loss) or leaves them open for whoever collects later. If
    the owner wants to hold the waiter responsible, that goes into the counted amount they agree
    on. The user accepted the known loophole: a waiter can pocket a table's cash and call it a
    walkout. Every such table is named on the report, so the owner sees it.
  - **The left-open list is recorded at the moment the shift ends** (which orders, and their
    totals at that time). That way a settled report never changes when one of those orders is
    paid or cancelled later. This needs its own storage, for example a small
    `shift_left_open(shiftId, orderId, totalCentimes)` table. The exact shape is the builder's
    call, but it must be part of the 2→3 migration.
- **A shift nobody ended** *(my call)*. If the previous waiter went home without ending their
  shift, the next waiter sees "Karim's shift is still open" with a *Take over* button (no PIN).
  Taking over ends Karim's shift without collecting, so his still-open tables are recorded as left
  open on his report. The shift is marked **"ended by takeover"**, with the time, so the owner
  sees it wasn't Karim who ended it. The new waiter's shift then starts. Anything they collect
  from those tables counts toward their own expected cash, because they're the one holding the
  money. This keeps the café from ever being blocked.

### Which shift a sale counts toward

- Each order records the shift it was **opened** in and the shift it was **paid** in, plus
  `paidAt` and `cancelledAt` timestamps.
- **Expected cash for a shift = the sum of `totalCentimes` of orders marked paid during that
  shift.** Whoever collects the money owes it. This also handles orders left open across a shift
  change and orders that were open during the 1.0 upgrade (no opened-in shift, but they get a
  paid-in shift when collected).
- **The owner never takes a customer's money during a waiter's shift** (confirmed by the user).
  So every payment marked during a shift counts toward that shift's waiter, even when it's marked
  in owner mode. No "collected by" field is needed.
- Cancelling is unchanged in substance: only OPEN orders can be cancelled (as today), and every
  cancel now needs owner mode. A cancelled order records `cancelledAt`. **It shows on the report
  of the shift it was opened in** *(my call)*. That way a walkout left open by Karim and cancelled
  by the owner the next day still lands on Karim's report. Orders from before the upgrade have no
  opened-in shift, so theirs show on the shift that was active when they were cancelled.

### Settling

Owner only. Owner tab → **Shifts** → "To settle" lists ended, unsettled shifts, oldest first.

```
Settle: Karim                    Mon 17 Nov · 08:00 – 15:04
───────────────────────────────────────────────────────────
Orders collected                       23
Expected cash                     640.00 MAD
Counted                         [ 610.00 ]
───────────────────────────────────────────────────────────
Short                              −30.00 MAD
Cancelled (owner)            1 order · 36.00 MAD
Left open                    0
Wrong owner-PIN attempts     0
                                  [ Confirm settlement ]
```

- On confirm, the expected amount is **copied onto the shift** (so later changes can't move it),
  along with counted, difference and `settledAt`. A settlement is **final**: no editing (*my
  call*, keeps the history trustworthy).
- A counted amount above expected shows as **"Over +X MAD"**.

### Shift history

Owner tab → **Shifts** → "History": settled shifts newest first, each showing date, waiter,
expected, counted and difference. You can filter by waiter. Each waiter's filtered view shows
their running total of differences. Tapping a shift opens its full report: the settlement figures,
the list of orders collected, cancellations, orders left open, failed owner-PIN attempts, and
whether the shift was "ended by takeover".

### Upgrading from 1.0

- **No café runs 1.0 with real data** (confirmed by the user). Only test devices are affected. The
  migration is still written and tested properly, because Room requires it and the next projects
  will need the same discipline.
- Migration 2→3 adds the `staff` and `shifts` tables and the new nullable columns on `orders`. It
  doesn't touch existing values.
- Old orders have no shift. Sales still counts them as today. Shift reports simply don't include
  them.
- On first launch after the upgrade, owner setup runs (see above) before anything else.

### Data model sketch (*builder may adjust names*)

- `staff`: id, name, isOwner, isActive, createdAt
- `shifts`: id, staffId → staff, status (ACTIVE / ENDED / SETTLED), startedAt, endedAt?,
  endedByTakeover (default false), settledAt?, expectedCentimes?, countedCentimes?,
  failedOwnerPinAttempts (default 0)
- `shift_left_open`: shiftId → shifts, orderId → orders, totalCentimes, recorded when the
  shift ends
- `orders` + openedShiftId?, paidShiftId?, paidAt?, cancelledAt?
- Owner credentials and lockout state: hashes + salts + failure count + locked-until time
  (*storage is the builder's call*).

## Testing

- Unit tests (JVM, fakes in `testing/Fakes.kt`): expected-cash calculation, one-active-shift rule,
  ending with and without open orders, settlement snapshot and over/short, PIN hashing and
  verify, lockout timing (using the existing `Clock`), recovery-code reset, owner-mode timeout,
  waiter-vs-owner permissions in each ViewModel. `OrdersViewModelTest` and other existing tests
  need their constructors and fakes updated.
- Instrumented: DAO tests for the new tables and the single-active-shift transaction; `MigrationTest`
  2→3 proving existing orders, menu and stock survive unchanged.
- `StringResourcesTest` passes with the new strings in all three languages.
- A manual run on the `qahwa-api30` / `qahwa-api34` emulators: fresh install, upgrade from a 1.0
  database, a full shift (start → orders → end → settle), wrong-PIN lockout, recovery code, and
  Arabic RTL on every new screen.

## Known limits (say these honestly when selling)

- Orders the waiter never types in can't be caught. That needs the future stock-vs-sales check.
- Someone with the tablet could clear the app's data from Android settings and wipe everything,
  including the history. Backup (project 3) reduces the damage, and kiosk lockdown would prevent
  it.
- The tablet's clock is trusted.

## Left to the builder

Exact screen layouts within the existing design system; where credentials are stored; exact
lockout timings; table and column names; how the owner banner and PIN pad look; and how the new
screens are split into ViewModels.
