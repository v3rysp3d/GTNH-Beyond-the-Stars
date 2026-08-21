package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
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

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        final World world = player.worldObj;
        if (world.isRemote) return;

        final MTEReactorCoreMk1 station = BTSCommands.nearestStation(player);
        if (station == null) {
            tell(
                player,
                EnumChatFormatting.RED + "No Unit One main station within "
                    + BTSCommands.SEARCH_RADIUS
                    + " blocks. Stand beside the controller embedded in the silo face.");
            return;
        }

        final int standing = station.standingSiloTier();
        if (standing <= 0) {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "No Steam Generator shell is standing on that station's pad, so there is no face to migrate. A bare platform just needs projecting - set Build Tier in the controller GUI first.");
            return;
        }

        final int[] result = station.migrateSiloFace();
        final int tier = result[0];
        final int rewritten = result[1];
        final int salvaged = result[2];
        station.checkStructure(true, station.getBaseMetaTileEntity());

        tell(player, EnumChatFormatting.AQUA + "== Silo face migrated to the point-of-use layout ==");
        tell(
            player,
            "Tier " + tier
                + " shell measured off the standing walls. "
                + rewritten
                + " face cells rewritten, "
                + salvaged
                + " machines dropped at the wall for you to pick up.");
        tell(
            player,
            "The face now reads, up from the pad: y0 Maintenance | MAIN STATION - y1 Feedwater | SILO CONTROL BLOCK | Output Hatch - y2 Rod Input Bus | Depleted Output Bus, all within one cell of centre.");
        tell(
            player,
            EnumChatFormatting.YELLOW
                + "The four ENERGY HATCHES this wall used to carry belong on the OUTPUT MANIFOLD's back face now (y4, between the dynamos and the laser sources). They are not a structure demand - the reactor and turbine never draw EU - so the plant forms without them. Fit them anyway if you build an Extraction Derrick or an Enrichment Annex: those two draw their power through these sockets and will not run without one.");
        if (station.mMachine) {
            tell(
                player,
                EnumChatFormatting.GREEN + "The plant FORMS: "
                    + station.buildSummary()
                    + ". Refit the hatches you just picked up wherever you want them; the ones in the face are fresh and empty.");
        } else {
            tell(
                player,
                EnumChatFormatting.YELLOW
                    + "Still not formed - the FACE is correct now, so whatever is left is elsewhere. Open the main station and read the STRUCTURE ERRORS panel: it names the first wrong cell and its coordinates.");
        }
    }
}
