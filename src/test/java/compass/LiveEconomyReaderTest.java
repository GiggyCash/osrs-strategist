package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LiveEconomyReaderTest
{
    private final MarketPriceService prices = new MarketPriceService()
    {
        @Override
        public int priceByItemId(int itemId)
        {
            if (itemId == 1515) return 1000;
            if (itemId == 1517) return 500;
            return 0;
        }
    };

    @Test
    public void mainCountsCoinsTokensAndObservedBankValue()
    {
        LiveEconomyReader reader = new LiveEconomyReader(prices);
        ItemsState inventory = new ItemsState(Arrays.asList(
                new ItemState(995, "Coins", 5000),
                new ItemState(13204, "Platinum token", 2)));
        ItemsState bank = new ItemsState(Arrays.asList(
                new ItemState(995, "Coins", 10000),
                new ItemState(1515, "Yew logs", 10),
                new ItemState(1517, "Maple logs", 20)), 1L);

        AccountEconomySnapshot economy = reader.read(
                account(0), inventory, bank);

        assertEquals(17000L, economy.getCoins());
        assertEquals(20000L, economy.getEstimatedBankValue());
        assertEquals(Confidence.VERIFIED,
                economy.getConfidence());
    }

    @Test
    public void unopenedMainBankDoesNotPretendCashIsComplete()
    {
        LiveEconomyReader reader = new LiveEconomyReader(prices);
        AccountEconomySnapshot economy = reader.read(
                account(0),
                new ItemsState(Collections.singletonList(
                        new ItemState(995, "Coins", 5000))),
                null);

        assertEquals(5000L, economy.getCoins());
        assertEquals(Confidence.CHECK_NEEDED,
                economy.getConfidence());
    }

    @Test
    public void uimIgnoresImpossibleNormalBankCash()
    {
        LiveEconomyReader reader = new LiveEconomyReader(prices);
        AccountEconomySnapshot economy = reader.read(
                account(2),
                new ItemsState(Collections.singletonList(
                        new ItemState(995, "Coins", 1234))),
                new ItemsState(Collections.singletonList(
                        new ItemState(995, "Coins", 999999)), 1L));

        assertEquals(1234L, economy.getCoins());
        assertEquals(0L, economy.getEstimatedBankValue());
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Economy Test",
                typeCode,
                typeCode == 0 ? "Main" : "UIM",
                MembershipStatus.P2P,
                1,
                32,
                0L,
                levels,
                xp);
    }
}
