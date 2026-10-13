# Renders the three questbook layout diagrams that the "Beyond the Stars" chapter embeds
# with BetterQuesting's [img height=N]domain:path[/img] tag.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File gen-diagrams.ps1
#
# Output: .\resources\btsquests\textures\gui\*.png, and the same files in the jar's own resources
# (src\main\resources\assets\btsquests\textures\gui\): a mod jar serves every assets\ domain it
# holds, so the book the jar installs (overlay.json, the plant chapter included) finds its
# diagrams with no manual merge (round-2 screenshots: without that copy the three [img] tags drew
# as missing-texture squares). The questbook\resources copy is for the manual merge only: those
# files merge into <instance>/.minecraft/config/betterquesting/resources/, which
# BetterQuesting registers as a folder resource pack (betterquesting.misc.QuestResourcesFolder,
# added to Minecraft's defaultResourcePacks): a resource location "btsquests:textures/gui/x.png"
# resolves to <root>/btsquests/textures/gui/x.png. The domain folder name MUST be lowercase.
#
# Geometry is the canonical one in docs/plant-geometry.md, drawn at Build Tier 3 (edge 13).
# Images are 380 px wide on purpose: GuiQuest always draws a description image at the
# description canvas's width with the aspect maintained, so a source much wider than the
# canvas would shrink the labels into mush.
#
# D-20: the Automation Bay is gone and every socket moved onto the building it serves, so
# diagram 2 is now the POINT OF USE map (silo face + spiral chamber front wall) and
# diagram 3 grew the Energy Hatch course it inherited.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot 'resources\btsquests\textures\gui'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force $outDir | Out-Null }
$jarDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\btsquests\textures\gui'
if (-not (Test-Path $jarDir)) { New-Item -ItemType Directory -Force $jarDir | Out-Null }

# The bay diagram is retired. Leaving it in the folder would ship a texture that contradicts
# the book, so it is removed rather than merely unreferenced.
$stale = Join-Path $outDir 'bay-sockets.png'
if (Test-Path $stale) { Remove-Item -Force $stale; Write-Host "removed stale $stale" }
$staleJar = Join-Path $jarDir 'bay-sockets.png'
if (Test-Path $staleJar) { Remove-Item -Force $staleJar; Write-Host "removed stale $staleJar" }

# ------------------------------------------------- D5: the Energy Hatch caption, DERIVED
# The manifold legend used to caption the E row 'OPTIONAL - the plant never draws EU'. That
# was true until D-17 hung the Extraction Derrick and the Enrichment Annex off those very
# hatches. Step 9's prose and the Turbine blueprint were corrected; this IMAGE, embedded two
# lines below them, was not - so the shipped PNG contradicted the text it sat next to.
#
# The rate is now never typed here either. It is read off the same two Java constants
# gen-questbook.ps1 reads, by the same rule, so a tier change in the Java re-renders this
# diagram with the new figure or throws on the next run.
$script:CoreJava = Join-Path (Split-Path $PSScriptRoot -Parent) `
    'src\main\java\com\nateyburr\beyondthestars\machines\MTEReactorCoreMk1.java'
# TierEU.RECIPE_<tier> is GTValues.VP[tier], and VP[i] = V[i] * 30 / 32 - the familiar
# 30 / 120 / 480 / 1,920 / 7,680 recipe ladder.
$TIER_EU_RECIPE = @{
    'RECIPE_ULV' = 7; 'RECIPE_LV' = 30; 'RECIPE_MV' = 120; 'RECIPE_HV' = 480
    'RECIPE_EV' = 1920; 'RECIPE_IV' = 7680; 'RECIPE_LuV' = 30720; 'RECIPE_ZPM' = 122880
    'RECIPE_UV' = 491520
}
function ModuleEuPerTick([string]$constant) {
    $core = [System.IO.File]::ReadAllText($script:CoreJava)
    $m = [regex]::Match($core, "public static final long $constant = TierEU\.(?<tier>\w+);")
    if (-not $m.Success) {
        throw ("gen-diagrams.ps1: MTEReactorCoreMk1.$constant is no longer written as " +
            "'public static final long $constant = TierEU.<tier>;'. The manifold diagram's " +
            'Energy Hatch caption is read off that line and can no longer be derived.')
    }
    $tier = $m.Groups['tier'].Value
    if (-not $TIER_EU_RECIPE.ContainsKey($tier)) {
        throw "gen-diagrams.ps1: $constant now reads TierEU.$tier, which this script has no EU/t value for. Add it to the `$TIER_EU_RECIPE table above."
    }
    $TIER_EU_RECIPE[$tier]
}
$DERRICK_EU = ModuleEuPerTick 'DERRICK_EU_PER_TICK'
$ANNEX_EU = ModuleEuPerTick 'ANNEX_EU_PER_TICK'
$ENERGY_CAPTION = 'Derrick {0:N0} + Annex {1:N0} EU/t draw HERE' -f $DERRICK_EU, $ANNEX_EU

