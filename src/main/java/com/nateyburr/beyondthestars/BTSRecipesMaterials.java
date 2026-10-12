package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.items.BTSItemsFuel;
import com.nateyburr.beyondthestars.machines.MTESiteBatchingPlant;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gtPlusPlus.core.material.MaterialsAlloy;

/**
 * D-26 THE MATERIAL LINES - bulk casing manufacture on the Site Batching Plant
 * (docs/production-lines.md S3). <b>Owned exclusively by Agent B (Materials).</b>
 * <p>
 * <b>These recipes sit BESIDE GT's own casing recipes and never replace them.</b> Every output is
 * a stock block; every stock recipe is left standing. What this file sells is throughput - 8 to 32
 * casings a batch, times the plant's 4/8/12 parallel - and it buys that with a multiblock, a
 * higher voltage and, in most steps, a slightly HIGHER material cost than the stock recipe (the
 * shielding gas and the braze filler a real production line consumes and a bench assembler does
 * not). A player who never builds the Batching Plant loses nothing but time.
 *
 * <h2>The bill this has to serve</h2> Read off {@link BTSBom#TOTAL}[5] - the generated file, not a
 * guess. A whole tier-5 plant is <b>8,440 stock blocks</b>, plus 1,608 Reactor Fuel Pillar Frames and
 * 201 Rod Loading Ports which are BTS blocks and belong to {@link BTSRecipesMachines}:
 *
 * <pre>
 *   Light Concrete                     3,773      M-01   EV
 *   Radiant Naquadah Alloy Casing      1,667      M-05   UV / UHV
 *   Tungstensteel Turbine Casing         798      M-06   EV
 *   Solid Steel Machine Casing           509      M-02a  EV
 *   Iridium Reinforced Block             497      M-03c  IV
 *   Clean Stainless Steel Casing         452      M-02b  EV
 *   Tungstensteel Reinforced Block       249      M-03b  IV
 *   Steel Frame Box                      170      - stock only, see below
 *   Filter Machine Casing                152      M-02c  EV
 *   Radiation Proof Machine Casing       141      M-03a  IV
 *   Steel Pipe Casing                     32      - stock only, see below
 *   ---------------------------------------
 *   Europium Reinforced Rad. Proof     1,343      M-04   LuV   (tier-4 peak; tier 5 uses none)
 * </pre>
 *
 * <b>Two blocks deliberately have no bulk route.</b> Steel Frame Box (170) and Steel Pipe Casing
 * (32) are a three-figure and a two-figure bill that one Assembler clears in under a minute.
 * Adding a batch recipe for them would have meant inventing a frame ratio next to GT's, which is a
 * discount dressed as a boost.
 *
 * <h2>Throughput, against those bills</h2> Worst case on purpose: <b>one Batching Plant, the
 * cheapest Solid Steel shell (parallel 4), and an energy hatch exactly at the recipe's own tier so
 * nothing overclocks.</b> {@code cycles = ceil(batches / 4)}:
 *
 * <pre>
 *   M-01a  cement          30 batches   8 cycles x 16s =  128s
 *   M-01b  Light Concrete 118 batches  30 cycles x  8s =  240s
 *   M-02a  Solid Steel     64 batches  16 cycles x 12s =  192s
 *   M-02b  Clean Stainless 57 batches  15 cycles x 12s =  180s
 *   M-02c  Filter          19 batches   5 cycles x 12s =   60s
 *   M-03a  Radiation Proof 18 batches   5 cycles x 16s =   80s
 *   M-03b  TS Reinforced   32 batches   8 cycles x 16s =  128s
 *   M-03c  Ir Reinforced   63 batches  16 cycles x 16s =  256s
 *   M-05b  Radiant Naq.    53 batches  14 cycles x  8s =  112s
 *   M-06   TS Turbine     100 batches  25 cycles x 12s =  300s
 *                                              TOTAL   1,676s = 27.9 minutes
 * </pre>
 *
 * Twenty-eight minutes of one machine for the entire tier-5 buildout, at the worst shell and with
 * no overclock. Re-walled in Tungstensteel Reinforced (parallel 12) it is <b>608s = 10.1
 * minutes</b>. That is the difference between a line and a wall, and it is the whole reason MTE
 * 28112 exists.
 * <p>
 * <b>The table is self-consistent about power, which is the part that is easy to get wrong.</b>
 * The plant has ONE energy hatch cell, and GT caps parallel by available EU/t before the shell tier
 * ever gets a say - so "parallel 4, no overclock" is not two independent assumptions, it is one
 * hatch two tiers above the recipe. An EV recipe at parallel 4 draws 7,680 EU/t; an IV hatch
 * supplies 8,192 x 2A = 16,384, which covers it and cannot afford the 4x an overclock would cost.
 * Parallel 12 on the same recipe draws 23,040 and wants a LuV hatch. The two top rungs are the
 * ones to watch: <b>M-05b at UHV draws 1,966,080 EU/t a parallel, so one UHV hatch (2,097,152 x
 * 2A) reaches parallel 2 and no further</b> - parallel 4 there means a UEV hatch. Nothing about
 * that is unusual for GT, but it is why the table is quoted at parallel 4 and not at 12.
 *
 * <h2>The tier-3 and tier-4 shell, which the tier-5 table does not show</h2> A tier-5 silo uses no
 * Europium Reinforced casings at all - Radiant Naquadah replaces them - so M-04 is missing from
 * the table above even though it is the third-largest bill in the game. Its own arithmetic:
 *
 * <pre>
 *   tier 3    963 casings  = 121 batches  ->  31 cycles x 24s =  744s = 12.4 min
 *   tier 4  1,343 casings  = 168 batches  ->  42 cycles x 24s = 1,008s = 16.8 min
 * </pre>
 *
 * The longest single line in the file, and it is a LuV recipe, so parallel 4 there wants a UV
 * hatch.
 *
 * <h2>What actually gates each line</h2> Machine time is NOT the binding constraint for the two
 * most expensive bills - materials are, and the arithmetic says so plainly:
 *
 * <pre>
 *   Radiant Naquadah  1,667 casings -> 424 frames + 1,696 screws Prismatic Naquadah
 *                                      + 732,672 L molten Naquadah Alloy (5,088 ingots)
 *   Iridium Reinforced  497 blocks  -> 504 frames Iridium (1,008 ingots)
 *                                      +  72,576 L molten Iridium (504 ingots)
 * </pre>
 *
 * Both figures are EXACTLY what the stock recipes cost. The Batching Plant does not make naquadah
 * or iridium cheaper; it removes the five thousand plate-rolling and eight thousand assembler
 * operations between having the metal and having the wall.
 *
 * <h2>Where material cost deliberately does NOT match stock</h2>
 * <ol>
 * <li><b>M-01, concrete.</b> Real concrete is about one part binder to seven parts aggregate, and
 * the stock Fluid Solidifier recipe (144 L molten Concrete -&gt; 1 block) prices it as if it were
 * all binder. M-01b spends 2 Concrete Dust, 6 Sand, 8 Gravel and 2,000 L Water on 32 Light
 * Concrete - a 12.5 % binder fraction, which is the real number. This IS a discount in Concrete
 * Dust, and it is defensible only because Concrete Dust is stone dust: the platform's real cost
 * was never the material, it was placing 3,773 blocks one solidifier run at a time.</li>
 * <li><b>M-03b, the interlock.</b> One {@link BTSItemsFuel#depletedUraniumIngot} per Tungstensteel
 * Reinforced Block, on top of the stock frame and concrete. Depleted uranium is real ballast and
 * real shielding, the enrichment tails from F-11 have to go somewhere, and this makes the fuel
 * line feed the casing line. The stock Chemical Bath route stays available for anyone who has not
 * started enriching, so this can never soft-lock a build.</li>
 * <li><b>M-05b, the UHV step.</b> Casting the naquadah-alloy skin straight onto the space frame
 * needs no braze filler, so 324 L of Indalloy 140 per casing stops being spent - 540,108 L across
 * a tier-5 silo. What replaces it is a UHV energy hatch. M-05a keeps the stock recipe's Indalloy
 * exactly, at UV, for everybody who is not there yet.</li>
 * </ol>
 *
 * <h2>Two constraints the machine imposes, and how the recipes respect them</h2>
 * <ol>
 * <li><b>No output hatch.</b> {@link MTESiteBatchingPlant} sockets a Maintenance, an Energy, two
 * Input Buses, one Input Hatch and one Output Bus - and no output hatch at all. <b>Every recipe
 * here therefore has item-only outputs.</b> That is why M-01a cannot vent the CO2 that real
 * clinker calcination drives off, and why the concrete line ends at cast blocks rather than at a
 * tank of ready-mix.</li>
 * <li><b>One input hatch.</b> One hatch holds one fluid, so <b>no recipe here takes more than one
 * fluid input</b>, even though the map would allow two. That is what reshaped M-04 (europium
 * arrives as a melt, lead as plates) and M-05b (naquadah alloy as a melt, prismatic naquadah as
 * items) rather than a lazy pair of fluids that would have needed a quad hatch nobody was told to
 * build.</li>
 * </ol>
 *
 * <h2>Deviation from the design sketch</h2> S3 listed <b>M-02d as a Forming Press recipe on GT's
 * own map</b> - the "one exception". It is not here. Pressing implies a pleat pack or a blank, and
 * that means a new item; {@link com.nateyburr.beyondthestars.items.BTSItemsMaterials} registers
 * nothing by decision, and a token press step turning a stock block into a stock block would have
 * been an exception kept for its own sake. The Filter Machine Casing is made by M-02c on the
 * Batching Plant with the rest of the sheet-metal line. <b>Everything in this file goes to
 * {@link MTESiteBatchingPlant#RECIPE_MAP}.</b>
 */
