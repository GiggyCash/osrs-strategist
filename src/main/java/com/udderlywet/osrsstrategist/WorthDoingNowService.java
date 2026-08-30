package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Property-driven DO NOW/PREP/WAIT/SKIP decision shared by domains. */
@Singleton
public final class WorthDoingNowService
{
    public WorthDoingNowAssessment assess(ActivityValueProfile value)
    {
        if (value == null || !value.isLegal()
                || value.getReadiness() == RecommendationConfidence.BLOCKED)
            return result(WorthDoingNowState.SKIP, -1.0,
                    "The activity is not legal or safe for the current account state.");

        double benefit = value.getGoalValue() * 0.40
                + value.getAccountValue() * 0.25
                + value.getSetupReuseValue() * 0.15
                + Math.max(-1.0, value.getTravelValue()) * 0.10
                + Math.max(-1.0, value.getResourceValue()) * 0.10;
        double burden = value.getRiskBurden() * 0.45
                + value.getOpportunityCost() * 0.35
                + setupBurden(value) * 0.20;
        double net = benefit - burden;

        if (value.getReadiness() == RecommendationConfidence.CHECK_NEEDED)
        {
            if (value.isOrdinaryPreparationKnown() && net >= 0.05)
                return result(WorthDoingNowState.PREP_FIRST, net,
                        "The activity has enough value to prepare, but readiness is not yet proven.");
            return result(WorthDoingNowState.WAIT, net,
                    "Readiness or a worthwhile preparation route is not proven.");
        }
        if (value.getSessionMinutes() > 0
                && value.getSetupMinutes() >= value.getSessionMinutes())
            return result(WorthDoingNowState.WAIT, net,
                    "Setup would consume the available session.");
        if (net >= 0.05)
            return result(WorthDoingNowState.DO_NOW, net,
                    "The account is ready, and the activity's current benefit outweighs its setup and risk.");
        if (net <= -0.35)
            return result(WorthDoingNowState.SKIP, net,
                    "Risk or opportunity cost materially outweighs current value.");
        return result(WorthDoingNowState.WAIT, net,
                "The activity is legal, but its current value does not clear the detour threshold.");
    }

    private static double setupBurden(ActivityValueProfile value)
    {
        if (value.getSetupMinutes() <= 0) return 0.0;
        if (value.getSessionMinutes() <= 0)
            return Math.min(1.0, value.getSetupMinutes() / 20.0);
        return Math.min(1.0, value.getSetupMinutes()
                / (double) value.getSessionMinutes());
    }

    private static WorthDoingNowAssessment result(
            WorthDoingNowState state, double utility, String reason)
    {
        return new WorthDoingNowAssessment(state,
                Math.max(-1.0, Math.min(1.0, utility)), reason);
    }
}
