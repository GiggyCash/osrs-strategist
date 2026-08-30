package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Exact aggregate market-price evidence for a deterministic material list. */
public final class PurchaseCostEstimate
{
    @Getter
    private final boolean complete;
    @Getter
    private final long totalCost;

    public PurchaseCostEstimate(boolean complete, long totalCost)
    {
        this.complete = complete;
        this.totalCost = Math.max(0L, totalCost);
    }


    public static PurchaseCostEstimate unknown()
    {
        return new PurchaseCostEstimate(false, 0L);
    }
}
