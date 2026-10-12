package com.nateyburr.beyondthestars.machines.kerr;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofHint;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.nateyburr.beyondthestars.machines.BTSStructures.noOverwrite;
import static com.nateyburr.beyondthestars.machines.BTSStructures.tieredBlock;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.nateyburr.beyondthestars.Config;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing;
import com.nateyburr.beyondthestars.blocks.BlockSLCasing2;
import com.nateyburr.beyondthestars.blocks.SLBlocks;
import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;
import com.nateyburr.beyondthestars.machines.QuietTileUpdate;
import com.nateyburr.beyondthestars.machines.directive.DirectiveHost;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.KerrDirective;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleUtility;

/**
 * SINGULARITY LADDER P2 / KERR REDESIGN: THE KERR ERGOSPHERE ENGINE (KEE) - MTE base+14 (28114), UIV
 * (docs/kerr-redesign.md; singularity-ladder-build.md §3.1 / §3.2, D-41, D-42, D-44).
 * <p>
 * The KEE controls ONLY the hole (user ruling, brief item 9): seed / open / close, the mass setpoint,
 * the bolt governor and its bolt buses, Hawking bleed, stability, containment power, the BHC utility
 * hatches and the Density Upgrade (Kerr T2). It runs no recipes and has no machine modes; nothing a
 * player needs takes a screwdriver. Everything is in its MUI2 GUI ({@link MTEKerrEngineGui}).
 * <p>
 * The <b>modules</b> are separate machines ({@link MTEKerrModuleBase}) standing in the cage's arcs.
 * They find this KEE through the exporter's slot table ({@link KerrSlot}) and read it through
 * {@link KerrHole}; the KEE never stores them, and never needs reconfiguring to use one. The KEE's
 * module list is derived every second ({@link #slotStatus}).
 * <p>
 * Kerr T1 takes the Black Hole Compressor's slot (its recipe is removed and its uses rewritten to this
 * controller, see BTSBlackHoleSwap). Kerr T2 is this controller with the Kerr Density Upgrade: the
 * required shape becomes {@code kerr_t2} and the setpoint range rises to 512 SM.
 *
 * <h2>The hole (Q-8 / B-48, numbers PROPOSED)</h2>
 * A Black Hole Seed opens a hole at 8 SM; a Black Hole Collapser closes it (GUI buttons, or on their
 * own in a bolt bus while "Auto" is on). Every tick the hole bleeds {@code 3.2 / M^2} SM and draws
 * {@link #CONTAINMENT_EUT} EU of containment power; with the power the governor fires bolts from the
 * bolt buses to hold the setpoint, at most {@code cap} a tick (the LOWEST injector-rail tier: Mk I 4,
 * Mk II 8, Mk III 16). Without it the governor stops and the hole bleeds; below 2 SM it evaporates. GT's
 * power switch does not touch the hole (B-52). An unformed KEE freezes its hole (B-8).
 *
 * <h2>The four traps</h2>
 * <ol>
 * <li><b>Client vs server.</b> {@link #construct} reads only the trigger stack (1 = T1, 2+ = T2) and
 * hints the module slots with {@code ofHint}; the GUI reads synced values only.</li>
 * <li><b>Tile vs item NBT.</b> {@link #writeKerrState} is the ONE serializer of the persisted state,
 * called from both {@link #saveNBTData} and {@link #setItemNBT}.</li>
 * <li><b>Plant-wide hatch lists.</b> Bolt buses and utility hatches register into PRIVATE lists
 * ({@link KerrHatchElement}); the KEE has no recipe lists at all. Modules use their own standard
 * lists.</li>
 * <li><b>Element chains.</b> Every socket is {@code buildHatchAdder(...).buildAndChain(casing)}.</li>
 * </ol>
 */
