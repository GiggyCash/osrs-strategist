package compass;

import java.util.*;
import java.util.function.Consumer;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewedActionRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void ordinaryFishAndWineUseExactIngredientIdentities()
    {
        UniversalActionRecipe salmon = resolve("Salmon", Membership.F2P);
        assertTrue(salmon.hasExactInputs());
        assertEquals(331, salmon.inputs.get(0).itemId);
        assertEquals(7, salmon.inputs.get(0).quantity);
        UniversalActionRecipe wine = resolve("Jug of wine", Membership.F2P);
        assertEquals(2, wine.inputs.size());
        assertEquals(1987, wine.inputs.get(0).itemId);
        assertEquals(1937, wine.inputs.get(1).itemId);
        assertTrue(wine.getSetup().contains("not guaranteed"));
    }

    @Test
    public void unknownFoodAndSpecialPreparationCannotInventRawIngredients()
    {
        for (String name : Arrays.asList("Future mystery fish", "Cooked meat", "Sinew",
                "Bread", "Cooked karambwan", "Poison karambwan", "Meat pizza", "Shrimps surprise"))
        {
            UniversalActionRecipe recipe = resolve(name, Membership.P2P);
            assertFalse(name, recipe.hasExactInputs());
            assertTrue(name, recipe.inputs.isEmpty());
        }
    }

    @Test
    public void reviewedMembershipFailsClosedEvenIfActionMembershipIsWrong()
    {
        for (Membership membership : new Membership[] {Membership.UNKNOWN, Membership.F2P})
        {
            assertFalse(resolve("Shark", membership).hasExactInputs());
            assertFalse(resolve("Bluefin", membership).hasExactInputs());
            assertTrue(resolve("Shrimps", membership).hasExactInputs());
        }
        assertEquals(32341, resolve("Bluefin", Membership.P2P).inputs.get(0).itemId);
    }

    @Test
    public void allReviewedRowsHaveUsableProductionMappings()
    {
        ReviewedActionRecipeCatalog.Recipe[] recipes = definitions();
        assertEquals(117, recipes.length);
        ReviewedActionRecipeCatalog.validate(recipes);
        for (ReviewedActionRecipeCatalog.Recipe recipe : recipes)
        {
            if (!recipe.skill.equals("COOKING")) continue;
            UniversalActionRecipe resolved = resolve(recipe.match, Membership.P2P);
            assertTrue(recipe.match, resolved.hasExactInputs());
            assertTrue(resolved.inputs.stream().allMatch(input -> input.itemId > 0 && input.quantity > 0));
        }
    }

    @Test
    public void malformedOrUnreviewedDefinitionsCannotLoad()
    {
        rejects(recipe -> recipe.membersOnly = null);
        rejects(recipe -> recipe.contains = true);
        rejects(recipe -> recipe.sourceRevision = 0);
        rejects(recipe -> recipe.reviewed = null);
        rejects(recipe -> recipe.itemIds = new int[0]);
        rejects(recipe -> recipe.stackable = null);
        rejects(recipe -> recipe.stackable[0] = null);
        rejects(recipe -> recipe.units[0] = 0);
        rejects(recipe -> recipe.source = "https://example.com/unreviewed");
        ReviewedActionRecipeCatalog.Recipe recipe = definitions()[0];
        assertThrows(IllegalStateException.class, () -> ReviewedActionRecipeCatalog.validate(
                new ReviewedActionRecipeCatalog.Recipe[] {recipe, recipe}));
    }

    @Test
    public void universalGuidanceSkipsUnsupportedFoodInsteadOfInventingSupplies()
    {
        ActionDef unknown = new ActionDef(Skill.COOKING, "test:mystery_fish", "Mystery fish",
                80, 10000, "fish", Membership.F2P);
        ActionDef salmon = new ActionDef(Skill.COOKING, "test:salmon", "Salmon",
                25, 90, "fish", Membership.F2P);
        List<ActionDef> candidates = new ArrayList<>(Collections.singletonList(unknown));
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override public List<ActionDef> actionsFor(Skill skill) { return candidates; }
        };
        UniversalSkillActionGuidanceService service = new UniversalSkillActionGuidanceService(
                catalog, resolver, new SkillingXpModifierService(), TestFixtures.accountResourcePlanner());
        TrainingMethod method = new TrainingMethod("test:cooking", Skill.COOKING, 1, 99,
                "Cook fish", "Cook fish on a range.", 10, 10, 10, AttentionLevel.LOW,
                10, 0, Collections.emptyList(), Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "test", Confidence.VERIFIED, Collections.emptyList());
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            candidates.clear();
            candidates.add(unknown);
            assertNull(service.build(data, Skill.COOKING, 80, 81, plan, false));
            candidates.add(salmon);
            Guidance guidance = service.build(data, Skill.COOKING, 80, 81, plan, false);
            assertNotNull(guidance);
            assertTrue(guidance.supplies.contains("Raw salmon"));
            assertFalse(guidance.supplies.contains("Raw mystery"));
        }
    }

    private UniversalActionRecipe resolve(String name, Membership membership)
    {
        return resolver.resolve(new ActionDef(Skill.COOKING, "test:cooking", name,
                1, 1, "test", Membership.F2P), 7, membership);
    }

    private static ReviewedActionRecipeCatalog.Recipe[] definitions()
    {
        return BundledCatalogLoader.array("/content/catalogs/reviewed-action-recipes.json",
                ReviewedActionRecipeCatalog.Recipe[].class);
    }

    private static void rejects(Consumer<ReviewedActionRecipeCatalog.Recipe> mutation)
    {
        ReviewedActionRecipeCatalog.Recipe[] recipes = definitions();
        mutation.accept(recipes[0]);
        assertThrows(IllegalStateException.class, () -> ReviewedActionRecipeCatalog.validate(recipes));
    }
}
