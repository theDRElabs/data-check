# DataCheck M6 — Fresh-Context Review Findings
> **Note, 2026-10-03.** This is a read-only review snapshot from M6, kept
> verbatim as the record it was meant to be. One locked item below - the
> "committed keystore" - was later exposed and revoked, and that decision has
> since been reversed. See `README.md` for the current release-signing model.

Read-only review of the M1–M5 Kotlin sources, produced for user triage (ISSUE-004).
No code was changed. Every citation was read during this pass.

**Scope covered:** all in-scope sources under
`app/src/main/java/com/drelabs/datacheck/` — `data/SamplingEngine.kt`,
`data/Attribution.kt`, `data/Validation.kt`, `data/Prefs.kt`, `data/CsvExporter.kt`,
`work/Scheduler.kt`, `work/TickWorker.kt`, `work/BootReceiver.kt`,
`notify/PingNotifier.kt`, `ui/dashboard/DashboardScreen.kt`,
`ui/settings/SettingsScreen.kt`, `ui/onboarding/OnboardingScreen.kt`,
`ui/onboarding/UsageAccess.kt`, `MainActivity.kt`, `DataCheckApp.kt`, all five
`data/db/` files, `util/Format.kt`, `util/AppLabels.kt`, `AndroidManifest.xml`,
plus `res/xml/file_paths.xml`, `app/build.gradle.kts` (versions), `AGENTS.md` /
`PROJECT.md` (to avoid reporting documented intent), and the test file listing.

**Method:** sequential full-file reads (engine → work/notify → db → ui → app →
config), lenses: correctness, silent failure, data integrity, on-device
robustness, test gaps hiding correctness risk.

**Counts:** 13 findings — 1 high, 6 medium, 6 low.

**Verification limits:** no local JDK/SDK (per AGENTS.md) — no compile, no DAO
test, no runtime check. One finding (F-01) depends on Room codegen behavior and
is flagged as needing a one-test confirmation.

---

## Findings

### F-01 — HIGH — Suspected crash: `SUM()` over empty table mapped into non-null fields

**Files:** `app/src/main/java/com/drelabs/datacheck/data/db/UsageLogDao.kt:37-40`,
`.../data/db/UsageLogDao.kt:10`, callers `.../ui/dashboard/DashboardScreen.kt:282`
and `.../work/TickWorker.kt:29`

**Evidence:**

```kotlin
data class TotalsRow(val total: Long, val fgTotal: Long)          // :10 — non-null Longs

@Query(
    "SELECT SUM(rx + tx) AS total, SUM(fgRx + fgTx) AS fgTotal FROM usage WHERE tickStart >= :sinceMs",
)
suspend fun totalsSince(sinceMs: Long): TotalsRow?                // :37-40
```

**Why it matters:** a SQL aggregate without GROUP BY returns exactly one row,
with NULL columns when no rows match. Room 2.6.1 (`app/build.gradle.kts:72-74`)
maps that NULL row into the non-null `Long` fields and throws — a recognized
Room pitfall for SUM queries. The empty-table path is plausible and recurring:

- First ever launch: the first `runTick` takes the baseline branch and inserts
  nothing (`SamplingEngine.kt:94-97`: `last == 0L` → set pref, return `null`),
  then `loadDashboard` calls `totalsSince(midnight)` on an empty `usage` table.
- Every day from 00:00 local until the first tick of the day completes
  (~15 min), the filter `tickStart >= midnight` matches nothing.

In the dashboard this is an uncaught exception in `LaunchedEffect`
(`DashboardScreen.kt:67-70`) → app crash. In `TickWorker.postPing`
(`TickWorker.kt:27-29`) it is swallowed by `doWork`'s catch-all
(`TickWorker.kt:19-21`) into a retry-then-success loop — the ping silently
never posts.

**Caveat:** I could not run a DAO test here (CI-only builds, no local JDK per
AGENTS.md). Confirm the exact exception type with one instrumented/Robolectric
DAO test against an empty table before fixing — the fix is trivial either way.

