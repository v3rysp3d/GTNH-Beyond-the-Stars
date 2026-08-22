package com.nateyburr.beyondthestars;

/**
 * D-26 THE COOLANT LINE - bulk manufacture only (docs/production-lines.md S4.2). <b>Owned
 * exclusively by Agent C (Propellant + Coolant).</b>
 * <p>
 * Empty at the split commit: no coolant route existed before 0.24.0. C-01 to C-04 belong here.
 * <p>
 * <b>The scope statement, said plainly.</b> The coolant ladder is a settled D-08 table of six
 * EXISTING pack fluids. This line makes those fluids at plant scale and does nothing else -
 * inventing a seventh rung or moving a Q would be relitigating a ruling, not implementing one.
 * C-04 chemical shim is a boric-acid / lithium-hydroxide corrosion inhibitor consumed by C-02 and
 * C-03; it is never fed to the reactor and is not a coolant.
 * <p>
 * <b>Explicitly out of scope</b> and flagged for a future ruling: a coolant RECOVERY loop (hot
 * coolant out of the Spiral chamber, chilled, returned). It would be excellent and it is how a
 * real plant works, but it needs {@code MTEReactorCoreMk1} chamber drain to emit a fluid, which
 * is a reactor-behaviour change and not a recipe.
 */
public final class BTSRecipesCoolant {

    private BTSRecipesCoolant() {}

    public static void register() {
        // Nothing yet - see the class comment for what belongs here.
    }
}
