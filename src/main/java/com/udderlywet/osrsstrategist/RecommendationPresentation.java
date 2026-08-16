package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a recommendation into concise sidebar copy.
 *
 * <p>"Check Needed" is never left unexplained. The compact card shows the
 * first unresolved requirement; Details shows the complete readiness list and
 * the evidence behind both verified and unresolved checks.</p>
 */
public final class RecommendationPresentation
{
    private RecommendationPresentation()
    {
    }

    public static String compactHtml(Recommendation recommendation)
    {
        if (recommendation == null)
        {
            return "";
        }

        StringBuilder text = new StringBuilder();
        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            text.append("<b>BEST METHOD</b><br>")
                    .append("Check needed before choosing a method.");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, recommendation, method);

        List<RequirementCheck> unresolved = unresolved(plan);
        if (!unresolved.isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>CHECK NEEDED</b><br>");

            int shown = Math.min(2, unresolved.size());
            for (int i = 0; i < shown; i++)
            {
                if (i > 0)
                {
                    text.append("<br>");
                }
                RequirementCheck check = unresolved.get(i);
                text.append(check.getState() == RequirementState.BLOCKED
                                ? "• Blocked: "
                                : "• ")
                        .append(escape(check.getLabel()));
            }

            if (unresolved.size() > shown)
            {
                text.append("<br>• +")
                        .append(unresolved.size() - shown)
                        .append(" more in Details");
            }
        }

        return text.toString();
    }

    public static String detailedHtml(Recommendation recommendation)
    {
        if (recommendation == null)
        {
            return "";
        }

        StringBuilder text = new StringBuilder();
        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            text.append("<b>BEST METHOD</b><br>")
                    .append("Check needed before choosing a method.");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, recommendation, method);

        appendBreak(text, 2);
        text.append("<b>HOW</b><br>")
                .append(escape(method.getInstructions()));

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

                // Details is intentionally the deeper view, so show the proof
                // for successful checks too. This lets a player understand that
                // an unlock came from quest state or remembered access instead
                // of wondering why Strategist marked it Verified.
                if (check.getEvidence() != null
                        && !check.getEvidence().trim().isEmpty())
                {
                    text.append("<br><i>")
                            .append(escape(check.getEvidence()))
                            .append("</i>");
                }
            }
        }

        if (recommendation.getReason() != null
                && !recommendation.getReason().trim().isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }

        return text.toString();
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

    private static String stateMarker(RequirementState state)
    {
        if (state == RequirementState.VERIFIED)
        {
            return "✓";
        }
        if (state == RequirementState.BLOCKED)
        {
            return "✕";
        }
        return "?";
    }

    private static void appendMethodHeader(
            StringBuilder text,
            Recommendation recommendation,
            TrainingMethod method)
    {
        text.append("<b>BEST METHOD</b><br>")
                .append(escape(method.getName()));

        appendBreak(text, 1);
        text.append("<i>")
                .append(attentionLabel(method.getAttentionLevel()))
                .append(" • ")
                .append(confidenceLabel(recommendation.getConfidence()))
                .append("</i>");
    }

    private static String attentionLabel(AttentionLevel attention)
    {
        if (attention == null)
        {
            return "Unknown attention";
        }

        switch (attention)
        {
            case AFK:
                return "AFK";
            case LOW:
                return "Low attention";
            case ACTIVE:
                return "Active";
            case MODERATE:
            default:
                return "Moderate attention";
        }
    }

    private static String confidenceLabel(
            RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED)
        {
            return "Verified";
        }
        if (confidence == RecommendationConfidence.BLOCKED)
        {
            return "Blocked";
        }
        return "Check Needed";
    }

    private static void appendBreak(StringBuilder text, int count)
    {
        for (int i = 0; i < count; i++)
        {
            text.append("<br>");
        }
    }

    private static String escape(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
