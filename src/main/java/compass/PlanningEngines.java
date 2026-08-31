package compass;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static compass.Text.get;

/**
 * Shared account-aware material planner for deterministic recommendations.
 *
 * <p>This is the single place that decides whether an observed item can satisfy
 * a milestone plan. Main, Iron, GIM, and UIM guidance therefore use the same
 * shortfall math instead of each skill reimplementing bank semantics.</p>
 */
@Singleton
class AccountResourcePlanner
{
    private final PurchaseCostAdvisor purchaseCostAdvisor;
    private final MainEconomyPlanner mainEconomyPlanner;
    private final ResourceSourceCatalog resourceSourceCatalog;

    @Inject
    public AccountResourcePlanner(PurchaseCostAdvisor purchaseCostAdvisor,
            MainEconomyPlanner mainEconomyPlanner,
            ResourceSourceCatalog resourceSourceCatalog)
    {
        this.purchaseCostAdvisor = purchaseCostAdvisor;
        this.mainEconomyPlanner = mainEconomyPlanner;
        this.resourceSourceCatalog = resourceSourceCatalog;
    }

    public AccountResourcePlanner(PurchaseCostAdvisor purchaseCostAdvisor)
    {
        this(purchaseCostAdvisor, new MainEconomyPlanner(),
                new ResourceSourceCatalog());
    }

    /** Test/compatibility constructor that deliberately omits live prices. */
    public AccountResourcePlanner()
    {
        this(null, new MainEconomyPlanner(), new ResourceSourceCatalog());
    }

    public SupplyPlan plan(
            GameData data,
            List<MethodInput> rawNeeds,
            boolean useGroupStorage)
    {
        var account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.modeCode());
        var observed = new ItemIndex(data, useGroupStorage);
        var primaryObserved = observed.usableOwnershipObserved();
        var groupIncluded = useGroupStorage && mode.isGroupIronman();
        var groupObserved = observed.groupStorageObserved();

        var needs = merge(rawNeeds);
        List<ResourcePlanEntry> entries = new ArrayList<>();
        for (MethodInput need : needs)
        {
            var reusable = reusableSourceFor(observed, need.getName());
            var owned = observed.quantity(need.getName());
            var restricted = observed.restrictedQuantity(need.getName());
            int missing = reusable == null
                    ? Math.max(0, need.getQuantity() - owned)
                    : 0;
            entries.add(new ResourcePlanEntry(
                    need.getName(),
                    need.getItemId(),
                    need.getQuantity(),
                    owned,
                    missing,
                    restricted,
                    reusable));
        }

        String guidance = buildGuidance(
                data,
                mode,
                primaryObserved,
                groupIncluded,
                groupObserved,
                entries);
        return new SupplyPlan(
                mode,
                primaryObserved,
                groupIncluded,
                groupObserved,
                entries,
                guidance);
    }

    private String buildGuidance(
            GameData data,
            AccountMode mode,
            boolean primaryObserved,
            boolean groupIncluded,
            boolean groupObserved,
            List<ResourcePlanEntry> entries)
    {
        if (entries.isEmpty())
        {
            return get(69);
        }

        List<String> required = new ArrayList<>();
        List<String> verified = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> reusable = new ArrayList<>();
        List<String> restricted = new ArrayList<>();
        List<MethodInput> missingInputs = new ArrayList<>();

        for (ResourcePlanEntry entry : entries)
        {
            required.add(format(entry.getRequired()) + " " + entry.getName());
            if (entry.isSatisfiedByReusableSource())
            {
                reusable.add(entry.getName() + " supplied by "
                        + entry.getReusableSource());
            }
            else
            {
                verified.add(format(entry.getUsableOwned()) + " "
                        + entry.getName());
            }

            if (entry.getMissing() > 0)
            {
                missing.add(format(entry.getMissing()) + " " + entry.getName());
                missingInputs.add(entry.missingInput());
            }
            if (entry.getRestrictedOwned() > 0)
            {
                restricted.add(format(entry.getRestrictedOwned()) + " "
                        + entry.getName());
            }
        }

        var text = new StringBuilder();
        text.append("Need ").append(join(required)).append(". ");
        if (!reusable.isEmpty())
        {
            text.append(get(1588)).append(join(reusable)).append(". ");
        }

        // An unobserved ownership surface is unknown, never empty. UIM uses
        // inventory/equipment evidence instead of the conventional bank.
        if (!primaryObserved)
        {
            if (mode == AccountMode.ULTIMATE_IRONMAN)
                text.append(get(80));
            else
                text.append(get(1376))
                        .append(get(81));
            if (groupIncluded && !groupObserved)
            {
                text.append(get(82));
            }
            return text.toString();
        }

        if (!verified.isEmpty())
        {
            text.append(get(1589))
                    .append(join(verified)).append(". ");
        }

        if (missing.isEmpty())
        {
            text.append(get(83));
            if (groupIncluded && groupObserved)
            {
                text.append(get(84));
            }
            appendRestrictedUimNote(text, restricted);
            return text.toString();
        }

        var shortfall = join(missing);
        if (mode.usesGrandExchange())
        {
            appendMainOpportunityGuidance(text, data, shortfall,
                    missingInputs);
        }
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            List<String> routes = sourceRoutes(missingInputs, mode,
                    data == null || data.account() == null
                            ? MembershipStatus.UNKNOWN
                            : data.account().membership(), true);
            text.append(get(1377)).append(shortfall)
                    .append(get(85));
            if (!routes.isEmpty())
                text.append(" Route: ").append(routes.get(0));
            text.append(get(86));
        }
        else if (mode.isGroupIronman())
        {
            text.append("Self-source ").append(shortfall)
                    .append(get(1378));
            if (groupIncluded && groupObserved)
            {
                text.append(get(87));
            }
            else if (groupIncluded)
            {
                text.append(get(70));
            }
        }
        else if (mode.isIronLike())
        {
            List<String> routes = sourceRoutes(missingInputs, mode,
                    data == null || data.account() == null
                            ? MembershipStatus.UNKNOWN
                            : data.account().membership(), false);
            text.append("Self-source ").append(shortfall)
                    .append(get(1378));
            if (!routes.isEmpty())
                text.append(" Route: ").append(routes.get(0));
        }
        else
        {
            text.append("Source ").append(shortfall)
                    .append(get(71));
        }

        appendRestrictedUimNote(text, restricted);
        return text.toString();
    }

    private void appendMainOpportunityGuidance(StringBuilder text,
            GameData data, String shortfall,
            List<MethodInput> missingInputs)
    {
        PurchaseCostEstimate estimate = purchaseCostAdvisor == null
                ? PurchaseCostEstimate.unknown()
                : purchaseCostAdvisor.estimate(missingInputs);
        List<String> routes = mainRoutes(missingInputs,
                data == null || data.account() == null
                        ? MembershipStatus.UNKNOWN
                        : data.account().membership());
        MainPurchaseDecision decision = mainEconomyPlanner == null
                ? null : mainEconomyPlanner.evaluateUnmeasuredPurchase(
                        data == null ? null : data.economy(), estimate,
                        !routes.isEmpty());

        if (decision != null && decision.getChoice() == MainPurchaseChoice.BUY)
        {
            text.append("Buy ").append(shortfall)
                    .append(get(1379))
                    .append(get(1380))
                    .append(format(decision.getTotalCost()))
                    .append(get(72))
                    .append(format(decision.getObservedCoins()))
                    .append(get(1381));
            return;
        }

        if (decision != null
                && decision.getChoice() == MainPurchaseChoice.SELF_SOURCE)
        {
            text.append("Self-source ").append(shortfall).append(". ")
                    .append(get(1382))
                    .append(format(decision.getTotalCost()))
                    .append(" of ")
                    .append(format(decision.getObservedCoins()))
                    .append(get(73))
                    .append(routes.get(0));
            return;
        }

        if (decision != null && decision.getChoice()
                == MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES)
        {
            text.append(get(74))
                    .append(format(decision.getTotalCost()))
                    .append(get(1590))
                    .append(format(decision.getObservedCoins()))
                    .append(get(1383));
            if (!routes.isEmpty())
                text.append(get(75))
                        .append(routes.get(0));
            return;
        }

        text.append(get(76))
                .append(get(77));
        if (!routes.isEmpty())
            text.append(get(1384)).append(routes.get(0));
    }

    private List<String> mainRoutes(List<MethodInput> missingInputs,
            MembershipStatus membership)
    {
        if (resourceSourceCatalog == null || missingInputs == null)
            return java.util.Collections.emptyList();
        List<String> routes = new ArrayList<>();
        for (MethodInput input : missingInputs)
        {
            for (String route : resourceSourceCatalog.suggestions(
                    input.getName(), AccountMode.MAIN, membership, false))
            {
                if (!routes.contains(route)) routes.add(route);
            }
        }
        return routes;
    }

    private List<String> sourceRoutes(List<MethodInput> missingInputs,
            AccountMode mode, MembershipStatus membership,
            boolean justInTime)
    {
        if (resourceSourceCatalog == null || missingInputs == null)
            return java.util.Collections.emptyList();
        List<String> routes = new ArrayList<>();
        for (MethodInput input : missingInputs)
            for (String route : resourceSourceCatalog.suggestions(
                    input.getName(), mode, membership, justInTime))
                if (!routes.contains(route)) routes.add(route);
        return routes;
    }

    private static void appendRestrictedUimNote(
            StringBuilder text,
            List<String> restricted)
    {
        if (restricted == null || restricted.isEmpty()) return;
        text.append(get(78))
                .append(join(restricted))
                .append(get(79));
    }

    /**
     * Only currently equipped reusable rune sources waive rune consumption.
     * Owning a staff in a bank is not enough because build requirements and the
     * recommendation's intended equipment setup may make it unusable.
     */
    private static String reusableSourceFor(
            ItemIndex observed,
            String itemName)
    {
        if (observed == null || itemName == null) return null;
        var rune = Names.lower(itemName);
        if ("fire rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of fire", get(1591), get(1592),
                    get(1593), get(1594),
                    get(1595), get(1385),
                    get(1596), get(1386),
                    "Tome of fire");
        }
        if ("water rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of water", get(1597), get(1387),
                    "Mud battlestaff", get(1598),
                    get(1595), get(1385),
                    get(1599), get(1600));
        }
        if ("earth rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of earth", get(1601), get(1388),
                    get(1593), get(1594),
                    "Mud battlestaff", get(1598),
                    get(1602), get(1603));
        }
        if ("air rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of air", "Air battlestaff", get(1604),
                    get(1596), get(1386),
                    get(1599), get(1600),
                    get(1602), get(1603));
        }
        return null;
    }

    private static String firstEquipped(
            ItemIndex observed,
            String... candidates)
    {
        for (String candidate : candidates)
        {
            if (observed.equipped(candidate)) return candidate;
        }
        return null;
    }

    /** Merge duplicate recipe rows before comparing them with storage. */
    private static List<MethodInput> merge(
            List<MethodInput> rawNeeds)
    {
        if (rawNeeds == null || rawNeeds.isEmpty()) return new ArrayList<>();
        Map<String, MutableNeed> merged = new LinkedHashMap<>();
        for (MethodInput input : rawNeeds)
        {
            if (input == null || input.getName() == null
                    || input.getName().trim().isEmpty()
                    || input.getQuantity() <= 0)
            {
                continue;
            }
            var key = Names.lower(input.getName());
            var existing = merged.get(key);
            if (existing == null)
            {
                merged.put(key, new MutableNeed(
                        input.getName().trim(),
                        input.getItemId(),
                        input.getQuantity()));
            }
            else
            {
                existing.quantity = safeAdd(
                        existing.quantity, input.getQuantity());
            }
        }

        List<MethodInput> result = new ArrayList<>();
        for (MutableNeed need : merged.values())
        {
            result.add(new MethodInput(
                    need.name, need.itemId, need.quantity));
        }
        return result;
    }

    private static int safeAdd(int a, int b)
    {
        if (a >= Integer.MAX_VALUE - b) return Integer.MAX_VALUE;
        return a + b;
    }

    private static String join(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);
        var text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0) text.append(i == parts.size() - 1 ? ", and " : ", ");
            text.append(parts.get(i));
        }
        return text.toString();
    }

    private static String format(long value)
    {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }


    private static final class MutableNeed
    {
        private final String name;
        private final int itemId;
        private int quantity;

        private MutableNeed(String name, int itemId, int quantity)
        {
            this.name = name;
            this.itemId = itemId;
            this.quantity = Math.max(0, quantity);
        }
    }
}

