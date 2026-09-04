package compass;
import lombok.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import java.util.*;
import java.util.Deque;
import javax.inject.*;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.StatChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;
import static compass.Text.get;

/**
 * Learns positive access facts from normal gameplay without user prompts.
 *
 * <p>Walking into a known Farming region is proof that this character can reach
 * it. That proof is remembered per character and can satisfy future readiness
 * checks even after RuneLite is restarted.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class AccessObservationService
{
    private final Client client;
    private final AccountAccessMemoryStore memoryStore;
    private final FarmingAccessCatalog farmingAccessCatalog;
    private int lastRegionId = -1;

    /**
     * @return true only when newly learned evidence can affect current strategy.
     */
    public boolean observeCurrentLocation()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return false;
        }

        var player = client.getLocalPlayer();
        if (player == null)
        {
            return false;
        }

        var location = player.getWorldLocation();
        if (location == null)
        {
            return false;
        }

        var regionId = location.getRegionID();
        if (regionId == lastRegionId)
        {
            return false;
        }

        lastRegionId = regionId;

        // Generic region memory is useful later for transport/content discovery,
        // but it does not currently require an immediate recommendation rerank.
        memoryStore.remember("region." + regionId);

        FarmingAccessDefinition farming =
                farmingAccessCatalog.forRegion(regionId);
        return farming != null
                && memoryStore.remember(farming.observationKey());
    }

    public void clearForAccountChange()
    {
        lastRegionId = -1;
        memoryStore.clearCacheForAccountChange();
    }
}

/**
 * Derives property-first strategy priorities from mode mechanics and observed
 * account state. It does not name or select training methods.
 */
@Singleton
final class AccountStrategicPriorityService
{
    public AccountPriorities assess(StrategyContext context)
    {
        if (context == null)
            return assess(AccountMode.UNKNOWN, null, false);
        return assess(context.accountMode(), context.data(),
                context.usesGroupStorage());
    }

    public AccountPriorities assess(AccountMode requestedMode,
            GameData data, boolean useGroupStorage)
    {
        AccountMode mode = requestedMode == null
                ? AccountMode.UNKNOWN : requestedMode;
        EnumMap<AccountDimension, AccountPriority> result =
                new EnumMap<>(AccountDimension.class);

        if (mode == AccountMode.UNKNOWN)
        {
            unknown(result);
            return new AccountPriorities(mode, result);
        }

        var ge = AccountModePolicy.mayUseGrandExchange(mode);
        var selfSource = AccountModePolicy.requiresSelfSourcing(mode);
        var uim = AccountModePolicy.requiresCapabilityCheckedStorage(mode);
        var group = mode.isGroupIronman();
        boolean hardcore = mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN;

        var inventory = data == null ? null : data.inventory();
        int occupied = inventory == null
                || !inventory.hasCompleteSlotObservation() ? -1
                : UimSetupCostService.occupiedInventorySlots(inventory);
        put(result, AccountDimension.INVENTORY_PRESSURE,
                uim ? occupied >= 24 ? Priority.CRITICAL
                        : Priority.HIGH : Priority.LOW,
                occupied < 0 ? Confidence.CHECK_NEEDED
                        : Confidence.VERIFIED,
                uim ? occupied < 0
                        ? get(89)
                        : get(1434) + occupied
                                + get(100)
                        : get(111));
        put(result, AccountDimension.BANK_AVAILABILITY,
                uim ? Priority.CRITICAL : Priority.LOW,
                uim ? Capability.BLOCKED : Capability.VERIFIED,
                Confidence.VERIFIED,
                uim ? get(122)
                        : get(124));
        put(result, AccountDimension.GRAND_EXCHANGE_AVAILABILITY,
                ge || selfSource ? Priority.HIGH : Priority.LOW,
                ge ? Capability.VERIFIED : Capability.BLOCKED,
                Confidence.VERIFIED,
                ge ? get(125)
                        : get(126));
        put(result, AccountDimension.SELF_SOURCING_BURDEN,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.HIGH
                        : Priority.LOW,
                Confidence.VERIFIED,
                selfSource ? get(127)
                        : get(128));

        ItemsState groupStorage = data == null
                ? null : data.groupStorage();
        boolean freshGroupStorage = group && useGroupStorage
                && groupStorage != null && groupStorage.isObserved();
        Confidence groupConfidence = freshGroupStorage
                ? Confidence.VERIFIED
                : Confidence.CHECK_NEEDED;
        put(result, AccountDimension.SHARED_RESOURCE_VALUE,
                freshGroupStorage ? Priority.HIGH
                        : Priority.NONE,
                freshGroupStorage ? Capability.VERIFIED
                        : group ? Capability.UNKNOWN
                        : Capability.BLOCKED,
                group ? groupConfidence : Confidence.VERIFIED,
                freshGroupStorage
                        ? get(90)
                        : group ? get(91)
                        : get(92));
        put(result, AccountDimension.SHARED_INFRASTRUCTURE_VALUE,
                Priority.NONE,
                group ? Capability.UNKNOWN : Capability.BLOCKED,
                group ? Confidence.CHECK_NEEDED
                        : Confidence.VERIFIED,
                group ? get(93)
                        : get(94));
        put(result, AccountDimension.STORAGE_VALUE,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.MODERATE
                        : Priority.LOW,
                Confidence.VERIFIED,
                uim ? get(95)
                        : selfSource ? get(96)
                        : get(97));
        put(result, AccountDimension.POH_VALUE,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.HIGH
                        : Priority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(98)
                        : selfSource ? get(99)
                        : get(101));
        put(result, AccountDimension.TELEPORT_INFRASTRUCTURE_VALUE,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.HIGH
                        : Priority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(102)
                        : selfSource ? get(103)
                        : get(104));
        put(result, AccountDimension.SETUP_COST_SENSITIVITY,
                uim ? Priority.CRITICAL
                        : hardcore ? Priority.HIGH
                        : Priority.MODERATE,
                Confidence.VERIFIED,
                uim ? get(105)
                        : hardcore ? get(106)
                        : get(107));
        put(result, AccountDimension.DEATH_RISK_SENSITIVITY,
                hardcore ? Priority.CRITICAL
                        : uim ? Priority.HIGH
                        : Priority.MODERATE,
                Confidence.VERIFIED,
                hardcore ? get(108)
                        : uim ? get(109)
                        : get(110));
        put(result, AccountDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.HIGH
                        : Priority.LOW,
                Confidence.VERIFIED,
                selfSource ? get(112)
                        : get(113));
        put(result, AccountDimension.STORABLE_EQUIPMENT_VALUE,
                uim ? Priority.CRITICAL
                        : selfSource ? Priority.MODERATE
                        : Priority.LOW,
                Confidence.VERIFIED,
                uim ? get(114)
                        : get(115));
        put(result, AccountDimension.DUPLICATE_GRIND_PENALTY,
                freshGroupStorage ? Priority.HIGH
                        : Priority.NONE,
                group ? groupConfidence : Confidence.VERIFIED,
                freshGroupStorage
                        ? get(116)
                        : group ? get(117)
                        : get(118));
        put(result, AccountDimension.GP_LIQUIDITY_STORAGE_VALUE,
                uim ? Priority.HIGH
                        : selfSource ? Priority.MODERATE
                        : Priority.LOW,
                Confidence.VERIFIED,
                uim ? get(119)
                        : selfSource ? get(120)
                        : get(121));

        return new AccountPriorities(mode, result);
    }

    private static void unknown(
            Map<AccountDimension, AccountPriority> values)
    {
        for (AccountDimension dimension
                : AccountDimension.values())
        {
            Priority priority = dimension
                    == AccountDimension.BANK_AVAILABILITY
                    || dimension
                    == AccountDimension.GRAND_EXCHANGE_AVAILABILITY
                    || dimension
                    == AccountDimension.SELF_SOURCING_BURDEN
                    ? Priority.CRITICAL : Priority.NONE;
            put(values, dimension, priority,
                    Capability.UNKNOWN,
                    Confidence.CHECK_NEEDED,
                    get(123));
        }
    }

    private static void put(
            Map<AccountDimension, AccountPriority> values,
            AccountDimension dimension,
            Priority priority,
            Confidence confidence,
            String reason)
    {
        put(values, dimension, priority, Capability.VERIFIED, confidence,
                reason);
    }

    private static void put(
            Map<AccountDimension, AccountPriority> values,
            AccountDimension dimension,
            Priority priority,
            Capability capabilityState,
            Confidence confidence,
            String reason)
    {
        values.put(dimension, new AccountPriority(dimension,
                priority, capabilityState, confidence, reason));
    }
}

/** Applies cross-domain sourced strategy and plan-relative UIM inventory fit. */
@Singleton
final class ActivityStrategyKnowledgeService
{
    private final ActivityStrategyKnowledgeCatalog catalog;
    private final UimInventoryResolutionService inventoryResolution;

    @Inject
    public ActivityStrategyKnowledgeService(ActivityStrategyKnowledgeCatalog catalog,
            UimInventoryResolutionService inventoryResolution)
    {
        this.catalog = catalog == null
                ? new ActivityStrategyKnowledgeCatalog() : catalog;
        this.inventoryResolution = inventoryResolution == null
                ? new UimInventoryResolutionService() : inventoryResolution;
    }

    public ActivityStrategyKnowledgeService(ActivityStrategyKnowledgeCatalog catalog)
    {
        this(catalog, new UimInventoryResolutionService());
    }

    public ActivityStrategyKnowledgeService()
    {
        this(new ActivityStrategyKnowledgeCatalog());
    }

    /** Returns null when exact live inventory proves the plan cannot fit. */
    public Recommendation attach(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null || context == null) return recommendation;
        ActivityStrategyProfile profile = catalog.profileFor(
                recommendation.id, context.accountMode());
        if (profile == null) return recommendation;

        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                && !fitsObservedInventory(context.data(), profile,
                        inventoryResolution))
            return null;

        StrategicValue.Builder sourced =
                StrategicValue.builder()
                        .setupReuse(profile.setupReuse);
        for (Source source : profile.getSources())
            sourced.evidence(get(1608) + source.name());
        return recommendation.withStrategicValue(
                recommendation.strategicValue.merge(sourced.build()));
    }

    private static boolean fitsObservedInventory(GameData data,
            ActivityStrategyProfile profile,
            UimInventoryResolutionService inventoryResolution)
    {
        var inventory = data == null ? null : data.inventory();
        var footprint = profile.inventoryFootprint;
        if (footprint == null) return true;
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return footprint.minimumPracticalFreeSlots == 0;
        UimInventoryResolution result = inventoryResolution.resolve(data,
                footprint, false, false, emptyList());
        return result.getKind() == UimInventoryKind.USE_AS_IS;
    }
}

/** Produces seven evidence-separated gear answers for one encounter context. */
@Singleton
final class ContextualGearDecisionService
{
    private final GearAcquisitionCatalog acquisition =
            new GearAcquisitionCatalog();

    public GearAssessment assess(GearProgressionEntry entry,
            StrategyContext context)
    {
        Map<GearAspect, GearDecision> result =
                new EnumMap<>(GearAspect.class);
        ItemIndex items = new ItemIndex(
                context == null ? null : context.data(),
                context != null && context.usesGroupStorage());
        var ownershipObserved = items.usableOwnershipObserved();
        List<String> owned = new ArrayList<>();
        List<String> unresolvedRoutes = new ArrayList<>();
        for (String target : entry.getRecommendedItems())
        {
            if (!isExactOwnershipTarget(target)) continue;
            if (items.has(target)) owned.add(target);
            else if (acquisition.forItem(target) != null)
                unresolvedRoutes.add(target);
        }
        String ownedValue = !ownershipObserved
                ? get(142)
                : owned.isEmpty()
                ? get(143)
                : owned.get(0);
        put(result, GearAspect.BEST_OWNED, ownedValue,
                ownershipObserved && !owned.isEmpty()
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED);
        put(result, GearAspect.BEST_USABLE,
                owned.isEmpty()
                        ? get(144)
                        : owned.get(0) + get(145),
                Confidence.CHECK_NEEDED);

        String routed = unresolvedRoutes.isEmpty() ? null
                : unresolvedRoutes.get(0);
        AccountMode mode = context == null ? AccountMode.UNKNOWN
                : context.accountMode();
        String available = routed == null
                ? get(146)
                : mode.usesGrandExchange()
                ? get(147) + routed
                : get(148) + routed;
        put(result, GearAspect.BEST_AVAILABLE_NOW, available,
                Confidence.CHECK_NEEDED);
        put(result, GearAspect.BEST_VALUE_UPGRADE,
                get(149),
                Confidence.CHECK_NEEDED);
        put(result, GearAspect.BEST_PRACTICAL_UPGRADE,
                routed == null ? entry.getWeaponGuidance()
                        : routed + get(150),
                Confidence.CHECK_NEEDED);
        put(result, GearAspect.LONG_TERM_TARGET,
                entry.getWeaponGuidance(), Confidence.CHECK_NEEDED);
        put(result, GearAspect.TARGET_SPECIFIC_BEST,
                entry.note, Confidence.CHECK_NEEDED);
        return new GearAssessment(result);
    }

    /** Compound slot prose must never be treated as proof that one exact item is missing. */
    static boolean isExactOwnershipTarget(String target)
    {
        if (target == null || target.trim().isEmpty()) return false;
        var value = target.toLowerCase(Locale.ROOT);
        return !value.contains(" or ") && !value.contains("/")
                && !value.contains("depending") && !value.contains("target-")
                && !value.contains(" mix") && !value.contains(" pieces")
                && !value.contains(" switch") && !value.contains(" as ")
                && !value.contains(" progression") && !value.contains("applicable");
    }

