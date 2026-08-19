# Generates the "Beyond the Stars" BetterQuesting quest-line overlay in GTNH 2.9.0's
# split DefaultQuests format (QuestLines/<line>/ placements + QuestLine.json, and
# Quests/<line>/<quest>.json). Run from this folder:  powershell -File gen-questbook.ps1
# Output goes to .\DefaultQuests\ ; merge per README.md.
#
# Also emits ONE entry quest into the pack's own "How to Generate Power" chapter
# (TC-5 pt 5): only ADDED files, never a modified pack file.
#
# IDs use questIDHigh = 190 (non-zero) so they can never collide with pack quests
# (which all use high 0). HIGH must stay <= 255: its byte then sits alone in a base64
# triple whose characters are provably never '+' or '/', so filenames stay legal.
# Lows are auto-advanced past any value whose base64 would contain '+' or '/'.
#
# Bill-of-materials counts in the Blueprints row are computed from the canonical
# geometry in docs/plant-geometry.md (verified against the shape code at 0.16.0):
#   platform floor 50x50 = 2243 Light Concrete + 249 TS-Reinforced pad + 8 markers;
#   silo per tier: walls 515/807/(960+103)/(1340+127)/(1664+139), dome 113/197/273/
#   365/481, pillars 45/77/109/157/201 (x8 fuel columns + 1 port each);
#   spine casings 96/88/80/72/64 + 4 housings; spiral 327 casings + 50 concrete;
#   turbine generator 416 casings (313 gallery + 103 manifold); tower 736 concrete
#   + 51 solid steel ring. See scratch derivation in the TC-5 work log.

$ErrorActionPreference = 'Stop'
$HIGH = 190
$out = Join-Path $PSScriptRoot 'DefaultQuests'

function B64Id([long]$high, [long]$low) {
    $b = New-Object byte[] 16
    for ($i = 0; $i -lt 8; $i++) {
        $b[7 - $i] = ($high -shr (8 * $i)) -band 0xFF
        $b[15 - $i] = ($low -shr (8 * $i)) -band 0xFF
    }
    [Convert]::ToBase64String($b)
}

function SafeLow([long]$startLow) {
    $low = $startLow
    while ((B64Id $HIGH $low) -match '[+/]') { $low++ }
    $low
}

function NamePrefix([string]$name) {
    $clean = ($name -replace '[^A-Za-z0-9]', '')
    if ($clean.Length -gt 16) { $clean = $clean.Substring(0, 16) }
    $clean
}

function Icon([string]$id, [int]$damage, [int]$count) {
    [ordered]@{ 'Count:3' = $count; 'Damage:2' = $damage; 'OreDict:8' = ''; 'id:8' = $id }
}

# BetterQuesting's parser expects BOM-less files (the pack's own files have no BOM).
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function WriteText([string]$path, [string]$text) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, $text, $Utf8NoBom)
}

function WriteJson([string]$path, $obj) {
    WriteText $path (($obj | ConvertTo-Json -Depth 12) + "`n")
}

