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
 * Only unambiguous item bullets become executable ownership requirements.
 * Explicit alternatives are supported when every branch names a concrete item.
 * Generic item classes, optional items, and complicated prose stay explicit
 * verification text rather than being guessed.
 */
public final class QuestItemEvidenceParser
{
    private static final Pattern ITEM_TERM = Pattern.compile(
            "^(?:(\\d[\\d,]*)\\s+|(?:an?|one)\\s+)?"
                    + "\\[\\[([^]|#]+)(?:\\|[^]]+)?]](?:s|es)?\\s*[.;]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final String[] AMBIGUOUS_MARKERS = {
        " any ", " either ", "one of", "other ", " works",
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
        // them fail-closed until their parent-child meaning can be proven.
        if (line.startsWith("**")) return null;
        line = stripSafeAnnotations(line);
        String lower = " " + display(line).toLowerCase(Locale.ROOT) + " ";
        for (String marker : AMBIGUOUS_MARKERS)
            if (lower.contains(marker)) return null;

        String body = line.replaceFirst("^\\*\\s*", "").trim();
        if (body.toLowerCase(Locale.ROOT).contains(" or "))
            return parseExplicitAlternatives(body);
        return parseTerm(body);
    }

    private static String stripSafeAnnotations(String line)
    {
        // These notes only describe where the item is found or which quest
        // phase uses it. They do not alter identity, quantity, alternatives,
        // consumption, or ownership scope.
        String stripped = line.replaceAll("(?i)\\s*\\((?:obtainable|obtained|can be obtained|found|spawns?|part\\s+\\d+)[^)]*\\)\\s*$", "")
                .replaceAll("(?i)\\s*\\(unnoted\\)\\s*$", "")
                .replaceAll("(?i)\\s+unnoted\\s*$", "")
                .trim();
        Matcher note = Pattern.compile("^(.*)\\s+\\(([^()]*)\\)\\s*$")
                .matcher(stripped);
        if (!note.matches()) return stripped;
        String detail = note.group(2).toLowerCase(Locale.ROOT);
        String[] semantic = {" or ", "also", "instead", "unless", " if ",
                "lose", "consum", "return", "keep", "recommend", "dose",
                "charge", "at least", "up to", "more"};
        for (String marker : semantic)
            if ((" " + detail + " ").contains(marker)) return stripped;
        return note.group(1).trim();
    }

    private static ItemRequirementExpression parseExplicitAlternatives(String body)
    {
        // Parentheticals, conjunctions, and prose around an alternative often
        // change the actual semantics. Only a bare list of explicit linked items
        // separated by "or" is safe to execute automatically.
        String lower = body.toLowerCase(Locale.ROOT);
        if (body.contains("(") || body.contains(")") || lower.contains(" and "))
            return null;

        String[] branches = body.split("(?i)\\s+or\\s+");
        if (branches.length < 2 || branches.length > 4) return null;
        List<ItemRequirementExpression> alternatives = new ArrayList<>();
        for (String branch : branches)
        {
            ItemRequirementExpression alternative = parseTerm(branch.trim());
            if (alternative == null) return null;
            alternatives.add(alternative);
        }
        return ItemRequirementExpression.anyOf(
                alternatives.toArray(new ItemRequirementExpression[0]));
    }

    private static ItemRequirementExpression parseTerm(String term)
    {
        Matcher matcher = ITEM_TERM.matcher(term);
        if (!matcher.matches()) return null;
        String name = canonicalItemName(matcher.group(2).trim());
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

    private static String canonicalItemName(String name)
    {
        if (name == null || name.isEmpty()) return name;
        char first = name.charAt(0);
        if (!Character.isLetter(first) || Character.isUpperCase(first)) return name;
        return Character.toUpperCase(first) + name.substring(1);
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
