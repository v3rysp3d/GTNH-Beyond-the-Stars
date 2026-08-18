# Generates the "Beyond the Stars" BetterQuesting quest-line overlay in GTNH 2.9.0's
# split DefaultQuests format (QuestLines/<line>/ placements + QuestLine.json, and
# Quests/<line>/<quest>.json). Run from this folder:  powershell -File gen-questbook.ps1
# Output goes to .\DefaultQuests\ ; merge per README.md.
#
# IDs use questIDHigh = 190 (non-zero) so they can never collide with pack quests
# (which all use high 0). HIGH must stay <= 255: its byte then sits alone in a base64
# triple whose characters are provably never '+' or '/', so filenames stay legal.
# Lows are auto-advanced past any value whose base64 would contain '+' or '/'.

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
# keys: Key (unique), Name, Desc, Icon, Prereqs (keys), Task ('checkbox' or retrieval
# icon hashtable), X, Y
$ICON_CTRL = Icon 'gregtech:gt.blockmachines' 28100 1
$quests = @(
    @{ Key='groundwork'; Name='Groundwork'; X=0; Y=0; Icon=$ICON_CTRL; Task=$ICON_CTRL; Prereqs=@()
       Desc='Every great plant starts with concrete. The Nuclear Plant Foundation is the MAIN STATION: project it and the full 50x50 platform builds around it - a Tungstensteel-Reinforced shield pad at center (your controller stands on its front line, and every reactor tier''s front wall will grow around that exact spot) and eight flush steel markers set into the floor. Those are DOCK POSITIONS: keep them clear until a module controller stands there. The bare platform forms and idles - everything else in this chapter is added ONTO it.' }
    @{ Key='silo'; Name='Raising the Silo'; X=48; Y=0; Icon=(Icon 'beyondthestars:reactorFuelColumn' 0 1); Task=(Icon 'beyondthestars:reactorFuelColumn' 0 8); Prereqs=@('groundwork')
       Desc='The STEAM GENERATOR is the first addon: set Build Tier 1-5 in the main station''s GUI (or hold a 2-5 trigger stack) and project the silo onto the shield pad. Nine to seventeen blocks wide, ribbed walls twice the diameter tall, radiation bands, a hemispherical shield dome - and your controller embedded in its face at ground level. Hatches go in the silo walls; fuel columns stack EIGHT segments high inside. This machine makes a specific REACTOR STEAM - everything downstream is built to spend it well.' }
    @{ Key='firststeam'; Name='First Steam'; X=96; Y=0; Icon=(Icon 'minecraft:water_bucket' 0 1); Task='checkbox'; Prereqs=@('silo')
       Desc='Feed water, load fuel, set the control rods (1-24 in the GUI; a programmed circuit overrides). With no downstream machinery the plant simply exports Reactor Steam - or Hot Coolant if you feed it IC2 Coolant. It works, it is honest, and it is a fraction of what this plant can do. The real numbers live at the end of a much longer pipe.' }
    @{ Key='cascade'; Name='The Decay Cascade'; X=48; Y=48; Icon=(Icon 'beyondthestars:fuelRodUranium' 0 1); Task=(Icon 'beyondthestars:fuelRodUranium' 0 8); Prereqs=@('silo')
       Desc='Real reactors do not eat neat assemblies from a bus - they run RODS. Each fuel pillar is a working 8-segment gravity column: insert a rod at the TOP and it falls to the lowest empty segment; only the BOTTOM rod burns, one decay cycle at a time; the spent rod drops into the base buffer for extraction and the whole stack shifts down. Your job is the automation: keep up to 201 pillars topped up and their bases drained. Classic assemblies in the input bus still work - at HALF output. The cascade is where the other half lives.' }
    @{ Key='spine'; Name='The Spine'; X=144; Y=0; Icon=(Icon 'gregtech:gt.blockcasings' 12 1); Task='checkbox'; Prereqs=@('firststeam')
       Desc='Steam leaves the silo through a custom 3x3 duct with logic of its own, and along it sit FOUR turbine housings. Each rotor is set CLOCKWISE or ANTICLOCKWISE with a wrench - and the order matters, because backwash swirl accumulates from housing to housing. A rotor that spins WITH the incoming swirl rides it for more steam speed (and more heat); a rotor set AGAINST it cancels the swirl for more EU per litre. Same-direction trains are fast and thirsty, alternating trains are efficient and cool. There is no single right answer - only your goal.' }
    @{ Key='spindoctrine'; Name='Spin Doctrine'; X=144; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 32304 1); Task='checkbox'; Prereqs=@('spine')
       Desc='The math, plainly: swirl S starts at 0 and each housing adds +1 (CW) or -1 (CCW). Riding rotors multiply steam speed by 1 + 0.15x|S|; opposing rotors multiply efficiency by 1 + 0.10x|S|; whatever swirl survives the fourth housing enters the spiral chamber as extra heat. CW-CW-CW-CW is a dragster; CW-CCW-CW-CCW is an economist. Retune the train when your goals - or your propellants - change.' }
    @{ Key='spiral'; Name='The Spiral'; X=192; Y=0; Icon=(Icon 'gregtech:gt.blockmachines' 28104 1); Task='checkbox'; Prereqs=@('spine')
       Desc='The 7x7 injection chamber spirals the steam downward past EIGHT precision injectors. Steam moves in slugs - one enters every second, and the SENSOR HATCH fires a redstone pulse the tick it does. Each injector wants EXACTLY 1L of propellant inside a window of +/-2 ticks around that pulse. Hit the window and the slug accelerates. Miss it - or double-inject - and the litre is wasted AND the chamber takes a heat spike. Sloppy timing does not just cost you boost; it actively pushes the machine toward seizure. Build the timing circuit. Then trust it.' }
    @{ Key='octuple'; Name='Octuple Injection'; X=192; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 30671 1); Task='checkbox'; Prereqs=@('spiral')
       Desc='Eight injectors, one ladder. Worst to best: Benzene, High Octane Gasoline, H8N4C2O4 Rocket Fuel, then Naquadah Fuel Lv 1 through Lv 6. Every well-timed litre adds its share of the propellant''s stage factor - all eight timed gives the full multiplier, from x1.25 on Benzene to x5 at Lv 3. Past Lv 3 the gains go LINEAR (+0.5 per level to x6.5 at Lv 6, UMV) - the plant will drink it, but the sane stop is Lv 3 unless you are chasing a number, not a ratio.' }
    @{ Key='coldblood'; Name='Cold Blood'; X=192; Y=96; Icon=(Icon 'ic2:itemFluidCell' 0 1); Task='checkbox'; Prereqs=@('spiral')
       Desc='Speed squared is heat. Coolant drain per tick scales with the SQUARE of steam velocity, divided by coolant quality - and if the chamber starves, the heat climbs until the rotor assembly SEIZES: rotors destroyed, plant down. The ladder, worst to best: Water, Distilled Water, IC2 Coolant, Super Coolant, Liquid Cryotheum, Tachyon Rich Temporal Fluid. Every propellant tier you climb, your coolant logistics must climb with it. The plant does not forgive a cooling loop designed for last month''s speed.' }
    @{ Key='assembly'; Name='The Assembly'; X=240; Y=48; Icon=(Icon 'gregtech:gt.blockmachines' 28100 1); Task='checkbox'; Prereqs=@('spindoctrine','octuple','coldblood')
       Desc='The end of the pipe: the LARGE TURBINE ROTOR ASSEMBLY. The spiral chamber''s downcomer feeds a rotor gallery that converts the final flow - litres times velocity times train efficiency - into EU through its dynamos. Rod cascade fed, spine tuned, eight injectors timed, coolant loop scaled: this is where the insane numbers happen. (The old Turbine Hall retires here - its fans and multipliers live on inside this chain.)' }
    @{ Key='writteninfire'; Name='Written in Fire'; X=96; Y=48; Icon=(Icon 'gregtech:gt.metaitem.01' 32605 1); Task='checkbox'; Prereqs=@('silo')
       Desc='Read this before it reads you. Core heat SCRAMS the plant at 8,000 and MELTS DOWN at 10,000 - a vertical detonation up the silo stack and a 30-minute radiation field centered on the wreck (hazmat counters it; config can disable it). Every shutdown leaves DECAY HEAT: twenty minutes of mandatory cooling water, deferred - never waived - if you dismantle the silo. Cooling towers halve the demand and speed passive decay. The plant is generous with power and unforgiving with physics.' }
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

