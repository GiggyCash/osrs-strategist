package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One live-evidence-backed Slayer reward purchase decision. */
@RequiredArgsConstructor
public final class SlayerRewardAdvice
{
    @Getter
    private final SlayerReward reward;
    @Getter
    private final double score;
    @Getter
    private final String reason;


}
