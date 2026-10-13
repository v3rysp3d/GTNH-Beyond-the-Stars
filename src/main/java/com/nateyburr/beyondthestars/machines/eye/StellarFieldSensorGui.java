package com.nateyburr.beyondthestars.machines.eye;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSHatchGui;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Controller GUI cleanup (controller-gui-spec.md §5.10): the Stellar Field Sensor Hatch's GUI on the hatch layout. The
 * readout box (40 high) names the channel and the output it drives, shows the reading (a ±120 zone gauge for P, Θ,
 * Ω, Π and the T5 setpoint error; a bar for the 0-100 channels; a lamp for the zone, warnings and upkeep) with the
 * threshold as a yellow tick, and the rule ("ON above +10.0" or "Analog 0-15"). Under it the Channel cycle, Above /
 * Below, Digital / Analog and the threshold field. Every word is a lang key ({@code beyondthestars.gui.sensor.*}).
 */
public class StellarFieldSensorGui extends BTSHatchGui<MTEHatchStellarFieldSensor> {

    private static final String K = "beyondthestars.gui.sensor.";
    /** The synced "no reading" value (a finite comparison holds for it, unlike NaN). */
    private static final double NO_READING = Double.NEGATIVE_INFINITY;
    /** Readout lines (box-relative y): the channel, the reading, the rule. */
    private static final int Y_CHANNEL = 5, Y_VALUE = 16, Y_RULE = 28;
    /** Zone lamp fills, Resonant .. Breach (the zone text colours). */
    private static final int[] ZONE_FILL = { 0xFF55FF55, 0xFF55FFFF, 0xFFFFFF55, 0xFFFFAA00, 0xFFFF5555 };

    private IntSyncValue channel, output;
    private BooleanSyncValue lessThan, analog;
    private DoubleSyncValue threshold, reading;

    public StellarFieldSensorGui(MTEHatchStellarFieldSensor sensor) {
        super(sensor);
    }

    @Override
    protected int readoutHeight() {
        return 40;
    }

    // ================================================================================ text (client and bench)

    /** The kind of readout a channel draws. */
    enum Kind {
        GAUGE,
        BAR,
        LAMP
    }

    static Kind kind(FieldReadout.Channel c) {
        if (c.gauge) return Kind.GAUGE;
        return c.max >= 100 ? Kind.BAR : Kind.LAMP;
    }

    public static String channelName(FieldReadout.Channel c) {
        return tr(
            K + "channel."
                + c.name()
                    .toLowerCase(Locale.ROOT));
    }

    /** "out 15" in green, or dark grey at 0. */
    public static String outText(int out) {
        return (out > 0 ? "§a" : "§8") + tr(K + "out", out);
    }

