package compass;
import static net.runelite.api.Skill.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import com.google.gson.Gson;
import java.awt.Color;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.*;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import lombok.*;
import lombok.experimental.Accessors;
import net.runelite.api.*;
import net.runelite.api.gameval.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import static compass.Text.get;

/**
 * Persists positive access observations per RuneScape character.
 *
 * <p>Only directly observed facts belong here. Inferred access from quests is
 * recalculated from live state instead of being permanently cached as fact.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class AccountAccessMemoryStore
{
    static final String GROUP = get(1609);
    private static final String KEY = "accessMemory";
    final ConfigManager configManager;
    final Gson gson;
    final Map<String, Long> memory = new HashMap<>();
    private String loadedProfileKey;

    public synchronized AccessMemorySnapshot snapshot()
    {
        syncProfile();
        return new AccessMemorySnapshot(memory);
    }

    /**
     * Records a fact only when it is new. Re-saving every game tick would create
     * unnecessary config writes, so repeated observations are intentionally cheap.
     */
    public synchronized boolean remember(String observationKey)
    {
        if (observationKey == null || observationKey.trim().isEmpty())
        {
            return false;
        }

        syncProfile();

        if (memory.containsKey(observationKey))
        {
            return false;
        }

        memory.put(observationKey, System.currentTimeMillis());
        configManager.setRSProfileConfiguration(
                GROUP,
                KEY,
                gson.toJson(memory)
        );
        loadedProfileKey = configManager.getRSProfileKey();
        return true;
    }

    public synchronized void clearCacheForAccountChange()
    {
        loadedProfileKey = null;
        memory.clear();
    }

    private void syncProfile()
    {
        var activeKey = configManager.getRSProfileKey();

        if (Objects.equals(loadedProfileKey, activeKey)
                && loadedProfileKey != null)
        {
            return;
        }

        memory.clear();

        if (activeKey != null)
        {
            var json = configManager.getRSProfileConfiguration(GROUP, KEY);
            if (json != null && !json.trim().isEmpty())
            {
                var stored = ProfileJsonCodec.longs(gson, json);
                if (stored != null)
                {
                    memory.putAll(stored);
                }
            }
        }

        loadedProfileKey = activeKey;
    }
}

/**
 * Stores only capabilities the plugin can verify or the player has confirmed.
 * Unknown is a first-class state. The strategist should never assume a storage
 * method or unlock exists just because that method exists in the game.
 */
final class AccountCapabilities
{
    final Map<String, Capability> states = new HashMap<>();

    public Capability get(String key)
    {
        return states.getOrDefault(key, Capability.UNKNOWN);
    }

    public void set(String key, Capability state)
    {
        states.put(key, state);
    }

    public boolean verified(String key)
    {
        return get(key) == Capability.VERIFIED;
    }
}

enum AccountMode
{
    MAIN,
    IRONMAN,
    ULTIMATE_IRONMAN,
    HARDCORE_IRONMAN,
    GROUP_IRONMAN,
    HARDCORE_GROUP_IRONMAN,
    UNRANKED_GROUP_IRONMAN,
    UNKNOWN;

    public static AccountMode fromTypeCode(int typeCode)
    {
        switch (typeCode)
        {
            case 0: return MAIN;
            case 1: return IRONMAN;
            case 2: return ULTIMATE_IRONMAN;
            case 3: return HARDCORE_IRONMAN;
            case 4: return GROUP_IRONMAN;
            case 5: return HARDCORE_GROUP_IRONMAN;
            case 6: return UNRANKED_GROUP_IRONMAN;
            default: return UNKNOWN;
        }
    }

    public boolean usesGrandExchange()
    {
        return this == MAIN;
    }

    public boolean isGroupIronman()
    {
        return this == GROUP_IRONMAN
                || this == HARDCORE_GROUP_IRONMAN
                || this == UNRANKED_GROUP_IRONMAN;
    }
    public boolean isIronLike()
    {
        return this != MAIN && this != UNKNOWN;
    }
}

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class AccountReader
{
    final Client client;

    public AccountSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN
                || client.getLocalPlayer() == null)
        {
            return null;
        }

        var playerName = client.getLocalPlayer().getName();

        if (playerName == null || playerName.isEmpty())
        {
            playerName = "Unknown Player";
        }

        int accountTypeCode =
                client.getVarbitValue(VarbitID.IRONMAN);

        String accountTypeName =
                formatAccountType(accountTypeCode);

        // RuneLite itself uses ACCOUNT_CREDIT > 0 as the account-level member
        // signal. A members world is retained as a safety proof because an F2P
        // account cannot be logged into one.
        int membershipCredit = client.getVarpValue(
                VarPlayerID.ACCOUNT_CREDIT
        );

        boolean membersWorld = client.getWorldType() != null
                && client.getWorldType().contains(WorldType.MEMBERS);

        Membership membershipStatus =
                membershipCredit > 0 || membersWorld
                        ? Membership.P2P
                        : Membership.F2P;

        Map<Skill, Integer> skillLevels =
                new EnumMap<>(Skill.class);

        Map<Skill, Integer> skillExperience =
                new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            int level =
                    client.getRealSkillLevel(skill);

            int experience =
                    client.getSkillExperience(skill);

            skillLevels.put(skill, level);
            skillExperience.put(skill, experience);
        }

        return new AccountSnapshot(
                playerName,
                client.getAccountHash(),
                accountTypeCode,
                accountTypeName,
                membershipStatus,
                membershipCredit,
                client.getTotalLevel(),
                client.getOverallExperience(),
                skillLevels,
                skillExperience
        );
    }

    private String formatAccountType(int type)
    {
        switch (type)
        {
            case 0:
                return "Main";

            case 1:
                return "Ironman";

            case 2:
                return get(1606);

            case 3:
                return get(1607);

            case 4:
                return "Group Ironman";

            case 5:
                return get(1108);

            case 6:
                return get(1109);

            default:
                return "Unknown";
        }
    }
}

/**
 * Account properties that change the value of an activity or unlock.
 *
 * <p>These are deliberately not method identities. Candidate metadata can be
 * matched to these dimensions without teaching the selector that a named
 * account mode should always choose a named method.</p>
 */
@RequiredArgsConstructor
@Getter
enum AccountDimension
{
    INVENTORY_PRESSURE(AccountDimensionRole.BURDEN_WEIGHT),
    BANK_AVAILABILITY(AccountDimensionRole.CAPABILITY_GATE),
    GRAND_EXCHANGE_AVAILABILITY(AccountDimensionRole.CAPABILITY_GATE),
    SELF_SOURCING_BURDEN(AccountDimensionRole.BURDEN_WEIGHT),
    SHARED_RESOURCE_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    SHARED_INFRASTRUCTURE_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    STORAGE_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    POH_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    TELEPORT_INFRASTRUCTURE_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    SETUP_COST_SENSITIVITY(AccountDimensionRole.BURDEN_WEIGHT),
    DEATH_RISK_SENSITIVITY(AccountDimensionRole.BURDEN_WEIGHT),
    CONSUMABLE_REPLACEMENT_DIFFICULTY(
            AccountDimensionRole.BURDEN_WEIGHT),
    STORABLE_EQUIPMENT_VALUE(AccountDimensionRole.BENEFIT_WEIGHT),
    DUPLICATE_GRIND_PENALTY(AccountDimensionRole.BURDEN_WEIGHT),
    GP_LIQUIDITY_STORAGE_VALUE(AccountDimensionRole.BENEFIT_WEIGHT);

    final AccountDimensionRole role;
}

/** One explainable account-mode/state contribution. */
@Getter
final class AccountPriority
{
    final AccountDimension dimension;
    final Priority priority;
    final Capability capabilityState;
    final Confidence confidence;
    final String reason;

    public AccountPriority(
            AccountDimension dimension,
            Priority priority,
            Capability capabilityState,
            Confidence confidence,
            String reason)
    {
        if (dimension == null) throw new IllegalArgumentException("dimension");
        this.dimension = dimension;
        this.priority = priority == null ? Priority.NONE : priority;
        this.capabilityState = capabilityState == null
                ? Capability.UNKNOWN : capabilityState;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
        this.reason = reason == null ? "" : reason;
    }

}

/** One action exposed by RuneLite's maintained skill-calculator data. */
@Getter
final class ActionDef
{
    final Skill skill;
    final String id;
    final String name;
    final int level;
    final float xp;
    final String category;
    final Membership membership;
    final int itemId;

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, Membership membership)
    {
        this(skill, id, name, level, xp, category, membership, -1);
    }

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, Membership membership,
            int itemId)
    {
        this.skill = skill;
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.category = category;
        this.membership = membership == null ? Membership.UNKNOWN : membership;
        this.itemId = itemId;
    }

}

enum AttentionLevel
{
    ACTIVE,
    MODERATE,
    LOW,
    AFK
}

/**
 * Treasure Trail tiers plus membership eligibility.
 *
 * <p>Membership filtering belongs on the clue tier itself so every Compass
 * surface (DO NEXT, opportunities, strategy signals, future reminders) uses the
 * same rule. This prevents a members-only clue left in a bank from being
 * presented as actionable while the character is currently F2P.</p>
 */
@RequiredArgsConstructor
@Getter
enum ClueTier
{
    BEGINNER(0.0),
    EASY(1.0),
    MEDIUM(2.0),
    HARD(3.5),
    ELITE(5.0),
    MASTER(7.0),
    UNKNOWN(0.0);

    final double priorityBonus;

    /**
     * Only beginner Treasure Trails are actionable on a F2P planning profile.
     * Unknown tiers stay eligible so Compass can surface them as Needs Info
     * rather than silently pretending it knows the tier.
     */
    public boolean isAvailableFor(Membership membershipStatus)
    {
        if (membershipStatus != Membership.P2P)
        {
            return this == BEGINNER;
        }
        return true;
    }

    public static ClueTier fromText(String value)
    {
        if (value == null) return UNKNOWN;
        var normalized = value.toLowerCase(Locale.ROOT);
        for (ClueTier tier : values())
            if (tier != UNKNOWN && normalized.contains(tier.name().toLowerCase(Locale.ROOT)))
                return tier;
        return UNKNOWN;
    }
}

