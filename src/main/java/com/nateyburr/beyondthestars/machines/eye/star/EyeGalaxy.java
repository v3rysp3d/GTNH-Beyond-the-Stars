package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.nateyburr.beyondthestars.machines.eye.field.GalaxyField;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstLink;

/**
 * P4: the Eye's galaxy-tier state (quasar-p4.md §3.1, §4, §5), kept beside the D-49 star by {@link EyeStarRuntime}:
 * the mode (galaxy or planets), the T8 Ergomatter feed setpoint, the two link milestones' live windows and streaks,
 * the litre carries of the fractional draws, the companions and the planet slots. The runtime owns the flow; this class
 * owns the data and its NBT.
 * <p>
 * <b>Trap 2.</b> {@link #writePersistent} (the mode and the feed setpoint) goes with the item, like the dial;
 * everything else is the live star's and goes into the tile only ({@link #writeLive}): a wrench restarts the links'
 * streaks (their {@code done} flags and best streaks are milestones, in {@link EyeMilestones}).
 */
public final class EyeGalaxy {

    /** Galaxy mode (the T7 / T8 object, its companions) or planets mode (3 / 6 planet slots, a one-gauge field). */
    public enum Mode {

        GALAXY("galaxy"),
        PLANETS("planets");

        public final String key;

        Mode(String key) {
            this.key = key;
        }

        /** "galaxy" / "planets" (any case), or null. */
        public static Mode parse(String s) {
            if (s == null) return null;
            for (Mode m : values()) if (m.key.equalsIgnoreCase(s.trim()) || m.name()
                .equalsIgnoreCase(s.trim())) return m;
            return null;
        }
    }

    /** The T8 feed setpoint's GUI step, L/s. */
    public static final double F8_STEP = 0.5;

    // ---------------------------------------------------------------- persistent (item NBT)
    Mode mode = Mode.GALAXY;
    double f8Set = GalaxyField.F8_DEFAULT;

    // ---------------------------------------------------------------- live (tile NBT)
    /** The Stability Link (T6 → T7): the rail's total, the high bank as the part. */
    final BurstLink link6 = new BurstLink();
    /** The Heavy Link (T7 → T8): the Eye's total, the halo as the part. */
    final BurstLink link7 = new BurstLink();
    /**
     * SMBH (smbh-v1.md §1.3): the Supermassive Link (T9 → T10, P5's milestone): the Eye's total, the part from a
     * qualifying source (a lattice in force, at least 65,536 SM). Data only until Eye T9.
     */
    final BurstLink link9 = new BurstLink();
    /** Fractional litres owed per purpose and fluid ("up:fluid", "charge:fluid", "feed:fluid", "out:fluid"). */
    final Map<String, Double> carry = new LinkedHashMap<>();
    /** The companions running (earlier object tiers 1..6), latched at the last relight; empty in planets mode. */
    int[] companions = new int[0];
    /** Each companion's upkeep fraction r for the cycle in progress. */
    double[] companionR = new double[0];
    /** Planets mode: one run per slot (null = no planet in that slot of the slot bus). */
    PlanetRun[] slots = new PlanetRun[0];
    /** The last finished planet run lost parallels (field factor under 1): Slots [4]. */
    boolean lastRunLost;

    // ---------------------------------------------------------------- transient (one second)
    EyeBursts bursts = EyeBursts.NONE;
    /** Last second's inputs as the field saw them (GUI, scanner, sensors, OpenComputers). */
    double beta, gHi, gLo, gHalo, gEye, demand, normDemand;
    /** A begin was asked for (GUI, Burst Control Hatch in Begin mode, OpenComputers): used by the next second. */
    boolean beginRequested;
    /** The last finished planet runs, one line each (GUI). */
    String lastRuns = "";

    /** One planet slot's run (quasar-p4.md §4.8; the runtime pays it at its start and rolls it at its end). */
    static final class PlanetRun {

        /** The converted planet's EyeRecipe key ("planet:Ma"). */
        final String key;
        /** Eye-seconds run; Σ field factor; seconds counted. */
        double t, zf;
        int n;
        /** The parallel paid for (0 = not paid yet: the run waits for its H/He and EU). */
        long paid;

        PlanetRun(String key) {
            this.key = key;
        }

        void reset() {
            t = 0;
            zf = 0;
            n = 0;
            paid = 0;
        }
    }

    // ============================================================================ accessors

    public Mode mode() {
        return mode;
    }

