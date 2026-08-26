package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.runelite.api.Skill;

/** Rejects known placeholder-shaped copy before it can reach player cards. */
final class RecommendationQualityPolicy
{
    private static final List<String> GENERIC_TITLES = Arrays.asList(
            "train smithing", "train fishing", "train prayer",
            "train combat", "get better gear", "do some quests",
            "get supplies");
    private static final List<String> GENERIC_ACTIONS = Arrays.asList(
            "begin the verified activity",
            "do the verified action",
            "use the verified method",
            "choose the best",
            "choose a suitable",
            "choose whichever",
            "craft whichever",
            "choose a populated",
            "use a reachable",
            "use a nearby",
            "whatever",
            "best reachable",
            "best sensible",
            "best practical",
            "best useful",
            "best available",
            "highest practical",
            "highest hunter rumour",
            "highest mahogany homes contract",
            "highest fruit seed tier",
            "best commission-compatible alloy",
            "prioritize orders",
            "fits the selected",
            "train this skill",
            "get supplies",
            "upgrade gear");

    private static final List<String> GENERIC_LOCATIONS = Arrays.asList(
            "any bank",
            "a nearby",
            "nearby range",
            "nearby furnace",
            "nearby-bank location",
            "an anvil",
            "a furnace",
            "an altar",
            "a fishing spot",
            "a training area",
            "f2p anvil",
            "rune altar",
            "good training area",
            "active farming checklist",
            "active clue location",
            "quest's established replacement",
            "exact non-wilderness route recorded",
            "suitable location",
            "choose a populated",
            "fits the selected",
            "use a discovered star location",
            "unlocked varlamore",
            "highest shipwreck tier",
            "verified route associated",
            "verified task-valid location",
            "nearby reachable",
            "nearby low-risk",
            "safest reachable non-wilderness location",
            "best available location");

    private static final List<String> UNRESOLVED_SUPPLIES = Arrays.asList(
            "selected allotment seed",
            "selected herb seed",
            "suitable seed",
            "matching observed weapon",
            "appropriate to the account",
            "appropriate to your account",
            "food appropriate",
            "best build-legal armour",
            "best legal weapon",
            "your best usable",
            "your preferred",
            "matching plank tier",
            "no relevant setup item",
            "matching ammunition",
            "strongest build-legal",
            "selected weapon",
            "spare eligible herbs",
            "preserving herbs needed",
            "choose the",
            "whichever");

    boolean isPresentable(Recommendation recommendation)
    {
        if (recommendation == null || !hasText(recommendation.getTitle())) return false;
        if (isGenericTitle(recommendation.getTitle()))
            return false;
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (guidance == null || !hasText(guidance.getAction())) return false;
        if (containsAny(guidance.getAction(), GENERIC_ACTIONS)) return false;
        if (containsAny(guidance.getLocation(), GENERIC_LOCATIONS)) return false;
        if (containsAny(guidance.getSupplies(), UNRESOLVED_SUPPLIES)) return false;

        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null)
        {
            if (plan.getMethod() == null || !hasText(guidance.getLocation())) return false;
            if (containsAny(plan.getMethod().getName(), GENERIC_ACTIONS))
            {
                return false;
            }
        }
        return coherentRuneRoute(plan, guidance);
    }

    private static boolean coherentRuneRoute(
            TrainingPlan plan, RecommendationGuidance guidance)
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
