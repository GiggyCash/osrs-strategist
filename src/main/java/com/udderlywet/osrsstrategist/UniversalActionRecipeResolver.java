package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Conservative consumed-input resolver for RuneLite skill-calculator actions.
 * Exact is only returned when the material recipe is stable and unambiguous.
 */
@Singleton
public class UniversalActionRecipeResolver
{
    public UniversalActionRecipe resolve(
            RuneLiteSkillActionDefinition action,
            int actions,
            MembershipStatus membership)
    {
        if (action == null || action.getSkill() == null || actions <= 0)
        {
            return UniversalActionRecipe.unknown("Action recipe unavailable.");
        }

        String name = clean(action.getName());
        String lower = name.toLowerCase(Locale.ROOT);
        switch (action.getSkill())
        {
            case AGILITY:
                return noInput("No consumed material is required for the modeled Agility action.");
            case MINING:
                return noInput("Bring the best legal pickaxe. The modeled Mining action consumes no material.");
            case FISHING:
                return noInput("Bring the tool and bait required by the selected spot. Bait is only counted when a dedicated route models it exactly.");
            case WOODCUTTING:
                return noInput("Bring the best legal axe. The modeled Woodcutting action consumes no material.");
            case THIEVING:
                return noInput("No material is consumed by the successful action. Bring food or healing when failure damage is possible.");
            case HUNTER:
                return noInput("Bring the traps or tools for the selected creature. Reusable traps are not counted as consumed inputs.");
            case COOKING:
                return cooking(name, lower, actions);
            case FIREMAKING:
                return one(name, actions, "Bring a tinderbox or another verified ignition source.");
            case PRAYER:
                return one(name, actions, "This models the consumed Prayer item before altar or other XP multipliers.");
            case RUNECRAFT:
                return runecraft(actions, membership);
            case CRAFTING:
                return crafting(name, lower, actions);
            case FLETCHING:
                return fletching(name, lower, actions);
            case SMITHING:
                return smithing(name, lower, actions);
            case HERBLORE:
                return herblore(lower, actions);
            case CONSTRUCTION:
                return construction(lower, actions);
            case FARMING:
                return farming(name, lower, actions);
            case MAGIC:
                return magic(lower, actions);
            default:
                return UniversalActionRecipe.unknown(
                        "Strategist knows the action XP but does not have a safe generic material recipe for this skill.");
        }
    }

