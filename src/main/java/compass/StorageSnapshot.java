package compass;

import java.util.*;

import lombok.Getter;

/**
 * Verified/unknown state and observed contents for storage systems.
 *
 * <p>UNKNOWN remains different from unavailable. Contents are only stored when
 * actually observed. This is especially important for UIM, where a generic
 * Text.get(1947) assumption can create unsafe or impossible advice.</p>
 */
public final class StorageSnapshot
{
    @Getter
    private final Map<StorageCapability, CapabilityState> states;
    private final Map<StorageCapability, List<ItemState>> contents;

    public StorageSnapshot(Map<StorageCapability, CapabilityState> states)
    {
        this(states, Collections.emptyMap());
    }

    public StorageSnapshot(
            Map<StorageCapability, CapabilityState> states,
            Map<StorageCapability, List<ItemState>> contents)
    {
        EnumMap<StorageCapability, CapabilityState> stateCopy =
                new EnumMap<>(StorageCapability.class);
        if (states != null) stateCopy.putAll(states);
        this.states = Collections.unmodifiableMap(stateCopy);

        EnumMap<StorageCapability, List<ItemState>> contentCopy =
                new EnumMap<>(StorageCapability.class);
        if (contents != null)
        {
            for (Map.Entry<StorageCapability, List<ItemState>> entry
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

    public List<ItemState> contentsOf(StorageCapability capability)
    {
        return contents.getOrDefault(capability, Collections.emptyList());
    }

    /**
     * Convenience view for planners that must reason about dangerous-death UIM
     * state. This returns observed contents only. An empty list does not imply
     * the capability is unavailable; callers should use {@link #verified} or
     * {@link #hasObservedContents} when that distinction matters.
     */
    public List<ItemState> getDeathStorageItems()
    {
        List<ItemState> observed = new ArrayList<>();
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
        var total = 0;
        for (ItemState item : contentsOf(capability))
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }


    public Map<StorageCapability, List<ItemState>> getObservedContents()
    {
        return contents;
    }
}
