package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameKnowledgeCoverageRegistryTest
{
    @Test
    public void registryContainsEveryDeclaredKnowledgeArea()
    {
        GameKnowledgeCoverageRegistry registry =
                new GameKnowledgeCoverageRegistry();

        assertTrue(registry.containsEveryDeclaredArea());
        assertEquals(GameKnowledgeArea.values().length,
                registry.all().size());
    }

    @Test
    public void plannerBehaviorCanBeVerifiedWhileGameDataRemainsHonest()
    {
        GameKnowledgeCoverageRegistry registry =
                new GameKnowledgeCoverageRegistry();

        assertEquals(KnowledgeCoverage.VERIFIED,
                registry.coverageOf(GameKnowledgeArea.HEALTHY_VARIETY));
        assertEquals(KnowledgeCoverage.VERIFIED,
                registry.coverageOf(GameKnowledgeArea.CONFIDENCE_AND_EVIDENCE));

        assertEquals(KnowledgeCoverage.PARTIAL,
                registry.coverageOf(GameKnowledgeArea.PVM_GEAR_LOADOUTS));
        assertEquals(KnowledgeCoverage.SCAFFOLDED,
                registry.coverageOf(GameKnowledgeArea.CLUE_STEPS));
        assertFalse(registry.notVerified().isEmpty());
    }
}
