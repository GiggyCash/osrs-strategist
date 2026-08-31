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
        for (StrategistFeature feature : StrategistFeature.values())
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
        StrategistEntitlementSnapshot unknown =
                StrategistEntitlementSnapshot.none();
        for (StrategistFeature feature : StrategistFeature.values())
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
        StrategistEntitlementSnapshot snapshot =
                new StrategistEntitlementSnapshot(
                        EnumSet.of(
                                StrategistFeature.CLOUD_PROFILE_SYNC,
                                StrategistFeature.CROSS_DEVICE_HISTORY),
                        Confidence.VERIFIED,
                        "test");

        assertTrue(policy.canUse(
                StrategistFeature.CLOUD_PROFILE_SYNC, snapshot));
        assertTrue(policy.canUse(
                StrategistFeature.CROSS_DEVICE_HISTORY, snapshot));
        assertFalse(policy.canUse(
                StrategistFeature.ONLINE_REASONING, snapshot));
        assertTrue(policy.canUse(
                StrategistFeature.CORE_PLANNER, snapshot));
    }

    @Test
    public void unverifiedSnapshotCannotUnlockHostedCapability()
    {
        StrategistEntitlementSnapshot snapshot =
                new StrategistEntitlementSnapshot(
                        Collections.singleton(
                                StrategistFeature.ONLINE_REASONING),
                        Confidence.CHECK_NEEDED,
                        "stale-cache");
        assertFalse(policy.canUse(
                StrategistFeature.ONLINE_REASONING, snapshot));
    }

    @Test(expected = StrategistFeatureAccessPolicy.HostedFeatureUnavailableException.class)
    public void requireHostedRejectsUnentitledFeature()
    {
        policy.requireHosted(
                StrategistFeature.GIM_TEAM_PLANNING,
                StrategistEntitlementSnapshot.none());
    }
}
