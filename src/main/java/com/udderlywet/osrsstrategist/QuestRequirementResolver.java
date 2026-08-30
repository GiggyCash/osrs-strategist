package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Resolves only evidence represented by the current local snapshots. */
@Singleton
public class QuestRequirementResolver
{
    private static final String IMPORTED_ITEM_PREFIX = "Required items:";
    private final ImportedQuestItemRequirementCatalog importedItems;
    private final ResourceSourceCatalog resourceSources;
    private final ResourceAcquisitionPlanner resourcePlanner;

    @Inject
    public QuestRequirementResolver(ResourceSourceCatalog resourceSources,
            ResourceAcquisitionPlanner resourcePlanner)
    {
        this.importedItems = new ImportedQuestItemRequirementCatalog();
        this.resourceSources = resourceSources == null
                ? new ResourceSourceCatalog() : resourceSources;
        this.resourcePlanner = resourcePlanner == null
                ? new ResourceAcquisitionPlanner(this.resourceSources)
                : resourcePlanner;
    }

    public QuestRequirementResolver(ResourceSourceCatalog resourceSources)
    {
        this(resourceSources, new ResourceAcquisitionPlanner(resourceSources));
    }

    /** Compatibility constructor for focused tests and local tooling. */
    public QuestRequirementResolver()
    {
        this(new ResourceSourceCatalog());
    }

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
        boolean ownershipObserved = items.usableOwnershipObserved();
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

        QuestItemEvidenceParser.Result imported = hasImportedItemEvidence(definition)
                ? importedItems.resultFor(definition.getName()) : null;
        ItemRequirementExpression expression = definition.getItemRequirementExpression();
        if (expression == null && imported != null)
            expression = imported.getExpression();
        ItemRequirementResult expressionResult = new ItemRequirementEvaluator()
                .evaluate(expression, data, context.isUseGroupStorage());
        if (!expressionResult.isSatisfied()
                && !expressionResult.getAction().isEmpty())
        {
            missing.add(itemPreparation(expressionResult, definition, context));
        }

        if (definition.getQuestPointsRequired() > 0)
            missing.add(new Preparation("Verify at least "
                    + definition.getQuestPointsRequired() + " quest points",
                    CandidateSafetyEvidence.harmless(definition.isFreeToPlay())));
        for (String check : definition.getAccessChecks())
        {
            if (check != null && check.startsWith(IMPORTED_ITEM_PREFIX))
            {
                if (imported == null)
                {
                    missing.add(new Preparation(check,
                            CandidateSafetyEvidence.harmless(
                                    definition.isFreeToPlay())));
                }
                else
                {
                    for (String unresolved : imported.getUnresolved())
                        missing.add(new Preparation(
                                "Verify quest item requirement: " + unresolved,
                                CandidateSafetyEvidence.harmless(
                                        definition.isFreeToPlay())));
                }
                continue;
            }
            missing.add(new Preparation(check,
                    CandidateSafetyEvidence.harmless(definition.isFreeToPlay())));
        }

        String unlocks = definition.getUnlocks().isEmpty() ? ""
                : String.join(", ", definition.getUnlocks());
        if (missing.isEmpty())
        {
            return new QuestResolution(RecommendationConfidence.VERIFIED,
                    new RecommendationGuidance(
                            "Start " + definition.getName() + ".",
                            "Requirements satisfied. Use Quest Helper for the walkthrough.",
                            definition.getStartLocation(),
                            unlocks.isEmpty() ? "You have everything required by the available quest data."
                                    : "Progression unlocked: " + unlocks + "."),
                    "Requirements satisfied",
                    CandidateSafetyEvidence.verifiedSafe(
                            definition.isFreeToPlay()));
        }

