package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Verified Slayer-master mechanics plus property-driven strategic qualities. */
public final class SlayerMasterProfile
{
    private final String id;
    private final List<String> names;
    private final String location;
    private final int minimumCombat;
    private final int minimumSlayer;
    private final String requiredQuest;
    private final boolean questStartSuffices;
    private final int normalPoints;
    private final int cancelCost;
    private final int blockCost;
    private final double experiencePotential;
    private final double supplyValue;
    private final double setupBurden;
    private final double locationConstraint;
    private final boolean wilderness;

    public SlayerMasterProfile(String id, List<String> names, String location,
            int minimumCombat, int minimumSlayer, String requiredQuest,
            boolean questStartSuffices, int normalPoints, int cancelCost,
            int blockCost, double experiencePotential,
            double supplyValue, double setupBurden, double locationConstraint,
            boolean wilderness)
    {
        this.id = id;
        this.names = Collections.unmodifiableList(new ArrayList<>(names));
        this.location = location;
        this.minimumCombat = Math.max(0, minimumCombat);
        this.minimumSlayer = Math.max(1, minimumSlayer);
        this.requiredQuest = requiredQuest;
        this.questStartSuffices = questStartSuffices;
        this.normalPoints = Math.max(0, normalPoints);
        this.cancelCost = Math.max(0, cancelCost);
        this.blockCost = Math.max(0, blockCost);
        this.experiencePotential = bounded(experiencePotential);
        this.supplyValue = bounded(supplyValue);
        this.setupBurden = bounded(setupBurden);
        this.locationConstraint = bounded(locationConstraint);
        this.wilderness = wilderness;
    }

    public String getId() { return id; }
    public List<String> getNames() { return names; }
    public String getDisplayName() { return names.get(0); }
    public String getLocation() { return location; }
    public int getMinimumCombat() { return minimumCombat; }
    public int getMinimumSlayer() { return minimumSlayer; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public int getNormalPoints() { return normalPoints; }
    public int getCancelCost() { return cancelCost; }
    public int getBlockCost() { return blockCost; }
    public double getExperiencePotential() { return experiencePotential; }
    public double getSupplyValue() { return supplyValue; }
    public double getSetupBurden() { return setupBurden; }
    public double getLocationConstraint() { return locationConstraint; }
    public boolean isWilderness() { return wilderness; }

    public int pointsForCompletion(int completedAfterTask)
    {
        return normalPoints * SlayerPointEconomy.pointMultiplier(completedAfterTask);
    }

    private static double bounded(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