# ------------------------------------------------------------------ palette (dark book)
$C = @{
    bg        = '#0E1219'
    panel     = '#151B25'
    grid      = '#232C3A'
    text      = '#E6ECF2'
    dim       = '#93A1B0'
    title     = '#FFC94D'
    concrete  = '#2C333C'
    shield    = '#4C452F'
    stainless = '#B8BFC4'
    radproof  = '#7E8654'
    tt        = '#3A5A7A'
    steel     = '#4A525C'
    ctrl      = '#E8622A'
    energy    = '#D9A521'
    dynamo    = '#D94A21'
    laser     = '#D921D9'
    maint     = '#C87A4A'
    panelblk  = '#E8E8E8'
    fluidin   = '#21B8D9'
    fluidout  = '#21D98A'
    busin     = '#8FD921'
    busout    = '#D9D021'
}

function Brush([string]$hex) {
    New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($hex))
}
function Pen([string]$hex, [single]$w = 1) {
    New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml($hex), $w)
}

$fTitle = New-Object System.Drawing.Font 'Segoe UI', 13, ([System.Drawing.FontStyle]::Bold)
$fSub = New-Object System.Drawing.Font 'Segoe UI', 8.5, ([System.Drawing.FontStyle]::Regular)
$fLeg = New-Object System.Drawing.Font 'Segoe UI', 9, ([System.Drawing.FontStyle]::Regular)
$fLegB = New-Object System.Drawing.Font 'Segoe UI', 9, ([System.Drawing.FontStyle]::Bold)
$fCell = New-Object System.Drawing.Font 'Segoe UI', 8, ([System.Drawing.FontStyle]::Bold)
$fTiny = New-Object System.Drawing.Font 'Segoe UI', 7.5, ([System.Drawing.FontStyle]::Regular)
$fNum = New-Object System.Drawing.Font 'Segoe UI', 10, ([System.Drawing.FontStyle]::Bold)

function NewCanvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml($C.bg))
    @{ Bmp = $bmp; G = $g; W = $w; H = $h }
}

function Head($cv, [string]$title, [string]$sub) {
    $cv.G.FillRectangle((Brush $C.panel), 0, 0, $cv.W, 34)
    $cv.G.FillRectangle((Brush $C.title), 0, 33, $cv.W, 1)
    $cv.G.DrawString($title, $fTitle, (Brush $C.title), 10, 3)
    $cv.G.DrawString($sub, $fSub, (Brush $C.dim), 11, 19)
}

function Save($cv, [string]$name) {
    $path = Join-Path $outDir $name
    $cv.G.Dispose()
    $cv.Bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $cv.Bmp.Dispose()
    Copy-Item -Force $path (Join-Path $jarDir $name)
    Write-Host "wrote $path (and the jar's copy)"
}

function CenterText($g, [string]$s, $font, $brush, [single]$cx, [single]$cy) {
    $sz = $g.MeasureString($s, $font)
    $g.DrawString($s, $font, $brush, ($cx - $sz.Width / 2), ($cy - $sz.Height / 2))
}

