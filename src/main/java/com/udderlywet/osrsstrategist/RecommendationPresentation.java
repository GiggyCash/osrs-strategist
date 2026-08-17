package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts strategy recommendations into compact, readable RuneLite sidebar
 * copy.
 *
 * <p>The presentation layer intentionally hides information that is useful to
 * the scoring engine but not useful to the player at decision time. For
 * example, attention level still influences ranking, but it does not occupy a
 * permanent line in the compact card. The compact view answers three questions:
 * what should I do, how should I do it, and what still needs to be confirmed?</p>
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
                        .append(confidenceLabel(recommendation.getConfidence()));
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
            text.append("<b>CHECK BEFORE STARTING</b><br>");
            int shown = Math.min(2, unresolved.size());
            for (int i = 0; i < shown; i++)
            {
                if (i > 0) text.append("<br>");
                RequirementCheck check = unresolved.get(i);
                text.append(check.getState() == RequirementState.BLOCKED
                                ? "• Blocked: " : "• ")
                        .append(escape(check.getLabel()));
            }
            if (unresolved.size() > shown)
            {
                text.append("<br>• +")
                        .append(unresolved.size() - shown)
                        .append(" more in Details");
            }
        }
        else
        {
            appendBreak(text, 2);
            text.append("<b>READY</b>");
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
                        .append(confidenceLabel(recommendation.getConfidence()));
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

        if (hasText(recommendation.getReason()))
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }
        return text.toString();
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
     * Unknown readiness uses a neutral bullet rather than a question mark.
     * Question marks read like broken/missing UI in a narrow game overlay. The
     * adjacent evidence text already explains that the player needs to check it.
     */
    private static String stateMarker(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return "✓";
        if (state == RequirementState.BLOCKED) return "✕";
        return "•";
    }

    private static void appendMethodHeader(
            StringBuilder text,
            TrainingMethod method)
    {
        text.append("<b>BEST METHOD</b><br>")
                .append(escape(method.getName()));
    }

    private static String confidenceLabel(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED) return "Ready";
        if (confidence == RecommendationConfidence.BLOCKED) return "Blocked";
        return "Check before starting";
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
