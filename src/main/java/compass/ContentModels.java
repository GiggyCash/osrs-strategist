package compass;

import java.util.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;

/** Verified prerequisites for a prayer, spell, or spellbook unlock. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
final class AbilityUnlockDefinition
{
    final String id;
    private final String name;
    private final GoalNodeKind kind;
    private final String quest;
    private final Skill skill;
    private final int level;
    private final Skill secondarySkill;
    private final int secondaryLevel;
    private final String requiredItem;
    private final String encounterId;
    private final String accessCheck;


}

/** Immutable, exhaustive account strategic-priority profile. */
final class AccountStrategicPriorityProfile
{
    @Getter
    private final AccountMode accountMode;
    @Getter
    private final Map<AccountStrategicDimension, AccountStrategicPriority>
            priorities;

    AccountStrategicPriorityProfile(AccountMode accountMode,
            Map<AccountStrategicDimension, AccountStrategicPriority> values)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        EnumMap<AccountStrategicDimension, AccountStrategicPriority> copy =
                new EnumMap<>(AccountStrategicDimension.class);
        if (values != null) copy.putAll(values);
        for (AccountStrategicDimension dimension
                : AccountStrategicDimension.values())
        {
            if (!copy.containsKey(dimension))
                throw new IllegalArgumentException(
                        Text.get(1110) + dimension);
        }
        this.priorities = Collections.unmodifiableMap(copy);
    }


    public AccountStrategicPriority get(AccountStrategicDimension dimension)
    {
        return priorities.get(dimension);
    }

    public StrategicPriority priorityOf(AccountStrategicDimension dimension)
    {
        var value = get(dimension);
        return value == null ? StrategicPriority.NONE : value.getPriority();
    }

}

/** Sourced strategic properties shared by non-skill candidate families. */
@RequiredArgsConstructor
final class ActivityStrategyProfile
{
    @Getter
    private final String candidatePrefix;
    private final Set<AccountMode> accountModes;
    @Getter
    private final MethodInventoryFootprint inventoryFootprint;
    @Getter
    private final double setupReuse;
    @Getter
    private final String strategicReason;
    @Getter
    private final List<StrategySourceId> sources;


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}

@Getter
@RequiredArgsConstructor
final class AgilityCourseDefinition
{
    final String id;
    private final String displayName;
    private final int requiredLevel;
    private final int regionId;
    private final String requiredQuest;
    private final boolean wilderness;



    public String observationKey()
    {
        return "region." + regionId;
    }
}

@Getter
final class DiaryTaskDefinition
{
    private final String region;
    private final DiaryTier tier;
    private final String task;
    private final List<DiaryTaskRequirement> requirements;

    DiaryTaskDefinition(String region, DiaryTier tier, String task,
            List<DiaryTaskRequirement> requirements)
    {
        this.region = region;
        this.tier = tier;
        this.task = task;
        this.requirements = Collections.unmodifiableList(
                new ArrayList<>(requirements));
    }

    public String getId()
    {
        return "diary-task:" + Names.slug(region) + ":"
                + tier.name().toLowerCase(Locale.ROOT) + ":" + Names.slug(task);
    }
    public boolean isTransportRelevant()
    {
        var value = task.toLowerCase(Locale.ROOT);
        return value.contains("teleport") || value.contains("travel")
                || value.contains("fairy ring") || value.contains("glider")
                || value.contains("balloon") || value.contains("boat")
                || value.contains("minecart");
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
    private final String displayName;
    private final Set<Integer> regionIds;
    private final String requiredQuest;
    private final boolean herbPatch;



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
    private final String displayName;
    private final FarmingPatchKind kind;
    private final int minimumLevel;
    private final Set<Integer> regionIds;
    private final int varbitId;
    private final String requiredQuest;



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
    private final String itemName;
    private final CombatStyle style;
    private final boolean tradeable;
    private final List<GearAcquisitionStep> steps;
    private final String valueRule;
    private final String provenance;


}

/** One contextual gear tier. Names stay data-driven until ownership is verified by item IDs. */
@RequiredArgsConstructor
@Getter
final class GearProgressionEntry
{
    final String id;
    private final String contextId;
    private final CombatStyle style;
    private final GearBudgetTier tier;
    private final List<String> recommendedItems;
    private final String weaponGuidance;
    private final String note;
    private final boolean freeToPlay;
    private final boolean selfSourceFriendly;
    private final boolean uimFriendly;
    private final boolean hardcoreSafe;


}

/** One material rule consumed by a deterministic training action. */
@RequiredArgsConstructor
@Getter
final class MethodInputRule
{
    private final MethodProfile.InputMode mode;
    private final String fixedName;
    private final double quantityPerAction;


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
    private final String name;
    private final int ordinaryTravelBurden;
    private final String advantageousRouteId;
    private final int verifiedRouteTravelBurden;
    private final boolean membersOnly;
    private final boolean wilderness;



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

