package com.nateyburr.beyondthestars.machines.eye;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSHatchGui;
import com.nateyburr.beyondthestars.machines.quasar.QuasarSensorChannel;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Controller GUI cleanup (controller-gui-spec.md §5.10): the Stellar Field Sensor Hatch's GUI on the hatch layout. The
 * readout box (40 high) names the channel and the output it drives, shows the reading (a ±120 zone gauge for the
 * signed channels: P, Θ, Ω, Π, the T5 setpoint error, W, Λ, the quasar's E; a bar for the 0-max channels, the
 * unbounded ones against the window w; a lamp for the zone, warnings and upkeep) with the threshold as a yellow tick,
 * and the rule ("ON above +10.0" or "Analog 0-15, w 120"). Under it the Channel cycle, Above / Below, Digital /
 * Analog, Invert, the threshold and the window w. Every word is a lang key ({@code beyondthestars.gui.sensor.*}).
 * <p>
 * P4 (merged in from master): the channel list is the host's (the Eye's field channels, or the Quasar Engine's disc
 * channels, {@code qchannel.*} keys), so the channel button asks the server for the next channel of whichever machine
 * publishes; the window w and invert are rows of their own.
 */
public class StellarFieldSensorGui extends BTSHatchGui<MTEHatchStellarFieldSensor> {

    private static final String K = "beyondthestars.gui.sensor.";
    /** The synced "no reading" value (a finite comparison holds for it, unlike NaN). */
    private static final double NO_READING = Double.NEGATIVE_INFINITY;
    /** Readout lines (box-relative y): the channel, the reading, the rule. */
    private static final int Y_CHANNEL = 5, Y_VALUE = 16, Y_RULE = 28;
    /** Zone lamp fills, Resonant .. Breach (the zone text colours). */
    private static final int[] ZONE_FILL = { 0xFF55FF55, 0xFF55FFFF, 0xFFFFFF55, 0xFFFFAA00, 0xFFFF5555 };

    private IntSyncValue channel, count, host, output, window;
    private BooleanSyncValue lessThan, analog, invert;
    private DoubleSyncValue threshold, reading;

    public StellarFieldSensorGui(MTEHatchStellarFieldSensor sensor) {
        super(sensor);
    }

    @Override
    protected int readoutHeight() {
        return 40;
    }

    /** Two control rows more than the hatch layout's four (Invert, Window): 36 px, rounded to 30 + the slack. */
    @Override
    protected int panelHeight() {
        return PANEL_H + 30;
    }

    // ================================================================================ channels

    /** The kind of readout a channel draws. */
    enum Kind {
        GAUGE,
        BAR,
        LAMP
    }

    /** What a lamp channel means. */
    enum Lamp {
        NONE,
        ZONE,
        UPKEEP,
        WARN
    }

    /** One channel of a host, as the GUI draws it. */
    static final class Ch {

        /** Lang key suffix ({@code channel.p}, {@code qchannel.mass}). */
        final String key;
        final Kind kind;
        final Lamp lamp;
        /** A bar's full scale; 0: the window w is the full scale (unbounded values). */
        final double scale;
        /** A 0-100 health value (the health colour rule). */
        final boolean health;

        Ch(String key, Kind kind, Lamp lamp, double scale, boolean health) {
            this.key = key;
            this.kind = kind;
            this.lamp = lamp;
            this.scale = scale;
            this.health = health;
        }

        /** The window w sets the analog scale (signed channels: ±w; unbounded: 0..w). */
        boolean usesWindow() {
            return kind == Kind.GAUGE || (kind == Kind.BAR && scale <= 0);
        }
    }

    static Ch eyeChannel(FieldReadout.Channel c) {
        final String key = "channel." + c.name()
            .toLowerCase(Locale.ROOT);
        if (c.gauge) return new Ch(key, Kind.GAUGE, Lamp.NONE, 0, false);
        switch (c) {
            case ZONE:
                return new Ch(key, Kind.LAMP, Lamp.ZONE, 4, false);
            case UPKEEP_OK:
                return new Ch(key, Kind.LAMP, Lamp.UPKEEP, 1, false);
            case WARN_PLUS:
            case WARN_MINUS:
            case WARN_ANY:
                return new Ch(key, Kind.LAMP, Lamp.WARN, 1, false);
            case SHIELD:
            case INTEGRITY:
            case STABILITY:
                return new Ch(key, Kind.BAR, Lamp.NONE, 100, true);
            default:
                return new Ch(key, Kind.BAR, Lamp.NONE, c.max, false);
        }
    }

