package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Dynamo;
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
import net.minecraft.init.Blocks;
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
import com.nateyburr.beyondthestars.blocks.TileTurbineHousing;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.items.BTSItems;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
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
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.xmod.thermalfoundation.fluid.TFFluids;

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
    /** The chain's own seizure: COOLANT starvation, not the retired hall's propellant one. */
    private static final ShutDownReason SEIZURE_REASON = SimpleShutDownReason
        .ofCritical("beyondthestars.seizure.chain");

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
    /** World cells of the recorded housings - the rotors a seizure destroys. */
    private final List<int[]> spineHousingCells = new ArrayList<>();

    /** Spiral chamber state (SD-3). */
    private boolean spiralBuilt;
    final List<MTEHatchPrecisionInjector> spiralInjectors = new ArrayList<>();
    MTEHatchSpineSensor spineSensor;
    private int lastGoodInjectors;
    private int chamberCoolantDrain;
    private String chamberCoolantName = "";
    private long lastChamberTick;

    /** Rotor gallery state (SD-4). */
    private boolean galleryBuilt;
    private int galleryStages;
    private int walkGalleryStages;
    /** Stage-socket cells recorded by the current gallery walk / committed on success. */
    private final List<int[]> walkStageCells = new ArrayList<>();
    private final List<int[]> stageCells = new ArrayList<>();

    /** TC-2 control-block panels (dedicated sockets, cap 1 each). */
    MTESiloControlBlock siloPanel;
    MTETurbineControlBlock turbinePanel;

    // ------------------------------------------------------------- TC-3 RPM model
    /** Redline: the RPM the best sustained clock (1 Hz, 8/8 timed Lv 6) approaches. */
    public static final int RPM_REDLINE = 12_000;
    private static final int RPM_REDLINE_MILLI = RPM_REDLINE * 1000;
    /**
     * RPM gained per accepted cycle = RPM_GAIN x P x timed/8. Equilibrium math: with decay
     * r *= 0.995 per tick and one cycle every 20 ticks (1 Hz - the fastest the steam budget
     * sustains, since a cycle drinks a full 20-tick slug), the peak fixed point right after
     * a gain G is R* = G / (1 - 0.995^20) = G / 0.095368 ~= 10.486 x G. Lv 6 propellant
     * (P = 6.5) with all 8 injectors timed gives G = 176 x 6.5 = 1,144 and R* ~= 11,996 -
     * the redline. The ladder below it: Lv 3 (P=5) ~= 9,227; Lv 1 naq (P=3) ~= 5,536;
     * Benzene (P=1.25) ~= 2,307. (The ruling's sketch constant 60 would top out at ~4,089.)
     */
    private static final int RPM_GAIN = 176;
    /** Steam slugs a cycle drinks (ticks worth of L/t) and the buffer cap in slugs x 5. */
    private static final int CYCLE_SLUG_TICKS = 20;
    private static final int STEAM_BUFFER_CAP_TICKS = 100;
    /** Fan RPM x1000 (the central variable; persisted). */
    private int rpmMilli;
    /** Steam standing in the chain, litres (fed L/t per running tick, cap 100 x L/t). */
    private long steamBuffer;
    /** A redstone rising edge on the Turbine Control Block requested a cycle. */
    private boolean pendingCycle;
    /** World tick at which the pending cycle's +/-2t window gets scored (0 = none). */
    private long cycleEvalTick;
    /** Whether the pending cycle had its steam slug (no slug = propellant burned, no gain). */
    private boolean cycleHadSteam;
    private long lastCycleTick;
    private int lastCyclePropMilli = 1000;
    /** Accepted-cycle ticks of the last minute, for the measured cycles/min readout. */
    private final java.util.ArrayDeque<Long> recentCycles = new java.util.ArrayDeque<>();
    private int cyclesPerMinute;
    /** Average stage factor x1000 over all 8 stage sockets (missing rotor/housing = 0). */
    private int avgRotorFactorMilli;
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

    public boolean addInjectorToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchPrecisionInjector && spiralInjectors.size() < 8) {
            ((MTEHatchPrecisionInjector) mte).updateTexture(aBaseCasingIndex);
            return spiralInjectors.add((MTEHatchPrecisionInjector) mte);
        }
        return false;
    }

    @Override
    public boolean addInputHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        // Precision Injectors must never demote to plain input hatches: a 9th injector (or
        // one in a silo wall) would register silently into mInputHatches and never be
        // slug-scored. Rejecting it makes the structure walk fail visibly instead.
        if (aTileEntity != null && aTileEntity.getMetaTileEntity() instanceof MTEHatchPrecisionInjector) return false;
        return super.addInputHatchToMachineList(aTileEntity, aBaseCasingIndex);
    }

    public boolean addSensorToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchSpineSensor && spineSensor == null) {
            ((MTEHatchSpineSensor) mte).updateTexture(aBaseCasingIndex);
            spineSensor = (MTEHatchSpineSensor) mte;
            return true;
        }
        return false;
    }

    public boolean addSiloPanelToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTESiloControlBlock && siloPanel == null) {
            ((MTESiloControlBlock) mte).updateTexture(aBaseCasingIndex);
            siloPanel = (MTESiloControlBlock) mte;
            siloPanel.setParent(this);
            return true;
        }
        return false;
    }

    public boolean addTurbinePanelToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTETurbineControlBlock && turbinePanel == null) {
            ((MTETurbineControlBlock) mte).updateTexture(aBaseCasingIndex);
            turbinePanel = (MTETurbineControlBlock) mte;
            turbinePanel.setParent(this);
            return true;
        }
        return false;
    }

    /** Unregisters the panel and clears its back-reference (if it still points here). */
    private void resetSiloPanel() {
        if (siloPanel != null && siloPanel.getParent() == this) {
            siloPanel.setParent(null);
        }
        siloPanel = null;
    }

    private void resetTurbinePanel() {
        if (turbinePanel != null && turbinePanel.getParent() == this) {
            turbinePanel.setParent(null);
        }
        turbinePanel = null;
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

    private static String ductPieceName(String base, int tier) {
        return base + ".duct.tier" + tier;
    }

    private static String spiralPieceName(String base) {
        return base + ".spiral";
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
     * Open dock positions {a, c}; a module controller stands directly on the marked floor,
     * FACING AWAY from its building. The canonical one-projection layout: Turbine Hall at
     * {38,30} (hall body behind it, c 30-38 x a 36-40), Cooling Towers at {8,1} (front
     * corner, body c 0-16 x a 0-16) and {8,48} (back corner, body c 33-49 x a 0-16). The
     * east corridor (a 31-42, c 20-26 across tiers) stays reserved for the Spine duct and
     * the future spiral chamber - no dock may sit in it.
     */
    private static final int[][] DOCK_POSITIONS = { { 38, 30 }, { 8, 1 }, { 8, 48 }, { 40, 8 }, { 40, 42 }, { 25, 43 },
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

    /** Duct length per tier: always ends at platform a=42, flush with the Spiral chamber. */
    private static int ductLengthFor(int tier) {
        return 16 - edgeFor(tier) / 2;
    }

    /**
     * The Spine: a 3x3 steam duct running from the silo's side wall to the Spiral chamber,
     * four Turbine Housing sockets nearest the silo alternating with the open bore.
     * [y top-first][3 c-rows][L a-chars].
     */
    private static String[][] ductShape(int tier) {
        final int len = ductLengthFor(tier);
        final char[] solid = new char[len];
        Arrays.fill(solid, 'T');
        final char[] bore = new char[len];
        Arrays.fill(bore, 'A');
        for (int i = 1; i <= 7 && i < len; i += 2) {
            bore[i] = 'H';
        }
        final String wall = new String(solid);
        return new String[][] { { wall, wall, wall }, { wall, new String(bore), wall }, { wall, wall, wall } };
    }

    // --------------------------------------------- the Spiral chamber (SD-3, optional piece)

    /** Chamber footprint: 7x7 at platform a 43-49, centered on the tier's mid-row, 12 tall. */
    private static final int SPIRAL_SIZE = 7;
    private static final int SPIRAL_HEIGHT = 12;

    /**
     * The Spiral: solid casing roof/floor, plain casing walls, and inside a helix of concrete
     * arcs stepping down the 5x5 interior ring around an open 3x3 downcomer. TC-1 sockets:
     * one Precision Injector per interior level 1..8 following the helix around the rotating
     * faces, the Spine Sensor in the roof's front-center, and Input Hatch + Maintenance in
     * the bottom layer's front corners. [y top-first][7 c-rows][7 a-chars].
     */
    private static String[][] spiralShape() {
        final String[][] layers = new String[SPIRAL_HEIGHT][];
        final String solid = "5555555";
        for (int level = 0; level < SPIRAL_HEIGHT; level++) {
            if (level == 0 || level == SPIRAL_HEIGHT - 1) {
                final char[][] cap = new char[SPIRAL_SIZE][];
                for (int r = 0; r < SPIRAL_SIZE; r++) {
                    cap[r] = solid.toCharArray();
                }
                if (level == 0) {
                    // Roof: the Spine Sensor socket sits front-center.
                    cap[0][3] = 'j';
                } else {
                    // Bottom: Input Hatch + Maintenance sockets in the front corners.
                    cap[0][0] = 'k';
                    cap[0][6] = 'l';
                }
                final String[] capRows = new String[SPIRAL_SIZE];
                for (int r = 0; r < SPIRAL_SIZE; r++) {
                    capRows[r] = new String(cap[r]);
                }
                layers[level] = capRows;
                continue;
            }
            final char[][] grid = new char[SPIRAL_SIZE][SPIRAL_SIZE];
            for (int r = 0; r < SPIRAL_SIZE; r++) {
                for (int x = 0; x < SPIRAL_SIZE; x++) {
                    final boolean wall = r == 0 || x == 0 || r == SPIRAL_SIZE - 1 || x == SPIRAL_SIZE - 1;
                    grid[r][x] = wall ? '5' : 'A';
                }
            }
            // Injector sockets follow the helix down: level L (1..8) on face (L-1)%4 -
            // back(c+), west(a-), front(c-), east(a+) - at that wall's mid-cell. The
            // rotation STARTS on the back face so the two back-face sockets land at levels
            // 1 and 5 (world y 10 and 6), clear above the gallery's 5-tall front wall;
            // starting on the front face buried the level-7 socket behind it.
            if (level >= 1 && level <= 8) {
                final int face = (level - 1) % 4;
                if (face == 0) grid[6][3] = 'i';
                else if (face == 1) grid[3][0] = 'i';
                else if (face == 2) grid[0][3] = 'i';
                else grid[3][6] = 'i';
            }
            // Helix: a 5-cell concrete arc on the interior 5x5 ring, rotating each level down.
            final int[][] ring = interiorRing();
            final int start = ((level - 1) * 3) % ring.length;
            for (int k = 0; k < 5; k++) {
                final int[] cell = ring[(start + k) % ring.length];
                grid[cell[0]][cell[1]] = 'O';
            }
            final String[] rows = new String[SPIRAL_SIZE];
            for (int r = 0; r < SPIRAL_SIZE; r++) {
                rows[r] = new String(grid[r]);
            }
            layers[level] = rows;
        }
        return layers;
    }

    /** The 16 cells of the interior 5x5 ring (grid coords 1..5), in clockwise walk order. */
    private static int[][] interiorRing() {
        final List<int[]> cells = new ArrayList<>();
        for (int x = 1; x <= 5; x++) cells.add(new int[] { 1, x });
        for (int r = 2; r <= 5; r++) cells.add(new int[] { r, 5 });
        for (int x = 4; x >= 1; x--) cells.add(new int[] { 5, x });
        for (int r = 4; r >= 2; r--) cells.add(new int[] { r, 1 });
        return cells.toArray(new int[0][]);
    }

    /** Chamber rows centered on the tier's silo mid-row, like the duct. */
    private static int spiralDOff(int tier) {
        return 3 - edgeFor(tier) / 2;
    }

    // -------------------------- the Large Turbine Rotor Assembly (SD-4/TC-4, optional piece)

    private static final int GALLERY_LENGTH = 18;
    /** Rows 0..14 are the 7x5 gallery proper; rows 15..17 the 7x7 OUTPUT MANIFOLD. */
    private static final int MANIFOLD_START = 15;
    private static final int GALLERY_STAGES = 8;

    private static String galleryPieceName(String base) {
        return base + ".gallery";
    }

    /**
     * The rotor gallery + output manifold (TC-4): the gallery proper is 7 wide x 5 tall
     * (world y 0..4) over rows 0..14 - front wall row 0 with the TURBINE CONTROL BLOCK
     * socket ('q') center and a Maintenance socket ('n') beside it, hollow bore, SEVEN
     * rotor stage sockets ('9', Turbine Housings seating TC-4 rotors, spin-agnostic) on
     * the centerline's odd rows 1..13. Rows 15..17 widen into the 7x7x3 hollow OUTPUT
     * MANIFOLD (world y 0..6): its front plane is walled except the mouth matching the
     * gallery bore, the EIGHTH stage socket hangs center-bore in the mouth (row 15), and
     * the 4 Dynamo sockets ('m') sit mid-height across the back face. Total length is
     * still 18 rows, so the world span is unchanged from SD-4. [y top-first][18 c-rows][7
     * a-chars].
     */
    private static String[][] galleryShape() {
        final String[][] layers = new String[7][];
        for (int b = 0; b < 7; b++) {
            final int y = 6 - b;
            final String[] rows = new String[GALLERY_LENGTH];
            for (int c = 0; c < GALLERY_LENGTH; c++) {
                final char[] row = new char[7];
                for (int a = 0; a < 7; a++) {
                    row[a] = galleryCell(y, c, a);
                }
                rows[c] = new String(row);
            }
            layers[b] = rows;
        }
        return layers;
    }

    /** One cell of the gallery piece, by world height y (0..6), row c (0..17), column a. */
    private static char galleryCell(int y, int c, int a) {
        if (c < MANIFOLD_START) {
            // Gallery proper: 7 wide x 5 tall shell, nothing above it.
            if (y > 4) return ' ';
            if (y == 2 && a == 3 && c % 2 == 1 && c <= 13) return '9'; // stages 1..7
            if (y == 0 || y == 4 || a == 0 || a == 6 || c == 0) return '8';
            return 'A';
        }
        // Output manifold: hollow 7x7x3 box over the full 7-block height.
        if (y == 3 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {
            return 'm'; // TC-1: the 4 Dynamo sockets, back face mid-height
        }
        // TC-2 control sockets live on the manifold's BACK face - the gallery's front wall
        // is entombed against the Spiral's back wall, so a redstone line could never
        // reach a control block there.
        if (c == GALLERY_LENGTH - 1 && a == 3) {
            if (y == 2) return 'q'; // Turbine Control Block (redstone cycle input)
            if (y == 1) return 'n'; // Maintenance beside it
        }
        if (y == 2 && c == MANIFOLD_START && a == 3) return '9'; // stage 8, in the mouth
        if (y == 0 || y == 6 || a == 0 || a == 6 || c == GALLERY_LENGTH - 1) return '8';
        if (c == MANIFOLD_START) {
            // Front plane: walled except the mouth matching the gallery bore (a 1..5, y 1..3).
            return (a >= 1 && a <= 5 && y >= 1 && y <= 3) ? 'A' : '8';
        }
        return 'A';
    }

    /** Gallery starts one row past the chamber's south face, same east-strip columns. */
    private static int galleryDOff(int tier) {
        return -4 - edgeFor(tier) / 2;
    }

    /** Duct starts two blocks past the tier's east wall: wall, rib, then duct flush on it. */
    private static int ductHOff(int tier) {
        return -(edgeFor(tier) / 2 + 2);
    }

    /** Duct rows centered on the tier's silo mid-row (world c = 17 + edge/2). */
    private static int ductDOff(int tier) {
        return 1 - edgeFor(tier) / 2;
    }

    // ------------------- one-projection building shells (hologram/build only, never checked)

    private static String shellPieceName(String base, String which) {
        return base + ".shell." + which;
    }

    /**
     * Build-only leniency for shell cells: place into air, NEVER touch an existing block
     * (a re-projection must not replace a player's swapped-in hatches), and never fail a
     * survival walk (SKIP instead of REJECT so later pieces stay reachable).
     */
    private static <X> IStructureElement<X> shellOnly(IStructureElement<X> delegate) {
        return new IStructureElement<X>() {

            @Override
            public boolean check(X t, World world, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean spawnHint(X t, World world, int x, int y, int z, ItemStack trigger) {
                return delegate.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(X t, World world, int x, int y, int z, ItemStack trigger) {
                if (!world.isAirBlock(x, y, z)) return false;
                return delegate.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(X t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (!world.isAirBlock(x, y, z)) return PlaceResult.SKIP;
                return delegate.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /** Accepts air like isAir() but never places or clears - dock cells are player territory. */
    private static IStructureElement<MTEReactorCoreMk1> dockAir() {
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                return world.isAirBlock(x, y, z);
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                return true;
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                return false;
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                return PlaceResult.SKIP;
            }
        };
    }

    /**
     * Embeds a module's shape as a build-only shell in the main station's frame: chars are
     * remapped onto this structure definition's elements, the module's controller cell
     * becomes a skip (the dock marker must stay clear for the player-placed controller),
     * and reversing rows flips the shell for modules that face opposite the main station.
     */
    private static String[][] remapShape(String[][] src, String from, String to, boolean reverseRows) {
        final String[][] out = new String[src.length][];
        for (int i = 0; i < src.length; i++) {
            final String[] rows = new String[src[i].length];
            for (int j = 0; j < src[i].length; j++) {
                final char[] chars = src[i][reverseRows ? src[i].length - 1 - j : j].toCharArray();
                for (int k = 0; k < chars.length; k++) {
                    final int m = from.indexOf(chars[k]);
                    if (m >= 0) chars[k] = to.charAt(m);
                }
                rows[j] = new String(chars);
            }
            out[i] = rows;
        }
        return out;
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

    /** Overwrites one cell of the silo grid's FRONT row (octagon row 0 = grid row 1). */
    private static void setFrontCell(String[] layer, int xx, char ch) {
        final char[] row = layer[1].toCharArray();
        row[xx] = ch;
        layer[1] = new String(row);
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
        // TC-1 face panel: dedicated hatch sockets on the flat front face around the main
        // station. The front row spans >= 5 flat cells at every tier (CUT <= n/2 - 2), so
        // the 5-wide panel always fits. Bottom-up layer i = list index domeH + wallH-1 - i.
        final int mid = edgeFor(tier) / 2 + 1;
        final String[] panel0 = layers.get(domeH + wallH - 2); // pad level: the '~' row
        setFrontCell(panel0, mid - 2, 'd'); // Input Hatch
        setFrontCell(panel0, mid - 1, 'b'); // Maintenance
        setFrontCell(panel0, mid + 1, 'e'); // Output Hatch
        setFrontCell(panel0, mid + 2, 'd'); // Input Hatch
        final String[] panel1 = layers.get(domeH + wallH - 3); // level 2: energy quad
        setFrontCell(panel1, mid - 2, 'a');
        setFrontCell(panel1, mid - 1, 'a');
        setFrontCell(panel1, mid, 'p'); // TC-2: the Silo Control Block socket
        setFrontCell(panel1, mid + 1, 'a');
        setFrontCell(panel1, mid + 2, 'a');
        final String[] panel2 = layers.get(domeH + wallH - 4); // level 3: bus corners
        setFrontCell(panel2, mid - 2, 'f'); // Input Bus
        setFrontCell(panel2, mid + 2, 'g'); // Output Bus
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
                    t.walkHousings.add(new int[] { dist, world.getBlockMetadata(x, y, z), x, y, z });
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

    // ------------------------------------------------- creative socket auto-fitting

    /** Places a GT machine (hatch / control block) exactly as ItemMachines.placeBlockAt does. */
    public static boolean placeGtMachine(World world, int x, int y, int z, short mteId, ForgeDirection facing) {
        if (mteId <= 0 || GregTechAPI.METATILEENTITIES[mteId] == null) return false;
        final int baseType = GregTechAPI.METATILEENTITIES[mteId].getTileEntityBaseType();
        // 1.7.10's Chunk.setBlock returns false when block AND meta already match, so an
        // identical machine already in the cell reads as "failed" - treat it as success.
        if (world.getBlock(x, y, z) == GregTechAPI.sBlockMachines) {
            final net.minecraft.tileentity.TileEntity existing = world.getTileEntity(x, y, z);
            if (existing instanceof IGregTechTileEntity && ((IGregTechTileEntity) existing).getMetaTileID() == mteId) {
                if (facing != null && facing != ForgeDirection.UNKNOWN) {
                    ((IGregTechTileEntity) existing).setFrontFacing(facing);
                }
                return true;
            }
            world.setBlockToAir(x, y, z);
        }
        if (!world.setBlock(x, y, z, GregTechAPI.sBlockMachines, baseType, 3)) return false;
        final net.minecraft.tileentity.TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return false;
        final IGregTechTileEntity gte = (IGregTechTileEntity) te;
        gte.setInitialValuesAsNBT(null, mteId);
        if (facing != null && facing != ForgeDirection.UNKNOWN) {
            gte.setFrontFacing(facing);
        }
        return true;
    }

    /**
     * The socket's exposed side: first air neighbour, horizontal preferred, else up/down.
     * Only meaningful ONCE the surrounding structure exists - during the build every
     * neighbour is still air, which is why orientation runs as a pass after construct().
     */
    private static ForgeDirection outwardFacing(World world, int x, int y, int z) {
        final ForgeDirection[] order = { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST,
            ForgeDirection.WEST, ForgeDirection.UP, ForgeDirection.DOWN };
        for (final ForgeDirection d : order) {
            if (world.isAirBlock(x + d.offsetX, y + d.offsetY, z + d.offsetZ)) return d;
        }
        return ForgeDirection.NORTH;
    }

    /** Cells auto-fitted with a hatch during the last creative build, for the facing pass. */
    private final List<int[]> fittedCells = new ArrayList<>();

    /**
     * Points every auto-fitted hatch at its exposed face. Dynamos and energy hatches only
     * work through their FRONT, so this must run after the walls are up - during the build
     * every neighbour is still air and the guess would be meaningless.
     */
    public int orientFittedHatches() {
        final World world = getBaseMetaTileEntity().getWorld();
        int oriented = 0;
        for (final int[] c : fittedCells) {
            if (!world.blockExists(c[0], c[1], c[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
            if (!(te instanceof IGregTechTileEntity)) continue;
            ((IGregTechTileEntity) te).setFrontFacing(outwardFacing(world, c[0], c[1], c[2]));
            oriented++;
        }
        return oriented;
    }

    /**
     * Wraps a socket element so a CREATIVE build (the /btsbuild command or a hologram
     * projector) drops the real hatch in rather than a blank casing - the structure comes
     * out working. An existing GT machine in the cell is never overwritten, so re-running a
     * build cannot wipe hatches the player has already configured. Survival is untouched:
     * players still supply and place their own hatches.
     */
    private static IStructureElement<MTEReactorCoreMk1> autoFit(IStructureElement<MTEReactorCoreMk1> base,
        java.util.function.Supplier<Short> mteId) {
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                return base.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                return base.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public java.util.List<String> getDescription(MTEReactorCoreMk1 t) {
                return base.getDescription(t);
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity) {
                    t.fittedCells.add(new int[] { x, y, z });
                    return true;
                }
                final Short id = mteId.get();
                if (id != null && placeGtMachine(
                    world,
                    x,
                    y,
                    z,
                    id,
                    t.getBaseMetaTileEntity()
                        .getFrontFacing())) {
                    // Facing is fixed up by orientFittedHatches() once the walls exist.
                    t.fittedCells.add(new int[] { x, y, z });
                    return true;
                }
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /** MTE id behind an ItemList entry (its stack damage), or 0 when the entry is absent. */
    private static short idOf(ItemList entry) {
        try {
            final ItemStack stack = entry.get(1L);
            return stack == null ? 0 : (short) stack.getItemDamage();
        } catch (final Exception ignored) {
            return 0;
        }
    }

    private static short idOf(ItemStack stack) {
        return stack == null ? 0 : (short) stack.getItemDamage();
    }

    @Override
    public IStructureDefinition<MTEReactorCoreMk1> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final StructureDefinition.Builder<MTEReactorCoreMk1> builder = StructureDefinition.builder();
            builder.addShape(floorPieceName(mName), transpose(floorShape()));
            for (int t = 1; t <= MAX_TIER; t++) {
                builder.addShape(ductPieceName(mName, t), transpose(ductShape(t)));
            }
            builder.addShape(spiralPieceName(mName), transpose(spiralShape()));
            builder.addShape(galleryPieceName(mName), transpose(galleryShape()));
            // Build-only shells of the module buildings (their controllers verify them).
            // Dedicated lenient chars: shells must never affect the CHECKED pieces' elements.
            builder.addShape(
                shellPieceName(mName, "hall"),
                transpose(remapShape(MTEModuleTurbineHall.hallShape(), "SUWPA~", "1234  ", false)));
            builder.addShape(
                shellPieceName(mName, "towernw"),
                transpose(remapShape(MTEModuleCoolingTower.towerShape(), "NS~", "67 ", false)));
            builder.addShape(
                shellPieceName(mName, "towersw"),
                transpose(remapShape(MTEModuleCoolingTower.towerShape(), "NS~", "67 ", true)));
            for (int t = 1; t <= MAX_TIER; t++) {
                builder.addShape(pieceName(mName, t), transpose(coreShape(t)));
            }
            STRUCTURE_DEFINITION = builder.addElement('B', ofBlock(GregTechAPI.sBlockReinforced, 3))
                .addElement('I', ofBlock(GregTechAPI.sBlockReinforced, 1))
                .addElement('A', isAir())
                // TC-1: silo walls are PLAIN casings - every hatch lives in a dedicated
                // socket cell on the flat front face (chars 'a','b','d','e','f','g' below).
                .addElement('C', onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement()))
                .addElement(
                    'D',
                    onElementPass(x -> ++x.mCasing, Casings.EuropiumReinforcedRadiationProofMachineCasing.asElement()))
                .addElement('E', onElementPass(x -> ++x.mCasing, Casings.RadiantNaquadahAlloyCasing.asElement()))
                // Silo face-panel sockets: tier-agnostic Radiation Proof blank filler (the
                // panel reads as a control panel on every tier); blanks count as casings.
                .addElement(
                    'a',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Energy)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Energy_IV)))
                .addElement(
                    'b',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Maintenance)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Maintenance)))
                .addElement(
                    'd',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(InputHatch)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Input_IV)))
                .addElement(
                    'e',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(OutputHatch)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Output_IV)))
                .addElement(
                    'f',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(InputBus)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Input_Bus_IV)))
                .addElement(
                    'g',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(OutputBus)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(ItemList.Hatch_Output_Bus_IV)))
                .addElement(
                    'p',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(PanelHatchElement.SiloPanel)
                            .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                            .hint(1)
                            .buildAndChain(
                                onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
                        () -> idOf(BTSMachines.siloControlBlock)))
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
                        // Air-or-nothing tail that never places: a creative re-projection must
                        // not delete a docked module controller by setting its cell to air.
                        dockAir()))
                .addElement('M', Casings.SolidSteelMachineCasing.asElement())
                .addElement('G', onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement()))
                .addElement('F', fuelCellElement())
                .addElement('J', portCellElement())
                .addElement('T', Casings.TungstensteelTurbineCasing.asElement())
                .addElement('H', ofChain(housingElement(), Casings.TungstensteelTurbineCasing.asElement()))
                // TC-1: spiral and gallery walls are PLAIN casings; hatches live in the
                // dedicated socket chars below ('i','j','k','l' spiral / 'm','n' gallery).
                .addElement('5', Casings.TungstensteelTurbineCasing.asElement())
                .addElement(
                    'i',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(SpiralHatchElement.Injector)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(BTSMachines.precisionInjector)))
                .addElement(
                    'j',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(SpiralHatchElement.Sensor)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(BTSMachines.spineSensor)))
                .addElement(
                    'k',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(InputHatch)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Input_IV)))
                .addElement(
                    'l',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Maintenance)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Maintenance)))
                .addElement('8', Casings.TungstensteelTurbineCasing.asElement())
                .addElement(
                    'm',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Dynamo)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Dynamo_IV)))
                .addElement(
                    'n',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Maintenance)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Maintenance)))
                .addElement(
                    'q',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(PanelHatchElement.TurbinePanel)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(BTSMachines.turbineControlBlock)))
                .addElement('9', ofChain(new IStructureElement<MTEReactorCoreMk1>() {

                    @Override
                    public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                        if (world.getBlock(x, y, z) == BTSBlocks.turbineHousing) {
                            t.walkGalleryStages++;
                            // TC-4: stage seats are read back for rotors after the walk.
                            t.walkStageCells.add(new int[] { x, y, z });
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
                    public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                        ItemStack trigger) {
                        world.setBlock(x, y, z, BTSBlocks.turbineHousing, 0, 2);
                        return true;
                    }

                    @Override
                    public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                        ItemStack trigger, AutoPlaceEnvironment env) {
                        return PlaceResult.SKIP;
                    }
                }, Casings.TungstensteelTurbineCasing.asElement()))
                .addElement('1', shellOnly(Casings.CleanStainlessSteelMachineCasing.asElement()))
                .addElement('2', shellOnly(Casings.TungstensteelTurbineCasing.asElement()))
                .addElement('3', shellOnly(ofBlock(Blocks.glass, 0)))
                .addElement('4', shellOnly(Casings.SteelPipeCasing.asElement()))
                .addElement('6', shellOnly(ofBlock(GregTechAPI.sBlockConcretes, 8)))
                .addElement('7', shellOnly(Casings.SolidSteelMachineCasing.asElement()))
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
        // Control-block panels re-register during the silo/gallery walks below; drop the
        // old links now so a failed floor walk cannot keep stale panels reading this MTE.
        resetSiloPanel();
        resetTurbinePanel();
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
            // The face panel's Silo Control Block registers per probe like any hatch.
            resetSiloPanel();
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
            spineBuilt = checkPiece(
                ductPieceName(mName, reactorTier),
                ductHOff(reactorTier),
                2,
                ductDOff(reactorTier),
                null);
            computeSpine();
            // The Spiral (optional): the chamber's hatches register here during the walk. A
            // FAILED walk still registered Input/Maintenance hatches before aborting - mark
            // the general lists and truncate back, keeping the silo's own hatches intact.
            spiralInjectors.clear();
            spineSensor = null;
            final int inputMark = mInputHatches.size();
            final int maintMark = mMaintenanceHatches.size();
            spiralBuilt = checkPiece(spiralPieceName(mName), -18, SPIRAL_HEIGHT - 1, spiralDOff(reactorTier), null);
            if (!spiralBuilt) {
                spiralInjectors.clear();
                spineSensor = null;
                lastGoodInjectors = 0;
                while (mInputHatches.size() > inputMark) {
                    mInputHatches.remove(mInputHatches.size() - 1);
                }
                while (mMaintenanceHatches.size() > maintMark) {
                    mMaintenanceHatches.remove(mMaintenanceHatches.size() - 1);
                }
            }
            // The Rotor Assembly (optional): same mark-and-truncate discipline for its
            // Dynamo/Maintenance registrations when the walk fails.
            walkGalleryStages = 0;
            walkStageCells.clear();
            resetTurbinePanel();
            final int dynamoMark = mDynamoHatches.size();
            final int galleryMaintMark = mMaintenanceHatches.size();
            galleryBuilt = checkPiece(galleryPieceName(mName), -18, 6, galleryDOff(reactorTier), null);
            if (galleryBuilt) {
                stageCells.clear();
                stageCells.addAll(walkStageCells);
                refreshRotorStats();
            } else {
                stageCells.clear();
                galleryStages = 0;
                avgRotorFactorMilli = 0;
                resetTurbinePanel();
                while (mDynamoHatches.size() > dynamoMark) {
                    mDynamoHatches.remove(mDynamoHatches.size() - 1);
                }
                while (mMaintenanceHatches.size() > galleryMaintMark) {
                    mMaintenanceHatches.remove(mMaintenanceHatches.size() - 1);
                }
            }
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
            spiralBuilt = false;
            spiralInjectors.clear();
            spineSensor = null;
            lastGoodInjectors = 0;
            galleryBuilt = false;
            galleryStages = 0;
            stageCells.clear();
            avgRotorFactorMilli = 0;
            resetSiloPanel();
            resetTurbinePanel();
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
        spineHousingCells.clear();
        for (final int[] housing : walkHousings) {
            spineHousingCells.add(new int[] { housing[2], housing[3], housing[4] });
        }
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
        if (mMachine && reactorTier > 0) {
            // Formed plant: platform/silo are done, but the duct, chamber and module shells
            // are still legitimate build targets (skip floor/silo so a mismatched trigger
            // tier can never auto-place a second silo inside the formed one).
            final int duct = survivalBuildPiece(
                ductPieceName(mName, reactorTier),
                stackSize,
                ductHOff(reactorTier),
                2,
                ductDOff(reactorTier),
                elementBudget,
                env,
                false,
                true);
            if (duct >= 0) return duct;
            final int spiral = survivalBuildPiece(
                spiralPieceName(mName),
                stackSize,
                -18,
                SPIRAL_HEIGHT - 1,
                spiralDOff(reactorTier),
                elementBudget,
                env,
                false,
                true);
            if (spiral >= 0) return spiral;
            final int galleryFormed = survivalBuildPiece(
                galleryPieceName(mName),
                stackSize,
                -18,
                6,
                galleryDOff(reactorTier),
                elementBudget,
                env,
                false,
                true);
            if (galleryFormed >= 0) return galleryFormed;
            final int hall = survivalBuildPiece(
                shellPieceName(mName, "hall"),
                stackSize,
                -11,
                3,
                -13,
                elementBudget,
                env,
                false,
                true);
            if (hall >= 0) return hall;
            final int nw = survivalBuildPiece(
                shellPieceName(mName, "towernw"),
                stackSize,
                25,
                19,
                17,
                elementBudget,
                env,
                false,
                true);
            if (nw >= 0) return nw;
            return survivalBuildPiece(
                shellPieceName(mName, "towersw"),
                stackSize,
                25,
                19,
                -16,
                elementBudget,
                env,
                false,
                true);
        }
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
        final int ductBuilt = survivalBuildPiece(
            ductPieceName(mName, t),
            stackSize,
            ductHOff(t),
            2,
            ductDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (ductBuilt >= 0) return ductBuilt;
        final int spiralPiece = survivalBuildPiece(
            spiralPieceName(mName),
            stackSize,
            -18,
            SPIRAL_HEIGHT - 1,
            spiralDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (spiralPiece >= 0) return spiralPiece;
        final int galleryPiece = survivalBuildPiece(
            galleryPieceName(mName),
            stackSize,
            -18,
            6,
            galleryDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (galleryPiece >= 0) return galleryPiece;
        final int hallBuilt = survivalBuildPiece(
            shellPieceName(mName, "hall"),
            stackSize,
            -11,
            3,
            -13,
            elementBudget,
            env,
            false,
            true);
        if (hallBuilt >= 0) return hallBuilt;
        final int nwBuilt = survivalBuildPiece(
            shellPieceName(mName, "towernw"),
            stackSize,
            25,
            19,
            17,
            elementBudget,
            env,
            false,
            true);
        if (nwBuilt >= 0) return nwBuilt;
        return survivalBuildPiece(
            shellPieceName(mName, "towersw"),
            stackSize,
            25,
            19,
            -16,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        if (!hintsOnly) {
            fittedCells.clear();
        }
        final int t = tierFromTrigger(stackSize);
        buildPiece(floorPieceName(mName), stackSize, hintsOnly, CENTER, 0, SILO_FRONT);
        buildPiece(pieceName(mName, t), stackSize, hintsOnly, siloHOff(t), siloVOff(t), siloDOff(t));
        buildPiece(ductPieceName(mName, t), stackSize, hintsOnly, ductHOff(t), 2, ductDOff(t));
        buildPiece(spiralPieceName(mName), stackSize, hintsOnly, -18, SPIRAL_HEIGHT - 1, spiralDOff(t));
        buildPiece(galleryPieceName(mName), stackSize, hintsOnly, -18, 6, galleryDOff(t));
        // One projection builds the whole plant: the module building shells come too, and
        // only the three module controllers remain to be stood on their dock markers.
        buildPiece(shellPieceName(mName, "hall"), stackSize, hintsOnly, -11, 3, -13);
        buildPiece(shellPieceName(mName, "towernw"), stackSize, hintsOnly, 25, 19, 17);
        buildPiece(shellPieceName(mName, "towersw"), stackSize, hintsOnly, 25, 19, -16);
        if (!hintsOnly) {
            // Every wall is up now, so each auto-fitted hatch can be pointed at its exposed
            // face - dynamos and energy hatches only work through their front.
            orientFittedHatches();
        }
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

        // Stamps accumulated while the plant was idle/scrammed are not timing faults -
        // discard them so the first slug of a new campaign scores only fresh injections.
        // (Seamless decay-cycle rollovers keep wasRunningLastTick true and are untouched.)
        if (!wasRunningLastTick) {
            for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                if (injector != null && injector.isValid()) injector.drainEvents();
            }
        }

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
        if (galleryBuilt && galleryStages > 0) {
            // TC-3 finale: FAN RPM IS THE CENTRAL VARIABLE. The produced steam feeds the
            // chain's standing buffer (redstone cycles drink it in 20-tick slugs), and
            // EU/t = L/t x speed x eff x (RPM/1000) x avgRotorFactor - the avg already
            // carries stages/8 (a missing or shattered rotor contributes a zero stage).
            if (depleteInput(Materials.Water.getFluid(flowPerTick))
                || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                steamBuffer = Math.min(steamBuffer + steamPerTick, STEAM_BUFFER_CAP_TICKS * steamPerTick);
                // /500_000 (not /1_000_000): back-solved so a fully optimized T5 - 201 naq
                // pillars, all-riding spine, redline RPM, 8 fresh rotors - peaks ~1.10B and
                // averages ~1.05B EU/t across the decay sawtooth, landing on the settled
                // D-01 ~1B ceiling instead of half of it.
                long eu = spineBoostedSteam(true) * rpmMilli / 500_000L * avgRotorFactorMilli / 1000L;
                // Clamp to summed dynamo capacity BEFORE emitting - GT explodes the whole
                // multiblock when the injected EU exceeds it - and below the int-overflow
                // EU-duplication threshold, exactly like the hall did.
                long capacity = 0;
                for (final gregtech.api.metatileentity.implementations.MTEHatchDynamo dynamo : gregtech.api.util.GTUtility
                    .validMTEList(mDynamoHatches)) {
                    capacity += (long) dynamo.maxAmperesOut() * dynamo.maxEUOutput();
                }
                eu = Math.min(Math.min(eu, capacity), Integer.MAX_VALUE);
                if (eu > 0) {
                    addEnergyOutputMultipleDynamos(eu, true);
                }
                lastEUOutput = eu;
                emitWasteWater();
            } else {
                starveTick();
            }
        } else if (hall != null) {
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
        runTurbineTick();
        endRecipeProcessing();
        return true;
    }

    /**
     * Steam after the Spine: speed multiplies throughput; efficiency only pays out in EU.
     * (TC-3: the retired injector chamber boost is SUBSUMED by RPM - the injectors now
     * feed redstone-paced cycles instead of multiplying the flow.)
     */
    private long spineBoostedSteam(boolean withEfficiency) {
        long steam = steamPerTick * spineSpeedMilli / 1000;
        if (withEfficiency) {
            steam = steam * spineEffMilli / 1000;
        }
        return steam;
    }

    // ------------------------------------------------------------ Spiral chamber runtime

    /** Propellant stage factor P (x1000) per fluid; 1000 for unknown. */
    private static int propellantMilliOf(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) return 1000;
        final net.minecraftforge.fluids.Fluid fluid = stack.getFluid();
        if (fluid == Materials.Benzene.mFluid) return 1250;
        if (fluid == Materials.GasolinePremium.mFluid) return 1500;
        if (fluid == GTPPFluids.H8N4C2O4RocketFuel) return 2000;
        if (fluid == BTSFluids.naqPropellantT1) return 3000;
        if (fluid == BTSFluids.naqPropellantT2) return 4000;
        if (fluid == BTSFluids.naqPropellantT3) return 5000;
        if (fluid == BTSFluids.naqPropellantT4) return 5500;
        if (fluid == BTSFluids.naqPropellantT5) return 6000;
        if (fluid == BTSFluids.naqPropellantT6) return 6500;
        return 1000;
    }

    /** Coolant ladder for the chamber, best first: {quality Q x1000}. */
    private FluidStack chamberCoolantStack(int index, int litres) {
        switch (index) {
            case 0:
                return Materials.Time.getMolten(litres);
            case 1:
                return new FluidStack(TFFluids.fluidCryotheum, litres);
            case 2:
                return Materials.SuperCoolant.getFluid(litres);
            case 3:
                return GTModHandler.getIC2Coolant(litres);
            case 4:
                return GTModHandler.getDistilledWater(litres);
            default:
                return Materials.Water.getFluid(litres);
        }
    }

    private static final int[] CHAMBER_COOLANT_Q = { 160_000, 40_000, 12_000, 4_000, 1_500, 1_000 };
    private static final String[] CHAMBER_COOLANT_NAMES = { "Tachyon Rich Temporal Fluid", "Liquid Cryotheum",
        "Super Coolant", "IC2 Coolant", "Distilled Water", "Water" };

    /**
     * TC-3 turbine runtime, each running tick: score a due cycle window, accept a pending
     * redstone-requested cycle (drinking one 20-tick steam slug from the buffer and echoing
     * the Sensor), and drink coolant scaled with the SQUARE of the fan RPM. Starvation
     * heats the chamber until the rotors SEIZE. No redstone clock = RPM decays to zero.
     */
    private void runTurbineTick() {
        if (!spiralBuilt) return;
        final long now = getBaseMetaTileEntity().getWorld()
            .getTotalWorldTime();
        if (now - lastChamberTick > 20) {
            // Chamber was not ticking for at least one full slug span: discard timestamps
            // recorded while stopped (keep the fluid - only the timing is stale).
            for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                if (injector != null && injector.isValid()) injector.drainEvents();
            }
        }
        lastChamberTick = now;

        // Score the last accepted cycle one tick AFTER its +/-2t window closes: every
        // achievable in-window fill was recorded on a fully completed earlier tick, so
        // scoring never depends on tile-entity tick order (the SD-3 deferral pattern).
        if (cycleEvalTick != 0 && now >= cycleEvalTick) {
            evaluateCycle(cycleEvalTick - 3, cycleHadSteam);
            cycleEvalTick = 0;
        }

        // Accept a pending redstone cycle: needs the turbine gen formed, a free window
        // slot, and a full 20-tick steam slug in the buffer. A slugless cycle is MISSED -
        // the injectors' litres still burn at scoring time, but no RPM is gained and the
        // Sensor stays silent (its pulse is the ACCEPTED-cycle confirmation).
        if (pendingCycle && cycleEvalTick == 0 && galleryBuilt && now - lastCycleTick >= 5) {
            pendingCycle = false;
            lastCycleTick = now;
            final long slug = CYCLE_SLUG_TICKS * steamPerTick;
            cycleHadSteam = slug > 0 && steamBuffer >= slug;
            if (cycleHadSteam) {
                steamBuffer -= slug;
                recentCycles.addLast(now);
                if (spineSensor != null && spineSensor.isValid()) {
                    spineSensor.pulse(2);
                }
            }
            cycleEvalTick = now + 3;
        }

        // Coolant demand scales with RPM squared: drain = ceil(200 x (RPM/1000)^2 / Q).
        final double rpmNorm = rpmMilli / 1_000_000.0;
        final int demand = (int) Math.ceil(200.0 * rpmNorm * rpmNorm);
        if (demand <= 0) {
            chamberCoolantDrain = 0;
            chamberCoolantName = "idle";
            return;
        }
        boolean cooled = false;
        for (int i = 0; i < CHAMBER_COOLANT_Q.length; i++) {
            final int litres = Math.max(1, (int) Math.ceil(demand * 1000.0 / CHAMBER_COOLANT_Q[i]));
            final FluidStack coolant = chamberCoolantStack(i, litres);
            if (coolant != null && depleteInput(coolant)) {
                cooled = true;
                chamberCoolantDrain = litres;
                chamberCoolantName = CHAMBER_COOLANT_NAMES[i];
                break;
            }
        }
        if (!cooled) {
            chamberCoolantDrain = 0;
            chamberCoolantName = "STARVED";
            // Uncooled at speed: heat builds each second until the rotors tear apart.
            if (now % 20 == 10) {
                heat += (int) Math.ceil(rpmNorm * rpmNorm) * 3;
                if (heat >= SCRAM_HEAT) {
                    seize();
                }
            }
        }
    }

    /**
     * Scores one cycle's +/-2t injection windows. Timed litres burn either way; RPM is
     * gained only when the cycle had its steam slug: gain = RPM_GAIN x P x timed/8 (P =
     * the LOWEST propellant factor among timed injectors), clamped at the redline.
     */
    private void evaluateCycle(long cycleTick, boolean hadSteam) {
        int good = 0;
        int minPropMilli = Integer.MAX_VALUE;
        int wastedLitres = 0;
        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
            if (injector == null || !injector.isValid()) continue;
            long inWindow = 0;
            long outWindow = 0;
            for (final long[] event : injector.drainEvents()) {
                if (event[0] >= cycleTick - 2 && event[0] <= cycleTick + 2) {
                    inWindow += event[1];
                } else {
                    outWindow += event[1];
                }
            }
            if (inWindow == 1) {
                good++;
                minPropMilli = Math.min(minPropMilli, propellantMilliOf(injector.storedPropellant()));
            } else {
                wastedLitres += (int) inWindow;
            }
            wastedLitres += (int) outWindow;
            injector.consumeLitres((int) (inWindow + outWindow));
        }
        if (wastedLitres > 0) {
            // Each wasted litre is a heat spike, capped so a dumped bucket cannot instantly
            // destroy the plant - it "only" pushes it hard toward SCRAM.
            heat += Math.min(wastedLitres, 8) * 50;
            if (heat >= MELTDOWN_HEAT) {
                meltdown();
            } else if (heat >= SCRAM_HEAT) {
                scram();
            }
        }
        lastGoodInjectors = good;
        if (good > 0 && minPropMilli < Integer.MAX_VALUE) {
            lastCyclePropMilli = minPropMilli;
            if (hadSteam) {
                // gain(milli) = RPM_GAIN x P x timed/8; see the RPM_GAIN equilibrium note.
                final long gainMilli = (long) RPM_GAIN * minPropMilli * good / 8;
                rpmMilli = (int) Math.min(RPM_REDLINE_MILLI, rpmMilli + gainMilli);
            }
        } else {
            lastCyclePropMilli = 1000;
        }
    }

    /** A redstone rising edge on the Turbine Control Block: run one cycle when possible. */
    /**
     * TEST AID (set by /btsbuild, NBT-persisted): pulses a turbine cycle every 20 ticks so a
     * freshly commanded plant spins up without wiring a redstone clock first. Redstone
     * remains the real mechanic - this is a bench clock, and the GUI says so.
     */
    private boolean debugAutoCycle;

    public boolean isDebugAutoCycle() {
        return debugAutoCycle;
    }

    public void setDebugAutoCycle(boolean value) {
        debugAutoCycle = value;
    }

    /**
     * Fills every Rod Loading Port of the formed silo with the given rod stack (test aid for
     * /btsbuild). Returns how many ports were stocked.
     */
    /** Turbine Housing cells this plant owns: the Spine's 4 rotor sockets + 8 gallery stages. */
    public List<int[]> housingCells() {
        final List<int[]> all = new ArrayList<>(spineHousingCells);
        all.addAll(stageCells);
        return all;
    }

    public int stockRodPorts(ItemStack rods) {
        if (rods == null) return 0;
        final World world = getBaseMetaTileEntity().getWorld();
        int stocked = 0;
        for (final int[] p : portCells) {
            if (!world.blockExists(p[0], p[1], p[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
            if (!(te instanceof TileRodLoadingPort)) continue;
            final TileRodLoadingPort port = (TileRodLoadingPort) te;
            for (int slot = 0; slot < TileRodLoadingPort.SLOTS; slot++) {
                if (port.getStackInSlot(slot) == null) {
                    port.setInventorySlotContents(slot, rods.copy());
                }
            }
            stocked++;
        }
        return stocked;
    }

    public void requestTurbineCycle() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide()) return;
        if (mMaxProgresstime > 0 && galleryBuilt && spiralBuilt) {
            pendingCycle = true;
        }
    }

    /**
     * TC-4 stage census (after every gallery walk + 1/s): a stage counts while its seat
     * holds a rotor; stageFactor = 1 - 5% per wear point; avgRotorFactor averages over ALL
     * 8 sockets, so missing housings/rotors already carry the stages/8 penalty.
     */
    private void refreshRotorStats() {
        if (!galleryBuilt) {
            galleryStages = 0;
            avgRotorFactorMilli = 0;
            return;
        }
        final World world = getBaseMetaTileEntity().getWorld();
        int count = 0;
        int sumMilli = 0;
        for (final int[] cell : stageCells) {
            if (!world.blockExists(cell[0], cell[1], cell[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(cell[0], cell[1], cell[2]);
            if (te instanceof TileTurbineHousing && ((TileTurbineHousing) te).hasRotor()) {
                count++;
                sumMilli += Math.max(0, 1000 - 50 * ((TileTurbineHousing) te).getRotorDamage());
            }
        }
        galleryStages = count;
        avgRotorFactorMilli = sumMilli / GALLERY_STAGES;
    }

    /**
     * TC-4 wear, rolled once a second per seated stage rotor: chance = (RPM / redline) /
     * 600 - full tilt averages one wear point per 10 minutes, a shattered rotor (10
     * points) every ~100 minutes per stage. Like a plane turbine, every chip hurts.
     */
    private void rollRotorDamage() {
        if (rpmMilli <= 0 || stageCells.isEmpty()) return;
        final World world = getBaseMetaTileEntity().getWorld();
        final double chance = rpmMilli / (double) RPM_REDLINE_MILLI / 600.0;
        for (final int[] cell : stageCells) {
            if (!world.blockExists(cell[0], cell[1], cell[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(cell[0], cell[1], cell[2]);
            if (te instanceof TileTurbineHousing && ((TileTurbineHousing) te).hasRotor()
                && world.rand.nextDouble() < chance) {
                ((TileTurbineHousing) te).damageRotor(1);
            }
        }
    }

    /** Uncooled at speed: the Spine's rotors are torn apart and the plant slams shut. */
    private void seize() {
        final World world = getBaseMetaTileEntity().getWorld();
        for (final int[] cell : spineHousingCells) {
            if (world.blockExists(cell[0], cell[1], cell[2])
                && world.getBlock(cell[0], cell[1], cell[2]) == BTSBlocks.turbineHousing) {
                world.setBlockToAir(cell[0], cell[1], cell[2]);
            }
        }
        // The seizure also slams the fan to a stop - the RPM investment is lost.
        rpmMilli = 0;
        clearCampaign();
        stopMachine(SEIZURE_REASON);
        getBaseMetaTileEntity().disableWorking();
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
        // The chain's standing steam vents with the campaign; RPM only ever coasts down.
        steamBuffer = 0;
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

        // TC-3: the fan coasts - RPM decays 0.5% EVERY tick, running or not. No redstone
        // clock (or no plant) means it winds down to zero and the output dies with it.
        if (rpmMilli > 0) {
            rpmMilli = (int) ((long) rpmMilli * 995 / 1000);
        }
        if (!running) {
            // A cycle request or half-scored window cannot survive a stopped campaign.
            pendingCycle = false;
            cycleEvalTick = 0;
        }
        if (aTick % 20 == 0) {
            // Measured cycles/min for the panel/GUI, from the accepted-cycle record.
            final long now = aBaseMetaTileEntity.getWorld()
                .getTotalWorldTime();
            while (!recentCycles.isEmpty() && recentCycles.peekFirst() < now - 1200) {
                recentCycles.pollFirst();
            }
            cyclesPerMinute = recentCycles.size();
            // TC-4: refresh the stage census and roll the once-a-second rotor wear.
            if (galleryBuilt) {
                refreshRotorStats();
                if (running) {
                    rollRotorDamage();
                }
            }
        }

        // Residual Spine swirl is turbulence: |S|^2 x 5 heat per second while running. An
        // all-riding rotor pattern (S=4) outruns bare passive decay - bring cooling towers.
        if (running && spineSwirl != 0 && aTick % 20 == 0) {
            heat += spineSwirl * spineSwirl * 5;
            if (heat >= SCRAM_HEAT) {
                scram();
            }
        }

        final int towers = coolingTowerCount();

        // Bench clock for commanded test plants: a cycle a second, no redstone wiring.
        if (debugAutoCycle && aTick % 20 == 0) {
            requestTurbineCycle();
        }

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
        aNBT.setBoolean("btsDebugAutoCycle", debugAutoCycle);
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
        aNBT.setInteger("btsRpmMilli", rpmMilli);
        aNBT.setLong("btsSteamBuffer", steamBuffer);
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
        debugAutoCycle = aNBT.getBoolean("btsDebugAutoCycle");
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
        rpmMilli = Math.max(0, Math.min(RPM_REDLINE_MILLI, aNBT.getInteger("btsRpmMilli")));
        steamBuffer = Math.max(0, aNBT.getLong("btsSteamBuffer"));
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

    public boolean isSpiralBuilt() {
        return spiralBuilt;
    }

    public int getRegisteredInjectors() {
        return spiralInjectors.size();
    }

    /** Injectors timed in-window on the last scored cycle. */
    public int getLastGoodInjectors() {
        return lastGoodInjectors;
    }

    public int getRpm() {
        return rpmMilli / 1000;
    }

    public int getCyclesPerMinute() {
        return cyclesPerMinute;
    }

    public int getLastCyclePropMilli() {
        return lastCyclePropMilli;
    }

    public int getAvgRotorFactorMilli() {
        return avgRotorFactorMilli;
    }

    public int getChamberCoolantDrain() {
        return chamberCoolantDrain;
    }

    public String getChamberCoolantName() {
        return chamberCoolantName;
    }

    public boolean isGalleryBuilt() {
        return galleryBuilt;
    }

    public int getGalleryStages() {
        return galleryStages;
    }

    /** True when the Rotor Assembly is the plant's generator (takes precedence over a hall). */
    public boolean isGalleryActive() {
        return galleryBuilt && galleryStages > 0;
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
        if (spiralBuilt) {
            info.add(
                "Turbine: " + String.format("%,d", getRpm())
                    + " RPM (redline "
                    + String.format("%,d", RPM_REDLINE)
                    + ")  ·  "
                    + cyclesPerMinute
                    + " cyc/min  ·  timed "
                    + lastGoodInjectors
                    + "/8  ·  P x"
                    + String.format("%.2f", lastCyclePropMilli / 1000.0));
            info.add("Spiral coolant: " + chamberCoolantName + " (" + chamberCoolantDrain + " L/t)");
        }
        if (galleryBuilt) {
            info.add(
                "Rotor Assembly: " + galleryStages
                    + "/"
                    + GALLERY_STAGES
                    + " stages seated  ·  avg rotor factor x"
                    + String.format("%.2f", avgRotorFactorMilli / 1000.0));
        }
        info.add(
            isGalleryActive() ? "Generating: " + lastEUOutput + " EU/t (via Rotor Assembly)"
                : isTurbineInstalled() ? "Generating: " + lastEUOutput + " EU/t (via Turbine Hall)"
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
            .addInfo(gold + "1." + reset + " Hologram-project THIS controller: ONE projection builds it ALL -")
            .addInfo("platform, silo (GUI Build Tier 1-5), Spine duct, and every module building shell.")
            .addInfo(gold + "2." + reset + " Stand the 3 module controllers on the glowing markers, each")
            .addInfo("FACING AWAY from its building - they dock; each forms once its hatches are in.")
            .addInfo(gold + "3." + reset + " Every hatch has a DEDICATED socket cell (hologram-marked): the")
            .addInfo("silo face panel, the Spiral's walls, the gallery. Rods in the top ports; water in.")
            .addSeparator()
            .addInfo(aqua + "Fuel:" + reset + " rods cascade down the pillars - insert at the TOP ports,")
            .addInfo("bottom rod burns each cycle, depleted rods exit the output bus.")
            .addInfo(gray + "Bus assemblies still run, at a heavy penalty." + reset)
            .addInfo(aqua + "Spine:" + reset + " optional 3x3 duct off the silo's side, 4 rotor sockets.")
            .addInfo("Right-click a housing to set spin: ride the swirl for speed, oppose it for efficiency.")
            .addInfo(aqua + "Spiral:" + reset + " optional 7x7 chamber at the duct's end. Each redstone RISING")
            .addInfo("EDGE on the Turbine Control Block runs one CYCLE: a buffered 20-tick steam slug")
            .addInfo("+ a +/-2t window where 8 Precision Injectors each want EXACTLY 1L of propellant.")
            .addInfo("Misses waste the litre and heat the chamber; the Sensor echoes accepted cycles.")
            .addInfo(aqua + "RPM:" + reset + " each cycle adds 176 x P x timed/8 RPM; decay 0.5%/t; redline 12,000.")
            .addInfo("Coolant drain scales with RPM SQUARED - starve it and the rotors SEIZE.")
            .addInfo(aqua + "Rotor Assembly:" + reset + " optional gallery + 7x7x3 output manifold south of the")
            .addInfo("Spiral. Seat a TURBINE ROTOR in each of the 8 stage housings (right-click; wear")
            .addInfo("-5%/point, shatters at 10; sneak-click empty-handed to pull). 4 Dynamos in the")
            .addInfo("manifold's back face take EU/t = L/t x speed x eff x (RPM/1000) x avgRotorFactor.")
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
            .addEnergyHatch("Exactly 4 - silo face panel, the row above the main station", 1)
            .addMaintenanceHatch("Exactly 1 - left of the main station (+1 Spiral floor, +1 manifold back)", 1, 3, 4)
            .addInputHatch("Exactly 2 - silo face panel ends beside the main station (+1 Spiral floor)", 1, 3)
            .addOutputHatch("Exactly 1 - right of the main station", 1)
            .addInputBus("Exactly 1 - silo face panel, top-left corner", 1)
            .addOutputBus("Exactly 1 - silo face panel, top-right corner", 1)
            .addStructureInfo("Silo: 9-17 wide octagon, walls 2x the diameter tall, dome on top.")
            .addStructureInfo("Walls by tier: Radiation Proof -> Europium-Reinforced -> Radiant Naq Alloy.")
            .addStructureInfo("FACE PANEL (5 wide x 3 high around the main station, all tiers): pad level")
            .addStructureInfo("Input|Maint|STATION|Output|Input; 4 Energy sockets + the SILO CONTROL")
            .addStructureInfo("BLOCK center above; Input Bus / Output Bus in the top corners.")
            .addStructureInfo("Empty face-panel sockets are filled with Radiation Proof Machine Casing.")
            .addStructureInfo("Spiral sockets: 8 Injectors follow the helix (back/west/front/east mid-cells,")
            .addStructureInfo("top 8 levels), Sensor in the roof front-center, Input Hatch + Maintenance in")
            .addStructureInfo("the floor's front corners. OUTPUT MANIFOLD back face: TURBINE CONTROL BLOCK")
            .addStructureInfo("+ 1 Maintenance below it, and 4 Dynamos mid-height - all reachable outside.")
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
