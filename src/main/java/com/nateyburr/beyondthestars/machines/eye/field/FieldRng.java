package com.nateyburr.beyondthestars.machines.eye.field;

/**
 * D-49 stability field: the field's noise source, an exact port of the prototype's {@code mulberry} generator
 * (designs/star-lifecycle-prototype.html, {@code mulberry(cfg.seed*2654435761>>>0)}).
 * <p>
 * Java {@code int} arithmetic wraps exactly like JavaScript's {@code |0} and {@code Math.imul}, so the sequence is
 * bit-identical to the prototype's for every seed. The whole state is one {@code int}; save it with {@link #state()}
 * and restore it with {@link #ofState(int)} so a reload continues the same sequence (d49-build-plan.md §3.3).
 * No Minecraft or GT types (plan §16, unit 1).
 */
public final class FieldRng {

    private static final double TWO_32 = 4294967296.0;

    private int a;

    /** Seeds like the prototype: {@code a = ToUint32(seed * 2654435761)}. */
    public FieldRng(long seed) {
        this.a = seedState(seed);
    }

    private FieldRng(int state, @SuppressWarnings("unused") boolean raw) {
        this.a = state;
    }

    /** A generator that continues from a saved {@link #state()}. */
    public static FieldRng ofState(int state) {
        return new FieldRng(state, true);
    }

    /** The current state (one int), for NBT. */
    public int state() {
        return a;
    }

    /**
     * JavaScript's {@code (seed*2654435761)>>>0}: the product is a double (rounded like JS for huge seeds), then
     * truncated towards zero and reduced modulo 2^32. Equals {@code (seed * 2654435761L) & 0xFFFFFFFFL} for every
     * seed below about 3.39e6, where the double product is exact.
     */
    public static int seedState(long seed) {
        final double prod = (double) seed * 2654435761.0;
        if (Double.isNaN(prod) || Double.isInfinite(prod)) return 0;
        final double whole = prod < 0 ? -Math.floor(-prod) : Math.floor(prod);
        double r = whole % TWO_32; // IEEE fmod: exact
        if (r < 0) r += TWO_32;
        return (int) (long) r;
    }

    /** The next draw in [0, 1). One call = one prototype {@code S.rng()} call. */
    public double next() {
        a += 0x6D2B79F5;
        int t = (a ^ (a >>> 15)) * (1 | a);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / TWO_32;
    }
}
