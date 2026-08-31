package com.udderlywet.osrsstrategist;

import lombok.Getter;

@Getter
public final class MainPurchaseDecision
{
    private final MainPurchaseChoice choice;
    private final long totalCost;
    private final long observedCoins;
    private final Confidence confidence;
    private final String explanation;

    public MainPurchaseDecision(
            MainPurchaseChoice choice,
            long totalCost,
            long observedCoins,
            Confidence confidence,
            String explanation)
    {
        this.choice = choice;
        this.totalCost = Math.max(0L, totalCost);
        this.observedCoins = Math.max(0L, observedCoins);
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.explanation = explanation;
    }

}
