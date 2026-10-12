package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * D-49 test bench for the Eye's live star (d49-build-plan.md §15.2): {@code /btseye star <x> <y> <z> <sub> …}, typed
 * on the dev-server console or by an op. Every subcommand acts on the Eye controller at x y z ("~" relative to the
 * sender).
 * <ul>
 * <li>{@code status} - the scanner lines;</li>
 * <li>{@code target <sol|siriusa|arcturus|neutron|siriusb|magnetar|none>} - the controller-slot chart;</li>
 * <li>{@code lit [chart]} - light the slot's (or the named) star at once, without a charge (T1-T4), and start the
 * bench ledger;</li>
 * <li>{@code seed <n>} - a fresh field with that noise seed (a burning star relights at the gauges' zero);</li>
 * <li>{@code rig <off|bang|fine|none>} - the prototype's regulators replace the Field sockets ({@code none} = the
 * sockets again);</li>
 * <li>{@code sim <seconds> [compare]} - run field seconds now; {@code compare} also runs {@code FieldSim} offline
 * from the same seed and settings and prints both hashes (equal when the Eye matches the prototype);</li>
 * <li>{@code set dial|feed <v>}, {@code unlock <1-6>}, {@code milestones <n> done}, {@code fill};</li>
 * <li>{@code sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog]} and {@code probe} - the sensor hatches;</li>
 * <li>{@code wrench} - what picking the controller up and placing it back keeps (item NBT only).</li>
 * </ul>
 */
public final class EyeStarBench {

    private EyeStarBench() {}

    public static final String USAGE = "/btseye star <x> <y> <z> status | target <name> | lit [name] | seed <n> | "
        + "rig <off|bang|fine|none> | sim <s> [compare] | set dial|feed <v> | unlock <1-6> | milestones <n> done | "
        + "fill | sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog] | probe | wrench";

