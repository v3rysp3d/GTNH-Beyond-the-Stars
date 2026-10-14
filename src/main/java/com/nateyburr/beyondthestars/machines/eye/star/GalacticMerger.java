package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.EventDef;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalacticMergerOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * P4 Galactic Merger, T7 → T8 (quasar-p4.md §5.3). The field part is the engine unit's {@link GalacticMergerOverlay}
 * (the prototype's {@code beginGM} / {@code transTick} 'gm', bit for bit); this class carries it through the runtime
 * and NBT. 1,800 s: the burst demand ramps 1.5 → 6 Bu/t and the Ergomatter charge feeds 5 L/s (× p); three warned
 * pericentre passes at 600 / 1,200 / 1,800 s (W ±12 and P +5 per s for 5 s); at the first pass the tier becomes T8
 * (Λ ramps in). Then Stable for 120 s in a row within 300 s lights the Milky Way, unlocks T8 and consumes the
 * Supermassive Seed Core. A failure loses Triangulum and the charge; the core stays in escrow.
 */
public final class GalacticMerger extends EyeTransition {

    /** Null until {@link #onBegin} draws the passes' signs (or {@link #readExtra} restores them). */
    GalacticMergerOverlay gm;

    GalacticMerger() {
        super(Type.GALACTIC_MERGER);
    }

    /** The overlay, or null before it began. */
    public GalacticMergerOverlay overlay() {
        return gm;
    }

    @Override
    void onBegin(FieldState s) {
        gm = GalacticMergerOverlay.begin(s);
    }

    @Override
    public EventDef event(TierParams t, FieldState s) {
        return gm == null ? t.event : gm.event(t, s);
    }

    @Override
    public double gain(TierParams t, int k) {
        return gm == null ? t.gain(k) : gm.gain(t, k);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in) {
        return gm == null ? super.drift(t, s, in) : gm.drift(t, s, in);
    }

    @Override
    public int addEvents(FieldState s, StepInputs in, double[] E, double q, StepResult out) {
        return gm == null ? 0 : gm.addEvents(s, in, E, q, out);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        if (gm != null) gm.tick(s, zone, in, out);
    }

    /** The burst demand now, Bu/t: 1.5 + 4.5·min(1, t/1,800) (also β's normaliser). */
    public double demand() {
        return gm == null ? GalacticMergerOverlay.DEMAND_FROM : gm.demand();
    }

    /** The pass whose 60 s warning runs (0..2), or -1. */
    public int warnedPass() {
        return gm == null ? -1 : gm.warnedPass();
    }

    /** Seconds to the next pass, or -1 after the last. */
    public int secondsToNextPass() {
        if (gm == null) return -1;
        for (int p : GalacticMergerOverlay.PASSES) if (gm.st < p) return p - gm.st;
        return -1;
    }

    /** The sign of the next pass (+1 / −1), 0 after the last. */
    public int nextPassSign() {
        if (gm == null) return 0;
        for (int i = 0; i < 3; i++) if (gm.st < GalacticMergerOverlay.PASSES[i]) return gm.signs[i];
        return 0;
    }

    /** 0..1 for the GUI bar: the passes are 6/7, the settle the rest. */
    public double progress() {
        if (gm == null) return 0;
        final double total = GalacticMergerOverlay.DURATION + GalacticMergerOverlay.SETTLE_WINDOW;
        return Math.min(1, gm.st / total);
    }

    @Override
    public String phaseLine() {
        if (gm == null) return "Galactic Merger, starting";
        if (gm.phase == 1) {
            final int next = secondsToNextPass();
            final int sign = nextPassSign();
            return String.format(
                Locale.ROOT,
                "Galactic Merger, %d / %d s: demand %s (1.5 -> 6 Bu/t), Ergomatter 5 L/s x p; %s",
                gm.st,
                GalacticMergerOverlay.DURATION,
                com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(gm.demand()),
                next < 0 ? "the passes are done"
                    : "pericentre pass in " + next
                        + " s ("
                        + (sign < 0 ? "-" : "+")
                        + (gm.warnedPass() >= 0 ? ", WARNED" : "")
                        + ")");
        }
        return String.format(
            Locale.ROOT,
            "Galactic Merger, settle: Stable streak %d / %d s, %d / %d s used",
            gm.streak,
            GalacticMergerOverlay.SETTLE_STREAK,
            gm.st - GalacticMergerOverlay.DURATION,
            GalacticMergerOverlay.SETTLE_WINDOW);
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        if (gm == null) return;
        nbt.setByte("phase", (byte) gm.phase);
        nbt.setInteger("st", gm.st);
        nbt.setInteger("streak", gm.streak);
        nbt.setIntArray("signs", gm.signs.clone());
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        if (!nbt.hasKey("signs")) return;
        final int[] signs = nbt.getIntArray("signs");
        if (signs.length != 3) return;
        gm = GalacticMergerOverlay.restore(
            nbt.hasKey("phase") ? nbt.getByte("phase") : 1,
            nbt.getInteger("st"),
            nbt.getInteger("streak"),
            signs);
    }
}
