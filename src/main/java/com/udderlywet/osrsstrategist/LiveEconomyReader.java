package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Builds a live economy snapshot from observed item containers and RuneLite prices. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LiveEconomyReader
{
    private final MarketPriceService marketPriceService;

    public AccountEconomySnapshot read(
            AccountSnapshot account,
            ItemsState inventory,
            ItemsState bank)
    {
        if (account == null) return null;
        var mode = AccountMode.fromTypeCode(account.getAccountTypeCode());

        long coins = spendableCurrency(inventory == null
                ? null : inventory.getItems());
        if (mode != AccountMode.ULTIMATE_IRONMAN && bank != null)
        {
            coins = safeAdd(coins, spendableCurrency(bank.getItems()));
        }

        var bankValue = 0L;
        if (bank != null && mode != AccountMode.ULTIMATE_IRONMAN)
        {
            for (ItemState item : bank.getItems())
            {
                if (item == null || item.getQuantity() <= 0) continue;
                int unitPrice = marketPriceService == null
                        ? 0
                        : marketPriceService.priceByItemId(item.getItemId());
                if (unitPrice <= 0) continue;
                bankValue = safeAdd(
                        bankValue,
                        safeMultiply(unitPrice, item.getQuantity()));
            }
        }

        Confidence confidence;
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            // Inventory coins are real, but coins held in specialized storage
            // are not universally exposed here yet.
            confidence = Confidence.CHECK_NEEDED;
        }
        else
        {
            confidence = bank == null
                    ? Confidence.CHECK_NEEDED
                    : Confidence.VERIFIED;
        }

        return new AccountEconomySnapshot(
                coins,
                bankValue,
                confidence);
    }

    private static long spendableCurrency(List<ItemState> items)
    {
        if (items == null) return 0L;
        var total = 0L;
        for (ItemState item : items)
        {
            if (item == null || item.getName() == null) continue;
            if ("Coins".equalsIgnoreCase(item.getName()))
            {
                total = safeAdd(total, item.getQuantity());
            }
            else if ("Platinum token".equalsIgnoreCase(item.getName())
                    || "Platinum tokens".equalsIgnoreCase(item.getName()))
            {
                total = safeAdd(total,
                        safeMultiply(1000L, item.getQuantity()));
            }
        }
        return total;
    }

    private static long safeMultiply(long a, long b)
    {
        if (a <= 0 || b <= 0) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    private static long safeAdd(long a, long b)
    {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
