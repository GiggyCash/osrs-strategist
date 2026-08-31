package compass;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PurchaseCostAdvisorTest
{
    private final MarketPriceService prices = new MarketPriceService()
    {
        @Override
        public MarketPriceQuote quote(String name)
        {
            if ("Yew logs".equalsIgnoreCase(name))
                return new MarketPriceQuote(1515, "Yew logs", 400);
            if ("Bow string".equalsIgnoreCase(name))
                return new MarketPriceQuote(1777, "Bow string", 100);
            return null;
        }
    };

    @Test
    public void tellsMainExactCostAndRemainingCashWhenAffordable()
    {
        PurchaseCostAdvisor advisor = new PurchaseCostAdvisor(prices);
        String advice = advisor.advice(
                new AccountEconomySnapshot(
                        100000,
                        0,
                        Confidence.VERIFIED),
                Arrays.asList(
                        new MethodInput("Yew logs", -1, 100),
                        new MethodInput("Bow string", -1, 100)));

        assertTrue(advice.contains("50,000 coins total"));
        assertTrue(advice.contains("leaving about 50,000"));
    }

    @Test
    public void tellsMainExactCashGapWhenNotAffordable()
    {
        PurchaseCostAdvisor advisor = new PurchaseCostAdvisor(prices);
        String advice = advisor.advice(
                new AccountEconomySnapshot(
                        20000,
                        0,
                        Confidence.VERIFIED),
                Arrays.asList(
                        new MethodInput("Yew logs", -1, 100),
                        new MethodInput("Bow string", -1, 100)));

        assertTrue(advice.contains("30,000 coins short"));
        assertTrue(advice.contains("Do not treat the purchase as ready"));
    }
}
