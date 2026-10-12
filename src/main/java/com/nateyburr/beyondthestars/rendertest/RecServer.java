package com.nateyburr.beyondthestars.rendertest;

import java.lang.reflect.Field;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;

import gregtech.api.enums.ItemList;

/**
 * DEV-ONLY video recorder ({@link RecClient}), the server half: the script commands that change the world. Every
 * method runs on the integrated server's thread ({@link RenderTest#onServer}).
 */
final class RecServer {

    private RecServer() {}

    /** The controller the last {@code place} put down (the {@code placed} anchor). */
    static volatile int[] placed;

    /**
     * A GT machine's item: {@code ItemList.<name>} (GT's list, e.g. {@code ItemList.Machine_Multi_BlastFurnace}) or
     * {@code bts.<field>} (this addon's {@link BTSMachines}, e.g. {@code bts.eyeOfHarmony}).
     */
    static ItemStack machine(String spec) {
        if (spec.startsWith("ItemList.")) {
            return ItemList.valueOf(spec.substring("ItemList.".length()))
                .get(1L);
        }
        if (spec.startsWith("bts.")) {
            try {
                final Field f = BTSMachines.class.getField(spec.substring(4));
                final Object v = f.get(null);
                return v instanceof ItemStack ? ((ItemStack) v).copy() : null;
            } catch (ReflectiveOperationException bad) {
                throw new IllegalArgumentException("no BTSMachines." + spec.substring(4));
            }
        }
        throw new IllegalArgumentException("machine must be ItemList.<name> or bts.<field>: " + spec);
    }

    static ForgeDirection facing(String name) {
        try {
            return ForgeDirection.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException bad) {
            return ForgeDirection.NORTH;
        }
    }

    /** Places a GT machine's controller at {@code p} facing {@code facing}; it becomes the {@code placed} anchor. */
    static String place(String spec, int[] p, ForgeDirection facing) {
        final ItemStack stack = machine(spec);
        if (stack == null) return "no item for " + spec;
        final WorldServer world = RenderTest.world();
        RenderTest.loadAndKeep(world, new int[] { p[0] - 48, 0, p[2] - 48, p[0] + 48, 255, p[2] + 48 });
        final boolean ok = MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing);
        if (ok) placed = p.clone();
        return (ok ? "placed " : "COULD NOT place ") + spec + " at " + RenderTest.pos(p) + " facing " + facing;
    }

    /**
     * Builds the multiblock whose controller stands at {@code p} the way a Hologram Projector does when a creative
     * player sneak-uses it ({@code IConstructable.construct(stack, false)}); {@code tier} is the projector's stack
     * size.
     */
    static String construct(int[] p, int tier) {
        final WorldServer world = RenderTest.world();
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        IConstructable c = null;
        if (te instanceof IConstructableProvider) c = ((IConstructableProvider) te).getConstructable();
        else if (te instanceof IConstructable) c = (IConstructable) te;
        if (c == null) return "nothing constructable at " + RenderTest.pos(p);
        c.construct(new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(1, tier)), false);
        return "built the multiblock at " + RenderTest.pos(p) + " (projector tier " + tier + ")";
    }

    /** Runs a server command as the player (op in the integrated world). */
    static String command(String cmd) {
        final EntityPlayerMP player = RenderTest.player();
        if (player == null) return "no player";
        final int r = MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(player, cmd.startsWith("/") ? cmd.substring(1) : cmd);
        return "command '" + cmd + "' -> " + r;
    }

    /**
     * {@code day} 6000, {@code noon} 6000, {@code sunset} 12000, {@code night} 18000, {@code sunrise} 23000 or ticks.
     */
    static String time(String when) {
        final long t;
        switch (when.toLowerCase(Locale.ROOT)) {
            case "day":
            case "noon":
                t = 6000;
                break;
            case "sunset":
                t = 12000;
                break;
            case "night":
            case "midnight":
                t = 18000;
                break;
            case "sunrise":
                t = 23000;
                break;
            default:
                t = Long.parseLong(when);
                break;
        }
        RenderTest.world()
            .setWorldTime(t);
        return "time " + t;
    }

    /** The daylight cycle on (a time-lapse's moving sky) or off (the recorder's default). */
    static String daylight(boolean on) {
        RenderTest.world()
            .getGameRules()
            .setOrCreateGameRule("doDaylightCycle", String.valueOf(on));
        return "daylight cycle " + (on ? "on" : "off");
    }
}
