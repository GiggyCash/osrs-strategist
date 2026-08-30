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
        wiki(values, StrategySourceId.ITEM_RETRIEVAL_SERVICES,
                "https://oldschool.runescape.wiki/w/Item_Retrieval_Service",
                "Exact Item Retrieval Service location, fee, and second-death mechanics");
        wiki(values, StrategySourceId.IRONMAN_SKILL_GUIDES,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "Account-specific Ironman skill methods");
        wiki(values, StrategySourceId.UIM_SKILL_GUIDES,
                "https://oldschool.runescape.wiki/w/Skill_training_guides",
                "Account-specific UIM skill methods and footprints");
        accountSkillSources(values);
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
        wiki(values, StrategySourceId.WINTERTODT,
                "https://oldschool.runescape.wiki/w/Wintertodt",
                "Wintertodt requirements, warmth, tools, and execution");
        wiki(values, StrategySourceId.GIANTS_FOUNDRY,
                "https://oldschool.runescape.wiki/w/Giants%27_Foundry",
                "Giants' Foundry commissions, materials, moulds, and execution");
        wiki(values, StrategySourceId.MAHOGANY_HOMES,
                "https://oldschool.runescape.wiki/w/Mahogany_Homes",
                "Mahogany Homes contracts, tier materials, travel, and rewards");
        wiki(values, StrategySourceId.TITHE_FARM,
                "https://oldschool.runescape.wiki/w/Tithe_Farm",
                "Tithe Farm tools, level-tier seeds, inventory, and rewards");
        wiki(values, StrategySourceId.SAILING_TRAINING,
                "https://oldschool.runescape.wiki/w/Sailing_training",
                "Current Sailing methods, transitions, facilities, and boat progression");
        wiki(values, StrategySourceId.SHIPWRECK_SALVAGING,
                "https://oldschool.runescape.wiki/w/Shipwreck_salvaging",
                "Shipwreck levels, boat hazards, salvage flow, and inventory behavior");
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
            case IRONMAN_SMITHING: return "oldid=15177119 (2026-04-15T12:20:44Z)";
            case UIM_SMITHING: return "oldid=15288833 (2026-08-06T08:39:11Z)";
            case IRONMAN_CRAFTING: return "oldid=15316213 (2026-08-22T05:11:35Z)";
            case UIM_CRAFTING: return "oldid=15305169 (2026-08-18T12:54:21Z)";
            case IRONMAN_HERBLORE: return "oldid=15312412 (2026-08-20T02:31:31Z)";
            case UIM_HERBLORE: return "oldid=15319540 (2026-08-25T06:48:22Z)";
            case IRONMAN_CONSTRUCTION: return "oldid=15280023 (2026-07-29T12:57:18Z)";
            case UIM_CONSTRUCTION: return "oldid=15319378 (2026-08-25T05:44:38Z)";
            case IRONMAN_RUNECRAFT: return "oldid=15317928 (2026-08-24T07:51:27Z)";
            case UIM_RUNECRAFT: return "oldid=15322506 (2026-08-27T22:10:28Z)";
            case IRONMAN_PRAYER: return "oldid=15322576 (2026-08-28T02:05:38Z)";
            case UIM_PRAYER: return "oldid=15316277 (2026-08-22T05:31:45Z)";
            case IRONMAN_FARMING: return "oldid=15321988 (2026-08-27T11:25:17Z)";
            case UIM_FARMING: return "oldid=15315479 (2026-08-21T06:53:29Z)";
            case IRONMAN_COOKING: return "oldid=15296981 (2026-08-13T11:55:26Z)";
            case UIM_COOKING: return "oldid=15217261 (2026-05-26T07:43:36Z)";
            case IRONMAN_FLETCHING: return "oldid=15301621 (2026-08-14T23:19:41Z)";
            case UIM_FLETCHING: return "oldid=14972436 (2025-08-23T04:06:41Z)";
            case IRONMAN_FISHING: return "oldid=15322232 (2026-08-27T18:32:27Z)";
            case UIM_FISHING: return "oldid=15318352 (2026-08-25T00:25:11Z)";
            case IRONMAN_MINING: return "oldid=15292446 (2026-08-11T01:48:19Z)";
            case UIM_MINING: return "oldid=15276637 (2026-07-27T14:03:35Z)";
            case IRONMAN_WOODCUTTING: return "oldid=15257822 (2026-07-08T18:21:33Z)";
            case UIM_WOODCUTTING: return "oldid=15232438 (2026-06-12T12:56:55Z)";
            case IRONMAN_HUNTER: return "oldid=15287846 (2026-08-05T16:13:51Z)";
            case UIM_HUNTER: return "oldid=15154715 (2026-03-24T01:26:43Z)";
            case IRONMAN_FIREMAKING: return "oldid=15316626 (2026-08-22T17:35:56Z)";
            case UIM_FIREMAKING: return "oldid=15122261 (2026-02-06T08:15:26Z)";
            case IRONMAN_THIEVING: return "oldid=15267141 (2026-07-18T23:34:41Z)";
            case UIM_THIEVING: return "oldid=15316625 (2026-08-22T17:35:40Z)";
            case IRONMAN_GENERAL:
                return "oldid=15316742 (2026-08-22T21:08:30Z)";
            case F2P_IRONMAN_GENERAL:
                return "oldid=15315034 (2026-08-21T03:38:38Z)";
            case UIM_GENERAL:
                return "oldid=15215498 (2026-05-23T07:11:07Z)";
            case UIM_ITEM_MANAGEMENT:
                return "oldid=15321539 (2026-08-26T21:44:21Z)";
            case ITEM_RETRIEVAL_SERVICES:
                return "oldid=15124878 (2026-02-10T18:40:49Z)";
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
            case WINTERTODT:
                return "oldid=15317763 (2026-08-24T04:51:43Z)";
            case GIANTS_FOUNDRY:
                return "oldid=15307204 (2026-08-19T16:12:34Z)";
            case MAHOGANY_HOMES:
                return "oldid=15318320 (2026-08-24T23:36:19Z)";
            case TITHE_FARM:
                return "oldid=15299958 (2026-08-14T04:17:57Z)";
            case SAILING_TRAINING:
                return "oldid=15323443 (2026-08-28T19:16:42Z)";
            case SHIPWRECK_SALVAGING:
                return "oldid=15323162 (2026-08-28T14:49:43Z)";
            case PVM_STRATEGY:
                return "oldid=15319480 (2026-08-25T06:16:24Z)";
            default:
                throw new IllegalArgumentException("Unpinned Wiki source " + id);
        }
    }

    private static java.util.List<String> familiesFor(StrategySourceId id)
    {
        if (id.name().startsWith("IRONMAN_")
                && id != StrategySourceId.IRONMAN_GENERAL
                && id != StrategySourceId.IRONMAN_SKILL_GUIDES
                && id != StrategySourceId.IRONMAN_SLAYER)
            return Arrays.asList("iron-skilling",
                    "iron-" + id.name().substring("IRONMAN_".length())
                            .toLowerCase());
        if (id.name().startsWith("UIM_")
                && id != StrategySourceId.UIM_GENERAL
                && id != StrategySourceId.UIM_ITEM_MANAGEMENT
                && id != StrategySourceId.UIM_SKILL_GUIDES)
            return Arrays.asList("uim-skilling",
                    "uim-" + id.name().substring("UIM_".length())
                            .toLowerCase(),
                    "uim-method-footprints");
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
            case ITEM_RETRIEVAL_SERVICES:
                return Arrays.asList("uim-dangerous-storage",
                        "item-retrieval-fees", "second-death-rules");
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
            case WINTERTODT:
                return Arrays.asList("minigames", "wintertodt",
                        "minigame-inventory", "hardcore-risk");
            case GIANTS_FOUNDRY:
                return Arrays.asList("minigames", "giants-foundry",
                        "smithing-resources", "minigame-inventory");
            case MAHOGANY_HOMES:
                return Arrays.asList("minigames", "mahogany-homes",
                        "construction-resources", "minigame-inventory");
            case TITHE_FARM:
                return Arrays.asList("minigames", "tithe-farm",
                        "farming-resources", "minigame-inventory");
            case SAILING_TRAINING:
                return Arrays.asList("shared-skilling", "sailing",
                        "sailing-transitions", "boat-infrastructure");
            case SHIPWRECK_SALVAGING:
                return Arrays.asList("sailing", "shipwreck-salvaging",
                        "sailing-inventory", "boat-capabilities");
            case PVM_STRATEGY:
                return Arrays.asList("pvm-readiness", "gear-progression",
                        "hardcore-risk");
            default:
                return Collections.emptyList();
        }
    }

    private static void accountSkillSources(
            Map<StrategySourceId, StrategySourceDefinition> values)
    {
        accountSkill(values, "Smithing", StrategySourceId.IRONMAN_SMITHING,
                StrategySourceId.UIM_SMITHING);
        accountSkill(values, "Crafting", StrategySourceId.IRONMAN_CRAFTING,
                StrategySourceId.UIM_CRAFTING);
        accountSkill(values, "Herblore", StrategySourceId.IRONMAN_HERBLORE,
                StrategySourceId.UIM_HERBLORE);
        accountSkill(values, "Construction", StrategySourceId.IRONMAN_CONSTRUCTION,
                StrategySourceId.UIM_CONSTRUCTION);
        accountSkill(values, "Runecraft", StrategySourceId.IRONMAN_RUNECRAFT,
                StrategySourceId.UIM_RUNECRAFT);
        accountSkill(values, "Prayer", StrategySourceId.IRONMAN_PRAYER,
                StrategySourceId.UIM_PRAYER);
        accountSkill(values, "Farming", StrategySourceId.IRONMAN_FARMING,
                StrategySourceId.UIM_FARMING);
        accountSkill(values, "Cooking", StrategySourceId.IRONMAN_COOKING,
                StrategySourceId.UIM_COOKING);
        accountSkill(values, "Fletching", StrategySourceId.IRONMAN_FLETCHING,
                StrategySourceId.UIM_FLETCHING);
        accountSkill(values, "Fishing", StrategySourceId.IRONMAN_FISHING,
                StrategySourceId.UIM_FISHING);
        accountSkill(values, "Mining", StrategySourceId.IRONMAN_MINING,
                StrategySourceId.UIM_MINING);
        accountSkill(values, "Woodcutting", StrategySourceId.IRONMAN_WOODCUTTING,
                StrategySourceId.UIM_WOODCUTTING);
        accountSkill(values, "Hunter", StrategySourceId.IRONMAN_HUNTER,
                StrategySourceId.UIM_HUNTER);
        accountSkill(values, "Firemaking", StrategySourceId.IRONMAN_FIREMAKING,
                StrategySourceId.UIM_FIREMAKING);
        accountSkill(values, "Thieving", StrategySourceId.IRONMAN_THIEVING,
                StrategySourceId.UIM_THIEVING);
    }

    private static void accountSkill(
            Map<StrategySourceId, StrategySourceDefinition> values,
            String skill, StrategySourceId iron, StrategySourceId uim)
    {
        wiki(values, iron,
                "https://oldschool.runescape.wiki/w/Ironman_Guide/" + skill,
                "Ironman " + skill + " methods and resource strategy");
        wiki(values, uim,
                "https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/" + skill,
                "Ultimate Ironman " + skill
                        + " methods, inventory, and setup strategy");
    }
}
