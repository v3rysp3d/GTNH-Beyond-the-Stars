package com.nateyburr.beyondthestars.fluids;

import net.minecraftforge.fluids.Fluid;

import gregtech.api.enums.FluidState;

/**
 * D-26 FUEL LINE FLUIDS - the twelve process streams of the ore-to-bundle chain and the PUREX
 * head-end (docs/production-lines.md S1). Owned by <b>Agent A (Fuel)</b>.
 * <p>
 * <b>The three UF6 streams are the point of the file.</b> F-11's cascade is the only step in the
 * pack whose defining property is that it makes TWO products - a small enriched cut and a large
 * tails stream - and the tails are a real product, not a discard: F-20 reconverts them into the
 * MOX diluent and F-11c reduces them to the depleted-uranium metal M-03b spends on shielding
 * casings. Three separate fluids is what makes that legible in NEI.
 * <p>
 * <b>Not registered here, deliberately:</b> uranium tetrafluoride and uranium hexafluoride
 * THEMSELVES. GT++ already ships {@code MaterialsFluorides.URANIUM_TETRAFLUORIDE} and
 * {@code URANIUM_HEXAFLUORIDE} for the LFTR chain (collision C2), and F-09/F-10 deliberately
 * produce those existing materials from ore rather than a private copy - so a standing LFTR line
 * can feed its UF6 straight into our cascade. Only the three ENRICHMENT ASSAYS, which GT++ has no
 * concept of, are ours.
 */
public final class BTSFluidsProcess {

    // ------------------------------------------------------------------ F-02, the flotation cell
    /**
     * F-02 out: the floated concentrate, still as a slurry.
     * <p>
     * <b>This fluid exists because the Flotation Cell physically cannot make an item.</b>
     * {@code GTPPRecipeMaps.flotationCellRecipes} is declared {@code maxIO(6, 0, 1, 1)} - six item
     * inputs, <b>zero</b> item outputs, one fluid in, one fluid out - which is exactly how GT++'s
     * own sphalerite and chalcopyrite lines run: milled ore plus a collector plus pine oil, out
     * comes a froth FLUID. F-02b then thickens and filters the froth back into solids on the
     * Dehydrator, which is what a real flotation circuit does anyway.
     */
    public static Fluid uraniniteFlotationFroth;

    // --------------------------------------------------------- F-03..F-08, the hydrometallurgy
    /** F-03 out: the pregnant leach solution off the sulfuric digest. */
    public static Fluid pregnantLeachSolution;
    /** F-04 out: PLS after clarification and ion exchange. */
    public static Fluid clarifiedUranylLiquor;
    /** F-04 / F-05 byproduct: the barren aqueous phase. F-04b recovers its acid. */
    public static Fluid raffinate;
    /** F-05a out: tributyl phosphate in kerosene - the real PUREX/SX extractant. */
    public static Fluid tbpSolvent;
    /** F-05 out: the organic phase with the uranium in it. */
    public static Fluid loadedOrganic;
    /** F-06 out: uranium stripped back into water, ready to precipitate. */
    public static Fluid uranylStripLiquor;
    /** F-07 byproduct: a real ADU-plant co-product. */
    public static Fluid ammoniumSulfateLiquor;

    // ---------------------------------------------------------- F-11, the three cascade assays
    /** F-11 out: the ~3.2% cut. The only stream F-12 will reconvert into fuel-grade powder. */
    public static Fluid enrichedUF6;
    /** F-11 out: the tails. A PRODUCT - F-20 makes MOX diluent, F-11c makes DU metal. */
    public static Fluid depletedUF6;
    /** F-11b out: the HEU cut, for the naquadah-bonded grade only. */
    public static Fluid highlyEnrichedUF6;

    // ------------------------------------------------------------------- R-02, the PUREX head-end
    /** R-02 out: chopped fuel dissolved in nitric acid, ready to partition. */
    public static Fluid dissolverLiquor;

    private BTSFluidsProcess() {}

    public static void register() {
        uraniniteFlotationFroth = liquid("btsuraniniteflotationfroth", "Uraninite Flotation Froth", 150, 145, 110, 300);

        pregnantLeachSolution = liquid("btspregnantleachsolution", "Pregnant Leach Solution", 210, 190, 70, 320);
        clarifiedUranylLiquor = liquid("btsclarifieduranylliquor", "Clarified Uranyl Liquor", 230, 215, 110, 320);
        raffinate = liquid("btsraffinate", "Uranium Raffinate", 130, 120, 95, 320);
        tbpSolvent = liquid("btstbpsolvent", "TBP/Kerosene Solvent", 190, 175, 130, 300);
        loadedOrganic = liquid("btsloadedorganic", "Loaded Organic", 210, 165, 60, 300);
        uranylStripLiquor = liquid("btsuranylstripliquor", "Uranyl Strip Liquor", 245, 225, 95, 320);
        ammoniumSulfateLiquor = liquid("btsammoniumsulfateliquor", "Ammonium Sulfate Liquor", 200, 210, 200, 320);

        // The three assays are GASES: UF6 sublimes at 56 C and the whole cascade runs on that.
        enrichedUF6 = gas("btsenricheduf6", "Enriched Uranium Hexafluoride", 150, 230, 130, 350);
        depletedUF6 = gas("btsdepleteduf6", "Depleted Uranium Hexafluoride", 150, 160, 150, 350);
        highlyEnrichedUF6 = gas("btshighlyenricheduf6", "Highly Enriched Uranium Hexafluoride", 120, 255, 160, 350);

        dissolverLiquor = liquid("btsdissolverliquor", "Dissolver Liquor", 120, 165, 110, 360);
    }

    private static Fluid liquid(String name, String localName, int r, int g, int b, int kelvin) {
        return build(name, localName, r, g, b, kelvin, FluidState.LIQUID);
    }

    private static Fluid gas(String name, String localName, int r, int g, int b, int kelvin) {
        return build(name, localName, r, g, b, kelvin, FluidState.GAS);
    }

    private static Fluid build(String name, String localName, int r, int g, int b, int kelvin, FluidState state) {
        return gregtech.api.fluid.GTFluidFactory.builder(name)
            .withDefaultLocalName(localName)
            .withTextureName("molten.autogenerated")
            .withColorRGBA(new short[] { (short) r, (short) g, (short) b, (short) 240 })
            .withStateAndTemperature(state, kelvin)
            .buildAndRegister()
            .addLocalizedName()
            .asFluid();
    }
}
