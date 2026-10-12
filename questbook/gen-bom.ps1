# Derives every Blueprint bill of materials in the questbook DIRECTLY from the structure
# shape code, so a hand-typed count can never drift from what the projection actually
# raises again (review finding Q5 - all five bills were stale twice in a row).
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File gen-bom.ps1        # THE build step
#   . .\gen-bom.ps1                                                        # dot-source: $BTS_BOM
#
# RUN AS A SCRIPT THIS FILE IS THE WHOLE QUESTBOOK BUILD, NOT HALF OF IT. It re-renders
# BTSBom.java, then runs gen-questbook.ps1 to rewrite DefaultQuests\ out of the same
# shape code, then prints the table. There is no way to get one without the other, and
# no switch that asks for one - see "T1: BOTH CONSUMERS OR NEITHER" at the foot of this
# file for why. Do not run gen-questbook.ps1 as the build step; it cannot write
# BTSBom.java, so it throws when the geometry has moved.
#
# gen-questbook.ps1 dot-sources this file and interpolates $BTS_BOM into the five
# Blueprint quests, so THE NUMBERS IN THE BOOK ARE NEVER TYPED BY HAND.
#
# ---------------------------------------------------------------------------------
# HOW IT WORKS. Every shape function of MTEReactorCoreMk1 (floorShape, coreShape,
# ductShape, spiralShape, galleryShape, ventShape, switchyardShape, derrickShape,
# annexShape) and the borrowed MTEModuleCoolingTower.towerShape
# is re-implemented below in the SAME index order the Java uses:
# layers[b][c-row][a-char], layer 0 = top. Each cell's char is then looked up in the
# structure definition's element table (ELEMENT_ITEM below, transcribed from
# getStructureDefinition's .addElement chain) and tallied.
#
# WHY A PORT AND NOT A PARSE. The shapes are procedural Java; there is nothing to parse.
# The port is therefore guarded instead: Assert-Source below reads MTEReactorCoreMk1.java
# and MTEModuleCoolingTower.java and fails LOUDLY if any constant or socket-placing line
# this port depends on has changed. A geometry edit in the Java breaks this script on the
# next run rather than silently shipping a wrong shopping list.
#
# ASCII-only, like gen-questbook.ps1: Windows PowerShell 5.1 reads a BOM-less .ps1 as
# Windows-1252.

$ErrorActionPreference = 'Stop'

$script:JavaRoot = Join-Path (Split-Path $PSScriptRoot -Parent) 'src\main\java\com\nateyburr\beyondthestars\machines'
$script:CoreJava = Join-Path $script:JavaRoot 'MTEReactorCoreMk1.java'
$script:TowerJava = Join-Path $script:JavaRoot 'MTEModuleCoolingTower.java'

# --------------------------------------------------------------- ported constants
$MAX_TIER = 5
$CUT = @(0, 2, 2, 3, 3, 4)
$COLUMN_CAP = @(0, 45, 77, 109, 157, 201)
$PLATFORM = 50
$CENTER = 25            # PLATFORM / 2
$SPIRAL_SIZE = 7
$SPIRAL_HEIGHT = 12
$GALLERY_LENGTH = 18
$MANIFOLD_START = 15
$SEGMENTS_PER_PILLAR = 8
$TOWER_HEIGHT = 20
$TOWER_EDGE = 17
$TOWER_COUNT = 2         # the controller raises a front and a back tower
# D-17 / D-22 plant modules. Site rectangles are platform coordinates; the shape ports
# below re-implement ventShape/switchyardShape/derrickShape/annexShape cell for cell.
$VENT_SIZE = 7;    $VENT_HEIGHT = 20
$YARD_WIDTH = 21;  $YARD_DEPTH = 6;    $YARD_HEIGHT = 6
$DERRICK_SIZE = 7; $DERRICK_HEIGHT = 20
$ANNEX_WIDTH = 5;  $ANNEX_LENGTH = 15; $ANNEX_HEIGHT = 6

# ------------------------------------------------------- drift guard against the Java
# Each entry: a human name, the file, and a literal the port depends on. A missing
# literal means the Java moved and this port is now a guess - which is exactly the
# failure mode Q5 was.
function Assert-Source {
    $core = [System.IO.File]::ReadAllText($script:CoreJava)
    $tower = [System.IO.File]::ReadAllText($script:TowerJava)
    $checks = @(
        @{ N = 'MAX_TIER = 5'; T = $core; S = 'private static final int MAX_TIER = 5;' },
        @{ N = 'CUT table'; T = $core; S = 'private static final int[] CUT = { 0, 2, 2, 3, 3, 4 };' },
        @{ N = 'COLUMN_CAP table'; T = $core; S = 'private static final int[] COLUMN_CAP = { 0, 45, 77, 109, 157, 201 };' },
        @{ N = 'PLATFORM = 50'; T = $core; S = 'private static final int PLATFORM = 50;' },
        @{ N = 'edgeFor'; T = $core; S = 'return 7 + 2 * tier;' },
        @{ N = 'wallHeightFor'; T = $core; S = 'return edgeFor(tier) * 2;' },
        @{ N = 'domeHeightFor'; T = $core; S = 'return edgeFor(tier) / 2;' },
        @{ N = 'ductLengthFor'; T = $core; S = 'return 16 - edgeFor(tier) / 2;' },
        @{ N = 'insideOct'; T = $core; S = 'return Math.min(r, n - 1 - r) + Math.min(x, n - 1 - x) >= k;' },
        @{ N = 'fuel layers 3..10'; T = $core; S = 'if (i >= 3 && i <= 10) {' },
        @{ N = 'rod-port layer 11'; T = $core; S = '} else if (i == 11) {' },
        @{ N = 'band layers'; T = $core; S = 'return bottomUpIndex == 2 || bottomUpIndex == (9 + wallH - 1) / 2 || bottomUpIndex == wallH - 2;' },
        @{ N = 'wall material by tier'; T = $core; S = "return tier >= 5 ? 'E' : tier >= 3 ? 'D' : 'C';" },
        @{ N = 'dome material by tier'; T = $core; S = "return tier >= 3 ? 'I' : 'B';" },
        @{ N = 'silo socket t (Maintenance)'; T = $core; S = "setFrontCell(padCourse, mid - 1, 't');" },
        @{ N = 'silo socket y (Feedwater)'; T = $core; S = "setFrontCell(drumCourse, mid - 1, 'y');" },
        @{ N = 'silo socket p (Silo Panel)'; T = $core; S = "setFrontCell(drumCourse, mid, 'p');" },
        @{ N = 'silo socket Y (Output Hatch)'; T = $core; S = "setFrontCell(drumCourse, mid + 1, 'Y');" },
        @{ N = 'silo socket u (Rod Input Bus)'; T = $core; S = "setFrontCell(fuelCourse, mid - 1, 'u');" },
        @{ N = 'silo socket Q (Depleted Bus)'; T = $core; S = "setFrontCell(fuelCourse, mid + 1, 'Q');" },
        @{ N = 'SPIRAL_SIZE = 7'; T = $core; S = 'private static final int SPIRAL_SIZE = 7;' },
        @{ N = 'SPIRAL_HEIGHT = 12'; T = $core; S = 'private static final int SPIRAL_HEIGHT = 12;' },
        @{ N = 'spiral sensor socket j'; T = $core; S = "cap[0][3] = 'j';" },
        @{ N = 'spiral maintenance socket l'; T = $core; S = "cap[0][6] = 'l';" },
        @{ N = 'spiral coolant socket k'; T = $core; S = "grid[0][1] = 'k';" },
        @{ N = 'spiral propellant socket P'; T = $core; S = "grid[0][5] = 'P';" },
        @{ N = 'spiral injector level range'; T = $core; S = 'if (level >= 1 && level <= 8) {' },
        @{ N = 'chamber socket course'; T = $core; S = 'if (level == SPIRAL_HEIGHT - 2) {' },
        @{ N = 'GALLERY_LENGTH = 18'; T = $core; S = 'private static final int GALLERY_LENGTH = 18;' },
        @{ N = 'MANIFOLD_START = 15'; T = $core; S = 'private static final int MANIFOLD_START = 15;' },
        @{ N = 'GALLERY_STAGES = 8'; T = $core; S = 'private static final int GALLERY_STAGES = 8;' },
        @{ N = 'manifold energy row r'; T = $core; S = 'if (y == 4 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {' },
        @{ N = 'manifold dynamo row m'; T = $core; S = 'if (y == 3 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {' },
        @{ N = 'manifold laser row L'; T = $core; S = 'if (y == 5 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {' },
        @{ N = 'tower remap NS~ -> ORR'; T = $core; S = '"NS~", "ORR"' },
        @{ N = 'tower piece front'; T = $core; S = 'towerPieceName(mName, "front")' },
        @{ N = 'tower piece back'; T = $core; S = 'towerPieceName(mName, "back")' },
        @{ N = 'tower HEIGHT = 20'; T = $tower; S = 'private static final int HEIGHT = 20;' },
        @{ N = 'tower EDGE = 17'; T = $tower; S = 'private static final int EDGE = 17;' },
        @{ N = 'tower profile'; T = $tower; S = 'final double rBase = 7.5, rWaist = 4.6, rTop = 5.6, waistT = 0.62;' },
        @{ N = 'vent stack site'; T = $core; S = 'private static final int VENT_A0 = 2, VENT_C0 = 21, VENT_SIZE = 7, VENT_HEIGHT = 20;' },
        @{ N = 'switchyard site'; T = $core; S = 'private static final int YARD_A0 = 20, YARD_C0 = 44, YARD_WIDTH = 21, YARD_DEPTH = 6, YARD_HEIGHT = 6;' },
        @{ N = 'derrick site'; T = $core; S = 'private static final int DERRICK_A0 = 33, DERRICK_C0 = 4, DERRICK_SIZE = 7, DERRICK_HEIGHT = 20;' },
        @{ N = 'annex site'; T = $core; S = 'private static final int ANNEX_A0 = 17, ANNEX_C0 = 0, ANNEX_WIDTH = 5, ANNEX_LENGTH = 15, ANNEX_HEIGHT = 6;' },
        @{ N = 'vent flue ring'; T = $core; S = "if (c != 3 || a != 3) setCell(rows, c, a, 'W');" },
        @{ N = 'switchyard transformer banks'; T = $core; S = 'final int[] banks = { 2, 6, 10, 14, 18 };' },
        @{ N = 'switchyard pylons'; T = $core; S = 'final int[] pylons = { 0, 5, 10, 15, 20 };' },
        @{ N = 'derrick product bus socket b'; T = $core; S = "setCell(rows, 0, 3, 'b');" },
        @{ N = 'derrick crown ring'; T = $core; S = "if (c == 1 || c == 5 || a == 1 || a == 5) setCell(rows, c, a, 'M');" },
        @{ N = 'annex conversion line'; T = $core; S = "for (int c = 1; c <= ANNEX_LENGTH - 2; c++) setCell(rows, c, 2, 'X');" },
        @{ N = 'annex feed bus socket e'; T = $core; S = "setCell(rows, 3, ANNEX_WIDTH - 1, 'e');" },
        @{ N = 'annex acid hatch socket f'; T = $core; S = "setCell(rows, 7, ANNEX_WIDTH - 1, 'f');" },
        @{ N = 'annex product bus socket g'; T = $core; S = "setCell(rows, 11, ANNEX_WIDTH - 1, 'g');" }
    )
    $missing = @()
    foreach ($c in $checks) {
        if ($c.T.IndexOf($c.S) -lt 0) { $missing += $c.N }
    }
    if ($missing.Count -gt 0) {
        throw ("gen-bom.ps1 is out of date with the Java. These structure facts it was ported from " +
            "are no longer in the source: " + ($missing -join '; ') + ". Re-port the changed shape " +
            "function before regenerating the questbook - the bills would otherwise ship wrong.")
    }
}

