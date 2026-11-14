package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class BattlestaffRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void eachElementUsesTheReviewedChargedOrbAndBattlestaff()
    {
        String[] elements = {"Air", "Water", "Earth", "Fire"};
        int[] orbs = {573, 571, 575, 569};
        for (int i = 0; i < elements.length; i++)
        {
            UniversalActionRecipe recipe = resolver.resolve(action(elements[i] + " battlestaff", 1), 14, Membership.P2P);
            assertTrue(recipe.hasExactInputs());
            assertEquals(2, recipe.inputs.size());
            assertEquals(1391, recipe.inputs.get(0).itemId);
            assertEquals(orbs[i], recipe.inputs.get(1).itemId);
            assertEquals(14, recipe.inputs.get(0).quantity);
            assertEquals(14, recipe.inputs.get(1).quantity);
        }
    }

    @Test
    public void substringsAndWrongMembershipCannotProveAssembly()
    {
        for (String name : Arrays.asList("Future air battlestaff", "Ice battlestaff", "Mystic air staff"))
            assertFalse(resolver.resolve(action(name, 1), 1, Membership.P2P).hasExactInputs());
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertFalse(resolver.resolve(action("Air battlestaff", 1), 1, membership).hasExactInputs());
        ActionDef transmutation = new ActionDef(Skill.MAGIC, "test:transmute", "Air battlestaff",
                1, 1, "test", Membership.P2P);
        assertFalse(resolver.resolve(transmutation, 1, Membership.P2P).hasExactInputs());
    }

    @Test
    public void unsupportedHigherXpStaffCannotWinGuidanceSelection()
    {
        List<ActionDef> actions = new ArrayList<>();
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override public List<ActionDef> actionsFor(Skill skill) { return actions; }
        };
        UniversalSkillActionGuidanceService service = new UniversalSkillActionGuidanceService(catalog,
                resolver, new SkillingXpModifierService(), TestFixtures.accountResourcePlanner());
        TrainingMethod method = new TrainingMethod("test:staff", Skill.CRAFTING, 1, 99,
                "Air staff assembly", "Use the Air orb on the Battlestaff.", 10, 10, 10,
                AttentionLevel.LOW, 10, 0, Collections.emptyList(), Confidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(method, "test", Confidence.VERIFIED, Collections.emptyList());
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            actions.clear();
            actions.add(action("Future air battlestaff", 10000));
            assertNull(service.build(data, Skill.CRAFTING, 80, 81, plan, false));
            actions.add(action("Air battlestaff", 137.5f));
            Guidance guidance = service.build(data, Skill.CRAFTING, 80, 81, plan, false);
            assertNotNull(guidance);
            assertTrue(guidance.supplies.contains("Air orb"));
            assertTrue(guidance.supplies.contains("Battlestaff"));
        }
    }

    private static ActionDef action(String name, float xp)
    {
        return new ActionDef(Skill.CRAFTING, "test:staff", name, 1, xp, "test", Membership.F2P);
    }
}
