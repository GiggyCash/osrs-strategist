package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Small deterministic recipe graph; unknown items remain leaf checks. */
@Singleton
public class ResourceDependencyCatalog
{
    private final Map<Integer, ResourceDependencyDefinition> definitions;

    public ResourceDependencyCatalog()
    {
        this(seed());
    }

    ResourceDependencyCatalog(List<ResourceDependencyDefinition> values)
    {
        Map<Integer, ResourceDependencyDefinition> result = new LinkedHashMap<>();
        if (values != null)
            for (ResourceDependencyDefinition value : values)
                if (value != null) result.put(value.getItemId(), value);
        definitions = Collections.unmodifiableMap(result);
    }

    public ResourceDependencyDefinition forItem(int itemId)
    {
        return definitions.get(itemId);
    }

    private static List<ResourceDependencyDefinition> seed()
    {
        List<ResourceDependencyDefinition> values = new ArrayList<>();
        values.add(new ResourceDependencyDefinition(ItemID.MCANNONBALL,
                "Smith the required cannonballs from steel bars after Dwarf Cannon.",
                35, 4, java.util.Arrays.asList(
                DependencyRequirement.quest("Dwarf Cannon"),
                DependencyRequirement.skill(Skill.SMITHING, 35),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.STEEL_BAR, "Steel bar", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.STEEL_BAR,
                "Smelt steel bars from iron ore and coal at a verified furnace.",
                20, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.SMITHING, 30),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.IRON_ORE, "Iron ore", 1)),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.COAL, "Coal", 2)))));
        values.add(new ResourceDependencyDefinition(ItemID.MOLTEN_GLASS,
                "Make molten glass from a bucket of sand and soda ash at a furnace.",
                18, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.CRAFTING, 1),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.BUCKET_SAND, "Bucket of sand", 1)),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.SODA_ASH, "Soda ash", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.BRONZE_BAR,
                "Smelt a bronze bar from one copper ore and one tin ore at a furnace.",
                8, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.SMITHING, 1),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.COPPER_ORE, "Copper ore", 1)),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.TIN_ORE, "Tin ore", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.GOLD_BAR,
                "Smelt the gold ore at a furnace.", 12, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.SMITHING, 40),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.GOLD_ORE, "Gold ore", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.SODA_ASH,
                "Cook seaweed on a range or fire to make soda ash.",
                8, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.COOKING, 1),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.SEAWEED, "Seaweed", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.BOW_STRING,
                "Spin flax into bow string at a spinning wheel.",
                10, java.util.Arrays.asList(
                DependencyRequirement.skill(Skill.CRAFTING, 10),
                DependencyRequirement.resource(new ResourceNeed(
                        ItemID.FLAX, "Flax", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.IRON_BAR,
                "Smelt iron ore at a verified furnace.", 10,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.SMITHING, 15),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.IRON_ORE, "Iron ore", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.SILVER_BAR,
                "Smelt silver ore at a verified furnace.", 10,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.SMITHING, 20),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.SILVER_ORE, "Silver ore", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.MITHRIL_BAR,
                "Smelt mithril ore with four coal at a verified furnace.", 25,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.SMITHING, 50),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.MITHRIL_ORE, "Mithril ore", 1)),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.COAL, "Coal", 4)))));
        values.add(new ResourceDependencyDefinition(ItemID.ADAMANTITE_BAR,
                "Smelt adamantite ore with six coal at a verified furnace.", 30,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.SMITHING, 70),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.ADAMANTITE_ORE, "Adamantite ore", 1)),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.COAL, "Coal", 6)))));
        values.add(new ResourceDependencyDefinition(ItemID.RUNITE_BAR,
                "Smelt runite ore with eight coal at a verified furnace.", 35,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.SMITHING, 85),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.RUNITE_ORE, "Runite ore", 1)),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.COAL, "Coal", 8)))));
        values.add(new ResourceDependencyDefinition(ItemID.BALL_OF_WOOL,
                "Spin wool into a ball of wool at a spinning wheel.", 8,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.CRAFTING, 1),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.WOOL, "Wool", 1)))));
        values.add(new ResourceDependencyDefinition(ItemID.VIAL_EMPTY,
                "Use a glassblowing pipe on molten glass to make an empty vial.", 12,
                java.util.Arrays.asList(
                        DependencyRequirement.skill(Skill.CRAFTING, 33),
                        DependencyRequirement.gear("Glassblowing pipe"),
                        DependencyRequirement.resource(new ResourceNeed(
                                ItemID.MOLTEN_GLASS, "Molten glass", 1)))));
        addAmmunition(values);
        addCrafting(values);
        return values;
    }

    public int size()
    {
        return definitions.size();
    }

    private static void addAmmunition(List<ResourceDependencyDefinition> values)
    {
        values.add(recipe(ItemID.ARROW_SHAFT, "Cut normal logs into arrow shafts.",
                15, Skill.FLETCHING, 1, ItemID.LOGS, "Logs", 1));
        values.add(recipe(ItemID.HEADLESS_ARROW,
                "Attach feathers to arrow shafts to make headless arrows.",
                1, Skill.FLETCHING, 1, ItemID.ARROW_SHAFT, "Arrow shaft", 1,
                ItemID.FEATHER, "Feather", 1));
        arrow(values, ItemID.BRONZE_ARROW, "Bronze", ItemID.BRONZE_ARROWHEADS, 1);
        arrow(values, ItemID.IRON_ARROW, "Iron", ItemID.IRON_ARROWHEADS, 15);
        arrow(values, ItemID.STEEL_ARROW, "Steel", ItemID.STEEL_ARROWHEADS, 30);
        arrow(values, ItemID.MITHRIL_ARROW, "Mithril", ItemID.MITHRIL_ARROWHEADS, 45);
        arrow(values, ItemID.ADAMANT_ARROW, "Adamant", ItemID.ADAMANT_ARROWHEADS, 60);
        arrow(values, ItemID.RUNE_ARROW, "Rune", ItemID.RUNE_ARROWHEADS, 75);
        dart(values, ItemID.BRONZE_DART, "Bronze", ItemID.BRONZE_DART_TIP, 1);
        dart(values, ItemID.IRON_DART, "Iron", ItemID.IRON_DART_TIP, 22);
        dart(values, ItemID.STEEL_DART, "Steel", ItemID.STEEL_DART_TIP, 37);
        dart(values, ItemID.MITHRIL_DART, "Mithril", ItemID.MITHRIL_DART_TIP, 52);
        dart(values, ItemID.ADAMANT_DART, "Adamant", ItemID.ADAMANT_DART_TIP, 67);
        dart(values, ItemID.RUNE_DART, "Rune", ItemID.RUNE_DART_TIP, 81);
    }

    private static void addCrafting(List<ResourceDependencyDefinition> values)
    {
        leather(values, ItemID.LEATHER_GLOVES, "Leather gloves", 1);
        leather(values, ItemID.LEATHER_BOOTS, "Leather boots", 7);
        leather(values, ItemID.LEATHER_COWL, "Leather cowl", 9);
        leather(values, ItemID.LEATHER_VAMBRACES, "Leather vambraces", 11);
        leather(values, ItemID.LEATHER_ARMOUR, "Leather body", 14);
        values.add(recipe(ItemID.HARDLEATHER_BODY, "Sew a hardleather body.",
                1, Skill.CRAFTING, 28, ItemID.HARD_LEATHER, "Hard leather", 1));
        battlestaff(values, ItemID.AIR_BATTLESTAFF, "Air", ItemID.AIR_ORB, 66);
        battlestaff(values, ItemID.WATER_BATTLESTAFF, "Water", ItemID.WATER_ORB, 54);
        battlestaff(values, ItemID.EARTH_BATTLESTAFF, "Earth", ItemID.EARTH_ORB, 58);
        battlestaff(values, ItemID.FIRE_BATTLESTAFF, "Fire", ItemID.FIRE_ORB, 62);
        glass(values, ItemID.BEER_GLASS, "beer glass", 1);
        glass(values, ItemID.CANDLE_LANTERN_EMPTY, "empty candle lantern", 4);
        glass(values, ItemID.FISHBOWL_EMPTY, "empty fishbowl", 42);
    }

    private static void arrow(List<ResourceDependencyDefinition> values, int output,
            String metal, int heads, int level)
    {
        values.add(recipe(output, "Attach " + metal.toLowerCase()
                + " arrowheads to headless arrows.", 1, Skill.FLETCHING, level,
                ItemID.HEADLESS_ARROW, "Headless arrow", 1,
                heads, metal + " arrowhead", 1));
    }

    private static void dart(List<ResourceDependencyDefinition> values, int output,
            String metal, int tips, int level)
    {
        values.add(recipe(output, "Attach feathers to " + metal.toLowerCase()
                + " dart tips.", 1, Skill.FLETCHING, level,
                tips, metal + " dart tip", 1, ItemID.FEATHER, "Feather", 1));
    }

    private static void leather(List<ResourceDependencyDefinition> values,
            int output, String name, int level)
    {
        values.add(recipe(output, "Sew " + name.toLowerCase() + ".", 1,
                Skill.CRAFTING, level, ItemID.LEATHER, "Leather", 1));
    }

    private static void battlestaff(List<ResourceDependencyDefinition> values,
            int output, String element, int orb, int level)
    {
        values.add(recipe(output, "Attach the charged " + element.toLowerCase()
                + " orb to a battlestaff.", 1, Skill.CRAFTING, level,
                ItemID.BATTLESTAFF, "Battlestaff", 1,
                orb, element + " orb", 1));
    }

    private static void glass(List<ResourceDependencyDefinition> values,
            int output, String name, int level)
    {
        values.add(recipe(output, "Blow molten glass into an " + name + ".", 1,
                Skill.CRAFTING, level, ItemID.MOLTEN_GLASS, "Molten glass", 1));
    }

    private static ResourceDependencyDefinition recipe(int output, String action,
            int yield, Skill skill, int level, Object... resources)
    {
        List<DependencyRequirement> requirements = new ArrayList<>();
        requirements.add(DependencyRequirement.skill(skill, level));
        for (int index = 0; index + 2 < resources.length; index += 3)
            requirements.add(DependencyRequirement.resource(new ResourceNeed(
                    (Integer) resources[index], (String) resources[index + 1],
                    (Integer) resources[index + 2])));
        return new ResourceDependencyDefinition(output, action, 8, yield, requirements);
    }
}
