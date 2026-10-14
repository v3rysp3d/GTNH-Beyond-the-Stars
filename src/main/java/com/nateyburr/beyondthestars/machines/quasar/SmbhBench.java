package com.nateyburr.beyondthestars.machines.quasar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing4;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.ConduitKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrTestBench;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * SMBH v1 test path (smbh-v1.md §10.4): {@code /btskit lattice [1-4]} hands over a Supermassive Lattice's parts, and
 * the op-only bench verbs of {@code /btssmbh} raise and drive a lattice quasar through real code paths. Every count
 * and position comes from {@code manifest.json} and the exported shapes (D-22), as {@link QuasarKit}'s.
 * <p>
 * Nothing here is gameplay: the bench sets things by script (a seeded hole, Saturation, filled hatches) and says so,
 * so a film can caption it honestly.
 */
public final class SmbhBench {

    private SmbhBench() {}

    /** The last bench quasar, so {@code mass M} / {@code fill} / {@code drain anchor} may omit x y z. */
    private static World lastWorld;
    private static int[] lastPos;

    // ===================================================================================== kit

    /** {@code /btskit lattice [tier]}: the lattice's frame and nodes, a Mk IV crown's parts, a Mk IV throat. */
    public static void giveKit(EntityPlayerMP player, int tier) {
        final int t = Math.max(1, Math.min(QuasarParams.LATTICE_MAX, tier));
        final int frames = BTSShapes.count(MTEQuasarEngine.PIECE_LATTICE, 'L');
        final int nodes = BTSShapes.count(MTEQuasarEngine.PIECE_LATTICE, 'N');
        final String crown = MTEBurstThermaliserCrown.PIECES[MTEBurstThermaliserCrown.MK_MAX - 1];
        QuasarKit.giveMany(player, SLBlocks.casing4(BlockSLCasing4.LATTICE_FRAME, 1), frames);
        QuasarKit.giveMany(player, SLBlocks.casing4(BlockSLCasing4.node(t), 1), nodes);
        QuasarKit.giveMany(player, copy(BTSMachines.burstThermaliserCrown), 1);
        QuasarKit
            .giveMany(player, SLBlocks.casing4(BlockSLCasing4.HORIZON_THERMALISER, 1), BTSShapes.count(crown, 'X'));
        QuasarKit.giveMany(player, SLBlocks.casing2(BlockSLCasing2.RADIATOR_PETAL, 1), BTSShapes.count(crown, 'R'));
        QuasarKit.giveMany(player, SLBlocks.casing2(BlockSLCasing2.JET_COLLIMATOR, 1), BTSShapes.count(crown, 'C'));
        if (SLQuasarItems.conduitThroatMk4 != null) {
            QuasarKit.giveMany(player, new ItemStack(SLQuasarItems.conduitThroatMk4, 1), 1);
        }
        player.inventoryContainer.detectAndSendChanges();
        tell(player, EnumChatFormatting.AQUA + "== Supermassive Lattice kit (L" + t + ") ==");
        tell(
            player,
            "Given: " + QuasarKit.qty(frames)
                + " Lattice Frame Casings, "
                + nodes
                + " Containment Nodes (L"
                + t
                + ", "
                + QuasarParams.LATTICE_NAME[t]
                + "), a crown controller with the Mk IV parts, a Conduit Throat Mk IV.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bills read from manifest.json:");
        tell(player, " - quasar_lattice: " + QuasarKit.bill(MTEQuasarEngine.PIECE_LATTICE));
        tell(player, " - " + crown + ": " + QuasarKit.bill(crown));
        tell(
            player,
            EnumChatFormatting.GREEN + "The lattice upgrades a Quasar Engine in place: project the controller with the"
                + " lattice channel set to "
                + t
                + " (MagMatter coils). It counts after Saturation: 4,096 SM held Stable for 1 h."
                + " Fastest look: /btsquasar l"
                + t
                + " saturate, or /btssmbh bench.");
    }

    // ===================================================================================== bench

    /**
     * {@code /btssmbh bench}: an L4-lattice quasar at (x, y, z) facing {@code facing} (creative build, every standard
     * hatch, the four trim sockets fitted), Saturation set, a 4,096 SM hole seeded (spin 0.9), the buffer and every
     * upkeep hatch filled, and a Mk IV crown docked on each jet port. Chunks kept loaded. Server side only.
     */
    public static List<String> bench(World world, int x, int y, int z, ForgeDirection facing) {
        final List<String> out = new ArrayList<>();
        final int[] box = QuasarKit.bounds(facing, x, y, z, true);
        SLConsoleTest.loadChunks(world, box);
        KerrTestBench.keep(world, box);
        final QuasarKit.Result r = QuasarKit.build(world, x, y, z, facing, 4, QuasarParams.LATTICE_MAX, false, false);
        if (r.failure != null) {
            out.add("Bench: BUILD FAILED - " + r.failure);
            return out;
        }
        out.addAll(r.lines);
        final MTEQuasarEngine q = QuasarKit.quasarAt(world, x, y, z);
        if (q == null) {
            out.add("Bench: no controller after the build");
            return out;
        }
        lastWorld = world;
        lastPos = new int[] { x, y, z };
        // an owner, so GT's power-fail tracker has someone to tell (bench only)
        q.getBaseMetaTileEntity()
            .setOwnerUuid(ConduitKit.BENCH_OWNER);
        out.add("Set up by script: Saturation -> lattice in force L" + q.debugSaturate(true));
        if (!q.isFormed()) {
            out.add(EnumChatFormatting.RED + "Bench: the lattice quasar is NOT FORMED");
            return out;
        }
        q.debugSeed(QuasarParams.M_REF, 0.9);
        q.setGas(com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas.HE);
        out.add("Set up by script: a " + MTEQuasarEngine.fmt(q.getMass()) + " SM hole, spin 0.9, Helium");
        out.addAll(fill(world, q));
        out.addAll(ConduitKit.crowns(world, q, MTEBurstThermaliserCrown.MK_MAX, false));
        out.addAll(q.statusLines());
        return out;
    }

