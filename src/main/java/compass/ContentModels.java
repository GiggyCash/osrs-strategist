package compass;
import lombok.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import static compass.Text.get;

import java.util.*;
import net.runelite.api.Skill;

/** Verified prerequisites for a prayer, spell, or spellbook unlock. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
final class AbilityUnlockDefinition
{
    final String id;
    final String name;
    final GoalNodeKind kind;
    final String quest;
    final Skill skill;
    final int level;
    final Skill secondarySkill;
    final int secondaryLevel;
    final String requiredItem;
    final String encounterId;
    final String accessCheck;


}

/** Immutable, exhaustive account strategic-priority profile. */
final class AccountPriorities
{
    @Getter
    final AccountMode accountMode;
    @Getter
    final Map<AccountDimension, AccountPriority>
            priorities;

    AccountPriorities(AccountMode accountMode,
            Map<AccountDimension, AccountPriority> values)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        EnumMap<AccountDimension, AccountPriority> copy =
                new EnumMap<>(AccountDimension.class);
        if (values != null) copy.putAll(values);
        for (AccountDimension dimension
                : AccountDimension.values())
        {
            if (!copy.containsKey(dimension))
                throw new IllegalArgumentException(
                        Text.get(1110) + dimension);
        }
        this.priorities = unmodifiableMap(copy);
    }


    public AccountPriority get(AccountDimension dimension)
    {
        return priorities.get(dimension);
    }

    public Priority priorityOf(AccountDimension dimension)
    {
        var value = get(dimension);
        return value == null ? Priority.NONE : value.getPriority();
    }

}

/** Sourced strategic properties shared by non-skill candidate families. */
@RequiredArgsConstructor
final class ActivityStrategyProfile
{
    @Getter
    final String candidatePrefix;
    final Set<AccountMode> accountModes;
    @Getter
    final InventoryFootprint inventoryFootprint;
    @Getter
    final double setupReuse;
    @Getter
    final String strategicReason;
    @Getter
    final List<Source> sources;


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}

@Getter
@RequiredArgsConstructor
final class AgilityCourseDefinition
{
    final String id;
    final String displayName;
    final int requiredLevel;
    final int regionId;
    final String requiredQuest;
    final boolean wilderness;

    public String observationKey()
    {
        return "region." + regionId;
    }
}

@Getter
final class DiaryTaskDefinition
{
    final String region;
    final DiaryTier tier;
    final String task;
    final List<DiaryTaskRequirement> requirements;

    DiaryTaskDefinition(String region, DiaryTier tier, String task,
            List<DiaryTaskRequirement> requirements)
    {
        this.region = region;
        this.tier = tier;
        this.task = task;
        this.requirements = unmodifiableList(
                new ArrayList<>(requirements));
    }

    public String getId()
    {
        return "diary-task:" + Names.slug(region) + ":"
                + tier.name().toLowerCase(Locale.ROOT) + ":" + Names.slug(task);
    }
    public boolean isWilderness()
    {
        return "Wilderness".equals(region);
    }

}

/**
 * Small piece of verified game-data describing a Farming patch group.
 *
 * <p>Region IDs are used only as positive observation evidence. Quest
 * requirements are used to infer access before the patch has ever been visited.</p>
 */
@RequiredArgsConstructor
@Getter
final class FarmingAccessDefinition
{
    final String id;
    final String displayName;
    final Set<Integer> regionIds;
    final String requiredQuest;
    final boolean herbPatch;

    public String observationKey()
    {
        return "farming.patch." + id;
    }
}

@RequiredArgsConstructor
@Getter
final class FarmingRunPatchDefinition
{
    final String id;
    final String displayName;
    final FarmingPatchKind kind;
    final int minimumLevel;
    final Set<Integer> regionIds;
    final int varbitId;
    final String requiredQuest;

    public boolean matchesRegion(int regionId)
    {
        return regionIds.contains(regionId);
    }

    public String observationKey()
    {
        return "farm-run." + id;
    }
}

