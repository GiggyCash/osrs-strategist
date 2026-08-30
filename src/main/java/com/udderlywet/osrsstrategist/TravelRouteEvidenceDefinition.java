package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Exact, deterministic evidence that proves a destination route is usable. */
public final class TravelRouteEvidenceDefinition
{
    @Getter
    private final String routeId;
    @Getter
    private final String requiredCompletedQuest;
    @Getter
    private final List<String> requiredItems;

    public TravelRouteEvidenceDefinition(String routeId,
            String requiredCompletedQuest, List<String> requiredItems)
    {
        this.routeId = routeId;
        this.requiredCompletedQuest = requiredCompletedQuest;
        this.requiredItems = Collections.unmodifiableList(requiredItems == null
                ? new ArrayList<>() : new ArrayList<>(requiredItems));
    }

}
