package com.nateyburr.beyondthestars;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

/**
 * B-218: a machine completes its own milestone quests in BetterQuesting (soft dependency, reflection only, through
 * {@link BQBridge}). The Eye of Harmony is the one user ({@code EyeQuests}).
 * <p>
 * <b>The task.</b> Each such quest has one {@code bq_standard:scoreboard} task on a dummy objective of ours
 * ({@code bts_...}, listed in {@link BTSQuestIds#EYE_QUESTS}): nobody can tick it by hand, and its panel shows the
 * machine's count against the target ("412 >= 1000 Galaxies held").
 * <p>
 * <b>A credit.</b> For the owner and every member of the owner's BetterQuesting party:
 * <ol>
 * <li>the count goes into the objective, raised and never lowered, twice: the world scoreboard (by the player's name,
 * when one is known: BetterQuesting's own task reads it there while the player is online) and bq_standard's per-UUID
 * copy the panel shows (sent to the player if online);</li>
 * <li>a quest whose count has reached its target and that this player has not completed is completed through
 * BetterQuesting's own admin complete ({@link BQBridge#completeFor}): the quest and its task for that UUID, online or
 * not, the database marked for saving, the quest sync sent if the player is online. Prerequisites are not waited for,
 * as with {@code /bq_admin complete}: the machine's record is the proof, and a locked prerequisite must not hold back
 * the chain behind it.</li>
 * </ol>
 * Completing is idempotent (a complete quest is skipped: calling BetterQuesting's complete on it would reset its
 * claim), so a caller may credit as often as it likes. Nothing is queued: the machine's own saved state is the record,
 * and the caller credits again after every load. Without BetterQuesting, or without the quest in this world's book,
 * nothing happens; the first reflection failure is logged once and turns the bridge off until the next start.
 */
public final class QuestCredit {

    private QuestCredit() {}

    /** One quest a machine completes: its questIDLow, the scoreboard objective its task reads, the task's target. */
    public static final class Goal {

        public final long low;
        public final String objective;
        public final long target;
        final UUID id;

        public Goal(long low, String objective, long target) {
            this.low = low;
            this.objective = objective;
            this.target = Math.max(1, target);
            this.id = new UUID(BQBridge.ID_HIGH, low);
        }
    }

    /** The quests the Eye completes, from the generated {@link BTSQuestIds} tables. */
    public static Goal[] eyeGoals() {
        final int n = Math.min(
            BTSQuestIds.EYE_QUESTS.length,
            Math.min(BTSQuestIds.EYE_QUEST_SCORES.length, BTSQuestIds.EYE_QUEST_TARGETS.length));
        final Goal[] out = new Goal[n];
        for (int i = 0; i < n; i++) {
            out[i] = new Goal(
                BTSQuestIds.EYE_QUESTS[i],
                BTSQuestIds.EYE_QUEST_SCORES[i],
                BTSQuestIds.EYE_QUEST_TARGETS[i]);
        }
        return out;
    }

    private static boolean broken;

    /** BetterQuesting is loaded and the bridge has not failed. */
    public static boolean available() {
        return !broken && BQBridge.isLoaded();
    }

