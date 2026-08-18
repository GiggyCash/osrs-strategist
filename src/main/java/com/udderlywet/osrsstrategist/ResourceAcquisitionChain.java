package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered resource route from current ownership to the requested quantity. */
public final class ResourceAcquisitionChain
{
    private final ResourceNeed need;
    private final int shortfall;
    private final List<ResourceAcquisitionStep> steps;

    public ResourceAcquisitionChain(ResourceNeed need, int shortfall,
            List<ResourceAcquisitionStep> steps)
    {
        this.need = need;
        this.shortfall = Math.max(0, shortfall);
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public ResourceNeed getNeed() { return need; }
    public int getShortfall() { return shortfall; }
    public List<ResourceAcquisitionStep> getSteps() { return steps; }
    public ResourceAcquisitionStep nextStep()
    {
        return steps.isEmpty() ? null : steps.get(0);
    }
}
