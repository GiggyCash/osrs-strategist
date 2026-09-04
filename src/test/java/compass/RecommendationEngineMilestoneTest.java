package compass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Guards the distinction between milestone variety and explicit snoozing.
 */
public class RecommendationEngineMilestoneTest
{
    @Test
    public void completedSkillRemainsEligibleDespiteLargeSoftPenalty()
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(new TrainingMethodDatabase(), null, new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(), new MethodStrategyService());
        RecommendationEngine engine = TestFixtures.recommendationEngine(selector);
        PreferenceProfile profile = new PreferenceProfile();

        profile.addTemporaryScoreAdjustment(
                "skill:farming",
                -1000.0,
                60_000L
        );

        // A completion adjustment is deliberately not a cooldown.
        assertFalse(profile.isOnCooldown("skill:farming"));

        List<Recommendation> recommendations = engine.recommend(
                farmingOnlyAccount(),
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                profile
        );

        assertEquals(1, recommendations.size());
        assertEquals(
                "skill:farming",
                recommendations.get(0).getId()
        );
    }

    private static AccountSnapshot farmingOnlyAccount()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            experience.put(skill, 0);
        }

        // Hitpoints is skipped by the recommendation engine either way.
        levels.put(Skill.FARMING, 10);

        return new AccountSnapshot("Test", 0L, 0, "Main", Membership.P2P, 1, 2200, 0L, levels, experience);
    }
}
