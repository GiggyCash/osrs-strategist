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
                    "Log in to RuneScape to load your account state.",
                    "No supplies required.",
                    "RuneScape login screen.",
                    "No character state is currently available.");

        if (data.getInventory() == null)
            return fallback("inventory", "Open your inventory",
                    "Open your inventory tab to verify carried supplies.",
                    "No supplies required.",
                    "Inventory tab in the RuneScape side panel.",
                    "Your carried items have not been observed yet.");

        if (data.getEquipment() == null)
            return fallback("equipment", "Open your equipment tab",
                    "Open your equipment tab to verify your current setup.",
                    "No supplies required.",
                    "Worn Equipment tab in the RuneScape side panel.",
                    "Your equipped items have not been observed yet.");

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.getBank() == null)
            return fallback("bank", "Open your bank",
                    "Open your bank once to verify available supplies.",
                    "No supplies required.",
                    "Lumbridge Castle bank, on the top floor.",
                    "No bank snapshot has been observed for this account.");

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
                        "Talk to the Mining tutor at the east Lumbridge Swamp mine and ask for a bronze pickaxe. Keep it in your inventory so Compass can advance the plan when it is observed.",
                        "No supplies required.",
                        "East Lumbridge Swamp mine, beside the Mining tutor.",
                        "No higher-value route is executable with the currently observed supplies, so this obtains a free reusable starter tool.");
            }
            int current = Math.max(1, account.getSkillLevel(Skill.MINING));
            int target = Math.min(99, current + 1);
            boolean maxed = current >= 99;
            return fallback("starter-mining",
                    maxed ? "Mine one inventory of copper"
                            : "Mine copper to level " + target,
                    "Ask the Mining tutor for a bronze pickaxe if needed, mine copper at the east Lumbridge Swamp mine, drop the ore when full, and stop "
                            + (maxed ? "after one inventory." : "at level " + target + "."),
                    "Bronze pickaxe; the Mining tutor at the mine supplies one when needed.",
                    "East Lumbridge Swamp mine, beside the Mining tutor.",
                    "No higher-value route passed the current access, resource, and safety checks, so Compass is using a verified no-cost F2P recovery route.");
        }

        Skill combatSkill = firstTrainableMeleeSkill(account);
        if (combatSkill != null)
        {
            int current = Math.max(1, account.getSkillLevel(combatSkill));
            int target = Math.min(99, current + 1);
            return fallback("safe-combat-" + combatSkill.name().toLowerCase(),
                    "Train " + combatSkill.getName() + " to " + target,
                    "Set the combat style to " + attackStyle(combatSkill)
                            + ", fight unarmed monks, and stop at level "
                            + target + ". Ask a monk to heal you when needed.",
                    "No weapon or food required.",
                    "Edgeville Monastery, west of Edgeville.",
                    "This no-cost F2P route is used only when no higher-value plan passes the current build and resource checks.");
        }

        return fallback("safe-combat", "Fight 10 monks",
                "Fight 10 unarmed monks with the current build-legal combat style. Ask a monk to heal you when needed, then stop.",
                "No weapon or food required.",
                "Edgeville Monastery, west of Edgeville.",
                "This no-cost F2P route is used only when no higher-value plan passes the current build and resource checks.");
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
                        "This fallback reports only account state that has not been observed."),
                CandidateSafetyEvidence.harmless(true));
    }
}