# =====================================================================================
# 1. PLANT OVERVIEW - plan view of the 50x50 platform at Build Tier 3 (edge 13).
# =====================================================================================
# Platform frame: a 0..49 left->right, c 0..49 with the FRONT edge (c = 0) at the BOTTOM.
# Everything below is read straight off docs/plant-geometry.md with n = edge = 13, n/2 = 6.
#
# C3. The front edge HAS to be the bottom one, and not only because that is where the
# "FRONT (you stand here)" caption is: it is the only way round that reads true. The reader
# is standing at the caption looking INTO the page, so page-right is their right hand. `a`
# is the controller's relative LEFT (ExtendedFacing's `a` field is getRelativeLeftInWorld -
# verified against StructureLib 1.4.38: facing NORTH gives a = WEST, c = SOUTH), and the
# left of a machine you are facing is YOUR right - so a increasing to the page's right is
# exactly what that reader sees. Put c = 0 at the top instead and the reader is standing
# behind the plant: the cooling towers and the rotor gallery swap sides, and the caption is
# on the back edge as well. One flip fixes both.
# Cell rows and cell EDGES are two different coordinates, so there are two helpers: PlanEdge
# takes a boundary 0..50, PlanRow takes a row index 0..49 and returns its top edge.
$W = 380
$cell = 6.0
$planX = 40.0
$planY = 44.0
$cv = NewCanvas $W 490
Head $cv 'NUCLEAR PLANT - OVERVIEW' 'plan view, Build Tier 3 - one projection builds all of it'

# c = 0 (FRONT) is the bottom edge: boundary c -> planY + (50 - c) * cell.
function PlanEdge([double]$cBoundary) { $script:planY + (50 - $cBoundary) * $script:cell }
function PlanRow([double]$c) { PlanEdge ($c + 1) }

$g = $cv.G
# platform floor
$g.FillRectangle((Brush $C.concrete), $planX, $planY, 50 * $cell, 50 * $cell)
# shield pad octagon footprint (a,c 17..33)
$g.FillRectangle((Brush $C.shield), ($planX + 17 * $cell), (PlanRow 33), 17 * $cell, 17 * $cell)

function Box([double]$a0, [double]$a1, [double]$c0, [double]$c1, [string]$fill, [string]$edge) {
    $x = $planX + $a0 * $cell
    $y = PlanRow $c1
    $w = ($a1 - $a0 + 1) * $cell
    $h = ($c1 - $c0 + 1) * $cell
    $script:g.FillRectangle((Brush $fill), $x, $y, $w, $h)
    if ($edge) { $script:g.DrawRectangle((Pen $edge 1), $x, $y, $w, $h) }
    @{ CX = $x + $w / 2; CY = $y + $h / 2 }
}

