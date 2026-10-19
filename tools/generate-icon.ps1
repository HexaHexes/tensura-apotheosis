# Generates the mod icon: dark slate tile, crossed blades, central faceted gem.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = 'C:\Games\Minecraft\Mods\tensura-apotheosis'
$out = Join-Path $root 'icon.png'

# Reuse the gem HSL helper (dot-source the texture script's functions by re-defining here).
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

function Pt([double]$x, [double]$y) { New-Object System.Drawing.PointF($x, $y) }

$size = 256
$bmp = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'
$g.Clear([System.Drawing.Color]::Transparent)

# background rounded tile
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$r = 40
$path.AddArc(0, 0, 2 * $r, 2 * $r, 180, 90)
$path.AddArc($size - 2 * $r, 0, 2 * $r, 2 * $r, 270, 90)
$path.AddArc($size - 2 * $r, $size - 2 * $r, 2 * $r, 2 * $r, 0, 90)
$path.AddArc(0, $size - 2 * $r, 2 * $r, 2 * $r, 90, 90)
$path.CloseFigure()
$bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 24, 26, 38))
$g.FillPath($bgBrush, $path)
$goldPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 212, 170, 70), 5)
$g.DrawPath($goldPen, $path)

# crossed blades (two rotated tapers behind the gem)
$bladeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 196, 202, 214))
$hiltBrush  = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 120, 84, 40))
foreach ($flip in @($false, $true)) {
    $st = $g.Save()
    $g.TranslateTransform(128, 128)
    if ($flip) { $g.ScaleTransform(-1, 1) }
    $g.RotateTransform(45)
    # blade (points up)
    $blade = @((Pt -7 -8), (Pt 7 -8), (Pt 4 70), (Pt -4 70))
    $g.FillPolygon($bladeBrush, $blade)
    # guard + grip below center
    $g.FillRectangle($hiltBrush, (New-Object System.Drawing.RectangleF(-22, 8, 44, 9)))
    $g.FillRectangle($hiltBrush, (New-Object System.Drawing.RectangleF(-5, 17, 10, 42)))
    $g.Restore($st)
}

# central faceted gem (scaled from the 16px design)
$st = $g.Save()
$g.TranslateTransform(128, 118)
$g.ScaleTransform(7.5, 7.5)
$hue = 18  # flame / dragonsteel orange-red
$base    = Get-HslColor $hue 0.70 0.48
$light   = Get-HslColor $hue 0.74 0.68
$lighter = Get-HslColor $hue 0.62 0.82
$dark    = Get-HslColor $hue 0.64 0.32
$darker  = Get-HslColor $hue 0.66 0.40
$edge    = Get-HslColor $hue 0.58 0.18
$pts = @((Pt 5 1), (Pt 10 1), (Pt 14 5), (Pt 10 14), (Pt 5 14), (Pt 1 5))
$pen = New-Object System.Drawing.Pen($edge, 1)
$g.DrawPolygon($pen, $pts)
$g.FillPolygon((New-Object System.Drawing.SolidBrush($base)), $pts)
$g.FillPolygon((New-Object System.Drawing.SolidBrush($light)), @((Pt 5 2), (Pt 10 2), (Pt 7 7), (Pt 4 5)))
$g.FillPolygon((New-Object System.Drawing.SolidBrush($dark)), @((Pt 10 2), (Pt 13 5), (Pt 7 7)))
$g.FillPolygon((New-Object System.Drawing.SolidBrush($darker)), @((Pt 4 5), (Pt 7 7), (Pt 10 14), (Pt 5 14), (Pt 2 5)))
$g.FillRectangle((New-Object System.Drawing.SolidBrush($lighter)), (New-Object System.Drawing.RectangleF(5, 3, 2, 2)))
$g.Restore($st)

$g.Dispose()
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
"icon.png written"
