package com.nateyburr.beyondthestars;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.items.SLCircuitItems;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import tectech.recipe.TTRecipeAdder;

/**
 * CIRCUITS v2, phase C2: THE CIRCUIT CHAIN (design repo {@code designs/docs/circuits-v2.md} §2, revision 3).
 * <b>One owner: the circuits unit.</b> Called from one line in {@link BTSRecipes} ("circuits").
 *
 * <h2>What is here (ids SL-X01..X32)</h2>
 * <ul>
 * <li><b>Wafers and chips</b> (§2.1): SL-X01 Stellar Boule (EBF, 12,600 K, Raw Star Matter: the T1 gate), SL-X02/X03
 * Stellar Wafer (Cutting Machine, Grade-7 / Grade-8 water), SL-X04..X06 the three engravings (Laser Engraver; each
 * family engraves the wafer of the family below: Raw Star Matter T1, White Dwarf Matter T4, Black Dwarf Matter T5),
 * SL-X07..X12 the chips (8 per wafer, 12 with Grade-8 water).</li>
 * <li><b>Stellar SMDs</b> (§2.2): SL-X13..X17, Assembler, 64 each, ESPM (Eye T2 or the Homogeny).</li>
 * <li><b>Boards</b> (§2.3): SL-X18..X20, Assembler, no Eye matter (they can be stockpiled before the Eye).</li>
 * <li><b>Circuits</b> (§2.4): Processors SL-X21 / X25 / X29 in the Circuit Assembler; Assembly, Supercomputer and
 * Mainframe (SL-X22..X24, X26..X28, X30..X32) as <b>research Assembly Line</b> recipes through TecTech's
 * {@link TTRecipeAdder} (the {@code Object[]} overload GT's Optical line uses), researched in the Research Station
 * from the member below. Each higher family's Mainframe also takes one Mainframe of the family below.</li>
 * </ul>
 *
 * <h2>Rules</h2>
 * <ul>
 * <li>Only stock GT machines run these (no machine of ours gains a recipe), so every step automates with a normal AE2
 * pattern. Lenses are not consumed (stack size 0).</li>
 * <li>No step runs above RECIPE_UMV (§2.4): UMV is the highest voltage a player at Eye T4-T6 can feed through normal
 * hatches.</li>
 * <li>Revision 3 (user: "Any of the tier such as the (any uxv) forming oress recipe"): nothing outside this chain
 * names our circuits. They enter every other recipe only through GT's ore names ({@link SLCircuitItems}).</li>
 * <li>With {@code stellarCircuits=false} the whole line registers nothing (one log line).</li>
 * </ul>
 * Every recipe runs under {@link BTSRecipeGuard#recipe}; every foreign component goes through {@code req}/{@code nn}.
 */
public final class BTSRecipesCircuits {

    private BTSRecipesCircuits() {}

    /** Every recipe id of this line, in order (for the boot check and /btsrecipes circuits). */
    public static final int RECIPE_COUNT = 32;

    public static void register() {
        if (!Config.stellarCircuits) {
            BeyondTheStars.LOG.info("[BTS] Circuits v2: stellarCircuits=false, no SL-X recipe registered.");
            return;
        }
        BTSRecipeLedger.gated("stellarCircuits=true", () -> {
            wafers();
            chips();
            smds();
            boards();
            processors();
            researchedCircuits();
        });
    }

    // ============================================================================== §2.1 wafers and chips

