package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;

/**
 * Separates unresolved knowledge from ordinary preparation work.
 *
 * <p>A recommendation may still be actionable when Strategist knows the route
 * but the player needs to obtain consumables or ordinary tools first. That is
 * different from not knowing whether a quest, area, spellbook, activity, or
 * irreversible build requirement is unlocked.</p>
 */
public final class RequirementActionability
{
    private RequirementActionability()
    {
    }

    public static boolean isActionablePreparation(
            TrainingPlan plan,
            RecommendationGuidance guidance)
    {
        if (plan == null || plan.getMethod() == null
                || guidance == null || !hasText(guidance.getAction()))
        {
            return false;
        }

        List<RequirementCheck> checks = plan.getRequirementChecks();
        if (checks == null || checks.isEmpty()) return true;

        boolean hasPreparation = false;
        for (RequirementCheck check : checks)
        {
            if (check == null) return false;
            if (check.getState() == RequirementState.BLOCKED) return false;
            if (check.getState() == RequirementState.VERIFIED) continue;
            if (!isPreparationRequirement(check)) return false;
            hasPreparation = true;
        }

        // If preparation is outstanding, the recommendation must actually tell
        // the player what supplies/setup to prepare instead of hiding the gap.
        return !hasPreparation || hasText(guidance.getSupplies());
    }

    public static boolean hasHardUnresolvedRequirement(TrainingPlan plan)
    {
        if (plan == null || plan.getRequirementChecks() == null) return false;
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check == null) return true;
            if (check.getState() == RequirementState.BLOCKED) return true;
            if (check.getState() == RequirementState.CHECK_NEEDED
                    && !isPreparationRequirement(check))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isPreparationRequirement(RequirementCheck check)
    {
        if (check == null || check.getState() != RequirementState.CHECK_NEEDED)
        {
            return false;
        }

        String id = normalize(check.getId());
        String label = normalize(check.getLabel());
        if (!id.startsWith("generic:")) return false;

        // These words describe resources or ordinary setup that can be acquired
        // as part of the recommendation. Access/unlock requirements deliberately
        // do not appear in this list and therefore stay hard CHECK_NEEDED gates.
        return containsAny(label,
                "supply", "supplies", "food", "healing", "rune", "runes",
                "ammo", "ammunition", "arrow", "bolt", "dart", "cannonball",
                "chinchompa", "bone", "bones", "head", "heads",
                "essence", "talisman", "tiara", "binding necklace",
                "ore", "bar", "bars", "metal", "log", "logs", "plank",
                "planks", "herb", "herbs", "secondary", "secondaries",
                "seed", "seeds", "compost", "payment", "payments",
                "potion", "potions", "grape", "grapes", "jug",
                "dynamite", "tick-manipulation supplies", "warm clothing",
                "knife", "hammer", "saw", "chisel", "rope", "bucket",
                "pickaxe", "hatchet", "axe", "harpoon", "fishing rod",
                "fly fishing rod", "net", "lobster pot", "cage",
                "teleport setup", "teleports", "stamina", "repair supplies");
    }

    private static boolean containsAny(String value, String... needles)
    {
        for (String needle : needles)
        {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
