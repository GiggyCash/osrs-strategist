package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestUncertaintyReportTest
{
    @Test
    public void reportExpandsEveryFieldIntoDeterministicCategories()
    {
        QuestUncertaintyReport report = new QuestUncertaintyReport();
        assertEquals(104, report.uncertainQuestCount());
        assertFalse(report.all().isEmpty());
        assertTrue(report.countsByCategory().get(
                QuestUncertaintyEntry.Category.ITEMS) > 0);
        assertTrue(report.countsByCategory().get(
                QuestUncertaintyEntry.Category.COMBAT) > 0);
        assertTrue(report.countsByCategory().get(
                QuestUncertaintyEntry.Category.IRREVERSIBLE_XP) > 0);
        for (QuestUncertaintyEntry entry : report.all())
        {
            assertFalse(entry.getQuestName().trim().isEmpty());
            assertFalse(entry.getDetail().trim().isEmpty());
        }
    }
}
