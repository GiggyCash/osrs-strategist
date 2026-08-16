package com.udderlywet.osrsstrategist;

/** Verified-price input for comparing a Main's buy-vs-gather options. */
public final class MainPurchaseCandidate
{
    private final int itemId;
    private final String itemName;
    private final int quantity;
    private final long verifiedUnitPrice;
    private final int estimatedBuyMinutes;
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

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public long getVerifiedUnitPrice() { return verifiedUnitPrice; }
    public int getEstimatedBuyMinutes() { return estimatedBuyMinutes; }
    public int getEstimatedSelfSourceMinutes() { return estimatedSelfSourceMinutes; }

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
