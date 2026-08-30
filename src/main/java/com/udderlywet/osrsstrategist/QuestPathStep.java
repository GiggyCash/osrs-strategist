package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import net.runelite.api.Skill;

/** One unfinished quest shared by one or more proven selected-goal paths. */
public final class QuestPathStep
{
    @Getter
    private final String questName;
    @Getter
    private final QuestStatus status;
    @Getter
    private final Map<GoalType, List<String>> provenancePaths;
    @Getter
    private final List<String> unfinishedDependents;
    @Getter
    private final RecommendationConfidence readiness;
    @Getter
    private final boolean eligibleNow;
    @Getter
    private final int depth;
    @Getter
    private final Map<Skill, Integer> guaranteedRewardXp;
    @Getter
    private final double goalPathRewardValue;

    QuestPathStep(String questName, QuestStatus status,
            Map<GoalType, List<String>> provenancePaths,
            List<String> unfinishedDependents,
            RecommendationConfidence readiness,
            boolean eligibleNow, int depth,
            Map<Skill, Integer> guaranteedRewardXp,
            double goalPathRewardValue)
    {
        this.questName = questName;
        this.status = status == null ? QuestStatus.UNKNOWN : status;
        EnumMap<GoalType, List<String>> paths = new EnumMap<>(GoalType.class);
        if (provenancePaths != null)
        {
            for (Map.Entry<GoalType, List<String>> entry
                    : provenancePaths.entrySet())
                paths.put(entry.getKey(), Collections.unmodifiableList(
                        new ArrayList<>(entry.getValue())));
        }
        this.provenancePaths = Collections.unmodifiableMap(paths);
        this.unfinishedDependents = Collections.unmodifiableList(
                new ArrayList<>(unfinishedDependents == null
                        ? Collections.emptyList() : unfinishedDependents));
        this.readiness = readiness == null
                ? RecommendationConfidence.CHECK_NEEDED : readiness;
        this.eligibleNow = eligibleNow;
        this.depth = Math.max(0, depth);
        EnumMap<Skill, Integer> rewards = new EnumMap<>(Skill.class);
        if (guaranteedRewardXp != null)
            rewards.putAll(guaranteedRewardXp);
        this.guaranteedRewardXp = Collections.unmodifiableMap(rewards);
        this.goalPathRewardValue = Math.max(0.0,
                Math.min(1.0, goalPathRewardValue));
    }

    public int getGoalCount() { return provenancePaths.size(); }

    /** Bounded property value for the common recommendation decision layer. */
    public double sharedDependencyValue()
    {
        double goals = Math.max(0, getGoalCount() - 1) * 0.35;
        double dependents = unfinishedDependents.size() * 0.12;
        return Math.min(1.0, goals + dependents);
    }

    public RecommendationStrategicValue strategicValue()
    {
        double shared = sharedDependencyValue();
        if (shared <= 0.0 && goalPathRewardValue <= 0.0)
            return RecommendationStrategicValue.neutral();
        return RecommendationStrategicValue.builder()
                .sharedDependencyValue(shared)
                .unlockValue(goalPathRewardValue)
                .evidence("quest-path:" + questName)
                .build();
    }
}
