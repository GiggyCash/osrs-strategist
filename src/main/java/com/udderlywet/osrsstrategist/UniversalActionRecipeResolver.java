package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Conservative material recipes for deterministic RuneLite calculator actions. */
@Singleton
public class UniversalActionRecipeResolver
{
    private static final Map<String, String[]> POTIONS = rows(new String[][]{
            {"attack potion", "Guam leaf", "Eye of newt"},
            {"antipoison", "Marrentill", "Unicorn horn dust"},
            {"strength potion", "Tarromin", "Limpwurt root"},
            {"restore potion", "Harralander", "Red spiders' eggs"},
            {"energy potion", "Harralander", "Chocolate dust"},
            {"defence potion", "Ranarr weed", "White berries"},
            {"agility potion", "Toadflax", "Toad's legs"},
            {"combat potion", "Harralander", "Goat horn dust"},
            {"prayer potion", "Ranarr weed", "Snape grass"},
            {"super attack", "Irit leaf", "Eye of newt"},
            {"superantipoison", "Irit leaf", "Unicorn horn dust"},
            {"fishing potion", "Avantoe", "Snape grass"},
            {"super energy", "Avantoe", "Mort myre fungus"},
            {"hunter potion", "Avantoe", "Kebbit teeth dust"},
            {"super strength", "Kwuarm", "Limpwurt root"},
            {"weapon poison", "Kwuarm", "Dragon scale dust"},
            {"super restore", "Snapdragon", "Red spiders' eggs"},
            {"super defence", "Cadantine", "White berries"},
            {"antifire", "Lantadyme", "Dragon scale dust"},
            {"ranging potion", "Dwarf weed", "Wine of zamorak"},
            {"magic potion", "Lantadyme", "Potato cactus"},
            {"zamorak brew", "Torstol", "Jangerberries"},
            {"saradomin brew", "Toadflax", "Crushed nest"}
    });

    public UniversalActionRecipe resolve(RuneLiteSkillActionDefinition action,
            int actions, MembershipStatus membership)
    {
        if (action == null || action.getSkill() == null || actions <= 0)
            return unknown("Action recipe unavailable.");
        String name = clean(action.getName());
        String lower = name.toLowerCase(Locale.ROOT);
        switch (action.getSkill())
        {
            case AGILITY: return none("No consumed material is required for the modeled Agility action.");
            case MINING: return none("Bring the best legal pickaxe. The modeled Mining action consumes no material.");
            case FISHING: return none("Bring the tool and bait required by the selected spot. Bait is counted only by a dedicated exact route.");
            case WOODCUTTING: return none("Bring the best legal axe. The modeled Woodcutting action consumes no material.");
            case THIEVING: return none("No material is consumed by a successful action. Bring healing when failure damage is possible.");
            case HUNTER: return none("Bring the selected creature's reusable traps or tools.");
            case COOKING: return cooking(name, lower, actions);
            case FIREMAKING: return lower.contains("log")
                    ? recipe("Bring a tinderbox or another verified ignition source.", actions, name, 1)
                    : unknown("This is not a conventional one-log burn and needs route-specific setup.");
            case PRAYER: return contains(lower, "bones", "ashes", "head")
                    ? recipe("One Prayer item is consumed per calculator action; route multipliers are separate.", actions, name, 1)
                    : unknown("The consumed Prayer offering cannot be inferred safely.");
            case RUNECRAFT: return recipe("Rune multipliers affect output, not base XP per essence.", actions,
                    membership == MembershipStatus.P2P ? "Pure essence" : "Rune essence", 1);
            case CRAFTING: return crafting(name, lower, actions);
            case FLETCHING: return fletching(name, lower, actions);
            case SMITHING: return smithing(lower, actions);
            case HERBLORE: return herblore(lower, actions);
            case CONSTRUCTION: return construction(lower, actions);
            case FARMING: return farming(name, lower, actions);
            case MAGIC: return magic(lower, actions);
            default: return unknown("No safe generic material recipe is available for this skill.");
        }
    }

    private static UniversalActionRecipe cooking(String name, String lower, int n)
    {
        if (contains(lower, "jug of wine") || lower.equals("wine"))
            return recipe("One grapes and one jug of water are consumed per wine.", n,
                    "Grapes", 1, "Jug of water", 1);
        if (contains(lower, "cake", "pie", "pizza", "stew", "curry"))
            return unknown("Composite Cooking recipes require a route-specific ingredient model.");
        String raw = lower.startsWith("raw ") ? name
                : "Raw " + (lower.startsWith("cooked ") ? name.substring(7) : name).toLowerCase(Locale.ROOT);
        return raw.trim().length() <= 4 ? unknown("Raw food could not be inferred safely.")
                : recipe("Successful-cook input count; add a route-specific burn buffer where needed.", n, raw, 1);
    }