@RequiredArgsConstructor
@Getter
enum CombatAchievementTier
{
    EASY(41),
    MEDIUM(161),
    HARD(416),
    ELITE(1064),
    MASTER(1904),
    GRANDMASTER(2630);

    final int rewardPoints;
}

enum Confidence
{
    VERIFIED,
    CHECK_NEEDED,
    BLOCKED
}

/**
 * Membership-level content gates that run before deeper account capability
 * checks. Unknown membership fails closed to F2P-safe content instead of
 * temporarily leaking members-only recommendations into the queue.
 */
final class ContentAccessRules
{
    private static final Set<Skill> FREE_TO_PLAY_SKILLS = EnumSet.of(
            ATTACK,
            STRENGTH,
            DEFENCE,
            RANGED,
            PRAYER,
            MAGIC,
            RUNECRAFT,
            HITPOINTS,
            CRAFTING,
            MINING,
            SMITHING,
            FISHING,
            COOKING,
            FIREMAKING,
            WOODCUTTING
    );

    private static final Set<String> MEMBERS_ONLY_METHOD_IDS = Set.of(
            "runecraft_gotr",
            "mining_mlm",
            get(1671),
            get(1672),
            get(1673),
            get(1674),
            get(1675),
            get(1676),
            get(1677),
            "farming_tithe",
            "hunter_rumours",
            get(1678),
            get(1679),
            get(1680),
            get(1681)
    );

    private ContentAccessRules()
    {
    }

    public static boolean isSkillAvailable(
            Skill skill,
            Membership membershipStatus)
    {
        if (skill == null) return false;
        if (membershipStatus == Membership.P2P) return true;

        // F2P and UNKNOWN both use the F2P skill boundary. UNKNOWN is treated
        // conservatively until RuneLite gives Compass verified membership.
        return FREE_TO_PLAY_SKILLS.contains(skill);
    }

    public static boolean isMethodAvailable(
            TrainingMethod method,
            Membership membershipStatus)
    {
        if (method == null || !isSkillAvailable(method.getSkill(), membershipStatus))
        {
            return false;
        }
        if (membershipStatus == Membership.P2P) return true;

        // F2P and UNKNOWN are intentionally identical here. A transient access
        // read may temporarily narrow a member to safe F2P routes, but can never
        // expose a members-only route to an F2P account.
        return !method.membersOnly
                && !MEMBERS_ONLY_METHOD_IDS.contains(method.id);
    }

    public static boolean isFreeToPlaySkill(Skill skill)
    {
        return skill != null && FREE_TO_PLAY_SKILLS.contains(skill);
    }

    /** UNKNOWN receives only records explicitly marked F2P-safe. */
    public static boolean isContentAvailable(
            Membership membershipStatus,
            boolean freeToPlay)
    {
        return membershipStatus == Membership.P2P || freeToPlay;
    }

    public static boolean hasVerifiedMembership(Membership membershipStatus)
    {
        return membershipStatus == Membership.P2P;
    }
}

/** A concrete method paired with the strategy metadata needed to rank it safely. */
@Getter
@RequiredArgsConstructor
final class CuratedTrainingMethod
{
    final TrainingMethod method;

    TrainingMethod method() { return method; }
    final TrainingMethodMetadata metadata;


}

/**
 * Reviewed, recommendation-relevant live changes that are easy to regress.
 * Announced changes remain separate and must never alter runtime planning.
 */
final class CurrentLiveContentChanges
{
    public enum Status
    {
        LIVE_CURRENT,
        ANNOUNCED_NOT_LIVE,
        UNKNOWN,
        REMOVED_SUPERSEDED
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Entry
    {
        final String id;
        final LocalDate effectiveDate;
        final Status status;
        final String behavior;
        final String source;
    }

    private static final String OFFICIAL = get(189);
    private static final List<Entry> ENTRIES = unmodifiableList(Arrays.asList(
            new Entry(get(1652), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(192), OFFICIAL),
            new Entry(get(1653), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(193), OFFICIAL),
            new Entry(get(1664), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(194), OFFICIAL),
            new Entry(get(1685), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(195), OFFICIAL),
            new Entry(get(1686), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(196), OFFICIAL),
            new Entry(get(1687), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(197), OFFICIAL),
            new Entry(get(1654), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(198), OFFICIAL),
            new Entry(get(1665), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(199), OFFICIAL),
            new Entry(get(1688), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(190), OFFICIAL),
            new Entry(get(1689), LocalDate.of(2026, 9, 2),
                    Status.ANNOUNCED_NOT_LIVE,
                    get(191), OFFICIAL)
    ));

    private CurrentLiveContentChanges() { }

    public static List<Entry> all() { return ENTRIES; }

    public static boolean mayAffectPlanning(String id, LocalDate validationDate)
    {
        for (Entry entry : ENTRIES)
            if (entry.id.equals(id))
                return entry.status == Status.LIVE_CURRENT
                        && !entry.effectiveDate.isAfter(validationDate);
        return false;
    }
}

/**
 * Narrow corrections for verified live changes newer than the pinned RuneLite
 * skill-calculator data. Announced changes never enter this map.
 */
final class CurrentLiveSkillActionOverrides
{
    private static final LocalDate VALIDATION_DATE = LocalDate.of(2026, 8, 25);
    private static final Map<String, Integer> LEVELS;
    private static final Map<String, Float> XP;
    private static final Set<String> UNSAFE_STALE_XP;

    static
    {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                get(1652), VALIDATION_DATE))
            levels.put(get(201), 77);
        if (CurrentLiveContentChanges.mayAffectPlanning(
                get(1653), VALIDATION_DATE))
            levels.put(get(202), 87);
        LEVELS = unmodifiableMap(levels);

        Map<String, Float> xp = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                get(1654), VALIDATION_DATE))
        {
            xp.put(get(1655), 112f);
            xp.put(get(1656), 168f);
            xp.put(get(1657), 224f);
            xp.put(get(1658), 280f);
            xp.put(get(1659), 369f);
            xp.put(get(1660), 480f);
            xp.put(get(1661), 612f);
            xp.put(get(1662), 969f);
            xp.put(get(1663), 1200f);
        }
        XP = unmodifiableMap(xp);

        Set<String> stale = new LinkedHashSet<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                get(1664), VALIDATION_DATE))
        {
            stale.add(get(203));
            stale.add(get(204));
        }
        if (CurrentLiveContentChanges.mayAffectPlanning(
                get(1665), VALIDATION_DATE))
        {
            stale.add(get(1666));
            stale.add(get(1667));
            stale.add(get(1668));
            stale.add(get(1669));
            stale.add(get(1670));
        }
        UNSAFE_STALE_XP = unmodifiableSet(stale);
    }

    private CurrentLiveSkillActionOverrides() { }

    public static int level(String actionId, int upstreamLevel)
    {
        return LEVELS.getOrDefault(actionId, upstreamLevel);
    }

    public static Map<String, Integer> levelOverrides()
    {
        return LEVELS;
    }

    public static float xp(String actionId, float upstreamXp)
    {
        if (UNSAFE_STALE_XP.contains(actionId)) return 0f;
        return XP.getOrDefault(actionId, upstreamXp);
    }

    public static Map<String, Float> xpOverrides() { return XP; }
    public static Set<String> suppressedStaleXp() { return UNSAFE_STALE_XP; }
}

enum DiaryTier
{
    EASY,
    MEDIUM,
    HARD,
    ELITE
}

enum PatchState
{
    UNKNOWN,
    EMPTY,
    GROWING,
    READY,
    DISEASED,
    DEAD
}

enum FarmingPatchKind
{
    HERB,
    TREE
}

/** Per-character memory of directly observed herb/tree patch state. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class FarmingRunStateStore
{
    static final String GROUP = get(1609);
    private static final String KEY = get(1705);
    final ConfigManager configManager;
    final Gson gson;
    final Map<String, ObservedFarmingPatchState> states = new HashMap<>();
    private String loadedProfileKey;

    public synchronized FarmingRunSnapshot snapshot()
    {
        syncProfile();
        return new FarmingRunSnapshot(states);
    }

    public synchronized boolean remember(
            String patchId,
            PatchState state)
    {
        if (patchId == null || state == null || state == PatchState.UNKNOWN)
        {
            return false;
        }
        syncProfile();
        var previous = states.get(patchId);
        if (previous != null && previous.getState() == state)
        {
            return false;
        }
        states.put(patchId, new ObservedFarmingPatchState(
                state, System.currentTimeMillis()));
        configManager.setRSProfileConfiguration(
                GROUP, KEY, gson.toJson(states));
        loadedProfileKey = configManager.getRSProfileKey();
        return true;
    }

    public synchronized void clearCacheForAccountChange()
    {
        loadedProfileKey = null;
        states.clear();
    }

    private void syncProfile()
    {
        var active = configManager.getRSProfileKey();
        if (Objects.equals(loadedProfileKey, active) && active != null) return;
        states.clear();
        if (active != null)
        {
            var json = configManager.getRSProfileConfiguration(GROUP, KEY);
            if (json != null && !json.trim().isEmpty())
            {
                Map<String, ObservedFarmingPatchState> stored =
                        ProfileJsonCodec.farmingStates(gson, json);
                if (stored != null) states.putAll(stored);
            }
        }
        loadedProfileKey = active;
    }
}

enum FeedbackAction
{
    LATER,
    NOT_TODAY,
    DISLIKE
}

/**
 * Defense-in-depth validation after method, resources, inventory, location and
 * guidance have all been resolved.
 */
@Singleton
final class FinalExecutionPlanValidator
{
    public Recommendation validate(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null) return null;
        var plan = recommendation.plan();
        MethodStrategyProfile profile = plan == null
                ? null : plan.getStrategyProfile();