/**
 * Resolves the best Agility course from level, membership, quest state, and
 * direct region observations. Direct observation is the strongest evidence.
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class AgilityAccessEvaluator
{
    private final AgilityCourseCatalog catalog;

    public AgilityCourseDefinition bestStandardCourse(GameData data)
    {
        AgilityCourseDefinition best = null;
        for (AgilityCourseDefinition course : catalog.all())
        {
            if (course.isWilderness() || !isVerifiedAvailable(data, course))
            {
                continue;
            }
            if (best == null
                    || course.getRequiredLevel() > best.getRequiredLevel())
            {
                best = course;
            }
        }
        return best;
    }

    public RequirementCheck courseCheck(
            GameData data,
            AgilityCourseDefinition course)
    {
        if (course == null)
        {
            return new RequirementCheck(
                    "agility:course",
                    get(1390),
                    RequirementState.CHECK_NEEDED,
                    get(0)
            );
        }

        var account = data == null ? null : data.account();
        if (account == null)
        {
            return unknown(course, get(1391));
        }

        var level = account.level(Skill.AGILITY);
        if (level < course.getRequiredLevel())
        {
            return new RequirementCheck(
                    "agility:" + course.id,
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    "Requires " + course.getRequiredLevel()
                            + get(1392) + level + "."
            );
        }

        if (account.membership() != MembershipStatus.P2P)
        {
            return new RequirementCheck(
                    "agility:" + course.id,
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    get(1)
            );
        }

        var memory = data.accessMemory();
        if (memory != null && memory.hasObserved(course.observationKey()))
        {
            return verified(
                    course,
                    get(2)
            );
        }

        var quest = course.getRequiredQuest();
        if (quest != null)
        {
            var quests = data.quests();
            QuestStatus status = quests == null
                    ? QuestStatus.UNKNOWN
                    : quests.statusOf(quest);

            if (status == QuestStatus.COMPLETE)
            {
                return verified(
                        course,
                        quest + get(3)
                );
            }
            if (status == QuestStatus.NOT_STARTED
                    || status == QuestStatus.IN_PROGRESS)
            {
                return new RequirementCheck(
                        "agility:" + course.id,
                        course.getDisplayName(),
                        RequirementState.BLOCKED,
                        quest + get(1690)
                );
            }
            return unknown(
                    course,
                    get(1691) + quest + get(1393)
            );
        }

        return verified(
                course,
                get(4)
        );
    }

    public RequirementCheck wildernessCourseCheck(GameData data)
    {
        return courseCheck(data, catalog.wildernessCourse());
    }

    private boolean isVerifiedAvailable(
            GameData data,
            AgilityCourseDefinition course)
    {
        return courseCheck(data, course).getState() == RequirementState.VERIFIED;
    }

    private RequirementCheck verified(
            AgilityCourseDefinition course,
            String evidence)
    {
        return new RequirementCheck(
                "agility:" + course.id,
                course.getDisplayName(),
                RequirementState.VERIFIED,
                evidence
        );
    }

    private RequirementCheck unknown(
            AgilityCourseDefinition course,
            String evidence)
    {
        return new RequirementCheck(
                "agility:" + course.id,
                course.getDisplayName(),
                RequirementState.CHECK_NEEDED,
                evidence
        );
    }
}

/**
 * Resolves Farming access from the strongest evidence available.
 *
 * <p>Direct observation wins. Otherwise, open-world patches are inferred for a
 * member account and quest-gated patches are inferred only when the required
 * quest is confirmed complete. This is the pattern other content systems can
 * follow later: live state first, remembered proof second, safe inference third.</p>
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class FarmingAccessEvaluator
{
    private final FarmingAccessCatalog catalog;

    public FarmingSnapshot evaluate(
            AccountSnapshot account,
            QuestSnapshot quests,
            AccessMemorySnapshot memory,
            FarmingSnapshot existing)
    {
        Set<String> reachable = new HashSet<>();
        Map<String, CapabilityState> tools = new HashMap<>();
        Map<String, Long> readyAt = new HashMap<>();

        if (existing != null)
        {
            reachable.addAll(existing.getReachablePatchIds());
            tools.putAll(existing.getLeprechaunTools());
            readyAt.putAll(existing.getPatchReadyAtMillis());
        }

        if (account == null
                || account.membership() != MembershipStatus.P2P)
        {
            return new FarmingSnapshot(reachable, tools, readyAt);
        }

        AccessMemorySnapshot safeMemory = memory == null
                ? AccessMemorySnapshot.empty()
                : memory;

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (safeMemory.hasObserved(definition.observationKey()))
            {
                reachable.add(definition.id);
                continue;
            }

            var requiredQuest = definition.getRequiredQuest();
            if (requiredQuest == null)
            {
                reachable.add(definition.id);
                continue;
            }

            if (quests != null
                    && quests.statusOf(requiredQuest) == QuestStatus.COMPLETE)
            {
                reachable.add(definition.id);
            }
        }

        return new FarmingSnapshot(reachable, tools, readyAt);
    }

    public String firstReachablePatchName(FarmingSnapshot farming)
    {
        if (farming == null)
        {
            return null;
        }

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (farming.isPatchReachable(definition.id))
            {
                return definition.getDisplayName();
            }
        }
        return null;
    }

    public String firstReachableHerbPatchName(FarmingSnapshot farming)
    {
        if (farming == null)
        {
            return null;
        }

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (definition.isHerbPatch()
                    && farming.isPatchReachable(definition.id))
            {
                return definition.getDisplayName();
            }
        }
        return null;
    }
}

/** Builds the current herb/tree checklist from verified access and resources. */
@Singleton
class FarmingRunPlanner
{
    private final FarmingRunCatalog catalog;
    private final FarmingSupplyCatalog supplyCatalog;
    private final ResourceReadinessService resources;

    @Inject
    public FarmingRunPlanner(
            FarmingRunCatalog catalog,
            FarmingSupplyCatalog supplyCatalog,
            ResourceReadinessService resources)
    {
        this.catalog = catalog;
        this.supplyCatalog = supplyCatalog;
        this.resources = resources;
    }

    /** Compatibility constructor retained for focused tests. */
    public FarmingRunPlanner(FarmingRunCatalog catalog)
    {
        this(catalog, new FarmingSupplyCatalog(), new ResourceReadinessService());
    }

    public GuidanceChecklist build(GameData data, String activityId)
    {
        List<GuidanceStep> steps = new ArrayList<>();
        if (data == null || data.account() == null)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    get(1463), steps);
        }

        var account = data.account();
        var farmingLevel = account.level(Skill.FARMING);
        if (account.membership() != MembershipStatus.P2P)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    get(1464), steps);
        }

        appendPrep(steps, data, farmingLevel);
        FarmingRunSnapshot snapshot = data.farmingRuns() == null
                ? FarmingRunSnapshot.empty() : data.farmingRuns();

        for (FarmingRunPatchDefinition patch : catalog.all())
        {
            if (farmingLevel < patch.getMinimumLevel()
                    || !isConfirmedReachable(data, patch)) continue;
            steps.add(stepFor(patch, snapshot.stateOf(patch.id)));
        }

        return new GuidanceChecklist(
                activityId, "Farming run",
                get(244), steps);
    }

    private void appendPrep(
            List<GuidanceStep> steps,
            GameData data,
            int farmingLevel)
    {
        var farming = data.farming();
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.rake(), toolState(farming, "rake"),
                get(245)));
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.dibber(), toolState(farming, "dibber"),
                get(246)));
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.spade(), toolState(farming, "spade"),
                get(247)));

        if (farmingLevel >= 9)
        {
            appendResource(steps, resources.evaluate(
                    data, supplyCatalog.herbSeedsForLevel(farmingLevel)));
        }
        if (farmingLevel >= 15)
        {
            appendResource(steps, resources.evaluate(
                    data, supplyCatalog.treeSaplingsForLevel(farmingLevel)));
        }
    }

    private CapabilityState toolState(FarmingSnapshot farming, String id)
    {
        return farming == null
                ? CapabilityState.UNKNOWN
                : farming.leprechaunToolState(id);
    }

    private void appendResource(
            List<GuidanceStep> steps,
            RequirementCheck check)
    {
        GuidanceStepState state = check.getState() == RequirementState.VERIFIED
                ? GuidanceStepState.COMPLETE
                : GuidanceStepState.CHECK_NEEDED;
        steps.add(new GuidanceStep(
                check.id, "Prep • " + check.getLabel(),
                check.getEvidence(), state));
    }

    private boolean isConfirmedReachable(
            GameData data,
            FarmingRunPatchDefinition patch)
    {
        var memory = data.accessMemory();
        if (memory != null)
        {
            for (Integer region : patch.getRegionIds())
            {
                if (memory.hasObserved("region." + region)) return true;
            }
        }
        var quest = patch.getRequiredQuest();
        if (quest == null) return true;
        var quests = data.quests();
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private GuidanceStep stepFor(
            FarmingRunPatchDefinition patch,
            ObservedFarmingPatchState observed)
    {
        String prefix = patch.getKind() == FarmingPatchKind.HERB
                ? "Herb • " : "Tree • ";
        if (observed == null)
        {
            return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                    get(248),
                    GuidanceStepState.CHECK_NEEDED);
        }
        switch (observed.getState())
        {
            case GROWING:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        "Planted", GuidanceStepState.COMPLETE);
            case READY:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        patch.getKind() == FarmingPatchKind.TREE
                                ? get(1465) : get(1466),
                        GuidanceStepState.ACTION);
            case EMPTY:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        get(1695), GuidanceStepState.ACTION);
            case DISEASED:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        "Cure the crop", GuidanceStepState.WARNING);
            case DEAD:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        get(1696), GuidanceStepState.WARNING);
            case UNKNOWN:
            default:
                return new GuidanceStep(patch.id, prefix + patch.getDisplayName(),
                        get(1697), GuidanceStepState.CHECK_NEEDED);
        }
    }
}

/** Evaluates ALL/ANY item alternatives without treating unobserved storage as empty. */
final class ItemRequirementEvaluator
{
    public ItemRequirementResult evaluate(ItemRequirementExpression expression,
            GameData data, boolean useGroupStorage)
    {
        if (expression == null)
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        var items = new ItemIndex(data, useGroupStorage);
        return evaluate(expression, data, items, useGroupStorage);
    }

    private ItemRequirementResult evaluate(ItemRequirementExpression expression,
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM)
            return item(expression, data, items, useGroupStorage);
        if (expression.getKind() == ItemRequirementExpression.Kind.ITEM_CLASS)
            return itemClass(expression, data, items, useGroupStorage);
        if (expression.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED)
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    expression.getCheckAction());

        List<ItemRequirementResult> results = new ArrayList<>();
        for (ItemRequirementExpression child : expression.getChildren())
            results.add(evaluate(child, data, items, useGroupStorage));

        if (expression.getKind() == ItemRequirementExpression.Kind.ANY_OF)
        {
            for (ItemRequirementResult result : results)
                if (result.isSatisfied()) return result;

            boolean needsVerification = results.stream().anyMatch(result ->
                    result.getState() == RequirementState.CHECK_NEEDED);
            RequirementState state = needsVerification
                    ? RequirementState.CHECK_NEEDED : RequirementState.BLOCKED;
            // If an alternative may still exist in unobserved storage, there is
            // no proven shortfall to source. Once every branch is proven absent,
            // carry the smallest concrete branch into acquisition guidance.
            List<MethodInput> inputs = needsVerification
                    ? Collections.emptyList() : bestAlternativeInputs(results);
            return new ItemRequirementResult(state,
                    (needsVerification
                            ? get(1320) : "Get one of: ")
                            + expression.label(), inputs);
        }

        List<String> actions = new ArrayList<>();
        List<MethodInput> inputs = new ArrayList<>();
        var state = RequirementState.VERIFIED;
        for (ItemRequirementResult result : results)
        {
            if (!result.isSatisfied() && !result.getAction().isEmpty())
                actions.add(result.getAction());
            inputs.addAll(result.getMissingInputs());
            if (result.getState() == RequirementState.BLOCKED)
                state = RequirementState.BLOCKED;
            else if (result.getState() == RequirementState.CHECK_NEEDED
                    && state != RequirementState.BLOCKED)
                state = RequirementState.CHECK_NEEDED;
        }
        return new ItemRequirementResult(state, String.join("; ", actions), inputs);
    }

    private ItemRequirementResult item(ItemRequirementExpression expression,
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        var names = expression.getItemNames().toArray(new String[0]);
        int owned;
        boolean observed;
        switch (expression.getScope())
        {
            case EQUIPPED:
                owned = items.equippedQuantity(names);
                observed = data != null && data.equipment() != null;
                break;
            case CARRIED:
                owned = items.inventoryQuantity(names);
                observed = data != null && data.inventory() != null;
                break;
            case CARRIED_OR_EQUIPPED:
                owned = items.inventoryQuantity(names) + items.equippedQuantity(names);
                observed = data != null && data.inventory() != null
                        && data.equipment() != null;
                break;
            case OWNED_OR_RETRIEVABLE:
                owned = items.quantity(names) + items.restrictedQuantity(names);
                observed = ownershipObserved(data, items, useGroupStorage)
                        && !isUim(data);
                break;
            case IMMEDIATELY_USABLE:
            default:
                owned = items.quantity(names);
                observed = ownershipObserved(data, items, useGroupStorage);
                break;
        }
        if (owned >= expression.getQuantity())
            return new ItemRequirementResult(RequirementState.VERIFIED, "");

        var shortfall = Math.max(0, expression.getQuantity() - owned);
        var target = String.join(" or ", expression.getItemNames());
        if (expression.getItemNames().size() > 1) target = "(" + target + ")";
        String action = (observed ? "Get " : get(1321))
                + shortfall + " × " + target;

        List<MethodInput> inputs = Collections.emptyList();
        if (observed && expression.getItemNames().size() == 1)
        {
            inputs = Collections.singletonList(new MethodInput(
                    expression.getItemNames().get(0), -1, shortfall));
        }
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                action, inputs);
    }

    private ItemRequirementResult itemClass(ItemRequirementExpression expression,
            GameData data, ItemIndex items,
            boolean useGroupStorage)
    {
        var itemClass = expression.getItemClass();
        if (itemClass == null)
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    get(1322));
        if (itemClass == ItemRequirementClass.EMPTY_INVENTORY_SPACE)
            return freeInventorySlots(expression, data);
        if (!itemClass.isNameObservable())
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    get(1692) + expression.label());

        int owned;
        boolean observed;
        switch (expression.getScope())
        {
            case EQUIPPED:
                owned = items.equippedQuantityMatching(itemClass,
                        expression.getExcludedItemNames());
                observed = data != null && data.equipment() != null;
                break;
            case CARRIED:
                owned = items.inventoryQuantityMatching(itemClass,
                        expression.getExcludedItemNames());
                observed = data != null && data.inventory() != null;
                break;
            case CARRIED_OR_EQUIPPED:
                owned = items.inventoryQuantityMatching(itemClass,
                        expression.getExcludedItemNames())
                        + items.equippedQuantityMatching(itemClass,
                                expression.getExcludedItemNames());
                observed = data != null && data.inventory() != null
                        && data.equipment() != null;
                break;
            case OWNED_OR_RETRIEVABLE:
                owned = items.quantityMatching(itemClass,
                        expression.getExcludedItemNames())
                        + items.restrictedQuantityMatching(itemClass,
                                expression.getExcludedItemNames());
                observed = ownershipObserved(data, items, useGroupStorage)
                        && !isUim(data);
                break;
            case IMMEDIATELY_USABLE:
            default:
                owned = items.quantityMatching(itemClass,
                        expression.getExcludedItemNames());
                observed = ownershipObserved(data, items, useGroupStorage);
                break;
        }
        if (owned >= expression.getQuantity())
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        var shortfall = Math.max(0, expression.getQuantity() - owned);
        var target = itemClass.getLabel();
        if (!expression.getExcludedItemNames().isEmpty())
            target += " (excluding " + String.join(" or ",
                    expression.getExcludedItemNames()) + ")";
        return new ItemRequirementResult(observed
                ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                (observed ? "Get " : get(1321)) + shortfall
                        + " × " + target);
    }

    private static ItemRequirementResult freeInventorySlots(
            ItemRequirementExpression expression, GameData data)
    {
        var inventory = data == null ? null : data.inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return new ItemRequirementResult(RequirementState.CHECK_NEEDED,
                    get(332));
        var required = Math.max(1, expression.getQuantity());
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        if (free >= required)
            return new ItemRequirementResult(RequirementState.VERIFIED, "");
        return new ItemRequirementResult(RequirementState.BLOCKED,
                get(1323) + required
                        + get(1324) + free
                        + get(333));
    }

    private static List<MethodInput> bestAlternativeInputs(
            List<ItemRequirementResult> results)
    {
        List<MethodInput> best = Collections.emptyList();
        var bestCost = Long.MAX_VALUE;
        for (ItemRequirementResult result : results)
        {
            if (result.getMissingInputs().isEmpty()) continue;
            var cost = 0L;
            for (MethodInput input : result.getMissingInputs())
                cost += Math.max(1, input.getQuantity());
            if (cost < bestCost)
            {
                bestCost = cost;
                best = result.getMissingInputs();
            }
        }
        return best;
    }

    private static boolean ownershipObserved(GameData data,
            ItemIndex items, boolean useGroupStorage)
    {
        if (data == null || data.account() == null) return false;
        return items.usableOwnershipObserved();
    }

    private static boolean isUim(GameData data)
    {
        return data != null && data.account() != null
                && AccountMode.fromTypeCode(data.account().modeCode())
                        == AccountMode.ULTIMATE_IRONMAN;
    }

}

/** Conservative Main-account buy-vs-gather decision layer. */
@Singleton
class MainEconomyPlanner
{
    private static final long MINIMUM_LIQUID_BUFFER = 10_000L;

