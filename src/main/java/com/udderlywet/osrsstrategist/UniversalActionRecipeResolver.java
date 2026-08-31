package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Conservative recipes for deterministic RuneLite calculator actions. */
@Singleton
public class UniversalActionRecipeResolver
{
    private static final Recipe[] EXACT = BundledCatalogLoader.array(
            "/content/catalogs/action-recipes.json", Recipe[].class);

    public UniversalActionRecipe resolve(ActionDef action, int count,
            MembershipStatus membership)
    {
        if (action == null || action.getSkill() == null || count <= 0)
            return unknown(get(1259));
        String name = action.getName() == null ? "" : action.getName().trim();
        String lower = name.toLowerCase(Locale.ROOT);
        Recipe exact = exact(action.getSkill(), lower);
        if (exact != null) return exact.build(count);
        switch (action.getSkill())
        {
            case AGILITY: return none(get(899));
            case MINING: return none(get(910));
            case FISHING: return none(get(921));
            case WOODCUTTING: return none(get(932));
            case THIEVING: return none(get(935));
            case HUNTER: return none(get(936));
            case COOKING: return cooking(name, lower, count);
            case FIREMAKING: return lower.contains("log")
                    ? recipe(get(937), count, name, 1)
                    : unknown(get(938));
            case PRAYER: return contains(lower, "bones", "ashes", "head")
                    ? recipe(get(939), count, name, 1)
                    : unknown(get(900));
            case RUNECRAFT: return recipe(get(901), count,
                    membership == MembershipStatus.P2P
                            ? "Pure essence" : "Rune essence", 1);
            case CRAFTING: return crafting(name, lower, count);
            case FLETCHING: return fletching(name, lower, count);
            case SMITHING: return smithing(lower, count);
            case FARMING: return lower.endsWith(" tree")
                    ? recipe(get(930), count,
                    name.substring(0, name.length() - 5).trim() + " sapling", 1)
                    : unknown(get(929));
            case MAGIC: return lower.endsWith(" curse")
                    ? recipe(get(933), count, "Earth rune", 3,
                    "Water rune", 2, "Body rune", 1)
                    : unknown(get(934));
            case HERBLORE: return unknown(get(925));
            case CONSTRUCTION: return unknown(get(928));
            default: return unknown(get(902));
        }
    }

    private static Recipe exact(Skill skill, String name)
    {
        for (Recipe recipe : EXACT)
            if (skill.name().equals(recipe.skill)
                    && (recipe.contains ? name.contains(recipe.match)
                    : name.equals(recipe.match))) return recipe;
        return null;
    }

    private static UniversalActionRecipe cooking(String name, String lower, int n)
    {
        if (contains(lower, "jug of wine") || lower.equals("wine"))
            return recipe(get(903), n, "Grapes", 1, "Jug of water", 1);
        if (contains(lower, "cake", "pie", "pizza", "stew", "curry"))
            return unknown(get(904));
        String raw = lower.startsWith("raw ") ? name : "Raw "
                + (lower.startsWith("cooked ") ? name.substring(7) : name)
                .toLowerCase(Locale.ROOT);
        return raw.trim().length() <= 4 ? unknown(get(905))
                : recipe(get(906), n, raw, 1);
    }

    private static UniversalActionRecipe crafting(String name, String lower, int n)
    {
        if (gemName(lower) != null && !jewellery(lower) && !lower.contains("bolt"))
            return recipe("Bring a chisel.", n,
                    lower.startsWith("uncut ") ? name : "Uncut " + lower, 1);
        if (contains(lower, "beer glass", "candle lantern", "oil lamp", "vial",
                "fishbowl", "unpowered orb", "lantern lens")
                && !lower.contains("molten"))
            return recipe(get(909), n, "Molten glass", 1);
        if (lower.contains("bird house"))
            return recipe(get(911), n, wood(name, "Logs"), 1);
        if (contains(lower, "d'hide", "dragonhide"))
        {
            int hides = lower.contains("body") ? 3 : lower.contains("chaps") ? 2
                    : lower.contains("vambrace") ? 1 : 0;
            if (hides > 0) return recipe(get(1264), n,
                    capitalize(first(lower, "green", "blue", "red", "black",
                            "dragon")) + " dragon leather", hides);
        }
        if (contains(lower, "leather gloves", "leather boots", "cowl",
                "leather vambraces", "leather body", "leather chaps", "coif"))
            return recipe(get(1264), n, "Leather", 1);
        if (contains(lower, "hardleather body", "hard leather body"))
            return recipe(get(1264), n, "Hard leather", 1);
        if (jewellery(lower))
        {
            String gem = gemName(lower);
            String bar = gem != null && contains(gem, "opal", "jade", "red topaz")
                    ? "Silver bar" : "Gold bar";
            return gem == null ? recipe(get(1265), n, bar, 1)
                    : recipe(get(1265), n, bar, 1, capitalize(gem), 1);
        }
        if (contains(lower, "battlestaff", "battlestave"))
        {
            String element = first(lower, "air", "water", "earth", "fire");
            if (element != null) return recipe(get(912), n, "Battlestaff", 1,
                    capitalize(element) + " orb", 1);
        }
        return unknown(get(913));
    }