    boolean usesVerifiedRoute(TransportSnapshot transport)
    {
        return advantageousRouteId != null && transport != null
                && transport.hasVerifiedRoute(advantageousRouteId);
    }

}

/** Data descriptor connecting a training method to legal concrete locations. */
@RequiredArgsConstructor
@Getter
final class MethodLocationProfile
{
    private final String methodId;
    private final List<MethodLocationOption> locations;
    private final String sourceUrl;


}

/** Sourced strategic properties layered over a mechanically legal method. */
@RequiredArgsConstructor
@Getter
final class MethodStrategyProfile
{
    private final String methodId;
    private final StrategyKnowledgeTier tier;
    private final Set<AccountMode> accountModes;
    private final MethodBankingBehavior bankingBehavior;
    private final MethodInventoryFootprint inventoryFootprint;
    private final double accountValueFit;
    private final String playerReason;
    private final List<StrategySourceId> sources;


    public boolean supports(AccountMode mode) { return accountModes.contains(mode); }
}

@Getter
final class MinigameDefinition
{
    final String id;
    private final String name;
    private final Skill primarySkill;
    private final int minimumLevel;
    private final boolean freeToPlay;
    private final RiskLevel riskLevel;
    private final AttentionLevel attention;
    private final Set<AccountMode> supportedModes;
    private final String rewardFocus;
    private final boolean combatActivity;

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
        this.minimumLevel = Math.max(1, minimumLevel);
        this.freeToPlay = freeToPlay;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.supportedModes = supportedModes == null || supportedModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supportedModes));
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
    private final String activityId;
    private final ItemRequirementExpression items;
    private final String location;
    private final String supplies;
    private final String instructions;


}

/** Stable strategy metadata for a money/resource-producing activity. */
@RequiredArgsConstructor
@Getter
final class MoneyMakingDefinition
{
    final String id;
    private final String name;
    private final String description;
    private final Skill primarySkill;
    private final int minimumLevel;
    private final boolean freeToPlay;
    private final Set<AccountMode> supportedModes;
    private final RiskLevel riskLevel;
    private final AttentionLevel attention;
    private final boolean wilderness;
    private final boolean requiresLivePrices;



    public boolean supports(AccountMode mode)
    {
        return mode != null && supportedModes.contains(mode);
    }
}

/** Per-character planning preferences that survive logout/restart. */
@Getter
class PlayerStrategyProfile
{
    private final StrategyMode strategyMode;
    private final SessionIntent sessionIntent;
    private final QuestTolerance questTolerance;
    private final GoalType activeGoal;
    private final boolean useGroupStorage;
    private final boolean collectionistMode;
    private final boolean allowWildernessMethods;

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
    QuestTolerance tolerance() { return questTolerance; }
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
    private final String title;
    private final String methodId;
    private final ProgressionObjectiveType type;


}

/**
 * Per-character list of items Compass must never suggest selling, dropping,
 * alching, destroying, or otherwise consuming as an "obsolete" resource.
 *
 * <p>Built-in protection rules for quest/rare/progression items will eventually
 * be layered on top of this explicit player list.</p>
 */
final class ProtectedItemProfile
{
    private final Set<Integer> protectedItemIds = new HashSet<>();

    public boolean isProtected(int itemId)
    {
        return protectedItemIds.contains(itemId);
    }

    public void protect(int itemId)
    {
        if (itemId >= 0)
        {
            protectedItemIds.add(itemId);
        }
    }

    public void unprotect(int itemId)
    {
        protectedItemIds.remove(itemId);
    }

    public void replaceAll(Set<Integer> itemIds)
    {
        protectedItemIds.clear();

        if (itemIds == null)
        {
            return;
        }

        for (Integer itemId : itemIds)
        {
            if (itemId != null && itemId >= 0)
            {
                protectedItemIds.add(itemId);
            }
        }
    }

    public Set<Integer> snapshot()
    {
        return Collections.unmodifiableSet(
                new HashSet<>(protectedItemIds)
        );
    }
}

