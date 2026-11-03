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

$ErrorActionPreference = "Stop"
$receiver = "com.spidervision.tv/.network.ProvisionReceiver"

if (-not (Test-Path $Adb)) { throw "adb.exe not found at $Adb (pass -Adb)." }
& $Adb connect $Tv | Out-Null
$state = (& $Adb -s $Tv get-state 2>$null)
if ($state -ne "device") { throw "The TV at $Tv isn't reachable over ADB." }

if ($Unlink) {
    & $Adb -s $Tv shell am broadcast -n $receiver --ez nextdns_clear true | Select-String "data="
    Write-Host "SpiderVision's NextDNS link was removed from the TV."
    return
}

$secure = Read-Host "NextDNS API key (input hidden)" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $key = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr).Trim()
    if ($key -notmatch '^[A-Za-z0-9_-]{16,128}$') { throw "That doesn't look like a NextDNS API key." }
    $out = & $Adb -s $Tv shell am broadcast -n $receiver --es nextdns_key $key
    $result = ($out | Select-String 'data="([^"]*)"').Matches.Groups[1].Value
    if ($result -eq "linked") {
        Write-Host "Linked. Open SpiderVision's Home base tile and press OK, then 'Refresh now'."
    } else {
        Write-Host "SpiderVision didn't accept it: $result"
    }
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    $key = $null
}