# ---------------------------------------------------------------- quest definitions
# keys: Key (unique), Name, Desc, Icon, Prereqs (keys), Task ('checkbox', retrieval
# icon hashtable, or an ARRAY of icon hashtables for multi-item retrieval), X, Y.
# Optional: CountAs (0 = does not count toward line completion; default 1),
# RawPrereqs (array of @{High=..; Low=..} for prerequisites outside this line).
$ICON_CTRL = Icon 'gregtech:gt.blockmachines' 28100 1
$quests = @(
    @{ Key='groundwork'; Name='The Blueprint'; X=0; Y=0; Icon=$ICON_CTRL; Task=$ICON_CTRL; Prereqs=@()
       Desc='ONE projection builds the whole plant. Step 1: place the Nuclear Plant Foundation controller - it is the main station, and the reactor grows around it. Step 2: set Build Tier 1-5 in its GUI (or hold a trigger stack of 2-5). Step 3: point a Hologram Projector at the controller - it builds EVERYTHING: the 50x50 platform, the Steam Generator silo, the Spine duct, and the empty shells of the Turbine Hall and both Cooling Towers (in survival, the projector auto-builds from your inventory). Step 4: stand the three module controllers on the three glowing floor markers, each FACING AWAY from its building - they dock, and each forms once its hatches go in. Step 5: hatches - silo walls (including an output bus for the depleted rods), Dynamo + Maintenance + Input into the hall plinth, and one Maintenance hatch into each tower base ring. Then fuel rods into the top ports, water in. That is the entire build. The other markers are spare dock positions for future modules - keep them clear.' }
    @{ Key='silo'; Name='Raising the Silo'; X=48; Y=0; Icon=(Icon 'beyondthestars:reactorFuelColumn' 0 1); Task=(Icon 'beyondthestars:reactorFuelColumn' 0 8); Prereqs=@('groundwork')
       Desc='The STEAM GENERATOR is the heart the projection raised for you: nine to seventeen blocks wide by Build Tier, ribbed walls twice the diameter tall, radiation bands, a hemispherical shield dome - and your main station controller embedded in its face at ground level. Hatches go in the silo walls (energy, maintenance, water in, steam out, input bus, output bus - the depleted rods leave through the bus). Inside stand the fuel pillars, EIGHT segments high, each with a Rod Loading Port on top. This machine makes a specific REACTOR STEAM - everything downstream is built to spend it well.' }
    @{ Key='firststeam'; Name='First Steam'; X=96; Y=0; Icon=(Icon 'minecraft:water_bucket' 0 1); Task='checkbox'; Prereqs=@('silo')
       Desc='Feed water, load fuel, set the control rods (1-24 in the GUI; a programmed circuit overrides). With no downstream machinery the plant simply exports Reactor Steam - or Hot Coolant if you feed it IC2 Coolant. It works, it is honest, and it is a fraction of what this plant can do. The real numbers live at the end of a much longer pipe.' }
    @{ Key='cascade'; Name='The Decay Cascade'; X=48; Y=48; Icon=(Icon 'beyondthestars:fuelRodUranium' 0 1); Task=(Icon 'beyondthestars:fuelRodUranium' 0 8); Prereqs=@('silo')
       Desc='Real reactors do not eat neat assemblies from a bus - they run RODS. Each fuel pillar is a working 8-segment gravity column: insert a rod at the TOP and it falls to the lowest empty segment; only the BOTTOM rod burns, one decay cycle at a time; the spent rod drops into the base buffer for extraction and the whole stack shifts down. Your job is the automation: keep up to 201 pillars topped up and their bases drained. Classic assemblies in the input bus still work - at HALF output. The cascade is where the other half lives.' }
    @{ Key='spine'; Name='The Spine'; X=144; Y=0; Icon=(Icon 'gregtech:gt.blockcasings' 12 1); Task='checkbox'; Prereqs=@('firststeam')
       Desc='Steam leaves the silo through the 3x3 Spine duct the projection built east of the silo. Along its bore sit FOUR rotor sockets - in survival the auto-build blanks them with turbine casings, so swap in your four Turbine Housings, then RIGHT-CLICK each to set CLOCKWISE or ANTICLOCKWISE. Order matters: backwash swirl accumulates from housing to housing. A rotor spinning WITH the incoming swirl rides it for more steam speed (and more heat); one set AGAINST it cancels the swirl for more EU per litre. Same-direction trains are fast and thirsty, alternating trains are efficient and cool. There is no single right answer - only your goal.' }
    @{ Key='spindoctrine'; Name='Spin Doctrine'; X=144; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 32304 1); Task='checkbox'; Prereqs=@('spine')
       Desc='The math, plainly: swirl S starts at 0 and each housing adds +1 (CW) or -1 (CCW). Riding rotors multiply steam speed by 1 + 0.08x|S|; opposing rotors multiply efficiency by 1 + 0.10x|S|; whatever swirl survives the fourth housing enters the spiral chamber as extra heat. CW-CW-CW-CW is a dragster; CW-CCW-CW-CCW is an economist. Retune the train when your goals - or your propellants - change.' }
    @{ Key='spiral'; Name='The Spiral'; X=192; Y=0; Icon=(Icon 'gregtech:gt.blockmachines' 28104 1); Task='checkbox'; Prereqs=@('spine')
       Desc='The 7x7 injection chamber spirals the steam downward past EIGHT precision injectors. Nothing here runs on its own clock: YOU set the pace. A redstone rising edge on the TURBINE CONTROL BLOCK starts a cycle, and the cycle is ACCEPTED only if a buffered steam slug is there to drink - the SENSOR HATCH echoes every accepted cycle with a 2-tick pulse, so use it as your confirmation. Each injector wants EXACTLY 1L of propellant inside a window of +/-2 ticks around that edge. Timed litres spin the fan up; miss the window - or double-inject - and the litre is wasted AND the chamber takes a heat spike. Clock it faster and you burn more propellant for more RPM. Build the timing circuit. Then trust it.' }
    @{ Key='octuple'; Name='Octuple Injection'; X=192; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 30671 1); Task='checkbox'; Prereqs=@('spiral')
       Desc='Eight injectors, one ladder. Worst to best: Benzene, High Octane Gasoline, H8N4C2O4 Rocket Fuel, then Naquadah Fuel Lv 1 through Lv 6. Propellant sets how hard each accepted cycle spins the fan: RPM gain = 176 x P x (timed/8), bled off by 0.5% every tick. All eight timed on Lv 6 at one cycle per second is what finally touches the 12,000 redline; Lv 3 settles near 9,200, Benzene near 2,300. Past Lv 3 the P gains go LINEAR (+0.5 per level to 6.5 at Lv 6, UMV) - the plant will drink it, but the sane stop is Lv 3 unless you are chasing a number, not a ratio.' }
    @{ Key='coldblood'; Name='Cold Blood'; X=192; Y=96; Icon=(Icon 'ic2:itemFluidCell' 0 1); Task='checkbox'; Prereqs=@('spiral')
       Desc='RPM squared is heat. Coolant drain per tick is ceil(200 x (RPM/1000)^2 / Q) - quadratic in fan speed, divided by coolant quality - and if the chamber starves, the heat climbs until the turbine SEIZES: the Spine''s rotor housings are torn apart, plant down. The ladder, worst to best: Water, Distilled Water, IC2 Coolant, Super Coolant, Liquid Cryotheum, Tachyon Rich Temporal Fluid. Every propellant tier you climb, your coolant logistics must climb with it. The plant does not forgive a cooling loop designed for last month''s speed.' }
    @{ Key='assembly'; Name='The Assembly'; X=240; Y=48; Icon=(Icon 'gregtech:gt.blockmachines' 28100 1); Task='checkbox'; Prereqs=@('spindoctrine','octuple','coldblood')
       Desc='The end of the pipe: the TURBINE GENERATOR. The spiral chamber''s downcomer feeds a rotor gallery that opens into a 7x7 output manifold, and the dynamos on its back face pay out EU/t = litres x spine speed x efficiency x (RPM/1000) x average rotor condition. Seat a TURBINE ROTOR in each of the eight stage housings - they wear like real blades, and every chip costs 5% of that stage until the rotor shatters at ten. Rod cascade fed, spine tuned, cycles clocked, injectors timed, coolant loop scaled, rotors fresh: this is where the insane numbers happen. (The old Turbine Hall retires here - its fans and multipliers live on inside this chain.)' }
    @{ Key='writteninfire'; Name='Written in Fire'; X=96; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 32605 1); Task='checkbox'; Prereqs=@('silo')
       Desc='Read this before it reads you. Core heat SCRAMS the plant at 8,000 and MELTS DOWN at 10,000 - a vertical detonation up the silo stack and a 30-minute radiation field centered on the wreck (hazmat counters it; config can disable it). Every shutdown leaves DECAY HEAT: twenty minutes of mandatory cooling water, deferred - never waived - if you dismantle the silo. Cooling towers halve the demand and speed passive decay. The plant is generous with power and unforgiving with physics.' }

    # ------------------------- Blueprints row (TC-5 pt 7): bills of materials, y=120
    @{ Key='controllers'; Name='The Controllers'; X=0; Y=120; Icon=$ICON_CTRL; Prereqs=@()
       Task=@( (Icon 'gregtech:gt.blockmachines' 28100 1), (Icon 'gregtech:gt.blockmachines' 28105 2) )
       Desc='Three machines run the whole campus: ONE Nuclear Plant Foundation (the main station) and TWO Plant Module - Cooling Tower controllers for the canonical twin-tower layout. Stand each tower controller on its dock marker FACING AWAY from its tower body. This quest only checks that you own them - nothing is consumed. The blueprints along this row list every block each building needs.' }
    @{ Key='bpplatform'; Name='Blueprint: Platform'; X=48; Y=120; Icon=(Icon 'gregtech:gt.blockconcretes' 8 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'The mandatory first projection: a 50x50 two-layer foundation - floor layer one block down, walking level open.'
         'BILL OF MATERIALS:'
         '- 2243x Light Concrete'
         '- 249x Tungstensteel-Reinforced Block (the shield-pad octagon under the silo)'
         '- 8x Solid Steel Machine Casing (flush dock markers)'
         'The walking level stays open: the main station stands on the pad''s front line, and all eight dock markers stay CLEAR until a module controller stands on one. In survival the Hologram Projector pulls every block from your inventory.'
       ) -join "`n") }
    @{ Key='bpsilo'; Name='Blueprint: Steam Generator'; X=96; Y=120; Icon=(Icon 'beyondthestars:reactorFuelColumn' 0 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'The silo scales with Build Tier: diameter 7+2xT, ribbed walls twice the diameter tall, hemispherical shield dome. Hatches are identical at every tier, socketed in the flat front face around the main station: 4x Energy Hatch, 2x Input Hatch, 1x Output Hatch, 1x Input Bus, 1x Output Bus, 1x Maintenance Hatch, 1x Silo Control Block.'
         'WALLS + DOME BY TIER:'
         '- T1 (9 wide): 515x Radiation Proof Machine Casing + 113x Tungstensteel-Reinforced Block (dome)'
         '- T2 (11 wide): 807x Radiation Proof Machine Casing + 197x Tungstensteel-Reinforced Block'
         '- T3 (13 wide): 960x Europium Reinforced Radiation Proof Casing + 103x Radiation Proof (band rings) + 273x Iridium Tungstensteel-Reinforced Block (dome)'
         '- T4 (15 wide): 1340x Europium Reinforced + 127x Radiation Proof + 365x Iridium Tungstensteel-Reinforced'
         '- T5 (17 wide): 1664x Radiant Naquadah Alloy Casing + 139x Radiation Proof + 481x Iridium Tungstensteel-Reinforced'
         'FUEL PILLARS (optional interior; full count per tier): T1 45 pillars (360x Reactor Fuel Column + 45x Rod Loading Port), T2 77 (616+77), T3 109 (872+109), T4 157 (1256+157), T5 201 (1608+201). Partial pillars run at partial output.'
       ) -join "`n") }
    @{ Key='bpspine'; Name='Blueprint: Spine'; X=144; Y=120; Icon=(Icon 'gregtech:gt.blockcasings' 12 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'A 3x3 duct from the silo''s east wall to the spiral chamber. It always ends on the same line, so it SHRINKS as the silo grows.'
         'BILL OF MATERIALS:'
         '- Tungstensteel Turbine Casing: T1 96x / T2 88x / T3 80x / T4 72x / T5 64x'
         '- 4x Turbine Housing in the bore sockets nearest the silo (the projector blanks them with plain casings - swap in real housings, then right-click each to set CW or ACW)'
       ) -join "`n") }
    @{ Key='bpspiral'; Name='Blueprint: Spiral'; X=192; Y=120; Icon=(Icon 'gregtech:gt.blockmachines' 28106 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'The 7x7x12 injection chamber at the duct''s end. Same bill at every tier:'
         '- 327x Tungstensteel Turbine Casing'
         '- 50x Light Concrete (the internal helix arcs)'
         '- 8x Precision Injector (one per interior level, following the helix down)'
         '- 1x Spine Sensor (roof, front-center)'
         '- 1x Input Hatch + 1x Maintenance Hatch (bottom wall, front corners)'
       ) -join "`n") }
    @{ Key='bpturbinegen'; Name='Blueprint: Turbine Generator'; X=240; Y=120; Icon=(Icon 'beyondthestars:turbineHousing' 0 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'The rotor gallery (7x5x15) and its output manifold (7x7x3) down the platform''s east strip. Same bill at every tier:'
         '- 416x Tungstensteel Turbine Casing (313 gallery + 103 manifold)'
         '- 8x Turbine Housing (the rotor stage sockets along the centerline)'
         '- 8x Turbine Rotor (wear parts: -5% stage output per damage point, shatters at 10)'
         '- 4x Dynamo Hatch (manifold back face)'
         '- 1x Turbine Control Block + 1x Maintenance Hatch (gallery front wall)'
         'Feed the Turbine Control Block a redstone clock or the RPM decays to nothing.'
       ) -join "`n") }
    @{ Key='bptower'; Name='Blueprint: Cooling Tower'; X=288; Y=120; Icon=(Icon 'gregtech:gt.blockmachines' 28105 1); Task='checkbox'; Prereqs=@(); CountAs=0
       Desc=(@(
         'One 17x17x20 hyperboloid shell, per tower:'
         '- 736x Light Concrete (the flare)'
         '- 51x Solid Steel Machine Casing (base ring)'
         '- 1x Maintenance Hatch (in the base ring)'
         '- 1x Plant Module - Cooling Tower controller on the dock marker, FACING AWAY from the tower'
         'The canonical campus runs TWO (NW and SW corners) - double everything. Towers halve shutdown cooling-water demand and speed passive decay.'
       ) -join "`n") }
)

