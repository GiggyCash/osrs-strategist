package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Resolves only evidence represented by the current local snapshots. */
@Singleton
public class QuestRequirementResolver
{
    public QuestResolution resolve(QuestDefinition definition, StrategyContext context)
    {
        if (definition == null || context == null || context.getData() == null
                || context.getData().getAccount() == null) return null;

        StrategyDataBundle data = context.getData();
        AccountSnapshot account = data.getAccount();
        QuestSnapshot quests = data.getQuests();
        List<String> missing = new ArrayList<>();

        for (String prerequisite : definition.getPrerequisites())
        {
            QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                    : quests.statusOf(prerequisite);
            if (status != QuestStatus.COMPLETE)
                missing.add(status == QuestStatus.UNKNOWN
                        ? "Verify and complete prerequisite quest: " + prerequisite
                        : "Complete prerequisite quest: " + prerequisite);
        }

        for (Map.Entry<Skill, Integer> requirement
                : definition.getSkillRequirements().entrySet())
        {
            int current = account.getSkillLevel(requirement.getKey());
            if (current < requirement.getValue())
                missing.add("Train " + requirement.getKey().getName() + " from "
                        + current + " to " + requirement.getValue());
        }

        ObservedItemIndex items = new ObservedItemIndex(data,
                context.isUseGroupStorage());
        // An inventory observation does not prove that an unobserved bank is empty.
        boolean ownershipObserved = context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                || items.bankObserved();
        for (QuestDefinition.QuestItemRequirement requirement
                : definition.getItemRequirements())
        {
            int owned = items.quantity(requirement.getName());
            if (owned < requirement.getQuantity())
                missing.add((ownershipObserved ? "Obtain " : "Verify ownership of ")
                        + Math.max(0, requirement.getQuantity() - owned) + " × "
                        + requirement.getName());
        }

        if (definition.getQuestPointsRequired() > 0)
            missing.add("Verify at least " + definition.getQuestPointsRequired()
                    + " quest points");
        missing.addAll(definition.getAccessChecks());

        String unlocks = definition.getUnlocks().isEmpty() ? ""
                : String.join(", ", definition.getUnlocks());
        if (missing.isEmpty())
        {
            return new QuestResolution(RecommendationConfidence.VERIFIED,
                    new RecommendationGuidance(
                            "Start " + definition.getName() + ".",
                            "Requirements satisfied. Use Quest Helper for the walkthrough.",
                            definition.getStartLocation(),
                            unlocks.isEmpty() ? "The modeled requirements are verified."
                                    : "Progression unlocked: " + unlocks + "."),
                    "All modeled requirements are verified");
        }

        return new QuestResolution(RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(missing.get(0) + ".",
                        String.join("; ", missing), definition.getStartLocation(),
                        unlocks.isEmpty()
                                ? "Resolve the listed requirement before starting."
                                : "Resolving this path unlocks: " + unlocks + "."),
                "Preparation required: " + missing.get(0));
    }
}
