package com.udderlywet.osrsstrategist;

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
        if (profile == null || context == null || context.getData() == null
                || context.getData().getAccount() == null)
        {
            return null;
        }
        AccountSnapshot account = context.getData().getAccount();
        MethodLocationOption selected = profile.getLocations().stream()
                .filter(option -> !option.isMembersOnly()
                        || account.getMembershipStatus() == MembershipStatus.P2P)
                .filter(option -> !option.isWilderness()
                        || context.isAllowWildernessMethods())
                .min(Comparator.comparingInt(option -> option.effectiveBurden(
                        routeEvidence.verified(
                                option.getAdvantageousRouteId(), context))))
                .orElse(null);
        if (selected == null) return null;

        boolean routed = routeEvidence.verified(
                selected.getAdvantageousRouteId(), context);
        int burden = selected.effectiveBurden(routed);
        // Travel can refine close method choices but cannot overpower legality,
        // goal provenance, or readiness. The value is intentionally bounded.
        int adjustment = Math.max(-6, Math.min(4, 3 - burden));
        String evidence = routed
                ? "Verified route " + selected.getAdvantageousRouteId()
                        + " lowers travel/setup burden for "
                        + selected.getName() + "."
                : "No exact advantageous route is verified; use ordinary access to "
                        + selected.getName() + ".";
        return new TravelAwareMethodAssessment(selected, burden, adjustment,
                routed, evidence);
    }
}
