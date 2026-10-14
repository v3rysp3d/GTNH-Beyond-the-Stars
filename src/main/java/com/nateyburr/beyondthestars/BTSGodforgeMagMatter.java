package com.nateyburr.beyondthestars;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import tectech.thing.metaTileEntity.multi.godforge.upgrade.ForgeOfGodsUpgrade;

/**
 * SL-Q36 (B-153, P4-24's suggestion; smbh-v1.md §1.3's audit): MagMatter before Eye T10. One owner. Called from
 * {@code CommonProxy.loadComplete}, only with {@code replaceTecTechEyeOfHarmony} on.
 * <p>
 * <b>The loop.</b> MagMatter's only source is the Godforge's Heliofusion Exoticizer in magmatter mode, which needs the
 * Godforge upgrade EE. EE's extra cost (TecTech's {@code Godforge} loader) holds 2 Universium frame boxes, and with the
 * flag on Universium exists only at our Eye T10. But the Supermassive Link (P5's Eye T9 -> T10 milestone) needs a
 * Supermassive Lattice, the lattice needs Saturation, and Saturation needs MagMatter (T4) Disc Field Coils and the
 * lattice's MagMatter frames: T10 needs MagMatter, and MagMatter needed T10.
 * <p>
 * <b>The fix.</b> The 2 Universium frame boxes in EE's cost become 8 Black Dwarf Matter frame boxes (Eye T5).
 * EE's other costs stand: 16 Eternity frames (Eye T7 through SL-Q26) are its newest product, so MagMatter, the T4
 * coils, Saturation and the lattice arrive with Eternity at T7, before the T10 gate. The stack is changed in place:
 * TecTech's upgrade list and its NEI page ({@code godforgeFakeUpgradeCostRecipes}, built from the same stacks) both
 * show the new cost. With the flag off TecTech's Eye makes Universium and nothing changes.
 */
public final class BTSGodforgeMagMatter {

    private BTSGodforgeMagMatter() {}

    /** Black Dwarf Matter frame boxes in place of the 2 Universium ones. */
    public static final int BLACK_DWARF_FRAMES = 8;

    private static boolean applied;

    /** True once the EE cost has been rewritten this run (the gate audit's MagMatter tier: T7, else T10). */
    public static boolean applied() {
        return applied;
    }

    public static void onLoadComplete() {
        if (!Config.replaceTecTechEyeOfHarmony) return;
        final ItemStack universium = GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Universium, 1);
        final ItemStack blackDwarf = GTOreDictUnificator
            .get(OrePrefixes.frameGt, Materials.BlackDwarfMatter, BLACK_DWARF_FRAMES);
        if (universium == null || blackDwarf == null || blackDwarf.getItem() == null) {
            BeyondTheStars.LOG.error(
                "[BTS] SL-Q36: no Universium or Black Dwarf Matter frame box in this pack; the Godforge EE upgrade is "
                    + "unchanged, so MagMatter (and Saturation, the Supermassive Lattice) still waits for Eye T10.");
            return;
        }
        int changed = 0;
        for (ItemStack cost : ForgeOfGodsUpgrade.EE.getExtraCost()) {
            if (cost == null || !GTUtility.areStacksEqual(cost, universium, true)) continue;
            final int was = cost.stackSize;
            cost.func_150996_a(blackDwarf.getItem());
            cost.setItemDamage(blackDwarf.getItemDamage());
            cost.setTagCompound(blackDwarf.hasTagCompound() ? blackDwarf.getTagCompound() : null);
            cost.stackSize = BLACK_DWARF_FRAMES;
            changed++;
            BeyondTheStars.LOG.info(
                "[BTS] SL-Q36: the Godforge EE upgrade (magmatter mode) costs {} Black Dwarf Matter frame boxes in place"
                    + " of {} Universium ones: MagMatter arrives with Eternity at Eye T7, before the T9 -> T10"
                    + " Supermassive Link that needs it (B-153).",
                BLACK_DWARF_FRAMES,
                was);
        }
        applied = changed > 0;
        if (!applied) {
            BeyondTheStars.LOG.warn(
                "[BTS] SL-Q36: the Godforge EE upgrade has no Universium frame box in its cost in this pack (without "
                    + "Eternal Singularity TecTech loads a dev cost); nothing was changed.");
        }
    }
}
