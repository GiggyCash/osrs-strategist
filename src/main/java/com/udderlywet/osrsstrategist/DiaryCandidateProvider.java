package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;

/** Ranks the next unfinished tier across all 12 Achievement Diary regions. */
@Singleton
public class DiaryCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "diary-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getDiaries() == null
                || context.getData().getAccount() == null
                || context.getData().getAccount().getMembershipStatus() == MembershipStatus.F2P)
        {
            return result;
        }

        DiarySnapshot diaries = context.getData().getDiaries();
        for (String region : diaries.getRegions())
        {
            DiaryTier next = nextIncomplete(diaries, region);
            if (next == null) continue;
            if ("Wilderness".equals(region) && !context.isAllowWildernessMethods())
            {
                continue;
            }

            String id = "diary:" + region.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-") + ":"
                    + next.name().toLowerCase();
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;

            double score = tierScore(next);
            if (context.getActiveGoal() == GoalType.DIARY_CAPE) score += 20.0;
            int observedTasks = diaries.completedIn(region);
            score += Math.min(8.0, observedTasks * 0.15);
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            result.add(new StrategyCandidate(
                    id,
                    pretty(next.name()) + " " + region + " Diary",
                    "This is the next unclaimed tier in " + region
                            + ". Strategist has the live tier state and will treat individual skill, quest, item, and combat tasks as Check Needed until their requirements are verified.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 5) return new ArrayList<>(result.subList(0, 5));
        return result;
    }

    private static DiaryTier nextIncomplete(DiarySnapshot diaries, String region)
    {
        for (DiaryTier tier : DiaryTier.values())
            if (!diaries.isTierComplete(region, tier)) return tier;
        return null;
    }

    private static double tierScore(DiaryTier tier)
    {
        switch (tier)
        {
            case EASY: return 42.0;
            case MEDIUM: return 39.0;
            case HARD: return 34.0;
            case ELITE:
            default: return 29.0;
        }
    }

    private static String pretty(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