        var evidence = recommendation.safetyEvidence;
        var guidance = recommendation.guidance;
        if (plan != null)
        {
            var method = plan.method();
            var current = recommendation.currentLevel;
            var stageTarget = recommendation.getCurrentExecutionTargetLevel();
            boolean invalid = method == null
                    || blank(method.getName())
                    || current <= 0
                    || !method.supportsLevel(current)
                    || stageTarget <= current
                    || recommendation.targetLevel > 0
                        && stageTarget > recommendation.targetLevel
                    || context != null && context.data() != null
                        && context.data().account() != null
                        && !ContentAccessRules.isMethodAvailable(method,
                                context.data().account()
                                        .membership())
                    || guidance == null
                    || blank(guidance.getAction())
                    || blank(guidance.location);
            if (invalid) evidence = evidence.withInvalidCurrentExecution();
        }
        if (profile != null && profile.bankingBehavior
                        == BankingMode.CONVENTIONAL_BANK_LOOP
                || guidance != null && guidance.bankingBehavior
                        == BankingMode.CONVENTIONAL_BANK_LOOP)
        {
            evidence = evidence.requiringConventionalBank();
        }
        if (guidance != null && guidance.getStorageCapability() != null)
        {
            var capability = guidance.getStorageCapability();
            var decision = guidance.getStorageDecision();
            boolean storageUnverified = decision == null
                    || !decision.isAllowed()
                    || decision.confidence
                            != Confidence.VERIFIED;
            boolean incompleteDangerDisclosure =
                    UimStorageMechanics.isDangerous(capability)
                    && (guidance.getRiskDisclosure() == null
                    || !guidance.getRiskDisclosure()
                            .isAcknowledgementRequired());
            if (storageUnverified
                    || UimStorageMechanics.isTooGenericToRecommend(capability)
                    || incompleteDangerDisclosure)
                evidence = evidence.withUnverifiedDangerousStorage();
        }
        return recommendation.withSafetyEvidence(evidence);
    }

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}

/** Immutable bundle containing everything Compass currently knows. */
@Getter
@Accessors(fluent = true)
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class GameData
{
    final AccountSnapshot account;
    final ItemsState inventory;
    final ItemsState bank;
    final ItemsState equipment;
    final QuestSnapshot quests;
    final DiarySnapshot diaries;
    final ClueSnapshot clue;
    final CombatAchievementSnapshot combatAchievements;
    final CollectionLogSnapshot collectionLog;
    final AccountEconomySnapshot economy;
    final AccountCapabilities capabilities;
    final AccessMemorySnapshot accessMemory;
    final FarmingRunSnapshot farmingRuns;
    final StorageSnapshot storage;
    final TransportSnapshot transport;
    final PohSnapshot poh;
    final ItemsState groupStorage;
    final SlayerSnapshot slayer;
    final FarmingSnapshot farming;
    final SailingSnapshot sailing;
    final MinigameSnapshot minigames;
    final PvmSnapshot pvm;
    final RecurringOpportunitySnapshot recurringOpportunities;
    final CombatEvidenceSnapshot combatEvidence;

    public GameData(
            AccountSnapshot account,
            ItemsState inventory,
            ItemsState bank,
            ItemsState equipment,
            QuestSnapshot quests,
            DiarySnapshot diaries,
            ClueSnapshot clue,
            CombatAchievementSnapshot combatAchievements,
            CollectionLogSnapshot collectionLog,
            AccountEconomySnapshot economy,
            AccountCapabilities capabilities)
    {
        this(account, inventory, bank, equipment, quests, diaries, clue,
                combatAchievements, collectionLog, economy, capabilities,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    public static Builder builder(AccountSnapshot account)
    {
        return newBuilder().account(account);
    }
}

/** Verified quest roots used by the shared goal-path traversal. */
@Singleton
final class GoalGraph
{
    private static final Map<GoalType, List<String>> QUEST_ROOTS;
    static
    {
        Map<GoalType, List<String>> roots = new EnumMap<>(GoalType.class);
        roots.put(GoalType.BARROWS_GLOVES,
                singletonList(get(1198)));
        roots.put(GoalType.PRIFDDINAS,
                singletonList(get(1721)));
        roots.put(GoalType.BOWFA,
                singletonList(get(1721)));
        QUEST_ROOTS = unmodifiableMap(roots);
    }

    public List<String> questRootsFor(GoalType goal)
    {
        return QUEST_ROOTS.getOrDefault(goal, emptyList());
    }

    public boolean hasPlanningPath(GoalType goal)
    {
        return goal != null && goal != GoalType.AUTOMATIC
                && goal != GoalType.CUSTOM;
    }
}

/** A validated dependency path connecting one recommendation to one selected goal. */
@Getter
final class GoalProvenance
{
    final GoalType goal;
    final GoalRelation relationship;
    final String recommendationId;
    final List<String> path;

    private GoalProvenance(GoalType goal,
            GoalRelation relationship,
            String recommendationId, List<String> path)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || relationship != GoalRelation.DIRECT
                && relationship != GoalRelation.PREREQUISITE
                || recommendationId == null || recommendationId.trim().isEmpty()
                || path == null || path.size() < 2)
        {
            throw new IllegalArgumentException(
                    get(263));
        }
        this.goal = goal;
        this.relationship = relationship;
        this.recommendationId = recommendationId;
        this.path = unmodifiableList(new ArrayList<>(path));
    }

    public static GoalProvenance direct(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
                GoalRelation.DIRECT,
                recommendationId, path);
    }

    public static GoalProvenance prerequisite(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
                GoalRelation.PREREQUISITE,
                recommendationId, path);
    }


    public boolean proves(GoalType selectedGoal, String actionId)
    {
        return goal == selectedGoal && recommendationId.equals(actionId)
                && path.size() >= 2;
    }

    public String compactPath()
    {
        return String.join(" → ", path);
    }

    /** Causal player copy derived only from the validated dependency path. */
    public String playerReason()
    {
        var goalName = path.isEmpty() ? goal.toString() : path.get(0);
        var action = path.get(path.size() - 1);
        if (relationship == GoalRelation.DIRECT)
            return action + get(1226) + goalName + " goal.";
        if (path.size() >= 3)
        {
            var parent = path.get(path.size() - 2);
            return action + get(1708) + parent
                    + get(1227) + goalName + " path.";
        }
        return action + get(1228) + goalName + " goal.";
    }
}

/** Guaranteed XP available from unfinished quests on the selected goal path. */
@Getter
final class GoalQuestRewardForecast
{
    final Skill skill;
    final int experience;
    final List<String> sourceQuests;

    GoalQuestRewardForecast(Skill skill, int experience, List<String> sourceQuests)
    {
        this.skill = skill;
        this.experience = max(0, experience);
        this.sourceQuests = unmodifiableList(
                new ArrayList<>(sourceQuests));
    }

    public boolean hasGuaranteedExperience() { return experience > 0; }
}

enum GoalType
{
    AUTOMATIC,
    MAX,
    QUEST_CAPE,
    BARROWS_GLOVES,
    FIRE_CAPE,
    PRIFDDINAS,
    BOWFA,
    INFERNAL_CAPE,
    DIARY_CAPE,
    ELITE_COMBAT_ACHIEVEMENTS,
    RAID_READY,
    TOTAL_2000,
    SLAYER_85,
    BASE_70S,
    GEAR_TARGET,
    CUSTOM;

