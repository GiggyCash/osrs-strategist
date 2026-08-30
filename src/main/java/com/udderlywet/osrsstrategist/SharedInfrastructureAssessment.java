package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Explicit boundary for group capabilities RuneLite does not observe. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SharedInfrastructureAssessment
{
    @Getter
    private final CapabilityState state;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String reason;


}