    static Ch quasarChannel(QuasarSensorChannel c) {
        final String key = "qchannel." + c.name()
            .toLowerCase(Locale.ROOT);
        switch (c) {
            case ZONE:
                return new Ch(key, Kind.LAMP, Lamp.ZONE, 4, false);
            case ERUPT_WARN:
                return new Ch(key, Kind.LAMP, Lamp.WARN, 1, false);
            case INTEGRITY:
            case STABILITY:
                return new Ch(key, Kind.BAR, Lamp.NONE, 100, true);
            default:
                switch (c.kind) {
                    case SIGNED:
                        return new Ch(key, Kind.GAUGE, Lamp.NONE, 0, false);
                    case OPEN:
                        return new Ch(key, Kind.BAR, Lamp.NONE, 0, false);
                    default:
                        return new Ch(key, Kind.BAR, Lamp.NONE, c.max, false);
                }
        }
    }

    /** The channel {@code ordinal} of {@code host}. */
    static Ch channelOf(MTEHatchStellarFieldSensor.Host host, int ordinal) {
        if (host == MTEHatchStellarFieldSensor.Host.QUASAR) return quasarChannel(QuasarSensorChannel.of(ordinal));
        final FieldReadout.Channel[] all = FieldReadout.Channel.values();
        return eyeChannel(all[Math.floorMod(ordinal, all.length)]);
    }

    // ================================================================================ text (client and bench)

    public static String channelName(Ch c) {
        return tr(K + c.key);
    }

    public static String hostName(MTEHatchStellarFieldSensor.Host h) {
        return tr(
            K + "host."
                + h.name()
                    .toLowerCase(Locale.ROOT));
    }

    /** "out 15" in green, or dark grey at 0. */
    public static String outText(int out) {
        return (out > 0 ? "§a" : "§8") + tr(K + "out", out);
    }

    /** A threshold or reading as the channel prints it: signed for a gauge, one decimal otherwise. */
    public static String fmt(Ch c, double v) {
        return c.kind == Kind.GAUGE ? BTSFormat.signed(v) : BTSFormat.fmt1(v);
    }

    /** A bar's full scale: its own, or the window for an unbounded value. */
    static double scale(Ch c, int w) {
        return c.scale > 0 ? c.scale : Math.max(1, w);
    }

    /** The reading's value text, coloured: zone colour for a gauge, the health rule for a 0-100 channel. */
    public static String valueText(Ch c, double v) {
        switch (c.kind) {
            case GAUGE:
                return BTSGuiKit.gaugeValue(v, 0);
            case BAR:
                return (c.health ? BTSGuiKit.healthText(v) : "") + BTSFormat.fmt1(v);
            default:
                return "";
        }
    }

    /** A lamp channel's text: the zone's name, or the flag's state. */
    public static String lampText(Ch c, double v) {
        switch (c.lamp) {
            case ZONE:
                return tr("beyondthestars.gui.zone." + Math.max(0, Math.min(4, (int) Math.round(v))));
            case UPKEEP:
                return tr(K + (v > 0 ? "upkeep.met" : "upkeep.short"));
            default:
                return tr(K + (v > 0 ? "warn.on" : "warn.off"));
        }
    }

    /** A lamp channel's fill: the zone colour; a warning red; upkeep green when met, red when short. */
    public static int lampFill(Ch c, double v) {
        switch (c.lamp) {
            case ZONE:
                return ZONE_FILL[Math.max(0, Math.min(4, (int) Math.round(v)))];
            case UPKEEP:
                return v > 0 ? BTSGuiKit.LAMP_ON : BTSGuiKit.LAMP_FAULT;
            default:
                return v > 0 ? BTSGuiKit.LAMP_FAULT : BTSGuiKit.LAMP_OFF;
        }
    }

    /** "ON above +10.0", "ON below 40.0", "Analog 0-15" or "Analog 0-15, w 120"; ", inverted" when inverted. */
    public static String ruleText(Ch c, boolean below, boolean isAnalog, double t, int w, boolean inv) {
        final String r;
        if (isAnalog) r = c.usesWindow() ? tr(K + "rule.analogw", w) : tr(K + "rule.analog");
        else r = tr(K + (below ? "rule.below" : "rule.above"), fmt(c, t));
        return inv ? r + tr(K + "rule.inverted") : r;
    }

