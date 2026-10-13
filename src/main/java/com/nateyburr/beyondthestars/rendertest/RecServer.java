package com.nateyburr.beyondthestars.rendertest;

import java.lang.reflect.Field;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrSlot;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

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

    /**
     * Places a GT machine's controller at {@code p} facing {@code facing}; it becomes the {@code placed} anchor (and
     * the place {@code name}, if given).
     */
    static String place(String spec, int[] p, ForgeDirection facing, String name) {
        final String r = place(spec, p, facing);
        if (name != null && placed != null) name(name, p, null);
        return r;
    }

    /** A named place ({@code name}: the controller block; {@code name.c}: the machine's centre, if given). */
    static void name(String name, int[] p, double[] centre) {
        AutoRig.POINTS.put(name.toLowerCase(Locale.ROOT), AutoRigEyeAE.ctr(p));
        if (centre != null) AutoRig.POINTS.put(name.toLowerCase(Locale.ROOT) + ".c", centre);
    }

    /**
     * A whole machine by its dev kit, as {@code /btseye} and {@code /btskerr} raise it: {@code eye} (tier 1-6,
     * generators at {@code gen} 0-8, every hatch fitted, unlocked to the tier, formed) or {@code kerr} (T1 or T2 with
     * both modules). Dark / closed: nothing is lit. It becomes the {@code placed} anchor (and {@code name}, if given).
     */
    static String kit(String what, int tier, int[] p, ForgeDirection facing, int gen, String name) {
        final WorldServer world = RenderTest.world();
        RenderTest.loadAndKeep(world, new int[] { p[0] - 100, 0, p[2] - 100, p[0] + 100, 255, p[2] + 160 });
        final StringBuilder out = new StringBuilder();
        double[] centre = null;
        if (what.equals("eye")) {
            final EyeKit.Result r = EyeKit.build(
                world,
                p[0],
                p[1],
                p[2],
                facing,
                Math.max(1, Math.min(6, tier)),
                gen,
                false,
                AutoRig.OWNER,
                "BTS rec",
                false);
            out.append(r.failure != null ? "Eye kit FAILED: " + r.failure : "Eye kit T" + tier);
            for (String l : r.lines) out.append(" | ")
                .append(RenderTest.strip(l));
            final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(world, p, MTEEyeOfHarmonyBTS.class);
            if (eye != null) {
                final int[] c = com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender
                    .centrePos(eye.getExtendedFacing(), p[0], p[1], p[2]);
                centre = AutoRigEyeAE.ctr(c);
            }
        } else if (what.equals("kerr")) {
            final KerrKit.Result r = KerrKit
                .build(world, p[0], p[1], p[2], facing, tier >= 2, false, AutoRig.OWNER, "BTS rec");
            out.append(r.failure != null ? "Kerr kit FAILED: " + r.failure : "Kerr kit T" + (tier >= 2 ? 2 : 1));
            for (String l : r.lines) out.append(" | ")
                .append(RenderTest.strip(l));
            final MTEKerrEngine kee = AutoRig.mteAt(world, p, MTEKerrEngine.class);
            if (kee != null) {
                centre = AutoRigEyeAE
                    .ctr(KerrSlot.worldPos(kee.getExtendedFacing(), p[0], p[1], p[2], KerrSlot.centreRel()));
            }
        } else {
            return "kit eye|kerr, not " + what;
        }
        placed = p.clone();
        if (name != null) name(name, p, centre);
        return out.toString();
    }

    /**
     * One round of the Hologram Projector's survival auto-place on the multiblock at {@code p}
     * ({@code survivalConstruct(projector of stack size tier, budget, env)}), drawing on an endless item source as a
     * creative player's projector does: returns the elements placed this round, -1 when everything stands, -2 if
     * nothing survival-constructable is there.
     */
    static int survivalRound(int[] p, int tier, int budget) {
        final WorldServer world = RenderTest.world();
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        Object c = null;
        if (te instanceof IGregTechTileEntity) c = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(c instanceof ISurvivalConstructable) && te instanceof IConstructableProvider)
            c = ((IConstructableProvider) te).getConstructable();
        if (!(c instanceof ISurvivalConstructable)) return -2;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment
            .create(SLConsoleTest.endlessSource(), FakePlayerFactory.getMinecraft(world));
        return ((ISurvivalConstructable) c).survivalConstruct(
            new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(1, tier)),
            Math.max(1, budget),
            env);
    }

    /**
     * A condition the recorder can wait for: {@code eyelit}, {@code holeopen}, {@code aelit}, {@code aedark},
     * {@code formed} (the {@code placed} machine).
     */
    static boolean cond(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "eyelit":
                return AutoRig.eyeLit();
            case "holeopen":
                return AutoRig.kee() != null && AutoRig.kee()
                    .isHoleOpen();
            case "aelit":
                return AutoRigEyeAE.lit();
            case "aedark":
                return !AutoRigEyeAE.lit();
            case "formed": {
                if (placed == null) return false;
                final TileEntity te = RenderTest.world()
                    .getTileEntity(placed[0], placed[1], placed[2]);
                return te instanceof IGregTechTileEntity
                    && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEMultiBlockBase
                    && ((MTEMultiBlockBase) ((IGregTechTileEntity) te).getMetaTileEntity()).mMachine;
            }
            default:
                throw new IllegalArgumentException("unknown condition " + name);
        }
    }

    /** One status line for the rigs built (the recorder logs it at the end of each scene). */
    static String status() {
        final StringBuilder b = new StringBuilder();
        if (AutoRig.eyeCtrl != null || AutoRig.keeCtrl != null) b.append(AutoRig.status());
        if (AutoRigEyeAE.ctrl != null) b.append(b.length() > 0 ? " || " : "")
            .append(AutoRigEyeAE.status());
        return b.toString();
    }

    private static String place(String spec, int[] p, ForgeDirection facing) {
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