/** Account-aware, multi-hop route for one meaningful gear target. */
@RequiredArgsConstructor
@Getter
final class GearAcquisitionRoute
{
    final String id;
    final String itemName;
    final CombatStyle style;
    final boolean tradeable;
    final List<GearAcquisitionStep> steps;
    final String valueRule;
    final String provenance;


}

/** One contextual gear tier. Names stay data-driven until ownership is verified by item IDs. */
@RequiredArgsConstructor
@Getter
final class GearProgressionEntry
{
    final String id;
    final String contextId;
    final CombatStyle style;
    final GearBudgetTier tier;
    final List<String> recommendedItems;
    final String weaponGuidance;
    final String note;
    final boolean freeToPlay;
    final boolean selfSourceFriendly;
    final boolean uimFriendly;
    final boolean hardcoreSafe;


}

/** One material rule consumed by a deterministic training action. */
@RequiredArgsConstructor
@Getter
final class MethodInputRule
{
    final MethodProfile.InputMode mode;
    final String fixedName;
    final double quantityPerAction;


}

/**
 * One concrete place where a method can be executed.
 *
 * <p>The optional transport route is an advantage, not an assumed gate. A
 * caller may only describe that route as available when the live transport
 * snapshot contains the exact route key.</p>
 */
@RequiredArgsConstructor
@Getter
final class MethodLocationOption
{
    final String id;
    final String name;
    final int ordinaryTravelBurden;
    final String advantageousRouteId;
    final int verifiedRouteTravelBurden;
    final boolean membersOnly;
    final boolean wilderness;

    int effectiveBurden(TransportSnapshot transport)
    {
        return advantageousRouteId != null && transport != null
                && transport.hasVerifiedRoute(advantageousRouteId)
                ? verifiedRouteTravelBurden : ordinaryTravelBurden;
    }

    int effectiveBurden(boolean verifiedRoute)
    {
        return advantageousRouteId != null && verifiedRoute
                ? verifiedRouteTravelBurden : ordinaryTravelBurden;
    }
}

/** Data descriptor connecting a training method to legal concrete locations. */
@RequiredArgsConstructor
@Getter
final class MethodLocationProfile
{
    final String methodId;
    final List<MethodLocationOption> locations;
    final String sourceUrl;


}

/** Sourced strategic properties layered over a mechanically legal method. */
@RequiredArgsConstructor
@Getter
final class MethodStrategyProfile
{
    final String methodId;
    final KnowledgeTier tier;
    final Set<AccountMode> accountModes;
    final BankingMode bankingBehavior;
    final InventoryFootprint inventoryFootprint;
    final double accountValueFit;
    final String playerReason;
    final List<Source> sources;


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}

@Getter
final class MinigameDefinition
{
    final String id;
    final String name;
    final Skill primarySkill;
    final int minimumLevel;
    final boolean freeToPlay;
    final RiskLevel riskLevel;
    final AttentionLevel attention;
    final Set<AccountMode> supportedModes;
    final String rewardFocus;
    final boolean combatActivity;

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus)
    {
        this(id, name, primarySkill, minimumLevel, freeToPlay, riskLevel,
                attention, supportedModes, rewardFocus, false);
    }

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus, boolean combatActivity)
    {
        this.id = id;
        this.name = name;
        this.primarySkill = primarySkill;
        this.minimumLevel = max(1, minimumLevel);
        this.freeToPlay = freeToPlay;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.supportedModes = supportedModes == null || supportedModes.isEmpty()
                ? emptySet()
                : unmodifiableSet(EnumSet.copyOf(supportedModes));
        this.rewardFocus = rewardFocus;
        this.combatActivity = combatActivity;
    }

    public boolean supports(AccountMode mode) { return supportedModes.contains(mode); }
}

/** Locally verifiable setup contract for a progression minigame. */
@Getter
@RequiredArgsConstructor
final class MinigameSetupProfile
{
    final String activityId;
    final ItemRule items;
    final String location;
    final String supplies;
    final String instructions;


}

