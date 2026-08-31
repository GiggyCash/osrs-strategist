package compass;

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
        GameData data = GameData.builder(account())
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
        GameData data = GameData.builder(account())
                .recurringOpportunities(RecurringOpportunitySnapshot.unknown())
                .build();
        assertTrue(new OpportunityEngine().evaluate(data).isEmpty());
    }

    @Test
    public void herbRunNeedsPositiveCarriedSetupAndHerbPatchEvidence()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:herb-run", 0L);
        GameData unresolved = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new ItemsState(Collections.emptyList()))
                .farming(FarmingSnapshot.unknown()).build();
        Opportunity missing = new OpportunityEngine().evaluate(unresolved).get(0);
        assertTrue(!missing.isSetupVerified());
        assertTrue(missing.getPreparation().contains("Carry a spade"));

        GameData wrongSeed = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new ItemsState(Arrays.asList(
                        item("Spade"), item("Seed dibber"),
                        new ItemState(ItemID.ACORN, "Acorn", 1))))
                .farming(new FarmingSnapshot(
                        new HashSet<>(Collections.singletonList("falador")),
                        Collections.emptyMap(), Collections.emptyMap())).build();
        assertTrue(new OpportunityEngine().evaluate(wrongSeed).get(0)
                .getPreparation().contains("Carry one guam seed"));

        GameData ready = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new ItemsState(Arrays.asList(
                        item("Spade"), item("Seed dibber"),
                        new ItemState(ItemID.GUAM_SEED, "Guam seed", 1))))
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
        GameData unresolved = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .inventory(new ItemsState(Collections.emptyList()))
                .quests(new QuestSnapshot(Collections.emptyMap())).build();
        Opportunity missing = new OpportunityEngine().evaluate(unresolved).get(0);
        assertTrue(!missing.isSetupVerified());
        assertTrue(missing.getPreparation().contains(
                "Complete Bone Voyage for Fossil Island access"));
        assertTrue(missing.getPreparation().contains("Carry 4 regular logs"));
        assertTrue(missing.getPreparation().contains("Carry 40 barley seeds"));

        GameData treeSeeds = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Bone Voyage", QuestStatus.COMPLETE)))
                .inventory(new ItemsState(Arrays.asList(
                        item("Hammer"), item("Chisel"),
                        new ItemState(1, "Clockwork", 4),
                        new ItemState(2, "Logs", 4),
                        new ItemState(ItemID.ACORN, "Acorn", 40)))).build();
        assertTrue(!new OpportunityEngine().evaluate(treeSeeds).get(0)
                .isSetupVerified());

        GameData ready = GameData.builder(account())
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Bone Voyage", QuestStatus.COMPLETE)))
                .inventory(new ItemsState(Arrays.asList(
                        item("Hammer"), item("Chisel"),
                        new ItemState(1, "Clockwork", 4),
                        new ItemState(2, "Logs", 4),
                        new ItemState(3, "Guam seed", 40)))).build();
        assertTrue(new OpportunityEngine().evaluate(ready).get(0).isSetupVerified());
    }

    private static ItemState item(String name)
    {
        return new ItemState(1, name, 1);
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
