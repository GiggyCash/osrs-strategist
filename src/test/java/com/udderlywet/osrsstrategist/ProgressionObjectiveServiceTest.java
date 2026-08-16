package com.udderlywet.osrsstrategist;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
}
