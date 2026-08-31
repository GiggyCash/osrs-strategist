package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResourceDetourCandidateProviderTest
{
    private final ResourceDetourCandidateProvider provider = new ResourceDetourCandidateProvider();

    @Test
    public void ironTemporossDetourCanActuallyLeadDoNext()
    {
        Recommendation candidate = find(provider.candidates(context(account(1), GoalType.MAX)),
                "detour:tempoross-planks");
        assertNotNull(candidate);
        assertEquals(Confidence.VERIFIED, candidate.getConfidence());
        assertNotNull(candidate.getGuidance());
        assertTrue(candidate.getGuidance().getAction().contains("Tempoross"));
        assertTrue(candidate.getGuidance().getNote().contains("cross-skill"));
        assertTrue(new ActionabilityPolicy().canLeadQueue(candidate));
    }

    @Test
    public void uimDoesNotUseBankBasedResourceDetourProvider()
    {
        assertTrue(provider.candidates(context(account(2), GoalType.MAX)).isEmpty());
    }

    @Test
    public void gearOnlyGoalDoesNotInventConstructionResourceDetour()
    {
        assertNull(find(provider.candidates(context(account(1), GoalType.GEAR_TARGET)),
                "detour:tempoross-planks"));
    }

    private static StrategyContext context(AccountSnapshot account, GoalType goal)
    {
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL, goal,
                true, false, false, new PreferenceProfile());
    }

    private static Recommendation find(List<Recommendation> candidates, String id)
    {
        for (Recommendation candidate : candidates)
            if (id.equals(candidate.getId())) return candidate;
        return null;
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = 60;
            if (skill == Skill.HITPOINTS) level = 70;
            if (skill == Skill.CONSTRUCTION) level = 40;
            if (skill == Skill.FISHING) level = 50;
            if (skill == Skill.FIREMAKING) level = 60;
            if (skill == Skill.WOODCUTTING) level = 50;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        return new AccountSnapshot("Detour Test", typeCode,
                AccountMode.fromTypeCode(typeCode).name(), MembershipStatus.P2P,
                1, total, totalXp, levels, xp);
    }
}
