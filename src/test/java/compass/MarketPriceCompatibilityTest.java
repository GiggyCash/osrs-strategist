package compass;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MarketPriceCompatibilityTest
{
    @Test
    public void preservesRepresentableQuotes()
    {
        assertEquals(1, MarketPriceService.supportedPrice(1L));
        assertEquals(Integer.MAX_VALUE,
                MarketPriceService.supportedPrice((long) Integer.MAX_VALUE));
    }

    @Test
    public void unsupportedQuotesRemainUnknownInsteadOfBecomingAffordable()
    {
        for (long price : new long[] {Long.MIN_VALUE, -1L, 0L,
                (long) Integer.MAX_VALUE + 1L, 4294967297L, Long.MAX_VALUE})
            assertEquals(0, MarketPriceService.supportedPrice(price));
    }
}
