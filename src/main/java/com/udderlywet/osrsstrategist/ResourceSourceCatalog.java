package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;

/**
 * Common account-progression resource routes. Exact shop stock, drop rate, or
 * current GE economics remain separate live-data concerns.
 */
@Singleton
public class ResourceSourceCatalog
{
    private final List<ResourceSourceDefinition> sources = new ArrayList<>();

    public ResourceSourceCatalog()
    {
        woodAndConstruction();
        miningAndSmithing();
        farmingAndHerblore();
        runesAndMagic();
        fishingAndCooking();
        craftingAndFletching();
        prayerAndCombat();
        commonProgressionFamilies();
    }

    public List<ResourceSourceDefinition> all()
    {
        return Collections.unmodifiableList(sources);
    }

    public List<ResourceSourceDefinition> match(String itemName)
    {
        String normalized = normalize(itemName);
        if (normalized.isEmpty()) return Collections.emptyList();
        List<ResourceSourceDefinition> result = new ArrayList<>();
        for (ResourceSourceDefinition source : sources)
        {
            if ("raw-fish".equals(source.getId()) && !normalized.startsWith("raw ")) continue;
            if ("cooked-food".equals(source.getId()) && normalized.startsWith("raw ")) continue;

            for (String token : source.getNameTokens())
            {
                if (containsPhrase(normalized, normalize(token)))
                {
                    result.add(source);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> suggestions(String itemName, AccountMode mode,
            boolean allowWilderness)
    {
        List<String> result = new ArrayList<>();
        for (ResourceSourceDefinition source : match(itemName))
        {
            if (source.isWilderness() && !allowWilderness) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && source.getRiskLevel() == RiskLevel.HIGH) continue;

            String route;
            if (mode == AccountMode.ULTIMATE_IRONMAN)
                route = source.getUimRoute();
            else if (mode != null && mode.isIronLike())
                route = source.getIronRoute();
            else
                route = source.getMainRoute();

            if (route != null && !route.trim().isEmpty() && !result.contains(route))
                result.add(route);
            if (result.size() >= 4) break;
        }
        return Collections.unmodifiableList(result);
    }

    private void woodAndConstruction()
    {
        add("logs", tokens("logs", "log"),
                "Buy if current GE price beats the time value of chopping; otherwise cut the required tree tier.",
                "Cut the required tree tier or use verified PvM/minigame log rewards already available to the account.",
                "Cut only the quantity needed near the destination, or use verified noted/reward sources that do not create long-term inventory pressure.");
        add("planks", tokens("plank", "planks"),
                "Compare GE planks with converting owned logs at a sawmill or through Plank Make.",
                "Convert self-sourced logs at an unlocked sawmill/Plank Make route; use Managing Miscellania only when already unlocked and funded.",
                "Prefer just-in-time sawmill/Plank Make conversion and verified POH/transport routes; never assume banked plank storage.");
        add("nails", tokens("nail", "nails"),
                "Buy the required nail tier or smith from suitable bars if that is cheaper.",
                "Smith nails from suitable bars or use a verified shop source.",
                "Smith/buy only the immediate quantity needed for the current Construction step.");
    }

    private void miningAndSmithing()
    {
        add("ores", tokens("ore", "coal"),
                "Compare GE ore prices with mining time and any Blast Furnace processing plan.",
                "Mine the required ore, obtain it from verified PvM/minigame rewards, or use an unlocked shop where appropriate.",
                "Mine/process in small batches near the destination; avoid routes that depend on normal bank storage.");
        add("bars", tokens("bar", "bars"),
                "Buy bars only after comparing ore+processing cost with finished-bar price.",
                "Smelt self-sourced ore, use Blast Furnace when unlocked, or reclaim bars from verified rewards/drops.",
                "Use Blast Furnace or nearby furnaces in just-in-time batches and immediately consume the bars in the target activity.");
        add("cannonballs", tokens("cannonball", "cannonballs"),
                "Compare buying cannonballs with the time saved on the planned Slayer/combat task.",
                "Smith steel bars into cannonballs when Dwarf Cannon and the mould are available; preserve steel if other progression needs are higher.",
                "Smith only the batch needed for the planned task; account for mould and inventory slots before starting.");
    }

    private void farmingAndHerblore()
    {
        add("herb-seeds", tokens("seed", "seeds"),
                "Buy the seed only if expected herb value/XP and current price justify it.",
                "Prioritize Farming contracts, Master Farmers, Slayer/PvM seed drops, birdhouses, and existing seed packs.",
                "Prioritize seed packs/contracts and immediately usable seeds; verify seed-box access before treating it as storage.");
        add("herbs", tokens("grimy", "ranarr", "snapdragon", "toadflax", "kwuarm", "cadantine", "lantadyme", "dwarf weed", "torstol", "irit", "avantoe", "harralander", "tarromin", "guam", "marrentill"),
                "Compare buying herbs with farming them and the value of the finished potion.",
                "Farm herbs, use Slayer/PvM herb drops, and use kingdom/herb rewards only when those systems are unlocked.",
                "Favor herb runs and immediately process herbs into needed potions; verify herb-sack/storage capability before counting it.");
        add("snape-grass", tokens("snape grass"),
                "Buy or farm snape grass according to current price and potion volume.",
                "Grow snape grass at allotments or gather from a verified spawn when farming is not yet practical.",
                "Grow/gather only the immediate potion batch to minimize carried secondaries.");
        add("red-spider-eggs", tokens("red spiders' eggs", "red spider eggs"),
                "Buy eggs after comparing current price with collection time.",
                "Use a safe verified spawn or unlocked repeatable source; avoid dangerous collection routes on Hardcore.",
                "Collect in small batches close to the potion-making route and avoid storage assumptions.");
        add("bird-nests", tokens("bird nest", "bird nests", "crushed nest", "crushed nests"),
                "Buy nests only after comparing current brew economics with birdhouse/nest acquisition time.",
                "Birdhouse runs, woodcutting nests, and verified PvM rewards are preferred self-sources.",
                "Birdhouse runs are the preferred low-inventory-pressure source; crush/process nests near use.");
    }

    private void runesAndMagic()
    {
        add("runes", tokens("air rune", "mind rune", "water rune", "earth rune",
                        "fire rune", "body rune", "cosmic rune", "chaos rune",
                        "nature rune", "law rune", "death rune", "astral rune",
                        "blood rune", "soul rune", "wrath rune"),
                "Compare GE rune cost with shop stock and Runecraft time for the planned spell volume.",
                "Runecraft, Guardians of the Rift, rune shops, and verified monster drops are standard self-sources.",
                "Use rune shops/GOTR/Runecraft in task-sized batches; verify rune pouch before counting it as storage.");
        add("essence", tokens("pure essence", "rune essence", "daeyalt essence"),
                "Buy essence when cheaper than sourcing time unless a training route specifically benefits from self-sourcing.",
                "Use verified mining, PvM drops, or Daeyalt access when appropriate to the target Runecraft method.",
                "Acquire essence immediately before the Runecraft session; avoid accumulating unusable inventory stacks.");
    }

    private void fishingAndCooking()
    {
        add("raw-fish", tokens("raw shark", "raw karambwan", "raw monkfish",
                        "raw swordfish", "raw lobster", "raw anglerfish", "raw manta ray"),
                "Buy raw food only when Cooking XP or finished-food value justifies the current spread.",
                "Fish the required food or use verified minigame/PvM food rewards.",
                "Fish and cook in the same route where possible; keep only the immediate combat/quest supply.");
        add("cooked-food", tokens("shark", "karambwan", "anglerfish", "manta ray",
                        "lobster", "swordfish", "moonlight antelope"),
                "Buy food according to encounter risk and current price rather than always using the most expensive option.",
                "Fish/hunt/cook sustainable food or use verified PvM/minigame supplies appropriate to the encounter.",
                "Choose food that fits available inventory slots and resupply route; do not assume a banked reserve.");
    }

    private void craftingAndFletching()
    {
        add("glass", tokens("molten glass", "bucket of sand", "soda ash", "giant seaweed"),
                "Compare buying molten glass/materials with Superglass Make and sand/seaweed processing.",
                "Grow giant seaweed, obtain sand through verified collection/mining, and use Superglass Make when unlocked.",
                "Use giant seaweed/sand and Superglass Make in immediate Crafting batches to control inventory pressure.");
        add("bowstrings", tokens("bow string", "bow strings", "flax"),
                "Buy bowstrings if the current margin makes stringing worthwhile; otherwise prefer faster Fletching routes.",
                "Pick/spin flax or use verified Temple Trekking/reward sources when bowstrings are strategically useful.",
                "Use a nearby flax/spinning or reward route and string bows immediately rather than storing large batches.");
        add("feathers", tokens("feather", "feathers"),
                "Buy feathers from GE/shops according to current price and required quantity.",
                "Use feather shops, bird drops, or fishing-related supply sources.",
                "Shop-buy the immediate stack needed; feathers are stackable and UIM-friendly once acquired.");
        add("gems", tokens("uncut sapphire", "uncut emerald", "uncut ruby",
                        "uncut diamond", "uncut dragonstone"),
                "Compare uncut gem price with the finished product and XP value.",
                "Mine gem rocks, use Mining/PvM rewards, or cut banked gems already earned.",
                "Gem rocks and immediate cutting are preferred; only retain gems with a clear near-term use.");
    }

    private void prayerAndCombat()
    {
        add("bones", tokens("bones", "dragon bones", "wyrm bones", "wyvern bones", "superior dragon bones"),
                "Buy the bone tier that gives the best current cost per Prayer XP for the chosen altar method.",
                "Use bones from planned combat/Slayer/PvM. Prefer safe dragons or other verified sources over Wilderness routes on risk-sensitive accounts.",
                "Use just-in-time combat-to-altar routes or other verified UIM-safe methods; never recommend banking bones.");
        add("dragon-bones-wilderness", tokens("dragon bones"),
                "Green dragons can be fast but are a Wilderness option and should only be compared when risk is enabled.",
                "Green dragons are optional Wilderness sourcing; safer blue/other dragon routes should be preferred on Hardcore.",
                "Wilderness dragon routes require explicit risk acceptance and a UIM death/storage safety check before recommendation.",
                true, RiskLevel.HIGH);
    }

    /** Broad, qualitative routes for common deterministic planning inputs. */
    private void commonProgressionFamilies()
    {
        family("compost", "make or buy it near the Farming route", "compost", "supercompost", "ultracompost");
        family("allotment-seeds", "use seed shops, contracts, pickpocketing, or verified drops", "potato seed", "onion seed", "cabbage seed", "tomato seed", "sweetcorn seed", "watermelon seed", "snape grass seed");
        family("tree-seeds", "use bird nests, contracts, seed packs, or verified PvM drops", "acorn", "willow seed", "maple seed", "yew seed", "magic seed");
        family("fruit-tree-seeds", "use bird nests, contracts, seed packs, or verified PvM drops", "apple tree seed", "banana tree seed", "orange tree seed", "curry tree seed", "pineapple seed", "papaya tree seed", "palm tree seed", "dragonfruit tree seed");
        family("eye-of-newt", "buy from a verified Herblore shop or obtain from a known spawn", "eye of newt");
        family("limpwurt-root", "farm limpwurts or use verified monster drops", "limpwurt root");
        family("white-berries", "farm bushes or use a verified safe spawn/drop source", "white berries");
        family("mort-myres", "gather in Mort Myre only after the required Morytania access and blessing are verified", "mort myre fungus", "mort myre stem", "mort myre pear");
        family("potato-cactus", "farm cactus patches or use verified reward/drop sources", "potato cactus");
        family("zammy-wine", "use a verified telegrab, temple, or PvM source without assuming a dangerous route", "wine of zamorak");
        family("blue-dragon-scale", "collect scales only from a verified reachable blue-dragon area", "blue dragon scale", "ground blue dragon scale");
        family("unfinished-potions", "combine the verified clean herb with a vial of water", "potion (unf)", "unfinished potion");
        family("finished-potions", "make from verified herbs and secondaries; mains may compare buying the finished potion", "prayer potion", "super restore", "antipoison", "stamina potion", "ranging potion", "super combat potion", "sanfew serum");
        family("vials", "buy vials or blow them from molten glass when that detour is worthwhile", "vial", "vial of water");
        family("hides", "obtain from a safe verified creature source before tanning", "cowhide", "snake hide", "yak hide");
        family("leather", "tan the matching hide at a verified tanner; mains may compare buying it", "leather", "hard leather");
        family("dragon-leather", "tan self-sourced dragonhides or buy the exact leather on a Main", "green dragon leather", "blue dragon leather", "red dragon leather", "black dragon leather");
        family("dragonhides", "use a safe verified dragon source; Wilderness dragons require explicit risk acceptance", "green dragonhide", "blue dragonhide", "red dragonhide", "black dragonhide");
        family("jewellery", "craft from the matching bar, gem, and mould or buy on a Main", "sapphire ring", "emerald ring", "ruby ring", "diamond ring", "dragonstone ring", "sapphire necklace", "emerald necklace", "ruby necklace", "diamond necklace");
        family("charged-orbs", "charge an unpowered orb at the verified elemental obelisk", "air orb", "water orb", "earth orb", "fire orb");
        family("battlestaves", "buy from verified staff shops/rewards or attach a charged orb for the elemental staff", "battlestaff", "air battlestaff", "water battlestaff", "earth battlestaff", "fire battlestaff");
        family("clay", "mine clay and add water, or buy the exact form when account mode permits", "clay", "soft clay");
        family("wool", "shear sheep and spin wool, or use a verified shop", "wool", "ball of wool");
        family("sand", "collect or mine buckets of sand using a verified route", "bucket of sand");
        family("seaweed", "gather/farm seaweed and cook ordinary seaweed when soda ash is required", "seaweed", "giant seaweed", "soda ash");
        family("arrows", "fletch shafts, feathers, and matching arrowheads or buy compatible ammunition", "bronze arrow", "iron arrow", "steel arrow", "mithril arrow", "adamant arrow", "rune arrow", "amethyst arrow");
        family("bolts", "combine matching unfinished bolts, feathers, and tips where applicable", "bronze bolts", "iron bolts", "steel bolts", "mithril bolts", "adamant bolts", "runite bolts", "broad bolts");
        family("darts", "attach feathers to matching dart tips after the Smithing unlock is available", "bronze dart", "iron dart", "steel dart", "mithril dart", "adamant dart", "rune dart", "amethyst dart");
        family("javelins", "attach matching heads to javelin shafts or use verified ammunition rewards", "bronze javelin", "iron javelin", "steel javelin", "mithril javelin", "adamant javelin", "rune javelin");
        family("bow-components", "cut the matching logs and spin or source bow strings", "shortbow (u)", "longbow (u)", "bow string");
        family("crossbow-components", "craft matching stocks and limbs, then add a crossbow string", "crossbow stock", "crossbow limb", "crossbow string", "crossbow (u)");
        family("raw-meat", "hunt or kill a safe verified creature that provides the exact meat", "raw beef", "raw chicken", "raw rat meat", "raw bear meat", "raw chompy", "raw yak meat");
        family("hunter-food", "hunt the unlocked creature and cook the meat using its verified recipe", "antelope meat", "dashing kebbit meat", "pyre fox meat");
        family("ashes", "obtain the required ash tier from a verified demon or fire source", "ashes", "fiendish ashes", "vile ashes", "malicious ashes", "abyssal ashes", "infernal ashes");
        family("teleport-jewellery", "craft and enchant the matching jewellery or buy charged variants on a Main", "games necklace", "dueling ring", "ring of wealth", "glory", "skills necklace", "combat bracelet", "slayer ring");
        family("teleport-tablets", "make tablets in an observed suitable POH or buy them on a Main", "teleport to house", "varrock teleport", "falador teleport", "camelot teleport", "ardougne teleport");
        family("construction-stone", "buy from the verified Construction supplier only when the target build justifies the cost", "limestone brick", "marble block", "gold leaf", "magic stone");
        family("quest-tools", "obtain from a nearby verified tool shop or spawn", "rope", "spade", "hammer", "chisel", "tinderbox", "knife", "bucket", "pot");
        family("poison-protection", "make, buy, or obtain the exact antipoison tier before the dangerous route", "antipoison", "antidote", "anti-venom");
    }

    private void family(String id, String route, String... itemTokens)
    {
        add(id, tokens(itemTokens),
                "Buy the exact requirement when current price and time saved justify it; otherwise " + route + ".",
                "Self-source the exact requirement: " + route + ".",
                "Use a task-sized, immediately consumable route: " + route
                        + "; do not assume conventional bank storage.");
    }

    private void add(String id, List<String> tokens,
            String main, String iron, String uim)
    {
        add(id, tokens, main, iron, uim, false, RiskLevel.NONE);
    }

    private void add(String id, List<String> tokens,
            String main, String iron, String uim,
            boolean wilderness, RiskLevel risk)
    {
        sources.add(new ResourceSourceDefinition(id, tokens,
                main, iron, uim, wilderness, risk));
    }

    private static List<String> tokens(String... values)
    {
        return Arrays.asList(values);
    }

    private static boolean containsPhrase(String value, String phrase)
    {
        if (value.equals(phrase)) return true;
        String paddedValue = " " + value + " ";
        String paddedPhrase = " " + phrase + " ";
        return paddedValue.contains(paddedPhrase);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^a-z0-9']+", " ")
                .trim();
    }
}
