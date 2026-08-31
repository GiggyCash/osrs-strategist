package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Immutable entitlement evidence for optional hosted Compass services.
 *
 * <p>An unverified/empty snapshot never disables the local planner. Hosted
 * capabilities fail closed until their entitlement is verified.</p>
 */
public final class StrategistEntitlementSnapshot
{
    @Getter
    private final Set<StrategistFeature> hostedFeatures;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String source;

    public StrategistEntitlementSnapshot(
            Set<StrategistFeature> hostedFeatures,
            RecommendationConfidence confidence,
            String source)
    {
        EnumSet<StrategistFeature> copy = EnumSet.noneOf(StrategistFeature.class);
        if (hostedFeatures != null)
        {
            for (StrategistFeature feature : hostedFeatures)
            {
                if (feature != null && feature.isHostedPremium()) copy.add(feature);
            }
        }
        this.hostedFeatures = Collections.unmodifiableSet(copy);
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
        this.source = source == null ? "unknown" : source;
    }

    public static StrategistEntitlementSnapshot none()
    {
        return new StrategistEntitlementSnapshot(
                Collections.emptySet(),
                RecommendationConfidence.CHECK_NEEDED,
                "not-connected");
    }

    public static StrategistEntitlementSnapshot verifiedAllHosted()
    {
        EnumSet<StrategistFeature> all = EnumSet.noneOf(StrategistFeature.class);
        for (StrategistFeature feature : StrategistFeature.values())
        {
            if (feature.isHostedPremium()) all.add(feature);
        }
        return new StrategistEntitlementSnapshot(
                all, RecommendationConfidence.VERIFIED, "test/all-hosted");
    }

    public boolean hasHostedFeature(StrategistFeature feature)
    {
        return feature != null
                && feature.isHostedPremium()
                && confidence == RecommendationConfidence.VERIFIED
                && hostedFeatures.contains(feature);
    }



}
