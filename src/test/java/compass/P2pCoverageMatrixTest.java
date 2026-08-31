package compass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Guards broad members training coverage. This is intentionally a route-level
 * test, not an XP-rate claim: every normal trainable skill should have at least
 * one curated method at representative progression bands.
 */
public class P2pCoverageMatrixTest
{
    private static final int[] LEVELS = {
            1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 98
    };

    private static final Set<Skill> INDIRECT_ONLY = new HashSet<>(Arrays.asList(
            Skill.HITPOINTS
    ));

    @Test
    public void everyP2pSkillHasCuratedRouteAcrossProgressionBands()
    {
        ExpandedTrainingMethodCatalog expanded =
                new ExpandedTrainingMethodCatalog();
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();

        for (Skill skill : Skill.values())
        {
            if (INDIRECT_ONLY.contains(skill)) continue;
            for (int level : LEVELS)
            {
                boolean found = hasRoute(expanded.methodsFor(skill), level);
                if (!found)
                {
                    for (TrainingMethod method : legacy.methodsFor(skill))
                    {
                        if (method.supportsLevel(level)
                                && ContentAccessRules.isMethodAvailable(
                                        method, MembershipStatus.P2P))
                        {
                            found = true;
                            break;
                        }
                    }
                }
                assertTrue(skill.getName() + " has no P2P route at level " + level,
                        found);
            }
        }
    }

    @Test
    public void membersAccountCanStillUseExplicitF2pRoutes()
    {
        F2pBaselineMethodCatalog baseline = new F2pBaselineMethodCatalog();
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod candidate : baseline.methodsFor(skill))
            {
                assertTrue(ContentAccessRules.isMethodAvailable(
                        candidate.getMethod(), MembershipStatus.P2P));
            }
        }
    }

    private static boolean hasRoute(
            List<CuratedTrainingMethod> methods,
            int level)
    {
        for (CuratedTrainingMethod candidate : methods)
        {
            if (candidate == null || candidate.getMethod() == null) continue;
            TrainingMethod method = candidate.getMethod();
            if (method.supportsLevel(level)
                    && ContentAccessRules.isMethodAvailable(
                            method, MembershipStatus.P2P))
            {
                return true;
            }
        }
        return false;
    }
}