    public MainPurchaseDecision evaluatePurchase(
            StrategyContext context,
            MainPurchaseCandidate candidate)
    {
        if (context == null || candidate == null)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED, 0L, 0L,
                    Confidence.CHECK_NEEDED,
                    get(1389));
        }

        if (context.accountMode() != AccountMode.MAIN)
        {
            return decision(MainPurchaseChoice.NOT_APPLICABLE,
                    candidate.totalCost(), 0L,
                    Confidence.VERIFIED,
                    get(358));
        }

        var data = context.data();
        var economy = data == null ? null : data.economy();
        if (economy == null
                || economy.getConfidence() != Confidence.VERIFIED)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    candidate.totalCost(), economy == null ? 0L : economy.getCoins(),
                    Confidence.CHECK_NEEDED,
                    get(363));
        }

        var cost = candidate.totalCost();
        var coins = economy.getCoins();
        if (cost == Long.MAX_VALUE)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                    Confidence.CHECK_NEEDED,
                    get(364));
        }

        if (coins < cost)
        {
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, Confidence.CHECK_NEEDED,
                    get(365));
        }

        if (candidate.getEstimatedSelfSourceMinutes() > 0
                && candidate.getEstimatedBuyMinutes()
                >= candidate.getEstimatedSelfSourceMinutes())
        {
            return decision(MainPurchaseChoice.SELF_SOURCE,
                    cost, coins, Confidence.VERIFIED,
                    get(366));
        }

        if (candidate.getEstimatedSelfSourceMinutes() <= 0)
        {
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    cost, coins, Confidence.CHECK_NEEDED,
                    get(367));
        }

        return decision(MainPurchaseChoice.BUY,
                cost, coins, Confidence.VERIFIED,
                get(368));
    }

    /**
     * Uses deliberately broad liquid-wealth bands when no defensible time
     * estimate exists. This avoids both fake GP/hour precision and the old
     * rule that every observed Main shortfall should simply be bought.
     */
    public MainPurchaseDecision evaluateUnmeasuredPurchase(
            AccountEconomySnapshot economy,
            PurchaseCostEstimate estimate,
            boolean reviewedSelfSourceRoute)
    {
        if (estimate == null || !estimate.isComplete()
                || estimate.getTotalCost() <= 0)
            return decision(MainPurchaseChoice.CHECK_NEEDED, 0L,
                    economy == null ? 0L : economy.getCoins(),
                    Confidence.CHECK_NEEDED,
                    get(369));
        if (economy == null
                || economy.getConfidence() != Confidence.VERIFIED)
            return decision(MainPurchaseChoice.CHECK_NEEDED,
                    estimate.getTotalCost(),
                    economy == null ? 0L : economy.getCoins(),
                    Confidence.CHECK_NEEDED,
                    get(370));

        var cost = estimate.getTotalCost();
        var coins = Math.max(0L, economy.getCoins());
        if (coins < cost)
            return decision(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                    cost, coins, Confidence.CHECK_NEEDED,
                    get(359));

        var remaining = coins - cost;
        var trivialSpend = cost <= 1_000L && coins >= 5_000L;
        boolean lowBurden = cost <= coins / 10L
                && remaining >= MINIMUM_LIQUID_BUFFER;
        if (trivialSpend || lowBurden)
            return decision(MainPurchaseChoice.BUY, cost, coins,
                    Confidence.VERIFIED,
                    get(360));

        if (reviewedSelfSourceRoute)
            return decision(MainPurchaseChoice.SELF_SOURCE, cost, coins,
                    Confidence.VERIFIED,
                    get(361));

        return decision(MainPurchaseChoice.CHECK_NEEDED, cost, coins,
                Confidence.CHECK_NEEDED,
                get(362));
    }

    public boolean maySuggestSale(
            int itemId,
            ProtectedItemProfile playerProtectedItems,
            boolean builtInProtected)
    {
        if (builtInProtected) return false;
        return playerProtectedItems == null
                || !playerProtectedItems.isProtected(itemId);
    }

    private static MainPurchaseDecision decision(
            MainPurchaseChoice choice,
            long totalCost,
            long coins,
            Confidence confidence,
            String explanation)
    {
        return new MainPurchaseDecision(
                choice, totalCost, coins, confidence, explanation);
    }
}

/** Resolves profile input rules against a concrete RuneLite action. */
@Singleton
class MethodInputResolver
{
    public List<MethodInput> resolve(
            MethodProfile profile,
            ActionDef action,
            int actions)
    {
        Map<String, MethodInput> merged = new LinkedHashMap<>();
        if (profile == null || action == null || actions <= 0)
        {
            return new ArrayList<>();
        }

        for (MethodInputRule rule : profile.getInputs())
        {
            var input = resolveOne(rule, action, actions);
            if (input == null || input.getQuantity() <= 0) continue;
            String key = input.getItemId() > 0
                    ? "id:" + input.getItemId()
                    : "name:" + input.getName().toLowerCase(Locale.ROOT);
            var previous = merged.get(key);
            if (previous == null)
            {
                merged.put(key, input);
            }
            else
            {
                merged.put(key, new MethodInput(
                        previous.getName(),
                        previous.getItemId(),
                        previous.getQuantity() + input.getQuantity()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static MethodInput resolveOne(
            MethodInputRule rule,
            ActionDef action,
            int actions)
    {
        if (rule == null
                || rule.getMode() == MethodProfile.InputMode.NONE)
        {
            return null;
        }

        String name;
        var itemId = -1;
        var perAction = rule.getQuantityPerAction();
        switch (rule.getMode())
        {
            case ACTION_ITEM:
                name = action.getName();
                itemId = action.getItemId();
                if (perAction <= 0) perAction = 1.0;
                break;
            case RAW_ACTION_ITEM:
                name = rawName(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case LOG_FOR_BOW:
                name = logForBow(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case BAR_FOR_SMITHED_ITEM:
                name = barForSmithing(action.getName());
                if (name == null) return null;
                if (perAction <= 0)
                {
                    perAction = Names.actionKey(action.getName()).contains("platebody")
                            ? 5.0 : 1.0;
                }
                break;
            case UNCUT_GEM:
                name = uncutGem(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case SAPLING_FOR_TREE:
                name = saplingForTree(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case DART_TIP_FOR_DART:
                name = dartTipForDart(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case UNFINISHED_BOLT:
                name = unfinishedBolt(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case FIXED:
                name = rule.getFixedName();
                if (name == null || name.trim().isEmpty()) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case NONE:
            default:
                return null;
        }

        return new MethodInput(
                name,
                itemId,
                (int) Math.ceil(actions * perAction));
    }

    private static String rawName(String actionName)
    {
        var clean = actionName == null ? "" : actionName.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("cooked "))
            clean = clean.substring(7);
        return "Raw " + clean;
    }

    private static String logForBow(String actionName)
    {
        String clean = actionName == null ? "" : actionName
                .replace("(u)", "").trim();
        var lower = clean.toLowerCase(Locale.ROOT);
        String[] woods = {"oak", "willow", "maple", "yew", "magic", "redwood"};
        for (String wood : woods)
        {
            if (lower.startsWith(wood + " "))
                return capitalize(wood) + " logs";
        }
        return "Logs";
    }

    private static String barForSmithing(String actionName)
    {
        var lower = Names.actionKey(actionName);
        if (lower.contains("bronze")) return "Bronze bar";
        if (lower.contains("iron")) return "Iron bar";
        if (lower.contains("steel")) return "Steel bar";
        if (lower.contains("mithril")) return "Mithril bar";
        if (lower.contains("adamant")) return "Adamantite bar";
        if (lower.contains("rune")) return "Runite bar";
        return null;
    }

    private static String uncutGem(String actionName)
    {
        var clean = actionName == null ? "gem" : actionName.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("uncut ")) return clean;
        return "Uncut " + clean.toLowerCase(Locale.ROOT);
    }

    private static String saplingForTree(String actionName)
    {
        if (actionName == null) return null;
        var clean = actionName.trim();
        var lower = clean.toLowerCase(Locale.ROOT);
        if (lower.equals("spirit tree")) return "Spirit seed";
        if (lower.equals("crystal tree")) return "Crystal acorn";
        if (!lower.endsWith(" tree")) return null;
        var tree = clean.substring(0, clean.length() - 5).trim();
        if (tree.isEmpty()) return null;
        return tree + " sapling";
    }

    private static String dartTipForDart(String actionName)
    {
        if (actionName == null) return null;
        var clean = actionName.trim();
        var lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(" dart")) return null;
        return clean.substring(0, clean.length() - 5).trim() + " dart tip";
    }

    private static String unfinishedBolt(String actionName)
    {
        if (actionName == null) return null;
        var clean = actionName.trim();
        var lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(" bolts")) return null;
        return clean + " (unf)";
    }


    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

/**
 * One engine for recurring and interrupt-driven opportunities.
 *
 * <p>Nothing appears merely because content exists in OSRS. A recurring entry
 * is surfaced only after a reader has observed a ready/cooldown timestamp for
 * that character. Membership is also enforced here so stale observations from
 * a previously-member account cannot leak members-only opportunities into an
 * F2P session.</p>
 */
@Singleton
class OpportunityEngine
{
    private static final Set<String> BIRDHOUSE_SEEDS = new HashSet<>(Arrays.asList(
            "barley seed", get(1883), "asgarnian seed", "jute seed",
            "yanillian seed", "krandorian seed", "wildblood seed", "guam seed",
            "marrentill seed", "tarromin seed", get(1884), "ranarr seed",
            "toadflax seed", "irit seed", "avantoe seed", "kwuarm seed",
            "snapdragon seed", "cadantine seed", "lantadyme seed",
            "dwarf weed seed", "torstol seed"));
    private final FarmingAccessCatalog farmingAccessCatalog =
            new FarmingAccessCatalog();
    private final FarmingSupplyCatalog farmingSupplyCatalog =
            new FarmingSupplyCatalog();

    public List<Opportunity> evaluate(AccountSnapshot snapshot)
    {
        if (snapshot == null) return Collections.emptyList();
        return evaluate(GameData.builder(snapshot).build());
    }

    public List<Opportunity> evaluate(GameData data)
    {
        List<Opportunity> opportunities = new ArrayList<>();
        if (data == null || data.account() == null) return opportunities;

        var membership = data.account().membership();
        var recurring = data.recurringOpportunities();
        var now = System.currentTimeMillis();

        // Every currently-modelled recurring activity below is members content.
        // Keep the entire family out of an F2P plan even if its timer was
        // observed while this character previously had membership.
        if (ContentAccessRules.hasVerifiedMembership(membership))
        {
            addBirdhouseOpportunity(opportunities, data, recurring, now);

            addHerbRunOpportunity(opportunities, data, recurring, now,
                    farmingAccessCatalog, farmingSupplyCatalog);

            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1885), OpportunityType.TREE_RUN,
                    "Tree run", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1886), OpportunityType.FARMING_CONTRACT,
                    get(1887), Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1888), OpportunityType.TEARS_OF_GUTHIX,
                    "Tears of Guthix", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1889), OpportunityType.KINGDOM,
                    "Kingdom", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1890), OpportunityType.KINGDOM_APPROVAL,
                    get(1891), Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1892), OpportunityType.BATTLESTAVES,
                    get(1522), Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1893), OpportunityType.DYNAMITE,
                    "Daily dynamite", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    get(1894), OpportunityType.DAILY_DIARY_REWARD,
                    get(1523), Collections.emptyList());
        }

        var clue = data.clue();
        if (clue != null && clue.isCluePresent())
        {
            var tier = ClueTier.fromText(clue.getClueType());
            if (tier.isAvailableFor(membership))
            {
                var step = clue.getCurrentStep();
                List<String> preparation = new ArrayList<>();
                if (step == null)
                    preparation.add(get(392));
                else
                {
                    preparation.addAll(step.getItemRequirements());
                    if (step.isRequiresSpade()) preparation.add("Spade");
                    if (step.isRequiresLight()) preparation.add("Light source");
                    if (step.hasEnemy()) preparation.add(
                            get(1524) + step.getEnemy());
                    if (step.isWilderness()) preparation.add(
                            get(1525));
                    if (step.hasStashUnit()) preparation.add(
                            "Observe the " + step.getStashUnit()
                                    + get(1526));
                }
                boolean ready = step != null
                        && tier == ClueTier.BEGINNER
                        && !step.requiresPreparation();
                opportunities.add(new Opportunity(
                        get(1895),
                        OpportunityType.CLUE,
                        clue.getClueType() == null ? "Pending clue"
                                : clue.getClueType() + " clue"
                                + (step == null ? "" : ": " + step.getKind()),
                        ready,
                        ready ? Confidence.VERIFIED
                                : Confidence.CHECK_NEEDED,
                        preparation,
                        false,
                        step != null && step.hasEnemy()
                                ? SafetyEvidence.potentiallyIrreversible(
                                        tier == ClueTier.BEGINNER)
                                : SafetyEvidence.harmless(
                                        tier == ClueTier.BEGINNER)
                ));
            }
        }

        return opportunities;
    }

    private static void addPreparedTimedOpportunity(
            List<Opportunity> result,
            RecurringOpportunitySnapshot recurring,
            long now,
            String id,
            OpportunityType type,
            String title,
            List<String> preparation)
    {
        if (recurring == null || recurring.readyAt(id) == null) return;
        var ready = recurring.isReadyNow(id, now);
        result.add(new Opportunity(
                id, type, title, ready,
                Confidence.VERIFIED, preparation));
    }

    private static void addHerbRunOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now, FarmingAccessCatalog accessCatalog,
            FarmingSupplyCatalog supplyCatalog)
    {
        var id = get(1896);
        if (recurring == null || recurring.readyAt(id) == null) return;

        List<String> missing = new ArrayList<>();
        var inventory = data.inventory();
        if (quantity(inventory, "spade") == 0) missing.add("Carry a spade");
        if (quantity(inventory, "seed dibber") == 0) missing.add(get(1527));
        var farmingLevel = data.account().level(net.runelite.api.Skill.FARMING);
        ResourceRequirement herbSeeds = supplyCatalog.herbSeedsForLevel(
                farmingLevel);
        if (inventory == null || inventory.quantityOf(herbSeeds.getItemIds()) == 0)
            missing.add(get(1528));
        if (farmingLevel < 9)
            missing.add(get(393));
        var farming = data.farming();
        if (!hasReachableHerbPatch(farming, accessCatalog))
            missing.add(get(394));

        var ready = recurring.isReadyNow(id, now);
        var setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.HERB_RUN, "Herb run",
                ready, Confidence.VERIFIED, missing,
                setupVerified, SafetyEvidence.skill(false,
                net.runelite.api.Skill.FARMING)));
    }

    private static void addBirdhouseOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now)
    {
        var id = get(1897);
        if (recurring == null || recurring.readyAt(id) == null) return;
        List<String> missing = new ArrayList<>();
        var quests = data.quests();
        if (quests == null || quests.statusOf("Bone Voyage") != QuestStatus.COMPLETE)
            missing.add(get(395));
        var inventory = data.inventory();
        if (quantity(inventory, "hammer") == 0) missing.add("Carry a hammer");
        if (quantity(inventory, "chisel") == 0) missing.add("Carry a chisel");
        if (quantity(inventory, "clockwork") < 4)
            missing.add(get(1529));
        if (inventory == null || inventory.quantityWhere(name ->
                name.endsWith(" log") || name.endsWith(" logs")
                        || name.equals("logs")) < 4)
            missing.add(get(1530));
        if (birdhouseSeedQuantity(inventory) < 40)
            missing.add(get(1531));
        if (data.account().level(net.runelite.api.Skill.HUNTER) < 5)
            missing.add(get(1532));
        if (data.account().level(net.runelite.api.Skill.CRAFTING) < 5)
            missing.add(get(1533));
        var ready = recurring.isReadyNow(id, now);
        var setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.BIRDHOUSE_RUN,
                "Birdhouse run", ready, Confidence.VERIFIED,
                missing, setupVerified,
                SafetyEvidence.skill(false,
                        net.runelite.api.Skill.HUNTER)));
    }

    private static int quantity(ItemsState inventory, String name)
    {
        return inventory == null ? 0 : inventory.quantityNamed(name);
    }

    private static int birdhouseSeedQuantity(ItemsState inventory)
    {
        return inventory == null ? 0
                : inventory.quantityWhere(BIRDHOUSE_SEEDS::contains);
    }

    private static boolean hasReachableHerbPatch(FarmingSnapshot farming,
            FarmingAccessCatalog catalog)
    {
        if (farming == null) return false;
        for (FarmingAccessDefinition patch : catalog.all())
            if (patch.isHerbPatch() && farming.isPatchReachable(patch.id))
                return true;
        return false;
    }
}

