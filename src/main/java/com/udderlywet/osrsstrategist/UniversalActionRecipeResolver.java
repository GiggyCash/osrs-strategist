package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Conservative consumed-input resolver for RuneLite skill-calculator actions.
 * Exact is only returned when the material recipe is stable and unambiguous.
 *
 * <p>The RuneLite calculator often expresses XP per produced item, while the
 * game interface may create a batch. Recipes therefore model the calculator's
 * action unit rather than assuming that one click equals one output.</p>
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
                return firemaking(name, lower, actions);
            case PRAYER:
                return prayer(name, lower, actions);
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
                        "The action XP is known, but no safe generic material recipe is available for this skill.");
        }
    }

    private static UniversalActionRecipe cooking(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("jug of wine") || lower.equals("wine"))
        {
            add(inputs, "Grapes", actions);
            add(inputs, "Jug of water", actions);
            return exact(inputs,
                    "One grapes and one jug of water are consumed per completed wine action.");
        }
        if (containsAny(lower, "cake", "pie", "pizza", "stew", "curry"))
        {
            return UniversalActionRecipe.unknown(
                    "Composite Cooking recipes need a recipe-specific ingredient model before a shopping list can be shown.");
        }
        String raw = rawFood(name);
        if (raw == null)
        {
            return UniversalActionRecipe.unknown(
                    "Raw food could not be inferred safely.");
        }
        add(inputs, raw, actions);
        return exact(inputs,
                "This is the successful-cook input count. A burn-aware route must add a level and location specific raw-food buffer when burns are possible.");
    }

    private static UniversalActionRecipe firemaking(
            String name,
            String lower,
            int actions)
    {
        // RuneLite Firemaking actions are normally named for the consumed log.
        // Reject special activities rather than turning their reward/action name
        // into a fake log requirement.
        if (!lower.contains("log") && !lower.equals("logs"))
        {
            return UniversalActionRecipe.unknown(
                    "This Firemaking action is not a conventional one-log burn and needs a route-specific setup model.");
        }
        return one(name, actions,
                "Bring a tinderbox or another verified ignition source.");
    }

    private static UniversalActionRecipe prayer(
            String name,
            String lower,
            int actions)
    {
        if (containsAny(lower, "bones", "ashes", "head"))
        {
            return one(name, actions,
                    "This models one consumed Prayer item per calculator action. Altar, offering, and reanimation multipliers belong to the selected route.");
        }
        return UniversalActionRecipe.unknown(
                "The Prayer action XP is known, but the consumed offering cannot be inferred safely from its name.");
    }

    private static UniversalActionRecipe runecraft(
            int actions,
            MembershipStatus membership)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        add(inputs, membership != MembershipStatus.P2P
                ? "Rune essence" : "Pure essence", actions);
        return exact(inputs,
                "Rune multipliers affect output, not base XP per essence. Pouches, raiments, and alternate essence types are route modifiers rather than extra consumed units.");
    }

    private static UniversalActionRecipe crafting(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();

        if (lower.equals("ball of wool"))
        {
            add(inputs, "Wool", actions);
            return exact(inputs, "Use a spinning wheel.");
        }
        if (lower.equals("bow string"))
        {
            add(inputs, "Flax", actions);
            return exact(inputs, "Use a spinning wheel.");
        }
        if (lower.equals("unfired pot"))
        {
            add(inputs, "Soft clay", actions);
            return exact(inputs, "Use the clay on a pottery wheel.");
        }
        if (lower.equals("pot"))
        {
            add(inputs, "Unfired pot", actions);
            return exact(inputs, "Fire the unfired pots in a pottery oven.");
        }
        if (lower.equals("unfired pie dish"))
        {
            add(inputs, "Soft clay", actions);
            return exact(inputs, "Use the clay on a pottery wheel.");
        }
        if (lower.equals("pie dish"))
        {
            add(inputs, "Unfired pie dish", actions);
            return exact(inputs, "Fire the unfired dishes in a pottery oven.");
        }
        if (lower.equals("unfired bowl"))
        {
            add(inputs, "Soft clay", actions);
            return exact(inputs, "Use the clay on a pottery wheel.");
        }
        if (lower.equals("bowl"))
        {
            add(inputs, "Unfired bowl", actions);
            return exact(inputs, "Fire the unfired bowls in a pottery oven.");
        }
        if (lower.equals("unfired plant pot") || lower.equals("unfired pot lid"))
        {
            add(inputs, "Soft clay", actions);
            return exact(inputs, "Use the clay on a pottery wheel.");
        }
        if (lower.equals("empty plant pot"))
        {
            add(inputs, "Unfired plant pot", actions);
            return exact(inputs, "Fire the unfired plant pots in a pottery oven.");
        }
        if (lower.equals("pot lid"))
        {
            add(inputs, "Unfired pot lid", actions);
            return exact(inputs, "Fire the unfired lids in a pottery oven.");
        }
        if (lower.equals("molten glass"))
        {
            add(inputs, "Bucket of sand", actions);
            add(inputs, "Soda ash", actions);
            return exact(inputs,
                    "This models conventional furnace glassmaking. Superglass Make has different yield math and must use its own route.");
        }

        if (isCutGem(lower))
        {
            add(inputs, lower.startsWith("uncut ")
                    ? name : "Uncut " + lower, actions);
            return exact(inputs, "Bring a chisel.");
        }
        if (isGlassProduct(lower))
        {
            add(inputs, "Molten glass", actions);
            return exact(inputs,
                    "Bring a glassblowing pipe for blown-glass products.");
        }
        if (isBirdHouse(lower))
        {
            add(inputs, birdHouseLog(lower), actions);
            return exact(inputs,
                    "A clockwork is reusable between bird houses, so only the consumed log is counted here.");
        }
        if (isDragonhideProduct(lower))
        {
            int count = dragonhideMaterialCount(lower);
            if (count > 0)
            {
                add(inputs, dragonLeatherName(lower),
                        safeMultiply(actions, count));
                return exact(inputs,
                        "Needle and thread are setup supplies. Thread is not modeled as one full item consumed per craft.");
            }
        }
        if (isPlainLeatherProduct(lower))
        {
            add(inputs, "Leather", actions);
            return exact(inputs,
                    "Each modeled plain-leather item consumes one leather. Bring a needle and thread.");
        }
        if (lower.contains("hardleather body")
                || lower.contains("hard leather body"))
        {
            add(inputs, "Hard leather", actions);
            return exact(inputs, "Bring a needle and thread.");
        }
        if (isJewellery(lower))
        {
            String gem = jewelleryGem(lower);
            add(inputs, jewelleryBar(lower, gem), actions);
            if (gem != null) add(inputs, gem, actions);
            return exact(inputs,
                    "Bring the mould for the selected jewellery piece. Opal, jade, and red topaz jewellery use silver; standard precious-gem jewellery uses gold.");
        }
        if (lower.equals("tiara"))
        {
            add(inputs, "Silver bar", actions);
            return exact(inputs,
                    "Bring a tiara mould to the Edgeville furnace, smelt each silver bar into a tiara, bank, and repeat.");
        }
        if (lower.equals("gold tiara"))
        {
            add(inputs, "Gold bar", actions);
            return exact(inputs,
                    "Bring a tiara mould to the Edgeville furnace, smelt each gold bar into a gold tiara, bank, and repeat.");
        }
        if (lower.contains("battlestaff") || lower.contains("battlestave"))
        {
            String orb = elementalOrb(lower);
            if (orb != null)
            {
                add(inputs, "Battlestaff", actions);
                add(inputs, orb, actions);
                return exact(inputs,
                        "This models attaching a charged elemental orb to a battlestaff.");
            }
        }
        return UniversalActionRecipe.unknown(
                "This Crafting action has fixed XP, but the generic resolver does not have a proven consumed-material recipe for it.");
    }

    private static UniversalActionRecipe fletching(String name, String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.equals("arrow shaft") || lower.equals("arrow shafts"))
        {
            // RuneLite records 0.33 XP per shaft. The basic normal-log recipe
            // produces 15 shafts from one log, so the material count is based on
            // shafts, not clicks.
            add(inputs, "Logs", (int) Math.ceil(actions / 15.0));
            return exact(inputs,
                    "The fallback models the basic normal-log recipe at 15 shafts per log. Higher-log shaft routes need a dedicated profile before their different yield is claimed.");
        }
        if (lower.equals("headless arrow") || lower.equals("headless arrows"))
        {
            add(inputs, "Arrow shaft", actions);
            add(inputs, "Feather", actions);
            return exact(inputs,
                    "RuneLite expresses this as XP per headless arrow even though the interface processes shafts in batches.");
        }
        if (lower.endsWith(" shortbow (u)") || lower.endsWith(" longbow (u)")
                || lower.equals("shortbow (u)") || lower.equals("longbow (u)"))
        {
            add(inputs, logForBow(name), actions);
            return exact(inputs, "Bring a knife.");
        }
        if ((lower.contains("shortbow") || lower.contains("longbow"))
                && !lower.contains("(u)"))
        {
            add(inputs, name + " (u)", actions);
            add(inputs, "Bow string", actions);
            return exact(inputs, "This models stringing an unstrung bow.");
        }
        if (lower.endsWith(" shield"))
        {
            String logs = logsForWoodPrefix(name);
            if (logs != null)
            {
                add(inputs, logs, safeMultiply(actions, 2));
                return exact(inputs,
                        "Standard Fletching shields consume two matching logs per shield. Bring a knife.");
            }
        }
        if (lower.endsWith(" stock"))
        {
            String logs = logsForWoodPrefix(name);
            if (logs != null)
            {
                add(inputs, logs, actions);
                return exact(inputs,
                        "A standard crossbow stock consumes one matching log. Bring a knife.");
            }
        }
        if (lower.endsWith(" dart") || lower.endsWith(" darts"))
        {
            add(inputs, firstWord(name) + " dart tip", actions);
            add(inputs, "Feather", actions);
            return exact(inputs,
                    "One dart tip and one feather are consumed per dart. RuneLite's action XP is per finished dart.");
        }
        if (lower.contains("broad arrow"))
        {
            add(inputs, "Headless arrow", actions);
            add(inputs, "Broad arrowhead", actions);
            return exact(inputs,
                    "One headless arrow and one broad arrowhead are consumed per finished arrow.");
        }
        if (isStandardArrow(lower))
        {
            add(inputs, "Headless arrow", actions);
            add(inputs, firstWord(name) + " arrowhead", actions);
            return exact(inputs,
                    "One headless arrow and one matching arrowhead are consumed per finished arrow.");
        }
        if (isJavelin(lower))
        {
            add(inputs, "Javelin shaft", actions);
            add(inputs, firstWord(name) + " javelin head", actions);
            return exact(inputs,
                    "One shaft and one matching javelin head are consumed per javelin.");
        }
        if (isBasicFeatheredBolt(lower))
        {
            String metal = firstWord(name);
            add(inputs, metal + " bolts (unf)", actions);
            add(inputs, "Feather", actions);
            return exact(inputs,
                    "One unfinished bolt and one feather are consumed per finished basic bolt. Gem-tipped and specialty bolts are intentionally excluded from this generic recipe.");
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
                return exact(inputs,
                        "Standard furnace input. Iron smelting failure must be handled by the selected route when applicable.");
            case "silver bar":
                add(inputs, "Silver ore", actions);
                return exact(inputs, "Standard furnace recipe.");
            case "gold bar":
                add(inputs, "Gold ore", actions);
                return exact(inputs,
                        "Standard furnace recipe. Goldsmith gauntlet XP belongs in the route XP modifier.");
            case "steel bar":
                add(inputs, "Iron ore", actions);
                add(inputs, "Coal", safeMultiply(actions, 2));
                return exact(inputs,
                        "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "mithril bar":
                add(inputs, "Mithril ore", actions);
                add(inputs, "Coal", safeMultiply(actions, 4));
                return exact(inputs,
                        "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "adamantite bar":
                add(inputs, "Adamantite ore", actions);
                add(inputs, "Coal", safeMultiply(actions, 6));
                return exact(inputs,
                        "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            case "runite bar":
                add(inputs, "Runite ore", actions);
                add(inputs, "Coal", safeMultiply(actions, 8));
                return exact(inputs,
                        "Standard furnace recipe. Blast Furnace uses a dedicated coal model.");
            default:
                break;
        }

        String bar = metalBarFor(lower);
        int bars = smithingBarsFor(lower);
        if (bar != null && bars > 0)
        {
            add(inputs, bar, safeMultiply(actions, bars));
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
                "The shopping list models making the complete potion from raw herb, secondary, and vial. Saving effects and decanting can reduce effective supply use.");
    }

    private static UniversalActionRecipe construction(String lower, int actions)
    {
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        if (lower.contains("oak larder"))
        {
            add(inputs, "Oak plank", safeMultiply(actions, 8));
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("oak dungeon door") || lower.equals("oak door"))
        {
            add(inputs, "Oak plank", safeMultiply(actions, 10));
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("mahogany table"))
        {
            add(inputs, "Mahogany plank", safeMultiply(actions, 6));
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("crude wooden chair"))
        {
            add(inputs, "Plank", safeMultiply(actions, 2));
            add(inputs, "Steel nails", safeMultiply(actions, 2));
            return exact(inputs,
                    "Bring a hammer and saw. Steel nails or better reduce bending; the count is the minimum consumed by successful builds.");
        }
        if (lower.contains("teak garden bench"))
        {
            add(inputs, "Teak plank", safeMultiply(actions, 6));
            return exact(inputs, "Bring a hammer and saw or verified equivalents.");
        }
        if (lower.contains("mythical cape"))
        {
            add(inputs, "Teak plank", safeMultiply(actions, 3));
            return exact(inputs,
                    "The mounted cape is returned when removed; only consumed planks are counted.");
        }
        return UniversalActionRecipe.unknown(
                "Furniture recipes vary. The exact build count is retained without fabricating plank counts for an unmapped hotspot.");
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
        if (lower.equals("wind strike"))
        {
            add(inputs, "Air rune", actions);
            add(inputs, "Mind rune", actions);
            return exact(inputs,
                    "This safe baseline counts one air rune and one mind rune per cast.");
        }
        if (lower.equals("fire bolt"))
        {
            add(inputs, "Air rune", safeMultiply(actions, 3));
            add(inputs, "Fire rune", safeMultiply(actions, 4));
            add(inputs, "Chaos rune", actions);
            return exact(inputs, "This safe baseline counts the full rune cost.");
        }
        if (lower.equals("fire blast"))
        {
            add(inputs, "Air rune", safeMultiply(actions, 4));
            add(inputs, "Fire rune", safeMultiply(actions, 5));
            add(inputs, "Death rune", actions);
            return exact(inputs, "This safe baseline counts the full rune cost.");
        }
        if (lower.contains("high level alchemy") || lower.contains("high alchemy"))
        {
            add(inputs, "Nature rune", actions);
            add(inputs, "Fire rune", safeMultiply(actions, 5));
            return exact(inputs,
                    "An equipped fire-rune source can replace the fire runes. The item being alched must come from a verified safe alch list and is deliberately not invented here.");
        }
        if (lower.equals("curse") || lower.endsWith(" curse"))
        {
            add(inputs, "Earth rune", safeMultiply(actions, 3));
            add(inputs, "Water rune", safeMultiply(actions, 2));
            add(inputs, "Body rune", actions);
            return exact(inputs,
                    "Equipped elemental staves can replace their corresponding elemental runes.");
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

    private static UniversalActionRecipe exact(
            List<ResolvedMethodInput> inputs,
            String setup)
    {
        return new UniversalActionRecipe(inputs, setup, true);
    }

    private static void add(
            List<ResolvedMethodInput> inputs,
            String name,
            int quantity)
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
        if (containsAny(lower,
                "mace", "sword", "dagger", " axe", "med helm",
                "dart tip", "knife", "arrowtip", "nails", "wire",
                "unfinished bolt")) return 1;
        return 0;
    }

    private static String rawFood(String name)
    {
        if (name == null || name.trim().isEmpty()) return null;
        String value = name.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("raw ")) return value;
        if (lower.startsWith("cooked ")) value = value.substring(7).trim();
        if (value.isEmpty()) return null;
        return "Raw " + value.toLowerCase(Locale.ROOT);
    }

    private static boolean isCutGem(String lower)
    {
        return containsAny(lower,
                "opal", "jade", "red topaz", "sapphire", "emerald", "ruby",
                "diamond", "dragonstone", "onyx", "zenyte")
                && !isJewellery(lower)
                && !lower.contains("bolt");
    }

    private static boolean isGlassProduct(String lower)
    {
        return containsAny(lower,
                "beer glass", "candle lantern", "oil lamp", "vial",
                "fishbowl", "unpowered orb", "lantern lens")
                && !lower.contains("molten glass");
    }

    private static boolean isBirdHouse(String lower)
    {
        return lower.contains("bird house");
    }

    private static String birdHouseLog(String lower)
    {
        for (String wood : new String[]{
                "redwood", "magic", "yew", "mahogany", "maple", "teak",
                "willow", "oak"})
        {
            if (lower.startsWith(wood + " ")) return capitalize(wood) + " logs";
        }
        return "Logs";
    }

    private static boolean isDragonhideProduct(String lower)
    {
        return lower.contains("d'hide") || lower.contains("dragonhide");
    }

    private static boolean isPlainLeatherProduct(String lower)
    {
        return lower.equals("leather gloves")
                || lower.equals("leather boots")
                || lower.equals("cowl")
                || lower.equals("leather vambraces")
                || lower.equals("leather body")
                || lower.equals("leather chaps")
                || lower.equals("coif");
    }

    private static int dragonhideMaterialCount(String lower)
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
            if (lower.contains(color))
            {
                return capitalize(color) + " dragon leather";
            }
        }
        return "Dragon leather";
    }

    private static boolean isJewellery(String lower)
    {
        return lower.endsWith(" ring")
                || lower.endsWith(" bracelet")
                || lower.endsWith(" necklace")
                || lower.contains("amulet");
    }

    private static String jewelleryGem(String lower)
    {
        if (lower.contains("red topaz") || lower.contains("topaz"))
            return "Red topaz";
        for (String gem : new String[]{
                "opal", "jade", "sapphire", "emerald", "ruby", "diamond",
                "dragonstone", "onyx", "zenyte"})
        {
            if (lower.contains(gem)) return capitalize(gem);
        }
        return null;
    }

    private static String jewelleryBar(String lower, String gem)
    {
        String normalizedGem = gem == null
                ? "" : gem.toLowerCase(Locale.ROOT);
        if (normalizedGem.equals("opal")
                || normalizedGem.equals("jade")
                || normalizedGem.equals("red topaz"))
        {
            return "Silver bar";
        }
        return "Gold bar";
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
        for (String wood : new String[]{
                "oak", "willow", "maple", "yew", "magic", "redwood"})
        {
            if (lower.startsWith(wood + " ")) return capitalize(wood) + " logs";
        }
        return "Logs";
    }

    private static String logsForWoodPrefix(String name)
    {
        String lower = clean(name).toLowerCase(Locale.ROOT);
        for (String wood : new String[]{
                "oak", "willow", "maple", "yew", "magic", "redwood",
                "teak", "mahogany"})
        {
            if (lower.startsWith(wood + " ")) return capitalize(wood) + " logs";
        }
        if (lower.startsWith("wooden ")) return "Logs";
        return null;
    }

    private static boolean isStandardArrow(String lower)
    {
        if (!(lower.endsWith(" arrow") || lower.endsWith(" arrows"))) return false;
        return containsAny(lower,
                "bronze", "iron", "steel", "mithril", "adamant", "rune",
                "amethyst", "dragon");
    }

    private static boolean isJavelin(String lower)
    {
        if (!(lower.endsWith(" javelin") || lower.endsWith(" javelins"))) return false;
        return containsAny(lower,
                "bronze", "iron", "steel", "mithril", "adamant", "rune",
                "amethyst", "dragon");
    }

    private static boolean isBasicFeatheredBolt(String lower)
    {
        if (!(lower.endsWith(" bolts") || lower.endsWith(" bolt"))) return false;
        if (containsAny(lower,
                "opal", "pearl", "barbed", "kebbit", "sapphire", "emerald",
                "ruby", "diamond", "dragonstone", "onyx", "amethyst",
                "broad"))
        {
            return false;
        }
        return containsAny(lower,
                "bronze", "blurite", "iron", "silver", "steel", "mithril",
                "adamant", "runite", "rune", "dragon");
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
        if (lower.contains("attack potion") && !lower.contains("super"))
            return p("Guam leaf", "Eye of newt");
        if (lower.contains("antipoison") && !lower.contains("super"))
            return p("Marrentill", "Unicorn horn dust");
        if (lower.contains("strength potion") && !lower.contains("super"))
            return p("Tarromin", "Limpwurt root");
        if (lower.contains("restore potion") && !lower.contains("super"))
            return p("Harralander", "Red spiders' eggs");
        if (lower.contains("energy potion") && !lower.contains("super"))
            return p("Harralander", "Chocolate dust");
        if (lower.contains("defence potion") && !lower.contains("super"))
            return p("Ranarr weed", "White berries");
        if (lower.contains("agility potion"))
            return p("Toadflax", "Toad's legs");
        if (lower.contains("combat potion") && !lower.contains("super"))
            return p("Harralander", "Goat horn dust");
        if (lower.contains("prayer potion"))
            return p("Ranarr weed", "Snape grass");
        if (lower.contains("super attack"))
            return p("Irit leaf", "Eye of newt");
        if (lower.contains("superantipoison"))
            return p("Irit leaf", "Unicorn horn dust");
        if (lower.contains("fishing potion"))
            return p("Avantoe", "Snape grass");
        if (lower.contains("super energy"))
            return p("Avantoe", "Mort myre fungus");
        if (lower.contains("hunter potion"))
            return p("Avantoe", "Kebbit teeth dust");
        if (lower.contains("super strength"))
            return p("Kwuarm", "Limpwurt root");
        if (lower.contains("weapon poison"))
            return p("Kwuarm", "Dragon scale dust");
        if (lower.contains("super restore"))
            return p("Snapdragon", "Red spiders' eggs");
        if (lower.contains("super defence"))
            return p("Cadantine", "White berries");
        if (lower.contains("antifire"))
            return p("Lantadyme", "Dragon scale dust");
        if (lower.contains("ranging potion"))
            return p("Dwarf weed", "Wine of zamorak");
        if (lower.contains("magic potion"))
            return p("Lantadyme", "Potato cactus");
        if (lower.contains("zamorak brew"))
            return p("Torstol", "Jangerberries");
        if (lower.contains("saradomin brew"))
            return p("Toadflax", "Crushed nest");
        return null;
    }

    private static PotionRecipe p(String primary, String secondary)
    {
        return new PotionRecipe(primary, secondary);
    }

    private static String firstWord(String value)
    {
        String cleaned = clean(value);
        int space = cleaned.indexOf(' ');
        return space < 0 ? cleaned : cleaned.substring(0, space);
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

    private static int safeMultiply(int a, int b)
    {
        if (a <= 0 || b <= 0) return 0;
        if (a > Integer.MAX_VALUE / b) return Integer.MAX_VALUE;
        return a * b;
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
