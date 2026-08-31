package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.RequiredArgsConstructor;

/**
 * Read-only item ownership queries over the evidence Compass has actually
 * observed for the current character.
 *
 * <p>Unknown storage is never treated as empty. Callers can choose whether GIM
 * group storage is usable, and UIM callers can explicitly exclude unsafe death
 * and looting-bag storage from immediately usable supply checks.</p>
 */
@RequiredArgsConstructor
public final class ObservedItemIndex
{
    private final StrategyDataBundle data;
    private final boolean useGroupStorage;


    public boolean has(String... names)
    {
        return quantity(names) > 0;
    }

    /**
     * Returns only items that can be treated as directly usable by the current
     * recommendation. For UIM this intentionally excludes the normal bank,
     * looting bag, death storage, and deathpiles.
     */
    public int quantity(String... names)
    {
        if (data == null || names == null || names.length == 0) return 0;
        int total = 0;
        total = safeAdd(total, quantityIn(data.getInventory() == null
                ? null : data.getInventory().getItems(), names));
        total = safeAdd(total, quantityIn(data.getEquipment() == null
                ? null : data.getEquipment().getEquippedItems(), names));

        AccountMode mode = accountMode();
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            total = safeAdd(total, quantityIn(data.getBank() == null
                    ? null : data.getBank().getItems(), names));
        }

        if (useGroupStorage && mode.isGroupIronman()
                && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved())
        {
            total = safeAdd(total,
                    quantityIn(data.getGroupStorage().getItems(), names));
        }

