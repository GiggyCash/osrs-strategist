package compass;

import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;

/**
 * Per-character memory of places/capabilities Compass has directly observed.
 *
 * <p>The value is the last observed wall-clock time in milliseconds. Remembering
 * positive evidence lets the planner stop repeatedly asking whether an account
 * can reach an area it has already seen the player use.</p>
 */
final class AccessMemorySnapshot
{
    @Getter
    private final Map<String, Long> lastObservedAtMillis;

    public AccessMemorySnapshot(Map<String, Long> values)
    {
        this.lastObservedAtMillis = Collections.unmodifiableMap(
                values == null
                        ? new HashMap<>()
                        : new HashMap<>(values)
        );
    }

    public static AccessMemorySnapshot empty()
    {
        return new AccessMemorySnapshot(Collections.emptyMap());
    }

    public boolean hasObserved(String key)
    {
        return key != null && lastObservedAtMillis.containsKey(key);
    }

    public Long lastObservedAt(String key)
    {
        return lastObservedAtMillis.get(key);
    }

}

@Getter
@RequiredArgsConstructor
final class AccountEconomySnapshot
{
    private final long coins;
    private final long estimatedBankValue;
    private final Confidence confidence;




}

final class AccountSnapshot
{
    @Getter
    private final String playerName;
    private final long accountHash;
    @Getter
    private final int accountTypeCode;
    @Getter
    private final String accountTypeName;
    @Getter
    private final MembershipStatus membershipStatus;
    @Getter
    private final int membershipCredit;
    @Getter
    private final int totalLevel;
    @Getter
    private final long totalExperience;

    @Getter
    private final Map<Skill, Integer> skillLevels;
    @Getter
    private final Map<Skill, Integer> skillExperience;

    /**
     * Compatibility constructor for tests and callers that do not yet supply
     * membership state.
     */
    public AccountSnapshot(
            String playerName,
            int accountTypeCode,
            String accountTypeName,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this(
                playerName,
                0L,
                accountTypeCode,
                accountTypeName,
                MembershipStatus.UNKNOWN,
                0,
                totalLevel,
                totalExperience,
                skillLevels,
                skillExperience
        );
    }

    public AccountSnapshot(
            String playerName,
            int accountTypeCode,
            String accountTypeName,
            MembershipStatus membershipStatus,
            int membershipCredit,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this(playerName, 0L, accountTypeCode, accountTypeName,
                membershipStatus, membershipCredit, totalLevel,
                totalExperience, skillLevels, skillExperience);
    }

    public AccountSnapshot(
            String playerName,
            long accountHash,
            int accountTypeCode,
            String accountTypeName,
            MembershipStatus membershipStatus,
            int membershipCredit,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this.playerName = playerName;
        this.accountHash = accountHash;
        this.accountTypeCode = accountTypeCode;
        this.accountTypeName = accountTypeName;
        this.membershipStatus = membershipStatus == null
                ? MembershipStatus.UNKNOWN
                : membershipStatus;
        this.membershipCredit = membershipCredit;
        this.totalLevel = totalLevel;
        this.totalExperience = totalExperience;

        this.skillLevels = Collections.unmodifiableMap(
                new EnumMap<>(skillLevels)
        );

        this.skillExperience = Collections.unmodifiableMap(
                new EnumMap<>(skillExperience)
        );
    }


    /** Stable local character identity. Zero means RuneLite has not supplied it yet. */
    public long getAccountHash()
    {
        return accountHash;
    }

    public boolean hasStableAccountIdentity()
    {
        return accountHash != 0L;
    }









    public int getSkillLevel(Skill skill)
    {
        return skillLevels.getOrDefault(skill, 1);
    }

    int level(Skill skill) { return getSkillLevel(skill); }
    int xp(Skill skill) { return getSkillExperience(skill); }
    MembershipStatus membership() { return membershipStatus; }
    int modeCode() { return accountTypeCode; }

    public int getSkillExperience(Skill skill)
    {
        return skillExperience.getOrDefault(skill, 0);
    }

    public int getTrackedSkillCount()
    {
        return skillLevels.size();
    }
}

@Getter
final class ClueSnapshot
{
    private final boolean cluePresent;
    private final String clueType;
    private final long firstSeenAtMillis;
    private final Confidence confidence;
    private final ClueStepSnapshot currentStep;

