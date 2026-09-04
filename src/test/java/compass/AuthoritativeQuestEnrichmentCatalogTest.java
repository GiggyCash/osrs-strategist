package compass;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthoritativeQuestEnrichmentCatalogTest
{
    @Test
    public void consolidatedSnapshotIsDuplicateSafeAndComplete()
    {
        QuestKnowledgeCatalog knowledge = new QuestKnowledgeCatalog();
        for (net.runelite.api.Quest quest : net.runelite.api.Quest.values())
            assertNotNull(quest.getName(), knowledge.definitionFor(quest.getName()));
    }

    @Test
    public void regeneratedBlankFieldsRemainExplicitlyResolved()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Ethically Acquired Antiquities");
        assertNotNull(quest);
        assertTrue(quest.getItemRequirements().isEmpty());
        assertFalse(quest.hasFieldUncertainty());
        assertFalse(quest.getStartLocation().isEmpty());
    }

    @Test
    public void nonBlankImportedFieldsRemainPlannerEvidence()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog().definitionFor(
                "Recipe for Disaster - King Awowogei");
        assertNotNull(quest);
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                value -> value.contains("greegree")));
        assertFalse(quest.hasFieldUncertainty());
    }

    @Test
    public void renamedMiniquestIdentityUsesCurrentPlannerRecord()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Vale Totems");
        assertNotNull(quest);
        assertTrue(quest.getUnlocks().contains("Vale Totems minigame"));
        assertFalse(quest.hasFieldUncertainty());
    }

    @Test
    public void distributedMiniquestRewardsUseTypedLocalStructureEvidence()
    {
        assertDistributedReward("The Frozen Door", "Nex access");
        assertDistributedReward("Barbarian Training",
                "Ancient Cavern");
    }

    private static void assertDistributedReward(String questName,
            String expectedUnlock)
    {
        QuestDefinition definition = new QuestKnowledgeCatalog()
                .definitionFor(questName);
        assertNotNull(definition);
        assertTrue(definition.getUnlocks().contains(expectedUnlock));
        assertFalse(definition.hasFieldUncertainty());
    }
}
