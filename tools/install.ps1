<#
.SYNOPSIS
  Installs the newest signed SpiderVision build from release\ on the TV as an update, then checks
  that it is still the Home app.

.DESCRIPTION
  Uses "adb install -r", so saved layouts, launch counts and a linked NextDNS key are kept. Checks
  the SHA-256 against the .sha256 file written by sign-release.ps1 before installing. Afterwards
  prints the installed version and who holds the HOME role. Nothing else on the TV is touched.

.EXAMPLE
  .\gradlew assembleRelease
  powershell -ExecutionPolicy Bypass -File tools\sign-release.ps1
  powershell -ExecutionPolicy Bypass -File tools\install.ps1
#>
param(
    [string]$Tv = "192.168.1.79:5555",
    [string]$Adb = "C:\Users\mahin\Desktop\platform-tools-latest-windows\platform-tools\adb.exe",
    [string]$Apk
)
# Windows PowerShell 5.1 treats adb's stderr chatter as errors under "Stop"; check output instead.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
if (-not $Apk) {
    $Apk = Get-ChildItem (Join-Path $root "release") -Filter "SpiderVision-*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $Apk -or -not (Test-Path $Apk)) { Write-Host "No signed APK in release\. Run tools\sign-release.ps1 first."; exit 1 }
if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found at $Adb (pass -Adb)."; exit 1 }

$sumFile = "$Apk.sha256"
if (Test-Path $sumFile) {
    $expected = ((Get-Content $sumFile -TotalCount 1) -split '\s+')[0]
    $actual = (Get-FileHash $Apk -Algorithm SHA256).Hash.ToLower()
    if ($expected -ne $actual) { Write-Host "SHA-256 mismatch for $Apk - not installing."; exit 1 }
    Write-Host "SHA-256 ok  $actual"
}

& $Adb connect $Tv | Out-Null
$state = (& $Adb -s $Tv get-state 2>$null)
if ($state -ne "device") { Write-Host "TV $Tv is not reachable (is it on and awake?)."; exit 1 }

Write-Host "Installing $(Split-Path -Leaf $Apk) on $Tv ..."
$result = (& $Adb -s $Tv install -r $Apk 2>&1) -join "`n"
Write-Host $result
if ($result -notmatch "Success") { exit 1 }

$version = (& $Adb -s $Tv shell dumpsys package com.spidervision.tv | Select-String "versionName=" | Select-Object -First 1).Line.Trim()
$holder = (& $Adb -s $Tv shell cmd role get-role-holders android.app.role.HOME) -join ""
Write-Host "Installed  $version"
Write-Host "HOME role  $holder"
if ($holder -ne "com.spidervision.tv") { Write-Host "Note: SpiderVision is not the Home app on this TV (see docs/HOME_EXPERIMENT.md)." }
