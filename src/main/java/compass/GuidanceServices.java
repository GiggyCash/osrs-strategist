package compass;

import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import static compass.Text.get;

/**
 * Converts deterministic curated methods into account-specific milestone work.
 *
 * <p>RuneLite supplies maintained XP-per-action data. Compass supplies route
 * selection, account-mode/build policy, observed resources, XP modifiers, live
 * Main-account purchase prices, and acquisition advice. A concrete action is
 * only emitted when its math can be resolved without inventing a rate.</p>
 */
@Singleton
class AdaptiveMilestoneGuidanceService
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
                || plan == null || plan.method() == null)
        {
            return null;
        }

        MethodProfile profile = profileCatalog.forMethod(
                plan.method().id);
        if (profile == null) return null;

        var currentXp = data.account().xp(skill);
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
                data.account().membership(),
                currentXp,
                targetXp,
                combinedMultiplier,
                useGroupStorage);
        if (action == null || action.getXp() <= 0) return null;

        var xpPerAction = action.getXp() * combinedMultiplier;
        if (xpPerAction <= 0) return null;
        List<ActionDef> routeOutputs =
                unlockedRouteOutputs(actionCatalog.actionsFor(skill), profile,
                        currentLevel, data.account().membership());
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
                data, plan.method().id, action, currentLevel,
                useGroupStorage);
        if (routeSetup != null)
        {
            supplies = supplies == null || supplies.trim().isEmpty()
                    ? routeSetup : routeSetup + " " + supplies;
        }

        String location = routeLocation(data, plan.method().id,
                action, plan.method().getInstructions());
        String actionText = executionAction(plan.method(), profile, action,
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
                data.account().modeCode());
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
                    "Crystal pickaxe", get(1622), "Dragon pickaxe",
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
        if (get(1623).equals(methodId))
            return items.has("Bird snare")
                    ? get(1283)
                    : get(7);
        if ("hunter_falconry".equals(methodId))
            return get(8);
        if (get(1624).equals(methodId))
        {
            var traps = currentLevel >= 60 ? 4 : currentLevel >= 40 ? 3 : 2;
            return "Bring " + traps + get(1284) + traps
                    + get(9);
        }
        if (get(1625).equals(methodId))
            return get(10);
        if (get(1626).equals(methodId))
            return get(11);
        if (get(1627).equals(methodId))
            return get(12);
        if ("magic_f2p_curse".equals(methodId))
            return get(13);
        if (get(1628).equals(methodId))
            return get(14);
        if (get(1629).equals(methodId)
                || get(1630).equals(methodId))
            return get(1268);
        if (get(1631).equals(methodId))
            return get(15);
        if (get(1632).equals(methodId))
            return get(17);
        if (get(1633).equals(methodId))
            return get(18);
        if (get(1634).equals(methodId))
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
        if (get(1624).equals(methodId))
        {
            if (actionName.contains("red salamander"))
                return get(20);
            if (actionName.contains(get(1635)))
                return get(21);
            return get(22);
        }
        if (isFlyFishingMethod(methodId))
            return get(23);
        if (isNetFishingMethod(methodId))
            return get(24);
        if ("magic_f2p_curse".equals(methodId)
                || get(1628).equals(methodId))
        {
            return get(25);
        }
        if (get(1631).equals(methodId))
        {
            return get(26);
        }
        if (get(1632).equals(methodId))
        {
            return get(28);
        }

        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(
                        data.account().modeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            if ("crafting_gems".equals(methodId)
                    || "fletching_bows".equals(methodId)
                    || get(1579).equals(methodId))
            {
                return get(29);
            }
            if (get(1636).equals(methodId))
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
        return format(xpNeeded) + get(1637)
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
        String haystack = Names.actionKey(action.id) + " "
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
            if (isFlyFishingMethod(method.id))
                return "Fly-fish " + names
                        + get(31);
            if (isNetFishingMethod(method.id))
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
                || get(1638).equals(methodId);
    }

    private static boolean isNetFishingMethod(String methodId)
    {
        return get(1639).equals(methodId)
                || get(1640).equals(methodId);
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

/**
 * Concrete combat guidance for Attack, Strength, Defence, and Ranged.
 *
 * <p>Combat is not treated like Cooking. Misses give no skill XP and target
 * choice, weapon speed, quest access, account builds, and monster XP modifiers
 * matter. Compass therefore gives an exact remaining-XP/damage target only
 * when the selected route has ordinary per-damage XP. Special encounters keep
 * exact XP remaining but do not invent a fake kill count.</p>
 */
@Singleton
class CombatGuidanceService
{
    private static final Loadouts LOADOUTS = BundledCatalogLoader.array(
            get(1682), Loadouts[].class)[0];
    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            SessionIntent sessionIntent,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.method() == null
                || !isDirectCombatSkill(skill))
        {
            return null;
        }

        var account = data.account();
        if (account.membership() == MembershipStatus.UNKNOWN) return null;
        var build = AccountBuildPolicy.effectiveBuild(account);
        if (!AccountBuildPolicy.allowsSkill(account, skill)) return null;

        String methodId = plan.method().id == null
                ? "" : plan.method().id.toLowerCase();
        CombatRoute route = chooseRoute(
                data, account, skill, currentLevel, build,
                methodId, sessionIntent);
        if (route == null) return null;

        var items = new ItemIndex(data, useGroupStorage);
        var weapon = chooseWeapon(account, skill, build, items);
        if (weapon == null && skill != Skill.RANGED
                && build == RestrictedBuildType.STANDARD
                && currentLevel < 20)
        {
            return new Guidance(
                    get(151),
                    get(1333),
                    get(162),
                    get(173));
        }
        boolean unarmed = weapon == null && currentLevel < 10
                && skill != Skill.RANGED;
        if (weapon == null && !unarmed) return null;
        var style = attackStyle(skill);

        var currentXp = account.xp(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        var action = new StringBuilder();
        action.append(withoutPeriod(route.loop));
        if (weapon != null) action.append(" with ").append(weapon);
        else if (unarmed) action.append(" while unarmed");
        action.append(" on ").append(style).append(". ");
        action.append(format(xpNeeded)).append(" ")
                .append(skill.getName()).append(get(1334))
                .append(targetLevel).append(".");

        if (route.xpPerDamage > 0)
        {
            var damageNeeded = (int) Math.ceil(xpNeeded / route.xpPerDamage);
            action.append(" That is about ")
                    .append(format(damageNeeded))
                    .append(get(183))
                    .append(trim(route.xpPerDamage))
                    .append(" XP per damage.");
        }

        String supplies = unarmed
                ? get(184)
                : supplyGuidance(account, skill, build, route, weapon, items);
        if (supplies == null) return null;
        var location = route.location;
        var note = route.note;
        if (build != RestrictedBuildType.STANDARD)
        {
            note += get(1335) + AccountBuildPolicy.label(account)
                    + get(185);
        }

        return new Guidance(
                action.toString(),
                supplies,
                location,
                note);
    }

    private static CombatRoute chooseRoute(
            GameData data,
            AccountSnapshot account,
            Skill skill,
            int level,
            RestrictedBuildType build,
            String methodId,
            SessionIntent intent)
    {
        var membership = account.membership();

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            if (membership != MembershipStatus.P2P)
            {
                if (intent == SessionIntent.AFK)
                {
                    return new CombatRoute(
                            get(186),
                            get(187),
                            4.0,
                            get(188));
                }
                return new CombatRoute(
                        get(1336),
                        get(152),
                        4.0,
                        get(153));
            }

            var crab = bestCrab(data, intent);
            if (crab != null)
            {
                crab.note = get(1337) + crab.note;
                return crab;
            }
        }

        if (methodId.contains("scurrius"))
        {
            return new CombatRoute(
                    get(1338),
                    get(154),
                    0.0,
                    get(155));
        }

        if (methodId.contains("slayer"))
        {
            // A live task count does not prove a concrete legal location or
            // loadout. SlayerGuidanceService owns task-specific execution.
            return null;
        }

        if (methodId.contains("nmz"))
        {
            return new CombatRoute(
                    get(1339),
                    get(156),
                    0.0,
                    get(157));
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (methodId.contains("f2p_giants"))
        {
            return new CombatRoute(
                    get(1340),
                    get(158),
                    4.0,
                    get(159));
        }

        if (membership != MembershipStatus.P2P || methodId.contains("f2p"))
        {
            if (level < 20)
            {
                return new CombatRoute(
                        get(160),
                        get(161),
                        4.0,
                        get(163));
            }
            if (level < 40)
            {
                return new CombatRoute(
                        get(164),
                        get(165),
                        4.0,
                        get(166));
            }
            return new CombatRoute(
                    get(167),
                    get(168),
                    4.0,
                    get(169));
        }

        var crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                get(1341),
                get(170),
                4.0,
                get(171));
    }

    private static CombatRoute bestCrab(
            GameData data,
            SessionIntent intent)
    {
        var quests = data == null ? null : data.quests();
        var childrenOfSun = completed(quests, get(1342));
        var boneVoyage = completed(quests, "Bone Voyage");

        if (childrenOfSun && intent == SessionIntent.AFK)
        {
            return new CombatRoute(
                    get(172),
                    get(174),
                    3.5,
                    get(175));
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    get(1343),
                    get(176),
                    4.0,
                    get(177));
        }
        return new CombatRoute(
                get(1341),
                get(178),
                4.0,
                get(179));
    }

    private static String chooseWeapon(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            ItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return firstObserved(items, LOADOUTS.rangedWeapons);
        }

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            return firstObserved(items, LOADOUTS.defenceWeapons);
        }

        if (build == RestrictedBuildType.OBSIDIAN_MAULER)
        {
            return firstObserved(items, LOADOUTS.obsidianWeapons);
        }

        if (skill == Skill.STRENGTH)
        {
            // Whips do not offer a dedicated Strength style, so they are not
            // placed in the Strength list even when one is owned.
            return firstObserved(items, LOADOUTS.strengthWeapons);
        }

        return firstObserved(items, LOADOUTS.meleeWeapons);
    }

    private static String supplyGuidance(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            CombatRoute route,
            String weapon,
            ItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return rangedSupplies(weapon, items);
        }
        if (route.location.contains("Scurrius"))
        {
            var food = firstObserved(items, LOADOUTS.food);
            if (food == null) return null;
            var prayer = firstObserved(items, LOADOUTS.prayer);
            var boost = firstObserved(items, LOADOUTS.boost);
            StringBuilder result = new StringBuilder("Bring ")
                    .append(weapon).append(get(1344))
                    .append(food).append(" food stack");
            if (prayer != null) result.append(", plus ").append(prayer);
            if (boost != null) result.append(" and ").append(boost);
            result.append(get(180));
            return result.toString();
        }
        return "Bring " + weapon
                + get(181);
    }

    private static String rangedSupplies(String weapon, ItemIndex items)
    {
        if (weapon == null) return null;
        if (get(1345).equals(weapon))
            return get(182);
        if (get(1683).equals(weapon)
                || "Venator bow".equals(weapon)) return null;
        if (weapon.contains(get(1346))
                || weapon.contains("Bone crossbow"))
        {
            var bolts = firstObserved(items, "Bone bolts");
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("crossbow"))
        {
            var bolts = firstObserved(items, LOADOUTS.bolts);
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("bow")
                && !weapon.toLowerCase().contains("blowpipe"))
        {
            var arrows = firstObserved(items, LOADOUTS.arrows);
            return arrows == null ? null : "Bring " + weapon + " and " + arrows + ".";
        }
        // Blowpipe, Venator, and other charged/ammo-bearing weapons need live
        // charge evidence before Compass can claim the setup is executable.
        return null;
    }

    private static String attackStyle(Skill skill)
    {
        switch (skill)
        {
            case ATTACK: return get(1317);
            case STRENGTH: return get(1318);
            case DEFENCE: return get(1319);
            case RANGED: return get(1684);
            default: return get(1347) + skill.getName() + " XP";
        }
    }

    private static String firstObserved(ItemIndex items, String... names)
    {
        for (String name : names)
        {
            if (items.has(name)) return name;
        }
        return null;
    }

    private static boolean completed(QuestSnapshot quests, String name)
    {
        return quests != null && quests.statusOf(name) == QuestStatus.COMPLETE;
    }

    private static boolean isDirectCombatSkill(Skill skill)
    {
        return skill == Skill.ATTACK
                || skill == Skill.STRENGTH
                || skill == Skill.DEFENCE
                || skill == Skill.RANGED;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static String trim(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
            return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String withoutPeriod(String value)
    {
        if (value == null) return get(1348);
        var trimmed = value.trim();
        return trimmed.endsWith(".")
                ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static final class CombatRoute
    {
        private final String location;
        private final String loop;
        private final double xpPerDamage;
        private String note;

        private CombatRoute(String location, String loop,
                double xpPerDamage, String note)
        {
            this.location = location;
            this.loop = loop;
            this.xpPerDamage = xpPerDamage;
            this.note = note;
        }
    }

    private static final class Loadouts
    {
        private String[] rangedWeapons, defenceWeapons, obsidianWeapons,
                strengthWeapons, meleeWeapons, food, prayer, boost, bolts,
                arrows;
    }
}

/** Converts any selected method into the same reusable checklist model. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class MethodGuidanceService
{
    private final FarmingRunPlanner farmingRunPlanner;

    public GuidanceChecklist build(
            Recommendation recommendation,
            GameData data)
    {
        if (recommendation == null) return null;
        var plan = recommendation.plan();
        if (plan == null || plan.method() == null) return null;

        var method = plan.method();
        var guidance = recommendation.getGuidance();
        if (method.getSkill() == Skill.FARMING && guidance == null)
        {
            return farmingRunPlanner.build(data, recommendation.id);
        }

        List<GuidanceStep> steps = new ArrayList<>();
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            steps.add(new GuidanceStep(
                    check.id, check.getLabel(), check.getEvidence(),
                    convert(check.getState())));
        }

        if (steps.isEmpty())
        {
            steps.add(new GuidanceStep(
                    "method:ready", "Method ready",
                    Text.get(372),
                    GuidanceStepState.COMPLETE));
        }

        String bring = guidance == null ? null
                : Presentation.compactSentence(
                        guidance.getSupplies(), 120);
        String where = guidance == null ? null
                : Presentation.compactSentence(
                        guidance.getLocation(), 110);
        String action = guidance == null
                ? method.getInstructions()
                : guidance.getAction();
        action = Presentation.compactSentence(action, 135);
        String progress = guidance != null
                && guidance.getProgress() != null
                && !guidance.getProgress().trim().isEmpty()
                ? guidance.getProgress()
                : recommendation.getCurrentLevel() > 0
                && recommendation.getCurrentExecutionTargetLevel()
                        > recommendation.getCurrentLevel()
                ? "Level " + recommendation.getCurrentLevel() + " → "
                        + recommendation.getCurrentExecutionTargetLevel() : null;
        String important = guidance == null ? null
                : guidance.getRiskDisclosure() != null
                ? guidance.getRiskDisclosure().getHeading() + ": "
                        + guidance.getRiskDisclosure().getMessage()
                : criticalNote(guidance.getNote());

        return new GuidanceChecklist(
                recommendation.id, method.getName(),
                plan.getWhyThisMethod(), steps, bring, where, action,
                progress, important);
    }

    private static String criticalNote(String note)
    {
        if (note == null || note.trim().isEmpty()) return null;
        var lower = note.toLowerCase(java.util.Locale.ROOT);
        if (!(lower.contains("wilderness") || lower.contains("hardcore")
                || lower.contains("uim") || lower.contains("iron")
                || lower.contains("restricted") || lower.contains("mandatory")
                || lower.contains(Text.get(1238))
                || lower.contains("irreversible"))) return null;
        return Presentation.compactSentence(note, 135);
    }

    private GuidanceStepState convert(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return GuidanceStepState.COMPLETE;
        if (state == RequirementState.BLOCKED) return GuidanceStepState.BLOCKED;
        return GuidanceStepState.CHECK_NEEDED;
    }
}

/**
 * Builds concrete, account-aware instructions for skill recommendations.
 *
 * <p>Guidance resolves in layers: special burn-aware routes, curated exact
 * execution profiles, variable-XP activity planners, then the universal
 * RuneLite action fallback. Every layer refuses to invent data it cannot prove.</p>
 */
@Singleton
class RecommendationGuidanceService
{
    private static final String LOW_LEVEL_FISH_METHOD = get(1735);
    private static final double LOW_LEVEL_BURN_BUFFER = 2.5;

    private static final List<CookingStage> F2P_EARLY_COOKING = Arrays.asList(
            new CookingStage(1, 5, "sardine", "Raw sardine", 40),
            new CookingStage(5, 15, "herring", "Raw herring", 50),
            new CookingStage(15, 20, "trout", "Raw trout", 70),
            new CookingStage(20, 25, "pike", "Raw pike", 80),
            new CookingStage(25, 30, "salmon", "Raw salmon", 90)
    );

    private final AdaptiveMilestoneGuidanceService adaptiveGuidance;
    private final VariableMethodGuidanceService variableGuidance;
    private final UniversalSkillActionGuidanceService universalGuidance;

    @Inject
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance,
            VariableMethodGuidanceService variableGuidance,
            UniversalSkillActionGuidanceService universalGuidance)
    {
        this.adaptiveGuidance = adaptiveGuidance;
        this.variableGuidance = variableGuidance;
        this.universalGuidance = universalGuidance;
    }

    /** Compatibility constructor retained for focused tests and older callers. */
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance)
    {
        this(adaptiveGuidance,
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    /** Compatibility constructor for focused tests and older callers. */
    public RecommendationGuidanceService()
    {
        this(new AdaptiveMilestoneGuidanceService(),
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan)
    {
        return build(data, skill, currentLevel, targetLevel,
                trainingPlan, true);
    }

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        Guidance uimBronze = uimF2pBronzeGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimBronze != null) return uimBronze;
        Guidance uimCooking = uimF2pCookingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimCooking != null) return uimCooking;
        Guidance uimRunecraft = uimF2pRunecraftGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimRunecraft != null) return uimRunecraft;
        Guidance uimThieving = uimThievingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimThieving != null) return uimThieving;

        Guidance cooking = earlyCookingGuidance(
                data, skill, currentLevel, targetLevel,
                trainingPlan, useGroupStorage);
        if (cooking != null) return cooking;

        Guidance exact = adaptiveGuidance == null
                ? null
                : adaptiveGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (exact != null) return exact;

        Guidance variable = variableGuidance == null
                ? null
                : variableGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (variable != null) return variable;

        return universalGuidance == null
                ? null
                : universalGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
    }

    private static Guidance uimF2pBronzeGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.SMITHING || plan == null
                || plan.method() == null
                || !get(1632).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                get(662)
                        + targetLevel + ".",
                get(673),
                get(684),
                get(686),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pCookingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.COOKING || plan == null
                || plan.method() == null
                || !get(1857).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                get(687)
                        + targetLevel + ".",
                get(688),
                get(689),
                get(690),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pRunecraftGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.RUNECRAFT || plan == null
                || plan.method() == null
                || !get(1858).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        var level = data.account().level(Skill.RUNECRAFT);
        String rune = level >= 20 ? "body" : level >= 14 ? "fire"
                : level >= 9 ? "earth" : level >= 5 ? "water"
                : level >= 2 ? "mind" : "air";
        String altar = level >= 20 ? get(691)
                : level >= 14 ? get(1440)
                : level >= 9 ? get(1441)
                : level >= 5 ? get(1442)
                : level >= 2 ? get(1443)
                : get(1444);
        return new Guidance(
                get(663)
                        + altar + ", craft " + rune
                        + get(664)
                        + targetLevel + ".",
                "Bring the " + rune + get(1285) + rune
                        + get(665),
                get(666) + altar + ".",
                get(667),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimThievingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.THIEVING || plan == null
                || plan.method() == null
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        var id = plan.method().id;
        if (get(1859).equals(id))
            return new Guidance(
                    get(668)
                            + targetLevel + ".",
                    get(669),
                    get(670),
                    get(671),
                    MethodBankingBehavior.NONE);
        if (get(1860).equals(id))
            return new Guidance(
                    get(672)
                            + targetLevel + ".",
                    get(674),
                    get(675),
                    get(676),
                    MethodBankingBehavior.NONE);
        return null;
    }

    private Guidance earlyCookingGuidance(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        if (data == null
                || data.account() == null
                || skill != Skill.COOKING
                || trainingPlan == null
                || trainingPlan.method() == null
                || !LOW_LEVEL_FISH_METHOD.equals(trainingPlan.method().id))
        {
            return null;
        }

        if (currentLevel < 1 || currentLevel >= 30 || targetLevel > 30)
        {
            return null;
        }

        List<StagePlan> stages = buildStages(
                data.account(), currentLevel, targetLevel);
        if (stages.isEmpty()) return null;

        var action = actionGuidance(stages);
        String supplies = supplyGuidance(
                data, data.account(), stages, useGroupStorage);
        var location = locationGuidance(data.quests());
        var note = get(677);

        return new Guidance(
                action, supplies, location, note);
    }

    private static List<StagePlan> buildStages(
            AccountSnapshot account,
            int currentLevel,
            int targetLevel)
    {
        List<StagePlan> plans = new ArrayList<>();
        var currentXp = account.xp(Skill.COOKING);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }

        var stageStartXp = currentXp;
        for (CookingStage stage : F2P_EARLY_COOKING)
        {
            if (stage.endLevel <= currentLevel
                    || stage.startLevel >= targetLevel)
            {
                continue;
            }

            var stageTargetLevel = Math.min(stage.endLevel, targetLevel);
            var stageTargetXp = Experience.getXpForLevel(stageTargetLevel);
            var xpNeeded = Math.max(0, stageTargetXp - stageStartXp);
            var successfulCooks = divideRoundUp(xpNeeded, stage.xpEach);
            int rawNeeded = Math.max(
                    successfulCooks,
                    (int) Math.ceil(successfulCooks * LOW_LEVEL_BURN_BUFFER));

            if (successfulCooks > 0)
            {
                plans.add(new StagePlan(
                        stage, stageTargetLevel, successfulCooks, rawNeeded));
            }

            stageStartXp = stageTargetXp;
            if (stageTargetLevel >= targetLevel) break;
        }
        return plans;
    }

    private static String actionGuidance(List<StagePlan> stages)
    {
        var text = new StringBuilder();
        for (int i = 0; i < stages.size(); i++)
        {
            var stage = stages.get(i);
            if (i > 0) text.append(" Then ");
            text.append("cook ")
                    .append(stage.stage.foodName)
                    .append(" to level ")
                    .append(stage.targetLevel)
                    .append(" (about ")
                    .append(stage.successfulCooks)
                    .append(get(1861))
                    .append(stage.successfulCooks == 1 ? "" : "s")
                    .append(").");
        }
        return capitalize(text.toString());
    }

    private static String supplyGuidance(
            GameData data,
            AccountSnapshot account,
            List<StagePlan> stages,
            boolean useGroupStorage)
    {
        var mode = AccountMode.fromTypeCode(account.modeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
        {
            return "Plan for " + requiredSummary(stages)
                    + get(680);
        }
        var items = new ItemIndex(data, useGroupStorage);
        List<String> ownedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            var verified = items.quantity(stage.stage.rawItemName);
            var missing = Math.max(0, stage.rawNeeded - verified);
            ownedParts.add(verified + " "
                    + stage.stage.rawItemName.toLowerCase());
            if (missing > 0)
                missingParts.add(missing + " "
                        + stage.stage.rawItemName.toLowerCase());
        }

        var text = new StringBuilder();
        text.append("Plan for ").append(requiredSummary(stages));
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            text.append(get(1445));
        else text.append(". Verified: ");
        text.append(joinNatural(ownedParts)).append(".");

        if (missingParts.isEmpty())
        {
            text.append(mode == AccountMode.ULTIMATE_IRONMAN
                    ? get(678) : get(681));
            return text.toString();
        }
        var missing = joinNatural(missingParts);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            text.append(" Acquire ").append(missing).append(get(679));
        else if (mode.usesGrandExchange())
        {
            text.append(" Buy ").append(missing).append(get(1446));
        }
        else if (mode.isGroupIronman())
        {
            text.append(" Source ").append(missing)
                    .append(useGroupStorage
                            ? get(1447)
                            : ".");
        }
        else
        {
            text.append(" Source ").append(missing).append(get(682));
        }
        return text.toString();
    }

    private static String requiredSummary(List<StagePlan> stages)
    {
        List<String> parts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            parts.add("about " + stage.rawNeeded + " "
                    + stage.stage.rawItemName.toLowerCase());
        }
        return joinNatural(parts);
    }

    private static String locationGuidance(QuestSnapshot quests)
    {
        if (quests != null
                && quests.statusOf(get(1862)) == QuestStatus.COMPLETE)
        {
            return get(683);
        }

        return get(685);
    }

    private static String joinNatural(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);

        var text = new StringBuilder();
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

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static final class CookingStage
    {
        private final int startLevel;
        private final int endLevel;
        private final String foodName;
        private final String rawItemName;
        private final int xpEach;

        private CookingStage(
                int startLevel,
                int endLevel,
                String foodName,
                String rawItemName,
                int xpEach)
        {
            this.startLevel = startLevel;
            this.endLevel = endLevel;
            this.foodName = foodName;
            this.rawItemName = rawItemName;
            this.xpEach = xpEach;
        }
    }

    private static final class StagePlan
    {
        private final CookingStage stage;
        private final int targetLevel;
        private final int successfulCooks;
        private final int rawNeeded;

        private StagePlan(
                CookingStage stage,
                int targetLevel,
                int successfulCooks,
                int rawNeeded)
        {
            this.stage = stage;
            this.targetLevel = targetLevel;
            this.successfulCooks = successfulCooks;
            this.rawNeeded = rawNeeded;
        }
    }
}

