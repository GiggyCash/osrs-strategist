package compass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainEconomyPlannerTest
{
    private final MainEconomyPlanner planner = new MainEconomyPlanner();

    @Test
    public void unmeasuredPurchaseUsesBroadWealthBands()
    {
        AccountEconomySnapshot economy = new AccountEconomySnapshot(
                100_000L, 100_000L, Confidence.VERIFIED);
        assertEquals(MainPurchaseChoice.BUY,
                planner.evaluateUnmeasuredPurchase(economy,
                        new PurchaseCostEstimate(true, 5_000L), true)
                        .getChoice());
        assertEquals(MainPurchaseChoice.SELF_SOURCE,
                planner.evaluateUnmeasuredPurchase(economy,
                        new PurchaseCostEstimate(true, 50_000L), true)
                        .getChoice());
        assertEquals(MainPurchaseChoice.CHECK_NEEDED,
                planner.evaluateUnmeasuredPurchase(economy,
                        PurchaseCostEstimate.unknown(), true).getChoice());
    }
}
