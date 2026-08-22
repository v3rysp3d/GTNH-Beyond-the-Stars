package com.nateyburr.beyondthestars.items;

import net.minecraft.item.Item;

/**
 * D-26 PROPELLANT LINE ITEMS (S4.1). Owned by <b>Agent C (Propellant + Coolant)</b>.
 * <p>
 * P-04 only: the gelled cartridges are a STORAGE AND LOGISTICS form, not a new rung. Each holds
 * 1,000 L of its own ladder fluid and is re-liquefied in a Fluid Extractor, so a cartridge is
 * worth exactly the fluid that went into it - the P values of D-08 are untouched, and a cartridge
 * is not something the Spiral chamber can be fed directly.
 */
public final class BTSItemsPropellant {

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
        propellantCartridge1 = BTSItems.define("propellantCartridge1");
        propellantCartridge2 = BTSItems.define("propellantCartridge2");
        propellantCartridge3 = BTSItems.define("propellantCartridge3");
        propellantCartridge4 = BTSItems.define("propellantCartridge4");
        propellantCartridge5 = BTSItems.define("propellantCartridge5");
        propellantCartridge6 = BTSItems.define("propellantCartridge6");
    }
}
