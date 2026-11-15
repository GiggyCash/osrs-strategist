package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdaptiveStageBoundaryTest
{
    @Test
    public void futureBoundariesRequireReviewedRecipeAndActualMembership()
    {
        AdaptiveActionSelector selector = selector(Arrays.asList(
                action("Future sapphire", 30, 1000, Membership.F2P),
                action("Dragonstone", 55, 100, Membership.P2P),
                action("Sapphire", 70, 50, Membership.F2P)));
        assertEquals(55, selector.resolve(plan(99), 20, 99, Membership.P2P));
        assertEquals(70, selector.resolve(plan(99), 20, 99, Membership.F2P));
        assertEquals(70, selector.resolve(plan(99), 20, 99, Membership.UNKNOWN));
        assertEquals(70, selector.resolve(plan(99), 20, 99));
        assertEquals(61, selector.resolve(plan(60), 20, 99, Membership.F2P));
    }

    @Test
    public void invalidXpAndUnknownAccessCannotCreateStageTransitions()
    {
        List<ActionDef> invalid = Arrays.asList(
                action("Sapphire", 25, Float.NaN, Membership.F2P),
                action("Sapphire", 30, Float.POSITIVE_INFINITY, Membership.F2P),
                action("Sapphire", 35, 0, Membership.F2P),
                action("Sapphire", 40, 50, Membership.UNKNOWN));
        AdaptiveActionSelector selector = selector(invalid);
        assertEquals(99, selector.resolve(plan(99), 20, 99, Membership.P2P));
        MethodProfile profile = new MethodExecutionProfileCatalog().forMethod("crafting_gems");
        GameData data = MethodIntelligenceTest.data(0, -1, Collections.emptyList(), Collections.emptyList());
        assertNull(selector.select(data, profile, invalid, 99, Membership.P2P, 0, 10000, 1, false));
    }

    private static AdaptiveActionSelector selector(List<ActionDef> actions)
    {
        return new AdaptiveActionSelector(new RuneLiteSkillActionCatalog()
        {
            @Override public List<ActionDef> actionsFor(Skill skill) { return actions; }
        }, new MethodExecutionProfileCatalog());
    }

    private static TrainingPlan plan(int max)
    {
        return new TrainingPlan(new TrainingMethod("crafting_gems", Skill.CRAFTING, 1, max,
                "Test cutting", "Test setup", 10, 10, 10, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(), Confidence.VERIFIED), "Test",
                Confidence.VERIFIED, Collections.emptyList());
    }

    // Synthetic levels isolate boundary policy from game-level data.
    private static ActionDef action(String name, int level, float xp, Membership membership)
    {
        return new ActionDef(Skill.CRAFTING, "test:" + name, name, level, xp, "test", membership);
    }
}
