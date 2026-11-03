<#
.SYNOPSIS
  Crops and resizes the owner-supplied artwork (art-src\) into small, display-sized JPEGs in
  app\src\main\res\drawable-nodpi. The TV then only ever decodes images at the size they appear.

  Each row: output name, source file, crop centre (cx, cy as fractions), crop width fraction,
  output width x height. The crop keeps the output's aspect ratio.
#>
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "art-src"
$out = Join-Path $root "app\src\main\res\drawable-nodpi"

$jobs = @(
    # --- app tiles (4:3, 400x300) ---
    @("art_netflix",     "mask-reach.jpg",        0.55, 0.62, 0.95, 400, 300),
    @("art_youtube",     "red-symbiote.jpg",      0.50, 0.24, 0.95, 400, 300),
    @("art_prime",       "streak-swing.jpg",      0.62, 0.62, 0.95, 400, 300),
    @("art_disney",      "blue-symbiote.jpg",     0.50, 0.28, 0.95, 400, 300),
    @("art_max",         "spidey-gwen-split.jpg", 0.74, 0.44, 0.55, 400, 300),
    @("art_crunchyroll", "miles-hood-jump.jpg",   0.52, 0.30, 0.95, 400, 300),
    @("art_spotify",     "miles-bugle.jpg",       0.50, 0.33, 0.95, 400, 300),
    @("art_ytmusic",     "spidey-gwen-split.jpg", 0.26, 0.44, 0.55, 400, 300),
    @("art_hulu",        "miles-bugle.jpg",       0.50, 0.78, 0.95, 400, 300),
    @("art_appletv",     "blue-symbiote.jpg",     0.62, 0.56, 0.95, 400, 300),
    @("art_peacock",     "red-symbiote.jpg",      0.50, 0.60, 0.95, 400, 300),
    @("art_googletv",    "streak-swing.jpg",      0.62, 0.24, 0.95, 400, 300),
    @("art_tubi",        "miles-hood-jump.jpg",   0.40, 0.78, 0.95, 400, 300),
    @("art_pluto",       "mask-reach.jpg",        0.50, 0.18, 0.95, 400, 300),
    @("art_roku",        "red-symbiote.jpg",      0.50, 0.80, 0.95, 400, 300),
    @("art_instagram",   "spidey-gwen-split.jpg", 0.50, 0.14, 0.95, 400, 300),
    @("art_applemusic",  "miles-bugle.jpg",       0.50, 0.12, 0.95, 400, 300),
    @("art_aircast",     "streak-swing.jpg",      0.30, 0.86, 0.95, 400, 300),
    # --- utility panels, sized to their board cells ---
    @("panel_settings",  "blue-symbiote.jpg",     0.50, 0.40, 1.00, 400, 380),
    @("panel_inputs",    "mask-reach.jpg",        0.36, 0.30, 0.42, 200, 400),
    @("panel_allapps",   "streak-swing.jpg",      0.50, 0.42, 1.00, 640, 200),
    @("panel_homebase",  "miles-bugle.jpg",       0.50, 0.30, 1.00, 440, 400),
    # --- rotating cover (portrait 600x860) ---
    @("cover_netflix",     "hood-collage.jpg",      0.50, 0.42, 1.00, 600, 860),
    @("cover_youtube",     "red-symbiote.jpg",      0.50, 0.45, 1.00, 600, 860),
    @("cover_disney",      "blue-symbiote.jpg",     0.50, 0.40, 1.00, 600, 860),
    @("cover_prime",       "streak-swing.jpg",      0.50, 0.50, 1.00, 600, 860),
    @("cover_crunchyroll", "miles-hood-jump.jpg",   0.50, 0.45, 1.00, 600, 860),
    @("cover_max",         "spidey-gwen-split.jpg", 0.50, 0.42, 1.00, 600, 860)
)

$codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$params = New-Object System.Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), 82L

foreach ($j in $jobs) {
    $name, $file, $cx, $cy, $wf, $ow, $oh = $j
    $img = [System.Drawing.Image]::FromFile((Join-Path $src $file))
    $W = $img.Width; $H = $img.Height
    $cw = [double]$W * $wf
    $ch = $cw * $oh / $ow
    if ($ch -gt $H) { $ch = [double]$H; $cw = $ch * $ow / $oh }
    $x = [Math]::Min([Math]::Max(0, $cx * $W - $cw / 2), $W - $cw)
    $y = [Math]::Min([Math]::Max(0, $cy * $H - $ch / 2), $H - $ch)
    $bmp = New-Object System.Drawing.Bitmap $ow, $oh
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $srcRect = New-Object System.Drawing.Rectangle ([int][Math]::Round($x)), ([int][Math]::Round($y)), ([int][Math]::Floor($cw)), ([int][Math]::Floor($ch))
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $ow, $oh), $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $path = Join-Path $out "$name.jpg"
    $bmp.Save($path, $codec, $params)
    $g.Dispose(); $bmp.Dispose(); $img.Dispose()
    "{0,-18} {1,4}x{2,-4} {3,5} KB" -f $name, $ow, $oh, [int]((Get-Item $path).Length / 1KB)
}
