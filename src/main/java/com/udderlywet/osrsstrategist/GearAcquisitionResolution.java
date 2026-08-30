package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Bounded cross-domain acquisition path, ordered from next action to target. */
public final class GearAcquisitionResolution
{
    @Getter
    private final String target;
    @Getter
    private final List<GearAcquisitionStep> steps;
    @Getter
    private final boolean cyclePrevented;
    @Getter
    private final boolean depthLimited;

    public GearAcquisitionResolution(String target,
            List<GearAcquisitionStep> steps, boolean cyclePrevented,
            boolean depthLimited)
    {
        this.target = target;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.cyclePrevented = cyclePrevented;
        this.depthLimited = depthLimited;
    }

}
