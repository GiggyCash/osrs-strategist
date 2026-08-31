package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Exposes the single current Slayer state-machine action to the shared queue. */
@Singleton
public class SlayerCandidateProvider implements StrategyCandidateProvider
{
    private final SlayerStrategist strategist;

    @Inject
    public SlayerCandidateProvider(SlayerStrategist strategist)
    {
        this.strategist = strategist == null ? new SlayerStrategist() : strategist;
    }

    public SlayerCandidateProvider()
    {
        this(new SlayerStrategist());
    }

    @Override
    public String getId()
    {
        return "slayer-strategist";
    }

    @Override
    public Set<String> supersededCandidateIds()
    {
        return Collections.singleton("skill:slayer");
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        SlayerDecisionResult result = strategist.assess(context);
        if (result == null || result.getGuidance() == null)
            return Collections.emptyList();

        SlayerSnapshot slayer = context.getData().getSlayer();
        String id;
        String title;
        if (result.getRecommendedReward() != null)
        {
            id = "slayer:unlock:" + result.getRecommendedReward().getId();
            title = "Unlock " + result.getRecommendedReward().getDisplayName();
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.UNKNOWN)
        {
            id = "verify:slayer-assignment";
            title = "Check your Slayer assignment";
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.CHOICE_PENDING)
        {
            id = "slayer:choose-task";
            title = result.getRecommendedOffer() == null
                    ? "Review Mortimer's task choices"
                    : "Choose " + result.getRecommendedOffer().getTaskName()
                            + " from Mortimer";
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.NO_TASK)
        {
            id = "slayer:get-task";
            title = "Get a task from " + result.getMaster().getDisplayName();
        }
        else
        {
            String task = slayer == null ? "Slayer task" : slayer.getTaskName();
            SlayerTaskDecision decision = result.getDecision();
            switch (decision)
            {
                case BLOCK:
                    id = "slayer:block-task";
                    title = "Block " + task;
                    break;
                case SKIP:
                    id = "slayer:skip-task";
                    title = "Skip " + task;
                    break;
                case PREP_FIRST:
                    id = "prepare:slayer-task";
                    title = "Prepare for " + task;
                    break;
                case ALTERNATIVE:
                    id = "slayer:alternative";
                    title = result.getSelectedAlternativeName() != null
                            ? "Use " + result.getSelectedAlternativeName()
                            : "Replace the risky Slayer task";
                    break;
                case DO:
                default:
                    id = "slayer:do-task";
                    title = task + " — do this task";
                    break;
            }
        }

        CandidateSafetyEvidence safety = result.getDecision()
                == SlayerTaskDecision.DO
                ? CandidateSafetyEvidence.skill(false, Skill.SLAYER)
                : result.getDecision() == SlayerTaskDecision.ALTERNATIVE
                    && result.getSelectedAlternativeName() != null
                    ? CandidateSafetyEvidence.potentiallyIrreversible(false)
                    : CandidateSafetyEvidence.verifiedSafe(false);
        RecommendationStrategicValue strategicValue = strategicValue(result,
                context);
        return Collections.singletonList(new StrategyCandidate(id, title,
                result.getReason(), result.getScore(), result.getConfidence(),
                result.getGuidance(), safety, strategicValue));
    }

    private static RecommendationStrategicValue strategicValue(
            SlayerDecisionResult result, StrategyContext context)
    {
        RecommendationStrategicValue.Builder builder =
                RecommendationStrategicValue.builder()
                        .evidence("slayer:typed-decision");
        SlayerTaskStrategicProfile task = result.getTaskProfile();
        if (task != null)
        {
            builder.resourceFit((task.getResourceValue() - 2.5) / 2.5)
                    .riskBurden(task.getInherentRisk() == RiskLevel.NONE
                            || task.getInherentRisk() == RiskLevel.LOW ? 0.0
                            : task.getInherentRisk() == RiskLevel.MEDIUM
                                    ? 0.5 : 1.0)
                    .setupReuse(Math.max(0.0,
                            1.0 - task.getSetupBurden() / 5.0));
        }
        if (context != null && context.getActiveGoal() == GoalType.SLAYER_85)
            builder.unlockValue(1.0);
        if (result.getRecommendedReward() != null)
            builder.unlockValue(1.0).infrastructureValue(0.6)
                    .evidence("slayer:live-reward-varbit");
        return builder.build();
    }
}
