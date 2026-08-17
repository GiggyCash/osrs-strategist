package com.udderlywet.osrsstrategist;

import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Global value model used after every skill, quest, upgrade, detour, gear path,
 * minigame, and PvM candidate has entered the same pool.
 *
 * <p>Individual providers remain responsible for legality and local method
 * quality. This service answers the higher-level question: which legal,
 * actionable opportunity matters most for this account right now? The model is
 * intentionally additive and inspectable so a future premium/online reasoning
 * layer can explain or tune the same dimensions without replacing the local
 * safety gates.</p>
 */
@Singleton
public class RecommendationIntelligenceService
{
    /**
     * Returns the final ranking score. Raw provider score remains important, but
     * cannot by itself represent goal alignment, readiness, session friction,
     * account-mode practicality, and cross-account risk.
     */
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
        score += safetyValue(recommendation, context, id, title, reason);
        score += resourceValue(guidance, context.getAccountMode());
        score += opportunityCostValue(recommendation, context, id, reason);

        // Explicit player feedback is the final soft signal. Cooldowns are hard
        // filtered by providers; weights/timed adjustments can still move close
        // calls without overriding build or membership legality.
        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences != null && recommendation.getId() != null)
        {
            score += preferences.weightFor(recommendation.getId()) * 8.0;
            score += preferences.timedScoreAdjustmentFor(recommendation.getId());
        }
        return score;
    }

    private static double readinessValue(
            Recommendation recommendation,
            RecommendationGuidance guidance)
    {
        double value = 0.0;
        if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED)
        {
            value += 7.0;
        }
        else if (recommendation.getConfidence() == RecommendationConfidence.CHECK_NEEDED)
        {
            value -= 9.0;
        }
        else if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
        {
            return -10_000.0;
        }

        if (guidance != null && hasText(guidance.getAction())) value += 5.0;
        if (guidance != null && hasText(guidance.getLocation())) value += 2.0;
        if (guidance != null && hasText(guidance.getSupplies())) value += 2.0;
        return value;
    }

    private static double goalValue(
            Recommendation recommendation,
            StrategyContext context,
            String id,
            String title)
    {
        GoalType goal = context.getActiveGoal();
        if (goal == null) goal = GoalType.MAX;
        boolean skill = id.startsWith("skill:");
        boolean quest = id.startsWith("quest:");
        boolean gear = id.startsWith("gear:") || id.startsWith("upgrade:");
        boolean pvm = id.startsWith("pvm:");
        boolean diary = id.startsWith("diary:");
        boolean ca = id.startsWith("combat-achievement:")
                || id.startsWith("combat_achievement:");

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
                if (skill) return 6.0;
                return 0.0;
            case BARROWS_GLOVES:
                if (contains(id, title, "recipe-for-disaster", "recipe for disaster")) return 38.0;
                if (quest) return 17.0;
                if (skill) return 5.0;
                return 0.0;
            case PRIFDDINAS:
                if (contains(id, title, "song-of-the-elves", "song of the elves")) return 42.0;
                if (quest) return 18.0;
                if (skill) return 8.0;
                return 0.0;
            case BOWFA:
                if (contains(id, title, "bowfa", "crystal", "gauntlet")) return 38.0;
                if (gear || pvm) return 15.0;
                if (quest) return 8.0;
                return 0.0;
            case INFERNAL_CAPE:
                if (contains(id, title, "inferno", "infernal")) return 45.0;
                if (gear || pvm) return 20.0;
                return 0.0;
            case DIARY_CAPE:
                if (diary) return 30.0;
                if (skill || quest) return 8.0;
                return 0.0;
            case ELITE_COMBAT_ACHIEVEMENTS:
                if (ca) return 32.0;
                if (pvm || gear) return 16.0;
                return 0.0;
            case RAID_READY:
                if (pvm || gear) return 24.0;
                if (id.startsWith("skill:slayer")
                        || id.startsWith("skill:prayer")
                        || id.startsWith("skill:magic")
                        || id.startsWith("skill:ranged")) return 11.0;
                return 0.0;
            case TOTAL_2000:
                return skill ? 19.0 : 0.0;
            case SLAYER_85:
                if (id.startsWith("skill:slayer")) return 45.0;
                if (contains(id, title, "whip", "slayer")) return 20.0;
                return 0.0;
            case BASE_70S:
                if (skill && recommendation.getCurrentLevel() > 0
                        && recommendation.getCurrentLevel() < 70)
                {
                    return 23.0 + Math.min(8.0,
                            (70 - recommendation.getCurrentLevel()) * 0.15);
                }
                return 0.0;
            case GEAR_TARGET:
                return gear ? 35.0 : pvm ? 15.0 : 0.0;
            case CUSTOM:
            default:
                return 0.0;
        }
    }

    private static double sessionValue(
            Recommendation recommendation,
            SessionIntent intent)
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
                if (minimum > 90) return -5.0;
                return 0.0;
            case LONG_SESSION:
                if (minimum >= 30) return 4.0;
                return 1.0;
            case AFK:
                AttentionLevel attention = method.getAttentionLevel();
                if (attention == AttentionLevel.AFK) return 12.0;
                if (attention == AttentionLevel.LOW) return 7.0;
                if (attention == AttentionLevel.ACTIVE) return -9.0;
                return 0.0;
            case PICK_FOR_ME:
            default:
                return 0.0;
        }
    }

    private static double accountModeValue(
            Recommendation recommendation,
            StrategyContext context,
            String id,
            String title,
            String reason)
    {
        AccountMode mode = context.getAccountMode();
        TrainingPlan plan = recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        double value = 0.0;

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            // UIM setup destruction is real account cost. Prefer routes with a
            // light setup unless the candidate is explicitly UIM-oriented.
            if (method != null)
            {
                int setup = Math.max(0, method.getSetupMinutes());
                if (setup <= 3) value += 8.0;
                else if (setup >= 12) value -= 14.0;
                else if (setup >= 7) value -= 5.0;
            }
            if (id.startsWith("detour:") && !contains(id, title, reason, "uim")) value -= 8.0;
            if (contains(reason, "bank", "grand exchange")) value -= 12.0;
        }
        else if (mode.isIronLike())
        {
            if (id.startsWith("detour:")) value += 9.0;
            if (id.startsWith("upgrade:")) value += 5.0;
            if (contains(reason, "self-source", "self source", "supplies")) value += 4.0;
            if (contains(reason, "grand exchange")) value -= 15.0;
        }
        else if (mode.usesGrandExchange())
        {
            if (id.startsWith("money:")) value += 4.0;
            // Resource detours generally have less value to a tradeable main
            // unless the activity itself is a goal.
            if (id.startsWith("detour:")) value -= 3.0;
        }
        return value;
    }

    private static double safetyValue(
            Recommendation recommendation,
            StrategyContext context,
            String id,
            String title,
            String reason)
    {
        double value = 0.0;
        boolean wilderness = contains(id, title, reason,
                "wilderness", "wildy", "revenant", "revs");
        if (wilderness && !context.isAllowWildernessMethods()) return -5_000.0;

        AccountMode mode = context.getAccountMode();
        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN) && wilderness)
        {
            return -7_500.0;
        }
        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && id.startsWith("pvm:")
                && contains(reason, "dangerous", "high risk"))
        {
            value -= 20.0;
        }
        return value;
    }

    private static double resourceValue(
            RecommendationGuidance guidance,
            AccountMode mode)
    {
        if (guidance == null || !hasText(guidance.getSupplies())) return 0.0;
        String supplies = lower(guidance.getSupplies());
        double value = 0.0;

        if (contains(supplies, "verified:", "you own", "already have")) value += 5.0;
        if (contains(supplies, "open your bank", "needs bank", "bank once")) value -= 6.0;
        if (contains(supplies, "needs info", "not yet verified")) value -= 10.0;
        if (mode != null && mode.isIronLike()
                && contains(supplies, "self-source", "self source")) value -= 2.0;
        if (mode != null && mode.usesGrandExchange()
                && contains(supplies, "grand exchange")
                && !contains(supplies, "cannot afford", "short of")) value += 2.0;
        return value;
    }

    private static double opportunityCostValue(
            Recommendation recommendation,
            StrategyContext context,
            String id,
            String reason)
    {
        if (id.startsWith("detour:"))
        {
            // A detour should earn its place by doing at least two useful jobs.
            if (contains(reason, " + ", "while", "also", "cross-skill")) return 5.0;
            return -6.0;
        }

        if (id.contains("angler"))
        {
            AccountSnapshot account = context.getData().getAccount();
            int fishing = account.getSkillLevel(Skill.FISHING);
            if (fishing >= 90) return 5.0;
            if (fishing < 55 && context.getActiveGoal() != GoalType.MAX
                    && !context.isCollectionistMode()) return -8.0;
        }
        return 0.0;
    }

    private static boolean contains(String haystack, String... needles)
    {
        if (haystack == null) return false;
        for (String needle : needles)
        {
            if (needle != null && haystack.contains(lower(needle))) return true;
        }
        return false;
    }

    private static boolean contains(
            String first, String second, String... needles)
    {
        return contains(first + " " + second, needles);
    }

    private static boolean contains(
            String first, String second, String third, String... needles)
    {
        return contains(first + " " + second + " " + third, needles);
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