    private static void put(
            Map<GearAspect, GearDecision> decisions,
            GearAspect kind, String value,
            Confidence confidence)
    {
        decisions.put(kind, new GearDecision(kind, value, confidence));
    }
}

/** Reads patch varbits only while the player is in a known Farming region. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class FarmingRunObservationService
{
    private final Client client;
    private final FarmingRunCatalog catalog;
    private final FarmingPatchStateDecoder decoder;
    private final FarmingRunStateStore store;

    public boolean observeCurrentPatches()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        var player = client.getLocalPlayer();
        if (player == null) return false;
        var location = player.getWorldLocation();
        if (location == null) return false;

        var changed = false;
        List<FarmingRunPatchDefinition> patches =
                catalog.forRegion(location.getRegionID());
        for (FarmingRunPatchDefinition patch : patches)
        {
            var raw = client.getVarbitValue(patch.getVarbitId());
            var state = decoder.decode(patch.getKind(), raw);
            if (state != PatchState.UNKNOWN)
            {
                changed |= store.remember(patch.id, state);
            }
        }
        return changed;
    }

    public void clearForAccountChange()
    {
        store.clearCacheForAccountChange();
    }
}

/**
 * GIM strategy grounded only in enabled, fresh Group Storage item evidence.
 * It never infers teammate levels, roles, POH rooms, or other capabilities.
 */
@Singleton
final class GimGroupStrategyService
{
    public GroupResourceAssessment assess(
            StrategyContext context, GroupResourceNeed need)
    {
        if (need == null)
            throw new IllegalArgumentException(get(1429));
        AccountMode mode = context == null
                ? AccountMode.UNKNOWN : context.accountMode();
        if (!mode.isGroupIronman())
            return result(GroupResourceState.NOT_A_GROUP_ACCOUNT,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(287));
        if (!context.usesGroupStorage())
            return result(GroupResourceState.GROUP_STORAGE_DISABLED,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(288));
        var data = context.data();
        ItemsState storage = data == null
                ? null : data.groupStorage();
        if (storage == null || !storage.isObserved())
            return result(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                    Confidence.CHECK_NEEDED, 0, need, 0.0,
                    get(289));

        var quantity = quantity(storage, need.getAcceptableItemIds());
        if (quantity <= 0)
            return result(GroupResourceState.SHARED_STOCK_NONE,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(290));
        double fraction = min(1.0, quantity
                / (double) need.quantity);
        if (quantity < need.quantity)
            return result(GroupResourceState.SHARED_STOCK_PARTIAL,
                    Confidence.VERIFIED, quantity, need,
                    fraction * 0.45,
                    get(291));
        var avoidance = need.isReusable() ? 1.0 : 0.75;
        return result(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                Confidence.VERIFIED, quantity, need, avoidance,
                get(292));
    }

    private static GroupResourceAssessment result(GroupResourceState state,
            Confidence confidence, int quantity,
            GroupResourceNeed need, double avoidance, String reason)
    {
        return new GroupResourceAssessment(state, confidence, quantity,
                need.quantity, avoidance, reason);
    }

    private static int quantity(ItemsState storage, Set<Integer> ids)
    {
        var total = 0;
        for (ItemState item : storage.getItems())
        {
            if (item == null || !ids.contains(item.itemId)) continue;
            var amount = max(0, item.quantity);
            if (total >= Integer.MAX_VALUE - amount) return Integer.MAX_VALUE;
            total += amount;
        }
        return total;
    }
}

/** Resolves recommendation relationships from actual typed goal dependencies. */
@Singleton
class GoalDependencyProvenanceService
{
    private final GoalGraph goalGraph;
    private final QuestKnowledgeCatalog quests;

    @Inject
    public GoalDependencyProvenanceService(
            GoalGraph goalGraph, QuestKnowledgeCatalog quests)
    {
        this.goalGraph = goalGraph == null ? new GoalGraph() : goalGraph;
        this.quests = quests == null ? new QuestKnowledgeCatalog() : quests;
    }

