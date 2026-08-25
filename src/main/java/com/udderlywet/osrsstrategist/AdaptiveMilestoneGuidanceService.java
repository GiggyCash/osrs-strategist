package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Converts deterministic curated methods into account-specific milestone work.
 *
 * <p>RuneLite supplies maintained XP-per-action data. Compass supplies route
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
    private final AccountResourcePlanner resourcePlanner;

    @Inject
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog,
            SkillingXpModifierService xpModifierService,
            AdaptiveActionSelector actionSelector,
            MethodInputResolver inputResolver,
            AccountResourcePlanner resourcePlanner)
    {
        this.actionCatalog = actionCatalog;
        this.profileCatalog = profileCatalog;
        this.xpModifierService = xpModifierService;
        this.actionSelector = actionSelector;
        this.inputResolver = inputResolver;
        this.resourcePlanner = resourcePlanner;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog,
            SkillingXpModifierService xpModifierService)
    {
        this(actionCatalog, profileCatalog, xpModifierService,
                new AdaptiveActionSelector(), new MethodInputResolver(),
                new AccountResourcePlanner());
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

        String actionText = format(xpNeeded) + " XP remaining — about "
                + actionsNeeded + " " + profile.unit(actionsNeeded)
                + " with " + action.getName() + " to level "
                + targetLevel + ".";

        List<ResolvedMethodInput> inputs = inputResolver.resolve(
                profile, action, actionsNeeded);
        AccountResourcePlan resources = resourcePlanner == null
                ? null
                : resourcePlanner.plan(data, inputs, useGroupStorage);
        String supplies = inputs.isEmpty()
                ? null
                : resources == null ? null : resources.getGuidance();

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
        if (mode == AccountMode.ULTIMATE_IRONMAN && resources != null
                && resources.getTotalMissingUnits() > 0)
        {
            note += " UIM shortfalls are intentionally based on immediately usable supplies, not retrieval-only storage.";
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
