package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * One concrete place where a method can be executed.
 *
 * <p>The optional transport route is an advantage, not an assumed gate. A
 * caller may only describe that route as available when the live transport
 * snapshot contains the exact route key.</p>
 */
public final class MethodLocationOption
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final int ordinaryTravelBurden;
    @Getter
    private final String advantageousRouteId;
    @Getter
    private final int verifiedRouteTravelBurden;
    @Getter
    private final boolean membersOnly;
    @Getter
    private final boolean wilderness;

    public MethodLocationOption(String id, String name,
            int ordinaryTravelBurden, String advantageousRouteId,
            int verifiedRouteTravelBurden, boolean membersOnly,
            boolean wilderness)
    {
        this.id = id;
        this.name = name;
        this.ordinaryTravelBurden = bounded(ordinaryTravelBurden);
        this.advantageousRouteId = advantageousRouteId;
        this.verifiedRouteTravelBurden = bounded(verifiedRouteTravelBurden);
        this.membersOnly = membersOnly;
        this.wilderness = wilderness;
    }


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
