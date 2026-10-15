package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.InfallOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;

/**
 * P5 The Infall, T8 → T9 (p5-t9-t10.md §2.8). The field part is {@link InfallOverlay} (the prototype's
 * {@code beginInfall} and its 'in' rules, bit for bit); this class carries it through the runtime and NBT. 1,800 s: the
 * object becomes the Great Attractor at once (T9 field rules), V ramps in over 600 s, the demand ramps 6 → 24 Bu/t,
 * the charge drains Superdense Ergomatter 10 L/s (× p), a forced Galactic Flow of 6 arrives at 1,200 s; then Stable for
 * 120 s in a row (from 1,500 s, after the flow) lights the Attractor, unlocks T9 and consumes the Attractor Seed Core.
 * A failure is soft (Q-P5-5): the Milky Way is thrown back out still lit at T8, Integrity 50; the charge is spent and
 * the core stays in escrow.
 */
public final class Infall extends EyeTransition {

    InfallOverlay in;

    Infall() {
        super(Type.INFALL);
    }

    public InfallOverlay overlay() {
        return in;
    }

    @Override
    void onBegin(FieldState s) {
        in = InfallOverlay.begin(s);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs i, StepResult out) {
        return in == null ? super.drift(t, s, i, out) : in.drift(t, s, i, out);
    }

    @Override
    public void onFlowEnded() {
        if (in != null) in.onFlowEnded();
    }

    @Override
    public void onFail(FieldState s, StepInputs i, StepResult out) {
        if (in != null) in.onFail(s, i, out);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs i, StepResult out) {
        t++;
        if (in != null) in.tick(s, zone, i, out);
    }

    /** The burst demand now, Bu/t (6 → 24 over 1,800 s; also β's normaliser). */
    public double demand() {
        return in == null ? InfallOverlay.DEMAND_FROM : in.demand();
    }

    /** V's ramp-in, 0..1 over the first 600 s (the render's vRamp). */
    public double vRamp() {
        return in == null ? 0 : in.vRamp();
    }

    public double progress() {
        return in == null ? 0 : Math.min(1, in.st / (double) InfallOverlay.DEADLINE);
    }

    @Override
    public int phase() {
        return in == null ? 1 : in.phase();
    }

    @Override
    public int phases() {
        return 3;
    }

    @Override
    public String phaseLine() {
        if (in == null) return "The Infall, starting";
        if (in.phase() < 3) {
            final int toFlow = AttractorField.FORCED_AT - in.st;
            return String.format(
                Locale.ROOT,
                "The Infall, %d / %d s: demand %s (6 -> 24 Bu/t), V ramp %.0f %%, Superdense Ergomatter 10 L/s x p; %s",
                in.st,
                InfallOverlay.DURATION,
                BTSFormat.buEuShort(in.demand()),
                100 * in.vRamp(),
                toFlow > 0 ? "the forced flow of 6 arrives in " + toFlow + " s"
                    : "the forced flow is in the core (hold it)");
        }
        return String.format(
            Locale.ROOT,
            "The Infall, settle: Stable streak %d / %d s, deadline %d s (now %d s)",
            in.streak,
            InfallOverlay.SETTLE_STREAK,
            InfallOverlay.DEADLINE,
            in.st);
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        if (in == null) return;
        nbt.setInteger("st", in.st);
        nbt.setInteger("streak", in.streak);
        nbt.setBoolean("flowDone", in.flowDone);
        nbt.setBoolean("begun", true);
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        if (!nbt.getBoolean("begun")) return;
        in = InfallOverlay.restore(nbt.getInteger("st"), nbt.getInteger("streak"), nbt.getBoolean("flowDone"));
    }
}
