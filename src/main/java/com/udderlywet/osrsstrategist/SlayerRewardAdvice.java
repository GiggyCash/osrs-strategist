package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One live-evidence-backed Slayer reward purchase decision. */
@Getter
@RequiredArgsConstructor
public final class SlayerRewardAdvice
{
    private final SlayerReward reward;
    private final double score;
    private final String reason;


}