**Suggested fix (minimal):**

```sql
SELECT IFNULL(SUM(rx + tx), 0) AS total, IFNULL(SUM(fgRx + fgTx), 0) AS fgTotal
FROM usage WHERE tickStart >= :sinceMs
```

(or make `TotalsRow.total/fgTotal` nullable and keep `?: 0L` at call sites).

---

### F-02 — MEDIUM — Windows crossing midnight are attributed wholly to the previous day

**Files:** `.../data/db/UsageLogDao.kt:38, 43, 31-33` (all "since" queries key on
`tickStart`), `.../data/SamplingEngine.kt:70-75`, consumers
`.../ui/dashboard/DashboardScreen.kt:282-286` and `.../work/TickWorker.kt:28-29`

**Evidence:**

```kotlin
// UsageLogDao.kt:38
"SELECT SUM(rx + tx) AS total, ... FROM usage WHERE tickStart >= :sinceMs"
// SamplingEngine.kt:71-73 — NSM raw total measured from true midnight
val midnight = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
val device = queryDeviceTotal(midnight, nowMs) ?: return null
```

**Why it matters:** a tick window [23:50, 00:05) stores `tickStart` yesterday,
so *all* its bytes — including the 15 minutes after midnight — are excluded
from "today" in the dashboard card, the notification's "Mobile today", top-apps,
the bundle cycle usage, and the 7-day history. Meanwhile the validation card's
NSM figure starts at true midnight, so every morning the card shows a
systematically positive delta equal to the overnight tick's post-midnight
bytes — directly undermining the M6 accuracy goal (AGENTS.md targets ±1% over
a day; one 15-min window can exceed that on light-usage days). The
`rawMobileTotalTodayBytes` midnight math itself is correct; the mismatch is in
what "logged today" counts.

**Suggested fix (minimal, respects the window model):** never let a tick span
midnight — in `SamplingEngine.resolveWindowStart`, clamp the start forward to
the last local midnight when the previous window crosses it:

```kotlin
// in resolveWindowStart, after resolving `start`:
val midnight = /* local midnight of nowMs */
return if (start < midnight && nowMs > midnight) midnight else start
```

(NSM happily answers the shorter window; the overnight tail then lands in the
first tick of the new day.) Alternative: prorate rows at query time via
`Attribution.overlapMs` against [midnight, now).

---

### F-03 — MEDIUM — Retention purge predicates disagree: usage rows dropped while their tick survives

**Files:** `.../data/db/UsageLogDao.kt:62-72`

**Evidence:**

```kotlin
@Query("DELETE FROM ticks WHERE endMs < :cutoffMs")
suspend fun deleteTicksBefore(cutoffMs: Long)

@Query("DELETE FROM usage WHERE tickStart < :cutoffMs")
suspend fun deleteUsageBefore(cutoffMs: Long)
```

**Why it matters:** a tick that *spans* the 30-day cutoff (`tickStart <
cutoff ≤ endMs` — one exists every day) has its usage rows deleted (predicate
on `tickStart`) while the tick row survives (predicate on `endMs`). Result: an
orphaned tick with `deviceRx/deviceTx` but no per-app rows, and one window of
per-app data silently dropped — a "row dropped outside the documented
boot-reconciliation exception" (AGENTS.md:34-35 allows no other mutation of
past rows).

**Suggested fix (minimal):** delete usage via the ticks they belong to, in the
same transaction:

```kotlin
@Query("DELETE FROM usage WHERE tickId IN (SELECT id FROM ticks WHERE endMs < :cutoffMs)")
suspend fun deleteUsageBefore(cutoffMs: Long)
```

---

### F-04 — MEDIUM — Window bookkeeping is not atomic: overlapping ticks can double-count

