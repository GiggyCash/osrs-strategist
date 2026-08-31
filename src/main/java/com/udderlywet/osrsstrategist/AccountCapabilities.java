package com.udderlywet.osrsstrategist;

import java.util.*;

/**
 * Stores only capabilities the plugin can verify or the player has confirmed.
 * Unknown is a first-class state. The strategist should never assume a storage
 * method or unlock exists just because that method exists in the game.
 */
public final class AccountCapabilities
{
    private final Map<String, CapabilityState> states = new HashMap<>();

    public CapabilityState get(String key)
    {
        return states.getOrDefault(key, CapabilityState.UNKNOWN);
    }

    public void set(String key, CapabilityState state)
    {
        states.put(key, state);
    }

    public boolean verified(String key)
    {
        return get(key) == CapabilityState.VERIFIED;
    }
}
