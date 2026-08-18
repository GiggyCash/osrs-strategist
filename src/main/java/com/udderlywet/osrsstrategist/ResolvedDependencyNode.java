package com.udderlywet.osrsstrategist;

/** One deduplicated result node in traversal order. */
public final class ResolvedDependencyNode
{
    private final String id;
    private final String action;
    private final RecommendationConfidence confidence;
    private final int depth;

    public ResolvedDependencyNode(String id, String action,
            RecommendationConfidence confidence, int depth)
    {
        this.id = id;
        this.action = action;
        this.confidence = confidence;
        this.depth = depth;
    }

    public String getId() { return id; }
    public String getAction() { return action; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public int getDepth() { return depth; }
}
