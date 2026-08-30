package com.udderlywet.osrsstrategist;

/**
 * Product capabilities rather than paywall checks scattered through gameplay
 * code. Core planning features intentionally remain separate from optional
 * hosted services so a future Plus tier can be added without weakening the
 * local planner.
 */
public enum StrategistFeature
{
    CORE_PLANNER,
    LOCAL_PROFILE_MEMORY,
    LOCAL_METHOD_GUIDANCE,
    CLOUD_PROFILE_SYNC,
    CROSS_DEVICE_HISTORY,
    GIM_TEAM_PLANNING,
    REMOTE_REMINDERS,
    WEB_DASHBOARD,
    ONLINE_REASONING
}
