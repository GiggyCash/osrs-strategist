package compass;

import javax.inject.Singleton;

/**
 * Hard gate for the primary DO NEXT slot and quality gate for alternatives.
 *
 * <p>The primary card must always contain an executable next action. A fully
 * verified route can lead immediately. A route with only ordinary preparation
 * outstanding may also lead when its guidance explains that preparation. True
 * unknown access, quest, build, or unlock requirements can never lead.</p>
 */
@Singleton
public class ActionabilityPolicy
{
    private final RecommendationQualityPolicy qualityPolicy =
            new RecommendationQualityPolicy();

    public boolean canLeadQueue(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() == Confidence.BLOCKED)
        {
            return false;
        }

        var plan = recommendation.plan();
        var guidance = recommendation.getGuidance();
        if (!qualityPolicy.isPresentable(recommendation)) return false;

        if (plan == null)
        {
            // Non-skill candidates must be fully verified and structured before
            // they can displace a concrete training action. The one exception
            // is an explicitly typed preparation/verification action whose
            // remaining work is fully described by the quality contract.
            return (recommendation.getConfidence()
                        == Confidence.VERIFIED
                    || (recommendation.getConfidence()
                        == Confidence.CHECK_NEEDED
                        && isExplicitPreparation(recommendation)))
                    && guidance != null && hasText(guidance.getAction());
        }

        if (plan.method() == null || guidance == null
                || !hasText(guidance.getAction()))
        {
            return false;
        }

        if (recommendation.getConfidence() == Confidence.VERIFIED)
        {
            return !RequirementActionability.hasHardUnresolvedRequirement(plan);
        }

        // CHECK_NEEDED is allowed only when every unresolved check is ordinary
        // preparation and the guidance explicitly covers supplies/setup.
        return RequirementActionability.isActionablePreparation(plan, guidance);
    }

    public boolean mayAppearAsAlternative(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() == Confidence.BLOCKED)
        {
            return false;
        }
        if (canLeadQueue(recommendation)) return true;

        if (!qualityPolicy.isPresentable(recommendation)) return false;

        var guidance = recommendation.getGuidance();
        var plan = recommendation.plan();

        // A secondary card still needs to tell the player something useful.
        // Bare "Needs Info" quest/upgrade placeholders are hidden until their
        // provider can produce a concrete next verification/preparation step.
        if (plan == null)
        {
            return guidance != null && hasText(guidance.getAction())
                    && (recommendation.getConfidence()
                            == Confidence.VERIFIED
                        || isExplicitPreparation(recommendation));
        }

        return plan.method() != null
                && guidance != null
                && hasText(guidance.getAction())
                && !RequirementActionability.hasHardUnresolvedRequirement(plan);
    }

    public int queuePriority(Recommendation recommendation)
    {
        if (canLeadQueue(recommendation)) return 2;
        if (mayAppearAsAlternative(recommendation)) return 1;
        return 0;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isExplicitPreparation(Recommendation recommendation)
    {
        String id = recommendation == null || recommendation.getId() == null
                ? "" : recommendation.getId().toLowerCase(
                        java.util.Locale.ROOT);
        return id.startsWith("prepare:")
                || id.startsWith("preparation:")
                || id.startsWith("verify:");
    }
}
