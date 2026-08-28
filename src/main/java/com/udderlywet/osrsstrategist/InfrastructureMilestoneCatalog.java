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
        add(definition("poh-costume-room", "POH costume storage", true,
                Skill.CONSTRUCTION, 46, null, false, "poh-access",
                InfrastructureEvidenceKind.STORAGE_CAPABILITY, null,
                StorageCapability.POH_COSTUME_ROOM,
                benefits(InfrastructureBenefit.INVENTORY_RELIEF, StrategicPriority.HIGH,
                        InfrastructureBenefit.STORAGE, StrategicPriority.HIGH,
                        InfrastructureBenefit.STORABLE_EQUIPMENT, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.MODERATE),
                "In a verified POH, build a Costume room for 50,000 coins, then at 46 Construction build and verify an oak armour case or magic wardrobe before relying on equipment storage.",
                "https://oldschool.runescape.wiki/w/Costume_room"));
        add(definition("poh-portal-chamber", "POH portal chamber", true,
                Skill.CONSTRUCTION, 50, null, false, "poh-access",
                InfrastructureEvidenceKind.TRANSPORT_ROUTE,
                "poh-portal-nexus", null,
                benefits(InfrastructureBenefit.TRAVEL_NETWORK, StrategicPriority.HIGH,
                        InfrastructureBenefit.SETUP_REUSE, StrategicPriority.HIGH,
                        InfrastructureBenefit.SELF_SUFFICIENCY, StrategicPriority.MODERATE),
                "Choose a useful destination first. In a verified POH, build a Portal chamber for 100,000 coins, then build its focus and configure the exact portal only after verifying the Magic level and rune cost.",
                "https://oldschool.runescape.wiki/w/Portal_chamber"));
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
