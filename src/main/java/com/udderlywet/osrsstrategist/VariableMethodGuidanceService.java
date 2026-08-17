package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Concrete setup guidance for good methods whose XP per game/task is variable.
 *
 * <p>These methods should still remove setup guesswork, but they must not show a
 * fabricated number of games, contracts, kills, or rewards. Exact XP remaining
 * is always shown and the plugin explains what live state is still needed before
 * a repeat count can be trustworthy.</p>
 */
@Singleton
public class VariableMethodGuidanceService
{
    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }
        String id = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId();
        int currentXp = data.getAccount().getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);

        switch (id)
        {
            case "firemaking_wintertodt":
                return wintertodt(data, targetLevel, xpNeeded, items);
            case "fishing_tempoross":
                return tempoross(targetLevel, xpNeeded, items);
            case "runecraft_gotr":
                return gotr(data, targetLevel, xpNeeded, items);
            case "mining_mlm":
                return motherlode(targetLevel, xpNeeded, items);
            case "mining_stars":
                return shootingStars(targetLevel, xpNeeded, items);
            case "mining_volcanic":
                return volcanicMine(data, targetLevel, xpNeeded, items);
            case "mining_blast_mine":
                return blastMine(targetLevel, xpNeeded, items);
            case "smithing_foundry":
                return giantsFoundry(data, targetLevel, xpNeeded, items);
            case "construction_homes":
                return mahoganyHomes(data, targetLevel, xpNeeded, items);
            case "herblore_mixology":
                return mixology(targetLevel, xpNeeded, items);
            case "farming_tithe":
                return titheFarm(targetLevel, xpNeeded, items);
            case "hunter_rumours":
                return hunterRumours(targetLevel, xpNeeded, items);
            default:
                return null;
        }
    }

    private static RecommendationGuidance wintertodt(
            StrategyDataBundle data,
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Run Wintertodt rounds and reach at least 500 personal points before each kill so the round qualifies for reward rolls. You need "
                + format(xpNeeded) + " Firemaking XP to level " + target + ".";
        String supplies = "Equip four warm items, bring a knife and hammer, and bring food appropriate to your Hitpoints level. "
                + observed(items, "Bruma torch", "Warm gloves", "Pyromancer hood",
                "Pyromancer garb", "Pyromancer robe", "Pyromancer boots");
        String where = "Use the Wintertodt camp in northern Great Kourend. Chop bruma roots, feed braziers, repair broken braziers, and fletch roots when the extra points/supply-roll value is worth the slower raw Firemaking XP.";
        String note = "Round length, interruptions, fletching choice, and your Firemaking/Woodcutting levels change XP per game. Strategist keeps exact XP remaining but will not invent a fixed number of Wintertodt kills.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance tempoross(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Complete Tempoross games while keeping your activity high enough to earn reward permits. You need "
                + format(xpNeeded) + " Fishing XP to level " + target + ".";
        String supplies = "Bring your best usable harpoon if you own one; otherwise use the island's tools. A rope, hammer, and buckets are available on the island, so do not fill the bank plan with unnecessary duplicates. "
                + observed(items, "Dragon harpoon", "Crystal harpoon", "Infernal harpoon", "Harpoon");
        String where = "Board the boat from the Tempoross lobby, fish harpoonfish, cook them when the chosen strategy values points/rewards, load cannons, and tether during waves.";
        String note = "Cooking fish, team size, storm timing, and reward-point strategy change Fishing XP per game. The planner therefore shows exact XP remaining but not a fake number of games.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance gotr(
            StrategyDataBundle data,
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Play Guardians of the Rift and balance elemental/catalytic energy while using the best essence-storage setup your account has unlocked. You need "
                + format(xpNeeded) + " Runecraft XP to level " + target + ".";
        String supplies = "Bring a pickaxe and chisel. Bring every essence pouch you can use safely; repair/degradation handling depends on your current pouch/quest access. "
                + observed(items, "Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch", "Abyssal lantern");
        String where = "Enter the Temple of the Eye minigame area, mine guardian fragments, craft essence, use opened altars, and place cells where they protect/upgrade guardians.";
        String note = "Portal timing, altar choices, pouch capacity, cells, and match outcome change XP per game. Strategist will not convert the milestone into a fixed match count until those live factors are modeled.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance motherlode(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Mine pay-dirt at Motherlode Mine until you gain "
                + format(xpNeeded) + " Mining XP toward level " + target
                + ", banking the ores and spending golden nuggets on permanent unlocks before cosmetic detours.";
        String supplies = "Bring your best usable pickaxe. "
                + observed(items, "Dragon pickaxe", "Crystal pickaxe", "Infernal pickaxe", "Rune pickaxe", "Prospector helmet", "Prospector jacket", "Prospector legs", "Prospector boots");
        String where = "Use the lower level until the upper-area requirement is met and purchased, then favor the upper veins for longer depletion timers and lower attention.";
        String note = "Pay-dirt ore results and vein uptime vary, so XP per sack is not fixed. Golden nuggets are separate progression currency; Strategist should preserve goals such as Prospector/upper level instead of rotating away on a level milestone.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance shootingStars(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Mine a reachable Shooting Star with your best pickaxe until it depletes or you gain "
                + format(xpNeeded) + " Mining XP toward level " + target + ".";
        String supplies = "Only a usable pickaxe is required. Keep stardust instead of dropping it when Celestial ring/charge rewards still matter. "
                + observed(items, "Celestial ring", "Celestial signet", "Dragon pickaxe", "Rune pickaxe");
        String where = "Use a discovered star location that your account can reach without breaking Wilderness/risk settings.";
        String note = "Star tier changes during depletion and affects XP, so an exact swing count would be false precision. Shooting Stars are primarily the low-attention Mining option.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance volcanicMine(
            StrategyDataBundle data,
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Run Volcanic Mine with a verified team/role until you gain "
                + format(xpNeeded) + " Mining XP toward level " + target + ".";
        String supplies = "Bring your best pickaxe and the food/stamina setup required by your team's role. "
                + observed(items, "Dragon pickaxe", "Crystal pickaxe", "Rune pickaxe");
        String where = "Use the Volcanic Mine on Fossil Island only after its access requirements and your intended team/role are verified.";
        String note = "Vent state, team role, boulder phase, and points change XP per game. The plugin deliberately refuses to fabricate a game count.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance blastMine(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Mine blasted ore at Blast Mine until you gain "
                + format(xpNeeded) + " Mining XP toward level " + target + ".";
        String supplies = "Bring dynamite and your best usable pickaxe. Exact dynamite usage depends on the number of wall sections you choose to blast. "
                + observed(items, "Dynamite", "Dragon pickaxe", "Rune pickaxe");
        String where = "Use Blast Mine in Lovakengj and keep a safe blast rhythm: place dynamite, light it, move clear, excavate the blasted rock, then collect ore.";
        String note = "Ore tier depends partly on Mining level and the loop's timing, so Strategist keeps exact XP remaining without claiming a universal dynamite-to-level count.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance giantsFoundry(
            StrategyDataBundle data,
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Take a Giants' Foundry commission and forge the best commission-compatible alloy you can sustain until you gain "
                + format(xpNeeded) + " Smithing XP toward level " + target + ".";
        String supplies = "Each commissioned sword consumes 28 bars' worth of metal. Prefer banked metal on Iron-style accounts and avoid destroying items reserved for quests/upgrades. "
                + observed(items, "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar");
        String where = "At Giants' Foundry, choose the best owned moulds for the commission, pour the 28-bar alloy, then work the sword through heat/cool/hammer/grind/polish stations while staying in each target temperature band.";
        String note = "Sword quality, mould score, alloy, mistakes, and commission all change XP per sword. Strategist can calculate exact bar shortfalls per chosen commission later, but it will not pretend every sword gives the same XP.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance mahoganyHomes(
            StrategyDataBundle data,
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Take the highest Mahogany Homes contract tier your Construction level and plank supply can sustain, then complete contracts until you gain "
                + format(xpNeeded) + " Construction XP toward level " + target + ".";
        String supplies = "Bring a hammer, saw, teleports for contract cities, and the matching plank tier. A plank sack should be used when owned. "
                + observed(items, "Plank sack", "Oak plank", "Teak plank", "Mahogany plank", "Steel bar");
        String where = "Get a contract from a Mahogany Homes contractor, travel to the named client, repair every marked hotspot, then return for the next contract/reward points.";
        String note = "Furniture mix varies by client, so plank/steel-bar use and XP vary per contract. Exact material advice should be generated after the live contract/client is known.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance mixology(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Fill Mastering Mixology orders from the current order list until you gain "
                + format(xpNeeded) + " Herblore XP toward level " + target + ".";
        String supplies = "Convert spare eligible herbs into the three paste types, but preserve herbs needed for higher-value combat potions before turning the whole bank into paste. "
                + observed(items, "Mox paste", "Aga paste", "Lye paste", "Digweed");
        String where = "At the Alchemical Society, mix the ordered potion, use the correct processing station, and hand it in before selecting the next order.";
        String note = "Orders and paste combinations change from potion to potion, so exact paste-to-level counts require the live order state. Strategist should optimize banked herbs and reward goals instead of inventing a constant XP/order.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance titheFarm(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Run Tithe Farm with the highest fruit seed tier unlocked and a plot count you can water without losing plants. You need "
                + format(xpNeeded) + " Farming XP to level " + target + ".";
        String supplies = "Bring a spade and watering cans. Use Gricoller's can when owned because its large capacity cuts refill downtime. "
                + observed(items, "Gricoller's can", "Seed box", "Farmer's strawhat", "Farmer's jacket", "Farmer's boro trousers", "Farmer's boots");
        String where = "Plant, water every growth cycle, harvest the fruit, and deposit it for points before the batch expires.";
        String note = "The number of simultaneous plants and missed growth cycles changes XP/hour. Reward goals such as the seed box/can/outfit should be preserved as progression objectives instead of being abandoned at an arbitrary Farming level.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance hunterRumours(
            int target,
            int xpNeeded,
            ObservedItemIndex items)
    {
        String action = "Take the highest Hunter Rumour tier you can access and complete the assigned creature until you gain "
                + format(xpNeeded) + " Hunter XP toward level " + target + ".";
        String supplies = "Bring the trap/tool required by the assigned creature and your hunter's whistle when available. Do not pre-pack one generic trap loadout before the creature is known. "
                + observed(items, "Basic quetzal whistle", "Enhanced quetzal whistle", "Perfected quetzal whistle");
        String where = "Get the rumour from the Hunter Guild, travel to the assigned creature, obtain its rare piece, then return it for the reward sack and next assignment.";
        String note = "Creature assignment and rare-part RNG change XP per rumour. Once Strategist can observe the current rumour directly, this planner can switch from generic setup to creature-specific traps, route, and inventory.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static String observed(ObservedItemIndex items, String... names)
    {
        StringBuilder found = new StringBuilder();
        for (String name : names)
        {
            int quantity = items.quantity(name);
            if (quantity <= 0) continue;
            if (found.length() > 0) found.append(", ");
            found.append(quantity).append("x ").append(name);
        }
        return found.length() == 0
                ? "No relevant setup item from this short list is currently observed."
                : "Observed: " + found + ".";
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}
