package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Honest, harmless actions used only when the ranked pool cannot lead. */
final class FallbackRecommendationFactory
{
    static final String PREFIX = "fallback:";

    private FallbackRecommendationFactory() {}

    static Recommendation forState(StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null)
            return fallback("login", "Log in to continue",
                    PlayerText.get("FRF1"),
                    "No supplies required.",
                    "RuneScape login screen.",
                    PlayerText.get("FRF2"));

        if (data.getInventory() == null)
            return fallback("inventory", "Open your inventory",
                    PlayerText.get("FRF3"),
                    "No supplies required.",
                    PlayerText.get("FRF4"),
                    PlayerText.get("FRF5"));

        if (data.getEquipment() == null)
            return fallback("equipment", "Open your equipment tab",
                    PlayerText.get("FRF6"),
                    "No supplies required.",
                    PlayerText.get("FRF7"),
                    PlayerText.get("FRF8"));

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.getBank() == null)
            return fallback("bank", "Open your bank",
                    PlayerText.get("FRF9"),
                    "No supplies required.",
                    PlayerText.get("FRF10"),
                    PlayerText.get("FRF11"));

        AccountSnapshot account = data.getAccount();
        if (AccountBuildPolicy.allowsSkill(account, Skill.MINING))
        {
            ObservedItemIndex items = new ObservedItemIndex(data, false);
            boolean hasPickaxe = items.quantityMatching(
                    ItemRequirementClass.PICKAXE,
                    java.util.Collections.emptyList()) > 0;
            if (!hasPickaxe)
            {
                return fallback("starter-pickaxe", "Get a bronze pickaxe",
                        PlayerText.get("FRF12"),
                        "No supplies required.",
                        PlayerText.get("FRF13"),
                        PlayerText.get("FRF14"));
            }
            int current = Math.max(1, account.getSkillLevel(Skill.MINING));
            int target = Math.min(99, current + 1);
            boolean maxed = current >= 99;
            return fallback("starter-mining",
                    maxed ? "Mine one inventory of copper"
                            : "Mine copper to level " + target,
                    PlayerText.get("FRF15")
                            + (maxed ? "after one inventory." : "at level " + target + "."),
                    PlayerText.get("FRF16"),
                    PlayerText.get("FRF17"),
                    PlayerText.get("FRF18"));
        }

        Skill combatSkill = firstTrainableMeleeSkill(account);
        if (combatSkill != null)
        {
            int current = Math.max(1, account.getSkillLevel(combatSkill));
            int target = Math.min(99, current + 1);
            return fallback("safe-combat-" + combatSkill.name().toLowerCase(),
                    "Train " + combatSkill.getName() + " to " + target,
                    "Set the combat style to " + attackStyle(combatSkill)
                            + PlayerText.get("FRF19")
                            + target + ". Ask a monk to heal you when needed.",
                    "No weapon or food required.",
                    PlayerText.get("FRF20"),
                    PlayerText.get("FRF21"));
        }

        return fallback("safe-combat", "Fight 10 monks",
                PlayerText.get("FRF22"),
                "No weapon or food required.",
                PlayerText.get("FRF23"),
                PlayerText.get("FRF24"));
    }

    static boolean isFallback(Recommendation recommendation)
    {
        return recommendation != null && recommendation.getId() != null
                && recommendation.getId().startsWith(PREFIX);
    }

    private static Skill firstTrainableMeleeSkill(AccountSnapshot account)
    {
        Skill[] skills = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE};
        for (Skill skill : skills)
            if (AccountBuildPolicy.allowsSkill(account, skill)
                    && account.getSkillLevel(skill) < 99) return skill;
        return null;
    }

    private static String attackStyle(Skill skill)
    {
        if (skill == Skill.ATTACK) return "Accurate / Attack XP";
        if (skill == Skill.STRENGTH) return "Aggressive / Strength XP";
        return "Defensive / Defence XP";
    }

    private static Recommendation fallback(String id, String title,
            String action, String supplies, String location, String reason)
    {
        return new Recommendation(PREFIX + id, title, reason,
                Double.NEGATIVE_INFINITY, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance(action, supplies, location,
                        PlayerText.get("FRF25")),
                CandidateSafetyEvidence.harmless(true));
    }
}
