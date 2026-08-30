package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Context that cannot safely be inferred from an item name. The encounter
 * model supplies benefit and goal relevance after validating them.
 */
public final class GearUpgradeValueRequest
{
    @Getter
    private final GearProgressionEntry progression;
    @Getter
    private final String targetItem;
    @Getter
    private final GearMarginalBenefit marginalBenefit;
    @Getter
    private final GearReplacementHorizon replacementHorizon;
    @Getter
    private final GearStorageDisposition storageDisposition;
    @Getter
    private final GearAcquisitionBurden acquisitionBurden;
    @Getter
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

}
