package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.PrivateHatchFluids;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;

import appeng.api.AEApi;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.item.AEFluidStack;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * DEV-ONLY bench for the private-socket ME reads and drains (QA-B02, B03, B07): {@code /btsquasar mestock x y z
 * <fluids> <litres> <ae>} on the console turns the input hatch AT x y z (a quasar gas socket, a conduit fuel socket, a
 * Region Anchor socket) into an ME stocking input hatch configured to {@code fluids} (comma list: registry names or
 * he, h, espm, hsm, primordial, space, spacetime, ergomatter), on a NORMAL (not creative) AE2 network built
 * straight out from the hatch's face: an ME controller, an ME drive with one fluid cell per fluid holding
 * {@code litres} each, and an energy cell charged with {@code ae} AE. {@code /btsquasar mestock x y z status} prints
 * the network's stock of every configured fluid, its stored AE, and what {@link PrivateHatchFluids} reads. Loaded only
 * by that command.
 */
public final class MeStockBench {

    private MeStockBench() {}

    public static List<String> run(World world, int x, int y, int z, String[] rest) {
        final List<String> out = new ArrayList<>();
        final int[] p = { x, y, z };
        if (rest.length >= 3 && rest[0].equalsIgnoreCase("sockets")) {
            // the world cells of a socket character of a piece, for the controller at x y z (its facing)
            final TileEntity te = world.getTileEntity(x, y, z);
            final Object mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
                : null;
            if (!(mte instanceof IAlignmentProvider)) {
                out.add("no multiblock controller at " + MeParts.pos(p));
                return out;
            }
            final ExtendedFacing f = ((IAlignmentProvider) mte).getAlignment()
                .getExtendedFacing();
            final int[][] rels = BTSShapes.socketRels(rest[1], rest[2].charAt(0));
            final StringBuilder sb = new StringBuilder(rest[1] + " '" + rest[2].charAt(0) + "' (" + rels.length + "):");
            for (int i = 0; i < Math.min(8, rels.length); i++) {
                sb.append(' ')
                    .append(MeParts.pos(SLConsoleTest.worldPos(f, x, y, z, rels[i])));
            }
            out.add(sb.toString());
            return out;
        }
        if (rest.length >= 1 && rest[0].equalsIgnoreCase("status")) {
            status(world, p, out);
            return out;
        }
        if (rest.length < 3) {
            out.add("mestock x y z <fluid,fluid> <litres each> <ae> | mestock x y z status");
            return out;
        }
        final List<Fluid> fluids = new ArrayList<>();
        for (String n : rest[0].split(",")) {
            final Fluid f = fluid(n.trim());
            if (f == null) {
                out.add("unknown fluid " + n);
                return out;
            }
            fluids.add(f);
        }
        setup(world, p, fluids, (long) Double.parseDouble(rest[1]), Double.parseDouble(rest[2]), out);
        status(world, p, out);
        return out;
    }

    static Fluid fluid(String n) {
        switch (n.toLowerCase(Locale.ROOT)) {
            case "he":
            case "helium":
                return MTEQuasarEngine.gasFluid(com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas.HE);
            case "h":
            case "hydrogen":
                return MTEQuasarEngine.gasFluid(com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas.H);
            case "espm":
                return MTEQuasarEngine.espmFluid();
            case "hsm":
                return MTEQuasarEngine.hsmFluid();
            case "primordial":
                return MTEQuasarEngine.primordialFluid();
            case "space":
                return MTEQuasarEngine.spaceFluid();
            case "spacetime":
                return MTEConduitElement.spaceTimeFluid();
            case "ergomatter":
                return MTEConduitElement.ergomatterFluid();
            default:
                return FluidRegistry.getFluid(n);
        }
    }

