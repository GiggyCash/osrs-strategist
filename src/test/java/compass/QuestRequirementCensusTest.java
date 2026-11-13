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
        assertEquals(0, census.getUnsupportedExpressions());
        assertEquals(192, census.getQuestsWithItemRequirements());
        assertEquals(89, census.getFullyExecutable());
        assertEquals(127, census.getPartiallyExecutable());
        assertEquals(342, census.getExplicitCheckExpressions());
        for (QuestRequirementCensus.Unresolved entry : census.getUnresolved())
        {
            assertFalse(entry.getQuest().trim().isEmpty());
            assertFalse(entry.getField().trim().isEmpty());
            assertFalse(entry.getReason().trim().isEmpty());
        }
    }
}
