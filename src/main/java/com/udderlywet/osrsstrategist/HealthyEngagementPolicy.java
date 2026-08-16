package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Gentle, autonomy-supportive variety policy.
 *
 * <p>This policy is deliberately not a retention system. It does not create
 * streaks, scarcity, loss aversion, random rewards, forced breaks, or penalties
 * for leaving the game. Its only job is to notice repeated choices the player
 * already made and use those choices as weak evidence when several next actions
 * are otherwise close in strategic value.</p>
 *
 * <p>Design rules:
 * <ul>
 *   <li>One skip never changes an entire activity family.</li>
 *   <li>Repeated avoidance can temporarily lower that family.</li>
 *   <li>Several recent completions in the same family can create a small
 *       freshness adjustment, unless the method is progression-protected.</li>
 *   <li>Adjustments are time-decayed and strictly capped, so goals, readiness,
 *       account restrictions, and explicit preferences remain dominant.</li>
 * </ul>
 *
 * <p>The underlying product principle is consistent with self-determination
 * research: support player autonomy and competence rather than manufacturing
 * compulsion. The exact numbers below are product heuristics, not clinical or
 * psychological measurements of the player.</p>
 */
@Singleton
public class HealthyEngagementPolicy
{
    private static final long HOUR = 60L * 60L * 1000L;
    private static final long DAY = 24L * HOUR;

    private static final long LATER_WINDOW = 6L * HOUR;
    private static final long NOT_TODAY_WINDOW = 30L * HOUR;
    private static final long DISLIKE_WINDOW = 14L * DAY;
    private static final long COMPLETION_WINDOW = 10L * HOUR;

    /** Family adjustments should never overwhelm a genuinely better action. */
    private static final double MAX_ABSOLUTE_ADJUSTMENT = 5.0;

    private final ActivityFamilyClassifier classifier =
            new ActivityFamilyClassifier();

    public List<Recommendation> adjust(
            List<Recommendation> recommendations,
            RecommendationHistory history,
            VarietyPreference varietyPreference)
    {
        return adjust(recommendations, history, varietyPreference,
                System.currentTimeMillis());
    }

    List<Recommendation> adjust(
            List<Recommendation> recommendations,
            RecommendationHistory history,
            VarietyPreference varietyPreference,
            long nowMillis)
    {
        if (recommendations == null || recommendations.isEmpty())
            return recommendations == null ? new ArrayList<>()
                    : new ArrayList<>(recommendations);
        if (history == null || history.snapshot().isEmpty())
            return new ArrayList<>(recommendations);

        VarietyPreference safePreference = varietyPreference == null
                ? VarietyPreference.BALANCED : varietyPreference;
        FamilyEvidence evidence = summarize(history.snapshot(), nowMillis);
        List<Recommendation> adjusted = new ArrayList<>();

        for (Recommendation recommendation : recommendations)
        {
            if (recommendation == null) continue;
            ActivityFamily family = classifier.classify(recommendation);
            double delta = familyAdjustment(
                    family, evidence, safePreference,
                    isProgressionProtected(recommendation));
            adjusted.add(copyWithScore(
                    recommendation,
                    recommendation.getScore() + delta));
        }
        return adjusted;
    }

    double adjustmentFor(
            Recommendation recommendation,
            RecommendationHistory history,
            VarietyPreference preference,
            long nowMillis)
    {
        if (recommendation == null || history == null) return 0.0;
        FamilyEvidence evidence = summarize(history.snapshot(), nowMillis);
        return familyAdjustment(
                classifier.classify(recommendation), evidence,
                preference == null ? VarietyPreference.BALANCED : preference,
                isProgressionProtected(recommendation));
    }

