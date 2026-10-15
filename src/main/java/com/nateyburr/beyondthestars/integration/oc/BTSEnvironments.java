package com.nateyburr.beyondthestars.integration.oc;

import net.minecraft.world.World;

import com.nateyburr.beyondthestars.integration.BTSComputerApi;
import com.nateyburr.beyondthestars.integration.ConduitComputerApi;
import com.nateyburr.beyondthestars.integration.QuasarComputerApi;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitElement;
import com.nateyburr.beyondthestars.machines.conduit.MTEConduitJunction;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrModuleBase;
import com.nateyburr.beyondthestars.machines.quasar.MTEBurstThermaliserCrown;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/**
 * AUTOMATION PASS: the OpenComputers components (docs/automation.md §4). Each finds its machine again on every call
 * (a wrench or a chunk reload replaces the tile), runs on the server thread (no {@code direct} callbacks) and goes
 * through {@link BTSComputerApi}, which uses the GUI's own setters: the same clamps and safety rules as the GUI.
 */
public final class BTSEnvironments {

    private BTSEnvironments() {}

    private static final Object[] GONE = { null, "the machine is gone" };
    /** A setter called again within 20 ticks (quasar-p4.md §8.2: a script cannot beat the 1 s lag). */
    private static final Object[] RATE_LIMITED = { null, BTSComputerApi.RATE_MESSAGE };

    /** One Adapter-side component on one block. */
    abstract static class Base extends ManagedEnvironment implements NamedBlock {

        private final World world;
        private final int x, y, z;
        private final String name;

        Base(World world, int x, int y, int z, String name) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            setNode(
                Network.newNode(this, Visibility.Network)
                    .withComponent(name, Visibility.Network)
                    .create());
        }

        IMetaTileEntity mte() {
            return BTSDriver.mteAt(world, x, y, z);
        }

        /** True when a setter must refuse: the same method on this machine within 20 ticks. */
        boolean limited(String method) {
            return !BTSComputerApi.rateOk(mte(), method);
        }

        @Override
        public String preferredName() {
            return name;
        }

