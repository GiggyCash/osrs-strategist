package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressionObjectiveServiceTest
{
    private final MilestoneTracker service =
            new MilestoneTracker(new ProgressionObjectiveCatalog());

    @Test
    public void unknownCollectionStateConservativelyProtectsGracefulGrind()
    {
        TrainingPlan plan = new TrainingPlan(method("agility_rooftop"), "test",
                Confidence.VERIFIED, Collections.emptyList());
        assertTrue(service.protect(plan.method(), null));
    }

    @Test
    public void explicitlyCompletedObjectiveStopsProtection()
    {
        TrainingPlan plan = new TrainingPlan(method("agility_rooftop"), "test",
                Confidence.VERIFIED, Collections.emptyList());
        CollectionLogSnapshot log = new CollectionLogSnapshot(
                Collections.emptySet(),
                Collections.singleton("objective:graceful"),
                Collections.emptyMap(), Collections.emptyMap()
        );
        assertFalse(service.protect(plan.method(), log));
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
