package compass;

import lombok.Getter;

/** One live RuneLite market-price lookup result. */
@Getter
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


    public boolean hasPrice()
    {
        return itemId > 0 && unitPrice > 0;
    }
}
