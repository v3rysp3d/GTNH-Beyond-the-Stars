package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Locale;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.InflationOverlay;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;

/**
 * P5 Inflation, T9 → T10 (p5-t9-t10.md §6.3). The field part is {@link InflationOverlay}; this class carries it
 * through the runtime and NBT. 900 s: unfolding under T9 rules (the demand ramps 24 → 96 Bu/t); at 300 s the universe
 * inflates (T10 rules, H at +85, Integrity −20; Dark Matter 30.72 L/s × p from then); at 600 s recombination, then
 * Stable for 120 s in a row inside 600-900 s lights the first universe, unlocks T10 and consumes the Universal Upgrade
 * Core. A failure is soft: the Great Attractor is relit at T9 with Integrity 50; the charge is spent, the core kept.
 */
public final class Inflation extends EyeTransition {

    InflationOverlay inf;
    private final double shock;

    Inflation(double shock) {
        super(Type.INFLATION);
        this.shock = shock;
    }

    public InflationOverlay overlay() {
        return inf;
    }

    @Override
    void onBegin(FieldState s) {
        inf = new InflationOverlay(shock);
    }

    @Override
    public double[] drift(TierParams t, FieldState s, StepInputs i, StepResult out) {
        return inf == null ? super.drift(t, s, i, out) : inf.drift(t, s, i, out);
    }

    @Override
    public void onFail(FieldState s, StepInputs i, StepResult out) {
        if (inf != null) inf.onFail(s, i, out);
    }

    @Override
    public void tick(FieldState s, int zone, StepInputs i, StepResult out) {
        t++;
        if (inf != null) inf.tick(s, zone, i, out);
    }

    public double demand() {
        return inf == null ? InflationOverlay.DEMAND_FROM : inf.demand();
    }

    public double progress() {
        return inf == null ? 0 : Math.min(1, inf.st / (double) InflationOverlay.DEADLINE);
    }

    @Override
    public int phase() {
        return inf == null ? 1 : inf.phase;
    }

    @Override
    public int phases() {
        return 3;
    }

    @Override
    public String phaseLine() {
        if (inf == null) return "Inflation, starting";
        switch (inf.phase) {
            case 1:
                return String.format(
                    Locale.ROOT,
                    "Inflation, unfolding %d / %d s: demand %s (24 -> 96 Bu/t), T9 rules; H appears at +%.0f",
                    inf.st,
                    InflationOverlay.INFLATE_AT,
                    BTSFormat.buEuShort(inf.demand()),
                    inf.shock);
            case 2:
                return String.format(
                    Locale.ROOT,
                    "Inflation, %d / %d s: H too high, pull it down with Dark Matter; demand %s; Dark Matter 30.72 L/s x p",
                    inf.st,
                    InflationOverlay.RECOMBINE_AT,
                    BTSFormat.buEuShort(inf.demand()));
            default:
                return String.format(
                    Locale.ROOT,
                    "Inflation, recombination: Stable streak %d / %d s, %d / %d s",
                    inf.streak,
                    InflationOverlay.SETTLE_STREAK,
                    inf.st,
                    InflationOverlay.DEADLINE);
        }
    }

    @Override
    void writeExtra(NBTTagCompound nbt) {
        if (inf == null) return;
        nbt.setDouble("infShock", inf.shock);
        nbt.setByte("phase", (byte) inf.phase);
        nbt.setInteger("st", inf.st);
        nbt.setInteger("streak", inf.streak);
    }

    @Override
    void readExtra(NBTTagCompound nbt) {
        if (!nbt.hasKey("infShock")) return;
        inf = InflationOverlay
            .restore(nbt.getDouble("infShock"), nbt.getByte("phase"), nbt.getInteger("st"), nbt.getInteger("streak"));
    }
}
