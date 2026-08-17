package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Converts deterministic curated methods into account-specific milestone work.
 *
 * <p>RuneLite supplies maintained XP-per-action data. Strategist supplies route
 * selection, account-mode/build policy, observed resources, XP modifiers, live
 * Main-account purchase prices, and acquisition advice. A concrete action is
 * only emitted when its math can be resolved without inventing a rate.</p>
 */
@Singleton
public class AdaptiveMilestoneGuidanceService
{
    private final RuneLiteSkillActionCatalog actionCatalog;
    private final MethodExecutionProfileCatalog profileCatalog;
    private final SkillingXpModifierService xpModifierService;
    private final AdaptiveActionSelector actionSelector;
    private final MethodInputResolver inputResolver;
    private final PurchaseCostAdvisor purchaseCostAdvisor;

    @Inject
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog,
            SkillingXpModifierService xpModifierService,
            AdaptiveActionSelector actionSelector,
            MethodInputResolver inputResolver,
            PurchaseCostAdvisor purchaseCostAdvisor)
    {
        this.actionCatalog = actionCatalog;
        this.profileCatalog = profileCatalog;
        this.xpModifierService = xpModifierService;
        this.actionSelector = actionSelector;
        this.inputResolver = inputResolver;
        this.purchaseCostAdvisor = purchaseCostAdvisor;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog,
            SkillingXpModifierService xpModifierService)
    {
        this(actionCatalog, profileCatalog, xpModifierService,
                new AdaptiveActionSelector(), new MethodInputResolver(),
                new PurchaseCostAdvisor());
    }

    /** Compatibility constructor retained for older callers. */
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog)
    {
        this(actionCatalog, profileCatalog, new SkillingXpModifierService());
    }

    /** Compatibility constructor for tests without RuneLite injection. */
    public AdaptiveMilestoneGuidanceService()
    {
        this(new RuneLiteSkillActionCatalog(),
                new MethodExecutionProfileCatalog(),
                new SkillingXpModifierService());
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null || skill == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }

        MethodExecutionProfile profile = profileCatalog.forMethod(
                plan.getMethod().getId());
        if (profile == null) return null;

        int currentXp = data.getAccount().getSkillExperience(skill);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        SkillingXpModifier modifier = xpModifierService == null
                ? SkillingXpModifier.none()
                : xpModifierService.modifier(data, skill, useGroupStorage);
        double combinedMultiplier = profile.getXpMultiplier()
                * modifier.getMultiplier();

        RuneLiteSkillActionDefinition action = actionSelector.select(
                data,
                profile,
                actionCatalog.actionsFor(skill),
                currentLevel,
                data.getAccount().getMembershipStatus(),
                currentXp,
                targetXp,
                combinedMultiplier,
                useGroupStorage);
        if (action == null || action.getXp() <= 0) return null;

        double xpPerAction = action.getXp() * combinedMultiplier;
        if (xpPerAction <= 0) return null;
        int actionsNeeded = divideRoundUp(xpNeeded, xpPerAction);

        String actionText = "Use " + action.getName() + " for about "
                + actionsNeeded + " " + profile.unit(actionsNeeded)
                + " to reach level " + targetLevel + ". "
                + format(xpNeeded) + " XP remains; this action gives "
                + format(xpPerAction) + " XP each in the modeled setup.";

        List<ResolvedMethodInput> inputs = inputResolver.resolve(
                profile, action, actionsNeeded);
        String supplies = inputs.isEmpty()
                ? null
                : supplyGuidance(
                        data,
                        data.getAccount(),
                        inputs,
                        useGroupStorage);

        String location = plan.getMethod().getInstructions();
        String note = profile.getNote();
        if (note == null || note.trim().isEmpty())
        {
            note = "Action count uses RuneLite's maintained base action XP.";
        }
        else
        {
            note += " Action count uses RuneLite's maintained action XP.";
        }

        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note += " Count assumes you wear the " + modifier.getLabel() + ".";
        }
        else
        {
            note += " Any unmodeled XP bonus can reduce the remaining count.";
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode.isIronLike() && !inputs.isEmpty())
        {
            note += " The concrete action was ranked against your observed "
                    + "materials so a supplied lower-tier route can beat an "
                    + "unsupplied higher-tier route.";
        }

        return new RecommendationGuidance(
                actionText,
                supplies,
                location,
                note);
    }

    /**
     * Compatibility hook used by focused selector tests. Resource-aware builds
     * use AdaptiveActionSelector.select from build(...).
     */
    RuneLiteSkillActionDefinition selectAction(
            List<RuneLiteSkillActionDefinition> actions,
            MethodExecutionProfile profile,
            int currentLevel,
            MembershipStatus membership)
    {
        return actionSelector.selectSimple(
                actions, profile, currentLevel, membership);
    }

    private String supplyGuidance(
            StrategyDataBundle data,
            AccountSnapshot account,
            List<ResolvedMethodInput> needs,
            boolean useGroupStorage)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        ObservedItemIndex index = new ObservedItemIndex(data, useGroupStorage);
        boolean infiniteFire = hasInfiniteFireRunes(index);

        List<String> requiredParts = new ArrayList<>();
        List<String> verifiedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        List<ResolvedMethodInput> missingInputs = new ArrayList<>();

        for (ResolvedMethodInput need : needs)
        {
            if (infiniteFire && "fire rune".equalsIgnoreCase(need.getName()))
            {
                requiredParts.add("0 fire runes (use your fire-rune staff)");
                continue;
            }

            int verified = index.quantity(need.getName());
            int missing = Math.max(0, need.getQuantity() - verified);
            requiredParts.add(need.getQuantity() + " " + need.getName());
            verifiedParts.add(verified + " " + need.getName());
            if (missing > 0)
            {
                missingParts.add(missing + " " + need.getName());
                missingInputs.add(new ResolvedMethodInput(
                        need.getName(), need.getItemId(), missing));
            }
        }

        String required = joinNatural(requiredParts);
        if (mode != AccountMode.ULTIMATE_IRONMAN && !index.bankObserved())
        {
            return "Need " + required
                    + ". Open your bank once so Strategist can verify stored "
                    + "materials before deciding exact shortfalls.";
        }

        StringBuilder text = new StringBuilder();
        text.append("Need ").append(required).append(". ");
        if (!verifiedParts.isEmpty())
        {
            text.append("Verified: ")
                    .append(joinNatural(verifiedParts)).append(". ");
        }

        if (missingParts.isEmpty())
        {
            text.append("No extra modeled materials are needed.");
            return text.toString();
        }

        String missing = joinNatural(missingParts);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            text.append("Acquire ").append(missing)
                    .append(" just in time. Normal bank state is ignored for UIM.");
        }
        else if (mode.usesGrandExchange())
        {
            text.append("Buy ").append(missing)
                    .append(" at the Grand Exchange.");
            String costAdvice = purchaseCostAdvisor == null
                    ? null
                    : purchaseCostAdvisor.advice(
                            data.getEconomy(), missingInputs);
            if (costAdvice != null)
            {
                text.append(" ").append(costAdvice);
            }
            else
            {
                text.append(" Exact quantities are ready, but a live exact-price quote is not currently available.");
            }
        }
        else if (mode.isGroupIronman())
        {
            text.append("Source ").append(missing);
            if (useGroupStorage)
            {
                text.append(" after checking observed Group Storage");
            }
            text.append(".");
        }
        else
        {
            text.append("Self-source ").append(missing).append(".");
        }
        return text.toString();
    }

    private static boolean hasInfiniteFireRunes(ObservedItemIndex items)
    {
        return items.has(
                "Staff of fire", "Mystic fire staff",
                "Lava battlestaff", "Mystic lava staff",
                "Steam battlestaff", "Mystic steam staff",
                "Smoke battlestaff", "Mystic smoke staff",
                "Tome of fire", "Tome of fire (empty)");
    }

    private static String joinNatural(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0)
            {
                text.append(i == parts.size() - 1 ? ", and " : ", ");
            }
            text.append(parts.get(i));
        }
        return text.toString();
    }

    private static int divideRoundUp(int numerator, double denominator)
    {
        if (numerator <= 0) return 0;
        return (int) Math.ceil(numerator / denominator);
    }

    private static String format(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
        {
            return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%,.1f", value);
    }
}
