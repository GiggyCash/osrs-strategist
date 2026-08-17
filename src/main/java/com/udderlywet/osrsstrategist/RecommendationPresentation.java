package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/** Converts a recommendation into concise sidebar copy and full detail copy. */
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
            appendNonSkillCompact(text, recommendation);
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, recommendation, method);

        RecommendationGuidance guidance = recommendation.getGuidance();
        if (guidance != null)
        {
            appendGuidance(text, guidance, false);
        }

        List<RequirementCheck> unresolved = unresolved(plan);
        if (!unresolved.isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>NEEDS INFO</b><br>");
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
        return text.toString();
    }

    public static String detailedHtml(Recommendation recommendation)
    {
        if (recommendation == null) return "";
        StringBuilder text = new StringBuilder();
        TrainingPlan plan = recommendation.getTrainingPlan();

        if (plan == null || plan.getMethod() == null)
        {
            appendNonSkillDetailed(text, recommendation);
            return text.toString();
        }

        TrainingMethod method = plan.getMethod();
        appendMethodHeader(text, recommendation, method);

        RecommendationGuidance guidance = recommendation.getGuidance();
        if (guidance != null)
        {
            appendGuidance(text, guidance, true);
        }
        else
        {
            appendBreak(text, 2);
            text.append("<b>HOW</b><br>")
                    .append(escape(method.getInstructions()));
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

        if (hasText(recommendation.getReason()))
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }
        return text.toString();
    }

    /** Plain copy used by real line-wrapping Swing components and game overlays. */
    public static String compactText(Recommendation recommendation)
    {
        return toPlainText(compactHtml(recommendation));
    }

    /** Plain full copy used by the on-game Details overlay. */
    public static String detailedText(Recommendation recommendation)
    {
        return toPlainText(detailedHtml(recommendation));
    }

    private static void appendNonSkillCompact(
            StringBuilder text,
            Recommendation recommendation)
    {
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
        {
            text.append("<b>BLOCKED</b><br>This is not available for the current account state.");
            return;
        }
        if (recommendation.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            text.append("<b>NOT READY YET</b><br>")
                    .append("Requirements still need verification. Keep playing a ready option for now.");
            return;
        }

        text.append("<b>NEXT STEP</b><br>Ready");
        if (guidance != null)
        {
            appendGuidance(text, guidance, false);
        }
        else if (hasText(recommendation.getReason()))
        {
            appendBreak(text, 2);
            text.append(escape(recommendation.getReason()));
        }
    }

    private static void appendNonSkillDetailed(
            StringBuilder text,
            Recommendation recommendation)
    {
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED)
        {
            text.append("<b>BLOCKED</b><br>This is not available for the current account state.");
            return;
        }

        text.append("<b>NEXT STEP</b><br>")
                .append(confidenceLabel(recommendation.getConfidence()));
        RecommendationGuidance guidance = recommendation.getGuidance();
        if (guidance != null)
        {
            appendGuidance(text, guidance, true);
        }
        else if (recommendation.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            appendBreak(text, 2);
            text.append("<b>STATUS</b><br>")
                    .append("This stays out of the primary recommendation until its requirements are verified.");
        }

        if (hasText(recommendation.getReason()))
        {
            appendBreak(text, 2);
            text.append("<b>WHY IT MATTERS</b><br>")
                    .append(escape(recommendation.getReason()));
        }
    }

    private static void appendGuidance(
            StringBuilder text,
            RecommendationGuidance guidance,
            boolean includeLocationAndNote)
    {
        if (hasText(guidance.getAction()))
        {
            appendBreak(text, 2);
            text.append("<b>DO THIS</b><br>")
                    .append(escape(guidance.getAction()));
        }

        if (hasText(guidance.getSupplies()))
        {
            appendBreak(text, 2);
            text.append("<b>SUPPLIES</b><br>")
                    .append(escape(guidance.getSupplies()));
        }

        if (!includeLocationAndNote)
        {
            return;
        }

        if (hasText(guidance.getLocation()))
        {
            appendBreak(text, 2);
            text.append("<b>WHERE</b><br>")
                    .append(escape(guidance.getLocation()));
        }

        if (hasText(guidance.getNote()))
        {
            appendBreak(text, 2);
            text.append("<b>NOTE</b><br>")
                    .append(escape(guidance.getNote()));
        }
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static List<RequirementCheck> unresolved(TrainingPlan plan)
    {
        List<RequirementCheck> unresolved = new ArrayList<>();
        if (plan == null || plan.getRequirementChecks() == null) return unresolved;
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check != null && check.getState() != RequirementState.VERIFIED)
            {
                unresolved.add(check);
            }
        }
        return unresolved;
    }

    private static String stateMarker(RequirementState state)
    {
        if (state == RequirementState.VERIFIED) return "✓";
        if (state == RequirementState.BLOCKED) return "✕";
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

    private static String confidenceLabel(RecommendationConfidence confidence)
    {
        if (confidence == RecommendationConfidence.VERIFIED) return "Ready";
        if (confidence == RecommendationConfidence.BLOCKED) return "Blocked";
        return "Needs Info";
    }

    private static void appendBreak(StringBuilder text, int count)
    {
        for (int i = 0; i < count; i++) text.append("<br>");
    }

    static String toPlainText(String html)
    {
        if (html == null || html.isEmpty()) return "";
        return html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</?(b|i|strong|em|html|div)(?:\\s+[^>]*)?>", "")
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
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
