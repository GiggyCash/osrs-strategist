package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Typed readiness profiles for common methods whose item requirements can be
 * proven safely from live RuneLite item names.
 *
 * <p>This is intentionally conservative. A profile is only added when the item
 * relationship is stable and straightforward. Complex route requirements stay
 * as explicit Check First rows until a dedicated evaluator exists. A false
 * negative is inconvenient; a false Ready state damages trust.</p>
 */
@Singleton
public class MethodReadinessCatalog
{
    private final Map<String, MethodReadinessProfile> profiles = new HashMap<>();

    public MethodReadinessCatalog()
    {
        cooking();
        smithing();
        crafting();
        fletching();
        firemaking();
        herblore();
        construction();
        prayerAndMagic();
        fishing();
        combatAndSlayer();
        mining();
    }

    public MethodReadinessProfile forMethod(String methodId)
    {
        return methodId == null ? null : profiles.get(methodId);
    }

    public boolean supports(String methodId)
    {
        return forMethod(methodId) != null;
    }

    public int size()
    {
        return profiles.size();
    }

    private void cooking()
    {
        profile("cooking_wines",
                items(
                        exact("resource:grapes", "Grapes", 1, "Grapes"),
                        exact("resource:jug-water", "Jug of water", 1, "Jug of water")),
                checks());

        profile("cooking_karambwan_1t",
                items(exact("resource:raw-karambwan", "Raw karambwan", 1,
                        "Raw karambwan")),
                checks("Verified cooking range/location for the intended one-tick loop"));
    }

    private void smithing()
    {
        profile("smithing_cannonballs",
                items(
                        exact("resource:steel-bar", "Steel bar", 1, "Steel bar"),
                        alternatives("resource:ammo-mould", "Ammo mould", 1,
                                ItemNameRule.exact("Ammo mould"),
                                ItemNameRule.exact("Double ammo mould"))),
                checks("Dwarf Cannon completion/access"));

        profile("smithing_blast_furnace_gold",
                items(exact("resource:gold-ore", "Gold ore", 1, "Gold ore")),
                checks("Blast Furnace access and safe travel/payment setup"));

        profile("smithing_dart_tips",
                items(exact("resource:hammer", "Hammer", 1, "Hammer")),
                checks("A bar tier that the current Smithing level can turn into dart tips"));
    }

    private void crafting()
    {
        profile("crafting_gems",
                items(
                        exact("resource:chisel", "Chisel", 1, "Chisel"),
                        alternatives("resource:usable-uncut-gem", "Usable uncut gem", 1,
                                ItemNameRule.exactAt(Skill.CRAFTING, 20, "Uncut sapphire"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 27, "Uncut emerald"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 34, "Uncut ruby"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 43, "Uncut diamond"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 55, "Uncut dragonstone"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 67, "Uncut onyx"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 89, "Uncut zenyte"))),
                checks());

        profile("crafting_dhide",
                items(
                        exact("resource:needle", "Needle", 1, "Needle"),
                        exact("resource:thread", "Thread", 1, "Thread"),
                        alternatives("resource:usable-dragon-leather", "Usable dragon leather", 3,
                                ItemNameRule.exactAt(Skill.CRAFTING, 63, "Green dragon leather"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 71, "Blue dragon leather"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 77, "Red dragon leather"),
                                ItemNameRule.exactAt(Skill.CRAFTING, 84, "Black dragon leather"))),
                checks());
    }

    private void fletching()
    {
        profile("fletching_arrow_shafts",
                items(
                        exact("resource:knife", "Knife", 1, "Knife"),
                        exact("resource:logs", "Logs", 1, "Logs")),
                checks());

        profile("fletching_bows",
                items(
                        exact("resource:knife", "Knife", 1, "Knife"),
                        fletchingLogs()),
                checks());
    }

    private void firemaking()
    {
        profile("firemaking_f2p_logs",
                items(
                        exact("resource:tinderbox", "Tinderbox", 1, "Tinderbox"),
                        firemakingLogs()),
                checks("A clear safe place to light the selected logs"));
    }

    private void herblore()
    {
        profile("herblore_prayer_potions",
                items(
                        herb("resource:ranarr", "Ranarr weed", "Ranarr weed", "Grimy ranarr weed"),
                        exact("resource:snape-grass", "Snape grass", 1, "Snape grass"),
                        exact("resource:vial-water", "Vial of water", 1, "Vial of water")),
                checks());

        profile("herblore_restores",
                items(
                        herb("resource:snapdragon", "Snapdragon", "Snapdragon", "Grimy snapdragon"),
                        exact("resource:red-spider-eggs", "Red spiders' eggs", 1,
                                "Red spiders' eggs"),
                        exact("resource:vial-water", "Vial of water", 1, "Vial of water")),
                checks());

        profile("herblore_brews",
                items(
                        herb("resource:toadflax", "Toadflax", "Toadflax", "Grimy toadflax"),
                        exact("resource:crushed-nest", "Crushed nest", 1, "Crushed nest"),
                        exact("resource:vial-water", "Vial of water", 1, "Vial of water")),
                checks());
    }

    private void construction()
    {
        profile("construction_oak_larders",
                items(
                        exact("resource:oak-plank", "Oak planks", 8, "Oak plank"),
                        exact("resource:hammer", "Hammer", 1, "Hammer"),
                        exact("resource:saw", "Saw", 1, "Saw")),
                checks("POH kitchen with an oak-larder build space"));

        profile("construction_oak_doors",
                items(
                        exact("resource:oak-plank", "Oak planks", 10, "Oak plank"),
                        exact("resource:hammer", "Hammer", 1, "Hammer"),
                        exact("resource:saw", "Saw", 1, "Saw")),
                checks("POH dungeon room with a dungeon-door build space"));

        profile("construction_mahogany_tables",
                items(
                        exact("resource:mahogany-plank", "Mahogany planks", 6, "Mahogany plank"),
                        exact("resource:hammer", "Hammer", 1, "Hammer"),
                        exact("resource:saw", "Saw", 1, "Saw")),
                checks("POH dining room with a table build space"));
    }

