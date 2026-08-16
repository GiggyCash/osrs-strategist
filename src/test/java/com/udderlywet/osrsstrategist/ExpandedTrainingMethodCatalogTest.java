package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpandedTrainingMethodCatalogTest
{
    private final ExpandedTrainingMethodCatalog catalog = new ExpandedTrainingMethodCatalog();

    @Test
    public void everyTrainableSkillHasConcreteExpandedCoverage()
    {
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.HITPOINTS) continue;
            assertFalse(skill.getName(), catalog.methodsFor(skill).isEmpty());
        }
    }

    @Test
    public void catalogContainsF2pAndMembersVariants()
    {
        assertTrue(catalog.methodsFor(Skill.FISHING).stream()
                .anyMatch(method -> !method.isMembersOnly()));
        assertTrue(catalog.methodsFor(Skill.FISHING).stream()
                .anyMatch(TrainingMethod::isMembersOnly));
    }

    @Test
    public void catalogMarksDangerousWildernessVariants()
    {
        assertTrue(catalog.methodsFor(Skill.PRAYER).stream()
                .anyMatch(TrainingMethod::isWilderness));
        assertTrue(catalog.methodsFor(Skill.HUNTER).stream()
                .anyMatch(TrainingMethod::isWilderness));
    }
}
