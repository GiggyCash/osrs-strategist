package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

@RequiredArgsConstructor
@Getter
public final class FarmingRunPatchDefinition
{
    private final String id;
    private final String displayName;
    private final FarmingPatchKind kind;
    private final int minimumLevel;
    private final Set<Integer> regionIds;
    private final int varbitId;
    private final String requiredQuest;



    public boolean matchesRegion(int regionId)
    {
        return regionIds.contains(regionId);
    }

    public String observationKey()
    {
        return "farm-run." + id;
    }
}
