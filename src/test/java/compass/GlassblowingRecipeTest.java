package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class GlassblowingRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void sevenExistingCalculatorActionsConsumeOneGlassAndRetainPipeGuidance()
    {
        for (String name : Arrays.asList("Beer glass", "Empty candle lantern", "Empty oil lamp",
                "Vial", "Fishbowl", "Unpowered orb", "Lantern lens"))
        {
            UniversalActionRecipe recipe = resolver.resolve(action(name, 1), 27, Membership.P2P);
            assertTrue(name, recipe.hasExactInputs());
            assertEquals(1, recipe.inputs.size());
            assertEquals(1775, recipe.inputs.get(0).itemId);
            assertEquals(27, recipe.inputs.get(0).quantity);
            assertTrue(recipe.getSetup().contains("glassblowing pipe"));
        }
        assertTrue(resolver.resolve(action("Fishbowl", 1), 1, Membership.P2P)
                .getSetup().contains("empty fishbowl"));
    }

    @Test
    public void freeItemMembershipDoesNotMakeItsCraftingRecipeFree()
    {
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            for (String name : Arrays.asList("Beer glass", "Vial"))
                assertFalse(resolver.resolve(action(name, 1), 1, membership).hasExactInputs());
    }

    @Test
    public void filledOrInventedItemsCannotInheritAGlassRecipe()
    {
        for (String name : Arrays.asList("Oil lamp", "Candle lantern", "Vial of water",
                "Future vial", "Fishbowl (water)", "Enchanted lantern lens"))
            assertFalse(name, resolver.resolve(action(name, 1), 1, Membership.P2P).hasExactInputs());
    }

    @Test
    public void highXpUnknownVialCannotDisplaceReviewedGuidance()
    {
        List<ActionDef> actions = new ArrayList<>();
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override public List<ActionDef> actionsFor(Skill skill) { return actions; }
        };
        UniversalSkillActionGuidanceService service = new UniversalSkillActionGuidanceService(catalog,
                resolver, new SkillingXpModifierService(), TestFixtures.accountResourcePlanner());
        TrainingMethod method = new TrainingMethod("test:glass", Skill.CRAFTING, 1, 99,
                "Craft vial", "Blow molten glass into a vial.", 10, 10, 10,
                AttentionLevel.LOW, 10, 0, Collections.emptyList(), Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "test", Confidence.VERIFIED, Collections.emptyList());
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            actions.clear();
            actions.add(action("Future vial", 10000));
            assertNull(service.build(data, Skill.CRAFTING, 80, 81, plan, false));
            actions.add(action("Vial", 35));
            Guidance guidance = service.build(data, Skill.CRAFTING, 80, 81, plan, false);
            assertNotNull(guidance);
            assertTrue(guidance.supplies.contains("Molten glass"));
        }
    }

    private static ActionDef action(String name, float xp)
    {
        return new ActionDef(Skill.CRAFTING, "test:glass", name, 1, xp, "test", Membership.F2P);
    }
}
