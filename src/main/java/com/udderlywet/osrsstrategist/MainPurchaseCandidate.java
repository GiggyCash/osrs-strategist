package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Verified-price input for comparing a Main's buy-vs-gather options. */
public final class MainPurchaseCandidate
{
    @Getter
    private final int itemId;
    @Getter
    private final String itemName;
    @Getter
    private final int quantity;
    @Getter
    private final long verifiedUnitPrice;
    @Getter
    private final int estimatedBuyMinutes;
    @Getter
    private final int estimatedSelfSourceMinutes;

    public MainPurchaseCandidate(
            int itemId,
            String itemName,
            int quantity,
            long verifiedUnitPrice,
            int estimatedBuyMinutes,
            int estimatedSelfSourceMinutes)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = Math.max(0, quantity);
        this.verifiedUnitPrice = Math.max(0L, verifiedUnitPrice);
        this.estimatedBuyMinutes = Math.max(0, estimatedBuyMinutes);
        this.estimatedSelfSourceMinutes = Math.max(0, estimatedSelfSourceMinutes);
    }


    public long totalCost()
    {
        try
        {
            return Math.multiplyExact(verifiedUnitPrice, (long) quantity);
        }
        catch (ArithmeticException ex)
        {
            return Long.MAX_VALUE;
        }
    }
}
