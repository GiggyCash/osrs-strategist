package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrainingMethodSelectorEvidenceTest
{
    @Test
    public void blockedHighScoreMethodFallsBackToUsableAlternative()
    {
        TrainingMethod blockedFast = method("fast", 20.0);
        TrainingMethod usableSlower = method("safe", 10.0);

        TrainingMethodDatabase database = new TrainingMethodDatabase()
        {
            @Override
            public List<TrainingMethod> methodsFor(Skill skill)
            {
                return Arrays.asList(blockedFast, usableSlower);
            }
        };

        RequirementEvidenceEngine evidence = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()))
        {
            @Override
            public List<RequirementCheck> evaluate(
                    StrategyDataBundle data,
                    TrainingMethod method)
            {
                if ("fast".equals(method.getId()))
                {
                    return Collections.singletonList(
                            new RequirementCheck(
                                    "locked",
                                    "Required unlock",
                                    RequirementState.BLOCKED,
                                    "Known locked"
                            )
                    );
                }

                return Collections.singletonList(
                        new RequirementCheck(
                                "ready",
                                "Required unlock",
                                RequirementState.VERIFIED,
                                "Verified"
                        )
                );
            }
        };

        TrainingMethodSelector selector =
                new TrainingMethodSelector(database, evidence);

        TrainingPlan plan = selector.select(
                null,
                Skill.MINING,
                50,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME
        );

        assertEquals("safe", plan.getMethod().getId());
        assertEquals(
                RecommendationConfidence.VERIFIED,
                plan.getConfidence()
        );
    }

    private static TrainingMethod method(String id, double score)
    {
        return new TrainingMethod(
                id,
                Skill.MINING,
                1,
                99,
                id,
                "test",
                score,
                score,
                score,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.singletonList("Required unlock"),
                RecommendationConfidence.CHECK_NEEDED
        );
    }
}
