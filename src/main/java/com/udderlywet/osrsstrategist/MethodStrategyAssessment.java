package com.udderlywet.osrsstrategist;

/** Live account assessment of one sourced method profile. */
public final class MethodStrategyAssessment
{
    private final boolean viable;
    private final double scoreAdjustment;
    private final String explanation;

    public MethodStrategyAssessment(boolean viable, double scoreAdjustment,
            String explanation)
    {
        this.viable = viable;
        this.scoreAdjustment = scoreAdjustment;
        this.explanation = explanation;
    }

    public boolean isViable() { return viable; }
    public double getScoreAdjustment() { return scoreAdjustment; }
    public String getExplanation() { return explanation; }
}
