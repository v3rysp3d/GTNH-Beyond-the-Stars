package com.nateyburr.beyondthestars.machines.eye;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.channelIndex;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static com.nateyburr.beyondthestars.machines.BTSStructures.tieredBlock;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.directive.DirectiveHost;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;
import com.nateyburr.beyondthestars.machines.eye.star.EyeBursts;
import com.nateyburr.beyondthestars.machines.eye.star.EyeNbt;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.util.item.AEItemStack;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import tectech.thing.casing.TTCasingsContainer;

/**
 * SINGULARITY LADDER P3: THE EYE OF HARMONY, T1-T4 - MTE base+15 (28115), UMV
 * (singularity-ladder-build.md §3.3, D-28, D-34, D-41, D-42). Owned by the Eye unit.
 * <p>
 * One controller, grounded on six pylons over an oval plinth (D-34). It replaces TecTech's Eye of
 * Harmony ({@code Config.replaceTecTechEyeOfHarmony}, {@link EyeRecipeRegistry}).
 *
 * <h2>Shape and tiers</h2>
 * Six resource pieces exported from {@code designs/voxel.js}, loaded by {@link BTSShapes}, each
 * checked at its own {@code off=}: {@code eye_core} (T1, required), {@code eye_beam} (T2 beam band),
 * {@code eye_jet} (T3 jet crescent), {@code eye_arms} (T4 spiral arms), {@code eye_arms2} (T5 second
 * arms) and {@code eye_rim} (T6 galactic rim). D-49 makes the bands CUMULATIVE ({@link #structureTier}):
 * T2 is required, and a band counts only while every band below it stands. A tier also needs D-49's
 * permanent unlock (milestones, core, transition; {@link EyeStarRuntime}). The core band carries D-49's Field
 * ({@code f}) and Sensor ({@code n}) sockets, the rim four more stellar sockets ({@code g}); all of
 * them fall back to their casing, so older Eyes still form. The blocks are TecTech's own EOH
 * blocks wherever a role fits (the mid-run ruling): Reinforced Spatial shell, Reinforced Temporal
 * plinth, Infinite Spacetime Energy Boundary lining, Quantum Glass, and the three tiered field
 * generators; only the three band casings are ours.
 *
 * <h2>Field generators (Q-20, my rulings)</h2>
 * {@code W} Spacetime Compression (upper clamp struts), {@code X} Time Dilation (lower struts),
 * {@code Y} Stabilisation (pylon foot pads), any tier 0-8, mixable - the LOWEST of each type counts
 * (as the Kerr's rails, Q-26).
 * <ul>
 * <li><b>Compression is the tier gate</b> (1:1): Eye tier N needs every compression generator at
 * TecTech tier N or higher ({@link #COMPRESSION_META_FOR_TIER}, the ONE table), and that gate alone
 * decides which planets run: an Eye at tier N runs every planet mapped to tiers 1..N (round-4 ruling;
 * TecTech's per-planet compression minimum is NOT enforced). The only planet effect kept is TecTech's
 * reward: each compression tier above the planet's TecTech requirement cuts 3 % of the time.</li>
 * <li><b>Time Dilation and Stabilisation act on the stars</b> (D-49 §3.6, which the approved proposal
 * makes supersede the Q-20 placeholders): Time Dilation tier tt caps the time dial at
 * {@code 1 + 0.25·max(0, tt − N)}; Stabilisation tier ts sets {@code q = 1 − 0.05·(ts − 1)}, which scales the
 * field's noise and events. Planets keep only the compression reward.</li>
 * </ul>
 *
 * <h2>Running</h2>
 * Inputs drain every second from the private gas and stellar sockets into {@code long} buffers. The
 * controller slot holds the target (a star chart or a dimension display block; never consumed).
 * <ul>
 * <li><b>Stars</b> (D-49) are live: {@link EyeStarRuntime} runs one field second every 20 ticks
 * (ignition, 60 s cycles of wireless EU and upkeep, the stability field fed by the Field sockets,
 * product to ME per cycle). GT's run loop never runs a star.</li>
 * <li><b>Planets</b> are batch runs as built: the whole EU from the owner's wireless network at start (no
 * EU return, Q-4), the inputs, success rolled per parallel, outputs to the ME output bus / hatch.</li>
 * </ul>
 * Parallel = {@code 2^(installed tier - recipe tier)} (planets: cut down to what the buffers can pay for).
 *
 * <h2>The four traps</h2>
 * <ol>
 * <li><b>Client vs server.</b> {@link #construct} reads only the trigger stack (1..6 = T1..T6, bands
 * cumulative); it never reads the unlocked tier or the star.</li>
 * <li><b>Tile vs item NBT.</b> {@link #writeEyeState} is the one serializer of the persisted state (the
 * D-49 unlock, milestones, escrow and setpoints, and the buffers), called from both {@link #saveNBTData}
 * and {@link #setItemNBT}; the live star is tile-only.</li>
 * <li><b>Plant-wide hatch lists.</b> Input sockets live in private lists ({@link EyeHatchElement}); a
 * band whose walk fails drops its sockets again (mark-and-truncate).</li>
 * <li><b>Element chains.</b> Every socket is {@code buildHatchAdder(...).buildAndChain(casing)}, the
 * hatch first. The field generators are {@code BTSStructures.tieredBlock}s: the walk accepts any
 * tier, and every build places one tier picked from the trigger alone (trap 1): compression at the
 * stack's 1:1 gate, dilation and stabilisation at tier 1, each raised by GT's EOH structure channel
 * if the projector sets one. Builds never downgrade a generator already standing.</li>
 * </ol>
 * All walk state is zeroed in {@link #resetWalk()}, the first statement of {@link #checkMachine}.
 */
