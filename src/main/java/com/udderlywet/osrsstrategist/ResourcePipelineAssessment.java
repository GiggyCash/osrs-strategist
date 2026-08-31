package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Readiness plus replacement/opportunity value for one or more inputs. */
public final class ResourcePipelineAssessment
{
    @Getter
    private final ResourcePipelineState state;
    @Getter
    private final int scoreAdjustment;
    @Getter
    private final int observedQuantity;
    @Getter
    private final int requiredQuantity;
    @Getter
    private final List<String> acquisitionRoutes;
    @Getter
    private final String evidence;

    ResourcePipelineAssessment(ResourcePipelineState state,
            int scoreAdjustment, int observedQuantity, int requiredQuantity,
            List<String> acquisitionRoutes, String evidence)
    {
        this.state = state;
        this.scoreAdjustment = scoreAdjustment;
        this.observedQuantity = observedQuantity;
        this.requiredQuantity = requiredQuantity;
        this.acquisitionRoutes = Collections.unmodifiableList(
                new ArrayList<>(acquisitionRoutes));
        this.evidence = evidence;
    }

}