    public GoalDependencyProvenanceService()
    {
        this(new GoalGraph(), new QuestKnowledgeCatalog());
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null) return null;
        var existing = recommendation.goalProvenance;
        if (context != null && existing != null
                && existing.proves(context.goal(),
                        recommendation.id))
            return recommendation;
        var provenance = resolve(recommendation, context);
        return recommendation.withGoalProvenance(provenance);
    }

    public GoalProvenance resolve(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null) return null;
        var goal = context.goal();
        if (goal == null || goal == GoalType.AUTOMATIC
                || goal == GoalType.CUSTOM) return null;

        var skill = recommendationSkill(recommendation);
        if (skill != null)
        {
            var path = skillPath(goal, skill, context);
            if (path == null) return null;
            return isDirectSkillGoal(goal, skill)
                    ? GoalProvenance.direct(goal,
                            recommendation.id, path)
                    : GoalProvenance.prerequisite(goal,
                            recommendation.id, path);
        }

        var quest = recommendationQuest(recommendation, context);
        if (quest != null)
        {
            var path = questPath(goal, quest, context);
            if (path == null) return null;
            boolean direct = goal == GoalType.QUEST_CAPE
                    || path.size() == 2;
            return direct
                    ? GoalProvenance.direct(goal,
                            recommendation.id, path)
                    : GoalProvenance.prerequisite(goal,
                            recommendation.id, path);
        }

        var direct = directActivityPath(goal, recommendation);
        return direct == null ? null : GoalProvenance.direct(
                goal, recommendation.id, direct);
    }

    public boolean isRequiredQuest(
            GoalType goal, String questName, StrategyContext context)
    {
        return questPath(goal, questName, context) != null;
    }

    /** Nearest still-unmet, proven skill requirement on the selected goal path. */
    public int nextRequiredSkillLevel(
            GoalType goal, Skill skill, StrategyContext context)
    {
        if (goal == null || skill == null || context == null
                || context.data() == null
                || context.data().account() == null) return 0;
        var current = context.data().account().level(skill);
        if (isDirectSkillGoal(goal, skill))
        {
            int target = goal == GoalType.BASE_70S ? 70
                    : goal == GoalType.SLAYER_85 ? 85 : 99;
            return current < target ? target : 0;
        }
        var nearest = Integer.MAX_VALUE;
        for (String quest : requiredQuestNames(goal, context))
        {
            var definition = quests.definitionFor(quest);
            if (definition == null) continue;
            int level = definition.skillRequirements
                    .getOrDefault(skill, 0);
            if (level > current) nearest = min(nearest, level);
        }
        return nearest == Integer.MAX_VALUE ? 0 : nearest;
    }

    public GoalQuestRewardForecast guaranteedRewardsBeforeManualTraining(
            StrategyContext context, Skill skill)
    {
        if (context == null || skill == null || context.data() == null
                || context.data().account() == null
                || context.data().quests() == null)
            return new GoalQuestRewardForecast(skill, 0,
                    emptyList());

        var currentLevel = context.data().account().level(skill);
        var goalQuests = requiredQuestNames(context.goal(), context);
        var experience = 0;
        List<String> sources = new ArrayList<>();
        for (String quest : goalQuests)
        {
            var status = statusOf(context, quest);
            if (status == QuestStatus.COMPLETE || status == QuestStatus.UNKNOWN)
                continue;
            var definition = quests.definitionFor(quest);
            if (definition == null
                    || definition.skillRequirements.getOrDefault(skill, 1)
                            > currentLevel
                    || !canReachWithoutTrainingSkill(definition, context,
                            skill, new HashSet<>()))
                continue;
            var reward = definition.getRewardXp().getOrDefault(skill, 0);
            if (reward <= 0) continue;
            experience += reward;
            sources.add(quest);
        }
        return new GoalQuestRewardForecast(skill, experience, sources);
    }

    private boolean canReachWithoutTrainingSkill(QuestDefinition definition,
            StrategyContext context, Skill skill, Set<String> active)
    {
        if (definition == null) return false;
        var key = Names.words(definition.getName());
        if (!active.add(key)) return false;
        for (Map.Entry<Skill, Integer> requirement
                : definition.skillRequirements.entrySet())
        {
            int current = context.data().account()
                    .level(requirement.getKey());
            var gap = requirement.getValue() - current;
            // The forecast may look through one short prerequisite grind, but
            // never treat a distant quest reward as near-term XP.
            if (gap > 0 && (requirement.getKey() == skill || gap > 10))
            {
                active.remove(key);
                return false;
            }
        }
        for (String prerequisite : definition.prerequisites)
        {
            var status = statusOf(context, prerequisite);
            if (status == QuestStatus.COMPLETE) continue;
            if (status == QuestStatus.UNKNOWN)
            {
                active.remove(key);
                return false;
            }
            if (!canReachWithoutTrainingSkill(quests.definitionFor(prerequisite),
                    context, skill, active))
            {
                active.remove(key);
                return false;
            }
        }
        active.remove(key);
        return true;
    }

    private List<String> questPath(
            GoalType goal, String questName, StrategyContext context)
    {
        if (goal == null || questName == null) return null;
        var targetStatus = statusOf(context, questName);
        if (targetStatus != QuestStatus.NOT_STARTED
                && targetStatus != QuestStatus.IN_PROGRESS) return null;
        if (goal == GoalType.QUEST_CAPE)
        {
            return list(goal.toString(), questName);
        }
        for (String root : goalGraph.questRootsFor(goal))
        {
            List<String> path = QuestGraphs.path(quests, root, questName);
            if (path != null)
            {
                List<String> result = new ArrayList<>();
                result.add(goal.toString());
                if (goal == GoalType.BOWFA) result.add(get(1722));
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> skillPath(
            GoalType goal, Skill skill, StrategyContext context)
    {
        if (isDirectSkillGoal(goal, skill))
        {
            var target = goal == GoalType.BASE_70S ? 70 : 99;
            if (goal == GoalType.SLAYER_85) target = 85;
            return list(goal.toString(), target + " " + display(skill));
        }
        if (goal == GoalType.QUEST_CAPE)
        {
            for (Map.Entry<String, QuestStatus> entry
                    : context.data().quests().quests().entrySet())
            {
                if (entry.getValue() == QuestStatus.COMPLETE
                        || entry.getValue() == QuestStatus.UNKNOWN) continue;
                List<String> path = skillPathInQuest(entry.getKey(), skill,
                        context, new HashSet<>());
                if (path != null)
                {
                    List<String> result = new ArrayList<>();
                    result.add(goal.toString());
                    result.addAll(path);
                    return result;
                }
            }
            return null;
        }
        for (String root : goalGraph.questRootsFor(goal))
        {
            List<String> path = skillPathInQuest(root, skill, context,
                    new HashSet<>());
            if (path != null)
            {
                List<String> result = new ArrayList<>();
                result.add(goal.toString());
                if (goal == GoalType.BOWFA) result.add(get(1722));
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> skillPathInQuest(String quest, Skill skill,
            StrategyContext context, Set<String> active)
    {
        var key = Names.words(quest);
        if (!active.add(key)) return null;
        var definition = quests.definitionFor(quest);
        if (definition == null)
        {
            active.remove(key);
            return null;
        }
        var questStatus = statusOf(context, quest);
        if (questStatus == QuestStatus.COMPLETE
                || questStatus == QuestStatus.UNKNOWN)
        {
            active.remove(key);
            return null;
        }
        var required = definition.skillRequirements.getOrDefault(skill, 0);
        var current = context.data().account().level(skill);
        List<String> best = null;
        if (required > current)
            best = list(quest, required + " " + display(skill));
        for (String prerequisite : definition.prerequisites)
        {
            List<String> child = skillPathInQuest(
                    prerequisite, skill, context, active);
            if (child != null)
            {
                List<String> result = new ArrayList<>();
                result.add(quest);
                result.addAll(child);
                best = strongerSkillPath(best, result);
            }
        }
        active.remove(key);
        return best;
    }

    private static List<String> strongerSkillPath(
            List<String> left, List<String> right)
    {
        if (left == null) return right;
        if (right == null) return left;
        return requirementLevel(right) > requirementLevel(left) ? right : left;
    }

    private static int requirementLevel(List<String> path)
    {
        if (path == null || path.isEmpty()) return 0;
        var value = path.get(path.size() - 1);
        var space = value.indexOf(' ');
        if (space <= 0) return 0;
        try { return Integer.parseInt(value.substring(0, space)); }
        catch (NumberFormatException ex) { return 0; }
    }

    private Set<String> requiredQuestNames(
            GoalType goal, StrategyContext context)
    {
        Set<String> result = new HashSet<>();
        if (goal == GoalType.QUEST_CAPE)
        {
            for (Map.Entry<String, QuestStatus> entry
                    : context.data().quests().quests().entrySet())
                if (entry.getValue() != QuestStatus.COMPLETE
                        && entry.getValue() != QuestStatus.UNKNOWN)
                    QuestGraphs.collect(quests, entry.getKey(), result);
            return result;
        }
        for (String root : goalGraph.questRootsFor(goal))
            QuestGraphs.collect(quests, root, result);
        return result;
    }

    private static boolean isDirectSkillGoal(GoalType goal, Skill skill)
    {
        if (skill == null) return false;
        return goal == GoalType.MAX || goal == GoalType.TOTAL_2000
                || goal == GoalType.BASE_70S
                || goal == GoalType.SLAYER_85 && skill == Skill.SLAYER;
    }

    private static List<String> directActivityPath(
            GoalType goal, Recommendation recommendation)
    {
        String identity = Names.words(recommendation.id + " "
                + recommendation.title);
        switch (goal)
        {
            case FIRE_CAPE:
                return contains(identity, "fire cape", "tztok jad",
                        get(1723))
                        ? list(goal.toString(), get(1325))
                        : null;
            case BOWFA:
                return contains(identity, "bowfa", get(1326))
                        ? list(goal.toString(), get(264))
                        : null;
            case INFERNAL_CAPE:
                return contains(identity, "inferno", "infernal cape", "tzkal zuk")
                        ? list(goal.toString(), get(1327)) : null;
            default:
                return null;
        }
    }

    private static Skill recommendationSkill(Recommendation recommendation)
    {
        var plan = recommendation.plan();
        return plan == null || plan.method() == null
                ? null : plan.method().getSkill();
    }

    private static String recommendationQuest(
            Recommendation recommendation, StrategyContext context)
    {
        var id = recommendation.id;
        if (id == null || !id.startsWith("quest:") || context.data() == null
                || context.data().quests() == null) return null;
        var slug = id.substring("quest:".length());
        for (String quest : context.data().quests().quests().keySet())
            if (slug(quest).equals(slug)) return quest;
        return null;
    }

    private static QuestStatus statusOf(
            StrategyContext context, String quest)
    {
        return QuestGraphs.status(context, quest);
    }

    private static String display(Skill skill)
    {
        var value = skill.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean contains(String value, String... tokens)
    {
        for (String token : tokens)
            if (value.contains(Names.words(token))) return true;
        return false;
    }

    private static String slug(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }


    private static List<String> list(String... values)
    {
        List<String> result = new ArrayList<>();
        addAll(result, values);
        return result;
    }
}

/** Attaches infrastructure utility to its actual typed prerequisite actions. */
@Singleton
@RequiredArgsConstructor
final class InfrastructureRecommendationValueService
{
    private final InfrastructureMilestoneCatalog catalog;
    private final InfrastructureUnlockValueService values;

    public InfrastructureRecommendationValueService()
    {
        this(new InfrastructureMilestoneCatalog(),
                new InfrastructureUnlockValueService());
    }
    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null
                || context.data() == null
                || context.data().account() == null) return recommendation;
        var merged = recommendation.strategicValue;
        for (InfrastructureMilestone definition : catalog.all())
        {
            InfraAssessment assessment = values.assess(
                    definition.id, context);
            if (assessment.getState() == InfrastructureMilestoneState.COMPLETE
                    || assessment.getState()
                            == InfrastructureMilestoneState.NOT_APPLICABLE)
                continue;
            if (!matches(recommendation, definition, context)) continue;
            double utility = assessment.strategicValue.ordinal()
                    / (double) Priority.CRITICAL.ordinal();
            merged = merged.merge(StrategicValue.builder()
                    .infrastructureValue(utility)
                    .accountModeFit(utility * 0.6)
                    .unlockValue(utility * 0.5)
                    .evidence("infrastructure:" + definition.id)
                    .build());
        }
        return recommendation.withStrategicValue(merged);
    }

    private static boolean matches(Recommendation recommendation,
            InfrastructureMilestone definition,
            StrategyContext context)
    {
        var training = recommendation.plan();
        Skill skill = training == null || training.method() == null
                ? null : training.method().getSkill();
        int current = skill == null ? 0 : context.data().account()
                .level(skill);
        var required = definition.requiredSkills.getOrDefault(skill, 0);
        if (skill != null && required > 0
                && current < required
                && recommendation.targetLevel
                        >= required) return true;

        for (String quest : definition.getRequiredQuests().keySet())
            if (recommendation.id != null
                    && recommendation.id.equals("quest:" + slug(quest)))
                return true;
        return recommendation.id != null
                && recommendation.id.equals(
                        "infrastructure:" + definition.id);
    }

    private static String slug(String value)
    {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

/** Assesses infrastructure through typed utility and observed provenance. */
@Singleton
@RequiredArgsConstructor
final class InfrastructureUnlockValueService
{
    private final InfrastructureMilestoneCatalog catalog;
    private final AccountStrategicPriorityService priorityService;

    public InfrastructureUnlockValueService()
    {
        this(new InfrastructureMilestoneCatalog(),
                new AccountStrategicPriorityService());
    }
    public InfraAssessment assess(String milestoneId,
            StrategyContext context)
    {
        var definition = catalog.get(milestoneId);
        if (definition == null)
            throw new IllegalArgumentException(
                    get(1422) + milestoneId);
        AccountPriorities priorities =
                priorityService.assess(context);
        var data = context == null ? null : context.data();
        return assess(definition, priorities, data);
    }

    public InfraAssessment assess(
            InfrastructureMilestone definition,
            AccountPriorities priorities,
            GameData data)
    {
        if (definition == null) throw new IllegalArgumentException("definition");
        if (priorities == null) throw new IllegalArgumentException("priorities");

        List<InfraContribution> contributions = new ArrayList<>();
        var overall = Priority.NONE;
        for (Map.Entry<InfraBenefit, Priority> entry
                : definition.getBenefits().entrySet())
        {
            Priority account = priorities.priorityOf(
                    entry.getKey().getDimension());
            InfraContribution contribution =
                    new InfraContribution(entry.getKey(),
                            account, entry.getValue());
            contributions.add(contribution);
            overall = Priority.higherOf(overall,
                    contribution.getEffectivePriority());
        }

        var account = data == null ? null : data.account();
        if (definition.membersOnly)
        {
            Membership membership = account == null
                    ? Membership.UNKNOWN : account.membership();
            if (membership == Membership.F2P)
                return result(definition,
                        InfrastructureMilestoneState.NOT_APPLICABLE,
                        Confidence.BLOCKED, overall,
                        contributions, get(1423));
            if (membership == Membership.UNKNOWN)
                return result(definition,
                        InfrastructureMilestoneState.CHECK_NEEDED,
                        Confidence.CHECK_NEEDED, overall,
                        contributions,
                        get(334));
        }

        var completion = completionState(definition, data);
        if (completion == Capability.VERIFIED)
            return result(definition, InfrastructureMilestoneState.COMPLETE,
                    Confidence.VERIFIED, overall, contributions,
                    get(335));

        var requirements = requirements(definition, data);
        if (requirements == RequirementState.BLOCKED)
            return result(definition,
                    InfrastructureMilestoneState.REQUIREMENTS_MISSING,
                    Confidence.BLOCKED, overall, contributions,
                    get(336));
        if (requirements == RequirementState.CHECK_NEEDED
                || completion == Capability.UNKNOWN)
            return result(definition,
                    InfrastructureMilestoneState.CHECK_NEEDED,
                    Confidence.CHECK_NEEDED, overall,
                    contributions,
                    get(337));

        return result(definition, InfrastructureMilestoneState.ACTIONABLE,
                Confidence.VERIFIED, overall, contributions,
                get(338));
    }

    private RequirementState requirements(
            InfrastructureMilestone definition,
            GameData data)
    {
        var account = data == null ? null : data.account();
        if (account == null) return RequirementState.CHECK_NEEDED;
        for (Map.Entry<net.runelite.api.Skill, Integer> skill
                : definition.requiredSkills.entrySet())
            if (account.level(skill.getKey()) < skill.getValue())
                return RequirementState.BLOCKED;

        for (Map.Entry<String, Boolean> quest
                : definition.getRequiredQuests().entrySet())
        {
            var quests = data.quests();
            if (quests == null) return RequirementState.CHECK_NEEDED;
            var status = quests.statusOf(quest.getKey());
            boolean satisfied = status == QuestStatus.COMPLETE
                    || quest.getValue()
                    && status == QuestStatus.IN_PROGRESS;
            if (!satisfied)
                return status == QuestStatus.UNKNOWN
                        ? RequirementState.CHECK_NEEDED
                        : RequirementState.BLOCKED;
        }

        if (definition.getPrerequisiteMilestoneId() != null)
        {
            InfrastructureMilestone prerequisite = catalog.get(
                    definition.getPrerequisiteMilestoneId());
            var state = completionState(prerequisite, data);
            if (state == Capability.UNKNOWN)
                return RequirementState.CHECK_NEEDED;
            if (state == Capability.BLOCKED)
                return RequirementState.BLOCKED;
        }
        return RequirementState.VERIFIED;
    }

    private Capability completionState(
            InfrastructureMilestone definition,
            GameData data)
    {
        if (definition == null || data == null) return Capability.UNKNOWN;
        switch (definition.getEvidenceKind())
        {
            case POH_ACCESS:
                return data.poh() == null ? Capability.UNKNOWN
                        : data.poh().getHouseAccess();
            case POH_FURNITURE:
                return data.poh() == null ? Capability.UNKNOWN
                        : data.poh().furnitureState(
                                definition.getEvidenceKey());
            case STORAGE_CAPABILITY:
                return data.storage() == null ? Capability.UNKNOWN
                        : data.storage().stateOf(
                                definition.getStorageCapability());
            case TRANSPORT_ROUTE:
                return data.transport() != null
                        && data.transport().hasVerifiedRoute(
                                definition.getEvidenceKey())
                        ? Capability.VERIFIED : Capability.UNKNOWN;
            default:
                return Capability.UNKNOWN;
        }
    }

    private static InfraAssessment result(
            InfrastructureMilestone definition,
            InfrastructureMilestoneState state,
            Confidence confidence,
            Priority value,
            List<InfraContribution> contributions,
            String reason)
    {
        return new InfraAssessment(definition, state, confidence,
                value, contributions, reason);
    }
}

/**
 * Resolves current tradeable-item prices through RuneLite's own ItemManager.
 *
 * <p>RuneLite refreshes its price cache on login and exposes both name search
 * and getItemPrice. Compass only uses exact-name matches so a request for
 * "Yew logs" never silently becomes a similarly named item.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class MarketPriceService
{
    private final ItemManager itemManager;

        public MarketPriceQuote quote(String exactItemName)
    {
        if (itemManager == null || exactItemName == null
                || exactItemName.trim().isEmpty())
        {
            return null;
        }

        try
        {
            var results = itemManager.search(exactItemName);
            if (results == null) return null;
            for (ItemPrice result : results)
            {
                var itemId = result.getId();
                if (itemId <= 0) continue;
                var composition = itemManager.getItemComposition(itemId);
                if (composition == null || composition.getName() == null
                        || !exactItemName.equalsIgnoreCase(composition.getName()))
                {
                    continue;
                }
                var price = itemManager.getItemPrice(itemId);
                if (price <= 0) return null;
                return new MarketPriceQuote(
                        itemId,
                        composition.getName(),
                        price);
            }
        }
        catch (RuntimeException ex)
        {
            return null;
        }
        return null;
    }

    public int priceByItemId(int itemId)
    {
        if (itemManager == null || itemId <= 0) return 0;
        try
        {
            return max(0, itemManager.getItemPrice(itemId));
        }
        catch (RuntimeException ex)
        {
            return 0;
        }
    }

}

/** Applies travel properties to a selected method and its rendered location. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
final class MethodRecommendationValueService
{
    private final MethodLocationCatalog locations;
    private final TravelRouteEvidenceCatalog routes;
    private final MethodResourceValueService resources;

    MethodRecommendationValueService()
    {
        this(new MethodLocationCatalog(), new TravelRouteEvidenceCatalog(),
                new MethodResourceValueService(null));
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        TrainingPlan plan = recommendation == null
                ? null : recommendation.plan();
        var method = plan == null ? null : plan.method();
        if (method == null || context == null) return recommendation;
        recommendation = resources.attach(recommendation, context);
        MethodLocationProfile profile = locations.forMethod(method.id);
        if (profile == null || context.data() == null
                || context.data().account() == null) return recommendation;
        MethodLocationOption location = profile.getLocations().stream()
                .filter(option -> !option.membersOnly
                        || context.data().account().membership() == Membership.P2P)
                .filter(option -> !option.wilderness
                        || context.allowsWilderness())
                .min(Comparator.comparingInt(option -> option.effectiveBurden(
                        routeVerified(option.getAdvantageousRouteId(), context))))
                .orElse(null);
        if (location == null) return recommendation;
        boolean routed = routeVerified(location.getAdvantageousRouteId(), context);
        int burden = location.effectiveBurden(routed);
        int adjustment = max(-6, min(4, 3 - burden));
        String evidence = routed
                ? "Verified route " + location.getAdvantageousRouteId()
                        + get(1297) + location.getName() + "."
                : get(895) + location.getName() + ".";

        StrategicValue value =
                recommendation.strategicValue.merge(
                        StrategicValue.builder()
                                .travelFit(adjustment / 6.0)
                                .evidence("travel:" + location.id)
                                .build());
        var result = recommendation.withStrategicValue(value);
        var guidance = result.guidance;
        if (guidance == null) return result;
        return result.withGuidance(new Guidance(
                guidance.getAction(), guidance.supplies,
                location.getName() + ".",
                append(guidance.note, evidence),
                guidance.bankingBehavior));
    }

    private boolean routeVerified(String routeId, StrategyContext context)
    {
        if (routeId == null || context == null || context.data() == null)
            return false;
        var data = context.data();
        if (data.transport() != null
                && data.transport().hasVerifiedRoute(routeId)) return true;
        if (data.account() == null || data.account().membership() != Membership.P2P)
            return false;
        RouteEvidence route = routes.get(routeId);
        if (route == null) return false;
        if (route.getRequiredCompletedQuest() != null
                && (data.quests() == null || data.quests().statusOf(
                        route.getRequiredCompletedQuest()) != QuestStatus.COMPLETE))
            return false;
        ItemIndex items = new ItemIndex(data, context.usesGroupStorage());
        for (String item : route.getRequiredItems())
            if (!items.has(item)) return false;
        return true;
    }

    private static String append(String first, String second)
    {
        if (first == null || first.trim().isEmpty()) return second;
        if (second == null || second.trim().isEmpty()) return first;
        return first + " " + second;
    }
}

/** Values deterministic method inputs without an intermediate resource DTO pipeline. */
@Singleton
final class MethodResourceValueService
{
    private static final ResourceSourceCatalog SOURCES = new ResourceSourceCatalog();
    private final RuneLiteSkillActionCatalog actions;
    private final MethodExecutionProfileCatalog profiles =
            new MethodExecutionProfileCatalog();
    private final SkillingXpModifierService modifiers =
            new SkillingXpModifierService();
    private final AdaptiveActionSelector selector = new AdaptiveActionSelector();
    private final MethodInputResolver inputs = new MethodInputResolver();
    private final GimGroupStrategyService groupStrategy =
            new GimGroupStrategyService();

    @Inject
    public MethodResourceValueService(RuneLiteSkillActionCatalog actions)
    {
        this.actions = actions == null ? new RuneLiteSkillActionCatalog() : actions;
    }
    public Recommendation attach(Recommendation recommendation,
            StrategyContext context)
    {
        TrainingPlan plan = recommendation == null ? null
                : recommendation.plan();
        var method = plan == null ? null : plan.method();
        if (method == null || method.getSkill() == null || context == null
                || context.data() == null || context.data().account() == null
                || recommendation.targetLevel <= 0) return recommendation;
        var profile = profiles.forMethod(method.id);
        if (profile == null) return recommendation;

        var account = context.data().account();
        var skill = method.getSkill();
        var currentXp = account.xp(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(account.level(skill));
        var targetXp = Experience.getXpForLevel(recommendation.targetLevel);
        double multiplier = profile.getXpMultiplier() * modifiers.modifier(
                context.data(), skill, context.usesGroupStorage()).multiplier;
        ActionDef action = selector.select(context.data(), profile,
                actions.actionsFor(skill), account.level(skill),
                account.membership(), currentXp, targetXp, multiplier,
                context.usesGroupStorage());
        if (action == null || action.xp <= 0) return recommendation;
        int count = (int) ceil(max(0, targetXp - currentXp)
                / (action.xp * multiplier));

        var score = 0;
        var known = false;
        var shared = StrategicValue.neutral();
        for (MethodInput input : inputs.resolve(profile, action, count))
        {
            var policy = policy(input.getName());
            if (policy == null) continue;
            known = true;
            score += resourceAdjustment(context, input.getName(),
                    input.quantity, policy[0], policy[1] == 1);
            var sharedIds = observedGroupItemIds(context, input.getName());
            if (!sharedIds.isEmpty())
                shared = shared.merge(groupStrategy.assess(context,
                        new GroupResourceNeed(input.getName(), sharedIds,
                                input.quantity, false)).strategicValue(
                        "group-resource:" + input.getName().toLowerCase(Locale.ROOT)));
        }
        if (!known) return recommendation;
        StrategicValue value = StrategicValue.builder()
                .resourceFit(max(-16, min(6, score)) / 12.0)
                .evidence(get(1881) + method.id).build()
                .merge(shared);
        return recommendation.withStrategicValue(
                recommendation.strategicValue.merge(value));
    }

    /** Shared scoring primitive retained for regression coverage of mode safety. */
    static int resourceAdjustment(StrategyContext context, String name,
            int required, int scarcity, boolean tradeable)
    {
        if (context == null || context.data() == null || name == null) return -4;
        ItemIndex items = new ItemIndex(context.data(),
                context.usesGroupStorage());
        var observed = items.quantity(name);
        var mode = context.accountMode();
        int burden = mode.usesGrandExchange() && tradeable ? 1
                : mode.isIronLike() ? 5 : 4;
        if (mode.isGroupIronman() && context.usesGroupStorage()
                && items.groupStorageObserved()) burden--;
        if (mode == AccountMode.ULTIMATE_IRONMAN) burden += 2;
        if (observed >= max(1, required))
            return max(-10, min(4, 4 - burden - scarcity));
        if (!items.resourceContainersObserved()) return -2;
        if (SOURCES.match(name).isEmpty()) return -4;
        return max(-12, min(3, 2 - burden - scarcity));
    }

    private static int[] policy(String name)
    {
        var value = Names.words(name);
        if (value.equals("spirit seed") || value.equals("crystal acorn"))
            return new int[]{4, 0};
        for (String term : new String[]{"rune", "essence", "bar", "plank",
                "nail", "log", "raw ", "grape", "jug of water", "feather",
                "arrowhead", "headless arrow", "dart tip", "unfinished bolt",
                "uncut ", "herb", "weed", "snape grass", "crushed nest",
                get(1882), "sapling", "seed"})
            if (value.contains(term)) return new int[]{1, 1};
        return null;
    }

    private static Set<Integer> observedGroupItemIds(StrategyContext context,
            String itemName)
    {
        if (context == null || !context.accountMode().isGroupIronman()
                || !context.usesGroupStorage() || context.data() == null
                || context.data().groupStorage() == null
                || !context.data().groupStorage().isObserved())
            return emptySet();
        var target = Names.words(itemName);
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemState item : context.data().groupStorage().getItems())
            if (item != null && item.quantity > 0 && item.itemId > 0
                    && target.equals(Names.words(item.getName())))
                ids.add(item.itemId);
        return ids;
    }

}

/** Applies live account and plan-relative inventory evidence before ranking. */
@Singleton
final class MethodStrategyService
{
    private final UimInventoryResolutionService inventoryResolution;

    @Inject
    public MethodStrategyService(
            UimInventoryResolutionService inventoryResolution)
    {
        this.inventoryResolution = inventoryResolution == null
                ? new UimInventoryResolutionService() : inventoryResolution;
    }

    public MethodStrategyService()
    {
        this(new UimInventoryResolutionService());
    }

    public MethodStrategyAssessment assess(GameData data,
            MethodStrategyProfile profile)
    {
        if (data == null || data.account() == null)
            return new MethodStrategyAssessment(profile != null, 0.0,
                    profile == null ? get(1304)
                            : profile.getPlayerReason());
        if (profile == null)
            return new MethodStrategyAssessment(false, 0.0,
                    get(388));
        AccountMode mode = AccountMode.fromTypeCode(
                data.account().modeCode());
        if (!profile.supports(mode))
            return new MethodStrategyAssessment(false, 0.0,
                    get(389));
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && profile.bankingBehavior
                        == BankingMode.CONVENTIONAL_BANK_LOOP)
            return new MethodStrategyAssessment(false, 0.0,
                    get(390));

        var footprint = profile.inventoryFootprint;
        var inventory = data.inventory();
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && footprint != null
                && footprint.minimumPracticalFreeSlots > 0
                && (inventory == null
                || !inventory.hasCompleteSlotObservation()))
            return new MethodStrategyAssessment(false, 0.0,
                    get(391));
        var occupied = UimSetupCostService.occupiedInventorySlots(inventory);
        var free = max(0, 28 - occupied);
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && inventory != null
                && inventory.hasCompleteSlotObservation())
        {
            UimInventoryResolution resolution = inventoryResolution.resolve(
                    data, footprint, false, false,
                    emptyList());
            if (resolution.getKind() != UimInventoryKind.USE_AS_IS)
                return new MethodStrategyAssessment(false, 0.0,
                        resolution.reason);
        }

        var score = profile.getAccountValueFit() * 8.0;
        if (mode == AccountMode.ULTIMATE_IRONMAN && inventory != null
                && inventory.hasCompleteSlotObservation())
        {
            var margin = free - footprint.minimumPracticalFreeSlots;
            if (margin <= 1) score -= 5.0;
            if (footprint.tearsDownCurrentSetup()) score -= 8.0;
            if (footprint.getFlow()
                    == InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS) score -= 3.0;
        }
        return new MethodStrategyAssessment(true, score,
                profile.getPlayerReason());
    }
}

/** Retains a valid goal step across minor score/inventory refreshes. */
@Singleton
final class PlanContinuityService
{
    public StrategicPlan reconcile(
            StrategicPlan previous,
            StrategicPlan rebuilt,
            StrategyContext context,
            List<Recommendation> currentRecommendations)
    {
        if (previous == null || !previous.matchesContext(context))
            return rebuilt;

        StrategicPlan advanced = previous.advanceCompleted(
                context == null ? null : context.data());
        var current = advanced.getCurrentStep();
        Set<String> recommendationIds = recommendationIds(
                currentRecommendations);

        // A current executable action becoming illegal, blocked or absent is a
        // material invalidation. Dependency-only future steps may remain while
        // the rebuilt plan supplies their newly executable recommendation.
        var currentRecommendation = current.getRecommendationId();
        if (currentRecommendation != null
                && !recommendationIds.contains(currentRecommendation))
            return rebuilt;

        if (rebuilt == null) return advanced;
        if (current.id.equals(rebuilt.getCurrentStep().id))
            return advanced;

        // Completing an intermediate target deliberately moves to the rebuilt
        // next action. Ordinary score movement does not replace unfinished work.
        if (advanced.getCurrentIndex() > previous.getCurrentIndex())
            return rebuilt;
        return advanced;
    }

    private static Set<String> recommendationIds(
            List<Recommendation> recommendations)
    {
        if (recommendations == null) recommendations = emptyList();
        Set<String> result = new HashSet<>();
        for (Recommendation recommendation : recommendations)
            if (recommendation != null && recommendation.id != null)
                result.add(recommendation.id);
        return result;
    }
}

/**
 * Event-driven local XP/session analytics.
 *
 * <p>RuneLite {@link StatChanged} values are absolute. A first observation or
 * a lower value therefore establishes a baseline and never becomes negative
 * progress. Rates use only nearby positive-XP intervals, become available
 * after multiple timed observations, and reset across idle gaps or method
 * changes. No game-tick polling is required.</p>
 */
@Singleton
class ProgressAnalyticsService
{
    static final int MAX_SKILL_XP = 200_000_000;
    static final long BUCKET_MILLIS = 5L * 60L * 1000L;
    static final int MAX_BUCKETS = 288;
    static final int MAX_RATE_INTERVALS = 120;
    static final long RATE_WINDOW_MILLIS = 30L * 60L * 1000L;
    static final long IDLE_GAP_MILLIS = 5L * 60L * 1000L;
    static final long MIN_RATE_DURATION_MILLIS = 30_000L;
    static final int MIN_RATE_INTERVALS = 2;
    static final int MAX_SESSION_MILESTONES = 100;

    private final EnumMap<Skill, MutableSkill> skills =
            new EnumMap<>(Skill.class);
    private final Deque<MutableBucket> buckets = new ArrayDeque<>();
    private final List<ProgressMilestone> milestones = new ArrayList<>();
    private final Set<String> milestoneIds = new HashSet<>();
    private long startedAtMillis;
    private long updatedAtMillis;
    private long activeDurationMillis;
    private long lastProgressAtMillis;
    private ProgressTarget target;

    public void beginSession(AccountSnapshot account)
    {
        beginSession(account, System.currentTimeMillis());
    }

    /** Begins or rebases the session from a complete, read-only account view. */
    public synchronized void beginSession(AccountSnapshot account, long nowMillis)
    {
        reset(nowMillis);
        if (account == null) return;
        for (Skill skill : Skill.values())
        {
            if (overall(skill)) continue;
            var xp = account.xp(skill);
            var level = account.level(skill);
            if (validXp(xp) && level > 0)
                skills.put(skill, new MutableSkill(xp, level));
        }
    }

    /** Clears all volatile state, used on login/profile changes. */
    public void reset()
    {
        reset(System.currentTimeMillis());
    }

    public synchronized void reset(long nowMillis)
    {
        skills.clear();
        buckets.clear();
        milestones.clear();
        milestoneIds.clear();
        target = null;
        startedAtMillis = max(0L, nowMillis);
        updatedAtMillis = startedAtMillis;
        activeDurationMillis = 0L;
        lastProgressAtMillis = 0L;
    }

    public boolean record(StatChanged event)
    {
        return record(event, System.currentTimeMillis());
    }

    public boolean record(StatChanged event, long nowMillis)
    {
        return event != null && record(event.getSkill(), event.getXp(),
                event.getLevel(), nowMillis);
    }

    /**
     * Records an absolute RuneLite XP value. Returns true only for positive
     * session progress; baselines, duplicates and invalid observations return
     * false.
     */
    public synchronized boolean record(
            Skill skill, int absoluteXp, int level, long nowMillis)
    {
        if (skill == null || overall(skill) || !validXp(absoluteXp)
                || level <= 0 || nowMillis < startedAtMillis
                || nowMillis < updatedAtMillis)
            return false;

        var state = skills.get(skill);
        if (state == null)
        {
            skills.put(skill, new MutableSkill(absoluteXp, level));
            updatedAtMillis = max(updatedAtMillis, nowMillis);
            return false;
        }
        if (nowMillis < state.lastObservationAtMillis)
            return false;

        var previousXp = state.currentXp;
        state.lastObservationAtMillis = nowMillis;
        state.currentLevel = max(state.currentLevel, level);
        updatedAtMillis = max(updatedAtMillis, nowMillis);

        if (absoluteXp < previousXp)
        {
            // Account resets, stale profile transitions and RuneLite rebases
            // must never retain another account's chart, milestones, target,
            // or active-time totals. The next complete account read will fill
            // the other skill baselines again.
            reset(nowMillis);
            skills.put(skill, new MutableSkill(absoluteXp, level));
            return false;
        }
        if (absoluteXp == previousXp) return false;

        var gained = absoluteXp - previousXp;
        state.currentXp = absoluteXp;
        addActiveTime(nowMillis);
        addRateInterval(state, gained, nowMillis);
        addBucket(skill, gained, nowMillis);
        return true;
    }

    /** Updates the active plan checkpoint and rebases incompatible method rates. */
    public synchronized boolean setTarget(ProgressTarget next)
    {
        var changed = !sameTarget(target, next);
        if (!changed) return false;
        if (target != null && next != null
                && (target.getSkill() != next.getSkill()
                || !Objects.equals(target.methodId, next.methodId)))
        {
            var state = skills.get(next.getSkill());
            if (state != null) state.rateIntervals.clear();
        }
        target = next;
        return true;
    }

    public synchronized void clearTarget()
    {
        target = null;
    }

    /** Ends the current active/rate segment without discarding session gains. */
    public synchronized void pause(long nowMillis)
    {
        updatedAtMillis = max(updatedAtMillis,
                max(startedAtMillis, nowMillis));
        lastProgressAtMillis = 0L;
        for (MutableSkill state : skills.values())
        {
            state.lastProgressAtMillis = 0L;
            state.rateIntervals.clear();
        }
    }

    /** Adds a typed non-XP milestone once per session. */
    public synchronized boolean recordMilestone(ProgressMilestone milestone)
    {
        if (milestone == null || !milestoneIds.add(milestone.id))
            return false;
        milestones.add(milestone);
        while (milestones.size() > MAX_SESSION_MILESTONES)
        {
            var removed = milestones.remove(0);
            milestoneIds.remove(removed.id);
        }
        updatedAtMillis = max(updatedAtMillis,
                milestone.getOccurredAtMillis());
        return true;
    }

    public synchronized ProgressSessionSnapshot snapshot(long nowMillis)
    {
        long effectiveNow = max(updatedAtMillis,
                max(startedAtMillis, nowMillis));
        EnumMap<Skill, SkillSessionProgress> result =
                new EnumMap<>(Skill.class);
        for (Map.Entry<Skill, MutableSkill> entry : skills.entrySet())
        {
            var value = entry.getValue();
            result.put(entry.getKey(), new SkillSessionProgress(
                    entry.getKey(), value.startingXp, value.currentXp,
                    value.startingLevel, value.currentLevel,
                    rateFor(value, effectiveNow)));
        }

        List<ProgressTimeBucket> bucketCopy = new ArrayList<>();
        for (MutableBucket value : buckets) bucketCopy.add(value.snapshot());
        return new ProgressSessionSnapshot(startedAtMillis, effectiveNow,
                activeDurationMillis, result, bucketCopy, milestones,
                targetProjection(result));
    }

    public ProgressSessionSnapshot snapshot()
    {
        return snapshot(System.currentTimeMillis());
    }

    private void addActiveTime(long nowMillis)
    {
        if (lastProgressAtMillis > 0L)
        {
            var gap = nowMillis - lastProgressAtMillis;
            if (gap >= 0L && gap <= IDLE_GAP_MILLIS)
                activeDurationMillis += gap;
        }
        lastProgressAtMillis = nowMillis;
    }

    private static void addRateInterval(
            MutableSkill state, int gained, long nowMillis)
    {
        if (state.lastProgressAtMillis > 0L)
        {
            var gap = nowMillis - state.lastProgressAtMillis;
            if (gap > IDLE_GAP_MILLIS)
                state.rateIntervals.clear();
            else if (gap > 0L)
                state.rateIntervals.addLast(new RateInterval(
                        gained, gap, nowMillis));
        }
        state.lastProgressAtMillis = nowMillis;
        trimRate(state.rateIntervals, nowMillis);
    }

    private static void trimRate(Deque<RateInterval> intervals, long nowMillis)
    {
        while (intervals.size() > MAX_RATE_INTERVALS
                || (!intervals.isEmpty()
                && nowMillis - intervals.peekFirst().endedAtMillis
                > RATE_WINDOW_MILLIS))
            intervals.removeFirst();
    }

    private void addBucket(Skill skill, int gained, long nowMillis)
    {
        var start = nowMillis - floorMod(nowMillis, BUCKET_MILLIS);
        var bucket = buckets.peekLast();
        if (bucket == null || bucket.startedAtMillis != start)
        {
            bucket = new MutableBucket(start);
            buckets.addLast(bucket);
        }
        bucket.add(skill, gained);
        while (buckets.size() > MAX_BUCKETS) buckets.removeFirst();
    }

    private static XpRateEstimate rateFor(MutableSkill state, long nowMillis)
    {
        trimRate(state.rateIntervals, nowMillis);
        var xp = 0L;
        var millis = 0L;
        for (RateInterval interval : state.rateIntervals)
        {
            xp += interval.xp;
            millis += interval.activeMillis;
        }
        if (state.rateIntervals.size() < MIN_RATE_INTERVALS
                || millis < MIN_RATE_DURATION_MILLIS || xp <= 0L)
            return XpRateEstimate.calculating(state.rateIntervals.size());
        var hourly = round(xp * 3_600_000.0 / millis);
        return hourly <= 0L
                ? XpRateEstimate.calculating(state.rateIntervals.size())
                : XpRateEstimate.ready(hourly, state.rateIntervals.size());
    }

    private TargetProjection targetProjection(
            Map<Skill, SkillSessionProgress> progress)
    {
        if (target == null) return TargetProjection.noTarget();
        var skill = progress.get(target.getSkill());
        var currentXp = skill == null ? 0 : skill.getCurrentXp();
        var remaining = max(0, target.getTargetXp() - currentXp);
        if (remaining == 0) return TargetProjection.complete(target);
        if (skill == null || !skill.getRate().isReady())
            return TargetProjection.calculating(target, remaining);
        var rate = skill.getRate().getXpPerHour();
        if (rate <= 0L)
            return TargetProjection.calculating(target, remaining);
        var eta = remaining * 3_600_000.0 / rate;
        long etaMillis = eta >= Long.MAX_VALUE
                ? Long.MAX_VALUE : max(1L, round(eta));
        return TargetProjection.ready(target, remaining, etaMillis);
    }

    private static boolean sameTarget(ProgressTarget first, ProgressTarget second)
    {
        if (first == null || second == null) return first == second;
        return Objects.equals(first.activityId, second.activityId)
                && Objects.equals(first.methodId, second.methodId)
                && first.getSkill() == second.getSkill()
                && first.targetLevel == second.targetLevel;
    }

    private static boolean validXp(int value)
    {
        return value >= 0 && value <= MAX_SKILL_XP;
    }

    private static boolean overall(Skill skill)
    {
        return skill != null && "OVERALL".equals(skill.name());
    }

    private static final class MutableSkill
    {
        private int startingXp;
        private int currentXp;
        private int startingLevel;
        private int currentLevel;
        private long lastObservationAtMillis;
        private long lastProgressAtMillis;
        private final Deque<RateInterval> rateIntervals = new ArrayDeque<>();

        private MutableSkill(int xp, int level)
        {
            startingXp = xp;
            currentXp = xp;
            startingLevel = level;
            currentLevel = level;
        }

    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)

    private static final class RateInterval
    {
        private final int xp;
        private final long activeMillis;
        private final long endedAtMillis;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)

    private static final class MutableBucket
    {
        private final long startedAtMillis;
        private final EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        private void add(Skill skill, int value)
        {
            xp.merge(skill, value, (first, second) -> {
                var total = (long) first + second;
                return (int) min(Integer.MAX_VALUE, total);
            });
        }

        private ProgressTimeBucket snapshot()
        {
            return new ProgressTimeBucket(startedAtMillis, xp);
        }
    }
}

/** Decides whether a method is still serving a larger reward objective. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class ProgressionObjectiveService
{
    private final ProgressionObjectiveCatalog catalog;

    public ProgressionObjectiveDefinition activeObjective(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.method() == null)
        {
            return null;
        }

        ProgressionObjectiveDefinition objective =
                catalog.forMethod(plan.method().id);
        if (objective == null)
        {
            return null;
        }

        if (collectionLog != null
                && collectionLog.isObjectiveComplete(objective.id))
        {
            return null;
        }

        return objective;
    }

    public boolean shouldProtect(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.method() == null)
        {
            return false;
        }

        // Explicit catalog objectives take precedence. The method flag remains
        // a conservative fallback while collection-log readers are incomplete.
        ProgressionObjectiveDefinition objective =
                catalog.forMethod(plan.method().id);
        if (objective != null)
        {
            return collectionLog == null
                    || !collectionLog.isObjectiveComplete(objective.id);
        }

        return plan.method().progressionProtected;
    }
}

/**
 * Orders quest work from typed dependency edges and compresses shared paths.
 * Optional-quest preference is deliberately absent: every returned quest is a
 * proven dependency of one of the supplied goals.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
final class QuestPathPlanningService
{
    private final GoalGraph goalGraph;
    private final QuestKnowledgeCatalog quests;
    private final QuestRequirementResolver resolver;

    public QuestPathPlan plan(StrategyContext context)
    {
        return plan(context, singleton(
                context == null ? GoalType.AUTOMATIC
                        : context.goal()));
    }

    public QuestPathPlan plan(StrategyContext context,
            Collection<GoalType> selectedGoals)
    {
        if (context == null || context.data() == null
                || context.data().account() == null
                || context.data().quests() == null
                || selectedGoals == null)
            return new QuestPathPlan(emptyList());

        Map<String, MutableNode> nodes = new LinkedHashMap<>();
        for (GoalType goal : selectedGoals)
        {
            if (goal == null || goal == GoalType.AUTOMATIC
                    || goal == GoalType.CUSTOM) continue;
            if (goal == GoalType.QUEST_CAPE)
                addQuestCapeRoots(goal, context, nodes);
            else
                for (String root : goalGraph.questRootsFor(goal))
                    traverse(goal, root, context, nodes,
                            new ArrayList<>(), new HashSet<>());
        }

        Map<Skill, Integer> unmetSkillTargets = unmetSkillTargets(
                nodes, context);
        List<QuestPathStep> result = new ArrayList<>();
        for (MutableNode node : nodes.values())
        {
            var status = statusOf(context, node.questName);
            if (status == QuestStatus.COMPLETE
                    || status == QuestStatus.UNKNOWN) continue;
            var definition = quests.definitionFor(node.questName);
            var account = context.data().account();
            if (definition == null
                    || !QuestMembershipPolicy.isAvailable(
                            definition.getName(),
                            account.membership())
                    || !RestrictedQuestPolicy.isSafe(account,
                            definition.getName()))
                continue;
            QuestResolution resolution = definition == null ? null
                    : resolver.resolve(definition, context);
            Confidence readiness = resolution == null
                    ? Confidence.CHECK_NEEDED
                    : resolution.confidence;
            var prerequisitesComplete = definition != null;
            if (definition != null)
                for (String prerequisite : definition.prerequisites)
                    if (statusOf(context, prerequisite)
                            != QuestStatus.COMPLETE)
                        prerequisitesComplete = false;
            boolean eligible = prerequisitesComplete
                    && readiness != Confidence.BLOCKED;
            var rewards = guaranteedRewards(definition);
            result.add(new QuestPathStep(node.questName, status,
                    node.paths, node.unfinishedDependents,
                    readiness, eligible, node.depth, rewards,
                    rewardValue(rewards, unmetSkillTargets,
                            context.data().account())));
        }
        result.sort(Comparator
                .comparing(QuestPathStep::isEligibleNow).reversed()
                .thenComparing(step -> step.status
                        == QuestStatus.IN_PROGRESS, Comparator.reverseOrder())
                .thenComparing(step -> step.getReadiness()
                        == Confidence.VERIFIED,
                        Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(
                        QuestPathStep::getGoalCount).reversed())
                .thenComparing(Comparator.comparingInt(
                        (QuestPathStep step) -> step
                                .getUnfinishedDependents().size()).reversed())
                .thenComparing(Comparator.comparingDouble(
                        QuestPathStep::getGoalPathRewardValue).reversed())
                .thenComparing(Comparator.comparingInt(
                        QuestPathStep::getDepth).reversed())
                .thenComparing(QuestPathStep::getQuestName));
        return new QuestPathPlan(result);
    }

    private void addQuestCapeRoots(GoalType goal, StrategyContext context,
            Map<String, MutableNode> nodes)
    {
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
            if (entry.getValue() == QuestStatus.NOT_STARTED
                    || entry.getValue() == QuestStatus.IN_PROGRESS)
                traverse(goal, entry.getKey(), context, nodes,
                        new ArrayList<>(), new HashSet<>());
    }

    private void traverse(GoalType goal, String questName,
            StrategyContext context, Map<String, MutableNode> nodes,
            List<String> ancestors, Set<String> active)
    {
        var key = Names.words(questName);
        if (!active.add(key)) return;
        var definition = quests.definitionFor(questName);
        if (definition == null)
        {
            active.remove(key);
            return;
        }
        var account = context.data().account();
        if (!QuestMembershipPolicy.isAvailable(definition.getName(),
                account.membership())
                || !RestrictedQuestPolicy.isSafe(account,
                        definition.getName()))
        {
            active.remove(key);
            return;
        }
        List<String> path = new ArrayList<>();
        path.add(goal.toString());
        path.addAll(ancestors);
        path.add(definition.getName());
        MutableNode node = nodes.computeIfAbsent(key,
                ignored -> new MutableNode(definition.getName()));
        node.paths.put(goal, shortest(node.paths.get(goal), path));
        node.depth = max(node.depth, ancestors.size());

        List<String> childAncestors = new ArrayList<>(ancestors);
        childAncestors.add(definition.getName());
        for (String prerequisite : definition.prerequisites)
        {
            var status = statusOf(context, prerequisite);
            if (status != QuestStatus.COMPLETE
                    && status != QuestStatus.UNKNOWN)
            {
                MutableNode child = nodes.computeIfAbsent(
                        Names.words(prerequisite),
                        ignored -> new MutableNode(prerequisite));
                if (!child.unfinishedDependents.contains(definition.getName()))
                    child.unfinishedDependents.add(definition.getName());
            }
            traverse(goal, prerequisite, context, nodes,
                    childAncestors, active);
        }
        active.remove(key);
    }

    private static List<String> shortest(
            List<String> current, List<String> candidate)
    {
        if (current == null || candidate.size() < current.size())
            return candidate;
        return current;
    }

    private Map<Skill, Integer> unmetSkillTargets(
            Map<String, MutableNode> nodes, StrategyContext context)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        var account = context.data().account();
        for (MutableNode node : nodes.values())
        {
            var definition = quests.definitionFor(node.questName);
            if (definition == null) continue;
            for (Map.Entry<Skill, Integer> requirement
                    : definition.skillRequirements.entrySet())
                if (requirement.getValue()
                        > account.level(requirement.getKey()))
                    result.merge(requirement.getKey(), requirement.getValue(),
                            Math::max);
        }
        return result;
    }

    private static Map<Skill, Integer> guaranteedRewards(
            QuestDefinition definition)
    {
        if (definition == null) return emptyMap();
        for (String uncertainty : definition.getFieldUncertainties())
        {
            var value = Names.words(uncertainty);
            if (value.contains("reward") || value.contains("irreversible xp"))
                return emptyMap();
        }
        return definition.getRewardXp();
    }

    private static double rewardValue(Map<Skill, Integer> rewards,
            Map<Skill, Integer> targets, AccountSnapshot account)
    {
        var value = 0.0;
        for (Map.Entry<Skill, Integer> reward : rewards.entrySet())
        {
            var target = targets.get(reward.getKey());
            if (target == null || reward.getValue() <= 0) continue;
            var currentLevel = account.level(reward.getKey());
            if (currentLevel >= target) continue;
            int currentXp = max(account.xp(reward.getKey()),
                    Experience.getXpForLevel(max(1, currentLevel)));
            var targetXp = Experience.getXpForLevel(target);
            var gap = max(1, targetXp - currentXp);
            value += min(1.0, reward.getValue() / (double) gap);
        }
        return min(1.0, value);
    }

    private static QuestStatus statusOf(
            StrategyContext context, String questName)
    {
        return QuestGraphs.status(context, questName);
    }


    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)


    private static final class MutableNode
    {
        private final String questName;
        private final Map<GoalType, List<String>> paths =
                new EnumMap<>(GoalType.class);
        private final List<String> unfinishedDependents = new ArrayList<>();
        private int depth;
    }
}

/** Adds dependency fan-out value from the typed selected-goal quest plan. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
final class QuestRecommendationValueService
{
    private final QuestPathPlanningService planner;

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || recommendation.id == null
                || !recommendation.id.startsWith("quest:")
                || context == null) return recommendation;
        var plan = planner.plan(context);
        var quest = recommendation.id.substring("quest:".length());
        var step = plan.stepForQuest(quest.replace('-', ' '));
        return step == null ? recommendation
                : recommendation.withStrategicValue(
                        recommendation.strategicValue.merge(
                                step.strategicValue()));
    }
}

/** Global account-value ranking after all legal candidates enter one pool. */
@Singleton
class RecommendationIntelligenceService
{
    private final UimSetupCostService uimSetupCostService;
    private final GoalDependencyProvenanceService goalProvenanceService;

    @Inject
    public RecommendationIntelligenceService(UimSetupCostService uimSetupCostService,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this.uimSetupCostService = uimSetupCostService == null
                ? new UimSetupCostService() : uimSetupCostService;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
    }

    public RecommendationIntelligenceService(UimSetupCostService uimSetupCostService)
    {
        this(uimSetupCostService, new GoalDependencyProvenanceService());
    }

    public RecommendationIntelligenceService()
    {
        this(new UimSetupCostService());
    }

    public double rankScore(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null) return Double.NEGATIVE_INFINITY;
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return recommendation.score;
        }

        var score = recommendation.score;
        score += recommendation.strategicValue.scoreDelta();
        var guidance = recommendation.guidance;

        score += readinessValue(recommendation, guidance);
        score += goalValue(recommendation, context.goal());
        score += questRewardValue(recommendation, context);
        score += sessionValue(recommendation, context.intent());
        score += strategyModeValue(recommendation, context);
        score += uimSetupCostService.score(recommendation, context);

        // Preference weight, snooze timing, dislike weight, and fatigue are
        // already priced into candidate scores by their producer. Applying them
        // again here made feedback stronger than configured and could rotate an
        // otherwise-correct DO NEXT choice too aggressively.
        return score;
    }

    private static double readinessValue(Recommendation recommendation, Guidance guidance)
    {
        if (recommendation.confidence == Confidence.BLOCKED)
            return -10_000.0;
        if (recommendation.confidence == Confidence.CHECK_NEEDED)
            return -9.0;
        // Presentability/actionability is a gate, not strategic value. A more
        // verbose or easily verified candidate must not beat a better action
        // merely because it supplied more text fields.
        return 0.0;
    }

    static double goalValue(Recommendation recommendation, GoalType selectedGoal)
    {
        if (recommendation == null || selectedGoal == null) return 0.0;
        var provenance = recommendation.goalProvenance;
        if (provenance == null
                || !provenance.proves(selectedGoal, recommendation.id))
            return 0.0;
        double direct;
        switch (selectedGoal)
        {
            case MAX: direct = 8.0; break;
            case QUEST_CAPE: direct = 28.0; break;
            case BARROWS_GLOVES: direct = 38.0; break;
            case FIRE_CAPE: direct = 45.0; break;
            case PRIFDDINAS: direct = 42.0; break;
            case BOWFA: direct = 38.0; break;
            case INFERNAL_CAPE: direct = 45.0; break;
            case DIARY_CAPE: direct = 30.0; break;
            case ELITE_COMBAT_ACHIEVEMENTS: direct = 32.0; break;
            case RAID_READY: direct = 24.0; break;
            case TOTAL_2000: direct = 19.0; break;
            case SLAYER_85: direct = 45.0; break;
            case BASE_70S: direct = 25.0; break;
            case GEAR_TARGET: direct = 35.0; break;
            default: direct = 0.0; break;
        }
        return provenance.getRelationship() == GoalRelation.DIRECT
                ? direct : min(26.0, direct * 0.7);
    }

    private double questRewardValue(
            Recommendation recommendation, StrategyContext context)
    {
        var plan = recommendation.plan();
        if (plan == null || plan.method() == null
                || plan.method().getSkill() == null
                || recommendation.targetLevel <= 0) return 0.0;
        var skill = plan.method().getSkill();
        GoalQuestRewardForecast forecast = goalProvenanceService
                .guaranteedRewardsBeforeManualTraining(context, skill);
        if (!forecast.hasGuaranteedExperience()) return 0.0;
        var currentXp = context.data().account().xp(skill);
        if (currentXp <= 0)
            currentXp = Experience.getXpForLevel(
                    context.data().account().level(skill));
        int remaining = max(0,
                Experience.getXpForLevel(recommendation.targetLevel)
                        - currentXp);
        if (remaining <= 0) return 0.0;
        double coverage = min(1.0,
                forecast.getExperience() / (double) remaining);
        if (coverage >= 1.0) return -36.0;
        if (coverage >= 0.75) return -28.0;
        if (coverage >= 0.5) return -18.0;
        return -10.0 * coverage;
    }

    private static double sessionValue(Recommendation recommendation, SessionIntent intent)
    {
        var plan = recommendation.plan();
        if (plan == null || plan.method() == null || intent == null) return 0.0;
        var method = plan.method();
        var setup = max(0, method.getSetupMinutes());
        var minimum = max(0, method.getMinimumSessionMinutes());
        switch (intent)
        {
            case QUICK_20_MIN:
                if (setup <= 3 && minimum <= 20) return 8.0;
                if (setup >= 10 || minimum > 30) return -12.0;
                return 1.0;
            case ONE_HOUR:
                if (setup <= 8 && minimum <= 60) return 4.0;
                return minimum > 90 ? -5.0 : 0.0;
            case LONG_SESSION:
                return minimum >= 30 ? 4.0 : 1.0;
            case AFK:
                if (method.attentionLevel == AttentionLevel.AFK) return 12.0;
                if (method.attentionLevel == AttentionLevel.LOW) return 7.0;
                if (method.attentionLevel == AttentionLevel.ACTIVE) return -9.0;
                return 0.0;
            case PICK_FOR_ME:
            default:
                return 0.0;
        }
    }

    /** Strategy mode prices typed properties; IDs and player-facing prose do not. */
    private static double strategyModeValue(Recommendation recommendation,
            StrategyContext context)
    {
        var mode = context.mode();
        var plan = recommendation.plan();
        var method = plan == null ? null : plan.method();
        var value = recommendation.strategicValue;
        switch (mode)
        {
            case EFFICIENT:
                return value.getUnlockValue() * 4.0
                        + value.getSharedDependencyValue() * 5.0
                        + max(0.0, value.getTravelFit()) * 2.0
                        + (method != null
                        && method.attentionLevel == AttentionLevel.ACTIVE
                        ? 3.0 : 0.0);
            case RELAXED:
                double relaxed = value.setupReuse * 4.0
                        - value.getRiskBurden() * 5.0;
                if (method != null)
                {
                    if (method.attentionLevel == AttentionLevel.AFK) relaxed += 7.0;
                    if (method.attentionLevel == AttentionLevel.LOW) relaxed += 4.0;
                    if (method.attentionLevel == AttentionLevel.ACTIVE) relaxed -= 5.0;
                }
                return relaxed;
            case BALANCED:
            default:
                return value.getSharedDependencyValue() * 3.0
                        + value.setupReuse * 1.5;
        }
    }
}

/** Compatibility facade; item evidence is now resolved by the shared index. */
@Singleton
class ResourceReadinessService
{
    public EvidenceCheck evaluate(GameData data, ResourceRequirement need)
    {
        return evaluate(data, need, false);
    }

    public EvidenceCheck evaluate(GameData data, ResourceRequirement need,
            boolean useGroupStorage)
    {
        return new ItemIndex(data, useGroupStorage).check(need);
    }

    public EvidenceCheck evaluate(GameData data, ResourceRequirement need,
            Capability alternate, String evidence)
    {
        return alternate == Capability.VERIFIED
                ? new EvidenceCheck(need.id, need.getLabel(),
                        RequirementState.VERIFIED, evidence == null
                        ? get(1569) : evidence)
                : evaluate(data, need);
    }

    public int observedQuantity(GameData data, int... itemIds)
    {
        return new ItemIndex(data, false).quantity(itemIds);
    }

    public int observedQuantity(GameData data, boolean group, int... itemIds)
    {
        return new ItemIndex(data, group).quantity(itemIds);
    }
}

/** Chooses actual unlock/requirement levels before generic level checkpoints. */
@Singleton
final class SkillBreakpointService
{
    private final GoalDependencyProvenanceService goals;
    private final AbilityUnlockCatalog abilities;
    private final RuneLiteSkillActionCatalog actions;
    private final InfrastructureMilestoneCatalog infrastructure;
    private final InfrastructureUnlockValueService infrastructureValue;

    @Inject
    public SkillBreakpointService(
            GoalDependencyProvenanceService goals,
            AbilityUnlockCatalog abilities,
            RuneLiteSkillActionCatalog actions,
            InfrastructureMilestoneCatalog infrastructure,
            InfrastructureUnlockValueService infrastructureValue)
    {
        this.goals = goals == null
                ? new GoalDependencyProvenanceService() : goals;
        this.abilities = abilities == null
                ? new AbilityUnlockCatalog() : abilities;
        this.actions = actions == null
                ? new RuneLiteSkillActionCatalog() : actions;
        this.infrastructure = infrastructure == null
                ? new InfrastructureMilestoneCatalog() : infrastructure;
        this.infrastructureValue = infrastructureValue == null
                ? new InfrastructureUnlockValueService() : infrastructureValue;
    }

    public SkillBreakpointService()
    {
        this(new GoalDependencyProvenanceService(),
                new AbilityUnlockCatalog(), new RuneLiteSkillActionCatalog(),
                new InfrastructureMilestoneCatalog(),
                new InfrastructureUnlockValueService());
    }

    public SkillBreakpoint next(
            Skill skill, int currentLevel, StrategyContext context)
    {
        int goalLevel = goals.nextRequiredSkillLevel(
                context == null ? GoalType.AUTOMATIC : context.goal(),
                skill, context);
        if (goalLevel > currentLevel)
            return new SkillBreakpoint(skill, goalLevel,
                    get(1301),
                    SkillBreakpoint.Kind.GOAL_REQUIREMENT,
                    "goal:" + context.goal().name().toLowerCase());

        InfrastructureMilestone infrastructureTarget = context == null
                ? null : infrastructure.all().stream()
                .filter(value -> value.requiredSkills
                        .getOrDefault(skill, 0) > currentLevel)
                .filter(value -> isNextMissingSkill(value, skill,
                        context.data().account()))
                .filter(value -> {
                    InfrastructureMilestoneState state = infrastructureValue
                            .assess(value.id, context).getState();
                    return state != InfrastructureMilestoneState.COMPLETE
                            && state != InfrastructureMilestoneState.NOT_APPLICABLE;
                })
                .min(Comparator.comparingInt(
                        value -> value.requiredSkills.get(skill)))
                .orElse(null);
        if (infrastructureTarget != null)
            return new SkillBreakpoint(skill,
                    infrastructureTarget.requiredSkills.get(skill),
                    "Unlock " + infrastructureTarget.getName(),
                    SkillBreakpoint.Kind.INFRASTRUCTURE_UNLOCK,
                    "infrastructure:" + infrastructureTarget.id);

        AbilityUnlockDefinition ability = abilities.all().stream()
                .filter(value -> value.getSkill() == skill
                        && value.getLevel() > currentLevel)
                .min(Comparator.comparingInt(
                        AbilityUnlockDefinition::getLevel))
                .orElse(null);
        if (ability != null)
            return new SkillBreakpoint(skill, ability.getLevel(),
                    "Unlock " + ability.getName(),
                    SkillBreakpoint.Kind.ABILITY_UNLOCK,
                    "ability:" + ability.id);

        Membership membership = context == null
                || context.data() == null
                || context.data().account() == null
                ? Membership.UNKNOWN
                : context.data().account().membership();
        ActionDef action = actions.actionsFor(skill).stream()
                .filter(value -> value.getLevel() > currentLevel)
                .filter(value -> isAvailable(value.membership, membership))
                .min(Comparator.comparingInt(
                        ActionDef::getLevel))
                .orElse(null);
        if (action != null)
            return new SkillBreakpoint(skill, action.getLevel(),
                    "Unlock " + action.getName(),
                    SkillBreakpoint.Kind.TRAINING_ACTION_UNLOCK,
                    action.id);

        if (context != null && context.goal() == GoalType.MAX)
            return new SkillBreakpoint(skill, 99, get(1302),
                    SkillBreakpoint.Kind.MAX_TARGET, "goal:max");
        return new SkillBreakpoint(skill, min(99, currentLevel + 1),
                get(1303),
                SkillBreakpoint.Kind.NEXT_LEVEL_FALLBACK, "level:next");
    }

    private static boolean isAvailable(
            Membership action, Membership account)
    {
        if (account == Membership.UNKNOWN) return false;
        if (account == Membership.F2P)
            return action == Membership.F2P;
        return action == Membership.F2P || action == Membership.P2P;
    }

    private static boolean isNextMissingSkill(
            InfrastructureMilestone definition, Skill requested,
            AccountSnapshot account)
    {
        Skill next = null;
        var smallestGap = Integer.MAX_VALUE;
        for (java.util.Map.Entry<Skill, Integer> requirement
                : definition.requiredSkills.entrySet())
        {
            int gap = requirement.getValue()
                    - account.level(requirement.getKey());
            if (gap > 0 && gap < smallestGap)
            {
                smallestGap = gap;
                next = requirement.getKey();
            }
        }
        return requested == next;
    }
}

/**
 * Detects full XP-boosting skilling outfits from observed item state.
 *
 * <p>Only full sets are modeled here. Partial sets do give smaller bonuses, but
 * exact piece-by-piece modeling can be layered in later. Full sets are useful
 * now because the common outfits share a verified 2.5% total bonus and the
 * player can simply equip the observed set before following the recommendation.</p>
 */
@Singleton
class SkillingXpModifierService
{
    private static final double FULL_SET_MULTIPLIER = 1.025;

    public SkillingXpModifier modifier(
            GameData data,
            Skill skill,
            boolean useGroupStorage)
    {
        if (data == null || skill == null) return SkillingXpModifier.none();
        var items = new ItemIndex(data, useGroupStorage);

        switch (skill)
        {
            case FISHING:
                if (hasAngler(items))
                    return full(get(1208));
                break;
            case MINING:
                if (hasProspector(items))
                    return full(get(1209));
                break;
            case WOODCUTTING:
                if (hasLumberjack(items))
                    return full(get(1210));
                break;
            case FARMING:
                if (hasFarmer(items))
                    return full(get(1211));
                break;
            case FIREMAKING:
                if (hasPyromancer(items))
                    return full(get(1212));
                break;
            case CONSTRUCTION:
                if (hasCarpenter(items))
                    return full(get(1213));
                break;
            default:
                break;
        }
        return SkillingXpModifier.none();
    }

    private static SkillingXpModifier full(String label)
    {
        return new SkillingXpModifier(FULL_SET_MULTIPLIER, label);
    }

    private static boolean hasAngler(ItemIndex items)
    {
        return items.has("Angler hat", get(1214))
                && items.has("Angler top", get(1856))
                && items.has("Angler waders", get(1215))
                && items.has("Angler boots", get(1216));
    }

    private static boolean hasProspector(ItemIndex items)
    {
        return items.has(get(1979), get(1217))
                && items.has(get(1980), get(1218), get(1981))
                && items.has("Prospector legs", get(1219))
                && items.has(get(1982), get(1220));
    }

    private static boolean hasLumberjack(ItemIndex items)
    {
        return items.has("Lumberjack hat", "Forestry hat")
                && items.has("Lumberjack top", "Forestry top")
                && items.has("Lumberjack legs", "Forestry legs")
                && items.has(get(1983), "Forestry boots");
    }

    private static boolean hasFarmer(ItemIndex items)
    {
        return items.has(get(1984))
                && items.has("Farmer's jacket", "Farmer's shirt")
                && items.has(get(1221))
                && items.has("Farmer's boots");
    }

    private static boolean hasPyromancer(ItemIndex items)
    {
        return items.has("Pyromancer hood")
                && items.has("Pyromancer garb")
                && items.has("Pyromancer robe")
                && items.has(get(1985));
    }

    private static boolean hasCarpenter(ItemIndex items)
    {
        return items.has(get(1222))
                && items.has(get(1986))
                && items.has(get(1223))
                && items.has(get(1987));
    }
}

/** Builds a concise ordered plan from the same proven path used by DO NEXT. */
@Singleton
final class StrategicPlanService
{
    private final QuestKnowledgeCatalog quests;

    @Inject
    public StrategicPlanService(QuestKnowledgeCatalog quests)
    {
        this.quests = quests == null ? new QuestKnowledgeCatalog() : quests;
    }

    public StrategicPlanService()
    {
        this(new QuestKnowledgeCatalog());
    }

    public StrategicPlan build(
            List<Recommendation> recommendations,
            StrategyContext context,
            long nowMillis)
    {
        if (recommendations == null || context == null
                || context.data() == null
                || context.data().account() == null
                || context.goal() == null
                || context.goal() == GoalType.AUTOMATIC
                || context.goal() == GoalType.CUSTOM)
            return null;

        Recommendation anchor = null;
        GoalProvenance provenance = null;
        for (Recommendation candidate : recommendations)
        {
            GoalProvenance value = candidate == null
                    ? null : candidate.goalProvenance;
            if (value != null && value.proves(
                    context.goal(), candidate.id))
            {
                anchor = candidate;
                provenance = value;
                break;
            }
        }
        if (anchor == null) return null;

        List<StrategicPlanStep> steps = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        var current = currentStep(anchor, provenance);
        steps.add(current);
        ids.add(current.id);

        var path = provenance.getPath();
        for (int i = path.size() - 2; i >= 0; i--)
        {
            var label = path.get(i);
            StrategicPlanStep step = dependencyStep(label,
                    context.goal());
            if (ids.add(step.id)) steps.add(step);
        }
        return new StrategicPlan(context.goal(),
                context.data().account(), steps, 0, nowMillis);
    }

    private StrategicPlanStep currentStep(
            Recommendation recommendation,
            GoalProvenance provenance)
    {
        var training = recommendation.plan();
        if (training != null && training.method() != null
                && training.method().getSkill() != null
                && recommendation.targetLevel > 0)
        {
            var skill = training.method().getSkill();
            var currentTarget = recommendation.getCurrentExecutionTargetLevel();
            return new StrategicPlanStep(
                    "skill:" + skill.name().toLowerCase(Locale.ROOT) + ":"
                            + currentTarget,
                    GoalNodeKind.SKILL_LEVEL,
                    display(skill) + " " + recommendation.currentLevel
                            + " → " + currentTarget,
                    provenance.compactPath(),
                    CompletionRule.skillLevel(
                            skill, currentTarget),
                    recommendation.id);
        }

        var quest = questName(recommendation);
        if (quest != null)
            return new StrategicPlanStep(
                    "quest:" + slug(quest), GoalNodeKind.QUEST,
                    quest, provenance.compactPath(),
                    CompletionRule.questComplete(quest),
                    recommendation.id);

        return new StrategicPlanStep(
                "action:" + slug(recommendation.id),
                GoalNodeKind.ACTIVITY,
                recommendation.title, provenance.compactPath(),
                CompletionRule.none(), recommendation.id);
    }

    private StrategicPlanStep dependencyStep(String label, GoalType goal)
    {
        var definition = quests.definitionFor(label);
        if (definition != null)
            return new StrategicPlanStep(
                    "quest:" + slug(definition.getName()), GoalNodeKind.QUEST,
                    definition.getName(), get(1298) + goal,
                    CompletionRule.questComplete(definition.getName()),
                    "quest:" + slug(definition.getName()));

        var target = label.equalsIgnoreCase(goal.toString());
        return new StrategicPlanStep(
                (target ? "goal:" : "dependency:") + slug(label),
                target ? GoalNodeKind.META : GoalNodeKind.ACCESS,
                label,
                target ? "Selected target" : get(1299) + goal,
                CompletionRule.none(), null);
    }

    private String questName(Recommendation recommendation)
    {
        if (recommendation == null || recommendation.id == null
                || !recommendation.id.startsWith("quest:")) return null;
        String title = recommendation.title == null ? ""
                : recommendation.title;
        title = title.replaceFirst(get(1300), "");
        var separator = title.indexOf(": ");
        if (separator > 0) title = title.substring(0, separator);
        var definition = quests.definitionFor(title);
        return definition == null ? null : definition.getName();
    }

    private static String display(Skill skill)
    {
        String value = skill.name().toLowerCase(Locale.ROOT)
                .replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

/**
 * Conservative capability gate for Ultimate Ironman storage suggestions.
 *
 * <p>A storage system being possible in OSRS is never enough. Compass needs
 * direct evidence that this character has the capability and, for an item-
 * specific route, evidence that the item is compatible and that required space
 * or preconditions are satisfied.</p>
 */
@Singleton
class UimCapabilityService
{
    public UimStorageDecision evaluateStorage(
            GameData data,
            StorageKind capability,
            Capability itemCompatibility,
            Capability capacityOrPreconditions)
    {
        if (data == null || data.account() == null)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(1421));
        }

        AccountMode mode = AccountMode.fromTypeCode(
                data.account().modeCode()
        );
        if (mode != AccountMode.ULTIMATE_IRONMAN)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    RiskLevel.NONE,
                    get(940));
        }

        if (UimStorageMechanics.isTooGenericToRecommend(capability))
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    RiskLevel.HIGH,
                    get(942));
        }

        if (UimStorageMechanics.isRestrictedRetrieval(capability))
        {
            UimStorageMechanicProfile mechanic =
                    UimStorageMechanics.profile(capability);
            if (mechanic == null
                    || !mechanic.hasCompleteRecommendationRules())
                return decision(capability, false,
                        Confidence.CHECK_NEEDED,
                        riskFor(capability),
                        get(943));
        }

        var storage = data.storage();
        Capability capabilityState = storage == null
                ? Capability.UNKNOWN
                : storage.stateOf(capability);

        if (capabilityState == Capability.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(944));
        }
        if (capabilityState != Capability.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(945));
        }

        if (itemCompatibility == Capability.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(946));
        }
        if (itemCompatibility != Capability.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(947));
        }

        if (capacityOrPreconditions == Capability.BLOCKED)
        {
            return decision(capability, false,
                    Confidence.BLOCKED,
                    riskFor(capability),
                    get(948));
        }
        if (capacityOrPreconditions != Capability.VERIFIED)
        {
            return decision(capability, false,
                    Confidence.CHECK_NEEDED,
                    riskFor(capability),
                    get(949));
        }

        return decision(capability, true,
                Confidence.VERIFIED,
                riskFor(capability),
                get(941));
    }

    public boolean shouldRequireExplicitWarning(StorageKind capability)
    {
        var risk = riskFor(capability);
        return risk == RiskLevel.HIGH || risk == RiskLevel.IRREVERSIBLE;
    }

    private static RiskLevel riskFor(StorageKind capability)
    {
        UimStorageMechanicProfile profile =
                UimStorageMechanics.profile(capability);
        if (profile != null) return profile.getRisk();
        if (capability == StorageKind.DEATH_STORAGE
                || UimStorageMechanics.isExactItemRetrievalService(capability))
        {
            return RiskLevel.HIGH;
        }
        if (capability == StorageKind.DEATHPILE)
        {
            return RiskLevel.IRREVERSIBLE;
        }
        if (capability == StorageKind.LOOTING_BAG)
        {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private static UimStorageDecision decision(
            StorageKind capability,
            boolean allowed,
            Confidence confidence,
            RiskLevel risk,
            String explanation)
    {
        return new UimStorageDecision(
                capability, allowed, confidence, risk, explanation
        );
    }
}

/**
 * Enforces the UIM inventory hierarchy without ever inventing a droppable item.
 * A caller supplies item-specific compatibility and plan value; static storage
 * possibility alone is insufficient.
 */
@Singleton
final class UimInventoryResolutionService
{
    private final UimCapabilityService capabilityService;

    @Inject
    public UimInventoryResolutionService(UimCapabilityService capabilityService)
    {
        this.capabilityService = capabilityService == null
                ? new UimCapabilityService() : capabilityService;
    }

    public UimInventoryResolutionService()
    {
        this(new UimCapabilityService());
    }

    public UimInventoryResolution resolve(GameData data,
            InventoryFootprint footprint,
            boolean goodLowFootprintAlternativeKnown,
            boolean productiveConsumptionKnown,
            List<UimStorageOption> proposedStorage)
    {
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN : AccountMode.fromTypeCode(
                        data.account().modeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            return unresolved(get(997));

        var inventory = data.inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return unresolved(get(999));
        InventoryFootprint needed = footprint == null
                ? InventoryFootprint.lowPressure() : footprint;
        int free = max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        if (free >= needed.minimumPracticalFreeSlots)
            return result(UimInventoryKind.USE_AS_IS,
                    Confidence.VERIFIED, null, null,
                    get(1000));
        if (goodLowFootprintAlternativeKnown)
            return result(UimInventoryKind.USE_LOW_FOOTPRINT_ALTERNATIVE,
                    Confidence.VERIFIED, null, null,
                    get(1001));
        if (productiveConsumptionKnown)
            return result(UimInventoryKind.PRODUCTIVELY_CONSUME_RESOURCES,
                    Confidence.CHECK_NEEDED, null, null,
                    get(1002));

        List<UimStorageOption> options = proposedStorage == null
                ? emptyList() : new ArrayList<>(proposedStorage);
        options.sort(Comparator.comparingInt(
                option -> priority(option.capability)));

        for (UimStorageOption option : options)
        {
            var capability = option.capability;
            if (option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(capability))
                continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryKind.USE_VERIFIED_SAFE_STORAGE,
                        Confidence.VERIFIED, decision, null,
                        get(1003));
        }

        for (UimStorageOption option : options)
        {
            if (!option.isRequiresConstruction()
                    || UimStorageMechanics.isRestrictedRetrieval(
                            option.capability)
                    || option.getRecurringInfrastructureValue().ordinal()
                            < Priority.HIGH.ordinal()) continue;
            return result(UimInventoryKind.BUILD_HIGH_VALUE_SAFE_STORAGE,
                    Confidence.CHECK_NEEDED, null, null,
                    get(1004));
        }

        for (UimStorageOption option : options)
        {
            if (option.capability != StorageKind.LOOTING_BAG)
                continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryKind.USE_RESTRICTED_RETRIEVAL,
                        Confidence.CHECK_NEEDED, decision, null,
                        get(1005));
        }

        for (UimStorageOption option : options)
        {
            var capability = option.capability;
            if (!UimStorageMechanics.isDangerous(capability)
                    || !option.isMajorProgressionTransition()) continue;
            var decision = evaluate(data, option);
            if (decision.isAllowed())
                return result(UimInventoryKind.USE_DANGEROUS_DEATH_STORAGE,
                        Confidence.CHECK_NEEDED, decision,
                        RecommendationRiskDisclosure.deathStorage(),
                        get(1006));
        }
        return unresolved(get(998));
    }

    private UimStorageDecision evaluate(GameData data,
            UimStorageOption option)
    {
        return capabilityService.evaluateStorage(data, option.capability,
                option.getItemCompatibility(),
                option.getCapacityOrPreconditions());
    }

    private static int priority(StorageKind capability)
    {
        if (capability == StorageKind.POH_COSTUME_ROOM
                || capability == StorageKind.POH_STORAGE
                || capability == StorageKind.STASH
                || capability == StorageKind.TOOL_LEPRECHAUN) return 0;
        if (capability == StorageKind.SEED_BOX
                || capability == StorageKind.HERB_SACK
                || capability == StorageKind.RUNE_POUCH) return 1;
        if (capability == StorageKind.LOOTING_BAG) return 2;
        return 3;
    }

    private static UimInventoryResolution unresolved(String reason)
    {
        return result(UimInventoryKind.UNRESOLVED,
                Confidence.CHECK_NEEDED, null, null, reason);
    }

    private static UimInventoryResolution result(
            UimInventoryKind kind,
            Confidence confidence,
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure, String reason)
    {
        return new UimInventoryResolution(kind, confidence, decision,
                disclosure, reason);
    }
}