    public ClueSnapshot(
            boolean cluePresent,
            String clueType,
            long firstSeenAtMillis,
            Confidence confidence)
    {
        this(cluePresent, clueType, firstSeenAtMillis, confidence, null);
    }

    public ClueSnapshot(
            boolean cluePresent,
            String clueType,
            long firstSeenAtMillis,
            Confidence confidence,
            ClueStepSnapshot currentStep)
    {
        this.cluePresent = cluePresent;
        this.clueType = clueType;
        this.firstSeenAtMillis = firstSeenAtMillis;
        this.confidence = confidence;
        this.currentStep = currentStep;
    }





    public boolean hasObservedCurrentStep() { return currentStep != null; }
}

/** Exact current-step evidence supplied by RuneLite's Clue Scroll plugin. */
@Getter
final class ClueStepSnapshot
{
    private final String kind;
    private final String action;
    private final String location;
    private final List<String> itemRequirements;
    private final boolean requiresSpade;
    private final boolean requiresLight;
    private final String enemy;
    private final boolean wilderness;
    private final String stashUnit;

    public ClueStepSnapshot(String kind, String action, String location,
            List<String> itemRequirements, boolean requiresSpade,
            boolean requiresLight, String enemy, boolean wilderness,
            String stashUnit)
    {
        this.kind = clean(kind);
        this.action = clean(action);
        this.location = clean(location);
        this.itemRequirements = Collections.unmodifiableList(new ArrayList<>(
                itemRequirements == null
                        ? Collections.emptyList() : itemRequirements));
        this.requiresSpade = requiresSpade;
        this.requiresLight = requiresLight;
        this.enemy = clean(enemy);
        this.wilderness = wilderness;
        this.stashUnit = clean(stashUnit);
    }

    public boolean hasEnemy() { return enemy != null; }
    public boolean hasStashUnit() { return stashUnit != null; }

    /** Ordinary preparation that must be resolved before claiming DO NOW. */
    public boolean requiresPreparation()
    {
        return !itemRequirements.isEmpty() || requiresSpade || requiresLight
                || hasEnemy() || wilderness;
    }

    private static String clean(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

/**
 * Observed collection-log state plus explicitly proven long-form objectives.
 *
 * <p>Category counts are optional observed evidence. A missing category total is
 * unknown, not zero-complete. The named objective set is intentionally separate
 * from raw item IDs because some progression goals are multi-item/currency based.</p>
 */
@Getter
final class CollectionLogSnapshot
{
    private final Set<Integer> obtainedItemIds;
    private final Set<String> completedObjectiveIds;
    private final Map<String, Integer> categoryCompleted;
    private final Map<String, Integer> categoryTotals;

    public CollectionLogSnapshot(Set<Integer> obtainedItemIds)
    {
        this(obtainedItemIds, Collections.emptySet(),
                Collections.emptyMap(), Collections.emptyMap());
    }

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds)
    {
        this(obtainedItemIds, completedObjectiveIds,
                Collections.emptyMap(), Collections.emptyMap());
    }

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds,
            Map<String, Integer> categoryCompleted,
            Map<String, Integer> categoryTotals)
    {
        this.obtainedItemIds = Collections.unmodifiableSet(
                obtainedItemIds == null
                        ? new HashSet<>()
                        : new HashSet<>(obtainedItemIds)
        );
        this.completedObjectiveIds = Collections.unmodifiableSet(
                completedObjectiveIds == null
                        ? new HashSet<>()
                        : new HashSet<>(completedObjectiveIds)
        );
        this.categoryCompleted = Collections.unmodifiableMap(
                categoryCompleted == null
                        ? new HashMap<>()
                        : new HashMap<>(categoryCompleted)
        );
        this.categoryTotals = Collections.unmodifiableMap(
                categoryTotals == null
                        ? new HashMap<>()
                        : new HashMap<>(categoryTotals)
        );
    }

    public boolean hasItem(int itemId)
    {
        return obtainedItemIds.contains(itemId);
    }

    public boolean isObjectiveComplete(String objectiveId)
    {
        return objectiveId != null
                && completedObjectiveIds.contains(objectiveId);
    }

