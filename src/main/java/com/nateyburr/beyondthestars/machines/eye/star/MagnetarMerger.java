package com.nateyburr.beyondthestars.machines.eye.star;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.PI;

import java.util.LinkedHashMap;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.EventDef;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * D-49 Magnetar Merger, T5 → T6 and every re-spawn (d49-build-plan.md §7.6, plan reading R-6; design §2.7). The
 * runtime takes the merger charge (White Dwarf Matter 18,432 + Black Dwarf Matter 4,608 + Antipolarization Agent
 * 100,000, × p) and lights the magnetar at x = 0, I = 100, A = 0 under T6 rules, with the Agent upkeep cycles running.
 * The one inverted challenge: the dynamo winds the polarity up (Π drift +0.5/s on top of T6's runaway gain) and the
 * giant flares are held back. During the imprint the Agent upkeep does not charge the flare shield (ruling 12's +2r/s):
 * only Agent trims do, so a dose above the band's middle trims Π back into it instead of the shield pinning Π at 0
 * (B-71).
 * <ol>
 * <li><b>Imprint:</b> hold Π inside +60..+90 for 120 consecutive seconds, within 600 s.</li>
 * <li><b>Release:</b> then bring Π back inside ±40 within 60 s. The magnetar is LIT under full T6 rules (its flares
 * scheduled), T6 unlocks and the core is consumed.</li>
 * </ol>
 * Breach, Integrity 0 or a timeout loses the charge and keeps the core; the Eye goes DARK.
 */
public final class MagnetarMerger extends EyeTransition {

    public static final double IMPRINT_DRIFT = 0.5;
    public static final double BAND_LO = 60, BAND_HI = 90;
    public static final int HOLD = 120, IMPRINT_TIMEOUT = 600;
    public static final double RELEASE_BAND = 40;
    public static final int RELEASE_TIMEOUT = 60;

    /** 1 imprint, 2 release. */
    public int phase = 1;
    /** Consecutive imprint seconds in band. */
    public int hold;
    /** Release seconds used. */
    public int t2;

    MagnetarMerger() {
        super(Type.MAGNETAR_MERGER);
    }

    /** No giant flares during the merger. */
    @Override
    public EventDef event(TierParams tier, FieldState s) {
        return null;
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        dp[PI] += IMPRINT_DRIFT;
    }

    /** The imprint: the upkeep's shield charge is off (B-71); the release: T6's own rate. */
    @Override
    public double shieldPerUpkeep(TierParams t) {
        return phase == 1 ? 0 : StellarField.SHIELD_PER_UPKEEP;
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        final double pi = s.x[PI];
        if (phase == 1) {
            hold = pi >= BAND_LO && pi <= BAND_HI ? hold + 1 : 0;
            if (hold >= HOLD) {
                phase = 2;
                t2 = 0;
            } else if (t >= IMPRINT_TIMEOUT) {
                out.fail = "the imprint timed out (Π not held inside +60..+90 for 120 s within 600 s)";
            }
            return;
        }
        t2++;
        if (Math.abs(pi) <= RELEASE_BAND) {
            StellarField.scheduleEvent(s, in.dialSetpoint, in.tt); // the flares begin
            out.transitionSuccess = true;
        } else if (t2 >= RELEASE_TIMEOUT) {
            out.fail = "the release timed out (Π not back inside ±40 within 60 s)";
        }
    }

    /**
     * Debug rigs only. The prototype's T6 rig doses Agent whenever |Π| > 5 or 10, which would never let the imprint
     * happen; during the merger its Agent rule becomes: imprint, a small Agent dose above +68 (fine) / +75 (bang) so
     * Π sits in the band; release, Agent at the cap while Π is above +30.
     */
    @Override
    public void adjustRig(LinkedHashMap<String, Double> raw, FieldState s, FieldRig.Mode mode) {
        if (mode == null || mode == FieldRig.Mode.OFF) return;
        raw.remove("Agent");
        final double e = s.prevE[PI];
        if (phase == 1) {
            if (e > (mode == FieldRig.Mode.FINE ? 68 : 75)) raw.put("Agent", mode == FieldRig.Mode.FINE ? 1.0 : 2.0);
        } else if (e > 30) {
            raw.put("Agent", 5.0);
        }
    }

    @Override
    public String phaseLine() {
        if (phase == 1) {
            return "Magnetar Merger, imprint: hold Π inside +60..+90 for " + HOLD
                + " s: "
                + hold
                + " / "
                + HOLD
                + " s held, "
                + t
                + " / "
                + IMPRINT_TIMEOUT
                + " s used (Π drifts +0.5/s; no flares)";
        }
        return "Magnetar Merger, release: bring Π back inside ±40 within " + RELEASE_TIMEOUT
            + " s: "
            + t2
            + " / "
            + RELEASE_TIMEOUT
            + " s used";
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        nbt.setByte("phase", (byte) phase);
        nbt.setInteger("hold", hold);
        nbt.setInteger("t2", t2);
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        phase = nbt.hasKey("phase") ? nbt.getByte("phase") : 1;
        hold = nbt.getInteger("hold");
        t2 = nbt.getInteger("t2");
    }
}