/**
 * Remembers distinct exact inventory layouts only when multiple live activity
 * families are blocked by their own sourced footprint. Repeated evaluations of
 * one unchanged full inventory therefore cannot manufacture infrastructure
 * value.
 */
@Singleton
final class UimRecurringPressureService
{
    private static final int MAX_LAYOUTS_PER_ACCOUNT = 8;
    private final Map<String, LinkedHashSet<Integer>> layouts = new HashMap<>();
    private final ActivityStrategyKnowledgeCatalog activityCatalog =
            new ActivityStrategyKnowledgeCatalog();
    private final MethodStrategyKnowledgeCatalog methodCatalog =
            new MethodStrategyKnowledgeCatalog();
    private final List<CuratedTrainingMethod> skillingMethods =
            skillingMethods();

    public synchronized UimRecurringPressureAssessment observe(
            StrategyContext context)
    {
        var blocked = blockedFamilies(context);
        if (blocked.size() < 2)
            return new UimRecurringPressureAssessment(0, blocked);

        var data = context.data();
        var account = accountKey(data.account());
        LinkedHashSet<Integer> observed = layouts.computeIfAbsent(account,
                ignored -> new LinkedHashSet<>());
        observed.add(fingerprint(data.inventory()));
        while (observed.size() > MAX_LAYOUTS_PER_ACCOUNT)
            observed.remove(observed.iterator().next());
        return new UimRecurringPressureAssessment(observed.size(), blocked);
    }

