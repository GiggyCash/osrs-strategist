package com.udderlywet.osrsstrategist;

import java.util.*;
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
        List<RuneLiteSkillActionDefinition> routeOutputs =
                unlockedRouteOutputs(actionCatalog.actionsFor(skill), profile,
                        currentLevel, data.getAccount().getMembershipStatus());
        boolean variableOutput = profile.getProgressEstimateMode()
                    == MethodExecutionProfile.ProgressEstimateMode
                            .VARIABLE_OUTPUT_RANGE
                && hasDifferentXp(routeOutputs);
        int minimumActions = variableOutput
                ? divideRoundUp(xpNeeded,
                        maximumXp(routeOutputs) * combinedMultiplier)
                : divideRoundUp(xpNeeded, xpPerAction);
        int maximumActions = variableOutput
                ? divideRoundUp(xpNeeded,
                        minimumXp(routeOutputs) * combinedMultiplier)
                : minimumActions;

        String progressText = progressText(profile, action, routeOutputs,
                xpNeeded, minimumActions, maximumActions, targetLevel,
                variableOutput);

        List<ResolvedMethodInput> inputs = inputResolver.resolve(
                profile, action, maximumActions);
        AccountResourcePlan resources = resourcePlanner == null
                ? null
                : resourcePlanner.plan(data, inputs, useGroupStorage);
        String supplies = inputs.isEmpty()
                ? null
                : resources == null ? null : resources.getGuidance();
        String routeSetup = routeSetup(
                data, plan.getMethod().getId(), action, currentLevel,
                useGroupStorage);
        if (routeSetup != null)
        {
            supplies = supplies == null || supplies.trim().isEmpty()
                    ? routeSetup : routeSetup + " " + supplies;
        }

        String location = routeLocation(data, plan.getMethod().getId(),
                action, plan.getMethod().getInstructions());
        String actionText = executionAction(plan.getMethod(), profile, action,
                routeOutputs);
        String note = profile.getNote();
        if (note == null || note.trim().isEmpty())
        {
            note = PlayerText.get("AMGS1");
        }
        else
        {
            note += PlayerText.get("AMGS2");
        }

        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note += " Count assumes you wear the " + modifier.getLabel() + ".";
        }
        else
        {
            note += PlayerText.get("AMGS3");
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode.isIronLike() && !inputs.isEmpty())
        {
            note += PlayerText.get("AMGS4")
                    + PlayerText.get("AMGS5")
                    + "unsupplied higher-tier route.";
        }
        if (mode == AccountMode.ULTIMATE_IRONMAN && resources != null
                && resources.getTotalMissingUnits() > 0)
        {
            note += PlayerText.get("AMGS6");
        }

        RecommendationGuidance result = new RecommendationGuidance(
                actionText,
                supplies,
                location,
                note);
        return result.withProgress(progressText);
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

    /** Reusable tools are not consumed recipes, but still belong in BRING. */
    private static String routeSetup(StrategyDataBundle data, String methodId,
            RuneLiteSkillActionDefinition action, int currentLevel,
            boolean useGroupStorage)
    {
        if (methodId == null) return null;
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
        if (methodId.startsWith("mining_"))
        {
            String pickaxe = firstObserved(items,
                    "Crystal pickaxe", "Infernal pickaxe", "Dragon pickaxe",
                    "Rune pickaxe", "Adamant pickaxe", "Mithril pickaxe",
                    "Black pickaxe", "Steel pickaxe", "Iron pickaxe",
                    "Bronze pickaxe");
            return pickaxe == null
                    ? PlayerText.get("AMGS7")
                    : "Bring your " + pickaxe + ".";
        }
        if (methodId.startsWith("woodcutting_"))
        {
            String axe = firstObserved(items,
                    "Crystal axe", "Infernal axe", "Dragon axe", "Rune axe",
                    "Adamant axe", "Mithril axe", "Black axe", "Steel axe",
                    "Iron axe", "Bronze axe");
            return axe == null
                    ? PlayerText.get("AMGS8")
                    : "Bring your " + axe + ".";
        }
        if (isFlyFishingMethod(methodId))
            return items.has("Fly fishing rod")
                    ? "Bring your fly fishing rod."
                    : PlayerText.get("AMGS9");
        if (isNetFishingMethod(methodId))
            return PlayerText.get("AMGS10");
        if ("hunter_bird_traps".equals(methodId))
            return items.has("Bird snare")
                    ? "Bring one bird snare."
                    : PlayerText.get("AMGS11");
        if ("hunter_falconry".equals(methodId))
            return PlayerText.get("AMGS12");
        if ("hunter_salamanders".equals(methodId))
        {
            int traps = currentLevel >= 60 ? 4 : currentLevel >= 40 ? 3 : 2;
            return "Bring " + traps + " small fishing nets and " + traps
                    + PlayerText.get("AMGS13");
        }
        if ("magic_f2p_combat".equals(methodId))
            return PlayerText.get("AMGS14");
        if ("magic_f2p_fire_bolt".equals(methodId))
            return PlayerText.get("AMGS15");
        if ("magic_f2p_fire_blast".equals(methodId))
            return PlayerText.get("AMGS16");
        if ("magic_f2p_curse".equals(methodId))
            return PlayerText.get("AMGS17");
        if ("magic_f2p_fire_strike_splash".equals(methodId))
            return PlayerText.get("AMGS18");
        if ("construction_crude_chairs".equals(methodId)
                || "construction_oak_larders".equals(methodId))
            return "Bring a hammer and saw.";
        if ("smithing_f2p_platebodies".equals(methodId))
            return PlayerText.get("AMGS19");
        if ("smithing_f2p_uim_bronze".equals(methodId))
            return PlayerText.get("AMGS20");
        if ("thieving_lumbridge_people".equals(methodId))
            return PlayerText.get("AMGS21");
        if ("thieving_ardy_knights".equals(methodId))
            return PlayerText.get("AMGS22");
        if (methodId.startsWith("runecraft_f2p_"))
        {
            String rune = methodId.substring("runecraft_f2p_".length());
            return "Bring the " + rune + " talisman or wear the " + rune
                    + " tiara.";
        }
        return null;
    }

    private static String routeLocation(StrategyDataBundle data,
            String methodId, RuneLiteSkillActionDefinition action,
            String fallback)
    {
        String actionName = action == null || action.getName() == null
                ? "" : action.getName().toLowerCase(Locale.ROOT);
        if ("hunter_salamanders".equals(methodId))
        {
            if (actionName.contains("red salamander"))
                return PlayerText.get("AMGS23");
            if (actionName.contains("orange salamander"))
                return PlayerText.get("AMGS24");
            return PlayerText.get("AMGS25");
        }
        if (isFlyFishingMethod(methodId))
            return PlayerText.get("AMGS26");
        if (isNetFishingMethod(methodId))
            return PlayerText.get("AMGS27");
        if ("magic_f2p_curse".equals(methodId)
                || "magic_f2p_fire_strike_splash".equals(methodId))
        {
            return PlayerText.get("AMGS28");
        }
        if ("smithing_f2p_platebodies".equals(methodId))
        {
            return PlayerText.get("AMGS29");
        }
        if ("smithing_f2p_uim_bronze".equals(methodId))
        {
            return PlayerText.get("AMGS30");
        }

        AccountMode mode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            if ("crafting_gems".equals(methodId)
                    || "fletching_bows".equals(methodId)
                    || "herblore_low_potions".equals(methodId))
            {
                return PlayerText.get("AMGS31");
            }
            if ("firemaking_f2p_logs".equals(methodId))
            {
                return PlayerText.get("AMGS32");
            }
        }
        String explicit = locationBeforeColon(fallback);
        return explicit == null ? fallback : explicit;
    }

    private static String progressText(MethodExecutionProfile profile,
            RuneLiteSkillActionDefinition selected,
            List<RuneLiteSkillActionDefinition> outputs,
            int xpNeeded, int minimumActions, int maximumActions,
            int targetLevel, boolean variableOutput)
    {
        if (profile.getProgressEstimateMode()
                == MethodExecutionProfile.ProgressEstimateMode.XP_ONLY)
            return format(xpNeeded) + " XP remaining to level "
                    + targetLevel + ".";
        if (variableOutput)
            return format(xpNeeded) + " XP remaining — approximately "
                    + format(minimumActions) + "–"
                    + format(maximumActions) + " "
                    + profile.unit(maximumActions) + " across "
                    + outputNames(outputs) + " to level " + targetLevel + ".";
        return format(xpNeeded) + " XP remaining — "
                + format(maximumActions) + " "
                + profile.unit(maximumActions) + " with "
                + selected.getName() + " to level " + targetLevel + ".";
    }

    private static List<RuneLiteSkillActionDefinition> unlockedRouteOutputs(
            List<RuneLiteSkillActionDefinition> actions,
            MethodExecutionProfile profile, int currentLevel,
            MembershipStatus membership)
    {
        List<RuneLiteSkillActionDefinition> result = new ArrayList<>();
        if (actions == null || profile == null) return result;
        for (RuneLiteSkillActionDefinition action : actions)
        {
            if (action == null || action.getXp() <= 0
                    || action.getLevel() > currentLevel
                    || !membershipAllowed(action.getMembership(), membership)
                    || !matches(action, profile.getActionTerms())) continue;
            result.add(action);
        }
        return result;
    }

    private static boolean membershipAllowed(MembershipStatus action,
            MembershipStatus account)
    {
        if (action == MembershipStatus.F2P) return true;
        return action == MembershipStatus.P2P
                && account == MembershipStatus.P2P;
    }

    private static boolean matches(RuneLiteSkillActionDefinition action,
            List<String> terms)
    {
        if (terms == null || terms.isEmpty()) return false;
        String haystack = normalize(action.getId()) + " "
                + normalize(action.getName()) + " "
                + normalize(action.getCategory());
        for (String term : terms)
            if (haystack.contains(normalize(term))) return true;
        return false;
    }

    private static boolean hasDifferentXp(
            List<RuneLiteSkillActionDefinition> actions)
    {
        if (actions == null || actions.size() < 2) return false;
        float first = actions.get(0).getXp();
        for (RuneLiteSkillActionDefinition action : actions)
            if (Math.abs(action.getXp() - first) > 0.001f) return true;
        return false;
    }

    private static double minimumXp(List<RuneLiteSkillActionDefinition> actions)
    {
        double value = Double.POSITIVE_INFINITY;
        for (RuneLiteSkillActionDefinition action : actions)
            value = Math.min(value, action.getXp());
        return value;
    }

    private static double maximumXp(List<RuneLiteSkillActionDefinition> actions)
    {
        double value = 0.0;
        for (RuneLiteSkillActionDefinition action : actions)
            value = Math.max(value, action.getXp());
        return value;
    }

    private static String outputNames(List<RuneLiteSkillActionDefinition> actions)
    {
        List<String> names = new ArrayList<>();
        for (RuneLiteSkillActionDefinition action : actions)
            if (action.getName() != null && !names.contains(action.getName()))
                names.add(action.getName());
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        return String.join(", ", names);
    }

    private static String routeAction(String instructions, String methodName)
    {
        if (instructions == null || instructions.trim().isEmpty())
            return methodName;
        int colon = instructions.indexOf(':');
        String action = colon >= 0 && colon + 1 < instructions.length()
                ? instructions.substring(colon + 1).trim()
                : instructions.trim();
        if (action.isEmpty()) return methodName;
        return Character.toUpperCase(action.charAt(0)) + action.substring(1);
    }

    private static String executionAction(TrainingMethod method,
            MethodExecutionProfile profile,
            RuneLiteSkillActionDefinition selected,
            List<RuneLiteSkillActionDefinition> outputs)
    {
        if (profile != null && profile.getProgressEstimateMode()
                == MethodExecutionProfile.ProgressEstimateMode
                        .VARIABLE_OUTPUT_RANGE)
        {
            String names = outputNames(outputs).toLowerCase(Locale.ROOT);
            if (isFlyFishingMethod(method.getId()))
                return "Fly-fish " + names
                        + PlayerText.get("AMGS33");
            if (isNetFishingMethod(method.getId()))
                return "Use the small net to catch " + names
                        + PlayerText.get("AMGS34");
        }
        String instruction = routeAction(method.getInstructions(),
                method.getName());
        if (selected == null || selected.getName() == null
                || normalize(instruction).contains(
                        normalize(selected.getName())))
            return instruction;
        return selected.getName() + ": " + instruction;
    }

    private static String locationBeforeColon(String instructions)
    {
        if (instructions == null) return null;
        int colon = instructions.indexOf(':');
        if (colon < 3) return null;
        return instructions.substring(0, colon).trim() + ".";
    }

    private static boolean isFlyFishingMethod(String methodId)
    {
        return "fishing_f2p_fly".equals(methodId)
                || "fishing_f2p_fly_baseline".equals(methodId);
    }

    private static boolean isNetFishingMethod(String methodId)
    {
        return "fishing_lumbridge_shrimps".equals(methodId)
                || "fishing_f2p_shrimps".equals(methodId);
    }

    private static String firstObserved(
            ObservedItemIndex items, String... names)
    {
        for (String name : names) if (items.has(name)) return name;
        return null;
    }

    private static String format(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
        {
            return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }
}
