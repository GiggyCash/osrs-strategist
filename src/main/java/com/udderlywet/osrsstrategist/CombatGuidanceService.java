package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Concrete combat guidance for Attack, Strength, Defence, and Ranged.
 *
 * <p>Combat is not treated like Cooking. Misses give no skill XP and target
 * choice, weapon speed, quest access, account builds, and monster XP modifiers
 * matter. Compass therefore gives an exact remaining-XP/damage target only
 * when the selected route has ordinary per-damage XP. Special encounters keep
 * exact XP remaining but do not invent a fake kill count.</p>
 */
@Singleton
public class CombatGuidanceService
{
    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            SessionIntent sessionIntent,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null || skill == null
                || plan == null || plan.getMethod() == null
                || !isDirectCombatSkill(skill))
        {
            return null;
        }

        AccountSnapshot account = data.getAccount();
        if (account.getMembershipStatus() == MembershipStatus.UNKNOWN) return null;
        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        if (!AccountBuildPolicy.allowsSkill(account, skill)) return null;

        String methodId = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId().toLowerCase();
        CombatRoute route = chooseRoute(
                data, account, skill, currentLevel, build,
                methodId, sessionIntent);
        if (route == null) return null;

        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
        String weapon = chooseWeapon(account, skill, build, items);
        if (weapon == null && skill != Skill.RANGED
                && build == RestrictedBuildType.STANDARD
                && currentLevel < 20)
        {
            return new RecommendationGuidance(
                    PlayerText.get("CGS1"),
                    "No coins or other supplies required.",
                    PlayerText.get("CGS2"),
                    PlayerText.get("CGS3"));
        }
        boolean unarmed = weapon == null && currentLevel < 10
                && skill != Skill.RANGED;
        if (weapon == null && !unarmed) return null;
        String style = attackStyle(skill);

        int currentXp = account.getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        StringBuilder action = new StringBuilder();
        action.append(withoutPeriod(route.loop));
        if (weapon != null) action.append(" with ").append(weapon);
        else if (unarmed) action.append(" while unarmed");
        action.append(" on ").append(style).append(". ");
        action.append(format(xpNeeded)).append(" ")
                .append(skill.getName()).append(" XP remains to level ")
                .append(targetLevel).append(".");

        if (route.xpPerDamage > 0)
        {
            int damageNeeded = (int) Math.ceil(xpNeeded / route.xpPerDamage);
            action.append(" That is about ")
                    .append(format(damageNeeded))
                    .append(PlayerText.get("CGS4"))
                    .append(trim(route.xpPerDamage))
                    .append(" XP per damage.");
        }

        String supplies = unarmed
                ? PlayerText.get("CGS5")
                : supplyGuidance(account, skill, build, route, weapon, items);
        if (supplies == null) return null;
        String location = route.location;
        String note = route.note;
        if (build != RestrictedBuildType.STANDARD)
        {
            note += " Protected build: " + AccountBuildPolicy.label(account)
                    + PlayerText.get("CGS6");
        }

        return new RecommendationGuidance(
                action.toString(),
                supplies,
                location,
                note);
    }

    private static CombatRoute chooseRoute(
            StrategyDataBundle data,
            AccountSnapshot account,
            Skill skill,
            int level,
            RestrictedBuildType build,
            String methodId,
            SessionIntent intent)
    {
        MembershipStatus membership = account.getMembershipStatus();

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            if (membership != MembershipStatus.P2P)
            {
                if (intent == SessionIntent.AFK)
                {
                    return new CombatRoute(
                            PlayerText.get("CGS7"),
                            PlayerText.get("CGS8"),
                            4.0,
                            PlayerText.get("CGS9"));
                }
                return new CombatRoute(
                        "Port Sarim docks and shoreline.",
                        PlayerText.get("CGS10"),
                        4.0,
                        PlayerText.get("CGS11"));
            }

            CombatRoute crab = bestCrab(data, intent);
            if (crab != null)
            {
                crab.note = "Defence-pure route. " + crab.note;
                return crab;
            }
        }

        if (methodId.contains("scurrius"))
        {
            return new CombatRoute(
                    "Scurrius arena in Varrock Sewers.",
                    PlayerText.get("CGS12"),
                    0.0,
                    PlayerText.get("CGS13"));
        }

        if (methodId.contains("slayer"))
        {
            // A live task count does not prove a concrete legal location or
            // loadout. SlayerGuidanceService owns task-specific execution.
            return null;
        }

        if (methodId.contains("nmz"))
        {
            return new CombatRoute(
                    "Nightmare Zone in Yanille.",
                    PlayerText.get("CGS14"),
                    0.0,
                    PlayerText.get("CGS15"));
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (methodId.contains("f2p_giants"))
        {
            return new CombatRoute(
                    "Hill giants in Edgeville Dungeon.",
                    PlayerText.get("CGS16"),
                    4.0,
                    PlayerText.get("CGS17"));
        }

        if (membership != MembershipStatus.P2P || methodId.contains("f2p"))
        {
            if (level < 20)
            {
                return new CombatRoute(
                        PlayerText.get("CGS18"),
                        PlayerText.get("CGS19"),
                        4.0,
                        PlayerText.get("CGS20"));
            }
            if (level < 40)
            {
                return new CombatRoute(
                        PlayerText.get("CGS21"),
                        PlayerText.get("CGS22"),
                        4.0,
                        PlayerText.get("CGS23"));
            }
            return new CombatRoute(
                    PlayerText.get("CGS24"),
                    PlayerText.get("CGS25"),
                    4.0,
                    PlayerText.get("CGS26"));
        }

        CombatRoute crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                "Sand crab beach south of Hosidius.",
                PlayerText.get("CGS27"),
                4.0,
                PlayerText.get("CGS28"));
    }

    private static CombatRoute bestCrab(
            StrategyDataBundle data,
            SessionIntent intent)
    {
        QuestSnapshot quests = data == null ? null : data.getQuests();
        boolean childrenOfSun = completed(quests, "Children of the Sun");
        boolean boneVoyage = completed(quests, "Bone Voyage");

        if (childrenOfSun && intent == SessionIntent.AFK)
        {
            return new CombatRoute(
                    PlayerText.get("CGS29"),
                    PlayerText.get("CGS30"),
                    3.5,
                    PlayerText.get("CGS31"));
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    "Ammonite Crab coast on Fossil Island.",
                    PlayerText.get("CGS32"),
                    4.0,
                    PlayerText.get("CGS33"));
        }
        return new CombatRoute(
                "Sand crab beach south of Hosidius.",
                PlayerText.get("CGS34"),
                4.0,
                PlayerText.get("CGS35"));
    }

    private static String chooseWeapon(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            ObservedItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return firstObserved(items,
                    "Twisted bow", "Bow of faerdhinen (c)", "Bow of faerdhinen",
                    "Toxic blowpipe", "Venator bow", "Dragon hunter crossbow",
                    "Dragon crossbow", "Rune crossbow", "Magic shortbow (i)",
                    "Magic shortbow", "Bone shortbow", "Dorgeshuun crossbow",
                    "Willow shortbow", "Oak shortbow", "Shortbow");
        }

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            return firstObserved(items,
                    "Swift blade", "Ham joint", "Goblin paint cannon",
                    "Maple blackjack", "Silverlight", "Event rpg");
        }

        if (build == RestrictedBuildType.OBSIDIAN_MAULER)
        {
            return firstObserved(items,
                    "Slayer's staff", "Swift blade", "Ham joint",
                    "Goblin paint cannon", "Tzhaar-ket-om");
        }

        if (skill == Skill.STRENGTH)
        {
            // Whips do not offer a dedicated Strength style, so they are not
            // placed in the Strength list even when one is owned.
            return firstObserved(items,
                    "Soulreaper axe", "Dual macuahuitl", "Ghrazi rapier",
                    "Blade of saeldor (c)", "Blade of saeldor",
                    "Abyssal dagger", "Zombie axe", "Saradomin sword",
                    "Dragon scimitar", "Dragon longsword", "Rune scimitar",
                    "Adamant scimitar", "Mithril scimitar", "Steel scimitar",
                    "Iron scimitar", "Bronze scimitar");
        }

        return firstObserved(items,
                "Scythe of vitur", "Soulreaper axe", "Osmumten's fang",
                "Ghrazi rapier", "Blade of saeldor (c)", "Blade of saeldor",
                "Abyssal whip", "Abyssal whip (or)", "Zombie axe",
                "Dragon scimitar", "Dragon longsword", "Rune scimitar",
                "Adamant scimitar", "Mithril scimitar", "Steel scimitar",
                "Iron scimitar", "Bronze scimitar");
    }

    private static String supplyGuidance(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            CombatRoute route,
            String weapon,
            ObservedItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return rangedSupplies(weapon, items);
        }
        if (route.location.contains("Scurrius"))
        {
            String food = firstObserved(items, "Manta ray", "Shark",
                    "Sea turtle", "Monkfish", "Swordfish", "Lobster",
                    "Tuna", "Cake", "Jug of wine");
            if (food == null) return null;
            String prayer = firstObserved(items, "Prayer potion(4)",
                    "Prayer potion(3)", "Prayer potion(2)",
                    "Prayer potion(1)", "Super restore(4)",
                    "Super restore(3)", "Super restore(2)",
                    "Super restore(1)");
            String boost = firstObserved(items, "Super combat potion(4)",
                    "Super combat potion(3)", "Super combat potion(2)",
                    "Super combat potion(1)", "Super attack(4)",
                    "Super strength(4)", "Ranging potion(4)");
            StringBuilder result = new StringBuilder("Bring ")
                    .append(weapon).append(" and the observed ")
                    .append(food).append(" food stack");
            if (prayer != null) result.append(", plus ").append(prayer);
            if (boost != null) result.append(" and ").append(boost);
            result.append(PlayerText.get("CGS36"));
            return result.toString();
        }
        return "Bring " + weapon
                + PlayerText.get("CGS37");
    }

    private static String rangedSupplies(String weapon, ObservedItemIndex items)
    {
        if (weapon == null) return null;
        if ("Bow of faerdhinen (c)".equals(weapon))
            return PlayerText.get("CGS38");
        if ("Bow of faerdhinen".equals(weapon)
                || "Venator bow".equals(weapon)) return null;
        if (weapon.contains("Dorgeshuun crossbow")
                || weapon.contains("Bone crossbow"))
        {
            String bolts = firstObserved(items, "Bone bolts");
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("crossbow"))
        {
            String bolts = firstObserved(items,
                    "Dragon bolts", "Runite bolts", "Adamant bolts",
                    "Mithril bolts", "Steel bolts", "Iron bolts",
                    "Bronze bolts");
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("bow")
                && !weapon.toLowerCase().contains("blowpipe"))
        {
            String arrows = firstObserved(items,
                    "Dragon arrow", "Amethyst arrow", "Rune arrow",
                    "Adamant arrow", "Mithril arrow", "Steel arrow",
                    "Iron arrow", "Bronze arrow", "Training arrows");
            return arrows == null ? null : "Bring " + weapon + " and " + arrows + ".";
        }
        // Blowpipe, Venator, and other charged/ammo-bearing weapons need live
        // charge evidence before Compass can claim the setup is executable.
        return null;
    }

    private static String attackStyle(Skill skill)
    {
        switch (skill)
        {
            case ATTACK: return "Accurate / Attack XP";
            case STRENGTH: return "Aggressive / Strength XP";
            case DEFENCE: return "Defensive / Defence XP";
            case RANGED: return "Rapid / Ranged XP";
            default: return "the style that awards " + skill.getName() + " XP";
        }
    }

    private static String firstObserved(ObservedItemIndex items, String... names)
    {
        for (String name : names)
        {
            if (items.has(name)) return name;
        }
        return null;
    }

    private static boolean completed(QuestSnapshot quests, String name)
    {
        return quests != null && quests.statusOf(name) == QuestStatus.COMPLETE;
    }

    private static boolean isDirectCombatSkill(Skill skill)
    {
        return skill == Skill.ATTACK
                || skill == Skill.STRENGTH
                || skill == Skill.DEFENCE
                || skill == Skill.RANGED;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static String trim(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
            return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String withoutPeriod(String value)
    {
        if (value == null) return "Fight the selected target";
        String trimmed = value.trim();
        return trimmed.endsWith(".")
                ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static final class CombatRoute
    {
        private final String location;
        private final String loop;
        private final double xpPerDamage;
        private String note;

        private CombatRoute(String location, String loop,
                double xpPerDamage, String note)
        {
            this.location = location;
            this.loop = loop;
            this.xpPerDamage = xpPerDamage;
            this.note = note;
        }
    }
}
