package compass;
import static java.util.Collections.*;
import lombok.Getter;
import static java.lang.Math.*;
import static compass.Text.get;

import java.util.*;

/** Converts a recommendation into concise sidebar copy and full detail copy. */
public final class Presentation
{
    private static final int COMPACT_ACTION_CHARS = 150;
    private static final int COMPACT_SUPPLIES_CHARS = 120;
    private static final int COMPACT_LOCATION_CHARS = 130;

    private Presentation() {}

    public static String compactHtml(Recommendation recommendation)
    {
        return compactHtml(recommendation, null);
    }

    public static String compactHtml(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        if (recommendation == null) return "";
        var text = new StringBuilder();
        var plan = recommendation.plan();

        appendGoalStatus(text, goalContext);
        appendRiskDisclosure(text, recommendation.guidance);

        if (plan == null || plan.method() == null)
        {
            appendNonSkillCompact(text, recommendation);
            return text.toString();
        }

        var method = plan.method();
        appendMethodHeader(text, recommendation, method);

        var guidance = recommendation.guidance;
        if (guidance != null)
        {
            appendCompactGuidance(text, guidance);
        }

        var unresolved = hardUnresolved(plan);
        if (!unresolved.isEmpty())
        {
            appendBreak(text, 2);
            text.append(get(1911));
            var shown = min(2, unresolved.size());
            for (int i = 0; i < shown; i++)
            {
                if (i > 0) text.append("<br>");
                var check = unresolved.get(i);
                text.append(check.getState() == RequirementState.BLOCKED
                                ? "• Blocked: " : "• ")
                        .append(escape(check.getLabel()));
            }
            if (unresolved.size() > shown)
            {
                text.append("<br>• +")
                        .append(unresolved.size() - shown)
                        .append(get(1912));
            }
        }
        return text.toString();
    }

    public static String detailedHtml(Recommendation recommendation)
    {
        return detailedHtml(recommendation, null);
    }

    public static String detailedHtml(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        var text = new StringBuilder();
        for (Section section : detailsSections(recommendation, goalContext))
        {
            if (text.length() > 0) appendBreak(text, 2);
            text.append("<b>").append(escape(section.heading))
                    .append("</b><br>").append(escape(section.getValue()));
        }
        return text.toString();
    }

    public static String compactText(Recommendation recommendation)
    {
        return toPlainText(compactHtml(recommendation));
    }

    public static String compactText(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        return toPlainText(compactHtml(recommendation, goalContext));
    }

    public static String detailedText(Recommendation recommendation)
    {
        return toPlainText(detailedHtml(recommendation));
    }

    public static String detailedText(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        return toPlainText(detailedHtml(recommendation, goalContext));
    }

    /** Four compact sections retain the useful decision without graph dumps. */
    public static List<Section> detailsSections(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        if (recommendation == null) return emptyList();
        List<Section> sections = new ArrayList<>();
        var guidance = recommendation.guidance;
        if (guidance != null && guidance.getRiskDisclosure() != null)
            sections.add(new Section(
                    guidance.getRiskDisclosure().heading,
                    guidance.getRiskDisclosure().getMessage()));
        if (goalContext != null && goalContext.hasProvenRelationship()
                && hasText(goalContext.status))
            sections.add(new Section("GOAL",
                    compactSentence(goalContext.status, 160)));

        var why = playerWhy(recommendation);
        if (hasText(why))
            sections.add(new Section("WHY", compactSentence(why, 140)));

        var needed = firstNeeded(recommendation);
        if (hasText(needed))
            sections.add(new Section(
                    recommendation.confidence == Confidence.BLOCKED
                            ? "BLOCKED BY" : "NEEDED",
                    compactSentence(needed, 140)));

        // Reserve the fourth compact slot for the executable current step.
        if (sections.size() < 3 && guidance != null
                && hasText(guidance.location))
            sections.add(new Section("WHERE",
                    compactSentence(guidance.location, 140)));

        String current = guidance != null
                ? guidance.getAction() : recommendation.title;
        if (hasText(current))
            sections.add(new Section("CURRENT STEP",
                    compactSentence(current, 150)));

        if (sections.size() < 4 && hasText(recommendation.reason)
                && recommendation.goalProvenance == null
                && !sameSentence(why, recommendation.reason))
            sections.add(new Section("NEXT",
                    compactSentence(recommendation.reason, 130)));
        return unmodifiableList(sections.subList(0,
                min(4, sections.size())));
    }

    private static String playerWhy(Recommendation recommendation)
    {
        if (recommendation != null
                && recommendation.goalProvenance != null)
            return recommendation.goalProvenance.playerReason();
        return recommendation == null ? null : recommendation.reason;
    }

