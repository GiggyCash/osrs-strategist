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
