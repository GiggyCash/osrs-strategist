package compass;

import java.util.*;
import javax.inject.Inject;

/** Exact usable sources, not matches against acquisition prose. */
final class MethodPreparationService
{
    private final AccountResourcePlanner resources;

    @Inject
    MethodPreparationService(AccountResourcePlanner resources) { this.resources = resources; }

    MethodPreparation evaluate(GameData data, List<MethodInput> needs, boolean group,
            boolean bankRoute, boolean purchaseAllowed)
    {
        return evaluate(data, needs, group, bankRoute, purchaseAllowed, Collections.emptySet());
    }

    MethodPreparation evaluate(GameData data, List<MethodInput> needs, boolean group,
            boolean bankRoute, boolean purchaseAllowed, Set<Integer> equippedReusable)
    {
        List<MethodPreparation.Step> steps = new ArrayList<>();
        List<MethodInput> missing = new ArrayList<>();
        AccountMode mode = data == null || data.account() == null ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().modeCode());
        // Merge before subtracting ownership, otherwise duplicate rows reuse the same stock.
        List<MethodInput> merged = merge(needs);
        if (merged == null || merged.isEmpty())
            return new MethodPreparation(MethodPreparation.State.UNRESOLVED, steps, 0);
        boolean preparation = false;
        for (MethodInput need : merged)
        {
            int carried = quantity(data == null ? null : data.inventory(), need.itemId);
            int remaining = Math.max(0, need.quantity - carried);
            if (carried > 0) add(steps, MethodPreparation.Kind.CARRIED, need,
                    Math.min(carried, need.quantity), "Use input already carried: ");
            int equipped = Math.min(remaining, quantity(data == null ? null : data.equipment(), need.itemId));
            if (equipped > 0)
            {
                boolean retained = equippedReusable.contains(need.itemId);
                add(steps, MethodPreparation.Kind.EQUIPPED, need, equipped,
                        retained ? "Use equipped reusable tool: " : "Unequip observed input: ");
                remaining -= equipped;
                preparation |= !retained;
            }
            if (remaining > 0 && bankRoute && mode != AccountMode.ULTIMATE_IRONMAN
                    && mode != AccountMode.UNKNOWN)
            {
                int bank = Math.min(remaining, quantity(data.bank(), need.itemId));
                if (bank > 0)
                {
                    add(steps, MethodPreparation.Kind.RETRIEVE, need, bank, "Withdraw observed bank stock: ");
                    remaining -= bank;
                    preparation = true;
                }
                if (group && mode.isGroupIronman() && data.groupStorage() != null
                        && data.groupStorage().isObserved())
                {
                    int shared = Math.min(remaining, quantity(data.groupStorage(), need.itemId));
                    if (shared > 0)
                    {
                        add(steps, MethodPreparation.Kind.RETRIEVE, need, shared,
                                "Withdraw freshly observed Group Storage stock, rechecking availability: ");
                        remaining -= shared;
                        preparation = true;
                    }
                }
            }
            if (remaining > 0) missing.add(new MethodInput(need.name, need.itemId, remaining));
        }
        if (missing.isEmpty()) return new MethodPreparation(preparation
                ? MethodPreparation.State.VERIFIED : MethodPreparation.State.READY, steps, 0);
        if (purchaseAllowed && mode.usesGrandExchange() && resources != null
                && observed(data.inventory()) && observed(data.equipment())
                && observed(data.bank()))
        {
            AccountResourcePlanner.Purchase purchase = resources.purchase(data, missing);
            for (MethodInput need : missing)
                steps.add(new MethodPreparation.Step(MethodPreparation.Kind.BUY, need, purchase.state,
                        (purchase.state == RequirementState.VERIFIED ? "Buy " : "Missing ")
                                + need.quantity + " " + need.name + ". " + purchase.reason));
            return new MethodPreparation(purchase.state == RequirementState.VERIFIED
                    ? MethodPreparation.State.VERIFIED : purchase.state == RequirementState.BLOCKED
                    ? MethodPreparation.State.BLOCKED : MethodPreparation.State.UNRESOLVED, steps, purchase.cost);
        }
        for (MethodInput need : missing)
            steps.add(new MethodPreparation.Step(MethodPreparation.Kind.UNSUPPORTED, need,
                    RequirementState.CHECK_NEEDED, "Acquisition unresolved for " + need.quantity + " " + need.name
                            + "; no verified source/retrieval plan."
                            + (mode == AccountMode.ULTIMATE_IRONMAN ? " UIM bank or restricted storage is not assumed." : "")));
        return new MethodPreparation(MethodPreparation.State.UNRESOLVED, steps, 0);
    }

    private static boolean observed(ItemsState items)
    {
        return items != null && items.isObserved();
    }

    private static List<MethodInput> merge(List<MethodInput> needs)
    {
        if (needs == null) return null;
        Map<Integer, MethodInput> merged = new LinkedHashMap<>();
        for (MethodInput need : needs)
        {
            if (need == null || need.itemId <= 0 || need.quantity <= 0
                    || need.name == null || need.name.trim().isEmpty()) return null;
            MethodInput previous = merged.get(need.itemId);
            long quantity = (long) need.quantity + (previous == null ? 0 : previous.quantity);
            if (quantity > Integer.MAX_VALUE || previous != null
                    && !previous.name.equalsIgnoreCase(need.name)) return null;
            merged.put(need.itemId, new MethodInput(need.name, need.itemId, (int) quantity));
        }
        return new ArrayList<>(merged.values());
    }

    private static int quantity(ItemsState items, int id)
    {
        return items == null || !items.isObserved() ? 0 : items.quantityOf(id);
    }

    private static void add(List<MethodPreparation.Step> steps, MethodPreparation.Kind kind,
            MethodInput need, int quantity, String reason)
    {
        steps.add(new MethodPreparation.Step(kind, new MethodInput(need.name, need.itemId, quantity),
                RequirementState.VERIFIED, reason + quantity + " " + need.name + "."));
    }
}
