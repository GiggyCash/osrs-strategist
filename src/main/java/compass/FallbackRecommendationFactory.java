package compass;
import static compass.Text.get;

import net.runelite.api.Skill;

/** Honest, harmless actions used only when the ranked pool cannot lead. */
final class FallbackRecommendationFactory
{
    static final String PREFIX = "fallback:";

    private FallbackRecommendationFactory() {}

    static Recommendation forState(GameData data)
    {
        if (data == null || data.account() == null)
            return fallback("login", get(1305),
                    get(219),
                    get(1306),
                    get(1307),
                    get(230));

        if (data.inventory() == null)
            return fallback("inventory", get(1308),
                    get(237),
                    get(1306),
                    get(238),
                    get(239));

        if (data.equipment() == null)
            return fallback("equipment", get(1309),
                    get(240),
                    get(1306),
                    get(241),
                    get(242));

        AccountMode mode = AccountMode.fromTypeCode(
                data.account().modeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
            return fallback("bank", "Open your bank",
                    get(243),
                    get(1306),
                    get(220),
                    get(221));

        var account = data.account();
        if (AccountBuildPolicy.allowsSkill(account, Skill.MINING))
        {
            var items = new ItemIndex(data, false);
            boolean hasPickaxe = items.quantityMatching(
                    ItemRequirementClass.PICKAXE,
                    java.util.Collections.emptyList()) > 0;
            if (!hasPickaxe)
            {
                return fallback("starter-pickaxe", get(1310),
                        get(222),
                        get(1306),
                        get(223),
                        get(224));
            }
            var current = Math.max(1, account.level(Skill.MINING));
            var target = Math.min(99, current + 1);
            var maxed = current >= 99;
            return fallback("starter-mining",
                    maxed ? get(1311)
                            : get(1312) + target,
                    get(225)
                            + (maxed ? get(1313) : "at level " + target + "."),
                    get(226),
                    get(227),
                    get(228));
        }

        var combatSkill = firstTrainableMeleeSkill(account);
        if (combatSkill != null)
        {
            var current = Math.max(1, account.level(combatSkill));
            var target = Math.min(99, current + 1);
            return fallback("safe-combat-" + combatSkill.name().toLowerCase(),
                    "Train " + combatSkill.getName() + " to " + target,
                    get(1314) + attackStyle(combatSkill)
                            + get(229)
                            + target + get(1315),
                    get(1316),
                    get(231),
                    get(232));
        }

        return fallback("safe-combat", "Fight 10 monks",
                get(233),
                get(1316),
                get(234),
                get(235));
    }

    static boolean isFallback(Recommendation recommendation)
    {
        return recommendation != null && recommendation.id != null
                && recommendation.id.startsWith(PREFIX);
    }

    private static Skill firstTrainableMeleeSkill(AccountSnapshot account)
    {
        Skill[] skills = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE};
        for (Skill skill : skills)
            if (AccountBuildPolicy.allowsSkill(account, skill)
                    && account.level(skill) < 99) return skill;
        return null;
    }

    private static String attackStyle(Skill skill)
    {
        if (skill == Skill.ATTACK) return get(1317);
        if (skill == Skill.STRENGTH) return get(1318);
        return get(1319);
    }

    private static Recommendation fallback(String id, String title,
            String action, String supplies, String location, String reason)
    {
        return new Recommendation(PREFIX + id, title, reason,
                Double.NEGATIVE_INFINITY, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance(action, supplies, location,
                        get(236)),
                SafetyEvidence.harmless(true));
    }
}
