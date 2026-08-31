package compass;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.Experience;

/** Global account-value ranking after all legal candidates enter one pool. */
@Singleton
public class RecommendationIntelligenceService
{
    private final UimSetupCostService uimSetupCostService;
    private final GoalDependencyProvenanceService goalProvenanceService;

    @Inject
    public RecommendationIntelligenceService(UimSetupCostService uimSetupCostService,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this.uimSetupCostService = uimSetupCostService == null
                ? new UimSetupCostService() : uimSetupCostService;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
    }

    public RecommendationIntelligenceService(UimSetupCostService uimSetupCostService)
    {
        this(uimSetupCostService, new GoalDependencyProvenanceService());
    }

    public RecommendationIntelligenceService()
    {
        this(new UimSetupCostService());
    }

    public double rankScore(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null) return Double.NEGATIVE_INFINITY;
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return recommendation.getScore();
        }

        var score = recommendation.getScore();
        score += recommendation.getStrategicValue().scoreDelta();
        var guidance = recommendation.getGuidance();

        score += readinessValue(recommendation, guidance);
        score += goalValue(recommendation, context.goal());
        score += questRewardValue(recommendation, context);
        score += sessionValue(recommendation, context.intent());
        score += strategyModeValue(recommendation, context);
        score += uimSetupCostService.score(recommendation, context);

        // Preference weight, snooze timing, dislike weight, and fatigue are
        // already priced into candidate scores by their producer. Applying them
        // again here made feedback stronger than configured and could rotate an
        // otherwise-correct DO NEXT choice too aggressively.
        return score;
    }

    private static double readinessValue(Recommendation recommendation, Guidance guidance)
    {
        if (recommendation.getConfidence() == Confidence.BLOCKED)
            return -10_000.0;
        if (recommendation.getConfidence() == Confidence.CHECK_NEEDED)
            return -9.0;
        // Presentability/actionability is a gate, not strategic value. A more
        // verbose or easily verified candidate must not beat a better action
        // merely because it supplied more text fields.
        return 0.0;
    }

    static double goalValue(Recommendation recommendation, GoalType selectedGoal)
    {
        if (recommendation == null || selectedGoal == null) return 0.0;
        var provenance = recommendation.getGoalProvenance();
        if (provenance == null
                || !provenance.proves(selectedGoal, recommendation.getId()))
            return 0.0;
        double direct;
        switch (selectedGoal)
        {
            case MAX: direct = 8.0; break;
            case QUEST_CAPE: direct = 28.0; break;
            case BARROWS_GLOVES: direct = 38.0; break;
            case FIRE_CAPE: direct = 45.0; break;
            case PRIFDDINAS: direct = 42.0; break;
            case BOWFA: direct = 38.0; break;
            case INFERNAL_CAPE: direct = 45.0; break;
            case DIARY_CAPE: direct = 30.0; break;
            case ELITE_COMBAT_ACHIEVEMENTS: direct = 32.0; break;
            case RAID_READY: direct = 24.0; break;
            case TOTAL_2000: direct = 19.0; break;
            case SLAYER_85: direct = 45.0; break;
            case BASE_70S: direct = 25.0; break;
            case GEAR_TARGET: direct = 35.0; break;
            default: direct = 0.0; break;
        }
        return provenance.getRelationship() == GoalRecommendationRelationship.DIRECT
                ? direct : Math.min(26.0, direct * 0.7);
    }

    private double questRewardValue(
            Recommendation recommendation, StrategyContext context)
    {
        var plan = recommendation.plan();
        if (plan == null || plan.method() == null
                || plan.method().getSkill() == null
                || recommendation.getTargetLevel() <= 0) return 0.0;
        var skill = plan.method().getSkill();
        GoalQuestRewardForecast forecast = goalProvenanceService
                .guaranteedRewardsBeforeManualTraining(context, skill);
        if (!forecast.hasGuaranteedExperience()) return 0.0;
        var currentXp = context.data().account().xp(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(
                    context.data().account().level(skill));
        int remaining = Math.max(0,
                Experience.getXpForLevel(recommendation.getTargetLevel())
                        - currentXp);
        if (remaining <= 0) return 0.0;
        double coverage = Math.min(1.0,
                forecast.getExperience() / (double) remaining);
        if (coverage >= 1.0) return -36.0;
        if (coverage >= 0.75) return -28.0;
        if (coverage >= 0.5) return -18.0;
        return -10.0 * coverage;
    }

    private static double sessionValue(Recommendation recommendation, SessionIntent intent)
    {
        var plan = recommendation.plan();
        if (plan == null || plan.method() == null || intent == null) return 0.0;
        var method = plan.method();
        var setup = Math.max(0, method.getSetupMinutes());
        var minimum = Math.max(0, method.getMinimumSessionMinutes());
        switch (intent)
        {
            case QUICK_20_MIN:
                if (setup <= 3 && minimum <= 20) return 8.0;
                if (setup >= 10 || minimum > 30) return -12.0;
                return 1.0;
            case ONE_HOUR:
                if (setup <= 8 && minimum <= 60) return 4.0;
                return minimum > 90 ? -5.0 : 0.0;
            case LONG_SESSION:
                return minimum >= 30 ? 4.0 : 1.0;
            case AFK:
                if (method.getAttentionLevel() == AttentionLevel.AFK) return 12.0;
                if (method.getAttentionLevel() == AttentionLevel.LOW) return 7.0;
                if (method.getAttentionLevel() == AttentionLevel.ACTIVE) return -9.0;
                return 0.0;
            case PICK_FOR_ME:
            default:
                return 0.0;
        }
    }

    /** Strategy mode prices typed properties; IDs and player-facing prose do not. */
    private static double strategyModeValue(Recommendation recommendation,
            StrategyContext context)
    {
        var mode = context.mode();
        var plan = recommendation.plan();
        var method = plan == null ? null : plan.method();
        var value = recommendation.getStrategicValue();
        switch (mode)
        {
            case EFFICIENT:
                return value.getUnlockValue() * 4.0
                        + value.getSharedDependencyValue() * 5.0
                        + Math.max(0.0, value.getTravelFit()) * 2.0
                        + (method != null
                        && method.getAttentionLevel() == AttentionLevel.ACTIVE
                        ? 3.0 : 0.0);
            case RELAXED:
                double relaxed = value.getSetupReuse() * 4.0
                        - value.getRiskBurden() * 5.0;
                if (method != null)
                {
                    if (method.getAttentionLevel() == AttentionLevel.AFK) relaxed += 7.0;
                    if (method.getAttentionLevel() == AttentionLevel.LOW) relaxed += 4.0;
                    if (method.getAttentionLevel() == AttentionLevel.ACTIVE) relaxed -= 5.0;
                }
                return relaxed;
            case BALANCED:
            default:
                return value.getSharedDependencyValue() * 3.0
                        + value.getSetupReuse() * 1.5;
        }
    }
}