    private static void setup(World world, int[] p, List<Fluid> fluids, long litres, double ae, List<String> out) {
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) {
            out.add("no GT hatch at " + MeParts.pos(p));
            return;
        }
        final ForgeDirection face = ((IGregTechTileEntity) te).getFrontFacing();
        MTEReactorCoreMk1.placeGtMachine(
            world,
            p[0],
            p[1],
            p[2],
            (short) ItemList.Hatch_Input_ME.get(1L)
                .getItemDamage(),
            face);
        final MTEHatchInputME sh = MeParts.mteAt(world, p, MTEHatchInputME.class);
        if (sh == null) {
            out.add("could not place the ME stocking input hatch at " + MeParts.pos(p));
            return;
        }
        for (int i = 0; i < fluids.size(); i++) sh.setSlotConfig(i, new FluidStack(fluids.get(i), 1));
        final int[] ctrl = step(p, face, 1), drive = step(p, face, 2), cellAt = step(p, face, 3);
        MeParts.block(
            world,
            ctrl,
            AEApi.instance()
                .definitions()
                .blocks()
                .controller()
                .maybeBlock()
                .orNull());
        MeParts.block(world, drive, MeParts.driveBlock());
        // the energy cell: placed by the console with its charge as NBT (an AE tile's grid is not ready in the tick it
        // is placed): setblock <cell> appliedenergistics2:tile.BlockEnergyCell 0 replace {internalCurrentPower:<ae>d}
        final TileEntity cellTe = world.getTileEntity(cellAt[0], cellAt[1], cellAt[2]);
        final double stored = cellTe instanceof IAEPowerStorage ? ((IAEPowerStorage) cellTe).getAECurrentPower() : -1;
        final List<String> log = new ArrayList<>();
        Item cellItem = MeParts.ae2fc("QUANTUM_CELL");
        if (cellItem == null) cellItem = MeParts.ae2fc("CELL16384K");
        int cells = 0;
        for (Fluid f : fluids) {
            final ItemStack cell = MeParts.filledCell(
                cellItem == null ? null : new ItemStack(cellItem),
                StorageChannel.FLUIDS,
                log,
                "cell: ",
                MeParts.fluid(f, litres));
            if (MeParts.intoDrive(world, drive, cells, cell)) cells++;
        }
        out.addAll(log);
        out.add(
            "mestock: ME stocking hatch " + MeParts.pos(p)
                + " facing "
                + face
                + ", ME controller (normal) "
                + MeParts.pos(ctrl)
                + ", drive "
                + MeParts.pos(drive)
                + " ("
                + cells
                + " cells), energy cell "
                + MeParts.pos(cellAt)
                + (stored < 0
                    ? " (none yet: setblock " + cellAt[0]
                        + " "
                        + cellAt[1]
                        + " "
                        + cellAt[2]
                        + " appliedenergistics2:tile.BlockEnergyCell 0 replace {internalCurrentPower:"
                        + (long) ae
                        + ".0d})"
                    : " holding " + String.format(Locale.ROOT, "%,.0f", stored) + " AE"));
    }

    private static void status(World world, int[] p, List<String> out) {
        final MTEHatchInputME sh = MeParts.mteAt(world, p, MTEHatchInputME.class);
        if (sh == null) {
            out.add("no ME stocking input hatch at " + MeParts.pos(p));
            return;
        }
        final StringBuilder sb = new StringBuilder("mestock " + MeParts.pos(p) + ":");
        try {
            final IEnergyGrid e = sh.getProxy()
                .getEnergy();
            sb.append(String.format(Locale.ROOT, " network AE %,.0f;", e.getStoredPower()));
            for (int i = 0; i < MTEHatchInputME.SLOT_COUNT; i++) {
                final FluidStack c = sh.getSlotConfig(i);
                if (c == null) continue;
                final IAEFluidStack found = sh.getProxy()
                    .getStorage()
                    .getFluidInventory()
                    .getStorageList()
                    .findPrecise(AEFluidStack.create(new FluidStack(c.getFluid(), 1)));
                sb.append(' ')
                    .append(
                        c.getFluid()
                            .getName())
                    .append(String.format(Locale.ROOT, " network %,d L", found == null ? 0 : found.getStackSize()))
                    .append(String.format(Locale.ROOT, " (read %,d L)", PrivateHatchFluids.available(sh, c.getFluid())))
                    .append(';');
            }
        } catch (GridAccessException ex) {
            sb.append(" no grid");
        }
        sb.append(" active ")
            .append(sh.isActive())
            .append(", allowed ")
            .append(sh.isAllowedToWork());
        out.add(sb.toString());
    }

    private static int[] step(int[] p, ForgeDirection d, int n) {
        return new int[] { p[0] + d.offsetX * n, p[1] + d.offsetY * n, p[2] + d.offsetZ * n };
    }
}
