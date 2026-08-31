package com.udderlywet.osrsstrategist;

import java.util.*;
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
        var definition = catalog.get(milestoneId);
        if (definition == null)
            throw new IllegalArgumentException(
                    Text.get(1422) + milestoneId);
        AccountStrategicPriorityProfile priorities =
                priorityService.assess(context);
        var data = context == null ? null : context.data();
        return assess(definition, priorities, data);
    }

    public InfrastructureValueAssessment assess(
            InfrastructureMilestone definition,
            AccountStrategicPriorityProfile priorities,
            GameData data)
    {
        if (definition == null) throw new IllegalArgumentException("definition");
        if (priorities == null) throw new IllegalArgumentException("priorities");

        List<InfrastructureValueContribution> contributions = new ArrayList<>();
        var overall = StrategicPriority.NONE;
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

        var account = data == null ? null : data.account();
        if (definition.isMembersOnly())
        {
            MembershipStatus membership = account == null
                    ? MembershipStatus.UNKNOWN : account.getMembershipStatus();
            if (membership == MembershipStatus.F2P)
                return result(definition,
                        InfrastructureMilestoneState.NOT_APPLICABLE,
                        Confidence.BLOCKED, overall,
                        contributions, Text.get(1423));
            if (membership == MembershipStatus.UNKNOWN)
                return result(definition,
                        InfrastructureMilestoneState.CHECK_NEEDED,
                        Confidence.CHECK_NEEDED, overall,
                        contributions,
                        Text.get(334));
        }

        var completion = completionState(definition, data);
        if (completion == CapabilityState.VERIFIED)
            return result(definition, InfrastructureMilestoneState.COMPLETE,
                    Confidence.VERIFIED, overall, contributions,
                    Text.get(335));

        var requirements = requirements(definition, data);
        if (requirements == RequirementState.BLOCKED)
            return result(definition,
                    InfrastructureMilestoneState.REQUIREMENTS_MISSING,
                    Confidence.BLOCKED, overall, contributions,
                    Text.get(336));
        if (requirements == RequirementState.CHECK_NEEDED
                || completion == CapabilityState.UNKNOWN)
            return result(definition,
                    InfrastructureMilestoneState.CHECK_NEEDED,
                    Confidence.CHECK_NEEDED, overall,
                    contributions,
                    Text.get(337));

        return result(definition, InfrastructureMilestoneState.ACTIONABLE,
                Confidence.VERIFIED, overall, contributions,
                Text.get(338));
    }

    private RequirementState requirements(
            InfrastructureMilestone definition,
            GameData data)
    {
        var account = data == null ? null : data.account();
        if (account == null) return RequirementState.CHECK_NEEDED;
        for (Map.Entry<net.runelite.api.Skill, Integer> skill
                : definition.getRequiredSkills().entrySet())
            if (account.getSkillLevel(skill.getKey()) < skill.getValue())
                return RequirementState.BLOCKED;

        for (Map.Entry<String, Boolean> quest
                : definition.getRequiredQuests().entrySet())
        {
            var quests = data.quests();
            if (quests == null) return RequirementState.CHECK_NEEDED;
            var status = quests.statusOf(quest.getKey());
            boolean satisfied = status == QuestStatus.COMPLETE
                    || quest.getValue()
                    && status == QuestStatus.IN_PROGRESS;
            if (!satisfied)
                return status == QuestStatus.UNKNOWN
                        ? RequirementState.CHECK_NEEDED
                        : RequirementState.BLOCKED;
        }

        if (definition.getPrerequisiteMilestoneId() != null)
        {
            InfrastructureMilestone prerequisite = catalog.get(
                    definition.getPrerequisiteMilestoneId());
            var state = completionState(prerequisite, data);
            if (state == CapabilityState.UNKNOWN)
                return RequirementState.CHECK_NEEDED;
            if (state == CapabilityState.BLOCKED)
                return RequirementState.BLOCKED;
        }
        return RequirementState.VERIFIED;
    }

    private CapabilityState completionState(
            InfrastructureMilestone definition,
            GameData data)
    {
        if (definition == null || data == null) return CapabilityState.UNKNOWN;
        switch (definition.getEvidenceKind())
        {
            case POH_ACCESS:
                return data.poh() == null ? CapabilityState.UNKNOWN
                        : data.poh().getHouseAccess();
            case POH_FURNITURE:
                return data.poh() == null ? CapabilityState.UNKNOWN
                        : data.poh().furnitureState(
                                definition.getEvidenceKey());
            case STORAGE_CAPABILITY:
                return data.storage() == null ? CapabilityState.UNKNOWN
                        : data.storage().stateOf(
                                definition.getStorageCapability());
            case TRANSPORT_ROUTE:
                return data.transport() != null
                        && data.transport().hasVerifiedRoute(
                                definition.getEvidenceKey())
                        ? CapabilityState.VERIFIED : CapabilityState.UNKNOWN;
            default:
                return CapabilityState.UNKNOWN;
        }
    }

    private static InfrastructureValueAssessment result(
            InfrastructureMilestone definition,
            InfrastructureMilestoneState state,
            Confidence confidence,
            StrategicPriority value,
            List<InfrastructureValueContribution> contributions,
            String reason)
    {
        return new InfrastructureValueAssessment(definition, state, confidence,
                value, contributions, reason);
    }
}
