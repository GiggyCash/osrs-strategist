package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Generic inventory/bank readiness evaluator used by every future method that
 * needs concrete resources. A missing bank snapshot stays unknown, not absent.
 */
@Singleton
public class ResourceReadinessService
{
    public RequirementCheck evaluate(
            StrategyDataBundle data,
            ResourceRequirement requirement)
    {
        return evaluate(data, requirement, CapabilityState.UNKNOWN, null);
    }

    public RequirementCheck evaluate(
            StrategyDataBundle data,
            ResourceRequirement requirement,
            CapabilityState alternateStorageState,
            String alternateEvidence)
    {
        if (alternateStorageState == CapabilityState.VERIFIED)
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.VERIFIED,
                    alternateEvidence == null
                            ? "Verified in account-specific storage."
                            : alternateEvidence);
        }

        int observed = observedQuantity(data, requirement.getItemIds());
        if (observed >= requirement.getRequiredQuantity())
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.VERIFIED,
                    "Observed quantity: " + observed + " across inventory and known bank state.");
        }

        boolean bankKnown = data != null && data.getBank() != null;
        if (!bankKnown)
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.CHECK_NEEDED,
                    "Inventory has " + observed
                            + "; the bank has not been observed yet.");
        }

        return new RequirementCheck(
                requirement.getId(), requirement.getLabel(),
                RequirementState.CHECK_NEEDED,
                "Only " + observed + " observed; need at least "
                        + requirement.getRequiredQuantity() + ".");
    }

    public int observedQuantity(StrategyDataBundle data, int... itemIds)
    {
        if (data == null || itemIds == null) return 0;
        int total = 0;
        InventorySnapshot inventory = data.getInventory();
        BankSnapshot bank = data.getBank();
        for (int itemId : itemIds)
        {
            if (inventory != null) total += inventory.quantityOf(itemId);
            if (bank != null) total += bank.quantityOf(itemId);
        }
        return total;
    }
}
