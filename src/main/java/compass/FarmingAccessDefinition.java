package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/**
 * Small piece of verified game-data describing a Farming patch group.
 *
 * <p>Region IDs are used only as positive observation evidence. Quest
 * requirements are used to infer access before the patch has ever been visited.</p>
 */
@RequiredArgsConstructor
@Getter
public final class FarmingAccessDefinition
{
    final String id;
    private final String displayName;
    private final Set<Integer> regionIds;
    private final String requiredQuest;
    private final boolean herbPatch;



    public String observationKey()
    {
        return "farming.patch." + id;
    }
}