/** Current boss/raid identity plus Compass safety metadata. */
@Getter
final class PvmActivityDefinition
{
    String id;
    private String name;
    private boolean wilderness;
    private boolean raid;
    private boolean freeToPlay;
    private RiskLevel riskLevel;
    private boolean hardcoreSafeByDefault;

}

/** Complete evidence contract for encounters simple enough to verify locally. */
@RequiredArgsConstructor
@Getter
final class PvmEvidenceProfile
{
    private final String activityId;
    private final String weaponStyle;
    private final List<String> accessItems;
    private final int minimumFood;
    private final int minimumRestoration;


}

/** Reviewable preparation evidence that may produce actions but never READY. */
@Getter
final class PvmPreparationProfile
{
    private String activityId;
    private String style;
    private List<String> checks;
    private String accountValue;
    private String provenance;
    private int attack, strength, defence, ranged, magic, prayer, slayer;
    private String preferredStyle;
    private String requiredQuest;
    private boolean questMayBeInProgress;
    private boolean requiresSupplies;


}

/** One per-character interaction/completion event retained for later learning. */
@Getter
@RequiredArgsConstructor
final class RecommendationHistoryEntry
{
    private final String activityId;
    private final String title;
    private final RecommendationHistoryAction action;
    private final long occurredAtMillis;


}

/** Verified self-source recipe/access route for one resource family. */
@Getter
final class ResourceDependencyDefinition
{
    private final int itemId;
    private final String itemName;
    private final String action;
    private final int opportunityCost;
    private final int outputQuantity;
    private final List<DependencyRequirement> prerequisites;

