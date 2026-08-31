package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Property-driven value of one exact upgrade in one supplied context. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ContextualGearValueAssessment
{
    private final GearUpgradeValueState state;
    private final int scoreAdjustment;
    private final GearAcquisitionRoute acquisitionRoute;
    private final String evidence;


}
