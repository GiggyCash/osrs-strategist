package com.udderlywet.osrsstrategist;

/**
 * Context that cannot safely be inferred from an item name. The encounter
 * model supplies benefit and goal relevance after validating them.
 */
public final class GearUpgradeValueRequest
{
    private final GearProgressionEntry progression;
    private final String targetItem;
    private final GearMarginalBenefit marginalBenefit;
    private final GearReplacementHorizon replacementHorizon;
    private final GearStorageDisposition storageDisposition;
    private final GearAcquisitionBurden acquisitionBurden;
    private final boolean provenGoalRelevant;

    public GearUpgradeValueRequest(GearProgressionEntry progression,
            String targetItem, GearMarginalBenefit marginalBenefit,
            GearReplacementHorizon replacementHorizon,
            GearStorageDisposition storageDisposition,
            GearAcquisitionBurden acquisitionBurden,
            boolean provenGoalRelevant)
    {
        this.progression = progression;
        this.targetItem = targetItem;
        this.marginalBenefit = marginalBenefit == null
                ? GearMarginalBenefit.UNKNOWN : marginalBenefit;
        this.replacementHorizon = replacementHorizon == null
                ? GearReplacementHorizon.UNKNOWN : replacementHorizon;
        this.storageDisposition = storageDisposition == null
                ? GearStorageDisposition.UNKNOWN : storageDisposition;
        this.acquisitionBurden = acquisitionBurden == null
                ? GearAcquisitionBurden.UNKNOWN : acquisitionBurden;
        this.provenGoalRelevant = provenGoalRelevant;
    }

    public GearProgressionEntry getProgression() { return progression; }
    public String getTargetItem() { return targetItem; }
    public GearMarginalBenefit getMarginalBenefit() { return marginalBenefit; }
    public GearReplacementHorizon getReplacementHorizon() { return replacementHorizon; }
    public GearStorageDisposition getStorageDisposition() { return storageDisposition; }
    public GearAcquisitionBurden getAcquisitionBurden() { return acquisitionBurden; }
    public boolean isProvenGoalRelevant() { return provenGoalRelevant; }
}
