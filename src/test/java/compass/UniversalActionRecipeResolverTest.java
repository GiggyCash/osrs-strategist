package compass;

import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
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
    public void curseRequiresItsExactReviewedActionName()
    {
        UniversalActionRecipe curse = resolver.resolve(
                action(Skill.MAGIC, "Curse", 29), 10, Membership.F2P);
        assertTrue(curse.hasExactInputs());
        assertInput(curse, "Earth rune", 30);
        assertInput(curse, "Water rune", 20);
        assertInput(curse, "Body rune", 10);

        for (Membership membership : Membership.values())
        {
            UniversalActionRecipe invented = resolver.resolve(
                    action(Skill.MAGIC, "Future curse", 29), 10,
                    membership);
            assertFalse(invented.hasExactInputs());
            assertTrue(invented.getInputs().isEmpty());
        }
    }

    @Test
    public void constructionRecipesRequireExactRuneLiteActionNames()
    {
        String[] names = {"Oak Larder", "Oak Door", "Mahogany Table",
                "Crude Wooden Chair", "Teak Garden Bench",
                "Mounted Mythical Cape"};
        String[] inputs = {"Oak plank", "Oak plank", "Mahogany plank",
                "Plank", "Teak plank", "Teak plank"};
        int[] quantities = {8, 10, 6, 2, 6, 3};
        for (int i = 0; i < names.length; i++)
        {
            UniversalActionRecipe recipe = resolver.resolve(
                    action(Skill.CONSTRUCTION, names[i], 1), 2,
                    Membership.P2P);
            assertTrue(names[i], recipe.hasExactInputs());
            assertInput(recipe, inputs[i], quantities[i] * 2);
        }
        assertInput(resolver.resolve(action(Skill.CONSTRUCTION,
                "Crude Wooden Chair", 1), 2, Membership.P2P),
                "Steel nails", 4);

        UniversalActionRecipe invented = resolver.resolve(
                action(Skill.CONSTRUCTION, "Future Oak Larder", 1), 2,
                Membership.P2P);
        assertFalse(invented.hasExactInputs());
        assertTrue(invented.getInputs().isEmpty());
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
    public void unreviewedPrayerMethodsFailClosedWithoutRouteInputs()
    {
        for (String name : new String[] {"Big bones", "Infernal ashes",
                "Ensouled dragon head", "Future bones"})
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.PRAYER, name, 15), 250, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
    }

    @Test
    public void fishingNeedsAReviewedRouteBeforeClaimingConsumedInputs()
    {
        for (String name : new String[] {"Raw karambwan", "Raw anglerfish",
                "Raw dark crab", "Future fish"})
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.FISHING, name, 50), 100, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
    }

    @Test
    public void farmingNeedsACuratedRouteBeforeInferringSaplings()
    {
        for (String name : new String[] {"Maple tree", "Spirit tree",
                "Crystal tree", "Future tree"})
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.FARMING, name, 50), 5, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
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
    public void leatherCraftingFailsClosedWithoutThreadAndNeedleEvidence()
    {
        for (String name : new String[] {"Leather body", "Hardleather body",
                "Green d'hide body", "Coif", "Future leather body"})
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.CRAFTING, name, 25), 80, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
    }

    @Test
    public void unreviewedWoodProductsFailClosedWithoutReusableAndUnlockEvidence()
    {
        for (String name : new String[] {"Bird house", "Oak bird house",
                "Future bird house", "Oak shield", "Future shield",
                "Oak stock", "Future stock"})
            for (Membership membership : Membership.values())
            {
                Skill skill = name.contains("bird house")
                        ? Skill.CRAFTING : Skill.FLETCHING;
                UniversalActionRecipe recipe = resolver.resolve(
                        action(skill, name, 25), 40, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
    }

    @Test
    public void unreviewedFiremakingActionsFailClosed()
    {
        for (String name : new String[] {"Logs", "Oak pyre logs",
                "Sacred oil(4)", "Future logs"})
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.FIREMAKING, name, 40), 60, membership);
                assertFalse(name + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(name, recipe.getInputs().isEmpty());
            }
    }

    @Test
    public void runecraftRecipesRequireExactSimpleRuneActionIdentity()
    {
        UniversalActionRecipe air = resolver.resolve(
                action(Skill.RUNECRAFT, "runelite:runecraft:air_rune",
                        "Air rune", 5, Membership.F2P),
                28, Membership.F2P);
        assertTrue(air.hasExactInputs());
        assertInput(air, "Rune essence", 28);

        UniversalActionRecipe water = resolver.resolve(
                action(Skill.RUNECRAFT, "runelite:runecraft:water_rune",
                        "Water rune", 6, Membership.F2P),
                28, Membership.P2P);
        assertTrue(water.hasExactInputs());
        assertInput(water, "Pure essence", 28);

        String[][] unsupported = {
                {"runelite:runecraft:water_tiara", "Water tiara"},
                {"runelite:runecraft:lava_rune", "Lava rune"},
                {"runelite:runecraft:mind_core", "Mind core"},
                {"runelite:runecraft:zeah_blood_rune", "Blood rune"},
                {"runelite:runecraft:soul_rune", "Soul rune"},
                {"runelite:runecraft:aether_rune", "Aether rune"},
                {"test:future_rune", "Future rune"}};
        for (String[] value : unsupported)
            for (Membership membership : Membership.values())
            {
                UniversalActionRecipe recipe = resolver.resolve(
                        action(Skill.RUNECRAFT, value[0], value[1], 10,
                                Membership.P2P), 28, membership);
                assertFalse(value[0] + " / " + membership,
                        recipe.hasExactInputs());
                assertTrue(value[0], recipe.getInputs().isEmpty());
            }
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
    public void standardArrowsUseReviewedArrowtipIdentitiesAndMembership()
    {
        String[] arrows = {"Bronze arrow", "Iron arrow", "Steel arrow",
                "Mithril arrow", "Adamant arrow", "Rune arrow"};
        String[] tips = {"Bronze arrowtips", "Iron arrowtips",
                "Steel arrowtips", "Mithril arrowtips",
                "Adamant arrowtips", "Rune arrowtips"};
        int[] tipIds = {ItemID.BRONZE_ARROWHEADS, ItemID.IRON_ARROWHEADS,
                ItemID.STEEL_ARROWHEADS, ItemID.MITHRIL_ARROWHEADS,
                ItemID.ADAMANT_ARROWHEADS, ItemID.RUNE_ARROWHEADS};
        for (int i = 0; i < arrows.length; i++)
        {
            UniversalActionRecipe recipe = resolver.resolve(
                    action(Skill.FLETCHING, arrows[i], 1), 15,
                    Membership.P2P);
            assertTrue(arrows[i], recipe.hasExactInputs());
            assertInput(recipe, "Headless arrow", 15,
                    ItemID.HEADLESS_ARROW);
            assertInput(recipe, tips[i], 15, tipIds[i]);

            assertFalse(arrows[i], resolver.resolve(
                    action(Skill.FLETCHING, arrows[i], 1), 15,
                    Membership.F2P).hasExactInputs());
        }

        UniversalActionRecipe invented = resolver.resolve(
                action(Skill.FLETCHING, "Future bronze arrow", 1), 15,
                Membership.P2P);
        assertFalse(invented.hasExactInputs());
        assertTrue(invented.getInputs().isEmpty());
    }

    @Test
    public void broadArrowsRequireTheExplicitCheckNeededMethodContract()
    {
        ActionDef action = action(Skill.FLETCHING, "Broad arrows", 10);
        UniversalActionRecipe universal = resolver.resolve(action, 15,
                Membership.P2P);
        assertFalse(universal.hasExactInputs());
        assertTrue(universal.getInputs().isEmpty());

        MethodProfile profile = new MethodExecutionProfileCatalog()
                .forMethod("fletching_broad_arrows");
        java.util.List<MethodInput> inputs = resolver.profileInputs(
                profile, action, 15, Membership.P2P);
        assertInput(inputs, "Headless arrow", 15);
        assertInput(inputs, "Broad arrowheads", 15);
    }

    @Test
    public void javelinsUseReviewedTipIdentitiesAndMembership()
    {
        String[] javelins = {"Bronze javelin", "Iron javelin",
                "Steel javelin", "Mithril javelin", "Adamant javelin",
                "Rune javelin", "Amethyst javelin", "Dragon javelin"};
        String[] tips = {"Bronze javelin tips", "Iron javelin tips",
                "Steel javelin tips", "Mithril javelin tips",
                "Adamant javelin tips", "Rune javelin tips",
                "Amethyst javelin tips", "Dragon javelin tips"};
        int[] tipIds = {ItemID.BRONZE_JAVELIN_HEAD,
                ItemID.IRON_JAVELIN_HEAD, ItemID.STEEL_JAVELIN_HEAD,
                ItemID.MITHRIL_JAVELIN_HEAD, ItemID.ADAMANT_JAVELIN_HEAD,
                ItemID.RUNE_JAVELIN_HEAD, ItemID.AMETHYST_JAVELIN_HEAD,
                ItemID.DRAGON_JAVELIN_HEAD};
        for (int i = 0; i < javelins.length; i++)
        {
            UniversalActionRecipe recipe = resolver.resolve(
                    action(Skill.FLETCHING, javelins[i], 1), 15,
                    Membership.P2P);
            assertTrue(javelins[i], recipe.hasExactInputs());
            assertInput(recipe, "Javelin shaft", 15,
                    ItemID.JAVELIN_SHAFT);
            assertInput(recipe, tips[i], 15, tipIds[i]);

            assertFalse(javelins[i], resolver.resolve(
                    action(Skill.FLETCHING, javelins[i], 1), 15,
                    Membership.F2P).hasExactInputs());
        }

        UniversalActionRecipe invented = resolver.resolve(
                action(Skill.FLETCHING, "Future bronze javelin", 1), 15,
                Membership.P2P);
        assertFalse(invented.hasExactInputs());
        assertTrue(invented.getInputs().isEmpty());
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

    private static ActionDef action(Skill skill, String id, String name,
            float xp, Membership membership)
    {
        return new ActionDef(skill, id, name, 1, xp, "test", membership);
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

    private static void assertInput(UniversalActionRecipe recipe,
            String name, int quantity, int itemId)
    {
        for (MethodInput input : recipe.getInputs())
        {
            if (name.equals(input.getName()))
            {
                assertEquals(quantity, input.getQuantity());
                assertEquals(itemId, input.itemId);
                return;
            }
        }
        throw new AssertionError("Missing input: " + name);
    }

    private static void assertInput(java.util.List<MethodInput> inputs,
            String name, int quantity)
    {
        for (MethodInput input : inputs)
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
