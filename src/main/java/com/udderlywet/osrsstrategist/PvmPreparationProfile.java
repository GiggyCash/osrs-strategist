package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reviewable preparation evidence that may produce actions but never READY. */
public final class PvmPreparationProfile
{
    private final String activityId;
    private final String style;
    private final List<String> checks;
    private final String accountValue;
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

    public String getActivityId() { return activityId; }
    public String getStyle() { return style; }
    public List<String> getChecks() { return checks; }
    public String getAccountValue() { return accountValue; }
    public String getProvenance() { return provenance; }
}