    public static String comparatorLabel(boolean below) {
        return tr(K + (below ? "below" : "above"));
    }

    public static String modeLabel(boolean isAnalog) {
        return tr(K + (isAnalog ? "analog" : "digital"));
    }

    public static String invertLabel(boolean inv) {
        return tr(K + (inv ? "inverted" : "notinverted"));
    }

    /** The readout and the control labels as text, from the hatch itself (server bench; same words as the GUI). */
    public static List<String> benchLines(MTEHatchStellarFieldSensor h) {
        final List<String> out = new ArrayList<>();
        final Ch c = channelOf(h.getHost(), h.getChannelOrdinal());
        final double v = h.getLastValue();
        out.add("[" + channelName(c) + "]  " + outText(h.currentOutput()) + "  (" + hostName(h.getHost()) + ")");
        if (Double.isNaN(v)) out.add(tr(K + "noreading"));
        else if (c.kind == Kind.LAMP) out.add("(lamp " + Integer.toHexString(lampFill(c, v)) + ") " + lampText(c, v));
        else out.add(
            "(" + c.kind.name()
                .toLowerCase(Locale.ROOT)
                + (c.kind == Kind.BAR ? " of " + BTSFormat.fmt1(scale(c, h.getWindow())) : "")
                + ") "
                + valueText(c, v)
                + (h.isAnalog() ? "" : ", tick " + fmt(c, h.getThreshold())));
        out.add(ruleText(c, h.isLessThan(), h.isAnalog(), h.getThreshold(), h.getWindow(), h.isInvert()));
        out.add(tr(K + "channelrow", channelName(c)));
        out.add(comparatorLabel(h.isLessThan()));
        out.add(modeLabel(h.isAnalog()));
        out.add(invertLabel(h.isInvert()));
        out.add("[" + fmt(c, h.getThreshold()) + "] " + tr(K + "threshold"));
        out.add("[" + h.getWindow() + "] " + tr(K + "window"));
        return out;
    }

    // ================================================================================ readout

    @Override
    protected void addReadout(ParentWidget<?> box, PanelSyncManager syncManager) {
        channel = new IntSyncValue(machine::getChannelOrdinal, machine::setChannelOrdinal).allowC2S();
        count = new IntSyncValue(machine::channelCount);
        host = new IntSyncValue(
            () -> machine.getHost()
                .ordinal());
        lessThan = new BooleanSyncValue(machine::isLessThan, machine::setLessThan).allowC2S();
        analog = new BooleanSyncValue(machine::isAnalog, machine::setAnalog).allowC2S();
        invert = new BooleanSyncValue(machine::isInvert, machine::setInvert).allowC2S();
        window = new IntSyncValue(machine::getWindow, machine::setWindow).allowC2S();
        threshold = new DoubleSyncValue(machine::getThreshold, machine::setThreshold).allowC2S();
        // NaN (no reading) never equals itself, so MUI2's change check would resend it every tick: sync a sentinel
        // that compares equal instead
        reading = new DoubleSyncValue(() -> {
            final double v = machine.getLastValue();
            return Double.isNaN(v) ? NO_READING : v;
        });
        output = new IntSyncValue(machine::currentOutput);
        syncManager.syncValue("btsSensorChannel", channel);
        syncManager.syncValue("btsSensorCount", count);
        syncManager.syncValue("btsSensorHost", host);
        syncManager.syncValue("btsSensorLessThan", lessThan);
        syncManager.syncValue("btsSensorAnalog", analog);
        syncManager.syncValue("btsSensorInvert", invert);
        syncManager.syncValue("btsSensorWindow", window);
        syncManager.syncValue("btsSensorThreshold", threshold);
        syncManager.syncValue("btsSensorReading", reading);
        syncManager.syncValue("btsSensorOutput", output);

        box.child(
            line(
                Y_CHANNEL,
                () -> channelName(ch()),
                BTSGuiKit.VALUE,
                () -> outText(output.getIntValue()),
                BTSGuiKit.VALUE,
                true).tooltipDynamic(t -> {
                    t.addLine(channelName(ch()));
                    t.addLine("§7" + tr(K + "host", hostName(host())));
                    t.addLine("§7" + tr(K + "out.tip"));
                })
                    .tooltipAutoUpdate(true));
        box.child(line(Y_VALUE, () -> tr(K + "noreading"), BTSGuiKit.DIM, null, 0).setEnabledIf(w -> !hasReading()));
        box.child(
            gaugeLine(Y_VALUE, this::value, this::tickValue, () -> valueText(ch(), value()))
                .setEnabledIf(w -> hasReading() && ch().kind == Kind.GAUGE));
        box.child(
            barLine(
                Y_VALUE,
                () -> value() / scale(ch(), window.getIntValue()),
                () -> Double.isNaN(tickValue()) ? Double.NaN : tickValue() / scale(ch(), window.getIntValue()),
                () -> ch().health ? BTSGuiKit.healthGrad(value()) : BTSGuiKit.Grad.BLUE,
                () -> valueText(ch(), value())).setEnabledIf(w -> hasReading() && ch().kind == Kind.BAR));
        box.child(
            lampLine(Y_VALUE, () -> lampFill(ch(), value()), () -> lampText(ch(), value()), () -> "")
                .setEnabledIf(w -> hasReading() && ch().kind == Kind.LAMP));
        box.child(
            line(
                Y_RULE,
                () -> ruleText(
                    ch(),
                    lessThan.getBoolValue(),
                    analog.getBoolValue(),
                    threshold.getDoubleValue(),
                    window.getIntValue(),
                    invert.getBoolValue()),
                BTSGuiKit.LABEL,
                null,
                0));
    }

