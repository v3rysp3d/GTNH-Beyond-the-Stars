package com.nateyburr.beyondthestars.machines.directive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSHatchGui;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Controller GUI cleanup (controller-gui-spec.md §5.10): the Directive Hatch's GUI on the hatch layout. The readout
 * box (46 high) says which set the hatch reports and for which machine, the four bits as lamps (lit while on) with
 * their names, and the code with the redstone it drives; on a control set, the input it reads and the setpoint that
 * drives. Under it the Set and Output cycles, Invert and the window field. The panel is 214 high so nothing overlaps
 * the inventory.
 * <p>
 * Every word is a lang key ({@code beyondthestars.gui.directive.*}); the bit names and the control line come from
 * the host (runtime text it shares with the scanner and OpenComputers, spec Q6).
 */
public class DirectiveHatchGui extends BTSHatchGui<MTEHatchDirective> {

    private static final String K = "beyondthestars.gui.directive.";
    /** Readout lines (box-relative y): the set, two lamp rows, the code. */
    private static final int Y_SET = 5, Y_BITS1 = 17, Y_BITS2 = 27, Y_CODE = 37;
    /** The second lamp column (box-relative x). */
    private static final int LAMP_X2 = 85;

    private IntSyncValue set, output, code, out, host, controlIn;
    private BooleanSyncValue invert;
    private IntSyncValue window;
    private StringSyncValue controlLine;
    private final StringSyncValue[] labels = new StringSyncValue[4];

    public DirectiveHatchGui(MTEHatchDirective hatch) {
        super(hatch);
    }

    @Override
    protected int readoutHeight() {
        return 46;
    }

    // ================================================================================ text (client and bench)

    /** The set's short name for the readout ("Trims", "Setpoint (control)"): the host is on the right. */
    public static String setShort(DirectiveSet s) {
        return tr(
            K + "short."
                + s.name()
                    .toLowerCase(Locale.ROOT));
    }

    /** The set's full name ("Eye: Trims"), as the Set row and the tooltip give it. */
    public static String setName(DirectiveSet s) {
        return tr(
            K + "set."
                + s.name()
                    .toLowerCase(Locale.ROOT));
    }

    public static String outputName(DirectiveSet.Output o) {
        return tr(
            K + "mode."
                + o.name()
                    .toLowerCase(Locale.ROOT));
    }

    /** The machine the hatch reports for, or "§6no machine" when none is pushing. */
    public static String hostName(DirectiveSet.Host h) {
        return (h == DirectiveSet.Host.NONE ? "§6" : "") + tr(
            K + "host."
                + h.name()
                    .toLowerCase(Locale.ROOT));
    }

    /** A bit lamp's text: its value (1, 2, 4, 8) and its name. */
    public static String bitText(int bit, String label) {
        return (1 << bit) + " " + label;
    }

    /** The code line's left text. */
    public static String codeLeft(boolean reporting, int code) {
        return reporting ? tr(K + "code", code) : "§6" + tr(K + "nomachine");
    }

    /** The code line's right text: green while it drives a signal, dark grey at 0. */
    public static String codeRight(int redstone) {
        return (redstone > 0 ? "§a" : "§8") + tr(K + "redstone", redstone);
    }

    public static String inputLeft(int in) {
        return tr(K + "input", in);
    }

    public static String windowLabel(DirectiveSet s) {
        return tr(K + "window", tr(K + (s.host == DirectiveSet.Host.KERR ? "window.kerr" : "window.eye")));
    }

    public static String outputLabel(DirectiveSet s, DirectiveSet.Output o) {
        return s.control ? tr(K + "output.none") : tr(K + "output", outputName(o));
    }

    public static String invertLabel(boolean inv) {
        return tr(K + (inv ? "inverted" : "notinverted"));
    }