# ---------------------------------------------------------------- generation
$lineLow = SafeLow 47990
$lineB64 = B64Id $HIGH $lineLow
$lineDir = "BeyondTheStars-$lineB64"

$low = 48000
$byKey = @{}
foreach ($q in $quests) {
    $low = SafeLow $low
    $q.Low = $low
    $q.B64 = B64Id $HIGH $low
    $byKey[$q.Key] = $q
    $low++
}

if (Test-Path $out) { Remove-Item -Recurse -Force $out -Confirm:$false }

# QuestLine.json
WriteJson (Join-Path $out "QuestLines\$lineDir\QuestLine.json") ([ordered]@{
    'properties:10' = [ordered]@{
        'betterquesting:10' = [ordered]@{
            'bg_image:8' = ''
            'bg_size:3' = 256
            'desc:8' = 'GTNH: Beyond the Stars - the Unit One nuclear campus. A 50x50 foundation, a cascade-fed reactor silo, a chirality-tuned turbine spine, an eight-injector spiral chamber, and the rotor assembly at the end of the pipe.'
            'icon:10' = (Icon 'gregtech:gt.blockmachines' 28100 1)
            'name:8' = 'Beyond the Stars'
            'visibility:8' = 'NORMAL'
        }
    }
    'questLineIDHigh:4' = $HIGH
    'questLineIDLow:4' = $lineLow
})

