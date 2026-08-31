package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Safe acquisition recommendation for one resource requirement.
 *
 * <p>This never performs a purchase, sale, drop, withdrawal, or other gameplay
 * action. It only tells the strategy engine where a resource appears to be or
 * which sourcing family should be evaluated next.</p>
 */
@Getter
public final class ResourceAcquisitionPlan
{
    private final ResourceNeed need;
    private final AcquisitionSource source;
    private final int confirmedQuantity;
    private final RecommendationConfidence confidence;
    private final String note;

    public ResourceAcquisitionPlan(
            ResourceNeed need,
            AcquisitionSource source,
            int confirmedQuantity,
            RecommendationConfidence confidence,
            String note)
    {
        this.need = need;
        this.source = source;
        this.confirmedQuantity = Math.max(0, confirmedQuantity);
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED
                : confidence;
        this.note = note;
    }


    public boolean hasEnoughConfirmed()
    {
        return need != null
                && confirmedQuantity >= need.getQuantity();
    }
}
