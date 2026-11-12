package compass;

import net.runelite.api.Skill;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuneLiteSkillActionCatalogTest
{
    @Test
    public void maintainedRuneLiteActionEnumsAreVisible()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        assertFalse(catalog.actionsFor(Skill.MINING).isEmpty());
        assertFalse(catalog.actionsFor(Skill.COOKING).isEmpty());
        assertFalse(catalog.actionsFor(Skill.CRAFTING).isEmpty());
        assertFalse(catalog.actionsFor(Skill.AGILITY).isEmpty());
        long coveredSkills = java.util.Arrays.stream(Skill.values())
                .filter(skill -> !catalog.actionsFor(skill).isEmpty()).count();
        assertTrue(coveredSkills >= 16);
    }

    @Test
    public void oakDoorProfileUsesTheCurrentRuneLiteActionIdentity()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        ActionDef oakDoor = catalog.actionsFor(Skill.CONSTRUCTION).stream()
                .filter(action -> "runelite:construction:oak_door"
                        .equals(action.id))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("oak_door", Names.actionKey(oakDoor.name));
        MethodProfile profile = new MethodExecutionProfileCatalog()
                .forMethod("construction_oak_doors");
        assertTrue(profile.actionTerms.contains(oakDoor.id));
    }

    @Test
    public void diagnosticHerbloreNamesPreserveDosePunctuation()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        ActionDef attackPotion = catalog.actionsFor(Skill.HERBLORE).stream()
                .filter(action -> "runelite:herblore:attack_potion_3"
                        .equals(action.id))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("Attack potion(3)", attackPotion.name);
    }

    @Test
    public void headlessArrowRecipeUsesCurrentRuneLiteActionIdentity()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        ActionDef headless = catalog.actionsFor(Skill.FLETCHING).stream()
                .filter(action -> "Headless arrow".equals(action.name))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals("headless_arrow", Names.actionKey(headless.name));
    }

    @Test
    public void everyLegacyRecipeNamesAPinnedRuneLiteAction()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        LegacyRecipe[] recipes = BundledCatalogLoader.array(
                "/content/catalogs/action-recipes.json", LegacyRecipe[].class);
        List<String> missing = new ArrayList<>();
        for (LegacyRecipe recipe : recipes)
        {
            Skill skill = Skill.valueOf(recipe.skill);
            boolean found = catalog.actionsFor(skill).stream()
                    .anyMatch(action -> legacyActionKey(skill, action.name)
                            .equals(Names.actionKey(recipe.match)));
            if (!found) missing.add(recipe.skill + ":" + recipe.match);
        }
        assertTrue(missing.toString(), missing.isEmpty());
    }

    private static String legacyActionKey(Skill skill, String name)
    {
        if (skill == Skill.HERBLORE
                && (name.endsWith("(3)") || name.endsWith("(4)")))
            name = name.substring(0, name.length() - 3);
        return Names.actionKey(name);
    }

    private static final class LegacyRecipe
    {
        String skill;
        String match;
    }
}
