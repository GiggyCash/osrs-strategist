package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DiarySnapshot
{
    private final Map<String, Integer> completedTasksByRegion;
    private final Map<String, Integer> totalTasksByRegion;

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion)
    {
        this.completedTasksByRegion = Collections.unmodifiableMap(
                new HashMap<>(completedTasksByRegion)
        );
        this.totalTasksByRegion = Collections.unmodifiableMap(
                new HashMap<>(totalTasksByRegion)
        );
    }

    public int completedIn(String region)
    {
        return completedTasksByRegion.getOrDefault(region, 0);
    }

    public int totalIn(String region)
    {
        return totalTasksByRegion.getOrDefault(region, 0);
    }
}