    /** The readout and the control labels as text, from the hatch itself (server bench; same words as the GUI). */
    public static List<String> benchLines(MTEHatchDirective h) {
        final List<String> out = new ArrayList<>();
        final DirectiveSet s = h.getSet();
        final DirectiveSet.Host host = h.getHost();
        out.add("[" + setShort(s) + "]  " + hostName(host));
        if (s.control) {
            out.add(inputLeft(h.getControlIn()) + "  |  " + h.getControlLine());
            out.add(tr(K + "control.hint1"));
            out.add(tr(K + "control.hint2"));
        } else {
            final int c = h.currentCode();
            final StringBuilder b = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                b.append((c & (1 << i)) != 0 ? "(on) " : "(  ) ")
                    .append(bitText(i, h.label(i)))
                    .append(i == 1 ? "\n" : "   ");
            }
            for (String l : b.toString()
                .split("\n")) out.add(l.trim());
            out.add(codeLeft(host != DirectiveSet.Host.NONE, c) + "  |  " + codeRight(h.currentOutput()));
        }
        out.add(tr(K + "setrow", setName(s)));
        out.add(outputLabel(s, h.getOutput()));
        out.add(invertLabel(h.isInvert()));
        out.add("[" + h.getWindow() + "] " + windowLabel(s));
        return out;
    }

    // ================================================================================ readout

    @Override
    protected void addReadout(ParentWidget<?> box, PanelSyncManager syncManager) {
        // the server setter skips on to the host's next set in the direction the button moved (left click +1, right
        // click -1, B-72), so a cycle never lands on a foreign set and a backward cycle wraps to the host's last one
        set = new IntSyncValue(machine::getSetOrdinal, v -> {
            final DirectiveSet.Host h = machine.getHost();
            final int n = DirectiveSet.VALUES.length;
            final int step = Math.floorMod(v - machine.getSetOrdinal(), n) == n - 1 ? -1 : 1;
            int o = v;
            for (int i = 0; i < n; i++) {
                final DirectiveSet s = DirectiveSet.VALUES[Math.floorMod(o, n)];
                if (h == DirectiveSet.Host.NONE || s.host == h) break;
                o += step;
            }
            machine.setSetOrdinal(o);
        }).allowC2S();
        output = new IntSyncValue(machine::getOutputOrdinal, machine::setOutputOrdinal).allowC2S();
        invert = new BooleanSyncValue(machine::isInvert, machine::setInvert).allowC2S();
        window = new IntSyncValue(machine::getWindow, machine::setWindow).allowC2S();
        code = new IntSyncValue(machine::currentCode);
        out = new IntSyncValue(machine::currentOutput);
        host = new IntSyncValue(
            () -> machine.getHost()
                .ordinal());
        controlIn = new IntSyncValue(machine::getControlIn);
        controlLine = new StringSyncValue(machine::getControlLine);
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
        syncManager.syncValue("btsDirOut", out);
        syncManager.syncValue("btsDirHostId", host);
        syncManager.syncValue("btsDirControlIn", controlIn);
        syncManager.syncValue("btsDirControlLine", controlLine);

        box.child(
            line(Y_SET, () -> setShort(set()), BTSGuiKit.VALUE, () -> hostName(host()), BTSGuiKit.LABEL)
                .tooltipDynamic(this::setTooltip)
                .tooltipAutoUpdate(true));
        // an output set: the four bits as lamps, then the code and the redstone it drives
        for (int i = 0; i < 4; i++) {
            final int bit = i;
            box.child(
                lamp(
                    i % 2 == 0 ? TEXT_X : LAMP_X2,
                    i < 2 ? Y_BITS1 : Y_BITS2,
                    i % 2 == 0 ? LAMP_X2 - TEXT_X - 4 : TEXT_X + TEXT_W - LAMP_X2,
                    () -> (code.getIntValue() & (1 << bit)) != 0,
                    () -> bitText(bit, labels[bit].getStringValue())).setEnabledIf(w -> !set().control));
        }
        box.child(
            line(
                Y_CODE,
                () -> codeLeft(host() != DirectiveSet.Host.NONE, code.getIntValue()),
                BTSGuiKit.LABEL,
                () -> codeRight(out.getIntValue()),
                BTSGuiKit.VALUE).setEnabledIf(w -> !set().control));
        // a control set: the input it reads and the setpoint that drives, then how
        box.child(
            line(
                Y_BITS1,
                () -> inputLeft(controlIn.getIntValue()),
                BTSGuiKit.VALUE,
                () -> "§b" + controlLine.getStringValue(),
                BTSGuiKit.VALUE).setEnabledIf(w -> set().control));
        box.child(
            line(Y_BITS2, () -> tr(K + "control.hint1"), BTSGuiKit.DIM, null, 0).setEnabledIf(w -> set().control));
        box.child(line(Y_CODE, () -> tr(K + "control.hint2"), BTSGuiKit.DIM, null, 0).setEnabledIf(w -> set().control));
    }

    private void setTooltip(RichTooltip t) {
        t.addLine(setName(set()));
        if (host() == DirectiveSet.Host.NONE) t.addLine("§7" + tr(K + "nomachine.tip"));
    }

    // ================================================================================ controls

    @Override
    protected void addControls(Flow column, PanelSyncManager syncManager) {
        column.child(
            cycleRow(
                set,
                DirectiveSet.VALUES.length,
                () -> tr(K + "setrow", setName(set())),
                t -> t.addLine(tr(K + "set.tip"))));
        column.child(
            cycleRow(
                output,
                DirectiveSet.Output.VALUES.length,
                () -> outputLabel(set(), DirectiveSet.Output.of(output.getIntValue())),
                t -> {
                    t.addLine(tr(K + "output.tip"));
                    for (DirectiveSet.Output o : DirectiveSet.Output.VALUES) t.addLine("§7" + outputName(o));
                    t.addLine("§7" + tr(K + "output.tip2"));
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
                new TextFieldWidget().numbersInt(MTEHatchDirective.WINDOW_MIN, MTEHatchDirective.WINDOW_MAX)
                    .value(window),
                26,
                () -> windowLabel(set()),
                t -> t.addLine(tr(K + "window.tip"))));
    }

    private DirectiveSet set() {
        return DirectiveSet.VALUES[Math.floorMod(set.getIntValue(), DirectiveSet.VALUES.length)];
    }

    private DirectiveSet.Host host() {
        final DirectiveSet.Host[] all = DirectiveSet.Host.values();
        return all[Math.floorMod(host.getIntValue(), all.length)];
    }
}
