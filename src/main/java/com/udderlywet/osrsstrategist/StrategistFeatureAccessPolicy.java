package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Single access boundary for optional hosted/premium capabilities. */
@Singleton
public class StrategistFeatureAccessPolicy
{
    public boolean canUse(
            StrategistFeature feature,
            StrategistEntitlementSnapshot entitlements)
    {
        if (feature == null) return false;
        if (feature.isCoreLocal()) return true;
        return entitlements != null && entitlements.hasHostedFeature(feature);
    }

    public void requireHosted(
            StrategistFeature feature,
            StrategistEntitlementSnapshot entitlements)
    {
        if (feature == null || feature.isCoreLocal())
        {
            throw new IllegalArgumentException(
                    "requireHosted may only guard hosted features");
        }
        if (!canUse(feature, entitlements))
        {
            throw new HostedFeatureUnavailableException(feature);
        }
    }

    public static final class HostedFeatureUnavailableException
            extends IllegalStateException
    {
        private final StrategistFeature feature;

        HostedFeatureUnavailableException(StrategistFeature feature)
        {
            super("Hosted Compass feature is not entitled: " + feature);
            this.feature = feature;
        }

        public StrategistFeature getFeature()
        {
            return feature;
        }
    }
}
