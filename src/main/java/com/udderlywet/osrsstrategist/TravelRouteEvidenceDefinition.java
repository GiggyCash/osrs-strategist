package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact, deterministic evidence that proves a destination route is usable. */
public final class TravelRouteEvidenceDefinition
{
    private final String routeId;
    private final String requiredCompletedQuest;
    private final List<String> requiredItems;

    public TravelRouteEvidenceDefinition(String routeId,
            String requiredCompletedQuest, List<String> requiredItems)
    {
        this.routeId = routeId;
        this.requiredCompletedQuest = requiredCompletedQuest;
        this.requiredItems = Collections.unmodifiableList(requiredItems == null
                ? new ArrayList<>() : new ArrayList<>(requiredItems));
    }

    public String getRouteId() { return routeId; }
    public String getRequiredCompletedQuest() { return requiredCompletedQuest; }
    public List<String> getRequiredItems() { return requiredItems; }
}
