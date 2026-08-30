package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selected method plus the confidence after evaluating the current account.
 *
 * <p>The static TrainingMethod definition describes the method in general;
 * this object describes whether that method is actually verified for this
 * character's current state and why.</p>
 */
public final class TrainingPlan
{
    private final TrainingMethod method;
    private final String whyThisMethod;
    private final RecommendationConfidence confidence;
    private final List<RequirementCheck> requirementChecks;
    private final MethodStrategyProfile strategyProfile;

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod)
    {
        this(
                method,
                whyThisMethod,
                method == null
                        ? RecommendationConfidence.CHECK_NEEDED
                        : method.getConfidence(),
                Collections.emptyList(),
                null
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            RecommendationConfidence confidence)
    {
        this(
                method,
                whyThisMethod,
                confidence,
                Collections.emptyList(),
                null
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            RecommendationConfidence confidence,
            List<RequirementCheck> requirementChecks)
    {
        this(method, whyThisMethod, confidence, requirementChecks, null);
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            RecommendationConfidence confidence,
            List<RequirementCheck> requirementChecks,
            MethodStrategyProfile strategyProfile)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
        this.requirementChecks = Collections.unmodifiableList(
                requirementChecks == null
                        ? new ArrayList<>()
                        : new ArrayList<>(requirementChecks)
        );
        this.strategyProfile = strategyProfile;
    }

    public TrainingMethod getMethod()
    {
        return method;
    }

    public String getWhyThisMethod()
    {
        return whyThisMethod;
    }

    public RecommendationConfidence getConfidence()
    {
        return confidence;
    }

    public List<RequirementCheck> getRequirementChecks()
    {
        return requirementChecks;
    }

    public MethodStrategyProfile getStrategyProfile()
    {
        return strategyProfile;
    }
}
