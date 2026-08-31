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
                    Text.get(219),
                    "No supplies required.",
                    "RuneScape login screen.",
                    Text.get(230));

        if (data.getInventory() == null)
            return fallback("inventory", "Open your inventory",
                    Text.get(237),
                    "No supplies required.",
                    Text.get(238),
                    Text.get(239));

        if (data.getEquipment() == null)
            return fallback("equipment", "Open your equipment tab",
                    Text.get(240),
                    "No supplies required.",
                    Text.get(241),
                    Text.get(242));

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.getBank() == null)
            return fallback("bank", "Open your bank",
                    Text.get(243),
                    "No supplies required.",
                    Text.get(220),
                    Text.get(221));

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
                        Text.get(222),
                        "No supplies required.",
                        Text.get(223),
                        Text.get(224));
            }
            int current = Math.max(1, account.getSkillLevel(Skill.MINING));
            int target = Math.min(99, current + 1);
            boolean maxed = current >= 99;
            return fallback("starter-mining",
                    maxed ? "Mine one inventory of copper"
                            : "Mine copper to level " + target,
                    Text.get(225)
                            + (maxed ? "after one inventory." : "at level " + target + "."),
                    Text.get(226),
                    Text.get(227),
                    Text.get(228));
        }

        Skill combatSkill = firstTrainableMeleeSkill(account);
        if (combatSkill != null)
        {
            int current = Math.max(1, account.getSkillLevel(combatSkill));
            int target = Math.min(99, current + 1);
            return fallback("safe-combat-" + combatSkill.name().toLowerCase(),
                    "Train " + combatSkill.getName() + " to " + target,
                    "Set the combat style to " + attackStyle(combatSkill)
                            + Text.get(229)
                            + target + ". Ask a monk to heal you when needed.",
                    "No weapon or food required.",
                    Text.get(231),
                    Text.get(232));
        }

        return fallback("safe-combat", "Fight 10 monks",
                Text.get(233),
                "No weapon or food required.",
                Text.get(234),
                Text.get(235));
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
                        Text.get(236)),
                CandidateSafetyEvidence.harmless(true));
    }
}