/** Resolves only evidence represented by the current local snapshots. */
@Singleton
class QuestRequirementResolver
{
    private static final String IMPORTED_ITEM_PREFIX = "Required items:";
    private final ImportedQuestItemRequirementCatalog importedItems;
    private final ResourceSourceCatalog resourceSources;
    private final ResourceAcquisitionPlanner resourcePlanner;

    @Inject
    public QuestRequirementResolver(ResourceSourceCatalog resourceSources,
            ResourceAcquisitionPlanner resourcePlanner)
    {
        this.importedItems = new ImportedQuestItemRequirementCatalog();
        this.resourceSources = resourceSources == null
                ? new ResourceSourceCatalog() : resourceSources;
        this.resourcePlanner = resourcePlanner == null
                ? new ResourceAcquisitionPlanner(this.resourceSources)
                : resourcePlanner;
    }

    public QuestRequirementResolver(ResourceSourceCatalog resourceSources)
    {
        this(resourceSources, new ResourceAcquisitionPlanner(resourceSources));
    }

    /** Compatibility constructor for focused tests and local tooling. */
    public QuestRequirementResolver()
    {
        this(new ResourceSourceCatalog());
    }

    public QuestResolution resolve(QuestDefinition definition, StrategyContext context)
    {
        if (definition == null || context == null || context.data() == null
                || context.data().account() == null) return null;

        var data = context.data();
        var account = data.account();
        var quests = data.quests();
        List<Preparation> missing = new ArrayList<>();

        for (String prerequisite : definition.getPrerequisites())
        {
            QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                    : quests.statusOf(prerequisite);
            if (status != QuestStatus.COMPLETE)
                missing.add(new Preparation(status == QuestStatus.UNKNOWN
                        ? get(560) + prerequisite
                        : get(1349) + prerequisite,
                        RestrictedQuestPolicy.isSafe(account, prerequisite)
                                ? SafetyEvidence.verifiedSafe(
                                definition.isFreeToPlay())
                                : SafetyEvidence.potentiallyIrreversible(
                                definition.isFreeToPlay())));
        }

        for (Map.Entry<Skill, Integer> requirement
                : definition.getSkillRequirements().entrySet())
        {
            var current = account.level(requirement.getKey());
            if (current < requirement.getValue())
                missing.add(new Preparation("Train "
                        + requirement.getKey().getName() + " from " + current
                        + " to " + requirement.getValue(),
                        SafetyEvidence.skill(definition.isFreeToPlay(),
                                requirement.getKey())));
        }

        ItemIndex items = new ItemIndex(data,
                context.usesGroupStorage());
        // An inventory observation does not prove that an unobserved bank is empty.
        var ownershipObserved = items.usableOwnershipObserved();
        for (QuestDefinition.QuestItemRequirement requirement
                : definition.getItemRequirements())
        {
            var owned = items.quantity(requirement.getName());
            if (owned < requirement.getQuantity())
                missing.add(new Preparation((ownershipObserved ? "Obtain " : get(1350))
                        + Math.max(0, requirement.getQuantity() - owned) + " × "
                        + requirement.getName(), SafetyEvidence.harmless(
                        definition.isFreeToPlay())));
        }

        ImportedQuestItemRequirementCatalog.Result imported = hasImportedItemEvidence(definition)
                ? importedItems.resultFor(definition.getName()) : null;
        var expression = definition.getItemRequirementExpression();
        if (expression == null && imported != null)
            expression = imported.getExpression();
        ItemRequirementResult expressionResult = new ItemRequirementEvaluator()
                .evaluate(expression, data, context.usesGroupStorage());
        if (!expressionResult.isSatisfied()
                && !expressionResult.getAction().isEmpty())
        {
            missing.add(itemPreparation(expressionResult, definition, context));
        }

        if (definition.getQuestPointsRequired() > 0)
            missing.add(new Preparation(get(1924)
                    + definition.getQuestPointsRequired() + " quest points",
                    SafetyEvidence.harmless(definition.isFreeToPlay())));
        for (String check : definition.getAccessChecks())
        {
            if (check != null && check.startsWith(IMPORTED_ITEM_PREFIX))
            {
                if (imported == null)
                {
                    missing.add(new Preparation(check,
                            SafetyEvidence.harmless(
                                    definition.isFreeToPlay())));
                }
                else
                {
                    for (String unresolved : imported.getUnresolved())
                        missing.add(new Preparation(
                                get(1351) + unresolved,
                                SafetyEvidence.harmless(
                                        definition.isFreeToPlay())));
                }
                continue;
            }
            missing.add(new Preparation(check,
                    SafetyEvidence.harmless(definition.isFreeToPlay())));
        }

        String unlocks = definition.getUnlocks().isEmpty() ? ""
                : String.join(", ", definition.getUnlocks());
        if (missing.isEmpty())
        {
            return new QuestResolution(Confidence.VERIFIED,
                    new Guidance(
                            "Start " + definition.getName() + ".",
                            get(561),
                            definition.getStartLocation(),
                            unlocks.isEmpty() ? get(562)
                                    : get(1352) + unlocks + "."),
                    get(1353),
                    SafetyEvidence.verifiedSafe(
                            definition.isFreeToPlay()));
        }

        List<String> missingText = new ArrayList<>();
        for (Preparation preparation : missing) missingText.add(preparation.detail);
        return new QuestResolution(Confidence.CHECK_NEEDED,
                new Guidance(missing.get(0).text + ".",
                        String.join("; ", missingText), definition.getStartLocation(),
                        unlocks.isEmpty()
                                ? get(563)
                                : get(1354) + unlocks + "."),
                get(1355) + missing.get(0).text,
                missing.get(0).safetyEvidence);
    }

    private Preparation itemPreparation(ItemRequirementResult result,
            QuestDefinition definition, StrategyContext context)
    {
        SafetyEvidence safety = SafetyEvidence.harmless(
                definition.isFreeToPlay());
        if (result.getMissingInputs().isEmpty())
            return new Preparation(result.getAction(), safety);

        var first = result.getMissingInputs().get(0);
        var dependency = dependencyResolution(context, first);
        var next = dependency == null ? null : dependency.nextAction();
        if (next != null
                && next.getConfidence() != Confidence.VERIFIED
                && next.getAction() != null
                && !next.getAction().trim().isEmpty())
        {
            var action = withoutTerminalPeriod(next.getAction().trim());
            var detail = new StringBuilder(result.getAction());
            detail.append(get(1356))
                    .append(formatInputs(result.getMissingInputs())).append(".");
            detail.append(get(1357))
                    .append(quantity(first)).append(": ")
                    .append(next.getAction().trim());
            if (result.getMissingInputs().size() > 1)
                detail.append(get(564));
            return new Preparation(action, detail.toString(), safety);
        }

        var mode = context.accountMode();
        String action;
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            action = "Acquire " + quantity(first) + " just in time";
        else if (mode.isIronLike())
            action = "Self-source " + quantity(first);
        else if (mode == AccountMode.UNKNOWN)
            action = "Source " + quantity(first) + get(1358);
        else
            action = "Acquire " + quantity(first);

        var routes = sourceRoutes(context, first.getName());
        if (!routes.isEmpty()) action += ": " + routes.get(0);

        var detail = new StringBuilder(result.getAction());
        detail.append(get(1356))
                .append(formatInputs(result.getMissingInputs())).append(".");
        if (!routes.isEmpty())
            detail.append(get(1359)).append(routes.get(0));
        if (result.getMissingInputs().size() > 1)
            detail.append(get(565));
        return new Preparation(action, detail.toString(), safety);
    }

    private DependencyResolution dependencyResolution(
            StrategyContext context, MethodInput input)
    {
        if (context == null || input == null || context.data() == null
                || context.data().account() == null)
            return null;
        return resourcePlanner.resolveKnownShortfall(
                context, input.getName(), input.getQuantity());
    }

    private List<String> sourceRoutes(StrategyContext context, String itemName)
    {
        if (context == null || context.data() == null
                || context.data().account() == null)
            return java.util.Collections.emptyList();
        return resourceSources.suggestions(itemName, context.accountMode(),
                context.data().account().membership(),
                context.allowsWilderness());
    }

    private static String quantity(MethodInput input)
    {
        return Math.max(1, input.getQuantity()) + " × " + input.getName();
    }

    private static String formatInputs(List<MethodInput> inputs)
    {
        List<String> values = new ArrayList<>();
        for (MethodInput input : inputs) values.add(quantity(input));
        return String.join(", ", values);
    }

    private static String withoutTerminalPeriod(String value)
    {
        if (value == null) return "";
        var result = value.trim();
        while (result.endsWith("."))
            result = result.substring(0, result.length() - 1).trim();
        return result;
    }

    private static boolean hasImportedItemEvidence(QuestDefinition definition)
    {
        for (String check : definition.getAccessChecks())
            if (check != null && check.startsWith(IMPORTED_ITEM_PREFIX)) return true;
        return false;
    }

    private static final class Preparation
    {
        private final String text;
        private final String detail;
        private final SafetyEvidence safetyEvidence;

        private Preparation(String text,
                SafetyEvidence safetyEvidence)
        {
            this(text, text, safetyEvidence);
        }

        private Preparation(String text, String detail,
                SafetyEvidence safetyEvidence)
        {
            this.text = text;
            this.detail = detail == null ? text : detail;
            this.safetyEvidence = safetyEvidence;
        }
    }
}

@Singleton
class RecommendationEngine
{
    private final TrainingMethodSelector trainingMethodSelector;
    private final RecommendationGuidanceService guidanceService;
    private final CombatGuidanceService combatGuidanceService;
    private final SlayerGuidanceService slayerGuidanceService;
    private final SailingGuidanceService sailingGuidanceService;
    private final SkillBreakpointService breakpointService;
    private final AdaptiveActionSelector actionResolver;

    @Inject
    public RecommendationEngine(
            TrainingMethodSelector trainingMethodSelector,
            RecommendationGuidanceService guidanceService,
            CombatGuidanceService combatGuidanceService,
            SlayerGuidanceService slayerGuidanceService,
            SailingGuidanceService sailingGuidanceService,
            SkillBreakpointService breakpointService,
            AdaptiveActionSelector actionResolver)
    {
        this.trainingMethodSelector = trainingMethodSelector;
        this.guidanceService = guidanceService;
        this.combatGuidanceService = combatGuidanceService;
        this.slayerGuidanceService = slayerGuidanceService;
        this.sailingGuidanceService = sailingGuidanceService;
        this.breakpointService = breakpointService == null
                ? new SkillBreakpointService() : breakpointService;
        this.actionResolver = actionResolver == null
                ? new AdaptiveActionSelector() : actionResolver;
    }

