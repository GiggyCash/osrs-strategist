package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Values deterministic method inputs without an intermediate resource DTO pipeline. */
@Singleton
public final class MethodResourceValueService
{
    private static final ResourceSourceCatalog SOURCES = new ResourceSourceCatalog();
    private final RuneLiteSkillActionCatalog actions;
    private final MethodExecutionProfileCatalog profiles =
            new MethodExecutionProfileCatalog();
    private final SkillingXpModifierService modifiers =
            new SkillingXpModifierService();
    private final AdaptiveActionSelector selector = new AdaptiveActionSelector();
    private final MethodInputResolver inputs = new MethodInputResolver();
    private final GimGroupStrategyService groupStrategy =
            new GimGroupStrategyService();

    @Inject
    public MethodResourceValueService(RuneLiteSkillActionCatalog actions)
    {
        this.actions = actions == null ? new RuneLiteSkillActionCatalog() : actions;
    }

    public MethodResourceValueService() { this(null); }

    public Recommendation attach(Recommendation recommendation,
            StrategyContext context)
    {
        TrainingPlan plan = recommendation == null ? null
                : recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        if (method == null || method.getSkill() == null || context == null
                || context.data() == null || context.data().account() == null
                || recommendation.getTargetLevel() <= 0) return recommendation;
        MethodProfile profile = profiles.forMethod(method.getId());
        if (profile == null) return recommendation;

        AccountSnapshot account = context.data().account();
        Skill skill = method.getSkill();
        int currentXp = account.getSkillExperience(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(account.getSkillLevel(skill));
        int targetXp = Experience.getXpForLevel(recommendation.getTargetLevel());
        double multiplier = profile.getXpMultiplier() * modifiers.modifier(
                context.data(), skill, context.isUseGroupStorage()).getMultiplier();
        ActionDef action = selector.select(context.data(), profile,
                actions.actionsFor(skill), account.getSkillLevel(skill),
                account.getMembershipStatus(), currentXp, targetXp, multiplier,
                context.isUseGroupStorage());
        if (action == null || action.getXp() <= 0) return recommendation;
        int count = (int) Math.ceil(Math.max(0, targetXp - currentXp)
                / (action.getXp() * multiplier));

        int score = 0;
        boolean known = false;
        StrategicValue shared = StrategicValue.neutral();
        for (MethodInput input : inputs.resolve(profile, action, count))
        {
            int[] policy = policy(input.getName());
            if (policy == null) continue;
            known = true;
            score += resourceAdjustment(context, input.getName(),
                    input.getQuantity(), policy[0], policy[1] == 1);
            Set<Integer> sharedIds = observedGroupItemIds(context, input.getName());
            if (!sharedIds.isEmpty())
                shared = shared.merge(groupStrategy.assess(context,
                        new GroupResourceNeed(input.getName(), sharedIds,
                                input.getQuantity(), false)).strategicValue(
                        "group-resource:" + input.getName().toLowerCase(Locale.ROOT)));
        }
        if (!known) return recommendation;
        StrategicValue value = StrategicValue.builder()
                .resourceFit(Math.max(-16, Math.min(6, score)) / 12.0)
                .evidence("resource-pipeline:" + method.getId()).build()
                .merge(shared);
        return recommendation.withStrategicValue(
                recommendation.getStrategicValue().merge(value));
    }

    /** Shared scoring primitive retained for regression coverage of mode safety. */
    static int resourceAdjustment(StrategyContext context, String name,
            int required, int scarcity, boolean tradeable)
    {
        if (context == null || context.data() == null || name == null) return -4;
        ItemIndex items = new ItemIndex(context.data(),
                context.isUseGroupStorage());
        int observed = items.quantity(name);
        AccountMode mode = context.accountMode();
        int burden = mode.usesGrandExchange() && tradeable ? 1
                : mode.isIronLike() ? 5 : 4;
        if (mode.isGroupIronman() && context.isUseGroupStorage()
                && items.groupStorageObserved()) burden--;
        if (mode == AccountMode.ULTIMATE_IRONMAN) burden += 2;
        if (observed >= Math.max(1, required))
            return Math.max(-10, Math.min(4, 4 - burden - scarcity));
        if (!items.resourceContainersObserved()) return -2;
        if (SOURCES.match(name).isEmpty()) return -4;
        return Math.max(-12, Math.min(3, 2 - burden - scarcity));
    }

    private static int[] policy(String name)
    {
        String value = normalize(name);
        if (value.equals("spirit seed") || value.equals("crystal acorn"))
            return new int[]{4, 0};
        for (String term : new String[]{"rune", "essence", "bar", "plank",
                "nail", "log", "raw ", "grape", "jug of water", "feather",
                "arrowhead", "headless arrow", "dart tip", "unfinished bolt",
                "uncut ", "herb", "weed", "snape grass", "crushed nest",
                "red spiders eggs", "sapling", "seed"})
            if (value.contains(term)) return new int[]{1, 1};
        return null;
    }

    private static Set<Integer> observedGroupItemIds(StrategyContext context,
            String itemName)
    {
        if (context == null || !context.accountMode().isGroupIronman()
                || !context.isUseGroupStorage() || context.data() == null
                || context.data().groupStorage() == null
                || !context.data().groupStorage().isObserved())
            return Collections.emptySet();
        String target = normalize(itemName);
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemState item : context.data().groupStorage().getItems())
            if (item != null && item.getQuantity() > 0 && item.getItemId() > 0
                    && target.equals(normalize(item.getName())))
                ids.add(item.getItemId());
        return ids;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