    private static void appendNonSkillCompact(
            StringBuilder text,
            Recommendation recommendation)
    {
        var guidance = recommendation.guidance;
        if (recommendation.confidence == Confidence.BLOCKED)
        {
            text.append(get(1913))
                    .append(get(692));
            return;
        }
        text.append(get(1914))
                .append(escape(compactSentence(recommendation.title, 110)));
        if (recommendation.confidence != Confidence.VERIFIED)
        {
            if (guidance != null && hasText(guidance.getAction()))
                appendCompactGuidance(text, guidance);
            else
            {
                appendBreak(text, 2);
                text.append("<b>DO</b><br>");
                text.append(get(693));
            }
            return;
        }

        if (guidance != null)
        {
            appendCompactGuidance(text, guidance);
        }
        else if (hasText(recommendation.reason))
        {
            appendBreak(text, 2);
            text.append(get(694));
        }
    }

    private static void appendRiskDisclosure(StringBuilder text,
            Guidance guidance)
    {
        if (guidance == null || guidance.getRiskDisclosure() == null) return;
        var disclosure = guidance.getRiskDisclosure();
        text.append("<b>").append(escape(disclosure.heading))
                .append("</b><br>")
                .append(escape(compactSentence(disclosure.getMessage(), 180)));
        if (disclosure.isAcknowledgementRequired())
            text.append(get(695));
        appendBreak(text, 2);
    }

    /** The primary card must contain the executable loop, not only its inputs. */
    private static void appendCompactGuidance(
            StringBuilder text, Guidance guidance)
    {
        if (guidance == null) return;
        if (meaningfulSupplies(guidance.supplies))
        {
            appendBreak(text, 2);
            text.append(get(1915))
                    .append(escape(compactSentence(guidance.supplies,
                            COMPACT_SUPPLIES_CHARS)));
        }
        if (hasText(guidance.location))
        {
            appendBreak(text, 2);
            text.append(get(1916))
                    .append(escape(compactSentence(guidance.location,
                            COMPACT_LOCATION_CHARS)));
        }
        if (hasText(guidance.getAction()))
        {
            appendBreak(text, 2);
            text.append("<b>DO</b><br>")
                    .append(escape(compactSentence(guidance.getAction(),
                            COMPACT_ACTION_CHARS)));
        }
    }

    private static boolean meaningfulSupplies(String supplies)
    {
        if (!hasText(supplies)) return false;
        var normalized = supplies.trim().toLowerCase();
        return !normalized.equals("none")
                && !normalized.startsWith(get(1234))
                && !normalized.startsWith(get(1235))
                && !normalized.startsWith(get(1236));
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
        var normalized = value.trim().replaceAll("\\s+", " ");

        var sentence = normalized.indexOf(". ");
        if (sentence > 20 && sentence + 1 <= maxChars)
        {
            return normalized.substring(0, sentence + 1);
        }
        if (normalized.length() <= maxChars) return normalized;

        var cut = min(maxChars, normalized.length());
        var word = normalized.lastIndexOf(' ', cut);
        if (word >= max(20, maxChars / 2)) cut = word;
        return normalized.substring(0, cut).trim() + "…";
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static void appendGoalStatus(StringBuilder text,
            GoalRecommendationContext context)
    {
        if (context == null || !context.hasProvenRelationship()
                || !hasText(context.status)) return;
        text.append("<b>GOAL</b><br>")
                .append(escape(compactSentence(context.status, 125)));
        appendBreak(text, 2);
    }

    private static String firstNeeded(Recommendation recommendation)
    {
        var plan = recommendation.plan();
        if (plan != null)
            for (EvidenceCheck check : hardUnresolved(plan))
                if (check != null && hasText(check.getLabel()))
                    return check.getLabel();
        var guidance = recommendation.guidance;
        if (recommendation.confidence
                    == Confidence.CHECK_NEEDED
                && guidance != null && hasText(guidance.supplies))
            return guidance.supplies;
        if (recommendation.confidence == Confidence.CHECK_NEEDED)
            return get(696);
        if (recommendation.confidence == Confidence.BLOCKED)
            return get(697);
        return "";
    }

    private static boolean sameSentence(String left, String right)
    {
        if (!hasText(left) || !hasText(right)) return false;
        return compactSentence(left, 140).equals(compactSentence(right, 140));
    }

    private static List<EvidenceCheck> hardUnresolved(TrainingPlan plan)
    {
        List<EvidenceCheck> unresolved = new ArrayList<>();
        if (plan == null || plan.requirementChecks == null) return unresolved;
        for (EvidenceCheck check : plan.requirementChecks)
        {
            if (check == null || check.getState() == RequirementState.VERIFIED) continue;
            if (RequirementActionability.isPreparationRequirement(check)) continue;
            unresolved.add(check);
        }
        return unresolved;
    }

    private static void appendMethodHeader(
            StringBuilder text,
            Recommendation recommendation,
            TrainingMethod method)
    {
        text.append(get(1917))
                .append(escape(method.getName()));
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
                .replaceAll(get(1918), "")
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

    @Getter
    public static final class Section
    {
        final String heading;
        private final String value;

        Section(String heading, String value)
        {
            this.heading = heading == null ? "" : heading;
            this.value = value == null ? "" : value;
        }

    }
}
