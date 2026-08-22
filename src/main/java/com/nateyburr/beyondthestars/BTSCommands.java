package com.nateyburr.beyondthestars;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Shared plumbing for the test-bench commands (/btsclock, /btssupply, /btssafety). */
final class BTSCommands {

    static final int SEARCH_RADIUS = 64;

    private BTSCommands() {}

    /**
     * The nearest Unit One main station to the player, or null. Walks the world's loaded tile
     * entities rather than probing half a million block positions.
     */
    static MTEReactorCoreMk1 nearestStation(EntityPlayerMP player) {
        MTEReactorCoreMk1 nearest = null;
        double bestDist = SEARCH_RADIUS * (double) SEARCH_RADIUS;
        for (final Object o : new java.util.ArrayList<Object>(player.worldObj.loadedTileEntityList)) {
            if (!(o instanceof IGregTechTileEntity)) continue;
            final IGregTechTileEntity gte = (IGregTechTileEntity) o;
            if (!(gte.getMetaTileEntity() instanceof MTEReactorCoreMk1)) continue;
            final TileEntity te = (TileEntity) o;
            final double d = player.getDistanceSq(te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);
            if (d < bestDist) {
                bestDist = d;
                nearest = (MTEReactorCoreMk1) gte.getMetaTileEntity();
            }
        }
        return nearest;
    }
}
