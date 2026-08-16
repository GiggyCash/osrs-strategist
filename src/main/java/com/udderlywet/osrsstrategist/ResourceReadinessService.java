package com.udderlywet.osrsstrategist;

import java.util.List;
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
 *
 * <p>Two requirement forms are supported. {@link ResourceRequirement} is the
 * strongest form for exact RuneLite gameval item IDs. {@link NamedResourceRequirement}
 * handles safe item families such as level-gated logs or several equivalent
 * moulds without hardcoding unstable numeric IDs. Both forms follow identical
 * account-mode and UIM storage-safety rules.</p>
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
            return verifiedFromAlternateStorage(
                    requirement.getId(), requirement.getLabel(), alternateEvidence);
        }

        boolean uim = isUim(data);
        int observed = observedQuantity(data, requirement.getItemIds());
        if (observed >= requirement.getRequiredQuantity())
        {
            return verifiedObserved(
                    requirement.getId(), requirement.getLabel(), observed, uim);
        }

        if (uim)
        {
            int restricted = restrictedUimStorageQuantity(
                    data == null ? null : data.getStorage(),
                    requirement.getItemIds());
            return unresolvedUim(
                    data, requirement.getId(), requirement.getLabel(),
                    requirement.getRequiredQuantity(), observed, restricted);
        }

        return unresolvedBankAware(
                data, requirement.getId(), requirement.getLabel(),
                requirement.getRequiredQuantity(), observed);
    }

    /**
     * Evaluates a family-style item requirement from names already observed by
     * RuneLite. A rule may include a skill gate, so a high-tier item cannot make
     * a route look Ready before the account can actually use it.
     */
    public RequirementCheck evaluate(
            StrategyDataBundle data,
            NamedResourceRequirement requirement)
    {
        if (requirement == null)
        {
            return new RequirementCheck(
                    "resource:unknown", "Unknown resource",
                    RequirementState.CHECK_NEEDED,
                    "No typed resource requirement was provided.");
        }

        boolean uim = isUim(data);
        int observed = observedNamedQuantity(data, requirement, false);
        if (observed >= requirement.getRequiredQuantity())
        {
            return verifiedObserved(
                    requirement.getId(), requirement.getLabel(), observed, uim);
        }

        if (uim)
        {
            int restricted = observedNamedQuantity(data, requirement, true);
            return unresolvedUim(
                    data, requirement.getId(), requirement.getLabel(),
                    requirement.getRequiredQuantity(), observed, restricted);
        }

        return unresolvedBankAware(
                data, requirement.getId(), requirement.getLabel(),
                requirement.getRequiredQuantity(), observed);
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

    private int observedNamedQuantity(
            StrategyDataBundle data,
            NamedResourceRequirement requirement,
            boolean restrictedUimStorageOnly)
    {
        if (data == null || requirement == null) return 0;
        AccountSnapshot account = data.getAccount();

        // When requesting restricted-only storage we deliberately do not count
        // equipment/inventory again. The result is added to the directly usable
        // quantity only to decide whether the UIM route needs a risk/access check.
        if (restrictedUimStorageOnly)
        {
            return namedUimStorageQuantity(
                    data.getStorage(), requirement, account, true);
        }

        int total = 0;
        total += matchingQuantity(
                data.getEquipment() == null
                        ? null : data.getEquipment().getEquippedItems(),
                requirement, account);
        total += matchingQuantity(
                data.getInventory() == null
                        ? null : data.getInventory().getItems(),
                requirement, account);

        if (isUim(data))
        {
            total += namedUimStorageQuantity(
                    data.getStorage(), requirement, account, false);
        }
        else
        {
            total += matchingQuantity(
                    data.getBank() == null
                            ? null : data.getBank().getItems(),
                    requirement, account);
        }
        return total;
    }

    private static int matchingQuantity(
            List<ItemStackSnapshot> items,
            NamedResourceRequirement requirement,
            AccountSnapshot account)
    {
        if (items == null || requirement == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (requirement.matches(item, account))
                total += item.getQuantity();
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
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
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

    private static int namedUimStorageQuantity(
            StorageSnapshot storage,
            NamedResourceRequirement requirement,
            AccountSnapshot account,
            boolean restrictedOnly)
    {
        if (storage == null || requirement == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            boolean restricted = requiresAdditionalAccessCheck(capability);
            if (restrictedOnly != restricted) continue;
            total += matchingQuantity(entry.getValue(), requirement, account);
        }
        return total;
    }

    private static RequirementCheck verifiedFromAlternateStorage(
            String id,
            String label,
            String evidence)
    {
        return new RequirementCheck(
                id, label, RequirementState.VERIFIED,
                evidence == null
                        ? "Verified in account-specific storage."
                        : evidence);
    }

    private static RequirementCheck verifiedObserved(
            String id,
            String label,
            int observed,
            boolean uim)
    {
        return new RequirementCheck(
                id, label, RequirementState.VERIFIED,
                uim
                        ? "Observed quantity: " + observed
                                + " across equipment, inventory, and directly usable verified UIM storage."
                        : "Observed quantity: " + observed
                                + " across equipment, inventory, and known bank state.");
    }

    private static RequirementCheck unresolvedUim(
            StrategyDataBundle data,
            String id,
            String label,
            int required,
            int observed,
            int restricted)
    {
        if (observed + restricted >= required)
        {
            return new RequirementCheck(
                    id, label, RequirementState.CHECK_NEEDED,
                    "Enough is observed only after counting UIM storage with additional access/risk preconditions; verify that route before using the resource.");
        }

        StorageSnapshot storage = data == null ? null : data.getStorage();
        boolean storageContentsKnown = storage != null
                && !storage.getObservedContents().isEmpty();
        return new RequirementCheck(
                id, label, RequirementState.CHECK_NEEDED,
                storageContentsKnown
                        ? "Only " + observed
                                + " directly usable quantity observed across equipment, inventory, and verified UIM storage; need at least "
                                + required + "."
                        : "Equipment and inventory have " + observed
                                + "; relevant UIM storage contents have not been observed yet.");
    }

    private static RequirementCheck unresolvedBankAware(
            StrategyDataBundle data,
            String id,
            String label,
            int required,
            int observed)
    {
        boolean bankKnown = data != null && data.getBank() != null;
        if (!bankKnown)
        {
            return new RequirementCheck(
                    id, label, RequirementState.CHECK_NEEDED,
                    "Equipment and inventory have " + observed
                            + "; the bank has not been observed yet.");
        }

        return new RequirementCheck(
                id, label, RequirementState.CHECK_NEEDED,
                "Only " + observed + " observed; need at least "
                        + required + ".");
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
