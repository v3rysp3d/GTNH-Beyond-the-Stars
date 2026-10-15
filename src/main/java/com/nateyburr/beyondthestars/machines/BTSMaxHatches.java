package com.nateyburr.beyondthestars.machines;

import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;

import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;

/**
 * P5 MAX power hatches (p5-t9-t10.md §7.2). RC-2 has MAX wireless energy and dynamo hatches but no wired MAX energy
 * or dynamo hatch and no MAX laser hatch, so the four the spec prices are GT's and TecTech's own hatch classes at the
 * MAX tier (14), on four of the addon's spare ids (question B-204): base+33 energy, base+34 dynamo, base+36 the
 * 4,096 A laser target, base+37 the 4,096 A laser source. base+35 stays reserved (SMBH §11). Called from
 * {@code CommonProxy.preInit} after {@link BTSMachines#register()}. Recipes: {@code BTSRecipesP5}.
 */
public final class BTSMaxHatches {

    /** The MAX tier: GT's {@code V[14]} = 2,147,483,640 EU/t a amp. */
    public static final int TIER = 14;
    /** The laser hatches' amperage (the spec's 4,096 A). */
    public static final int LASER_AMPS = 4_096;

    public static ItemStack energyHatchMAX;
    public static ItemStack dynamoHatchMAX;
    public static ItemStack laserTargetMAX;
    public static ItemStack laserSourceMAX;

    private BTSMaxHatches() {}

    public static void register() {
        final int base = Config.mteBaseId;
        energyHatchMAX = new MTEHatchEnergy(base + 33, "hatch.beyondthestars.energy.max", "MAX Energy Hatch", TIER)
            .getStackForm(1);
        dynamoHatchMAX = new MTEHatchDynamo(base + 34, "hatch.beyondthestars.dynamo.max", "MAX Dynamo Hatch", TIER)
            .getStackForm(1);
        laserTargetMAX = new MTEHatchEnergyTunnel(
            base + 36,
            "hatch.beyondthestars.lasertarget.max",
            "MAX 4096A Laser Target Hatch",
            TIER,
            LASER_AMPS).getStackForm(1);
        laserSourceMAX = new MTEHatchDynamoTunnel(
            base + 37,
            "hatch.beyondthestars.lasersource.max",
            "MAX 4096A Laser Source Hatch",
            TIER,
            LASER_AMPS).getStackForm(1);
        BeyondTheStars.LOG.info(
            "[BTS] P5 MAX hatches registered: energy {}, dynamo {}, laser target {}, laser source {}.",
            base + 33,
            base + 34,
            base + 36,
            base + 37);
    }
}
