package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Exact, deterministic evidence that proves a destination route is usable. */
@RequiredArgsConstructor
@Getter
public final class TravelRouteEvidenceDefinition
{
    private final String routeId;
    private final String requiredCompletedQuest;
    private final List<String> requiredItems;


}