    public RecommendationEngine(
            TrainingMethodSelector trainingMethodSelector,
            RecommendationGuidanceService guidanceService)
    {
        this(trainingMethodSelector, guidanceService,
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    public RecommendationEngine(TrainingMethodSelector trainingMethodSelector)
    {
        this(trainingMethodSelector, new RecommendationGuidanceService(),
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            PreferenceProfile preferenceProfile)
    {
        return recommend(GameData.builder(snapshot).build(),
                strategyMode, SessionIntent.PICK_FOR_ME, true, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(GameData.builder(snapshot).build(),
                strategyMode, sessionIntent, true, false, preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(data, strategyMode, sessionIntent, true, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return recommend(data, strategyMode, sessionIntent, true,
                allowWildernessMethods, preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return topThree(recommendAll(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, GoalType.AUTOMATIC,
                preferenceProfile));
    }

    /** Full skill candidate pool for the global strategy queue. Do not trim here. */
    public List<Recommendation> recommendAll(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return recommendAllInternal(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, GoalType.AUTOMATIC,
                preferenceProfile);
    }

    public List<Recommendation> recommendAll(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            GoalType activeGoal,
            PreferenceProfile preferenceProfile)
    {
        // Focused queue tests historically override the public six-argument
        // method with a synthetic pool. Preserve that extension seam when no
        // production selector exists instead of entering the concrete skill
        // generator with a null dependency.
        if (trainingMethodSelector == null)
            return recommendAll(data, strategyMode, sessionIntent,
                    useGroupStorage, allowWildernessMethods,
                    preferenceProfile);
        return recommendAllInternal(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, activeGoal,
                preferenceProfile);
    }

    private List<Recommendation> recommendAllInternal(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            GoalType activeGoal,
            PreferenceProfile preferenceProfile)
    {
        List<Recommendation> recommendations = new ArrayList<>();
        if (trainingMethodSelector == null || data == null
                || data.account() == null) return recommendations;
        var snapshot = data.account();
        PreferenceProfile safePreferences = preferenceProfile == null
                ? new PreferenceProfile() : preferenceProfile;
        StrategyContext context = new StrategyContext(data, strategyMode,
                sessionIntent, QuestTolerance.NORMAL, activeGoal,
                useGroupStorage, false, allowWildernessMethods,
                safePreferences);

        for (Skill skill : Skill.values())
        {
            var level = snapshot.level(skill);
            if (level >= 99 || skill == Skill.HITPOINTS) continue;
            if (!ContentAccessRules.isSkillAvailable(skill,
                    snapshot.membership())) continue;
            if (!AccountBuildPolicy.allowsSkill(snapshot, skill)) continue;

            var activityId = "skill:" + skill.name().toLowerCase();
            if (safePreferences.isOnCooldown(activityId)) continue;

            SkillBreakpoint breakpoint = breakpointService.next(
                    skill, level, context);
            var target = breakpoint.getLevel();
            TrainingPlan trainingPlan = null;
            Guidance guidance = null;
            TrainingPlan highestRankedPlan = null;
            for (TrainingPlan candidate : trainingMethodSelector.rankedCandidates(
                    data, skill, level, strategyMode, sessionIntent,
                    allowWildernessMethods, useGroupStorage))
            {
                if (highestRankedPlan == null) highestRankedPlan = candidate;
                Guidance candidateGuidance = buildGuidance(
                        data, skill, level,
                        actionResolver.resolve(candidate, level, target),
                        candidate, sessionIntent,
                        useGroupStorage);
                if (candidateGuidance != null
                        && candidate.getStrategyProfile() != null)
                {
                    candidateGuidance = candidateGuidance.withBankingBehavior(
                            candidate.getStrategyProfile()
                                    .getBankingBehavior());
                }
                // Some activities can only be rendered truthfully once live
                // resources or state identify a concrete execution loop. A
                // higher-scoring but unrenderable route must not consume the
                // skill's only candidate and hide a ready lower-ranked route.
                if (candidateGuidance == null) continue;
                trainingPlan = candidate.withCurrentStageTargetLevel(
                        actionResolver.resolve(candidate, level, target));
                guidance = candidateGuidance;
                break;
            }
            // Keep the historical diagnostic candidate when this engine was
            // constructed without a renderer capable of any method in the
            // skill. The final actionability boundary still prevents it from
            // leading DO NEXT; this also preserves focused selector callers.
            if (trainingPlan == null) trainingPlan = highestRankedPlan;
            if (trainingPlan == null || trainingPlan.method() == null) continue;

            var score = baseScore(level, breakpoint);
            score += safePreferences.weightFor(activityId) * 10.0;
            score += safePreferences.timedScoreAdjustmentFor(activityId);
            score += milestoneMomentum(level, target);

            var primaryReason = trainingPlan.getWhyThisMethod();
            if (primaryReason == null || primaryReason.trim().isEmpty())
                primaryReason = breakpoint.getLabel() + ".";
            Recommendation recommendation = new Recommendation(
                    activityId,
                    "Train " + skill.getName() + " to " + target,
                    primaryReason,
                    score,
                    trainingPlan,
                    trainingPlan.getConfidence(),
                    level,
                    target,
                    guidance,
                    SafetyEvidence.skill(
                            ContentAccessRules.isMethodAvailable(
                                    trainingPlan.method(), MembershipStatus.F2P),
                            skill));
            recommendation = recommendation.withStrategicValue(
                    StrategicValue.builder()
                            .unlockValue(breakpoint.strategicValue())
                            .evidence(breakpoint.getEvidenceId())
                            .build());
            recommendations.add(recommendation);
        }

        recommendations.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        return recommendations;
    }

    private Guidance buildGuidance(
            GameData data,
            Skill skill,
            int level,
            int target,
            TrainingPlan trainingPlan,
            SessionIntent sessionIntent,
            boolean useGroupStorage)
    {
        Guidance guidance = combatGuidanceService == null
                ? null : combatGuidanceService.build(
                        data, skill, level, target, trainingPlan,
                        sessionIntent, useGroupStorage);

        if (guidance == null && skill == Skill.SLAYER
                && slayerGuidanceService != null)
        {
            guidance = slayerGuidanceService.build(
                    data, level, target, useGroupStorage);
        }

        if (guidance == null && skill == Skill.SAILING
                && sailingGuidanceService != null)
        {
            guidance = sailingGuidanceService.build(
                    data, level, target, trainingPlan);
        }

        if (guidance == null && guidanceService != null)
        {
            guidance = guidanceService.build(
                    data, skill, level, target, trainingPlan,
                    useGroupStorage);
        }
        return guidance;
    }

    private static List<Recommendation> topThree(
            List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
            return new ArrayList<>();
        if (recommendations.size() <= 3)
            return new ArrayList<>(recommendations);
        return new ArrayList<>(recommendations.subList(0, 3));
    }

    private double baseScore(int level, SkillBreakpoint breakpoint)
    {
        var distance = Math.max(1, breakpoint.getLevel() - level);
        double proximity = distance <= 1 ? 12.0
                : distance <= 3 ? 7.0 : distance <= 7 ? 3.0 : 0.0;
        return 24.0 + proximity + breakpoint.strategicValue() * 20.0;
    }

    private double milestoneMomentum(int level, int target)
    {
        var remaining = target - level;
        if (remaining <= 1) return 8.0;
        if (remaining <= 3) return 4.0;
        return 0.0;
    }

}

/** Resolves method requirements from live access, account and item evidence. */
@Singleton
class RequirementEvidenceEngine
{
    private final FarmingAccessEvaluator farmingAccess;
    private final AgilityAccessEvaluator agilityAccess;
    private final FarmingSupplyCatalog farmingSupplies;
    private final RunecraftSupplyCatalog runecraftSupplies;

    @Inject
    public RequirementEvidenceEngine(FarmingAccessEvaluator farmingAccess,
            AgilityAccessEvaluator agilityAccess,
            FarmingSupplyCatalog farmingSupplies,
            RunecraftSupplyCatalog runecraftSupplies)
    {
        this.farmingAccess = farmingAccess;
        this.agilityAccess = agilityAccess;
        this.farmingSupplies = farmingSupplies;
        this.runecraftSupplies = runecraftSupplies;
    }

    public RequirementEvidenceEngine(FarmingAccessEvaluator farming,
            AgilityAccessEvaluator agility)
    {
        this(farming, agility, new FarmingSupplyCatalog(),
                new RunecraftSupplyCatalog());
    }

    public RequirementEvidenceEngine(FarmingAccessEvaluator farming)
    {
        this(farming, null);
    }

    public List<RequirementCheck> evaluate(GameData data, TrainingMethod method)
    {
        return evaluate(data, method, false);
    }

    public List<RequirementCheck> evaluate(GameData data, TrainingMethod method,
            boolean group)
    {
        if (method == null) return new ArrayList<>();
        var id = method.id;
        var skill = method.getSkill();
        var items = new ItemIndex(data, group);
        if (skill == Skill.FARMING) return farming(data, method, items);
        if (skill == Skill.AGILITY && agilityAccess != null)
            return agility(data, id);
        if (skill == Skill.SAILING) return sailing(data, id, items);
        if (skill == Skill.RUNECRAFT && runecraftSupplies.supports(id))
            return runecraft(id, items);
        if (skill == Skill.MAGIC && (id.equals(get(1625))
                || id.equals(get(1626))
                || id.equals(get(1627))
                || id.equals(get(1628))
                || id.equals("magic_f2p_curse"))) return magic(data, id, items);
        if (skill == Skill.COOKING && id.equals(get(1735)))
            return cookedFish(data, items);
        if (skill == Skill.COOKING && (id.equals(get(1577))
                || id.equals("cooking_wines"))) return cooking(data, id, items);
        if (skill == Skill.FISHING)
        {
            var result = fishing(data, id, items);
            if (result != null) return result;
        }
        if (skill == Skill.HUNTER)
        {
            var result = hunter(data, id, items);
            if (result != null) return result;
        }
        if (id.equals("runecraft_gotr"))
        {
            List<RequirementCheck> result = quest(data, get(1736),
                    get(1737));
            result.add(tool(items, ItemRequirementClass.PICKAXE,
                    get(1738), "Usable pickaxe"));
            result.add(item(items, get(1739), "Chisel", 1,
                    ItemID.CHISEL));
            return result;
        }
        if (id.equals("runecraft_zmi")) return list(item(items,
                get(1740), "Pure essence", 1,
                ItemID.BLANKRUNE_HIGH));
        if (id.equals(get(1629)))
            return construction(data, items, false);
        if (id.equals(get(1630)))
            return construction(data, items, true);
        if (skill == Skill.MINING || skill == Skill.WOODCUTTING)
        {
            ItemRequirementClass type = skill == Skill.MINING
                    ? ItemRequirementClass.PICKAXE : ItemRequirementClass.AXE;
            List<RequirementCheck> result = list(tool(items, type,
                    skill == Skill.MINING ? get(1741)
                            : get(1742),
                    skill == Skill.MINING ? "Usable pickaxe" : "Usable axe"));
            addGeneric(result, method);
            return result;
        }
        List<RequirementCheck> result = new ArrayList<>();
        addGeneric(result, method);
        return result;
    }

    private static List<RequirementCheck> magic(GameData data, String id,
            ItemIndex items)
    {
        var combat = data == null ? null : data.combatEvidence();
        var observed = combat != null;
        List<RequirementCheck> result = list(new RequirementCheck(
                get(1743), get(1534), observed
                        ? combat.getSpellbookSelector() == 0
                        ? RequirementState.VERIFIED : RequirementState.BLOCKED
                        : RequirementState.CHECK_NEEDED,
                !observed ? get(613) : combat.getSpellbookSelector() == 0
                        ? get(1535) : get(624)));
        if (id.equals("magic_f2p_curse"))
        {
            result.add(item(items, get(1744), "Body rune", 1,
                    ItemID.BODYRUNE));
            result.add(item(items, get(1745), "Earth runes", 3,
                    ItemID.EARTHRUNE));
            result.add(item(items, get(1746), "Water runes", 2,
                    ItemID.WATERRUNE));
            result.add(splashing(data));
            return result;
        }
        var splash = id.equals(get(1628));
        int air = id.equals(get(1627)) ? 4
                : id.equals(get(1626)) ? 3 : splash ? 2 : 1;
        result.add(item(items, get(1747), get(1536),
                air, ItemID.AIRRUNE));
        if (splash)
        {
            result.add(item(items, get(1748), "Fire runes", 3,
                    ItemID.FIRERUNE));
            result.add(item(items, get(1749), "Mind rune", 1,
                    ItemID.MINDRUNE));
            result.add(splashing(data));
        }
        else if (id.equals(get(1626))
                || id.equals(get(1627)))
        {
            var blast = id.endsWith("blast");
            result.add(item(items, get(1750), get(1537),
                    blast ? 5 : 4, ItemID.FIRERUNE));
            result.add(item(items, get(1751),
                    get(1538), 1, blast ? ItemID.DEATHRUNE : ItemID.CHAOSRUNE));
        }
        else result.add(item(items, get(1752), "Mind rune",
                1, ItemID.MINDRUNE));
        return result;
    }

    private static RequirementCheck splashing(GameData data)
    {
        var equipment = data == null ? null : data.equipment();
        boolean helm = false, body = false, legs = false, shield = false,
                boots = false, staff = false;
        if (equipment != null) for (ItemState item : equipment.getEquippedItems())
        {
            if (item == null || item.getName() == null || item.getQuantity() <= 0)
                continue;
            var name = item.getName().toLowerCase(Locale.ROOT);
            helm |= metal(name, "full helm");
            body |= metal(name, "platebody");
            legs |= metal(name, "platelegs") || metal(name, "plateskirt");
            shield |= metal(name, "kiteshield");
            boots |= name.equals("fancy boots") || name.equals("fighting boots")
                    || name.equals(get(1753));
            staff |= name.equals(get(1542));
        }
        var ready = helm && body && legs && shield && boots && staff;
        return new RequirementCheck(get(1754), get(661),
                ready ? RequirementState.VERIFIED : RequirementState.CHECK_NEEDED,
                get(ready ? 614 : 615));
    }

    private static boolean metal(String name, String piece)
    {
        if (!name.endsWith(piece)) return false;
        for (String metal : new String[]{"bronze ", "iron ", "steel ", "black ",
                "mithril ", "adamant ", "rune ", "gilded "})
            if (name.startsWith(metal)) return true;
        return false;
    }

    private static List<RequirementCheck> sailing(GameData data, String id,
            ItemIndex items)
    {
        List<RequirementCheck> result = quest(data, "Pandemonium",
                get(1755));
        if (id.equals("sailing_courier"))
        {
            var sailing = data == null ? null : data.sailing();
            boolean route = sailing != null
                    && sailing.hasPort(SailingSnapshot.PORT_SARIM)
                    && sailing.hasPort(SailingSnapshot.PORT_PANDEMONIUM)
                    && sailing.hasActivity(SailingSnapshot.ACTIVITY_COURIER);
            result.add(state(get(1756), get(1539), route,
                    get(635), get(646)));
            result.add(item(items, get(1757), "Captain's log", 1,
                    ItemID.SAILING_LOG_INITIAL, ItemID.SAILING_LOG));
        }
        else if (id.equals(get(1758))) result.add(new RequirementCheck(
                get(1759), get(1540),
                RequirementState.CHECK_NEEDED, get(657)));
        else if (id.startsWith(get(1760)))
        {
            if (id.contains("jubbly")) result.addAll(quest(data, get(1541),
                    get(1761)));
            if (id.contains("gwenith")) result.addAll(quest(data, "Regicide",
                    "quest:regicide"));
            result.add(new RequirementCheck(get(1762),
                    get(1763), RequirementState.CHECK_NEEDED,
                    get(658)));
        }
        else result.add(new RequirementCheck(get(1764), get(659),
                    RequirementState.CHECK_NEEDED, get(660)));
        return result;
    }

    private static List<RequirementCheck> cookedFish(GameData data,
            ItemIndex items)
    {
        int level = data == null || data.account() == null ? 1
                : data.account().level(Skill.COOKING);
        int[] ids = {ItemID.RAW_SHRIMP, ItemID.RAW_SARDINE, ItemID.RAW_HERRING,
                ItemID.RAW_TROUT, ItemID.RAW_PIKE, ItemID.RAW_SALMON,
                ItemID.RAW_TUNA, ItemID.RAW_LOBSTER, ItemID.RAW_SWORDFISH};
        int[] levels = {1, 1, 5, 15, 20, 25, 30, 40, 45};
        var count = 0;
        for (int required : levels) if (level >= required) count++;
        return list(item(items, get(1765), get(1543), 1,
                Arrays.copyOf(ids, count)));
    }

    private static List<RequirementCheck> cooking(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals("cooking_wines")) return list(
                item(items, get(1766), "Grapes", 1, ItemID.GRAPES),
                item(items, get(1767), "Jug of water", 1,
                        ItemID.JUG_WATER));
        var result = cookedFish(data, items);
        var diaries = data == null ? null : data.diaries();
        boolean ready = diaries != null
                && diaries.isTierComplete("Kourend & Kebos", DiaryTier.EASY);
        result.add(state(get(1768), get(1544), ready,
                get(616), get(617)));
        return result;
    }

    private static List<RequirementCheck> fishing(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals(get(1639))
                || id.equals(get(1673))) return new ArrayList<>();
        if (id.equals("fishing_f2p_fly")) return list(
                item(items, get(1769), "Fly fishing rod", 1,
                        ItemID.FLY_FISHING_ROD),
                item(items, get(1770), "Feathers", 1,
                        ItemID.FEATHER));
        if (!id.equals(get(1771))) return null;
        var quests = data == null ? null : data.quests();
        var trio = complete(quests, get(1545));
        var transport = data == null ? null : data.transport();
        boolean observedRoute = transport != null
                && transport.hasVerifiedRoute("fairy-rings");
        var fairy = observedRoute || complete(quests, get(1547));
        var diaries = data == null ? null : data.diaries();
        boolean staffless = diaries != null && diaries.isTierComplete(
                get(1152), DiaryTier.ELITE);
        boolean staff = observedRoute || staffless || items.quantity(
                ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF) > 0;
        return list(state(get(1772), get(1546), trio,
                        get(618), get(619)),
                state(get(1773), get(1548), fairy,
                        get(620), get(621)),
                state(get(1774), get(622), staff,
                        get(623), get(625)),
                item(items, get(1775), get(1776), 1,
                        ItemID.TBWT_KARAMBWAN_VESSEL,
                        ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI),
                item(items, get(1777), get(1549), 1,
                        ItemID.TBWT_RAW_KARAMBWANJI,
                        ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI));
    }

    private static List<RequirementCheck> hunter(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals("hunter_falconry")) return list(item(items,
                get(1778), get(1550), 500, ItemID.COINS));
        if (id.equals(get(1623))) return list(item(items,
                get(1779), "Bird snare", 1, ItemID.HUNTING_SNARE));
        if (!id.equals(get(1678))) return null;
        int herblore = data == null || data.account() == null ? 1
                : data.account().level(Skill.HERBLORE);
        var voyage = complete(data == null ? null : data.quests(), "Bone Voyage");
        return list(new RequirementCheck(get(1780),
                        "31 Herblore", herblore >= 31 ? RequirementState.VERIFIED
                        : RequirementState.BLOCKED, get(1551) + herblore + "."),
                state(get(1781), get(1552), voyage,
                        get(626), get(627)));
    }

    private static List<RequirementCheck> construction(GameData data,
            ItemIndex items, boolean oak)
    {
        var poh = data == null ? null : data.poh();
        CapabilityState house = poh == null ? CapabilityState.UNKNOWN
                : poh.getHouseAccess();
        CapabilityState room = poh == null ? CapabilityState.UNKNOWN
                : poh.furnitureState(oak ? "room:kitchen" : "room:parlour");
        List<RequirementCheck> result = list(
                capability(get(1782), get(1554), house,
                        get(oak ? 631 : 629)),
                capability(oak ? get(1783) : get(1784),
                        oak ? "POH Kitchen" : "POH Parlour", room,
                        get(oak ? 632 : 630)),
                item(items, oak ? get(1785)
                        : get(1786),
                        oak ? "Oak planks" : "Planks", oak ? 8 : 2,
                        oak ? ItemID.PLANK_OAK : ItemID.WOODPLANK));
        if (!oak) result.add(item(items, get(1787), "Nails",
                2, ItemID.NAILS_BRONZE, ItemID.NAILS_IRON, ItemID.NAILS,
                ItemID.NAILS_BLACK, ItemID.NAILS_MITHRIL, ItemID.NAILS_ADAMANT,
                ItemID.NAILS_RUNE));
        result.add(item(items, get(1788), "Hammer", 1,
                ItemID.HAMMER));
        result.add(item(items, get(1789), "Saw", 1,
                ItemID.POH_SAW, ItemID.EYEGLO_CRYSTAL_SAW, ItemID.WEARABLE_SAW));
        return result;
    }

    private List<RequirementCheck> runecraft(String id, ItemIndex items)
    {
        List<RequirementCheck> result = list(items.check(
                runecraftSupplies.runeEssence()));
        var entry = runecraftSupplies.altarEntryFor(id);
        if (entry != null) result.add(items.check(entry));
        return result;
    }

    private List<RequirementCheck> agility(GameData data, String id)
    {
        if (id.equals(get(1790))) return list(
                agilityAccess.wildernessCourseCheck(data), new RequirementCheck(
                        get(1791), get(1556),
                        RequirementState.VERIFIED, get(636)));
        return list(agilityAccess.courseCheck(data,
                agilityAccess.bestStandardCourse(data)));
    }

    private List<RequirementCheck> farming(GameData data, TrainingMethod method,
            ItemIndex items)
    {
        var id = method.id;
        var account = data == null ? null : data.account();
        var farming = data == null ? null : data.farming();
        var level = account == null ? 1 : account.level(Skill.FARMING);
        if (id.equals("farming_early"))
        {
            var patch = farmingAccess.firstReachablePatchName(farming);
            return list(new RequirementCheck(get(1792),
                    get(1557), patch == null ? RequirementState.CHECK_NEEDED
                    : RequirementState.VERIFIED, patch == null ? get(637)
                    : patch + get(638)), new RequirementCheck(
                    get(1793), get(1558),
                    RequirementState.CHECK_NEEDED, get(639)));
        }
        if (id.equals(get(1794))
                || id.equals(get(1795)))
        {
            var watermelon = id.endsWith("watermelons");
            var reachable = farming != null && farming.isPatchReachable("falador");
            RequirementCheck seeds = items.check(watermelon
                    ? farmingSupplies.watermelonSeeds()
                    : farmingSupplies.potatoSeeds());
            List<RequirementCheck> result = list(state(get(1796),
                    get(1559), reachable, get(640), get(641)), seeds);
            if (watermelon && seeds.getState() != RequirementState.VERIFIED
                    && (account == null || account.level(Skill.THIEVING) < 38))
                result.add(new RequirementCheck(get(1797),
                        get(642), RequirementState.BLOCKED, get(643)));
            result.add(farmingTool(items, farming, farmingSupplies.rake(),
                    "rake", get(644)));
            result.add(farmingTool(items, farming, farmingSupplies.dibber(),
                    "dibber", get(645)));
            result.add(farmingTool(items, farming, farmingSupplies.spade(),
                    "spade", get(647)));
            return result;
        }
        if (id.equals("farming_tithe"))
        {
            int cans = items.quantity(ItemID.WATERING_CAN_1, ItemID.WATERING_CAN_2,
                    ItemID.WATERING_CAN_3, ItemID.WATERING_CAN_4,
                    ItemID.WATERING_CAN_5, ItemID.WATERING_CAN_6,
                    ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_8);
            var water = cans >= 8 || items.quantity(ItemID.ZEAH_WATERINGCAN) > 0;
            return list(items.check(farmingSupplies.spade()),
                    items.check(farmingSupplies.dibber()),
                    state(get(1798), get(648), water,
                            get(649), get(650)));
        }
        if (id.equals("farming_herbs") || id.equals(get(1799)))
        {
            var patch = farmingAccess.firstReachableHerbPatchName(farming);
            return list(new RequirementCheck("farming:level_9", "9 Farming",
                            level >= 9 ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED, get(1560) + level + "."),
                    new RequirementCheck(get(1800), get(1561),
                            patch == null ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED, patch == null
                            ? get(651) : patch + get(652)),
                    items.check(farmingSupplies.herbSeedsForLevel(level)),
                    farmingTool(items, farming, farmingSupplies.rake(), "rake",
                            get(653)),
                    farmingTool(items, farming, farmingSupplies.dibber(), "dibber",
                            get(654)),
                    farmingTool(items, farming, farmingSupplies.spade(), "spade",
                            get(655)));
        }
        List<RequirementCheck> result = new ArrayList<>();
        addGeneric(result, method);
        return result;
    }

    private static RequirementCheck farmingTool(ItemIndex items,
            FarmingSnapshot farming, ResourceRequirement need, String tool,
            String evidence)
    {
        if (farming != null
                && farming.leprechaunToolState(tool) == CapabilityState.VERIFIED)
            return new RequirementCheck(need.id, need.getLabel(),
                    RequirementState.VERIFIED, evidence == null
                    ? get(1569) : evidence);
        return items.check(need);
    }

    private static RequirementCheck item(ItemIndex items, String id,
            String label, int quantity, int... itemIds)
    {
        return items.check(new ResourceRequirement(id, label, quantity, itemIds));
    }

    private static RequirementCheck tool(ItemIndex items,
            ItemRequirementClass type, String id, String label)
    {
        var ready = items.quantityMatching(type, Collections.emptyList()) > 0;
        return new RequirementCheck(id, label, ready ? RequirementState.VERIFIED
                : RequirementState.CHECK_NEEDED, ready ? label + get(633)
                : "No " + label.toLowerCase() + get(634));
    }

    private static RequirementCheck state(String id, String label,
            boolean ready, String yes, String no)
    {
        return new RequirementCheck(id, label, ready ? RequirementState.VERIFIED
                : RequirementState.CHECK_NEEDED, ready ? yes : no);
    }

    private static RequirementCheck capability(String id, String label,
            CapabilityState value, String unknown)
    {
        var ready = value == CapabilityState.VERIFIED;
        return state(id, label, ready, label + get(1555), unknown);
    }

    private static List<RequirementCheck> quest(GameData data, String name,
            String id)
    {
        var ready = complete(data == null ? null : data.quests(), name);
        return list(state(id, name + " completed", ready,
                name + get(628), name + get(1553)));
    }

    private static boolean complete(QuestSnapshot quests, String name)
    {
        return quests != null && quests.statusOf(name) == QuestStatus.COMPLETE;
    }

    @SafeVarargs
    private static <T> List<T> list(T... values)
    {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static void addGeneric(List<RequirementCheck> result,
            TrainingMethod method)
    {
        for (String value : method.getRequirements()) result.add(
                new RequirementCheck("generic:" + value, value,
                        RequirementState.CHECK_NEEDED, get(656)));
    }
}

/** Account-mode-aware sourcing planner for a required item. */
@Singleton
class ResourceAcquisitionPlanner
{
    private final ResourceSourceCatalog sourceCatalog;
    private final ResourceDependencyCatalog dependencyCatalog;

    @Inject
    public ResourceAcquisitionPlanner(ResourceSourceCatalog sourceCatalog,
            ResourceDependencyCatalog dependencyCatalog)
    {
        this.sourceCatalog = sourceCatalog;
        this.dependencyCatalog = dependencyCatalog;
    }

    public ResourceAcquisitionPlanner(ResourceSourceCatalog sourceCatalog)
    {
        this(sourceCatalog, new ResourceDependencyCatalog());
    }

    /** Compatibility constructor for existing focused tests. */
    public ResourceAcquisitionPlanner()
    {
        this(new ResourceSourceCatalog(), new ResourceDependencyCatalog());
    }

    public AcquisitionPlan plan(
            StrategyContext context,
            ResourceNeed need)
    {
        if (context == null || need == null || context.data() == null)
        {
            return checkNeeded(need, get(1430));
        }

        var data = context.data();
        var mode = context.accountMode();
        var inventoryQuantity = quantityIn(data.inventory(), need.getItemId());
        var confirmedQuantity = inventoryQuantity;

        if (inventoryQuantity >= need.getQuantity())
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.INVENTORY, inventoryQuantity,
                    Confidence.VERIFIED,
                    get(566)
            );
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            var remaining = Math.max(0, need.getQuantity() - inventoryQuantity);
            StoredResource stored = findVerifiedStoredResource(
                    data.storage(), need.getItemId(), remaining);
            if (stored != null)
            {
                var needsAccessCheck = stored.requiresAccessCheck();
                confirmedQuantity = safeAdd(inventoryQuantity, stored.quantity);
                return new AcquisitionPlan(
                        need,
                        AcquisitionSource.VERIFIED_STORAGE,
                        confirmedQuantity,
                        needsAccessCheck
                                ? Confidence.CHECK_NEEDED
                                : Confidence.VERIFIED,
                        needsAccessCheck
                                ? get(575)
                                        + pretty(stored.capabilities)
                                        + get(576)
                                : get(577)
                                        + pretty(stored.capabilities) + "."
                );
            }
        }
        else
        {
            var bankQuantity = quantityIn(data.bank(), need.getItemId());
            var ordinaryQuantity = safeAdd(inventoryQuantity, bankQuantity);
            confirmedQuantity = ordinaryQuantity;
            if (ordinaryQuantity >= need.getQuantity())
            {
                return new AcquisitionPlan(
                        need, AcquisitionSource.BANK, ordinaryQuantity,
                        Confidence.VERIFIED,
                        get(578)
                );
            }

            if (AccountModePolicy.mayUseGroupStorage(
                    mode, context.usesGroupStorage()))
            {
                var groupStorage = data.groupStorage();
                int groupQuantity = groupStorage != null
                        && groupStorage.isObserved()
                        ? quantityIn(groupStorage, need.getItemId()) : 0;
                if (groupStorage != null && groupStorage.isObserved())
                {
                    confirmedQuantity = safeAdd(ordinaryQuantity, groupQuantity);
                    if (confirmedQuantity >= need.getQuantity())
                    {
                        return new AcquisitionPlan(
                                need, AcquisitionSource.GROUP_STORAGE,
                                confirmedQuantity,
                                Confidence.VERIFIED,
                                get(579)
                        );
                    }
                }
            }
        }

        var sourceNote = sourceSuggestions(need, context);

        // Do not turn an unobserved container into a proven shortfall. An
        // inventory read is required for every mode; ordinary accounts also
        // require the bank, and opted-in GIM requires fresh Group Storage.
        if (data.inventory() == null)
            return checkNeeded(need,
                    get(580));
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
            return checkNeeded(need,
                    get(581));
        if (AccountModePolicy.mayUseGroupStorage(mode,
                context.usesGroupStorage())
                && (data.groupStorage() == null
                || !data.groupStorage().isObserved()))
            return checkNeeded(need,
                    get(582));

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.GRAND_EXCHANGE, confirmedQuantity,
                    Confidence.CHECK_NEEDED,
                    get(567)
                            + sourceNote
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.SELF_SOURCE, confirmedQuantity,
                    Confidence.CHECK_NEEDED,
                    (mode == AccountMode.ULTIMATE_IRONMAN
                            ? get(568)
                            : get(569))
                            + sourceNote
            );
        }

        return checkNeeded(need,
                get(570) + sourceNote);
    }

    /**
     * Plans a shortfall already proven by another evidence-aware evaluator.
     * The root quantity is the missing quantity, so owned state must not be
     * subtracted a second time here.
     */
    public AcquisitionPlan planKnownShortfall(
            StrategyContext context,
            ResourceNeed shortfall)
    {
        if (context == null || shortfall == null || context.data() == null)
        {
            return checkNeeded(shortfall, get(1430));
        }

        var mode = context.accountMode();
        var sourceNote = sourceSuggestions(shortfall, context);
        String prefix = get(1431) + shortfall.getQuantity()
                + " × " + shortfall.getItemName() + ". ";

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new AcquisitionPlan(
                    shortfall, AcquisitionSource.GRAND_EXCHANGE, 0,
                    Confidence.CHECK_NEEDED,
                    prefix + get(571)
                            + sourceNote);
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new AcquisitionPlan(
                    shortfall, AcquisitionSource.SELF_SOURCE, 0,
                    Confidence.CHECK_NEEDED,
                    prefix + (mode == AccountMode.ULTIMATE_IRONMAN
                            ? get(572)
                            : get(1432))
                            + sourceNote);
        }

        return checkNeeded(shortfall,
                prefix + get(573)
                        + sourceNote);
    }

