package com.udderlywet.osrsstrategist;

import java.util.List;

/**
 * Converts a recommendation into concise sidebar copy.
 *
 * <p>The strategy engine is allowed to be complicated. The default UI should
 * not be. This formatter deliberately shows the decision first and hides the
 * deeper explanation behind the Details button.</p>
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

        if (recommendation.getCurrentLevel() > 0
                && recommendation.getTargetLevel() > 0)
        {
            text.append("Current: ")
                    .append(recommendation.getCurrentLevel())
                    .append(" → ")
                    .append(recommendation.getTargetLevel());
        }

        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            appendBreak(text, 2);
            text.append("<b>BEST METHOD</b><br>")
                    .append("Check needed before choosing a method.");
            appendBreak(text, 1);
            text.append("<i>Confidence: ")
                    .append(confidenceLabel(
                            recommendation.getConfidence()
                    ))
                    .append("</i>");
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();

        appendBreak(text, 2);
        text.append("<b>BEST METHOD</b><br>")
                .append(escape(method.getName()));

        appendBreak(text, 1);
        text.append("<i>")
                .append(attentionLabel(method.getAttentionLevel()))
                .append(" • ")
                .append(confidenceLabel(recommendation.getConfidence()))
                .append("</i>");

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

        StringBuilder text = new StringBuilder(
                compactHtml(recommendation)
        );

        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null && plan.getMethod() != null)
        {
            TrainingMethod method = plan.getMethod();

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
        }

        return text.toString();
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
