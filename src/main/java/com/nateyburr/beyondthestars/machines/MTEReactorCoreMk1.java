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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
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
    /** Segments stacked per full fuel column. */
    private static final int SEGMENTS_PER_COLUMN = 6;
    /** Octagon corner cut per tier (0 unused). */
    private static final int[] CUT = { 0, 2, 2, 3, 3, 4 };
    /** Interior fuel-column capacity per tier (0 unused). */
    private static final int[] COLUMN_CAP = { 0, 45, 77, 109, 157, 201 };

    private static IStructureDefinition<MTEReactorCoreMk1> STRUCTURE_DEFINITION = null;

    /** Plant modules docked in this silo's walls (Space Elevator pattern). */
    public final ArrayList<MTEPlantModuleBase> plantModules = new ArrayList<>();

    private int mCasing;
    private int reactorTier;
    private int fuelSegments;
    private int activeColumns;
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
    /** Open dock positions {a, c}; a module controller stands directly on the marked floor. */
    private static final int[][] DOCK_POSITIONS = { { 38, 25 }, { 40, 8 }, { 40, 42 }, { 8, 8 }, { 8, 42 }, { 25, 43 },
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

    private enum LayerKind {
        PLENUM,
        FUEL,
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
                    row[xx] = (kind == LayerKind.PLENUM || kind == LayerKind.BAND) ? 'A' : 'F';
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
     * six fuel layers, lower plenums with a band - ribbed the whole way up. No floor layer:
     * the silo stands on the platform's shield pad (bottom-up index 0 is the pad itself).
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
            if (i >= 3 && i <= 8) {
                kind = LayerKind.FUEL;
            } else {
                kind = isBandLayer(tier, i) ? LayerKind.BAND : LayerKind.PLENUM;
            }
            layers.add(buildLayer(tier, kind, true, i == 1));
        }
        return layers.toArray(new String[0][]);
    }

    @Override
    public IStructureDefinition<MTEReactorCoreMk1> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            final StructureDefinition.Builder<MTEReactorCoreMk1> builder = StructureDefinition.builder();
            builder.addShape(floorPieceName(mName), transpose(floorShape()));
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
                .addElement(
                    'F',
                    ofChain(onElementPass(x -> ++x.fuelSegments, ofBlock(BTSBlocks.reactorFuelColumn, 0)), isAir()))
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
        // The starter structure: the full 50x50 platform must stand (docks register modules).
        if (!checkPiece(floorPieceName(mName), CENTER, 0, SILO_FRONT, errors)) return;
        // The Steam Generator silo is the first addon ON the platform - optional, largest
        // first. Failed walks register hatches as side effects, so clear between probes (the
        // platform contributed only modules, which silo pieces never touch).
        for (int t = MAX_TIER; t >= 1; t--) {
            clearHatches();
            mCasing = 0;
            fuelSegments = 0;
            if (checkPiece(pieceName(mName, t), siloHOff(t), siloVOff(t), siloDOff(t), null)) {
                reactorTier = t;
                break;
            }
        }
        // Hatch demands only apply once the silo exists; the bare platform forms and idles.
        if (reactorTier > 0) {
            checkCasingMin(errors, mCasing, minCasingsFor(reactorTier));
            checkHasEnergyHatch(errors);
            checkHasMaintenanceHatch(errors);
            checkHasInputHatch(errors);
            checkHasOutputHatch(errors);
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
        }
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
        return survivalBuildPiece(
            pieceName(mName, t),
            stackSize,
            siloHOff(t),
            siloVOff(t),
            siloDOff(t),
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
        int columns = Math.min(fuelSegments / SEGMENTS_PER_COLUMN, COLUMN_CAP[Math.max(1, reactorTier)]);
        if (columns <= 0) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        // GUI throttle setting, overridable by a programmed circuit in the controller slot.
        int n = Math.min(24, Math.max(1, throttleSetting));
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(24, circuit.getItemDamage());
        }

        // Fuel ladder (best first): naq-bonded x9, thorium-bonded x5, MOX x2, uranium x1.
        final Item[] assemblies = { BTSItems.fuelAssemblyNaqBonded, BTSItems.fuelAssemblyThoriumBonded,
            BTSItems.fuelAssemblyMox, BTSItems.fuelAssemblyUranium };
        final Item[] depletedForms = { BTSItems.depletedFuelAssemblyNaqBonded,
            BTSItems.depletedFuelAssemblyThoriumBonded, BTSItems.depletedFuelAssemblyMox,
            BTSItems.depletedFuelAssemblyUranium };
        final int[] fuelMults = { 9, 5, 2, 1 };
        ItemStack chosen = null;
        ItemStack depleted = null;
        int fuelMult = 1;
        for (int i = 0; i < assemblies.length; i++) {
            final ItemStack candidate = new ItemStack(assemblies[i], 1);
            if (countMatchingInputs(candidate) >= columns) {
                chosen = candidate;
                depleted = new ItemStack(depletedForms[i], columns);
                fuelMult = fuelMults[i];
                break;
            }
        }
        if (chosen == null) {
            clearCampaign();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        for (int i = 0; i < columns; i++) {
            depleteInput(chosen);
        }
        updateSlots();

        activeColumns = columns;
        throttleN = n;
        activeFuelMult = fuelMult;
        steamPerTick = STEAM_PER_COLUMN * columns * fuelMult * n / 24;
        this.mOutputItems = new ItemStack[] { depleted };
        this.mMaxProgresstime = Math.max(1, Config.genICampaignMinutes) * 60 * 20;
        this.mEfficiency = 10_000 - (getIdealStatus() - getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10_000;
        this.lEUt = 0;
        return CheckRecipeResultRegistry.SUCCESSFUL;
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
                final long eu = hall.processReactorSteam(steamPerTick, reactorTier >= MAX_TIER, baseFlow);
                if (eu < 0) {
                    // Hall has no fan installed: export the steam instead.
                    addOutput(Materials.Steam.getGas(steamPerTick));
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

    /** Fluid-export mode: no docked Turbine Hall. */
    private void runFluidTick(int flowPerTick, int baseFlow) {
        final int coolantFlow = reactorTier >= MAX_TIER ? baseFlow * 2 : baseFlow;
        if (depleteInput(GTModHandler.getIC2Coolant(coolantFlow))) {
            addOutput(GTModHandler.getHotCoolant(coolantFlow));
            lastEUOutput = 0;
            emitWasteWater();
        } else if (depleteInput(Materials.Water.getFluid(flowPerTick))
            || depleteInput(GTModHandler.getDistilledWater(flowPerTick))) {
                addOutput(Materials.Steam.getGas(steamPerTick));
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

        final int towers = coolingTowerCount();

        // Machine-block updates cannot propagate across the unregistered concrete platform to
        // the embedded controller, so force periodic revalidation: every 200t while a campaign
        // runs (a torn-open silo must not keep producing), every 600t while idle (modules
        // placed or removed on the dock markers are otherwise never noticed, and a hand-built
        // floor would never form). Idle probes are cheap - they abort on the first mismatch.
        if (aTick % 200 == 0 && (mMaxProgresstime > 0 || aTick % 600 == 0)) {
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
        return Math.min(fuelSegments / SEGMENTS_PER_COLUMN, COLUMN_CAP[getReactorTier()]);
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
        info.add("Fuel columns: " + getFuelColumns() + " / " + COLUMN_CAP[tier]);
        info.add("Fuel type: " + fuelName(activeFuelMult));
        info.add("Control rods: " + (throttleN * 100 / 24) + "% power");
        info.add("Modules: " + getModulesSummary());
        info.add(
            isTurbineInstalled() ? "Generating: " + lastEUOutput + " EU/t (via Turbine Hall)"
                : "Steam output: " + steamPerTick + " L/t");
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
        tt.addMachineType("Nuclear Plant Foundation - Unit One main station")
            .addInfo("STARTER STRUCTURE: the full 50x50 concrete platform. Every plant building is")
            .addInfo("added onto it - nothing else is mandatory. THIS controller is the main station,")
            .addInfo("standing on the front line of the Tungstensteel-Reinforced shield pad at platform")
            .addInfo("center; 8 flush steel markers set into the floor are the module dock positions.")
            .addInfo("Dock positions stay CLEAR until a module controller stands on them.")
            .addInfo("FIRST ADDON - the STEAM GENERATOR silo, projected with THIS controller onto the")
            .addInfo("shield pad; its front wall grows around the main station (9 to 17 wide octagon,")
            .addInfo("ribbed walls twice the diameter tall, radiation bands, hemispherical shield dome).")
            .addInfo("Build Tier: set 1-5 in the controller GUI (a trigger stack of 2-5 overrides it).")
            .addInfo("Walls by tier: Radiation Proof -> Europium-Reinforced -> Radiant Naquadah Alloy;")
            .addInfo("hatches go in the silo walls. Fuel columns stack 6 high inside.")
            .addInfo("SECOND - the TURBINE HALL module on the marker beside the silo (directly")
            .addInfo("connects; steam -> EU with fan/coolant/propellant). Then Cooling Towers")
            .addInfo("(condensate return + faster heat decay, up to 2) and further upgrades. Place a")
            .addInfo("PLANT MODULE controller on a marker and project its building there. Undocked = inert.")
            .addInfo("No Turbine Hall docked: the plant exports Steam (or Hot Coolant when fed IC2 Coolant).")
            .addInfo("Fuel ladder: Uranium x1, MOX x2, Thorium-Bonded x5, Naquadah-Bonded x9.")
            .addInfo("Control rods: set 1-24 in the controller GUI; a programmed circuit overrides it.")
            .addInfo("Heat: SCRAM at 8,000; decay heat 20 min after shutdown; 10,000 = MELTDOWN")
            .addInfo("(vertical detonation + radiation field; hazmat counters; config toggles).")
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
