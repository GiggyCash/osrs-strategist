package com.udderlywet.osrsstrategist;

import java.util.Map;
import javax.inject.Singleton;

/**
 * Generic resource readiness evaluator.
 *
 * <p>Main/Iron/GIM can use equipment, inventory, and an actually observed bank
 * snapshot. UIM never waits on or counts a bank; it uses equipped items,
 * inventory, plus contents directly observed in verified account-specific
 * storage. Storage that has additional retrieval or death risk proves existence
 * but does not become method-ready automatically.</p>
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
                                    + " across equipment, inventory, and directly usable verified UIM storage."
                            : "Observed quantity: " + observed
                                    + " across equipment, inventory, and known bank state.");
        }

        if (uim)
        {
            int restricted = restrictedUimStorageQuantity(
                    data == null ? null : data.getStorage(),
                    requirement.getItemIds());
            if (observed + restricted >= requirement.getRequiredQuantity())
            {
                return new RequirementCheck(
                        requirement.getId(), requirement.getLabel(),
                        RequirementState.CHECK_NEEDED,
                        "Enough is observed only after counting UIM storage with additional access/risk preconditions; verify that route before using the resource."
                );
            }

            StorageSnapshot storage = data == null ? null : data.getStorage();
            boolean storageContentsKnown = storage != null
                    && !storage.getObservedContents().isEmpty();
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.CHECK_NEEDED,
                    storageContentsKnown
                            ? "Only " + observed
                                    + " directly usable quantity observed across equipment, inventory, and verified UIM storage; need at least "
                                    + requirement.getRequiredQuantity() + "."
                            : "Equipment and inventory have " + observed
                                    + "; relevant UIM storage contents have not been observed yet."
            );
        }

        boolean bankKnown = data != null && data.getBank() != null;
        if (!bankKnown)
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.CHECK_NEEDED,
                    "Equipment and inventory have " + observed
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
        EquipmentSnapshot equipment = data.getEquipment();
        InventorySnapshot inventory = data.getInventory();
        BankSnapshot bank = data.getBank();
        boolean uim = isUim(data);

        for (int itemId : itemIds)
        {
            if (equipment != null) total += equipment.quantityOf(itemId);
            if (inventory != null) total += inventory.quantityOf(itemId);
            if (!uim && bank != null) total += bank.quantityOf(itemId);
            if (uim)
            {
                total += observedUimStorageQuantity(
                        data.getStorage(), itemId, false);
            }
        }
        return total;
    }

    private static int restrictedUimStorageQuantity(
            StorageSnapshot storage,
            int... itemIds)
    {
        if (storage == null || itemIds == null) return 0;
        int total = 0;
        for (int itemId : itemIds)
        {
            total += observedUimStorageQuantity(storage, itemId, true);
        }
        return total;
    }

    private static int observedUimStorageQuantity(
            StorageSnapshot storage,
            int itemId,
            boolean restrictedOnly)
    {
        if (storage == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, java.util.List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            boolean restricted = requiresAdditionalAccessCheck(capability);
            if (restrictedOnly != restricted) continue;
            for (ItemStackSnapshot item : entry.getValue())
            {
                if (item.getItemId() == itemId) total += item.getQuantity();
            }
        }
        return total;
    }

    private static boolean requiresAdditionalAccessCheck(
            StorageCapability capability)
    {
        return capability == StorageCapability.LOOTING_BAG
                || capability == StorageCapability.DEATH_STORAGE
                || capability == StorageCapability.DEATHPILE;
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
