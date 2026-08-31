package com.udderlywet.osrsstrategist;

import java.util.*;
import java.util.function.Predicate;
import lombok.Getter;

/**
 * Observed contents of an inventory-like container.  The optional timestamp
 * distinguishes cached bank/group evidence without multiplying container DTOs.
 */
@Getter
public final class ItemsState
{
    static final long FRESH_FOR_MILLIS = 5L * 60L * 1000L;
    private final List<ItemState> items;
    private final long capturedAtMillis;
    private final boolean observed;
    private final boolean completeSlotObservation;
    private final boolean expires;

    /** Live inventory or equipment. */
    public ItemsState(List<ItemState> items)
    {
        this(items, true, 0L, hasSlots(items), false);
    }

    /** Cached bank observation. */
    public ItemsState(List<ItemState> items, long capturedAtMillis)
    {
        this(items, true, capturedAtMillis, false, false);
    }

    /** Group Storage observation using the current time. */
    public ItemsState(boolean observed, List<ItemState> items)
    {
        this(items, observed, observed ? System.currentTimeMillis() : 0L,
                false, true);
    }

    /** Group Storage observation with a supplied evidence time. */
    public ItemsState(boolean observed, List<ItemState> items, long observedAtMillis)
    {
        this(items, observed, observedAtMillis, false, true);
    }

    /** Live inventory with an explicit complete-slot evidence flag. */
    public ItemsState(List<ItemState> items, boolean completeSlotObservation)
    {
        this(items, true, 0L, completeSlotObservation, false);
    }

    private ItemsState(List<ItemState> values, boolean observed, long time,
            boolean completeSlots, boolean expires)
    {
        this.items = Collections.unmodifiableList(new ArrayList<>(
                values == null ? Collections.emptyList() : values));
        this.observed = observed;
        this.capturedAtMillis = Math.max(0L, time);
        this.completeSlotObservation = completeSlots;
        this.expires = expires;
    }

    public static ItemsState unknown()
    {
        return new ItemsState(false, Collections.emptyList(), 0L);
    }

    public List<ItemState> getEquippedItems() { return items; }
    public long getObservedAtMillis() { return capturedAtMillis; }
    public boolean hasCompleteSlotObservation() { return completeSlotObservation; }

    public boolean isObserved()
    {
        if (!observed) return false;
        if (!expires) return true;
        long age = System.currentTimeMillis() - capturedAtMillis;
        return capturedAtMillis > 0L && age >= 0L && age <= FRESH_FOR_MILLIS;
    }

    public int quantityOf(int itemId)
    {
        int total = 0;
        for (ItemState item : items)
            if (item != null && item.getItemId() == itemId)
                total += item.getQuantity();
        return total;
    }

    public int quantityOf(int... itemIds)
    {
        int total = 0;
        if (itemIds == null) return total;
        for (ItemState item : items)
            if (item != null) for (int id : itemIds)
                if (item.getItemId() == id) total += item.getQuantity();
        return total;
    }

    public int quantityNamed(String... names)
    {
        var expected = new HashSet<>();
        if (names != null) for (String name : names)
            if (name != null) expected.add(name.toLowerCase(Locale.ROOT));
        return quantityWhere(expected::contains);
    }

    public int quantityWhere(Predicate<String> nameTest)
    {
        int total = 0;
        if (nameTest == null) return total;
        for (ItemState item : items)
            if (item != null && item.getName() != null
                    && nameTest.test(item.getName().toLowerCase(Locale.ROOT)))
                total += Math.max(0, item.getQuantity());
        return total;
    }

    public boolean containsItem(int itemId)
    {
        return isObserved() && quantityOf(itemId) > 0;
    }

    private static boolean hasSlots(List<ItemState> items)
    {
        if (items != null)
            for (ItemState item : items)
                if (item != null && item.getSlotIndex() >= 0) return true;
        return false;
    }
}
