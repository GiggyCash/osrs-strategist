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
                    "An exact live price is unavailable, so Compass will not assume the material is tradeable or cheap.");
        if (economy == null
                || economy.getConfidence() != RecommendationConfidence.VERIFIED)
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    estimate.getTotalCost(),
                    economy == null ? 0L : economy.getCoins(),
                    RecommendationConfidence.CHECK_NEEDED,
                    "The price is known, but verified liquid coins are not.");

        long cost = estimate.getTotalCost();
        long coins = Math.max(0L, economy.getCoins());
        if (coins < cost)
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, RecommendationConfidence.CHECK_NEEDED,
                    "Verified liquid coins do not cover the purchase.");

        long remaining = coins - cost;
        boolean trivialSpend = cost <= 1_000L && coins >= 5_000L;
        boolean lowBurden = cost <= coins / 10L
                && remaining >= MINIMUM_LIQUID_BUFFER;
        if (trivialSpend || lowBurden)
            return decision(MainPurchaseChoice.BUY, cost, coins,
                    RecommendationConfidence.VERIFIED,
                    "The exact purchase is a low-burden use of verified liquid wealth.");

        if (reviewedSelfSourceRoute)
            return decision(MainPurchaseChoice.SELF_SOURCE, cost, coins,
                    RecommendationConfidence.VERIFIED,
                    "The purchase would consume a material share of verified liquid wealth, so the reviewed self-source family is preferred without pretending to know an exact GP/hour value.");

        return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                RecommendationConfidence.CHECK_NEEDED,
                "The purchase is affordable but materially burdens liquid wealth, and no reviewed self-source family is attached.");
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
