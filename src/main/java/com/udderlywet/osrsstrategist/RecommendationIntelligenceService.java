package com.udderlywet.osrsstrategist;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Global account-value ranking after all legal candidates enter one pool. */
@Singleton
public class RecommendationIntelligenceService
{
    private final UimSetupCostService uimSetupCostService;

    @Inject
    public RecommendationIntelligenceService(UimSetupCostService uimSetupCostService)
    {
        this.uimSetupCostService = uimSetupCostService == null
                ? new UimSetupCostService() : uimSetupCostService;
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
        score += goalValue(recommendation, context, id, title);
        score += sessionValue(recommendation, context.getSessionIntent());
        score += accountModeValue(recommendation, context, id, title, reason);
        score += safetyValue(context, id, title, reason);
        score += resourceValue(guidance, context.getAccountMode());
        score += opportunityCostValue(recommendation, context, id, reason);
        score += uimSetupCostService.score(recommendation, context);

        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences != null && recommendation.getId() != null)
        {
            score += preferences.weightFor(recommendation.getId()) * 8.0;
            score += preferences.timedScoreAdjustmentFor(recommendation.getId());
        }
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

    private static double goalValue(Recommendation recommendation, StrategyContext context, String id, String title)
    {
        GoalType goal = context.getActiveGoal() == null ? GoalType.MAX : context.getActiveGoal();
        boolean skill = id.startsWith("skill:");
        boolean quest = id.startsWith("quest:");
        boolean gear = id.startsWith("gear:") || id.startsWith("upgrade:");
        boolean pvm = id.startsWith("pvm:");
        boolean diary = id.startsWith("diary:");
        boolean ca = id.startsWith("combat-achievement:") || id.startsWith("combat_achievement:");
        String identity = id + " " + title;

        switch (goal)
        {
            case MAX:
                if (skill) return 8.0;
                if (id.startsWith("detour:")) return 5.0;
                if (gear) return 3.0;
                if (quest) return 2.0;
                return 0.0;
            case QUEST_CAPE:
                if (quest) return 28.0;
                return skill ? 6.0 : 0.0;
            case BARROWS_GLOVES:
                if (containsAny(identity, "recipe-for-disaster", "recipe for disaster")) return 38.0;
                if (quest) return 17.0;
                return skill ? 5.0 : 0.0;
            case PRIFDDINAS:
                if (containsAny(identity, "song-of-the-elves", "song of the elves")) return 42.0;
                if (quest) return 18.0;
                return skill ? 8.0 : 0.0;
            case BOWFA:
                if (containsAny(identity, "bowfa", "crystal", "gauntlet")) return 38.0;
                if (gear || pvm) return 15.0;
                return quest ? 8.0 : 0.0;
            case INFERNAL_CAPE:
                if (containsAny(identity, "inferno", "infernal")) return 45.0;
                return gear || pvm ? 20.0 : 0.0;
            case DIARY_CAPE:
                if (diary) return 30.0;
                return skill || quest ? 8.0 : 0.0;
            case ELITE_COMBAT_ACHIEVEMENTS:
                if (ca) return 32.0;
                return pvm || gear ? 16.0 : 0.0;
            case RAID_READY:
                if (pvm || gear) return 24.0;
                if (id.startsWith("skill:slayer") || id.startsWith("skill:prayer")
                        || id.startsWith("skill:magic") || id.startsWith("skill:ranged")) return 11.0;
                return 0.0;
            case TOTAL_2000:
                return skill ? 19.0 : 0.0;
            case SLAYER_85:
                if (id.startsWith("skill:slayer")) return 45.0;
                return containsAny(identity, "whip", "slayer") ? 20.0 : 0.0;
            case BASE_70S:
                if (skill && recommendation.getCurrentLevel() > 0 && recommendation.getCurrentLevel() < 70)
                    return 23.0 + Math.min(8.0, (70 - recommendation.getCurrentLevel()) * 0.15);
                return 0.0;
            case GEAR_TARGET:
                return gear ? 35.0 : pvm ? 15.0 : 0.0;
            case CUSTOM:
            default:
                return 0.0;
        }
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
