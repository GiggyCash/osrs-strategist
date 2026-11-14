package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class BoltRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void nineOrdinaryFamiliesUseExactUnfinishedItemIdsAndPerBoltUnits()
    {
        String[] metals = {"Bronze", "Blurite", "Iron", "Silver", "Steel", "Mithril", "Adamant", "Runite", "Dragon"};
        int[] ids = {9375, 9376, 9377, 9382, 9378, 9379, 9380, 9381, 21930};
        for (int i = 0; i < metals.length; i++)
        {
            UniversalActionRecipe recipe = resolver.resolve(action(metals[i] + " bolts", 1), 11, Membership.P2P);
            assertTrue(recipe.hasExactInputs());
            assertEquals(ids[i], recipe.inputs.get(0).itemId);
            assertEquals(314, recipe.inputs.get(1).itemId);
            assertEquals(11, recipe.inputs.get(0).quantity);
            assertEquals(11, recipe.inputs.get(1).quantity);
        }
        assertEquals("Adamant bolts(unf)", resolver.resolve(action("Adamant bolts", 1), 1, Membership.P2P).inputs.get(0).name);
    }

    @Test
    public void unreviewedSpecialtyNamesAndFreeMembershipFailClosed()
    {
        for (String name : Arrays.asList("Future bronze bolts", "Ruby bolts", "Broad bolts", "Dragon bolts (unf)"))
            assertFalse(name, resolver.resolve(action(name, 1), 10, Membership.P2P).hasExactInputs());
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertFalse(resolver.resolve(action("Bronze bolts", 1), 10, membership).hasExactInputs());
    }

    @Test
    public void reviewedProfileCannotRecreateUnsupportedBoltInputs()
    {
        MethodProfile profile = new MethodExecutionProfileCatalog().forMethod("fletching_bolts");
        assertNotNull(profile.reviewedRecipes);
        assertTrue(profile.inputs.isEmpty());
        assertEquals(9380, resolver.profileInputs(profile, action("Adamant bolts", 7), 11, Membership.P2P).get(0).itemId);
        ActionDef unknown = action("Future bronze bolts", 10000);
        ActionDef known = action("Bronze bolts", 0.5f);
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            AdaptiveActionSelector selector = new AdaptiveActionSelector();
            assertNull(selector.select(data, profile, Collections.singletonList(unknown), 80, Membership.P2P, 0, 10000, 1, false));
            assertSame(known, selector.select(data, profile, Arrays.asList(unknown, known), 80, Membership.P2P, 0, 10000, 1, false));
        }
    }

    private static ActionDef action(String name, float xp)
    {
        return new ActionDef(Skill.FLETCHING, "test:" + name.toLowerCase(Locale.ROOT).replace(' ', '_'), name, 1, xp, "test", Membership.F2P);
    }
}
