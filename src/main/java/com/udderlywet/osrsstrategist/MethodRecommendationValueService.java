package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Applies travel properties to a selected method and its rendered location. */
@Singleton
public final class MethodRecommendationValueService
{
    private final TravelAwareMethodValueService travel;
    private final MethodResourceValueService resources;

    @Inject
    public MethodRecommendationValueService(
            TravelAwareMethodValueService travel,
            MethodResourceValueService resources)
    {
        this.travel = travel == null
                ? new TravelAwareMethodValueService() : travel;
        this.resources = resources == null
                ? new MethodResourceValueService() : resources;
    }

    public MethodRecommendationValueService(
            TravelAwareMethodValueService travel)
    {
        this(travel, new MethodResourceValueService());
    }

    public MethodRecommendationValueService()
    {
        this(new TravelAwareMethodValueService(),
                new MethodResourceValueService());
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        TrainingPlan plan = recommendation == null
                ? null : recommendation.getTrainingPlan();
        TrainingMethod method = plan == null ? null : plan.getMethod();
        if (method == null || context == null) return recommendation;
        recommendation = resources.attach(recommendation, context);
        TravelAwareMethodAssessment assessment = travel.assess(method, context);
        if (assessment == null || assessment.getLocation() == null)
            return recommendation;

        StrategicValue value =
                recommendation.getStrategicValue().merge(
                        StrategicValue.builder()
                                .travelFit(assessment.getScoreAdjustment()
                                        / 6.0)
                                .evidence("travel:"
                                        + assessment.getLocation().getId())
                                .build());
        Recommendation result = recommendation.withStrategicValue(value);
        Guidance guidance = result.getGuidance();
        if (guidance == null) return result;
        return result.withGuidance(new Guidance(
                guidance.getAction(), guidance.getSupplies(),
                assessment.getLocation().getName() + ".",
                append(guidance.getNote(), assessment.getEvidence()),
                guidance.getBankingBehavior()));
    }

    private static String append(String first, String second)
    {
        if (first == null || first.trim().isEmpty()) return second;
        if (second == null || second.trim().isEmpty()) return first;
        return first + " " + second;
    }
}
