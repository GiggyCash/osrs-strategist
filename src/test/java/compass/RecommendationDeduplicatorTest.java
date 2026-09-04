package compass;

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
                Confidence.CHECK_NEEDED, 60, 70,
                guidance(), Safety.harmless(false));
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
        StrategyEngine engine = TestFixtures.strategyEngine(null, null, null, null,
                new ActionabilityPolicy(),
                new RecommendationIntelligenceService());
        List<Recommendation> first = engine.buildPlayerQueue(
                Arrays.asList(beta, alpha), null);
        List<Recommendation> second = engine.buildPlayerQueue(
                Arrays.asList(alpha, beta), null);
        assertEquals(first.get(0).getId(), second.get(0).getId());
    }

    @Test
    public void skillAndMinigameVersionsOfSameLoopBecomeOneAction()
    {
        Recommendation skill = skillMethod("skill:firemaking",
                "Train Firemaking to 70", "firemaking_wintertodt", 40,
                "skill-breakpoint");
        Recommendation minigame = new Recommendation("minigame:wintertodt",
                "Wintertodt", "Pyromancer progression.", 42, null,
                Confidence.VERIFIED, 0, 0, guidance(),
                Safety.skill(false, Skill.FIREMAKING))
                .withStrategicValue(StrategicValue.builder()
                        .resourceFit(0.6).evidence("wintertodt-rewards").build());

        List<Recommendation> result = new RecommendationDeduplicator()
                .deduplicate(Arrays.asList(skill, minigame));

        assertEquals(1, result.size());
        assertTrue(result.get(0).getReason().contains("Pyromancer"));
        assertTrue(result.get(0).getStrategicValue().getEvidenceIds()
                .contains("skill-breakpoint"));
        assertTrue(result.get(0).getStrategicValue().getEvidenceIds()
                .contains("wintertodt-rewards"));
    }

    private static Recommendation skill(String id, String title,
            String reason, double score)
    {
        Skill skill = title.contains("Fishing") ? Skill.FISHING
                : title.contains("Mining") ? Skill.MINING : Skill.AGILITY;
        TrainingMethod method = new TrainingMethod(id + ":method", skill,
                1, 99, title, "Do the method.", 1, 1, 1,
                AttentionLevel.LOW, 20, 2, Collections.emptyList(),
                Confidence.VERIFIED);
        return new Recommendation(id, title, reason, score,
                new TrainingPlan(method, reason,
                        Confidence.VERIFIED, Collections.emptyList()),
                Confidence.VERIFIED, 60, 70, guidance(),
                Safety.skill(false, skill));
    }

    private static Recommendation skillMethod(String id, String title,
            String methodId, double score, String evidence)
    {
        TrainingMethod method = new TrainingMethod(methodId, Skill.FIREMAKING,
                1, 99, title, "Do the method.", 1, 1, 1,
                AttentionLevel.LOW, 20, 2, Collections.emptyList(),
                Confidence.VERIFIED);
        return new Recommendation(id, title, "Firemaking breakpoint.", score,
                new TrainingPlan(method, "route",
                        Confidence.VERIFIED, Collections.emptyList()),
                Confidence.VERIFIED, 60, 70, guidance(),
                Safety.skill(false, Skill.FIREMAKING))
                .withStrategicValue(StrategicValue.builder()
                        .unlockValue(0.5).evidence(evidence).build());
    }

    private static Guidance guidance()
    {
        return new Guidance("Do the action.",
                "Verified: setup available.", "Safe location.", "Useful.");
    }
}
