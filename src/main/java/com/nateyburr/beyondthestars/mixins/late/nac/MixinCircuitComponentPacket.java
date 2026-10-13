package com.nateyburr.beyondthestars.mixins.late.nac;

import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.nateyburr.beyondthestars.nac.BTSNacPacketGuard;

import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponentPacket;

/**
 * CIRCUITS v2 §4.4: a NAC hatch's saved contents are read back by component NAME
 * ({@code CircuitComponent.valueOf(key)} for every key of the tag), which throws on a name the enum does not know: a
 * degraded patch, a GT update that renamed a component, a world moved to a jar without ours. The key-set call of
 * {@code CircuitComponentPacket(NBTTagCompound)} is redirected to a copy without the unknown names, each logged once
 * with its amount ({@link BTSNacPacketGuard}); the hatch then loads with those components voided instead of crashing.
 * <p>
 * {@code func_150296_c} ({@code NBTTagCompound.getKeySet}) is the SRG name in both the dev and the obfuscated run (it
 * has no MCP name), and {@code net/minecraft/nbt/NBTTagCompound} is not renamed at run time, so nothing is remapped.
 */
@Mixin(value = CircuitComponentPacket.class, remap = false)
public abstract class MixinCircuitComponentPacket {

    @Redirect(
        method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V",
        require = 0,
        expect = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NBTTagCompound;func_150296_c()Ljava/util/Set;"))
    private Set<String> bts$knownKeys(NBTTagCompound nbt) {
        return BTSNacPacketGuard.knownKeys(nbt);
    }
}
