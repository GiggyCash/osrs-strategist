package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Local re-verification index. Runtime planning uses only the structured facts
 * derived from these sources and never fetches a page.
 */
@Singleton
public final class StrategySourceRegistry
{
    public static final String WIKI_LICENSE = "CC BY-NC-SA 3.0";
    private static final LocalDate REVIEWED = LocalDate.of(2026, 8, 30);
    private final Map<StrategySourceId, StrategySourceDefinition> sources;

    public StrategySourceRegistry()
    {
        EnumMap<StrategySourceId, StrategySourceDefinition> values =
                new EnumMap<>(StrategySourceId.class);
        wiki(values, StrategySourceId.GENERAL_SKILL_TRAINING,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "General, Main, Ironman, and UIM training-guide index");
        wiki(values, StrategySourceId.F2P_SKILL_TRAINING,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "Free-to-play training families");
        wiki(values, StrategySourceId.IRONMAN_GENERAL,
                "https://oldschool.runescape.wiki/w/Ironman_guide",
                "Ironman progression and self-sufficient strategy");
        wiki(values, StrategySourceId.F2P_IRONMAN_GENERAL,
                "https://oldschool.runescape.wiki/w/Free-to-play_Ironman_guide",
                "F2P Ironman, Hardcore, Group, and UIM progression");
        wiki(values, StrategySourceId.UIM_GENERAL,
                "https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide",
                "Ultimate Ironman progression and setup strategy");
        wiki(values, StrategySourceId.UIM_ITEM_MANAGEMENT,
                "https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management",
                "UIM inventory, storage, retrieval, and risk mechanics");
        wiki(values, StrategySourceId.IRONMAN_SKILL_GUIDES,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "Account-specific Ironman skill methods");
        wiki(values, StrategySourceId.UIM_SKILL_GUIDES,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "Account-specific UIM skill methods and footprints");
        wiki(values, StrategySourceId.OPTIMAL_QUEST_GUIDE,
                "https://oldschool.runescape.wiki/w/Optimal_quest_guide",
                "Quest ordering and early experience compression");
        wiki(values, StrategySourceId.SLAYER_TRAINING,
                "https://oldschool.runescape.wiki/w/Slayer_training",
                "Slayer task, master, skip, block, and extension strategy");
        wiki(values, StrategySourceId.IRONMAN_SLAYER,
                "https://oldschool.runescape.wiki/w/Ironman_Guide/Slayer",
                "Ironman Slayer resources and task economics");
        wiki(values, StrategySourceId.CLUE_STASH,
                "https://oldschool.runescape.wiki/w/STASH",
                "Clue equipment storage mechanics");
        wiki(values, StrategySourceId.POH_STORAGE,
                "https://oldschool.runescape.wiki/w/Costume_room",
                "POH equipment-storage capabilities");
        wiki(values, StrategySourceId.MINIGAME_GUIDES,
                "https://oldschool.runescape.wiki/w/Minigames",
                "Minigame requirements and progression rewards");
        wiki(values, StrategySourceId.PVM_STRATEGY,
                "https://oldschool.runescape.wiki/w/Guide:Bossing_Ladder",
                "Encounter preparation and useful PvM unlocks");
        values.put(StrategySourceId.RUNELITE_MECHANICS,
                new StrategySourceDefinition(
                        StrategySourceId.RUNELITE_MECHANICS,
                        "https://github.com/runelite/runelite",
                        "RuneLite-maintained live and mechanical evidence",
                        REVIEWED, "RuneLite 1.12.35", "BSD-2-Clause",
                        Arrays.asList("live-state", "mechanical-evidence",
                                "stash-identities", "quest-identities")));
        sources = Collections.unmodifiableMap(values);
    }

    public StrategySourceDefinition get(StrategySourceId id)
    {
        return sources.get(id);
    }

    public Map<StrategySourceId, StrategySourceDefinition> all()
    {
        return sources;
    }