    private List<String> blockedFamilies(StrategyContext context)
    {
        List<String> result = new ArrayList<>();
        if (context == null
                || context.accountMode() != AccountMode.ULTIMATE_IRONMAN
                || context.data() == null) return result;
        var inventory = context.data().inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return result;
        int free = max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));

        if (blockedSkilling(context.data().account(), free))
            result.add("skilling");

        var quests = context.data().quests();
        if (quests != null && quests.quests().values().stream().anyMatch(
                status -> status == QuestStatus.NOT_STARTED
                        || status == QuestStatus.IN_PROGRESS)
                && blocked("quest:observed", free)) result.add("questing");

        var clue = context.data().clue();
        if (clue != null && clue.cluePresent
                && blocked("clue:observed", free)) result.add("clues");

        var pvm = context.data().pvm();
        if (pvm != null && !pvm.getReadinessByActivity().isEmpty()
                && blocked("pvm:observed", free)) result.add("pvm");

        var minigames = context.data().minigames();
        if (minigames != null)
            for (String id : minigames.getUnlocked())
                if (blocked("minigame:" + id, free))
                {
                    result.add("minigames");
                    break;
                }
        return result;
    }

    private boolean blockedSkilling(AccountSnapshot account, int free)
    {
        if (account == null) return false;
        for (CuratedTrainingMethod candidate : skillingMethods)
        {
            var method = candidate.method();
            var metadata = candidate.getMetadata();
            if (method == null || metadata == null
                    || !metadata.uimFriendly
                    || !method.supportsLevel(account.level(
                            method.getSkill()))
                    || method.confidence
                            != Confidence.VERIFIED
                    || !method.requirements.isEmpty()
                    || !AccountBuildPolicy.allowsMethod(account, method)
                    || !ContentAccessRules.isMethodAvailable(method,
                            account.membership())) continue;
            MethodStrategyProfile profile = methodCatalog.profileFor(method,
                    metadata, AccountMode.ULTIMATE_IRONMAN);
            if (profile != null && profile.inventoryFootprint != null
                    && profile.inventoryFootprint
                            .minimumPracticalFreeSlots > free)
                return true;
        }
        return false;
    }

    private boolean blocked(String candidateId, int free)
    {
        ActivityStrategyProfile profile = activityCatalog.profileFor(
                candidateId, AccountMode.ULTIMATE_IRONMAN);
        return profile != null && profile.inventoryFootprint != null
                && profile.inventoryFootprint
                        .minimumPracticalFreeSlots > free;
    }

    private static String accountKey(AccountSnapshot account)
    {
        if (account == null) return "unknown-uim";
        if (account.hasStableAccountIdentity())
            return Long.toUnsignedString(account.accountHash);
        return account.playerName + ":" + account.modeCode();
    }

    private static int fingerprint(ItemsState inventory)
    {
        var value = 1;
        for (ItemState item : inventory.getItems())
        {
            value = 31 * value + (item == null ? 0 : item.itemId);
            value = 31 * value + (item == null ? -1 : item.slotIndex);
            value = 31 * value + (item == null ? 0 : item.quantity);
        }
        return value;
    }

    private static List<CuratedTrainingMethod> skillingMethods()
    {
        List<CuratedTrainingMethod> result = new ArrayList<>();
        var catalog = new TrainingMethodCatalog();
        for (Skill skill : Skill.values())
        {
            result.addAll(catalog.curatedFor(skill));
            result.addAll(catalog.f2pFor(skill));
        }
        return unmodifiableList(result);
    }
}

