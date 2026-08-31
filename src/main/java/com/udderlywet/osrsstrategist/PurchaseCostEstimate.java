package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Exact aggregate market-price evidence for a deterministic material list. */
@Getter
public final class PurchaseCostEstimate
{
    private final boolean complete;
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
