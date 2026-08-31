package com.udderlywet.osrsstrategist;

import java.util.*;
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
    private static final Pattern LINKED_ITEM_TERM = Pattern.compile(
            "^(?:(\\d[\\d,]*)\\s+|(?:an?|one|the)\\s+)?"
                    + "(?:(?:un[- ]?noted|regular|normal|cut|cooked|unlit)\\s+)*"
                    + "\\[\\[([^]|#]+)(?:\\|[^]]+)?]](?:s|es)?(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COIN_TERM = Pattern.compile(
            "^(\\d[\\d,]*)\\s+coins?(.*)$", Pattern.CASE_INSENSITIVE);

    private static final String[] AMBIGUOUS_MARKERS = {
        " any ", " either ", "one of", "other ", " unless ",
        "alternatively", "at least", "up to", "such as", " if ",
        "several", "some "
    };

    public Result parse(String wikiItems)
    {
        if (wikiItems == null || wikiItems.trim().isEmpty()
                || "none".equalsIgnoreCase(wikiItems.trim()))
            return new Result(null, Collections.emptyList(), 0);

        ItemRequirementExpression labelledAlternatives =
                parseLabelledRouteAlternatives(wikiItems);
        if (labelledAlternatives != null)
            return new Result(labelledAlternatives, Collections.emptyList(),
                    wikiItems.split("\\r?\\n").length);

        List<ItemRequirementExpression> parsed = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String rawLine : wikiItems.split("\\r?\\n"))
        {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (isStructuralOrOptional(line)) continue;
            ItemRequirementExpression expression = parseLine(line);
            if (expression == null)
            {
                ItemRequirementExpression semanticCheck =
                        parseActionableSemanticCheck(line);
                if (semanticCheck == null) unresolved.add(display(line));
                else parsed.add(semanticCheck);
            }
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
        line = stripSafeAnnotations(line);
        ItemRequirementExpression conditional = parseConditionalItem(line);
        if (conditional != null) return conditional;
        ItemRequirementExpression recipeAlternative =
                parseParentheticalAcquisitionAlternative(line);
        if (recipeAlternative != null) return recipeAlternative;
        ItemRequirementExpression substitutes = parseParentheticalSubstitutes(line);
        if (substitutes != null) return substitutes;
        ItemRequirementExpression access = parseSkillAccessAlternative(line);
        if (access != null) return access;
        ItemRequirementExpression itemClass = parseItemClass(line);
        if (itemClass != null) return itemClass;
        ItemRequirementExpression concreteList = parseConcreteAlternativeList(line);
        if (concreteList != null) return concreteList;
        String lower = " " + display(line).toLowerCase(Locale.ROOT) + " ";
        String body = line.replaceFirst("^\\*+\\s*", "").trim();
        if (display(body).matches("(?i)(?:at least\\s+|\\d[\\d,]*\\+\\s+).*"))
        {
            ItemRequirementExpression minimum = parseTerm(body);
            if (minimum != null) return minimum;
        }
        for (String marker : AMBIGUOUS_MARKERS)
            if (lower.contains(marker)) return null;

        ItemRequirementExpression conjunction = parseExplicitConjunction(body);
        if (conjunction != null) return conjunction;
        if (body.toLowerCase(Locale.ROOT).contains(" or "))
            return parseExplicitAlternatives(body);
        return parseTerm(body);
    }

    /**
     * Complex authoritative prose still has planning value even when it cannot
     * safely become an exact ownership expression. Classify the uncertainty and
     * give the player the concrete fact to verify instead of dropping the whole
     * quest into an opaque raw-only bucket.
     */
    private static ItemRequirementExpression parseActionableSemanticCheck(
            String line)
    {
        String evidence = display(line)
                .replaceAll("^[,;:.\\-–—]+\\s*", "").trim();
        if (evidence.isEmpty()) return null;
        String lower = evidence.toLowerCase(Locale.ROOT);
        if (isAcquisitionNarrative(lower)
                || isRecommendationNarrative(lower))
            return null;

        String action;
        if (lower.contains(" or ") || lower.contains("either ")
                || lower.contains("unless") || lower.contains(" if "))
            action = "Verify which quest requirement alternative applies, then prepare: ";
        else if (lower.matches(".*(?:~\\d|\\d+\\s*[-–]\\s*\\d|at most|up to|several).*"))
            action = "Verify the required quantity for your route, then prepare: ";
        else if (lower.contains("obtainable during")
                || lower.contains("obtained during"))
            action = "Plan this quest-phase acquisition before starting: ";
        else if (lower.contains("equipment") || lower.contains("weapon")
                || lower.contains("armour") || lower.contains("food")
                || lower.contains("combat") || lower.contains("spellbook"))
            action = "Verify a mechanically valid quest setup for: ";
        else
            action = "Before starting, confirm and prepare this quest requirement: ";
        return ItemRequirementExpression.checkNeeded(action + evidence);
    }

    private static ItemRequirementExpression parseParentheticalAcquisitionAlternative(
            String line)
    {
        Matcher note = Pattern.compile("^(.*)\\s+\\((?i:or)\\s+([^()]*)\\)\\s*$")
                .matcher(line);
        if (!note.matches()) return null;
        ItemRequirementExpression base = parseTerm(
                note.group(1).replaceFirst("^\\*+\\s*", "").trim());
        if (base == null) return null;
        ItemRequirementExpression alternative = parseCompoundBranch(note.group(2));
        return alternative == null ? null
                : ItemRequirementExpression.anyOf(base, alternative);
    }

    private static ItemRequirementExpression parseConcreteAlternativeList(String line)
    {
        String body = line.replaceFirst("^\\*+\\s*", "").trim()
                .replaceFirst("(?i)^(?:alternatively|either)[:,]?\\s*", "");
        String display = display(body).toLowerCase(Locale.ROOT);
        if (!display.contains(" or ")) return null;
        String normalized = body.replaceAll("\\s*,\\s*(?:(?i:or)\\s+)?", " or ");
        return parseExplicitAlternatives(normalized);
    }

    private static ItemRequirementExpression parseCompoundBranch(String body)
    {
        ItemRequirementExpression direct = parseTerm(body.trim());
        if (direct != null) return direct;
        String normalized = body.replace('+', ',')
                .replaceAll("\\s*,\\s*(?:(?i:and)\\s+)?", " and ");
        String[] branches = normalized.split("(?i)\\s+and\\s+");
        if (branches.length == 1) return parseTerm(branches[0].trim());
        if (branches.length > 8) return null;
        List<ItemRequirementExpression> values = new ArrayList<>();
        for (String branch : branches)
        {
            ItemRequirementExpression value = parseTerm(branch.trim());
            if (value == null) return null;
            values.add(value);
        }
        return ItemRequirementExpression.allOf(
                values.toArray(new ItemRequirementExpression[0]));
    }

    private static ItemRequirementExpression parseLabelledRouteAlternatives(
            String wikiItems)
    {
        List<ItemRequirementExpression> routes = new ArrayList<>();
        for (String raw : wikiItems.split("\\r?\\n"))
        {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (!line.matches("^\\*[^*].*")) return null;
            String body = line.replaceFirst("^\\*\\s*", "");
            int colon = body.indexOf(':');
            if (colon < 1) return null;
            String label = display(body.substring(0, colon));
            if (!label.toLowerCase(Locale.ROOT).endsWith("gang")) return null;
            ItemRequirementExpression route = parseLine(
                    "*" + body.substring(colon + 1).trim());
            if (route == null) return null;
            routes.add(route);
        }
        return routes.size() < 2 ? null : ItemRequirementExpression.anyOf(
                routes.toArray(new ItemRequirementExpression[0]));
    }

    private static ItemRequirementExpression parseConditionalItem(String line)
    {
        String lower = line.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(" (unless ");
        boolean unless = index >= 0;
        if (index < 0) index = lower.indexOf(" (if ");
        if (index < 0) index = lower.indexOf(" if ");
        if (index < 0) return null;

        String itemText = line.substring(0, index).replaceFirst("^\\*+\\s*", "").trim();
        ItemRequirementExpression item = parseTerm(itemText);
        if (item == null) item = parseItemClass("*" + itemText);
        if (item == null) return null;
        String condition = display(line.substring(index).trim())
                .replaceFirst("(?i)^\\(?\\s*(?:unless|if)\\s+", "")
                .replaceAll("[.)]+$", "").trim();
        if (condition.isEmpty()) return null;
        String action = "Check whether " + condition + "; "
                + (unless ? "if not, bring " : "if so, bring ") + item.label();
        return ItemRequirementExpression.checkNeeded(action);
    }

    private static ItemRequirementExpression parseSkillAccessAlternative(String line)
    {
        Matcher skill = Pattern.compile(
                "data-skill=\\\"([^\\\"]+)\\\"[^>]*data-level=\\\"(\\d+)\\\"",
                Pattern.CASE_INSENSITIVE).matcher(line);
        if (!skill.find()) return null;
        Matcher separator = Pattern.compile("(?i)\\s+or\\s+").matcher(line);
        if (!separator.find()) return null;
        String itemText = line.substring(0, separator.start())
                .replaceFirst("^\\*+\\s*", "").trim();
        ItemRequirementExpression item = parseTerm(itemText);
        if (item == null) return null;
        return ItemRequirementExpression.checkNeeded("Bring " + item.label()
                + " or verify access using " + skill.group(2) + " "
                + skill.group(1));
    }

    private static ItemRequirementExpression parseItemClass(String line)
    {
        String text = display(line).toLowerCase(Locale.ROOT)
                .replaceFirst("^[*\\s]+", "").trim();
        if (text.startsWith("runes or a powered staff")
                || text.startsWith("runes or powered staff"))
            return itemClass(ItemRequirementClass.MAGIC_COMBAT_LOADOUT);
        if (text.startsWith("items for two different combat classes"))
            return itemClass(ItemRequirementClass.MULTI_STYLE_OR_POISON);
        if (text.startsWith("magic or ranged gear")
                || text.startsWith("some form of ranged or magic"))
            return itemClass(ItemRequirementClass.MAGIC_OR_RANGED_LOADOUT);
        if (text.startsWith("means to cast telekinetic grab")
                || text.startsWith("telekinetic grab runes"))
            return itemClass(ItemRequirementClass.TELEKINETIC_GRAB_RUNES);
        if (text.startsWith("means to cast all ")
                || text.startsWith("runes to cast ")
                || text.startsWith("runes for "))
            return itemClass(ItemRequirementClass.SPELL_RUNE_LOADOUT);
        if (text.startsWith("any poison cure"))
            return itemClass(ItemRequirementClass.POISON_CURE);
        if (text.startsWith("a water container"))
            return itemClass(ItemRequirementClass.WATER_CONTAINER);
        Matcher slots = Pattern.compile(
                "^(?:up to )?(#|\\d+|one|two|three|four|five) (?:free |empty )?inventory slots?.*")
                .matcher(text);
        if (slots.matches())
        {
            int required = smallNumber(slots.group(1));
            return required <= 0
                    ? ItemRequirementExpression.checkNeeded(
                            "Check the quest's exact free-inventory-slot requirement")
                    : ItemRequirementExpression.itemClass(
                            ItemRequirementClass.EMPTY_INVENTORY_SPACE,
                            required, ItemRequirementScope.CARRIED);
        }
        if (text.startsWith("something to cut webs"))
            return itemClass(ItemRequirementClass.WEB_CUTTING_TOOL);
        if (text.matches("(?:an? )?cat or (?:a )?kitten(?:[ .(].*)?"))
            return itemClass(ItemRequirementClass.CAT_OR_KITTEN);
        if (text.matches("(?:an? |any )?light source(?:[ .(].*)?")
                && (!text.matches(".*\\b(?:if|unless)\\b.*")
                        || text.contains("if you don't have")))
            return itemClass(ItemRequirementClass.LIGHT_SOURCE);
        if (text.matches(".*\\b(?:if|unless)\\b.*")) return null;
        if (text.contains(" or ") && !text.startsWith("any cat")
                && !text.startsWith("feather ")
                && !text.matches("(?:an? |any )?light source(?:[ .(].*)?"))
            return null;
        ItemRequirementClass itemClass = null;
        int quantity = 1;
        List<String> exclusions = new ArrayList<>();

        Matcher nails = Pattern.compile("^(?:at least )?(\\d[\\d,]*)\\+? nails?\\b.*")
                .matcher(text);
        if (nails.matches())
        {
            quantity = parseQuantity(nails.group(1));
            if (quantity < 1) return null;
            return ItemRequirementExpression.itemClassAtLeast(
                    ItemRequirementClass.NAILS, quantity,
                    ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        }

        if (text.matches("(?:an? |any )?pickaxe(?:[ .].*)?"))
            itemClass = ItemRequirementClass.PICKAXE;
        else if (text.matches("(?:an? |any )?axe(?:[ .].*)?"))
            itemClass = ItemRequirementClass.AXE;
        else if (text.matches("(?:an? |any |woodcutting )?machete(?:[ .(].*)?"))
            itemClass = ItemRequirementClass.MACHETE;
        else if (text.startsWith("any crossbow"))
            itemClass = ItemRequirementClass.CROSSBOW;
        else if (text.startsWith("any bow") && !text.contains("except"))
            itemClass = ItemRequirementClass.BOW;
        else if (text.startsWith("any cat"))
            itemClass = ItemRequirementClass.CAT_OR_KITTEN;
        else if (text.startsWith("feather ") && text.contains("coloured feather")
                && text.contains("magic gold feather") && text.contains("cannot"))
            itemClass = ItemRequirementClass.FEATHER;
        else if (text.startsWith("combat equipment")
                || text.startsWith("certain armour and weapons"))
        {
            if (text.contains("food"))
                return ItemRequirementExpression.allOf(
                        itemClass(ItemRequirementClass.COMBAT_EQUIPMENT),
                        itemClass(ItemRequirementClass.HEALING_FOOD));
            itemClass = ItemRequirementClass.COMBAT_EQUIPMENT;
        }
        else if (text.equals("food") || text.startsWith("good healing food"))
            itemClass = ItemRequirementClass.HEALING_FOOD;
        else if (text.startsWith("2 full sets of h.a.m. robes"))
        {
            itemClass = ItemRequirementClass.FULL_HAM_ROBE_SET;
            quantity = 2;
        }

        if (itemClass == null) return null;
        if (text.contains("except an overgrown cat"))
            exclusions.add("Overgrown cat");
        if (text.contains("magic gold feather") && text.contains("cannot"))
            exclusions.add("Magic gold feather");
        if (text.contains("except the love crossbow"))
            exclusions.add("Love crossbow");
        return ItemRequirementExpression.itemClass(itemClass, quantity,
                ItemRequirementScope.OWNED_OR_RETRIEVABLE,
                exclusions.toArray(new String[0]));
    }

    private static ItemRequirementExpression itemClass(
            ItemRequirementClass itemClass)
    {
        return ItemRequirementExpression.itemClass(itemClass, 1,
                ItemRequirementScope.OWNED_OR_RETRIEVABLE);
    }

    private static int smallNumber(String value)
    {
        if (value == null || value.isEmpty() || "#".equals(value)) return 0;
        switch (value)
        {
            case "one": return 1;
            case "two": return 2;
            case "three": return 3;
            case "four": return 4;
            case "five": return 5;
            default:
                try
                {
                    return Integer.parseInt(value);
                }
                catch (NumberFormatException ex)
                {
                    return 0;
                }
        }
    }

    private static boolean isStructuralOrOptional(String line)
    {
        String text = display(line).toLowerCase(Locale.ROOT);
        if (text.matches("(?:trip\\s+\\d+.*|farming|firemaking|fishing|smithing|herblore|obtainable during (?:the )?quest):"))
            return true;
        if (text.startsWith("note:") || text.startsWith("offer ")
                || isAcquisitionNarrative(text)
                || isRecommendationNarrative(text)) return true;
        return text.contains("(optional)")
                || text.startsWith("optional:")
                || text.startsWith("recommended:");
    }

    private static boolean isRecommendationNarrative(String text)
    {
        return text.startsWith("recommended to bring ")
                || text.startsWith("bring additional ")
                || text.startsWith("bringing ") && text.endsWith(" also works")
                || text.startsWith("a good weapon ")
                || text.startsWith("good armour ")
                || text.startsWith("good food ");
    }

    private static boolean isAcquisitionNarrative(String text)
    {
        return text.startsWith("can be purchased from ")
                || text.startsWith("can be bought from ")
                || text.startsWith("buy from ")
                || text.startsWith("free-to-play: buy ")
                || text.startsWith("member: several ")
                || text.startsWith("there is a ")
                || text.startsWith("clay rocks are found ")
                || text.startsWith("gordon's house has ")
                || text.startsWith("all required items can be bought ")
                || text.startsWith("alternatively, get it from ");
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
        Matcher embedded = Pattern.compile("\\(([^()]*)\\)").matcher(stripped);
        StringBuffer safe = new StringBuffer();
        boolean removed = false;
        while (embedded.find())
        {
            if (isAcquisitionOnly(display(embedded.group(1)).toLowerCase(Locale.ROOT)))
            {
                embedded.appendReplacement(safe, "");
                removed = true;
            }
        }
        if (removed)
        {
            embedded.appendTail(safe);
            stripped = safe.toString().replaceAll("\\s+", " ").trim();
        }
        Matcher note = Pattern.compile("^(.*)\\s+\\(([^()]*)\\)\\s*$")
                .matcher(stripped);
        if (!note.matches()) return stripped;
        String detail = note.group(2).toLowerCase(Locale.ROOT);
        if (isAcquisitionOnly(detail)) return note.group(1).trim();
        if (isRecommendationOrOptionalExtra(detail)) return note.group(1).trim();
        String[] semantic = {" or ", "also", "instead", "unless", " if ",
                "lose", "consum", "return", "keep", "recommend", "dose",
                "charge", "at least", "up to", "more"};
        for (String marker : semantic)
            if ((" " + detail + " ").contains(marker)) return stripped;
        return note.group(1).trim();
    }

    private static boolean isRecommendationOrOptionalExtra(String detail)
    {
        String value = " " + detail.toLowerCase(Locale.ROOT) + " ";
        if (value.contains(" if you want ") || value.contains(" if you wish ")
                || value.contains(" for each additional ")) return true;
        if (!value.contains("recommend")) return false;
        return !value.contains("also work") && !value.contains("also be used")
                && !value.contains("unless") && !value.contains("required");
    }

    private static boolean isAcquisitionOnly(String detail)
    {
        String padded = " " + detail + " ";
        boolean acquisition = padded.contains("obtainable")
                || padded.contains("obtained")
                || padded.contains("can be bought")
                || padded.contains("can be purchased")
                || padded.contains("can be stolen")
                || padded.contains("acquire")
                || padded.contains("made by")
                || padded.contains("sold by")
                || padded.startsWith(" buy ")
                || padded.contains("buy from")
                || padded.contains("spawn");
        if (!acquisition) return false;
        String[] changesRequirement = {"also work", "also be used", "instead",
                "in place of", "unless", "only if", "not required"};
        for (String marker : changesRequirement)
            if (padded.contains(marker)) return false;
        return true;
    }

    private static ItemRequirementExpression parseParentheticalSubstitutes(
            String line)
    {
        Matcher note = Pattern.compile("^(.*)\\s+\\(([^()]*)\\)\\s*$")
                .matcher(line);
        if (!note.matches()) return null;
        String detail = note.group(2);
        String lower = display(detail).toLowerCase(Locale.ROOT);
        if ((!lower.contains("also work") && !lower.contains("also be used"))
                || lower.contains(" but ") || lower.contains("except")
                || lower.contains("better"))
            return null;

        String alternatives = detail.replaceAll(
                "(?i)\\s+(?:can\\s+)?also\\s+(?:work|works|be used)\\s*[.;]?\\s*$", "");
        ItemRequirementExpression base = parseTerm(
                note.group(1).replaceFirst("^\\*+\\s*", "").trim());
        if (base == null) return null;
        ItemRequirementExpression substitute;
        if (alternatives.toLowerCase(Locale.ROOT).contains(" or "))
            substitute = parseExplicitAlternatives(alternatives);
        else
            substitute = parseTerm(alternatives);
        if (substitute == null) return null;
        if (substitute.getKind() == ItemRequirementExpression.Kind.ANY_OF)
        {
            List<ItemRequirementExpression> values = new ArrayList<>();
            values.add(base);
            values.addAll(substitute.getChildren());
            return ItemRequirementExpression.anyOf(
                    values.toArray(new ItemRequirementExpression[0]));
        }
        return ItemRequirementExpression.anyOf(base, substitute);
    }

    private static ItemRequirementExpression parseExplicitConjunction(String body)
    {
        String[] branches = body.split("(?i)\\s+(?:and|&)\\s+");
        if (branches.length < 2 || branches.length > 6) return null;
        List<ItemRequirementExpression> values = new ArrayList<>();
        for (String branch : branches)
        {
            ItemRequirementExpression value = parseTerm(branch.trim());
            if (value == null) return null;
            values.add(value);
        }
        return ItemRequirementExpression.allOf(
                values.toArray(new ItemRequirementExpression[0]));
    }

    private static ItemRequirementExpression parseExplicitAlternatives(String body)
    {
        // Parentheticals, conjunctions, and prose around an alternative often
        // change the actual semantics. Only a bare list of explicit linked items
        // separated by "or" is safe to execute automatically.
        String lower = body.toLowerCase(Locale.ROOT);
        if (lower.contains(" and "))
            return null;

        String[] branches = body.split("(?i)\\s+or\\s+");
        if (branches.length < 2 || branches.length > 8) return null;
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
        term = term.replaceFirst("^\\*+\\s*", "").trim();
        boolean minimum = term.matches("(?i)^at least\\s+.*")
                || term.matches("^\\d[\\d,]*\\+\\s+.*");
        term = term.replaceFirst("(?i)^at least\\s+", "")
                .replaceFirst("^(\\d[\\d,]*)\\+\\s+", "$1 ");
        Matcher matcher = LINKED_ITEM_TERM.matcher(term);
        String name;
        String quantityText;
        String tail;
        if (matcher.matches())
        {
            quantityText = matcher.group(1);
            name = canonicalItemName(matcher.group(2).trim());
            tail = matcher.group(3);
        }
        else
        {
            Matcher coins = COIN_TERM.matcher(display(term));
            if (!coins.matches()) return null;
            quantityText = coins.group(1);
            name = "Coins";
            tail = coins.group(2);
        }
        if (isGenericClass(name)) return null;
        int quantity = 1;
        if (quantityText != null)
        {
            try
            {
                quantity = Integer.parseInt(quantityText.replace(",", ""));
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
        }
        if (quantity < 1) return null;
        if (!isSafeTrailingDetail(tail)) return null;
        return minimum
                ? ItemRequirementExpression.itemAtLeast(name, quantity,
                        ItemRequirementScope.OWNED_OR_RETRIEVABLE)
                : ItemRequirementExpression.item(name, quantity,
                        ItemRequirementScope.OWNED_OR_RETRIEVABLE);
    }

    private static int parseQuantity(String value)
    {
        if (value == null) return -1;
        try
        {
            return Integer.parseInt(value.replace(",", ""));
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    private static boolean isSafeTrailingDetail(String tail)
    {
        String value = display(tail == null ? "" : tail)
                .replaceFirst("^[,;:.\\-–—]+\\s*", "").trim();
        if (value.isEmpty()) return true;
        String lower = " " + value.toLowerCase(Locale.ROOT) + " ";
        if (lower.startsWith(" to buy ") || lower.startsWith(" to obtain "))
            return true;
        String[] unsafe = {" or ", " and ", "also work", "also be used",
                "instead", "unless", " if ", "at least", "up to",
                "not work", "except", "only if"};
        for (String marker : unsafe)
            if (lower.contains(marker)) return false;
        return lower.startsWith(" to ") || lower.startsWith(" for ")
                || lower.startsWith(" since ") || lower.startsWith(" required ")
                || lower.startsWith(" obtainable ") || lower.startsWith(" obtained ")
                || lower.startsWith(" can be ") || lower.startsWith(" made by ")
                || lower.startsWith(" bought ") || lower.startsWith(" used ")
                || lower.startsWith(" you'll ") || lower.startsWith(" you will ");
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
                .replaceAll("(?i)link=[^|]+\\|alt=[^ ]+\\s*", "")
                .replaceAll("(?i)File:[^ ]+\\.(?:png|gif|jpe?g)\\s*", "")
                .replace("&nbsp;", " ")
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

        /** True only when every parsed edge can be evaluated from account state. */
        public boolean isDeterministicallyExecutable()
        {
            return unresolved.isEmpty() && countChecks(expression) == 0;
        }

        public int getCheckNeededExpressionCount()
        {
            return countChecks(expression);
        }

        private static int countChecks(ItemRequirementExpression value)
        {
            if (value == null) return 0;
            int count = value.getKind()
                    == ItemRequirementExpression.Kind.CHECK_NEEDED ? 1 : 0;
            for (ItemRequirementExpression child : value.getChildren())
                count += countChecks(child);
            return count;
        }
    }
}
