package compass;

import com.google.gson.Gson;
import java.awt.Color;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.Accessors;
import net.runelite.api.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
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
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class AccountAccessMemoryStore
{
    static final String GROUP = Text.get(1609);
    private static final String KEY = "accessMemory";
    private final ConfigManager configManager;
    private final Gson gson;
    private final Map<String, Long> memory = new HashMap<>();
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
    private final Map<String, CapabilityState> states = new HashMap<>();

    public CapabilityState get(String key)
    {
        return states.getOrDefault(key, CapabilityState.UNKNOWN);
    }

    public void set(String key, CapabilityState state)
    {
        states.put(key, state);
    }

    public boolean verified(String key)
    {
        return get(key) == CapabilityState.VERIFIED;
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

    public boolean isUltimateIronman()
    {
        return this == ULTIMATE_IRONMAN;
    }

    public boolean isIronLike()
    {
        return this != MAIN && this != UNKNOWN;
    }
}

@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class AccountReader
{
    private final Client client;

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

        MembershipStatus membershipStatus =
                membershipCredit > 0 || membersWorld
                        ? MembershipStatus.P2P
                        : MembershipStatus.F2P;

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
                return Text.get(1606);

            case 3:
                return Text.get(1607);

            case 4:
                return "Group Ironman";

            case 5:
                return Text.get(1108);

            case 6:
                return Text.get(1109);

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
enum AccountStrategicDimension
{
    INVENTORY_PRESSURE(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    BANK_AVAILABILITY(AccountStrategicDimensionRole.CAPABILITY_GATE),
    GRAND_EXCHANGE_AVAILABILITY(AccountStrategicDimensionRole.CAPABILITY_GATE),
    SELF_SOURCING_BURDEN(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    SHARED_RESOURCE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    SHARED_INFRASTRUCTURE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    STORAGE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    POH_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    TELEPORT_INFRASTRUCTURE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    SETUP_COST_SENSITIVITY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    DEATH_RISK_SENSITIVITY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    CONSUMABLE_REPLACEMENT_DIFFICULTY(
            AccountStrategicDimensionRole.BURDEN_WEIGHT),
    STORABLE_EQUIPMENT_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    DUPLICATE_GRIND_PENALTY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    GP_LIQUIDITY_STORAGE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT);

    private final AccountStrategicDimensionRole role;

    AccountStrategicDimension(AccountStrategicDimensionRole role)
    {
        this.role = role;
    }

    public AccountStrategicDimensionRole getRole() { return role; }
}

/** One explainable account-mode/state contribution. */
@Getter
final class AccountStrategicPriority
{
    private final AccountStrategicDimension dimension;
    private final StrategicPriority priority;
    private final CapabilityState capabilityState;
    private final Confidence confidence;
    private final String reason;

    public AccountStrategicPriority(
            AccountStrategicDimension dimension,
            StrategicPriority priority,
            CapabilityState capabilityState,
            Confidence confidence,
            String reason)
    {
        if (dimension == null) throw new IllegalArgumentException("dimension");
        this.dimension = dimension;
        this.priority = priority == null ? StrategicPriority.NONE : priority;
        this.capabilityState = capabilityState == null
                ? CapabilityState.UNKNOWN : capabilityState;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
        this.reason = reason == null ? "" : reason;
    }

}

/** One action exposed by RuneLite's maintained skill-calculator data. */
@Getter
final class ActionDef
{
    private final Skill skill;
    final String id;
    private final String name;
    private final int level;
    private final float xp;
    private final String category;
    private final MembershipStatus membership;
    private final int itemId;

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership)
    {
        this(skill, id, name, level, xp, category, membership, -1);
    }

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership,
            int itemId)
    {
        this.skill = skill;
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.category = category;
        this.membership = membership == null ? MembershipStatus.UNKNOWN : membership;
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
enum ClueTier
{
    BEGINNER(0.0),
    EASY(1.0),
    MEDIUM(2.0),
    HARD(3.5),
    ELITE(5.0),
    MASTER(7.0),
    UNKNOWN(0.0);

    private final double priorityBonus;

    ClueTier(double priorityBonus)
    {
        this.priorityBonus = priorityBonus;
    }

    public double getPriorityBonus() { return priorityBonus; }

    /**
     * Only beginner Treasure Trails are actionable on a F2P planning profile.
     * Unknown tiers stay eligible so Compass can surface them as Needs Info
     * rather than silently pretending it knows the tier.
     */
    public boolean isAvailableFor(MembershipStatus membershipStatus)
    {
        if (membershipStatus != MembershipStatus.P2P)
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

enum CombatAchievementTier
{
    EASY(41),
    MEDIUM(161),
    HARD(416),
    ELITE(1064),
    MASTER(1904),
    GRANDMASTER(2630);

    private final int rewardPoints;

    CombatAchievementTier(int rewardPoints)
    {
        this.rewardPoints = rewardPoints;
    }

    public int getRewardPoints()
    {
        return rewardPoints;
    }
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
            Skill.ATTACK,
            Skill.STRENGTH,
            Skill.DEFENCE,
            Skill.RANGED,
            Skill.PRAYER,
            Skill.MAGIC,
            Skill.RUNECRAFT,
            Skill.HITPOINTS,
            Skill.CRAFTING,
            Skill.MINING,
            Skill.SMITHING,
            Skill.FISHING,
            Skill.COOKING,
            Skill.FIREMAKING,
            Skill.WOODCUTTING
    );

    private static final Set<String> MEMBERS_ONLY_METHOD_IDS = Set.of(
            "runecraft_gotr",
            "mining_mlm",
            Text.get(1671),
            Text.get(1672),
            Text.get(1673),
            Text.get(1674),
            Text.get(1675),
            Text.get(1676),
            Text.get(1677),
            "farming_tithe",
            "hunter_rumours",
            Text.get(1678),
            Text.get(1679),
            Text.get(1680),
            Text.get(1681)
    );

    private ContentAccessRules()
    {
    }

    public static boolean isSkillAvailable(
            Skill skill,
            MembershipStatus membershipStatus)
    {
        if (skill == null) return false;
        if (membershipStatus == MembershipStatus.P2P) return true;

        // F2P and UNKNOWN both use the F2P skill boundary. UNKNOWN is treated
        // conservatively until RuneLite gives Compass verified membership.
        return FREE_TO_PLAY_SKILLS.contains(skill);
    }

    public static boolean isMethodAvailable(
            TrainingMethod method,
            MembershipStatus membershipStatus)
    {
        if (method == null || !isSkillAvailable(method.getSkill(), membershipStatus))
        {
            return false;
        }
        if (membershipStatus == MembershipStatus.P2P) return true;

        // F2P and UNKNOWN are intentionally identical here. A transient access
        // read may temporarily narrow a member to safe F2P routes, but can never
        // expose a members-only route to an F2P account.
        return !method.isMembersOnly()
                && !MEMBERS_ONLY_METHOD_IDS.contains(method.id);
    }

    public static boolean isFreeToPlaySkill(Skill skill)
    {
        return skill != null && FREE_TO_PLAY_SKILLS.contains(skill);
    }

    /** UNKNOWN receives only records explicitly marked F2P-safe. */
    public static boolean isContentAvailable(
            MembershipStatus membershipStatus,
            boolean freeToPlay)
    {
        return membershipStatus == MembershipStatus.P2P || freeToPlay;
    }

    public static boolean hasVerifiedMembership(MembershipStatus membershipStatus)
    {
        return membershipStatus == MembershipStatus.P2P;
    }
}

/** A concrete method paired with the strategy metadata needed to rank it safely. */
@Getter
@RequiredArgsConstructor
final class CuratedTrainingMethod
{
    private final TrainingMethod method;

    TrainingMethod method() { return method; }
    private final TrainingMethodMetadata metadata;


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

    public static final class Entry
    {
        final String id;
        private final LocalDate effectiveDate;
        private final Status status;
        private final String behavior;
        private final String source;

        private Entry(String id, LocalDate effectiveDate, Status status,
                String behavior, String source)
        {
            this.id = id;
            this.effectiveDate = effectiveDate;
            this.status = status;
            this.behavior = behavior;
            this.source = source;
        }

        public String getId() { return id; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public Status getStatus() { return status; }
        public String getBehavior() { return behavior; }
        public String getSource() { return source; }
    }

    private static final String OFFICIAL = get(189);
    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
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
                Text.get(1652), VALIDATION_DATE))
            levels.put(Text.get(201), 77);
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1653), VALIDATION_DATE))
            levels.put(Text.get(202), 87);
        LEVELS = Collections.unmodifiableMap(levels);

        Map<String, Float> xp = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1654), VALIDATION_DATE))
        {
            xp.put(Text.get(1655), 112f);
            xp.put(Text.get(1656), 168f);
            xp.put(Text.get(1657), 224f);
            xp.put(Text.get(1658), 280f);
            xp.put(Text.get(1659), 369f);
            xp.put(Text.get(1660), 480f);
            xp.put(Text.get(1661), 612f);
            xp.put(Text.get(1662), 969f);
            xp.put(Text.get(1663), 1200f);
        }
        XP = Collections.unmodifiableMap(xp);

        Set<String> stale = new LinkedHashSet<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1664), VALIDATION_DATE))
        {
            stale.add(Text.get(203));
            stale.add(Text.get(204));
        }
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1665), VALIDATION_DATE))
        {
            stale.add(Text.get(1666));
            stale.add(Text.get(1667));
            stale.add(Text.get(1668));
            stale.add(Text.get(1669));
            stale.add(Text.get(1670));
        }
        UNSAFE_STALE_XP = Collections.unmodifiableSet(stale);
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

