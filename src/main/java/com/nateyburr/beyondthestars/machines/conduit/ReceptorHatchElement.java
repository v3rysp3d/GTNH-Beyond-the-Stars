package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * P4 (quasar-p4.md §3.1): a private GRB Receptor Port socket for a burst host that has no hatch-element enum of its
 * own (the Homogeny's stirring ring, Reality Diffraction's portal). The port joins the host's own list through
 * {@code adder}, never a GT list (trap 3).
 */
public final class ReceptorHatchElement<T> implements IHatchElement<T> {

    private final IGTHatchAdder<? super T> adder;
    private final ToLongFunction<T> counter;

    private ReceptorHatchElement(IGTHatchAdder<? super T> adder, ToLongFunction<T> counter) {
        this.adder = adder;
        this.counter = counter;
    }

    /** A receptor socket that adds through {@code adder} and counts with {@code counter}. */
    public static <T> ReceptorHatchElement<T> of(IGTHatchAdder<? super T> adder, ToLongFunction<T> counter) {
        return new ReceptorHatchElement<>(adder, counter);
    }

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return Collections.singletonList(MTEHatchGRBReceptor.class);
    }

    @Override
    public IGTHatchAdder<? super T> adder() {
        return adder;
    }

    @Override
    public String name() {
        return "GRB Receptor Port";
    }

    @Override
    public long count(T t) {
        return counter.applyAsLong(t);
    }
}
