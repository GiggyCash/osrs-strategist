package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative parser for the pinned Wiki "items required" field.
 *
 * Only unambiguous, single-item bullet lines become executable ownership
 * requirements. Alternatives, generic item classes, optional items, and
 * complicated prose stay explicit verification text rather than being guessed.
 */
public final class QuestItemEvidenceParser
{
    private static final Pattern SIMPLE_ITEM = Pattern.compile(
            "^\\*\\s*(?:(\\d[\\d,]*)\\s+|(?:an?|one)\\s+)?"
                    + "\\[\\[([^]|#]+)(?:\\|[^]]+)?]](?:s|es)?\\s*[.;]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final String[] AMBIGUOUS_MARKERS = {
        " or ", " any ", " either ", "one of", "other ", " works",
        "work too", "obtainable", "optional", "recommended", " unless ",
        "alternatively", "at least", "up to", "such as", " if "
    };

    public Result parse(String wikiItems)
    {
        if (wikiItems == null || wikiItems.trim().isEmpty()
                || "none".equalsIgnoreCase(wikiItems.trim()))
            return new Result(null, Collections.emptyList(), 0);

        List<ItemRequirementExpression> parsed = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String rawLine : wikiItems.split("\\r?\\n"))
        {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            ItemRequirementExpression expression = parseLine(line);
            if (expression == null)
                unresolved.add(display(line));
            else
                parsed.add(expression);
        }

        ItemRequirementExpression expression = null;
        if (parsed.size() == 1) expression = parsed.get(0);
        else if (!parsed.isEmpty()) expression = ItemRequirementExpression.allOf(
                parsed.toArray(new ItemRequirementExpression[0]));
        return new Result(expression, unresolved, parsed.size());
    }

    private static ItemRequirementExpression parseLine(String line)
    {
        // Nested bullets usually explain alternatives or subrequirements. Keep
        // them fail-closed until a richer structured parser can prove semantics.
        if (line.startsWith("**")) return null;
        String lower = " " + display(line).toLowerCase(Locale.ROOT) + " ";
        for (String marker : AMBIGUOUS_MARKERS)
            if (lower.contains(marker)) return null;

        Matcher matcher = SIMPLE_ITEM.matcher(line);
        if (!matcher.matches()) return null;
        String name = matcher.group(2).trim();
        if (isGenericClass(name)) return null;
        int quantity = 1;
        if (matcher.group(1) != null)
        {
            try
            {
                quantity = Integer.parseInt(matcher.group(1).replace(",", ""));
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
        }
        if (quantity < 1) return null;
        return ItemRequirementExpression.item(name, quantity,
                ItemRequirementScope.OWNED_OR_RETRIEVABLE);
    }

    private static boolean isGenericClass(String name)
    {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        return normalized.equals("weapon")
                || normalized.equals("slash weapon")
                || normalized.equals("light source")
                || normalized.equals("food")
                || normalized.equals("axe")
                || normalized.equals("pickaxe")
                || normalized.equals("cat")
                || normalized.equals("kitten");
    }

    private static String display(String wiki)
    {
        return wiki.replaceAll("\\[\\[(?:[^]|]+\\|)?([^]]+)]]", "$1")
                .replaceAll("\\{\\{[^}]+}}", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceFirst("^\\*+\\s*", "")
                .replaceAll("'{2,}", "")
                .replaceAll("\\s+", " ").trim();
    }

    public static final class Result
    {
        private final ItemRequirementExpression expression;
        private final List<String> unresolved;
        private final int parsedLineCount;

        private Result(ItemRequirementExpression expression,
                List<String> unresolved, int parsedLineCount)
        {
            this.expression = expression;
            this.unresolved = Collections.unmodifiableList(
                    new ArrayList<>(unresolved));
            this.parsedLineCount = parsedLineCount;
        }

        public ItemRequirementExpression getExpression() { return expression; }
        public List<String> getUnresolved() { return unresolved; }
        public int getParsedLineCount() { return parsedLineCount; }
        public boolean isFullyExecutable()
        {
            return unresolved.isEmpty();
        }
    }
}
