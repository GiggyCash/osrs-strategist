package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

public final class StashBuildPlan
{
    @Getter
    private final StashUnitDefinition unit;
    @Getter
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
