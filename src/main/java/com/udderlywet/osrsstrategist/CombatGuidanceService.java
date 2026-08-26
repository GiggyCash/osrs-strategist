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
                    .append(" successful damage dealt on this style at ")
                    .append(trim(route.xpPerDamage))
                    .append(" XP per damage.");
        }

        String supplies = unarmed
                ? "No supplies required for the first trip."
                : supplyGuidance(account, skill, build, route, weapon, items);
        if (skill == Skill.RANGED && supplies == null) return null;
        String location = route.location;
        String note = route.note;
        if (build != RestrictedBuildType.STANDARD)
        {
            note += " Protected build: " + AccountBuildPolicy.label(account)
                    + ". A blocked combat stat will not be trained intentionally.";
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
                            "Edgeville Monastery, west of Edgeville.",
                            "Fight monks and ask a monk to heal you when needed.",
                            4.0,
                            "Monks are a low-risk Defence-pure target. Their healing can extend a training interaction, so do not turn the damage target into a fixed kill count.");
                }
                return new CombatRoute(
                        "Port Sarim docks and shoreline.",
                        "Fight seagulls and stay on Defensive style.",
                        4.0,
                        "Use Defensive style only. Seagulls are efficient for the low-damage Defence-pure profile because accuracy matters more than loot.");
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
                    "Fight Scurrius and use the matching rat-bone weapon once it is observed and legal for the build.",
                    0.0,
                    "Scurrius has a combat-XP bonus and rat-bone weapons change the effective XP model, so no fixed kill count is shown.");
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
                    "Enter with the verified setup and select only bosses already unlocked for this account.",
                    0.0,
                    "Nightmare Zone boss choices and modifiers change effective XP per damage, so the remaining XP is exact but a universal kill count is not.");
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (methodId.contains("f2p_giants"))
        {
            return new CombatRoute(
                    "Hill giants in Edgeville Dungeon.",
                    "Fight hill giants, collect only the drops worth keeping, and repeat.",
                    4.0,
                    "Hill giants provide a concrete F2P combat-and-Prayer-supply loop without requiring a members area.");
        }

        if (membership != MembershipStatus.P2P || methodId.contains("f2p"))
        {
            if (level < 20)
            {
                return new CombatRoute(
                        "Edgeville Monastery, west of Edgeville.",
                        "Fight monks and ask a monk to heal you when needed.",
                        4.0,
                        "Monks can heal you and themselves, reducing food use while early accuracy and max hit are low.");
            }
            if (level < 40)
            {
                return new CombatRoute(
                        "Giant frogs in Lumbridge Swamp, south of Lumbridge Castle.",
                        "Fight giant frogs, bury or keep the big bones, and repeat.",
                        4.0,
                        "Giant frogs have high Hitpoints for their level, low Defence, and always drop big bones that can support Prayer progression.");
            }
            return new CombatRoute(
                    "Flesh Crawlers in the Stronghold of Security's Catacomb of Famine.",
                    "Fight Flesh Crawlers, reset aggression when they stop attacking, and repeat.",
                    4.0,
                    "Flesh Crawlers stay aggressive for low-attention combat; this route does not ask the player to choose between two targets.");
        }

        CombatRoute crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                "Sand crab beach south of Hosidius.",
                "Fight sand crabs and reset aggression after roughly 10 minutes.",
                4.0,
                "Sand crabs have 60 Hitpoints, very low combat stats, and no quest requirement for the basic beach route.");
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
                    "Gemstone Crab cave in the Tlati Rainforest.",
                    "Attack the Gemstone Crab and follow it through the cave when it relocates.",
                    3.5,
                    "Gemstone Crab has effectively infinite Hitpoints and gives 87.5% of ordinary combat XP per damage. It is excellent for long idle sessions but not always the best low-level raw XP choice.");
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    "Ammonite Crab coast on Fossil Island.",
                    "Fight Ammonite Crabs and reset aggression after roughly 10 minutes.",
                    4.0,
                    "Ammonite Crabs have 100 Hitpoints and very low combat stats, reducing downtime and food use.");
        }
        return new CombatRoute(
                "Sand crab beach south of Hosidius.",
                "Fight sand crabs and reset aggression after roughly 10 minutes.",
                4.0,
                "Sand crabs have 60 Hitpoints, very low combat stats, and the basic beach route has no quest requirement.");
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
            return "Bring food, prayer restoration, and combat boosts appropriate to the account. Rat-bone weapon progression should replace generic training gear once obtained.";
        }
        return "Bring " + weapon
                + "; no other supplies are required for the first trip. Leave if the target damages you faster than you recover.";
    }

    private static String rangedSupplies(String weapon, ObservedItemIndex items)
    {
        if (weapon == null) return null;
        if ("Bow of faerdhinen (c)".equals(weapon))
            return "Bring your charged Bow of faerdhinen (c); it supplies its own ammunition.";
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
