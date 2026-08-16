package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Makes every observed incomplete quest eligible for the shared queue. */
@Singleton
public class QuestCandidateProvider implements StrategyCandidateProvider
{
    private final QuestKnowledgeCatalog catalog;

    @Inject
    public QuestCandidateProvider(QuestKnowledgeCatalog catalog)
    {
        this.catalog = catalog;
    }

    @Override
    public String getId()
    {
        return "quests";
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

        QuestSnapshot quests = context.getData().getQuests();
        AccountSnapshot account = context.getData().getAccount();
        AccountMode mode = context.getAccountMode();
        boolean member = account != null
                && account.getMembershipStatus() != MembershipStatus.FREE_TO_PLAY;

        for (Map.Entry<String, QuestStatus> entry : quests.getQuests().entrySet())
        {
            if (entry.getValue() == QuestStatus.COMPLETE
                    || entry.getValue() == QuestStatus.UNKNOWN)
            {
                continue;
            }

            String observedName = entry.getKey();
            QuestKnowledgeDefinition definition = catalog.get(observedName);
            if (definition != null && definition.isMembersOnly() && !member)
            {
                continue;
            }

            double score = definition == null ? 4.0 : definition.getProgressionScore();
            RecommendationConfidence confidence = RecommendationConfidence.CHECK_NEEDED;
            String reason;

            if (definition == null)
            {
                reason = "RuneLite confirms this quest is incomplete. Strategist has not yet validated its full prerequisite/reward metadata, so deeper routing stays Check Needed.";
            }
            else
            {
                int missingPrerequisites = 0;
                for (String prerequisite : definition.getPrerequisiteQuests())
                {
                    if (!quests.isComplete(prerequisite)) missingPrerequisites++;
                }

                if (missingPrerequisites == 0)
                {
                    confidence = RecommendationConfidence.VERIFIED;
                    reason = definition.getUnlockSummary();
                }
                else
                {
                    score -= missingPrerequisites * 5.0;
                    reason = definition.getUnlockSummary() + ". "
                            + missingPrerequisites + " prerequisite quest(s) still need completion.";
                }

                if ((mode == AccountMode.HARDCORE_IRONMAN
                        || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                        && definition.isHardcoreRisky())
                {
                    score -= 8.0;
                    reason += " Hardcore safety: review combat/boss sections and escape supplies before starting.";
                }
            }

            if (context.getQuestTolerance() == QuestTolerance.HIGH) score += 7.0;
            else if (context.getQuestTolerance() == QuestTolerance.LOW) score -= 8.0;

            result.add(new StrategyCandidate(
                    "quest:" + observedName,
                    "Quest: " + displayName(observedName),
                    reason,
                    score,
                    confidence));
        }

        return result;
    }

    private static String displayName(String normalized)
    {
        if (normalized == null || normalized.isEmpty()) return "Quest";
        String[] words = normalized.split(" ");
        StringBuilder text = new StringBuilder();
        for (String word : words)
        {
            if (word.isEmpty()) continue;
            if (text.length() > 0) text.append(' ');
            text.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return text.toString();
    }
}
