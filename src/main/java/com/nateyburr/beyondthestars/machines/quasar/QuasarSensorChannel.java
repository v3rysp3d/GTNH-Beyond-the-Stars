package com.nateyburr.beyondthestars.machines.quasar;

import java.util.Locale;

/**
 * P4: the Stellar Field Sensor Hatch's channels on a Quasar Engine (quasar-p4.md §1.5, §8.1). The quasar publishes one
 * value per channel every second ({@link MTEQuasarEngine#sensorValues}); the hatch's channel ordinal indexes this list
 * while it sits in a quasar. Analog output by {@link #kind}:
 * <ul>
 * <li>{@link Kind#SIGNED}: the window, {@code round(15·clamp((v + w)/(2w), 0, 1))} (w default 120 = D-49's
 * mapping);</li>
 * <li>{@link Kind#SCALED}: {@code round(15·v/max)} (0-100 values, the zone 0..4, λ 0..2, a 0..1, flags 15 or 0);</li>
 * <li>{@link Kind#OPEN}: unbounded values (mass, Bu/t, seconds, σ) read with the window as full scale,
 * {@code round(15·clamp(v/w, 0, 1))}.</li>
 * </ul>
 * NBT keeps the hatch's channel as an ordinal: append new channels at the end.
 */
public enum QuasarSensorChannel {

    E("E (Eddington balance)", Kind.SIGNED, 0),
    PHI("Φ (horizon flux, 0-100)", Kind.SCALED, 100),
    LAMBDA("λ (Eddington ratio)", Kind.SCALED, 2),
    SPIN("a (spin)", Kind.SCALED, 1),
    MASS("M (hole mass, SM)", Kind.OPEN, 0),
    MASS_CAP("M / coil cap (%)", Kind.SCALED, 100),
    G_TOTAL("Bursts, both jets (Bu/t)", Kind.OPEN, 0),
    G_TOP("Bursts, top jet (Bu/t)", Kind.OPEN, 0),
    G_BOTTOM("Bursts, bottom jet (Bu/t)", Kind.OPEN, 0),
    ZONE("Zone (0 Resonant .. 4 Breach)", Kind.SCALED, 4),
    INTEGRITY("Disc Integrity", Kind.SCALED, 100),
    STABILITY("Stability (100 - |E|)", Kind.SCALED, 100),
    ERUPT_WARN("Eruption warning (Φ >= 90)", Kind.SCALED, 1),
    ERUPT_SECONDS("Seconds to the next eruption", Kind.OPEN, 0),
    LINK_SIGMA("σ of the best-fed host", Kind.OPEN, 0),
    LINK_STREAK("Link streak (% of 30 min)", Kind.SCALED, 100);

    /** How a channel's value maps onto 0-15. */
    public enum Kind {
        SIGNED,
        SCALED,
        OPEN
    }

    public static final QuasarSensorChannel[] VALUES = values();

    public final String label;
    public final Kind kind;
    /** Full scale for {@link Kind#SCALED}. */
    public final double max;

    QuasarSensorChannel(String label, Kind kind, double max) {
        this.label = label;
        this.kind = kind;
        this.max = max;
    }

    public static QuasarSensorChannel of(int ordinal) {
        return VALUES[Math.floorMod(ordinal, VALUES.length)];
    }

    /** The channel with this name (any case), or null. */
    public static QuasarSensorChannel parse(String name) {
        if (name == null) return null;
        final String n = name.trim()
            .toUpperCase(Locale.ROOT);
        for (QuasarSensorChannel c : VALUES) if (c.name()
            .equals(n)) return c;
        return null;
    }

    /** Analog 0..15 of {@code v} on this channel with window {@code w}. */
    public int analog(double v, double w) {
        final double ww = Math.max(1e-9, w);
        final double f;
        switch (kind) {
            case SIGNED:
                f = (v + ww) / (2 * ww);
                break;
            case OPEN:
                f = v / ww;
                break;
            default:
                f = v / max;
                break;
        }
        return (int) Math.round(15 * Math.max(0, Math.min(1, f)));
    }
}