    private static UniversalActionRecipe cooking(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("jug of wine") || lower.equals("wine"))
        {
            add(inputs, "Grapes", actions);
            add(inputs, "Jug of water", actions);
            return exact(inputs, "One grapes and one jug of water are consumed per completed wine action.");
        }
        if (containsAny(lower, "cake", "pie", "pizza", "stew", "curry"))
        {
            return UniversalActionRecipe.unknown(
                    "Composite Cooking recipes need a recipe-specific ingredient model before Strategist gives a shopping list.");
        }
        String raw = rawFood(name);
        if (raw == null) return UniversalActionRecipe.unknown("Raw food could not be inferred safely.");
        add(inputs, raw, actions);
        return exact(inputs,
                "This is the successful-cook input count. A burn-aware route must add a level and location specific raw-food buffer when burns are possible.");
    }

    private static UniversalActionRecipe runecraft(int actions, MembershipStatus membership)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        add(inputs, membership == MembershipStatus.F2P ? "Rune essence" : "Pure essence", actions);
        return exact(inputs,
                "Rune multipliers affect output, not base XP per essence. Pouches and outfit effects are separate route modifiers.");
    }

    private static UniversalActionRecipe crafting(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (isCutGem(lower))
        {
            add(inputs, lower.startsWith("uncut ") ? name : "Uncut " + lower, actions);
            return exact(inputs, "Bring a chisel.");
        }
        if (isGlassProduct(lower))
        {
            add(inputs, "Molten glass", actions);
            return exact(inputs, "Bring a glassblowing pipe for blown-glass products.");
        }
        if (isDragonhideProduct(lower))
        {
            int count = armourMaterialCount(lower);
            if (count > 0)
            {
                add(inputs, dragonLeatherName(lower), actions * count);
                return exact(inputs,
                        "Needle and thread are setup supplies. Thread is not modeled as one full item consumed per craft.");
            }
        }
        if (isPlainLeatherProduct(lower))
        {
            int count = armourMaterialCount(lower);
            if (count > 0)
            {
                add(inputs, "Leather", actions * count);
                return exact(inputs, "Bring a needle and thread.");
            }
        }
        if (isJewellery(lower))
        {
            add(inputs, "Gold bar", actions);
            String gem = jewelleryGem(lower);
            if (gem != null) add(inputs, gem, actions);
            return exact(inputs, "Bring the mould for the selected jewellery piece.");
        }
        if (lower.contains("battlestaff") || lower.contains("battlestave"))
        {
            String orb = elementalOrb(lower);
            if (orb != null)
            {
                add(inputs, "Battlestaff", actions);
                add(inputs, orb, actions);
                return exact(inputs, "This models attaching a charged elemental orb to a battlestaff.");
            }
        }
        return UniversalActionRecipe.unknown(
                "This Crafting action has fixed XP, but the generic resolver does not have a proven consumed-material recipe for it.");
    }

    private static UniversalActionRecipe fletching(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("arrow shaft"))
        {
            add(inputs, "Logs", (int) Math.ceil(actions / 15.0));
            return exact(inputs, "One normal log makes 15 arrow shafts in the basic route.");
        }
        if (lower.endsWith(" shortbow (u)") || lower.endsWith(" longbow (u)"))
        {
            add(inputs, logForBow(name), actions);
            return exact(inputs, "Bring a knife.");
        }
        if ((lower.contains("shortbow") || lower.contains("longbow")) && !lower.contains("(u)"))
        {
            add(inputs, name + " (u)", actions);
            add(inputs, "Bow string", actions);
            return exact(inputs, "This models stringing an unstrung bow.");
        }
        if (lower.endsWith(" dart") || lower.endsWith(" darts"))
        {
            String metal = firstWord(name);
            add(inputs, metal + " dart tip", actions);
            add(inputs, "Feather", actions);
            return exact(inputs, "One dart tip and one feather are consumed per dart.");
        }
        if (lower.contains("broad arrow"))
        {
            add(inputs, "Headless arrow", actions);
            add(inputs, "Broad arrowhead", actions);
            return exact(inputs, "One headless arrow and one broad arrowhead are consumed per finished arrow.");
        }
        if (lower.endsWith(" arrows") || lower.endsWith(" arrow"))
        {
            add(inputs, "Headless arrow", actions);
            add(inputs, firstWord(name) + " arrowhead", actions);
            return exact(inputs, "One headless arrow and one matching arrowhead are consumed per finished arrow.");
        }
        if (lower.endsWith(" bolts") || lower.endsWith(" bolt"))
        {
            add(inputs, name + " (unf)", actions);
            add(inputs, "Feather", actions);
            return exact(inputs, "This models the basic feathering step. Gem tipping and enchanting are separate actions.");
        }
        return UniversalActionRecipe.unknown(
                "This Fletching action has fixed XP, but its components are not safely inferred by the generic resolver.");
    }

    private static UniversalActionRecipe smithing(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        switch (lower)
        {
            case "bronze bar":
                add(inputs, "Copper ore", actions);
                add(inputs, "Tin ore", actions);
                return exact(inputs, "Standard furnace recipe.");
            case "iron bar":
                add(inputs, "Iron ore", actions);
                return exact(inputs, "Standard furnace input. Iron smelting failure must be handled by the selected route when applicable.");
            case "silver bar":
                add(inputs, "Silver ore", actions);
                return exact(inputs, "Standard furnace recipe.");
            case "gold bar":
                add(inputs, "Gold ore", actions);
                return exact(inputs, "Standard furnace recipe. Goldsmith gauntlet XP belongs in the route XP modifier.");
            case "steel bar":
                add(inputs, "Iron ore", actions);
                add(inputs, "Coal", actions * 2);
                return exact(inputs, "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "mithril bar":
                add(inputs, "Mithril ore", actions);
                add(inputs, "Coal", actions * 4);
                return exact(inputs, "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "adamantite bar":
                add(inputs, "Adamantite ore", actions);
                add(inputs, "Coal", actions * 6);
                return exact(inputs, "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "runite bar":
                add(inputs, "Runite ore", actions);
                add(inputs, "Coal", actions * 8);
                return exact(inputs, "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            default:
                break;
        }

        String bar = metalBarFor(lower);
        int bars = smithingBarsFor(lower);
        if (bar != null && bars > 0)
        {
            add(inputs, bar, actions * bars);
            return exact(inputs,
                    "Bring a hammer. The bar count follows the standard anvil recipe for this item family.");
        }
        return UniversalActionRecipe.unknown(
                "This Smithing action needs a dedicated furnace or anvil recipe before materials are exact.");
    }

    private static UniversalActionRecipe herblore(String lower, int actions)
    {
        PotionRecipe recipe = potionRecipe(lower);
        if (recipe == null)
        {
            return UniversalActionRecipe.unknown(
                    "The potion has fixed XP, but its herb and secondary recipe is not yet in the verified fallback table.");
        }
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        add(inputs, recipe.primary, actions);
        add(inputs, recipe.secondary, actions);
        add(inputs, "Vial of water", actions);
        return exact(inputs,
                "The recipe models one complete potion per action. Saving effects and decanting can reduce effective supply use.");
    }

    private static UniversalActionRecipe construction(String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("oak larder"))
        {
            add(inputs, "Oak plank", actions * 8);
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("oak dungeon door"))
        {
            add(inputs, "Oak plank", actions * 10);
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("mahogany table"))
        {
            add(inputs, "Mahogany plank", actions * 6);
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("mythical cape"))
        {
            add(inputs, "Teak plank", actions * 3);
            return exact(inputs, "The cape is reusable; only consumed planks are counted.");
        }
        return UniversalActionRecipe.unknown(
                "Furniture recipes vary. Strategist keeps the exact build count but does not fabricate plank counts for an unmapped hotspot.");
    }

    private static UniversalActionRecipe farming(String name, String lower, int actions)
    {
        if (lower.endsWith(" tree"))
        {
            List<ResolvedMethodInput> inputs = new ArrayList<>();
            String tree = name.substring(0, name.length() - 5).trim();
            add(inputs, tree + " sapling", actions);
            return exact(inputs,
                    "This models one tree-cycle action per sapling. Protection, compost, growth time, and patch availability remain route decisions.");
        }
        return UniversalActionRecipe.unknown(
                "Farming can combine planting, variable harvesting, and check-health XP. Live Farming run logic should model this action family.");
    }

    private static UniversalActionRecipe magic(String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("high level alchemy") || lower.contains("high alchemy"))
        {
            add(inputs, "Nature rune", actions);
            add(inputs, "Fire rune", actions * 5);
            return exact(inputs,
                    "A fire-rune staff can replace the fire runes. The item being alched must come from a verified safe alch list.");
        }
        if (lower.equals("curse") || lower.endsWith(" curse"))
        {
            add(inputs, "Earth rune", actions * 3);
            add(inputs, "Water rune", actions * 2);
            add(inputs, "Body rune", actions);
            return exact(inputs, "Elemental staves can replace their corresponding elemental runes.");
        }
        return UniversalActionRecipe.unknown(
                "Rune costs depend on the exact spell and equipped staff. The cast count is known, but the fallback does not invent a rune loadout.");
    }

    private static UniversalActionRecipe noInput(String setup)
    {
        return UniversalActionRecipe.noConsumedInputs(setup);
    }

    private static UniversalActionRecipe one(String item, int actions, String setup)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        add(inputs, item, actions);
        return exact(inputs, setup);
    }

    private static UniversalActionRecipe exact(List<ResolvedMethodInput> inputs, String setup)
    {
        return new UniversalActionRecipe(inputs, setup, true);
    }

    private static void add(List<ResolvedMethodInput> inputs, String name, int quantity)
    {
        if (name == null || name.trim().isEmpty() || quantity <= 0) return;
        inputs.add(new ResolvedMethodInput(name, -1, quantity));
    }

    static int smithingBarsFor(String lower)
    {
        if (lower == null) return 0;
        if (lower.contains("platebody")) return 5;
        if (lower.contains("plateskirt") || lower.contains("platelegs")) return 3;
        if (lower.contains("2h sword")) return 3;
        if (lower.contains("kiteshield") || lower.contains("chainbody")) return 3;
        if (lower.contains("battleaxe") || lower.contains("warhammer")) return 3;
        if (lower.contains("claws")) return 2;
        if (lower.contains("full helm") || lower.contains("sq shield")) return 2;
        if (lower.contains("longsword") || lower.contains("scimitar")) return 2;
        if (containsAny(lower, "mace", "sword", "dagger", " axe", "med helm",
                "dart tip", "knife", "arrowtip", "nails", "wire", "unfinished bolt")) return 1;
        return 0;
    }

    private static String rawFood(String name)
    {
        if (name == null || name.trim().isEmpty()) return null;
        String clean = name.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith("raw ")) return clean;
        if (lower.startsWith("cooked ")) clean = clean.substring(7).trim();
        if (clean.isEmpty()) return null;
        return "Raw " + clean.toLowerCase(Locale.ROOT);
    }

    private static boolean isCutGem(String lower)
    {
        return containsAny(lower, "opal", "jade", "red topaz", "sapphire",
                "emerald", "ruby", "diamond", "dragonstone", "onyx", "zenyte")
                && !lower.contains("ring") && !lower.contains("necklace")
                && !lower.contains("amulet") && !lower.contains("bracelet");
    }

    private static boolean isGlassProduct(String lower)
    {
        return containsAny(lower, "glass", "orb", "lantern lens", "vial", "fishbowl")
                && !lower.contains("molten glass");
    }

    private static boolean isDragonhideProduct(String lower)
    {
        return lower.contains("d'hide") || lower.contains("dragonhide");
    }

    private static boolean isPlainLeatherProduct(String lower)
    {
        return lower.contains("leather body") || lower.contains("leather chaps")
                || lower.contains("leather vambraces");
    }

    private static int armourMaterialCount(String lower)
    {
        if (lower.contains("body")) return 3;
        if (lower.contains("chaps")) return 2;
        if (lower.contains("vambrace")) return 1;
        return 0;
    }

    private static String dragonLeatherName(String lower)
    {
        for (String color : new String[]{"green", "blue", "red", "black"})
        {
            if (lower.contains(color)) return capitalize(color) + " dragon leather";
        }
        return "Dragon leather";
    }

    private static boolean isJewellery(String lower)
    {
        return containsAny(lower, " ring", "bracelet", "necklace", "amulet");
    }

    private static String jewelleryGem(String lower)
    {
        for (String gem : new String[]{"sapphire", "emerald", "ruby", "diamond", "dragonstone", "onyx", "zenyte"})
        {
            if (lower.contains(gem)) return capitalize(gem);
        }
        return null;
    }

    private static String elementalOrb(String lower)
    {
        if (lower.contains("air")) return "Air orb";
        if (lower.contains("water")) return "Water orb";
        if (lower.contains("earth")) return "Earth orb";
        if (lower.contains("fire")) return "Fire orb";
        return null;
    }

    private static String logForBow(String name)
    {
        String lower = clean(name).toLowerCase(Locale.ROOT);
        for (String wood : new String[]{"oak", "willow", "maple", "yew", "magic", "redwood"})
        {
            if (lower.startsWith(wood + " ")) return capitalize(wood) + " logs";
        }
        return "Logs";
    }

    private static String metalBarFor(String lower)
    {
        if (lower.contains("bronze")) return "Bronze bar";
        if (lower.contains("iron")) return "Iron bar";
        if (lower.contains("steel")) return "Steel bar";
        if (lower.contains("mithril")) return "Mithril bar";
        if (lower.contains("adamant")) return "Adamantite bar";
        if (lower.contains("rune")) return "Runite bar";
        return null;
    }

    private static PotionRecipe potionRecipe(String lower)
    {
        if (lower.contains("attack potion") && !lower.contains("super")) return p("Guam leaf", "Eye of newt");
        if (lower.contains("antipoison") && !lower.contains("super")) return p("Marrentill", "Unicorn horn dust");
        if (lower.contains("strength potion") && !lower.contains("super")) return p("Tarromin", "Limpwurt root");
        if (lower.contains("restore potion") && !lower.contains("super")) return p("Harralander", "Red spiders' eggs");
        if (lower.contains("energy potion") && !lower.contains("super")) return p("Harralander", "Chocolate dust");
        if (lower.contains("defence potion") && !lower.contains("super")) return p("Ranarr weed", "White berries");
        if (lower.contains("agility potion")) return p("Toadflax", "Toad's legs");
        if (lower.contains("combat potion") && !lower.contains("super")) return p("Harralander", "Goat horn dust");
        if (lower.contains("prayer potion")) return p("Ranarr weed", "Snape grass");
        if (lower.contains("super attack")) return p("Irit leaf", "Eye of newt");
        if (lower.contains("superantipoison")) return p("Irit leaf", "Unicorn horn dust");
        if (lower.contains("fishing potion")) return p("Avantoe", "Snape grass");
        if (lower.contains("super energy")) return p("Avantoe", "Mort myre fungus");
        if (lower.contains("hunter potion")) return p("Avantoe", "Kebbit teeth dust");
        if (lower.contains("super strength")) return p("Kwuarm", "Limpwurt root");
        if (lower.contains("weapon poison")) return p("Kwuarm", "Dragon scale dust");
        if (lower.contains("super restore")) return p("Snapdragon", "Red spiders' eggs");
        if (lower.contains("super defence")) return p("Cadantine", "White berries");
        if (lower.contains("antifire")) return p("Lantadyme", "Dragon scale dust");
        if (lower.contains("ranging potion")) return p("Dwarf weed", "Wine of zamorak");
        if (lower.contains("magic potion")) return p("Lantadyme", "Potato cactus");
        if (lower.contains("zamorak brew")) return p("Torstol", "Jangerberries");
        if (lower.contains("saradomin brew")) return p("Toadflax", "Crushed nest");
        return null;
    }

    private static PotionRecipe p(String primary, String secondary)
    {
        return new PotionRecipe(primary, secondary);
    }

    private static String firstWord(String value)
    {
        String clean = clean(value);
        int space = clean.indexOf(' ');
        return space < 0 ? clean : clean.substring(0, space);
    }

    private static boolean containsAny(String text, String... values)
    {
        if (text == null || values == null) return false;
        for (String value : values)
        {
            if (value != null && text.contains(value)) return true;
        }
        return false;
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.trim();
    }

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static final class PotionRecipe
    {
        private final String primary;
        private final String secondary;

        private PotionRecipe(String primary, String secondary)
        {
            this.primary = primary;
            this.secondary = secondary;
        }
    }
}
