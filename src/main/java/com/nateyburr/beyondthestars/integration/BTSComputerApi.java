package com.nateyburr.beyondthestars.integration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import com.nateyburr.beyondthestars.machines.directive.DirectiveHost;
import com.nateyburr.beyondthestars.machines.directive.DirectiveSet;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime;
import com.nateyburr.beyondthestars.machines.eye.star.EyeTransition;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrModuleBase;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * AUTOMATION PASS: what a computer may read and set on the Singularity Ladder's machines, with no computer mod's types
 * in it. The OpenComputers components ({@code integration.oc}, a soft dependency) are thin wrappers over these calls,
 * and the dev-server bench drives the same calls ({@code /btskerr ocloop}, {@code /btseye star … ocloop}) to prove the
 * setter rules without OpenComputers installed.
 * <p>
 * <b>Same rules as the GUI.</b> Every setter goes through the setter the GUI uses: the Kerr setpoint clamps to the
 * tier's range, Open and Collapse need a formed KEE and a Seed / Collapser in a bolt bus (and consume it), the time
 * dial snaps to 0.25 steps in 1-3 and the HSM feed clamps to 0-1.5 L/s. There is no setter a player does not have.
 * Results are maps of numbers, strings and booleans (Lua tables on the computer). Server thread only.
 */
public final class BTSComputerApi {

    private BTSComputerApi() {}

    // ===================================================================================== Eye

    /** {@code bts_eye.getState()}: tiers, the star, the transition, setpoints, upkeep, last event and failure. */
    public static Map<String, Object> eyeState(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", eye.isFormedForGui());
        m.put("tier", eye.installedTier());
        m.put("unlockedTier", rt.unlockedTier());
        m.put("structureTier", eye.structureTier());
        m.put("compressionTier", eye.compressionTier());
        m.put(
            "state",
            rt.state()
                .name());
        final StarDef star = rt.star();
        m.put("star", star == null ? "" : star.name);
        m.put("starTier", star == null ? 0 : star.n());
        m.put("burning", rt.burning());
        m.put("parallel", rt.lastParallel());
        final ItemStack target = eye.getControllerSlot();
        m.put("target", target == null ? "" : target.getDisplayName());
        final EyeTransition tr = rt.transition();
        m.put("transition", tr == null ? "" : tr.type.title);
        m.put("transitionProgress", rt.transitionProgress());
        m.put("dial", rt.dialSetpoint());
        m.put("dialPaid", rt.dialPaid());
        m.put("dialMax", rt.dialMax());
        m.put("feed", rt.feedSetpoint());
        m.put("feedActual", rt.feedActual());
        m.put("upkeepR", rt.upkeepR());
        m.put("nextCycleSeconds", rt.nextCycleSeconds());
        m.put("waitReason", rt.waitReason());
        m.put("lastEvent", rt.lastEvent());
        m.put("lastFailure", rt.lastFailure());
        return m;
    }

    /** {@code bts_eye.getField()}: last second's stability field (the sensor hatches' readout). */
    public static Map<String, Object> eyeField(MTEEyeOfHarmonyBTS eye) {
        final FieldReadout r = eye.starRuntime()
            .lastReadout();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("lit", r.lit);
        m.put("tier", r.tier);
        m.put("P", r.value(FieldReadout.Channel.P));
        m.put("theta", r.value(FieldReadout.Channel.THETA));
        m.put("omega", r.value(FieldReadout.Channel.OMEGA));
        m.put("pi", r.value(FieldReadout.Channel.PI));
        m.put("shield", r.A);
        m.put("integrity", r.I);
        m.put("stability", r.stability);
        m.put("zone", r.zone);
        m.put("zoneName", r.zone >= 0 && r.zone < StellarField.ZONE_NAME.length ? StellarField.ZONE_NAME[r.zone] : "");
        m.put("yield", r.Y);
        m.put("lock", r.lock);
        m.put("warning", r.warn);
        m.put("setpointError", r.value(FieldReadout.Channel.SETPOINT_ERROR));
        m.put("r", r.r);
        m.put("m", r.m);
        m.put("p", r.p);
        return m;
    }

    /** {@code bts_eye.getBuffers()}: fluid registry name → litres held. */
    public static Map<String, Object> eyeBuffers(MTEEyeOfHarmonyBTS eye) {
        return new LinkedHashMap<>(eye.bufferSnapshot());
    }

