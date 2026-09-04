package compass;
import lombok.*;
import static java.util.Collections.*;

import java.util.*;


/** A composable, local-evidence item requirement shared by planning systems. */
@Getter
public final class ItemRule
{
    public enum Kind { ITEM, ITEM_CLASS, CHECK_NEEDED, ALL_OF, ANY_OF }

    final Kind kind;
    final List<String> itemNames;
    final int quantity;
    final ItemQuantityMode quantityMode;
    final ItemRequirementScope scope;
    final ItemRequirementClass itemClass;
    final List<String> excludedItemNames;
    final String checkAction;
    final List<ItemRule> children;

    ItemRule(Kind kind, List<String> itemNames,
            int quantity, ItemQuantityMode quantityMode,
            ItemRequirementScope scope,
            ItemRequirementClass itemClass, List<String> excludedItemNames,
            String checkAction,
            List<ItemRule> children)
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
        this.children = children == null ? emptyList()
                : unmodifiableList(new ArrayList<>(children));
    }

    public static ItemRule item(String name, int quantity,
            ItemRequirementScope scope, String... substitutes)
    {
        List<String> names = new ArrayList<>();
        names.add(name);
        if (substitutes != null) names.addAll(Arrays.asList(substitutes));
        return new ItemRule(Kind.ITEM, names, quantity,
                ItemQuantityMode.EXACT, scope,
                null, null, null, null);
    }
    public static ItemRule itemClass(
            ItemRequirementClass itemClass, int quantity,
            ItemRequirementScope scope, String... excludedItemNames)
    {
        return new ItemRule(Kind.ITEM_CLASS, null, quantity,
                ItemQuantityMode.EXACT, scope, itemClass, excludedItemNames == null
                        ? null : Arrays.asList(excludedItemNames), null, null);
    }
    public static ItemRule checkNeeded(String action)
    {
        return new ItemRule(Kind.CHECK_NEEDED, null, 1,
                ItemQuantityMode.EXACT, null,
                null, null, action, null);
    }

    public static ItemRule allOf(ItemRule... children)
    {
        return composite(Kind.ALL_OF, children);
    }

    public static ItemRule anyOf(ItemRule... children)
    {
        return composite(Kind.ANY_OF, children);
    }

    static ItemRule composite(Kind kind,
            ItemRule... children)
    {
        return new ItemRule(kind, null, 1,
                ItemQuantityMode.EXACT, null,
                null, null, null,
                children == null ? emptyList() : Arrays.asList(children));
    }


    public String label()
    {
        if (kind == Kind.ITEM)
        {
            var names = String.join(" or ", itemNames);
            if (quantity <= 1) return names;
            return quantityPrefix() + (itemNames.size() > 1
                    ? "(" + names + ")" : names);
        }
        if (kind == Kind.ITEM_CLASS)
        {
            var value = itemClass == null ? "item class" : itemClass.getLabel();
            if (!excludedItemNames.isEmpty())
                value += " (excluding " + String.join(" or ", excludedItemNames) + ")";
            return quantity <= 1 ? value : quantityPrefix() + value;
        }
        if (kind == Kind.CHECK_NEEDED) return checkAction;
        List<String> labels = new ArrayList<>();
        for (ItemRule child : children)
        {
            var label = child.label();
            if (child.kind != Kind.ITEM && child.kind != Kind.ITEM_CLASS
                    && child.kind != Kind.CHECK_NEEDED
                    && child.kind != kind)
                label = "(" + label + ")";
            labels.add(label);
        }
        return String.join(kind == Kind.ALL_OF ? " and " : " or ", labels);
    }

    static List<String> immutable(List<String> values)
    {
        return values == null ? emptyList()
                : unmodifiableList(new ArrayList<>(values));
    }

    String quantityPrefix()
    {
        return (quantityMode == ItemQuantityMode.AT_LEAST ? "at least " : "")
                + quantity + " × ";
    }

    /** Restores the immutable boundary after Gson hydrates bundled evidence. */
    ItemRule freeze()
    {
        List<ItemRule> frozen = new ArrayList<>();
        for (ItemRule child : children) frozen.add(child.freeze());
        return new ItemRule(kind, itemNames, quantity,
                quantityMode, scope, itemClass, excludedItemNames, checkAction,
                frozen);
    }
}
