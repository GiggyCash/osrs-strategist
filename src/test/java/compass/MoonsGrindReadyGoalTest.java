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
        assertTrue(candidate.getReason()
                .contains("without claiming player execution skill"));
        assertFalse(candidate.getReason().contains("carried supplies"));
        assertTrue(new ActionabilityPolicy().canLeadQueue(candidate));
    }

    @Test
    public void repeatableDungeonSuppliesRequireReviewedSkillQuality()
    {
        PvmReadiness underprepared = supplyReadiness(30, 38, 20, 20);
        assertFalse(underprepared.isReadyForRecommendation());
        assertTrue(underprepared.getMissingRequirements().contains("Cooking 60"));
        assertTrue(underprepared.getMissingRequirements().contains("Herblore 55"));
        assertTrue(underprepared.getMissingRequirements().contains(
                "Hunter 50 or Fishing 60"));

        assertTrue(supplyReadiness(60, 55, 50, 20)
                .isReadyForRecommendation());
        assertTrue(supplyReadiness(60, 55, 20, 60)
                .isReadyForRecommendation());
    }

    @Test
    public void repeatableFarmCombatFloorExceedsQuestAccess()
    {
        PvmReadiness initialClear = combatReadiness(60, 60, 60);
        assertFalse(initialClear.isReadyForRecommendation());
        assertTrue(initialClear.getMissingRequirements().contains("Attack 70"));
        assertTrue(initialClear.getMissingRequirements().contains("Strength 70"));
        assertTrue(initialClear.getMissingRequirements().contains("Defence 70"));
        assertTrue(combatReadiness(70, 70, 70)
                .isReadyForRecommendation());
    }

    @Test
    public void selectedGoalProducesTypedGlobalDoNext()
    {
        PvmReadiness readiness = new PvmReadiness(ACTIVITY, true,
                Confidence.VERIFIED, Collections.emptyList());
        CandidateProvider generic = context -> Collections.singletonList(
                new Recommendation("generic:progress", "Generic progress",
                        "Useful but unrelated account progress.", 80.0,
                        Confidence.VERIFIED,
                        new Guidance("Do the generic step.", "Ready.",
                                "Safe location.", "Generic progress."),
                        Safety.harmless(false)));
        StrategyEngine engine = TestFixtures.strategyEngine(null, null, null,
                new StrategyCandidateRegistry(java.util.Arrays.asList(
                        provider, generic)), new ActionabilityPolicy(),
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy(),
                new GoalDependencyProvenanceService());

        List<Recommendation> queue = engine.evaluate(
                data(0, Membership.P2P, QuestStatus.COMPLETE,
                        readiness, true),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MOONS_GRIND_READY,
                false, false, new PreferenceProfile()).getRecommendations();

        assertFalse(queue.isEmpty());
        assertEquals(ACTIVITY, queue.get(0).getId());
        assertNotNull(queue.get(0).getGoalProvenance());
        assertEquals(GoalRelation.DIRECT,
                queue.get(0).getGoalProvenance().getRelationship());
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
    public void liveSetupProgressionClearsStaleMoonsPreparation()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        PvmReadinessAnalyzer analyzer = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog());
        PvmSnapshot preparing = analyzer.analyze(
                base.account(), base.quests(),
                new ItemsState(Collections.singletonList(
                        equipped("Rune scimitar",
                                EquipmentInventorySlot.WEAPON))),
                new ItemsState(Collections.emptyList()), null, null);
        assertFalse(preparing.readinessFor(ACTIVITY)
                .isReadyForRecommendation());

        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune longsword", EquipmentInventorySlot.WEAPON),
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(2, "Dragon mace", 1)));
        PvmReadiness ready = analyzer.analyze(
                base.account(), base.quests(), equipment, inventory,
                null, preparing).readinessFor(ACTIVITY);

        assertTrue(ready.isReadyForRecommendation());
        assertTrue(ready.getMissingRequirements().isEmpty());
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
    public void weaponComponentsCannotMasqueradeAsMoonsStyleOptions()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(java.util.Arrays.asList(
                new ItemState(1, "Godsword shard 1", 1),
                new ItemState(2, "Dragon mace", 1)));

        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);

        assertFalse(analyzed.isReadyForRecommendation());
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("stab")));
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("slash")));
    }

    @Test
    public void notedWeaponsCannotMasqueradeAsCarriedMoonsSwitches()
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune scimitar", EquipmentInventorySlot.WEAPON),
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(1435, "Rune mace", 1, -1, true)));

        PvmReadiness analyzed = new PvmReadinessAnalyzer(
                new PvmActivityCatalog(), new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);

        assertFalse(analyzed.isReadyForRecommendation());
        assertTrue(analyzed.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("crush")));
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
        return data(type, membership, questStatus, readiness, observedSetup,
                Collections.emptyMap());
    }

    private static GameData data(int type, Membership membership,
            QuestStatus questStatus, PvmReadiness readiness,
            boolean observedSetup, Map<Skill, Integer> levelOverrides)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.SLAYER ? 40 : 70);
            xp.put(skill, 0);
        }
        levels.putAll(levelOverrides);
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

    private static PvmReadiness supplyReadiness(int cooking, int herblore,
            int hunter, int fishing)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        levels.put(Skill.COOKING, cooking);
        levels.put(Skill.HERBLORE, herblore);
        levels.put(Skill.HUNTER, hunter);
        levels.put(Skill.FISHING, fishing);
        return readiness(levels);
    }

    private static PvmReadiness combatReadiness(int attack, int strength,
            int defence)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.COOKING, 60);
        levels.put(Skill.HERBLORE, 55);
        levels.put(Skill.HUNTER, 50);
        return readiness(levels);
    }

    private static PvmReadiness readiness(Map<Skill, Integer> levels)
    {
        GameData base = data(0, Membership.P2P, QuestStatus.COMPLETE,
                checkNeeded(), true, levels);
        ItemsState equipment = new ItemsState(java.util.Arrays.asList(
                equipped("Rune longsword", EquipmentInventorySlot.WEAPON),
                equipped("Rune platebody", EquipmentInventorySlot.BODY),
                equipped("Rune platelegs", EquipmentInventorySlot.LEGS)));
        ItemsState inventory = new ItemsState(Collections.singletonList(
                new ItemState(2, "Dragon mace", 1)));
        return new PvmReadinessAnalyzer(new PvmActivityCatalog(),
                new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog()).analyze(
                        base.account(), base.quests(), equipment, inventory,
                        null, null).readinessFor(ACTIVITY);
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
