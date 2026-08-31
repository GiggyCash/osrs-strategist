package compass;

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
            List<MethodInput> missing)
    {
        var estimate = estimate(missing);
        if (!estimate.isComplete() || estimate.getTotalCost() <= 0) return null;
        var total = estimate.getTotalCost();

        var text = new StringBuilder();
        text.append(Text.get(412))
                .append(format(total))
                .append(" coins total.");

        if (economy != null
                && economy.getConfidence() == Confidence.VERIFIED)
        {
            var cash = economy.getCoins();
            if (cash >= total)
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(Text.get(413))
                        .append(format(cash - total))
                        .append(" after the buy.");
            }
            else
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(Text.get(414))
                        .append(format(total - cash))
                        .append(Text.get(415));
            }
        }
        else
        {
            text.append(Text.get(416));
        }
        return text.toString();
    }

    /**
     * Resolves every exact-name quote or fails the aggregate closed. A partial
     * price list must never make an entire method appear cheaper than it is.
     */
    public PurchaseCostEstimate estimate(List<MethodInput> missing)
    {
        if (marketPriceService == null || missing == null || missing.isEmpty())
            return PurchaseCostEstimate.unknown();

        var total = 0L;
        var sawInput = false;
        for (MethodInput input : missing)
        {
            if (input == null || input.getQuantity() <= 0) continue;
            sawInput = true;
            var quote = marketPriceService.quote(input.getName());
            if (quote == null || !quote.hasPrice())
                return PurchaseCostEstimate.unknown();
            total = safeAdd(total, safeMultiply(
                    quote.getUnitPrice(), input.getQuantity()));
        }
        return sawInput && total > 0
                ? new PurchaseCostEstimate(true, total)
                : PurchaseCostEstimate.unknown();
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
