package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

/**
 * D-26 FUEL LINE ITEMS - every intermediate of the 23-step ore-to-bundle chain and of the PUREX
 * head-end (docs/production-lines.md S1). Owned by <b>Track A (Fuel)</b>: nobody else adds a
 * field here, and the fields are read - never written - by everyone else.
 * <p>
 * The four PRODUCTS of this chain are not here. {@code uraniumFuelPellet}, {@code fuelRodUranium},
 * {@code fuelAssemblyUranium} and their MOX / thorium / naquadah siblings stay in
 * {@link BTSItems}, exactly where an existing world's stacks are already bound to them. The
 * redesign changes the ROUTE to those items and nothing about the items themselves - which is
 * what makes every pellet, rod and assembly in a player's chest worth precisely what it was worth
 * yesterday (S8.2).
 * <p>
 * <b>Why the pressing and sintering intermediates are grade-tagged.</b> F-13 and F-14 are shared
 * by all three ceramic grades, and the design distinguishes the grades by recipe circuit. A
 * circuit cannot survive an ITEM, though: one fungible "Green Pellet" would let uranium powder be
 * pressed, then sintered, then ground on circuit 2 into MOX pellets without a gram of plutonium
 * ever entering the line. So the press and the furnace carry the grade in the item -
 * {@code greenPellet} / {@code greenPelletMox} / {@code greenPelletThorium} - and the circuit only
 * ever selects between recipes that already have the right input.
 */
public final class BTSItemsFuel {

    // --------------------------------------------------------------- F-01..F-08, the front end
    /** F-01 out: wet-attrition-milled ore, ready for flotation. */
    public static Item milledUraninite;
    /** F-01 byproduct: the barren rock the mill liberates. */
    public static Item gangueDust;
    /** F-02a out: pine-oil frother + soda ash, the reagent charge for one flotation run. */
    public static Item frothCell;
    /** F-02 out: the floated concentrate. */
    public static Item uraniniteConcentrate;
    /** F-02 byproduct: what sank. */
    public static Item flotationTailings;
    /** F-03 byproduct: the undissolved solids the digester leaves behind. */
    public static Item leachResidue;
    /** F-04 in (90% returned) and C-01 in: the ion-exchange bed itself. */
    public static Item ionExchangeResin;
    /** F-07 out: (NH4)2U2O7, the precipitate. */
    public static Item ammoniumDiuranate;
    /** F-08 out: U3O8, calcined at 800 C. The classic yellowcake. */
    public static Item yellowcake;

    // ------------------------------------------------------ F-11c, the tails as a real product
    /**
     * F-11c out: the enrichment tails reduced to metal with calcium. Dense, and therefore
     * shielding - which is why M-03b spends it on Tungstensteel Reinforced Blocks and the fuel
     * line ends up feeding the casing line.
     */
    public static Item depletedUraniumIngot;

    // ---------------------------------------------------- F-12..F-18, ceramics and fabrication
    /** F-12 out: THE WATER STEP's product. Count per rung is {@code BTSWaterGrade.powderFor}. */
    public static Item ceramicUO2Powder;
    /** F-13 out: cold-pressed, not yet fired. */
    public static Item greenPellet;
    /** F-14 out: fired at 1,800 K in hydrogen. F-15 grinds it into {@code uraniumFuelPellet}. */
    public static Item sinteredPellet;
    /** F-15 byproduct: centreless-grinding swarf, returned to F-13. */
    public static Item grinderSludge;
    /** F-16 out: pilgered from GoodGenerator's Zircaloy-4, the real PWR cladding alloy. */
    public static Item claddingTube;
    /** F-16b out: end plugs and the plenum spring, one set per rod. */
    public static Item rodHardwareSet;
    /** F-18 in: the bundle's grids and tie plates. */
    public static Item gridSpacer;

    // ------------------------------------------------------------ F-19..F-23, the other grades
    /** F-20 out: the tails reconverted. Flat output, NO water rung - S2.3 says why. */
    public static Item depletedUO2Powder;
    /** F-19 in / R-03 out: plutonium dioxide, the MOX driver and the reprocessing payoff. */
    public static Item plutoniumDioxide;
    /** F-19 out: the MIMAS masterblend, 6.25% PuO2 in depleted UO2. */
    public static Item moxPowder;
    /** F-21a out: thoria, calcined from GoodGenerator's thorium oxalate. */
    public static Item thoriumDioxidePowder;
    /** F-21b out: (Th,U)O2 - thoria with 6.25% of a fissile driver in it. */
    public static Item thoriumUraniumOxidePowder;
    /** F-13 out, MOX grade. */
    public static Item greenPelletMox;
    /** F-13 out, thorium grade. */
    public static Item greenPelletThorium;
    /** F-14 out, MOX grade. F-15 circuit 2 grinds it into {@code moxFuelPellet}. */
    public static Item sinteredPelletMox;
    /** F-14 out, thorium grade. F-15 circuit 3 grinds it into {@code thoriumBondedPellet}. */
    public static Item sinteredPelletThorium;
    /** F-15 out, thorium grade: the pellet F-17 loads into a thorium-bonded rod. */
    public static Item thoriumBondedPellet;
    /** F-22 out: naquadah chemical-vapour-infiltrated into a sintered matrix. The UHV step. */
    public static Item naqBondedPellet;

