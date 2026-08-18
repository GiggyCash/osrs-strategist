package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Concrete combat guidance for Attack, Strength, Defence, and Ranged.
 *
 * <p>Combat is not treated like Cooking. Misses give no skill XP and target
 * choice, weapon speed, quest access, account builds, and monster XP modifiers
 * matter. Strategist therefore gives an exact remaining-XP/damage target only
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
        String style = attackStyle(skill);

        int currentXp = account.getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        StringBuilder action = new StringBuilder();
        action.append("Set your combat style to ").append(style).append(". ");
        if (weapon != null)
        {
            action.append("Use ").append(weapon).append(". ");
        }
        else if (!items.bankObserved())
        {
            action.append("Open your bank once to compare the legal observed weapons for this build. ");
        }
        else
        {
            action.append("No preferred legal weapon is currently observed; use the best legal weapon you own while Strategist works the next weapon acquisition into the queue. ");
        }
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

        String supplies = supplyGuidance(account, skill, build, route, weapon);
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
                            "Train on monks at the Edgeville Monastery. Ask a monk to heal you when needed.",
                            4.0,
                            "Monks are a low-risk Defence-pure target. Their healing can extend a training interaction, so do not turn the damage target into a fixed kill count.");
                }
                return new CombatRoute(
                        "Train on seagulls around Port Sarim. They have very low Defence and low offensive pressure.",
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
                    "Fight Scurrius in the Varrock Sewers. Prefer the matching rat-bone weapon once you have a spine and the weapon is legal for your build.",
                    0.0,
                    "Scurrius has a combat-XP bonus and rat-bone weapons change the effective XP model, so Strategist deliberately does not invent a kill count here.");
        }

        if (methodId.contains("slayer"))
        {
            SlayerSnapshot slayer = data.getSlayer();
            if (slayer != null && slayer.hasTask())
            {
                return new CombatRoute(
                        "Train on your current Slayer task: " + slayer.getTaskName()
                                + " (" + slayer.getRemaining() + " remaining).",
                        0.0,
                        "Task monsters can have different defence, hitpoints, and XP modifiers. The task count is live evidence, but a fixed combat kill count would be fake precision.");
            }
            return null;
        }

        if (methodId.contains("nmz"))
        {
            return new CombatRoute(
                    "Use your verified Nightmare Zone setup and only bosses already unlocked for the account.",
                    0.0,
                    "Nightmare Zone boss choices and modifiers change effective XP per damage, so the remaining XP is exact but a universal kill count is not.");
        }

        if (methodId.contains("crab"))
        {
            return bestCrab(data, intent);
        }

        if (membership != MembershipStatus.P2P)
        {
            if (level < 20)
            {
                return new CombatRoute(
                        "Train on monks at the Edgeville Monastery until the low-level combat band is complete.",
                        4.0,
                        "Monks can heal you and themselves, reducing food use while early accuracy and max hit are low.");
            }
            if (level < 40)
            {
                return new CombatRoute(
                        "Train on giant frogs in Lumbridge Swamp.",
                        4.0,
                        "Giant frogs have high Hitpoints for their level, low Defence, and always drop big bones that can support Prayer progression.");
            }
            return new CombatRoute(
                    "Train on Flesh Crawlers in the Stronghold of Security for low-attention combat, or giant frogs when you want big bones for Prayer.",
                    4.0,
                    "Flesh Crawlers stay aggressive and are useful for AFK combat. Giant frogs trade some convenience for Prayer supplies.");
        }

        CombatRoute crab = bestCrab(data, intent);
        if (crab != null) return crab;

        return new CombatRoute(
                "Train on sand crabs south of Hosidius until a higher-value combat route is verified.",
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
                    "Train on the Gemstone Crab in the Tlati Rainforest and follow it through the nearby cave when it relocates.",
                    3.5,
                    "Gemstone Crab has effectively infinite Hitpoints and gives 87.5% of ordinary combat XP per damage. It is excellent for long idle sessions but not always the best low-level raw XP choice.");
        }
        if (boneVoyage)
        {
            return new CombatRoute(
                    "Train on Ammonite Crabs on Fossil Island. Reset aggression after roughly 10 minutes.",
                    4.0,
                    "Ammonite Crabs have 100 Hitpoints and very low combat stats, reducing downtime and food use.");
        }
        return new CombatRoute(
                "Train on sand crabs south of Hosidius. Reset aggression after roughly 10 minutes.",
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
            String weapon)
    {
        if (skill == Skill.RANGED)
        {
            return "Use ammunition that matches the observed weapon and account mode. An exact purchase/source count requires modeled consumption for the selected weapon.";
        }
        if (build == RestrictedBuildType.DEFENCE_PURE)
        {
            return "Prioritize accuracy/Strength-bonus equipment that does not violate the build. Bring food only if the chosen target can out-damage your natural sustain.";
        }
        if (route.location.contains("Scurrius"))
        {
            return "Bring food, prayer restoration, and combat boosts appropriate to the account. Rat-bone weapon progression should replace generic training gear once obtained.";
        }
        return weapon == null
                ? "Open the bank once before Strategist commits to an exact combat loadout."
                : "Use your best build-legal armour with " + weapon
                        + ". Prefer supplies already observed on Iron-style accounts before creating a new acquisition detour.";
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

    private static final class CombatRoute
    {
        private final String location;
        private final double xpPerDamage;
        private String note;

        private CombatRoute(String location, double xpPerDamage, String note)
        {
            this.location = location;
            this.xpPerDamage = xpPerDamage;
            this.note = note;
        }
    }
}