    public double f8Setpoint() {
        return f8Set;
    }

    public BurstLink stabilityLink() {
        return link6;
    }

    public BurstLink heavyLink() {
        return link7;
    }

    /** SMBH: the Supermassive Link's live window and streak (P5's T9 → T10 milestone; read-only data). */
    public BurstLink supermassiveLink() {
        return link9;
    }

    public EyeBursts bursts() {
        return bursts;
    }

    public double beta() {
        return beta;
    }

    public double gammaHi() {
        return gHi;
    }

    public double gammaLo() {
        return gLo;
    }

    public double gammaHalo() {
        return gHalo;
    }

    /** The burst demand last second, Bu/t (× p). */
    public double demand() {
        return demand;
    }

    public int[] companions() {
        return companions.clone();
    }

    public boolean lastRunLost() {
        return lastRunLost;
    }

    public String lastRuns() {
        return lastRuns;
    }

    /** Planets mode: each slot's planet key and progress 0..1 ("" for an empty slot). */
    public Map<String, Double> slotProgress(java.util.function.ToDoubleFunction<String> seconds) {
        final Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < slots.length; i++) {
            final PlanetRun r = slots[i];
            if (r == null) continue;
            final double total = Math.max(1, seconds.applyAsDouble(r.key));
            out.put(i + 1 + ":" + r.key, r.paid > 0 ? Math.min(1, r.t / total) : -1);
        }
        return out;
    }

    // ============================================================================ settings

    void setF8Quiet(double v) {
        f8Set = Double.isNaN(v) ? GalaxyField.F8_DEFAULT
            : Math.max(GalaxyField.F8_MIN, Math.min(GalaxyField.F8_MAX, Math.round(v * 100) / 100.0));
    }

    // ============================================================================ carries

    /**
     * Adds {@code litres} owed under {@code key} and returns the whole litres to draw now (the fraction stays owed),
     * so a draw of 7.5 L per cycle or 61.44 L/s costs exactly that over time.
     */
    long owe(String key, double litres) {
        final double owed = carry.getOrDefault(key, 0d) + Math.max(0, litres);
        final long whole = (long) Math.floor(owed + 1e-9);
        carry.put(key, owed - whole);
        return whole;
    }

    /** Adds output litres under {@code key} and returns the whole litres to emit now. */
    long earn(String key, double litres) {
        return owe("out:" + key, litres);
    }

    // ============================================================================ lifecycle

    /** A new controller's defaults (before an NBT read). */
    void reset() {
        mode = Mode.GALAXY;
        f8Set = GalaxyField.F8_DEFAULT;
        resetLive();
    }

    /** The live part only (a new star, a wrench). */
    void resetLive() {
        link6.restore(new double[2 * BurstLink.WINDOW], 0);
        link6.streak = link6.best = 0;
        link6.done = false;
        link7.restore(new double[2 * BurstLink.WINDOW], 0);
        link7.streak = link7.best = 0;
        link7.done = false;
        link9.restore(new double[2 * BurstLink.WINDOW], 0);
        link9.streak = link9.best = 0;
        link9.done = false;
        carry.clear();
        companions = new int[0];
        companionR = new double[0];
        slots = new PlanetRun[0];
        lastRunLost = false;
        bursts = EyeBursts.NONE;
        beta = gHi = gLo = gHalo = gEye = demand = normDemand = 0;
        beginRequested = false;
        lastRuns = "";
    }

    /** A star went out: the planet runs and the companions stop; the links' windows keep rolling. */
    void starOut() {
        companions = new int[0];
        companionR = new double[0];
        for (PlanetRun r : slots) if (r != null) r.reset();
    }

    // ================================================================================== NBT

    void writePersistent(NBTTagCompound nbt) {
        nbt.setString("btsEyeMode", mode.key);
        nbt.setDouble("btsEyeF8", f8Set);
    }

    void readPersistent(NBTTagCompound nbt) {
        final Mode m = Mode.parse(nbt.getString("btsEyeMode"));
        mode = m == null ? Mode.GALAXY : m;
        if (nbt.hasKey("btsEyeF8")) setF8Quiet(nbt.getDouble("btsEyeF8"));
    }

    /** The live part (tile only; written beside the star). */
    NBTTagCompound writeLive() {
        final NBTTagCompound g = new NBTTagCompound();
        writeLink(g, "link6", link6);
        writeLink(g, "link7", link7);
        writeLink(g, "link9", link9);
        if (!carry.isEmpty()) {
            final NBTTagCompound c = new NBTTagCompound();
            for (Map.Entry<String, Double> e : carry.entrySet()) c.setDouble(e.getKey(), e.getValue());
            g.setTag("carry", c);
        }
        if (companions.length > 0) {
            g.setIntArray("comp", companions.clone());
            final NBTTagCompound r = new NBTTagCompound();
            for (int i = 0; i < companionR.length; i++) r.setDouble("r" + i, companionR[i]);
            g.setTag("compR", r);
        }
        if (slots.length > 0) {
            final NBTTagList list = new NBTTagList();
            for (PlanetRun s : slots) {
                final NBTTagCompound t = new NBTTagCompound();
                if (s != null) {
                    t.setString("key", s.key);
                    t.setDouble("t", s.t);
                    t.setDouble("zf", s.zf);
                    t.setInteger("n", s.n);
                    t.setLong("paid", s.paid);
                }
                list.appendTag(t);
            }
            g.setTag("slots", list);
        }
        g.setBoolean("lost", lastRunLost);
        if (!lastRuns.isEmpty()) g.setString("runs", lastRuns);
        return g;
    }

    void readLive(NBTTagCompound g) {
        readLink(g, "link6", link6);
        readLink(g, "link7", link7);
        readLink(g, "link9", link9);
        carry.clear();
        if (g.hasKey("carry")) {
            final NBTTagCompound c = g.getCompoundTag("carry");
            for (Object key : c.func_150296_c()) carry.put((String) key, c.getDouble((String) key));
        }
        companions = g.hasKey("comp") ? g.getIntArray("comp") : new int[0];
        companionR = new double[companions.length];
        Arrays.fill(companionR, 1);
        if (g.hasKey("compR")) {
            final NBTTagCompound r = g.getCompoundTag("compR");
            for (int i = 0; i < companionR.length; i++) if (r.hasKey("r" + i)) companionR[i] = r.getDouble("r" + i);
        }
        if (g.hasKey("slots")) {
            final NBTTagList list = g.getTagList("slots", 10);
            slots = new PlanetRun[list.tagCount()];
            for (int i = 0; i < slots.length; i++) {
                final NBTTagCompound t = list.getCompoundTagAt(i);
                if (!t.hasKey("key")) continue;
                final PlanetRun s = new PlanetRun(t.getString("key"));
                s.t = t.getDouble("t");
                s.zf = t.getDouble("zf");
                s.n = t.getInteger("n");
                s.paid = t.getLong("paid");
                slots[i] = s;
            }
        } else {
            slots = new PlanetRun[0];
        }
        lastRunLost = g.getBoolean("lost");
        lastRuns = g.getString("runs");
    }

    private static void writeLink(NBTTagCompound g, String key, BurstLink l) {
        final NBTTagCompound t = new NBTTagCompound();
        // the window as a sparse compound ("i" -> value): a quiet link saves almost nothing
        final double[] w = l.window();
        final NBTTagCompound arr = new NBTTagCompound();
        for (int i = 0; i < w.length; i++) if (w[i] != 0) arr.setDouble(Integer.toString(i), w[i]);
        t.setTag("w", arr);
        t.setLong("n", l.n);
        t.setInteger("streak", l.streak);
        t.setInteger("best", l.best);
        t.setBoolean("done", l.done);
        g.setTag(key, t);
    }

    private static void readLink(NBTTagCompound g, String key, BurstLink l) {
        final double[] w = new double[2 * BurstLink.WINDOW];
        if (!g.hasKey(key)) {
            l.restore(w, 0);
            l.streak = l.best = 0;
            l.done = false;
            return;
        }
        final NBTTagCompound t = g.getCompoundTag(key);
        final NBTTagCompound arr = t.getCompoundTag("w");
        for (int i = 0; i < w.length; i++)
            if (arr.hasKey(Integer.toString(i))) w[i] = arr.getDouble(Integer.toString(i));
        l.restore(w, t.getLong("n"));
        l.streak = t.getInteger("streak");
        l.best = t.getInteger("best");
        l.done = t.getBoolean("done");
    }

    /** "β 1.12 (D 1.50 Bu/t): hi 0.45 lo 0.41 halo 0" for the scanner. */
    String inputLine() {
        return String.format(
            Locale.ROOT,
            "β %.2f (demand %s): γ high %.2f, low %.2f, halo %.2f",
            beta,
            com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(demand),
            gHi,
            gLo,
            gHalo);
    }
}
