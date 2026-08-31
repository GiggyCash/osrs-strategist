package com.udderlywet.osrsstrategist;

import java.util.*;
import net.runelite.api.Skill;

/** Rejects known placeholder-shaped copy before it can reach player cards. */
final class RecommendationQualityPolicy
{
    private static final List<String> GENERIC_TITLES =
            PolicyLists.list(PolicyLists.DATA.generic_titles);
    private static final List<String> GENERIC_ACTIONS =
            PolicyLists.list(PolicyLists.DATA.generic_actions);
    private static final List<String> GENERIC_LOCATIONS =
            PolicyLists.list(PolicyLists.DATA.generic_locations);
    private static final List<String> UNRESOLVED_SUPPLIES =
            PolicyLists.list(PolicyLists.DATA.unresolved_supplies);

    boolean isPresentable(Recommendation recommendation)
    {
        if (recommendation == null || !hasText(recommendation.getTitle())) return false;
        if (isGenericTitle(recommendation.getTitle()))
            return false;
        Guidance guidance = recommendation.getGuidance();
        if (guidance == null || !hasText(guidance.getAction())) return false;
        if (containsAny(guidance.getAction(), GENERIC_ACTIONS)) return false;
        if (containsAny(guidance.getLocation(), GENERIC_ACTIONS)) return false;
        if (containsAny(guidance.getLocation(), GENERIC_LOCATIONS)) return false;
        if (containsAny(guidance.getSupplies(), UNRESOLVED_SUPPLIES)) return false;

        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null)
        {
            if (plan.getMethod() == null
                    || !hasText(plan.getMethod().getName())
                    || !hasText(guidance.getLocation())) return false;
            if (containsAny(plan.getMethod().getName(), GENERIC_ACTIONS))
            {
                return false;
            }
        }
        return coherentRuneRoute(plan, guidance);
    }

    private static boolean coherentRuneRoute(
            TrainingPlan plan, Guidance guidance)
    {
        if (plan == null || plan.getMethod() == null
                || plan.getMethod().getSkill() != net.runelite.api.Skill.RUNECRAFT)
        {
            return true;
        }
        String method = normalize(plan.getMethod().getName());
        String action = normalize(guidance.getAction());
        String[] runes = {"air", "mind", "water", "earth", "fire", "body"};
        for (String rune : runes)
        {
            if (method.contains(rune + " rune"))
                return action.contains(rune + " rune");
        }
        return true;
    }

    private static boolean containsAny(String value, List<String> needles)
    {
        String normalized = normalize(value);
        if (normalized.isEmpty()) return false;
        for (String needle : needles)
            if (normalized.contains(needle)) return true;
        return false;
    }

    private static boolean isGenericTitle(String value)
    {
        String normalized = normalize(value);
        if (GENERIC_TITLES.contains(normalized)) return true;
        for (Skill skill : Skill.values())
        {
            if (normalized.equals("train " + normalize(skill.getName())))
                return true;
        }
        return false;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }
}
