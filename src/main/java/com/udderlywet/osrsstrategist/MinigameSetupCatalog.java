package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

/** Verified minimum setups for low-risk skilling activities. */
@Singleton
public final class MinigameSetupCatalog
{
    private final Map<String, MinigameSetupProfile> profiles = new LinkedHashMap<>();

    public MinigameSetupCatalog()
    {
        add(profile("tempoross", item("Harpoon", "Barb-tail harpoon",
                        "Dragon harpoon", "Infernal harpoon", "Crystal harpoon"),
                "Ruins of Unkah", "An observed harpoon; rope, buckets and hammers are available on the island.",
                "Fish harpoonfish, cook the catch, load both cannons, and tether during every wave until Tempoross is subdued."));
        add(profile("guardians-of-the-rift", all(item("Pickaxe", axes()),
                        item("Chisel")), "Temple of the Eye", "An observed pickaxe and chisel.",
                "Mine guardian fragments, craft essence, then charge and enter the best legal altar available."));
        add(profile("motherlode-mine", item("Pickaxe", axes()),
                "Motherlode Mine beneath Falador", "An observed pickaxe.",
                "Mine pay-dirt, clean it through the hopper, and collect the sack before it reaches capacity."));
        // A generic unlock does not prove that the client has observed a
        // currently active star and its location. Keep Shooting Stars in the
        // broader catalog, but do not promote it to Ready from tool ownership.
        add(profile("forestry", item("Axe", "Bronze axe", "Iron axe",
                        "Steel axe", "Black axe", "Mithril axe", "Adamant axe",
                        "Rune axe", "Dragon axe", "Infernal axe", "Crystal axe"),
                "A level-appropriate Forestry tree cluster", "An observed usable axe; bring a Forestry kit when owned.",
                "Cut the named tree, bank each inventory, and complete spawned Forestry events."));
        add(profile("vale-totems", all(item("Knife", "Fletching knife"),
                        any(item("Axe", "Bronze axe", "Iron axe", "Steel axe",
                                        "Black axe", "Mithril axe", "Adamant axe",
                                        "Rune axe", "Dragon axe", "Infernal axe", "Crystal axe"),
                                item("Logs", "Oak logs", "Willow logs", "Maple logs",
                                        "Yew logs", "Magic logs", "Redwood logs"))),
                "Auburn Valley", "An observed knife plus an axe or usable logs.",
                "Build, carve and decorate a matching totem, then claim the ent offerings."));

        add(profile("wintertodt", all(
                        itemClass(ItemRequirementClass.AXE),
                        item("Tinderbox", "Bruma torch"),
                        ItemRequirementExpression.checkNeeded(
                                "Equip four verified warm-clothing pieces and carry food that heals at least 4 Hitpoints, or an exact rejuvenation-potion setup")),
                "Wintertodt Camp in northern Great Kourend",
                "A usable axe, tinderbox or bruma torch, four warm items, and warmth-restoring food or rejuvenation potions. Knives and hammers are available inside.",
                "Chop bruma roots, feed and repair braziers, keep warmth above zero, and earn at least 500 personal points before the round ends."));
        add(profile("giants-foundry", itemAtLeast("Iron bar", 28,
                        "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar"),
                "Giants' Foundry beneath Kovac's workshop, east of Al Kharid",
                "At least 28 observed eligible bars; adjacent-metal alloys are evaluated by the skilling method when both supplies are known.",
                "Take Kovac's commission, choose the highest-scoring owned moulds, load exactly 28 bars' worth into the crucible, pour the preform, and keep each station inside its temperature band."));
        add(profile("mahogany-homes", all(item("Hammer"), item("Saw", "Amy's saw"),
                        ItemRequirementExpression.checkNeeded(
                                "Take a live Mahogany Homes contract, then observe its tier and exact plank and steel-bar shortfall")),
                "Amy immediately south of Falador Park",
                "A hammer, saw, the plank type for the live contract tier, any required steel bar, and verified travel to the named client.",
                "Take the highest useful contract tier whose materials are observed, repair every marked hotspot at the named client's house, speak to the client, and return for another contract."));
        add(profile("tithe-farm", all(item("Spade"), item("Seed dibber"),
                        any(item("Gricoller's can"), itemAtLeast("Watering can(8)", 8,
                                "Watering can(7)", "Watering can(6)", "Watering can(5)",
                                "Watering can(4)", "Watering can(3)", "Watering can(2)",
                                "Watering can(1)"))),
                "Tithe Farm in Hosidius",
                "A spade, seed dibber, and either Gricoller's can or eight filled watering cans.",
                "Take the seed for the observed Farming level, plant and immediately water a manageable plot cycle, revisit every watering stage, then harvest and deposit the fruit."));
    }

    public MinigameSetupProfile forActivity(String id)
    {
        return id == null ? null : profiles.get(id);
    }

    public int size() { return profiles.size(); }

    private void add(MinigameSetupProfile profile)
    {
        profiles.put(profile.getActivityId(), profile);
    }

    private static MinigameSetupProfile profile(String id,
            ItemRequirementExpression items, String location, String supplies,
            String guidance)
    {
        return new MinigameSetupProfile(id, items, location, supplies, guidance);
    }

    private static ItemRequirementExpression item(String name,
            String... substitutes)
    {
        return ItemRequirementExpression.item(name, 1,
                ItemRequirementScope.IMMEDIATELY_USABLE, substitutes);
    }

    private static ItemRequirementExpression itemAtLeast(String name,
            int quantity, String... substitutes)
    {
        return ItemRequirementExpression.itemAtLeast(name, quantity,
                ItemRequirementScope.IMMEDIATELY_USABLE, substitutes);
    }

    private static ItemRequirementExpression itemClass(
            ItemRequirementClass itemClass)
    {
        return ItemRequirementExpression.itemClass(itemClass, 1,
                ItemRequirementScope.CARRIED_OR_EQUIPPED);
    }

    private static ItemRequirementExpression all(ItemRequirementExpression... values)
    {
        return ItemRequirementExpression.allOf(values);
    }

    private static ItemRequirementExpression any(ItemRequirementExpression... values)
    {
        return ItemRequirementExpression.anyOf(values);
    }

    private static String[] axes()
    {
        return new String[] {"Bronze pickaxe", "Iron pickaxe", "Steel pickaxe",
                "Black pickaxe", "Mithril pickaxe", "Adamant pickaxe",
                "Rune pickaxe", "Dragon pickaxe", "Infernal pickaxe",
                "Crystal pickaxe", "Gilded pickaxe", "3rd age pickaxe"};
    }
}
