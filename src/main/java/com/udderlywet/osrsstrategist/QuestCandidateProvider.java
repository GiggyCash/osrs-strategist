package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Turns RuneLite's complete live quest-state snapshot into ranked quest work.
 * Not-started quests remain Check Needed until prerequisites are verified.
 */
@Singleton
public class QuestCandidateProvider implements StrategyCandidateProvider
{
    private final QuestPriorityCatalog priorityCatalog;

    @Inject
    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog)
    {
        this.priorityCatalog = priorityCatalog;
    }

    @Override
    public String getId()
    {
        return "quest-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getQuests() == null)
        {
            return result;
        }

        PreferenceProfile preferences = context.getPreferenceProfile();
        for (Map.Entry<String, QuestStatus> entry
                : context.getData().getQuests().getQuests().entrySet())
        {
            QuestStatus status = entry.getValue();
            if (status == null || status == QuestStatus.COMPLETE
                    || status == QuestStatus.UNKNOWN)
            {
                continue;
            }

            String questName = entry.getKey();
            String id = "quest:" + slug(questName);
            if (preferences.isOnCooldown(id)) continue;

            QuestPriorityCatalog.QuestPriority priority =
                    priorityCatalog.priorityFor(questName);
            double score = baseScore(context.getQuestTolerance());
            String reason;

            if (status == QuestStatus.IN_PROGRESS)
            {
                score += 12.0;
                reason = "This quest is already in progress, so finishing it avoids leaving partially completed progression behind.";
            }
            else
            {
                score -= 7.0;
                reason = "This quest is unfinished. Strategist will verify its skill, quest, item, and combat requirements before treating it as immediately ready.";
            }

            if (priority != null)
            {
                score += priority.getScoreBonus();
                reason += " " + priority.getReason() + ".";
            }

            if (context.getActiveGoal() == GoalType.QUEST_CAPE) score += 18.0;
            if (context.getActiveGoal() == GoalType.BARROWS_GLOVES
                    && "Recipe for Disaster".equalsIgnoreCase(questName)) score += 25.0;
            if (context.getActiveGoal() == GoalType.PRIFDDINAS
                    && "Song of the Elves".equalsIgnoreCase(questName)) score += 30.0;

            score += preferences.weightFor(id) * 10.0;
            score += preferences.timedScoreAdjustmentFor(id);

            result.add(new StrategyCandidate(
                    id,
                    (status == QuestStatus.IN_PROGRESS ? "Continue " : "Quest: ") + questName,
                    reason,
                    score,
                    RecommendationConfidence.CHECK_NEEDED
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        // The engine only needs the strongest quest options, not the entire quest
        // journal competing with every other activity on every evaluation.
        if (result.size() > 8)
        {
            return new ArrayList<>(result.subList(0, 8));
        }
        return result;
    }

    private static double baseScore(QuestTolerance tolerance)
    {
        if (tolerance == null) return 30.0;
        switch (tolerance)
        {
            case HIGH: return 47.0;
            case LOW: return 18.0;
            case NORMAL:
            default: return 31.0;
        }
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
