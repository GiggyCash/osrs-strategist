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

        AccountSnapshot account = data.account();
        if (account.getMembershipStatus() == MembershipStatus.UNKNOWN) return null;
        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        if (!AccountBuildPolicy.allowsSkill(account, skill)) return null;

        String methodId = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId().toLowerCase();
        CombatRoute route = chooseRoute(
                data, account, skill, currentLevel, build,
                methodId, sessionIntent);
        if (route == null) return null;

        ItemIndex items = new ItemIndex(data, useGroupStorage);
        String weapon = chooseWeapon(account, skill, build, items);
        if (weapon == null && skill != Skill.RANGED
                && build == RestrictedBuildType.STANDARD
                && currentLevel < 20)
        {
            return new Guidance(
                    Text.get(151),
                    Text.get(1333),
                    Text.get(162),
                    Text.get(173));
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
                .append(skill.getName()).append(Text.get(1334))
                .append(targetLevel).append(".");

        if (route.xpPerDamage > 0)
        {
            int damageNeeded = (int) Math.ceil(xpNeeded / route.xpPerDamage);
            action.append(" That is about ")
                    .append(format(damageNeeded))
                    .append(Text.get(183))
                    .append(trim(route.xpPerDamage))
                    .append(" XP per damage.");
        }

        String supplies = unarmed
                ? Text.get(184)
                : supplyGuidance(account, skill, build, route, weapon, items);
        if (supplies == null) return null;
        String location = route.location;
        String note = route.note;
        if (build != RestrictedBuildType.STANDARD)
        {
            note += Text.get(1335) + AccountBuildPolicy.label(account)
                    + Text.get(185);
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
        MembershipStatus membership = account.getMembershipStatus();

        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            if (membership != MembershipStatus.P2P)
            {
                if (intent == SessionIntent.AFK)
                {
                    return new CombatRoute(
                            Text.get(186),
                            Text.get(187),
                            4.0,
                            Text.get(188));
                }
                return new CombatRoute(
                        Text.get(1336),
                        Text.get(152),
                        4.0,
                        Text.get(153));
            }

            CombatRoute crab = bestCrab(data, intent);
            if (crab != null)
            {
                crab.note = Text.get(1337) + crab.note;
                return crab;
            }
        }

        if (methodId.contains("scurrius"))
        {
            return new CombatRoute(
                    Text.get(1338),
                    Text.get(154),
                    0.0,
                    Text.get(155));
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
                    Text.get(1339),
                    Text.get(156),
                    0.0,
                    Text.get(157));
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (methodId.contains("f2p_giants"))
        {
            return new CombatRoute(
                    Text.get(1340),
                    Text.get(158),
                    4.0,
                    Text.get(159));
        }

        if (membership != MembershipStatus.P2P || methodId.contains("f2p"))
        {
            if (level < 20)
            {
                return new CombatRoute(
                        Text.get(160),
                        Text.get(161),
                        4.0,
                        Text.get(163));
            }
            if (level < 40)
            {
                return new CombatRoute(
                        Text.get(164),
                        Text.get(165),
                        4.0,
                        Text.get(166));
            }
            return new CombatRoute(
                    Text.get(167),
                    Text.get(168),
                    4.0,
                    Text.get(169));
        }

        CombatRoute crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                Text.get(1341),
                Text.get(170),
                4.0,
                Text.get(171));
    }

    private static CombatRoute bestCrab(
            GameData data,
            SessionIntent intent)
    {
        QuestSnapshot quests = data == null ? null : data.quests();
        boolean childrenOfSun = completed(quests, Text.get(1342));
        boolean boneVoyage = completed(quests, "Bone Voyage");

        if (childrenOfSun && intent == SessionIntent.AFK)
        {
            return new CombatRoute(
                    Text.get(172),
                    Text.get(174),
                    3.5,
                    Text.get(175));
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    Text.get(1343),
                    Text.get(176),
                    4.0,
                    Text.get(177));
        }
        return new CombatRoute(
                Text.get(1341),
                Text.get(178),
                4.0,
                Text.get(179));
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
            String food = firstObserved(items, LOADOUTS.food);
            if (food == null) return null;
            String prayer = firstObserved(items, LOADOUTS.prayer);
            String boost = firstObserved(items, LOADOUTS.boost);
            StringBuilder result = new StringBuilder("Bring ")
                    .append(weapon).append(Text.get(1344))
                    .append(food).append(" food stack");
            if (prayer != null) result.append(", plus ").append(prayer);
            if (boost != null) result.append(" and ").append(boost);
            result.append(Text.get(180));
            return result.toString();
        }
        return "Bring " + weapon
                + Text.get(181);
    }

    private static String rangedSupplies(String weapon, ItemIndex items)
    {
        if (weapon == null) return null;
        if (Text.get(1345).equals(weapon))
            return Text.get(182);
        if ("Bow of faerdhinen".equals(weapon)
                || "Venator bow".equals(weapon)) return null;
        if (weapon.contains(Text.get(1346))
                || weapon.contains("Bone crossbow"))
        {
            String bolts = firstObserved(items, "Bone bolts");
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("crossbow"))
        {
            String bolts = firstObserved(items, LOADOUTS.bolts);
            return bolts == null ? null : "Bring " + weapon + " and " + bolts + ".";
        }
        if (weapon.toLowerCase().contains("bow")
                && !weapon.toLowerCase().contains("blowpipe"))
        {
            String arrows = firstObserved(items, LOADOUTS.arrows);
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
            case ATTACK: return Text.get(1317);
            case STRENGTH: return Text.get(1318);
            case DEFENCE: return Text.get(1319);
            case RANGED: return "Rapid / Ranged XP";
            default: return Text.get(1347) + skill.getName() + " XP";
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
        if (value == null) return Text.get(1348);
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

    private static final class Loadouts
    {
        private String[] rangedWeapons, defenceWeapons, obsidianWeapons,
                strengthWeapons, meleeWeapons, food, prayer, boost, bolts,
                arrows;
    }
}
