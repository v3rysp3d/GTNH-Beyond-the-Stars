package com.nateyburr.beyondthestars.integration.oc;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrModuleBase;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import li.cil.oc.api.driver.SidedBlock;
import li.cil.oc.api.network.ManagedEnvironment;

/**
 * AUTOMATION PASS: the OpenComputers block driver for the ladder's controllers and hatches. An Adapter against the
 * block gets the matching component ({@link BTSEnvironments}); every other GT machine is left to OpenComputers' own
 * drivers.
 */
public final class BTSDriver implements SidedBlock {

    static IMetaTileEntity mteAt(World world, int x, int y, int z) {
        if (world == null || !world.blockExists(x, y, z)) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity() : null;
    }

    @Override
    public boolean worksWith(World world, int x, int y, int z, ForgeDirection side) {
        final IMetaTileEntity mte = mteAt(world, x, y, z);
        return mte instanceof MTEEyeOfHarmonyBTS || mte instanceof MTEKerrEngine
            || mte instanceof MTEKerrModuleBase
            || mte instanceof MTEHatchStellarFieldSensor
            || mte instanceof MTEHatchDirective
            || mte instanceof com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine
            || mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement
            || mte instanceof com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown
            || mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor
            || mte instanceof com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny
            || mte instanceof com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction
            || mte instanceof com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
        final IMetaTileEntity mte = mteAt(world, x, y, z);
        if (mte instanceof MTEEyeOfHarmonyBTS) return new BTSEnvironments.Eye(world, x, y, z);
        if (mte instanceof com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector) {
            return new BTSEnvironments.Injector(world, x, y, z);
        }
        if (mte instanceof MTEKerrEngine) return new BTSEnvironments.Kerr(world, x, y, z);
        if (mte instanceof MTEKerrModuleBase) return new BTSEnvironments.KerrModule(world, x, y, z);
        if (mte instanceof MTEHatchStellarFieldSensor) return new BTSEnvironments.Sensor(world, x, y, z);
        if (mte instanceof MTEHatchDirective) return new BTSEnvironments.Directive(world, x, y, z);
        if (mte instanceof com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine) {
            return new BTSEnvironments.Quasar(world, x, y, z);
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction) {
            return new BTSEnvironments.Junction(world, x, y, z);
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEWormholeMouth) {
            return new BTSEnvironments.Conduit(world, x, y, z, "bts_mouth");
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement) {
            return new BTSEnvironments.Conduit(world, x, y, z, "bts_mirror");
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown) {
            return new BTSEnvironments.Crown(world, x, y, z);
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor) {
            return new BTSEnvironments.Receptor(world, x, y, z);
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny) {
            return new BTSEnvironments.Homogeny(world, x, y, z);
        }
        if (mte instanceof com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction) {
            return new BTSEnvironments.Reality(world, x, y, z);
        }
        return null;
    }
}
