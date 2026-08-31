package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Reviewable preparation evidence that may produce actions but never READY. */
public final class PvmPreparationProfile
{
    @Getter
    private final String activityId;
    @Getter
    private final String style;
    @Getter
    private final List<String> checks;
    @Getter
    private final String accountValue;
    @Getter
    private final String provenance;

    public PvmPreparationProfile(String activityId, String style,
            List<String> checks, String accountValue, String provenance)
    {
        this.activityId = activityId;
        this.style = style;
        this.checks = Collections.unmodifiableList(checks == null
                ? new ArrayList<>() : new ArrayList<>(checks));
        this.accountValue = accountValue;
        this.provenance = provenance;
    }

}