**Files:** `.../data/SamplingEngine.kt:91-105` (read-then-write of
`prefs.lastTickEndMs`), `.../ui/dashboard/DashboardScreen.kt:265-272`
(UI catch-up tick), `.../data/SamplingEngine.kt:84-87` (pref written after DB
insert), `.../data/db/UsageLogDao.kt:27-28` (`latestEndMs()` — dead code)

**Evidence:**

```kotlin
// SamplingEngine.kt:91-104 — resolveWindowStart reads and writes the pref
val last = prefs.lastTickEndMs
return when { ... nowMs <= last -> null; else -> last }
// DashboardScreen.kt:269-271 — UI path, concurrent with WorkManager
if (last == 0L || now - last >= 10 * 60_000L) {
    SamplingEngine(context).runTick(now)
}
// SamplingEngine.kt:85-87 — DB insert happens before the pref is advanced
dao.insertTickWithUsages(tick, rows)
...
prefs.lastTickEndMs = end
```

**Why it matters:** two failure modes, same root cause — the window start
lives in SharedPreferences, decoupled from the DB:

1. **Concurrent ticks:** the dashboard backfill tick and the WorkManager tick
   can both pass the staleness check and enter `runTick` together; both read
   the same `lastTickEndMs`, both insert a tick covering an overlapping
   window, and the overlap's bytes are counted twice in every `totalsSince` /
   `topAppsSince` sum. Nothing serializes them (no mutex; unique-work name only
   covers the worker).
2. **Crash between DB commit and pref write:** the next tick re-queries from
   the stale pref and re-inserts the same window — same double count.

Notably, `dao.latestEndMs()` (UsageLogDao.kt:27-28) would make the DB the
source of truth for the window start, but it is never called anywhere (grep:
single occurrence, the declaration).

**Suggested fix (minimal):** serialize and re-derive. In `runTick`, take an
app-scoped `Mutex` (or rely on `insertTickWithUsages` being called under it),
and compute the window start as `max(prefs.lastTickEndMs, dao.latestEndMs() ?: 0)`
— the existing dead DAO method — so a crash-after-commit self-heals instead of
duplicating.

---

### F-05 — MEDIUM — NSM/USM failures are swallowed: ticks stop silently, >6 h gaps are lost without a trace

**Files:** `.../data/SamplingEngine.kt:122-126, 135-137, 161-163, 98-101`

**Evidence:**

```kotlin
// SamplingEngine.kt:122-126 — in queryMobilePerUid
} catch (_: SecurityException) {
    return emptyMap()
} catch (_: Exception) {
    return emptyMap()
}
// :135-137 queryDeviceTotal, :161-163 foregroundFractions — same pattern
// :98-101 — the silent clamp
nowMs - last > MAX_WINDOW_MS -> {
    prefs.lastTickEndMs = nowMs - MAX_WINDOW_MS
    nowMs - MAX_WINDOW_MS
}
```

**Why it matters:** when NSM fails (usage access revoked, OEM stats glitch),
`runTick` returns `null`, the window is *not* advanced, and nothing is
recorded — self-healing for gaps ≤ 6 h, but the user sees nothing: no tick, no
ping refresh, and the only error surface is the validation card's Error state,
which is off by default (`Prefs.kt:41-44`, `showValidationCard` default
`false`). Gaps > 6 h are silently clamped away and permanently lost from the
log. For a revoked permission the only recovery path is that onboarding
happens to re-show (see F-11) — accidental, not designed.

**Suggested fix (minimal):** distinguish and surface. On `SecurityException`
set a `permissionLost` pref flag the dashboard reads (mirror the existing
battery card pattern at DashboardScreen.kt:213-231) offering the usage-access
Settings intent; for other failures, record a marker row / set a pref shown as
"logging paused since <time>" in the dashboard. Keep the 6 h clamp, but stop
it from being invisible.

---

### F-06 — MEDIUM — Blocking NSM binder calls and file I/O run on the main thread from the dashboard

**Files:** `.../ui/dashboard/DashboardScreen.kt:67-70, 265-272, 300-309`,
`.../data/SamplingEngine.kt:70-75, 108-128` (blocking queries, no dispatcher),
`.../data/CsvExporter.kt:21-24` (file write), launched at
`.../ui/dashboard/DashboardScreen.kt:234-241`

