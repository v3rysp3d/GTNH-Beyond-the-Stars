# Tooltip style

How Beyond the Stars writes tooltips, taken from what GregTech 5.09.54.205 (GTNH 2.9.0-RC-2) does in its own
machines, hatches and casings. The helpers live in `machines/gui/BTSTip.java`; use them instead of raw `§` codes so
every tooltip gets the same colours.

## What GregTech does

Sources read: `gregtech.api.util.MultiblockTooltipBuilder`, `gregtech.api.util.tooltip.TooltipHelper`,
`TooltipTier`, `GTValues.TIER_COLORS`, the markdown tooltips in `assets/gregtech/lang/en_US/tooltip/*.md`
(`eye-of-harmony.md`, `forge-of-gods.md`, `godforge-plasma-module.md`), the `createTooltip()` of
`MTEBlackHoleCompressor`, `MTELargeHadronCollider`, `MTENanochipAssemblyComplex`, `MTEQuantumForceTransformer`,
`MTEEyeOfHarmony` and the Forge of the Gods modules, and the hatch and casing lines in GT's `en_US.lang`.

### Shape of a multiblock tooltip

`MultiblockTooltipBuilder` gives every multi the same frame:

1. `Machine Type: §eCompressor, Advanced Neutronium Compressor, BHC` (yellow, from `addMachineType`).
2. One to three plain lines saying what it does. The Black Hole Compressor opens with three light-purple lines:
   *Uses the immense power of the event horizon to compress things*.
3. Groups of short lines, one idea each, split by `addSeparator()` (a grey rule, or a blank line, per the player's
   GT config). The BHC has four groups: open/stability, decay and spacetime, closing, numbers.
4. The standard bonus lines from the builder: `addBulkMachineInfo(8, 5.0F, 0.7F)` prints
   `§68§7 Parallels per §fVoltage§7 Tier`, `§a500%§7 Speed`, `§b70%§7 EU Usage`; `addSupportAny()` prints
   `§aSupports Multi-Amp and Laser Hatches!`.
5. Limits and warnings in red: `§cRecipe tier is limited to hatch tier + 1`.
6. `toolTipFinisher(...)`: a rule, `Hold §l[LSHIFT]§r§7 to display structure guidelines`, and
   `Added by: §aOllie§7 & ...`.

Shift replaces the info with the structure section: `Dimensions: §635x33x35§7 (LxWxH) §c(Hollow)`, `Structure:`, the
controller, casing counts (`§63667-3671 §fExtreme Density Space-Bending Casing`), and hatch lines with a yellow count,
a white name and grey placement: `§e0+ §fBlack Hole Utility Hatch§7: Any absorbent casing`. An optional hatch is
`0+`; a required one `1` or `1+`.

### Colours

`TooltipHelper` fixes the meaning of each colour, and the big multis follow it:

| What | Colour | GT source |
| --- | --- | --- |
| Body text | grey `§7` | every `addInfo` line |
| Things you handle (items, blocks, machines) | white `§f` | BHC: `§fBlack Hole Seed§7`, LHC: `§fBooster Fluids§7` |
| EU, EU/t, voltage numbers | yellow `§e` | `EU_VOLT_COLOR` |
| Voltage tier names | the GT tier colour | `voltageTierText`: `TIER_COLORS[i] + VN[i]` (UHV dark red underlined, UEV dark purple underlined, UIV dark blue bold underlined, UMV red bold underlined, UXV dark red bold underlined, MAX white bold underlined) |
| Amperes | aqua `§b` | `AMP_COLOR` |
| Parallels | gold `§6` | `PARALLEL_COLOR` |
| Speed | green `§a` | `SPEED_COLOR` |
| Efficiency | aqua `§b` | `EFF_COLOR` |
| Fluid amounts, tiers | white `§f` | `L_COLOR`, `TIER_COLOR` |
| Capacity | blue `§9` | `CAPACITY_COLOR` |
| Warnings | red `§c` | BHC: `§cUNSTABLE`, `§cRecipe tier is limited ...` |
| Danger, irreversible | dark red `§4` | BHC: `§4... void recipes ...` |
| Flavour | light purple `§d` or italic | BHC's first three lines, NAC's GREGOS lines |
| Sub-headings | white underlined `§f§n` | LHC: `§f§nAccelerator mode§7` |