    private static UniversalActionRecipe crafting(String name, String lower, int n)
    {
        switch (lower)
        {
            case "ball of wool": return recipe("Use a spinning wheel.", n, "Wool", 1);
            case "bow string": return recipe("Use a spinning wheel.", n, "Flax", 1);
            case "unfired pot": case "unfired pie dish": case "unfired bowl":
            case "unfired plant pot": case "unfired pot lid":
                return recipe("Use the clay on a pottery wheel.", n, "Soft clay", 1);
            case "pot": return recipe("Use a pottery oven.", n, "Unfired pot", 1);
            case "pie dish": return recipe("Use a pottery oven.", n, "Unfired pie dish", 1);
            case "bowl": return recipe("Use a pottery oven.", n, "Unfired bowl", 1);
            case "empty plant pot": return recipe("Use a pottery oven.", n, "Unfired plant pot", 1);
            case "pot lid": return recipe("Use a pottery oven.", n, "Unfired pot lid", 1);
            case "molten glass": return recipe("Conventional furnace glassmaking; Superglass Make has different yield math.", n,
                    "Bucket of sand", 1, "Soda ash", 1);
            case "tiara": return recipe("Bring a tiara mould to the Edgeville furnace, smelt each silver bar, bank, and repeat.", n,
                    "Silver bar", 1);
            case "gold tiara": return recipe("Bring a tiara mould to a furnace.", n, "Gold bar", 1);
            default: break;
        }
        if (isGem(lower) && !isJewellery(lower) && !lower.contains("bolt"))
            return recipe("Bring a chisel.", n, lower.startsWith("uncut ") ? name : "Uncut " + lower, 1);
        if (contains(lower, "beer glass", "candle lantern", "oil lamp", "vial",
                "fishbowl", "unpowered orb", "lantern lens") && !lower.contains("molten"))
            return recipe("Bring a glassblowing pipe for blown-glass products.", n, "Molten glass", 1);
        if (lower.contains("bird house"))
            return recipe("Clockworks are reusable, so only the log is counted.", n, woodItem(name, "Logs"), 1);
        if (contains(lower, "d'hide", "dragonhide"))
        {
            int hides = lower.contains("body") ? 3 : lower.contains("chaps") ? 2
                    : lower.contains("vambrace") ? 1 : 0;
            if (hides > 0) return recipe("Bring a needle and thread.", n,
                    color(lower) + " dragon leather", hides);
        }
        if (contains(lower, "leather gloves", "leather boots", "cowl",
                "leather vambraces", "leather body", "leather chaps", "coif"))
            return recipe("Bring a needle and thread.", n, "Leather", 1);
        if (contains(lower, "hardleather body", "hard leather body"))
            return recipe("Bring a needle and thread.", n, "Hard leather", 1);
        if (isJewellery(lower))
        {
            String gem = gem(lower);
            String bar = gem != null && contains(gem.toLowerCase(Locale.ROOT), "opal", "jade", "red topaz")
                    ? "Silver bar" : "Gold bar";
            return gem == null ? recipe("Bring the matching mould.", n, bar, 1)
                    : recipe("Bring the matching mould.", n, bar, 1, gem, 1);
        }
        if (contains(lower, "battlestaff", "battlestave"))
        {
            String element = firstMatch(lower, "air", "water", "earth", "fire");
            if (element != null) return recipe("Attach the charged orb to the battlestaff.", n,
                    "Battlestaff", 1, capitalize(element) + " orb", 1);
        }
        return unknown("No proven consumed-material recipe exists for this Crafting action.");
    }

