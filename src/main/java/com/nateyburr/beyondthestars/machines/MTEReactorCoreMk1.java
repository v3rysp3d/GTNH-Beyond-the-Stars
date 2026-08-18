package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.blocks.BlockReactorFuelColumn;
import com.nateyburr.beyondthestars.blocks.BlockTurbineHousing;
import com.nateyburr.beyondthestars.blocks.TileRodLoadingPort;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * The Plant Foundation - the Unit One MAIN STATION (Space Elevator architecture). The only
 * mandatory STARTER structure is the full 50x50 concrete platform: shield pad at center with
 * the controller on its front line (embedded in the silo face once the silo is built - every
 * silo tier's front wall is anchored on that line), and eight flush steel dock markers set
 * into the floor. Everything else is added ONTO the platform: the Steam Generator silo (an
 * optional piece of this controller, five diameters, Build Tier set in the GUI or by trigger
 * stack) comes first, then MODULE MACHINES whose controllers stand on the dock markers
 * (accepted like hatches, positions otherwise kept clear) and only function while connected -
 * Turbine Hall (steam -> EU with fan/coolant/propellant multipliers, beside the silo),
 * Cooling Towers (condensate return, faster heat decay). Fuel ladder, throttle, waste water,
 * and the SCRAM/decay-heat/meltdown model are unchanged. Numbers per docs/reactor-gen1.md and
 * docs/plant-unit-one.md.
 */
public class MTEReactorCoreMk1 extends MTEExtendedPowerMultiBlockBase<MTEReactorCoreMk1>
    implements ISurvivalConstructable {

    /** L/t of steam per full fuel column at 100% throttle. */
    private static final long STEAM_PER_COLUMN = 16_384L;
    private static final int SCRAM_HEAT = 8_000;
    private static final int MELTDOWN_HEAT = 10_000;
    private static final int HEAT_PER_STARVED_TICK = 4;
    private static final int HEAT_DECAY_PER_TICK = 2;
    private static final int DECAY_HEAT_TICKS = 24_000;
    private static final int DECAY_WATER_PER_TICK = 64;
    private static final int RADIATION_TICKS = 36_000;
    private static final int RADIATION_RADIUS = 12;
    private static final int RADIATION_LEVEL = 3;
    private static final ShutDownReason SCRAM_REASON = SimpleShutDownReason.ofCritical("beyondthestars.scram");
    private static final ShutDownReason MELTDOWN_REASON = SimpleShutDownReason.ofCritical("beyondthestars.meltdown");

    private static final int MAX_TIER = 5;
    /** Frame segments stacked per full fuel pillar (SD-1: rods cascade through these). */
    private static final int SEGMENTS_PER_COLUMN = 8;
    /** Rod type ids stored per pillar segment; 0 = empty. */
    private static final int ROD_NONE = 0, ROD_URANIUM = 1, ROD_MOX = 2, ROD_THORIUM = 3, ROD_NAQ = 4;
    /** Steam multiplier per rod type (index = rod type id). */
    private static final int[] ROD_MULT = { 0, 1, 2, 5, 9 };
    /**
     * Half-rate cycles one assembly batch buys in bus-fallback mode. Four slices at half rate
     * = 2 rod-units of steam per 3-rod assembly - a true 33% per-rod penalty vs the cascade.
     */
    private static final int FALLBACK_SLICES = 4;
    /** Octagon corner cut per tier (0 unused). */
    private static final int[] CUT = { 0, 2, 2, 3, 3, 4 };
    /** Interior fuel-column capacity per tier (0 unused). */
    private static final int[] COLUMN_CAP = { 0, 45, 77, 109, 157, 201 };

    private static IStructureDefinition<MTEReactorCoreMk1> STRUCTURE_DEFINITION = null;

    /** Plant modules docked in this silo's walls (Space Elevator pattern). */
    public final ArrayList<MTEPlantModuleBase> plantModules = new ArrayList<>();

    /** One fuel pillar: its world column position and the frame segments present. */
    private static final class ColumnInfo {

        final int x, z;
        /** Y of every frame segment, sorted ascending - rod index k lives at ys.get(k). */
        final List<Integer> ys = new ArrayList<>();
        int minY = Integer.MAX_VALUE;
        int height;

        ColumnInfo(int x, int z) {
            this.x = x;
            this.z = z;
        }

        static long key(int x, int z) {
            return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        }
    }

    /** Fuel/port cells recorded by the current structure walk (reset per probe). */
    private final List<int[]> walkFuelCells = new ArrayList<>();
    private final List<int[]> walkPortCells = new ArrayList<>();
    /** Housing sockets recorded by the duct walk: {distance from controller, meta}. */
    private final List<int[]> walkHousings = new ArrayList<>();

    /** Spine state, recomputed on every structure check (x1000 fixed-point multipliers). */
    private boolean spineBuilt;
    private int spineHousings;
    private int spineSwirl;
    private int spineSpeedMilli = 1000;
    private int spineEffMilli = 1000;
    private String spinePattern = "";
    /** Pillars of the formed silo, rebuilt after every successful structure check. */
    private final java.util.HashMap<Long, ColumnInfo> columns = new java.util.HashMap<>();
    private final List<int[]> portCells = new ArrayList<>();
    /** Rod contents per pillar, bottom-first and gap-free; persisted in NBT. */
    private final java.util.HashMap<Long, byte[]> columnRods = new java.util.HashMap<>();

    private int mCasing;
    private int reactorTier;
    private int fuelSegments;
    private int activeColumns;
    private boolean cascadeMode;
    private int liveColumnsLastCycle;
    /** Parked assembly-fallback credit: remaining half-rate slices and their parameters. */
    private int fallbackCyclesLeft;
    private int fallbackColumns;
    private int fallbackMult = 1;
    private ItemStack fallbackDepleted;
    private int throttleN = 24;
    private int throttleSetting = 24;
    private int buildTierSetting = 1;
    private int activeFuelMult = 1;
    private int heat;
    private int decayHeatTicks;
    private int radiationTicks;
    /** Depth of the radiation field's center behind the controller, frozen at meltdown time. */
    private int radiationCenterDepth;
    private long steamPerTick;
    private long lastEUOutput;
    private boolean starvedThisTick;
    private boolean wasRunningLastTick;

    public MTEReactorCoreMk1(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEReactorCoreMk1(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEReactorCoreMk1(this.mName);
    }

    // ------------------------------------------------------------------ modules

    public boolean addPlantModuleToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEPlantModuleBase) {
            return plantModules.add((MTEPlantModuleBase) mte);
        }
        return false;
    }

    private void disconnectModules() {
        for (MTEPlantModuleBase module : plantModules) {
            module.disconnect();
        }
    }

    private MTEModuleTurbineHall dockedTurbineHall() {
        for (MTEPlantModuleBase module : plantModules) {
            if (module instanceof MTEModuleTurbineHall && module.isOperational()) {
                return (MTEModuleTurbineHall) module;
            }
        }
        return null;
    }

    private int coolingTowerCount() {
        int count = 0;
        for (MTEPlantModuleBase module : plantModules) {
            if (module instanceof MTEModuleCoolingTower && module.isOperational()) {
                count = Math.min(2, count + 1);
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ structure

    /** Silo cross-section edge for a tier: 9, 11, 13, 15, 17. */
    private static int edgeFor(int tier) {
        return 7 + 2 * tier;
    }

    private static String pieceName(String base, int tier) {
        return base + ".tier" + tier + ".core";
    }

    private static String floorPieceName(String base) {
        return base + ".floor";
    }

    private static String ductPieceName(String base) {
        return base + ".duct";
    }

    private static boolean insideOct(int n, int k, int r, int x) {
        return Math.min(r, n - 1 - r) + Math.min(x, n - 1 - x) >= k;
    }

    private static boolean interiorCell(int n, int k, int r, int x) {
        return r >= 1 && r <= n - 2 && x >= 1 && x <= n - 2 && Math.min(r, n - 1 - r) + Math.min(x, n - 1 - x) >= k + 1;
    }

    private static int octagonCells(int tier) {
        final int n = edgeFor(tier);
        final int k = CUT[tier];
        return n * n - 2 * k * (k + 1);
    }

    private static int ringCells(int tier) {
        return octagonCells(tier) - COLUMN_CAP[tier];
    }

    /** Wall height (layers, incl. floor) - the mockup's tall 2:1 silo proportions. */
    private static int wallHeightFor(int tier) {
        return edgeFor(tier) * 2;
    }

    private static int domeHeightFor(int tier) {
        return edgeFor(tier) / 2;
    }

    private static int minCasingsFor(int tier) {
        // Wall rings for every layer above the floor, minus a hatch allowance; ribs and
        // radiation bands also count toward this.
        return ringCells(tier) * (wallHeightFor(tier) - 1) - 16;
    }

    // -------------------------------------------------- starter platform (mandatory piece)

    /** The starter structure: every plant building is added onto this 50x50 platform. */
    private static final int PLATFORM = 50;
    private static final int CENTER = PLATFORM / 2;
    /**
     * Front row of the shield pad. Every silo tier's FRONT wall is anchored on this line, so
     * the controller (the main station) keeps one fixed spot: embedded in the silo face at
     * ground level, standing on the pad even before the silo exists.
     */
    private static final int SILO_FRONT = CENTER - 8;
    /**
     * Open dock positions {a, c}; a module controller stands directly on the marked floor.
     * The east-side corridor (a 31-42, c 20-26 across tiers) is reserved for the Spine duct
     * and the future spiral chamber - no dock may sit in it.
     */
    private static final int[][] DOCK_POSITIONS = { { 38, 30 }, { 40, 8 }, { 40, 42 }, { 8, 8 }, { 8, 42 }, { 25, 43 },
        { 10, 25 }, { 25, 7 } };

    /**
     * Full 50x50 platform, two layers: light concrete flooring with a Tungstensteel-Reinforced
     * shield pad (sized for the max-tier silo) at center and flush steel markers set into the
     * floor under the eight dock positions; then the walking level - open except the
     * controller on the pad's front line and the dock positions, which must stay CLEAR until
     * a module controller stands there.
     */
    private static String[][] floorShape() {
        final String[] padLayer = new String[PLATFORM];
        for (int c = 0; c < PLATFORM; c++) {
            final char[] row = new char[PLATFORM];
            Arrays.fill(row, ' ');
            if (c == SILO_FRONT) row[CENTER] = '~';
            for (int[] dock : DOCK_POSITIONS) {
                if (dock[1] == c) row[dock[0]] = 'K';
            }
            padLayer[c] = new String(row);
        }
        final int n = edgeFor(MAX_TIER);
        final int k = CUT[MAX_TIER];
        final int origin = CENTER - n / 2;
        final String[] floorLayer = new String[PLATFORM];
        for (int c = 0; c < PLATFORM; c++) {
            final char[] row = new char[PLATFORM];
            for (int a = 0; a < PLATFORM; a++) {
                final int lr = c - origin;
                final int lx = a - origin;
                row[a] = (lr >= 0 && lx >= 0 && lr < n && lx < n && insideOct(n, k, lr, lx)) ? 'B' : 'O';
            }
            for (int[] dock : DOCK_POSITIONS) {
                if (dock[1] == c) row[dock[0]] = 'M';
            }
            floorLayer[c] = new String(row);
        }
        return new String[][] { padLayer, floorLayer };
    }

    // ---------------------------------------------- steam generator silo (optional piece)

    /** Silo piece grid index of the octagon center (one-cell rib margin around the octagon). */
    private static int siloCenterIdx(int tier) {
        return edgeFor(tier) / 2 + 1;
    }

    private static int siloHOff(int tier) {
        return siloCenterIdx(tier);
    }

    /** Bottom silo layer sits at the controller's walking level on the shield pad. */
    private static int siloVOff(int tier) {
        return domeHeightFor(tier) + wallHeightFor(tier) - 2;
    }

    /**
     * Every tier's front wall sits on the pad's front line, where the controller is embedded:
     * the controller cell is grid row 1 (the octagon's front row) of the silo piece.
     */
    private static int siloDOff(int tier) {
        return 1;
    }

    // ------------------------------------------------ the Spine (SD-2, optional piece)

    /** Duct length in blocks and the number of rotor sockets along it. */
    private static final int DUCT_LENGTH = 8;

    /**
     * The Spine: a 3x3 steam duct leaving the silo's side wall, four Turbine Housing
     * sockets alternating with the open bore. [y top-first][3 c-rows][8 a-chars].
     */
    private static String[][] ductShape() {
        return new String[][] { { "TTTTTTTT", "TTTTTTTT", "TTTTTTTT" }, { "TTTTTTTT", "AHAHAHAH", "TTTTTTTT" },
            { "TTTTTTTT", "TTTTTTTT", "TTTTTTTT" } };
    }

    /** Duct starts two blocks past the tier's east wall: wall, rib, then duct flush on it. */
    private static int ductHOff(int tier) {
        return -(edgeFor(tier) / 2 + 2);
    }

    /** Duct rows centered on the tier's silo mid-row (world c = 17 + edge/2). */
    private static int ductDOff(int tier) {
        return 1 - edgeFor(tier) / 2;
    }

    private enum LayerKind {
        PLENUM,
        FUEL,
        PORTS,
        BAND
    }

    private static char wallChar(int tier) {
        return tier >= 5 ? 'E' : tier >= 3 ? 'D' : 'C';
    }

    private static char shieldChar(int tier) {
        return tier >= 3 ? 'I' : 'B';
    }

    /** Silo grid has a one-cell margin on every side so the buttress ribs fit. */
    private static String[] buildLayer(int tier, LayerKind kind, boolean ribs, boolean ctrl) {
        final int n = edgeFor(tier);
        final int k = CUT[tier];
        final int gridSize = n + 2;
        final char wall = kind == LayerKind.BAND ? 'G' : wallChar(tier);
        final String[] rows = new String[gridSize];
        for (int rr = 0; rr < gridSize; rr++) {
            final char[] row = new char[gridSize];
            for (int xx = 0; xx < gridSize; xx++) {
                final int r = rr - 1;
                final int x = xx - 1;
                if (r < 0 || x < 0 || r >= n || x >= n || !insideOct(n, k, r, x)) {
                    row[xx] = ' ';
                } else if (interiorCell(n, k, r, x)) {
                    row[xx] = kind == LayerKind.FUEL ? 'F' : kind == LayerKind.PORTS ? 'J' : 'A';
                } else {
                    // The main station: the controller is embedded front-center at ground level.
                    row[xx] = (ctrl && r == 0 && x == n / 2) ? '~' : wall;
                }
            }
            rows[rr] = new String(row);
        }
        if (ribs) {
            addRibs(rows, tier);
        }
        return rows;
    }

    /**
     * Buttress ribs one block proud of the wall; the front is skipped to keep the
     * controller face clear.
     */
    private static void addRibs(String[] rows, int tier) {
        final int n = edgeFor(tier);
        final int k = CUT[tier];
        final int c = n / 2;
        final int gridSize = n + 2;
        final int[][] dirs = { { 1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
        for (int[] d : dirs) {
            int r = c;
            int x = c;
            while (r + d[0] >= 0 && r + d[0] < n
                && x + d[1] >= 0
                && x + d[1] < n
                && insideOct(n, k, r + d[0], x + d[1])) {
                r += d[0];
                x += d[1];
            }
            final int rr = r + d[0] + 1;
            final int xx = x + d[1] + 1;
            if (rr >= 0 && rr < gridSize && xx >= 0 && xx < gridSize) {
                final char[] row = rows[rr].toCharArray();
                if (row[xx] == ' ') {
                    row[xx] = wallChar(tier);
                    rows[rr] = new String(row);
                }
            }
        }
    }

    /** Hemispherical shield dome shell ring at the given height above the wall top. */
    private static String[] domeLayer(int tier, int yAbove) {
        final int n = edgeFor(tier);
        final int gridSize = n + 2;
        final double radius = n / 2.0;
        final double center = (n - 1) / 2.0;
        final double ringRadius = Math.sqrt(Math.max(0, radius * radius - (double) yAbove * yAbove));
        final boolean cap = yAbove >= domeHeightFor(tier);
        final char shield = shieldChar(tier);
        final String[] rows = new String[gridSize];
        for (int rr = 0; rr < gridSize; rr++) {
            final char[] row = new char[gridSize];
            for (int xx = 0; xx < gridSize; xx++) {
                final int r = rr - 1;
                final int x = xx - 1;
                if (r < 0 || x < 0 || r >= n || x >= n) {
                    row[xx] = ' ';
                } else {
                    final double d = Math.hypot(r - center, x - center);
                    row[xx] = (cap ? d <= ringRadius + 0.4 : Math.abs(d - ringRadius) <= 0.75) ? shield : ' ';
                }
            }
            rows[rr] = new String(row);
        }
        return rows;
    }

    /** Radiation-band layers sit in the plenum regions at the mockup's thirds. */
    private static boolean isBandLayer(int tier, int bottomUpIndex) {
        final int wallH = wallHeightFor(tier);
        return bottomUpIndex == 2 || bottomUpIndex == (9 + wallH - 1) / 2 || bottomUpIndex == wallH - 2;
    }

    /**
     * The mockup silo, top-first: hemispherical dome, tall upper plenum with radiation bands,
     * a rod-loading-port layer over EIGHT fuel layers (the decay-cascade pillars), lower
     * plenums with a band - ribbed the whole way up. No floor layer: the silo stands on the
     * platform's shield pad (bottom-up index 0 is the pad itself).
     */
    private static String[][] coreShape(int tier) {
        final int wallH = wallHeightFor(tier);
        final int domeH = domeHeightFor(tier);
        final List<String[]> layers = new ArrayList<>();
        for (int y = domeH; y >= 1; y--) {
            layers.add(domeLayer(tier, y));
        }
        for (int i = wallH - 1; i >= 1; i--) {
            final LayerKind kind;
            if (i >= 3 && i <= 10) {
                kind = LayerKind.FUEL;
            } else if (i == 11) {
                kind = LayerKind.PORTS;
            } else {
                kind = isBandLayer(tier, i) ? LayerKind.BAND : LayerKind.PLENUM;
            }
            layers.add(buildLayer(tier, kind, true, i == 1));
        }
        return layers.toArray(new String[0][]);
    }

    /**
     * Fuel pillar frame cell: frame block (any meta - fueled frames carry meta 1) or air.
     * Frames are recorded during the walk so the controller can run the rod cascade.
     */
    private static IStructureElement<MTEReactorCoreMk1> fuelCellElement() {
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                if (world.getBlock(x, y, z) == BTSBlocks.reactorFuelColumn) {
                    t.fuelSegments++;
                    t.walkFuelCells.add(new int[] { x, y, z });
                    return true;
                }
                return world.isAirBlock(x, y, z);
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, BTSBlocks.reactorFuelColumn, 0);
                return true;
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                world.setBlock(x, y, z, BTSBlocks.reactorFuelColumn, 0, 2);
                return true;
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                // Pillar frames are optional interior - never auto-consumed in survival.
                return PlaceResult.SKIP;
            }
        };
    }

    /** Rod Loading Port cell above a pillar: port block or air; ports are recorded. */
    private static IStructureElement<MTEReactorCoreMk1> portCellElement() {
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                if (world.getBlock(x, y, z) == BTSBlocks.rodLoadingPort) {
                    t.walkPortCells.add(new int[] { x, y, z });
                    return true;
                }
                return world.isAirBlock(x, y, z);
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, BTSBlocks.rodLoadingPort, 0);
                return true;
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                world.setBlock(x, y, z, BTSBlocks.rodLoadingPort, 0, 2);
                return true;
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                return PlaceResult.SKIP;
            }
        };
    }

    /**
     * Rotor socket in the Spine: a Turbine Housing in either spin direction, recorded with
     * its distance from the controller so swirl accumulates silo-outward. Blanked sockets
     * are handled by chaining a turbine casing element behind this one.
     */
    private static IStructureElement<MTEReactorCoreMk1> housingElement() {
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                if (world.getBlock(x, y, z) == BTSBlocks.turbineHousing) {
                    final IGregTechTileEntity base = t.getBaseMetaTileEntity();
                    final int dist = Math.abs(x - base.getXCoord()) + Math.abs(z - base.getZCoord());
                    t.walkHousings.add(new int[] { dist, world.getBlockMetadata(x, y, z) });
                    return true;
                }
                return false;
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, BTSBlocks.turbineHousing, 0);
                return true;
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                world.setBlock(x, y, z, BTSBlocks.turbineHousing, 0, 2);
                return true;
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                return PlaceResult.SKIP;
            }
        };
    }

    @Override
    public IStructureDefinition<MTEReactorCoreMk1> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final StructureDefinition.Builder<MTEReactorCoreMk1> builder = StructureDefinition.builder();
            builder.addShape(floorPieceName(mName), transpose(floorShape()));
            builder.addShape(ductPieceName(mName), transpose(ductShape()));
            for (int t = 1; t <= MAX_TIER; t++) {
                builder.addShape(pieceName(mName, t), transpose(coreShape(t)));
            }
            STRUCTURE_DEFINITION = builder.addElement('B', ofBlock(GregTechAPI.sBlockReinforced, 3))
                .addElement('I', ofBlock(GregTechAPI.sBlockReinforced, 1))
                .addElement('A', isAir())
                .addElement(
                    'C',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())))
                .addElement(
                    'D',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.EuropiumReinforcedRadiationProofMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(
                                x -> ++x.mCasing,
                                Casings.EuropiumReinforcedRadiationProofMachineCasing.asElement())))
                .addElement(
                    'E',
                    buildHatchAdder(MTEReactorCoreMk1.class)
                        .atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy)
                        .casingIndex(Casings.RadiantNaquadahAlloyCasing.textureId)
                        .hint(1)
                        .buildAndChain(onElementPass(x -> ++x.mCasing, Casings.RadiantNaquadahAlloyCasing.asElement())))
                .addElement('O', ofBlock(GregTechAPI.sBlockConcretes, 8))
                .addElement(
                    'K',
                    ofChain(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(PlantModuleElement.PlantModule)
                            .casingIndex(Casings.SolidSteelMachineCasing.textureId)
                            .hint(2)
                            // Module controllers are stood on markers by hand, never auto-placed.
                            .shouldReject(t -> true)
                            .build(),
                        isAir()))
                .addElement('M', Casings.SolidSteelMachineCasing.asElement())
                .addElement('G', onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement()))
                .addElement('F', fuelCellElement())
                .addElement('J', portCellElement())
                .addElement('T', Casings.TungstensteelTurbineCasing.asElement())
                .addElement('H', ofChain(housingElement(), Casings.TungstensteelTurbineCasing.asElement()))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        reactorTier = 0;
        disconnectModules();
        plantModules.clear();
        clearHatches();
        mCasing = 0;
        fuelSegments = 0;
        // Reset the spine so a failed floor walk's early return cannot keep stale boosts.
        spineBuilt = false;
        computeSpine();
        // The starter structure: the full 50x50 platform must stand (docks register modules).
        if (!checkPiece(floorPieceName(mName), CENTER, 0, SILO_FRONT, errors)) return;
        // The Steam Generator silo is the first addon ON the platform - optional, largest
        // first. Failed walks register hatches as side effects, so clear between probes (the
        // platform contributed only modules, which silo pieces never touch).
        for (int t = MAX_TIER; t >= 1; t--) {
            clearHatches();
            mCasing = 0;
            fuelSegments = 0;
            walkFuelCells.clear();
            walkPortCells.clear();
            if (checkPiece(pieceName(mName, t), siloHOff(t), siloVOff(t), siloDOff(t), null)) {
                reactorTier = t;
                break;
            }
        }
        // Hatch demands only apply once the silo exists; the bare platform forms and idles.
        if (reactorTier > 0) {
            rebuildColumns();
            // The Spine (optional): 3x3 duct with rotor sockets, leaving the silo's side.
            walkHousings.clear();
            spineBuilt = checkPiece(ductPieceName(mName), ductHOff(reactorTier), 2, ductDOff(reactorTier), null);
            computeSpine();
            checkCasingMin(errors, mCasing, minCasingsFor(reactorTier));
            checkHasEnergyHatch(errors);
            checkHasMaintenanceHatch(errors);
            checkHasInputHatch(errors);
            checkHasOutputHatch(errors);
            // The cascade emits depleted rods as items - without a bus they would be voided.
            checkHasOutputBus(errors);
        } else {
            // The last failed probe registered hatches/counters before aborting its walk;
            // drop that residue so the platform-only state is canonical. The floor's dock
            // registrations live in plantModules, which clearHatches() never touches.
            clearHatches();
            mCasing = 0;
            fuelSegments = 0;
            // No maintenance hatch exists on a bare platform: keep it serviced so the base
            // class idles into checkProcessing instead of a permanent NO_REPAIR shutdown.
            fixAllIssues();
            // Cascade/spine bookkeeping is meaningless without a silo; keep it canonical.
            columns.clear();
            portCells.clear();
            cascadeMode = false;
            liveColumnsLastCycle = 0;
            spineBuilt = false;
            computeSpine();
        }
    }

    /** Folds the recorded housings (silo-outward) into the swirl model of the Spine. */
    private void computeSpine() {
        if (!spineBuilt) {
            walkHousings.clear();
        }
        walkHousings.sort(java.util.Comparator.comparingInt(h -> h[0]));
        int swirl = 0;
        double speed = 1.0;
        double eff = 1.0;
        final StringBuilder pattern = new StringBuilder();
        for (final int[] housing : walkHousings) {
            final int d = housing[1] == BlockTurbineHousing.META_CW ? 1 : -1;
            if (pattern.length() > 0) pattern.append('-');
            pattern.append(d > 0 ? "CW" : "CCW");
            if (swirl == 0 || Integer.signum(swirl) == d) {
                // 0.08 coefficient: all-riding CW-CW-CW-CW lands at the ruled ~x1.55.
                speed *= 1.0 + 0.08 * Math.abs(swirl);
            } else {
                eff *= 1.0 + 0.10 * Math.abs(swirl);
            }
            swirl += d;
        }
        spineHousings = walkHousings.size();
        spineSwirl = swirl;
        spineSpeedMilli = (int) Math.round(speed * 1000);
        spineEffMilli = (int) Math.round(eff * 1000);
        spinePattern = pattern.toString();
    }

    /** True when every chunk the 50x50 platform could occupy (any facing) is loaded. */
    private boolean footprintLoaded(IGregTechTileEntity base) {
        return base.getWorld()
            .checkChunksExist(
                base.getXCoord() - PLATFORM,
                64,
                base.getZCoord() - PLATFORM,
                base.getXCoord() + PLATFORM,
                64,
                base.getZCoord() + PLATFORM);
    }

    @Override
    public boolean checkStructure(boolean aForceReset, IGregTechTileEntity aBaseMetaTileEntity) {
        // A neighbor update at the plant's loaded edge must not fail the walk against
        // unloaded chunks and stop a running campaign - defer until fully loaded.
        if (aBaseMetaTileEntity.isServerSide() && mMachine
            && mMaxProgresstime > 0
            && !footprintLoaded(aBaseMetaTileEntity)) {
            return mMachine;
        }
        return super.checkStructure(aForceReset, aBaseMetaTileEntity);
    }

    /** The rod cascade's port-over-pillar mapping only survives unrotated horizontal builds. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && !f.isVerticallyFliped();
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        // A campaign cannot survive its silo: abort if the reactor vanished mid-run. The
        // shutdown transition then starts the decay-heat obligation, which stays deferred
        // (not waived) until the silo stands again.
        if (reactorTier <= 0 && mMaxProgresstime > 0) {
            stopMachine(ShutDownReasonRegistry.STRUCTURE_INCOMPLETE);
        }
        // Reconnect immediately: checkMachine disconnected and rebuilt the module list, and
        // waiting for the 20-tick docking loop would leave a running plant briefly hall-less
        // (dumping raw steam into its own hatches on every periodic revalidation).
        if (mMachine) {
            for (MTEPlantModuleBase module : plantModules) {
                module.connect(this);
            }
        }
    }

    /**
     * Build target: a trigger stack larger than 1 selects the silo tier directly; a single
     * trigger defers to the Build Tier setting in the controller GUI (PCB-Factory style).
     */
    private int tierFromTrigger(ItemStack stackSize) {
        if (stackSize.stackSize > 1) {
            return Math.min(stackSize.stackSize, MAX_TIER);
        }
        return Math.min(Math.max(buildTierSetting, 1), MAX_TIER);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine && reactorTier > 0) return -1;
        final int t = tierFromTrigger(stackSize);
        final int built = survivalBuildPiece(
            floorPieceName(mName),
            stackSize,
            CENTER,
            0,
            SILO_FRONT,
            elementBudget,
            env,
            false,
            true);
        if (built >= 0) return built;
        final int siloBuilt = survivalBuildPiece(
            pieceName(mName, t),
            stackSize,
            siloHOff(t),
            siloVOff(t),
            siloDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (siloBuilt >= 0) return siloBuilt;
        return survivalBuildPiece(
            ductPieceName(mName),
            stackSize,
            ductHOff(t),
            2,
            ductDOff(t),
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int t = tierFromTrigger(stackSize);
        buildPiece(floorPieceName(mName), stackSize, hintsOnly, CENTER, 0, SILO_FRONT);
        buildPiece(pieceName(mName, t), stackSize, hintsOnly, siloHOff(t), siloVOff(t), siloDOff(t));
        buildPiece(ductPieceName(mName), stackSize, hintsOnly, ductHOff(t), 2, ductDOff(t));
    }

    // ------------------------------------------------------------------ rod cascade (SD-1)

    /** Rebuilds the pillar map from the last structure walk and reconciles rod contents. */
    private void rebuildColumns() {
        columns.clear();
        for (int[] cell : walkFuelCells) {
            final long key = ColumnInfo.key(cell[0], cell[2]);
            ColumnInfo info = columns.get(key);
            if (info == null) {
                info = new ColumnInfo(cell[0], cell[2]);
                columns.put(key, info);
            }
            info.minY = Math.min(info.minY, cell[1]);
            info.ys.add(cell[1]);
            info.height++;
        }
        for (final ColumnInfo info : columns.values()) {
            java.util.Collections.sort(info.ys);
        }
        portCells.clear();
        portCells.addAll(walkPortCells);
        // Rods only survive in pillars that still stand; anything orphaned by a dismantled
        // or shortened pillar is EJECTED at the controller rather than silently deleted.
        final List<ItemStack> orphans = new ArrayList<>();
        for (final java.util.Map.Entry<Long, byte[]> entry : columnRods.entrySet()) {
            final ColumnInfo info = columns.get(entry.getKey());
            final int keep = info == null ? 0 : info.height;
            final byte[] rods = entry.getValue();
            for (int i = keep; i < rods.length; i++) {
                if (rods[i] != ROD_NONE) {
                    orphans.add(new ItemStack(fuelRodFor(rods[i]), 1));
                }
            }
        }
        columnRods.keySet()
            .retainAll(columns.keySet());
        for (final java.util.Map.Entry<Long, ColumnInfo> entry : columns.entrySet()) {
            final byte[] rods = columnRods.get(entry.getKey());
            final int height = entry.getValue().height;
            if (rods != null && rods.length != height) {
                columnRods.put(entry.getKey(), Arrays.copyOf(rods, height));
            }
        }
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!orphans.isEmpty() && base != null && base.isServerSide()) {
            final World world = base.getWorld();
            for (final ItemStack orphan : orphans) {
                world.spawnEntityInWorld(
                    new net.minecraft.entity.item.EntityItem(
                        world,
                        base.getXCoord() + 0.5,
                        base.getYCoord() + 1.5,
                        base.getZCoord() + 0.5,
                        orphan));
            }
        }
    }

    private static Item fuelRodFor(int type) {
        switch (type) {
            case ROD_MOX:
                return BTSItems.fuelRodMox;
            case ROD_THORIUM:
                return BTSItems.fuelRodThoriumBonded;
            case ROD_NAQ:
                return BTSItems.fuelRodNaqBonded;
            default:
                return BTSItems.fuelRodUranium;
        }
    }

    private byte[] rodsFor(long key, ColumnInfo info) {
        byte[] rods = columnRods.get(key);
        if (rods == null) {
            rods = new byte[info.height];
            columnRods.put(key, rods);
        }
        return rods;
    }

    private static int rodTypeOf(Item item) {
        if (item == BTSItems.fuelRodUranium) return ROD_URANIUM;
        if (item == BTSItems.fuelRodMox) return ROD_MOX;
        if (item == BTSItems.fuelRodThoriumBonded) return ROD_THORIUM;
        if (item == BTSItems.fuelRodNaqBonded) return ROD_NAQ;
        return ROD_NONE;
    }

    private static Item depletedRodFor(int type) {
        switch (type) {
            case ROD_MOX:
                return BTSItems.depletedFuelRodMox;
            case ROD_THORIUM:
                return BTSItems.depletedFuelRodThoriumBonded;
            case ROD_NAQ:
                return BTSItems.depletedFuelRodNaqBonded;
            default:
                return BTSItems.depletedFuelRodUranium;
        }
    }

    private static int firstEmpty(byte[] rods) {
        for (int i = 0; i < rods.length; i++) {
            if (rods[i] == ROD_NONE) return i;
        }
        return -1;
    }

    /** Each port drains its buffer into the pillar directly below - lowest empty first. */
    private void drainPorts() {
        final World world = getBaseMetaTileEntity().getWorld();
        for (final int[] p : portCells) {
            if (!world.blockExists(p[0], p[1], p[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
            if (!(te instanceof TileRodLoadingPort)) continue;
            final TileRodLoadingPort port = (TileRodLoadingPort) te;
            final long key = ColumnInfo.key(p[0], p[2]);
            final ColumnInfo info = columns.get(key);
            if (info == null) continue;
            final byte[] rods = rodsFor(key, info);
            int free = firstEmpty(rods);
            while (free >= 0) {
                final ItemStack rod = port.takeOneRod();
                if (rod == null) break;
                rods[free] = (byte) rodTypeOf(rod.getItem());
                free = firstEmpty(rods);
            }
        }
    }

    /** Sync the pillars' fill-level metas (meta 1 = a rod occupies this segment). */
    private void updateColumnVisuals() {
        final World world = getBaseMetaTileEntity().getWorld();
        for (final java.util.Map.Entry<Long, ColumnInfo> entry : columns.entrySet()) {
            final ColumnInfo info = entry.getValue();
            final byte[] rods = columnRods.get(entry.getKey());
            for (int i = 0; i < info.ys.size(); i++) {
                final int y = info.ys.get(i);
                if (!world.blockExists(info.x, y, info.z)) continue;
                if (world.getBlock(info.x, y, info.z) != BTSBlocks.reactorFuelColumn) continue;
                final int wanted = (rods != null && i < rods.length && rods[i] != ROD_NONE)
                    ? BlockReactorFuelColumn.META_FUELED
                    : BlockReactorFuelColumn.META_EMPTY;
                if (world.getBlockMetadata(info.x, y, info.z) != wanted) {
                    world.setBlockMetadataWithNotify(info.x, y, info.z, wanted, 2);
                }
            }
        }
    }

    // ------------------------------------------------------------------ campaign

    private int countMatchingInputs(ItemStack sample) {
        int available = 0;
        for (ItemStack stack : getStoredInputs()) {
            if (GTUtility.areStacksEqual(sample, stack)) {
                available += stack.stackSize;
            }
        }
        return available;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        // Bare platform: no Steam Generator silo built yet, nothing to run.
        if (reactorTier <= 0) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        // GUI throttle setting, overridable by a programmed circuit in the controller slot.
        int n = Math.min(24, Math.max(1, throttleSetting));
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(24, circuit.getItemDamage());
        }
        throttleN = n;

        // One decay cycle: an eighth of the classic campaign.
        final int cycleTicks = Math.max(1, Config.genICampaignMinutes * 60 * 20 / SEGMENTS_PER_COLUMN);

        // --- Candidates are PEEKED first, then only the best one commits. Everything runs
        // on the decay-cycle cadence so a dry spell never locks the plant into a long
        // half-output campaign, and buffered rods win back the plant the moment they land.
        drainPorts();
        final int cap = COLUMN_CAP[Math.max(1, reactorTier)];

        // Candidate 1: rod cascade (peek bottom rods, nothing consumed yet).
        final int[] liveByType = new int[ROD_MULT.length];
        final List<Long> burnKeys = new ArrayList<>();
        long steamBase = 0;
        for (final java.util.Map.Entry<Long, ColumnInfo> entry : columns.entrySet()) {
            if (burnKeys.size() >= cap) break;
            final byte[] rods = columnRods.get(entry.getKey());
            if (rods == null || rods.length == 0 || rods[0] == ROD_NONE) continue;
            liveByType[rods[0]]++;
            burnKeys.add(entry.getKey());
            steamBase += STEAM_PER_COLUMN * ROD_MULT[rods[0]];
        }
        final long cascadeSteam = steamBase * n / 24;

        // Candidate 2: a parked assembly-fallback credit (remaining half-rate slices).
        final long creditSteam = fallbackCyclesLeft > 0 ? STEAM_PER_COLUMN * fallbackColumns * fallbackMult * n / 24 / 2
            : 0;

        // Candidate 3: a NEW assembly batch from the bus (peek only; blocked while credit
        // is parked). One batch = FALLBACK_SLICES half-rate cycles, a true per-rod penalty.
        final Item[] assemblies = { BTSItems.fuelAssemblyNaqBonded, BTSItems.fuelAssemblyThoriumBonded,
            BTSItems.fuelAssemblyMox, BTSItems.fuelAssemblyUranium };
        final Item[] depletedForms = { BTSItems.depletedFuelAssemblyNaqBonded,
            BTSItems.depletedFuelAssemblyThoriumBonded, BTSItems.depletedFuelAssemblyMox,
            BTSItems.depletedFuelAssemblyUranium };
        final int[] fuelMults = { 9, 5, 2, 1 };
        final int busColumns = Math.min(fuelSegments / SEGMENTS_PER_COLUMN, cap);
        int busChoice = -1;
        long newFallbackSteam = 0;
        if (fallbackCyclesLeft <= 0 && busColumns > 0) {
            for (int i = 0; i < assemblies.length; i++) {
                if (countMatchingInputs(new ItemStack(assemblies[i], 1)) >= busColumns) {
                    busChoice = i;
                    newFallbackSteam = STEAM_PER_COLUMN * busColumns * fuelMults[i] * n / 24 / 2;
                    break;
                }
            }
        }

        // --- Commit the best candidate (the cascade wins ties - rods are the real system).
        if (cascadeSteam > 0 && cascadeSteam >= creditSteam && cascadeSteam >= newFallbackSteam) {
            final List<ItemStack> depleted = new ArrayList<>();
            int bestType = ROD_URANIUM;
            for (final long key : burnKeys) {
                final byte[] rods = columnRods.get(key);
                System.arraycopy(rods, 1, rods, 0, rods.length - 1);
                rods[rods.length - 1] = ROD_NONE;
            }
            for (int type = 1; type < liveByType.length; type++) {
                int remaining = liveByType[type];
                if (remaining > 0) bestType = type;
                while (remaining > 0) {
                    final int stack = Math.min(64, remaining);
                    depleted.add(new ItemStack(depletedRodFor(type), stack));
                    remaining -= stack;
                }
            }
            updateColumnVisuals();
            cascadeMode = true;
            liveColumnsLastCycle = burnKeys.size();
            activeColumns = burnKeys.size();
            activeFuelMult = ROD_MULT[bestType];
            steamPerTick = cascadeSteam;
            this.mOutputItems = depleted.toArray(new ItemStack[0]);
            this.mMaxProgresstime = cycleTicks;
            this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
            this.mEfficiencyIncrease = 10_000;
            this.lEUt = 0;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
        updateColumnVisuals();

        if (creditSteam > 0) {
            fallbackCyclesLeft--;
            cascadeMode = false;
            liveColumnsLastCycle = 0;
            activeColumns = fallbackColumns;
            activeFuelMult = fallbackMult;
            steamPerTick = creditSteam;
            this.mOutputItems = fallbackCyclesLeft == 0 && fallbackDepleted != null
                ? new ItemStack[] { fallbackDepleted }
                : null;
            if (fallbackCyclesLeft == 0) fallbackDepleted = null;
            this.mMaxProgresstime = cycleTicks;
            this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
            this.mEfficiencyIncrease = 10_000;
            this.lEUt = 0;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        if (newFallbackSteam > 0) {
            final ItemStack chosen = new ItemStack(assemblies[busChoice], 1);
            for (int i = 0; i < busColumns; i++) {
                depleteInput(chosen);
            }
            updateSlots();
            fallbackColumns = busColumns;
            fallbackMult = fuelMults[busChoice];
            fallbackDepleted = new ItemStack(depletedForms[busChoice], busColumns);
            fallbackCyclesLeft = FALLBACK_SLICES - 1;
            cascadeMode = false;
            liveColumnsLastCycle = 0;
            activeColumns = busColumns;
            activeFuelMult = fallbackMult;
            steamPerTick = newFallbackSteam;
            this.mOutputItems = null;
            this.mMaxProgresstime = cycleTicks;
            this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
            this.mEfficiencyIncrease = 10_000;
            this.lEUt = 0;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        clearCampaign();
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (steamPerTick <= 0 || !getBaseMetaTileEntity().isServerSide()) return true;

        // Cooling towers return condensate: each halves the feedwater draw.
        final int towers = coolingTowerCount();
        final int baseFlow = (int) ((steamPerTick + GTValues.STEAM_PER_WATER - 1) / GTValues.STEAM_PER_WATER);
        final int flowPerTick = Math.max(1, baseFlow >> towers);

        startRecipeProcessing();
        final MTEModuleTurbineHall hall = dockedTurbineHall();
        if (hall != null) {
            // Feedwater first; starving it heats the core exactly as fluid mode.
            if (depleteInput(Materials.Water.getFluid(flowPerTick))
                || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                // The Spine boosts what the hall sees: speed (throughput) x efficiency.
                final long eu = hall.processReactorSteam(spineBoostedSteam(true), reactorTier >= MAX_TIER, baseFlow);
                if (eu < 0) {
                    // Hall has no fan installed: export the steam instead.
                    addOutput(Materials.Steam.getGas(spineBoostedSteam(false)));
                    lastEUOutput = 0;
                } else {
                    lastEUOutput = eu;
                }
                emitWasteWater();
            } else {
                starveTick();
            }
        } else {
            runFluidTick(flowPerTick, baseFlow);
        }
        endRecipeProcessing();
        return true;
    }

    /** Steam after the Spine: speed multiplies throughput; efficiency only pays out in EU. */
    private long spineBoostedSteam(boolean withEfficiency) {
        long steam = steamPerTick * spineSpeedMilli / 1000;
        if (withEfficiency) {
            steam = steam * spineEffMilli / 1000;
        }
        return steam;
    }

    /** Fluid-export mode: no docked Turbine Hall. */
    private void runFluidTick(int flowPerTick, int baseFlow) {
        final int coolantFlow = reactorTier >= MAX_TIER ? baseFlow * 2 : baseFlow;
        if (depleteInput(GTModHandler.getIC2Coolant(coolantFlow))) {
            addOutput(GTModHandler.getHotCoolant(coolantFlow));
            lastEUOutput = 0;
            emitWasteWater();
        } else if (depleteInput(Materials.Water.getFluid(flowPerTick))
            || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                addOutput(Materials.Steam.getGas(spineBoostedSteam(false)));
                lastEUOutput = 0;
                emitWasteWater();
            } else {
                starveTick();
            }
    }

    private void starveTick() {
        starvedThisTick = true;
        heat += HEAT_PER_STARVED_TICK;
        if (heat >= SCRAM_HEAT) {
            scram();
        }
    }

    private void emitWasteWater() {
        if (activeColumns > 0 && BTSFluids.nuclearWasteWater != null) {
            addOutput(new FluidStack(BTSFluids.nuclearWasteWater, activeColumns));
        }
    }

    private void clearCampaign() {
        steamPerTick = 0;
        activeColumns = 0;
        liveColumnsLastCycle = 0;
        lastEUOutput = 0;
    }

    /** Campaign lost, machine disabled until the operator re-enables it. Decay heat follows. */
    private void scram() {
        clearCampaign();
        stopMachine(SCRAM_REASON);
        getBaseMetaTileEntity().disableWorking();
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        clearCampaign();
        // The rods behind the staged depleted forms were consumed at cycle START; a mid-cycle
        // shutdown must not delete 90 minutes of them. Flush to the buses (base stopMachine
        // nulls mOutputItems) - steam loss and decay heat remain the punishment.
        if (mOutputItems != null) {
            addItemOutputs(mOutputItems);
            mOutputItems = null;
        }
        super.stopMachine(reason);
    }

    /** World column at the given depth behind the front-wall controller (0 = the controller). */
    private Vec3Impl worldBehindController(int depth) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final Vec3Impl off = getExtendedFacing().getWorldOffset(new Vec3Impl(0, 0, depth));
        return new Vec3Impl(
            base.getXCoord() + off.get0(),
            base.getYCoord() + off.get1(),
            base.getZCoord() + off.get2());
    }

    /** World column of the silo center, half a diameter behind the front-wall controller. */
    private Vec3Impl siloCenterWorld() {
        return worldBehindController(edgeFor(getReactorTier()) / 2);
    }

    /** Vertical detonation and a lingering radiation field; both honor config toggles. */
    private void meltdown() {
        heat = 0;
        decayHeatTicks = 0;
        clearCampaign();
        stopMachine(MELTDOWN_REASON);
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        base.disableWorking();
        if (Config.meltdownExplosions && GregTechAPI.sMachineExplosions) {
            final World world = base.getWorld();
            final Vec3Impl center = siloCenterWorld();
            final int stackTop = wallHeightFor(getReactorTier()) + domeHeightFor(getReactorTier()) - 1;
            for (int dy = stackTop + 2; dy <= stackTop + 11; dy += 3) {
                world.newExplosion(
                    null,
                    center.get0() + 0.5,
                    center.get1() + dy + 0.5,
                    center.get2() + 0.5,
                    3.0F,
                    true,
                    true);
            }
        }
        if (Config.radiationField) {
            radiationTicks = RADIATION_TICKS;
            // Freeze the field on the silo that melted - the tier is reset once the wreck
            // fails a structure check, and the hazard must not drift with it.
            radiationCenterDepth = edgeFor(getReactorTier()) / 2;
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;

        // Docking authority: connect modules while the platform stands, disconnect when it breaks.
        if (aTick % 20 == 0) {
            if (mMachine) {
                for (MTEPlantModuleBase module : plantModules) {
                    module.connect(this);
                }
            } else {
                disconnectModules();
            }
        }

        final boolean running = mMaxProgresstime > 0;
        if (wasRunningLastTick && !running) {
            decayHeatTicks = DECAY_HEAT_TICKS;
        }
        wasRunningLastTick = running;

        // Residual Spine swirl is turbulence: |S|^2 x 5 heat per second while running. An
        // all-riding rotor pattern (S=4) outruns bare passive decay - bring cooling towers.
        if (running && spineSwirl != 0 && aTick % 20 == 0) {
            heat += spineSwirl * spineSwirl * 5;
            if (heat >= SCRAM_HEAT) {
                scram();
            }
        }

        final int towers = coolingTowerCount();

        // Machine-block updates cannot propagate across the unregistered concrete platform to
        // the embedded controller, so force periodic revalidation: every 200t while a campaign
        // runs (a torn-open silo must not keep producing), every 600t while idle (modules
        // placed or removed on the dock markers are otherwise never noticed, and a hand-built
        // floor would never form). Never force a walk while the footprint is partially
        // unloaded - a formed machine's checker treats unloaded blocks as hard failures.
        if (aTick % 200 == 0 && (mMaxProgresstime > 0 || aTick % 600 == 0) && footprintLoaded(aBaseMetaTileEntity)) {
            onMachineBlockUpdate();
        }

        // The decay-heat obligation only ticks down while the SILO stands - dismantling it
        // (platform included or not) defers the obligation, never waives it. Cooling towers
        // halve the demand per tower.
        if (decayHeatTicks > 0 && mMachine && reactorTier > 0) {
            decayHeatTicks--;
            if (!running) {
                final int decayWater = Math.max(8, DECAY_WATER_PER_TICK >> towers);
                startRecipeProcessing();
                boolean cooled = depleteInput(Materials.Water.getFluid(decayWater))
                    || depleteInput(GTModHandler.getDistilledWater(decayWater))
                    || depleteInput(GTModHandler.getIC2Coolant(decayWater));
                endRecipeProcessing();
                if (!cooled) {
                    starvedThisTick = true;
                    heat += HEAT_PER_STARVED_TICK / 2;
                    if (heat >= MELTDOWN_HEAT) {
                        meltdown();
                    }
                }
            }
        }

        if (radiationTicks > 0) {
            radiationTicks--;
            if (aTick % 40 == 0) {
                final World world = aBaseMetaTileEntity.getWorld();
                // Center on the melted silo's frozen depth (fallback for pre-snapshot saves).
                final Vec3Impl center = worldBehindController(
                    radiationCenterDepth > 0 ? radiationCenterDepth : edgeFor(getReactorTier()) / 2);
                final int x = center.get0();
                final int y = center.get1();
                final int z = center.get2();
                final List<EntityLivingBase> victims = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    AxisAlignedBB.getBoundingBox(
                        x - RADIATION_RADIUS,
                        y - RADIATION_RADIUS,
                        z - RADIATION_RADIUS,
                        x + RADIATION_RADIUS + 1,
                        y + RADIATION_RADIUS + 1,
                        z + RADIATION_RADIUS + 1));
                for (EntityLivingBase living : victims) {
                    if (living instanceof EntityPlayer && ((EntityPlayer) living).capabilities.isCreativeMode) {
                        continue;
                    }
                    GTUtility.applyRadioactivity(living, RADIATION_LEVEL, 1);
                }
            }
        }

        // Cooling towers double passive heat decay per tower. A hot core with its silo (or
        // whole structure) dismantled does not cool - the obligation is frozen, not dodged.
        final boolean brokenHotCore = (!mMachine || reactorTier <= 0) && decayHeatTicks > 0;
        if (!starvedThisTick && !brokenHotCore && heat > 0) {
            heat = Math.max(0, heat - HEAT_DECAY_PER_TICK * (1 + towers));
        }
        starvedThisTick = false;
    }

    // ------------------------------------------------------------------ persistence & info

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsHeat", heat);
        aNBT.setInteger("btsReactorTier", reactorTier);
        aNBT.setInteger("btsActiveColumns", activeColumns);
        aNBT.setInteger("btsThrottleN", throttleN);
        aNBT.setInteger("btsThrottleSetting", throttleSetting);
        aNBT.setInteger("btsBuildTierSetting", buildTierSetting);
        aNBT.setInteger("btsFuelMult", activeFuelMult);
        aNBT.setInteger("btsDecayHeatTicks", decayHeatTicks);
        aNBT.setInteger("btsRadiationTicks", radiationTicks);
        aNBT.setInteger("btsRadCenterDepth", radiationCenterDepth);
        aNBT.setBoolean("btsCascadeMode", cascadeMode);
        aNBT.setInteger("btsLiveColumns", liveColumnsLastCycle);
        aNBT.setInteger("btsFallbackCyclesLeft", fallbackCyclesLeft);
        aNBT.setInteger("btsFallbackColumns", fallbackColumns);
        aNBT.setInteger("btsFallbackMult", fallbackMult);
        if (fallbackDepleted != null) {
            final NBTTagCompound fbTag = new NBTTagCompound();
            fallbackDepleted.writeToNBT(fbTag);
            aNBT.setTag("btsFallbackDepleted", fbTag);
        }
        final NBTTagList rodList = new NBTTagList();
        for (final java.util.Map.Entry<Long, byte[]> entry : columnRods.entrySet()) {
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("x", (int) (entry.getKey() >> 32));
            tag.setInteger(
                "z",
                entry.getKey()
                    .intValue());
            // Clone: 1.7.10 serializes chunk NBT on the async File IO thread.
            tag.setByteArray(
                "rods",
                entry.getValue()
                    .clone());
            rodList.appendTag(tag);
        }
        aNBT.setTag("btsColumnRods", rodList);
        aNBT.setLong("btsSteamPerTick", steamPerTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        heat = aNBT.getInteger("btsHeat");
        reactorTier = aNBT.getInteger("btsReactorTier");
        activeColumns = aNBT.getInteger("btsActiveColumns");
        throttleN = aNBT.getInteger("btsThrottleN");
        throttleSetting = aNBT.getInteger("btsThrottleSetting");
        if (throttleSetting <= 0) throttleSetting = 24;
        buildTierSetting = aNBT.getInteger("btsBuildTierSetting");
        if (buildTierSetting <= 0) buildTierSetting = 1;
        activeFuelMult = Math.max(1, aNBT.getInteger("btsFuelMult"));
        decayHeatTicks = aNBT.getInteger("btsDecayHeatTicks");
        radiationTicks = aNBT.getInteger("btsRadiationTicks");
        radiationCenterDepth = aNBT.getInteger("btsRadCenterDepth");
        cascadeMode = aNBT.getBoolean("btsCascadeMode");
        liveColumnsLastCycle = aNBT.getInteger("btsLiveColumns");
        fallbackCyclesLeft = aNBT.getInteger("btsFallbackCyclesLeft");
        fallbackColumns = aNBT.getInteger("btsFallbackColumns");
        fallbackMult = Math.max(1, aNBT.getInteger("btsFallbackMult"));
        fallbackDepleted = aNBT.hasKey("btsFallbackDepleted")
            ? ItemStack.loadItemStackFromNBT(aNBT.getCompoundTag("btsFallbackDepleted"))
            : null;
        columnRods.clear();
        final NBTTagList rodList = aNBT.getTagList("btsColumnRods", 10);
        for (int i = 0; i < rodList.tagCount(); i++) {
            final NBTTagCompound tag = rodList.getCompoundTagAt(i);
            columnRods.put(
                ColumnInfo.key(tag.getInteger("x"), tag.getInteger("z")),
                tag.getByteArray("rods")
                    .clone());
        }
        steamPerTick = aNBT.getLong("btsSteamPerTick");
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new MTEReactorSiloGui(this);
    }

    public boolean isSiloBuilt() {
        return reactorTier > 0;
    }

    public int getReactorTier() {
        return Math.max(1, reactorTier);
    }

    public int getEdge() {
        return edgeFor(getReactorTier());
    }

    public int getFuelColumns() {
        if (cascadeMode) return liveColumnsLastCycle;
        return Math.min(fuelSegments / SEGMENTS_PER_COLUMN, COLUMN_CAP[getReactorTier()]);
    }

    public boolean isCascadeMode() {
        return cascadeMode;
    }

    public boolean isSpineBuilt() {
        return spineBuilt;
    }

    public int getSpineHousings() {
        return spineHousings;
    }

    public int getSpineSpeedMilli() {
        return spineSpeedMilli;
    }

    public int getSpineEffMilli() {
        return spineEffMilli;
    }

    public int getSpineSwirl() {
        return spineSwirl;
    }

    public String getSpinePattern() {
        return spinePattern;
    }

    public int getLiveColumns() {
        return liveColumnsLastCycle;
    }

    public int getTotalColumns() {
        return columns.size();
    }

    public int getColumnCap() {
        return COLUMN_CAP[getReactorTier()];
    }

    public String getFuelDisplayName() {
        return fuelName(activeFuelMult);
    }

    public int getThrottleSetting() {
        return throttleSetting;
    }

    public void setThrottleSetting(int value) {
        throttleSetting = Math.min(24, Math.max(1, value));
    }

    public int getBuildTierSetting() {
        return buildTierSetting;
    }

    public void setBuildTierSetting(int value) {
        buildTierSetting = Math.min(MAX_TIER, Math.max(1, value));
    }

    public int getHeat() {
        return heat;
    }

    public int getDecaySeconds() {
        return decayHeatTicks / 20;
    }

    public int getRadiationSeconds() {
        return radiationTicks / 20;
    }

    public boolean isTurbineInstalled() {
        return dockedTurbineHall() != null;
    }

    public int getCoolingTowers() {
        return coolingTowerCount();
    }

    public long getLastEUOutput() {
        return lastEUOutput;
    }

    public long getSteamPerTick() {
        return steamPerTick;
    }

    /** What actually leaves the hatches in export mode: campaign steam after the Spine. */
    public long getBoostedSteamOutput() {
        return spineBoostedSteam(false);
    }

    public int installedFanTier() {
        final MTEModuleTurbineHall hall = dockedTurbineHall();
        return hall == null ? 0 : hall.installedFanTier();
    }

    /** One-line docked-module inventory for the GUI. */
    public String getModulesSummary() {
        final MTEModuleTurbineHall hall = dockedTurbineHall();
        final int towers = coolingTowerCount();
        if (hall == null && towers == 0) return "none docked";
        final StringBuilder sb = new StringBuilder();
        if (hall != null) sb.append("Turbine Hall");
        if (towers > 0) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append("Cooling Tower x")
                .append(towers);
        }
        return sb.toString();
    }

    private static String fuelName(int mult) {
        switch (mult) {
            case 9:
                return "Naquadah-Bonded (x9)";
            case 5:
                return "Thorium-Bonded (x5)";
            case 2:
                return "MOX (x2)";
            default:
                return "Uranium";
        }
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        if (!isSiloBuilt()) {
            info.add("Steam Generator: NOT BUILT - project it onto the shield pad at platform center");
            info.add("Modules: " + getModulesSummary());
            return info.toArray(new String[0]);
        }
        final int tier = getReactorTier();
        info.add("Steam Generator tier: " + tier + " (" + edgeFor(tier) + " wide octagon)");
        info.add(
            cascadeMode ? "Fuel: rod cascade - " + liveColumnsLastCycle + " live / " + columns.size() + " pillars"
                : "Fuel: assembly fallback (output halved) - " + getFuelColumns() + " / " + COLUMN_CAP[tier]);
        info.add("Fuel type: " + fuelName(activeFuelMult));
        info.add("Control rods: " + (throttleN * 100 / 24) + "% power");
        info.add("Modules: " + getModulesSummary());
        if (spineBuilt) {
            info.add(
                "Spine: [" + spinePattern
                    + "]  speed x"
                    + String.format("%.2f", spineSpeedMilli / 1000.0)
                    + "  eff x"
                    + String.format("%.2f", spineEffMilli / 1000.0)
                    + "  swirl "
                    + spineSwirl);
        }
        info.add(
            isTurbineInstalled() ? "Generating: " + lastEUOutput + " EU/t (via Turbine Hall)"
                : "Steam output: " + getBoostedSteamOutput() + " L/t");
        info.add("Core heat: " + heat + " / " + MELTDOWN_HEAT + " (SCRAM at " + SCRAM_HEAT + ")");
        if (decayHeatTicks > 0) {
            info.add("Decay heat: cooling required for " + (decayHeatTicks / 20) + "s");
        }
        if (radiationTicks > 0) {
            info.add("RADIATION FIELD: " + (radiationTicks / 20) + "s remaining");
        }
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String gold = EnumChatFormatting.GOLD.toString();
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String gray = EnumChatFormatting.GRAY.toString();
        final String red = EnumChatFormatting.RED.toString();
        final String reset = EnumChatFormatting.RESET.toString() + EnumChatFormatting.GRAY;
        tt.addMachineType("Nuclear Plant Foundation - Unit One main station")
            .addInfo(aqua + "The 50x50 platform is the starter structure - every building is added onto it." + reset)
            .addInfo("Shield pad at center = reactor site. 8 flush steel markers = module docks (keep clear).")
            .addSeparator()
            .addInfo(gold + "1." + reset + " Steam Generator silo - project onto the pad (GUI Build Tier 1-5).")
            .addInfo(gold + "2." + reset + " Turbine Hall - on the dock marker beside the silo.")
            .addInfo(gold + "3." + reset + " Cooling Towers (max 2) and more modules on the other markers.")
            .addSeparator()
            .addInfo(aqua + "Fuel:" + reset + " rods cascade down the pillars - insert at the TOP ports,")
            .addInfo("bottom rod burns each cycle, depleted rods exit the output bus.")
            .addInfo(gray + "Bus assemblies still run, at a heavy penalty." + reset)
            .addInfo(aqua + "Spine:" + reset + " optional 3x3 duct off the silo's side, 4 rotor sockets.")
            .addInfo("Right-click a housing to set spin: ride the swirl for speed, oppose it for efficiency.")
            .addInfo(aqua + "Control:" + reset + " rods 1-24 + Build Tier in the GUI; circuit overrides throttle.")
            .addInfo(red + "Heat:" + reset + " SCRAM 8,000 - MELTDOWN 10,000. Decay heat needs 20 min of water.")
            .beginStructureBlock(50, 42, 50, true)
            .addController("Main station: front line of the shield pad, at floor level (in the silo face)")
            .addCasingInfoExactly("Light Concrete (platform)", 2243, false)
            .addCasingInfoExactly("Tungstensteel Reinforced Block (shield pad)", 249, false)
            .addCasingInfoExactly("Solid Steel Machine Casing (flush dock markers)", 8, false)
            .addCasingInfoRange("Silo tier wall casing incl. ribs", 400, 1700, false)
            .addCasingInfoRange("Radiation Proof Machine Casing (band rings)", 60, 150, false)
            .addCasingInfoRange("Shield blocks (dome)", 40, 320, false)
            .addEnergyHatch("Any silo wall casing", 1)
            .addMaintenanceHatch("Any silo wall casing", 1)
            .addInputHatch("Any silo wall casing", 1)
            .addOutputHatch("Any silo wall casing", 1)
            .addInputBus("Any silo wall casing", 1)
            .addOutputBus("Any silo wall casing", 1)
            .addStructureInfo("Silo: 9-17 wide octagon, walls 2x the diameter tall, dome on top.")
            .addStructureInfo("Walls by tier: Radiation Proof -> Europium-Reinforced -> Radiant Naq Alloy.")
            .addStructureInfo("Inside: fuel pillar frames 8 high, a Rod Loading Port above each pillar.")
            .addStructureInfo("Rod ladder: Uranium x1 - MOX x2 - Thorium-Bonded x5 - Naquadah-Bonded x9.")
            .addStructureInfo("Breaking a fueled pillar frame ejects its rods at the controller.")
            .addStructureInfo("Spine: 3x3x8 Tungstensteel Turbine Casing duct on the platform, two blocks")
            .addStructureInfo("past the silo's side wall; 4 Turbine Housings alternate with the open bore.")
            .addStructureInfo("Residual swirl is turbulence heat - all-riding patterns need cooling towers.")
            .addStructureInfo("Modules dock by standing their controller on a marker and projecting there.")
            .addStructureInfo("No Turbine Hall: exports Steam (or Hot Coolant when fed IC2 Coolant).")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_HEAT_EXCHANGER_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Casings.RadiationProofMachineCasing.getCasingTexture() };
        }
        return rTexture;
    }

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }
}
