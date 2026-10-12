package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofTileAdder;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Dynamo;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.LaserSource;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureWalker;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.nateyburr.beyondthestars.BTSBom;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BTSBlocks;
import com.nateyburr.beyondthestars.blocks.BlockReactorFuelColumn;
import com.nateyburr.beyondthestars.blocks.BlockTurbineHousing;
import com.nateyburr.beyondthestars.blocks.TileRodLoadingPort;
import com.nateyburr.beyondthestars.blocks.TileTurbineHousing;
import com.nateyburr.beyondthestars.fluids.BTSFluids;
import com.nateyburr.beyondthestars.fluids.BTSWaterGrade;
import com.nateyburr.beyondthestars.items.BTSItems;
import com.nateyburr.beyondthestars.items.BTSItemsFuel;
import com.nateyburr.beyondthestars.progression.BTSChannel;
import com.nateyburr.beyondthestars.progression.BTSMilestone;
import com.nateyburr.beyondthestars.progression.BTSProgression;
import com.nateyburr.beyondthestars.progression.BTSUpgrade;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.casing.ICasing;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IItemContainer;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.xmod.thermalfoundation.fluid.TFFluids;
import tectech.thing.CustomItemList;

/**
 * The Plant Foundation - the Unit One MAIN STATION. ONE controller now owns the whole plant:
 * there are no module controllers and no dock markers left anywhere.
 * <p>
 * MANDATORY starter structure = the full 50x50 concrete platform (shield pad at centre, the
 * controller on its front line, embedded in the silo face once the silo stands).
 * <p>
 * POINT OF USE (D-20): every insert and extract point sits on the BUILDING IT SERVES, at the
 * height it enters the logic flow - the rod bus and the depleted bus in the silo's face at the
 * bottom fuel course, feedwater and the waste-water/steam output one course below it on the
 * drum line, the bulk propellant and the chamber coolant in the Spiral's own front wall, the
 * energy hatches on the rotor manifold beside the dynamos they share a face with, and
 * maintenance beside the main station. There is no plumbing frontage and no single wall of
 * sockets. Distribution is still INTERNAL past those points: one rod insert fans to all
 * 45-201 pillars, one propellant line stocks all eight injectors.
 * <p>
 * Everything else is an OPTIONAL CHECKED PIECE of this same controller and appears in the one
 * hologram projection: the Steam Generator silo (five diameters, Build Tier in the GUI or by
 * trigger stack), the Spine duct, the Spiral chamber, the Turbine Generator + output manifold,
 * and the two Cooling Towers (absorbed from the retired Cooling Tower module - each standing
 * tower halves the feedwater draw, doubles passive heat decay and halves the decay-heat
 * demand, max 2). Fuel ladder, throttle, waste water and the SCRAM/decay-heat/meltdown model
 * are unchanged. Numbers per docs/reactor-gen1.md, docs/plant-geometry.md and
 * docs/steam-dynamics.md.
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
    /**
     * Relative b offsets {@link #standingSiloTier()} samples the shell on (+b is DOWN, so these
     * are the controller's own course and 1 and 11 above it). They are bottom-up courses 1, 2
     * and 12, which {@code coreShape} makes PLENUM, BAND and PLENUM at every one of the five
     * tiers - never FUEL (3..10) and never PORTS (11), whose interiors can legitimately hold
     * blocks. That is what makes "not air" inside a bigger silo impossible for these cells.
     */
    private static final int[] PROBE_COURSES = { 0, -1, -11 };
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

    // ------------------------------------------------ point-of-use fluid lines (D-20)
    /**
     * The SILO's Feedwater Input Hatch(es), cut into its front face on the drum course. The
     * reactor's water is drawn from THESE ONLY, never from the shared pool: the chamber's
     * coolant hatch and the bulk propellant line land in the same {@code mInputHatches}, and a
     * blanket depleteInput() would happily drink the chamber's water-grade coolant.
     */
    private final List<MTEHatchInput> feedwaterHatches = new ArrayList<>();
    /**
     * The SPIRAL CHAMBER's Propellant Input Hatch(es) - the ONE bulk line the player plumbs,
     * in the wall of the chamber whose eight injectors it feeds. The controller tops those
     * eight reserves up from here every tick, so nobody hand-times eight 1L inserts any more
     * (the CYCLE is still a redstone edge at the Turbine Control Block, and how many injectors
     * count as TIMED is still decided per cycle - by how many of them this line managed to
     * stock).
     */
    private final List<MTEHatchInput> propellantHatches = new ArrayList<>();

    // --------------------------------------------------- absorbed Cooling Towers (A3)
    /** The two tower shells, now optional CHECKED pieces of this controller (max 2). */
    private boolean towerFrontBuilt;
    private boolean towerBackBuilt;

    // ------------------------------------------------- D-17 PLANT MODULES (D-22, A3 pattern)
    /**
     * The four remaining D-17 buildings, built and checked EXACTLY like the Cooling Towers:
     * optional pieces of this one controller, raised by the same single projection, detected on
     * every structure walk, no dock and no second controller anywhere. The retired
     * space-elevator dock-pad architecture is not resurrected for any of them.
     */
    private boolean ventStackBuilt;
    private boolean switchyardBuilt;
    private boolean derrickBuilt;
    private boolean annexBuilt;

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
    /** Display only; starts "idle", as a stopped plant resets it, so a fresh plant never shows a blank coolant name. */
    private String chamberCoolantName = "idle";
    private long lastChamberTick;
    /**
     * The chamber's COOLANT feed. Semantics unchanged from SD-3 - a dedicated list, drained
     * only by the chamber, so the reactor's feedwater can never answer "can the chamber be
     * cooled?" - and D-20 puts it back where it belongs: the Coolant Input Hatch is in the
     * SPIRAL CHAMBER's own front wall, one course above its floor, feeding the thing that
     * overheats.
     */
    private final List<MTEHatchInput> chamberHatches = new ArrayList<>();
    /** Whether the chamber got the coolant its current speed asks for, on the last tick. */
    private boolean chamberCooled;
    private boolean chamberStarved;
    /** Localized name of the propellant standing in the injectors ("none" when dry). */
    private String propellantName = "none";
    /** P (x1000) of the propellant currently loaded - kept in step with propellantName. */
    private int propellantMilli;
    /** Last world tick any injector held propellant - the "the plant is dry" detector. */
    private long lastPropellantTick;

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
    /** RPM kept from tick to tick, in permille: the fan bleeds 0.5% a tick toward zero. */
    private static final int RPM_DECAY_PERMILLE = 995;
    /** Steam slugs a cycle drinks (ticks worth of L/t) and the buffer cap in slugs x 5. */
    private static final int CYCLE_SLUG_TICKS = 20;
    private static final int STEAM_BUFFER_CAP_TICKS = 100;
    /** TC-4 wear divisor: chance = (RPM / redline) / this, rolled once a second per rotor. */
    private static final int ROTOR_WEAR_DIVISOR = 600;
    /**
     * F3: the controller's explosion resistance. It carries the whole D-23/D-24 campaign record
     * and an exploded block drops nothing two times in three, so this one block is blast-proof.
     * See {@link #getExplosionResistance(ForgeDirection)} for the traced call path and for why
     * 2,000 is "any blast" rather than a number that merely survives a creeper.
     */
    private static final float CONTROLLER_BLAST_RESISTANCE = 2_000.0F;
    /**
     * The highest figure the RPM LIMIT field will accept. Fixed at GUI-build time, like the
     * throttle field's 36, so it is the ABSOLUTE ceiling of every unlock together:
     * {@code 12,000 x 5/4} (TURBINE T6) {@code x 1.30} (the tree's Overspeed Certification +0.10
     * and Synchronous Condenser +0.20) = 19,500. A plant that has unlocked none of it may still
     * type 19,500 and {@link #setRpmLimitSetting} clamps it straight back to 12,000 - the safe
     * direction, exactly as for the throttle.
     */
    public static final int RPM_LIMIT_MAX = 19_500;
    /** Fan RPM x1000 (the central variable; persisted). */
    private int rpmMilli;
    /**
     * F4 - THE OVERSPEED THE OPERATOR HAS AUTHORISED, RPM. Persisted; 12,000 on every plant that
     * has never touched it, which is the design redline and therefore a bit-identical plant.
     * <p>
     * TURBINE 5/6 used to raise the redline (and the per-cycle gain with it) the moment the tier
     * landed, with no opt-in at all. Steady-state chamber coolant draw scales with RPM SQUARED,
     * so a plant plumbed for exactly 12,000 saw its draw rise 26% at T5 and 56% at T6 and the
     * TC-7 interlock then throttled a plant that had run fine the day before - which is the one
     * thing an update in this project may not do. Every other unlock in the system is either
     * opt-in (BURNUP raises a CAP the player types into the throttle field) or a pure reduction
     * (UPTIME's condensate). This one is opt-in now too: the milestone raises the CEILING, and
     * this field is where the player says how much of it to use.
     */
    private int rpmLimitSetting = RPM_REDLINE;
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
    /** Per-stage rotor wear from the census, e.g. "0 0 1 - 0 0 0 2" ('-' = empty seat). */
    private String rotorWear = "";
    /**
     * TC-7 INTERLOCK: why the turbine is refusing to spin up (missing coolant headroom,
     * empty injectors, no steam slug), or "" while it is happy. Surfaced in the GUI and both
     * control blocks - a plant that will not accelerate must always say why.
     */
    private String turbineHoldReason = "";
    /** Pillars of the formed silo, rebuilt after every successful structure check. */
    private final java.util.HashMap<Long, ColumnInfo> columns = new java.util.HashMap<>();
    private final List<int[]> portCells = new ArrayList<>();
    /** Rod contents per pillar, bottom-first and gap-free; persisted in NBT. */
    private final java.util.HashMap<Long, byte[]> columnRods = new java.util.HashMap<>();

    private int mCasing;
    private int reactorTier;
    /** Why a Cooling Tower did not form, when one is partly standing ("" = nothing to say). */
    private String towerFrontFault = "";
    private String towerBackFault = "";
    /** Same question for the four D-17 modules, same answer rule: "" unless blocks are up. */
    private String ventStackFault = "";
    private String switchyardFault = "";
    private String derrickFault = "";
    private String annexFault = "";
    /**
     * The two PROCESS modules' dedicated sockets. Deliberately NOT in any plant-wide list -
     * see {@link ModuleHatchElement} for the two ways a shared list breaks this.
     */
    MTEHatchOutputBus derrickBus;
    MTEHatchInputBus annexFeedBus;
    MTEHatchOutputBus annexProductBus;
    private final List<MTEHatchInput> annexAcidHatches = new ArrayList<>();
    /** RUN state of the two process modules (persisted; a walk must never zero it). */
    private int derrickProgress;
    private int derrickCycles;
    private int annexProgress;
    private int annexCycles;
    /** Why the Derrick or the Annex is not turning over, in the interlock's voice ("" = fine). */
    private String moduleStall = "";
    /**
     * D-23 PROGRESSION: this plant's campaign record - four counters and everything derived
     * from them. PER CONTROLLER by ruling, exactly like the Godforge's: it is written by
     * {@code saveNBTData} AND by {@code setItemNBT} and read by {@code loadNBTData}, so it
     * survives a world reload, a chunk unload, a WRENCH, and the structure un-forming.
     * <p>
     * {@link #resetPieceState()} must never touch it - a structure check fires on every
     * neighbour update, and zeroing a campaign several times a second is the one bug this
     * whole system cannot survive. It is not RUN state and not WALK state; it is the save.
     */
    private final BTSProgression progression = new BTSProgression();
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
    /**
     * R7: the PRE-THROTTLE steam the running campaign is actually built on - the per-pillar
     * SUM {@code sum(16,384 x ROD_MULT[bottom rod])}, not {@code live x best grade x 16,384}.
     * The two only agree while every burning pillar holds the same grade, so the math panel
     * prints THIS and never re-derives step 1 from {@link #activeFuelMult} (which is the
     * highest grade in the silo and would overstate a mixed load).
     */
    private long fuelBaseSteam;
    /**
     * R7: the burning grades, best first ("32x9 + 13x1"). Empty while a single grade is
     * burning - that is the case where "live x fuelMult" is the whole truth and the panel can
     * print the simple product.
     */
    private String fuelGradeMix = "";
    private int heat;
    private int decayHeatTicks;
    private int radiationTicks;
    /** Depth of the radiation field's center behind the controller, frozen at meltdown time. */
    private int radiationCenterDepth;
    private long steamPerTick;
    private long lastEUOutput;
    /**
     * What the RPM model ASKED for on the last generating tick, before the output ceiling
     * was applied. The gap between this and lastEUOutput is the whole "why am I only seeing
     * 32,768 EU/t" story, so both are persisted to the GUI and both control blocks.
     */
    private long lastEUDemand;
    /**
     * D-26: the feedwater rung the last draw actually took. -1 = plain water or nothing, 0 =
     * distilled, 1..8 = the purified grades.
     * <p>
     * AXIS 1: written by {@link #drawFeedwater} and, to park it back at the baseline over a draw
     * that is not happening, by {@link #runFluidTick}'s Hot Coolant branch and
     * {@link #clearCampaign} - all three server-side, so the GUI reads it through a sync value
     * and never the field (which is -1 on the client). B7: the post-shutdown decay-heat draw
     * deliberately does NOT write it - that draw earns no output term, and a shut-down plant
     * must not print a grade as though it were still generating on it. AXIS 2: deliberately
     * NOT persisted - it is re-derived every generating tick from what is standing in the hatch,
     * which is the only way it can be right after a player swaps the water out.
     */
    private int feedwaterGrade = BTSWaterGrade.NOT_GRADED;
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

    // ------------------------------------------------- point-of-use fluid hatch adders

    /**
     * The plant's three discriminated fluid sockets all register as ordinary input hatches,
     * but the plant has to be able to tell them apart: the silo's feedwater, the Spiral
     * chamber's coolant and the Spiral chamber's bulk propellant line. Each socket char
     * therefore has its own adder, exactly like the control-block sockets - and each keeps its
     * own list, so no draw can ever reach across into another line's fluid.
     *
     * <p>
     * GT 2.9.0-RC-2: {@code addInputHatchToMachineList} now also accepts fluid-capable dual input
     * hatches (crafting input buffers / slaves) into {@code mDualInputHatches} and answers true.
     * The plant never reads that list - each socket drains only its own {@link MTEHatchInput}
     * list - so a CRIB in a socket would form the structure and feed nothing. The sockets
     * therefore refuse anything that is not a plain {@link MTEHatchInput} BEFORE super sees it,
     * and the walk fails visibly instead of starving silently.
     */
    private static MTEHatchInput socketInput(IGregTechTileEntity aTileEntity) {
        if (aTileEntity == null) return null;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        return mte instanceof MTEHatchInput ? (MTEHatchInput) mte : null;
    }

    public boolean addFeedwaterHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        final MTEHatchInput hatch = socketInput(aTileEntity);
        if (hatch == null || !addInputHatchToMachineList(aTileEntity, aBaseCasingIndex)) return false;
        feedwaterHatches.add(hatch);
        return true;
    }

    public boolean addChamberCoolantHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        final MTEHatchInput hatch = socketInput(aTileEntity);
        if (hatch == null || !addInputHatchToMachineList(aTileEntity, aBaseCasingIndex)) return false;
        chamberHatches.add(hatch);
        return true;
    }

    public boolean addPropellantHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        final MTEHatchInput hatch = socketInput(aTileEntity);
        if (hatch == null || !addInputHatchToMachineList(aTileEntity, aBaseCasingIndex)) return false;
        propellantHatches.add(hatch);
        return true;
    }

    public int getFeedwaterHatchCount() {
        return feedwaterHatches.size();
    }

    /** World cells {x, y, z} of the silo's feedwater hatches (console test bench: the CRIB socket check). */
    public List<int[]> feedwaterHatchCells() {
        final List<int[]> cells = new ArrayList<>();
        for (final MTEHatchInput hatch : GTUtility.validMTEList(feedwaterHatches)) {
            final IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            cells.add(new int[] { base.getXCoord(), base.getYCoord(), base.getZCoord() });
        }
        return cells;
    }

    public int getChamberCoolantHatchCount() {
        return chamberHatches.size();
    }

    public int getPropellantHatchCount() {
        return propellantHatches.size();
    }

    // -------------------------------------------- D-17 module sockets (dedicated, never shared)
    /**
     * The Extraction Derrick's product bus, the Enrichment Annex's feed bus, acid hatch and
     * product bus. Every one of these adders deliberately does NOT call its {@code super}
     * counterpart, so none of the four ever reaches {@code mOutputBusses} / {@code mInputBusses}
     * / {@code mInputHatches}. Two separate failures that buys:
     * <ul>
     * <li>a module socket in a plant-wide list satisfies a demand made of a DIFFERENT piece -
     * an Annex acid hatch would answer {@code checkHasInputHatch} on the SILO's behalf and let a
     * plant form with no feedwater socket at all (the Cooling Tower ring hatch, again);</li>
     * <li>and a shared list is a shared INVENTORY - the reactor's rod distribution would eat the
     * Annex's uraninite, and the cascade's depleted rods would land in the Derrick's bus.</li>
     * </ul>
     */
    public boolean addDerrickBusToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchOutputBus && derrickBus == null) {
            ((MTEHatchOutputBus) mte).updateTexture(aBaseCasingIndex);
            derrickBus = (MTEHatchOutputBus) mte;
            return true;
        }
        return false;
    }

    public boolean addAnnexFeedBusToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchInputBus && annexFeedBus == null) {
            ((MTEHatchInputBus) mte).updateTexture(aBaseCasingIndex);
            annexFeedBus = (MTEHatchInputBus) mte;
            return true;
        }
        return false;
    }

    public boolean addAnnexProductBusToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchOutputBus && annexProductBus == null) {
            ((MTEHatchOutputBus) mte).updateTexture(aBaseCasingIndex);
            annexProductBus = (MTEHatchOutputBus) mte;
            return true;
        }
        return false;
    }

    public boolean addAnnexAcidHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        final IMetaTileEntity mte = aTileEntity.getMetaTileEntity();
        if (mte instanceof MTEHatchInput && annexAcidHatches.isEmpty()) {
            ((MTEHatchInput) mte).updateTexture(aBaseCasingIndex);
            annexAcidHatches.add((MTEHatchInput) mte);
            return true;
        }
        return false;
    }

    // C4: every one of these four answers about a LIVE socket. A broken hatch keeps a non-null
    // reference (and a non-null base, with isDead() true) until the next structure check clears
    // it, so a null test would have the panel report a socket that is lying on the floor.
    public int getDerrickBusCount() {
        return live(derrickBus) == null ? 0 : 1;
    }

    public int getAnnexFeedBusCount() {
        return live(annexFeedBus) == null ? 0 : 1;
    }

    public int getAnnexProductBusCount() {
        return live(annexProductBus) == null ? 0 : 1;
    }

    public int getAnnexAcidHatchCount() {
        return hasLiveAcidHatch() ? annexAcidHatches.size() : 0;
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

    /**
     * A3: the Cooling Towers are pieces of THIS structure now, not docked modules. A tower
     * counts the moment its shell passes the walk - there is nothing to connect and nothing to
     * form separately. Two is the whole platform's worth, which is also the ruled cap.
     */
    private int coolingTowerCount() {
        return (towerFrontBuilt ? 1 : 0) + (towerBackBuilt ? 1 : 0);
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
     * The eight cells {a, c} that 0.18.0/0.19.0 stamped into the platform floor as flush
     * SOLID STEEL dock markers, one under each module controller. A3 retired the docks with
     * the module controllers, so a NEW platform lays plain Light Concrete here - but every
     * platform built before that still holds Solid Steel in all eight, and all eight lie
     * outside the max-tier octagon, so they are ordinary floor cells in the new shape.
     * <p>
     * R1-B: the floor piece is checkMachine's FIRST walk and it returns on failure, BEFORE
     * the tier probe. Demanding Light Concrete here would therefore have failed every legacy
     * platform's floor, left {@code reactorTier} at 0 on a plant with a standing tier-5 silo,
     * and handed the projector back to the GUI Build Tier - which is exactly the tier-1-inside
     * -a-tier-5 destruction R1 was written to stop. The 'd' element accepts EITHER block.
     */
    private static final int[][] LEGACY_DOCK_CELLS = { { 38, 30 }, { 8, 1 }, { 8, 48 }, { 40, 8 }, { 40, 42 },
        { 25, 43 }, { 10, 25 }, { 25, 7 } };

    /**
     * Full 50x50 platform, two layers: light concrete flooring with a Tungstensteel-Reinforced
     * shield pad (sized for the max-tier silo) at center, then the walking level - open except
     * the controller on the pad's front line.
     * <p>
     * A3 retired the eight flush steel DOCK MARKERS with the module controllers they existed
     * for: every building is a piece of this controller now, so those cells are plain floor -
     * 'd', which takes the new Light Concrete OR the legacy Solid Steel that is already there,
     * and PLACES the Light Concrete when it builds. Nobody has to re-block their platform.
     * The walking level above them is unchecked (' '), so a retired module controller still
     * standing on one of them is simply ignored rather than demanded or destroyed.
     */
    private static String[][] floorShape() {
        final String[] padLayer = new String[PLATFORM];
        for (int c = 0; c < PLATFORM; c++) {
            final char[] row = new char[PLATFORM];
            Arrays.fill(row, ' ');
            if (c == SILO_FRONT) row[CENTER] = '~';
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
            for (final int[] dock : LEGACY_DOCK_CELLS) {
                if (dock[1] == c) row[dock[0]] = 'd';
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
     * faces, the Spine Sensor in the roof's front-center, the Maintenance socket in the bottom
     * layer's front-right corner, and (D-20) the chamber's own two supply sockets - COOLANT
     * and bulk PROPELLANT - in the front wall one course above the floor.
     * [y top-first][7 c-rows][7 a-chars].
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
                    // Bottom: Maintenance socket in the front-right corner, unchanged.
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
            // back(+c), -a, front(-c), +a - at that wall's mid-cell. AXIS LETTERS, never
            // compass words: +a is the CONTROLLER's relative left (SocketFace.LEFT), which
            // is the PLAYER's right as they stand outside facing the silo, and which compass
            // direction either of those is depends entirely on how the plant was placed. The
            // rotation STARTS on the back face so the two back-face sockets land at levels
            // 1 and 5 (world y 10 and 6), clear above the gallery's 5-tall front wall;
            // starting on the front face buried the level-7 socket behind it.
            // Each wall gets its OWN socket char so the auto-fitter knows which way that
            // injector's exposed face points: the chamber has air on both sides of every
            // wall cell, so an outward direction can only be declared, never observed.
            if (level >= 1 && level <= 8) {
                final int face = (level - 1) % 4;
                if (face == 0) grid[6][3] = 'i'; // back wall (+c)
                else if (face == 1) grid[3][0] = 'v'; // -a wall (SocketFace.RIGHT)
                else if (face == 2) grid[0][3] = 'w'; // front wall (-c)
                else grid[3][6] = 'x'; // +a wall (SocketFace.LEFT)
            }
            // D-20 POINT OF USE: the chamber's own two supply sockets, in its FRONT wall
            // (-c) at interior level 10 = world y 1 - one course above the floor, at the
            // feet of a player standing on the open platform, and directly under the eight
            // injectors the propellant line stocks. The front wall looks out over bare
            // platform at EVERY tier (chamber front row is c = 14 + edge/2, the cell in
            // front of it c = 13 + edge/2 = 17..21, and nothing else is ever built in the
            // a 43..49 strip forward of the chamber), so neither socket can be entombed.
            // Their outward normal is DECLARED FORWARD, like the 'w' injector and the 'l'
            // maintenance socket in the same wall - never inferred from air neighbours.
            if (level == SPIRAL_HEIGHT - 2) {
                grid[0][1] = 'k'; // Coolant Input Hatch - the thing that overheats
                grid[0][5] = 'P'; // Propellant Input Hatch - bulk line to the 8 injectors
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
     * gallery bore, the EIGHTH stage socket hangs center-bore in the mouth (row 15), and the
     * back face carries the whole electrical side of the plant in three courses - 4 Dynamo
     * sockets ('m') at y3, 4 Energy Hatch sockets ('r') at y4 and 4 Laser Source sockets
     * ('L') at y5, all in columns a 1,2,4,5. Total length is still 18 rows, so the world
     * span is unchanged from SD-4. [y top-first][18 c-rows][7 a-chars].
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
        // TC-5 LASER OUTPUT: 4 Laser Source sockets two rows ABOVE the dynamos, same back
        // face, same four columns. Nothing is ever built behind the manifold at any tier, so
        // every one of them looks straight out at open sky - laser pipes run away from the
        // plant without threading the gallery. The a=3 column is left as casing so it stays
        // a clean spine over the Turbine Control Block; a=0/a=6 are the manifold's frame.
        if (y == 5 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {
            return 'L';
        }
        // D-20 POINT OF USE: the four ENERGY HATCHES belong AT THE GENERATOR, so they sit on
        // the same back face one course between the dynamo row (y3) and the laser row (y5),
        // in the same four columns. Same open-sky face, same declared BACK normal - nothing is
        // ever built behind the manifold at any tier, so all twelve sockets on this wall look
        // straight out and a cable or a laser pipe reaches every one of them.
        if (y == 4 && c == GALLERY_LENGTH - 1 && (a == 1 || a == 2 || a == 4 || a == 5)) {
            return 'r';
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

    /** Gallery starts one row past the chamber's BACK face, on the same RIGHT-strip columns. */
    private static int galleryDOff(int tier) {
        return -4 - edgeFor(tier) / 2;
    }

    /** Duct starts two blocks past the tier's RIGHT-hand wall: wall, rib, then duct on it. */
    private static int ductHOff(int tier) {
        return -(edgeFor(tier) / 2 + 2);
    }

    /** Duct rows centered on the tier's silo mid-row (world c = 17 + edge/2). */
    private static int ductDOff(int tier) {
        return 1 - edgeFor(tier) / 2;
    }

    // ------------------------- the absorbed COOLING TOWERS (A3, optional checked pieces)

    /**
     * Tower placement, unchanged from the retired module's docks so an existing platform's
     * towers keep standing where they always did. Both sit on the platform strip to your
     * LEFT as you face the silo (a 0-16): the FRONT tower out in front of the silo face
     * (c 0-16), the BACK tower behind the shell (c 33-49), both 20 layers tall from pad
     * level (y 0..19). Neither touches the silo (a 17-33 at max tier) or the RIGHT strip
     * (a 35-49) that the Spine, Spiral and gallery run down.
     * <p>
     * D-21: the docks' NW/SW labels went with the docks. {@code a} is the controller's
     * RELATIVE LEFT, so a compass word here is only ever true for one facing - and these
     * two were not true for any of them ("NW" is north-EAST on a north-facing plant).
     * <p>
     * R2 NOTE: {@link MTEModuleCoolingTower#towerShape()} still emits TWO controller-anchor
     * cells per tower in its bottom layer - {@code base[0][8]} and {@code base[frontZ = 1][8]},
     * platform (a 8, c 0) and (a 8, c 1) for the FRONT tower, (a 8, c 49) and (a 8, c 48)
     * for the BACK one. The remap below turns BOTH into ordinary Solid Steel base ring
     * ('R'), a hard-checked,
     * block-placing element, so the one projection and /btsbuild raise them like any other
     * ring cell. On a platform whose towers were raised by the RETIRED module controller
     * those two cells hold that controller and open air instead, and must be replaced with
     * Solid Steel Machine Casing by hand - see the release notes.
     */
    private static final int TOWER_FRONT_DOFF = SILO_FRONT;
    private static final int TOWER_BACK_DOFF = -16;

    private static String towerPieceName(String base, String which) {
        return base + ".tower." + which;
    }

    /**
     * Re-letters the Cooling Tower's own shape onto THIS structure definition's elements. The
     * module controller's anchor cells ('~') become ordinary base-ring casing - there is no
     * module controller any more, so the ring simply closes - and reversing the rows flips the
     * shell for the tower that stands at the platform's far end.
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
                    // R2 GUARD: the borrowed shape marks its retired controller with '~'. If a
                    // future edit ever drops that char from `from`, StructureLib would silently
                    // treat the cell as unregistered and the tower could never form. Fail loudly
                    // at class-init instead of shipping four un-buildable cells.
                    if (chars[k] == '~') {
                        throw new IllegalStateException(
                            "remapShape left a controller anchor '~' unmapped at layer " + i + " row " + j);
                    }
                }
                rows[j] = new String(chars);
            }
            out[i] = rows;
        }
        return out;
    }

    // ================= D-17 PLANT MODULES: Vent Stack, Switchyard, Derrick, Annex ==========
    //
    // ARCHITECTURE (D-20 A3, and NOT the "Implementation notes" section of docs/plant-unit-one.md,
    // which still described the DELETED space-elevator dock-pad design). Every one of these four
    // is an OPTIONAL CHECKED PIECE of this controller, exactly like the two absorbed Cooling
    // Towers: one hologram projection raises them, checkMachine walks them, a partly-standing one
    // that will not form prints WHY, and there is no module controller and no dock marker
    // anywhere in the design.
    //
    // SITING. Each site is a FIXED rectangle of the 50x50 platform that no tier of any other
    // piece can reach, so a module never has to move when the silo is re-tiered:
    //
    // piece platform a platform c y proven clear of
    // Vent Stack 2..8 21..27 0..19 both towers (c 0..16 / 33..49), every silo
    // Switchyard 20..40 44..49 0..5 silo (c <= 34), spine (c <= 26), gallery (a >= 43)
    // Derrick 33..39 4..10 0..19 silo/spine/spiral (all c >= 16 or a >= 43)
    // Annex 17..21 0..14 0..5 front tower (a <= 16), silo (c >= 16)
    //
    // Verified cell by cell at all five tiers against platform, silo, spine, spiral, gallery,
    // manifold and both towers: zero overlaps, zero cells off the platform, zero of the eight
    // LEGACY DOCK CELLS covered (so a retired module controller still standing on one can never
    // be in a cell one of these pieces wants), and all four declared socket faces open.
    //
    // None of them touches the two invariants the shipped sockets rest on: nothing is built in
    // the a 43..49 strip in front of the Spiral chamber, and nothing is built BEHIND the output
    // manifold at any tier.

    /** Offsets are derived from the site rectangle: hOff = 25 - a0, dOff = 17 - c0, vOff = top. */
    private static final int VENT_A0 = 2, VENT_C0 = 21, VENT_SIZE = 7, VENT_HEIGHT = 20;
    private static final int YARD_A0 = 20, YARD_C0 = 44, YARD_WIDTH = 21, YARD_DEPTH = 6, YARD_HEIGHT = 6;
    private static final int DERRICK_A0 = 33, DERRICK_C0 = 4, DERRICK_SIZE = 7, DERRICK_HEIGHT = 20;
    private static final int ANNEX_A0 = 17, ANNEX_C0 = 0, ANNEX_WIDTH = 5, ANNEX_LENGTH = 15, ANNEX_HEIGHT = 6;

    private static String ventPieceName(String base) {
        return base + ".module.vent";
    }

    private static String yardPieceName(String base) {
        return base + ".module.switchyard";
    }

    private static String derrickPieceName(String base) {
        return base + ".module.derrick";
    }

    private static String annexPieceName(String base) {
        return base + ".module.annex";
    }

    /** Structure-space offsets of a module site, as buildPiece/checkPiece take them. */
    private static int moduleHOff(int a0) {
        return CENTER - a0;
    }

    private static int moduleDOff(int c0) {
        return SILO_FRONT - c0;
    }

    private static String[] blankRows(int rows, int width) {
        return solidRows(rows, width, ' ');
    }

    private static String[] solidRows(int rows, int width, char ch) {
        final char[] line = new char[width];
        Arrays.fill(line, ch);
        final String full = new String(line);
        final String[] out = new String[rows];
        for (int i = 0; i < rows; i++) out[i] = full;
        return out;
    }

    private static void setCell(String[] rows, int row, int col, char ch) {
        final char[] chars = rows[row].toCharArray();
        chars[col] = ch;
        rows[row] = new String(chars);
    }

    /**
     * The VENT STACK (D-17 5): a 7x7x20 scrubber house with a 3x3 flue running the whole way up.
     * Safety systems, so it is deliberately the plainest of the four - no sockets at all, exactly
     * like a Cooling Tower. It stands or it does not.
     * <p>
     * Effect: the post-shutdown DECAY-HEAT window and the meltdown RADIATION field both HALVE
     * (20 min -&gt; 10 min of cooling water owed, 30 min -&gt; 15 min of field). Both are applied
     * as a CLAMP on the remaining ticks and not only at arming time, so a stack finished halfway
     * through a decay cycle shortens the cycle it is standing over.
     * <p>
     * [layer b, 0 = top][row c][char a].
     */
    private static String[][] ventShape() {
        final String[][] layers = new String[VENT_HEIGHT][];
        for (int b = 0; b < VENT_HEIGHT; b++) {
            final int y = VENT_HEIGHT - 1 - b;
            final String[] rows;
            if (y == 0) {
                rows = solidRows(VENT_SIZE, VENT_SIZE, 'V');
            } else if (y == 4) {
                // Roof of the scrubber house, opened for the flue it carries.
                rows = solidRows(VENT_SIZE, VENT_SIZE, 'V');
                for (int c = 2; c <= 4; c++) {
                    for (int a = 2; a <= 4; a++) setCell(rows, c, a, ' ');
                }
                ventFlue(rows);
            } else if (y <= 3) {
                rows = blankRows(VENT_SIZE, VENT_SIZE);
                for (int c = 0; c < VENT_SIZE; c++) {
                    for (int a = 0; a < VENT_SIZE; a++) {
                        if (c == 0 || c == VENT_SIZE - 1 || a == 0 || a == VENT_SIZE - 1) setCell(rows, c, a, 'V');
                    }
                }
                ventFlue(rows);
            } else {
                rows = blankRows(VENT_SIZE, VENT_SIZE);
                ventFlue(rows);
            }
            layers[b] = rows;
        }
        return layers;
    }

    /** The flue: a 3x3 ring of Filter Machine Casing around an open bore. */
    private static void ventFlue(String[] rows) {
        for (int c = 2; c <= 4; c++) {
            for (int a = 2; a <= 4; a++) {
                if (c != 3 || a != 3) setCell(rows, c, a, 'W');
            }
        }
    }

    /**
     * The SWITCHYARD (D-17 6): a 21x6x6 transformer yard on the platform's back edge, behind the
     * silo. Five 3x3x3 transformer banks in a fenced compound with five pylons carrying the line
     * out over the fence. No sockets - the plant's power still leaves through the manifold's own
     * dynamo and laser rows; the yard is the GRID CONDITIONING that lets those rows run above
     * {@link #GRID_SOFT_CAP}.
     * <p>
     * [layer b, 0 = top][row c][char a].
     */
    private static String[][] switchyardShape() {
        final int[] banks = { 2, 6, 10, 14, 18 };
        final int[] pylons = { 0, 5, 10, 15, 20 };
        final String[][] layers = new String[YARD_HEIGHT][];
        for (int b = 0; b < YARD_HEIGHT; b++) {
            final int y = YARD_HEIGHT - 1 - b;
            final String[] rows;
            if (y == 0) {
                rows = solidRows(YARD_DEPTH, YARD_WIDTH, 'Z');
            } else {
                rows = blankRows(YARD_DEPTH, YARD_WIDTH);
                if (y == 1) {
                    // The compound fence, one course up, all the way round.
                    for (int c = 0; c < YARD_DEPTH; c++) {
                        for (int a = 0; a < YARD_WIDTH; a++) {
                            if (c == 0 || c == YARD_DEPTH - 1 || a == 0 || a == YARD_WIDTH - 1) {
                                setCell(rows, c, a, 'M');
                            }
                        }
                    }
                } else {
                    for (final int a : pylons) {
                        setCell(rows, 0, a, 'M');
                        setCell(rows, YARD_DEPTH - 1, a, 'M');
                    }
                }
                if (y <= 3) {
                    for (final int centre : banks) {
                        for (int c = 2; c <= 4; c++) {
                            for (int a = centre - 1; a <= centre + 1; a++) setCell(rows, c, a, 'Z');
                        }
                    }
                }
            }
            layers[b] = rows;
        }
        return layers;
    }

    /**
     * The EXTRACTION DERRICK (D-17 3): a 7x7 machine deck under a 20-block lattice tower with a
     * drill string down its middle - the standalone {@link MTEExtractionPlatform}'s silhouette,
     * scaled to the plant. ONE socket, the product bus, in the deck's FRONT wall at y1: chest
     * height for a player standing on the open platform, looking out over bare concrete at every
     * tier (nothing is ever built in c 0..3).
     * <p>
     * [layer b, 0 = top][row c][char a].
     */
    private static String[][] derrickShape() {
        final String[][] layers = new String[DERRICK_HEIGHT][];
        for (int b = 0; b < DERRICK_HEIGHT; b++) {
            final int y = DERRICK_HEIGHT - 1 - b;
            final String[] rows;
            if (y == 0) {
                rows = solidRows(DERRICK_SIZE, DERRICK_SIZE, 'Z');
            } else if (y == 1) {
                rows = solidRows(DERRICK_SIZE, DERRICK_SIZE, 'Z');
                setCell(rows, 3, 3, 'X');
                setCell(rows, 0, 3, 'b'); // product bus, FRONT wall, declared FORWARD
            } else if (y == 2) {
                rows = solidRows(DERRICK_SIZE, DERRICK_SIZE, 'Z');
                setCell(rows, 3, 3, 'X');
            } else if (y == DERRICK_HEIGHT - 1) {
                // The crown is the 5x5 ring, NOT a 3x3 one: the four lattice columns stand at
                // grid (1|5, 1|5), so a 3x3 cap would have hung in the air over nothing.
                rows = blankRows(DERRICK_SIZE, DERRICK_SIZE);
                for (int c = 1; c <= 5; c++) {
                    for (int a = 1; a <= 5; a++) {
                        if (c == 1 || c == 5 || a == 1 || a == 5) setCell(rows, c, a, 'M');
                    }
                }
                setCell(rows, 3, 3, 'X');
            } else {
                rows = blankRows(DERRICK_SIZE, DERRICK_SIZE);
                for (final int c : new int[] { 1, 5 }) {
                    for (final int a : new int[] { 1, 5 }) setCell(rows, c, a, 'M');
                }
                setCell(rows, 3, 3, 'X');
            }
            layers[b] = rows;
        }
        return layers;
    }

    /**
     * The ENRICHMENT ANNEX (D-17 4): a 5x15x6 process shed running down the platform's front
     * edge, one conversion line of Steel Pipe Casing along its spine - the standalone
     * {@link MTEEnrichmentTrain} stretched to plant scale. THREE sockets, all in its +a wall at
     * y1 (declared LEFT), looking out over open platform at every tier: uraninite feed bus,
     * hydrofluoric acid hatch, and the yellowcake/thorium product bus (M7: it paid finished fuel
     * pellets until 0.24.0 - anything reading this bus receives Yellowcake now).
     * <p>
     * [layer b, 0 = top][row c][char a].
     */
    private static String[][] annexShape() {
        final String[][] layers = new String[ANNEX_HEIGHT][];
        for (int b = 0; b < ANNEX_HEIGHT; b++) {
            final int y = ANNEX_HEIGHT - 1 - b;
            final String[] rows;
            if (y == 0 || y == ANNEX_HEIGHT - 1) {
                rows = solidRows(ANNEX_LENGTH, ANNEX_WIDTH, 'V');
            } else {
                rows = blankRows(ANNEX_LENGTH, ANNEX_WIDTH);
                for (int c = 0; c < ANNEX_LENGTH; c++) {
                    for (int a = 0; a < ANNEX_WIDTH; a++) {
                        if (c == 0 || c == ANNEX_LENGTH - 1 || a == 0 || a == ANNEX_WIDTH - 1) {
                            setCell(rows, c, a, 'V');
                        }
                    }
                }
                if (y == 2) {
                    for (int c = 1; c <= ANNEX_LENGTH - 2; c++) setCell(rows, c, 2, 'X');
                }
                if (y == 1) {
                    setCell(rows, 3, ANNEX_WIDTH - 1, 'e'); // uraninite feed bus
                    setCell(rows, 7, ANNEX_WIDTH - 1, 'f'); // hydrofluoric acid hatch
                    setCell(rows, 11, ANNEX_WIDTH - 1, 'g'); // pellet + thorium product bus
                }
            }
            layers[b] = rows;
        }
        return layers;
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

    /**
     * The casing behind a silo shape char. ONE source of truth: the structure definition builds
     * its 'C'/'D'/'E'/'G' elements from this and {@link #migrateSiloFace()} re-lays the face's
     * wall cells from it, so the check and the migration can never drift into demanding
     * different blocks of the same cell.
     */
    private static ICasing casingFor(char ch) {
        switch (ch) {
            case 'D':
                return Casings.EuropiumReinforcedRadiationProofMachineCasing;
            case 'E':
                return Casings.RadiantNaquadahAlloyCasing;
            default:
                // 'C' - the tier 1-2 wall - and 'G', the radiation bands, at every tier.
                return Casings.RadiationProofMachineCasing;
        }
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
        // ------------------------------------------------------------ D-20 SILO FACE
        // The silo's front face carries the sockets that belong TO THE SILO, each on the
        // course where it enters the reactor's logic flow. Bottom-up layer i = list index
        // domeH + wallH - 1 - i, and the front row is octagon row 0 = grid row 1.
        //
        // y2 [ ] u [ ] Q [ ] bottom FUEL course: Rod Input Bus / Depleted Output Bus,
        // the exact segment the cascade pushes into and burns out of
        // y1 [ ] y p Y [ ] drum course: Feedwater IN / Silo Control Block / waste
        // water + exported steam OUT
        // y0 [ ] t ~ [ ] [ ] pad level: MAIN STATION, Maintenance beside it
        //
        // Offsets are +/-1 from the octagon's centre column, which is inside the front row at
        // EVERY tier (tier 1's front row is the narrowest at 5 cells, centre +/-2). All six
        // sockets declare the SAME outward normal, FORWARD - the way the controller looks -
        // and every one of them faces 17 blocks of bare platform at every tier.
        final int mid = edgeFor(tier) / 2 + 1;
        final String[] padCourse = layers.get(domeH + wallH - 2); // i = 1, world y 0
        setFrontCell(padCourse, mid - 1, 't'); // Maintenance, beside the main station
        final String[] drumCourse = layers.get(domeH + wallH - 3); // i = 2, world y 1
        setFrontCell(drumCourse, mid - 1, 'y'); // Feedwater Input Hatch
        setFrontCell(drumCourse, mid, 'p'); // TC-2: the Silo Control Block socket
        setFrontCell(drumCourse, mid + 1, 'Y'); // Output Hatch: waste water / exported steam
        final String[] fuelCourse = layers.get(domeH + wallH - 4); // i = 3, world y 2
        setFrontCell(fuelCourse, mid - 1, 'u'); // Rod Input Bus
        setFrontCell(fuelCourse, mid + 1, 'Q'); // Depleted Output Bus
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
                // C1 audit: a raw setBlock replaces a standing tile entity in place and drops
                // nothing. A pillar cell is silo interior, which is exactly where a player
                // parks a bus or an ME part while loading rods.
                if (standsOnTile(world, x, y, z)) return true;
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
                // C1 audit: same raw-setBlock gap. A Rod Loading Port carries its own tile
                // entity, so this also stops a re-projection blanking a LOADED port.
                if (standsOnTile(world, x, y, z)) return true;
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
                // C1 audit: a Turbine Housing carries TileTurbineHousing, and that tile is where
                // the ROTOR lives (TC-4). Housing meta encodes spin direction, so laying meta 0
                // over a standing meta-1 housing was a real block replacement - it voided the
                // rotor in it. Never write into an occupied cell.
                if (standsOnTile(world, x, y, z)) return true;
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
     * The outward normal of the WALL a socket sits in, named RELATIVE to the controller and
     * resolved through the plant's ExtendedFacing. It is declared per socket char because it
     * cannot be observed: every socket in these shells has air on BOTH faces - the hollow
     * structure interior on one side, open sky on the other - so the old "first air
     * neighbour in world order {N,S,E,W,U,D}" scan returned the same world direction for the
     * silo's FRONT wall and the manifold's BACK wall, which face opposite ways. One of the
     * two was therefore turned inward on every rotation, and an inward-facing hatch renders
     * as the casing it was textured with (MTEHatch.getTexture paints the overlay only on
     * side == frontFacing), which is why the control blocks read as "not placed".
     * <p>
     * Structure space is the frame the shape strings are written in: the row index runs +c
     * (relative BACK), the char index inside a row runs +a (relative LEFT), and the layer
     * index runs +b (relative DOWN).
     */
    private enum SocketFace {

        /** Out of the silo's front wall, the way the controller looks. */
        FORWARD,
        /** Out of the manifold's back wall, directly away from the controller. */
        BACK,
        /** Toward growing shape-row char index (+a). */
        LEFT,
        /** Toward shrinking shape-row char index (-a). */
        RIGHT,
        UP,
        DOWN;

        ForgeDirection inWorld(ExtendedFacing facing) {
            switch (this) {
                case FORWARD:
                    return facing.getRelativeForwardInWorld();
                case BACK:
                    return facing.getRelativeBackInWorld();
                case LEFT:
                    return facing.getRelativeLeftInWorld();
                case RIGHT:
                    return facing.getRelativeRightInWorld();
                case UP:
                    return facing.getRelativeUpInWorld();
                default:
                    return facing.getRelativeDownInWorld();
            }
        }
    }

    /** Cells auto-fitted during the last creative build: {x, y, z, SocketFace ordinal}. */
    private final List<int[]> fittedCells = new ArrayList<>();

    /**
     * Re-points every auto-fitted hatch at its declared exposed face. The facing is already
     * correct at placement time (the normal is a property of the wall, not of the neighbours),
     * so this is a cheap idempotent repair pass - it also straightens out plants built by an
     * older version whose facings were guessed.
     */
    public int orientFittedHatches() {
        final World world = getBaseMetaTileEntity().getWorld();
        final ExtendedFacing facing = getExtendedFacing();
        int oriented = 0;
        for (final int[] c : fittedCells) {
            if (!world.blockExists(c[0], c[1], c[2])) continue;
            final net.minecraft.tileentity.TileEntity te = world.getTileEntity(c[0], c[1], c[2]);
            if (!(te instanceof IGregTechTileEntity)) continue;
            ((IGregTechTileEntity) te).setFrontFacing(SocketFace.values()[c[3]].inWorld(facing));
            oriented++;
        }
        return oriented;
    }

    /**
     * Wraps a socket element so a CREATIVE build (the /btsbuild command or a hologram
     * projector) drops the real hatch in rather than a blank casing - the structure comes
     * out working, pointed OUT of the wall it is socketed into. An existing GT machine in the
     * cell is never overwritten, so re-running a build cannot wipe hatches the player has
     * already configured. Survival is untouched: players still supply and place their own
     * hatches.
     * <p>
     * C1: and NO OTHER TILE ENTITY is overwritten either. Refusing only for
     * {@code IGregTechTileEntity} was the whole hole: both remaining paths WRITE - the
     * {@link #placeGtMachine} path calls {@code setBlockToAir} on whatever stands in the cell
     * and the chained fallback is a casing, which is {@code ofBlock} - so an AE2 ME Interface,
     * a chest, a hopper or an EnderIO conduit parked on a socket cell was silently voided by
     * one right-click of a projector. Every socket in the plant goes through this wrapper (the
     * silo face, the chamber, the manifold, the injectors AND the four D-17 module sockets
     * 'b','e','f','g'), so this is the socket half of the rule {@link #protectedElement}
     * states for material cells: if a tile entity is in the cell, refuse to write and let the
     * structure check fail loudly naming it.
     */
    private static IStructureElement<MTEReactorCoreMk1> autoFit(IStructureElement<MTEReactorCoreMk1> base,
        java.util.function.Supplier<Short> mteId, SocketFace face) {
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
                    t.fittedCells.add(new int[] { x, y, z, face.ordinal() });
                    return true;
                }
                // C1: any OTHER tile entity is somebody's machine, and everything past this
                // point writes. Leave it exactly as it stands.
                if (standsOnTile(world, x, y, z)) return true;
                final Short id = mteId.get();
                if (id != null && placeGtMachine(world, x, y, z, id, face.inWorld(t.getExtendedFacing()))) {
                    t.fittedCells.add(new int[] { x, y, z, face.ordinal() });
                    return true;
                }
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /**
     * MTE id behind a GT item-container entry (its stack damage), or 0 when the entry is
     * absent. Typed on IItemContainer so TecTech's CustomItemList works alongside ItemList.
     */
    private static short idOf(IItemContainer entry) {
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

    /**
     * One socket in the SILO's front face (D-20). All six of them - Maintenance, Feedwater,
     * the Silo Control Block, the Output Hatch, the Rod Input Bus and the Depleted Output Bus
     * - are cut into the same wall as the controller, so every one declares the same outward
     * normal, FORWARD. A blank socket falls back to Radiation Proof casing at EVERY tier (the
     * wall material changes with the tier, the socket filler deliberately does not) and counts
     * toward the silo's casing minimum exactly like the wall cell it replaced.
     */
    private static IStructureElement<MTEReactorCoreMk1> siloSocket(IHatchElement<? super MTEReactorCoreMk1> element,
        java.util.function.Supplier<Short> mteId) {
        return autoFit(
            buildHatchAdder(MTEReactorCoreMk1.class).atLeast(element)
                .casingIndex(Casings.RadiationProofMachineCasing.textureId)
                .hint(1)
                .buildAndChain(onElementPass(x -> ++x.mCasing, Casings.RadiationProofMachineCasing.asElement())),
            mteId,
            SocketFace.FORWARD);
    }

    /**
     * One supply socket in the SPIRAL CHAMBER's front wall (D-20): the bulk propellant line
     * that stocks the eight injectors above it, and the coolant that carries the chamber's own
     * heat away. Declared FORWARD, like the 'w' injector and the 'l' maintenance socket in the
     * same wall; blanks chain to the chamber's own Tungstensteel Turbine Casing.
     */
    private static IStructureElement<MTEReactorCoreMk1> chamberSocket(IHatchElement<? super MTEReactorCoreMk1> element,
        java.util.function.Supplier<Short> mteId) {
        return autoFit(
            buildHatchAdder(MTEReactorCoreMk1.class).atLeast(element)
                .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                .hint(3)
                .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
            mteId,
            SocketFace.FORWARD);
    }

    /**
     * One socket in the OUTPUT MANIFOLD's back face (D-20): the four Energy Hatches, at the
     * generator. Declared BACK, like the dynamos one course below and the laser sources one
     * course above; nothing is ever built behind the manifold at any tier.
     */
    private static IStructureElement<MTEReactorCoreMk1> manifoldSocket(IHatchElement<? super MTEReactorCoreMk1> element,
        java.util.function.Supplier<Short> mteId) {
        return autoFit(
            buildHatchAdder(MTEReactorCoreMk1.class).atLeast(element)
                .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                .hint(4)
                .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
            mteId,
            SocketFace.BACK);
    }

    /**
     * The absorbed Cooling Towers' base ring (M4/M5): Solid Steel Machine Casing, OR the
     * Maintenance Hatch that 0.19.0's tower controller demanded in that ring and that a legacy
     * tower still has standing in it.
     * <p>
     * It is a plain TOLERANCE, not an adoption, and both halves of that matter:
     * <ul>
     * <li>the casing element comes FIRST in the chain, so {@code spawnHint} - which returns at
     * the first fallback that answers true - still draws all 52 ring cells per tower as Solid
     * Steel ghosts. A hatch adder answers true unconditionally from its own hint, which is what
     * turned 104 cells into blank markers;</li>
     * <li>the hatch is matched by its TILE and never registered, so it cannot land in
     * {@code mMaintenanceHatches}. A ring hatch in that plant-wide list satisfied
     * {@code checkHasMaintenanceHatch} on the SILO's behalf - making the silo's own 't' socket
     * optional against the tooltip - and, because the towers are walked before it, left a bare
     * platform's {@code mMaintenanceHatches.isEmpty()} self-repair unreachable.</li>
     * </ul>
     */
    private static IStructureElement<MTEReactorCoreMk1> towerRingElement() {
        final IStructureElement<MTEReactorCoreMk1> casing = Casings.SolidSteelMachineCasing.asElement();
        final IStructureElement<MTEReactorCoreMk1> legacyHatch = ofTileAdder(
            (MTEReactorCoreMk1 t, net.minecraft.tileentity.TileEntity te) -> te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEHatchMaintenance,
            Casings.SolidSteelMachineCasing.getBlock(),
            Casings.SolidSteelMachineCasing.getBlockMeta());
        final IStructureElement<MTEReactorCoreMk1> chain = ofChain(casing, legacyHatch);
        // A projection must never destroy a machine it did not place. ofChain.placeBlock
        // delegates to the FIRST fallback - the casing - so without this guard, projecting
        // onto a migrated legacy plant would overwrite the 0.19.0 Maintenance Hatch this
        // element exists to tolerate, and void it. Same class as the legacy-face refusal:
        // /btsmigrate only rewrites the silo face, so after migrating, the tower ring is
        // the one place a standing machine still sits in a cell the projector will write.
        return new IStructureElement<MTEReactorCoreMk1>() {

            @Override
            public boolean check(MTEReactorCoreMk1 t, World world, int x, int y, int z) {
                return chain.check(t, world, x, y, z);
            }

            @Override
            public boolean spawnHint(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                return chain.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z, ItemStack trigger) {
                // Already acceptable - a casing, or the legacy hatch we deliberately allow.
                // Leave it exactly as it stands rather than paving over it.
                if (chain.check(t, world, x, y, z)) return true;
                // C1 audit: and the guard above only covered the legacy MAINTENANCE hatch. Any
                // OTHER tile entity in a ring cell - a chest, an ME part, a 0.19.0 tower's own
                // retired controller - failed the chain and was paved over by the casing
                // fallback, which is ofBlock. Same refusal, for every tile entity.
                if (standsOnTile(world, x, y, z)) return true;
                return chain.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                if (chain.check(t, world, x, y, z)) return PlaceResult.SKIP;
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return chain.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /**
     * Is a TILE ENTITY standing in this cell? THE question every write path in this file has to
     * ask before it writes, because a tile entity is the one thing in a cell that is somebody's
     * MACHINE - inventory, configuration, an ME network membership - and every write path here
     * (a raw {@code world.setBlock}, {@code ofBlock}, {@code isAir}, {@link #placeGtMachine})
     * replaces it in place and drops NOTHING. C1: this is deliberately not narrowed to
     * {@code IGregTechTileEntity}; an AE2 ME Interface, a chest, a hopper and an EnderIO
     * conduit are all exactly as expensive to lose as a GT hatch.
     */
    private static boolean standsOnTile(World world, int x, int y, int z) {
        return world.getTileEntity(x, y, z) != null;
    }

    /**
     * The minimal form of the rule: delegate everything, but never WRITE into a cell that holds
     * a tile entity. {@code check} and {@code spawnHint} pass straight through, so the hologram
     * still draws the real block for the cell and the structure check still fails loudly and
     * names it - only the write is refused.
     * <p>
     * C1 audit: this wraps every remaining plain-material element in the definition. All of them
     * are {@code ofBlock} / {@code isAir} / a raw {@code world.setBlock}, and every one of those
     * paves over a standing tile entity and drops nothing. {@link #protectedElement} is the same
     * rule plus an explicit "already acceptable - leave it exactly as it stands" clause; it is
     * kept separate because its {@code base.check} call is only safe on elements with no
     * side effects, and the silo wall chars count casings from inside {@code check}.
     */
    private static IStructureElement<MTEReactorCoreMk1> noOverwrite(IStructureElement<MTEReactorCoreMk1> base) {
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
                if (standsOnTile(world, x, y, z)) return true;
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /**
     * A plain block element that will NEVER OVERWRITE A STANDING TILE ENTITY.
     * <p>
     * Every MATERIAL cell of the four D-17 modules goes through this, and every SOCKET cell of
     * them goes through {@link #autoFit}, which refuses on the same terms (C1 - it used to
     * refuse only for {@code IGregTechTileEntity}, which is what let a projection void an ME
     * Interface standing on the Derrick's bus cell or one of the Annex's three socket cells).
     * Between the two, no cell of any module can overwrite a tile entity. The sites are virgin
     * platform on a plant this version raised - but not on the plant a player has been running
     * for weeks, who
     * may well have a chest, a cable, an AE2 interface or a retired module controller parked on
     * the open concrete where the Switchyard is about to land. A raw {@code ofBlock} element
     * writes its casing over whatever is in the cell and drops nothing, so ONE right-click of a
     * projector would void it.
     * <p>
     * The rule is the tower ring's rule generalised: if the cell already satisfies the element,
     * leave it exactly as it stands; if a TILE ENTITY is in it, refuse to write and let the
     * structure check fail loudly (the module's fault line then names the cell and its
     * coordinates). Only genuinely empty or genuinely wrong plain blocks are ever written.
     * <p>
     * {@code spawnHint} delegates straight through, so the hologram still draws the real casing
     * for all of these cells - which is exactly what wrapping a casing in a HATCH ADDER would
     * have destroyed, since a hatch adder's spawnHint answers true unconditionally.
     */
    private static IStructureElement<MTEReactorCoreMk1> protectedElement(IStructureElement<MTEReactorCoreMk1> base) {
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
                if (base.check(t, world, x, y, z)) return true;
                if (standsOnTile(world, x, y, z)) return true;
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment env) {
                if (base.check(t, world, x, y, z)) return PlaceResult.SKIP;
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /**
     * One socket of a D-17 process module. Same shape as {@link #siloSocket} and
     * {@link #chamberSocket}: the hatch adder chained to the module's own casing so a blank
     * socket is just wall, wrapped in {@link #autoFit} so a creative build drops the real hatch
     * in pointing OUT of the wall it is cut into. The normal is DECLARED per socket char and
     * never inferred from air neighbours.
     */
    private static IStructureElement<MTEReactorCoreMk1> moduleSocket(IHatchElement<? super MTEReactorCoreMk1> element,
        java.util.function.Supplier<Short> mteId, Casings filler, int hint, SocketFace face) {
        return autoFit(
            buildHatchAdder(MTEReactorCoreMk1.class).atLeast(element)
                .casingIndex(filler.textureId)
                .hint(hint)
                .buildAndChain(filler.asElement()),
            mteId,
            face);
    }

    /** One Precision Injector socket; the chamber wall it is cut into decides its facing. */
    private static IStructureElement<MTEReactorCoreMk1> injectorSocket(SocketFace face) {
        return autoFit(
            buildHatchAdder(MTEReactorCoreMk1.class).atLeast(SpiralHatchElement.Injector)
                .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                .hint(3)
                .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
            () -> idOf(BTSMachines.precisionInjector),
            face);
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
            // A3: the Cooling Towers are CHECKED pieces of this controller now - their own
            // letters remapped onto this definition's elements, the retired module
            // controller's anchor cells closed back up into base ring.
            builder.addShape(
                towerPieceName(mName, "front"),
                transpose(remapShape(MTEModuleCoolingTower.towerShape(), "NS~", "ORR", false)));
            builder.addShape(
                towerPieceName(mName, "back"),
                transpose(remapShape(MTEModuleCoolingTower.towerShape(), "NS~", "ORR", true)));
            // D-17 / D-22: the four remaining plant modules, pieces of this controller in the
            // same sense the towers are - one projection, one structure walk, no dock.
            builder.addShape(ventPieceName(mName), transpose(ventShape()));
            builder.addShape(yardPieceName(mName), transpose(switchyardShape()));
            builder.addShape(derrickPieceName(mName), transpose(derrickShape()));
            builder.addShape(annexPieceName(mName), transpose(annexShape()));
            for (int t = 1; t <= MAX_TIER; t++) {
                builder.addShape(pieceName(mName, t), transpose(coreShape(t)));
            }
            STRUCTURE_DEFINITION = builder.addElement('B', noOverwrite(ofBlock(GregTechAPI.sBlockReinforced, 3)))
                .addElement('I', noOverwrite(ofBlock(GregTechAPI.sBlockReinforced, 1)))
                .addElement('A', noOverwrite(isAir()))
                // TC-1: silo walls are PLAIN casings - every hatch lives in a dedicated
                // socket cell on the flat front face (chars 't','y','p','Y','u','Q' below).
                .addElement('C', noOverwrite(onElementPass(x -> ++x.mCasing, casingFor('C').asElement())))
                .addElement('D', noOverwrite(onElementPass(x -> ++x.mCasing, casingFor('D').asElement())))
                .addElement('E', noOverwrite(onElementPass(x -> ++x.mCasing, casingFor('E').asElement())))
                // A3: the absorbed Cooling Towers' base ring (their shell is plain 'O'
                // Light Concrete, shared with the platform and the Spiral's helix arcs).
                // R2: this is also what the retired module controller's two anchor cells per
                // tower remap to, so the projection raises a CLOSED base ring.
                // R1-C: 0.19.0's Cooling Tower controller REQUIRED a Maintenance Hatch in this
                // ring, so every legacy tower has one sitting in it. The ring TOLERATES it
                // without registering it - see towerRingElement() for why adopting it broke
                // both the ring's hologram and the plant's maintenance demand.
                .addElement('R', towerRingElement())
                // ------------------------------------- D-17 PLANT MODULE materials (D-22)
                // All five go through protectedElement, so a projection onto a lived-in
                // platform can never void a machine standing in one of these cells.
                // 'V' Vent Stack shell + Enrichment Annex shell.
                .addElement('V', protectedElement(Casings.CleanStainlessSteelMachineCasing.asElement()))
                // 'W' the Vent Stack's scrubber core and flue.
                .addElement('W', protectedElement(Casings.FilterMachineCasing.asElement()))
                // 'Z' Switchyard deck + transformer banks, and the Derrick's machine deck.
                .addElement('Z', protectedElement(Casings.SolidSteelMachineCasing.asElement()))
                // 'M' lattice: the Derrick's tower and the Switchyard's fence and pylons.
                .addElement('M', protectedElement(ofFrame(Materials.Steel)))
                // 'X' process piping: the Derrick's drill string, the Annex's conversion line.
                .addElement('X', protectedElement(Casings.SteelPipeCasing.asElement()))
                // The four module sockets. NOTHING here joins a plant-wide hatch list - see
                // ModuleHatchElement - so none of them can answer a demand made of the silo.
                .addElement(
                    'b',
                    moduleSocket(
                        ModuleHatchElement.DerrickBus,
                        () -> idOf(ItemList.Hatch_Output_Bus_IV),
                        Casings.SolidSteelMachineCasing,
                        2,
                        SocketFace.FORWARD))
                .addElement(
                    'e',
                    moduleSocket(
                        ModuleHatchElement.AnnexFeed,
                        () -> idOf(ItemList.Hatch_Input_Bus_IV),
                        Casings.CleanStainlessSteelMachineCasing,
                        2,
                        SocketFace.LEFT))
                .addElement(
                    'f',
                    moduleSocket(
                        ModuleHatchElement.AnnexAcid,
                        () -> idOf(ItemList.Hatch_Input_IV),
                        Casings.CleanStainlessSteelMachineCasing,
                        2,
                        SocketFace.LEFT))
                .addElement(
                    'g',
                    moduleSocket(
                        ModuleHatchElement.AnnexProduct,
                        () -> idOf(ItemList.Hatch_Output_Bus_IV),
                        Casings.CleanStainlessSteelMachineCasing,
                        2,
                        SocketFace.LEFT))
                // R1-B: the eight LEGACY DOCK MARKER cells in the platform floor. Light
                // Concrete is what a new platform lays and what the projection places; Solid
                // Steel is what every platform built before A3 already holds. Accepting both
                // is what keeps a pre-existing plant FORMED - and therefore keeps its tier
                // detectable - instead of demanding the player re-block eight floor cells
                // before the reactor will so much as admit it exists.
                .addElement(
                    'd',
                    noOverwrite(
                        ofChain(ofBlock(GregTechAPI.sBlockConcretes, 8), Casings.SolidSteelMachineCasing.asElement())))
                // ------------------------------------------- D-20 THE SILO'S OWN SOCKETS
                // The silo face carries what belongs to the SILO, on the course it enters the
                // logic flow: rods in/out at the bottom fuel course (y2), feedwater in and
                // waste water / exported steam out on the drum course (y1) either side of the
                // Silo Control Block, and Maintenance beside the main station at pad level.
                .addElement('t', siloSocket(Maintenance, () -> idOf(ItemList.Hatch_Maintenance)))
                .addElement('y', siloSocket(FluidHatchElement.Feedwater, () -> idOf(ItemList.Hatch_Input_IV)))
                .addElement('Y', siloSocket(OutputHatch, () -> idOf(ItemList.Hatch_Output_IV)))
                .addElement('u', siloSocket(InputBus, () -> idOf(ItemList.Hatch_Input_Bus_IV)))
                .addElement('Q', siloSocket(OutputBus, () -> idOf(ItemList.Hatch_Output_Bus_IV)))
                .addElement('p', siloSocket(PanelHatchElement.SiloPanel, () -> idOf(BTSMachines.siloControlBlock)))
                .addElement('O', noOverwrite(ofBlock(GregTechAPI.sBlockConcretes, 8)))
                .addElement('G', noOverwrite(onElementPass(x -> ++x.mCasing, casingFor('G').asElement())))
                .addElement('F', fuelCellElement())
                .addElement('J', portCellElement())
                .addElement('T', noOverwrite(Casings.TungstensteelTurbineCasing.asElement()))
                .addElement('H', noOverwrite(ofChain(housingElement(), Casings.TungstensteelTurbineCasing.asElement())))
                // TC-1: spiral and gallery walls are PLAIN casings; hatches live in the
                // dedicated socket chars below ('i','v','w','x','j','k','P','l' spiral /
                // 'm','r','L','n','q' gallery). One injector char per chamber wall so each
                // socket's outward normal is declared rather than guessed.
                .addElement('5', noOverwrite(Casings.TungstensteelTurbineCasing.asElement()))
                .addElement('i', injectorSocket(SocketFace.BACK))
                .addElement('v', injectorSocket(SocketFace.RIGHT))
                .addElement('w', injectorSocket(SocketFace.FORWARD))
                .addElement('x', injectorSocket(SocketFace.LEFT))
                // D-20: the chamber's own supply sockets, in its front wall at world y 1.
                .addElement('k', chamberSocket(FluidHatchElement.Coolant, () -> idOf(ItemList.Hatch_Input_IV)))
                .addElement('P', chamberSocket(FluidHatchElement.Propellant, () -> idOf(ItemList.Hatch_Input_IV)))
                .addElement(
                    'j',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(SpiralHatchElement.Sensor)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(BTSMachines.spineSensor),
                        SocketFace.UP))
                .addElement(
                    'l',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Maintenance)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(3)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Maintenance),
                        SocketFace.FORWARD))
                .addElement('8', noOverwrite(Casings.TungstensteelTurbineCasing.asElement()))
                .addElement(
                    'm',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Dynamo)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Dynamo_IV),
                        SocketFace.BACK))
                // D-20: the four ENERGY HATCHES, one course above the dynamos on the same
                // back face - at the generator, which is the only place in the plant they
                // have anything to do with.
                .addElement('r', manifoldSocket(Energy, () -> idOf(ItemList.Hatch_Energy_IV)))
                // TC-5: the LASER OUTPUT sockets. HatchElement.LaserSource accepts TecTech's
                // MTEHatchDynamoTunnel (which addLaserSourceToMachineList files into
                // mExoticDynamoHatches), so the whole plant's ceiling stops being the four
                // 1A IV dynamos. Auto-fits the ZPM 4,096 A/t Laser Source Hatch: two of them
                // already cover the T5 design peak, four leave the int-clamp as the only
                // limit. Blank sockets chain to plain turbine casing like every other socket.
                .addElement(
                    'L',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(LaserSource)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(5)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(CustomItemList.eM_dynamoTunnel3_ZPM),
                        SocketFace.BACK))
                .addElement(
                    'n',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(Maintenance)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(ItemList.Hatch_Maintenance),
                        SocketFace.BACK))
                .addElement(
                    'q',
                    autoFit(
                        buildHatchAdder(MTEReactorCoreMk1.class).atLeast(PanelHatchElement.TurbinePanel)
                            .casingIndex(Casings.TungstensteelTurbineCasing.textureId)
                            .hint(4)
                            .buildAndChain(Casings.TungstensteelTurbineCasing.asElement()),
                        () -> idOf(BTSMachines.turbineControlBlock),
                        SocketFace.BACK))
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
                        // C1 audit: the gallery's stage seats are the eight cells that HOLD
                        // ROTORS. Re-laying a blank housing over one voided the rotor in it.
                        if (standsOnTile(world, x, y, z)) return true;
                        world.setBlock(x, y, z, BTSBlocks.turbineHousing, 0, 2);
                        return true;
                    }

                    @Override
                    public PlaceResult survivalPlaceBlock(MTEReactorCoreMk1 t, World world, int x, int y, int z,
                        ItemStack trigger, AutoPlaceEnvironment env) {
                        return PlaceResult.SKIP;
                    }
                }, Casings.TungstensteelTurbineCasing.asElement()))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    /**
     * Does ANY block of the given piece still stand? Walks the piece's own cells with
     * StructureLib's iterator and stops at the first non-air one, so a piece that is merely
     * DAMAGED answers yes on its first surviving block while a piece that was DELIBERATELY
     * DISMANTLED (or never built) has to be scanned to the end before answering no.
     * <p>
     * Only {@link #towerFault} asks now, and only to tell "no tower here" (a choice, and not
     * worth reporting) from "a tower is half up and does not form" (a fault, and the one a
     * 0.19.0 world hits). An unloaded chunk answers YES - it proves nothing, and the safe
     * assumption is that the piece is still there.
     */
    private boolean anyBlockStanding(String piece, int hOff, int vOff, int dOff) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return true;
        final int cx = base.getXCoord();
        final int cy = base.getYCoord();
        final int cz = base.getZCoord();
        final boolean[] found = { false };
        getStructureDefinition().iterate(
            piece,
            base.getWorld(),
            getExtendedFacing(),
            cx,
            cy,
            cz,
            hOff,
            vOff,
            dOff,
            new IStructureWalker<MTEReactorCoreMk1>() {

                @Override
                public boolean visit(IStructureElement<MTEReactorCoreMk1> element, World world, int x, int y, int z,
                    int a, int b, int c) {
                    // The controller is not evidence that its own silo stands.
                    if (x == cx && y == cy && z == cz) return true;
                    if (world.isAirBlock(x, y, z)) return true;
                    found[0] = true;
                    return false;
                }

                @Override
                public boolean blockNotLoaded(IStructureElement<MTEReactorCoreMk1> element, World world, int x, int y,
                    int z, int a, int b, int c) {
                    found[0] = true;
                    return false;
                }
            });
        return found[0];
    }

    /**
     * The tier of the silo shell PHYSICALLY STANDING on the pad, or 0 - MEASURED off the world,
     * never remembered.
     * <p>
     * This is what replaced the persisted {@code lastBuiltTier}, and the replacement is the
     * whole point. A remembered number has to be written on the server, carried in tile NBT AND
     * item NBT, pushed to the client for the hologram, and released when the shell comes down;
     * it got every one of those wrong in turn. A measurement needs none of them: it is identical
     * on client and server because both read the same blocks, it survives a wrench, a reload and
     * a chunk round-trip because it is not stored anywhere, and it releases itself the instant
     * the shell is dismantled.
     * <p>
     * Three columns per tier, chosen because nothing else in the plant can occupy them: the two
     * side-wall cells on the octagon's centre row - relative side offset {@code edge/2}, the same
     * distance BACK, because every tier's front wall is anchored on the controller's line - and
     * the back wall's centre cell, {@code edge - 1} back. Each is sampled on three courses that
     * are plenum or band at EVERY tier (bottom-up 1, 2 and 12), so INSIDE a larger silo all nine
     * cells are open interior air. That is what lets the scan walk 5..1 and stop at the largest
     * tier with walls in place, and spreading the samples up the shell is what keeps a locally
     * broken-open wall answering.
     * <p>
     * A cell counts only when it holds THAT TIER'S OWN WALL MATERIAL - {@code wallChar} at the
     * controller's course, the radiation band's Radiation Proof casing at the one above, both
     * read through {@link #casingFor} so this cannot disagree with the check. "Any block" would
     * have been enough for a silo, but not enough for a bare pad: the walking level is unchecked
     * (' ') in the platform shape, so a player who paves or decorates it would have handed the
     * scan a tier-5 shell that is not there - and re-tiered their own build with it.
     * <p>
     * D-21 - WHY THE MATERIAL ALONE IS NOT ENOUGH, AND WHAT REPLACED "two hits of nine". At
     * tiers 1 and 2 {@code wallChar} is 'C' and {@code casingFor('C') == casingFor('G') ==}
     * Radiation Proof Machine Casing - the single most common casing in the whole plant, and the
     * one a player has spare stacks of at exactly this point in the build. Two of them dropped
     * anywhere on the pad satisfied "two hits of nine", so a BARE PAD read as a standing tier-1
     * or tier-2 silo and {@link #buildTargetTier} pinned the projector to it: Build Tier 5 in the
     * GUI, a tier-1 silo on the ground, and a tooltip explaining that a standing silo is never
     * re-tiered. The threshold is now TWO OF THE THREE COLUMNS (a column answers on any of its
     * courses), plus at least one hit ABOVE the walking level. Both are pure tightenings that
     * every genuinely standing shell passes - it has all three columns and all three courses -
     * while a paved or decorated pad has hits on ONE course only, the controller's own, and
     * scattered casings now have to land on two different exact cells to fake a shell.
     * <p>
     * Reach check: a tier-S Spine occupies side offsets {@code S+5..S+7} at back {@code S+2..S+4}
     * (side and back are never equal there), the Spiral and the gallery start at side 18, and both
     * Cooling Towers sit at side 9..25 on the opposite half of the platform.
     */
    public int standingSiloTier() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return 0;
        for (int tier = MAX_TIER; tier >= 1; tier--) {
            final int half = edgeFor(tier) / 2;
            final int[][] probes = { { half, half }, { -half, half }, { 0, edgeFor(tier) - 1 } };
            int columnsStanding = 0;
            boolean aboveWalkingLevel = false;
            for (final int[] probe : probes) {
                boolean columnStands = false;
                for (final int b : PROBE_COURSES) {
                    if (!probeCellIsWall(base, probe[0], b, probe[1], tier)) continue;
                    columnStands = true;
                    // b == 0 is the controller's own course - the platform's unchecked walking
                    // level, and the only course anything a player paves or drops can reach.
                    if (b != 0) aboveWalkingLevel = true;
                }
                if (columnStands) columnsStanding++;
            }
            if (columnsStanding >= 2 && aboveWalkingLevel) return tier;
        }
        return 0;
    }

    /** Does one probe cell hold the tier's own wall material? Unloaded chunks answer no. */
    private boolean probeCellIsWall(IGregTechTileEntity base, int a, int b, int c, int tier) {
        final World world = base.getWorld();
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        // Structure space: +a is relative LEFT, +b is relative DOWN, +c is BACK.
        abc[0] = a;
        abc[1] = b;
        abc[2] = c;
        getExtendedFacing().getWorldOffset(abc, xyz);
        final int x = base.getXCoord() + xyz[0];
        final int y = base.getYCoord() + xyz[1];
        final int z = base.getZCoord() + xyz[2];
        if (!world.blockExists(x, y, z)) return false;
        // The band course carries Radiation Proof at EVERY tier; the other two carry the
        // tier's own wall casing. Both read through casingFor, so this cannot drift from
        // the structure check.
        final ICasing wall = casingFor(b == -1 ? 'G' : wallChar(tier));
        return wall.matches(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    /** The MTE the silo face's socket char wants fitted, or null when the char is not a socket. */
    private static Short faceSocketId(char ch) {
        switch (ch) {
            case 't':
                return idOf(ItemList.Hatch_Maintenance);
            case 'y':
                return idOf(ItemList.Hatch_Input_IV);
            case 'Y':
                return idOf(ItemList.Hatch_Output_IV);
            case 'u':
                return idOf(ItemList.Hatch_Input_Bus_IV);
            case 'Q':
                return idOf(ItemList.Hatch_Output_Bus_IV);
            case 'p':
                return idOf(BTSMachines.siloControlBlock);
            default:
                return null;
        }
    }

    /**
     * Does the machine already in this cell satisfy the face socket? Matched on the same CLASS
     * the socket's hatch element accepts, not on the exact MTE id, so a player's LuV Input Hatch
     * in the right cell is left where it is instead of being swapped for the IV one.
     */
    private static boolean faceSocketSatisfied(char ch, World world, int x, int y, int z) {
        final net.minecraft.tileentity.TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return false;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        switch (ch) {
            case 't':
                return mte instanceof MTEHatchMaintenance;
            case 'y':
                // MTEHatchPrecisionInjector extends MTEHatchInput; it is never a feedwater hatch.
                return mte instanceof MTEHatchInput && !(mte instanceof MTEHatchPrecisionInjector);
            case 'Y':
                return mte instanceof MTEHatchOutput;
            case 'u':
                return mte instanceof MTEHatchInputBus;
            case 'Q':
                return mte instanceof MTEHatchOutputBus;
            case 'p':
                return mte instanceof MTESiloControlBlock;
            default:
                return false;
        }
    }

    /** Drops whatever stands in a cell as an item and clears it; true when it was a machine. */
    private static boolean salvage(World world, int x, int y, int z) {
        final Block standing = world.getBlock(x, y, z);
        if (standing == null || standing.isAir(world, x, y, z)) return false;
        final boolean machine = world.getTileEntity(x, y, z) instanceof IGregTechTileEntity;
        // dropBlockAsItem runs while the tile is still in place, which is what lets GT's
        // getDrops read the machine (and its NBT) off it; setBlockToAir then fires breakBlock,
        // which spills the inventory. Between them nothing the player owned is voided.
        standing.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return machine;
    }

    /**
     * D-20 MIGRATION (/btsmigrate). Rewrites the silo's FRONT FACE - and nothing else - to the
     * current layout, at the tier {@link #standingSiloTier()} reads off the standing walls.
     * <p>
     * 0.19.0 cut a five-wide plumbing frontage into this wall: two Input Hatches, a Maintenance
     * Hatch and an Output Hatch at pad level, four ENERGY hatches across the drum course, and
     * the two buses out at the +/-2 corners. D-20 moved every socket to the building it serves,
     * so the two layouts CONFLICT cell for cell - each puts a hatch where the other wants wall -
     * and no chained element can accept both. That is why a legacy plant fails all five silo
     * probes at every tier, and why it needs a rewrite rather than a more forgiving check.
     * <p>
     * Only the front row's bottom three courses are touched, because they are the only cells the
     * two layouts disagree about. What belongs in each is read straight out of {@link #coreShape}
     * and {@link #casingFor}, so this cannot drift from the check. A cell that is already right
     * is left completely alone; a wrong one has its contents DROPPED as items before the correct
     * casing or socket goes in. The shell, the ribs, the dome, the pillars and the rods are never
     * read and never written.
     *
     * @return {tier, cells rewritten, machines handed back}; tier 0 = no shell on the pad.
     */
    public int[] migrateSiloFace() {
        final int tier = standingSiloTier();
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (tier <= 0 || base == null || base.getWorld() == null) return new int[] { 0, 0, 0 };
        final World world = base.getWorld();
        final ExtendedFacing facing = getExtendedFacing();
        final ForgeDirection outward = SocketFace.FORWARD.inWorld(facing);
        final String[][] layers = coreShape(tier);
        final int mid = siloHOff(tier);
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        int rewritten = 0;
        int salvaged = 0;
        for (int course = 0; course <= 2; course++) {
            // Bottom-up course i = 1 + course is list index (size - 1 - course); the silo's
            // front row is octagon row 0, which is grid row 1.
            final String frontRow = layers[layers.length - 1 - course][1];
            for (int xx = 0; xx < frontRow.length(); xx++) {
                final char ch = frontRow.charAt(xx);
                // ' ' is outside the octagon, '~' is the main station itself.
                if (ch == ' ' || ch == '~') continue;
                abc[0] = xx - mid;
                abc[1] = -course;
                abc[2] = 0;
                facing.getWorldOffset(abc, xyz);
                final int x = base.getXCoord() + xyz[0];
                final int y = base.getYCoord() + xyz[1];
                final int z = base.getZCoord() + xyz[2];
                if (!world.blockExists(x, y, z)) continue;
                final Short socket = faceSocketId(ch);
                if (socket != null) {
                    if (faceSocketSatisfied(ch, world, x, y, z)) {
                        // Right machine, right cell: keep it, and only make sure it looks OUT of
                        // the wall - an inward hatch renders as the casing it was textured with.
                        ((IGregTechTileEntity) world.getTileEntity(x, y, z)).setFrontFacing(outward);
                        continue;
                    }
                    if (salvage(world, x, y, z)) salvaged++;
                    if (placeGtMachine(world, x, y, z, socket, outward)) rewritten++;
                    continue;
                }
                // Everything else in these three front rows is wall material - the tier's own
                // casing, or 'G' where the radiation band crosses the face (and, at tiers 1-2,
                // the two front ribs, which addRibs lays in wallChar). Anything the shape ever
                // grows here that is NEITHER a socket nor wall is left strictly alone rather
                // than silently re-laid as the default casing.
                if (ch != 'G' && ch != wallChar(tier)) continue;
                final ICasing casing = casingFor(ch);
                if (casing.matches(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z))) continue;
                if (salvage(world, x, y, z)) salvaged++;
                world.setBlock(x, y, z, casing.getBlock(), casing.getBlockMeta(), 3);
                rewritten++;
            }
        }
        return new int[] { tier, rewritten, salvaged };
    }

    /**
     * Does a MACHINE stand in a silo FRONT FACE cell that the CURRENT layout wants as plain
     * WALL? That, and nothing else, is the 0.19.0 conflict: the retired plumbing frontage put
     * hatches where D-20 puts casing, which is why no chained element can accept a legacy face
     * and why {@link #migrateSiloFace} exists.
     * <p>
     * It reads exactly the cells that command rewrites, so the diagnostic, the projector's
     * refusal and the repair can never disagree about what is wrong. Two things hang on it:
     * <ul>
     * <li>B1: a creative projection lays those cells with {@code casingFor(..).asElement()},
     * which is {@code ofBlock} - it OVERWRITES a standing machine and drops nothing. Every
     * hatch of a legacy frontage was voided by one right-click, on the very plant the panel
     * had just said no projection could repair.</li>
     * <li>B2: "a silo is standing" is equally true of one halfway through being built by
     * hand - {@link #standingSiloTier()} answers on two probe cells of nine, by design, so
     * the first laid ring of a tier-5 shell already reads 5. A COMPLETE shell with a
     * conflicting FACE is the legacy state; a half-built one has no machine in a wall cell at
     * all, and must keep its platform formed and idling.</li>
     * </ul>
     */
    private boolean legacyFaceConflict(int tier) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (tier <= 0 || base == null || base.getWorld() == null) return false;
        final World world = base.getWorld();
        final ExtendedFacing facing = getExtendedFacing();
        final String[][] layers = coreShape(tier);
        final int mid = siloHOff(tier);
        final int[] abc = new int[3];
        final int[] xyz = new int[3];
        for (int course = 0; course <= 2; course++) {
            final String frontRow = layers[layers.length - 1 - course][1];
            for (int xx = 0; xx < frontRow.length(); xx++) {
                // ONLY wall cells: a machine in a SOCKET cell is a machine where one belongs,
                // and ' ' / '~' are outside the octagon and the main station itself.
                final char ch = frontRow.charAt(xx);
                if (ch != 'G' && ch != wallChar(tier)) continue;
                abc[0] = xx - mid;
                abc[1] = -course;
                abc[2] = 0;
                facing.getWorldOffset(abc, xyz);
                final int x = base.getXCoord() + xyz[0];
                final int y = base.getYCoord() + xyz[1];
                final int z = base.getZCoord() + xyz[2];
                if (!world.blockExists(x, y, z)) continue;
                if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity) return true;
            }
        }
        return false;
    }

    /**
     * The tier to REPORT as a legacy plant - a standing shell whose face still conflicts - or
     * 0 for every other state, a silo merely under construction included. Everything that
     * prints {@link #siloAbsentLine} reads this, so the GUI, both control blocks, the scanner
     * and the structure check cannot disagree about what is wrong.
     */
    public int legacySiloTier() {
        final int standing = standingSiloTier();
        return legacyFaceConflict(standing) ? standing : 0;
    }

    /** Tells whoever just right-clicked this controller why the projection wrote nothing. */
    private void warnLegacyFace() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return;
        final EntityPlayer player = base.getWorld()
            .getClosestPlayer(base.getXCoord() + 0.5, base.getYCoord() + 0.5, base.getZCoord() + 0.5, 16.0);
        if (player == null) return;
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.RED + "Projection refused"
                    + EnumChatFormatting.RESET
                    + " - this silo's face still has the 0.19.0 hatch layout, and building over it would destroy those hatches. Run "
                    + EnumChatFormatting.WHITE
                    + "/btsmigrate"
                    + EnumChatFormatting.RESET
                    + " beside the controller first: it rewrites the face and drops the old hatches at the wall. Nothing was placed or broken."));
    }

    /**
     * R1-B: the reason a Cooling Tower did not form, or "" when there is nothing worth saying.
     * <p>
     * A tower is OPTIONAL, so "no tower here" is a choice and not a fault - reporting the
     * walk's complaint about a platform that never had one would be pure noise. A tower that
     * has blocks standing and still fails IS a fault, and it is the one a 0.19.0 world hits:
     * that build's Cooling Tower controller demanded a Maintenance Hatch in the base ring, and
     * until now the main station's ring element rejected it in complete silence.
     */
    private String towerFault(List<StructureError> walkErrors, String piece, int dOff) {
        return pieceFault(walkErrors, piece, CENTER, TOWER_TOP, dOff);
    }

    /**
     * The same question for ANY optional piece, and the same two-part answer rule: an optional
     * piece that is simply absent is a CHOICE and reporting the walk's complaint about bare
     * concrete would be pure noise, while a piece with blocks up that still will not form is a
     * FAULT and is the one thing a player needs told. Shared by both Cooling Towers and all four
     * D-17 modules, so no optional building can be added in future that fails in silence.
     */
    private String pieceFault(List<StructureError> walkErrors, String piece, int hOff, int vOff, int dOff) {
        if (walkErrors.isEmpty()) return "";
        if (!anyBlockStanding(piece, hOff, vOff, dOff)) return "";
        final String reason = walkErrors.get(0)
            .getDisplayString();
        return reason == null || reason.isEmpty() ? "structure incomplete" : reason;
    }

    /**
     * EVERY field a structure walk writes, zeroed in ONE place that nothing can get past.
     * <p>
     * D-21 fixes the CLASS of bug, not the instance. M6 moved the two tower fault strings to
     * the top of the check because clearing them BELOW the floor walk's early return left a
     * plant whose platform failed still asserting a reason, in absolute block coordinates, for
     * towers the check never looked at. The identical argument applies to every other piece
     * flag, and not one of them had been moved: mine a single platform block on a running plant
     * and the walk returns at the floor, leaving the panel swearing to a live gallery with 8
     * stages, a Spiral with 8 fitted injectors and a rotor factor - for buildings this check
     * never reached. Called from the FIRST statement of {@link #checkMachine}, so there is no
     * longer any path that skips part of it.
     * <p>
     * RUN state is deliberately NOT here. {@code cascadeMode}, {@code liveColumnsLastCycle},
     * heat, RPM and the buffers are written by the CAMPAIGN, not by a walk, and a structure
     * check fires on every neighbour update - zeroing those here would blank a running plant's
     * readouts several times a second. {@code columnRods} stays for the same reason: it is rod
     * CONTENTS, and {@link #rebuildColumns()} reconciles it against the new pillar map and
     * ejects what no longer has a home, which is not something a failed floor walk may do.
     */
    private void resetPieceState() {
        reactorTier = 0;
        clearHatches();
        // clearHatches() does NOT touch the exotic lists - the laser sockets would otherwise
        // accumulate a duplicate registration on every structure check.
        mExoticDynamoHatches.clear();
        mCasing = 0;
        fuelSegments = 0;
        // D-20: the three discriminated fluid lists are rebuilt by the walks of the pieces
        // that OWN them - feedwater by the silo, coolant and propellant by the Spiral.
        feedwaterHatches.clear();
        propellantHatches.clear();
        chamberHatches.clear();
        // Control-block panels re-register during the silo/gallery walks below; drop the old
        // links now so a failed walk cannot keep stale panels reading this MTE.
        resetSiloPanel();
        resetTurbinePanel();
        // Per-walk scratch: a piece that is never reached must not contribute yesterday's cells.
        walkFuelCells.clear();
        walkPortCells.clear();
        walkGalleryStages = 0;
        walkStageCells.clear();
        // The silo's pillar map.
        columns.clear();
        portCells.clear();
        // The Spine. computeSpine() with spineBuilt false also empties walkHousings and puts
        // both multipliers back to x1.000.
        spineBuilt = false;
        computeSpine();
        // The Spiral.
        spiralBuilt = false;
        spiralInjectors.clear();
        spineSensor = null;
        lastGoodInjectors = 0;
        // The Rotor Assembly.
        galleryBuilt = false;
        galleryStages = 0;
        stageCells.clear();
        avgRotorFactorMilli = 0;
        rotorWear = "";
        // The Cooling Towers. The fault STRINGS are annotations on the two flags and die with
        // them (M6).
        towerFrontBuilt = false;
        towerBackBuilt = false;
        towerFrontFault = "";
        towerBackFault = "";
        // The four D-17 modules, on exactly the same terms. Their PROGRESS counters are NOT
        // here - derrickProgress/annexProgress/the cycle tallies are RUN state written by the
        // modules themselves, and a structure check fires on every neighbour update; zeroing
        // them here would restart a half-finished Annex batch (whose uraninite and acid are
        // already spent) several times a second.
        ventStackBuilt = false;
        switchyardBuilt = false;
        derrickBuilt = false;
        annexBuilt = false;
        ventStackFault = "";
        switchyardFault = "";
        derrickFault = "";
        annexFault = "";
        derrickBus = null;
        annexFeedBus = null;
        annexProductBus = null;
        annexAcidHatches.clear();
        moduleStall = "";
        // D-23: and the PROGRESSION RECORD is not here either, on exactly the same terms and
        // for a much larger stake. The four milestone counters are the player's whole campaign
        // and they are written by the RUNNING plant, never by a walk; a structure check fires
        // on every neighbour update, so touching them here would wipe a campaign several times
        // a second. Un-forming the plant suspends progress; it does not reset it.
        //
        // D-24: the UPGRADE TREE's node set lives in that same record, so the same exclusion
        // covers it - and it is the half a mistake here would hurt most, because a node is a
        // point already spent. Breaking one casing must un-form the plant, not un-buy it.
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetPieceState();
        // The starter structure: the full 50x50 platform must stand.
        if (!checkPiece(floorPieceName(mName), CENTER, 0, SILO_FRONT, errors)) return;
        // The Steam Generator silo is the first addon ON the platform - optional, largest
        // first. Failed walks register hatches as side effects, so clear between probes (the
        // bare platform registers nothing at all).
        for (int t = MAX_TIER; t >= 1; t--) {
            clearHatches();
            mCasing = 0;
            fuelSegments = 0;
            walkFuelCells.clear();
            walkPortCells.clear();
            // The face panel's Silo Control Block registers per probe like any hatch, and so
            // does its Feedwater socket - a probe that aborts halfway must not leave a hatch
            // of a tier that does not stand in the list the reactor draws its water from.
            resetSiloPanel();
            feedwaterHatches.clear();
            if (checkPiece(pieceName(mName, t), siloHOff(t), siloVOff(t), siloDOff(t), null)) {
                reactorTier = t;
                break;
            }
        }
        if (reactorTier > 0) {
            rebuildColumns();
        } else {
            // M1: all five probes failed, and that is TWO completely different states the plant
            // used to report identically as "nothing built yet" - a bare shield pad, and a silo
            // that is physically standing and does not pass. The probes above deliberately pass
            // a NULL error list (four of the five are expected to fail on any plant), so a
            // standing-but-failing silo reached the GUI with an EMPTY error panel, mMachine
            // still true because structureErrors was empty, every fatal demand skipped behind
            // `reactorTier > 0`, and one line of advice - "project it onto the shield pad" -
            // that the projector then declined to act on, silently.
            //
            // So ask the WORLD which shell is on the pad and, when there is one, walk that tier
            // AGAIN with the real error list. GT's StructureChecker halts on the first failing
            // cell, so that is exactly ONE positioned error with coordinates - no packet flood -
            // mMachine goes false because the plant genuinely is not formed, and the summary
            // error below names the one thing that can fix a legacy face.
            // B2: only a LEGACY plant earns the diagnostic. standingSiloTier() alone also
            // answers 5 for the first ring of a tier-5 shell laid by hand, and re-walking that
            // tier with the REAL error list un-formed the PLATFORM - which is specified to
            // form and idle on its own - and accused the player of running 0.19.0 for the rest
            // of their build. The FACE is what separates the two states, and legacySiloTier()
            // is 0 for a silo that is merely under construction.
            final int standing = legacySiloTier();
            if (standing > 0) {
                checkPiece(
                    pieceName(mName, standing),
                    siloHOff(standing),
                    siloVOff(standing),
                    siloDOff(standing),
                    errors);
                errors.add(
                    StructureErrors.of(
                        "beyondthestars.structure.silo.standing",
                        TranslatableText.literal(String.valueOf(standing))));
            }
            // Those probes - including the diagnostic re-walk - registered hatches and counters
            // before aborting; drop that residue so the platform-only state is canonical.
            // D-21: ONLY the residue, now. Everything a walk writes was already zeroed by
            // resetPieceState() at the top of the check, and the only walks between there and
            // here are the SILO probes - so this is exactly the set they can have dirtied.
            clearHatches();
            mExoticDynamoHatches.clear();
            feedwaterHatches.clear();
            mCasing = 0;
            fuelSegments = 0;
            walkFuelCells.clear();
            walkPortCells.clear();
            resetSiloPanel();
            // RUN state, and the one place it is right to clear it: the campaign cannot survive
            // a silo that is not there, so the cascade census stops describing one.
            cascadeMode = false;
            liveColumnsLastCycle = 0;
        }
        // ---------------------------------- the absorbed COOLING TOWERS (optional pieces)
        // Detected exactly like the Spine and the Spiral: no controller, no docking - the
        // shell stands or it does not. R1-B: it is an OPTIONAL piece, so its errors must never
        // reach the fatal `errors` list; but they must not be thrown away either. Each walk
        // gets its own list, and whatever it complains about becomes the reason printed on the
        // "Cooling towers:" line that D-21 gave every panel in every state - a tower that fails
        // to form now says WHY instead of just quietly not counting.
        //
        // M5: the tower ring TOLERATES a legacy Maintenance Hatch but never registers one, so
        // there is nothing to mark and truncate here and - more to the point - nothing a tower
        // can contribute to mMaintenanceHatches. See towerRingElement().
        // Shared by all six optional pieces below (two towers + four modules): each walk gets a
        // clean list, and nothing any of them complains about ever reaches the fatal `errors`.
        final List<StructureError> pieceErrors = new ArrayList<>();
        towerFrontBuilt = checkPiece(towerPieceName(mName, "front"), CENTER, TOWER_TOP, TOWER_FRONT_DOFF, pieceErrors);
        if (!towerFrontBuilt) {
            towerFrontFault = towerFault(pieceErrors, towerPieceName(mName, "front"), TOWER_FRONT_DOFF);
        }
        pieceErrors.clear();
        towerBackBuilt = checkPiece(towerPieceName(mName, "back"), CENTER, TOWER_TOP, TOWER_BACK_DOFF, pieceErrors);
        if (!towerBackBuilt) {
            towerBackFault = towerFault(pieceErrors, towerPieceName(mName, "back"), TOWER_BACK_DOFF);
        }
        // -------------------------------- the four D-17 PLANT MODULES (optional checked pieces)
        // Same contract as the towers, all the way down: their own error list so nothing they
        // complain about can un-form the plant, a fault string when blocks are standing, and no
        // dependence on the silo - a bare platform can carry all four.
        //
        // The Derrick and the Annex REGISTER sockets during their walk, into dedicated fields
        // (never a plant-wide list). Those fields are nulled before each walk and again if the
        // walk fails, so a half-walked module cannot leave a hatch of a building that does not
        // stand pointing at the controller.
        pieceErrors.clear();
        ventStackBuilt = checkPiece(
            ventPieceName(mName),
            moduleHOff(VENT_A0),
            VENT_HEIGHT - 1,
            moduleDOff(VENT_C0),
            pieceErrors);
        if (!ventStackBuilt) {
            ventStackFault = pieceFault(
                pieceErrors,
                ventPieceName(mName),
                moduleHOff(VENT_A0),
                VENT_HEIGHT - 1,
                moduleDOff(VENT_C0));
        }
        pieceErrors.clear();
        switchyardBuilt = checkPiece(
            yardPieceName(mName),
            moduleHOff(YARD_A0),
            YARD_HEIGHT - 1,
            moduleDOff(YARD_C0),
            pieceErrors);
        if (!switchyardBuilt) {
            switchyardFault = pieceFault(
                pieceErrors,
                yardPieceName(mName),
                moduleHOff(YARD_A0),
                YARD_HEIGHT - 1,
                moduleDOff(YARD_C0));
        }
        pieceErrors.clear();
        derrickBus = null;
        derrickBuilt = checkPiece(
            derrickPieceName(mName),
            moduleHOff(DERRICK_A0),
            DERRICK_HEIGHT - 1,
            moduleDOff(DERRICK_C0),
            pieceErrors);
        if (!derrickBuilt) {
            derrickBus = null;
            derrickFault = pieceFault(
                pieceErrors,
                derrickPieceName(mName),
                moduleHOff(DERRICK_A0),
                DERRICK_HEIGHT - 1,
                moduleDOff(DERRICK_C0));
        }
        pieceErrors.clear();
        annexFeedBus = null;
        annexProductBus = null;
        annexAcidHatches.clear();
        annexBuilt = checkPiece(
            annexPieceName(mName),
            moduleHOff(ANNEX_A0),
            ANNEX_HEIGHT - 1,
            moduleDOff(ANNEX_C0),
            pieceErrors);
        if (!annexBuilt) {
            annexFeedBus = null;
            annexProductBus = null;
            annexAcidHatches.clear();
            annexFault = pieceFault(
                pieceErrors,
                annexPieceName(mName),
                moduleHOff(ANNEX_A0),
                ANNEX_HEIGHT - 1,
                moduleDOff(ANNEX_C0));
        }
        // Hatch demands only apply once the silo exists; a bare platform forms and idles.
        if (reactorTier > 0) {
            // The Spine (optional): 3x3 duct with rotor sockets, leaving the silo's side.
            walkHousings.clear();
            spineBuilt = checkPiece(
                ductPieceName(mName, reactorTier),
                ductHOff(reactorTier),
                2,
                ductDOff(reactorTier),
                null);
            computeSpine();
            // The Spiral (optional): its injectors, Sensor, Coolant Input Hatch and bulk
            // Propellant Input Hatch all register here during the walk (D-20). The general
            // lists it touches - Maintenance from the floor socket, Input Hatch from the two
            // supply sockets - are marked and truncated on a FAILED walk, so the SILO's own
            // feedwater hatch, registered earlier, survives untouched.
            spiralInjectors.clear();
            spineSensor = null;
            chamberHatches.clear();
            propellantHatches.clear();
            final int maintMark = mMaintenanceHatches.size();
            final int inputMark = mInputHatches.size();
            // RC-2: GT's input adder can now register dual input hatches (CRIBs) too. The supply
            // sockets refuse them before super, so this list should never grow during the walk;
            // it is marked and truncated anyway so a failed Spiral can never leave one behind.
            final int dualInputMark = mDualInputHatches.size();
            spiralBuilt = checkPiece(spiralPieceName(mName), -18, SPIRAL_HEIGHT - 1, spiralDOff(reactorTier), null);
            if (!spiralBuilt) {
                spiralInjectors.clear();
                spineSensor = null;
                lastGoodInjectors = 0;
                chamberHatches.clear();
                propellantHatches.clear();
                while (mMaintenanceHatches.size() > maintMark) {
                    mMaintenanceHatches.remove(mMaintenanceHatches.size() - 1);
                }
                while (mInputHatches.size() > inputMark) {
                    mInputHatches.remove(mInputHatches.size() - 1);
                }
                while (mDualInputHatches.size() > dualInputMark) {
                    mDualInputHatches.remove(mDualInputHatches.size() - 1);
                }
            }
            // The Rotor Assembly (optional): same mark-and-truncate discipline for its
            // Dynamo/Energy/LaserSource/Maintenance registrations when the walk fails.
            walkGalleryStages = 0;
            walkStageCells.clear();
            resetTurbinePanel();
            final int dynamoMark = mDynamoHatches.size();
            final int laserMark = mExoticDynamoHatches.size();
            final int energyMark = mEnergyHatches.size();
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
                while (mExoticDynamoHatches.size() > laserMark) {
                    mExoticDynamoHatches.remove(mExoticDynamoHatches.size() - 1);
                }
                while (mEnergyHatches.size() > energyMark) {
                    mEnergyHatches.remove(mEnergyHatches.size() - 1);
                }
                while (mMaintenanceHatches.size() > galleryMaintMark) {
                    mMaintenanceHatches.remove(mMaintenanceHatches.size() - 1);
                }
            }
            checkCasingMin(errors, mCasing, minCasingsFor(reactorTier));
            // D-20: each demand is now checked against the piece that CARRIES that socket.
            // Maintenance, the rod bus, the depleted bus, feedwater and the fluid output are
            // all in the silo's own face, so they are due the moment a silo stands...
            checkHasMaintenanceHatch(errors);
            checkHasInputHatch(errors);
            checkHasOutputHatch(errors);
            // The cascade emits depleted rods as items - without a bus they would be voided.
            checkHasOutputBus(errors);
            // ...while the four Energy Hatches on the OUTPUT MANIFOLD are NOT a structure
            // demand at all any more (R1-B / C1).
            //
            // WHY: the REACTOR AND TURBINE never draw EU. Nothing in the steam chain reads
            // mEnergyHatches - onRunningTick emits through the dynamo and laser rows, every
            // branch of checkProcessing sets lEUt to 0, and outputCeiling() is dynamos +
            // lasers. A missing Energy Hatch cannot change one number the REACTOR produces,
            // so making it FATAL made the structure lie about the behaviour: it un-formed a
            // plant that was physically and functionally complete. And un-forming is not
            // cosmetic here - it stops the campaign, which starts the twenty-minute
            // decay-heat obligation. Charging a player twenty minutes of mandatory cooling
            // for a hatch the reactor does not use is not a difficulty setting, it is a bug.
            //
            // C5 - READ THIS BEFORE DELETING THE 'r' SOCKET AS UNUSED FURNITURE. Since 0.21.0
            // the sockets ARE a load, just not a structure demand. moduleSupplied() calls
            // drainEnergyInput() once per tick for each of the two D-17 PROCESS modules - the
            // Extraction Derrick at DERRICK_EU_PER_TICK (TierEU.RECIPE_EV, 1,920 EU/t) and the
            // Enrichment Annex at ANNEX_EU_PER_TICK (TierEU.RECIPE_HV, 480 EU/t) - and that is
            // the ONLY EU this class consumes. So: an empty 'r' socket still forms the plant
            // and still generates, and that is exactly why it stays optional; but on a plant
            // carrying either module it means neither module ever turns over. energyHatchNote()
            // says so out loud, in RED when a module stands with no hatch fitted at all.
            //
            // It also made the build order unsurvivable. galleryBuilt flips false->true the
            // instant the last gallery Turbine Housing is placed (the '9' element fails on air
            // and survivalPlaceBlock SKIPs, so the projector never fills them). The demand
            // switched on mid-step, the hatches were three steps away, and the whole plant
            // stopped - while the panel promised readouts that could not happen yet.
            //
            // The manifold's empty E sockets are still reported, as a NON-FATAL note in the
            // OUTPUT group (see energyHatchNote), so "the manifold is not finished" is still
            // said out loud - it just no longer shuts the reactor down.
        } else if (mMaintenanceHatches.isEmpty()) {
            // A bare platform has no Maintenance socket at all - without this it would sit in
            // a permanent NO_REPAIR shutdown instead of idling into checkProcessing.
            fixAllIssues();
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

    /**
     * THE PROJECTOR SAFETY PROPERTY: no projection may ever target a SMALLER silo than the one
     * standing on the pad. In strict priority order,
     * <ol>
     * <li>the silo the live probe just detected - a formed plant is never re-tiered;</li>
     * <li>the silo the WORLD says is standing ({@link #standingSiloTier()}) - the repair case,
     * and the one a live reading gets wrong: a single missing casing zeroes {@code reactorTier}
     * while a full-size shell is physically there, which is exactly when a player projects;</li>
     * <li>only when neither holds - a bare pad, or a silo that has actually been taken down -
     * the trigger stack / GUI Build Tier, which is then free to choose any tier.</li>
     * </ol>
     * Step 2 is a MEASUREMENT, and that is what makes the property hold everywhere the old
     * remembered field could not: {@link #construct} runs CLIENT-side for the hint path and
     * reads the same blocks the server does, so the ghost can never be a tier smaller than the
     * shell the player is standing in; and wrenching the controller out and back cannot re-arm
     * anything, because there was nothing stored to lose.
     */
    private int buildTargetTier(ItemStack stackSize) {
        if (reactorTier > 0) return reactorTier;
        final int standing = standingSiloTier();
        if (standing > 0) return standing;
        return tierFromTrigger(stackSize);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        // R1 FIX. `reactorTier` is written by the SILO PROBE at the top of checkMachine, which
        // walks tiers 5..1 and records the one whose shell passes; it survives a later piece
        // failing and therefore describes the silo that is PHYSICALLY STANDING, whether or not
        // the plant as a whole formed. The old gate `mMachine && reactorTier > 0` threw that
        // away: any I/O change un-forms every pre-existing plant, so mMachine went false while
        // reactorTier was still correctly 5, the else-branch fell through to tierFromTrigger()
        // = the GUI Build Tier (default 1), and the projection tried to raise a TIER-1 silo
        // INSIDE the standing TIER-5 one (a T1 wall cell at octagon r=4,x=0 is platform
        // a=21,c=21, which is open interior in a T5 silo). Trust a detected tier whenever there
        // is one; only fall back to the Build Tier setting when NOTHING was detected.
        final boolean siloStanding = reactorTier > 0;
        // R1-B: a DAMAGED silo reads reactorTier 0 but must still be repaired at its own tier,
        // so the piece is offered for building (siloStanding stays a live reading) while the
        // TARGET comes from the memory. This is what turns the projector back into a repair
        // tool instead of a demolition charge.
        final int t = buildTargetTier(stackSize);
        // The platform is the mandatory starter piece and is always a legal target: on a plant
        // whose floor already stands this returns -1 immediately and costs nothing.
        final int floorPart = survivalBuildPiece(
            floorPieceName(mName),
            stackSize,
            CENTER,
            0,
            SILO_FRONT,
            elementBudget,
            env,
            false,
            true);
        if (floorPart >= 0) return floorPart;
        if (!siloStanding) {
            final int siloPart = survivalBuildPiece(
                pieceName(mName, t),
                stackSize,
                siloHOff(t),
                siloVOff(t),
                siloDOff(t),
                elementBudget,
                env,
                false,
                true);
            if (siloPart >= 0) return siloPart;
        }
        final int ductPart = survivalBuildPiece(
            ductPieceName(mName, t),
            stackSize,
            ductHOff(t),
            2,
            ductDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (ductPart >= 0) return ductPart;
        final int spiralPart = survivalBuildPiece(
            spiralPieceName(mName),
            stackSize,
            -18,
            SPIRAL_HEIGHT - 1,
            spiralDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (spiralPart >= 0) return spiralPart;
        final int galleryPart = survivalBuildPiece(
            galleryPieceName(mName),
            stackSize,
            -18,
            6,
            galleryDOff(t),
            elementBudget,
            env,
            false,
            true);
        if (galleryPart >= 0) return galleryPart;
        // R2: the tower pieces include the two Solid Steel cells per tower that the retired
        // module controller used to stand on, so a survival projection closes that base ring.
        final int frontPart = survivalBuildPiece(
            towerPieceName(mName, "front"),
            stackSize,
            CENTER,
            TOWER_TOP,
            TOWER_FRONT_DOFF,
            elementBudget,
            env,
            false,
            true);
        if (frontPart >= 0) return frontPart;
        final int backPart = survivalBuildPiece(
            towerPieceName(mName, "back"),
            stackSize,
            CENTER,
            TOWER_TOP,
            TOWER_BACK_DOFF,
            elementBudget,
            env,
            false,
            true);
        if (backPart >= 0) return backPart;
        // D-17 / D-22: the four plant modules are part of the same one projection.
        final int ventPart = survivalBuildPiece(
            ventPieceName(mName),
            stackSize,
            moduleHOff(VENT_A0),
            VENT_HEIGHT - 1,
            moduleDOff(VENT_C0),
            elementBudget,
            env,
            false,
            true);
        if (ventPart >= 0) return ventPart;
        final int yardPart = survivalBuildPiece(
            yardPieceName(mName),
            stackSize,
            moduleHOff(YARD_A0),
            YARD_HEIGHT - 1,
            moduleDOff(YARD_C0),
            elementBudget,
            env,
            false,
            true);
        if (yardPart >= 0) return yardPart;
        final int derrickPart = survivalBuildPiece(
            derrickPieceName(mName),
            stackSize,
            moduleHOff(DERRICK_A0),
            DERRICK_HEIGHT - 1,
            moduleDOff(DERRICK_C0),
            elementBudget,
            env,
            false,
            true);
        if (derrickPart >= 0) return derrickPart;
        return survivalBuildPiece(
            annexPieceName(mName),
            stackSize,
            moduleHOff(ANNEX_A0),
            ANNEX_HEIGHT - 1,
            moduleDOff(ANNEX_C0),
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
        // M3: this runs on the CLIENT for the hint path (ConstructableUtility dispatches the
        // non-EntityPlayerMP path there), where reactorTier is always 0 because checkMachine
        // only ever runs server-side. buildTargetTier's second step is a world measurement, so
        // the client reaches the same tier the server would and the ghost matches the shell the
        // player is standing in. Drawing a Build-Tier-1 silo inside a tier-5 one - with tier-1
        // interior cells hinted as AIR over live fuel pillars - is how a player was invited to
        // demolish their own plant by hand.
        final int t = buildTargetTier(stackSize);
        // B1: the non-hint path WRITES, and a plain casing element is ofBlock - it overwrites
        // whatever stands in its cell and drops nothing. On a legacy face that is every hatch
        // of the retired frontage voided by one right-click, on exactly the plant the panel
        // has just said no projection can repair. Refuse the write - the HINT path is
        // untouched, so the ghost still draws - and name the command that does repair it.
        // A correct or half-built plant has no machine in a wall cell and is never refused.
        if (!hintsOnly && legacyFaceConflict(t)) {
            warnLegacyFace();
            return;
        }
        // A4: ONE projection is now literally the whole plant. Nothing is left for a second
        // controller to build, because there is no second controller anywhere in the design.
        buildPiece(floorPieceName(mName), stackSize, hintsOnly, CENTER, 0, SILO_FRONT);
        buildPiece(pieceName(mName, t), stackSize, hintsOnly, siloHOff(t), siloVOff(t), siloDOff(t));
        buildPiece(ductPieceName(mName, t), stackSize, hintsOnly, ductHOff(t), 2, ductDOff(t));
        buildPiece(spiralPieceName(mName), stackSize, hintsOnly, -18, SPIRAL_HEIGHT - 1, spiralDOff(t));
        buildPiece(galleryPieceName(mName), stackSize, hintsOnly, -18, 6, galleryDOff(t));
        buildPiece(towerPieceName(mName, "front"), stackSize, hintsOnly, CENTER, TOWER_TOP, TOWER_FRONT_DOFF);
        buildPiece(towerPieceName(mName, "back"), stackSize, hintsOnly, CENTER, TOWER_TOP, TOWER_BACK_DOFF);
        // D-17 / D-22 modules. Their sites are FIXED platform rectangles - no tier term anywhere
        // in these three arguments - so the client hint path draws them in the right place
        // whatever `t` came out to, and no server-only field is read to place them.
        buildPiece(
            ventPieceName(mName),
            stackSize,
            hintsOnly,
            moduleHOff(VENT_A0),
            VENT_HEIGHT - 1,
            moduleDOff(VENT_C0));
        buildPiece(
            yardPieceName(mName),
            stackSize,
            hintsOnly,
            moduleHOff(YARD_A0),
            YARD_HEIGHT - 1,
            moduleDOff(YARD_C0));
        buildPiece(
            derrickPieceName(mName),
            stackSize,
            hintsOnly,
            moduleHOff(DERRICK_A0),
            DERRICK_HEIGHT - 1,
            moduleDOff(DERRICK_C0));
        buildPiece(
            annexPieceName(mName),
            stackSize,
            hintsOnly,
            moduleHOff(ANNEX_A0),
            ANNEX_HEIGHT - 1,
            moduleDOff(ANNEX_C0));
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

    // ------------------------------------------------------- B1: the NEI display page's data
    // The NEI page is DISPLAY ONLY - it never feeds checkProcessing - but every number on it
    // is read out of the RUNTIME's own tables through these accessors, so a tuning change can
    // never leave the page lying. Nothing below is new game data; it is the existing private
    // state made readable.

    /** Rod grades, worst first: 1..4 = Uranium, MOX, Thorium-Bonded, Naquadah-Bonded. */
    public static final int ROD_GRADES = 4;

    /** The fresh rod item of a grade (1..4). */
    public static Item fuelRodItem(int grade) {
        return fuelRodFor(Math.min(ROD_NAQ, Math.max(ROD_URANIUM, grade)));
    }

    /** The depleted rod a burned segment of that grade drops. */
    public static Item depletedRodItem(int grade) {
        return depletedRodFor(Math.min(ROD_NAQ, Math.max(ROD_URANIUM, grade)));
    }

    /** Fuel multiplier of a rod grade: Uranium x1, MOX x2, Thorium x5, Naquadah x9. */
    public static int rodMultiplier(int grade) {
        return ROD_MULT[Math.min(ROD_NAQ, Math.max(ROD_URANIUM, grade))];
    }

    /** Display name of a rod grade, from the same table the panels use. */
    public static String rodGradeName(int grade) {
        return fuelName(rodMultiplier(grade));
    }

    /** Steam basis of ONE live pillar before the fuel multiplier and the throttle. */
    public static long steamPerPillar() {
        return STEAM_PER_COLUMN;
    }

    /** Pillars a tier's silo can hold (tiers 1..5: 45/77/109/157/201). */
    public static int columnCapOf(int tier) {
        return COLUMN_CAP[Math.min(MAX_TIER, Math.max(1, tier))];
    }

    /** Rod segments in one pillar - a rod burns one segment per decay cycle. */
    public static int segmentsPerPillar() {
        return SEGMENTS_PER_COLUMN;
    }

    /** Length of ONE decay cycle in ticks: a campaign divided over the eight segments. */
    public static int decayCycleTicks() {
        return Math.max(1, Config.genICampaignMinutes * 60 * 20 / SEGMENTS_PER_COLUMN);
    }

    /** RPM added per perfectly timed cycle, before P and the timed/8 fraction. */
    public static int rpmGain() {
        return RPM_GAIN;
    }

    /** Ticks between two sustainable cycles (one buffered steam slug is 20 ticks of steam). */
    public static int cycleSlugTicks() {
        return CYCLE_SLUG_TICKS;
    }

    /** RPM retained per tick, in permille - the other half of the held-RPM equilibrium. */
    public static int rpmDecayPermille() {
        return RPM_DECAY_PERMILLE;
    }

    /** Propellant ladder, worst rung first, as it is offered to the injectors. */
    public static final int PROPELLANT_RUNGS = 9;

    /**
     * One litre-stack of the ladder's rung {@code rung} (0 = Benzene .. 8 = Naquadah Propellant Lv 6
     * - our own fluid, NOT GoodGenerator's Naquadah Fuel, which is what it is mixed FROM).
     * The NEI page walks this and asks {@link #propellantMilliOf} for each rung's P, so the page
     * and the running plant read the SAME table - a retuned P cannot survive in only one of them.
     */
    public static FluidStack propellantOfRung(int rung, int litres) {
        switch (rung) {
            case 0:
                return Materials.Benzene.getFluid(litres);
            case 1:
                return Materials.GasolinePremium.getFluid(litres);
            case 2:
                return new FluidStack(GTPPFluids.H8N4C2O4RocketFuel, litres);
            case 3:
                return new FluidStack(BTSFluids.naqPropellantT1, litres);
            case 4:
                return new FluidStack(BTSFluids.naqPropellantT2, litres);
            case 5:
                return new FluidStack(BTSFluids.naqPropellantT3, litres);
            case 6:
                return new FluidStack(BTSFluids.naqPropellantT4, litres);
            case 7:
                return new FluidStack(BTSFluids.naqPropellantT5, litres);
            default:
                return new FluidStack(BTSFluids.naqPropellantT6, litres);
        }
    }

    /** Stage factor P (x1000) of a propellant - the runtime lookup, exposed for the NEI page. */
    public static int propellantMilli(FluidStack stack) {
        return propellantMilliOf(stack);
    }

    /** Coolant ladder rungs, best first (index 0 = Tachyon Rich Temporal Fluid). */
    public static int coolantRungs() {
        return CHAMBER_COOLANT_Q.length;
    }

    /** Coolant quality Q (x1000) of a rung. */
    public static int coolantQMilli(int rung) {
        return CHAMBER_COOLANT_Q[Math.min(CHAMBER_COOLANT_Q.length - 1, Math.max(0, rung))];
    }

    /** Coolant fluid of a rung (the runtime ladder itself). */
    public static FluidStack coolantOfRung(int rung, int litres) {
        switch (rung) {
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

    /** The chamber's per-tick heat load at a fan speed, with a residual swirl bias. */
    public static int chamberLoadFor(int rpmMilliValue, int swirl) {
        // B4: a STATIONARY fan is not a load. Residual swirl is turbulence a TURNING fan
        // stirs up, so at rest there is nothing to carry away and nothing to pay for - the
        // same ruling that says only a turning fan can seize. Charging the swirl term at
        // RPM 0 drained the chamber's whole coolant reserve (4 L/t at |S|=4 - the four Water
        // Buckets step 8 hands out, gone inside 50 seconds) while the player was still wiring
        // the clock, and the fan then refused to spin for a reason nothing on the panel showed.
        if (rpmMilliValue <= 0) return 0;
        final double n = rpmMilliValue / 1_000_000.0;
        // One ceiling over the WHOLE load: rounding the swirl term separately truncated an
        // |S|=1 spine to zero, so a mistuned Spine cost neither coolant nor heat.
        return (int) Math.ceil(200.0 * n * n + swirl * (double) swirl * SWIRL_HEAT_PER_SECOND / 20.0);
    }

    /** Litres per tick a rung must supply to carry a given load away. */
    public static int coolantDrainFor(int load, int rung) {
        return coolantLitresFor(load, rung);
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

    /**
     * A2 INTERNAL ROD DISTRIBUTION - the thing that makes the plant automatable. The
     * controller pulls fuel rods out of the silo's own ROD INPUT BUS every decay cycle
     * and tops the pillar reserves up ITSELF, lowest empty segment first, exactly as a Rod
     * Loading Port would. The player never has to touch a pillar port again: 45-201 ports
     * spread across a 17-wide octagon were the whole reason "it doesn't have a good automation
     * flow". The ports still work for hand-feeding (see {@link #drainPorts()}); they are simply
     * no longer REQUIRED, and the hologram no longer asks for one per pillar.
     * <p>
     * Best grade first, so a bus holding several fuels feeds the strongest into the cascade.
     * Returns how many rods were distributed.
     */
    private int distributeRodsFromBus() {
        if (columns.isEmpty()) return 0;
        int placed = 0;
        for (int type = ROD_NAQ; type >= ROD_URANIUM; type--) {
            final ItemStack sample = new ItemStack(fuelRodFor(type), 1);
            int available = countMatchingInputs(sample);
            if (available <= 0) continue;
            int used = 0;
            for (final java.util.Map.Entry<Long, ColumnInfo> entry : columns.entrySet()) {
                if (available <= 0) break;
                final byte[] rods = rodsFor(entry.getKey(), entry.getValue());
                int free = firstEmpty(rods);
                while (free >= 0 && available > 0) {
                    rods[free] = (byte) type;
                    available--;
                    used++;
                    free = firstEmpty(rods);
                }
            }
            for (int i = 0; i < used; i++) {
                depleteInput(sample);
            }
            placed += used;
        }
        if (placed > 0) {
            updateSlots();
        }
        return placed;
    }

    /** Empty pillar segments across the whole silo - the supply readout's "gaps" figure. */
    private int pillarGaps() {
        int gaps = 0;
        for (final java.util.Map.Entry<Long, ColumnInfo> entry : columns.entrySet()) {
            final byte[] rods = columnRods.get(entry.getKey());
            final int height = entry.getValue().height;
            if (rods == null) {
                gaps += height;
                continue;
            }
            for (final byte rod : rods) {
                if (rod == ROD_NONE) gaps++;
            }
        }
        return gaps;
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
        // D-23 BURNUP 5/6/7: the cap is 24 (the design stop) until the BURNUP line lifts it to
        // 26, 28, 30. D-24 adds the tree's three rod nodes on top of that, +2 each, to an
        // absolute 36. The DENOMINATOR below stays 24 throughout, so 36/24 really is 150% of
        // design power and not a relabelled 100%.
        final int throttleCap = maxThrottle();
        int n = Math.min(throttleCap, Math.max(1, throttleSetting));
        ItemStack circuit = getControllerSlot();
        if (GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1) {
            n = Math.min(throttleCap, circuit.getItemDamage());
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
        // A2: and then the rod bus does the same job for the whole silo at once.
        distributeRodsFromBus();
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
            // R7: keep the derivation's first term equal to the number the cascade actually
            // summed. bestType is only the label ("what is the best rod in there"); the steam
            // came from steamBase, one term per pillar.
            fuelBaseSteam = steamBase;
            fuelGradeMix = gradeMix(liveByType);
            // D-23 BURNUP: one committed cycle burns one segment per live pillar, weighted by
            // its grade (ROD_MULT), so the line rewards the FUEL CHAIN and not the rod count.
            // Fed from the COMMIT branch only - the peek loop above runs on every failed
            // checkProcessing and must never score - and checkProcessing is not a structure
            // check, so no neighbour update can double-count it.
            for (int type = 1; type < liveByType.length; type++) {
                progression.add(BTSMilestone.BURNUP, (long) liveByType[type] * ROD_MULT[type]);
            }
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
            // One batch is one grade, so the sum IS the product - but it is already halved.
            fuelBaseSteam = STEAM_PER_COLUMN * fallbackColumns * fallbackMult / 2;
            fuelGradeMix = "";
            // D-23 BURNUP: a half-rate fallback slice scores at half rate too.
            progression.add(BTSMilestone.BURNUP, (long) fallbackColumns * fallbackMult / 2);
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
            fuelBaseSteam = STEAM_PER_COLUMN * busColumns * fallbackMult / 2;
            fuelGradeMix = "";
            progression.add(BTSMilestone.BURNUP, (long) fallbackColumns * fallbackMult / 2);
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
        // D-23 UPTIME 5/7: condensate recovery adds further right-shifts on top of the towers'.
        // It is purely a REDUCTION in what the plant drinks, so it can starve nothing and it
        // moves no output figure - which is exactly why it is safe to grant automatically.
        final int flowPerTick = Math.max(1, baseFlow >> Math.min(16, towers + progression.condensateStages()));

        startRecipeProcessing();
        if (galleryBuilt && galleryStages > 0) {
            // TC-3 finale: FAN RPM IS THE CENTRAL VARIABLE. The produced steam feeds the
            // chain's standing buffer (redstone cycles drink it in 20-tick slugs), and
            // EU/t = L/t x speed x eff x (RPM/1000) x avgRotorFactor - the avg already
            // carries stages/8 (a missing or shattered rotor contributes a zero stage).
            if (drawFeedwater(flowPerTick)) {
                steamBuffer = Math.min(steamBuffer + steamPerTick, (long) steamBufferCapTicks() * steamPerTick);
                // /500_000 (not /1_000_000): back-solved so a fully optimized T5 - 201 naq
                // pillars, all-riding spine, redline RPM, 8 fresh rotors - peaks ~1.10B and
                // averages ~1.05B EU/t across the decay sawtooth, landing on the settled
                // D-01 ~1B ceiling instead of half of it.
                final long demand = euDemand();
                // Clamp to the SUMMED OUTPUT CEILING before emitting: never hand the hatch row
                // more than it can absorb. The ceiling counts the laser sources as well as the
                // dynamos - this pre-clamp was once blind to them and pinned the plant at the
                // four IV dynamos' 32,768 EU/t.
                // D-17: outputLimit() is min(hatch ceiling, grid ceiling), so the SWITCHYARD
                // soft cap can only ever LOWER what is injected. It can never lift the figure
                // above what the fitted dynamos and lasers can absorb.
                // D-25: the injection itself goes through OUR distributor, not GT's
                // addEnergyOutputMultipleDynamos - see {@link #distributeOutput} for the int
                // overflow in GT's version that used to force a 2^31-1 clamp on this plant.
                final long emitted = Math.min(demand, outputLimit());
                long delivered = 0L;
                if (emitted > 0) {
                    delivered = distributeOutput(emitted);
                    // D-23 UPTIME: a GENERATING tick, and only a generating tick. An idle,
                    // chunk-loaded plant earns nothing here, which is the whole character of
                    // the line. onRunningTick is server-only and fires once a tick, so this can
                    // neither run on the client nor be double-counted by a structure re-check.
                    // Gated on the DEMAND side, exactly as before: a plant that asked for EU
                    // earns its tick even if a hatch was pulled out from under it mid-tick.
                    progression.add(BTSMilestone.UPTIME, 1L);
                }
                lastEUDemand = demand;
                lastEUOutput = delivered;
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
        final long speedOnly = steamPerTick * spineSpeedMilli / 1000;
        return withEfficiency ? speedOnly * spineEffMilli / 1000 : speedOnly;
    }

    // -------------------------------------------------- B2: the ONE arithmetic, shared
    // The GUI's math breakdown, the control-block readouts and the generating tick all call
    // these, so the derivation a player reads is literally the code that pays them.

    /** Steam through the whole chain: campaign L/t x spine speed x spine efficiency. */
    public static long steamAfterSpine(long steam, int speedMilli, int effMilli) {
        return steam * speedMilli / 1000 * effMilli / 1000;
    }

    /**
     * The plant's EU/t DEMAND before the output ceiling clamps it.
     * <p>
     * The RPM term is {@code rpmMilli / 500,000}, i.e. RPM/500 (x24 at the 12,000 redline) -
     * back-solved so a fully optimized T5 lands on the settled ~1B ceiling instead of half of
     * it. The readouts print the divisor they actually use; do not "correct" it to 1,000.
     */
    public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli) {
        return euDemandOf(afterSpine, rpmMilliValue, rotorMilli, 1000, 1000);
    }

    /** D-23's four-factor form, before D-24 added the tree. Delegates with no tree term. */
    public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli, int progMilli) {
        return euDemandOf(afterSpine, rpmMilliValue, rotorMilli, progMilli, 1000);
    }

    /**
     * D-23: the same arithmetic with the MILESTONE term on the end - the product of the four
     * lines' tier-1..4 multipliers, x1000. Everything that derives EU goes through here, so
     * the milestone bonus is a TERM of the published derivation and not a private fudge
     * factor: {@link #mathTurbineLine} prints it whenever it is not x1.000.
     * <p>
     * It multiplies the EU and deliberately NOT the steam. Steam L/t is what sets the
     * feedwater draw, the waste-water output and the export line; a milestone that raised it
     * would raise the water demand of a plant that is already exactly supplied, and could
     * starve one that ran fine yesterday. The ruling asked for a reward in EU; it lands in EU.
     * <p>
     * At {@code progMilli == 1000} the last factor is {@code x * 1000 / 1000}, which is
     * exactly {@code x} - so a plant with no progress derives a BIT-IDENTICAL number to the
     * one it derived before this system existed. That is the regression test.
     */
    public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli, int progMilli, int upgradeMilli) {
        return euDemandOf(afterSpine, rpmMilliValue, rotorMilli, progMilli, upgradeMilli, 1000);
    }

    /**
     * D-26: the same arithmetic with the FEEDWATER GRADE on the end - {@code x1.000} for plain and
     * distilled water, {@code x1.05} to {@code x1.40} for purified Grades 1 to 8, straight out of
     * {@link BTSWaterGrade#outputMilli}.
     * <p>
     * <b>Why it is a term of THIS method and not a private adjustment somewhere downstream.</b>
     * Everything that derives EU goes through here, and {@link #mathTurbineLine} prints every
     * factor this method performs. A water bonus applied anywhere else would make the published
     * derivation false - the panel would print four factors and the plant would have used five.
     * That defect has happened on this machine before and the fix was this rule.
     * <p>
     * <b>The ruling's balance guard, discharged in the arithmetic itself.</b> The water grade's
     * OUTPUT span here is x1.000 to x1.400. Its YIELD span on the fuel line is x0.5 to x4.5, a
     * factor of 9 - so the output effect is the SMALLER of the two by 6.4x. Put the other way:
     * the entire water ladder is worth +40% EU, while the cheapest single rung of the FUEL ladder
     * (ROD_MULT 1 -> 2) is worth +100% and the whole fuel ladder +800%. A Purification Plant can
     * therefore never be a route to power; it is a route to cheaper fuel.
     * <p>
     * <b>The regression test.</b> {@code drawFeedwater} tries plain water FIRST and grades plain
     * and distilled water alike at {@code waterMilli == 1000}, so the last factor is
     * {@code x * 1000 / 1000}, which is exactly {@code x}. A plant that ran yesterday on plain
     * water derives a BIT-IDENTICAL figure today. No water grade is, or can become, mandatory.
     */
    public static long euDemandOf(long afterSpine, int rpmMilliValue, int rotorMilli, int progMilli, int upgradeMilli,
        int waterMilli) {
        return afterSpine * rpmMilliValue
            / 500_000L
            * rotorMilli
            / 1000L
            * Math.max(1000, progMilli)
            / 1000L
            * Math.max(1000, upgradeMilli)
            / 1000L
            * Math.max(1000, waterMilli)
            / 1000L;
    }

    /**
     * THIS controller's demand: the shared arithmetic, with its milestone term AND D-24's
     * upgrade-tree term.
     * <p>
     * The two progression terms are kept as SEPARATE factors, applied in the order
     * {@link #mathTurbineLine} prints them, rather than pre-multiplied into one. That is what
     * makes the MATH panel exact rather than approximately right: two printed factors are two
     * performed factors, down to the integer division between them.
     * <p>
     * Both default to 1000, so at zero progress and an unbought tree the last two factors are
     * {@code x * 1000 / 1000 * 1000 / 1000 == x} and the plant derives a BIT-IDENTICAL number
     * to the one it derived in 0.21.0. That is the regression test for this whole release.
     */
    public long euDemand() {
        return euDemandOf(
            spineBoostedSteam(true),
            rpmMilli,
            avgRotorFactorMilli,
            progOutputMilli(),
            progUpgradeMilli(),
            waterOutputMilli());
    }

    // ------------------------------------------------------------ manifold output ceiling

    /** Summed A/t x V of the plain Dynamo Hatches in the manifold's mid-height row. */
    public long getDynamoCapacity() {
        long capacity = 0;
        for (final MTEHatchDynamo dynamo : GTUtility.validMTEList(mDynamoHatches)) {
            capacity += dynamo.maxAmperesOut() * dynamo.maxEUOutput();
        }
        return capacity;
    }

    /**
     * Summed A/t x V of the Laser Source Hatches in the manifold's upper row. TecTech laser
     * sources land in mExoticDynamoHatches, which {@link #distributeOutput} walks straight
     * after the dynamo row - they simply have to be counted here as well or the pre-clamp
     * throws the power away before it is ever offered to them.
     * <p>
     * Long-clean: both {@code maxAmperesOut()} and {@code maxEUOutput()} return {@code long} in
     * GT and the product is evaluated in long, so a UMV 4096A source contributes its full
     * 549,755,813,888 EU/t rather than a wrapped one.
     */
    public long getLaserCapacity() {
        long capacity = 0;
        for (final MTEHatch hatch : GTUtility.validMTEList(mExoticDynamoHatches)) {
            capacity += hatch.maxAmperesOut() * hatch.maxEUOutput();
        }
        return capacity;
    }

    /** Laser Source Hatches currently registered in the manifold. */
    public int getLaserHatchCount() {
        int fitted = 0;
        for (final MTEHatch ignored : GTUtility.validMTEList(mExoticDynamoHatches)) {
            fitted++;
        }
        return fitted;
    }

    /**
     * Laser sources that are PAINTED. TecTech only fires a laser link from a coloured source
     * hatch to a matching-colour Laser Target Hatch, so an unpainted source silently buffers
     * every EU it is handed - the classic "the multi says it generates, nothing arrives".
     */
    public int getLinkedLaserHatchCount() {
        int linked = 0;
        for (final MTEHatch hatch : GTUtility.validMTEList(mExoticDynamoHatches)) {
            final IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base != null && base.getColorization() >= 0) linked++;
        }
        return linked;
    }

    /**
     * D-25: THE PLANT'S OWN OUTPUT DISTRIBUTOR. This is why the manifold no longer routes a
     * single EU through {@code MTEMultiBlockBase.addEnergyOutputMultipleDynamos}.
     * <p>
     * <b>The defect in GT's version</b> (confirmed against
     * {@code GT5-Unofficial-5.09.52.594-dev.jar} with {@code javap -c}, not from the source's
     * apparent shape). Two thirds of that method are already long-safe and are NOT the problem:
     * <ul>
     * <li>the CAPACITY SUM is a long local - {@code lconst_0/lstore 5}, then
     * {@code maxEUOutput():J * maxAmperesOut():J} accumulated with {@code lmul}/{@code ladd};</li>
     * <li>the EXPLODE GUARD is a long compare - offsets 190-194 {@code lload 5; lload_1; lcmp;
     * iflt}, branching to {@code explodeMultiblock()} when summed capacity &lt; requested.</li>
     * </ul>
     * The real fault is the DISTRIBUTED-SO-FAR accumulator, local 4, which is an
     * <b>int</b> ({@code iconst_0; istore 4}). Both distribution loops recompute it in long and
     * then throw the top half away every single iteration - offsets 491-502:
     * {@code iload 4; i2l; lload 12; iload 16; i2l; lmul; ladd; l2i; istore 4}. It is read back
     * at offsets 246-251 / 413-418 as {@code remaining = requested - distributed}
     * ({@code lload_1; iload 4; i2l; lsub}). So the moment cumulative distribution passes
     * 2^31-1 the counter wraps NEGATIVE, {@code remaining} becomes LARGER than the original
     * request, and every hatch after that point is over-filled. It is also the basis of the
     * return value ({@code iload 4; ifle}).
     * <p>
     * <b>Which is why the old 2^31-1 clamp could not simply be widened, and why CHUNKING IS NOT
     * A FIX.</b> Splitting the injection into several sub-2^31 calls would let EACH call walk
     * the hatch row from the top and push every hatch to its full {@code maxAmperesOut} again -
     * delivering several times the fitted rating per tick. The clamp guarded a real defect; it
     * just never named the right one. What follows is GT's algorithm minus the narrowing.
     * <p>
     * <b>Over-injection: TWO behaviours, one switch.</b> GT detonates the whole multiblock when
     * it is handed more than the hatches can take, and in NORMAL PLAY so do we - same trigger
     * (summed capacity &lt; requested), same consequence, full parity with the method this
     * replaces. Nothing about the destructiveness is softened. With
     * {@link #isDebugOutputSafety() output safety} on - the bench switch, {@code /btssafety} -
     * the plant is FLAGGED instead of destroyed: the excess is clamped away, the plant keeps
     * running, and {@link #outputSafetyLine} prints the EU offered, the summed capacity, the
     * shortfall and which hatch row came up short, on every panel, until the mode is changed.
     * <p>
     * Note what arms the guard in practice. {@code onRunningTick} clamps to
     * {@link #outputLimit()} before calling here, and {@code outputLimit() <= outputCeiling()}
     * IS the summed capacity - so on correct arithmetic the branch is unreachable and an armed
     * plant is in no more danger than it was under GT. It fires on an arithmetic slip in OUR
     * code, which is exactly the case it is there to catch and exactly the case the bench
     * switch exists to let someone investigate without losing the plant.
     * <p>
     * <b>Mixed voltages stay legal.</b> GT's other explode branch fires when hatches of
     * differing voltage are mixed, but only when its second argument is {@code false}; this
     * plant has always passed {@code true}, so that branch has been off here since day one.
     * Implementing it now would be a pure REGRESSION - and a targeted one, because the manifold
     * is designed around exactly the mix it would kill: an IV dynamo row at y3 and a UMV laser
     * source row at y5.
     * <p>
     * Every local on this path is a {@code long}. There is no {@code Math.min} against
     * {@code Integer.MAX_VALUE} anywhere in it.
     *
     * @param request EU to place this tick, already clamped to {@link #outputLimit()}.
     * @return EU actually handed to the hatch row - equal to {@code request} in every case
     *         where the fitted hatches can absorb it.
     */
    private long distributeOutput(final long request) {
        if (request <= 0) return 0L;
        // Our OWN capacity check, in long, standing where GT's explode guard stood. Same two
        // lists, same tick, read immediately before we walk them.
        final long dynamoCap = getDynamoCapacity();
        final long laserCap = getLaserCapacity();
        final long capacity = dynamoCap + laserCap;
        if (request > capacity) {
            recordOutputOverrun(request, capacity, dynamoCap, laserCap);
            // NORMAL PLAY: GT parity, and deliberately so. addEnergyOutputMultipleDynamos
            // detonates a plant that is handed more EU than its hatches can absorb; that is
            // intended behaviour here too and is not softened. Only the bench switch disarms it.
            if (!debugOutputSafety) {
                explodeMultiblock();
                return 0L;
            }
            // TEST MODE: keep the plant standing and let the flag do the talking.
        }
        long remaining = Math.min(request, capacity);
        if (remaining <= 0) return 0L;
        long distributed = 0L;
        // Dynamos first, then the exotic/laser row - GT's order, so a plant that already
        // balances across a mixed manifold keeps filling it in the same sequence.
        for (final MTEHatchDynamo dynamo : GTUtility.validMTEList(mDynamoHatches)) {
            final long given = feedDynamoHatch(dynamo, remaining);
            distributed += given;
            remaining -= given;
            if (remaining <= 0) return distributed;
        }
        for (final MTEHatch laser : GTUtility.validMTEList(mExoticDynamoHatches)) {
            final long given = feedDynamoHatch(laser, remaining);
            distributed += given;
            remaining -= given;
            if (remaining <= 0) return distributed;
        }
        return distributed;
    }

    /**
     * The last over-injection, latched with its numbers, plus a rate-limited server-log entry.
     * <p>
     * Called on BOTH sides of the switch - an armed plant records before it detonates, so the
     * log still says what killed it and with which figures. In test mode the latch is what the
     * panels read, and it survives until {@link #setDebugOutputSafety} is used to change mode:
     * a flag that cleared itself the moment the plant recovered would be exactly as useless as
     * no flag at all.
     * <p>
     * The log is once per {@value #OVERRUN_LOG_TICKS} ticks, not once per tick. A plant in this
     * state is in it every tick, and an unthrottled ERROR would put 72,000 identical lines in
     * the log per minute and bury the first one - which is the only one anybody needs.
     */
    private void recordOutputOverrun(final long offered, final long capacity, final long dynamoCap,
        final long laserCap) {
        outputOverruns++;
        outputOverrunOffered = offered;
        outputOverrunCapacity = capacity;
        outputOverrunDynamo = dynamoCap;
        outputOverrunLaser = laserCap;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final long now = base == null || base.getWorld() == null ? 0L
            : base.getWorld()
                .getTotalWorldTime();
        if (outputOverruns > 1 && now - outputOverrunLogTick < OVERRUN_LOG_TICKS) return;
        outputOverrunLogTick = now;
        BeyondTheStars.LOG.error(
            "[Nuclear Plant Foundation @ {},{},{}] OUTPUT OVER-INJECTION #{}: offered {} EU/t into a hatch row that can absorb {} EU/t (dynamos {} + lasers {}); short by {} EU/t - {}. Mode: {}.",
            base == null ? "?" : base.getXCoord(),
            base == null ? "?" : base.getYCoord(),
            base == null ? "?" : base.getZCoord(),
            outputOverruns,
            offered,
            capacity,
            dynamoCap,
            laserCap,
            offered - capacity,
            shortRowName(dynamoCap, laserCap),
            debugOutputSafety ? "OUTPUT SAFETY (clamped, plant kept running)" : "ARMED (plant exploded)");
    }

    /** Which hatch row is the shortfall, in the words the panel and the log both use. */
    public static String shortRowName(final long dynamoCap, final long laserCap) {
        if (dynamoCap <= 0 && laserCap <= 0) return "NEITHER row has a hatch in it";
        if (laserCap <= 0) return "the LASER SOURCE row (y5) is empty";
        if (dynamoCap <= 0) return "the DYNAMO row (y3) is empty";
        return "both rows together are too small";
    }

    /**
     * One hatch, once per tick, never above its own rating.
     * <p>
     * {@code maxEUOutput() * maxAmperesOut()} IS the hatch's per-tick allowance, and because
     * {@link #distributeOutput} visits each hatch exactly once, taking the whole allowance in a
     * single deposit spends the amp budget exactly once - it cannot be re-spent the way a
     * chunked caller would re-spend it.
     * <p>
     * The single bulk deposit is also equivalent to GT's per-amp loop rather than merely close
     * to it. {@code BaseMetaTileEntity.increaseStoredEnergyUnits(long, boolean)} is
     * {@code if (getStoredEU() < getEUCapacity() || ignore) setStoredEU(getEUVar() + energy)} -
     * an "is there ANY room" gate, and {@code setStoredEU} clamps only negatives, never the
     * ceiling. So one deposit of N*V lands precisely when N deposits of V would, and it costs
     * one interface call instead of 4096 for a UMV 4096A source. {@code false} is GT's own
     * second argument ({@code iconst_0} at offsets 311 and 478) and is kept.
     * <p>
     * Like GT, the hatch's per-tick allowance is counted as SPENT whether or not the buffer had
     * room for it, so a momentarily full hatch cannot make the plant's headline EU/t figure
     * flicker. The one case where that matters - an UNPAINTED laser source silently buffering
     * everything it is handed - already has its own dedicated readout in {@code laserLine()},
     * which names the real cause instead of showing a mystery dip.
     *
     * @return the EU handed to this hatch, never more than {@code remaining}.
     */
    private static long feedDynamoHatch(final MTEHatch hatch, final long remaining) {
        if (remaining <= 0) return 0L;
        // Both getters return long in GT; the product is evaluated in long.
        final long perTick = hatch.maxEUOutput() * hatch.maxAmperesOut();
        if (perTick <= 0) return 0L;
        final IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
        // validMTEList already excludes dead hatches, but a base can still be absent while a
        // chunk is unloading and increaseStoredEnergyUnits is an interface call on it.
        if (base == null) return 0L;
        final long share = Math.min(remaining, perTick);
        base.increaseStoredEnergyUnits(share, false);
        return share;
    }

    /**
     * The most EU/t this manifold may be handed in one tick: everything the fitted hatches can
     * absorb. No clamp. Every consumer of this figure - the GUI, both control blocks, the
     * scanner, the MATH panel's ceiling and CLAMPED lines, {@code /btsbuild} - takes it as a
     * long and always did.
     * <p>
     * D-25: this used to be {@code Math.min(dynamos + lasers, Integer.MAX_VALUE)}, on the
     * stated grounds that {@code addEnergyOutputMultipleDynamos} accumulates its injected total
     * in an int. <b>The stated reason was wrong, and it is worth being precise about why so the
     * clamp does not come back.</b> That method's capacity sum and its explode guard are both
     * fully long ({@code lstore 5}, {@code lcmp}); the int is its DISTRIBUTED-SO-FAR counter,
     * which wraps negative past 2^31-1 and makes it over-deliver. The clamp was a real
     * safeguard against a real bug - just not the one on the label, which is exactly why it
     * could not be widened or chunked away. {@link #distributeOutput} now does the injection in
     * long, so nothing on this path narrows and the ceiling is free to tell the truth.
     * <p>
     * Long overflow on the sum is unreachable: the manifold has four dynamo sockets and four
     * laser sockets, and eight hatches at the highest tier GT defines cannot approach
     * {@code Long.MAX_VALUE}. The setup that prompted this - four UMV 4096A laser sources -
     * is 4 x 134,217,728 x 4096 = 2,199,023,255,552 EU/t, about 0.00002% of a long.
     */
    public long outputCeiling() {
        return getDynamoCapacity() + getLaserCapacity();
    }

    // -------------------------------------------------- D-17 SWITCHYARD: the grid soft cap

    /**
     * D-17 6: the output a plant with NO SWITCHYARD may put on the grid. Everything the reactor
     * derives above this is discarded until the transformer yard stands.
     * <p>
     * This is a LIVE BALANCE CHANGE and is treated as one: {@link Config#switchyardSoftCap} is
     * the single setting that turns it off, every panel that can print an output figure also
     * prints why the figure is short and what to build, and the cap is applied by LOWERING WHAT
     * IS INJECTED - never by raising a ceiling. That last part is not cosmetic: the number
     * handed to the hatch row has to stay {@code <= outputCeiling()} in every branch, or the
     * plant would be trying to push EU into hatches that cannot take it.
     * {@link #outputLimit()} is a {@code Math.min} of the two for exactly that reason and is the
     * ONLY thing onRunningTick clamps to.
     * <p>
     * D-25: this used to say GT's {@code addEnergyOutputMultipleDynamos} would call
     * {@code explodeMultiblock()} over an over-injection. It would have - but this plant no
     * longer calls it, and {@link #distributeOutput} deliberately REFUSES rather than
     * detonating (see its javadoc). The invariant above is unchanged and still worth keeping:
     * it is now enforced by clamping, in two independent places, instead of by an explosion.
     * <p>
     * The cap is a plain {@code long} and is unaffected by the ceiling now running past 2^31:
     * 100,000,000 stays far below a four-UMV-laser ceiling of 2,199,023,255,552, so an
     * un-Switchyarded plant is still held to the same 100M EU/t it always was.
     */
    public static final long GRID_SOFT_CAP = 100_000_000L;

    /** True while the cap is armed (config on) and unrelieved (no Switchyard standing). */
    public boolean isGridCapActive() {
        return Config.switchyardSoftCap && !switchyardBuilt;
    }

    /**
     * The CONFIG flag on its own, for the panel. It is synced rather than read from
     * {@link Config} on the client, because a Forge config is per-side: a dedicated server and
     * its client can disagree about it, and the panel has to describe the SERVER's plant.
     */
    public boolean isGridCapEnabled() {
        return Config.switchyardSoftCap;
    }

    /** What the GRID will take: the soft cap, or everything once a Switchyard conditions it. */
    public long gridCeiling() {
        return isGridCapActive() ? GRID_SOFT_CAP : Long.MAX_VALUE;
    }

    /** Hatch ceiling AND grid ceiling folded into one number. Never above outputCeiling(). */
    public long outputLimit() {
        return Math.min(outputCeiling(), gridCeiling());
    }

    /** True when the SWITCHYARD's absence - not the fitted hatches - is what is costing EU. */
    public boolean isGridCapped() {
        return Math.min(lastEUDemand, outputCeiling()) > gridCeiling();
    }

    /** The RPM model's pre-clamp figure on the last generating tick. */
    public long getLastEUDemand() {
        return lastEUDemand;
    }

    /** True when the ceiling, not the reactor, is what limits the plant's output. */
    public boolean isOutputClamped() {
        return lastEUDemand > lastEUOutput;
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
        return coolantOfRung(index, litres);
    }

    private static final int[] CHAMBER_COOLANT_Q = { 160_000, 40_000, 12_000, 4_000, 1_500, 1_000 };
    private static final String[] CHAMBER_COOLANT_NAMES = { "Tachyon Rich Temporal Fluid", "Liquid Cryotheum",
        "Super Coolant", "IC2 Coolant", "Distilled Water", "Water" };
    /** Residual-swirl turbulence: SD-2's k1 x |S|^2 heat per SECOND while running. */
    private static final int SWIRL_HEAT_PER_SECOND = 5;
    /** Ticks of empty injectors before the plant counts as DRY (longer than a cycle span). */
    private static final int PROPELLANT_DRY_TICKS = 100;

    /**
     * The chamber's cooling load per tick at a given fan speed, in heat units: the ruling's
     * RPM-squared term plus the Spine's residual-swirl bias q_spine (SD-3: q = k2 x V^2 +
     * q_spine). A COOLED chamber carries the turbulence away with the coolant; an uncooled one
     * takes it as heat instead (see onPostTick), which is what "all-riding patterns run hot"
     * has always meant.
     */
    private int chamberLoad(int rpmMilliValue) {
        return chamberLoadFor(rpmMilliValue, spineSwirl);
    }

    private static int coolantLitresFor(int load, int rung) {
        return Math.max(1, (int) Math.ceil(load * 1000.0 / CHAMBER_COOLANT_Q[rung]));
    }

    /**
     * Feedwater for the reactor, drawn from the SILO's own Feedwater Input Hatch ONLY. The
     * chamber's coolant line and the bulk propellant line land in the same
     * {@code mInputHatches} pool, so a blanket {@code depleteInput()} would happily drink
     * water-grade coolant out from under the chamber. While the bench-supply flag is on the
     * draw is WAIVED in code rather than by filling tanks - a filled hatch can be pumped back
     * out, which would have made a test plant an unlimited water/coolant source.
     * <p>
     * WHAT counts as feedwater is {@link #drainFeedwaterLadder} and is deliberately not written
     * here (B7). This method only turns the rung that ladder took into the OUTPUT term.
     */
    private boolean drawFeedwater(int litres) {
        if (debugInfiniteSupplies) {
            // The bench WAIVES the draw, so there is nothing standing in a hatch to grade. Park
            // the term at the baseline rather than leaving the last grade seen: a test plant must
            // not go on being paid a x1.40 for water it is no longer drinking.
            feedwaterGrade = BTSWaterGrade.NOT_GRADED;
            return true;
        }
        // The RUNNING draw never takes IC2 Coolant: runFluidTick offers coolant its own branch
        // before this one and pays HOT COOLANT for it, so drinking it here would silently
        // convert a Hot Coolant line into a steam line.
        final int rung = drainFeedwaterLadder(litres, false);
        feedwaterGrade = rung == NO_FEEDWATER ? BTSWaterGrade.NOT_GRADED : rung;
        return rung != NO_FEEDWATER;
    }

    /** {@link #drainFeedwaterLadder} drew nothing: the silo's row holds no accepted fluid. */
    private static final int NO_FEEDWATER = -2;

    /**
     * THE ACCEPTED-FEEDWATER LADDER, WRITTEN ONCE. Worst rung first, all-or-nothing per rung,
     * out of the SILO's own hatches; returns the rung it drained - {@link BTSWaterGrade#NOT_GRADED}
     * for plain water (and for IC2 Coolant, which has no grade), {@link BTSWaterGrade#DISTILLED}
     * for distilled, 1..8 for the purified grades - or {@link #NO_FEEDWATER} when nothing in the
     * row would answer.
     * <p>
     * <b>B7 - why this is ONE method and not two lists.</b> The running draw and the
     * post-shutdown decay-heat obligation each used to carry their own idea of what cooling water
     * is, and D-26 widened only the running one. A plant built to the exact spec this release
     * REWARDS - one Grade 4 Purified Water hatch and nothing else - therefore ran perfectly, and
     * then could not pay its decay heat: a guaranteed meltdown handed to the player who followed
     * the tooltip. Both callers now read this ladder, so the two sets cannot drift apart again.
     * <p>
     * <b>Why offering ten fluids a tick is cheap.</b> {@code MTEHatchInput} inherits
     * {@code drain(ForgeDirection, FluidStack, boolean)} from {@code CommonMetaTileEntity}; its
     * bytecode forwards to {@code drain(dir, stack, stack.amount, doDrain)}, which returns null
     * unless {@code getFluid().isFluidEqual(want)}. A rung the row does not hold costs one
     * comparison and draws nothing - which is also precisely why the old three-fluid decay list
     * came up dry against a purified hatch instead of falling back to it.
     * <p>
     * WORST FIRST is the mechanic and not an optimisation: a row holding plain water and Grade 8
     * spends the plain water, so a purified feedwater line only ever pays the OUTPUT term when it
     * is a DEDICATED line. IC2 Coolant sits between distilled and Grade 1 on the same principle -
     * it is cheaper than purified water and dearer than distilled - and is offered only to the
     * caller that passes {@code acceptCoolant}.
     */
    private int drainFeedwaterLadder(int litres, boolean acceptCoolant) {
        if (drainFrom(feedwaterHatches, Materials.Water.getFluid(litres), false)) {
            return BTSWaterGrade.NOT_GRADED;
        }
        if (drainFrom(feedwaterHatches, GTModHandler.getDistilledWater(litres), false)) {
            return BTSWaterGrade.DISTILLED;
        }
        if (acceptCoolant && drainFrom(feedwaterHatches, GTModHandler.getIC2Coolant(litres), false)) {
            return BTSWaterGrade.NOT_GRADED;
        }
        for (int grade = 1; grade <= BTSWaterGrade.MAX_GRADE; grade++) {
            final FluidStack rung = BTSWaterGrade.fluidFor(grade, litres);
            // A rung whose fluid is missing SKIPS rather than throws: this runs every tick of
            // every running plant, and one absent material would otherwise be a crash loop.
            if (rung != null && drainFrom(feedwaterHatches, rung, false)) {
                return grade;
            }
        }
        return NO_FEEDWATER;
    }

    /**
     * B7: the POST-SHUTDOWN decay-heat draw - every fluid the plant was allowed to drink while it
     * ran, plus IC2 Coolant. The obligation is paid out of the same water line that was feeding
     * the core a tick earlier, so it must not be fussier than that line was.
     * <p>
     * The bench supply waives it exactly as it waives the running draw. That is the same defect
     * in miniature: {@code refillDebugSupplies} deliberately does NOT materialise feedwater (a
     * filled hatch can be pumped back out), so a commanded plant whose running draw is waived
     * would still have melted down the moment it was switched off, with nothing a player could
     * put in a hatch to stop it. The DESTRUCTION policy is untouched - the window still runs, the
     * heat still climbs from every other source, and turning {@code /btssupply} off arms this
     * draw again on the next tick.
     */
    private boolean drawDecayHeatCooling(int litres) {
        if (debugInfiniteSupplies) return true;
        return drainFeedwaterLadder(litres, true) != NO_FEEDWATER;
    }

    /**
     * D-26 THE WATER OUTPUT TERM, as the plant is actually drinking it right now.
     * <p>
     * <b>AXIS 3, and this is precisely the axis-3 failure mode.</b> The grade is set inside
     * {@link #drawFeedwater}, which reads {@code feedwaterHatches} - the SILO's own private list -
     * and nothing else. The Spiral chamber's {@code chamberHatches} may perfectly legitimately
     * hold Grade 8 purified water as COOLANT, and it must never pay the reactor's output term. A
     * blanket sweep of {@code mInputHatches} would have handed every plant with good coolant a
     * free x1.40.
     * <p>
     * <b>AXIS 2:</b> nothing is persisted. The grade is re-derived from what is standing in the
     * hatch on every generating tick, so it cannot survive the water being swapped out.
     */
    public int waterOutputMilli() {
        return BTSWaterGrade.outputMilli(feedwaterGrade);
    }

    /** The feedwater rung the last draw actually took: -1 none/plain, 0 distilled, 1..8 purified. */
    public int getFeedwaterGrade() {
        return feedwaterGrade;
    }

    /**
     * Draws coolant from the chamber's dedicated feed - D-20 puts that back in the Spiral's
     * own front wall, and the semantics are the SD-3 ones exactly: a private list, so the
     * reactor's feedwater can never answer "can the chamber be cooled?" and then be drunk as
     * coolant, starving the reactor it was meant to feed.
     */
    private boolean drainChamber(FluidStack want, boolean simulate) {
        return drainFrom(chamberHatches, want, simulate);
    }

    /**
     * All-or-nothing draw across one dedicated hatch list: simulate first across the whole
     * list, and only commit when the full amount is actually there (a partial drain would
     * consume fluid for nothing).
     */
    private boolean drainFrom(List<MTEHatchInput> hatches, FluidStack want, boolean simulate) {
        if (want == null || want.getFluid() == null) return false;
        if (hatches.isEmpty()) return false;
        int left = want.amount;
        for (final MTEHatchInput hatch : GTUtility.validMTEList(hatches)) {
            final FluidStack got = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(want.getFluid(), left), false);
            if (got != null) left -= got.amount;
            if (left <= 0) break;
        }
        if (left > 0) return false;
        if (simulate) return true;
        left = want.amount;
        for (final MTEHatchInput hatch : GTUtility.validMTEList(hatches)) {
            final FluidStack got = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(want.getFluid(), left), true);
            if (got != null) left -= got.amount;
            if (left <= 0) return true;
        }
        return false;
    }

    /**
     * A2 INTERNAL PROPELLANT DISTRIBUTION. Pulls one litre of any recognised ladder propellant
     * out of the chamber's own Propellant Input Hatch. The unknown-fluid guard matters: an
     * injector's
     * reserve is set directly (never through {@code fill()}, which would stamp it as a timed
     * injection), so anything that got in there would be treated as propellant.
     */
    private FluidStack drainBulkPropellant() {
        for (final MTEHatchInput hatch : GTUtility.validMTEList(propellantHatches)) {
            final FluidStack stored = hatch.getFluid();
            if (stored == null || stored.getFluid() == null || stored.amount < 1) continue;
            if (propellantMilliOf(stored) <= 1000) continue;
            final FluidStack got = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(stored.getFluid(), 1), true);
            if (got != null && got.amount >= 1) return new FluidStack(got.getFluid(), 1);
        }
        return null;
    }

    /**
     * Tops every Precision Injector's one-litre reserve up from the chamber's bulk propellant
     * line,
     * un-stamped, so the controller can land a perfectly timed shot when a cycle is accepted.
     * <p>
     * THE TIMING RULING IS INTACT: a CYCLE still only happens on a redstone rising edge at the
     * Turbine Control Block, and how many injectors count as TIMED is still decided per cycle -
     * by how many of them this line managed to stock before the edge arrived. What changed is
     * only WHO presses the eight buttons. An injector that already carries timing stamps is
     * never touched: a player hand-timing a litre the old way still owns that injector, and the
     * plant will not add a second stamp on top of theirs (two litres in one window is a miss).
     */
    private int feedInjectors() {
        // The bench supply owns the injectors while it is on - stocking them twice would land
        // two stamps in the same window and score every cycle as eight wasted litres.
        if (debugInfiniteSupplies || spiralInjectors.isEmpty() || propellantHatches.isEmpty()) return 0;
        int fed = 0;
        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
            if (injector == null || !injector.isValid()) continue;
            if (injector.hasTimingStamps()) continue;
            if (injector.storedLitres() > 0) {
                // A litre left over from a refused or missed cycle: adopt it rather than
                // stranding it - otherwise that injector would never fire again.
                injector.adoptReserve();
                continue;
            }
            final FluidStack drawn = drainBulkPropellant();
            if (drawn == null) break;
            if (injector.stockReserve(drawn)) fed++;
        }
        return fed;
    }

    /** Injectors holding a litre RIGHT NOW - the census term of the TC-7 dry test. */
    private int loadedInjectorCount() {
        int loaded = 0;
        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
            if (injector != null && injector.isValid() && injector.storedLitres() > 0) loaded++;
        }
        return loaded;
    }

    /**
     * THE dry test, in ONE place: nothing is loaded, AND nothing has been loaded for longer
     * than a cycle span. A port is empty between shots in skilled play, which is why the clock
     * term exists - and why "is there fluid in the bulk hatch" is a different question that
     * nothing is allowed to substitute for it.
     */
    private boolean injectorsDryAt(int loadedInjectors, long now) {
        return loadedInjectors == 0 && now - lastPropellantTick > PROPELLANT_DRY_TICKS;
    }

    /**
     * The best ladder rung that could cover this load RIGHT NOW, consuming nothing (-1 = the
     * plant cannot cool that much). This is the TC-7 interlock's oracle: the fan is never
     * allowed to accelerate past the speed the chamber can actually pay for.
     */
    private int coolantRungAvailable(int load) {
        for (int i = 0; i < CHAMBER_COOLANT_Q.length; i++) {
            final FluidStack coolant = chamberCoolantStack(i, coolantLitresFor(load, i));
            if (coolant != null && (debugInfiniteSupplies || drainChamber(coolant, true))) return i;
        }
        return -1;
    }

    /**
     * The RPM the plant would reach if the cycle about to start went PERFECTLY: every fitted
     * injector timed, on the best propellant it has. Deliberately pessimistic - the interlock
     * must guarantee cooling for the speed it is about to ALLOW, not for the speed it expects.
     */
    private int projectedRpmMilli(int propMilli) {
        final int timed = Math.min(GALLERY_STAGES, Math.max(1, spiralInjectors.size()));
        final long gain = (long) RPM_GAIN * Math.max(1000, propMilli) * timed / 8 * rpmGainMilli() / 1000L;
        return (int) Math.min(redlineMilli(), rpmMilli + gain);
    }

    /**
     * TC-3 turbine runtime, each running tick: score a due cycle window, put a pending
     * redstone-requested cycle THROUGH THE TC-7 INTERLOCK (it may only be accepted when the
     * chamber can cool the RPM it would reach and the injectors have something to burn), and
     * drink coolant scaled with the SQUARE of the fan RPM. Losing coolant at speed TRIPS the
     * fan into a protective spin-down instead of cooking it - a plant whose supplies are not
     * plumbed yet simply never spins up, and never seizes. No redstone clock = RPM decays.
     */
    private void runTurbineTick() {
        if (!spiralBuilt) {
            // No chamber at all: nothing carries the Spine's turbulence away (SD-2 interim).
            chamberCooled = false;
            chamberStarved = false;
            return;
        }
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

        // A2: the chamber's bulk propellant line refills the 8 injector reserves BEFORE the
        // census, so "how many injectors are loaded" already reflects what the plant managed
        // to distribute this tick.
        feedInjectors();

        // Propellant census. A port is EMPTY between shots in skilled play (the litre arrives
        // inside the window), so "dry" is only declared after PROPELLANT_DRY_TICKS with
        // nothing anywhere - never from the single tick a cycle happens to be requested on.
        int loadedInjectors = 0;
        int bestPropMilli = 0;
        FluidStack bestStack = null;
        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
            if (injector == null || !injector.isValid() || injector.storedLitres() <= 0) continue;
            loadedInjectors++;
            final int p = propellantMilliOf(injector.storedPropellant());
            if (p > bestPropMilli) {
                bestPropMilli = p;
                bestStack = injector.storedPropellant();
            }
        }
        if (bestStack != null) {
            lastPropellantTick = now;
            propellantName = bestStack.getLocalizedName();
            // Keep the displayed P with the fluid it came from: pairing this name with the
            // LAST cycle's factor mislabels the moment a player swaps propellant.
            propellantMilli = bestPropMilli;
        } else if (now - lastPropellantTick > PROPELLANT_DRY_TICKS) {
            propellantName = "none";
            propellantMilli = 0;
        }

        // Score the last accepted cycle one tick AFTER its +/-2t window closes: every
        // achievable in-window fill was recorded on a fully completed earlier tick, so
        // scoring never depends on tile-entity tick order (the SD-3 deferral pattern).
        if (cycleEvalTick != 0 && now >= cycleEvalTick) {
            evaluateCycle(cycleEvalTick - 3, cycleHadSteam);
            cycleEvalTick = 0;
        }

        // Accept a pending redstone cycle: needs the turbine gen formed, a free window slot,
        // and now the TC-7 INTERLOCK's blessing. A slugless cycle is still MISSED per the
        // ruling - its litres burn at scoring time, no RPM is gained and the Sensor stays
        // silent - but a cycle that CANNOT be cooled, or has nothing to burn, is never
        // started at all: no steam drunk, no propellant burned, no heat, and a stated reason.
        if (pendingCycle && cycleEvalTick == 0 && galleryBuilt && now - lastCycleTick >= 5) {
            final boolean dry = injectorsDryAt(loadedInjectors, now);
            final int projected = projectedRpmMilli(Math.max(bestPropMilli, lastCyclePropMilli));
            final int projectedLoad = chamberLoad(projected);
            final long slug = CYCLE_SLUG_TICKS * steamPerTick;
            if (dry || coolantRungAvailable(projectedLoad) < 0) {
                pendingCycle = false;
                turbineHoldReason = dry
                    ? "injectors are dry - fill the Spiral chamber's Propellant Input Hatch; no cycle was started"
                    : "coolant cannot cover " + String.format("%,d", projected / 1000)
                        + " RPM (needs "
                        + String.format("%,d", coolantLitresFor(projectedLoad, CHAMBER_COOLANT_Q.length - 1))
                        + " L/t of water, less of a better coolant) - top up the Spiral chamber's Coolant Input Hatch";
                // The refused cycle never opens a window, so stamps recorded around it must
                // not be scored as "mistimed" by whatever cycle eventually runs: drop the
                // timing, keep the fluid.
                for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                    if (injector != null && injector.isValid()) injector.drainEvents();
                }
            } else {
                pendingCycle = false;
                lastCycleTick = now;
                cycleHadSteam = slug > 0 && steamBuffer >= slug;
                if (cycleHadSteam) {
                    steamBuffer -= slug;
                    recentCycles.addLast(now);
                    if (spineSensor != null && spineSensor.isValid()) {
                        spineSensor.pulse(2);
                    }
                    turbineHoldReason = "";
                    if (debugInfiniteSupplies) {
                        // TEST AID: the debug supply is the perfect operator - exactly 1L per
                        // injector, stamped inside the window it just opened.
                        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                            if (injector != null && injector.isValid()) injector.debugTimedShot();
                        }
                    } else {
                        // A2: and in normal play the PLANT is that operator, for every reserve
                        // the chamber's propellant line managed to stock. Injectors a player is
                        // hand-timing are not auto-stocked and are left strictly alone, so the
                        // two styles never double-stamp the same window.
                        for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                            if (injector != null && injector.isValid()) injector.autoTimedShot();
                        }
                    }
                } else {
                    turbineHoldReason = "steam buffer short of a " + String.format("%,d", slug)
                        + " L slug - cycle missed (clock faster than the reactor)";
                }
                cycleEvalTick = now + 3;
            }
        }

        // Coolant drain: ceil(load / Q) per tick, load = 200 x (RPM/1000)^2 + swirl bias.
        final int load = chamberLoad(rpmMilli);
        if (load <= 0) {
            chamberCoolantDrain = 0;
            chamberCoolantName = "idle";
            chamberCooled = true;
            chamberStarved = false;
            return;
        }
        for (int i = 0; i < CHAMBER_COOLANT_Q.length; i++) {
            final int litres = coolantLitresFor(load, i);
            final FluidStack coolant = chamberCoolantStack(i, litres);
            if (coolant != null && (debugInfiniteSupplies || drainChamber(coolant, false))) {
                chamberCooled = true;
                chamberStarved = false;
                chamberCoolantDrain = litres;
                chamberCoolantName = debugInfiniteSupplies ? CHAMBER_COOLANT_NAMES[i] + " (bench supply)"
                    : CHAMBER_COOLANT_NAMES[i];
                return;
            }
        }
        // Coolant LOST (the interlock means it was there when this speed was authorised):
        // trip the fan and let it spin down fast rather than cook the rotors. Friction heat
        // still accrues at speed, so a chamber that was already near SCRAM can still seize -
        // but nothing seizes merely because the player has not plumbed the coolant yet.
        chamberCooled = false;
        chamberStarved = true;
        chamberCoolantDrain = 0;
        chamberCoolantName = "STARVED";
        if (rpmMilli > 0) {
            rpmMilli = (int) ((long) rpmMilli * 95 / 100);
            // Tidy B4: the old "protective spin-down" read as safe, and it is not - the friction
            // heat below can still seize the Spine near SCRAM heat (logged as L2).
            turbineHoldReason = "COOLANT LOST - spinning down; near SCRAM heat this can SEIZE the Spine";
        } else {
            turbineHoldReason = "no coolant in the Spiral chamber's Coolant Input Hatch - the turbine will not spin up";
        }
        // Only a TURNING fan generates friction heat - and only a turning fan can seize.
        // Testing the threshold at rest let a plant that was merely never plumbed destroy
        // its own Spine housings (and the rotors in them).
        if (rpmMilli > 0 && now % 20 == 10) {
            final double rpmNorm = rpmMilli / 1_000_000.0;
            heat += (int) Math.ceil(rpmNorm * rpmNorm) * 3;
            if (heat >= SCRAM_HEAT) {
                seize();
            }
        }
    }

    /**
     * Scores one cycle's +/-2t injection windows. Timed litres burn either way; RPM is
     * gained only when the cycle had its steam slug: gain = RPM_GAIN x P x timed/8 (P =
     * the LOWEST propellant factor among timed injectors), clamped at the redline.
     * <p>
     * R3 / TC-3: a MISSED cycle - one the clock requested faster than the reactor could raise
     * a slug for - burns the reserves the plant stocked for it anyway. Hand-timed litres are
     * charged by the scoring loop below (they carry stamps); the plant's own un-stamped
     * reserves are voided afterwards, so the same eight litres can never be re-used forever
     * and over-clocking costs propellant exactly as it did under hand timing.
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
        // R3: the over-clock penalty. Every reserve the plant itself stocked for THIS cycle is
        // burned even though no slug arrived - the shot was fired into an empty chamber. Runs
        // after the scoring loop, so a hand-timed litre (already consumed above, and never
        // auto-stocked) cannot be charged a second time.
        int voidedLitres = 0;
        if (!hadSteam) {
            for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                if (injector == null || !injector.isValid()) continue;
                if (injector.voidAutoReserve()) voidedLitres++;
            }
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
                // D-23 TURBINE 5/6: the gain and the redline move TOGETHER (equilibrium is
                // ~10.486 x gain), so a raised redline is one the plant can actually reach
                // instead of a ceiling it never touches. F4: BOTH are held to the overspeed the
                // operator authorised in the RPM LIMIT field, so a tier landing never moves
                // either one on its own.
                final long gainMilli = (long) RPM_GAIN * minPropMilli * good / 8 * rpmGainMilli() / 1000L;
                rpmMilli = (int) Math.min(redlineMilli(), rpmMilli + gainMilli);
                turbineHoldReason = "";
                // D-23 TURBINE: a PERFECT cycle - it had its steam slug AND every one of the
                // eight injectors landed exactly 1 L inside the +/-2t window. 7/8 scores
                // nothing, and a fast clock with no fuel scores nothing (no slug, no credit).
                if (good >= GALLERY_STAGES) progression.add(BTSMilestone.TURBINE, 1L);
            }
        } else {
            lastCyclePropMilli = 1000;
            if (wastedLitres > 0) {
                turbineHoldReason = "last cycle: " + wastedLitres
                    + " L mistimed - no injector landed exactly 1 L in the ±2 tick window";
            } else if (hadSteam) {
                // A2 made this the "the propellant line ran dry" message: with the line
                // plumbed the plant times its own shots, so an empty window means empty ports.
                turbineHoldReason = "last cycle: no propellant reached the injectors - fill the Spiral "
                    + "chamber's Propellant Input Hatch; RPM only decays";
            }
            // No steam slug: the accept path already said so ("clock faster than the reactor").
            // Overwriting it here with a propellant complaint sent players to the wrong hatch.
        }
        // R3: and say what the miss COST. The panel used to show a missed cycle as free, which
        // is exactly the impression the reused litres gave.
        if (!hadSteam && voidedLitres > 0) {
            turbineHoldReason = (turbineHoldReason.isEmpty() ? "cycle missed (clock faster than the reactor)"
                : turbineHoldReason) + " - " + voidedLitres + " L of propellant burned for nothing";
        }
    }

    /**
     * TEST AID (set by /btsbuild, NBT-persisted): pulses a turbine cycle every 20 ticks so a
     * freshly commanded plant spins up without wiring a redstone clock first. Redstone
     * remains the real mechanic - this is a bench clock, and the GUI says so.
     */
    private boolean debugAutoCycle;
    /**
     * TEST AID (set by /btsbuild, toggled with /btssupply, NBT-persisted): the plant's
     * consumables are refilled every tick - the silo's feedwater, the chamber's coolant and
     * the best propellant in all eight injectors - and each accepted cycle gets a perfectly
     * timed 1L shot per injector. A
     * commanded plant therefore runs indefinitely without any plumbing, while the real
     * mechanics (interlock, timing window, coolant ladder) stay exactly as ruled.
     */
    private boolean debugInfiniteSupplies;

    /**
     * D-25 OUTPUT SAFETY (toggled with {@code /btssafety}, NBT-persisted in TILE NBT only).
     * <p>
     * {@code false} - the DEFAULT and normal play - means the over-injection guard in
     * {@link #distributeOutput} is ARMED and behaves exactly as GT's
     * {@code addEnergyOutputMultipleDynamos} does: hand the hatch row more EU than it can
     * absorb and the plant explodes. {@code true} replaces the detonation with a loud,
     * numbered flag on every panel and clamps the excess away.
     * <p>
     * <b>Why this is its own switch and not {@code debugInfiniteSupplies}.</b> They answer
     * different questions and both directions of coupling break something real. Bench supplies
     * answer "where do my fuel, water and coolant come from" - an INPUT convenience. This
     * answers "does a mistake cost me the plant" - a DESTRUCTION policy. Fold them together and
     * (a) the armed path becomes effectively untestable, because the only way to drive output
     * high enough to reach it is with bench supplies, which would disarm it; and (b) a player
     * running a real supply chain while nervous about a mis-sized hatch row cannot get the net
     * without also cheating their inputs. Two concerns, two switches.
     * <p>
     * <b>AXIS 2 - deliberately NOT in {@code setItemNBT}.</b> This is M2's lesson exactly, in
     * mirror image: {@code btsLastBuiltTier} was a value that ARMED a destructive path and
     * carrying it across a wrench was the bug. This one DISARMS a destructive path, so carrying
     * it would let a controller arrive - out of a creative item, off a stack, from another
     * world - silently disarmed, which is precisely the "believes they are armed when they are
     * not" failure. A wrenched and replaced controller comes back in normal play, and because
     * {@link #outputSafetyLine} is printed unconditionally the reset is visible the first time
     * anybody opens a panel. It DOES survive a reload, because a test session surviving a relog
     * is the whole point of tile NBT.
     */
    private boolean debugOutputSafety;

    /** How many ticks the plant has been handed more EU than its hatch row can absorb. */
    private int outputOverruns;
    private long outputOverrunOffered;
    private long outputOverrunCapacity;
    private long outputOverrunDynamo;
    private long outputOverrunLaser;
    /** World time of the last server-log entry, so a stuck plant does not flood the log. */
    private long outputOverrunLogTick;
    /** 5 seconds. One log line per over-injection episode, not 20 per second. */
    private static final long OVERRUN_LOG_TICKS = 100L;

    public boolean isDebugAutoCycle() {
        return debugAutoCycle;
    }

    public void setDebugAutoCycle(boolean value) {
        debugAutoCycle = value;
    }

    public boolean isDebugInfiniteSupplies() {
        return debugInfiniteSupplies;
    }

    public void setDebugInfiniteSupplies(boolean value) {
        debugInfiniteSupplies = value;
    }

    public boolean isDebugOutputSafety() {
        return debugOutputSafety;
    }

    /**
     * Changing MODE clears the latched record; re-setting the same mode leaves it standing.
     * <p>
     * A test session starts clean, and re-arming a plant retires the flag with the mode that
     * produced it - which keeps the panel from reporting a shortfall against a plant that is no
     * longer being tested. Setting the mode it is already in is a no-op precisely so somebody
     * can type {@code /btssafety on} twice without wiping the numbers they were reading.
     */
    public void setDebugOutputSafety(boolean value) {
        if (debugOutputSafety != value) clearOutputOverrun();
        debugOutputSafety = value;
    }

    private void clearOutputOverrun() {
        outputOverruns = 0;
        outputOverrunOffered = 0;
        outputOverrunCapacity = 0;
        outputOverrunDynamo = 0;
        outputOverrunLaser = 0;
        outputOverrunLogTick = 0;
    }

    public int getOutputOverruns() {
        return outputOverruns;
    }

    public long getOutputOverrunOffered() {
        return outputOverrunOffered;
    }

    public long getOutputOverrunCapacity() {
        return outputOverrunCapacity;
    }

    public long getOutputOverrunDynamo() {
        return outputOverrunDynamo;
    }

    public long getOutputOverrunLaser() {
        return outputOverrunLaser;
    }

    /** Best propellant that actually exists in this pack, worst-case Benzene. */
    public static FluidStack bestPropellant(int litres) {
        final net.minecraftforge.fluids.Fluid[] ladder = { BTSFluids.naqPropellantT6, BTSFluids.naqPropellantT5,
            BTSFluids.naqPropellantT4, BTSFluids.naqPropellantT3, BTSFluids.naqPropellantT2, BTSFluids.naqPropellantT1,
            GTPPFluids.H8N4C2O4RocketFuel, Materials.GasolinePremium.mFluid, Materials.Benzene.mFluid };
        for (final net.minecraftforge.fluids.Fluid fluid : ladder) {
            if (fluid != null) return new FluidStack(fluid, litres);
        }
        return null;
    }

    /** Best chamber coolant that actually exists in this pack, worst-case plain Water. */
    public FluidStack bestCoolant(int litres) {
        for (int i = 0; i < CHAMBER_COOLANT_Q.length; i++) {
            final FluidStack stack = chamberCoolantStack(i, litres);
            if (stack != null && stack.getFluid() != null) return stack;
        }
        return Materials.Water.getFluid(litres);
    }

    /**
     * TEST AID: tops the plant's consumables back up. Feedwater (Water) goes into the silo's
     * own Feedwater hatch, the best coolant in the pack into the chamber's Coolant hatch, and
     * the best propellant into every Precision Injector - the injector reserve is
     * set WITHOUT a timing stamp, so it never reads as a bucket dumped into a window. Returns
     * {feedwater hatches, coolant hatches, injectors}.
     */
    public int[] refillDebugSupplies() {
        int feed = 0, cool = 0, inject = 0;
        // Feedwater and chamber coolant are NOT materialised: while the flag is on the draw
        // is simply waived in code (see drainChamber / the feedwater path). Filling those
        // tanks would have made the plant an unlimited tap for the pack's best coolant,
        // since a hatch's contents can be pumped straight back out.
        for (final MTEHatchInput ignored : GTUtility.validMTEList(feedwaterHatches)) {
            feed++;
        }
        for (final MTEHatchInput ignored : GTUtility.validMTEList(chamberHatches)) {
            cool++;
        }
        // The propellant DOES need to be a real litre - the slug scorer consumes and counts
        // it - but only ever one litre per injector per cycle, which is not a useful rate.
        final FluidStack propellant = bestPropellant(1);
        if (propellant != null) {
            for (final MTEHatchPrecisionInjector injector : spiralInjectors) {
                if (injector == null || !injector.isValid()) continue;
                injector.debugRefill(propellant);
                inject++;
            }
        }
        return new int[] { feed, cool, inject };
    }

    // ------------------------------------------------- plant envelope (for /btsbuild siting)

    /** Highest buildable tier. */
    public static int maxTier() {
        return MAX_TIER;
    }

    /** Edge of the square platform footprint, in blocks (the plant's a AND c extent). */
    public static int platformSize() {
        return PLATFORM;
    }

    /** Platform coordinate `a` of the controller: 25 cells of platform lie to its RIGHT. */
    public static int platformControllerA() {
        return CENTER;
    }

    /** Platform coordinate `c` of the controller: 17 cells of platform lie in FRONT of it. */
    public static int platformControllerC() {
        return SILO_FRONT;
    }

    /**
     * Blocks of headroom a tier-t plant needs above the controller's own level. The silo is
     * the tall piece - walls 2n plus an n/2 dome, minus the pad layer the controller stands
     * on - except at tier 1, where the 19-high cooling tower shells out-reach the 20-high
     * silo by a hair; take the larger. Tiers 1..5: 20, 25, 30, 35, 40.
     */
    public static int plantHeightFor(int tier) {
        final int t = Math.min(MAX_TIER, Math.max(1, tier));
        // D-22: the Vent Stack and the Extraction Derrick are 20 layers tall too, so they join
        // the max rather than being remembered separately. Tiers 1..5 are still 20, 25, 30, 35,
        // 40 - at tier 1 four different pieces now tie at 19-20 blocks of headroom.
        return Math.max(Math.max(siloVOff(t), TOWER_TOP), Math.max(VENT_HEIGHT - 1, DERRICK_HEIGHT - 1));
    }

    /** Top layer of the module tower shells, as passed to buildPiece in {@link #construct}. */
    private static final int TOWER_TOP = 19;

    /**
     * One-line census of what the last structure walk actually found - the /btsbuild
     * verification readout, so a per-tier build can be checked without opening five GUIs.
     */
    public String buildSummary() {
        return "towers " + coolingTowerCount()
            + "/2 | silo T"
            + (isSiloBuilt() ? reactorTier : 0)
            + " ("
            + getFuelColumns()
            + " columns, "
            + mInputBusses.size()
            + " rod bus + "
            + getFeedwaterHatchCount()
            + " feedwater) | spine "
            + (spineBuilt ? spineHousings + " housings" : "MISSING")
            + " | spiral "
            + (spiralBuilt
                ? spiralInjectors.size() + " injectors + "
                    + getPropellantHatchCount()
                    + " propellant + "
                    + getChamberCoolantHatchCount()
                    + " coolant"
                : "MISSING")
            + " | gallery "
            + (galleryBuilt ? galleryStages + "/" + GALLERY_STAGES + " stages" : "MISSING")
            + " | "
            + mDynamoHatches.size()
            + " dynamos + "
            + mEnergyHatches.size()
            + " energy + "
            + mExoticDynamoHatches.size()
            + " lasers | modules "
            + (ventStackBuilt ? "vent " : "")
            + (switchyardBuilt ? "yard " : "")
            + (derrickBuilt ? "derrick " : "")
            + (annexBuilt ? "annex " : "")
            + (ventStackBuilt || switchyardBuilt || derrickBuilt || annexBuilt ? "" : "none");
    }

    /** Turbine Housing cells this plant owns: the Spine's 4 rotor sockets + 8 gallery stages. */
    public List<int[]> housingCells() {
        final List<int[]> all = new ArrayList<>(spineHousingCells);
        all.addAll(stageCells);
        return all;
    }

    /**
     * Fills every Rod Loading Port of the formed silo with the given rod stack (test aid for
     * /btsbuild). Returns how many ports were stocked.
     */
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
            rotorWear = "";
            return;
        }
        final World world = getBaseMetaTileEntity().getWorld();
        int count = 0;
        int sumMilli = 0;
        // Per-stage condition for the readouts: wear points 0-9, '-' for an empty or missing
        // seat. Eight entries always, so stage N is always the Nth column.
        final StringBuilder wear = new StringBuilder();
        int listed = 0;
        for (final int[] cell : stageCells) {
            int damage = -1;
            if (world.blockExists(cell[0], cell[1], cell[2])) {
                final net.minecraft.tileentity.TileEntity te = world.getTileEntity(cell[0], cell[1], cell[2]);
                if (te instanceof TileTurbineHousing && ((TileTurbineHousing) te).hasRotor()) {
                    damage = ((TileTurbineHousing) te).getRotorDamage();
                }
            }
            if (damage >= 0) {
                count++;
                sumMilli += Math.max(0, 1000 - 50 * damage);
            }
            if (listed > 0) wear.append(' ');
            wear.append(damage < 0 ? "-" : String.valueOf(damage));
            listed++;
        }
        while (listed < GALLERY_STAGES) {
            if (listed > 0) wear.append(' ');
            wear.append('-');
            listed++;
        }
        galleryStages = count;
        avgRotorFactorMilli = sumMilli / GALLERY_STAGES;
        rotorWear = wear.toString();
    }

    /**
     * TC-4 wear, rolled once a second per seated stage rotor: chance = (RPM / redline) /
     * 600 - full tilt averages one wear point per 10 minutes, a shattered rotor (10
     * points) every ~100 minutes per stage. Like a plane turbine, every chip hurts.
     */
    private void rollRotorDamage() {
        if (rpmMilli <= 0 || stageCells.isEmpty()) return;
        final World world = getBaseMetaTileEntity().getWorld();
        // D-23 TURBINE 7: BLADE METALLURGY doubles the divisor - the same RPM chips a blade
        // half as often. Scored against the LIVE redline, so an overspeed-trimmed plant at its
        // new redline wears exactly as fast as a stock one at 12,000 did.
        final double chance = rpmMilli / (double) redlineMilli() / progression.rotorWearDivisor(ROTOR_WEAR_DIVISOR);
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

    /**
     * Fluid-export mode: no Rotor Assembly yet, so the reactor sells its steam instead. Both
     * draws come out of the silo's FEEDWATER hatch - putting IC2 Coolant in the reactor's own
     * water line is how you ask for Hot Coolant, and the chamber's coolant is never touched.
     */
    private void runFluidTick(int flowPerTick, int baseFlow) {
        final int coolantFlow = reactorTier >= MAX_TIER ? baseFlow * 2 : baseFlow;
        if (drainFrom(feedwaterHatches, GTModHandler.getIC2Coolant(coolantFlow), false)) {
            // D-26: this branch draws COOLANT, not feedwater, so it never reaches drawFeedwater
            // and would otherwise leave the last water grade standing on the panel over a draw
            // that is no longer happening.
            feedwaterGrade = BTSWaterGrade.NOT_GRADED;
            addOutput(GTModHandler.getHotCoolant(coolantFlow));
            lastEUOutput = 0;
            lastEUDemand = 0;
            emitWasteWater();
        } else if (drawFeedwater(flowPerTick)) {
            addOutput(Materials.Steam.getGas(spineBoostedSteam(false)));
            lastEUOutput = 0;
            lastEUDemand = 0;
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

    /**
     * R7: the burning grades as "32x9 + 13x1", best first - or "" while one grade covers every
     * live pillar, which is when {@code live x fuelMult x 16,384} is the honest derivation and
     * the panel prints the plain product.
     */
    private static String gradeMix(int[] liveByType) {
        int grades = 0;
        for (int type = 1; type < liveByType.length; type++) {
            if (liveByType[type] > 0) grades++;
        }
        if (grades < 2) return "";
        final StringBuilder mix = new StringBuilder();
        for (int type = liveByType.length - 1; type >= 1; type--) {
            if (liveByType[type] <= 0) continue;
            if (mix.length() > 0) mix.append(" + ");
            mix.append(liveByType[type])
                .append('x')
                .append(ROD_MULT[type]);
        }
        return mix.toString();
    }

    private void clearCampaign() {
        steamPerTick = 0;
        fuelBaseSteam = 0;
        fuelGradeMix = "";
        activeColumns = 0;
        liveColumnsLastCycle = 0;
        lastEUOutput = 0;
        lastEUDemand = 0;
        // D-26: the water term goes with the campaign. A stopped plant is drinking nothing, and a
        // panel that kept printing "Feedwater: Grade 8" over an idle silo would be describing a
        // draw that is not happening.
        feedwaterGrade = BTSWaterGrade.NOT_GRADED;
        // The chain's standing steam vents with the campaign; RPM only ever coasts down.
        steamBuffer = 0;
        // Chamber bookkeeping is about a RUNNING chain: a stopped plant must not keep
        // reporting "STARVED" (it is not drinking anything) or a stale interlock reason.
        chamberStarved = false;
        chamberCoolantDrain = 0;
        chamberCoolantName = "idle";
        turbineHoldReason = "";
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
            radiationTicks = radiationWindowTicks();
            // Freeze the field on the silo that melted - the tier is reset once the wreck
            // fails a structure check, and the hazard must not drift with it.
            radiationCenterDepth = edgeFor(getReactorTier()) / 2;
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;

        final boolean running = mMaxProgresstime > 0;
        if (wasRunningLastTick && !running) {
            decayHeatTicks = decayWindowTicks();
        }
        wasRunningLastTick = running;

        // D-17 5, THE VENT STACK. Both safety windows are also CLAMPED here, every tick, and not
        // only stamped at arming time: a stack finished halfway through a decay cycle has to
        // shorten the cycle it is standing over, or "halves the decay-heat window" would be a
        // claim about the next accident and not about this one. The clamp is a no-op without a
        // stack (the window IS the full one), so this costs nothing on a plant that has none.
        if (decayHeatTicks > 0) decayHeatTicks = Math.min(decayHeatTicks, decayWindowTicks());
        if (radiationTicks > 0) radiationTicks = Math.min(radiationTicks, radiationWindowTicks());

        // TC-3: the fan coasts - RPM decays 0.5% EVERY tick, running or not. No redstone
        // clock (or no plant) means it winds down to zero and the output dies with it.
        if (rpmMilli > 0) {
            rpmMilli = (int) ((long) rpmMilli * RPM_DECAY_PERMILLE / 1000);
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

        // Residual Spine swirl is turbulence: |S|^2 x 5 heat per second while running - unless
        // the Spiral chamber is being cooled, in which case the coolant loop carries it away
        // (SD-3's q_spine term is part of the chamber's load, and chamberLoad() charges for
        // it). An all-riding pattern therefore costs coolant, and only bites as HEAT when the
        // chamber is starved or absent.
        if (running && spineSwirl != 0 && !chamberCooled && aTick % 20 == 0) {
            heat += spineSwirl * spineSwirl * SWIRL_HEAT_PER_SECOND;
            if (heat >= SCRAM_HEAT) {
                scram();
            }
        }

        final int towers = coolingTowerCount();

        // Bench clock for commanded test plants: a cycle a second, no redstone wiring.
        if (debugAutoCycle && aTick % 20 == 0) {
            requestTurbineCycle();
        }
        // Infinite supplies (test aid): keep feedwater, coolant and propellant topped up so a
        // commanded plant runs indefinitely. Runs AFTER the machine logic of this tick, so
        // every hatch is full again at the start of the next one.
        if (debugInfiniteSupplies) {
            refillDebugSupplies();
        }

        // Machine-block updates cannot propagate across the unregistered concrete platform to
        // the embedded controller, so force periodic revalidation: every 200t while a campaign
        // runs (a torn-open silo must not keep producing), every 600t while idle (a Cooling
        // Tower or a manifold hatch added far from the controller is otherwise never noticed, and a
        // hand-built floor would never form). Never force a walk while the footprint is partially
        // unloaded - a formed machine's checker treats unloaded blocks as hard failures.
        if (aTick % 200 == 0 && (mMaxProgresstime > 0 || aTick % 600 == 0) && footprintLoaded(aBaseMetaTileEntity)) {
            onMachineBlockUpdate();
        }

        // D-17 3/4: the two self-fuelling modules turn over here. They run on their OWN clocks
        // and do not need a campaign - a Derrick on a bare platform mines perfectly well - but
        // they do need the plant to be FORMED and switched on, like every other multiblock:
        // a plant whose structure is broken must not keep producing, and a soft-malleted
        // controller is the player's off switch for the whole station, modules included.
        if (mMachine && aBaseMetaTileEntity.isAllowedToWork()) {
            runPlantModules();
        } else {
            moduleStall = "";
        }

        // The decay-heat obligation only ticks down while the SILO stands - dismantling it
        // (platform included or not) defers the obligation, never waives it. Cooling towers
        // halve the demand per tower.
        if (decayHeatTicks > 0 && mMachine && reactorTier > 0) {
            decayHeatTicks--;
            if (!running) {
                final int decayWater = Math.max(8, DECAY_WATER_PER_TICK >> towers);
                startRecipeProcessing();
                // Post-shutdown cooling is a REACTOR obligation, so it is paid out of the
                // silo's feedwater line - never out of the chamber's coolant. B7: it takes
                // EVERYTHING drawFeedwater takes (drainFeedwaterLadder is the single list) plus
                // IC2 Coolant. It used to name three fluids of its own and miss all eight
                // purified grades, so the dedicated Grade 1-8 line this release pays a x1.05 to
                // x1.40 for could run a campaign and then not cool the core it had just run.
                final boolean cooled = drawDecayHeatCooling(decayWater);
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
            // D-24 HEAT_SINK: the tree's residual heat sink adds to the towers' rejection.
            // Additive and non-negative at mask 0, so a plant with no tree cools exactly as
            // fast as it did before.
            heat = Math.max(0, heat - (HEAT_DECAY_PER_TICK * (1 + towers) + progHeatSink()));
        }
        starvedThisTick = false;
    }

    // ------------------------------------------- D-17 module effects: windows, mining, enrichment

    /**
     * D-17 5: with a VENT STACK standing, the post-shutdown decay-heat obligation is HALVED -
     * 24,000 ticks (20 minutes) of cooling water owed becomes 12,000 (10 minutes). The demand
     * per tick is unchanged; it is the WINDOW that closes sooner.
     */
    private int decayWindowTicks() {
        final int window = ventStackBuilt ? DECAY_HEAT_TICKS / 2 : DECAY_HEAT_TICKS;
        // D-24 DECAY_WINDOW: shortened further by the tree, exactly the way the Vent Stack
        // shortens it - the per-tick demand is unchanged, it is the WINDOW that closes sooner,
        // and onPostTick already clamps a standing obligation down to the current window.
        return Math.max(1, window * 1000 / Math.max(1000, progDecayWindowMilli()));
    }

    /**
     * D-17 5: and the meltdown radiation field is halved with it - 36,000 ticks (30 minutes)
     * becomes 18,000 (15). Radius and dose are unchanged; the field simply clears twice as fast.
     */
    private int radiationWindowTicks() {
        return ventStackBuilt ? RADIATION_TICKS / 2 : RADIATION_TICKS;
    }

    // The two PROCESS modules run the STANDALONE machines' numbers, exactly - the point of D-17
    // is that they are the integrated endgame form of the IV entry tech, not a better version of
    // it. MTEExtractionPlatform: 200t, -TierEU.RECIPE_EV, 6 Uraninite + 1 Thorium, +1 Tantalum
    // every 4th cycle. MTEEnrichmentTrain: 600t, -TierEU.RECIPE_HV, 8 Uraninite + 2,000 L
    // Hydrofluoric Acid -> 2 Yellowcake + 2 Thorium (M7 re-pointed BOTH; if one of these two
    // numbers is ever edited alone, the module has stopped being the standalone machine again).
    private static final int DERRICK_CYCLE_TICKS = 200;
    /**
     * R: {@code TierEU.RECIPE_EV}, NOT {@code TierEU.EV}. GT's RECIPE_ ladder is
     * {@code GTValues.VP}, which is {@code V * 30 / 32} - so EV is 1,920 EU/t and not 2,048,
     * and HV is 480 and not 512. Typing the tier voltage here made both modules 6.7% more
     * expensive to run than the standalone machines whose rates they are required to match,
     * which is exactly why they now READ the same constants instead of quoting them.
     */
    public static final long DERRICK_EU_PER_TICK = TierEU.RECIPE_EV;
    private static final int DERRICK_URANINITE = 6;
    private static final int DERRICK_THORIUM = 1;
    private static final int DERRICK_TANTALUM_EVERY = 4;
    private static final int ANNEX_CYCLE_TICKS = 600;
    public static final long ANNEX_EU_PER_TICK = TierEU.RECIPE_HV;
    private static final int ANNEX_URANINITE = 8;
    private static final int ANNEX_ACID_MB = 2_000;
    /**
     * M7: 8 Uraninite -> 2 YELLOWCAKE, the front end's own 4:1 ratio (F-01 .. F-08 turn 16
     * Uraninite into 4 Yellowcake). It used to be 20 finished {@code uraniumFuelPellet}, which
     * skipped F-01 .. F-11 - the water YIELD step at F-12 included - at 1.6 Uraninite per rod
     * against the line's 16.0 at Grade 1. See {@link MTEEnrichmentTrain} for the whole argument;
     * the two machines are required to agree and this is one of the two numbers that says so.
     */
    private static final int ANNEX_YELLOWCAKE = 2;
    private static final int ANNEX_THORIUM = 2;

    /**
     * One tick of a module's draw, ALL OF IT OR NONE OF IT.
     * <p>
     * TEST AID, on exactly the terms the feedwater and coolant lines already use: while
     * {@code /btssupply} is on, a module's draw is WAIVED IN CODE rather than materialised in a
     * hatch. Filling the tanks instead would turn a commanded plant into an unlimited tap for
     * the pack's Hydrofluoric Acid, since anything standing in a hatch can be pumped back out.
     * <p>
     * D4: this used to be {@code debugInfiniteSupplies || drainEnergyInput(eu)}, and
     * {@code drainEnergyInput} is NOT all-or-nothing. It resolves to
     * {@code ExoticEnergyInputHelper.drainEnergy(long, Collection)}, which for each hatch takes
     * {@code min(stored, remaining)}, calls {@code decreaseStoredEnergyUnits} with it, and only
     * THEN returns {@code remaining <= 0}. It empties every hatch it touches and then reports
     * failure. A Derrick on a starter grid averaging ~500 EU/t against its 1,920 EU/t draw
     * therefore burned every EU that ever arrived, for ever, while {@code derrickProgress}
     * never left 0 - the module was an incinerator, and the panel said "no EU" as though the
     * hatches were empty because the player had not wired them.
     * <p>
     * So the bank is COUNTED FIRST and nothing is spent unless the whole tick is covered. The
     * trickle now accumulates in the hatches instead of being destroyed: an under-fed module
     * simply advances more slowly - one powered tick per {@code draw / supply} real ticks -
     * which is the behaviour a player can actually reason about and fix. {@link #modulePowerNote}
     * says which of the two things is wrong.
     */
    private boolean moduleSupplied(long eu) {
        if (debugInfiniteSupplies) return true;
        if (moduleEnergyStored() < eu) return false;
        return drainEnergyInput(eu);
    }

    /**
     * D4: everything the modules could pay with on THIS tick - the same hatch list
     * {@code drainEnergyInput} spends out of, {@code getExoticAndNormalEnergyHatchList()},
     * summed through {@code GTUtility.validMTEList} so a hatch the player just broke is not
     * counted as a bank the plant does not have.
     */
    private long moduleEnergyStored() {
        long stored = 0;
        for (final MTEHatch hatch : GTUtility.validMTEList(getExoticAndNormalEnergyHatchList())) {
            stored += hatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        return stored;
    }

    /** D4: is there a LIVE energy hatch? Same shape, and same reason, as {@link #hasLiveAcidHatch}. */
    private boolean hasLiveEnergyHatch() {
        for (final MTEHatch ignored : GTUtility.validMTEList(getExoticAndNormalEnergyHatchList())) {
            return true;
        }
        return false;
    }

    /**
     * D4: the panel line for a module that could not pay this tick, which is TWO different
     * faults and used to be one word. "no EU" sent a player who had wired four Energy Hatches
     * looking for a wiring fault that was not there; what they actually needed to know is that
     * the hatches ARE fed, just not fast enough, and by how much they are short.
     */
    private String modulePowerNote(String module, long eu) {
        if (!hasLiveEnergyHatch()) {
            return module + ": no Energy Hatch on the output manifold - it draws " + euRate(eu);
        }
        return module + ": under-powered - it draws "
            + euRate(eu)
            + " and the manifold's Energy Hatches hold "
            + String.format("%,d", moduleEnergyStored())
            + " EU; nothing is lost, it waits until a whole tick is covered - raise the supply or add hatches";
    }

    /**
     * One EU/t figure, formatted once, so no panel or tooltip can quote a rate that drifted.
     * <p>
     * Tidy B4: with its voltage tier, {@code 1,920 EU/t (EV)}. The tier name is left uncoloured:
     * GT's coloured form ends in a RESET, and this figure is spliced into coloured warning lines
     * (the module stall text) whose colour that RESET would cut off half-way.
     */
    public static String euRate(long euPerTick) {
        final int tier = Math.max(0, Math.min(GTValues.VN.length - 1, GTUtility.getTier(euPerTick)));
        return String.format("%,d", euPerTick) + " EU/t (" + GTValues.VN[tier] + ")";
    }

    /** One line per stalled module, joined - the panel's "why is nothing coming out" answer. */
    private static void note(StringBuilder sb, String text) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(text);
    }

    /**
     * C2: what a SUSPENDED batch says. It has to name the feedstock, because the whole point of
     * suspending rather than zeroing is that the player's 8 Uraninite Dust and 2,000 L of
     * Hydrofluoric Acid are still theirs - they are being HELD, not eaten - and the only way
     * they can know that is if the plant says so.
     */
    private String annexSuspendNote() {
        return "Annex: batch paused at " + annexProgress
            + "/"
            + annexCycleTicks()
            + " ticks - its "
            + ANNEX_URANINITE
            + " Uraninite and "
            + String.format("%,d", ANNEX_ACID_MB)
            + " L Hydrofluoric Acid are held for it; rebuild the Annex (and its Output Bus) to resume";
    }

    /** Same for the Derrick, whose stake is the EU already paid into the load, not feedstock. */
    private String derrickSuspendNote() {
        return "Derrick: load paused at " + derrickProgress
            + "/"
            + derrickCycleTicks()
            + " ticks - the EU already paid in is held; rebuild the Derrick to resume";
    }

    /**
     * The Derrick and the Annex, once per server tick.
     * <p>
     * Both draw EU through {@link #drainEnergyInput} PER TICK rather than as a lump sum at the
     * end of a cycle. That is deliberate: a lump sum of 409,600 EU is larger than a single IV
     * Energy Hatch's buffer, so a plant with a perfectly good power feed would have stalled
     * forever waiting for a payment its hatches physically cannot hold. Per-tick also means an
     * unpowered module simply does not advance instead of banking free progress.
     * <p>
     * D4: and per-tick means ALL OF THAT TICK. GT's {@code drainEnergyInput} takes what it can
     * find across the hatches and reports failure only at the end, so an UNDER-powered module
     * used to bleed the trickle it was fed without ever advancing. {@link #moduleSupplied}
     * counts the bank first and spends nothing it cannot cover, so an under-fed module advances
     * slowly instead of burning everything and standing still.
     * <p>
     * This is the one and only place in this class that consumes EU. The manifold's four ENERGY
     * HATCHES are still not a STRUCTURE demand - the plant forms and generates without them, as
     * C1 requires - but they stop being pure furniture the moment one of these two stands, and
     * {@link #energyHatchNote} says so.
     */
    private void runPlantModules() {
        final StringBuilder stall = new StringBuilder();
        runDerrickTick(stall);
        runAnnexTick(stall);
        moduleStall = stall.toString();
    }

    /**
     * EXACTLY {@link #DERRICK_CYCLE_TICKS} powered ticks per load, and the load is deposited on
     * the LAST of them - not on the tick after. Incrementing first and testing second is what
     * keeps the cycle 200 ticks rather than 201, which is what "at standalone-Platform rates"
     * has to mean if it is to be checkable against {@link MTEExtractionPlatform}.
     */
    private void runDerrickTick(StringBuilder stall) {
        if (!derrickBuilt) {
            // C2: SUSPEND, never zero. derrickProgress is EU that has ALREADY BEEN PAID - up to
            // 200 ticks x 1,920 EU/t - and at the full count it is a finished load waiting on a
            // bus. A structure check fires on every neighbour update, so zeroing here threw that
            // away the instant a player broke one Solid Steel casing to route a pipe past the
            // deck. The batch waits, exactly where it stopped, until the Derrick stands again.
            if (derrickProgress > 0) note(stall, derrickSuspendNote());
            return;
        }
        if (live(derrickBus) == null) {
            note(stall, "Derrick: no Output Bus in its deck socket (front wall, one course up)");
            return;
        }
        // D-23 REPROCESSING 6: PROCESS INTENSIFICATION shortens the cycle. A load parked at the
        // OLD (longer) count simply falls straight through to the deposit below, which is
        // correct - it is a finished load and every tick of it has already been paid for.
        final int derrickCycle = derrickCycleTicks();
        if (derrickProgress < derrickCycle) {
            if (!moduleSupplied(DERRICK_EU_PER_TICK)) {
                note(stall, modulePowerNote("Derrick", DERRICK_EU_PER_TICK));
                return;
            }
            derrickProgress++;
            if (derrickProgress < derrickCycle) return;
        }
        // The load is finished. It is held - progress parked at the full count, no further EU
        // drawn - until the bus can take all of it, so a full bus never voids or duplicates one.
        final int cycle = derrickCycles + 1;
        final List<ItemStack> out = new ArrayList<>();
        out.add(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, DERRICK_URANINITE));
        out.add(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, DERRICK_THORIUM));
        if (cycle % derrickTantalumEvery() == 0) {
            out.add(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Tantalum, 1L));
        }
        if (!storeAll(derrickBus, out)) {
            note(
                stall,
                "Derrick: product bus will not take the load - empty it (or clear its item LOCK); the drill is holding a finished load");
            return;
        }
        derrickProgress = 0;
        derrickCycles = cycle;
        // D-23 REPROCESSING: scored on the DEPOSIT, never on the tick. A held load waiting on
        // a full bus scores once, when the bus finally takes it.
        progression.add(BTSMilestone.REPROCESSING, 1L);
    }

    /**
     * EXACTLY {@link #ANNEX_CYCLE_TICKS} powered ticks per batch: the first one is the tick the
     * feedstock is consumed on, the last one deposits. Same reason as the Derrick's - "at
     * standalone-Train rates" has to be checkable against {@link MTEEnrichmentTrain}.
     */
    private void runAnnexTick(StringBuilder stall) {
        if (!annexBuilt) {
            // C2: SUSPEND, never zero. The Annex commits its feedstock UP FRONT (8 Uraninite
            // Dust and 2,000 L of Hydrofluoric Acid, below), so ANY non-zero annexProgress means
            // the batch is already PAID FOR and nothing has been handed back. Zeroing it here
            // destroyed it, and a structure check fires on every neighbour update - one Clean
            // Stainless casing broken to route a pipe was enough. This is the same invariant
            // resetPieceState() states by deliberately EXCLUDING the progress counters; it is
            // now stated in both places instead of one.
            if (annexProgress > 0) note(stall, annexSuspendNote());
            return;
        }
        if (annexProgress > 0) {
            // A committed batch needs only the PRODUCT bus to finish - the feed bus and the acid
            // hatch have already been drawn from and are nothing to it now. Demanding all three
            // would strand a paid batch because the player pulled the empty feed bus.
            if (live(annexProductBus) == null) {
                note(stall, annexSuspendNote());
                return;
            }
        } else if (live(annexFeedBus) == null || live(annexProductBus) == null || !hasLiveAcidHatch()) {
            note(
                stall,
                "Annex: fit all 3 sockets in its outward wall - uraninite Input Bus, Hydrofluoric Acid Input Hatch, product Output Bus");
            return;
        }
        // D-23 REPROCESSING 6: same shortening as the Derrick's, and same fall-through for a
        // batch parked at the old count.
        final int annexCycle = annexCycleTicks();
        if (annexProgress == 0) {
            // A batch is only STARTED once both feedstocks are actually there, and they are
            // consumed up front exactly like the standalone Train's recipe. Nothing is drawn -
            // not EU, not acid - until the whole batch is committed, so an idle Annex with an
            // empty feed bus costs the plant nothing at all.
            final FluidStack acid = Materials.HydrofluoricAcid.getFluid(ANNEX_ACID_MB);
            if (!debugInfiniteSupplies) {
                if (countAnnexUraninite() < ANNEX_URANINITE) {
                    note(stall, "Annex: needs " + ANNEX_URANINITE + " Uraninite Dust in its feed bus");
                    return;
                }
                if (!drainFrom(annexAcidHatches, acid, true)) {
                    note(stall, "Annex: needs " + String.format("%,d", ANNEX_ACID_MB) + " L Hydrofluoric Acid");
                    return;
                }
            }
            if (!moduleSupplied(ANNEX_EU_PER_TICK)) {
                note(stall, modulePowerNote("Annex", ANNEX_EU_PER_TICK));
                return;
            }
            if (!debugInfiniteSupplies) {
                startRecipeProcessing();
                drainFrom(annexAcidHatches, acid, false);
                endRecipeProcessing();
                consumeAnnexUraninite(ANNEX_URANINITE);
            }
            annexProgress = 1;
            if (annexProgress < annexCycle) return;
        } else if (annexProgress < annexCycle) {
            if (!moduleSupplied(ANNEX_EU_PER_TICK)) {
                note(stall, modulePowerNote("Annex", ANNEX_EU_PER_TICK));
                return;
            }
            annexProgress++;
            if (annexProgress < annexCycle) return;
        }
        // Finished, and HELD until the product bus can take all of it. The uraninite and the
        // acid are already spent, so voiding the batch or paying it out twice are both wrong.
        final List<ItemStack> out = new ArrayList<>();
        // M7: YELLOWCAKE. The Annex feeds the fuel line at F-09; it does not replace it.
        out.add(new ItemStack(BTSItemsFuel.yellowcake, annexYellowcake()));
        out.add(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Thorium, ANNEX_THORIUM));
        if (!storeAll(annexProductBus, out)) {
            note(
                stall,
                "Annex: product bus will not take the batch - empty it (or clear its item LOCK); a finished batch is waiting");
            return;
        }
        annexProgress = 0;
        annexCycles++;
        progression.add(BTSMilestone.REPROCESSING, 1L);
    }

    /**
     * Uraninite standing in the ANNEX's own feed bus - never the silo's rod bus. C4: read through
     * {@link #live} so a bus the player broke this tick reports 0 rather than answering out of a
     * dead tile entity's stale inventory (and so committing a batch against feedstock that is
     * already on the floor as an item drop).
     */
    private int countAnnexUraninite() {
        return annexUraninite(0);
    }

    /** C4: same liveness rule on the WRITE side - never decrement a dead tile entity. */
    private void consumeAnnexUraninite(int amount) {
        annexUraninite(amount);
    }

    /**
     * The Annex's feed bus, READ and (when {@code consume > 0}) DRAWN FROM in one pass. Returns
     * how much uraninite was standing there BEFORE the draw.
     * <p>
     * D3: this used to walk raw slots - {@code base.getSizeInventory()} filtered by
     * {@code feed.isValidSlot(i)} - and an ME STOCKING INPUT BUS therefore always read ZERO.
     * {@link #addAnnexFeedBusToMachineList} accepts any {@code MTEHatchInputBus} and
     * {@code MTEHatchInputBusME} extends it, so a stocking bus FORMS the Annex; but in
     * 5.09.52.594 that class answers {@code isValidSlot(i)} with {@code i == getManualSlot()}
     * and nothing else, and its {@code getStackInSlot(i)} returns {@code null} for every slot
     * that is not the circuit or the manual one UNLESS {@code processingRecipe} is set. Ten
     * thousand uraninite in the network read as 0 and the Annex never started.
     * <p>
     * The fix is GT's own protocol for exactly this, {@code IRecipeProcessingAwareHatch}:
     * {@code startRecipeProcessing()} makes the bus publish its AE-backed contents through
     * {@code getStackInSlot}, and {@code endRecipeProcessing(this)} turns whatever was taken
     * out of those stacks into a real {@code Platform.poweredExtraction} from the network. It
     * is the same window {@code MTEMultiBlockBase.checkRecipe()} opens around
     * {@code checkProcessing()} for every stock GT multiblock - including its consequence, that
     * a stocking bus which has lost its grid stops the machine from inside
     * {@code endRecipeProcessing}. A plain bus does not implement the interface and is simply
     * walked directly, exactly as before.
     * <p>
     * Consumption goes through {@code decrStackSize}, which is what makes both cases work from
     * one loop: {@code CommonMetaTileEntity.decrStackSize} mutates the very {@code ItemStack}
     * that {@code getStackInSlot} returned (it {@code splitStack}s it, or zeroes it when
     * {@code setStackToZeroInsteadOfNull} says so - and the ME bus says so for the whole
     * window), and the difference against {@code extractedAmount} is precisely what
     * {@code endRecipeProcessing} bills the network for.
     * <p>
     * The slot filter is gone with the raw walk and is not missed: the only slot the old
     * {@code isValidSlot} excluded is the CONFIGURATION CIRCUIT slot, and a circuit can never
     * be {@link GTUtility#areStacksEqual} to Uraninite Dust, so it can neither be counted nor
     * consumed. Matching on the item is the filter.
     */
    private int annexUraninite(int consume) {
        final MTEHatchInputBus feed = live(annexFeedBus);
        final ItemStack sample = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Uraninite, 1L);
        final IGregTechTileEntity base = feed == null ? null : feed.getBaseMetaTileEntity();
        if (sample == null || base == null) return 0;
        final IRecipeProcessingAwareHatch stocking = feed instanceof IRecipeProcessingAwareHatch
            ? (IRecipeProcessingAwareHatch) feed
            : null;
        if (stocking != null) stocking.startRecipeProcessing();
        try {
            int found = 0;
            int left = consume;
            for (int i = 0; i < base.getSizeInventory(); i++) {
                final ItemStack stack = base.getStackInSlot(i);
                if (stack == null || stack.stackSize <= 0) continue;
                if (!GTUtility.areStacksEqual(sample, stack)) continue;
                found += stack.stackSize;
                if (left <= 0) continue;
                final int take = Math.min(left, stack.stackSize);
                base.decrStackSize(i, take);
                left -= take;
            }
            return found;
        } finally {
            // The window is closed on EVERY path, including the one where the loop threw, or a
            // stocking bus would be left publishing its network into a bus it no longer owns.
            if (stocking != null) setResultIfFailure(stocking.endRecipeProcessing(this));
            feed.updateSlots();
        }
    }

    /**
     * Deposits a finished load into ONE module's own bus, ALL OF IT OR NONE OF IT.
     * <p>
     * C3: this used to reason about SLOT COUNTS - "at least as many empty slots as stacks" -
     * and that made an ME Output Bus impossible to pay into, forever. {@code MTEHatchOutputBusME}
     * extends {@code MTEHatchOutputBus} with {@code aSlots = 1}, and that one slot is the
     * STORAGE CELL slot, not a landing slot: {@code empty} is 1 with the cell out and 0 with it
     * in, {@code wanted} is 2, so the Derrick's finished load was refused on every tick of every
     * cycle on the obvious endgame bus for bulk dusts. The count also stalled any load whose
     * items would simply have MERGED into a part-full slot of an ordinary bus.
     * <p>
     * The fix is to stop modelling the bus and ASK IT, through the bus's own
     * {@link IOutputBusTransaction}. Every implementation - the standard bus, the compressed
     * bus, the ME bus - answers for itself: the standard one works on a COPY of its inventory
     * and only writes it back in {@code commit()}, the compressed one on a {@code copy()} of
     * its {@code AEInventory}, the ME one accumulates into an AE cache counter and only pushes
     * it in {@code commit()}. So the whole batch is offered stack by stack, and if any part of
     * it does not fit we simply RETURN WITHOUT COMMITTING and not one item has moved. That is
     * the same all-or-nothing contract as before, without a slot count. The caller holds the
     * load and retries next tick.
     * <p>
     * D1: THE RETURN VALUE OF {@code storePartial(ItemId, ItemStack)} IS NOT "made progress",
     * and it is not even consistent between implementations. Read in 5.09.52.594:
     * <ul>
     * <li>{@code MTEHatchOutputBus.StandardOutputBusTransaction} fills AT MOST ONE SLOT per
     * call and returns {@code true} only if that call moved at least one item - so it has to
     * be called once per slot, and it also answers {@code false} for an already-empty stack;</li>
     * <li>{@code MTEHatchOutputBusCompressed.CompressedOutputBusTransaction} injects the WHOLE
     * stack into its AE inventory, writes the leftover back into {@code stack.stackSize} - and
     * then {@code return false} UNCONDITIONALLY (bytecode {@code iconst_0; ireturn}). It never
     * returns true, not even when it took everything;</li>
     * <li>{@code MTEHatchOutputBusME.MEOutputBusTransaction} takes the WHOLE stack in one call,
     * zeroes {@code stack.stackSize} and returns {@code true}, or refuses outright with
     * {@code false} and leaves the stack untouched.</li>
     * </ul>
     * The previous {@code while (pending.stackSize > 0) if (!storePartial(...)) return false;}
     * therefore made every Compressed Output Bus (LuV-UHV) and every QUANTUM Output Bus
     * (UEV-UXV) - the same {@code MTEHatchOutputBusCompressed} class - refuse the load FOREVER,
     * stranding the Annex's already-spent 8 Uraninite and 2,000 L HF. The only fact all three
     * agree on is the OUT PARAMETER: whatever is left unstored is left in {@code stack.stackSize}.
     * So the loop is driven off that, and it stops the moment a call fails to shrink it, which
     * also makes it provably non-looping (the standard bus shrinks it once per slot, the other
     * two settle in at most two calls).
     * <p>
     * D2: the ITEM LOCK is enforced HERE, because no transaction enforces it. The OLD
     * {@code bus.storePartial(ItemStack, boolean)} opened with an explicit test - bytecode
     * offsets 8-27, {@code if (lockedItem != null && !lockedItem.isItemEqual(stack)) return
     * false} - and NONE of the three transaction implementations carries it: the standard and
     * compressed ones go straight to their inventories, and only the ME one consults a filter
     * (its own {@code MEFilterItem}, not {@code lockedItem}). Without this test a Derrick bus
     * locked to Uraninite, so a downstream AE2 Storage Bus only ever sees uraninite, had
     * Thorium and Tantalum forced into it. {@code IOutputBusTransaction.isFiltered()} /
     * {@code isFilteredTo(ItemId)} (isFilteredToItem before 2.9.0-RC-2) are GT's own question for this and delegate to
     * the bus,
     * so the standard and compressed buses answer out of {@code lockedItem} and the ME bus out
     * of its filter - one test, correct for all four bus families.
     * <p>
     * C4: and the bus is validated first. {@code MetaTileEntity.isValid()} is
     * {@code base != null && base.getMetaTileEntity() == this && !base.isDead()}; a BROKEN hatch
     * keeps a NON-NULL base with {@code isDead()} true, so the old {@code base == null} test
     * never fired for a removed bus and the load was written into a dead tile entity and lost.
     */
    private static boolean storeAll(MTEHatchOutputBus bus, List<ItemStack> stacks) {
        final MTEHatchOutputBus target = live(bus);
        if (target == null) return false;
        final IOutputBusTransaction transaction = target.createTransaction();
        for (final ItemStack stack : stacks) {
            if (stack == null || stack.stackSize <= 0) continue;
            final GTUtility.ItemId id = GTUtility.ItemId.create(stack);
            // D2: the lock, before anything is offered. A locked bus that is not locked to THIS
            // item refuses the whole batch, exactly as the old simulate pass did.
            if (transaction.isFiltered() && !transaction.isFilteredTo(id)) return false;
            // D1: storePartial mutates the stack it is handed - what it could not take is left
            // in stackSize. Drive on THAT, never on the return value, and stop the moment a
            // call stops shrinking it.
            final ItemStack pending = stack.copy();
            int before;
            do {
                before = pending.stackSize;
                // RC-2 (IOutputTransaction): the two parallel weights are only read when
                // needsTotalParallelData() is true (an ME bus put in recipe-check mode, which a
                // plain createTransaction() never is), so the stack's own size is a safe value.
                transaction.storePartial(id, pending, pending.stackSize, pending.stackSize);
            } while (pending.stackSize > 0 && pending.stackSize < before);
            if (pending.stackSize > 0) return false;
            transaction.complete(id);
        }
        transaction.commit();
        return true;
    }

    /**
     * C4: the MTE if it is STILL LIVE, else null - {@code GTUtility.validMTEList}'s test, for the
     * dedicated module sockets, which are single references rather than lists and so cannot go
     * through that iterator.
     * <p>
     * {@code hatch == null} is NOT that test. A hatch the player broke keeps a non-null
     * {@code getBaseMetaTileEntity()} whose {@code isDead()} is true until the field is cleared
     * by the next structure check, and a structure check is not guaranteed to run before the
     * next module tick. In that window the Derrick wrote 6 Uraninite and 1 Thorium into a dead
     * tile entity and the Annex read its feedstock out of one.
     */
    private static <T extends MetaTileEntity> T live(T mte) {
        return mte != null && mte.isValid() ? mte : null;
    }

    /** C4: is there a LIVE acid hatch? {@code isEmpty()} answers no such thing. */
    private boolean hasLiveAcidHatch() {
        for (final MTEHatchInput ignored : GTUtility.validMTEList(annexAcidHatches)) {
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ persistence & info

    /**
     * <b>F3 - A CREEPER MAY NOT DELETE THE CAMPAIGN.</b> The D-23/D-24 record lives on this one
     * controller by ruling, and four of the five ways it can leave the world were already safe:
     * a reload and a chunk unload ride {@code saveNBTData}, a WRENCH and a pickaxe ride
     * {@code setItemNBT} through {@code BaseMetaTileEntity.getDrops()}, and an un-form/re-form
     * never touches the tile at all. The fifth was not. {@code Explosion.doExplosionB} calls
     * {@code dropBlockAsItemWithChance(world, x, y, z, meta, 1.0F / 3.0F, 0)}, so two times in
     * three an exploded controller yields NO ITEM - and with it every milestone counter and
     * every purchased node, with nothing to pick up and no way back.
     * <p>
     * The least machinery that closes it is to keep the block out of the explosion in the first
     * place, which costs one method and no new persistent state - as against a world-level
     * backup, which would be a second copy of the record and a second thing to get wrong, and
     * would quietly contradict the per-controller ruling. Traced, not assumed: Forge's
     * {@code Entity.func_145772_a} patch routes the exploder's query to
     * {@code Block.getExplosionResistance(Entity, World, x, y, z, ex, ey, ez)},
     * {@code BlockMachines} overrides that and delegates it to
     * {@code IMetaTileEntity.getExplosionResistance(ForgeDirection.UNKNOWN)} - this method - and
     * {@code doExplosionA} subtracts {@code (resistance + 0.3F) * 0.3F} from a ray that starts
     * at most at {@code size * 1.3}. At 2,000 the block survives any blast up to size ~461; a
     * creeper is 3, a charged creeper 6, TNT 4.
     * <p>
     * The plant's own meltdown is unaffected: it detonates ABOVE the dome
     * ({@code stackTop + 2 .. + 11}), never at the front-wall controller, and it disables the
     * machine through {@code stopMachine} rather than by destroying the block. GT's default for
     * a machine is 10.0F and every other block in this multiblock keeps it - a blast still opens
     * a hole in the casings and un-forms the plant, which is the recoverable case.
     */
    @Override
    public float getExplosionResistance(ForgeDirection side) {
        return CONTROLLER_BLAST_RESISTANCE;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("btsHeat", heat);
        aNBT.setInteger("btsReactorTier", reactorTier);
        // M2: there is deliberately no remembered build tier here any more. A number in tile NBT
        // is not in ITEM NBT, so wrenching the controller out and back dropped it and re-armed
        // the very destruction it existed to prevent; standingSiloTier() measures the shell
        // instead and therefore has nothing to persist and nothing to lose.
        aNBT.setInteger("btsActiveColumns", activeColumns);
        aNBT.setInteger("btsThrottleN", throttleN);
        aNBT.setInteger("btsThrottleSetting", throttleSetting);
        aNBT.setInteger("btsBuildTierSetting", buildTierSetting);
        aNBT.setInteger("btsFuelMult", activeFuelMult);
        aNBT.setInteger("btsDecayHeatTicks", decayHeatTicks);
        aNBT.setInteger("btsRadiationTicks", radiationTicks);
        aNBT.setInteger("btsRadCenterDepth", radiationCenterDepth);
        aNBT.setBoolean("btsDebugAutoCycle", debugAutoCycle);
        aNBT.setBoolean("btsDebugSupplies", debugInfiniteSupplies);
        // D-25: the output-safety MODE and the record of what it caught. Tile NBT only - see
        // the field's javadoc for why this one must never ride the wrench into item NBT.
        aNBT.setBoolean("btsDebugOutputSafety", debugOutputSafety);
        aNBT.setInteger("btsOutputOverruns", outputOverruns);
        aNBT.setLong("btsOutputOverrunOffered", outputOverrunOffered);
        aNBT.setLong("btsOutputOverrunCapacity", outputOverrunCapacity);
        aNBT.setLong("btsOutputOverrunDynamo", outputOverrunDynamo);
        aNBT.setLong("btsOutputOverrunLaser", outputOverrunLaser);
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
        // R7: steamPerTick is persisted, so its derivation has to be too - otherwise the math
        // panel prints a step 1 of zero for a whole decay cycle after every world load.
        aNBT.setLong("btsFuelBaseSteam", fuelBaseSteam);
        aNBT.setString("btsFuelGradeMix", fuelGradeMix);
        aNBT.setInteger("btsRpmMilli", rpmMilli);
        // F4: the authorised overspeed. Must be read back BEFORE the rpmMilli clamp below, and
        // loadNBTData does exactly that.
        aNBT.setInteger("btsRpmLimit", rpmLimitSetting);
        aNBT.setLong("btsSteamBuffer", steamBuffer);
        // D-17: the two process modules' RUN state. derrickCycles is not decoration - it is what
        // decides which cycle pays the tantalum bonus - and annexProgress carries a batch whose
        // uraninite and acid are ALREADY SPENT, so losing it across a reload would eat them.
        aNBT.setInteger("btsDerrickProgress", derrickProgress);
        aNBT.setInteger("btsDerrickCycles", derrickCycles);
        aNBT.setInteger("btsAnnexProgress", annexProgress);
        aNBT.setInteger("btsAnnexCycles", annexCycles);
        // D-23: the campaign record, through its ONE serializer. force = true here, because
        // tile NBT is the reload/chunk-unload path and always carries the whole block.
        progression.writeTo(aNBT, true);
    }

    /**
     * C2 / AXIS 2: {@code saveNBTData} survives a RELOAD. Surviving a WRENCH needs this - the
     * dropped controller item carries only what {@code setItemNBT} writes into it, and
     * {@code loadNBTData} is what reads it back when the controller is placed again.
     * <p>
     * Only the two process modules' RUN state travels, and only because a non-zero
     * {@code annexProgress} means 8 Uraninite Dust and 2,000 L of Hydrofluoric Acid are ALREADY
     * SPENT with nothing paid back yet. Wrenching the controller out to move a cable is not
     * consent to eat them, and the counters go with them so the tantalum bonus (every 4th
     * Derrick cycle) does not silently restart either.
     * <p>
     * Nothing ELSE is carried, deliberately. M2's lesson is the opposite one and still stands:
     * {@code btsLastBuiltTier} was a number that ARMED a destructive path, and carrying it
     * across a wrench was the bug. These four are the reverse - they only ever hand property
     * BACK, and the worst a corrupt value can do is hold a batch (loadNBTData already clamps
     * everything that needs clamping).
     * <p>
     * <b>F9 - AND THE THREE OPERATOR SETTINGS.</b> {@code btsThrottleSetting} was written by
     * {@code saveNBTData} but not here, so {@code loadNBTData} read 0 off the item tag and forced
     * the field back to 24: wrenching a plant that had unlocked 30/24 or 36/24 cost it 20-33% of
     * its output until somebody noticed and re-typed the number. The RPM LIMIT (F4) would have
     * had exactly the same hole, and Build Tier is the same class of value. All three travel
     * now.
     * <p>
     * Each is written ONLY when it differs from its default, which is the same rule
     * {@code BTSProgression.writeTo(force = false)} follows and for the same reason:
     * {@code BaseMetaTileEntity.getDrops()} attaches a tag {@code if (!tNBT.hasNoTags())}, so an
     * unconditional write would give EVERY wrenched controller a tag and stop untouched ones
     * stacking. None of the three ARMS anything - M2's distinction - and {@code loadNBTData}
     * clamps all three on the way back in, including Build Tier's upper bound, which now matters
     * because it arrives from item NBT as well.
     */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        if (derrickProgress > 0) aNBT.setInteger("btsDerrickProgress", derrickProgress);
        if (derrickCycles > 0) aNBT.setInteger("btsDerrickCycles", derrickCycles);
        if (annexProgress > 0) aNBT.setInteger("btsAnnexProgress", annexProgress);
        if (annexCycles > 0) aNBT.setInteger("btsAnnexCycles", annexCycles);
        if (throttleSetting > 0 && throttleSetting != 24) aNBT.setInteger("btsThrottleSetting", throttleSetting);
        if (rpmLimitSetting > RPM_REDLINE) aNBT.setInteger("btsRpmLimit", rpmLimitSetting);
        if (buildTierSetting > 1) aNBT.setInteger("btsBuildTierSetting", buildTierSetting);
        // D-23 / AXIS 2: THE WRENCH PATH, and the reason the whole record exists in one
        // serializer. The SAME writeTo body that saveNBTData calls, with force = false, so a
        // controller that has earned nothing still drops as a plain stackable machine item
        // (getDrops() only attaches a tag `if (!tNBT.hasNoTags())`) while one that HAS earned
        // something carries every counter out with it.
        //
        // This is the opposite of M2's btsLastBuiltTier lesson, not a repeat of it: that was a
        // number which ARMED a destructive path, and carrying it across a wrench was the bug.
        // These four only ever hand property BACK, and readFrom clamps every one of them.
        progression.writeTo(aNBT, false);
    }

    /**
     * F9 GUARD, behind {@code /btsmilestone verify}: the three OPERATOR SETTINGS that ride the
     * WRENCH, checked by writing a probe tag through the REAL {@link #setItemNBT} and reading
     * the keys straight back out of it.
     * <p>
     * {@code BTSProgression.verifyRoundTrip} does this for the campaign record and would have
     * caught F9 the day it happened - if the throttle had been part of that record. It is not;
     * it is a plain field, and a field written by {@code saveNBTData} and forgotten by
     * {@code setItemNBT} is exactly the shape of bug that cost this plant a third of its output
     * after every wrench. The invariant checked here is the one the writer actually promises:
     * <b>a setting that is off its default is on the item, with the same value.</b> Settings AT
     * their default are deliberately absent (an untouched controller has to stay stackable), so
     * they are not asserted - their absence is the correct answer and {@code loadNBTData}
     * restores the default from it.
     * <p>
     * Nothing is mutated: {@code setItemNBT} only ever writes into the tag it is handed.
     */
    public String verifyItemSettings() {
        final NBTTagCompound probe = new NBTTagCompound();
        setItemNBT(probe);
        final StringBuilder faults = new StringBuilder();
        appendSettingFault(faults, probe, "btsThrottleSetting", throttleSetting, 24);
        appendSettingFault(faults, probe, "btsRpmLimit", rpmLimitSetting, RPM_REDLINE);
        appendSettingFault(faults, probe, "btsBuildTierSetting", buildTierSetting, 1);
        return faults.toString();
    }

    private static void appendSettingFault(StringBuilder faults, NBTTagCompound probe, String key, int value,
        int defaultValue) {
        if (value == defaultValue) return;
        final int carried = probe.getInteger(key);
        if (carried == value) return;
        if (faults.length() > 0) faults.append("; ");
        faults.append("item ")
            .append(key)
            .append(' ')
            .append(value)
            .append(" -> ")
            .append(carried);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        // D-23: THE ONE READER, and it runs FIRST because the rpmMilli clamp further down is
        // taken against redlineMilli(), which is derived from the TURBINE line. Reading the
        // record after that clamp would knock an overspeed-trimmed plant back to 12,000 RPM
        // on every single world load. Item NBT and tile NBT both land here - traced:
        // ItemMachines.placeBlock -> setInitialValuesAsNBT (server only) ->
        // BaseMetaTileEntity.loadMetaTileNBT -> CommonBaseMetaTileEntity -> loadNBTData.
        progression.readFrom(aNBT);
        // F4 / F9: and the two OPERATOR SETTINGS the record's ceilings are consumed through -
        // both before the rpmMilli clamp at the foot of this method, which is taken against
        // redlineMilli() and therefore against rpmLimitSetting.
        rpmLimitSetting = Math.max(RPM_REDLINE, aNBT.getInteger("btsRpmLimit"));
        heat = aNBT.getInteger("btsHeat");
        reactorTier = aNBT.getInteger("btsReactorTier");
        // "btsLastBuiltTier" from 0.19.x is deliberately not read: an unclamped persisted tier
        // was fed straight into pieceName(), and getStructureFor() throws NoSuchElementException
        // for a shape name that was never registered - once per tick, from inside checkMachine.
        activeColumns = aNBT.getInteger("btsActiveColumns");
        throttleN = aNBT.getInteger("btsThrottleN");
        throttleSetting = aNBT.getInteger("btsThrottleSetting");
        if (throttleSetting <= 0) throttleSetting = 24;
        // F9: clamped at BOTH ends now. The lower clamp covers an absent key (a fresh controller
        // or a pre-0.22.1 world); the upper one exists because this arrives from ITEM NBT as
        // well as tile NBT, and an out-of-range build tier is the shape of value M2 was about.
        buildTierSetting = Math.min(MAX_TIER, Math.max(1, aNBT.getInteger("btsBuildTierSetting")));
        activeFuelMult = Math.max(1, aNBT.getInteger("btsFuelMult"));
        decayHeatTicks = aNBT.getInteger("btsDecayHeatTicks");
        radiationTicks = aNBT.getInteger("btsRadiationTicks");
        // C2: clamped, because these now arrive from ITEM NBT as well as tile NBT - and because
        // a progress counter above its cycle length would deposit on the tick it loaded.
        derrickProgress = Math.max(0, Math.min(DERRICK_CYCLE_TICKS, aNBT.getInteger("btsDerrickProgress")));
        derrickCycles = Math.max(0, aNBT.getInteger("btsDerrickCycles"));
        annexProgress = Math.max(0, Math.min(ANNEX_CYCLE_TICKS, aNBT.getInteger("btsAnnexProgress")));
        annexCycles = Math.max(0, aNBT.getInteger("btsAnnexCycles"));
        radiationCenterDepth = aNBT.getInteger("btsRadCenterDepth");
        debugAutoCycle = aNBT.getBoolean("btsDebugAutoCycle");
        debugInfiniteSupplies = aNBT.getBoolean("btsDebugSupplies");
        // D-25: absent tag -> false -> ARMED, which is both the default and the safe reading of
        // a missing value: a plant loaded from an older world is in normal play, and says so.
        debugOutputSafety = aNBT.getBoolean("btsDebugOutputSafety");
        outputOverruns = Math.max(0, aNBT.getInteger("btsOutputOverruns"));
        outputOverrunOffered = Math.max(0L, aNBT.getLong("btsOutputOverrunOffered"));
        outputOverrunCapacity = Math.max(0L, aNBT.getLong("btsOutputOverrunCapacity"));
        outputOverrunDynamo = Math.max(0L, aNBT.getLong("btsOutputOverrunDynamo"));
        outputOverrunLaser = Math.max(0L, aNBT.getLong("btsOutputOverrunLaser"));
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
        // A pre-R7 world has no stored base; 0 makes the formatters fall back to the old
        // live x grade product, which is exactly right for the uniform load it must have been.
        fuelBaseSteam = aNBT.getLong("btsFuelBaseSteam");
        fuelGradeMix = aNBT.getString("btsFuelGradeMix");
        rpmMilli = Math.max(0, Math.min(redlineMilli(), aNBT.getInteger("btsRpmMilli")));
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

    // ------------------------------------------------- D-23 the milestone-derived limits
    // Every one of these takes the plant's BASE constant and hands back a NEW MAXIMUM. At
    // zero progress each returns its base unchanged, which is the whole "a plant that works
    // today keeps working" guarantee - it is checkable by reading these eight methods.
    //
    // AXIS 1: all of them read `progression`, which is written SERVER-side only. Nothing here
    // may be called from a client-side path. construct(hintsOnly=true) never touches them: no
    // structure SHAPE is gated on progression at all, only settings and rates are.

    /** D-23: the four milestone counters, for the panel, the sync values and the command. */
    public BTSProgression getProgression() {
        return progression;
    }

    /** The four counters as one array, ordered by {@link BTSMilestone} ordinal. */
    public long[] getProgressSnapshot() {
        return progression.snapshot();
    }

    public long getProgUptime() {
        return progression.get(BTSMilestone.UPTIME);
    }

    public long getProgBurnup() {
        return progression.get(BTSMilestone.BURNUP);
    }

    public long getProgReproc() {
        return progression.get(BTSMilestone.REPROCESSING);
    }

    public long getProgPerfect() {
        return progression.get(BTSMilestone.TURBINE);
    }

    /**
     * The milestone term of the EU derivation, x1000 - the product of the four lines' tier-1..4
     * multipliers. 1000 (a no-op) on a plant with no progress.
     */
    public int progOutputMilli() {
        return progression.outputMilli();
    }

    // ------------------------------------------------- D-24 the upgrade tree, at the plant
    // Three channels reach the plant through methods of their own (the other nine are folded
    // into the D-23 accessors above, so their call sites never had to move). Everything here
    // reads `progression`, which is written SERVER-side; the GUI reads the SYNCED node mask and
    // runs the identical static bodies in BTSUpgrade/BTSProgression to reach the same numbers.

    /** The tree's OUTPUT term, x1000 - the fifth factor of {@link #euDemandOf}. */
    public int progUpgradeMilli() {
        return progression.upgradeMilli();
    }

    /** D-24 HEAT_SINK: extra heat bled off per tick, on top of the Cooling Towers'. 0 at mask 0. */
    private int progHeatSink() {
        return BTSUpgrade.channel(progression.nodes(), BTSChannel.HEAT_SINK);
    }

    /** D-24 DECAY_WINDOW: the divisor on the post-shutdown window, x1000. 1000 at mask 0. */
    private int progDecayWindowMilli() {
        return BTSUpgrade.channel(progression.nodes(), BTSChannel.DECAY_WINDOW);
    }

    /** The purchased-node bitmask. The ONE value the tree GUI syncs (AXIS 1). */
    public long getUpgradeNodes() {
        return progression.nodes();
    }

    /**
     * BUY, SERVER-SIDE AND SERVER-AUTHORITATIVE. The only caller is the tree panel's
     * server-only synced action and the debug command; the client sends a node INDEX and
     * nothing else, and every rule - already owned, prerequisites, milestone gate, points - is
     * re-asked here against the server's own record. A client that lies changes nothing.
     */
    public boolean buyUpgrade(BTSUpgrade node) {
        if (!progression.buy(node)) return false;
        markProgressionDirty();
        return true;
    }

    /** REFUND, same terms. Free and unlimited; refused only if it would orphan a held node. */
    public boolean refundUpgrade(BTSUpgrade node) {
        if (!progression.refund(node)) return false;
        markProgressionDirty();
        return true;
    }

    /**
     * A purchase has to reach the disk on its own, not on the next incidental save: GT tiles
     * ride their chunk's write, so the chunk is what must be marked. Without this a buy
     * followed by a crash - or by a chunk unload that happened to find the chunk clean - is a
     * point spent for nothing, which is exactly the class of loss this release must not have.
     */
    private void markProgressionDirty() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base instanceof TileEntity) {
            ((TileEntity) base).markDirty();
        }
    }

    /**
     * D-23 BURNUP 5/6/7: the highest control-rod setting this plant may take. 24 is the design
     * stop; the line lifts it to 26, 28 and 30. The N/24 DENOMINATOR never moves, so 30/24 is
     * genuinely 125% of design power and not a relabelled 100%.
     */
    public int maxThrottle() {
        return progression.maxThrottle(24);
    }

    /**
     * The redline the fan is actually held to: the UNLOCKED ceiling, capped by what the operator
     * authorised in the RPM LIMIT field (F4). At the default 12,000 setting this is 12,000 on
     * every plant however far the TURBINE line has run, so nothing that ran yesterday changes.
     */
    public int getRedlineRpm() {
        return operatingRedline(getUnlockedRedlineRpm(), rpmLimitSetting);
    }

    /**
     * ONE body for the min, so the client (which derives the unlocked ceiling from the synced
     * counters and node mask) and the server reach the same number (AXIS 1).
     */
    public static int operatingRedline(int unlockedRedline, int limitSetting) {
        return Math.min(Math.max(RPM_REDLINE, unlockedRedline), Math.max(RPM_REDLINE, limitSetting));
    }

    /** D-23 TURBINE 5/6 + D-24: the CEILING the campaign has unlocked. 12,000 -> 19,500. */
    public int getUnlockedRedlineRpm() {
        return progression.redlineRpm(RPM_REDLINE);
    }

    /** The RPM LIMIT field, clamped to the live ceiling - a refunded tree node lowers it. */
    public int getRpmLimitSetting() {
        return operatingRedline(getUnlockedRedlineRpm(), rpmLimitSetting);
    }

    /**
     * F4: server-side clamp to the unlocked ceiling, exactly like {@link #setThrottleSetting}.
     * A LOWERED limit takes effect at once - leaving the fan spinning above its new ceiling
     * would have it drawing coolant for an RPM the interlock has stopped projecting.
     */
    public void setRpmLimitSetting(int value) {
        rpmLimitSetting = operatingRedline(getUnlockedRedlineRpm(), value);
        rpmMilli = Math.min(rpmMilli, redlineMilli());
    }

    /**
     * F4: the per-cycle RPM gain, x1000, CLAMPED TO THE AUTHORISED OVERSPEED. The redline and
     * the gain have to move together - the equilibrium is {@code 10.486 x gain}, so 11,996 at
     * x1.000 - and authorising 13,500 is authorising the x1.125 that reaches it. Clamping the
     * gain here rather than only the redline is what makes this a true opt-in: a plant whose
     * propellant tops out BELOW the redline (Lv 3 settles at ~9,227) is never pushed to a
     * higher equilibrium, and therefore never to a higher coolant draw, until it asks.
     */
    private int rpmGainMilli() {
        final int authorised = getRedlineRpm() * 1000 / RPM_REDLINE;
        return Math.max(1000, Math.min(progression.rpmGainMilli(), authorised));
    }

    private int redlineMilli() {
        return getRedlineRpm() * 1000;
    }

    /** D-23 UPTIME 6: 20-tick slugs the chain's steam buffer holds. 100 -> 160. */
    private int steamBufferCapTicks() {
        return progression.steamBufferSlugs(STEAM_BUFFER_CAP_TICKS);
    }

    private int derrickCycleTicks() {
        return progression.moduleCycleTicks(DERRICK_CYCLE_TICKS);
    }

    private int annexCycleTicks() {
        return progression.moduleCycleTicks(ANNEX_CYCLE_TICKS);
    }

    private int derrickTantalumEvery() {
        return progression.derrickTantalumEvery(DERRICK_TANTALUM_EVERY);
    }

    /**
     * M7 + REPROCESSING 7: how many Yellowcake THIS batch pays.
     * <p>
     * {@code BTSProgression.annexPellets(base)} is {@code max(base, base * 6/5 * ANNEX_BATCH /
     * 1000)}, so passing 1000 reads it back as a straight MILLI multiplier - 1000 unimproved,
     * 1200 with REPROCESSING 7, more with the tree - rather than as a count. That indirection is
     * the whole point of this method: a fifth of the old twenty-pellet batch was four whole
     * pellets, but a fifth of two cakes is 0.4, and {@code 2 * 6 / 5} truncates straight back to
     * 2. Left as a plain multiply, M7 would have silently turned the milestone into a no-op.
     * <p>
     * So the fraction is CARRIED between batches instead of being discarded: the cakes owed
     * through this batch, minus the cakes owed through the one before it. At x1.200 that pays
     * 2, 2, 3, 2, 3 - twelve cakes per five batches, which is the 2.4 the milestone promises.
     * At x1.000 every term is exactly {@code ANNEX_YELLOWCAKE}, so a plant with no milestone and
     * no tree pays the same batch it paid before this method existed.
     * <p>
     * AXIS 2: the carry is NOT a new field. It is derived from {@code annexCycles}, which is
     * already persisted to both tile NBT and item NBT, so a wrenched controller resumes the
     * sequence rather than restarting it - and any value of it derives an unbroken run of
     * {@code ANNEX_YELLOWCAKE} while the multiplier is 1000. A held batch (product bus full)
     * re-derives the same number every tick, because {@code annexCycles} only moves on deposit.
     */
    private int annexYellowcake() {
        final long batchMilli = progression.annexPellets(1000);
        final long owedThrough = (long) ANNEX_YELLOWCAKE * (annexCycles + 1L) * batchMilli / 1000L;
        final long owedBefore = (long) ANNEX_YELLOWCAKE * annexCycles * batchMilli / 1000L;
        return (int) Math.max(ANNEX_YELLOWCAKE, owedThrough - owedBefore);
    }

    public int getCyclesPerMinute() {
        return cyclesPerMinute;
    }

    public int getLastCyclePropMilli() {
        return lastCyclePropMilli;
    }

    /** P (x1000) of the propellant standing in the injectors right now. */
    public int getPropellantMilli() {
        return propellantMilli;
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

    /** True when the chamber asked for coolant this tick and got none. */
    public boolean isChamberStarved() {
        return chamberStarved;
    }

    /** Localized name of the propellant standing in the injectors, or "none". */
    public String getPropellantName() {
        return propellantName;
    }

    /** Per-stage rotor wear, "0 0 1 - ..." with '-' for an empty seat. */
    public String getRotorWear() {
        return rotorWear;
    }

    /**
     * Why the turbine is not accelerating (TC-7 interlock, a missed cycle, a mistimed window),
     * or "" when nothing is wrong. Never a shutdown - just the reason the RPM is not climbing.
     */
    public String getTurbineHoldReason() {
        if (reactorTier <= 0 || !spiralBuilt) return "";
        if (mMaxProgresstime <= 0) {
            return "reactor not running - put fuel rods in the silo's Rod Input Bus and enable the controller";
        }
        if (!galleryBuilt) return "no Rotor Assembly - cycles cannot be accepted";
        if (turbineHoldReason.isEmpty()) {
            // Nothing is wrong - but a fan that is simply never being asked to spin must say
            // so too, or "0 RPM with no error" reads as a bug rather than a missing clock.
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            if (base != null && base.isServerSide()
                && base.getWorld()
                    .getTotalWorldTime() - lastCycleTick > 100) {
                return "no cycle requests - pulse the Turbine Control Block with redstone; RPM is decaying";
            }
        }
        return turbineHoldReason;
    }

    /**
     * Standing steam, in tenths of a 20-tick cycle slug (a cycle needs 10 = 1.0).
     * <p>
     * D-25 - ASSESSED AND DELIBERATELY LEFT ALONE while the EU output path was widened to long.
     * This {@code Integer.MAX_VALUE} is not the same animal as the one that was on
     * {@link #outputCeiling}. It is a RATIO, not an energy figure: the value is a fill level
     * against the buffer's own cap, so in practice it lives between 0 and about 10 and the
     * clamp only exists so a degenerate {@code steamPerTick} cannot hand the GUI a nonsense
     * width. Nothing downstream of it is EU, it is synced as an int because it IS an int, and
     * widening it would change nothing except to make it look like it was part of this fix.
     */
    public int getSteamBufferSlugsTenths() {
        final long slug = CYCLE_SLUG_TICKS * steamPerTick;
        if (slug <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, steamBuffer * 10 / slug);
    }

    /**
     * True when a programmed circuit is driving the rods instead of the GUI field.
     * <p>
     * Tidy B2: read straight off the controller slot with the same test the campaign commit uses,
     * rather than by comparing the committed N with the field. The committed N only moves at the
     * next campaign, so the old comparison read "overridden" for a whole campaign after a player
     * merely edited the field. Server-side; the GUI syncs it (AXIS 1). Display only - nothing in
     * the plant's logic calls this.
     */
    public boolean isThrottleOverridden() {
        final ItemStack circuit = getControllerSlot();
        return GTUtility.isAnyIntegratedCircuit(circuit) && circuit.getItemDamage() >= 1;
    }

    /**
     * The rods the Programmed Circuit in the controller slot asks for, clamped the way the
     * campaign commit clamps it; 0 when no circuit overrides the field. Read live off the slot,
     * so the Rods field's tooltip quotes the circuit now in the slot rather than the N committed
     * at the last campaign start ({@link #getThrottleN}). Server-side; the GUI syncs it (AXIS 1).
     * Display only.
     */
    public int getCircuitThrottle() {
        if (!isThrottleOverridden()) return 0;
        return Math.min(maxThrottle(), getControllerSlot().getItemDamage());
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

    /** The BEST burning grade's fuel multiplier - the label on the cascade line, not the math. */
    public int getActiveFuelMult() {
        return activeFuelMult;
    }

    /**
     * R7: the pre-throttle steam the campaign was actually built on - the per-pillar sum. This
     * is what the math panel's step 1 prints, so the printed identity holds no matter how many
     * grades are burning at once. 0 = nothing committed yet.
     */
    public long getFuelBaseSteam() {
        return fuelBaseSteam;
    }

    /** R7: "32x9 + 13x1" while several grades burn together; empty while the load is uniform. */
    public String getFuelGradeMix() {
        return fuelGradeMix;
    }

    /** The EFFECTIVE control-rod setting as the raw N of N/24 (a circuit may override it). */
    public int getThrottleN() {
        return Math.min(maxThrottle(), Math.max(1, throttleN));
    }

    /** Campaign steam after the Spine's speed AND efficiency - what the EU math multiplies. */
    public long getSteamAfterSpine() {
        return spineBoostedSteam(true);
    }

    /**
     * The EU/t the plant is asking for RIGHT NOW, from the live RPM and rotor state, before the
     * ceiling clamps it. {@link #getLastEUDemand} is the last GENERATING tick's figure and goes
     * stale the moment the campaign pauses; the math panel has to keep deriving.
     */
    public long getLiveEUDemand() {
        return euDemand();
    }

    /**
     * D-24: CLAMPED to the live cap, not the raw stored field. The tree's rod nodes are
     * refundable, so the cap is the first limit in this plant that can go DOWN - and every
     * consumer of the setting (the cascade at {@code throttleCap}, {@link #getThrottleN},
     * {@link #isThrottleOverridden}) already re-clamps, so an unclamped getter would only ever
     * have made the GUI field print a number the plant refuses to run at.
     */
    public int getThrottleSetting() {
        return Math.min(maxThrottle(), Math.max(1, throttleSetting));
    }

    /**
     * D-23: clamped to the UNLOCKED cap, server side. The GUI field's own range is the full
     * 1..30 because a MUI2 text field's bounds are fixed at build time and the client does not
     * get to decide what this plant may do - so a player without the unlock can type 30, and
     * this clamps it straight back to 24 and the field re-syncs. The safe direction: the field
     * offering a value the server refuses, never the field refusing one the server allows.
     */
    public void setThrottleSetting(int value) {
        throttleSetting = Math.min(maxThrottle(), Math.max(1, value));
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

    /**
     * Legacy readout hook. The Turbine Hall module is RETIRED (A3) - the Rotor Assembly is the
     * plant's only generator now - so nothing can ever be "installed" here again. Kept so the
     * shared {@link #outputLine} formatter and the GUI keep their signatures.
     */
    public boolean isTurbineInstalled() {
        return false;
    }

    public int getCoolingTowers() {
        return coolingTowerCount();
    }

    // ------------------------------------------------- D-17 plant modules, for the shared panel

    public boolean isVentStackBuilt() {
        return ventStackBuilt;
    }

    public boolean isSwitchyardBuilt() {
        return switchyardBuilt;
    }

    public boolean isDerrickBuilt() {
        return derrickBuilt;
    }

    public boolean isAnnexBuilt() {
        return annexBuilt;
    }

    public int getDerrickCycles() {
        return derrickCycles;
    }

    public int getAnnexCycles() {
        return annexCycles;
    }

    /** Why the Derrick or the Annex is not turning over ("" while both are happy or absent). */
    public String getModuleStall() {
        return moduleStall;
    }

    /**
     * One-line inventory of the four D-17 modules, faults and all - the towers' own line, for
     * the other four buildings. Same rule as {@link #getModulesSummary}: an ABSENT module is a
     * choice and reads grey, a module with blocks up that will not form is a FAULT and says why.
     */
    public String getPlantModulesSummary() {
        final StringBuilder line = new StringBuilder();
        appendModule(line, "Vent Stack", ventStackBuilt);
        appendModule(line, "Switchyard", switchyardBuilt);
        appendModule(line, "Derrick", derrickBuilt);
        appendModule(line, "Annex", annexBuilt);
        appendModuleFault(line, "Vent Stack", ventStackFault);
        appendModuleFault(line, "Switchyard", switchyardFault);
        appendModuleFault(line, "Derrick", derrickFault);
        appendModuleFault(line, "Annex", annexFault);
        return line.toString();
    }

    private static void appendModule(StringBuilder line, String name, boolean built) {
        if (line.length() > 0) line.append(" · ");
        line.append(
            built ? EnumChatFormatting.GREEN + name + EnumChatFormatting.RESET
                : EnumChatFormatting.GRAY + "no " + name + EnumChatFormatting.RESET);
    }

    private static void appendModuleFault(StringBuilder line, String name, String fault) {
        if (fault.isEmpty()) return;
        line.append(EnumChatFormatting.YELLOW)
            .append(" | ")
            .append(name)
            .append(" NOT FORMED: ")
            .append(fault)
            .append(EnumChatFormatting.RESET);
    }

    /** Fuel rods standing in the silo's Rod Input Bus, ready to be pushed into the pillars. */
    public int getRodsQueued() {
        if (reactorTier <= 0) return 0;
        int queued = 0;
        for (int type = ROD_URANIUM; type <= ROD_NAQ; type++) {
            queued += countMatchingInputs(new ItemStack(fuelRodFor(type), 1));
        }
        return queued;
    }

    /** Empty pillar segments the rod bus still has to fill. */
    public int getPillarGaps() {
        return pillarGaps();
    }

    /** Litres standing in the Spiral chamber's bulk Propellant Input Hatch. */
    public int getPropellantLitres() {
        int litres = 0;
        for (final MTEHatchInput hatch : GTUtility.validMTEList(propellantHatches)) {
            litres += hatch.getFluidAmount();
        }
        return litres;
    }

    /**
     * M8: the TC-7 interlock's own verdict on the NEXT cycle - can the chamber pay for the RPM
     * that cycle would reach? Consumes nothing (coolantRungAvailable simulates the drain).
     * <p>
     * This is deliberately the interlock's question and not "is there any fluid in the hatch".
     * The two only agree at the extremes: a hatch holding a splash of water can cool a load of
     * 1 and still be refused for the speed the fan is about to take, and that middle state is
     * exactly where a player sits staring at a plant that will not accelerate. Reporting the
     * same condition the interlock refuses on is the only way the panel and the machine can
     * never disagree.
     */
    public boolean isChamberCoolantReady() {
        if (!spiralBuilt) return false;
        return coolantRungAvailable(chamberLoad(projectedRpmMilli(Math.max(propellantMilli, lastCyclePropMilli)))) >= 0;
    }

    /**
     * M8: TC-7's OTHER refusal, asked in the interlock's OWN words.
     * <p>
     * D-21: this was {@code propellantMilli <= 0 && getPropellantLitres() <= 0}, and that second
     * term is a question the interlock never asks. The bulk hatch's litres are RAW litres, and
     * {@link #drainBulkPropellant()} refuses everything the ladder does not recognise
     * ({@code propellantMilliOf(..) <= 1000}) - so a PROP hatch full of an unrecognised fluid
     * stocks no injector, the interlock refuses every cycle as DRY, and this line looked at the
     * same hatch, saw litres, and reported the plant fed. The HOLD line and the weakest-term
     * line then named two different faults on one plant, which is the exact failure the shared
     * formatters exist to make impossible. Both now call {@link #injectorsDryAt}.
     */
    public boolean isInjectorsDry() {
        if (!spiralBuilt) return false;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return false;
        return injectorsDryAt(
            loadedInjectorCount(),
            base.getWorld()
                .getTotalWorldTime());
    }

    /** Litres standing in the Spiral chamber's Coolant Input Hatch. */
    public int getChamberCoolantLitres() {
        int litres = 0;
        for (final MTEHatchInput hatch : GTUtility.validMTEList(chamberHatches)) {
            litres += hatch.getFluidAmount();
        }
        return litres;
    }

    /** Litres standing in the silo's Feedwater Input Hatch. */
    public int getFeedwaterLitres() {
        int litres = 0;
        for (final MTEHatchInput hatch : GTUtility.validMTEList(feedwaterHatches)) {
            litres += hatch.getFluidAmount();
        }
        return litres;
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

    /** Retired with the Turbine Hall module (A3); the fan items are museum pieces. */
    public int installedFanTier() {
        return 0;
    }

    /**
     * One-line inventory of the absorbed buildings for the GUI (A3: towers are pieces).
     * <p>
     * R1-B: a tower that has blocks standing and still refuses to form now says why, right
     * here. It used to fail with the error list set to {@code null}, so a 0.19.0 tower whose
     * base ring still held its Maintenance Hatch simply stopped counting - no message
     * anywhere, and half the plant's water saving gone with it.
     */
    public String getModulesSummary() {
        final int towers = coolingTowerCount();
        final StringBuilder line = new StringBuilder(towers == 0 ? "no cooling towers" : "Cooling Tower x" + towers);
        if (!towerFrontFault.isEmpty()) {
            line.append(EnumChatFormatting.YELLOW)
                .append(" | front-left tower NOT FORMED: ")
                .append(towerFrontFault)
                .append(EnumChatFormatting.RESET);
        }
        if (!towerBackFault.isEmpty()) {
            line.append(EnumChatFormatting.YELLOW)
                .append(" | back-left tower NOT FORMED: ")
                .append(towerBackFault)
                .append(EnumChatFormatting.RESET);
        }
        return line.toString();
    }

    /**
     * Energy Hatches fitted to the output manifold. C5: NOT furniture - since 0.21.0 these carry
     * the only EU load in the class, {@link #moduleSupplied} for the Derrick and the Annex. They
     * are optional because the REACTOR does not need them, not because nothing does.
     */
    public int getEnergyHatchCount() {
        return mEnergyHatches.size();
    }

    // ------------------------------------------------------------------ readout (shared)

    /**
     * Every readout the plant has - the controller GUI, BOTH control blocks and the scanner -
     * is composed from these builders, so no panel can drift from another and no number is
     * only visible in one place. They are static and take primitives so the client-side GUI
     * can format them straight from its sync values.
     */
    public static String header(String group) {
        return EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + group + EnumChatFormatting.RESET;
    }

    /**
     * Tidy B2/B3: the rods are quoted in the one unit the GUI field, the milestones and the tree
     * all use (N/24), and a Programmed Circuit that is overriding the field says so here, on the
     * line a player reads first, instead of the field silently doing nothing.
     */
    public static String tierLine(int tier, int edge, int rodsN, boolean circuitOverride) {
        return "Tier " + tier
            + " · "
            + edge
            + "x"
            + edge
            + " octagon · rods "
            + rodsN
            + "/24"
            + (circuitOverride
                ? EnumChatFormatting.YELLOW + " (Programmed Circuit - remove it to use the field)"
                    + EnumChatFormatting.RESET
                : "");
    }

    /**
     * THE COOLING TOWERS, on a line of their OWN, on every panel and in EVERY state.
     * <p>
     * D-21: the tower inventory used to be the tail of {@link #tierLine}, which only exists
     * once a silo stands - and {@code statusReport} papered over that with a second, differently
     * worded line in its no-silo branch that the controller GUI never rendered at all. So on a
     * BARE PLATFORM - the one state where a tower fault is most likely, and the only state where
     * the tower IS the build - the reason a tower would not form was invisible in the only panel
     * a player can reach: the Silo Control Block is itself the silo face's 'p' socket, so it does
     * not exist yet. One line, always drawn, is also what makes the controller tooltip's promise
     * ("prints the reason beside \"Cooling towers:\" on every panel") literally true.
     */
    public static String towerLine(String modules) {
        return "Cooling towers: " + modules;
    }

    /**
     * THE OTHER FOUR BUILDINGS, on a line of their own, on every panel and in EVERY state - the
     * lesson of D-21 applied the moment the pieces were added rather than a release later. All
     * four are most likely to be built on a BARE PLATFORM, which is exactly the state where the
     * Silo Control Block does not exist yet (it is one of the silo face's sockets), so this line
     * is UNGATED: the controller GUI is the only panel a player can reach then, and a module
     * that has blocks up and will not form has to be able to say so there.
     */
    public static String moduleLine(String modules) {
        return "Plant modules: " + modules;
    }

    /**
     * D-17 3/4: the two FEEDSTOCK modules, their tallies and whatever is holding them up. Only
     * drawn when at least one of them stands - a plant with neither has nothing to say here and
     * the line would be four words of noise on every panel forever.
     * <p>
     * Tidy B4: the rates are the LIVE ones, built from the same static progression getters the
     * plant's own tick runs ({@link BTSProgression#moduleCycleTicks},
     * {@link BTSProgression#derrickTantalumEvery}, {@link BTSProgression#annexPellets}) on the
     * synced counters and node mask, so a REPROCESSING 6 plant reads "/ 8 s", not the base
     * "/ 10 s" (AXIS 1: the client derives, it reads no field). The Annex's yellowcake is the
     * batch AVERAGE ({@code annexYellowcake} carries the fraction between batches).
     */
    public static String selfFuelLine(boolean derrick, boolean annex, int derrickCycles, int annexCycles, String stall,
        long[] progress, long nodes) {
        // C2: a SUSPENDED batch is reported by a module that is no longer standing, so "neither
        // module is built" can no longer be the whole test - it would hide the one line that
        // tells a player their paid-for uraninite and acid are being held for them.
        if (!derrick && !annex && (stall == null || stall.isEmpty())) return "";
        final double derrickSeconds = BTSProgression.moduleCycleTicks(progress, nodes, DERRICK_CYCLE_TICKS) / 20.0;
        final double annexSeconds = BTSProgression.moduleCycleTicks(progress, nodes, ANNEX_CYCLE_TICKS) / 20.0;
        final int tantalumEvery = BTSProgression.derrickTantalumEvery(progress, nodes, DERRICK_TANTALUM_EVERY);
        final double cakes = ANNEX_YELLOWCAKE * BTSProgression.annexPellets(progress, nodes, 1000) / 1000.0;
        final String head = "Feedstock · Derrick "
            + (derrick
                ? String.format("%,d", derrickCycles) + " loads ("
                    + DERRICK_URANINITE
                    + " Uraninite + "
                    + DERRICK_THORIUM
                    + " Thorium / "
                    + plainNumber(derrickSeconds)
                    + " s, Tantalum every "
                    + ordinal(tantalumEvery)
                    + ")"
                : "-")
            + " · Annex "
            + (annex
                ? String.format("%,d", annexCycles) + " batches ("
                    + plainNumber(cakes)
                    + " Yellowcake + "
                    + ANNEX_THORIUM
                    + " Thorium / "
                    + plainNumber(annexSeconds)
                    + " s)"
                : "-");
        if (stall == null || stall.isEmpty()) return head;
        return head + EnumChatFormatting.YELLOW + " | " + stall + EnumChatFormatting.RESET;
    }

    /** "8" for a whole number, "2.4" otherwise - panel figures without a trailing ".0". */
    private static String plainNumber(double value) {
        if (value == Math.rint(value)) return String.format("%,d", (long) value);
        return String.format("%.1f", value);
    }

    /** "load" for 1, "2nd" / "3rd" / "4th" otherwise - the Derrick's tantalum interval. */
    private static String ordinal(int n) {
        if (n <= 1) return "load";
        final int mod100 = n % 100;
        final String suffix = mod100 >= 11 && mod100 <= 13 ? "th"
            : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }

    /**
     * D-17 6: THE GRID SOFT CAP, said out loud in every state it can be in.
     * <p>
     * The requirement this line exists for is that the cap must be impossible to hit without the
     * panel naming the cause AND the fix, in the same voice as the CLAMPED line beside it - it
     * is a live balance change, and a plant that made a billion EU/t yesterday must not simply
     * make a tenth of that today with nothing on screen to explain it. So all four states print:
     * cap disabled in the config, yard standing, yard missing but the plant still under the cap,
     * and yard missing with EU actually being discarded.
     */
    public static String switchyardLine(boolean built, boolean capEnabled, long demand, long ceiling) {
        if (!capEnabled) {
            return EnumChatFormatting.GRAY + "Grid: soft cap DISABLED in the config - no Switchyard needed"
                + EnumChatFormatting.RESET;
        }
        if (built) {
            return EnumChatFormatting.GREEN + "Grid: Switchyard conditioning the line - no output cap"
                + EnumChatFormatting.RESET;
        }
        final long wanted = Math.min(demand, ceiling);
        if (wanted <= GRID_SOFT_CAP) {
            return EnumChatFormatting.YELLOW + "Grid: no Switchyard - output SOFT-CAPPED at "
                + String.format("%,d", GRID_SOFT_CAP)
                + " EU/t. This plant wants "
                + String.format("%,d", wanted)
                + " and is still under it"
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.RED + "Grid: no Switchyard - the "
            + String.format("%,d", GRID_SOFT_CAP)
            + " EU/t soft cap is discarding "
            + String.format("%,d", wanted - GRID_SOFT_CAP)
            + " EU/t. BUILD THE SWITCHYARD: project this controller (or /btsbuild) and it raises the transformer yard on the platform's back edge, behind the silo - "
            + String.format("%,d", BTSBom.SWITCHYARD.of(BTSBom.SOLID_STEEL_MACHINE_CASING))
            + " "
            + BTSBom.SOLID_STEEL_MACHINE_CASING
            + " + "
            + String.format("%,d", BTSBom.SWITCHYARD.of(BTSBom.STEEL_FRAME_BOX))
            + " "
            + BTSBom.STEEL_FRAME_BOX
            + ", no hatches"
            + EnumChatFormatting.RESET;
    }

    public static String cascadeLine(boolean cascade, int live, int total, int columns, int cap, String fuel,
        long steam) {
        final String head = cascade ? "Cascade " + live + "/" + total + " pillars"
            : EnumChatFormatting.YELLOW + "Assembly fallback (output halved) "
                + columns
                + "/"
                + cap
                + EnumChatFormatting.RESET;
        return head + " · " + fuel + " · " + String.format("%,d", steam) + " L/t steam";
    }

    /**
     * THE SUPPLY LINES, in one line: what each point-of-use socket is holding and what the
     * plant still has to distribute from it. This is the readout that answers "why is my
     * output low" without walking the silo - a rod queue of 0 against non-zero pillar gaps is
     * a starved cascade, and 0 L of propellant is a fan that will not accelerate. The three
     * figures come from three different buildings now (silo rod bus, chamber propellant,
     * chamber coolant), which is exactly what D-20 asked for.
     */
    public static String supplyLine(boolean siloBuilt, int rodsQueued, int pillarGaps, int propellantLitres,
        int coolantLitres) {
        if (!siloBuilt) {
            return EnumChatFormatting.RED
                + "No Reactor Silo yet - its front face holds the Rod Input Bus, feedwater and output sockets"
                + EnumChatFormatting.RESET;
        }
        final EnumChatFormatting rodColor = pillarGaps == 0 ? EnumChatFormatting.GREEN
            : rodsQueued > 0 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
        return "Supply · " + rodColor
            + String.format("%,d", rodsQueued)
            + " rods queued -> "
            + String.format("%,d", pillarGaps)
            + " pillar gaps"
            + EnumChatFormatting.RESET
            + " · propellant "
            + String.format("%,d", propellantLitres)
            + " L · coolant "
            + String.format("%,d", coolantLitres)
            + " L";
    }

    public static String heatLine(int heat, int decaySeconds, int radiationSeconds, boolean ventStack) {
        final EnumChatFormatting color = heat >= SCRAM_HEAT ? EnumChatFormatting.RED
            : heat >= SCRAM_HEAT / 2 ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN;
        return color + "Heat "
            + String.format("%,d", heat)
            + "/"
            + String.format("%,d", MELTDOWN_HEAT)
            + EnumChatFormatting.RESET
            + " (SCRAM "
            + String.format("%,d", SCRAM_HEAT)
            + ") · decay "
            + (decaySeconds > 0 ? EnumChatFormatting.GOLD + "" + decaySeconds + " s" + EnumChatFormatting.RESET
                : "none")
            + " · radiation "
            // Tidy B4: RED, not DARK_RED - dark red is near-unreadable on GT's dark terminal.
            + (radiationSeconds > 0 ? EnumChatFormatting.RED + "" + radiationSeconds + " s" + EnumChatFormatting.RESET
                : "clear")
            // D-17 5: the Vent Stack halves BOTH of those windows, so it belongs on the line
            // that prints them and nowhere else. Tidy B4: no minutes quoted - the windows are
            // live (the tree's DECAY_WINDOW channel moves one of them) and the seconds left are
            // already printed above.
            + (ventStack ? EnumChatFormatting.GREEN + " · Vent Stack: windows halved" + EnumChatFormatting.RESET : "");
    }

    public static String spineLine(boolean built, String pattern, int housings, int speedMilli, int effMilli,
        int swirl) {
        if (!built) return EnumChatFormatting.GRAY + "Spine: not built" + EnumChatFormatting.RESET;
        return "Spine [" + pattern
            + "] "
            + housings
            + " housings · x"
            + String.format("%.2f", speedMilli / 1000.0)
            + " spd · x"
            + String.format("%.2f", effMilli / 1000.0)
            + " eff · swirl "
            + swirl
            + (swirl != 0 ? " (heat if uncooled)" : "");
    }

    public static String spiralLine(boolean built, int fitted, int timed, String propellant, int propMilli) {
        if (!built) return EnumChatFormatting.GRAY + "Spiral: not built" + EnumChatFormatting.RESET;
        final EnumChatFormatting timing = timed >= 8 ? EnumChatFormatting.GREEN
            : timed > 0 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
        return "Spiral " + fitted
            + "/8 injectors · timed "
            + timing
            + timed
            + "/8"
            + EnumChatFormatting.RESET
            + " · "
            + propellant
            + " · propellant x"
            + String.format("%.2f", propMilli / 1000.0);
    }

    public static String coolantLine(boolean built, boolean starved, boolean ready, String coolant, int drain) {
        if (!built) return EnumChatFormatting.GRAY + "Coolant: no chamber" + EnumChatFormatting.RESET;
        if (starved) {
            return EnumChatFormatting.RED
                + "Coolant STARVED - the Spiral chamber's Coolant Input Hatch holds no usable coolant"
                + EnumChatFormatting.RESET;
        }
        if (!ready) {
            // M8: at rest with a balanced Spine the chamber's load is 0, so the drain loop takes
            // its `load <= 0` early return and parks this line on "idle · 0 L/t" - which reads
            // as HEALTHY over a chamber with nothing in its hatch. Idle is not supplied, and the
            // difference is the entire reason the fan never leaves zero.
            return EnumChatFormatting.YELLOW
                + "Coolant short - the Coolant Input Hatch cannot cover the next cycle, so the interlock will refuse it"
                + EnumChatFormatting.RESET;
        }
        return "Coolant " + coolant + " · " + String.format("%,d", drain) + " L/t";
    }

    /**
     * D-23: the redline is a PARAMETER now, not the constant. TURBINE 5/6 raise it to 13,500
     * and 15,000, and a readout that kept quoting 12,000 would paint an overspeed-trimmed
     * plant as permanently redlined while it was still accelerating.
     * <p>
     * F4: {@code redline} is the OPERATING one - the unlocked ceiling capped by the RPM LIMIT
     * field - and {@code unlocked} is the ceiling itself, so a plant sitting at the design
     * 12,000 with an unused unlock is told there is headroom it has not asked for rather than
     * being pushed into it.
     */
    public static String rpmLine(int rpm, int redline, int unlocked, int cyclesPerMinute, int bufferSlugsTenths,
        boolean benchClock) {
        final EnumChatFormatting color = rpm >= redline ? EnumChatFormatting.RED
            : rpm > 0 ? EnumChatFormatting.GREEN : EnumChatFormatting.GRAY;
        return color + "RPM "
            + String.format("%,d", rpm)
            + EnumChatFormatting.RESET
            + "/"
            + String.format("%,d", redline)
            + (redline > RPM_REDLINE ? EnumChatFormatting.GOLD + " (overspeed)" + EnumChatFormatting.RESET : "")
            + (unlocked > redline
                ? EnumChatFormatting.GOLD + " (up to "
                    + String.format("%,d", unlocked)
                    + " unlocked - set the RPM Limit field)"
                    + EnumChatFormatting.RESET
                : "")
            + " redline · "
            + cyclesPerMinute
            + " cyc/min ("
            + (benchClock ? EnumChatFormatting.LIGHT_PURPLE + "bench clock" + EnumChatFormatting.RESET : "redstone")
            + ") · steam buffer "
            + String.format("%.1f", bufferSlugsTenths / 10.0)
            + " cycles";
    }

    public static String rotorLine(boolean built, int stages, int avgMilli, String wear) {
        if (!built) return EnumChatFormatting.GRAY + "Rotor Assembly: not built" + EnumChatFormatting.RESET;
        final EnumChatFormatting color = stages >= GALLERY_STAGES && avgMilli >= 900 ? EnumChatFormatting.GREEN
            : stages > 0 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
        final String head = color + "Rotors " + stages + "/" + GALLERY_STAGES + EnumChatFormatting.RESET;
        // Tidy B4: an empty gallery said "wear - - - - - - - - (of 10)"; say what it means.
        if (stages <= 0) return head + " · no rotors seated";
        return head + " · avg x"
            + String.format("%.2f", avgMilli / 1000.0)
            + " · wear "
            + (wear == null || wear.isEmpty() ? "-" : wear)
            + " (of 10)";
    }

    public static String holdLine(String reason) {
        return EnumChatFormatting.YELLOW + "HOLD: " + reason + EnumChatFormatting.RESET;
    }

    /**
     * D-17: {@code gridCapped} splits the one word this line used to have for "you are not
     * getting all of it". A CLAMP is the fitted hatches; a GRID CAP is a missing building, and
     * telling a player to fit more Laser Source Hatches when the answer is a Switchyard is the
     * whole failure mode the shared formatters exist to make impossible.
     */
    public static String outputLine(boolean galleryActive, boolean hallInstalled, long emitted, long demand,
        boolean clamped, boolean gridCapped, long steamOut) {
        if (!galleryActive && !hallInstalled) {
            return "Steam export " + String.format("%,d", steamOut) + " L/t (no generator)";
        }
        return "Generating " + EnumChatFormatting.GREEN
            + String.format("%,d", emitted)
            + " EU/t"
            + EnumChatFormatting.RESET
            + (galleryActive ? " (Rotor Assembly)" : " (legacy Turbine Hall)")
            + (clamped
                ? EnumChatFormatting.RED + (gridCapped ? " [GRID-CAPPED from " : " [CLAMPED from ")
                    + String.format("%,d", demand)
                    + (gridCapped ? " - no Switchyard]" : "]")
                    + EnumChatFormatting.RESET
                : "");
    }

    /**
     * D-25: no more {@code " (capped at 2^31-1)"} suffix. The ceiling is not capped any more -
     * {@link #distributeOutput} injects in long - so the line now prints the fitted hatches'
     * true total and the three numbers on it always add up. A plant reading
     * {@code Ceiling 2,199,023,255,552 EU/t = dynamos 0 + lasers 2,199,023,255,552} is four UMV
     * 4096A laser sources, and that is exactly what it now gets to emit.
     */
    /**
     * D-25: WHICH MODE THE OVER-INJECTION GUARD IS IN, and what it has caught. Printed
     * UNCONDITIONALLY beside the ceiling, in all four states, from the one formatter the GUI,
     * both control blocks and the scanner share.
     * <p>
     * Unconditional is the point. The failure this line exists to prevent is somebody testing
     * for an hour believing they are safe while the plant is armed, or trusting an armed plant
     * that has quietly been disarmed since they last looked - and a line that only appears in
     * one of the two modes cannot prevent the second of those. It sits next to
     * {@link #ceilingLine} because that is where a player is already reading "can my hatch row
     * take this", which is the same question the guard answers destructively.
     */
    public static String outputSafetyLine(boolean testMode, int overruns, long offered, long capacity, long dynamo,
        long laser) {
        if (!testMode) {
            // ARMED. Calm, grey, one line - this is normal play and must not read as an alarm.
            return EnumChatFormatting.GRAY
                + "Output safety: armed (output above dynamo + laser capacity explodes the plant, as in GT)"
                + EnumChatFormatting.RESET;
        }
        if (overruns <= 0) {
            return EnumChatFormatting.LIGHT_PURPLE
                + "OUTPUT SAFETY ON (test mode) - over-injection will NOT explode this plant; it is clamped and reported here. /btssafety off to re-arm."
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.RED + "!! OUTPUT OVER-INJECTION CAUGHT "
            + overruns
            + "x - THIS PLANT WOULD HAVE EXPLODED IN NORMAL PLAY !! offered "
            + String.format("%,d", offered)
            + " EU/t into a hatch row that can absorb "
            + String.format("%,d", capacity)
            + " (dynamos "
            + String.format("%,d", dynamo)
            + " + lasers "
            + String.format("%,d", laser)
            + "), short by "
            + String.format("%,d", offered - capacity)
            + " EU/t - "
            + shortRowName(dynamo, laser)
            + ". Clamped and kept running because output safety is ON."
            + EnumChatFormatting.RESET;
    }

    public static String ceilingLine(long ceiling, long dynamo, long laser) {
        return "Ceiling " + String.format("%,d", ceiling)
            + " EU/t = dynamos "
            + String.format("%,d", dynamo)
            + " + lasers "
            + String.format("%,d", laser);
    }

    /**
     * C1: the manifold's four Energy Hatch sockets, as a NOTE and never as an error.
     * <p>
     * The plant does not consume EU - not idling, not starting, not running - so an empty E
     * socket costs exactly nothing. It used to un-form the whole plant the moment the gallery
     * housings went in, three steps before the book asks for the hatches. Saying "unfinished"
     * is honest; stopping a running reactor over it was not.
     */
    public static String energyHatchNote(boolean galleryBuilt, int fitted, boolean moduleLoad) {
        // Tidy B4: the reactor never draws EU, so without a Derrick or an Annex there is nothing
        // these sockets feed and nothing worth a line.
        if (!galleryBuilt || !moduleLoad) return "";
        // D-17: the Extraction Derrick and the Enrichment Annex are the FIRST things in this
        // plant that consume EU, and they consume it through these sockets. C1 still holds -
        // they are a NOTE and never a structure demand, because the reactor and the turbine run
        // perfectly without them - but on a plant carrying one of those two modules, "furniture"
        // would be a lie, and with no hatch at all the modules simply never turn over.
        if (fitted <= 0) {
            return EnumChatFormatting.RED
                + "Manifold Energy Hatches 0/4 - the Derrick and the Annex draw their EU through these, so neither will run"
                + EnumChatFormatting.RESET;
        }
        if (fitted >= 4) return "";
        return EnumChatFormatting.GRAY + "Manifold Energy Hatches "
            + fitted
            + "/4 - they power the Derrick and the Annex"
            + EnumChatFormatting.RESET;
    }

    public static String laserLine(int fitted, int painted) {
        if (fitted == 0) {
            return EnumChatFormatting.YELLOW + "Laser sockets empty - output manifold back face, 6th row up (4 cells)"
                + EnumChatFormatting.RESET;
        }
        if (painted < fitted) {
            return EnumChatFormatting.YELLOW + "Lasers "
                + fitted
                + "/4 fitted · "
                + painted
                + " painted - an unpainted source cannot fire"
                + EnumChatFormatting.RESET;
        }
        return "Lasers " + fitted + "/4 fitted · " + painted + " painted";
    }

    // ------------------------------------------------------------------ B2: the math panel
    // "Show the math like the Water Line does": four numbered lines that walk the CURRENT
    // EU/t from the pillars to the manifold, every factor labelled with the value it has right
    // now, plus a fifth line naming the term that is costing the most. They are static and
    // take primitives, so the client GUI, both control blocks and the scanner print the same
    // derivation from the same shared arithmetic (steamAfterSpine / euDemandOf).

    /**
     * R7: the campaign's PRE-THROTTLE steam, as the reactor computed it. The cascade sums one
     * term per pillar - {@code sum(16,384 x ROD_MULT[bottom rod])} - so {@code live x best
     * grade x 16,384} overstates every mixed load. {@code fuelBase <= 0} means "nothing
     * committed yet, or a world saved before this fix": fall back to the plain product, which
     * is exactly right for the uniform load such a world must have had.
     */
    public static long fuelBaseOf(boolean cascade, int live, int fuelMult, long fuelBase) {
        if (fuelBase > 0) return fuelBase;
        return (long) live * Math.max(1, fuelMult) * STEAM_PER_COLUMN / (cascade ? 1 : 2);
    }

    /**
     * The AVERAGE burning grade behind that base, x1000 (naquadah-only = 9,000). This is the
     * number the fuel term must be scored against: a silo running 32 naquadah pillars and 13
     * uranium ones is at x6.69, not the x9 its best rod would claim. The fallback's /2 is
     * divided back out - that penalty is the batch's, not the grade's.
     */
    public static int avgFuelMilli(boolean cascade, int live, int fuelMult, long fuelBase) {
        if (live <= 0) return Math.max(1, fuelMult) * 1000;
        final long base = fuelBaseOf(cascade, live, fuelMult, fuelBase);
        return (int) Math.max(1L, base * (cascade ? 1000L : 2000L) / ((long) live * STEAM_PER_COLUMN));
    }

    /** Step 1 - the reactor: the per-pillar steam sum, then the control-rod throttle. */
    public static String mathSteamLine(boolean cascade, int live, int fuelMult, int throttleN, long fuelBase,
        String gradeMix, long steam) {
        final long base = fuelBaseOf(cascade, live, fuelMult, fuelBase);
        final boolean mixed = gradeMix != null && !gradeMix.isEmpty();
        // Mixed load: name the average AND the census, so "why is my steam low" answers itself
        // instead of hiding behind the best rod in the silo.
        final String grade = mixed
            ? "avg x" + String.format("%.2f", avgFuelMilli(cascade, live, fuelMult, fuelBase) / 1000.0)
                + " fuel ("
                + gradeMix
                + ")"
            : "x" + Math.max(1, fuelMult) + " fuel";
        final String head = EnumChatFormatting.GRAY + "1) "
            + EnumChatFormatting.RESET
            + String.format("%,d", live)
            + (cascade ? " pillars " : " assemblies /2 ")
            + grade
            + " x"
            + String.format("%,d", STEAM_PER_COLUMN);
        final String tail = EnumChatFormatting.AQUA + String.format("%,d", steam) + " L/t" + EnumChatFormatting.RESET;
        // At full throttle the base IS the output, so printing it twice only adds noise.
        // D-23: EQUAL to 24, not >= 24. With the BURNUP unlocks a plant can sit at 26..30/24,
        // and the short form would have printed "(rods 24/24)" over a 25% overdrive.
        return throttleN == 24 ? head + " = " + tail + " (rods 24/24)"
            : head + " = " + String.format("%,d", base) + " · rods " + throttleN + "/24 -> " + tail;
    }

    /** Step 2 - the Spine: speed carries throughput, efficiency carries EU per litre. */
    public static String mathSpineLine(int speedMilli, int effMilli, long afterSpine) {
        return EnumChatFormatting.GRAY + "2) "
            + EnumChatFormatting.RESET
            + "x"
            + String.format("%.2f", speedMilli / 1000.0)
            + " spine speed x"
            + String.format("%.2f", effMilli / 1000.0)
            + " efficiency = "
            + EnumChatFormatting.AQUA
            + String.format("%,d", afterSpine)
            + " L/t"
            + EnumChatFormatting.RESET
            + " through the chain";
    }

    /**
     * Step 3 - the fan: RPM/500 (x24 at the stock redline), the average rotor factor, D-23's
     * MILESTONE term and D-24's UPGRADE-TREE term.
     * <p>
     * Each progression factor is printed whenever it is not x1.000, because both are real terms of
     * {@link #euDemandOf} and a derivation that hid it would be lying about its own
     * arithmetic. At x1.000 nothing is printed at all, so a plant with no progress sees the
     * exact line it saw before this system existed.
     */
    public static String mathTurbineLine(int rpm, int rotorMilli, int progMilli, int upgradeMilli, long demand) {
        return mathTurbineLine(rpm, rotorMilli, progMilli, upgradeMilli, 1000, demand);
    }

    /**
     * D-26: the same line with the FEEDWATER term printed as its own labelled factor, in the same
     * position {@link #euDemandOf} performs it.
     * <p>
     * It follows the two progression terms' rule exactly: printed only when it is not x1.000,
     * because a derivation that hid a factor it actually used would be lying about its own
     * arithmetic - and, in the other direction, a plant running plain water sees the EXACT line it
     * saw in 0.23.0, character for character. That is the visible half of the regression test;
     * {@link #euDemandOf} is the arithmetic half.
     * <p>
     * The water factor is AQUA where the progression terms are GOLD, because it is a different
     * kind of thing: milestones and upgrades are earned once and kept, while this one is a live
     * property of what is in the hatch this tick and stops the moment the good water runs out.
     */
    public static String mathTurbineLine(int rpm, int rotorMilli, int progMilli, int upgradeMilli, int waterMilli,
        long demand) {
        return EnumChatFormatting.GRAY + "3) "
            + EnumChatFormatting.RESET
            + "x RPM "
            + String.format("%,d", rpm)
            + "/500 (x"
            + String.format("%.2f", rpm / 500.0)
            + ") x rotors x"
            + String.format("%.2f", rotorMilli / 1000.0)
            + (progMilli > 1000
                ? EnumChatFormatting.GOLD + " x milestones x"
                    + String.format("%.3f", progMilli / 1000.0)
                    + EnumChatFormatting.RESET
                : "")
            + (upgradeMilli > 1000
                ? EnumChatFormatting.GOLD + " x upgrades x"
                    + String.format("%.3f", upgradeMilli / 1000.0)
                    + EnumChatFormatting.RESET
                : "")
            + (waterMilli > 1000
                ? EnumChatFormatting.AQUA + " x feedwater x"
                    + String.format("%.2f", waterMilli / 1000.0)
                    + EnumChatFormatting.RESET
                : "")
            + " = "
            + EnumChatFormatting.AQUA
            + String.format("%,d", demand)
            + " EU/t"
            + EnumChatFormatting.RESET
            + " demand";
    }

    /**
     * D-26: the STEAM CHAIN group's feedwater row. Empty below Grade 1, which is the whole point -
     * a plant on plain or distilled water gets no new line, no new instruction and no suggestion
     * that it is doing anything wrong. The purified grades are a bonus a player opts into, never a
     * requirement, and the panel says so out loud on the one line where somebody might wonder.
     * <p>
     * The multiplier is read back out of {@link BTSWaterGrade#outputMilli} rather than passed in,
     * so this line and {@link #euDemandOf}'s fifth factor come from one table and cannot drift.
     */
    public static String feedwaterLine(int grade, boolean galleryActive) {
        if (grade < 1) return "";
        final String head = EnumChatFormatting.AQUA + "Feedwater: " + BTSWaterGrade.label(grade);
        final String mult = String.format("%.2f", BTSWaterGrade.outputMilli(grade) / 1000.0);
        // R9's rule, applied to this term too: a plant with no Turbine Generator derives no EU at
        // all, so telling it that its water is worth "output x1.20" would be the same lie the
        // numbered steps used to tell it about its non-existent rotors. The multiplier is real and
        // it is banked - it just does not pay until something turns those litres into EU.
        if (!galleryActive) {
            return head + " - x"
                + mult
                + " once a Rotor Assembly turns those litres into EU"
                + EnumChatFormatting.RESET
                + EnumChatFormatting.GRAY
                + " (a steam-export plant is paid in litres, and the grade moves no litres)"
                + EnumChatFormatting.RESET;
        }
        return head + " - output x"
            + mult
            + EnumChatFormatting.RESET
            + EnumChatFormatting.GRAY
            + " (plain and distilled water run at x1.00 - the grade is a bonus, not a requirement)"
            + EnumChatFormatting.RESET;
    }

    /**
     * Step 4 - the manifold: the demand clamped to what the fitted hatches can absorb, AND to
     * what the grid will take without a Switchyard. {@code gridCap} is {@code Long.MAX_VALUE}
     * whenever the yard stands or the config flag is off, so the two cases stay one branch of
     * arithmetic and separate branches of ADVICE - which is the only part that differs.
     */
    public static String mathClampLine(long demand, long ceiling, long gridCap) {
        final long limit = Math.min(ceiling, gridCap);
        final long emitted = Math.min(demand, limit);
        if (ceiling <= 0) {
            return EnumChatFormatting.GRAY + "4) "
                + EnumChatFormatting.YELLOW
                + "no Dynamo or Laser Source fitted - nothing can leave"
                + EnumChatFormatting.RESET;
        }
        if (demand > limit) {
            final boolean grid = gridCap < ceiling;
            return EnumChatFormatting.GRAY + "4) "
                + EnumChatFormatting.RED
                + (grid ? "GRID-CAPPED at " : "CLAMPED at ceiling ")
                + String.format("%,d", limit)
                + " -> "
                + String.format("%,d", emitted)
                + " EU/t - "
                + (grid ? "build the Switchyard" : "fit more Laser Source Hatches")
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.GRAY + "4) "
            + EnumChatFormatting.RESET
            + "under ceiling "
            + String.format("%,d", ceiling)
            + " -> "
            + EnumChatFormatting.GREEN
            + String.format("%,d", emitted)
            + " EU/t"
            + EnumChatFormatting.RESET
            + " out";
    }

    /**
     * R9: the MATH group's own title. A plant with no Turbine Generator derives no EU at all,
     * so promising "how that EU/t is derived" above a steam-export readout was the first lie
     * on the panel.
     * <p>
     * R1-B (B1): the gate is {@code galleryActive} - housings AND rotors - which is what the
     * OUTPUT group uses. On {@code galleryBuilt && galleryStages == 0}, a perfectly formable
     * state (the 'm' dynamo, 'L' laser and '9' stage sockets all chain to plain casing when
     * blank), the two disagreed: OUTPUT printed "Steam export N L/t (no generator)" and the
     * MATH group directly beneath it printed the EU derivation, closing on "4) no Dynamo or
     * Laser Source fitted - nothing can leave". Both about the same plant, in the same tick.
     */
    public static String mathHeader(boolean galleryActive) {
        return header(galleryActive ? "MATH · how that EU/t is derived" : "MATH · how that steam figure is derived");
    }

    /**
     * R9: steps 3 and 4, EXPORT variant. With no Turbine Generator there is no fan term, no
     * rotor term and no output ceiling - the chain's steam IS the product, and it leaves on
     * spine SPEED alone ({@code runFluidTick} exports {@code spineBoostedSteam(false)}).
     * Printing the generator steps here told a correctly running export plant that its
     * non-existent rotors were broken.
     */
    public static String mathExportLine(int speedMilli, long steamOut) {
        return EnumChatFormatting.GRAY + "3) "
            + EnumChatFormatting.RESET
            + "no Rotor Assembly - this is a steam export plant: x"
            + String.format("%.2f", speedMilli / 1000.0)
            + " spine speed = "
            + EnumChatFormatting.AQUA
            + String.format("%,d", steamOut)
            + " L/t"
            + EnumChatFormatting.RESET
            + " out of the silo's Output Hatch · spine efficiency and RPM only pay once rotors turn";
    }

    /**
     * The one line a newcomer actually needs: of every factor above, WHICH is furthest from its
     * best value, and what to do about it. Ratios are taken against each term's own ceiling
     * (pillar cap, naquadah rods, 24/24 rods, an all-riding spine, the redline, fresh rotors).
     * <p>
     * R7: the fuel term is scored on {@code fuelMilli}, the AVERAGE grade actually burning
     * ({@link #avgFuelMilli}) - scoring the best rod in the silo hid every mixed load.
     * <p>
     * R9: with no Turbine Generator the fan, the rotors and the output ceiling are not terms of
     * anything - the plant exports steam, which spine SPEED alone carries. Scoring them anyway
     * told a healthy export plant to "seat 8 fresh Turbine Rotors" it has nowhere to put.
     * <p>
     * R1-B (B2): a SCORE and a FIX are different questions, and R9 answered only the first.
     * Every optional piece scores its missing multiplier honestly - an absent Spine really does
     * leave the plant at x1.00 of x1.55, so 64% is the right number - but the ADVICE has to
     * name the piece that is not there instead of telling the player to wrench four housings
     * that do not exist. Every branch below therefore takes the piece's BUILT flag: score by
     * the number, advise by what actually stands. The gallery gate is {@code galleryActive}
     * (housings AND rotors), the same condition the OUTPUT group and the numbered steps use, so
     * a gallery with no rotors in it is scored as the steam-export plant it is behaving as.
     */
    public static String mathLimiterLine(boolean spineBuilt, int spineHousings, boolean spiralBuilt,
        boolean galleryBuilt, boolean galleryActive, int live, int cap, int totalColumns, int fuelMilli, int throttleN,
        int throttleCap, int speedMilli, int effMilli, int rpm, int redline, int injectors, boolean injectorsDry,
        boolean coolantReady, int rotorMilli, long demand, long ceiling, long gridCap) {
        String worstName = "";
        String worstFix = "";
        int worstPermille = 1000;
        // Pillars actually burning, against what this tier's silo can hold. The silo is the one
        // piece that is never optional here - this line is only ever printed under one.
        if (cap > 0) {
            final int p = (int) Math.min(1000L, (long) live * 1000 / cap);
            if (p < worstPermille) {
                worstPermille = p;
                worstName = "pillars " + String.format("%,d", live) + "/" + String.format("%,d", cap);
                // M8 RE-AUDIT: the pillar FRAMES are optional interior ('F' takes a frame or
                // air, and survival placement SKIPs them), so a formed silo with none in it is
                // an ordinary state - and it scored 0% here and was told to keep a bus stocked
                // for pillars that do not exist. Score by the number, advise by what stands.
                worstFix = totalColumns <= 0
                    ? "the silo is empty - stack Reactor Fuel Pillar Frames 8 high on its floor, then stock the Rod Input Bus"
                    : "keep the silo's Rod Input Bus stocked";
            }
        }
        // Fuel grade, against Naquadah-Bonded x9 - the AVERAGE of the pillars that are burning.
        // M8 RE-AUDIT: only scored while something IS burning. With live 0 avgFuelMilli falls
        // back to the LABEL grade, so a plant with no fuel at all scored 11% on a grade term
        // over zero rods and was told to "burn a better rod grade".
        if (live > 0) {
            final int fuelP = (int) Math.min(1000L, (long) Math.max(1, fuelMilli) / 9);
            if (fuelP < worstPermille) {
                worstPermille = fuelP;
                worstName = "fuel avg x" + String.format("%.2f", Math.max(1, fuelMilli) / 1000.0) + " of x9";
                worstFix = "burn a better rod grade";
            }
        }
        // Control rods. M8 RE-AUDIT: throttleN is the EFFECTIVE setting, and a programmed
        // circuit in the controller slot overrides the GUI field - so the fix has to name both
        // places it can live rather than send a circuit-driven plant to a field that does
        // nothing. This is the one branch where naming the right control costs no new state.
        // F7: scored against THIS PLANT'S CAP, not a hard 24. With the BURNUP unlocks and the
        // three rod nodes the cap runs 26..36, checkProcessing clamps the setting to
        // maxThrottle() and getThrottleN() hands back the raw N - so the old min(24, N) term
        // saturated at 100% on a setting the plant had already exceeded, while mathSteamLine
        // one line above was correctly printing "rods 36/24". The DENOMINATOR the player reads
        // is still 24 (36/24 really is 150% of design power); only the RATIO this line scores
        // moves, and at an un-upgraded cap of 24 it is bit-identical to what it printed before.
        final int rodCap = Math.max(1, throttleCap);
        final int throttleP = Math.min(rodCap, Math.max(1, throttleN)) * 1000 / rodCap;
        if (throttleP < worstPermille) {
            worstPermille = throttleP;
            worstName = "rods " + throttleN + "/24" + (rodCap > 24 ? " of " + rodCap + "/24 unlocked" : "");
            worstFix = "raise the Rods field (or the Programmed Circuit in the controller slot, if one is in it)";
        }
        // Spine, against the x1.553 an all-riding train reaches. Efficiency is an EU-side term:
        // an export plant is paid for SPEED only, and x1.553 is exactly what all-riding gives.
        // computeSpine() parks BOTH multipliers at x1.00 when there is no duct, so an ABSENT
        // spine and a badly-wrenched one score identically - only the fix can tell them apart.
        final int spineNow = galleryActive ? speedMilli * effMilli / 1000 : speedMilli;
        final int spineP = (int) Math.min(1000L, (long) spineNow * 1000 / 1553);
        if (spineP < worstPermille) {
            worstPermille = spineP;
            if (!spineBuilt) {
                worstName = "spine NOT BUILT (x1.00 of x1.55)";
                worstFix = "build the Spine (the 3x3 duct off the silo's side wall), then set its 4 Turbine Housings";
            } else if (spineHousings <= 0) {
                // M8 RE-AUDIT: 'H' chains to plain turbine casing, so a duct with every socket
                // blank STANDS and multiplies nothing - and was told to wrench four housings
                // that are not in it.
                worstName = "spine x1.00 of x1.55 - duct is empty";
                worstFix = "set the 4 Turbine Housings in the duct's bore (they alternate with the open cells)";
            } else {
                worstName = (galleryActive ? "spine x" : "spine speed x") + String.format("%.2f", spineNow / 1000.0)
                    + " of x1.55";
                worstFix = galleryActive
                    ? "right-click the " + spineHousings
                        + " Spine housings to retune them (ride the swirl for speed, oppose it for efficiency)"
                    : "right-click the " + spineHousings
                        + " Spine housings to ride the swirl - steam export is paid in speed";
            }
        }
        if (galleryActive) {
            // Fan speed, against the redline. RPM is added by CYCLES, and cycles are run by the
            // Spiral's eight injectors - with no chamber there is nothing to cycle, so "cycle
            // faster" would be one more instruction to tune a building that is not there.
            final int rpmP = (int) Math.min(1000L, (long) rpm * 1000 / Math.max(1, redline));
            if (rpmP < worstPermille) {
                worstPermille = rpmP;
                final String rpmNow = "RPM " + String.format("%,d", rpm) + "/" + String.format("%,d", redline);
                // M8: RPM is added by CYCLES, and the TC-7 interlock refuses a cycle outright on
                // three separate grounds before "not enough of them" is even a coherent reading.
                // The branches follow the interlock's own precedence (`dry || cannot cool`), so
                // the weakest-term line and the hold reason printed above it can never name two
                // different faults for the same tick.
                if (!spiralBuilt) {
                    worstName = "RPM 0 - no Spiral chamber";
                    worstFix = "build the Spiral at the end of the Spine and fit its 8 Precision Injectors";
                } else if (injectors <= 0) {
                    worstName = rpmNow + " - no Precision Injectors";
                    worstFix = "fit the chamber's 8 Precision Injectors: a cycle with nothing to inject is refused, not run badly";
                } else if (injectorsDry) {
                    worstName = rpmNow + " - injectors dry";
                    worstFix = "fill the Spiral chamber's Propellant Input Hatch - every cycle is being refused";
                } else if (!coolantReady) {
                    worstName = rpmNow + " - chamber cannot be cooled";
                    worstFix = "fill the Spiral chamber's Coolant Input Hatch - the interlock refuses every cycle it cannot cool";
                } else {
                    worstName = rpmNow;
                    worstFix = "cycle faster / better propellant";
                }
            }
            // Rotors: missing stages and wear both land in the average. Only reachable under
            // galleryActive, which already means at least one rotor IS seated.
            if (rotorMilli < worstPermille) {
                worstPermille = rotorMilli;
                worstName = "rotors x" + String.format("%.2f", rotorMilli / 1000.0);
                worstFix = "seat 8 fresh Turbine Rotors in the gallery";
            }
            // The clamp beats everything: EU generated and thrown away is the worst loss - and
            // an EMPTY manifold throws away ALL of it, which the old `ceiling > 0` guard let
            // pass in silence as though nothing were wrong.
            if (ceiling <= 0) {
                worstPermille = 0;
                worstName = "output ceiling 0";
                worstFix = "fit Dynamo or Laser Source Hatches in the manifold's back face - nothing can leave";
            } else if (demand > ceiling) {
                final int clampP = (int) Math.min(1000L, ceiling * 1000 / Math.max(1, demand));
                if (clampP < worstPermille) {
                    worstPermille = clampP;
                    worstName = "output ceiling " + String.format("%,d", ceiling);
                    worstFix = "fit more Laser Source Hatches";
                }
            }
            // D-17: the SWITCHYARD's soft cap is scored exactly like the hatch ceiling and named
            // completely separately, because the fix is a building and not a hatch. It is only a
            // term at all when it is the tighter of the two - a plant whose hatches cannot carry
            // 100M EU/t is not being held back by the grid.
            if (gridCap < ceiling && demand > gridCap) {
                final int gridP = (int) Math.min(1000L, Math.max(0L, gridCap) * 1000 / Math.max(1, demand));
                if (gridP < worstPermille) {
                    worstPermille = gridP;
                    worstName = "grid soft cap " + String.format("%,d", gridCap);
                    worstFix = "build the Switchyard - without one the plant may not put more than that on the grid";
                }
            }
        }
        if (worstName.isEmpty()) {
            if (galleryActive) {
                return EnumChatFormatting.GREEN + "Every term is at its ceiling - this is the plant's peak"
                    + EnumChatFormatting.RESET;
            }
            // The same mistake in the all-green branch: a gallery that STANDS with no rotors in
            // it does not need building, it needs loading.
            return EnumChatFormatting.GREEN + "Every steam term is at its ceiling"
                + EnumChatFormatting.RESET
                + (galleryBuilt ? " - seat Turbine Rotors in the 8 gallery housings to turn those litres into EU"
                    : " - build the Rotor Assembly to turn those litres into EU");
        }
        return EnumChatFormatting.GOLD + "Weakest term: "
            + worstName
            + " ("
            + worstPermille / 10
            + "%)"
            + EnumChatFormatting.RESET
            + " - "
            + worstFix;
    }

    /**
     * M1: the REACTOR group's first line while nothing is formed - and the two states that hid
     * behind it. "No silo yet" is a build instruction; "a silo IS standing and does not pass"
     * is a fault, and the projector cannot fix the one it used to be offered for, because a
     * legacy face conflicts with the current one cell for cell rather than merely missing
     * blocks. The structure-error panel above this line now carries the coordinates; this line
     * carries the only action that resolves them.
     */
    public static String siloAbsentLine(int standingTier) {
        if (standingTier <= 0) {
            return EnumChatFormatting.YELLOW
                + "Reactor Silo not built - project this controller to raise it on the shield pad"
                + EnumChatFormatting.RESET;
        }
        // Tidy B4: was ~600 characters; the migration story belongs to the questbook.
        return EnumChatFormatting.RED + "Tier "
            + standingTier
            + " Reactor Silo is standing but not formed"
            + EnumChatFormatting.RESET
            + " - see Structure Errors for the first wrong cell. (Built on 0.19.0 or older? Run "
            + EnumChatFormatting.WHITE
            + "/btsmigrate"
            + EnumChatFormatting.RESET
            + " beside the controller.)";
    }

    public static String debugLine(boolean benchClock, boolean infiniteSupplies) {
        return EnumChatFormatting.LIGHT_PURPLE + "Bench clock "
            + (benchClock ? "ON" : "off")
            + " · infinite supplies "
            + (infiniteSupplies ? "ON" : "off")
            + EnumChatFormatting.RESET;
    }

    /**
     * Tidy C6: the panel's named groups. The controller GUI and {@link #statusReport()} print
     * all of them; a control block prints only its own, so a right-click fits on one chat
     * screen. Every group is built from the same static formatters as before.
     */
    public enum ReportSection {
        REACTOR,
        STEAM_CHAIN,
        TURBINE,
        OUTPUT,
        MATH,
        PROGRESSION
    }

    /**
     * The whole instrument panel, grouped and colour-coded: every {@link ReportSection} in
     * order, then the bench flags. The groups between REACTOR and PROGRESSION exist only once a
     * silo stands.
     */
    public List<String> statusReport() {
        final List<String> out = new ArrayList<>();
        for (final ReportSection section : ReportSection.values()) {
            appendSection(out, section);
        }
        if (debugAutoCycle || debugInfiniteSupplies) {
            out.add(debugLine(debugAutoCycle, debugInfiniteSupplies));
        }
        return out;
    }

    /** One or more groups of the panel, in the order asked for. */
    public List<String> statusReportSection(ReportSection... sections) {
        final List<String> out = new ArrayList<>();
        for (final ReportSection section : sections) {
            appendSection(out, section);
        }
        return out;
    }

    private void appendSection(List<String> out, ReportSection section) {
        if (section == ReportSection.REACTOR) {
            appendReactor(out);
            return;
        }
        if (section == ReportSection.PROGRESSION) {
            // F6: the progression block belongs to EVERY readout in EVERY state - a bare
            // platform's Derrick accrues REPROCESSING before there is a silo to gate on.
            appendProgression(out);
            return;
        }
        // The steam chain, turbine, output and math groups describe a silo that stands.
        if (!isSiloBuilt()) return;
        switch (section) {
            case STEAM_CHAIN:
                appendSteamChain(out);
                return;
            case TURBINE:
                appendTurbine(out);
                return;
            case OUTPUT:
                appendOutput(out);
                return;
            case MATH:
                appendMath(out);
                return;
            default:
                return;
        }
    }

    private String selfFuelNow() {
        return selfFuelLine(
            derrickBuilt,
            annexBuilt,
            derrickCycles,
            annexCycles,
            getModuleStall(),
            progression.snapshot(),
            progression.nodes());
    }

    private void appendReactor(List<String> out) {
        out.add(header("REACTOR"));
        if (!isSiloBuilt()) {
            out.add(siloAbsentLine(legacySiloTier()));
            out.add(supplyLine(false, 0, 0, 0, 0));
            out.add(towerLine(getModulesSummary()));
            out.add(moduleLine(getPlantModulesSummary()));
            final String bareStall = selfFuelNow();
            if (!bareStall.isEmpty()) out.add(bareStall);
            return;
        }
        out.add(tierLine(getReactorTier(), getEdge(), getThrottleN(), isThrottleOverridden()));
        out.add(supplyLine(true, getRodsQueued(), pillarGaps(), getPropellantLitres(), getChamberCoolantLitres()));
        out.add(towerLine(getModulesSummary()));
        out.add(moduleLine(getPlantModulesSummary()));
        out.add(
            cascadeLine(
                cascadeMode,
                liveColumnsLastCycle,
                columns.size(),
                getFuelColumns(),
                getColumnCap(),
                fuelName(activeFuelMult),
                steamPerTick));
        out.add(heatLine(heat, getDecaySeconds(), getRadiationSeconds(), ventStackBuilt));
        final String selfFuel = selfFuelNow();
        if (!selfFuel.isEmpty()) out.add(selfFuel);
    }

    private void appendSteamChain(List<String> out) {
        out.add(header("STEAM CHAIN"));
        out.add(spineLine(spineBuilt, spinePattern, spineHousings, spineSpeedMilli, spineEffMilli, spineSwirl));
        out.add(spiralLine(spiralBuilt, spiralInjectors.size(), lastGoodInjectors, propellantName, propellantMilli));
        out.add(
            coolantLine(spiralBuilt, chamberStarved, isChamberCoolantReady(), chamberCoolantName, chamberCoolantDrain));
        // D-26: the feedwater grade, and ONLY when a purified grade is actually being drunk.
        // Empty at plain and distilled water, so a plant that ran yesterday shows the same panel
        // it showed yesterday - the water ladder adds a line when you opt into it and not before.
        final String feedwater = feedwaterLine(feedwaterGrade, isGalleryActive());
        if (!feedwater.isEmpty()) out.add(feedwater);
    }

    private String rpmNow() {
        return rpmLine(
            getRpm(),
            getRedlineRpm(),
            getUnlockedRedlineRpm(),
            cyclesPerMinute,
            getSteamBufferSlugsTenths(),
            debugAutoCycle);
    }

    private void appendTurbine(List<String> out) {
        out.add(header("TURBINE"));
        out.add(rpmNow());
        out.add(rotorLine(galleryBuilt, galleryStages, avgRotorFactorMilli, rotorWear));
        final String hold = getTurbineHoldReason();
        if (!hold.isEmpty()) out.add(holdLine(hold));
    }

    private String outputNow() {
        return outputLine(
            isGalleryActive(),
            isTurbineInstalled(),
            lastEUOutput,
            lastEUDemand,
            isOutputClamped(),
            isGridCapped(),
            getBoostedSteamOutput());
    }

    private void appendOutput(List<String> out) {
        out.add(header("OUTPUT"));
        out.add(outputNow());
        if (galleryBuilt) {
            out.add(ceilingLine(outputCeiling(), getDynamoCapacity(), getLaserCapacity()));
            // D-25: which mode the over-injection guard is in, ALWAYS, in both modes - and the
            // numbers whenever it has caught something. Same formatter as the GUI's row.
            out.add(
                outputSafetyLine(
                    debugOutputSafety,
                    outputOverruns,
                    outputOverrunOffered,
                    outputOverrunCapacity,
                    outputOverrunDynamo,
                    outputOverrunLaser));
            // D-17: the grid cap, in all four of its states, right under the ceiling it shares a
            // Math.min with.
            out.add(switchyardLine(switchyardBuilt, Config.switchyardSoftCap, getLiveEUDemand(), outputCeiling()));
            out.add(laserLine(getLaserHatchCount(), getLinkedLaserHatchCount()));
            // C1: the manifold's empty E sockets, said out loud and costing nothing.
            final String eNote = energyHatchNote(true, getEnergyHatchCount(), derrickBuilt || annexBuilt);
            if (!eNote.isEmpty()) out.add(eNote);
        }
    }

    private void appendMath(List<String> out) {
        // B2: the derivation, in the Water Line's spirit - every factor with its current value,
        // so "why is my number small" is answerable without opening the source.
        // R1-B (B1): gated on isGalleryActive(), the OUTPUT group's own condition, so the two
        // groups can never describe two different plants.
        out.add(mathHeader(isGalleryActive()));
        // getFuelColumns() already answers "live pillars" in cascade mode and "assemblies in
        // the batch" in fallback - the same value the GUI syncs, so the two cannot disagree.
        final int live = getFuelColumns();
        final long demandNow = getLiveEUDemand();
        final long ceilingNow = outputCeiling();
        out.add(
            mathSteamLine(
                cascadeMode,
                live,
                activeFuelMult,
                getThrottleN(),
                fuelBaseSteam,
                fuelGradeMix,
                steamPerTick));
        out.add(mathSpineLine(spineSpeedMilli, spineEffMilli, getSteamAfterSpine()));
        // R9: the generator steps only exist where a generator does.
        // R1-B (B1): "a generator does" is isGalleryActive() - a gallery whose 8 seats are all
        // empty generates nothing and the plant is genuinely exporting steam through the silo,
        // so it gets the export step, not steps 3 and 4 about a fan that is not turning.
        if (isGalleryActive()) {
            out.add(
                mathTurbineLine(
                    getRpm(),
                    avgRotorFactorMilli,
                    progOutputMilli(),
                    progUpgradeMilli(),
                    waterOutputMilli(),
                    demandNow));
            out.add(mathClampLine(demandNow, ceilingNow, gridCeiling()));
        } else {
            out.add(mathExportLine(spineSpeedMilli, getBoostedSteamOutput()));
        }
        out.add(weakestTermLine());
    }

    /**
     * The MATH group's last line - the term costing the most and its fix - on its own, so the
     * control blocks and the scanner summary can print it without the derivation above it.
     */
    private String weakestTermLine() {
        final int live = getFuelColumns();
        return mathLimiterLine(
            spineBuilt,
            spineHousings,
            spiralBuilt,
            galleryBuilt,
            isGalleryActive(),
            live,
            getColumnCap(),
            getTotalColumns(),
            avgFuelMilli(cascadeMode, live, activeFuelMult, fuelBaseSteam),
            getThrottleN(),
            maxThrottle(),
            spineSpeedMilli,
            spineEffMilli,
            getRpm(),
            getRedlineRpm(),
            spiralInjectors.size(),
            isInjectorsDry(),
            isChamberCoolantReady(),
            avgRotorFactorMilli,
            getLiveEUDemand(),
            outputCeiling(),
            gridCeiling());
    }

    /**
     * Tidy C6: a control block's right-click. The Silo Control Block prints the REACTOR group,
     * the Turbine Control Block the TURBINE and OUTPUT groups; both add the weakest term and
     * point at the controller GUI for the rest.
     */
    public List<String> controlBlockReport(boolean turbinePanel) {
        final List<String> out;
        if (!turbinePanel) {
            out = statusReportSection(ReportSection.REACTOR);
        } else if (isSiloBuilt()) {
            out = statusReportSection(ReportSection.TURBINE, ReportSection.OUTPUT);
        } else {
            out = new ArrayList<>();
            out.add(siloAbsentLine(legacySiloTier()));
        }
        if (isSiloBuilt()) out.add(weakestTermLine());
        out.add(EnumChatFormatting.GRAY + "Open the Nuclear Plant Foundation for the full panel");
        return out;
    }

    /**
     * Tidy C7: the Portable Scanner's summary - about eight lines instead of the whole panel:
     * tier and rods, heat, supply, RPM, EU/t, the hold reason if any, the weakest term and the
     * milestone roll-up. Same formatters as the full panel.
     */
    public List<String> scannerSummary() {
        final List<String> out = new ArrayList<>();
        if (!isSiloBuilt()) {
            out.add(siloAbsentLine(legacySiloTier()));
            out.add(towerLine(getModulesSummary()));
            out.add(moduleLine(getPlantModulesSummary()));
        } else {
            out.add(tierLine(getReactorTier(), getEdge(), getThrottleN(), isThrottleOverridden()));
            out.add(heatLine(heat, getDecaySeconds(), getRadiationSeconds(), ventStackBuilt));
            out.add(supplyLine(true, getRodsQueued(), pillarGaps(), getPropellantLitres(), getChamberCoolantLitres()));
            out.add(rpmNow());
            out.add(outputNow());
            final String hold = getTurbineHoldReason();
            if (!hold.isEmpty()) out.add(holdLine(hold));
            out.add(weakestTermLine());
        }
        out.add(BTSMilestone.milestoneSummaryLine(progression.snapshot()));
        if (debugAutoCycle || debugInfiniteSupplies) {
            out.add(debugLine(debugAutoCycle, debugInfiniteSupplies));
        }
        out.add(EnumChatFormatting.GRAY + "Open the controller GUI for the full panel");
        return out;
    }

    /**
     * The D-23 MILESTONES and D-24 UPGRADES block, in ONE place so it cannot be appended on one
     * path and forgotten on another (F6).
     * <p>
     * UNGATED on the silo, exactly like the tower and module lines - the REPROCESSING line
     * accumulates on a bare platform's Derrick as readily as on a finished silo, and the
     * controller GUI is the only panel that exists while a player is still building one. Every
     * line comes out of the same static formatter the GUI's own rows call, off the same four
     * counters and the same node mask, so the GUI, BOTH control blocks and the scanner cannot
     * describe four different campaigns.
     */
    private void appendProgression(List<String> out) {
        out.add(header("MILESTONES"));
        final long[] progress = progression.snapshot();
        for (final BTSMilestone line : BTSMilestone.values()) {
            out.add(BTSMilestone.milestoneLine(line, BTSProgression.counterOf(progress, line)));
        }
        out.add(BTSMilestone.milestoneSummaryLine(progress));
        out.add(BTSMilestone.milestoneUnlockLine(progress));
        // D-24 UPGRADES: what the points earned above have been SPENT on, and what those
        // purchases are currently moving. Both off BTSUpgrade's shared static formatters - the
        // same two the tree panel's header prints.
        out.add(BTSUpgrade.treeSummaryLine(progression.nodes(), progress));
        out.add(BTSUpgrade.treeEffectLine(progression.nodes()));
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
        // Tidy C7: a summary, not the whole 35-45 line panel. Every line comes from the same
        // formatters the GUI uses, so the two cannot disagree; the GUI has the rest.
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(scannerSummary());
        return info.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        // Info block: what it is, how to run it, the numbers a player needs at the machine.
        // The long explanations (swirl, injector window, laser painting, module layouts) live
        // in the questbook; the GUI's MATH section prints the live derivation.
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String red = EnumChatFormatting.RED.toString();
        final String gray = EnumChatFormatting.GRAY.toString();
        tt.addMachineType("Nuclear Reactor, Steam Turbine")
            .addInfo(aqua + "A whole nuclear power plant, run from one controller.")
            .addInfo("Fuel rods boil feedwater into steam; the turbine turns it into EU.")
            .addInfo("One hologram projection builds every piece: platform, Reactor Silo,")
            .addInfo("Spine, Spiral chamber, Rotor Assembly, Cooling Towers and the modules.")
            .addSeparator()
            .addInfo(aqua + "Supply:" + gray + " rods + feedwater at the silo face; coolant + propellant at")
            .addInfo("the Spiral chamber's front wall; Dynamos + Laser Sources at the manifold.")
            .addInfo(aqua + "Running:" + gray + " each redstone pulse on the Turbine Control Block runs one cycle.")
            .addInfo(
                aqua + "GUI:"
                    + gray
                    + String.format(
                        " Rods 1-24 (up to 36), RPM limit %,d (up to %,d), Build Tier.",
                        RPM_REDLINE,
                        RPM_LIMIT_MAX))
            .addInfo("A Programmed Circuit in the controller overrides the Rods field.")
            .addInfo(
                red + "Heat:"
                    + gray
                    + String.format(
                        " SCRAM at %,d, MELTDOWN at %,d; %d min of decay cooling.",
                        SCRAM_HEAT,
                        MELTDOWN_HEAT,
                        DECAY_HEAT_TICKS / 1200))
            .addInfo(red + String.format("Without a Switchyard, output is capped at %,d EU/t.", GRID_SOFT_CAP))
            .addInfo(aqua + "Progress:" + gray + " milestones raise output; spend points in the Upgrade Tree.")
            .addInfo("Progress is saved in the controller and survives breaking it.")
            .addInfo("The GUI's MATH section shows how the EU/t is worked out.")
            .addInfo("NEI: look up this controller for the fuel, propellant and coolant tables.")
            // No addSupportLaser(): GT's green line means laser ENERGY INPUT, and the plant's
            // Energy Hatch sockets take plain Energy Hatches only. Its lasers are Laser Source
            // (output) hatches, which the info line above and the structure block name.
            // Structure block (Shift). Counts come from the shape code (questbook/gen-bom.ps1
            // tallies the same cells); ranges run tier 1 to tier 5 or none to all pieces.
            .beginVariableStructureBlock(50, 50, 21, 41, 50, 50, true)
            .addController("Silo face, centre, at platform level")
            .addCasing("2,251", "Light Concrete (platform; +50 Spiral, +736 per Cooling Tower)", false)
            .addCasing("249", "Tungstensteel Reinforced Block (shield pad under the silo)", false)
            .addCasing(
                "451-1,667",
                "Silo wall: Radiation Proof Machine Casing (T1-2), Europium Reinforced"
                    + " Radiation Proof Machine Casing (T3-4), Radiant Naquadah Alloy Casing (T5)",
                true)
            .addCasing("69-141", "Radiation Proof Machine Casing (silo bands, empty face sockets)", false)
            .addCasing(
                "113-497",
                "Silo dome: Tungstensteel Reinforced Block (T1-2), Iridium Reinforced Block (T3+)",
                true)
            .addCasing("0-830", "Tungstensteel Turbine Casing (Spine, Spiral chamber, Rotor Assembly)", false)
            .addCasing("0-1,608", "Reactor Fuel Pillar Frame (stacks of 8 on 45-201 pillars)", false)
            .addCasing("0-12", "Turbine Housing (4 in the Spine, 8 in the gallery)", false)
            .addOtherStructurePart("Silo Control Block", "Silo face, directly above the controller", 1)
            .addOtherStructurePart("Turbine Control Block", "Manifold back face, above its Maintenance Hatch", 4)
            .addOtherStructurePart("Precision Injector", "8: Spiral chamber walls, along the helix", 3)
            .addOtherStructurePart("Spine Sensor", "Optional: Spiral chamber roof, front centre", 3)
            .addOtherStructurePart(
                "Module sockets",
                "Derrick: Output Bus; Annex: Input Bus, Acid Input Hatch, Output Bus",
                2)
            .addOtherStructurePart(
                "Laser Source Hatch",
                "0-4: manifold back face, 6th row up; paint each to match its Laser Target",
                5)
            .addDynamoHatch("0-4", "Output manifold back face, 4th row up", 4)
            .addEnergyHatch(
                "0-4",
                "Manifold back face, 5th row up. The reactor needs none; they power the Derrick and the Annex",
                4)
            .addMaintenanceHatch(
                "1-3",
                "Silo face, beside the controller (required); Spiral floor and manifold",
                1,
                3,
                4)
            .addInputBus("1", "Rod Input Bus: silo face, 2 above the controller, left of centre", 1)
            .addInputHatch(
                "1-3",
                "Feedwater: left of the Silo Control Block; Coolant, Propellant: Spiral front wall",
                1,
                3)
            .addOutputBus("1", "Depleted Output Bus: silo face, 2 above the controller, right of centre", 1)
            .addOutputHatch("1", "Silo face, right of the Silo Control Block (water / steam out)", 1)
            .addStructureInfo("Rod Loading Ports on top of the pillars are optional (hand-feeding).")
            .addStructureInfo("Optional pieces: Spine, Spiral chamber, Rotor Assembly, 2 Cooling Towers,")
            .addStructureInfo("Vent Stack, Switchyard, Extraction Derrick, Enrichment Annex.")
            .addStructureInfo("The Blueprint quests list every piece's blocks.")
            .addMasterChannel("Silo tier 1-5 (a stack of 1 uses the GUI Build Tier)")
            .addStructureInfo("Plants built on 0.19.0 or older: run /btsmigrate beside the controller once.")
            .toolTipFinisher(com.nateyburr.beyondthestars.BeyondTheStars.TOOLTIP_AUTHOR);
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

    /**
     * B1 - NEI ONLY. This is the hook NEI's catalyst index reads to decide which recipe tabs a
     * machine is the catalyst for; it is NOT the hook that makes a machine process recipes.
     * That one is {@code getRecipeMap()}, which stays null here because the plant runs its own
     * {@link #checkProcessing()} over rod segments and never calls findRecipe. Returning the
     * display maps therefore only puts the controller's icon on four pages of tables (and
     * those pages' entries are {@code .fake()} on top of that); the machine's behaviour and the
     * hatches' filtering are completely untouched.
     * <p>
     * D-26 adds the fourth: the purified-water ladder, which belongs on this controller's icon
     * because one of its two terms is a term of THIS machine's EU derivation.
     */
    @Nonnull
    @Override
    public java.util.Collection<gregtech.api.recipe.RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(
            com.nateyburr.beyondthestars.BTSDisplayRecipes.plantFuelRods,
            com.nateyburr.beyondthestars.BTSDisplayRecipes.plantPropellant,
            com.nateyburr.beyondthestars.BTSDisplayRecipes.plantCoolant,
            com.nateyburr.beyondthestars.BTSDisplayRecipes.plantWater);
    }

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    /**
     * Tidy B7: no parallel to limit, so GT's Power Panel would hold only its power-fail toggle. GT
     * hides it on its own single-parallel multis (Heat Exchanger, Air Filter, Integrated Ore Factory)
     * the same way. GUI only.
     */
    @Override
    public boolean supportsPowerPanel() {
        return false;
    }
}
