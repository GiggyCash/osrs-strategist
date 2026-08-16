package com.udderlywet.osrsstrategist;

public final class AccountEconomySnapshot
{
    private final long coins;
    private final long estimatedBankValue;
    private final RecommendationConfidence confidence;

    public AccountEconomySnapshot(
            long coins,
            long estimatedBankValue,
            RecommendationConfidence confidence)
    {
        this.coins = coins;
        this.estimatedBankValue = estimatedBankValue;
        this.confidence = confidence;
    }

    public long getCoins()
    {
        return coins;
    }

    public long getEstimatedBankValue()
    {
        return estimatedBankValue;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }
}
