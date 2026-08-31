package compass;

import java.util.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Safe acquisition recommendation for one resource requirement.
 *
 * <p>This never performs a purchase, sale, drop, withdrawal, or other gameplay
 * action. It only tells the strategy engine where a resource appears to be or
 * which sourcing family should be evaluated next.</p>
 */
@Getter
final class AcquisitionPlan
{
    private final ResourceNeed need;
    private final AcquisitionSource source;
    private final int confirmedQuantity;
    private final Confidence confidence;
    private final String note;

    public AcquisitionPlan(
            ResourceNeed need,
            AcquisitionSource source,
            int confirmedQuantity,
            Confidence confidence,
            String note)
    {
        this.need = need;
        this.source = source;
        this.confirmedQuantity = Math.max(0, confirmedQuantity);
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.note = note;
    }


    public boolean hasEnoughConfirmed()
    {
        return need != null
                && confirmedQuantity >= need.getQuantity();
    }
}

final class ContextualGearAssessment
{
    private final Map<GearDecisionKind, ContextualGearDecision> decisions;

    ContextualGearAssessment(
            Map<GearDecisionKind, ContextualGearDecision> decisions)
    {
        this.decisions = Collections.unmodifiableMap(
                new EnumMap<>(decisions));
    }

    public ContextualGearDecision get(GearDecisionKind kind)
    {
        return decisions.get(kind);
    }

    public Map<GearDecisionKind, ContextualGearDecision> all()
    {
        return decisions;
    }
}

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ContextualGearDecision
{
    private final GearDecisionKind kind;
    private final String value;
    private final Confidence confidence;


}

/** Typed prerequisite in a resource acquisition route. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class DependencyRequirement
{
    public enum Kind { RESOURCE, QUEST, SKILL, GEAR }

    final String id;
    private final String label;
    private final Kind kind;
    private final ResourceNeed resource;
    private final Skill skill;
    private final int level;


    public static DependencyRequirement resource(ResourceNeed need)
    {
        return new DependencyRequirement("resource:" + need.getItemId(),
                need.getItemName(), Kind.RESOURCE, need, null, 0);
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
    private final List<ResolvedDependencyNode> nodes;
    private final boolean cycleDetected;
    private final boolean depthLimited;
    private final boolean opportunityCostRejected;
    private final boolean nodeLimited;

    public DependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected)
    {
        this(nodes, cycleDetected, depthLimited, opportunityCostRejected, false);
    }

    public DependencyResolution(List<ResolvedDependencyNode> nodes,
            boolean cycleDetected, boolean depthLimited,
            boolean opportunityCostRejected, boolean nodeLimited)
    {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.cycleDetected = cycleDetected;
        this.depthLimited = depthLimited;
        this.opportunityCostRejected = opportunityCostRejected;
        this.nodeLimited = nodeLimited;
    }

    public ResolvedDependencyNode nextAction()
    {
        for (ResolvedDependencyNode node : nodes)
            if (node.getConfidence() != Confidence.VERIFIED) return node;
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}

@Getter
final class DiaryTaskRequirement
{
    public enum Kind { SKILL, QUEST, COMBAT_LEVEL, QUEST_POINTS, ALTERNATIVE_CHECK }

    private final Kind kind;
    private final Skill skill;
    private final int level;
    private final String quest;
    private final boolean startedOnly;
    private final String check;

    private DiaryTaskRequirement(Kind kind, Skill skill, int level,
            String quest, boolean startedOnly, String check)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = Math.max(0, level);
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

    private final Kind kind;
    private final String target;
    private final String action;


}

/** Bounded GIM resource value derived from one fresh storage observation. */
@Getter
final class GroupResourceAssessment
{
    private final GroupResourceState state;
    private final Confidence confidence;
    private final int observedSharedQuantity;
    private final int requiredQuantity;
    private final double duplicateGrindAvoidance;
    private final String reason;

