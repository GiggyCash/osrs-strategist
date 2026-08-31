package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Fail-closed STASH -> skill -> method/material -> access planning. */
@Singleton
public final class StashDependencyPlanner
{
    private final ItemRequirementEvaluator itemEvaluator =
            new ItemRequirementEvaluator();

    public StashBuildPlan plan(StashUnitDefinition unit, StashUnitState state,
            StrategyContext context)
    {
        List<StashDependencyStep> steps = new ArrayList<>();
        if (unit == null)
            return new StashBuildPlan(null, steps);

        steps.add(step(GoalNodeKind.STASH,
                "Prepare the " + unit.getTier().name().toLowerCase()
                        + " STASH at " + unit.getLocation(),
                RecommendationConfidence.CHECK_NEEDED));

        if (state == null || state == StashUnitState.UNKNOWN)
        {
            steps.add(step(GoalNodeKind.PREPARATION_ACTION,
                    PlayerText.get("SDP1"),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }
        if (state == StashUnitState.BUILT_CONTENTS_UNKNOWN)
        {
            steps.add(step(GoalNodeKind.PREPARATION_ACTION,
                    PlayerText.get("SDP2"),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }
        if (state == StashUnitState.BUILT_AND_FILLED)
        {
            steps.add(step(GoalNodeKind.CLUE,
                    PlayerText.get("SDP3"),
                    RecommendationConfidence.VERIFIED));
            return new StashBuildPlan(unit, steps);
        }

        AccountSnapshot account = context == null || context.getData() == null
                ? null : context.getData().getAccount();
        if (account == null || account.getMembershipStatus() != MembershipStatus.P2P)
        {
            steps.add(step(GoalNodeKind.ACCESS,
                    PlayerText.get("SDP4"),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        int requiredLevel = unit.getTier().getConstructionLevel();
        int level = account.getSkillLevel(Skill.CONSTRUCTION);
        if (level < requiredLevel)
        {
            steps.add(step(GoalNodeKind.SKILL_LEVEL,
                    "Train Construction from " + level + " to " + requiredLevel,
                    RecommendationConfidence.VERIFIED));
            steps.add(step(GoalNodeKind.TRAINING_METHOD,
                    constructionRoute(level, requiredLevel),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        ItemRequirementResult materials = itemEvaluator.evaluate(
                unit.getTier().materials(), context.getData(),
                context.isUseGroupStorage());
        if (!materials.isSatisfied())
        {
            steps.add(step(GoalNodeKind.RESOURCE, materials.getAction(),
                    RecommendationConfidence.CHECK_NEEDED));
            steps.add(step(GoalNodeKind.PREPARATION_ACTION,
                    PlayerText.get("SDP5"),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        if (unit.isWilderness() && (context == null
                || !context.isAllowWildernessMethods()))
        {
            steps.add(step(GoalNodeKind.ACCESS,
                    PlayerText.get("SDP6"),
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        steps.add(step(GoalNodeKind.TRANSPORTATION,
                PlayerText.get("SDP7") + unit.getLocation()
                        + PlayerText.get("SDP8"),
                RecommendationConfidence.CHECK_NEEDED));
        return new StashBuildPlan(unit, steps);
    }

    private static StashDependencyStep step(GoalNodeKind kind, String action,
            RecommendationConfidence confidence)
    {
        return new StashDependencyStep(kind, action, confidence);
    }

    private static String constructionRoute(int currentLevel, int targetLevel)
    {
        if (targetLevel <= 32)
        {
            return PlayerText.get("SDP9")
                    + targetLevel + " Construction.";
        }
        if (currentLevel < 33)
        {
            return PlayerText.get("SDP10")
                    + targetLevel + " Construction.";
        }
        return PlayerText.get("SDP11")
                + targetLevel + " Construction.";
    }
}
