package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Travel evidence and bounded value for a selected concrete method location. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TravelAwareMethodAssessment
{
    @Getter
    private final MethodLocationOption location;
    @Getter
    private final int travelBurden;
    @Getter
    private final int scoreAdjustment;
    @Getter
    private final boolean verifiedRouteUsed;
    @Getter
    private final String evidence;


}
