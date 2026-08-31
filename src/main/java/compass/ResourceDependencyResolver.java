package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Recursively resolves verified resource recipes with strict termination. */
@Singleton
public class ResourceDependencyResolver
{
    public static final int DEFAULT_MAX_DEPTH = 8;
    public static final int DEFAULT_MAX_NODES = 128;
    private final ResourceAcquisitionPlanner ownershipPlanner;
    private final ResourceDependencyCatalog catalog;
    private final int maxDepth;
    private final int maxNodes;

    @Inject
    public ResourceDependencyResolver(ResourceAcquisitionPlanner ownershipPlanner,
            ResourceDependencyCatalog catalog)
    {
        this(ownershipPlanner, catalog, DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);
    }

    ResourceDependencyResolver(ResourceAcquisitionPlanner ownershipPlanner,
            ResourceDependencyCatalog catalog, int maxDepth)
    {
        this(ownershipPlanner, catalog, maxDepth, DEFAULT_MAX_NODES);
    }

    ResourceDependencyResolver(ResourceAcquisitionPlanner ownershipPlanner,
            ResourceDependencyCatalog catalog, int maxDepth, int maxNodes)
    {
        this.ownershipPlanner = ownershipPlanner;
        this.catalog = catalog;
        this.maxDepth = Math.max(1, maxDepth);
        this.maxNodes = Math.max(1, maxNodes);
    }

    public DependencyResolution resolve(StrategyContext context,
            ResourceNeed root)
    {
        var state = new State(maxNodes);
        visit(context, root, 0, new HashSet<>(), state, false);
        return result(state);
    }

    /**
     * Resolves a root quantity already proven missing by another evaluator.
     * Child dependencies still use normal ownership checks.
     */
    public DependencyResolution resolveKnownShortfall(
            StrategyContext context, ResourceNeed root)
    {
        var state = new State(maxNodes);
        visit(context, root, 0, new HashSet<>(), state, true);
        return result(state);
    }

    private static DependencyResolution result(State state)
    {
        return new DependencyResolution(new ArrayList<>(state.nodes.values()),
                state.cycle, state.depth, state.cost, state.nodeLimit);
    }

    private void visit(StrategyContext context, ResourceNeed need, int depth,
            Set<String> active, State state, boolean knownShortfall)
    {
        if (need == null) return;
        if (state.nodes.size() >= maxNodes)
        {
            state.nodeLimit = true;
            return;
        }
        var id = "resource:" + need.getItemId();
        if (active.contains(id))
        {
            state.cycle = true;
            addResource(state, id + ":cycle", Text.get(610),
                    Confidence.CHECK_NEEDED, depth, need.getQuantity());
            return;
        }
        var previousRequested = state.requested.getOrDefault(need.getItemId(), 0);
        var totalRequested = safeAdd(previousRequested, need.getQuantity());
        state.requested.put(need.getItemId(), totalRequested);
        var previousProcessed = state.processed.getOrDefault(need.getItemId(), 0);
        if (totalRequested <= previousProcessed) return;
        state.processed.put(need.getItemId(), totalRequested);
        ResourceNeed totalNeed = new ResourceNeed(need.getItemId(),
                need.getItemName(), totalRequested);
        if (depth > maxDepth)
        {
            state.depth = true;
            addResource(state, id + ":depth", Text.get(611),
                    Confidence.CHECK_NEEDED, depth, totalRequested);
            return;
        }

        AcquisitionPlan ownership = knownShortfall
                ? ownershipPlanner.planKnownShortfall(context, totalNeed)
                : ownershipPlanner.plan(context, totalNeed);
        if (ownership != null && ownership.hasEnoughConfirmed())
        {
            // Retrieval-only UIM storage can prove quantity without proving that
            // the item is immediately usable. Preserve that preparation state.
            addResource(state, id, ownership.getNote(), ownership.getConfidence(),
                    depth, totalRequested);
            return;
        }
        var mode = context == null ? AccountMode.UNKNOWN : context.accountMode();
        if (mode.usesGrandExchange())
        {
            addResource(state, id, ownership == null ? Text.get(1330) : ownership.getNote(),
                    Confidence.CHECK_NEEDED, depth, totalRequested);
            return;
        }

        int confirmedOwned = knownShortfall || ownership == null
                ? 0 : Math.min(totalRequested, ownership.getConfirmedQuantity());
        var unresolvedRequested = Math.max(0, totalRequested - confirmedOwned);
        int previousUnresolved = knownShortfall
                ? previousProcessed
                : Math.max(0, previousProcessed - confirmedOwned);

        var definition = catalog.forItem(need.getItemId());
        if (definition == null)
        {
            addResource(state, id, ownership == null ? Text.get(1331) : ownership.getNote(),
                    Confidence.CHECK_NEEDED, depth, totalRequested);
            return;
        }
        if (rejectForOpportunityCost(context, definition.getOpportunityCost()))
        {
            state.cost = true;
            addResource(state, id, Text.get(612),
                    Confidence.CHECK_NEEDED, depth, totalRequested);
            return;
        }

        active.add(id);
        int batches = ceilDiv(unresolvedRequested, definition.getOutputQuantity())
                - ceilDiv(previousUnresolved, definition.getOutputQuantity());
        Map<Integer, ResourceNeed> resourceNeeds = new LinkedHashMap<>();
        for (DependencyRequirement requirement : definition.getPrerequisites())
        {
            if (requirement.getKind() != DependencyRequirement.Kind.RESOURCE)
            {
                visitRequirement(context, requirement, depth + 1, active, state);
                continue;
            }
            if (batches <= 0) continue;
            var child = requirement.getResource();
            var required = safeMultiply(child.getQuantity(), batches);
            var prior = resourceNeeds.get(child.getItemId());
            int combined = prior == null ? required
                    : safeAdd(prior.getQuantity(), required);
            resourceNeeds.put(child.getItemId(), new ResourceNeed(
                    child.getItemId(), child.getItemName(), combined));
        }
        for (ResourceNeed child : resourceNeeds.values())
            visit(context, child, depth + 1, active, state, false);
        active.remove(id);
        addResource(state, id, definition.getAction(),
                Confidence.CHECK_NEEDED, depth, totalRequested);
    }

