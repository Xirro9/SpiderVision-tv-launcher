# Phase 6 — SpiderVision as the Home app

## Read-only findings (2026-11-03)

| Check | Result |
| --- | --- |
| How HOME is chosen | Android's HOME role (RoleManager). Holder: `com.google.android.tungsten.setupwraith` (its `RecoveryActivity` hands the Home key to Google TV's `launcherx`) |
| HOME candidates | `launcherx/.home.HomeActivity` (priority 2), `setupwraith/.RecoveryActivity` (1), `tv.settings/.system.FallbackHome` (−1000) |
| On-TV picker | `android.settings.HOME_SETTINGS` → `permissioncontroller/…HomeSettingsActivity` exists, so the Home app can be changed (and changed back) on the TV |
| Google TV home package | `com.google.android.apps.tv.launcherx`, updatable system app, privileged; not protected from being disabled |
| Already disabled launchers | `jp.co.sharp…epoplauncher`, `com.smartdevice.leanbacklauncher.customizer` (owner's earlier debloat) |
| Second user | user 10 (`com.android.tv.profile`) exists; changes below apply to user 0 only |

Whether the firmware keeps a third-party Home app after the Home key, a reboot or a Google TV
update can only be proven by trying it. That is what the steps below test.

## Step A — make SpiderVision the Home app (fully reversible)

1. Add a `MAIN` + `HOME` + `DEFAULT` intent filter to SpiderVision (it still also appears as a
   normal TV app).
2. Give SpiderVision the HOME role:
   `adb shell cmd role add-role-holder android.app.role.HOME com.spidervision.tv`
3. Test: Home key, cold boot, reboot, returning from Netflix/YouTube, Settings, input picker,
   screensaver wake, profile picker in Google TV apps.
4. Measure whether `launcherx` still runs and how much RAM is freed.

**Rollback A** (either one):
- On the TV: Settings → Apps → Default apps → Home app → Google TV (or `setupwraith`).
- From the PC:
  ```powershell
  .\adb.exe -s 192.168.1.79:5555 shell cmd role add-role-holder android.app.role.HOME com.google.android.tungsten.setupwraith
  ```
- Last resort: uninstall SpiderVision; Android falls back to the Google TV home.

## Step B — stop Google TV home from running at all (only if A works, separate approval)

`adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx`

Expected gain: the ~78–107 MB `launcherx` uses today. Expected losses while disabled: Google
TV's own home rows and recommendations, its profile switcher and its account/profile screens,
and anything that deep-links into the Google TV home. Streaming apps, Play Store, Settings,
casting, the screensaver (`dreamx`, separate package) and DNS are separate apps and stay.

**Rollback B:**
```powershell
.\adb.exe -s 192.168.1.79:5555 shell pm enable com.google.android.apps.tv.launcherx
.\adb.exe -s 192.168.1.79:5555 shell cmd role add-role-holder android.app.role.HOME com.google.android.tungsten.setupwraith
```

If the TV ever boots without a working Home app, Android's `FallbackHome` still appears and ADB
still works, so both rollbacks remain available.

## Results (2026-11-03, owner-approved, then rolled back)

Backup taken first: `platform-tools\backup-2026-11-03_home-phase6` (+ `ROLLBACK-HOME.ps1`,
also `tools/rollback-home.ps1`).

| Step | What happened |
| --- | --- |
| A: SpiderVision declares HOME, gets the HOME role | Role and preferred activity both moved to SpiderVision, but the Home key still started `launcherx/.home.HomeActivity` (explicit component, from the system). |
| A + reboot | At boot a Google TV component started `launcherx` directly, which showed its "Who's watching?" profile chooser; the Home key returned to it. |
| B: `launcherx` disabled | The Home key then started `setupwraith/.RecoveryActivity` (a bare Settings screen), not SpiderVision. The firmware picks the highest-priority **system** Home activity and ignores the role/preference for third-party apps. |
| Rollback | `ROLLBACK-HOME.ps1` re-enabled `launcherx` and returned the role to `setupwraith`. Verified against the backup: role, preferred activity, 19 disabled packages and Private DNS all identical; Home key opens Google TV again. |

Conclusion: on this Philips/Google TV build a third-party app can't become the Home button just by
holding the HOME role, and turning off the Google TV home alone hands Home to `setupwraith`.
SpiderVision stays a normal app (its HOME filter is harmless while it doesn't hold the role).

Remaining option, **not done** (needs separate approval): also disable
`com.google.android.tungsten.setupwraith`, leaving SpiderVision as the only Home candidate above
`FallbackHome`. Risks: Google TV's setup/recovery and account re-verification screens go away,
and a factory reset or major update may need both re-enabled. Rollback would re-enable both.
