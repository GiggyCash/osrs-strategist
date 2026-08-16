package com.udderlywet.osrsstrategist;

import java.util.Map;
import javax.inject.Singleton;

/** Account-mode-aware sourcing planner for a required item. */
@Singleton
public class ResourceAcquisitionPlanner
{
    public ResourceAcquisitionPlan plan(
            StrategyContext context,
            ResourceNeed need)
    {
        if (context == null || need == null || context.getData() == null)
        {
            return checkNeeded(need, "Account state is not available.");
        }

        StrategyDataBundle data = context.getData();
        AccountMode mode = context.getAccountMode();
        int inventoryQuantity = quantityIn(data.getInventory(), need.getItemId());

        if (inventoryQuantity >= need.getQuantity())
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.INVENTORY, inventoryQuantity,
                    RecommendationConfidence.VERIFIED,
                    "Required quantity is confirmed in inventory."
            );
        }

        // UIM is its own resource universe. Normal bank state is ignored even if
        // a stale/test snapshot exists. Observed storage contents can count only
        // when the matching capability itself is verified.
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            StoredResource stored = findVerifiedStoredResource(
                    data.getStorage(), need.getItemId(), need.getQuantity());
            if (stored != null)
            {
                boolean risky = stored.capability == StorageCapability.DEATH_STORAGE
                        || stored.capability == StorageCapability.DEATHPILE;
                return new ResourceAcquisitionPlan(
                        need,
                        AcquisitionSource.VERIFIED_STORAGE,
                        stored.quantity,
                        risky
                                ? RecommendationConfidence.CHECK_NEEDED
                                : RecommendationConfidence.VERIFIED,
                        risky
                                ? "Required quantity is observed in "
                                        + pretty(stored.capability)
                                        + ", but retrieval needs an explicit UIM risk/precondition check."
                                : "Required quantity is confirmed in observed "
                                        + pretty(stored.capability) + "."
                );
            }
        }
        else
        {
            int bankQuantity = quantityIn(data.getBank(), need.getItemId());
            if (bankQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need, AcquisitionSource.BANK, bankQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in the latest observed bank snapshot."
                );
            }
        }

        if (AccountModePolicy.mayUseGroupStorage(
                mode, context.isUseGroupStorage()))
        {
            GroupStorageSnapshot groupStorage = data.getGroupStorage();
            int groupQuantity = quantityIn(groupStorage, need.getItemId());
            if (groupStorage != null
                    && groupStorage.isObserved()
                    && groupQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need, AcquisitionSource.GROUP_STORAGE, groupQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in observed Group Storage."
                );
            }
        }

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.GRAND_EXCHANGE, inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    "GE is an option, but Strategist must verify price, available GP, and opportunity cost before recommending a purchase."
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.SELF_SOURCE, inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    mode == AccountMode.ULTIMATE_IRONMAN
                            ? "No sufficient verified UIM inventory/storage source is known; use a verified self-source route that also fits current inventory pressure."
                            : "This account must use a verified gathering, shop, crafting, minigame, or drop source."
            );
        }

        return checkNeeded(need, "A verified acquisition route is not available yet.");
    }

    private static StoredResource findVerifiedStoredResource(
            StorageSnapshot storage,
            int itemId,
            int needed)
    {
        if (storage == null) return null;
        for (Map.Entry<StorageCapability, java.util.List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            int quantity = 0;
            for (ItemStackSnapshot item : entry.getValue())
            {
                if (item.getItemId() == itemId) quantity += item.getQuantity();
            }
            if (quantity >= needed)
            {
                return new StoredResource(capability, quantity);
            }
        }
        return null;
    }

    private static ResourceAcquisitionPlan checkNeeded(
            ResourceNeed need,
            String note)
    {
        return new ResourceAcquisitionPlan(
                need, AcquisitionSource.CHECK_NEEDED, 0,
                RecommendationConfidence.CHECK_NEEDED, note
        );
    }

    private static int quantityIn(InventorySnapshot inventory, int itemId)
    {
        return inventory == null ? 0 : quantityInItems(inventory.getItems(), itemId);
    }

    private static int quantityIn(BankSnapshot bank, int itemId)
    {
        return bank == null ? 0 : quantityInItems(bank.getItems(), itemId);
    }

    private static int quantityIn(GroupStorageSnapshot storage, int itemId)
    {
        if (storage == null || !storage.isObserved()) return 0;
        return quantityInItems(storage.getItems(), itemId);
    }

    private static int quantityInItems(Iterable<ItemStackSnapshot> items, int itemId)
    {
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }

    private static String pretty(StorageCapability capability)
    {
        return capability.name().toLowerCase().replace('_', ' ');
    }

    private static final class StoredResource
    {
        private final StorageCapability capability;
        private final int quantity;

        private StoredResource(StorageCapability capability, int quantity)
        {
            this.capability = capability;
            this.quantity = quantity;
        }
    }
}
