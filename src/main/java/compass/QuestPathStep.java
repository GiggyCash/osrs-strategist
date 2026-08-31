package compass;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** One unfinished quest shared by one or more proven selected-goal paths. */
@Getter
public final class QuestPathStep
{
    private final String questName;
    private final QuestStatus status;
    private final Map<GoalType, List<String>> provenancePaths;
    private final List<String> unfinishedDependents;
    private final Confidence readiness;
    private final boolean eligibleNow;
    private final int depth;
    private final Map<Skill, Integer> guaranteedRewardXp;
    private final double goalPathRewardValue;

    QuestPathStep(String questName, QuestStatus status,
            Map<GoalType, List<String>> provenancePaths,
            List<String> unfinishedDependents,
            Confidence readiness,
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
                ? Confidence.CHECK_NEEDED : readiness;
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
        var goals = Math.max(0, getGoalCount() - 1) * 0.35;
        var dependents = unfinishedDependents.size() * 0.12;
        return Math.min(1.0, goals + dependents);
    }

    public StrategicValue strategicValue()
    {
        var shared = sharedDependencyValue();
        if (shared <= 0.0 && goalPathRewardValue <= 0.0)
            return StrategicValue.neutral();
        return StrategicValue.builder()
                .sharedDependencyValue(shared)
                .unlockValue(goalPathRewardValue)
                .evidence("quest-path:" + questName)
                .build();
    }
}
