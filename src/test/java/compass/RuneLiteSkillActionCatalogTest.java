package compass;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
}
