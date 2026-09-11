# DataCheck M6 — Issue Backlog

Generated from `docs/PRD-M6-VALIDATION.md` (accepted). Tracker of record for
M6 issue status lives in this file.

## Backlog

```markdown
ISSUE-001: Ship a signed, size-gated release APK from CI
STATUS: done
  (attempt 1: impl + local checks + review PASS; CI run 34480922864 failed on
  a Kotlin DSL trap — 'java.util.Properties' unresolved because 'java' is the
  project extension. Attempt 2 fix: top-of-file import, commit fdc792b.
  CI run 34481250613 GREEN: both jobs pass, datacheck-release-apk artifact
  ~1.03 MB < 4 MiB gate, debug flow unchanged. Evidence:
  ~/.config/opencode/runs/data-check/M6/ISSUE-001/attempt-1/ + events.jsonl.)
TYPE: afk
BLOCKERS: none
OUTCOME: every green CI run attaches a signed, minified release APK (< 4 MB)
  as a downloadable artifact
ACCEPTANCE:
- CI release job runs `assembleRelease` and attaches the signed APK as an
  artifact (name: `datacheck-release-apk`)
- Release buildType has minify + resource shrinking enabled
- Release signing uses the self-signed keystore committed to the repo
  (passwords stored alongside in-repo)
- CI release job fails when the release APK is >= 4 MB (hard gate step)
- Existing debug job (`assembleDebug testDebugUnitTest lint`) stays green
LAYERS: build+ci
MODULES: app release buildType + signing config; .github/workflows/ci.yml
  release job; keystore file location is an implementation choice
TESTS: green CI run with release artifact present; size-gate step present in
  workflow and fails the job at threshold
COMMANDS: `gh run watch` / `gh run list --repo theDRElabs/data-check`;
  artifact presence check via `gh api` or run page
CONSTRAINTS: CI-only builds (no local toolchain); keystore + passwords
  committed to private repo (user-approved trade-off); git commit/push
  requires user approval per repo AGENTS.md — batch for approval at issue
  completion; no new dependencies or permissions; keep the existing
  debug-apk artifact flow unchanged
NON-GOALS: Play-signing, .aab bundles, keystore-as-CI-secret, size shrinking
  work (only if the gate trips does shrinking become a follow-up issue)
```

```markdown
ISSUE-002: Add in-app validation card for accuracy attribution
STATUS: done
  (two-push TDD via CI, user-approved. RED: run 34483016952, 8 predicted
  test failures against stubs. GREEN: run 34484850567 exposed a defective
  phase-1 test (1 MB operand vs asserted 1 GB delta) masked by red; operand
  fixed in c42d5a0, final green run 34485585367, 17/17 tests, both jobs
  green. Fresh review PASS: 0 blocking / 2 non-blocking (stale phase-2
  self-report claim; engine window math untested — follow-up candidate),
  5 notes incl. midnight-attribution + OEM snapshot caveats for human QA.
  Evidence: ~/.config/opencode/runs/data-check/M6/ISSUE-002/attempt-1/.)
TYPE: afk
BLOCKERS: none
OUTCOME: with a Settings toggle enabled, the dashboard shows a card comparing
  "NSM raw total today" vs "sum of logged ticks" plus the delta, so a mismatch
  vs Android Settings is attributable (our sampling vs system-level)
ACCEPTANCE:
- Card is hidden unless its persisted Settings toggle is enabled
- Card shows NSM raw mobile total today, sum of logged tick rows today, and
  the delta (human-readable units)
- Card renders a visible error state when the NSM query fails
- Comparison/delta logic is unit-tested (TDD: red before implementation)
- UI makes no direct NetworkStatsManager/UsageStatsManager calls; new query
  goes through the sampling engine
LAYERS: data+ui
MODULES: sampling engine gains a raw-today-total query; SettingsScreen gains
  the toggle; DashboardScreen gains the card; exact names follow existing
  conventions
TESTS: unit tests for delta computation and toggle-gated visibility logic
COMMANDS: CI only — `testDebugUnitTest` + `lint` via the existing workflow
  (no local builds)
CONSTRAINTS: no Room schema changes; no new permissions; card is read-only
  diagnostics and never mutates stored rows; locked decisions unchanged
NON-GOALS: full diagnostics screen, manual entry of the Settings figure,
  CSV-based comparison tooling
```

