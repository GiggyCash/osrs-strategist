package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RestrictedBuildDetectorTest
{
    private final RestrictedBuildDetector detector = new RestrictedBuildDetector();

    @Test
    public void identifiesLikelyOneDefencePureWithoutEnforcingIt()
    {
        AccountSnapshot account = account(MembershipStatus.P2P,
                60, 70, 1, 80, 52, 82, 70, 50);
        RestrictedBuildSuggestion result = detector.suggest(account);
        assertEquals(RestrictedBuildType.ONE_DEFENCE_PURE, result.getType());
        assertEquals(RecommendationConfidence.CHECK_NEEDED, result.getConfidence());
    }

    @Test
    public void identifiesLikelyDefencePure()
    {
        AccountSnapshot account = account(MembershipStatus.P2P,
                1, 1, 75, 1, 1, 1, 60, 40);
        assertEquals(RestrictedBuildType.DEFENCE_PURE,
                detector.suggest(account).getType());
    }

    @Test
    public void identifiesLikelySkiller()
    {
        AccountSnapshot account = account(MembershipStatus.P2P,
                1, 1, 1, 1, 1, 1, 10, 80);
        assertEquals(RestrictedBuildType.SKILLER,
                detector.suggest(account).getType());
    }

    private static AccountSnapshot account(MembershipStatus membership,
            int attack, int strength, int defence, int ranged, int prayer,
            int magic, int hp, int nonCombat)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, nonCombat); xp.put(skill, 0); }
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp);
        return new AccountSnapshot("Test", 0, "Main", membership,
                0, 500, 0L, levels, xp);
    }
}