    public ResourceDependencyDefinition(int itemId, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this(itemId, null, action, opportunityCost, outputQuantity, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String itemName, String action,
            int opportunityCost, List<DependencyRequirement> prerequisites)
    {
        this(itemId, itemName, action, opportunityCost, 1, prerequisites);
    }

    public ResourceDependencyDefinition(int itemId, String itemName, String action,
            int opportunityCost, int outputQuantity,
            List<DependencyRequirement> prerequisites)
    {
        this.itemId = itemId;
        this.itemName = itemName == null ? null : itemName.trim();
        this.action = action;
        this.opportunityCost = Math.max(0, opportunityCost);
        this.outputQuantity = Math.max(1, outputQuantity);
        this.prerequisites = Collections.unmodifiableList(prerequisites == null
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
    private final String name;
    private final int itemId;
    private final int required;
    private final int usableOwned;
    private final int missing;
    private final int restrictedOwned;
    private final String reusableSource;

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
        this.required = Math.max(0, required);
        this.usableOwned = Math.max(0, usableOwned);
        this.missing = Math.max(0, missing);
        this.restrictedOwned = Math.max(0, restrictedOwned);
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
final class ResourceSourceDefinition
{
    final String id;
    private final List<String> nameTokens;
    private final String mainRoute;
    private final String ironRoute;
    private final String uimRoute;
    private final List<String> freeToPlayItemNames;
    private final String freeToPlayMainRoute;
    private final String freeToPlayIronRoute;
    private final String freeToPlayUimRoute;
    private final List<StrategySourceId> sourceIds;
    private final boolean wilderness;
    private final RiskLevel riskLevel;

    public ResourceSourceDefinition(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this(id, nameTokens, mainRoute, ironRoute, uimRoute,
                Collections.emptyList(), null, null, null,
                wilderness, riskLevel);
    }

    public ResourceSourceDefinition(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            List<String> freeToPlayItemNames, String freeToPlayMainRoute,
            String freeToPlayIronRoute, String freeToPlayUimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this.id = id;
        this.nameTokens = Collections.unmodifiableList(nameTokens == null
                ? new ArrayList<>() : new ArrayList<>(nameTokens));
        this.mainRoute = mainRoute;
        this.ironRoute = ironRoute;
        this.uimRoute = uimRoute;
        this.freeToPlayItemNames = Collections.unmodifiableList(
                freeToPlayItemNames == null ? new ArrayList<>()
                        : new ArrayList<>(freeToPlayItemNames));
        this.freeToPlayMainRoute = freeToPlayMainRoute;
        this.freeToPlayIronRoute = freeToPlayIronRoute;
        this.freeToPlayUimRoute = freeToPlayUimRoute;
        this.sourceIds = Collections.unmodifiableList(
                freeToPlayItemNames == null || freeToPlayItemNames.isEmpty()
                        ? Arrays.asList(StrategySourceId.GENERAL_SKILL_TRAINING,
                                StrategySourceId.IRONMAN_GENERAL,
                                StrategySourceId.UIM_GENERAL)
                        : Arrays.asList(StrategySourceId.GENERAL_SKILL_TRAINING,
                                StrategySourceId.IRONMAN_GENERAL,
                                StrategySourceId.F2P_IRONMAN_GENERAL,
                                StrategySourceId.UIM_GENERAL));
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
    private final List<String> names;
    @Getter
    private final String location;
    @Getter
    private final int minimumCombat;
    @Getter
    private final int minimumSlayer;
    @Getter
    private final String requiredQuest;
    private final boolean questStartSuffices;
    @Getter
    private final int normalPoints;
    @Getter
    private final int cancelCost;
    @Getter
    private final int blockCost;
    @Getter
    private final double experiencePotential;
    @Getter
    private final double supplyValue;
    @Getter
    private final double setupBurden;
    @Getter
    private final double locationConstraint;
    @Getter
    private final boolean wilderness;


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
    private final List<String> aliases;
    private final List<String> requiredProtection;
    private final String preferredLocation;
    private final String styleGuidance;
    private final String mechanicsNote;
    private final CapabilityState cannonEligibility;
    private final CapabilityState multiTargetMagicEligibility;
    private final boolean wildernessVariantKnown;
    private final List<String> ironObjectives;
    private final String taskDecisionGuidance;

    public SlayerTaskProfile(
            String id,
            List<String> aliases,
            List<String> requiredProtection,
            String preferredLocation,
            String styleGuidance,
            String mechanicsNote)
    {
        this(id, aliases, requiredProtection, preferredLocation, styleGuidance,
                mechanicsNote, CapabilityState.UNKNOWN, CapabilityState.UNKNOWN,
                false, Collections.emptyList(), null);
    }

    public SlayerTaskProfile(String id, List<String> aliases,
            List<String> requiredProtection, String preferredLocation,
            String styleGuidance, String mechanicsNote,
            CapabilityState cannonEligibility,
            CapabilityState multiTargetMagicEligibility,
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
                ? CapabilityState.UNKNOWN : cannonEligibility;
        this.multiTargetMagicEligibility = multiTargetMagicEligibility == null
                ? CapabilityState.UNKNOWN : multiTargetMagicEligibility;
        this.wildernessVariantKnown = wildernessVariantKnown;
        this.ironObjectives = immutable(ironObjectives);
        this.taskDecisionGuidance = taskDecisionGuidance == null
                ? Text.get(891)
                : taskDecisionGuidance;
    }


    private static List<String> immutable(List<String> values)
    {
        return values == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}

/** Strategic task properties; decisions score these rather than task identities. */
final class SlayerTaskStrategicProfile
{
    @Getter
    private final String taskProfileId;
    @Getter
    private final int xpQuality;
    @Getter
    private final int resourceValue;
    @Getter
    private final int completionBurden;
    @Getter
    private final int setupBurden;
    @Getter
    private final AttentionLevel attention;
    @Getter
    private final RiskLevel inherentRisk;
    @Getter
    private final SlayerRequiredItemUse requiredItemUse;
    @Getter
    private final CombatStyle requiredCombatStyle;
    private final Map<String, Integer> assignmentWeights;
    @Getter
    private final String alternativeActivityId;
    @Getter
    private final String alternativeName;
    @Getter
    private final String alternativeLocation;
    @Getter
    private final boolean directEncounter;

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
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

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
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

    public SlayerTaskStrategicProfile(String taskProfileId, int xpQuality,
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
        this.assignmentWeights = Collections.unmodifiableMap(
                assignmentWeights == null ? Collections.emptyMap()
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
        return Math.max(1, Math.min(5, value));
    }

}

/** Exact, deterministic evidence that proves a destination route is usable. */
@RequiredArgsConstructor
@Getter
final class TravelRouteEvidenceDefinition
{
    private final String routeId;
    private final String requiredCompletedQuest;
    private final List<String> requiredItems;


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
    private final StorageCapability capability;
    private final String location;
    private final String accessRequirements;
    private final String eligibleItems;
    private final String insertionOrDepositRules;
    private final String retrievalRules;
    private final String cost;
    private final String expiration;
    private final String secondDeathBehavior;
    private final RiskLevel risk;
    private final StrategySourceId source;
    private final boolean recommendationEligible;



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
    private final StorageCapability capability;
    private final CapabilityState itemCompatibility;
    private final CapabilityState capacityOrPreconditions;
    private final boolean requiresConstruction;
    private final StrategicPriority recurringInfrastructureValue;
    private final boolean majorProgressionTransition;


}
