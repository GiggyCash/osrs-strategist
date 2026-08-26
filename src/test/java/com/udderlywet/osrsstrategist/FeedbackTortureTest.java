package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class FeedbackTortureTest
{
    private final RecommendationDeduplicator deduplicator =
            new RecommendationDeduplicator();

    @Test
    public void laterNotTodayAndDislikeSuppressEverySemanticAliasImmediately()
    {
        for (FeedbackAction action : Arrays.asList(FeedbackAction.LATER,
                FeedbackAction.NOT_TODAY, FeedbackAction.DISLIKE))
        {
            Recommendation displayed = ready("quest-prereq:agility-70",
                    "Train Agility to 70", 50);
            Recommendation alias = ready("diary-prereq:agility-70",
                    "Train Agility to 70", 50);
            Recommendation unrelated = ready("skill:mining",
                    "Train Mining to 71", 30);
            PreferenceProfile profile = new PreferenceProfile();
            String semantic = deduplicator.semanticKey(displayed);
            profile.applySemantic(semantic, action);

            List<Recommendation> queue = engine().buildPlayerQueue(
                    Arrays.asList(alias, unrelated), context(profile));
            assertEquals(action.name(), "skill:mining", queue.get(0).getId());
            assertFalse(action.name(), queue.stream().anyMatch(value ->
                    value.getTitle().equals("Train Agility to 70")));
            if (action == FeedbackAction.DISLIKE)
                assertTrue(profile.semanticWeightFor(semantic) < 0.0);
            else
                assertEquals(0.0, profile.semanticWeightFor(semantic), 0.0);
        }
    }

    @Test
    public void accountSwitchAndReturnRestoresOnlyTheCorrectFeedbackState()
    {
        Recommendation action = ready("skill:fishing",
                "Train Fishing to 71", 40);
        String semantic = deduplicator.semanticKey(action);

        PreferenceProfile accountA = new PreferenceProfile();
        accountA.applySemantic(semantic, FeedbackAction.DISLIKE);
        Map<String, Double> aWeights = accountA.snapshot();
        Map<String, Long> aCooldowns = accountA.cooldownSnapshot();

        PreferenceProfile accountB = new PreferenceProfile();
        assertEquals(0.0, accountB.semanticWeightFor(semantic), 0.0);
        assertFalse(accountB.isSemanticOnCooldown(semantic));
        accountB.applySemantic(deduplicator.semanticKey(ready("skill:mining",
                "Train Mining to 71", 40)), FeedbackAction.DISLIKE);
        assertEquals(0.0, accountB.semanticWeightFor(semantic), 0.0);

        PreferenceProfile returnedA = new PreferenceProfile();
        returnedA.replaceAll(aWeights);
        returnedA.replaceCooldowns(aCooldowns);
        assertEquals(accountA.semanticWeightFor(semantic),
                returnedA.semanticWeightFor(semantic), 0.0);
        assertTrue(returnedA.isSemanticOnCooldown(semantic));
    }

    private static StrategyEngine engine()
    {
        return new StrategyEngine(null, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static StrategyContext context(PreferenceProfile profile)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Feedback", 700L, 0,
                "MAIN", MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        return new StrategyContext(StrategyDataBundle.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false, profile);
    }

    private static Recommendation ready(String id, String title, double score)
    {
        Skill skill = title.contains("Agility") ? Skill.AGILITY
                : title.contains("Fishing") ? Skill.FISHING : Skill.MINING;
        TrainingMethod method = new TrainingMethod(id + ":method", skill,
                1, 99, title, "Use the method.", 1, 1, 1,
                AttentionLevel.LOW, 20, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new Recommendation(id, title, "Useful account progress.", score,
                new TrainingPlan(method, "Useful account progress.",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 70, 71,
                new RecommendationGuidance("Do it now.",
                        "Setup verified.", "Safe location.", "Useful."),
                CandidateSafetyEvidence.skill(false, skill));
    }
}