    /** Builds an ordered chain without pretending prose source hints are verified unlocks. */
    public ResourceAcquisitionChain planChain(StrategyContext context,
            ResourceNeed need)
    {
        var ownership = plan(context, need);
        List<ResourceAcquisitionStep> steps = new ArrayList<>();
        int shortfall = ownership == null || need == null ? 0
                : Math.max(0, need.getQuantity() - ownership.getConfirmedQuantity());
        if (ownership == null || need == null)
            return new ResourceAcquisitionChain(need, shortfall, steps);

        if (ownership.hasEnoughConfirmed())
        {
            steps.add(new ResourceAcquisitionStep(ownership.getSource(),
                    ownership.getNote(), ownership.getConfidence()));
            return new ResourceAcquisitionChain(need, 0, steps);
        }

        steps.add(new ResourceAcquisitionStep(ownership.getSource(),
                ownership.getNote(), ownership.getConfidence()));
        if (sourceCatalog != null && context != null)
        {
            List<String> routes = sourceCatalog.suggestions(need.getItemName(),
                    context.accountMode(), membership(context),
                    context.allowsWilderness());
            for (String route : routes)
                steps.add(new ResourceAcquisitionStep(
                        context.accountMode().usesGrandExchange()
                                ? AcquisitionSource.GRAND_EXCHANGE
                                : AcquisitionSource.SELF_SOURCE,
                        route, Confidence.CHECK_NEEDED));
        }
        return new ResourceAcquisitionChain(need, shortfall, steps);
    }

