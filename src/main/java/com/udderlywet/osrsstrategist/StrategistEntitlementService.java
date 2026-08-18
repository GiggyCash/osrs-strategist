package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Single entitlement boundary for a possible future Compass Plus service.
 *
 * <p>The current implementation is intentionally local/free and performs no
 * authentication or network work. A future trusted provider can replace the
 * snapshot without teaching the strategy engine about billing.</p>
 */
@Singleton
public class StrategistEntitlementService
{
    private volatile EntitlementSnapshot snapshot =
            EntitlementSnapshot.freeLocal();

    public EntitlementSnapshot current()
    {
        return snapshot;
    }

    public boolean canUse(StrategistFeature feature)
    {
        return snapshot.has(feature);
    }

    void replaceFromTrustedProvider(EntitlementSnapshot value)
    {
        snapshot = value == null
                ? EntitlementSnapshot.freeLocal()
                : value;
    }
}
