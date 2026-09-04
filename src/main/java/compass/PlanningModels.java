package compass;
import static java.lang.Math.*;
import static java.util.Collections.*;

import static compass.Text.get;

import java.util.*;
import lombok.*;
import net.runelite.api.*;

/** Typed prerequisite in a resource acquisition route. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class DependencyRequirement
{
    public enum Kind { RESOURCE, QUEST, SKILL, GEAR }

    final String id;
    final String label;
    final Kind kind;
    final ResourceNeed resource;
    final Skill skill;
    final int level;


    public static DependencyRequirement resource(ResourceNeed need)
    {
        return new DependencyRequirement("resource:" + need.itemId,
                need.itemName, Kind.RESOURCE, need, null, 0);
    }

    public static DependencyRequirement quest(String name)
    {
        return new DependencyRequirement("quest:" + normalize(name), name,
                Kind.QUEST, null, null, 0);
    }

    public static DependencyRequirement skill(Skill skill, int level)
    {
        return new DependencyRequirement("skill:" + skill.name().toLowerCase(),
                skill.getName() + " " + level, Kind.SKILL, null, skill, level);
    }

    public static DependencyRequirement gear(String name)
    {
        return new DependencyRequirement("gear:" + normalize(name), name,
                Kind.GEAR, null, null, 0);
    }


    private static String normalize(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}

/** Finite traversal result with explicit termination diagnostics. */
@Getter
final class DependencyResolution
{
    final List<ResolvedDependencyNode> nodes;
    final boolean cycleDetected;
    final boolean depthLimited;
    final boolean opportunityCostRejected;
    final boolean nodeLimited;

    public DependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected, boolean nodeLimited)
    {
        this.nodes = unmodifiableList(new ArrayList<>(nodes));
        this.cycleDetected = cycleDetected;
        this.depthLimited = depthLimited;
        this.opportunityCostRejected = opportunityCostRejected;
        this.nodeLimited = nodeLimited;
    }

    public ResolvedDependencyNode nextAction()
    {
        for (ResolvedDependencyNode node : nodes)
            if (node.confidence != Confidence.VERIFIED) return node;
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}

@Getter
final class DiaryTaskRequirement
{
    public enum Kind { SKILL, QUEST, COMBAT_LEVEL, QUEST_POINTS, ALTERNATIVE_CHECK }

    final Kind kind;
    final Skill skill;
    final int level;
    final String quest;
    final boolean startedOnly;
    final String check;

    private DiaryTaskRequirement(Kind kind, Skill skill, int level,
            String quest, boolean startedOnly, String check)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = max(0, level);
        this.quest = quest;
        this.startedOnly = startedOnly;
        this.check = check;
    }

    public static DiaryTaskRequirement skill(Skill skill, int level)
    {
        return new DiaryTaskRequirement(Kind.SKILL, skill, level,
                null, false, null);
    }

    public static DiaryTaskRequirement quest(String quest, boolean startedOnly)
    {
        return new DiaryTaskRequirement(Kind.QUEST, null, 0,
                quest, startedOnly, null);
    }

    public static DiaryTaskRequirement combat(int level)
    {
        return new DiaryTaskRequirement(Kind.COMBAT_LEVEL, null, level,
                null, false, null);
    }

    public static DiaryTaskRequirement questPoints(int points)
    {
        return new DiaryTaskRequirement(Kind.QUEST_POINTS, null, points,
                null, false, null);
    }

    public static DiaryTaskRequirement alternative(String check)
    {
        return new DiaryTaskRequirement(Kind.ALTERNATIVE_CHECK, null, 0,
                null, false, check);
    }

}

/** One reusable edge in a gear acquisition chain. */
@Getter
@RequiredArgsConstructor
final class GearAcquisitionStep
{
    public enum Kind { QUEST, SKILL, BOSS, MINIGAME, RESOURCE, SHOP, VERIFY }

    final Kind kind;
    final String target;
    final String action;


}

@Getter
final class GuidanceStep
{
    final String id;
    final String label;
    final String detail;
    final GuidanceStepState state;

    public GuidanceStep(
            String id,
            String label,
            String detail,
            GuidanceStepState state)
    {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.state = state == null ? GuidanceStepState.CHECK_NEEDED : state;
    }

