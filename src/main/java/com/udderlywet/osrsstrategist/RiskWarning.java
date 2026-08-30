package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Structured warning attached to a risky recommendation before it can be shown
 * as a normal action.
 */
public final class RiskWarning
{
    @Getter
    private final RiskLevel level;
    @Getter
    private final String title;
    @Getter
    private final String message;
    @Getter
    private final boolean requiresExplicitConfirmation;

    public RiskWarning(
            RiskLevel level,
            String title,
            String message,
            boolean requiresExplicitConfirmation)
    {
        this.level = level == null ? RiskLevel.NONE : level;
        this.title = title;
        this.message = message;
        this.requiresExplicitConfirmation = requiresExplicitConfirmation;
    }

}
