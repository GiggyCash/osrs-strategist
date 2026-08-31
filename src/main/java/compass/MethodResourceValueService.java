package compass;

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
                : recommendation.plan();
        var method = plan == null ? null : plan.method();
        if (method == null || method.getSkill() == null || context == null
                || context.data() == null || context.data().account() == null
                || recommendation.getTargetLevel() <= 0) return recommendation;
        var profile = profiles.forMethod(method.getId());
        if (profile == null) return recommendation;

        var account = context.data().account();
        var skill = method.getSkill();
        var currentXp = account.xp(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(account.level(skill));
        var targetXp = Experience.getXpForLevel(recommendation.getTargetLevel());
        double multiplier = profile.getXpMultiplier() * modifiers.modifier(
                context.data(), skill, context.usesGroupStorage()).getMultiplier();
        ActionDef action = selector.select(context.data(), profile,
                actions.actionsFor(skill), account.level(skill),
                account.membership(), currentXp, targetXp, multiplier,
                context.usesGroupStorage());
        if (action == null || action.getXp() <= 0) return recommendation;
        int count = (int) Math.ceil(Math.max(0, targetXp - currentXp)
                / (action.getXp() * multiplier));

        var score = 0;
        var known = false;
        var shared = StrategicValue.neutral();
        for (MethodInput input : inputs.resolve(profile, action, count))
        {
            var policy = policy(input.getName());
            if (policy == null) continue;
            known = true;
            score += resourceAdjustment(context, input.getName(),
                    input.getQuantity(), policy[0], policy[1] == 1);
            var sharedIds = observedGroupItemIds(context, input.getName());
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
                context.usesGroupStorage());
        var observed = items.quantity(name);
        var mode = context.accountMode();
        int burden = mode.usesGrandExchange() && tradeable ? 1
                : mode.isIronLike() ? 5 : 4;
        if (mode.isGroupIronman() && context.usesGroupStorage()
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
        var value = Names.words(name);
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
                || !context.usesGroupStorage() || context.data() == null
                || context.data().groupStorage() == null
                || !context.data().groupStorage().isObserved())
            return Collections.emptySet();
        var target = Names.words(itemName);
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemState item : context.data().groupStorage().getItems())
            if (item != null && item.getQuantity() > 0 && item.getItemId() > 0
                    && target.equals(Names.words(item.getName())))
                ids.add(item.getItemId());
        return ids;
    }

}
