package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;

/** Ranks the next unfinished tier across all 12 Achievement Diary regions. */
@Singleton
public class DiaryCandidateProvider implements StrategyCandidateProvider
{
    private final DiaryTaskCatalog taskCatalog = new DiaryTaskCatalog();
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
                || !ContentAccessRules.hasVerifiedMembership(
                        context.getData().getAccount().getMembershipStatus()))
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

            List<DiaryTaskDefinition> tierTasks = taskCatalog.forTier(region, next);
            String firstCheck = tierTasks.isEmpty()
                    ? "Open the diary interface and inspect the incomplete tasks."
                    : "Open the diary interface and check whether this task is incomplete: "
                            + tierTasks.get(0).getTask();
            result.add(new StrategyCandidate(
                    id,
                    pretty(next.name()) + " " + region + " Diary",
                    "This is the next unclaimed tier in " + region + ". "
                            + tierTasks.size() + " current task definitions provide skill, quest, activity, and transport prerequisite evidence.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED,
                    new RecommendationGuidance(firstCheck,
                            "After identifying the first incomplete task, resolve its structured skill/quest prerequisite before gathering task-specific items.",
                            "Use the task location shown in the in-game diary and verified transport state.",
                            "Per-task completion is not inferred from the tier count."),
                    CandidateSafetyEvidence.potentiallyIrreversible(false)
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
