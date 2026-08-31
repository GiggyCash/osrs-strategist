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
        return evaluate(data, requirement, false);
    }

    public RequirementCheck evaluate(
            StrategyDataBundle data,
            ResourceRequirement requirement,
            boolean useGroupStorage)
    {
        return evaluate(data, requirement, CapabilityState.UNKNOWN, null,
                useGroupStorage);
    }

    public RequirementCheck evaluate(
            StrategyDataBundle data,
            ResourceRequirement requirement,
            CapabilityState alternateStorageState,
            String alternateEvidence)
    {
        return evaluate(data, requirement, alternateStorageState,
                alternateEvidence, false);
    }

    private RequirementCheck evaluate(
            StrategyDataBundle data,
            ResourceRequirement requirement,
            CapabilityState alternateStorageState,
            String alternateEvidence,
            boolean useGroupStorage)
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
        int observed = observedQuantity(data, useGroupStorage,
                requirement.getItemIds());
        if (observed >= requirement.getRequiredQuantity())
        {
            return new RequirementCheck(
                    requirement.getId(), requirement.getLabel(),
                    RequirementState.VERIFIED,
                    uim
                            ? "Observed quantity: " + observed
                                    + Text.get(703)
                            : "Observed quantity: " + observed
                                    + Text.get(704)
                                    + (usesObservedGroupStorage(data, useGroupStorage)
                                            ? ", and recent Group Storage state." : "."));
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
                        Text.get(705)
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
                                    + Text.get(706)
                                    + requirement.getRequiredQuantity() + "."
                            : "Equipment and inventory have " + observed
                                    + Text.get(707)
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
        return observedQuantity(data, false, itemIds);
    }

    public int observedQuantity(StrategyDataBundle data,
            boolean useGroupStorage, int... itemIds)
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
            if (!uim && usesObservedGroupStorage(data, useGroupStorage))
                total += data.getGroupStorage().getItems().stream()
                        .filter(item -> item.getItemId() == itemId)
                        .mapToInt(item -> Math.max(0, item.getQuantity())).sum();
            if (uim)
            {
                total += observedUimStorageQuantity(
                        data.getStorage(), itemId, false);
            }
        }
        return total;
    }

    private static boolean usesObservedGroupStorage(
            StrategyDataBundle data, boolean enabled)
    {
        if (!enabled || data == null || data.getAccount() == null
                || data.getGroupStorage() == null
                || !data.getGroupStorage().isObserved()) return false;
        return AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode())
                .isGroupIronman();
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
        return UimStorageMechanics.isRestrictedRetrieval(capability);
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
