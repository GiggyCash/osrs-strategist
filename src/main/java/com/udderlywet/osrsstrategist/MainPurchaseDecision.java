package com.udderlywet.osrsstrategist;

import lombok.Getter;

@Getter
public final class MainPurchaseDecision
{
    private final MainPurchaseChoice choice;
    private final long totalCost;
    private final long observedCoins;
    private final RecommendationConfidence confidence;
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
