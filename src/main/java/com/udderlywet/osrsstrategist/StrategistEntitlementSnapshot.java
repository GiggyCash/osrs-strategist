package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Immutable entitlement evidence for optional hosted Compass services.
 *
 * <p>An unverified/empty snapshot never disables the local planner. Hosted
 * capabilities fail closed until their entitlement is verified.</p>
 */
@Getter
public final class StrategistEntitlementSnapshot
{
    private final Set<StrategistFeature> hostedFeatures;
    private final Confidence confidence;
    private final String source;

    public StrategistEntitlementSnapshot(
            Set<StrategistFeature> hostedFeatures,
            Confidence confidence,
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
                ? Confidence.CHECK_NEEDED : confidence;
        this.source = source == null ? "unknown" : source;
    }

    public static StrategistEntitlementSnapshot none()
    {
        return new StrategistEntitlementSnapshot(
                Collections.emptySet(),
                Confidence.CHECK_NEEDED,
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
                all, Confidence.VERIFIED, "test/all-hosted");
    }

    public boolean hasHostedFeature(StrategistFeature feature)
    {
        return feature != null
                && feature.isHostedPremium()
                && confidence == Confidence.VERIFIED
                && hostedFeatures.contains(feature);
    }



}
