<#
.SYNOPSIS
  Link (or unlink) SpiderVision's Home base tile to your NextDNS account.

.DESCRIPTION
  Prompts for your NextDNS API key (my.nextdns.io -> Account -> API) without echoing it, and
  sends it straight to SpiderVision on the TV over ADB. The TV stores it encrypted in
  SpiderVision's private storage. The key is not written to disk on this PC, not stored in
  the repository, and not printed.

  Only the ADB shell can deliver it: SpiderVision accepts the key only from a sender holding
  WRITE_SECURE_SETTINGS, which ordinary TV apps cannot get.

.EXAMPLE
  .\link-nextdns.ps1
  .\link-nextdns.ps1 -Unlink
  .\link-nextdns.ps1 -Tv 192.168.1.79:5555 -Adb C:\path\to\adb.exe
#>
param(
    [string]$Tv = "192.168.1.79:5555",
    [string]$Adb = "C:\Users\mahin\Desktop\platform-tools-latest-windows\platform-tools\adb.exe",
    [switch]$Unlink
)

# Windows PowerShell 5.1 turns anything adb prints on stderr (e.g. "daemon started") into a
# terminating error under "Stop", so native calls run with "Continue" and are checked by output.
$ErrorActionPreference = "Continue"
$receiver = "com.spidervision.tv/.network.ProvisionReceiver"

if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found at $Adb (pass -Adb)."; exit 1 }
Write-Host "1/3 Connecting to the TV at $Tv ..."
& $Adb connect $Tv | Out-Null
$state = (& $Adb -s $Tv get-state) | Select-Object -Last 1
if ("$state".Trim() -ne "device") { Write-Host "The TV at $Tv isn't reachable over ADB (state: '$state')."; exit 1 }
Write-Host "    connected."

if ($Unlink) {
    & $Adb -s $Tv shell am broadcast -n $receiver --ez nextdns_clear true | Select-String "data="
    Write-Host "SpiderVision's NextDNS link was removed from the TV."
    return
}

Write-Host "2/3 Paste your NextDNS API key (my.nextdns.io > Account > API). Nothing will show as you paste."
$secure = Read-Host "NextDNS API key" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $key = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr).Trim()
    if ($key -notmatch '^[A-Za-z0-9_-]{16,128}$') {
        Write-Host "That doesn't look like a NextDNS API key ($($key.Length) characters). Nothing was sent."
        exit 1
    }
    Write-Host "3/3 Sending it to SpiderVision on the TV ..."
    $out = (& $Adb -s $Tv shell am broadcast -n $receiver --es nextdns_key $key) | Out-String
    $m = [regex]::Match($out, 'data="([^"]*)"')
    if ($m.Success -and $m.Groups[1].Value -eq "linked") {
        Write-Host "Linked. On the TV open the Home base tile, press OK, then 'Refresh now'."
    } elseif ($m.Success) {
        Write-Host "SpiderVision didn't accept it: $($m.Groups[1].Value)"
    } else {
        Write-Host "No answer from SpiderVision. Is version 0.4.0 installed? adb said: $($out.Trim())"
    }
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    $key = $null
}
