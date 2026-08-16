package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * Stable money/resource methods. Volatile GP/hour is intentionally zero until
 * a current price/rate service supplies it; Strategist ranks by suitability,
 * account mode and readiness rather than shipping stale profit claims.
 */
@Singleton
public class MoneyMakingCatalog
{
    private final List<MoneyMakingMethod> methods = new ArrayList<>();

    public MoneyMakingCatalog()
    {
        m("f2p_high_alch", "F2P High Level Alchemy", false, false, true,
                "55 Magic", "Nature runes", "Verified profitable/safe alch item");
        m("f2p_ogress", "Ogress combat drops", false, false, false,
                "Corsair Cove access", "Safe combat setup");
        m("f2p_runite", "Runite mining", false, false, true,
                "85 Mining", "Rune rocks route");
        m("f2p_crafting", "F2P processing/crafting margin", false, false, true,
                "Current GE margins must be verified");

        m("herb_runs", "Herb runs", true, false, false,
                "Farming level", "Seeds", "Reachable herb patches", "Tools/compost");
        m("birdhouses", "Birdhouse runs", true, false, false,
                "Bone Voyage", "Hunter/Crafting requirements", "Logs", "Seeds", "Clockworks");
        m("slayer", "Slayer and valuable task drops", true, false, false,
                "Current Slayer assignment", "Task-appropriate combat setup");
        m("barrows", "Barrows", true, false, false,
                "Barrows-ready combat setup", "Prayer/food", "Morytania access");
        m("perilous_moons", "Moons of Peril", true, false, false,
                "Perilous Moons access", "Midgame melee setup");
        m("vorkath", "Vorkath", true, false, false,
                "Dragon Slayer II", "Dragonfire protection", "Vorkath-ready ranged/melee setup");
        m("zulrah", "Zulrah", true, false, false,
                "Regicide", "Zulrah-ready ranged/magic setup");
        m("muspah", "Phantom Muspah", true, false, false,
                "Secrets of the North", "Muspah-ready ranged/magic setup");
        m("gauntlet", "The Gauntlet", true, false, false,
                "Song of the Elves", "Prifddinas access");
        m("corrupted_gauntlet", "Corrupted Gauntlet", true, false, false,
                "Song of the Elves", "Strong Gauntlet mechanics");
        m("raids", "Raids uniques and common loot", true, false, false,
                "Raid-specific access", "Three-style/role readiness", "Mechanics knowledge");
        m("yama", "Yama", true, false, false,
                "A Kingdom Divided", "High-level multi-style setup", "Mechanics readiness");
        m("revs", "Revenants", true, true, false,
                "Wilderness permission", "Escape plan", "Low-risk carried value");
        m("wildy_bosses", "Wilderness bosses", true, true, false,
                "Wilderness permission", "Escape plan", "Boss-ready setup");
        m("pickpocketing", "High-level pickpocketing", true, false, false,
                "Appropriate Thieving level", "Food/healing plan");
        m("runecraft_runes", "Useful/profitable Runecraft", true, false, false,
                "Appropriate Runecraft level", "Verified rune route", "Essence");
        m("smithing_processing", "Smithing/Blast Furnace processing", true, false, true,
                "Current ore/bar margin", "Blast Furnace requirements when applicable");
        m("crafting_processing", "Crafting processing margins", true, false, true,
                "Current input/output prices", "Crafting level");
        m("fletching_processing", "Fletching processing margins", true, false, true,
                "Current input/output prices", "Fletching level");
    }

    public List<MoneyMakingMethod> all()
    {
        return Collections.unmodifiableList(methods);
    }

    private void m(String id, String name, boolean members, boolean wilderness,
            boolean tradeDependent, String... requirements)
    {
        methods.add(new MoneyMakingMethod(
                id, name, 0L, RecommendationConfidence.CHECK_NEEDED,
                members, wilderness, tradeDependent,
                Arrays.asList(requirements),
                tradeDependent
                        ? "Use only after current prices, volume, tax and acquisition cost are verified."
                        : "Value comes from coins, useful supplies, drops or account progression; current profitability can be layered in separately."));
    }
}
