package compass;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Prevents silent exact-planner coverage loss from method-id drift. */
public class MethodExecutionProfileCoverageTest
{
    @Test
    public void everyExecutionProfileMapsToARealTrainingMethod()
    {
        Set<String> methodIds = allMethodIds();
        for (String profileId : new MethodExecutionProfileCatalog().all().keySet())
        {
            assertTrue(
                    "Execution profile points at missing training method: " + profileId,
                    methodIds.contains(profileId));
        }
    }

    @Test
    public void deterministicPlannerTouchesEveryNonCombatSkillWithStableActions()
    {
        Set<Skill> covered = new HashSet<>();
        Set<String> profileIds = new MethodExecutionProfileCatalog().all().keySet();
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog f2p = new F2pBaselineMethodCatalog();
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();

        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod method : expanded.methodsFor(skill))
                if (profileIds.contains(method.getMethod().getId())) covered.add(skill);
            for (CuratedTrainingMethod method : f2p.methodsFor(skill))
                if (profileIds.contains(method.getMethod().getId())) covered.add(skill);
            for (TrainingMethod method : legacy.methodsFor(skill))
                if (profileIds.contains(method.getId())) covered.add(skill);
        }

        // Combat, Slayer, and Sailing need target/task/activity-aware planners
        // rather than pretending their XP comes from one stable repeatable unit.
        Skill[] expected = {
                Skill.AGILITY, Skill.COOKING, Skill.CONSTRUCTION,
                Skill.CRAFTING, Skill.FARMING, Skill.FIREMAKING,
                Skill.FISHING, Skill.FLETCHING, Skill.HERBLORE,
                Skill.HUNTER, Skill.MAGIC, Skill.MINING, Skill.PRAYER,
                Skill.RUNECRAFT, Skill.SMITHING, Skill.THIEVING,
                Skill.WOODCUTTING
        };
        for (Skill skill : expected)
        {
            assertTrue("No deterministic execution profile for " + skill,
                    covered.contains(skill));
        }
    }

    private static Set<String> allMethodIds()
    {
        Set<String> ids = new HashSet<>();
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog f2p = new F2pBaselineMethodCatalog();
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod method : expanded.methodsFor(skill))
                ids.add(method.getMethod().getId());
            for (CuratedTrainingMethod method : f2p.methodsFor(skill))
                ids.add(method.getMethod().getId());
            for (TrainingMethod method : legacy.methodsFor(skill))
                ids.add(method.getId());
        }
        return ids;
    }
}
