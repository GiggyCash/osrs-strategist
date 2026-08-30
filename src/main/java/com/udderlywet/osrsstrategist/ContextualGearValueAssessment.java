package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Property-driven value of one exact upgrade in one supplied context. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ContextualGearValueAssessment
{
    @Getter
    private final GearUpgradeValueState state;
    @Getter
    private final int scoreAdjustment;
    @Getter
    private final GearAcquisitionRoute acquisitionRoute;
    @Getter
    private final String evidence;


}