A coloured part always returns to grey (`TooltipHelper.coloredText` appends `§7`).

### Lines and spacing

- Short lines: GT's longest info lines are about 70 characters; most are 40-60. Long explanations wrap onto a new
  line that stands on its own, never a hanging indent.
- One idea per line. The BHC states stability start, decay rate, the unstable state and its effect on four lines.
- Groups separated by `addSeparator()`, not blank `addInfo("")` lines.
- Numbers sit in the sentence, coloured, never in a parenthesised formula unless the formula is the point (the EOH
  markdown puts formulas on their own lines: `§fParallels =§7 §a2^(PE)`).

### Hatches and casings

- A hatch opens with a short noun phrase: `Energy Injector for Multiblocks`, `Fluid Input for Multiblocks`,
  `Optional hatch for Pseudostable Black Hole Containment Field.` Then a few facts (`Accepts up to 2 Amps`,
  `Capacity: %s L`) and the controls (`Right click to open the GUI and change settings.`). Red for a hazard
  (`§cDO NOT OBSTRUCT THE INPUT!`).
- Detail behind a key: `Hold Shift for more info` (`GT5U.armor.tooltip.hold_shift`), GT++'s
  `Hold Ctrl to show additional info.`
- A casing has one or two lines: a fact (`This Block is Blast Proof`, `Casing Tier: §e%s`) or one sentence of what
  it does (`Controls the outward flow of Hawking Radiation to stabilize a black hole.`).

## Our rules

1. **Lead with one plain sentence** of what it does. Machine type stays first (the builder puts it there).
2. **Key numbers coloured, never buried**: `BTSTip.euT`, `BTSTip.amps(1, BTSTip.UEV)`, `BTSTip.num`, `BTSTip.parallel`,
   `BTSTip.speed`. A Bu/t figure always carries its EU/t: `BTSTip.bu(0.1)` gives `0.1 Bu/t (1.0e12 EU/t)`.
3. **One idea per line, about 60-70 characters at most.** No hanging-indent continuations.
4. **Sections** split by `addSeparator()`. A machine's info normally has three: what it does and how to start it;
   the key numbers; automation and warnings.
5. **Deep mechanics go elsewhere.** Formulas, per-tier tables and every redstone bit live in the GUI's info page and
   the questbook. The controller tooltip keeps the numbers a player needs before building or feeding it. A hatch
   keeps its full reference behind `[LSHIFT]` (`BTSTip.withDetails`).
6. **Structure section in GT's words.** `addCasingInfoExactly` for blocks; hatches with GT's yellow count and `0+` for
   optional ones. Optional sockets never read as required.
7. **Consistent naming.** Title Case for item, block and machine names exactly as in game (`GRB Receptor Port`,
   `Directive Hatch`); "Kerr T1", "Eye T3"; no internal ids (no MTE numbers, no `SL-xx` recipe ids, no socket letters
   such as `(u)` in player text).
8. **Numbers match the code.** Every number in a tooltip is read from the constant the machine uses (or checked
   against it by a grep), never typed from memory.
9. **Sign-off** with `toolTipFinisher(BeyondTheStars.TOOLTIP_AUTHOR)`: GT's `Hold [LSHIFT]` line and
   `Added by: Beyond the Stars`.

## Examples

Kerr Ergosphere Engine, first lines (normal view):

```
Machine Type: §eBlack Hole Containment, Kerr Ergosphere Engine
Holds a black hole for the Kerr modules built into its arcs.
Runs no recipes itself. Everything else is in the GUI.
----------------------------------------
Open: a §fBlack Hole Seed§7 in a bolt bus (the hole starts at §f8 SM§7).
Close: a §fBlack Hole Collapser§7, or let it evaporate below §f2 SM§7.
```

A hatch (normal view):

```
Tells automation what its machine needs, as a 4-bit redstone code.
Fits the Eye of Harmony, the Kerr Ergosphere Engine and the Quasar Engine.
Right-click: pick the set and the output.
Hold [LSHIFT] for details
```

A casing:

```
Part of the Kerr Ergosphere Engine's cage.
```
