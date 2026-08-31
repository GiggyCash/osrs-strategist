package compass;

import java.util.*;

import lombok.Getter;

/**
 * Selected method plus the confidence after evaluating the current account.
 *
 * <p>The static TrainingMethod definition describes the method in general;
 * this object describes whether that method is actually verified for this
 * character's current state and why.</p>
 */
public final class TrainingPlan
{
    @Getter
    private final TrainingMethod method;
    @Getter
    private final String whyThisMethod;
    @Getter
    private final Confidence confidence;
    @Getter
    private final List<RequirementCheck> requirementChecks;
    @Getter
    private final MethodStrategyProfile strategyProfile;
    private final int currentStageTargetLevel;

    TrainingMethod method() { return method; }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod)
    {
        this(
                method,
                whyThisMethod,
                method == null
                        ? Confidence.CHECK_NEEDED
                        : method.getConfidence(),
                Collections.emptyList(),
                null
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence)
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
            Confidence confidence,
            List<RequirementCheck> requirementChecks)
    {
        this(method, whyThisMethod, confidence, requirementChecks, null);
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<RequirementCheck> requirementChecks,
            MethodStrategyProfile strategyProfile)
    {
        this(method, whyThisMethod, confidence, requirementChecks,
                strategyProfile, 0);
    }

    private TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<RequirementCheck> requirementChecks,
            MethodStrategyProfile strategyProfile,
            int currentStageTargetLevel)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.requirementChecks = Collections.unmodifiableList(
                requirementChecks == null
                        ? new ArrayList<>()
                        : new ArrayList<>(requirementChecks)
        );
        this.strategyProfile = strategyProfile;
        this.currentStageTargetLevel = Math.max(0, currentStageTargetLevel);
    }






    /**
     * The next level at which the visible execution plan must be rebuilt. This
     * is deliberately separate from the recommendation's distant objective.
     */
    public int getCurrentStageTargetLevel()
    {
        return currentStageTargetLevel;
    }

    public TrainingPlan withCurrentStageTargetLevel(int targetLevel)
    {
        return new TrainingPlan(method, whyThisMethod, confidence,
                requirementChecks, strategyProfile, targetLevel);
    }
}
