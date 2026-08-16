package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameKnowledgeManifestTest
{
    @Test
    public void everyPlannedKnowledgeDomainHasAnExplicitCoverageState()
    {
        GameKnowledgeManifest manifest = new GameKnowledgeManifest();
        assertTrue(manifest.hasTypedHomeForEveryDomain());
        assertEquals(GameKnowledgeDomain.values().length,
                manifest.all().size());
    }

    @Test
    public void broadSkeletonDoesNotPretendScaffoldedDomainsAreVerified()
    {
        GameKnowledgeManifest manifest = new GameKnowledgeManifest();
        assertEquals(KnowledgeCoverage.SCAFFOLDED,
                manifest.coverageOf(GameKnowledgeDomain.RAIDS));
        assertEquals(KnowledgeCoverage.PARTIAL,
                manifest.coverageOf(GameKnowledgeDomain.FARMING));
    }
}
