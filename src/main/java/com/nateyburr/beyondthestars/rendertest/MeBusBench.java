package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.StorageChannel;
import appeng.api.util.AEColor;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

/**
 * DEV-ONLY test bench for the Eye's plinth ME input bus (the core and charge pickups; QA S-01, QA-A07):
 * {@code /btseye star x y z mebus setup | stock <slot> <core|charge|cobble> [n] | manual <core|charge|cobble> [n] |
 * power <AE> | status}, typed on the dev-server console. Loaded only by those commands.
 * <p>
 * {@code setup} turns the Eye's first plinth input bus into an ME input bus on a small AE2 network built straight out
 * from its face: an ME controller (creative power), a drive with an empty item cell, and a cable the bench reads the
 * network through. {@code stock} puts n items into the network and configures one of the bus's 16 stocking slots to
 * them; {@code manual} puts n items into the bus's manual slot; {@code power} swaps the creative controller for a plain
 * one and an Energy Cell holding that many AE; {@code status} prints the network's counts, the manual slot and the
 * escrow.
 */
public final class MeBusBench {

    private MeBusBench() {}

    /** The rig of the last {@code setup}: the bus, the controller, the drive and the reading cable. */
    private static int[] bus, ctrl, drive, cable;

    public static List<String> run(MTEEyeOfHarmonyBTS eye, String[] args) {
        final List<String> out = new ArrayList<>();
        final String sub = args.length > 5 ? args[5].toLowerCase(Locale.ROOT) : "status";
        final World world = eye.getBaseMetaTileEntity()
            .getWorld();
        switch (sub) {
            case "setup":
                setup(eye, world, out);
                break;
            case "stock": {
                final MTEHatchInputBusME me = meBus(world);
                if (me == null || args.length < 8) {
                    out.add(me == null ? "no ME input bus: run mebus setup" : "mebus stock <slot 1-16> <item> [n]");
                    break;
                }
                final int slot = Integer.parseInt(args[6]) - 1;
                final ItemStack what = item(eye, args[7]);
                final long n = args.length > 8 ? Long.parseLong(args[8]) : 1;
                if (what == null) {
                    out.add("unknown item " + args[7] + " (core, charge, cobble)");
                    break;
                }
                me.setSlotConfig(slot, what.copy());
                out.add(
                    "config slot " + (slot + 1)
                        + " = "
                        + what.getDisplayName()
                        + "; "
                        + inject(world, what, n)
                        + " injected into the network");
                break;
            }
            case "manual": {
                final MTEHatchInputBusME me = meBus(world);
                final ItemStack what = args.length > 6 ? item(eye, args[6]) : null;
                if (me == null || what == null) {
                    out.add(me == null ? "no ME input bus: run mebus setup" : "mebus manual <core|charge|cobble> [n]");
                    break;
                }
                final ItemStack s = what.copy();
                s.stackSize = args.length > 7 ? Integer.parseInt(args[7]) : 1;
                me.getBaseMetaTileEntity()
                    .setInventorySlotContents(me.getManualSlot(), s);
                out.add("manual slot = " + s.stackSize + " " + s.getDisplayName());
                break;
            }
            case "power": {
                final double ae = args.length > 6 ? Double.parseDouble(args[6]) : 200_000;
                if (ctrl == null) {
                    out.add("no rig: run mebus setup");
                    break;
                }
                final Block plain = AEApi.instance()
                    .definitions()
                    .blocks()
                    .controller()
                    .maybeBlock()
                    .orNull();
                final Block cell = AEApi.instance()
                    .definitions()
                    .blocks()
                    .energyCell()
                    .maybeBlock()
                    .orNull();
                MeParts.block(world, ctrl, plain);
                final int[] above = { ctrl[0], ctrl[1] + 1, ctrl[2] };
                MeParts.block(world, above, cell);
                final net.minecraft.tileentity.TileEntity te = world.getTileEntity(above[0], above[1], above[2]);
                if (te instanceof appeng.tile.networking.TileEnergyCell) {
                    final net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    te.writeToNBT(tag);
                    tag.setDouble("internalCurrentPower", ae);
                    te.readFromNBT(tag);
                }
                out.add(
                    "plain ME controller " + RenderTest
                        .pos(ctrl) + ", Energy Cell " + RenderTest.pos(above) + " " + ae + " AE");
                break;
            }
            default:
                break;
        }
        status(eye, world, out);
        return out;
    }

    private static MTEHatchInputBusME meBus(World world) {
        return bus == null ? null : AutoRig.mteAt(world, bus, MTEHatchInputBusME.class);
    }

