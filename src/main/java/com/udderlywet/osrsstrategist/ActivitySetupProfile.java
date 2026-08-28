package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Typed setup properties for an activity; it contains no method identity rules. */
public final class ActivitySetupProfile
{
    private final Set<Integer> equippedItemIds;
    private final Set<Integer> inventoryItemIds;
    private final String regionId;
    private final String spellbookId;
    private final int setupMinutes;

    private ActivitySetupProfile(Builder builder)
    {
        equippedItemIds = immutable(builder.equippedItemIds);
        inventoryItemIds = immutable(builder.inventoryItemIds);
        regionId = clean(builder.regionId);
        spellbookId = clean(builder.spellbookId);
        setupMinutes = Math.max(0, builder.setupMinutes);
    }

    public static Builder builder() { return new Builder(); }
    public Set<Integer> getEquippedItemIds() { return equippedItemIds; }
    public Set<Integer> getInventoryItemIds() { return inventoryItemIds; }
    public String getRegionId() { return regionId; }
    public String getSpellbookId() { return spellbookId; }
    public int getSetupMinutes() { return setupMinutes; }

    private static Set<Integer> immutable(Set<Integer> values)
    {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static String clean(String value)
    {
        return value == null || value.trim().isEmpty()
                ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static final class Builder
    {
        private final Set<Integer> equippedItemIds = new LinkedHashSet<>();
        private final Set<Integer> inventoryItemIds = new LinkedHashSet<>();
        private String regionId;
        private String spellbookId;
        private int setupMinutes;

        public Builder requiresEquipped(int itemId)
        {
            if (itemId > 0) equippedItemIds.add(itemId);
            return this;
        }
        public Builder requiresInventory(int itemId)
        {
            if (itemId > 0) inventoryItemIds.add(itemId);
            return this;
        }
        public Builder region(String value) { regionId = value; return this; }
        public Builder spellbook(String value) { spellbookId = value; return this; }
        public Builder setupMinutes(int value) { setupMinutes = value; return this; }
        public ActivitySetupProfile build() { return new ActivitySetupProfile(this); }
    }
}