    /** Runs {@code args} = {@code star x y z sub …}. Returns the lines to print. */
    public static List<String> run(ICommandSender sender, String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length < 5) {
            out.add(USAGE);
            return out;
        }
        final World world = sender.getEntityWorld();
        final int x = coord(sender, args[1], 0), y = coord(sender, args[2], 1), z = coord(sender, args[3], 2);
        final MTEEyeOfHarmonyBTS eye = eyeAt(world, x, y, z);
        if (eye == null) {
            out.add("No Eye of Harmony controller at (" + x + ", " + y + ", " + z + ").");
            return out;
        }
        final EyeStarRuntime rt = eye.starRuntime();
        final String sub = args[4].toLowerCase(Locale.ROOT);
        final String a5 = args.length > 5 ? args[5].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "status":
                break;
            case "target": {
                if (!a5.isEmpty() && !"none".equals(a5) && chart(a5) == null) {
                    out.add("Unknown chart " + a5 + " (sol, siriusa, arcturus, neutron, siriusb, magnetar, none).");
                    return out;
                }
                eye.debugSetTarget(chart(a5) == null ? null : new ItemStack(chart(a5), 1, 0));
                out.add("Target: " + (chart(a5) == null ? "none" : a5));
                break;
            }
            case "lit": {
                if (!a5.isEmpty()) {
                    if (chart(a5) == null) {
                        out.add("Unknown chart " + a5 + ".");
                        return out;
                    }
                    eye.debugSetTarget(new ItemStack(chart(a5), 1, 0));
                }
                final String err = rt.debugLight(StarDef.byChart(rt.host.target()));
                out.add(err == null ? "LIT (bench ledger started, seed " + rt.seed + ")" : "lit refused: " + err);
                break;
            }
            case "seed":
                rt.debugSeed(Long.parseLong(a5));
                out.add("Seed " + a5 + ": fresh field" + (rt.burning() ? ", the star relit (ledger restarted)" : ""));
                break;
            case "rig": {
                final FieldRig.Mode mode = FieldRig.Mode.parse(a5);
                rt.setRig(mode);
                out.add("Rig: " + (mode == null ? "none (the Field sockets)" : mode.name()));
                break;
            }
            case "sim":
                sim(eye, rt, Long.parseLong(a5), args.length > 6 && "compare".equalsIgnoreCase(args[6]), out);
                break;
            case "set":
                if ("dial".equals(a5)) rt.setDial(Double.parseDouble(args[6]));
                else if ("feed".equals(a5)) rt.setFeed(Double.parseDouble(args[6]));
                else {
                    out.add("set dial|feed <v> (shock is the Core Collapse's, unit 5)");
                    return out;
                }
                out.add(
                    "dial " + EyeStarRuntime.f2(rt.dialSetpoint())
                        + ", feed "
                        + String.format(Locale.ROOT, "%.3f", rt.feedSetpoint()));
                break;
            case "unlock":
                rt.setUnlocked(Integer.parseInt(a5));
                out.add("Unlocked T" + rt.unlockedTier());
                break;
            case "milestones":
                rt.milestones.complete(Integer.parseInt(a5));
                out.add(rt.milestones.progressLine(Integer.parseInt(a5)));
                break;
            case "fill": {
                eye.debugFillBuffers(1_000_000_000_000L);
                final UUID owner = rt.host.owner();
                if (owner != null) {
                    WirelessNetworkManager.strongCheckOrAddUser(owner);
                    WirelessNetworkManager.addEUToGlobalEnergyMap(owner, BigInteger.TEN.pow(18));
                }
                out.add("Buffers filled to 10^12 L" + (owner != null ? ", 10^18 EU granted" : " (no owner: no EU)"));
                break;
            }
            case "sensor":
                sensor(sender, world, args, out);
                return out;
            case "probe":
                for (MTEHatchStellarFieldSensor s : eye.sensorHatches()) {
                    out.add(s.statusLine() + " -> redstone " + s.currentOutput());
                }
                if (eye.sensorHatches()
                    .isEmpty()) out.add("No Stellar Field Sensor Hatches in the Sensor sockets.");
                return out;
            case "wrench":
                eye.debugWrench();
                out.add("Wrenched and placed back (item NBT only):");
                break;
            default:
                out.add(USAGE);
                return out;
        }
        out.addAll(eye.statusLines());
        return out;
    }

    private static void sim(MTEEyeOfHarmonyBTS eye, EyeStarRuntime rt, long secs, boolean compare, List<String> out) {
        final long start = System.nanoTime();
        for (long i = 0; i < secs; i++) eye.fieldSecond();
        final long us = (System.nanoTime() - start) / 1000L;
        out.add("sim " + secs + " s in " + us / 1000 + " ms (" + (secs > 0 ? us / secs : 0) + " us per field second)");
        final EyeStarRuntime.Ledger l = rt.ledger;
        if (l == null) {
            out.add("No bench ledger (it starts at 'lit' or 'seed').");
            return;
        }
        out.add(
            "ledger: " + l.steps
                + " s since lit, zones "
                + l.zoneT[0]
                + "/"
                + l.zoneT[1]
                + "/"
                + l.zoneT[2]
                + "/"
                + l.zoneT[3]
                + "/"
                + l.zoneT[4]
                + ", mean Y "
                + String.format(Locale.ROOT, "%.6f", l.yN > 0 ? l.ySum / l.yN : 0)
                + ", fails "
                + l.fails
                + ", cycles "
                + l.cycles
                + ", product "
                + String.format(Locale.ROOT, "%.3f", l.product)
                + " L, HSM fed "
                + String.format(Locale.ROOT, "%.3f", l.hsm)
                + " L, hash "
                + Long.toHexString(l.hash & 0xFFFFFFFFL));
        if (!compare || rt.fs == null) return;
        final FieldSim.Cfg c = new FieldSim.Cfg();
        c.seed = l.seed;
        c.rig = rt.rig == null ? FieldRig.Mode.OFF : rt.rig;
        c.m = rt.dialSet;
        c.tt = rt.host.timeTier();
        c.ts = rt.host.stabilisationTier();
        c.r = 1;
        c.feed = rt.feedSet;
        final Map<String, Object> res = FieldSim.runCase(rt.fs.tier.n, c, l.steps);
        final long hash = ((Number) res.get("hash")).longValue();
        out.add(
            "offline FieldSim T" + rt.fs.tier.n
                + " seed "
                + c.seed
                + " rig "
                + c.rig
                + " m "
                + c.m
                + " tt "
                + c.tt
                + " ts "
                + c.ts
                + " feed "
                + c.feed
                + ": zones "
                + res.get("zoneT")
                + ", mean Y "
                + String.format(Locale.ROOT, "%.6f", ((Number) res.get("meanY")).doubleValue())
                + ", fails "
                + res.get("fails")
                + ", prod "
                + res.get("prod")
                + ", hash "
                + Long.toHexString(hash));
        out.add(
            hash == (l.hash & 0xFFFFFFFFL) ? "COMPARE PASS: the Eye's field equals the prototype's, second for second"
                : "COMPARE DIFFERS (needs p = 1, upkeep r = 1, no failure, and the rig or no trims since 'lit')");
    }

    private static void sensor(ICommandSender sender, World world, String[] args, List<String> out) {
        if (args.length < 11) {
            out.add("sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog]");
            return;
        }
        final TileEntity te = world
            .getTileEntity(coord(sender, args[5], 0), coord(sender, args[6], 1), coord(sender, args[7], 2));
        final IMetaTileEntity mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
            : null;
        if (!(mte instanceof MTEHatchStellarFieldSensor)) {
            out.add("No Stellar Field Sensor Hatch there.");
            return;
        }
        final MTEHatchStellarFieldSensor s = (MTEHatchStellarFieldSensor) mte;
        final FieldReadout.Channel ch = channel(args[8]);
        if (ch == null) {
            out.add(
                "Channels: P, THETA, OMEGA, PI, SHIELD, INTEGRITY, STABILITY, ZONE, WARN_PLUS, WARN_MINUS, "
                    + "WARN_ANY, UPKEEP_OK, SETPOINT_ERROR");
            return;
        }
        s.setChannelOrdinal(ch.ordinal());
        s.setLessThan("lt".equalsIgnoreCase(args[9]));
        s.setThreshold(Double.parseDouble(args[10]));
        s.setAnalog(args.length > 11 && "analog".equalsIgnoreCase(args[11]));
        out.add("Sensor set: " + s.statusLine());
    }

    private static FieldReadout.Channel channel(String name) {
        final String n = name.toUpperCase(Locale.ROOT);
        switch (n) {
            case "TH":
                return FieldReadout.Channel.THETA;
            case "OM":
                return FieldReadout.Channel.OMEGA;
            case "A":
                return FieldReadout.Channel.SHIELD;
            case "I":
                return FieldReadout.Channel.INTEGRITY;
            default:
                for (FieldReadout.Channel c : FieldReadout.Channel.values()) if (c.name()
                    .equals(n)) return c;
                return null;
        }
    }

    private static Item chart(String name) {
        switch (name) {
            case "sol":
                return SLItems.starChartSol;
            case "siriusa":
                return SLItems.starChartSiriusA;
            case "arcturus":
                return SLItems.starChartArcturus;
            case "neutron":
            case "neutronstar":
                return SLItems.starChartNeutronStar;
            case "siriusb":
                return SLItems.starChartSiriusB;
            case "magnetar":
                return SLItems.starChartMagnetar;
            default:
                return null;
        }
    }

    private static MTEEyeOfHarmonyBTS eyeAt(World world, int x, int y, int z) {
        if (world == null) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEEyeOfHarmonyBTS ? (MTEEyeOfHarmonyBTS) mte : null;
    }

    /** An int, or "~" / "~n" relative to the sender's block position on {@code axis} (0 x, 1 y, 2 z). */
    private static int coord(ICommandSender sender, String text, int axis) {
        if (text.startsWith("~")) {
            final ChunkCoordinates at = sender.getPlayerCoordinates();
            final int base = axis == 0 ? at.posX : axis == 1 ? at.posY : at.posZ;
            return base + (text.length() > 1 ? Integer.parseInt(text.substring(1)) : 0);
        }
        return Integer.parseInt(text);
    }
}
