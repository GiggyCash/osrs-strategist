package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finite traversal result with explicit termination diagnostics. */
public final class ResourceDependencyResolution
{
    private final List<ResolvedDependencyNode> nodes;
    private final boolean cycleDetected;
    private final boolean depthLimited;
    private final boolean opportunityCostRejected;
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

    public List<ResolvedDependencyNode> getNodes() { return nodes; }
    public boolean isCycleDetected() { return cycleDetected; }
    public boolean isDepthLimited() { return depthLimited; }
    public boolean isOpportunityCostRejected() { return opportunityCostRejected; }
    public boolean isNodeLimited() { return nodeLimited; }
    public ResolvedDependencyNode nextAction()
    {
        for (ResolvedDependencyNode node : nodes)
            if (node.getConfidence() != RecommendationConfidence.VERIFIED) return node;
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}