/**
 * Sailing-specific progression planner.
 *
 * <p>Sailing contains one-off charting XP, variable port tasks, salvaging, and
 * fixed-completion Barracuda Trials. Only the fixed trial completion XP is
 * reduced to an exact repeat count. Other routes receive concrete unlock/tool
 * guidance instead of fake precision.</p>
 */
@Singleton
class SailingGuidanceService
{
    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || plan == null || plan.method() == null)
        {
            return null;
        }
        var account = data.account();
        if (account.membership() != MembershipStatus.P2P)
        {
            return null;
        }

        String id = plan.method().id == null
                ? "" : plan.method().id;
        var currentXp = account.xp(Skill.SAILING);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        if (id.startsWith(get(1760)))
        {
            return barracudaGuidance(data, id, targetLevel, xpNeeded);
        }
        if (get(1949).equals(id))
        {
            return salvageGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if ("sailing_courier".equals(id))
        {
            return courierGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if (get(1950).equals(id))
        {
            return new Guidance(
                    get(1370) + format(xpNeeded)
                            + get(1371) + targetLevel + ".",
                    get(732),
                    get(743),
                    get(754)
            );
        }

        return chartingGuidance(data, currentLevel, targetLevel, xpNeeded);
    }

    private static Guidance barracudaGuidance(
            GameData data,
            String methodId,
            int targetLevel,
            int xpNeeded)
    {
        Trial trial;
        if (methodId.contains("gwenith"))
        {
            trial = new Trial(
                    get(1951),
                    16050,
                    get(765),
                    get(776));
        }
        else if (methodId.contains("jubbly"))
        {
            trial = new Trial(
                    "The Jubbly Jive",
                    6200,
                    get(787),
                    get(788));
        }
        else
        {
            trial = new Trial(
                    get(1372),
                    1250,
                    get(789),
                    get(790));
        }

        var marlinCompletions = divideRoundUp(xpNeeded, trial.marlinXp);
        var action = get(733)
                + trial.name + get(734)
                + marlinCompletions + get(1373)
                + (marlinCompletions == 1 ? "" : "s")
                + " at " + format(trial.marlinXp)
                + get(1374) + format(xpNeeded)
                + " XP to level " + targetLevel + ".";

        var supplies = trial.requirements;
        var note = get(735);
        return new Guidance(
                action, supplies, trial.location, note);
    }

    private static Guidance salvageGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var action = get(736)
                + format(xpNeeded) + get(1952) + targetLevel + ".";
        var supplies = get(737);
        var where = get(738);
        var note = get(739);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance courierGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var action = get(740)
                + format(xpNeeded) + get(1375) + targetLevel + ".";
        var supplies = get(741);
        var where = get(742);
        var note = get(744);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance chartingGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var quests = data.quests();
        if (!complete(quests, "Pandemonium"))
        {
            return new Guidance(
                    get(745),
                    get(746),
                    get(747),
                    get(748)
            );
        }

        var action = new StringBuilder();
        action.append(get(749))
                .append(format(xpNeeded)).append(get(1375))
                .append(targetLevel).append(".");

        var supplies = new StringBuilder(get(750));
        if (currentLevel >= 12 && !complete(quests, "Prying Times"))
        {
            supplies.append(get(751));
        }
        if (currentLevel >= 22 && !complete(quests, "Current Affairs"))
        {
            supplies.append(get(752));
        }
        if (currentLevel >= 15)
        {
            var economy = data.economy();
            if (economy != null
                    && economy.getConfidence() == Confidence.VERIFIED)
            {
                if (economy.getCoins() >= 15000)
                {
                    supplies.append(get(753));
                }
                else
                {
                    supplies.append(" You are ")
                            .append(format(15000 - economy.getCoins()))
                            .append(get(755));
                }
            }
            else
            {
                supplies.append(get(756));
            }
        }

        var where = get(757);
        var note = get(758);
        return new Guidance(
                action.toString(), supplies.toString(), where, note);
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static final class Trial
    {
        private final String name;
        private final int marlinXp;
        private final String requirements;
        private final String location;

        private Trial(String name, int marlinXp, String requirements, String location)
        {
            this.name = name;
            this.marlinXp = marlinXp;
            this.requirements = requirements;
            this.location = location;
        }
    }
}