**Evidence:**

```kotlin
// DashboardScreen.kt:67-69 — LaunchedEffect runs on Dispatchers.Main
LaunchedEffect(Unit) {
    runCatching { backfillTick(context) }
    data = loadDashboard(context)
}
// backfillTick → SamplingEngine.runTick → nsm.querySummary(...) — blocking binder
// DashboardScreen.kt:236 — scope (Main) launches CsvExporter.export,
// whose bufferedWriter().use { ... } (CsvExporter.kt:21-24) writes files inline
```

**Why it matters:** `runTick`'s `querySummary` / `queryDetailsForUid` /
`queryEvents` are synchronous binder round-trips (hundreds of ms, worse under
load — see the `queryDetailsForUid` loop per UID at `SamplingEngine.kt:30-40`);
`rawMobileTotalTodayBytes` (SamplingEngine.kt:70-75) is a plain blocking call
invoked from `loadDashboard`; the CSV export writes the file on the caller's
context. All of these run on the main thread here → frozen frames, and with a
slow NSM the 5-second ANR threshold is reachable. The worker path is fine
(`CoroutineWorker` uses `Dispatchers.Default`).

**Suggested fix (minimal):** wrap the three call sites in
`withContext(Dispatchers.IO) { ... }` — `backfillTick`, the
`rawMobileTotalTodayBytes()` call in `loadDashboard` (or make that function
suspend internally), and `CsvExporter.export`'s write block. No architecture
change needed (AGENTS.md's "UI must not call NSM directly" rule is preserved —
the engine is still the only caller).

---

### F-07 — MEDIUM — Notification permission loss silently disables the product's core output

**Files:** `.../notify/PingNotifier.kt:58-61`, `.../ui/onboarding/OnboardingScreen.kt:29-31, 33-37`

**Evidence:**

```kotlin
// PingNotifier.kt:58-61
try {
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
} catch (_: SecurityException) {
}
// OnboardingScreen.kt:29-31 — notifications state is checked only at composition
var notificationsGranted by remember {
    mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
}
```

**Why it matters:** the ping notification *is* the product (AGENTS.md:13-14).
If the user denies `POST_NOTIFICATIONS` at onboarding time, or revokes it
later from system settings, every `notify()` throws and is swallowed — pings
stop with no in-app signal, forever. The permission is requested exactly once
in onboarding; there is no re-check anywhere (the battery card at
DashboardScreen.kt:213-231 shows the pattern that is missing here).

**Suggested fix (minimal):** in `DashboardScreen`, when
`!NotificationManagerCompat.from(context).areNotificationsEnabled()` (cached
like the battery check), render a card mirroring the battery one, offering to
re-launch the permission request on API 33+ / a link to notification settings
below 33.

---

### F-08 — LOW — Worker swallows CancellationException and reports permanent failure as success

**File:** `.../work/TickWorker.kt:14-21`

**Evidence:**

```kotlin
override suspend fun doWork(): Result = try {
    ...
    Result.success()
} catch (t: Throwable) {
    if (runAttemptCount < 3) Result.retry() else Result.success()
}
```

**Why it matters:** two distinct issues. (1) `catch (t: Throwable)` also
catches `CancellationException` — when WorkManager stops the worker, the
cancellation is swallowed and `Result.retry()` is returned, fighting the
stop request (standard CoroutineWorker anti-pattern). (2) After 3 failed
attempts the worker returns `Result.success()` — WorkManager's diagnostics
show a permanently healthy worker while ticks have stopped entirely; there is
no failure signal anywhere (compounds F-05).

**Suggested fix (minimal):** rethrow `CancellationException` before the
catch-all (`if (t is kotlinx.coroutines.CancellationException) throw t`), and
return `Result.failure()` on the final attempt (periodic work simply runs
again at the next period; nothing is lost).

