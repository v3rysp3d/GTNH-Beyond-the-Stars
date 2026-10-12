package com.nateyburr.beyondthestars.machines.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller GUI cleanup (controller-gui-spec.md §4.5): a machine that cannot run shows one row per requirement, met or
 * not, instead of the first missing one in a sentence. The SERVER builds the lines (the checks read machine state and
 * consume nothing); {@link BTSRows.Builder#checks} encodes them into the terminal's one row string and the client
 * draws them from its fixed row pool ({@link BTSGuiKit#rowPool}). Texts are {@link BTSText} encodings.
 */
public final class BTSChecklist {

    /** ✓ met (green), ✗ missing (red), ! advisory (amber). */
    public enum State {
        OK,
        BAD,
        WARN
    }

    /** One requirement: state, label, value ({@code have / need}), an optional fill line and a tooltip. */
    public static final class CheckLine {

        public final State state;
        public final String label;
        public final String value;
        /** 0..1 for the thin fill line under the row, or −1 for none. */
        public final float frac;
        /** Tooltip lines (encoded texts), possibly empty. */
        public final List<String> tooltip;

        public CheckLine(State state, String label, String value, float frac, List<String> tooltip) {
            this.state = state;
            this.label = label == null ? "" : label;
            this.value = value == null ? "" : value;
            this.frac = frac;
            this.tooltip = tooltip == null ? Collections.<String>emptyList() : tooltip;
        }

        public boolean met() {
            return state == State.OK;
        }
    }

    private final List<CheckLine> lines = new ArrayList<>();

    /** Adds a line; returns this. */
    public BTSChecklist add(State state, String label, String value, float frac, String... tooltip) {
        final List<String> tip = new ArrayList<>();
        for (String t : tooltip) if (t != null && !t.isEmpty()) tip.add(t);
        lines.add(new CheckLine(state, label, value, frac, tip));
        return this;
    }

    /** OK when {@code ok}, else BAD. */
    public BTSChecklist add(boolean ok, String label, String value, float frac, String... tooltip) {
        return add(ok ? State.OK : State.BAD, label, value, frac, tooltip);
    }

    public List<CheckLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public int size() {
        return lines.size();
    }

    /** Lines that are OK. */
    public int met() {
        int n = 0;
        for (CheckLine l : lines) if (l.met()) n++;
        return n;
    }

    /** True when no line is BAD (WARN lines are advisory). */
    public boolean allMet() {
        for (CheckLine l : lines) if (l.state == State.BAD) return false;
        return true;
    }

    /** "{met} of {total}" in OK colour when all are met, WARN colour otherwise (a section's right text). */
    public String countText() {
        return BTSText
            .cat(BTSText.lit(allMet() ? "§a" : "§6"), BTSText.lang("beyondthestars.gui.kit.of", met(), size()));
    }

    /** A fill fraction for have/need (clamped 0..1; 1 when nothing is needed). */
    public static float frac(double have, double need) {
        if (need <= 0) return 1;
        return (float) Math.max(0, Math.min(1, have / need));
    }
}
