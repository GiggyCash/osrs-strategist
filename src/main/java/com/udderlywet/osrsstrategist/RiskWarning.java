package com.udderlywet.osrsstrategist;

/**
 * Structured warning attached to a risky recommendation before it can be shown
 * as a normal action.
 */
public final class RiskWarning
{
    private final RiskLevel level;
    private final String title;
    private final String message;
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

    public RiskLevel getLevel() { return level; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRequiresExplicitConfirmation()
    {
        return requiresExplicitConfirmation;
    }
}
