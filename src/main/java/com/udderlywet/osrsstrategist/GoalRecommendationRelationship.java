package com.udderlywet.osrsstrategist;

/** How the visible DO NEXT relates to the selected long-term goal. */
public enum GoalRecommendationRelationship
{
    AUTOMATIC,
    DIRECT,
    PREREQUISITE,
    FALLBACK,
    CHECK_NEEDED,
    BLOCKED
}
