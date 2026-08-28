package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Singleton;

/**
 * Scores the practical cost of changing activities on Ultimate Ironman.
 *
 * <p>UIM efficiency is not simply XP/hour. A route can be theoretically fast
 * while being strategically poor because it requires dismantling a valuable
 * inventory, retrieving death storage, emptying a looting bag, or exposing a
 * retrieval service to a dangerous death. This service only uses observed state
 * and method metadata. Unknown storage is never treated as empty.</p>
 */
@Singleton
public class UimSetupCostService
{
    public double score(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null
                || context.getAccountMode() != AccountMode.ULTIMATE_IRONMAN)
        {
            return 0.0;
        }

        StrategyDataBundle data = context.getData();
        if (data == null) return 0.0;

        double value = 0.0;
        TrainingPlan plan = recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        int setupMinutes = method == null ? 0 : Math.max(0, method.getSetupMinutes());
        int occupied = occupiedInventorySlots(data.getInventory());

        // Unknown/non-skill setup does not receive a fake "low setup" bonus.
        if (method != null)
        {
            if (setupMinutes <= 3) value += 5.0;
            else if (setupMinutes >= 12) value -= 11.0;
            else if (setupMinutes >= 7) value -= 5.0;
        }

        if (occupied >= 24 && setupMinutes >= 7) value -= 8.0;
        else if (occupied >= 20 && setupMinutes >= 7) value -= 4.0;

        StorageSnapshot storage = data.getStorage();
        boolean deathStorageObserved = hasObservedItems(
                storage, StorageCapability.DEATH_STORAGE);
        boolean deathpileObserved = hasObservedItems(
                storage, StorageCapability.DEATHPILE);
        boolean lootingBagObserved = hasObservedItems(
                storage, StorageCapability.LOOTING_BAG);

        RecommendationStrategicValue strategic =
                recommendation.getStrategicValue();
        boolean dangerous = method != null && method.isWilderness()
                || strategic.getRiskBurden() >= 0.5;

        // Active death storage is not a small inconvenience. A dangerous death
        // can delete or otherwise invalidate a carefully prepared UIM state, so
        // a merely attractive gear goal must not overwhelm this protection with
        // raw provider score.
        if (dangerous && deathStorageObserved) value -= 50.0;
        if (dangerous && deathpileObserved) value -= 22.0;

        if ((deathStorageObserved || deathpileObserved || lootingBagObserved)
                && strategic.getOpportunityCost() >= 0.5
                && strategic.getSetupReuse() < 0.5)
        {
            value -= 10.0;
        }
        value += strategic.getSetupReuse() * 7.0;
        return value;
    }

    static int occupiedInventorySlots(InventorySnapshot inventory)
    {
        if (inventory == null || inventory.getItems() == null) return 0;
        int slots = 0;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            if (item != null && item.getQuantity() > 0) slots++;
        }
        return slots;
    }

    static boolean hasObservedItems(
            StorageSnapshot storage,
            StorageCapability capability)
    {
        if (storage == null || capability == null
                || !storage.hasObservedContents(capability))
        {
            return false;
        }
        List<ItemStackSnapshot> items = storage.contentsOf(capability);
        if (items == null) return false;
        for (ItemStackSnapshot item : items)
        {
            if (item != null && item.getQuantity() > 0) return true;
        }
        return false;
    }
}
