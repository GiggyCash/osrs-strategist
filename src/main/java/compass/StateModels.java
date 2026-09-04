package compass;
import lombok.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import static compass.Text.get;

import java.util.*;
import lombok.experimental.Accessors;
import net.runelite.api.*;

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
    final Map<String, Long> lastObservedAtMillis;

    public AccessMemorySnapshot(Map<String, Long> values)
    {
        this.lastObservedAtMillis = SnapshotCollections.map(values);
    }

    public static AccessMemorySnapshot empty()
    {
        return new AccessMemorySnapshot(emptyMap());
    }

    public boolean hasObserved(String key)
    {
        return key != null && lastObservedAtMillis.containsKey(key);
    }
}

@Getter
@RequiredArgsConstructor
final class AccountEconomySnapshot
{
    final long coins;
    final long estimatedBankValue;
    final Confidence confidence;

}

@Getter
final class AccountSnapshot
{
    final String playerName;
    final long accountHash;
    final int accountTypeCode;
    final String accountTypeName;
    final Membership membershipStatus;
    final int membershipCredit;
    final int totalLevel;
    final long totalExperience;

    final Map<Skill, Integer> skillLevels;
    final Map<Skill, Integer> skillExperience;

       public AccountSnapshot(
            String playerName,
            long accountHash,
            int accountTypeCode,
            String accountTypeName,
            Membership membershipStatus,
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
                ? Membership.UNKNOWN
                : membershipStatus;
        this.membershipCredit = membershipCredit;
        this.totalLevel = totalLevel;
        this.totalExperience = totalExperience;

        this.skillLevels = SnapshotCollections.map(skillLevels);
        this.skillExperience = SnapshotCollections.map(skillExperience);
    }


    /** Stable local character identity. Zero means RuneLite has not supplied it yet. */

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
    Membership membership() { return membershipStatus; }
    int modeCode() { return accountTypeCode; }

    public int getSkillExperience(Skill skill)
    {
        return skillExperience.getOrDefault(skill, 0);
    }
}

@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
final class ClueSnapshot
{
    final boolean cluePresent;
    final String clueType;
    final long firstSeenAtMillis;
    final Confidence confidence;
    final ClueStepSnapshot currentStep;

}

/** Exact current-step evidence supplied by RuneLite's Clue Scroll plugin. */
@Getter
final class ClueStepSnapshot
{
    final String kind;
    final String action;
    final String location;
    final List<String> itemRequirements;
    final boolean requiresSpade;
    final boolean requiresLight;
    final String enemy;
    final boolean wilderness;
    final String stashUnit;

