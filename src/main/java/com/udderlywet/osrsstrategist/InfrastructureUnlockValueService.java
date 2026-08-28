package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/** Assesses infrastructure through typed utility and observed provenance. */
@Singleton
public final class InfrastructureUnlockValueService
{
    private final InfrastructureMilestoneCatalog catalog;
    private final AccountStrategicPriorityService priorityService;

    public InfrastructureUnlockValueService()
    {
        this(new InfrastructureMilestoneCatalog(),
                new AccountStrategicPriorityService());
    }

    InfrastructureUnlockValueService(InfrastructureMilestoneCatalog catalog,
            AccountStrategicPriorityService priorityService)
    {
        this.catalog = catalog;
        this.priorityService = priorityService;
    }

    public InfrastructureValueAssessment assess(String milestoneId,
            StrategyContext context)
    {
        InfrastructureMilestoneDefinition definition = catalog.get(milestoneId);
        if (definition == null)
            throw new IllegalArgumentException(
                    "Unknown infrastructure milestone " + milestoneId);
        AccountStrategicPriorityProfile priorities =
                priorityService.assess(context);
        StrategyDataBundle data = context == null ? null : context.getData();
        return assess(definition, priorities, data);
    }

    public InfrastructureValueAssessment assess(
            InfrastructureMilestoneDefinition definition,
            AccountStrategicPriorityProfile priorities,
            StrategyDataBundle data)
    {
        if (definition == null) throw new IllegalArgumentException("definition");
        if (priorities == null) throw new IllegalArgumentException("priorities");

        List<InfrastructureValueContribution> contributions = new ArrayList<>();
        StrategicPriority overall = StrategicPriority.NONE;
        for (Map.Entry<InfrastructureBenefit, StrategicPriority> entry
                : definition.getBenefits().entrySet())
        {
            StrategicPriority account = priorities.priorityOf(
                    entry.getKey().getDimension());
            InfrastructureValueContribution contribution =
                    new InfrastructureValueContribution(entry.getKey(),
                            account, entry.getValue());
            contributions.add(contribution);
            overall = StrategicPriority.higherOf(overall,
                    contribution.getEffectivePriority());
        }

        AccountSnapshot account = data == null ? null : data.getAccount();
        if (definition.isMembersOnly())
        {
            MembershipStatus membership = account == null
                    ? MembershipStatus.UNKNOWN : account.getMembershipStatus();
            if (membership == MembershipStatus.F2P)
                return result(definition,
                        InfrastructureMilestoneState.NOT_APPLICABLE,
                        RecommendationConfidence.BLOCKED, overall,
                        contributions, "This infrastructure is members-only.");
            if (membership == MembershipStatus.UNKNOWN)
                return result(definition,
                        InfrastructureMilestoneState.CHECK_NEEDED,
                        RecommendationConfidence.CHECK_NEEDED, overall,
                        contributions,
                        "Membership must be verified before this infrastructure can be planned.");
        }

        CapabilityState completion = completionState(definition, data);
        if (completion == CapabilityState.VERIFIED)
            return result(definition, InfrastructureMilestoneState.COMPLETE,
                    RecommendationConfidence.VERIFIED, overall, contributions,
                    "Observed account state proves this milestone is complete.");

        RequirementState requirements = requirements(definition, data);
        if (requirements == RequirementState.BLOCKED)
            return result(definition,
                    InfrastructureMilestoneState.REQUIREMENTS_MISSING,
                    RecommendationConfidence.BLOCKED, overall, contributions,
                    "A verified skill, quest, or prerequisite requirement is not complete.");
        if (requirements == RequirementState.CHECK_NEEDED
                || completion == CapabilityState.UNKNOWN)
            return result(definition,
                    InfrastructureMilestoneState.CHECK_NEEDED,
                    RecommendationConfidence.CHECK_NEEDED, overall,
                    contributions,
                    "Requirements or completion are not fully observed; inspect the live capability before recommending acquisition.");

        return result(definition, InfrastructureMilestoneState.ACTIONABLE,
                RecommendationConfidence.VERIFIED, overall, contributions,
                "Requirements are verified and observed capability state proves the milestone is not yet complete.");
    }

    private RequirementState requirements(
            InfrastructureMilestoneDefinition definition,
            StrategyDataBundle data)
    {
        AccountSnapshot account = data == null ? null : data.getAccount();
        if (account == null) return RequirementState.CHECK_NEEDED;
        if (definition.getRequiredSkill() != null
                && account.getSkillLevel(definition.getRequiredSkill())
                < definition.getRequiredLevel())
            return RequirementState.BLOCKED;

        if (definition.getRequiredQuest() != null)
        {
            QuestSnapshot quests = data.getQuests();
            if (quests == null) return RequirementState.CHECK_NEEDED;
            QuestStatus status = quests.statusOf(definition.getRequiredQuest());
            boolean satisfied = status == QuestStatus.COMPLETE
                    || definition.isQuestStartSufficient()
                    && status == QuestStatus.IN_PROGRESS;
            if (!satisfied)
                return status == QuestStatus.UNKNOWN
                        ? RequirementState.CHECK_NEEDED
                        : RequirementState.BLOCKED;
        }

        if (definition.getPrerequisiteMilestoneId() != null)
        {
            InfrastructureMilestoneDefinition prerequisite = catalog.get(
                    definition.getPrerequisiteMilestoneId());
            CapabilityState state = completionState(prerequisite, data);
            if (state == CapabilityState.UNKNOWN)
                return RequirementState.CHECK_NEEDED;
            if (state == CapabilityState.BLOCKED)
                return RequirementState.BLOCKED;
        }
        return RequirementState.VERIFIED;
    }

    private CapabilityState completionState(
            InfrastructureMilestoneDefinition definition,
            StrategyDataBundle data)
    {
        if (definition == null || data == null) return CapabilityState.UNKNOWN;
        switch (definition.getEvidenceKind())
        {
            case POH_ACCESS:
                return data.getPoh() == null ? CapabilityState.UNKNOWN
                        : data.getPoh().getHouseAccess();
            case POH_FURNITURE:
                return data.getPoh() == null ? CapabilityState.UNKNOWN
                        : data.getPoh().furnitureState(
                                definition.getEvidenceKey());
            case STORAGE_CAPABILITY:
                return data.getStorage() == null ? CapabilityState.UNKNOWN
                        : data.getStorage().stateOf(
                                definition.getStorageCapability());
            case TRANSPORT_ROUTE:
                return data.getTransport() != null
                        && data.getTransport().hasVerifiedRoute(
                                definition.getEvidenceKey())
                        ? CapabilityState.VERIFIED : CapabilityState.UNKNOWN;
            default:
                return CapabilityState.UNKNOWN;
        }
    }

    private static InfrastructureValueAssessment result(
            InfrastructureMilestoneDefinition definition,
            InfrastructureMilestoneState state,
            RecommendationConfidence confidence,
            StrategicPriority value,
            List<InfrastructureValueContribution> contributions,
            String reason)
    {
        return new InfrastructureValueAssessment(definition, state, confidence,
                value, contributions, reason);
    }
}
