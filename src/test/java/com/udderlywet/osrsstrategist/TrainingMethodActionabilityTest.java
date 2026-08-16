package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Verifies live readiness participates in HOW-method selection, not just DO NEXT. */
public class TrainingMethodActionabilityTest
{
    @Test
    public void verifiedMethodWinsCloseInternalSkillContest()
    {
        TrainingMethod unresolved = method(
                "unresolved", 11.0,
                Collections.singletonList("Unknown route requirement"),
                RecommendationConfidence.CHECK_NEEDED);
        TrainingMethod ready = method(
                "ready", 10.0,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED);

        TrainingMethodDatabase database = new TrainingMethodDatabase()
        {
            @Override
            public List<TrainingMethod> methodsFor(Skill skill)
            {
                return skill == Skill.COOKING
                        ? Arrays.asList(unresolved, ready)
                        : Collections.emptyList();
            }
        };

        TrainingMethodSelector selector = new TrainingMethodSelector(
                database,
                new RequirementEvidenceEngine((FarmingAccessEvaluator) null),
                null,
                null,
                new TrainingMethodPolicy(),
                new ActionabilityScoringPolicy());

        TrainingPlan plan = selector.select(
                null, Skill.COOKING, 50,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false);

        assertEquals("ready", plan.getMethod().getId());
        assertEquals(RecommendationConfidence.VERIFIED, plan.getConfidence());
    }

    @Test
    public void largeMethodAdvantageStillBeatsReadyAlternative()
    {
        TrainingMethod important = method(
                "important", 20.0,
                Collections.singletonList("Confirm access"),
                RecommendationConfidence.CHECK_NEEDED);
        TrainingMethod easy = method(
                "easy", 10.0,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED);

        TrainingMethodDatabase database = new TrainingMethodDatabase()
        {
            @Override
            public List<TrainingMethod> methodsFor(Skill skill)
            {
                return skill == Skill.COOKING
                        ? Arrays.asList(important, easy)
                        : Collections.emptyList();
            }
        };

        TrainingMethodSelector selector = new TrainingMethodSelector(
                database,
                new RequirementEvidenceEngine((FarmingAccessEvaluator) null),
                null,
                null,
                new TrainingMethodPolicy(),
                new ActionabilityScoringPolicy());

        TrainingPlan plan = selector.select(
                null, Skill.COOKING, 50,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false);

        assertEquals("important", plan.getMethod().getId());
    }

    private static TrainingMethod method(
            String id,
            double score,
            List<String> requirements,
            RecommendationConfidence confidence)
    {
        return new TrainingMethod(
                id, Skill.COOKING, 1, 99,
                id, "test", score, score, score,
                AttentionLevel.MODERATE, 10, 1,
                requirements, confidence);
    }
}
