# DataCheck — Project Guide

Lightweight Android app that accurately tracks **mobile data usage** per app (foreground/background), pings the user via **notification every 15 minutes**, and keeps a full log of top consumers.

## Locked Decisions

| Decision | Choice |
|---|---|
| Ping delivery | Notification only (updatable, no spam) |
| Networks tracked | **Mobile data only** |
| Tick timing | Flexible ±minutes → WorkManager-only, NO exact alarms / `SCHEDULE_EXACT_ALARM` |
| Distribution | Sideload / personal use — no Play policy constraints |

## Architecture

**Stack:** Kotlin + Jetpack Compose · minSdk 26 · Room · WorkManager · target APK < 4 MB

| Concern | Mechanism |
|---|---|
| Usage source | `NetworkStatsManager.querySummary(MOBILE, window)` per-UID rx/tx — authoritative per window; spans reboots; mobile-only by construction (TrafficStats dropped: mixes in Wi-Fi + resets on boot) |
| Fine-grained sampling | Not needed — NSM returns exact per-window totals; windows are 15 min |
| FG/BG attribution | `UsageStatsManager.queryEvents(window)` → each app's foreground intervals → fraction of window foregrounded; unattributed UIDs = system bucket |
| 15-min tick | Single `PeriodicWorkRequest(15.min)` — API minimum is exactly 15 min, flexes under Doze (accepted) |
| Reboot survival | WorkManager auto-restores after boot; boot receiver sets `rebootPending` flag + re-ensures periodic work; NSM queries cover missed windows natively |
| Ping | Updatable notification each tick: mobile total today, last-window delta, top-3 apps by MB |
| Log | Room table `(tick, pkg/uid, rx, tx, fgRx, fgTx)` + CSV export button |
| Anti-kill | Battery-optimization exemption dialog + one-time OEM autostart hint screen |

### Permissions (4 — app is fully offline, no INTERNET permission)
1. `ACCESS_NETWORK_STATE`
2. `PACKAGE_USAGE_STATS` — special permission, granted via Settings onboarding screen
3. `RECEIVE_BOOT_COMPLETED`
4. `POST_NOTIFICATIONS`

### Accuracy notes
- `NetworkStatsManager` = ground truth; hourly buckets reconcile drift from TrafficStats deltas.
- Per-app stats stay attributed to real apps even under VPN; tethering counts to system UID.
- Reboot resets TrafficStats counters → detect via boot flag + reconcile with NetworkStatsManager totals.

## Milestones
1. **M1 Scaffold** — project structure, manifest, permissions, usage-access onboarding flow
2. **M2 Sampling engine** — snapshot → delta → FG/BG split + Room schema
3. **M3 Scheduling** — WorkManager periodic task + boot reconciliation
4. **M4 Ping + export** — updatable notification each tick, CSV export
5. **M5 Dashboard** — Compose UI: today's mobile total, per-app FG/BG ranking, 7-day history
6. **M6 Validation** — compare totals vs Settings → Data usage, reboot test, signed sideload APK

## Build Environment Notes (on-device Termux)
- Gradle JVM tuning required: `org.gradle.jvmargs=-Xmx1536m` in `gradle.properties`.
- Generous timeouts on dependency downloads (slow phone network).
- If local release builds choke, wire GitHub Actions CI via `pipeline-init`.
