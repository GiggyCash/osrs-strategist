package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;

/** Evaluates ALL/ANY item alternatives without treating unobserved storage as empty. */
public final class ItemRequirementEvaluator
{
    public ItemRequirementResult evaluate(ItemRequirementExpression expression,
            GameData data, boolean useGroupStorage)
    {
        if (expression == null)
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        var items = new ItemIndex(data, useGroupStorage);
        return evaluate(expression, data, items, useGroupStorage);
    }

    private ItemRequirementResult evaluate(ItemRequirementExpression expression,
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM)
            return item(expression, data, items, useGroupStorage);
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM_CLASS)
            return itemClass(expression, data, items, useGroupStorage);
        if (expression.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED)
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    expression.getCheckAction());

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
            List<MethodInput> inputs = needsVerification
                    ? Collections.emptyList() : bestAlternativeInputs(results);
            return new ItemRequirementResult(state,
                    (needsVerification
                            ? get(1320) : "Get one of: ")
                            + expression.label(), inputs);
        }

        List<String> actions = new ArrayList<>();
        List<MethodInput> inputs = new ArrayList<>();
        var state = RequirementState.VERIFIED;
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
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        var names = expression.getItemNames().toArray(new String[0]);
        int owned;
        boolean observed;
        switch (expression.getScope())
        {
            case EQUIPPED:
                owned = items.equippedQuantity(names);
                observed = data != null && data.equipment() != null;
                break;
            case CARRIED:
                owned = quantityIn(data == null || data.inventory() == null
                        ? null : data.inventory().getItems(), names);
                observed = data != null && data.inventory() != null;
                break;
            case CARRIED_OR_EQUIPPED:
                owned = quantityIn(data == null || data.inventory() == null
                        ? null : data.inventory().getItems(), names)
                        + items.equippedQuantity(names);
                observed = data != null && data.inventory() != null
                        && data.equipment() != null;
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

        var shortfall = Math.max(0, expression.getQuantity() - owned);
        var target = String.join(" or ", expression.getItemNames());
        if (expression.getItemNames().size() > 1) target = "(" + target + ")";
        String action = (observed ? "Get " : get(1321))
                + shortfall + " × " + target;

        List<MethodInput> inputs = Collections.emptyList();
        if (observed && expression.getItemNames().size() == 1)
        {
            inputs = Collections.singletonList(new MethodInput(
                    expression.getItemNames().get(0), -1, shortfall));
        }
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                action, inputs);
    }

    private ItemRequirementResult itemClass(ItemRequirementExpression expression,
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        var itemClass = expression.getItemClass();
        if (itemClass == null)
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    get(1322));
        if (itemClass == ItemRequirementClass.EMPTY_INVENTORY_SPACE)
            return freeInventorySlots(expression, data);
        if (!itemClass.isNameObservable())
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    "Check and bring " + expression.label());

        int owned;
        boolean observed;
        switch (expression.getScope())
        {
            case EQUIPPED:
                owned = items.equippedQuantityMatching(itemClass,
                        expression.getExcludedItemNames());
                observed = data != null && data.equipment() != null;
                break;
            case CARRIED:
                owned = quantityMatching(data == null || data.inventory() == null
                        ? null : data.inventory().getItems(), itemClass,
                        expression.getExcludedItemNames());
                observed = data != null && data.inventory() != null;
                break;
            case CARRIED_OR_EQUIPPED:
                owned = quantityMatching(data == null || data.inventory() == null
                        ? null : data.inventory().getItems(), itemClass,
                        expression.getExcludedItemNames())
                        + items.equippedQuantityMatching(itemClass,
                                expression.getExcludedItemNames());
                observed = data != null && data.inventory() != null
                        && data.equipment() != null;
                break;
            case OWNED_OR_RETRIEVABLE:
                owned = items.quantityMatching(itemClass,
                        expression.getExcludedItemNames())
                        + items.restrictedQuantityMatching(itemClass,
                                expression.getExcludedItemNames());
                observed = ownershipObserved(data, items, useGroupStorage)
                        && !isUim(data);
                break;
            case IMMEDIATELY_USABLE:
            default:
                owned = items.quantityMatching(itemClass,
                        expression.getExcludedItemNames());
                observed = ownershipObserved(data, items, useGroupStorage);
                break;
        }
        if (owned >= expression.getQuantity())
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        var shortfall = Math.max(0, expression.getQuantity() - owned);
        var target = itemClass.getLabel();
        if (!expression.getExcludedItemNames().isEmpty())
            target += " (excluding " + String.join(" or ",
                    expression.getExcludedItemNames()) + ")";
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                (observed ? "Get " : get(1321)) + shortfall
                        + " × " + target);
    }

    private static ItemRequirementResult freeInventorySlots(
            ItemRequirementExpression expression, GameData data)
    {
        var inventory = data == null ? null : data.inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    get(332));
        var required = Math.max(1, expression.getQuantity());
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        if (free >= required)
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        return new ItemRequirementResult(RequirementState.BLOCKED,
                get(1323) + required
                        + get(1324) + free
                        + get(333));
    }

    private static List<MethodInput> bestAlternativeInputs(
            List<ItemRequirementResult> results)
    {
        List<MethodInput> best = Collections.emptyList();
        var bestCost = Long.MAX_VALUE;
        for (ItemRequirementResult result : results)
        {
            if (result.getMissingInputs().isEmpty()) continue;
            var cost = 0L;
            for (MethodInput input : result.getMissingInputs())
                cost += Math.max(1, input.getQuantity());
            if (cost < bestCost)
            {
                bestCost = cost;
                best = result.getMissingInputs();
            }
        }
        return best;
    }

    private static boolean ownershipObserved(GameData data,
            ItemIndex items, boolean useGroupStorage)
    {
        if (data == null || data.account() == null) return false;
        return items.usableOwnershipObserved();
    }

    private static boolean isUim(GameData data)
    {
        return data != null && data.account() != null
                && AccountMode.fromTypeCode(data.account().getAccountTypeCode())
                        == AccountMode.ULTIMATE_IRONMAN;
    }

    private static int quantityIn(Iterable<ItemState> stacks, String[] names)
    {
        if (stacks == null) return 0;
        var total = 0;
        for (ItemState stack : stacks)
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

    private static int quantityMatching(Iterable<ItemState> stacks,
            ItemRequirementClass itemClass, Iterable<String> excludedNames)
    {
        if (stacks == null || itemClass == null) return 0;
        var total = 0;
        for (ItemState stack : stacks)
        {
            if (stack == null || stack.getName() == null
                    || !itemClass.matches(stack.getName())) continue;
            var excluded = false;
            if (excludedNames != null)
                for (String value : excludedNames)
                    if (value != null && stack.getName().trim()
                            .equalsIgnoreCase(value.trim()))
                    {
                        excluded = true;
                        break;
                    }
            if (!excluded) total += Math.max(0, stack.getQuantity());
        }
        return total;
    }
}
