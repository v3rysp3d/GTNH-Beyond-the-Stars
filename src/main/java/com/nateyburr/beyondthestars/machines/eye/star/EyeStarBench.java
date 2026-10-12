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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
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
        + "fill | sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog] | sensors <channel> <gt|lt> <thr> "
        + "[analog] | probe | wrench | charge [n] | buffer <fluid> <L> | core <name> | escrow | "
        + "legacy [galactic] [run <key> <n>]";

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
        eye.benchEnsureChecked(); // a chunk just reloaded: walk now so the hatch lists are there
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
            case "charge": {
                final int n = a5.isEmpty() ? 1 : Integer.parseInt(a5);
                out.add(putCharges(eye, n));
                return out;
            }
            case "buffer": {
                final Fluid fluid = args.length > 6 ? FluidRegistry.getFluid(args[5]) : null;
                if (fluid == null) {
                    out.add("buffer <fluid registry name> <litres>");
                    return out;
                }
                eye.debugSetBuffer(fluid, Long.parseLong(args[6]));
                out.add("Buffer " + fluid.getName() + " = " + args[6] + " L");
                break;
            }
            case "sensors": {
                if (args.length < 8) {
                    out.add("sensors <channel> <gt|lt> <threshold> [analog]");
                    return out;
                }
                final FieldReadout.Channel ch = channel(args[5]);
                if (ch == null) {
                    out.add("Unknown channel " + args[5] + ".");
                    return out;
                }
                for (MTEHatchStellarFieldSensor s : eye.sensorHatches()) {
                    s.setChannelOrdinal(ch.ordinal());
                    s.setLessThan("lt".equalsIgnoreCase(args[6]));
                    s.setThreshold(Double.parseDouble(args[7]));
                    s.setAnalog(args.length > 8 && "analog".equalsIgnoreCase(args[8]));
                }
                out.add(
                    eye.sensorHatches()
                        .size() + " sensors set");
                return out;
            }
            case "core": {
                final Item core = core(a5);
                if (core == null) {
                    out.add("core <stellarmass|shellburning|galactic|subspace|magnetar>");
                    return out;
                }
                out.add(rt.offerCore(new ItemStack(core), new boolean[1]));
                return out;
            }
            case "escrow": {
                final ItemStack back = rt.takeEscrow();
                out.add("Escrow returned: " + (back == null ? "nothing" : back.getDisplayName()));
                break;
            }
            case "legacy": {
                // legacy [galactic] [run <key> <successes>]
                boolean galactic = false;
                String run = null;
                long successes = 0;
                for (int i = 5; i < args.length; i++) {
                    if ("galactic".equalsIgnoreCase(args[i])) galactic = true;
                    if ("run".equalsIgnoreCase(args[i]) && i + 2 < args.length) {
                        run = args[++i];
                        successes = Long.parseLong(args[++i]);
                    }
                }
                eye.debugLegacyLoad(galactic, run, successes);
                out.add(
                    "Loaded 0.24.x NBT (galactic " + galactic + ", run " + run + " x" + successes + "), re-walked:");
                break;
            }
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

    private static Item core(String name) {
        switch (name) {
            case "stellarmass":
                return SLItems.stellarMassCore;
            case "shellburning":
                return SLItems.shellBurningCore;
            case "galactic":
                return SLItems.galacticUpgradeCore;
            case "subspace":
                return SLItems.subspaceCoolingCore;
            case "magnetar":
                return SLItems.magnetarSeedCore;
            default:
                return null;
        }
    }

    /** Puts {@code n} Stellar Ignition Charges into the first live plinth input bus. */
    private static String putCharges(MTEEyeOfHarmonyBTS eye, int n) {
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                if (bus.getStackInSlot(i) != null) continue;
                bus.getBaseMetaTileEntity()
                    .setInventorySlotContents(i, new ItemStack(SLItems.stellarIgnitionCharge, n, 0));
                return n + " Stellar Ignition Charge(s) in a plinth input bus (slot " + i + ")";
            }
        }
        return "No plinth input bus with a free slot.";
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
