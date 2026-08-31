package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Live account assessment of one sourced method profile. */
@Getter
@RequiredArgsConstructor
public final class MethodStrategyAssessment
{
    private final boolean viable;
    private final double scoreAdjustment;
    private final String explanation;


}
