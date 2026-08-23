package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Evaluates ALL/ANY item alternatives without treating unobserved storage as empty. */
public final class ItemRequirementEvaluator
{
    public ItemRequirementResult evaluate(ItemRequirementExpression expression,
            StrategyDataBundle data, boolean useGroupStorage)
    {
        if (expression == null)
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
        return evaluate(expression, data, items, useGroupStorage);
    }

    private ItemRequirementResult evaluate(ItemRequirementExpression expression,
            StrategyDataBundle data, ObservedItemIndex items,
            boolean useGroupStorage)
    {
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM)
            return item(expression, data, items, useGroupStorage);

        List<ItemRequirementResult> results = new ArrayList<>();
        for (ItemRequirementExpression child : expression.getChildren())
            results.add(evaluate(child, data, items, useGroupStorage));

        if (expression.getKind() == ItemRequirementExpression.Kind.ANY_OF)
        {
            for (ItemRequirementResult result : results)
                if (result.isSatisfied()) return result;

            boolean needsVerification = results.stream().anyMatch(result ->
                    result.getState() == RequirementState.CHECK_NEEDED);
            RequirementState state = needsVerification
                    ? RequirementState.CHECK_NEEDED : RequirementState.BLOCKED;
            // If an alternative may still exist in unobserved storage, there is
            // no proven shortfall to source. Once every branch is proven absent,
            // carry the smallest concrete branch into acquisition guidance.
            List<ResolvedMethodInput> inputs = needsVerification
                    ? Collections.emptyList() : bestAlternativeInputs(results);
            return new ItemRequirementResult(state,
                    (needsVerification
                            ? "Check whether you own one of: " : "Get one of: ")
                            + expression.label(), inputs);
        }

        List<String> actions = new ArrayList<>();
        List<ResolvedMethodInput> inputs = new ArrayList<>();
        RequirementState state = RequirementState.VERIFIED;
        for (ItemRequirementResult result : results)
        {
            if (!result.isSatisfied() && !result.getAction().isEmpty())
                actions.add(result.getAction());
            inputs.addAll(result.getMissingInputs());
            if (result.getState() == RequirementState.BLOCKED)
                state = RequirementState.BLOCKED;
            else if (result.getState() == RequirementState.CHECK_NEEDED
                    && state != RequirementState.BLOCKED)
                state = RequirementState.CHECK_NEEDED;
        }
        return new ItemRequirementResult(state, String.join("; ", actions), inputs);
    }

    private ItemRequirementResult item(ItemRequirementExpression expression,
            StrategyDataBundle data, ObservedItemIndex items,
            boolean useGroupStorage)
    {
        String[] names = expression.getItemNames().toArray(new String[0]);
        int owned;
        boolean observed;
        switch (expression.getScope())
        {
            case EQUIPPED:
                owned = items.equippedQuantity(names);
                observed = data != null && data.getEquipment() != null;
                break;
            case CARRIED:
                owned = quantityIn(data == null || data.getInventory() == null
                        ? null : data.getInventory().getItems(), names);
                observed = data != null && data.getInventory() != null;
                break;
            case CARRIED_OR_EQUIPPED:
                owned = quantityIn(data == null || data.getInventory() == null
                        ? null : data.getInventory().getItems(), names)
                        + items.equippedQuantity(names);
                observed = data != null && data.getInventory() != null
                        && data.getEquipment() != null;
                break;
            case OWNED_OR_RETRIEVABLE:
                owned = items.quantity(names) + items.restrictedQuantity(names);
                observed = ownershipObserved(data, items, useGroupStorage)
                        && !isUim(data);
                break;
            case IMMEDIATELY_USABLE:
            default:
                owned = items.quantity(names);
                observed = ownershipObserved(data, items, useGroupStorage);
                break;
        }
        if (owned >= expression.getQuantity())
            return new ItemRequirementResult(RequirementState.VERIFIED, "");

        int shortfall = Math.max(0, expression.getQuantity() - owned);
        String target = String.join(" or ", expression.getItemNames());
        if (expression.getItemNames().size() > 1) target = "(" + target + ")";
        String action = (observed ? "Get " : "Check whether you own ")
                + shortfall + " × " + target;

        List<ResolvedMethodInput> inputs = Collections.emptyList();
        if (observed && expression.getItemNames().size() == 1)
        {
            inputs = Collections.singletonList(new ResolvedMethodInput(
                    expression.getItemNames().get(0), -1, shortfall));
        }
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                action, inputs);
    }

    private static List<ResolvedMethodInput> bestAlternativeInputs(
            List<ItemRequirementResult> results)
    {
        List<ResolvedMethodInput> best = Collections.emptyList();
        long bestCost = Long.MAX_VALUE;
        for (ItemRequirementResult result : results)
        {
            if (result.getMissingInputs().isEmpty()) continue;
            long cost = 0L;
            for (ResolvedMethodInput input : result.getMissingInputs())
                cost += Math.max(1, input.getQuantity());
            if (cost < bestCost)
            {
                bestCost = cost;
                best = result.getMissingInputs();
            }
        }
        return best;
    }

    private static boolean ownershipObserved(StrategyDataBundle data,
            ObservedItemIndex items, boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null) return false;
        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            return data.getInventory() != null && data.getEquipment() != null;
        if (!items.bankObserved()) return false;
        // When the player opted into Group Storage, an unobserved group store is
        // unknown rather than empty. Do not publish a fake GIM shortfall.
        return !mode.isGroupIronman() || !useGroupStorage
                || items.groupStorageObserved();
    }

    private static boolean isUim(StrategyDataBundle data)
    {
        return data != null && data.getAccount() != null
                && AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode())
                        == AccountMode.ULTIMATE_IRONMAN;
    }

    private static int quantityIn(Iterable<ItemStackSnapshot> stacks, String[] names)
    {
        if (stacks == null) return 0;
        int total = 0;
        for (ItemStackSnapshot stack : stacks)
        {
            if (stack == null || stack.getName() == null) continue;
            for (String name : names)
                if (name != null && stack.getName().trim().equalsIgnoreCase(name.trim()))
                {
                    total += Math.max(0, stack.getQuantity());
                    break;
                }
        }
        return total;
    }
}