# ---------------------------------------------------- element char -> real item name
# Transcribed from MTEReactorCoreMk1.getStructureDefinition()'s .addElement chain. A
# socket char maps to the HATCH the player fits there; the casing it chains to when left
# blank is noted in the comment but never counted - a blank socket is not a bill item.
#
# It MUST be a .NET Hashtable, not a PowerShell @{} literal: the element alphabet is
# case-sensitive ('Y' the silo Output Hatch vs 'y' the Feedwater hatch, 'P' propellant vs
# 'p' the Silo Control Block, 'L' laser vs 'l' maintenance, 'Q'/'q', 'T'/'t', 'R'/'r',
# 'I'/'i', 'J'/'j'), and @{} folds case.
$ELEMENT_ITEM = New-Object System.Collections.Hashtable
# --- platform
$ELEMENT_ITEM['O'] = 'Light Concrete'
$ELEMENT_ITEM['B'] = 'Tungstensteel Reinforced Block'
$ELEMENT_ITEM['I'] = 'Iridium Reinforced Block'
$ELEMENT_ITEM['R'] = 'Solid Steel Machine Casing'
# --- silo walls (tiered) + radiation bands
$ELEMENT_ITEM['C'] = 'Radiation Proof Machine Casing'
$ELEMENT_ITEM['D'] = 'Europium Reinforced Radiation Proof Machine Casing'
$ELEMENT_ITEM['E'] = 'Radiant Naquadah Alloy Casing'
$ELEMENT_ITEM['G'] = 'Radiation Proof Machine Casing'
# --- silo interior
$ELEMENT_ITEM['F'] = 'Reactor Fuel Pillar Frame'
$ELEMENT_ITEM['J'] = 'Rod Loading Port'
# --- silo face sockets (blank -> Radiation Proof)
$ELEMENT_ITEM['t'] = 'Maintenance Hatch'
$ELEMENT_ITEM['y'] = 'Input Hatch (IV)'
$ELEMENT_ITEM['p'] = 'Silo Control Block'
$ELEMENT_ITEM['Y'] = 'Output Hatch (IV)'
$ELEMENT_ITEM['u'] = 'Input Bus (IV)'
$ELEMENT_ITEM['Q'] = 'Output Bus (IV)'
# --- spine duct
$ELEMENT_ITEM['T'] = 'Tungstensteel Turbine Casing'
$ELEMENT_ITEM['H'] = 'Turbine Housing'
# --- spiral chamber (blank sockets -> Tungstensteel Turbine Casing)
$ELEMENT_ITEM['5'] = 'Tungstensteel Turbine Casing'
$ELEMENT_ITEM['i'] = 'Precision Injector'
$ELEMENT_ITEM['v'] = 'Precision Injector'
$ELEMENT_ITEM['w'] = 'Precision Injector'
$ELEMENT_ITEM['x'] = 'Precision Injector'
$ELEMENT_ITEM['j'] = 'Spine Sensor'
$ELEMENT_ITEM['l'] = 'Maintenance Hatch'
$ELEMENT_ITEM['k'] = 'Input Hatch (IV)'
$ELEMENT_ITEM['P'] = 'Input Hatch (IV)'
# --- gallery + output manifold
$ELEMENT_ITEM['8'] = 'Tungstensteel Turbine Casing'
$ELEMENT_ITEM['9'] = 'Turbine Housing'
$ELEMENT_ITEM['m'] = 'Dynamo Hatch (IV)'
$ELEMENT_ITEM['r'] = 'Energy Hatch (IV)'
$ELEMENT_ITEM['L'] = 'Laser Source Hatch'
$ELEMENT_ITEM['n'] = 'Maintenance Hatch'
$ELEMENT_ITEM['q'] = 'Turbine Control Block'
# --- D-17 plant modules (blank sockets fall back to the module's own casing)
$ELEMENT_ITEM['V'] = 'Clean Stainless Steel Machine Casing'
$ELEMENT_ITEM['W'] = 'Filter Machine Casing'
$ELEMENT_ITEM['Z'] = 'Solid Steel Machine Casing'
$ELEMENT_ITEM['M'] = 'Steel Frame Box'
$ELEMENT_ITEM['X'] = 'Steel Pipe Casing'
$ELEMENT_ITEM['b'] = 'Output Bus (IV)'
$ELEMENT_ITEM['e'] = 'Input Bus (IV)'
$ELEMENT_ITEM['f'] = 'Input Hatch (IV)'
$ELEMENT_ITEM['g'] = 'Output Bus (IV)'

