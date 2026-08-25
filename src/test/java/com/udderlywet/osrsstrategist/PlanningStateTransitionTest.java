package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** End-to-end evidence sequence: observations must change the first safe action. */
public class PlanningStateTransitionTest
{
    @Test
    public void loginObservationProgressionGearFeedbackAndRelogSequence()
    {
        UniversalDependencyPlanner planner = new UniversalDependencyPlanner();

        StrategyContext login = context(null, Collections.emptyMap(),
                Collections.emptyList(), new PreferenceProfile());
        UniversalDependencyResolution beforeBank = planner.resolveResource(
                "Cannonball", 100, login);
        assertTrue(beforeBank.nextAction().getAction().contains("Open the bank"));

        StrategyContext bankObserved = context(
                new BankSnapshot(Collections.emptyList(), 1L),
                Collections.emptyMap(), Collections.emptyList(),
                new PreferenceProfile());
        UniversalDependencyResolution afterBank = planner.resolveResource(
                "Cannonball", 100, bankObserved);
        assertFalse(afterBank.nextAction().getAction().contains("Open the bank"));
        assertTrue(afterBank.getNodes().stream().anyMatch(node ->
                "resource:steel-bar".equals(node.getId())));

        StrategyContext acquired = context(
                new BankSnapshot(Collections.emptyList(), 1L),
                Collections.emptyMap(), Collections.singletonList(
                        new ItemStackSnapshot(ItemID.MCANNONBALL,
                                "Cannonball", 100)),
                new PreferenceProfile());
        UniversalDependencyResolution afterItem = planner.resolveResource(
                "Cannonball", 100, acquired);
        assertEquals(RecommendationConfidence.VERIFIED,
                afterItem.getNodes().stream().filter(node ->
                        "resource:cannonball".equals(node.getId()))
                        .findFirst().orElseThrow(AssertionError::new)
                        .getConfidence());

        Map<String, QuestStatus> questState = new HashMap<>();
        questState.put("Dwarf Cannon", QuestStatus.COMPLETE);
        StrategyContext questComplete = context(
                new BankSnapshot(Collections.emptyList(), 1L), questState,
                Collections.emptyList(), new PreferenceProfile());
        UniversalDependencyResolution afterQuest = planner.resolveResource(
                "Cannonball", 100, questComplete);
        assertTrue(afterQuest.getNodes().stream().anyMatch(node ->
                "quest:dwarf-cannon".equals(node.getId())
                        && node.getConfidence()
                        == RecommendationConfidence.VERIFIED));

        GearProgressionEntry ranged = new GearProgressionCatalog()
                .forStyle(CombatStyle.RANGED).stream()
                .filter(value -> value.getTier() == GearBudgetTier.MIDGAME)
                .findFirst().orElseThrow(AssertionError::new);
        StrategyContext gearChanged = context(
                new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(3000, "Crystal body", 1)), 1L),
                questState, Collections.emptyList(), new PreferenceProfile());
        ContextualGearAssessment gear = new ContextualGearDecisionService()
                .assess(ranged, gearChanged);
        assertEquals("Crystal body",
                gear.get(GearDecisionKind.BEST_OWNED).getValue());

        RecommendationEngine recommendations = new RecommendationEngine(
                new TrainingMethodSelector(new TrainingMethodDatabase(), null,
                        new ExpandedTrainingMethodCatalog(),
                        new F2pBaselineMethodCatalog(),
                        new TrainingMethodPolicy()));
        PreferenceProfile feedback = new PreferenceProfile();
        java.util.List<Recommendation> first = recommendations.recommend(
                gearChanged.getData(), StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false, false, feedback);
        assertFalse(first.isEmpty());
        feedback.apply(first.get(0).getId(), FeedbackAction.LATER);
        java.util.List<Recommendation> rotated = recommendations.recommend(
                gearChanged.getData(), StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false, false, feedback);
        assertFalse(rotated.isEmpty());
        assertFalse(first.get(0).getId().equals(rotated.get(0).getId()));

        Map<String, Double> savedWeights = feedback.snapshot();
        Map<String, Long> savedCooldowns = feedback.cooldownSnapshot();
        feedback.clear(); // logout/account change
        assertFalse(feedback.isOnCooldown(first.get(0).getId()));
        feedback.replaceAll(savedWeights); // return to the same account profile
        feedback.replaceCooldowns(savedCooldowns);
        assertTrue(feedback.isOnCooldown(first.get(0).getId()));
    }

    private static StrategyContext context(BankSnapshot bank,
            Map<String, QuestStatus> quests, java.util.List<ItemStackSnapshot> inventory,
            PreferenceProfile preferences)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Sequence", 991L, 1,
                "IRONMAN", MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        StrategyDataBundle.Builder data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(inventory))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .quests(new QuestSnapshot(quests));
        if (bank != null) data.bank(bank);
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, preferences);
    }
}
