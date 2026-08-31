package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Live, per-character ownership evidence for reviewed Slayer rewards. */
public final class SlayerRewardSnapshot
{
    @Getter
    private final Map<SlayerReward, CapabilityState> states;

    public SlayerRewardSnapshot(Map<SlayerReward, CapabilityState> states)
    {
        EnumMap<SlayerReward, CapabilityState> copy =
                new EnumMap<>(SlayerReward.class);
        if (states != null) copy.putAll(states);
        this.states = Collections.unmodifiableMap(copy);
    }

    public static SlayerRewardSnapshot unknown()
    {
        return new SlayerRewardSnapshot(Collections.emptyMap());
    }

    public CapabilityState stateOf(SlayerReward reward)
    {
        return reward == null ? CapabilityState.UNKNOWN
                : states.getOrDefault(reward, CapabilityState.UNKNOWN);
    }

    public boolean isUnlocked(SlayerReward reward)
    {
        return stateOf(reward) == CapabilityState.VERIFIED;
    }

}
