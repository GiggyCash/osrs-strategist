package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Live account assessment of one sourced method profile. */
@RequiredArgsConstructor
public final class MethodStrategyAssessment
{
    @Getter
    private final boolean viable;
    @Getter
    private final double scoreAdjustment;
    @Getter
    private final String explanation;


}