    public int obtainedCount()
    {
        return obtainedItemIds.size();
    }

    public int getCategoryCompleted(String category)
    {
        return categoryCompleted.getOrDefault(category, 0);
    }

    public int getCategoryTotal(String category)
    {
        return categoryTotals.getOrDefault(category, 0);
    }




}

@Getter
final class CombatAchievementSnapshot
{
    private final int completedTasks;
    private final int earnedPoints;
    private final Set<CombatAchievementTier> completedRewardTiers;

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints)
    {
        this(completedTasks, earnedPoints,
                Collections.emptySet());
    }

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints,
            Set<CombatAchievementTier> completedRewardTiers)
    {
        this.completedTasks = Math.max(0, completedTasks);
        this.earnedPoints = Math.max(0, earnedPoints);
        var tiers = EnumSet.noneOf(CombatAchievementTier.class);
        if (completedRewardTiers != null) tiers.addAll(completedRewardTiers);
        this.completedRewardTiers = Collections.unmodifiableSet(tiers);
    }




    public boolean isRewardTierComplete(CombatAchievementTier tier)
    {
        return completedRewardTiers.contains(tier);
    }

    public CombatAchievementTier nextRewardTier()
    {
        for (CombatAchievementTier tier : CombatAchievementTier.values())
            if (!completedRewardTiers.contains(tier)) return tier;
        return null;
    }
}

/** Directly observed spellbook selector and prayer state. */
@Getter
final class CombatEvidenceSnapshot
{
    private final int spellbookSelector;
    private final Set<Prayer> activePrayers;
    private final boolean rigourUnlocked;
    private final boolean auguryUnlocked;
    private final boolean preserveUnlocked;

    public CombatEvidenceSnapshot(int spellbookSelector,
            Set<Prayer> activePrayers, boolean rigourUnlocked,
            boolean auguryUnlocked, boolean preserveUnlocked)
    {
        this.spellbookSelector = spellbookSelector;
        this.activePrayers = Collections.unmodifiableSet(activePrayers == null
                || activePrayers.isEmpty() ? EnumSet.noneOf(Prayer.class)
                : EnumSet.copyOf(activePrayers));
        this.rigourUnlocked = rigourUnlocked;
        this.auguryUnlocked = auguryUnlocked;
        this.preserveUnlocked = preserveUnlocked;
    }

}

final class DiarySnapshot
{
    private final Map<String, Integer> completedTasksByRegion;
    private final Map<String, Integer> totalTasksByRegion;
    @Getter
    private final Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion;
    @Getter
    private final Map<String, Boolean> observedTaskCompletion;

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion)
    {
        this(completedTasksByRegion, totalTasksByRegion,
                Collections.emptyMap(), Collections.emptyMap());
    }

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion,
            Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion)
    {
        this(completedTasksByRegion, totalTasksByRegion,
                completedTiersByRegion, Collections.emptyMap());
    }

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion,
            Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion,
            Map<String, Boolean> observedTaskCompletion)
    {
        this.completedTasksByRegion = Collections.unmodifiableMap(
                completedTasksByRegion == null
                        ? new HashMap<>()
                        : new HashMap<>(completedTasksByRegion)
        );
        this.totalTasksByRegion = Collections.unmodifiableMap(
                totalTasksByRegion == null
                        ? new HashMap<>()
                        : new HashMap<>(totalTasksByRegion)
        );
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();
        if (completedTiersByRegion != null)
        {
            for (Map.Entry<String, Map<DiaryTier, Boolean>> entry
                    : completedTiersByRegion.entrySet())
            {
                EnumMap<DiaryTier, Boolean> copy = new EnumMap<>(DiaryTier.class);
                if (entry.getValue() != null) copy.putAll(entry.getValue());
                tiers.put(entry.getKey(), Collections.unmodifiableMap(copy));
            }
        }
        this.completedTiersByRegion = Collections.unmodifiableMap(tiers);
        this.observedTaskCompletion = Collections.unmodifiableMap(
                observedTaskCompletion == null
                        ? new HashMap<>()
                        : new HashMap<>(observedTaskCompletion));
    }

    public int completedIn(String region)
    {
        return completedTasksByRegion.getOrDefault(region, 0);
    }

    public int totalIn(String region)
    {
        return totalTasksByRegion.getOrDefault(region, 0);
    }

    public Set<String> getRegions()
    {
        Set<String> result = new HashSet<>(completedTasksByRegion.keySet());
        result.addAll(totalTasksByRegion.keySet());
        result.addAll(completedTiersByRegion.keySet());
        return Collections.unmodifiableSet(result);
    }

    public boolean isTierComplete(String region, DiaryTier tier)
    {
        var tiers = completedTiersByRegion.get(region);
        return tiers != null && Boolean.TRUE.equals(tiers.get(tier));
    }

    public Map<DiaryTier, Boolean> tiersFor(String region)
    {
        var tiers = completedTiersByRegion.get(region);
        return tiers == null ? Collections.emptyMap() : tiers;
    }


    /** Null means the individual task row has not been observed. */
    public Boolean taskCompletion(String taskId)
    {
        return observedTaskCompletion.get(taskId);
    }

}

