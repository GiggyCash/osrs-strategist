package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedRecipeContractTest
{
    @Test
    public void everyReferencedMethodUsesTheSameReviewedInputsAsActionGuidance()
    {
        MethodIntelligenceCatalog methods = new MethodIntelligenceCatalog();
        ReviewedActionRecipeCatalog reviewed = new ReviewedActionRecipeCatalog();
        int references = 0;
        for (MethodIntelligenceCatalog.Recipe definition : definitions().recipes)
        {
            if (definition.inputRecipe == null) continue;
            references++;
            assertNull("No duplicated inline facts", definition.inputs);
            MethodIntelligenceCatalog.Recipe method = methods.recipe(definition.id);
            ReviewedActionRecipeCatalog.Recipe source = reviewed.recipe(method.skill, method.inputRecipe);
            if (source.actionUnitsPerBatch == null)
            {
                assertEquals(source.itemIds[0], method.inputs.get(0).itemId);
                assertFalse(source.build(4, Membership.P2P).hasExactInputs());
                continue;
            }
            UniversalActionRecipe action = source.build(4 * source.actionUnitsPerBatch, Membership.P2P);
            assertEquals(method.inputs.size(), action.inputs.size());
            for (int i = 0; i < method.inputs.size(); i++)
            {
                assertEquals(method.inputs.get(i).itemId, action.inputs.get(i).itemId);
                assertEquals(method.inputs.get(i).name, action.inputs.get(i).name);
                assertEquals(4 * method.inputs.get(i).quantity, action.inputs.get(i).quantity);
                assertEquals(source.stackable[i].booleanValue(), method.inputs.get(i).stackable);
            }
        }
        assertEquals(24, references);
    }

    @Test
    public void referencesCannotBeMissingWrongSkillOrOverrideIngredientFacts()
    {
        MethodIntelligenceCatalog.Bundle missing = definitions();
        missing.recipes.get(0).inputRecipe = "missing";
        rejects(missing);
        MethodIntelligenceCatalog.Bundle wrongSkill = definitions();
        wrongSkill.recipes.get(0).skill = Skill.FISHING;
        rejects(wrongSkill);
        MethodIntelligenceCatalog.Bundle overridden = definitions();
        overridden.recipes.get(0).inputs = Collections.emptyList();
        rejects(overridden);
    }

    @Test
    public void freeMethodCannotReferenceMembersIngredients()
    {
        MethodIntelligenceCatalog.Bundle bundle = definitions();
        bundle.recipes.get(0).inputRecipe = "shark";
        rejects(bundle);
    }

    @Test
    public void sharedWineInputsPreserveTwoSlotCapacityAndOutputFlow()
    {
        MethodIntelligenceCatalog.Recipe wine = new MethodIntelligenceCatalog().recipe("wine");
        ItemsState empty = new ItemsState(Collections.emptyList(), true);
        assertEquals(RequirementState.VERIFIED, MethodCapacity.evaluate(empty, wine, 14).working);
        List<ItemState> retained = Collections.singletonList(new ItemState(9000, "Retained setup", 1));
        assertEquals(RequirementState.BLOCKED,
                MethodCapacity.evaluate(new ItemsState(retained, true), wine, 14).working);
        assertEquals(1, wine.outputsPerAction.intValue());
        assertFalse(wine.outputStackable);
    }

    @Test
    public void reviewedIngredientQuantityCannotSaturateIntoFalseExactness()
    {
        ReviewedActionRecipeCatalog.Recipe source = new ReviewedActionRecipeCatalog().recipe(Skill.COOKING, "salmon");
        source.units[0] = 2;
        assertFalse(source.build(Integer.MAX_VALUE, Membership.P2P).hasExactInputs());
        assertFalse(source.build(0, Membership.P2P).hasExactInputs());
        assertTrue(source.build(Integer.MAX_VALUE / 2, Membership.P2P).hasExactInputs());
    }

    @Test
    public void shaftCalculatorUnitsRoundUpWithoutChangingMethodBatchUnits()
    {
        ReviewedActionRecipeCatalog.Recipe source = new ReviewedActionRecipeCatalog().recipe(Skill.FLETCHING, "arrow shaft");
        for (int count : new int[] {1, 15, 16, Integer.MAX_VALUE})
            assertEquals((count - 1) / 15 + 1, source.build(count, Membership.P2P).inputs.get(0).quantity);
        assertFalse(source.build(15, Membership.F2P).hasExactInputs());
        assertFalse(source.build(15, Membership.UNKNOWN).hasExactInputs());
        MethodIntelligenceCatalog methods = new MethodIntelligenceCatalog();
        assertEquals(1, methods.recipe("regular_arrow_shafts").inputs.get(0).quantity);
        assertEquals(1, methods.recipe("oak_arrow_shafts").inputs.get(0).quantity);
        assertEquals(30, methods.recipe("oak_arrow_shafts").outputsPerAction.intValue());
    }

    private static MethodIntelligenceCatalog.Bundle definitions()
    {
        return BundledCatalogLoader.array("/content/catalogs/method-intelligence.json",
                MethodIntelligenceCatalog.Bundle[].class)[0];
    }

    private static void rejects(MethodIntelligenceCatalog.Bundle bundle)
    {
        assertThrows(IllegalStateException.class, () -> new MethodIntelligenceCatalog(bundle));
    }
}
