package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One deduplicated result node in traversal order. */
public final class ResolvedDependencyNode
{
    @Getter
    private final String id;
    @Getter
    private final String action;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final int depth;
    @Getter
    private final int requiredQuantity;

    public ResolvedDependencyNode(String id, String action,
            RecommendationConfidence confidence, int depth)
    {
        this(id, action, confidence, depth, 0);
    }

    public ResolvedDependencyNode(String id, String action,
            RecommendationConfidence confidence, int depth,
            int requiredQuantity)
    {
        this.id = id;
        this.action = action;
        this.confidence = confidence;
        this.depth = depth;
        this.requiredQuantity = Math.max(0, requiredQuantity);
    }

}
