package com.nateyburr.beyondthestars.mixins.late.nac;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitCalibration;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;

/**
 * CIRCUITS v2 §4.2: static constructor invokers for GT's {@link CircuitComponent} enum. javac forbids {@code new} on an
 * enum, so Mixin generates {@code new CircuitComponent(...)} inside the enum itself, where the private constructor is
 * legal. The first two parameters are the enum's hidden name and ordinal; the rest match GT's constructors (javap on
 * GT 5.09.54.205, both the Java 8 and the {@code META-INF/versions/17} class):
 * <ul>
 * <li>CC {@code (Ljava/lang/String;IILjava/lang/String;Ljava/util/function/Supplier;)V}</li>
 * <li>PC {@code (Ljava/lang/String;IILjava/lang/String;Ljava/util/function/Supplier;Z)V}</li>
 * <li>circuit
 * {@code (Ljava/lang/String;IILjava/lang/String;Ljava/util/function/Supplier;L.../CircuitCalibration;F)V}</li>
 * </ul>
 * If GT changes a constructor, this mixin is skipped (the config is {@code required: false}) and the bodies below run:
 * they throw, and {@code BTSNacComponents.extend} catches that and leaves the enum stock.
 */
@Mixin(value = CircuitComponent.class, remap = false)
public interface CircuitComponentInvoker {

    @Invoker("<init>")
    static CircuitComponent bts$cc(String name, int ordinal, int id, String nameKey, Supplier<ItemStack> real) {
        throw new AssertionError("CircuitComponent CC invoker not applied");
    }

    @Invoker("<init>")
    static CircuitComponent bts$pc(String name, int ordinal, int id, String nameKey, Supplier<CircuitComponent> cc,
        boolean processed) {
        throw new AssertionError("CircuitComponent PC invoker not applied");
    }

    @Invoker("<init>")
    static CircuitComponent bts$circuit(String name, int ordinal, int id, String nameKey, Supplier<ItemStack> real,
        CircuitCalibration calibration, float weight) {
        throw new AssertionError("CircuitComponent circuit invoker not applied");
    }
}