/** Account-aware Slayer task guidance without inventing task-specific DPS. */
@Singleton
class SlayerGuidanceService
{
    private final SlayerTaskProfileCatalog taskProfiles;

    @Inject
    public SlayerGuidanceService(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null
                ? new SlayerTaskProfileCatalog() : taskProfiles;
    }

    public SlayerGuidanceService()
    {
        this(new SlayerTaskProfileCatalog());
    }

    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel)
    {
        return build(data, currentLevel, targetLevel, true);
    }

    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null) return null;
        var account = data.account();
        if (!AccountBuildPolicy.allowsSkill(account, Skill.SLAYER)) return null;
        if (account.membership() != MembershipStatus.P2P) return null;

        var currentXp = account.xp(Skill.SLAYER);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        var slayer = data.slayer();
        if (slayer != null && slayer.hasTask())
        {
            var profile = taskProfiles.profileFor(slayer.getTaskName());
            var items = new ItemIndex(data, useGroupStorage);
            var action = taskAction(slayer, profile, xpNeeded, targetLevel);
            var supplies = taskSupplies(account, items, profile);
            var where = taskLocation(slayer, profile);
            var note = taskNote(account, profile);
            return new Guidance(action, supplies, where, note);
        }

        var master = bestMaster(account, data.quests());
        var action = get(1452) + master.name
                + ". You need " + format(xpNeeded)
                + get(1453) + targetLevel + ".";
        var supplies = get(759);
        var note = master.reason + get(760);
        return new Guidance(action, supplies, master.location, note);
    }

    private static String taskAction(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile,
            int xpNeeded,
            int targetLevel)
    {
        var action = new StringBuilder();
        action.append(get(1454))
                .append(slayer.getTaskName())
                .append(" assignment: ")
                .append(slayer.getRemaining())
                .append(get(1455))
                .append(format(xpNeeded))
                .append(get(1453))
                .append(targetLevel).append(".");
        if (profile != null && hasText(profile.getStyleGuidance()))
        {
            action.append(" ").append(profile.getStyleGuidance());
        }
        return action.toString();
    }

    private static String taskSupplies(
            AccountSnapshot account,
            ItemIndex items,
            SlayerTaskProfile profile)
    {
        if (profile == null || profile.getRequiredProtection().isEmpty())
        {
            return get(761);
        }

        var required = profile.getRequiredProtection();
        var owned = firstOwned(items, required);
        if (owned != null)
        {
            return get(1456) + owned
                    + get(762);
        }

        var mode = AccountMode.fromTypeCode(account.modeCode());
        var choices = joinChoices(required);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            var restricted = restrictedOwned(items, required);
            if (restricted > 0)
            {
                return get(763)
                        + choices
                        + get(764);
            }
            return get(766)
                    + choices
                    + get(767);
        }

        if (!items.primaryOwnershipObserved())
        {
            return get(768)
                    + choices + ".";
        }

        if (mode.isIronLike())
        {
            return get(769)
                    + choices + ".";
        }
        return get(770)
                + choices
                + get(771);
    }

    private static String taskLocation(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile)
    {
        if (hasText(slayer.getTaskLocation()))
        {
            return get(1457)
                    + slayer.getTaskLocation()
                    + get(772);
        }
        if (profile != null && hasText(profile.getPreferredLocation()))
        {
            return profile.getPreferredLocation();
        }
        if (hasText(slayer.getMasterName()))
        {
            return get(1458)
                    + slayer.getMasterName()
                    + get(773);
        }
        return get(774);
    }

    private static String taskNote(AccountSnapshot account,
            SlayerTaskProfile profile)
    {
        var base = get(775);
        if (profile == null) return base;
        var note = new StringBuilder();
        if (hasText(profile.getMechanicsNote()))
        {
            note.append(profile.getMechanicsNote()).append(" ");
        }
        if (profile.getMultiTargetMagicEligibility() == CapabilityState.VERIFIED)
            note.append(get(777));
        if (profile.getCannonEligibility() == CapabilityState.UNKNOWN)
            note.append(get(778));
        if (profile.isWildernessVariantKnown())
            note.append(get(779));
        if (AccountMode.fromTypeCode(account.modeCode()).isIronLike()
                && !profile.getIronObjectives().isEmpty())
            note.append(get(1953)).append(String.join(", ",
                    profile.getIronObjectives())).append(". ");
        if (hasText(profile.getTaskDecisionGuidance()))
            note.append(profile.getTaskDecisionGuidance()).append(" ");
        return note.append(base).toString();
    }

    private static String firstOwned(
            ItemIndex items,
            List<String> candidates)
    {
        for (String candidate : candidates)
        {
            if (items.has(candidate)) return candidate;
        }
        return null;
    }

    private static int restrictedOwned(
            ItemIndex items,
            List<String> candidates)
    {
        var total = 0;
        for (String candidate : candidates)
        {
            total += items.restrictedQuantity(candidate);
        }
        return total;
    }

    private static String joinChoices(List<String> choices)
    {
        var text = new StringBuilder();
        for (int i = 0; i < choices.size(); i++)
        {
            if (i > 0) text.append(i == choices.size() - 1 ? " or " : ", ");
            text.append(choices.get(i));
        }
        return text.toString();
    }

    private static SlayerMasterChoice bestMaster(
            AccountSnapshot account,
            QuestSnapshot quests)
    {
        var combat = combatLevel(account);
        var slayer = account.level(Skill.SLAYER);

        if (combat >= 100 && slayer >= 50 && complete(quests, "Shilo Village"))
            return new SlayerMasterChoice("Duradel/Kuradal", "Shilo Village",
                    get(780));
        if (combat >= 85)
            return new SlayerMasterChoice("Nieve/Steve", get(1459),
                    get(781));
        if (combat >= 75)
            return new SlayerMasterChoice("Konar quo Maten", "Mount Karuulm",
                    get(782));
        if (combat >= 70 && complete(quests, "Lost City"))
            return new SlayerMasterChoice("Chaeldar", "Zanaris",
                    get(783));
        if (combat >= 40)
            return new SlayerMasterChoice("Vannaka", get(1954),
                    get(784));
        if (combat >= 20 && complete(quests, "Priest in Peril"))
            return new SlayerMasterChoice("Mazchna/Achtryn", "Canifis",
                    get(785));
        return new SlayerMasterChoice("Turael/Aya", "Burthorpe",
                get(786));
    }

    /** Mirrors the standard OSRS combat-level formula closely enough for gates. */
    static int combatLevel(AccountSnapshot account)
    {
        double base = 0.25 * (account.level(Skill.DEFENCE)
                + account.level(Skill.HITPOINTS)
                + Math.floor(account.level(Skill.PRAYER) / 2.0));
        double melee = 0.325 * (account.level(Skill.ATTACK)
                + account.level(Skill.STRENGTH));
        var ranged = 0.325 * Math.floor(account.level(Skill.RANGED) * 1.5);
        var magic = 0.325 * Math.floor(account.level(Skill.MAGIC) * 1.5);
        return (int) Math.floor(base + Math.max(melee, Math.max(ranged, magic)));
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static final class SlayerMasterChoice
    {
        private final String name;
        private final String location;
        private final String reason;

        private SlayerMasterChoice(String name, String location, String reason)
        {
            this.name = name;
            this.location = location;
            this.reason = reason;
        }
    }
}

