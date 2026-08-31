package compass;

import java.util.*;
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
public class QuestCandidateProvider implements CandidateProvider
{
    private final QuestPriorityCatalog priorityCatalog;
    private final QuestKnowledgeCatalog knowledgeCatalog;
    private final QuestRequirementResolver requirementResolver;
    private final GoalDependencyProvenanceService goalProvenanceService;

    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog)
    {
        this(priorityCatalog, new QuestKnowledgeCatalog(),
                new QuestRequirementResolver(),
                new GoalDependencyProvenanceService());
    }

    @Inject
    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog,
            QuestKnowledgeCatalog knowledgeCatalog,
            QuestRequirementResolver requirementResolver,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this.priorityCatalog = priorityCatalog;
        this.knowledgeCatalog = knowledgeCatalog;
        this.requirementResolver = requirementResolver;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
    }

    /** Compatibility constructor retained for focused tests. */
    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog,
            QuestKnowledgeCatalog knowledgeCatalog,
            QuestRequirementResolver requirementResolver)
    {
        this(priorityCatalog, knowledgeCatalog, requirementResolver,
                new GoalDependencyProvenanceService());
    }

    @Override
    public String getId()
    {
        return "quest-candidates";
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().quests() == null
                || context.data().account() == null)
        {
            return result;
        }

        var account = context.data().account();
        var membership = account.membership();
        var preferences = context.preferenceProfile();
        Set<String> neededPrerequisites = neededPrerequisites(
                context.data().quests());
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
        {
            var status = entry.getValue();
            if (status == null || status == QuestStatus.COMPLETE
                    || status == QuestStatus.UNKNOWN)
            {
                continue;
            }

            var questName = entry.getKey();
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

            var id = "quest:" + slug(questName);
            if (preferences.isOnCooldown(id)) continue;

            QuestPriorityCatalog.QuestPriority priority =
                    priorityCatalog.priorityFor(questName);
            var definition = knowledgeCatalog.definitionFor(questName);
            QuestResolution resolution = definition == null ? null
                    : requirementResolver.resolve(definition, context);
            boolean requiredForGoal = goalProvenanceService.isRequiredQuest(
                    context.goal(), questName, context);
            double score = requiredForGoal ? 42.0
                    : baseScore(context.getQuestTolerance());
            String reason;

            if (status == QuestStatus.IN_PROGRESS)
            {
                score += 12.0;
                reason = Text.get(546);
            }
            else
            {
                score -= 7.0;
                reason = Text.get(547);
            }

            var build = AccountBuildPolicy.effectiveBuild(account);
            if (build != RestrictedBuildType.STANDARD)
            {
                reason += Text.get(548)
                        + AccountBuildPolicy.label(account) + " build.";
            }

            if (priority != null)
            {
                score += priority.getScoreBonus();
                reason += " " + priority.getReason() + ".";
            }

            if (neededPrerequisites.contains(Names.words(questName)))
            {
                score += 24.0;
                reason += Text.get(549);
            }

            if (requiredForGoal)
                reason += Text.get(550);

            score += preferences.weightFor(id) * 10.0;
            score += preferences.timedScoreAdjustmentFor(id);

            Confidence confidence = resolution == null
                    ? Confidence.CHECK_NEEDED
                    : resolution.getConfidence();
            Guidance guidance = resolution == null ? null
                    : resolution.getGuidance();
            if (resolution != null) reason += " " + resolution.getReason() + ".";

            String title = (status == QuestStatus.IN_PROGRESS ? "Continue " : "Quest: ")
                    + questName;
            if (resolution != null
                    && resolution.getConfidence() == Confidence.CHECK_NEEDED
                    && guidance != null && guidance.getAction() != null
                    && !guidance.getAction().trim().isEmpty())
                title = "Prepare for " + questName + ": "
                        + guidance.getAction().replaceFirst("\\.$", "");
            result.add(new Recommendation(
                    id,
                    title,
                    reason,
                    score,
                    confidence,
                    guidance,
                    resolution == null
                            ? SafetyEvidence.unknown()
                            : resolution.getSafetyEvidence()
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
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
        for (Map.Entry<String, QuestStatus> entry : quests.quests().entrySet())
        {
            if (entry.getValue() == QuestStatus.COMPLETE) continue;
            var definition = knowledgeCatalog.definitionFor(entry.getKey());
            if (definition == null) continue;
            for (String prerequisite : definition.getPrerequisites())
                if (quests.statusOf(prerequisite) != QuestStatus.COMPLETE)
                    result.add(Names.words(prerequisite));
        }
        return result;
    }

}
