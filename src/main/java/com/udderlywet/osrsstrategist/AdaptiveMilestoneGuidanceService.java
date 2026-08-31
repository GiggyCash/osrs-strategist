package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }

        MethodProfile profile = profileCatalog.forMethod(
                plan.getMethod().getId());
        if (profile == null) return null;

        var currentXp = data.account().getSkillExperience(skill);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        SkillingXpModifier modifier = xpModifierService == null
                ? SkillingXpModifier.none()
                : xpModifierService.modifier(data, skill, useGroupStorage);
        double combinedMultiplier = profile.getXpMultiplier()
                * modifier.getMultiplier();

        ActionDef action = actionSelector.select(
                data,
                profile,
                actionCatalog.actionsFor(skill),
                currentLevel,
                data.account().getMembershipStatus(),
                currentXp,
                targetXp,
                combinedMultiplier,
                useGroupStorage);
        if (action == null || action.getXp() <= 0) return null;

        var xpPerAction = action.getXp() * combinedMultiplier;
        if (xpPerAction <= 0) return null;
        List<ActionDef> routeOutputs =
                unlockedRouteOutputs(actionCatalog.actionsFor(skill), profile,
                        currentLevel, data.account().getMembershipStatus());
        boolean variableOutput = profile.getProgressEstimateMode()
                    == MethodProfile.ProgressEstimateMode
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

        List<MethodInput> inputs = inputResolver.resolve(
                profile, action, maximumActions);
        SupplyPlan resources = resourcePlanner == null
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
        var note = profile.getNote();
        if (note == null || note.trim().isEmpty())
        {
            note = get(5);
        }
        else
        {
            note += get(16);
        }

        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note += get(1280) + modifier.getLabel() + ".";
        }
        else
        {
            note += get(27);
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.account().getAccountTypeCode());
        if (mode.isIronLike() && !inputs.isEmpty())
        {
            note += get(33)
                    + get(34)
                    + get(1281);
        }
        if (mode == AccountMode.ULTIMATE_IRONMAN && resources != null
                && resources.getTotalMissingUnits() > 0)
        {
            note += get(35);
        }

        Guidance result = new Guidance(
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
    ActionDef selectAction(
            List<ActionDef> actions,
            MethodProfile profile,
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
    private static String routeSetup(GameData data, String methodId,
            ActionDef action, int currentLevel,
            boolean useGroupStorage)
    {
        if (methodId == null) return null;
        var items = new ItemIndex(data, useGroupStorage);
        if (methodId.startsWith("mining_"))
        {
            String pickaxe = firstObserved(items,
                    "Crystal pickaxe", "Infernal pickaxe", "Dragon pickaxe",
                    "Rune pickaxe", "Adamant pickaxe", "Mithril pickaxe",
                    "Black pickaxe", "Steel pickaxe", "Iron pickaxe",
                    "Bronze pickaxe");
            return pickaxe == null
                    ? get(36)
                    : "Bring your " + pickaxe + ".";
        }
        if (methodId.startsWith("woodcutting_"))
        {
            String axe = firstObserved(items,
                    "Crystal axe", "Infernal axe", "Dragon axe", "Rune axe",
                    "Adamant axe", "Mithril axe", "Black axe", "Steel axe",
                    "Iron axe", "Bronze axe");
            return axe == null
                    ? get(37)
                    : "Bring your " + axe + ".";
        }
        if (isFlyFishingMethod(methodId))
            return items.has("Fly fishing rod")
                    ? get(1282)
                    : get(38);
        if (isNetFishingMethod(methodId))
            return get(6);
        if ("hunter_bird_traps".equals(methodId))
            return items.has("Bird snare")
                    ? get(1283)
                    : get(7);
        if ("hunter_falconry".equals(methodId))
            return get(8);
        if ("hunter_salamanders".equals(methodId))
        {
            var traps = currentLevel >= 60 ? 4 : currentLevel >= 40 ? 3 : 2;
            return "Bring " + traps + get(1284) + traps
                    + get(9);
        }
        if ("magic_f2p_combat".equals(methodId))
            return get(10);
        if ("magic_f2p_fire_bolt".equals(methodId))
            return get(11);
        if ("magic_f2p_fire_blast".equals(methodId))
            return get(12);
        if ("magic_f2p_curse".equals(methodId))
            return get(13);
        if ("magic_f2p_fire_strike_splash".equals(methodId))
            return get(14);
        if ("construction_crude_chairs".equals(methodId)
                || "construction_oak_larders".equals(methodId))
            return get(1268);
        if ("smithing_f2p_platebodies".equals(methodId))
            return get(15);
        if ("smithing_f2p_uim_bronze".equals(methodId))
            return get(17);
        if ("thieving_lumbridge_people".equals(methodId))
            return get(18);
        if ("thieving_ardy_knights".equals(methodId))
            return get(19);
        if (methodId.startsWith("runecraft_f2p_"))
        {
            var rune = methodId.substring("runecraft_f2p_".length());
            return "Bring the " + rune + get(1285) + rune
                    + " tiara.";
        }
        return null;
    }

    private static String routeLocation(GameData data,
            String methodId, ActionDef action,
            String fallback)
    {
        String actionName = action == null || action.getName() == null
                ? "" : action.getName().toLowerCase(Locale.ROOT);
        if ("hunter_salamanders".equals(methodId))
        {
            if (actionName.contains("red salamander"))
                return get(20);
            if (actionName.contains("orange salamander"))
                return get(21);
            return get(22);
        }
        if (isFlyFishingMethod(methodId))
            return get(23);
        if (isNetFishingMethod(methodId))
            return get(24);
        if ("magic_f2p_curse".equals(methodId)
                || "magic_f2p_fire_strike_splash".equals(methodId))
        {
            return get(25);
        }
        if ("smithing_f2p_platebodies".equals(methodId))
        {
            return get(26);
        }
        if ("smithing_f2p_uim_bronze".equals(methodId))
        {
            return get(28);
        }

        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            if ("crafting_gems".equals(methodId)
                    || "fletching_bows".equals(methodId)
                    || "herblore_low_potions".equals(methodId))
            {
                return get(29);
            }
            if ("firemaking_f2p_logs".equals(methodId))
            {
                return get(30);
            }
        }
        var explicit = locationBeforeColon(fallback);
        return explicit == null ? fallback : explicit;
    }

    private static String progressText(MethodProfile profile,
            ActionDef selected,
            List<ActionDef> outputs,
            int xpNeeded, int minimumActions, int maximumActions,
            int targetLevel, boolean variableOutput)
    {
        if (profile.getProgressEstimateMode()
                == MethodProfile.ProgressEstimateMode.XP_ONLY)
            return format(xpNeeded) + get(1286)
                    + targetLevel + ".";
        if (variableOutput)
            return format(xpNeeded) + get(1287)
                    + format(minimumActions) + "–"
                    + format(maximumActions) + " "
                    + profile.unit(maximumActions) + " across "
                    + outputNames(outputs) + " to level " + targetLevel + ".";
        return format(xpNeeded) + " XP remaining — "
                + format(maximumActions) + " "
                + profile.unit(maximumActions) + " with "
                + selected.getName() + " to level " + targetLevel + ".";
    }

    private static List<ActionDef> unlockedRouteOutputs(
            List<ActionDef> actions,
            MethodProfile profile, int currentLevel,
            MembershipStatus membership)
    {
        List<ActionDef> result = new ArrayList<>();
        if (actions == null || profile == null) return result;
        for (ActionDef action : actions)
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

    private static boolean matches(ActionDef action,
            List<String> terms)
    {
        if (terms == null || terms.isEmpty()) return false;
        String haystack = Names.actionKey(action.getId()) + " "
                + Names.actionKey(action.getName()) + " "
                + Names.actionKey(action.getCategory());
        for (String term : terms)
            if (haystack.contains(Names.actionKey(term))) return true;
        return false;
    }

    private static boolean hasDifferentXp(
            List<ActionDef> actions)
    {
        if (actions == null || actions.size() < 2) return false;
        var first = actions.get(0).getXp();
        for (ActionDef action : actions)
            if (Math.abs(action.getXp() - first) > 0.001f) return true;
        return false;
    }

    private static double minimumXp(List<ActionDef> actions)
    {
        var value = Double.POSITIVE_INFINITY;
        for (ActionDef action : actions)
            value = Math.min(value, action.getXp());
        return value;
    }

    private static double maximumXp(List<ActionDef> actions)
    {
        var value = 0.0;
        for (ActionDef action : actions)
            value = Math.max(value, action.getXp());
        return value;
    }

    private static String outputNames(List<ActionDef> actions)
    {
        List<String> names = new ArrayList<>();
        for (ActionDef action : actions)
            if (action.getName() != null && !names.contains(action.getName()))
                names.add(action.getName());
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        return String.join(", ", names);
    }

    private static String routeAction(String instructions, String methodName)
    {
        if (instructions == null || instructions.trim().isEmpty())
            return methodName;
        var colon = instructions.indexOf(':');
        String action = colon >= 0 && colon + 1 < instructions.length()
                ? instructions.substring(colon + 1).trim()
                : instructions.trim();
        if (action.isEmpty()) return methodName;
        return Character.toUpperCase(action.charAt(0)) + action.substring(1);
    }

    private static String executionAction(TrainingMethod method,
            MethodProfile profile,
            ActionDef selected,
            List<ActionDef> outputs)
    {
        if (profile != null && profile.getProgressEstimateMode()
                == MethodProfile.ProgressEstimateMode
                        .VARIABLE_OUTPUT_RANGE)
        {
            var names = outputNames(outputs).toLowerCase(Locale.ROOT);
            if (isFlyFishingMethod(method.getId()))
                return "Fly-fish " + names
                        + get(31);
            if (isNetFishingMethod(method.getId()))
                return get(1288) + names
                        + get(32);
        }
        String instruction = routeAction(method.getInstructions(),
                method.getName());
        if (selected == null || selected.getName() == null
                || Names.actionKey(instruction).contains(
                        Names.actionKey(selected.getName())))
            return instruction;
        return selected.getName() + ": " + instruction;
    }

    private static String locationBeforeColon(String instructions)
    {
        if (instructions == null) return null;
        var colon = instructions.indexOf(':');
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
            ItemIndex items, String... names)
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

}