    /**
     * {@code bts_eye.getIntakeCaps()} (B-108): fluid registry name → the litres the sockets fill that buffer to now
     * (a fluid not listed is not pulled).
     */
    public static Map<String, Object> eyeIntakeCaps(MTEEyeOfHarmonyBTS eye) {
        final Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<Fluid, Long> e : eye.intakeCaps()
            .entrySet()) {
            if (e.getKey() != null) m.put(
                e.getKey()
                    .getName(),
                Math.min(MTEEyeOfHarmonyBTS.BUFFER_CAP, e.getValue()));
        }
        return m;
    }

    /** {@code bts_eye.getTransition()}: unlocking: milestones, the wanted core, escrow, ready, what is missing. */
    public static Map<String, Object> eyeTransition(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final int n = rt.unlockedTier();
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("unlockedTier", n);
        m.put(
            "milestonesMet",
            rt.milestones()
                .met(n));
        m.put(
            "milestones",
            rt.milestones()
                .progressLine(n));
        final net.minecraft.item.Item core = EyeStarRuntime.coreFor(n);
        m.put("core", core == null ? "" : new ItemStack(core).getDisplayName());
        m.put("coreWanted", rt.coreWanted());
        m.put(
            "escrow",
            rt.escrow() == null ? ""
                : rt.escrow()
                    .getDisplayName());
        m.put("ready", rt.transitionReady());
        final StarDef star = rt.star();
        final List<String> missing = rt.missing(star != null && rt.live() ? star.n() : n);
        final Map<Integer, String> list = new LinkedHashMap<>();
        for (int i = 0; i < missing.size(); i++) list.put(i + 1, missing.get(i));
        m.put("missing", list);
        return m;
    }

    /** {@code bts_eye.setDial(v)}: the GUI's clamping setter (1-3, 0.25 steps). Returns the new setpoint. */
    public static double eyeSetDial(MTEEyeOfHarmonyBTS eye, double v) {
        eye.starRuntime()
            .setDial(v);
        return eye.starRuntime()
            .dialSetpoint();
    }

    /** {@code bts_eye.setFeed(v)}: the GUI's clamping setter (0-1.5 L/s per parallel). Returns the new setpoint. */
    public static double eyeSetFeed(MTEEyeOfHarmonyBTS eye, double v) {
        eye.starRuntime()
            .setFeed(v);
        return eye.starRuntime()
            .feedSetpoint();
    }

    // ============================================================================ directives

    /**
     * {@code getDirective(set [, w])} on the Eye or the KEE: the code of one of the host's output sets, with its bits
     * by name. An unknown set, another host's set or a control set gives {@code {error=...}}.
     */
    public static Map<String, Object> directive(DirectiveHost host, String setName, int window) {
        final Map<String, Object> m = new LinkedHashMap<>();
        final DirectiveSet set = DirectiveSet.parse(setName);
        if (set == null || set.host != host.directiveHost() || set.control) {
            m.put("error", "sets here: " + setNames(host.directiveHost()));
            return m;
        }
        final int w = Math.max(MTEHatchDirective.WINDOW_MIN, Math.min(MTEHatchDirective.WINDOW_MAX, window));
        final int code = host.directiveCode(set, w) & 15;
        m.put("set", set.name());
        m.put("code", code);
        final Map<String, Object> bits = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) bits.put(host.directiveBitLabel(set, i), (code & (1 << i)) != 0);
        m.put("bits", bits);
        return m;
    }

    /** The output-set names of a host, for error messages and {@code getSets()}. */
    public static String setNames(DirectiveSet.Host host) {
        final StringBuilder sb = new StringBuilder();
        for (DirectiveSet s : DirectiveSet.VALUES) {
            if (s.host != host || s.control) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.name());
        }
        return sb.toString();
    }

    // ==================================================================================== Kerr

    /** {@code bts_kerr.getState()}: the hole, the governor, bolts, power, modules. */
    public static Map<String, Object> kerrState(MTEKerrEngine kee) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("formed", kee.isFormed());
        m.put("tier", kee.isDensityInstalled() ? 2 : 1);
        m.put("open", kee.isHoleOpen());
        m.put("mass", kee.getMass());
        m.put("setpoint", kee.getSetpoint());
        m.put("setpointMax", kee.maxSetpoint());
        m.put("bleed", kee.getBleed());
        m.put("boltsPerTick", kee.getBoltsPerTick());
        m.put("boltCap", kee.boltCap());
        m.put("railTier", kee.railTier());
        m.put("stability", kee.getStability());
        m.put("powerOk", kee.isPowerOk());
        m.put("containmentEUt", kee.getContainmentEut());
        m.put("boltStockSM", kee.getBoltStockSm());
        m.put("boltStockSeconds", kee.getBoltStockSeconds());
        m.put("evaporateSeconds", kee.getEvapSeconds());
        m.put("seeds", kee.getSeeds());
        m.put("collapsers", kee.getCollapsers());
        m.put("linkedModules", kee.getLinkedModules());
        m.put("auto", kee.isAutoSeeds());
        return m;
    }

    /**
     * {@code bts_kerr.setSetpoint(n)}: the GUI's setter (whole SM, clamped to 2..64 or 2..512). Returns the new one.
     */
    public static int kerrSetSetpoint(MTEKerrEngine kee, int sm) {
        kee.setSetpoint(sm);
        return kee.getSetpoint();
    }

    /** {@code bts_kerr.open()}: the GUI's Open button (a formed KEE, a closed hole, a Seed in a bolt bus). */
    public static Object[] kerrOpen(MTEKerrEngine kee) {
        if (kee.openHoleFromGui()) return new Object[] { true, "opened at " + MTEKerrEngine.OPEN_MASS + " SM" };
        return new Object[] { false,
            !kee.isFormed() ? "not formed" : kee.isHoleOpen() ? "already open" : "no Black Hole Seed in a bolt bus" };
    }

    /** {@code bts_kerr.collapse()}: the GUI's Collapse button (an open hole, a Collapser in a bolt bus). */
    public static Object[] kerrCollapse(MTEKerrEngine kee) {
        if (kee.collapseHoleFromGui()) return new Object[] { true, "collapsed" };
        return new Object[] { false, !kee.isFormed() ? "not formed"
            : !kee.isHoleOpen() ? "no hole open" : "no Black Hole Collapser in a bolt bus" };
    }

    /** {@code bts_kerr.setAuto(b)}: the GUI's Auto switch (a Seed / Collapser in a bolt bus acts on its own). */
    public static boolean kerrSetAuto(MTEKerrEngine kee, boolean on) {
        kee.setAutoSeeds(on);
        return kee.isAutoSeeds();
    }

    /** {@code bts_kerr_module.getState()}: the link, the hole's bonus, the run. */
    public static Map<String, Object> kerrModuleState(MTEKerrModuleBase<?> module) {
        final Map<String, Object> m = new LinkedHashMap<>();
        final MTEKerrModuleBase.Link link = module.getLink();
        m.put("formed", module.mMachine);
        m.put("link", link.key);
        m.put("linkText", link.text);
        m.put("mass", module.linkedMass());
        m.put("parallel", module.bonusParallel());
        m.put("speed", module.bonusSpeed());
        final IGregTechTileEntity base = module.getBaseMetaTileEntity();
        m.put("active", base != null && base.isActive());
        m.put("progress", module.mProgresstime);
        m.put("maxProgress", module.mMaxProgresstime);
        m.put("enabled", base != null && base.isAllowedToWork());
        return m;
    }

    // ================================================================================= hatches

    /** {@code bts_sensor.getState()}: the Stellar Field Sensor Hatch's channel, comparator, reading and output. */
    public static Map<String, Object> sensorState(MTEHatchStellarFieldSensor s) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "channel",
            s.channel()
                .name());
        m.put("threshold", s.getThreshold());
        m.put("comparator", s.isLessThan() ? "lt" : "gt");
        m.put("analog", s.isAnalog());
        final double v = s.getLastValue();
        m.put("value", Double.isNaN(v) ? null : v);
        m.put("output", s.currentOutput());
        return m;
    }

    /** {@code bts_sensor.setChannel(name)}: P, THETA, OMEGA, PI, SHIELD, INTEGRITY, ... Returns the channel set. */
    public static String sensorSetChannel(MTEHatchStellarFieldSensor s, String name) {
        for (FieldReadout.Channel c : FieldReadout.Channel.values()) {
            if (c.name()
                .equalsIgnoreCase(name)) {
                s.setChannelOrdinal(c.ordinal());
                break;
            }
        }
        return s.channel()
            .name();
    }

    /** {@code bts_directive.getState()}: the hatch's set, output mode, window, code, bits and output. */
    public static Map<String, Object> directiveHatchState(MTEHatchDirective h) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "set",
            h.getSet()
                .name());
        m.put("host", h.getHost().label);
        m.put(
            "output",
            h.getOutput()
                .name());
        m.put("invert", h.isInvert());
        m.put("window", h.getWindow());
        m.put("code", h.currentCode());
        m.put("redstone", h.currentOutput());
        m.put("controlIn", h.getControlIn());
        final Map<String, Object> bits = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) bits.put(h.label(i), (h.currentCode() & (1 << i)) != 0);
        m.put("bits", bits);
        return m;
    }

    /** {@code bts_directive.setSet(name)}: any set of the hatch's host (EYE_TRIMS, KERR_HOLE, ...). */
    public static String directiveSetSet(MTEHatchDirective h, String name) {
        final DirectiveSet s = DirectiveSet.parse(name);
        final DirectiveSet.Host host = h.getHost();
        if (s != null && (host == DirectiveSet.Host.NONE || s.host == host)) h.setSet(s);
        return h.getSet()
            .name();
    }

    /** {@code bts_directive.setOutput(name)}: CODE, BIT1..BIT4, ANY. */
    public static String directiveSetOutput(MTEHatchDirective h, String name) {
        for (DirectiveSet.Output o : DirectiveSet.Output.VALUES) {
            if (o.name()
                .equalsIgnoreCase(name)) h.setOutput(o);
        }
        return h.getOutput()
            .name();
    }
}
