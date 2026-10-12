package com.nateyburr.beyondthestars.machines.directive;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverRedstoneTransmitterInternal;
import gregtech.common.covers.redstone.CoverAdvancedRedstoneTransmitterInternal;

/**
 * AUTOMATION PASS (brief item 18): the DIRECTIVE HATCH, MTE base+27 = 28127 (the id P4 reserved for it; one MTE for
 * every host, P4's Q-19 recommendation). It is GT's Grade-7 water Degasser Control Hatch pattern: once a second its
 * host tells it what it needs now as a 4-bit code ({@link DirectiveSet}), and the hatch puts that out as redstone, so
 * a machine can be run by plain redstone and logic gates, an AE2 export bus with a redstone card, GT covers, Steve's
 * Factory Manager or OpenComputers.
 * <ul>
 * <li><b>Where:</b> the Eye of Harmony's Sensor sockets ({@code n}) and the Kerr Ergosphere Engine's utility sockets
 * ({@code u}). A hatch adopts its host's first set when placed.</li>
 * <li><b>Output sets:</b> the code, one bit (15 / 0) or "any bit" ({@link DirectiveSet.Output}), optionally inverted,
 * on every side (GT's internal transmitter covers carry it, as on the Stellar Field Sensor Hatch). A comparator reads
 * the raw code.</li>
 * <li><b>Control sets:</b> the hatch emits nothing and reads the strongest redstone input (covers included); 1-15
 * sets the host's setpoint, 0 leaves it.</li>
 * <li><b>Window w</b> (1-60): the Eye's gauge band ±w, the Kerr's bolt reserve in minutes. Default 10.</li>
 * </ul>
 * Settings are saved in the tile (and survive a wrench through GT's own NBT); the code is not: the host re-pushes it,
 * and when the pushes stop (unformed and silent, unloaded) the output falls to 0 after {@link #STALE_TICKS}.
 */
public class MTEHatchDirective extends MTEHatch {