    public boolean isComplete() { return state == GuidanceStepState.COMPLETE; }
}

/** Typed result suitable for a future candidate strategic-value payload. */
@Getter
final class InfraAssessment
{
    final InfrastructureMilestone milestone;
    final InfrastructureMilestoneState state;
    final Confidence confidence;
    final Priority strategicValue;
    final String reason;

    InfraAssessment(InfrastructureMilestone milestone,
            InfrastructureMilestoneState state,
            Confidence confidence,
            Priority strategicValue,
            String reason)
    {
        this.milestone = milestone;
        this.state = state;
        this.confidence = confidence;
        this.strategicValue = strategicValue;
        this.reason = reason == null ? "" : reason;
    }

    public boolean canRecommendAcquisition()
    {
        return state == InfrastructureMilestoneState.ACTIONABLE
                && confidence == Confidence.VERIFIED;
    }
}

/** Result of evaluating a composable item requirement against observed state. */
@Getter
final class ItemRequirementResult
{
    final RequirementState state;
    final String action;
    final List<MethodInput> missingInputs;

    public ItemRequirementResult(RequirementState state, String action)
    {
        this(state, action, emptyList());
    }

    public ItemRequirementResult(RequirementState state, String action,
            List<MethodInput> missingInputs)
    {
        this.state = state;
        this.action = action == null ? "" : action;
        this.missingInputs = unmodifiableList(missingInputs == null
                ? new ArrayList<>() : new ArrayList<>(missingInputs));
    }

    /** Exact, evidence-backed shortfalls only. Unknown storage never appears here. */
    public boolean isSatisfied() { return state == RequirementState.VERIFIED; }
}

/** One meaningful account progression event suitable for a session recap. */
@Getter
final class ProgressMilestone
{
    final String id;
    final ProgressMilestoneType type;
    final String title;
    final String detail;
    final String goalId;
    final long occurredAtMillis;

    public ProgressMilestone(
            String id,
            ProgressMilestoneType type,
            String title,
            String detail,
            String goalId,
            long occurredAtMillis)
    {
        if (id == null || id.trim().isEmpty() || type == null
                || title == null || title.trim().isEmpty())
        {
            throw new IllegalArgumentException(get(1156));
        }
        this.id = id;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.goalId = goalId;
        this.occurredAtMillis = max(0L, occurredAtMillis);
    }

}

/** The skill checkpoint currently being executed by the active plan. */
@Getter
final class ProgressTarget
{
    final String activityId;
    final String methodId;
    final Skill skill;
    final int targetLevel;
    final int targetXp;

    public ProgressTarget(
            String activityId,
            String methodId,
            Skill skill,
            int targetLevel)
    {
        if (skill == null || targetLevel < 2 || targetLevel > 126)
        {
            throw new IllegalArgumentException(get(1157));
        }
        this.activityId = activityId;
        this.methodId = methodId;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.targetXp = Experience.getXpForLevel(targetLevel);
    }

}

/** Remaining work and ETA for the current skill target. */
@Getter
final class TargetProjection
{
    public enum State
    {
        NO_TARGET,
        CALCULATING,
        READY,
        COMPLETE
    }

    final State state;
    final ProgressTarget target;
    final int xpRemaining;
    final long etaMillis;

    private TargetProjection(
            State state,
            ProgressTarget target,
            int xpRemaining,
            long etaMillis)
    {
        this.state = state;
        this.target = target;
        this.xpRemaining = max(0, xpRemaining);
        this.etaMillis = max(0L, etaMillis);
    }

    public static TargetProjection noTarget()
    {
        return new TargetProjection(State.NO_TARGET, null, 0, 0L);
    }

    public static TargetProjection calculating(
            ProgressTarget target, int xpRemaining)
    {
        return new TargetProjection(State.CALCULATING, target,
                xpRemaining, 0L);
    }

    public static TargetProjection ready(
            ProgressTarget target, int xpRemaining, long etaMillis)
    {
        return new TargetProjection(State.READY, target,
                xpRemaining, etaMillis);
    }

    public static TargetProjection complete(ProgressTarget target)
    {
        return new TargetProjection(State.COMPLETE, target, 0, 0L);
    }

}

