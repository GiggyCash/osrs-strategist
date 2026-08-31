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
                dep("max:unlocks", PlayerText.get("GG9"), GoalNodeKind.ACCESS),
                dep("max:resources", PlayerText.get("GG10"), GoalNodeKind.RESOURCE));

        register(GoalType.QUEST_CAPE,
                dep("quest-cape:quests", "Complete the quest catalogue", GoalNodeKind.QUEST),
                dep("quest-cape:skills", "Meet quest skill requirements", GoalNodeKind.SKILL),
                dep("quest-cape:items", PlayerText.get("GG11"), GoalNodeKind.RESOURCE));

        register(GoalType.BARROWS_GLOVES,
                dep("barrows-gloves:rfd-start", PlayerText.get("GG12"), GoalNodeKind.QUEST),
                dep("barrows-gloves:rfd-subquests", PlayerText.get("GG13"), GoalNodeKind.QUEST),
                dep("barrows-gloves:quest-chain", "Prerequisite quest chain", GoalNodeKind.QUEST),
                dep("barrows-gloves:quest-points", PlayerText.get("GG14"), GoalNodeKind.META),
                dep("barrows-gloves:skills", PlayerText.get("GG15"), GoalNodeKind.SKILL),
                dep("barrows-gloves:combat", PlayerText.get("GG16"), GoalNodeKind.PVM_ENCOUNTER));
        roots(GoalType.BARROWS_GLOVES, "Recipe for Disaster");

        register(GoalType.FIRE_CAPE,
                dep("fire-cape:combat", PlayerText.get("GG17"), GoalNodeKind.SKILL),
                dep("fire-cape:gear", "Practical Ranged gear and supplies", GoalNodeKind.GEAR),
                dep("fire-cape:jad", PlayerText.get("GG18"), GoalNodeKind.ACTIVITY));

        register(GoalType.PRIFDDINAS,
                dep("prif:quest-chain", "Song of the Elves prerequisite chain", GoalNodeKind.QUEST),
                dep("prif:skills", "Required skill levels", GoalNodeKind.SKILL));
        roots(GoalType.PRIFDDINAS, "Song of the Elves");

        register(GoalType.BOWFA,
                dep("bowfa:prif", "Prifddinas access", GoalNodeKind.ACCESS),
                dep("bowfa:gauntlet", PlayerText.get("GG19"), GoalNodeKind.ACTIVITY),
                dep("bowfa:seed", "Enhanced crystal weapon seed", GoalNodeKind.ITEM),
                dep("bowfa:shards", "Required crystal shard resources", GoalNodeKind.RESOURCE));
        roots(GoalType.BOWFA, "Song of the Elves");

        register(GoalType.INFERNAL_CAPE,
                dep("infernal:access", PlayerText.get("GG20"), GoalNodeKind.ACCESS),
                dep("infernal:gear", "Practical Inferno gear and supplies", GoalNodeKind.GEAR),
                dep("infernal:readiness", PlayerText.get("GG21"), GoalNodeKind.ACTIVITY));

        register(GoalType.DIARY_CAPE,
                dep("diary-cape:tiers", "Complete all Achievement Diary tiers", GoalNodeKind.DIARY),
                dep("diary-cape:skills", "Meet diary skill requirements", GoalNodeKind.SKILL),
                dep("diary-cape:quests", PlayerText.get("GG22"), GoalNodeKind.QUEST));

        register(GoalType.ELITE_COMBAT_ACHIEVEMENTS,
                dep("elite-ca:points", PlayerText.get("GG23"), GoalNodeKind.COMBAT_ACHIEVEMENT),
                dep("elite-ca:boss-access", "Unlock suitable bosses and encounters", GoalNodeKind.ACCESS),
                dep("elite-ca:gear", PlayerText.get("GG24"), GoalNodeKind.GEAR));

        register(GoalType.RAID_READY,
                dep("raid-ready:combat", "Appropriate combat stats and prayers", GoalNodeKind.SKILL),
                dep("raid-ready:spellbooks", PlayerText.get("GG25"), GoalNodeKind.ACCESS),
                dep("raid-ready:gear", "Practical raid gear ladder", GoalNodeKind.GEAR),
                dep("raid-ready:supplies", PlayerText.get("GG26"), GoalNodeKind.RESOURCE));

        register(GoalType.TOTAL_2000,
                dep("total-2000:levels", PlayerText.get("GG27"), GoalNodeKind.SKILL));

        register(GoalType.SLAYER_85,
                dep("slayer-85:slayer", "Reach 85 Slayer", GoalNodeKind.SKILL),
                dep("slayer-85:combat", PlayerText.get("GG28"), GoalNodeKind.GEAR));

        register(GoalType.BASE_70S,
                dep("base-70s:skills", PlayerText.get("GG29"), GoalNodeKind.SKILL));

        register(GoalType.GEAR_TARGET,
                dep("gear-target:item", "Selected gear target", GoalNodeKind.GEAR),
                dep("gear-target:requirements", PlayerText.get("GG30"), GoalNodeKind.META));
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
