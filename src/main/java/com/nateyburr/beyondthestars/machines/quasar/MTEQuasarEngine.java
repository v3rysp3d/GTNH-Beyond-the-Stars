package com.nateyburr.beyondthestars.machines.quasar;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing4;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.QuietTileUpdate;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.directive.DirectiveHost;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.directive.QuasarDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstDocks;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;
import com.nateyburr.beyondthestars.machines.quasar.burst.IBurstDock;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPacket;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.HorizonLattice;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarDisc;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarInputs;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;
import com.nateyburr.beyondthestars.machines.quasar.render.BlockQuasarHole;
import com.nateyburr.beyondthestars.machines.quasar.render.TileQuasarHole;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * P4: THE QUASAR ENGINE, MTE base+20 = 28120, UXV (quasar-p4.md §1, §2, §7, §8; D-41, D-44, D-50).
 * <p>
 * Its own machine with its own hole, seeded ONCE from a Quasar Seed (made on a Kerr T2's DENSITY map; it costs the
 * KEE's hole nothing, Q-5) and never linked to a KEE at runtime. It runs no recipes. Gas fed through the 24 Rim Gas
 * Injectors builds the <b>accretion disc</b>, a two-variable field: E, the Eddington balance (a gauge in D-49's zones),
 * and Φ, the horizon flux (0-100, charges and erupts). The disc spins and grows the hole and drives two jets of
 * gamma-ray bursts (Bu/t; a crown turns 1 Bu/t into 10¹³ EU/t), half on each jet port. Every formula is the pure
 * engine's
 * ({@link QuasarDisc#step}, bit-identical to the prototype); this class only feeds it and hands out what it made.
 *
 * <h2>The second (every 20 ticks, while formed and every structure chunk is loaded)</h2>
 * <ol>
 * <li>Refill the Accretion Buffer (the selected gas only) from the {@code h} hatches; draw f·Ṁ_Edd from it. The buffer
 * holds {@link QuasarParams#ACCRETION_SECONDS} s of Disc + Hold-up (and one second of Growth room); after the second
 * it keeps refilling every tick while the hatches give (each drain at most 2³¹ − 1 L), so an ME stocking hatch is
 * read up to 20 times a second (smbh-v1.md §4.2).</li>
 * <li>Coil power 2 A UXV × b² is drained every tick (b = min(setting, coil tier)); a short tick makes the next second's
 * b 0 (no jets, Φ frozen; the disc itself is unaffected).</li>
 * <li>Burst Control Hatches: a PURGE-mode rising edge requests a purge; a FIELD-mode hatch sets b.</li>
 * <li>Trim hatches ({@code t}, plain only): up to 10 units each of the feed gas (E +2) and Spatially Enlarged Fluid
 * (E −2), 1,000·M/64 and 100·M/64 L a unit.</li>
 * <li>{@link QuasarDisc#step}; then each jet port's {@link JetPacket} goes to the dock registered on its cell
 * ({@link BurstDocks}); a port without one <b>vents</b>.</li>
 * <li>Sensors, Directive Hatches, the render.</li>
 * </ol>
 * A frozen quasar (unformed, or a structure chunk unloaded) does not step and publishes nothing: its hosts pause
 * (Q-8, {@link BurstSource#status}). <b>No block damage, and the hole is never lost</b> (§1.4): blow-outs only dark
 * the disc for its 300 s re-form; Collapse makes the hole dormant with M and a kept; a wrench keeps it on the item.
 *
 * <h2>The four traps</h2>
 * <ol>
 * <li><b>Client vs server.</b> {@link #construct} reads only the trigger (the {@code coil} channel, else the stack
 * size = coil tier); the GUI reads synced values only.</li>
 * <li><b>Tile vs item NBT.</b> {@link #writeQuasarState} is the ONE serializer of the hole and disc, called from both
 * {@link #saveNBTData} and {@link #setItemNBT}; the render position and the gas buffer are tile-only.</li>
 * <li><b>Plant-wide hatch lists.</b> Every socket registers into a private list ({@link QuasarHatchElement}).</li>
 * <li><b>Element chains.</b> Every socket is {@code buildHatchAdder(...).buildAndChain(casing)}; the coil element
 * is the only tiered one and every coil must be the same tier.</li>
 * </ol>
 */
public class MTEQuasarEngine extends MTEExtendedPowerMultiBlockBase<MTEQuasarEngine>
    implements ISurvivalConstructable, DirectiveHost {

    public static final String PIECE = "quasar";
    /** The hologram channel for the Disc Field Coil tier (1-4). */
    public static final String CHANNEL_COIL = "coil";
    /**
     * SMBH v1 (smbh-v1.md §1.1): the Supermassive Lattice, the quasar's optional second piece (an armillary cage round
     * the same controller, hole and disc: an upgrade in place). Its Containment Nodes set the lattice tier.
     */
    public static final String PIECE_LATTICE = "quasar_lattice";
    /** The hologram channel for the lattice tier (1-4; unset = no lattice). */
    public static final String CHANNEL_LATTICE = "lattice";
    /**
     * Saturation (§1.2): 4,096 SM on MagMatter coils, |E| at most 40 (Stable or better), this many seconds unbroken.
     */
    public static final int SATURATION_SECONDS = 3600;
    public static final double SATURATION_E = 40;

    /** Periodic structure re-check, ticks. */
    private static final int RECHECK = 200;
    /** A host report counts for this many ticks. */
    private static final long HOST_FRESH = 60;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_QUASAR_ENGINE");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_QUASAR_ENGINE_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_QUASAR_ENGINE_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_QUASAR_ENGINE_ACTIVE_GLOW");

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread. */
    private static volatile IStructureDefinition<MTEQuasarEngine> STRUCTURE_DEFINITION = null;

    // ------------------------------------------------- PERSISTED: the hole (writeQuasarState is the one writer)
    private QuasarState q = new QuasarState(QuasarState.rngFor(1));
    private double feed = QuasarParams.F_DEFAULT;
    private QuasarGas gas = QuasarGas.HE;
    private double bSetting = 3;
    /** The Directive Disc set's purge setpoint (bit 3); 0 = never. */
    private double purgeAt = QuasarParams.PURGE_AT_DEFAULT;
    private boolean renderOn = true;
    /**
     * SMBH (smbh-v1.md §2.2, §3.2, §5.1): the mass setpoint (0 = the lattice's safe limit), Unbind and the Overdrive
     * level. All three do nothing without a lattice; the engine ({@link HorizonLattice}) reads them every second.
     */
    private double massSetpoint;
    private boolean unbound;
    private double overdrive;
    /**
     * SMBH Saturation (smbh-v1.md §1.2): the lattice counts only once the quasar has held 4,096 SM Stable for an hour
     * on MagMatter coils. Saved with the hole (tile and wrenched item); the streak is the seconds held so far.
     */
    private boolean saturated;
    private int saturationStreak;
    /** Lifetime counters (GUI, scanner). */
    private long eruptions, purges, blowOuts;
    private String lastEvent = "";

    // ------------------------------------------------- PERSISTED, tile only
    /** The gas buffer, fluid name → litres (refilled from the gas hatches every second). */
    private final Map<String, Long> buffer = new HashMap<>();
    private int[] renderPos;

    // ------------------------------------------------- WALK state (resetWalk)
    private int mCasing;
    private int coilTier = -1;
    /**
     * SMBH: the Supermassive Lattice tier in force (0-4; smbh-v1.md §1.1): the tier the walk found
     * ({@link #latticeBuilt}) once the quasar is saturated, else 0. The engine reads it every second.
     */
    private int latticeTier;
    /** The tier of the lattice that stands (0: none, or incomplete), saturated or not. */
    private int latticeBuilt;
    /** The Containment Nodes' tier, set by the tiered element during the lattice walk (-1: none seen yet). */
    private int nodeTier = -1;
    /** What the last walk found of the lattice, for the scanner ("" when there is none and none is needed). */
    private String latticeNote = "";
    private final ArrayList<MTEHatchInput> gasHatches = new ArrayList<>();
    private final ArrayList<MTEHatchInput> trimHatches = new ArrayList<>();
    private final ArrayList<MTEHatchBurstControl> burstHatches = new ArrayList<>();
    private final ArrayList<MTEHatchStellarFieldSensor> sensorHatches = new ArrayList<>();
    private final ArrayList<MTEHatchDirective> directiveHatches = new ArrayList<>();

    // ------------------------------------------------- RUN state (server)
    private boolean chunksLoaded;
    private boolean secondPowerOk = true;
    private boolean lastPowerOk = true;
    private QuasarResult last = new QuasarResult();
    private final double[] jetBu = new double[JetPort.VALUES.length];
    private final double[] jetTaken = new double[JetPort.VALUES.length];
    private final String[] jetDock = new String[JetPort.VALUES.length];
    /** A dock stood on the port but refused the jet (misaligned): the jet vented (P4-94). */
    private final boolean[] jetRefused = new boolean[JetPort.VALUES.length];
    private double gasDrawn;
    private double trimGasUnits, trimSpaceUnits;
    private double bInUse;
    private final Map<String, HostReport> hosts = new LinkedHashMap<>();
    private final QuietTileUpdate quietTileUpdate = new QuietTileUpdate();
    private String renderNote = "";
    private boolean renderOpenSeen;
    private int collapseTimer = -1;
    private boolean flashPending;
    /** The last time a Conduit element on one of this quasar's paths was closed (Disc bit 4, P4-44). */
    private long conduitClosedTick = Long.MIN_VALUE / 2;
    private String conduitClosedAt = "";

    /** One host fed from this quasar, as the conduit or the host reported it last second. */
    public static final class HostReport {

        public final String label;
        public final double bu, sigma, streak;
        public final long tick;

        HostReport(String label, double bu, double sigma, double streak, long tick) {
            this.label = label;
            this.bu = bu;
            this.sigma = sigma;
            this.streak = streak;
            this.tick = tick;
        }
    }

    public MTEQuasarEngine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEQuasarEngine(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEQuasarEngine(this.mName);
    }

    // =================================================================================== shape

    @Override
    public IStructureDefinition<MTEQuasarEngine> getStructureDefinition() {
        IStructureDefinition<MTEQuasarEngine> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEQuasarEngine.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTEQuasarEngine> buildDefinition() {
        final Block sl = SLBlocks.slCasing;
        final Block sl2 = SLBlocks.slCasing2;
        final Block sl4 = SLBlocks.slCasing4;
        final int shell = Casings.ExtremeDensitySpaceBendingCasing.textureId;
        return StructureDefinition.<MTEQuasarEngine>builder()
            .addShape(PIECE, BTSShapes.load(PIECE))
            .addShape(PIECE_LATTICE, BTSShapes.load(PIECE_LATTICE))
            // SMBH: the lattice's frame and its Containment Nodes (smbh-v1.md §1.1)
            .addElement('L', block(sl4, BlockSLCasing4.LATTICE_FRAME))
            .addElement('N', nodes(sl4))
            .addElement('A', casing(Casings.ExtremeDensitySpaceBendingCasing))
            .addElement('B', noOverwrite(onElementPass(x -> ++x.mCasing, ofFrame(Materials.NaquadahAlloy))))
            .addElement('C', block(sl2, BlockSLCasing2.JET_COLLIMATOR))
            .addElement('E', block(sl2, BlockSLCasing2.EDDINGTON_WALL))
            .addElement('G', casing(Casings.QuantumGlass))
            .addElement('H', coils(sl2))
            .addElement('I', block(sl2, BlockSLCasing2.RIM_GAS_INJECTOR))
            .addElement('P', block(sl, BlockSLCasing.CASIMIR))
            .addElement('T', casing(Casings.BackgroundRadiationAbsorbentCasing))
            // ---- sockets (private lists, trap 3); every one chains its fallback casing (trap 4)
            .addElement(
                'h',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(QuasarHatchElement.Gas)
                    .casingIndex(shell)
                    .hint(1)
                    .buildAndChain(block(sl2, BlockSLCasing2.EDDINGTON_WALL)))
            .addElement(
                't',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(QuasarHatchElement.Trim)
                    .casingIndex(shell)
                    .hint(2)
                    .buildAndChain(casing(Casings.BackgroundRadiationAbsorbentCasing)))
            .addElement(
                'c',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(QuasarHatchElement.BurstControl)
                    .casingIndex(shell)
                    .hint(3)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'n',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(QuasarHatchElement.Sensor)
                    .casingIndex(shell)
                    .hint(4)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'd',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(QuasarHatchElement.Directive)
                    .casingIndex(shell)
                    .hint(5)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'e',
                buildHatchAdder(MTEQuasarEngine.class).atLeast(Energy, ExoticEnergy)
                    .casingIndex(shell)
                    .hint(6)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            // the quasar runs no recipes, so it takes no maintenance: plain shell first (builds place it), a
            // maintenance hatch still accepted there
            .addElement(
                'm',
                ofChain(
                    casing(Casings.ExtremeDensitySpaceBendingCasing),
                    buildHatchAdder(MTEQuasarEngine.class).atLeast(Maintenance)
                        .casingIndex(shell)
                        .hint(7)
                        .build()))
            .build();
    }

    private static IStructureElement<MTEQuasarEngine> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEQuasarEngine> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /**
     * The Disc Field Coils, T1-T4, on the {@code coil} hologram channel: StructureLib's tiered element (every coil the
     * same tier; the walk records it), so the projector and NEI's preview get a 1-4 slider. Builds place the
     * channel's tier (else the trigger stack's size) and never write into a tile.
     */
    private static IStructureElement<MTEQuasarEngine> coils(Block sl2) {
        final List<Pair<Block, Integer>> tiers = new ArrayList<>();
        for (int t = 1; t <= 4; t++) tiers.add(Pair.of(sl2, BlockSLCasing2.discCoil(t)));
        return withChannel(
            CHANNEL_COIL,
            noOverwrite(
                ofBlocksTiered(
                    (block, meta) -> block == sl2 && BlockSLCasing2.discCoilTier(meta) > 0
                        ? BlockSLCasing2.discCoilTier(meta)
                        : null,
                    tiers,
                    -1,
                    (MTEQuasarEngine t, Integer tier) -> t.coilTier = tier,
                    t -> t.coilTier)));
    }

    /**
     * The Containment Nodes L1-L4 on the {@code lattice} channel (smbh-v1.md §1.1): every node the same tier, as GT
     * coils; the walk records it. Builds place the channel's tier and never write into a tile.
     */
    private static IStructureElement<MTEQuasarEngine> nodes(Block sl4) {
        final List<Pair<Block, Integer>> tiers = new ArrayList<>();
        for (int t = 1; t <= QuasarParams.LATTICE_MAX; t++) tiers.add(Pair.of(sl4, BlockSLCasing4.node(t)));
        return withChannel(
            CHANNEL_LATTICE,
            noOverwrite(
                ofBlocksTiered(
                    (block, meta) -> block == sl4 && BlockSLCasing4.nodeTier(meta) > 0 ? BlockSLCasing4.nodeTier(meta)
                        : null,
                    tiers,
                    -1,
                    (MTEQuasarEngine t, Integer tier) -> t.nodeTier = tier,
                    t -> t.nodeTier)));
    }

    /** Horizontal facings only: the quasar is 161 blocks tall and its jets are vertical. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    private void resetWalk() {
        mCasing = 0;
        coilTier = -1;
        latticeTier = 0;
        latticeBuilt = 0;
        nodeTier = -1;
        gasHatches.clear();
        trimHatches.clear();
        burstHatches.clear();
        sensorHatches.clear();
        directiveHatches.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final int[] o = BTSShapes.offset(PIECE);
        if (!checkPiece(PIECE, o[0], o[1], o[2], errors)) return;
        // coil power is 2 A UXV x b^2 (18 A UXV at b 3): any mix of energy and laser hatches, at least one
        if (mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.quasar.energy"));
        }
        if (gasHatchCount() == 0) errors.add(StructureErrors.of("beyondthestars.structure.quasar.gas"));
        if (coilTier < 1) errors.add(StructureErrors.of("beyondthestars.structure.quasar.coil"));
        checkLattice(errors);
    }

    /**
     * SMBH (smbh-v1.md §1.1-§1.2): the optional lattice piece. A whole lattice of one node tier sets
     * {@link #latticeBuilt}; it is in force ({@link #latticeTier}) once the quasar is saturated. A lattice swapped to a
     * lower tier is a whole lattice: the engine sheds the hole to that tier's top (§2.5). An INCOMPLETE lattice under
     * a hole heavier than any coil holds (over 4,096 SM) unforms the quasar instead, so a broken frame block freezes
     * the hole (nothing moves, nothing is lost) until it is repaired; to take a lattice down, shed to 4,096 SM first.
     */
    private void checkLattice(List<StructureError> errors) {
        nodeTier = -1;
        final int[] o = BTSShapes.offset(PIECE_LATTICE);
        final List<StructureError> trial = new ArrayList<>();
        final boolean whole = checkPiece(PIECE_LATTICE, o[0], o[1], o[2], trial) && nodeTier >= 1;
        latticeBuilt = whole ? Math.min(QuasarParams.LATTICE_MAX, nodeTier) : 0;
        latticeTier = saturated ? latticeBuilt : 0;
        final boolean needed = q.hole != QuasarState.Hole.NONE && q.M > QuasarParams.M_REF * (1 + 1e-9);
        if (whole) {
            latticeNote = "Lattice L" + latticeBuilt
                + " ("
                + QuasarParams.LATTICE_NAME[latticeBuilt]
                + ")"
                + (!saturated ? ": NOT in force, reach Saturation first"
                    : coilTier < QuasarParams.LATTICE_COIL ? ": NOT in force, it needs MagMatter (T4) coils"
                        : ": in force");
        } else if (needed) {
            latticeNote = "Lattice INCOMPLETE under a " + fmt(q.M)
                + " SM hole: the quasar is frozen until the lattice is whole again";
            errors.add(StructureErrors.of("beyondthestars.structure.quasar.lattice"));
            errors.addAll(trial); // the first wrong lattice cell (the walk stops there)
        } else {
            latticeNote = "";
        }
    }

    private static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity mte : list) if (live(mte)) n++;
        return n;
    }

    public boolean addGasHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchInput)) return false;
        ((MTEHatchInput) mte).updateTexture(casingIndex);
        gasHatches.add((MTEHatchInput) mte);
        return true;
    }

    /** A plain input hatch only: an ME stocking hatch would trim at the cap every second (D-49's trim rule). */
    public boolean addTrimHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchInput) || mte instanceof MTEHatchInputME) return false;
        ((MTEHatchInput) mte).updateTexture(casingIndex);
        trimHatches.add((MTEHatchInput) mte);
        return true;
    }

    public boolean addBurstControlHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchBurstControl)) return false;
        ((MTEHatchBurstControl) mte).updateTexture(casingIndex);
        burstHatches.add((MTEHatchBurstControl) mte);
        return true;
    }

    public boolean addSensorHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchStellarFieldSensor)) return false;
        ((MTEHatchStellarFieldSensor) mte).updateTexture(casingIndex);
        sensorHatches.add((MTEHatchStellarFieldSensor) mte);
        return true;
    }

    public boolean addDirectiveHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (!(mte instanceof MTEHatchDirective)) return false;
        ((MTEHatchDirective) mte).updateTexture(casingIndex);
        directiveHatches.add((MTEHatchDirective) mte);
        return true;
    }

    public int gasHatchCount() {
        return liveCount(gasHatches);
    }

    public int trimHatchCount() {
        return liveCount(trimHatches);
    }

    public int burstControlCount() {
        return liveCount(burstHatches);
    }

    public int sensorHatchCount() {
        return liveCount(sensorHatches);
    }

    public int directiveHatchCount() {
        return liveCount(directiveHatches);
    }

    public List<MTEHatchDirective> directiveHatches() {
        return Collections.unmodifiableList(directiveHatches);
    }

    public List<MTEHatchBurstControl> burstControlHatches() {
        return Collections.unmodifiableList(burstHatches);
    }

    public List<MTEHatchStellarFieldSensor> sensorHatches() {
        return Collections.unmodifiableList(sensorHatches);
    }

    // ==================================================================== construct / survival

    /** The coil tier the trigger asks for: the {@code coil} channel, else the stack size (1-4). Trap 1. */
    public static int triggerCoilTier(ItemStack trigger) {
        final int ch = BTSStructures.channelIndex(trigger, CHANNEL_COIL, -1) + 1;
        if (ch >= 1) return Math.min(4, ch);
        return trigger == null ? 1 : Math.max(1, Math.min(4, trigger.stackSize));
    }

    /** The lattice tier the trigger asks for: the {@code lattice} channel (1-4), else 0 (no lattice). Trap 1. */
    public static int triggerLatticeTier(ItemStack trigger) {
        final int ch = BTSStructures.channelIndex(trigger, CHANNEL_LATTICE, -1) + 1;
        return ch >= 1 ? Math.min(QuasarParams.LATTICE_MAX, ch) : 0;
    }

    /**
     * A lattice counts only on MagMatter coils (smbh-v1.md §1.2): with the {@code lattice} channel set and no
     * {@code coil} channel, a copy of the trigger that builds T4 coils. Otherwise the trigger itself.
     */
    static ItemStack latticeTrigger(ItemStack trigger) {
        if (trigger == null || triggerLatticeTier(trigger) == 0
            || ChannelDataAccessor.hasSubChannel(trigger, CHANNEL_COIL)) return trigger;
        final ItemStack copy = trigger.copy();
        ChannelDataAccessor.setChannelData(copy, CHANNEL_COIL, QuasarParams.LATTICE_COIL);
        return copy;
    }

    /**
     * The whole quasar, coils at the trigger's tier (the {@code coil} channel, else stack 1-4), and with the
     * {@code lattice} channel set the Supermassive Lattice round it, its nodes at that tier (MagMatter coils unless the
     * coil channel says otherwise).
     */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final ItemStack trigger = latticeTrigger(stackSize);
        final int[] o = BTSShapes.offset(PIECE);
        buildPiece(PIECE, trigger, hintsOnly, o[0], o[1], o[2]);
        if (triggerLatticeTier(trigger) > 0) {
            final int[] lo = BTSShapes.offset(PIECE_LATTICE);
            buildPiece(PIECE_LATTICE, trigger, hintsOnly, lo[0], lo[1], lo[2]);
        }
    }

    /**
     * Survival auto-place (projector, NEI preview): the quasar, then (with the {@code lattice} channel) the lattice;
     * -1 once everything asked for stands. Tiles are never overwritten.
     */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final int lattice = triggerLatticeTier(stackSize);
        if (mMachine && (lattice == 0 || latticeBuilt >= lattice)) return -1;
        final ItemStack trigger = latticeTrigger(stackSize);
        final int budget = BTSStructures.survivalBudget(elementBudget);
        final int[] o = BTSShapes.offset(PIECE);
        final int built = survivalBuildPiece(PIECE, trigger, o[0], o[1], o[2], budget, env, false, true);
        if (built >= 0 || lattice == 0) return built;
        final int[] lo = BTSShapes.offset(PIECE_LATTICE);
        return survivalBuildPiece(PIECE_LATTICE, trigger, lo[0], lo[1], lo[2], budget, env, false, true);
    }

    /** The world box of the whole structure for this controller's facing (cached per facing and lattice). */
    private int[] structureBox;
    private ExtendedFacing structureBoxFacing;
    private boolean structureBoxLattice;

    private int[] structureBox(IGregTechTileEntity base) {
        final ExtendedFacing f = getExtendedFacing();
        // SMBH: with a lattice (or a hole that needs one) every lattice chunk must be loaded too
        final boolean lattice = latticeBuilt > 0 || q.hole != QuasarState.Hole.NONE && q.M > QuasarParams.M_REF;
        if (structureBox == null || structureBoxFacing != f || structureBoxLattice != lattice) {
            structureBox = lattice
                ? SLConsoleTest.bounds(f, base.getXCoord(), base.getYCoord(), base.getZCoord(), PIECE, PIECE_LATTICE)
                : SLConsoleTest.bounds(f, base.getXCoord(), base.getYCoord(), base.getZCoord(), PIECE);
            structureBoxFacing = f;
            structureBoxLattice = lattice;
        }
        return structureBox;
    }

    /** True when every chunk the structure stands in is loaded (never loads one). */
    private boolean structureChunksLoaded(IGregTechTileEntity base) {
        final World world = base.getWorld();
        if (world == null) return false;
        final int[] b = structureBox(base);
        return world.checkChunksExist(b[0], 0, b[2], b[3], 255, b[5]);
    }

    // =============================================================================== the tick

    /** The quasar runs no recipes: GT's recipe loop never starts here. */
    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {}

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        if (aTick % 20 == 0) chunksLoaded = structureChunksLoaded(aBaseMetaTileEntity);
        if (aTick > 0 && aTick % RECHECK == 0 && chunksLoaded) checkStructure(true, aBaseMetaTileEntity);
        final boolean frozen = isFrozen();
        if (!frozen && running()) {
            // coil power, every tick (GT's power switch does not stop the hole: Collapse does)
            final long eut = (long) Math.ceil(QuasarParams.coilEUt(effectiveB()));
            if (eut > 0 && !drainEnergyInput(eut)) secondPowerOk = false;
            // the Accretion Buffer between seconds (§4.2): per-tick draws while the hatches give
            if (refillMore && aTick % 20 != 0) refillMore = refillBuffer() > 0 && buffer(gasFluid(gas)) < bufferCap();
        }
        if (aTick % 20 == 0) {
            if (!frozen) {
                second(aBaseMetaTileEntity);
                // the disc state changes every second but GT marks a chunk modified only on inventory changes: without
                // this a quasar whose chunk sees no inventory change reloads an old save after a restart (P4-85)
                aBaseMetaTileEntity.markDirty();
            }
            publishDirectives();
            if (!frozen) publishSensors();
            if (aTick % 600 == 0) BurstDocks.prune(aBaseMetaTileEntity.getWorld());
        }
        tickRender(aBaseMetaTileEntity, aTick);
        aBaseMetaTileEntity.setActive(!frozen && q.hole == QuasarState.Hole.OPEN && q.G > 0);
    }

    /** Only a changed structure centre (or a fresh form) sends the controller to clients (render QA P-1). */
    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onStructureCheckFinished(quietTileUpdate.wrap(aBaseMetaTileEntity));
        quietTileUpdate.after(this, aBaseMetaTileEntity);
    }

    /**
     * Q-8 and §1.4: the disc is frozen (no step, nothing published, so hosts pause) while the structure is not formed
     * or
     * any chunk it stands in is unloaded.
     */
    public boolean isFrozen() {
        return !mMachine || !chunksLoaded;
    }

    /**
     * Bench: re-reads whether every structure chunk is loaded now (the tick does it every 20 ticks, so a command run
     * in the same tick as a build would see a stale "frozen"); returns {@link #isFrozen()}.
     */
    public boolean refreshFrozen() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) chunksLoaded = structureChunksLoaded(base);
        return isFrozen();
    }

    /** The hole is open or re-forming: the disc steps and the coils draw power. */
    private boolean running() {
        return q.hole == QuasarState.Hole.OPEN || q.hole == QuasarState.Hole.REFORM;
    }

    /** One disc second. */
    private void second(IGregTechTileEntity base) {
        final World world = base.getWorld();
        final long now = world.getTotalWorldTime();
        pollBurstHatches();
        double vented = 0;
        if (running()) {
            final QuasarInputs in = new QuasarInputs();
            in.gas = gas;
            in.coilTier = Math.max(1, coilTier);
            in.bSetpoint = bSetting();
            in.powerOk = secondPowerOk;
            in.lattice = latticeTier;
            if (unbound && !Config.smbhAllowUnbound) {
                unbound = false;
                lastEvent = "Unbind is off in the config: the hole sheds to " + fmt(QuasarParams.LATTICE_TOP[4])
                    + " SM";
            }
            in.massSetpoint = massSetpointInUse();
            in.unbound = unbound;
            in.overdrive = overdriveInUse();
            final double medd = QuasarParams.eddingtonFeed(q.M, gas);
            final double m0 = q.M;
            refillBuffer();
            final double want = Math.max(0, feed) * medd;
            final long have = buffer(gasFluid(gas));
            final double drawn = Math.min(have, want);
            takeBuffer(gasFluid(gas), (long) Math.ceil(drawn));
            in.gasDrawn = drawn;
            gasDrawn = drawn;
            // SMBH (smbh-v1.md §10.1-§10.2): the anchor on hand caps Unbound growth, so it is read before the plan;
            // the hold-up and growth segments come out of what is left in the feed buffer, the Overdrive fluids and
            // the anchor from the trim sockets, then the gas sockets (ME stocking hatches are fine there: these are
            // upkeeps, not trims). Without a lattice every one of them is 0 and nothing is drawn.
            final boolean lattice = QuasarParams.latticeInForce(in.coilTier, latticeTier) > 0;
            if (lattice && unbound) in.anchorAvailable = upkeepAvailable(primordialFluid());
            final HorizonLattice.Plan plan = HorizonLattice.plan(q, in);
            // the Accretion Buffer (§4.2) keeps 60 s of Disc + Hold-up: the hold-up may use all of it, the Growth
            // segment only what is over that reserve (so growth is the first to drop out when the feed runs short)
            if (plan.holdUpNeed + plan.growthRoom > 0) {
                final double onHand = buffer(gasFluid(gas));
                final double reserve = accretion()[3];
                in.intakeAvailable = Math.min(onHand, plan.holdUpNeed)
                    + Math.max(0, onHand - plan.holdUpNeed - reserve);
            }
            if (plan.overdriveNeed > 0) {
                in.mhdcsmAvailable = upkeepAvailable(mhdcsmFluid());
                in.universiumAvailable = upkeepAvailable(universiumFluid());
            }
            // trims: what the plain trim hatches hold, in whole units, at most 10 each
            final double gasUnitL = QuasarParams.gasTrimUnitLitres(q.M);
            final double spaceUnitL = QuasarParams.spaceTrimUnitLitres(q.M);
            // P4-77: an OpenComputers trim pulse adds units paid from the feed buffer (gas) or the gas sockets
            // (Spatially Enlarged Fluid, ME stocking included), within the same cap of 10
            final double ocG = Math.min(ocGasUnits, Math.floor(buffer(gasFluid(gas)) / gasUnitL));
            final double ocS = Math.min(ocSpaceUnits, Math.floor(available(gasHatches, spaceFluid()) / spaceUnitL));
            ocGasUnits = 0;
            ocSpaceUnits = 0;
            in.gasTrimUnits = Math
                .min(QuasarParams.TRIM_CAP, Math.floor(available(trimHatches, gasFluid(gas)) / gasUnitL) + ocG);
            in.spaceTrimUnits = Math
                .min(QuasarParams.TRIM_CAP, Math.floor(available(trimHatches, spaceFluid()) / spaceUnitL) + ocS);
            final QuasarResult r = QuasarDisc.step(q, in);
            HorizonLattice.step(q, in, plan, r);
            final double intake = r.holdUpLitres + r.growthLitres;
            if (intake > 0) takeBuffer(gasFluid(gas), (long) Math.ceil(intake));
            if (r.mhdcsmLitres > 0) takeUpkeep(mhdcsmFluid(), r.mhdcsmLitres);
            if (r.universiumLitres > 0) takeUpkeep(universiumFluid(), r.universiumLitres);
            if (r.anchorLitres > 0) takeUpkeep(primordialFluid(), r.anchorLitres);
            if (r.gasTrimLitres > 0) {
                final long need = (long) Math.ceil(r.gasTrimLitres);
                final long got = take(trimHatches, gasFluid(gas), need);
                if (got < need) takeBuffer(gasFluid(gas), need - got);
            }
            if (r.spaceLitres > 0) {
                final long need = (long) Math.ceil(r.spaceLitres);
                final long got = take(trimHatches, spaceFluid(), need);
                if (got < need) take(gasHatches, spaceFluid(), need - got);
            }
            trimGasUnits = r.gasTrimUnits;
            trimSpaceUnits = r.spaceTrimUnits;
            bInUse = r.b;
            massRatePerHour = m0 > 0 ? (q.M - m0) / m0 * 3600 : 0;
            leakRatePerHour = m0 > 0 ? -r.leakedMass / m0 * 3600 : 0;
            refillMore = true;
            last = r;
            noteEvents(r);
            trackSaturation();
        } else {
            last = new QuasarResult();
            massRatePerHour = leakRatePerHour = 0;
            refillMore = false;
            gasDrawn = 0;
            trimGasUnits = trimSpaceUnits = 0;
            bInUse = 0;
        }
        lastPowerOk = secondPowerOk;
        secondPowerOk = true;
        // the jets: every port gets this second's packet, a loaded dark quasar sends 0 (hosts feel it in full)
        final boolean linkable = mMachine && q.hole == QuasarState.Hole.OPEN && q.b > 0;
        final BurstSource source = BurstSource.of(base);
        for (JetPort port : JetPort.VALUES) {
            final int i = port.ordinal();
            final double bu = running() ? last.jet : 0;
            jetBu[i] = bu;
            final int[] cell = portCell(base, port);
            final IBurstDock dock = BurstDocks.find(world, cell[0], cell[1], cell[2]);
            if (dock == null) {
                jetDock[i] = null;
                jetRefused[i] = false;
                jetTaken[i] = 0;
                vented += bu;
                continue;
            }
            jetDock[i] = dock.dockLabel();
            final double got = dock
                .acceptJet(new JetPacket(source, port, bu, linkable, now, getLatticeInForce(), getMass()));
            jetRefused[i] = got < 0; // misaligned: the whole jet vents (P4-94); a cap's clip does not
            if (jetRefused[i]) vented += bu;
            jetTaken[i] = Math.max(0, Math.min(bu, got));
        }
        lastVented = vented;
        pruneHosts(now);
    }

    private double lastVented;
    /** SMBH: last second's net mass change and the leak alone, as fractions of M per hour (GUI, sensors). */
    private double massRatePerHour, leakRatePerHour;
    /** The Accretion Buffer keeps refilling every tick after a second's draw while the hatches still give. */
    private boolean refillMore;

    private void noteEvents(QuasarResult r) {
        if (r.eruption) {
            eruptions++;
            lastEvent = "Eruption (flux 100)";
            flashPending = true;
        }
        if (r.purge) {
            purges++;
            lastEvent = "Purge";
            flashPending = true;
        }
        if (r.purgeRefused) lastEvent = "Purge refused: flux under 20";
        if (r.blowOut) {
            blowOuts++;
            lastEvent = "Disc blow-out: " + r.blowOutReason;
        }
        if (r.reformed) lastEvent = "Disc re-formed";
        if (r.shedding) {
            lastEvent = "Shedding to " + fmt(r.shedTarget) + " SM (" + shedWhy(r.shedReason) + "): no power meanwhile";
        }
        if (r.firstLight) lastEvent = "First Light";
        if (r.eddington) lastEvent = "Milestone: 1 h at the Eddington limit";
        if (r.massMilestone >= 0) {
            lastEvent = "Milestone: " + fmt(QuasarParams.MASS_MILESTONES[r.massMilestone]) + " SM";
        }
    }

    /**
     * Saturation (smbh-v1.md §1.2), once a disc second: 4,096 SM on MagMatter coils, the hole open and |E| at most 40,
     * for {@link #SATURATION_SECONDS} unbroken. Then the lattice that stands is in force at once.
     */
    private void trackSaturation() {
        if (saturated) return;
        final boolean holding = coilTier >= QuasarParams.LATTICE_COIL && q.hole == QuasarState.Hole.OPEN
            && q.M >= QuasarParams.M_REF * (1 - 1e-9)
            && Math.abs(q.E) <= SATURATION_E;
        saturationStreak = holding ? saturationStreak + 1 : 0;
        if (saturationStreak >= SATURATION_SECONDS) {
            saturated = true;
            latticeTier = latticeBuilt;
            if (latticeBuilt > 0) latticeNote = "Lattice L" + latticeBuilt
                + " ("
                + QuasarParams.LATTICE_NAME[latticeBuilt]
                + "): in force";
            lastEvent = "Milestone: Saturation (4,096 SM held Stable for 1 h): the Supermassive Lattice unlocks";
        }
    }

    /** Bench: re-walks the structure now (hatches placed by a script join their lists at once). */
    public void benchRecheck() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) checkStructure(true, base);
    }

    /** True once the Saturation milestone is held (smbh-v1.md §1.2): the lattice counts. */
    public boolean isSaturated() {
        return saturated;
    }

    /** Seconds of the Saturation streak so far (the full hour once saturated; 0 when the streak broke). */
    public int getSaturationSeconds() {
        return saturated ? SATURATION_SECONDS : saturationStreak;
    }

    /**
     * Bench / op only ({@code /btssmbh}, {@code /btsquasar ... saturate}): sets or clears the Saturation milestone and
     * re-walks the structure so the lattice counts at once. Returns the lattice tier in force after it.
     */
    public int debugSaturate(boolean on) {
        saturated = on;
        saturationStreak = 0;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) checkStructure(true, base);
        return latticeTier;
    }

    /** The world cell of a jet port (where a dock's intake must be). */
    public int[] portCell(IGregTechTileEntity base, JetPort port) {
        return QuasarGeometry.world(
            getExtendedFacing(),
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            QuasarGeometry.portRel(port));
    }

    // ------------------------------------------------------------------ b and the Burst Control Hatches

    /** The b setpoint in use: a FIELD-mode Burst Control Hatch (the highest level) overrides the GUI's. */
    private double bSetting() {
        int s = -1;
        for (MTEHatchBurstControl h : burstHatches) {
            if (live(h) && h.getMode() == MTEHatchBurstControl.Mode.FIELD) s = Math.max(s, h.level());
        }
        if (s < 0) return bSetting;
        final int tier = Math.max(1, coilTier);
        return QuasarParams.B_MIN + (tier - QuasarParams.B_MIN) * s / 15.0;
    }

    /** The field the coils carry now: min(setting, coil tier). */
    public double effectiveB() {
        return Math.min(bSetting(), Math.max(1, coilTier));
    }

    /** The strongest level of the live Burst Control Hatches in {@code mode}, or -1 when none is in it. */
    private int hatchLevel(MTEHatchBurstControl.Mode mode) {
        int s = -1;
        for (MTEHatchBurstControl h : burstHatches) if (live(h) && h.getMode() == mode) s = Math.max(s, h.level());
        return s;
    }

    /**
     * SMBH (smbh-v1.md §6): the mass setpoint in use. A HOLD-mode Burst Control Hatch (the highest level) overrides
     * the GUI's: 0 the safe limit .. 15 the band top, on a log scale (on an Unbound L4, the L4 safe limit .. two
     * doublings past the L4 top). Without a lattice in force the setting is kept and unused.
     */
    double massSetpointInUse() {
        final int s = hatchLevel(MTEHatchBurstControl.Mode.HOLD);
        final int L = getLatticeInForce();
        if (s < 0 || L == 0) return massSetpoint;
        return holdSetpoint(L, unbound, s);
    }

    /** A HOLD level 0-15 as a mass setpoint, SM (log-linear from the safe limit to the top it may reach). */
    static double holdSetpoint(int L, boolean unbound, int s) {
        final double lo = QuasarParams.LATTICE_SAFE[L];
        final double hi = L == QuasarParams.LATTICE_MAX && unbound ? QuasarParams.LATTICE_TOP[L] * 4
            : QuasarParams.LATTICE_TOP[L];
        return lo * Math.pow(hi / lo, Math.max(0, Math.min(15, s)) / 15.0);
    }

    /** SMBH: the Overdrive level in use: an OVERDRIVE-mode hatch (s / 15, 5 % steps) overrides the GUI's. */
    double overdriveInUse() {
        final int s = hatchLevel(MTEHatchBurstControl.Mode.OVERDRIVE);
        if (s < 0) return overdrive;
        return Math.round(s / 15.0 / QuasarParams.OVERDRIVE_STEP) * QuasarParams.OVERDRIVE_STEP;
    }

    /** True while a Burst Control Hatch in HOLD mode sets the mass setpoint. */
    public boolean holdFromHatch() {
        return hatchLevel(MTEHatchBurstControl.Mode.HOLD) >= 0;
    }

    /** True while a Burst Control Hatch in OVERDRIVE mode sets the Overdrive level. */
    public boolean overdriveFromHatch() {
        return hatchLevel(MTEHatchBurstControl.Mode.OVERDRIVE) >= 0;
    }

    /**
     * Once a second: a PURGE-mode rising edge requests a purge, a SHED-mode one lowers the mass setpoint to the safe
     * limit; every hatch learns its host and line.
     */
    private void pollBurstHatches() {
        for (MTEHatchBurstControl h : burstHatches) {
            if (!live(h)) continue;
            final MTEHatchBurstControl.Mode mode = h.poll(
                "Quasar Engine",
                burstHatchLine(h.getMode()),
                MTEHatchBurstControl.Mode.PURGE,
                MTEHatchBurstControl.Mode.FIELD,
                MTEHatchBurstControl.Mode.HOLD,
                MTEHatchBurstControl.Mode.OVERDRIVE,
                MTEHatchBurstControl.Mode.SHED);
            final boolean edge = h.consumeRisingEdge();
            if (edge && mode == MTEHatchBurstControl.Mode.PURGE && running()) q.requestPurge();
            if (edge && mode == MTEHatchBurstControl.Mode.SHED) shedToSafe();
        }
    }

    /** What a hatch in {@code mode} drives now (its GUI and the scanner). */
    private String burstHatchLine(MTEHatchBurstControl.Mode mode) {
        switch (mode) {
            case FIELD:
                return String.format(Locale.ROOT, "b = %.2f", bSetting());
            case HOLD:
                return getLatticeInForce() == 0 ? "no lattice in force: nothing to hold"
                    : "mass setpoint " + fmt(getMassCap()) + " SM";
            case OVERDRIVE:
                return getLatticeInForce() < QuasarParams.LATTICE_OVERDRIVE ? "Overdrive needs an L3 lattice"
                    : String.format(Locale.ROOT, "Overdrive %.0f %%", 100 * overdriveInUse());
            case SHED:
                return getLatticeInForce() == 0 ? "no lattice in force"
                    : "a rising edge sheds to the safe limit (" + fmt(QuasarParams.LATTICE_SAFE[getLatticeInForce()])
                        + " SM)";
            default:
                return "purge on a rising edge (flux " + Math.round(q.Phi) + ")";
        }
    }

    // ------------------------------------------------------------------ fluids

    public static Fluid gasFluid(QuasarGas g) {
        final FluidStack s;
        switch (g) {
            case H:
                s = Materials.Hydrogen.getGas(1L);
                break;
            case RSM:
                s = Materials.RawStarMatter.getFluid(1L);
                break;
            default:
                s = Materials.Helium.getGas(1L);
                break;
        }
        return s == null ? null : s.getFluid();
    }

    /** SMBH Overdrive (§3.2): Molten MHDCSM and Molten Universium; the Unbound anchor (§5.2): Primordial Matter. */
    public static Fluid mhdcsmFluid() {
        final FluidStack s = Materials.MHDCSM.getMolten(1L);
        return s == null ? null : s.getFluid();
    }

    public static Fluid universiumFluid() {
        final FluidStack s = Materials.Universium.getMolten(1L);
        return s == null ? null : s.getFluid();
    }

    public static Fluid primordialFluid() {
        final FluidStack s = Materials.PrimordialMatter.getFluid(1L);
        return s == null ? null : s.getFluid();
    }

    /** An SMBH upkeep fluid on hand: the trim sockets and the gas sockets together, litres. */
    private double upkeepAvailable(Fluid fluid) {
        return (double) available(trimHatches, fluid) + available(gasHatches, fluid);
    }

    /** Drains an SMBH upkeep: the trim sockets first, then the gas sockets. */
    private void takeUpkeep(Fluid fluid, double litres) {
        final long need = (long) Math.ceil(litres);
        final long got = take(trimHatches, fluid, need);
        if (got < need) take(gasHatches, fluid, need - got);
    }

    static String shedWhy(String reason) {
        if ("structure".equals(reason)) return "the structure holds less";
        if ("anchor".equals(reason)) return "anchor short";
        return "setpoint lowered";
    }

    /** Spatially Enlarged Fluid, molten {@code Materials.Space}. */
    public static Fluid spaceFluid() {
        final FluidStack s = Materials.Space.getMolten(1L);
        return s == null ? null : s.getFluid();
    }

    public long buffer(Fluid fluid) {
        if (fluid == null) return 0;
        final Long v = buffer.get(fluid.getName());
        return v == null ? 0 : v;
    }

    private void takeBuffer(Fluid fluid, long amount) {
        if (fluid == null || amount <= 0) return;
        final long left = buffer(fluid) - amount;
        if (left > 0) buffer.put(fluid.getName(), left);
        else buffer.remove(fluid.getName());
    }

    /**
     * The Accretion Buffer's sizes at this mass (smbh-v1.md §4.2), litres: {Disc a second (f·Ṁ_Edd, at least the
     * re-form's 0.3), Hold-up a second (3·x·Ṁ_Edd), the Growth room a second (up to the 5x cap), the reserve the
     * Growth segment leaves alone (59 s of Disc + Hold-up)}. Without a lattice in force only the Disc counts.
     */
    double[] accretion() {
        final double medd = QuasarParams.eddingtonFeed(q.M, gas);
        final double disc = Math.max(feed, QuasarParams.REFORM_MIN_FEED) * medd;
        double hold = 0, grow = 0;
        if (getLatticeInForce() > 0) {
            hold = QuasarParams.holdUpNeed(QuasarParams.bandX(q.M, Math.max(1, coilTier), latticeTier), medd);
            grow = Math.max(0, QuasarParams.INTAKE_CAP * medd - disc - hold);
        }
        return new double[] { disc, hold, grow, (QuasarParams.ACCRETION_SECONDS - 1) * (disc + hold) };
    }

    /**
     * The Accretion Buffer's cap (smbh-v1.md §4.2): {@link QuasarParams#ACCRETION_SECONDS} of Disc + Hold-up, plus
     * one second of Growth room. Kept in a long (an Unbound hole's minute of hold-up passes 2³¹ by far).
     */
    public long bufferCap() {
        final double[] a = accretion();
        final double cap = QuasarParams.ACCRETION_SECONDS * (a[0] + a[1]) + a[2];
        return (long) Math.min(Long.MAX_VALUE / 4, Math.ceil(cap));
    }

    /**
     * Seconds of Disc (at the feed set) + Hold-up the Accretion Buffer holds now, for the GUI ("Buffer 42 s"), the
     * sensors and the Horizon set's "feed short" bit; +∞ with no hole running.
     */
    public double getBufferSeconds() {
        if (!running()) return Double.POSITIVE_INFINITY;
        final double medd = QuasarParams.eddingtonFeed(q.M, gas);
        final double perSecond = Math.max(0, feed) * medd + accretion()[1];
        return perSecond > 0 ? buffer(gasFluid(gas)) / perSecond : Double.POSITIVE_INFINITY;
    }

    /**
     * Tops the selected gas's Accretion Buffer up from the Rim Gas Injector hatches (ME stocking hatches pull from
     * AE); each drain is one FluidStack, at most 2³¹ − 1 L. Returns the litres taken.
     */
    private long refillBuffer() {
        final Fluid fluid = gasFluid(gas);
        if (fluid == null) return 0;
        final long room = bufferCap() - buffer(fluid);
        if (room <= 0) return 0;
        final long got = take(gasHatches, fluid, room);
        if (got > 0) buffer.put(fluid.getName(), buffer(fluid) + got);
        return got;
    }

    static long available(List<MTEHatchInput> list, Fluid fluid) {
        if (fluid == null) return 0;
        long n = 0;
        for (MTEHatchInput hatch : list) {
            if (!live(hatch)) continue;
            for (FluidStack s : storedFluids(hatch)) if (s != null && s.getFluid() == fluid) n += s.amount;
        }
        return n;
    }

    private long take(List<MTEHatchInput> list, Fluid fluid, long amount) {
        if (fluid == null) return 0;
        long got = 0;
        for (MTEHatchInput hatch : list) {
            if (got >= amount) break;
            if (!live(hatch)) continue;
            final boolean aware = hatch instanceof IRecipeProcessingAwareHatch;
            if (aware) ((IRecipeProcessingAwareHatch) hatch).startRecipeProcessing();
            try {
                for (FluidStack s : storedFluids(hatch)) {
                    if (got >= amount) break;
                    if (s == null || s.getFluid() != fluid || s.amount <= 0) continue;
                    final int want = (int) Math.min(s.amount, amount - got);
                    final FluidStack d = hatch.drain(ForgeDirection.UNKNOWN, new FluidStack(fluid, want), true);
                    if (d != null) got += d.amount;
                }
            } finally {
                if (aware) ((IRecipeProcessingAwareHatch) hatch).endRecipeProcessing(this);
            }
        }
        return got;
    }

    static FluidStack[] storedFluids(MTEHatchInput hatch) {
        if (hatch instanceof MTEHatchInputME) return ((MTEHatchInputME) hatch).getStoredFluids();
        if (hatch instanceof MTEHatchMultiInput) return ((MTEHatchMultiInput) hatch).getStoredFluid();
        return new FluidStack[] { hatch.getFluid() };
    }

    // ============================================================================ the hole

    /**
     * GUI / OpenComputers "Open": a hole that was never seeded takes a Quasar Seed from the controller slot (64 SM,
     * a = 0, P4-16); a dormant hole takes one of GT's Black Hole Seeds. Needs a formed quasar. Returns the reason it
     * did not open, or "" when it opened.
     */
    public String openHole() {
        if (!mMachine) return "the quasar is not formed";
        if (running()) return "the hole is already open";
        if (q.hole != QuasarState.Hole.NONE && q.M > getStructureTop()) {
            return "the hole (" + fmt(q.M)
                + " SM) is too big for this structure (it holds "
                + fmt(getStructureTop())
                + " SM): fit higher coils or a lattice first";
        }
        final ItemStack slot = mInventory[getControllerSlotIndex()];
        if (q.hole == QuasarState.Hole.NONE) {
            if (slot == null || SLQuasarItems.quasarSeed == null || slot.getItem() != SLQuasarItems.quasarSeed) {
                return "needs a Quasar Seed in the controller slot";
            }
            consumeControllerSlot();
            reseedNoise();
            q.seed(QuasarParams.SEED_MASS, QuasarParams.SEED_SPIN);
            lastEvent = "Seeded: a 64 SM hole";
            return "";
        }
        final ItemStack opener = ItemList.Black_Hole_Opener.get(1L);
        if (slot == null || opener == null || !GTUtility.areStacksEqual(slot, opener, true)) {
            return "a dormant hole re-opens with a Black Hole Seed in the controller slot";
        }
        consumeControllerSlot();
        q.reopen();
        lastEvent = "Re-opened";
        return "";
    }

    /** A new hole gets its own disc noise sequence (from its position and the world time); the state is saved. */
    private void reseedNoise() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return;
        final long seed = (base.getXCoord() * 73_856_093L) ^ (base.getYCoord() * 19_349_663L)
            ^ (base.getZCoord() * 83_492_791L)
            ^ base.getWorld()
                .getTotalWorldTime();
        q.rng = QuasarState.rngFor(seed & 0xFFFFFL);
    }

    private void consumeControllerSlot() {
        final int i = getControllerSlotIndex();
        if (mInventory[i] == null) return;
        mInventory[i].stackSize--;
        if (mInventory[i].stackSize <= 0) mInventory[i] = null;
        markDirty();
    }

    /** GUI / OpenComputers "Collapse": the hole goes dormant, M and a kept (§1.1). */
    public boolean collapseHole() {
        if (!running()) return false;
        q.collapse();
        lastEvent = "Collapsed: the hole is dormant (mass kept)";
        return true;
    }

    /** P4-77: OpenComputers trim pulses for the next disc second, in units (not saved). */
    private double ocGasUnits, ocSpaceUnits;

    /**
     * OpenComputers {@code trim(kind, units)} (quasar-p4.md §8.2, P4-77): the next disc second takes up to
     * {@code units} (0-10) trim units of the feed gas ("gas", from the quasar's own feed buffer) or of Spatially
     * Enlarged Fluid ("space", from the gas sockets: an ME stocking hatch can hold it there), on top of what the trim
     * hatches hold, within the same cap of 10. Returns what was queued, or why not.
     */
    public String requestTrim(String kind, double units) {
        if (!running()) return "no hole running: nothing to trim";
        final double u = Math.max(0, Math.min(QuasarParams.TRIM_CAP, Double.isNaN(units) ? 0 : units));
        final String k = kind == null ? ""
            : kind.trim()
                .toLowerCase(Locale.ROOT);
        if (k.equals("gas") || k.equals("feed")) {
            ocGasUnits = u;
            return String.format(Locale.ROOT, "%s: %.1f units next second", gas.displayName, u);
        }
        if (k.equals("space") || k.equals("spatially enlarged fluid")) {
            ocSpaceUnits = u;
            return String.format(Locale.ROOT, "Spatially Enlarged Fluid: %.1f units next second", u);
        }
        return "trim gas|space";
    }

    /** A purge request (GUI, OpenComputers): false when the hole is not running or the flux is under 20. */
    public boolean requestPurge() {
        if (!running() || q.Phi < QuasarParams.PURGE_MIN) return false;
        q.requestPurge();
        return true;
    }

    /**
     * Debug / creative: a hole of {@code mass} SM, open at once, spin {@code spin} (no item). The mass is clamped to
     * 1 .. the structure's top (smbh-v1.md §10.3); returns the mass set.
     */
    public double debugSeed(double mass, double spin) {
        reseedNoise();
        q.seed(clampMass(mass), Math.max(0, Math.min(QuasarParams.A_MAX, Double.isFinite(spin) ? spin : 0)));
        lastEvent = "Seeded (debug)";
        return q.M;
    }

    /** Debug: set the mass (the hole must exist), clamped to 1 .. the structure's top; returns the mass now. */
    public double debugMass(double mass) {
        if (q.hole == QuasarState.Hole.NONE) return 0;
        q.M = clampMass(mass);
        return q.M;
    }

    /** 1 .. the structure's top (§10.3); a non-finite mass is the seed's 64 SM. */
    private double clampMass(double mass) {
        if (!Double.isFinite(mass)) return QuasarParams.SEED_MASS;
        return Math.max(1, Math.min(getStructureTop(), mass));
    }

    /** Debug / bench: fill the selected gas's buffer. */
    public void debugFillBuffer() {
        final Fluid f = gasFluid(gas);
        if (f != null) buffer.put(f.getName(), bufferCap());
    }

    /**
     * Test bench: {@code seconds} disc seconds at once, as the world would run them (power assumed paid, the buffer
     * kept full when {@code fill}). Returns the last result.
     */
    public QuasarResult benchRun(int seconds, boolean fill) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        for (int i = 0; i < seconds; i++) {
            if (fill) debugFillBuffer();
            secondPowerOk = true;
            second(base);
        }
        publishDirectives();
        publishSensors();
        return last;
    }

    // ============================================================================ hosts and docks

    /**
     * Called by the conduit solver or a host once a second for every host this quasar feeds: its label, the Bu/t that
     * arrived, its σ and its link streak (0..1). Shown in the GUI's Links list and the sensor's link channels;
     * nothing is saved.
     */
    public void reportHost(String key, String label, double bu, double sigma, double streak) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null || key == null) return;
        hosts.put(
            key,
            new HostReport(
                label,
                bu,
                sigma,
                streak,
                base.getWorld()
                    .getTotalWorldTime()));
    }

    /**
     * The conduit, inside this quasar's walk: a stream from it reached a closed element (upkeep short, or no throat).
     * Directive Disc bit 4 (the alarm) holds while this was seen in the last 2 s (P4-44).
     */
    public void noteConduitClosed(String where) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return;
        conduitClosedTick = base.getWorld()
            .getTotalWorldTime();
        conduitClosedAt = where == null ? "" : where;
    }

    /** True while no stream of this quasar met a closed Conduit element in the last 2 s. */
    public boolean conduitOpen() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return true;
        return base.getWorld()
            .getTotalWorldTime() - conduitClosedTick > 40;
    }

    /** Where a stream last met a closed Conduit element ("" while the conduit is open): the GUI's warning. */
    public String conduitClosedWhere() {
        return conduitOpen() ? "" : conduitClosedAt;
    }

    /** The coil power sockets the walk found, as the GUI's checklist names them: "Energy", "Laser", both, or "". */
    public String coilPowerKinds() {
        final boolean energy = liveCount(mEnergyHatches) > 0, laser = liveCount(mExoticEnergyHatches) > 0;
        return energy && laser ? "Energy + Laser" : energy ? "Energy" : laser ? "Laser" : "";
    }

    private void pruneHosts(long now) {
        for (Iterator<HostReport> it = hosts.values()
            .iterator(); it.hasNext();) {
            if (now - it.next().tick > HOST_FRESH) it.remove();
        }
    }

    public List<HostReport> hostReports() {
        return new ArrayList<>(hosts.values());
    }

    /** The source id this quasar puts on its packets. */
    public BurstSource source() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null ? null : BurstSource.of(base);
    }

    /** Last second's Bu/t on a port (what was sent; the dock took {@link #jetTaken}). */
    public double jetBu(JetPort port) {
        return jetBu[port.ordinal()];
    }

    public double jetTaken(JetPort port) {
        return jetTaken[port.ordinal()];
    }

    /** The dock on a port as last seen, or null (venting). */
    public String jetDock(JetPort port) {
        return jetDock[port.ordinal()];
    }

    /** True when the port's dock refused the jet last second (misaligned), so it vented (P4-94). */
    public boolean jetRefused(JetPort port) {
        return jetRefused[port.ordinal()];
    }

    /** Bu/t vented last second (ports with nothing docked, or a misaligned dock). */
    public double getVented() {
        return lastVented;
    }

    // ============================================================================ automation

    private void publishDirectives() {
        for (MTEHatchDirective hatch : directiveHatches) if (live(hatch)) hatch.publish(this);
    }

    @Override
    public DirectiveSet.Host directiveHost() {
        return DirectiveSet.Host.QUASAR;
    }

    /**
     * The Disc set (quasar-p4.md §8.1), {@link QuasarDirective#disc} on last second's E and Φ; the Horizon set
     * (smbh-v1.md §6), {@link QuasarDirective#horizon}.
     */
    @Override
    public int directiveCode(DirectiveSet set, int window) {
        if (set == DirectiveSet.QUASAR_HORIZON) return QuasarDirective.horizon(directiveState());
        if (set != DirectiveSet.QUASAR_DISC) return 0;
        return QuasarDirective.disc(directiveState(), window);
    }

    public QuasarDirective.State directiveState() {
        final QuasarDirective.State s = new QuasarDirective.State();
        s.lagE = q.lagE;
        s.lagPhi = q.lagPhi;
        s.purgeAt = purgeAt;
        s.open = mMachine && q.hole == QuasarState.Hole.OPEN;
        s.b = mMachine ? q.b : 0;
        s.vented = lastVented;
        s.conduitOpen = conduitOpen();
        // SMBH: the Horizon set (last second's result; all quiet with no hole running)
        final boolean on = mMachine && running();
        s.leaking = on && last.leaking;
        s.bufferSeconds = on ? getBufferSeconds() : Double.POSITIVE_INFINITY;
        s.anchorShort = on && last.anchorShort;
        s.overdriveShort = on && last.overdriveNeed > 0 && last.overdriveCoverage < 1 - 1e-9;
        return s;
    }

    @Override
    public void directiveControl(DirectiveSet set, int strength) {}

    @Override
    public String directiveControlLine(DirectiveSet set) {
        return "";
    }

    /** One value per {@link QuasarSensorChannel}, last second. */
    public double[] sensorValues() {
        final QuasarSensorChannel[] ch = QuasarSensorChannel.VALUES;
        final double[] v = new double[ch.length];
        final double cap = getMassCap();
        final double rate = q.b * q.lc / QuasarParams.PHI_PER_B;
        double bestSigma = 0, bestStreak = 0;
        for (HostReport h : hosts.values()) {
            bestSigma = Math.max(bestSigma, h.sigma);
            bestStreak = Math.max(bestStreak, h.streak);
        }
        v[QuasarSensorChannel.E.ordinal()] = q.E;
        v[QuasarSensorChannel.PHI.ordinal()] = q.Phi;
        v[QuasarSensorChannel.LAMBDA.ordinal()] = q.lam;
        v[QuasarSensorChannel.SPIN.ordinal()] = q.a;
        v[QuasarSensorChannel.MASS.ordinal()] = q.M;
        v[QuasarSensorChannel.MASS_CAP.ordinal()] = 100 * q.M / cap;
        v[QuasarSensorChannel.G_TOTAL.ordinal()] = q.G;
        v[QuasarSensorChannel.G_TOP.ordinal()] = jetBu[JetPort.TOP.ordinal()];
        v[QuasarSensorChannel.G_BOTTOM.ordinal()] = jetBu[JetPort.BOTTOM.ordinal()];
        v[QuasarSensorChannel.ZONE.ordinal()] = q.zone;
        v[QuasarSensorChannel.INTEGRITY.ordinal()] = q.I;
        v[QuasarSensorChannel.STABILITY.ordinal()] = q.stability();
        v[QuasarSensorChannel.ERUPT_WARN.ordinal()] = q.Phi >= QuasarParams.ERUPT_WARN ? 1 : 0;
        v[QuasarSensorChannel.ERUPT_SECONDS.ordinal()] = rate > 0 ? (QuasarParams.PHI_MAX - q.Phi) / rate : 999_999;
        v[QuasarSensorChannel.LINK_SIGMA.ordinal()] = bestSigma;
        v[QuasarSensorChannel.LINK_STREAK.ordinal()] = 100 * bestStreak;
        // SMBH (smbh-v1.md §6): 0 without a lattice in force
        final QuasarResult r = last;
        if (getLatticeInForce() > 0) {
            v[QuasarSensorChannel.MASS_SAFE.ordinal()] = r.safe > 0 ? 100 * q.M / r.safe : 0;
            v[QuasarSensorChannel.MASS_TOP.ordinal()] = r.top > 0 ? 100 * q.M / r.top : 0;
            v[QuasarSensorChannel.BAND_X.ordinal()] = r.x;
            v[QuasarSensorChannel.INTAKE_DISC.ordinal()] = gasDrawn;
            v[QuasarSensorChannel.INTAKE_HOLD.ordinal()] = r.holdUpLitres;
            v[QuasarSensorChannel.INTAKE_GROWTH.ordinal()] = r.growthLitres;
            v[QuasarSensorChannel.INTAKE_CAP.ordinal()] = r.intakeCap > 0
                ? 100 * (gasDrawn + r.holdUpLitres + r.growthLitres) / r.intakeCap
                : 0;
            v[QuasarSensorChannel.HOLD_COVER.ordinal()] = 100 * r.holdUpCoverage;
            v[QuasarSensorChannel.OVERDRIVE.ordinal()] = 100 * overdriveInUse();
            v[QuasarSensorChannel.OVERDRIVE_COVER.ordinal()] = r.overdriveNeed > 0 ? 100 * r.overdriveCoverage : 0;
            v[QuasarSensorChannel.OVERDRIVE_UPKEEP.ordinal()] = r.overdriveNeed;
            v[QuasarSensorChannel.UNBOUND_N.ordinal()] = r.n;
            v[QuasarSensorChannel.ANCHOR_COVER.ordinal()] = r.anchorNeed > 0 ? 100 * r.anchorCoverage : 0;
        }
        v[QuasarSensorChannel.MASS_RATE.ordinal()] = 100 * massRatePerHour;
        final double buf = getBufferSeconds();
        v[QuasarSensorChannel.BUFFER_SECONDS.ordinal()] = Double.isInfinite(buf) ? 999_999 : buf;
        v[QuasarSensorChannel.OUTPUT_X.ordinal()] = r.outputX;
        v[QuasarSensorChannel.SHEDDING.ordinal()] = q.shed ? 1 : 0;
        v[QuasarSensorChannel.SM_LINK_READY.ordinal()] = supermassiveLinkSource() ? 1 : 0;
        return v;
    }

    private void publishSensors() {
        if (sensorHatches.isEmpty()) return;
        final double[] v = sensorValues();
        final boolean lit = q.hole == QuasarState.Hole.OPEN || q.hole == QuasarState.Hole.REFORM;
        for (MTEHatchStellarFieldSensor s : sensorHatches) if (live(s)) s.publishQuasar(v, lit);
    }

    // ============================================================================ settings (GUI, OC)

    public double getFeed() {
        return feed;
    }

    /** The GUI's / OpenComputers' feed f, clamped; a non-finite value (Lua's 0/0) is ignored. */
    public void setFeed(double f) {
        if (!Double.isFinite(f)) return;
        feed = Math.max(QuasarParams.F_MIN, Math.min(QuasarParams.F_MAX, f));
    }

    public QuasarGas getGas() {
        return gas;
    }

    public int getGasOrdinal() {
        return gas.ordinal();
    }

    public void setGasOrdinal(int o) {
        gas = QuasarGas.values()[Math.floorMod(o, QuasarGas.values().length)];
    }

    public void setGas(QuasarGas g) {
        if (g != null) gas = g;
    }

    public double getBSetting() {
        return bSetting;
    }

    /**
     * The GUI's b, 0.5 up to the coil tier (4 while the coils are unknown: unformed, or the NBT load before the first
     * walk). The coils cap it in use anyway; clamping here keeps what the GUI and OpenComputers' setField report true.
     */
    public void setBSetting(double b) {
        if (!Double.isFinite(b)) return;
        final double cap = coilTier >= 1 ? coilTier : 4;
        bSetting = Math.max(QuasarParams.B_MIN, Math.min(cap, b));
    }

    /** True while a Burst Control Hatch in FIELD mode sets b. */
    public boolean bFromHatch() {
        for (MTEHatchBurstControl h : burstHatches) {
            if (live(h) && h.getMode() == MTEHatchBurstControl.Mode.FIELD) return true;
        }
        return false;
    }

    public double getPurgeAt() {
        return purgeAt;
    }

    public void setPurgeAt(double v) {
        if (!Double.isFinite(v)) return;
        purgeAt = Math.max(0, Math.min(100, v));
    }

    public boolean isRenderEnabled() {
        return renderOn;
    }

    public void setRenderEnabled(boolean on) {
        renderOn = on;
    }

    // ============================================================================ readouts

    public QuasarState state() {
        return q;
    }

    public QuasarResult lastResult() {
        return last;
    }

    public boolean isFormed() {
        return mMachine;
    }

    /** The source end of a link, as this second's jet packets carry it: formed, hole OPEN, b &gt; 0. */
    public boolean linkable() {
        return mMachine && q.hole == QuasarState.Hole.OPEN && q.b > 0;
    }

    public int getCoilTier() {
        return Math.max(0, coilTier);
    }

    public String holeName() {
        switch (q.hole) {
            case OPEN:
                return "OPEN";
            case REFORM:
                return "RE-FORMING";
            case DORMANT:
                return "DORMANT";
            default:
                return "NONE";
        }
    }

    public int holeOrdinal() {
        return q.hole.ordinal();
    }

    public double getMass() {
        return q.hole == QuasarState.Hole.NONE ? 0 : q.M;
    }

    /** Where growth stops: the coil cap without a lattice, the mass setpoint in force with one. */
    public double getMassCap() {
        // coils unknown (unformed, or loaded and not walked yet): read as MagMatter, as getStructureTop does
        final int coils = coilTier < 1 ? QuasarParams.LATTICE_COIL : coilTier;
        return QuasarParams.massSetpoint(coils, latticeTier, unbound, massSetpoint);
    }

    /**
     * The most mass this structure holds (smbh-v1.md §10.3): the coil cap, the lattice's band top, or 2^50 SM on an
     * Unbound L4. While the coils are unknown (unformed, or loaded and not yet walked) it is the MagMatter coil cap
     * with no lattice: debug masses clamp there, and a formed structure that holds less sheds the rest.
     */
    public double getStructureTop() {
        if (coilTier < 1) return QuasarParams.structureTop(QuasarParams.LATTICE_COIL, latticeTier, unbound);
        return QuasarParams.structureTop(coilTier, latticeTier, unbound);
    }

    /** The lattice tier in force by the walk and Saturation (0-4); {@link #getLatticeBuilt} is what stands. */
    public int getLatticeTier() {
        return latticeTier;
    }

    /** The tier of the whole lattice that stands (0: none or incomplete), saturated or not. */
    public int getLatticeBuilt() {
        return latticeBuilt;
    }

    /** The scanner's lattice line ("" when there is no lattice and none is needed). */
    public String getLatticeNote() {
        return latticeNote;
    }

    /** The lattice tier in force: 0 on coils under MagMatter. */
    public int getLatticeInForce() {
        return QuasarParams.latticeInForce(Math.max(1, coilTier), latticeTier);
    }

    /** The mass setpoint as set (0 = the lattice's safe limit); {@link #getMassCap} is the one in force. */
    public double getMassSetpoint() {
        return massSetpoint;
    }

    /** GUI / OpenComputers / Burst Control "hold": the mass setpoint, SM (0 or less = the safe limit). */
    public void setMassSetpoint(double m) {
        if (!Double.isFinite(m)) return;
        massSetpoint = m <= 0 ? 0 : Math.min(QuasarParams.UNBOUND_MAX, Math.max(QuasarParams.M_REF, m));
    }

    public boolean isUnbound() {
        return unbound;
    }

    /**
     * Unbind as such (the bench, the NBT): on L4 the setpoint may pass the L4 top. Turning it off sheds an Unbound
     * hole to that top. Players go through {@link #unbind}, which checks the lattice and the config.
     */
    public void setUnbound(boolean b) {
        unbound = b;
    }

    /**
     * GUI / OpenComputers Unbind (smbh-v1.md §5.1): on needs an L4 lattice in force and {@code smbhAllowUnbound};
     * off is always allowed (an Unbound hole then sheds to 67,108,864 SM). Returns "" when done, else why not.
     */
    public String unbind(boolean on) {
        if (!on) {
            if (unbound) lastEvent = "Bound again: the hole sheds to " + fmt(QuasarParams.LATTICE_TOP[4]) + " SM";
            unbound = false;
            return "";
        }
        if (!Config.smbhAllowUnbound) return "Unbind is turned off in the config (smbhAllowUnbound)";
        if (getLatticeInForce() < QuasarParams.LATTICE_MAX) {
            return "Unbind needs an L4 (Constrained Eternity) lattice in force";
        }
        if (!unbound) lastEvent = "Unbound: the mass setpoint may pass " + fmt(QuasarParams.LATTICE_TOP[4]) + " SM";
        unbound = true;
        return "";
    }

    /**
     * GUI / OpenComputers / a SHED-mode Burst Control Hatch rising edge (smbh-v1.md §6): the mass setpoint goes to the
     * lattice's safe limit, so a hole above it sheds there at 2 % a second (always safe: no power meanwhile). Returns
     * what it did.
     */
    public String shedToSafe() {
        final int L = getLatticeInForce();
        if (L == 0) return "no lattice in force: nothing to shed";
        massSetpoint = 0;
        final double safe = QuasarParams.LATTICE_SAFE[L];
        if (q.M > safe * (1 + 1e-9)) {
            lastEvent = "Shed requested: down to the safe limit, " + fmt(safe) + " SM";
            return "shedding to the safe limit, " + fmt(safe)
                + " SM (about "
                + BTSFormat.duration(Math.round(QuasarParams.shedSeconds(q.M, safe)))
                + ")";
        }
        return "the mass setpoint is the safe limit, " + fmt(safe) + " SM";
    }

    public double getOverdrive() {
        return overdrive;
    }

    /** The Overdrive level in use (a hatch in OVERDRIVE mode overrides the GUI's), 0-1. */
    public double getOverdriveInUse() {
        return overdriveInUse();
    }

    /** Last second's net mass change, a fraction of M per hour (growth, the leak and a shed together). */
    public double getMassRatePerHour() {
        return massRatePerHour;
    }

    /** Last second's leak alone, a fraction of M per hour (0 or negative). */
    public double getLeakRatePerHour() {
        return leakRatePerHour;
    }

    /**
     * The Supermassive Link source record (smbh-v1.md §1.3; P5's T9 -> T10 milestone reads it, every stream also
     * carries it): a lattice in force, the hole at least 65,536 SM, and the quasar linkable (formed, open, b &gt; 0).
     */
    public boolean supermassiveLinkSource() {
        return getLatticeInForce() > 0 && q.M >= QuasarParams.SUPERMASSIVE_LINK_MASS && linkable();
    }

    /** The output multiplier X last second's G0 used (x1 = a maxed 4,096 SM quasar; never past x100). */
    public double getOutputX() {
        return last.outputX;
    }

    /** The Overdrive level o, 0-1 in 5 % steps (§3.2); it needs L3 and its upkeep to do anything. */
    public void setOverdrive(double o) {
        if (!Double.isFinite(o)) return;
        overdrive = Math.round(Math.max(0, Math.min(1, o)) / QuasarParams.OVERDRIVE_STEP) * QuasarParams.OVERDRIVE_STEP;
    }

    /** "spinning up" (P4-16): a below 90 % of its equilibrium at λc 1 and the field in use. */
    public boolean spinningUp() {
        return q.hole == QuasarState.Hole.OPEN
            && q.a < 0.9 * QuasarParams.aEq(Math.max(q.lc, 1e-3), Math.max(0.5, q.b));
    }

    public double getGasDrawn() {
        return gasDrawn;
    }

    public double getTrimGasUnits() {
        return trimGasUnits;
    }

    public double getTrimSpaceUnits() {
        return trimSpaceUnits;
    }

    public double getBInUse() {
        return bInUse;
    }

    public boolean isPowerOk() {
        return lastPowerOk;
    }

    public long getCoilEUt() {
        return running() && !isFrozen() ? (long) Math.ceil(QuasarParams.coilEUt(effectiveB())) : 0;
    }

    public long getEruptions() {
        return eruptions;
    }

    public long getPurges() {
        return purges;
    }

    public long getBlowOuts() {
        return blowOuts;
    }

    public String getLastEvent() {
        return lastEvent;
    }

    public String getRenderNote() {
        return renderNote;
    }

    public static String fmt(double v) {
        if (v >= 100) return String.format(Locale.ROOT, "%,.0f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** "1.2e+15"-free Bu/t, the number only: "0.81", "12.3", "4,712" ({@link BTSFormat#buNum}). */
    public static String bu(double v) {
        return BTSFormat.buNum(v);
    }

    /** A Bu/t figure with its EU/t for a scanner line: "4,898 Bu/t (4.9e16 EU/t)" ({@link BTSFormat#buEuShort}). */
    public static String buEu(double v) {
        return BTSFormat.buEuShort(v);
    }

    public static String zoneName(int zone) {
        return zone >= 0 && zone < StellarField.ZONE_NAME.length ? StellarField.ZONE_NAME[zone] : "-";
    }

    /** The scanner and the console bench. Server state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        out.add(
            "Quasar Engine" + (mMachine ? "" : ", NOT FORMED")
                + (chunksLoaded || !mMachine ? "" : ", FROZEN (a structure chunk is unloaded)")
                + ", coils T"
                + Math.max(0, coilTier)
                + " (cap "
                + fmt(getMassCap())
                + " SM)");
        if (q.hole == QuasarState.Hole.NONE) {
            out.add("No hole: put a Quasar Seed in the controller slot and press Open.");
        } else {
            out.add(
                String.format(
                    Locale.ROOT,
                    "Hole %s: M %s SM, a %.3f%s",
                    holeName(),
                    fmt(q.M),
                    q.a,
                    spinningUp() ? " (spinning up)" : ""));
            out.add(
                String.format(
                    Locale.ROOT,
                    "Disc: E %s (%s), flux %.0f, lambda %.2f, Integrity %.0f, Yq %.2f, Stability %.0f",
                    StellarField.fmtSigned(q.E),
                    zoneName(q.zone),
                    q.Phi,
                    q.lam,
                    q.I,
                    q.Yq,
                    q.stability()));
            final double bufS = getBufferSeconds();
            out.add(
                String.format(
                    Locale.ROOT,
                    "Feed %s at f %.2f: %s of %s L/s, Accretion Buffer %s L (%s); trims gas %.0f, Space %.0f",
                    gas.displayName,
                    feed,
                    fmt(gasDrawn),
                    fmt(QuasarParams.eddingtonFeed(q.M, gas)),
                    fmt(buffer(gasFluid(gas))),
                    Double.isInfinite(bufS) ? "-" : BTSFormat.duration((long) bufS),
                    trimGasUnits,
                    trimSpaceUnits));
            out.add(
                "Bursts: " + buEu(q.G)
                    + (q.M > QuasarParams.M_REF || q.oEff > 0
                        ? String.format(Locale.ROOT, ", output x%.2f", last.outputX)
                        : ""));
            out.add(
                String.format(
                    Locale.ROOT,
                    "Coils: b %.2f%s, %s EU/t%s",
                    bInUse,
                    bFromHatch() ? " (Burst Control Hatch)" : "",
                    fmt(getCoilEUt()),
                    lastPowerOk ? "" : ", POWER SHORT: b = 0"));
            // SMBH (smbh-v1.md §1.2): Saturation, then what stands of the lattice
            if (!saturated && coilTier >= QuasarParams.LATTICE_COIL) {
                out.add(
                    String.format(
                        Locale.ROOT,
                        "Saturation: %,d / %,d s at 4,096 SM with |E| <= 40 (unlocks the Supermassive Lattice)",
                        saturationStreak,
                        SATURATION_SECONDS));
            }
            if (!latticeNote.isEmpty()) out.add(latticeNote);
            // SMBH (smbh-v1.md): shown once there is a lattice, a shed or a hole past the coils
            final QuasarResult r = last;
            if (getLatticeInForce() > 0 || q.shed) {
                out.add(
                    String.format(
                        Locale.ROOT,
                        "Lattice L%d: safe %s, top %s, setpoint %s SM%s; x %.2f, output x%.2f%s",
                        getLatticeInForce(),
                        fmt(QuasarParams.safeLimit(Math.max(1, coilTier), latticeTier)),
                        fmt(QuasarParams.bandTop(Math.max(1, coilTier), latticeTier)),
                        fmt(getMassCap()),
                        unbound ? " (Unbound, n " + String.format(Locale.ROOT, "%.2f", r.n) + ")" : "",
                        r.x,
                        r.outputX,
                        q.shed ? ", SHEDDING to " + fmt(r.shedTarget) + " SM (" + shedWhy(r.shedReason) + ")" : ""));
                out.add(
                    String.format(
                        Locale.ROOT,
                        "Intake %s of %s L/s max: disc %s, hold-up %s of %s (%.0f %%%s), growth %s; mass %+.2f %% per hour",
                        fmt(gasDrawn + r.holdUpLitres + r.growthLitres),
                        fmt(r.intakeCap),
                        fmt(gasDrawn),
                        fmt(r.holdUpLitres),
                        fmt(r.holdUpNeed),
                        100 * r.holdUpCoverage,
                        r.leaking ? ", LEAKING" : "",
                        fmt(r.growthLitres),
                        100 * massRatePerHour));
                if (overdriveInUse() > 0 && getLatticeInForce() < QuasarParams.LATTICE_OVERDRIVE) {
                    out.add(
                        String.format(
                            Locale.ROOT,
                            "Overdrive %.0f %% set: idle, it needs an L3 lattice",
                            100 * overdriveInUse()));
                } else if (overdriveInUse() > 0 || r.overdriveNeed > 0) {
                    out.add(
                        String.format(
                            Locale.ROOT,
                            "Overdrive %.0f %%%s: %s L/s each of Molten MHDCSM and Molten Universium, %.0f %% paid",
                            100 * overdriveInUse(),
                            q.shed ? " (idle while shedding)" : "",
                            fmt(r.overdriveNeed),
                            100 * r.overdriveCoverage));
                }
                if (unbound) {
                    out.add(
                        String.format(
                            Locale.ROOT,
                            "Unbound n %.2f: anchor %s L/s of Primordial Matter, %.0f %% paid%s",
                            r.n,
                            fmt(r.anchorNeed),
                            100 * r.anchorCoverage,
                            r.anchorShort ? String.format(Locale.ROOT, ", SHORT: shedding to n %.2f", r.anchorPaysN)
                                : ""));
                }
            }
        }
        for (JetPort port : JetPort.VALUES) {
            final int i = port.ordinal();
            out.add(
                port.label + " jet: "
                    + buEu(jetBu[i])
                    + " -> "
                    + (jetDock[i] == null ? "venting (nothing docked)"
                        : jetRefused[i] ? "venting (" + jetDock[i] + " misaligned)"
                            : jetDock[i] + " took " + bu(jetTaken[i])));
        }
        out.add(
            "Eruptions " + eruptions
                + ", purges "
                + purges
                + ", blow-outs "
                + blowOuts
                + (lastEvent.isEmpty() ? "" : "; last: " + lastEvent));
        for (HostReport h : hosts.values()) {
            out.add(String.format(Locale.ROOT, "Link: %s, %s, sigma %.2f", h.label, buEu(h.bu), h.sigma));
        }
        if (!conduitOpen()) out.add("Conduit: " + conduitClosedAt + " (Directive Disc bit 4 on)");
        out.add(
            "Sockets: gas " + gasHatchCount()
                + ", trim "
                + trimHatchCount()
                + ", burst control "
                + burstControlCount()
                + ", sensors "
                + sensorHatchCount()
                + ", directive "
                + directiveHatchCount());
        for (MTEHatchDirective h : directiveHatches) if (live(h)) out.add("Directive: " + h.statusLine());
        for (MTEHatchBurstControl h : burstHatches) if (live(h)) out.add("Burst Control: " + h.statusLine());
        for (MTEHatchStellarFieldSensor h : sensorHatches) if (live(h)) out.add("Sensor: " + h.statusLine());
        if (!renderNote.isEmpty()) out.add(renderNote);
        return out;
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    // ============================================================================== render

    private static final int COLLAPSE_TICKS = 40;

    /** The render's GL scale at {@code m} SM: (4 + 4·log₂(M/64)/6)/1.3 (§1.6; 3.1 at 64 SM, 6.2 at 4,096 SM). */
    public static float renderScaleFor(double m) {
        final double mm = Math.max(1, m);
        return (float) ((4 + 4 * (Math.log(mm / 64.0) / Math.log(2)) / 6) / 1.3);
    }

    private int[] centrePos(IGregTechTileEntity base) {
        return QuasarGeometry.world(
            getExtendedFacing(),
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord(),
            QuasarGeometry.centreRel());
    }

    public boolean claimsRender(int x, int y, int z) {
        final int[] p = renderPos;
        return p != null && p[0] == x && p[1] == y && p[2] == z;
    }

    /**
     * Server, every tick (the world is touched only on an open/close and every 10 ticks): the render exists while the
     * quasar is formed, has a hole (open, re-forming or dormant) and both switches are on; it is placed only into air,
     * never loads a chunk, and goes with GT's collapse animation (Kerr pattern).
     */
    private void tickRender(IGregTechTileEntity base, long aTick) {
        if (mStartUpCheck >= 0) return;
        final boolean hasHole = q.hole != QuasarState.Hole.NONE;
        final boolean changed = hasHole != renderOpenSeen;
        renderOpenSeen = hasHole;
        if (collapseTimer > 0 && --collapseTimer == 0) {
            collapseTimer = -1;
            removeRender();
        }
        if (!changed && aTick % 10 != 0) return;
        final World world = base.getWorld();
        if (world == null) return;
        final int[] at = centrePos(base);
        if (renderPos != null && !Arrays.equals(renderPos, at)) {
            collapseTimer = -1;
            removeRender();
        }
        final boolean want = mMachine && hasHole && renderOn && Config.renderQuasar;
        if (!want) {
            renderNote = "";
            if (renderPos == null || collapseTimer > 0) return;
            final TileQuasarHole tile = renderTile(world, renderPos);
            if (tile == null) {
                removeRender();
            } else {
                tile.startScaleChange(false);
                collapseTimer = COLLAPSE_TICKS;
            }
            return;
        }
        TileQuasarHole tile = renderPos == null ? null : renderTile(world, renderPos);
        if (tile != null && collapseTimer > 0) {
            collapseTimer = -1;
            tile.startScaleChange(true);
        }
        if (tile == null) {
            tile = placeRender(base, world, at);
            if (tile == null) return;
        }
        if (changed || aTick % 20 == 0) pushLook(tile);
    }

    private static TileQuasarHole renderTile(World world, int[] p) {
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof TileQuasarHole ? (TileQuasarHole) te : null;
    }

    private TileQuasarHole placeRender(IGregTechTileEntity base, World world, int[] at) {
        if (BlockQuasarHole.INSTANCE == null || at[1] < 0 || at[1] > 255 || !world.blockExists(at[0], at[1], at[2])) {
            return null;
        }
        final Block block = world.getBlock(at[0], at[1], at[2]);
        final boolean fresh = block != BlockQuasarHole.INSTANCE;
        if (fresh) {
            if (!block.isAir(world, at[0], at[1], at[2])) {
                renderNote = "Render blocked at " + at[0] + ", " + at[1] + ", " + at[2] + ": clear that block";
                return null;
            }
            world.setBlock(at[0], at[1], at[2], BlockQuasarHole.INSTANCE, 0, 3);
        }
        final TileQuasarHole tile = renderTile(world, at);
        if (tile == null) return null;
        renderNote = "";
        renderPos = at;
        tile.setOwner(base.getXCoord(), base.getYCoord(), base.getZCoord());
        pushLook(tile);
        if (fresh || !tile.getScaling()) tile.startScaleChange(true);
        return tile;
    }

    private void pushLook(TileQuasarHole tile) {
        final boolean lit = q.hole == QuasarState.Hole.OPEN;
        final boolean flash = flashPending;
        flashPending = false;
        tile.setLook(
            renderScaleFor(q.M),
            (float) (q.stability() / 100.0),
            lit ? (float) q.lc : 0f,
            lit ? q.zone : -1,
            lit ? (float) q.E : 0f,
            (float) jetBu[JetPort.TOP.ordinal()],
            (float) jetBu[JetPort.BOTTOM.ordinal()],
            flash,
            (float) Math.min(1, q.eruptG / Math.max(1e-9, q.G0 * 3)));
    }

    private void removeRender() {
        final int[] p = renderPos;
        if (p == null) return;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final World world = base == null ? null : base.getWorld();
        if (world == null || world.isRemote) return;
        if (p[1] >= 0 && p[1] <= 255) {
            if (!world.blockExists(p[0], p[1], p[2])) return;
            if (world.getBlock(p[0], p[1], p[2]) == BlockQuasarHole.INSTANCE) world.setBlockToAir(p[0], p[1], p[2]);
        }
        renderPos = null;
        collapseTimer = -1;
    }

    @Override
    public void onBlockDestroyed() {
        removeRender();
        super.onBlockDestroyed();
    }

    @Override
    public void onRemoval() {
        removeRender();
        super.onRemoval();
    }

    // ===================================================================================== NBT

    /** The ONE serializer of the hole, the disc and the settings (trap 2): tile NBT and the wrenched item's NBT. */
    private void writeQuasarState(NBTTagCompound nbt) {
        final NBTTagCompound t = new NBTTagCompound();
        t.setString("hole", q.hole.name());
        t.setDouble("M", q.M);
        t.setDouble("a", q.a);
        t.setDouble("E", q.E);
        t.setDouble("Phi", q.Phi);
        t.setDouble("I", q.I);
        t.setInteger("lock", q.lock);
        t.setInteger("zone", q.zone);
        t.setDouble("Yq", q.Yq);
        t.setInteger("erupt", q.erupt);
        t.setDouble("eruptG", q.eruptG);
        t.setInteger("reform", q.reform);
        t.setDouble("lagE", q.lagE);
        t.setDouble("lagPhi", q.lagPhi);
        t.setDouble("G0", q.G0);
        t.setDouble("G", q.G);
        t.setDouble("lam", q.lam);
        t.setDouble("lc", q.lc);
        t.setDouble("fEff", q.fEff);
        t.setDouble("b", q.b);
        t.setDouble("Medd", q.Medd);
        t.setBoolean("purgeLatch", q.purgeLatch);
        t.setInteger("flash", q.flash);
        t.setBoolean("lastWasEruption", q.lastWasEruption);
        t.setBoolean("firstLight", q.firstLight);
        t.setBoolean("eddDone", q.eddDone);
        t.setBoolean("firstErupt", q.firstErupt);
        t.setInteger("edd", q.edd);
        t.setInteger("massMilestones", q.massMilestones);
        t.setInteger("rng", q.rng.state());
        t.setDouble("feed", feed);
        t.setString("gas", gas.name());
        t.setDouble("bSet", bSetting);
        t.setDouble("purgeAt", purgeAt);
        t.setBoolean("render", renderOn);
        t.setLong("eruptions", eruptions);
        t.setLong("purges", purges);
        t.setLong("blowOuts", blowOuts);
        // SMBH
        t.setDouble("oEff", q.oEff);
        t.setBoolean("shed", q.shed);
        t.setDouble("massSetpoint", massSetpoint);
        t.setBoolean("unbound", unbound);
        t.setDouble("overdrive", overdrive);
        t.setBoolean("saturated", saturated);
        t.setInteger("satStreak", saturationStreak);
        nbt.setTag("btsQuasar", t);
    }

    /** A saved double, or {@code def} if it is NaN or infinite (a save corrupted before setters checked heals). */
    private static double finite(double v, double def) {
        return Double.isFinite(v) ? v : def;
    }

    private void readQuasarState(NBTTagCompound nbt) {
        if (!nbt.hasKey("btsQuasar")) return;
        final NBTTagCompound t = nbt.getCompoundTag("btsQuasar");
        final QuasarState s = new QuasarState(FieldRng.ofState(t.getInteger("rng")));
        try {
            s.hole = QuasarState.Hole.valueOf(t.getString("hole"));
        } catch (IllegalArgumentException bad) {
            s.hole = QuasarState.Hole.NONE;
        }
        // §10.3: a saved mass is clamped to 1 .. the most any structure could hold (the coils are not walked yet);
        // once formed, a structure that holds less sheds the rest, and a dormant hole too big for it will not open
        final double savedM = t.hasKey("M") ? finite(t.getDouble("M"), QuasarParams.SEED_MASS) : QuasarParams.SEED_MASS;
        final boolean savedUnbound = t.getBoolean("unbound");
        s.M = Math.max(
            1,
            Math.min(
                savedUnbound ? QuasarParams.UNBOUND_MAX : QuasarParams.LATTICE_TOP[QuasarParams.LATTICE_MAX],
                savedM));
        s.a = finite(t.getDouble("a"), 0);
        s.E = finite(t.getDouble("E"), 0);
        s.Phi = finite(t.getDouble("Phi"), 0);
        s.I = t.hasKey("I") ? finite(t.getDouble("I"), 100) : 100;
        s.lock = t.getInteger("lock");
        s.zone = t.getInteger("zone");
        s.Yq = finite(t.getDouble("Yq"), 0);
        s.erupt = t.getInteger("erupt");
        s.eruptG = finite(t.getDouble("eruptG"), 0);
        s.reform = t.getInteger("reform");
        s.lagE = finite(t.getDouble("lagE"), 0);
        s.lagPhi = finite(t.getDouble("lagPhi"), 0);
        s.G0 = finite(t.getDouble("G0"), 0);
        s.G = finite(t.getDouble("G"), 0);
        s.lam = finite(t.getDouble("lam"), 0);
        s.lc = finite(t.getDouble("lc"), 0);
        s.fEff = finite(t.getDouble("fEff"), 0);
        s.b = finite(t.getDouble("b"), 0);
        s.Medd = finite(t.getDouble("Medd"), 0);
        s.purgeLatch = t.getBoolean("purgeLatch");
        s.flash = t.getInteger("flash");
        s.lastWasEruption = t.getBoolean("lastWasEruption");
        s.firstLight = t.getBoolean("firstLight");
        s.eddDone = t.getBoolean("eddDone");
        s.firstErupt = t.getBoolean("firstErupt");
        s.edd = t.getInteger("edd");
        s.massMilestones = t.getInteger("massMilestones");
        s.oEff = Math.max(0, Math.min(1, finite(t.getDouble("oEff"), 0)));
        s.shed = t.getBoolean("shed");
        q = s;
        unbound = savedUnbound;
        setMassSetpoint(finite(t.getDouble("massSetpoint"), 0));
        setOverdrive(finite(t.getDouble("overdrive"), 0));
        saturated = t.getBoolean("saturated");
        saturationStreak = Math.max(0, Math.min(SATURATION_SECONDS, t.getInteger("satStreak")));
        setFeed(t.hasKey("feed") ? t.getDouble("feed") : QuasarParams.F_DEFAULT);
        final QuasarGas g = QuasarGas.parse(t.getString("gas"));
        gas = g == null ? QuasarGas.HE : g;
        setBSetting(t.hasKey("bSet") ? t.getDouble("bSet") : 3);
        setPurgeAt(t.hasKey("purgeAt") ? t.getDouble("purgeAt") : QuasarParams.PURGE_AT_DEFAULT);
        renderOn = !t.hasKey("render") || t.getBoolean("render");
        eruptions = t.getLong("eruptions");
        purges = t.getLong("purges");
        blowOuts = t.getLong("blowOuts");
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeQuasarState(aNBT);
        // tile only: the gas buffer and where this quasar's render block stands
        final NBTTagCompound buf = new NBTTagCompound();
        for (Map.Entry<String, Long> e : buffer.entrySet()) buf.setLong(e.getKey(), e.getValue());
        aNBT.setTag("btsQuasarBuffer", buf);
        if (renderPos != null) aNBT.setIntArray("btsQuasarRenderPos", renderPos);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeQuasarState(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        readQuasarState(aNBT);
        buffer.clear();
        final NBTTagCompound buf = aNBT.getCompoundTag("btsQuasarBuffer");
        for (Object k : buf.func_150296_c()) {
            final String key = (String) k;
            final long v = buf.getLong(key);
            if (v > 0) buffer.put(key, v);
        }
        final int[] rp = aNBT.getIntArray("btsQuasarRenderPos");
        renderPos = rp.length == 3 ? rp : null;
        renderOpenSeen = q.hole != QuasarState.Hole.NONE;
    }

    // ============================================================================ tooltip

    private static int bom(char c) {
        return bom(PIECE, c);
    }

    private static int bom(String piece, char c) {
        try {
            return BTSShapes.count(piece, c);
        } catch (RuntimeException missing) {
            return 0;
        }
    }

    /** The quasar and its lattice together: {w, h, d}. */
    private static int[] latticeSize() {
        try {
            final int[] b = SLConsoleTest.bounds(ExtendedFacing.DEFAULT, 0, 0, 0, PIECE, PIECE_LATTICE);
            return new int[] { b[3] - b[0] + 1, b[4] - b[1] + 1, b[5] - b[2] + 1 };
        } catch (RuntimeException missing) {
            return new int[] { 0, 0, 0 };
        }
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final int[] size = BTSShapes.size(PIECE);
        final int[] latticeSize = latticeSize();
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Quasar, Gamma-Ray Burst Source")
            .addInfo("Holds its own black hole and feeds it gas. The gas builds an accretion disc that spins")
            .addInfo("  and grows the hole and drives two jets of gamma-ray bursts (Bu/t). Runs no recipes.")
            .addInfo("  1 Bu/t = 10^13 EU/t in a crown: a maxed 4,096 SM quasar's 9,796 Bu/t is 9.8e16 EU/t (98")
            .addInfo("  quadrillion EU/t), so every Bu/t in its GUI shows its EU/t beside it.")
            .addInfo("Seed it once: a Quasar Seed (Kerr T2 Densification) in the controller slot, then Open.")
            .addInfo("  The hole starts at 64 SM, spin 0, and is never lost: Collapse only makes it dormant.")
            .addSeparator()
            .addInfo("Disc: E, the Eddington balance (target 0; the zones Resonant, Stable, Strained, Critical,")
            .addInfo("  Breach on |E|), and the flux (0-100). Feed f x the Eddington feed 40 x M / k L/s")
            .addInfo("  (k: Hydrogen 0.4, Helium 0.2, Raw Star Matter 0.1). Above f 1, E runs away.")
            .addInfo("Flux charges b x lambda / 2.4 a second; at 100 it erupts (a burst spike, E -30).")
            .addInfo("  A purge (Burst Control Hatch rising edge, or the GUI) dumps it early from 20.")
            .addInfo("Bursts: G0 = lambda x a^2 x b^2 x (M/64)^2, x Yq x (0.5 + flux/100); half per jet.")
            .addInfo("Trims (plain input hatches in the 4 tank sockets, at most 10 units a second each):")
            .addInfo("  feed gas, 1,000 x M/64 L a unit: E +2; Spatially Enlarged Fluid, 100 x M/64 L: E -2.")
            .addInfo("Coils (one tier everywhere) cap b and the mass: T1 256, T2 1,024, T3 2,048, T4 4,096 SM.")
            .addInfo("Coil power: 2 A UXV x b^2 from the energy sockets. Short power: b = 0, jets stop.")
            .addInfo("Blow-out (Breach, or Integrity 0): the disc re-forms over 300 s at f 0.3 or more.")
            .addSeparator()
            .addInfo("Jets: dock a Burst Thermaliser Crown or a Wormhole Mouth on each cap's port (above the")
            .addInfo("  top cap, below the bottom one). An empty port vents its bursts.")
            .addInfo("Automation: Directive Hatch, Disc set: [1] E low: gas trim [2] E high: Space trim")
            .addInfo("  [4] flux at the purge setpoint: purge [8] alarm. Wire [4] to a Burst Control Hatch.")
            .addInfo("  Sensor Hatches read E, flux, lambda, a, M, Bu/t and more. OpenComputers: bts_quasar.")
            .addInfo("Chunk-load the whole footprint: an unloaded chunk freezes the disc and pauses its hosts.")
            .addSeparator()
            .addInfo("Supermassive Lattice (the last upgrade, in place): an armillary cage round this quasar. It")
            .addInfo("  unlocks at Saturation: 4,096 SM on MagMatter coils, |E| <= 40, for 1 hour unbroken.")
            .addInfo("  48 Containment Nodes, all one tier, set the safe limit / top: L1 Superdense Ergomatter")
            .addInfo("  16,384 / 65,536 SM, L2 White Hole Matter 65,536 / 262,144, L3 Universium 262,144 /")
            .addInfo("  4,194,304 (Overdrive), L4 Constrained Eternity 4,194,304 / 67,108,864 (Unbind).")
            .addInfo("  Past 4,096 SM power grows as the square root of mass: x2 (2.0e17 EU/t) at 16,384 SM,")
            .addInfo("  x7.9 (7.7e17) at 262,144, x31 (3.0e18 EU/t, 3 quintillion) at the L4 top.")
            .addInfo("  Intake (GUI bar): Disc (your feed f, the only part that moves E), then Hold-up (3 x band")
            .addInfo("  x x the Eddington feed: holds the mass over the safe limit), then Growth (the rest), up to")
            .addInfo("  5x the Eddington feed. Short hold-up: the extra mass leaks back (halves per hour), never")
            .addInfo("  below the safe limit. The Accretion Buffer holds 60 s of Disc + Hold-up.")
            .addInfo("  Mass setpoint (GUI, OC, Burst Control 'Hold'): growth stops there; lower it to shed (2 %/s,")
            .addInfo("  no power meanwhile). Overdrive (L3+): up to x100 (9.8e18 EU/t) for 100,000 x o^2 L/s each")
            .addInfo("  of Molten MHDCSM and Molten Universium (trim or gas sockets). Unbind (L4): each doubling")
            .addInfo("  past 67,108,864 SM halves that upkeep, for 1,024 x 4^(n^2) L/s of Primordial Matter.")
            .addInfo("  Automation: Directive 'Horizon' set, Sensor channels, OpenComputers (setMassSetpoint, ...).")
            .addInfo("  A broken lattice under a hole over 4,096 SM freezes the quasar until it is repaired.")
            .beginStructureBlock(size[0], size[1], size[2], false)
            .addController("Control box, front face of the lower wall")
            .addCasingInfoExactly("Eddington Wall Casing", bom('E'), false)
            .addCasingInfoExactly("Extreme Density Space-Bending Casing", bom('A'), false)
            .addCasingInfoExactly("Naquadah Alloy Frame Box", bom('B'), false)
            .addCasingInfoExactly("Disc Field Coil (any one tier)", bom('H'), true)
            .addCasingInfoExactly("Quantum Glass", bom('G'), false)
            .addCasingInfoExactly("Background Radiation Absorbent Casing", bom('T'), false)
            .addCasingInfoExactly("Jet Collimator Ring", bom('C'), false)
            .addCasingInfoExactly("Rim Gas Injector", bom('I'), false)
            .addCasingInfoExactly("Casimir Plate", bom('P'), false)
            .addOtherStructurePart("Input Hatch (gas feed, ME stocking OK)", "24 rim sockets, at least 1", 1)
            .addOtherStructurePart("Input Hatch, plain only (trims)", "4 wall-tank sockets", 2)
            .addOtherStructurePart("Burst Control Hatch", "4 cap-rim sockets, optional", 3)
            .addOtherStructurePart("Stellar Field Sensor Hatch", "2 control-box sockets, optional", 4)
            .addOtherStructurePart("Directive Hatch (Disc set)", "2 control-box sockets, optional", 5)
            .addEnergyHatch("Control box, 3 sockets (laser hatches for b 2+): coil power", 6)
            .addStructureInfo(
                "Optional Supermassive Lattice (" + latticeSize[0]
                    + " x "
                    + latticeSize[1]
                    + " x "
                    + latticeSize[2]
                    + " with the quasar): "
                    + String.format(Locale.ROOT, "%,d", bom(PIECE_LATTICE, 'L'))
                    + " Lattice Frame Casing, "
                    + bom(PIECE_LATTICE, 'N')
                    + " Containment Nodes (one tier)")
            .addStructureInfo("Hologram: the coil channel (or trigger stack 1-4) picks the coil tier;")
            .addStructureInfo("  the lattice channel (1-4) adds the lattice at that node tier, on MagMatter coils.")
            .addStructureInfo("No maintenance hatch needed. Builds never overwrite a hatch.")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = Casings.ExtremeDensitySpaceBendingCasing.getCasingTexture();
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

    // ===================================================================================== GUI

    /**
     * §10.3: GT's multiblock GUI open clears the owner's power-fail events, and GT's tracker throws on a machine with
     * no owner (a quasar placed by a bench kit). Such a quasar adopts the first player to open it.
     */
    @Override
    protected void onGuiOpened(EntityPlayer player) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide() && base.getOwnerUuid() == null && player != null) {
            base.setOwnerUuid(player.getUniqueID());
            base.setOwnerName(player.getCommandSenderName());
        }
        super.onGuiOpened(player);
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new MTEQuasarEngineGui(this);
    }

    /** The controller slot (Quasar Seed, Black Hole Seed) and the player inventory. */
    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Collections.singletonList(QuasarRecipeMaps.QUASAR);
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsPowerPanel() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }
}
