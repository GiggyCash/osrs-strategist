package compass;
import lombok.*;

import java.util.*;
import javax.inject.*;
import net.runelite.api.Skill;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MilestoneTracker
{
    private final ProgressionObjectiveService progressionObjectiveService;

    public MilestoneCompletion detectCompletion(
            TrackedMilestone tracked,
            AccountSnapshot account)
    {
        if (tracked == null || account == null) return null;
        var skill = tracked.getSkill();
        if (skill == null) return null;
        var currentLevel = account.level(skill);
        if (currentLevel < tracked.targetLevel) return null;
        return new MilestoneCompletion(
                tracked.activityId, tracked.title, skill,
                tracked.getStartedAtLevel(), tracked.targetLevel
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
                || best.currentLevel <= 0
                || best.targetLevel <= best.currentLevel)
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
                    && plan.method().progressionProtected;
        }

        return new TrackedMilestone(
                best.id, best.title, skill.name(),
                best.currentLevel, best.targetLevel,
                progressionProtected
        );
    }

    public boolean sameCheckpoint(TrackedMilestone first, TrackedMilestone second)
    {
        if (first == null || second == null) return first == second;
        return safeEquals(first.activityId, second.activityId)
                && first.targetLevel == second.targetLevel
                && first.progressionProtected == second.progressionProtected;
    }

    public static Skill skillFor(Recommendation recommendation)
    {
        if (recommendation == null || recommendation.id == null) return null;
        var prefix = "skill:";
        if (!recommendation.id.startsWith(prefix)) return null;
        String name = recommendation.id.substring(prefix.length())
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
