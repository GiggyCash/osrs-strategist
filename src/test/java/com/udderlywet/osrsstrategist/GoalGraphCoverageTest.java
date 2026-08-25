package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoalGraphCoverageTest
{
    @Test
    public void universalGraphHasTypedHomesForEveryPlanningDomain()
    {
        java.util.EnumSet<GoalNodeKind> kinds =
                java.util.EnumSet.allOf(GoalNodeKind.class);
        assertTrue(kinds.contains(GoalNodeKind.MINIQUEST));
        assertTrue(kinds.contains(GoalNodeKind.TRAINING_METHOD));
        assertTrue(kinds.contains(GoalNodeKind.PVM_ENCOUNTER));
        assertTrue(kinds.contains(GoalNodeKind.SLAYER));
        assertTrue(kinds.contains(GoalNodeKind.MINIGAME));
        assertTrue(kinds.contains(GoalNodeKind.CLUE));
        assertTrue(kinds.contains(GoalNodeKind.STASH));
        assertTrue(kinds.contains(GoalNodeKind.TRANSPORTATION));
        assertTrue(kinds.contains(GoalNodeKind.SHOP));
        assertTrue(kinds.contains(GoalNodeKind.CURRENCY));
        assertTrue(kinds.contains(GoalNodeKind.SPELLBOOK));
        assertTrue(kinds.contains(GoalNodeKind.PRAYER));
        assertTrue(kinds.contains(GoalNodeKind.RECURRING_OPPORTUNITY));
        assertTrue(kinds.contains(GoalNodeKind.PREPARATION_ACTION));
    }
    private final GoalGraph graph = new GoalGraph();

    @Test
    public void majorLongTermGoalsHaveTypedDependencyPaths()
    {
        assertCovered(GoalType.MAX);
        assertCovered(GoalType.QUEST_CAPE);
        assertCovered(GoalType.BOWFA);
        assertCovered(GoalType.INFERNAL_CAPE);
        assertCovered(GoalType.DIARY_CAPE);
        assertCovered(GoalType.ELITE_COMBAT_ACHIEVEMENTS);
        assertCovered(GoalType.RAID_READY);
    }

    @Test
    public void bowfaPathIncludesAccessActivityItemAndResourceLayers()
    {
        GoalPathPreview preview = graph.previewFor(GoalType.BOWFA);
        boolean access = false;
        boolean activity = false;
        boolean item = false;
        boolean resource = false;
        for (GoalDependency dependency : preview.getDependencies())
        {
            access |= dependency.getKind() == GoalNodeKind.ACCESS;
            activity |= dependency.getKind() == GoalNodeKind.ACTIVITY;
            item |= dependency.getKind() == GoalNodeKind.ITEM;
            resource |= dependency.getKind() == GoalNodeKind.RESOURCE;
        }
        assertTrue(access && activity && item && resource);
    }

    private void assertCovered(GoalType goal)
    {
        assertFalse(goal.name(), graph.previewFor(goal)
                .getDependencies().isEmpty());
    }
}