    private static void wiki(Map<StrategySourceId, StrategySourceDefinition> values,
            StrategySourceId id, String url, String subject)
    {
        values.put(id, new StrategySourceDefinition(id, url, subject, REVIEWED,
                revisionFor(id), WIKI_LICENSE, familiesFor(id)));
    }

    private static String revisionFor(StrategySourceId id)
    {
        switch (id)
        {
            case GENERAL_SKILL_TRAINING:
            case F2P_SKILL_TRAINING:
            case IRONMAN_SKILL_GUIDES:
            case UIM_SKILL_GUIDES:
                return "oldid=15231153 (2026-06-10T08:54:42Z)";
            case IRONMAN_GENERAL:
                return "oldid=15316742 (2026-08-22T21:08:30Z)";
            case F2P_IRONMAN_GENERAL:
                return "oldid=15315034 (2026-08-21T03:38:38Z)";
            case UIM_GENERAL:
                return "oldid=15215498 (2026-05-23T07:11:07Z)";
            case UIM_ITEM_MANAGEMENT:
                return "oldid=15321539 (2026-08-26T21:44:21Z)";
            case OPTIMAL_QUEST_GUIDE:
                return "oldid=15319535 (2026-08-25T06:47:14Z)";
            case SLAYER_TRAINING:
                return "oldid=15319310 (2026-08-25T05:21:09Z)";
            case IRONMAN_SLAYER:
                return "oldid=15319945 (2026-08-25T18:42:46Z)";
            case CLUE_STASH:
                return "oldid=15319948 (2026-08-25T18:56:28Z)";
            case POH_STORAGE:
                return "oldid=15035574 (2025-11-19T01:31:46Z)";
            case MINIGAME_GUIDES:
                return "oldid=15304433 (2026-08-17T17:39:24Z)";
            case PVM_STRATEGY:
                return "oldid=15319480 (2026-08-25T06:16:24Z)";
            default:
                throw new IllegalArgumentException("Unpinned Wiki source " + id);
        }
    }

    private static java.util.List<String> familiesFor(StrategySourceId id)
    {
        switch (id)
        {
            case GENERAL_SKILL_TRAINING:
            case F2P_SKILL_TRAINING:
                return Arrays.asList("shared-skilling", "main-skilling",
                        "f2p-skilling", "method-transitions");
            case IRONMAN_GENERAL:
            case F2P_IRONMAN_GENERAL:
                return Arrays.asList("iron-progression",
                        "self-source-pipelines", "quest-compression");
            case UIM_GENERAL:
                return Arrays.asList("uim-progression", "setup-reuse",
                        "infrastructure-value");
            case UIM_ITEM_MANAGEMENT:
                return Arrays.asList("uim-inventory-footprints",
                        "uim-storage", "uim-dangerous-storage");
            case IRONMAN_SKILL_GUIDES:
                return Arrays.asList("iron-skilling",
                        "iron-resource-pipelines");
            case UIM_SKILL_GUIDES:
                return Arrays.asList("uim-skilling",
                        "uim-method-footprints");
            case OPTIMAL_QUEST_GUIDE:
                return Arrays.asList("quest-ordering", "quest-xp-value");
            case SLAYER_TRAINING:
            case IRONMAN_SLAYER:
                return Arrays.asList("slayer-tasks", "slayer-points",
                        "slayer-resource-value");
            case CLUE_STASH:
                return Arrays.asList("clues", "stash", "uim-clue-storage");
            case POH_STORAGE:
                return Arrays.asList("poh", "costume-storage",
                        "uim-infrastructure");
            case MINIGAME_GUIDES:
                return Arrays.asList("minigames", "minigame-inventory");
            case PVM_STRATEGY:
                return Arrays.asList("pvm-readiness", "gear-progression",
                        "hardcore-risk");
            default:
                return Collections.emptyList();
        }
    }
}