    // -------------------------------------------------------------- R-01..R-04, reprocessing
    /** R-01 out: a uranium assembly that has sat in the pond. Cooled fuel yields plutonium. */
    public static Item cooledAssemblyUranium;
    /** R-01 out, MOX. */
    public static Item cooledAssemblyMox;
    /** R-01 out, thorium-bonded. */
    public static Item cooledAssemblyThoriumBonded;
    /** R-01 out, naquadah-bonded. */
    public static Item cooledAssemblyNaqBonded;
    /** R-01 out, rod scale - the SD-1 cascade drops rods, not assemblies. */
    public static Item cooledRodUranium;
    /** R-01 out, rod scale, MOX. */
    public static Item cooledRodMox;
    /** R-01 out, rod scale, thorium-bonded. */
    public static Item cooledRodThoriumBonded;
    /** R-01 out, rod scale, naquadah-bonded. */
    public static Item cooledRodNaqBonded;
    /** R-02 out: the chopped, leached cladding. R-04 recovers the Zircaloy from it. */
    public static Item claddingHull;

    private BTSItemsFuel() {}

    public static void register() {
        milledUraninite = BTSItems.define("milledUraninite");
        gangueDust = BTSItems.define("gangueDust");
        frothCell = BTSItems.define("frothCell");
        uraniniteConcentrate = BTSItems.define("uraniniteConcentrate");
        flotationTailings = BTSItems.define("flotationTailings");
        leachResidue = BTSItems.define("leachResidue");
        ionExchangeResin = BTSItems.define("ionExchangeResin");
        ammoniumDiuranate = BTSItems.define("ammoniumDiuranate");
        yellowcake = BTSItems.define("yellowcake");

        depletedUraniumIngot = BTSItems.define("depletedUraniumIngot");

        ceramicUO2Powder = BTSItems.define("ceramicUO2Powder");
        greenPellet = BTSItems.define("greenPellet");
        sinteredPellet = BTSItems.define("sinteredPellet");
        grinderSludge = BTSItems.define("grinderSludge");
        claddingTube = BTSItems.define("claddingTube");
        rodHardwareSet = BTSItems.define("rodHardwareSet");
        gridSpacer = BTSItems.define("gridSpacer");

        depletedUO2Powder = BTSItems.define("depletedUO2Powder");
        plutoniumDioxide = BTSItems.define("plutoniumDioxide");
        moxPowder = BTSItems.define("moxPowder");
        thoriumDioxidePowder = BTSItems.define("thoriumDioxidePowder");
        thoriumUraniumOxidePowder = BTSItems.define("thoriumUraniumOxidePowder");
        greenPelletMox = BTSItems.define("greenPelletMox");
        greenPelletThorium = BTSItems.define("greenPelletThorium");
        sinteredPelletMox = BTSItems.define("sinteredPelletMox");
        sinteredPelletThorium = BTSItems.define("sinteredPelletThorium");
        thoriumBondedPellet = BTSItems.define("thoriumBondedPellet");
        naqBondedPellet = BTSItems.define("naqBondedPellet");

        cooledAssemblyUranium = BTSItems.define("cooledAssemblyUranium");
        cooledAssemblyMox = BTSItems.define("cooledAssemblyMox");
        cooledAssemblyThoriumBonded = BTSItems.define("cooledAssemblyThoriumBonded");
        cooledAssemblyNaqBonded = BTSItems.define("cooledAssemblyNaqBonded");
        cooledRodUranium = BTSItems.define("cooledRodUranium");
        cooledRodMox = BTSItems.define("cooledRodMox");
        cooledRodThoriumBonded = BTSItems.define("cooledRodThoriumBonded");
        cooledRodNaqBonded = BTSItems.define("cooledRodNaqBonded");
        claddingHull = BTSItems.define("claddingHull");
    }
}
