package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One live RuneLite market-price lookup result. */
public final class MarketPriceQuote
{
    @Getter
    private final int itemId;
    @Getter
    private final String itemName;
    @Getter
    private final int unitPrice;

    public MarketPriceQuote(int itemId, String itemName, int unitPrice)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unitPrice = Math.max(0, unitPrice);
    }


    public boolean hasPrice()
    {
        return itemId > 0 && unitPrice > 0;
    }
}
