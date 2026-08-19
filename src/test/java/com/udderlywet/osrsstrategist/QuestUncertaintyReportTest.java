package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuestUncertaintyReportTest
{
    @Test
    public void authoritativeEnrichmentLeavesNoBlanketImportedUncertainty()
    {
        QuestUncertaintyReport report = new QuestUncertaintyReport();
        assertEquals(0, report.uncertainQuestCount());
        assertTrue(report.all().isEmpty());
        assertTrue(report.countsByCategory().isEmpty());
    }
}
