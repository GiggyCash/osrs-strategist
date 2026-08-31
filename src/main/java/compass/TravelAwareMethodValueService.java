package compass;

import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Selects a legal concrete method location from observed travel properties. */
@Singleton
public final class TravelAwareMethodValueService
{
    private final MethodLocationCatalog catalog;
    private final TravelRouteEvidenceService routeEvidence;

    @Inject
    public TravelAwareMethodValueService(MethodLocationCatalog catalog,
            TravelRouteEvidenceService routeEvidence)
    {
        this.catalog = catalog;
        this.routeEvidence = routeEvidence;
    }

    public TravelAwareMethodValueService()
    {
        this(new MethodLocationCatalog(), new TravelRouteEvidenceService());
    }

    public TravelAwareMethodAssessment assess(TrainingMethod method,
            StrategyContext context)
    {
        MethodLocationProfile profile = method == null ? null
                : catalog.forMethod(method.getId());
        return assess(profile, context);
    }

    public TravelAwareMethodAssessment assess(MethodLocationProfile profile,
            StrategyContext context)
    {
        if (profile == null || context == null || context.data() == null
                || context.data().account() == null)
        {
            return null;
        }
        var account = context.data().account();
        MethodLocationOption selected = profile.getLocations().stream()
                .filter(option -> !option.isMembersOnly()
                        || account.membership() == MembershipStatus.P2P)
                .filter(option -> !option.isWilderness()
                        || context.allowsWilderness())
                .min(Comparator.comparingInt(option -> option.effectiveBurden(
                        routeEvidence.verified(
                                option.getAdvantageousRouteId(), context))))
                .orElse(null);
        if (selected == null) return null;

        boolean routed = routeEvidence.verified(
                selected.getAdvantageousRouteId(), context);
        var burden = selected.effectiveBurden(routed);
        // Travel can refine close method choices but cannot overpower legality,
        // goal provenance, or readiness. The value is intentionally bounded.
        var adjustment = Math.max(-6, Math.min(4, 3 - burden));
        String evidence = routed
                ? "Verified route " + selected.getAdvantageousRouteId()
                        + Text.get(1297)
                        + selected.getName() + "."
                : Text.get(895)
                        + selected.getName() + ".";
        return new TravelAwareMethodAssessment(selected, burden, adjustment,
                routed, evidence);
    }
}