/** Actionability result for one fully identified quest. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
final class QuestResolution
{
    final Confidence confidence;
    final Guidance guidance;
    final String reason;
    final Safety safetyEvidence;

}

/**
 * A reusable observed-resource requirement. itemIds are alternatives: quantities
 * across every listed ID are summed toward the requirement.
 */
@Getter
final class ResourceRequirement
{
    final String id;
    final String label;
    final int requiredQuantity;
    final int[] itemIds;

    public ResourceRequirement(
            String id,
            String label,
            int requiredQuantity,
            int... itemIds)
    {
        this.id = id;
        this.label = label;
        this.requiredQuantity = max(1, requiredQuantity);
        this.itemIds = itemIds == null ? new int[0] : Arrays.copyOf(itemIds, itemIds.length);
    }

    public int[] getItemIds() { return Arrays.copyOf(itemIds, itemIds.length); }
}

/** Structured access/build evidence consumed by the final recommendation gate. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
final class Safety
{
    public enum Access
    {
        F2P_SAFE,
        MEMBERS_ONLY,
        UNKNOWN
    }

    public enum BuildEffect
    {
        HARMLESS,
        SKILL_XP,
        VERIFIED_SAFE,
        POTENTIALLY_IRREVERSIBLE,
        UNKNOWN
    }

    final Access access;
    final BuildEffect buildEffect;
    final Skill affectedSkill;
    final boolean conventionalBankRequired;

    public static Safety unknown()
    {
        return new Safety(Access.UNKNOWN, BuildEffect.UNKNOWN,
                null, false);
    }

    public static Safety harmless(boolean freeToPlay)
    {
        return new Safety(access(freeToPlay),
                BuildEffect.HARMLESS, null, false);
    }

    public static Safety skill(boolean freeToPlay, Skill skill)
    {
        return new Safety(access(freeToPlay),
                BuildEffect.SKILL_XP, skill, false);
    }

    public static Safety verifiedSafe(boolean freeToPlay)
    {
        return new Safety(access(freeToPlay),
                BuildEffect.VERIFIED_SAFE, null, false);
    }

    public static Safety potentiallyIrreversible(boolean freeToPlay)
    {
        return new Safety(access(freeToPlay),
                BuildEffect.POTENTIALLY_IRREVERSIBLE, null, false);
    }

    public Safety requiringConventionalBank()
    {
        return new Safety(access, buildEffect,
                affectedSkill, true);
    }

    private static Access access(boolean freeToPlay)
    {
        return freeToPlay ? Access.F2P_SAFE : Access.MEMBERS_ONLY;
    }

}

/** Immutable explanation and execution payload from the Slayer strategist. */
@Getter
final class SlayerDecisionResult
{
    final SlayerState assignmentState;
    final SlayerDecision decision;
    final SlayerMasterProfile master;
    final SlayerStrategy taskProfile;
    final double score;
    final Confidence confidence;
    final String reason;
    final Guidance guidance;
    final String selectedAlternativeName;
    final SlayerReward recommendedReward;
    final SlayerTaskOffer recommendedOffer;

    public SlayerDecisionResult(SlayerState assignmentState,
            SlayerDecision decision, SlayerMasterProfile master,
            SlayerStrategy taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward, SlayerTaskOffer recommendedOffer)
    {
        this.assignmentState = assignmentState == null
                ? SlayerState.UNKNOWN : assignmentState;
        this.decision = decision;
        this.master = master;
        this.taskProfile = taskProfile;
        this.score = score;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
        this.reason = reason;
        this.guidance = guidance;
        this.selectedAlternativeName = selectedAlternativeName;
        this.recommendedReward = recommendedReward;
        this.recommendedOffer = recommendedOffer;
    }

}

/** Ordered plan retained across recommendation refreshes for one account/goal. */
final class StrategicPlan
{
    @Getter
    final GoalType goal;
    final long accountHash;
    final String playerName;
    final AccountMode accountMode;
    final Membership membership;
    @Getter
    final List<StrategicPlanStep> steps;
    @Getter
    final int currentIndex;
    @Getter
    final long createdAtMillis;

