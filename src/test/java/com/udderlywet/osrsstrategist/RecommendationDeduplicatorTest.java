package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecommendationDeduplicatorTest
{
    @Test
    public void sharedSkillPrerequisiteBecomesOneActionWithMultipleReasons()
    {
        Recommendation quest = skill("quest-prereq:agility-70",
                "Train Agility to 70", "Required by a quest.", 40);
        Recommendation diary = skill("diary-prereq:agility-70",
                "Train Agility to 70", "Also completes a diary requirement.", 42);
        Recommendation clue = skill("clue-prereq:agility-70",
                "Train Agility to 70", "Unlocks the active clue route.", 41);

        List<Recommendation> result = new RecommendationDeduplicator()
                .deduplicate(Arrays.asList(quest, diary, clue));
        assertEquals(1, result.size());
        assertTrue(result.get(0).getReason().contains("quest"));
        assertTrue(result.get(0).getReason().contains("diary"));
        assertTrue(result.get(0).getReason().contains("clue"));
        assertEquals(48.0, result.get(0).getScore(), 0.001);
    }

    @Test
    public void evidenceLevelsNeverMergeAcrossSafetyBoundary()
    {
        Recommendation verified = skill("skill:agility", "Train Agility to 70",
                "Verified route.", 30);
        Recommendation check = new Recommendation("diary:agility",
                "Train Agility to 70", "Access unknown.", 100, null,
                RecommendationConfidence.CHECK_NEEDED, 60, 70,
                guidance(), CandidateSafetyEvidence.harmless(false));
        assertEquals(2, new RecommendationDeduplicator()
                .deduplicate(Arrays.asList(verified, check)).size());
    }

    @Test
    public void orderingDoesNotChangeTieWinner()
    {
        Recommendation alpha = skill("skill:alpha", "Train Mining to 70",
                "One route.", 40);
        Recommendation beta = skill("skill:beta", "Train Fishing to 70",
                "Another route.", 40);
        StrategyEngine engine = new StrategyEngine(null, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
        List<Recommendation> first = engine.buildPlayerQueue(
                Arrays.asList(beta, alpha));
        List<Recommendation> second = engine.buildPlayerQueue(
                Arrays.asList(alpha, beta));
        assertEquals(first.get(0).getId(), second.get(0).getId());
    }

    private static Recommendation skill(String id, String title,
            String reason, double score)
    {
        Skill skill = title.contains("Fishing") ? Skill.FISHING
                : title.contains("Mining") ? Skill.MINING : Skill.AGILITY;
        TrainingMethod method = new TrainingMethod(id + ":method", skill,
                1, 99, title, "Do the method.", 1, 1, 1,
                AttentionLevel.LOW, 20, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new Recommendation(id, title, reason, score,
                new TrainingPlan(method, reason,
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 60, 70, guidance(),
                CandidateSafetyEvidence.skill(false, skill));
    }

    private static RecommendationGuidance guidance()
    {
        return new RecommendationGuidance("Do the action.",
                "Verified: setup available.", "Safe location.", "Useful.");
    }
}
