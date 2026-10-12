package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.EventDef;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.eye.field.MagnetarCollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * P4 Magnetar Collapse, T6 → T7 (quasar-p4.md §5.2). The field part is the engine unit's
 * {@link MagnetarCollapseOverlay} (the prototype's {@code beginMC} / {@code transTick} 'mc' / {@code collapse}, bit for
 * bit); this class carries it through the runtime and NBT.
 * <ol>
 * <li><b>Overfeed</b> (300 s, T6 rules): the charge, White Dwarf Matter 61.44 + Black Dwarf Matter 15.36 L/s (× p), on
 * top of the magnetar's upkeep; Ω and Π wind up by +2·t/300; a 30 s countdown from 270 s.</li>
 * <li><b>Collapse</b> (300 s): the tier becomes T7; the shock P +{@link MagnetarCollapseOverlay#DEFAULT_SHOCK}, less 10
 * per Bu/t delivered that second above D₇; Integrity −20. P above +100 loses the magnetar (the charge spent, the core
 * kept).</li>
 * <li><b>Halo capture</b> (600 s, T7 rules): the demand ramps in; a forced minor merger at 450 s; Stable for 60 s in a
 * row within 420-600 s unlocks T7 and consumes the Intergalactic Upgrade Core.</li>
 * </ol>
 */
public final class MagnetarCollapse extends EyeTransition {

    public final MagnetarCollapseOverlay mc;

    MagnetarCollapse(double shock) {
        super(Type.MAGNETAR_COLLAPSE);
        mc = new MagnetarCollapseOverlay(shock);
    }

    @Override
    public EventDef event(TierParams t, FieldState s) {
        return mc.event(t, s);
    }

    @Override
    public int[] gauges(TierParams t) {
        return mc.gauges(t);
    }

    @Override
    public double gain(TierParams t, int k) {
        return mc.gain(t, k);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs in) {
        return mc.drift(t, s, in);
    }

    @Override
    public double sigma(TierParams t, int k, StepInputs in) {
        return mc.sigma(t, k, in);
    }

    @Override
    public int addEvents(FieldState s, StepInputs in, double[] E, double q, StepResult out) {
        return mc.addEvents(s, in, E, q, out);
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        mc.addDrift(s, in, dp);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        mc.tick(s, zone, in, out);
    }

    /** The burst demand now, Bu/t (the halo capture's ramp; D₇ in the overfeed). */
    public double demand() {
        return mc.demand();
    }

    /** β's normaliser (D₇ throughout, finding F-1). */
    public double normDemand() {
        return mc.normDemand();
    }

    /** True in the overfeed (the magnetar still burns under T6 rules). */
    public boolean overfeed() {
        return mc.phase == 1;
    }

    @Override
    public String phaseLine() {
        if (mc.phase == 1) {
            final int left = MagnetarCollapseOverlay.OVERFEED_SECONDS - mc.st;
            return String.format(
                Locale.ROOT,
                "Magnetar Collapse, overfeed: M_ns %.2f / %.1f M☉, %d s to the collapse%s (WDM 61.44 + BDM 15.36 L/s"
                    + " x p; a purge timed onto the collapse second cancels the shock)",
                mc.Mns,
                MagnetarCollapseOverlay.MNS_START + MagnetarCollapseOverlay.MNS_RISE,
                left,
                left <= MagnetarCollapseOverlay.OVERFEED_SECONDS - MagnetarCollapseOverlay.COUNTDOWN_AT ? " (COUNTDOWN)"
                    : "");
        }
        return String.format(
            Locale.ROOT,
            "Magnetar Collapse, halo capture: %d / %d s, Stable streak %d / %d s (counts from %d s); the shock was"
                + " P %s -> %s (%.1f Bu/t cancelled %.0f)",
            mc.st,
            MagnetarCollapseOverlay.CAPTURE_SECONDS,
            mc.streak,
            MagnetarCollapseOverlay.CAPTURE_STREAK,
            MagnetarCollapseOverlay.CAPTURE_FROM,
            StellarField.fmtSigned(mc.shockP0),
            StellarField.fmtSigned(mc.shockP),
            mc.shockG,
            mc.shockCancel);
    }

    /** 0..1 for the GUI bar: the overfeed is the first third, the capture the rest. */
    public double progress() {
        return mc.phase == 1 ? mc.st / (3.0 * MagnetarCollapseOverlay.OVERFEED_SECONDS)
            : 1 / 3.0 + 2 / 3.0 * Math.min(1, mc.st / (double) MagnetarCollapseOverlay.CAPTURE_SECONDS);
    }

    /** Seconds to the collapse while overfeeding (the event countdown), else −1. */
    public int secondsToCollapse() {
        return mc.phase == 1 ? MagnetarCollapseOverlay.OVERFEED_SECONDS - mc.st : -1;
    }

    /** The shock the collapse would land with {@code gEye} Bu/t delivered on its second (GUI). */
    public double predictedShock(double gEye) {
        return mc.predictedShock(gEye);
    }

    /** The burst surplus over D₇ that cancels the whole shock, Bu/t. */
    public double fullCancelBu() {
        return GalaxyField.D7 + mc.shock / MagnetarCollapseOverlay.CANCEL_PER_BU;
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        nbt.setDouble("mcShock", mc.shock);
        nbt.setByte("phase", (byte) mc.phase);
        nbt.setInteger("st", mc.st);
        nbt.setInteger("streak", mc.streak);
        nbt.setDouble("Mns", mc.Mns);
        nbt.setBoolean("collapsed", mc.collapsedOnce);
        nbt.setDouble("shockP0", mc.shockP0);
        nbt.setDouble("shockP", mc.shockP);
        nbt.setDouble("shockG", mc.shockG);
        nbt.setDouble("shockCancel", mc.shockCancel);
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        mc.phase = nbt.hasKey("phase") ? nbt.getByte("phase") : 1;
        mc.st = nbt.getInteger("st");
        mc.streak = nbt.getInteger("streak");
        mc.Mns = nbt.hasKey("Mns") ? nbt.getDouble("Mns") : MagnetarCollapseOverlay.MNS_START;
        mc.collapsedOnce = nbt.getBoolean("collapsed");
        mc.shockP0 = nbt.getDouble("shockP0");
        mc.shockP = nbt.getDouble("shockP");
        mc.shockG = nbt.getDouble("shockG");
        mc.shockCancel = nbt.getDouble("shockCancel");
    }
}
