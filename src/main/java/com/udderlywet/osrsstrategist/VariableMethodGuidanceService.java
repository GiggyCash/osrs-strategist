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
                "Run Wintertodt rounds and reach at least 500 personal points before each kill. You need " + format(xp) + " Firemaking XP to level " + target + ".",
                "Equip four warm items, bring a knife and hammer, and use food appropriate to your Hitpoints level. " + observed(items, "Bruma torch", "Warm gloves", "Pyromancer hood", "Pyromancer garb", "Pyromancer robe", "Pyromancer boots"),
                "Use the Wintertodt camp in northern Great Kourend. Chop bruma roots, feed braziers, repair broken braziers, and fletch only when extra points/reward value is worth lower raw Firemaking XP.",
                "Round length, interruptions, fletching, and player levels change XP per game. Strategist keeps exact XP remaining and does not invent a fixed kill count."
        );
    }

    private static RecommendationGuidance tempoross(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Complete Tempoross games while maintaining enough activity for reward permits. You need " + format(xp) + " Fishing XP to level " + target + ".",
                "Bring your best usable harpoon if owned; island tools cover the fallback. Rope, hammer, and buckets are available on the island. " + observed(items, "Dragon harpoon", "Crystal harpoon", "Infernal harpoon", "Harpoon"),
                "Fish harpoonfish, cook them when points/rewards matter, load cannons, and tether during waves.",
                "Cooking choice, team size, storm timing, and reward strategy change Fishing XP per game, so no fake game count is shown."
        );
    }

    private static RecommendationGuidance gotr(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Play Guardians of the Rift and balance elemental and catalytic energy until you gain " + format(xp) + " Runecraft XP toward level " + target + ".",
                "Bring a pickaxe, chisel, and every essence pouch your account can safely use. " + observed(items, "Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch", "Abyssal lantern"),
                "Mine fragments, craft guardian essence, use opened altars, and place cells where they protect or upgrade guardians.",
                "Portal timing, altar choices, pouch capacity, and match outcome change XP per game. The planner therefore reports the exact XP gap, not a fabricated match count."
        );
    }

    private static RecommendationGuidance zmi(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run Ourania Altar trips until you gain " + format(xp) + " Runecraft XP toward level " + target + ". Fill every usable essence pouch before each trip.",
                "Bring pure essence, usable essence pouches, and your preferred banking/teleport setup. " + observed(items, "Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch", "Ourania teleport"),
                "Bank near the Ourania entrance, follow the safe altar path, craft the mixed rune result, then repeat with full pouches.",
                "Ourania produces a mix of runes and XP per essence depends on the result distribution and account level. Strategist will not turn the XP gap into a fake fixed essence count."
        );
    }

    private static RecommendationGuidance motherlode(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Mine pay-dirt at Motherlode Mine until you gain " + format(xp) + " Mining XP toward level " + target + ", banking ores and preserving golden nuggets for useful unlocks.",
                "Bring your best usable pickaxe. " + observed(items, "Dragon pickaxe", "Crystal pickaxe", "Infernal pickaxe", "Rune pickaxe", "Prospector helmet", "Prospector jacket", "Prospector legs", "Prospector boots"),
                "Use the lower level until upper-level access is available, then prefer upper veins for longer depletion timers and lower attention.",
                "Vein uptime and pay-dirt flow vary. Golden-nugget goals such as Prospector or upper-level access can remain protected beyond an arbitrary level milestone."
        );
    }

    private static RecommendationGuidance shootingStars(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Mine a reachable Shooting Star until it depletes or you gain " + format(xp) + " Mining XP toward level " + target + ".",
                "Only a usable pickaxe is required. Keep stardust when Celestial ring or charge rewards still matter. " + observed(items, "Celestial ring", "Celestial signet", "Dragon pickaxe", "Rune pickaxe"),
                "Use a discovered star location that respects the account's membership, access, and Wilderness-risk settings.",
                "Star tier changes while mining and affects XP. Exact swing counts would be false precision; this is primarily the low-attention Mining option."
        );
    }

    private static RecommendationGuidance volcanicMine(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run Volcanic Mine with a verified team and role until you gain " + format(xp) + " Mining XP toward level " + target + ".",
                "Bring your best pickaxe plus food and stamina support required by the team's role. " + observed(items, "Dragon pickaxe", "Crystal pickaxe", "Rune pickaxe"),
                "Use Volcanic Mine only after its access requirements and intended team role are verified.",
                "Vent state, role, boulder phase, and points change XP per game. Strategist deliberately refuses to fabricate a game count."
        );
    }

    private static RecommendationGuidance blastMine(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Mine blasted ore until you gain " + format(xp) + " Mining XP toward level " + target + ". Keep a consistent place, light, move, excavate, collect rhythm.",
                "Bring dynamite and your best usable pickaxe. " + observed(items, "Dynamite", "Dragon pickaxe", "Rune pickaxe"),
                "Use Blast Mine in Lovakengj and maintain safe distance from lit dynamite.",
                "Ore tier and timing affect XP and dynamite use, so the plugin keeps the exact XP gap without claiming a universal dynamite count."
        );
    }

    private static RecommendationGuidance giantsFoundry(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Take a Giants' Foundry commission and use the best commission-compatible alloy your account can sustain until you gain " + format(xp) + " Smithing XP toward level " + target + ".",
                "Each commissioned sword consumes 28 bars' worth of metal. Prefer banked metal on Iron-style accounts and preserve quest or upgrade items. " + observed(items, "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar"),
                "Choose the best owned moulds for the commission, pour the 28-bar alloy, then work the sword through each temperature station while staying inside the target band.",
                "Mould score, alloy, commission, and mistakes change XP per sword. Strategist can state the 28-bar input rule without inventing a sword count to the level."
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
                "At the Alchemical Society, mix the ordered potion, use its correct processing station, then hand it in before choosing the next order.",
                "Order combinations change continuously. Exact paste-to-level counts require live order state, so Strategist does not assume constant XP per order."
        );
    }

    private static RecommendationGuidance titheFarm(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run Tithe Farm with the highest fruit seed tier unlocked and a plot count you can water reliably. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a spade and watering cans. Use Gricoller's can when owned. " + observed(items, "Gricoller's can", "Seed box", "Farmer's strawhat", "Farmer's jacket", "Farmer's boro trousers", "Farmer's boots"),
                "Plant, water every growth cycle, harvest the fruit, and deposit it for points before the batch expires.",
                "Plot count and missed cycles change XP per hour. Reward goals such as the can, seed box, or outfit can remain protected instead of being abandoned at a level breakpoint."
        );
    }

    private static RecommendationGuidance farmingAllotments(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Run every verified reachable allotment patch that is ready, then replant using the best useful seed supply. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a rake, seed dibber, spade, compost plan, and the selected allotment seeds. " + observed(items, "Seed dibber", "Spade", "Rake", "Bottomless compost bucket", "Gricoller's can"),
                "Use Strategist's Farming checklist for patches it has actually observed instead of assuming every unlocked patch is currently empty or ready.",
                "Harvest yield is variable, so a seed-to-level count would be false. Patch state, seed supply, and travel access should drive the next run."
        );
    }

    private static RecommendationGuidance farmingHerbs(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Harvest and replant every verified herb patch that is ready, using the herb seed with the best current account value. You need " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a seed dibber, spade, compost plan, herb seeds, and patch teleports. " + observed(items, "Seed dibber", "Spade", "Bottomless compost bucket", "Magic secateurs", "Seed box"),
                "Follow the live Farming checklist so dead, diseased, growing, and ready patches are handled differently instead of blindly running a fixed route.",
                "Herb yield is variable and Iron accounts may value potion supply over raw Farming XP. Strategist therefore optimizes the run rather than fabricating an exact seed count."
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
                "Follow herbiboar tracks around Fossil Island and inspect the final burrow before beginning the next trail.",
                "Track length and herb rewards vary. Iron accounts gain extra value from herbs, so Strategist can prefer this over higher raw Hunter XP when Herblore supply is scarce."
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
                "Enter Pyramid Plunder in Sophanem, move quickly through low-value rooms, and prioritize urns/chests in your highest available room according to the chosen sceptre-versus-XP goal.",
                "Room access, failed traps, chest/sarcophagus choices, and sceptre hunting change XP per run. Strategist therefore does not invent a run count."
        );
    }

    private static RecommendationGuidance varlamoreThieving(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Use the unlocked Varlamore citizen and house-robbery loop until you gain " + format(xp) + " Thieving XP toward level " + target + ".",
                "Bring only the healing and teleport support needed for the chosen loop; keep enough inventory room for valuables. " + observed(items, "Dodgy necklace", "Rogue top", "Rogue trousers", "Rogue gloves", "Rogue boots"),
                "Use the Varlamore Thieving area unlocked by your current quest/access state and stay on the low-friction loop rather than mixing in unrelated travel.",
                "House availability and loop timing vary, so Strategist preserves exact XP remaining without claiming a fixed number of robberies."
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
                ? "No relevant setup item from this short list is currently observed."
                : "Observed: " + found + ".";
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}
