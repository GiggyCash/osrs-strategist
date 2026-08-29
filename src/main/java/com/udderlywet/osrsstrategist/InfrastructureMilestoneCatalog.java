package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Small audited infrastructure catalog. It intentionally contains only
 * milestones for which current requirements and local completion evidence are
 * both represented; it is not a generic Construction wish list.
 */
@Singleton
public final class InfrastructureMilestoneCatalog
{
    public static final String AUDITED_AT = "2026-08-29";
    public static final List<String> PROVENANCE_URLS =
            Collections.unmodifiableList(java.util.Arrays.asList(
                    "https://oldschool.runescape.wiki/w/Construction",
                    "https://oldschool.runescape.wiki/w/Costume_room",
                    "https://oldschool.runescape.wiki/w/Portal_chamber",
                    "https://oldschool.runescape.wiki/w/Portal_nexus",
                    "https://oldschool.runescape.wiki/w/Pool_space",
                    "https://oldschool.runescape.wiki/w/Achievement_gallery",
                    "https://oldschool.runescape.wiki/w/Fairy_rings",
                    "https://oldschool.runescape.wiki/w/Spirit_tree",
                    "https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management"));

    private final Map<String, InfrastructureMilestoneDefinition> milestones =
            new LinkedHashMap<>();

    public InfrastructureMilestoneCatalog()
    {
        add(definition("poh-access", "Player-owned house", true,
                Skill.CONSTRUCTION, 1, null, false, null,
                InfrastructureEvidenceKind.POH_ACCESS, null, null,
                benefits(InfrastructureBenefit.POH_PLATFORM,
                        StrategicPriority.MODERATE),
                "Buy a starter house from an Estate agent for 1,000 coins (or complete Daddy's Home), enter it once, and verify house access.",
                "https://oldschool.runescape.wiki/w/Construction"));
        add(definition("poh-costume-room", "POH Costume room", true,
                Skill.CONSTRUCTION, 42, null, false, "poh-access",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.COSTUME_ROOM, null,
                benefits(InfrastructureBenefit.POH_PLATFORM,
                        StrategicPriority.HIGH,
                        InfrastructureBenefit.STORAGE,
                        StrategicPriority.LOW),
                "In your own POH in building mode, build a Costume room for 50,000 coins.",
                "https://oldschool.runescape.wiki/w/Costume_room"));
        add(definition("poh-armour-case", "Oak armour case", true,
                Skill.CONSTRUCTION, 46, null, false, "poh-costume-room",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.ARMOUR_CASE, null,
                benefits(InfrastructureBenefit.INVENTORY_RELIEF, StrategicPriority.HIGH,
                        InfrastructureBenefit.STORAGE, StrategicPriority.HIGH,
                        InfrastructureBenefit.STORABLE_EQUIPMENT, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.MODERATE),
                "In your Costume room, use a hammer, saw, and 3 oak planks to build an oak armour case.",
                "https://oldschool.runescape.wiki/w/Oak_armour_case"));
        add(definition("poh-portal-chamber", "Configured POH portal", true,
                skills(Skill.CONSTRUCTION, 50, Skill.MAGIC, 25),
                Collections.emptyMap(), "poh-access",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.PERMANENT_PORTAL, null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "Build a Portal chamber for 100,000 coins, a teleport focus with 2 limestone bricks, and a teak portal with 3 teak planks; direct it to Varrock with 300 air runes, 100 fire runes, and 100 law runes.",
                "https://oldschool.runescape.wiki/w/Portal_chamber"));
        add(definition("poh-superior-garden", "POH superior garden", true,
                Skill.CONSTRUCTION, 65, null, false, "poh-access",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.SUPERIOR_GARDEN, null,
                benefits(InfrastructureBenefit.POH_PLATFORM, StrategicPriority.HIGH,
                        InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.MODERATE,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.MODERATE),
                "In your own POH in building mode, build a Superior garden for 75,000 coins.",
                "https://oldschool.runescape.wiki/w/Construction"));
        add(definition("poh-restoration-pool", "Restoration pool", true,
                Skill.CONSTRUCTION, 65, null, false, "poh-superior-garden",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.RESTORATION_POOL, null,
                benefits(InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "In your Superior garden, use a hammer and saw to build a restoration pool with 5 limestone bricks, 5 buckets of water, 1,000 soul runes, and 1,000 body runes.",
                "https://oldschool.runescape.wiki/w/Pool_space"));
        add(definition("poh-portal-nexus", "Marble portal nexus", true,
                Skill.CONSTRUCTION, 72, null, false, "poh-access",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.PORTAL_NEXUS, null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "Build a Portal nexus room for 200,000 coins, then use a hammer, saw, and 4 marble blocks to build its four-destination marble portal nexus. Add destinations only when their 1,000-cast rune cost is worth the permanent slot.",
                "https://oldschool.runescape.wiki/w/Portal_nexus"));
        add(definition("poh-spirit-tree", "POH spirit tree", true,
                skills(Skill.CONSTRUCTION, 75, Skill.FARMING, 83),
                quests("Tree Gnome Village", false), "poh-superior-garden",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.SPIRIT_TREE, null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.HIGH),
                "In your Superior garden, use a filled watering can and a self-grown spirit sapling to plant the POH spirit tree.",
                "https://oldschool.runescape.wiki/w/Spirit_tree"));
        add(definition("poh-basic-jewellery-box", "Basic jewellery box", true,
                Skill.CONSTRUCTION, 81, null, false, "poh-access",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.JEWELLERY_BOX, null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "In an Achievement Gallery, use a hammer and saw to build a basic jewellery box with 1 bolt of cloth, 1 steel bar, 3 games necklaces(8), and 3 rings of dueling(8).",
                "https://oldschool.runescape.wiki/w/Achievement_gallery"));
        add(definition("poh-fairy-ring", "POH fairy ring", true,
                Skill.CONSTRUCTION, 85, "Fairytale II - Cure a Queen", false,
                "poh-superior-garden",
                InfrastructureEvidenceKind.POH_FURNITURE,
                LivePohStateReader.FAIRY_RING, null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.HIGH),
                "In your Superior garden, use a filled watering can, 10 unnoted mushrooms, and the fairy enchantment bought after Fairytale II to plant a fairy ring.",
                "https://oldschool.runescape.wiki/w/Fairy_ring_(Construction)"));
        add(definition("fairy-ring-network", "Fairy ring network", true,
                null, 0, "Fairytale II - Cure a Queen", true, null,
                InfrastructureEvidenceKind.TRANSPORT_ROUTE, "fairy-rings", null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.MODERATE,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "Advance Fairytale II until the Fairy Godfather grants permission, carry a dramen or lunar staff unless staff-free access is verified, and verify the route before relying on it.",
                "https://oldschool.runescape.wiki/w/Fairy_rings"));
        add(definition("spirit-tree-access", "Spirit tree access", true,
                null, 0, "Tree Gnome Village", false, null,
                InfrastructureEvidenceKind.TRANSPORT_ROUTE, "spirit-trees", null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.MODERATE),
                "Complete Tree Gnome Village for initial spirit-tree access; The Grand Tree separately expands bidirectional network access. Verify the needed route before relying on it.",
                "https://oldschool.runescape.wiki/w/Spirit_tree"));
    }

    public InfrastructureMilestoneDefinition get(String id)
    {
        return id == null ? null : milestones.get(id);
    }

    public List<InfrastructureMilestoneDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(milestones.values()));
    }

    private void add(InfrastructureMilestoneDefinition definition)
    {
        if (milestones.put(definition.getId(), definition) != null)
            throw new IllegalStateException(
                    "Duplicate infrastructure milestone " + definition.getId());
    }

    private static InfrastructureMilestoneDefinition definition(String id,
            String name, boolean membersOnly, Skill skill, int level,
            String quest, boolean questStartSuffices, String prerequisite,
            InfrastructureEvidenceKind evidence, String evidenceKey,
            StorageCapability storage,
            Map<InfrastructureBenefit, StrategicPriority> benefits,
            String action, String source)
    {
        return new InfrastructureMilestoneDefinition(id, name, membersOnly,
                skill, level, quest, questStartSuffices, prerequisite,
                evidence, evidenceKey, storage, benefits, action, source);
    }

    private static InfrastructureMilestoneDefinition definition(String id,
            String name, boolean membersOnly, Map<Skill, Integer> skills,
            Map<String, Boolean> quests, String prerequisite,
            InfrastructureEvidenceKind evidence, String evidenceKey,
            StorageCapability storage,
            Map<InfrastructureBenefit, StrategicPriority> benefits,
            String action, String source)
    {
        return new InfrastructureMilestoneDefinition(id, name, membersOnly,
                skills, quests, prerequisite, evidence, evidenceKey, storage,
                benefits, action, source);
    }

    private static Map<Skill, Integer> skills(
            Skill first, int firstLevel, Skill second, int secondLevel)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        result.put(first, firstLevel);
        result.put(second, secondLevel);
        return result;
    }

    private static Map<String, Boolean> quests(
            String quest, boolean startSuffices)
    {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put(quest, startSuffices);
        return result;
    }

    private static Map<InfrastructureBenefit, StrategicPriority> benefits(
            InfrastructureBenefit first, StrategicPriority firstPriority)
    {
        EnumMap<InfrastructureBenefit, StrategicPriority> result =
                new EnumMap<>(InfrastructureBenefit.class);
        result.put(first, firstPriority);
        return result;
    }

    private static Map<InfrastructureBenefit, StrategicPriority> benefits(
            InfrastructureBenefit first, StrategicPriority firstPriority,
            InfrastructureBenefit second, StrategicPriority secondPriority)
    {
        Map<InfrastructureBenefit, StrategicPriority> result =
                benefits(first, firstPriority);
        result.put(second, secondPriority);
        return result;
    }

    private static Map<InfrastructureBenefit, StrategicPriority> benefits(
            InfrastructureBenefit first, StrategicPriority firstPriority,
            InfrastructureBenefit second, StrategicPriority secondPriority,
            InfrastructureBenefit third, StrategicPriority thirdPriority)
    {
        Map<InfrastructureBenefit, StrategicPriority> result =
                benefits(first, firstPriority, second, secondPriority);
        result.put(third, thirdPriority);
        return result;
    }

    private static Map<InfrastructureBenefit, StrategicPriority> benefits(
            InfrastructureBenefit first, StrategicPriority firstPriority,
            InfrastructureBenefit second, StrategicPriority secondPriority,
            InfrastructureBenefit third, StrategicPriority thirdPriority,
            InfrastructureBenefit fourth, StrategicPriority fourthPriority)
    {
        Map<InfrastructureBenefit, StrategicPriority> result = benefits(first,
                firstPriority, second, secondPriority, third, thirdPriority);
        result.put(fourth, fourthPriority);
        return result;
    }
}