    private void prayerAndMagic()
    {
        profile("prayer_f2p_bones",
                items(anyBones("resource:bones", "Bones", 1)),
                checks());

        profile("prayer_gilded_altar",
                items(anyBones("resource:bones", "Bone supply", 1)),
                checks("Verified lit gilded altar route"));

        profile("magic_high_alch",
                items(exact("resource:nature-rune", "Nature rune", 1, "Nature rune")),
                checks("Fire-rune source or fire staff", "Verified safe alch item list"));
    }

    private void fishing()
    {
        profile("fishing_f2p_fly",
                items(
                        exact("resource:fly-rod", "Fly fishing rod", 1, "Fly fishing rod"),
                        exact("resource:feather", "Feathers", 1, "Feather")),
                checks("Reachable fly-fishing spot"));

        profile("fishing_barbarian",
                items(
                        exact("resource:barbarian-rod", "Barbarian rod", 1, "Barbarian rod"),
                        alternatives("resource:barbarian-bait", "Feathers or fishing bait", 1,
                                ItemNameRule.exact("Feather"),
                                ItemNameRule.exact("Fishing bait"))),
                checks("Barbarian Fishing training/access"));

        profile("fishing_3t_barb",
                items(
                        exact("resource:barbarian-rod", "Barbarian rod", 1, "Barbarian rod"),
                        alternatives("resource:barbarian-bait", "Feathers or fishing bait", 1,
                                ItemNameRule.exact("Feather"),
                                ItemNameRule.exact("Fishing bait"))),
                checks("Barbarian Fishing training/access",
                        "Verified tick-manipulation item setup"));

        profile("fishing_karambwan",
                items(
                        exact("resource:karambwan-vessel", "Karambwan vessel", 1,
                                "Karambwan vessel"),
                        exact("resource:karambwanji", "Raw karambwanji", 1,
                                "Raw karambwanji")),
                checks("Karambwan fishing access"));
    }

    private void combatAndSlayer()
    {
        profile("ranged_cannon_slayer",
                items(exact("resource:cannonball", "Cannonballs", 1, "Cannonball")),
                checks("Dwarf multicannon available/placed",
                        "Current task allows a cannon"));

        profile("slayer_cannon_tasks",
                items(exact("resource:cannonball", "Cannonballs", 1, "Cannonball")),
                checks("Dwarf multicannon available/placed",
                        "Current task allows a cannon"));
    }

    private void mining()
    {
        profile("mining_blast_mine",
                items(
                        exact("resource:dynamite", "Dynamite", 1, "Dynamite"),
                        exact("resource:tinderbox", "Tinderbox", 1, "Tinderbox")),
                checks("Blast Mine access and safe route"));
    }

    private NamedResourceRequirement fletchingLogs()
    {
        return alternatives("resource:fletching-logs", "Usable bow logs", 1,
                ItemNameRule.exactAt(Skill.FLETCHING, 5, "Logs"),
                ItemNameRule.exactAt(Skill.FLETCHING, 20, "Oak logs"),
                ItemNameRule.exactAt(Skill.FLETCHING, 35, "Willow logs"),
                ItemNameRule.exactAt(Skill.FLETCHING, 50, "Maple logs"),
                ItemNameRule.exactAt(Skill.FLETCHING, 65, "Yew logs"),
                ItemNameRule.exactAt(Skill.FLETCHING, 80, "Magic logs"));
    }

    private NamedResourceRequirement firemakingLogs()
    {
        return alternatives("resource:firemaking-logs", "Usable logs", 1,
                ItemNameRule.exactAt(Skill.FIREMAKING, 1, "Logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 15, "Oak logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 30, "Willow logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 35, "Teak logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 42, "Arctic pine logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 45, "Maple logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 50, "Mahogany logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 60, "Yew logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 75, "Magic logs"),
                ItemNameRule.exactAt(Skill.FIREMAKING, 90, "Redwood logs"));
    }

    private NamedResourceRequirement anyBones(
            String id, String label, int quantity)
    {
        return alternatives(id, label, quantity,
                ItemNameRule.exact("Bones"),
                ItemNameRule.suffix(" bones"));
    }

    private NamedResourceRequirement herb(
            String id,
            String label,
            String clean,
            String grimy)
    {
        return alternatives(id, label, 1,
                ItemNameRule.exact(clean),
                ItemNameRule.exact(grimy));
    }

    private NamedResourceRequirement exact(
            String id,
            String label,
            int quantity,
            String name)
    {
        return new NamedResourceRequirement(
                id, label, quantity, ItemNameRule.exact(name));
    }

    private NamedResourceRequirement alternatives(
            String id,
            String label,
            int quantity,
            ItemNameRule... rules)
    {
        return new NamedResourceRequirement(id, label, quantity, rules);
    }

    private void profile(
            String methodId,
            List<NamedResourceRequirement> items,
            List<String> otherChecks)
    {
        profiles.put(methodId,
                new MethodReadinessProfile(methodId, items, otherChecks));
    }

    private static List<NamedResourceRequirement> items(
            NamedResourceRequirement... values)
    {
        return values == null
                ? Collections.emptyList()
                : Arrays.asList(values);
    }

    private static List<String> checks(String... values)
    {
        return values == null
                ? Collections.emptyList()
                : Arrays.asList(values);
    }
}