/** Stable strategy metadata for a money/resource-producing activity. */
@RequiredArgsConstructor
@Getter
final class MoneyMakingDefinition
{
    final String id;
    final String name;
    final String description;
    final Skill primarySkill;
    final int minimumLevel;
    final boolean freeToPlay;
    final Set<AccountMode> supportedModes;
    final RiskLevel riskLevel;
    final AttentionLevel attention;
    final boolean wilderness;
    final boolean requiresLivePrices;

    public boolean supports(AccountMode mode)
    {
        return mode != null && supportedModes.contains(mode);
    }
}

/** Per-character planning preferences that survive logout/restart. */
@Getter
class PlayerStrategyProfile
{
    final StrategyMode strategyMode;
    final SessionIntent sessionIntent;
    final QuestTolerance questTolerance;
    final GoalType activeGoal;
    final boolean useGroupStorage;
    final boolean collectionistMode;
    final boolean allowWildernessMethods;

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode)
    {
        this(strategyMode, sessionIntent, questTolerance, activeGoal,
                useGroupStorage, collectionistMode, false);
    }

    public PlayerStrategyProfile(
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            QuestTolerance questTolerance,
            GoalType activeGoal,
            boolean useGroupStorage,
            boolean collectionistMode,
            boolean allowWildernessMethods)
    {
        this.strategyMode = strategyMode == null ? StrategyMode.BALANCED : strategyMode;
        this.sessionIntent = sessionIntent == null ? SessionIntent.PICK_FOR_ME : sessionIntent;
        this.questTolerance = questTolerance == null ? QuestTolerance.NORMAL : questTolerance;
        this.activeGoal = activeGoal == null ? GoalType.AUTOMATIC : activeGoal;
        this.useGroupStorage = useGroupStorage;
        this.collectionistMode = collectionistMode;
        this.allowWildernessMethods = allowWildernessMethods;
    }

    public static PlayerStrategyProfile fromConfig(OsrsStrategistConfig config)
    {
        var configuredGoal = config.activeGoal();
        return new PlayerStrategyProfile(
                config.strategyMode(), config.sessionIntent(),
                config.questTolerance(), configuredGoal == null
                        ? GoalType.AUTOMATIC
                        : configuredGoal.toPlanningGoal(),
                config.useGroupStorage(), config.collectionistMode(),
                config.allowWildernessMethods()
        );
    }

    StrategyMode mode() { return strategyMode; }
    SessionIntent intent() { return sessionIntent; }
    GoalType goal() { return activeGoal; }
    boolean usesGroupStorage() { return useGroupStorage; }
    boolean collectionist() { return collectionistMode; }
    boolean allowsWilderness() { return allowWildernessMethods; }


    PlayerStrategyProfile sanitizedForPublicProduct()
    {
        if (PlayerGoal.isPlayerFacing(activeGoal)) return this;
        return new PlayerStrategyProfile(strategyMode, sessionIntent,
                questTolerance, GoalType.AUTOMATIC, useGroupStorage,
                collectionistMode, allowWildernessMethods);
    }
}

/**
 * A longer-running reward objective associated with a training method. These
 * objectives outrank tiny variety nudges until completion is actually known.
 */
@Getter
@RequiredArgsConstructor
final class ProgressionObjectiveDefinition
{
    final String id;
    final String title;
    final String methodId;
    final ProgressionObjectiveType type;


}

/** Current boss/raid identity plus Compass safety metadata. */
@Getter
final class PvmActivity
{
    String id;
    String name;
    boolean wilderness;
    boolean raid;
    boolean freeToPlay;
    RiskLevel riskLevel;
    boolean hardcoreSafeByDefault;

}

/** Complete evidence contract for encounters simple enough to verify locally. */
@RequiredArgsConstructor
@Getter
final class PvmEvidenceProfile
{
    final String activityId;
    final String weaponStyle;
    final List<String> accessItems;
    final int minimumFood;
    final int minimumRestoration;


}

/** Reviewable preparation evidence that may produce actions but never READY. */
@Getter
final class PvmPreparationProfile
{
    String activityId;
    String style;
    List<String> checks;
    String accountValue;
    String provenance;
    int attack, strength, defence, ranged, magic, prayer, slayer;
    String preferredStyle;
    String requiredQuest;
    boolean questMayBeInProgress;
    boolean requiresSupplies;


}