    GroupResourceAssessment(GroupResourceState state,
            Confidence confidence, int observedSharedQuantity,
            int requiredQuantity, double duplicateGrindAvoidance, String reason)
    {
        this.state = state;
        this.confidence = confidence;
        this.observedSharedQuantity = Math.max(0, observedSharedQuantity);
        this.requiredQuantity = Math.max(1, requiredQuantity);
        this.duplicateGrindAvoidance = Math.max(0.0,
                Math.min(1.0, duplicateGrindAvoidance));
        this.reason = reason == null ? "" : reason;
    }

    public boolean satisfiesNeed()
    {
        return state == GroupResourceState.SHARED_STOCK_SATISFIES_NEED;
    }

    public StrategicValue strategicValue(String evidenceId)
    {
        if (confidence != Confidence.VERIFIED
                || duplicateGrindAvoidance <= 0.0)
            return StrategicValue.neutral();
        return StrategicValue.builder()
                .accountModeFit(duplicateGrindAvoidance * 0.6)
                .resourceFit(duplicateGrindAvoidance)
                .evidence(evidenceId)
                .build();
    }
}

@Getter
final class GuidanceStep
{
    final String id;
    private final String label;
    private final String detail;
    private final GuidanceStepState state;

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
final class InfrastructureValueAssessment
{
    private final InfrastructureMilestone milestone;
    private final InfrastructureMilestoneState state;
    private final Confidence confidence;
    private final StrategicPriority strategicValue;
    private final List<InfrastructureValueContribution> contributions;
    private final String reason;

    InfrastructureValueAssessment(InfrastructureMilestone milestone,
            InfrastructureMilestoneState state,
            Confidence confidence,
            StrategicPriority strategicValue,
            List<InfrastructureValueContribution> contributions,
            String reason)
    {
        this.milestone = milestone;
        this.state = state;
        this.confidence = confidence;
        this.strategicValue = strategicValue;
        this.contributions = Collections.unmodifiableList(
                new ArrayList<>(contributions));
        this.reason = reason == null ? "" : reason;
    }

    public boolean canRecommendAcquisition()
    {
        return state == InfrastructureMilestoneState.ACTIONABLE
                && confidence == Confidence.VERIFIED;
    }
}

/** Result of evaluating a composable item requirement against observed state. */
final class ItemRequirementResult
{
    @Getter
    private final RequirementState state;
    @Getter
    private final String action;
    private final List<MethodInput> missingInputs;

    public ItemRequirementResult(RequirementState state, String action)
    {
        this(state, action, Collections.emptyList());
    }

    public ItemRequirementResult(RequirementState state, String action,
            List<MethodInput> missingInputs)
    {
        this.state = state;
        this.action = action == null ? "" : action;
        this.missingInputs = Collections.unmodifiableList(missingInputs == null
                ? new ArrayList<>() : new ArrayList<>(missingInputs));
    }

    /** Exact, evidence-backed shortfalls only. Unknown storage never appears here. */
    public List<MethodInput> getMissingInputs() { return missingInputs; }
    public boolean isSatisfied() { return state == RequirementState.VERIFIED; }
}

@Getter
final class MainPurchaseDecision
{
    private final MainPurchaseChoice choice;
    private final long totalCost;
    private final long observedCoins;
    private final Confidence confidence;
    private final String explanation;

    public MainPurchaseDecision(
            MainPurchaseChoice choice,
            long totalCost,
            long observedCoins,
            Confidence confidence,
            String explanation)
    {
        this.choice = choice;
        this.totalCost = Math.max(0L, totalCost);
        this.observedCoins = Math.max(0L, observedCoins);
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.explanation = explanation;
    }

}

/** Live account assessment of one sourced method profile. */
@Getter
@RequiredArgsConstructor
final class MethodStrategyAssessment
{
    private final boolean viable;
    private final double scoreAdjustment;
    private final String explanation;


}

/** One meaningful account progression event suitable for a session recap. */
@Getter
final class ProgressMilestone
{
    final String id;
    private final ProgressMilestoneType type;
    private final String title;
    private final String detail;
    private final String goalId;
    private final long occurredAtMillis;

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
            throw new IllegalArgumentException(Text.get(1156));
        }
        this.id = id;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.goalId = goalId;
        this.occurredAtMillis = Math.max(0L, occurredAtMillis);
    }

}

