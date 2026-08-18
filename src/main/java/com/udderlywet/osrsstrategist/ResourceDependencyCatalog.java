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
                35, java.util.Arrays.asList(
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
        return values;
    }
}
