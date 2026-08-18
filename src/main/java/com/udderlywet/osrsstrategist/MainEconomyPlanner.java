package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Conservative Main-account buy-vs-gather decision layer. */
@Singleton
public class MainEconomyPlanner
{
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
                    "Grand Exchange purchase planning only applies to Main accounts.");
        }

        StrategyDataBundle data = context.getData();
        AccountEconomySnapshot economy = data == null ? null : data.getEconomy();
        if (economy == null
                || economy.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    candidate.totalCost(), economy == null ? 0L : economy.getCoins(),
                    RecommendationConfidence.CHECK_NEEDED,
                    "Verified cash state is required before recommending a purchase.");
        }

        long cost = candidate.totalCost();
        long coins = economy.getCoins();
        if (cost == Long.MAX_VALUE)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                    RecommendationConfidence.CHECK_NEEDED,
                    "Purchase cost overflowed safe calculation limits.");
        }

        if (coins < cost)
        {
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    "Current verified coins do not cover the purchase. Compare money makers, banked materials, and only safe protected-item-aware sale options before spending.");
        }

        if (candidate.getEstimatedSelfSourceMinutes() > 0
                && candidate.getEstimatedBuyMinutes()
                >= candidate.getEstimatedSelfSourceMinutes())
        {
            return decision(MainPurchaseChoice.SELF_SOURCE,
                    cost, coins, RecommendationConfidence.VERIFIED,
                    "Buying is affordable, but the verified time estimate does not beat self-sourcing.");
        }

        if (candidate.getEstimatedSelfSourceMinutes() <= 0)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    "The purchase is affordable, but a verified self-source/time comparison is still needed.");
        }

        return decision(MainPurchaseChoice.BUY,
                cost, coins, RecommendationConfidence.VERIFIED,
                "The purchase is affordable and the verified estimate saves time versus self-sourcing. No sale or purchase is performed automatically.");
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
