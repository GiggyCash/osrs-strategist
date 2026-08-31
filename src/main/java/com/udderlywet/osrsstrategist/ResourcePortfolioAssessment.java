package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/**
 * Multi-resource result that preserves per-item units. Quantities from unlike
 * resources are intentionally never added together.
 */
@Getter
public final class ResourcePortfolioAssessment
{
    private final ResourcePipelineState state;
    private final int scoreAdjustment;
    private final List<ResourcePipelineAssessment> resources;
    private final List<String> acquisitionRoutes;

    ResourcePortfolioAssessment(ResourcePipelineState state,
            int scoreAdjustment, List<ResourcePipelineAssessment> resources,
            List<String> acquisitionRoutes)
    {
        this.state = state;
        this.scoreAdjustment = scoreAdjustment;
        this.resources = Collections.unmodifiableList(
                new ArrayList<>(resources));
        this.acquisitionRoutes = Collections.unmodifiableList(
                new ArrayList<>(acquisitionRoutes));
    }

}