    public StrategicPlan(
            GoalType goal,
            AccountSnapshot account,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        this(goal,
                account == null ? 0L : account.accountHash,
                account == null ? "" : account.playerName,
                account == null ? AccountMode.UNKNOWN
                        : AccountMode.fromTypeCode(account.modeCode()),
                account == null ? Membership.UNKNOWN
                        : account.membership(),
                steps, currentIndex, createdAtMillis);
    }

    private StrategicPlan(
            GoalType goal,
            long accountHash,
            String playerName,
            AccountMode accountMode,
            Membership membership,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || steps == null || steps.isEmpty())
            throw new IllegalArgumentException(
                    get(791));
        this.goal = goal;
        this.accountHash = accountHash;
        this.playerName = playerName == null ? "" : playerName;
        this.accountMode = accountMode == null
                ? AccountMode.UNKNOWN : accountMode;
        this.membership = membership == null
                ? Membership.UNKNOWN : membership;
        this.steps = unmodifiableList(new ArrayList<>(steps));
        this.currentIndex = max(0,
                min(currentIndex, this.steps.size() - 1));
        this.createdAtMillis = max(0L, createdAtMillis);
    }

    public StrategicPlan advanceCompleted(GameData data)
    {
        var next = currentIndex;
        while (next < steps.size() - 1 && steps.get(next).isComplete(data))
            next++;
        if (next == currentIndex) return this;
        return copyAt(next, createdAtMillis);
    }

    StrategicPlan copyAt(int index, long createdAt)
    {
        return new StrategicPlan(goal, accountHash, playerName, accountMode,
                membership, steps, index, createdAt);
    }

    public boolean matchesContext(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null
                || goal != context.goal()) return false;
        var account = context.data().account();
        if (accountHash != 0L && account.accountHash != 0L)
            return accountHash == account.accountHash
                    && accountMode == context.accountMode()
                    && membership == account.membership();
        return playerName != null && playerName.equals(account.playerName)
                && accountMode == context.accountMode()
                && membership == account.membership();
    }

    public StrategicPlanStep getCurrentStep() { return steps.get(currentIndex); }
    public StrategicPlanStep getNextStep()
    {
        return currentIndex + 1 < steps.size()
                ? steps.get(currentIndex + 1) : null;
    }
}

/** One ordered, evidence-backed transition in the active goal plan. */
@Getter
@EqualsAndHashCode
final class StrategicPlanStep
{
    final String id;
    final GoalNodeKind kind;
    final String objective;
    final String reason;
    final CompletionRule completion;
    final String recommendationId;

    public StrategicPlanStep(
            String id,
            GoalNodeKind kind,
            String objective,
            String reason,
            CompletionRule completion,
            String recommendationId)
    {
        if (id == null || id.trim().isEmpty()
                || objective == null || objective.trim().isEmpty())
            throw new IllegalArgumentException(get(1207));
        this.id = id;
        this.kind = kind == null ? GoalNodeKind.META : kind;
        this.objective = objective.trim();
        this.reason = reason == null ? "" : reason.trim();
        this.completion = completion == null
                ? CompletionRule.none() : completion;
        this.recommendationId = recommendationId;
    }

    public boolean isComplete(GameData data)
    {
        return completion.isComplete(data);
    }

}

/**
 * Complete output of one strategy evaluation.
 *
 * <p>The sidebar mainly consumes recommendations and opportunities. Signals
 * hold the deeper reasoning behind those decisions so a Details view, debug
 * tools, and future scoring layers can explain decisions without turning the
 * default sidebar into a wall of text.</p>
 */
@Getter
final class StrategyResult
{
    final List<Recommendation> recommendations;
    final List<Opportunity> opportunities;
    final StrategicPlan plan;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            StrategicPlan plan)
    {
        this.recommendations = unmodifiableList(
                new ArrayList<>(recommendations)
        );
        this.opportunities = unmodifiableList(
                new ArrayList<>(opportunities)
        );
        this.plan = plan;
    }

    public StrategyResult withPlan(StrategicPlan value)
    {
        return new StrategyResult(recommendations, opportunities, value);
    }
}

/**
 * Immutable result of resolving a deterministic material list against the
 * account state Compass has actually observed.
 */
@Getter
final class SupplyPlan
{
    final AccountMode accountMode;
    final boolean primaryStorageObserved;
    final boolean groupStorageIncluded;
    final boolean groupStorageObserved;
    final List<MethodInput> missingInputs;
    final String guidance;