    @Override
    public String toString()
    {
        if (this == AUTOMATIC) return "Automatic";
        if (this == BOWFA) return "Bowfa";
        if (this == MAX) return "Max cape";
        String lower = name().toLowerCase(java.util.Locale.ROOT)
                .replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

/** An exact item-ID requirement that fresh Group Storage may satisfy. */
@Getter
final class GroupResourceNeed
{
    final String label;
    final Set<Integer> acceptableItemIds;
    final int quantity;
    final boolean reusable;

    public GroupResourceNeed(String label, Set<Integer> acceptableItemIds,
            int quantity, boolean reusable)
    {
        if (acceptableItemIds == null || acceptableItemIds.isEmpty())
            throw new IllegalArgumentException(
                    get(299));
        this.label = label == null ? "Required item" : label;
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (Integer itemId : acceptableItemIds)
            if (itemId != null && itemId > 0) ids.add(itemId);
        if (ids.isEmpty())
            throw new IllegalArgumentException(
                    get(300));
        this.acceptableItemIds = unmodifiableSet(ids);
        this.quantity = max(1, quantity);
        this.reusable = reusable;
    }

}

/**
 * Account-specific instructions attached to a ranked recommendation.
 *
 * <p>The training-method catalog describes a route in general. This object
 * turns that route into concrete instructions for the current milestone and
 * the supplies Compass has actually observed.</p>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class Guidance
{
    final String action;
    final String supplies;
    final String location;
    final String progress;
    final String note;
    final BankingMode bankingBehavior;
    final UimStorageDecision storageDecision;
    final RecommendationRiskDisclosure riskDisclosure;

    public Guidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this(action, supplies, location, null, note,
                BankingMode.UNKNOWN, null, null);
    }

    public Guidance(
            String action,
            String supplies,
            String location,
            String note,
            BankingMode bankingBehavior)
    {
        this(action, supplies, location, null, note, bankingBehavior, null, null);
    }

    public Guidance(
            String action,
            String supplies,
            String location,
            String note,
            BankingMode bankingBehavior,
            UimStorageDecision storageDecision,
            RecommendationRiskDisclosure riskDisclosure)
    {
        this(action, supplies, location, null, note, bankingBehavior,
                storageDecision, riskDisclosure);
    }

    public BankingMode getBankingBehavior()
    {
        return bankingBehavior == null ? BankingMode.UNKNOWN : bankingBehavior;
    }

    public StorageKind getStorageCapability()
    {
        return storageDecision == null ? null
                : storageDecision.capability;
    }

    public Guidance withBankingBehavior(
            BankingMode value)
    {
        return new Guidance(action, supplies, location, progress,
                note,
                value, storageDecision, riskDisclosure);
    }

    public Guidance withProgress(String value)
    {
        return new Guidance(action, supplies, location, value,
                note, bankingBehavior, storageDecision, riskDisclosure);
    }
}

@Getter
final class GuidanceChecklist
{
    final String activityId;
    final String title;
    final String subtitle;
    final List<GuidanceStep> steps;
    final String bring;
    final String where;
    final String action;
    final String progress;
    final String important;

    public GuidanceChecklist(
            String activityId,
            String title,
            String subtitle,
            List<GuidanceStep> steps)
    {
        this(activityId, title, subtitle, steps, null, null, null, null, null);
    }

    public GuidanceChecklist(
            String activityId, String title, String subtitle,
            List<GuidanceStep> steps, String bring, String where,
            String action, String progress, String important)
    {
        this.activityId = activityId;
        this.title = title;
        this.subtitle = subtitle;
        this.steps = unmodifiableList(new ArrayList<>(
                steps == null ? emptyList() : steps));
        this.bring = bring;
        this.where = where;
        this.action = action;
        this.progress = progress;
        this.important = important;
    }
    public GuidanceStep firstPending()
    {
        for (GuidanceStep step : steps)
        {
            if (!step.isComplete()) return step;
        }
        return null;
    }
}

/** A reusable property supplied by an account infrastructure milestone. */
@RequiredArgsConstructor
@Getter
enum InfraBenefit
{
    INVENTORY_RELIEF(AccountDimension.INVENTORY_PRESSURE),
    POH_PLATFORM(AccountDimension.POH_VALUE),
    STORAGE(AccountDimension.STORAGE_VALUE),
    TRAVEL_NETWORK(AccountDimension.TELEPORT_INFRASTRUCTURE_VALUE),
    SETUP_REUSE(AccountDimension.SETUP_COST_SENSITIVITY),
    SELF_SUFFICIENCY(AccountDimension.SELF_SOURCING_BURDEN),
    SHARED_UTILITY(AccountDimension.SHARED_INFRASTRUCTURE_VALUE),
    RISK_REDUCTION(AccountDimension.DEATH_RISK_SENSITIVITY),
    RESOURCE_SUSTAINABILITY(
            AccountDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY),
    STORABLE_EQUIPMENT(AccountDimension.STORABLE_EQUIPMENT_VALUE),
    GP_LIQUIDITY(AccountDimension.GP_LIQUIDITY_STORAGE_VALUE);

    final AccountDimension dimension;
}

/** Property-level explanation of an infrastructure value assessment. */
@Getter
final class InfraContribution
{
    final InfraBenefit benefit;
    final AccountDimension dimension;
    final Priority accountPriority;
    final Priority milestoneUtility;
    final Priority effectivePriority;

    InfraContribution(InfraBenefit benefit,
            Priority accountPriority,
            Priority milestoneUtility)
    {
        this.benefit = benefit;
        this.dimension = benefit.getDimension();
        this.accountPriority = accountPriority;
        this.milestoneUtility = milestoneUtility;
        this.effectivePriority = Priority.lowerOf(accountPriority,
                milestoneUtility);
    }

}

/** Qualitative inventory change over one repeatable method loop. */
enum InventoryFlow
{
    NEUTRAL,
    CONSUMES_CARRIED_INPUTS,
    GROWS_NONSTACKABLE_OUTPUTS,
    REPLACES_INPUTS_WITH_OUTPUTS
}

/**
 * A semantic item/preparation class from authoritative requirement evidence.
 *
 * <p>Simple name-stable tool families can be evaluated from observed item
 * names. Broader mechanical categories deliberately remain check-only: an
 * item name alone cannot prove that gear is a slash weapon, a safe light
 * source, or a suitable encounter loadout.</p>
 */
@Getter
@RequiredArgsConstructor
enum ItemRequirementClass
{
    AXE("any usable axe", true),
    PICKAXE(get(1139), true),
    BOW(get(1693), true),
    CROSSBOW(get(1140), true),
    CAT_OR_KITTEN("a cat or kitten", true),
    FEATHER(get(1694), true),
    NAILS(get(1141), true),
    MACHETE(get(1142), true),
    LIGHT_SOURCE(get(1143), false),
    SLASH_WEAPON(get(1144), false),
    WEB_CUTTING_TOOL(get(1145), false),
    MAGIC_COMBAT_LOADOUT(get(327), false),
    MAGIC_OR_RANGED_LOADOUT(get(328), false),
    TELEKINETIC_GRAB_RUNES(get(329), false),
    SPELL_RUNE_LOADOUT(get(330), false),
    POISON_CURE(get(1146), false),
    WATER_CONTAINER(get(1147), false),
    EMPTY_INVENTORY_SPACE(get(1148), false),
    COMBAT_EQUIPMENT(get(1149), false),
    HEALING_FOOD(get(1150), false),
    MULTI_STYLE_OR_POISON(get(331), false),
    FULL_HAM_ROBE_SET(get(1151), false);

    final String label;
    final boolean nameObservable;

    public boolean matches(String itemName)
    {
        if (!nameObservable || itemName == null) return false;
        var name = itemName.trim().toLowerCase(Locale.ROOT);
        switch (this)
        {
            case AXE:
                return (name.equals("axe") || name.contains(" axe"))
                        && !name.contains("pickaxe")
                        && !name.contains("battleaxe")
                        && !name.contains("greataxe")
                        && !name.contains(" axe head")
                        && !name.startsWith("broken ");
            case PICKAXE:
                return (name.equals("pickaxe") || name.contains(" pickaxe"))
                        && !name.contains("pickaxe head")
                        && !name.contains("pickaxe handle")
                        && !name.startsWith("broken ");
            case BOW:
                return (name.equals("bow") || name.endsWith(" bow"))
                        && !name.endsWith("crossbow");
            case CROSSBOW:
                return name.equals("crossbow") || name.endsWith(" crossbow");
            case CAT_OR_KITTEN:
                return name.equals("cat") || name.equals("kitten")
                        || name.endsWith(" cat") || name.endsWith(" kitten")
                        || name.endsWith(" hellcat") || name.endsWith(" hellkitten");
            case FEATHER:
                return name.equals("feather") || name.endsWith(" feather");
            case NAILS:
                return name.equals("nails") || name.endsWith(" nails");
            case MACHETE:
                return name.equals("machete") || name.endsWith(" machete");
            default:
                return false;
        }
    }
}

/** One observed item stack; slot is -1 for persisted or synthetic evidence. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
final class ItemState
{
    final int itemId;
    final String name;
    final int quantity;
    final int slotIndex;

    public ItemState(int itemId, String name, int quantity)
    {
        this(itemId, name, quantity, -1);
    }
}

/** One live RuneLite market-price lookup result. */
@Getter
final class MarketPriceQuote
{
    final int itemId;
    final String itemName;
    final int unitPrice;

    public MarketPriceQuote(int itemId, String itemName, int unitPrice)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unitPrice = max(0, unitPrice);
    }


    public boolean hasPrice()
    {
        return itemId > 0 && unitPrice > 0;
    }
}

/**
 * Membership access observed for the currently logged-in RuneScape profile.
 */
@RequiredArgsConstructor
@Getter
enum Membership
{
    F2P("F2P"),
    P2P("P2P"),
    UNKNOWN("Unknown access");

    final String displayName;

    public boolean isFreeToPlay()
    {
        return this == F2P;
    }

    public boolean isMembers()
    {
        return this == P2P;
    }
}

/** Exact material quantity resolved for one planned training segment. */
@Getter
final class MethodInput
{
    final String name;
    final int itemId;
    final int quantity;

    public MethodInput(String name, int itemId, int quantity)
    {
        this.name = name;
        this.itemId = itemId;
        this.quantity = max(0, quantity);
    }

}

/** Plan-relative inventory requirements; deliberately avoids fake precision. */
@RequiredArgsConstructor
final class InventoryFootprint
{
    @Getter
    final int minimumPracticalFreeSlots;
    @Getter
    final int persistentRequiredSlots;
    @Getter
    final int temporarySlots;
    @Getter
    final InventoryFlow flow;
    final boolean tearsDownCurrentSetup;


    public static InventoryFootprint lowPressure()
    {
        return new InventoryFootprint(0, 0, 0,
                InventoryFlow.NEUTRAL, false);
    }

    public boolean tearsDownCurrentSetup() { return tearsDownCurrentSetup; }
}

/**
 * One detected completion event shown briefly in the sidebar.
 */
@Getter
@RequiredArgsConstructor
final class MilestoneCompletion
{
    final String activityId;
    final String title;
    final Skill skill;
    final int startedAtLevel;
    final int targetLevel;

}

/** Canonical local identifiers shared by catalogs, evidence, and planners. */
final class Names
{
    private Names() { }

