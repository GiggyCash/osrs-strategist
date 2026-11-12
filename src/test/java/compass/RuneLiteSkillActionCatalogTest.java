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
        String key = Names.actionKey(name);
        if (skill == Skill.HERBLORE
                && (key.endsWith("_3") || key.endsWith("_4")))
            return key.substring(0, key.length() - 2);
        return key;
    }

    private static final class LegacyRecipe
    {
        String skill;
        String match;
    }
}
