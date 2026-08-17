package com.udderlywet.osrsstrategist;

/**
 * Product capabilities rather than paywall checks scattered through gameplay
 * code. Core planning and safety features are permanently local/free. Future
 * paid capabilities are additive hosted services and must never weaken the
 * player's build, membership, resource, or actionability safety gates.
 */
public enum StrategistFeature
{
    CORE_PLANNER(false),
    LOCAL_PROFILE_MEMORY(false),
    LOCAL_METHOD_GUIDANCE(false),
    LOCAL_BUILD_SAFETY(false),
    LOCAL_RESOURCE_PLANNING(false),
    LOCAL_RECOMMENDATION_HISTORY(false),

    CLOUD_PROFILE_SYNC(true),
    CROSS_DEVICE_HISTORY(true),
    GIM_TEAM_PLANNING(true),
    REMOTE_REMINDERS(true),
    WEB_DASHBOARD(true),
    ONLINE_REASONING(true),
    ADVANCED_CLOUD_ANALYTICS(true);

    private final boolean hostedPremium;

    StrategistFeature(boolean hostedPremium)
    {
        this.hostedPremium = hostedPremium;
    }

    public boolean isHostedPremium()
    {
        return hostedPremium;
    }

    public boolean isCoreLocal()
    {
        return !hostedPremium;
    }
}