    /** Resolves acquisition prerequisites recursively with bounded, cycle-safe traversal. */
    public DependencyResolution resolveDependencies(
            StrategyContext context, ResourceNeed need)
    {
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolve(context, need);
    }

    /**
     * Resolves a proven shortfall by canonical dependency output name. Unknown
     * names deliberately remain with the caller's conservative source guidance.
     */
    public DependencyResolution resolveKnownShortfall(
            StrategyContext context, String itemName, int quantity)
    {
        if (dependencyCatalog == null) return null;
        var definition = dependencyCatalog.forItemName(itemName);
        if (definition == null) return null;
        String canonical = definition.getItemName() == null
                ? itemName : definition.getItemName();
        var need = new ResourceNeed(definition.getItemId(), canonical, quantity);
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolveKnownShortfall(context, need);
    }

    private String sourceSuggestions(ResourceNeed need, StrategyContext context)
    {
        if (sourceCatalog == null || need == null) return "";
        List<String> suggestions = sourceCatalog.suggestions(
                need.getItemName(), context == null ? AccountMode.UNKNOWN
                        : context.accountMode(), membership(context),
                context != null && context.allowsWilderness());
        if (suggestions.isEmpty())
        {
            return get(574);
        }

        var note = new StringBuilder(" Useful route");
        if (suggestions.size() > 1) note.append("s");
        note.append(": ");
        for (int i = 0; i < suggestions.size(); i++)
        {
            if (i > 0) note.append(" | ");
            note.append(suggestions.get(i));
        }
        return note.toString();
    }

    private static MembershipStatus membership(StrategyContext context)
    {
        return context == null || context.data() == null
                || context.data().account() == null
                ? MembershipStatus.UNKNOWN
                : context.data().account().membership();
    }

    private static StoredResource findVerifiedStoredResource(
            StorageSnapshot storage,
            int itemId,
            int needed)
    {
        if (storage == null) return null;
        List<StorageCapability> safeCapabilities = new ArrayList<>();
        List<StorageCapability> restrictedCapabilities = new ArrayList<>();
        var safeQuantity = 0;
        var restrictedQuantity = 0;
        for (Map.Entry<StorageCapability, java.util.List<ItemState>> entry
                : storage.getObservedContents().entrySet())
        {
            var capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            var quantity = 0;
            for (ItemState item : entry.getValue())
            {
                if (item.getItemId() == itemId) quantity += item.getQuantity();
            }
            if (quantity <= 0) continue;
            if (requiresAdditionalAccessCheck(capability))
            {
                restrictedCapabilities.add(capability);
                restrictedQuantity = safeAdd(restrictedQuantity, quantity);
            }
            else
            {
                safeCapabilities.add(capability);
                safeQuantity = safeAdd(safeQuantity, quantity);
            }
        }
        if (safeQuantity >= needed)
            return new StoredResource(safeCapabilities, safeQuantity);
        if (safeAdd(safeQuantity, restrictedQuantity) >= needed)
        {
            safeCapabilities.addAll(restrictedCapabilities);
            return new StoredResource(safeCapabilities,
                    safeAdd(safeQuantity, restrictedQuantity));
        }
        return null;
    }

    private static boolean requiresAdditionalAccessCheck(
            StorageCapability capability)
    {
        return UimStorageMechanics.isRestrictedRetrieval(capability);
    }

    private static AcquisitionPlan checkNeeded(
            ResourceNeed need,
            String note)
    {
        return new AcquisitionPlan(
                need, AcquisitionSource.CHECK_NEEDED, 0,
                Confidence.CHECK_NEEDED, note
        );
    }

    private static int quantityIn(ItemsState items, int itemId)
    {
        return items == null ? 0 : quantityInItems(items.getItems(), itemId);
    }

    private static int quantityInItems(Iterable<ItemState> items, int itemId)
    {
        var total = 0;
        for (ItemState item : items)
        {
            if (item.getItemId() == itemId) total = safeAdd(total, item.getQuantity());
        }
        return total;
    }

    private static int safeAdd(int left, int right)
    {
        var safeRight = Math.max(0, right);
        if (left > Integer.MAX_VALUE - safeRight) return Integer.MAX_VALUE;
        return left + safeRight;
    }

    private static String pretty(StorageCapability capability)
    {
        return capability.name().toLowerCase().replace('_', ' ');
    }

    private static String pretty(List<StorageCapability> capabilities)
    {
        if (capabilities == null || capabilities.isEmpty())
            return get(1955);
        List<String> names = new ArrayList<>();
        for (StorageCapability capability : capabilities)
            names.add(pretty(capability));
        if (names.size() == 1) return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1))
                + " and " + names.get(names.size() - 1);
    }

    private static final class StoredResource
    {
        private final List<StorageCapability> capabilities;
        private final int quantity;

        private StoredResource(List<StorageCapability> capabilities,
                int quantity)
        {
            this.capabilities = new ArrayList<>(capabilities);
            this.quantity = quantity;
        }

        private boolean requiresAccessCheck()
        {
            for (StorageCapability capability : capabilities)
                if (requiresAdditionalAccessCheck(capability)) return true;
            return false;
        }
    }
}

/** Recursively resolves verified resource recipes with strict termination. */
@Singleton
class ResourceDependencyResolver
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

/** Top-level strategist coordinator. */
@Singleton
class StrategyEngine
{
    private final RecommendationEngine recommendationEngine;
    private final OpportunityEngine opportunityEngine;
    private final StrategyCandidateRegistry candidateRegistry;
    private final ActionabilityPolicy actionabilityPolicy;
    private final RecommendationIntelligenceService intelligenceService;
    private final CandidateSafetyPolicy candidateSafetyPolicy;
    private final GoalDependencyProvenanceService goalProvenanceService;
    private final RecommendationDeduplicator deduplicator =
            new RecommendationDeduplicator();
    private final StrategicPlanService strategicPlanService =
            new StrategicPlanService();
    private final InfrastructureRecommendationValueService infrastructureValue =
            new InfrastructureRecommendationValueService();
    private final MethodRecommendationValueService methodValue;
    private final FinalExecutionPlanValidator finalExecutionValidator;
    private final ActivityStrategyKnowledgeService activityStrategyKnowledge =
            new ActivityStrategyKnowledgeService();
    private final QuestRecommendationValueService questValue =
            new QuestRecommendationValueService();
    private static final FarmingAccessCatalog FARMING_ACCESS_CATALOG =
            new FarmingAccessCatalog();

    @Inject
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy,
            GoalDependencyProvenanceService goalProvenanceService,
            MethodRecommendationValueService methodValue,
            FinalExecutionPlanValidator finalExecutionValidator)
    {
        this.recommendationEngine = recommendationEngine;
        this.opportunityEngine = opportunityEngine;
        this.candidateRegistry = candidateRegistry;
        this.actionabilityPolicy = actionabilityPolicy == null
                ? new ActionabilityPolicy()
                : actionabilityPolicy;
        this.intelligenceService = intelligenceService == null
                ? new RecommendationIntelligenceService()
                : intelligenceService;
        this.candidateSafetyPolicy = candidateSafetyPolicy == null
                ? new CandidateSafetyPolicy() : candidateSafetyPolicy;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
        this.methodValue = methodValue == null
                ? new MethodRecommendationValueService() : methodValue;
        this.finalExecutionValidator = finalExecutionValidator == null
                ? new FinalExecutionPlanValidator() : finalExecutionValidator;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService,
            CandidateSafetyPolicy candidateSafetyPolicy,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                candidateSafetyPolicy, goalProvenanceService,
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy,
            RecommendationIntelligenceService intelligenceService)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy, intelligenceService,
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService(),
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public StrategyEngine(
            RecommendationEngine recommendationEngine,
            OpportunityEngine opportunityEngine,
            Object unusedModules,
            StrategyCandidateRegistry candidateRegistry,
            ActionabilityPolicy actionabilityPolicy)
    {
        this(recommendationEngine, opportunityEngine,
                candidateRegistry, actionabilityPolicy,
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy(), new GoalDependencyProvenanceService(),
                new MethodRecommendationValueService(),
                new FinalExecutionPlanValidator());
    }

    public StrategyResult evaluate(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent,
                QuestTolerance.NORMAL, GoalType.MAX,
                true, false, false, preferenceProfile);
    }

    public StrategyResult evaluate(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            PreferenceProfile preferenceProfile)
    {
        return evaluate(data, strategyMode, sessionIntent, questTolerance,
                activeGoal, useGroupStorage, collectionistMode, false,
                preferenceProfile);
    }

    public StrategyResult evaluate(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        if (data == null || data.account() == null)
        {
            return new StrategyResult(
                    Collections.singletonList(
                            FallbackRecommendationFactory.forState(data)),
                    Collections.emptyList());
        }

        StrategyContext context = new StrategyContext(
                data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods,
                preferenceProfile);

        List<Opportunity> opportunities = new ArrayList<>();
        List<Opportunity> evaluatedOpportunities = opportunityEngine == null
                ? Collections.emptyList() : opportunityEngine.evaluate(data);
        for (Opportunity opportunity : evaluatedOpportunities)
        {
            if (opportunity == null
                    || opportunity.getConfidence() == Confidence.BLOCKED
                    || context.preferenceProfile().isOnCooldown(
                    opportunity.id)
                    || !candidateSafetyPolicy.isAllowed(
                    opportunitySafety(opportunity), context)) continue;
            opportunities.add(opportunity);
        }

        // The global queue needs the complete skill candidate pool. Trimming to
        // three inside RecommendationEngine can hide a lower-scoring executable
        // action behind three unresolved skills before actionability is checked.
        List<Recommendation> pool = new ArrayList<>(
                recommendationEngine == null ? Collections.emptyList()
                : recommendationEngine.recommendAll(
                        data,
                        context.mode(),
                        context.intent(),
                        context.usesGroupStorage(),
                        context.allowsWilderness(),
                        context.goal(),
                        context.preferenceProfile()));

        if (candidateRegistry != null)
        {
            for (CandidateProvider provider : candidateRegistry.getProviders())
            {
                var candidates = provider.candidates(context);
                if (candidates == null || candidates.isEmpty()) continue;
                var superseded = provider.supersededCandidateIds();
                if (superseded != null && !superseded.isEmpty())
                    pool.removeIf(value -> value != null
                            && superseded.contains(value.id));
                for (Recommendation candidate : candidates)
                {
                    if (candidate == null
                            || candidate.getConfidence() == Confidence.BLOCKED)
                    {
                        continue;
                    }
                    Recommendation sourced = activityStrategyKnowledge.attach(
                            candidate, context);
                    if (sourced != null) pool.add(sourced);
                }
            }
        }

        for (Opportunity opportunity : opportunities)
        {
            var promoted = opportunityRecommendation(opportunity, context);
            if (promoted != null) pool.add(promoted);
        }

        List<Recommendation> attributed = new ArrayList<>(pool.size());
        for (Recommendation recommendation : pool)
            attributed.add(finalExecutionValidator.validate(methodValue.attach(questValue.attach(
                    infrastructureValue.attach(
                            goalProvenanceService.attach(recommendation,
                                    context), context), context), context), context));
        pool = attributed;

        // Only after legality/actionability is known do we compare account value
        // across skills, quests, upgrades, detours, PvM, gear and minigames.
        var recommendations = buildPlayerQueue(pool, context);
        if (recommendations.isEmpty())
        {
            recommendations = Collections.singletonList(
                    FallbackRecommendationFactory.forState(data));
        }
        if (!recommendations.isEmpty())
        {
            java.util.Set<String> promotedIds = new java.util.HashSet<>();
            for (Recommendation recommendation : recommendations)
                if (recommendation.id.startsWith("opportunity:"))
                    promotedIds.add(recommendation.id);
            opportunities.removeIf(value -> promotedIds.contains(value.id));
        }
        StrategicPlan plan = strategicPlanService.build(
                recommendations, context, System.currentTimeMillis());
        return new StrategyResult(recommendations, opportunities, plan);
    }

    Recommendation opportunityRecommendation(
            Opportunity opportunity,
            StrategyContext context)
    {
        if (opportunity == null || !opportunity.isReady()
                || opportunity.getConfidence() != Confidence.VERIFIED)
        {
            return null;
        }
        var id = opportunity.id;
        var preferences = context.preferenceProfile();
        if (preferences.isOnCooldown(id)) return null;

        var setupVerified = opportunity.isSetupVerified();
        if (!setupVerified && opportunity.getPreparation().isEmpty()) return null;
        var location = opportunityLocation(opportunity.getType(), context);
        String action = opportunityAction(opportunity.getType(),
                opportunity.getTitle());
        if (location == null || action == null) return null;
        String supplies = setupVerified
                ? get(720)
                : get(1490) + String.join(", ", opportunity.getPreparation()) + ".";
        double score = 46.0 + preferences.weightFor(id) * 10.0
                + preferences.timedScoreAdjustmentFor(id);
        if (opportunity.getType() == OpportunityType.HERB_RUN
                || opportunity.getType() == OpportunityType.BIRDHOUSE_RUN
                || opportunity.getType() == OpportunityType.FARMING_CONTRACT)
        {
            score += 8.0;
        }
        Guidance guidance = new Guidance(
                setupVerified
                        ? action
                        : get(722) + opportunity.getTitle()
                                + get(1491),
                supplies,
                location,
                setupVerified
                        ? get(723)
                        : get(724));
        return new Recommendation(
                id, opportunity.getTitle(),
                get(725),
                score, null, setupVerified
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED,
                0, 0, guidance,
                opportunitySafety(opportunity));
    }

    private static String opportunityAction(
            OpportunityType type, String title)
    {
        if (type == null) return null;
        switch (type)
        {
            case BIRDHOUSE_RUN:
                return get(726);
            case HERB_RUN:
                return get(727);
            case BATTLESTAVES:
                return get(728);
            default:
                return null;
        }
    }

    private static String opportunityLocation(
            OpportunityType type, StrategyContext context)
    {
        if (type == null) return null;
        switch (type)
        {
            case BIRDHOUSE_RUN:
                return get(729);
            case HERB_RUN:
                return verifiedHerbPatchRoute(context);
            case BATTLESTAVES:
                return get(721);
            default:
                return null;
        }
    }

    private static String verifiedHerbPatchRoute(StrategyContext context)
    {
        FarmingSnapshot farming = context == null || context.data() == null
                ? null : context.data().farming();
        if (farming == null) return null;
        List<String> names = new ArrayList<>();
        for (FarmingAccessDefinition definition : FARMING_ACCESS_CATALOG.all())
        {
            if (definition.isHerbPatch()
                    && farming.isPatchReachable(definition.id))
            {
                names.add(definition.getDisplayName());
            }
        }
        if (names.isEmpty()) return null;
        return String.join(" -> ", names) + ".";
    }

    private static SafetyEvidence opportunitySafety(Opportunity opportunity)
    {
        switch (opportunity.getType())
        {
            case BIRDHOUSE_RUN:
                return SafetyEvidence.skill(false, net.runelite.api.Skill.HUNTER);
            case HERB_RUN:
            case TREE_RUN:
            case FARMING_CONTRACT:
                return SafetyEvidence.skill(false, net.runelite.api.Skill.FARMING);
            case KINGDOM:
            case KINGDOM_APPROVAL:
            case BATTLESTAVES:
            case DYNAMITE:
                return SafetyEvidence.harmless(false);
            case TEARS_OF_GUTHIX:
            case DAILY_DIARY_REWARD:
                return SafetyEvidence.potentiallyIrreversible(false);
            case CLUE:
                return opportunity.getSafetyEvidence();
            default:
                return SafetyEvidence.potentiallyIrreversible(false);
        }
    }

    /** Compatibility entry used by focused queue/actionability tests. */
    List<Recommendation> buildPlayerQueue(List<Recommendation> pool)
    {
        return buildPlayerQueue(pool, null);
    }

    /**
     * A high raw score cannot buy its way into DO NEXT while the candidate is
     * still unresolved. Ready actions are ranked against ready actions first;
     * Check Needed work is allowed only in the secondary slots and only when a
     * real primary action exists.
     */
    List<Recommendation> buildPlayerQueue(
            List<Recommendation> pool,
            StrategyContext context)
    {
        if (pool == null || pool.isEmpty()) return Collections.emptyList();

        List<Recommendation> ready = new ArrayList<>();
        List<Recommendation> secondary = new ArrayList<>();
        for (Recommendation recommendation : deduplicator.deduplicate(pool))
        {
            recommendation = goalProvenanceService.attach(
                    recommendation, context);
            var semanticKey = deduplicator.semanticKey(recommendation);
            if (context != null && (context.preferenceProfile()
                    .isOnCooldown(recommendation.id)
                    || context.preferenceProfile()
                    .isSemanticOnCooldown(semanticKey))) continue;
            if (!candidateSafetyPolicy.isAllowed(recommendation, context)) continue;
            if (!actionabilityPolicy.mayAppearAsAlternative(recommendation)) continue;
            if (actionabilityPolicy.canLeadQueue(recommendation)) ready.add(recommendation);
            else secondary.add(recommendation);
        }

        Comparator<Recommendation> byAccountValue = Comparator
                .comparingDouble((Recommendation recommendation) ->
                        intelligenceService.rankScore(recommendation, context)
                                + semanticPreferenceScore(recommendation,
                                        context))
                .reversed()
                .thenComparing(Recommendation::getId,
                        Comparator.nullsLast(String::compareTo));
        ready.sort(byAccountValue);
        secondary.sort(byAccountValue);

        // Never put a Needs Info recommendation in the primary slot merely to
        // avoid an empty card. No recommendation is safer than false certainty.
        if (ready.isEmpty()) return Collections.emptyList();

        List<Recommendation> result = new ArrayList<>(3);
        Set<String> representedDimensions = new HashSet<>();
        addDiverse(result, representedDimensions, ready);
        addDiverse(result, representedDimensions, secondary);
        return result;
    }

    /**
     * Alternative slots are product choices, not an unfiltered scoreboard.
     * Keep at most one candidate per activity dimension unless the candidates
     * are different skills, which are inherently different playable sessions.
     */
    private static void addDiverse(List<Recommendation> result,
            Set<String> representedDimensions, List<Recommendation> candidates)
    {
        for (Recommendation recommendation : candidates)
        {
            if (result.size() >= 3) return;
            var dimension = alternativeDimension(recommendation);
            if (!result.isEmpty() && representedDimensions.contains(dimension))
                continue;
            result.add(recommendation);
            representedDimensions.add(dimension);
        }
    }

    static String alternativeDimension(Recommendation recommendation)
    {
        if (recommendation == null) return "unknown";
        var plan = recommendation.plan();
        if (plan != null && plan.method() != null
                && plan.method().getSkill() != null)
            return "skill:" + plan.method().getSkill().name();
        String id = recommendation.id == null ? ""
                : recommendation.id.toLowerCase(Locale.ROOT);
        var colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(0, colon);
    }

    private double semanticPreferenceScore(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null || context == null) return 0.0;
        var key = deduplicator.semanticKey(recommendation);
        return context.preferenceProfile().semanticWeightFor(key) * 10.0
                + context.preferenceProfile()
                .semanticTimedScoreAdjustmentFor(key);
    }
}

