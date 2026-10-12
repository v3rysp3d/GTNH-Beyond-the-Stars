package com.nateyburr.beyondthestars.machines.eye;

import java.util.Locale;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/**
 * D-49: the Stellar Field Sensor Hatch's GUI, cloned from GT's {@code MTEHatchPHSensorGui} (MUI2):
 * a channel selector, the comparator (above / below), digital or analog output, the threshold, and a
 * live line with the last reading and the output it drives.
 */
public class StellarFieldSensorGui extends MTEHatchBaseGui<MTEHatchStellarFieldSensor> {

    /** The synced "no reading" value (a finite comparison holds for it, unlike NaN). */
    private static final double NO_READING = Double.NEGATIVE_INFINITY;

    public StellarFieldSensorGui(MTEHatchStellarFieldSensor sensor) {
        super(sensor);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        final IntSyncValue channel = new IntSyncValue(machine::getChannelOrdinal, machine::setChannelOrdinal)
            .allowC2S();
        final BooleanSyncValue lessThan = new BooleanSyncValue(machine::isLessThan, machine::setLessThan).allowC2S();
        final BooleanSyncValue analog = new BooleanSyncValue(machine::isAnalog, machine::setAnalog).allowC2S();
        // NaN (no reading) never equals itself, so MUI2's change check would resend it every tick: sync a sentinel
        // that compares equal instead
        final DoubleSyncValue reading = new DoubleSyncValue(() -> {
            final double v = machine.getLastValue();
            return Double.isNaN(v) ? NO_READING : v;
        });
        final IntSyncValue output = new IntSyncValue(machine::currentOutput);
        syncManager.syncValue("btsSensorChannel", channel);
        syncManager.syncValue("btsSensorLessThan", lessThan);
        syncManager.syncValue("btsSensorAnalog", analog);
        syncManager.syncValue("btsSensorReading", reading);
        syncManager.syncValue("btsSensorOutput", output);

        final Flow col = Flow.column()
            .child(
                Flow.row()
                    .child(
                        new CycleButtonWidget().value(channel)
                            .stateCount(FieldReadout.Channel.values().length)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .size(16))
                    .child(
                        IKey.dynamic(() -> "Channel: " + MTEHatchStellarFieldSensor.channelLabel(channel(channel)))
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new ToggleButton().value(lessThan)
                            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                            .size(16))
                    .child(
                        IKey.dynamic(
                            () -> lessThan.getBoolValue() ? "On when BELOW the threshold"
                                : "On when ABOVE the threshold")
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new ToggleButton().value(analog)
                            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                            .size(16))
                    .child(
                        IKey.dynamic(() -> analog.getBoolValue() ? "Analog 0-15 (threshold ignored)" : "Digital 15 / 0")
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new TextFieldWidget().numbersDouble(val -> Math.min(120, Math.max(-120, val)))
                            .size(60, 12)
                            .value(new DoubleSyncValue(machine::getThreshold, machine::setThreshold).allowC2S()))
                    .child(
                        IKey.str("Threshold")
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(IKey.dynamic(() -> {
                final double v = reading.getDoubleValue();
                return (Double.isNaN(v) || v == NO_READING ? "No reading (star dark or Eye silent)"
                    : String.format(Locale.ROOT, "Reading %.1f", v)) + ", output " + output.getIntValue();
            })
                .asWidget())
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(2);
        return super.createContentSection(panel, syncManager).child(col);
    }

    private static FieldReadout.Channel channel(IntSyncValue sv) {
        final FieldReadout.Channel[] all = FieldReadout.Channel.values();
        return all[Math.floorMod(sv.getIntValue(), all.length)];
    }
}