/**
 * Safe fallback over RuneLite's maintained skill-calculator action data.
 *
 * <p>Curated method profiles remain preferred. This service lets any other
 * deterministic RuneLite action become exact milestone guidance when its route
 * match, membership, and material recipe can all be proven.</p>
 */
@Singleton
class UniversalSkillActionGuidanceService
{
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "train", "training", "best", "sensible", "practical", "use",
            "f2p", "p2p", "expanded", "method", "route", "for", "with",
            "and", "the", "from", "into", "while", "when", "your", "account",
            "high", "low", "level", "fast", "active", "relaxed", "efficient"));

    private static final Set<String> GENERIC_ROUTE_IDS = new HashSet<>(Arrays.asList(
            get(1577),
            get(1578),
            get(1579)));

    private static final String F2P_ANVIL_ROUTE =
            get(1007);

    private final RuneLiteSkillActionCatalog actionCatalog;
    private final UniversalActionRecipeResolver recipeResolver;
    private final SkillingXpModifierService xpModifierService;
    private final AccountResourcePlanner resourcePlanner;

    @Inject
    public UniversalSkillActionGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            UniversalActionRecipeResolver recipeResolver,
            SkillingXpModifierService xpModifierService,
            AccountResourcePlanner resourcePlanner)
    {
        this.actionCatalog = actionCatalog;
        this.recipeResolver = recipeResolver;
        this.xpModifierService = xpModifierService;
        this.resourcePlanner = resourcePlanner;
    }

    public UniversalSkillActionGuidanceService()
    {
        this(new RuneLiteSkillActionCatalog(),
                new UniversalActionRecipeResolver(),
                new SkillingXpModifierService(),
                new AccountResourcePlanner());
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
                || plan == null || plan.method() == null
                || !supportsUniversalAction(skill))
        {
            return null;
        }

        var currentXp = data.account().xp(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);
        if (xpNeeded <= 0) return null;

        SkillingXpModifier modifier = xpModifierService == null
                ? SkillingXpModifier.none()
                : xpModifierService.modifier(data, skill, useGroupStorage);
        var multiplier = Math.max(1.0, modifier.getMultiplier());

        Choice choice = choose(
                data,
                actionCatalog.actionsFor(skill),
                plan.method(),
                currentLevel,
                xpNeeded,
                multiplier,
                useGroupStorage);
        if (choice == null) return null;

        var action = choice.action;
        var xpEach = action.getXp() * multiplier;
        var actions = divideRoundUp(xpNeeded, xpEach);
        UniversalActionRecipe recipe = recipeResolver.resolve(
                action, actions, data.account().membership());
        if (requiresExactRecipe(skill) && !recipe.hasExactInputs()) return null;

        String progressText;
        if (skill == Skill.RUNECRAFT)
        {
            progressText = format(xpNeeded) + get(1272)
                    + pluralRunes(action.getName()) + " with about "
                    + format(actions) + get(1273)
                    + targetLevel + ".";
        }
        else
        {
            progressText = format(xpNeeded) + get(1274)
                    + format(actions) + " "
                    + playerAction(skill, action.getName(), actions)
                    + " to level " + targetLevel + ".";
        }

        SupplyPlan resources = resourcePlanner == null
                ? null
                : resourcePlanner.plan(data, recipe.getInputs(), useGroupStorage);
        String supplies;
        if (recipe.getInputs().isEmpty())
        {
            supplies = recipe.hasExactInputs()
                    ? get(1013)
                    : get(1014);
        }
        else
        {
            supplies = resources == null ? null : resources.getGuidance();
        }

        if (skill == Skill.RUNECRAFT)
        {
            var altarEntry = runecraftEntryInstruction(action.getName());
            if (altarEntry != null)
            {
                supplies = supplies == null || supplies.trim().isEmpty()
                        ? altarEntry
                        : altarEntry + " " + supplies;
            }
        }

        String location = locationBeforeColon(
                plan.method().getInstructions());
        String actionText = executionAction(skill, action,
                plan.method().getInstructions());
        if (isAnvilSmithingMethod(plan.method()))
        {
            location = F2P_ANVIL_ROUTE;
        }

        var note = new StringBuilder();
        note.append(get(1015))
                .append(get(1016));
        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note.append(get(1275))
                    .append(modifier.getLabel()).append(" is worn.");
        }
        if (recipe.getSetup() != null && !recipe.getSetup().trim().isEmpty())
        {
            note.append(" ").append(recipe.getSetup());
        }
        if (skill == Skill.COOKING)
        {
            note.append(get(1017));
        }
        if (resources != null
                && resources.accountMode() == AccountMode.ULTIMATE_IRONMAN
                && resources.getTotalMissingUnits() > 0)
        {
            note.append(get(1018));
        }

        return new Guidance(
                actionText, supplies, location, note.toString())
                .withProgress(progressText);
    }

    private static String executionAction(Skill skill,
            ActionDef action, String instructions)
    {
        if (skill == Skill.RUNECRAFT)
            return "Craft " + pluralRunes(action.getName())
                    + get(1019);
        var loop = actionAfterColon(instructions);
        if (loop == null) loop = instructions;
        if (loop == null || loop.trim().isEmpty())
            return "Repeat " + action.getName() + ".";
        if (Names.actionText(loop).contains(Names.actionText(action.getName()))) return loop;
        return action.getName() + ": " + loop;
    }

    private static String locationBeforeColon(String instructions)
    {
        if (instructions == null) return null;
        var colon = instructions.indexOf(':');
        if (colon < 3) return instructions;
        return instructions.substring(0, colon).trim() + ".";
    }

    private static String actionAfterColon(String instructions)
    {
        if (instructions == null) return null;
        var colon = instructions.indexOf(':');
        if (colon < 0 || colon + 1 >= instructions.length()) return null;
        var value = instructions.substring(colon + 1).trim();
        return value.isEmpty() ? null
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private Choice choose(
            GameData data,
            List<ActionDef> actions,
            TrainingMethod method,
            int currentLevel,
            int xpNeeded,
            double multiplier,
            boolean useGroupStorage)
    {
        if (actions == null || actions.isEmpty()) return null;
        var membership = data.account().membership();
        var routeTokens = routeTokens(method);
        var observed = new ItemIndex(data, useGroupStorage);
        var genericRoute = GENERIC_ROUTE_IDS.contains(method.id);
        var anvilSmithing = isAnvilSmithingMethod(method);
        Choice best = null;

        for (ActionDef action : actions)
        {
            if (action == null || action.getXp() <= 0
                    || action.getLevel() > currentLevel
                    || !membershipAllowed(action, membership)
                    || isOneTimeOrRewardAction(action)
                    || (action.getSkill() == Skill.RUNECRAFT
                            && !Names.actionText(action.getName()).endsWith(" rune"))
                    || (anvilSmithing && isBarSmeltingAction(action)))
            {
                continue;
            }

            int actionsNeeded = divideRoundUp(
                    xpNeeded, action.getXp() * multiplier);
            UniversalActionRecipe recipe = recipeResolver.resolve(
                    action, actionsNeeded, membership);
            if (requiresExactRecipe(action.getSkill()) && !recipe.hasExactInputs())
            {
                continue;
            }

            var matches = routeMatchCount(routeTokens, action);
            if (matches == 0 && !genericRoute) continue;

            var score = matches * 1000.0;
            score += Math.min(300.0, action.getLevel() * 3.0);
            score += Math.min(250.0, Math.log1p(action.getXp()) * 35.0);
            score += resourceCoverageScore(data, observed, recipe);

            if (best == null || score > best.score)
            {
                best = new Choice(action, score);
            }
        }
        return best;
    }

    private static double resourceCoverageScore(
            GameData data,
            ItemIndex observed,
            UniversalActionRecipe recipe)
    {
        if (recipe == null || recipe.getInputs().isEmpty()) return 0.0;
        var required = 0;
        var owned = 0;
        for (MethodInput input : recipe.getInputs())
        {
            required += input.getQuantity();
            owned += Math.min(input.getQuantity(), observed.quantity(input.getName()));
        }
        if (required <= 0) return 0.0;

        var coverage = Math.min(1.0, owned / (double) required);
        AccountMode mode = AccountMode.fromTypeCode(
                data.account().modeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            return coverage * 650.0 + (coverage <= 0.0 ? -280.0 : 0.0);
        }
        if (mode.isIronLike())
        {
            return coverage * 500.0 + (coverage <= 0.0 ? -220.0 : 0.0);
        }
        return coverage * 120.0;
    }

    private static boolean supportsUniversalAction(Skill skill)
    {
        return skill != Skill.ATTACK
                && skill != Skill.STRENGTH
                && skill != Skill.DEFENCE
                && skill != Skill.RANGED
                && skill != Skill.HITPOINTS
                && skill != Skill.SLAYER
                && skill != Skill.SAILING;
    }

    private static boolean requiresExactRecipe(Skill skill)
    {
        return skill == Skill.COOKING
                || skill == Skill.CRAFTING
                || skill == Skill.FLETCHING
                || skill == Skill.FIREMAKING
                || skill == Skill.HERBLORE
                || skill == Skill.SMITHING
                || skill == Skill.CONSTRUCTION
                || skill == Skill.MAGIC
                || skill == Skill.PRAYER
                || skill == Skill.RUNECRAFT;
    }

    private static boolean isAnvilSmithingMethod(TrainingMethod method)
    {
        if (method == null || method.getSkill() != Skill.SMITHING) return false;
        var name = Names.actionText(method.getName());
        var instructions = Names.actionText(method.getInstructions());
        return name.startsWith("smith ")
                || name.startsWith("smithing ")
                || instructions.contains("anvil");
    }

    private static boolean isBarSmeltingAction(ActionDef action)
    {
        return action != null
                && action.getSkill() == Skill.SMITHING
                && Names.actionText(action.getName()).endsWith(" bar");
    }

    private static boolean membershipAllowed(
            ActionDef action,
            MembershipStatus account)
    {
        if (account == MembershipStatus.F2P)
        {
            return action.getMembership() == MembershipStatus.F2P;
        }
        if (account == MembershipStatus.P2P)
        {
            return action.getMembership() == MembershipStatus.F2P
                    || action.getMembership() == MembershipStatus.P2P;
        }
        return false;
    }

    private static boolean isOneTimeOrRewardAction(ActionDef action)
    {
        String text = Names.actionText(action.getName() + " "
                + action.getCategory() + " " + action.id);
        return containsAny(text,
                "quest reward", "experience lamp", "xp lamp", "diary reward",
                get(1276), get(1580), "genie lamp",
                "museum quiz", "one time", "one-time", "tears of guthix");
    }

    private static Set<String> routeTokens(TrainingMethod method)
    {
        Set<String> result = new HashSet<>();
        if (method == null) return result;
        var skillToken = stem(Names.actionText(method.getSkill().getName()));
        String text = Names.actionText(method.id + " "
                + method.getName() + " " + method.getInstructions());
        for (String token : text.split("[^a-z0-9]+"))
        {
            if (token.length() < 3 || STOP_WORDS.contains(token)
                    || stem(token).equals(skillToken)) continue;
            result.add(stem(token));
        }
        return result;
    }

    private static int routeMatchCount(
            Set<String> routeTokens,
            ActionDef action)
    {
        String text = Names.actionText(action.getName() + " "
                + action.getCategory() + " " + action.id);
        Set<String> actionTokens = new HashSet<>();
        for (String token : text.split("[^a-z0-9]+"))
        {
            if (token.length() >= 3) actionTokens.add(stem(token));
        }
        var matches = 0;
        for (String token : routeTokens)
        {
            if (actionTokens.contains(token)) matches++;
        }
        return matches;
    }

    private static String stem(String token)
    {
        var value = token == null ? "" : token.toLowerCase(Locale.ROOT);
        if (value.endsWith("ies") && value.length() > 4)
            return value.substring(0, value.length() - 3) + "y";
        if (value.endsWith("ing") && value.length() > 5)
            return value.substring(0, value.length() - 3);
        if (value.endsWith("es") && value.length() > 4)
            return value.substring(0, value.length() - 2);
        if (value.endsWith("s") && value.length() > 3)
            return value.substring(0, value.length() - 1);
        return value;
    }

    private static String pluralRunes(String actionName)
    {
        var name = actionName == null ? "runes" : actionName.trim();
        if (name.isEmpty()) return "runes";
        var lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" runes")) return name;
        if (lower.endsWith(" rune")) return name + "s";
        return name;
    }

    private static String runecraftEntryInstruction(String actionName)
    {
        var lower = Names.actionText(actionName);
        if (lower.contains("air rune"))
            return get(1020);
        if (lower.contains("mind rune"))
            return get(1008);
        if (lower.contains("water rune"))
            return get(1009);
        if (lower.contains("earth rune"))
            return get(1010);
        if (lower.contains("fire rune"))
            return get(1011);
        if (lower.contains("body rune"))
            return get(1012);
        return null;
    }

    private static String playerAction(Skill skill, String actionName, int count)
    {
        var name = actionName == null ? "" : actionName.trim();
        if (skill == Skill.WOODCUTTING)
        {
            var tree = name.replaceAll("(?i)\\s+logs?$", "").trim();
            if (tree.isEmpty()) tree = "tree";
            return tree.toLowerCase(Locale.ROOT) + " chop" + (count == 1 ? "" : "s");
        }
        if (skill == Skill.FISHING)
            return name + " catch" + (count == 1 ? "" : "es");
        if (skill == Skill.MINING)
            return name + " mining action" + (count == 1 ? "" : "s");
        if (skill == Skill.COOKING)
            return name + " cook" + (count == 1 ? "" : "s");
        if (skill == Skill.SMITHING)
        {
            if (Names.actionText(name).endsWith(" bar"))
                return name + " smelt" + (count == 1 ? "" : "s");
            return name + get(1581) + (count == 1 ? "" : "s");
        }

        String singular;
        switch (skill)
        {
            case AGILITY: singular = get(1277); break;
            case THIEVING: singular = get(1278); break;
            case HUNTER: singular = "catch"; break;
            case FIREMAKING: singular = "burn"; break;
            case PRAYER: singular = "Prayer action"; break;
            case RUNECRAFT: singular = "craft"; break;
            case CRAFTING: singular = "craft"; break;
            case FLETCHING: singular = "fletch"; break;
            case HERBLORE: singular = "potion action"; break;
            case CONSTRUCTION: singular = "build"; break;
            case FARMING: singular = "Farming action"; break;
            case MAGIC: singular = "cast"; break;
            default: singular = "action"; break;
        }
        return name + " " + (count == 1 ? singular : singular + "s");
    }

    private static int divideRoundUp(int numerator, double denominator)
    {
        if (numerator <= 0 || denominator <= 0) return 0;
        return (int) Math.ceil(numerator / denominator);
    }

    private static boolean containsAny(String text, String... values)
    {
        for (String value : values)
        {
            if (value != null && text.contains(value)) return true;
        }
        return false;
    }


    private static String format(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
            return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    private static final class Choice
    {
        private final ActionDef action;
        private final double score;

        private Choice(ActionDef action, double score)
        {
            this.action = action;
            this.score = score;
        }
    }
}

