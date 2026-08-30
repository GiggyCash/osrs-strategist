package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight dependency graph. Goal data will grow independently of the
 * strategy engine so new game content can usually be represented as data.
 */
public final class GoalGraph
{
    private final Map<GoalType, List<String>> dependencies =
            new EnumMap<>(GoalType.class);

    public GoalGraph()
    {
        register(GoalType.MAX,
                "All skills to 99",
                "Quest and unlock dependencies needed for efficient training",
                "Account-specific resource and access requirements");

        register(GoalType.QUEST_CAPE,
                "Quest skill requirements",
                "Quest prerequisite chains",
                "Required items and access unlocks");

        register(GoalType.BARROWS_GLOVES,
                "Recipe for Disaster prerequisite chain",
                "Required quest and skill dependencies");

        register(GoalType.PRIFDDINAS,
                "Song of the Elves prerequisite chain",
                "Required skill levels and quests");

        register(GoalType.TOTAL_2000,
                "Reach 2000 total level using the highest-value available skill gains");

        register(GoalType.SLAYER_85,
                "Reach 85 Slayer while building appropriate combat support");

        register(GoalType.BASE_70S,
                "Raise every trainable skill to at least 70");
    }

    public List<String> dependenciesFor(GoalType goalType)
    {
        return dependencies.getOrDefault(
                goalType,
                Collections.emptyList()
        );
    }

    private void register(GoalType type, String... nodes)
    {
        List<String> values = new ArrayList<>();
        Collections.addAll(values, nodes);
        dependencies.put(
                type,
                Collections.unmodifiableList(values)
        );
    }
}
