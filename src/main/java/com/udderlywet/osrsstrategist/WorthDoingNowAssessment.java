package com.udderlywet.osrsstrategist;

/** Typed activity verdict plus its normalized net utility. */
public final class WorthDoingNowAssessment
{
    private final WorthDoingNowState state;
    private final double netUtility;
    private final String reason;

    WorthDoingNowAssessment(
            WorthDoingNowState state, double netUtility, String reason)
    {
        this.state = state;
        this.netUtility = netUtility;
        this.reason = reason == null ? "" : reason;
    }

    public WorthDoingNowState getState() { return state; }
    public double getNetUtility() { return netUtility; }
    public String getReason() { return reason; }
}
