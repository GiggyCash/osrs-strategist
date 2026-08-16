package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a recommendation into sidebar copy.
 *
 * <p>The collapsed view is intentionally terse. Readiness and attention are
 * different concepts: attention describes how actively a method is played,
 * while readiness describes whether Strategist has enough evidence to know the
 * method can be started. Showing both on one tiny line made the UI harder to
 * understand, so attention/setup metadata now lives in Details and the compact
 * view only calls out checks that actually need the player's attention.</p>
 */
public final class RecommendationPresentation
{
    private RecommendationPresentation() {}

    public static String compactHtml(Recommendation recommendation)
    {
        if (recommendation == null) return "";
        StringBuilder text = new StringBuilder();
        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            if (!isSkillRecommendation(recommendation))
            {
                text.append("<b>NEXT STEP</b><br>")
                        .append(statusLabel(recommendation.getConfidence()));
                if (hasText(recommendation.getReason()))
                {
                    appendBreak(text, 2);
                    text.append(escape(recommendation.getReason()));
                }
                return text.toString();
            }

            text.append("<b>METHOD UNAVAILABLE</b><br>")
                    .append("Strategist does not currently have a usable method for this account state.");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, method);

        List<RequirementCheck> unresolved = unresolved(plan);
        if (!unresolved.isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>CHECK FIRST</b><br>");
            int shown = Math.min(3, unresolved.size());
            for (int i = 0; i < shown; i++)
            {
                if (i > 0) text.append("<br>");
                RequirementCheck check = unresolved.get(i);
                text.append(check.getState() == RequirementState.BLOCKED
                                ? "✕ " : "○ ")
                        .append(escape(check.getLabel()));
            }
            if (unresolved.size() > shown)
            {
                text.append("<br>• +")
                        .append(unresolved.size() - shown)
                        .append(" more in Details");
            }
        }
        else if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED)
        {
            appendBreak(text, 2);
            text.append("<b>✓ READY</b>");
        }
        return text.toString();
    }

    public static String detailedHtml(Recommendation recommendation)
    {
        if (recommendation == null) return "";
        StringBuilder text = new StringBuilder();
        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            if (!isSkillRecommendation(recommendation))
            {
                text.append("<b>NEXT STEP</b><br>")
                        .append(statusLabel(recommendation.getConfidence()));
                if (hasText(recommendation.getReason()))
                {
                    appendBreak(text, 2);
                    text.append("<b>WHY IT MATTERS</b><br>")
                            .append(escape(recommendation.getReason()));
                }
                return text.toString();
            }

            text.append("<b>METHOD UNAVAILABLE</b><br>")
                    .append("Strategist does not currently have a usable method for this account state. This recommendation should normally be filtered from DO NEXT.");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, method);

        appendBreak(text, 2);
        text.append("<b>HOW</b><br>")
                .append(escape(method.getInstructions()));

        appendBreak(text, 2);
        text.append("<b>SESSION FIT</b><br>")
                .append(attentionLabel(method.getAttentionLevel()))
                .append(" • ")
                .append(Math.max(1, method.getMinimumSessionMinutes()))
                .append("+ min session");
        if (method.getSetupMinutes() > 0)
        {
            text.append(" • ~")
                    .append(method.getSetupMinutes())
                    .append(" min setup");
        }

        if (!plan.getRequirementChecks().isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>READINESS</b>");
            for (RequirementCheck check : plan.getRequirementChecks())
            {
                text.append("<br>")
                        .append(stateMarker(check.getState()))
                        .append(" ")
                        .append(escape(check.getLabel()));
                if (hasText(check.getEvidence()))
                {
                    text.append("<br><i>")
                            .append(escape(check.getEvidence()))
                            .append("</i>");
                }
            }
        }

        if (hasText(plan.getWhyThisMethod()))
        {
            appendBreak(text, 2);
            text.append("<b>WHY THIS METHOD</b><br>")
                    .append(escape(plan.getWhyThisMethod()));
        }

        if (hasText(recommendation.getReason()))
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }
        return text.toString();
    }

    private static void appendMethodHeader(
            StringBuilder text,
            TrainingMethod method)
    {
        text.append("<b>BEST METHOD</b><br>")
                .append(escape(method.getName()));
    }

    private static boolean isSkillRecommendation(Recommendation recommendation)
    {
        return recommendation.getId() != null
                && recommendation.getId().startsWith("skill:");
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static List<RequirementCheck> unresolved(TrainingPlan plan)
    {
        List<RequirementCheck> unresolved = new ArrayList<>();
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check.getState() != RequirementState.VERIFIED)
            {
                unresolved.add(check);
            }
        }
        return unresolved;
    }

    /**
     * A hollow circle means "not yet proven", not "wrong". This avoids the
     * question-mark glyph looking like an error or an unfinished placeholder.
     */
    private static String stateMarker(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return "✓";
        if (state == RequirementState.BLOCKED) return "✕";
        return "○";
    }

    private static String attentionLabel(AttentionLevel attention)
    {
        if (attention == null) return "Unknown attention";
        switch (attention)
        {
            case AFK: return "AFK";
            case LOW: return "Low attention";
            case ACTIVE: return "Active";
            case MODERATE:
            default: return "Moderate attention";
        }
    }

    private static String statusLabel(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED) return "Ready";
        if (confidence == RecommendationConfidence.BLOCKED) return "Blocked";
        return "Check first";
    }

    private static void appendBreak(StringBuilder text, int count)
    {
        for (int i = 0; i < count; i++) text.append("<br>");
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
