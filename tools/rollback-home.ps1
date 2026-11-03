<#
.SYNOPSIS
  Put the TV's Home button back to Google TV, exactly as it was before Phase 6.

.DESCRIPTION
  Undoes both Phase 6 steps and checks the result against the backup taken beforehand
  (platform-tools\backup-2026-11-03_home-phase6):
    1. re-enables the Google TV home (com.google.android.apps.tv.launcherx)
    2. gives the HOME role back to com.google.android.tungsten.setupwraith
    3. compares Home role, Google TV home state and the disabled-apps list with the backup
  SpiderVision stays installed as a normal app unless -UninstallSpiderVision is given.
  Nothing else on the TV is touched.

.EXAMPLE
  .\rollback-home.ps1
  .\rollback-home.ps1 -UninstallSpiderVision
#>
param(
    [string]$Tv = "192.168.1.79:5555",
    [string]$Adb = "C:\Users\mahin\Desktop\platform-tools-latest-windows\platform-tools\adb.exe",
    [string]$Backup = "C:\Users\mahin\Desktop\platform-tools-latest-windows\platform-tools\backup-2026-11-03_home-phase6",
    [switch]$UninstallSpiderVision
)
$ErrorActionPreference = "Continue"
function Adb { & $Adb -s $Tv @args }

& $Adb connect $Tv | Out-Null
if ("$(Adb get-state)".Trim() -ne "device") { Write-Host "The TV at $Tv isn't reachable over ADB."; exit 1 }

Write-Host "1/4 Re-enabling the Google TV home ..."
Adb shell pm enable com.google.android.apps.tv.launcherx | Out-Host

Write-Host "2/4 Giving the Home button back to Google TV ..."
Adb shell cmd role remove-role-holder android.app.role.HOME com.spidervision.tv | Out-Null
Adb shell cmd role add-role-holder android.app.role.HOME com.google.android.tungsten.setupwraith | Out-Host

if ($UninstallSpiderVision) {
    Write-Host "3/4 Uninstalling SpiderVision ..."
    Adb uninstall com.spidervision.tv | Out-Host
} else {
    Write-Host "3/4 Keeping SpiderVision installed as a normal app."
}

Write-Host "4/4 Checking against the backup ..."
$ok = $true
$role = "$(Adb shell cmd role get-role-holders android.app.role.HOME)".Trim()
$roleWas = (Get-Content "$Backup\home-role-holder.txt" -Raw).Trim()
Write-Host "    Home role: $role (before Phase 6: $roleWas)"
if ($role -ne $roleWas) { $ok = $false }
$lx = "$(Adb shell pm list packages -e com.google.android.apps.tv.launcherx)".Trim()
Write-Host "    Google TV home enabled: $([bool]$lx)"
if (-not $lx) { $ok = $false }
$now = (Adb shell pm list packages -d) | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object
$was = Get-Content "$Backup\disabled-packages.txt" | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object
$diff = Compare-Object $was $now
if ($diff) { Write-Host "    Disabled apps differ from the backup:"; $diff | Out-Host; $ok = $false }
else { Write-Host "    Disabled apps: same $($now.Count) as the backup" }

Adb shell input keyevent KEYCODE_HOME | Out-Null
if ($ok) { Write-Host "Done. The Home button opens Google TV again." }
else { Write-Host "Rolled back, but something differs from the backup (see above)." }
