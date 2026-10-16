package com.nateyburr.beyondthestars.machines.eye;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.StatCollector;

import com.nateyburr.beyondthestars.machines.conduit.MTEHatchBurstControl;
import com.nateyburr.beyondthestars.machines.conduit.MTEHatchGRBReceptor;
import com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;

/**
 * SINGULARITY LADDER P3: the Eye of Harmony's sockets (singularity-ladder-build.md §3.3, "Structure
 * export" sockets table). Owned by the Eye unit.
 * <p>
 * <b>Trap 3.</b> The input sockets never join GT's {@code mInputHatches}: they register into the
 * Eye's own lists and are drained every second into {@code long} buffers, the way TecTech's EOH
 * drains its hatches. A band's sockets are dropped again if the band's walk fails
 * (mark-and-truncate), so a half-built band feeds nothing. The ME outputs are the Eye's ONLY output
 * path, so they register into GT's {@code mOutputBusses} / {@code mOutputHatches}, which GT's
 * ejection helpers read.
 * <p>
 * <b>D-49</b> (d49-build-plan.md §11.2): {@link #Field} (the stability-field trims, never buffered),
 * {@link #Sensor} (the Stellar Field Sensor Hatch) and {@link #Items} (input buses on the plinth gas
 * sockets, for the Stellar Ignition Charges). An ME stocking input hatch in a Field socket is walked,
 * so the Eye still forms, but it goes to a rejected list and is never drained (R-15).
 */
public enum EyeHatchElement implements IHatchElement<MTEEyeOfHarmonyBTS> {

    /** Plinth gas sockets (any input hatch, ME stocking included): H and He, or any Eye input. */
    Gas(MTEEyeOfHarmonyBTS::addGasHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.gasHatchCount();
        }
    },
    /** Beam-band and jet-crescent sockets: the stellar fluids (RSPM, ESPM, HSM, WDM, coolant). */
    Stellar(MTEEyeOfHarmonyBTS::addStellarHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.stellarHatchCount();
        }
    },
    /**
     * D-49 Field sockets ({@code f}, core band): plain input hatches whose fluids are trims, drained per field
     * second by the star runtime and never buffered. ME stocking hatches register as rejected (R-15).
     */
    Field(MTEEyeOfHarmonyBTS::addFieldHatch, MTEHatchInput.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.fieldHatchCount() + t.rejectedFieldHatchCount();
        }
    },
    /** D-49 Sensor sockets ({@code n}, core band): Stellar Field Sensor Hatches (MTE 28116) only. */
    Sensor(MTEEyeOfHarmonyBTS::addSensorHatch, MTEHatchStellarFieldSensor.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.sensorHatchCount();
        }
    },
    /** AUTOMATION PASS: Directive Hatches (MTE 28127) also fit the Sensor sockets ({@code n}). */
    Directive(MTEEyeOfHarmonyBTS::addDirectiveHatch, MTEHatchDirective.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.directiveHatchCount();
        }
    },
    /** D-49 item input on the plinth gas sockets ({@code h}): input buses, ME input bus included (R-1). */
    Items(MTEEyeOfHarmonyBTS::addItemBus, MTEHatchInputBus.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.itemBusCount();
        }
    },
    /**
     * P4 ('r', the T7 rail and the T8 halo): GRB Receptor Ports (MTE 28125). The group (rail-high, rail-low, halo) is
     * the manifest's for the socket's offset.
     */
    Receptor(MTEEyeOfHarmonyBTS::addReceptor, MTEHatchGRBReceptor.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.receptorCount();
        }
    },
    /** P4 ('d'): a Burst Control Hatch (MTE 28126) in Begin mode: a rising edge begins a galaxy transition. */
    BurstControl(MTEEyeOfHarmonyBTS::addBurstControl, MTEHatchBurstControl.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.burstControlCount();
        }
    },
    /**
     * P5 ('i', the T9 top collar): a Pellet Injector Hatch (MTE 28128), the injector's Burst Control Hatch (mode Pellet
     * beam), or an input bus holding more pellets for it (ME stocking buses included).
     */
    Pellet(MTEEyeOfHarmonyBTS::addPelletSocket, MTEHatchPelletInjector.class, MTEHatchBurstControl.class,
        MTEHatchInputBus.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.pelletSocketCount();
        }
    },
    /** P4 ('p'): the slot bus, an input bus (ME included): planet items and companion star charts, never consumed. */
    SlotBus(MTEEyeOfHarmonyBTS::addSlotBus, MTEHatchInputBus.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.slotBusCount();
        }
    },
    /**
     * Plinth output sockets ({@code o}): the ME output bus. The Eye needs one ME output bus AND one ME output hatch, so
     * the bus and the hatch are two elements with their own counts: GT's {@code atLeast(OutputBus, OutputHatch)} then
     * places one of each in the two sockets. As one element counting both, auto-place and the NEI preview stopped
     * after the bus and paved the second socket with casing, and the Eye did not form.
     */
    OutputBus(MTEEyeOfHarmonyBTS::addOutputBusME, MTEHatchOutputBusME.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.outputBusCount();
        }
    },
    /** Plinth output sockets ({@code o}): the ME output hatch (long-sized fluid outputs). See {@link #OutputBus}. */
    OutputHatch(MTEEyeOfHarmonyBTS::addOutputHatchME, MTEHatchOutputME.class) {

        @Override
        public long count(MTEEyeOfHarmonyBTS t) {
            return t.outputHatchCount();
        }
    };

    private final List<Class<? extends IMetaTileEntity>> mteClasses;
    private final IGTHatchAdder<MTEEyeOfHarmonyBTS> adder;

    @SafeVarargs
    EyeHatchElement(IGTHatchAdder<MTEEyeOfHarmonyBTS> adder, Class<? extends IMetaTileEntity>... mteClasses) {
        this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
        this.adder = adder;
    }

    /**
     * B-249: the name GT's structure errors and the NEI hatch list print. GT's default key {@code hatch_type_<name>}
     * has no translation, so a generic error read "hatch_type_stellar".
     */
    @Override
    public String getDescriptionLangKey() {
        return "beyondthestars.hatch.eye." + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getDisplayName() {
        return StatCollector.translateToLocal(getDescriptionLangKey());
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return mteClasses;
    }

    @Override
    public IGTHatchAdder<? super MTEEyeOfHarmonyBTS> adder() {
        return adder;
    }
}