# ------------------------------------------------------------------ shape helpers
function Get-Edge([int]$t) { 7 + 2 * $t }
function Get-WallHeight([int]$t) { (Get-Edge $t) * 2 }
function Get-DomeHeight([int]$t) { [int][Math]::Floor((Get-Edge $t) / 2) }
function Get-DuctLength([int]$t) { 16 - [int][Math]::Floor((Get-Edge $t) / 2) }

function Test-InsideOct([int]$n, [int]$k, [int]$r, [int]$x) {
    ([Math]::Min($r, $n - 1 - $r) + [Math]::Min($x, $n - 1 - $x)) -ge $k
}
function Test-InteriorCell([int]$n, [int]$k, [int]$r, [int]$x) {
    if ($r -lt 1 -or $r -gt ($n - 2) -or $x -lt 1 -or $x -gt ($n - 2)) { return $false }
    ([Math]::Min($r, $n - 1 - $r) + [Math]::Min($x, $n - 1 - $x)) -ge ($k + 1)
}
function Get-WallChar([int]$t) { if ($t -ge 5) { 'E' } elseif ($t -ge 3) { 'D' } else { 'C' } }
function Get-ShieldChar([int]$t) { if ($t -ge 3) { 'I' } else { 'B' } }

# ------------------------------------------------------------------- platform floor
function Get-FloorShape {
    $pad = @()
    for ($c = 0; $c -lt $PLATFORM; $c++) {
        $row = New-Object char[] $PLATFORM
        for ($i = 0; $i -lt $PLATFORM; $i++) { $row[$i] = ' ' }
        if ($c -eq ($CENTER - 8)) { $row[$CENTER] = '~' }   # SILO_FRONT: the main station
        $pad += , (-join $row)
    }
    $n = Get-Edge $MAX_TIER
    $k = $CUT[$MAX_TIER]
    $origin = $CENTER - [int][Math]::Floor($n / 2)
    $floor = @()
    for ($c = 0; $c -lt $PLATFORM; $c++) {
        $row = New-Object char[] $PLATFORM
        for ($a = 0; $a -lt $PLATFORM; $a++) {
            $lr = $c - $origin
            $lx = $a - $origin
            if ($lr -ge 0 -and $lx -ge 0 -and $lr -lt $n -and $lx -lt $n -and (Test-InsideOct $n $k $lr $lx)) {
                $row[$a] = 'B'
            } else {
                $row[$a] = 'O'
            }
        }
        $floor += , (-join $row)
    }
    , @($pad, $floor)
}

