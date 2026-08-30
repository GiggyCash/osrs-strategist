package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable snapshot of capabilities granted to the current Strategist user. */
public final class EntitlementSnapshot
{
    private final StrategistEdition edition;
    private final Set<StrategistFeature> features;

    public EntitlementSnapshot(
            StrategistEdition edition,
            Set<StrategistFeature> features)
    {
        this.edition = edition == null ? StrategistEdition.FREE : edition;
        EnumSet<StrategistFeature> copy = EnumSet.noneOf(StrategistFeature.class);
        if (features != null)
        {
            copy.addAll(features);
        }
        // The useful local planner is never treated as a premium entitlement.
        copy.add(StrategistFeature.CORE_PLANNER);
        copy.add(StrategistFeature.LOCAL_PROFILE_MEMORY);
        copy.add(StrategistFeature.LOCAL_METHOD_GUIDANCE);
        this.features = Collections.unmodifiableSet(copy);
    }

    public static EntitlementSnapshot freeLocal()
    {
        return new EntitlementSnapshot(
                StrategistEdition.FREE,
                EnumSet.of(
                        StrategistFeature.CORE_PLANNER,
                        StrategistFeature.LOCAL_PROFILE_MEMORY,
                        StrategistFeature.LOCAL_METHOD_GUIDANCE
                )
        );
    }

    public StrategistEdition getEdition()
    {
        return edition;
    }

    public boolean has(StrategistFeature feature)
    {
        return feature != null && features.contains(feature);
    }

    public Set<StrategistFeature> getFeatures()
    {
        return features;
    }
}
