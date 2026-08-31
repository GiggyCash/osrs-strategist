package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Finite traversal result with explicit termination diagnostics. */
@Getter
public final class DependencyResolution
{
    private final List<ResolvedDependencyNode> nodes;
    private final boolean cycleDetected;
    private final boolean depthLimited;
    private final boolean opportunityCostRejected;
    private final boolean nodeLimited;

    public DependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected)
    {
        this(nodes, cycleDetected, depthLimited, opportunityCostRejected, false);
    }

    public DependencyResolution(List<ResolvedDependencyNode> nodes,
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
            if (node.getConfidence() != Confidence.VERIFIED) return node;
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}