function PlacementJson($q) {
    [ordered]@{
        'questIDHigh:4' = $HIGH
        'questIDLow:4' = $q.Low
        'sizeX:3' = 24
        'sizeY:3' = 24
        'x:3' = $q.X
        'y:3' = $q.Y
    }
}

function QuestJson($q) {
    # prerequisites: in-line keys plus raw (high, low) pairs from outside this line
    $pre = [ordered]@{}
    $pi = 0
    foreach ($pk in $q.Prereqs) {
        $pre["${pi}:10"] = [ordered]@{ 'questIDHigh:4' = $HIGH; 'questIDLow:4' = $byKey[$pk].Low }
        $pi++
    }
    if ($q.RawPrereqs) {
        foreach ($rp in $q.RawPrereqs) {
            $pre["${pi}:10"] = [ordered]@{ 'questIDHigh:4' = [long]$rp.High; 'questIDLow:4' = [long]$rp.Low }
            $pi++
        }
    }

    # task: checkbox, one retrieval item, or several (never consumed)
    if ($q.Task -is [string]) {
        $task = [ordered]@{ 'index:3' = 0; 'taskID:8' = 'bq_standard:checkbox' }
    } else {
        $items = [ordered]@{}
        $ii = 0
        foreach ($it in @($q.Task)) {
            $items["${ii}:10"] = $it
            $ii++
        }
        $task = [ordered]@{
            'autoConsume:1' = 0; 'consume:1' = 0; 'groupDetect:1' = 0; 'ignoreNBT:1' = 0
            'index:3' = 0; 'partialMatch:1' = 1; 'requireOnlyOneItem:1' = 0
            'requiredItems:9' = $items
            'taskID:8' = 'bq_standard:retrieval'
        }
    }

    $countAs = if ($q.ContainsKey('CountAs')) { $q.CountAs } else { 1 }

    [ordered]@{
        'preRequisites:9' = $pre
        'properties:10' = [ordered]@{
            'betterquesting:10' = [ordered]@{
                'autoClaim:1' = 0
                'completion_animation:8' = 'default'
                'completion_particle:8' = 'default'
                'confetti_icon:10' = (Icon 'minecraft:stick' 0 0)
                'countAsQuest:1' = $countAs
                'desc:8' = $q.Desc
                'globalShare:1' = 1
                'icon:10' = $q.Icon
                'isGlobal:1' = 0
                'isMain:1' = 0
                'isSilent:1' = 0
                'lockedProgress:1' = 0
                'name:8' = $q.Name
                'particle_count:3' = -1
                'partySingleReward:1' = 0
                'questLogic:8' = 'AND'
                'repeatTime:3' = -1
                'repeat_relative:1' = 1
                'simultaneous:1' = 0
                'snd_complete:8' = 'random.levelup'
                'snd_update:8' = 'random.levelup'
                'taskLogic:8' = 'AND'
                'visibility:8' = 'NORMAL'
            }
        }
        'questIDHigh:4' = $HIGH
        'questIDLow:4' = $q.Low
        'rewards:9' = [ordered]@{}
        'tasks:9' = [ordered]@{ '0:10' = $task }
    }
}

