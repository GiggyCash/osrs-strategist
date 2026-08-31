package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Renders variable-output methods from bundled templates and live account variables. */
@Singleton
public class VariableMethodGuidanceService
{
    private static final Profile[] PROFILES = BundledCatalogLoader.array(
            Text.get(1057), Profile[].class);
    private static final FarmingAccessEvaluator FARMING =
            new FarmingAccessEvaluator(new FarmingAccessCatalog());

    public Guidance build(GameData data, Skill skill, int currentLevel,
            int targetLevel, TrainingPlan plan, boolean useGroupStorage)
    {
        if (data == null || data.account() == null || skill == null
                || plan == null || plan.getMethod() == null) return null;
        int currentXp = data.account().getSkillExperience(skill);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        Map<String, String> common = new HashMap<>();
        common.put("xp", format(Math.max(0,
                Experience.getXpForLevel(targetLevel) - currentXp)));
        common.put("target", Integer.toString(targetLevel));
        ItemIndex items = new ItemIndex(data, useGroupStorage);
        for (Profile profile : PROFILES)
        {
            if (!profile.matches(plan.getMethod().getId())) continue;
            Map<String, String> values = variables(profile, data, items);
            if (values == null) return null;
            values.putAll(common);
            values.putIfAbsent("observed", observed(items, profile.observed));
            values.putIfAbsent("pickaxe", tool(items, true));
            return new Guidance(profile.render(profile.action, values),
                    profile.render(profile.supplies, values),
                    profile.render(profile.location, values),
                    profile.render(profile.note, values));
        }
        return null;
    }

    private static Map<String, String> variables(Profile profile,
            GameData data, ItemIndex items)
    {
        Map<String, String> v = new HashMap<>();
        String kind = profile.kind;
        if (kind == null) return v;
        int farming = data.account().getSkillLevel(Skill.FARMING);
        switch (kind)
        {
            case "tempoross":
                String harpoon = first(items, "Dragon harpoon", "Crystal harpoon",
                        "Infernal harpoon", "Harpoon");
                v.put("temporossSupplies", harpoon == null ? Text.get(1079)
                        : "Bring " + harpoon + Text.get(1090));
                break;
            case "gotr":
                String pouches = observed(items, "Small pouch", "Medium pouch",
                        "Large pouch", "Giant pouch", "Colossal pouch");
                v.put("pouches", pouches.isEmpty() ? "" : " " + pouches);
                break;
            case "stars":
                boolean members = data.account().getMembershipStatus()
                        == MembershipStatus.P2P;
                v.put("starScout", Text.get(members ? 1058 : 1059));
                v.put("starLocation", Text.get(members ? 1062 : 1063));
                break;
            case "foundry":
                String alloy = alloy(items);
                if (alloy == null) return null;
                v.put("alloy", alloy);
                break;
            case "homes":
                String[] contract = contract(data.account().getSkillLevel(
                        Skill.CONSTRUCTION), items);
                if (contract == null) return null;
                v.put("contract", contract[0]);
                v.put("plank", contract[1]);
                v.put("plankLower", contract[1].toLowerCase(Locale.ROOT));
                break;
            case "tithe":
                v.put("seed", farming >= 74 ? "Logavano"
                        : farming >= 54 ? "Bologano" : "Golovanova");
                break;
            case "allotments":
                String seed = tier(items, farming, 6, ALLOTMENTS);
                String patch = FARMING.firstReachablePatchName(data.farming());
                if (seed == null || patch == null) return null;
                v.put("seedLower", seed.toLowerCase(Locale.ROOT));
                v.put("patch", patch);
                v.put("observed", observed(items, seed, "Seed dibber", "Spade",
                        "Rake", Text.get(1478), "Gricoller's can"));
                break;
            case "herbs":
                String herb = tier(items, farming, 1, HERBS);
                patch = FARMING.firstReachableHerbPatchName(data.farming());
                if (herb == null || patch == null) return null;
                v.put("herbSeed", "one " + herb.toLowerCase(Locale.ROOT));
                v.put("patch", patch);
                v.put("observed", observed(items, "Seed dibber", "Spade",
                        Text.get(1478), "Magic secateurs", "Seed box"));
                break;
            case "contracts":
                v.put("contract", farming >= 85 ? "hard"
                        : farming >= 65 ? "medium" : "easy");
                break;
            case "rumours":
                int hunter = data.account().getSkillLevel(Skill.HUNTER);
                boolean master = hunter >= 91 && data.quests() != null
                        && data.quests().statusOf("At First Light")
                        == QuestStatus.COMPLETE;
                v.put("rumourTier", master ? "Master" : hunter >= 72 ? "Expert"
                        : hunter >= 57 ? "Adept" : "Novice");
                v.put("hunter", master ? "Guild Hunter Wolf" : hunter >= 72
                        ? "Guild Hunter Teco" : hunter >= 57
                        ? Text.get(1482) : "Huntmaster Gilman");
                break;
            case "forestry":
                int level = data.account().getSkillLevel(Skill.WOODCUTTING);
                v.put("tree", level >= 60 ? "yew trees" : level >= 45
                        ? "maple trees" : level >= 30 ? "willow trees" : "oak trees");
                v.put("treeLocation", Text.get(level >= 60 ? 1094
                        : level >= 45 ? 1095 : level >= 30 ? 1096 : 1097));
                v.put("axe", tool(items, false));
                break;
            default: break;
        }
        return v;
    }

