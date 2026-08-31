package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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
    private static final Loadouts LOADOUTS = BundledCatalogLoader.array(
            "/content/catalogs/combat-loadouts.json", Loadouts[].class)[0];
    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            SessionIntent sessionIntent,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.getMethod() == null
                || !isDirectCombatSkill(skill))
        {
            return null;
        }

        var account = data.account();
        if (account.getMembershipStatus() == MembershipStatus.UNKNOWN) return null;
        var build = AccountBuildPolicy.effectiveBuild(account);
        if (!AccountBuildPolicy.allowsSkill(account, skill)) return null;

        String methodId = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId().toLowerCase();
        CombatRoute route = chooseRoute(
                data, account, skill, currentLevel, build,
                methodId, sessionIntent);
        if (route == null) return null;

        var items = new ItemIndex(data, useGroupStorage);
        var weapon = chooseWeapon(account, skill, build, items);
        if (weapon == null && skill != Skill.RANGED
                && build == RestrictedBuildType.STANDARD
                && currentLevel < 20)
        {
            return new Guidance(
                    get(151),
                    get(1333),
                    get(162),
                    get(173));
        }
        boolean unarmed = weapon == null && currentLevel < 10
                && skill != Skill.RANGED;
        if (weapon == null && !unarmed) return null;
        var style = attackStyle(skill);

        var currentXp = account.getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        var action = new StringBuilder();
        action.append(withoutPeriod(route.loop));
        if (weapon != null) action.append(" with ").append(weapon);
        else if (unarmed) action.append(" while unarmed");
        action.append(" on ").append(style).append(". ");
        action.append(format(xpNeeded)).append(" ")
                .append(skill.getName()).append(get(1334))
                .append(targetLevel).append(".");

        if (route.xpPerDamage > 0)
        {
            var damageNeeded = (int) Math.ceil(xpNeeded / route.xpPerDamage);
            action.append(" That is about ")
                    .append(format(damageNeeded))
                    .append(get(183))
                    .append(trim(route.xpPerDamage))
                    .append(" XP per damage.");
        }

        String supplies = unarmed
                ? get(184)
                : supplyGuidance(account, skill, build, route, weapon, items);
        if (supplies == null) return null;
        var location = route.location;
        var note = route.note;
        if (build != RestrictedBuildType.STANDARD)
        {
            note += get(1335) + AccountBuildPolicy.label(account)
                    + get(185);
        }

        return new Guidance(
                action.toString(),
                supplies,
                location,
                note);
    }

    private static CombatRoute chooseRoute(
            GameData data,
            AccountSnapshot account,
            Skill skill,
            int level,
            RestrictedBuildType build,
            String methodId,
            SessionIntent intent)
    {
        var membership = account.getMembershipStatus();

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            if (membership != MembershipStatus.P2P)
            {
                if (intent == SessionIntent.AFK)
                {
                    return new CombatRoute(
                            get(186),
                            get(187),
                            4.0,
                            get(188));
                }
                return new CombatRoute(
                        get(1336),
                        get(152),
                        4.0,
                        get(153));
            }

            var crab = bestCrab(data, intent);
            if (crab != null)
            {
                crab.note = get(1337) + crab.note;
                return crab;
            }
        }

        if (methodId.contains("scurrius"))
        {
            return new CombatRoute(
                    get(1338),
                    get(154),
                    0.0,
                    get(155));
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
                    get(1339),
                    get(156),
                    0.0,
                    get(157));
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (methodId.contains("f2p_giants"))
        {
            return new CombatRoute(
                    get(1340),
                    get(158),
                    4.0,
                    get(159));
        }

        if (membership != MembershipStatus.P2P || methodId.contains("f2p"))
        {
            if (level < 20)
            {
                return new CombatRoute(
                        get(160),
                        get(161),
                        4.0,
                        get(163));
            }
            if (level < 40)
            {
                return new CombatRoute(
                        get(164),
                        get(165),
                        4.0,
                        get(166));
            }
            return new CombatRoute(
                    get(167),
                    get(168),
                    4.0,
                    get(169));
        }

        var crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                get(1341),
                get(170),
                4.0,
                get(171));
    }

    private static CombatRoute bestCrab(
            GameData data,
            SessionIntent intent)
    {
        var quests = data == null ? null : data.quests();
        var childrenOfSun = completed(quests, get(1342));
        var boneVoyage = completed(quests, "Bone Voyage");

        if (childrenOfSun && intent == SessionIntent.AFK)
        {
            return new CombatRoute(
                    get(172),
                    get(174),
                    3.5,
                    get(175));
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    get(1343),
                    get(176),
                    4.0,
                    get(177));
        }
        return new CombatRoute(
                get(1341),
                get(178),
                4.0,
                get(179));
    }

    private static String chooseWeapon(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            ItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return firstObserved(items, LOADOUTS.rangedWeapons);
        }

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            return firstObserved(items, LOADOUTS.defenceWeapons);
        }

        if (build == RestrictedBuildType.OBSIDIAN_MAULER)
        {
            return firstObserved(items, LOADOUTS.obsidianWeapons);
        }

        if (skill == Skill.STRENGTH)
        {
            // Whips do not offer a dedicated Strength style, so they are not
            // placed in the Strength list even when one is owned.
            return firstObserved(items, LOADOUTS.strengthWeapons);
        }

        return firstObserved(items, LOADOUTS.meleeWeapons);
    }

    private static String supplyGuidance(
            AccountSnapshot account,
            Skill skill,
            RestrictedBuildType build,
            CombatRoute route,
            String weapon,
            ItemIndex items)
    {
        if (skill == Skill.RANGED)
        {
            return rangedSupplies(weapon, items);
        }
        if (route.location.contains("Scurrius"))
        {
            var food = firstObserved(items, LOADOUTS.food);
            if (food == null) return null;
            var prayer = firstObserved(items, LOADOUTS.prayer);
            var boost = firstObserved(items, LOADOUTS.boost);
            StringBuilder result = new StringBuilder("Bring ")
                    .append(weapon).append(get(1344))
                    .append(food).append(" food stack");
            if (prayer != null) result.append(", plus ").append(prayer);
            if (boost != null) result.append(" and ").append(boost);
            result.append(get(180));
            return result.toString();
        }
        return "Bring " + weapon
                + get(181);
    }

    private static String rangedSupplies(String weapon, ItemIndex items)
    {
        if (weapon == null) return null;
        if (get(1345).equals(weapon))
            return get(182);
        if ("Bow of faerdhinen".equals(weapon)
                || "Venator bow".equals(weapon)) return null;
        if (weapon.contains(get(1346))
                || weapon.contains("Bone crossbow"))
        {
            var bolts = firstObserved(items, "Bone bolts");
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("crossbow"))
        {
            var bolts = firstObserved(items, LOADOUTS.bolts);
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("bow")
                && !weapon.toLowerCase().contains("blowpipe"))
        {
            var arrows = firstObserved(items, LOADOUTS.arrows);
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
            case ATTACK: return get(1317);
            case STRENGTH: return get(1318);
            case DEFENCE: return get(1319);
            case RANGED: return "Rapid / Ranged XP";
            default: return get(1347) + skill.getName() + " XP";
        }
    }

    private static String firstObserved(ItemIndex items, String... names)
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
        if (value == null) return get(1348);
        var trimmed = value.trim();
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

    private static final class Loadouts
    {
        private String[] rangedWeapons, defenceWeapons, obsidianWeapons,
                strengthWeapons, meleeWeapons, food, prayer, boost, bolts,
                arrows;
    }
}
