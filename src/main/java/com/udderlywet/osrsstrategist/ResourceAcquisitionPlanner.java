package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Account-mode-aware first pass at sourcing a required item.
 *
 * <p>The planner checks already-owned resources before proposing a new source.
 * It deliberately stops at broad sourcing families (GE candidate/self-source)
 * until price, drop, skilling, shop, and money-making data are verified.</p>
 */
@Singleton
public class ResourceAcquisitionPlanner
{
    public ResourceAcquisitionPlan plan(
            StrategyContext context,
            ResourceNeed need)
    {
        if (context == null || need == null || context.getData() == null)
        {
            return checkNeeded(
                    need,
                    "Account state is not available."
            );
        }

        StrategyDataBundle data = context.getData();
        AccountMode mode = context.getAccountMode();

        int inventoryQuantity = quantityIn(
                data.getInventory(),
                need.getItemId()
        );

        if (inventoryQuantity >= need.getQuantity())
        {
            return new ResourceAcquisitionPlan(
                    need,
                    AcquisitionSource.INVENTORY,
                    inventoryQuantity,
                    RecommendationConfidence.VERIFIED,
                    "Required quantity is confirmed in inventory."
            );
        }

        // UIM does not receive normal bank-routing advice. Other account modes
        // may use a bank snapshot only when it was actually observed.
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            int bankQuantity = quantityIn(
                    data.getBank(),
                    need.getItemId()
            );

            if (bankQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need,
                        AcquisitionSource.BANK,
                        bankQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in the latest observed bank snapshot."
                );
            }
        }

        if (AccountModePolicy.mayUseGroupStorage(
                mode,
                context.isUseGroupStorage()))
        {
            GroupStorageSnapshot groupStorage = data.getGroupStorage();
            int groupQuantity = quantityIn(
                    groupStorage,
                    need.getItemId()
            );

            if (groupStorage != null
                    && groupStorage.isObserved()
                    && groupQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need,
                        AcquisitionSource.GROUP_STORAGE,
                        groupQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in observed Group Storage."
                );
            }
        }

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new ResourceAcquisitionPlan(
                    need,
                    AcquisitionSource.GRAND_EXCHANGE,
                    inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    "GE is an option, but Strategist must verify price, available GP, and opportunity cost before recommending a purchase."
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new ResourceAcquisitionPlan(
                    need,
                    AcquisitionSource.SELF_SOURCE,
                    inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    "This account must use a verified gathering, shop, crafting, minigame, or drop source."
            );
        }

        return checkNeeded(
                need,
                "A verified acquisition route is not available yet."
        );
    }

    private static ResourceAcquisitionPlan checkNeeded(
            ResourceNeed need,
            String note)
    {
        return new ResourceAcquisitionPlan(
                need,
                AcquisitionSource.CHECK_NEEDED,
                0,
                RecommendationConfidence.CHECK_NEEDED,
                note
        );
    }

    private static int quantityIn(
            InventorySnapshot inventory,
            int itemId)
    {
        if (inventory == null)
        {
            return 0;
        }
        return quantityInItems(inventory.getItems(), itemId);
    }

    private static int quantityIn(
            BankSnapshot bank,
            int itemId)
    {
        if (bank == null)
        {
            return 0;
        }
        return quantityInItems(bank.getItems(), itemId);
    }

    private static int quantityIn(
            GroupStorageSnapshot storage,
            int itemId)
    {
        if (storage == null || !storage.isObserved())
        {
            return 0;
        }
        return quantityInItems(storage.getItems(), itemId);
    }

    private static int quantityInItems(
            Iterable<ItemStackSnapshot> items,
            int itemId)
    {
        int total = 0;

        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId)
            {
                total += item.getQuantity();
            }
        }

        return total;
    }
}