    private static UniversalActionRecipe fletching(String name, String lower, int n)
    {
        if (lower.equals("arrow shaft") || lower.equals("arrow shafts"))
            return recipe("The basic recipe produces 15 shafts per log.", 1, "Logs", ceil(n, 15));
        if (lower.equals("headless arrow") || lower.equals("headless arrows"))
            return recipe("RuneLite XP is per arrow although the interface batches them.", n,
                    "Arrow shaft", 1, "Feather", 1);
        if (lower.endsWith("bow (u)")) return recipe("Bring a knife.", n, woodItem(name, "Logs"), 1);
        if (contains(lower, "shortbow", "longbow") && !lower.contains("(u)"))
            return recipe("String the unstrung bow.", n, name + " (u)", 1, "Bow string", 1);
        if (lower.endsWith(" shield")) return recipe("Bring a knife.", n, woodItem(name, null), 2);
        if (lower.endsWith(" stock")) return recipe("Bring a knife.", n, woodItem(name, null), 1);
        String metal = firstWord(name);
        if (lower.endsWith(" dart") || lower.endsWith(" darts"))
            return recipe("One tip and feather are consumed per dart.", n, metal + " dart tip", 1, "Feather", 1);
        if (lower.contains("broad arrow"))
            return recipe("One headless arrow and broad arrowhead are consumed.", n,
                    "Headless arrow", 1, "Broad arrowhead", 1);
        if (isMetalProjectile(lower, "arrow"))
            return recipe("One headless arrow and matching arrowhead are consumed.", n,
                    "Headless arrow", 1, metal + " arrowhead", 1);
        if (isMetalProjectile(lower, "javelin"))
            return recipe("One shaft and matching head are consumed.", n,
                    "Javelin shaft", 1, metal + " javelin head", 1);
        if (isBasicBolt(lower))
            return recipe("One unfinished bolt and feather are consumed.", n,
                    metal + " bolts (unf)", 1, "Feather", 1);
        return unknown("This Fletching action's components cannot be inferred safely.");
    }

    private static UniversalActionRecipe smithing(String lower, int n)
    {
        Map<String, Object[]> bars = new HashMap<>();
        bars.put("bronze bar", new Object[]{"Copper ore", 1, "Tin ore", 1});
        bars.put("iron bar", new Object[]{"Iron ore", 1});
        bars.put("silver bar", new Object[]{"Silver ore", 1});
        bars.put("gold bar", new Object[]{"Gold ore", 1});
        bars.put("steel bar", new Object[]{"Iron ore", 1, "Coal", 2});
        bars.put("mithril bar", new Object[]{"Mithril ore", 1, "Coal", 4});
        bars.put("adamantite bar", new Object[]{"Adamantite ore", 1, "Coal", 6});
        bars.put("runite bar", new Object[]{"Runite ore", 1, "Coal", 8});
        if (bars.containsKey(lower)) return recipe("Standard furnace recipe.", n, bars.get(lower));
        int count = smithingBarsFor(lower);
        String metal = firstMatch(lower, "bronze", "iron", "steel", "mithril", "adamant", "rune");
        if (count > 0 && metal != null)
            return recipe("Bring a hammer; this is the standard anvil recipe.", n,
                    metal.equals("adamant") ? "Adamantite bar"
                            : metal.equals("rune") ? "Runite bar" : capitalize(metal) + " bar", count);
        return unknown("This Smithing action needs a dedicated furnace or anvil recipe.");
    }

    static int smithingBarsFor(String lower)
    {
        if (lower == null) return 0;
        if (lower.contains("platebody")) return 5;
        if (contains(lower, "plateskirt", "platelegs", "2h sword", "kiteshield",
                "chainbody", "battleaxe", "warhammer")) return 3;
        if (contains(lower, "claws", "full helm", "sq shield", "longsword", "scimitar")) return 2;
        return contains(lower, "mace", "sword", "dagger", " axe", "med helm",
                "dart tip", "knife", "arrowtip", "nails", "wire", "unfinished bolt") ? 1 : 0;
    }

    private static UniversalActionRecipe herblore(String lower, int n)
    {
        String[] ingredients = null;
        for (Map.Entry<String, String[]> row : POTIONS.entrySet())
            if (lower.contains(row.getKey()) && (!isBasicPotion(row.getKey()) || !lower.contains("super")))
            { ingredients = row.getValue(); break; }
        return ingredients == null ? unknown("This potion is not in the verified fallback table.")
                : recipe("Make the complete potion from herb, secondary, and vial.", n,
                        ingredients[0], 1, ingredients[1], 1, "Vial of water", 1);
    }

    private static UniversalActionRecipe construction(String lower, int n)
    {
        if (lower.contains("oak larder")) return recipe("Bring a hammer and saw.", n, "Oak plank", 8);
        if (contains(lower, "oak dungeon door", "oak door")) return recipe("Bring a hammer and saw.", n, "Oak plank", 10);
        if (lower.contains("mahogany table")) return recipe("Bring a hammer and saw.", n, "Mahogany plank", 6);
        if (lower.contains("crude wooden chair")) return recipe("Bring a hammer and saw.", n,
                "Plank", 2, "Steel nails", 2);
        if (lower.contains("teak garden bench")) return recipe("Bring a hammer and saw.", n, "Teak plank", 6);
        if (lower.contains("mythical cape")) return recipe("The cape is returned; only planks are counted.", n, "Teak plank", 3);
        return unknown("Furniture recipes vary; this hotspot is not mapped.");
    }

