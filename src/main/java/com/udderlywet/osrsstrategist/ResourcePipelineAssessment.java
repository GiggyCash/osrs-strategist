package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Readiness plus replacement/opportunity value for one or more inputs. */
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

    public ResourcePipelineState getState() { return state; }
    public int getScoreAdjustment() { return scoreAdjustment; }
    public int getObservedQuantity() { return observedQuantity; }
    public int getRequiredQuantity() { return requiredQuantity; }
    public List<String> getAcquisitionRoutes() { return acquisitionRoutes; }
    public String getEvidence() { return evidence; }
}
