package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
    public void explicitBlankBucketFieldsAreVerifiedNone()
    {
        AuthoritativeQuestEnrichmentCatalog.Record quest =
                new AuthoritativeQuestEnrichmentCatalog()
                        .recordFor("Ethically Acquired Antiquities");
        assertNotNull(quest);
        assertTrue(quest.getItems().isEmpty());
        assertTrue(quest.hasItemEvidence());
        assertTrue(quest.hasCombatEvidence());
    }

    @Test
    public void recipeSubquestAliasesResolveToTheirOwnEvidence()
    {
        AuthoritativeQuestEnrichmentCatalog.Record quest =
                new AuthoritativeQuestEnrichmentCatalog()
                        .recordFor("Recipe for Disaster - King Awowogei");
        assertNotNull(quest);
        assertTrue(quest.getItems().contains("greegree"));
        assertTrue(quest.hasRewardEvidence());
    }
}
