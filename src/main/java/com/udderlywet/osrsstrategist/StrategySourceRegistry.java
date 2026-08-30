package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
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
                "https://oldschool.runescape.wiki/w/PvM_unlock_guide",
                "Encounter preparation and useful PvM unlocks");
        values.put(StrategySourceId.RUNELITE_MECHANICS,
                new StrategySourceDefinition(
                        StrategySourceId.RUNELITE_MECHANICS,
                        "https://github.com/runelite/runelite",
                        "RuneLite-maintained live and mechanical evidence",
                        REVIEWED, "RuneLite 1.12.35", "BSD-2-Clause"));
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
                "reviewed-local-paraphrase-2026-08-30", WIKI_LICENSE));
    }
}
