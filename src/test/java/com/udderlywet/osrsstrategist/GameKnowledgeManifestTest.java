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
    public void manifestDistinguishesUsefulPartialCoverageFromScaffolding()
    {
        GameKnowledgeManifest manifest = new GameKnowledgeManifest();

        // Raid identities/readiness have useful implemented coverage, but are
        // deliberately not called VERIFIED until exhaustive encounter data and
        // prerequisites have a maintained source-backed import.
        assertEquals(KnowledgeCoverage.PARTIAL,
                manifest.coverageOf(GameKnowledgeDomain.RAIDS));
        assertEquals(KnowledgeCoverage.PARTIAL,
                manifest.coverageOf(GameKnowledgeDomain.FARMING));

        // STASH and miniquest knowledge still have typed homes without enough
        // exhaustive verified records to claim practical domain coverage.
        assertEquals(KnowledgeCoverage.SCAFFOLDED,
                manifest.coverageOf(GameKnowledgeDomain.STASH));
        assertEquals(KnowledgeCoverage.SCAFFOLDED,
                manifest.coverageOf(GameKnowledgeDomain.MINIQUESTS));
    }

    @Test
    public void maintainedRuneLiteSkillSetIsTheOnlyVerifiedDomainForNow()
    {
        GameKnowledgeManifest manifest = new GameKnowledgeManifest();
        assertEquals(KnowledgeCoverage.VERIFIED,
                manifest.coverageOf(GameKnowledgeDomain.SKILLS));
        for (GameKnowledgeDomain domain : GameKnowledgeDomain.values())
        {
            if (domain == GameKnowledgeDomain.SKILLS) continue;
            assertTrue("Only the maintained skill identity set is fully verified",
                    manifest.coverageOf(domain) != KnowledgeCoverage.VERIFIED);
        }
    }
}
