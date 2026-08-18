package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Turns RuneLite's complete live quest-state snapshot into ranked quest work.
 *
 * <p>Quest membership and restricted-build safety are hard gates. An unfinished
 * quest whose remaining prerequisites are not yet proven stays Check Needed and
 * therefore cannot occupy the primary DO NEXT slot.</p>
 */
@Singleton
public class QuestCandidateProvider implements StrategyCandidateProvider
{
    private final QuestPriorityCatalog priorityCatalog;
    private final QuestKnowledgeCatalog knowledgeCatalog;
    private final QuestRequirementResolver requirementResolver;

    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog)
    {
        this(priorityCatalog, new QuestKnowledgeCatalog(),
                new QuestRequirementResolver());
    }

    @Inject
    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog,
            QuestKnowledgeCatalog knowledgeCatalog,
            QuestRequirementResolver requirementResolver)
    {
        this.priorityCatalog = priorityCatalog;
        this.knowledgeCatalog = knowledgeCatalog;
        this.requirementResolver = requirementResolver;
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
                || context.getData().getQuests() == null
                || context.getData().getAccount() == null)
        {
            return result;
        }

        AccountSnapshot account = context.getData().getAccount();
        MembershipStatus membership = account.getMembershipStatus();
        PreferenceProfile preferences = context.getPreferenceProfile();
        Set<String> neededPrerequisites = neededPrerequisites(
                context.getData().getQuests());
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
            if (!QuestMembershipPolicy.isAvailable(questName, membership))
            {
                continue;
            }

            // Quest XP is irreversible. Restricted builds fail closed: a quest
            // that is not on the curated safe list never reaches the queue.
            if (!RestrictedQuestPolicy.isSafe(account, questName))
            {
                continue;
            }

            String id = "quest:" + slug(questName);
            if (preferences.isOnCooldown(id)) continue;

            QuestPriorityCatalog.QuestPriority priority =
                    priorityCatalog.priorityFor(questName);
            QuestDefinition definition = knowledgeCatalog.definitionFor(questName);
            QuestResolution resolution = definition == null ? null
                    : requirementResolver.resolve(definition, context);
            double score = baseScore(context.getQuestTolerance());
            String reason;

            if (status == QuestStatus.IN_PROGRESS)
            {
                score += 12.0;
                reason = "This quest is already in progress. It remains an alternative until the remaining step and requirements are observed.";
            }
            else
            {
                score -= 7.0;
                reason = "This quest is unfinished, but its requirements are not fully verified yet. It stays below ready actions until those checks are complete.";
            }

            RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
            if (build != RestrictedBuildType.STANDARD)
            {
                reason += " Its reward profile is on the safe list for this "
                        + AccountBuildPolicy.label(account) + " build.";
            }

            if (priority != null)
            {
                score += priority.getScoreBonus();
                reason += " " + priority.getReason() + ".";
            }

            if (neededPrerequisites.contains(normalize(questName)))
            {
                score += 24.0;
                reason += " It is a verified prerequisite for another unfinished quest.";
            }

            if (context.getActiveGoal() == GoalType.QUEST_CAPE) score += 18.0;
            if (context.getActiveGoal() == GoalType.BARROWS_GLOVES
                    && "Recipe for Disaster".equalsIgnoreCase(questName)) score += 25.0;
            if (context.getActiveGoal() == GoalType.PRIFDDINAS
                    && "Song of the Elves".equalsIgnoreCase(questName)) score += 30.0;

            score += preferences.weightFor(id) * 10.0;
            score += preferences.timedScoreAdjustmentFor(id);

            RecommendationConfidence confidence = resolution == null
                    ? RecommendationConfidence.CHECK_NEEDED
                    : resolution.getConfidence();
            RecommendationGuidance guidance = resolution == null ? null
                    : resolution.getGuidance();
            if (resolution != null) reason += " " + resolution.getReason() + ".";

            result.add(new StrategyCandidate(
                    id,
                    (status == QuestStatus.IN_PROGRESS ? "Continue " : "Quest: ") + questName,
                    reason,
                    score,
                    confidence,
                    guidance,
                    CandidateSafetyEvidence.verifiedSafe(
                            QuestMembershipPolicy.isFreeToPlayQuest(questName))
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
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

    private Set<String> neededPrerequisites(QuestSnapshot quests)
    {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, QuestStatus> entry : quests.getQuests().entrySet())
        {
            if (entry.getValue() == QuestStatus.COMPLETE) continue;
            QuestDefinition definition = knowledgeCatalog.definitionFor(entry.getKey());
            if (definition == null) continue;
            for (String prerequisite : definition.getPrerequisites())
                if (quests.statusOf(prerequisite) != QuestStatus.COMPLETE)
                    result.add(normalize(prerequisite));
        }
        return result;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}
