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
            case "fishing_karambwan": return karambwans(targetLevel, xpNeeded, items);
            case "runecraft_gotr": return gotr(targetLevel, xpNeeded, items);
            case "runecraft_zmi": return zmi(targetLevel, xpNeeded, items);
            case "mining_mlm": return motherlode(targetLevel, xpNeeded, items);
            case "mining_stars": return shootingStars(data, targetLevel, xpNeeded, items);
            case "mining_volcanic": return volcanicMine(targetLevel, xpNeeded, items);
            case "mining_blast_mine": return blastMine(targetLevel, xpNeeded, items);
            case "smithing_foundry":
            case "smithing_giants_foundry": return giantsFoundry(targetLevel, xpNeeded, items);
            case "construction_homes":
            case "construction_mahogany_homes": return mahoganyHomes(data, targetLevel, xpNeeded, items);
            case "herblore_mixology": return mixology(targetLevel, xpNeeded, items);
            case "farming_tithe": return titheFarm(data, targetLevel, xpNeeded, items);
            case "farming_allotments_expanded": return farmingAllotments(data, targetLevel, xpNeeded, items);
            case "farming_falador_potatoes": return faladorPotatoes(targetLevel, xpNeeded, items);
            case "farming_falador_watermelons": return faladorWatermelons(targetLevel, xpNeeded, items);
            case "farming_herbs_expanded": return farmingHerbs(data, targetLevel, xpNeeded, items);
            case "farming_contracts": return farmingContracts(data, targetLevel, xpNeeded, items);
            case "hunter_rumours": return hunterRumours(data, targetLevel, xpNeeded, items);
            case "hunter_herbiboar": return herbiboar(targetLevel, xpNeeded, items);
            case "woodcutting_forestry": return forestry(data, targetLevel, xpNeeded, items);
            case "thieving_pyramid": return pyramidPlunder(targetLevel, xpNeeded, items);
            case "thieving_varlamore": return varlamoreThieving(targetLevel, xpNeeded, items);
            default: return null;
        }
    }

    private static RecommendationGuidance wintertodt(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Chop bruma roots, feed and repair braziers, reach at least 500 personal points, then repeat until you gain " + format(xp) + " Firemaking XP for level " + target + ". Fletch only when needed to secure the 500-point threshold.",
                "Equip four warm items and bring a knife, hammer, and cakes. Each cake bite heals at least 4 Hitpoints and restores 35% warmth under the current warmth system. " + observed(items, "Cake", "Bruma torch", "Warm gloves", "Pyromancer hood", "Pyromancer garb", "Pyromancer robe", "Pyromancer boots"),
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

    private static RecommendationGuidance karambwans(
            int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Load the vessel with raw karambwanji, fish the static spot until full, take fairy ring DKP to Zanaris, bank, return through the Zanaris ring to DKP, and repeat until you gain " + format(xp) + " Fishing XP for level " + target + ".",
                "Bring a karambwan vessel, raw karambwanji, and a dramen or lunar staff unless staffless fairy-ring use is unlocked. "
                        + observed(items, "Karambwan vessel", "Raw karambwanji", "Fish barrel"),
                "Karambwan fishing spot north of fairy ring DKP on north-east Karamja.",
                "The static spot and stackable bait make this the low-attention option. Faster bank teleports can improve the loop later without being assumed here."
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
                "Bring pure essence and every usable essence pouch; leave inventory space for the mixed runes. " + observed(items, "Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch", "Ourania teleport"),
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

    private static RecommendationGuidance shootingStars(
            StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        MembershipStatus membership = data.getAccount().getMembershipStatus();
        String scout = membership == MembershipStatus.P2P
                ? "Use an unobstructed POH telescope to obtain the landing region and time, then check each named landing site in that region."
                : "Check the 21 free-to-play landing sites after a star wave; begin with the safe Falador mine and Varrock mine sites and stop only when a crashed star is visible.";
        String pickaxe = pickaxe(items);
        return new RecommendationGuidance(
                scout + " Mine the located star until it depletes or you gain " + format(xp) + " Mining XP toward level " + target + ".",
                "Bring " + pickaxe + ". Keep stardust when Celestial ring or charge rewards still matter. " + observed(items, "Celestial ring", "Celestial signet"),
                membership == MembershipStatus.P2P
                        ? "The exact non-Wilderness crash site found from the telescope's named region."
                        : "A visible crashed star at a free-to-play Falador or Varrock mine landing site.",
                "Stars land at random from fixed sites, so Compass cannot truthfully name an active crash site without observing one. Star tier changes while mining and affects XP; exact swing counts would be false precision."
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
        FoundryAlloy alloy = foundryAlloy(items);
        if (alloy == null) return null;
        return new RecommendationGuidance(
                "Ask Kovac for a commission. Load " + alloy.description
                        + " into the crucible. For each blade section, select the owned mould with the highest green score shown for that commission, pour the sword, then work every temperature station inside its target band. Repeat for "
                        + format(xp) + " Smithing XP toward level " + target + ".",
                "Bring " + alloy.description + "; each commissioned sword consumes 28 bars' worth of metal. "
                        + observed(items, "Iron bar", "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar"),
                "Giants' Foundry beneath Kovac's workshop, east of Al Kharid.",
                "The 14/14 adjacent-metal alloy is a verified default when both metals are observed. Mould score, commission, and mistakes change XP per sword, so no fixed sword count is shown."
        );
    }

    private static RecommendationGuidance mahoganyHomes(
            StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data.getAccount().getSkillLevel(Skill.CONSTRUCTION);
        ContractTier tier = contractTier(level, items);
        if (tier == null) return null;
        return new RecommendationGuidance(
                "Ask Amy for a " + tier.name + " contract, travel to the named client, repair every marked hotspot, speak to the client, and take another " + tier.name + " contract. Repeat for " + format(xp) + " Construction XP toward level " + target + ".",
                "Bring a hammer, saw, at least 15 " + tier.plank.toLowerCase(java.util.Locale.ROOT)
                        + ", one steel bar, and teleports for Falador, Varrock, East Ardougne, and Hosidius. Use a plank sack when owned. "
                        + observed(items, "Plank sack", tier.plank, "Steel bar"),
                "Amy at Mahogany Homes, immediately south of Falador Park.",
                "Furniture mix varies by client, so exact planks and XP per contract require the live contract state. No universal contract count is fabricated."
        );
    }

    private static RecommendationGuidance mixology(int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Complete all three displayed potion orders together for the 40% resin bonus, then take the next three. Repeat until you gain " + format(xp) + " Herblore XP toward level " + target + ".",
                "Bring herbs already committed to Mixology and convert them into the displayed Mox, Aga, and Lye paste shortfalls. " + observed(items, "Mox paste", "Aga paste", "Lye paste", "Digweed"),
                "Alchemical Society in Aldarin.",
                "Order combinations change continuously. Exact paste-to-level counts require live order state; XP per order is not assumed constant."
        );
    }

    private static RecommendationGuidance titheFarm(StrategyDataBundle data,
            int target, int xp, ObservedItemIndex items)
    {
        int level = data == null || data.getAccount() == null ? 34
                : data.getAccount().getSkillLevel(net.runelite.api.Skill.FARMING);
        String seed = level >= 74 ? "Logavano"
                : level >= 54 ? "Bologano" : "Golovanova";
        return new RecommendationGuidance(
                "Take " + seed + " seeds from the table. Plant and immediately water a 20-plot cycle, revisit each plant for every watering stage, harvest, deposit the fruit, and repeat for " + format(xp) + " Farming XP to level " + target + ".",
                "Bring a spade, seed dibber, and eight filled watering cans; Gricoller's can replaces the eight cans. " + observed(items, "Gricoller's can", "Seed box", "Farmer's strawhat", "Farmer's jacket", "Farmer's boro trousers", "Farmer's boots"),
                "Tithe Farm in Hosidius.",
                "Plot count and missed cycles change XP per hour. Reward goals such as the can, seed box, or outfit can remain protected instead of being abandoned at a level breakpoint."
        );
    }

    private static RecommendationGuidance farmingAllotments(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data.getAccount().getSkillLevel(Skill.FARMING);
        String seed = highestObservedAllotmentSeed(items, level);
        if (seed == null) return null;
        String patch = new FarmingAccessEvaluator(new FarmingAccessCatalog())
                .firstReachablePatchName(data.getFarming());
        if (patch == null) return null;
        return new RecommendationGuidance(
                "At " + patch + ", harvest each ready allotment, plant three "
                        + seed.toLowerCase(java.util.Locale.ROOT)
                        + " in each cleared allotment, compost, and return after the crop is ready. Repeat for "
                        + format(xp) + " Farming XP to level " + target + ".",
                "Bring six " + seed.toLowerCase(java.util.Locale.ROOT)
                        + ", a rake, seed dibber, spade, and compost. "
                        + observed(items, seed, "Seed dibber", "Spade", "Rake", "Bottomless compost bucket", "Gricoller's can"),
                patch + ".",
                "Harvest yield is variable, so a seed-to-level count would be false. Patch state, seed supply, and travel access should drive the next run."
        );
    }

    private static RecommendationGuidance faladorPotatoes(
            int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Rake one allotment, plant three potato seeds, leave while they grow, then return with a spade to harvest and replant; repeat this loop until you gain "
                        + format(xp) + " Farming XP toward level " + target + ".",
                "Bring three potato seeds, a rake, seed dibber, and spade. Buy tools from Sarah at the farm and potato seeds from Olivia in Draynor Village when missing. "
                        + observed(items, "Potato seed", "Rake", "Seed dibber", "Spade"),
                "South Falador Farm allotments, between Falador and Port Sarim.",
                "Harvest yield and disease are variable, so Compass does not invent a seed-to-level count. Compost is optional but reduces disease and improves yield."
        );
    }

    private static RecommendationGuidance faladorWatermelons(
            int target, int xp, ObservedItemIndex items)
    {
        return new RecommendationGuidance(
                "Rake one allotment, plant three watermelon seeds, leave while they grow, then return with a spade to harvest and replant. Repeat until you gain "
                        + format(xp) + " Farming XP toward level " + target + ".",
                "Bring three watermelon seeds, a rake, seed dibber, and spade. If seeds are missing, pickpocket Master Farmers in Draynor Village until you have three; the number of pickpockets is variable. "
                        + observed(items, "Watermelon seed", "Rake", "Seed dibber", "Spade"),
                "South Falador Farm allotments, between Falador and Port Sarim.",
                "Harvest yield and disease are variable, so Compass does not invent a seed-to-level count. Compost is optional but reduces disease and improves yield."
        );
    }

    private static RecommendationGuidance farmingHerbs(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data == null || data.getAccount() == null ? 9
                : data.getAccount().getSkillLevel(net.runelite.api.Skill.FARMING);
        String seed = herbSeed(items, level);
        if (seed == null) return null;
        String patch = new FarmingAccessEvaluator(new FarmingAccessCatalog())
                .firstReachableHerbPatchName(
                        data == null ? null : data.getFarming());
        if (patch == null) return null;
        return new RecommendationGuidance(
                "At " + patch + ", harvest any ready herbs, plant " + seed + ", apply compost when carried, and return after the patch is ready. Repeat for " + format(xp) + " Farming XP to level " + target + ".",
                "Bring " + seed + ", a seed dibber, and a spade. Compost is optional but protects yield. " + observed(items, "Seed dibber", "Spade", "Bottomless compost bucket", "Magic secateurs", "Seed box"),
                patch + ".",
                "Herb yield is variable and Iron accounts may value potion supply over raw Farming XP, so no fabricated exact seed count is shown."
        );
    }

    private static String herbSeed(ObservedItemIndex items, int level)
    {
        String[][] tiers = {
                {"Torstol seed", "85"}, {"Dwarf weed seed", "79"},
                {"Lantadyme seed", "73"}, {"Cadantine seed", "67"},
                {"Snapdragon seed", "62"}, {"Kwuarm seed", "56"},
                {"Avantoe seed", "50"}, {"Irit seed", "44"},
                {"Toadflax seed", "38"}, {"Ranarr seed", "32"},
                {"Harralander seed", "26"}, {"Tarromin seed", "19"},
                {"Marrentill seed", "14"}, {"Guam seed", "9"}
        };
        for (String[] tier : tiers)
        {
            if (level >= Integer.parseInt(tier[1])
                    && items.quantity(tier[0]) > 0)
                return "one " + tier[0].toLowerCase(java.util.Locale.ROOT);
        }
        return null;
    }

    private static RecommendationGuidance farmingContracts(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data.getAccount().getSkillLevel(Skill.FARMING);
        String tier = level >= 85 ? "hard" : level >= 65 ? "medium" : "easy";
        return new RecommendationGuidance(
                "Ask Guildmaster Jane for a " + tier + " contract. Grow the named crop inside the Farming Guild, check its health or finish harvesting it, claim the seed pack, and request another " + tier + " contract. Farming still needs " + format(xp) + " XP to level " + target + ".",
                "Keep common contract seeds and planting tools available when storage allows. " + observed(items, "Seed box", "Spade", "Seed dibber", "Bottomless compost bucket"),
                "Guildmaster Jane in the central Farming Guild greenhouse, Kebos Lowlands.",
                "Contracts are primarily a seed-supply progression loop, not constant XP per contract. They should be surfaced as a detour when the seed value outweighs continuous training."
        );
    }

    private static RecommendationGuidance hunterRumours(
            StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data.getAccount().getSkillLevel(Skill.HUNTER);
        boolean master = level >= 91 && data.getQuests() != null
                && data.getQuests().statusOf("At First Light") == QuestStatus.COMPLETE;
        String tier = master ? "Master" : level >= 72 ? "Expert"
                : level >= 57 ? "Adept" : "Novice";
        String hunter = master ? "Guild Hunter Wolf"
                : level >= 72 ? "Guild Hunter Teco"
                : level >= 57 ? "Guild Hunter Ornus"
                : "Huntmaster Gilman";
        return new RecommendationGuidance(
                "Get a " + tier + " rumour from " + hunter
                        + ". Hunt the named creature until it drops the rare part, return the part for the loot sack, and take another rumour. Repeat for "
                        + format(xp) + " Hunter XP toward level " + target + ".",
                "Bring the trap or tool required by the assigned creature and your hunter's whistle when available. " + observed(items, "Basic quetzal whistle", "Enhanced quetzal whistle", "Perfected quetzal whistle"),
                hunter + " in the Burrow beneath the Hunter Guild, Varlamore.",
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

    private static RecommendationGuidance forestry(
            StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data.getAccount().getSkillLevel(Skill.WOODCUTTING);
        String tree = level >= 60 ? "yew trees" : level >= 45
                ? "maple trees" : level >= 30 ? "willow trees" : "oak trees";
        String location = level >= 60
                ? "Yew trees beside Seers' Village church."
                : level >= 45
                        ? "Maple trees immediately north of Seers' Village bank."
                        : level >= 30
                                ? "Willow trees south of Draynor Village bank."
                                : "Oak trees east of Draynor Village bank.";
        String axe = axe(items);
        return new RecommendationGuidance(
                "On an official Forestry world, cut " + tree
                        + " and complete each event that spawns until you gain "
                        + format(xp) + " Woodcutting XP toward level " + target + ".",
                "Bring " + axe + " and a Forestry kit when owned. " + observed(items, "Forestry kit", "Dragon axe", "Crystal axe", "Rune axe", "Lumberjack hat", "Lumberjack top", "Lumberjack legs", "Lumberjack boots"),
                location,
                "Event frequency and event type vary, so exact event counts are not meaningful. The planner treats Forestry as varied Woodcutting plus reward progression."
        );
    }

    private static FoundryAlloy foundryAlloy(ObservedItemIndex items)
    {
        String[][] adjacent = {
                {"Runite bar", "Adamantite bar"},
                {"Adamantite bar", "Mithril bar"},
                {"Mithril bar", "Steel bar"},
                {"Steel bar", "Iron bar"}
        };
        for (String[] pair : adjacent)
        {
            if (items.quantity(pair[0]) >= 14 && items.quantity(pair[1]) >= 14)
                return new FoundryAlloy("14 " + pair[0].toLowerCase(java.util.Locale.ROOT)
                        + " and 14 " + pair[1].toLowerCase(java.util.Locale.ROOT));
        }
        String[] metals = {"Runite bar", "Adamantite bar", "Mithril bar",
                "Steel bar", "Iron bar"};
        for (String metal : metals)
            if (items.quantity(metal) >= 28)
                return new FoundryAlloy("28 " + metal.toLowerCase(java.util.Locale.ROOT));
        return null;
    }

    private static ContractTier contractTier(int level, ObservedItemIndex items)
    {
        ContractTier[] tiers = {
                new ContractTier("Expert", "Mahogany plank", 70),
                new ContractTier("Adept", "Teak plank", 50),
                new ContractTier("Novice", "Oak plank", 20),
                new ContractTier("Beginner", "Plank", 1)
        };
        for (ContractTier tier : tiers)
            if (level >= tier.level && items.quantity(tier.plank) >= 15)
                return tier;
        return null;
    }

    private static String highestObservedAllotmentSeed(
            ObservedItemIndex items, int level)
    {
        String[][] tiers = {
                {"Snape grass seed", "61"}, {"Watermelon seed", "47"},
                {"Strawberry seed", "31"}, {"Sweetcorn seed", "20"},
                {"Tomato seed", "12"}, {"Cabbage seed", "7"},
                {"Onion seed", "5"}, {"Potato seed", "1"}
        };
        for (String[] tier : tiers)
            if (level >= Integer.parseInt(tier[1])
                    && items.quantity(tier[0]) >= 6)
                return tier[0];
        return null;
    }

    private static final class FoundryAlloy
    {
        private final String description;

        private FoundryAlloy(String description)
        {
            this.description = description;
        }
    }

    private static final class ContractTier
    {
        private final String name;
        private final String plank;
        private final int level;

        private ContractTier(String name, String plank, int level)
        {
            this.name = name;
            this.plank = plank;
            this.level = level;
        }
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
                "Pickpocket wealthy citizens when a street urchin distracts them until you obtain a house key. Unlock an empty house, search the wardrobes, chests, and jewellery cases, take each flashing-object bonus, escape through the window, and repeat until you gain " + format(xp) + " Thieving XP toward level " + target + ".",
                "Bring a chisel for blessed bone statuettes and leave inventory space for valuables. " + observed(items, "House key", "Chisel", "Dodgy necklace", "Rogue top", "Rogue trousers", "Rogue gloves", "Rogue boots"),
                "Wealthy citizens in the Civitas illa Fortis bazaar, then Caius', Victor's, or Lavinia's empty house in the south-west city.",
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
        return "a bronze pickaxe; get one free from the Mining tutor at the east Lumbridge Swamp mine before leaving";
    }

    private static String axe(ObservedItemIndex items)
    {
        String[] names = {"Crystal axe", "Infernal axe", "Dragon axe",
                "Rune axe", "Adamant axe", "Mithril axe", "Black axe",
                "Steel axe", "Iron axe", "Bronze axe"};
        for (String name : names) if (items.has(name)) return name;
        return "a bronze axe; buy one from Bob's Brilliant Axes in Lumbridge before leaving";
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
