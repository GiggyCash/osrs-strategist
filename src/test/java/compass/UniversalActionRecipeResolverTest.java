package compass;

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
    public void ironSmeltingCannotPromiseOrePerSuccessfulBarWithoutSetupEvidence()
    {
        for (Membership membership : Membership.values())
        {
            UniversalActionRecipe recipe = resolver.resolve(
                    action(Skill.SMITHING, "Iron bar", 12.5f), 100, membership);
            assertFalse(recipe.hasExactInputs());
            assertTrue(recipe.getInputs().isEmpty());
        }
    }

    @Test
    public void standardFurnaceInputsRetainVerifiedIdentitiesAndQuantities()
    {
        for (Membership membership : Membership.values())
        {
            UniversalActionRecipe bronze = resolver.resolve(
                    action(Skill.SMITHING, "Bronze bar", 6.2f), 10, membership);
            assertInput(bronze, "Copper ore", 10);
            assertInput(bronze, "Tin ore", 10);
            assertEquals(436, bronze.getInputs().get(0).itemId);
            UniversalActionRecipe rune = resolver.resolve(
                    action(Skill.SMITHING, "Runite bar", 50), 10, membership);
            assertInput(rune, "Runite ore", 10);
            assertInput(rune, "Coal", 80);
            assertEquals(451, rune.getInputs().get(0).itemId);
            assertEquals(453, rune.getInputs().get(1).itemId);
        }
    }

    @Test
    public void windStrikeHasExactRuneInputs()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.MAGIC, "Wind Strike", 5.5f), 25,
                Membership.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Air rune", 25);
        assertInput(recipe, "Mind rune", 25);
    }

    @Test
    public void f2pFireCombatSpellsHaveExactRuneInputs()
    {
        UniversalActionRecipe bolt = resolver.resolve(
                action(Skill.MAGIC, "Fire Bolt", 22.5f), 10,
                Membership.F2P);
        assertTrue(bolt.hasExactInputs());
        assertInput(bolt, "Air rune", 30);
        assertInput(bolt, "Fire rune", 40);
        assertInput(bolt, "Chaos rune", 10);

        UniversalActionRecipe blast = resolver.resolve(
                action(Skill.MAGIC, "Fire Blast", 34.5f), 10,
                Membership.F2P);
        assertTrue(blast.hasExactInputs());
        assertInput(blast, "Air rune", 40);
        assertInput(blast, "Fire rune", 50);
        assertInput(blast, "Death rune", 10);
    }

    @Test
    public void smithingRequiresReviewedIdentityRatherThanMetalAndItemFragments()
    {
        for (Membership membership : Membership.values())
        {
            for (String name : new String[] {"Future rune platebody", "Bronze ceremonial sword",
                    "Rune claws", "Steel wire", "Adamant knife"})
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.SMITHING, name, 1), 10, membership);
                assertFalse(name, recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
            UniversalActionRecipe reviewed = resolver.resolve(
                    action(Skill.SMITHING, "Bronze scimitar", 25), 10, membership);
            assertTrue(reviewed.hasExactInputs());
            assertInput(reviewed, "Bronze bar", 20);
        }
    }

    @Test
    public void highAlchemyModelsRunesWithoutPretendingToKnowAlchItem()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.MAGIC, "High Level Alchemy", 65),
                100,
                Membership.F2P);

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
                Membership.F2P);

        assertTrue(recipe.hasExactInputs());
        assertInput(recipe, "Big bones", 250);
    }

    @Test
    public void compositeCookingRecipeFailsClosed()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.COOKING, "Meat pizza", 169),
                50,
                Membership.F2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void herblorePrayerPotionHasAllConsumedInputs()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.HERBLORE, "Prayer potion", 87.5f),
                40,
                Membership.P2P);

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
                Membership.F2P);

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
                Membership.P2P);

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
                Membership.F2P);

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
                Membership.F2P);

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
                Membership.P2P);

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
                Membership.P2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void nonLogFiremakingActivityFailsClosed()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.FIREMAKING, "Wintertodt", 100),
                50,
                Membership.P2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
    }

    @Test
    public void largeSmithingCountCannotClaimAnExactSaturatedQuantity()
    {
        UniversalActionRecipe recipe = resolver.resolve(
                action(Skill.SMITHING, "Rune platebody", 375),
                Integer.MAX_VALUE,
                Membership.P2P);

        assertFalse(recipe.hasExactInputs());
        assertTrue(recipe.getInputs().isEmpty());
        assertInput(resolver.resolve(action(Skill.SMITHING, "Rune platebody", 375),
                Integer.MAX_VALUE / 5, Membership.P2P), "Runite bar", Integer.MAX_VALUE / 5 * 5);
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
                Membership.F2P);
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
