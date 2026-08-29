package com.udderlywet.osrsstrategist;

/** One live-evidence-backed Slayer reward purchase decision. */
public final class SlayerRewardAdvice
{
    private final SlayerReward reward;
    private final double score;
    private final String reason;

    public SlayerRewardAdvice(SlayerReward reward, double score, String reason)
    {
        this.reward = reward;
        this.score = score;
        this.reason = reason;
    }

    public SlayerReward getReward() { return reward; }
    public double getScore() { return score; }
    public String getReason() { return reason; }
}
