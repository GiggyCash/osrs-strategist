package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ContextualGearDecision
{
    private final GearDecisionKind kind;
    private final String value;
    private final Confidence confidence;


}
