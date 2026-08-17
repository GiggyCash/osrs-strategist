package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniversalActionRecipeResolverTest
{
    private final UniversalActionRecipeResolver resolver =
            new UniversalActionRecipeResolver();

    @Test
    public void smithingBarCountsMatchStandardAnvilFamilies()
    {
        assertEquals(5, UniversalActionRecipeResolver.smithingBarsFor("rune platebody"));
        assertEquals(3, UniversalActionRecipeResolver.smithingBarsFor("rune platelegs"));
        assertEquals(3, UniversalActionRecipeResolver.smithingBarsFor("rune 2h sword"));
        assertEquals(3, UniversalActionRecipeResolver.smithingBarsFor("rune battleaxe"));
        assertEquals(2, UniversalActionRecipeResolver.smithingBarsFor("rune claws"));
        assertEquals(2, UniversalActionRecipeResolver.smithingBarsFor("rune scimitar"));
        assertEquals(1, UniversalActionRecipeResolver.smithingBarsFor("rune dagger"));
    }

    @Test
    public void highAlchemyModelsRunesWithoutPretendingToKnowAlchItem()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.MAGIC, "High Level Alchemy", 65),
                100,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertEquals(2, recipe.getInputs().size());
        assertInput(recipe, "Nature rune", 100);
        assertInput(recipe, "Fire rune", 500);
        assertTrue(recipe.getSetup().contains("safe alch list"));
    }

    @Test
    public void prayerTreatsConcreteCalculatorItemAsConsumedInput()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.PRAYER, "Big bones", 15),
                250,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Big bones", 250);
    }

    @Test
    public void compositeCookingRecipeFailsClosed()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.COOKING, "Meat pizza", 169),
                50,
                MembershipStatus.F2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void herblorePrayerPotionHasAllConsumedInputs()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.HERBLORE, "Prayer potion", 87.5f),
                40,
                MembershipStatus.P2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Ranarr weed", 40);
        assertInput(recipe, "Snape grass", 40);
        assertInput(recipe, "Vial of water", 40);
    }

    private static RuneLiteSkillActionDefinition action(
            Skill skill, String name, float xp)
    {
        return new RuneLiteSkillActionDefinition(
                skill,
                "test:" + name.toLowerCase().replace(' ', '_'),
                name,
                1,
                xp,
                "test",
                MembershipStatus.F2P);
    }

    private static void assertInput(
            UniversalActionRecipe recipe,
            String name,
            int quantity)
    {
        for (ResolvedMethodInput input : recipe.getInputs())
        {
            if (name.equals(input.getName()))
            {
                assertEquals(quantity, input.getQuantity());
                return;
            }
        }
        throw new AssertionError("Missing input: " + name);
    }
}
