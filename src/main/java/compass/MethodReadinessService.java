package compass;

import java.util.*;
import javax.inject.Inject;

/** Generic contract evaluation; all method/recipe/access identities come from bundled data. */
final class MethodReadinessService
{
    private final MethodIntelligenceCatalog catalog;
    private final MethodPreparationService preparation;

    @Inject
    MethodReadinessService(MethodIntelligenceCatalog catalog, MethodPreparationService preparation)
    {
        this.catalog = catalog;
        this.preparation = preparation;
    }

    MethodReadiness evaluate(GameData data, TrainingMethod training, boolean group)
    {
        MethodIntelligenceCatalog.Method method = catalog.method(training.id);
        if (method == null) return null;
        RequirementState access = access(data, method);
        String reason = method.unresolved != null ? method.unresolved
                : access == RequirementState.VERIFIED ? "The modeled access requirements are verified."
                : "Method access is unresolved or unavailable for this account.";
        int review = Math.min(99, training.maxLevel + 1);
        int xp = 0;
        if (data != null && data.account() != null)
        {
            int level = data.account().level(training.getSkill());
            xp = data.account().xp(training.getSkill());
            if (xp <= 0) xp = net.runelite.api.Experience.getXpForLevel(level);
            for (String id : method.recipes)
                if (catalog.recipe(id).level > level) review = Math.min(review, catalog.recipe(id).level);
        }
        MethodReadiness best = new MethodReadiness(access, null, null, method, null, 0, reason, review, xp);
        if (access != RequirementState.VERIFIED) return best;
        for (String id : method.recipes)
        {
            MethodIntelligenceCatalog.Recipe recipe = catalog.recipe(id);
            if (recipe.skill != training.getSkill() || data.account().level(recipe.skill) < recipe.level) continue;
            int held = recipe.maximumBatch;
            for (MethodIntelligenceCatalog.Ingredient input : recipe.inputs)
                held = Math.min(held, data.inventory() == null ? 0
                        : data.inventory().quantityOf(input.itemId) / input.quantity);
            int batch = held > 0 ? held : recipe.workingBatch;
            MethodCapacity capacity = MethodCapacity.evaluate(data.inventory(), data.equipment(), recipe, batch);
            List<MethodInput> needs = new ArrayList<>();
            for (MethodIntelligenceCatalog.Ingredient input : recipe.inputs)
                needs.add(new MethodInput(input.name, input.itemId, input.quantity * batch));
            Set<Integer> equippedReusable = new HashSet<>();
            for (MethodIntelligenceCatalog.Ingredient tool : recipe.tools)
            {
                needs.add(new MethodInput(tool.name, tool.itemId, tool.quantity));
                if (tool.equippedAllowed) equippedReusable.add(tool.itemId);
            }
            MethodPreparation prep = preparation.evaluate(data, needs, group,
                    method.bankLoop || method.bankSupplies, method.purchaseAllowed, equippedReusable);
            MethodReadiness candidate = new MethodReadiness(access, prep, capacity,
                    method, recipe, batch, reason, review, xp);
            if (best.recipe == null || candidate.adjustment() > best.adjustment()
                    || candidate.adjustment() == best.adjustment() && recipe.level > best.recipe.level)
                best = candidate;
        }
        return best;
    }

    private static RequirementState access(GameData data, MethodIntelligenceCatalog.Method method)
    {
        if (data == null || data.account() == null) return RequirementState.CHECK_NEEDED;
        AccountMode mode = AccountMode.fromTypeCode(data.account().modeCode());
        if (method.membersOnly && data.account().membership() != Membership.P2P
                || method.bankLoop && (mode == AccountMode.ULTIMATE_IRONMAN || mode == AccountMode.UNKNOWN))
            return RequirementState.BLOCKED;
        if (method.requiredQuest != null && (data.quests() == null
                || data.quests().statusOf(method.requiredQuest) != QuestStatus.COMPLETE))
            return RequirementState.CHECK_NEEDED;
        if (method.observedAccess != null && (data.accessMemory() == null
                || !data.accessMemory().hasObserved(method.observedAccess)))
            return RequirementState.CHECK_NEEDED;
        if (method.diaryRegion != null && (data.diaries() == null
                || !data.diaries().isTierComplete(method.diaryRegion, method.diaryTier)))
            return RequirementState.CHECK_NEEDED;
        return method.unresolved != null ? RequirementState.CHECK_NEEDED : RequirementState.VERIFIED;
    }
}
