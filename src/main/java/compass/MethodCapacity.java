package compass;

import java.util.*;

/** Simulates retained input/output slots; never assumes banking, dropping or alching. */
final class MethodCapacity
{
    final RequirementState hard, working, efficient;
    final int actions;
    final String reason;

    private MethodCapacity(RequirementState hard, RequirementState working, int actions, String reason)
    {
        this(hard, working, RequirementState.CHECK_NEEDED, actions, reason);
    }

    private MethodCapacity(RequirementState hard, RequirementState working,
            RequirementState efficient, int actions, String reason)
    {
        this.hard = hard;
        this.working = working;
        this.efficient = efficient;
        this.actions = actions;
        this.reason = reason;
    }

    static MethodCapacity evaluate(ItemsState inventory, MethodIntelligenceCatalog.Recipe recipe,
            int batch)
    {
        return evaluate(inventory, null, recipe, batch);
    }

    static MethodCapacity evaluate(ItemsState inventory, ItemsState equipment,
            MethodIntelligenceCatalog.Recipe recipe, int batch)
    {
        if (recipe == null || batch < 1 || batch > recipe.maximumBatch)
            return new MethodCapacity(RequirementState.BLOCKED, RequirementState.BLOCKED,
                    0, "Choose a positive batch within the reviewed recipe limit.");
        if (inventory == null || !inventory.isObserved() || !inventory.hasCompleteSlotObservation())
            return new MethodCapacity(RequirementState.CHECK_NEEDED, RequirementState.CHECK_NEEDED,
                    0, "Observe the complete inventory before planning a working batch.");
        List<MethodIntelligenceCatalog.Ingredient> required = new ArrayList<>(recipe.inputs);
        required.addAll(recipe.tools);
        for (MethodIntelligenceCatalog.Ingredient input : required)
            for (ItemState item : inventory.getItems())
                if (item != null && item.itemId == input.itemId && !input.stackable && item.quantity > 1)
                    return new MethodCapacity(RequirementState.CHECK_NEEDED, RequirementState.CHECK_NEEDED,
                            0, "Non-stackable inputs require an exact slot observation.");
        boolean one = fits(inventory, equipment, recipe, 1);
        boolean work = fits(inventory, equipment, recipe, batch);
        // This proves the catalog's batch capacity, never an XP rate or sustainable loop.
        RequirementState efficient = recipe.efficientBatch <= 0 ? RequirementState.CHECK_NEEDED
                : !fits(inventory, equipment, recipe, recipe.efficientBatch) ? RequirementState.BLOCKED
                : work && batch >= recipe.efficientBatch ? RequirementState.VERIFIED
                : RequirementState.CHECK_NEEDED;
        return new MethodCapacity(one ? RequirementState.VERIFIED : RequirementState.BLOCKED,
                work ? RequirementState.VERIFIED : RequirementState.BLOCKED, efficient,
                work ? batch : one ? 1 : 0,
                work ? "The inputs and retained outputs fit this working batch."
                        : "Inventory lacks working capacity; no disposal or storage move is assumed.");
    }

    private static boolean fits(ItemsState inventory, ItemsState equipment,
            MethodIntelligenceCatalog.Recipe recipe, int batch)
    {
        int occupied = UimSetupCostService.occupiedInventorySlots(inventory);
        if (occupied > 28) return false;
        Map<Integer, Long> quantities = new HashMap<>();
        for (ItemState item : inventory.getItems())
            if (item != null && item.quantity > 0)
                quantities.merge(item.itemId, (long) item.quantity, Long::sum);
        // Tools remain occupied throughout processing. Equipped use must be explicitly modeled.
        for (MethodIntelligenceCatalog.Ingredient tool : recipe.tools)
        {
            long equipped = tool.equippedAllowed && equipment != null && equipment.isObserved()
                    ? equipment.quantityOf(tool.itemId) : 0;
            long held = quantities.getOrDefault(tool.itemId, 0L);
            long missing = Math.max(0L, tool.quantity - held - equipped);
            occupied += tool.stackable ? held == 0 && missing > 0 ? 1 : 0 : (int) missing;
        }
        // Reserve missing inputs before executing anything. No implicit clear-inventory step.
        for (MethodIntelligenceCatalog.Ingredient input : recipe.inputs)
        {
            long held = quantities.getOrDefault(input.itemId, 0L);
            long needed = (long) batch * input.quantity;
            long missing = Math.max(0L, needed - held);
            occupied += input.stackable ? held == 0 && missing > 0 ? 1 : 0 : (int) missing;
            quantities.put(input.itemId, Math.max(held, needed));
        }
        if (occupied > 28) return false;
        boolean outputStackExists = quantities.getOrDefault(recipe.outputItemId, 0L) > 0;
        for (int action = 0; action < batch; action++)
        {
            for (MethodIntelligenceCatalog.Ingredient input : recipe.inputs)
            {
                long left = quantities.get(input.itemId) - input.quantity;
                occupied -= input.stackable ? left == 0 ? 1 : 0 : input.quantity;
                quantities.put(input.itemId, left);
            }
            if (recipe.outputStackable)
            {
                if (!outputStackExists && recipe.outputsPerAction > 0) occupied++;
                outputStackExists = recipe.outputsPerAction > 0 || outputStackExists;
            }
            else occupied += recipe.outputsPerAction;
            if (occupied > 28) return false;
        }
        return true;
    }
}
