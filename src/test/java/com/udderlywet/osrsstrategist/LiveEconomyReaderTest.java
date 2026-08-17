package com.udderlywet.osrsstrategist;

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
        InventorySnapshot inventory = new InventorySnapshot(Arrays.asList(
                new ItemStackSnapshot(995, "Coins", 5000),
                new ItemStackSnapshot(13204, "Platinum token", 2)));
        BankSnapshot bank = new BankSnapshot(Arrays.asList(
                new ItemStackSnapshot(995, "Coins", 10000),
                new ItemStackSnapshot(1515, "Yew logs", 10),
                new ItemStackSnapshot(1517, "Maple logs", 20)), 1L);

        AccountEconomySnapshot economy = reader.read(
                account(0), inventory, bank);

        assertEquals(17000L, economy.getCoins());
        assertEquals(20000L, economy.getEstimatedBankValue());
        assertEquals(RecommendationConfidence.VERIFIED,
                economy.getConfidence());
    }

    @Test
    public void unopenedMainBankDoesNotPretendCashIsComplete()
    {
        LiveEconomyReader reader = new LiveEconomyReader(prices);
        AccountEconomySnapshot economy = reader.read(
                account(0),
                new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(995, "Coins", 5000))),
                null);

        assertEquals(5000L, economy.getCoins());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                economy.getConfidence());
    }

    @Test
    public void uimIgnoresImpossibleNormalBankCash()
    {
        LiveEconomyReader reader = new LiveEconomyReader(prices);
        AccountEconomySnapshot economy = reader.read(
                account(2),
                new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(995, "Coins", 1234))),
                new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(995, "Coins", 999999)), 1L));

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