    /**
     * Credits {@code owner} and their party with {@code counts[i]} toward {@code goals[i]} (see the class comment).
     * Server thread only. Returns how many completions were made (0 when everyone had them already).
     *
     * @param source who did it, for the log and the chat line ("the Eye of Harmony at 0 (12, 10, 4)")
     */
    public static int credit(UUID owner, Goal[] goals, long[] counts, String source) {
        if (owner == null || !available()) return 0;
        int made = 0;
        try {
            final Map<UUID, Object> db = BQBridge.quests();
            final MinecraftServer server = MinecraftServer.getServer();
            for (final UUID member : BQBridge.partyOf(owner)) {
                final EntityPlayerMP online = online(server, member);
                final String name = online != null ? online.getCommandSenderName() : BQBridge.cachedName(member);
                boolean shown = false;
                for (int i = 0; i < goals.length && i < counts.length; i++) {
                    final Goal g = goals[i];
                    final Object quest = db.get(g.id);
                    if (quest == null) continue; // not in this world's book
                    final int value = (int) Math.max(0, Math.min(Integer.MAX_VALUE, counts[i]));
                    shown |= raise(server, member, name, g.objective, value);
                    if (value < g.target || BQBridge.isComplete(quest, member)) continue;
                    BQBridge.completeFor(g.id, member);
                    made++;
                    final String title = plain(BQBridge.questName(quest));
                    BeyondTheStars.LOG.info(
                        "Questbook: " + source
                            + " completed "
                            + BQBridge.label(g.id)
                            + " "
                            + title
                            + " for "
                            + (name != null ? name + " (" + member + ")" : member)
                            + (member.equals(owner) ? "" : ", a party member of the owner"));
                    if (online != null) online.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.AQUA + "Quest complete: "
                                + EnumChatFormatting.RESET
                                + title
                                + EnumChatFormatting.GRAY
                                + " ("
                                + source
                                + ")"));
                }
                if (shown && online != null) BQBridge.sendScores(online);
            }
        } catch (final Throwable t) {
            broken = true;
            BeyondTheStars.LOG.warn(
                "Questbook: the quest bridge failed (" + BQBridge.describe(t)
                    + "); machines stop completing quests until the next start",
                t);
        }
        return made;
    }

    /**
     * One player's count on one objective, raised (never lowered) in bq_standard's per-UUID copy and, when the name is
     * known, in the overworld scoreboard. Returns true when the per-UUID copy changed (the player's panel needs it).
     */
    private static boolean raise(MinecraftServer server, UUID who, String name, String objective, int value)
        throws Exception {
        if (value <= 0) return false;
        boolean changed = false;
        if (BQBridge.bqScore(who, objective) < value) {
            BQBridge.setBqScore(who, objective, value);
            changed = true;
        }
        final WorldServer overworld = server == null ? null : server.worldServerForDimension(0);
        if (name != null && overworld != null) {
            final Scoreboard board = overworld.getScoreboard();
            ScoreObjective obj = board.getObjective(objective);
            if (obj == null) obj = board.addScoreObjective(objective, IScoreObjectiveCriteria.field_96641_b);
            final Score score = board.func_96529_a(name, obj);
            if (score.getScorePoints() < value) score.setScorePoints(value);
        }
        return changed;
    }

    /** A quest name without its formatting codes ({@code EnumChatFormatting}'s own stripper is client-only). */
    private static String plain(String text) {
        return text == null ? "" : text.replaceAll("§.", "");
    }

    private static EntityPlayerMP online(MinecraftServer server, UUID who) {
        if (server == null || server.getConfigurationManager() == null) return null;
        for (final Object o : server.getConfigurationManager().playerEntityList) {
            if (o instanceof EntityPlayerMP && who.equals(
                ((EntityPlayerMP) o).getGameProfile()
                    .getId()))
                return (EntityPlayerMP) o;
        }
        return null;
    }

    /** Status lines for {@code /btsquests eye}: each Eye quest for one player, complete or not, with its count. */
    public static void describe(UUID player, List<String> out) {
        try {
            final Map<UUID, Object> db = BQBridge.quests();
            for (final Goal g : eyeGoals()) {
                final Object quest = db.get(g.id);
                if (quest == null) {
                    out.add(BQBridge.label(g.id) + " (" + g.objective + "): not in this world's book");
                    continue;
                }
                out.add(
                    (BQBridge.isComplete(quest, player) ? "[complete] " : "[open] ") + BQBridge.label(g.id)
                        + " "
                        + plain(BQBridge.questName(quest))
                        + ": "
                        + g.objective
                        + " "
                        + BQBridge.bqScore(player, g.objective)
                        + " / "
                        + g.target);
            }
        } catch (final Throwable t) {
            out.add("could not read the quest book: " + BQBridge.describe(t));
        }
    }
}
