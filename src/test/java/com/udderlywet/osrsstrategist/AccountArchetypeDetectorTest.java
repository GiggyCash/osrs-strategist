package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountArchetypeDetectorTest
{
    private final AccountArchetypeDetector detector = new AccountArchetypeDetector();

    @Test
    public void detectsOneDefencePureAndProtectsDefence()
    {
        AccountSnapshot account = account(60, 80, 1, 80, 52, 80, 75);
        assertEquals(AccountArchetype.ONE_DEFENCE_PURE, detector.detect(account));
        assertFalse(AccountArchetypePolicy.mayTrain(
                AccountArchetype.ONE_DEFENCE_PURE, Skill.DEFENCE));
        assertTrue(AccountArchetypePolicy.mayTrain(
                AccountArchetype.ONE_DEFENCE_PURE, Skill.RANGED));
    }

    @Test
    public void detectsDefencePure()
    {
        assertEquals(AccountArchetype.DEFENCE_PURE,
                detector.detect(account(1, 1, 75, 1, 43, 1, 63)));
    }

    @Test
    public void detectsSkiller()
    {
        assertEquals(AccountArchetype.SKILLER,
                detector.detect(account(1, 1, 1, 1, 1, 1, 10)));
    }

    @Test
    public void detectsZerkerShape()
    {
        assertEquals(AccountArchetype.ZERKER,
                detector.detect(account(60, 80, 45, 80, 52, 80, 75)));
    }

    private static AccountSnapshot account(int attack, int strength, int defence,
            int ranged, int prayer, int magic, int hp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp);
        return new AccountSnapshot("Restricted", 0, "Main", 1000, 0L, levels, xp);
    }
}
