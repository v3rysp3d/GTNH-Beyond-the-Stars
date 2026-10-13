package com.nateyburr.beyondthestars.mixins.late.nac;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nateyburr.beyondthestars.nac.BTSNacComponents;

import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;

/**
 * CIRCUITS v2 §4.2: appends our 34 NAC components to GT's {@link CircuitComponent} enum. The handler runs inside the
 * enum's static initialiser right after {@code $VALUES} is stored and BEFORE GT's own {@code VALUES = values()} and
 * {@code META_IDS} loop (bytecode 6639 &lt; 6713 &lt; 6723 in both class versions), so every consumer of the enum (the
 * fake item, NEI, {@code META_IDS}, the coremod's conversion loop, NBT {@code valueOf}) sees ours with no further
 * patch.
 * <p>
 * <b>Degrade, never crash</b> (revision 2): {@code require = 0} (a missed injection point is not fatal; the handler
 * then never runs and {@code BTSNacComponents.active} stays false), and {@link BTSNacComponents#extend} catches every
 * {@code Throwable} and returns the stock array.
 */
@Mixin(value = CircuitComponent.class, remap = false)
public abstract class MixinCircuitComponent {

    @Shadow
    @Final
    @Mutable
    private static CircuitComponent[] $VALUES;

    @Inject(
        method = "<clinit>",
        require = 0,
        expect = 1,
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTSTATIC,
            shift = At.Shift.AFTER,
            target = "Lgregtech/common/tileentities/machines/multi/nanochip/util/CircuitComponent;$VALUES:[Lgregtech/common/tileentities/machines/multi/nanochip/util/CircuitComponent;"))
    private static void bts$extend(CallbackInfo ci) {
        $VALUES = BTSNacComponents.extend($VALUES);
    }
}