```markdown
ISSUE-003: Write VALIDATION.md QA kit
STATUS: done
  (attempt 1: docs-only, one pass. VALIDATION.md at repo root: day-long
  accuracy procedure with ±1% tolerance and validation-card attribution
  steps, reboot test (ping within ~one interval, NSM reconciliation,
  >6 h clamp caveat), signed datacheck-release-apk sideload/upgrade steps,
  results template. All 32 material behavior claims citation-verified by
  fresh reviewer (32/32); verdict PASS, 0 blocking / 3 non-blocking notes.
  Evidence:
  ~/.config/opencode/runs/data-check/M6/ISSUE-003/attempt-1/ + events.jsonl.
  Docs commit batched for user approval.)
TYPE: afk
BLOCKERS: ISSUE-001, ISSUE-002
OUTCOME: the user has one repo document to execute the M6 on-device checks
  (accuracy comparison, reboot test, sideload upgrade)
ACCEPTANCE:
- Accuracy procedure: day-long run, dashboard total vs Android Settings ->
  Data usage, +/-1% pass tolerance, mismatch-attribution steps using the
  validation card from ISSUE-002
- Reboot test steps with expected behavior (ping resumes after one interval,
  totals reconcile from NSM)
- Sideload/upgrade steps using the signed release APK from ISSUE-001
- Results-recording template (date, figures, pass/fail per check)
LAYERS: docs
MODULES: none (VALIDATION.md at repo root, alongside PROJECT.md/AGENTS.md)
TESTS: n/a — documentation deliverable
COMMANDS: inspection only — file exists at repo root and references the
  actual implemented toggle/card/APK behavior from its blockers
CONSTRAINTS: procedures must match implemented behavior (hence blockers);
  no app code changes in this issue
NON-GOALS: automated/device-farm testing, changing tolerance values
```

```markdown
ISSUE-004: Fresh-context review of existing Kotlin sources
STATUS: done
  (attempt 1: one full pass, 13 findings — 1 high (F-01 suspected Room
  SUM-NULL crash on empty table), 6 medium (midnight attribution, retention
  predicate mismatch, non-atomic window bookkeeping, swallowed NSM/USM
  errors, main-thread blocking, silent notification-permission loss),
  6 low. Fresh reviewer verified 13/13 citations and claims, 0 blocking /
  2 non-blocking (Mutex placement note on F-04; F-01/F-05 cross-reference),
  no material false negatives. Findings NOT implemented — awaiting user
  triage. Evidence:
  ~/.config/opencode/runs/data-check/M6/ISSUE-004/attempt-1/ + events.jsonl.
  Docs commit batched for user approval.)
TYPE: afk
BLOCKERS: none
OUTCOME: a findings document (docs/REVIEW-FINDINGS-M6.md) with file:line
  evidence covering the M1-M5 implementation, ready for user triage
ACCEPTANCE:
- Covers SamplingEngine, Scheduler/TickWorker/BootReceiver, PingNotifier,
  DashboardScreen/SettingsScreen, CsvExporter, Room DB layer
- Each finding states severity, file:line, evidence, and a suggested fix
- Strictly read-only: zero code or config changes
- Budget: stop after 15 findings or one full pass, whichever comes first
LAYERS: none (read-only review)
MODULES: none
TESTS: n/a — review deliverable
COMMANDS: read-only source inspection; artifact is the findings document
CONSTRAINTS: findings are NOT implemented until the user accepts them;
  locked product decisions (WorkManager-only, mobile-only, permission set)
  are constraints, not findings; untrusted-input rules n/a (offline app)
NON-GOALS: implementing fixes, style nitpicks, reopening locked decisions
```

```markdown
ISSUE-005: Run on-device M6 validation and record results
STATUS: in progress (owner: user, HITL)
  Check 3 (sideload/upgrade) PASS, prerequisites PASS. Check 1 (accuracy)
  FAILED 2026-09-11 with attribution data — spawn per contract:
  validation card read 2026-09-11: NSM raw today 441 MB / logged today
  749 MB / delta +308 MB; Android Settings today 470 MB. NSM agrees with
  Settings (~6%, minutes apart, includes catch-up); LOGGED is +308 MB over
  NSM → double-counted windows (F-04 race between dashboard catch-up tick
  and worker tick). Secondary observation: bundle "left" also misleading
  because usage counts from renewal-cycle start, not bundle-entry time
  (user entered 121 MB at 17:21 the day before; real balance 63 MB).
  Follow-ups spawned: ISSUE-006 (F-04 double-count fix), ISSUE-007
  (bundle baseline semantics). Check 2 (reboot) still pending.
TYPE: hitl
BLOCKERS: ISSUE-001, ISSUE-002, ISSUE-003
OUTCOME: recorded pass/fail for the day-long accuracy comparison (+/-1%),
  the reboot test, and the signed-APK upgrade install
ACCEPTANCE:
- Results recorded in the VALIDATION.md results template (or a linked
  RESULTS.md) with dates and figures
- Any failure spawns a follow-up issue carrying the validation card's
  attribution data (sampling bug vs system-level difference)
LAYERS: none (physical device testing)
MODULES: none
TESTS: the checks described in VALIDATION.md
COMMANDS: n/a — human-operated device checks
CONSTRAINTS: requires the physical phone and ~1 day elapsed time for the
  accuracy run; owner: user; agent may not simulate or claim these results
NON-GOALS: emulator-based substitutes, agent-declared pass/fail
```