    private void visitRequirement(StrategyContext context,
            DependencyRequirement requirement, int depth, Set<String> active,
            State state)
    {
        if (requirement.getKind() == DependencyRequirement.Kind.RESOURCE)
        {
            visit(context, requirement.getResource(), depth, active, state, false);
            return;
        }
        if (state.nodes.containsKey(requirement.id)) return;
        if (state.nodes.size() >= maxNodes)
        {
            state.nodeLimit = true;
            return;
        }
        var verified = false;
        var data = context == null ? null : context.data();
        if (data != null && data.account() != null)
        {
            switch (requirement.getKind())
            {
                case QUEST:
                    verified = data.quests() != null
                            && data.quests().statusOf(requirement.getLabel()) == QuestStatus.COMPLETE;
                    break;
                case SKILL:
                    verified = data.account().level(requirement.getSkill())
                            >= requirement.getLevel();
                    break;
                case GEAR:
                    verified = new ItemIndex(data,
                            context.usesGroupStorage()).has(requirement.getLabel());
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
            action = "Train " + requirement.getLabel() + Text.get(1332);
        else action = Text.get(1925) + requirement.getLabel() + ".";
        add(state, requirement.id, action, verified
                ? Confidence.VERIFIED
                : Confidence.CHECK_NEEDED, depth);
    }

    private static boolean rejectForOpportunityCost(StrategyContext context, int cost)
    {
        if (context == null) return cost > 20;
        var limit = context.intent() == SessionIntent.LONG_SESSION ? 70 : 35;
        if (context.mode() == StrategyMode.RELAXED) limit += 10;
        return cost > limit;
    }

    private static void add(State state, String id, String action,
            Confidence confidence, int depth)
    {
        if (!state.nodes.containsKey(id) && state.nodes.size() >= state.maxNodes)
        {
            state.nodeLimit = true;
            return;
        }
        state.nodes.putIfAbsent(id,
                new ResolvedDependencyNode(id, action, confidence, depth));
    }

    private static void addResource(State state, String id, String action,
            Confidence confidence, int depth, int quantity)
    {
        if (!state.nodes.containsKey(id) && state.nodes.size() >= state.maxNodes)
        {
            state.nodeLimit = true;
            return;
        }
        state.nodes.put(id,
                new ResolvedDependencyNode(id, action, confidence, depth, quantity));
    }

    private static int ceilDiv(int value, int divisor)
    {
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }

    private static int safeMultiply(int left, int right)
    {
        if (left > Integer.MAX_VALUE / Math.max(1, right)) return Integer.MAX_VALUE;
        return left * right;
    }

    private static int safeAdd(int left, int right)
    {
        if (left > Integer.MAX_VALUE - right) return Integer.MAX_VALUE;
        return left + right;
    }

    private static final class State
    {
        private final int maxNodes;
        private final Map<String, ResolvedDependencyNode> nodes = new LinkedHashMap<>();
        private final Map<Integer, Integer> requested = new HashMap<>();
        private final Map<Integer, Integer> processed = new HashMap<>();
        private boolean cycle;
        private boolean depth;
        private boolean cost;
        private boolean nodeLimit;

        private State(int maxNodes)
        {
            this.maxNodes = maxNodes;
        }
    }
}
