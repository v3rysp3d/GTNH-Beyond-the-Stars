package com.nateyburr.beyondthestars.machines.eye.star;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.field.UniverseField;
import com.nateyburr.beyondthestars.machines.quasar.engine.DeepLink;

import gregtech.api.enums.Materials;

/**
 * P5: the Eye's T9 / T10 runtime state beside the field (p5-t9-t10.md §2, §3, §6), as {@link EyeGalaxy} is P4's: the
 * pellet beam's inputs and its fractional pellet carry, the Deep Link (T8 → T9 milestone) window, the flow accept
 * limit, the universe charge owed, and the last events the GUI, the scanner and the render read. The field part (the
 * beam, the flow, the universe) is in {@code FieldState} and saved by {@link EyeNbt}.
 * <p>
 * Trap 2: the accept limit is persistent (item and tile NBT, a setting); the rest is the live star's (tile NBT).
 */
public final class EyeAttractor {

    /** The pellet stock that counts as low: under this many seconds of fire at the current rate (Flow bit 4). */
    public static final double PELLETS_LOW_SECONDS = 120;

    // ---------------------------------------------------------------- persistent (item NBT)
    /** The flow accept limit (0-12), or -1 for "the hold rating" (the default, Q-P5-6). */
    int acceptSet = -1;

    // ---------------------------------------------------------------- live (tile NBT)
    /** The Deep Link (T8 → T9): the Eye's total with the collar part, and the beam's ρ, rolled each T8 second. */
    final DeepLink deep = new DeepLink();
    /** Pellets fired but not yet taken (whole pellets are drawn from the injector). */
    double pelletCarry;
    /** A universe's Dark Matter charge still owed, litres (T10 waits for it). */
    long uniOwed;
    /** The last capture (render: the galaxies spiral into the halo), as a field clock second, and how many. */
    long captureT = -1;
    int captureN;
    /** The last universe's end: the field clock second and how (1 last light, 2 Big Rip, 3 Big Crunch). */
    long endT = -1;
    int endKind;
    /** The last flow's outcome for the GUI ("" none yet). */
    String lastFlow = "";
    /** The last declined group: when it streamed past (field clock), its size and side (render). */
    long passT = -1;
    int passSize, passSign;

    // ---------------------------------------------------------------- transient (this second)
    /** The beam's grade fired (0 none), the injector's barrel Mk, ρ_max, the hold rating, pellets per second. */
    int grade, barrelMk;
    double rhoMax, rate;
    int holdRating;
    /** The pellets in stock of the grade fired (all injectors and stock buses), and of every grade. */
    long stock;
    final long[] stockByGrade = new long[4];
    /** The level the injector's Burst Control Hatch read (−1: none, the centre 8 is used). */
    int level = -1;
    /** Pellet Injector Hatches standing. */
    int injectors;
    /** The beam fired this second (T9, or the T8 Deep Link test fire). */
    boolean firing;
    /** B-221: the injector's Burst Control Hatch stands but has never carried a signal (the beam fires at 8). */
    boolean unwired;
    /**
     * B-222: the beam fires nothing while V is above +5 (the Attractor needs it): no injector, barrel or pellets, or
     * the Burst Control Hatch holding the rate law at 0. No working V loop does that (analog: s ≥ 9 there; two-level:
     * 15). Flow bit 4 and the GUI raise it; {@link #why} says what is missing.
     */
    boolean fault;
    /** Why the beam fires nothing this second ("" while it fires). */
    String why = "";

    /** B-222: the beam fires nothing while V climbs (see {@link #fault}). */
    public boolean beamFault() {
        return fault;
    }

    /** B-221: the injector's Burst Control Hatch has never carried a signal (the beam fires at the centre 8). */
    public boolean unwired() {
        return unwired;
    }

    /** Why the beam fires nothing ("" while it fires). */
    public String idleReason() {
        return why;
    }

    /** B-222: Flow bit 4, the beam starved: pellets low, or the beam not firing while V climbs. */
    public boolean beamStarved() {
        return pelletsLow() || fault;
    }

    /** The accept limit in force: the setting, or the hold rating. */
    public int acceptLimit() {
        return acceptSet >= 0 ? Math.min(AttractorField.FLOW_MAX, acceptSet) : holdRating;
    }

    public int acceptSetting() {
        return acceptSet;
    }

    public DeepLink deepLink() {
        return deep;
    }

    /** Seconds of fire the stock holds at the current rate (+infinity when not firing). */
    public double stockSeconds() {
        return rate > 1e-9 ? stock / rate : Double.POSITIVE_INFINITY;
    }

    /** Flow bit 4: pellets low (under 120 s of fire at the current rate), or none to fire. */
    public boolean pelletsLow() {
        return stock <= 0 || stockSeconds() < PELLETS_LOW_SECONDS;
    }

    public int grade() {
        return grade;
    }

