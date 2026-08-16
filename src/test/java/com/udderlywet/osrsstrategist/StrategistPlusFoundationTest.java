package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrategistPlusFoundationTest
{
    @Test
    public void freeEditionAlwaysRetainsCompleteLocalCore()
    {
        EntitlementSnapshot snapshot = EntitlementSnapshot.freeLocal();
        assertTrue(snapshot.has(StrategistFeature.CORE_PLANNER));
        assertTrue(snapshot.has(StrategistFeature.LOCAL_PROFILE_MEMORY));
        assertTrue(snapshot.has(StrategistFeature.LOCAL_METHOD_GUIDANCE));
        assertFalse(snapshot.has(StrategistFeature.CLOUD_PROFILE_SYNC));
    }

    @Test
    public void remoteGatewayCannotTransmitInCurrentBuildEvenWithEntitlement()
    {
        StrategistEntitlementService entitlements =
                new StrategistEntitlementService();
        entitlements.replaceFromTrustedProvider(new EntitlementSnapshot(
                StrategistEdition.PLUS,
                EnumSet.of(StrategistFeature.CLOUD_PROFILE_SYNC)
        ));

        StrategistRemoteGateway gateway =
                new StrategistRemoteGateway(entitlements);

        assertFalse(gateway.isConfigured());
        assertFalse(gateway.canTransmit());
        assertFalse(gateway.sync(new PlusSyncEnvelope(
                "local-test-token",
                1L,
                EnumSet.of(PlusDataCategory.PREFERENCES)
        )).isAccepted());
    }
}
