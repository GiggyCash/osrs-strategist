package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/** Independent overlay preferences; neither affects sidebar planning. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class OverlayDisplayState
{
    private final boolean details;
    private final boolean methodGuidance;


    static OverlayDisplayState from(OsrsStrategistConfig config)
    {
        return new OverlayDisplayState(config != null
                && config.showDetailsOverlay(), config != null
                && config.showInGameGuidance());
    }

    boolean showsDetails() { return details; }
    boolean showsMethodGuidance() { return methodGuidance; }
    boolean showsMethodGuidance(boolean detailsVisible)
    {
        return methodGuidance && !detailsVisible;
    }
}
