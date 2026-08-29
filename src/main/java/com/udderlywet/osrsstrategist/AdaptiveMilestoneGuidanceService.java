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
                    ? "Get a free bronze pickaxe from the Mining tutor at the east Lumbridge Swamp mine."
                    : "Bring your " + pickaxe + ".";
        }
        if (methodId.startsWith("woodcutting_"))
        {
            String axe = firstObserved(items,
                    "Crystal axe", "Infernal axe", "Dragon axe", "Rune axe",
                    "Adamant axe", "Mithril axe", "Black axe", "Steel axe",
                    "Iron axe", "Bronze axe");
            return axe == null
                    ? "Buy a bronze axe from Bob's Brilliant Axes in Lumbridge before starting."
                    : "Bring your " + axe + ".";
        }
        if ("fishing_f2p_fly".equals(methodId))
            return items.has("Fly fishing rod") && items.has("Feather")
                    ? "Bring your fly fishing rod and feathers."
                    : "Buy a fly fishing rod and feather packs from Gerrant's Fishy Business in Port Sarim before walking to Barbarian Village.";
        if ("fishing_lumbridge_shrimps".equals(methodId))
            return "Bring a small fishing net; the Fishing tutor beside the spots supplies one when needed.";
        if ("hunter_bird_traps".equals(methodId))
            return items.has("Bird snare")
                    ? "Bring one bird snare."
                    : "Buy one bird snare from Aleck's Hunter Emporium in Yanille before walking south to the Hunter area.";
        if ("hunter_falconry".equals(methodId))
            return "Bring 500 coins. Unequip weapon, shield, and gloves before renting the gyr falcon from Matthias.";
        if ("hunter_salamanders".equals(methodId))
        {
            int traps = currentLevel >= 60 ? 4 : currentLevel >= 40 ? 3 : 2;
            return "Bring " + traps + " small fishing nets and " + traps
                    + " ropes; set every available net trap and drop each catch.";
        }
        if ("magic_f2p_combat".equals(methodId))
            return "Bring one mind rune and one air rune per Wind Strike; if short, buy both from Aubury's Rune Shop just south of Varrock East Bank.";
        if ("magic_f2p_fire_bolt".equals(methodId))
            return "Bring three air runes, four fire runes, and one chaos rune per Fire Bolt; buy shortfalls from Aubury's Rune Shop just south of Varrock East Bank.";
        if ("magic_f2p_fire_blast".equals(methodId))
            return "Bring four air runes, five fire runes, and one death rune per Fire Blast; buy shortfalls from Aubury's Rune Shop just south of Varrock East Bank.";
        if ("magic_f2p_curse".equals(methodId))
            return "Bring one body rune, three earth runes, and two water runes per Curse. Equip full metal armour plus vambraces or a cursed goblin staff until the equipment panel shows -64 Magic attack or lower.";
        if ("magic_f2p_fire_strike_splash".equals(methodId))
            return "Bring two air runes, three fire runes, and one mind rune per Fire Strike. Keep the verified cursed-staff metal setup equipped and enable Fire Strike autocast.";
        if ("construction_crude_chairs".equals(methodId)
                || "construction_oak_larders".equals(methodId))
            return "Bring a hammer and saw.";
        if ("thieving_lumbridge_people".equals(methodId))
            return "Bring five cooked shrimp for failed pickpockets; fish and cook them in Lumbridge first if needed.";
        if ("thieving_ardy_knights".equals(methodId))
            return "Bring food for failed pickpockets; restock at Ardougne South Bank when needed.";
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
                return "Red salamander net-trap trees south of the Ourania Cave entrance.";
            if (actionName.contains("orange salamander"))
                return "The southern three net-trap trees in the Uzer Hunter area, east of the desert bridge.";
            return "The western swamp-lizard net-trap trees in the Canifis Hunter area, east of Canifis.";
        }
        if ("magic_f2p_curse".equals(methodId)
                || "magic_f2p_fire_strike_splash".equals(methodId))
        {
            return "The caged Monk of Zamorak under the Varrock Palace stairs.";
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
                return "Your current safe non-Wilderness tile; process only the immediately usable carried materials named in DO.";
            }
            if ("firemaking_f2p_logs".equals(methodId))
            {
                return "Grand Exchange south-east corner: burn the immediately usable carried logs named in DO in east-to-west rows.";
            }
        }
        return fallback;
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
}
