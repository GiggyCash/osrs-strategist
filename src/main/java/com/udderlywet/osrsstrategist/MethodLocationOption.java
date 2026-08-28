package com.udderlywet.osrsstrategist;

/**
 * One concrete place where a method can be executed.
 *
 * <p>The optional transport route is an advantage, not an assumed gate. A
 * caller may only describe that route as available when the live transport
 * snapshot contains the exact route key.</p>
 */
public final class MethodLocationOption
{
    private final String id;
    private final String name;
    private final int ordinaryTravelBurden;
    private final String advantageousRouteId;
    private final int verifiedRouteTravelBurden;
    private final boolean membersOnly;
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

    public String getId() { return id; }
    public String getName() { return name; }
    public int getOrdinaryTravelBurden() { return ordinaryTravelBurden; }
    public String getAdvantageousRouteId() { return advantageousRouteId; }
    public int getVerifiedRouteTravelBurden() { return verifiedRouteTravelBurden; }
    public boolean isMembersOnly() { return membersOnly; }
    public boolean isWilderness() { return wilderness; }

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