/** Renders variable-output methods from bundled templates and live account variables. */
@Singleton
class VariableMethodGuidanceService
{
    private static final Profile[] PROFILES = BundledCatalogLoader.array(
            get(1057), Profile[].class);
    private static final FarmingAccessEvaluator FARMING =
            new FarmingAccessEvaluator(new FarmingAccessCatalog());

    public Guidance build(GameData data, Skill skill, int currentLevel,
            int targetLevel, TrainingPlan plan, boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.method() == null) return null;
        var currentXp = data.account().xp(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        Map<String, String> common = new HashMap<>();
        common.put("xp", format(Math.max(0,
                Experience.getXpForLevel(targetLevel) - currentXp)));
        common.put("target", Integer.toString(targetLevel));
        var items = new ItemIndex(data, useGroupStorage);
        for (Profile profile : PROFILES)
        {
            if (!profile.matches(plan.method().id)) continue;
            var values = variables(profile, data, items);
            if (values == null) return null;
            values.putAll(common);
            values.putIfAbsent("observed", observed(items, profile.observed));
            values.putIfAbsent("pickaxe", tool(items, true));
            return new Guidance(profile.render(profile.action, values),
                    profile.render(profile.supplies, values),
                    profile.render(profile.location, values),
                    profile.render(profile.note, values));
        }
        return null;
    }

