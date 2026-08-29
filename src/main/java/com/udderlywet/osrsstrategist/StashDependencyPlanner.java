package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
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
                    "Check Watson's noticeboard or an observed POH STASH chart to confirm whether this exact unit is built and filled.",
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }
        if (state == StashUnitState.BUILT_CONTENTS_UNKNOWN)
        {
            steps.add(step(GoalNodeKind.PREPARATION_ACTION,
                    "Inspect this STASH and confirm its complete item set before relying on it.",
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }
        if (state == StashUnitState.BUILT_AND_FILLED)
        {
            steps.add(step(GoalNodeKind.CLUE,
                    "Use the verified filled STASH for this emote step, then follow RuneLite Clue Helper.",
                    RecommendationConfidence.VERIFIED));
            return new StashBuildPlan(unit, steps);
        }

        AccountSnapshot account = context == null || context.getData() == null
                ? null : context.getData().getAccount();
        if (account == null || account.getMembershipStatus() != MembershipStatus.P2P)
        {
            steps.add(step(GoalNodeKind.ACCESS,
                    "Verify active membership; Construction STASH units are members-only.",
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
                    "Self-source verified shortfalls on Iron accounts; mains may compare a GE purchase only after price and GP are observed.",
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        if (unit.isWilderness() && (context == null
                || !context.isAllowWildernessMethods()))
        {
            steps.add(step(GoalNodeKind.ACCESS,
                    "Do not route this Wilderness STASH until Wilderness risk is explicitly enabled and accepted.",
                    RecommendationConfidence.CHECK_NEEDED));
            return new StashBuildPlan(unit, steps);
        }

        steps.add(step(GoalNodeKind.TRANSPORTATION,
                "Verify the quest/access route and travel to " + unit.getLocation()
                        + " with a hammer, saw, and unnoted materials.",
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
            return "In a verified POH Parlour, build and remove crude wooden chairs with two planks and two steel nails each until "
                    + targetLevel + " Construction.";
        }
        if (currentLevel < 33)
        {
            return "In a verified POH Parlour, build and remove crude wooden chairs to 33 Construction; then use a verified POH Kitchen to build and remove oak larders with eight oak planks each until "
                    + targetLevel + " Construction.";
        }
        return "In a verified POH Kitchen, build and remove oak larders with eight oak planks each until "
                + targetLevel + " Construction.";
    }
}