    public SupplyPlan(
            AccountMode accountMode,
            boolean primaryStorageObserved,
            boolean groupStorageIncluded,
            boolean groupStorageObserved,
            List<MethodInput> missingInputs,
            String guidance)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        this.primaryStorageObserved = primaryStorageObserved;
        this.groupStorageIncluded = groupStorageIncluded;
        this.groupStorageObserved = groupStorageObserved;
        this.missingInputs = unmodifiableList(missingInputs == null
                ? new ArrayList<>() : new ArrayList<>(missingInputs));
        this.guidance = guidance;
    }


    public AccountMode accountMode() { return accountMode; }

    public int getTotalMissingUnits()
    {
        var total = 0L;
        for (MethodInput input : missingInputs)
        {
            total += input.quantity;
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

}

/** The recommendation Compass is currently watching for natural completion. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
final class TrackedMilestone
{
    final String activityId;
    final String title;
    final String skillName;
    final int startedAtLevel;
    final int targetLevel;
    final boolean progressionProtected;

    public Skill getSkill()
    {
        if (skillName == null) return null;
        try
        {
            return Skill.valueOf(skillName.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}

/**
 * Selected method plus the confidence after evaluating the current account.
 *
 * <p>The static TrainingMethod definition describes the method in general;
 * this object describes whether that method is actually verified for this
 * character's current state and why.</p>
 */
@Getter
final class TrainingPlan
{
    final TrainingMethod method;
    final String whyThisMethod;
    final Confidence confidence;
    final List<EvidenceCheck> requirementChecks;
    final MethodStrategyProfile strategyProfile;
    final int currentStageTargetLevel;

    TrainingMethod method() { return method; }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<EvidenceCheck> requirementChecks)
    {
        this(method, whyThisMethod, confidence, requirementChecks, null);
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<EvidenceCheck> requirementChecks,
            MethodStrategyProfile strategyProfile)
    {
        this(method, whyThisMethod, confidence, requirementChecks,
                strategyProfile, 0);
    }

    private TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<EvidenceCheck> requirementChecks,
            MethodStrategyProfile strategyProfile,
            int currentStageTargetLevel)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.requirementChecks = unmodifiableList(
                requirementChecks == null
                        ? new ArrayList<>()
                        : new ArrayList<>(requirementChecks)
        );
        this.strategyProfile = strategyProfile;
        this.currentStageTargetLevel = max(0, currentStageTargetLevel);
    }


    public TrainingPlan withCurrentStageTargetLevel(int targetLevel)
    {
        return new TrainingPlan(method, whyThisMethod, confidence,
                requirementChecks, strategyProfile, targetLevel);
    }
}

/** Result of the ordered UIM inventory-resolution policy. */
@Getter
@RequiredArgsConstructor
final class UimInventoryResolution
{
    final UimInventoryKind kind;
    final Confidence confidence;
    final UimStorageDecision storageDecision;
    final RecommendationRiskDisclosure riskDisclosure;
    final String reason;


}

/** Session-local evidence that distinct UIM setups hit the same constraints. */
@Getter
final class UimRecurringPressureAssessment
{
    final int distinctObservedLayouts;
    final List<String> blockedFamilies;

    UimRecurringPressureAssessment(int distinctObservedLayouts,
            List<String> blockedFamilies)
    {
        this.distinctObservedLayouts = max(0, distinctObservedLayouts);
        this.blockedFamilies = unmodifiableList(new ArrayList<>(
                blockedFamilies == null ? emptyList()
                        : blockedFamilies));
    }

    public boolean isRepeated()
    {
        return distinctObservedLayouts >= 2 && blockedFamilies.size() >= 2;
    }
}

/** Result of checking one proposed UIM storage route. */
@Getter
final class UimStorageDecision
{
    final StorageKind capability;
    final boolean allowed;
    final Confidence confidence;
    final RiskLevel riskLevel;
    final String explanation;

    public UimStorageDecision(
            StorageKind capability,
            boolean allowed,
            Confidence confidence,
            RiskLevel riskLevel,
            String explanation)
    {
        this.capability = capability;
        this.allowed = allowed;
        this.confidence = confidence;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.explanation = explanation;
    }

}
