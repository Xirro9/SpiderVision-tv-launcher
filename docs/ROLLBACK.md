# Rollback

SpiderVision is installed as a **normal app**. It does not replace HOME, disable anything, or
change DNS or TV settings. Removing it fully restores the previous state.

## Remove SpiderVision

On the TV: Settings → Apps → SpiderVision → Uninstall.

Or from the PC, in the folder that contains `adb.exe`:

```powershell
.\adb.exe devices -l
.\adb.exe -s 192.168.1.79:5555 uninstall com.spidervision.tv
```

Only ever uninstall `com.spidervision.tv`. Never uninstall or disable a stock package.

## Confirm the TV is back to its baseline

```powershell
.\adb.exe -s 192.168.1.79:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
.\adb.exe -s 192.168.1.79:5555 shell pm list packages -d
```

The HOME result must still be `com.google.android.apps.tv.launcherx/.home.HomeActivity`, and
the disabled list must match `docs/baseline/disabled-packages.txt` (19 packages).

## If SpiderVision misbehaves while open

- Press Home on the remote: the stock Google TV home is still the default.
- Or force-stop it: `.\adb.exe -s 192.168.1.79:5555 shell am force-stop com.spidervision.tv`

## Earlier TV backups

The pre-existing backups in `platform-tools\backup-2026-11-02_2005` and the
`philips-*-before.txt` snapshots are outside this repo and are not modified by it.

## SpiderVision is the Home app (Phase 6, active since 2026-11-03)

The Google TV home (`launcherx`) and its setup helper (`setupwraith`) are disabled for user 0 so
that the Home button opens SpiderVision. To put Google TV back:

```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\mahin\Desktop\platform-tools-latest-windows\platform-tools\backup-2026-11-03_home-phase6\ROLLBACK-HOME.ps1"
```

Run it before a factory reset or if a Google TV update misbehaves. Without a PC: Settings → Apps →
See all apps → Show system apps → enable "Google TV Home" and the Google TV setup app, then
Settings → Apps → Default apps → Home app → Google TV. Details: `docs/HOME_EXPERIMENT.md`.

Uninstalling SpiderVision while it is the Home app leaves only Android's bare FallbackHome, so run
the rollback script first.
