package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class MilestoneTracker
{
    private final ProgressionObjectiveService progressionObjectiveService;

    /** Compatibility constructor retained for focused unit tests. */
    public MilestoneTracker()
    {
        this.progressionObjectiveService = null;
    }

    @Inject
    public MilestoneTracker(
            ProgressionObjectiveService progressionObjectiveService)
    {
        this.progressionObjectiveService = progressionObjectiveService;
    }

    public MilestoneCompletion detectCompletion(
            TrackedMilestone tracked,
            AccountSnapshot account)
    {
        if (tracked == null || account == null) return null;
        var skill = tracked.getSkill();
        if (skill == null) return null;
        var currentLevel = account.level(skill);
        if (currentLevel < tracked.getTargetLevel()) return null;
        return new MilestoneCompletion(
                tracked.getActivityId(), tracked.getTitle(), skill,
                tracked.getStartedAtLevel(), tracked.getTargetLevel()
        );
    }

    public TrackedMilestone fromRecommendations(
            List<Recommendation> recommendations)
    {
        return fromRecommendations(recommendations, null);
    }

    public TrackedMilestone fromRecommendations(
            List<Recommendation> recommendations,
            CollectionLogSnapshot collectionLog)
    {
        if (recommendations == null || recommendations.isEmpty()) return null;
        var best = recommendations.get(0);
        var skill = skillFor(best);
        if (skill == null
                || best.getCurrentLevel() <= 0
                || best.getTargetLevel() <= best.getCurrentLevel())
        {
            return null;
        }

        var plan = best.plan();
        boolean progressionProtected;
        if (progressionObjectiveService != null)
        {
            progressionProtected = progressionObjectiveService.shouldProtect(
                    plan, collectionLog
            );
        }
        else
        {
            progressionProtected = plan != null
                    && plan.method() != null
                    && plan.method().isProgressionProtected();
        }

        return new TrackedMilestone(
                best.getId(), best.getTitle(), skill.name(),
                best.getCurrentLevel(), best.getTargetLevel(),
                progressionProtected
        );
    }

    public boolean sameCheckpoint(TrackedMilestone first, TrackedMilestone second)
    {
        if (first == null || second == null) return first == second;
        return safeEquals(first.getActivityId(), second.getActivityId())
                && first.getTargetLevel() == second.getTargetLevel()
                && first.isProgressionProtected() == second.isProgressionProtected();
    }

    public static Skill skillFor(Recommendation recommendation)
    {
        if (recommendation == null || recommendation.getId() == null) return null;
        var prefix = "skill:";
        if (!recommendation.getId().startsWith(prefix)) return null;
        String name = recommendation.getId().substring(prefix.length())
                .toUpperCase(Locale.ROOT);
        try
        {
            return Skill.valueOf(name);
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    private static boolean safeEquals(String first, String second)
    {
        return first == null ? second == null : first.equals(second);
    }
}
