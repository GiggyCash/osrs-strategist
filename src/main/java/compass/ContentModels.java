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

/** Sourced strategic properties shared by non-skill candidate families. */
@RequiredArgsConstructor
@Getter
final class ActivityStrategyProfile
{
    final String candidatePrefix;
    @Getter(AccessLevel.NONE)
    final Set<AccountMode> accountModes;
    final InventoryFootprint inventoryFootprint;
    final double setupReuse;
    final String strategicReason;
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
    String id;
    String name;
    Skill primarySkill;
    int minimumLevel;
    boolean freeToPlay;
    RiskLevel riskLevel;
    AttentionLevel attention;
    Set<AccountMode> supportedModes;
    String rewardFocus;
    boolean combatActivity;

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
    int itemId;
    String itemName;
    String action;
    int opportunityCost;
    int outputQuantity;
    List<DependencyRequirement> prerequisites;

}

/** A stable, human-readable acquisition family for common progression resources. */
@Getter
final class ResourceSource
{
    String id;
    List<String> nameTokens;
    String mainRoute;
    String ironRoute;
    String uimRoute;
    List<String> freeToPlayItemNames;
    String freeToPlayMainRoute;
    String freeToPlayIronRoute;
    String freeToPlayUimRoute;
    List<Source> sourceIds;
    boolean wilderness;
    RiskLevel riskLevel;

}

/** Verified Slayer-master mechanics plus property-driven strategic qualities. */
@RequiredArgsConstructor
@Getter
final class SlayerMasterProfile
{
    final String id;
    final List<String> names;
    final String location;
    final int minimumCombat;
    final int minimumSlayer;
    final String requiredQuest;
    @Getter(AccessLevel.NONE)
    final boolean questStartSuffices;
    final int normalPoints;
    final int cancelCost;
    final int blockCost;
    final double experiencePotential;
    final double supplyValue;
    final double setupBurden;
    final double locationConstraint;
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
    String id;
    List<String> aliases;
    List<String> requiredProtection;
    String preferredLocation;
    String styleGuidance;
    String mechanicsNote;
    Capability cannonEligibility;
    Capability multiTargetMagicEligibility;
    boolean wildernessVariantKnown;
    List<String> ironObjectives;
    String taskDecisionGuidance;
}

/** Strategic task properties; decisions score these rather than task identities. */
@Getter
final class SlayerStrategy
{
    String taskProfileId;
    int xpQuality;
    int resourceValue;
    int completionBurden;
    int setupBurden;
    AttentionLevel attention;
    RiskLevel inherentRisk;
    SlayerRequiredItemUse requiredItemUse;
    CombatStyle requiredCombatStyle;
    @Getter(AccessLevel.NONE)
    Map<String, Integer> assignmentWeights;
    String alternativeActivityId;
    String alternativeName;
    String alternativeLocation;
    boolean directEncounter;


    public Integer weightFor(String masterId)
    {
        return assignmentWeights.get(Names.slug(masterId));
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
