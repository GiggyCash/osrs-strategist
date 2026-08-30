package com.udderlywet.osrsstrategist;

import lombok.Getter;

public final class MainPurchaseDecision
{
    @Getter
    private final MainPurchaseChoice choice;
    @Getter
    private final long totalCost;
    @Getter
    private final long observedCoins;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String explanation;

    public MainPurchaseDecision(
            MainPurchaseChoice choice,
            long totalCost,
            long observedCoins,
            RecommendationConfidence confidence,
            String explanation)
    {
        this.choice = choice;
        this.totalCost = Math.max(0L, totalCost);
        this.observedCoins = Math.max(0L, observedCoins);
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
        this.explanation = explanation;
    }

}
