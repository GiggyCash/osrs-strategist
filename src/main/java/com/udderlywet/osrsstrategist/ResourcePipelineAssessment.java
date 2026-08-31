package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Readiness plus replacement/opportunity value for one or more inputs. */
@Getter
public final class ResourcePipelineAssessment
{
    private final ResourcePipelineState state;
    private final int scoreAdjustment;
    private final int observedQuantity;
    private final int requiredQuantity;
    private final List<String> acquisitionRoutes;
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