    private boolean hasReading() {
        final double v = reading.getDoubleValue();
        return !Double.isNaN(v) && v != NO_READING;
    }

    private double value() {
        return hasReading() ? reading.getDoubleValue() : 0;
    }

    /** The threshold tick, or NaN in analog mode (the threshold is ignored there). */
    private double tickValue() {
        return analog.getBoolValue() ? Double.NaN : threshold.getDoubleValue();
    }

    // ================================================================================ controls

    @Override
    protected void addControls(Flow column, PanelSyncManager syncManager) {
        // the host's channel count changes with the host (Eye 20, quasar 16), so this is a plain button that steps
        // through the synced count rather than a fixed-state cycle button
        final ButtonWidget<?> next = new ButtonWidget<>().size(BUTTON)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
            .onMousePressed(b -> {
                final int n = Math.max(1, count.getIntValue());
                final int step = b == 1 ? n - 1 : 1;
                channel.setIntValue(Math.floorMod(channel.getIntValue() + step, n));
                return true;
            });
        next.tooltip(t -> {
            t.addLine(tr(K + "channel.tip"));
            t.addLine("§7" + tr(K + "channel.tip2"));
        })
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        column.child(controlRow(next, BUTTON, () -> tr(K + "channelrow", channelName(ch()))));
        column.child(
            toggleRow(
                lessThan,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF,
                () -> comparatorLabel(lessThan.getBoolValue()),
                t -> t.addLine(tr(K + "comparator.tip"))));
        column.child(
            toggleRow(
                analog,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF,
                () -> modeLabel(analog.getBoolValue()),
                t -> {
                    t.addLine(tr(K + "mode.tip"));
                    t.addLine("§7" + tr(K + "mode.tip2"));
                }));
        column.child(
            toggleRow(
                invert,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON,
                GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF,
                () -> invertLabel(invert.getBoolValue()),
                t -> t.addLine(tr(K + "invert.tip"))));
        column.child(
            fieldRow(
                new TextFieldWidget()
                    .numbersDouble(
                        val -> Math.min(
                            MTEHatchStellarFieldSensor.THRESHOLD_LIMIT,
                            Math.max(-MTEHatchStellarFieldSensor.THRESHOLD_LIMIT, val)))
                    .value(threshold),
                40,
                () -> tr(K + "threshold"),
                t -> t.addLine(tr(K + "threshold.tip"))));
        column.child(
            fieldRow(
                new TextFieldWidget()
                    .numbersInt(MTEHatchStellarFieldSensor.WINDOW_MIN, MTEHatchStellarFieldSensor.WINDOW_MAX)
                    .value(window),
                40,
                () -> tr(K + "window"),
                t -> {
                    t.addLine(tr(K + "window.tip"));
                    t.addLine("§7" + tr(K + "window.tip2"));
                }));
    }

    private MTEHatchStellarFieldSensor.Host host() {
        final MTEHatchStellarFieldSensor.Host[] all = MTEHatchStellarFieldSensor.Host.values();
        return all[Math.floorMod(host.getIntValue(), all.length)];
    }

    private Ch ch() {
        return channelOf(host(), channel.getIntValue());
    }
}
