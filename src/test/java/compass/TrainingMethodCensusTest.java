package compass;

import java.time.LocalDate;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrainingMethodCensusTest
{
    @Test
    public void everyCurrentSkillHasValidStrategicMethods()
    {
        TrainingMethodCensus census = new TrainingMethodCensus();
        assertEquals(Skill.values().length, census.getSkillCount());
        assertTrue(census.getCuratedMethodCount() >= 125);
        assertEquals(0, census.getDuplicateIds());
        assertTrue(census.getInvalidMethods().toString(),
                census.getInvalidMethods().isEmpty());
        for (Skill skill : Skill.values())
        {
            TrainingMethodCensus.SkillCoverage coverage =
                    census.getBySkill().get(skill);
            assertTrue("too few strategic methods for " + skill,
                    coverage.getCuratedMethods() >= 2);
            assertTrue("no self-source route for " + skill,
                    coverage.hasSelfSource());
            assertTrue("no Hardcore-safe route for " + skill,
                    coverage.hasHardcoreSafe());
        }
    }

    @Test
    public void runeLiteActionsSupplementButDoNotReplaceStrategyMethods()
    {
        TrainingMethodCensus census = new TrainingMethodCensus();
        assertEquals(17, census.getSkillsWithRuneLiteActions());
        assertTrue(census.getRuneLiteActionCount() > 500);
        assertTrue(census.getBySkill().get(Skill.HUNTER)
                .getRuneLiteActions() > 60);
        assertEquals(0, census.getBySkill().get(Skill.SAILING)
                .getRuneLiteActions());
    }

    @Test
    public void currentLiveSepulchreLevelsOverridePinnedRuneLiteData()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        assertEquals(77, level(catalog,
                "runelite:agility:hallowed_sepulchre_floor_4"));
        assertEquals(87, level(catalog,
                "runelite:agility:hallowed_sepulchre_floor_5"));
        assertEquals(2,
                CurrentLiveSkillActionOverrides.levelOverrides().size());
        assertEquals(9,
                CurrentLiveSkillActionOverrides.xpOverrides().size());
        assertEquals(7,
                CurrentLiveSkillActionOverrides.suppressedStaleXp().size());
        assertEquals(112.0f, xp(catalog,
                "runelite:hunter:regular_bird_house"), 0.0f);
        assertEquals(969.0f, xp(catalog,
                "runelite:hunter:magic_bird_house"), 0.0f);
        assertEquals(0.0f, xp(catalog,
                "runelite:hunter:pyre_fox"), 0.0f);
        assertEquals(0.0f, xp(catalog,
                "runelite:agility:colossal_wyrm_basic_course"), 0.0f);
        assertFalse(CurrentLiveContentChanges.mayAffectPlanning(
                "2026-09-02-sweep-up-follow-up",
                LocalDate.of(2026, 8, 25)));
    }

    private static int level(RuneLiteSkillActionCatalog catalog, String id)
    {
        for (ActionDef action
                : catalog.actionsFor(Skill.AGILITY))
            if (id.equals(action.getId())) return action.getLevel();
        throw new AssertionError("Missing action " + id);
    }

    private static float xp(RuneLiteSkillActionCatalog catalog, String id)
    {
        for (Skill skill : Skill.values())
            for (ActionDef action
                    : catalog.actionsFor(skill))
                if (id.equals(action.getId())) return action.getXp();
        throw new AssertionError("Missing action " + id);
    }
}