        @Override
        public int priority() {
            return 10;
        }
    }

    /** {@code bts_eye}: the Eye of Harmony controller. */
    public static final class Eye extends Base {

        Eye(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_eye");
        }

        private MTEEyeOfHarmonyBTS eye() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEEyeOfHarmonyBTS ? (MTEEyeOfHarmonyBTS) m : null;
        }

        @Callback(
            doc = "function():table -- tiers, star, state, transition, dial and feed setpoints, upkeep r, last event")
        public Object[] getState(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeState(e) };
        }

        @Callback(
            doc = "function():table -- the stability field last second: P, theta, omega, pi, shield, integrity, "
                + "stability, zone, yield, warning")
        public Object[] getField(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeField(e) };
        }

        @Callback(doc = "function():table -- the input buffers, fluid name -> litres")
        public Object[] getBuffers(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeBuffers(e) };
        }

        @Callback(doc = "function():table -- the intake caps, fluid name -> litres the sockets fill that buffer to")
        public Object[] getIntakeCaps(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeIntakeCaps(e) };
        }

        @Callback(doc = "function():table -- milestones, the wanted core, escrow, ready, what is missing")
        public Object[] getTransition(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeTransition(e) };
        }

        @Callback(
            doc = "function(set:string[, w:number]):table -- a Directive code: EYE_TRIMS, EYE_STATUS, EYE_FIELD, EYE_EVENTS, EYE_GALAXY_TRANSITION, EYE_SLOTS, "
                + "EYE_TRANSITION (w = gauge band, default 10)")
        public Object[] getDirective(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE
                : new Object[] { BTSComputerApi.directive(e, a.checkString(0), a.optInteger(1, 10)) };
        }

        @Callback(
            doc = "function(v:number):number -- the time dial setpoint (1-3, 0.25 steps; capped in use by the "
                + "Time Dilation generators); returns the new setpoint")
        public Object[] setDial(Context c, Arguments a) {
            if (limited("setDial")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetDial(e, a.checkDouble(0)) };
        }

        @Callback(doc = "function(v:number):number -- the T4 HSM feed setpoint (0-1.5 L/s per parallel); returns it")
        public Object[] setFeed(Context c, Arguments a) {
            if (limited("setFeed")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetFeed(e, a.checkDouble(0)) };
        }

        // ---- P4 (T7+; quasar-p4.md §8.2)

        @Callback(
            doc = "function():table -- the running countdown: kind (collapse, pass, crossing, merger), secondsLeft,"
                + " sign")
        public Object[] getEvent(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeEvent(e) };
        }

        @Callback(
            doc = "function():table -- the link milestone: kind (stability, heavy), streak, best, mean, frac, linked,"
                + " source; ready; testFireWanted; quasarStream (Bu/t); testFireStream (what J1 at 15 sends: the"
                + " stream or J1's out 1 limit); testFireShare (1 up to 32 Bu/t, else 16 / testFireStream); j1Limit"
                + " (the standard rig's, Bu/t); supermassiveLink (P5's T9 -> T10 data: streak, best, held, mean, need,"
                + " sourceMass, sourceLattice, sourceQualifies)")
        public Object[] getUnlock(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeUnlock(e) };
        }

        @Callback(doc = "function():string -- begins a ready Magnetar Collapse or Galactic Merger (else says why not)")
        public Object[] beginTransition(Context c, Arguments a) {
            if (limited("beginTransition")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeBegin(e) };
        }

        @Callback(doc = "function(v:number):number -- the T8 Ergomatter feed f8 (0-10 L/s per parallel); returns it")
        public Object[] setFeed8(Context c, Arguments a) {
            if (limited("setFeed8")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetFeed8(e, a.checkDouble(0)) };
        }

        @Callback(doc = "function(mode:string):string -- galaxy or planets (puts a lit T7 / T8 field out); returns it")
        public Object[] setMode(Context c, Arguments a) {
            if (limited("setMode")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetMode(e, a.checkString(0)) };
        }

        @Callback(
            doc = "function(kind:string, units:number):string -- one second's trim pulse (T7+: \"space\"; any trim of the"
                + " burning star by key or name) from the gas / stellar sockets, on top of the Field sockets, same cap")
        public Object[] trim(Context c, Arguments a) {
            if (limited("trim")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE
                : new Object[] { e.starRuntime()
                    .requestTrim(a.checkString(0), a.checkDouble(1)) };
        }

        @Callback(doc = "function():table -- mode, planet slots, progress, companions, idle, companionShort")
        public Object[] getSlots(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSlots(e) };
        }

        // ---- P5 (p5-t9-t10.md §9.3)

        @Callback(
            doc = "function():table -- the T9 pellet beam: rho, gev, rate (pellets/s), cap, grade, holdRating, stock,"
                + " level, rhoMax, acceptLimit, firing")
        public Object[] getBeam(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeBeam(e) };
        }

        @Callback(
            doc = "function(c:number[, k:number]):table -- the injector's rate-law centre (negative: auto) and gain;"
                + " returns getBeam()")
        public Object[] setBeam(Context c, Arguments a) {
            if (limited("setBeam")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            if (e == null) return GONE;
            BTSComputerApi.eyeSetBeam(e, a.checkDouble(0), a.count() > 1 ? a.checkDouble(1) : Double.NaN);
            return new Object[] { BTSComputerApi.eyeBeam(e) };
        }

        @Callback(doc = "function(grade:string):string -- the grade fired: magnetar, ergomatter, superdense or auto")
        public Object[] setGrade(Context c, Arguments a) {
            if (limited("setGrade")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetGrade(e, a.checkString(0)) };
        }

        @Callback(
            doc = "function():table -- the Galactic Flow: secondsLeft (to arrival, negative while it surges), side"
                + " (1 high, -1 low), size, accepted, surging, held (s Stable), galaxies, flowsHeld, last")
        public Object[] getFlow(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeFlow(e) };
        }

        @Callback(
            doc = "function(n:number):number -- the flow accept limit 0-12 (negative: the hold rating); returns it")
        public Object[] setAcceptLimit(Context c, Arguments a) {
            if (limited("setAcceptLimit")) return RATE_LIMITED;
            final MTEEyeOfHarmonyBTS e = eye();
            if (e == null) return GONE;
            e.starRuntime()
                .setAcceptLimit(a.checkInteger(0));
            return new Object[] { e.starRuntime()
                .attractor()
                .acceptLimit() };
        }

        @Callback(
            doc = "function():table -- the T10 universe: age, H, setpointPhase, nextEvent (recombination / dawn),"
                + " secondsToEvent, dawnOk, chargeOwed, chargeShort, universes, dawns")
        public Object[] getUniverse(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeUniverse(e) };
        }
    }

    /** P5 {@code bts_injector}: the Pellet Injector Hatch (MTE 28128). */
    public static final class Injector extends Base {

        Injector(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_injector");
        }

        private com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector inj() {
            final IMetaTileEntity m = mte();
            return m instanceof com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector
                ? (com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector) m
                : null;
        }

        @Callback(doc = "function():table -- pellets by grade (magnetar, ergomatter, superdense) and the barrel Mk")
        public Object[] getStock(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector i = inj();
            if (i == null) return GONE;
            final java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("magnetar", i.pellets(1));
            m.put("ergomatter", i.pellets(2));
            m.put("superdense", i.pellets(3));
            m.put("barrel", i.barrelMk());
            return new Object[] { m };
        }

        @Callback(doc = "function():table -- the rate law's settings: cap, centre (or auto), gain, locked grade")
        public Object[] getRate(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector i = inj();
            if (i == null) return GONE;
            final java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("cap", com.nateyburr.beyondthestars.machines.eye.field.AttractorField.cap(i.barrelMk()));
            m.put("centre", i.centreAuto() ? "auto" : (Object) i.centre());
            m.put("gain", i.gain());
            m.put("grade", com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor.gradeName(i.lockedGrade()));
            return new Object[] { m };
        }
    }

    /** {@code bts_kerr}: the Kerr Ergosphere Engine controller. */
    public static final class Kerr extends Base {

        Kerr(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_kerr");
        }

        private MTEKerrEngine kee() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEKerrEngine ? (MTEKerrEngine) m : null;
        }

        @Callback(doc = "function():table -- open, mass, setpoint, bleed, bolts, stability, power, stock, modules")
        public Object[] getState(Context c, Arguments a) {
            final MTEKerrEngine k = kee();
            return k == null ? GONE : new Object[] { BTSComputerApi.kerrState(k) };
        }

        @Callback(
            doc = "function(set:string[, w:number]):table -- a Directive code: KERR_HOLE, KERR_STATUS "
                + "(w = bolt reserve in minutes, default 10)")
        public Object[] getDirective(Context c, Arguments a) {
            final MTEKerrEngine k = kee();
            return k == null ? GONE
                : new Object[] { BTSComputerApi.directive(k, a.checkString(0), a.optInteger(1, 10)) };
        }

        @Callback(
            doc = "function(sm:number):number -- the mass setpoint (whole SM, 2..64, or 2..512 at Kerr T2; 768 while a"
                + " quasar link holds sigma >= 1)")
        public Object[] setSetpoint(Context c, Arguments a) {
            if (limited("setSetpoint")) return RATE_LIMITED;
            final MTEKerrEngine k = kee();
            return k == null ? GONE : new Object[] { BTSComputerApi.kerrSetSetpoint(k, a.checkInteger(0)) };
        }

        @Callback(doc = "function():boolean,string -- Open: needs a closed hole and a Black Hole Seed in a bolt bus")
        public Object[] open(Context c, Arguments a) {
            if (limited("open")) return RATE_LIMITED;
            final MTEKerrEngine k = kee();
            return k == null ? GONE : BTSComputerApi.kerrOpen(k);
        }

        @Callback(doc = "function():boolean,string -- Collapse: needs an open hole and a Collapser in a bolt bus")
        public Object[] collapse(Context c, Arguments a) {
            if (limited("collapse")) return RATE_LIMITED;
            final MTEKerrEngine k = kee();
            return k == null ? GONE : BTSComputerApi.kerrCollapse(k);
        }

        @Callback(doc = "function(on:boolean):boolean -- Auto: a Seed or Collapser in a bolt bus acts on its own")
        public Object[] setAuto(Context c, Arguments a) {
            if (limited("setAuto")) return RATE_LIMITED;
            final MTEKerrEngine k = kee();
            return k == null ? GONE : new Object[] { BTSComputerApi.kerrSetAuto(k, a.checkBoolean(0)) };
        }
    }

    /** {@code bts_kerr_module}: a Kerr Compressor or Extractor Module controller. */
    public static final class KerrModule extends Base {

        KerrModule(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_kerr_module");
        }

        @Callback(doc = "function():table -- link, the hole's mass, parallel and speed bonus, run progress")
        public Object[] getState(Context c, Arguments a) {
            final IMetaTileEntity m = mte();
            return m instanceof MTEKerrModuleBase
                ? new Object[] { BTSComputerApi.kerrModuleState((MTEKerrModuleBase<?>) m) }
                : GONE;
        }
    }

    /** {@code bts_sensor}: a Stellar Field Sensor Hatch. */
    public static final class Sensor extends Base {

        Sensor(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_sensor");
        }

        private MTEHatchStellarFieldSensor sensor() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEHatchStellarFieldSensor ? (MTEHatchStellarFieldSensor) m : null;
        }

        @Callback(doc = "function():table -- channel, threshold, comparator, analog, value, output")
        public Object[] getState(Context c, Arguments a) {
            final MTEHatchStellarFieldSensor s = sensor();
            return s == null ? GONE : new Object[] { BTSComputerApi.sensorState(s) };
        }

        @Callback(
            doc = "function(name:string):string -- P, THETA, OMEGA, PI, SHIELD, INTEGRITY, STABILITY, ZONE, "
                + "WARN_PLUS, WARN_MINUS, WARN_ANY, UPKEEP_OK, SETPOINT_ERROR")
        public Object[] setChannel(Context c, Arguments a) {
            if (limited("setChannel")) return RATE_LIMITED;
            final MTEHatchStellarFieldSensor s = sensor();
            return s == null ? GONE : new Object[] { BTSComputerApi.sensorSetChannel(s, a.checkString(0)) };
        }

        @Callback(
            doc = "function(v:number[, comparator:string[, analog:boolean]]):table -- threshold, gt or lt, analog")
        public Object[] setThreshold(Context c, Arguments a) {
            if (limited("setThreshold")) return RATE_LIMITED;
            final MTEHatchStellarFieldSensor s = sensor();
            if (s == null) return GONE;
            final double v = a.checkDouble(0);
            if (Double.isNaN(v)) return new Object[] { null, "threshold is not a number" };
            s.setThreshold(v);
            if (a.count() > 1) s.setLessThan("lt".equalsIgnoreCase(a.checkString(1)));
            if (a.count() > 2) s.setAnalog(a.checkBoolean(2));
            return new Object[] { BTSComputerApi.sensorState(s) };
        }
    }

    /** {@code bts_quasar}: the Quasar Engine controller (P4, quasar-p4.md §8.2). */
    public static final class Quasar extends Base {

        Quasar(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_quasar");
        }

        private com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q() {
            final IMetaTileEntity m = mte();
            return m instanceof com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine
                ? (com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine) m
                : null;
        }

        @Callback(
            doc = "function():table -- state, E, phi, lambda, a, M, cap, integrity, zone, Yq, stability, b, power")
        public Object[] getDisc(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.disc(q) };
        }

        @Callback(
            doc = "function():table -- Bu/t total (totalEUt: x 10^13 EU/t; totalText), top, bottom, the dock on each"
                + " port, vented")
        public Object[] getBurst(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.burst(q) };
        }

        @Callback(doc = "function():number -- the feed setpoint f (0-1.5)")
        public Object[] getFeed(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { q.getFeed() };
        }

        @Callback(doc = "function(f:number):number -- set the feed setpoint f (0-1.5); returns it")
        public Object[] setFeed(Context c, Arguments a) {
            if (limited("setFeed")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.setFeed(q, a.checkDouble(0)) };
        }

        @Callback(doc = "function(name:string):string -- the feed gas: H, He or RSM; returns the gas in use")
        public Object[] setGas(Context c, Arguments a) {
            if (limited("setGas")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.setGas(q, a.checkString(0)) };
        }

        @Callback(doc = "function(b:number):number -- the coil field b (0.5 up to the coil tier); returns it")
        public Object[] setField(Context c, Arguments a) {
            if (limited("setField")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.setField(q, a.checkDouble(0)) };
        }

        @Callback(
            doc = "function(kind:string, units:number):string -- one second's trim pulse: \"gas\" (from the feed buffer)"
                + " or \"space\" (Spatially Enlarged Fluid from the gas sockets), 0-10 units on top of the trim hatches")
        public Object[] trim(Context c, Arguments a) {
            if (limited("trim")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { q.requestTrim(a.checkString(0), a.checkDouble(1)) };
        }

        @Callback(doc = "function():boolean -- purge the flux now; false below flux 20 or with no hole")
        public Object[] purge(Context c, Arguments a) {
            if (limited("purge")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { q.requestPurge() };
        }

        @Callback(doc = "function():table -- the hosts fed: host, Bu/t, sigma, link streak")
        public Object[] getLinks(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.links(q) };
        }

        @Callback(doc = "function():table -- First Light, the first eruption, the Eddington hour, mass milestones")
        public Object[] getMilestones(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.milestones(q) };
        }

        @Callback(doc = "function([w:number]):table -- the Disc set's code and bits (w = E band, default 5)")
        public Object[] getDirective(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.directive(q, a.optInteger(0, 5)) };
        }

        // ---- SMBH (smbh-v1.md §6): the Supermassive Lattice

        @Callback(
            doc = "function():table -- lattice (in force), latticeBuilt, saturated, M, safe, top, setpoint, x, unbound,"
                + " n, shedding, massPerHourPct, supermassiveLinkSource")
        public Object[] getBand(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.band(q) };
        }

        @Callback(doc = "function():table -- last second's intake L/s: disc, holdUp, growth, total, cap (5x), capPct")
        public Object[] getIntake(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.intake(q) };
        }

        @Callback(doc = "function():table -- the hold-up: need, paid (L/s), coverage, leaking, leakPerHourPct")
        public Object[] getHoldUp(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.holdUp(q) };
        }

        @Callback(
            doc = "function():table -- the Accretion Buffer: litres, cap, seconds (-1: nothing needed), feedShort")
        public Object[] getBuffer(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.buffer(q) };
        }

        @Callback(
            doc = "function():table -- Overdrive level, inUse, available (L3), upkeepEach (L/s), coverage, outputX,"
                + " bursts (Bu/t), burstsEUt, burstsText")
        public Object[] getOverdrive(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.overdrive(q) };
        }

        @Callback(doc = "function():table -- allowed, unbound, n, need (L/s Primordial Matter), paid, coverage, short")
        public Object[] getAnchor(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.anchor(q) };
        }

        @Callback(doc = "function():table -- the Horizon set's code and bits")
        public Object[] getHorizon(Context c, Arguments a) {
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.horizon(q) };
        }

        @Callback(
            doc = "function(M:number):number -- the mass setpoint, SM (0 = the safe limit; 4,096 .. the top); returns"
                + " the setpoint in force")
        public Object[] setMassSetpoint(Context c, Arguments a) {
            if (limited("setMassSetpoint")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.setMassSetpoint(q, a.checkDouble(0)) };
        }

        @Callback(doc = "function(o:number):number -- the Overdrive level 0-1 (5 % steps; needs L3); returns it")
        public Object[] setOverdrive(Context c, Arguments a) {
            if (limited("setOverdrive")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { QuasarComputerApi.setOverdrive(q, a.checkDouble(0)) };
        }

        @Callback(
            doc = "function(on:boolean):boolean,string -- Unbind (needs an L4 lattice and smbhAllowUnbound); off sheds"
                + " to 67,108,864 SM; returns Unbound and why not")
        public Object[] setUnbound(Context c, Arguments a) {
            if (limited("setUnbound")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            if (q == null) return GONE;
            final String why = q.unbind(a.checkBoolean(0));
            return new Object[] { q.isUnbound(), why };
        }

        @Callback(
            doc = "function():string -- lower the mass setpoint to the safe limit (a safe shed); says what it did")
        public Object[] shed(Context c, Arguments a) {
            if (limited("shed")) return RATE_LIMITED;
            final com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine q = q();
            return q == null ? GONE : new Object[] { q.shedToSafe() };
        }
    }

    /**
     * {@code bts_mouth} / {@code bts_mirror} / {@code bts_junction}: a Conduit element (P4, quasar-p4.md section 8.2).
     */
    public static class Conduit extends Base {

        Conduit(World world, int x, int y, int z, String name) {
            super(world, x, y, z, name);
        }

        MTEConduitElement<?> element() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEConduitElement ? (MTEConduitElement<?>) m : null;
        }

        @Callback(doc = "function():table -- formed, open, through / clipped / lost Bu/t, the partner (mouth), routes")
        public Object[] getLink(Context c, Arguments a) {
            final MTEConduitElement<?> e = element();
            return e == null ? GONE : new Object[] { ConduitComputerApi.link(e) };
        }

        @Callback(doc = "function():table -- the Conduit Throat: mk (0 = none) and cap Bu/t")
        public Object[] getThroat(Context c, Arguments a) {
            final MTEConduitElement<?> e = element();
            return e == null ? GONE : new Object[] { ConduitComputerApi.throat(e) };
        }

        @Callback(doc = "function():table -- fuel mode, L/s on each fuel, open, reopen seconds, litres used")
        public Object[] getUpkeep(Context c, Arguments a) {
            final MTEConduitElement<?> e = element();
            return e == null ? GONE : new Object[] { ConduitComputerApi.upkeep(e) };
        }

        @Callback(doc = "function(mode:string):string -- throat fuel: auto, spacetime or ergomatter; returns it")
        public Object[] setFuel(Context c, Arguments a) {
            if (limited("setFuel")) return RATE_LIMITED;
            final MTEConduitElement<?> e = element();
            return e == null ? GONE : new Object[] { ConduitComputerApi.setFuel(e, a.checkString(0)) };
        }

        @Callback(doc = "function():boolean -- open (upkeep paid, a throat fitted)")
        public Object[] isOpen(Context c, Arguments a) {
            final MTEConduitElement<?> e = element();
            return e == null ? GONE : new Object[] { e.isOpen() };
        }
    }

    /** {@code bts_junction}: a Conduit Junction (split / combine). */
    public static final class Junction extends Conduit {

        Junction(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_junction");
        }

        private MTEConduitJunction junction() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEConduitJunction ? (MTEConduitJunction) m : null;
        }

        @Callback(doc = "function():table -- mode, in, out1, out2, out3 (or out), clipped, c, k, p3, limit, s, share")
        public Object[] getFlow(Context c, Arguments a) {
            final MTEConduitJunction j = junction();
            return j == null ? GONE : new Object[] { ConduitComputerApi.flow(j) };
        }

        @Callback(doc = "function(c:number):number -- the split ratio c (0-1); returns it")
        public Object[] setRatio(Context c, Arguments a) {
            if (limited("setRatio")) return RATE_LIMITED;
            final MTEConduitJunction j = junction();
            if (j == null) return GONE;
            j.setRatio(a.checkDouble(0));
            return new Object[] { j.getRatio() };
        }

        @Callback(doc = "function(k:number):number -- the redstone gain k (0, 1, 2 or 4; snaps); returns it")
        public Object[] setGain(Context c, Arguments a) {
            if (limited("setGain")) return RATE_LIMITED;
            final MTEConduitJunction j = junction();
            return j == null ? GONE : new Object[] { j.setGain(a.checkDouble(0)) };
        }

        @Callback(doc = "function(p:number):number -- out 3's fixed share (0-1) taken off the top; returns it")
        public Object[] setOut3(Context c, Arguments a) {
            if (limited("setOut3")) return RATE_LIMITED;
            final MTEConduitJunction j = junction();
            if (j == null) return GONE;
            j.setOut3Share(a.checkDouble(0));
            return new Object[] { j.getOut3Share() };
        }

        @Callback(
            doc = "function(bu:number):number -- out 1's limit in Bu/t (0 = none; the Eye's J1: 7.5 per parallel); returns it")
        public Object[] setLimit(Context c, Arguments a) {
            if (limited("setLimit")) return RATE_LIMITED;
            final MTEConduitJunction j = junction();
            if (j == null) return GONE;
            j.setLimit(a.checkDouble(0));
            return new Object[] { j.getLimit() };
        }

        @Callback(doc = "function(mode:string):string -- split or combine; returns it")
        public Object[] setMode(Context c, Arguments a) {
            if (limited("setMode")) return RATE_LIMITED;
            final MTEConduitJunction j = junction();
            if (j == null) return GONE;
            j.setMode(
                "combine".equalsIgnoreCase(a.checkString(0)) ? MTEConduitJunction.Mode.COMBINE
                    : MTEConduitJunction.Mode.SPLIT);
            return new Object[] { j.getMode() == MTEConduitJunction.Mode.SPLIT ? "split" : "combine" };
        }
    }

    /** {@code bts_crown}: a Burst Thermaliser Crown. */
    public static final class Crown extends Base {

        Crown(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_crown");
        }

        private MTEBurstThermaliserCrown crown() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEBurstThermaliserCrown ? (MTEBurstThermaliserCrown) m : null;
        }

        @Callback(doc = "function():number -- EU/t pushed to the wireless network last second")
        public Object[] getPower(Context c, Arguments a) {
            final MTEBurstThermaliserCrown k = crown();
            return k == null ? GONE : new Object[] { k.getEUt() };
        }

        @Callback(doc = "function():table -- Mk, stream, used, EU/t, clipped, stacked, lifetime EU")
        public Object[] getStream(Context c, Arguments a) {
            final MTEBurstThermaliserCrown k = crown();
            return k == null ? GONE : new Object[] { ConduitComputerApi.crown(k) };
        }

        @Callback(doc = "function():number -- the Mk that stands (0 = not formed)")
        public Object[] getMk(Context c, Arguments a) {
            final MTEBurstThermaliserCrown k = crown();
            return k == null ? GONE : new Object[] { k.getMk() };
        }
    }

    /** {@code bts_receptor}: a GRB Receptor Port. */
    public static final class Receptor extends Base {

        Receptor(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_receptor");
        }

        @Callback(doc = "function():table -- total Bu/t last second, bySource, linked, source, paused, host")
        public Object[] getDelivery(Context c, Arguments a) {
            final IMetaTileEntity m = mte();
            return m instanceof MTEHatchGRBReceptor
                ? new Object[] { ConduitComputerApi.delivery((MTEHatchGRBReceptor) m) }
                : GONE;
        }
    }

    /** {@code bts_homogeny}: the Homogeny controller (P4: its quasar link, quasar-p4.md §3.1, §8.3). */
    public static final class Homogeny extends Base {

        Homogeny(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_homogeny");
        }

        @Callback(doc = "function():table -- tier, parallels, speed, demand, entanglementOpen, running, progress, link")
        public Object[] getState(Context c, Arguments a) {
            final IMetaTileEntity m = mte();
            return m instanceof com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny
                ? new Object[] {
                    BTSComputerApi.homogenyState((com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny) m) }
                : GONE;
        }
    }

    /** {@code bts_reality}: Reality Diffraction (P4: its burst-driven regions, quasar-p4.md §3.1, §8.3). */
    public static final class Reality extends Base {

        Reality(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_reality");
        }

        @Callback(doc = "function():table -- burstDriven, speed, demand, running, progress, anchorMask, link")
        public Object[] getState(Context c, Arguments a) {
            final IMetaTileEntity m = mte();
            return m instanceof com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction
                ? new Object[] { BTSComputerApi
                    .realityState((com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction) m) }
                : GONE;
        }
    }

    /** {@code bts_directive}: a Directive Hatch. */
    public static final class Directive extends Base {

        Directive(World world, int x, int y, int z) {
            super(world, x, y, z, "bts_directive");
        }

        private MTEHatchDirective hatch() {
            final IMetaTileEntity m = mte();
            return m instanceof MTEHatchDirective ? (MTEHatchDirective) m : null;
        }

        @Callback(doc = "function():table -- set, host, output mode, window, code, bits by name, redstone out")
        public Object[] getState(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveHatchState(h) };
        }

        @Callback(doc = "function():number -- the current 4-bit code")
        public Object[] getCode(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { h.currentCode() };
        }

        @Callback(doc = "function():table -- code, on[1..4] (bits [1] [2] [4] [8]) and names[1..4]")
        public Object[] getBits(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveBits(h) };
        }

        @Callback(
            doc = "function(name:string):string -- the set: EYE_TRIMS, EYE_STATUS, EYE_TRANSITION, EYE_FIELD, "
                + "EYE_EVENTS, EYE_GALAXY_TRANSITION, EYE_SLOTS, EYE_DIAL, EYE_FEED, KERR_HOLE, KERR_STATUS, "
                + "KERR_SETPOINT, QUASAR_DISC (the host's own only)")
        public Object[] setSet(Context c, Arguments a) {
            if (limited("setSet")) return RATE_LIMITED;
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveSetSet(h, a.checkString(0)) };
        }

        @Callback(doc = "function(mode:string):string -- CODE, BIT1, BIT2, BIT3, BIT4 or ANY")
        public Object[] setOutput(Context c, Arguments a) {
            if (limited("setOutput")) return RATE_LIMITED;
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveSetOutput(h, a.checkString(0)) };
        }

        @Callback(doc = "function(w:number):number -- the window (1-60)")
        public Object[] setWindow(Context c, Arguments a) {
            if (limited("setWindow")) return RATE_LIMITED;
            final MTEHatchDirective h = hatch();
            if (h == null) return GONE;
            h.setWindow(a.checkInteger(0));
            return new Object[] { h.getWindow() };
        }

        @Callback(doc = "function(on:boolean):boolean -- invert the output")
        public Object[] setInvert(Context c, Arguments a) {
            if (limited("setInvert")) return RATE_LIMITED;
            final MTEHatchDirective h = hatch();
            if (h == null) return GONE;
            h.setInvert(a.checkBoolean(0));
            return new Object[] { h.isInvert() };
        }
    }
}