# cooling towers (NW a0..16 c0..16, SW a0..16 c33..49)
$twNW = Box 0 16 0 16 $C.steel '#5F6873'
$twSW = Box 0 16 33 49 $C.steel '#5F6873'
# silo octagon a19..31, c17..29 (drawn as an octagon with a 3-cell corner cut)
$k = 3
$sa0 = 19; $sa1 = 31; $sc0 = 17; $sc1 = 29
$pts = @(
    (New-Object System.Drawing.PointF (($planX + ($sa0 + $k) * $cell), (PlanEdge $sc0))),
    (New-Object System.Drawing.PointF (($planX + ($sa1 + 1 - $k) * $cell), (PlanEdge $sc0))),
    (New-Object System.Drawing.PointF (($planX + ($sa1 + 1) * $cell), (PlanEdge ($sc0 + $k)))),
    (New-Object System.Drawing.PointF (($planX + ($sa1 + 1) * $cell), (PlanEdge ($sc1 + 1 - $k)))),
    (New-Object System.Drawing.PointF (($planX + ($sa1 + 1 - $k) * $cell), (PlanEdge ($sc1 + 1)))),
    (New-Object System.Drawing.PointF (($planX + ($sa0 + $k) * $cell), (PlanEdge ($sc1 + 1)))),
    (New-Object System.Drawing.PointF (($planX + $sa0 * $cell), (PlanEdge ($sc1 + 1 - $k)))),
    (New-Object System.Drawing.PointF (($planX + $sa0 * $cell), (PlanEdge ($sc0 + $k))))
)
$g.FillPolygon((Brush $C.radproof), $pts)
$g.DrawPolygon((Pen '#A6B072' 1), $pts)
$siloCX = $planX + 25 * $cell
$siloCY = PlanEdge 23.5
# spine duct a33..42, c22..24
$spine = Box 33 42 22 24 $C.tt '#5C86B5'
# spiral a43..49, c20..26
$spiral = Box 43 49 20 26 $C.tt '#5C86B5'
# rotor gallery a43..49, c27..41
$gallery = Box 43 49 27 41 $C.tt '#5C86B5'
# output manifold a43..49, c42..44
$manifold = Box 43 49 42 44 $C.tt '#5C86B5'
# the controller cell (a25, c17) - drawn oversized so it reads at 6 px per block
$ctrlX = $planX + 25 * $cell
$ctrlY = PlanRow 17
$g.FillRectangle((Brush $C.ctrl), ($ctrlX - 3), ($ctrlY - 3), $cell + 6, $cell + 6)
$g.DrawRectangle((Pen '#FFFFFF' 1), ($ctrlX - 3), ($ctrlY - 3), $cell + 6, $cell + 6)

# platform border + "FRONT" marker
$g.DrawRectangle((Pen '#4A5666' 1), $planX, $planY, 50 * $cell, 50 * $cell)
$g.DrawString('FRONT  (you stand here, facing the silo face)', $fTiny, (Brush $C.dim), ($planX + 30), ($planY + 50 * $cell + 3))
$g.DrawString('50 x 50 platform  |  BACK edge', $fTiny, (Brush $C.dim), $planX, ($planY - 12))

# numbered callouts
function Tag([double]$cx, [double]$cy, [string]$n) {
    $r = 8.0
    $script:g.FillEllipse((Brush '#0E1219'), ($cx - $r), ($cy - $r), 2 * $r, 2 * $r)
    $script:g.DrawEllipse((Pen $C.title 1.4), ($cx - $r), ($cy - $r), 2 * $r, 2 * $r)
    CenterText $script:g $n $fNum (Brush $C.title) $cx ($cy - 1)
}
Tag $twNW.CX $twNW.CY '7'
Tag $twSW.CX $twSW.CY '7'
Tag $siloCX $siloCY '2'
Tag $spine.CX $spine.CY '3'
Tag $spiral.CX $spiral.CY '4'
Tag $gallery.CX $gallery.CY '5'
Tag $manifold.CX $manifold.CY '6'
# the controller is one block, so its callout sits clear of the silo with a leader line
$tag1X = $planX + 36 * $cell
$tag1Y = $ctrlY + $cell / 2
$g.DrawLine((Pen $C.title 1), ($ctrlX + $cell + 4), $tag1Y, ($tag1X - 9), $tag1Y)
Tag $tag1X $tag1Y '1'

