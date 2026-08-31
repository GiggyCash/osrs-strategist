package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Conservative Main-account buy-vs-gather decision layer. */
@Singleton
public class MainEconomyPlanner
{
    private static final long MINIMUM_LIQUID_BUFFER = 10_000L;

    public MainPurchaseDecision evaluatePurchase(
            StrategyContext context,
            MainPurchaseCandidate candidate)
    {
        if (context == null || candidate == null)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED, 0L, 0L,
                    RecommendationConfidence.CHECK_NEEDED,
                    "Purchase inputs are incomplete.");
        }

        if (context.getAccountMode() != AccountMode.MAIN)
        {
            return decision(MainPurchaseChoice.NOT_APPLICABLE,
                    candidate.totalCost(), 0L,
                    RecommendationConfidence.VERIFIED,
                    PlayerText.get("MEP1"));
        }

        StrategyDataBundle data = context.getData();
        AccountEconomySnapshot economy = data == null ? null : data.getEconomy();
        if (economy == null
                || economy.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    candidate.totalCost(), economy == null ? 0L : economy.getCoins(),
                    RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP2"));
        }

        long cost = candidate.totalCost();
        long coins = economy.getCoins();
        if (cost == Long.MAX_VALUE)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                    RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP3"));
        }

        if (coins < cost)
        {
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP4"));
        }

        if (candidate.getEstimatedSelfSourceMinutes() > 0
                && candidate.getEstimatedBuyMinutes()
                >= candidate.getEstimatedSelfSourceMinutes())
        {
            return decision(MainPurchaseChoice.SELF_SOURCE,
                    cost, coins, RecommendationConfidence.VERIFIED,
                    PlayerText.get("MEP5"));
        }

        if (candidate.getEstimatedSelfSourceMinutes() <= 0)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP6"));
        }

        return decision(MainPurchaseChoice.BUY,
                cost, coins, RecommendationConfidence.VERIFIED,
                PlayerText.get("MEP7"));
    }

    /**
     * Uses deliberately broad liquid-wealth bands when no defensible time
     * estimate exists. This avoids both fake GP/hour precision and the old
     * rule that every observed Main shortfall should simply be bought.
     */
    public MainPurchaseDecision evaluateUnmeasuredPurchase(
            AccountEconomySnapshot economy,
            PurchaseCostEstimate estimate,
            boolean reviewedSelfSourceRoute)
    {
        if (estimate == null || !estimate.isComplete()
                || estimate.getTotalCost() <= 0)
            return decision(MainPurchaseChoice.CHECK_NEEDED, 0L,
                    economy == null ? 0L : economy.getCoins(),
                    RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP8"));
        if (economy == null
                || economy.getConfidence() != RecommendationConfidence.VERIFIED)
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    estimate.getTotalCost(),
                    economy == null ? 0L : economy.getCoins(),
                    RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP9"));

        long cost = estimate.getTotalCost();
        long coins = Math.max(0L, economy.getCoins());
        if (coins < cost)
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    PlayerText.get("MEP10"));

        long remaining = coins - cost;
        boolean trivialSpend = cost <= 1_000L && coins >= 5_000L;
        boolean lowBurden = cost <= coins / 10L
                && remaining >= MINIMUM_LIQUID_BUFFER;
        if (trivialSpend || lowBurden)
            return decision(MainPurchaseChoice.BUY, cost, coins,
                    RecommendationConfidence.VERIFIED,
                    PlayerText.get("MEP11"));

        if (reviewedSelfSourceRoute)
            return decision(MainPurchaseChoice.SELF_SOURCE, cost, coins,
                    RecommendationConfidence.VERIFIED,
                    PlayerText.get("MEP12"));

        return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                RecommendationConfidence.CHECK_NEEDED,
                PlayerText.get("MEP13"));
    }

    public boolean maySuggestSale(
            int itemId,
            ProtectedItemProfile playerProtectedItems,
            boolean builtInProtected)
    {
        if (builtInProtected) return false;
        return playerProtectedItems == null
                || !playerProtectedItems.isProtected(itemId);
    }

    private static MainPurchaseDecision decision(
            MainPurchaseChoice choice,
            long totalCost,
            long coins,
            RecommendationConfidence confidence,
            String explanation)
    {
        return new MainPurchaseDecision(
                choice, totalCost, coins, confidence, explanation);
    }
}
