package compass;

import java.util.*;
import net.runelite.api.Skill;

/** Reviewed consumed inputs shared by action guidance and method capacity contracts. */
final class ReviewedActionRecipeCatalog
{
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    ReviewedActionRecipeCatalog()
    {
        for (Recipe recipe : validate(BundledCatalogLoader.array(
                "/content/catalogs/reviewed-action-recipes.json", Recipe[].class)))
            recipes.put(recipe.skill + ":" + recipe.match, recipe);
    }

    Recipe recipe(Skill skill, String name) { return recipes.get(skill + ":" + name); }

    static Recipe[] validate(Recipe[] recipes)
    {
        Set<String> keys = new HashSet<>();
        for (Recipe recipe : recipes)
        {
            if (recipe == null || recipe.skill == null || recipe.match == null || recipe.match.isEmpty()
                    || !recipe.match.equals(recipe.match.toLowerCase(Locale.ROOT)) || recipe.contains
                    || !keys.add(recipe.skill + ":" + recipe.match) || recipe.membersOnly == null
                    || recipe.setup == null || recipe.setup.trim().isEmpty()
                    || recipe.source == null || !recipe.source.startsWith("https://oldschool.runescape.wiki/w/")
                    || recipe.sourceRevision <= 0 || recipe.reviewed == null
                    || java.time.LocalDate.parse(recipe.reviewed).isAfter(java.time.LocalDate.now())
                    || recipe.inputs == null || recipe.inputs.length == 0 || recipe.units == null
                    || recipe.itemIds == null || recipe.stackable == null
                    || recipe.inputs.length != recipe.stackable.length || recipe.inputs.length != recipe.units.length
                    || recipe.inputs.length != recipe.itemIds.length)
                throw new IllegalStateException("Invalid reviewed action recipe");
            Skill.valueOf(recipe.skill);
            Set<Integer> ids = new HashSet<>();
            for (int i = 0; i < recipe.inputs.length; i++)
                if (recipe.inputs[i] == null || recipe.inputs[i].trim().isEmpty() || recipe.units[i] <= 0
                        || recipe.itemIds[i] <= 0 || recipe.stackable[i] == null || !ids.add(recipe.itemIds[i]))
                    throw new IllegalStateException("Invalid reviewed recipe ingredient");
        }
        return recipes;
    }

    static final class Recipe
    {
        String skill, match, setup, source, reviewed;
        String[] inputs;
        int[] units, itemIds;
        Boolean[] stackable;
        boolean contains;
        Boolean membersOnly;
        long sourceRevision;

        UniversalActionRecipe build(int count, Membership membership)
        {
            if (count <= 0 || membersOnly && membership != Membership.P2P)
                return UniversalActionRecipe.unknown("Recipe quantity or members access is unresolved.");
            List<MethodInput> result = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++)
            {
                if (count > Integer.MAX_VALUE / units[i])
                    return UniversalActionRecipe.unknown("Recipe quantity exceeds supported planning capacity.");
                result.add(new MethodInput(inputs[i], itemIds[i], count * units[i]));
            }
            return new UniversalActionRecipe(result, setup, true);
        }
    }
}
