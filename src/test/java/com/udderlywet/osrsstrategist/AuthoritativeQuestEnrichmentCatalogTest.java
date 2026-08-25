package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthoritativeQuestEnrichmentCatalogTest
{
    @Test
    public void snapshotIsDuplicateSafeAndCoversEveryImportedIdentity()
    {
        AuthoritativeQuestEnrichmentCatalog enrichment =
                new AuthoritativeQuestEnrichmentCatalog();
        QuestKnowledgeCatalog knowledge = new QuestKnowledgeCatalog();
        for (AuthoritativeQuestRequirementCatalog.Record requirement
                : new AuthoritativeQuestRequirementCatalog().all().values())
            if (enrichment.recordFor(requirement.getName()) == null)
                assertTrue(requirement.getName(), knowledge.definitionFor(
                        requirement.getName()).getFieldUncertainties().isEmpty());
        assertEquals(enrichment.all().size(), enrichment.all().keySet().size());
    }

    @Test
    public void regeneratedBlankBucketFieldsHaveExplicitStrictEvidence()
    {
        AuthoritativeQuestEnrichmentCatalog catalog =
                new AuthoritativeQuestEnrichmentCatalog();
        AuthoritativeQuestEnrichmentCatalog.Record quest =
                catalog.recordFor("Ethically Acquired Antiquities");
        assertNotNull(quest);
        assertTrue(quest.getItems().isEmpty());
        assertEquals(AuthoritativeQuestEnrichmentCatalog.EvidenceState.NONE,
                quest.getItemState());
        assertTrue(quest.hasItemEvidence());
        assertTrue(quest.hasStrictItemEvidence());
        assertTrue(catalog.hasStrictFieldEvidence());
    }

    @Test
    public void nonBlankLegacyFieldsRemainValueEvidence()
    {
        AuthoritativeQuestEnrichmentCatalog.Record quest =
                new AuthoritativeQuestEnrichmentCatalog()
                        .recordFor("Recipe for Disaster - King Awowogei");
        assertNotNull(quest);
        assertTrue(quest.getItems().contains("greegree"));
        assertEquals(AuthoritativeQuestEnrichmentCatalog.EvidenceState.VALUE,
                quest.getItemState());
        assertTrue(quest.hasStrictItemEvidence());
        assertTrue(quest.hasRewardEvidence());
    }

    @Test
    public void renamedMiniquestIdentityMapsToCurrentWikiPage()
    {
        AuthoritativeQuestEnrichmentCatalog.Record quest =
                new AuthoritativeQuestEnrichmentCatalog().recordFor("Vale Totems");
        assertNotNull(quest);
        assertEquals("Vale Totems (miniquest)", quest.getName());
        assertTrue(quest.hasStrictItemEvidence());
    }
}
