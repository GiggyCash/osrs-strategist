package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** A composable, local-evidence item requirement shared by planning systems. */
public final class ItemRequirementExpression
{
    public enum Kind { ITEM, ITEM_CLASS, CHECK_NEEDED, ALL_OF, ANY_OF }

    @Getter
    private final Kind kind;
    @Getter
    private final List<String> itemNames;
    @Getter
    private final int quantity;
    @Getter
    private final ItemQuantityMode quantityMode;
    @Getter
    private final ItemRequirementScope scope;
    @Getter
    private final ItemRequirementClass itemClass;
    @Getter
    private final List<String> excludedItemNames;
    @Getter
    private final String checkAction;
    @Getter
    private final List<ItemRequirementExpression> children;

    private ItemRequirementExpression(Kind kind, List<String> itemNames,
            int quantity, ItemQuantityMode quantityMode,
            ItemRequirementScope scope,
            ItemRequirementClass itemClass, List<String> excludedItemNames,
            String checkAction,
            List<ItemRequirementExpression> children)
    {
        this.kind = kind;
        this.itemNames = immutable(itemNames);
        this.quantity = Math.max(1, quantity);
        this.quantityMode = quantityMode == null
                ? ItemQuantityMode.EXACT : quantityMode;
        this.scope = scope == null ? ItemRequirementScope.IMMEDIATELY_USABLE : scope;
        this.itemClass = itemClass;
        this.excludedItemNames = immutable(excludedItemNames);
        this.checkAction = checkAction == null ? "" : checkAction;
        this.children = children == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(children));
    }

    public static ItemRequirementExpression item(String name, int quantity,
            ItemRequirementScope scope, String... substitutes)
    {
        List<String> names = new ArrayList<>();
        names.add(name);
        if (substitutes != null) names.addAll(Arrays.asList(substitutes));
        return new ItemRequirementExpression(Kind.ITEM, names, quantity,
                ItemQuantityMode.EXACT, scope,
                null, null, null, null);
    }

    public static ItemRequirementExpression itemAtLeast(String name, int quantity,
            ItemRequirementScope scope, String... substitutes)
    {
        List<String> names = new ArrayList<>();
        names.add(name);
        if (substitutes != null) names.addAll(Arrays.asList(substitutes));
        return new ItemRequirementExpression(Kind.ITEM, names, quantity,
                ItemQuantityMode.AT_LEAST, scope, null, null, null, null);
    }

    public static ItemRequirementExpression itemClass(
            ItemRequirementClass itemClass, int quantity,
            ItemRequirementScope scope, String... excludedItemNames)
    {
        return new ItemRequirementExpression(Kind.ITEM_CLASS, null, quantity,
                ItemQuantityMode.EXACT, scope, itemClass, excludedItemNames == null
                        ? null : Arrays.asList(excludedItemNames), null, null);
    }

    public static ItemRequirementExpression itemClassAtLeast(
            ItemRequirementClass itemClass, int quantity,
            ItemRequirementScope scope, String... excludedItemNames)
    {
        return new ItemRequirementExpression(Kind.ITEM_CLASS, null, quantity,
                ItemQuantityMode.AT_LEAST, scope, itemClass,
                excludedItemNames == null ? null : Arrays.asList(excludedItemNames),
                null, null);
    }

    public static ItemRequirementExpression checkNeeded(String action)
    {
        return new ItemRequirementExpression(Kind.CHECK_NEEDED, null, 1,
                ItemQuantityMode.EXACT, null,
                null, null, action, null);
    }

    public static ItemRequirementExpression allOf(ItemRequirementExpression... children)
    {
        return composite(Kind.ALL_OF, children);
    }

    public static ItemRequirementExpression anyOf(ItemRequirementExpression... children)
    {
        return composite(Kind.ANY_OF, children);
    }

    private static ItemRequirementExpression composite(Kind kind,
            ItemRequirementExpression... children)
    {
        return new ItemRequirementExpression(kind, null, 1,
                ItemQuantityMode.EXACT, null,
                null, null, null,
                children == null ? Collections.emptyList() : Arrays.asList(children));
    }


    public String label()
    {
        if (kind == Kind.ITEM)
        {
            String names = String.join(" or ", itemNames);
            if (quantity <= 1) return names;
            return quantityPrefix() + (itemNames.size() > 1
                    ? "(" + names + ")" : names);
        }
        if (kind == Kind.ITEM_CLASS)
        {
            String value = itemClass == null ? "item class" : itemClass.getLabel();
            if (!excludedItemNames.isEmpty())
                value += " (excluding " + String.join(" or ", excludedItemNames) + ")";
            return quantity <= 1 ? value : quantityPrefix() + value;
        }
        if (kind == Kind.CHECK_NEEDED) return checkAction;
        List<String> labels = new ArrayList<>();
        for (ItemRequirementExpression child : children)
        {
            String label = child.label();
            if (child.kind != Kind.ITEM && child.kind != Kind.ITEM_CLASS
                    && child.kind != Kind.CHECK_NEEDED
                    && child.kind != kind)
                label = "(" + label + ")";
            labels.add(label);
        }
        return String.join(kind == Kind.ALL_OF ? " and " : " or ", labels);
    }

    private static List<String> immutable(List<String> values)
    {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private String quantityPrefix()
    {
        return (quantityMode == ItemQuantityMode.AT_LEAST ? "at least " : "")
                + quantity + " × ";
    }
}
