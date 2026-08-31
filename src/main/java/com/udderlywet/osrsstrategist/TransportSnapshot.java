package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Transport and teleport options that the plugin has actually verified.
 *
 * <p>This deliberately stores opaque route keys rather than hard-coding the
 * transport network into Java. Future game-data files can define what each
 * key means and which activities require it.</p>
 */
public final class TransportSnapshot
{
    @Getter
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

}
