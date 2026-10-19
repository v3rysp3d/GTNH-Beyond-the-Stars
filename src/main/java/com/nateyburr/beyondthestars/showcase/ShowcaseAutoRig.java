package com.nateyburr.beyondthestars.showcase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.eye.field.TrimDef;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStdRig;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchInputBusDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInputDebug;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.conditions.RedstoneCondition;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * THE AUTOMATION SHOWCASE (designs/docs/showcase-worlds.md): {@code /btsshowcase autorig x y z} turns the machine at x
 * y
 * z into an exhibit that runs on in-world blocks a player can inspect, with no showcase mode:
 * <ul>
 * <li><b>Supplies:</b> every plain input hatch and bus that feeds it becomes a GT debug hatch / bus holding what it
 * held
 * (one fluid per hatch, as the Eye's buffer sockets read only the first). The Eye's Field hatches and the quasar's trim
 * hatches stay plain: they are metered.</li>
 * <li><b>Power:</b> the owner's wireless network gets one top-up that outlasts any visit (the in-world equivalent of
 * {@code /gt global_energy_add}); wired hatches are GT debug energy hatches already.</li>
 * <li><b>The Eye's trims:</b> the bench's free field rig and standard rig are switched off; each trim the tier needs is
 * a Directive Hatch (Bit n) above a Field socket, dust onto a GT Debug Fluid Tank whose Machine Controller cover lets
 * its Fluid Regulator cover ("Conditional", the trim's cap) pour into the Field hatch (docs/automation.md §4.1).</li>
 * <li><b>Chunks:</b> the keep-loaded list ({@link BTSShowcase#setKeep}), since the pack's chunk loaders cannot cover
 * these footprints within Forge's default ticket limits.</li>
 * </ul>
 * Idempotent: running it again converts nothing twice and rebuilds no station that stands.
 */
public final class ShowcaseAutoRig {

    private ShowcaseAutoRig() {}

    /** The owner top-up: 10^30 EU (an Eye T10 pays 3.96 x 10^16 a cycle). */
    private static final BigInteger EU_TOPUP = BigInteger.TEN.pow(30);

    public static List<String> build(World world, int x, int y, int z) {
        final List<String> out = new ArrayList<>();
        final IMetaTileEntity mte = mteAt(world, x, y, z);
        if (!(mte instanceof MTEMultiBlockBase) || !(mte instanceof ShowcaseHost)) {
            out.add("No Beyond the Stars controller at " + x + " " + y + " " + z + ".");
            return out;
        }
        final MTEMultiBlockBase m = (MTEMultiBlockBase) mte;
        final ShowcaseHost host = (ShowcaseHost) mte;
        if (host.isShowcase()) {
            host.setShowcase(false);
            out.add("showcase mode was on: switched off (this exhibit runs on its world)");
        }
        BTSShowcase.setKeep(world, x, y, z, true);
        final int[] n = convertSupplies(world, m);
        out.add(host.showcaseName() + ": " + n[0] + " hatch(es) and " + n[1] + " bus(es) now GT debug supplies");
        if (mte instanceof MTEEyeOfHarmonyBTS) out.addAll(eye(world, (MTEEyeOfHarmonyBTS) mte));
        out.add("keep loaded: on");
        return out;
    }

    // ================================================================================== supplies

    /** The hatches that feed a machine (its trim / Field hatches left out). */
    private static Set<MTEHatchInput> supplyHatches(MTEMultiBlockBase m) {
        final Set<MTEHatchInput> s = new LinkedHashSet<>(m.mInputHatches);
        if (m instanceof MTEEyeOfHarmonyBTS) {
            final MTEEyeOfHarmonyBTS e = (MTEEyeOfHarmonyBTS) m;
            s.addAll(e.gasHatches());
            s.addAll(e.stellarHatches());
            s.removeAll(e.fieldHatches());
        } else if (m instanceof MTEQuasarEngine) {
            final MTEQuasarEngine q = (MTEQuasarEngine) m;
            s.addAll(q.gasHatches());
            s.removeAll(q.trimHatches());
        } else if (m instanceof MTEConduitElement) {
            s.addAll(((MTEConduitElement<?>) m).fuelHatches());
        } else if (m instanceof MTERealityDiffraction) {
            for (int k = 0; k < 6; k++) s.addAll(((MTERealityDiffraction) m).anchorHatches(k));
        }
        return s;
    }

    private static Set<MTEHatchInputBus> supplyBuses(MTEMultiBlockBase m) {
        final Set<MTEHatchInputBus> s = new LinkedHashSet<>(m.mInputBusses);
        if (m instanceof MTEKerrEngine) s.addAll(((MTEKerrEngine) m).boltBuses());
        // the Eye's plinth buses stay plain: they take cores and charts too (a chart in a debug bus floods the ME
        // output); the builder stocks them with ignition charges instead
        if (m instanceof MTEEyeOfHarmonyBTS) s.clear();
        return s;
    }

    /** Returns {hatches, buses} converted. */
    static int[] convertSupplies(World world, MTEMultiBlockBase m) {
        int h = 0, b = 0;
        for (MTEHatchInput hatch : supplyHatches(m)) {
            if (hatch == null || !hatch.isValid() || hatch instanceof MTEHatchInputDebug || isMe(hatch)) continue;
            final List<FluidStack> fluids = new ArrayList<>();
            if (hatch instanceof MTEHatchMultiInput) {
                for (FluidStack f : ((MTEHatchMultiInput) hatch).getStoredFluid())
                    if (f != null && f.getFluid() != null) {
                        fluids.add(f);
                    }
            } else if (hatch.getFluid() != null && hatch.getFluid()
                .getFluid() != null) fluids.add(hatch.getFluid());
            if (fluids.isEmpty()) continue;
            if (toDebugHatch(world, hatch.getBaseMetaTileEntity(), fluids)) h++;
        }
        for (MTEHatchInputBus bus : supplyBuses(m)) {
            if (bus == null || !bus.isValid() || bus instanceof MTEHatchInputBusDebug || isMe(bus)) continue;
            final IGregTechTileEntity t = bus.getBaseMetaTileEntity();
            final List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < t.getSizeInventory(); i++) {
                final ItemStack s = t.getStackInSlot(i);
                if (s != null && s.getItem() != null && i != bus.getCircuitSlot()) items.add(s.copy());
            }
            if (items.isEmpty()) continue;
            if (toDebugBus(world, t, items)) b++;
        }
        return new int[] { h, b };
    }

    private static boolean isMe(Object hatch) {
        final String n = hatch.getClass()
            .getSimpleName();
        return n.contains("InputME") || n.contains("BusME");
    }

    /** Replaces the hatch at {@code base} by a GT debug input hatch holding {@code fluids} (same facing). */
    private static boolean toDebugHatch(World world, IGregTechTileEntity base, List<FluidStack> fluids) {
        final int x = base.getXCoord(), y = base.getYCoord(), z = base.getZCoord();
        final ForgeDirection facing = base.getFrontFacing();
        final short id = (short) ItemList.Hatch_Input_Debug.get(1L)
            .getItemDamage();
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, id, facing)) return false;
        final IMetaTileEntity mte = mteAt(world, x, y, z);
        if (!(mte instanceof MTEHatchInputDebug)) return false;
        final NBTTagCompound nbt = new NBTTagCompound();
        ((MTEHatchInputDebug) mte).saveNBTData(nbt);
        for (int i = 0; i < fluids.size(); i++) {
            final FluidStack f = new FluidStack(fluids.get(i), 1000);
            nbt.setTag("debugFluid" + i, f.writeToNBT(new NBTTagCompound()));
        }
        ((MTEHatchInputDebug) mte).loadNBTData(nbt);
        return true;
    }

    /** Replaces the bus at {@code base} by an endless GT debug input bus holding {@code items}. */
    private static boolean toDebugBus(World world, IGregTechTileEntity base, List<ItemStack> items) {
        final int x = base.getXCoord(), y = base.getYCoord(), z = base.getZCoord();
        final ForgeDirection facing = base.getFrontFacing();
        final short id = (short) ItemList.Hatch_Input_Bus_Debug.get(1L)
            .getItemDamage();
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, id, facing)) return false;
        final IMetaTileEntity mte = mteAt(world, x, y, z);
        if (!(mte instanceof MTEHatchInputBusDebug)) return false;
        final MTEHatchInputBusDebug d = (MTEHatchInputBusDebug) mte;
        d.finiteMode = false;
        for (int i = 0; i < items.size() && i < d.phantomHolder.getSlots(); i++) {
            final ItemStack s = items.get(i)
                .copy();
            s.stackSize = 1;
            d.phantomHolder.setStackInSlot(i, s);
        }
        return true;
    }

    // ======================================================================================= Eye

    /** A trim valve: the Directive set and bit that opens it, its fluid and its rate (L/t), and its sign. */
    private static final class Valve {

        final DirectiveSet set;
        final DirectiveSet.Output bit;
        final Fluid fluid;
        final int litresPerTick;
        final String label;

        Valve(DirectiveSet set, DirectiveSet.Output bit, TrimDef trim, int parallel) {
            this.set = set;
            this.bit = bit;
            fluid = EyeStarRuntime.trimFluid(trim.kind);
            litresPerTick = Math.max(1, (int) Math.ceil(trim.cap * trim.kind.unitLitres * parallel / 20.0));
            label = trim.kind.displayName;
        }
    }

    /** The valves a lit Eye of tier {@code n} needs to hold its field (docs/automation.md §2.1, §4). */
    private static List<Valve> valves(int n, int parallel) {
        final List<Valve> v = new ArrayList<>();
        final TrimDef[] trims = TierParams.of(n)
            .trims()
            .toArray(new TrimDef[0]);
        final DirectiveSet.Output[] bits = { DirectiveSet.Output.BIT1, DirectiveSet.Output.BIT2,
            DirectiveSet.Output.BIT3, DirectiveSet.Output.BIT4 };
        if (n <= 6) {
            for (int i = 0; i < trims.length && i < 4; i++) {
                v.add(new Valve(DirectiveSet.EYE_TRIMS, bits[i], trims[i], parallel));
            }
            // the event valve on Status bit 3: T3's second Super Coolant (the flash), T6's Antipolarization Agent
            if (n == 3) v.add(new Valve(DirectiveSet.EYE_STATUS, DirectiveSet.Output.BIT3, trims[2], parallel));
            if (n == 6 && trims.length > 4) {
                v.add(new Valve(DirectiveSet.EYE_STATUS, DirectiveSet.Output.BIT3, trims[4], parallel));
            }
        } else {
            // the galaxy tiers: the Space valve on Field bit 1; T10 adds Dark Matter / SpaceTime on Universe 1 / 2
            v.add(new Valve(DirectiveSet.EYE_FIELD, DirectiveSet.Output.BIT1, trims[0], parallel));
            if (n >= 10 && trims.length >= 3) {
                v.add(new Valve(DirectiveSet.EYE_UNIVERSE, DirectiveSet.Output.BIT1, trims[1], parallel));
                v.add(new Valve(DirectiveSet.EYE_UNIVERSE, DirectiveSet.Output.BIT2, trims[2], parallel));
            }
        }
        return v;
    }

    private static List<String> eye(World world, MTEEyeOfHarmonyBTS eye) {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = eye.starRuntime();
        if (rt.rig() != null) {
            rt.setRig(null);
            out.add("the bench's free field rig: off (the Field sockets count now)");
        }
        EyeStdRig.set(eye, 0);
        final UUID owner = eye.getBaseMetaTileEntity()
            .getOwnerUuid();
        if (owner != null) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            if (WirelessNetworkManager.getUserEU(owner)
                .compareTo(EU_TOPUP.divide(BigInteger.TEN)) < 0) {
                WirelessNetworkManager.addEUToGlobalEnergyMap(owner, EU_TOPUP);
                out.add("owner's wireless network topped up to 10^30 EU");
            }
        } else {
            out.add("NO OWNER: the Eye cannot pay its cycles from a wireless network");
        }
        // ignition charges for a relight, in the first plinth bus (64 x every free slot)
        int charges = 0;
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            final IGregTechTileEntity t = bus.getBaseMetaTileEntity();
            for (int i = 0; i < t.getSizeInventory(); i++) {
                if (i == bus.getCircuitSlot() || t.getStackInSlot(i) != null) continue;
                t.setInventorySlotContents(
                    i,
                    new ItemStack(com.nateyburr.beyondthestars.items.SLItems.stellarIgnitionCharge, 64, 0));
                charges += 64;
            }
            break;
        }
        if (charges > 0) out.add(charges + " Stellar Ignition Charges in the plinth bus (relights)");
        final int tier = rt.star() == null ? rt.unlockedTier()
            : rt.star()
                .n();
        out.add(upkeepHatches(world, eye, tier));
        final List<Valve> valves = valves(tier, Math.max(1, rt.lastParallel()));
        final ExtendedFacing al = eye.getExtendedFacing();
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        final int[][] f = BTSShapes.socketRels(MTEEyeOfHarmonyBTS.PIECE_CORE, 'f');
        int built = 0, socket = 0;
        for (Valve v : valves) {
            boolean placed = false;
            while (socket < f.length && !placed) {
                final int[] p = SLConsoleTest
                    .worldPos(al, base.getXCoord(), base.getYCoord(), base.getZCoord(), f[socket++]);
                final String why = station(world, p, v);
                if (why == null) {
                    placed = true;
                    built++;
                    out.add("valve: " + v.label + " on " + v.set.label + " " + v.bit.label + " at " + pos(p));
                } else {
                    out.add("socket at " + pos(p) + " skipped: " + why);
                }
            }
            if (!placed) out.add("NO free Field socket left for the " + v.label + " valve");
        }
        eye.checkStructure(true, base);
        out.add(
            "Eye T" + tier
                + ": "
                + built
                + " of "
                + valves.size()
                + " valve(s) built; formed "
                + eye.mMachine
                + ", Directive Hatches "
                + eye.directiveHatchCount());
        return out;
    }

    /**
     * The fluids an Eye of tier {@code n} draws to run, and to relight after a loss (D-54: T4 from a charge and HSM, T5
     * / T6 from dwarf matter and the Agent, T7-T9 from the Magnetar Collapse charge, T9's Infall, T10's universe).
     */
    private static List<Fluid> upkeepFluids(int n) {
        final Set<Fluid> f = new LinkedHashSet<>();
        final com.nateyburr.beyondthestars.machines.eye.star.StarDef def = com.nateyburr.beyondthestars.machines.eye.star.StarDef
            .ofTier(n);
        if (def != null) {
            for (com.nateyburr.beyondthestars.machines.eye.EyeRecipe.FluidAmount a : def.upkeep) f.add(a.fluid);
            if (def.feed != null) f.add(def.feed);
            f.addAll(def.exactUpkeep.keySet());
        }
        if (n == 4) f.add(com.nateyburr.beyondthestars.fluids.SLFluids.hyperdenseStellarMixture);
        if (n >= 5) f.add(EyeStarRuntime.wdmFluid());
        if (n >= 6) {
            f.add(EyeStarRuntime.bdmFluid());
            f.add(com.nateyburr.beyondthestars.fluids.SLFluids.antipolarizationAgent);
        }
        if (n >= 9) f.add(com.nateyburr.beyondthestars.fluids.SLQuasarFluids.superdenseErgomatter);
        if (n >= 10) f.add(com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.darkMatter());
        f.remove(null);
        return new ArrayList<>(f);
    }

    /** Empty gas / stellar sockets become debug hatches of the upkeep fluids no debug hatch gives yet. */
    private static String upkeepHatches(World world, MTEEyeOfHarmonyBTS eye, int tier) {
        final List<Fluid> want = upkeepFluids(tier);
        final List<MTEHatchInput> sockets = new ArrayList<>(eye.gasHatches());
        sockets.addAll(eye.stellarHatches());
        for (MTEHatchInput h : sockets) {
            if (h instanceof MTEHatchInputDebug && h.getFluid() != null) want.remove(
                h.getFluid()
                    .getFluid());
        }
        int n = 0;
        final List<String> missing = new ArrayList<>();
        for (Fluid f : new ArrayList<>(want)) {
            MTEHatchInput free = null;
            for (MTEHatchInput h : sockets) {
                if (h != null && h.isValid()
                    && !(h instanceof MTEHatchInputDebug)
                    && !isMe(h)
                    && h.getFluid() == null) {
                    free = h;
                    break;
                }
            }
            if (free == null) {
                missing.add(f.getName());
                continue;
            }
            sockets.remove(free);
            final List<FluidStack> one = new ArrayList<>();
            one.add(new FluidStack(f, 1000));
            if (toDebugHatch(world, free.getBaseMetaTileEntity(), one)) n++;
        }
        eye.checkStructure(true, eye.getBaseMetaTileEntity());
        return "upkeep: " + n
            + " socket(s) made debug hatches of "
            + want.size()
            + " fluid(s) wanted"
            + (missing.isEmpty() ? "" : "; NO free socket for " + String.join(", ", missing));
    }

    /**
     * One trim station on the Field socket at {@code p}: a Directive Hatch above it, a Debug Fluid Tank in front of
     * it with a Fluid Regulator (Conditional) into the hatch and a Machine Controller on top, dust between. Null when
     * built (or already standing), else why the socket was skipped.
     */
    private static String station(World world, int[] p, Valve v) {
        final IMetaTileEntity field = mteAt(world, p[0], p[1], p[2]);
        if (!(field instanceof MTEHatchInput)) return "no Field hatch";
        ForgeDirection o = ((MTEHatchInput) field).getBaseMetaTileEntity()
            .getFrontFacing();
        if (o == ForgeDirection.UP || o == ForgeDirection.DOWN || o == ForgeDirection.UNKNOWN)
            return "hatch faces " + o;
        final int[] up = { p[0], p[1] + 1, p[2] };
        final int[] tank = { p[0] + o.offsetX, p[1], p[2] + o.offsetZ };
        final int[] dust = { tank[0], tank[1] + 1, tank[2] };
        final IMetaTileEntity above = mteAt(world, up[0], up[1], up[2]);
        if (above instanceof MTEHatchDirective) {
            final MTEHatchDirective d = (MTEHatchDirective) above;
            if (d.getSet() == v.set && d.getOutput() == v.bit && mteAt(world, tank[0], tank[1], tank[2]) != null) {
                return null; // standing already
            }
        }
        if (!(above instanceof MTEHatchDirective) && above != null
            && !(above instanceof com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor)) {
            return "the socket above holds a " + above.getClass()
                .getSimpleName();
        }
        if (above instanceof com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor
            && hasCover(world, up)) return "the sensor above is wired (covers)";
        if (!world.isAirBlock(tank[0], tank[1], tank[2]) && mteAt(world, tank[0], tank[1], tank[2]) == null) {
            return "the cell in front is not free";
        }
        if (!world.isAirBlock(dust[0], dust[1], dust[2])
            && world.getBlock(dust[0], dust[1], dust[2]) != Blocks.redstone_wire)
            return "the cell above it is not free";
        // the Directive Hatch above the Field socket
        MTEReactorCoreMk1.placeGtMachine(
            world,
            up[0],
            up[1],
            up[2],
            (short) BTSMachines.directiveHatch.getItemDamage(),
            ((IGregTechTileEntity) world.getTileEntity(up[0], up[1], up[2]) == null ? o
                : ((IGregTechTileEntity) world.getTileEntity(up[0], up[1], up[2])).getFrontFacing()));
        final IMetaTileEntity dmte = mteAt(world, up[0], up[1], up[2]);
        if (!(dmte instanceof MTEHatchDirective)) return "could not place the Directive Hatch";
        final MTEHatchDirective d = (MTEHatchDirective) dmte;
        d.setSet(v.set);
        d.setOutput(v.bit);
        d.setWindow(10);
        // the valve
        final ForgeDirection side = o.getRotation(ForgeDirection.UP);
        if (!MTEReactorCoreMk1.placeGtMachine(
            world,
            tank[0],
            tank[1],
            tank[2],
            (short) ItemList.Debug_Fluid_Tank.get(1L)
                .getItemDamage(),
            side)) return "could not place the tank";
        final IMetaTileEntity t = mteAt(world, tank[0], tank[1], tank[2]);
        if (!(t instanceof MTEBasicTank) || v.fluid == null) return "no tank or no fluid";
        ((MTEBasicTank) t).setFillableStack(new FluidStack(v.fluid, 1000));
        final Cover reg = cover(world, tank, o.getOpposite(), regulatorFor(v.litresPerTick).get(1L));
        if (reg instanceof CoverFluidRegulator) {
            final CoverFluidRegulator r = (CoverFluidRegulator) reg;
            r.setTickRateForUi(1);
            r.setSpeed(v.litresPerTick);
            r.setCondition(CoverFluidRegulator.Conditional.Conditional);
        }
        final Cover ctl = cover(world, tank, ForgeDirection.UP, ItemList.Cover_Controller.get(1L));
        if (ctl instanceof CoverControlsWork) {
            ((CoverControlsWork) ctl).setRedstoneCondition(RedstoneCondition.ENABLE_WITH_REDSTONE);
        }
        issue(world, tank, o.getOpposite());
        issue(world, tank, ForgeDirection.UP);
        world.setBlock(dust[0], dust[1], dust[2], Blocks.redstone_wire, 0, 3);
        sign(
            world,
            new int[] { tank[0] + o.offsetX, tank[1], tank[2] + o.offsetZ },
            o,
            v.label + " valve",
            v.set.label.replace("Eye: ", ""),
            v.bit.label.replace(" only (15 / 0)", ""),
            v.litresPerTick + " L/t");
        return null;
    }

    /** The smallest GT Fluid Regulator whose rate reaches {@code lpt} L/t. */
    private static ItemList regulatorFor(int lpt) {
        final ItemList[] tiers = { ItemList.FluidRegulator_LV, ItemList.FluidRegulator_MV, ItemList.FluidRegulator_HV,
            ItemList.FluidRegulator_EV, ItemList.FluidRegulator_IV, ItemList.FluidRegulator_LuV,
            ItemList.FluidRegulator_ZPM, ItemList.FluidRegulator_UV };
        int cap = 32;
        for (ItemList t : tiers) {
            if (lpt <= cap) return t;
            cap *= 4;
        }
        return ItemList.FluidRegulator_UV;
    }

    // ======================================================================================= helpers

    static IMetaTileEntity mteAt(World world, int x, int y, int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity() : null;
    }

    private static boolean hasCover(World world, int[] p) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return false;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (((IGregTechTileEntity) te).getCoverAtSide(d) != null && ((IGregTechTileEntity) te).getCoverAtSide(d)
                .getCoverID() != 0) return true;
        }
        return false;
    }

    private static Cover cover(World world, int[] p, ForgeDirection side, ItemStack item) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity) || item == null) return null;
        final IGregTechTileEntity base = (IGregTechTileEntity) te;
        base.attachCover(CoverRegistry.buildCover(item, side, base));
        return base.getCoverAtSide(side);
    }

    private static void issue(World world, int[] p, ForgeDirection side) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof IGregTechTileEntity) ((IGregTechTileEntity) te).issueCoverUpdate(side);
    }

    /** A wall sign facing {@code out}, if the cell is air (up to four lines of 15 characters). */
    private static void sign(World world, int[] p, ForgeDirection out, String... lines) {
        if (!world.isAirBlock(p[0], p[1], p[2])) return;
        final int meta = out == ForgeDirection.NORTH ? 2
            : out == ForgeDirection.SOUTH ? 3 : out == ForgeDirection.WEST ? 4 : 5;
        world.setBlock(p[0], p[1], p[2], Blocks.wall_sign, meta, 3);
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (te instanceof TileEntitySign) {
            final TileEntitySign s = (TileEntitySign) te;
            for (int i = 0; i < 4; i++) {
                final String l = i < lines.length ? lines[i] : "";
                s.signText[i] = l.length() > 15 ? l.substring(0, 15) : l;
            }
            s.markDirty();
            world.markBlockForUpdate(p[0], p[1], p[2]);
        }
    }

    static String pos(int[] p) {
        return p[0] + " " + p[1] + " " + p[2];
    }
}