final class FarmingRunSnapshot
{
    @Getter
    private final Map<String, ObservedFarmingPatchState> states;

    public FarmingRunSnapshot(Map<String, ObservedFarmingPatchState> states)
    {
        this.states = Collections.unmodifiableMap(
                states == null ? new HashMap<>() : new HashMap<>(states));
    }

    public static FarmingRunSnapshot empty()
    {
        return new FarmingRunSnapshot(Collections.emptyMap());
    }

    public ObservedFarmingPatchState stateOf(String patchId)
    {
        return states.get(patchId);
    }

}

/**
 * Farming-specific account state used by herb/tree/farming-contract planning.
 *
 * <p>Patch availability is stored as verified IDs. Tool Leprechaun contents
 * are kept separate because simply having access to a Leprechaun does not mean
 * every tool is stored there.</p>
 */
@Getter
final class FarmingSnapshot
{
    private final Set<String> reachablePatchIds;
    private final Map<String, CapabilityState> leprechaunTools;
    private final Map<String, Long> patchReadyAtMillis;

    public FarmingSnapshot(
            Set<String> reachablePatchIds,
            Map<String, CapabilityState> leprechaunTools,
            Map<String, Long> patchReadyAtMillis)
    {
        this.reachablePatchIds = Collections.unmodifiableSet(
                reachablePatchIds == null
                        ? new HashSet<>()
                        : new HashSet<>(reachablePatchIds)
        );
        this.leprechaunTools = Collections.unmodifiableMap(
                leprechaunTools == null
                        ? new HashMap<>()
                        : new HashMap<>(leprechaunTools)
        );
        this.patchReadyAtMillis = Collections.unmodifiableMap(
                patchReadyAtMillis == null
                        ? new HashMap<>()
                        : new HashMap<>(patchReadyAtMillis)
        );
    }

