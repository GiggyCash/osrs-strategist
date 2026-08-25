package com.udderlywet.osrsstrategist;

/** Independent overlay preferences; neither affects sidebar planning. */
final class OverlayDisplayState
{
    private final boolean details;
    private final boolean methodGuidance;

    OverlayDisplayState(boolean details, boolean methodGuidance)
    {
        this.details = details;
        this.methodGuidance = methodGuidance;
    }

    static OverlayDisplayState from(OsrsStrategistConfig config)
    {
        return new OverlayDisplayState(config != null
                && config.showDetailsOverlay(), config != null
                && config.showInGameGuidance());
    }

    boolean showsDetails() { return details; }
    boolean showsMethodGuidance() { return methodGuidance; }
}
