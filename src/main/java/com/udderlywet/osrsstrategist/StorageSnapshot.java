package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Verified/unknown state and observed contents for storage systems.
 *
 * <p>UNKNOWN remains different from unavailable. Contents are only stored when
 * actually observed. This is especially important for UIM, where a generic
 * "you can store this" assumption can create unsafe or impossible advice.</p>
 */
public final class StorageSnapshot
{
    private final Map<StorageCapability, CapabilityState> states;
    private final Map<StorageCapability, List<ItemStackSnapshot>> contents;

    public StorageSnapshot(Map<StorageCapability, CapabilityState> states)
    {
        this(states, Collections.emptyMap());
    }

    public StorageSnapshot(
            Map<StorageCapability, CapabilityState> states,
            Map<StorageCapability, List<ItemStackSnapshot>> contents)
    {
        EnumMap<StorageCapability, CapabilityState> stateCopy =
                new EnumMap<>(StorageCapability.class);
        if (states != null) stateCopy.putAll(states);
        this.states = Collections.unmodifiableMap(stateCopy);

        EnumMap<StorageCapability, List<ItemStackSnapshot>> contentCopy =
                new EnumMap<>(StorageCapability.class);
        if (contents != null)
        {
            for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                    : contents.entrySet())
            {
                contentCopy.put(entry.getKey(), Collections.unmodifiableList(
                        entry.getValue() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(entry.getValue())
                ));
            }
        }
        this.contents = Collections.unmodifiableMap(contentCopy);
    }

    public static StorageSnapshot unknown()
    {
        return new StorageSnapshot(Collections.emptyMap());
    }

    public CapabilityState stateOf(StorageCapability capability)
    {
        return states.getOrDefault(capability, CapabilityState.UNKNOWN);
    }

    public boolean verified(StorageCapability capability)
    {
        return stateOf(capability) == CapabilityState.VERIFIED;
    }

    public boolean hasObservedContents(StorageCapability capability)
    {
        return capability != null && contents.containsKey(capability);
    }

    public List<ItemStackSnapshot> contentsOf(StorageCapability capability)
    {
        return contents.getOrDefault(capability, Collections.emptyList());
    }

    /**
     * Convenience view for planners that must reason about dangerous-death UIM
     * state. This returns observed contents only. An empty list does not imply
     * the capability is unavailable; callers should use {@link #verified} or
     * {@link #hasObservedContents} when that distinction matters.
     */
    public List<ItemStackSnapshot> getDeathStorageItems()
    {
        List<ItemStackSnapshot> observed = new ArrayList<>();
        observed.addAll(contentsOf(StorageCapability.DEATH_STORAGE));
        observed.addAll(contentsOf(StorageCapability.HESPORI_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(StorageCapability.ZULRAH_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(
                StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL));
        return Collections.unmodifiableList(observed);
    }

    public int quantityOf(StorageCapability capability, int itemId)
    {
        if (!verified(capability) || !hasObservedContents(capability)) return 0;
        int total = 0;
        for (ItemStackSnapshot item : contentsOf(capability))
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }

    public Map<StorageCapability, CapabilityState> getStates()
    {
        return states;
    }

    public Map<StorageCapability, List<ItemStackSnapshot>> getObservedContents()
    {
        return contents;
    }
}