/** The skill checkpoint currently being executed by the active plan. */
@Getter
final class ProgressTarget
{
    private final String activityId;
    private final String methodId;
    private final Skill skill;
    private final int targetLevel;
    private final int targetXp;

    public ProgressTarget(
            String activityId,
            String methodId,
            Skill skill,
            int targetLevel)
    {
        if (skill == null || targetLevel < 2 || targetLevel > 126)
        {
            throw new IllegalArgumentException(Text.get(1157));
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
final class ProgressTargetProjection
{
    public enum State
    {
        NO_TARGET,
        CALCULATING,
        READY,
        COMPLETE
    }

    private final State state;
    private final ProgressTarget target;
    private final int xpRemaining;
    private final long etaMillis;

    private ProgressTargetProjection(
            State state,
            ProgressTarget target,
            int xpRemaining,
            long etaMillis)
    {
        this.state = state;
        this.target = target;
        this.xpRemaining = Math.max(0, xpRemaining);
        this.etaMillis = Math.max(0L, etaMillis);
    }

    public static ProgressTargetProjection noTarget()
    {
        return new ProgressTargetProjection(State.NO_TARGET, null, 0, 0L);
    }

    public static ProgressTargetProjection calculating(
            ProgressTarget target, int xpRemaining)
    {
        return new ProgressTargetProjection(State.CALCULATING, target,
                xpRemaining, 0L);
    }

    public static ProgressTargetProjection ready(
            ProgressTarget target, int xpRemaining, long etaMillis)
    {
        return new ProgressTargetProjection(State.READY, target,
                xpRemaining, etaMillis);
    }

    public static ProgressTargetProjection complete(ProgressTarget target)
    {
        return new ProgressTargetProjection(State.COMPLETE, target, 0, 0L);
    }

}

/** Ordered unfinished quest work derived only from verified dependency edges. */
final class QuestPathPlan
{
    @Getter
    private final List<QuestPathStep> steps;

    QuestPathPlan(List<QuestPathStep> steps)
    {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }


    public QuestPathStep nextEligibleStep()
    {
        for (QuestPathStep step : steps)
            if (step.isEligibleNow()) return step;
        return null;
    }

    public QuestPathStep stepForQuest(String questName)
    {
        var expected = Names.words(questName);
        for (QuestPathStep step : steps)
            if (Names.words(step.getQuestName()).equals(expected)) return step;
        return null;
    }

    public boolean isEmpty() { return steps.isEmpty(); }

}

/** One unfinished quest shared by one or more proven selected-goal paths. */
@Getter
final class QuestPathStep
{
    private final String questName;
    private final QuestStatus status;
    private final Map<GoalType, List<String>> provenancePaths;
    private final List<String> unfinishedDependents;
    private final Confidence readiness;
    private final boolean eligibleNow;
    private final int depth;
    private final Map<Skill, Integer> guaranteedRewardXp;
    private final double goalPathRewardValue;

    QuestPathStep(String questName, QuestStatus status,
            Map<GoalType, List<String>> provenancePaths,
            List<String> unfinishedDependents,
            Confidence readiness,
            boolean eligibleNow, int depth,
            Map<Skill, Integer> guaranteedRewardXp,
            double goalPathRewardValue)
    {
        this.questName = questName;
        this.status = status == null ? QuestStatus.UNKNOWN : status;
        EnumMap<GoalType, List<String>> paths = new EnumMap<>(GoalType.class);
        if (provenancePaths != null)
        {
            for (Map.Entry<GoalType, List<String>> entry
                    : provenancePaths.entrySet())
                paths.put(entry.getKey(), Collections.unmodifiableList(
                        new ArrayList<>(entry.getValue())));
        }
        this.provenancePaths = Collections.unmodifiableMap(paths);
        this.unfinishedDependents = Collections.unmodifiableList(
                new ArrayList<>(unfinishedDependents == null
                        ? Collections.emptyList() : unfinishedDependents));
        this.readiness = readiness == null
                ? Confidence.CHECK_NEEDED : readiness;
        this.eligibleNow = eligibleNow;
        this.depth = Math.max(0, depth);
        EnumMap<Skill, Integer> rewards = new EnumMap<>(Skill.class);
        if (guaranteedRewardXp != null)
            rewards.putAll(guaranteedRewardXp);
        this.guaranteedRewardXp = Collections.unmodifiableMap(rewards);
        this.goalPathRewardValue = Math.max(0.0,
                Math.min(1.0, goalPathRewardValue));
    }

    public int getGoalCount() { return provenancePaths.size(); }

    /** Bounded property value for the common recommendation decision layer. */
    public double sharedDependencyValue()
    {
        var goals = Math.max(0, getGoalCount() - 1) * 0.35;
        var dependents = unfinishedDependents.size() * 0.12;
        return Math.min(1.0, goals + dependents);
    }

    public StrategicValue strategicValue()
    {
        var shared = sharedDependencyValue();
        if (shared <= 0.0 && goalPathRewardValue <= 0.0)
            return StrategicValue.neutral();
        return StrategicValue.builder()
                .sharedDependencyValue(shared)
                .unlockValue(goalPathRewardValue)
                .evidence("quest-path:" + questName)
                .build();
    }
}

/** Actionability result for one fully identified quest. */
@Getter
final class QuestResolution
{
    private final Confidence confidence;
    private final Guidance guidance;
    private final String reason;
    private final SafetyEvidence safetyEvidence;

    public QuestResolution(Confidence confidence,
            Guidance guidance, String reason)
    {
        this(confidence, guidance, reason, SafetyEvidence.unknown());
    }

    public QuestResolution(Confidence confidence,
            Guidance guidance, String reason,
            SafetyEvidence safetyEvidence)
    {
        this.confidence = confidence;
        this.guidance = guidance;
        this.reason = reason;
        this.safetyEvidence = safetyEvidence;
    }

}

/** One ordered, non-destructive step in a resource acquisition chain. */
@Getter
final class ResourceAcquisitionStep
{
    private final AcquisitionSource source;
    private final String action;
    private final Confidence confidence;

    public ResourceAcquisitionStep(AcquisitionSource source, String action,
            Confidence confidence)
    {
        this.source = source;
        this.action = action;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
    }

}

/**
 * A reusable observed-resource requirement. itemIds are alternatives: quantities
 * across every listed ID are summed toward the requirement.
 */
final class ResourceRequirement
{
    @Getter
    final String id;
    @Getter
    private final String label;
    @Getter
    private final int requiredQuantity;
    private final int[] itemIds;

    public ResourceRequirement(
            String id,
            String label,
            int requiredQuantity,
            int... itemIds)
    {
        this.id = id;
        this.label = label;
        this.requiredQuantity = Math.max(1, requiredQuantity);
        this.itemIds = itemIds == null ? new int[0] : Arrays.copyOf(itemIds, itemIds.length);
    }

    public int[] getItemIds() { return Arrays.copyOf(itemIds, itemIds.length); }
}

/** Structured access/build evidence consumed by the final recommendation gate. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class SafetyEvidence
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

    @Getter
    private final Access access;
    @Getter
    private final BuildEffect buildEffect;
    @Getter
    private final Skill affectedSkill;
    @Getter
    private final boolean conventionalBankRequired;
    private final boolean unverifiedDangerousStorage;
    private final boolean invalidCurrentExecution;

    public static SafetyEvidence unknown()
    {
        return new SafetyEvidence(Access.UNKNOWN, BuildEffect.UNKNOWN,
                null, false, false, false);
    }

    public static SafetyEvidence harmless(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.HARMLESS, null, false, false, false);
    }

    public static SafetyEvidence skill(boolean freeToPlay, Skill skill)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.SKILL_XP, skill, false, false, false);
    }

    public static SafetyEvidence verifiedSafe(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.VERIFIED_SAFE, null, false, false, false);
    }

    public static SafetyEvidence potentiallyIrreversible(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.POTENTIALLY_IRREVERSIBLE, null, false, false, false);
    }

    public SafetyEvidence requiringConventionalBank()
    {
        return new SafetyEvidence(access, buildEffect,
                affectedSkill, true, unverifiedDangerousStorage,
                invalidCurrentExecution);
    }

    public SafetyEvidence withUnverifiedDangerousStorage()
    {
        return new SafetyEvidence(access, buildEffect,
                affectedSkill, conventionalBankRequired, true,
                invalidCurrentExecution);
    }

    public SafetyEvidence withInvalidCurrentExecution()
    {
        return new SafetyEvidence(access, buildEffect, affectedSkill,
                conventionalBankRequired, unverifiedDangerousStorage, true);
    }

    private static Access access(boolean freeToPlay)
    {
        return freeToPlay ? Access.F2P_SAFE : Access.MEMBERS_ONLY;
    }

    public boolean hasUnverifiedDangerousStorage()
    {
        return unverifiedDangerousStorage;
    }
    public boolean hasInvalidCurrentExecution()
    {
        return invalidCurrentExecution;
    }
}

/** Explicit boundary for group capabilities RuneLite does not observe. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SharedInfrastructureAssessment
{
    private final CapabilityState state;
    private final Confidence confidence;
    private final String reason;


}

/** Immutable explanation and execution payload from the Slayer strategist. */
@Getter
final class SlayerDecisionResult
{
    private final SlayerAssignmentState assignmentState;
    private final SlayerTaskDecision decision;
    private final SlayerMasterProfile master;
    private final SlayerTaskStrategicProfile taskProfile;
    private final double score;
    private final Confidence confidence;
    private final String reason;
    private final Guidance guidance;
    private final String selectedAlternativeName;
    private final SlayerReward recommendedReward;
    private final SlayerTaskOffer recommendedOffer;

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance)
    {
        this(assignmentState, decision, master, taskProfile, score,
                confidence, reason, guidance, null, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, null, null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward)
    {
        this(assignmentState, decision, master, taskProfile, score, confidence,
                reason, guidance, selectedAlternativeName, recommendedReward,
                null);
    }

    public SlayerDecisionResult(SlayerAssignmentState assignmentState,
            SlayerTaskDecision decision, SlayerMasterProfile master,
            SlayerTaskStrategicProfile taskProfile, double score,
            Confidence confidence, String reason,
            Guidance guidance, String selectedAlternativeName,
            SlayerReward recommendedReward, SlayerTaskOffer recommendedOffer)
    {
        this.assignmentState = assignmentState == null
                ? SlayerAssignmentState.UNKNOWN : assignmentState;
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
    private final GoalType goal;
    private final long accountHash;
    private final String playerName;
    private final AccountMode accountMode;
    private final MembershipStatus membership;
    @Getter
    private final List<StrategicPlanStep> steps;
    @Getter
    private final int currentIndex;
    @Getter
    private final long createdAtMillis;

    public StrategicPlan(
            GoalType goal,
            AccountSnapshot account,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        this(goal,
                account == null ? 0L : account.getAccountHash(),
                account == null ? "" : account.getPlayerName(),
                account == null ? AccountMode.UNKNOWN
                        : AccountMode.fromTypeCode(account.modeCode()),
                account == null ? MembershipStatus.UNKNOWN
                        : account.membership(),
                steps, currentIndex, createdAtMillis);
    }

    private StrategicPlan(
            GoalType goal,
            long accountHash,
            String playerName,
            AccountMode accountMode,
            MembershipStatus membership,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || steps == null || steps.isEmpty())
            throw new IllegalArgumentException(
                    Text.get(791));
        this.goal = goal;
        this.accountHash = accountHash;
        this.playerName = playerName == null ? "" : playerName;
        this.accountMode = accountMode == null
                ? AccountMode.UNKNOWN : accountMode;
        this.membership = membership == null
                ? MembershipStatus.UNKNOWN : membership;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.currentIndex = Math.max(0,
                Math.min(currentIndex, this.steps.size() - 1));
        this.createdAtMillis = Math.max(0L, createdAtMillis);
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
        if (accountHash != 0L && account.getAccountHash() != 0L)
            return accountHash == account.getAccountHash()
                    && accountMode == context.accountMode()
                    && membership == account.membership();
        return playerName != null && playerName.equals(account.getPlayerName())
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
final class StrategicPlanStep
{
    final String id;
    private final GoalNodeKind kind;
    private final String objective;
    private final String reason;
    private final PlanCompletionCondition completion;
    private final String recommendationId;

    public StrategicPlanStep(
            String id,
            GoalNodeKind kind,
            String objective,
            String reason,
            PlanCompletionCondition completion,
            String recommendationId)
    {
        if (id == null || id.trim().isEmpty()
                || objective == null || objective.trim().isEmpty())
            throw new IllegalArgumentException(Text.get(1207));
        this.id = id;
        this.kind = kind == null ? GoalNodeKind.META : kind;
        this.objective = objective.trim();
        this.reason = reason == null ? "" : reason.trim();
        this.completion = completion == null
                ? PlanCompletionCondition.none() : completion;
        this.recommendationId = recommendationId;
    }

    public boolean isComplete(GameData data)
    {
        return completion.isComplete(data);
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof StrategicPlanStep)) return false;
        var that = (StrategicPlanStep) other;
        return id.equals(that.id) && kind == that.kind
                && objective.equals(that.objective)
                && reason.equals(that.reason)
                && completion.equals(that.completion)
                && Objects.equals(recommendationId, that.recommendationId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, kind, objective, reason, completion,
                recommendationId);
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
    private final List<Recommendation> recommendations;
    private final List<Opportunity> opportunities;
    private final StrategicPlan plan;

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities)
    {
        this(
                recommendations,
                opportunities,
                null
        );
    }

    public StrategyResult(
            List<Recommendation> recommendations,
            List<Opportunity> opportunities,
            StrategicPlan plan)
    {
        this.recommendations = Collections.unmodifiableList(
                new ArrayList<>(recommendations)
        );
        this.opportunities = Collections.unmodifiableList(
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
    private final AccountMode accountMode;
    private final boolean primaryStorageObserved;
    private final boolean groupStorageIncluded;
    private final boolean groupStorageObserved;
    private final List<ResourcePlanEntry> entries;
    private final String guidance;

    public SupplyPlan(
            AccountMode accountMode,
            boolean primaryStorageObserved,
            boolean groupStorageIncluded,
            boolean groupStorageObserved,
            List<ResourcePlanEntry> entries,
            String guidance)
    {
        this.accountMode = accountMode == null ? AccountMode.UNKNOWN : accountMode;
        this.primaryStorageObserved = primaryStorageObserved;
        this.groupStorageIncluded = groupStorageIncluded;
        this.groupStorageObserved = groupStorageObserved;
        this.entries = Collections.unmodifiableList(entries == null
                ? new ArrayList<>() : new ArrayList<>(entries));
        this.guidance = guidance;
    }


    public boolean isFullySupplied()
    {
        if (!primaryStorageObserved && accountMode != AccountMode.ULTIMATE_IRONMAN)
        {
            return false;
        }
        for (ResourcePlanEntry entry : entries)
        {
            if (!entry.isSatisfied()) return false;
        }
        return true;
    }

    public AccountMode accountMode() { return accountMode; }

    public int getTotalMissingUnits()
    {
        var total = 0L;
        for (ResourcePlanEntry entry : entries)
        {
            total += entry.getMissing();
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public List<MethodInput> getMissingInputs()
    {
        List<MethodInput> result = new ArrayList<>();
        for (ResourcePlanEntry entry : entries)
        {
            if (entry.getMissing() > 0) result.add(entry.missingInput());
        }
        return Collections.unmodifiableList(result);
    }
}

/** The recommendation Compass is currently watching for natural completion. */
@Getter
final class TrackedMilestone
{
    private final String activityId;
    private final String title;
    private final String skillName;
    private final int startedAtLevel;
    private final int targetLevel;
    private final boolean progressionProtected;

    public TrackedMilestone(
            String activityId,
            String title,
            String skillName,
            int startedAtLevel,
            int targetLevel)
    {
        this(activityId, title, skillName, startedAtLevel,
                targetLevel, false);
    }

    public TrackedMilestone(
            String activityId,
            String title,
            String skillName,
            int startedAtLevel,
            int targetLevel,
            boolean progressionProtected)
    {
        this.activityId = activityId;
        this.title = title;
        this.skillName = skillName;
        this.startedAtLevel = startedAtLevel;
        this.targetLevel = targetLevel;
        this.progressionProtected = progressionProtected;
    }


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
final class TrainingPlan
{
    @Getter
    private final TrainingMethod method;
    @Getter
    private final String whyThisMethod;
    @Getter
    private final Confidence confidence;
    @Getter
    private final List<RequirementCheck> requirementChecks;
    @Getter
    private final MethodStrategyProfile strategyProfile;
    private final int currentStageTargetLevel;

    TrainingMethod method() { return method; }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod)
    {
        this(
                method,
                whyThisMethod,
                method == null
                        ? Confidence.CHECK_NEEDED
                        : method.getConfidence(),
                Collections.emptyList(),
                null
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence)
    {
        this(
                method,
                whyThisMethod,
                confidence,
                Collections.emptyList(),
                null
        );
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<RequirementCheck> requirementChecks)
    {
        this(method, whyThisMethod, confidence, requirementChecks, null);
    }

    public TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<RequirementCheck> requirementChecks,
            MethodStrategyProfile strategyProfile)
    {
        this(method, whyThisMethod, confidence, requirementChecks,
                strategyProfile, 0);
    }

    private TrainingPlan(
            TrainingMethod method,
            String whyThisMethod,
            Confidence confidence,
            List<RequirementCheck> requirementChecks,
            MethodStrategyProfile strategyProfile,
            int currentStageTargetLevel)
    {
        this.method = method;
        this.whyThisMethod = whyThisMethod;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED
                : confidence;
        this.requirementChecks = Collections.unmodifiableList(
                requirementChecks == null
                        ? new ArrayList<>()
                        : new ArrayList<>(requirementChecks)
        );
        this.strategyProfile = strategyProfile;
        this.currentStageTargetLevel = Math.max(0, currentStageTargetLevel);
    }






    /**
     * The next level at which the visible execution plan must be rebuilt. This
     * is deliberately separate from the recommendation's distant objective.
     */
    public int getCurrentStageTargetLevel()
    {
        return currentStageTargetLevel;
    }

    public TrainingPlan withCurrentStageTargetLevel(int targetLevel)
    {
        return new TrainingPlan(method, whyThisMethod, confidence,
                requirementChecks, strategyProfile, targetLevel);
    }
}

/** Travel evidence and bounded value for a selected concrete method location. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TravelAwareMethodAssessment
{
    private final MethodLocationOption location;
    private final int travelBurden;
    private final int scoreAdjustment;
    private final boolean verifiedRouteUsed;
    private final String evidence;


}

/** Result of the ordered UIM inventory-resolution policy. */
@Getter
@RequiredArgsConstructor
final class UimInventoryResolution
{
    private final UimInventoryResolutionKind kind;
    private final Confidence confidence;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;
    private final String reason;


}

/** Session-local evidence that distinct UIM setups hit the same constraints. */
@Getter
final class UimRecurringPressureAssessment
{
    private final int distinctObservedLayouts;
    private final List<String> blockedFamilies;

    UimRecurringPressureAssessment(int distinctObservedLayouts,
            List<String> blockedFamilies)
    {
        this.distinctObservedLayouts = Math.max(0, distinctObservedLayouts);
        this.blockedFamilies = Collections.unmodifiableList(new ArrayList<>(
                blockedFamilies == null ? Collections.emptyList()
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
    private final StorageCapability capability;
    private final boolean allowed;
    private final Confidence confidence;
    private final RiskLevel riskLevel;
    private final String explanation;

    public UimStorageDecision(
            StorageCapability capability,
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
