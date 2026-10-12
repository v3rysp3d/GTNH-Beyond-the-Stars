package com.nateyburr.beyondthestars.integration.oc;

import net.minecraft.world.World;

import com.nateyburr.beyondthestars.integration.BTSComputerApi;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrModuleBase;

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

        @Callback(doc = "function():table -- milestones, the wanted core, escrow, ready, what is missing")
        public Object[] getTransition(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeTransition(e) };
        }

        @Callback(
            doc = "function(set:string[, w:number]):table -- a Directive code: EYE_TRIMS, EYE_STATUS, "
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
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetDial(e, a.checkDouble(0)) };
        }

        @Callback(doc = "function(v:number):number -- the T4 HSM feed setpoint (0-1.5 L/s per parallel); returns it")
        public Object[] setFeed(Context c, Arguments a) {
            final MTEEyeOfHarmonyBTS e = eye();
            return e == null ? GONE : new Object[] { BTSComputerApi.eyeSetFeed(e, a.checkDouble(0)) };
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

        @Callback(doc = "function(sm:number):number -- the mass setpoint (whole SM, 2..64, or 2..512 at Kerr T2)")
        public Object[] setSetpoint(Context c, Arguments a) {
            final MTEKerrEngine k = kee();
            return k == null ? GONE : new Object[] { BTSComputerApi.kerrSetSetpoint(k, a.checkInteger(0)) };
        }

        @Callback(doc = "function():boolean,string -- Open: needs a closed hole and a Black Hole Seed in a bolt bus")
        public Object[] open(Context c, Arguments a) {
            final MTEKerrEngine k = kee();
            return k == null ? GONE : BTSComputerApi.kerrOpen(k);
        }

        @Callback(doc = "function():boolean,string -- Collapse: needs an open hole and a Collapser in a bolt bus")
        public Object[] collapse(Context c, Arguments a) {
            final MTEKerrEngine k = kee();
            return k == null ? GONE : BTSComputerApi.kerrCollapse(k);
        }

        @Callback(doc = "function(on:boolean):boolean -- Auto: a Seed or Collapser in a bolt bus acts on its own")
        public Object[] setAuto(Context c, Arguments a) {
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
            final MTEHatchStellarFieldSensor s = sensor();
            return s == null ? GONE : new Object[] { BTSComputerApi.sensorSetChannel(s, a.checkString(0)) };
        }

        @Callback(
            doc = "function(v:number[, comparator:string[, analog:boolean]]):table -- threshold, gt or lt, analog")
        public Object[] setThreshold(Context c, Arguments a) {
            final MTEHatchStellarFieldSensor s = sensor();
            if (s == null) return GONE;
            s.setThreshold(a.checkDouble(0));
            if (a.count() > 1) s.setLessThan("lt".equalsIgnoreCase(a.checkString(1)));
            if (a.count() > 2) s.setAnalog(a.checkBoolean(2));
            return new Object[] { BTSComputerApi.sensorState(s) };
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

        @Callback(
            doc = "function(name:string):string -- the set: EYE_TRIMS, EYE_STATUS, EYE_TRANSITION, EYE_DIAL, "
                + "EYE_FEED, KERR_HOLE, KERR_STATUS, KERR_SETPOINT (the host's own only)")
        public Object[] setSet(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveSetSet(h, a.checkString(0)) };
        }

        @Callback(doc = "function(mode:string):string -- CODE, BIT1, BIT2, BIT3, BIT4 or ANY")
        public Object[] setOutput(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            return h == null ? GONE : new Object[] { BTSComputerApi.directiveSetOutput(h, a.checkString(0)) };
        }

        @Callback(doc = "function(w:number):number -- the window (1-60)")
        public Object[] setWindow(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            if (h == null) return GONE;
            h.setWindow(a.checkInteger(0));
            return new Object[] { h.getWindow() };
        }

        @Callback(doc = "function(on:boolean):boolean -- invert the output")
        public Object[] setInvert(Context c, Arguments a) {
            final MTEHatchDirective h = hatch();
            if (h == null) return GONE;
            h.setInvert(a.checkBoolean(0));
            return new Object[] { h.isInvert() };
        }
    }
}