# legend
$ly = $planY + 50 * $cell + 20
$legend = @(
    @('1', $C.ctrl, 'FOUNDATION', 'the only controller'),
    @('2', $C.radproof, 'REACTOR SILO', 'silo + 45-201 rod pillars'),
    @('3', $C.tt, 'SPINE DUCT', '4 housings: speed vs eff.'),
    @('4', $C.tt, 'SPIRAL CHAMBER', '8 injectors, +/-2t window'),
    @('5', $C.tt, 'ROTOR GALLERY', '8 turbine rotors'),
    @('6', $C.tt, 'OUTPUT MANIFOLD', 'dynamos + energy + lasers'),
    @('7', $C.steel, 'COOLING TOWERS x2', 'water x0.5, decay x2')
)
foreach ($row in $legend) {
    $g.FillRectangle((Brush $row[1]), 12, ($ly + 2), 9, 9)
    $g.DrawRectangle((Pen '#000000' 1), 12, ($ly + 2), 9, 9)
    $g.DrawString($row[0], $fLegB, (Brush $C.title), 26, $ly)
    $g.DrawString($row[2], $fLegB, (Brush $C.text), 38, $ly)
    $sz = $g.MeasureString($row[2], $fLegB)
    $g.DrawString($row[3], $fTiny, (Brush $C.dim), (38 + $sz.Width), ($ly + 2))
    $ly += 14
}
$g.DrawString('No plumbing wall: every inlet is cut into the building that uses it.', $fTiny, (Brush $C.dim), 12, ($ly + 4))
$g.DrawString('The silo face looks out over open platform at every tier.', $fTiny, (Brush $C.dim), 12, ($ly + 16))
Save $cv 'plant-overview.png'

# =====================================================================================
# 2. THE POINT OF USE MAP - the silo face and the spiral chamber's front wall.
# =====================================================================================
# Silo face: the octagon's front row at tier 3 spans platform a 22..28 (7 flat cells); the
# six sockets sit one cell either side of the controller's own column, a 24..26. Chamber
# front wall: platform a 43..49, with COOLANT and PROPELLANT at a 44 and a 48, world y 1.
$W = 380
$cv = NewCanvas $W 478
Head $cv 'POINT OF USE - WHERE EVERYTHING GOES IN' 'each inlet is cut into the building it feeds, on the course it feeds'
$g = $cv.G

$cw = 40.0; $ch = 26.0
$gx = (380 - 7 * $cw) / 2

$cellColor = @{
    'ROD' = $C.busin; 'DEP' = $C.busout; 'H2O' = $C.fluidin; 'OUT' = $C.fluidout;
    'SILO' = $C.panelblk; 'MNT' = $C.maint; 'CTRL' = $C.ctrl;
    'COOL' = $C.fluidin; 'PROP' = $C.fluidin
}

function Face($rows, [double]$gy, [string]$wallColor, [string]$wallEdge, [int]$a0) {
    $ry = $gy
    foreach ($r in $rows) {
        $script:g.DrawString(("y" + $r.Y), $fCell, (Brush $C.dim), ($script:gx - 24), ($ry + 6))
        for ($i = 0; $i -lt 7; $i++) {
            $x = $script:gx + $i * $script:cw
            $code = $r.Cells[$i]
            if ($code -eq '') {
                $script:g.FillRectangle((Brush $wallColor), $x, $ry, $script:cw - 2, $script:ch - 2)
                $script:g.DrawRectangle((Pen $wallEdge 1), $x, $ry, $script:cw - 2, $script:ch - 2)
            } else {
                $script:g.FillRectangle((Brush $script:cellColor[$code]), $x, $ry, $script:cw - 2, $script:ch - 2)
                $script:g.DrawRectangle((Pen '#101010' 1.4), $x, $ry, $script:cw - 2, $script:ch - 2)
                CenterText $script:g $code $fCell (Brush '#101010') ($x + ($script:cw - 2) / 2) ($ry + ($script:ch - 2) / 2)
            }
        }
        $ry += $script:ch
    }
    for ($i = 0; $i -lt 7; $i++) {
        CenterText $script:g ([string]($a0 + $i)) $fTiny (Brush $C.dim) ($script:gx + $i * $script:cw + ($script:cw - 2) / 2) ($ry + 6)
    }
    $script:g.DrawString('a', $fTiny, (Brush $C.dim), ($script:gx - 20), ($ry + 1))
    $ry + 14
}

