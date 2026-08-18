package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Recursively resolves verified resource recipes with strict termination. */
@Singleton
public class ResourceDependencyResolver
{
    public static final int DEFAULT_MAX_DEPTH = 8;
    private final ResourceAcquisitionPlanner ownershipPlanner;
    private final ResourceDependencyCatalog catalog;
    private final int maxDepth;

    @Inject
    public ResourceDependencyResolver(ResourceAcquisitionPlanner ownershipPlanner,
            ResourceDependencyCatalog catalog)
    {
        this(ownershipPlanner, catalog, DEFAULT_MAX_DEPTH);
    }

    ResourceDependencyResolver(ResourceAcquisitionPlanner ownershipPlanner,
            ResourceDependencyCatalog catalog, int maxDepth)
    {
        this.ownershipPlanner = ownershipPlanner;
        this.catalog = catalog;
        this.maxDepth = Math.max(1, maxDepth);
    }

    public ResourceDependencyResolution resolve(StrategyContext context,
            ResourceNeed root)
    {
        State state = new State();
        visit(context, root, 0, new HashSet<>(), state);
        return new ResourceDependencyResolution(new ArrayList<>(state.nodes.values()),
                state.cycle, state.depth, state.cost);
    }

    private void visit(StrategyContext context, ResourceNeed need, int depth,
            Set<String> active, State state)
    {
        if (need == null) return;
        String id = "resource:" + need.getItemId();
        if (active.contains(id))
        {
            state.cycle = true;
            add(state, id + ":cycle", "Stop: the resource route contains a cycle.",
                    RecommendationConfidence.CHECK_NEEDED, depth);
            return;
        }
        if (state.nodes.containsKey(id)) return;
        if (depth > maxDepth)
        {
            state.depth = true;
            add(state, id + ":depth", "Stop: the resource route exceeds the safe planning depth.",
                    RecommendationConfidence.CHECK_NEEDED, depth);
            return;
        }

        ResourceAcquisitionPlan ownership = ownershipPlanner.plan(context, need);
        if (ownership != null && ownership.hasEnoughConfirmed())
        {
            // Retrieval-only UIM storage can prove quantity without proving that
            // the item is immediately usable. Preserve that preparation state.
            add(state, id, ownership.getNote(), ownership.getConfidence(), depth);
            return;
        }
        AccountMode mode = context == null ? AccountMode.UNKNOWN : context.getAccountMode();
        if (mode.usesGrandExchange())
        {
            add(state, id, ownership == null ? "Verify a purchase route." : ownership.getNote(),
                    RecommendationConfidence.CHECK_NEEDED, depth);
            return;
        }

        ResourceDependencyDefinition definition = catalog.forItem(need.getItemId());
        if (definition == null)
        {
            add(state, id, ownership == null ? "Verify a self-source route." : ownership.getNote(),
                    RecommendationConfidence.CHECK_NEEDED, depth);
            return;
        }
        if (rejectForOpportunityCost(context, definition.getOpportunityCost()))
        {
            state.cost = true;
            add(state, id, "Use a shorter direct source; this detour costs too much for the current session.",
                    RecommendationConfidence.CHECK_NEEDED, depth);
            return;
        }

        active.add(id);
        for (DependencyRequirement requirement : definition.getPrerequisites())
            visitRequirement(context, requirement, depth + 1, active, state);
        active.remove(id);
        add(state, id, definition.getAction(), RecommendationConfidence.CHECK_NEEDED, depth);
    }

    private void visitRequirement(StrategyContext context,
            DependencyRequirement requirement, int depth, Set<String> active,
            State state)
    {
        if (requirement.getKind() == DependencyRequirement.Kind.RESOURCE)
        {
            visit(context, requirement.getResource(), depth, active, state);
            return;
        }
        if (state.nodes.containsKey(requirement.getId())) return;
        boolean verified = false;
        StrategyDataBundle data = context == null ? null : context.getData();
        if (data != null && data.getAccount() != null)
        {
            switch (requirement.getKind())
            {
                case QUEST:
                    verified = data.getQuests() != null
                            && data.getQuests().statusOf(requirement.getLabel()) == QuestStatus.COMPLETE;
                    break;
                case SKILL:
                    verified = data.getAccount().getSkillLevel(requirement.getSkill())
                            >= requirement.getLevel();
                    break;
                case GEAR:
                    verified = new ObservedItemIndex(data,
                            context.isUseGroupStorage()).has(requirement.getLabel());
                    break;
                default:
                    break;
            }
        }
        String action;
        if (verified) action = "Verified: " + requirement.getLabel() + ".";
        else if (requirement.getKind() == DependencyRequirement.Kind.QUEST)
            action = "Complete " + requirement.getLabel() + ".";
        else if (requirement.getKind() == DependencyRequirement.Kind.SKILL)
            action = "Train " + requirement.getLabel() + " using the current legal method.";
        else action = "Equip or obtain " + requirement.getLabel() + ".";
        add(state, requirement.getId(), action, verified
                ? RecommendationConfidence.VERIFIED
                : RecommendationConfidence.CHECK_NEEDED, depth);
    }

    private static boolean rejectForOpportunityCost(StrategyContext context, int cost)
    {
        if (context == null) return cost > 20;
        int limit = context.getSessionIntent() == SessionIntent.LONG_SESSION ? 70 : 35;
        if (context.getStrategyMode() == StrategyMode.RELAXED) limit += 10;
        return cost > limit;
    }

    private static void add(State state, String id, String action,
            RecommendationConfidence confidence, int depth)
    {
        state.nodes.putIfAbsent(id,
                new ResolvedDependencyNode(id, action, confidence, depth));
    }

    private static final class State
    {
        private final Map<String, ResolvedDependencyNode> nodes = new LinkedHashMap<>();
        private boolean cycle;
        private boolean depth;
        private boolean cost;
    }
}
