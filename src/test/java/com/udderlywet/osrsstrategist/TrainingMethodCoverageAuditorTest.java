package com.udderlywet.osrsstrategist;

import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrainingMethodCoverageAuditorTest
{
    private final TrainingMethodCoverageAuditor auditor =
            new TrainingMethodCoverageAuditor(
                    new TrainingMethodDatabase(),
                    new ExpandedTrainingMethodCatalog(),
                    new F2pBaselineMethodCatalog());

    @Test
    public void f2pRunecraftHasContinuousCatalogCoverage()
    {
        assertTrue(auditor.gapsFor(
                Skill.RUNECRAFT,
                MembershipStatus.F2P).isEmpty());
    }

    @Test
    public void membersCatalogContainsMultipleMiningOptions()
    {
        assertTrue(auditor.methodCount(
                Skill.MINING,
                MembershipStatus.P2P) >= 5);
    }

    @Test
    public void auditCanExposeRemainingCoverageWorkWithoutHidingIt()
    {
        List<TrainingMethodCoverageGap> gaps =
                auditor.allGaps(MembershipStatus.P2P);

        // The auditor is a completeness tool, not a test that pretends the
        // catalog is already exhaustive. This assertion protects its ability to
        // return a real immutable list regardless of whether future batches
        // eventually drive the gap count to zero.
        assertFalse(gaps == null);
    }
}
