package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

/**
 * Resolves current tradeable-item prices through RuneLite's own ItemManager.
 *
 * <p>RuneLite refreshes its price cache on login and exposes both name search
 * and getItemPrice. Compass only uses exact-name matches so a request for
 * "Yew logs" never silently becomes a similarly named item.</p>
 */
@Singleton
public class MarketPriceService
{
    private final ItemManager itemManager;

    @Inject
    public MarketPriceService(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    /** Test constructor for callers that only need a no-price fallback. */
    public MarketPriceService()
    {
        this.itemManager = null;
    }

    public MarketPriceQuote quote(String exactItemName)
    {
        if (itemManager == null || exactItemName == null
                || exactItemName.trim().isEmpty())
        {
            return null;
        }

        try
        {
            var results = itemManager.search(exactItemName);
            if (results == null) return null;
            for (ItemPrice result : results)
            {
                var itemId = result.getId();
                if (itemId <= 0) continue;
                var composition = itemManager.getItemComposition(itemId);
                if (composition == null || composition.getName() == null
                        || !exactItemName.equalsIgnoreCase(composition.getName()))
                {
                    continue;
                }
                var price = itemManager.getItemPrice(itemId);
                if (price <= 0) return null;
                return new MarketPriceQuote(
                        itemId,
                        composition.getName(),
                        price);
            }
        }
        catch (RuntimeException ex)
        {
            return null;
        }
        return null;
    }

    public int priceByItemId(int itemId)
    {
        if (itemManager == null || itemId <= 0) return 0;
        try
        {
            return Math.max(0, itemManager.getItemPrice(itemId));
        }
        catch (RuntimeException ex)
        {
            return 0;
        }
    }

}
