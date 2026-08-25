package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ContextualGearAssessment
{
    private final Map<GearDecisionKind, ContextualGearDecision> decisions;

    ContextualGearAssessment(
            Map<GearDecisionKind, ContextualGearDecision> decisions)
    {
        this.decisions = Collections.unmodifiableMap(
                new EnumMap<>(decisions));
    }

    public ContextualGearDecision get(GearDecisionKind kind)
    {
        return decisions.get(kind);
    }

    public Map<GearDecisionKind, ContextualGearDecision> all()
    {
        return decisions;
    }
}