---

### F-09 — LOW — `AppLabels` cache is an unsynchronized `HashMap` shared across threads

**Files:** `.../util/AppLabels.kt:6, 16-24`; cross-thread callers
`.../work/TickWorker.kt:31-33` (worker thread) and
`.../ui/dashboard/DashboardScreen.kt:186-189` (main thread)

**Evidence:**

```kotlin
private val cache = HashMap<String, String>()        // :6
...
cache[pkg]?.let { return it }                        // :16 — read
cache[pkg] = label                                   // :20 — write
```

**Why it matters:** the same map is read and mutated from the main thread
(Compose) and from the WorkManager worker's `Dispatchers.Default` thread when
both label the same package concurrently. Unsynchronized `HashMap` under
concurrent write is a data race — lost entries at best, rare
`ConcurrentModificationException`/corrupt buckets at worst.

**Suggested fix (minimal):** `private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()`.

---

### F-10 — LOW — CSV exports accumulate forever in `filesDir/exports`

**File:** `.../data/CsvExporter.kt:18-20`

**Evidence:**

```kotlin
val dir = File(context.filesDir, "exports").apply { mkdirs() }
val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
val file = File(dir, "datacheck_$stamp.csv")
```

**Why it matters:** every export tap writes a new timestamped file; nothing
ever deletes them. On a long-lived sideload install this grows unboundedly
(each file is the full 30-day log). Storage nit, but free to fix.

**Suggested fix (minimal):** before writing, delete all but the newest N
(e.g. 3) files in `dir`, or reuse one stable filename.

---

### F-11 — LOW — Onboarding state is not persisted: re-shown on every cold start

**File:** `.../MainActivity.kt:25, 36-38`

**Evidence:**

```kotlin
var onboarded by remember { mutableStateOf(false) }   // :25 — in-memory only
...
} else {
    OnboardingScreen(onDone = { onboarded = true })   // :36-38
}
```

**Why it matters:** `onboarded` lives only in composition state — it is lost
on process death, rotation, and every cold start, so a fully-onboarded user
sees the onboarding screen (one extra "Continue" tap, since usage access
persists) every launch. It also means `remember` (not `rememberSaveable`)
resets mid-flow on configuration change. No doc in AGENTS.md/PROJECT.md
declares re-showing as intended. (Silver lining: this accident is currently
the only usage-access re-grant path — see F-05.)

**Suggested fix (minimal):** persist a `onboarded` boolean in `Prefs`
(default false), read it as the initial value at `MainActivity.kt:25`, set it
in `onDone`.

---

### F-12 — LOW — "in last window" delta can describe a stale window

**Files:** `.../ui/dashboard/DashboardScreen.kt:314, 103-108`,
`.../data/db/UsageLogDao.kt:48-53`

**Evidence:**

```kotlin
lastWindowDelta = dao.latestTickTotal()?.total ?: 0L,     // :314 — newest tick, any age
...
if (d.lastWindowDelta > 0) {                                // :103 — shown unconditionally
    Text("+${Format.bytes(d.lastWindowDelta)} in last window", ...)
```

**Why it matters:** `latestTickTotal` returns the most recent tick regardless
of when it ended. Opening the app in the morning before the first tick of the
day (or after logging has silently stopped, F-05) presents yesterday's window
delta as "last window" — a wrong figure, if a small one.

**Suggested fix (minimal):** also select `endMs` in `latestTickTotal` (or use
`MAX(endMs)` from ticks) and render the line only when
`now - tickEnd < 2 × pingIntervalMinutes`.

---

### F-13 — LOW — Documented "one-time OEM autostart hint screen" was never implemented

**Files:** `PROJECT.md:30` (the promise); absence confirmed by reading all
`ui/` sources and grepping `app/src/main` for
`autostart|AUTO_START|Startup|autostarts` — no matches.

**Evidence (PROJECT.md:30):**

```text
| Anti-kill | Battery-optimization exemption dialog + one-time OEM autostart hint screen |
```

