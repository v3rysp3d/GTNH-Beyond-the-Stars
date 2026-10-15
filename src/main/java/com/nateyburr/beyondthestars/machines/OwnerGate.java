package com.nateyburr.beyondthestars.machines;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;

/**
 * Who may change a machine (QA-B15): its owner (the player who placed the controller), anyone on the owner's GT team
 * (the space-project team, as wireless EU uses), and server operators. A machine with no owner (placed by a console
 * bench) takes everyone, and so does a call with no player (OpenComputers, Directive and Burst Control automation:
 * the owner wired those). Server side.
 */
public final class OwnerGate {

    private OwnerGate() {}

    public static boolean mayControl(IGregTechTileEntity base, EntityPlayer player) {
        if (base == null || player == null) return true;
        final UUID owner = base.getOwnerUuid();
        if (owner == null) return true;
        final UUID me = player.getUniqueID();
        if (owner.equals(me)) return true;
        if (player instanceof EntityPlayerMP && ((EntityPlayerMP) player).mcServer != null
            && ((EntityPlayerMP) player).mcServer.getConfigurationManager()
                .func_152596_g(((EntityPlayerMP) player).getGameProfile()))
            return true;
        try {
            final UUID a = SpaceProjectManager.getLeader(owner), b = SpaceProjectManager.getLeader(me);
            return a != null && a.equals(b);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** {@link #mayControl}, and if not, tells the player whose machine it is. */
    public static boolean check(IGregTechTileEntity base, EntityPlayer player, String what) {
        if (mayControl(base, player)) return true;
        final String owner = base.getOwnerName();
        player.addChatComponentMessage(
            new ChatComponentText(
                EnumChatFormatting.RED + "Only "
                    + (owner == null || owner.isEmpty() ? "its owner" : owner)
                    + " or their team can "
                    + what
                    + " here."));
        return false;
    }
}
