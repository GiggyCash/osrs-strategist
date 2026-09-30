package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class DartRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void ordinaryDartsCountIndividualTipsAndFeathersWithExactIds()
    {
        String[] metals = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune", "Amethyst", "Dragon"};
        int[] tips = {819, 820, 821, 822, 823, 824, 25853, 11232};
        for (int i = 0; i < metals.length; i++)
            for (int count : new int[] {1, 10, 11})
            {
                UniversalActionRecipe recipe = resolver.resolve(action(metals[i] + " dart", 1), count, Membership.P2P);
                assertTrue(recipe.hasExactInputs());
                assertEquals(tips[i], recipe.inputs.get(0).itemId);
                assertEquals(314, recipe.inputs.get(1).itemId);
                assertEquals(count, recipe.inputs.get(0).quantity);
                assertEquals(count, recipe.inputs.get(1).quantity);
            }
    }

    @Test
    public void unsupportedVariantsAndFreeAccountsDoNotInheritMetalDartInputs()
    {
        for (String name : Arrays.asList("Future dart", "Atlatl dart", "Headless atlatl dart", "Bronze dart(p)"))
            assertFalse(name, resolver.resolve(action(name, 1), 10, Membership.P2P).hasExactInputs());
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertFalse(resolver.resolve(action("Bronze dart", 1), 10, membership).hasExactInputs());
    }

    @Test
    public void unsupportedHighXpVariantCannotWinGuidanceForMainIronOrUim()
    {
        List<ActionDef> actions = new ArrayList<>();
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override public List<ActionDef> actionsFor(Skill skill) { return actions; }
        };
        UniversalSkillActionGuidanceService service = new UniversalSkillActionGuidanceService(catalog,
                resolver, new SkillingXpModifierService(), TestFixtures.accountResourcePlanner());
        TrainingMethod method = new TrainingMethod("test:darts", Skill.FLETCHING, 1, 99,
                "Fletch darts", "Attach feathers to dart tips.", 10, 10, 10,
                AttentionLevel.LOW, 10, 0, Collections.emptyList(), Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "test", Confidence.VERIFIED, Collections.emptyList());
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            actions.clear(); actions.add(action("Future dart", 10000));
            assertNull(service.build(data, Skill.FLETCHING, 80, 81, plan, false));
            actions.add(action("Bronze dart", 1.8f));
            Guidance guidance = service.build(data, Skill.FLETCHING, 80, 81, plan, false);
            assertNotNull(guidance);
            assertTrue(guidance.supplies.contains("Bronze dart tip"));
            assertTrue(guidance.supplies.contains("Feather"));
        }
    }

    private static ActionDef action(String name, float xp)
    {
        return new ActionDef(Skill.FLETCHING, "test:darts", name, 1, xp, "test", Membership.F2P);
    }
}