    static String lower(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String text(String value)
    {
        return lower(value).replaceAll("\\s+", " ");
    }

    static String words(String value)
    {
        return lower(value).replace('\u2019', '\'')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    static String slug(String value)
    {
        return words(value).replace(' ', '-');
    }

    static String actionKey(String value)
    {
        return lower(value).replace('-', '_').replace(' ', '_');
    }

    static String actionText(String value)
    {
        return text(value).replace('_', ' ').replace('-', ' ');
    }
}

@Getter
final class ObservedFarmingPatchState
{
    final PatchState state;
    final long observedAtMillis;

    public ObservedFarmingPatchState(
            PatchState state,
            long observedAtMillis)
    {
        this.state = state == null ? PatchState.UNKNOWN : state;
        this.observedAtMillis = observedAtMillis;
    }

}

@Getter
final class Opportunity
{
    final String id;
    final OpportunityType type;
    final String title;
    final boolean ready;
    final Confidence confidence;
    final List<String> preparation;
    final boolean setupVerified;
    final Safety safetyEvidence;

    public Opportunity(
            String id,
            OpportunityType type,
            String title,
            boolean ready,
            Confidence confidence,
            List<String> preparation)
    {
        this(id, type, title, ready, confidence, preparation, false,
                Safety.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            Confidence confidence, List<String> preparation,
            boolean setupVerified)
    {
        this(id, type, title, ready, confidence, preparation, setupVerified,
                Safety.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            Confidence confidence, List<String> preparation,
            boolean setupVerified, Safety safetyEvidence)
    {
        this.id = id;
        this.type = type;
        this.title = title;
        this.ready = ready;
        this.confidence = confidence;
        this.preparation = unmodifiableList(
                preparation == null ? new ArrayList<>() : new ArrayList<>(preparation)
        );
        this.setupVerified = setupVerified;
        this.safetyEvidence = safetyEvidence == null
                ? Safety.unknown() : safetyEvidence;
    }

}

enum OpportunityType
{
    BIRDHOUSE_RUN,
    HERB_RUN,
    TREE_RUN,
    FARMING_CONTRACT,
    TEARS_OF_GUTHIX,
    KINGDOM,
    BATTLESTAVES,
    DYNAMITE,
    DAILY_DIARY_REWARD,
    KINGDOM_APPROVAL,
    CLUE,
    OTHER_COOLDOWN
}

/** Independent overlay preferences; neither affects sidebar planning. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class OverlayDisplayState
{
    final boolean details;
    final boolean methodGuidance;


    static OverlayDisplayState from(OsrsStrategistConfig config)
    {
        return new OverlayDisplayState(config != null
                && config.showDetailsOverlay(), config != null
                && config.showInGameGuidance());
    }

    boolean showsDetails() { return details; }
    boolean showsMethodGuidance() { return methodGuidance; }
    boolean showsMethodGuidance(boolean detailsVisible)
    {
        return methodGuidance && !detailsVisible;
    }
}

/** Idempotence guard for sidebar overlay registration and removal. */
final class OverlayLifecycleGuard
{
    @Getter private boolean registered;

    boolean beginRegistration()
    {
        if (registered) return false;
        registered = true;
        return true;
    }

    boolean beginRemoval()
    {
        if (!registered) return false;
        registered = false;
        return true;
    }

}

/** Observable completion rule for a strategic plan step. */
@Getter
final class CompletionRule
{
    public enum Kind
    {
        SKILL_LEVEL,
        QUEST_COMPLETE,
        NONE
    }

    final Kind kind;
    final Skill skill;
    final int level;
    final String quest;

    private CompletionRule(
            Kind kind, Skill skill, int level, String quest)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = max(0, level);
        this.quest = quest;
    }

    public static CompletionRule skillLevel(Skill skill, int level)
    {
        if (skill == null || level < 1)
            throw new IllegalArgumentException(get(1328));
        return new CompletionRule(
                Kind.SKILL_LEVEL, skill, level, null);
    }

    public static CompletionRule questComplete(String quest)
    {
        if (quest == null || quest.trim().isEmpty())
            throw new IllegalArgumentException(get(1329));
        return new CompletionRule(
                Kind.QUEST_COMPLETE, null, 0, quest.trim());
    }

    public static CompletionRule none()
    {
        return new CompletionRule(Kind.NONE, null, 0, null);
    }

    public boolean isComplete(GameData data)
    {
        if (data == null) return false;
        if (kind == Kind.SKILL_LEVEL)
            return data.account() != null
                    && data.account().level(skill) >= level;
        if (kind == Kind.QUEST_COMPLETE)
            return data.quests() != null
                    && data.quests().statusOf(quest) == QuestStatus.COMPLETE;
        return false;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof CompletionRule)) return false;
        var that = (CompletionRule) other;
        return level == that.level && kind == that.kind
                && skill == that.skill && Objects.equals(quest, that.quest);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(kind, skill, level, quest);
    }
}

/**
 * Goals that are complete enough to expose in RuneLite configuration.
 *
 * <p>Internal planning goals remain in {@link GoalType}; keeping this list
 * separate prevents experimental enum values from silently becoming public
 * controls.</p>
 */
@RequiredArgsConstructor
enum PlayerGoal
{
    AUTOMATIC(GoalType.AUTOMATIC, "Automatic"),
    BARROWS_GLOVES(GoalType.BARROWS_GLOVES, "Barrows gloves"),
    FIRE_CAPE(GoalType.FIRE_CAPE, "Fire cape"),
    QUEST_CAPE(GoalType.QUEST_CAPE, "Quest cape"),
    PRIFDDINAS(GoalType.PRIFDDINAS, "Prifddinas"),
    BOWFA(GoalType.BOWFA, "Bowfa"),
    INFERNAL_CAPE(GoalType.INFERNAL_CAPE, "Infernal cape"),
    MAX(GoalType.MAX, "Max cape");

    final GoalType planningGoal;
    final String displayName;
    public GoalType toPlanningGoal()
    {
        return planningGoal;
    }

    public static boolean isPlayerFacing(GoalType goal)
    {
        if (goal == null) return false;
        for (PlayerGoal candidate : values())
            if (candidate.planningGoal == goal) return true;
        return false;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}

/** Static safety and presentation allow/deny lists bundled for review. */
final class PolicyLists
{
    static final PolicyLists DATA = BundledCatalogLoader.array(
            get(1908), PolicyLists[].class)[0];
    String[] one_defence_safe;
    String[] level_three_safe;
    String[] prayer_skiller_extra;
    String[] free_to_play_quests;
    String[] generic_titles;
    String[] generic_actions;
    String[] generic_locations;
    String[] unresolved_supplies;

    static Set<String> normalizedSet(String[] values)
    {
        Set<String> result = new HashSet<>();
        if (values != null)
            for (String value : values) result.add(normalize(value));
        return unmodifiableSet(result);
    }

    static List<String> list(String[] values)
    {
        return unmodifiableList(Arrays.asList(values));
    }

    static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("\\s+", " ");
    }
}

/** Compact persisted recap; raw XP events are intentionally not retained. */
@Getter
final class ProgressSessionSummary
{
    private static final int MAX_MILESTONES = 100;
    final long startedAtMillis;
    final long endedAtMillis;
    final long activeDurationMillis;
    final long totalXpGained;
    final int levelsGained;
    final Map<Skill, Integer> xpBySkill;
    final List<ProgressMilestone> milestones;

    public ProgressSessionSummary(ProgressSessionSnapshot snapshot)
    {
        this(snapshot == null ? 0L : snapshot.startedAtMillis,
                snapshot == null ? 0L : snapshot.getUpdatedAtMillis(),
                snapshot == null ? 0L : snapshot.activeDurationMillis,
                snapshot == null ? 0L : snapshot.getTotalXpGained(),
                snapshot == null ? 0 : snapshot.getLevelsGained(),
                gains(snapshot), snapshot == null
                        ? emptyList() : snapshot.milestones);
    }

    ProgressSessionSummary(
            long startedAtMillis,
            long endedAtMillis,
            long activeDurationMillis,
            long totalXpGained,
            int levelsGained,
            Map<Skill, Integer> xpBySkill)
    {
        this(startedAtMillis, endedAtMillis, activeDurationMillis,
                totalXpGained, levelsGained, xpBySkill,
                emptyList());
    }

    ProgressSessionSummary(
            long startedAtMillis,
            long endedAtMillis,
            long activeDurationMillis,
            long totalXpGained,
            int levelsGained,
            Map<Skill, Integer> xpBySkill,
            List<ProgressMilestone> milestones)
    {
        this.startedAtMillis = max(0L, startedAtMillis);
        this.endedAtMillis = max(this.startedAtMillis, endedAtMillis);
        this.activeDurationMillis = max(0L,
                min(activeDurationMillis,
                        this.endedAtMillis - this.startedAtMillis));
        this.totalXpGained = max(0L, totalXpGained);
        this.levelsGained = max(0, levelsGained);
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null)
            for (Map.Entry<Skill, Integer> entry : xpBySkill.entrySet())
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getValue() > 0)
                    copy.put(entry.getKey(), entry.getValue());
        this.xpBySkill = unmodifiableMap(copy);
        List<ProgressMilestone> milestoneCopy = new ArrayList<>(
                milestones == null ? emptyList() : milestones);
        while (milestoneCopy.size() > MAX_MILESTONES)
            milestoneCopy.remove(0);
        this.milestones = unmodifiableList(milestoneCopy);
    }

    private static Map<Skill, Integer> gains(ProgressSessionSnapshot snapshot)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        if (snapshot != null)
            for (SkillSessionProgress progress : snapshot.getSkills().values())
                if (progress.getXpGained() > 0)
                    result.put(progress.getSkill(), progress.getXpGained());
        return result;
    }

}

/** Compact event-driven XP aggregation used by charts and persistence. */
@Getter
final class ProgressTimeBucket
{
    final long startedAtMillis;
    final Map<Skill, Integer> xpBySkill;

    ProgressTimeBucket(long startedAtMillis, Map<Skill, Integer> xpBySkill)
    {
        this.startedAtMillis = startedAtMillis;
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null) copy.putAll(xpBySkill);
        this.xpBySkill = unmodifiableMap(copy);
    }

    public int getTotalXp()
    {
        var total = 0L;
        for (Integer value : xpBySkill.values())
            total += value == null ? 0 : max(0, value);
        return (int) min(Integer.MAX_VALUE, total);
    }
}

