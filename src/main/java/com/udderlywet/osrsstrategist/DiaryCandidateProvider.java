package com.udderlywet.osrsstrategist;

import java.util.*;
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
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
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

            double score = tierScore(next);
            if (context.getActiveGoal() == GoalType.DIARY_CAPE) score += 20.0;
            int observedTasks = diaries.completedIn(region);
            score += Math.min(8.0, observedTasks * 0.15);
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            List<DiaryTaskDefinition> tierTasks = taskCatalog.forTier(region, next);
            DiaryTaskDefinition ready = firstReadyIncomplete(
                    tierTasks, diaries, context);
            boolean tierObserved = tierTasks.stream().anyMatch(task ->
                    diaries.taskCompletion(task.getId()) != null);
            if (ready == null && tierObserved) continue;

            if (!tierObserved)
            {
                String verifyId = "verify:" + id;
                if (context.getPreferenceProfile().isOnCooldown(verifyId))
                    continue;
                result.add(new Recommendation(
                        verifyId,
                        "Check " + pretty(next.name()) + " " + region + " Diary",
                        PlayerText.get("DCP1"),
                        score,
                        RecommendationConfidence.CHECK_NEEDED,
                        new RecommendationGuidance(
                                "Open the " + region + PlayerText.get("DCP2"),
                                PlayerText.get("DCP3"),
                                "Quest tab → Achievement Diaries → " + region + ".",
                                PlayerText.get("DCP4")),
                        CandidateSafetyEvidence.harmless(false)
                ));
                continue;
            }

            if (context.getPreferenceProfile().isOnCooldown(ready.getId()))
                continue;

            result.add(new Recommendation(
                    ready.getId(),
                    "Complete a " + pretty(next.name()) + " " + region + " task",
                    PlayerText.get("DCP5"),
                    score,
                    RecommendationConfidence.VERIFIED,
                    new RecommendationGuidance(
                            ready.getTask(),
                            requirementSummary(ready),
                            region + PlayerText.get("DCP6"),
                            PlayerText.get("DCP7")),
                    CandidateSafetyEvidence.potentiallyIrreversible(false)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 5) return new ArrayList<>(result.subList(0, 5));
        return result;
    }

    private static DiaryTaskDefinition firstReadyIncomplete(
            List<DiaryTaskDefinition> tasks, DiarySnapshot snapshot,
            StrategyContext context)
    {
        for (DiaryTaskDefinition task : tasks)
            if (Boolean.FALSE.equals(snapshot.taskCompletion(task.getId()))
                    && requirementsMet(task, context)) return task;
        return null;
    }

    private static boolean requirementsMet(DiaryTaskDefinition task,
            StrategyContext context)
    {
        AccountSnapshot account = context.getData().getAccount();
        QuestSnapshot quests = context.getData().getQuests();
        for (DiaryTaskRequirement requirement : task.getRequirements())
        {
            switch (requirement.getKind())
            {
                case SKILL:
                    if (account.getSkillLevel(requirement.getSkill())
                            < requirement.getLevel()) return false;
                    break;
                case QUEST:
                    QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                            : quests.statusOf(requirement.getQuest());
                    if (status != QuestStatus.COMPLETE
                            && !(requirement.isStartedOnly()
                            && status == QuestStatus.IN_PROGRESS)) return false;
                    break;
                case COMBAT_LEVEL:
                case QUEST_POINTS:
                case ALTERNATIVE_CHECK:
                default:
                    return false;
            }
        }
        return true;
    }

    private static String requirementSummary(DiaryTaskDefinition task)
    {
        List<String> values = new ArrayList<>();
        for (DiaryTaskRequirement requirement : task.getRequirements())
        {
            if (requirement.getKind() == DiaryTaskRequirement.Kind.SKILL)
                values.add(requirement.getLevel() + " "
                        + requirement.getSkill().getName());
            else if (requirement.getKind() == DiaryTaskRequirement.Kind.QUEST)
                values.add(requirement.getQuest()
                        + (requirement.isStartedOnly() ? " started" : " complete"));
        }
        return values.isEmpty()
                ? PlayerText.get("DCP8")
                : "Verified: " + String.join(", ", values) + ".";
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
