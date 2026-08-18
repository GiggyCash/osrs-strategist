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
        List<Preparation> missing = new ArrayList<>();

        for (String prerequisite : definition.getPrerequisites())
        {
            QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                    : quests.statusOf(prerequisite);
            if (status != QuestStatus.COMPLETE)
                missing.add(new Preparation(status == QuestStatus.UNKNOWN
                        ? "Verify and complete prerequisite quest: " + prerequisite
                        : "Complete prerequisite quest: " + prerequisite,
                        RestrictedQuestPolicy.isSafe(account, prerequisite)
                                ? CandidateSafetyEvidence.verifiedSafe(
                                definition.isFreeToPlay())
                                : CandidateSafetyEvidence.potentiallyIrreversible(
                                definition.isFreeToPlay())));
        }

        for (Map.Entry<Skill, Integer> requirement
                : definition.getSkillRequirements().entrySet())
        {
            int current = account.getSkillLevel(requirement.getKey());
            if (current < requirement.getValue())
                missing.add(new Preparation("Train "
                        + requirement.getKey().getName() + " from " + current
                        + " to " + requirement.getValue(),
                        CandidateSafetyEvidence.skill(definition.isFreeToPlay(),
                                requirement.getKey())));
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
                missing.add(new Preparation((ownershipObserved ? "Obtain " : "Verify ownership of ")
                        + Math.max(0, requirement.getQuantity() - owned) + " × "
                        + requirement.getName(), CandidateSafetyEvidence.harmless(
                        definition.isFreeToPlay())));
        }

        ItemRequirementResult expressionResult = new ItemRequirementEvaluator()
                .evaluate(definition.getItemRequirementExpression(), data,
                        context.isUseGroupStorage());
        if (!expressionResult.isSatisfied()
                && !expressionResult.getAction().isEmpty())
            missing.add(new Preparation(expressionResult.getAction(),
                    CandidateSafetyEvidence.harmless(definition.isFreeToPlay())));

        if (definition.getQuestPointsRequired() > 0)
            missing.add(new Preparation("Verify at least "
                    + definition.getQuestPointsRequired() + " quest points",
                    CandidateSafetyEvidence.harmless(definition.isFreeToPlay())));
        for (String check : definition.getAccessChecks())
            missing.add(new Preparation(check,
                    CandidateSafetyEvidence.harmless(definition.isFreeToPlay())));

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
                    "All modeled requirements are verified",
                    CandidateSafetyEvidence.verifiedSafe(
                            definition.isFreeToPlay()));
        }

        List<String> missingText = new ArrayList<>();
        for (Preparation preparation : missing) missingText.add(preparation.text);
        return new QuestResolution(RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(missing.get(0).text + ".",
                        String.join("; ", missingText), definition.getStartLocation(),
                        unlocks.isEmpty()
                                ? "Resolve the listed requirement before starting."
                                : "Resolving this path unlocks: " + unlocks + "."),
                "Preparation required: " + missing.get(0).text,
                missing.get(0).safetyEvidence);
    }

    private static final class Preparation
    {
        private final String text;
        private final CandidateSafetyEvidence safetyEvidence;

        private Preparation(String text,
                CandidateSafetyEvidence safetyEvidence)
        {
            this.text = text;
            this.safetyEvidence = safetyEvidence;
        }
    }
}