/** Converts an exact Main-account material shortfall into live GE cost advice. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class PurchaseCostAdvisor
{
    final MarketPriceService marketPriceService;
    /**
     * Returns an optional cost sentence. If any item price is unresolved, the
     * caller should still show exact quantities but omit a fake total GP value.
     */
    public String advice(
            AccountEconomySnapshot economy,
            List<MethodInput> missing)
    {
        var estimate = estimate(missing);
        if (!estimate.isComplete() || estimate.totalCost <= 0) return null;
        var total = estimate.totalCost;

        var text = new StringBuilder();
        text.append(get(412))
                .append(format(total))
                .append(" coins total.");

        if (economy != null
                && economy.confidence == Confidence.VERIFIED)
        {
            var cash = economy.coins;
            if (cash >= total)
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(get(413))
                        .append(format(cash - total))
                        .append(" after the buy.");
            }
            else
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(get(414))
                        .append(format(total - cash))
                        .append(get(415));
            }
        }
        else
        {
            text.append(get(416));
        }
        return text.toString();
    }

    /**
     * Resolves every exact-name quote or fails the aggregate closed. A partial
     * price list must never make an entire method appear cheaper than it is.
     */
    public PurchaseCostEstimate estimate(List<MethodInput> missing)
    {
        if (marketPriceService == null || missing == null || missing.isEmpty())
            return PurchaseCostEstimate.unknown();

        var total = 0L;
        var sawInput = false;
        for (MethodInput input : missing)
        {
            if (input == null || input.quantity <= 0) continue;
            sawInput = true;
            var quote = marketPriceService.quote(input.getName());
            if (quote == null || !quote.hasPrice())
                return PurchaseCostEstimate.unknown();
            total = safeAdd(total, safeMultiply(
                    quote.getUnitPrice(), input.quantity));
        }
        return sawInput && total > 0
                ? new PurchaseCostEstimate(true, total)
                : PurchaseCostEstimate.unknown();
    }

    private static long safeMultiply(long a, long b)
    {
        if (a <= 0 || b <= 0) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    private static long safeAdd(long a, long b)
    {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}

/** Exact aggregate market-price evidence for a deterministic material list. */
@Getter
final class PurchaseCostEstimate
{
    final boolean complete;
    final long totalCost;

    public PurchaseCostEstimate(boolean complete, long totalCost)
    {
        this.complete = complete;
        this.totalCost = max(0L, totalCost);
    }


    public static PurchaseCostEstimate unknown()
    {
        return new PurchaseCostEstimate(false, 0L);
    }
}

@Getter
final class PvmReadiness
{
    final String activityId;
    final boolean realisticallyReady;
    final Confidence confidence;
    final List<String> missingRequirements;

    public PvmReadiness(
            String activityId,
            boolean realisticallyReady,
            Confidence confidence,
            List<String> missingRequirements)
    {
        this.activityId = activityId;
        this.realisticallyReady = realisticallyReady;
        this.confidence = confidence;
        this.missingRequirements = unmodifiableList(
                new ArrayList<>(missingRequirements)
        );
    }

    /** Conservative beta contract: observed carried setup is ready to attempt. */
    public boolean isReadyForRecommendation()
    {
        return realisticallyReady && confidence == Confidence.VERIFIED;
    }


}

enum QuestStatus
{
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETE,
    UNKNOWN
}

enum QuestTolerance
{
    LOW,
    NORMAL,
    HIGH
}

/** Bounded local history so learning cannot grow profile config forever. */
final class RecommendationHistory
{
    private static final int MAX_ENTRIES = 200;
    final List<RecommendationHistoryEntry> entries = new ArrayList<>();

    public void add(
            String activityId,
            String title,
            RecommendationHistoryAction action)
    {
        if (activityId == null || action == null) return;
        entries.add(new RecommendationHistoryEntry(
                activityId, title, action, System.currentTimeMillis()));
        trim();
    }

    public void replaceAll(List<RecommendationHistoryEntry> values)
    {
        entries.clear();
        if (values != null) entries.addAll(values);
        trim();
    }

    public void clear()
    {
        entries.clear();
    }

    public List<RecommendationHistoryEntry> snapshot()
    {
        return unmodifiableList(new ArrayList<>(entries));
    }

    private void trim()
    {
        while (entries.size() > MAX_ENTRIES)
        {
            entries.remove(0);
        }
    }
}

enum RecommendationHistoryAction
{
    LATER,
    NOT_TODAY,
    DISLIKE,
    COMPLETED
}

final class RecommendationHistoryDocument
{
    final int schemaVersion;
    final List<RecommendationHistoryEntry> entries;

    RecommendationHistoryDocument(List<RecommendationHistoryEntry> entries)
    {
        this.schemaVersion = 1;
        this.entries = entries == null
                ? emptyList()
                : new ArrayList<>(entries);
    }
    List<RecommendationHistoryEntry> getEntries()
    {
        return entries == null ? emptyList() : entries;
    }
}

/** Prominent, typed warning for an unusual player-visible dangerous action. */
@Getter
@RequiredArgsConstructor
final class RecommendationRiskDisclosure
{
    final String heading;
    final String message;
    final boolean acknowledgementRequired;


    public static RecommendationRiskDisclosure deathStorage()
    {
        return new RecommendationRiskDisclosure("HIGH RISK",
                get(702),
                true);
    }

}

/** Keeps a still-valid plan steady across low-signal account refreshes. */
final class RecommendationStabilizer
{
    private static final double MAX_SCORE_DEFICIT = 5.0;
    final ActionabilityPolicy actionabilityPolicy =
            new ActionabilityPolicy();

    StrategyResult stabilize(List<Recommendation> previous, StrategyResult fresh)
    {
        if (fresh == null || fresh.recommendations.isEmpty()
                || previous == null || previous.isEmpty()) return fresh;

        var oldTop = previous.get(0);
        if (oldTop == null || FallbackRecommendationFactory.isFallback(oldTop))
            return fresh;

        var current = fresh.recommendations;
        Recommendation stillValid = null;
        for (Recommendation candidate : current)
        {
            if (sameCheckpoint(oldTop, candidate))
            {
                stillValid = candidate;
                break;
            }
        }
        // A formerly executable plan may remain in the fresh queue as a
        // secondary preparation card after an item, quest, membership, or
        // access change. Never promote that alternative back into DO NEXT.
        if (stillValid == null
                || !actionabilityPolicy.canLeadQueue(stillValid)
                || stillValid == current.get(0)) return fresh;
        if (current.get(0).score - stillValid.score
                > MAX_SCORE_DEFICIT) return fresh;

        List<Recommendation> stable = new ArrayList<>(current.size());
        stable.add(stillValid);
        for (Recommendation candidate : current)
            if (candidate != stillValid) stable.add(candidate);
        return new StrategyResult(stable, fresh.getOpportunities(),
                fresh.getPlan());
    }

    private static boolean sameCheckpoint(
            Recommendation previous, Recommendation current)
    {
        if (previous == null || current == null
                || previous.id == null
                || !previous.id.equals(current.id)) return false;
        return previous.targetLevel == current.targetLevel
                && safe(previous.title).equals(safe(current.title));
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}

/**
 * One human-readable readiness check shown by the recommendation UI.
 *
 * <p>The evidence text explains why Compass reached the state. This makes
 * "Check Needed" actionable instead of a vague warning.</p>
 */
@Getter
final class EvidenceCheck
{
    final String id;
    final String label;
    final RequirementState state;
    final String evidence;

    public EvidenceCheck(
            String id,
            String label,
            RequirementState state,
            String evidence)
    {
        this.id = id;
        this.label = label;
        this.state = state == null
                ? RequirementState.CHECK_NEEDED
                : state;
        this.evidence = evidence;
    }

}

/** One deduplicated result node in traversal order. */
@Getter
final class ResolvedDependencyNode
{
    final String id;
    final String action;
    final Confidence confidence;
    final int depth;
    final int requiredQuantity;

    public ResolvedDependencyNode(String id, String action,
            Confidence confidence, int depth)
    {
        this(id, action, confidence, depth, 0);
    }

    public ResolvedDependencyNode(String id, String action,
            Confidence confidence, int depth,
            int requiredQuantity)
    {
        this.id = id;
        this.action = action;
        this.confidence = confidence;
        this.depth = depth;
        this.requiredQuantity = max(0, requiredQuantity);
    }

}

/**
 * One item requirement for an activity, training method, gear upgrade, or
 * preparation checklist.
 */
@Getter
final class ResourceNeed
{
    final int itemId;
    final String itemName;
    final int quantity;

    public ResourceNeed(int itemId, String itemName, int quantity)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = max(1, quantity);
    }

}

@Getter
@RequiredArgsConstructor
final class RestrictedBuildSuggestion
{
    final BuildType type;
    final Confidence confidence;
    final String evidence;


}

/**
 * Optional account-build restrictions layered on top of the Jagex account mode.
 *
 * <p>These describe player-imposed stat restrictions. They are deliberately
 * separate from Main/Ironman/UIM/Hardcore/GIM because any of those account
 * modes can also be a pure or skiller.</p>
 */
enum BuildType
{
    STANDARD,
    SKILLER,
    PRAYER_SKILLER,
    F2P_SKILLER,
    ONE_DEFENCE_PURE,
    LOW_DEFENCE_PURE,
    INITIATE_PURE,
    RUNE_PURE,
    VOID_PURE,
    ZERKER,
    OBSIDIAN_MAULER,
    RANGE_TANK,
    MED_BUILD,
    DEFENCE_PURE,
    TEN_HITPOINTS,
    COMBAT_ONLY
}

/**
 * Severity of a recommendation that could cost meaningful resources or be hard
 * to reverse.
 */
enum RiskLevel
{
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    IRREVERSIBLE
}

@RequiredArgsConstructor
enum SessionIntent
{
    QUICK_20_MIN("Quick session"),
    ONE_HOUR("~1 hour"),
    LONG_SESSION("Long session"),
    AFK("AFK"),
    PICK_FOR_ME("Pick for me");

    final String displayName;
    @Override
    public String toString()
    {
        return displayName;
    }
}

/** User-selectable scaling limited to the Compass sidebar. */
@RequiredArgsConstructor
@Getter
enum SidebarTextSize
{
    STANDARD("Standard", 1.00f),
    LARGE("Large", 1.12f),
    EXTRA_LARGE("Extra large", 1.24f);

    final String displayName;
    final float scale;

    @Override
    public String toString() { return displayName; }
}

/** One actual level target and the typed reason it matters. */
@Getter
final class SkillBreakpoint
{
    public enum Kind
    {
        GOAL_REQUIREMENT,
        INFRASTRUCTURE_UNLOCK,
        ABILITY_UNLOCK,
        TRAINING_ACTION_UNLOCK,
        MAX_TARGET,
        NEXT_LEVEL_FALLBACK
    }

    final Skill skill;
    final int level;
    final String label;
    final Kind kind;
    final String evidenceId;

    public SkillBreakpoint(Skill skill, int level, String label,
            Kind kind, String evidenceId)
    {
        if (skill == null || level < 2 || label == null
                || label.trim().isEmpty() || kind == null)
            throw new IllegalArgumentException(get(1179));
        this.skill = skill;
        this.level = level;
        this.label = label.trim();
        this.kind = kind;
        this.evidenceId = evidenceId == null ? "" : evidenceId;
    }


    public double strategicValue()
    {
        switch (kind)
        {
            case GOAL_REQUIREMENT: return 1.0;
            case INFRASTRUCTURE_UNLOCK: return 0.8;
            case ABILITY_UNLOCK: return 0.65;
            case TRAINING_ACTION_UNLOCK: return 0.35;
            case MAX_TARGET: return 0.45;
            case NEXT_LEVEL_FALLBACK:
            default: return 0.05;
        }
    }
}

/** Immutable per-skill progress for the current client session. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SkillSessionProgress
{
    final Skill skill;
    final int startingXp;
    final int currentXp;
    final int startingLevel;
    final int currentLevel;
    final XpRateEstimate rate;


    public int getXpGained() { return max(0, currentXp - startingXp); }
    public int getLevelsGained()
    {
        return max(0, currentLevel - startingLevel);
    }
}

/** XP multiplier Compass can safely plan around because the required set is observed. */
@Getter
final class SkillingXpModifier
{
    final double multiplier;
    final String label;

    public SkillingXpModifier(double multiplier, String label)
    {
        this.multiplier = multiplier <= 0 ? 1.0 : multiplier;
        this.label = label;
    }


    public static SkillingXpModifier none()
    {
        return new SkillingXpModifier(1.0, null);
    }
}

/** Deterministic Slayer point/streak rules shared by master and task decisions. */
final class SlayerPointEconomy
{
    public static final int SKIP_COST = 30;

    private SlayerPointEconomy() {}

    public static boolean isBonusCompletion(int completedAfterTask)
    {
        return completedAfterTask > 0 && (completedAfterTask % 1_000 == 0
                || completedAfterTask % 250 == 0
                || completedAfterTask % 100 == 0
                || completedAfterTask % 50 == 0
                || completedAfterTask % 10 == 0);
    }

    public static int pointMultiplier(int completedAfterTask)
    {
        if (completedAfterTask < 5) return 0;
        if (completedAfterTask > 0 && completedAfterTask % 1_000 == 0) return 50;
        if (completedAfterTask > 0 && completedAfterTask % 250 == 0) return 35;
        if (completedAfterTask > 0 && completedAfterTask % 100 == 0) return 25;
        if (completedAfterTask > 0 && completedAfterTask % 50 == 0) return 15;
        if (completedAfterTask > 0 && completedAfterTask % 10 == 0) return 5;
        return 1;
    }

    public static int blockCapacity(int questPoints, boolean lumbridgeElite)
    {
        var ordinary = min(6, max(0, questPoints) / 50);
        return ordinary + (lumbridgeElite ? 1 : 0);
    }

    /** Avoids spending the account's final cancellation on an ordinary dislike. */
    public static boolean hasSustainableSkipBalance(int points)
    {
        return hasSustainableSkipBalance(points, SKIP_COST);
    }

    /** Retains one further cancellation at the current master's actual cost. */
    public static boolean hasSustainableSkipBalance(int points, int cancelCost)
    {
        var cost = max(1, cancelCost);
        return points >= cost * 2;
    }
}

/** Reviewed, strategically meaningful Slayer rewards with live ownership varbits. */
@RequiredArgsConstructor
@Getter
enum SlayerReward
{
    BIGGER_AND_BADDER(get(1926), get(1927), 50,
            VarbitID.SLAYER_UNLOCK_SUPERIORMOBS),
    MALEVOLENT_MASQUERADE(get(1928), get(1180), 400,
            VarbitID.SLAYER_HELM_UNLOCKED),
    BROADER_FLETCHING(get(1929), get(1930), 300,
            VarbitID.SLAYER_AMMO_UNLOCKED),
    RING_BLING("ring-bling", "Ring Bling", 150,
            VarbitID.SLAYER_RING_UNLOCKED),
    TASK_STORAGE("task-storage", "Task Storage", 500,
            VarbitID.SLAYER_UNLOCK_STORAGE),
    LIKE_A_BOSS("like-a-boss", "Like a Boss", 200,
            VarbitID.SLAYER_UNLOCK_BOSSES),
    HOT_STUFF("hot-stuff", "Hot Stuff", 100,
            VarbitID.SLAYER_UNLOCK_TZHAAR),
    WATCH_THE_BIRDIE(get(1931), get(1932), 80,
            VarbitID.SLAYER_UNLOCK_AVIANSIES),
    BASILOCKED("basilocked", "Basilocked", 80,
            VarbitID.SLAYER_UNLOCK_BASILISK),
    ACTUAL_VAMPYRE_SLAYER(get(1933), get(1181), 80,
            VarbitID.SLAYER_UNLOCK_VAMPYRES),
    REPTILE_GOT_RIPPED(get(1934), get(1182), 75,
            VarbitID.SLAYER_UNLOCK_LIZARDMEN),
    STOP_THE_WYVERN("stop-the-wyvern", "Stop the Wyvern", 500,
            VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK),
    EXTEND_ABYSSAL_DEMONS(get(1935), get(1936), 100,
            VarbitID.SLAYER_LONGER_ABYSSALDEMONS),
    EXTEND_BLOODVELDS(get(1937), "Bleed Me Dry", 75,
            VarbitID.SLAYER_LONGER_BLOODVELD),
    EXTEND_DUST_DEVILS(get(1938), get(1183), 100,
            VarbitID.SLAYER_LONGER_DUSTDEVILS),
    EXTEND_GARGOYLES(get(1939), "Get Smashed", 100,
            VarbitID.SLAYER_LONGER_GARGOYLES),
    EXTEND_NECHRYAELS(get(1940), "Nechs Please", 100,
            VarbitID.SLAYER_LONGER_NECHRYAEL),
    EXTEND_KRAKEN("extend-kraken", "Krack On", 100,
            VarbitID.SLAYER_LONGER_CAVEKRAKEN);

    final String id;
    final String displayName;
    final int pointCost;
    final int varbitId;
}

/** One live-evidence-backed Slayer reward purchase decision. */
@Getter
@RequiredArgsConstructor
final class SlayerRewardAdvice
{
    final SlayerReward reward;
    final double score;
    final String reason;


}

/** One live Mortimer task/modifier option decoded from RuneLite game data. */
@Getter
final class SlayerTaskOffer
{
    final String taskName;
    final String modifierName;
    final int modifierValue;
    final boolean negativeModifier;

    public SlayerTaskOffer(String taskName, String modifierName,
            int modifierValue, boolean negativeModifier)
    {
        this.taskName = taskName;
        this.modifierName = modifierName;
        this.modifierValue = max(0, modifierValue);
        this.negativeModifier = negativeModifier;
    }

}

/**
 * Storage systems that can materially change an account's viable strategy.
 *
 * <p>These are capabilities, not assumptions. A UIM should only be told to use
 * one of these when the matching {@link StorageSnapshot} entry is VERIFIED.</p>
 */
enum StorageKind
{
    TOOL_LEPRECHAUN,
    STASH,
    LOOTING_BAG,
    POH_COSTUME_ROOM,
    POH_STORAGE,
    /**
     * Legacy observation bucket retained for snapshot compatibility only.
     * It is not specific enough to authorize a storage recommendation.
     */
    DEATH_STORAGE,
    HESPORI_ITEM_RETRIEVAL,
    ZULRAH_ITEM_RETRIEVAL,
    VOLCANIC_MINE_ITEM_RETRIEVAL,
    DEATHPILE,
    SEED_BOX,
    HERB_SACK,
    RUNE_POUCH,
    GROUP_STORAGE
}

/** Qualitative importance; intentionally not a hidden recommendation score. */
enum Priority
{
    NONE,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    public boolean isAtLeast(Priority other)
    {
        return other != null && ordinal() >= other.ordinal();
    }

    public static Priority higherOf(
            Priority left,
            Priority right)
    {
        if (left == null) return right == null ? NONE : right;
        if (right == null) return left;
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public static Priority lowerOf(
            Priority left,
            Priority right)
    {
        if (left == null || right == null) return NONE;
        return left.ordinal() <= right.ordinal() ? left : right;
    }
}

/** Typed, bounded account-value evidence used by the final decision layer. */
@Getter
final class StrategicValue
{
    private static final StrategicValue NEUTRAL = builder().build();
    final double accountModeFit, infrastructureValue, unlockValue,
            travelFit, resourceFit, setupReuse, sharedDependencyValue,
            riskBurden, opportunityCost;
    final List<String> evidenceIds;

    @lombok.Builder(builderClassName = "Builder")
    private StrategicValue(double accountModeFit, double infrastructureValue,
            double unlockValue, double travelFit, double resourceFit,
            double setupReuse, double sharedDependencyValue,
            double riskBurden, double opportunityCost,
            @Singular("evidence") List<String> evidenceIds)
    {
        this.accountModeFit = signed(accountModeFit);
        this.infrastructureValue = unit(infrastructureValue);
        this.unlockValue = unit(unlockValue);
        this.travelFit = signed(travelFit);
        this.resourceFit = signed(resourceFit);
        this.setupReuse = unit(setupReuse);
        this.sharedDependencyValue = unit(sharedDependencyValue);
        this.riskBurden = unit(riskBurden);
        this.opportunityCost = unit(opportunityCost);
        this.evidenceIds = evidenceIds;
    }

    public static StrategicValue neutral() { return NEUTRAL; }

    public StrategicValue merge(StrategicValue other)
    {
        if (other == null || other == NEUTRAL) return this;
        Builder value = builder()
                .accountModeFit(stronger(accountModeFit, other.accountModeFit))
                .infrastructureValue(max(infrastructureValue, other.infrastructureValue))
                .unlockValue(max(unlockValue, other.unlockValue))
                .travelFit(stronger(travelFit, other.travelFit))
                .resourceFit(stronger(resourceFit, other.resourceFit))
                .setupReuse(max(setupReuse, other.setupReuse))
                .sharedDependencyValue(max(sharedDependencyValue, other.sharedDependencyValue))
                .riskBurden(max(riskBurden, other.riskBurden))
                .opportunityCost(max(opportunityCost, other.opportunityCost));
        evidenceIds.forEach(value::evidence);
        other.evidenceIds.forEach(value::evidence);
        return value.build();
    }

    public boolean hasTypedEvidence() { return !evidenceIds.isEmpty(); }

    public double scoreDelta()
    {
        return accountModeFit * 10 + infrastructureValue * 14 + unlockValue * 12
                + travelFit * 7 + resourceFit * 8 + setupReuse * 7
                + sharedDependencyValue * 10 - riskBurden * 18
                - opportunityCost * 12;
    }

    private static double unit(double value)
    {
        return max(0, min(1, value));
    }

    private static double signed(double value)
    {
        return max(-1, min(1, value));
    }

    private static double stronger(double left, double right)
    {
        return abs(left) >= abs(right) ? left : right;
    }
}

/** Reusable CLOG-style reward payload for skill and non-skill completions. */
@Getter
@RequiredArgsConstructor
final class StrategistRewardNotification
{
    final String id;
    final String header;
    final String left;
    final String right;
    final String footerLeft;
    final String footerRight;


    public static StrategistRewardNotification fromMilestone(
            MilestoneCompletion completion)
    {
        if (completion == null) return null;
        return new StrategistRewardNotification(
                completion.activityId,
                get(1707),
                completion.getSkill().getName(),
                completion.getStartedAtLevel() + " → " + completion.targetLevel,
                "Goal complete",
                "Next move ready"
        );
    }

}

/**
 * Central visual language for the Compass sidebar.
 *
 * <p>The goal is get(1706): charcoal surfaces,
 * muted gold accents, and restrained status colors. Keeping colors here avoids
 * turning individual panels into a pile of one-off styling decisions.</p>
 */
final class StrategistTheme
{
    public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    public static final Color CARD_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;

    public static final Color GOLD = new Color(211, 166, 67);
    public static final Color GOLD_SOFT = new Color(181, 142, 63);
    public static final Color TEXT = new Color(220, 220, 220);
    public static final Color MUTED_TEXT = new Color(160, 160, 160);
    public static final Color SUCCESS = new Color(112, 184, 113);
    public static final Color WARNING = new Color(214, 166, 82);
    public static final Color DANGER = new Color(196, 96, 96);
    public static final Color DIVIDER = new Color(67, 67, 67);

    private StrategistTheme()
    {
    }

    public static Border cardBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)
        );
    }

    public static Border highlightedCardBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_SOFT),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)
        );
    }
}

