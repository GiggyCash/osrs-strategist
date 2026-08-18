package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Dormant remote-service seam for a possible Compass Plus product.
 *
 * <p>There is deliberately no HTTP client, endpoint, account system, telemetry,
 * or billing integration here. The gateway refuses remote operations until a
 * future reviewed implementation both has an entitlement and explicit user
 * opt-in.</p>
 */
@Singleton
public class StrategistRemoteGateway
{
    private final StrategistEntitlementService entitlements;

    @Inject
    public StrategistRemoteGateway(StrategistEntitlementService entitlements)
    {
        this.entitlements = entitlements;
    }

    public boolean isConfigured()
    {
        return false;
    }

    public boolean canTransmit()
    {
        return false;
    }

    public RemoteOperationResult sync(PlusSyncEnvelope envelope)
    {
        if (!entitlements.canUse(StrategistFeature.CLOUD_PROFILE_SYNC))
        {
            return RemoteOperationResult.disabled(
                    "Cloud sync is not entitled for this profile."
            );
        }

        return RemoteOperationResult.disabled(
                "Remote Compass services are not configured in this build."
        );
    }
}
