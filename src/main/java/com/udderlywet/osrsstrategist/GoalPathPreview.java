package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Read-only preview of the dependency groups behind a selected big goal. */
@Getter
public final class GoalPathPreview
{
    private final GoalType goal;
    private final List<GoalDependency> dependencies;

    public GoalPathPreview(
            GoalType goal,
            List<GoalDependency> dependencies)
    {
        this.goal = goal;
        this.dependencies = Collections.unmodifiableList(
                dependencies == null
                        ? new ArrayList<>()
                        : new ArrayList<>(dependencies)
        );
    }

}
