package com.nateyburr.beyondthestars.machines.eye;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.quasar.QuasarSensorChannel;

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
 * D-49: the Stellar Field Sensor Hatch (MTE base+16 = 28116, d49-build-plan.md §12). It sits in an
 * Eye's {@code n} socket and turns one channel of the stability field into redstone, so a player can
 * hold a star with plain GT/AE2 parts: sensor past its threshold, then a Fluid Regulator cover or an
 * export bus with a redstone card, then a plain input hatch in an {@code f} (Field) socket.
 * <p>
 * The Eye pushes a {@link FieldReadout} every field second through {@link #publish(FieldReadout)}. The
 * hatch keeps nothing else of the Eye: when the pushes stop (the Eye unformed, the chunk unloaded,
 * the star dark) the output falls to 0 after {@link #STALE_TICKS}.
 * <p>
 * <b>Channel:</b> any {@link FieldReadout.Channel}. <b>Comparator:</b> greater than (default) or less
 * than the threshold. <b>Output:</b> digital (15 or 0) or analog ({@link FieldReadout#analog}: gauges
 * map −120..+120 onto 0..15, the 0-100 channels map 0..100 onto 0..15, the zone 0..4 onto 0..15, and the
 * warning and upkeep flags give 15 when set). The signal goes out on all
 * six sides, like the addon's Spine Sensor, so it can be picked up from any face that is exposed; covers keep working
 * ({@link #emit}: GT's internal transmitter covers carry it, other redstone covers keep their own side).
 * The GUI is cloned from GT's pH sensor ({@code MTEHatchPHSensorGui}, MUI2).
 * <p>
 * <b>P4 (quasar-p4.md §1.5, §8.1).</b> The same hatch also sits in a Quasar Engine's {@code n} sockets: while a quasar
 * publishes to it ({@link #publishQuasar}) its channel list is {@link QuasarSensorChannel} (E, Φ, λ, a, M, M/cap,
 * Bu/t per jet and total, zone, Integrity, Stability, the eruption warning, seconds to the next eruption, the best
 * host's σ and the link streak). Every host gains the <b>analog window w</b> (signed gauges map ±w onto 0..15; the
 * quasar's unbounded channels read w as full scale; default 120, the D-49 mapping, so a pre-P4 Eye hatch reads the
 * same) and <b>invert</b> (15 − output, digital and analog). The digital mode is the threshold mode: 15 above (or
 * below) a set value, else 0.
 */
public class MTEHatchStellarFieldSensor extends MTEHatch {

    /** A readout older than this (3 field seconds) no longer drives the output. */
    public static final long STALE_TICKS = 60;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_STELLAR_FIELD_SENSOR");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_STELLAR_FIELD_SENSOR_GLOW");

    // ------------------------------------------------------------------ settings (saved)
    private int channel = FieldReadout.Channel.P.ordinal();
    private double threshold = 0;
    /** {@code true}: the output is on when the value is BELOW the threshold. */
    private boolean lessThan = false;
    /** {@code true}: analog 0-15 from the value; the threshold and comparator are ignored. */
    private boolean analog = false;
    /** P4: the analog window w (signed channels ±w; the quasar's unbounded channels: full scale). */
    private int window = WINDOW_DEFAULT;
    /** P4: output 15 − v. */
    private boolean invert = false;

    /** The window's range and default (120: D-49's as-built gauge mapping, so old hatches read the same). */
    public static final int WINDOW_MIN = 1, WINDOW_MAX = 100_000, WINDOW_DEFAULT = 120;
    /** The threshold's range (the quasar's mass reaches 4,096 SM). */
    public static final double THRESHOLD_LIMIT = 100_000;

    /** Which machine last published to this hatch (live, not saved). */
    public enum Host {

        EYE("Eye of Harmony"),
        QUASAR("Quasar Engine");

        public final String label;

        Host(String label) {
            this.label = label;
        }
    }

    private Host host = Host.EYE;

    // ------------------------------------------------------------------ live (not saved: the Eye re-pushes every
    // second)
    private double lastValue = Double.NaN;
    private boolean lastLit = false;
    private long lastPublish = Long.MIN_VALUE / 2;
    private byte output = 0;

    public MTEHatchStellarFieldSensor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 12, 0, "Reads the Eye of Harmony's stability field as redstone");
    }

    public MTEHatchStellarFieldSensor(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchStellarFieldSensor(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            "Goes in a Sensor socket (n) of the Eye of Harmony or the Quasar Engine (Beyond the Stars).",
            "Outputs redstone from one channel. Eye: P, Θ, Ω, Π, shield, Integrity, Stability, zone,",
            "  event warning (+ / - / any), upkeep OK or the T5 setpoint error.",
            "Quasar: E, Φ, λ, a, M, M/cap, Bu/t (total, top, bottom), zone, Integrity, Stability,",
            "  eruption warning (Φ >= 90), seconds to the next eruption, best host σ, link streak %.",
            "Threshold (digital): 15 when the value is above (or below) the threshold, else 0.",
            "Analog: 0-15 across the range. Window w (default 120): signed values map -w..+w; masses, Bu/t,",
            "  seconds and σ map 0..w; 0-100 values, λ 0..2, a 0..1 and the zone 0..4 have fixed scales.",
            "Invert: 15 - output. Emits on all sides. Falls to 0 when its machine stops reporting." };
    }

    // ------------------------------------------------------------------ the Eye's side

    /**
     * Called by the Eye after every field second (server side) with the snapshot it publishes to its
     * sensors, GUI and scanner. A dark star is {@link FieldReadout#DARK}.
     */
    public void publish(FieldReadout readout) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide() || readout == null) return;
        lastPublish = base.getTimer();
        host = Host.EYE;
        lastLit = readout.lit;
        final FieldReadout.Channel c = channel();
        if (!readout.lit) {
            lastValue = Double.NaN;
            output = 0;
            return;
        }
        lastValue = readout.value(c);
        // gauges through the window (w 120 = D-49's round(15(v+120)/240)); the rest on their fixed scales
        final int a = c.gauge ? (int) Math.round(15 * Math.max(0, Math.min(1, (lastValue + window) / (2.0 * window))))
            : readout.analog(c);
        output = shape(a);
    }

    /**
     * P4: called by a Quasar Engine once a second (server side) with one value per {@link QuasarSensorChannel}
     * ({@code values[channel.ordinal()]}); {@code lit} false while there is no hole (output 0).
     */
    public void publishQuasar(double[] values, boolean lit) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide() || values == null) return;
        lastPublish = base.getTimer();
        host = Host.QUASAR;
        lastLit = lit;
        final QuasarSensorChannel c = quasarChannel();
        if (!lit || c.ordinal() >= values.length) {
            lastValue = Double.NaN;
            output = 0;
            return;
        }
        lastValue = values[c.ordinal()];
        output = shape(c.analog(lastValue, window));
    }

    /** Analog {@code a}, or the threshold's 15 / 0; then invert. */
    private byte shape(int a) {
        final int v = analog ? a : (lessThan ? lastValue < threshold : lastValue > threshold) ? 15 : 0;
        return (byte) (invert ? 15 - v : v);
    }

    /** The output this hatch drives now: 0 once the last readout is stale. */
    public int currentOutput() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || base.getTimer() - lastPublish > STALE_TICKS) return 0;
        return output;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        final byte strength = (byte) currentOutput();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            emit(aBaseMetaTileEntity, side, strength);
        }
        aBaseMetaTileEntity.setActive(lastLit && aBaseMetaTileEntity.getTimer() - lastPublish <= STALE_TICKS);
    }

    /**
     * Cover-friendly output on one side. A plain side, or one whose cover passes redstone through, gets the signal the
     * GT way ({@code setInternalOutputRedstoneSignal}, as GT's own sensor hatches do), so a cover that drives its own
     * side (a Redstone Receiver, a detector, a wireless receiver) is never overwritten. GT's Redstone Transmitter
     * (Internal) and Advanced Wireless Transmitter (Internal) covers transmit what the machine puts on their side, so
     * that side gets the signal directly: a sensor can feed a GT wireless channel to a Fluid Regulator or an export
     * bus anywhere in the base.
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

    // ------------------------------------------------------------------ settings

    public FieldReadout.Channel channel() {
        final FieldReadout.Channel[] all = FieldReadout.Channel.values();
        return all[Math.floorMod(channel, all.length)];
    }

    /** P4: the channel as a quasar channel (meaningful while a quasar publishes). */
    public QuasarSensorChannel quasarChannel() {
        return QuasarSensorChannel.of(channel);
    }

    /** How many channels the current host offers. */
    public int channelCount() {
        return host == Host.QUASAR ? QuasarSensorChannel.VALUES.length : FieldReadout.Channel.values().length;
    }

    public int getChannelOrdinal() {
        return Math.floorMod(channel, channelCount());
    }

    public void setChannelOrdinal(int ordinal) {
        channel = Math.floorMod(ordinal, channelCount());
    }

    /** The GUI's cycle: the next channel of the current host. */
    public void cycleChannel() {
        setChannelOrdinal(getChannelOrdinal() + 1);
    }

    /** The current channel's label for the current host. */
    public String currentChannelLabel() {
        return host == Host.QUASAR ? quasarChannel().label : channelLabel(channel());
    }

    /** The current channel's enum name for the current host (OpenComputers). */
    public String currentChannelName() {
        return host == Host.QUASAR ? quasarChannel().name() : channel().name();
    }

    /** The machine last publishing (the Eye until a quasar publishes). */
    public Host getHost() {
        return host;
    }

    public int getWindow() {
        return window;
    }

    public void setWindow(int w) {
        window = Math.max(WINDOW_MIN, Math.min(WINDOW_MAX, w));
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = Math.max(-THRESHOLD_LIMIT, Math.min(THRESHOLD_LIMIT, threshold));
    }

    public boolean isLessThan() {
        return lessThan;
    }

    public void setLessThan(boolean lessThan) {
        this.lessThan = lessThan;
    }

    public boolean isAnalog() {
        return analog;
    }

    public void setAnalog(boolean analog) {
        this.analog = analog;
    }

    /** The last value read on the channel (NaN when dark or never published). */
    public double getLastValue() {
        return currentOutputFresh() ? lastValue : Double.NaN;
    }

    private boolean currentOutputFresh() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.getTimer() - lastPublish <= STALE_TICKS;
    }

    /** One line for the GUI and the Eye's scanner: channel, comparator, value, output. */
    public String statusLine() {
        final double v = getLastValue();
        return String.format(
            Locale.ROOT,
            "%s %s%s → %s, out %d",
            currentChannelLabel(),
            analog ? "analog w " + window : (lessThan ? "< " : "> ") + fmt(threshold),
            invert ? ", inverted" : "",
            Double.isNaN(v) ? "no reading" : fmt(v),
            currentOutput());
    }

    /** Short display name of a channel (GUI, scanner). */
    public static String channelLabel(FieldReadout.Channel c) {
        switch (c) {
            case P:
                return "P (pressure)";
            case THETA:
                return "Θ (temperature)";
            case OMEGA:
                return "Ω (spin)";
            case PI:
                return "Π (polarity)";
            case SHIELD:
                return "A (agent shield)";
            case INTEGRITY:
                return "Integrity";
            case STABILITY:
                return "Stability";
            case ZONE:
                return "Zone (0 Resonant .. 4 Breach)";
            case WARN_PLUS:
                return "Event warning +";
            case WARN_MINUS:
                return "Event warning -";
            case WARN_ANY:
                return "Event warning (any)";
            case UPKEEP_OK:
                return "Upkeep OK";
            default:
                return "T5 setpoint error";
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ------------------------------------------------------------------ GT plumbing (as GT's pH sensor)

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
        aNBT.setInteger("btsChannel", channel);
        aNBT.setDouble("btsThreshold", threshold);
        aNBT.setBoolean("btsLessThan", lessThan);
        aNBT.setBoolean("btsAnalog", analog);
        aNBT.setInteger("btsWindow", window);
        aNBT.setBoolean("btsInvert", invert);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        // the raw ordinal: the host (so the channel count) is known only once a machine publishes
        channel = Math.max(0, aNBT.getInteger("btsChannel"));
        setThreshold(aNBT.getDouble("btsThreshold"));
        lessThan = aNBT.getBoolean("btsLessThan");
        analog = aNBT.getBoolean("btsAnalog");
        setWindow(aNBT.hasKey("btsWindow") ? aNBT.getInteger("btsWindow") : WINDOW_DEFAULT);
        invert = aNBT.getBoolean("btsInvert");
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
        return new StellarFieldSensorGui(this).build(data, syncManager, uiSettings);
    }
}