    /** A threshold or reading as the channel prints it: signed for a gauge, one decimal otherwise. */
    public static String fmt(FieldReadout.Channel c, double v) {
        return c.gauge ? BTSFormat.signed(v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** The reading's value text, coloured: zone colour for a gauge, the health rule for a 0-100 channel. */
    public static String valueText(FieldReadout.Channel c, double v) {
        switch (kind(c)) {
            case GAUGE:
                return BTSGuiKit.gaugeValue(v, 0);
            case BAR:
                return BTSGuiKit.healthText(v) + BTSFormat.fmt1(v);
            default:
                return "";
        }
    }

    /** A lamp channel's text: the zone's name, or the flag's state. */
    public static String lampText(FieldReadout.Channel c, double v) {
        switch (c) {
            case ZONE:
                return tr("beyondthestars.gui.zone." + Math.max(0, Math.min(4, (int) Math.round(v))));
            case UPKEEP_OK:
                return tr(K + (v > 0 ? "upkeep.met" : "upkeep.short"));
            default:
                return tr(K + (v > 0 ? "warn.on" : "warn.off"));
        }
    }

    /** A lamp channel's fill: the zone colour; a warning red; upkeep green when met, red when short. */
    public static int lampFill(FieldReadout.Channel c, double v) {
        switch (c) {
            case ZONE:
                return ZONE_FILL[Math.max(0, Math.min(4, (int) Math.round(v)))];
            case UPKEEP_OK:
                return v > 0 ? BTSGuiKit.LAMP_ON : BTSGuiKit.LAMP_FAULT;
            default:
                return v > 0 ? BTSGuiKit.LAMP_FAULT : BTSGuiKit.LAMP_OFF;
        }
    }

    /** "ON above +10.0", "ON below 40.0" or "Analog 0-15". */
    public static String ruleText(FieldReadout.Channel c, boolean below, boolean isAnalog, double t) {
        if (isAnalog) return tr(K + "rule.analog");
        return tr(K + (below ? "rule.below" : "rule.above"), fmt(c, t));
    }

    public static String comparatorLabel(boolean below) {
        return tr(K + (below ? "below" : "above"));
    }

    public static String modeLabel(boolean isAnalog) {
        return tr(K + (isAnalog ? "analog" : "digital"));
    }

    /** The readout and the control labels as text, from the hatch itself (server bench; same words as the GUI). */
    public static List<String> benchLines(MTEHatchStellarFieldSensor h) {
        final List<String> out = new ArrayList<>();
        final FieldReadout.Channel c = h.channel();
        final double v = h.getLastValue();
        out.add("[" + channelName(c) + "]  " + outText(h.currentOutput()));
        if (Double.isNaN(v)) out.add(tr(K + "noreading"));
        else if (kind(c) == Kind.LAMP) out.add("(lamp " + Integer.toHexString(lampFill(c, v)) + ") " + lampText(c, v));
        else out.add(
            "(" + kind(c).name()
                .toLowerCase(Locale.ROOT)
                + ") "
                + valueText(c, v)
                + (h.isAnalog() ? "" : ", tick " + fmt(c, h.getThreshold())));
        out.add(ruleText(c, h.isLessThan(), h.isAnalog(), h.getThreshold()));
        out.add(tr(K + "channelrow", channelName(c)));
        out.add(comparatorLabel(h.isLessThan()));
        out.add(modeLabel(h.isAnalog()));
        out.add("[" + fmt(c, h.getThreshold()) + "] " + tr(K + "threshold"));
        return out;
    }

    // ================================================================================ readout

    @Override
    protected void addReadout(ParentWidget<?> box, PanelSyncManager syncManager) {
        channel = new IntSyncValue(machine::getChannelOrdinal, machine::setChannelOrdinal).allowC2S();
        lessThan = new BooleanSyncValue(machine::isLessThan, machine::setLessThan).allowC2S();
        analog = new BooleanSyncValue(machine::isAnalog, machine::setAnalog).allowC2S();
        threshold = new DoubleSyncValue(machine::getThreshold, machine::setThreshold).allowC2S();
        // NaN (no reading) never equals itself, so MUI2's change check would resend it every tick: sync a sentinel
        // that compares equal instead
        reading = new DoubleSyncValue(() -> {
            final double v = machine.getLastValue();
            return Double.isNaN(v) ? NO_READING : v;
        });
        output = new IntSyncValue(machine::currentOutput);
        syncManager.syncValue("btsSensorChannel", channel);
        syncManager.syncValue("btsSensorLessThan", lessThan);
        syncManager.syncValue("btsSensorAnalog", analog);
        syncManager.syncValue("btsSensorThreshold", threshold);
        syncManager.syncValue("btsSensorReading", reading);
        syncManager.syncValue("btsSensorOutput", output);

        box.child(
            line(
                Y_CHANNEL,
                () -> channelName(channel()),
                BTSGuiKit.VALUE,
                () -> outText(output.getIntValue()),
                BTSGuiKit.VALUE,
                true).tooltipDynamic(t -> {
                    t.addLine(channelName(channel()));
                    t.addLine("§7" + tr(K + "out.tip"));
                })
                    .tooltipAutoUpdate(true));
        box.child(line(Y_VALUE, () -> tr(K + "noreading"), BTSGuiKit.DIM, null, 0).setEnabledIf(w -> !hasReading()));
        box.child(
            gaugeLine(Y_VALUE, this::value, this::tickValue, () -> valueText(channel(), value()))
                .setEnabledIf(w -> hasReading() && kind(channel()) == Kind.GAUGE));
        box.child(
            barLine(
                Y_VALUE,
                () -> value() / channel().max,
                () -> Double.isNaN(tickValue()) ? Double.NaN : tickValue() / channel().max,
                () -> BTSGuiKit.healthGrad(value()),
                () -> valueText(channel(), value())).setEnabledIf(w -> hasReading() && kind(channel()) == Kind.BAR));
        box.child(
            lampLine(Y_VALUE, () -> lampFill(channel(), value()), () -> lampText(channel(), value()), () -> "")
                .setEnabledIf(w -> hasReading() && kind(channel()) == Kind.LAMP));
        box.child(
            line(
                Y_RULE,
                () -> ruleText(channel(), lessThan.getBoolValue(), analog.getBoolValue(), threshold.getDoubleValue()),
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
        column.child(
            cycleRow(
                channel,
                FieldReadout.Channel.values().length,
                () -> tr(K + "channelrow", channelName(channel())),
                t -> {
                    t.addLine(tr(K + "channel.tip"));
                    t.addLine("§7" + tr(K + "channel.tip2"));
                }));
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
            fieldRow(
                new TextFieldWidget().numbersDouble(val -> Math.min(120, Math.max(-120, val)))
                    .value(threshold),
                40,
                () -> tr(K + "threshold"),
                t -> t.addLine(tr(K + "threshold.tip"))));
    }

    private FieldReadout.Channel channel() {
        final FieldReadout.Channel[] all = FieldReadout.Channel.values();
        return all[Math.floorMod(channel.getIntValue(), all.length)];
    }
}
