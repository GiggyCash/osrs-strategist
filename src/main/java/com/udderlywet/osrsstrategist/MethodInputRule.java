package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One material rule consumed by a deterministic training action. */
public final class MethodInputRule
{
    @Getter
    private final MethodExecutionProfile.InputMode mode;
    @Getter
    private final String fixedName;
    @Getter
    private final double quantityPerAction;

    public MethodInputRule(
            MethodExecutionProfile.InputMode mode,
            String fixedName,
            double quantityPerAction)
    {
        this.mode = mode == null
                ? MethodExecutionProfile.InputMode.NONE
                : mode;
        this.fixedName = fixedName;
        this.quantityPerAction = Math.max(0.0, quantityPerAction);
    }

}
