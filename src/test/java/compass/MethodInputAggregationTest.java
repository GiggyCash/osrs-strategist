package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodInputAggregationTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void duplicateProfileInputsNeverWrapIntoZeroRequirements()
    {
        MethodProfile profile = profile(1, 1);
        assertNull(resolver.profileInputs(profile, action(1), Integer.MAX_VALUE, Membership.P2P));
        List<MethodInput> valid = resolver.profileInputs(profile, action(1), 10, Membership.P2P);
        assertEquals(1, valid.size());
        assertEquals(20, valid.get(0).quantity);
    }

    @Test
    public void unrepresentableIndividualQuantitiesFailClosed()
    {
        for (double units : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 2})
            assertNull(resolver.profileInputs(profile(units), action(1), Integer.MAX_VALUE, Membership.P2P));
    }

    @Test
    public void identityRequirementsRemainStricterForPreparation()
    {
        List<MethodInput> named = Arrays.asList(new MethodInput("Logs", -1, 4),
                new MethodInput("logs", -1, 2));
        assertNull(MethodInput.mergeExact(named, true));
        assertEquals(6, MethodInput.mergeExact(named, false).get(0).quantity);
        assertNull(MethodInput.mergeExact(Arrays.asList(new MethodInput("Logs", 1511, 4),
                new MethodInput("Knife", 1511, 1)), true));
        assertNull(MethodInput.mergeExact(Collections.singletonList(new MethodInput("Logs", 1511, 0)), true));
    }

    @Test
    public void unresolvedRecipeCannotWinAdaptiveSelectionForAnyAccountMode()
    {
        MethodProfile profile = profile(1, 1);
        ActionDef overflow = action(0.000001f);
        ActionDef valid = action(1);
        for (int mode : new int[] {0, 1, 2, 3, 4, 5, 6})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1,
                    Collections.emptyList(), Collections.emptyList());
            AdaptiveActionSelector selector = new AdaptiveActionSelector();
            assertNull(selector.select(data, profile, Collections.singletonList(overflow),
                    1, Membership.P2P, 0, 10_000, 1, false));
            assertSame(valid, selector.select(data, profile, Arrays.asList(overflow, valid),
                    1, Membership.P2P, 0, 10_000, 1, false));
        }
    }

    @Test
    public void catalogRecipeOverflowDiscardsAllPartialInputs()
    {
        ActionDef steel = new ActionDef(Skill.SMITHING, "test:steel", "Steel bar", 1, 1, "test", Membership.P2P);
        UniversalActionRecipe overflow = resolver.resolve(steel, Integer.MAX_VALUE, Membership.P2P);
        assertFalse(overflow.hasExactInputs());
        assertTrue(overflow.inputs.isEmpty());
        UniversalActionRecipe boundary = resolver.resolve(steel, Integer.MAX_VALUE / 2, Membership.P2P);
        assertTrue(boundary.hasExactInputs());
        assertEquals(Integer.MAX_VALUE / 2 * 2, boundary.inputs.get(1).quantity);
    }

    @Test
    public void scaledRecipesMergeDuplicatesAndRejectAmbiguousOrOverflowingTotals()
    {
        String[] names = {"Material", "Material"};
        int[] ids = {123, 123};
        UniversalActionRecipe merged = UniversalActionRecipe.scaled(names, ids, new int[] {2, 3}, 4, "Setup");
        assertTrue(merged.hasExactInputs());
        assertEquals(1, merged.inputs.size());
        assertEquals(20, merged.inputs.get(0).quantity);
        assertFalse(UniversalActionRecipe.scaled(names, ids, new int[] {1, 1}, Integer.MAX_VALUE, "Setup").hasExactInputs());
        assertFalse(UniversalActionRecipe.scaled(new String[] {"Material", "Different"}, ids,
                new int[] {1, 1}, 1, "Setup").hasExactInputs());
        assertFalse(UniversalActionRecipe.scaled(names, new int[] {123}, new int[] {1, 1}, 1, "Setup").hasExactInputs());
        assertFalse(UniversalActionRecipe.scaled(names, ids, new int[] {0, 1}, 1, "Setup").hasExactInputs());
        assertFalse(UniversalActionRecipe.scaled(names, new int[] {123, -1}, new int[] {1, 1}, 1, "Setup").hasExactInputs());
        assertFalse(UniversalActionRecipe.scaled(new String[] {null}, null, new int[] {1}, 1, "Setup").hasExactInputs());
    }

    private static MethodProfile profile(double... units)
    {
        MethodProfile profile = new MethodProfile();
        profile.actionTerms = Collections.singletonList("logs");
        profile.inputs = new ArrayList<>();
        for (double unit : units) profile.inputs.add(new MethodInputRule(
                MethodProfile.InputMode.FIXED, "Logs", unit));
        return profile;
    }

    private static ActionDef action(float xp)
    {
        return new ActionDef(Skill.FLETCHING, "test:logs", "Logs", 1, xp, "test", Membership.P2P);
    }
}
