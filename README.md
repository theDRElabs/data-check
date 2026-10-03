# DataCheck

**Per-app mobile data usage on Android, so you can check whether your ISP's number is telling the truth.**

Android tells you how much mobile data you used. It doesn't tell you *which apps* used it, in the foreground or the background. DataCheck does — logging every app's usage in 15-minute slices, pinging you a running total, and keeping the full history so you can find what's actually draining your bundle.

Built as an Android app by an AI coding agent on a 3GB RAM Android phone.

## What it does

- **Per-app usage, foreground vs background** — from `NetworkStatsManager`'s state buckets, so the split is real system attribution, not an estimate
- **A ping every 15 minutes** — one updatable notification with today's mobile total, the last window's delta, and your top 3 apps by MB. No sound, no spam
- **A full log** — a Room row per tick per app: `(tickStart, pkg/uid, rx, tx, fgRx, fgTx)`. Past rows are never rewritten, except for boot reconciliation
- **Bundle tracker** — enter your bundle size and renewal day; the dashboard shows what's left as of now, used since you saved, and days remaining
- **Validation card** — compare DataCheck's total against Android's own figure (see [`VALIDATION.md`](./VALIDATION.md))

## Why it exists

I wanted to know where my mobile data was actually going, and whether the number I was billed matched the apps I was using. Android's built-in settings couldn't answer that, and third-party apps either needed root, wanted Wi-Fi data too, or reset on reboot.

## Install

Sideloaded — not on the Play Store. Grant usage access after installing:

**Settings → Apps → Special app access → Usage access → DataCheck → allow**

The exact path varies by Android version. The app needs this to read system usage counters and cannot work without it. You'll probably also want to remove battery restrictions: **Settings → Apps → DataCheck → Battery → Unrestricted**. Background restrictions are the #1 cause of missed samples.

Verification steps — day-long accuracy check (±1% tolerance), reboot test, and upgrade install — are in [`VALIDATION.md`](./VALIDATION.md).

## Design decisions

| Decision | Choice | Why |
|---|---|---|
| Usage source | `NetworkStatsManager.querySummary(MOBILE, window)` per UID | Authoritative per window, survives reboots, mobile-only by construction. `TrafficStats` was dropped: it mixes in Wi-Fi and resets on boot |
| Networks tracked | Mobile only | A deliberate lock — Wi-Fi tracking is not added |
| Tick timing | WorkManager periodic, 15 min | 15 min is the API floor. No `AlarmManager` exact alarms, no `SCHEDULE_EXACT_ALARM` — those are a permissions fight and Doze doesn't respect them anyway |
| Ping delivery | Single updatable notification | One notification, refreshed each tick |
| Boot survival | WorkManager auto-restores; boot receiver records a pending flag | `NSM` queries cover missed windows natively |
| APKs | < 4 MB release | minify + resource shrinking on |

## Permissions — exactly these four

`ACCESS_NETWORK_STATE` · `PACKAGE_USAGE_STATS` · `RECEIVE_BOOT_COMPLETED` · `POST_NOTIFICATIONS`

**No `INTERNET` permission, ever.** The app is fully offline. It reads system counters and shows you numbers; it has no business talking to anything.

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · WorkManager · minSdk 26 · single `:app` module

```
data/   stats sources, Room
work/   scheduler + tick worker
notify/ ping builder
ui/     Compose screens
```

All stats logic goes through one sampling engine — UI and worker never call `NetworkStatsManager`, `TrafficStats`, or `UsageStatsManager` directly.

## Build

```bash
./gradlew assembleDebug      # unsigned debug build, no secrets needed
./gradlew assembleRelease    # signed only if a keystore is available
```

### Release signing

**The signing key is not in this repo, and must never be committed here.** `.gitignore` blocks `keystore/`, `*.p12`, `*.jks` and `keystore.properties`.

Generate one:

```bash
keytool -genkeypair -v \
  -keystore keystore/datacheck-release.p12 \
  -storetype PKCS12 \
  -alias datacheck-release \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then supply credentials either way:

| Setting | Local build | CI |
|---|---|---|
| `KEYSTORE_STORE_FILE` | `storeFile` in an untracked `keystore/keystore.properties` | `DATA_CHECK_KEYSTORE_BASE64` (base64 of the `.p12`) |
| `KEYSTORE_STORE_PASSWORD` | `storePassword` | `DATA_CHECK_KEYSTORE_PASSWORD` |
| `KEYSTORE_KEY_ALIAS` | `keyAlias` | `DATA_CHECK_KEY_ALIAS` |
| `KEYSTORE_KEY_PASSWORD` | `keyPassword` (defaults to store password on PKCS12) | same secret |
| `KEYSTORE_STORE_TYPE` | optional, defaults to `PKCS12` | — |

If no keystore is available, `assembleRelease` still succeeds and emits an **unsigned** APK plus a build-log warning. Nothing breaks.

> **Note:** an earlier release key was committed to this repository by mistake and became public on 2026-10-03. It was purged from git history but is permanently revoked — treat any APK signed with it as untrusted. A replacement key cannot upgrade an app installed with the old signature; uninstall the old build before installing a newly signed one.

## Project docs

| File | What it is |
|---|---|
| [`PROJECT.md`](./PROJECT.md) | Product plan, locked decisions, architecture, milestones M1–M6 |
| [`VALIDATION.md`](./VALIDATION.md) | On-device validation kit |
| [`AGENTS.md`](./AGENTS.md) | Operating rules for the AI agent working on this |
| [`docs/PRD-M6-VALIDATION.md`](./docs/PRD-M6-VALIDATION.md) | PRD for the validation milestone |
| [`docs/BACKLOG-M6.md`](./docs/BACKLOG-M6.md) | Remaining work |
| [`docs/REVIEW-FINDINGS-M6.md`](./docs/REVIEW-FINDINGS-M6.md) | Review findings |

## Status

Built and working as intended on an Infinix Smart 7 Plus (Android 12, 3GB RAM). Personal sideload build — no Play policy constraints applied. Validation kit is the gate for anything shipped.

## License

[MIT](./LICENSE)
