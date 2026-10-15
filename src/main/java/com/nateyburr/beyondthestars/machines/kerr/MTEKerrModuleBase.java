package com.nateyburr.beyondthestars.machines.kerr;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.BTSShapes;
import com.nateyburr.beyondthestars.machines.BTSStructures;

import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.RecipeCheckReason;

/**
 * KERR REDESIGN: a Kerr module, a SEPARATE multiblock machine standing in one arc of a Kerr
 * Ergosphere Engine's cage (docs/kerr-redesign.md §1-§4). Its structure is its arc; it has its own
 * controller, GUI, recipe maps and GT-standard hatches (its own energy included). Its one
 * requirement is a linked KEE with an open hole.
 *
 * <h2>Linking (pull, nothing stored)</h2>
 * The module computes the KEE controller's cell from its own facing and the exporter's slot table
 * ({@link KerrSlot#keePos}) and accepts the KEE there only if the KEE's view of the slot lands back
 * on this module, facing the slot's way. The check runs every 20 ticks and before every recipe
 * start; the result is a {@link Link} and a weak reference, never NBT, so it cannot go stale across
 * chunk unloads, and two KEEs side by side cannot be confused (each module looks at one cell).
 *
 * <h2>Pausing (B-47)</h2>
 * While the link is not {@link Link#LINKED} the module skips {@link #runMachine}: no progress, no
 * EU, no new recipe, and a running recipe is KEPT and resumes when the hole reopens. Unlike
 * Godforge's modules it still walks its own structure, so it forms (and says why it is waiting)
 * before any KEE exists.
 *
 * <h2>The hole's bonus (B-8 numbers)</h2>
 * Parallel {@code 8 x tier(module input, V x A) x floor(sqrt(M))}, speed {@code clamp((3200 / M) / 50, 1,
 * 4)}, both fixed when a recipe starts (GT's suppliers are read at recipe start), so a mass change
 * mid-recipe changes nothing. Modules cost the hole nothing (B-50).
 */