    public static FarmingSnapshot unknown()
    {
        return new FarmingSnapshot(
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    public boolean isPatchReachable(String patchId)
    {
        return patchId != null && reachablePatchIds.contains(patchId);
    }

    public CapabilityState leprechaunToolState(String toolId)
    {
        return leprechaunTools.getOrDefault(
                toolId,
                CapabilityState.UNKNOWN
        );
    }

    public Long readyAt(String patchId)
    {
        return patchReadyAtMillis.get(patchId);
    }

}

/**
 * Minigame unlocks and currencies observed on the account.
 *
 * <p>This covers progression systems such as Guardians of the Rift,
 * Tempoross, Wintertodt, Giants' Foundry, Mahogany Homes, Barbarian Assault,
 * and future minigames without teaching the strategy engine each minigame's
 * internal storage format.</p>
 */
@Getter
final class MinigameSnapshot
{
    private final Set<String> unlocked;
    private final Map<String, Integer> currencies;

    public MinigameSnapshot(
            Set<String> unlocked,
            Map<String, Integer> currencies)
    {
        this.unlocked = Collections.unmodifiableSet(
                unlocked == null
                        ? new HashSet<>()
                        : new HashSet<>(unlocked)
        );
        this.currencies = Collections.unmodifiableMap(
                currencies == null
                        ? new HashMap<>()
                        : new HashMap<>(currencies)
        );
    }

    public static MinigameSnapshot unknown()
    {
        return new MinigameSnapshot(
                Collections.emptySet(),
                Collections.emptyMap()
        );
    }

    public boolean isUnlocked(String minigameId)
    {
        return minigameId != null && unlocked.contains(minigameId);
    }

    public int currency(String currencyId)
    {
        return currencies.getOrDefault(currencyId, 0);
    }

}

/**
 * Player-owned-house capability snapshot.
 *
 * <p>POH planning is especially important for Ironman and UIM accounts, but
 * Compass must only recommend furniture/storage that is known to exist.
 * Furniture keys remain data-driven so new rooms and objects do not require a
 * rewrite of the strategy engine.</p>
 */
@Getter
final class PohSnapshot
{
    private final CapabilityState houseAccess;
    private final Map<String, CapabilityState> furniture;

    public PohSnapshot(
            CapabilityState houseAccess,
            Map<String, CapabilityState> furniture)
    {
        this.houseAccess = houseAccess == null
                ? CapabilityState.UNKNOWN
                : houseAccess;
        this.furniture = Collections.unmodifiableMap(
                furniture == null
                        ? new HashMap<>()
                        : new HashMap<>(furniture)
        );
    }

    public static PohSnapshot unknown()
    {
        return new PohSnapshot(
                CapabilityState.UNKNOWN,
                Collections.emptyMap()
        );
    }


    public CapabilityState furnitureState(String furnitureId)
    {
        return furniture.getOrDefault(
                furnitureId,
                CapabilityState.UNKNOWN
        );
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PohSnapshot)) return false;
        var that = (PohSnapshot) other;
        return houseAccess == that.houseAccess
                && furniture.equals(that.furniture);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(houseAccess, furniture);
    }
}

/** Immutable render-safe view of the current local progress session. */
@Getter
final class ProgressSessionSnapshot
{
    private final long startedAtMillis;
    private final long updatedAtMillis;
    private final long activeDurationMillis;
    private final Map<Skill, SkillSessionProgress> skills;
    private final List<ProgressTimeBucket> buckets;
    private final List<ProgressMilestone> milestones;
    private final ProgressTargetProjection targetProjection;

    ProgressSessionSnapshot(
            long startedAtMillis,
            long updatedAtMillis,
            long activeDurationMillis,
            Map<Skill, SkillSessionProgress> skills,
            List<ProgressTimeBucket> buckets,
            List<ProgressMilestone> milestones,
            ProgressTargetProjection targetProjection)
    {
        this.startedAtMillis = startedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.activeDurationMillis = Math.max(0L, activeDurationMillis);
        EnumMap<Skill, SkillSessionProgress> skillCopy =
                new EnumMap<>(Skill.class);
        if (skills != null) skillCopy.putAll(skills);
        this.skills = Collections.unmodifiableMap(skillCopy);
        this.buckets = Collections.unmodifiableList(new ArrayList<>(buckets));
        this.milestones = Collections.unmodifiableList(
                new ArrayList<>(milestones));
        this.targetProjection = targetProjection;
    }

    public long getSessionDurationMillis()
    {
        return Math.max(0L, updatedAtMillis - startedAtMillis);
    }

    public long getTotalXpGained()
    {
        var result = 0L;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getXpGained();
        return result;
    }

    public int getLevelsGained()
    {
        var result = 0;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getLevelsGained();
        return result;
    }
}

/**
 * Readiness assessments for bosses, raids, and other PvM activities.
 *
 * <p>A future PvM analyzer can populate this from combat stats, prayers,
 * spellbooks, equipment, supplies, quest access, kill count, and Combat
 * Achievements. Keeping the assessment separate from the UI makes that logic
 * testable with fake accounts.</p>
 */
final class PvmSnapshot
{
    @Getter
    private final Map<String, PvmReadiness> readinessByActivity;

    public PvmSnapshot(Map<String, PvmReadiness> readinessByActivity)
    {
        this.readinessByActivity = Collections.unmodifiableMap(
                readinessByActivity == null
                        ? new HashMap<>()
                        : new HashMap<>(readinessByActivity)
        );
    }

    public static PvmSnapshot unknown()
    {
        return new PvmSnapshot(Collections.emptyMap());
    }

    public PvmReadiness readinessFor(String activityId)
    {
        return readinessByActivity.get(activityId);
    }

}

