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
                "Ruins of Unkah", "Use the harpoon-fishing loop; ropes, buckets and hammers are available in the encounter area."));
        add(profile("guardians-of-the-rift", all(item("Pickaxe", axes()),
                        item("Chisel")), "Temple of the Eye",
                "Mine guardian fragments, craft essence, then charge and enter the best legal altar available."));
        add(profile("motherlode-mine", item("Pickaxe", axes()),
                "Motherlode Mine beneath Falador",
                "Mine pay-dirt, clean it through the hopper, and collect the sack before it reaches capacity."));
        // A generic unlock does not prove that the client has observed a
        // currently active star and its location. Keep Shooting Stars in the
        // broader catalog, but do not promote it to Ready from tool ownership.
        add(profile("forestry", item("Axe", "Bronze axe", "Iron axe",
                        "Steel axe", "Black axe", "Mithril axe", "Adamant axe",
                        "Rune axe", "Dragon axe", "Infernal axe", "Crystal axe"),
                "A Forestry-enabled tree area",
                "Cut the selected tree and join nearby Forestry events when they are useful."));
        add(profile("vale-totems", all(item("Knife", "Fletching knife"),
                        any(item("Axe", "Bronze axe", "Iron axe", "Steel axe",
                                        "Black axe", "Mithril axe", "Adamant axe",
                                        "Rune axe", "Dragon axe", "Infernal axe", "Crystal axe"),
                                item("Logs", "Oak logs", "Willow logs", "Maple logs",
                                        "Yew logs", "Magic logs", "Redwood logs"))),
                "Auburn Valley", "Build, carve and decorate a matching totem, then claim the ent offerings."));
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
            ItemRequirementExpression items, String location, String guidance)
    {
        return new MinigameSetupProfile(id, items, location, guidance);
    }

    private static ItemRequirementExpression item(String name,
            String... substitutes)
    {
        return ItemRequirementExpression.item(name, 1,
                ItemRequirementScope.IMMEDIATELY_USABLE, substitutes);
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