    private static UniversalActionRecipe fletching(String name, String lower, int n)
    {
        if (lower.equals("arrow shaft") || lower.equals("arrow shafts"))
            return recipe(get(914), 1, "Logs", ceil(n, 15));
        if (lower.equals("headless arrow") || lower.equals("headless arrows"))
            return recipe(get(915), n, "Arrow shaft", 1, "Feather", 1);
        if (lower.endsWith("bow (u)"))
            return recipe("Bring a knife.", n, wood(name, "Logs"), 1);
        if (contains(lower, "shortbow", "longbow") && !lower.contains("(u)"))
            return recipe(get(1266), n, name + " (u)", 1, "Bow string", 1);
        if (lower.endsWith(" shield") || lower.endsWith(" stock"))
            return recipe("Bring a knife.", n, wood(name, null),
                    lower.endsWith(" shield") ? 2 : 1);
        String metal = firstWord(name);
        if (lower.endsWith(" dart") || lower.endsWith(" darts"))
            return recipe(get(916), n, metal + " dart tip", 1, "Feather", 1);
        if (lower.contains("broad arrow")) return recipe(get(917), n,
                "Headless arrow", 1, "Broad arrowhead", 1);
        if (projectile(lower, "arrow")) return recipe(get(918), n,
                "Headless arrow", 1, metal + " arrowhead", 1);
        if (projectile(lower, "javelin")) return recipe(get(919), n,
                "Javelin shaft", 1, metal + " javelin head", 1);
        if (basicBolt(lower)) return recipe(get(920), n,
                metal + " bolts (unf)", 1, "Feather", 1);
        return unknown(get(922));
    }

    private static UniversalActionRecipe smithing(String lower, int n)
    {
        int bars = smithingBarsFor(lower);
        String metal = first(lower, "bronze", "iron", "steel", "mithril",
                "adamant", "rune");
        if (bars <= 0 || metal == null) return unknown(get(924));
        String bar = metal.equals("adamant") ? "Adamantite bar"
                : metal.equals("rune") ? "Runite bar" : capitalize(metal) + " bar";
        return recipe(get(923), n, bar, bars);
    }

    static int smithingBarsFor(String value)
    {
        String lower = value == null ? "" : value;
        if (lower.contains("platebody")) return 5;
        if (contains(lower, "plateskirt", "platelegs", "2h sword", "kiteshield",
                "chainbody", "battleaxe", "warhammer")) return 3;
        if (contains(lower, "claws", "full helm", "sq shield", "longsword",
                "scimitar")) return 2;
        return contains(lower, "mace", "sword", "dagger", " axe", "med helm",
                "dart tip", "knife", "arrowtip", "nails", "wire",
                "unfinished bolt") ? 1 : 0;
    }

    private static UniversalActionRecipe recipe(String setup, int count,
            Object... items)
    {
        List<MethodInput> inputs = new ArrayList<>();
        for (int i = 0; i + 1 < items.length; i += 2)
            if (items[i] != null && (Integer) items[i + 1] > 0)
                inputs.add(new MethodInput((String) items[i], -1,
                        multiply(count, (Integer) items[i + 1])));
        return new UniversalActionRecipe(inputs, setup, true);
    }

    private static UniversalActionRecipe none(String setup)
    {
        return UniversalActionRecipe.noConsumedInputs(setup);
    }
    private static UniversalActionRecipe unknown(String setup)
    {
        return UniversalActionRecipe.unknown(setup);
    }
    private static boolean contains(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return true;
        return false;
    }
    private static String first(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return part;
        return null;
    }
    private static String gemName(String lower)
    {
        return first(lower, "red topaz", "opal", "jade", "sapphire", "emerald",
                "ruby", "diamond", "dragonstone", "onyx", "zenyte");
    }
    private static boolean jewellery(String lower)
    {
        return lower.endsWith(" ring") || lower.endsWith(" bracelet")
                || lower.endsWith(" necklace") || lower.contains("amulet");
    }
    private static String wood(String name, String fallback)
    {
        String value = first(name.toLowerCase(Locale.ROOT), "redwood", "magic",
                "yew", "mahogany", "maple", "teak", "willow", "oak");
        return value == null ? fallback : capitalize(value) + " logs";
    }
    private static boolean projectile(String lower, String kind)
    {
        return (lower.endsWith(" " + kind) || lower.endsWith(" " + kind + "s"))
                && contains(lower, "bronze", "iron", "steel", "mithril",
                "adamant", "rune", "amethyst", "dragon");
    }
    private static boolean basicBolt(String lower)
    {
        return (lower.endsWith(" bolt") || lower.endsWith(" bolts"))
                && contains(lower, "bronze", "blurite", "iron", "silver",
                "steel", "mithril", "adamant", "runite", "rune", "dragon")
                && !contains(lower, "opal", "pearl", "barbed", "kebbit",
                "sapphire", "emerald", "ruby", "diamond", "dragonstone",
                "onyx", "amethyst", "broad");
    }
    private static int multiply(int a, int b)
    {
        return a > Integer.MAX_VALUE / b ? Integer.MAX_VALUE : a * b;
    }
    private static int ceil(int value, int divisor)
    {
        return (value + divisor - 1) / divisor;
    }
    private static String capitalize(String value)
    {
        return value == null || value.isEmpty() ? "Dragon"
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    private static String firstWord(String value)
    {
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }

    private static final class Recipe
    {
        private String skill, match, setup;
        private String[] inputs;
        private int[] units;
        private boolean contains;

        private UniversalActionRecipe build(int count)
        {
            List<MethodInput> result = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++)
                result.add(new MethodInput(inputs[i], -1,
                        multiply(count, units[i])));
            return new UniversalActionRecipe(result, setup, true);
        }
    }
}