public class MTEEyeOfHarmonyBTS extends MTEExtendedPowerMultiBlockBase<MTEEyeOfHarmonyBTS>
    implements ISurvivalConstructable, DirectiveHost {

    // ------------------------------------------------------------------------------- pieces
    public static final String PIECE_CORE = "eye_core";
    public static final String PIECE_BEAM = "eye_beam";
    public static final String PIECE_JET = "eye_jet";
    public static final String PIECE_ARMS = "eye_arms";
    /** D-49: the T5 band, the second pair of spiral arms. */
    public static final String PIECE_ARMS2 = "eye_arms2";
    /** D-49: the T6 band, the galactic rim with its four rim stellar sockets. */
    public static final String PIECE_RIM = "eye_rim";
    /** P4: the T7 band, the stability rail (tilted 8°) with 8 GRB Receptor Ports, rail-high and rail-low. */
    public static final String PIECE_RAIL = "eye_rail";
    /** P4: the T8 band, the shard halo with 8 GRB Receptor Ports at the shard roots. */
    public static final String PIECE_HALO = "eye_halo";
    /**
     * Pieces in hologram and tier order; the trigger stack size (1..8) projects the first N. D-49: the
     * bands are CUMULATIVE, so piece i (tier i+1) counts only while every piece before it stands.
     */
    public static final String[] PIECES = { PIECE_CORE, PIECE_BEAM, PIECE_JET, PIECE_ARMS, PIECE_ARMS2, PIECE_RIM,
        PIECE_RAIL, PIECE_HALO };
    /** What the scanner calls each piece (index = piece index). */
    private static final String[] BAND_NAMES = { "core", "beam band", "jet crescent", "spiral arms", "second arms",
        "galactic rim", "stability rail", "shard halo" };
    /** P4: the Receptor Port groups (manifest {@code sockets.r[i].group}). */
    public static final int GROUP_HIGH = 0, GROUP_LOW = 1, GROUP_HALO = 2;
    public static final String[] GROUP_NAMES = { "rail-high", "rail-low", "halo" };
    /** The highest tier the structure can carry. */
    public static final int MAX_TIER = PIECES.length;

    // -------------------------------------------------------------------- field generators
    public static final int FIELD_COMPRESSION = 0;
    public static final int FIELD_DILATION = 1;
    public static final int FIELD_STABILISATION = 2;
    private static final String[] FIELD_NAMES = { "Spacetime Compression", "Time Dilation", "Stabilisation" };
    /** TecTech's EOH constant (MTEEyeOfHarmony, 5.09.52.594): planet time per compression tier above need. */
    public static final double COMPRESSION_TIME_DISCOUNT = 0.97;
    /**
     * Q-20 THE COMPRESSION GATE, the one table: the lowest generator META (TecTech tier - 1) that
     * Eye tier N needs in every compression cell. 1:1 (my ruling): T1 Crude (meta 0) .. T6 meta 5
     * (D-49 extends the gate to T6; P4 to T7 meta 6 and T8 meta 7). Index 0 is unused. Change the mapping here and
     * nowhere else.
     */
    public static final int[] COMPRESSION_META_FOR_TIER = { -1, 0, 1, 2, 3, 4, 5, 6, 7 };

    /** Buffers cap at 2^62 L each (spec §3.3); what the sockets fill them to is {@link #intakeCaps} (B-108). */
    public static final long BUFFER_CAP = 1L << 62;
    /** B-108: the planet runs at full parallel the buffers fill to ahead (the next run is ready when one ends). */
    public static final int PLANET_INTAKE_RUNS = EyeStarRuntime.PLANET_INTAKE_RUNS;
    private static final int DRAIN_INTERVAL = 20;

    /** Touch the NEI page at class init (preInit), before NEI builds its handlers. */
    private static final RecipeMap<?> DISPLAY_MAP = EyeRecipeMaps.EYE;
    private static final Random RANDOM = new Random();

    // ------------------------------------------------------------------------------ textures
    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_EYE_OF_HARMONY_ACTIVE_GLOW");

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread at load. */
    private static volatile IStructureDefinition<MTEEyeOfHarmonyBTS> STRUCTURE_DEFINITION = null;

    // ---------------------------------------------------- PERSISTED (writeEyeState is the one writer)
    /** Fluid name → litres. */
    private final Map<String, Long> buffers = new LinkedHashMap<>();
    /**
     * D-49: the live star, the unlocked tier, the milestones and the core escrow ({@link EyeStarRuntime}); its NBT
     * goes through {@link EyeNbt} (persistent part in item and tile NBT, the star in the tile only).
     */
    private final EyeStarRuntime runtime = new EyeStarRuntime(new StarHost());
    /** D-49 answer 3: the centre render (its per-machine switch persisted; its block position tile-only). */
    private final EyeCentreRender centre = new EyeCentreRender();

    // ------------------------------------------------------------- RUN state (server, tile NBT)
    private String runningKey;
    private long runningParallel;
    private long runningSuccesses;
    private double runningYield = 1.0;
    private String lastRun = "";

    // ---------------------------------------------------------- WALK state (server, resetWalk)
    private int mCasing;
    private final int[] fieldMin = { -1, -1, -1 };
    private final int[] fieldCount = new int[3];
    /** Per piece: did its walk pass (index 0, the core, mirrors mMachine). Cumulation is applied on read. */
    private final boolean[] bandBuilt = new boolean[PIECES.length];
    /** Per piece: the first walk error of a band that did not pass, "" otherwise. */
    private final String[] bandFault = emptyFaults();
    private final ArrayList<MTEHatchInput> gasHatches = new ArrayList<>();
    private final ArrayList<MTEHatchInput> stellarHatches = new ArrayList<>();
    /** D-49 Field sockets: live trim hatches (never buffered; the star runtime drains them per field second). */
    private final ArrayList<MTEHatchInput> fieldHatches = new ArrayList<>();
    /** D-49 R-15: ME stocking hatches found in Field sockets: walked (the Eye forms), never drained. */
    private final ArrayList<MTEHatchInput> rejectedFieldHatches = new ArrayList<>();
    private final ArrayList<MTEHatchStellarFieldSensor> sensorHatches = new ArrayList<>();
    /** AUTOMATION PASS: Directive Hatches (MTE 28127) in the Sensor sockets. */
    private final ArrayList<MTEHatchDirective> directiveHatches = new ArrayList<>();
    /**
     * The Directive Hatches the last formed walk found. Most breaks stop the walk before it reaches the sockets, which
     * left {@link #directiveHatches} empty and the hatches silent, so the Status set's "not formed" bit never reached
     * the redstone (B-71). An unformed walk publishes to these plus any it reached (B-72); every formed walk replaces
     * them, and a removed hatch drops out by {@code live}.
     */
    private final ArrayList<MTEHatchDirective> formedDirectiveHatches = new ArrayList<>();
    /** D-49 R-1: input buses on the plinth gas sockets (the Stellar Ignition Charges). */
    private final ArrayList<MTEHatchInputBus> itemBuses = new ArrayList<>();
    /** P4: GRB Receptor Ports by group (rail-high, rail-low on the T7 rail; halo on the T8 shards). */
    @SuppressWarnings("unchecked")
    private final ArrayList<MTEHatchGRBReceptor>[] receptors = new ArrayList[] { new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>() };
    /** P4: Burst Control Hatches in the 'd' sockets (Begin mode: a rising edge begins a galaxy transition). */
    private final ArrayList<MTEHatchBurstControl> burstControls = new ArrayList<>();
    /** P4: the slot bus ('p'): planet items and companion star charts, read every second, never consumed. */
    private final ArrayList<MTEHatchInputBus> slotBuses = new ArrayList<>();
    /** P4: the host-side burst API (not saved: nothing about a link is). */
    private final BurstHost burstHost = new BurstHost();

    public MTEEyeOfHarmonyBTS(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEEyeOfHarmonyBTS(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEEyeOfHarmonyBTS(this.mName);
    }

    // =================================================================================== shape

    private static int[] off(String piece) {
        return BTSShapes.offset(piece);
    }

    @Override
    public IStructureDefinition<MTEEyeOfHarmonyBTS> getStructureDefinition() {
        IStructureDefinition<MTEEyeOfHarmonyBTS> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEEyeOfHarmonyBTS.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTEEyeOfHarmonyBTS> buildDefinition() {
        final Block sl = SLBlocks.slCasing;
        final int plinthIndex = Casings.ReinforcedTemporalStructureCasing.getTextureId();
        final int shellIndex = Casings.ReinforcedSpatialStructureCasing.getTextureId();
        return StructureDefinition.<MTEEyeOfHarmonyBTS>builder()
            .addShape(PIECE_CORE, BTSShapes.load(PIECE_CORE))
            .addShape(PIECE_BEAM, BTSShapes.load(PIECE_BEAM))
            .addShape(PIECE_JET, BTSShapes.load(PIECE_JET))
            .addShape(PIECE_ARMS, BTSShapes.load(PIECE_ARMS))
            .addShape(PIECE_ARMS2, BTSShapes.load(PIECE_ARMS2))
            .addShape(PIECE_RIM, BTSShapes.load(PIECE_RIM))
            .addShape(PIECE_RAIL, BTSShapes.load(PIECE_RAIL))
            .addShape(PIECE_HALO, BTSShapes.load(PIECE_HALO))
            // ---- TecTech's own EOH blocks (the mid-run ruling)
            .addElement('U', casing(Casings.ReinforcedSpatialStructureCasing))
            .addElement('Q', casing(Casings.ReinforcedTemporalStructureCasing))
            .addElement('J', casing(Casings.InfiniteSpacetimeEnergyBoundaryCasing))
            .addElement('Z', casing(Casings.QuantumGlass))
            .addElement('W', compressionField(TTCasingsContainer.SpacetimeCompressionFieldGenerators))
            .addElement(
                'X',
                field(
                    FIELD_DILATION,
                    TTCasingsContainer.TimeAccelerationFieldGenerator,
                    GTStructureChannels.EOH_DILATION.get()))
            .addElement(
                'Y',
                field(
                    FIELD_STABILISATION,
                    TTCasingsContainer.StabilisationFieldGenerators,
                    GTStructureChannels.EOH_STABILISATION.get()))
            // ---- our three per-tier band casings
            .addElement('R', block(sl, BlockSLCasing.BEAM_BAND))
            .addElement('S', block(sl, BlockSLCasing.JET_CRESCENT))
            .addElement('T', block(sl, BlockSLCasing.GALACTIC_SHELL))
            // ---- the window band (my answer 2026-10-13, render E-9; B-86): at the star's height, one Quantum
            // Glass pane (A: the core band's outer shell in its front ring segments) with the cells behind it, and
            // the arm / rim cells in front of it, open (' ' in the shapes). The pane also takes the block it
            // replaced, so an Eye built earlier still forms; glass comes first, so holograms and builds place glass
            .addElement('A', window(casing(Casings.ReinforcedSpatialStructureCasing)))
            // ---- plinth sockets (D-49 R-1: the gas sockets also take input buses for the ignition charges)
            .addElement(
                'h',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Gas, EyeHatchElement.Items)
                    .casingIndex(plinthIndex)
                    .hint(1)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .addElement(
                'o',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Output)
                    .casingIndex(plinthIndex)
                    .hint(2)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .addElement(
                'm',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(Maintenance)
                    .casingIndex(plinthIndex)
                    .hint(3)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            // ---- band sockets (stellar fluids)
            .addElement(
                's',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Stellar)
                    .casingIndex(shellIndex)
                    .hint(4)
                    .buildAndChain(block(sl, BlockSLCasing.BEAM_BAND)))
            .addElement(
                't',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Stellar)
                    .casingIndex(shellIndex)
                    .hint(4)
                    .buildAndChain(block(sl, BlockSLCasing.JET_CRESCENT)))
            .addElement(
                'g',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Stellar)
                    .casingIndex(shellIndex)
                    .hint(4)
                    .buildAndChain(block(sl, BlockSLCasing.GALACTIC_SHELL)))
            // ---- D-49 stability-field sockets on the core band's outer face. Both fall back to the band
            // shell, so an Eye built before D-49 (shell in every one of these cells) still forms.
            .addElement(
                'f',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Field)
                    .casingIndex(shellIndex)
                    .hint(5)
                    .buildAndChain(casing(Casings.ReinforcedSpatialStructureCasing)))
            .addElement(
                'n',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Sensor, EyeHatchElement.Directive)
                    .casingIndex(shellIndex)
                    .hint(6)
                    .buildAndChain(casing(Casings.ReinforcedSpatialStructureCasing)))
            // ---- P4 (quasar-p4.md §7): plinth face cells of eye_core. Both fall back to the plinth casing, so an
            // Eye built before P4 (deck in every one of these cells) still forms. 'd' = 4 sockets above the
            // controller row for Directive Hatches (Field, Events, Galaxy Transition, Slots) or a Burst Control Hatch
            // (Begin mode); 'p' = the slot bus below it (a private input bus: planet items, companion charts).
            .addElement(
                'd',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class)
                    .atLeast(EyeHatchElement.Directive, EyeHatchElement.BurstControl)
                    .casingIndex(plinthIndex)
                    .hint(7)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            .addElement(
                'p',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.SlotBus)
                    .casingIndex(plinthIndex)
                    .hint(8)
                    .buildAndChain(casing(Casings.ReinforcedTemporalStructureCasing)))
            // ---- P4: the T7 stability rail and the T8 shard halo (Bastion Plate shell, Casimir Plate trim) and their
            // 16 GRB Receptor Ports ('r', falling back to the shell); each port's group (rail-high / rail-low /
            // halo) comes from the manifest by its offset (P4-03)
            .addElement('B', block(sl, BlockSLCasing.BASTION))
            .addElement('P', block(sl, BlockSLCasing.CASIMIR))
            .addElement(
                'r',
                buildHatchAdder(MTEEyeOfHarmonyBTS.class).atLeast(EyeHatchElement.Receptor)
                    .casingIndex(shellIndex)
                    .hint(9)
                    .buildAndChain(block(sl, BlockSLCasing.BASTION)))
            .build();
    }

    private static IStructureElement<MTEEyeOfHarmonyBTS> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEEyeOfHarmonyBTS> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /** A window cell: Quantum Glass, or the block it held before the window bands (old builds keep forming). */
    private static IStructureElement<MTEEyeOfHarmonyBTS> window(IStructureElement<MTEEyeOfHarmonyBTS> before) {
        return ofChain(casing(Casings.QuantumGlass), before);
    }

    /** Field-generator metas 0-8 (TecTech tiers 1 Crude .. 9 Gallifreyan), lowest first. */
    private static final int[] FIELD_METAS = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

    /**
     * One TecTech field-generator type, tiers 0-8 MIXABLE in the walk: it records the lowest tier
     * it meets (Q-20). Every build path places one wanted tier ({@code BTSStructures.tieredBlock}):
     * the tier GT's structure channel for that generator asks for (set on the projector), else tier
     * 1 (Crude), the cheapest, since Time Dilation and Stabilisation have no effect yet. A build
     * never downgrades a generator that is already standing.
     */
    private static IStructureElement<MTEEyeOfHarmonyBTS> field(int type, Block generator, String channel) {
        return tieredBlock(generator, FIELD_METAS, fieldWalk(type, generator), t -> channelIndex(t, channel, 0));
    }

    /** The walk half of a field element: any tier passes, the lowest is recorded. */
    @SuppressWarnings("unchecked")
    private static IStructureElement<MTEEyeOfHarmonyBTS> fieldWalk(int type, Block generator) {
        final IStructureElement<MTEEyeOfHarmonyBTS>[] tiers = new IStructureElement[FIELD_METAS.length];
        for (int i = 0; i < FIELD_METAS.length; i++) {
            final int meta = FIELD_METAS[i];
            tiers[i] = onElementPass(x -> x.noteField(type, meta), ofBlock(generator, meta));
        }
        return ofChain(tiers);
    }

    /**
     * The compression generators: the walk accepts ANY tier and records the lowest (as
     * {@link #field}), but holograms, creative and survival builds place the tier the trigger stack
     * asks for ({@link #compressionMetaFor}), so a stack-4 projection builds a T4-capable Eye. The
     * placement reads the trigger stack only, never server state (trap 1).
     */
    private static IStructureElement<MTEEyeOfHarmonyBTS> compressionField(Block generator) {
        return tieredBlock(
            generator,
            FIELD_METAS,
            fieldWalk(FIELD_COMPRESSION, generator),
            MTEEyeOfHarmonyBTS::compressionMetaFor);
    }

    /**
     * The compression generator meta a build for this trigger stack places: the 1:1 gate of the
     * stack's tier (1..6 = T1..T6, {@link #COMPRESSION_META_FOR_TIER}), or higher if GT's
     * {@code spacetime_compression} structure channel asks for more. Never below the gate.
     */
    public static int compressionMetaFor(ItemStack trigger) {
        final int gate = COMPRESSION_META_FOR_TIER[bandsFor(trigger)];
        return Math.max(gate, channelIndex(trigger, GTStructureChannels.EOH_COMPRESSION.get(), gate));
    }

    private void noteField(int type, int meta) {
        fieldCount[type]++;
        fieldMin[type] = fieldMin[type] < 0 ? meta : Math.min(fieldMin[type], meta);
    }

    /** Horizontal facings only: the Eye stands on its plinth. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    /** Every field a walk writes, zeroed in one place that nothing can get past. */
    private void resetWalk() {
        mCasing = 0;
        Arrays.fill(fieldMin, -1);
        Arrays.fill(fieldCount, 0);
        Arrays.fill(bandBuilt, false);
        Arrays.fill(bandFault, "");
        gasHatches.clear();
        stellarHatches.clear();
        fieldHatches.clear();
        rejectedFieldHatches.clear();
        sensorHatches.clear();
        directiveHatches.clear();
        itemBuses.clear();
        for (List<MTEHatchGRBReceptor> group : receptors) group.clear();
        burstControls.clear();
        slotBuses.clear();
        mOutputBusses.clear();
        mOutputHatches.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final int[] o = off(PIECE_CORE);
        if (!checkPiece(PIECE_CORE, o[0], o[1], o[2], errors)) return;
        checkHasMaintenanceHatch(errors);
        if (gasHatchCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.eye.gas"));
        }
        if (outputBusCount() == 0 || outputHatchCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.eye.output"));
        }

        bandBuilt[0] = true;

        // The OPTIONAL bands, in tier order, each with its own error list, so nothing they report
        // un-forms the core. Every band is walked (so the scanner can name a band that stands without
        // the one below it), but D-49 makes them cumulative: a band counts only while every band below
        // it counts, and a band that does not count drops the stellar sockets it registered
        // (mark-and-truncate).
        final List<StructureError> pieceErrors = new ArrayList<>();
        boolean below = true;
        for (int i = 1; i < PIECES.length; i++) {
            pieceErrors.clear();
            final int mark = stellarHatches.size();
            final int[] rMark = { receptors[0].size(), receptors[1].size(), receptors[2].size() };
            final int[] at = off(PIECES[i]);
            bandBuilt[i] = checkPiece(PIECES[i], at[0], at[1], at[2], pieceErrors);
            bandFault[i] = fault(bandBuilt[i], pieceErrors);
            below = below && bandBuilt[i];
            if (!below) {
                while (stellarHatches.size() > mark) {
                    stellarHatches.remove(stellarHatches.size() - 1);
                }
                // P4: a rail or halo that does not count drops its Receptor Ports too
                for (int g = 0; g < 3; g++) {
                    while (receptors[g].size() > rMark[g]) receptors[g].remove(receptors[g].size() - 1);
                }
            }
        }
        // D-49 migration (plan §13): a pre-D-49 Eye's bands are grandfathered under the old rule. Every successful
        // walk raises it (grandfather keeps the max); it is settled only by a walk that saw the T1-T3 footprint's
        // chunks all loaded, so an Eye first walked with its band chunks unloaded is not locked low for good.
        if (errors.isEmpty() && runtime.legacyPending()) {
            runtime.grandfather(legacyStructureTier(), legacyFootprintLoaded(aBaseMetaTileEntity));
        }
        // QA-A11: the bands the footprint check covers: raised by every formed walk, lowered only by one that saw the
        // whole footprint it covered loaded (a band taken down on purpose)
        final int tier = structureTier();
        if (footprintLoaded(aBaseMetaTileEntity)) footprintBands = Math.max(1, tier);
        else footprintBands = Math.max(footprintBands, tier);
    }

    // ============================================================================ chunk freeze (QA-A11)

    /**
     * QA-A11 / R-8: the pieces (core first) whose chunks must all be loaded for the Eye to run: the most a formed walk
     * has counted (tile NBT); 0 before any, then the unlocked tier stands in (the transitions needed those bands).
     */
    private int footprintBands;
    /** The footprint's chunks were all loaded at the last field second (false: the Eye is frozen). */
    private boolean footprintLoaded = true;
    private int[] footprintBox;
    private com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing footprintBoxFacing;
    private int footprintBoxBands;
    /** QA-A11 / QS-02: a periodic re-walk (ticks), as the KEE and the quasar make, while the footprint is loaded. */
    private static final int RECHECK = 200;

    private int footprintPieces() {
        final int bands = footprintBands > 0 ? footprintBands : runtime.unlockedTier();
        return Math.max(1, Math.min(PIECES.length, bands));
    }

    /**
     * True when every chunk under the footprint's pieces is loaded (never loads one). StructureLib fails a cell in
     * an unloaded chunk as a missing block, so a walk then is not to be believed, and the Eye freezes instead of
     * putting its star out (R-8: "an unloaded chunk freezes it"), as the KEE and the quasar do.
     */
    private boolean footprintLoaded(IGregTechTileEntity base) {
        if (base == null || base.getWorld() == null) return false;
        final int bands = footprintPieces();
        final com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing f = getExtendedFacing();
        if (footprintBox == null || footprintBoxFacing != f || footprintBoxBands != bands) {
            footprintBox = com.nateyburr.beyondthestars.machines.SLConsoleTest
                .bounds(f, base.getXCoord(), base.getYCoord(), base.getZCoord(), Arrays.copyOf(PIECES, bands));
            footprintBoxFacing = f;
            footprintBoxBands = bands;
        }
        final int[] b = footprintBox;
        return base.getWorld()
            .checkChunksExist(b[0], 0, b[2], b[3], 255, b[5]);
    }

    /** GT's per-check tile update, kept to real changes: the periodic re-walk must not resend the controller. */
    private final com.nateyburr.beyondthestars.machines.QuietTileUpdate quietTileUpdate = new com.nateyburr.beyondthestars.machines.QuietTileUpdate();

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onStructureCheckFinished(quietTileUpdate.wrap(aBaseMetaTileEntity));
        quietTileUpdate.after(this, aBaseMetaTileEntity);
    }

    /** QA-A11: frozen (a footprint chunk is not loaded): no drain, no field second; nothing is lost. GUI, scanner. */
    public boolean isFrozen() {
        return !footprintLoaded;
    }

    /**
     * The field second's freeze gate (server): false while a footprint chunk is unloaded. When the chunks come back
     * the Eye re-walks before its star moves again, so a walk made across unloaded chunks never puts it out; every
     * {@link #RECHECK} ticks it re-walks anyway (edits far from the controller reach it, QS-02).
     */
    private boolean footprintGate(IGregTechTileEntity base, long aTick) {
        if (mStartUpCheck >= 0) return true; // the runtime waits for GT's first walk by itself
        final boolean loaded = footprintLoaded(base);
        if (!loaded) {
            footprintLoaded = false;
            return false;
        }
        if (!footprintLoaded || aTick % RECHECK == 0) {
            footprintLoaded = true;
            checkStructure(true, base);
            resumeIfRepaired(base);
        }
        return true;
    }

    /** "" per piece, so the status lines of a tile that has not been walked yet read "absent". */
    private static String[] emptyFaults() {
        final String[] faults = new String[PIECES.length];
        Arrays.fill(faults, "");
        return faults;
    }

    private static String fault(boolean built, List<StructureError> walkErrors) {
        if (built || walkErrors.isEmpty()) return "";
        final String reason = BTSStructures.oneLine(
            walkErrors.get(0)
                .getDisplayString());
        return reason == null || reason.isEmpty() ? "incomplete" : reason;
    }

    /** True when recipes of Eye tier {@code tier} may run on this structure (server state). */
    public boolean tierAvailable(int tier) {
        return bandsAvailable(tier) && compressionAllows(tier);
    }

    /** Q-20: every compression generator reaches the tier's gate (the one table). */
    public boolean compressionAllows(int tier) {
        return tier >= 1 && tier < COMPRESSION_META_FOR_TIER.length
            && fieldMin[FIELD_COMPRESSION] >= COMPRESSION_META_FOR_TIER[tier];
    }

    /**
     * The structural half of {@link #tierAvailable}: the cumulative bands, and D-49's permanent
     * unlocked tier (milestone, core, transition; §6.5 {@code installedTier = min(unlockedTier,
     * structureTier, compressionTier)}).
     */
    private boolean bandsAvailable(int tier) {
        return tier >= 1 && tier <= structureTier() && tier <= runtime.unlockedTier();
    }

    /**
     * Plan §13: the tier a pre-D-49 Eye's bands gave it under the OLD rule, where the jet crescent
     * counted without the beam band (T1-T3; T4 came from the Galactic core). Server state.
     */
    private int legacyStructureTier() {
        if (!mMachine && !bandBuilt[0]) return 0;
        return bandBuilt[2] ? 3 : bandBuilt[1] ? 2 : 1;
    }

    /**
     * True when every chunk under the core, beam band and jet crescent (the pieces the old rule counted) is loaded,
     * so a band that failed its walk really is not there (StructureLib fails an unloaded cell as a missing block).
     */
    private boolean legacyFootprintLoaded(IGregTechTileEntity base) {
        if (base == null || base.getWorld() == null) return false;
        final int[] box = com.nateyburr.beyondthestars.machines.SLConsoleTest.bounds(
            getExtendedFacing(),
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            PIECE_CORE,
            PIECE_BEAM,
            PIECE_JET);
        final net.minecraft.world.chunk.IChunkProvider chunks = base.getWorld()
            .getChunkProvider();
        for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
            for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                if (!chunks.chunkExists(cx, cz)) return false;
            }
        }
        return true;
    }

    /**
     * D-49 §6.5 {@code structureTier}: core = 1, + beam band = 2, + jet crescent = 3, + spiral arms = 4,
     * + second arms = 5, + galactic rim = 6. CUMULATIVE: a band counts only if every band below it
     * stands (a jet crescent without the beam band is a T1 Eye). 0 if unformed. Server state.
     */
    public int structureTier() {
        if (!mMachine) return 0;
        int tier = 1;
        while (tier < PIECES.length && bandBuilt[tier]) tier++;
        return tier;
    }

    /**
     * D-49 §6.5 {@code compressionTier}: the highest tier whose 1:1 gate every compression generator
     * meets ({@link #COMPRESSION_META_FOR_TIER}), 0 if none was walked.
     */
    public int compressionTier() {
        int tier = 0;
        for (int t = 1; t < COMPRESSION_META_FOR_TIER.length; t++) {
            if (compressionAllows(t)) tier = t;
        }
        return tier;
    }

    /** The highest tier available, 0 if unformed. */
    public int installedTier() {
        for (int t = MAX_TIER; t >= 1; t--) {
            if (tierAvailable(t)) return t;
        }
        return 0;
    }

    /** True if piece {@code index} (0 = core .. 5 = rim) passed its own walk, whether or not it counts. */
    public boolean bandStands(int index) {
        return mMachine && index >= 0 && index < PIECES.length && bandBuilt[index];
    }

    /** Lowest tier (generator meta 0-8) of one field type, or -1 if none was walked. */
    public int fieldTier(int type) {
        return fieldMin[type];
    }

    // ===================================================================== private hatch adders

    private static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    public boolean addGasHatch(IGregTechTileEntity te, int casingIndex) {
        return addInput(te, casingIndex, gasHatches);
    }

    public boolean addStellarHatch(IGregTechTileEntity te, int casingIndex) {
        return addInput(te, casingIndex, stellarHatches);
    }

    /**
     * D-49 Field socket: a plain input hatch joins the trim list; an ME stocking hatch is accepted (the
     * Eye still forms) but kept on the rejected list, never drained (R-15).
     */
    public boolean addFieldHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInputME) {
            ((MTEHatchInputME) mte).updateTexture(casingIndex);
            rejectedFieldHatches.add((MTEHatchInputME) mte);
            return true;
        }
        return addInput(te, casingIndex, fieldHatches);
    }

    /** D-49 Sensor socket: Stellar Field Sensor Hatches only. */
    public boolean addSensorHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchStellarFieldSensor) {
            ((MTEHatchStellarFieldSensor) mte).updateTexture(casingIndex);
            sensorHatches.add((MTEHatchStellarFieldSensor) mte);
            return true;
        }
        return false;
    }

    /** AUTOMATION PASS: a Directive Hatch (MTE 28127) in a Sensor socket. */
    public boolean addDirectiveHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchDirective) {
            ((MTEHatchDirective) mte).updateTexture(casingIndex);
            directiveHatches.add((MTEHatchDirective) mte);
            return true;
        }
        return false;
    }

    /**
     * D-49 R-1: an input bus (ME input bus included) on a plinth gas socket, for the ignition charges.
     * Crafting input buffers are refused, as the reactor's sockets refuse them (B-31): they hold
     * patterns, not stock.
     */
    public boolean addItemBus(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInputBus && !(mte instanceof MTEHatchCraftingInputME)) {
            ((MTEHatchInputBus) mte).updateTexture(casingIndex);
            itemBuses.add((MTEHatchInputBus) mte);
            return true;
        }
        return false;
    }

    /**
     * P4: a GRB Receptor Port in an 'r' socket of the rail or the halo. Its group is the manifest's for the socket at
     * its offset from the controller (P4-03); StructureLib only calls this at 'r' cells, so one always matches.
     */
    public boolean addReceptor(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchGRBReceptor)) return false;
        ((MTEHatchGRBReceptor) mte).updateTexture(casingIndex);
        receptors[receptorGroup(te.getXCoord(), te.getYCoord(), te.getZCoord())].add((MTEHatchGRBReceptor) mte);
        return true;
    }

    /** The group of the 'r' socket at world (x, y, z) for this controller (rail-low if none matches). */
    private int receptorGroup(int x, int y, int z) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null) return GROUP_LOW;
        final int[] xyz = new int[3];
        for (ReceptorSocket s : receptorSockets()) {
            getExtendedFacing().getWorldOffset(s.rel, xyz);
            if (base.getXCoord() + xyz[0] == x && base.getYCoord() + xyz[1] == y && base.getZCoord() + xyz[2] == z) {
                return s.group;
            }
        }
        return GROUP_LOW;
    }

    /** One 'r' socket of the manifest: its offset from the controller and its group. */
    public static final class ReceptorSocket {

        public final int[] rel;
        public final int group;

        ReceptorSocket(int[] rel, int group) {
            this.rel = rel;
            this.group = group;
        }
    }

    private static volatile List<ReceptorSocket> RECEPTOR_SOCKETS;

    /** The 16 'r' sockets of eye_rail and eye_halo with their groups (manifest, read once). */
    public static List<ReceptorSocket> receptorSockets() {
        List<ReceptorSocket> out = RECEPTOR_SOCKETS;
        if (out != null) return out;
        out = new ArrayList<>();
        for (String piece : new String[] { PIECE_RAIL, PIECE_HALO }) {
            try {
                final com.google.gson.JsonObject entry = BTSShapes.manifestPiece(piece);
                if (entry == null || !entry.has("sockets")) continue;
                final com.google.gson.JsonObject sockets = entry.getAsJsonObject("sockets");
                if (!sockets.has("r")) continue;
                for (com.google.gson.JsonElement e : sockets.getAsJsonArray("r")) {
                    final com.google.gson.JsonObject o = e.getAsJsonObject();
                    final com.google.gson.JsonArray rel = o.getAsJsonArray("rel");
                    final String g = o.has("group") ? o.get("group")
                        .getAsString() : "";
                    final int group = "rail-high".equals(g) ? GROUP_HIGH : "halo".equals(g) ? GROUP_HALO : GROUP_LOW;
                    out.add(
                        new ReceptorSocket(
                            new int[] { rel.get(0)
                                .getAsInt(),
                                rel.get(1)
                                    .getAsInt(),
                                rel.get(2)
                                    .getAsInt() },
                            group));
                }
            } catch (RuntimeException bad) {
                BeyondTheStars.LOG.warn("[BTS] Eye: the manifest's {} receptor sockets are unreadable: {}", piece, bad);
            }
        }
        RECEPTOR_SOCKETS = out;
        return out;
    }

    /** P4: a Burst Control Hatch in a 'd' socket (Begin mode). */
    public boolean addBurstControl(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchBurstControl)) return false;
        ((MTEHatchBurstControl) mte).updateTexture(casingIndex);
        burstControls.add((MTEHatchBurstControl) mte);
        return true;
    }

    /** P4: the slot bus ('p'): an input bus, ME input bus included; crafting input buffers refused. */
    public boolean addSlotBus(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInputBus && !(mte instanceof MTEHatchCraftingInputME)) {
            ((MTEHatchInputBus) mte).updateTexture(casingIndex);
            slotBuses.add((MTEHatchInputBus) mte);
            return true;
        }
        return false;
    }

    public int receptorCount(int group) {
        return liveCount(receptors[group]);
    }

    public int receptorCount() {
        return receptorCount(GROUP_HIGH) + receptorCount(GROUP_LOW) + receptorCount(GROUP_HALO);
    }

    public int burstControlCount() {
        return liveCount(burstControls);
    }

    public int slotBusCount() {
        return liveCount(slotBuses);
    }

    /** P4: the Receptor Ports of one group. Server state, read-only. */
    public List<MTEHatchGRBReceptor> receptors(int group) {
        return Collections.unmodifiableList(receptors[group]);
    }

    /** P4 bench: the first live slot bus, or null. */
    public MTEHatchInputBus firstSlotBus() {
        for (MTEHatchInputBus bus : slotBuses) if (live(bus)) return bus;
        return null;
    }

    /** P4: the Burst Control Hatches. Server state, read-only. */
    public List<MTEHatchBurstControl> burstControlHatches() {
        return Collections.unmodifiableList(burstControls);
    }

    private static boolean addInput(IGregTechTileEntity te, int casingIndex, List<MTEHatchInput> list) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchInput) {
            ((MTEHatchInput) mte).updateTexture(casingIndex);
            list.add((MTEHatchInput) mte);
            return true;
        }
        return false;
    }

    /** ME outputs only: they are the Eye's only output path, so they join GT's output lists. */
    public boolean addOutputME(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEHatchOutputBusME) {
            ((MTEHatchOutputBusME) mte).updateTexture(casingIndex);
            mOutputBusses.add((MTEHatchOutputBusME) mte);
            return true;
        }
        if (mte instanceof MTEHatchOutputME) {
            ((MTEHatchOutputME) mte).updateTexture(casingIndex);
            mOutputHatches.add((MTEHatchOutputME) mte);
            return true;
        }
        return false;
    }

    private static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity mte : list) {
            if (live(mte)) n++;
        }
        return n;
    }

    public int gasHatchCount() {
        return liveCount(gasHatches);
    }

    public int stellarHatchCount() {
        return liveCount(stellarHatches);
    }

    public int fieldHatchCount() {
        return liveCount(fieldHatches);
    }

    public int rejectedFieldHatchCount() {
        return liveCount(rejectedFieldHatches);
    }

    public int sensorHatchCount() {
        return liveCount(sensorHatches);
    }

    public int directiveHatchCount() {
        return liveCount(directiveHatches);
    }

    public int itemBusCount() {
        return liveCount(itemBuses);
    }

    /** D-49: the Field-socket trim hatches, for the star runtime. Server state, read-only. */
    public List<MTEHatchInput> fieldHatches() {
        return Collections.unmodifiableList(fieldHatches);
    }

    /** D-49: the Sensor-socket hatches, for the star runtime's publish. Server state, read-only. */
    public List<MTEHatchStellarFieldSensor> sensorHatches() {
        return Collections.unmodifiableList(sensorHatches);
    }

    /** AUTOMATION PASS: the Sensor-socket Directive Hatches. Server state, read-only. */
    public List<MTEHatchDirective> directiveHatches() {
        return Collections.unmodifiableList(directiveHatches);
    }

    /** D-49: the plinth input buses, for the ignition charges. Server state, read-only. */
    public List<MTEHatchInputBus> itemBuses() {
        return Collections.unmodifiableList(itemBuses);
    }

    public int outputBusCount() {
        return liveCount(mOutputBusses);
    }

    public int outputHatchCount() {
        return liveCount(mOutputHatches);
    }

    // ==================================================================== construct / survival

    /** Trap 1: the trigger stack alone picks how many bands to project (1 = T1 .. 6 = T6, cumulative). */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int n = bandsFor(stackSize);
        for (int i = 0; i < n; i++) {
            final int[] o = off(PIECES[i]);
            buildPiece(PIECES[i], stackSize, hintsOnly, o[0], o[1], o[2]);
        }
    }

    private static int bandsFor(ItemStack stack) {
        final int size = stack == null ? 1 : stack.stackSize;
        return Math.max(1, Math.min(PIECES.length, size));
    }

    /**
     * Survival auto-place (projector, from the player's inventory or a linked AE system, and NEI's
     * preview, which builds through here with a creative item source). The pieces go in order, core
     * first, and a band starts only once every piece before it is complete; -1 when all the stack
     * asks for is standing. Hatches and other tiles are never overwritten ({@code noOverwrite}).
     */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final int n = bandsFor(stackSize);
        if (mMachine && installedBands() >= n) return -1;
        final int budget = BTSStructures.survivalBudget(elementBudget);
        for (int i = 0; i < n; i++) {
            final int[] o = off(PIECES[i]);
            final int built = survivalBuildPiece(PIECES[i], stackSize, o[0], o[1], o[2], budget, env, false, true);
            if (built >= 0) return built;
        }
        return -1;
    }

    private int installedBands() {
        return structureTier();
    }

    // =================================================================== cores, escrow, screwdriver

    /**
     * D-49 §8: right-click holding a transition core puts the core for {@code unlockedTier →
     * unlockedTier+1} into the escrow (one item; any other core is refused, naming the one needed).
     * It is consumed only when the transition succeeds. This replaces the Galactic Upgrade
     * Core's instant install (A-2/B-1 superseded).
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        final ItemStack held = aPlayer.getHeldItem();
        if (held != null && EyeStarRuntime.isCore(held.getItem())) {
            if (aBaseMetaTileEntity.isServerSide()) {
                final boolean[] taken = new boolean[1];
                final String line = runtime.offerCore(held, taken);
                if (taken[0] && !aPlayer.capabilities.isCreativeMode) {
                    held.stackSize--;
                    if (held.stackSize <= 0) {
                        aPlayer.inventory.setInventorySlotContents(aPlayer.inventory.currentItem, null);
                    }
                }
                GTUtility
                    .sendChatToPlayer(aPlayer, (taken[0] ? EnumChatFormatting.AQUA : EnumChatFormatting.YELLOW) + line);
            }
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer);
    }

    /**
     * D-49 R-14: sneak-PUNCH with an empty hand returns the escrowed core. (Plan R-14 said
     * sneak-right-click, but GT never passes a sneaking right-click to the machine: B-37.)
     */
    @Override
    public void onLeftclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        super.onLeftclick(aBaseMetaTileEntity, aPlayer);
        if (!aBaseMetaTileEntity.isServerSide() || aPlayer == null
            || !aPlayer.isSneaking()
            || aPlayer.getHeldItem() != null
            || runtime.escrow() == null) return;
        if (runtime.transition() != null) {
            GTUtility.sendChatToPlayer(
                aPlayer,
                EnumChatFormatting.YELLOW + "The "
                    + runtime.transition().type.title
                    + " is running: the core stays in escrow until it ends.");
            return;
        }
        if (!mayTakeEscrow(aBaseMetaTileEntity, aPlayer)) {
            GTUtility.sendChatToPlayer(
                aPlayer,
                EnumChatFormatting.YELLOW + "Only the Eye's owner (or their team) can take the escrowed core back.");
            return;
        }
        final ItemStack core = runtime.takeEscrow();
        if (core == null) return;
        if (!aPlayer.inventory.addItemStackToInventory(core)) aPlayer.dropPlayerItemWithRandomChoice(core, false);
        GTUtility.sendChatToPlayer(aPlayer, "Escrow returned: " + core.getDisplayName() + ".");
    }

    /**
     * QA-A08: the escrowed core goes back only to the Eye's owner, a player on the owner's team (the wireless
     * network's leader is the same), or a creative-mode player. An Eye with no owner returns it to anyone.
     */
    private static boolean mayTakeEscrow(IGregTechTileEntity base, EntityPlayer player) {
        final UUID owner = base.getOwnerUuid();
        if (owner == null || player.capabilities.isCreativeMode) return true;
        final UUID who = player.getUniqueID();
        if (owner.equals(who)) return true;
        final UUID a = gregtech.common.misc.spaceprojects.SpaceProjectManager.getLeader(owner);
        final UUID b = gregtech.common.misc.spaceprojects.SpaceProjectManager.getLeader(who);
        return a != null && a.equals(b);
    }

    /**
     * Screwdriver (plan §12): plain = the time dial +0.25 (wraps 3.00 → 1.00; capped in use by the
     * Time Dilation generators); sneaking = the T4 HSM feed +0.1 L/s (wraps 1.5 → 0). Single-recipe
     * locking is not offered.
     */
    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (aPlayer.isSneaking()) {
            final double feed = runtime.stepFeed();
            GTUtility.sendChatToPlayer(
                aPlayer,
                "Eye HSM feed setpoint: " + EnumChatFormatting.AQUA
                    + String.format(Locale.ROOT, "%.3f", feed)
                    + " L/s"
                    + EnumChatFormatting.RESET
                    + " per parallel (the neutron star; 0 .. 1.5)");
            return;
        }
        final double dial = runtime.stepDial();
        GTUtility.sendChatToPlayer(
            aPlayer,
            "Eye time dial: " + EnumChatFormatting.AQUA
                + String.format(Locale.ROOT, "%.2f", dial)
                + EnumChatFormatting.RESET
                + " (each +0.25 costs 10 x 2^(N-1) L/s of Tachyon Rich Temporal Fluid from the Field sockets;"
                + " the cap is 1 + 0.25 x (Time Dilation tier - star tier))");
    }

    // ===================================================================================== GUI

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new EyeGui(this);
    }

    /** Formed (the GUI's head line). Server state. */
    public boolean isFormedForGui() {
        return mMachine;
    }

    /** GUI cleanup: the kit's badge says it (spec §0.6); GT's "Running perfectly." line is not added. */
    @Override
    public boolean hasRunningText() {
        return false;
    }

    /** GUI cleanup: GT's recipe-result line only for a planet run; a star's state is the badge and checklist. */
    @Override
    public boolean shouldDisplayCheckRecipeResult() {
        return targetIsPlanet();
    }

    /** GUI cleanup (spec Q2): wireless-only, it computes its own parallel; frees the gap row. */
    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    /** The controller slot holds a planet (a dimension display block with an Eye recipe), not a Star Chart. */
    public boolean targetIsPlanet() {
        return planetTarget() != null;
    }

    /** The planet recipe the controller slot selects, or null (empty, a Star Chart, anything else). */
    public EyeRecipe planetTarget() {
        final ItemStack target = getControllerSlot();
        if (target == null || StarDef.byChart(target) != null) return null;
        return EyeRecipeRegistry.lookup(target);
    }

    /** GUI: the planet run's parallel (0 when none runs). */
    public long runningParallelForGui() {
        return runningKey == null ? 0 : runningParallel;
    }

    /** GUI: the last planet run's result line, or "". */
    public String lastRunForGui() {
        return lastRun == null ? "" : lastRun;
    }

    /**
     * GUI (controller-gui-spec.md §5.1.3, planet target): what one planet run needs, every row met or not, read
     * from the same state {@link #checkProcessing()} reads: the tier gate, one run of each input in the buffers, an
     * owner and one run's EU in the wireless network. Consumes nothing.
     */
    public BTSChecklist planetChecklist() {
        final BTSChecklist c = new BTSChecklist();
        final EyeRecipe recipe = planetTarget();
        if (recipe == null) return c;
        final CheckRecipeResult gate = tierGate(recipe.tier);
        c.add(
            gate == null,
            BTSText.lang("beyondthestars.gui.eye.chk.reach", recipe.tier),
            BTSText.lit("T" + installedTier() + " / T" + recipe.tier),
            -1,
            // getID() is the result TYPE ("simple_result"), not its lang key: send the server's display string.
            gate == null ? "" : BTSText.lit(gate.getDisplayString()));
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            if (in.fluid == null || in.amount <= 0) continue;
            final long have = buffer(in.fluid);
            final String name = in.fluid.getLocalizedName(new FluidStack(in.fluid, 1));
            c.add(
                have >= in.amount,
                BTSText.lit(name),
                BTSText.lit(BTSFormat.haveNeed(have, in.amount) + " L"),
                BTSChecklist.frac(have, in.amount),
                BTSText.lang("beyondthestars.gui.eye.planet.input.tip", name, fmt(have), fmt(in.amount)));
        }
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final UUID uuid = base == null ? null : base.getOwnerUuid();
        if (uuid == null) {
            c.add(
                false,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lang("beyondthestars.gui.eye.chk.noowner"),
                -1,
                BTSText.lang(EyeStarRuntime.RESULT_KEY + "noowner"));
        } else {
            final BigInteger have = WirelessNetworkManager.getUserEU(uuid);
            final boolean ok = affordable(uuid, recipe) >= 1;
            c.add(
                ok,
                BTSText.lang("beyondthestars.gui.eye.chk.eu"),
                BTSText.lit(
                    ok ? BTSFormat.eu((double) recipe.euPerRun)
                        : BTSFormat.eu(have) + " / " + BTSFormat.eu((double) recipe.euPerRun)),
                -1,
                BTSText.lang(EyeStarRuntime.RESULT_KEY + "noeu"));
        }
        return c;
    }

    /** The D-49 star runtime (bench commands, kit). Server state. */
    public EyeStarRuntime starRuntime() {
        return runtime;
    }

    /** Creative / bench: sets the permanent unlocked tier (the kit's T{@code n} build). */
    public void debugUnlock(int n) {
        runtime.setUnlocked(n);
    }

    // ============================================================================== buffers

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        resumeIfRepaired(aBaseMetaTileEntity);
        if (aTick % DRAIN_INTERVAL == 0 && !footprintGate(aBaseMetaTileEntity, aTick)) {
            // frozen: the hatches only hear that the Eye reports (their codes stay as they were)
            publishDirectives();
            aBaseMetaTileEntity.markDirty();
        } else if (aTick % DRAIN_INTERVAL == 0) {
            refreshSlotItems();
            if (mMachine) {
                takeFromPlinthBuses();
                drainSockets();
            }
            // D-49: one field second, straight after the drain (plan §4). Runs unformed too, so a broken
            // structure puts the star out (R-8); a chunk that is not ticking freezes it.
            com.nateyburr.beyondthestars.machines.eye.star.EyeGalaxyBench.liveSecond(this); // P4 bench only
            runtime.second();
            publishDirectives();
            refreshStatusLine();
            // The buffers fill from the sockets and the field, milestones and setpoints move without an inventory
            // change of this tile, and GT marks a chunk for saving only on those. The runtime marks on most changes;
            // this once-a-second mark covers every path (a dark Eye filling its buffers too), as the KEE and P4's
            // machines do (B-112, P4-85).
            aBaseMetaTileEntity.markDirty();
        }
        // GT sets the active flag from its run progress; a burning star is active too (Activity Detector).
        if (runtime.burning()) aBaseMetaTileEntity.setActive(true);
        // D-49: the centre render follows the star (not before the first walk after a load: the star is frozen).
        if (mStartUpCheck < 0) centre.tick(aBaseMetaTileEntity, getExtendedFacing(), mMachine, runtime, aTick);
    }

    /** The star state the GUI's status line last showed (GT's recipe-check result), see {@link #refreshStatusLine}. */
    private String statusKey = "";

    /**
     * The controller GUI's head line is GT's last recipe-check result ({@link #starStatus}), and on GT 5.09.54 recipe
     * checks are event-driven (hatches push them; no periodic poll once the machine has idled). A star lights, burns
     * out or starts a transition with no hatch event, so the line stayed on "The star is dark" for a burning star
     * (seen in the automation photo run). Push a check whenever the star's state, a transition or the unlocked tier
     * changes.
     */
    private void refreshStatusLine() {
        if (!mMachine) return;
        final String key = runtime.state() + "/" + (runtime.transition() != null) + "/" + runtime.unlockedTier();
        if (key.equals(statusKey)) return;
        statusKey = key;
        scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
    }

    // ============================================================================ centre render

    /** True when this Eye's centre render block stands at (x, y, z) (the render tile asks). */
    public boolean claimsRender(int x, int y, int z) {
        return centre.claims(x, y, z);
    }

    /** The per-machine centre render switch (GUI). */
    public boolean isRenderEnabled() {
        return centre.isEnabled();
    }

    public void setRenderEnabled(boolean on) {
        centre.setEnabled(on);
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) base.markDirty();
    }

    /**
     * Dev-only render QA ({@code -Dbts.rendertest=true}): the centre shows {@code look} while formed (null: the
     * star's).
     */
    public void debugForceLook(com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook look) {
        centre.debugForceLook(look);
    }

    /** Why the centre render is missing although it should show, or "". */
    public String getRenderNote() {
        return centre.note();
    }

    /** Bench {@code render}: the centre cell, what stands there and the look its tile holds. Server state. */
    public List<String> debugRenderLines() {
        final List<String> out = new ArrayList<>();
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return out;
        final int[] at = EyeCentreRender
            .centrePos(getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord());
        final net.minecraft.world.World world = base.getWorld();
        final Block block = world.getBlock(at[0], at[1], at[2]);
        final net.minecraft.tileentity.TileEntity te = world.getTileEntity(at[0], at[1], at[2]);
        out.add(
            "Centre " + at[0]
                + ", "
                + at[1]
                + ", "
                + at[2]
                + ": "
                + Block.blockRegistry.getNameForObject(block)
                + (claimsRender(at[0], at[1], at[2]) ? " (claimed by this Eye)" : " (not claimed)")
                + "; switch "
                + (centre.isEnabled() ? "on" : "off")
                + ", config "
                + (Config.eyeRenderCentre ? "on" : "off"));
        if (te instanceof TileEyeStar) {
            final TileEyeStar t = (TileEyeStar) te;
            out.add(
                String.format(
                    Locale.ROOT,
                    "Look: kind %d (%s), radius %.2f, rgb %06X, gamma %.2f, spin %.1f, pulse %.3f",
                    t.getKind(),
                    centre.label(),
                    t.getRadius(),
                    t.getRgb(),
                    t.getGamma(),
                    t.getSpin(),
                    t.getPulse()));
        }
        if (!centre.note()
            .isEmpty()) out.add(centre.note());
        final com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook want = com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook
            .of(runtime);
        out.add(
            "Wanted: " + (want == null ? "none" : want.label() + " (kind " + want.kind + ")")
                + ", formed "
                + mMachine
                + ", start-up check "
                + (mStartUpCheck < 0 ? "done" : "pending"));
        return out;
    }

    /** Broken: the centre render goes with the controller. */
    @Override
    public void onBlockDestroyed() {
        centre.remove(getBaseMetaTileEntity());
        super.onBlockDestroyed();
    }

    /** Any removal of the tile (a wrench too). GT does not call it on a chunk unload; the render tile polices that. */
    @Override
    public void onRemoval() {
        centre.remove(getBaseMetaTileEntity());
        super.onRemoval();
    }

    /**
     * Bench: a tile just (re)loaded with its chunk has not been walked yet, and its star freezes until
     * GT's start-up check; the bench walks it at once instead of waiting.
     */
    public void benchEnsureChecked() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (mStartUpCheck < 0 || base == null) return;
        checkStructure(true, base);
        mStartUpCheck = -1;
    }

    /**
     * Bench: one field second now (the socket drain, then the star, then the centre render), as the 20-tick cadence
     * does.
     */
    public void fieldSecond() {
        refreshSlotItems();
        if (mMachine) {
            takeFromPlinthBuses();
            drainSockets();
        }
        runtime.second();
        publishDirectives();
        debugRenderTick();
    }

    /**
     * Bench: one centre-render update now (it touches the world), so a console test does not depend on the dev
     * server ticking this chunk (with no player there, it unloads and reloads the Eye between commands).
     */
    public void debugRenderTick() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) centre.tick(base, getExtendedFacing(), mMachine, runtime, 0);
    }

    // ============================================================================ automation

    /**
     * AUTOMATION PASS (docs/automation.md): what a plinth input bus may hand the controller, once a second before the
     * drain, so AE2, Steve's Factory Manager, a conveyor cover or an OpenComputers transposer can do what a player does
     * by hand:
     * <ul>
     * <li><b>the next transition core</b> (the one {@link EyeStarRuntime#offerCore} would take from a right-click) goes
     * into the escrow; a wrong core stays in the bus;</li>
     * <li><b>a star chart</b> different from the controller slot's target goes into the controller slot, and the old
     * target leaves through the ME output bus (same rules as swapping it by hand: the next tier's chart while the star
     * burns starts the transition, any other change puts the star out). Plain buses only: an ME stocking bus would show
     * its configured chart every second and swap it back and forth. While a transition runs or waits (PENDING) no chart
     * is swapped in (B-72): the swap would abort it.</li>
     * <li><b>a duplicate</b> of the controller slot's chart (a redstone-carded export bus pushing again before the bit
     * drops, a conveyor moving a stack) leaves through the ME output bus, so it can never be swapped back in later and
     * put out the next star (B-72).</li>
     * </ul>
     * One chart a second.
     */
    private void takeFromPlinthBuses() {
        final Item core = EyeStarRuntime.coreFor(runtime.unlockedTier());
        final ItemStack target = getControllerSlot();
        final boolean holdChart = runtime.transition() != null || runtime.state() == EyeStarRuntime.StarState.PENDING;
        for (MTEHatchInputBus bus : itemBuses) {
            if (!live(bus)) continue;
            // the next core: read and taken in one pass, so what goes into escrow is the item that left the bus
            // (S-01; an ME input bus's stocked cores too, QA-A07)
            if (core != null && runtime.escrow() == null && mayHold(bus, core)) {
                final ItemStack one = takeFromBus(bus, st -> st.getItem() == core);
                if (one != null) {
                    final boolean[] taken = new boolean[1];
                    runtime.offerCore(one, taken);
                    if (taken[0]) {
                        runtime.noteEvent(
                            "automation: the " + one.getDisplayName() + " came from a plinth input bus into escrow");
                    } else {
                        returnToOutput(one); // never lost (an escrow filled the same second)
                    }
                }
            }
            // charts: plain buses only (an ME stocking bus would show its configured chart every second)
            if (bus instanceof IRecipeProcessingAwareHatch) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                final ItemStack stack = bus.getStackInSlot(i);
                if (stack == null || stack.stackSize <= 0 || StarDef.byChart(stack) == null) continue;
                if (target != null && GTUtility.areStacksEqual(stack, target, true)) {
                    ejectDuplicate(bus, i, stack);
                    continue;
                }
                if (holdChart) continue;
                swapTarget(bus, i, stack, target);
                return;
            }
        }
    }

    /**
     * Whether {@code bus} may hold {@code item} now, without opening a window: a plain bus always (it is read
     * directly); an ME input bus when its manual slot holds it or one of its stocking slots is configured to it.
     */
    private static boolean mayHold(MTEHatchInputBus bus, Item item) {
        if (!(bus instanceof MTEHatchInputBusME)) return true;
        final MTEHatchInputBusME me = (MTEHatchInputBusME) bus;
        final ItemStack manual = me.getBaseMetaTileEntity()
            .getStackInSlot(me.getManualSlot());
        if (manual != null && manual.getItem() == item && manual.stackSize > 0) return true;
        for (int i = 0; i < MTEHatchInputBusME.SLOT_COUNT; i++) {
            final ItemStack config = me.getSlotConfig(i);
            if (config != null && config.getItem() == item) return true;
        }
        return false;
    }

    /** An item the Eye took but could not use goes out through the ME output bus (or drops at the controller). */
    private void returnToOutput(ItemStack stack) {
        final ItemEjectionHelper items = new ItemEjectionHelper(this);
        if (items.ejectStack(stack.copy()) >= stack.stackSize) {
            items.commit();
            return;
        }
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.getWorld() != null) {
            base.getWorld()
                .spawnEntityInWorld(
                    new net.minecraft.entity.item.EntityItem(
                        base.getWorld(),
                        base.getXCoord() + 0.5,
                        base.getYCoord() + 1.5,
                        base.getZCoord() + 0.5,
                        stack.copy()));
        }
    }

    /** One item out of a plain bus slot (read outside a window, as the slot was found); null if none came. */
    private static ItemStack takeOne(MTEHatchInputBus bus, int slot) {
        final ItemStack stack = bus.getStackInSlot(slot);
        if (stack == null || stack.stackSize <= 0) return null;
        final ItemStack one = stack.copy();
        one.stackSize = 1;
        stack.stackSize--;
        if (stack.stackSize <= 0) bus.getBaseMetaTileEntity()
            .setInventorySlotContents(slot, null);
        bus.getBaseMetaTileEntity()
            .markDirty();
        return one;
    }

    /** A bus chart equal to the target: the whole stack out through the ME output bus (or it waits for room). */
    private void ejectDuplicate(MTEHatchInputBus bus, int slot, ItemStack stack) {
        final ItemEjectionHelper items = new ItemEjectionHelper(this);
        if (items.ejectStack(stack.copy()) < stack.stackSize) return; // no room: try again next second
        items.commit();
        bus.getBaseMetaTileEntity()
            .setInventorySlotContents(slot, null);
        bus.getBaseMetaTileEntity()
            .markDirty();
        runtime.noteEvent(
            "automation: " + stack.stackSize
                + " duplicate "
                + stack.getDisplayName()
                + " sent from a plinth input bus to the ME output bus");
    }

    /** The bus chart into the controller slot; the old target out through the ME output bus (or the swap waits). */
    private void swapTarget(MTEHatchInputBus bus, int slot, ItemStack chart, ItemStack old) {
        if (old != null) {
            final ItemEjectionHelper items = new ItemEjectionHelper(this);
            if (items.ejectStack(old.copy()) < old.stackSize) return; // no room: try again next second
            items.commit();
        }
        final ItemStack next = takeOne(bus, slot);
        if (next == null) return;
        mInventory[getControllerSlotIndex()] = next;
        markDirty();
        runtime.noteEvent(
            "automation: " + next.getDisplayName()
                + " loaded from a plinth input bus"
                + (old == null ? "" : "; " + old.getDisplayName() + " sent to the ME output bus"));
    }

    /** QA-A10: the slot bus's items, read once a field second ({@link #refreshSlotItems}); copies, never consumed. */
    private List<ItemStack> slotItems = Collections.emptyList();

    /**
     * Once a field second, before the drain and the star: what the slot bus holds, through each ME input bus's
     * processing window (its stocked slots are visible only inside one), copied for every reader of the second.
     */
    private void refreshSlotItems() {
        final List<ItemStack> out = new ArrayList<>();
        for (MTEHatchInputBus bus : slotBuses) {
            if (!live(bus)) continue;
            final boolean aware = bus instanceof IRecipeProcessingAwareHatch;
            if (aware) ((IRecipeProcessingAwareHatch) bus).startRecipeProcessing();
            try {
                final int n = bus instanceof MTEHatchInputBusME ? MTEHatchInputBusME.SLOT_COUNT
                    : bus.getSizeInventory();
                for (int i = 0; i < n; i++) {
                    final ItemStack s = bus.getStackInSlot(i);
                    if (s != null && s.getItem() != null && s.stackSize > 0) out.add(s.copy());
                }
            } finally {
                if (aware) closeWindow((IRecipeProcessingAwareHatch) bus);
            }
        }
        slotItems = out;
    }

    /**
     * One item matching {@code what} out of a plinth input bus, read and taken in the same pass (S-01): a plain bus
     * directly; a processing-aware bus (the ME input bus) inside ONE window, where its stocked slots come first and
     * its circuit and manual slots after them. An item from a stocked slot counts only if the network really gave it
     * up when the window closed; the manual slot is the bus's own inventory. Returns the item taken, or null.
     */
    private ItemStack takeFromBus(MTEHatchInputBus bus, java.util.function.Predicate<ItemStack> what) {
        final boolean aware = bus instanceof IRecipeProcessingAwareHatch;
        if (aware) ((IRecipeProcessingAwareHatch) bus).startRecipeProcessing();
        ItemStack one = null;
        boolean stocked = false;
        boolean delivered = true;
        try {
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                final ItemStack stack = bus.getStackInSlot(i);
                if (stack == null || stack.stackSize <= 0 || !what.test(stack)) continue;
                one = stack.copy();
                one.stackSize = 1;
                stack.stackSize--;
                stocked = aware && !(bus instanceof MTEHatchInputBusME && i >= MTEHatchInputBusME.SLOT_COUNT);
                if (!aware && stack.stackSize <= 0) bus.getBaseMetaTileEntity()
                    .setInventorySlotContents(i, null);
                break;
            }
        } finally {
            if (aware) delivered = closeWindow((IRecipeProcessingAwareHatch) bus);
        }
        if (one == null) return null;
        if (aware) bus.updateSlots();
        bus.getBaseMetaTileEntity()
            .markDirty();
        return stocked && !delivered ? null : one;
    }

    /** Once a second (server): every live Directive Hatch gets this Eye's codes. */
    private void publishDirectives() {
        if (mMachine) {
            formedDirectiveHatches.clear();
            formedDirectiveHatches.addAll(directiveHatches);
        }
        for (MTEHatchDirective hatch : publishedDirectives()) {
            if (live(hatch)) hatch.publish(this);
        }
    }

    /**
     * The hatches that get the codes now: the walk's when formed; unformed, the last formed walk's plus any this broken
     * walk reached (a walk that stops partway reaches only some of them, B-72). A removed hatch drops out by live().
     */
    private List<MTEHatchDirective> publishedDirectives() {
        if (mMachine || formedDirectiveHatches.isEmpty()) return directiveHatches;
        if (directiveHatches.isEmpty()) return formedDirectiveHatches;
        final List<MTEHatchDirective> all = new ArrayList<>(formedDirectiveHatches);
        for (MTEHatchDirective hatch : directiveHatches) {
            if (!all.contains(hatch)) all.add(hatch);
        }
        return all;
    }

    @Override
    public DirectiveSet.Host directiveHost() {
        return DirectiveSet.Host.EYE;
    }

    @Override
    public int directiveCode(DirectiveSet set, int window) {
        return runtime.directiveCode(set, window);
    }

    @Override
    public String directiveBitLabel(DirectiveSet set, int i) {
        return runtime.directiveBitLabel(set, i);
    }

    /**
     * Control sets: redstone 1-15 sets the time dial (1.00 .. 3.00 in 0.25 steps) or the T4 HSM feed (0 .. 1.5 L/s),
     * through the GUI's own clamping setters.
     */
    @Override
    public void directiveControl(DirectiveSet set, int strength) {
        if (set == DirectiveSet.EYE_DIAL) {
            final double v = DirectiveSet.controlValue(strength, 1, EyeStarRuntime.DIAL_MAX, EyeStarRuntime.DIAL_STEP);
            if (Math.abs(v - runtime.dialSetpoint()) > 1e-9) runtime.setDial(v);
        } else if (set == DirectiveSet.EYE_FEED) {
            final double v = DirectiveSet.controlValue(strength, 0, EyeStarRuntime.FEED_MAX, 0.001);
            if (Math.abs(v - runtime.feedSetpoint()) > 1e-9) runtime.setFeed(v);
        }
    }

    @Override
    public String directiveControlLine(DirectiveSet set) {
        if (set == DirectiveSet.EYE_DIAL) {
            return String.format(Locale.ROOT, "time dial %.2f", runtime.dialSetpoint());
        }
        if (set == DirectiveSet.EYE_FEED) {
            return String.format(Locale.ROOT, "HSM feed %.3f L/s", runtime.feedSetpoint());
        }
        return "";
    }

    /**
     * Moves the Eye's input fluids out of the sockets into the long buffers, as TecTech's EOH does, but only up to
     * {@link #intakeCaps} (B-108): an ME stocking input hatch shows the Eye up to 2.1 G L a second, and with no cap one
     * hatch emptied the whole network into the buffers.
     */
    private void drainSockets() {
        final Map<Fluid, Long> caps = intakeCaps();
        if (caps.isEmpty()) return;
        for (MTEHatchInput hatch : gasHatches) {
            drain(hatch, caps);
        }
        for (MTEHatchInput hatch : stellarHatches) {
            drain(hatch, caps);
        }
    }

    /**
     * B-108 the intake cap per fluid: the star side's ({@link EyeStarRuntime#intakeCaps}) plus, for a planet in the
     * slot, {@link #PLANET_INTAKE_RUNS} runs of its inputs at full parallel. A fluid not listed is not pulled.
     */
    public Map<Fluid, Long> intakeCaps() {
        final Map<Fluid, Long> caps = runtime.intakeCaps();
        final EyeRecipe planet = EyeRecipeRegistry.lookup(getControllerSlot());
        if (planet != null) {
            final int inst = installedTier();
            final long p = inst >= planet.tier ? 1L << (inst - planet.tier) : 1L;
            for (EyeRecipe.FluidAmount in : planet.inputs) {
                if (in.fluid == null || in.amount <= 0) continue;
                final double litres = (double) in.amount * p * PLANET_INTAKE_RUNS;
                caps.merge(in.fluid, litres >= BUFFER_CAP ? BUFFER_CAP : (long) litres, Math::max);
            }
        }
        return caps;
    }

    /** B-115: one buffer against its intake cap, for the GUI and the scanner. */
    public static final class IntakeRow {

        /** The fluid's localised name (its registry name when the fluid is gone). */
        public final String name;
        public final long held;
        /** The litres the sockets fill it to now; -1: not pulled. */
        public final long cap;

        IntakeRow(String name, long held, long cap) {
            this.name = name;
            this.held = held;
            this.cap = cap;
        }
    }

    /**
     * B-115: every buffer that holds something or that the sockets pull now, with what it holds and its intake cap
     * (B-108): the fluids the sockets pull now first (held ones in their stored order, then the ones not held yet),
     * then the held leftovers that are not pulled (B-120: so a short list never cuts the fluid the star needs).
     */
    public List<IntakeRow> intakeRows() {
        final Map<Fluid, Long> caps = intakeCaps();
        final Map<String, Fluid> shown = new LinkedHashMap<>();
        for (String name : buffers.keySet()) shown.put(name, FluidRegistry.getFluid(name));
        for (Fluid fluid : caps.keySet()) if (fluid != null) shown.putIfAbsent(fluid.getName(), fluid);
        final List<IntakeRow> out = new ArrayList<>();
        final List<IntakeRow> leftovers = new ArrayList<>();
        for (Map.Entry<String, Fluid> e : shown.entrySet()) {
            final Fluid fluid = e.getValue();
            final Long cap = fluid == null ? null : caps.get(fluid);
            final Long held = buffers.get(e.getKey());
            (cap == null ? leftovers : out).add(
                new IntakeRow(
                    fluid == null ? e.getKey() : fluid.getLocalizedName(new FluidStack(fluid, 1)),
                    held == null ? 0 : held,
                    cap == null ? -1 : Math.min(BUFFER_CAP, cap)));
        }
        out.addAll(leftovers);
        return out;
    }

    /** B-108: the litres the buffer of {@code fluid} fills to now (0: not pulled). */
    public long intakeCap(Fluid fluid) {
        if (fluid == null) return 0;
        final Long cap = intakeCaps().get(fluid);
        return cap == null ? 0 : Math.min(BUFFER_CAP, cap);
    }

    private void drain(MTEHatchInput hatch, Map<Fluid, Long> caps) {
        if (!live(hatch)) return;
        if (hatch instanceof MTEHatchInputME) {
            drainStocking((MTEHatchInputME) hatch, caps);
            return;
        }
        final boolean aware = hatch instanceof IRecipeProcessingAwareHatch;
        // what came, credited only once the hatch's window has closed cleanly (QA-A01)
        final Map<Fluid, Long> got = new LinkedHashMap<>();
        if (aware) ((IRecipeProcessingAwareHatch) hatch).startRecipeProcessing();
        boolean delivered = true;
        try {
            for (FluidStack stored : storedFluids(hatch)) {
                if (stored == null || stored.amount <= 0 || !EyeRecipeRegistry.isInputFluid(stored.getFluid())) {
                    continue;
                }
                final Long cap = caps.get(stored.getFluid());
                // never more than the cap minus what is buffered (a buffer above its cap is only drawn down)
                final long room = cap == null ? 0
                    : Math.min(BUFFER_CAP, cap) - buffer(stored.getFluid()) - got.getOrDefault(stored.getFluid(), 0L);
                if (room <= 0) continue;
                final FluidStack want = stored.copy();
                want.amount = (int) Math.min(stored.amount, room);
                final FluidStack d = hatch.drain(ForgeDirection.UNKNOWN, want, true);
                if (d != null && d.amount > 0) got.merge(d.getFluid(), (long) d.amount, Long::sum);
            }
        } finally {
            if (aware) delivered = closeWindow((IRecipeProcessingAwareHatch) hatch);
        }
        if (!delivered) return; // the hatch could not deliver what its window showed: nothing is credited
        for (Map.Entry<Fluid, Long> e : got.entrySet()) addBuffer(e.getKey(), e.getValue());
    }

    /**
     * QA-A01: an ME stocking input hatch is drained OUTSIDE a recipe-processing window. GT's own path then extracts
     * from the network at once, paying AE2's power (1 AE per 1,000 L), and returns exactly what came; that, and only
     * that, is credited (inside a window the hatch shows a free simulation, and a short real extraction afterwards
     * left the difference duplicated in the buffer and switched the Eye off). Each pull is also kept small enough for
     * a normal network: at most a twentieth of the cap a second ({@link #STOCKING_FILL_SECONDS}), and never more
     * than half the AE the network holds, so filling the Eye never browns out the network it shares.
     */
    private void drainStocking(MTEHatchInputME hatch, Map<Fluid, Long> caps) {
        long budget = stockingPowerBudget(hatch);
        // outside a window the hatch lists its configured fluids (1 L each), not their stock
        for (FluidStack config : hatch.getStoredFluids()) {
            if (budget <= 0) return;
            if (config == null || !EyeRecipeRegistry.isInputFluid(config.getFluid())) continue;
            final Long cap = caps.get(config.getFluid());
            if (cap == null) continue;
            final long capped = Math.min(BUFFER_CAP, cap);
            final long room = capped - buffer(config.getFluid());
            if (room <= 0) continue;
            final long want = Math
                .min(Math.min(room, Math.max(1, capped / STOCKING_FILL_SECONDS)), Math.min(budget, Integer.MAX_VALUE));
            final FluidStack got = hatch
                .drain(ForgeDirection.UNKNOWN, new FluidStack(config.getFluid(), (int) want), true);
            if (got == null || got.amount <= 0) continue;
            addBuffer(got.getFluid(), got.amount);
            budget -= got.amount + 1000L; // AE2 rounds each extraction's power up to the next 1,000 L
        }
    }

    /** QA-A01: an ME stocking hatch pulls at most its cap / this a second (a full fill takes 20 s). */
    public static final long STOCKING_FILL_SECONDS = 20;

    /** The litres half the AE stored on the hatch's network pays for (AE2: 1 AE per 1,000 L); 0 without a grid. */
    private static long stockingPowerBudget(MTEHatchInputME hatch) {
        try {
            final double stored = hatch.getProxy()
                .getEnergy()
                .extractAEPower(1e15, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            final double litres = stored / 2 * 1000;
            return litres >= Long.MAX_VALUE / 4 ? Long.MAX_VALUE / 4 : (long) litres;
        } catch (GridAccessException e) {
            return 0;
        }
    }

    /**
     * QA-A01: depth of the Eye's own recipe-processing windows on its hatches and buses. GT's stocking hatches call
     * {@code controller.stopMachine(CRITICAL_NONE)} when the real extraction at the end of a window comes up short or
     * the grid is gone; inside one of the Eye's windows no GT recipe runs, so that stop is ignored
     * ({@link #stopMachine})
     * and the window's caller credits nothing it did not get.
     */
    private int windowDepth;

    /** Closes the Eye's window on {@code hatch}: true when its real extraction delivered what the window took. */
    private boolean closeWindow(IRecipeProcessingAwareHatch hatch) {
        windowDepth++;
        try {
            final CheckRecipeResult r = hatch.endRecipeProcessing(this);
            return r == null || r.wasSuccessful();
        } finally {
            windowDepth--;
        }
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        // QA-A01: a stocking hatch's or bus's short extraction inside the Eye's own window (the window's caller has
        // already refused to credit it): no GT run to stop, and switching the Eye off would put its star out
        if (windowDepth > 0) return;
        super.stopMachine(reason);
        // S-05: GT switches a multiblock off when its structure breaks or every maintenance issue is open; the Eye
        // switches itself back on once that is fixed (the star is out by R-8, and relights by itself)
        final String id = reason.getID();
        if (ShutDownReasonRegistry.STRUCTURE_INCOMPLETE.getID()
            .equals(id)
            || ShutDownReasonRegistry.NO_REPAIR.getID()
                .equals(id)) {
            autoOff = id;
        }
    }

    /** S-05: why GT switched the Eye off itself (a shutdown reason id), "" when not. Tile NBT. */
    private String autoOff = "";

    /** S-05: every tick, after GT's own: the Eye GT switched off goes back on once formed and maintained again. */
    private void resumeIfRepaired(IGregTechTileEntity base) {
        if (autoOff.isEmpty()) return;
        if (base.isAllowedToWork()) {
            autoOff = ""; // switched on by hand meanwhile
            return;
        }
        if (!mMachine) return;
        final boolean maintenance = ShutDownReasonRegistry.NO_REPAIR.getID()
            .equals(autoOff);
        if (maintenance && getRepairStatus() <= 0) return;
        autoOff = "";
        base.enableWorking();
        runtime.noteEvent(
            maintenance ? "switched back on: the maintenance is fixed"
                : "switched back on: the structure is whole again");
    }

    /** S-05: why the Eye is switched off ("" while it may work). GUI (server side). */
    public String offReasonForGui() {
        return offReason();
    }

    /** S-05: why the Eye is switched off, for the GUI's and the scanner's wait line ("" while it may work). */
    private String offReason() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.isAllowedToWork()) return "";
        if (ShutDownReasonRegistry.STRUCTURE_INCOMPLETE.getID()
            .equals(autoOff)) {
            return "GT switched the Eye off when its structure broke: it switches back on once the structure is whole";
        }
        if (ShutDownReasonRegistry.NO_REPAIR.getID()
            .equals(autoOff)) {
            return "GT switched the Eye off: every maintenance issue is open; it switches back on once they are fixed";
        }
        return "the Eye is switched off (soft mallet, a cover or the GUI's power button): switch it back on";
    }

    private static FluidStack[] storedFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchInputME) return ((MTEHatchInputME) hatch).getStoredFluids();
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    public long buffer(Fluid fluid) {
        if (fluid == null) return 0;
        final Long v = buffers.get(fluid.getName());
        return v == null ? 0 : v;
    }

    /** A copy of the buffers, fluid registry name → litres (OpenComputers, bench). Server state. */
    public Map<String, Long> bufferSnapshot() {
        return new LinkedHashMap<>(buffers);
    }

    private void addBuffer(Fluid fluid, long amount) {
        final long now = buffer(fluid);
        buffers.put(fluid.getName(), Math.min(BUFFER_CAP, now + amount));
    }

    private void takeBuffer(Fluid fluid, long amount) {
        final long left = buffer(fluid) - amount;
        if (left > 0) {
            buffers.put(fluid.getName(), left);
        } else {
            buffers.remove(fluid.getName());
        }
    }

    /** Debug/creative: fill every Eye input buffer to {@code litres}. */
    public void debugFillBuffers(long litres) {
        for (Fluid fluid : EyeRecipeRegistry.inputFluids()) {
            buffers.put(fluid.getName(), Math.min(BUFFER_CAP, Math.max(buffer(fluid), litres)));
        }
    }

    /** Bench: sets one buffer to exactly {@code litres} (0 empties it). */
    public void debugSetBuffer(Fluid fluid, long litres) {
        if (fluid == null) return;
        if (litres > 0) buffers.put(fluid.getName(), Math.min(BUFFER_CAP, litres));
        else buffers.remove(fluid.getName());
    }

    /**
     * Bench {@code legacy}: loads 0.24.x-shaped NBT (no schema; optionally the Galactic flag and a star
     * run in flight) and re-walks, exactly what a pre-D-49 save does on its first load (plan §13).
     */
    public void debugLegacyLoad(boolean galactic, String runKey, long successes) {
        final NBTTagCompound nbt = new NBTTagCompound();
        // a tile save always carries GT's mID (EyeNbt.isLegacy tells a real 0.24.x save from an anvil name by it)
        nbt.setInteger("mID", 1);
        if (galactic) nbt.setBoolean(EyeNbt.GALACTIC_KEY, true);
        if (!buffers.isEmpty()) {
            final NBTTagCompound b = new NBTTagCompound();
            for (Map.Entry<String, Long> e : buffers.entrySet()) b.setLong(e.getKey(), e.getValue());
            nbt.setTag("btsEyeBuffers", b);
        }
        if (runKey != null) {
            nbt.setString("btsEyeRunKey", runKey);
            nbt.setLong("btsEyeRunParallel", successes);
            nbt.setLong("btsEyeRunSuccesses", successes);
            nbt.setDouble("btsEyeRunYield", 1.0);
            nbt.setInteger("mMaxProgresstime", 24000);
            nbt.setInteger("mProgresstime", 100);
        }
        loadNBTData(nbt);
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) checkStructure(true, base);
    }

    // ============================================================================ processing

    @Override
    public RecipeMap<?> getRecipeMap() {
        return DISPLAY_MAP;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.singletonList(DISPLAY_MAP);
    }

    private UUID owner() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final UUID uuid = base == null ? null : base.getOwnerUuid();
        if (uuid != null) WirelessNetworkManager.strongCheckOrAddUser(uuid);
        return uuid;
    }

    /**
     * Looks up the target, checks the tier and the fields, sizes the parallel to the buffers, debits
     * the run's EU from the owner's wireless network, takes the inputs and rolls success. The
     * outputs land in {@link #outputAfterRecipe()}.
     */
    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        final ItemStack target = getControllerSlot();
        if (target == null) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.notarget");
        final StarDef star = StarDef.byChart(target);
        if (star != null) return starStatus(star);
        final EyeRecipe recipe = EyeRecipeRegistry.lookup(target);
        if (recipe == null) return CheckRecipeResultRegistry.NO_RECIPE;
        final CheckRecipeResult gate = tierGate(recipe.tier);
        if (gate != null) return gate;
        // One live star per Eye: selecting a planet puts the star out (plan §6.7).
        runtime.extinguish("a planet was selected");
        long parallel = 1L << (installedTier() - recipe.tier);
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            parallel = Math.min(parallel, in.amount <= 0 ? parallel : buffer(in.fluid) / in.amount);
        }
        if (parallel < 1) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.inputs");
        final UUID owner = owner();
        if (owner == null) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.noowner");
        // A network that cannot pay for every parallel runs fewer, never none (the buffers only grow).
        parallel = Math.min(parallel, affordable(owner, recipe));
        if (parallel < 1) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.noeu");
        final BigInteger cost = BigInteger.valueOf(recipe.euPerRun)
            .multiply(BigInteger.valueOf(parallel));
        if (!WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost.negate())) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.noeu");
        }
        for (EyeRecipe.FluidAmount in : recipe.inputs) {
            takeBuffer(in.fluid, in.amount * parallel);
        }

        final double chance = successChance(recipe);
        long successes = 0;
        for (long i = 0; i < parallel; i++) {
            if (RANDOM.nextDouble() < chance) successes++;
        }
        runningKey = recipe.key;
        runningParallel = parallel;
        runningSuccesses = successes;
        runningYield = yieldOf(recipe);
        mOutputItems = null;
        mOutputFluids = null;
        lEUt = 0;
        mEfficiency = 10_000;
        mEfficiencyIncrease = 10_000;
        mMaxProgresstime = (int) Math.min(Integer.MAX_VALUE, runTicks(recipe));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /**
     * Bench {@code check}: the result id and text GT's run check would show now, with no side effect (a planet that
     * would run is not started).
     */
    public String benchCheckRecipe() {
        final ItemStack target = getControllerSlot();
        if (target == null) return "beyondthestars.eye.notarget";
        final StarDef star = StarDef.byChart(target);
        CheckRecipeResult r;
        if (star != null) {
            r = starStatus(star);
        } else {
            final EyeRecipe recipe = EyeRecipeRegistry.lookup(target);
            r = recipe == null ? CheckRecipeResultRegistry.NO_RECIPE : tierGate(recipe.tier);
            if (r == null) return "the planet would run";
        }
        return r.getID() + " -> " + r.getDisplayString();
    }

    /** Null when Eye tier {@code tier} may run; else why not (bands, unlock, compression). */
    private CheckRecipeResult tierGate(int tier) {
        if (tier > structureTier()) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.tier" + tier);
        if (tier > runtime.unlockedTier()) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.locked" + tier);
        }
        if (!compressionAllows(tier)) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.compression" + tier);
        }
        return null;
    }

    /**
     * D-49: a star chart never starts a GT run (the live star runs from {@link #onPostTick}, and GT's
     * run loop would fire {@link #outputAfterRecipe}); the controller GUI shows the star's state.
     */
    private CheckRecipeResult starStatus(StarDef star) {
        if (runtime.transition() != null) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_transition");
        }
        // The chart of the tier above the unlocked one is how that tier is unlocked (PENDING, the First
        // Cooling, the Magnetar Merger), so its "locked" gate does not hide the star's own status.
        final boolean towardNext = star.n() == runtime.unlockedTier() + 1;
        final CheckRecipeResult gate = tierGate(star.n());
        if (gate != null && !towardNext) return gate;
        switch (runtime.state()) {
            case IGNITING:
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_igniting");
            case PENDING:
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_pending");
            case LIT:
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_lit");
            case BATCH:
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_batch");
            case IDLE:
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_idle");
            default:
                if (star.n() >= 7) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_galaxy");
                if (star.n() == 6) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_merger");
                if (star.n() == 5) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_idle");
                if (!star.tier.ignite) return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_remnant");
                // the next tier's chart on a dark Eye: the star below relights to run the transition (QA-A02)
                if (gate != null && !towardNext) return gate;
                return SimpleCheckRecipeResult.ofFailure("beyondthestars.eye.star_dark");
        }
    }

    /** How many runs of {@code recipe} the owner's wireless network can pay for right now. */
    private static long affordable(UUID owner, EyeRecipe recipe) {
        if (recipe.euPerRun <= 0) return Long.MAX_VALUE;
        final BigInteger runs = WirelessNetworkManager.getUserEU(owner)
            .divide(BigInteger.valueOf(recipe.euPerRun));
        return runs.signum() <= 0 ? 0
            : runs.min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValue();
    }

    /**
     * Planets: TecTech's base success (D-49 keeps planets as built; the field generators act on the
     * stars through the time dial and q, plan §3.6).
     */
    public double successChance(EyeRecipe recipe) {
        if (!recipe.isPlanet()) return 1.0;
        return Math.max(0.0, Math.min(1.0, recipe.baseSuccess));
    }

    /** Planets: full yield (as built). */
    public double yieldOf(EyeRecipe recipe) {
        return 1.0;
    }

    /**
     * Stars: the base time; planets: -3 % per compression tier above the planet's TecTech requirement
     * (TecTech's reward). Below the requirement there is no penalty: the tier gate alone decides
     * whether a planet runs (round-4 ruling). Time Dilation acts on stars only (the dial, D-49).
     */
    public long runTicks(EyeRecipe recipe) {
        if (!recipe.isPlanet()) return Math.max(1, recipe.baseTicks);
        final int above = Math.max(0, Math.max(0, fieldMin[FIELD_COMPRESSION]) - recipe.compressionRequired);
        final double t = recipe.baseTicks * Math.pow(COMPRESSION_TIME_DISCOUNT, above);
        return Math.max(1L, Math.round(t));
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        final EyeRecipe recipe = EyeRecipeRegistry.byKey(runningKey);
        if (recipe == null) {
            if (runningKey != null) lastRun = runningKey + ": recipe no longer registered, outputs lost";
        } else if (runningSuccesses <= 0) {
            lastRun = recipe.name + ": FAILED (0/" + runningParallel + " succeeded)";
        } else {
            final double mult = runningSuccesses * runningYield;
            final ItemEjectionHelper items = new ItemEjectionHelper(this);
            for (EyeRecipe.ItemAmount out : recipe.itemOutputs) {
                ejectItem(items, out.stack, (long) Math.floor(out.amount * mult));
            }
            items.commit();
            for (EyeRecipe.FluidAmount out : recipe.fluidOutputs) {
                dumpFluid(out.fluid, (long) Math.floor(out.amount * mult));
            }
            lastRun = recipe.name + ": " + runningSuccesses + "/" + runningParallel + " succeeded";
            // D-49 T4 -> T5 milestone: a Deep Dark run completed on this Eye.
            if ("planet:DD".equals(recipe.key)) runtime.milestones().deepDark++;
        }
        runningKey = null;
        runningParallel = 0;
        runningSuccesses = 0;
        runningYield = 1.0;
    }

    private static void ejectItem(ItemEjectionHelper helper, ItemStack stack, long amount) {
        for (long done = 0; done < amount; done += Integer.MAX_VALUE) {
            final int n = (int) Math.min(Integer.MAX_VALUE, amount - done);
            helper.ejectStack(GTUtility.copyAmountUnsafe(n, stack));
        }
    }

    private void dumpFluid(Fluid fluid, long amount) {
        long left = amount;
        while (left > 0) {
            final int n = (int) Math.min(Integer.MAX_VALUE, left);
            // RC-2 dropped the static dumpFluid(List, FluidStack, boolean); addOutputPartial goes through
            // GT's FluidEjectionHelper (locked / ME hatches, void protection). protectFluids=false keeps
            // the old "store what fits, void the rest" behaviour.
            addOutputPartial(new FluidStack(fluid, n), mOutputHatches, false);
            left -= n;
        }
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    // ===================================================================================== NBT

    /**
     * The ONE serializer of the persisted state (trap 2): the D-49 schema, unlocked tier, milestones,
     * escrow and setpoints ({@link EyeNbt#writePersistent}) and the buffers. The live star is tile-only
     * ({@link #saveNBTData}): a wrenched controller comes back DARK, its core still in escrow.
     */
    private void writeEyeState(NBTTagCompound nbt) {
        EyeNbt.writePersistent(runtime, nbt);
        centre.writeSetting(nbt);
        if (!buffers.isEmpty()) {
            final NBTTagCompound b = new NBTTagCompound();
            for (Map.Entry<String, Long> e : buffers.entrySet()) {
                b.setLong(e.getKey(), e.getValue());
            }
            nbt.setTag("btsEyeBuffers", b);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeEyeState(aNBT);
        EyeNbt.writeStar(runtime, aNBT);
        centre.writeTile(aNBT);
        if (!autoOff.isEmpty()) aNBT.setString("btsEyeAutoOff", autoOff);
        if (footprintBands > 0) aNBT.setInteger("btsEyeFootprint", footprintBands);
        if (runningKey != null) {
            aNBT.setString("btsEyeRunKey", runningKey);
            aNBT.setLong("btsEyeRunParallel", runningParallel);
            aNBT.setLong("btsEyeRunSuccesses", runningSuccesses);
            aNBT.setDouble("btsEyeRunYield", runningYield);
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeEyeState(aNBT);
        final String fail = runtime.pickupFailureLine();
        if (!fail.isEmpty()) aNBT.setString(EyeNbt.PICKUP_FAIL_KEY, fail);
        // a live star put out by the pick-up is a failure: logged like every other one (B-62 item 3, B-72)
        if (runtime.live()) BeyondTheStars.LOG.info("[BTS] Eye {}", fail);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        runtime.reset();
        final boolean legacy = EyeNbt.read(runtime, aNBT);
        centre.read(aNBT);
        buffers.clear();
        if (aNBT.hasKey("btsEyeBuffers")) {
            final NBTTagCompound b = aNBT.getCompoundTag("btsEyeBuffers");
            for (Object key : b.func_150296_c()) {
                final String name = (String) key;
                if (FluidRegistry.getFluid(name) != null && b.getLong(name) > 0) {
                    buffers.put(name, Math.min(BUFFER_CAP, b.getLong(name)));
                }
            }
        }
        autoOff = aNBT.getString("btsEyeAutoOff");
        footprintBands = aNBT.getInteger("btsEyeFootprint");
        runningKey = aNBT.hasKey("btsEyeRunKey") ? aNBT.getString("btsEyeRunKey") : null;
        runningParallel = aNBT.getLong("btsEyeRunParallel");
        runningSuccesses = aNBT.getLong("btsEyeRunSuccesses");
        runningYield = aNBT.hasKey("btsEyeRunYield") ? aNBT.getDouble("btsEyeRunYield") : 1.0;
        // D-49 migration (plan §13 item 3): a pre-D-49 star run in flight was paid for at its start, so its
        // full output is credited once the Eye stands; GT must not finish it (stars are no GT runs now).
        if (runningKey != null && runningKey.startsWith("star:")) {
            EyeNbt.owe(runtime, runningKey, runningSuccesses, runningYield);
            runningKey = null;
            runningParallel = 0;
            runningSuccesses = 0;
            runningYield = 1.0;
            mMaxProgresstime = 0;
            mProgresstime = 0;
        }
        if (legacy) {
            BeyondTheStars.LOG.info(
                "[BTS] Eye of Harmony: migrated pre-D-49 NBT (unlocked T{}{}).",
                runtime.unlockedTier(),
                runtime.legacyPending() ? ", bands grandfathered at the first walk" : ", Galactic core");
        }
    }

    /**
     * Bench {@code wrench}: what picking the controller up and placing it back does to its state
     * (item NBT only: unlocked tier, milestones, escrow, setpoints, buffers; the star is lost).
     */
    public void debugWrench() {
        final NBTTagCompound item = new NBTTagCompound();
        setItemNBT(item);
        loadNBTData(item);
    }

    /** Bench {@code target}: puts a chart (or null) in the controller slot. */
    public void debugSetTarget(ItemStack chart) {
        mInventory[getControllerSlotIndex()] = chart;
        markDirty();
    }

    // ============================================================================ the star's host

    /** Lowest tier (1..9) of a field-generator type, 1 if none was walked. */
    private int generatorTier(int type) {
        return fieldMin[type] < 0 ? 1 : fieldMin[type] + 1;
    }

    /** The world the D-49 star runtime sees (plan §4: the controller around step). Server side. */
    private final class StarHost implements EyeStarRuntime.Host {

        @Override
        public boolean structureChecked() {
            return mStartUpCheck < 0;
        }

        @Override
        public boolean formed() {
            return mMachine;
        }

        @Override
        public boolean allowedToWork() {
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            return base != null && base.isAllowedToWork();
        }

        @Override
        public String offReason() {
            return MTEEyeOfHarmonyBTS.this.offReason();
        }

        @Override
        public int installedTier() {
            return MTEEyeOfHarmonyBTS.this.installedTier();
        }

        @Override
        public int structureTier() {
            return MTEEyeOfHarmonyBTS.this.structureTier();
        }

        @Override
        public int compressionTier() {
            return MTEEyeOfHarmonyBTS.this.compressionTier();
        }

        @Override
        public int timeTier() {
            return generatorTier(FIELD_DILATION);
        }

        @Override
        public int stabilisationTier() {
            return generatorTier(FIELD_STABILISATION);
        }

        @Override
        public ItemStack target() {
            return getControllerSlot();
        }

        @Override
        public long buffer(Fluid fluid) {
            return MTEEyeOfHarmonyBTS.this.buffer(fluid);
        }

        @Override
        public void takeBuffer(Fluid fluid, long litres) {
            MTEEyeOfHarmonyBTS.this.takeBuffer(fluid, litres);
        }

        /** Trims and Time come from the live Field sockets only (never buffered; ME stocking hatches rejected). */
        @Override
        public long drainField(Fluid fluid, long max) {
            if (fluid == null || max <= 0) return 0;
            long got = 0;
            for (MTEHatchInput hatch : fieldHatches) {
                if (got >= max) break;
                if (!live(hatch)) continue;
                for (FluidStack stored : storedFluids(hatch)) {
                    if (got >= max) break;
                    if (stored == null || stored.amount <= 0 || stored.getFluid() != fluid) continue;
                    final int want = (int) Math.min(stored.amount, max - got);
                    final FluidStack drained = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(fluid, want), true);
                    if (drained != null && drained.amount > 0) got += drained.amount;
                }
            }
            return got;
        }

        /**
         * P4-77: an OpenComputers trim pulse's fluid, from the gas and stellar sockets (ME stocking included). An ME
         * stocking hatch is drained outside a window (its real, powered extraction, QA-A01); any other
         * processing-aware hatch inside one, counted only when the window closes cleanly.
         */
        @Override
        public long drainStock(Fluid fluid, long max) {
            if (fluid == null || max <= 0) return 0;
            long got = 0;
            for (List<MTEHatchInput> list : java.util.Arrays.asList(gasHatches, stellarHatches)) {
                for (MTEHatchInput hatch : list) {
                    if (got >= max) return got;
                    if (!live(hatch)) continue;
                    if (hatch instanceof MTEHatchInputME) {
                        final FluidStack d = hatch.drain(
                            ForgeDirection.UNKNOWN,
                            new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, max - got)),
                            true);
                        if (d != null && d.amount > 0) got += d.amount;
                        continue;
                    }
                    final boolean aware = hatch instanceof IRecipeProcessingAwareHatch;
                    if (aware) ((IRecipeProcessingAwareHatch) hatch).startRecipeProcessing();
                    long fromHatch = 0;
                    boolean delivered = true;
                    try {
                        for (FluidStack stored : storedFluids(hatch)) {
                            if (got + fromHatch >= max) break;
                            if (stored == null || stored.amount <= 0 || stored.getFluid() != fluid) continue;
                            final int want = (int) Math.min(stored.amount, max - got - fromHatch);
                            final FluidStack drained = hatch
                                .drain(ForgeDirection.UNKNOWN, new FluidStack(fluid, want), true);
                            if (drained != null && drained.amount > 0) fromHatch += drained.amount;
                        }
                    } finally {
                        if (aware) delivered = closeWindow((IRecipeProcessingAwareHatch) hatch);
                    }
                    if (delivered) got += fromHatch;
                }
            }
            return got;
        }

        /**
         * R-1: a Stellar Ignition Charge from a plinth input bus (ME input buses through their shadow stacks, read and
         * taken inside one window; a stocked charge counts only if the network really gave it up). {@code simulate}
         * opens no window: it is the read-only count (the GUI, the Directive Status bit 2 and OpenComputers ask it).
         */
        @Override
        public boolean takeIgnitionCharge(boolean simulate) {
            final Item charge = SLItems.stellarIgnitionCharge;
            if (charge == null) return false;
            if (simulate) return countIgnitionCharges() > 0;
            for (MTEHatchInputBus bus : itemBuses) {
                if (!live(bus)) continue;
                if (takeFromBus(bus, st -> st.getItem() == charge) != null) return true;
            }
            return false;
        }

        /**
         * GUI: the charges across the live plinth input buses. READ ONLY: no recipe-processing window is opened,
         * because GT's {@code MTEHatchInputBusME.endRecipeProcessing(controller)} stops the controller (a critical
         * shutdown) when the bus cannot reach its grid, and a GUI must never do that. Outside the window a stocking
         * bus shows only its circuit and manual slots through {@code getStackInSlot}, so its network stock is read
         * here from the grid's item list for each slot configured to the charge (once per distinct config; nothing
         * when the bus is disabled or its network is down, which is what a recipe check would see too).
         */
        @Override
        public int countIgnitionCharges() {
            final Item charge = SLItems.stellarIgnitionCharge;
            if (charge == null) return 0;
            long count = 0;
            for (MTEHatchInputBus bus : itemBuses) {
                if (!live(bus)) continue;
                for (int i = 0; i < bus.getSizeInventory(); i++) {
                    final ItemStack stack = bus.getStackInSlot(i);
                    if (stack != null && stack.getItem() == charge && stack.stackSize > 0) count += stack.stackSize;
                }
                if (bus instanceof MTEHatchInputBusME) count += stockedCharges((MTEHatchInputBusME) bus, charge);
            }
            return (int) Math.min(Integer.MAX_VALUE, count);
        }

        /** A stocking bus's network stock of the charge, read from the grid's item list (no extraction, no window). */
        private long stockedCharges(MTEHatchInputBusME bus, Item charge) {
            if (!bus.isAllowedToWork()) return 0;
            final List<ItemStack> seen = new ArrayList<>();
            long count = 0;
            try {
                final IItemList<IAEItemStack> list = bus.getProxy()
                    .getStorage()
                    .getItemInventory()
                    .getStorageList();
                for (int i = 0; i < MTEHatchInputBusME.SLOT_COUNT; i++) {
                    final ItemStack config = bus.getSlotConfig(i);
                    if (config == null || config.getItem() != charge) continue;
                    boolean dup = false;
                    for (ItemStack s : seen) dup |= GTUtility.areStacksEqual(s, config);
                    if (dup) continue;
                    seen.add(config);
                    final IAEItemStack stored = list.findPrecise(AEItemStack.create(config));
                    if (stored != null && stored.getStackSize() > 0) count += stored.getStackSize();
                }
            } catch (GridAccessException e) {
                return 0;
            }
            return count;
        }

        @Override
        public UUID owner() {
            return MTEEyeOfHarmonyBTS.this.owner();
        }

        @Override
        public void output(Fluid fluid, long litres) {
            dumpFluid(fluid, litres);
        }

        /** The T6 Tengam return: through GT's item ejection (ME output buses; void protection as built). */
        @Override
        public void outputItems(ItemStack stack, long count) {
            if (stack == null || count <= 0) return;
            final ItemEjectionHelper items = new ItemEjectionHelper(MTEEyeOfHarmonyBTS.this);
            ejectItem(items, stack, count);
            items.commit();
        }

        @Override
        public void publish(FieldReadout readout) {
            for (MTEHatchStellarFieldSensor sensor : sensorHatches) {
                if (live(sensor)) sensor.publish(readout);
            }
        }

        @Override
        public void markDirty() {
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            if (base != null) base.markDirty();
        }

        /**
         * P4: the receptor groups through the host-side burst API (quasar-p4.md §3.1): each group read on its own
         * (rail-high, rail-low, halo), then every port through the Eye's {@link BurstHost} once, which rolls the 60 s
         * mean, works out σ against the demand and reports the Eye to every feeding quasar.
         */
        @Override
        public EyeBursts bursts(double demandBuPerTick, double streak01) {
            injectBench();
            final EyeBursts b = new EyeBursts();
            final BurstReading hi = BurstHost.read(receptors[GROUP_HIGH], "Eye of Harmony (rail-high)");
            final BurstReading lo = BurstHost.read(receptors[GROUP_LOW], "Eye of Harmony (rail-low)");
            final BurstReading halo = BurstHost.read(receptors[GROUP_HALO], "Eye of Harmony (halo)");
            final List<MTEHatchGRBReceptor> all = new ArrayList<>(receptors[GROUP_HIGH]);
            all.addAll(receptors[GROUP_LOW]);
            all.addAll(receptors[GROUP_HALO]);
            final BurstReading r = burstHost.second(all, eyeKey(), "Eye of Harmony", demandBuPerTick, streak01);
            b.hi = hi.total;
            b.lo = lo.total;
            b.halo = halo.total;
            b.linkedRail = hi.linked() || lo.linked();
            b.linkedAll = r.linked();
            b.paused = r.paused;
            b.pausedBy = r.pausedBy == null ? "" : r.pausedBy.label();
            b.sourceId = r.linkSource();
            b.source = b.sourceId == null ? "" : b.sourceId.label();
            b.hiPorts = receptorCount(GROUP_HIGH);
            b.loPorts = receptorCount(GROUP_LOW);
            b.haloPorts = receptorCount(GROUP_HALO);
            b.mean60 = burstHost.mean();
            b.sigmaMean = burstHost.sigmaMean();
            b.limit = r.limitOf(b.sourceId);
            final double[] rec = r.recordOf(b.sourceId);
            b.smLattice = (int) rec[0];
            b.smMass = rec[1];
            return b;
        }

        @Override
        public int receptorPorts(int group) {
            return group >= 0 && group < 3 ? receptorCount(group) : 0;
        }

        /** Polls every Burst Control Hatch (Begin mode); true if any saw a rising edge since the last second. */
        @Override
        public boolean takeBeginEdge() {
            boolean edge = false;
            final String line = runtime.galaxyReady() ? "ready: an edge begins the transition"
                : runtime.galaxyTransition() ? "a transition is running" : "not ready (edges ignored)";
            for (MTEHatchBurstControl h : burstControls) {
                if (!live(h)) continue;
                h.poll("Eye of Harmony", line, MTEHatchBurstControl.Mode.BEGIN);
                if (h.consumeRisingEdge()) edge = true;
            }
            return edge;
        }

        /**
         * The slot bus's items in slot order (ME input buses: what they hold for the Eye now). Never consumed. The
         * list read in the last field second (QA-A10): the GUI, the scanner, OpenComputers and the Directive Hatches
         * read it every tick, and opening an ME bus's processing window for each of them could switch the Eye off.
         */
        @Override
        public List<ItemStack> slotBusItems() {
            return Collections.unmodifiableList(slotItems);
        }

        @Override
        public double planetSeconds(EyeRecipe recipe) {
            return runTicks(recipe) / 20.0;
        }
    }

    // ------------------------------------------------------------------ P4 bench: the real receptor path

    /** Bench {@code inject}: a source quasar and the Bu/t per group pushed into the ports each second (not saved). */
    private com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource injectFrom;
    private final double[] injectBu = new double[3];

    /**
     * Bench {@code inject <qx> <qy> <qz> <hi> <lo> <halo>}: from now on, every field second, streams from that quasar
     * (its real {@code BurstSource}: Q-8 follows its status) are pushed into this Eye's Receptor Ports, split evenly
     * across each group's ports, before the Eye reads them. {@code null} source stops it.
     */
    public void debugInject(com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource from, double hi, double lo,
        double halo) {
        injectFrom = from;
        injectBu[GROUP_HIGH] = hi;
        injectBu[GROUP_LOW] = lo;
        injectBu[GROUP_HALO] = halo;
    }

    private void injectBench() {
        if (injectFrom == null) return;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return;
        // a quasar that is frozen or not loaded sends nothing: its ports then remember it and pause the Eye (Q-8)
        if (injectFrom.status() != com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource.Status.RUNNING)
            return;
        final long now = base.getWorld()
            .getTotalWorldTime();
        // SMBH: the source record its real streams would carry (the lattice in force, the mass), for the Supermassive
        // Link's data
        final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = injectFrom.resolve();
        final int lattice = q == null ? 0 : q.getLatticeInForce();
        final double mass = q == null ? 0 : q.getMass();
        for (int g = 0; g < 3; g++) {
            final int n = receptorCount(g);
            if (n == 0 || injectBu[g] <= 0) continue;
            final com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream s = com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream
                .of(
                    new com.nateyburr.beyondthestars.machines.quasar.burst.JetPacket(
                        injectFrom,
                        com.nateyburr.beyondthestars.machines.quasar.burst.JetPort.TOP,
                        injectBu[g] / n,
                        true,
                        now,
                        lattice,
                        mass));
            for (MTEHatchGRBReceptor port : receptors[g]) if (live(port)) port.receive(s);
        }
    }

    /** "eye@dim:x,y,z": this Eye's key in its quasars' host lists. */
    private String eyeKey() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return "eye";
        return "eye@" + base.getWorld().provider.dimensionId
            + ":"
            + base.getXCoord()
            + ","
            + base.getYCoord()
            + ","
            + base.getZCoord();
    }

    // ============================================================================ panel/tooltip

    private static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static String fieldName(int meta) {
        return meta < 0 ? "none" : "tier " + (meta + 1);
    }

    /** The status lines the scanner and the creative build print. Server-side state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(
            "Eye T" + installedTier()
                + " (unlocked T"
                + runtime.unlockedTier()
                + ")"
                + ": structure T"
                + structureTier()
                + ", compression T"
                + compressionTier()
                + "; "
                + bandStates()
                + (footprintLoaded ? "" : "; FROZEN (a chunk of the footprint is not loaded)"));
        final String gap = bandGap();
        if (!gap.isEmpty()) out.add(gap);
        out.add(
            "Fields: " + FIELD_NAMES[0]
                + " "
                + fieldName(fieldMin[0])
                + ", "
                + FIELD_NAMES[1]
                + " "
                + fieldName(fieldMin[1])
                + ", "
                + FIELD_NAMES[2]
                + " "
                + fieldName(fieldMin[2])
                + " (dilation: the time dial's cap; stabilisation: q "
                + String.format(Locale.ROOT, "%.2f", 1 - 0.05 * (generatorTier(FIELD_STABILISATION) - 1))
                + ")");
        out.add(
            "Sockets: gas " + gasHatchCount()
                + ", input bus "
                + itemBusCount()
                + ", stellar "
                + stellarHatchCount()
                + ", Field "
                + fieldHatchCount()
                + (rejectedFieldHatchCount() > 0
                    ? " (+" + rejectedFieldHatchCount() + " ME stocking hatch REJECTED: never drained)"
                    : "")
                + ", Sensor "
                + sensorHatchCount()
                + ", Directive "
                + directiveHatchCount()
                + ", Burst Control "
                + burstControlCount()
                + ", slot bus "
                + slotBusCount()
                + ", GRB Receptor "
                + receptorCount(GROUP_HIGH)
                + " high / "
                + receptorCount(GROUP_LOW)
                + " low / "
                + receptorCount(GROUP_HALO)
                + " halo"
                + ", ME out "
                + outputBusCount()
                + " bus / "
                + outputHatchCount()
                + " hatch");
        // B-108: each buffer with the intake cap the sockets fill it to now (a fluid with no cap is not pulled)
        final List<IntakeRow> intake = intakeRows();
        if (intake.isEmpty()) {
            out.add("Buffers: empty (intake: nothing; set a target)");
        } else {
            final StringBuilder sb = new StringBuilder("Buffers (held / intake cap):");
            for (IntakeRow r : intake) {
                sb.append(' ')
                    .append(r.name)
                    .append(' ')
                    .append(fmt(r.held))
                    .append(" / ")
                    .append(r.cap < 0 ? "not pulled" : fmt(r.cap))
                    .append(" L;");
            }
            out.add(sb.toString());
        }
        final StarDef starTarget = StarDef.byChart(getControllerSlot());
        final EyeRecipe target = EyeRecipeRegistry.lookup(getControllerSlot());
        if (starTarget != null) {
            out.add(
                "Target: " + starTarget.name
                    + " (T"
                    + starTarget.n()
                    + " live star), parallel "
                    + runtime.parallel(starTarget.n())
                    + (compressionAllows(starTarget.n()) ? ""
                        : " (needs every Spacetime Compression generator at tier " + starTarget.n() + "+)"));
        } else if (target != null) {
            out.add(
                "Target: " + target.name
                    + " (T"
                    + target.tier
                    + "), parallel up to "
                    + (installedTier() >= target.tier ? (1L << (installedTier() - target.tier)) : 0)
                    + affordableNote(target)
                    + (compressionAllows(target.tier) ? ""
                        : " (needs every Spacetime Compression generator at tier " + target.tier + "+)")
                    + ", success "
                    + Math.round(successChance(target) * 100)
                    + "%, "
                    + fmt(runTicks(target) / 20)
                    + " s a run");
        } else {
            out.add("Target: none (put a star chart or a dimension display block in the controller slot)");
        }
        if (runningKey != null) {
            out.add("Running: " + runningKey + " x" + runningParallel + " (" + runningSuccesses + " will succeed)");
        }
        if (!lastRun.isEmpty()) out.add("Last run: " + lastRun);
        out.addAll(runtime.statusLines());
        for (MTEHatchDirective hatch : publishedDirectives()) {
            if (live(hatch)) out.add("Directive: " + hatch.statusLine());
        }
        for (MTEHatchBurstControl hatch : burstControls) {
            if (live(hatch)) out.add("Burst Control: " + hatch.statusLine());
        }
        if (!runtime.lastFailure()
            .isEmpty()) out.add("Last failure: " + runtime.lastFailure());
        out.add(
            "Centre render: "
                + (!Config.eyeRenderCentre ? "off (config eyeRenderCentre)"
                    : !centre.isEnabled() ? "off (this Eye; the GUI toggle)" : centre.label())
                + (centre.note()
                    .isEmpty() ? "" : "; " + centre.note()));
        return out;
    }

    /** ", N affordable" when the owner's network cannot pay for every parallel. Server state. */
    private String affordableNote(EyeRecipe target) {
        if (installedTier() < target.tier) return "";
        final UUID owner = owner();
        if (owner == null) return "";
        final long full = 1L << (installedTier() - target.tier);
        final long afford = affordable(owner, target);
        return afford >= full ? "" : " (" + afford + " affordable on the owner's network)";
    }

    private static String state(boolean built, String fault) {
        return built ? "FORMED" : fault.isEmpty() ? "absent" : "absent (" + fault + ")";
    }

    /** "beam band FORMED, jet crescent absent (...), ..." for every band. Server state. */
    private String bandStates() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i < PIECES.length; i++) {
            if (i > 1) sb.append(", ");
            sb.append(BAND_NAMES[i])
                .append(' ')
                .append(state(bandBuilt[i], bandFault[i]));
        }
        return sb.toString();
    }

    /**
     * D-49 cumulative bands: names the first band that stands above a missing one (e.g. an Eye built
     * before D-49 with the jet crescent but no beam band, which now runs as T1). "" if there is none.
     */
    private String bandGap() {
        if (!mMachine) return "";
        final int tier = structureTier();
        for (int i = tier; i < PIECES.length; i++) {
            if (bandBuilt[i]) {
                return "Bands count from the bottom up: the " + BAND_NAMES[i]
                    + " stands but does not count until the "
                    + BAND_NAMES[tier]
                    + " is built, so this Eye is structurally T"
                    + tier
                    + ".";
            }
        }
        return "";
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    /** Bench ({@code /btseye ... oldwindows}): the block each window char replaced, {@code {Block, meta}}. */
    public static Map<Character, Object[]> windowBefore() {
        final Map<Character, Object[]> m = new LinkedHashMap<>();
        m.put(
            'A',
            new Object[] { Casings.ReinforcedSpatialStructureCasing.getBlock(),
                Casings.ReinforcedSpatialStructureCasing.getBlockMeta() });
        return m;
    }

    /** {@link #bom} summed over every piece. */
    private static int bomAll(char c) {
        int n = 0;
        for (String piece : PIECES) {
            n += bom(piece, c);
        }
        return n;
    }

    /** The full T6 Eye's bounding box {w, h, d}, from the pieces' sizes and offsets. */
    private static int[] assembledSize() {
        final int[] lo = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        final int[] hi = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        try {
            for (String piece : PIECES) {
                final int[] o = BTSShapes.offset(piece);
                final int[] sz = BTSShapes.size(piece);
                for (int k = 0; k < 3; k++) {
                    lo[k] = Math.min(lo[k], -o[k]);
                    hi[k] = Math.max(hi[k], sz[k] - 1 - o[k]);
                }
            }
        } catch (RuntimeException missing) {
            return BTSShapes.size(PIECE_CORE);
        }
        return new int[] { hi[0] - lo[0] + 1, hi[1] - lo[1] + 1, hi[2] - lo[2] + 1 };
    }

    private static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] core = BTSShapes.size(PIECE_CORE);
        final int[] full = assembledSize();
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Stellar Forge, Eye of Harmony")
            .addInfo("Beyond the Stars: replaces TecTech's Eye of Harmony.")
            .addInfo("Put a Star Chart or a dimension display block in the controller slot (not consumed).")
            .addInfo("Bands are cumulative: each tier needs every band below it.")
            .addInfo("T1 core: Sol (Raw Star Matter) and planets R0-R3.")
            .addInfo("T2 + beam band: Sirius A (Excited Stellar Plasma) and planets R4-R6.")
            .addInfo("T3 + jet crescent: Arcturus (Raw Hyperdense Stellar Plasma) and planets R7-R8.")
            .addInfo("T4 + spiral arms: the neutron star (White Dwarf Matter, born by the Core Collapse), Deep Dark.")
            .addInfo("T5 + second arms: Sirius B (Black Dwarf Matter); T6 + galactic rim: the magnetar.")
            .addInfo("  The magnetar's Antipolarization Agent upkeep (3,000 L/cycle) also charges its flare shield;")
            .addInfo("  dose extra Agent into a Field socket on a flare warning.")
            .addInfo("T7 + stability rail: Triangulum (Ergomatter); T8 + shard halo: the Milky Way (Superdense")
            .addInfo("  Ergomatter from an Ergomatter feed). Galaxies live on a Quasar Engine's gamma-ray bursts on")
            .addInfo("  the GRB Receptor Ports: bring 1.5x the demand (1.5 / 6 Bu/t = 1.5e13 / 6.0e13 EU/t; 1 Bu/t")
            .addInfo("  = 10^13 EU/t), Space trims cool P, aim the")
            .addInfo("  rail's high / low banks for W, the halo holds Λ. Unlocks: the Stability Link (T6) and the")
            .addInfo("  Heavy Link (T7), 30 min of linked bursts; then the Magnetar Collapse and the Galactic Merger")
            .addInfo("  begin on a signal. Planets mode: 3 / 6 planets from the slot bus; companions: 1 / 2 charts.")
            .addInfo("Stars are LIVE: a Stellar Ignition Charge in a plinth input bus lights T1-T3; every")
            .addInfo("  60 s cycle takes EU and upkeep; hold the stability field or the star is lost.")
            .addInfo("  Once T4 is unlocked, a dark Eye with the Neutron Star chart relights Arcturus from a")
            .addInfo("  charge and re-runs the Core Collapse by itself (no core).")
            .addInfo("Tiers unlock in order: the current star's milestones, then the next core")
            .addInfo("  (right-click it into escrow), then its transition. Sneak-punch returns the core.")
            .addInfo("  A transition starts only when the buffers also hold the new star's first cycle.")
            .addInfo("The stability field: trim fluids into the Field sockets (plain input hatches, not")
            .addInfo("  buffered); Stellar Field Sensor Hatches read any gauge as redstone. Standard rig:")
            .addInfo("  sensor -> Fluid Regulator cover or export bus with a redstone card -> Field hatch.")
            .addInfo("The GUI shows every gauge, upkeep, the transition and the last failure, and sets the")
            .addInfo("  time dial (Time Dilation generators; costs Tachyon Rich Temporal Fluid) and the T4 feed.")
            .addInfo("Automation: every action has an AE2, redstone, OpenComputers and Factory Manager route.")
            .addInfo("  Directive Hatches (Sensor sockets) put out what the Eye needs as a 4-bit code: Trims,")
            .addInfo("  Status, Transition (tables on the hatch); a control-set hatch takes redstone for the dial.")
            .addInfo("  T7+: the 'd' sockets take Directive Hatches (Field, Events, Galaxy Transition, Slots) or a")
            .addInfo("  Burst Control Hatch (Begin: a rising edge begins a ready galaxy transition).")
            .addInfo("  A plinth input bus takes the next core into escrow and a star chart into the controller")
            .addInfo("  slot; the old target and spare copies leave through the ME output bus (export-bus card")
            .addInfo("  on Signal pulse; no swap during a transition). OpenComputers: Adapter -> bts_eye.")
            .addInfo("The star is drawn at the Eye's centre while it burns (GUI toggle, config eyeRenderCentre).")
            .addInfo("Planets give TecTech's ores and plasmas, without Raw Star Matter or dwarf matter.")
            .addSeparator()
            .addInfo("Power: wireless EU (planets: per run at start; stars: per cycle); none returned.")
            .addInfo("Parallel x2 for every installed tier above the recipe's tier (stars: from the next charge).")
            .addInfo("Field generators (any tier, mixable; the lowest tier of each type counts):")
            .addInfo("  Spacetime Compression GATES the tier: Eye TN (T1-T8) needs every one at tier N+.")
            .addInfo("  It alone decides the planets: a TN Eye runs every planet of T1..TN.")
            .addInfo("  Planets: -3% time per compression tier above the planet's TecTech tier.")
            .addInfo("  Time Dilation above the star's tier opens the time dial; Stabilisation calms the field.")
            .addInfo("  Tier step: upgrade each generator in place in the Condensate Assembler (the old one plus")
            .addInfo("  what the new tier adds; its Kerr controller stays inside). Stabilisation: + Retrofit Kits.")
            .addInfo("A short wireless network runs fewer parallels rather than none.")
            .addInfo("Inputs drain every second into internal buffers, only up to the target's intake cap:")
            .addInfo("  stars max(ignition, 20 cycles of upkeep) plus a transition the Eye could begin; planets")
            .addInfo("  2 runs at full parallel (T7/T8: each slot-bus planet; companions add 20 of their cycles).")
            .addInfo("  The rest stays in the hatch or ME network (scanner: held / cap),")
            .addInfo("  so one ME stocking hatch never drains the network. Outputs go to ME only.")
            .addSeparator()
            .addInfo("Chunk-load the full footprint: the structure check fails across unloaded chunks.")
            .beginVariableStructureBlock(core[0], full[0], core[1], full[1], core[2], full[2], false)
            .addController("Plinth, front centre")
            .addCasingInfoExactly("Reinforced Spatial Structure Casing", bom(PIECE_CORE, 'U'), false)
            .addCasingInfoExactly("Reinforced Temporal Structure Casing", bom(PIECE_CORE, 'Q'), false)
            .addCasingInfoExactly("Infinite SpaceTime Energy Boundary Casing", bomAll('J'), false)
            .addCasingInfoExactly("Quantum Glass", bomAll('Z') + bom(PIECE_CORE, 'A'), false)
            .addCasingInfoExactly(
                "Spacetime Compression Field Generator (tier = Eye tier or higher)",
                bom(PIECE_CORE, 'W'),
                true)
            .addCasingInfoExactly("Time Dilation Field Generator (any tier)", bom(PIECE_CORE, 'X'), true)
            .addCasingInfoExactly("Stabilisation Field Generator (any tier)", bom(PIECE_CORE, 'Y'), true)
            .addOtherStructurePart("T2 beam band", bom(PIECE_BEAM, 'R') + " Beam Band Casing")
            .addOtherStructurePart("T3 jet crescent", bom(PIECE_JET, 'S') + " Jet Crescent Casing")
            .addOtherStructurePart("T4 spiral arms", bom(PIECE_ARMS, 'T') + " Galactic Shell Plating")
            .addOtherStructurePart("T5 second arms", bom(PIECE_ARMS2, 'T') + " Galactic Shell Plating")
            .addOtherStructurePart(
                "T6 galactic rim",
                (bom(PIECE_RIM, 'T') + bom(PIECE_RIM, 'g')) + " Galactic Shell Plating (4 may be sockets)")
            .addOtherStructurePart(
                "T7 stability rail",
                (bom(PIECE_RAIL, 'B') + bom(PIECE_RAIL, 'r')) + " Bastion Plate, "
                    + bom(PIECE_RAIL, 'P')
                    + " Casimir Plate, lining and glass")
            .addOtherStructurePart(
                "T8 shard halo",
                (bom(PIECE_HALO, 'B') + bom(PIECE_HALO, 'r')) + " Bastion Plate and glass")
            .addOtherStructurePart(
                "GRB Receptor Port",
                "rail (4 high, 4 low) and halo (8); T7 needs 1+ per bank, T8 all 8 halo",
                9)
            .addOtherStructurePart("Directive Hatch or Burst Control Hatch", "plinth, 4 'd' sockets, optional", 7)
            .addOtherStructurePart("Input bus (the slot bus: planets, companion charts)", "plinth, optional", 8)
            .addOtherStructurePart("Gas input hatch (H, He), or input bus (ignition charges)", "plinth, 1+ hatch", 1)
            .addOtherStructurePart("ME Output Bus + ME Output Hatch", "plinth, at least 1 of each", 2)
            .addMaintenanceHatch("Plinth", 3)
            .addOtherStructurePart("Stellar input hatch", "beam band, jet crescent and rim sockets", 4)
            .addOtherStructurePart(
                "Field input hatch (stability trims, not buffered; no ME stocking)",
                "core band, " + bom(PIECE_CORE, 'f') + " sockets, optional",
                5)
            .addOtherStructurePart(
                "Stellar Field Sensor Hatch or Directive Hatch",
                "core band, " + bom(PIECE_CORE, 'n') + " sockets, optional",
                6)
            .addStructureInfo("Hologram: trigger stack 1 = T1 core, 2 = + beam band, 3 = + jet crescent, 4 = + arms,")
            .addStructureInfo("  5 = + second arms, 6 = + galactic rim, 7 = + stability rail, 8 = + shard halo.")
            .addStructureInfo("Compression generators are placed at the stack's tier (its 1:1 gate); Time")
            .addStructureInfo("  Dilation and Stabilisation at tier 1. The projector's EOH channels")
            .addStructureInfo("  (spacetime_compression, time_dilation, stabilisation) raise them.")
            .addStructureInfo("Builds never overwrite hatches, nor downgrade a field generator already placed.")
            .addStructureInfo("Window band at the star's height: a Quantum Glass pane in the core band's front, the")
            .addStructureInfo(
                "  cells behind it and the arm / rim cells in front of it open. Old Eyes keep their blocks.")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ReinforcedSpatialStructureCasing.getCasingTexture();
        if (side != aFacing) return new ITexture[] { base };
        return new ITexture[] { base, TextureFactory.builder()
            .addIcon(aActive ? OVERLAY_ACTIVE : OVERLAY)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(aActive ? OVERLAY_ACTIVE_GLOW : OVERLAY_GLOW)
                .extFacing()
                .glow()
                .build() };
    }
}
