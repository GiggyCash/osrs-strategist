package com.udderlywet.osrsstrategist;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProgressionProtectedMilestoneTest
{
    @Test
    public void gracefulStyleMethodProtectsTrackedCheckpointFromVarietyRotation()
    {
        TrainingMethod method = new TrainingMethod(
                "agility_rooftop", Skill.AGILITY, 1, 99,
                "Rooftops", "Keep earning marks.",
                10, 10, 10, AttentionLevel.ACTIVE, 20, 2,
                Collections.emptyList(), Confidence.VERIFIED,
                false, false, true);
        TrainingPlan plan = new TrainingPlan(
                method, "Graceful progression", Confidence.VERIFIED,
                Collections.emptyList());
        Recommendation recommendation = new Recommendation(
                "skill:agility", "Train Agility to 60", "Graceful",
                50.0, plan, Confidence.VERIFIED, 59, 60);

        TrackedMilestone tracked = new MilestoneTracker().fromRecommendations(
                Collections.singletonList(recommendation));
        assertTrue(tracked.isProgressionProtected());
    }
}