    private static Map<String, String> variables(Profile profile,
            GameData data, ItemIndex items)
    {
        Map<String, String> v = new HashMap<>();
        var kind = profile.kind;
        if (kind == null) return v;
        var farming = data.account().level(Skill.FARMING);
        switch (kind)
        {
            case "tempoross":
                String harpoon = first(items, "Dragon harpoon", "Crystal harpoon",
                        get(1570), "Harpoon");
                v.put(get(1571), harpoon == null ? get(1079)
                        : "Bring " + harpoon + get(1090));
                break;
            case "gotr":
                String pouches = observed(items, "Small pouch", "Medium pouch",
                        "Large pouch", "Giant pouch", "Colossal pouch");
                v.put("pouches", pouches.isEmpty() ? "" : " " + pouches);
                break;
            case "stars":
                boolean members = data.account().membership()
                        == MembershipStatus.P2P;
                v.put("starScout", get(members ? 1058 : 1059));
                v.put("starLocation", get(members ? 1062 : 1063));
                break;
            case "foundry":
                var alloy = alloy(items);
                if (alloy == null) return null;
                v.put("alloy", alloy);
                break;
            case "homes":
                String[] contract = contract(data.account().level(
                        Skill.CONSTRUCTION), items);
                if (contract == null) return null;
                v.put("contract", contract[0]);
                v.put("plank", contract[1]);
                v.put("plankLower", contract[1].toLowerCase(Locale.ROOT));
                break;
            case "tithe":
                v.put("seed", farming >= 74 ? "Logavano"
                        : farming >= 54 ? "Bologano" : "Golovanova");
                break;
            case "allotments":
                var seed = tier(items, farming, 6, ALLOTMENTS);
                var patch = FARMING.firstReachablePatchName(data.farming());
                if (seed == null || patch == null) return null;
                v.put("seedLower", seed.toLowerCase(Locale.ROOT));
                v.put("patch", patch);
                v.put("observed", observed(items, seed, "Seed dibber", "Spade",
                        "Rake", get(1478), "Gricoller's can"));
                break;
            case "herbs":
                var herb = tier(items, farming, 1, HERBS);
                patch = FARMING.firstReachableHerbPatchName(data.farming());
                if (herb == null || patch == null) return null;
                v.put("herbSeed", "one " + herb.toLowerCase(Locale.ROOT));
                v.put("patch", patch);
                v.put("observed", observed(items, "Seed dibber", "Spade",
                        get(1478), "Magic secateurs", "Seed box"));
                break;
            case "contracts":
                v.put("contract", farming >= 85 ? "hard"
                        : farming >= 65 ? "medium" : "easy");
                break;
            case "rumours":
                var hunter = data.account().level(Skill.HUNTER);
                boolean master = hunter >= 91 && data.quests() != null
                        && data.quests().statusOf("At First Light")
                        == QuestStatus.COMPLETE;
                v.put("rumourTier", master ? "Master" : hunter >= 72 ? "Expert"
                        : hunter >= 57 ? "Adept" : "Novice");
                v.put("hunter", master ? get(1572) : hunter >= 72
                        ? get(1573) : hunter >= 57
                        ? get(1482) : get(1574));
                break;
            case "forestry":
                var level = data.account().level(Skill.WOODCUTTING);
                v.put("tree", level >= 60 ? "yew trees" : level >= 45
                        ? "maple trees" : level >= 30 ? "willow trees" : "oak trees");
                v.put("treeLocation", get(level >= 60 ? 1094
                        : level >= 45 ? 1095 : level >= 30 ? 1096 : 1097));
                v.put("axe", tool(items, false));
                break;
            default: break;
        }
        return v;
    }

