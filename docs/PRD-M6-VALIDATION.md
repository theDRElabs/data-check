# PRD — DataCheck M6: Validation & Release

## Status

`active`

## Problem and Users

DataCheck (personal sideload Android app, sole user = DRE) has milestones M1–M5
complete. What remains is M6: producing an installable **signed release APK**
from CI, and enabling the **on-device validation** (day-long accuracy comparison
vs Android Settings ±1%, reboot test) that only the user can physically perform.

**Success condition:** every green CI run produces a signed, minified release
APK < 4 MB; the user can attribute any accuracy mismatch using an in-app
validation card; the user can execute the accuracy and reboot tests from a
written QA kit.

## Solution

Agent-side work plus a QA kit; on-device checks stay with the user.

1. **Release pipeline:** release build variant with minify + resource shrinking,
   signed with a self-signed keystore committed to the private repo (passwords
   in-repo alongside it). CI attaches the signed release APK as an artifact on
   every green run.
2. **Size gate:** CI fails the release job if the APK is ≥ 4 MB.
3. **Validation card:** a small in-app card (behind a Settings toggle) showing
   "NSM raw total today" vs "sum of logged ticks" so a mismatch is immediately
   attributable to our sampling logic vs a system-level difference. Shows an
   error state if the NSM query fails.
4. **QA kit:** `VALIDATION.md` in the repo — accuracy comparison procedure,
   reboot test steps, pass/fail tolerances.
5. **Review pass:** a read-only fresh-context review of the existing Kotlin
   sources produces a findings list; only user-accepted findings become backlog
   issues.

## Observable Stories

- The user downloads a signed release APK from a green GitHub Actions run,
  sideloads it over the previous install (same signing identity → upgrade
  installs without data loss), and it is < 4 MB.
- If a change pushes the release APK to ≥ 4 MB, the CI run goes red.
- The user opens Settings, enables the validation toggle, and sees the
  validation card on the dashboard: two totals (NSM raw today, sum of logged
  ticks) and the delta between them.
- The user follows `VALIDATION.md` for a day, compares the dashboard total to
  Android Settings → Data usage, and records pass/fail against ±1%.
- The user reboots the phone, waits one tick interval, and confirms the ping
  resumes and totals reconcile (procedure in `VALIDATION.md`).

## Acceptance Criteria

1. CI `assembleRelease` succeeds and produces a signed APK; existing
   `assembleDebug testDebugUnitTest lint` stay green.
2. CI release job fails when release APK size ≥ 4 MB (hard gate).
3. Release APK is signed with the committed keystore and installs as an
   upgrade over a previous install of the same identity.
4. Validation card renders only when its Settings toggle is enabled; shows NSM
   raw total, logged-tick sum, delta; renders a visible error state on NSM
   query failure; adds no new permissions.
5. TDD: new logic (delta computation, toggle state) covered by unit tests.
6. `VALIDATION.md` exists with accuracy procedure (±1% tolerance), reboot
   test steps, and recording instructions.
7. Review pass produces a findings list with file/line evidence; no findings
   are implemented until the user accepts them.
8. No locked product decision is violated (permissions set, mobile-only,
   WorkManager-only, offline).

## Data Behavior

- **No schema changes.** Validation card reads live NSM queries and Room
  aggregates; it persists nothing beyond its Settings toggle in Prefs.
- Source of truth for usage remains `NetworkStatsManager`; Room tick log is
  the history store and is never mutated by the validation card.
- Keystore file + passwords live in the private repo; losing the repo/keystore
  = losing the signing identity (accepted personal-use trade-off).

## Contracts and Module Map

- Single `:app` module; existing package layout `data/`, `work/`, `notify/`,
  `ui/` is preserved.
- Validation card logic goes through the existing sampling engine; UI must not
  call `NetworkStatsManager`/`UsageStatsManager` directly (AGENTS.md rule).
- CI workflow file (`.github/workflows/ci.yml`) gains a release job; exact
  internal step structure is an implementation decision.

## Security Constraints

- No INTERNET permission; exactly the 4 allowed permissions (locked).
- Fully offline; no new trust boundaries. Keystore-in-private-repo is an
  accepted trade-off for a personal app.

## Testing and QA

- **Automated:** existing CI checks; new unit tests for validation-card logic;
  release job + size gate.
- **Manual (user, on device):** day-long accuracy comparison vs Settings
  (±1%), reboot test, sideload upgrade install. Procedures in `VALIDATION.md`.
- **Unavailable locally:** no local JDK/SDK — all builds verified on CI only.

## Non-Goals

- Wi-Fi tracking; exact alarms / `SCHEDULE_EXACT_ALARM`; Play Store concerns.
- New dependencies, permissions, or SDK changes.
- Local toolchain builds.
- Implementing review findings before user acceptance.

## Unresolved Risks

| Risk | Label | Owner | Blocks |
|---|---|---|---|
| First gated release build may exceed 4 MB | `HITL` if shrink tradeoffs needed | User | Size gate shipping green |
| Review findings await acceptance | `HITL` | User | Backlog generation for findings |
| Actual ±1% accuracy result unknown until user runs the day-long test | `UNKNOWN` | User | M6 completion sign-off |
| Reboot behavior on the actual device | `UNKNOWN` until tested | User | M6 completion sign-off |

## Handoff

This document is ready for review. Issue generation (`prd-to-issues`) starts
only after the user accepts it.
