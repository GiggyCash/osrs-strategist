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
public final class ItemIndex
{
    private final GameData data;
    private final boolean useGroupStorage;


    public boolean has(String... names)
    {
        return quantity(names) > 0;
    }

    /** Resolves an item requirement against the mode-safe observed containers. */
    public RequirementCheck check(ResourceRequirement need)
    {
        int observed = quantity(need.getItemIds());
        if (observed >= need.getRequiredQuantity())
            return new RequirementCheck(need.getId(), need.getLabel(),
                    RequirementState.VERIFIED, Text.get(1435) + observed
                    + (accountMode() == AccountMode.ULTIMATE_IRONMAN
                            ? Text.get(703) : Text.get(704)
                            + (usesGroupStorage() ? Text.get(1436) : ".")));
        if (accountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            int restricted = restrictedQuantity(need.getItemIds());
            if (observed + restricted >= need.getRequiredQuantity())
                return checkNeeded(need, Text.get(705));
            boolean knownStorage = data != null && data.storage() != null
                    && !data.storage().getObservedContents().isEmpty();
            return checkNeeded(need, knownStorage
                    ? "Only " + observed + Text.get(706)
                            + need.getRequiredQuantity() + "."
                    : Text.get(1437) + observed + Text.get(707));
        }
        if (data == null || data.bank() == null)
            return checkNeeded(need, Text.get(1437) + observed
                    + Text.get(1438));
        return checkNeeded(need, "Only " + observed + Text.get(1439)
                + need.getRequiredQuantity() + ".");
    }

