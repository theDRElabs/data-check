# DataCheck — On-Device Validation Kit (M6)

Three checks you run on the phone: a day-long accuracy comparison against
Android's own data-usage figure (±1% tolerance), a reboot test, and a
sideload/upgrade install of the signed release APK. Record results in the
template at the bottom.

All figures below are mobile data only (the app tracks mobile, not Wi-Fi).

## Prerequisites (once, before any check)

1. Install the signed release APK (see Check 3 if not installed yet).
2. Grant usage access — the app reads system usage counters and cannot work
   without it. On the phone: Settings → Apps → Special app access → Usage
   access → DataCheck → allow. (Exact path varies by Android version.)
3. Verify usage access: open DataCheck → Settings (gear icon, top right) →
   under "Validation", turn on "Show validation card" → back to the dashboard.
   A "Validation" card with three numbers appears. If it instead says
   "Couldn't read NSM raw total — check usage access and reopen.", usage
   access is still off.
4. Battery: if the dashboard shows "Battery saver may delay background
   pings.", tap "Unrestrict DataCheck" and follow the prompt. Background
   restrictions are the #1 cause of missed samples.

Reading figures: the dashboard refreshes every time you open it (and logs a
catch-up sample if 10+ minutes passed since the last one). Open the app, wait
a few seconds, then read.

## Check 1 — Day-long accuracy (±1% tolerance)

Goal: after a full day of normal use, DataCheck's "Mobile today" total is
within ±1% of Android's own figure for the same day.

Setup notes:

- Fresh installs log nothing until the second tick — the first open only
  marks the starting point. Install and open the app the evening before, or
  start counting one interval after first open.
- Run at your normal ping interval. 15 minutes (the default and the minimum
  Android allows) gives the tightest coverage.

Steps:

1. Morning: confirm prerequisites are done. Note today's date.
2. Use the phone normally all day. Leave DataCheck installed and mobile data
   on. Do not force-stop it (a force-stopped app cannot run background
   samples until reopened).
3. End of day, before midnight, in this order:
   a. Open DataCheck, wait a few seconds, note the "Mobile today" figure from
      the top card.
   b. Immediately open Android Settings → Data usage (mobile) and note the
      system's mobile usage for today. (Menu path varies by device; you want
      the system screen showing today's mobile data, not the billing-cycle
      total.)
4. Compute: |app figure − system figure| ÷ system figure × 100.
5. Pass if the difference is ≤ 1%. Record both figures in the template.

### If it fails — attribute the mismatch with the validation card

The "Validation" card (Settings → "Show validation card", if not already on)
shows, on the dashboard:

- "NSM raw today" — the system's live mobile total since local midnight,
  read directly from Android's usage counters (the same data source the
  Settings screen is built on).
- "Logged today" — the sum of DataCheck's logged samples today. This is the
  same number as the "Mobile today" card.
- "Delta" — NSM raw minus logged. Positive = DataCheck logged less than the
  system recorded; the sign is shown as a leading "-".

Steps:

1. Open DataCheck, wait a few seconds, read all three card values.
2. Compare "NSM raw today" against the Android Settings figure from step 3b:
   - NSM raw is within ~1% of Settings, but the delta is larger than ~1% of
     the total → our sampling missed windows. Typical causes: battery
     restrictions/Doze gaps, the app was force-stopped, or any single gap
     longer than 6 hours (by design, a gap longer than 6 h only has its last
     6 h logged). Unrestrict the battery (Prerequisites step 4) and re-run
     the day.
   - The delta is small, but NSM raw itself differs from Settings by more
     than ~1% → the difference is system-level: Android's Settings screen
     accounts usage differently from the mobile counters the app reads. Not
     an app bug. Record the figures and note "system-level".
3. Record all three card figures plus the verdict in the template.

## Check 2 — Reboot test

Goal: after a reboot, sampling resumes on its own and nothing is lost.