public class MTEKerrEngine extends MTEExtendedPowerMultiBlockBase<MTEKerrEngine>
    implements ISurvivalConstructable, KerrHole, DirectiveHost {

    // ------------------------------------------------------------------------------- pieces
    public static final String PIECE_T1 = "kerr_t1";
    public static final String PIECE_T2 = "kerr_t2";
    /** Hint-only: where each module controller stands (the KEE's hologram marks it; nothing placed). */
    public static final String PIECE_SLOTS = "kerr_slots";
    /** The module machines' own pieces (each module checks its own; the KEE never does). */
    public static final String PIECE_COMPRESSOR = "kerr_compressor";
    public static final String PIECE_EXTRACTOR = "kerr_extractor";

    // --------------------------------------------------------------- the hole (Q-8, PROPOSED)
    /** Mass a fresh hole opens at, SM. */
    public static final double OPEN_MASS = 8.0;
    /** Below this the hole evaporates, SM. */
    public static final double MASS_FLOOR = 2.0;
    /** Hawking bleed: {@code BLEED_K / M^2} SM per tick. */
    public static final double BLEED_K = 3.2;
    /** DENSITY recipes need at least this much mass in the hole, SM. */
    public static final double DENSIFY_MIN_MASS = 128.0;
    /** Particle energy {@code E = ENERGY_K / M} GeV; speed = clamp(E / SPEED_DIVISOR, 1, MAX_SPEED). */
    public static final double ENERGY_K = 3200.0;
    public static final double SPEED_DIVISOR = 50.0;
    public static final double MAX_SPEED = 4.0;
    /** Whole-number setpoint range, SM (B-49): T1 2..64, T2 2..512. */
    public static final int SETPOINT_MIN = 2;
    public static final int SETPOINT_MAX_T1 = 64;
    public static final int SETPOINT_MAX_T2 = 512;
    /** Bolts per tick by the LOWEST injector-rail tier found in the walk (index = tier, Mk I = 1). */
    public static final int[] RAIL_CAP = { 0, 4, 8, 16 };
    /** Base parallel per voltage tier, the Black Hole Compressor's figure. */
    public static final int PARALLEL_PER_TIER = 8;
    /** Containment power while the hole is open: 1 A UEV (B-48, PROPOSED). */
    public static final long CONTAINMENT_EUT = 8_388_608L;

    /** Bolt materials and their masses in SM, lightest first. */
    private static final Materials[] BOLT_MATERIALS = { Materials.Neutronium, Materials.CosmicNeutronium,
        Materials.Infinity, Materials.TranscendentMetal, Materials.SpaceTime };
    private static final double[] BOLT_MASSES = { 1.0 / 64, 1.0 / 32, 1.0 / 16, 1.0 / 8, 1.0 / 4 };
    /** Resolved lazily (GT's items exist only after its preInit). */
    private static ItemStack[] boltStacks;

    // ------------------------------------------------------------------------------ textures
    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_GLOW");
    private static final IIconContainer OVERLAY_ACTIVE = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_ACTIVE");
    private static final IIconContainer OVERLAY_ACTIVE_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_FRONT_KERR_ENGINE_ACTIVE_GLOW");

    /** Built once, lazily; NEI's multiblock preview reads it from a background thread at load. */
    private static volatile IStructureDefinition<MTEKerrEngine> STRUCTURE_DEFINITION = null;

    /** Periodic structure re-check interval in ticks (see structureChunksLoaded). */
    private static final int RECHECK = 200;
    /** Horizontal reach that covers every Kerr piece from the controller (kerr_t2 is 92 x 89). */
    private static final int RECHECK_RADIUS = 96;

    // ---------------------------------------------------- PERSISTED (writeKerrState is the one writer)
    private boolean densityInstalled;
    private boolean holeOpen;
    private double mass;
    private int setpoint = (int) OPEN_MASS;
    /** A Seed / Collapser in a bolt bus acts on its own (automation); the GUI buttons work either way. */
    private boolean autoSeeds = true;
    /** Per-machine render switch (GUI); {@link Config#renderKerrHole} switches every KEE. */
    private boolean renderOn = true;
    /** GT's per-check tile update, kept to real changes (render QA round 3, P-1; {@link QuietTileUpdate}). */
    private final QuietTileUpdate quietTileUpdate = new QuietTileUpdate();

    // ------------------------------------------------- RENDER state (server; docs/kerr-redesign.md §5)
    /**
     * Where this KEE's render block stands, or null. Saved in the TILE NBT only, never on the wrenched
     * item (trap 2): a KEE placed somewhere else must never think it owns a render block here.
     */
    private int[] renderPos;
    /** The hole state the render last saw (an open/close is handled on the tick it happens). */
    private boolean renderHoleSeen;
    /** Ticks left of the collapse animation before the render block goes, or -1. */
    private int collapseTimer = -1;
    /** The scale and coverage last pushed to the render tile (-1: push next time). */
    private float sentScale = -1f;
    private float sentCoverage = -1f;
    /** Why there is no render although there should be one, or "". */
    private String renderNote = "";

    // ---------------------------------------------------------- WALK state (server, resetWalk)
    private int mCasing;
    private int railTierMin;
    private int railCount;
    private final ArrayList<MTEHatchInputBus> boltBuses = new ArrayList<>();
    private final ArrayList<MTEBlackHoleUtility> utilityHatches = new ArrayList<>();
    /** AUTOMATION PASS: Directive Hatches (MTE 28127) in the utility sockets. */
    private final ArrayList<MTEHatchDirective> directiveHatches = new ArrayList<>();
    /**
     * The Directive Hatches the last formed walk found. Most breaks stop the walk before it reaches the sockets, which
     * left {@link #directiveHatches} empty and the hatches silent, so the Status set's "not formed" bit never reached
     * the redstone (B-71). An unformed walk publishes to these plus any it reached (B-72); every formed walk replaces
     * them, and a removed hatch drops out by {@code live}.
     */
    private final ArrayList<MTEHatchDirective> formedDirectiveHatches = new ArrayList<>();

    // -------------------------------------------------------------------- RUN state (server)
    private int lastBoltsFired;
    private double lastBleed;
    private boolean powerOk = true;
    /** 20-tick window: mass fed, mass bled, bolts fired. */
    private double windowFed;
    private double windowBled;
    private int windowBolts;
    private int windowTicks;
    private double stability = 1.0;
    private double boltsPerTick;
    /** Refreshed every second: bolt stock, seeds, collapsers, slot lines. */
    private double boltStockSm;
    private int seeds;
    private int collapsers;
    private final String[] slotLines = new String[KerrSlot.VALUES.length];
    private int linkedModules;
    /** Migration runs once per load, after the first structure check (docs/kerr-redesign.md §7). */
    private boolean migrationPending = true;
    private String migrationNote = "";
    /**
     * PERSISTED (writeKerrState): a pre-redesign in-flight recipe's outputs, held here until the module
     * it ran on is formed and takes them (KerrMigration). Taken off GT's recipe fields at once, because
     * GT's stopMachine clears those whenever the KEE is unformed. Mode 1 = extractor, else compressor.
     */
    private ItemStack[] legacyItems;
    private FluidStack[] legacyFluids;
    private int legacyMode = -1;
    /** Where the held recipe stands (for the GUI), or "". */
    private String legacyNote = "";

    public MTEKerrEngine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEKerrEngine(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEKerrEngine(this.mName);
    }

    // =================================================================================== shape

    private static int[] off(String piece) {
        return BTSShapes.offset(piece);
    }

    @Override
    public IStructureDefinition<MTEKerrEngine> getStructureDefinition() {
        IStructureDefinition<MTEKerrEngine> def = STRUCTURE_DEFINITION;
        if (def == null) {
            synchronized (MTEKerrEngine.class) {
                def = STRUCTURE_DEFINITION;
                if (def == null) {
                    def = buildDefinition();
                    STRUCTURE_DEFINITION = def;
                }
            }
        }
        return def;
    }

    private static IStructureDefinition<MTEKerrEngine> buildDefinition() {
        final Block sl = SLBlocks.slCasing;
        final Block sl2 = SLBlocks.slCasing2;
        final int shellIndex = Casings.ExtremeDensitySpaceBendingCasing.textureId;
        return StructureDefinition.<MTEKerrEngine>builder()
            .addShape(PIECE_T1, BTSShapes.load(PIECE_T1))
            .addShape(PIECE_T2, BTSShapes.load(PIECE_T2))
            .addShape(PIECE_SLOTS, BTSShapes.load(PIECE_SLOTS))
            // ---- the cage (BHC family where a BHC block fits: the mid-run ruling)
            .addElement('A', casing(Casings.ExtremeDensitySpaceBendingCasing))
            .addElement('B', noOverwrite(onElementPass(x -> ++x.mCasing, ofFrame(Materials.NaquadahAlloy))))
            .addElement('C', block(sl, BlockSLCasing.MIRROR))
            .addElement('D', block(sl, BlockSLCasing.MIRROR_BACKING))
            .addElement('E', rails(sl))
            .addElement('F', block(sl, BlockSLCasing.QUADRUPOLE))
            .addElement('G', casing(Casings.ParticleBeamGuidancePipeCasing))
            .addElement('H', block(sl, BlockSLCasing.FIELD_COIL))
            .addElement('J', block(sl2, BlockSLCasing2.LINING))
            .addElement('K', casing(Casings.HawkingRadiationRealignmentFocus))
            .addElement('L', casing(Casings.ExtremeDensitySpaceBendingCasing))
            .addElement('M', block(sl, BlockSLCasing.BEAM_DUMP))
            .addElement('O', block(sl, BlockSLCasing.BASTION))
            .addElement('P', block(sl, BlockSLCasing.CASIMIR))
            // ---- the window band (my answer 2026-10-13, render K2; B-86): at the hole's height, one Quantum
            // Glass pane (S: the outer mirror ring's skin all round the drum; T: the governor arc's outer shell in
            // front of the hole) with the cells behind it open (' ' in the shape). A pane also takes the block it
            // replaced, so a Kerr built earlier still forms; glass comes first, so holograms and builds place glass
            .addElement('S', window(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement('T', window(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            // ---- cage sockets
            .addElement(
                'e',
                buildHatchAdder(MTEKerrEngine.class).atLeast(Energy, ExoticEnergy)
                    .casingIndex(shellIndex)
                    .hint(1)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            // The old maintenance socket: plain shell now (the KEE runs no recipes, so it has no maintenance),
            // still accepting the maintenance hatch a pre-fix Kerr has there. Casing first: builds place shell.
            .addElement(
                'm',
                ofChain(
                    casing(Casings.ExtremeDensitySpaceBendingCasing),
                    buildHatchAdder(MTEKerrEngine.class).atLeast(Maintenance)
                        .casingIndex(shellIndex)
                        .hint(2) // GT's build() requires one; never shown (the casing comes first)
                        .build()))
            .addElement(
                'b',
                buildHatchAdder(MTEKerrEngine.class).atLeast(KerrHatchElement.BoltBus)
                    .casingIndex(shellIndex)
                    .hint(3)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            .addElement(
                'u',
                buildHatchAdder(MTEKerrEngine.class).atLeast(KerrHatchElement.Utility, KerrHatchElement.Directive)
                    .casingIndex(shellIndex)
                    .hint(6)
                    .buildAndChain(casing(Casings.ExtremeDensitySpaceBendingCasing)))
            // ---- module slots: hint dots only (check always passes, nothing is placed, trap 4)
            .addElement('1', ofHint(KerrSlot.COMPRESSOR.hintDots))
            .addElement('2', ofHint(KerrSlot.EXTRACTOR.hintDots))
            .build();
    }

    private static IStructureElement<MTEKerrEngine> casing(Casings casing) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, casing.asElement()));
    }

    private static IStructureElement<MTEKerrEngine> block(Block block, int meta) {
        return noOverwrite(onElementPass(x -> ++x.mCasing, ofBlock(block, meta)));
    }

    /** A window cell: Quantum Glass, or the block it held before the window bands (old builds keep forming). */
    private static IStructureElement<MTEKerrEngine> window(IStructureElement<MTEKerrEngine> before) {
        return ofChain(casing(Casings.QuantumGlass), before);
    }

    /** The window pane chars (export-structures.js WINDOW.kerr): glass, or the old block. */
    private static final char[] WINDOW_CHARS = { 'S', 'T' };

    /** Bench ({@code /btskerr ... oldwindows}): the block each window char replaced, {@code {Block, meta}}. */
    public static Map<Character, Object[]> windowBefore() {
        final Map<Character, Object[]> m = new LinkedHashMap<>();
        final Object[] shell = { Casings.ExtremeDensitySpaceBendingCasing.getBlock(),
            Casings.ExtremeDensitySpaceBendingCasing.getBlockMeta() };
        m.put('S', shell);
        m.put('T', shell);
        return m;
    }

    private static int windowCount(String piece) {
        int n = 0;
        for (char c : WINDOW_CHARS) n += bom(piece, c);
        return n;
    }

    /** The injector rail metas, Mk I first. */
    private static final int[] RAIL_METAS = { BlockSLCasing.INJECTOR_MK1, BlockSLCasing.INJECTOR_MK2,
        BlockSLCasing.INJECTOR_MK3 };

    /**
     * The injector rails, Mk I/II/III, MIXABLE: the walk records the LOWEST tier it meets, and that
     * sets the bolt cap, so upgrading rails one at a time never un-forms the machine. Builds place Mk I
     * and never downgrade a rail already standing ({@code BTSStructures.tieredBlock}).
     */
    private static IStructureElement<MTEKerrEngine> rails(Block sl) {
        return tieredBlock(
            sl,
            RAIL_METAS,
            ofChain(
                onElementPass(x -> x.noteRail(1), ofBlock(sl, BlockSLCasing.INJECTOR_MK1)),
                onElementPass(x -> x.noteRail(2), ofBlock(sl, BlockSLCasing.INJECTOR_MK2)),
                onElementPass(x -> x.noteRail(3), ofBlock(sl, BlockSLCasing.INJECTOR_MK3))),
            trigger -> 0);
    }

    private void noteRail(int tier) {
        railCount++;
        railTierMin = railTierMin == 0 ? tier : Math.min(railTierMin, tier);
    }

    /** Horizontal facings only: the cage is 65 blocks tall and built upright. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    // ============================================================================ the walk

    /** Every field a walk writes, zeroed in one place that nothing can get past. */
    private void resetWalk() {
        mCasing = 0;
        railTierMin = 0;
        railCount = 0;
        boltBuses.clear();
        utilityHatches.clear();
        directiveHatches.clear();
    }

    /**
     * The core piece this controller must stand on right now (server: the installed state). Once the
     * Density Upgrade is in, only {@code kerr_t2} forms (round-4 ruling: "won't form").
     */
    public String corePiece() {
        return densityInstalled ? PIECE_T2 : PIECE_T1;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        resetWalk();
        final String core = corePiece();
        final int[] o = off(core);
        if (!checkPiece(core, o[0], o[1], o[2], errors)) return;
        checkExoticAndNormalEnergyHatches(errors);
        if (boltBusCount() == 0) {
            errors.add(StructureErrors.of("beyondthestars.structure.kerr.boltbus"));
        }
    }

    // ===================================================================== private hatch adders

    private static boolean live(MetaTileEntity mte) {
        return mte != null && mte.isValid();
    }

    public boolean addBoltBus(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        // An ME stocking bus shows shadow stacks the governor cannot decrement; plain buses only.
        if (mte instanceof MTEHatchInputBus && !(mte instanceof MTEHatchInputBusME)) {
            ((MTEHatchInputBus) mte).updateTexture(casingIndex);
            boltBuses.add((MTEHatchInputBus) mte);
            return true;
        }
        return false;
    }

    public boolean addUtilityHatch(IGregTechTileEntity te, int casingIndex) {
        if (te == null) return false;
        final IMetaTileEntity mte = te.getMetaTileEntity();
        if (mte instanceof MTEBlackHoleUtility) {
            ((MTEBlackHoleUtility) mte).updateTexture(casingIndex);
            utilityHatches.add((MTEBlackHoleUtility) mte);
            return true;
        }
        return false;
    }

    /** AUTOMATION PASS: a Directive Hatch in a utility socket. */
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

    public int directiveHatchCount() {
        return liveCount(directiveHatches);
    }

    /** The utility-socket Directive Hatches. Server state, read-only. */
    public List<MTEHatchDirective> directiveHatches() {
        return java.util.Collections.unmodifiableList(directiveHatches);
    }

    static int liveCount(List<? extends MetaTileEntity> list) {
        int n = 0;
        for (MetaTileEntity mte : list) {
            if (live(mte)) n++;
        }
        return n;
    }

    /** The bolt buses (bench, read-only). Server state. */
    public List<MTEHatchInputBus> boltBuses() {
        return java.util.Collections.unmodifiableList(boltBuses);
    }

    public int boltBusCount() {
        return liveCount(boltBuses);
    }

    public int utilityHatchCount() {
        return liveCount(utilityHatches);
    }

    // ==================================================================== construct / survival

    /**
     * Trap 1: the trigger stack alone picks the core (1 = T1, 2+ = T2). The module slots are hinted
     * ({@code ofHint}: no block, no tile), so no module machine ever enters the KEE's build or
     * preview; each module builds and previews its own arc.
     */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final String core = stackSize != null && stackSize.stackSize >= 2 ? PIECE_T2 : PIECE_T1;
        final int[] o = off(core);
        buildPiece(core, stackSize, hintsOnly, o[0], o[1], o[2]);
        if (hintsOnly) {
            final int[] so = off(PIECE_SLOTS);
            buildPiece(PIECE_SLOTS, stackSize, true, so[0], so[1], so[2]);
        }
    }

    /**
     * Survival auto-place (projector, NEI preview): the cage only. An upgraded Kerr builds T2, and a
     * stack of 2+ pre-builds the T2 reinforcement. -1 when it stands. Tiles are never overwritten.
     */
    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        final String core = densityInstalled || (stackSize != null && stackSize.stackSize >= 2) ? PIECE_T2 : PIECE_T1;
        if (mMachine && core.equals(corePiece())) return -1;
        final int[] o = off(core);
        return survivalBuildPiece(
            core,
            stackSize,
            o[0],
            o[1],
            o[2],
            BTSStructures.survivalBudget(elementBudget),
            env,
            false,
            true);
    }

    // ============================================================================ the upgrade

    /**
     * Right-click holding a Kerr Density Upgrade: consumes one (not in creative), installs Kerr T2
     * and re-checks the structure. Irreversible (Q-10). A plain right-click, not sneak.
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        final ItemStack held = aPlayer.getHeldItem();
        if (held != null && SLItems.kerrDensityUpgrade != null && held.getItem() == SLItems.kerrDensityUpgrade) {
            if (!aBaseMetaTileEntity.isServerSide()) return true;
            if (densityInstalled) {
                GTUtility.sendChatToPlayer(aPlayer, "This Kerr already carries the Density Upgrade.");
                return true;
            }
            if (!aPlayer.capabilities.isCreativeMode) {
                held.stackSize--;
                if (held.stackSize <= 0) {
                    aPlayer.inventory.setInventorySlotContents(aPlayer.inventory.currentItem, null);
                }
            }
            installDensity();
            checkStructure(true, aBaseMetaTileEntity);
            GTUtility.sendChatToPlayer(
                aPlayer,
                EnumChatFormatting.AQUA + "Kerr Density Upgrade installed."
                    + EnumChatFormatting.RESET
                    + " The cage now needs the T2 reinforcement (kerr_t2); setpoints up to 512 SM and DENSITY recipes are open."
                    + (mMachine ? "" : EnumChatFormatting.YELLOW + " Not formed yet: build the reinforcement."));
            return true;
        }
        return super.onRightclick(aBaseMetaTileEntity, aPlayer);
    }

    /** Installs Kerr T2. Also the creative build's hook. */
    public void installDensity() {
        densityInstalled = true;
    }

    public boolean isDensityInstalled() {
        return densityInstalled;
    }

    // ============================================================================== KerrHole

    @Override
    public boolean isFormed() {
        return mMachine;
    }

    @Override
    public boolean isHoleOpen() {
        return holeOpen;
    }

    @Override
    public double mass() {
        return holeOpen ? mass : 0;
    }

    @Override
    public boolean densityInstalled() {
        return densityInstalled;
    }

    /** The hole's parallel for a module: {@code 8 x tier(module energy) x floor(sqrt(M))}. */
    public static int parallelFor(long moduleMaxInputEu, double m) {
        final int base = PARALLEL_PER_TIER * Math.max(1, GTUtility.getTierExtended(moduleMaxInputEu));
        return base * Math.max(1, (int) Math.floor(Math.sqrt(Math.max(0, m))));
    }

    /** The hole's speed factor for a module: {@code clamp((3200 / M) / 50, 1, 4)}; 1 at no mass. */
    public static double speedFor(double m) {
        if (m <= 0) return 1.0;
        return Math.max(1.0, Math.min(MAX_SPEED, ENERGY_K / m / SPEED_DIVISOR));
    }

    // =============================================================================== the hole

    /** The KEE runs no recipes: GT's recipe loop never starts here. */
    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {}

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        final boolean chunksLoaded = aTick % 20 == 0 && structureChunksLoaded(aBaseMetaTileEntity);
        if (aTick > 0 && aTick % RECHECK == 0 && chunksLoaded) {
            checkStructure(true, aBaseMetaTileEntity);
        }
        // Migration needs only its own cells loaded (not the whole 96-block recheck radius); it waits for them.
        if (migrationPending && aTick >= 40 && aTick % 20 == 0 && KerrMigration.cellsLoaded(this)) {
            migrationPending = false;
            final String note = KerrMigration.convert(this);
            if (note != null) migrationNote = note;
        } else if (!migrationPending && aTick % 20 == 0 && hasLegacyRecipe() && KerrMigration.cellsLoaded(this)) {
            // A held legacy recipe waits for its module to form (an old arc had no energy hatch).
            KerrMigration.handOffLegacyRecipe(this, false);
        }
        if (mMachine) {
            if (autoSeeds && aTick % 20 == 0) useSeedOrCollapser();
            pulseUtilityHatches(aTick);
            tickHole();
        }
        tickRender(aBaseMetaTileEntity, aTick);
        if (aTick % 20 == 0) {
            refreshReadouts();
            publishDirectives();
            // The hole's mass, setpoint and open state change without an inventory change, and GT marks the chunk
            // for saving only on those: without this a restart brought back an old save of the hole (B-71: a
            // 64 SM hole on setpoint 64 came back as 16 SM on 16). Once a second, as the Eye's runtime does.
            aBaseMetaTileEntity.markDirty();
        }
        aBaseMetaTileEntity.setActive(holeOpen && mMachine);
    }

    /**
     * GT sends the whole controller to every nearby client after each structure check while formed; the KEE re-checks
     * every {@link #RECHECK} ticks, so only a check that changed the structure's centre (or re-formed it) sends it
     * (render QA round 3, P-1). The hole's own state travels on the render tile, and only when it moves
     * ({@link #pushLook}).
     */
    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onStructureCheckFinished(quietTileUpdate.wrap(aBaseMetaTileEntity));
        quietTileUpdate.after(this, aBaseMetaTileEntity);
    }

    /**
     * The BHC utility hatch protocol as GT 2.9.0-RC-2's MTEBlackHoleCompressor drives it: while the
     * hole is open every hatch gets cycleStart() on each 20-tick boundary and cycleMiddle() five
     * ticks later; while the hole is closed blackHoleClosed() turns every hatch off (repeated on both
     * beats, so a hatch added or reloaded lit while the hole is shut never stays on).
     */
    private void pulseUtilityHatches(long aTick) {
        final long phase = aTick % 20;
        if (phase != 0 && phase != 5) return;
        for (MTEBlackHoleUtility hatch : utilityHatches) {
            if (!live(hatch)) continue;
            if (!holeOpen) hatch.blackHoleClosed();
            else if (phase == 0) hatch.cycleStart();
            else hatch.cycleMiddle();
        }
    }

    /**
     * Most edits to the cage never reach the controller through GT's block-update walk, so a periodic
     * walk catches them. It is skipped while any chunk the structure could touch is unloaded.
     */
    private static boolean structureChunksLoaded(IGregTechTileEntity base) {
        final World world = base.getWorld();
        final int x = base.getXCoord();
        final int z = base.getZCoord();
        return world != null && world
            .checkChunksExist(x - RECHECK_RADIUS, 0, z - RECHECK_RADIUS, x + RECHECK_RADIUS, 255, z + RECHECK_RADIUS);
    }

    /**
     * Hawking bleed, containment power, then the governor. Runs every tick while formed. Without
     * containment power the governor stops and the hole bleeds. GT's power switch does NOT stop it: GT
     * switches a machine off whenever its structure breaks, so a repaired cage would otherwise bleed
     * until someone noticed (B-52). Collapse is the way to shut a hole down.
     */
    private void tickHole() {
        if (!holeOpen) {
            lastBoltsFired = 0;
            lastBleed = 0;
            powerOk = true;
            stability = 1.0;
            boltsPerTick = 0;
            windowFed = windowBled = 0;
            windowBolts = windowTicks = 0;
            return;
        }
        lastBleed = BLEED_K / (mass * mass);
        mass -= lastBleed;
        windowBled += lastBleed;
        powerOk = drainEnergyInput(CONTAINMENT_EUT);
        final double deficit = setpoint - mass;
        final double before = mass;
        lastBoltsFired = powerOk && deficit > 0 ? fireBolts(deficit, boltCap()) : 0;
        windowFed += mass - before;
        windowBolts += lastBoltsFired;
        if (++windowTicks >= 20) {
            final boolean holding = mass >= setpoint - 1e-3;
            stability = windowBled <= 0 || holding ? 1.0 : Math.min(1.0, windowFed / windowBled);
            boltsPerTick = windowBolts / (double) windowTicks;
            windowFed = windowBled = 0;
            windowBolts = windowTicks = 0;
        }
        if (mass < MASS_FLOOR) closeHole();
    }

    /** Bolts per tick the cage's rails allow (the lowest rail tier sets it). */
    public int boltCap() {
        return RAIL_CAP[Math.max(0, Math.min(RAIL_CAP.length - 1, railTierMin))];
    }

    public int railTier() {
        return railTierMin;
    }

    /** Fires up to {@code cap} bolts out of the bolt buses to cover {@code deficit} SM. */
    private int fireBolts(double deficit, int cap) {
        int fired = 0;
        double left = deficit;
        for (MTEHatchInputBus bus : boltBuses) {
            if (fired >= cap || left <= 0) break;
            if (!live(bus)) continue;
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            final int size = inv.getSizeInventory();
            for (int slot = 0; slot < size && fired < cap && left > 0; slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0) continue;
                final double boltMass = boltMass(stack);
                if (boltMass <= 0) continue;
                final int want = (int) Math.ceil(left / boltMass);
                final int n = Math.min(stack.stackSize, Math.min(cap - fired, want));
                if (n <= 0) continue;
                inv.decrStackSize(slot, n);
                fired += n;
                mass += n * boltMass;
                left -= n * boltMass;
            }
        }
        return fired;
    }

    /** The mass of one bolt in SM, or 0 if the stack is not a governor bolt. */
    public static double boltMass(ItemStack stack) {
        if (stack == null) return 0;
        ItemStack[] table = boltStacks;
        if (table == null) {
            table = new ItemStack[BOLT_MATERIALS.length];
            for (int i = 0; i < BOLT_MATERIALS.length; i++) {
                table[i] = GTOreDictUnificator.get(OrePrefixes.bolt, BOLT_MATERIALS[i], 1L);
            }
            boltStacks = table;
        }
        for (int i = 0; i < table.length; i++) {
            final ItemStack bolt = table[i];
            if (bolt != null && bolt.getItem() == stack.getItem() && bolt.getItemDamage() == stack.getItemDamage()) {
                return BOLT_MASSES[i];
            }
        }
        return 0;
    }

    /** "Auto": a Seed in a bolt bus opens a closed hole; a Collapser closes an open one. */
    private void useSeedOrCollapser() {
        if (holeOpen) {
            if (takeFromBoltBuses(ItemList.Black_Hole_Closer.get(1L))) closeHole();
        } else if (takeFromBoltBuses(ItemList.Black_Hole_Opener.get(1L))) {
            holeOpen = true;
            mass = OPEN_MASS;
        }
    }

    /** Takes one {@code wanted} out of the bolt buses; true if one was there. */
    private boolean takeFromBoltBuses(ItemStack wanted) {
        if (wanted == null) return false;
        for (MTEHatchInputBus bus : boltBuses) {
            if (!live(bus)) continue;
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null || !GTUtility.areStacksEqual(stack, wanted, true)) continue;
                inv.decrStackSize(slot, 1);
                return true;
            }
        }
        return false;
    }

    /** GUI "Open hole": needs a formed KEE, a closed hole and a Seed in a bolt bus (consumes it). */
    public boolean openHoleFromGui() {
        if (!mMachine || holeOpen) return false;
        if (!takeFromBoltBuses(ItemList.Black_Hole_Opener.get(1L))) return false;
        holeOpen = true;
        mass = OPEN_MASS;
        return true;
    }

    /** GUI "Collapse": needs an open hole and a Collapser in a bolt bus (consumes it). */
    public boolean collapseHoleFromGui() {
        if (!mMachine || !holeOpen) return false;
        if (!takeFromBoltBuses(ItemList.Black_Hole_Closer.get(1L))) return false;
        closeHole();
        return true;
    }

    private void closeHole() {
        holeOpen = false;
        mass = 0;
    }

    /** Debug/creative: open the hole at {@code m} SM without a seed. */
    public void debugOpenHole(double m) {
        holeOpen = true;
        mass = Math.max(MASS_FLOOR, m);
        setpoint = clampSetpoint((int) Math.round(mass));
    }

    /** Debug/test bench: close the hole without a collapser. */
    public void debugCloseHole() {
        closeHole();
    }

    /** Debug/creative: set the setpoint (clamped to the tier's range). */
    public void debugSetpoint(double wanted) {
        setpoint = clampSetpoint((int) Math.round(wanted));
    }

    public int maxSetpoint() {
        return densityInstalled ? SETPOINT_MAX_T2 : SETPOINT_MAX_T1;
    }

    private int clampSetpoint(int wanted) {
        return Math.max(SETPOINT_MIN, Math.min(maxSetpoint(), wanted));
    }

    public int getSetpoint() {
        return setpoint;
    }

    /** GUI setter (server side): any whole number of SM in the tier's range (B-49). */
    public void setSetpoint(int wanted) {
        setpoint = clampSetpoint(wanted);
    }

    public boolean isAutoSeeds() {
        return autoSeeds;
    }

    public void setAutoSeeds(boolean on) {
        autoSeeds = on;
    }

    /** The per-machine render switch (the render pass reads it). */
    public boolean isRenderEnabled() {
        return renderOn;
    }

    public void setRenderEnabled(boolean on) {
        renderOn = on;
    }

    // ============================================================================== render

    /** GT's collapse animation length (RenderingTileEntityBlackhole's 40-tick smootherstep). */
    private static final int COLLAPSE_TICKS = 40;
    /** The hole centre from the controller, (a, b, c): the manifest's {@code machines.kerr.centre.rel}. */
    private static volatile int[] centreRel;

    /** The hole's core scale, smallest and largest (2 SM and the T2 range above 290 SM clamp here). */
    private static final double CORE_MIN = 1.5, CORE_MAX = 6.4;
    /** The visible halo/disc reaches this many blocks per unit of core scale (render QA K1: was about 1.8). */
    public static final double HALO_PER_CORE = 1.35;
    /** The halo of GT's model, drawn at GL scale 1, reaches about 1.8 blocks from the centre (render QA round 1). */
    private static final double GT_HALO_PER_SCALE = 1.8;

    /**
     * The hole's core scale at {@code m} SM, logarithmic in mass so every doubling reads as a step (render QA
     * K1): {@code clamp(3.2 + 0.75 log2(M / 16), 1.5, 6.4)}, so 16 SM 3.2, 64 SM 4.7, 256 SM 6.2. The visible
     * halo/disc reaches {@link #HALO_PER_CORE} times that ({@link #haloRadiusFor}): 4.3, 6.3 and 8.4 blocks,
     * and at most 8.6 at the 512 SM T2 cap, which keeps the glow about 1.5 blocks inside the lowest coil ring
     * and the lining / interior ring of the cage (T1 at most 6.3 at its 64 SM cap).
     */
    public static double coreScaleFor(double m) {
        final double mm = Math.max(1e-3, m);
        return Math.max(CORE_MIN, Math.min(CORE_MAX, 3.2 + 0.75 * (Math.log(mm / 16.0) / Math.log(2))));
    }

    /** How far the hole's visible halo/disc reaches from the centre at {@code m} SM, blocks. */
    public static double haloRadiusFor(double m) {
        return HALO_PER_CORE * coreScaleFor(m);
    }

    /**
     * The GL scale {@link KerrHoleRenderer} applies around GT's model at {@code m} SM: the core scale times
     * 1.35 / 1.8 (0.75), so GT's halo, about 1.8 blocks per unit of GL scale, ends at {@link #haloRadiusFor}.
     */
    public static float renderScaleFor(double m) {
        return (float) (coreScaleFor(m) * HALO_PER_CORE / GT_HALO_PER_SCALE);
    }

    /** The hole centre in the world for this KEE's position and facing (T1 and T2 share the cell). */
    private int[] centrePos(IGregTechTileEntity base) {
        int[] rel = centreRel;
        if (rel == null) centreRel = rel = KerrSlot.centreRel();
        return KerrSlot.worldPos(getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord(), rel);
    }

    /** True when this KEE's render block stands at (x, y, z) (the render tile asks, see TileKerrHole). */
    public boolean claimsRender(int x, int y, int z) {
        final int[] p = renderPos;
        return p != null && p[0] == x && p[1] == y && p[2] == z;
    }

    /** Why the render is missing although it should show, or "". */
    public String getRenderNote() {
        return renderNote;
    }

    /**
     * Server, every tick (the world is touched only on an open/close and every 10 ticks). The render
     * exists while the cage is formed, the hole is open and both switches are on. Nothing happens until
     * GT's first structure check after a load or placement (mStartUpCheck, as the Eye's centre render):
     * mMachine is false until then, and a saved render must be adopted, not collapsed and regrown. When it
     * should go it plays GT's collapse and goes 40 ticks later; if the KEE turned, the old block goes at
     * once. It is placed only into air (or adopted where ours already stands), never over anything else,
     * and never loads a chunk.
     */
    private void tickRender(IGregTechTileEntity base, long aTick) {
        if (mStartUpCheck >= 0) return;
        final boolean holeChanged = holeOpen != renderHoleSeen;
        renderHoleSeen = holeOpen;
        if (collapseTimer > 0 && --collapseTimer == 0) {
            collapseTimer = -1;
            removeRender();
        }
        if (!holeChanged && aTick % 10 != 0) return;
        final World world = base.getWorld();
        if (world == null) return;
        final int[] at = centrePos(base);
        if (renderPos != null && !Arrays.equals(renderPos, at)) {
            collapseTimer = -1;
            removeRender();
        }
        final boolean want = mMachine && holeOpen && renderOn && Config.renderKerrHole;
        if (!want) {
            renderNote = "";
            if (renderPos == null || collapseTimer > 0) return;
            final TileKerrHole tile = renderTile(world, renderPos);
            if (tile == null) {
                removeRender();
            } else {
                tile.startScaleChange(false);
                collapseTimer = COLLAPSE_TICKS;
            }
            return;
        }
        TileKerrHole tile = renderPos == null ? null : renderTile(world, renderPos);
        if (tile != null && collapseTimer > 0) {
            collapseTimer = -1;
            tile.startScaleChange(true);
        }
        if (tile == null) {
            tile = placeRender(base, world, at);
            if (tile == null) return;
        }
        if (holeChanged || aTick % 20 == 0) pushLook(tile);
    }

    /** Our render tile at a loaded cell, or null. */
    private static TileKerrHole renderTile(World world, int[] p) {
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) return null;
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        return te instanceof TileKerrHole ? (TileKerrHole) te : null;
    }

    /** Places (or adopts) the render block at {@code at}; null if the cell is unloaded or not air. */
    private TileKerrHole placeRender(IGregTechTileEntity base, World world, int[] at) {
        if (BlockKerrHole.INSTANCE == null || at[1] < 0 || at[1] > 255 || !world.blockExists(at[0], at[1], at[2])) {
            return null;
        }
        final Block block = world.getBlock(at[0], at[1], at[2]);
        final boolean fresh = block != BlockKerrHole.INSTANCE;
        if (fresh) {
            if (!block.isAir(world, at[0], at[1], at[2])) {
                renderNote = "Render blocked at " + at[0] + ", " + at[1] + ", " + at[2] + ": clear that block";
                return null;
            }
            world.setBlock(at[0], at[1], at[2], BlockKerrHole.INSTANCE, 0, 3);
        }
        final TileKerrHole tile = renderTile(world, at);
        if (tile == null) return null;
        renderNote = "";
        renderPos = at;
        tile.setOwner(base.getXCoord(), base.getYCoord(), base.getZCoord());
        sentScale = sentCoverage = -1f;
        pushLook(tile);
        if (fresh || !tile.getScaling()) tile.startScaleChange(true);
        return tile;
    }

    /**
     * Scale by mass and GT's stability input, pushed only when the scale moved 2 % or the coverage 0.02
     * (checked every 20 ticks). GT reads an input of 0.5 and up as calm; the input is coverage / 2, so the
     * shader's stability IS the governor's coverage: full coverage is calm, less shrinks the disc, and none
     * (no containment power, no bolts) makes it spin.
     */
    private void pushLook(TileKerrHole tile) {
        final float scale = renderScaleFor(mass);
        final float coverage = (float) Math.max(0, Math.min(1, getStability()));
        if (sentScale > 0 && Math.abs(scale - sentScale) <= 0.02f * sentScale
            && sentCoverage >= 0
            && Math.abs(coverage - sentCoverage) <= 0.02f) {
            return;
        }
        sentScale = scale;
        sentCoverage = coverage;
        tile.setLook(scale, coverage);
    }

    /**
     * Removes this KEE's render block at once if its chunk is loaded. Otherwise the position is kept and
     * retried, and the render tile removes itself once it finds no KEE claiming it (TileKerrHole).
     */
    private void removeRender() {
        final int[] p = renderPos;
        if (p == null) return;
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        final World world = base == null ? null : base.getWorld();
        if (world == null || world.isRemote) return;
        if (p[1] >= 0 && p[1] <= 255) {
            if (!world.blockExists(p[0], p[1], p[2])) return;
            if (world.getBlock(p[0], p[1], p[2]) == BlockKerrHole.INSTANCE) world.setBlockToAir(p[0], p[1], p[2]);
        }
        renderPos = null;
        collapseTimer = -1;
        sentScale = sentCoverage = -1f;
    }

    /** Broken: the hole's render goes with the controller (as GT's BHC). */
    @Override
    public void onBlockDestroyed() {
        removeRender();
        super.onBlockDestroyed();
    }

    /** Any removal of the tile (a wrench too). GT does not call it on a chunk unload. */
    @Override
    public void onRemoval() {
        removeRender();
        super.onRemoval();
    }

    // ============================================================================ automation

    /** Once a second (server, formed or not): every live Directive Hatch gets this KEE's codes. */
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
        return DirectiveSet.Host.KERR;
    }

    /**
     * AUTOMATION PASS (docs/automation.md): the Kerr's Directive codes, {@link KerrDirective#code} on this second's
     * readouts (the offline rig proof uses the same function).
     */
    @Override
    public int directiveCode(DirectiveSet set, int window) {
        return KerrDirective.code(set, directiveState(), window);
    }

    /** This KEE's state as the Directive maths reads it. */
    public KerrDirective.State directiveState() {
        final KerrDirective.State s = new KerrDirective.State();
        s.formed = mMachine;
        s.open = holeOpen;
        s.powerOk = powerOk;
        s.mass = holeOpen ? mass : 0;
        s.setpoint = setpoint;
        s.boltStockSm = boltStockSm;
        s.bleedPerTick = holeOpen ? lastBleed : 0;
        s.stability = holeOpen ? stability : 1;
        s.seeds = seeds;
        return s;
    }

    /** Control set: redstone 1-15 sets the mass setpoint from 2 SM to the tier's top (64 or 512 SM). */
    @Override
    public void directiveControl(DirectiveSet set, int strength) {
        if (set != DirectiveSet.KERR_SETPOINT) return;
        final int v = (int) Math.round(DirectiveSet.controlValue(strength, SETPOINT_MIN, maxSetpoint(), 1));
        if (v != setpoint) setSetpoint(v);
    }

    @Override
    public String directiveControlLine(DirectiveSet set) {
        return set == DirectiveSet.KERR_SETPOINT ? "mass setpoint " + setpoint + " SM" : "";
    }

    /**
     * Test bench ({@code /btskerr autorig}): one second of the hole as 20 ticks run it (bleed, containment power and
     * the governor), then the readouts and the Directive push, without waiting for the world to tick the controller.
     */
    public void benchSecond() {
        if (mMachine && autoSeeds) useSeedOrCollapser();
        for (int i = 0; i < 20; i++) {
            if (mMachine) tickHole();
        }
        refreshReadouts();
        publishDirectives();
    }

    // ============================================================================ readouts

    public double getMass() {
        return mass();
    }

    public double getBleed() {
        return holeOpen ? lastBleed : 0;
    }

    public double getBoltsPerTick() {
        return boltsPerTick;
    }

    public double getStability() {
        return holeOpen ? stability : 1.0;
    }

    public boolean isPowerOk() {
        return powerOk;
    }

    public long getContainmentEut() {
        return holeOpen ? CONTAINMENT_EUT : 0;
    }

    public double getBoltStockSm() {
        return boltStockSm;
    }

    /** Seconds the bolt stock lasts at the current bleed (the governor feeds what bleeds). */
    public long getBoltStockSeconds() {
        if (!holeOpen || lastBleed <= 0) return -1;
        return (long) (boltStockSm / lastBleed / 20.0);
    }

    /** Seconds until the hole evaporates with no bolts: {@code (M^3 - 8) / 9.6} ticks. */
    public long getEvapSeconds() {
        if (!holeOpen) return 0;
        return (long) Math.max(0, (Math.pow(mass, 3) - Math.pow(MASS_FLOOR, 3)) / (3 * BLEED_K) / 20.0);
    }

    public int getSeeds() {
        return seeds;
    }

    public int getCollapsers() {
        return collapsers;
    }

    public int getLinkedModules() {
        return linkedModules;
    }

    public String getMigrationNote() {
        if (legacyNote.isEmpty()) return migrationNote;
        return migrationNote.isEmpty() ? legacyNote : migrationNote + "; " + legacyNote;
    }

    /** {@code state|parallel|speed|x, y, z} for one slot, refreshed every second (server). */
    public String slotStatus(KerrSlot slot) {
        final String s = slotLines[slot.ordinal()];
        return s == null ? "NOT BUILT|0|1|" : s;
    }

    /** Every second: bolt stock, seed and collapser counts, and the module slots. */
    private void refreshReadouts() {
        double stock = 0;
        int s = 0;
        int c = 0;
        final ItemStack seed = ItemList.Black_Hole_Opener.get(1L);
        final ItemStack collapser = ItemList.Black_Hole_Closer.get(1L);
        for (MTEHatchInputBus bus : boltBuses) {
            if (!live(bus)) continue;
            final IGregTechTileEntity inv = bus.getBaseMetaTileEntity();
            for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                final ItemStack stack = inv.getStackInSlot(slot);
                if (stack == null) continue;
                stock += boltMass(stack) * stack.stackSize;
                if (seed != null && GTUtility.areStacksEqual(stack, seed, true)) s += stack.stackSize;
                if (collapser != null && GTUtility.areStacksEqual(stack, collapser, true)) c += stack.stackSize;
            }
        }
        boltStockSm = stock;
        seeds = s;
        collapsers = c;
        int linked = 0;
        for (KerrSlot slot : KerrSlot.VALUES) {
            slotLines[slot.ordinal()] = computeSlot(slot);
            if (slotLines[slot.ordinal()].startsWith("RUNNING") || slotLines[slot.ordinal()].startsWith("IDLE")) {
                linked++;
            }
        }
        linkedModules = linked;
    }

    /** The mirror of the module's link check, from this side (docs/kerr-redesign.md §2.2). */
    private String computeSlot(KerrSlot slot) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return "NOT BUILT|0|1|";
        final int[] p = slot.modulePos(getExtendedFacing(), base.getXCoord(), base.getYCoord(), base.getZCoord());
        final String pos = p[0] + ", " + p[1] + ", " + p[2];
        if (p[1] < 0 || p[1] > 255
            || !base.getWorld()
                .blockExists(p[0], p[1], p[2])) {
            return "NOT LOADED|0|1|" + pos;
        }
        final IGregTechTileEntity te = KerrSlot.gtTileAt(base.getWorld(), p);
        if (te == null || !slot.accepts(te.getMetaTileEntity())) return "NOT BUILT|0|1|" + pos;
        final MTEKerrModuleBase<?> module = (MTEKerrModuleBase<?>) te.getMetaTileEntity();
        if (!module.mMachine) return "NOT FORMED|0|1|" + pos;
        if (module.kee() != this) return "NOT LINKED: " + module.getLink().text + "|0|1|" + pos;
        if (!mMachine) return "PAUSED: KEE not formed|0|1|" + pos;
        if (!holeOpen) return "PAUSED: hole closed|0|1|" + pos;
        final String state = te.isActive() ? "RUNNING" : "IDLE";
        return String.format(Locale.ROOT, "%s|%d|%.2f|%s", state, module.bonusParallel(), module.bonusSpeed(), pos);
    }

    // ============================================================================ migration

    /**
     * Moves a pre-redesign in-flight recipe off GT's recipe fields into the held legacy recipe. The KEE
     * runs no recipes, so anything in those fields is an old recipe. Idempotent; {@code mode} is the
     * mode it ran on (0/2 compressor, 1 extractor).
     */
    void captureLegacyRecipe(int mode) {
        final ItemStack[] items = compact(mOutputItems);
        final FluidStack[] fluids = compact(mOutputFluids);
        mOutputItems = null;
        mOutputFluids = null;
        mMaxProgresstime = 0;
        mProgresstime = 0;
        mEUt = 0;
        if (items == null && fluids == null) return;
        if (!hasLegacyRecipe()) legacyMode = mode;
        legacyItems = concat(legacyItems, items, ItemStack[]::new);
        legacyFluids = concat(legacyFluids, fluids, FluidStack[]::new);
    }

    /** The same, with the mode GT's field holds (the test bench's injected recipe), then no modes. */
    void captureLegacyRecipe() {
        captureLegacyRecipe(machineMode);
        machineMode = 0;
    }

    boolean hasLegacyRecipe() {
        return legacyItems != null || legacyFluids != null;
    }

    int legacyMode() {
        return legacyMode;
    }

    ItemStack[] legacyItems() {
        return legacyItems;
    }

    FluidStack[] legacyFluids() {
        return legacyFluids;
    }

    /** After a hand-off: the items are gone (output or dropped); {@code fluidsLeft} found no room yet. */
    void keepLegacyFluids(FluidStack[] fluidsLeft) {
        legacyItems = null;
        legacyFluids = compact(fluidsLeft);
        if (legacyFluids == null) legacyMode = -1;
    }

    void setLegacyNote(String note) {
        legacyNote = note == null ? "" : note;
    }

    /** Test bench: a pre-redesign KEE's in-flight recipe, as GT's recipe fields held it. */
    public void debugLegacyRecipe(int mode, ItemStack[] items, FluidStack[] fluids) {
        mOutputItems = items;
        mOutputFluids = fluids;
        mMaxProgresstime = 1000;
        mProgresstime = 100;
        machineMode = mode;
    }

    /** The non-null, non-empty entries, or null if none. */
    private static ItemStack[] compact(ItemStack[] in) {
        if (in == null) return null;
        final List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : in) {
            if (s != null && s.getItem() != null && s.stackSize > 0) out.add(s);
        }
        return out.isEmpty() ? null : out.toArray(new ItemStack[0]);
    }

    private static FluidStack[] compact(FluidStack[] in) {
        if (in == null) return null;
        final List<FluidStack> out = new ArrayList<>();
        for (FluidStack f : in) {
            if (f != null && f.getFluid() != null && f.amount > 0) out.add(f);
        }
        return out.isEmpty() ? null : out.toArray(new FluidStack[0]);
    }

    private static <S> S[] concat(S[] a, S[] b, IntFunction<S[]> make) {
        if (a == null) return b;
        if (b == null) return a;
        final S[] out = make.apply(a.length + b.length);
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static NBTTagCompound writeLegacy(ItemStack[] items, FluidStack[] fluids) {
        final NBTTagCompound tag = new NBTTagCompound();
        if (items != null) {
            tag.setInteger("items", items.length);
            for (int i = 0; i < items.length; i++) GTUtility.saveItem(tag, "item" + i, items[i]);
        }
        if (fluids != null) {
            tag.setInteger("fluids", fluids.length);
            for (int i = 0; i < fluids.length; i++) {
                tag.setTag("fluid" + i, fluids[i].writeToNBT(new NBTTagCompound()));
            }
        }
        return tag;
    }

    // ===================================================================================== NBT

    /** The ONE serializer of the persisted state (trap 2): tile NBT and the wrenched item's NBT. */
    private void writeKerrState(NBTTagCompound nbt) {
        if (densityInstalled) nbt.setBoolean("btsKerrDensity", true);
        if (holeOpen) {
            nbt.setBoolean("btsKerrHoleOpen", true);
            nbt.setDouble("btsKerrMass", mass);
        }
        if (setpoint != (int) OPEN_MASS) nbt.setDouble("btsKerrSetpoint", setpoint);
        nbt.setBoolean("btsKerrAutoSeeds", autoSeeds);
        nbt.setBoolean("btsKerrRender", renderOn);
        if (hasLegacyRecipe()) {
            nbt.setInteger("btsKerrLegacyMode", legacyMode);
            nbt.setTag("btsKerrLegacy", writeLegacy(legacyItems, legacyFluids));
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeKerrState(aNBT);
        // Tile only (not setItemNBT): where this KEE's render block stands, so a reload finds it again.
        if (renderPos != null) aNBT.setIntArray("btsKerrRenderPos", renderPos);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeKerrState(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        densityInstalled = aNBT.getBoolean("btsKerrDensity");
        holeOpen = aNBT.getBoolean("btsKerrHoleOpen");
        mass = holeOpen ? Math.max(MASS_FLOOR, aNBT.getDouble("btsKerrMass")) : 0;
        setpoint = aNBT.hasKey("btsKerrSetpoint") ? clampSetpoint((int) Math.round(aNBT.getDouble("btsKerrSetpoint")))
            : (int) OPEN_MASS;
        autoSeeds = !aNBT.hasKey("btsKerrAutoSeeds") || aNBT.getBoolean("btsKerrAutoSeeds");
        renderOn = !aNBT.hasKey("btsKerrRender") || aNBT.getBoolean("btsKerrRender");
        final int[] rp = aNBT.getIntArray("btsKerrRenderPos");
        renderPos = rp.length == 3 ? rp : null;
        // A reload is no open/close: an open hole's render block is adopted as it stands, not regrown.
        renderHoleSeen = holeOpen;
        // A held legacy recipe (saved by this code), then a pre-redesign one still in GT's recipe fields:
        // the running recipe's mode, else the selected mode (no modes any more). GT's fields are cleared.
        legacyItems = null;
        legacyFluids = null;
        legacyMode = -1;
        if (aNBT.hasKey("btsKerrLegacy")) {
            final NBTTagCompound tag = aNBT.getCompoundTag("btsKerrLegacy");
            final ItemStack[] items = new ItemStack[tag.getInteger("items")];
            for (int i = 0; i < items.length; i++) items[i] = GTUtility.loadItem(tag, "item" + i);
            final FluidStack[] fluids = new FluidStack[tag.getInteger("fluids")];
            for (int i = 0; i < fluids.length; i++) fluids[i] = GTUtility.loadFluid(tag, "fluid" + i);
            legacyItems = compact(items);
            legacyFluids = compact(fluids);
            legacyMode = hasLegacyRecipe() ? aNBT.getInteger("btsKerrLegacyMode") : -1;
        }
        captureLegacyRecipe(aNBT.hasKey("btsKerrBoundMode") ? aNBT.getInteger("btsKerrBoundMode") : machineMode);
        machineMode = 0;
    }

    // ============================================================================ panel/tooltip

    public static String fmt(double v) {
        if (v >= 100) return String.format(Locale.ROOT, "%,.0f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** The status lines the scanner and the creative build print. Server-side state. */
    public List<String> statusLines() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) refreshReadouts();
        final List<String> out = new ArrayList<>();
        out.add(
            "Kerr " + (densityInstalled ? "T2 (Density Upgrade installed)" : "T1") + (mMachine ? "" : ", NOT FORMED"));
        if (holeOpen) {
            out.add(
                "Hole OPEN: " + fmt(mass)
                    + " SM (setpoint "
                    + setpoint
                    + "), bleed "
                    + String.format(Locale.ROOT, "%.4f", lastBleed)
                    + " SM/t, bolts "
                    + lastBoltsFired
                    + "/"
                    + boltCap()
                    + " a tick, stability "
                    + Math.round(stability * 100)
                    + "%"
                    + (powerOk ? "" : ", CONTAINMENT POWER LOW"));
        } else {
            out.add("Hole closed: a Black Hole Seed in a bolt bus, or Open in the GUI (setpoint " + setpoint + " SM)");
        }
        out.add(
            "Rails Mk " + railTierMin
                + " ("
                + railCount
                + "), bolt buses "
                + boltBusCount()
                + ", bolt stock "
                + fmt(boltStockSm)
                + " SM, auto seeds "
                + (autoSeeds ? "on" : "off"));
        for (KerrSlot slot : KerrSlot.VALUES) {
            final String[] f = slotStatus(slot).split("\\|", -1);
            out.add(slot.label + " module: " + f[0] + (f.length > 3 && !f[3].isEmpty() ? " at (" + f[3] + ")" : ""));
        }
        for (MTEHatchDirective hatch : publishedDirectives()) {
            if (live(hatch)) out.add("Directive: " + hatch.statusLine());
        }
        if (!migrationNote.isEmpty()) out.add(migrationNote);
        if (!renderNote.isEmpty()) out.add(renderNote);
        return out;
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
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
        final int[] t1 = BTSShapes.size(PIECE_T1);
        final int[] t2 = BTSShapes.size(PIECE_T2);
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Black Hole Containment, Kerr Ergosphere Engine")
            .addInfo("Holds a black hole. Runs no recipes: the Kerr modules are separate machines in its arcs")
            .addInfo("  (Compressor: south arc; Extractor: east arc) that run while this KEE's hole is open.")
            .addInfo("Everything is in the GUI: hole state, setpoint, Open / Collapse, bolts, modules.")
            .addInfo("Right-click with a Kerr Density Upgrade for Kerr T2: heavier reinforcement, holes up")
            .addInfo("  to 512 SM, and Kerr Densification on the compressor module. Irreversible.")
            .addSeparator()
            .addInfo("A Black Hole Seed opens the hole at 8 SM; a Collapser closes it (GUI, or in a bolt")
            .addInfo("  bus with Auto on). Hawking bleed 3.2/M² SM a tick; the governor fires bolts to hold")
            .addInfo("  the setpoint: Neutronium 1/64, Cosmic Neutronium 1/32, Infinity 1/16, Transcendent 1/8,")
            .addInfo("  SpaceTime 1/4 SM, at most 4/8/16 a tick by the LOWEST injector rail (Mk I/II/III).")
            .addInfo("Containment: 8,388,608 EU/t (1 A UEV) while open. Without it the governor stops and")
            .addInfo("  the hole bleeds; below 2 SM it evaporates. The power switch does not stop the hole.")
            .addInfo("Modules get parallel 8 x tier x floor(sqrt(M)) and speed x(3200/M)/50 (1 to 4).")
            .addInfo("Optional BHC Utility Hatches on the cage emit redstone while the hole is open.")
            .addInfo("Automation: bolts, Seeds and Collapsers into the bolt buses (AE2, Factory Manager).")
            .addInfo("  Directive Hatches (utility sockets): Hole [1] closed [2] below [4] bolts short [8] above;")
            .addInfo("  Status [1] power [2] no Seed [4] unstable [8] not formed; or redstone 1-15 sets the")
            .addInfo("  setpoint. OpenComputers: an Adapter on the controller gives bts_kerr.")
            .addSeparator()
            .addInfo("Chunk-load the full footprint: the structure check fails across unloaded chunks.")
            .beginVariableStructureBlock(t1[0], t2[0], t1[1], t2[1], t1[2], t2[2], false)
            .addController("Governor arc, front centre")
            .addCasingInfoExactly(
                "Extreme Density Space-Bending Casing",
                bom(PIECE_T1, 'A') + bom(PIECE_T1, 'L'),
                false)
            .addCasingInfoExactly("Naquadah Alloy Frame Box", bom(PIECE_T1, 'B'), false)
            .addCasingInfoExactly("Superradiant Mirror Casing", bom(PIECE_T1, 'C'), false)
            .addCasingInfoExactly("Mirror Backing Casing", bom(PIECE_T1, 'D'), false)
            .addCasingInfoExactly("Bolt Injector Rail (any Mk, mixable)", bom(PIECE_T1, 'E'), true)
            .addCasingInfoExactly("Ergosphere Field Coil", bom(PIECE_T1, 'H'), false)
            .addCasingInfoExactly("Beam Focusing Quadrupole", bom(PIECE_T1, 'F'), false)
            .addCasingInfoExactly("Beam Dump", bom(PIECE_T1, 'M'), false)
            .addCasingInfoExactly("Harmonic Lining", bom(PIECE_T1, 'J'), false)
            .addCasingInfoExactly("Hawking Radiation Realignment Focus", bom(PIECE_T1, 'K'), false)
            .addCasingInfoExactly("Particle Beam Guidance Pipe Casing", bom(PIECE_T1, 'G'), false)
            .addCasingInfoExactly("Quantum Glass (window band)", windowCount(PIECE_T1), false)
            .addOtherStructurePart(
                "Kerr T2 adds",
                bom(PIECE_T2, 'O') + " Bastion Plate, " + bom(PIECE_T2, 'P') + " Casimir Plate, more lining and glass")
            .addEnergyHatch("Governor arc (2 sockets; 1 laser or up to 2 energy): containment power", 1)
            .addOtherStructurePart("Bolt bus (input bus, private)", "8 sockets on the cage, at least 1", 3)
            .addOtherStructurePart(
                "Black Hole Compressor Utility Hatch or Directive Hatch",
                "optional, beside the bolt buses",
                6)
            .addOtherStructurePart("Kerr Compressor Module controller", "a separate machine (marker)", 7)
            .addOtherStructurePart("Kerr Extractor Module controller", "a separate machine (marker)", 8)
            .addStructureInfo("Hologram: trigger stack 1 = Kerr T1, 2+ = Kerr T2. Markers 7 and 8 show where")
            .addStructureInfo("  each module controller goes; build a module with its own controller's hologram.")
            .addStructureInfo("Rails go in as Mk I; builds never overwrite hatches or downgrade rails.")
            .addStructureInfo("No maintenance hatch (an old one in the governor arc may stay).")
            .addStructureInfo(
                "Window band at the hole's height: one Quantum Glass pane (the drum's outer skin, and the")
            .addStructureInfo(
                "  governor arc in front) with the cells behind it left open. Old Kerrs keep their blocks.")
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

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new MTEKerrEngineGui(this);
    }

    /** The KEE runs no recipes, so maintenance would only shut its GUI down (as the modules and RC-2's BHC). */
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