# ---- panel A: the silo's own front face (the wall the controller is embedded in)
$g.DrawString('SILO FACE - the wall your controller is set into', $fLegB, (Brush $C.title), 12, 42)
$siloRows = @(
    @{ Y = 2; Cells = @('', '', 'ROD', '', 'DEP', '', '') },
    @{ Y = 1; Cells = @('', '', 'H2O', 'SILO', 'OUT', '', '') },
    @{ Y = 0; Cells = @('', '', 'MNT', 'CTRL', '', '', '') }
)
$after = Face $siloRows 60.0 $C.radproof '#A6B072' 22
$g.DrawString('y2 is the bottom FUEL course - rods enter and leave the segment that burns.', $fTiny, (Brush $C.dim), 12, $after)
$g.DrawString('y1 is the drum course - feedwater in, waste water and exported steam out.', $fTiny, (Brush $C.dim), 12, ($after + 11))

# ---- panel B: the spiral chamber's front wall, one course above the floor
$g.DrawString('SPIRAL CHAMBER FRONT WALL - what the chamber spends', $fLegB, (Brush $C.title), 12, ($after + 30))
$chamberRows = @(
    @{ Y = 2; Cells = @('', '', '', '', '', '', '') },
    @{ Y = 1; Cells = @('', 'COOL', '', '', '', 'PROP', '') },
    @{ Y = 0; Cells = @('', '', '', '', '', '', 'MNT') }
)
$after2 = Face $chamberRows ($after + 48) $C.tt '#2C4560' 43
$g.DrawString('The 8 Precision Injectors climb all four walls above this, y3 to y10.', $fTiny, (Brush $C.dim), 12, $after2)
$g.DrawString('The Spiral Cycle Sensor is in the ROOF, front-centre, facing up.', $fTiny, (Brush $C.dim), 12, ($after2 + 11))

# ---- legend
$ly = $after2 + 28
$legend = @(
    @($C.busin, 'ROD', 'Input Bus (IV)', 'rods in - fed to ALL 45-201 pillars'),
    @($C.busout, 'DEP', 'Output Bus (IV)', 'depleted rods out'),
    @($C.fluidin, 'H2O', 'Input Hatch (IV)', 'FEEDWATER for the reactor drum'),
    @($C.fluidout, 'OUT', 'Output Hatch (IV)', 'waste water / exported steam'),
    @($C.panelblk, 'SILO', 'Silo Control Block', 'prints the whole instrument panel'),
    @($C.ctrl, 'CTRL', 'FOUNDATION', 'the only controller in the plant'),
    @($C.maint, 'MNT', 'Maintenance Hatch', 'one per building - silo, chamber, manifold'),
    @($C.fluidin, 'COOL', 'Input Hatch (IV)', 'COOLANT - the chamber is what overheats'),
    @($C.fluidin, 'PROP', 'Input Hatch (IV)', 'bulk PROPELLANT - stocks all 8 injectors')
)
foreach ($row in $legend) {
    $g.FillRectangle((Brush $row[0]), 12, ($ly + 2), 9, 9)
    $g.DrawRectangle((Pen '#000000' 1), 12, ($ly + 2), 9, 9)
    $g.DrawString($row[1], $fLegB, (Brush $C.title), 26, $ly)
    $g.DrawString($row[2], $fLegB, (Brush $C.text), 62, $ly)
    $sz = $g.MeasureString($row[2], $fLegB)
    $g.DrawString($row[3], $fTiny, (Brush $C.dim), (64 + $sz.Width), ($ly + 2))
    $ly += 14
}
$g.DrawString('Every socket here faces FORWARD, out over open platform. EU leaves at the', $fTiny, (Brush $C.dim), 12, ($ly + 4))
$g.DrawString('output manifold instead - that face is the next diagram.', $fTiny, (Brush $C.dim), 12, ($ly + 16))
Save $cv 'point-of-use.png'

# =====================================================================================
# 3. THE OUTPUT MANIFOLD BACK FACE - where EU leaves and the clock goes in.
# =====================================================================================
$W = 380
$cv = NewCanvas $W 428
Head $cv 'OUTPUT MANIFOLD - BACK FACE' '7 x 7, at the far end of the rotor gallery - open sky behind it'
$g = $cv.G