/** Registry for non-skill activities that may compete for the main queue. */
@Singleton
class StrategyCandidateRegistry
{
    @Getter
    final List<CandidateProvider> providers;

    @Inject
    public StrategyCandidateRegistry(
            ClueCandidateProvider clueProvider,
            PvmCandidateProvider pvmProvider,
            QuestCandidateProvider questProvider,
            DiaryCandidateProvider diaryProvider,
            CombatAchievementCandidateProvider combatAchievementProvider,
            InfrastructureCandidateProvider infrastructureProvider,
            GearCandidateProvider gearProvider,
            ProgressionUpgradeCandidateProvider progressionUpgradeProvider,
            ResourceDetourCandidateProvider resourceDetourProvider,
            SlayerCandidateProvider slayerProvider,
            MoneyMakingCandidateProvider moneyProvider,
            MinigameCandidateProvider minigameProvider,
            CollectionLogCandidateProvider collectionLogProvider)
    {
        this.providers = unmodifiableList(
                new ArrayList<>(Arrays.asList(
                        clueProvider,
                        pvmProvider,
                        questProvider,
                        diaryProvider,
                        combatAchievementProvider,
                        infrastructureProvider,
                        progressionUpgradeProvider,
                        resourceDetourProvider,
                        slayerProvider,
                        gearProvider,
                        moneyProvider,
                        minigameProvider,
                        collectionLogProvider))
        );
    }