    public int quantity(int... itemIds)
    {
        if (data == null || itemIds == null) return 0;
        int total = quantityIn(data.inventory(), itemIds)
                + quantityIn(data.equipment(), itemIds);
        if (accountMode() != AccountMode.ULTIMATE_IRONMAN)
            total = safeAdd(total, quantityIn(data.bank(), itemIds));
        if (usesGroupStorage())
            total = safeAdd(total, quantityIn(data.groupStorage(), itemIds));
        if (data.storage() != null)
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : data.storage().getObservedContents().entrySet())
                if (data.storage().verified(entry.getKey())
                        && (accountMode() != AccountMode.ULTIMATE_IRONMAN
                        || !isRestrictedUimStorage(entry.getKey())))
                    total = safeAdd(total, quantityIn(entry.getValue(), itemIds));
        return total;
    }

    public int restrictedQuantity(int... itemIds)
    {
        if (data == null || itemIds == null
                || accountMode() != AccountMode.ULTIMATE_IRONMAN
                || data.storage() == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemState>> entry
                : data.storage().getObservedContents().entrySet())
            if (data.storage().verified(entry.getKey())
                    && isRestrictedUimStorage(entry.getKey()))
                total = safeAdd(total, quantityIn(entry.getValue(), itemIds));
        return total;
    }

    private RequirementCheck checkNeeded(ResourceRequirement need, String evidence)
    {
        return new RequirementCheck(need.getId(), need.getLabel(),
                RequirementState.CHECK_NEEDED, evidence);
    }

    private boolean usesGroupStorage()
    {
        return useGroupStorage && accountMode().isGroupIronman()
                && data != null && data.groupStorage() != null
                && data.groupStorage().isObserved();
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
        total = safeAdd(total, quantityIn(data.inventory() == null
                ? null : data.inventory().getItems(), names));
        total = safeAdd(total, quantityIn(data.equipment() == null
                ? null : data.equipment().getEquippedItems(), names));

        AccountMode mode = accountMode();
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            total = safeAdd(total, quantityIn(data.bank() == null
                    ? null : data.bank().getItems(), names));
        }

        if (useGroupStorage && mode.isGroupIronman()
                && data.groupStorage() != null
                && data.groupStorage().isObserved())
        {
            total = safeAdd(total,
                    quantityIn(data.groupStorage().getItems(), names));
        }

        if (data.storage() != null)
        {
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : data.storage().getObservedContents().entrySet())
            {
                StorageCapability capability = entry.getKey();
                if (!data.storage().verified(capability)) continue;
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
                || data.storage() == null)
        {
            return 0;
        }

        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemState>> entry
                : data.storage().getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!isRestrictedUimStorage(capability)
                    || !data.storage().verified(capability))
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
        total = safeAdd(total, quantityMatching(data.inventory() == null
                ? null : data.inventory().getItems(), itemClass, excludedNames));
        total = safeAdd(total, quantityMatching(data.equipment() == null
                ? null : data.equipment().getEquippedItems(), itemClass, excludedNames));
        AccountMode mode = accountMode();
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            total = safeAdd(total, quantityMatching(data.bank() == null
                    ? null : data.bank().getItems(), itemClass, excludedNames));
        if (useGroupStorage && mode.isGroupIronman()
                && data.groupStorage() != null
                && data.groupStorage().isObserved())
            total = safeAdd(total, quantityMatching(data.groupStorage().getItems(),
                    itemClass, excludedNames));
        if (data.storage() != null)
        {
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : data.storage().getObservedContents().entrySet())
            {
                if (!data.storage().verified(entry.getKey())) continue;
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
                || data.storage() == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemState>> entry
                : data.storage().getObservedContents().entrySet())
        {
            if (!isRestrictedUimStorage(entry.getKey())
                    || !data.storage().verified(entry.getKey())) continue;
            total = safeAdd(total, quantityMatching(entry.getValue(),
                    itemClass, excludedNames));
        }
        return total;
    }

    public int equippedQuantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        return data == null || data.equipment() == null ? 0
                : quantityMatching(data.equipment().getEquippedItems(),
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
        return data == null || data.equipment() == null ? 0
                : quantityIn(data.equipment().getEquippedItems(), names);
    }

    public boolean bankObserved()
    {
        return data != null && data.account() != null
                && accountMode() != AccountMode.ULTIMATE_IRONMAN
                && data.bank() != null;
    }

    /**
     * True only when the mode's ordinary directly-usable ownership surface has
     * been observed. UIM has no conventional bank, so a complete inventory and
     * equipment snapshot replaces bank evidence; account mode alone never
     * proves an empty inventory.
     */
    public boolean primaryOwnershipObserved()
    {
        if (data == null || data.account() == null) return false;
        if (data.inventory() == null || data.equipment() == null)
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
        if (data == null || data.account() == null
                || data.inventory() == null) return false;
        AccountMode mode = accountMode();
        if (mode == AccountMode.ULTIMATE_IRONMAN) return true;
        if (!bankObserved()) return false;
        return !mode.isGroupIronman() || !useGroupStorage
                || groupStorageObserved();
    }

    public boolean groupStorageObserved()
    {
        return data != null && data.groupStorage() != null
                && data.groupStorage().isObserved();
    }

    private AccountMode accountMode()
    {
        return data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().getAccountTypeCode());
    }

    private static boolean isRestrictedUimStorage(StorageCapability capability)
    {
        return UimStorageMechanics.isRestrictedRetrieval(capability);
    }

    private static int quantityIn(
            Iterable<ItemState> items,
            String... names)
    {
        if (items == null) return 0;
        int total = 0;
        for (ItemState item : items)
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

    private static int quantityIn(ItemsState state, int... ids)
    {
        return state == null ? 0 : quantityIn(state.getItems(), ids);
    }

    private static int quantityIn(Iterable<ItemState> items, int... ids)
    {
        if (items == null) return 0;
        int total = 0;
        for (ItemState item : items)
            if (item != null)
                for (int id : ids)
                    if (item.getItemId() == id)
                    {
                        total = safeAdd(total, Math.max(0, item.getQuantity()));
                        break;
                    }
        return total;
    }

    private static int quantityMatching(Iterable<ItemState> items,
            ItemRequirementClass itemClass, Iterable<String> excludedNames)
    {
        if (items == null || itemClass == null) return 0;
        int total = 0;
        for (ItemState item : items)
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
