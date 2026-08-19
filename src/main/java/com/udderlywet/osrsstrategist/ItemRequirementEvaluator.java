package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
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
        return evaluate(expression, data, items);
    }

    private ItemRequirementResult evaluate(ItemRequirementExpression expression,
            StrategyDataBundle data, ObservedItemIndex items)
    {
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM)
            return item(expression, data, items);

        List<ItemRequirementResult> results = new ArrayList<>();
        for (ItemRequirementExpression child : expression.getChildren())
            results.add(evaluate(child, data, items));

        if (expression.getKind() == ItemRequirementExpression.Kind.ANY_OF)
        {
            for (ItemRequirementResult result : results)
                if (result.isSatisfied()) return result;
            RequirementState state = results.stream().anyMatch(result ->
                    result.getState() == RequirementState.CHECK_NEEDED)
                    ? RequirementState.CHECK_NEEDED : RequirementState.BLOCKED;
            return new ItemRequirementResult(state,
                    (state == RequirementState.CHECK_NEEDED
                            ? "Check whether you own one of: " : "Get one of: ")
                            + expression.label());
        }

        List<String> actions = new ArrayList<>();
        RequirementState state = RequirementState.VERIFIED;
        for (ItemRequirementResult result : results)
        {
            if (!result.isSatisfied() && !result.getAction().isEmpty())
                actions.add(result.getAction());
            if (result.getState() == RequirementState.BLOCKED)
                state = RequirementState.BLOCKED;
            else if (result.getState() == RequirementState.CHECK_NEEDED
                    && state != RequirementState.BLOCKED)
                state = RequirementState.CHECK_NEEDED;
        }
        return new ItemRequirementResult(state, String.join("; ", actions));
    }

    private ItemRequirementResult item(ItemRequirementExpression expression,
            StrategyDataBundle data, ObservedItemIndex items)
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
                observed = ownershipObserved(data, items)
                        && !isUim(data);
                break;
            case IMMEDIATELY_USABLE:
            default:
                owned = items.quantity(names);
                observed = ownershipObserved(data, items);
                break;
        }
        if (owned >= expression.getQuantity())
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        int shortfall = Math.max(0, expression.getQuantity() - owned);
        String action = (observed ? "Get " : "Check whether you own ")
                + shortfall + " × " + expression.label();
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED, action);
    }

    private static boolean ownershipObserved(StrategyDataBundle data,
            ObservedItemIndex items)
    {
        if (data == null || data.getAccount() == null) return false;
        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            return data.getInventory() != null && data.getEquipment() != null;
        return items.bankObserved();
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
