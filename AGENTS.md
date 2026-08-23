# AGENTS.md — DataCheck

Personal-use Android app that tracks **mobile data usage per app** (foreground/background),
posts a **notification ping every ~15 minutes**, and keeps a full log of top consumers.
Sideloaded only — no store distribution.

## Required Reading
- `PROJECT.md` — product plan, locked decisions, architecture table, milestones M1–M6.
  Treat it as the source of truth for scope. Do not contradict it.

## Locked Product Decisions (do not reopen without user approval)
- Mobile data only (never add Wi-Fi tracking silently).
- Ping = single updatable notification refreshed each tick; no sound/spam.
- Tick = WorkManager periodic only, interval user-configurable (floor 15 min =
  WorkManager API minimum); **no** `AlarmManager` exact alarms,
  no `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` permission.
- Fully offline app: **no INTERNET permission, ever.**
- Allowed permissions are exactly these 4: `ACCESS_NETWORK_STATE`,
  `PACKAGE_USAGE_STATS`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`.
- Personal sideload build; Play-policy concerns are out of scope.

## Technology Stack (fixed)
- Kotlin + Jetpack Compose, Material 3.
- minSdk 26, target/latest SDK per installed toolchain.
- Room for the usage log; WorkManager for scheduling; no other major deps
  unless the user approves.
- APK size target < 4 MB release; enable minify + resource shrinking.

## Architecture Rules
- Single `:app` module. Keep package layout: `data/` (stats sources, Room),
  `work/` (scheduler + tick worker), `notify/` (ping builder), `ui/` (Compose).
- All stats logic goes through one sampling engine; UI and worker must not call
  `NetworkStatsManager`/`TrafficStats`/`UsageStatsManager` directly.
- Every tick writes a Room row set: `(tickStart, pkg/uid, rx, tx, fgRx, fgTx)`.
  Never mutate past rows except boot reconciliation.
- Boot handling: WorkManager re-registers itself; boot receiver only records a
  "device rebooted" flag (TrafficStats counters reset) and lets the next tick
  reconcile gaps from `NetworkStatsManager` hourly buckets.
- FG/BG split comes from `UsageStatsManager` events per window; unattributed
  UIDs land in a "system" bucket — never drop bytes.
- Room schema changes require migrations; never `fallbackToDestructiveMigration`
  once v1 ships on the device.

## Commands (Gradle wrapper, run from repo root)
- Build debug APK: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`
- JVM cap is mandatory on this device: `org.gradle.jvmargs=-Xmx1536m`
  (already set in `gradle.properties`; never raise it without asking).
- Builds run on GitHub Actions (`.github/workflows/ci.yml`), not locally:
  this device has no JDK/Android SDK by user decision. Debug APK is attached
  as the `datacheck-debug-apk` artifact on every green run of
  `theDRElabs/data-check`. Never add local-toolchain assumptions to scripts.
- Repo: private `theDRElabs/data-check`, branch `main`, commits pushed with
  the noreply email already configured in local git config.

## Development Workflow
- Work milestone-by-milestone (M1–M6 in `PROJECT.md`). Finish a milestone,
  verify it, then move on.
- Accuracy checks compare app totals vs Android Settings → Data usage
  (tolerance: ±1% over a day).
- Reboot test after any scheduler/boot-path change.

## Approval Required From User Before Doing
- Any git commit/push or repo creation.
- Adding/changing dependencies, permissions, or min/target SDK.
- Reopening a "Locked Product Decision" above.
- Installing new system toolchains (JDK/SDK packages).

## Definition of Done (per change)
- Compiles via the project build command with zero errors.
- New logic covered by at least a manual verification step described in the
  milestone; scheduler changes include a reboot check.
- No new permissions; notification stays silent/updatable.
- Totals still reconcile against system Settings within tolerance.