# --------------------------------------------------------------------------- silo
function Add-Ribs($rows, [int]$t) {
    $n = Get-Edge $t
    $k = $CUT[$t]
    $c = [int][Math]::Floor($n / 2)
    $gridSize = $n + 2
    $dirs = @(@(1, 0), @(0, 1), @(0, -1), @(1, 1), @(1, -1), @(-1, 1), @(-1, -1))
    foreach ($d in $dirs) {
        $r = $c; $x = $c
        while (($r + $d[0]) -ge 0 -and ($r + $d[0]) -lt $n -and ($x + $d[1]) -ge 0 -and ($x + $d[1]) -lt $n `
                -and (Test-InsideOct $n $k ($r + $d[0]) ($x + $d[1]))) {
            $r += $d[0]; $x += $d[1]
        }
        $rr = $r + $d[0] + 1
        $xx = $x + $d[1] + 1
        if ($rr -ge 0 -and $rr -lt $gridSize -and $xx -ge 0 -and $xx -lt $gridSize) {
            if ($rows[$rr][$xx] -eq ' ') {
                $chars = $rows[$rr].ToCharArray()
                $chars[$xx] = (Get-WallChar $t)
                $rows[$rr] = -join $chars
            }
        }
    }
}

function Get-BuildLayer([int]$t, [string]$kind, [bool]$ribs, [bool]$ctrl) {
    $n = Get-Edge $t
    $k = $CUT[$t]
    $gridSize = $n + 2
    if ($kind -eq 'BAND') { $wall = 'G' } else { $wall = Get-WallChar $t }
    $rows = New-Object string[] $gridSize
    for ($rr = 0; $rr -lt $gridSize; $rr++) {
        $row = New-Object char[] $gridSize
        for ($xx = 0; $xx -lt $gridSize; $xx++) {
            $r = $rr - 1
            $x = $xx - 1
            if ($r -lt 0 -or $x -lt 0 -or $r -ge $n -or $x -ge $n -or -not (Test-InsideOct $n $k $r $x)) {
                $row[$xx] = ' '
            } elseif (Test-InteriorCell $n $k $r $x) {
                if ($kind -eq 'FUEL') { $row[$xx] = 'F' } elseif ($kind -eq 'PORTS') { $row[$xx] = 'J' } else { $row[$xx] = 'A' }
            } elseif ($ctrl -and $r -eq 0 -and $x -eq [int][Math]::Floor($n / 2)) {
                $row[$xx] = '~'
            } else {
                $row[$xx] = $wall
            }
        }
        $rows[$rr] = -join $row
    }
    if ($ribs) { Add-Ribs $rows $t }
    , $rows
}

function Get-DomeLayer([int]$t, [int]$yAbove) {
    $n = Get-Edge $t
    $gridSize = $n + 2
    $radius = $n / 2.0
    $center = ($n - 1) / 2.0
    $ringRadius = [Math]::Sqrt([Math]::Max(0.0, $radius * $radius - [double]$yAbove * [double]$yAbove))
    $cap = $yAbove -ge (Get-DomeHeight $t)
    $shield = Get-ShieldChar $t
    $rows = New-Object string[] $gridSize
    for ($rr = 0; $rr -lt $gridSize; $rr++) {
        $row = New-Object char[] $gridSize
        for ($xx = 0; $xx -lt $gridSize; $xx++) {
            $r = $rr - 1
            $x = $xx - 1
            if ($r -lt 0 -or $x -lt 0 -or $r -ge $n -or $x -ge $n) {
                $row[$xx] = ' '
            } else {
                $dr = $r - $center
                $dx = $x - $center
                $d = [Math]::Sqrt($dr * $dr + $dx * $dx)
                if ($cap) { $ok = $d -le ($ringRadius + 0.4) } else { $ok = [Math]::Abs($d - $ringRadius) -le 0.75 }
                if ($ok) { $row[$xx] = $shield } else { $row[$xx] = ' ' }
            }
        }
        $rows[$rr] = -join $row
    }
    , $rows
}

function Test-BandLayer([int]$t, [int]$i) {
    $wallH = Get-WallHeight $t
    ($i -eq 2) -or ($i -eq [int][Math]::Floor((9 + $wallH - 1) / 2)) -or ($i -eq ($wallH - 2))
}

function Set-FrontCell($layer, [int]$xx, [char]$ch) {
    $chars = $layer[1].ToCharArray()
    $chars[$xx] = $ch
    $layer[1] = -join $chars
}

function Get-CoreShape([int]$t) {
    $wallH = Get-WallHeight $t
    $domeH = Get-DomeHeight $t
    $layers = @()
    for ($y = $domeH; $y -ge 1; $y--) { $layers += , (Get-DomeLayer $t $y) }
    for ($i = $wallH - 1; $i -ge 1; $i--) {
        if ($i -ge 3 -and $i -le 10) { $kind = 'FUEL' }
        elseif ($i -eq 11) { $kind = 'PORTS' }
        elseif (Test-BandLayer $t $i) { $kind = 'BAND' }
        else { $kind = 'PLENUM' }
        $layers += , (Get-BuildLayer $t $kind $true ($i -eq 1))
    }
    # D-20 silo face: the sockets that belong to the silo, on the course each one enters
    # the logic flow. Bottom-up layer i sits at list index domeH + wallH - 1 - i.
    $mid = [int][Math]::Floor((Get-Edge $t) / 2) + 1
    Set-FrontCell $layers[$domeH + $wallH - 2] ($mid - 1) 't'   # y0 Maintenance
    Set-FrontCell $layers[$domeH + $wallH - 3] ($mid - 1) 'y'   # y1 Feedwater
    Set-FrontCell $layers[$domeH + $wallH - 3] $mid 'p'         # y1 Silo Control Block
    Set-FrontCell $layers[$domeH + $wallH - 3] ($mid + 1) 'Y'   # y1 Output Hatch
    Set-FrontCell $layers[$domeH + $wallH - 4] ($mid - 1) 'u'   # y2 Rod Input Bus
    Set-FrontCell $layers[$domeH + $wallH - 4] ($mid + 1) 'Q'   # y2 Depleted Output Bus
    , $layers
}

# --------------------------------------------------------------------- spine duct
function Get-DuctShape([int]$t) {
    $len = Get-DuctLength $t
    $solid = 'T' * $len
    $bore = New-Object char[] $len
    for ($i = 0; $i -lt $len; $i++) { $bore[$i] = 'A' }
    for ($i = 1; $i -le 7 -and $i -lt $len; $i += 2) { $bore[$i] = 'H' }
    $boreRow = -join $bore
    , @(, @($solid, $solid, $solid), , @($solid, $boreRow, $solid), , @($solid, $solid, $solid))
}

# ------------------------------------------------------------------ spiral chamber
function Get-InteriorRing {
    $cells = @()
    for ($x = 1; $x -le 5; $x++) { $cells += , @(1, $x) }
    for ($r = 2; $r -le 5; $r++) { $cells += , @($r, 5) }
    for ($x = 4; $x -ge 1; $x--) { $cells += , @(5, $x) }
    for ($r = 4; $r -ge 2; $r--) { $cells += , @($r, 1) }
    , $cells
}

function Get-SpiralShape {
    $ring = Get-InteriorRing
    $layers = @()
    for ($level = 0; $level -lt $SPIRAL_HEIGHT; $level++) {
        if ($level -eq 0 -or $level -eq ($SPIRAL_HEIGHT - 1)) {
            $cap = @()
            for ($r = 0; $r -lt $SPIRAL_SIZE; $r++) { $cap += , ('5555555'.ToCharArray()) }
            if ($level -eq 0) { $cap[0][3] = 'j' } else { $cap[0][6] = 'l' }
            $rows = New-Object string[] $SPIRAL_SIZE
            for ($r = 0; $r -lt $SPIRAL_SIZE; $r++) { $rows[$r] = -join $cap[$r] }
            $layers += , $rows
            continue
        }
        $grid = @()
        for ($r = 0; $r -lt $SPIRAL_SIZE; $r++) {
            $row = New-Object char[] $SPIRAL_SIZE
            for ($x = 0; $x -lt $SPIRAL_SIZE; $x++) {
                if ($r -eq 0 -or $x -eq 0 -or $r -eq ($SPIRAL_SIZE - 1) -or $x -eq ($SPIRAL_SIZE - 1)) {
                    $row[$x] = '5'
                } else {
                    $row[$x] = 'A'
                }
            }
            $grid += , $row
        }
        if ($level -ge 1 -and $level -le 8) {
            $face = ($level - 1) % 4
            if ($face -eq 0) { $grid[6][3] = 'i' }
            elseif ($face -eq 1) { $grid[3][0] = 'v' }
            elseif ($face -eq 2) { $grid[0][3] = 'w' }
            else { $grid[3][6] = 'x' }
        }
        if ($level -eq ($SPIRAL_HEIGHT - 2)) {
            $grid[0][1] = 'k'   # D-20 Coolant Input Hatch
            $grid[0][5] = 'P'   # D-20 bulk Propellant Input Hatch
        }
        $start = (($level - 1) * 3) % $ring.Count
        for ($m = 0; $m -lt 5; $m++) {
            $cell = $ring[($start + $m) % $ring.Count]
            $grid[$cell[0]][$cell[1]] = 'O'
        }
        $rows = New-Object string[] $SPIRAL_SIZE
        for ($r = 0; $r -lt $SPIRAL_SIZE; $r++) { $rows[$r] = -join $grid[$r] }
        $layers += , $rows
    }
    , $layers
}

# ------------------------------------------------------ rotor gallery + manifold
function Get-GalleryCell([int]$y, [int]$c, [int]$a) {
    if ($c -lt $MANIFOLD_START) {
        if ($y -gt 4) { return ' ' }
        if ($y -eq 2 -and $a -eq 3 -and ($c % 2) -eq 1 -and $c -le 13) { return '9' }
        if ($y -eq 0 -or $y -eq 4 -or $a -eq 0 -or $a -eq 6 -or $c -eq 0) { return '8' }
        return 'A'
    }
    $backFace = ($c -eq ($GALLERY_LENGTH - 1))
    $col = ($a -eq 1 -or $a -eq 2 -or $a -eq 4 -or $a -eq 5)
    if ($y -eq 3 -and $backFace -and $col) { return 'm' }
    if ($y -eq 5 -and $backFace -and $col) { return 'L' }
    if ($y -eq 4 -and $backFace -and $col) { return 'r' }
    if ($backFace -and $a -eq 3) {
        if ($y -eq 2) { return 'q' }
        if ($y -eq 1) { return 'n' }
    }
    if ($y -eq 2 -and $c -eq $MANIFOLD_START -and $a -eq 3) { return '9' }
    if ($y -eq 0 -or $y -eq 6 -or $a -eq 0 -or $a -eq 6 -or $backFace) { return '8' }
    if ($c -eq $MANIFOLD_START) {
        if ($a -ge 1 -and $a -le 5 -and $y -ge 1 -and $y -le 3) { return 'A' } else { return '8' }
    }
    return 'A'
}

function Get-GalleryShape {
    $layers = @()
    for ($b = 0; $b -lt 7; $b++) {
        $y = 6 - $b
        $rows = New-Object string[] $GALLERY_LENGTH
        for ($c = 0; $c -lt $GALLERY_LENGTH; $c++) {
            $row = New-Object char[] 7
            for ($a = 0; $a -lt 7; $a++) { $row[$a] = Get-GalleryCell $y $c $a }
            $rows[$c] = -join $row
        }
        $layers += , $rows
    }
    , $layers
}

# ------------------------------------------------------------------ cooling tower
function Get-TowerRadius([int]$yFromBottom) {
    $tt = $yFromBottom / [double]($TOWER_HEIGHT - 1)
    $rBase = 7.5; $rWaist = 4.6; $rTop = 5.6; $waistT = 0.62
    if ($tt -lt $waistT) { return $rBase + ($rWaist - $rBase) * ($tt / $waistT) }
    $rWaist + ($rTop - $rWaist) * (($tt - $waistT) / (1 - $waistT))
}

function Get-TowerShape {
    $half = [int][Math]::Floor($TOWER_EDGE / 2)
    $layers = @()
    for ($i = 0; $i -lt $TOWER_HEIGHT; $i++) {
        $yFromBottom = $TOWER_HEIGHT - 1 - $i
        $r = Get-TowerRadius $yFromBottom
        if ($yFromBottom -eq 0) { $ring = 'S' } else { $ring = 'N' }
        $rows = New-Object string[] $TOWER_EDGE
        for ($z = 0; $z -lt $TOWER_EDGE; $z++) {
            $row = New-Object char[] $TOWER_EDGE
            for ($x = 0; $x -lt $TOWER_EDGE; $x++) {
                $dx = $x - $half
                $dz = $z - $half
                $d = [Math]::Sqrt($dx * $dx + $dz * $dz)
                if ($d -le ($r + 0.5) -and $d -ge ($r - 0.6)) { $row[$x] = $ring } else { $row[$x] = ' ' }
            }
            if ($yFromBottom -eq 0 -and $z -eq 0 -and $row[$half] -eq 'S') { $row[$half] = '~' }
            $rows[$z] = -join $row
        }
        $layers += , $rows
    }
    # Java: Math.round() is floor(x + 0.5). PowerShell's [Math]::Round is banker's
    # rounding and would give 0 where Java gives 1 - the anchor row would be wrong.
    $frontZ = [int][Math]::Floor(($half - (Get-TowerRadius 0)) + 0.5)
    if ($frontZ -lt 0) { $frontZ = 0 }
    $base = $layers[$TOWER_HEIGHT - 1]
    $chars = $base[$frontZ].ToCharArray()
    $chars[$half] = '~'
    $base[$frontZ] = -join $chars
    , $layers
}

# remapShape(src, "NS~", "ORR"): the retired module controller's anchor cells close back
# up into ordinary Solid Steel base ring, so the projection raises a CLOSED ring (R2).
function Convert-TowerShape($src) {
    $out = @()
    foreach ($layer in $src) {
        $rows = New-Object string[] $layer.Length
        for ($j = 0; $j -lt $layer.Length; $j++) {
            $chars = $layer[$j].ToCharArray()
            for ($k = 0; $k -lt $chars.Length; $k++) {
                if ($chars[$k] -eq 'N') { $chars[$k] = 'O' }
                elseif ($chars[$k] -eq 'S') { $chars[$k] = 'R' }
                elseif ($chars[$k] -eq '~') { $chars[$k] = 'R' }
            }
            $rows[$j] = -join $chars
        }
        $out += , $rows
    }
    , $out
}

# ------------------------------------------------- D-17 plant modules (ported cell for cell)
function New-Rows([int]$rows, [int]$width, [char]$ch) {
    $out = New-Object string[] $rows
    for ($i = 0; $i -lt $rows; $i++) { $out[$i] = ([string]$ch) * $width }
    , $out
}
function Set-ShapeCell($rows, [int]$row, [int]$col, [char]$ch) {
    $chars = $rows[$row].ToCharArray()
    $chars[$col] = $ch
    $rows[$row] = -join $chars
}
function Add-VentFlue($rows) {
    for ($c = 2; $c -le 4; $c++) {
        for ($a = 2; $a -le 4; $a++) {
            if ($c -ne 3 -or $a -ne 3) { Set-ShapeCell $rows $c $a 'W' }
        }
    }
}
function Get-VentShape {
    $layers = @()
    for ($b = 0; $b -lt $VENT_HEIGHT; $b++) {
        $y = $VENT_HEIGHT - 1 - $b
        if ($y -eq 0) {
            $rows = New-Rows $VENT_SIZE $VENT_SIZE 'V'
        } elseif ($y -eq 4) {
            $rows = New-Rows $VENT_SIZE $VENT_SIZE 'V'
            for ($c = 2; $c -le 4; $c++) { for ($a = 2; $a -le 4; $a++) { Set-ShapeCell $rows $c $a ' ' } }
            Add-VentFlue $rows
        } elseif ($y -le 3) {
            $rows = New-Rows $VENT_SIZE $VENT_SIZE ' '
            for ($c = 0; $c -lt $VENT_SIZE; $c++) {
                for ($a = 0; $a -lt $VENT_SIZE; $a++) {
                    if ($c -eq 0 -or $c -eq ($VENT_SIZE - 1) -or $a -eq 0 -or $a -eq ($VENT_SIZE - 1)) {
                        Set-ShapeCell $rows $c $a 'V'
                    }
                }
            }
            Add-VentFlue $rows
        } else {
            $rows = New-Rows $VENT_SIZE $VENT_SIZE ' '
            Add-VentFlue $rows
        }
        $layers += , $rows
    }
    , $layers
}
function Get-SwitchyardShape {
    $banks = @(2, 6, 10, 14, 18)
    $pylons = @(0, 5, 10, 15, 20)
    $layers = @()
    for ($b = 0; $b -lt $YARD_HEIGHT; $b++) {
        $y = $YARD_HEIGHT - 1 - $b
        if ($y -eq 0) {
            $rows = New-Rows $YARD_DEPTH $YARD_WIDTH 'Z'
        } else {
            $rows = New-Rows $YARD_DEPTH $YARD_WIDTH ' '
            if ($y -eq 1) {
                for ($c = 0; $c -lt $YARD_DEPTH; $c++) {
                    for ($a = 0; $a -lt $YARD_WIDTH; $a++) {
                        if ($c -eq 0 -or $c -eq ($YARD_DEPTH - 1) -or $a -eq 0 -or $a -eq ($YARD_WIDTH - 1)) {
                            Set-ShapeCell $rows $c $a 'M'
                        }
                    }
                }
            } else {
                foreach ($a in $pylons) {
                    Set-ShapeCell $rows 0 $a 'M'
                    Set-ShapeCell $rows ($YARD_DEPTH - 1) $a 'M'
                }
            }
            if ($y -le 3) {
                foreach ($centre in $banks) {
                    for ($c = 2; $c -le 4; $c++) {
                        for ($a = $centre - 1; $a -le $centre + 1; $a++) { Set-ShapeCell $rows $c $a 'Z' }
                    }
                }
            }
        }
        $layers += , $rows
    }
    , $layers
}
function Get-DerrickShape {
    $layers = @()
    for ($b = 0; $b -lt $DERRICK_HEIGHT; $b++) {
        $y = $DERRICK_HEIGHT - 1 - $b
        if ($y -eq 0) {
            $rows = New-Rows $DERRICK_SIZE $DERRICK_SIZE 'Z'
        } elseif ($y -eq 1) {
            $rows = New-Rows $DERRICK_SIZE $DERRICK_SIZE 'Z'
            Set-ShapeCell $rows 3 3 'X'
            Set-ShapeCell $rows 0 3 'b'
        } elseif ($y -eq 2) {
            $rows = New-Rows $DERRICK_SIZE $DERRICK_SIZE 'Z'
            Set-ShapeCell $rows 3 3 'X'
        } elseif ($y -eq ($DERRICK_HEIGHT - 1)) {
            $rows = New-Rows $DERRICK_SIZE $DERRICK_SIZE ' '
            for ($c = 1; $c -le 5; $c++) {
                for ($a = 1; $a -le 5; $a++) {
                    if ($c -eq 1 -or $c -eq 5 -or $a -eq 1 -or $a -eq 5) { Set-ShapeCell $rows $c $a 'M' }
                }
            }
            Set-ShapeCell $rows 3 3 'X'
        } else {
            $rows = New-Rows $DERRICK_SIZE $DERRICK_SIZE ' '
            foreach ($c in 1, 5) { foreach ($a in 1, 5) { Set-ShapeCell $rows $c $a 'M' } }
            Set-ShapeCell $rows 3 3 'X'
        }
        $layers += , $rows
    }
    , $layers
}
function Get-AnnexShape {
    $layers = @()
    for ($b = 0; $b -lt $ANNEX_HEIGHT; $b++) {
        $y = $ANNEX_HEIGHT - 1 - $b
        if ($y -eq 0 -or $y -eq ($ANNEX_HEIGHT - 1)) {
            $rows = New-Rows $ANNEX_LENGTH $ANNEX_WIDTH 'V'
        } else {
            $rows = New-Rows $ANNEX_LENGTH $ANNEX_WIDTH ' '
            for ($c = 0; $c -lt $ANNEX_LENGTH; $c++) {
                for ($a = 0; $a -lt $ANNEX_WIDTH; $a++) {
                    if ($c -eq 0 -or $c -eq ($ANNEX_LENGTH - 1) -or $a -eq 0 -or $a -eq ($ANNEX_WIDTH - 1)) {
                        Set-ShapeCell $rows $c $a 'V'
                    }
                }
            }
            if ($y -eq 2) {
                for ($c = 1; $c -le ($ANNEX_LENGTH - 2); $c++) { Set-ShapeCell $rows $c 2 'X' }
            }
            if ($y -eq 1) {
                Set-ShapeCell $rows 3 ($ANNEX_WIDTH - 1) 'e'
                Set-ShapeCell $rows 7 ($ANNEX_WIDTH - 1) 'f'
                Set-ShapeCell $rows 11 ($ANNEX_WIDTH - 1) 'g'
            }
        }
        $layers += , $rows
    }
    , $layers
}

# ------------------------------------------------------------------------ tallying
function Measure-Shape($layers) {
    $counts = @{}
    foreach ($layer in $layers) {
        foreach ($row in $layer) {
            foreach ($ch in $row.ToCharArray()) {
                if ($ch -eq ' ' -or $ch -eq 'A' -or $ch -eq '~') { continue }
                $name = $ELEMENT_ITEM["$ch"]
                if (-not $name) { throw "gen-bom.ps1: shape char '$ch' has no entry in ELEMENT_ITEM." }
                if ($counts.ContainsKey($name)) { $counts[$name] += 1 } else { $counts[$name] = 1 }
            }
        }
    }
    $counts
}

# ------------------------------------------------------------------------ the bills
Assert-Source

$BTS_BOM = @{}
$BTS_BOM.Platform = Measure-Shape (Get-FloorShape)
$BTS_BOM.Spiral = Measure-Shape (Get-SpiralShape)
$BTS_BOM.Gallery = Measure-Shape (Get-GalleryShape)
$BTS_BOM.Tower = Measure-Shape (Convert-TowerShape (Get-TowerShape))
$BTS_BOM.Vent = Measure-Shape (Get-VentShape)
$BTS_BOM.Switchyard = Measure-Shape (Get-SwitchyardShape)
$BTS_BOM.Derrick = Measure-Shape (Get-DerrickShape)
$BTS_BOM.Annex = Measure-Shape (Get-AnnexShape)
$BTS_BOM.Silo = @{}
$BTS_BOM.Duct = @{}
for ($t = 1; $t -le $MAX_TIER; $t++) {
    $BTS_BOM.Silo[$t] = Measure-Shape (Get-CoreShape $t)
    $BTS_BOM.Duct[$t] = Measure-Shape (Get-DuctShape $t)
}
$BTS_BOM.Edge = @{}
$BTS_BOM.Pillars = @{}
for ($t = 1; $t -le $MAX_TIER; $t++) {
    $BTS_BOM.Edge[$t] = Get-Edge $t
    $BTS_BOM.Pillars[$t] = $COLUMN_CAP[$t]
}

# Cross-checks against arithmetic the Java states independently of the shapes. A shape
# that stopped agreeing with its own documented cell count is a ported-wrong shape.
for ($t = 1; $t -le $MAX_TIER; $t++) {
    $n = Get-Edge $t
    $k = $CUT[$t]
    $octagon = $n * $n - 2 * $k * ($k + 1)
    $interior = $COLUMN_CAP[$t]
    $fuel = $BTS_BOM.Silo[$t]['Reactor Fuel Pillar Frame']
    if ($fuel -ne ($interior * $SEGMENTS_PER_PILLAR)) {
        throw ("gen-bom.ps1: tier $t counted $fuel fuel-column cells but COLUMN_CAP says " +
            "$interior pillars x $SEGMENTS_PER_PILLAR segments = " + ($interior * $SEGMENTS_PER_PILLAR) + ".")
    }
    if ($BTS_BOM.Silo[$t]['Rod Loading Port'] -ne $interior) {
        throw "gen-bom.ps1: tier $t rod-port layer holds $($BTS_BOM.Silo[$t]['Rod Loading Port']) cells, expected $interior."
    }
    if ($octagon -le 0) { throw "gen-bom.ps1: tier $t octagon cell count is not positive." }
}
$expectedPad = (Get-Edge $MAX_TIER) * (Get-Edge $MAX_TIER) - 2 * $CUT[$MAX_TIER] * ($CUT[$MAX_TIER] + 1)
if ($BTS_BOM.Platform['Tungstensteel Reinforced Block'] -ne $expectedPad) {
    throw "gen-bom.ps1: shield pad counted $($BTS_BOM.Platform['Tungstensteel Reinforced Block']), expected $expectedPad."
}

# --------------------------------------- the same bills, rendered as generated Java
# Review finding C5/A4: /btskit printed a SECOND, hand-typed shopping list. It had
# drifted by up to 3.7x (240 wall casings quoted where the tier-5 silo takes 1,808)
# and it named "Iridium Tungstensteel Block", which is the GT ItemList ENUM name, not
# a registered display name - NEI returns nothing for it. A command and a questbook
# keeping separate numbers is a bug that regrows on every geometry edit, so /btskit
# owns no numbers and no material names any more: it reads the class rendered below,
# which is written from the SAME in-memory $BTS_BOM the Blueprint quests interpolate.
#
# The rendering is re-derived and checked on EVERY run of this script, dot-sourced runs
# included. gen-questbook.ps1 dot-sources this file, so the questbook physically cannot
# be regenerated while the checked-in BTSBom.java disagrees - the two consumers move
# together or not at all.
$script:BomJava = Join-Path (Split-Path $PSScriptRoot -Parent) `
    'src\main\java\com\nateyburr\beyondthestars\BTSBom.java'

# "Input Hatch (IV)" -> INPUT_HATCH_IV. Every item name reaches the Java exactly once,
# as the value of one constant, and the bill tables refer to it by that constant.
# CommandBTSKit may only name a material through these, so an item that is renamed or
# drops out of the element alphabet breaks 'gradlew compileJava' rather than silently
# printing a block that does not exist - which is exactly how A4 shipped.
function Get-ItemConstName([string]$item) {
    $id = ($item.ToUpperInvariant() -replace '[^A-Z0-9]+', '_').Trim('_')
    if ($id -notmatch '^[A-Z][A-Z0-9_]*$') {
        throw "gen-bom.ps1: item name '$item' does not yield a legal Java identifier ('$id')."
    }
    $id
}

function Format-JavaBill([string]$label, $counts) {
    $entries = @(
        $counts.GetEnumerator() |
            Sort-Object -Property @{ Expression = { $_.Value }; Descending = $true }, Name)
    $items = @()
    $nums = @()
    foreach ($e in $entries) {
        $items += (Get-ItemConstName $e.Name)
        $nums += [string]$e.Value
    }
    'new Bill("' + $label + '", new String[] { ' + ($items -join ', ') + ' }, new int[] { ' +
    ($nums -join ', ') + ' })'
}

function Add-Counts($acc, $counts, [int]$times) {
    foreach ($e in $counts.GetEnumerator()) {
        if ($acc.ContainsKey($e.Name)) { $acc[$e.Name] += ($e.Value * $times) }
        else { $acc[$e.Name] = ($e.Value * $times) }
    }
}

# Everything one hologram raises at a tier, towers included - the figure a player in
# creative actually wants before they hit the projector.
function Get-TotalCounts([int]$t) {
    $acc = @{}
    Add-Counts $acc $BTS_BOM.Platform 1
    Add-Counts $acc $BTS_BOM.Silo[$t] 1
    Add-Counts $acc $BTS_BOM.Duct[$t] 1
    Add-Counts $acc $BTS_BOM.Spiral 1
    Add-Counts $acc $BTS_BOM.Gallery 1
    Add-Counts $acc $BTS_BOM.Tower $TOWER_COUNT
    Add-Counts $acc $BTS_BOM.Vent 1
    Add-Counts $acc $BTS_BOM.Switchyard 1
    Add-Counts $acc $BTS_BOM.Derrick 1
    Add-Counts $acc $BTS_BOM.Annex 1
    $acc
}

function Get-BomJavaText {
    $itemNames = @($ELEMENT_ITEM.Values | Sort-Object -Unique)
    foreach ($nm in $itemNames) {
        if ($nm -match '["\\/]') {
            throw "gen-bom.ps1: item name '$nm' holds a character that would break the generated Java."
        }
    }
    $L = New-Object System.Collections.Generic.List[string]

    $L.Add('// GENERATED FILE - DO NOT EDIT.')
    $L.Add('//')
    $L.Add('// Rendered by questbook/gen-bom.ps1 out of the structure shape code of')
    $L.Add('// MTEReactorCoreMk1 and MTEModuleCoolingTower, and out of nothing else. The very')
    $L.Add('// same in-memory tally is what gen-questbook.ps1 interpolates into the five')
    $L.Add('// Blueprint quests, so the questbook and /btskit cannot quote different numbers.')
    $L.Add('//')
    $L.Add('// gen-bom.ps1 re-derives this file on every run and throws if the copy on disk')
    $L.Add('// disagrees; gen-questbook.ps1 dot-sources gen-bom.ps1, so the questbook cannot be')
    $L.Add('// rebuilt while this file is stale. Whitespace and comments are ignored by that')
    $L.Add('// check, so spotlessApply is free to reformat what is below.')
    $L.Add('//')
    $L.Add('// The reverse is sealed the same way: gen-bom.ps1 NEVER rewrites this file on its')
    $L.Add('// own. The one script run that rewrites it also runs gen-questbook.ps1 and rewrites')
    $L.Add('// questbook/DefaultQuests from the same tally, and rolls this file back if that')
    $L.Add('// throws - so the pair is either written together or left in the stale state every')
    $L.Add('// gate in that script refuses. It cannot come to rest split and silent.')
    $L.Add('//')
    $L.Add('// To change a count, change the geometry in the Java, then run ONE command:')
    $L.Add('//   powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1')
    $L.Add('//   gradlew spotlessApply compileJava')
    $L.Add('package com.nateyburr.beyondthestars;')
    $L.Add('')
    $L.Add('/**')
    $L.Add(' * Bills of materials for every piece the Reactor Core Mk1 projection raises, derived')
    $L.Add(' * cell by cell from the shape functions. Consumed by CommandBTSKit; mirrored count')
    $L.Add(' * for count by the Blueprint quests.')
    $L.Add(' */')
    $L.Add('public final class BTSBom {')
    $L.Add('')
    $L.Add('    private BTSBom() {}')
    $L.Add('')
    $L.Add('    // ------------------------------------------------------------- item names')
    $L.Add('    // Display names exactly as GregTech registers them (assets/gregtech/lang).')
    $L.Add('    // Name a material through these constants and nowhere else.')
    foreach ($nm in $itemNames) {
        $L.Add('    public static final String ' + (Get-ItemConstName $nm) + ' = "' + $nm + '";')
    }
    $L.Add('')
    $L.Add('    // ------------------------------------------------------------- dimensions')
    $L.Add('    public static final int MAX_TIER = ' + $MAX_TIER + ';')
    $L.Add('')
    $L.Add('    /** Cooling towers this controller raises (nw + sw). */')
    $L.Add('    public static final int TOWERS = ' + $TOWER_COUNT + ';')
    $edge = @('0')
    $pill = @('0')
    $duct = @('0')
    for ($t = 1; $t -le $MAX_TIER; $t++) {
        $edge += [string]$BTS_BOM.Edge[$t]
        $pill += [string]$BTS_BOM.Pillars[$t]
        $duct += [string](Get-DuctLength $t)
    }
    $L.Add('')
    $L.Add('    /** Silo octagon edge, indexed by tier; slot 0 is unused. */')
    $L.Add('    public static final int[] EDGE = { ' + ($edge -join ', ') + ' };')
    $L.Add('')
    $L.Add('    /** Fuel pillars the silo interior holds, indexed by tier. */')
    $L.Add('    public static final int[] PILLARS = { ' + ($pill -join ', ') + ' };')
    $L.Add('')
    $L.Add('    /** Spine duct length, indexed by tier. */')
    $L.Add('    public static final int[] DUCT_LENGTH = { ' + ($duct -join ', ') + ' };')
    $L.Add('')
    $L.Add('    // ------------------------------------------------------------------ bills')
    $L.Add('')
    $L.Add('    /** One piece: item names paired with the cell count the projection places. */')
    $L.Add('    public static final class Bill {')
    $L.Add('')
    $L.Add('        private final String piece;')
    $L.Add('        private final String[] items;')
    $L.Add('        private final int[] counts;')
    $L.Add('')
    $L.Add('        Bill(String piece, String[] items, int[] counts) {')
    $L.Add('            this.piece = piece;')
    $L.Add('            this.items = items;')
    $L.Add('            this.counts = counts;')
    $L.Add('        }')
    $L.Add('')
    $L.Add('        public String piece() {')
    $L.Add('            return piece;')
    $L.Add('        }')
    $L.Add('')
    $L.Add('        /** Entry count. Entries are ordered biggest tally first. */')
    $L.Add('        public int size() {')
    $L.Add('            return items.length;')
    $L.Add('        }')
    $L.Add('')
    $L.Add('        public String item(int i) {')
    $L.Add('            return items[i];')
    $L.Add('        }')
    $L.Add('')
    $L.Add('        public int count(int i) {')
    $L.Add('            return counts[i];')
    $L.Add('        }')
    $L.Add('')
    $L.Add('        /** Tally of one item, 0 when this piece does not use it. */')
    $L.Add('        public int of(String item) {')
    $L.Add('            for (int i = 0; i < items.length; i++) {')
    $L.Add('                if (items[i].equals(item)) return counts[i];')
    $L.Add('            }')
    $L.Add('            return 0;')
    $L.Add('        }')
    $L.Add('    }')
    $L.Add('')
    $L.Add('    public static final Bill PLATFORM = ' + (Format-JavaBill 'Starter platform' $BTS_BOM.Platform) + ';')
    $L.Add('')
    $L.Add('    public static final Bill SPIRAL = ' + (Format-JavaBill 'Spiral chamber' $BTS_BOM.Spiral) + ';')
    $L.Add('')
    $L.Add('    public static final Bill GALLERY = ' + (Format-JavaBill 'Rotor gallery + output manifold' $BTS_BOM.Gallery) + ';')
    $L.Add('')
    $L.Add('    public static final Bill TOWER = ' + (Format-JavaBill 'Cooling tower (each)' $BTS_BOM.Tower) + ';')
    $L.Add('')
    $L.Add('    public static final Bill VENT = ' + (Format-JavaBill 'Vent Stack' $BTS_BOM.Vent) + ';')
    $L.Add('')
    $L.Add('    public static final Bill SWITCHYARD = ' + (Format-JavaBill 'Switchyard' $BTS_BOM.Switchyard) + ';')
    $L.Add('')
    $L.Add('    public static final Bill DERRICK = ' + (Format-JavaBill 'Extraction Derrick' $BTS_BOM.Derrick) + ';')
    $L.Add('')
    $L.Add('    public static final Bill ANNEX = ' + (Format-JavaBill 'Enrichment Annex' $BTS_BOM.Annex) + ';')
    $siloRows = @('        null')
    $ductRows = @('        null')
    $totalRows = @('        null')
    for ($t = 1; $t -le $MAX_TIER; $t++) {
        $siloRows += ('        ' + (Format-JavaBill ('Silo tier ' + $t) $BTS_BOM.Silo[$t]))
        $ductRows += ('        ' + (Format-JavaBill ('Spine duct tier ' + $t) $BTS_BOM.Duct[$t]))
        $totalRows += ('        ' + (Format-JavaBill ('Whole plant tier ' + $t) (Get-TotalCounts $t)))
    }
    $L.Add('')
    $L.Add('    /** Indexed by tier; slot 0 is null. */')
    $L.Add('    public static final Bill[] SILO = {')
    $L.Add(($siloRows -join ",`n") + ' };')
    $L.Add('')
    $L.Add('    /** Indexed by tier; slot 0 is null. */')
    $L.Add('    public static final Bill[] DUCT = {')
    $L.Add(($ductRows -join ",`n") + ' };')
    $L.Add('')
    $L.Add('    /** Platform + silo + duct + spiral + gallery + TOWERS towers, by tier. */')
    $L.Add('    public static final Bill[] TOTAL = {')
    $L.Add(($totalRows -join ",`n") + ' };')
    $L.Add('}')
    [string]::Join("`n", $L) + "`n"
}

# Whitespace- and comment-insensitive, so spotlessApply reformatting the generated file
# is never mistaken for drift, while any changed count, name or ordering still is.
function Get-NormalizedJava([string]$text) {
    $t = [regex]::Replace($text, '/\*.*?\*/', '', 'Singleline')
    $t = [regex]::Replace($t, '//[^\r\n]*', '')
    [regex]::Replace($t, '\s+', '')
}

$script:BomJavaText = Get-BomJavaText
$script:BomJavaWanted = Get-NormalizedJava $script:BomJavaText
$script:BomJavaOnDisk = ''
if (Test-Path $script:BomJava) {
    $script:BomJavaOnDisk = Get-NormalizedJava ([System.IO.File]::ReadAllText($script:BomJava))
}
$script:BomJavaStale = $script:BomJavaOnDisk -ne $script:BomJavaWanted

# Dot-sourced - gen-questbook.ps1's path. Verify only, and refuse to hand over a
# $BTS_BOM that the shipped Java disagrees with. THE WRITE DOES NOT LIVE HERE: a
# standalone run rewrites BTSBom.java at the foot of this file, in the same step that
# rewrites the questbook, so the two can never be produced separately.
if ($MyInvocation.InvocationName -eq '.' -and $script:BomJavaStale) {
    throw ("gen-bom.ps1: src/main/java/com/nateyburr/beyondthestars/BTSBom.java does not " +
        "match the bills derived from the current shape code, so /btskit would print " +
        "different numbers than the questbook. Re-render it AND the questbook together " +
        "with 'powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1' " +
        "(then gradlew spotlessApply compileJava).")
}

# One tally entry as a thousands-separated string, for interpolation into a quest
# description. An item the piece does not use answers '0' rather than blowing up, so a
# bill can ask about a material a tier does not happen to have.
function Bom($counts, [string]$item) {
    if ($counts.ContainsKey($item)) { '{0:N0}' -f $counts[$item] } else { '0' }
}

if ($MyInvocation.InvocationName -ne '.') {
    Write-Host ''
    Write-Host 'BILLS OF MATERIALS - derived from the shape code, never typed by hand'
    Write-Host '===================================================================='
    if ($script:BomJavaStale) {
        Write-Host ('  ' + $script:BomJava + ' is STALE - it and DefaultQuests\ are rewritten below')
    } else {
        Write-Host ('  ' + $script:BomJava + ' already agrees with these bills')
    }
    foreach ($piece in @('Platform', 'Spiral', 'Gallery', 'Tower', 'Vent', 'Switchyard', 'Derrick', 'Annex')) {
        Write-Host ''
        Write-Host ("{0} (same at every tier)" -f $piece.ToUpper())
        $BTS_BOM[$piece].GetEnumerator() | Sort-Object -Property @{ Expression = { $_.Value }; Descending = $true }, Name |
            ForEach-Object { Write-Host ("  {0,8:N0}  {1}" -f $_.Value, $_.Name) }
    }
    foreach ($t in 1..$MAX_TIER) {
        Write-Host ''
        Write-Host ("SILO TIER {0} (octagon {1}x{1}, {2} pillars)" -f $t, $BTS_BOM.Edge[$t], $BTS_BOM.Pillars[$t])
        $BTS_BOM.Silo[$t].GetEnumerator() | Sort-Object -Property @{ Expression = { $_.Value }; Descending = $true }, Name |
            ForEach-Object { Write-Host ("  {0,8:N0}  {1}" -f $_.Value, $_.Name) }
        Write-Host ("SPINE DUCT TIER {0} (length {1})" -f $t, (Get-DuctLength $t))
        $BTS_BOM.Duct[$t].GetEnumerator() | Sort-Object -Property @{ Expression = { $_.Value }; Descending = $true }, Name |
            ForEach-Object { Write-Host ("  {0,8:N0}  {1}" -f $_.Value, $_.Name) }
    }
    Write-Host ''
}

# ============================================================ T1: BOTH CONSUMERS OR NEITHER
# Two things in the game quote these bills: /btskit (from BTSBom.java) and the five
# Blueprint quests (from questbook\DefaultQuests\**.json). Nothing anywhere re-derives
# the quest JSON from $BTS_BOM after the fact - there is no gate on it, no test, and
# neither AssertStacks nor AssertDesc looks at a count - so the ONLY thing that can keep
# the two in step is that they are always written together.
#
# They were not. The gate above used to be one-directional: dot-sourced it threw when
# BTSBom.java was stale (questbook-newer-than-Java: sealed), but run as a script it
# SILENTLY rewrote BTSBom.java and stopped. Java-newer-than-questbook was the unguarded
# direction - and it is exactly the one the documented workflow takes: change the
# geometry, run this script, compile. /btskit then printed the new counts, the shipped
# quest JSON still printed the old ones, exit code 0, no warning. (Review finding T1.)
#
# So the write is HERE, welded to the regeneration, and there is no switch that performs
# one without the other. gen-questbook.ps1 deletes DefaultQuests\ and rewrites every file
# from a fresh derivation of the same shape code, so once this block returns the two
# consumers hold the same numbers by construction rather than by the developer
# remembering a second command.
#
# NO RECURSION: gen-questbook.ps1 dot-sources this file, and a dot-sourced run takes the
# verify-only branch above and stops there. BTSBom.java is already on disk by then, so
# that branch passes rather than throwing.
#
# IF THE REGENERATION THROWS, the previous BTSBom.java is PUT BACK before the error is
# re-thrown. Not because that makes the two agree in that instant - it does not.
# gen-questbook.ps1 clears DefaultQuests\ and writes it file by file, so a failure part
# way through can leave the folder empty, partial, or even fully rewritten with the NEW
# numbers. The rollback is about where the tree comes to REST: nothing in this system can
# detect a stale DefaultQuests\, but every gate here detects a stale BTSBom.java, so a
# failed run is deliberately parked in the detectable state. After it, the next
# dot-sourced run throws, the next script run rewrites both, and the run that just failed
# exited non-zero. What can never happen is the tree coming to rest split and SILENT,
# which is what T1 was. Only BTSBom.java is rolled back, because it is the only file this
# script owns - `git checkout questbook` or simply re-running fixes DefaultQuests\.
if ($MyInvocation.InvocationName -ne '.') {
    $previousBom = $null
    if ($script:BomJavaStale) {
        if (Test-Path $script:BomJava) {
            $previousBom = [System.IO.File]::ReadAllBytes($script:BomJava)
        }
        [System.IO.File]::WriteAllText(
            $script:BomJava,
            $script:BomJavaText,
            (New-Object System.Text.UTF8Encoding($false)))
    }
    try {
        & (Join-Path $PSScriptRoot 'gen-questbook.ps1')
    } catch {
        if ($script:BomJavaStale) {
            if ($null -eq $previousBom) { Remove-Item -Force -Confirm:$false $script:BomJava }
            else { [System.IO.File]::WriteAllBytes($script:BomJava, $previousBom) }
            Write-Warning ('gen-questbook.ps1 failed. ' + $script:BomJava + ' was ROLLED BACK to ' +
                'the stale copy ON PURPOSE - stale is the one state every gate here refuses, so ' +
                'nothing can ship quietly. DefaultQuests\ may now be partial or half-new: fix the ' +
                'error below and re-run this script, or git checkout questbook first.')
        }
        throw
    }
    Write-Host ''
    if ($script:BomJavaStale) {
        Write-Host ('  re-rendered ' + $script:BomJava)
        Write-Host '  regenerated DefaultQuests\ from the same tally'
        Write-Host '  NEXT: gradlew spotlessApply compileJava'
    } else {
        Write-Host ('  ' + $script:BomJava + ' was already in step; DefaultQuests\ regenerated from the same tally')
    }
    Write-Host ''
}
