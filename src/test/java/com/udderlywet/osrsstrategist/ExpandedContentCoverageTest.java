package com.udderlywet.osrsstrategist;

import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpandedContentCoverageTest
{
    @Test
    public void everySkillHasCuratedTrainingMethods()
    {
        ExpandedTrainingMethodCatalog catalog = new ExpandedTrainingMethodCatalog();
        for (Skill skill : Skill.values())
        {
            assertFalse("Missing curated methods for " + skill,
                    catalog.methodsFor(skill).isEmpty());
        }
    }

    @Test
    public void majorSkillsHaveMultiplePlayStyles()
    {
        ExpandedTrainingMethodCatalog catalog = new ExpandedTrainingMethodCatalog();
        assertHasIntensity(catalog.methodsFor(Skill.MINING), TrainingIntensity.AFK);
        assertHasIntensity(catalog.methodsFor(Skill.MINING), TrainingIntensity.SWEATY);
        assertHasIntensity(catalog.methodsFor(Skill.FISHING), TrainingIntensity.AFK);
        assertHasIntensity(catalog.methodsFor(Skill.RUNECRAFT), TrainingIntensity.SWEATY);
        assertHasIntensity(catalog.methodsFor(Skill.AGILITY), TrainingIntensity.EFFICIENT);
        assertHasIntensity(catalog.methodsFor(Skill.SAILING), TrainingIntensity.BALANCED);
    }

    @Test
    public void f2pRoutesExistForAllF2pSkills()
    {
        ExpandedTrainingMethodCatalog catalog = new ExpandedTrainingMethodCatalog();
        for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
                Skill.RANGED, Skill.PRAYER, Skill.MAGIC, Skill.RUNECRAFT,
                Skill.HITPOINTS, Skill.CRAFTING, Skill.MINING, Skill.SMITHING,
                Skill.FISHING, Skill.COOKING, Skill.FIREMAKING, Skill.WOODCUTTING})
        {
            boolean found = false;
            for (CuratedTrainingMethod method : catalog.methodsFor(skill))
                found |= method.getMetadata().isFreeToPlayAllowed();
            assertTrue("No F2P method for " + skill, found);
        }
    }

    @Test
    public void crudeChairsAreOnlyAnEarlyConstructionBaseline()
    {
        TrainingMethod method = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.CONSTRUCTION).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(candidate -> "construction_crude_chairs".equals(
                        candidate.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals(32, method.getMaxLevel());
    }

    @Test
    public void potatoAllotmentsAreOnlyAnEarlyFarmingBaseline()
    {
        TrainingMethod method = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.FARMING).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(candidate -> "farming_falador_potatoes".equals(
                        candidate.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals(14, method.getMaxLevel());
    }

    @Test
    public void curseMethodNamesTheVerifiedTargetAndFailureThreshold()
    {
        TrainingMethod method = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.MAGIC).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(candidate -> "magic_f2p_curse".equals(
                        candidate.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertTrue(method.getInstructions().contains("Varrock Palace"));
        assertTrue(method.getInstructions().contains("-64"));
        assertTrue(method.getInstructions().contains("Monk of Zamorak"));
    }

    private static void assertHasIntensity(
            List<CuratedTrainingMethod> methods,
            TrainingIntensity intensity)
    {
        for (CuratedTrainingMethod method : methods)
            if (method.getMetadata().getIntensity() == intensity) return;
        throw new AssertionError("Missing " + intensity + " method");
    }
}