foreach ($q in $quests) {
    $prefix = NamePrefix $q.Name
    WriteJson (Join-Path $out "QuestLines\$lineDir\$prefix-$($q.B64).json") (PlacementJson $q)
    WriteJson (Join-Path $out "Quests\$lineDir\$prefix-$($q.B64).json") (QuestJson $q)
}

# --------------------------------------- pack-line entry quest (TC-5 pt 5)
# "How to Generate Power" is GTNH's power-progression chapter - its own description
# says every way to generate EU lives on that tab. We only ADD two files to its
# folders (placement + quest), in our high-190 ID namespace, so no pack file is ever
# modified and a pack update can never collide. Prerequisite: "Standard Nuclear
# Fuel: Uranium" (ID 0/191), the chapter's mid-game entry into nuclear power - the
# same rods Unit One's cascade burns. Placement (540,324) is a free cell just below
# the chapter's nuclear cluster (uranium fuel sits at 504,216).
$packLineDir = 'HowtoGeneratePow-AAAAAAAAAAAAAAAAAAAACQ=='
if ($low -gt 48020) { throw "Quest lows overran the reserved pack-entry low 48020 - move it." }
$packQuest = @{
    Key = 'packentry'; Name = 'Unit One'; X = 540; Y = 324
    Icon = $ICON_CTRL; Task = $ICON_CTRL; Prereqs = @()
    RawPrereqs = @(@{ High = 0; Low = 191 })  # Standard Nuclear Fuel: Uranium
    Desc = (@(
        'A whole nuclear campus from ONE hologram projection: a 50x50 platform, a cascade-fed reactor silo, a chirality-tuned turbine spine, an eight-injector spiral chamber, and a rotor gallery at the end of the pipe. Rods fall, swirl accumulates, injectors fire on a +/-2 tick window - and a fully optimized Tier 5 plant pushes toward a billion EU/t.'
        'Craft the NUCLEAR PLANT FOUNDATION controller to begin. The full campaign - build order, spin doctrine, injection timing, and per-building blueprints - lives in its own chapter: BEYOND THE STARS.'
    ) -join "`n")
}
$packQuest.Low = SafeLow 48020
$packQuest.B64 = B64Id $HIGH $packQuest.Low
$packPrefix = NamePrefix $packQuest.Name
WriteJson (Join-Path $out "QuestLines\$packLineDir\$packPrefix-$($packQuest.B64).json") (PlacementJson $packQuest)
WriteJson (Join-Path $out "Quests\$packLineDir\$packPrefix-$($packQuest.B64).json") (QuestJson $packQuest)

# order line to append to the pack's QuestLinesOrder.txt
WriteText (Join-Path $out 'OrderLine.txt') "${lineB64}: Beyond the Stars`n"

Write-Host "Generated quest line $lineDir with $($quests.Count) quests (IDs $HIGH/$($quests[0].Low)..$($quests[-1].Low))"
Write-Host "Plus pack-line entry quest 'Unit One' ($HIGH/$($packQuest.Low)) in $packLineDir"