/**
 * Scores the practical cost of changing activities on Ultimate Ironman.
 *
 * <p>UIM efficiency is not simply XP/hour. A route can be theoretically fast
 * while being strategically poor because it requires dismantling a valuable
 * inventory, retrieving death storage, emptying a looting bag, or exposing a
 * retrieval service to a dangerous death. This service only uses observed state
 * and method metadata. Unknown storage is never treated as empty.</p>
 */
@Singleton
class UimSetupCostService
{
    public double score(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null
                || context.accountMode() != AccountMode.ULTIMATE_IRONMAN)
        {
            return 0.0;
        }

        var data = context.data();
        if (data == null) return 0.0;

        var value = 0.0;
        var plan = recommendation.plan();
        var method = plan == null ? null : plan.method();
        var setupMinutes = method == null ? 0 : max(0, method.getSetupMinutes());
        var occupied = occupiedInventorySlots(data.inventory());

        // Unknown/non-skill setup does not receive a fake "low setup" bonus.
        if (method != null)
        {
            if (setupMinutes <= 3) value += 5.0;
            else if (setupMinutes >= 12) value -= 11.0;
            else if (setupMinutes >= 7) value -= 5.0;
        }

        if (occupied >= 24 && setupMinutes >= 7) value -= 8.0;
        else if (occupied >= 20 && setupMinutes >= 7) value -= 4.0;