    /** "core" (the next transition core), "charge" (a Stellar Ignition Charge) or "cobble". */
    private static ItemStack item(MTEEyeOfHarmonyBTS eye, String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "core": {
                final Item core = EyeStarRuntime.coreFor(
                    eye.starRuntime()
                        .unlockedTier());
                return core == null ? null : new ItemStack(core);
            }
            case "charge":
                return SLItems.stellarIgnitionCharge == null ? null : new ItemStack(SLItems.stellarIgnitionCharge);
            case "cobble":
                return new ItemStack(net.minecraft.init.Blocks.cobblestone);
            default:
                return null;
        }
    }

    private static String inject(World world, ItemStack what, long n) {
        final IStorageGrid s = MeParts.storage(world, cable);
        if (s == null) return "0 (no grid yet: try again in a second)";
        final appeng.api.storage.data.IAEItemStack left = s.getItemInventory()
            .injectItems(MeParts.item(what, n), Actionable.MODULATE, new BaseActionSource());
        return String.valueOf(n - (left == null ? 0 : left.getStackSize()));
    }

    private static void setup(MTEEyeOfHarmonyBTS eye, World world, List<String> out) {
        MTEHatchInputBus first = null;
        for (MTEHatchInputBus b : eye.itemBuses()) {
            if (b != null && b.isValid()) {
                first = b;
                break;
            }
        }
        if (first == null) {
            out.add("no plinth input bus to replace");
            return;
        }
        final IGregTechTileEntity hb = first.getBaseMetaTileEntity();
        final ForgeDirection face = hb.getFrontFacing();
        bus = new int[] { hb.getXCoord(), hb.getYCoord(), hb.getZCoord() };
        final ItemStack meItem = ItemList.Hatch_Input_Bus_ME.get(1L);
        if (meItem == null
            || !MTEReactorCoreMk1.placeGtMachine(world, bus[0], bus[1], bus[2], (short) meItem.getItemDamage(), face)) {
            out.add("could not place an ME input bus at " + RenderTest.pos(bus));
            return;
        }
        ctrl = step(bus, face, 1);
        drive = step(bus, face, 2);
        cable = step(bus, face, 3);
        KerrTestBench.keep(
            world,
            new int[] { Math.min(bus[0], cable[0]) - 1, 0, Math.min(bus[2], cable[2]) - 1,
                Math.max(bus[0], cable[0]) + 1, 255, Math.max(bus[2], cable[2]) + 2 });
        MeParts.block(world, ctrl, MeParts.controllerBlock());
        MeParts.block(world, drive, MeParts.driveBlock());
        MeParts.cable(
            world,
            cable,
            MeParts.Kind.GLASS,
            AEColor.Transparent,
            FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world));
        Item cellItem = AEApi.instance()
            .definitions()
            .items()
            .cell64k()
            .maybeItem()
            .orNull();
        final List<String> log = new ArrayList<>();
        final ItemStack cellStack = MeParts
            .filledCell(cellItem == null ? null : new ItemStack(cellItem), StorageChannel.ITEMS, log, "cell: ");
        MeParts.intoDrive(world, drive, 0, cellStack);
        out.addAll(log);
        eye.checkStructure(true, eye.getBaseMetaTileEntity());
        out.add(
            "ME input bus " + RenderTest.pos(bus)
                + " facing "
                + face
                + ", creative ME controller "
                + RenderTest.pos(ctrl)
                + ", drive "
                + RenderTest.pos(drive)
                + ", cable "
                + RenderTest.pos(cable)
                + "; Eye re-walked: formed "
                + eye.mMachine
                + ", input buses "
                + eye.itemBusCount());
    }

    private static int[] step(int[] p, ForgeDirection d, int n) {
        return new int[] { p[0] + d.offsetX * n, p[1] + d.offsetY * n, p[2] + d.offsetZ * n };
    }

    private static void status(MTEEyeOfHarmonyBTS eye, World world, List<String> out) {
        if (bus == null) {
            out.add("mebus: no rig (setup)");
            return;
        }
        final MTEHatchInputBusME me = meBus(world);
        final StringBuilder sb = new StringBuilder("mebus: network");
        for (String name : new String[] { "core", "charge", "cobble" }) {
            final ItemStack what = item(eye, name);
            sb.append(' ')
                .append(name)
                .append(' ')
                .append(what == null ? "-" : String.valueOf(MeParts.count(world, cable, what)));
        }
        final ItemStack manual = me == null ? null
            : me.getBaseMetaTileEntity()
                .getStackInSlot(me.getManualSlot());
        sb.append("; manual slot ")
            .append(manual == null ? "empty" : manual.stackSize + " " + manual.getDisplayName());
        final ItemStack escrow = eye.starRuntime()
            .escrow();
        sb.append("; escrow ")
            .append(escrow == null ? "empty" : escrow.getDisplayName());
        sb.append("; bus ")
            .append(me == null ? "MISSING" : me.isAllowedToWork() ? "active" : "inactive");
        out.add(sb.toString());
    }
}
