package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorthDoingNowServiceTest
{
    private final WorthDoingNowService service = new WorthDoingNowService();

    @Test
    public void verifiedGoalWorkWithReadyResourcesIsDoNow()
    {
        WorthDoingNowAssessment result = service.assess(
                ActivityValueProfile.builder()
                        .readiness(RecommendationConfidence.VERIFIED)
                        .goalValue(1.0).accountValue(0.6)
                        .resourceValue(0.8).travelValue(0.5)
                        .setupMinutes(3).sessionMinutes(60).build());
        assertEquals(WorthDoingNowState.DO_NOW, result.getState());
        assertTrue(result.getNetUtility() > 0.0);
    }

    @Test
    public void knownOrdinaryPreparationCanLeadOnlyAsPrepFirst()
    {
        WorthDoingNowAssessment result = service.assess(
                ActivityValueProfile.builder()
                        .readiness(RecommendationConfidence.CHECK_NEEDED)
                        .ordinaryPreparationKnown(true)
                        .goalValue(0.9).accountValue(0.5)
                        .setupMinutes(5).sessionMinutes(60).build());
        assertEquals(WorthDoingNowState.PREP_FIRST, result.getState());
    }

    @Test
    public void unknownPreparationWaitsInsteadOfFabricatingReadiness()
    {
        WorthDoingNowAssessment result = service.assess(
                ActivityValueProfile.builder()
                        .readiness(RecommendationConfidence.CHECK_NEEDED)
                        .goalValue(1.0).build());
        assertEquals(WorthDoingNowState.WAIT, result.getState());
    }

    @Test
    public void illegalAndHighCostLowValueActivitiesSkip()
    {
        assertEquals(WorthDoingNowState.SKIP, service.assess(
                ActivityValueProfile.builder().legal(false).build()).getState());
        assertEquals(WorthDoingNowState.SKIP, service.assess(
                ActivityValueProfile.builder()
                        .readiness(RecommendationConfidence.VERIFIED)
                        .riskBurden(1.0).opportunityCost(1.0)
                        .setupMinutes(20).sessionMinutes(60).build()).getState());
    }

    @Test
    public void setupLongerThanSessionWaitsEvenWhenReady()
    {
        WorthDoingNowAssessment result = service.assess(
                ActivityValueProfile.builder()
                        .readiness(RecommendationConfidence.VERIFIED)
                        .goalValue(1.0).setupMinutes(20).sessionMinutes(20)
                        .build());
        assertEquals(WorthDoingNowState.WAIT, result.getState());
    }
}
