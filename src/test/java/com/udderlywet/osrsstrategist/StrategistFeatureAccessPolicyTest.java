package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrategistFeatureAccessPolicyTest
{
    private final StrategistFeatureAccessPolicy policy =
            new StrategistFeatureAccessPolicy();

    @Test
    public void localSafetyAndPlannerRemainAvailableWithoutEntitlementService()
    {
        for (StrategistFeatureAccessPolicy.Feature feature : StrategistFeatureAccessPolicy.Feature.values())
        {
            if (feature.isCoreLocal())
            {
                assertTrue(feature + " must remain local/free",
                        policy.canUse(feature, null));
            }
        }
    }

    @Test
    public void hostedFeaturesFailClosedWithoutVerifiedEvidence()
    {
        StrategistFeatureAccessPolicy.Snapshot unknown =
                StrategistFeatureAccessPolicy.Snapshot.none();
        for (StrategistFeatureAccessPolicy.Feature feature : StrategistFeatureAccessPolicy.Feature.values())
        {
            if (feature.isHostedPremium())
            {
                assertFalse(feature + " must fail closed",
                        policy.canUse(feature, unknown));
            }
        }
    }

    @Test
    public void verifiedSnapshotOnlyGrantsExplicitHostedFeatures()
    {
        StrategistFeatureAccessPolicy.Snapshot snapshot =
                new StrategistFeatureAccessPolicy.Snapshot(
                        EnumSet.of(
                                StrategistFeatureAccessPolicy.Feature.CLOUD_PROFILE_SYNC,
                                StrategistFeatureAccessPolicy.Feature.CROSS_DEVICE_HISTORY),
                        Confidence.VERIFIED,
                        "test");

        assertTrue(policy.canUse(
                StrategistFeatureAccessPolicy.Feature.CLOUD_PROFILE_SYNC, snapshot));
        assertTrue(policy.canUse(
                StrategistFeatureAccessPolicy.Feature.CROSS_DEVICE_HISTORY, snapshot));
        assertFalse(policy.canUse(
                StrategistFeatureAccessPolicy.Feature.ONLINE_REASONING, snapshot));
        assertTrue(policy.canUse(
                StrategistFeatureAccessPolicy.Feature.CORE_PLANNER, snapshot));
    }

    @Test
    public void unverifiedSnapshotCannotUnlockHostedCapability()
    {
        StrategistFeatureAccessPolicy.Snapshot snapshot =
                new StrategistFeatureAccessPolicy.Snapshot(
                        Collections.singleton(
                                StrategistFeatureAccessPolicy.Feature.ONLINE_REASONING),
                        Confidence.CHECK_NEEDED,
                        "stale-cache");
        assertFalse(policy.canUse(
                StrategistFeatureAccessPolicy.Feature.ONLINE_REASONING, snapshot));
    }

    @Test(expected = StrategistFeatureAccessPolicy.HostedFeatureUnavailableException.class)
    public void requireHostedRejectsUnentitledFeature()
    {
        policy.requireHosted(
                StrategistFeatureAccessPolicy.Feature.GIM_TEAM_PLANNING,
                StrategistFeatureAccessPolicy.Snapshot.none());
    }
}
