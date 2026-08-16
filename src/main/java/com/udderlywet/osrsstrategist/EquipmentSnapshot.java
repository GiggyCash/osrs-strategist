package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EquipmentSnapshot
{
    private final List<ItemStackSnapshot> equippedItems;

    public EquipmentSnapshot(List<ItemStackSnapshot> equippedItems)
    {
        this.equippedItems = Collections.unmodifiableList(
                new ArrayList<>(equippedItems)
        );
    }

    public List<ItemStackSnapshot> getEquippedItems()
    {
        return equippedItems;
    }
}
