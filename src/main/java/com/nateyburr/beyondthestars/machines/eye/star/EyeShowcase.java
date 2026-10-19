package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * SHOWCASE MODE for the Eye (showcase/BTSShowcase), one field second before the Eye's own (from
 * {@link EyeGalaxyBench#liveSecond}). It is the galaxy bench made permanent and saved with the Eye:
 * <ul>
 * <li><b>The trims:</b> the debug field rig at FINE, so every tier's Field sockets are trimmed for free and the T9
 * beam's V loop is held (the rig stands in for the beam's wire).</li>
 * <li><b>The bursts (T7+):</b> a bench quasar disc through the standard conduit rig, as {@code /btseye star x y z
 * galaxy} and the standard rig run it, at a mass that holds the tier (64 SM at T7, 140 at T8, 300 at T9, 600 at
 * T10). The disc is not saved: after a restart it spins up again.</li>
 * <li><b>The stock:</b> the input buffers filled when Status bit 1 says they are short, the Pellet Injector kept
 * stocked, and the owner's wireless network kept at 10^21 EU or more.</li>
 * <li><b>A dark star relights</b> at its chart's tier straight away (no charge, Merger or Infall).</li>
 * </ul>
 * The flag lives in {@link EyeStarRuntime#showcase} (tile NBT, {@link EyeNbt#writeStar}).
 */
public final class EyeShowcase {

    private EyeShowcase() {}

    private static final BigInteger EU_FLOOR = BigInteger.TEN.pow(21);
    private static final BigInteger EU_TOP = BigInteger.TEN.pow(22);

    /** The bench quasar mass that holds a galaxy tier, SM (the reference world's settings). */
    public static double discMass(int tier) {
        if (tier >= 10) return 600;
        if (tier == 9) return 300;
        if (tier == 8) return 140;
        return 64;
    }

    private static final class Disc {

        final int tier;
        final EyeGalaxyBench.Rig rig;

        Disc(int tier) {
            this.tier = tier;
            rig = new EyeGalaxyBench.Rig(discMass(tier), 1, false);
            rig.mk = 2;
        }
    }

    /** The discs, by Eye position (not saved). */
    private static final Map<Integer, Disc> DISCS = new HashMap<>();

    static void second(MTEEyeOfHarmonyBTS eye) {
        final EyeStarRuntime rt = eye.starRuntime();
        final int k = key(eye);
        if (!rt.showcase) {
            DISCS.remove(k);
            return;
        }
        if (rt.rig == null) rt.setRig(FieldRig.Mode.FINE);
        final StarDef want = rt.wanted();
        if (!rt.live() && want != null && want.n() <= rt.host.installedTier()) rt.debugLight(want);
        final StarDef star = rt.star();
        final int tier = star == null ? 0 : star.n();
        if (tier >= 7) {
            Disc d = DISCS.get(k);
            if (d == null || d.tier != tier) {
                d = new Disc(tier);
                DISCS.put(k, d);
            }
            EyeGalaxyBench.second(eye, rt, d.rig, rt.universeWaiting());
        } else {
            DISCS.remove(k);
        }
        EyeStdRig.stockPellets(eye);
        if (rt.buffersShort()) eye.debugFillBuffers(1_000_000_000_000L);
        final UUID owner = rt.host.owner();
        if (owner != null && WirelessNetworkManager.getUserEU(owner)
            .compareTo(EU_FLOOR) < 0) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
            WirelessNetworkManager.addEUToGlobalEnergyMap(owner, EU_TOP);
        }
    }

    private static int key(MTEEyeOfHarmonyBTS eye) {
        final IGregTechTileEntity b = eye.getBaseMetaTileEntity();
        return b == null ? 0 : (b.getXCoord() * 31 + b.getYCoord()) * 31 + b.getZCoord();
    }
}
