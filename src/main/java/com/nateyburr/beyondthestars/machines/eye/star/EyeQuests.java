package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.nateyburr.beyondthestars.QuestCredit;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * B-218: the Eye of Harmony completes its own milestone quests for its owner (and the owner's BetterQuesting party)
 * through {@link QuestCredit}. Which quests, their objectives and targets come from questbook/singularity.json (each
 * has one scoreboard task; the generator writes them into {@code BTSQuestIds.EYE_QUESTS} and checks that every
 * objective it names is one {@link #count} knows). The counts are the Eye's own saved state (item NBT milestones and
 * the unlocked tier), so nothing is queued: after a restart, a wrench or a reinstalled book the next credit finds the
 * same counts and completes whatever is missing.
 * <p>
 * The controller calls {@link #second} once per field second. A credit runs when a count changes or the owner does,
 * and at least every {@link #RECHECK_SECONDS} (a new party member, a quest an op reset, the book installed after the
 * fact); a credit of quests already complete only reads.
 */
public final class EyeQuests {

    private EyeQuests() {}

    /** The longest a quiet Eye goes between two credits, seconds. */
    public static final int RECHECK_SECONDS = 60;

    private static final QuestCredit.Goal[] GOALS = QuestCredit.eyeGoals();

    /** Per Eye: the counts and owner last credited, and the seconds until the next unprompted credit. */
    private static final Map<EyeStarRuntime, Watch> WATCH = new WeakHashMap<>();

    private static final class Watch {

        long[] counts;
        UUID owner;
        int wait;
    }

    /** One field second of the controller (server side). */
    public static void second(EyeStarRuntime rt, IGregTechTileEntity base) {
        if (GOALS.length == 0 || !QuestCredit.available()) return;
        final UUID owner = rt.host.owner();
        if (owner == null) return;
        final long[] counts = new long[GOALS.length];
        for (int i = 0; i < GOALS.length; i++) counts[i] = count(rt, GOALS[i].objective);
        Watch w = WATCH.get(rt);
        if (w == null) WATCH.put(rt, w = new Watch());
        if (w.counts != null && Arrays.equals(w.counts, counts) && owner.equals(w.owner) && --w.wait > 0) return;
        w.counts = counts;
        w.owner = owner;
        w.wait = RECHECK_SECONDS;
        QuestCredit.credit(owner, GOALS, counts, where(base));
    }

    /**
     * The Eye's count toward the quest whose scoreboard task reads {@code objective} (the quest completes at the task's
     * target). Unknown objectives count 0; the questbook generator rejects a task naming one.
     */
    static long count(EyeStarRuntime rt, String objective) {
        final EyeMilestones m = rt.milestones;
        switch (objective) {
            case "bts_stablink": // 48161 Stability Link (T6 -> T7)
                return m.linkDone[6] ? 1 : 0;
            case "bts_heavylink": // 48164 Heavy Link (T7 -> T8)
                return m.linkDone[7] ? 1 : 0;
            case "bts_deeplink": // 48201 The Deep Link (T8 -> T9)
                return m.linkDone[8] ? 1 : 0;
            case "bts_infall": // 48203 Out of the Zone of Avoidance: The Infall succeeded, T9 unlocked
                return rt.unlockedTier >= 9 ? 1 : 0;
            case "bts_eye_t9": // 48182 Eye T9: the Great Attractor lit and running (or the Eye already past it)
                return rt.unlockedTier >= 10 || runsT9(rt) ? 1 : 0;
            case "bts_flows": // 48204 Holding the Flow: Galactic Flows held in full
                return m.flowsHeld;
            case "bts_sd_min": // 48205 Twenty EeV: minutes of T9 on Superdense pellets
                return m.superdenseSeconds / 60;
            case "bts_galaxies": // 48206 Laniakea: galaxies held
                return m.galaxies;
            case "bts_wide3": // 48207 A Wider Pull: T9 cycles run with three companions
                return m.widePulls;
            case "bts_smlink": // 48198 Supermassive Link (T9 -> T10)
                return m.smLinkDone ? 1 : 0;
            case "bts_inflation": // 48208 Inflation succeeded, T10 unlocked
                return rt.unlockedTier >= 10 ? 1 : 0;
            case "bts_flat": // 48209 Keep It Flat: universes run to their last light at mean Y 1.0 or more
                return m.flatUniverses;
            case "bts_dawns": // 48210 Cosmic Dawn: dawns caught
                return m.dawns;
            case "bts_universes": // 48211 Last Light: universes run to their last light
                return m.universes;
            default:
                return 0;
        }
    }

    /** The Great Attractor burns on an unlocked T9, in galaxy mode, outside a transition. */
    private static boolean runsT9(EyeStarRuntime rt) {
        return rt.unlockedTier >= 9 && rt.burning()
            && rt.transition() == null
            && rt.fs != null
            && rt.fs.tier.attractor
            && !rt.fs.tier.planets();
    }

    private static String where(IGregTechTileEntity base) {
        if (base == null) return "the Eye of Harmony";
        return String.format(
            Locale.ROOT,
            "the Eye of Harmony at %d (%d, %d, %d)",
            base.getWorld() == null ? 0 : base.getWorld().provider.dimensionId,
            base.getXCoord(),
            base.getYCoord(),
            base.getZCoord());
    }
}
