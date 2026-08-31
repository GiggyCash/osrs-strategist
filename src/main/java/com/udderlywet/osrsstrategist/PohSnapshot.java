package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Player-owned-house capability snapshot.
 *
 * <p>POH planning is especially important for Ironman and UIM accounts, but
 * Compass must only recommend furniture/storage that is known to exist.
 * Furniture keys remain data-driven so new rooms and objects do not require a
 * rewrite of the strategy engine.</p>
 */
@Getter
public final class PohSnapshot
{
    private final CapabilityState houseAccess;
    private final Map<String, CapabilityState> furniture;

    public PohSnapshot(
            CapabilityState houseAccess,
            Map<String, CapabilityState> furniture)
    {
        this.houseAccess = houseAccess == null
                ? CapabilityState.UNKNOWN
                : houseAccess;
        this.furniture = Collections.unmodifiableMap(
                furniture == null
                        ? new HashMap<>()
                        : new HashMap<>(furniture)
        );
    }

    public static PohSnapshot unknown()
    {
        return new PohSnapshot(
                CapabilityState.UNKNOWN,
                Collections.emptyMap()
        );
    }


    public CapabilityState furnitureState(String furnitureId)
    {
        return furniture.getOrDefault(
                furnitureId,
                CapabilityState.UNKNOWN
        );
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PohSnapshot)) return false;
        PohSnapshot that = (PohSnapshot) other;
        return houseAccess == that.houseAccess
                && furniture.equals(that.furniture);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(houseAccess, furniture);
    }
}