foreach ($q in $quests) {
    $prefix = NamePrefix $q.Name
    # placement entry
    WriteJson (Join-Path $out "QuestLines\$lineDir\$prefix-$($q.B64).json") ([ordered]@{
        'questIDHigh:4' = $HIGH
        'questIDLow:4' = $q.Low
        'sizeX:3' = 24
        'sizeY:3' = 24
        'x:3' = $q.X
        'y:3' = $q.Y
    })

    # prerequisites
    $pre = [ordered]@{}
    $pi = 0
    foreach ($pk in $q.Prereqs) {
        $pre["${pi}:10"] = [ordered]@{ 'questIDHigh:4' = $HIGH; 'questIDLow:4' = $byKey[$pk].Low }
        $pi++
    }

    # task
    if ($q.Task -is [string]) {
        $task = [ordered]@{ 'index:3' = 0; 'taskID:8' = 'bq_standard:checkbox' }
    } else {
        $task = [ordered]@{
            'autoConsume:1' = 0; 'consume:1' = 0; 'groupDetect:1' = 0; 'ignoreNBT:1' = 0
            'index:3' = 0; 'partialMatch:1' = 1; 'requireOnlyOneItem:1' = 0
            'requiredItems:9' = [ordered]@{ '0:10' = $q.Task }
            'taskID:8' = 'bq_standard:retrieval'
        }
    }

    WriteJson (Join-Path $out "Quests\$lineDir\$prefix-$($q.B64).json") ([ordered]@{
        'preRequisites:9' = $pre
        'properties:10' = [ordered]@{
            'betterquesting:10' = [ordered]@{
                'autoClaim:1' = 0
                'completion_animation:8' = 'default'
                'completion_particle:8' = 'default'
                'confetti_icon:10' = (Icon 'minecraft:stick' 0 0)
                'countAsQuest:1' = 1
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
    })
}

# order line to append to the pack's QuestLinesOrder.txt
WriteText (Join-Path $out 'OrderLine.txt') "${lineB64}: Beyond the Stars`n"

Write-Host "Generated quest line $lineDir with $($quests.Count) quests (IDs $HIGH/$($quests[0].Low)..$($quests[-1].Low))"
