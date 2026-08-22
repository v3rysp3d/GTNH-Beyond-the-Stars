package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

/**
 * D-26 PROPELLANT LINE ITEMS (S4.1). Owned by <b>Agent C (Propellant + Coolant)</b>.
 * <p>
 * <b>P-04 cartridges</b> are a STORAGE AND LOGISTICS form, not a new rung. Each holds 1,000 L of
 * its own ladder fluid and is re-liquefied in a Fluid Extractor, so a cartridge is worth exactly
 * the fluid that went into it - the P values of D-08 are untouched, and a cartridge is not
 * something the Spiral chamber can be fed directly.
 * <p>
 * <b>P-02a ultrafine powders: why these exist at all.</b> S4.1 wanted ONE milling step -
 * "8 dust metal + Milling Ball + 1,000 L Kerosene -> 2,000 L Metal Additive Slurry" on
 * {@code GTPPRecipeMaps.millingRecipes}. That recipe cannot be registered. The Ore Milling Plant's
 * map is built {@code maxIO(3, 1, 0, 0)} - three item inputs, one item output, and <b>zero fluid
 * slots in either direction</b>. Kerosene has nowhere to go, so the single step had to become two:
 * P-02a mills dry on the IsaMill (items only), P-02b takes the powder into kerosene on the
 * Chemical Plant, which does have fluid slots. That split is also the more honest process -
 * ultrafine dry milling and slurry blending are genuinely two unit operations.
 * <p>
 * <b>Why three powders and not one.</b> Same rule the fuel line's green pellets are built on: a
 * circuit cannot survive an item. One fungible "Ultrafine Metal Powder" would let cheap aluminium
 * be milled and then blended on circuit 3 into an Infinity-grade slurry, laundering the tier gate
 * through an item that has forgotten what it is made of. The powder carries the METAL; the recipe
 * carries the rung.
 */
public final class BTSItemsPropellant {

    /** P-02a out, Lv 1-2 feedstock: aluminium milled to propellant-additive fineness. */
    public static Item ultrafineAluminium;
    /** P-02a out, Lv 3-4 feedstock. */
    public static Item ultrafineAdamantium;
    /** P-02a out, Lv 5-6 feedstock. */
    public static Item ultrafineInfinity;

    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT1}, gelled. */
    public static Item propellantCartridge1;
    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT2}, gelled. */
    public static Item propellantCartridge2;
    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT3}, gelled. */
    public static Item propellantCartridge3;
    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT4}, gelled. */
    public static Item propellantCartridge4;
    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT5}, gelled. */
    public static Item propellantCartridge5;
    /** P-04: 1,000 L of {@code BTSFluids.naqPropellantT6}, gelled. */
    public static Item propellantCartridge6;

    private BTSItemsPropellant() {}

    public static void register() {
        ultrafineAluminium = BTSItems.define("ultrafineAluminium");
        ultrafineAdamantium = BTSItems.define("ultrafineAdamantium");
        ultrafineInfinity = BTSItems.define("ultrafineInfinity");

        propellantCartridge1 = BTSItems.define("propellantCartridge1");
        propellantCartridge2 = BTSItems.define("propellantCartridge2");
        propellantCartridge3 = BTSItems.define("propellantCartridge3");
        propellantCartridge4 = BTSItems.define("propellantCartridge4");
        propellantCartridge5 = BTSItems.define("propellantCartridge5");
        propellantCartridge6 = BTSItems.define("propellantCartridge6");
    }
}
