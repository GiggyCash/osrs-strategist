package com.udderlywet.osrsstrategist;

/** One material rule consumed by a deterministic training action. */
public final class MethodInputRule
{
    private final MethodExecutionProfile.InputMode mode;
    private final String fixedName;
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

    public MethodExecutionProfile.InputMode getMode() { return mode; }
    public String getFixedName() { return fixedName; }
    public double getQuantityPerAction() { return quantityPerAction; }
}
