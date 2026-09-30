package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class GemRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();
    private final MethodProfile profile = new MethodExecutionProfileCatalog().forMethod("crafting_gems");

    @Test
    public void deterministicGemsShareExactIdentitiesAndMembership()
    {
        String[] names = {"Sapphire", "Emerald", "Ruby", "Diamond", "Dragonstone", "Onyx"};
        int[] ids = {1623, 1621, 1619, 1617, 1631, 6571};
        assertTrue(profile.reviewedInputs);
        assertTrue(profile.inputs.isEmpty());
        for (int i = 0; i < names.length; i++)
        {
            ActionDef action = action(names[i]);
            UniversalActionRecipe recipe = resolver.resolve(action, 17, Membership.P2P);
            assertTrue(recipe.hasExactInputs());
            assertEquals(1, recipe.inputs.size());
            assertEquals(ids[i], recipe.inputs.get(0).itemId);
            assertEquals(17, recipe.inputs.get(0).quantity);
            assertEquals(ids[i], resolver.profileInputs(profile, action, 17, Membership.P2P).get(0).itemId);
            for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
                assertEquals(i < 4, resolver.resolve(action, 17, membership).hasExactInputs());
        }
    }

    @Test
    public void variableUnverifiedAndInventedActionsCannotReceiveExactSupplies()
    {
        for (String name : Arrays.asList("Opal", "Jade", "Red topaz", "Zenyte", "Future sapphire", "Uncut sapphire"))
        {
            ActionDef action = action(name);
            assertFalse(name, resolver.resolve(action, 10, Membership.P2P).hasExactInputs());
            assertNull(resolver.profileInputs(profile, action, 10, Membership.P2P));
        }
        // A ring matches broad profile terms but is not a reviewed cutting recipe.
        assertNull(resolver.profileInputs(profile, action("Sapphire ring"), 10, Membership.P2P));
    }

    @Test
    public void unverifiedGemCannotDisplaceReviewedCuttingAcrossAccountModes()
    {
        ActionDef unverified = action("Zenyte");
        ActionDef supported = action("Sapphire");
        for (int mode : new int[] {0, 1, 2, 3, 4, 5, 6})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            AdaptiveActionSelector selector = new AdaptiveActionSelector();
            assertNull(selector.select(data, profile, Collections.singletonList(unverified), 99,
                    Membership.P2P, 0, 10000, 1, false));
            assertSame(supported, selector.select(data, profile, Arrays.asList(unverified, supported), 99,
                    Membership.P2P, 0, 10000, 1, false));
        }
    }

    private static ActionDef action(String name)
    {
        return new ActionDef(Skill.CRAFTING, "test:" + name.toLowerCase(Locale.ROOT).replace(' ', '_'),
                name, 1, name.equals("Zenyte") ? 200 : 50, "test", Membership.F2P);
    }
}
