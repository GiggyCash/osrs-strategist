package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ContextualGearDecision
{
    @Getter
    private final GearDecisionKind kind;
    @Getter
    private final String value;
    @Getter
    private final RecommendationConfidence confidence;


}
