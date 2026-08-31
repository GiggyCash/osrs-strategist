package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/**
 * One concrete place where a method can be executed.
 *
 * <p>The optional transport route is an advantage, not an assumed gate. A
 * caller may only describe that route as available when the live transport
 * snapshot contains the exact route key.</p>
 */
@RequiredArgsConstructor
@Getter
public final class MethodLocationOption
{
    private final String id;
    private final String name;
    private final int ordinaryTravelBurden;
    private final String advantageousRouteId;
    private final int verifiedRouteTravelBurden;
    private final boolean membersOnly;
    private final boolean wilderness;



    int effectiveBurden(TransportSnapshot transport)
    {
        return advantageousRouteId != null && transport != null
                && transport.hasVerifiedRoute(advantageousRouteId)
                ? verifiedRouteTravelBurden : ordinaryTravelBurden;
    }

    int effectiveBurden(boolean verifiedRoute)
    {
        return advantageousRouteId != null && verifiedRoute
                ? verifiedRouteTravelBurden : ordinaryTravelBurden;
    }

    boolean usesVerifiedRoute(TransportSnapshot transport)
    {
        return advantageousRouteId != null && transport != null
                && transport.hasVerifiedRoute(advantageousRouteId);
    }

    private static int bounded(int burden)
    {
        return Math.max(0, Math.min(10, burden));
    }
}
