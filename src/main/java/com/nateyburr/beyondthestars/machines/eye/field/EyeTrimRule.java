package com.nateyburr.beyondthestars.machines.eye.field;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.OM;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * AUTOMATION PASS: which trims the stability field wants now, as the Directive Hatch's Eye Trims bits (one bit per
 * trim, in the tier's trim-table order) - the conditions of the prototype's bang-bang rig ({@link FieldRig#ctrl}) with
 * the threshold a free window w, and no rates: a redstone rig opens a fixed-rate valve per bit. Pure maths (no
 * Minecraft types), so {@code machines.directive.DirectiveRigSim} runs the same rule offline.
 * <ul>
 * <li>T1, T2: Helium when P > +w, Hydrogen when P < −w.</li>
 * <li>T3: Helium when P > +w or Θ < −w; Hydrogen when P < −w; Super Coolant when P > +w, Θ > +w, or a helium flash is
 * warned while Θ > −30 (the rig's flash valve).</li>
 * <li>T4, T6: Space when P > +w, SpaceTime when P < −w, Super Coolant when Θ > +w, Tengam when Ω > +w; T6's fifth
 * trim, the Antipolarization Agent, when |Π| > w or a flare is warned ({@link #extra}).</li>
 * <li>T5: DSSF when Θ > +w or P > +w, Raw Star Matter when Θ < −w, Helium when P < −w.</li>
 * </ul>
 */
public final class EyeTrimRule {

    private EyeTrimRule() {}

    /** True when the field wants trim {@code kind} on a star of table {@code t}, from last second's errors. */
    public static boolean wants(TierParams t, TrimDef.Kind kind, double[] E, int warn, double w) {
        switch (t.n) {
            case 1:
            case 2:
                return kind == TrimDef.Kind.HELIUM ? E[P] > w : kind == TrimDef.Kind.HYDROGEN && E[P] < -w;
            case 3:
                switch (kind) {
                    case HELIUM:
                        return E[P] > w || E[TH] < -w;
                    case HYDROGEN:
                        return E[P] < -w;
                    case SUPER_COOLANT:
                        return E[P] > w || E[TH] > w || (warn != 0 && E[TH] > -30);
                    default:
                        return false;
                }
            case 5:
                switch (kind) {
                    case DSSF:
                        return E[TH] > w || E[P] > w;
                    case RAW_STAR_MATTER:
                        return E[TH] < -w;
                    case HELIUM:
                        return E[P] < -w;
                    default:
                        return false;
                }
            default: // 4 and 6
                switch (kind) {
                    case SPACE:
                        return E[P] > w;
                    case SPACETIME:
                        return E[P] < -w;
                    case SUPER_COOLANT:
                        return E[TH] > w;
                    case TENGAM:
                        return E[OM] > w;
                    case AGENT:
                        return warn != 0 || Math.abs(E[PI]) > w;
                    default:
                        return false;
                }
        }
    }

    /** The Trims code: bit i set when the table's trim i (0..3) is wanted. */
    public static int trimBits(TierParams t, double[] E, int warn, double w) {
        final List<TrimDef> trims = t.trims();
        int bits = 0;
        for (int i = 0; i < Math.min(4, trims.size()); i++) {
            if (wants(t, trims.get(i).kind, E, warn, w)) bits |= 1 << i;
        }
        return bits;
    }

    /**
     * The Status set's bit 3: the table's fifth trim is wanted (T6: the Agent), else an event is warned (T3: the
     * flash, open the bigger Super Coolant valve).
     */
    public static boolean extra(TierParams t, double[] E, int warn, double w) {
        final List<TrimDef> trims = t.trims();
        if (trims.size() > 4) return wants(t, trims.get(4).kind, E, warn, w);
        return warn != 0;
    }

    /**
     * A plain redstone rig's valve for trim-table position {@code i}, units/s at parallel 1: the prototype's bang-bang
     * rates (the docs' and the tests' standard rig; every rate is within the trim's cap).
     */
    public static double rigRate(int tier, int i) {
        final double[] r;
        switch (tier) {
            case 1:
                r = new double[] { 10, 10 };
                break;
            case 2:
                r = new double[] { 20, 20 };
                break;
            case 3:
                r = new double[] { 10, 10, 10 };
                break;
            case 5:
                r = new double[] { 10, 5, 10 };
                break;
            default: // 4, 6
                r = new double[] { 10, 10, 10, 5 };
                break;
        }
        return i >= 0 && i < r.length ? r[i] : 0;
    }

    /** The Status bit-3 event valve, units/s at parallel 1: T3 more Super Coolant (10), T6 the Agent (5). */
    public static double rigExtraRate(int tier) {
        return tier == 3 ? 10 : tier == 6 ? 5 : 0;
    }

    /** The label of trim bit i (0..3) on table {@code t}, e.g. "Helium (P > +w)"; "" when the tier has none. */
    public static String label(TierParams t, int i) {
        final List<TrimDef> trims = t.trims();
        if (i < 0 || i >= Math.min(4, trims.size())) return "(unused on T" + t.n + ")";
        return trims.get(i).kind.displayName;
    }

    /**
     * A redstone gate rig's request for one second (the offline proof, {@code machines.directive.DirectiveRigSim}):
     * every wanted trim's
     * valve at its fixed rate ({@code rate[i]} units/s for table trim i), plus {@code extraRate} units on the Status
     * bit 3 valve (T3: Super Coolant; T6: the Agent). Keys are the prototype's trim keys, in table order.
     */
    public static LinkedHashMap<String, Double> gateRig(TierParams t, int trimBits, boolean extraBit, double[] rate,
        double extraRate) {
        final LinkedHashMap<String, Double> u = new LinkedHashMap<>();
        final List<TrimDef> trims = t.trims();
        for (int i = 0; i < Math.min(4, trims.size()); i++) {
            if ((trimBits & (1 << i)) != 0) u.put(trims.get(i).kind.key, rate[i]);
        }
        if (extraBit) {
            final String key = trims.size() > 4 ? trims.get(4).kind.key
                : t.n == 3 ? TrimDef.Kind.SUPER_COOLANT.key : null;
            if (key != null) {
                final Double old = u.get(key);
                u.put(key, (old == null ? 0 : old) + extraRate);
            }
        }
        return u;
    }
}
