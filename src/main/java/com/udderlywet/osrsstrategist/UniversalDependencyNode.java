package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** One typed, deduplicated node in the universal planning graph. */
public final class UniversalDependencyNode
{
    private final String id;
    private final GoalNodeKind kind;
    private final String action;
    private final RecommendationConfidence confidence;
    private final int depth;
    private int quantity;
    private final Set<String> parentIds = new LinkedHashSet<>();

    UniversalDependencyNode(String id, GoalNodeKind kind, String action,
            RecommendationConfidence confidence, int depth, int quantity)
    {
        this.id = id;
        this.kind = kind;
        this.action = action == null ? "" : action;
        this.confidence = confidence == null
                ? RecommendationConfidence.CHECK_NEEDED : confidence;
        this.depth = Math.max(0, depth);
        this.quantity = Math.max(1, quantity);
    }

    boolean addParent(String parentId)
    {
        if (parentId != null && !parentId.isEmpty() && !parentId.equals(id))
            return parentIds.add(parentId);
        return false;
    }

    void addQuantity(int additional)
    {
        if (additional <= 0) return;
        quantity = quantity > Integer.MAX_VALUE - additional
                ? Integer.MAX_VALUE : quantity + additional;
    }

    public String getId() { return id; }
    public GoalNodeKind getKind() { return kind; }
    public String getAction() { return action; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public int getDepth() { return depth; }
    public int getQuantity() { return quantity; }
    public Set<String> getParentIds()
    {
        return Collections.unmodifiableSet(parentIds);
    }
}
