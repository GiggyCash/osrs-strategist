package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Typed activity verdict plus its normalized net utility. */
public final class WorthDoingNowAssessment
{
    @Getter
    private final WorthDoingNowState state;
    @Getter
    private final double netUtility;
    @Getter
    private final String reason;

    WorthDoingNowAssessment(
            WorthDoingNowState state, double netUtility, String reason)
    {
        this.state = state;
        this.netUtility = netUtility;
        this.reason = reason == null ? "" : reason;
    }

}
