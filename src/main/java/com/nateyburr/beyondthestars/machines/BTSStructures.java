package com.nateyburr.beyondthestars.machines;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

/**
 * D-26 AXIS 4: the shared structure-element guard for the four new production multiblocks.
 * <p>
 * {@link MTEReactorCoreMk1} already carries this rule as a private helper bound to its own type.
 * The four new machines of S5 need exactly the same rule and cannot borrow it, so it lives here
 * once, generically, rather than being pasted four more times.
 * <p>
 * <b>The rule, and why it exists.</b> A plain {@code ofBlock} / {@code isAir} element's
 * {@code placeBlock} writes into the cell unconditionally. If a player has an Energy Hatch, a
 * chest or another machine standing in a cell the projection wants as casing, the hologram build
 * PAVES OVER IT and drops nothing - the tile entity is simply gone. {@code check} and
 * {@code spawnHint} pass straight through, so the hologram still draws the block the cell wants
 * and the structure check still fails loudly and names it; only the destructive WRITE is refused.
 * <p>
 * Wrap every plain-material element of a new structure in this. Hatch adders built with GT's
 * {@code buildHatchAdder} do not need it - they already refuse to overwrite - but the casing they
 * chain to does, and {@code ofChain} returns at the FIRST fallback whose {@code placeBlock}
 * answers true, so the casing half is exactly the half that would do the damage.
 */
public final class BTSStructures {

    private BTSStructures() {}

    /** True when something with a tile entity is standing in this cell. */
    public static boolean standsOnTile(World world, int x, int y, int z) {
        return world.getTileEntity(x, y, z) != null;
    }

    /** Delegate everything, but never WRITE into a cell that holds a tile entity. */
    public static <T> IStructureElement<T> noOverwrite(IStructureElement<T> base) {
        return new IStructureElement<T>() {

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return base.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return base.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public List<String> getDescription(T t) {
                return base.getDescription(t);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (standsOnTile(world, x, y, z)) return true;
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }
}