    StrategyCandidateRegistry(List<CandidateProvider> providers)
    {
        this.providers = unmodifiableList(new ArrayList<>(
                providers == null ? emptyList() : providers));
    }

}

/** Immutable intent/context passed to strategy modules. */
@Getter
@Accessors(fluent = true)
final class StrategyContext extends PlayerStrategyProfile
{
    final GameData data;
    final PreferenceProfile preferenceProfile;
    final AccountMode accountMode;

    public StrategyContext(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            PreferenceProfile preferenceProfile)
    {
        this(data, strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, false, preferenceProfile);
    }

    public StrategyContext(
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
        super(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, allowWildernessMethods);
        this.data = data;
        this.preferenceProfile = preferenceProfile == null ? new PreferenceProfile() : preferenceProfile;
        this.accountMode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().modeCode());
    }

}

/** Evidence hierarchy for strategy candidate generation, not a score bonus. */
enum KnowledgeTier
{
    VERIFIED_ACCOUNT_SPECIFIC,
    VERIFIED_SHARED,
    MECHANICALLY_VERIFIED_FALLBACK,
    SAFE_RECOVERY
}

enum StrategyMode
{
    EFFICIENT,
    BALANCED,
    RELAXED
}

/** Stable keys for development-time strategy sources. */
enum Source
{
    GENERAL_SKILL_TRAINING,
    F2P_SKILL_TRAINING,
    IRONMAN_GENERAL,
    F2P_IRONMAN_GENERAL,
    UIM_GENERAL,
    UIM_ITEM_MANAGEMENT,
    ITEM_RETRIEVAL_SERVICES,
    IRONMAN_SKILL_GUIDES,
    UIM_SKILL_GUIDES,
    IRONMAN_SMITHING,
    UIM_SMITHING,
    IRONMAN_CRAFTING,
    UIM_CRAFTING,
    IRONMAN_HERBLORE,
    UIM_HERBLORE,
    IRONMAN_CONSTRUCTION,
    UIM_CONSTRUCTION,
    IRONMAN_RUNECRAFT,
    UIM_RUNECRAFT,
    IRONMAN_PRAYER,
    UIM_PRAYER,
    IRONMAN_FARMING,
    UIM_FARMING,
    IRONMAN_COOKING,
    UIM_COOKING,
    IRONMAN_FLETCHING,
    UIM_FLETCHING,
    IRONMAN_FISHING,
    UIM_FISHING,
    IRONMAN_MINING,
    UIM_MINING,
    IRONMAN_WOODCUTTING,
    UIM_WOODCUTTING,
    IRONMAN_HUNTER,
    UIM_HUNTER,
    IRONMAN_FIREMAKING,
    UIM_FIREMAKING,
    IRONMAN_THIEVING,
    UIM_THIEVING,
    OPTIMAL_QUEST_GUIDE,
    SLAYER_TRAINING,
    IRONMAN_SLAYER,
    CLUE_STASH,
    POH_STORAGE,
    MINIGAME_GUIDES,
    WINTERTODT,
    GIANTS_FOUNDRY,
    MAHOGANY_HOMES,
    TITHE_FARM,
    SAILING_TRAINING,
    SHIPWRECK_SALVAGING,
    PVM_STRATEGY,
    RUNELITE_MECHANICS
}

/**
 * A temporary soft recommendation adjustment.
 *
 * <p>This is intentionally different from a cooldown. A cooldown hides an
 * activity completely; a timed adjustment merely nudges ranking. Milestone
 * completion uses this to encourage variety without preventing Compass from
 * recommending the same skill again when it is still clearly the best move.</p>
 */
@Getter
@RequiredArgsConstructor
final class TimedScoreAdjustment
{
    final double scoreDelta;
    final long expiresAtMillis;

    public boolean isExpired(long nowMillis)
    {
        return expiresAtMillis <= nowMillis;
    }
}

/** Player-effort profile for a training method. */
enum TrainingIntensity
{
    SWEATY,
    EFFICIENT,
    BALANCED,
    RELAXED,
    AFK
}

/** Strategy/account-mode metadata layered on top of a concrete training method. */
@RequiredArgsConstructor
@Getter
final class TrainingMethodMetadata
{
    final TrainingIntensity intensity;
    final MethodCostTier costTier;
    final RiskLevel riskLevel;
    final boolean freeToPlayAllowed;
    final boolean selfSourceFriendly;
    final boolean uimFriendly;
    final boolean hardcoreSafe;
    final List<String> tags;

    public static TrainingMethodMetadata legacy(TrainingMethod method)
    {
        return new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.LOW,
                method != null && method.wilderness ? RiskLevel.HIGH : RiskLevel.NONE,
                method == null || !method.membersOnly,
                true,
                true,
                method == null || !method.wilderness,
                singletonList("legacy")
        );
    }
}

/** Rejects Swing work queued before a newer account/UI refresh. */
final class UiGenerationGuard
{
    final AtomicLong generation = new AtomicLong();

    long next() { return generation.incrementAndGet(); }
    void invalidate() { generation.incrementAndGet(); }
    boolean isCurrent(long candidate) { return generation.get() == candidate; }
}

/** Exact or partial consumed-input model for one deterministic skill action. */
final class UniversalActionRecipe
{
    @Getter
    final List<MethodInput> inputs;
    @Getter
    final String setup;
    final boolean exactInputs;

    public UniversalActionRecipe(
            List<MethodInput> inputs,
            String setup,
            boolean exactInputs)
    {
        List<MethodInput> copy = new ArrayList<>();
        if (inputs != null) copy.addAll(inputs);
        this.inputs = unmodifiableList(copy);
        this.setup = setup;
        this.exactInputs = exactInputs;
    }

    public static UniversalActionRecipe noConsumedInputs(String setup)
    {
        return new UniversalActionRecipe(emptyList(), setup, true);
    }

    public static UniversalActionRecipe unknown(String setup)
    {
        return new UniversalActionRecipe(emptyList(), setup, false);
    }

    public boolean hasExactInputs()
    {
        return exactInputs;
    }
}

/** A measured XP rate, or an honest indication that evidence is insufficient. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class XpRateEstimate
{
    public enum State
    {
        CALCULATING,
        READY
    }

    final State state;
    final long xpPerHour;
    final int timedIntervals;


    public static XpRateEstimate calculating(int timedIntervals)
    {
        return new XpRateEstimate(State.CALCULATING, 0L,
                max(0, timedIntervals));
    }

    public static XpRateEstimate ready(long xpPerHour, int timedIntervals)
    {
        return new XpRateEstimate(State.READY, max(1L, xpPerHour),
                max(0, timedIntervals));
    }

    public boolean isReady() { return state == State.READY; }
}
