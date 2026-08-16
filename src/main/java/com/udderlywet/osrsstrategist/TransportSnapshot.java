package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Transport and teleport options that the plugin has actually verified.
 *
 * <p>This deliberately stores opaque route keys rather than hard-coding the
 * transport network into Java. Future game-data files can define what each
 * key means and which activities require it.</p>
 */
public final class TransportSnapshot
{
    private final Set<String> verifiedRoutes;

    public TransportSnapshot(Set<String> verifiedRoutes)
    {
        this.verifiedRoutes = Collections.unmodifiableSet(
                verifiedRoutes == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedRoutes)
        );
    }

    public static TransportSnapshot unknown()
    {
        return new TransportSnapshot(Collections.emptySet());
    }

    public boolean hasVerifiedRoute(String routeId)
    {
        return routeId != null && verifiedRoutes.contains(routeId);
    }

    public Set<String> getVerifiedRoutes()
    {
        return verifiedRoutes;
    }
}
