<#
.SYNOPSIS
  Signs the release APK with SpiderVision's private release key and writes it, with its SHA-256,
  to release\ (git-ignored).

.DESCRIPTION
  Key material lives OUTSIDE the repository in %USERPROFILE%\.spidervision\:
    spidervision-release.jks       private release key (back this folder up somewhere safe)
    keystore.properties            its alias and password
    debug-to-release.lineage       proof that the old debug key hands over to the release key
  Thanks to the lineage (APK signature scheme v3 key rotation) the release build installs as an
  ordinary update over builds signed with this PC's debug key, keeping saved layouts and the
  Home role. Passwords are passed through an environment variable, never on the command line.

.EXAMPLE
  .\gradlew assembleRelease
  powershell -ExecutionPolicy Bypass -File tools\sign-release.ps1
#>
param(
    [string]$KeyDir = (Join-Path $env:USERPROFILE ".spidervision"),
    [string]$BuildTools = (Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\36.1.0")
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$unsigned = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"
if (-not (Test-Path $unsigned)) { throw "Build first: .\gradlew assembleRelease (expected $unsigned)" }

$props = @{}
Get-Content (Join-Path $KeyDir "keystore.properties") | ForEach-Object { if ($_ -match '^\s*([^=]+)=(.*)$') { $props[$Matches[1].Trim()] = $Matches[2].Trim() } }
$apksigner = Join-Path $BuildTools "apksigner.bat"
$debugKs = Join-Path $env:USERPROFILE ".android\debug.keystore"
$lineage = Join-Path $KeyDir "debug-to-release.lineage"
$env:SV_PW = $props["storePassword"]
try {
    if (-not (Test-Path $lineage)) {
        Write-Host "Creating the key-rotation lineage (debug key -> release key) ..."
        & $apksigner rotate --out $lineage `
            --old-signer --ks $debugKs --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android `
            --new-signer --ks $props["storeFile"] --ks-key-alias $props["keyAlias"] --ks-pass env:SV_PW --key-pass env:SV_PW
        if ($LASTEXITCODE -ne 0) { throw "apksigner rotate failed" }
    }
    $gradle = Get-Content (Join-Path $root "app\build.gradle.kts") -Raw
    $version = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
    $outDir = Join-Path $root "release"
    New-Item -ItemType Directory -Force $outDir | Out-Null
    $out = Join-Path $outDir "SpiderVision-$version.apk"
    Write-Host "Signing SpiderVision $version ..."
    & $apksigner sign `
        --ks $debugKs --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android `
        --next-signer --ks $props["storeFile"] --ks-key-alias $props["keyAlias"] --ks-pass env:SV_PW --key-pass env:SV_PW `
        --lineage $lineage --out $out $unsigned
    if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed" }
} finally {
    Remove-Item Env:SV_PW -ErrorAction SilentlyContinue
}
& $apksigner verify --verbose --print-certs $out | Select-String -Pattern "Verifies|Verified using|Number of signers|Signer #|SHA-256 digest" | ForEach-Object { "  " + $_.Line.Trim() }
$hash = (Get-FileHash $out -Algorithm SHA256).Hash.ToLower()
"$hash  $(Split-Path -Leaf $out)" | Set-Content -Encoding ascii "$out.sha256"
Write-Host "Wrote $out"
Write-Host "SHA-256 $hash"
