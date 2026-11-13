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
    public void reviewedHerbloreNamesPreserveDosePunctuation()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<String> identities = new ArrayList<>();
        for (ActionDef action : catalog.actionsFor(Skill.HERBLORE))
            identities.add(action.id + "=" + action.name);

        for (String identity : new String[] {
                "runelite:herblore:attack_potion_3=Attack potion(3)",
                "runelite:herblore:antipoison_3=Antipoison(3)",
                "runelite:herblore:strength_potion_3=Strength potion(3)",
                "runelite:herblore:restore_potion_3=Restore potion(3)",
                "runelite:herblore:energy_potion_3=Energy potion(3)",
                "runelite:herblore:defence_potion_3=Defence potion(3)",
                "runelite:herblore:agility_potion_3=Agility potion(3)",
                "runelite:herblore:combat_potion_3=Combat potion(3)",
                "runelite:herblore:prayer_potion_3=Prayer potion(3)"})
            assertTrue(identity, identities.contains(identity));
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
    public void spinningRecipesUseCurrentRuneLiteActionIdentities()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<ActionDef> crafting = catalog.actionsFor(Skill.CRAFTING);
        assertTrue(crafting.stream().anyMatch(action ->
                "runelite:crafting:ball_of_wool".equals(action.id)
                        && "Ball of wool".equals(action.name)));
        assertTrue(crafting.stream().anyMatch(action ->
                "runelite:crafting:bow_string".equals(action.id)
                        && "Bow string".equals(action.name)));
    }

    @Test
    public void constructionRecipesUseCurrentRuneLiteActionIdentities()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<String> identities = new ArrayList<>();
        for (ActionDef action : catalog.actionsFor(Skill.CONSTRUCTION))
            identities.add(action.id + "=" + Names.actionKey(action.name));

        for (String identity : new String[] {
                "runelite:construction:oak_larder=oak_larder",
                "runelite:construction:oak_door=oak_door",
                "runelite:construction:mahogany_table=mahogany_table",
                "runelite:construction:teak_garden_bench=teak_garden_bench",
                "runelite:construction:mounted_mythical_cape=mounted_mythical_cape"})
            assertTrue(identity, identities.contains(identity));
    }

    @Test
    public void magicRecipesUseCurrentRuneLiteActionIdentities()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<String> identities = new ArrayList<>();
        for (ActionDef action : catalog.actionsFor(Skill.MAGIC))
            identities.add(action.id + "=" + Names.actionKey(action.name));

        for (String identity : new String[] {
                "runelite:magic:wind_strike=wind_strike",
                "runelite:magic:fire_bolt=fire_bolt",
                "runelite:magic:fire_blast=fire_blast",
                "runelite:magic:high_level_alchemy=high_level_alchemy",
                "runelite:magic:curse=curse"})
            assertTrue(identity, identities.contains(identity));
    }

    @Test
    public void f2pPotteryRecipesUseCurrentRuneLiteActionIdentities()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<String> identities = new ArrayList<>();
        for (ActionDef action : catalog.actionsFor(Skill.CRAFTING))
            identities.add(action.id + "=" + Names.actionKey(action.name));

        for (String identity : new String[] {
                "runelite:crafting:unfired_pot=unfired_pot",
                "runelite:crafting:unfired_pie_dish=unfired_pie_dish",
                "runelite:crafting:unfired_bowl=unfired_bowl",
                "runelite:crafting:unfired_plant_pot=unfired_plant_pot",
                "runelite:crafting:pot=pot",
                "runelite:crafting:pie_dish=pie_dish",
                "runelite:crafting:bowl=bowl",
                "runelite:crafting:empty_plant_pot=empty_plant_pot"})
            assertTrue(identity, identities.contains(identity));
    }

    @Test
    public void glassAndTiaraActionsUseCurrentRuneLiteIdentities()
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog();
        List<String> identities = new ArrayList<>();
        for (ActionDef action : catalog.actionsFor(Skill.CRAFTING))
            identities.add(action.id + "=" + Names.actionKey(action.name));

        for (String identity : new String[] {
                "runelite:crafting:molten_glass=molten_glass",
                "runelite:crafting:tiara=tiara",
                "runelite:crafting:gold_tiara=gold_tiara"})
            assertTrue(identity, identities.contains(identity));
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
