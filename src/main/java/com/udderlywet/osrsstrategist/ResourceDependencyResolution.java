package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Finite traversal result with explicit termination diagnostics. */
public final class ResourceDependencyResolution
{
    @Getter
    private final List<ResolvedDependencyNode> nodes;
    @Getter
    private final boolean cycleDetected;
    @Getter
    private final boolean depthLimited;
    @Getter
    private final boolean opportunityCostRejected;
    @Getter
    private final boolean nodeLimited;

    public ResourceDependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected)
    {
        this(nodes, cycleDetected, depthLimited, opportunityCostRejected, false);
    }

    public ResourceDependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected, boolean nodeLimited)
    {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.cycleDetected = cycleDetected;
        this.depthLimited = depthLimited;
        this.opportunityCostRejected = opportunityCostRejected;
        this.nodeLimited = nodeLimited;
    }

    public ResolvedDependencyNode nextAction()
    {
        for (ResolvedDependencyNode node : nodes)
            if (node.getConfidence() != RecommendationConfidence.VERIFIED) return node;
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}
