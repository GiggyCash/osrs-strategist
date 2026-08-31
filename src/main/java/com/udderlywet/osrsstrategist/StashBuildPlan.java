package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
public final class StashBuildPlan
{
    private final StashUnitDefinition unit;
    private final List<StashDependencyStep> steps;

    StashBuildPlan(StashUnitDefinition unit, List<StashDependencyStep> steps)
    {
        this.unit = unit;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public StashDependencyStep nextAction()
    {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }
}