Expected behavior (why): the app re-registers its periodic background tick at
boot, and the first tick after the reboot samples the entire window back to
the last pre-reboot tick — usage during the powered-off gap is picked up from
the system's counters, not lost.

Steps:

1. Note your ping interval (DataCheck → Settings → "Ping interval"; default
   15m; the chips apply immediately, no save needed). Note the current
   "Mobile today" figure.
2. Reboot the phone normally.
3. After the phone comes back, unlock and note the time. Wait for the
   DataCheck notification — title "Mobile today: …", text "+… in last
   window", with your top apps listed. It is silent (no sound/vibration by
   design). Allow roughly one interval after boot (e.g. ~15–20 min at 15m);
   the scheduler is inexact and battery saver adds delay. If notifications
   are disabled for DataCheck, skip to step 4.
4. Open DataCheck:
   - "Mobile today" includes the pre-reboot usage (it should not have
     dropped to a small number).
   - The "Validation" card delta is small — comparable to a normal reading
     (it reflects at most the traffic since the last tick).
   - Note: if the phone was off for more than 6 hours, only the last 6 hours
     of the gap are logged — a larger delta is then expected and not a
     failure.
5. Pass criteria:
   - Ping (notification) arrived within about one interval of boot, and
   - "Mobile today" reconciles: matches Android Settings within the usual
     1% (or the validation delta is explained by the >6 h note above).
6. Record times and figures in the template.

## Check 3 — Sideload / upgrade from CI

Goal: install the signed release APK from a green CI run over the existing
install, keeping all data.

Steps:

1. On a computer or the phone's browser: open the GitHub repo → Actions tab
   → the latest run on `main` with a green checkmark.
2. Download the `datacheck-release-apk` artifact from that run. The download
   is a .zip — extract the .apk inside.
3. Sanity-check the size: it must be well under 4 MB (CI fails any build
   ≥ 4 MiB; current builds are ~1.0 MB).
4. Get the .apk onto the phone and tap it. If prompted, allow your file
   manager or browser to install unknown apps, then tap Install/Update.
   - Every CI release build is signed with the same committed keystore, so
     Android performs an in-place update: your logged history and settings
     are preserved.
   - If Android refuses with an installation error, the existing install was
     signed differently (e.g. an old debug build). Uninstall DataCheck first
     — this clears its data — then install the release APK.
5. Open DataCheck and verify:
   - History intact: "Mobile today", "Top apps today", and "Last 7 days"
     are populated as before.
   - Settings unchanged: ping interval, bundle, validation toggle.
6. To confirm the new version landed: Android Settings → Apps → DataCheck
   (the app itself does not display its version).
7. Pass criteria: install completed without a signature error, and the data
   from step 5 is still present.
8. Record in the template.

## Results template

Copy this block per test day.

```
Run date:            ____________
CI run (commit/link):____________
Ping interval:       ____________

Check 1 — Accuracy
  Dashboard "Mobile today":     ____________
  Android Settings (today):    ____________
  Difference:                  _____ %     Pass: [ ] ≤1%  [ ] fail
  Validation card (if failed):
    NSM raw today:             ____________
    Logged today:              ____________
    Delta:                     ____________
    Verdict:                   [ ] our sampling  [ ] system-level

Check 2 — Reboot
  Interval:                    ____________
  Notification arrived:        _____ min after boot   Pass: [ ] ~1 interval
  "Mobile today" pre-reboot:   ____________
  "Mobile today" post-reboot:  ____________
  Validation delta post-reboot:____________
  Reconciles:                  [ ] yes  [ ] no (>6 h off: ____________)

Check 3 — Sideload / upgrade
  APK size:                    _____ MB (< 4 MB)     Pass: [ ] yes
  Installed as in-place update:[ ] yes  [ ] no
  Data preserved after update: [ ] yes  [ ] no

Notes: ______________________________________________
```