    private static final String[][] ALLOTMENTS = {
            {get(1575), "61"}, {"Watermelon seed", "47"},
            {"Strawberry seed", "31"}, {"Sweetcorn seed", "20"},
            {"Tomato seed", "12"}, {"Cabbage seed", "7"},
            {"Onion seed", "5"}, {"Potato seed", "1"}};
    private static final String[][] HERBS = {
            {"Torstol seed", "85"}, {"Dwarf weed seed", "79"},
            {"Lantadyme seed", "73"}, {"Cadantine seed", "67"},
            {"Snapdragon seed", "62"}, {"Kwuarm seed", "56"},
            {"Avantoe seed", "50"}, {"Irit seed", "44"},
            {"Toadflax seed", "38"}, {"Ranarr seed", "32"},
            {get(1576), "26"}, {"Tarromin seed", "19"},
            {"Marrentill seed", "14"}, {"Guam seed", "9"}};

    private static String tier(ItemIndex items, int level, int quantity,
            String[][] tiers)
    {
        for (String[] tier : tiers)
            if (level >= Integer.parseInt(tier[1])
                    && items.quantity(tier[0]) >= quantity) return tier[0];
        return null;
    }

    private static String alloy(ItemIndex items)
    {
        String[] metals = {"Runite bar", "Adamantite bar", "Mithril bar",
                "Steel bar", "Iron bar"};
        for (int i = 0; i < metals.length - 1; i++)
            if (items.quantity(metals[i]) >= 14
                    && items.quantity(metals[i + 1]) >= 14)
                return "14 " + metals[i].toLowerCase(Locale.ROOT) + " and 14 "
                        + metals[i + 1].toLowerCase(Locale.ROOT);
        for (String metal : metals)
            if (items.quantity(metal) >= 28)
                return "28 " + metal.toLowerCase(Locale.ROOT);
        return null;
    }