    /** A code older than this (3 seconds) no longer drives the output. */
    public static final long STALE_TICKS = 60;
    public static final int WINDOW_MIN = 1, WINDOW_MAX = 60, WINDOW_DEFAULT = 10;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_DIRECTIVE");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_DIRECTIVE_GLOW");

    // ------------------------------------------------------------------ settings (saved)
    private DirectiveSet set = DirectiveSet.EYE_TRIMS;
    private DirectiveSet.Output output = DirectiveSet.Output.CODE;
    private boolean invert;
    private int window = WINDOW_DEFAULT;

    // ------------------------------------------------------------------ live (the host re-pushes it every second)
    private DirectiveSet.Host host = DirectiveSet.Host.NONE;
    private int code;
    private int controlIn;
    private String controlLine = "";
    private final String[] labels = new String[4];
    private long lastPublish = Long.MIN_VALUE / 2;

    public MTEHatchDirective(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 11, 0, "Tells automation what the machine needs now, as redstone");
    }

    public MTEHatchDirective(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchDirective(mName, mTier, mDescriptionArray, mTextures);
    }

    /** The bit tables (task: "with the bit table in its tooltip"); docs/automation.md has the same tables. */
    @Override
    public String[] getDescription() {
        return new String[] { "Tells automation what its machine needs now, as a 4-bit redstone code (Degasser style).",
            "Goes in an Eye of Harmony Sensor socket (n) or a Kerr Ergosphere Engine utility socket (u).",
            "GUI: the set; the output (the code, one bit, any bit; invert); the window w. Comparators read the code.",
            "Codes add up: [1] [2] [4] [8] are bits 1-4, several at once. Updates every second; 0 when silent.",
            "§bEye Trims§7 (the star's trims, wanted when the gauge is past ±w):",
            "  T1, T2: [1] Helium (P > +w) [2] Hydrogen (P < -w)",
            "  T3: [1] Helium (P > +w or Θ < -w) [2] Hydrogen (P < -w) [4] Super Coolant (P or Θ > +w, a flash)",
            "  T4, T6: [1] Space (P > +w) [2] SpaceTime (P < -w) [4] Super Coolant (Θ > +w) [8] Tengam (Ω > +w)",
            "  T5: [1] DSSF (P or Θ > +w) [2] Raw Star Matter (Θ < -w) [4] Helium (P < -w)",
            "§bEye Status§7: [1] buffers short [2] ignition charge wanted [4] event warned (T3 more Coolant,",
            "  T6 Agent wanted) [8] alarm (Critical, Breach, Integrity < 50, or not formed)",
            "  Magnetar Merger: [4] asks above Π +66; feed 1 unit/s (100 L/s) while Transition [2] is on.",
            "§bEye Transition§7: [1] ready: load the next chart [2] running [4] next core wanted [8] Tachyon wanted",
            "§bKerr Hole§7: [1] closed [2] below setpoint (1 SM) [4] bolts short (< w minutes) [8] above setpoint",
            "§bKerr Status§7: [1] containment power short [2] no Black Hole Seed [4] unstable [8] not formed",
            "§bControl sets§7: redstone 1-15 sets the Kerr mass setpoint, the Eye time dial or the HSM feed",
            "  (1 = lowest, 15 = highest; 0 leaves the GUI's value). The hatch emits nothing then.",
            "Window w (1-60, default 10): the Eye's gauge band, the Kerr's bolt reserve in minutes." };
    }

    // ------------------------------------------------------------------ the host's side

    /** Called by the host once a second (server side) with itself. */
    public void publish(DirectiveHost from) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide() || from == null) return;
        lastPublish = base.getTimer();
        host = from.directiveHost();
        if (set.host != host) set = DirectiveSet.first(host);
        for (int i = 0; i < labels.length; i++) labels[i] = from.directiveBitLabel(set, i);
        if (set.control) {
            code = 0;
            controlIn = base.getStrongestRedstone();
            if (controlIn > 0) from.directiveControl(set, controlIn);
            controlLine = from.directiveControlLine(set);
        } else {
            controlIn = 0;
            controlLine = "";
            code = from.directiveCode(set, window) & 15;
        }
    }

    private boolean fresh() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.getTimer() - lastPublish <= STALE_TICKS;
    }

    /** The host's last code (0 once stale, or on a control set). */
    public int currentCode() {
        return fresh() && !set.control ? code : 0;
    }

    /** The redstone this hatch drives now on every side. */
    public int currentOutput() {
        if (set.control || !fresh()) return 0;
        final int v = output.apply(code);
        return invert ? 15 - v : v;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        final byte strength = (byte) currentOutput();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) emit(aBaseMetaTileEntity, side, strength);
        aBaseMetaTileEntity.setActive(fresh() && (set.control ? controlIn > 0 : code != 0));
    }

    /**
     * As the Stellar Field Sensor Hatch: GT's internal transmitter covers carry the signal, other covers keep theirs.
     */
    private static void emit(IGregTechTileEntity base, ForgeDirection side, byte strength) {
        final Cover cover = base.getCoverAtSide(side);
        if (cover instanceof CoverRedstoneTransmitterInternal
            || cover instanceof CoverAdvancedRedstoneTransmitterInternal) {
            base.setOutputRedstoneSignal(side, strength);
        } else {
            base.setInternalOutputRedstoneSignal(side, strength);
        }
    }

    /** A comparator against the hatch reads the raw code (0 on a control set or when stale). */
    @Override
    public byte getComparatorValue(ForgeDirection side) {
        return (byte) currentCode();
    }

    // ------------------------------------------------------------------ settings (GUI, OpenComputers, bench)

    /**
     * A setting changed (GUI, OpenComputers, bench): mark the chunk for saving. GT does that only for inventory
     * changes, so a setting changed with nothing else happening in the chunk was lost on a restart (B-71).
     */
    private void settingsChanged() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) base.markDirty();
    }

    public DirectiveSet getSet() {
        return set;
    }

    public void setSet(DirectiveSet s) {
        if (s != null) set = s;
        settingsChanged();
    }

    public int getSetOrdinal() {
        return set.ordinal();
    }

    public void setSetOrdinal(int ordinal) {
        set = DirectiveSet.VALUES[Math.floorMod(ordinal, DirectiveSet.VALUES.length)];
        settingsChanged();
    }

    /** The GUI's cycle: the next set of the host the hatch sits in (any set while it has none). */
    public void cycleSet() {
        if (host == DirectiveSet.Host.NONE) setSetOrdinal(set.ordinal() + 1);
        else set = (set.host == host ? set : DirectiveSet.first(host)).next();
        settingsChanged();
    }

    public DirectiveSet.Output getOutput() {
        return output;
    }

    public void setOutput(DirectiveSet.Output o) {
        if (o != null) output = o;
        settingsChanged();
    }

    public int getOutputOrdinal() {
        return output.ordinal();
    }

    public void setOutputOrdinal(int ordinal) {
        output = DirectiveSet.Output.of(ordinal);
        settingsChanged();
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
        settingsChanged();
    }

    public int getWindow() {
        return window;
    }

    public void setWindow(int w) {
        window = Math.max(WINDOW_MIN, Math.min(WINDOW_MAX, w));
        settingsChanged();
    }

    public DirectiveSet.Host getHost() {
        return fresh() ? host : DirectiveSet.Host.NONE;
    }

    /** The control input last read (0..15), 0 on an output set. */
    public int getControlIn() {
        return fresh() ? controlIn : 0;
    }

    /** Bit {@code i}'s label (0..3) as the host names it, or the set's default. */
    public String label(int i) {
        final String l = labels[i];
        return l == null || l.isEmpty() ? set.bit(i) : l;
    }

    /** The bits of the current code by name, e.g. "Helium, Super Coolant", or "none". */
    public String bitsLine() {
        final int c = currentCode();
        if (c == 0) return "none";
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if ((c & (1 << i)) == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(label(i));
        }
        return sb.toString();
    }

    /** One line for the GUI, the host's scanner and the bench. */
    public String statusLine() {
        if (!fresh()) return set.label + ": no machine reporting (place it in an Eye n or a Kerr u socket)";
        if (set.control) {
            return String.format(
                Locale.ROOT,
                "%s: input %d%s",
                set.label,
                controlIn,
                controlLine.isEmpty() ? "" : " -> " + controlLine);
        }
        return String.format(
            Locale.ROOT,
            "%s: code %d (%s), %s%s -> %d",
            set.label,
            currentCode(),
            bitsLine(),
            output.label,
            invert ? ", inverted" : "",
            currentOutput());
    }

    /** The GUI's short last line: what the hatch drives (or reads) now. */
    public String guiLine() {
        if (!fresh()) return "No machine reporting";
        if (set.control) return "Input " + controlIn + (controlLine.isEmpty() ? "" : ": " + controlLine);
        return "Code " + currentCode() + ", drives redstone " + currentOutput();
    }

    // ------------------------------------------------------------------ GT plumbing (as the sensor hatch)

    @Override
    public boolean isValidSlot(int aIndex) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean allowGeneralRedstoneOutput() {
        return true;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        openGui(aPlayer);
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setString("btsDirSet", set.name());
        aNBT.setInteger("btsDirOutput", output.ordinal());
        aNBT.setBoolean("btsDirInvert", invert);
        aNBT.setInteger("btsDirWindow", window);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        final DirectiveSet s = DirectiveSet.parse(aNBT.getString("btsDirSet"));
        if (s != null) set = s;
        output = DirectiveSet.Output.of(aNBT.getInteger("btsDirOutput"));
        invert = aNBT.getBoolean("btsDirInvert");
        setWindow(aNBT.hasKey("btsDirWindow") ? aNBT.getInteger("btsDirWindow") : WINDOW_DEFAULT);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY), TextureFactory.builder()
            .addIcon(OVERLAY_GLOW)
            .glow()
            .build() };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY) };
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new DirectiveHatchGui(this).build(data, syncManager, uiSettings);
    }
}
