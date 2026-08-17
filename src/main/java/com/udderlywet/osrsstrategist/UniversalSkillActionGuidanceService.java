package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Safe fallback over RuneLite's maintained skill-calculator action data.
 *
 * <p>Curated method profiles remain preferred. This service lets any other
 * deterministic RuneLite action become exact milestone guidance when its route
 * match, membership, and material recipe can all be proven.</p>
 */
@Singleton
public class UniversalSkillActionGuidanceService
{
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "train", "training", "best", "sensible", "practical", "use",
            "f2p", "p2p", "expanded", "method", "route", "for", "with",
            "and", "the", "from", "into", "while", "when", "your", "account",
            "high", "low", "level", "fast", "active", "relaxed", "efficient"));

    private static final Set<String> GENERIC_ROUTE_IDS = new HashSet<>(Arrays.asList(
            "cooking_hosidius",
            "firemaking_campfires",
            "herblore_low_potions"));

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

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null || skill == null
                || plan == null || plan.getMethod() == null
                || !supportsUniversalAction(skill))
        {
            return null;
        }

        int currentXp = data.getAccount().getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);
        if (xpNeeded <= 0) return null;

        SkillingXpModifier modifier = xpModifierService == null
                ? SkillingXpModifier.none()
                : xpModifierService.modifier(data, skill, useGroupStorage);
        double multiplier = Math.max(1.0, modifier.getMultiplier());

        Choice choice = choose(
                data,
                actionCatalog.actionsFor(skill),
                plan.getMethod(),
                currentLevel,
                xpNeeded,
                multiplier,
                useGroupStorage);
        if (choice == null) return null;

        RuneLiteSkillActionDefinition action = choice.action;
        double xpEach = action.getXp() * multiplier;
        int actions = divideRoundUp(xpNeeded, xpEach);
        UniversalActionRecipe recipe = recipeResolver.resolve(
                action, actions, data.getAccount().getMembershipStatus());
        if (requiresExactRecipe(skill) && !recipe.hasExactInputs()) return null;

        String actionText = "Do " + format(actions) + " "
                + actionUnit(skill, actions) + " using " + action.getName()
                + " to cover the remaining " + format(xpNeeded)
                + " XP to level " + targetLevel + ". " + action.getName()
                + " gives " + format(xpEach)
                + " modeled XP per successful action.";

        AccountResourcePlan resources = resourcePlanner == null
                ? null
                : resourcePlanner.plan(data, recipe.getInputs(), useGroupStorage);
        String supplies;
        if (recipe.getInputs().isEmpty())
        {
            supplies = recipe.hasExactInputs()
                    ? "No consumed material is required for the modeled action."
                    : "The action count is known, but Strategist is not claiming an exact material list yet.";
        }
        else
        {
            supplies = resources == null ? null : resources.getGuidance();
        }

        String location = plan.getMethod().getInstructions();
        StringBuilder note = new StringBuilder();
        note.append("Action XP comes from RuneLite's maintained skill-calculator data. ")
                .append("Strategist only exposes this fallback after membership and recipe checks pass.");
        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note.append(" Count assumes the ")
                    .append(modifier.getLabel()).append(" is worn.");
        }
        if (recipe.getSetup() != null && !recipe.getSetup().trim().isEmpty())
        {
            note.append(" ").append(recipe.getSetup());
        }
        if (skill == Skill.COOKING)
        {
            note.append(" Successful-cook count is deterministic, but raw-food supply can require a burn buffer. Low-level F2P fish use the dedicated burn-aware planner.");
        }
        if (resources != null
                && resources.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                && resources.getTotalMissingUnits() > 0)
        {
            note.append(" UIM material totals exclude retrieval-only storage unless a separate retrieval step is deliberately selected.");
        }

        return new RecommendationGuidance(
                actionText, supplies, location, note.toString());
    }

    private Choice choose(
            StrategyDataBundle data,
            List<RuneLiteSkillActionDefinition> actions,
            TrainingMethod method,
            int currentLevel,
            int xpNeeded,
            double multiplier,
            boolean useGroupStorage)
    {
        if (actions == null || actions.isEmpty()) return null;
        MembershipStatus membership = data.getAccount().getMembershipStatus();
        Set<String> routeTokens = routeTokens(method);
        ObservedItemIndex observed = new ObservedItemIndex(data, useGroupStorage);
        boolean genericRoute = GENERIC_ROUTE_IDS.contains(method.getId());
        Choice best = null;

        for (RuneLiteSkillActionDefinition action : actions)
        {
            if (action == null || action.getXp() <= 0
                    || action.getLevel() > currentLevel
                    || !membershipAllowed(action, membership)
                    || isOneTimeOrRewardAction(action))
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

            int matches = routeMatchCount(routeTokens, action);
            if (matches == 0 && !genericRoute) continue;

            double score = matches * 1000.0;
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
            StrategyDataBundle data,
            ObservedItemIndex observed,
            UniversalActionRecipe recipe)
    {
        if (recipe == null || recipe.getInputs().isEmpty()) return 0.0;
        long required = 0;
        long owned = 0;
        for (ResolvedMethodInput input : recipe.getInputs())
        {
            required += input.getQuantity();
            owned += Math.min(input.getQuantity(), observed.quantity(input.getName()));
        }
        if (required <= 0) return 0.0;

        double coverage = Math.min(1.0, owned / (double) required);
        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
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

    private static boolean membershipAllowed(
            RuneLiteSkillActionDefinition action,
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

    private static boolean isOneTimeOrRewardAction(RuneLiteSkillActionDefinition action)
    {
        String text = normalize(action.getName() + " "
                + action.getCategory() + " " + action.getId());
        return containsAny(text,
                "quest reward", "experience lamp", "xp lamp", "diary reward",
                "tome of experience", "book of knowledge", "genie lamp",
                "museum quiz", "one time", "one-time", "tears of guthix");
    }

    private static Set<String> routeTokens(TrainingMethod method)
    {
        Set<String> result = new HashSet<>();
        if (method == null) return result;
        String text = normalize(method.getId() + " "
                + method.getName() + " " + method.getInstructions());
        for (String token : text.split("[^a-z0-9]+"))
        {
            if (token.length() < 3 || STOP_WORDS.contains(token)) continue;
            result.add(stem(token));
        }
        return result;
    }

    private static int routeMatchCount(
            Set<String> routeTokens,
            RuneLiteSkillActionDefinition action)
    {
        String text = normalize(action.getName() + " "
                + action.getCategory() + " " + action.getId());
        Set<String> actionTokens = new HashSet<>();
        for (String token : text.split("[^a-z0-9]+"))
        {
            if (token.length() >= 3) actionTokens.add(stem(token));
        }
        int matches = 0;
        for (String token : routeTokens)
        {
            if (actionTokens.contains(token)) matches++;
        }
        return matches;
    }

    private static String stem(String token)
    {
        String value = token == null ? "" : token.toLowerCase(Locale.ROOT);
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

    private static String actionUnit(Skill skill, int count)
    {
        String singular;
        switch (skill)
        {
            case AGILITY: singular = "successful course/obstacle action"; break;
            case MINING: singular = "successful mine"; break;
            case FISHING: singular = "successful catch"; break;
            case WOODCUTTING: singular = "successful cut"; break;
            case THIEVING: singular = "successful steal/pickpocket"; break;
            case HUNTER: singular = "successful catch"; break;
            case COOKING: singular = "successful cook"; break;
            case FIREMAKING: singular = "successful burn"; break;
            case PRAYER: singular = "Prayer action"; break;
            case RUNECRAFT: singular = "essence action"; break;
            case CRAFTING: singular = "craft"; break;
            case FLETCHING: singular = "fletch"; break;
            case SMITHING: singular = "smith/smelt action"; break;
            case HERBLORE: singular = "potion action"; break;
            case CONSTRUCTION: singular = "build"; break;
            case FARMING: singular = "Farming action"; break;
            case MAGIC: singular = "cast"; break;
            default: singular = "action"; break;
        }
        return count == 1 ? singular : singular + "s";
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

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    private static String format(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
            return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    private static final class Choice
    {
        private final RuneLiteSkillActionDefinition action;
        private final double score;

        private Choice(RuneLiteSkillActionDefinition action, double score)
        {
            this.action = action;
            this.score = score;
        }
    }
}
