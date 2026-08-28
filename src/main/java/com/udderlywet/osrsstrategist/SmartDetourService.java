package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Applies the common worth-doing-now policy to typed detour economics. */
@Singleton
public final class SmartDetourService
{
    private final WorthDoingNowService worthDoingNow;

    @Inject
    public SmartDetourService(WorthDoingNowService worthDoingNow)
    {
        this.worthDoingNow = worthDoingNow == null
                ? new WorthDoingNowService() : worthDoingNow;
    }

    public SmartDetourService() { this(new WorthDoingNowService()); }

    public WorthDoingNowAssessment assess(SmartDetourProfile detour)
    {
        if (detour == null) return worthDoingNow.assess(null);
        int duration = Math.max(1, detour.getDetourMinutes());
        double travel = Math.min(1.0,
                detour.getTravelMinutesSaved() / (double) duration);
        double setup = Math.min(1.0,
                detour.getSetupMinutesSaved() / (double) duration);
        double durationCost = detour.getSessionMinutes() <= 0 ? 0.0
                : Math.min(1.0, duration
                        / (double) detour.getSessionMinutes());
        double opportunityCost = Math.min(1.0,
                detour.getInterruptionCost() * 0.65 + durationCost * 0.35);
        ActivityValueProfile value = ActivityValueProfile.builder()
                .legal(detour.isLegal())
                .readiness(detour.getReadiness())
                .ordinaryPreparationKnown(detour.isOrdinaryPreparationKnown())
                .setupMinutes(Math.max(0, detour.getSetupMinutesRequired()
                        - detour.getSetupMinutesSaved()))
                .sessionMinutes(detour.getSessionMinutes())
                .goalValue(detour.getGoalValue())
                .accountValue(detour.getAccountValue())
                .setupReuseValue(setup)
                .travelValue(travel)
                .resourceValue(detour.getResourceValue())
                .riskBurden(detour.getRiskBurden())
                .opportunityCost(opportunityCost)
                .build();
        return worthDoingNow.assess(value);
    }
}