$cw = 32.0
$gx = (380 - 7 * $cw) / 2
$gy = 46.0
# rows top-down y6..y0 exactly as docs/plant-geometry.md prints them
$face = @(
    @('.', '.', '.', '.', '.', '.', '.'),
    @('.', 'L', 'L', '.', 'L', 'L', '.'),
    @('.', 'E', 'E', '.', 'E', 'E', '.'),
    @('.', 'D', 'D', '.', 'D', 'D', '.'),
    @('.', '.', '.', 'T', '.', '.', '.'),
    @('.', '.', '.', 'M', '.', '.', '.'),
    @('.', '.', '.', '.', '.', '.', '.')
)
$faceColor = @{ 'L' = $C.laser; 'E' = $C.energy; 'D' = $C.dynamo; 'T' = $C.panelblk; 'M' = $C.maint }
for ($r = 0; $r -lt 7; $r++) {
    $y = $gy + $r * $cw
    $g.DrawString(('y' + (6 - $r)), $fTiny, (Brush $C.dim), ($gx - 22), ($y + 10))
    for ($i = 0; $i -lt 7; $i++) {
        $x = $gx + $i * $cw
        $ch2 = $face[$r][$i]
        if ($ch2 -eq '.') {
            $g.FillRectangle((Brush $C.tt), $x, $y, $cw - 2, $cw - 2)
            $g.DrawRectangle((Pen '#2C4560' 1), $x, $y, $cw - 2, $cw - 2)
        } else {
            $g.FillRectangle((Brush $faceColor[$ch2]), $x, $y, $cw - 2, $cw - 2)
            $g.DrawRectangle((Pen '#101010' 1.4), $x, $y, $cw - 2, $cw - 2)
            CenterText $g $ch2 $fCell (Brush '#101010') ($x + ($cw - 2) / 2) ($y + ($cw - 2) / 2)
        }
    }
}
$ly = $gy + 7 * $cw + 10
$legend = @(
    @($C.laser, 'L', 'Laser Source Hatch x4', 'ZPM 4,096A - the real ceiling'),
    @($C.energy, 'E', 'Energy Hatch (IV) x4', $ENERGY_CAPTION),
    @($C.dynamo, 'D', 'Dynamo Hatch (IV) x4', '4 IV alone cap you at 32,768 EU/t'),
    @($C.panelblk, 'T', 'TURBINE CONTROL BLOCK', 'the redstone clock goes HERE'),
    @($C.maint, 'M', 'Maintenance Hatch', 'directly below the control block'),
    @($C.tt, '.', 'Tungstensteel Turbine Casing', 'plain wall')
)
foreach ($row in $legend) {
    $g.FillRectangle((Brush $row[0]), 12, ($ly + 2), 9, 9)
    $g.DrawRectangle((Pen '#000000' 1), 12, ($ly + 2), 9, 9)
    $g.DrawString($row[1], $fLegB, (Brush $C.title), 26, $ly)
    $g.DrawString($row[2], $fLegB, (Brush $C.text), 40, $ly)
    $sz = $g.MeasureString($row[2], $fLegB)
    $g.DrawString($row[3], $fTiny, (Brush $C.dim), (42 + $sz.Width), ($ly + 2))
    $ly += 14
}
$g.DrawString('Ceiling = dynamos + lasers, summed. Paint each source and its target', $fTiny, (Brush $C.dim), 12, ($ly + 4))
$g.DrawString('hatch the same colour and the SAME voltage tier - a lower tier explodes.', $fTiny, (Brush $C.dim), 12, ($ly + 16))
# D5: the nuance the old one-word caption was carrying, said properly rather than wrongly.
$g.DrawString('No PIECE of the plant demands an E hatch - the plant forms and runs with', $fTiny, (Brush $C.dim), 12, ($ly + 32))
$g.DrawString('all four empty. The Derrick and the Annex do, and only through these.', $fTiny, (Brush $C.dim), 12, ($ly + 44))
Save $cv 'manifold-face.png'

Write-Host 'diagrams done.'
