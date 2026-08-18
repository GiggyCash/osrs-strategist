package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StrategyCandidateFoundationTest
{
    @Test
    public void observedClueBecomesCheckNeededGenericRecommendation()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .clue(new ClueSnapshot(
                        true, "Hard", System.currentTimeMillis(),
                        RecommendationConfidence.VERIFIED))
                .build();
        StrategyContext context = new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX,
                false, false, new PreferenceProfile());

        StrategyCandidate candidate = new ClueCandidateProvider()
                .candidates(context).get(0);
        Recommendation recommendation = candidate.toRecommendation();

        assertEquals("clue:pending", recommendation.getId());
        assertEquals(0, recommendation.getCurrentLevel());
        assertTrue(RecommendationPresentation.compactHtml(recommendation)
                .contains("PREPARATION"));
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Test", 0, "Main", MembershipStatus.P2P,
                1, 1, 0L, levels, xp);
    }
}
