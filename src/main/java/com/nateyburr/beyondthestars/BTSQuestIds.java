// GENERATED FILE - DO NOT EDIT.
//
// Rendered by tools/gen-singularity.js out of questbook/singularity.json (and, for the plant
// chapter, out of the questbook/DefaultQuests files gen-questbook.ps1 writes). Every quest line and
// quest id Beyond the Stars owns, so /btsquests counts one declared list and no hand-kept copy.
// Regenerate with: powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1
package com.nateyburr.beyondthestars;

/** Every questbook id Beyond the Stars owns (questIDHigh {@link #ID_HIGH}), per chapter. */
public final class BTSQuestIds {

    private BTSQuestIds() {}

    public static final long ID_HIGH = 190L;

    public static final long LINE_PLANT = 47990L;
    public static final long LINE_UXV = 47991L;
    public static final long LINE_MAX = 47992L;
    public static final long LINE_END = 47993L;

    /** The plant chapter's entry quest, grafted into the pack's "How to Generate Power". */
    public static final long ENTRY_QUEST = 48020L;

    /** The plant chapter (line 47990). */
    public static final long[] PLANT_QUESTS = { 48000L, 48001L, 48002L, 48003L, 48004L, 48005L, 48006L, 48007L, 48008L,
        48009L, 48010L, 48011L, 48012L, 48013L, 48014L, 48015L, 48016L, 48017L, 48018L, 48019L, 48021L, 48022L, 48023L,
        48024L, 48025L, 48026L, 48027L, 48028L, 48029L, 48030L, 48031L, 48032L, 48033L, 48034L };

    /** Ours, grafted into the pack's Tier 11 - UIV. */
    public static final long[] UIV_QUESTS = { 48100L, 48101L, 48102L };

    /** Ours, grafted into the pack's Tier 12 - UMV. */
    public static final long[] UMV_QUESTS = { 48110L, 48111L, 48112L, 48113L, 48114L, 48115L, 48116L, 48117L, 48118L,
        48119L, 48120L, 48121L, 48122L, 48123L, 48124L };

    /** Chapter Tier 13 - UXV (line 47991). */
    public static final long[] UXV_QUESTS = { 48140L, 48141L, 48142L, 48143L, 48144L, 48145L, 48146L, 48147L, 48148L,
        48149L, 48150L, 48151L, 48152L, 48153L, 48154L, 48155L, 48156L, 48157L, 48158L, 48159L, 48160L, 48161L, 48162L,
        48163L, 48164L, 48165L, 48166L, 48167L, 48168L, 48169L, 48170L, 48171L, 48172L, 48173L, 48174L, 48175L, 48176L,
        48177L, 48178L, 48179L };

    /** Chapter Tier 14 - MAX (line 47992). */
    public static final long[] MAX_QUESTS = { 48180L, 48181L, 48182L, 48183L, 48184L, 48185L, 48186L, 48187L, 48188L,
        48189L, 48190L, 48191L, 48192L, 48193L, 48194L, 48195L, 48196L, 48197L, 48198L, 48199L, 48200L, 48201L, 48202L,
        48203L, 48204L, 48205L, 48206L, 48207L, 48208L, 48209L, 48210L, 48211L };

    /** Chapter The Endgame (line 47993). */
    public static final long[] END_QUESTS = { 48220L, 48221L, 48222L, 48223L, 48224L, 48225L, 48226L, 48227L, 48228L };

    /** Shown only while their pack edit is NOT applied (pack edits off, or drifted). */
    public static final long[] FALLBACK_QUESTS = { 48103L, 48109L };

    /** Checkbox placeholders in this build: their content is not in the mod yet. */
    public static final long[] PLACEHOLDER_QUESTS = {};

    /** Quests the Eye of Harmony completes itself (B-218, QuestCredit): one scoreboard task each. */
    public static final long[] EYE_QUESTS = { 48161L, 48164L, 48182L, 48198L, 48201L, 48203L, 48204L, 48205L, 48206L,
        48207L, 48208L, 48209L, 48210L, 48211L };

    /** The scoreboard objective each EYE_QUESTS task reads (EyeQuests.count writes it). */
    public static final String[] EYE_QUEST_SCORES = { "bts_stablink", "bts_heavylink", "bts_eye_t9", "bts_smlink",
        "bts_deeplink", "bts_infall", "bts_flows", "bts_sd_min", "bts_galaxies", "bts_wide3", "bts_inflation",
        "bts_flat", "bts_dawns", "bts_universes" };

    /** The count at which each EYE_QUESTS quest completes. */
    public static final long[] EYE_QUEST_TARGETS = { 1L, 1L, 1L, 1L, 1L, 1L, 1L, 60L, 1000L, 1L, 1L, 1L, 1L, 10L };

    /** Every pack edit in the bundle, in application order. */
    public static final String[] PACK_EDITS = { "E01", "E02", "E03", "E04", "E05", "E06", "E07", "E10", "E11", "E12",
        "E13", "E14", "E15", "E16", "E17", "E18", "E19", "E20", "E21", "E22", "E23", "E24", "E25", "E26", "E27", "E28",
        "E29", "E30", "E31", "E32", "E33", "E34", "E35", "E36", "E37", "E38", "E39", "E40", "E41", "E42", "E43", "E44",
        "E45", "E46", "E47", "E48", "E49", "E50", "E51", "E52", "E53", "E54", "E55", "E56", "E57", "E58", "E59", "E60",
        "E61", "E62", "E63", "E64", "E65", "E66", "E67", "E68" };
}
