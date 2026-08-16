package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A requirement satisfied by one or more observed item-name alternatives.
 *
 * <p>This complements {@link ResourceRequirement}, which is ideal when RuneLite
 * exposes one stable set of exact gameval item IDs. Named requirements are used
 * for families whose valid members vary by tier or account level. Matching is
 * intentionally explicit and conservative; an item only counts when at least
 * one declared rule matches it.</p>
 */
public final class NamedResourceRequirement
{
    private final String id;
    private final String label;
    private final int requiredQuantity;
    private final List<ItemNameRule> rules;

    public NamedResourceRequirement(
            String id,
            String label,
            int requiredQuantity,
            ItemNameRule... rules)
    {
        this.id = id;
        this.label = label;
        this.requiredQuantity = Math.max(1, requiredQuantity);
        this.rules = Collections.unmodifiableList(
                new ArrayList<>(rules == null
                        ? Collections.emptyList()
                        : Arrays.asList(rules)));
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public int getRequiredQuantity() { return requiredQuantity; }
    public List<ItemNameRule> getRules() { return rules; }

    public boolean matches(ItemStackSnapshot item, AccountSnapshot account)
    {
        if (item == null || item.getQuantity() <= 0) return false;
        for (ItemNameRule rule : rules)
        {
            if (rule != null && rule.matches(item.getName(), account))
                return true;
        }
        return false;
    }
}
