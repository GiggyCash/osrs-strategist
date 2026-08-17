package com.udderlywet.osrsstrategist;

/** One live RuneLite market-price lookup result. */
public final class MarketPriceQuote
{
    private final int itemId;
    private final String itemName;
    private final int unitPrice;

    public MarketPriceQuote(int itemId, String itemName, int unitPrice)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unitPrice = Math.max(0, unitPrice);
    }

    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getUnitPrice() { return unitPrice; }

    public boolean hasPrice()
    {
        return itemId > 0 && unitPrice > 0;
    }
}