/** One per-character interaction/completion event retained for later learning. */
@Getter
@RequiredArgsConstructor
final class RecommendationHistoryEntry
{
    final String activityId;
    final String title;
    final RecommendationHistoryAction action;
    final long occurredAtMillis;


}

/** Verified self-source recipe/access route for one resource family. */
@Getter
final class ResourceDependency
{
    final int itemId;
    final String itemName;
    final String action;
    final int opportunityCost;
    final int outputQuantity;
    final List<DependencyRequirement> prerequisites;

    public ResourceDependency(int itemId, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependency(int itemId, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, outputQuantity, prerequisites);
    }

    public ResourceDependency(int itemId, String itemName, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, itemName, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependency(int itemId, String itemName, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this.itemId = itemId;
        this.itemName = itemName == null ? null : itemName.trim();
        this.action = action;
        this.opportunityCost = max(0, opportunityCost);
        this.outputQuantity = max(1, outputQuantity);
        this.prerequisites = unmodifiableList(prerequisites == null
                ? new ArrayList<>() : new ArrayList<>(prerequisites));
    }

}

/**
 * One exact consumed-input requirement after Compass has compared the plan
 * with usable account storage.
 *
 * <p>A reusable source is deliberately represented separately from owned
 * quantity. For example, an equipped elemental staff can satisfy an elemental
 * rune requirement without pretending the account owns millions of runes.</p>
 */
@Getter
final class ResourcePlanEntry
{
    final String name;
    final int itemId;
    final int required;
    final int usableOwned;
    final int missing;
    final int restrictedOwned;
    final String reusableSource;

    public ResourcePlanEntry(
            String name,
            int itemId,
            int required,
            int usableOwned,
            int missing,
            int restrictedOwned,
            String reusableSource)
    {
        this.name = name;
        this.itemId = itemId;
        this.required = max(0, required);
        this.usableOwned = max(0, usableOwned);
        this.missing = max(0, missing);
        this.restrictedOwned = max(0, restrictedOwned);
        this.reusableSource = reusableSource;
    }


    public boolean isSatisfied()
    {
        return missing <= 0;
    }

    public boolean isSatisfiedByReusableSource()
    {
        return reusableSource != null && !reusableSource.trim().isEmpty();
    }

    public MethodInput missingInput()
    {
        return new MethodInput(name, itemId, missing);
    }
}

/** A stable, human-readable acquisition family for common progression resources. */
@Getter
final class ResourceSource
{
    final String id;
    final List<String> nameTokens;
    final String mainRoute;
    final String ironRoute;
    final String uimRoute;
    final List<String> freeToPlayItemNames;
    final String freeToPlayMainRoute;
    final String freeToPlayIronRoute;
    final String freeToPlayUimRoute;
    final List<Source> sourceIds;
    final boolean wilderness;
    final RiskLevel riskLevel;

    public ResourceSource(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this(id, nameTokens, mainRoute, ironRoute, uimRoute,
                emptyList(), null, null, null,
                wilderness, riskLevel);
    }

