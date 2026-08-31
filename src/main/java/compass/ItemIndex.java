package compass;
import static compass.Text.get;

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
        var observed = quantity(need.getItemIds());
        if (observed >= need.getRequiredQuantity())
            return new RequirementCheck(need.id, need.getLabel(),
                    RequirementState.VERIFIED, get(1435) + observed
                    + (accountMode() == AccountMode.ULTIMATE_IRONMAN
                            ? get(703) : get(704)
                            + (usesGroupStorage() ? get(1436) : ".")));
        if (accountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            var restricted = restrictedQuantity(need.getItemIds());
            if (observed + restricted >= need.getRequiredQuantity())
                return checkNeeded(need, get(705));
            boolean knownStorage = data != null && data.storage() != null
                    && !data.storage().getObservedContents().isEmpty();
            return checkNeeded(need, knownStorage
                    ? "Only " + observed + get(706)
                            + need.getRequiredQuantity() + "."
                    : get(1437) + observed + get(707));
        }
        if (data == null || data.bank() == null)
            return checkNeeded(need, get(1437) + observed
                    + get(1438));
        return checkNeeded(need, "Only " + observed + get(1439)
                + need.getRequiredQuantity() + ".");
    }

    public int quantity(int... itemIds)
    {
        if (data == null || itemIds == null) return 0;
        var total = 0;
        for (Iterable<ItemState> items : usableItems())
            total = safeAdd(total, quantityIn(items, itemIds));
        return total;
    }

    public int restrictedQuantity(int... itemIds)
    {
        if (data == null || itemIds == null
                || accountMode() != AccountMode.ULTIMATE_IRONMAN
                || data.storage() == null) return 0;
        var total = 0;
        for (Iterable<ItemState> items : restrictedItems())
            total = safeAdd(total, quantityIn(items, itemIds));
        return total;
    }

    private RequirementCheck checkNeeded(ResourceRequirement need, String evidence)
    {
        return new RequirementCheck(need.id, need.getLabel(),
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
        var total = 0;
        for (Iterable<ItemState> items : usableItems())
            total = safeAdd(total, quantityIn(items, names));
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

        var total = 0;
        for (Iterable<ItemState> items : restrictedItems())
            total = safeAdd(total, quantityIn(items, names));
        return total;
    }

    public int quantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        if (data == null || itemClass == null) return 0;
        var total = 0;
        for (Iterable<ItemState> items : usableItems())
            total = safeAdd(total, quantityMatching(items, itemClass, excludedNames));
        return total;
    }

    public int restrictedQuantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        if (data == null || itemClass == null
                || accountMode() != AccountMode.ULTIMATE_IRONMAN
                || data.storage() == null) return 0;
        var total = 0;
        for (Iterable<ItemState> items : restrictedItems())
            total = safeAdd(total, quantityMatching(items, itemClass, excludedNames));
        return total;
    }

    /** Highest-ranked observed usable item name, or null when none match. */
    public String bestName(java.util.function.ToIntFunction<String> rank)
    {
        return bestName(rank, usableItems());
    }

    public String bestInventoryName(java.util.function.ToIntFunction<String> rank)
    {
        return data == null || data.inventory() == null ? null
                : bestName(rank, Collections.singletonList(data.inventory().getItems()));
    }

    private static String bestName(java.util.function.ToIntFunction<String> rank,
            List<Iterable<ItemState>> sources)
    {
        String best = null;
        int bestRank = 0;
        for (Iterable<ItemState> items : sources)
            if (items != null) for (ItemState item : items)
            {
                if (item == null || item.getQuantity() <= 0) continue;
                int value = rank.applyAsInt(Names.lower(item.getName()));
                if (value > bestRank) { bestRank = value; best = item.getName(); }
            }
        return best;
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

    public int inventoryQuantity(String... names)
    {
        return data == null || data.inventory() == null ? 0
                : quantityIn(data.inventory().getItems(), names);
    }

    public int inventoryQuantityMatching(ItemRequirementClass itemClass,
            Iterable<String> excludedNames)
    {
        return data == null || data.inventory() == null ? 0
                : quantityMatching(data.inventory().getItems(), itemClass, excludedNames);
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
        var mode = accountMode();
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
        var mode = accountMode();
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
                : AccountMode.fromTypeCode(data.account().modeCode());
    }

    private List<Iterable<ItemState>> usableItems()
    {
        List<Iterable<ItemState>> result = new ArrayList<>();
        if (data == null) return result;
        if (data.inventory() != null) result.add(data.inventory().getItems());
        if (data.equipment() != null) result.add(data.equipment().getEquippedItems());
        var mode = accountMode();
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() != null)
            result.add(data.bank().getItems());
        if (usesGroupStorage()) result.add(data.groupStorage().getItems());
        if (data.storage() != null)
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : data.storage().getObservedContents().entrySet())
                if (data.storage().verified(entry.getKey())
                        && (mode != AccountMode.ULTIMATE_IRONMAN
                        || !isRestrictedUimStorage(entry.getKey())))
                    result.add(entry.getValue());
        return result;
    }

    private List<Iterable<ItemState>> restrictedItems()
    {
        List<Iterable<ItemState>> result = new ArrayList<>();
        if (data != null && accountMode() == AccountMode.ULTIMATE_IRONMAN
                && data.storage() != null)
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : data.storage().getObservedContents().entrySet())
                if (data.storage().verified(entry.getKey())
                        && isRestrictedUimStorage(entry.getKey()))
                    result.add(entry.getValue());
        return result;
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
        var total = 0;
        for (ItemState item : items)
        {
            if (item == null || item.getName() == null) continue;
            var actual = Names.lower(item.getName());
            for (String name : names)
            {
                if (name != null && actual.equals(Names.lower(name)))
                {
                    total = safeAdd(total, Math.max(0, item.getQuantity()));
                    break;
                }
            }
        }
        return total;
    }

    private static int quantityIn(Iterable<ItemState> items, int... ids)
    {
        if (items == null) return 0;
        var total = 0;
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
        var total = 0;
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
        var actual = Names.lower(itemName);
        for (String excluded : excludedNames)
            if (excluded != null && actual.equals(Names.lower(excluded))) return true;
        return false;
    }

    private static int safeAdd(int a, int b)
    {
        if (a >= Integer.MAX_VALUE - b) return Integer.MAX_VALUE;
        return a + b;
    }

}
