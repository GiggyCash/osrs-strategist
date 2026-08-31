package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Values exact deterministic method inputs through sustainable pipelines. */
@Singleton
public final class MethodResourceValueService
{
    private final RuneLiteSkillActionCatalog actions;
    private final MethodExecutionProfileCatalog profiles =
            new MethodExecutionProfileCatalog();
    private final SkillingXpModifierService modifiers =
            new SkillingXpModifierService();
    private final AdaptiveActionSelector selector =
            new AdaptiveActionSelector();
    private final MethodInputResolver inputs = new MethodInputResolver();
    private final ResourcePipelinePolicyCatalog policies =
            new ResourcePipelinePolicyCatalog();
    private final SustainableResourceValueService resources =
            new SustainableResourceValueService();
    private final GimGroupStrategyService groupStrategy =
            new GimGroupStrategyService();

    @Inject
    public MethodResourceValueService(RuneLiteSkillActionCatalog actions)
    {
        this.actions = actions == null
                ? new RuneLiteSkillActionCatalog() : actions;
    }

    public MethodResourceValueService()
    {
        this(new RuneLiteSkillActionCatalog());
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        TrainingPlan plan = recommendation == null
                ? null : recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        if (method == null || method.getSkill() == null || context == null
                || context.getData() == null
                || context.getData().getAccount() == null
                || recommendation.getTargetLevel() <= 0)
            return recommendation;
        MethodExecutionProfile profile = profiles.forMethod(method.getId());
        if (profile == null) return recommendation;

        AccountSnapshot account = context.getData().getAccount();
        Skill skill = method.getSkill();
        int currentXp = account.getSkillExperience(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(
                    account.getSkillLevel(skill));
        int targetXp = Experience.getXpForLevel(
                recommendation.getTargetLevel());
        double multiplier = profile.getXpMultiplier()
                * modifiers.modifier(context.getData(), skill,
                        context.isUseGroupStorage()).getMultiplier();
        RuneLiteSkillActionDefinition action = selector.select(
                context.getData(), profile, actions.actionsFor(skill),
                account.getSkillLevel(skill), account.getMembershipStatus(),
                currentXp, targetXp, multiplier,
                context.isUseGroupStorage());
        if (action == null || action.getXp() <= 0) return recommendation;
        int count = (int) Math.ceil(Math.max(0, targetXp - currentXp)
                / (action.getXp() * multiplier));
        List<ResourcePipelineRequest> requests = new ArrayList<>();
        RecommendationStrategicValue sharedValue =
                RecommendationStrategicValue.neutral();
        for (ResolvedMethodInput input : inputs.resolve(profile, action, count))
        {
            ResourcePipelinePolicy policy = policies.forInput(input.getName());
            if (policy == null) continue;
            requests.add(new ResourcePipelineRequest(
                    new ResourceNeed(input.getItemId(), input.getName(),
                            input.getQuantity()),
                    policy.getUseKind(), policy.getScarcity(),
                    policy.isTradeable()));
            Set<Integer> sharedIds = observedGroupItemIds(context,
                    input.getName());
            if (!sharedIds.isEmpty())
            {
                GroupResourceAssessment shared = groupStrategy.assess(context,
                        new GroupResourceNeed(input.getName(), sharedIds,
                                input.getQuantity(), policy.getUseKind()
                                == ResourceUseKind.REUSABLE));
                sharedValue = sharedValue.merge(shared.strategicValue(
                        "group-resource:" + input.getName()
                                .toLowerCase(Locale.ROOT)));
            }
        }
        if (requests.isEmpty()) return recommendation;
        ResourcePortfolioAssessment assessment = resources.assessAll(
                context, requests);
        RecommendationStrategicValue resourceValue =
                RecommendationStrategicValue.builder()
                        .resourceFit(assessment.getScoreAdjustment() / 12.0)
                        .evidence("resource-pipeline:" + method.getId())
                        .build()
                        .merge(sharedValue);
        return recommendation.withStrategicValue(
                recommendation.getStrategicValue().merge(resourceValue));
    }

    private static Set<Integer> observedGroupItemIds(StrategyContext context,
            String itemName)
    {
        if (context == null || !context.getAccountMode().isGroupIronman()
                || !context.isUseGroupStorage()
                || context.getData() == null
                || context.getData().getGroupStorage() == null
                || !context.getData().getGroupStorage().isObserved())
            return Collections.emptySet();
        String target = normalize(itemName);
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemStackSnapshot item
                : context.getData().getGroupStorage().getItems())
            if (item != null && item.getQuantity() > 0
                    && item.getItemId() > 0
                    && target.equals(normalize(item.getName())))
                ids.add(item.getItemId());
        return ids;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
