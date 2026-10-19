# Generates 16x16 faceted-gem textures for the 14 Tensura Apotheosis gems.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$out = 'C:\Games\Minecraft\Mods\tensura-apotheosis\src\main\resources\assets\tensura_apotheosis\textures\items\gems'
New-Item -ItemType Directory -Path $out -Force | Out-Null

function Get-HslColor([double]$h, [double]$s, [double]$l, [int]$a = 255) {
    $h = $h % 360
    if ($h -lt 0) { $h += 360 }
    $c = (1 - [math]::Abs(2 * $l - 1)) * $s
    $hp = $h / 60.0
    $x = $c * (1 - [math]::Abs(($hp % 2) - 1))
    $r1 = 0.0; $g1 = 0.0; $b1 = 0.0
    if ($hp -lt 1)       { $r1 = $c; $g1 = $x }
    elseif ($hp -lt 2)   { $r1 = $x; $g1 = $c }
    elseif ($hp -lt 3)   { $g1 = $c; $b1 = $x }
    elseif ($hp -lt 4)   { $g1 = $x; $b1 = $c }
    elseif ($hp -lt 5)   { $r1 = $x; $b1 = $c }
    else                 { $r1 = $c; $b1 = $x }
    $m = $l - $c / 2
    $R = [int][math]::Round(255 * ($r1 + $m))
    $G = [int][math]::Round(255 * ($g1 + $m))
    $B = [int][math]::Round(255 * ($b1 + $m))
    return [System.Drawing.Color]::FromArgb($a, $R, $G, $B)
}

function Pt([int]$x, [int]$y) { New-Object System.Drawing.Point($x, $y) }

function Draw-Gem([string]$path, [double]$hue) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'None'
    $g.Clear([System.Drawing.Color]::Transparent)

    $base    = Get-HslColor $hue 0.68 0.46
    $light   = Get-HslColor $hue 0.72 0.66
    $lighter = Get-HslColor $hue 0.60 0.80
    $dark    = Get-HslColor $hue 0.62 0.30
    $darker  = Get-HslColor $hue 0.64 0.38
    $edge    = Get-HslColor $hue 0.55 0.16

    # gem outline polygon on the 16px grid
    $pts = @((Pt 5 1), (Pt 10 1), (Pt 14 5), (Pt 10 14), (Pt 5 14), (Pt 1 5))
    $pen = New-Object System.Drawing.Pen($edge, 1)
    $g.DrawPolygon($pen, $pts)

    # body
    $g.FillPolygon((New-Object System.Drawing.SolidBrush($base)), $pts)

    # top facet (light)
    $g.FillPolygon((New-Object System.Drawing.SolidBrush($light)), @((Pt 5 2), (Pt 10 2), (Pt 7 7), (Pt 4 5)))

    # right facet (dark)
    $g.FillPolygon((New-Object System.Drawing.SolidBrush($dark)), @((Pt 10 2), (Pt 13 5), (Pt 7 7)))

    # bottom facet
    $g.FillPolygon((New-Object System.Drawing.SolidBrush($darker)), @((Pt 4 5), (Pt 7 7), (Pt 10 14), (Pt 5 14), (Pt 2 5)))

    # specular highlight
    $g.FillRectangle((New-Object System.Drawing.SolidBrush($lighter)), (New-Object System.Drawing.Rectangle(5, 3, 2, 2)))

    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$gems = [ordered]@{
    'magicule'     = 215   # blue
    'aura'         = 48    # gold
    'soul'         = 150   # pale green
    'predator'     = 0     # crimson
    'doppelganger' = 275   # purple
    'analyst'      = 190   # cyan
    'greedy'       = 28    # orange
    'swift'        = 100   # lime
    'flame'        = 18    # orange-red
    'frost'        = 200   # ice blue
    'storm'        = 265   # violet
    'holy'         = 44    # pale gold
    'spirit'       = 165   # mint
    'ultima'       = 315   # magenta
}
foreach ($name in $gems.Keys) {
    Draw-Gem (Join-Path $out "$name.png") $gems[$name]
    "gems/$name.png"
}
