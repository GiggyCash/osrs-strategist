package com.udderlywet.osrsstrategist;

/** Stable strategic metadata for a minigame or repeatable activity. */
public final class MinigameKnowledgeDefinition
{
    private final String id;
    private final String name;
    private final String purpose;
    private final double score;
    private final boolean progressionProtected;

    public MinigameKnowledgeDefinition(String id, String name,
            String purpose, double score, boolean progressionProtected)
    {
        this.id = id;
        this.name = name;
        this.purpose = purpose;
        this.score = score;
        this.progressionProtected = progressionProtected;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPurpose() { return purpose; }
    public double getScore() { return score; }
    public boolean isProgressionProtected() { return progressionProtected; }
}
