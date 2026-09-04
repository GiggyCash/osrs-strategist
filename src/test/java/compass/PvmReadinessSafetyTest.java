package compass;

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
            new PvmReadinessAnalyzer(new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(), new PvmPreparationProfileCatalog());

    @Test
    public void bankedWeaponAndOneFoodDoNotProveReadyCarriedSetup()
    {
        ItemsState bank = new ItemsState(Arrays.asList(
                item("Rune scimitar", 1), item("Shark", 100)), 1L);
        PvmReadiness bankOnly = analyze(null,
                new ItemsState(Collections.singletonList(item("Shark", 1))), bank);
        assertFalse(bankOnly.isReadyForRecommendation());
        assertTrue(bankOnly.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Equip")));

        PvmReadiness oneFood = analyze(
                new ItemsState(Collections.singletonList(weapon("Rune scimitar", 1))),
                new ItemsState(Collections.singletonList(item("Shark", 1))), bank);
        assertFalse(oneFood.isReadyForRecommendation());
        assertTrue(oneFood.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("token food")));
    }

    @Test
    public void genericLoadoutEvidenceCannotProveEncounterSpecificReadiness()
    {
        PvmReadiness readiness = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()),
                new ItemsState(Collections.singletonList(
                        weapon("Rune scimitar", 1))),
                new ItemsState(Arrays.asList(
                        item("Shark", 5), item("Prayer potion(4)", 1))),
                null, null).readinessFor("pvm:callisto");
        assertFalse(readiness.isReadyForRecommendation());
        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("encounter-specific")));
    }

    @Test
    public void equipmentWithoutWeaponSlotProvenanceDoesNotProveAReadiedWeapon()
    {
        PvmReadiness readiness = analyze(
                new ItemsState(Collections.singletonList(item("Rune scimitar", 1))),
                new ItemsState(Collections.emptyList()), null);
        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Equip a usable")));
    }

    @Test
    public void staleObservedVerifiedReadinessIsRecheckedAndBlockedIsPreserved()
    {
        Map<String, PvmReadiness> observedMap = new java.util.HashMap<>();
        observedMap.put("pvm:obor", new PvmReadiness("pvm:obor", true,
                Confidence.VERIFIED, Collections.emptyList()));
        PvmReadiness rechecked = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()), null,
                new ItemsState(Collections.emptyList()), null,
                new PvmSnapshot(observedMap)).readinessFor("pvm:obor");
        assertFalse(rechecked.isReadyForRecommendation());
        assertTrue(rechecked.getConfidence() == Confidence.CHECK_NEEDED);

        observedMap.put("pvm:obor", new PvmReadiness("pvm:obor", false,
                Confidence.BLOCKED,
                Collections.singletonList("Known unsafe state")));
        PvmReadiness blocked = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()), null,
                new ItemsState(Collections.emptyList()), null,
                new PvmSnapshot(observedMap)).readinessFor("pvm:obor");
        assertTrue(blocked.getConfidence() == Confidence.BLOCKED);
    }

    @Test
    public void oborTransitionsFromPreparationToFullyVerifiedCarriedEvidence()
    {
        ItemsState equipment = new ItemsState(
                Collections.singletonList(weapon("Rune scimitar", 1)));
        PvmReadiness preparation = analyze(equipment,
                new ItemsState(Arrays.asList(item("Shark", 5))), null);
        assertFalse(preparation.isReadyForRecommendation());
        assertTrue(preparation.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Giant key")));

        PvmReadiness ready = analyze(equipment,
                new ItemsState(Arrays.asList(
                        item("Shark", 5), item("Giant key", 1))), null);
        assertTrue(ready.isReadyForRecommendation());
        assertTrue(ready.getMissingRequirements().isEmpty());
    }

    @Test
    public void exactProfileRecomputesAndClearsStalePreparationEvidence()
    {
        Map<String, PvmReadiness> observed = new java.util.HashMap<>();
        observed.put("pvm:obor", new PvmReadiness("pvm:obor", false,
                Confidence.CHECK_NEEDED,
                Collections.singletonList("Old missing setup")));
        PvmReadiness ready = analyzer.analyze(account(),
                new QuestSnapshot(Collections.emptyMap()),
                new ItemsState(Collections.singletonList(
                        weapon("Rune scimitar", 1))),
                new ItemsState(Arrays.asList(
                        item("Shark", 5), item("Giant key", 1))),
                null, new PvmSnapshot(observed)).readinessFor("pvm:obor");
        assertTrue(ready.isReadyForRecommendation());
        assertFalse(ready.getMissingRequirements().contains("Old missing setup"));
    }

    @Test
    public void brutusRequiresCompletedQuestAndTransitionsToReady()
    {
        Map<String, QuestStatus> quests = Collections.singletonMap(
                "The Ides of Milk", QuestStatus.NOT_STARTED);
        PvmReadiness blocked = analyzer.analyze(account(),
                new QuestSnapshot(quests),
                new ItemsState(Collections.singletonList(
                        weapon("Rune scimitar", 1))),
                new ItemsState(Collections.singletonList(
                        item("Lobster", 5))), null, null)
                .readinessFor("pvm:brutus");
        assertFalse(blocked.isReadyForRecommendation());
        assertTrue(blocked.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("The Ides of Milk")));

        PvmReadiness ready = analyzer.analyze(account(),
                new QuestSnapshot(Collections.singletonMap(
                        "The Ides of Milk", QuestStatus.COMPLETE)),
                new ItemsState(Collections.singletonList(
                        weapon("Rune scimitar", 1))),
                new ItemsState(Collections.singletonList(
                        item("Lobster", 5))), null, null)
                .readinessFor("pvm:brutus");
        assertTrue(ready.isReadyForRecommendation());
    }

    private PvmReadiness analyze(ItemsState equipment,
            ItemsState inventory, ItemsState bank)
    {
        return analyzer.analyze(account(), new QuestSnapshot(Collections.emptyMap()),
                equipment, inventory, bank, null).readinessFor("pvm:obor");
    }

    private static ItemState item(String name, int quantity)
    {
        return new ItemState(1, name, quantity);
    }


    private static ItemState weapon(String name, int quantity)
    {
        return new ItemState(1, name, quantity,
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 80); xp.put(skill, 0); }
        return new AccountSnapshot("PvM", 0L, 0, "Main", Membership.P2P, 1, 1600, 0L, levels, xp);
    }
}