    /**
     * {@code /btssmbh fill}: a plain MAX Input Hatch in every empty trim socket, then the trims hold (in socket order)
     * Molten MHDCSM, Molten Universium, Primordial Matter and Spatially Enlarged Fluid, each to capacity; the gas
     * hatches are refilled with the selected gas and the feed buffer topped up.
     */
    public static List<String> fill(World world, MTEQuasarEngine q) {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity base = q.getBaseMetaTileEntity();
        final ExtendedFacing alignment = q.getExtendedFacing();
        final int x = base.getXCoord(), y = base.getYCoord(), z = base.getZCoord();
        final int[] centre = QuasarGeometry.world(alignment, x, y, z, QuasarGeometry.centreRel());
        final Fluid[] fluids = { MTEQuasarEngine.mhdcsmFluid(), MTEQuasarEngine.universiumFluid(),
            MTEQuasarEngine.primordialFluid(), MTEQuasarEngine.spaceFluid() };
        final int[][] trims = QuasarKit.socketRels('t');
        int placed = 0;
        final StringBuilder held = new StringBuilder();
        for (int i = 0; i < trims.length; i++) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, trims[i]);
            if (world.getTileEntity(p[0], p[1], p[2]) == null && QuasarKit
                .place(world, p, ItemList.Hatch_Input_MAX.get(1L), QuasarKit.outward(world, alignment, p, centre))) {
                placed++;
            }
            final MTEHatchInput h = QuasarKit.mteAt(world, p, MTEHatchInput.class);
            final Fluid f = i < fluids.length ? fluids[i] : null;
            if (h == null || f == null) continue;
            final FluidStack now = h.getFluid();
            if (now != null && now.getFluid() != f) h.drain(ForgeDirection.UNKNOWN, now.copy(), true);
            final int got = h.fill(new FluidStack(f, Integer.MAX_VALUE), true);
            final FluidStack after = h.getFluid();
            if (held.length() > 0) held.append(", ");
            held.append(f.getLocalizedName(new FluidStack(f, 1)))
                .append(' ')
                .append(QuasarKit.qty(after == null ? 0 : after.amount))
                .append(" L")
                .append(got > 0 ? "" : " (full)");
        }
        final int gas = QuasarKit.refillGas(q, world);
        q.debugFillBuffer();
        out.add(
            "Set up by script: " + placed
                + " trim hatch(es) placed; trims hold "
                + held
                + "; "
                + gas
                + " gas hatch(es) refilled, feed buffer full");
        return out;
    }

    /** {@code /btssmbh drain anchor}: every hatch on the quasar loses its Primordial Matter (the anchor is cut). */
    public static String drainAnchor(World world, MTEQuasarEngine q) {
        final Fluid pm = MTEQuasarEngine.primordialFluid();
        if (pm == null) return "Primordial Matter is not registered";
        final IGregTechTileEntity base = q.getBaseMetaTileEntity();
        long drained = 0;
        for (char socket : new char[] { 't', 'h' }) {
            for (int[] rel : QuasarKit.socketRels(socket)) {
                final int[] p = QuasarGeometry
                    .world(q.getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
                final MTEHatchInput h = QuasarKit.mteAt(world, p, MTEHatchInput.class);
                if (h == null) continue;
                final FluidStack s = h.getFluid();
                if (s == null || s.getFluid() != pm) continue;
                final FluidStack d = h.drain(ForgeDirection.UNKNOWN, s.copy(), true);
                if (d != null) drained += d.amount;
            }
        }
        return "Set up by script: the anchor is cut, " + String.format(Locale.ROOT, "%,d", drained)
            + " L of Primordial Matter drained (missing upkeep only sheds mass: nothing is damaged)";
    }

    /** The bench quasar at (x, y, z), or (with null) the last one {@link #bench} raised. */
    public static MTEQuasarEngine quasar(World world, int[] at) {
        if (at != null) return QuasarKit.quasarAt(world, at[0], at[1], at[2]);
        if (lastPos == null || lastWorld != world) return null;
        return QuasarKit.quasarAt(world, lastPos[0], lastPos[1], lastPos[2]);
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }
}
