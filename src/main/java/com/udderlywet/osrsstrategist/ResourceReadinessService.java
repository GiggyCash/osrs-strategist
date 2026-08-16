package com.udderlywet.osrsstrategist;

import java.util.Map;
import javax.inject.Singleton;

/**
 * Generic resource readiness evaluator.
 *
 * <p>Main/Iron/GIM can use an actually observed bank snapshot. UIM never waits
 * on or counts a bank; it uses inventory plus contents directly observed in
 * verified account-specific storage.</p>
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

        boolean uim = isUim(data);
        int observed = observedQuantity(data, requirement.getItemIds());
        if (observed >= requirement.getRequiredQuantity())
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.VERIFIED,
                    uim
                            ? "Observed quantity: " + observed
                                    + " across inventory and verified UIM storage."
                            : "Observed quantity: " + observed
                                    + " across inventory and known bank state.");
        }

        if (uim)
        {
            StorageSnapshot storage = data == null ? null : data.getStorage();
            boolean storageContentsKnown = storage != null
                    && !storage.getObservedContents().isEmpty();
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.CHECK_NEEDED,
                    storageContentsKnown
                            ? "Only " + observed
                                    + " observed across inventory and verified UIM storage; need at least "
                                    + requirement.getRequiredQuantity() + "."
                            : "Inventory has " + observed
                                    + "; relevant UIM storage contents have not been observed yet."
            );
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
        boolean uim = isUim(data);

        for (int itemId : itemIds)
        {
            if (inventory != null) total += inventory.quantityOf(itemId);
            if (!uim && bank != null) total += bank.quantityOf(itemId);
            if (uim) total += observedUimStorageQuantity(data.getStorage(), itemId);
        }
        return total;
    }

    private static int observedUimStorageQuantity(
            StorageSnapshot storage,
            int itemId)
    {
        if (storage == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, java.util.List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            for (ItemStackSnapshot item : entry.getValue())
            {
                if (item.getItemId() == itemId) total += item.getQuantity();
            }
        }
        return total;
    }

    private static boolean isUim(StrategyDataBundle data)
    {
        return data != null
                && data.getAccount() != null
                && AccountMode.fromTypeCode(
                        data.getAccount().getAccountTypeCode()
                ) == AccountMode.ULTIMATE_IRONMAN;
    }
}
