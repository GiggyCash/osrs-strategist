package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpandedOpportunityEngineTest
{
    @Test
    public void extraDailyContentOnlyAppearsAfterItsTimerWasObserved()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:battlestaves", 0L);
        timers.put("opportunity:dynamite", 0L);
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .build();

        java.util.List<Opportunity> result = new OpportunityEngine().evaluate(data);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(
                value -> value.getType() == OpportunityType.BATTLESTAVES));
        assertTrue(result.stream().anyMatch(
                value -> value.getType() == OpportunityType.DYNAMITE));
    }

    @Test
    public void unobservedDailyContentDoesNotInventReminder()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .recurringOpportunities(RecurringOpportunitySnapshot.unknown())
                .build();
        assertTrue(new OpportunityEngine().evaluate(data).isEmpty());
    }

    @Test
    public void herbRunNeedsPositiveCarriedSetupAndHerbPatchEvidence()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:herb-run", 0L);
        StrategyDataBundle unresolved = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .farming(FarmingSnapshot.unknown()).build();
        Opportunity missing = new OpportunityEngine().evaluate(unresolved).get(0);
        assertTrue(!missing.isSetupVerified());
        assertTrue(missing.getPreparation().contains("Carry a spade"));

        StrategyDataBundle wrongSeed = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new InventorySnapshot(Arrays.asList(
                        item("Spade"), item("Seed dibber"),
                        new ItemStackSnapshot(ItemID.ACORN, "Acorn", 1))))
                .farming(new FarmingSnapshot(
                        new HashSet<>(Collections.singletonList("falador")),
                        Collections.emptyMap(), Collections.emptyMap())).build();
        assertTrue(new OpportunityEngine().evaluate(wrongSeed).get(0)
                .getPreparation().contains("Carry a suitable herb seed"));

        StrategyDataBundle ready = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new InventorySnapshot(Arrays.asList(
                        item("Spade"), item("Seed dibber"),
                        new ItemStackSnapshot(ItemID.GUAM_SEED, "Guam seed", 1))))
                .farming(new FarmingSnapshot(
                        new HashSet<>(Collections.singletonList("falador")),
                        Collections.emptyMap(), Collections.emptyMap())).build();
        Opportunity verified = new OpportunityEngine().evaluate(ready).get(0);
        assertTrue(verified.isSetupVerified());
        assertTrue(verified.getPreparation().isEmpty());
    }

    @Test
    public void birdhouseTimerNeedsQuestLevelsAndCarriedSetup()
    {
        Map<String, Long> timers = Collections.singletonMap(
                "opportunity:birdhouse", 0L);
        StrategyDataBundle unresolved = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .quests(new QuestSnapshot(Collections.emptyMap())).build();
        Opportunity missing = new OpportunityEngine().evaluate(unresolved).get(0);
        assertTrue(!missing.isSetupVerified());
        assertTrue(missing.getPreparation().contains(
                "Complete Bone Voyage for Fossil Island access"));

        StrategyDataBundle treeSeeds = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Bone Voyage", QuestStatus.COMPLETE)))
                .inventory(new InventorySnapshot(Arrays.asList(
                        item("Hammer"), item("Chisel"),
                        new ItemStackSnapshot(1, "Clockwork", 4),
                        new ItemStackSnapshot(2, "Logs", 4),
                        new ItemStackSnapshot(ItemID.ACORN, "Acorn", 40)))).build();
        assertTrue(!new OpportunityEngine().evaluate(treeSeeds).get(0)
                .isSetupVerified());

        StrategyDataBundle ready = StrategyDataBundle.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Bone Voyage", QuestStatus.COMPLETE)))
                .inventory(new InventorySnapshot(Arrays.asList(
                        item("Hammer"), item("Chisel"),
                        new ItemStackSnapshot(1, "Clockwork", 4),
                        new ItemStackSnapshot(2, "Logs", 4),
                        new ItemStackSnapshot(3, "Guam seed", 40)))).build();
        assertTrue(new OpportunityEngine().evaluate(ready).get(0).isSetupVerified());
    }

    private static ItemStackSnapshot item(String name)
    {
        return new ItemStackSnapshot(1, name, 1);
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new java.util.EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new java.util.EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 50);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Test", 0, "Main", MembershipStatus.P2P,
                1, 1, 0L, levels, xp);
    }
}
