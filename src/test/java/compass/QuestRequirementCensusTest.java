package compass;

import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class QuestRequirementCensusTest
{
    @Test
    public void everyCurrentQuestIsAuditedWithoutSilentParserFailure()
    {
        QuestRequirementCensus census = new QuestRequirementCensus();
        assertEquals(Quest.values().length, census.getTotalQuests());
        assertEquals(0, census.getParseFailures());
        for (QuestRequirementCensus.Unresolved entry : census.getUnresolved())
        {
            assertFalse(entry.getQuest().trim().isEmpty());
            assertFalse(entry.getField().trim().isEmpty());
            assertFalse(entry.getReason().trim().isEmpty());
        }
    }
}
