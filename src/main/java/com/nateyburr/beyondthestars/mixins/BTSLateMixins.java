package com.nateyburr.beyondthestars.mixins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/**
 * CIRCUITS v2 §4.2 (design repo {@code designs/docs/circuits-v2.md}): the addon's only mixins, loaded LATE (after mod
 * discovery, the way GT and the coremod load theirs) because their targets are GT classes.
 * <ul>
 * <li>{@code nac.MixinCircuitComponentPacket}: a NAC hatch's saved contents with a component name the enum does not
 * know are dropped with a log line instead of crashing the hatch (§4.4).</li>
 * <li>{@code nac.CircuitComponentInvoker} + {@code nac.MixinCircuitComponent}: our 34 NAC components appended to GT's
 * {@code CircuitComponent} enum (§4.2, §4.3).</li>
 * </ul>
 * <b>No config switch</b> (revision 2): a client and a server must always agree on the enum, so the mixins apply
 * whenever GT is present; {@code nacCircuitComponents} gates only server-side recipe data ({@code BTSRecipesNac}).
 * The config file {@code mixins.beyondthestars.late.json} is {@code required: false} and the injector is
 * {@code require = 0}: any failure leaves GT's enum stock ({@code BTSNacComponents.active} stays false), never a crash.
 */
@LateMixin
public class BTSLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.beyondthestars.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (!loadedMods.contains("gregtech")) return Collections.emptyList();
        final List<String> mixins = new ArrayList<>();
        mixins.add("nac.MixinCircuitComponentPacket");
        mixins.add("nac.CircuitComponentInvoker");
        mixins.add("nac.MixinCircuitComponent");
        return mixins;
    }
}
