package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded cross-domain acquisition path, ordered from next action to target. */
public final class GearAcquisitionResolution
{
    private final String target;
    private final List<GearAcquisitionStep> steps;
    private final boolean cyclePrevented;
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

    public String getTarget() { return target; }
    public List<GearAcquisitionStep> getSteps() { return steps; }
    public boolean isCyclePrevented() { return cyclePrevented; }
    public boolean isDepthLimited() { return depthLimited; }
}
