package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Verified Slayer-master mechanics plus property-driven strategic qualities. */
@RequiredArgsConstructor
public final class SlayerMasterProfile
{
    @Getter
    final String id;
    @Getter
    private final List<String> names;
    @Getter
    private final String location;
    @Getter
    private final int minimumCombat;
    @Getter
    private final int minimumSlayer;
    @Getter
    private final String requiredQuest;
    private final boolean questStartSuffices;
    @Getter
    private final int normalPoints;
    @Getter
    private final int cancelCost;
    @Getter
    private final int blockCost;
    @Getter
    private final double experiencePotential;
    @Getter
    private final double supplyValue;
    @Getter
    private final double setupBurden;
    @Getter
    private final double locationConstraint;
    @Getter
    private final boolean wilderness;


    public String getDisplayName() { return names.get(0); }
    public boolean isQuestStartSufficient() { return questStartSuffices; }

    public int pointsForCompletion(int completedAfterTask)
    {
        return normalPoints * SlayerPointEconomy.pointMultiplier(completedAfterTask);
    }

}
