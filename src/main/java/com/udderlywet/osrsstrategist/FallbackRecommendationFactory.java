package com.udderlywet.osrsstrategist;

/** Honest, harmless actions used only when the ranked pool cannot lead. */
final class FallbackRecommendationFactory
{
    static final String PREFIX = "fallback:";

    private FallbackRecommendationFactory() {}

    static Recommendation forState(StrategyDataBundle data)
    {
        if (data == null || data.getAccount() == null)
            return fallback("login", "Log in to continue",
                    "Log in to RuneScape so Strategist can observe your account state.",
                    "No character state is currently available.");

        if (data.getInventory() == null)
            return fallback("inventory", "Open your inventory",
                    "Open your inventory tab so Strategist can verify carried supplies.",
                    "Your carried items have not been observed yet.");

        if (data.getEquipment() == null)
            return fallback("equipment", "Open your equipment tab",
                    "Open your equipment tab so Strategist can verify your current setup.",
                    "Your equipped items have not been observed yet.");

        AccountMode mode = AccountMode.fromTypeCode(
                data.getAccount().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.getBank() == null)
            return fallback("bank", "Open your bank",
                    "Open your bank once so Strategist can verify available supplies.",
                    "No bank snapshot has been observed for this account.");

        return fallback("goal", "Review your Strategist goal",
                "Choose or confirm a goal in Strategist, then continue a familiar legal activity while the next recommendation is evaluated.",
                "The safe ranked candidate pool is currently exhausted; no account facts are being assumed.");
    }

    static boolean isFallback(Recommendation recommendation)
    {
        return recommendation != null && recommendation.getId() != null
                && recommendation.getId().startsWith(PREFIX);
    }

    private static Recommendation fallback(String id, String title,
            String action, String reason)
    {
        return new Recommendation(PREFIX + id, title, reason,
                Double.NEGATIVE_INFINITY, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance(action,
                        "No supplies are required for this verification step.",
                        "Use the relevant RuneLite or game interface.",
                        "This fallback reports only the state Strategist has not observed."),
                CandidateSafetyEvidence.harmless(true));
    }
}
