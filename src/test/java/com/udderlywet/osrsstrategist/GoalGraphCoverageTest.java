package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoalGraphCoverageTest
{
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
