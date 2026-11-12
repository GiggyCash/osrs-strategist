package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.EquipmentInventorySlot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoonsGrindReadyGoalTest
{
    private static final String ACTIVITY = "pvm:lunar_chests";
    private final PvmCandidateProvider provider = new PvmCandidateProvider();

    @Test
    public void goalUsesReviewedQuestAndSkillDependencies()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Perilous Moons");
        assertNotNull(quest);
        assertEquals(Collections.singletonList("Twilight's Promise"),
                quest.getPrerequisites());
        assertEquals(Integer.valueOf(48),
                quest.getSkillRequirements().get(Skill.SLAYER));

        StrategyContext context = context(data(0, Membership.P2P,
                QuestStatus.NOT_STARTED, checkNeeded(), false),
                GoalType.MOONS_GRIND_READY);
        GoalDependencyProvenanceService goals =
                new GoalDependencyProvenanceService();
        assertTrue(goals.isRequiredQuest(GoalType.MOONS_GRIND_READY,
                "Perilous Moons", context));
        assertEquals(48, goals.nextRequiredSkillLevel(
                GoalType.MOONS_GRIND_READY, Skill.SLAYER, context));
    }

    @Test
    public void unfinishedOrUnknownQuestCannotBeBypassedByPvmPreparation()
    {
        assertNull(find(provider.candidates(context(data(0, Membership.P2P,
                QuestStatus.NOT_STARTED, checkNeeded(), true),
                GoalType.MOONS_GRIND_READY))));
        assertNull(find(provider.candidates(context(data(0, Membership.P2P,
                QuestStatus.UNKNOWN, checkNeeded(), true),
                GoalType.MOONS_GRIND_READY))));
    }

    @Test
    public void completedQuestWithUnobservedSetupProducesExactVerification()
    {
        Recommendation candidate = find(provider.candidates(context(
                data(0, Membership.P2P, QuestStatus.COMPLETE,
                        checkNeeded(), false),
                GoalType.MOONS_GRIND_READY)));

        assertNotNull(candidate);
        assertEquals("verify:pvm:lunar_chests", candidate.getId());
        assertEquals(Confidence.CHECK_NEEDED, candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction()
                .contains("live equipment and inventory"));
        assertFalse(candidate.getGuidance().getNote().contains("ready"));
        assertTrue(new ActionabilityPolicy().canLeadQueue(candidate));
    }

    @Test
    public void observedUnderpreparedSetupNamesRepeatableFarmWork()
    {
        Recommendation candidate = find(provider.candidates(context(
                data(1, Membership.P2P, QuestStatus.COMPLETE,
                        checkNeeded(), true),
                GoalType.MOONS_GRIND_READY)));

        assertNotNull(candidate);
        assertEquals("prepare:pvm:lunar_chests", candidate.getId());
        assertTrue(candidate.getGuidance().getAction()
                .contains("Equip stab, crush, and slash options"));
        assertTrue(candidate.getGuidance().getSupplies()
                .contains("moonlight-potion"));
        assertFalse(candidate.getGuidance().getAction()
                .toLowerCase().contains("grand exchange"));
        assertTrue(new ActionabilityPolicy().canLeadQueue(candidate));

        Recommendation attached = new GoalDependencyProvenanceService()
                .attach(candidate, context(data(1, Membership.P2P,
                        QuestStatus.COMPLETE, checkNeeded(), true),
                        GoalType.MOONS_GRIND_READY));
        assertNotNull(attached.getGoalProvenance());
        assertEquals(GoalRelation.DIRECT,
                attached.getGoalProvenance().getRelationship());
    }

    @Test
    public void verifiedReadinessProducesDirectEncounterCandidate()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune longsword", EquipmentInventorySlot.WEAPON),
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(2, "Dragon mace", 1)));
        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);
        assertTrue(analyzed.isReadyForRecommendation());

        Recommendation candidate = find(provider.candidates(context(
                data(0, Membership.P2P, QuestStatus.COMPLETE,
                        analyzed, true),
                GoalType.MOONS_GRIND_READY)));

        assertNotNull(candidate);
        assertEquals(ACTIVITY, candidate.getId());
        assertEquals(Confidence.VERIFIED, candidate.getConfidence());
        assertTrue(new ActionabilityPolicy().canLeadQueue(candidate));
    }

    @Test
    public void analyzerKeepsIncompleteStyleAndArmourEvidenceInPreparation()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(),
                        new ItemsState(Collections.singletonList(
                                equipped("Rune scimitar",
                                        EquipmentInventorySlot.WEAPON))),
                        new ItemsState(Collections.emptyList()), null, null)
                .readinessFor(ACTIVITY);

        assertFalse(analyzed.isReadyForRecommendation());
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("crush")));
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("body armour")));
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("leg armour")));
    }

    @Test
    public void foodAndUtilityToolsCannotMasqueradeAsMeleeSwitches()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune scimitar", EquipmentInventorySlot.WEAPON),
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(java.util.Arrays.asList(
                new ItemState(1, "Swordfish", 10),
                new ItemState(2, "Hammer", 1)));

        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);

        assertFalse(analyzed.isReadyForRecommendation());
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("stab")));
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("crush")));
    }

    @Test
    public void nonDefensiveSlotItemsCannotMasqueradeAsMoonsArmour()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune longsword", EquipmentInventorySlot.WEAPON),
                equipped("Graceful top", EquipmentInventorySlot.BODY),
                equipped("Graceful legs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(2, "Dragon mace", 1)));

        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);

        assertFalse(analyzed.isReadyForRecommendation());
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("body armour")));
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("leg armour")));
    }

    @Test
    public void selectionAndAccountSafetyRemainFailClosed()
    {
        GameData main = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        assertNull(find(provider.candidates(context(main,
                GoalType.AUTOMATIC))));
        assertNotNull(find(provider.candidates(context(main,
                GoalType.MOONS_GRIND_READY))));

        assertTrue(provider.candidates(context(data(0, Membership.F2P,
                QuestStatus.COMPLETE, checkNeeded(), true),
                GoalType.MOONS_GRIND_READY)).isEmpty());
        assertTrue(provider.candidates(context(data(3, Membership.P2P,
                QuestStatus.COMPLETE, checkNeeded(), true),
                GoalType.MOONS_GRIND_READY)).isEmpty());

        Recommendation uim = find(provider.candidates(context(data(2,
                Membership.P2P, QuestStatus.COMPLETE, checkNeeded(), true),
                GoalType.MOONS_GRIND_READY)));
        assertNotNull(uim);
        assertFalse(uim.getGuidance().getAction().toLowerCase()
                .contains("bank"));
    }

    private static PvmReadiness checkNeeded()
    {
        return new PvmReadiness(ACTIVITY, false, Confidence.CHECK_NEEDED,
                Collections.singletonList(
                        "Equip stab, crush, and slash options and verify supplies"));
    }

    private static StrategyContext context(GameData data, GoalType goal)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL, goal,
                false, false, new PreferenceProfile());
    }

    private static GameData data(int type, Membership membership,
            QuestStatus questStatus, PvmReadiness readiness,
            boolean observedSetup)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.SLAYER ? 40 : 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Moons", 901L,
                type, AccountMode.fromTypeCode(type).name(), membership,
                membership == Membership.P2P ? 1 : 0,
                70 * Skill.values().length, 0L, levels, xp);
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Twilight's Promise", QuestStatus.COMPLETE);
        quests.put("Perilous Moons", questStatus);
        Map<String, PvmReadiness> readinessByActivity = new HashMap<>();
        readinessByActivity.put(ACTIVITY, readiness);
        ItemsState setup = observedSetup
                ? new ItemsState(Collections.emptyList())
                : ItemsState.unknown();
        return GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .equipment(setup)
                .inventory(setup)
                .pvm(new PvmSnapshot(readinessByActivity))
                .build();
    }

    private static Recommendation find(List<Recommendation> candidates)
    {
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static ItemState equipped(String name,
            EquipmentInventorySlot slot)
    {
        return new ItemState(1, name, 1, slot.getSlotIdx());
    }
}
