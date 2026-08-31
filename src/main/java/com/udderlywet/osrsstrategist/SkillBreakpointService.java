package com.udderlywet.osrsstrategist;

import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Chooses actual unlock/requirement levels before generic level checkpoints. */
@Singleton
public final class SkillBreakpointService
{
    private final GoalDependencyProvenanceService goals;
    private final AbilityUnlockCatalog abilities;
    private final RuneLiteSkillActionCatalog actions;
    private final InfrastructureMilestoneCatalog infrastructure;
    private final InfrastructureUnlockValueService infrastructureValue;

    @Inject
    public SkillBreakpointService(
            GoalDependencyProvenanceService goals,
            AbilityUnlockCatalog abilities,
            RuneLiteSkillActionCatalog actions,
            InfrastructureMilestoneCatalog infrastructure,
            InfrastructureUnlockValueService infrastructureValue)
    {
        this.goals = goals == null
                ? new GoalDependencyProvenanceService() : goals;
        this.abilities = abilities == null
                ? new AbilityUnlockCatalog() : abilities;
        this.actions = actions == null
                ? new RuneLiteSkillActionCatalog() : actions;
        this.infrastructure = infrastructure == null
                ? new InfrastructureMilestoneCatalog() : infrastructure;
        this.infrastructureValue = infrastructureValue == null
                ? new InfrastructureUnlockValueService() : infrastructureValue;
    }

    public SkillBreakpointService()
    {
        this(new GoalDependencyProvenanceService(),
                new AbilityUnlockCatalog(), new RuneLiteSkillActionCatalog(),
                new InfrastructureMilestoneCatalog(),
                new InfrastructureUnlockValueService());
    }

    public SkillBreakpoint next(
            Skill skill, int currentLevel, StrategyContext context)
    {
        int goalLevel = goals.nextRequiredSkillLevel(
                context == null ? GoalType.AUTOMATIC : context.getActiveGoal(),
                skill, context);
        if (goalLevel > currentLevel)
            return new SkillBreakpoint(skill, goalLevel,
                    Text.get(1301),
                    SkillBreakpoint.Kind.GOAL_REQUIREMENT,
                    "goal:" + context.getActiveGoal().name().toLowerCase());

        InfrastructureMilestone infrastructureTarget = context == null
                ? null : infrastructure.all().stream()
                .filter(value -> value.getRequiredSkills()
                        .getOrDefault(skill, 0) > currentLevel)
                .filter(value -> isNextMissingSkill(value, skill,
                        context.data().account()))
                .filter(value -> {
                    InfrastructureMilestoneState state = infrastructureValue
                            .assess(value.getId(), context).getState();
                    return state != InfrastructureMilestoneState.COMPLETE
                            && state != InfrastructureMilestoneState.NOT_APPLICABLE;
                })
                .min(Comparator.comparingInt(
                        value -> value.getRequiredSkills().get(skill)))
                .orElse(null);
        if (infrastructureTarget != null)
            return new SkillBreakpoint(skill,
                    infrastructureTarget.getRequiredSkills().get(skill),
                    "Unlock " + infrastructureTarget.getName(),
                    SkillBreakpoint.Kind.INFRASTRUCTURE_UNLOCK,
                    "infrastructure:" + infrastructureTarget.getId());

        AbilityUnlockDefinition ability = abilities.all().stream()
                .filter(value -> value.getSkill() == skill
                        && value.getLevel() > currentLevel)
                .min(Comparator.comparingInt(
                        AbilityUnlockDefinition::getLevel))
                .orElse(null);
        if (ability != null)
            return new SkillBreakpoint(skill, ability.getLevel(),
                    "Unlock " + ability.getName(),
                    SkillBreakpoint.Kind.ABILITY_UNLOCK,
                    "ability:" + ability.getId());

        MembershipStatus membership = context == null
                || context.data() == null
                || context.data().account() == null
                ? MembershipStatus.UNKNOWN
                : context.data().account().getMembershipStatus();
        ActionDef action = actions.actionsFor(skill).stream()
                .filter(value -> value.getLevel() > currentLevel)
                .filter(value -> isAvailable(value.getMembership(), membership))
                .min(Comparator.comparingInt(
                        ActionDef::getLevel))
                .orElse(null);
        if (action != null)
            return new SkillBreakpoint(skill, action.getLevel(),
                    "Unlock " + action.getName(),
                    SkillBreakpoint.Kind.TRAINING_ACTION_UNLOCK,
                    action.getId());

        if (context != null && context.getActiveGoal() == GoalType.MAX)
            return new SkillBreakpoint(skill, 99, Text.get(1302),
                    SkillBreakpoint.Kind.MAX_TARGET, "goal:max");
        return new SkillBreakpoint(skill, Math.min(99, currentLevel + 1),
                Text.get(1303),
                SkillBreakpoint.Kind.NEXT_LEVEL_FALLBACK, "level:next");
    }

    private static boolean isAvailable(
            MembershipStatus action, MembershipStatus account)
    {
        if (account == MembershipStatus.UNKNOWN) return false;
        if (account == MembershipStatus.F2P)
            return action == MembershipStatus.F2P;
        return action == MembershipStatus.F2P || action == MembershipStatus.P2P;
    }

    private static boolean isNextMissingSkill(
            InfrastructureMilestone definition, Skill requested,
            AccountSnapshot account)
    {
        Skill next = null;
        int smallestGap = Integer.MAX_VALUE;
        for (java.util.Map.Entry<Skill, Integer> requirement
                : definition.getRequiredSkills().entrySet())
        {
            int gap = requirement.getValue()
                    - account.getSkillLevel(requirement.getKey());
            if (gap > 0 && gap < smallestGap)
            {
                smallestGap = gap;
                next = requirement.getKey();
            }
        }
        return requested == next;
    }
}