final class QuestSnapshot
{
    @Getter
    @Accessors(fluent = true)
    private final Map<String, QuestStatus> quests;

    public QuestSnapshot(Map<String, QuestStatus> quests)
    {
        this.quests = Collections.unmodifiableMap(
                new HashMap<>(quests)
        );
    }

    public QuestStatus statusOf(String questName)
    {
        return quests.getOrDefault(
                questName,
                QuestStatus.UNKNOWN
        );
    }

}

/**
 * Generic cooldown/ready-time storage for recurring OSRS activities.
 *
 * <p>Birdhouses, herb/tree runs, Tears of Guthix, Kingdom, farming contracts,
 * daily diary rewards, and future cooldown content should all flow through
 * this single model rather than each feature inventing its own timer system.</p>
 */
final class RecurringOpportunitySnapshot
{
    @Getter
    private final Map<String, Long> readyAtMillis;

    public RecurringOpportunitySnapshot(Map<String, Long> readyAtMillis)
    {
        this.readyAtMillis = Collections.unmodifiableMap(
                readyAtMillis == null
                        ? new HashMap<>()
                        : new HashMap<>(readyAtMillis)
        );
    }

    public static RecurringOpportunitySnapshot unknown()
    {
        return new RecurringOpportunitySnapshot(Collections.emptyMap());
    }

    public Long readyAt(String opportunityId)
    {
        return readyAtMillis.get(opportunityId);
    }

    public boolean isReadyNow(String opportunityId, long nowMillis)
    {
        var readyAt = readyAtMillis.get(opportunityId);
        return readyAt != null && readyAt <= nowMillis;
    }

}

/**
 * Sailing discovery/progression state.
 *
 * <p>Sailing is expected to evolve quickly. Keeping ports and activities as
 * data keys means the recommendation engine can gain new Sailing coverage by
 * updating structured game data instead of changing its core algorithm.</p>
 */
@Getter
final class SailingSnapshot
{
    public static final String PORT_SARIM = "port:sarim";
    public static final String PORT_PANDEMONIUM = Text.get(1960);
    public static final String ACTIVITY_COURIER = Text.get(1961);
    public static final String ACTIVITY_ACTIVE_PORT_TASK = Text.get(1962);
    public static final String ACTIVITY_SEA_CHARTING = Text.get(1963);
    public static final String ACTIVITY_BOAT_OWNED = Text.get(1964);
    public static final String TRIAL_TEMPOR_COMPLETE = Text.get(1965);
    public static final String TRIAL_JUBBLY_COMPLETE = Text.get(1966);
    public static final String TRIAL_GWENITH_COMPLETE = Text.get(1967);

    private final Set<String> verifiedPorts;
    private final Set<String> verifiedActivities;
    private final Confidence confidence;

    public SailingSnapshot(
            Set<String> verifiedPorts,
            Set<String> verifiedActivities,
            Confidence confidence)
    {
        this.verifiedPorts = Collections.unmodifiableSet(
                verifiedPorts == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedPorts)
        );
        this.verifiedActivities = Collections.unmodifiableSet(
                verifiedActivities == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedActivities)
        );
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
    }

    public static SailingSnapshot unknown()
    {
        return new SailingSnapshot(
                Collections.emptySet(),
                Collections.emptySet(),
                Confidence.CHECK_NEEDED
        );
    }

    public boolean hasPort(String portId)
    {
        return portId != null && verifiedPorts.contains(portId);
    }

    public boolean hasActivity(String activityId)
    {
        return activityId != null && verifiedActivities.contains(activityId);
    }

}

/** Live, per-character ownership evidence for reviewed Slayer rewards. */
final class SlayerRewardSnapshot
{
    @Getter
    private final Map<SlayerReward, CapabilityState> states;

    public SlayerRewardSnapshot(Map<SlayerReward, CapabilityState> states)
    {
        EnumMap<SlayerReward, CapabilityState> copy =
                new EnumMap<>(SlayerReward.class);
        if (states != null) copy.putAll(states);
        this.states = Collections.unmodifiableMap(copy);
    }

    public static SlayerRewardSnapshot unknown()
    {
        return new SlayerRewardSnapshot(Collections.emptyMap());
    }

