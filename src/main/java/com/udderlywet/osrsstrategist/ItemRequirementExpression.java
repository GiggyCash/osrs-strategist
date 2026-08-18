package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A composable, local-evidence item requirement shared by planning systems. */
public final class ItemRequirementExpression
{
    public enum Kind { ITEM, ALL_OF, ANY_OF }

    private final Kind kind;
    private final List<String> itemNames;
    private final int quantity;
    private final ItemRequirementScope scope;
    private final List<ItemRequirementExpression> children;

    private ItemRequirementExpression(Kind kind, List<String> itemNames,
            int quantity, ItemRequirementScope scope,
            List<ItemRequirementExpression> children)
    {
        this.kind = kind;
        this.itemNames = immutable(itemNames);
        this.quantity = Math.max(1, quantity);
        this.scope = scope == null ? ItemRequirementScope.IMMEDIATELY_USABLE : scope;
        this.children = children == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(children));
    }

    public static ItemRequirementExpression item(String name, int quantity,
            ItemRequirementScope scope, String... substitutes)
    {
        List<String> names = new ArrayList<>();
        names.add(name);
        if (substitutes != null) names.addAll(Arrays.asList(substitutes));
        return new ItemRequirementExpression(Kind.ITEM, names, quantity, scope, null);
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
        return new ItemRequirementExpression(kind, null, 1, null,
                children == null ? Collections.emptyList() : Arrays.asList(children));
    }

    public Kind getKind() { return kind; }
    public List<String> getItemNames() { return itemNames; }
    public int getQuantity() { return quantity; }
    public ItemRequirementScope getScope() { return scope; }
    public List<ItemRequirementExpression> getChildren() { return children; }

    public String label()
    {
        if (kind == Kind.ITEM) return String.join(" or ", itemNames);
        List<String> labels = new ArrayList<>();
        for (ItemRequirementExpression child : children) labels.add(child.label());
        return String.join(kind == Kind.ALL_OF ? " and " : " or ", labels);
    }

    private static List<String> immutable(List<String> values)
    {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
