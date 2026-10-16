package com.nateyburr.beyondthestars;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Shared plumbing for the plant's bench commands (/btsclock, /btssupply, /btssafety,
 * /btsmigrate, /btsmilestone, /btstree).
 */
final class BTSCommands {

    static final int SEARCH_RADIUS = 64;

    private BTSCommands() {}

    /**
     * The nearest Nuclear Plant Foundation to the player, or null. Walks the world's loaded tile
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

    /** The one "no plant nearby" reply every plant command gives. */
    static void noStation(ICommandSender to) {
        BTSChat
            .error(to, "No Nuclear Plant Foundation within " + SEARCH_RADIUS + " blocks (its chunk must be loaded).");
    }

    /** "the plant at (x, y, z)", so a reply names which controller it changed. */
    static String plantAt(MTEReactorCoreMk1 station) {
        final IGregTechTileEntity base = station.getBaseMetaTileEntity();
        if (base == null) return "the plant";
        return "the plant at (" + base.getXCoord() + ", " + base.getYCoord() + ", " + base.getZCoord() + ")";
    }

    /**
     * The bench toggles' one argument: {@code on}, {@code off}, or nothing to flip the current
     * state. Anything else is a usage error, not a silent "off".
     */
    static boolean parseToggle(ICommandSender sender, CommandBase command, String[] args, boolean current) {
        if (args.length == 0) return !current;
        if (args.length == 1) {
            if ("on".equalsIgnoreCase(args[0])) return true;
            if ("off".equalsIgnoreCase(args[0])) return false;
        }
        throw new WrongUsageException(command.getCommandUsage(sender));
    }

    /**
     * B-248: a server with {@code enableTestCommands=false} registers no bench command, and the two that also carry an
     * admin verb (/btskerr migrate, /btseye gates / bec) refuse everything else. True (and the sender told what is
     * left) when the call must stop; {@code left} names the forms that still run.
     */
    static boolean testCommandsOff(ICommandSender sender, String left) {
        if (Config.enableTestCommands) return false;
        BTSChat.error(
            sender,
            "The test commands are off on this server (enableTestCommands=false in config/beyondthestars.cfg)."
                + " Still available: "
                + left
                + ".");
        return true;
    }

    /** Tab completion for the bench toggles. */
    static List<String> toggleCompletions(String[] args) {
        if (args.length == 1) return CommandBase.getListOfStringsMatchingLastWord(args, "on", "off");
        return null;
    }
}
