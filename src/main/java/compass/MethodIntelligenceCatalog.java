package compass;

import java.time.LocalDate;
import java.util.*;
import net.runelite.api.Skill;

/** Reviewed method contracts. Missing contracts never imply readiness. */
final class MethodIntelligenceCatalog
{
    static final class Ingredient
    {
        int itemId, quantity;
        String name;
        boolean stackable, equippedAllowed;
    }

    static final class Recipe
    {
        String id, action, outputDescription;
        Skill skill;
        int level, workingBatch, maximumBatch, efficientBatch;
        List<Ingredient> inputs;
        List<Ingredient> tools = new ArrayList<>();
        // Worst-case retained outputs, including failed processing. No auto-disposal.
        int outputsPerAction;
        boolean outputStackable;
        int outputItemId;
    }

    static final class Method
    {
        List<String> ids, recipes;
        String location, source, reviewed, unresolved;
        long sourceRevision;
        String requiredQuest, observedAccess;
        String diaryRegion;
        DiaryTier diaryTier;
        boolean bankLoop, purchaseAllowed, membersOnly;
    }

    static final class Bundle
    {
        List<Recipe> recipes;
        List<Method> methods;
    }

    private final Map<String, Method> methods = new LinkedHashMap<>();
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    MethodIntelligenceCatalog()
    {
        this(BundledCatalogLoader.array("/content/catalogs/method-intelligence.json",
                Bundle[].class)[0]);
    }

    MethodIntelligenceCatalog(Bundle bundle)
    {
        if (bundle == null || bundle.recipes == null || bundle.methods == null)
            throw new IllegalStateException("Missing method intelligence records");
        for (Recipe recipe : bundle.recipes)
        {
            if (recipe == null) throw new IllegalStateException("Null method recipe");
            if (recipe.id == null || recipe.skill == null || recipe.level < 1
                    || recipe.workingBatch < 1 || recipe.maximumBatch < recipe.workingBatch
                    || recipe.maximumBatch > 28 || recipe.efficientBatch < 0
                    || recipe.efficientBatch > recipe.maximumBatch
                    || recipe.inputs == null || recipe.inputs.isEmpty() || recipe.tools == null
                    || recipe.outputsPerAction < 0 || recipe.outputsPerAction > 28 || recipe.action == null
                    || recipe.outputDescription == null
                    || recipe.outputStackable && recipe.outputItemId <= 0)
                throw new IllegalStateException("Invalid method recipe");
            Set<Integer> ids = new HashSet<>();
            for (Ingredient input : recipe.inputs)
                if (input == null || input.itemId <= 0 || input.quantity < 1 || input.quantity > 28
                        || input.name == null || !ids.add(input.itemId))
                    throw new IllegalStateException("Invalid or duplicate recipe input: " + recipe.id);
            for (Ingredient tool : recipe.tools)
                if (tool == null || tool.itemId <= 0 || tool.quantity < 1 || tool.quantity > 28
                        || tool.name == null || !ids.add(tool.itemId))
                    throw new IllegalStateException("Invalid or overlapping reusable tool: " + recipe.id);
            if (recipe.outputStackable && ids.contains(recipe.outputItemId))
                throw new IllegalStateException("Output recycling requires a separate flow contract: " + recipe.id);
            if (recipes.put(recipe.id, recipe) != null)
                throw new IllegalStateException("Duplicate recipe: " + recipe.id);
        }
        for (Method method : bundle.methods)
        {
            if (method == null) throw new IllegalStateException("Null method contract");
            if (method.ids == null || method.ids.isEmpty() || method.recipes == null
                    || method.location == null || method.source == null || method.reviewed == null
                    || !method.source.startsWith("https://oldschool.runescape.wiki/w/")
                    || method.sourceRevision <= 0
                    || method.recipes.isEmpty() && method.unresolved == null)
                throw new IllegalStateException("Incomplete method contract");
            LocalDate.parse(method.reviewed);
            for (String id : method.recipes)
                if (!recipes.containsKey(id)) throw new IllegalStateException("Unknown recipe: " + id);
            for (String id : method.ids)
                if (id == null || methods.put(id, method) != null)
                    throw new IllegalStateException("Duplicate method contract: " + id);
        }
    }

    Method method(String id) { return methods.get(id); }
    Recipe recipe(String id) { return recipes.get(id); }
    Set<String> methodIds() { return Collections.unmodifiableSet(methods.keySet()); }
}
