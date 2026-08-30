package com.udderlywet.osrsstrategist;

/** Exact aggregate market-price evidence for a deterministic material list. */
public final class PurchaseCostEstimate
{
    private final boolean complete;
    private final long totalCost;

    public PurchaseCostEstimate(boolean complete, long totalCost)
    {
        this.complete = complete;
        this.totalCost = Math.max(0L, totalCost);
    }

    public boolean isComplete() { return complete; }
    public long getTotalCost() { return totalCost; }

    public static PurchaseCostEstimate unknown()
    {
        return new PurchaseCostEstimate(false, 0L);
    }
}