        if (data.getStorage() != null)
        {
            for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                    : data.getStorage().getObservedContents().entrySet())
            {
                StorageCapability capability = entry.getKey();
                if (!data.getStorage().verified(capability)) continue;
                if (mode == AccountMode.ULTIMATE_IRONMAN
                        && isRestrictedUimStorage(capability))
                {
                    continue;
                }
                total = safeAdd(total, quantityIn(entry.getValue(), names));
            }
        }
        return total;
    }

    /**
     * Returns observed UIM items that exist but should not silently satisfy a
     * normal recommendation because accessing them requires a retrieval plan.
     */
    public int restrictedQuantity(String... names)
    {
        if (data == null || names == null || names.length == 0
                || accountMode() != AccountMode.ULTIMATE_IRONMAN
                || data.getStorage() == null)
        {
            return 0;
        }

        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : data.getStorage().getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!isRestrictedUimStorage(capability)
                    || !data.getStorage().verified(capability))
            {
                continue;
            }
            total = safeAdd(total, quantityIn(entry.getValue(), names));
        }
        return total;
    }

    public int quantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        if (data == null || itemClass == null) return 0;
        int total = 0;
        total = safeAdd(total, quantityMatching(data.getInventory() == null
                ? null : data.getInventory().getItems(), itemClass, excludedNames));
        total = safeAdd(total, quantityMatching(data.getEquipment() == null
                ? null : data.getEquipment().getEquippedItems(), itemClass, excludedNames));
        AccountMode mode = accountMode();
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            total = safeAdd(total, quantityMatching(data.getBank() == null
                    ? null : data.getBank().getItems(), itemClass, excludedNames));
        if (useGroupStorage && mode.isGroupIronman()
                && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved())
            total = safeAdd(total, quantityMatching(data.getGroupStorage().getItems(),
                    itemClass, excludedNames));
        if (data.getStorage() != null)
        {
            for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                    : data.getStorage().getObservedContents().entrySet())
            {
                if (!data.getStorage().verified(entry.getKey())) continue;
                if (mode == AccountMode.ULTIMATE_IRONMAN
                        && isRestrictedUimStorage(entry.getKey())) continue;
                total = safeAdd(total, quantityMatching(entry.getValue(),
                        itemClass, excludedNames));
            }
        }
        return total;
    }

    public int restrictedQuantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        if (data == null || itemClass == null
                || accountMode() != AccountMode.ULTIMATE_IRONMAN
                || data.getStorage() == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : data.getStorage().getObservedContents().entrySet())
        {
            if (!isRestrictedUimStorage(entry.getKey())
                    || !data.getStorage().verified(entry.getKey())) continue;
            total = safeAdd(total, quantityMatching(entry.getValue(),
                    itemClass, excludedNames));
        }
        return total;
    }

    public int equippedQuantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        return data == null || data.getEquipment() == null ? 0
                : quantityMatching(data.getEquipment().getEquippedItems(),
                        itemClass, excludedNames);
    }

    /** True only when one of the names is currently equipped. */
    public boolean equipped(String... names)
    {
        return equippedQuantity(names) > 0;
    }

    /** Returns the observed stack quantity in equipped slots. */
    public int equippedQuantity(String... names)
    {
        return data == null || data.getEquipment() == null ? 0
                : quantityIn(data.getEquipment().getEquippedItems(), names);
    }

    public boolean bankObserved()
    {
        return data != null && data.getAccount() != null
                && accountMode() != AccountMode.ULTIMATE_IRONMAN
                && data.getBank() != null;
    }

    /**
     * True only when the mode's ordinary directly-usable ownership surface has
     * been observed. UIM has no conventional bank, so a complete inventory and
     * equipment snapshot replaces bank evidence; account mode alone never
     * proves an empty inventory.
     */
    public boolean primaryOwnershipObserved()
    {
        if (data == null || data.getAccount() == null) return false;
        if (data.getInventory() == null || data.getEquipment() == null)
            return false;
        if (accountMode() == AccountMode.ULTIMATE_IRONMAN)
            return true;
        return bankObserved();
    }

    /** Includes opted-in GIM Group Storage in the known ownership boundary. */
    public boolean usableOwnershipObserved()
    {
        if (!primaryOwnershipObserved()) return false;
        AccountMode mode = accountMode();
        return !mode.isGroupIronman() || !useGroupStorage
                || groupStorageObserved();
    }

    /**
     * Evidence boundary for consumed resources, which never counts equipment
     * but still must include every enabled ordinary resource container.
     */
    public boolean resourceContainersObserved()
    {
        if (data == null || data.getAccount() == null
                || data.getInventory() == null) return false;
        AccountMode mode = accountMode();
        if (mode == AccountMode.ULTIMATE_IRONMAN) return true;
        if (!bankObserved()) return false;
        return !mode.isGroupIronman() || !useGroupStorage
                || groupStorageObserved();
    }

    public boolean groupStorageObserved()
    {
        return data != null && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved();
    }

    private AccountMode accountMode()
    {
        return data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
    }

    private static boolean isRestrictedUimStorage(StorageCapability capability)
    {
        return UimStorageMechanics.isRestrictedRetrieval(capability);
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
                    total = safeAdd(total, Math.max(0, item.getQuantity()));
                    break;
                }
            }
        }
        return total;
    }

    private static int quantityMatching(Iterable<ItemStackSnapshot> items,
            ItemRequirementClass itemClass, Iterable<String> excludedNames)
    {
        if (items == null || itemClass == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item == null || item.getName() == null
                    || !itemClass.matches(item.getName())
                    || excluded(item.getName(), excludedNames)) continue;
            total = safeAdd(total, Math.max(0, item.getQuantity()));
        }
        return total;
    }

    private static boolean excluded(String itemName, Iterable<String> excludedNames)
    {
        if (itemName == null || excludedNames == null) return false;
        String actual = normalize(itemName);
        for (String excluded : excludedNames)
            if (excluded != null && actual.equals(normalize(excluded))) return true;
        return false;
    }

    private static int safeAdd(int a, int b)
    {
        if (a >= Integer.MAX_VALUE - b) return Integer.MAX_VALUE;
        return a + b;
    }

    private static String normalize(String value)
    {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
