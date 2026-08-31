package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Honest, harmless actions used only when the ranked pool cannot lead. */
final class FallbackRecommendationFactory
{
    static final String PREFIX = "fallback:";

    private FallbackRecommendationFactory() {}

    static Recommendation forState(GameData data)
    {
        if (data == null || data.account() == null)
            return fallback("login", Text.get(1305),
                    Text.get(219),
                    Text.get(1306),
                    Text.get(1307),
                    Text.get(230));

        if (data.inventory() == null)
            return fallback("inventory", Text.get(1308),
                    Text.get(237),
                    Text.get(1306),
                    Text.get(238),
                    Text.get(239));

        if (data.equipment() == null)
            return fallback("equipment", Text.get(1309),
                    Text.get(240),
                    Text.get(1306),
                    Text.get(241),
                    Text.get(242));

        AccountMode mode = AccountMode.fromTypeCode(
                data.account().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
            return fallback("bank", "Open your bank",
                    Text.get(243),
                    Text.get(1306),
                    Text.get(220),
                    Text.get(221));

        AccountSnapshot account = data.account();
        if (AccountBuildPolicy.allowsSkill(account, Skill.MINING))
        {
            ItemIndex items = new ItemIndex(data, false);
            boolean hasPickaxe = items.quantityMatching(
                    ItemRequirementClass.PICKAXE,
                    java.util.Collections.emptyList()) > 0;
            if (!hasPickaxe)
            {
                return fallback("starter-pickaxe", Text.get(1310),
                        Text.get(222),
                        Text.get(1306),
                        Text.get(223),
                        Text.get(224));
            }
            int current = Math.max(1, account.getSkillLevel(Skill.MINING));
            int target = Math.min(99, current + 1);
            boolean maxed = current >= 99;
            return fallback("starter-mining",
                    maxed ? Text.get(1311)
                            : Text.get(1312) + target,
                    Text.get(225)
                            + (maxed ? Text.get(1313) : "at level " + target + "."),
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
                    Text.get(1314) + attackStyle(combatSkill)
                            + Text.get(229)
                            + target + Text.get(1315),
                    Text.get(1316),
                    Text.get(231),
                    Text.get(232));
        }

        return fallback("safe-combat", "Fight 10 monks",
                Text.get(233),
                Text.get(1316),
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
        if (skill == Skill.ATTACK) return Text.get(1317);
        if (skill == Skill.STRENGTH) return Text.get(1318);
        return Text.get(1319);
    }

    private static Recommendation fallback(String id, String title,
            String action, String supplies, String location, String reason)
    {
        return new Recommendation(PREFIX + id, title, reason,
                Double.NEGATIVE_INFINITY, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance(action, supplies, location,
                        Text.get(236)),
                SafetyEvidence.harmless(true));
    }
}
