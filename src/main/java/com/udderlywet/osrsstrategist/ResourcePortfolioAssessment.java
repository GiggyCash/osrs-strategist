package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-resource result that preserves per-item units. Quantities from unlike
 * resources are intentionally never added together.
 */
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

    public ResourcePipelineState getState() { return state; }
    public int getScoreAdjustment() { return scoreAdjustment; }
    public List<ResourcePipelineAssessment> getResources() { return resources; }
    public List<String> getAcquisitionRoutes() { return acquisitionRoutes; }
}
