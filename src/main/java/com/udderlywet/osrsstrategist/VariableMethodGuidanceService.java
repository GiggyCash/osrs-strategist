package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Concrete setup guidance for useful methods whose XP per game/task is variable.
 * Exact XP remaining is preserved, but variable games/contracts/catches are never
 * converted into fake repeat counts.
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
        if (data == null || data.getAccount() == null || skill == null
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
            case "firemaking_wintertodt": return wintertodt(targetLevel, xpNeeded, items);
            case "fishing_tempoross": return tempoross(targetLevel, xpNeeded, items);
            case "runecraft_gotr": return gotr(targetLevel, xpNeeded, items);
            case "runecraft_zmi": return zmi(targetLevel, xpNeeded, items);
            case "mining_mlm": return motherlode(targetLevel, xpNeeded, items);
            case "mining_stars": return shootingStars(targetLevel, xpNeeded, items);
            case "mining_volcanic": return volcanicMine(targetLevel, xpNeeded, items);
            case "mining_blast_mine": return blastMine(targetLevel, xpNeeded, items);
            case "smithing_foundry":
            case "smithing_giants_foundry": return giantsFoundry(targetLevel, xpNeeded, items);
            case "construction_homes":
            case "construction_mahogany_homes": return mahoganyHomes(targetLevel, xpNeeded, items);
            case "herblore_mixology": return mixology(targetLevel, xpNeeded, items);
            case "farming_tithe": return titheFarm(targetLevel, xpNeeded, items);
            case "farming_allotments_expanded": return farmingAllotments(data, targetLevel, xpNeeded, items);
            case "farming_herbs_expanded": return farmingHerbs(data, targetLevel, xpNeeded, items);
            case "farming_contracts": return farmingContracts(data, targetLevel, xpNeeded, items);
            case "hunter_rumours": return hunterRumours(targetLevel, xpNeeded, items);
            case "hunter_herbiboar": return herbiboar(targetLevel, xpNeeded, items);
            case "woodcutting_forestry": return forestry(targetLevel, xpNeeded, items);
            case "thieving_pyramid": return pyramidPlunder(targetLevel, xpNeeded, items);
            case "thieving_varlamore": return varlamoreThieving(targetLevel, xpNeeded, items);
            default: return null;
        }
    }

    private static RecommendationGuidance wintertodt(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Chop bruma roots, feed and repair braziers, reach at least 500 personal points, then repeat until you gain " + format(xp) + " Firemaking XP for level " + target + ". Fletch only when needed to secure the 500-point threshold.",
                "Equip four warm items, bring a knife and hammer, and use food appropriate to your Hitpoints level. " + observed(items, "Bruma torch", "Warm gloves", "Pyromancer hood", "Pyromancer garb", "Pyromancer robe", "Pyromancer boots"),
                "Wintertodt camp in northern Great Kourend.",
                "Round length, interruptions, fletching, and player levels change XP per game. Exact XP remaining is shown without inventing a fixed kill count."
        );
    }

    private static RecommendationGuidance tempoross(int target, int xp, ObservedItemIndex items)
    {
        String harpoon = firstObserved(items, "Dragon harpoon",
                "Crystal harpoon", "Infernal harpoon", "Harpoon");
        return new RecommendationGuidance(
                "Fish harpoonfish, cook the catch, load both cannons, tether during waves, and repeat until you gain " + format(xp) + " Fishing XP for level " + target + ".",
                harpoon == null
                        ? "No supplies required; use the harpoon, rope, hammer, and buckets on the island."
                        : "Bring " + harpoon + "; rope, hammer, and buckets are available on the island.",
                "Tempoross island, entered from the Ruins of Unkah ferry.",
                "Cooking choice, team size, storm timing, and reward strategy change Fishing XP per game, so no fake game count is shown."
        );
    }

    private static RecommendationGuidance gotr(int target, int xp, ObservedItemIndex items)
    {
        String pouches = observed(items, "Small pouch", "Medium pouch",
                "Large pouch", "Giant pouch", "Colossal pouch");
        return new RecommendationGuidance(
                "Mine fragments, craft guardian essence, enter the open altars, charge guardians, and place cells until you gain " + format(xp) + " Runecraft XP for level " + target + ".",
                "Bring a pickaxe and chisel."
                        + (pouches.isEmpty() ? "" : " " + pouches),
                "Guardians of the Rift arena in the Temple of the Eye.",
                "Portal timing, altar choices, pouch capacity, and match outcome change XP per game. The planner therefore reports the exact XP gap, not a fabricated match count."
        );
    }

    private static RecommendationGuidance zmi(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Fill every usable essence pouch, follow the safe altar path, craft the mixed rune result, bank, and repeat until you gain " + format(xp) + " Runecraft XP for level " + target + ".",
                "Bring pure essence, usable essence pouches, and your preferred banking/teleport setup. " + observed(items, "Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch", "Ourania teleport"),
                "Ourania Altar, southwest of Ardougne.",
                "Ourania produces a mix of runes, and XP per essence depends on the result distribution and account level. Treat the remaining essence count as variable."
        );
    }

    private static RecommendationGuidance motherlode(int target, int xp, ObservedItemIndex items)
    {
        String pickaxe = pickaxe(items);
        return new RecommendationGuidance(
                "Mine pay-dirt on the lower level, clean it through the hopper, empty the sack, bank the ores, and repeat until you gain " + format(xp) + " Mining XP for level " + target + ". Keep golden nuggets for planned unlocks.",
                "Bring " + pickaxe + ". " + observed(items, "Prospector helmet", "Prospector jacket", "Prospector legs", "Prospector boots"),
                "Motherlode Mine beneath Falador.",
                "Vein uptime and pay-dirt flow vary. Golden-nugget goals such as Prospector or upper-level access can remain protected beyond an arbitrary level milestone."
        );
    }

    private static RecommendationGuidance shootingStars(int target, int xp, ObservedItemIndex items)
    {
        String pickaxe = pickaxe(items);
        return new RecommendationGuidance(
                "Mine a reachable Shooting Star until it depletes or you gain " + format(xp) + " Mining XP toward level " + target + ".",
                "Bring " + pickaxe + ". Keep stardust when Celestial ring or charge rewards still matter. " + observed(items, "Celestial ring", "Celestial signet"),
                "Use a discovered star location that respects the account's membership, access, and Wilderness-risk settings.",
                "Star tier changes while mining and affects XP. Exact swing counts would be false precision; this is primarily the low-attention Mining option."
        );
    }

    private static RecommendationGuidance volcanicMine(int target, int xp, ObservedItemIndex items)
    {
        String pickaxe = pickaxe(items);
        return new RecommendationGuidance(
                "Perform the already verified team role and complete Volcanic Mine rounds until you gain " + format(xp) + " Mining XP for level " + target + ".",
                "Bring " + pickaxe + " plus the food and stamina supplies in the verified team-role setup.",
                "Volcanic Mine on Fossil Island.",
                "Vent state, role, boulder phase, and points change XP per game, so no fixed game count is shown."
        );
    }

    private static RecommendationGuidance blastMine(int target, int xp, ObservedItemIndex items)
    {
        String pickaxe = pickaxe(items);
        return new RecommendationGuidance(
                "Place dynamite, light it, move clear, excavate the blasted rock, collect the ore, and repeat until you gain " + format(xp) + " Mining XP for level " + target + ".",
                "Bring dynamite and " + pickaxe + ". " + observed(items, "Dynamite"),
                "Blast Mine in Lovakengj.",
                "Ore tier and timing affect XP and dynamite use, so the plugin keeps the exact XP gap without claiming a universal dynamite count."
        );
    }

    private static RecommendationGuidance giantsFoundry(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Take a Giants' Foundry commission and use the best commission-compatible alloy your account can sustain until you gain " + format(xp) + " Smithing XP toward level " + target + ".",
                "Each commissioned sword consumes 28 bars' worth of metal. Prefer banked metal on Iron-style accounts and preserve quest or upgrade items. " + observed(items, "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar"),
                "Choose the best owned moulds for the commission, pour the 28-bar alloy, then work the sword through each temperature station while staying inside the target band.",
                "Mould choice, alloy, commission, and mistakes change XP per sword. Each commission uses 28 bars, but the number of swords needed is variable."
        );
    }

    private static RecommendationGuidance mahoganyHomes(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Take the highest Mahogany Homes contract tier your level and plank supply can sustain, then complete contracts until you gain " + format(xp) + " Construction XP toward level " + target + ".",
                "Bring a hammer, saw, contract-city teleports, and the matching plank tier. Use a plank sack when owned. " + observed(items, "Plank sack", "Oak plank", "Teak plank", "Mahogany plank", "Steel bar"),
                "Get a contract, travel to the named client, repair every marked hotspot, then take the next contract.",
                "Furniture mix varies by client, so exact planks and XP per contract require the live contract state. No universal contract count is fabricated."
        );
    }

    private static RecommendationGuidance mixology(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Fill Mastering Mixology orders until you gain " + format(xp) + " Herblore XP toward level " + target + ". Prioritize orders that support the reward goal you are currently protecting.",
                "Convert spare eligible herbs into paste while preserving herbs needed for higher-value combat potions. " + observed(items, "Mox paste", "Aga paste", "Lye paste", "Digweed"),
                "Alchemical Society in Aldarin.",
                "Order combinations change continuously. Exact paste-to-level counts require live order state; XP per order is not assumed constant."
        );
    }

    private static RecommendationGuidance titheFarm(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run Tithe Farm with the highest fruit seed tier unlocked and a plot count you can water reliably. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a spade and watering cans. Use Gricoller's can when owned. " + observed(items, "Gricoller's can", "Seed box", "Farmer's strawhat", "Farmer's jacket", "Farmer's boro trousers", "Farmer's boots"),
                "Tithe Farm in Hosidius.",
                "Plot count and missed cycles change XP per hour. Reward goals such as the can, seed box, or outfit can remain protected instead of being abandoned at a level breakpoint."
        );
    }

    private static RecommendationGuidance farmingAllotments(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run every verified reachable allotment patch that is ready, then replant using the best useful seed supply. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a rake, seed dibber, spade, compost plan, and the selected allotment seeds. " + observed(items, "Seed dibber", "Spade", "Rake", "Bottomless compost bucket", "Gricoller's can"),
                "Use the Farming checklist for observed patches instead of assuming every unlocked patch is currently empty or ready.",
                "Harvest yield is variable, so a seed-to-level count would be false. Patch state, seed supply, and travel access should drive the next run."
        );
    }

    private static RecommendationGuidance farmingHerbs(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Harvest and replant every verified herb patch that is ready, using the herb seed with the best current account value. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a seed dibber, spade, compost plan, herb seeds, and patch teleports. " + observed(items, "Seed dibber", "Spade", "Bottomless compost bucket", "Magic secateurs", "Seed box"),
                "Follow the live Farming checklist so dead, diseased, growing, and ready patches are handled differently instead of blindly running a fixed route.",
                "Herb yield is variable and Iron accounts may value potion supply over raw Farming XP, so no fabricated exact seed count is shown."
        );
    }

    private static RecommendationGuidance farmingContracts(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Complete the current Farming Guild contract when its crop is ready, then take the highest practical contract tier. Farming still needs " + format(xp) + " XP to level " + target + ".",
                "Keep common contract seeds and planting tools available when storage allows. " + observed(items, "Seed box", "Spade", "Seed dibber", "Bottomless compost bucket"),
                "Use the Farming Guild contract area. Pre-plant only crops that do not block a more valuable active Farming objective.",
                "Contracts are primarily a seed-supply progression loop, not constant XP per contract. They should be surfaced as a detour when the seed value outweighs continuous training."
        );
    }

    private static RecommendationGuidance hunterRumours(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Take the highest Hunter Rumour tier you can access and complete the assigned creature until you gain " + format(xp) + " Hunter XP toward level " + target + ".",
                "Bring the trap or tool required by the assigned creature and your hunter's whistle when available. " + observed(items, "Basic quetzal whistle", "Enhanced quetzal whistle", "Perfected quetzal whistle"),
                "Get the rumour at the Hunter Guild, travel to the assigned creature, obtain its rare piece, then return it for the reward sack and next assignment.",
                "Assignment and rare-part RNG change XP per rumour. Creature-specific loadouts should replace this generic setup once the current rumour can be observed directly."
        );
    }

    private static RecommendationGuidance herbiboar(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Track and harvest herbiboars until you gain " + format(xp) + " Hunter XP toward level " + target + ". Bank or process herbs according to the account's Herblore needs.",
                "Bring a herb sack when owned, plus stamina or graceful-style movement support if useful. " + observed(items, "Herb sack", "Magic secateurs", "Graceful hood", "Graceful top", "Graceful legs", "Graceful boots"),
                "Herbiboar hunting area on Fossil Island.",
                "Track length and herb rewards vary. Iron accounts may prefer this over higher raw Hunter XP when Herblore supplies are scarce."
        );
    }

    private static RecommendationGuidance forestry(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Cut the best reachable Forestry-enabled tree and participate in nearby events when they appear until you gain " + format(xp) + " Woodcutting XP toward level " + target + ".",
                "Bring your best usable axe and Forestry kit when owned. " + observed(items, "Forestry kit", "Dragon axe", "Crystal axe", "Rune axe", "Lumberjack hat", "Lumberjack top", "Lumberjack legs", "Lumberjack boots"),
                "Choose a populated Forestry tree area that fits the selected tree tier and access state. Stay with the tree group so event downtime does not become wasted travel.",
                "Event frequency and event type vary, so exact event counts are not meaningful. The planner treats Forestry as varied Woodcutting plus reward progression."
        );
    }

    private static RecommendationGuidance pyramidPlunder(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run Pyramid Plunder and spend most of the timer in the highest rooms you can access until you gain " + format(xp) + " Thieving XP toward level " + target + ".",
                "Bring food or healing, antipoison if your route needs it, and free inventory space for loot. " + observed(items, "Pharaoh's sceptre", "Dodgy necklace", "Antipoison(4)", "Superantipoison(4)"),
                "Pyramid Plunder in Sophanem.",
                "Room access, failed traps, chest and sarcophagus choices, and sceptre hunting change XP per run, so no fixed run count is shown."
        );
    }

    private static RecommendationGuidance varlamoreThieving(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Use the unlocked Varlamore citizen and house-robbery loop until you gain " + format(xp) + " Thieving XP toward level " + target + ".",
                "Bring only the healing and teleport support needed for the chosen loop; keep enough inventory room for valuables. " + observed(items, "Dodgy necklace", "Rogue top", "Rogue trousers", "Rogue gloves", "Rogue boots"),
                "Use the Varlamore Thieving area unlocked by your current quest/access state and stay on the low-friction loop rather than mixing in unrelated travel.",
                "House availability and loop timing vary, so exact XP remaining is shown without claiming a fixed number of robberies."
        );
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
                ? ""
                : "Observed: " + found + ".";
    }

    private static String pickaxe(ObservedItemIndex items)
    {
        String[] names = {"Crystal pickaxe", "Infernal pickaxe",
                "3rd age pickaxe", "Dragon pickaxe", "Rune pickaxe",
                "Adamant pickaxe", "Mithril pickaxe", "Black pickaxe",
                "Steel pickaxe", "Iron pickaxe", "Bronze pickaxe"};
        for (String name : names) if (items.has(name)) return name;
        return "a pickaxe obtained before leaving";
    }

    private static String firstObserved(
            ObservedItemIndex items, String... names)
    {
        for (String name : names) if (items.has(name)) return name;
        return null;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}