    public CapabilityState stateOf(SlayerReward reward)
    {
        return reward == null ? CapabilityState.UNKNOWN
                : states.getOrDefault(reward, CapabilityState.UNKNOWN);
    }

    public boolean isUnlocked(SlayerReward reward)
    {
        return stateOf(reward) == CapabilityState.VERIFIED;
    }

}

/**
 * Verified/unknown state and observed contents for storage systems.
 *
 * <p>UNKNOWN remains different from unavailable. Contents are only stored when
 * actually observed. This is especially important for UIM, where a generic
 * Text.get(1947) assumption can create unsafe or impossible advice.</p>
 */
final class StorageSnapshot
{
    @Getter
    private final Map<StorageCapability, CapabilityState> states;
    private final Map<StorageCapability, List<ItemState>> contents;

    public StorageSnapshot(Map<StorageCapability, CapabilityState> states)
    {
        this(states, Collections.emptyMap());
    }

    public StorageSnapshot(
            Map<StorageCapability, CapabilityState> states,
            Map<StorageCapability, List<ItemState>> contents)
    {
        EnumMap<StorageCapability, CapabilityState> stateCopy =
                new EnumMap<>(StorageCapability.class);
        if (states != null) stateCopy.putAll(states);
        this.states = Collections.unmodifiableMap(stateCopy);

        EnumMap<StorageCapability, List<ItemState>> contentCopy =
                new EnumMap<>(StorageCapability.class);
        if (contents != null)
        {
            for (Map.Entry<StorageCapability, List<ItemState>> entry
                    : contents.entrySet())
            {
                contentCopy.put(entry.getKey(), Collections.unmodifiableList(
                        entry.getValue() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(entry.getValue())
                ));
            }
        }
        this.contents = Collections.unmodifiableMap(contentCopy);
    }

    public static StorageSnapshot unknown()
    {
        return new StorageSnapshot(Collections.emptyMap());
    }

    public CapabilityState stateOf(StorageCapability capability)
    {
        return states.getOrDefault(capability, CapabilityState.UNKNOWN);
    }

    public boolean verified(StorageCapability capability)
    {
        return stateOf(capability) == CapabilityState.VERIFIED;
    }

    public boolean hasObservedContents(StorageCapability capability)
    {
        return capability != null && contents.containsKey(capability);
    }

    public List<ItemState> contentsOf(StorageCapability capability)
    {
        return contents.getOrDefault(capability, Collections.emptyList());
    }

    /**
     * Convenience view for planners that must reason about dangerous-death UIM
     * state. This returns observed contents only. An empty list does not imply
     * the capability is unavailable; callers should use {@link #verified} or
     * {@link #hasObservedContents} when that distinction matters.
     */
    public List<ItemState> getDeathStorageItems()
    {
        List<ItemState> observed = new ArrayList<>();
        observed.addAll(contentsOf(StorageCapability.DEATH_STORAGE));
        observed.addAll(contentsOf(StorageCapability.HESPORI_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(StorageCapability.ZULRAH_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(
                StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL));
        return Collections.unmodifiableList(observed);
    }

    public int quantityOf(StorageCapability capability, int itemId)
    {
        if (!verified(capability) || !hasObservedContents(capability)) return 0;
        var total = 0;
        for (ItemState item : contentsOf(capability))
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }


    public Map<StorageCapability, List<ItemState>> getObservedContents()
    {
        return contents;
    }
}

/**
 * Transport and teleport options that the plugin has actually verified.
 *
 * <p>This deliberately stores opaque route keys rather than hard-coding the
 * transport network into Java. Future game-data files can define what each
 * key means and which activities require it.</p>
 */
final class TransportSnapshot
{
    @Getter
    private final Set<String> verifiedRoutes;

    public TransportSnapshot(Set<String> verifiedRoutes)
    {
        this.verifiedRoutes = Collections.unmodifiableSet(
                verifiedRoutes == null
                        ? new HashSet<>()
                        : new HashSet<>(verifiedRoutes)
        );
    }

    public static TransportSnapshot unknown()
    {
        return new TransportSnapshot(Collections.emptySet());
    }

    public boolean hasVerifiedRoute(String routeId)
    {
        return routeId != null && verifiedRoutes.contains(routeId);
    }

}
