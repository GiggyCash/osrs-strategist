package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuestPathPlanningServiceTest
{
    private final QuestPathPlanningService service =
            new QuestPathPlanningService();

    @Test
    public void sameQuestOnTwoGoalPathsIsOneStepWithBothProvenances()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Song of the Elves", QuestStatus.NOT_STARTED);
        StrategyContext context = context(statuses, MembershipStatus.P2P,
                QuestTolerance.LOW);

        QuestPathPlan plan = service.plan(context, Arrays.asList(
                GoalType.PRIFDDINAS, GoalType.BOWFA));
        QuestPathStep song = named(plan, "Song of the Elves");

        assertNotNull(song);
        assertEquals(2, song.getGoalCount());
        assertTrue(song.getProvenancePaths().containsKey(GoalType.PRIFDDINAS));
        assertTrue(song.getProvenancePaths().containsKey(GoalType.BOWFA));
        assertTrue(song.sharedDependencyValue() > 0.0);
        assertEquals(1, plan.getSteps().stream()
                .filter(step -> step.getQuestName()
                        .equals("Song of the Elves")).count());
    }

    @Test
    public void unfinishedPrerequisiteOrdersBeforeItsBlockedDependent()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Lost City", QuestStatus.NOT_STARTED);
        statuses.put("Fairytale I - Growing Pains", QuestStatus.NOT_STARTED);
        statuses.put("Nature Spirit", QuestStatus.COMPLETE);

        QuestPathPlan plan = service.plan(context(statuses,
                MembershipStatus.P2P, QuestTolerance.LOW),
                java.util.Collections.singleton(GoalType.QUEST_CAPE));
        QuestPathStep lostCity = named(plan, "Lost City");
        QuestPathStep fairy = named(plan, "Fairytale I - Growing Pains");

        assertNotNull(lostCity);
        assertNotNull(fairy);
        assertTrue(lostCity.isEligibleNow());
        assertFalse(fairy.isEligibleNow());
        assertEquals("Lost City", plan.nextEligibleStep().getQuestName());
        assertTrue(lostCity.getUnfinishedDependents()
                .contains("Fairytale I - Growing Pains"));
    }

    @Test
    public void optionalQuestToleranceDoesNotReorderRequiredPathWork()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Lost City", QuestStatus.NOT_STARTED);
        statuses.put("Fairytale I - Growing Pains", QuestStatus.NOT_STARTED);
        statuses.put("Nature Spirit", QuestStatus.COMPLETE);

        QuestPathPlan low = service.plan(context(statuses,
                MembershipStatus.P2P, QuestTolerance.LOW),
                java.util.Collections.singleton(GoalType.QUEST_CAPE));
        QuestPathPlan high = service.plan(context(statuses,
                MembershipStatus.P2P, QuestTolerance.HIGH),
                java.util.Collections.singleton(GoalType.QUEST_CAPE));

        assertEquals(low.getSteps().stream().map(QuestPathStep::getQuestName)
                        .collect(Collectors.toList()),
                high.getSteps().stream().map(QuestPathStep::getQuestName)
                        .collect(Collectors.toList()));
    }

    @Test
    public void membersGoalDoesNotCreateExecutableF2pQuestPath()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Song of the Elves", QuestStatus.NOT_STARTED);
        QuestPathPlan plan = service.plan(context(statuses,
                MembershipStatus.F2P, QuestTolerance.NORMAL),
                java.util.Collections.singleton(GoalType.PRIFDDINAS));

        assertTrue(plan.isEmpty());
        assertNull(plan.nextEligibleStep());
    }

    @Test
    public void unrelatedQuestRewardDoesNotBecomeAPathRequirement()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Recipe for Disaster - Wartface & Bentnoze",
                QuestStatus.NOT_STARTED);
        QuestPathPlan plan = service.plan(context(statuses,
                MembershipStatus.P2P, QuestTolerance.LOW, 1),
                java.util.Collections.singleton(GoalType.BARROWS_GLOVES));
        QuestPathStep goblins = named(plan,
                "Recipe for Disaster - Wartface & Bentnoze");

        assertNotNull(goblins);
        assertEquals(Integer.valueOf(1_000),
                goblins.getGuaranteedRewardXp().get(Skill.FARMING));
        assertEquals(0.0, goblins.getGoalPathRewardValue(), 0.0);
    }

    private static QuestPathStep named(QuestPathPlan plan, String name)
    {
        for (QuestPathStep step : plan.getSteps())
            if (name.equals(step.getQuestName())) return step;
        return null;
    }

    private static StrategyContext context(Map<String, QuestStatus> statuses,
            MembershipStatus membership, QuestTolerance tolerance)
    {
        return context(statuses, membership, tolerance, 99);
    }

    private static StrategyContext context(Map<String, QuestStatus> statuses,
            MembershipStatus membership, QuestTolerance tolerance,
            int farmingLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.FARMING ? farmingLevel : 99);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Quest planner", 31L,
                0, "MAIN", membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                levels.size() * 99, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .quests(new QuestSnapshot(statuses)).build();
        return new StrategyContext(data, StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, tolerance, GoalType.QUEST_CAPE,
                false, false, false, new PreferenceProfile());
    }
}
