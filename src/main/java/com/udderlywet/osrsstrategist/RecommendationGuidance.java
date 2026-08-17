package com.udderlywet.osrsstrategist;

/**
 * Account-specific instructions attached to a ranked recommendation.
 *
 * <p>The training-method catalog describes a route in general. This object
 * turns that route into concrete instructions for the current milestone and
 * the supplies Strategist has actually observed.</p>
 */
public final class RecommendationGuidance
{
    private final String action;
    private final String supplies;
    private final String location;
    private final String note;

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this.action = action;
        this.supplies = supplies;
        this.location = location;
        this.note = note;
    }

    public String getAction()
    {
        return action;
    }

    public String getSupplies()
    {
        return supplies;
    }

    public String getLocation()
    {
        return location;
    }

    public String getNote()
    {
        return note;
    }
}
