package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;

/** Converts a recommendation into concise sidebar copy and full detail copy. */
public final class RecommendationPresentation
{
    private static final int COMPACT_ACTION_CHARS = 190;
    private static final int COMPACT_SUPPLIES_CHARS = 150;

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

        List<RequirementCheck> unresolved = hardUnresolved(plan);
        if (!unresolved.isEmpty())
        {
            appendBreak(text, 2);
            text.append("<b>NEEDED</b><br>");
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
                        .append(readinessMarker(check))
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

    public static String compactText(Recommendation recommendation)
    {
        return toPlainText(compactHtml(recommendation));
    }

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
            text.append("<b>PREPARATION</b><br>");
            if (guidance != null && hasText(guidance.getAction()))
                text.append(escape(compactSentence(guidance.getAction(),
                        COMPACT_ACTION_CHARS)));
            else
                text.append("Open the relevant account panel so Compass can check the remaining requirement.");
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
            text.append(escape(compactSentence(
                    recommendation.getReason(), COMPACT_ACTION_CHARS)));
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
                .append(confidenceLabel(recommendation));
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
                    .append(escape(includeLocationAndNote
                            ? guidance.getAction()
                            : compactSentence(guidance.getAction(), COMPACT_ACTION_CHARS)));
        }

        if (hasText(guidance.getSupplies()))
        {
            appendBreak(text, 2);
            text.append("<b>NEEDED</b><br>")
                    .append(escape(includeLocationAndNote
                            ? guidance.getSupplies()
                            : compactSentence(guidance.getSupplies(), COMPACT_SUPPLIES_CHARS)));
        }

        if (!includeLocationAndNote) return;

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

    /**
     * Keep the sidebar useful without letting a detailed planner paragraph turn
     * into a wall of text. When guidance contains multiple sentences, the first
     * complete useful sentence wins even if the full paragraph technically fits
     * the character ceiling. Details retains the complete original string.
     */
    static String compactSentence(String value, int maxChars)
    {
        if (!hasText(value)) return "";
        String normalized = value.trim().replaceAll("\\s+", " ");

        int sentence = normalized.indexOf(". ");
        if (sentence > 20 && sentence + 1 <= maxChars)
        {
            return normalized.substring(0, sentence + 1);
        }
        if (normalized.length() <= maxChars) return normalized;

        int cut = Math.min(maxChars, normalized.length());
        int word = normalized.lastIndexOf(' ', cut);
        if (word >= Math.max(20, maxChars / 2)) cut = word;
        return normalized.substring(0, cut).trim() + "…";
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static List<RequirementCheck> hardUnresolved(TrainingPlan plan)
    {
        List<RequirementCheck> unresolved = new ArrayList<>();
        if (plan == null || plan.getRequirementChecks() == null) return unresolved;
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check == null || check.getState() == RequirementState.VERIFIED) continue;
            if (RequirementActionability.isPreparationRequirement(check)) continue;
            unresolved.add(check);
        }
        return unresolved;
    }

    private static String readinessMarker(RequirementCheck check)
    {
        if (check != null && RequirementActionability.isPreparationRequirement(check))
            return "•";
        return stateMarker(check == null ? RequirementState.CHECK_NEEDED : check.getState());
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
        text.append("<b>METHOD</b><br>")
                .append(escape(method.getName()));
        appendBreak(text, 1);
        text.append("<i>")
                .append(attentionLabel(method.getAttentionLevel()))
                .append(" • ")
                .append(confidenceLabel(recommendation))
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

    private static String confidenceLabel(Recommendation recommendation)
    {
        if (recommendation == null) return "Check Needed";
        if (recommendation.getConfidence() == RecommendationConfidence.VERIFIED) return "Ready";
        if (recommendation.getConfidence() == RecommendationConfidence.BLOCKED) return "Blocked";
        if (RequirementActionability.isActionablePreparation(
                recommendation.getTrainingPlan(), recommendation.getGuidance()))
            return "Ready to prep";
        return "Check Needed";
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
