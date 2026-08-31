package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrimaryRecommendationContractTest
{
    private final ActionabilityPolicy policy =
            new ActionabilityPolicy();

    @Test
    public void verifiedButVagueTrainingMethodStillCannotLead()
    {
        TrainingMethod method = new TrainingMethod(
                "mining_test",
                Skill.MINING,
                1,
                99,
                "Mine something",
                "Mine the best sensible rock.",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                Confidence.VERIFIED,
                Collections.emptyList());
        Recommendation recommendation = new Recommendation(
                "skill:mining",
                "Train Mining to 80",
                "test",
                100,
                plan,
                Confidence.VERIFIED,
                70,
                80,
                null);

        assertFalse(policy.canLeadQueue(recommendation));
    }

    @Test
    public void structuredActionMakesVerifiedTrainingEligible()
    {
        TrainingMethod method = new TrainingMethod(
                "mining_test",
                Skill.MINING,
                1,
                99,
                "Mine iron",
                "Mine iron.",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                Confidence.VERIFIED,
                Collections.emptyList());
        Recommendation recommendation = new Recommendation(
                "skill:mining",
                "Train Mining to 80",
                "test",
                100,
                plan,
                Confidence.VERIFIED,
                70,
                80,
                new Guidance(
                        "Mine 1,000 iron ore.",
                        "Bring a rune pickaxe.",
                        "Mining Guild",
                        "Verified route"));

        assertTrue(policy.canLeadQueue(recommendation));
    }
}
