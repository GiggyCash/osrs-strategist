package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProgressionObjectiveServiceTest
{
    private final ProgressionObjectiveService service =
            new ProgressionObjectiveService(new ProgressionObjectiveCatalog());

    @Test
    public void unknownCollectionStateConservativelyProtectsGracefulGrind()
    {
        TrainingPlan plan = new TrainingPlan(method("agility_rooftop"), "test");
        assertTrue(service.shouldProtect(plan, null));
    }

    @Test
    public void explicitlyCompletedObjectiveStopsProtection()
    {
        TrainingPlan plan = new TrainingPlan(method("agility_rooftop"), "test");
        CollectionLogSnapshot log = new CollectionLogSnapshot(
                Collections.emptySet(),
                Collections.singleton("objective:graceful")
        );
        assertFalse(service.shouldProtect(plan, log));
    }

    @Test
    public void sharedMethodStaysProtectedUntilEveryKnownObjectiveIsComplete()
    {
        TrainingPlan motherlode = new TrainingPlan(
                syntheticMethod("mining_motherlode", Skill.MINING), "test");

        CollectionLogSnapshot prospectorOnly = new CollectionLogSnapshot(
                Collections.emptySet(),
                Collections.singleton("objective:prospector"));

        assertTrue(service.shouldProtect(motherlode, prospectorOnly));
        ProgressionObjectiveDefinition next =
                service.activeObjective(motherlode, prospectorOnly);
        assertNotNull(next);
        assertEquals("objective:coal-bag", next.getId());

        CollectionLogSnapshot allKnown = new CollectionLogSnapshot(
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(
                        "objective:prospector",
                        "objective:coal-bag",
                        "objective:gem-bag")));
        assertFalse(service.shouldProtect(motherlode, allKnown));
    }

    @Test
    public void forestryCanRepresentSeveralLongFormRewards()
    {
        ProgressionObjectiveCatalog catalog = new ProgressionObjectiveCatalog();
        assertTrue(catalog.objectivesForMethod("woodcutting_forestry").size() >= 3);
        assertTrue(catalog.objectivesForMethod("hunter_rumours").size() >= 2);
    }

    private static TrainingMethod method(String id)
    {
        for (Skill skill : Skill.values())
        {
            for (TrainingMethod method : new TrainingMethodDatabase().methodsFor(skill))
            {
                if (id.equals(method.getId())) return method;
            }
        }
        throw new AssertionError("Missing method " + id);
    }

    private static TrainingMethod syntheticMethod(String id, Skill skill)
    {
        return new TrainingMethod(
                id, skill, 1, 99, id, "test",
                10, 10, 10, AttentionLevel.MODERATE,
                20, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
    }
}
