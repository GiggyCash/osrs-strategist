package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.EquipmentInventorySlot;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmReadinessSafetyTest
{
    private final PvmReadinessAnalyzer analyzer =
            new PvmReadinessAnalyzer(new PvmActivityCatalog());

    @Test
    public void bankedWeaponAndOneFoodDoNotProveReadyCarriedSetup()
    {
        BankSnapshot bank = new BankSnapshot(Arrays.asList(
                item("Rune scimitar", 1), item("Shark", 100)), 1L);
        PvmReadiness bankOnly = analyze(null,
                new InventorySnapshot(Collections.singletonList(item("Shark", 1))), bank);
        assertFalse(bankOnly.isReadyForRecommendation());
        assertTrue(bankOnly.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Equip")));

        PvmReadiness oneFood = analyze(
                new EquipmentSnapshot(Collections.singletonList(weapon("Rune scimitar", 1))),
                new InventorySnapshot(Collections.singletonList(item("Shark", 1))), bank);
        assertFalse(oneFood.isReadyForRecommendation());
        assertTrue(oneFood.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("token food")));
    }

    @Test
    public void genericLoadoutEvidenceCannotProveEncounterSpecificReadiness()
    {
        PvmReadiness readiness = analyze(
                new EquipmentSnapshot(Collections.singletonList(weapon("Rune scimitar", 1))),
                new InventorySnapshot(Arrays.asList(
                        item("Shark", 5), item("Prayer potion(4)", 1))), null);
        assertFalse(readiness.isReadyForRecommendation());
        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("encounter-specific")));
    }

    @Test
    public void equipmentWithoutWeaponSlotProvenanceDoesNotProveAReadiedWeapon()
    {
        PvmReadiness readiness = analyze(
                new EquipmentSnapshot(Collections.singletonList(item("Rune scimitar", 1))),
                new InventorySnapshot(Collections.emptyList()), null);
        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Equip a usable")));
    }

    @Test
    public void staleObservedVerifiedReadinessIsRecheckedAndBlockedIsPreserved()
    {
        Map<String, PvmReadiness> observedMap = new java.util.HashMap<>();
        observedMap.put("pvm:obor", new PvmReadiness("pvm:obor", true,
                RecommendationConfidence.VERIFIED, Collections.emptyList()));
        PvmReadiness rechecked = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()), null,
                new InventorySnapshot(Collections.emptyList()), null,
                new PvmSnapshot(observedMap)).readinessFor("pvm:obor");
        assertFalse(rechecked.isReadyForRecommendation());
        assertTrue(rechecked.getConfidence() == RecommendationConfidence.CHECK_NEEDED);

        observedMap.put("pvm:obor", new PvmReadiness("pvm:obor", false,
                RecommendationConfidence.BLOCKED,
                Collections.singletonList("Known unsafe state")));
        PvmReadiness blocked = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()), null,
                new InventorySnapshot(Collections.emptyList()), null,
                new PvmSnapshot(observedMap)).readinessFor("pvm:obor");
        assertTrue(blocked.getConfidence() == RecommendationConfidence.BLOCKED);
    }

    private PvmReadiness analyze(EquipmentSnapshot equipment,
            InventorySnapshot inventory, BankSnapshot bank)
    {
        return analyzer.analyze(account(), new QuestSnapshot(Collections.emptyMap()),
                equipment, inventory, bank, null).readinessFor("pvm:obor");
    }

    private static ItemStackSnapshot item(String name, int quantity)
    {
        return new ItemStackSnapshot(1, name, quantity);
    }


    private static ItemStackSnapshot weapon(String name, int quantity)
    {
        return new ItemStackSnapshot(1, name, quantity,
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 80); xp.put(skill, 0); }
        return new AccountSnapshot("PvM", 0, "Main", MembershipStatus.P2P,
                1, 1600, 0L, levels, xp);
    }
}
