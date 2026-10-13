package com.nateyburr.beyondthestars.machines.directive;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

/**
 * AUTOMATION PASS: the Directive Hatch's GUI (MUI2, laid out like the Stellar Field Sensor Hatch's): the set (only the
 * host's own sets are offered once the hatch sits in a machine), the output mode, invert, the window w, then the four
 * bits by name with the ones on now, and the line the hatch drives.
 */
public class DirectiveHatchGui extends MTEHatchBaseGui<MTEHatchDirective> {

    public DirectiveHatchGui(MTEHatchDirective hatch) {
        super(hatch);
    }

    /**
     * Ten lines (machine, set, output, invert, window, the four bits, the drive line) are taller than a hatch's
     * standard content area: without this the four bit lines and the drive line were drawn under the player inventory
     * (seen in the automation photo run).
     */
    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + EXTRA_HEIGHT;
    }

    /** Pixels added for the lines below the window field (five text lines and their padding). */
    private static final int EXTRA_HEIGHT = 64;

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        // the server setter skips on to the host's next set in the direction the button moved (left click +1, right
        // click -1, B-72), so a cycle never lands on a foreign set and a backward cycle wraps to the host's last one
        final IntSyncValue set = new IntSyncValue(machine::getSetOrdinal, v -> {
            final DirectiveSet.Host host = machine.getHost();
            final int n = DirectiveSet.VALUES.length;
            final int step = Math.floorMod(v - machine.getSetOrdinal(), n) == n - 1 ? -1 : 1;
            int o = v;
            for (int i = 0; i < n; i++) {
                final DirectiveSet s = DirectiveSet.VALUES[Math.floorMod(o, n)];
                if (host == DirectiveSet.Host.NONE || s.host == host) break;
                o += step;
            }
            machine.setSetOrdinal(o);
        }).allowC2S();
        final IntSyncValue output = new IntSyncValue(machine::getOutputOrdinal, machine::setOutputOrdinal).allowC2S();
        final BooleanSyncValue invert = new BooleanSyncValue(machine::isInvert, machine::setInvert).allowC2S();
        final IntSyncValue window = new IntSyncValue(machine::getWindow, machine::setWindow).allowC2S();
        final IntSyncValue code = new IntSyncValue(machine::currentCode);
        final StringSyncValue status = new StringSyncValue(machine::guiLine);
        final StringSyncValue host = new StringSyncValue(() -> machine.getHost().label);
        final StringSyncValue[] labels = new StringSyncValue[4];
        for (int i = 0; i < 4; i++) {
            final int bit = i;
            labels[i] = new StringSyncValue(() -> machine.label(bit));
            syncManager.syncValue("btsDirLabel" + i, labels[i]);
        }
        syncManager.syncValue("btsDirSet", set);
        syncManager.syncValue("btsDirOutput", output);
        syncManager.syncValue("btsDirInvert", invert);
        syncManager.syncValue("btsDirWindow", window);
        syncManager.syncValue("btsDirCode", code);
        syncManager.syncValue("btsDirStatus", status);
        syncManager.syncValue("btsDirHost", host);

        final Flow col = Flow.column()
            .child(
                IKey.dynamic(() -> "Machine: " + host.getStringValue())
                    .asWidget())
            .child(
                Flow.row()
                    .child(
                        new CycleButtonWidget().value(set)
                            .stateCount(DirectiveSet.VALUES.length)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .size(16))
                    .child(
                        IKey.dynamic(() -> "Set: " + setOf(set).label)
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new CycleButtonWidget().value(output)
                            .stateCount(DirectiveSet.Output.VALUES.length)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .size(16))
                    .child(
                        IKey.dynamic(
                            () -> setOf(set).control ? "Output: none (a control input reads redstone 1-15)"
                                : "Output: " + DirectiveSet.Output.of(output.getIntValue()).label)
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new ToggleButton().value(invert)
                            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_ON)
                            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_REDSTONE_OFF)
                            .size(16))
                    .child(
                        IKey.dynamic(() -> invert.getBoolValue() ? "Inverted (15 - output)" : "Not inverted")
                            .asWidget())
                    .coverChildren()
                    .childPadding(2))
            .child(
                Flow.row()
                    .child(
                        new TextFieldWidget().numbersInt(MTEHatchDirective.WINDOW_MIN, MTEHatchDirective.WINDOW_MAX)
                            .size(30, 12)
                            .value(window))
                    .child(
                        IKey.dynamic(
                            () -> setOf(set).host == DirectiveSet.Host.KERR ? "Window w: bolt reserve, minutes"
                                : "Window w: gauge band, ±w")
                            .asWidget())
                    .coverChildren()
                    .childPadding(2));
        for (int i = 0; i < 4; i++) {
            final int bit = i;
            col.child(IKey.dynamic(() -> {
                if (setOf(set).control) return bit == 0 ? "Redstone 1-15 sets it; 0 leaves the GUI's value" : "";
                final boolean on = (code.getIntValue() & (1 << bit)) != 0;
                return (on ? "§a■ " : "§8□ ") + "bit "
                    + (bit + 1)
                    + " ("
                    + (1 << bit)
                    + "): "
                    + labels[bit].getStringValue();
            })
                .asWidget());
        }
        col.child(
            IKey.dynamic(status::getStringValue)
                .asWidget())
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(2);
        return super.createContentSection(panel, syncManager).child(col);
    }

    private static DirectiveSet setOf(IntSyncValue sv) {
        return DirectiveSet.VALUES[Math.floorMod(sv.getIntValue(), DirectiveSet.VALUES.length)];
    }
}
