# Rollback

Since Phase 6 (2026-11-03) SpiderVision is the TV's **Home app**: the Google TV home
(`launcherx`) and its setup helper (`setupwraith`) are disabled for user 0 so that the Home button
opens SpiderVision. Nothing else is changed — no DNS, TV settings or other packages.

## Put Google TV home back

From the repo, with the TV on and reachable over ADB:

```powershell
powershell -ExecutionPolicy Bypass -File tools\rollback-home.ps1
```

It re-enables `launcherx` and `setupwraith`, gives the HOME role back to `setupwraith`, and compares
the result with the backup taken before Phase 6. The same script is also kept beside that backup:
`platform-tools\backup-2026-11-03_home-phase6\ROLLBACK-HOME.ps1`.

Run it **before a factory reset** or if a Google TV update misbehaves.

Without a PC: Settings → Apps → See all apps → Show system apps → enable "Google TV Home" and the
Google TV setup app, then Settings → Apps → Default apps → Home app → Google TV.

Details of how Home was switched: `docs/HOME_EXPERIMENT.md`.

## Remove SpiderVision completely

Restore Google TV home **first** — uninstalling SpiderVision while it is the Home app leaves only
Android's bare FallbackHome. The script can do both:

```powershell
powershell -ExecutionPolicy Bypass -File tools\rollback-home.ps1 -UninstallSpiderVision
```

Only ever uninstall `com.spidervision.tv`. Never uninstall or disable a stock package by hand.

## Confirm the TV is back to its baseline

```powershell
.\adb.exe -s 192.168.1.79:5555 shell cmd role get-role-holders android.app.role.HOME
.\adb.exe -s 192.168.1.79:5555 shell pm list packages -d
```

After rollback the Home role is `com.google.android.tungsten.setupwraith`, and the disabled list
matches `docs/baseline/disabled-packages.txt` (19 packages).

## If SpiderVision misbehaves while open

- Force-stop it; Android restarts it as Home:
  `.\adb.exe -s 192.168.1.79:5555 shell am force-stop com.spidervision.tv`
- Reinstall the last good build: `tools\install.ps1 -Apk <path to an earlier signed APK>`
- Or run the rollback above.

## Earlier TV backups

The pre-existing backups in `platform-tools\backup-2026-11-02_2005`, the `philips-*-before.txt`
snapshots and `platform-tools\backup-2026-11-03_home-phase6` are outside this repo and are not
modified by it.
