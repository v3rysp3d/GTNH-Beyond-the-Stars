package com.nateyburr.beyondthestars.nac;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;

/**
 * CIRCUITS v2 §4.4: the safety half of the NAC patch, called by {@code MixinCircuitComponentPacket} from GT's
 * {@code CircuitComponentPacket(NBTTagCompound)}. GT reads a Vacuum Conveyor hatch's saved contents by component name
 * and {@code CircuitComponent.valueOf} throws on a name the enum does not know; this hands GT the tag's keys without
 * those names, so the hatch loads and the unknown components are voided. Each dropped name is logged once per run
 * with the amount it carried. Removing the addon itself cannot be covered this way (its mixins go with it):
 * {@code docs/migration.md} says to empty the conveyors of {@code BTS_} components first.
 */
public final class BTSNacPacketGuard {

    private BTSNacPacketGuard() {}

    private static final Logger LOG = LogManager.getLogger("beyondthestars");

    /** Names already logged this run (each unknown name is logged once). */
    private static final Set<String> LOGGED = Collections.synchronizedSet(new HashSet<>());

    /** How many unknown entries were dropped this run (for the checks). */
    private static volatile int dropped;

    /**
     * The keys of {@code nbt} that name a component the enum knows. Returns the tag's own key set when every name is
     * known (the common case: no copy).
     */
    @SuppressWarnings("unchecked")
    public static Set<String> knownKeys(NBTTagCompound nbt) {
        final Set<String> keys = nbt.func_150296_c();
        Set<String> known = null;
        for (String key : keys) {
            if (isKnown(key)) continue;
            if (known == null) {
                known = new LinkedHashSet<>();
                for (String k : keys) if (isKnown(k)) known.add(k);
            }
            dropped++;
            if (LOGGED.add(key)) {
                LOG.warn(
                    "[BTS] NAC: a saved Vacuum Conveyor hatch held {} x \"{}\", a circuit component this game does not"
                        + " know (a disabled NAC patch, a renamed GT component or a world from another jar). It was"
                        + " voided so the hatch could load. Logged once per name.",
                    amount(nbt, key),
                    key);
            }
        }
        return known == null ? keys : known;
    }

    /** True when {@code CircuitComponent.valueOf(name)} resolves. */
    public static boolean isKnown(String name) {
        if (name == null) return false;
        try {
            CircuitComponent.valueOf(name);
            return true;
        } catch (IllegalArgumentException unknown) {
            return false;
        }
    }

    /** Unknown entries dropped so far this run. */
    public static int droppedCount() {
        return dropped;
    }

    /** The amount an entry carried: the legacy long, or the unnamed plus every named amount. */
    private static long amount(NBTTagCompound nbt, String key) {
        final NBTBase tag = nbt.getTag(key);
        if (tag == null) return 0;
        if (tag.getId() == Constants.NBT.TAG_LONG) return nbt.getLong(key);
        if (tag.getId() != Constants.NBT.TAG_COMPOUND) return 0;
        final NBTTagCompound sub = (NBTTagCompound) tag;
        long total = sub.hasKey("Unnamed", Constants.NBT.TAG_LONG) ? sub.getLong("Unnamed") : 0;
        if (sub.hasKey("NamedList", Constants.NBT.TAG_LIST)) {
            final NBTTagList list = sub.getTagList("NamedList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) total += list.getCompoundTagAt(i)
                .getLong("Amount");
        }
        return total;
    }
}
