package com.udderlywet.osrsstrategist;

import java.util.List;

/**
 * Converts a recommendation into concise sidebar copy.
 *
 * <p>Level progress is rendered visually by the recommendation card, so this
 * formatter focuses only on method, attention/confidence, and preparation.
 * Deeper instructions and reasoning remain behind Details.</p>
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
            appendBreak(text, 1);
            text.append("<i>")
                    .append(confidenceLabel(
                            recommendation.getConfidence()
                    ))
                    .append("</i>");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, recommendation, method);

        List<String> requirements = method.getRequirements();
        if (!requirements.isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>PREP</b><br>");

            int shown = Math.min(2, requirements.size());
            for (int i = 0; i < shown; i++)
            {
                if (i > 0)
                {
                    text.append("<br>");
                }
                text.append("• ")
                        .append(escape(requirements.get(i)));
            }

            if (requirements.size() > shown)
            {
                text.append("<br>• +")
                        .append(requirements.size() - shown)
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

        if (recommendation.getReason() != null
                && !recommendation.getReason().trim().isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }

        if (!method.getRequirements().isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>FULL PREP</b>");
            for (String requirement : method.getRequirements())
            {
                text.append("<br>• ")
                        .append(escape(requirement));
            }
        }

        return text.toString();
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

    private static void appendBreak(
            StringBuilder text,
            int count)
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
