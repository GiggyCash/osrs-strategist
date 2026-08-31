package compass;

import java.util.*;
import javax.inject.Singleton;

/** Surfaces the next claimable Combat Achievement reward tier. */
@Singleton
public class CombatAchievementCandidateProvider implements CandidateProvider
{
    @Override
    public String getId()
    {
        return Text.get(1650);
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().combatAchievements() == null
                || context.data().account() == null)
        {
            return result;
        }

        // F2P characters can complete a small subset of tasks, but cannot claim
        // tier rewards. Until Compass models those individual F2P tasks, a
        // reward-tier candidate would be misleading and is intentionally absent.
        if (!ContentAccessRules.hasVerifiedMembership(
                context.data().account().membership()))
        {
            return result;
        }

        var snapshot = context.data().combatAchievements();
        var next = snapshot.nextRewardTier();
        if (next == null) return result;

        var id = Text.get(1651) + next.name().toLowerCase();
        if (context.preferenceProfile().isOnCooldown(id)) return result;

        var gap = Math.max(0, next.getRewardPoints() - snapshot.getEarnedPoints());
        var score = 26.0;
        if (gap <= 20) score += 17.0;
        else if (gap <= 75) score += 10.0;
        else if (gap <= 200) score += 5.0;
        if (context.goal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
        {
            score += next.ordinal() <= CombatAchievementTier.ELITE.ordinal() ? 25.0 : 8.0;
        }
        score += context.preferenceProfile().weightFor(id) * 10.0;

        result.add(new Recommendation(
                id,
                Text.get(1367) + pretty(next.name()),
                Text.get(1368) + gap + " point"
                        + (gap == 1 ? "" : "s")
                        + Text.get(131),
                score,
                Confidence.CHECK_NEEDED,
                null,
                SafetyEvidence.potentiallyIrreversible(false)
        ));
        return result;
    }

    private static String pretty(String value)
    {
        var lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