    public ResourceSource(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            List<String> freeToPlayItemNames, String freeToPlayMainRoute,
            String freeToPlayIronRoute, String freeToPlayUimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this.id = id;
        this.nameTokens = unmodifiableList(nameTokens == null
                ? new ArrayList<>() : new ArrayList<>(nameTokens));
        this.mainRoute = mainRoute;
        this.ironRoute = ironRoute;
        this.uimRoute = uimRoute;
        this.freeToPlayItemNames = unmodifiableList(
                freeToPlayItemNames == null ? new ArrayList<>()
                        : new ArrayList<>(freeToPlayItemNames));
        this.freeToPlayMainRoute = freeToPlayMainRoute;
        this.freeToPlayIronRoute = freeToPlayIronRoute;
        this.freeToPlayUimRoute = freeToPlayUimRoute;
        this.sourceIds = unmodifiableList(
                freeToPlayItemNames == null || freeToPlayItemNames.isEmpty()
                        ? Arrays.asList(Source.GENERAL_SKILL_TRAINING,
                                Source.IRONMAN_GENERAL,
                                Source.UIM_GENERAL)
                        : Arrays.asList(Source.GENERAL_SKILL_TRAINING,
                                Source.IRONMAN_GENERAL,
                                Source.F2P_IRONMAN_GENERAL,
                                Source.UIM_GENERAL));
        this.wilderness = wilderness;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
    }

}

/** Verified Slayer-master mechanics plus property-driven strategic qualities. */
@RequiredArgsConstructor
final class SlayerMasterProfile
{
    @Getter
    final String id;
    @Getter
    final List<String> names;
    @Getter
    final String location;
    @Getter
    final int minimumCombat;
    @Getter
    final int minimumSlayer;
    @Getter
    final String requiredQuest;
    final boolean questStartSuffices;
    @Getter
    final int normalPoints;
    @Getter
    final int cancelCost;
    @Getter
    final int blockCost;
    @Getter
    final double experiencePotential;
    @Getter
    final double supplyValue;
    @Getter
    final double setupBurden;
    @Getter
    final double locationConstraint;
    @Getter
    final boolean wilderness;


    public String getDisplayName() { return names.get(0); }
    public boolean isQuestStartSufficient() { return questStartSuffices; }

    public int pointsForCompletion(int completedAfterTask)
    {
        return normalPoints * SlayerPointEconomy.pointMultiplier(completedAfterTask);
    }

}

/** Conservative task-specific Slayer knowledge without fake DPS precision. */
@Getter
final class SlayerTaskProfile
{
    final String id;
    final List<String> aliases;
    final List<String> requiredProtection;
    final String preferredLocation;
    final String styleGuidance;
    final String mechanicsNote;
    final Capability cannonEligibility;
    final Capability multiTargetMagicEligibility;
    final boolean wildernessVariantKnown;
    final List<String> ironObjectives;
    final String taskDecisionGuidance;

    public SlayerTaskProfile(
            String id,
            List<String> aliases,
            List<String> requiredProtection,
            String preferredLocation,
            String styleGuidance,
            String mechanicsNote)
    {
        this(id, aliases, requiredProtection, preferredLocation, styleGuidance,
                mechanicsNote, Capability.UNKNOWN, Capability.UNKNOWN,
                false, emptyList(), null);
    }

    public SlayerTaskProfile(String id, List<String> aliases,
            List<String> requiredProtection, String preferredLocation,
            String styleGuidance, String mechanicsNote,
            Capability cannonEligibility,
            Capability multiTargetMagicEligibility,
            boolean wildernessVariantKnown, List<String> ironObjectives,
            String taskDecisionGuidance)
    {
        this.id = id;
        this.aliases = immutable(aliases);
        this.requiredProtection = immutable(requiredProtection);
        this.preferredLocation = preferredLocation;
        this.styleGuidance = styleGuidance;
        this.mechanicsNote = mechanicsNote;
        this.cannonEligibility = cannonEligibility == null
                ? Capability.UNKNOWN : cannonEligibility;
        this.multiTargetMagicEligibility = multiTargetMagicEligibility == null
                ? Capability.UNKNOWN : multiTargetMagicEligibility;
        this.wildernessVariantKnown = wildernessVariantKnown;
        this.ironObjectives = immutable(ironObjectives);
        this.taskDecisionGuidance = taskDecisionGuidance == null
                ? Text.get(891)
                : taskDecisionGuidance;
    }


    private static List<String> immutable(List<String> values)
    {
        return values == null
                ? emptyList()
                : unmodifiableList(new ArrayList<>(values));
    }
}

/** Strategic task properties; decisions score these rather than task identities. */
final class SlayerStrategy
{
    @Getter
    final String taskProfileId;
    @Getter
    final int xpQuality;
    @Getter
    final int resourceValue;
    @Getter
    final int completionBurden;
    @Getter
    final int setupBurden;
    @Getter
    final AttentionLevel attention;
    @Getter
    final RiskLevel inherentRisk;
    @Getter
    final SlayerRequiredItemUse requiredItemUse;
    @Getter
    final CombatStyle requiredCombatStyle;
    final Map<String, Integer> assignmentWeights;
    @Getter
    final String alternativeActivityId;
    @Getter
    final String alternativeName;
    @Getter
    final String alternativeLocation;
    @Getter
    final boolean directEncounter;