    public int barrelMk() {
        return barrelMk;
    }

    public double rhoMax() {
        return rhoMax;
    }

    public int holdRating() {
        return holdRating;
    }

    public long stock() {
        return stock;
    }

    public long stock(int grade) {
        return grade >= 0 && grade < 4 ? stockByGrade[grade] : 0;
    }

    public int injectors() {
        return injectors;
    }

    public int level() {
        return level;
    }

    public double rate() {
        return rate;
    }

    public long chargeOwed() {
        return uniOwed;
    }

    public boolean firing() {
        return firing;
    }

    public long captureT() {
        return captureT;
    }

    public int captureN() {
        return captureN;
    }

    public long endT() {
        return endT;
    }

    public int endKind() {
        return endKind;
    }

    public long passT() {
        return passT;
    }

    public int passSize() {
        return passSize;
    }

    public int passSign() {
        return passSign;
    }

    public String lastFlow() {
        return lastFlow;
    }

    public static String gradeName(int g) {
        switch (g) {
            case 1:
                return "Magnetar";
            case 2:
                return "Ergomatter";
            case 3:
                return "Superdense";
            default:
                return "none";
        }
    }

    /** "1.25 EeV" / "20 EeV" for a grade. */
    public static String gevText(int g) {
        final double e = AttractorField.gev(g);
        if (e <= 0) return "-";
        final double eev = e / 1e9;
        return (eev == Math.rint(eev) ? String.format(java.util.Locale.ROOT, "%.0f", eev)
            : String.format(java.util.Locale.ROOT, "%.2f", eev)) + " EeV";
    }

    // ================================================================================== fluids

    /** Attractor Dark Matter ({@code btsdarkmatter}, registered by the content unit), or null. */
    public static Fluid darkMatter() {
        return FluidRegistry.getFluid("btsdarkmatter");
    }

    /** Molten Universium (GT), or null. */
    public static Fluid universium() {
        final FluidStack s = Materials.Universium.getMolten(1L);
        return s == null ? null : s.getFluid();
    }

    /** The universe charge per parallel, litres. */
    public static long universeCharge() {
        return Math.round(UniverseField.CHARGE);
    }

    // ================================================================================== NBT

    void writePersistent(NBTTagCompound nbt) {
        if (acceptSet >= 0) nbt.setInteger("btsEyeAccept", acceptSet);
    }

    void readPersistent(NBTTagCompound nbt) {
        acceptSet = nbt.hasKey("btsEyeAccept") ? nbt.getInteger("btsEyeAccept") : -1;
    }

    NBTTagCompound writeLive() {
        final NBTTagCompound t = new NBTTagCompound();
        t.setDouble("carry", pelletCarry);
        t.setLong("owed", uniOwed);
        t.setLong("captureT", captureT);
        t.setInteger("captureN", captureN);
        t.setLong("endT", endT);
        t.setInteger("endKind", endKind);
        t.setString("lastFlow", lastFlow);
        t.setLong("passT", passT);
        t.setInteger("passSize", passSize);
        t.setInteger("passSign", passSign);
        final NBTTagCompound d = new NBTTagCompound();
        d.setInteger("streak", deep.link.streak);
        d.setInteger("best", deep.link.best);
        d.setBoolean("done", deep.link.done);
        d.setLong("n", deep.link.n);
        final double[] w = deep.window();
        for (int i = 0; i < w.length; i++) d.setDouble("w" + i, w[i]);
        t.setTag("deep", d);
        return t;
    }

    void readLive(NBTTagCompound t) {
        resetLive();
        if (t == null || !t.hasKey("carry")) return;
        pelletCarry = t.getDouble("carry");
        uniOwed = t.getLong("owed");
        captureT = t.getLong("captureT");
        captureN = t.getInteger("captureN");
        endT = t.getLong("endT");
        endKind = t.getInteger("endKind");
        lastFlow = t.getString("lastFlow");
        passT = t.hasKey("passT") ? t.getLong("passT") : -1;
        passSize = t.getInteger("passSize");
        passSign = t.getInteger("passSign");
        final NBTTagCompound d = t.getCompoundTag("deep");
        final double[] w = new double[3 * 60];
        for (int i = 0; i < w.length; i++) w[i] = d.getDouble("w" + i);
        deep.restore(w, d.getLong("n"));
        deep.link.streak = d.getInteger("streak");
        deep.link.best = d.getInteger("best");
        deep.link.done = d.getBoolean("done");
    }

    void resetLive() {
        pelletCarry = 0;
        uniOwed = 0;
        captureT = -1;
        captureN = 0;
        endT = -1;
        endKind = 0;
        lastFlow = "";
        passT = -1;
        passSize = passSign = 0;
        deep.restore(new double[3 * 60], 0);
        deep.link.streak = deep.link.best = 0;
        deep.link.done = false;
    }
}