    private static UniversalActionRecipe farming(String name, String lower, int n)
    {
        if (!lower.endsWith(" tree")) return unknown("Live Farming run logic must model this action family.");
        return recipe("One tree-cycle action per sapling; protection and patch access remain route decisions.", n,
                name.substring(0, name.length() - 5).trim() + " sapling", 1);
    }

    private static UniversalActionRecipe magic(String lower, int n)
    {
        if (lower.equals("wind strike")) return recipe("Full base rune cost.", n, "Air rune", 1, "Mind rune", 1);
        if (lower.equals("fire bolt")) return recipe("Full base rune cost.", n,
                "Air rune", 3, "Fire rune", 4, "Chaos rune", 1);
        if (lower.equals("fire blast")) return recipe("Full base rune cost.", n,
                "Air rune", 4, "Fire rune", 5, "Death rune", 1);
        if (contains(lower, "high level alchemy", "high alchemy"))
            return recipe("An equipped fire source can replace fire runes; use only a verified safe alch list.", n,
                    "Nature rune", 1, "Fire rune", 5);
        if (lower.equals("curse") || lower.endsWith(" curse"))
            return recipe("Elemental staves can replace corresponding runes.", n,
                    "Earth rune", 3, "Water rune", 2, "Body rune", 1);
        return unknown("The spell and equipped staff do not prove a rune loadout.");
    }

    private static UniversalActionRecipe recipe(String setup, int actions, Object... itemAndUnits)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        for (int i = 0; i + 1 < itemAndUnits.length; i += 2)
        {
            String item = (String) itemAndUnits[i];
            int units = (Integer) itemAndUnits[i + 1];
            if (item != null && units > 0)
                inputs.add(new ResolvedMethodInput(item, -1, multiply(actions, units)));
        }
        return new UniversalActionRecipe(inputs, setup, true);
    }

    private static UniversalActionRecipe none(String setup) { return UniversalActionRecipe.noConsumedInputs(setup); }
    private static UniversalActionRecipe unknown(String setup) { return UniversalActionRecipe.unknown(setup); }
    private static boolean contains(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return true;
        return false;
    }
    private static String firstMatch(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return part;
        return null;
    }
    private static int multiply(int a, int b) { return a > Integer.MAX_VALUE / b ? Integer.MAX_VALUE : a * b; }
    private static int ceil(int value, int divisor) { return (value + divisor - 1) / divisor; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String capitalize(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static String firstWord(String value)
    {
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }
    private static String color(String lower)
    {
        String value = firstMatch(lower, "green", "blue", "red", "black");
        return value == null ? "Dragon" : capitalize(value);
    }
    private static boolean isGem(String lower)
    {
        return contains(lower, "opal", "jade", "red topaz", "sapphire", "emerald",
                "ruby", "diamond", "dragonstone", "onyx", "zenyte");
    }
    private static boolean isJewellery(String lower)
    {
        return lower.endsWith(" ring") || lower.endsWith(" bracelet")
                || lower.endsWith(" necklace") || lower.contains("amulet");
    }
    private static String gem(String lower)
    {
        String value = firstMatch(lower, "red topaz", "opal", "jade", "sapphire",
                "emerald", "ruby", "diamond", "dragonstone", "onyx", "zenyte");
        return value == null ? null : capitalize(value);
    }
    private static String woodItem(String name, String fallback)
    {
        String wood = firstMatch(name.toLowerCase(Locale.ROOT), "redwood", "magic", "yew",
                "mahogany", "maple", "teak", "willow", "oak");
        return wood == null ? fallback : capitalize(wood) + " logs";
    }
    private static boolean isMetalProjectile(String lower, String kind)
    {
        return (lower.endsWith(" " + kind) || lower.endsWith(" " + kind + "s"))
                && contains(lower, "bronze", "iron", "steel", "mithril", "adamant",
                        "rune", "amethyst", "dragon");
    }
    private static boolean isBasicBolt(String lower)
    {
        return (lower.endsWith(" bolt") || lower.endsWith(" bolts"))
                && contains(lower, "bronze", "blurite", "iron", "silver", "steel",
                        "mithril", "adamant", "runite", "rune", "dragon")
                && !contains(lower, "opal", "pearl", "barbed", "kebbit", "sapphire",
                        "emerald", "ruby", "diamond", "dragonstone", "onyx", "amethyst", "broad");
    }
    private static boolean isBasicPotion(String key)
    {
        return contains(key, "attack potion", "antipoison", "strength potion",
                "restore potion", "energy potion", "defence potion", "combat potion");
    }
    private static Map<String, String[]> rows(String[][] rows)
    {
        Map<String, String[]> result = new LinkedHashMap<>();
        for (String[] row : rows) result.put(row[0], new String[]{row[1], row[2]});
        return result;
    }
}
