package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Converts an exact Main-account material shortfall into live GE cost advice. */
@Singleton
public class PurchaseCostAdvisor
{
    private final MarketPriceService marketPriceService;

    @Inject
    public PurchaseCostAdvisor(MarketPriceService marketPriceService)
    {
        this.marketPriceService = marketPriceService;
    }

    public PurchaseCostAdvisor()
    {
        this(new MarketPriceService());
    }

    /**
     * Returns an optional cost sentence. If any item price is unresolved, the
     * caller should still show exact quantities but omit a fake total GP value.
     */
    public String advice(
            AccountEconomySnapshot economy,
            List<ResolvedMethodInput> missing)
    {
        if (marketPriceService == null || missing == null || missing.isEmpty())
        {
            return null;
        }

        long total = 0L;
        for (ResolvedMethodInput input : missing)
        {
            if (input == null || input.getQuantity() <= 0) continue;
            MarketPriceQuote quote = marketPriceService.quote(input.getName());
            if (quote == null || !quote.hasPrice()) return null;
            long itemTotal = safeMultiply(
                    quote.getUnitPrice(),
                    input.getQuantity());
            total = safeAdd(total, itemTotal);
        }
        if (total <= 0) return null;

        StringBuilder text = new StringBuilder();
        text.append("At current RuneLite market prices, that purchase is about ")
                .append(format(total))
                .append(" coins total.");

        if (economy != null
                && economy.getConfidence() == RecommendationConfidence.VERIFIED)
        {
            long cash = economy.getCoins();
            if (cash >= total)
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(" verified spendable coins, leaving about ")
                        .append(format(cash - total))
                        .append(" after the buy.");
            }
            else
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(" verified spendable coins, so you are about ")
                        .append(format(total - cash))
                        .append(" coins short. Do not treat the purchase as ready until Strategist solves that cash gap or the live prices move.");
            }
        }
        else
        {
            text.append(" Cash is not fully verified yet, so affordability is still Check Needed.");
        }
        return text.toString();
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

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}