```markdown
ISSUE-006: Fix double-counting — make tick window bookkeeping atomic
STATUS: done
  (two-push TDD via CI. RED: commit c9bb631, run 34587597213 — 7/7
  predicted TickBookkeeperTest failures, 4 guards + 21 existing green.
  GREEN: commit e3b919e, run 34588888051 — both jobs green, 28/28 tests,
  artifacts present. Fresh review PASS: 0 blocking / 3 non-blocking
  (sync prefs commit inside mutex; write-only cache getter; F-06 main-
  thread NSM interaction pre-existing), stamp-row harmlessness verified
  against all DAO queries, deadlock/reentrancy clear. Residual: today's
  already-double-counted rows stay until ~30d retention purge or reinstall;
  on-device "delta returns to small" check pending user validation.
  Evidence: ~/.config/opencode/runs/data-check/M6/ISSUE-006/attempt-1/.)
TYPE: afk
BLOCKERS: none
OUTCOME: a given usage window is sampled exactly once; opening the app
  while background ticks run can never re-log an already-logged window
EVIDENCE: validation card 2026-09-11: logged today 749 MB vs NSM raw
  today 441 MB (delta +308 MB, ~70% over-count) while user repeatedly
  opened the app to test the card — the F-04 race (REVIEW-FINDINGS-M6.md,
  SamplingEngine.kt:85-104, DashboardScreen.kt:265-272): window start is
  read-then-written in SharedPreferences; the dashboard catch-up tick
  races the WorkManager tick, both insert rows for the same window;
  crash-after-commit also re-inserts.
ACCEPTANCE:
- Window start comes from the DB (latestEndMs-style query), not
  SharedPreferences read-then-write; SharedPreferences only as cache
- An app-scoped mutex (companion/singleton level per review NB-1 — NOT a
  per-instance field, SamplingEngine is constructed per call) serializes
  runTick across worker and dashboard catch-up paths
- Regression test: concurrent/interleaved tick scenario in JVM unit test
  proves no double-insert (may require extracting window bookkeeping into
  a testable unit)
- Validation-card delta on a healthy day returns to "small" (expected
  ≤ one interval of traffic)
- No Room schema changes; no new dependencies/permissions; TDD red first
LAYERS: data
MODULES: SamplingEngine.kt (window bookkeeping), UsageLogDao.kt (window
  start query), TickWorker.kt/DashboardScreen.kt (call sites only)
TESTS: JVM unit test for window resolution under interleaving; existing
  17 tests stay green
COMMANDS: CI only — testDebugUnitTest + lint via existing workflow
CONSTRAINTS: locked decisions unchanged; no schema change without
  migration; commits batched for user approval
NON-GOALS: fixing F-02 midnight attribution, F-03 retention mismatch, or
  any other review finding (separate triage)
```

