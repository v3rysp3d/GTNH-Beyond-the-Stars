package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.LinkedHashMap;

import net.minecraft.nbt.NBTTagCompound;

import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.Overlay;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.StepInputs;
import com.nateyburr.beyondthestars.machines.eye.field.StepResult;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

/**
 * D-49: a tier transition as a state machine (d49-build-plan.md §7). Each one is an {@link Overlay} on the field
 * second, so its extra drift, ramps, forced events and per-second rules run inside {@link StellarField#step} in the
 * prototype's order (after Integrity, before the cycle accounting). The runtime ({@link EyeStarRuntime}) owns the
 * common frame: the preconditions, {@code beginTransition}, the materials, {@code completeTransition} (unlock, the
 * core consumed if used) and the abort (wrench, soft mallet, structure, target change: the star goes DARK, the event
 * materials are lost, the core stays in escrow).
 * <p>
 * Tile NBT only ({@code btsEyeTrans}): a wrenched controller has no transition (plan Q-10).
 */
public abstract class EyeTransition implements Overlay {

    /** The five transitions; {@link #from} is the tier they leave. */
    public enum Type {

        MASS_INFUSION(1, "Mass Infusion", "star:siriusa"),
        SHELL_IGNITION(2, "Shell Ignition", "star:arcturus"),
        CORE_COLLAPSE(3, "Core Collapse", "star:neutronstar"),
        FIRST_COOLING(4, "First Cooling", "star:siriusb"),
        MAGNETAR_MERGER(5, "Magnetar Merger", "star:magnetar");

        /** The tier the transition leaves (it unlocks from + 1). */
        public final int from;
        public final String title;
        /** The StarDef key of the star it makes (its chart is the one in the controller slot). */
        public final String dest;

        Type(int from, String title, String dest) {
            this.from = from;
            this.title = title;
            this.dest = dest;
        }

        /** The transition that leaves tier {@code n}, or null. */
        public static Type leaving(int n) {
            for (Type t : values()) if (t.from == n) return t;
            return null;
        }
    }

    public final Type type;
    /** Seconds the transition has run (the tick count). */
    public int t;
    /** The field clock when it began (the bench's offline compare). */
    public long beginT;
    /** True when the escrowed core is consumed on success (the tier was not unlocked when it began). */
    public boolean coreUsed;

    protected EyeTransition(Type type) {
        this.type = type;
    }

    /** A fresh transition of that type. {@code bounceShock} is the Core Collapse's bounce. */
    public static EyeTransition create(Type type, double bounceShock) {
        switch (type) {
            case MASS_INFUSION:
                return new MassInfusion();
            case SHELL_IGNITION:
                return new ShellIgnition();
            case CORE_COLLAPSE:
                return new CoreCollapse(bounceShock);
            case FIRST_COOLING:
                return new FirstCooling();
            default:
                return new MagnetarMerger();
        }
    }

    /** Field set-up when it begins (Shell Ignition: Θ at 0 and the forced flash scheduled). */
    void onBegin(FieldState s) {}

    /** The trim table the Field sockets are drained by while it runs (Shell Ignition: T3's). */
    public TierParams trimTable(TierParams tier) {
        return tier;
    }

    /** Debug rigs only: adjusts the prototype rig's request (the merger's imprint holds Π high). */
    public void adjustRig(LinkedHashMap<String, Double> raw, FieldState s, FieldRig.Mode mode) {}

    /** One scanner line: the phase and its progress. */
    public abstract String phaseLine();

    /**
     * The star becomes tier {@code next} mid-step (Mass Infusion, Shell Ignition): prototype {@code light()} for the
     * new tier, so the partial cycle is discarded and the new tier's first event is scheduled (plan R-5), but the
     * gauges and Integrity carry over (plan §7.2) and the Harmonic Lock restarts. The runtime charges the new cycle
     * on {@link StepResult#relit}.
     */
    protected static void becomeTier(FieldState s, TierParams next, StepInputs in, StepResult out) {
        final double integrity = s.I;
        s.tier = next;
        StellarField.light(s, in.dialSetpoint, in.tt);
        s.I = integrity;
        out.relit = true;
    }

    // ================================================================================== NBT

    void writeExtra(NBTTagCompound nbt) {}

    void readExtra(NBTTagCompound nbt) {}

    public static NBTTagCompound write(EyeTransition tr) {
        final NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("type", tr.type.name());
        nbt.setInteger("t", tr.t);
        nbt.setLong("beginT", tr.beginT);
        nbt.setBoolean("coreUsed", tr.coreUsed);
        tr.writeExtra(nbt);
        return nbt;
    }

    /** The saved transition, or null when the tag is unreadable. */
    public static EyeTransition read(NBTTagCompound nbt) {
        final Type type;
        try {
            type = Type.valueOf(nbt.getString("type"));
        } catch (IllegalArgumentException bad) {
            return null;
        }
        final EyeTransition tr = create(
            type,
            nbt.hasKey("shock") ? nbt.getDouble("shock")
                : com.nateyburr.beyondthestars.machines.eye.field.CollapseOverlay.BOUNCE_SHOCK);
        tr.t = nbt.getInteger("t");
        tr.beginT = nbt.getLong("beginT");
        tr.coreUsed = nbt.getBoolean("coreUsed");
        tr.readExtra(nbt);
        return tr;
    }
}
