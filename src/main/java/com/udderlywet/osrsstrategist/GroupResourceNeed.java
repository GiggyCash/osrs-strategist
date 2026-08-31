package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** An exact item-ID requirement that fresh Group Storage may satisfy. */
@Getter
public final class GroupResourceNeed
{
    private final String label;
    private final Set<Integer> acceptableItemIds;
    private final int quantity;
    private final boolean reusable;

    public GroupResourceNeed(String label, Set<Integer> acceptableItemIds,
            int quantity, boolean reusable)
    {
        if (acceptableItemIds == null || acceptableItemIds.isEmpty())
            throw new IllegalArgumentException(
                    PlayerText.get("GRN1"));
        this.label = label == null ? "Required item" : label;
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (Integer itemId : acceptableItemIds)
            if (itemId != null && itemId > 0) ids.add(itemId);
        if (ids.isEmpty())
            throw new IllegalArgumentException(
                    PlayerText.get("GRN2"));
        this.acceptableItemIds = Collections.unmodifiableSet(ids);
        this.quantity = Math.max(1, quantity);
        this.reusable = reusable;
    }

}
