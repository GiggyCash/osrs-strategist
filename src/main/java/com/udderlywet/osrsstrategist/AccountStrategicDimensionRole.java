package com.udderlywet.osrsstrategist;

/** How candidate metadata should interpret an account strategic dimension. */
public enum AccountStrategicDimensionRole
{
    /** More of the candidate property is useful to this account. */
    BENEFIT_WEIGHT,
    /** More of the candidate property is costly to this account. */
    BURDEN_WEIGHT,
    /** Legality/availability is read from the priority's capability state. */
    CAPABILITY_GATE
}