enum FarmingPatchCycleState
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
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
class FarmingRunStateStore
{
    static final String GROUP = Text.get(1609);
    private static final String KEY = Text.get(1705);
    private final ConfigManager configManager;
    private final Gson gson;
    private final Map<String, ObservedFarmingPatchState> states = new HashMap<>();
    private String loadedProfileKey;

    public synchronized FarmingRunSnapshot snapshot()
    {
        syncProfile();
        return new FarmingRunSnapshot(states);
    }

    public synchronized boolean remember(
            String patchId,
            FarmingPatchCycleState state)
    {
        if (patchId == null || state == null || state == FarmingPatchCycleState.UNKNOWN)
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

        var evidence = recommendation.getSafetyEvidence();
        var guidance = recommendation.getGuidance();
        if (plan != null)
        {
            var method = plan.method();
            var current = recommendation.getCurrentLevel();
            var stageTarget = recommendation.getCurrentExecutionTargetLevel();
            boolean invalid = method == null
                    || blank(method.getName())
                    || current <= 0
                    || !method.supportsLevel(current)
                    || stageTarget <= current
                    || recommendation.getTargetLevel() > 0
                        && stageTarget > recommendation.getTargetLevel()
                    || context != null && context.data() != null
                        && context.data().account() != null
                        && !ContentAccessRules.isMethodAvailable(method,
                                context.data().account()
                                        .membership())
                    || guidance == null
                    || blank(guidance.getAction())
                    || blank(guidance.getLocation());
            if (invalid) evidence = evidence.withInvalidCurrentExecution();
        }
        if (profile != null && profile.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                || guidance != null && guidance.getBankingBehavior()
                        == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP)
        {
            evidence = evidence.requiringConventionalBank();
        }
        if (guidance != null && guidance.getStorageCapability() != null)
        {
            var capability = guidance.getStorageCapability();
            var decision = guidance.getStorageDecision();
            boolean storageUnverified = decision == null
                    || !decision.isAllowed()
                    || decision.getConfidence()
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
    private final AccountSnapshot account;
    private final ItemsState inventory;
    private final ItemsState bank;
    private final ItemsState equipment;
    private final QuestSnapshot quests;
    private final DiarySnapshot diaries;
    private final ClueSnapshot clue;
    private final CombatAchievementSnapshot combatAchievements;
    private final CollectionLogSnapshot collectionLog;
    private final AccountEconomySnapshot economy;
    private final AccountCapabilities capabilities;
    private final AccessMemorySnapshot accessMemory;
    private final FarmingRunSnapshot farmingRuns;
    private final StorageSnapshot storage;
    private final TransportSnapshot transport;
    private final PohSnapshot poh;
    private final ItemsState groupStorage;
    private final SlayerSnapshot slayer;
    private final FarmingSnapshot farming;
    private final SailingSnapshot sailing;
    private final MinigameSnapshot minigames;
    private final PvmSnapshot pvm;
    private final RecurringOpportunitySnapshot recurringOpportunities;
    private final CombatEvidenceSnapshot combatEvidence;

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
                Collections.singletonList(Text.get(1198)));
        roots.put(GoalType.PRIFDDINAS,
                Collections.singletonList(Text.get(1721)));
        roots.put(GoalType.BOWFA,
                Collections.singletonList(Text.get(1721)));
        QUEST_ROOTS = Collections.unmodifiableMap(roots);
    }