    private static String[] contract(int level, ItemIndex items)
    {
        String[][] tiers = {{"Expert", "Mahogany plank", "70"},
                {"Adept", "Teak plank", "50"}, {"Novice", "Oak plank", "20"},
                {"Beginner", "Plank", "1"}};
        for (String[] tier : tiers)
            if (level >= Integer.parseInt(tier[2])
                    && items.quantity(tier[1]) >= 15) return tier;
        return null;
    }

    private static String tool(ItemIndex items, boolean pickaxe)
    {
        var suffix = pickaxe ? "pickaxe" : "axe";
        for (String metal : new String[]{"Crystal", "Infernal", "Dragon", "Rune",
                "Adamant", "Mithril", "Black", "Steel", "Iron", "Bronze"})
            if (items.has(metal + " " + suffix)) return metal + " " + suffix;
        return get(pickaxe ? 1100 : 1102);
    }

    private static String first(ItemIndex items, String... names)
    {
        for (String name : names) if (items.has(name)) return name;
        return null;
    }

    private static String observed(ItemIndex items, String... names)
    {
        if (names == null) return "";
        List<String> found = new ArrayList<>();
        for (String name : names)
        {
            var quantity = items.quantity(name);
            if (quantity > 0) found.add(quantity + "x " + name);
        }
        return found.isEmpty() ? "" : "Observed: " + String.join(", ", found) + ".";
    }

    private static String format(long value) { return String.format("%,d", value); }

    private static final class Profile
    {
        private String[] ids, observed;
        private String action, supplies, location, note, tool, kind;

        private boolean matches(String id)
        {
            if (ids != null) for (String value : ids) if (value.equals(id)) return true;
            return false;
        }

        private String render(String template, Map<String, String> values)
        {
            var result = template == null ? "" : template;
            for (Map.Entry<String, String> value : values.entrySet())
                result = result.replace("{" + value.getKey() + "}", value.getValue());
            return result.trim();
        }
    }
}
