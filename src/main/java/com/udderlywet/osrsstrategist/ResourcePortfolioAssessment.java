package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Multi-resource result that preserves per-item units. Quantities from unlike
 * resources are intentionally never added together.
 */
public final class ResourcePortfolioAssessment
{
    @Getter
    private final ResourcePipelineState state;
    @Getter
    private final int scoreAdjustment;
    @Getter
    private final List<ResourcePipelineAssessment> resources;
    @Getter
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
