package com.nateyburr.beyondthestars.machines.eye.star;

import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.P;
import static com.nateyburr.beyondthestars.machines.eye.field.StellarField.TH;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;

/**
 * D-49 Core Collapse, T3 → T4: Arcturus is fast-forwarded into the neutron star, with the supernova suppressed
 * (d49-build-plan.md §7.4; design §2.4). The field part is unit 1's {@link CollapseOverlay} (the prototype's
 * {@code beginCollapse} / {@code bounce} / settle, bit for bit); this class carries it through the runtime and NBT.
 * <ol>
 * <li><b>Fast-forward:</b> each second the runtime doses up to 100 L of Tachyon Rich Temporal Fluid from the Field
 * sockets; {@code Mc = 0.5 + tach/100,000}, the envelope swells P and Θ by {@code 2(Mc − 0.5)/0.94}. Arcturus's
 * upkeep and output continue. About 940 s at the cap.</li>
 * <li><b>Bounce</b> at 94,000 L: 5,000 L of HSM is taken as the neutron seed (gone: supernova, "no neutron seed");
 * every 1,000 L of molten SpaceTime buffered absorbs one point of the {@code BOUNCE_SHOCK} (150 adopted; up to 90
 * points, 90,000 L); Integrity −20. P or Θ above +100: SUPERNOVA, Arcturus lost, Time, HSM and SpaceTime spent, the
 * core kept.</li>
 * <li><b>Settle:</b> the proto-neutron star lights under T4 rules at (0, +60, +30); Stable or better for 60
 * consecutive seconds within 180 s unlocks T4 and consumes the core.</li>
 * </ol>
 */
public final class CoreCollapse extends EyeTransition {

    /** Time dosed per second at most (plan §7.4). */
    public static final double DOSE_CAP = 100;

    public final CollapseOverlay co;

    CoreCollapse(double bounceShock) {
        super(Type.CORE_COLLAPSE);
        co = new CollapseOverlay(bounceShock);
    }

    @Override
    public void addDrift(FieldState s, StepInputs in, double[] dp) {
        co.addDrift(s, in, dp);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs in, StepResult out) {
        t++;
        co.tick(s, zone, in, out);
    }

    /** P and Θ the bounce would leave, from the gauges now and this SpaceTime buffer (readout). */
    public double[] predictedAfterBounce(FieldState s, double spaceTime) {
        final double shock = co.predictedShock(spaceTime);
        return new double[] { Math.max(-120, Math.min(120, s.x[P] + shock)),
            Math.max(-120, Math.min(120, s.x[TH] + shock)) };
    }

    /** GUI: fast-forward (1), the bounce (2), settle (3). */
    @Override
    public int phase() {
        return Math.max(1, Math.min(3, co.phase));
    }

    @Override
    public int phases() {
        return 3;
    }

    @Override
    public String phaseLine() {
        if (co.phase == 1) {
            return String.format(
                Locale.ROOT,
                "Core Collapse, phase 1 (fast-forward): M_c %.3f / %.2f, Time %,.0f / %,.0f L (up to %.0f L/s from the"
                    + " Field sockets)",
                co.Mc,
                CollapseOverlay.MC_BOUNCE,
                co.tach,
                CollapseOverlay.TACH_BOUNCE,
                DOSE_CAP);
        }
        return String.format(
            Locale.ROOT,
            "Core Collapse, phase 3 (settle): Stable streak %d / %d s, %d / %d s used; the bounce absorbed %.0f points"
                + " (P %s -> %s, Θ %s -> %s)",
            co.streak,
            CollapseOverlay.SETTLE_STREAK,
            co.st,
            CollapseOverlay.SETTLE_LIMIT,
            co.absorbed,
            StellarField.fmtSigned(co.shockP0),
            StellarField.fmtSigned(co.shockP),
            StellarField.fmtSigned(co.shockTh0),
            StellarField.fmtSigned(co.shockTh));
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        nbt.setDouble("shock", co.bounceShock);
        nbt.setByte("phase", (byte) co.phase);
        nbt.setDouble("tach", co.tach);
        nbt.setDouble("Mc", co.Mc);
        nbt.setInteger("st", co.st);
        nbt.setInteger("streak", co.streak);
        nbt.setBoolean("bounced", co.bounced);
        nbt.setDouble("absorbed", co.absorbed);
        nbt.setDouble("shockP0", co.shockP0);
        nbt.setDouble("shockTh0", co.shockTh0);
        nbt.setDouble("shockP", co.shockP);
        nbt.setDouble("shockTh", co.shockTh);
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        co.phase = nbt.hasKey("phase") ? nbt.getByte("phase") : 1;
        co.tach = nbt.getDouble("tach");
        co.Mc = nbt.hasKey("Mc") ? nbt.getDouble("Mc") : 0.5;
        co.st = nbt.getInteger("st");
        co.streak = nbt.getInteger("streak");
        co.bounced = nbt.getBoolean("bounced");
        co.absorbed = nbt.getDouble("absorbed");
        co.shockP0 = nbt.getDouble("shockP0");
        co.shockTh0 = nbt.getDouble("shockTh0");
        co.shockP = nbt.getDouble("shockP");
        co.shockTh = nbt.getDouble("shockTh");
    }
}
