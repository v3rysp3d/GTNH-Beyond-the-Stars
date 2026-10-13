package com.nateyburr.beyondthestars.machines.quasar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * P4: the Quasar Engine's creative test path. {@code /btskit quasar} hands over the controller, coils, hatches and
 * seeds and prints the bill of materials; {@code /btsquasar} raises a complete, formed quasar (the creative hologram or
 * the projector's survival path), fits a hatch in every socket the standard rig uses, and can seed it.
 * <p>
 * <b>No count or position here is typed by hand (D-22):</b> sockets and bills come from {@code manifest.json} and the
 * exported shape, the same files the machine walks.
 */
public final class QuasarKit {

    private QuasarKit() {}

    // ===================================================================================== kit

    /** {@code /btskit quasar}: the controller, the coils of one tier, hatches, seeds, the gases' cells by NEI. */
    public static void giveKit(EntityPlayerMP player, int coilTier) {
        final int tier = Math.max(1, Math.min(4, coilTier));
        final int coils = BTSShapes.count(MTEQuasarEngine.PIECE, 'H');
        give(player, copy(BTSMachines.quasarEngine));
        giveMany(player, SLBlocks.casing2(BlockSLCasing2.discCoil(tier), 1), coils);
        give(player, new ItemStack(SLQuasarItems.quasarSeed, 1));
        give(player, ItemList.Black_Hole_Opener.get(1L));
        giveMany(player, ItemList.Hatch_Input_MAX.get(1L), 4 + 2);
        giveMany(player, ItemList.Hatch_Energy_UXV.get(1L), 3);
        giveMany(player, copy(BTSMachines.burstControlHatch), 2);
        giveMany(player, copy(BTSMachines.stellarFieldSensor), 2);
        giveMany(player, copy(BTSMachines.directiveHatch), 2);
        player.inventoryContainer.detectAndSendChanges();

        tell(player, EnumChatFormatting.AQUA + "== Quasar Engine test kit (coils T" + tier + ") ==");
        tell(
            player,
            "Given: the controller, " + qty(coils)
                + " Disc Field Coils (T"
                + tier
                + "), a Quasar Seed and a Black Hole Seed, 6 input hatches (4 gas + 2 trim), 3 energy hatches,"
                + " 2 Burst Control, 2 Sensor and 2 Directive Hatches.");
        tell(player, EnumChatFormatting.YELLOW + "Grab the casings through NEI. Bill read from manifest.json:");
        tell(player, " - quasar: " + bill(MTEQuasarEngine.PIECE));
        tell(
            player,
            EnumChatFormatting.GREEN + "Fastest look: /btsquasar [coil 1-4] [seed] raises a formed quasar in front of"
                + " you. By hand: place the controller, project it (stack or the coil channel 1-4 = coil tier), fit"
                + " the hatches, put the Quasar Seed in the controller slot and press Open. Feed Helium into the rim"
                + " input hatches; give it laser power (2 A UXV x b^2).");
    }

    /** "51,984 blocks: 27,815x Eddington Wall Casing, ..." from the manifest's bill of materials. */
    static String bill(String piece) {
        final JsonObject entry = BTSShapes.manifestPiece(piece);
        if (entry == null || !entry.has("bom")) return "(not in manifest)";
        final StringBuilder sb = new StringBuilder();
        for (JsonElement el : entry.getAsJsonArray("bom")) {
            final JsonObject row = el.getAsJsonObject();
            final String ch = row.get("ch")
                .getAsString();
            if ("~".equals(ch) || Character.isLowerCase(ch.charAt(0))) continue;
            if (sb.length() > 0) sb.append(", ");
            String block = row.get("block")
                .getAsString()
                .replace("NEW ", "");
            final int paren = block.indexOf(" (");
            if (paren > 0) block = block.substring(0, paren);
            final int comma = block.indexOf(',');
            if (comma > 0) block = block.substring(0, comma);
            sb.append(
                qty(
                    row.get("count")
                        .getAsInt()))
                .append("x ")
                .append(block);
        }
        return sb.toString();
    }

    /** Every socket of one char, as (a, b, c) offsets from the controller ({@code sockets.<ch>[i].rel}). */
    public static int[][] socketRels(char ch) {
        final JsonObject entry = BTSShapes.manifestPiece(MTEQuasarEngine.PIECE);
        if (entry == null || !entry.has("sockets")) return new int[0][];
        final JsonObject sockets = entry.getAsJsonObject("sockets");
        final String key = String.valueOf(ch);
        if (!sockets.has(key)) return new int[0][];
        final JsonArray list = sockets.getAsJsonArray(key);
        final int[][] out = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            final JsonArray rel = list.get(i)
                .getAsJsonObject()
                .getAsJsonArray("rel");
            out[i] = new int[] { rel.get(0)
                .getAsInt(),
                rel.get(1)
                    .getAsInt(),
                rel.get(2)
                    .getAsInt() };
        }
        return out;
    }

    // ========================================================================= creative build

    /** Result of one {@link #build}. */
    public static final class Result {

        public boolean formed;
        public String failure;
        public final List<String> lines = new ArrayList<>();
    }

    /** The world box {minX, minY, minZ, maxX, maxY, maxZ} of a quasar with its controller at (x, y, z). */
    public static int[] bounds(ForgeDirection facing, int x, int y, int z) {
        return SLConsoleTest.bounds(ExtendedFacing.of(facing), x, y, z, MTEQuasarEngine.PIECE);
    }

    /**
     * Raises a complete quasar with its controller at (x, y, z) facing {@code facing}: the structure at coil tier
     * {@code coilTier} (creative {@code construct}, or the projector's survival path), then a hatch in the sockets the
     * standard rig uses: creative energy (UXV, 64 A) in every energy socket, plain Input Hatches (MAX) in 4 gas sockets
     * filled with Helium and in 2 trim sockets (empty), a Burst Control Hatch on each cap (purge mode), two Sensor
     * Hatches (E analog, flux above 90) and two Directive Hatches (Disc set, code). With {@code seed} a Quasar Seed
     * goes in the controller slot and the hole is opened. Server side only.
     */
    public static Result build(World world, int x, int y, int z, ForgeDirection facing, int coilTier, boolean survival,
        boolean seed) {
        final Result result = new Result();
        final ItemStack controller = BTSMachines.quasarEngine;
        if (controller == null) {
            result.failure = "the Quasar Engine is not registered";
            return result;
        }
        if (!MTEReactorCoreMk1.placeGtMachine(world, x, y, z, (short) controller.getItemDamage(), facing)) {
            result.failure = "could not place the controller";
            return result;
        }
        final MTEQuasarEngine quasar = mteAt(world, new int[] { x, y, z }, MTEQuasarEngine.class);
        if (quasar == null) {
            result.failure = "the controller did not initialise";
            return result;
        }
        final IGregTechTileEntity base = quasar.getBaseMetaTileEntity();
        final int tier = Math.max(1, Math.min(4, coilTier));
        final ItemStack trigger = new ItemStack(controller.getItem(), tier, controller.getItemDamage());
        final long start = System.nanoTime();
        if (survival) {
            final SLConsoleTest.SurvivalRun run = SLConsoleTest
                .survivalBuild(quasar, world, trigger, SURVIVAL_BUDGET, SURVIVAL_MAX_ROUNDS);
            result.lines.add(
                "Survival auto-place: " + qty(run.placed)
                    + " blocks in "
                    + run.rounds
                    + " rounds ("
                    + (System.nanoTime() - start) / 1_000_000L
                    + " ms), "
                    + (run.finished ? "finished (-1)" : run.stalled ? "STALLED (0 placed twice)" : "round cap hit"));
        } else {
            quasar.construct(trigger, false);
            result.lines.add("Creative hologram: " + (System.nanoTime() - start) / 1_000_000L + " ms");
        }
        result.lines.addAll(fitHatches(world, quasar));
        quasar.checkStructure(true, base);
        base.enableWorking();
        if (seed && quasar.mMachine) {
            result.lines.add(seedFromSlot(quasar));
        }
        result.formed = quasar.mMachine;
        result.lines.add(
            (quasar.mMachine ? EnumChatFormatting.GREEN + "Quasar FORMED"
                : EnumChatFormatting.RED + "Quasar NOT FORMED") + EnumChatFormatting.RESET
                + " at "
                + pos(x, y, z)
                + ", facing "
                + facing.name()
                + ", coils T"
                + quasar.getCoilTier());
        return result;
    }

    /** Puts a Quasar Seed in the controller slot and presses Open. */
    public static String seedFromSlot(MTEQuasarEngine quasar) {
        if (SLQuasarItems.quasarSeed == null) return "No Quasar Seed item registered";
        quasar.mInventory[quasar.getControllerSlotIndex()] = new ItemStack(SLQuasarItems.quasarSeed, 1);
        final String why = quasar.openHole();
        return why.isEmpty() ? "Seeded from the controller slot: " + quasar.statusLines()
            .get(1) : "Open refused: " + why;
    }

    /** The hatches the standard rig uses; returns what it fitted. Sockets that already hold a tile are skipped. */
    public static List<String> fitHatches(World world, MTEQuasarEngine quasar) {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity base = quasar.getBaseMetaTileEntity();
        final ExtendedFacing alignment = quasar.getExtendedFacing();
        final int x = base.getXCoord(), y = base.getYCoord(), z = base.getZCoord();
        final int[] centre = QuasarGeometry.world(alignment, x, y, z, QuasarGeometry.centreRel());
        int energy = 0, gas = 0, trim = 0, burst = 0, sensor = 0, directive = 0;
        for (int[] rel : socketRels('e')) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, rel);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (!place(world, p, ItemList.DebugEnergyHatch.get(1L), outward(world, alignment, p, centre))) continue;
            final MTEHatchEnergyDebug hatch = mteAt(world, p, MTEHatchEnergyDebug.class);
            if (hatch != null) {
                hatch.setVoltageTier(13);
                hatch.setAmperage(64);
            }
            energy++;
        }
        final Fluid helium = MTEQuasarEngine.gasFluid(QuasarGas.HE);
        final int[][] gasSockets = socketRels('h');
        for (int i = 0; i < gasSockets.length && gas < 4; i++) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, gasSockets[i]);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (!place(world, p, ItemList.Hatch_Input_MAX.get(1L), outward(world, alignment, p, centre))) continue;
            final MTEHatchInput h = mteAt(world, p, MTEHatchInput.class);
            if (h != null && helium != null) h.fill(new FluidStack(helium, Integer.MAX_VALUE), true);
            gas++;
        }
        final int[][] trimSockets = socketRels('t');
        for (int i = 0; i < trimSockets.length && trim < 2; i++) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, trimSockets[i]);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (place(world, p, ItemList.Hatch_Input_MAX.get(1L), outward(world, alignment, p, centre))) trim++;
        }
        // one Burst Control Hatch per cap (the c sockets come top, bottom, top, bottom)
        final int[][] capSockets = socketRels('c');
        for (int i = 0; i < capSockets.length && burst < 2; i++) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, capSockets[i]);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (place(world, p, BTSMachines.burstControlHatch, outward(world, alignment, p, centre))) burst++;
        }
        final int[][] sensorSockets = socketRels('n');
        for (int i = 0; i < sensorSockets.length; i++) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, sensorSockets[i]);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (!place(world, p, BTSMachines.stellarFieldSensor, alignment.getDirection())) continue;
            final MTEHatchStellarFieldSensor s = mteAt(world, p, MTEHatchStellarFieldSensor.class);
            if (s != null) {
                if (i == 0) {
                    s.setChannelOrdinal(QuasarSensorChannel.E.ordinal());
                    s.setAnalog(true);
                    s.setWindow(30);
                } else {
                    s.setChannelOrdinal(QuasarSensorChannel.PHI.ordinal());
                    s.setThreshold(90);
                }
            }
            sensor++;
        }
        for (int[] rel : socketRels('d')) {
            final int[] p = QuasarGeometry.world(alignment, x, y, z, rel);
            if (world.getTileEntity(p[0], p[1], p[2]) != null) continue;
            if (!place(world, p, BTSMachines.directiveHatch, alignment.getDirection())) continue;
            final MTEHatchDirective h = mteAt(world, p, MTEHatchDirective.class);
            if (h != null) {
                h.setSet(DirectiveSet.QUASAR_DISC);
                h.setOutput(DirectiveSet.Output.CODE);
                h.setWindow(5);
            }
            directive++;
        }
        out.add(
            "Hatches fitted: " + energy
                + " creative energy (UXV 64 A), "
                + gas
                + " gas (Helium), "
                + trim
                + " trim, "
                + burst
                + " Burst Control, "
                + sensor
                + " sensor, "
                + directive
                + " directive");
        return out;
    }

    /** Fills every plain gas hatch with the selected gas again (bench). Returns how many it filled. */
    public static int refillGas(MTEQuasarEngine quasar, World world) {
        final Fluid fluid = MTEQuasarEngine.gasFluid(quasar.getGas());
        if (fluid == null) return 0;
        final IGregTechTileEntity base = quasar.getBaseMetaTileEntity();
        int n = 0;
        for (int[] rel : socketRels('h')) {
            final int[] p = QuasarGeometry
                .world(quasar.getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
            final MTEHatchInput h = mteAt(world, p, MTEHatchInput.class);
            if (h != null && h.fill(new FluidStack(fluid, Integer.MAX_VALUE), true) > 0) n++;
        }
        return n;
    }

    /**
     * Puts one litre-unit's worth of trim fluid into the trim hatches (bench): {@code gasUnits} / {@code spaceUnits}.
     */
    public static String fillTrims(MTEQuasarEngine quasar, World world, int gasUnits, int spaceUnits) {
        final IGregTechTileEntity base = quasar.getBaseMetaTileEntity();
        final double m = quasar.state().M;
        final Fluid gas = MTEQuasarEngine.gasFluid(quasar.getGas());
        final Fluid space = MTEQuasarEngine.spaceFluid();
        int i = 0;
        long gasL = 0, spaceL = 0;
        for (int[] rel : socketRels('t')) {
            final int[] p = QuasarGeometry
                .world(quasar.getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
            final MTEHatchInput h = mteAt(world, p, MTEHatchInput.class);
            if (h == null) continue;
            if (i == 0 && gas != null && gasUnits > 0) {
                gasL = (long) Math.ceil(gasUnits * 1000 * m / 64);
                h.fill(new FluidStack(gas, (int) Math.min(Integer.MAX_VALUE, gasL)), true);
            } else if (i == 1 && space != null && spaceUnits > 0) {
                spaceL = (long) Math.ceil(spaceUnits * 100 * m / 64);
                h.fill(new FluidStack(space, (int) Math.min(Integer.MAX_VALUE, spaceL)), true);
            }
            i++;
        }
        return "Trim hatches: " + qty((int) gasL) + " L feed gas, " + qty((int) spaceL) + " L Spatially Enlarged Fluid";
    }

    private static final int SURVIVAL_BUDGET = 25;
    private static final int SURVIVAL_MAX_ROUNDS = 4000;

    // ============================================================================ diagnostics

    /**
     * Console checks on a quasar {@link #build} raised: the walk (first wrong cells), then with {@code full} a creative
     * re-projection over the formed machine (nothing may break) and the survival damage-and-repair test.
     */
    public static List<String> diagnose(World world, int x, int y, int z, boolean full) {
        final List<String> out = new ArrayList<>();
        final MTEQuasarEngine quasar = mteAt(world, new int[] { x, y, z }, MTEQuasarEngine.class);
        if (quasar == null) {
            out.add("No Quasar Engine controller at " + pos(x, y, z));
            return out;
        }
        final IGregTechTileEntity base = quasar.getBaseMetaTileEntity();
        quasar.checkStructure(true, base);
        out.add("Quasar at " + pos(x, y, z) + ": " + (quasar.mMachine ? "FORMED" : "NOT FORMED"));
        if (!quasar.mMachine) {
            out.add("First wrong cells:");
            for (String line : SLConsoleTest.firstErrors(quasar, base, 3)) out.add("  " + line);
            quasar.checkStructure(true, base);
        }
        if (full && quasar.mMachine) {
            final ItemStack c = BTSMachines.quasarEngine;
            final ItemStack trigger = new ItemStack(c.getItem(), Math.max(1, quasar.getCoilTier()), c.getItemDamage());
            quasar.construct(trigger, false);
            quasar.checkStructure(true, base);
            out.add(
                "Creative re-projection over the formed quasar: " + (quasar.mMachine ? "still FORMED"
                    : "BROKE IT - " + String.join("; ", SLConsoleTest.firstErrors(quasar, base, 2))));
            if (quasar.mMachine) {
                out.addAll(
                    SLConsoleTest.damageAndRepair(
                        quasar,
                        base,
                        trigger,
                        bounds(
                            quasar.getExtendedFacing()
                                .getDirection(),
                            x,
                            y,
                            z)));
            }
        }
        out.addAll(quasar.statusLines());
        return out;
    }

    // ================================================================================ helpers

    public static MTEQuasarEngine quasarAt(World world, int x, int y, int z) {
        return mteAt(world, new int[] { x, y, z }, MTEQuasarEngine.class);
    }

    static <T> T mteAt(World world, int[] p, Class<T> type) {
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return type.isInstance(mte) ? type.cast(mte) : null;
    }

    private static boolean place(World world, int[] p, ItemStack stack, ForgeDirection facing) {
        if (stack == null) return false;
        return MTEReactorCoreMk1.placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing);
    }

    /** The air-facing side of a socket that points most directly away from the hole. */
    private static ForgeDirection outward(World world, ExtendedFacing alignment, int[] p, int[] c) {
        ForgeDirection best = alignment.getDirection();
        double bestDot = Double.NEGATIVE_INFINITY;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            final int nx = p[0] + d.offsetX;
            final int ny = p[1] + d.offsetY;
            final int nz = p[2] + d.offsetZ;
            if (!world.isAirBlock(nx, ny, nz)) continue;
            final double dot = d.offsetX * (p[0] - c[0]) + d.offsetY * (p[1] - c[1]) + d.offsetZ * (p[2] - c[2]);
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static void give(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) player.entityDropItem(stack, 0.5F);
    }

    private static void giveMany(EntityPlayerMP player, ItemStack one, int count) {
        if (one == null || count <= 0) return;
        final int max = Math.max(1, one.getMaxStackSize());
        int left = count;
        while (left > 0) {
            final ItemStack stack = one.copy();
            stack.stackSize = Math.min(max, left);
            give(player, stack);
            left -= stack.stackSize;
        }
    }

    private static void tell(EntityPlayerMP player, String message) {
        player.addChatMessage(new ChatComponentText(message));
    }

    static String qty(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    static String pos(int x, int y, int z) {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
