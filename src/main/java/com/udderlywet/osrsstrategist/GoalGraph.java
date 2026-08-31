package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Typed long-term dependency graph.
 *
 * <p>Nodes are intentionally broad until verified game-data definitions are
 * attached. The engine can therefore understand the shape of Max, Bowfa,
 * Quest Cape, raids, and similar goals without hard-coding one giant guide.</p>
 */
@Singleton
public final class GoalGraph
{
    private final Map<GoalType, List<GoalDependency>> graph =
            new EnumMap<>(GoalType.class);
    private final Map<GoalType, List<String>> questRoots =
            new EnumMap<>(GoalType.class);

    @Inject
    public GoalGraph()
    {
        register(GoalType.MAX,
                dep("max:skills", "All trainable skills to 99", GoalNodeKind.SKILL),
                dep("max:unlocks", "Quest and access unlocks needed by efficient methods", GoalNodeKind.ACCESS),
                dep("max:resources", "Account-appropriate resources for remaining training", GoalNodeKind.RESOURCE));

        register(GoalType.QUEST_CAPE,
                dep("quest-cape:quests", "Complete the quest catalogue", GoalNodeKind.QUEST),
                dep("quest-cape:skills", "Meet quest skill requirements", GoalNodeKind.SKILL),
                dep("quest-cape:items", "Obtain required quest items and access", GoalNodeKind.RESOURCE));

        register(GoalType.BARROWS_GLOVES,
                dep("barrows-gloves:rfd-start", "Recipe for Disaster start requirements", GoalNodeKind.QUEST),
                dep("barrows-gloves:rfd-subquests", "All mandatory Recipe for Disaster subquests", GoalNodeKind.QUEST),
                dep("barrows-gloves:quest-chain", "Prerequisite quest chain", GoalNodeKind.QUEST),
                dep("barrows-gloves:quest-points", "175 quest points for the final chapter", GoalNodeKind.META),
                dep("barrows-gloves:skills", "Actual skill requirements in the quest chain", GoalNodeKind.SKILL),
                dep("barrows-gloves:combat", "Culinaromancer fights and quest combat readiness", GoalNodeKind.PVM_ENCOUNTER));
        roots(GoalType.BARROWS_GLOVES, "Recipe for Disaster");

        register(GoalType.FIRE_CAPE,
                dep("fire-cape:combat", "Conservative Fight Cave combat readiness", GoalNodeKind.SKILL),
                dep("fire-cape:gear", "Practical Ranged gear and supplies", GoalNodeKind.GEAR),
                dep("fire-cape:jad", "Complete the TzHaar Fight Cave and defeat TzTok-Jad", GoalNodeKind.ACTIVITY));

        register(GoalType.PRIFDDINAS,
                dep("prif:quest-chain", "Song of the Elves prerequisite chain", GoalNodeKind.QUEST),
                dep("prif:skills", "Required skill levels", GoalNodeKind.SKILL));
        roots(GoalType.PRIFDDINAS, "Song of the Elves");

        register(GoalType.BOWFA,
                dep("bowfa:prif", "Prifddinas access", GoalNodeKind.ACCESS),
                dep("bowfa:gauntlet", "Gauntlet progression and practical PvM readiness", GoalNodeKind.ACTIVITY),
                dep("bowfa:seed", "Enhanced crystal weapon seed", GoalNodeKind.ITEM),
                dep("bowfa:shards", "Required crystal shard resources", GoalNodeKind.RESOURCE));
        roots(GoalType.BOWFA, "Song of the Elves");

        register(GoalType.INFERNAL_CAPE,
                dep("infernal:access", "Inferno access and prerequisite combat progression", GoalNodeKind.ACCESS),
                dep("infernal:gear", "Practical Inferno gear and supplies", GoalNodeKind.GEAR),
                dep("infernal:readiness", "Combat, Prayer, spellbook, and mechanical readiness", GoalNodeKind.ACTIVITY));

        register(GoalType.DIARY_CAPE,
                dep("diary-cape:tiers", "Complete all Achievement Diary tiers", GoalNodeKind.DIARY),
                dep("diary-cape:skills", "Meet diary skill requirements", GoalNodeKind.SKILL),
                dep("diary-cape:quests", "Meet diary quest and access requirements", GoalNodeKind.QUEST));

        register(GoalType.ELITE_COMBAT_ACHIEVEMENTS,
                dep("elite-ca:points", "Reach the Elite Combat Achievement reward tier", GoalNodeKind.COMBAT_ACHIEVEMENT),
                dep("elite-ca:boss-access", "Unlock suitable bosses and encounters", GoalNodeKind.ACCESS),
                dep("elite-ca:gear", "Build practical encounter-specific gear", GoalNodeKind.GEAR));

        register(GoalType.RAID_READY,
                dep("raid-ready:combat", "Appropriate combat stats and prayers", GoalNodeKind.SKILL),
                dep("raid-ready:spellbooks", "Required spellbook and utility unlocks", GoalNodeKind.ACCESS),
                dep("raid-ready:gear", "Practical raid gear ladder", GoalNodeKind.GEAR),
                dep("raid-ready:supplies", "Sustainable food, potions, ammunition, and runes", GoalNodeKind.RESOURCE));

        register(GoalType.TOTAL_2000,
                dep("total-2000:levels", "Reach 2000 total using high-value available skill gains", GoalNodeKind.SKILL));

        register(GoalType.SLAYER_85,
                dep("slayer-85:slayer", "Reach 85 Slayer", GoalNodeKind.SKILL),
                dep("slayer-85:combat", "Build supporting combat, Prayer, and gear", GoalNodeKind.GEAR));

        register(GoalType.BASE_70S,
                dep("base-70s:skills", "Raise every trainable skill to at least 70", GoalNodeKind.SKILL));

        register(GoalType.GEAR_TARGET,
                dep("gear-target:item", "Selected gear target", GoalNodeKind.GEAR),
                dep("gear-target:requirements", "Access, resources, and encounter prerequisites", GoalNodeKind.META));
    }

    public GoalPathPreview previewFor(GoalType goalType)
    {
        GoalType safe = goalType == null ? GoalType.AUTOMATIC : goalType;
        return new GoalPathPreview(
                safe,
                graph.getOrDefault(safe, Collections.emptyList())
        );
    }

    /** Backwards-compatible label view used by early UI/module code. */
    public List<String> dependenciesFor(GoalType goalType)
    {
        List<String> labels = new ArrayList<>();
        for (GoalDependency dependency : previewFor(goalType).getDependencies())
        {
            labels.add(dependency.getLabel());
        }
        return Collections.unmodifiableList(labels);
    }

    /** Verified quest entry points whose transitive requirements can prove a path. */
    public List<String> questRootsFor(GoalType goalType)
    {
        return questRoots.getOrDefault(goalType, Collections.emptyList());
    }

    private void register(GoalType type, GoalDependency... dependencies)
    {
        List<GoalDependency> values = new ArrayList<>();
        Collections.addAll(values, dependencies);
        graph.put(type, Collections.unmodifiableList(values));
    }

    private void roots(GoalType type, String... roots)
    {
        List<String> values = new ArrayList<>();
        Collections.addAll(values, roots);
        questRoots.put(type, Collections.unmodifiableList(values));
    }

    private static GoalDependency dep(
            String id,
            String label,
            GoalNodeKind kind)
    {
        return new GoalDependency(id, label, kind, true);
    }
}