```markdown
ISSUE-007: Bundle tracker counts from bundle-entry time, not cycle start
STATUS: done
  (two-push TDD via CI. RED: commit bdc2b03, run 34595788054 — 12/12
  predicted BundleLogicTest failures, 28 existing green. GREEN: commit
  7c3053d, run 34596751891 — build+release success, 40/40 tests,
  artifacts present. Fresh review PASS: 0 blocking / 1 medium + 8 low
  notes. Key design: BundleLogic pure unit (CardState Hidden/Values);
  Prefs gained bundleEntryAtMs; Save stamps entry time; bundleUsed =
  totalsSince(entryAtMs); renewal day kept informational (daysLeft).
  Migration: legacy installs (bundle set, no stamp) hide the card until
  re-save, with an in-Settings hint. Residual: F-01 hit-window widened
  (NB-1, feeds F-01 triage); daysLeft boundary cases untested (NB-3);
  pct bar direction needs on-device QA. PROJECT.md bundle row updated
  to match. Evidence:
  ~/.config/opencode/runs/data-check/M6/ISSUE-007/attempt-1/.)
TYPE: afk
BLOCKERS: none
OUTCOME: "X left" reflects what the user actually has: bundle usage counts
  from when the user entered/reset the bundle figure, not from the
  renewal-cycle calendar start
EVIDENCE: user entered 121 MB remaining at 17:21 on 2026-09-10; app showed
  18 MB left while real balance was 63 MB. DashboardScreen.kt:297-298
  computes bundleUsed = totalsSince(cycleStart(renewalDay)) — includes
  usage from before the entry, and treats the entry as the cycle total.
OUTCOME-NOTE: user-facing semantics decision embedded here: entry means
  "this is my current remaining balance", consumed from entry time
  forward. (If the user wants cycle-total semantics instead, say so and
  this issue changes.)
ACCEPTANCE:
- Entering a bundle figure stamps an entry timestamp (persisted)
- bundleUsed counts only usage rows with tickStart >= entry timestamp
- Entering a new figure resets the baseline without touching logged rows
- UI copy makes clear the figure means "remaining as of now"
- Unit tests for the baseline logic (TDD red first); existing tests green
- No Room schema changes; no new dependencies/permissions
LAYERS: data+ui
MODULES: Prefs.kt (entry timestamp), DashboardScreen.kt (baseline
  window), SettingsScreen.kt (copy), new pure-logic unit for baseline
TESTS: JVM unit tests for entry-time baseline computation
COMMANDS: CI only — testDebugUnitTest + lint via existing workflow
CONSTRAINTS: locked decisions unchanged; commits batched for user approval
NON-GOALS: carrier zero-rating/metering differences (system-level, out of
  scope), changing renewal-day feature, F-02/F-03 fixes
```

## Dependency Map

- `ISSUE-001` (release APK) — no blockers. Output: signed artifact + gate.
- `ISSUE-002` (validation card) — no blockers. Output: toggle + card + engine query.
- `ISSUE-003` (VALIDATION.md) — blocked by 001 + 002: the documented
  procedures consume the actual APK flow and card behavior they describe.
- `ISSUE-004` (review pass) — no blockers. Output: findings doc (feeds the
  HITL triage, not any issue here).
- `ISSUE-005` (on-device validation) — blocked by 001 + 002 + 003: it
  executes the kit against the shipped APK and the card. IN PROGRESS:
  Check 3 PASS, Check 1 FAIL (spawned 006 + 007), Check 2 pending.
- `ISSUE-006` (double-count fix) — no blockers; spawned from ISSUE-005
  Check 1 failure with attribution data.
- `ISSUE-007` (bundle baseline) — no blockers; spawned from ISSUE-005
  user observation.

## Parallel Branches

Initially ready and mutually independent: **ISSUE-001, ISSUE-002, ISSUE-004**.
Recommended execution order despite independence: 001 and 002 first (003 and
005 both wait on them); 004 can run any time in parallel.

## HITL Queue

- **ISSUE-005** — owner: user. Decision: run the on-device checks, record
  pass/fail, spawn follow-ups on failure. In progress: Check 3 PASS,
  Check 1 FAIL (follow-ups 006/007 spawned), Check 2 (reboot) still to run
  — ideally after ISSUE-006 lands so reconciliation is measured on fixed
  code.
- **ISSUE-006 + ISSUE-007 execution** — both ready/AFK; starting them needs
  user go-ahead (this message's "yes" covers ISSUE-006 first).
- **Review-findings triage** (after ISSUE-004) — owner: user. Decision: which
  findings become backlog issues.
- **Size-gate triage** (conditional, after ISSUE-001) — owner: user. Only if
  the first gated release build is >= 4 MB: decide shrinking tradeoffs.
- **Standing gate** — per repo AGENTS.md, any git commit/push needs explicit
  user approval; AFK issues batch their commits for approval at completion.

## Rejected Slices

- "Full diagnostics screen with Settings-figure input" — rejected in
  alignment; the validation card (ISSUE-002) covers the need.
- "All CI/infrastructure as standalone work" — rejected as consumer-less
  horizontal slicing; folded into ISSUE-001 whose outcome is the installable
  APK itself.
- "Implement review findings now" — rejected until the user accepts findings
  (post-ISSUE-004 triage).
- "APK shrinking work" — rejected as speculative; only becomes an issue if
  the ISSUE-001 gate actually trips.

## Handoff

Implementation waits for the user to select an issue. Suggested start:
ISSUE-001 or ISSUE-002 (both unblock the most downstream work).
