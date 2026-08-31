package com.udderlywet.osrsstrategist;

import java.util.*;

/**
 * Separates unresolved knowledge from ordinary preparation work.
 *
 * <p>A recommendation may still be actionable when Compass knows the route
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

        // Domain evaluators reserve preparation:* for a fully understood,
        // reversible setup such as fitting specified boat parts. Unknown area,
        // quest, risk, and live-assignment gates retain their typed IDs.
        if (id.startsWith("preparation:"))
        {
            String evidence = normalize(check.getEvidence());
            return !evidence.contains("unknown")
                    && !evidence.contains("additional access/risk")
                    && !evidence.contains("cannot be observed");
        }

        // ResourceReadinessService uses typed resource:* checks. A known tool
        // or material shortfall is ordinary preparation, not uncertainty about
        // whether a quest, region, spellbook, or activity is available.
        if (id.startsWith("resource:"))
        {
            String evidence = normalize(check.getEvidence());
            // Retrieval-only UIM storage is not ordinary shopping/banking
            // preparation. The route must first model its extra access or
            // death-risk setup explicitly.
            return !evidence.contains("additional access/risk preconditions")
                    && !evidence.contains("verify that route");
        }
        if (!id.startsWith("generic:")) return false;

        // A combined label such as "Ourania route and essence supply" is an
        // access check, not ordinary shopping/banking preparation. Check hard
        // gates first so one supply word cannot accidentally make an unknown
        // quest, area, activity, room, assignment, or risk gate actionable.
        if (containsAny(label,
                "access", "unlock", "quest", "completion", "completed",
                "route", "contract", "spellbook", "activity", "minigame",
                "region", "guild", "poh", "room", "course", "team",
                "patch", "reachable",
                "role", "assignment", "task", "location", "habitat",
                "target", "risk accepted"))
        {
            return false;
        }

        // These words describe resources or ordinary setup that can be acquired
        // as part of the recommendation. Access/unlock requirements deliberately
        // do not appear in this list and therefore stay hard CHECK_NEEDED gates.
        return containsAny(label,
                "supply", "supplies", "food", "healing", "rune", "runes",
                "ammo", "ammunition", "arrow", "bolt", "dart", "cannonball",
                "chinchompa", "bone", "bones", "head", "heads",
                "essence", "talisman", "tiara", "binding necklace",
                "ore", "bars", "metal", "log", "logs", "plank",
                "planks", "herb", "herbs", "secondary", "secondaries",
                "seed", "seeds", "compost", "payment", "payments",
                "potion", "potions", "grape", "grapes", "jug",
                "dynamite", "tick-manipulation supplies", "warm clothing",
                "knife", "hammer", "saw", "chisel", "rope", "bucket",
                "pickaxe", "hatchet", "axe", "harpoon", "fishing rod",
                "fly fishing rod", "net", "lobster pot", "cage", "snare",
                "talisman", "tiara",
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
