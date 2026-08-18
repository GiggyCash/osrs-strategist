package com.udderlywet.osrsstrategist;

/**
 * One exact consumed-input requirement after Compass has compared the plan
 * with usable account storage.
 *
 * <p>A reusable source is deliberately represented separately from owned
 * quantity. For example, an equipped elemental staff can satisfy an elemental
 * rune requirement without pretending the account owns millions of runes.</p>
 */
public final class ResourcePlanEntry
{
    private final String name;
    private final int itemId;
    private final int required;
    private final int usableOwned;
    private final int missing;
    private final int restrictedOwned;
    private final String reusableSource;

    public ResourcePlanEntry(
            String name,
            int itemId,
            int required,
            int usableOwned,
            int missing,
            int restrictedOwned,
            String reusableSource)
    {
        this.name = name;
        this.itemId = itemId;
        this.required = Math.max(0, required);
        this.usableOwned = Math.max(0, usableOwned);
        this.missing = Math.max(0, missing);
        this.restrictedOwned = Math.max(0, restrictedOwned);
        this.reusableSource = reusableSource;
    }

    public String getName() { return name; }
    public int getItemId() { return itemId; }
    public int getRequired() { return required; }
    public int getUsableOwned() { return usableOwned; }
    public int getMissing() { return missing; }
    public int getRestrictedOwned() { return restrictedOwned; }
    public String getReusableSource() { return reusableSource; }

    public boolean isSatisfied()
    {
        return missing <= 0;
    }

    public boolean isSatisfiedByReusableSource()
    {
        return reusableSource != null && !reusableSource.trim().isEmpty();
    }

    public ResolvedMethodInput missingInput()
    {
        return new ResolvedMethodInput(name, itemId, missing);
    }
}