public abstract class MTEKerrModuleBase<T extends MTEKerrModuleBase<T>> extends MTEExtendedPowerMultiBlockBase<T>
    implements ISurvivalConstructable {

    /** Why a module is (not) running. Server-side; the GUI gets the ordinal. */
    public enum Link {

        LINKED("linked", "Linked"),
        HOLE_CLOSED("holeclosed", "Hole closed: the KEE has no open hole"),
        KEE_NOT_FORMED("keenotformed", "The Kerr Ergosphere Engine is not formed"),
        KEE_NOT_LOADED("keenotloaded", "The Kerr Ergosphere Engine's chunk is not loaded"),
        WRONG_FACING("wrongfacing", "The KEE there faces another way: this slot is not one of its arcs"),
        NO_KEE("nokee", "No Kerr Ergosphere Engine at this arc's slot");

        public static final Link[] VALUES = values();

        /** Lang suffix: {@code GT5U.gui.text.recipe_result.beyondthestars.kerr.module.<key>}. */
        public final String key;
        /** English fallback (scanner, chat). */
        public final String text;

        Link(String key, String text) {
            this.key = key;
            this.text = text;
        }

        public static Link of(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NO_KEE;
        }

        /** True when a KEE stands at the slot and is ours (formed or not, hole open or not). */
        public boolean keePresent() {
            return this == LINKED || this == HOLE_CLOSED || this == KEE_NOT_FORMED;
        }
    }

    /** Link re-check interval, ticks (plus one before every recipe start). */
    private static final int LINK_INTERVAL = 20;

    // ------------------------------------------------------------------- link (server, never saved)
    private Link link = Link.NO_KEE;
    private WeakReference<MTEKerrEngine> keeRef = new WeakReference<>(null);
    private int keeX;
    private int keeY;
    private int keeZ;
    private boolean keeKnown;

    // --------------------------------------------- the bonus the running recipe started with (server)
    private int runParallel = 1;
    private double runSpeed = 1.0;
    /** {@link #gateState()} at the last 20-tick check (server, never saved). */
    private int gateWas;

    protected MTEKerrModuleBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected MTEKerrModuleBase(String aName) {
        super(aName);
    }

    /** The cage slot this module stands in. */
    public abstract KerrSlot slot();

    /** The module's controller casing (the arc's shell). */
    protected abstract ITexture casingTexture();

    protected abstract IIconContainer[] overlays();

    // ===================================================================================== link

    /** Recomputes the link from the world. Server side; never loads a chunk. */
    public Link refreshLink() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getWorld() == null) return setLink(Link.NO_KEE, null);
        final World world = base.getWorld();
        final int x = base.getXCoord();
        final int y = base.getYCoord();
        final int z = base.getZCoord();
        final int[] p = slot().keePos(getExtendedFacing(), x, y, z);
        keeX = p[0];
        keeY = p[1];
        keeZ = p[2];
        keeKnown = true;
        if (p[1] < 0 || p[1] > 255 || !world.blockExists(p[0], p[1], p[2])) {
            return setLink(Link.KEE_NOT_LOADED, null);
        }
        final IGregTechTileEntity te = KerrSlot.gtTileAt(world, p);
        if (te == null || !(te.getMetaTileEntity() instanceof MTEKerrEngine)) return setLink(Link.NO_KEE, null);
        final MTEKerrEngine kee = (MTEKerrEngine) te.getMetaTileEntity();
        // The KEE's own view of this slot must land back on this module, facing the slot's way.
        final int[] back = slot().modulePos(kee.getExtendedFacing(), p[0], p[1], p[2]);
        if (back[0] != x || back[1] != y
            || back[2] != z
            || slot().moduleFacing(kee.getExtendedFacing()) != base.getFrontFacing()) {
            return setLink(Link.WRONG_FACING, null);
        }
        if (!kee.isFormed()) return setLink(Link.KEE_NOT_FORMED, kee);
        if (!kee.isHoleOpen()) return setLink(Link.HOLE_CLOSED, kee);
        return setLink(Link.LINKED, kee);
    }

    /**
     * GT 5.09.54 checks recipes only when something pushes a check (doPeriodicChecks is false), and a
     * check that failed because the hole was closed used up the pending push. So the module pushes one
     * itself when the link comes back, or an idle module with stocked inputs would never restart.
     */
    private Link setLink(Link state, MTEKerrEngine kee) {
        if (state == Link.LINKED && link != Link.LINKED) scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
        link = state;
        if (keeRef.get() != kee) keeRef = new WeakReference<>(kee);
        return state;
    }

    public Link getLink() {
        return link;
    }

    /** The KEE this module resolved, or null. May be unformed or closed; see {@link #getLink()}. */
    @Nullable
    public MTEKerrEngine kee() {
        final MTEKerrEngine kee = keeRef.get();
        return kee != null && kee.isValid() ? kee : null;
    }

    /** "x, y, z" of the KEE cell this module looks at, or "" before the first check. */
    public String keePosText() {
        return keeKnown ? keeX + ", " + keeY + ", " + keeZ : "";
    }

    // ============================================================================== the bonus

    /**
     * Parallel this module would start a recipe with now (the hole's mass, this module's energy). The mass counts up to
     * the KEE's setpoint cap in force (P4-80): a T2 hole grown to 768 SM on a quasar link gives its +23 % parallel only
     * while the link holds, not for the ~460 h it takes the Hawking bleed to bring it back to 512 SM.
     */
    public int bonusParallel() {
        final MTEKerrEngine kee = kee();
        return MTEKerrEngine.parallelFor(
            getMaxInputEu(),
            kee == null || !kee.isHoleOpen() ? 0 : Math.min(kee.mass(), kee.maxSetpoint()));
    }

    /** Speed factor this module would start a recipe with now. */
    public double bonusSpeed() {
        final MTEKerrEngine kee = kee();
        return MTEKerrEngine.speedFor(kee == null || !kee.isHoleOpen() ? 0 : kee.mass());
    }

    public double linkedMass() {
        final MTEKerrEngine kee = kee();
        return kee == null || !kee.isHoleOpen() ? 0 : kee.mass();
    }

    // ================================================================================ ticking

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide() && aTick % LINK_INTERVAL == 0) {
            refreshLink();
            // A recipe gate opening (the compressor's DENSITY, P4's 256 SM and quasar link) is a change GT is not told
            // about either: any gate bit that comes on pushes a check.
            final int gate = gateState();
            if ((gate & ~gateWas) != 0) scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
            gateWas = gate;
            // A module's only state is its run (progress, the run's link need), and a long run (the Superdense
            // refinery) advances with no inventory change; GT marks a chunk for saving only on those (B-112, P4-85).
            if (mMaxProgresstime > 0) aBaseMetaTileEntity.markDirty();
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && (link != Link.LINKED || !runConditionHolds())
            && mMaxProgresstime > 0) {
            aBaseMetaTileEntity.setActive(false);
        }
    }

    /**
     * Paused while unlinked, or while the running recipe's own condition does not hold (P4: the Superdense refinery
     * loses its quasar link): no progress, no EU, no new recipe; a running recipe is kept, never voided (B-47,
     * quasar-p4.md §3.1).
     */
    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (link != Link.LINKED) return;
        if (mMaxProgresstime > 0 && !runConditionHolds()) return;
        super.runMachine(aBaseMetaTileEntity, aTick);
    }

    /**
     * True while the running recipe may progress (checked every tick it runs). Default: always. The compressor's
     * Superdense refinery needs the KEE's quasar link at σ ≥ 1.
     */
    protected boolean runConditionHolds() {
        return true;
    }

    /**
     * The recipe gates as bits, polled every 20 ticks; a bit that comes on pushes a recipe check. Default: bit 0 =
     * {@link #recipeGateOpen()}.
     */
    protected int gateState() {
        return recipeGateOpen() ? 1 : 0;
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        if (refreshLink() != Link.LINKED) {
            return SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module." + link.key);
        }
        runParallel = bonusParallel();
        runSpeed = bonusSpeed();
        return super.checkProcessing();
    }

    /**
     * True while this module's recipe gate (see {@link #gateRecipe}) would let its gated recipes run.
     * Polled every 20 ticks; a false-to-true change pushes a recipe check. No gate: always true.
     */
    protected boolean recipeGateOpen() {
        return true;
    }

    /** A recipe-specific gate (DENSITY on the compressor), or null to let GT decide. */
    @Nullable
    protected CheckRecipeResult gateRecipe(@Nonnull GTRecipe recipe) {
        return null;
    }

    /**
     * True if a match in {@code map} hides every map searched after it: the inputs are meant for that
     * map, so a shut gate reports its reason instead of letting a later map eat the inputs. None by
     * default.
     */
    protected boolean shadowsLaterMaps(@Nonnull RecipeMap<?> map) {
        return false;
    }

    /**
     * Every map in {@link #getAvailableRecipeMaps()} is searched together, in order: no machine modes.
     * GT tests the last recipe ahead of a map's own recipes whatever map it came from (and never clears
     * it, as getRecipeMap() never changes), so it is offered only to its own map.
     */
    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
                Stream<GTRecipe> all = Stream.empty();
                for (RecipeMap<?> m : getAvailableRecipeMaps()) {
                    final GTRecipe cached = lastRecipe != null && lastRecipe.getRecipeCategory() != null
                        && lastRecipe.getRecipeCategory().recipeMap == m ? lastRecipe : null;
                    final Stream<GTRecipe> found = m.findRecipeQuery()
                        .caching(recipeCaching)
                        .items(inputItems)
                        .fluids(inputFluids)
                        .specialSlot(specialSlotItem)
                        .cachedRecipe(cached)
                        .findAll();
                    if (!shadowsLaterMaps(m)) {
                        all = Stream.concat(all, found);
                        continue;
                    }
                    final List<GTRecipe> matched = found.collect(Collectors.toList());
                    if (!matched.isEmpty()) return Stream.concat(all, matched.stream());
                }
                return all;
            }

            @Nonnull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                final CheckRecipeResult gate = gateRecipe(recipe);
                return gate != null ? gate : super.validateRecipe(recipe);
            }
        }.setMaxParallelSupplier(() -> runParallel)
            .setSpeedBonusSupplier(() -> 1.0 / runSpeed);
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    /** GUI cleanup (controller-gui-spec.md §4.4): the kit's badge says it; GT's "Running perfectly." is not added. */
    @Override
    public boolean hasRunningText() {
        return false;
    }

    /**
     * GUI cleanup (spec §5.3): GT's recipe-result line stays for real recipe results (no recipe, outputs full, the
     * DENSITY gate), but not for the link results: while unlinked or paused the badge and the "Runs again when"
     * checklist say it. Read on the client, from the synced result.
     */
    @Override
    public boolean shouldDisplayCheckRecipeResult() {
        final CheckRecipeResult r = getCheckRecipeResult();
        if (r == null) return true;
        // getID() is the result TYPE ("simple_result" for every SimpleCheckRecipeResult); equals compares the key.
        for (Link l : Link.VALUES) {
            if (l != Link.LINKED && SimpleCheckRecipeResult.ofFailure("beyondthestars.kerr.module." + l.key)
                .equals(r)) return false;
        }
        return true;
    }

    // ============================================================================ structure

    /** Horizontal facings only, as the cage. */
    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && f == Flip.NONE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        final String piece = slot().piece;
        final int[] o = BTSShapes.offset(piece);
        if (!checkPiece(piece, o[0], o[1], o[2], errors)) return;
        checkExoticAndNormalEnergyHatches(errors);
        if (mInputBusses.isEmpty() && mInputHatches.isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.kerr.moduleinput"));
        }
        if (mOutputBusses.isEmpty() && mOutputHatches.isEmpty()) {
            errors.add(StructureErrors.of("beyondthestars.structure.kerr.moduleoutput"));
        }
    }

    /** One tier: the module's own arc. Reads only the stack (trap 1). */
    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        final int[] o = BTSShapes.offset(slot().piece);
        buildPiece(slot().piece, stackSize, hintsOnly, o[0], o[1], o[2]);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        final int[] o = BTSShapes.offset(slot().piece);
        return survivalBuildPiece(
            slot().piece,
            stackSize,
            o[0],
            o[1],
            o[2],
            BTSStructures.survivalBudget(elementBudget),
            env,
            false,
            true);
    }

    // ============================================================================ migration

    /**
     * Takes a pre-redesign KEE's in-flight recipe outputs (docs/kerr-redesign.md §7.3). Call only while
     * formed. Items go into the output buses; what does not fit drops at the controller. Returns the
     * fluids that found no room in the output hatches (the KEE keeps them and retries).
     */
    public FluidStack[] acceptLegacyOutputs(ItemStack[] items, FluidStack[] fluids) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack == null || stack.stackSize <= 0) continue;
                final ItemStack copy = stack.copy();
                addOutputPartial(copy); // ejects what fits and subtracts it from copy
                if (copy.stackSize <= 0 || base == null || base.getWorld() == null) continue;
                base.getWorld()
                    .spawnEntityInWorld(
                        new EntityItem(
                            base.getWorld(),
                            base.getXCoord() + 0.5,
                            base.getYCoord() + 1.5,
                            base.getZCoord() + 0.5,
                            copy));
            }
        }
        final List<FluidStack> left = new ArrayList<>();
        if (fluids != null) {
            for (FluidStack fluid : fluids) {
                if (fluid == null || fluid.amount <= 0) continue;
                if (!addOutputAtomic(fluid.copy())) left.add(fluid);
            }
        }
        return left.toArray(new FluidStack[0]);
    }

    // ================================================================================== GUI

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new MTEKerrModuleGui(this);
    }

    /** "Kerr link: KEE at x, y, z, hole OPEN 16.0 SM" or the reason it is not running. */
    public static String linkLine(Link link, String keePos, double mass) {
        final String where = keePos == null || keePos.isEmpty() ? "" : " (KEE cell " + keePos + ")";
        switch (link) {
            case LINKED:
                return "§aKerr link: KEE at " + keePos + ", hole OPEN " + MTEKerrEngine.fmt(mass) + " SM";
            case HOLE_CLOSED:
                return "§eKerr link: KEE at " + keePos + ", hole CLOSED - paused until it opens";
            default:
                return "§cNot linked: " + link.text + where;
        }
    }

    /** "Hole bonus: parallel 384, speed x4.00". */
    public static String bonusLine(Link link, int parallel, double speed) {
        if (link != Link.LINKED) return "§7Hole bonus: none while the hole is closed or unlinked";
        return String.format(Locale.ROOT, "§bHole bonus: parallel %,d, speed x%.2f", parallel, speed);
    }

    /**
     * A recipe gate's live status for the GUI (the compressor's DENSITY needs Kerr T2 and 128 SM), or
     * "" for none. Server state: the GUI syncs it.
     */
    public String gateLine() {
        return "";
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(linkLine(link, keePosText(), linkedMass()));
        info.add(bonusLine(link, bonusParallel(), bonusSpeed()));
        final String gate = gateLine();
        if (!gate.isEmpty()) info.add(gate);
        return info.toArray(new String[0]);
    }

    // ============================================================================= textures

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        final ITexture base = casingTexture();
        if (side != aFacing) return new ITexture[] { base };
        final IIconContainer[] o = overlays(); // { idle, idle glow, active, active glow }
        return new ITexture[] { base, TextureFactory.builder()
            .addIcon(aActive ? o[2] : o[0])
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(aActive ? o[3] : o[1])
                .extFacing()
                .glow()
                .build() };
    }
}