    private FamilyEvidence summarize(
            List<RecommendationHistoryEntry> entries,
            long nowMillis)
    {
        FamilyEvidence evidence = new FamilyEvidence();
        for (RecommendationHistoryEntry entry : entries)
        {
            if (entry == null || entry.getAction() == null) continue;
            long age = Math.max(0L, nowMillis - entry.getOccurredAtMillis());
            ActivityFamily family = classifier.classify(entry.getActivityId());

            switch (entry.getAction())
            {
                case LATER:
                    if (age <= LATER_WINDOW)
                    {
                        evidence.addAvoidance(family,
                                decayed(0.35, age, LATER_WINDOW));
                    }
                    break;
                case NOT_TODAY:
                    if (age <= NOT_TODAY_WINDOW)
                    {
                        evidence.addAvoidance(family,
                                decayed(0.80, age, NOT_TODAY_WINDOW));
                    }
                    break;
                case DISLIKE:
                    if (age <= DISLIKE_WINDOW)
                    {
                        evidence.addAvoidance(family,
                                decayed(1.50, age, DISLIKE_WINDOW));
                    }
                    break;
                case COMPLETED:
                    if (age <= COMPLETION_WINDOW)
                    {
                        evidence.addCompletion(family,
                                decayed(0.55, age, COMPLETION_WINDOW));
                    }
                    break;
                default:
                    break;
            }
        }
        return evidence;
    }

    private double familyAdjustment(
            ActivityFamily family,
            FamilyEvidence evidence,
            VarietyPreference preference,
            boolean progressionProtected)
    {
        int avoidanceCount = evidence.avoidanceCount(family);
        int completionCount = evidence.completionCount(family);

        // A single Later/Not Today/Dislike action applies to that exact activity
        // through PreferenceProfile, but it is not enough evidence to infer the
        // player is tired of an entire category.
        double avoidancePenalty = avoidanceCount >= 2
                ? -evidence.avoidanceWeight(family) : 0.0;

        // Likewise, finishing one or two related actions can simply be healthy
        // momentum. Only a longer same-family run receives a mild freshness
        // adjustment. Protected outfit/untradeable grinds are exempt so a
        // Graceful/Prospector/etc. objective is not interrupted artificially.
        double repetitionPenalty = !progressionProtected && completionCount >= 3
                ? -evidence.completionWeight(family) : 0.0;

        double delta = (avoidancePenalty + repetitionPenalty)
                * preference.getMultiplier();
        return clamp(delta, -MAX_ABSOLUTE_ADJUSTMENT,
                MAX_ABSOLUTE_ADJUSTMENT);
    }

    private static double decayed(double weight, long age, long window)
    {
        if (window <= 0L || age >= window) return 0.0;
        double remaining = 1.0 - (age / (double) window);
        return weight * Math.max(0.0, Math.min(1.0, remaining));
    }

    private static boolean isProgressionProtected(Recommendation recommendation)
    {
        TrainingPlan plan = recommendation.getTrainingPlan();
        return plan != null && plan.getMethod() != null
                && plan.getMethod().isProgressionProtected();
    }

    private static Recommendation copyWithScore(
            Recommendation source,
            double score)
    {
        return new Recommendation(
                source.getId(),
                source.getTitle(),
                source.getReason(),
                score,
                source.getTrainingPlan(),
                source.getConfidence(),
                source.getCurrentLevel(),
                source.getTargetLevel());
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }

    /** Small internal accumulator keeps policy math separate from persisted data. */
    private static final class FamilyEvidence
    {
        private final Map<ActivityFamily, Integer> avoidanceCounts =
                new EnumMap<>(ActivityFamily.class);
        private final Map<ActivityFamily, Double> avoidanceWeights =
                new EnumMap<>(ActivityFamily.class);
        private final Map<ActivityFamily, Integer> completionCounts =
                new EnumMap<>(ActivityFamily.class);
        private final Map<ActivityFamily, Double> completionWeights =
                new EnumMap<>(ActivityFamily.class);

        void addAvoidance(ActivityFamily family, double weight)
        {
            avoidanceCounts.put(family, avoidanceCount(family) + 1);
            avoidanceWeights.put(family, avoidanceWeight(family) + weight);
        }

        void addCompletion(ActivityFamily family, double weight)
        {
            completionCounts.put(family, completionCount(family) + 1);
            completionWeights.put(family, completionWeight(family) + weight);
        }

        int avoidanceCount(ActivityFamily family)
        {
            return avoidanceCounts.getOrDefault(family, 0);
        }

        double avoidanceWeight(ActivityFamily family)
        {
            return avoidanceWeights.getOrDefault(family, 0.0);
        }

        int completionCount(ActivityFamily family)
        {
            return completionCounts.getOrDefault(family, 0);
        }

        double completionWeight(ActivityFamily family)
        {
            return completionWeights.getOrDefault(family, 0.0);
        }
    }
}
