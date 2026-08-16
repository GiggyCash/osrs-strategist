package com.udderlywet.osrsstrategist;

import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
        assertHasIntensity(catalog.methodsFor(Skill.FISHING), TrainingIntensity.RELAXED);
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

    private static void assertHasIntensity(
            List<CuratedTrainingMethod> methods,
            TrainingIntensity intensity)
    {
        for (CuratedTrainingMethod method : methods)
            if (method.getMetadata().getIntensity() == intensity) return;
        throw new AssertionError("Missing " + intensity + " method");
    }
}