    public ClueStepSnapshot(String kind, String action, String location,
            List<String> itemRequirements, boolean requiresSpade,
            boolean requiresLight, String enemy, boolean wilderness,
            String stashUnit)
    {
        this.kind = clean(kind);
        this.action = clean(action);
        this.location = clean(location);
        this.itemRequirements = SnapshotCollections.list(itemRequirements);
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
    final Set<Integer> obtainedItemIds;
    final Set<String> completedObjectiveIds;
    final Map<String, Integer> categoryCompleted;
    final Map<String, Integer> categoryTotals;

    public CollectionLogSnapshot(
            Set<Integer> obtainedItemIds,
            Set<String> completedObjectiveIds,
            Map<String, Integer> categoryCompleted,
            Map<String, Integer> categoryTotals)
    {
        this.obtainedItemIds = SnapshotCollections.set(obtainedItemIds);
        this.completedObjectiveIds = SnapshotCollections.set(completedObjectiveIds);
        this.categoryCompleted = SnapshotCollections.map(categoryCompleted);
        this.categoryTotals = SnapshotCollections.map(categoryTotals);
    }
    public boolean isObjectiveComplete(String objectiveId)
    {
        return objectiveId != null
                && completedObjectiveIds.contains(objectiveId);
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
    final int completedTasks;
    final int earnedPoints;
    final Set<CombatAchievementTier> completedRewardTiers;

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints,
            Set<CombatAchievementTier> completedRewardTiers)
    {
        this.completedTasks = max(0, completedTasks);
        this.earnedPoints = max(0, earnedPoints);
        this.completedRewardTiers = SnapshotCollections.set(completedRewardTiers);
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
    final int spellbookSelector;
    final Set<Prayer> activePrayers;
    final boolean rigourUnlocked;
    final boolean auguryUnlocked;
    final boolean preserveUnlocked;

    public CombatEvidenceSnapshot(int spellbookSelector,
            Set<Prayer> activePrayers, boolean rigourUnlocked,
            boolean auguryUnlocked, boolean preserveUnlocked)
    {
        this.spellbookSelector = spellbookSelector;
        this.activePrayers = SnapshotCollections.set(activePrayers);
        this.rigourUnlocked = rigourUnlocked;
        this.auguryUnlocked = auguryUnlocked;
        this.preserveUnlocked = preserveUnlocked;
    }

}

final class DiarySnapshot
{
    final Map<String, Integer> completedTasksByRegion;
    final Map<String, Integer> totalTasksByRegion;
    @Getter
    final Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion;
    @Getter
    final Map<String, Boolean> observedTaskCompletion;

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion,
            Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion,
            Map<String, Boolean> observedTaskCompletion)
    {
        this.completedTasksByRegion = SnapshotCollections.map(completedTasksByRegion);
        this.totalTasksByRegion = SnapshotCollections.map(totalTasksByRegion);
        this.completedTiersByRegion = SnapshotCollections.maps(completedTiersByRegion);
        this.observedTaskCompletion = SnapshotCollections.map(observedTaskCompletion);
    }

    public int completedIn(String region)
    {
        return completedTasksByRegion.getOrDefault(region, 0);
    }
    public Set<String> getRegions()
    {
        Set<String> result = new HashSet<>(completedTasksByRegion.keySet());
        result.addAll(totalTasksByRegion.keySet());
        result.addAll(completedTiersByRegion.keySet());
        return unmodifiableSet(result);
    }

    public boolean isTierComplete(String region, DiaryTier tier)
    {
        var tiers = completedTiersByRegion.get(region);
        return tiers != null && Boolean.TRUE.equals(tiers.get(tier));
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
    final Map<String, ObservedFarmingPatchState> states;

    public FarmingRunSnapshot(Map<String, ObservedFarmingPatchState> states)
    {
        this.states = SnapshotCollections.map(states);
    }

    public static FarmingRunSnapshot empty()
    {
        return new FarmingRunSnapshot(emptyMap());
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
    final Set<String> reachablePatchIds;
    final Map<String, Capability> leprechaunTools;
    final Map<String, Long> patchReadyAtMillis;

    public FarmingSnapshot(
            Set<String> reachablePatchIds,
            Map<String, Capability> leprechaunTools,
            Map<String, Long> patchReadyAtMillis)
    {
        this.reachablePatchIds = SnapshotCollections.set(reachablePatchIds);
        this.leprechaunTools = SnapshotCollections.map(leprechaunTools);
        this.patchReadyAtMillis = SnapshotCollections.map(patchReadyAtMillis);
    }

    public static FarmingSnapshot unknown()
    {
        return new FarmingSnapshot(
                emptySet(),
                emptyMap(),
                emptyMap()
        );
    }

    public boolean isPatchReachable(String patchId)
    {
        return patchId != null && reachablePatchIds.contains(patchId);
    }

    public Capability leprechaunToolState(String toolId)
    {
        return leprechaunTools.getOrDefault(
                toolId,
                Capability.UNKNOWN
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
    final Set<String> unlocked;
    final Map<String, Integer> currencies;

    public MinigameSnapshot(
            Set<String> unlocked,
            Map<String, Integer> currencies)
    {
        this.unlocked = SnapshotCollections.set(unlocked);
        this.currencies = SnapshotCollections.map(currencies);
    }

    public static MinigameSnapshot unknown()
    {
        return new MinigameSnapshot(
                emptySet(),
                emptyMap()
        );
    }

    public boolean isUnlocked(String minigameId)
    {
        return minigameId != null && unlocked.contains(minigameId);
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
@EqualsAndHashCode
final class PohSnapshot
{
    final Capability houseAccess;
    final Map<String, Capability> furniture;

    public PohSnapshot(
            Capability houseAccess,
            Map<String, Capability> furniture)
    {
        this.houseAccess = houseAccess == null
                ? Capability.UNKNOWN
                : houseAccess;
        this.furniture = SnapshotCollections.map(furniture);
    }

    public static PohSnapshot unknown()
    {
        return new PohSnapshot(
                Capability.UNKNOWN,
                emptyMap()
        );
    }


    public Capability furnitureState(String furnitureId)
    {
        return furniture.getOrDefault(
                furnitureId,
                Capability.UNKNOWN
        );
    }

}

/** Immutable render-safe view of the current local progress session. */
@Getter
final class ProgressSessionSnapshot
{
    final long startedAtMillis;
    final long updatedAtMillis;
    final long activeDurationMillis;
    final Map<Skill, SkillSessionProgress> skills;
    final List<ProgressTimeBucket> buckets;
    final List<ProgressMilestone> milestones;
    final TargetProjection targetProjection;

    ProgressSessionSnapshot(
            long startedAtMillis,
            long updatedAtMillis,
            long activeDurationMillis,
            Map<Skill, SkillSessionProgress> skills,
            List<ProgressTimeBucket> buckets,
            List<ProgressMilestone> milestones,
            TargetProjection targetProjection)
    {
        this.startedAtMillis = startedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.activeDurationMillis = max(0L, activeDurationMillis);
        this.skills = SnapshotCollections.map(skills);
        this.buckets = SnapshotCollections.list(buckets);
        this.milestones = SnapshotCollections.list(milestones);
        this.targetProjection = targetProjection;
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
    final Map<String, PvmReadiness> readinessByActivity;

    public PvmSnapshot(Map<String, PvmReadiness> readinessByActivity)
    {
        this.readinessByActivity = SnapshotCollections.map(readinessByActivity);
    }

    public static PvmSnapshot unknown()
    {
        return new PvmSnapshot(emptyMap());
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
    final Map<String, QuestStatus> quests;

    public QuestSnapshot(Map<String, QuestStatus> quests)
    {
        this.quests = SnapshotCollections.map(quests);
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
    final Map<String, Long> readyAtMillis;

    public RecurringOpportunitySnapshot(Map<String, Long> readyAtMillis)
    {
        this.readyAtMillis = SnapshotCollections.map(readyAtMillis);
    }

    public static RecurringOpportunitySnapshot unknown()
    {
        return new RecurringOpportunitySnapshot(emptyMap());
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
    public static final String PORT_PANDEMONIUM = get(1960);
    public static final String ACTIVITY_COURIER = get(1961);
    public static final String ACTIVITY_ACTIVE_PORT_TASK = get(1962);
    public static final String ACTIVITY_SEA_CHARTING = get(1963);
    public static final String ACTIVITY_BOAT_OWNED = get(1964);
    public static final String TRIAL_TEMPOR_COMPLETE = get(1965);
    public static final String TRIAL_JUBBLY_COMPLETE = get(1966);
    public static final String TRIAL_GWENITH_COMPLETE = get(1967);

    final Set<String> verifiedPorts;
    final Set<String> verifiedActivities;
    final Confidence confidence;

    public SailingSnapshot(
            Set<String> verifiedPorts,
            Set<String> verifiedActivities,
            Confidence confidence)
    {
        this.verifiedPorts = SnapshotCollections.set(verifiedPorts);
        this.verifiedActivities = SnapshotCollections.set(verifiedActivities);
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
    }

    public static SailingSnapshot unknown()
    {
        return new SailingSnapshot(
                emptySet(),
                emptySet(),
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
    final Map<SlayerReward, Capability> states;

    public SlayerRewardSnapshot(Map<SlayerReward, Capability> states)
    {
        this.states = SnapshotCollections.map(states);
    }

    public static SlayerRewardSnapshot unknown()
    {
        return new SlayerRewardSnapshot(emptyMap());
    }

    public Capability stateOf(SlayerReward reward)
    {
        return reward == null ? Capability.UNKNOWN
                : states.getOrDefault(reward, Capability.UNKNOWN);
    }

    public boolean isUnlocked(SlayerReward reward)
    {
        return stateOf(reward) == Capability.VERIFIED;
    }

}

/**
 * Verified/unknown state and observed contents for storage systems.
 *
 * <p>UNKNOWN remains different from unavailable. Contents are only stored when
 * actually observed. This is especially important for UIM, where a generic
 * get(1947) assumption can create unsafe or impossible advice.</p>
 */
final class StorageSnapshot
{
    @Getter
    final Map<StorageKind, Capability> states;
    final Map<StorageKind, List<ItemState>> contents;

    public StorageSnapshot(
            Map<StorageKind, Capability> states,
            Map<StorageKind, List<ItemState>> contents)
    {
        this.states = SnapshotCollections.map(states);
        this.contents = SnapshotCollections.lists(contents);
    }

    public static StorageSnapshot unknown()
    {
        return new StorageSnapshot(emptyMap(), emptyMap());
    }

    public Capability stateOf(StorageKind capability)
    {
        return states.getOrDefault(capability, Capability.UNKNOWN);
    }

    public boolean verified(StorageKind capability)
    {
        return stateOf(capability) == Capability.VERIFIED;
    }

    public boolean hasObservedContents(StorageKind capability)
    {
        return capability != null && contents.containsKey(capability);
    }

    public List<ItemState> contentsOf(StorageKind capability)
    {
        return contents.getOrDefault(capability, emptyList());
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
        observed.addAll(contentsOf(StorageKind.DEATH_STORAGE));
        observed.addAll(contentsOf(StorageKind.HESPORI_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(StorageKind.ZULRAH_ITEM_RETRIEVAL));
        observed.addAll(contentsOf(
                StorageKind.VOLCANIC_MINE_ITEM_RETRIEVAL));
        return unmodifiableList(observed);
    }

    public int quantityOf(StorageKind capability, int itemId)
    {
        if (!verified(capability) || !hasObservedContents(capability)) return 0;
        var total = 0;
        for (ItemState item : contentsOf(capability))
        {
            if (item.itemId == itemId) total += item.quantity;
        }
        return total;
    }


    public Map<StorageKind, List<ItemState>> getObservedContents()
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
    final Set<String> verifiedRoutes;

    public TransportSnapshot(Set<String> verifiedRoutes)
    {
        this.verifiedRoutes = SnapshotCollections.set(verifiedRoutes);
    }

    public static TransportSnapshot unknown()
    {
        return new TransportSnapshot(emptySet());
    }

    public boolean hasVerifiedRoute(String routeId)
    {
        return routeId != null && verifiedRoutes.contains(routeId);
    }

}

/** One defensive-copy policy for every immutable live-state collection. */
final class SnapshotCollections
{
    private SnapshotCollections() { }

    static <K, V> Map<K, V> map(Map<K, V> values)
    {
        return unmodifiableMap(values == null ? emptyMap()
                : new HashMap<>(values));
    }

    static <V> Set<V> set(Set<V> values)
    {
        return unmodifiableSet(values == null ? emptySet()
                : new HashSet<>(values));
    }

    static <V> List<V> list(List<V> values)
    {
        return unmodifiableList(values == null ? emptyList()
                : new ArrayList<>(values));
    }

    static <K, V> Map<K, Map<V, Boolean>> maps(
            Map<K, Map<V, Boolean>> values)
    {
        Map<K, Map<V, Boolean>> copy = new HashMap<>();
        if (values != null) for (Map.Entry<K, Map<V, Boolean>> entry
                : values.entrySet()) copy.put(entry.getKey(), map(entry.getValue()));
        return unmodifiableMap(copy);
    }

    static <K, V> Map<K, List<V>> lists(Map<K, List<V>> values)
    {
        Map<K, List<V>> copy = new HashMap<>();
        if (values != null) for (Map.Entry<K, List<V>> entry
                : values.entrySet()) copy.put(entry.getKey(), list(entry.getValue()));
        return unmodifiableMap(copy);
    }
}
