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
        Integer outputsPerAction;
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
        Boolean bankLoop, purchaseAllowed, membersOnly;
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
        this(load());
    }

    private static Bundle load()
    {
        Bundle[] bundles = BundledCatalogLoader.array("/content/catalogs/method-intelligence.json", Bundle[].class);
        if (bundles.length != 1) throw new IllegalStateException("Expected one method intelligence bundle");
        return bundles[0];
    }

    MethodIntelligenceCatalog(Bundle bundle)
    {
        if (bundle == null || bundle.recipes == null || bundle.methods == null)
            throw new IllegalStateException("Missing method intelligence records");
        for (Recipe recipe : bundle.recipes)
        {
            if (recipe == null) throw new IllegalStateException("Null method recipe");
            if (!text(recipe.id) || recipe.skill == null || recipe.level < 1 || recipe.level > 99
                    || recipe.workingBatch < 1 || recipe.maximumBatch < recipe.workingBatch
                    || recipe.maximumBatch > 28 || recipe.efficientBatch < 0
                    || recipe.efficientBatch > recipe.maximumBatch
                    || recipe.efficientBatch > 0 && recipe.efficientBatch < recipe.workingBatch
                    || recipe.inputs == null || recipe.inputs.isEmpty() || recipe.tools == null
                    || recipe.outputsPerAction == null || recipe.outputsPerAction < 0
                    || recipe.outputsPerAction > 28 || !text(recipe.action)
                    || !text(recipe.outputDescription)
                    || recipe.outputStackable && recipe.outputItemId <= 0)
                throw new IllegalStateException("Invalid method recipe");
            Set<Integer> ids = new HashSet<>();
            for (Ingredient input : recipe.inputs)
                if (input == null || input.itemId <= 0 || input.quantity < 1 || input.quantity > 28
                        || !text(input.name) || input.equippedAllowed || !ids.add(input.itemId))
                    throw new IllegalStateException("Invalid or duplicate recipe input: " + recipe.id);
            for (Ingredient tool : recipe.tools)
                if (tool == null || tool.itemId <= 0 || tool.quantity < 1 || tool.quantity > 28
                        || !text(tool.name) || !ids.add(tool.itemId))
                    throw new IllegalStateException("Invalid or overlapping reusable tool: " + recipe.id);
            if (ids.contains(recipe.outputItemId))
                throw new IllegalStateException("Output recycling requires a separate flow contract: " + recipe.id);
            if (recipes.put(recipe.id, recipe) != null)
                throw new IllegalStateException("Duplicate recipe: " + recipe.id);
        }
        for (Method method : bundle.methods)
        {
            if (method == null) throw new IllegalStateException("Null method contract");
            if (method.ids == null || method.ids.isEmpty() || method.recipes == null
                    || !text(method.location) || method.source == null || method.reviewed == null
                    || method.bankLoop == null || method.purchaseAllowed == null || method.membersOnly == null
                    || (method.diaryRegion == null) != (method.diaryTier == null)
                    || method.diaryRegion != null && !text(method.diaryRegion)
                    || method.requiredQuest != null && !text(method.requiredQuest)
                    || method.observedAccess != null && !text(method.observedAccess)
                    || !method.source.startsWith("https://oldschool.runescape.wiki/w/")
                    || method.sourceRevision <= 0
                    || method.recipes.isEmpty() && !text(method.unresolved))
                throw new IllegalStateException("Incomplete method contract");
            if (LocalDate.parse(method.reviewed).isAfter(LocalDate.now()))
                throw new IllegalStateException("Method review date is in the future");
            Set<String> references = new HashSet<>();
            Skill skill = null;
            for (String id : method.recipes)
            {
                Recipe recipe = recipes.get(id);
                if (recipe == null || !references.add(id))
                    throw new IllegalStateException("Unknown or repeated recipe: " + id);
                if (skill != null && skill != recipe.skill)
                    throw new IllegalStateException("Method recipes must use the same skill");
                skill = recipe.skill;
            }
            for (String id : method.ids)
                if (!text(id) || methods.put(id, method) != null)
                    throw new IllegalStateException("Duplicate method contract: " + id);
        }
    }

    private static boolean text(String value) { return value != null && !value.trim().isEmpty(); }

    Method method(String id) { return methods.get(id); }
    Recipe recipe(String id) { return recipes.get(id); }
    Set<String> methodIds() { return Collections.unmodifiableSet(methods.keySet()); }
}
