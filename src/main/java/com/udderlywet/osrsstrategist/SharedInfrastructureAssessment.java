package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Explicit boundary for group capabilities RuneLite does not observe. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SharedInfrastructureAssessment
{
    private final CapabilityState state;
    private final Confidence confidence;
    private final String reason;


}