    public List<String> questRootsFor(GoalType goal)
    {
        return QUEST_ROOTS.getOrDefault(goal, Collections.emptyList());
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
    private final GoalType goal;
    private final GoalRecommendationRelationship relationship;
    private final String recommendationId;
    private final List<String> path;

    private GoalProvenance(GoalType goal,
            GoalRecommendationRelationship relationship,
            String recommendationId, List<String> path)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || relationship != GoalRecommendationRelationship.DIRECT
                && relationship != GoalRecommendationRelationship.PREREQUISITE
                || recommendationId == null || recommendationId.trim().isEmpty()
                || path == null || path.size() < 2)
        {
            throw new IllegalArgumentException(
                    Text.get(263));
        }
        this.goal = goal;
        this.relationship = relationship;
        this.recommendationId = recommendationId;
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
    }

    public static GoalProvenance direct(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
                GoalRecommendationRelationship.DIRECT,
                recommendationId, path);
    }

    public static GoalProvenance prerequisite(GoalType goal,
            String recommendationId, List<String> path)
    {
        return new GoalProvenance(goal,
                GoalRecommendationRelationship.PREREQUISITE,
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
        if (relationship == GoalRecommendationRelationship.DIRECT)
            return action + Text.get(1226) + goalName + " goal.";
        if (path.size() >= 3)
        {
            var parent = path.get(path.size() - 2);
            return action + Text.get(1708) + parent
                    + Text.get(1227) + goalName + " path.";
        }
        return action + Text.get(1228) + goalName + " goal.";
    }
}

/** Guaranteed XP available from unfinished quests on the selected goal path. */
@Getter
final class GoalQuestRewardForecast
{
    private final Skill skill;
    private final int experience;
    private final List<String> sourceQuests;

    GoalQuestRewardForecast(Skill skill, int experience, List<String> sourceQuests)
    {
        this.skill = skill;
        this.experience = Math.max(0, experience);
        this.sourceQuests = Collections.unmodifiableList(
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
    private final String label;
    private final Set<Integer> acceptableItemIds;
    private final int quantity;
    private final boolean reusable;

    public GroupResourceNeed(String label, Set<Integer> acceptableItemIds,
            int quantity, boolean reusable)
    {
        if (acceptableItemIds == null || acceptableItemIds.isEmpty())
            throw new IllegalArgumentException(
                    Text.get(299));
        this.label = label == null ? "Required item" : label;
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (Integer itemId : acceptableItemIds)
            if (itemId != null && itemId > 0) ids.add(itemId);
        if (ids.isEmpty())
            throw new IllegalArgumentException(
                    Text.get(300));
        this.acceptableItemIds = Collections.unmodifiableSet(ids);
        this.quantity = Math.max(1, quantity);
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
    private final String action;
    private final String supplies;
    private final String location;
    private final String progress;
    private final String note;
    private final MethodBankingBehavior bankingBehavior;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;

    public Guidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this(action, supplies, location, null, note,
                MethodBankingBehavior.UNKNOWN, null, null);
    }

    public Guidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior)
    {
        this(action, supplies, location, null, note, bankingBehavior, null, null);
    }

    public Guidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior,
            UimStorageDecision storageDecision,
            RecommendationRiskDisclosure riskDisclosure)
    {
        this(action, supplies, location, null, note, bankingBehavior,
                storageDecision, riskDisclosure);
    }

    public MethodBankingBehavior getBankingBehavior()
    {
        return bankingBehavior == null ? MethodBankingBehavior.UNKNOWN : bankingBehavior;
    }

    public StorageCapability getStorageCapability()
    {
        return storageDecision == null ? null
                : storageDecision.getCapability();
    }



    public Guidance withBankingBehavior(
            MethodBankingBehavior value)
    {
        return new Guidance(action, supplies, location, progress,
                note,
                value, storageDecision, riskDisclosure);
    }

    public Guidance withStorageDecision(
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure)
    {
        return new Guidance(action, supplies, location, progress,
                note,
                bankingBehavior, decision, disclosure);
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
    private final String activityId;
    private final String title;
    private final String subtitle;
    private final List<GuidanceStep> steps;
    private final String bring;
    private final String where;
    private final String action;
    private final String progress;
    private final String important;

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
        this.steps = Collections.unmodifiableList(new ArrayList<>(
                steps == null ? Collections.emptyList() : steps));
        this.bring = bring;
        this.where = where;
        this.action = action;
        this.progress = progress;
        this.important = important;
    }


