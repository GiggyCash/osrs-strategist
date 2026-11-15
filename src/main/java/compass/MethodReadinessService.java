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
        List<MethodReadiness> candidates = evaluateCandidates(data, training, group);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /** Retain independent recipe plans until strategy/session ranking; never merge their evidence. */
    List<MethodReadiness> evaluateCandidates(GameData data, TrainingMethod training, boolean group)
    {
        MethodIntelligenceCatalog.Method method = catalog.method(training.id);
        if (method == null) return Collections.emptyList();
        RequirementState access = access(data, method);
        String reason = method.unresolved != null ? method.unresolved
                : access == RequirementState.VERIFIED ? "The modeled access requirements are verified."
                : "Method access is unresolved or unavailable for this account.";
        int review = Math.min(99, training.maxLevel + 1);
        int xp = 0;
        MethodIntelligenceCatalog.Recipe nextUnlock = null;
        if (data != null && data.account() != null)
        {
            int level = data.account().level(training.getSkill());
            xp = data.account().xp(training.getSkill());
            if (xp <= 0) xp = net.runelite.api.Experience.getXpForLevel(level);
            for (String id : method.recipes)
            {
                MethodIntelligenceCatalog.Recipe recipe = catalog.recipe(id);
                if (recipe.level > level && (nextUnlock == null || recipe.level < nextUnlock.level))
                    nextUnlock = recipe;
            }
            if (nextUnlock != null) review = Math.min(review, nextUnlock.level);
        }
        MethodReadiness diagnostic = new MethodReadiness(access, null, null, method, null, 0,
                reason, review, xp, null, nextUnlock);
        if (access != RequirementState.VERIFIED) return Collections.singletonList(diagnostic);
        List<MethodReadiness> candidates = new ArrayList<>();
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
            candidates.add(new MethodReadiness(access, prep, capacity,
                    method, recipe, batch, reason, review, xp,
                    recipe.economics.get(AccountMode.fromTypeCode(data.account().modeCode())), nextUnlock));
        }
        if (candidates.isEmpty()) return Collections.singletonList(diagnostic);
        // Preserve the existing preference as a fallback, not an XP/economic value claim.
        // Feasibility is a hard tier even if future preparation scores change scale.
        candidates.sort(Comparator.comparing((MethodReadiness candidate) -> !candidate.actionable())
                .thenComparing(Comparator.comparingDouble(MethodReadiness::adjustment).reversed())
                .thenComparing(Comparator.comparingInt((MethodReadiness candidate) -> candidate.recipe.level).reversed()));
        return Collections.unmodifiableList(candidates);
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
