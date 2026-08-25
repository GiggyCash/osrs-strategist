package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StashBuildPlan
{
    private final StashUnitDefinition unit;
    private final List<StashDependencyStep> steps;

    StashBuildPlan(StashUnitDefinition unit, List<StashDependencyStep> steps)
    {
        this.unit = unit;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public StashUnitDefinition getUnit() { return unit; }
    public List<StashDependencyStep> getSteps() { return steps; }
    public StashDependencyStep nextAction()
    {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }
}
