package com.udderlywet.osrsstrategist;

/**
 * Membership access observed for the currently logged-in RuneScape profile.
 */
public enum MembershipStatus
{
    F2P("F2P"),
    P2P("P2P"),
    UNKNOWN("Unknown access");

    private final String displayName;

    MembershipStatus(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean isFreeToPlay()
    {
        return this == F2P;
    }

    public boolean isMembers()
    {
        return this == P2P;
    }
}
