package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One deduplicated result node in traversal order. */
@Getter
public final class ResolvedDependencyNode
{
    private final String id;
    private final String action;
    private final Confidence confidence;
    private final int depth;
    private final int requiredQuantity;

    public ResolvedDependencyNode(String id, String action,
            Confidence confidence, int depth)
    {
        this(id, action, confidence, depth, 0);
    }

    public ResolvedDependencyNode(String id, String action,
            Confidence confidence, int depth,
            int requiredQuantity)
    {
        this.id = id;
        this.action = action;
        this.confidence = confidence;
        this.depth = depth;
        this.requiredQuantity = Math.max(0, requiredQuantity);
    }

}
