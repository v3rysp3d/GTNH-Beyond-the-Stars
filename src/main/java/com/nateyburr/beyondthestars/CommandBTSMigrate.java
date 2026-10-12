package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

/**
 * /btsmigrate - brings a plant built by 0.19.0 or earlier onto the point-of-use silo face,
 * without taking anything down.
 * <p>
 * The relocation moved the silo's sockets onto the courses they enter the reactor's logic flow
 * on. The old face and the new one CONFLICT cell for cell - each puts a hatch where the other
 * wants wall - so a legacy silo fails every one of the five tier probes and no amount of
 * chaining or tolerance in the structure check can rescue it. The honest fix is to rewrite the
 * face, which is exactly thirteen cells, and to say so out loud rather than let the plant sit
 * there reporting FORMED with an empty error panel.
 * <p>
 * What it touches: the silo's front row, bottom three courses, at the tier read off the STANDING
 * WALLS (never a remembered number, never the GUI Build Tier). Cells that are already right are
 * left alone; wrong ones drop their contents as items at the wall before the correct casing or
 * socket goes in, so every hatch the player owned - and everything inside it - comes back. The
 * shell, the ribs, the dome, the fuel pillars and the rods standing in them are never read and
 * never written.
 */
public class CommandBTSMigrate extends CommandBase {

    @Override
    public String getCommandName() {
        return "btsmigrate";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsmigrate";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final MTEReactorCoreMk1 station = BTSCommands.nearestStation(player);
        if (station == null) {
            BTSCommands.noStation(player);
            return;
        }

        final int standing = station.standingSiloTier();
        if (standing <= 0) {
            BTSChat.warn(
                player,
                "No Reactor Silo is standing on " + BTSCommands.plantAt(station)
                    + ", so there is no face to migrate. A bare platform just needs projecting: set Build Tier in the controller GUI first.");
            return;
        }

        final int[] result = station.migrateSiloFace();
        final int tier = result[0];
        final int rewritten = result[1];
        final int salvaged = result[2];
        station.checkStructure(true, station.getBaseMetaTileEntity());

        BTSChat.header(player, "== Silo face migrated to the point-of-use layout ==");
        BTSChat.send(
            player,
            "Tier " + tier
                + " silo, measured off the standing walls: "
                + rewritten
                + " face cells rewritten, "
                + salvaged
                + " machines dropped at the wall for you to pick up.");
        BTSChat.send(
            player,
            "The face now holds: Maintenance Hatch beside the controller; Silo Control Block above it, Feedwater Input Hatch to its left, Output Hatch to its right; Rod Input Bus and Depleted Output Bus one row higher.");
        BTSChat.warn(
            player,
            "Energy Hatches no longer go in this wall: their sockets are on the output manifold's back face (5th row up). The plant forms without them; the Extraction Derrick and Enrichment Annex draw their EU through them.");
        if (station.mMachine) {
            BTSChat.ok(
                player,
                "The plant forms: " + station.buildSummary()
                    + ". Refit the hatches you picked up; the ones in the face are new and empty.");
        } else {
            BTSChat.warn(
                player,
                "Still not formed - the face is correct now, so the fault is elsewhere. Open the Nuclear Plant Foundation and read Structure Errors: it names the first wrong cell.");
        }
    }
}