    private static final String[][] ALLOTMENTS = {
            {"Snape grass seed", "61"}, {"Watermelon seed", "47"},
            {"Strawberry seed", "31"}, {"Sweetcorn seed", "20"},
            {"Tomato seed", "12"}, {"Cabbage seed", "7"},
            {"Onion seed", "5"}, {"Potato seed", "1"}};
    private static final String[][] HERBS = {
            {"Torstol seed", "85"}, {"Dwarf weed seed", "79"},
            {"Lantadyme seed", "73"}, {"Cadantine seed", "67"},
            {"Snapdragon seed", "62"}, {"Kwuarm seed", "56"},
            {"Avantoe seed", "50"}, {"Irit seed", "44"},
            {"Toadflax seed", "38"}, {"Ranarr seed", "32"},
            {"Harralander seed", "26"}, {"Tarromin seed", "19"},
            {"Marrentill seed", "14"}, {"Guam seed", "9"}};

    private static String tier(ItemIndex items, int level, int quantity,
            String[][] tiers)
    {
        for (String[] tier : tiers)
            if (level >= Integer.parseInt(tier[1])
                    && items.quantity(tier[0]) >= quantity) return tier[0];
        return null;
    }

    private static String alloy(ItemIndex items)
    {
        String[] metals = {"Runite bar", "Adamantite bar", "Mithril bar",
                "Steel bar", "Iron bar"};
        for (int i = 0; i < metals.length - 1; i++)
            if (items.quantity(metals[i]) >= 14
                    && items.quantity(metals[i + 1]) >= 14)
                return "14 " + metals[i].toLowerCase(Locale.ROOT) + " and 14 "
                        + metals[i + 1].toLowerCase(Locale.ROOT);
        for (String metal : metals)
            if (items.quantity(metal) >= 28)
                return "28 " + metal.toLowerCase(Locale.ROOT);
        return null;
    }

    private static String[] contract(int level, ItemIndex items)
    {
        String[][] tiers = {{"Expert", "Mahogany plank", "70"},
                {"Adept", "Teak plank", "50"}, {"Novice", "Oak plank", "20"},
                {"Beginner", "Plank", "1"}};
        for (String[] tier : tiers)
            if (level >= Integer.parseInt(tier[2])
                    && items.quantity(tier[1]) >= 15) return tier;
        return null;
    }

    private static String tool(ItemIndex items, boolean pickaxe)
    {
        String suffix = pickaxe ? "pickaxe" : "axe";
        for (String metal : new String[]{"Crystal", "Infernal", "Dragon", "Rune",
                "Adamant", "Mithril", "Black", "Steel", "Iron", "Bronze"})
            if (items.has(metal + " " + suffix)) return metal + " " + suffix;
        return Text.get(pickaxe ? 1100 : 1102);
    }

    private static String first(ItemIndex items, String... names)
    {
        for (String name : names) if (items.has(name)) return name;
        return null;
    }

    private static String observed(ItemIndex items, String... names)
    {
        if (names == null) return "";
        List<String> found = new ArrayList<>();
        for (String name : names)
        {
            int quantity = items.quantity(name);
            if (quantity > 0) found.add(quantity + "x " + name);
        }
        return found.isEmpty() ? "" : "Observed: " + String.join(", ", found) + ".";
    }

    private static String format(long value) { return String.format("%,d", value); }

    private static final class Profile
    {
        private String[] ids, observed;
        private String action, supplies, location, note, tool, kind;

        private boolean matches(String id)
        {
            if (ids != null) for (String value : ids) if (value.equals(id)) return true;
            return false;
        }

        private String render(String template, Map<String, String> values)
        {
            String result = template == null ? "" : template;
            for (Map.Entry<String, String> value : values.entrySet())
                result = result.replace("{" + value.getKey() + "}", value.getValue());
            return result.trim();
        }
    }
}
