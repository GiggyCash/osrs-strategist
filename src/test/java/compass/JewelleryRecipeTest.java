package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class JewelleryRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void castingRecipesUseReviewedMaterialsMouldsAndMembership()
    {
        String[] gems = {"Gold", "Sapphire", "Emerald", "Ruby", "Diamond", "Opal", "Jade", "Topaz", "Dragonstone", "Onyx", "Zenyte"};
        int[] gemIds = {0, 1607, 1605, 1603, 1601, 1609, 1611, 1613, 1615, 6573, 19493};
        String[] forms = {"ring", "necklace", "bracelet", "amulet (u)"};
        for (int i = 0; i < gems.length; i++)
            for (String form : forms)
            {
                String name = gems[i] + " " + form;
                if (name.equals("Dragonstone necklace")) name = "Dragon necklace";
                UniversalActionRecipe recipe = resolver.resolve(action(name), 17, Membership.P2P);
                assertTrue(name, recipe.hasExactInputs());
                Map<Integer, Integer> expected = new HashMap<>();
                expected.put(i >= 5 && i <= 7 ? 2355 : 2357, 17);
                if (gemIds[i] > 0) expected.put(gemIds[i], 17);
                Map<Integer, Integer> actual = new HashMap<>();
                for (MethodInput input : recipe.inputs) actual.put(input.itemId, input.quantity);
                assertEquals(name, expected, actual);
                assertTrue(recipe.setup, recipe.setup.contains(form.startsWith("amulet") ? "amulet mould" : form + " mould"));
                for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
                    assertEquals(name, i < 5 && !form.equals("bracelet"),
                            resolver.resolve(action(name), 17, membership).hasExactInputs());
            }
    }

    @Test
    public void topazUsesCutRedTopazAndSilverRatherThanGoldOnly()
    {
        UniversalActionRecipe recipe = resolver.resolve(action("Topaz ring"), 3, Membership.P2P);
        assertTrue(recipe.hasExactInputs());
        assertTrue(recipe.inputs.stream().anyMatch(i -> i.itemId == 1613 && i.name.equals("Red topaz") && i.quantity == 3));
        assertTrue(recipe.inputs.stream().anyMatch(i -> i.itemId == 2355 && i.quantity == 3));
        assertFalse(recipe.inputs.stream().anyMatch(i -> i.itemId == 2357));
    }

    @Test
    public void suffixesDoNotAuthorizeEnchantedStrungOrInventedRecipes()
    {
        for (String name : Arrays.asList("Future sapphire ring", "Sapphire amulet", "Amulet of glory",
                "Ruby bracelet (enchanted)", "Onyx necklace (unknown)", "Imaginary ring"))
            assertFalse(name, resolver.resolve(action(name), 10, Membership.P2P).hasExactInputs());
        assertFalse(resolver.resolve(action("Gold ring"), 0, Membership.F2P).hasExactInputs());
    }

    private static ActionDef action(String name)
    {
        return new ActionDef(Skill.CRAFTING, "test:casting", name, 1, 1, "test", Membership.UNKNOWN);
    }
}