    public SlayerStrategy(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation)
    {
        this(taskProfileId, xpQuality, resourceValue, completionBurden,
                setupBurden, attention, inherentRisk, requiredItemUse, null,
                assignmentWeights, alternativeActivityId, alternativeName,
                alternativeLocation, false);
    }

    public SlayerStrategy(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            CombatStyle requiredCombatStyle,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation)
    {
        this(taskProfileId, xpQuality, resourceValue, completionBurden,
                setupBurden, attention, inherentRisk, requiredItemUse,
                requiredCombatStyle, assignmentWeights, alternativeActivityId,
                alternativeName, alternativeLocation, false);
    }

    public SlayerStrategy(String taskProfileId, int xpQuality,
            int resourceValue, int completionBurden, int setupBurden,
            AttentionLevel attention, RiskLevel inherentRisk,
            SlayerRequiredItemUse requiredItemUse,
            CombatStyle requiredCombatStyle,
            Map<String, Integer> assignmentWeights,
            String alternativeActivityId, String alternativeName,
            String alternativeLocation, boolean directEncounter)
    {
        this.taskProfileId = taskProfileId;
        this.xpQuality = scale(xpQuality);
        this.resourceValue = scale(resourceValue);
        this.completionBurden = scale(completionBurden);
        this.setupBurden = scale(setupBurden);
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.inherentRisk = inherentRisk == null ? RiskLevel.LOW : inherentRisk;
        this.requiredItemUse = requiredItemUse == null
                ? SlayerRequiredItemUse.CARRIED_OR_EQUIPPED : requiredItemUse;
        this.requiredCombatStyle = requiredCombatStyle;
        this.assignmentWeights = unmodifiableMap(
                assignmentWeights == null ? emptyMap()
                        : new HashMap<>(assignmentWeights));
        this.alternativeActivityId = alternativeActivityId;
        this.alternativeName = alternativeName;
        this.alternativeLocation = alternativeLocation;
        this.directEncounter = directEncounter;
    }


    public Integer weightFor(String masterId)
    {
        return assignmentWeights.get(Names.slug(masterId));
    }

    private static int scale(int value)
    {
        return max(1, min(5, value));
    }

}

/** Exact, deterministic evidence that proves a destination route is usable. */
@RequiredArgsConstructor
@Getter
final class RouteEvidence
{
    final String routeId;
    final String requiredCompletedQuest;
    final List<String> requiredItems;


}

/**
 * Reviewed local mechanics for one UIM retrieval/storage system. These facts
 * do not prove that the live character has access or that a particular item is
 * compatible; {@link UimCapabilityService} still requires both observations.
 */
@RequiredArgsConstructor
@Getter
final class UimStorageMechanicProfile
{
    final StorageKind capability;
    final String location;
    final String accessRequirements;
    final String eligibleItems;
    final String insertionOrDepositRules;
    final String retrievalRules;
    final String cost;
    final String expiration;
    final String secondDeathBehavior;
    final RiskLevel risk;
    final Source source;
    final boolean recommendationEligible;

    public boolean hasCompleteRecommendationRules()
    {
        return recommendationEligible && capability != null && source != null
                && text(location) && text(accessRequirements)
                && text(eligibleItems) && text(insertionOrDepositRules)
                && text(retrievalRules) && text(cost) && text(expiration)
                && text(secondDeathBehavior);
    }

    private static boolean text(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}

/** Evidence for one possible resolution of a plan-specific UIM slot shortfall. */
@RequiredArgsConstructor
@Getter
final class UimStorageOption
{
    final StorageKind capability;
    final Capability itemCompatibility;
    final Capability capacityOrPreconditions;
    final boolean requiresConstruction;
    final Priority recurringInfrastructureValue;
    final boolean majorProgressionTransition;


}