**Why it matters:** the battery-optimization half exists
(DashboardScreen.kt:213-231), but the OEM autostart hint (Xiaomi/Huawei/Oppo
class of killers that prevent `BOOT_COMPLETED` and background work) is
nowhere in the codebase. On such devices the app will appear to "randomly"
stop logging — exactly the real-device robustness gap this row was meant to
close.

**Suggested fix (minimal):** a small one-time card on the dashboard
(prefs-flagged, mirroring the battery card) linking to
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with a short "check autostart
for DataCheck" hint; or consciously strike the row from PROJECT.md.

---

## Not findings (locked constraints and documented intent — deliberately not reported)

- **WorkManager-only, no exact alarms; 15-min floor; flexible ±minutes
  scheduling** — locked (AGENTS.md:14-16, PROJECT.md:11, 23). Not reported.
- **Mobile-only tracking (no Wi-Fi), `TYPE_MOBILE` everywhere** — locked
  (AGENTS.md:12, PROJECT.md:10, 20). Not reported.
- **Exactly the 4 manifest permissions** — locked (AGENTS.md:18-19); verified
  AndroidManifest.xml:5-9 matches. Not reported.
- **`rebootPending` is write-only (BootReceiver.kt:11 sets, SamplingEngine.kt:61
  clears, nothing reads it; grep: 3 occurrences)** — this was a seed candidate,
  but AGENTS.md:36-38 and PROJECT.md:24 document exactly this design: the
  receiver "only records a flag", and gap reconciliation works natively
  because NSM windows cover `[lastTickEndMs, now)` across reboots. Verdict:
  dead weight rather than a bug — safe to remove or leave. Not counted as a
  finding.
- **USM time-proportional FG/BG estimate as fallback, `queryDetailsForUid`
  state split as primary** — documented (PROJECT.md:26, AGENTS.md:39-40);
  code matches (SamplingEngine.kt:34-40, 182-205). Not reported.
- **Silent/no-spam updatable notification, low-importance channel** — locked
  (AGENTS.md:13). Not reported.
- **Room v1 schema, no destructive migration, committed keystore, <4 MB APK, [SUPERSEDED 2026-10-03]
  CI-only builds** — locked (AGENTS.md:27, 41-42, 50-53). Not reported.
- **6 h window clamp (`MAX_WINDOW_MS`)** — a deliberate design constant
  (SamplingEngine.kt:98-101, 208). Its *invisibility* is reported via F-05;
  the clamp itself is not a finding.
- **Minor items consciously not reported (below the lens / style):**
  `Format.bytes` returning `"?"` for negatives; 1024-based "GB/MB" labels;
  `usage.tickStart` having no index (table is small; perf-only); Settings'
  empty renewal-day field silently defaulting to 1; `Scheduler` KEEP-vs-prefs
  drift after a backup-restore edge; shared-UID packages labeled by first
  package name (documented system-bucket intent covers the fallback).

## Scope covered vs. skipped

**Covered:** every file in the manifest's coverage contract, read in full
(see top of document), plus calibration reads of AGENTS.md, PROJECT.md,
build config, and the FileProvider paths XML.

**Skipped (with reason):**
- `ui/DataCheckTheme.kt` — theming only, no logic in any lens.
- `app/src/test/**` — reviewed by listing only (two pure-JVM suites,
  `AttributionTest`, `ValidationLogicTest`); no DAO/Room test exists, which is
  noted inside F-01 as the reason the one high finding is unconfirmed.
- `VALIDATION.md` — ISSUE-003's deliverable, explicitly out of bounds.
- `res/` values/drawables and `.github/` CI wiring — no review lenses apply.
- **All runtime verification** — impossible on this host (no JDK/SDK, per
  AGENTS.md:50-53). Nothing was compiled, executed, or instrumented; findings
  rest on source evidence plus cited documented behavior.

*Findings are proposals only. Nothing here has been implemented; each awaits
user triage (see issue constraints).*
