package compass;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiaryTaskCatalogTest
{
    @Test
    public void allTwelveRegionsAndFortyEightTiersHaveTaskEvidence()
    {
        DiaryTaskCatalog catalog = new DiaryTaskCatalog();
        assertEquals(378, catalog.all().size());
        assertEquals(12, catalog.census().size());
        int tiers = 0;
        int requirements = 0;
        int transports = 0;
        for (Map<DiaryTier, Integer> region : catalog.census().values())
        {
            assertEquals(4, region.size());
            tiers += region.size();
            for (Integer count : region.values()) assertTrue(count > 0);
        }
        for (DiaryTaskDefinition task : catalog.all())
        {
            assertFalse(task.getTask().trim().isEmpty());
            requirements += task.getRequirements().size();
            if (task.isTransportRelevant()) transports++;
        }
        assertEquals(48, tiers);
        assertTrue(requirements > 250);
        assertTrue(transports > 20);
    }

    @Test
    public void directSkillAndQuestEvidenceRemainStructured()
    {
        DiaryTaskCatalog catalog = new DiaryTaskCatalog();
        DiaryTaskDefinition teleport = catalog.forTier("Ardougne",
                DiaryTier.MEDIUM).stream()
                .filter(task -> task.getTask().contains("Ardougne Teleport"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(teleport.getRequirements().stream().anyMatch(requirement ->
                requirement.getKind() == DiaryTaskRequirement.Kind.SKILL
                        && requirement.getLevel() == 51));
        assertTrue(teleport.getRequirements().stream().anyMatch(requirement ->
                requirement.getKind() == DiaryTaskRequirement.Kind.QUEST
                        && "Plague City".equals(requirement.getQuest())));
        assertTrue(teleport.isTransportRelevant());
    }

}
