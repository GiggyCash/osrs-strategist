package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

public final class UniversalDependencyResolution
{
    @Getter
    private final List<UniversalDependencyNode> nodes;
    @Getter
    private final boolean cyclePrevented;
    @Getter
    private final boolean depthLimited;
    @Getter
    private final boolean nodeLimited;

    UniversalDependencyResolution(List<UniversalDependencyNode> nodes,
            boolean cyclePrevented, boolean depthLimited, boolean nodeLimited)
    {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.cyclePrevented = cyclePrevented;
        this.depthLimited = depthLimited;
        this.nodeLimited = nodeLimited;
    }

    public int getEdgeCount()
    {
        int count = 0;
        for (UniversalDependencyNode node : nodes)
            count += node.getParentIds().size();
        return count;
    }
    public int getMaxDepth()
    {
        int result = 0;
        for (UniversalDependencyNode node : nodes)
            result = Math.max(result, node.getDepth());
        return result;
    }
    public UniversalDependencyNode nextAction()
    {
        UniversalDependencyNode best = null;
        for (UniversalDependencyNode node : nodes)
        {
            if (node.getConfidence() == RecommendationConfidence.VERIFIED)
                continue;
            if (best == null || node.getDepth() > best.getDepth()
                    || node.getDepth() == best.getDepth()
                    && node.getId().compareTo(best.getId()) < 0)
                best = node;
        }
        return best;
    }
}