        List<String> missingText = new ArrayList<>();
        for (Preparation preparation : missing) missingText.add(preparation.detail);
        return new QuestResolution(RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(missing.get(0).text + ".",
                        String.join("; ", missingText), definition.getStartLocation(),
                        unlocks.isEmpty()
                                ? "Resolve the listed requirement before starting."
                                : "Resolving this path unlocks: " + unlocks + "."),
                "Preparation required: " + missing.get(0).text,
                missing.get(0).safetyEvidence);
    }

    private Preparation itemPreparation(ItemRequirementResult result,
            QuestDefinition definition, StrategyContext context)
    {
        CandidateSafetyEvidence safety = CandidateSafetyEvidence.harmless(
                definition.isFreeToPlay());
        if (result.getMissingInputs().isEmpty())
            return new Preparation(result.getAction(), safety);

        ResolvedMethodInput first = result.getMissingInputs().get(0);
        ResourceDependencyResolution dependency = dependencyResolution(context, first);
        ResolvedDependencyNode next = dependency == null ? null : dependency.nextAction();
        if (next != null
                && next.getConfidence() != RecommendationConfidence.VERIFIED
                && next.getAction() != null
                && !next.getAction().trim().isEmpty())
        {
            String action = withoutTerminalPeriod(next.getAction().trim());
            StringBuilder detail = new StringBuilder(result.getAction());
            detail.append(". Confirmed shortfall: ")
                    .append(formatInputs(result.getMissingInputs())).append(".");
            detail.append(" Dependency first step for ")
                    .append(quantity(first)).append(": ")
                    .append(next.getAction().trim());
            if (result.getMissingInputs().size() > 1)
                detail.append(" Resolve the remaining confirmed item shortfalls after this first step.");
            return new Preparation(action, detail.toString(), safety);
        }

        AccountMode mode = context.getAccountMode();
        String action;
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            action = "Acquire " + quantity(first) + " just in time";
        else if (mode.isIronLike())
            action = "Self-source " + quantity(first);
        else if (mode == AccountMode.UNKNOWN)
            action = "Source " + quantity(first) + " after verifying account mode";
        else
            action = "Acquire " + quantity(first);

        List<String> routes = sourceRoutes(context, first.getName());
        if (!routes.isEmpty()) action += ": " + routes.get(0);

        StringBuilder detail = new StringBuilder(result.getAction());
        detail.append(". Confirmed shortfall: ")
                .append(formatInputs(result.getMissingInputs())).append(".");
        if (!routes.isEmpty())
            detail.append(" Suggested route: ").append(routes.get(0));
        if (result.getMissingInputs().size() > 1)
            detail.append(" Resolve the remaining confirmed item shortfalls after this first step.");
        return new Preparation(action, detail.toString(), safety);
    }

    private ResourceDependencyResolution dependencyResolution(
            StrategyContext context, ResolvedMethodInput input)
    {
        if (context == null || input == null || context.getData() == null
                || context.getData().getAccount() == null)
            return null;
        return resourcePlanner.resolveKnownShortfall(
                context, input.getName(), input.getQuantity());
    }

    private List<String> sourceRoutes(StrategyContext context, String itemName)
    {
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null)
            return java.util.Collections.emptyList();
        return resourceSources.suggestions(itemName, context.getAccountMode(),
                context.getData().getAccount().getMembershipStatus(),
                context.isAllowWildernessMethods());
    }

    private static String quantity(ResolvedMethodInput input)
    {
        return Math.max(1, input.getQuantity()) + " × " + input.getName();
    }

    private static String formatInputs(List<ResolvedMethodInput> inputs)
    {
        List<String> values = new ArrayList<>();
        for (ResolvedMethodInput input : inputs) values.add(quantity(input));
        return String.join(", ", values);
    }

    private static String withoutTerminalPeriod(String value)
    {
        if (value == null) return "";
        String result = value.trim();
        while (result.endsWith("."))
            result = result.substring(0, result.length() - 1).trim();
        return result;
    }

    private static boolean hasImportedItemEvidence(QuestDefinition definition)
    {
        for (String check : definition.getAccessChecks())
            if (check != null && check.startsWith(IMPORTED_ITEM_PREFIX)) return true;
        return false;
    }

    private static final class Preparation
    {
        private final String text;
        private final String detail;
        private final CandidateSafetyEvidence safetyEvidence;

        private Preparation(String text,
                CandidateSafetyEvidence safetyEvidence)
        {
            this(text, text, safetyEvidence);
        }

        private Preparation(String text, String detail,
                CandidateSafetyEvidence safetyEvidence)
        {
            this.text = text;
            this.detail = detail == null ? text : detail;
            this.safetyEvidence = safetyEvidence;
        }
    }
}
