package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameKnowledgeImportPolicyTest
{
    private final GameKnowledgeImportPolicy policy =
            new GameKnowledgeImportPolicy();

    @Test
    public void wikiImportIsStagedUntilValidated()
    {
        KnowledgeRecordMetadata staged = new KnowledgeRecordMetadata(
                "method:test",
                GameKnowledgeDomain.TRAINING_METHODS,
                KnowledgeSource.OSRS_WIKI,
                "wiki-revision-1",
                0L,
                false
        );
        assertFalse(policy.mayUseForPlanning(staged));
    }

    @Test
    public void verifiedRecordCanEnterPlanningDataset()
    {
        KnowledgeRecordMetadata verified = new KnowledgeRecordMetadata(
                "method:test",
                GameKnowledgeDomain.TRAINING_METHODS,
                KnowledgeSource.OSRS_WIKI,
                "wiki-revision-1",
                1L,
                true
        );
        assertTrue(policy.mayUseForPlanning(verified));
    }
}
