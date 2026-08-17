package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only item ownership queries over the evidence Strategist has actually
 * observed for the current character.
 *
 * <p>Unknown storage is never treated as empty. Callers can choose whether GIM
 * group storage is usable, and UIM callers can explicitly exclude unsafe death
 * and looting-bag storage from immediately usable supply checks.</p>
 */
public final class ObservedItemIndex
{
    private final StrategyDataBundle data;
    private final boolean useGroupStorage;

    public ObservedItemIndex(StrategyDataBundle data, boolean useGroupStorage)
    {
        this.data = data;
        this.useGroupStorage = useGroupStorage;
    }

    public boolean has(String... names)
    {
        return quantity(names) > 0;
    }

    public int quantity(String... names)
    {
        if (data == null || names == null || names.length == 0) return 0;
        int total = 0;
        total += quantityIn(data.getInventory() == null
                ? null : data.getInventory().getItems(), names);
        total += quantityIn(data.getEquipment() == null
                ? null : data.getEquipment().getEquippedItems(), names);

        AccountMode mode = data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());

        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            total += quantityIn(data.getBank() == null
                    ? null : data.getBank().getItems(), names);
        }

        if (useGroupStorage && mode.isGroupIronman()
                && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved())
        {
            total += quantityIn(data.getGroupStorage().getItems(), names);
        }

        if (data.getStorage() != null)
        {
            for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                    : data.getStorage().getObservedContents().entrySet())
            {
                StorageCapability capability = entry.getKey();
                if (!data.getStorage().verified(capability)) continue;

                if (mode == AccountMode.ULTIMATE_IRONMAN
                        && (capability == StorageCapability.LOOTING_BAG
                        || capability == StorageCapability.DEATH_STORAGE
                        || capability == StorageCapability.DEATHPILE))
                {
                    // Those items may exist, but are not safely/directly usable
                    // for a normal recommendation without a retrieval plan.
                    continue;
                }
                total += quantityIn(entry.getValue(), names);
            }
        }
        return total;
    }

    public boolean bankObserved()
    {
        if (data == null || data.getAccount() == null) return false;
        AccountMode mode = AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
        return mode == AccountMode.ULTIMATE_IRONMAN || data.getBank() != null;
    }

    public boolean groupStorageObserved()
    {
        return data != null && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved();
    }

    private static int quantityIn(
            Iterable<ItemStackSnapshot> items,
            String... names)
    {
        if (items == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item == null || item.getName() == null) continue;
            String actual = normalize(item.getName());
            for (String name : names)
            {
                if (name != null && actual.equals(normalize(name)))
                {
                    total += Math.max(0, item.getQuantity());
                    break;
                }
            }
        }
        return total;
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
