package com.udderlywet.osrsstrategist;

public final class Recommendation
{
    private final String id;
    private final String title;
    private final String reason;
    private final double score;

    public Recommendation(
            String id,
            String title,
            String reason,
            double score)
    {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.score = score;
    }

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getReason()
    {
        return reason;
    }

    public double getScore()
    {
        return score;
    }
}