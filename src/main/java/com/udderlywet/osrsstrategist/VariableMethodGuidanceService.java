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
    private static final StaticGuidance[] STATIC_GUIDANCE =
            BundledCatalogLoader.array(
                    "/content/catalogs/variable-method-guidance.json",
                    StaticGuidance[].class);
    private static final FarmingAccessEvaluator FARMING_ACCESS =
            new FarmingAccessEvaluator(new FarmingAccessCatalog());

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
        RecommendationGuidance bundled = bundledGuidance(
                id, targetLevel, xpNeeded, items);
        if (bundled != null) return bundled;

        switch (id)
        {
            case "fishing_tempoross": return tempoross(targetLevel, xpNeeded, items);
            case "runecraft_gotr": return gotr(targetLevel, xpNeeded, items);
            case "mining_stars": return shootingStars(data, targetLevel, xpNeeded, items);
            case "smithing_foundry":
            case "smithing_giants_foundry": return giantsFoundry(targetLevel, xpNeeded, items);
            case "construction_homes":
            case "construction_mahogany_homes": return mahoganyHomes(data, targetLevel, xpNeeded, items);
            case "farming_tithe": return titheFarm(data, targetLevel, xpNeeded, items);
            case "farming_allotments_expanded": return farmingAllotments(data, targetLevel, xpNeeded, items);
            case "farming_herbs_expanded": return farmingHerbs(data, targetLevel, xpNeeded, items);
            case "farming_contracts": return farmingContracts(data, targetLevel, xpNeeded, items);
            case "hunter_rumours": return hunterRumours(data, targetLevel, xpNeeded, items);
            case "woodcutting_forestry": return forestry(data, targetLevel, xpNeeded, items);
            default: return null;
        }
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
        String patch = FARMING_ACCESS.firstReachablePatchName(
                data.getFarming());
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

    private static RecommendationGuidance farmingHerbs(StrategyDataBundle data, int target, int xp, ObservedItemIndex items)
    {
        int level = data == null || data.getAccount() == null ? 9
                : data.getAccount().getSkillLevel(net.runelite.api.Skill.FARMING);
        String seed = herbSeed(items, level);
        if (seed == null) return null;
        String patch = FARMING_ACCESS.firstReachableHerbPatchName(
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

    private static RecommendationGuidance bundledGuidance(
            String methodId, int target, int xp, ObservedItemIndex items)
    {
        for (StaticGuidance profile : STATIC_GUIDANCE)
        {
            if (!profile.matches(methodId)) continue;
            String observed = observed(items, profile.observed == null
                    ? new String[0] : profile.observed);
            String pickaxe = "pickaxe".equals(profile.tool)
                    ? pickaxe(items) : "";
            return new RecommendationGuidance(
                    profile.render(profile.action, target, xp, observed, pickaxe),
                    profile.render(profile.supplies, target, xp, observed, pickaxe),
                    profile.render(profile.location, target, xp, observed, pickaxe),
                    profile.render(profile.note, target, xp, observed, pickaxe));
        }
        return null;
    }

    private static final class StaticGuidance
    {
        private String[] ids;
        private String action;
        private String supplies;
        private String location;
        private String note;
        private String[] observed;
        private String tool;

        private boolean matches(String id)
        {
            if (ids == null) return false;
            for (String value : ids) if (value.equals(id)) return true;
            return false;
        }

        private String render(String template, int target, int xp,
                String observedItems, String pickaxe)
        {
            if (template == null) return "";
            return template.replace("{xp}", format(xp))
                    .replace("{target}", Integer.toString(target))
                    .replace("{observed}", observedItems)
                    .replace("{pickaxe}", pickaxe).trim();
        }
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