    public int completeCount()
    {
        var count = 0;
        for (GuidanceStep step : steps) if (step.isComplete()) count++;
        return count;
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
enum InfrastructureBenefit
{
    INVENTORY_RELIEF(AccountStrategicDimension.INVENTORY_PRESSURE),
    POH_PLATFORM(AccountStrategicDimension.POH_VALUE),
    STORAGE(AccountStrategicDimension.STORAGE_VALUE),
    TRAVEL_NETWORK(AccountStrategicDimension.TELEPORT_INFRASTRUCTURE_VALUE),
    SETUP_REUSE(AccountStrategicDimension.SETUP_COST_SENSITIVITY),
    SELF_SUFFICIENCY(AccountStrategicDimension.SELF_SOURCING_BURDEN),
    SHARED_UTILITY(AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE),
    RISK_REDUCTION(AccountStrategicDimension.DEATH_RISK_SENSITIVITY),
    RESOURCE_SUSTAINABILITY(
            AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY),
    STORABLE_EQUIPMENT(AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE),
    GP_LIQUIDITY(AccountStrategicDimension.GP_LIQUIDITY_STORAGE_VALUE);

    private final AccountStrategicDimension dimension;

    InfrastructureBenefit(AccountStrategicDimension dimension)
    {
        this.dimension = dimension;
    }

    public AccountStrategicDimension getDimension() { return dimension; }
}

/** Property-level explanation of an infrastructure value assessment. */
@Getter
final class InfrastructureValueContribution
{
    private final InfrastructureBenefit benefit;
    private final AccountStrategicDimension dimension;
    private final StrategicPriority accountPriority;
    private final StrategicPriority milestoneUtility;
    private final StrategicPriority effectivePriority;

    InfrastructureValueContribution(InfrastructureBenefit benefit,
            StrategicPriority accountPriority,
            StrategicPriority milestoneUtility)
    {
        this.benefit = benefit;
        this.dimension = benefit.getDimension();
        this.accountPriority = accountPriority;
        this.milestoneUtility = milestoneUtility;
        this.effectivePriority = StrategicPriority.lowerOf(accountPriority,
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

    private final String label;
    private final boolean nameObservable;

    ItemRequirementClass(String label, boolean nameObservable)
    {
        this.label = label;
        this.nameObservable = nameObservable;
    }


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
final class ItemState
{
    private final int itemId;
    private final String name;
    private final int quantity;
    private final int slotIndex;

    public ItemState(int itemId, String name, int quantity)
    {
        this(itemId, name, quantity, -1);
    }

    public ItemState(int itemId, String name, int quantity, int slotIndex)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.slotIndex = slotIndex;
    }
}

/** Verified-price input for comparing a Main's buy-vs-gather options. */
@Getter
final class MainPurchaseCandidate
{
    private final int itemId;
    private final String itemName;
    private final int quantity;
    private final long verifiedUnitPrice;
    private final int estimatedBuyMinutes;
    private final int estimatedSelfSourceMinutes;

    public MainPurchaseCandidate(
            int itemId,
            String itemName,
            int quantity,
            long verifiedUnitPrice,
            int estimatedBuyMinutes,
            int estimatedSelfSourceMinutes)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = Math.max(0, quantity);
        this.verifiedUnitPrice = Math.max(0L, verifiedUnitPrice);
        this.estimatedBuyMinutes = Math.max(0, estimatedBuyMinutes);
        this.estimatedSelfSourceMinutes = Math.max(0, estimatedSelfSourceMinutes);
    }


    public long totalCost()
    {
        try
        {
            return Math.multiplyExact(verifiedUnitPrice, (long) quantity);
        }
        catch (ArithmeticException ex)
        {
            return Long.MAX_VALUE;
        }
    }
}

/** One live RuneLite market-price lookup result. */
@Getter
final class MarketPriceQuote
{
    private final int itemId;
    private final String itemName;
    private final int unitPrice;

    public MarketPriceQuote(int itemId, String itemName, int unitPrice)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.unitPrice = Math.max(0, unitPrice);
    }


    public boolean hasPrice()
    {
        return itemId > 0 && unitPrice > 0;
    }
}

/**
 * Membership access observed for the currently logged-in RuneScape profile.
 */
enum MembershipStatus
{
    F2P("F2P"),
    P2P("P2P"),
    UNKNOWN("Unknown access");

    private final String displayName;

    MembershipStatus(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

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
    private final String name;
    private final int itemId;
    private final int quantity;

    public MethodInput(String name, int itemId, int quantity)
    {
        this.name = name;
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
    }

}

/** Plan-relative inventory requirements; deliberately avoids fake precision. */
@RequiredArgsConstructor
final class MethodInventoryFootprint
{
    @Getter
    private final int minimumPracticalFreeSlots;
    @Getter
    private final int persistentRequiredSlots;
    @Getter
    private final int temporarySlots;
    @Getter
    private final InventoryFlow flow;
    private final boolean tearsDownCurrentSetup;


    public static MethodInventoryFootprint lowPressure()
    {
        return new MethodInventoryFootprint(0, 0, 0,
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
    private final String activityId;
    private final String title;
    private final Skill skill;
    private final int startedAtLevel;
    private final int targetLevel;






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
    private final FarmingPatchCycleState state;
    private final long observedAtMillis;

    public ObservedFarmingPatchState(
            FarmingPatchCycleState state,
            long observedAtMillis)
    {
        this.state = state == null ? FarmingPatchCycleState.UNKNOWN : state;
        this.observedAtMillis = observedAtMillis;
    }

}

@Getter
final class Opportunity
{
    final String id;
    private final OpportunityType type;
    private final String title;
    private final boolean ready;
    private final Confidence confidence;
    private final List<String> preparation;
    private final boolean setupVerified;
    private final SafetyEvidence safetyEvidence;

    public Opportunity(
            String id,
            OpportunityType type,
            String title,
            boolean ready,
            Confidence confidence,
            List<String> preparation)
    {
        this(id, type, title, ready, confidence, preparation, false,
                SafetyEvidence.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            Confidence confidence, List<String> preparation,
            boolean setupVerified)
    {
        this(id, type, title, ready, confidence, preparation, setupVerified,
                SafetyEvidence.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            Confidence confidence, List<String> preparation,
            boolean setupVerified, SafetyEvidence safetyEvidence)
    {
        this.id = id;
        this.type = type;
        this.title = title;
        this.ready = ready;
        this.confidence = confidence;
        this.preparation = Collections.unmodifiableList(
                preparation == null ? new ArrayList<>() : new ArrayList<>(preparation)
        );
        this.setupVerified = setupVerified;
        this.safetyEvidence = safetyEvidence == null
                ? SafetyEvidence.unknown() : safetyEvidence;
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
    private final boolean details;
    private final boolean methodGuidance;


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
    private boolean registered;

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

    boolean isRegistered() { return registered; }
}

/** Observable completion rule for a strategic plan step. */
@Getter
final class PlanCompletionCondition
{
    public enum Kind
    {
        SKILL_LEVEL,
        QUEST_COMPLETE,
        NONE
    }

    private final Kind kind;
    private final Skill skill;
    private final int level;
    private final String quest;

    private PlanCompletionCondition(
            Kind kind, Skill skill, int level, String quest)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = Math.max(0, level);
        this.quest = quest;
    }

    public static PlanCompletionCondition skillLevel(Skill skill, int level)
    {
        if (skill == null || level < 1)
            throw new IllegalArgumentException(Text.get(1328));
        return new PlanCompletionCondition(
                Kind.SKILL_LEVEL, skill, level, null);
    }

    public static PlanCompletionCondition questComplete(String quest)
    {
        if (quest == null || quest.trim().isEmpty())
            throw new IllegalArgumentException(Text.get(1329));
        return new PlanCompletionCondition(
                Kind.QUEST_COMPLETE, null, 0, quest.trim());
    }

    public static PlanCompletionCondition none()
    {
        return new PlanCompletionCondition(Kind.NONE, null, 0, null);
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
        if (!(other instanceof PlanCompletionCondition)) return false;
        var that = (PlanCompletionCondition) other;
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

    private final GoalType planningGoal;
    private final String displayName;

    PlayerGoal(GoalType planningGoal, String displayName)
    {
        this.planningGoal = planningGoal;
        this.displayName = displayName;
    }

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
            Text.get(1908), PolicyLists[].class)[0];
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
        return Collections.unmodifiableSet(result);
    }

    static List<String> list(String[] values)
    {
        return Collections.unmodifiableList(Arrays.asList(values));
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
    private final long startedAtMillis;
    private final long endedAtMillis;
    private final long activeDurationMillis;
    private final long totalXpGained;
    private final int levelsGained;
    private final Map<Skill, Integer> xpBySkill;
    private final List<ProgressMilestone> milestones;

    public ProgressSessionSummary(ProgressSessionSnapshot snapshot)
    {
        this(snapshot == null ? 0L : snapshot.getStartedAtMillis(),
                snapshot == null ? 0L : snapshot.getUpdatedAtMillis(),
                snapshot == null ? 0L : snapshot.getActiveDurationMillis(),
                snapshot == null ? 0L : snapshot.getTotalXpGained(),
                snapshot == null ? 0 : snapshot.getLevelsGained(),
                gains(snapshot), snapshot == null
                        ? Collections.emptyList() : snapshot.getMilestones());
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
                Collections.emptyList());
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
        this.startedAtMillis = Math.max(0L, startedAtMillis);
        this.endedAtMillis = Math.max(this.startedAtMillis, endedAtMillis);
        this.activeDurationMillis = Math.max(0L,
                Math.min(activeDurationMillis,
                        this.endedAtMillis - this.startedAtMillis));
        this.totalXpGained = Math.max(0L, totalXpGained);
        this.levelsGained = Math.max(0, levelsGained);
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null)
            for (Map.Entry<Skill, Integer> entry : xpBySkill.entrySet())
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getValue() > 0)
                    copy.put(entry.getKey(), entry.getValue());
        this.xpBySkill = Collections.unmodifiableMap(copy);
        List<ProgressMilestone> milestoneCopy = new ArrayList<>(
                milestones == null ? Collections.emptyList() : milestones);
        while (milestoneCopy.size() > MAX_MILESTONES)
            milestoneCopy.remove(0);
        this.milestones = Collections.unmodifiableList(milestoneCopy);
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
    private final long startedAtMillis;
    private final Map<Skill, Integer> xpBySkill;

    ProgressTimeBucket(long startedAtMillis, Map<Skill, Integer> xpBySkill)
    {
        this.startedAtMillis = startedAtMillis;
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null) copy.putAll(xpBySkill);
        this.xpBySkill = Collections.unmodifiableMap(copy);
    }

    public int getTotalXp()
    {
        var total = 0L;
        for (Integer value : xpBySkill.values())
            total += value == null ? 0 : Math.max(0, value);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
}

/** Converts an exact Main-account material shortfall into live GE cost advice. */
@Singleton
class PurchaseCostAdvisor
{
    private final MarketPriceService marketPriceService;

    @Inject
    public PurchaseCostAdvisor(MarketPriceService marketPriceService)
    {
        this.marketPriceService = marketPriceService;
    }

    public PurchaseCostAdvisor()
    {
        this(new MarketPriceService());
    }

    /**
     * Returns an optional cost sentence. If any item price is unresolved, the
     * caller should still show exact quantities but omit a fake total GP value.
     */
    public String advice(
            AccountEconomySnapshot economy,
            List<MethodInput> missing)
    {
        var estimate = estimate(missing);
        if (!estimate.isComplete() || estimate.getTotalCost() <= 0) return null;
        var total = estimate.getTotalCost();

        var text = new StringBuilder();
        text.append(Text.get(412))
                .append(format(total))
                .append(" coins total.");

        if (economy != null
                && economy.getConfidence() == Confidence.VERIFIED)
        {
            var cash = economy.getCoins();
            if (cash >= total)
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(Text.get(413))
                        .append(format(cash - total))
                        .append(" after the buy.");
            }
            else
            {
                text.append(" You have ")
                        .append(format(cash))
                        .append(Text.get(414))
                        .append(format(total - cash))
                        .append(Text.get(415));
            }
        }
        else
        {
            text.append(Text.get(416));
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
            if (input == null || input.getQuantity() <= 0) continue;
            sawInput = true;
            var quote = marketPriceService.quote(input.getName());
            if (quote == null || !quote.hasPrice())
                return PurchaseCostEstimate.unknown();
            total = safeAdd(total, safeMultiply(
                    quote.getUnitPrice(), input.getQuantity()));
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
    private final boolean complete;
    private final long totalCost;

    public PurchaseCostEstimate(boolean complete, long totalCost)
    {
        this.complete = complete;
        this.totalCost = Math.max(0L, totalCost);
    }


    public static PurchaseCostEstimate unknown()
    {
        return new PurchaseCostEstimate(false, 0L);
    }
}

@Getter
final class PvmReadiness
{
    private final String activityId;
    private final boolean realisticallyReady;
    private final Confidence confidence;
    private final List<String> missingRequirements;

    public PvmReadiness(
            String activityId,
            boolean realisticallyReady,
            Confidence confidence,
            List<String> missingRequirements)
    {
        this.activityId = activityId;
        this.realisticallyReady = realisticallyReady;
        this.confidence = confidence;
        this.missingRequirements = Collections.unmodifiableList(
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
    private final List<RecommendationHistoryEntry> entries = new ArrayList<>();

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
        return Collections.unmodifiableList(new ArrayList<>(entries));
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
    private final int schemaVersion;
    private final List<RecommendationHistoryEntry> entries;

    RecommendationHistoryDocument(List<RecommendationHistoryEntry> entries)
    {
        this.schemaVersion = 1;
        this.entries = entries == null
                ? Collections.emptyList()
                : new ArrayList<>(entries);
    }

    int getSchemaVersion() { return schemaVersion; }
    List<RecommendationHistoryEntry> getEntries()
    {
        return entries == null ? Collections.emptyList() : entries;
    }
}

/** Prominent, typed warning for an unusual player-visible dangerous action. */
@Getter
@RequiredArgsConstructor
final class RecommendationRiskDisclosure
{
    private final String heading;
    private final String message;
    private final boolean acknowledgementRequired;


    public static RecommendationRiskDisclosure deathStorage()
    {
        return new RecommendationRiskDisclosure("HIGH RISK",
                Text.get(702),
                true);
    }

}

/** Keeps a still-valid plan steady across low-signal account refreshes. */
final class RecommendationStabilizer
{
    private static final double MAX_SCORE_DEFICIT = 5.0;
    private final ActionabilityPolicy actionabilityPolicy =
            new ActionabilityPolicy();

    StrategyResult stabilize(List<Recommendation> previous, StrategyResult fresh)
    {
        if (fresh == null || fresh.getRecommendations().isEmpty()
                || previous == null || previous.isEmpty()) return fresh;

        var oldTop = previous.get(0);
        if (oldTop == null || FallbackRecommendationFactory.isFallback(oldTop))
            return fresh;

        var current = fresh.getRecommendations();
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
        if (current.get(0).getScore() - stillValid.getScore()
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
        return previous.getTargetLevel() == current.getTargetLevel()
                && safe(previous.getTitle()).equals(safe(current.getTitle()));
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
final class RequirementCheck
{
    final String id;
    private final String label;
    private final RequirementState state;
    private final String evidence;

    public RequirementCheck(
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
    private final String action;
    private final Confidence confidence;
    private final int depth;
    private final int requiredQuantity;

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
        this.requiredQuantity = Math.max(0, requiredQuantity);
    }

}

/** Ordered resource route from current ownership to the requested quantity. */
@Getter
final class ResourceAcquisitionChain
{
    private final ResourceNeed need;
    private final int shortfall;
    private final List<ResourceAcquisitionStep> steps;

    public ResourceAcquisitionChain(ResourceNeed need, int shortfall,
            List<ResourceAcquisitionStep> steps)
    {
        this.need = need;
        this.shortfall = Math.max(0, shortfall);
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public ResourceAcquisitionStep nextStep()
    {
        return steps.isEmpty() ? null : steps.get(0);
    }
}

/**
 * One item requirement for an activity, training method, gear upgrade, or
 * preparation checklist.
 */
@Getter
final class ResourceNeed
{
    private final int itemId;
    private final String itemName;
    private final int quantity;

    public ResourceNeed(int itemId, String itemName, int quantity)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = Math.max(1, quantity);
    }

}

@Getter
@RequiredArgsConstructor
final class RestrictedBuildSuggestion
{
    private final RestrictedBuildType type;
    private final Confidence confidence;
    private final String evidence;


}

/**
 * Optional account-build restrictions layered on top of the Jagex account mode.
 *
 * <p>These describe player-imposed stat restrictions. They are deliberately
 * separate from Main/Ironman/UIM/Hardcore/GIM because any of those account
 * modes can also be a pure or skiller.</p>
 */
enum RestrictedBuildType
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

enum SessionIntent
{
    QUICK_20_MIN("Quick session"),
    ONE_HOUR("~1 hour"),
    LONG_SESSION("Long session"),
    AFK("AFK"),
    PICK_FOR_ME("Pick for me");

    private final String displayName;

    SessionIntent(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}

/** User-selectable scaling limited to the Compass sidebar. */
enum SidebarTextSize
{
    STANDARD("Standard", 1.00f),
    LARGE("Large", 1.12f),
    EXTRA_LARGE("Extra large", 1.24f);

    private final String displayName;
    private final float scale;

    SidebarTextSize(String displayName, float scale)
    {
        this.displayName = displayName;
        this.scale = scale;
    }

    float getScale() { return scale; }

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

    private final Skill skill;
    private final int level;
    private final String label;
    private final Kind kind;
    private final String evidenceId;

    public SkillBreakpoint(Skill skill, int level, String label,
            Kind kind, String evidenceId)
    {
        if (skill == null || level < 2 || label == null
                || label.trim().isEmpty() || kind == null)
            throw new IllegalArgumentException(Text.get(1179));
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
    private final Skill skill;
    private final int startingXp;
    private final int currentXp;
    private final int startingLevel;
    private final int currentLevel;
    private final XpRateEstimate rate;


    public int getXpGained() { return Math.max(0, currentXp - startingXp); }
    public int getLevelsGained()
    {
        return Math.max(0, currentLevel - startingLevel);
    }
}

/** XP multiplier Compass can safely plan around because the required set is observed. */
@Getter
final class SkillingXpModifier
{
    private final double multiplier;
    private final String label;

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
        var ordinary = Math.min(6, Math.max(0, questPoints) / 50);
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
        var cost = Math.max(1, cancelCost);
        return points >= cost * 2;
    }
}

/** Reviewed, strategically meaningful Slayer rewards with live ownership varbits. */
enum SlayerReward
{
    BIGGER_AND_BADDER(Text.get(1926), Text.get(1927), 50,
            VarbitID.SLAYER_UNLOCK_SUPERIORMOBS),
    MALEVOLENT_MASQUERADE(Text.get(1928), Text.get(1180), 400,
            VarbitID.SLAYER_HELM_UNLOCKED),
    BROADER_FLETCHING(Text.get(1929), Text.get(1930), 300,
            VarbitID.SLAYER_AMMO_UNLOCKED),
    RING_BLING("ring-bling", "Ring Bling", 150,
            VarbitID.SLAYER_RING_UNLOCKED),
    TASK_STORAGE("task-storage", "Task Storage", 500,
            VarbitID.SLAYER_UNLOCK_STORAGE),
    LIKE_A_BOSS("like-a-boss", "Like a Boss", 200,
            VarbitID.SLAYER_UNLOCK_BOSSES),
    HOT_STUFF("hot-stuff", "Hot Stuff", 100,
            VarbitID.SLAYER_UNLOCK_TZHAAR),
    WATCH_THE_BIRDIE(Text.get(1931), Text.get(1932), 80,
            VarbitID.SLAYER_UNLOCK_AVIANSIES),
    BASILOCKED("basilocked", "Basilocked", 80,
            VarbitID.SLAYER_UNLOCK_BASILISK),
    ACTUAL_VAMPYRE_SLAYER(Text.get(1933), Text.get(1181), 80,
            VarbitID.SLAYER_UNLOCK_VAMPYRES),
    REPTILE_GOT_RIPPED(Text.get(1934), Text.get(1182), 75,
            VarbitID.SLAYER_UNLOCK_LIZARDMEN),
    STOP_THE_WYVERN("stop-the-wyvern", "Stop the Wyvern", 500,
            VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK),
    EXTEND_ABYSSAL_DEMONS(Text.get(1935), Text.get(1936), 100,
            VarbitID.SLAYER_LONGER_ABYSSALDEMONS),
    EXTEND_BLOODVELDS(Text.get(1937), "Bleed Me Dry", 75,
            VarbitID.SLAYER_LONGER_BLOODVELD),
    EXTEND_DUST_DEVILS(Text.get(1938), Text.get(1183), 100,
            VarbitID.SLAYER_LONGER_DUSTDEVILS),
    EXTEND_GARGOYLES(Text.get(1939), "Get Smashed", 100,
            VarbitID.SLAYER_LONGER_GARGOYLES),
    EXTEND_NECHRYAELS(Text.get(1940), "Nechs Please", 100,
            VarbitID.SLAYER_LONGER_NECHRYAEL),
    EXTEND_KRAKEN("extend-kraken", "Krack On", 100,
            VarbitID.SLAYER_LONGER_CAVEKRAKEN);

    final String id;
    private final String displayName;
    private final int pointCost;
    private final int varbitId;

    SlayerReward(String id, String displayName, int pointCost, int varbitId)
    {
        this.id = id;
        this.displayName = displayName;
        this.pointCost = pointCost;
        this.varbitId = varbitId;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getPointCost() { return pointCost; }
    public int getVarbitId() { return varbitId; }
}

/** One live-evidence-backed Slayer reward purchase decision. */
@Getter
@RequiredArgsConstructor
final class SlayerRewardAdvice
{
    private final SlayerReward reward;
    private final double score;
    private final String reason;


}

/** One live Mortimer task/modifier option decoded from RuneLite game data. */
@Getter
final class SlayerTaskOffer
{
    private final String taskName;
    private final String modifierName;
    private final int modifierValue;
    private final boolean negativeModifier;

    public SlayerTaskOffer(String taskName, String modifierName,
            int modifierValue, boolean negativeModifier)
    {
        this.taskName = taskName;
        this.modifierName = modifierName;
        this.modifierValue = Math.max(0, modifierValue);
        this.negativeModifier = negativeModifier;
    }

}

/**
 * Storage systems that can materially change an account's viable strategy.
 *
 * <p>These are capabilities, not assumptions. A UIM should only be told to use
 * one of these when the matching {@link StorageSnapshot} entry is VERIFIED.</p>
 */
enum StorageCapability
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
enum StrategicPriority
{
    NONE,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    public boolean isAtLeast(StrategicPriority other)
    {
        return other != null && ordinal() >= other.ordinal();
    }

    public static StrategicPriority higherOf(
            StrategicPriority left,
            StrategicPriority right)
    {
        if (left == null) return right == null ? NONE : right;
        if (right == null) return left;
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public static StrategicPriority lowerOf(
            StrategicPriority left,
            StrategicPriority right)
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
    private final double accountModeFit, infrastructureValue, unlockValue,
            travelFit, resourceFit, setupReuse, sharedDependencyValue,
            riskBurden, opportunityCost;
    private final List<String> evidenceIds;

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
                .infrastructureValue(Math.max(infrastructureValue, other.infrastructureValue))
                .unlockValue(Math.max(unlockValue, other.unlockValue))
                .travelFit(stronger(travelFit, other.travelFit))
                .resourceFit(stronger(resourceFit, other.resourceFit))
                .setupReuse(Math.max(setupReuse, other.setupReuse))
                .sharedDependencyValue(Math.max(sharedDependencyValue, other.sharedDependencyValue))
                .riskBurden(Math.max(riskBurden, other.riskBurden))
                .opportunityCost(Math.max(opportunityCost, other.opportunityCost));
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
        return Math.max(0, Math.min(1, value));
    }

    private static double signed(double value)
    {
        return Math.max(-1, Math.min(1, value));
    }

    private static double stronger(double left, double right)
    {
        return Math.abs(left) >= Math.abs(right) ? left : right;
    }
}

/** Reusable CLOG-style reward payload for skill and non-skill completions. */
@Getter
@RequiredArgsConstructor
final class StrategistRewardNotification
{
    final String id;
    private final String header;
    private final String left;
    private final String right;
    private final String footerLeft;
    private final String footerRight;


    public static StrategistRewardNotification fromMilestone(
            MilestoneCompletion completion)
    {
        if (completion == null) return null;
        return new StrategistRewardNotification(
                completion.getActivityId(),
                Text.get(1707),
                completion.getSkill().getName(),
                completion.getStartedAtLevel() + " → " + completion.getTargetLevel(),
                "Goal complete",
                "Next move ready"
        );
    }

}

/**
 * Central visual language for the Compass sidebar.
 *
 * <p>The goal is Text.get(1706): charcoal surfaces,
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
    private final List<CandidateProvider> providers;

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
        this.providers = Collections.unmodifiableList(
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

    /** Compatibility constructor for focused tests written before the expanded registry. */
    public StrategyCandidateRegistry(
            ClueCandidateProvider clueProvider,
            PvmCandidateProvider pvmProvider)
    {
        this.providers = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(clueProvider, pvmProvider))
        );
    }

    StrategyCandidateRegistry(List<CandidateProvider> providers)
    {
        this.providers = Collections.unmodifiableList(new ArrayList<>(
                providers == null ? Collections.emptyList() : providers));
    }

}

/** Immutable intent/context passed to strategy modules. */
@Getter
@Accessors(fluent = true)
final class StrategyContext extends PlayerStrategyProfile
{
    private final GameData data;
    private final PreferenceProfile preferenceProfile;
    private final AccountMode accountMode;

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
enum StrategyKnowledgeTier
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
enum StrategySourceId
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
    private final double scoreDelta;
    private final long expiresAtMillis;




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
    private final TrainingIntensity intensity;
    private final MethodCostTier costTier;
    private final RiskLevel riskLevel;
    private final boolean freeToPlayAllowed;
    private final boolean selfSourceFriendly;
    private final boolean uimFriendly;
    private final boolean hardcoreSafe;
    private final List<String> tags;



    public static TrainingMethodMetadata legacy(TrainingMethod method)
    {
        return new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.LOW,
                method != null && method.isWilderness() ? RiskLevel.HIGH : RiskLevel.NONE,
                method == null || !method.isMembersOnly(),
                true,
                true,
                method == null || !method.isWilderness(),
                Collections.singletonList("legacy")
        );
    }
}

/** Rejects Swing work queued before a newer account/UI refresh. */
final class UiGenerationGuard
{
    private final AtomicLong generation = new AtomicLong();

    long next() { return generation.incrementAndGet(); }
    void invalidate() { generation.incrementAndGet(); }
    boolean isCurrent(long candidate) { return generation.get() == candidate; }
}

/** Exact or partial consumed-input model for one deterministic skill action. */
final class UniversalActionRecipe
{
    @Getter
    private final List<MethodInput> inputs;
    @Getter
    private final String setup;
    private final boolean exactInputs;

    public UniversalActionRecipe(
            List<MethodInput> inputs,
            String setup,
            boolean exactInputs)
    {
        List<MethodInput> copy = new ArrayList<>();
        if (inputs != null) copy.addAll(inputs);
        this.inputs = Collections.unmodifiableList(copy);
        this.setup = setup;
        this.exactInputs = exactInputs;
    }

    public static UniversalActionRecipe noConsumedInputs(String setup)
    {
        return new UniversalActionRecipe(Collections.emptyList(), setup, true);
    }

    public static UniversalActionRecipe unknown(String setup)
    {
        return new UniversalActionRecipe(Collections.emptyList(), setup, false);
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

    private final State state;
    private final long xpPerHour;
    private final int timedIntervals;


    public static XpRateEstimate calculating(int timedIntervals)
    {
        return new XpRateEstimate(State.CALCULATING, 0L,
                Math.max(0, timedIntervals));
    }

    public static XpRateEstimate ready(long xpPerHour, int timedIntervals)
    {
        return new XpRateEstimate(State.READY, Math.max(1L, xpPerHour),
                Math.max(0, timedIntervals));
    }

    public boolean isReady() { return state == State.READY; }
}