        var storage = data.storage();
        boolean deathStorageObserved = hasObservedItems(
                storage, StorageKind.DEATH_STORAGE)
                || hasObservedItems(storage,
                        StorageKind.HESPORI_ITEM_RETRIEVAL)
                || hasObservedItems(storage,
                        StorageKind.ZULRAH_ITEM_RETRIEVAL)
                || hasObservedItems(storage,
                        StorageKind.VOLCANIC_MINE_ITEM_RETRIEVAL);
        boolean deathpileObserved = hasObservedItems(
                storage, StorageKind.DEATHPILE);
        boolean lootingBagObserved = hasObservedItems(
                storage, StorageKind.LOOTING_BAG);

        StrategicValue strategic =
                recommendation.strategicValue;
        boolean dangerous = method != null && method.wilderness
                || strategic.getRiskBurden() >= 0.5;

        // Active death storage is not a small inconvenience. A dangerous death
        // can delete or otherwise invalidate a carefully prepared UIM state, so
        // a merely attractive gear goal must not overwhelm this protection with
        // raw provider score.
        if (dangerous && deathStorageObserved) value -= 50.0;
        if (dangerous && deathpileObserved) value -= 22.0;

        if ((deathStorageObserved || deathpileObserved || lootingBagObserved)
                && strategic.getOpportunityCost() >= 0.5
                && strategic.setupReuse < 0.5)
        {
            value -= 10.0;
        }
        value += strategic.setupReuse * 7.0;
        return value;
    }

    static int occupiedInventorySlots(ItemsState inventory)
    {
        if (inventory == null || inventory.getItems() == null) return 0;
        var slots = 0;
        for (ItemState item : inventory.getItems())
        {
            if (item != null && item.quantity > 0) slots++;
        }
        return slots;
    }

    static boolean hasObservedItems(
            StorageSnapshot storage,
            StorageKind capability)
    {
        if (storage == null || capability == null
                || !storage.hasObservedContents(capability))
        {
            return false;
        }
        var items = storage.contentsOf(capability);
        if (items == null) return false;
        for (ItemState item : items)
        {
            if (item != null && item.quantity > 0) return true;
        }
        return false;
    }
}