    private static void wafers() {
        BTSRecipeGuard.recipe(
            "SL-X01",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Circuit_Silicon_Ingot6.get(1), "Optically Enriched Crystalline Boule"),
                    nn(GGMaterial.shirabon.get(OrePrefixes.dust, 4), "Shirabon dust"),
                    BTSRecipeGuard.req(OrePrefixes.dust, Materials.SpaceTime, 1L))
                .fluidInputs(nn(Materials.RawStarMatter.getFluid(1_000L), "Raw Star Matter"))
                .itemOutputs(ours(SLCircuitItems.stellarBoule, 1))
                .metadata(COIL_HEAT, 12_600)
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.blastFurnaceRecipes));
        cutWafer("SL-X02", 16, Materials.Grade7PurifiedWater);
        cutWafer("SL-X03", 24, Materials.Grade8PurifiedWater);

        // Each family engraves the wafer of the family below (§2.1): the ladder's story in one die.
        BTSRecipeGuard.recipe(
            "SL-X04",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.stellarWafer, 1),
                    GTUtility.copyAmount(
                        0,
                        nn(
                            GTModHandler.getModItem(Mods.NewHorizonsCoreMod.ID, "MysteriousCrystalLens", 1),
                            "Quite Certain Crystal Lens (Mysterious Crystal, dreamcraft)")))
                .fluidInputs(nn(Materials.RawStarMatter.getFluid(250L), "Raw Star Matter"))
                .itemOutputs(ours(SLCircuitItems.siderealWafer, 1))
                .requiresCleanRoom()
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.laserEngraverRecipes));
        BTSRecipeGuard.recipe(
            "SL-X05",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.siderealWafer, 1),
                    nn(ItemList.Gravitational_Lens.get(0), "Gravitational Lens"))
                .fluidInputs(
                    nn(Materials.WhiteDwarfMatter.getMolten(36L), "molten White Dwarf Matter"),
                    BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 250))
                .itemOutputs(ours(SLCircuitItems.empyreanWafer, 1))
                .requiresCleanRoom()
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RecipeMaps.laserEngraverRecipes));
        BTSRecipeGuard.recipe(
            "SL-X06",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(ours(SLCircuitItems.empyreanWafer, 1), lens(Materials.Forcicium))
                .fluidInputs(
                    nn(Materials.BlackDwarfMatter.getMolten(18L), "molten Black Dwarf Matter"),
                    BTSRecipeGuard.fluid(SLFluids.hyperdenseStellarMixture, 100))
                .itemOutputs(ours(SLCircuitItems.omegaWafer, 1))
                .requiresCleanRoom()
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RecipeMaps.laserEngraverRecipes));
    }

    /** SL-X02/X03: the Stellar Boule cut like GT's Ingot6 into Wafer6 (more wafers with the better water). */
    private static void cutWafer(String id, int wafers, Materials water) {
        BTSRecipeGuard.recipe(
            id,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(ours(SLCircuitItems.stellarBoule, 1))
                .fluidInputs(nn(water.getFluid(1_000L), water.mName))
                .itemOutputs(ours(SLCircuitItems.stellarWafer, wafers))
                .requiresCleanRoom()
                .duration(50 * SECONDS)
                .eut(TierEU.RECIPE_UEV)
                .addTo(RecipeMaps.cutterRecipes));
    }

    private static void chips() {
        chip("SL-X07", SLCircuitItems.siderealWafer, SLCircuitItems.siderealChip, 8, Materials.Grade7PurifiedWater);
        chip("SL-X08", SLCircuitItems.siderealWafer, SLCircuitItems.siderealChip, 12, Materials.Grade8PurifiedWater);
        chip("SL-X09", SLCircuitItems.empyreanWafer, SLCircuitItems.empyreanChip, 8, Materials.Grade7PurifiedWater);
        chip("SL-X10", SLCircuitItems.empyreanWafer, SLCircuitItems.empyreanChip, 12, Materials.Grade8PurifiedWater);
        chip("SL-X11", SLCircuitItems.omegaWafer, SLCircuitItems.omegaChip, 8, Materials.Grade7PurifiedWater);
        chip("SL-X12", SLCircuitItems.omegaWafer, SLCircuitItems.omegaChip, 12, Materials.Grade8PurifiedWater);
    }

    private static void chip(String id, Item wafer, Item chip, int count, Materials water) {
        BTSRecipeGuard.recipe(
            id,
            () -> GTValues.RA.stdBuilder()
                .itemInputs(ours(wafer, 1))
                .fluidInputs(nn(water.getFluid(250L), water.mName))
                .itemOutputs(ours(chip, count))
                .requiresCleanRoom()
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.cutterRecipes));
    }

    // =================================================================================== §2.2 Stellar SMDs

    private static void smds() {
        smd(
            "SL-X13",
            SLCircuitItems.stellarSmdTransistor,
            () -> new ItemStack[] { hypogenFineWire(32),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 4L) });
        smd(
            "SL-X14",
            SLCircuitItems.stellarSmdResistor,
            () -> new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.wireFine, Materials.ProtoHalkonite, 32L),
                shirabonFoil(4) });
        smd(
            "SL-X15",
            SLCircuitItems.stellarSmdCapacitor,
            () -> new ItemStack[] { BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 8L),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.RadoxPolymer, 8L) });
        smd(
            "SL-X16",
            SLCircuitItems.stellarSmdDiode,
            () -> new ItemStack[] { hypogenFineWire(16),
                BTSRecipeGuard.req(OrePrefixes.foil, Materials.SixPhasedCopper, 8L) });
        smd(
            "SL-X17",
            SLCircuitItems.stellarSmdInductor,
            () -> new ItemStack[] { nn(MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getFoil(8), "Astral Titanium foil"),
                BTSRecipeGuard.req(OrePrefixes.wireFine, Materials.ProtoHalkonite, 16L) });
    }

    /** One Stellar SMD kind: a Stellar Wafer, the kind's foils / fine wire and 144 L ESPM make 64 (cr, 20 s UIV). */
    private static void smd(String id, Item out, Supplier<ItemStack[]> parts) {
        BTSRecipeGuard.recipe(id, () -> {
            final ItemStack[] p = parts.get();
            final ItemStack[] in = new ItemStack[p.length + 1];
            in[0] = ours(SLCircuitItems.stellarWafer, 1);
            System.arraycopy(p, 0, in, 1, p.length);
            GTValues.RA.stdBuilder()
                .itemInputs(in)
                .fluidInputs(BTSRecipeGuard.fluid(SLFluids.excitedStellarPlasma, 144))
                .itemOutputs(ours(out, 64))
                .requiresCleanRoom()
                .duration(20 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.assemblerRecipes);
        });
    }

    // ======================================================================================== §2.3 boards

    private static void boards() {
        BTSRecipeGuard.recipe(
            "SL-X18",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    nn(ItemList.Circuit_Board_Optical.get(4), "Optical Circuit Board"),
                    shirabonFoil(16),
                    hypogenFineWire(32))
                .fluidInputs(mls(576))
                .itemOutputs(ours(SLCircuitItems.siderealCircuitBoard, 4))
                .requiresCleanRoom()
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.assemblerRecipes));
        BTSRecipeGuard.recipe(
            "SL-X19",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.siderealCircuitBoard, 4),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.SpaceTime, 16L),
                    BTSRecipeGuard.req(OrePrefixes.foil, Materials.RadoxPolymer, 16L))
                .fluidInputs(nn(Materials.RadoxPolymer.getMolten(1_152L), "molten Radox Polymer"))
                .itemOutputs(ours(SLCircuitItems.empyreanCircuitBoard, 4))
                .requiresCleanRoom()
                .duration(40 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RecipeMaps.assemblerRecipes));
        BTSRecipeGuard.recipe(
            "SL-X20",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.empyreanCircuitBoard, 4),
                    BTSRecipeGuard.req(OrePrefixes.plate, Materials.Hexanite, 8L),
                    shirabonFoil(32))
                .fluidInputs(nn(Materials.DTR.getFluid(1_000L), "Dimensionally Transcendent Residue"))
                .itemOutputs(ours(SLCircuitItems.omegaCircuitBoard, 4))
                .requiresCleanRoom()
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_UMV)
                .addTo(RecipeMaps.assemblerRecipes));
    }

    // ================================================================================ §2.4 the circuits

    /** The three Processors: Circuit Assembler, cleanroom (so a Circuit Assembly Line in assembler mode runs them). */
    private static void processors() {
        BTSRecipeGuard.recipe(
            "SL-X21",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.siderealChip, 1),
                    nn(ItemList.Optically_Compatible_Memory.get(2), "Optically Compatible Memory"),
                    nn(ItemList.Circuit_Parts_TransistorXSMD.get(8), "Optical SMD Transistor"),
                    nn(ItemList.Circuit_Parts_CapacitorXSMD.get(8), "Optical SMD Capacitor"),
                    BTSRecipeGuard.req(OrePrefixes.bolt, Materials.EnrichedHolmium, 8L))
                .fluidInputs(mls(144))
                .itemOutputs(ours(SLCircuitItems.siderealProcessor, 1))
                .requiresCleanRoom()
                .duration(30 * SECONDS)
                .eut(TierEU.RECIPE_UEV)
                .addTo(RecipeMaps.circuitAssemblerRecipes));
        BTSRecipeGuard.recipe(
            "SL-X25",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.empyreanChip, 1),
                    nn(ItemList.Optically_Compatible_Memory.get(4), "Optically Compatible Memory"),
                    ours(SLCircuitItems.stellarSmdTransistor, 16),
                    ours(SLCircuitItems.stellarSmdCapacitor, 16),
                    BTSRecipeGuard.req(OrePrefixes.bolt, Materials.SuperconductorUMVBase, 8L))
                .fluidInputs(mls(288))
                .itemOutputs(ours(SLCircuitItems.empyreanProcessor, 1))
                .requiresCleanRoom()
                .duration(60 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.circuitAssemblerRecipes));
        BTSRecipeGuard.recipe(
            "SL-X29",
            () -> GTValues.RA.stdBuilder()
                .itemInputs(
                    ours(SLCircuitItems.omegaChip, 1),
                    nn(ItemList.Optically_Compatible_Memory.get(8), "Optically Compatible Memory"),
                    ours(SLCircuitItems.stellarSmdTransistor, 32),
                    ours(SLCircuitItems.stellarSmdCapacitor, 32),
                    BTSRecipeGuard.req(OrePrefixes.bolt, Materials.TranscendentMetal, 8L))
                .fluidInputs(mls(576))
                .itemOutputs(ours(SLCircuitItems.omegaProcessor, 1))
                .requiresCleanRoom()
                .duration(240 * SECONDS)
                .eut(TierEU.RECIPE_UIV)
                .addTo(RecipeMaps.circuitAssemblerRecipes));
    }

    /**
     * The nine research Assembly Line circuits (§2.4). Research numbers: total computation, computation per second,
     * research EU/t and amperage; GT's Optical line for scale (Assembly 192,000 at 256/s, UHV 16 A).
     */
    private static void researchedCircuits() {
        // ---- Sidereal (Raw Star Matter, Eye T1). Research UEV x 16 / 32 / 64.
        rsal(
            "SL-X22",
            SLCircuitItems.siderealProcessor,
            384_000,
            512,
            TierEU.RECIPE_UEV,
            16,
            () -> new Object[] { ours(SLCircuitItems.siderealCircuitBoard, 1),
                ours(SLCircuitItems.siderealProcessor, 2), xsmd(ItemList.Circuit_Parts_InductorXSMD, 16),
                xsmd(ItemList.Circuit_Parts_CapacitorXSMD, 16), xsmd(ItemList.Circuit_Parts_ResistorXSMD, 16),
                gt(ItemList.Circuit_Chip_NOR, 32), gt(ItemList.Circuit_Chip_Ram, 64), hypogenFineWire(24),
                rubberFoil() },
            () -> fluids(1),
            SLCircuitItems.siderealAssembly,
            40,
            TierEU.RECIPE_UIV);
        rsal(
            "SL-X23",
            SLCircuitItems.siderealAssembly,
            768_000,
            1_024,
            TierEU.RECIPE_UEV,
            32,
            () -> new Object[] { ours(SLCircuitItems.siderealCircuitBoard, 2), ours(SLCircuitItems.siderealAssembly, 2),
                xsmd(ItemList.Circuit_Parts_TransistorXSMD, 24), xsmd(ItemList.Circuit_Parts_ResistorXSMD, 24),
                xsmd(ItemList.Circuit_Parts_CapacitorXSMD, 24), xsmd(ItemList.Circuit_Parts_DiodeXSMD, 24),
                gt(ItemList.Circuit_Chip_NOR, 64), gt(ItemList.Circuit_Chip_SoC2, 32), hypogenFineWire(32),
                rubberFoil(), pbiFoil() },
            () -> fluids(2),
            SLCircuitItems.siderealSupercomputer,
            150,
            TierEU.RECIPE_UIV);
        rsal(
            "SL-X24",
            SLCircuitItems.siderealSupercomputer,
            1_536_000,
            2_048,
            TierEU.RECIPE_UEV,
            64,
            () -> new Object[] {
                nn(MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getFrameBox(4), "Celestial Tungsten frame"),
                ours(SLCircuitItems.siderealSupercomputer, 2), xsmd(ItemList.Circuit_Parts_InductorXSMD, 32),
                xsmd(ItemList.Circuit_Parts_TransistorXSMD, 32), xsmd(ItemList.Circuit_Parts_ResistorXSMD, 32),
                xsmd(ItemList.Circuit_Parts_CapacitorXSMD, 32), xsmd(ItemList.Circuit_Parts_DiodeXSMD, 32),
                gt(ItemList.Circuit_Chip_SoC2, 64),
                BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUMV, 16L), rubberFoil(), pbiFoil() },
            () -> fluids(3),
            SLCircuitItems.siderealMainframe,
            100,
            TierEU.RECIPE_UMV);

        // ---- Empyrean (White Dwarf Matter, Eye T4). Research UIV x 16 / 32 / 64.
        rsal(
            "SL-X26",
            SLCircuitItems.empyreanProcessor,
            768_000,
            1_024,
            TierEU.RECIPE_UIV,
            16,
            () -> new Object[] { ours(SLCircuitItems.empyreanCircuitBoard, 1),
                ours(SLCircuitItems.empyreanProcessor, 2), ours(SLCircuitItems.stellarSmdInductor, 24),
                ours(SLCircuitItems.stellarSmdCapacitor, 24), ours(SLCircuitItems.stellarSmdResistor, 24),
                gt(ItemList.Circuit_Chip_NOR, 64), gt(ItemList.Circuit_Chip_Ram, 64),
                BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SpaceTime, 8L), rubberFoil() },
            () -> fluids(2),
            SLCircuitItems.empyreanAssembly,
            30,
            TierEU.RECIPE_UMV);
        rsal(
            "SL-X27",
            SLCircuitItems.empyreanAssembly,
            1_536_000,
            2_048,
            TierEU.RECIPE_UIV,
            32,
            () -> new Object[] { ours(SLCircuitItems.empyreanCircuitBoard, 2), ours(SLCircuitItems.empyreanAssembly, 2),
                ours(SLCircuitItems.stellarSmdTransistor, 32), ours(SLCircuitItems.stellarSmdResistor, 32),
                ours(SLCircuitItems.stellarSmdCapacitor, 32), ours(SLCircuitItems.stellarSmdDiode, 32),
                gt(ItemList.Circuit_Chip_SoC2, 64), BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SpaceTime, 16L),
                rubberFoil(), pbiFoil() },
            () -> fluids(3),
            SLCircuitItems.empyreanSupercomputer,
            60,
            TierEU.RECIPE_UMV);
        rsal(
            "SL-X28",
            SLCircuitItems.empyreanSupercomputer,
            3_072_000,
            4_096,
            TierEU.RECIPE_UIV,
            64,
            () -> new Object[] { nn(MaterialsElements.STANDALONE.HYPOGEN.getFrameBox(4), "Hypogen frame"),
                ours(SLCircuitItems.empyreanSupercomputer, 2), ours(SLCircuitItems.siderealMainframe, 1),
                ours(SLCircuitItems.stellarSmdTransistor, 48), ours(SLCircuitItems.stellarSmdResistor, 48),
                ours(SLCircuitItems.stellarSmdCapacitor, 48), ours(SLCircuitItems.stellarSmdDiode, 48),
                ours(SLCircuitItems.stellarSmdInductor, 48), gt(ItemList.Circuit_Chip_SoC2, 64),
                BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUMV, 32L), rubberFoil(), pbiFoil() },
            () -> fluids(4),
            SLCircuitItems.empyreanMainframe,
            120,
            TierEU.RECIPE_UMV);

        // ---- Omega (Black Dwarf Matter, Eye T5). Research UMV x 4 / 8 / 16.
        rsal(
            "SL-X30",
            SLCircuitItems.omegaProcessor,
            1_536_000,
            2_048,
            TierEU.RECIPE_UMV,
            4,
            () -> new Object[] { ours(SLCircuitItems.omegaCircuitBoard, 1), ours(SLCircuitItems.omegaProcessor, 2),
                ours(SLCircuitItems.stellarSmdInductor, 32), ours(SLCircuitItems.stellarSmdCapacitor, 32),
                ours(SLCircuitItems.stellarSmdResistor, 32), gt(ItemList.Circuit_Chip_NOR, 64),
                gt(ItemList.Circuit_Chip_Ram, 64),
                BTSRecipeGuard.req(OrePrefixes.wireFine, Materials.WhiteDwarfMatter, 32L), rubberFoil() },
            () -> fluids(3),
            SLCircuitItems.omegaAssembly,
            80,
            TierEU.RECIPE_UMV);
        rsal(
            "SL-X31",
            SLCircuitItems.omegaAssembly,
            3_072_000,
            4_096,
            TierEU.RECIPE_UMV,
            8,
            () -> new Object[] { ours(SLCircuitItems.omegaCircuitBoard, 2), ours(SLCircuitItems.omegaAssembly, 2),
                ours(SLCircuitItems.stellarSmdTransistor, 48), ours(SLCircuitItems.stellarSmdResistor, 48),
                ours(SLCircuitItems.stellarSmdCapacitor, 48), ours(SLCircuitItems.stellarSmdDiode, 48),
                gt(ItemList.Circuit_Chip_SoC2, 64),
                BTSRecipeGuard.req(OrePrefixes.wireFine, Materials.BlackDwarfMatter, 48L), rubberFoil(), pbiFoil() },
            () -> fluids(4),
            SLCircuitItems.omegaSupercomputer,
            160,
            TierEU.RECIPE_UMV);
        rsal(
            "SL-X32",
            SLCircuitItems.omegaSupercomputer,
            6_144_000,
            4_096,
            TierEU.RECIPE_UMV,
            16,
            () -> new Object[] { nn(MaterialsElements.STANDALONE.HYPOGEN.getFrameBox(4), "Hypogen frame"),
                BTSRecipeGuard.req(OrePrefixes.plate, Materials.Hexanite, 8L),
                ours(SLCircuitItems.omegaSupercomputer, 2), ours(SLCircuitItems.empyreanMainframe, 1),
                ours(SLCircuitItems.stellarSmdTransistor, 64), ours(SLCircuitItems.stellarSmdResistor, 64),
                ours(SLCircuitItems.stellarSmdCapacitor, 64), ours(SLCircuitItems.stellarSmdDiode, 64),
                ours(SLCircuitItems.stellarSmdInductor, 64), gt(ItemList.Circuit_Chip_SoC2, 64),
                BTSRecipeGuard.req(OrePrefixes.wireGt01, Materials.SuperconductorUMV, 64L), rubberFoil(), pbiFoil() },
            () -> fluids(5),
            SLCircuitItems.omegaMainframe,
            250,
            TierEU.RECIPE_UMV);
    }

    /**
     * One research Assembly Line recipe through TecTech (the Research Station writes the data stick from
     * {@code research}; the Assembly Line reads it). Inputs are built inside the guard, so a missing component skips
     * this one recipe. TecTech answers false (and adds nothing) on bad sizes; that is turned into a skip too.
     */
    @SuppressWarnings("deprecation")
    private static void rsal(String id, Item research, long totalComputation, long perSecond, long researchEUt,
        int amperage, Supplier<Object[]> inputs, Supplier<FluidStack[]> fluids, Item out, int seconds, long eut) {
        BTSRecipeGuard.recipe(id, () -> {
            final boolean added = TTRecipeAdder.addResearchableAssemblylineRecipeLong(
                ours(research, 1),
                totalComputation,
                perSecond,
                (int) researchEUt,
                amperage,
                inputs.get(),
                fluids.get(),
                ours(out, 1),
                seconds * SECONDS,
                (int) eut);
            if (!added) {
                throw new IllegalStateException("TecTech refused the research assembly line (sizes or a null input)");
            }
        });
    }

    /**
     * The assembly-line fluids by step (§2.4, half of GT's Optical volumes per step): A1 MLS 720, Radon plasma 720,
     * Super Coolant 10,000; every later step doubles them and adds Oganesson (1,000 L at A2).
     */
    private static FluidStack[] fluids(int step) {
        final int scale = 1 << (step - 1);
        final FluidStack solder = mls(720 * scale);
        final FluidStack radon = nn(Materials.Radon.getPlasma(720L * scale), "Radon plasma");
        final FluidStack coolant = nn(Materials.SuperCoolant.getFluid(10_000L * scale), "Super Coolant");
        if (step == 1) return new FluidStack[] { solder, radon, coolant };
        final FluidStack oganesson = BTSRecipeGuard.fluid(FluidRegistry.getFluid("oganesson"), 500 * scale);
        return new FluidStack[] { solder, radon, coolant, oganesson };
    }

    // =========================================================================================== helpers

    /**
     * A lens that stays in the bus (stack size 0). GT's 3-argument lookup answers null for amount 0, so the lens is
     * resolved at 1 and copied to 0, as GT's own engraver recipes do with {@code get(lens, m, 0, false)}.
     */
    private static ItemStack lens(Materials material) {
        return GTUtility.copyAmount(0, BTSRecipeGuard.req(OrePrefixes.lens, material, 1L));
    }

    private static ItemStack ours(Item item, int count) {
        return BTSRecipeGuard.stack(item, count);
    }

    private static <T> T nn(T value, String what) {
        return BTSRecipeGuard.nn(value, what);
    }

    private static ItemStack gt(ItemList item, int count) {
        return nn(item.get(count), "ItemList." + item.name());
    }

    private static ItemStack xsmd(ItemList item, int count) {
        return gt(item, count);
    }

    private static ItemStack hypogenFineWire(int count) {
        return nn(MaterialsElements.STANDALONE.HYPOGEN.getFineWire(count), "Hypogen fine wire");
    }

    private static ItemStack shirabonFoil(int count) {
        return nn(GGMaterial.shirabon.get(OrePrefixes.foil, count), "Shirabon foil");
    }

    /** Silicone Rubber foil 64 (§2.4; the NAC's FoilSiliconeRubber component). */
    private static ItemStack rubberFoil() {
        return BTSRecipeGuard.req(OrePrefixes.foil, Materials.RubberSilicone, 64L);
    }

    /** Polybenzimidazole foil 64. */
    private static ItemStack pbiFoil() {
        return BTSRecipeGuard.req(OrePrefixes.foil, Materials.Polybenzimidazole, 64L);
    }

    private static FluidStack mls(int litres) {
        return nn(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(litres), "Mutated Living Solder");
    }
}
