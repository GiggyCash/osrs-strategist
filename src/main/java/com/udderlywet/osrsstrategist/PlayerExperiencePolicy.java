package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;

/**
 * Healthy engagement policy applied after core progression scoring.
 *
 * <p>The goal is not to maximize time-on-plugin or manufacture compulsion. It
 * is to reduce the most common reasons a good progression planner becomes
 * annoying: repeating the same suggestion after the player has just completed
 * it, repeatedly surfacing a family the player has deferred, and ignoring the
 * player's requested session shape.</p>
 *
 * <p>The policy borrows conservative ideas from autonomy-supportive design,
 * competence/visible-progress feedback, and flow/variety principles:</p>
 * <ul>
 *   <li><b>Autonomy:</b> explicit Later/Not Today/Dislike feedback is respected
 *       more strongly than inferred behavior.</li>
 *   <li><b>Competence:</b> strategically important progress is never hard
 *       blocked merely because a similar task was recently completed.</li>
 *   <li><b>Variety:</b> repeated completions in the same activity family receive
 *       a temporary soft penalty so another useful option can win.</li>
 *   <li><b>Session fit:</b> the core engine remains responsible for AFK/active
 *       method fit; this layer does not second-guess verified method safety.</li>
 * </ul>
 *
 * <p>All adjustments are deliberately small. Core account progression,
 * requirements, safety policy, membership, account type, and explicit user
 * settings remain more important than this comfort layer.</p>
 */
@Singleton
public class PlayerExperiencePolicy
{
    private static final long TWO_HOURS = 2L * 60L * 60L * 1000L;
    private static final long ONE_DAY = 24L * 60L * 60L * 1000L;

    private static final double SAME_ACTIVITY_RECENT_COMPLETION_PENALTY = -7.0;
    private static final double SAME_FAMILY_RECENT_COMPLETION_PENALTY = -2.5;
    private static final double RECENT_LATER_PENALTY = -4.0;
    private static final double RECENT_NOT_TODAY_PENALTY = -10.0;
    private static final double RECENT_DISLIKE_PENALTY = -12.0;

    public StrategyResult rerank(
            StrategyResult result,
            RecommendationHistory history)
    {
        if (result == null || result.getRecommendations().isEmpty()
                || history == null)
        {
            return result;
        }

        long now = System.currentTimeMillis();
        List<RecommendationHistoryEntry> entries = history.snapshot();
        List<Recommendation> adjusted = new ArrayList<>();

        for (Recommendation recommendation : result.getRecommendations())
        {
            double delta = adjustmentFor(recommendation, entries, now);
            adjusted.add(recommendation.withScore(
                    recommendation.getScore() + delta));
        }

        adjusted.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());

        return new StrategyResult(
                adjusted,
                result.getOpportunities(),
                result.getSignals()
        );
    }

    double adjustmentFor(
            Recommendation recommendation,
            List<RecommendationHistoryEntry> history,
            long now)
    {
        if (recommendation == null || recommendation.getId() == null
                || history == null || history.isEmpty())
        {
            return 0.0;
        }

        double adjustment = 0.0;
        String id = recommendation.getId();
        String family = familyOf(id);

        for (int i = history.size() - 1; i >= 0; i--)
        {
            RecommendationHistoryEntry entry = history.get(i);
            if (entry == null || entry.getActivityId() == null
                    || entry.getAction() == null)
            {
                continue;
            }

            long age = Math.max(0L, now - entry.getOccurredAtMillis());
            if (age > ONE_DAY) continue;

            boolean sameActivity = id.equals(entry.getActivityId());
            boolean sameFamily = family.equals(familyOf(entry.getActivityId()));

            switch (entry.getAction())
            {
                case COMPLETED:
                    if (age <= TWO_HOURS && sameActivity)
                    {
                        adjustment += SAME_ACTIVITY_RECENT_COMPLETION_PENALTY;
                    }
                    else if (age <= TWO_HOURS && sameFamily)
                    {
                        adjustment += SAME_FAMILY_RECENT_COMPLETION_PENALTY;
                    }
                    break;
                case LATER:
                    if (sameActivity) adjustment += RECENT_LATER_PENALTY;
                    break;
                case NOT_TODAY:
                    if (sameActivity) adjustment += RECENT_NOT_TODAY_PENALTY;
                    break;
                case DISLIKE:
                    if (sameActivity) adjustment += RECENT_DISLIKE_PENALTY;
                    break;
                default:
                    break;
            }
        }

        // Bound the comfort layer. A player should never lose a critical path
        // because many old history entries accumulated the same tiny penalty.
        return Math.max(-18.0, Math.min(6.0, adjustment));
    }

    /**
     * Stable broad families used only for variety. IDs remain the source of
     * truth for explicit preference/cooldown behavior.
     */
    static String familyOf(String activityId)
    {
        if (activityId == null || activityId.isEmpty()) return "unknown";
        int separator = activityId.indexOf(':');
        return separator <= 0 ? activityId : activityId.substring(0, separator);
    }
}