/** Conservative recipes for deterministic RuneLite calculator actions. */
@Singleton
class UniversalActionRecipeResolver
{
    private static final Recipe[] EXACT = BundledCatalogLoader.array(
            get(1582), Recipe[].class);

    public UniversalActionRecipe resolve(ActionDef action, int count,
            MembershipStatus membership)
    {
        if (action == null || action.getSkill() == null || count <= 0)
            return unknown(get(1259));
        var name = action.getName() == null ? "" : action.getName().trim();
        var lower = name.toLowerCase(Locale.ROOT);
        var exact = exact(action.getSkill(), lower);
        if (exact != null) return exact.build(count);
        switch (action.getSkill())
        {
            case AGILITY: return none(get(899));
            case MINING: return none(get(910));
            case FISHING: return none(get(921));
            case WOODCUTTING: return none(get(932));
            case THIEVING: return none(get(935));
            case HUNTER: return none(get(936));
            case COOKING: return cooking(name, lower, count);
            case FIREMAKING: return lower.contains("log")
                    ? recipe(get(937), count, name, 1)
                    : unknown(get(938));
            case PRAYER: return contains(lower, "bones", "ashes", "head")
                    ? recipe(get(939), count, name, 1)
                    : unknown(get(900));
            case RUNECRAFT: return recipe(get(901), count,
                    membership == MembershipStatus.P2P
                            ? "Pure essence" : "Rune essence", 1);
            case CRAFTING: return crafting(name, lower, count);
            case FLETCHING: return fletching(name, lower, count);
            case SMITHING: return smithing(lower, count);
            case FARMING: return lower.endsWith(" tree")
                    ? recipe(get(930), count,
                    name.substring(0, name.length() - 5).trim() + " sapling", 1)
                    : unknown(get(929));
            case MAGIC: return lower.endsWith(" curse")
                    ? recipe(get(933), count, "Earth rune", 3,
                    "Water rune", 2, "Body rune", 1)
                    : unknown(get(934));
            case HERBLORE: return unknown(get(925));
            case CONSTRUCTION: return unknown(get(928));
            default: return unknown(get(902));
        }
    }

    private static Recipe exact(Skill skill, String name)
    {
        for (Recipe recipe : EXACT)
            if (skill.name().equals(recipe.skill)
                    && (recipe.contains ? name.contains(recipe.match)
                    : name.equals(recipe.match))) return recipe;
        return null;
    }

    private static UniversalActionRecipe cooking(String name, String lower, int n)
    {
        if (contains(lower, "jug of wine") || lower.equals("wine"))
            return recipe(get(903), n, "Grapes", 1, "Jug of water", 1);
        if (contains(lower, "cake", "pie", "pizza", "stew", "curry"))
            return unknown(get(904));
        String raw = lower.startsWith("raw ") ? name : "Raw "
                + (lower.startsWith("cooked ") ? name.substring(7) : name)
                .toLowerCase(Locale.ROOT);
        return raw.trim().length() <= 4 ? unknown(get(905))
                : recipe(get(906), n, raw, 1);
    }

    private static UniversalActionRecipe crafting(String name, String lower, int n)
    {
        if (gemName(lower) != null && !jewellery(lower) && !lower.contains("bolt"))
            return recipe("Bring a chisel.", n,
                    lower.startsWith("uncut ") ? name : "Uncut " + lower, 1);
        if (contains(lower, "beer glass", "candle lantern", "oil lamp", "vial",
                "fishbowl", "unpowered orb", "lantern lens")
                && !lower.contains("molten"))
            return recipe(get(909), n, "Molten glass", 1);
        if (lower.contains("bird house"))
            return recipe(get(911), n, wood(name, "Logs"), 1);
        if (contains(lower, "d'hide", "dragonhide"))
        {
            int hides = lower.contains("body") ? 3 : lower.contains("chaps") ? 2
                    : lower.contains("vambrace") ? 1 : 0;
            if (hides > 0) return recipe(get(1264), n,
                    capitalize(first(lower, "green", "blue", "red", "black",
                            "dragon")) + " dragon leather", hides);
        }
        if (contains(lower, "leather gloves", "leather boots", "cowl",
                get(1583), "leather body", "leather chaps", "coif"))
            return recipe(get(1264), n, "Leather", 1);
        if (contains(lower, get(1584), get(1585)))
            return recipe(get(1264), n, "Hard leather", 1);
        if (jewellery(lower))
        {
            var gem = gemName(lower);
            String bar = gem != null && contains(gem, "opal", "jade", "red topaz")
                    ? "Silver bar" : "Gold bar";
            return gem == null ? recipe(get(1265), n, bar, 1)
                    : recipe(get(1265), n, bar, 1, capitalize(gem), 1);
        }
        if (contains(lower, "battlestaff", "battlestave"))
        {
            var element = first(lower, "air", "water", "earth", "fire");
            if (element != null) return recipe(get(912), n, "Battlestaff", 1,
                    capitalize(element) + " orb", 1);
        }
        return unknown(get(913));
    }

    private static UniversalActionRecipe fletching(String name, String lower, int n)
    {
        if (lower.equals("arrow shaft") || lower.equals("arrow shafts"))
            return recipe(get(914), 1, "Logs", ceil(n, 15));
        if (lower.equals("headless arrow") || lower.equals("headless arrows"))
            return recipe(get(915), n, "Arrow shaft", 1, "Feather", 1);
        if (lower.endsWith("bow (u)"))
            return recipe("Bring a knife.", n, wood(name, "Logs"), 1);
        if (contains(lower, "shortbow", "longbow") && !lower.contains("(u)"))
            return recipe(get(1266), n, name + " (u)", 1, "Bow string", 1);
        if (lower.endsWith(" shield") || lower.endsWith(" stock"))
            return recipe("Bring a knife.", n, wood(name, null),
                    lower.endsWith(" shield") ? 2 : 1);
        var metal = firstWord(name);
        if (lower.endsWith(" dart") || lower.endsWith(" darts"))
            return recipe(get(916), n, metal + " dart tip", 1, "Feather", 1);
        if (lower.contains("broad arrow")) return recipe(get(917), n,
                "Headless arrow", 1, "Broad arrowhead", 1);
        if (projectile(lower, "arrow")) return recipe(get(918), n,
                "Headless arrow", 1, metal + " arrowhead", 1);
        if (projectile(lower, "javelin")) return recipe(get(919), n,
                "Javelin shaft", 1, metal + " javelin head", 1);
        if (basicBolt(lower)) return recipe(get(920), n,
                metal + " bolts (unf)", 1, "Feather", 1);
        return unknown(get(922));
    }

    private static UniversalActionRecipe smithing(String lower, int n)
    {
        var bars = smithingBarsFor(lower);
        String metal = first(lower, "bronze", "iron", "steel", "mithril",
                "adamant", "rune");
        if (bars <= 0 || metal == null) return unknown(get(924));
        String bar = metal.equals("adamant") ? "Adamantite bar"
                : metal.equals("rune") ? "Runite bar" : capitalize(metal) + " bar";
        return recipe(get(923), n, bar, bars);
    }

    static int smithingBarsFor(String value)
    {
        var lower = value == null ? "" : value;
        if (lower.contains("platebody")) return 5;
        if (contains(lower, "plateskirt", "platelegs", "2h sword", "kiteshield",
                "chainbody", "battleaxe", "warhammer")) return 3;
        if (contains(lower, "claws", "full helm", "sq shield", "longsword",
                "scimitar")) return 2;
        return contains(lower, "mace", "sword", "dagger", " axe", "med helm",
                "dart tip", "knife", "arrowtip", "nails", "wire",
                "unfinished bolt") ? 1 : 0;
    }

    private static UniversalActionRecipe recipe(String setup, int count,
            Object... items)
    {
        List<MethodInput> inputs = new ArrayList<>();
        for (int i = 0; i + 1 < items.length; i += 2)
            if (items[i] != null && (Integer) items[i + 1] > 0)
                inputs.add(new MethodInput((String) items[i], -1,
                        multiply(count, (Integer) items[i + 1])));
        return new UniversalActionRecipe(inputs, setup, true);
    }

    private static UniversalActionRecipe none(String setup)
    {
        return UniversalActionRecipe.noConsumedInputs(setup);
    }
    private static UniversalActionRecipe unknown(String setup)
    {
        return UniversalActionRecipe.unknown(setup);
    }
    private static boolean contains(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return true;
        return false;
    }
    private static String first(String text, String... parts)
    {
        for (String part : parts) if (text.contains(part)) return part;
        return null;
    }
    private static String gemName(String lower)
    {
        return first(lower, "red topaz", "opal", "jade", "sapphire", "emerald",
                "ruby", "diamond", "dragonstone", "onyx", "zenyte");
    }
    private static boolean jewellery(String lower)
    {
        return lower.endsWith(" ring") || lower.endsWith(" bracelet")
                || lower.endsWith(" necklace") || lower.contains("amulet");
    }
    private static String wood(String name, String fallback)
    {
        String value = first(name.toLowerCase(Locale.ROOT), "redwood", "magic",
                "yew", "mahogany", "maple", "teak", "willow", "oak");
        return value == null ? fallback : capitalize(value) + " logs";
    }
    private static boolean projectile(String lower, String kind)
    {
        return (lower.endsWith(" " + kind) || lower.endsWith(" " + kind + "s"))
                && contains(lower, "bronze", "iron", "steel", "mithril",
                "adamant", "rune", "amethyst", "dragon");
    }
    private static boolean basicBolt(String lower)
    {
        return (lower.endsWith(" bolt") || lower.endsWith(" bolts"))
                && contains(lower, "bronze", "blurite", "iron", "silver",
                "steel", "mithril", "adamant", "runite", "rune", "dragon")
                && !contains(lower, "opal", "pearl", "barbed", "kebbit",
                "sapphire", "emerald", "ruby", "diamond", "dragonstone",
                "onyx", "amethyst", "broad");
    }
    private static int multiply(int a, int b)
    {
        return a > Integer.MAX_VALUE / b ? Integer.MAX_VALUE : a * b;
    }
    private static int ceil(int value, int divisor)
    {
        return (value + divisor - 1) / divisor;
    }
    private static String capitalize(String value)
    {
        return value == null || value.isEmpty() ? "Dragon"
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    private static String firstWord(String value)
    {
        var space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space);
    }

    private static final class Recipe
    {
        private String skill, match, setup;
        private String[] inputs;
        private int[] units;
        private boolean contains;

        private UniversalActionRecipe build(int count)
        {
            List<MethodInput> result = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++)
                result.add(new MethodInput(inputs[i], -1,
                        multiply(count, units[i])));
            return new UniversalActionRecipe(result, setup, true);
        }
    }
}