public final class BTSRecipesMaterials {

    private BTSRecipesMaterials() {}

    /** Casings a sheet-metal or shielding batch yields. Eight, as S3 specifies. */
    private static final int CASINGS = 8;

    /** Blocks a pour yields. Thirty-two, as S3 specifies: 118 batches for a tier-5 platform. */
    private static final int POUR = 32;

    public static void register() {
        concrete();
        sheetMetal();
        shielding();
        europiumReinforced();
        radiantNaquadah();
        turbineShell();
        logBillCoverage();
    }

    // ------------------------------------------------------------------ M-01
    /**
     * <b>M-01 CONCRETE - EV.</b> Serves Light Concrete, 3,773 of them, the single largest line item
     * in the whole build and the reason a batching plant is the FIRST thing a player raises.
     * <p>
     * Real process, in the real order. A cement works burns a raw meal of limestone and clay at
     * about 1,450 C; the calcination drives CO2 off the limestone and leaves clinker, which is
     * inter-ground with a few percent gypsum to stop the cement flash-setting. The batching plant
     * next door then proportions cement, fine aggregate, coarse aggregate and water, and discharges
     * ready-mix into slipforms.
     */
    private static void concrete() {
        // M-01a Rotary kiln - clinker burning. Nine units of raw meal in, eight of cement out;
        // the missing unit is the CO2 calcination drives off, which cannot be emitted here
        // because the Batching Plant has no output hatch (see the class comment).
        //
        // This is a FEEDSTOCK route, not a cheaper one: it lets a concrete line run off the
        // carbonate rock a quarry is already producing instead of competing for stone dust with
        // everything else that wants it. GT's own route to Concrete Dust is untouched.
        //
        // THE CARBONATE IS CALCITE, NOT LIMESTONE, AND IT HAD TO BE. dust Materials.Limestone is
        // the SAME BUG that stopped 0.24.0 booting at F-11c: Limestone is absent from
        // MaterialsIDMap, so its mMetaItemSubID stays -1 and no meta-item is ever generated for
        // it - loadLimestone() is character for character the shape of loadFluorite(), setName,
        // setDefaultLocalName, addDustItems, constructMaterial and no ID. Nothing anywhere in the
        // pinned jar registers the oredict name dustLimestone either, so the OreDictionary
        // fallback inside GTOreDictUnificator.get had nothing to find. This would have been the
        // next crash after Fluorite; the guard would now turn it into a skipped recipe, but a
        // skipped M-01a means no cement, and no cement means no concrete for the largest line
        // item in the entire build.
        //
        // Calcite is the right substitute rather than merely an available one: it is CaCO3, which
        // is what limestone IS, it is ID 823 in MaterialsIDMap with addDustItems() and
        // addOreItems(), and it is quarry output exactly as the paragraph above wants. The kiln
        // chemistry is unchanged - calcination drives CO2 off a calcium carbonate either way.
        BTSRecipeGuard.recipe(
            "M-01a",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Calcite, 6L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Clay, 2L),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Gypsum, 1L))
                .circuit(1)
                .itemOutputs(BTSRecipeGuard.req(OrePrefixes.dust, Materials.Concrete, 8L))
                .duration(16 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-01b Ready-mix batching and slipform casting. 2 cement : 6 sand : 8 gravel is a
        // 12.5 % binder fraction, which is what structural concrete actually is - and it is why
        // 118 batches cover a tier-5 platform, its two cooling towers and the Spiral's helix arcs.
        BTSRecipeGuard.recipe(
            "M-01b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.Concrete, 2L),
                    new ItemStack(Blocks.sand, 6),
                    new ItemStack(Blocks.gravel, 8))
                .fluidInputs(Materials.Water.getFluid(2_000))
                .itemOutputs(new ItemStack(GregTechAPI.sBlockConcretes, POUR, 8))
                .duration(8 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------------ M-02
    /**
     * <b>M-02 SHEET-METAL CASINGS - EV.</b> Solid Steel (509), Clean Stainless (452), Filter (152).
     * <p>
     * Real process: coil stock is roll-formed into panels, the panels are jigged onto a welded
     * structural frame, and the seams are closed by robotic gas-metal-arc welding under an argon
     * shield. The argon is the honest price of the bulk route - a bench assembler in NEI does not
     * buy shielding gas, a production cell does, and 125 L a casing is what it costs to weld eight
     * at a time.
     * <p>
     * Plate and frame counts are <b>exactly</b> GT's: 6 plates and 1 frame per casing, times eight.
     */
    private static void sheetMetal() {
        // M-02a Solid Steel Machine Casing. 6 plate + 1 frame each, as stock.
        BTSRecipeGuard.recipe(
            "M-02a",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Steel, 48L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Steel, 8L))
                .circuit(1)
                .fluidInputs(Materials.Argon.getGas(1_000))
                .itemOutputs(Casings.SolidSteelMachineCasing.toStack(CASINGS))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-02b Clean Stainless Steel Machine Casing. Austenitic stainless wants a full argon
        // shield AND an argon back-purge, or the weld sensitises and rusts out of the
        // heat-affected zone - which is exactly why the "clean" casing costs the same gas as the
        // plain one and not less.
        BTSRecipeGuard.recipe(
            "M-02b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.StainlessSteel, 48L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.StainlessSteel, 8L))
                .circuit(1)
                .fluidInputs(Materials.Argon.getGas(1_000))
                .itemOutputs(Casings.CleanStainlessSteelMachineCasing.toStack(CASINGS))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-02c Filter Machine Casing - a HEPA bank, built the way a HEPA bank is built: a steel
        // case, aluminium separators holding the pleats apart, and a borosilicate glass-fibre
        // medium drawn from the melt. The Vent Stack wants 152 of them and nothing else in the
        // plant wants any, so this is a one-shot line that runs for a minute and stops.
        BTSRecipeGuard.recipe(
            "M-02c",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Steel, 32L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Steel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.Aluminium, 16L))
                .fluidInputs(Materials.Glass.getMolten(2_304))
                .itemOutputs(Casings.FilterMachineCasing.toStack(CASINGS))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------------ M-03
    /**
     * <b>M-03 HEAVY SHIELDING - IV.</b> Radiation Proof (141), Tungstensteel Reinforced (249),
     * Iridium Reinforced (497).
     * <p>
     * Real process: a hot-cell wall is a steel liner, a lead sheet and a heavy-aggregate concrete
     * pour, in that order, because lead stops gammas and hydrogen-rich concrete stops neutrons and
     * neither does the other's job. All three recipes here are the same operation on three
     * shielding recipes, which is why they share a machine and are told apart by a circuit.
     */
    private static void shielding() {
        // M-03a Radiation Proof Machine Casing. Exactly 8x bartworks' recipe: 6 dense lead
        // plates, 1 tungstensteel frame and 1,296 L of pour per casing.
        BTSRecipeGuard.recipe(
            "M-03a",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plateDense, Materials.Lead, 48L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TungstenSteel, 8L))
                .circuit(1)
                .fluidInputs(Materials.Concrete.getMolten(10_368))
                .itemOutputs(Casings.RadiationProofMachineCasing.toStack(CASINGS))
                .duration(16 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-03b Tungstensteel Reinforced Block - THE INTERLOCK (S3). GT's Chemical Bath route is a
        // frame and 144 L of concrete; this adds one depleted uranium ingot per block as cast-in
        // ballast. DU is genuinely used for exactly this, it is what F-11's enrichment tails ARE,
        // and it means the fuel line pays for the platform the fuel line stands on.
        //
        // The stock route is untouched, so a player who has not enriched anything yet is never
        // blocked - they simply do not get the bulk route until they have tails to spend.
        BTSRecipeGuard.recipe(
            "M-03b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.TungstenSteel, 8L),
                    new ItemStack(BTSItemsFuel.depletedUraniumIngot, 8))
                .circuit(2)
                .fluidInputs(Materials.Concrete.getMolten(1_152))
                .itemOutputs(new ItemStack(GregTechAPI.sBlockReinforced, CASINGS, 3))
                .duration(16 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-03c Iridium Reinforced Block. Exactly 8x GT's Chemical Bath recipe - 1 frame and
        // 144 L of melt each - but 16s for eight instead of 400 ticks for one. The tier-5 silo
        // wants 497 of these; on the stock route that is 198,800 ticks, two hours and forty-six
        // minutes, in one Chemical Bath. Here it is 63 batches and four minutes.
        BTSRecipeGuard.recipe(
            "M-03c",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.Iridium, 8L))
                .circuit(3)
                .fluidInputs(Materials.Iridium.getMolten(1_152))
                .itemOutputs(new ItemStack(GregTechAPI.sBlockReinforced, CASINGS, 1))
                .duration(16 * SECONDS)
                .eut(TierEU.RECIPE_IV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------------ M-04
    /**
     * <b>M-04 EUROPIUM REINFORCED RADIATION PROOF - LuV.</b> The tier-3 and tier-4 silo shell: 963
     * at tier 3, <b>1,343 at tier 4</b>, and none at tier 5 where Radiant Naquadah takes over.
     * <p>
     * <b>Why this one could not simply be "8x the stock recipe".</b> GT's recipe fastens the
     * europium on with <b>24 screws and 6 foils per casing</b>; eight casings would be 192 screws,
     * and an ItemStack stops at 64. The bulk line does what a real bulk line does instead: it
     * <b>diffusion-bonds</b>. The europium arrives as a melt and is infiltrated into the composite
     * under heat and pressure, so there are no fasteners left to count.
     * <p>
     * Material parity is exact and deliberate, and it is checked against
     * {@code OrePrefixes.foil.mMaterialAmount} and {@code screw.mMaterialAmount} rather than
     * against memory - <b>GT's screw is M/8, not the M/9 it is easy to assume</b>, and getting that
     * wrong would have quietly made the bulk route 7 % cheaper in europium than the recipe it sits
     * beside. 6 foil (6/4 ingot) + 24 screw (24/8 ingot) = <b>4.5 ingots</b> of europium per
     * casing; 5,184 L over eight casings is 648 L = 4.5 ingots each. The
     * 864 L of molten lead per casing becomes 6 lead plates - the same metal by a different
     * delivery, because the machine has one input hatch and the europium melt has claimed it.
     */
    private static void europiumReinforced() {
        BTSRecipeGuard.recipe(
            "M-04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    Casings.RadiationProofMachineCasing.toStack(CASINGS),
                    BTSRecipeGuard.nn(
                        ItemList.Radiation_Proof_Prismatic_Naquadah_Composite_Sheet.get(32L),
                        "ItemList.Radiation_Proof_Prismatic_Naquadah_Composite_Sheet"),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Lead, 48L))
                .fluidInputs(Materials.Europium.getMolten(5_184))
                .itemOutputs(Casings.EuropiumReinforcedRadiationProofMachineCasing.toStack(CASINGS))
                .duration(24 * SECONDS)
                .eut(TierEU.RECIPE_LuV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------------ M-05
    /**
     * <b>M-05 RADIANT NAQUADAH ALLOY CASING - UV, then UHV.</b> 1,667 for a tier-5 silo: the
     * second-largest bill in the plant and by a wide margin the most expensive.
     * <p>
     * This is the line where the arithmetic has to be honest about what is actually hard. Machine
     * time was never the problem - GT's assembler recipe is 10 ticks for four. The problem is that
     * 1,667 casings means <b>5,004 Naquadah Alloy plates and 1,668 Prismatic Naquadah screws</b>
     * have to be rolled and cut first, and that is thousands of extruder and lathe operations
     * feeding one assembler that then finishes in three minutes.
     * <p>
     * So the two rungs attack different halves of it.
     */
    private static void radiantNaquadah() {
        // M-05a Vacuum brazing - UV. Five times GT's recipe, atom for atom: 1 frame, 12 plates,
        // 4 screws and 1,296 L of Indalloy 140 per four casings. Five and not eight because 8x
        // would want 96 plates in one slot and a stack stops at 64. What this rung buys is the
        // parallel - 20 casings a batch, 4 to 12 batches at once.
        BTSRecipeGuard.recipe(
            "M-05a",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.PrismaticNaquadah, 5L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.NaquadahAlloy, 60L),
                    BTSRecipeGuard.req(OrePrefixes.screw, Materials.PrismaticNaquadah, 20L))
                .circuit(1)
                .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(6_480))
                .itemOutputs(
                    BTSRecipeGuard
                        .nn(ItemList.RadiantNaquadahAlloyCasing.get(20L), "ItemList.RadiantNaquadahAlloyCasing"))
                .duration(4 * SECONDS)
                .eut(TierEU.RECIPE_UV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));

        // M-05b Centrifugal skin casting - UHV. The spectacular rung, and the one that deletes the
        // plate line: the naquadah-alloy skin is spun straight onto the prismatic space frame out
        // of the melt instead of being rolled, cut and brazed on. Centrifugal casting is a real
        // process and this is really what it is for - thin, sound, dense-walled shells with no
        // joint in them - it just happens here to be doing it to naquadah.
        //
        // 13,824 L is 96 ingots, which is 3 a casing: EXACTLY the 3 plates a casing costs on the
        // stock route. Nothing about the naquadah gets cheaper. What goes away is the 324 L of
        // Indalloy 140 per casing - 540,108 L over a tier-5 silo - because a cast joint is
        // metallurgical and needs no filler metal. What arrives in its place is a UHV energy hatch.
        BTSRecipeGuard.recipe(
            "M-05b",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.PrismaticNaquadah, 8L),
                    BTSRecipeGuard.req(OrePrefixes.screw, Materials.PrismaticNaquadah, 32L))
                .circuit(2)
                .fluidInputs(Materials.NaquadahAlloy.getMolten(13_824))
                .itemOutputs(
                    BTSRecipeGuard
                        .nn(ItemList.RadiantNaquadahAlloyCasing.get(POUR), "ItemList.RadiantNaquadahAlloyCasing"))
                .duration(8 * SECONDS)
                .eut(TierEU.RECIPE_UHV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------------ M-06
    /**
     * <b>M-06 TURBINE SHELL - EV.</b> 798 Tungstensteel Turbine Casings: 326 in the Spiral, 408 in
     * the rotor gallery and 64 in the tier-5 spine duct.
     * <p>
     * GT builds this casing in two passes - 6 Magnalium plates on a Blue Steel frame make a plain
     * Turbine Casing, then 6 Tungstensteel plates clad it. That is exactly how a real turbine
     * casing is built, as a bimetal weldment with a light inner shell and a creep-resistant outer
     * one, and it is also two assembler recipes and 1,596 operations for one plant.
     * <p>
     * Here both shells are jigged and welded in one fixture, which is also what a real fabricator
     * does once the order is for hundreds. Material cost is the sum of GT's two steps, unchanged,
     * plus the argon.
     */
    private static void turbineShell() {
        BTSRecipeGuard.recipe(
            "M-06",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Magnalium, 48L),
                    BTSRecipeGuard.req(OrePrefixes.frameGt, Materials.BlueSteel, 8L),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.TungstenSteel, 48L))
                .fluidInputs(Materials.Argon.getGas(1_000))
                .itemOutputs(Casings.TungstensteelTurbineCasing.toStack(CASINGS))
                .duration(12 * SECONDS)
                .eut(TierEU.RECIPE_EV)
                .addTo(MTESiteBatchingPlant.RECIPE_MAP));
    }

    // ------------------------------------------------------------- self-check
    /**
     * Prints the batch counts a tier-5 buildout needs, derived at load time from {@link BTSBom} -
     * the file {@code gen-bom.ps1} regenerates out of the structure code and refuses to leave
     * stale.
     * <p>
     * <b>This is not decoration.</b> The batch counts in this file's comments were computed by hand
     * against today's geometry; if somebody widens the octagon or lengthens a duct, the generated
     * bill moves and the comments go quietly wrong. Logging the derived numbers means the drift
     * shows up in one line of the log the next time the mod loads, instead of in a questbook that
     * promises 118 concrete pours when the platform has started wanting 130.
     */
    private static void logBillCoverage() {
        final BTSBom.Bill t5 = BTSBom.TOTAL[BTSBom.MAX_TIER];
        BeyondTheStars.LOG.info(
            "Material lines, tier-5 batches: concrete " + batches(t5.of(BTSBom.LIGHT_CONCRETE), POUR)
                + ", radiant naquadah "
                + batches(t5.of(BTSBom.RADIANT_NAQUADAH_ALLOY_CASING), POUR)
                + ", turbine "
                + batches(t5.of(BTSBom.TUNGSTENSTEEL_TURBINE_CASING), CASINGS)
                + ", solid steel "
                + batches(t5.of(BTSBom.SOLID_STEEL_MACHINE_CASING), CASINGS)
                + ", iridium "
                + batches(t5.of(BTSBom.IRIDIUM_REINFORCED_BLOCK), CASINGS)
                + ", stainless "
                + batches(t5.of(BTSBom.CLEAN_STAINLESS_STEEL_MACHINE_CASING), CASINGS)
                + ", tungstensteel "
                + batches(t5.of(BTSBom.TUNGSTENSTEEL_REINFORCED_BLOCK), CASINGS)
                + ", filter "
                + batches(t5.of(BTSBom.FILTER_MACHINE_CASING), CASINGS)
                + ", radiation proof "
                + batches(t5.of(BTSBom.RADIATION_PROOF_MACHINE_CASING), CASINGS));
    }

    /** Batches needed to cover a bill, rounding up - a part batch is still a batch. */
    private static int batches(int need, int perBatch) {
        return (need + perBatch - 1) / perBatch;
    }
}
