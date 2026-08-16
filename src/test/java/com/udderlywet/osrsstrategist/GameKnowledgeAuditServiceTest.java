package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Prevents future refactors from silently collapsing broad game coverage. */
public class GameKnowledgeAuditServiceTest
{
    @Test
    public void everyTrainableSkillHasMultipleKnownRoutes()
    {
        GameKnowledgeAuditReport report = service().audit();
        for (Skill skill : Skill.values())
        {
            assertTrue(skill.getName() + " should have at least two route choices",
                    report.trainingMethodsFor(skill) >= 2);
        }
    }

    @Test
    public void broadCatalogSurfacesStayAboveFoundationFloor()
    {
        GameKnowledgeAuditReport report = service().audit();
        assertTrue("Minigame/repeatable activity catalog unexpectedly shrank",
                report.getMinigames() >= 45);
        assertTrue("PvM identity catalog unexpectedly shrank",
                report.getPvmActivities() >= 20);
        assertTrue("Progression-objective catalog unexpectedly shrank",
                report.getProgressionObjectives() >= 35);
        assertTrue("Money-making catalog unexpectedly shrank",
                report.getMoneyMakingMethods() >= 20);
        assertTrue("Resource-source catalog unexpectedly shrank",
                report.getResourceSources() >= 20);
        assertTrue(report.meetsFoundationBreadthFloor());
    }

    private static GameKnowledgeAuditService service()
    {
        return new GameKnowledgeAuditService(
                new TrainingMethodDatabase(),
                new ExpandedTrainingMethodCatalog(),
                new F2pBaselineMethodCatalog(),
                new MinigameCatalog(),
                new PvmActivityCatalog(),
                new ProgressionObjectiveCatalog(),
                new MoneyMakingCatalog(),
                new ResourceSourceCatalog());
    }
}
