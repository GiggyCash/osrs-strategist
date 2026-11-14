package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class BowRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();
    private final MethodProfile profile = new MethodExecutionProfileCatalog().forMethod("fletching_bows");

    @Test
    public void cuttingUsesExactLogIdsAndDoesNotConsumeTheKnife()
    {
        String[] woods = {"", "Oak", "Willow", "Maple", "Yew", "Magic"};
        int[] ids = {1511, 1521, 1519, 1517, 1515, 1513};
        assertEquals(12, profile.reviewedRecipes.size());
        assertTrue(profile.inputs.isEmpty());
        for (int i = 0; i < woods.length; i++)
            for (String shape : Arrays.asList("shortbow", "longbow"))
            {
                ActionDef action = action((woods[i] + " " + shape + " (u)").trim());
                UniversalActionRecipe recipe = resolver.resolve(action, 13, Membership.P2P);
                assertTrue(recipe.hasExactInputs());
                assertEquals(1, recipe.inputs.size());
                assertEquals(ids[i], recipe.inputs.get(0).itemId);
                assertEquals(13, recipe.inputs.get(0).quantity);
                assertEquals(ids[i], resolver.profileInputs(profile, action, 13, Membership.P2P).get(0).itemId);
                for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
                    assertFalse(resolver.resolve(action, 13, membership).hasExactInputs());
            }
    }

    @Test
    public void stringingUsesUnstrungBowAndStringAndCannotEnterCuttingProfile()
    {
        ActionDef action = action("Oak shortbow");
        UniversalActionRecipe recipe = resolver.resolve(action, 9, Membership.P2P);
        assertEquals(2, recipe.inputs.size());
        assertEquals(54, recipe.inputs.get(0).itemId);
        assertEquals(1777, recipe.inputs.get(1).itemId);
        assertEquals(9, recipe.inputs.get(0).quantity);
        assertEquals(9, recipe.inputs.get(1).quantity);
        assertNull(resolver.profileInputs(profile, action, 9, Membership.P2P));
        assertFalse(resolver.resolve(action, 9, Membership.F2P).hasExactInputs());
    }

    @Test
    public void suffixesCannotInventCrossbowOrUnreviewedBowIngredients()
    {
        for (String name : Arrays.asList("Bronze crossbow (u)", "Future shortbow (u)",
                "Future magic longbow", "Magic shortbow (i)", "Willow shortbow"))
        {
            assertFalse(name, resolver.resolve(action(name), 10, Membership.P2P).hasExactInputs());
            assertNull(resolver.profileInputs(profile, action(name), 10, Membership.P2P));
        }
    }

    private static ActionDef action(String name)
    {
        return new ActionDef(Skill.FLETCHING, "test:" + name.toLowerCase(Locale.ROOT), name,
                1, 1, "test", Membership.F2P);
    }
}
