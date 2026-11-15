package compass;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedMaterialIdentityTest
{
    private final AccountResourcePlanner planner = new AccountResourcePlanner(null, new ResourceSourceCatalog());
    private final List<MethodInput> needs = Collections.singletonList(new MethodInput("Uncut sapphire", 1623, 10));

    @Test
    public void invalidOrOverflowingNeedsCannotProducePartialSupplyGuidance()
    {
        GameData owned = data(1, 1623, "Uncut sapphire", Integer.MAX_VALUE);
        assertNull(planner.plan(owned, null, false));
        assertNull(planner.plan(owned, Arrays.asList(needs.get(0), null), false));
        assertNull(planner.plan(owned, Arrays.asList(needs.get(0),
                new MethodInput("Invalid", -1, 0)), false));
        assertNull(planner.plan(owned, Arrays.asList(
                new MethodInput("Uncut sapphire", 1623, Integer.MAX_VALUE), needs.get(0)), false));
        assertNotNull(planner.plan(owned, Collections.emptyList(), false));
    }

    @Test
    public void ambiguousRequirementLabelsFailClosedAndDifferentIdsRemainDistinct()
    {
        GameData owned = data(1, 1623, "Uncut sapphire", 10);
        List<MethodInput> aliases = Arrays.asList(new MethodInput("Uncut sapphire", 1623, 8),
                new MethodInput("Different label", 1623, 8));
        assertNull(planner.plan(owned, aliases, false));
        List<MethodInput> distinct = Arrays.asList(new MethodInput("Uncut sapphire", 1623, 5),
                new MethodInput("Uncut sapphire", 1621, 5));
        SupplyPlan plan = planner.plan(owned, distinct, false);
        assertEquals(5, plan.getTotalMissingUnits());
        assertEquals(1621, plan.getMissingInputs().get(0).itemId);
    }

    @Test
    public void plannerAndUniversalRankingUseReviewedIdsAcrossAccountModes()
    {
        for (int mode = 0; mode <= 6; mode++)
        {
            GameData exact = data(mode, 1623, "Different display label", 10);
            GameData wrong = data(mode, 999999, "Uncut sapphire", 10);
            assertTrue(planner.plan(exact, needs, false).getMissingInputs().isEmpty());
            assertEquals(10, planner.plan(wrong, needs, false).getTotalMissingUnits());
            assertTrue(score(exact, needs) > score(wrong, needs));
        }
    }

    @Test
    public void uimBankStillCannotSatisfyVerifiedIdentity()
    {
        GameData uim = MethodIntelligenceTest.data(2, -1, Collections.emptyList(),
                Collections.singletonList(new ItemState(1623, "Uncut sapphire", 10)));
        assertEquals(10, planner.plan(uim, needs, false).getTotalMissingUnits());
    }

    @Test
    public void universalCoverageCannotOverflowCombinedIngredientCounts()
    {
        GameData full = data(1, 1623, "Different label", Integer.MAX_VALUE);
        List<MethodInput> large = Arrays.asList(new MethodInput("Uncut sapphire", 1623, Integer.MAX_VALUE),
                new MethodInput("Uncut emerald", 1621, Integer.MAX_VALUE));
        assertEquals(250.0, score(full, large), 0.001);
    }

    private static double score(GameData data, List<MethodInput> needs)
    {
        return UniversalSkillActionGuidanceService.resourceCoverageScore(data, new ItemIndex(data, false),
                new UniversalActionRecipe(needs, "Test", true));
    }

    private static GameData data(int mode, int id, String name, int count)
    {
        return MethodIntelligenceTest.data(mode, -1,
                Collections.singletonList(new ItemState(id, name, count)), Collections.emptyList());
    }
}
