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
    public void windStrikeHasExactRuneInputs()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.MAGIC, "Wind Strike", 5.5f), 25,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Air rune", 25);
        assertInput(recipe, "Mind rune", 25);
    }

    @Test
    public void f2pFireCombatSpellsHaveExactRuneInputs()
    {
        UniversalActionRecipe bolt = resolver.resolve(
                action(Skill.MAGIC, "Fire Bolt", 22.5f), 10,
                MembershipStatus.F2P);
        assertTrue(bolt.hasExactInputs());
        assertInput(bolt, "Air rune", 30);
        assertInput(bolt, "Fire rune", 40);
        assertInput(bolt, "Chaos rune", 10);

        UniversalActionRecipe blast = resolver.resolve(
                action(Skill.MAGIC, "Fire Blast", 34.5f), 10,
                MembershipStatus.F2P);
        assertTrue(blast.hasExactInputs());
        assertInput(blast, "Air rune", 40);
        assertInput(blast, "Fire rune", 50);
        assertInput(blast, "Death rune", 10);
    }

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

    @Test
    public void plainLeatherBodyUsesOneLeatherNotDragonhideBodyCount()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.CRAFTING, "Leather body", 25),
                80,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertEquals(1, recipe.getInputs().size());
        assertInput(recipe, "Leather", 80);
    }

    @Test
    public void opalJewelleryUsesSilverBar()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.CRAFTING, "Opal ring", 10),
                25,
                MembershipStatus.P2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Silver bar", 25);
        assertInput(recipe, "Opal", 25);
        assertNoInput(recipe, "Gold bar");
    }

    @Test
    public void sapphireJewelleryUsesGoldBar()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.CRAFTING, "Sapphire ring", 40),
                25,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Gold bar", 25);
        assertInput(recipe, "Sapphire", 25);
        assertNoInput(recipe, "Silver bar");
    }

    @Test
    public void f2pTiaraUsesSilverBar()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.CRAFTING, "Tiara", 52.5f),
                30,
                MembershipStatus.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Silver bar", 30);
        assertTrue(recipe.getSetup().contains("Edgeville furnace"));
    }

    @Test
    public void arrowShaftXpUnitUsesFifteenShaftsPerBasicLog()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.FLETCHING, "Arrow shaft", 0.33f),
                31,
                MembershipStatus.P2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Logs", 3);
        assertTrue(recipe.getSetup().contains("15 shafts per log"));
    }

    @Test
    public void specialtyGemBoltFailsClosedInsteadOfGuessingBasicBoltRecipe()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.FLETCHING, "Ruby bolts", 6.3f),
                100,
                MembershipStatus.P2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void nonLogFiremakingActivityFailsClosed()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.FIREMAKING, "Wintertodt", 100),
                50,
                MembershipStatus.P2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void largeSmithingCountSaturatesInsteadOfOverflowingNegative()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.SMITHING, "Rune platebody", 375),
                Integer.MAX_VALUE,
                MembershipStatus.P2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Runite bar", Integer.MAX_VALUE);
    }

    private static ActionDef action(
            Skill skill, String name, float xp)
    {
        return new ActionDef(
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
        for (MethodInput input : recipe.getInputs())
        {
            if (name.equals(input.getName()))
            {
                assertEquals(quantity, input.getQuantity());
                return;
            }
        }
        throw new AssertionError("Missing input: " + name);
    }

    private static void assertNoInput(
            UniversalActionRecipe recipe,
            String name)
    {
        for (MethodInput input : recipe.getInputs())
        {
            if (name.equals(input.getName()))
            {
                throw new AssertionError("Unexpected input: " + name);
            }
        }
    }
}
