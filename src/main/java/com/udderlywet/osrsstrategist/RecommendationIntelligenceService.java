package com.udderlywet.osrsstrategist;

import java.util.Locale;
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
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null)
        {
            return recommendation.getScore();
        }

        double score = recommendation.getScore();
        String id = lower(recommendation.getId());
        String title = lower(recommendation.getTitle());
        String reason = lower(recommendation.getReason());
        RecommendationGuidance guidance = recommendation.getGuidance();

        score += readinessValue(recommendation, guidance);
        score += goalValue(recommendation, context.getActiveGoal());
        score += questRewardValue(recommendation, context);
        score += sessionValue(recommendation, context.getSessionIntent());
        score += globalIntentValue(recommendation, context, id, reason);
        score += strategyModeValue(recommendation, context, id, reason);
        score += accountModeValue(recommendation, context, id, title, reason);
        score += safetyValue(context, id, title, reason);
        score += resourceValue(guidance, context.getAccountMode());
        score += opportunityCostValue(recommendation, context, id, reason);
        score += uimSetupCostService.score(recommendation, context);

        // Preference weight, snooze timing, dislike weight, and fatigue are
        // already priced into candidate scores by their producer. Applying them
        // again here made feedback stronger than configured and could rotate an
        // otherwise-correct DO NEXT choice too aggressively.
        return score;
    }

    private static double readinessValue(Recommendation recommendation, RecommendationGuidance guidance)
    {
        double value = 0.0;
        if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED) value += 7.0;
        else if (recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED) value -= 9.0;
        else if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED) return -10_000.0;
        if (guidance != null && hasText(guidance.getAction())) value += 5.0;
        if (guidance != null && hasText(guidance.getLocation())) value += 2.0;
        if (guidance != null && hasText(guidance.getSupplies())) value += 2.0;
        return value;
    }

    static double goalValue(Recommendation recommendation, GoalType selectedGoal)
    {
        if (recommendation == null || selectedGoal == null) return 0.0;
        GoalDependencyProvenance provenance = recommendation.getGoalProvenance();
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
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan == null || plan.getMethod() == null
                || plan.getMethod().getSkill() == null
                || recommendation.getTargetLevel() <= 0) return 0.0;
        Skill skill = plan.getMethod().getSkill();
        GoalQuestRewardForecast forecast = goalProvenanceService
                .guaranteedRewardsBeforeManualTraining(context, skill);
        if (!forecast.hasGuaranteedExperience()) return 0.0;
        int currentXp = context.getData().getAccount().getSkillExperience(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(
                    context.getData().getAccount().getSkillLevel(skill));
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
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan == null || plan.getMethod() == null || intent == null) return 0.0;
        TrainingMethod method = plan.getMethod();
        int setup = Math.max(0, method.getSetupMinutes());
        int minimum = Math.max(0, method.getMinimumSessionMinutes());
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

    /** Session intent also prices non-training work in the shared final queue. */
    private static double globalIntentValue(Recommendation recommendation,
            StrategyContext context, String id, String reason)
    {
        if (recommendation.getTrainingPlan() != null) return 0.0;
        SessionIntent intent = context.getSessionIntent();
        boolean longActivity = id.startsWith("quest:") || id.startsWith("pvm:")
                || id.startsWith("minigame:") || id.startsWith("diary:");
        boolean readyRecurring = id.startsWith("opportunity:")
                || id.startsWith("recurring:");
        switch (intent)
        {
            case QUICK_20_MIN:
                if (readyRecurring) return 9.0;
                return longActivity ? -8.0 : 2.0;
            case ONE_HOUR:
                return longActivity ? 3.0 : readyRecurring ? 2.0 : 0.0;
            case LONG_SESSION:
                return longActivity ? 7.0 : 0.0;
            case AFK:
                if (readyRecurring) return 5.0;
                if (id.startsWith("pvm:") || id.startsWith("combat-achievement:"))
                    return -8.0;
                return containsAny(reason, "low attention", "afk", "relaxed")
                        ? 5.0 : 0.0;
            case PICK_FOR_ME:
            default:
                return 0.0;
        }
    }

    /** Strategy mode prices every recommendation family, not method names alone. */
    private static double strategyModeValue(Recommendation recommendation,
            StrategyContext context, String id, String reason)
    {
        StrategyMode mode = context.getStrategyMode();
        TrainingPlan plan = recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        boolean unlock = id.startsWith("quest:") || id.startsWith("diary:")
                || id.startsWith("transport:") || id.startsWith("detour:");
        boolean encounter = id.startsWith("pvm:")
                || id.startsWith("combat-achievement:");
        boolean shared = containsAny(reason, "also advances", "multiple goals",
                "shared prerequisite", "cross-skill", "future time");
        switch (mode)
        {
            case EFFICIENT:
                if (unlock || shared) return 6.0;
                if (encounter && recommendation.getConfidence()
                        == RecommendationConfidence.VERIFIED) return 3.0;
                return method != null
                        && method.getAttentionLevel() == AttentionLevel.ACTIVE
                        ? 3.0 : 0.0;
            case RELAXED:
                if (encounter) return -5.0;
                if (id.startsWith("opportunity:") || id.startsWith("recurring:"))
                    return 4.0;
                if (method != null)
                {
                    if (method.getAttentionLevel() == AttentionLevel.AFK) return 7.0;
                    if (method.getAttentionLevel() == AttentionLevel.LOW) return 4.0;
                    if (method.getAttentionLevel() == AttentionLevel.ACTIVE) return -5.0;
                }
                return containsAny(reason, "sustainable", "low fatigue") ? 4.0 : 0.0;
            case BALANCED:
            default:
                return shared || containsAny(reason, "sustainable") ? 3.0 : 0.0;
        }
    }

    private static double accountModeValue(Recommendation recommendation, StrategyContext context, String id, String title, String reason)
    {
        AccountMode mode = context.getAccountMode();
        TrainingPlan plan = recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        double value = 0.0;
        String identity = id + " " + title + " " + reason;

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            if (method != null)
            {
                int setup = Math.max(0, method.getSetupMinutes());
                if (setup <= 3) value += 8.0;
                else if (setup >= 12) value -= 14.0;
                else if (setup >= 7) value -= 5.0;
            }
            if (id.startsWith("detour:") && !containsAny(identity, "uim")) value -= 8.0;
            if (containsAny(reason, "bank", "grand exchange")) value -= 12.0;
        }
        else if (mode.isIronLike())
        {
            if (id.startsWith("detour:")) value += 9.0;
            if (id.startsWith("upgrade:")) value += 5.0;
            if (containsAny(reason, "self-source", "self source", "supplies")) value += 4.0;
            if (containsAny(reason, "grand exchange")) value -= 15.0;
        }
        else if (mode.usesGrandExchange())
        {
            if (id.startsWith("money:")) value += 4.0;
            if (id.startsWith("detour:")) value -= 3.0;
        }
        return value;
    }

    private static double safetyValue(StrategyContext context, String id, String title, String reason)
    {
        String identity = id + " " + title + " " + reason;
        boolean wilderness = containsAny(identity, "wilderness", "wildy", "revenant", "revs");
        if (wilderness && !context.isAllowWildernessMethods()) return -5_000.0;
        AccountMode mode = context.getAccountMode();
        if ((mode == AccountMode.HARDCORE_IRONMAN || mode == AccountMode.HARDCORE_GROUP_IRONMAN) && wilderness)
            return -7_500.0;
        if ((mode == AccountMode.HARDCORE_IRONMAN || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && id.startsWith("pvm:") && containsAny(reason, "dangerous", "high risk")) return -20.0;
        return 0.0;
    }

    private static double resourceValue(RecommendationGuidance guidance, AccountMode mode)
    {
        if (guidance == null || !hasText(guidance.getSupplies())) return 0.0;
        String supplies = lower(guidance.getSupplies());
        double value = 0.0;
        if (containsAny(supplies, "verified:", "you own", "already have")) value += 5.0;
        if (containsAny(supplies, "open your bank", "needs bank", "bank once")) value -= 6.0;
        if (containsAny(supplies, "needs info", "not yet verified")) value -= 10.0;
        if (mode != null && mode.isIronLike() && containsAny(supplies, "self-source", "self source")) value -= 2.0;
        if (mode != null && mode.usesGrandExchange() && containsAny(supplies, "grand exchange")
                && !containsAny(supplies, "cannot afford", "short of")) value += 2.0;
        return value;
    }

    private static double opportunityCostValue(Recommendation recommendation, StrategyContext context, String id, String reason)
    {
        if (id.startsWith("detour:"))
            return containsAny(reason, " + ", "while", "also", "cross-skill") ? 5.0 : -6.0;
        if (id.contains("angler"))
        {
            int fishing = context.getData().getAccount().getSkillLevel(Skill.FISHING);
            if (fishing >= 90) return 5.0;
            if (fishing < 55 && context.getActiveGoal() != GoalType.MAX && !context.isCollectionistMode()) return -8.0;
        }
        return 0.0;
    }

    private static boolean containsAny(String haystack, String... needles)
    {
        if (haystack == null) return false;
        for (String needle : needles)
            if (needle != null && haystack.contains(lower(needle))) return true;
        return false;
    }

    private static String lower(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
