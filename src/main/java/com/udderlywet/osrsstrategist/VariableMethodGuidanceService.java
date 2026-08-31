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
                    Text.get(1057),
                    StaticGuidance[].class);
    private static final FarmingAccessEvaluator FARMING_ACCESS =
            new FarmingAccessEvaluator(new FarmingAccessCatalog());

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }
        String id = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId();
        int currentXp = data.account().getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);
        ItemIndex items = new ItemIndex(data, useGroupStorage);
        Guidance bundled = bundledGuidance(
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

    private static Guidance tempoross(int target, int xp, ItemIndex items)
    {
        String harpoon = firstObserved(items, "Dragon harpoon",
                "Crystal harpoon", "Infernal harpoon", "Harpoon");
        return new Guidance(
                Text.get(1068) + format(xp) + Text.get(1467) + target + ".",
                harpoon == null
                        ? Text.get(1079)
                        : "Bring " + harpoon + Text.get(1090),
                Text.get(1101),
                Text.get(1103)
        );
    }

    private static Guidance gotr(int target, int xp, ItemIndex items)
    {
        String pouches = observed(items, "Small pouch", "Medium pouch",
                "Large pouch", "Giant pouch", "Colossal pouch");
        return new Guidance(
                Text.get(1104) + format(xp) + Text.get(1468) + target + ".",
                Text.get(1469)
                        + (pouches.isEmpty() ? "" : " " + pouches),
                Text.get(1105),
                Text.get(1106)
        );
    }

    private static Guidance shootingStars(
            GameData data, int target, int xp, ItemIndex items)
    {
        MembershipStatus membership = data.account().getMembershipStatus();
        String scout = membership == MembershipStatus.P2P
                ? Text.get(1058)
                : Text.get(1059);
        String pickaxe = pickaxe(items);
        return new Guidance(
                scout + Text.get(1060) + format(xp) + Text.get(1470) + target + ".",
                "Bring " + pickaxe + Text.get(1061) + observed(items, "Celestial ring", "Celestial signet"),
                membership == MembershipStatus.P2P
                        ? Text.get(1062)
                        : Text.get(1063),
                Text.get(1064)
        );
    }

    private static Guidance giantsFoundry(int target, int xp, ItemIndex items)
    {
        FoundryAlloy alloy = foundryAlloy(items);
        if (alloy == null) return null;
        return new Guidance(
                Text.get(1471) + alloy.description
                        + Text.get(1065)
                        + format(xp) + Text.get(1472) + target + ".",
                "Bring " + alloy.description + Text.get(1066)
                        + observed(items, "Iron bar", "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar"),
                Text.get(1067),
                Text.get(1069)
        );
    }

    private static Guidance mahoganyHomes(
            GameData data, int target, int xp, ItemIndex items)
    {
        int level = data.account().getSkillLevel(Skill.CONSTRUCTION);
        ContractTier tier = contractTier(level, items);
        if (tier == null) return null;
        return new Guidance(
                "Ask Amy for a " + tier.name + Text.get(1070) + tier.name + Text.get(1473) + format(xp) + Text.get(1474) + target + ".",
                Text.get(1475) + tier.plank.toLowerCase(java.util.Locale.ROOT)
                        + Text.get(1071)
                        + observed(items, "Plank sack", tier.plank, "Steel bar"),
                Text.get(1072),
                Text.get(1073)
        );
    }

    private static Guidance titheFarm(GameData data,
            int target, int xp, ItemIndex items)
    {
        int level = data == null || data.account() == null ? 34
                : data.account().getSkillLevel(net.runelite.api.Skill.FARMING);
        String seed = level >= 74 ? "Logavano"
                : level >= 54 ? "Bologano" : "Golovanova";
        return new Guidance(
                "Take " + seed + Text.get(1074) + format(xp) + Text.get(1476) + target + ".",
                Text.get(1075) + observed(items, "Gricoller's can", "Seed box", "Farmer's strawhat", "Farmer's jacket", Text.get(1221), "Farmer's boots"),
                Text.get(1477),
                Text.get(1076)
        );
    }

    private static Guidance farmingAllotments(GameData data, int target, int xp, ItemIndex items)
    {
        int level = data.account().getSkillLevel(Skill.FARMING);
        String seed = highestObservedAllotmentSeed(items, level);
        if (seed == null) return null;
        String patch = FARMING_ACCESS.firstReachablePatchName(
                data.farming());
        if (patch == null) return null;
        return new Guidance(
                "At " + patch + Text.get(1077)
                        + seed.toLowerCase(java.util.Locale.ROOT)
                        + Text.get(1078)
                        + format(xp) + Text.get(1476) + target + ".",
                "Bring six " + seed.toLowerCase(java.util.Locale.ROOT)
                        + Text.get(1080)
                        + observed(items, seed, "Seed dibber", "Spade", "Rake", Text.get(1478), "Gricoller's can"),
                patch + ".",
                Text.get(1081)
        );
    }

    private static Guidance farmingHerbs(GameData data, int target, int xp, ItemIndex items)
    {
        int level = data == null || data.account() == null ? 9
                : data.account().getSkillLevel(net.runelite.api.Skill.FARMING);
        String seed = herbSeed(items, level);
        if (seed == null) return null;
        String patch = FARMING_ACCESS.firstReachableHerbPatchName(
                data == null ? null : data.farming());
        if (patch == null) return null;
        return new Guidance(
                "At " + patch + Text.get(1479) + seed + Text.get(1082) + format(xp) + Text.get(1476) + target + ".",
                "Bring " + seed + Text.get(1083) + observed(items, "Seed dibber", "Spade", Text.get(1478), "Magic secateurs", "Seed box"),
                patch + ".",
                Text.get(1084)
        );
    }

    private static String herbSeed(ItemIndex items, int level)
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

    private static Guidance farmingContracts(GameData data, int target, int xp, ItemIndex items)
    {
        int level = data.account().getSkillLevel(Skill.FARMING);
        String tier = level >= 85 ? "hard" : level >= 65 ? "medium" : "easy";
        return new Guidance(
                Text.get(1480) + tier + Text.get(1085) + tier + Text.get(1481) + format(xp) + " XP to level " + target + ".",
                Text.get(1086) + observed(items, "Seed box", "Spade", "Seed dibber", Text.get(1478)),
                Text.get(1087),
                Text.get(1088)
        );
    }

    private static Guidance hunterRumours(
            GameData data, int target, int xp, ItemIndex items)
    {
        int level = data.account().getSkillLevel(Skill.HUNTER);
        boolean master = level >= 91 && data.quests() != null
                && data.quests().statusOf("At First Light") == QuestStatus.COMPLETE;
        String tier = master ? "Master" : level >= 72 ? "Expert"
                : level >= 57 ? "Adept" : "Novice";
        String hunter = master ? "Guild Hunter Wolf"
                : level >= 72 ? "Guild Hunter Teco"
                : level >= 57 ? Text.get(1482)
                : "Huntmaster Gilman";
        return new Guidance(
                "Get a " + tier + " rumour from " + hunter
                        + Text.get(1089)
                        + format(xp) + Text.get(1483) + target + ".",
                Text.get(1091) + observed(items, Text.get(1484), Text.get(1485), Text.get(1486)),
                hunter + Text.get(1092),
                Text.get(1093)
        );
    }

    private static Guidance forestry(
            GameData data, int target, int xp, ItemIndex items)
    {
        int level = data.account().getSkillLevel(Skill.WOODCUTTING);
        String tree = level >= 60 ? "yew trees" : level >= 45
                ? "maple trees" : level >= 30 ? "willow trees" : "oak trees";
        String location = level >= 60
                ? Text.get(1094)
                : level >= 45
                        ? Text.get(1095)
                        : level >= 30
                                ? Text.get(1096)
                                : Text.get(1097);
        String axe = axe(items);
        return new Guidance(
                Text.get(1487) + tree
                        + Text.get(1098)
                        + format(xp) + Text.get(1488) + target + ".",
                "Bring " + axe + Text.get(1489) + observed(items, "Forestry kit", "Dragon axe", "Crystal axe", "Rune axe", "Lumberjack hat", "Lumberjack top", "Lumberjack legs", "Lumberjack boots"),
                location,
                Text.get(1099)
        );
    }

    private static FoundryAlloy foundryAlloy(ItemIndex items)
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

    private static ContractTier contractTier(int level, ItemIndex items)
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
            ItemIndex items, int level)
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

    private static Guidance bundledGuidance(
            String methodId, int target, int xp, ItemIndex items)
    {
        for (StaticGuidance profile : STATIC_GUIDANCE)
        {
            if (!profile.matches(methodId)) continue;
            String observed = observed(items, profile.observed == null
                    ? new String[0] : profile.observed);
            String pickaxe = "pickaxe".equals(profile.tool)
                    ? pickaxe(items) : "";
            return new Guidance(
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

    private static String observed(ItemIndex items, String... names)
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

    private static String pickaxe(ItemIndex items)
    {
        String[] names = {"Crystal pickaxe", "Infernal pickaxe",
                "3rd age pickaxe", "Dragon pickaxe", "Rune pickaxe",
                "Adamant pickaxe", "Mithril pickaxe", "Black pickaxe",
                "Steel pickaxe", "Iron pickaxe", "Bronze pickaxe"};
        for (String name : names) if (items.has(name)) return name;
        return Text.get(1100);
    }

    private static String axe(ItemIndex items)
    {
        String[] names = {"Crystal axe", "Infernal axe", "Dragon axe",
                "Rune axe", "Adamant axe", "Mithril axe", "Black axe",
                "Steel axe", "Iron axe", "Bronze axe"};
        for (String name : names) if (items.has(name)) return name;
        return Text.get(1102);
    }

    private static String firstObserved(
            ItemIndex items, String... names)
    {
        for (String name : names) if (items.has(name)) return name;
        return null;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}
