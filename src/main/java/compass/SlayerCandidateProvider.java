package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Exposes the single current Slayer state-machine action to the shared queue. */
@Singleton
public class SlayerCandidateProvider implements CandidateProvider
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
        return Text.get(1969);
    }

    @Override
    public Set<String> supersededCandidateIds()
    {
        return Collections.singleton("skill:slayer");
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        var result = strategist.assess(context);
        if (result == null || result.getGuidance() == null)
            return Collections.emptyList();

        var slayer = context.data().slayer();
        String id;
        String title;
        if (result.getRecommendedReward() != null)
        {
            id = "slayer:unlock:" + result.getRecommendedReward().id;
            title = "Unlock " + result.getRecommendedReward().getDisplayName();
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.UNKNOWN)
        {
            id = Text.get(1970);
            title = Text.get(1460);
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.CHOICE_PENDING)
        {
            id = Text.get(1971);
            title = result.getRecommendedOffer() == null
                    ? Text.get(1461)
                    : "Choose " + result.getRecommendedOffer().getTaskName()
                            + " from Mortimer";
        }
        else if (result.getAssignmentState() == SlayerAssignmentState.NO_TASK)
        {
            id = "slayer:get-task";
            title = Text.get(1972) + result.getMaster().getDisplayName();
        }
        else
        {
            var task = slayer == null ? "Slayer task" : slayer.getTaskName();
            var decision = result.getDecision();
            switch (decision)
            {
                case BLOCK:
                    id = Text.get(1973);
                    title = "Block " + task;
                    break;
                case SKIP:
                    id = Text.get(1974);
                    title = "Skip " + task;
                    break;
                case PREP_FIRST:
                    id = Text.get(1975);
                    title = "Prepare for " + task;
                    break;
                case ALTERNATIVE:
                    id = Text.get(1976);
                    title = result.getSelectedAlternativeName() != null
                            ? "Use " + result.getSelectedAlternativeName()
                            : Text.get(1462);
                    break;
                case DO:
                default:
                    id = "slayer:do-task";
                    title = task + " — do this task";
                    break;
            }
        }

        SafetyEvidence safety = result.getDecision()
                == SlayerTaskDecision.DO
                ? SafetyEvidence.skill(false, Skill.SLAYER)
                : result.getDecision() == SlayerTaskDecision.ALTERNATIVE
                    && result.getSelectedAlternativeName() != null
                    ? SafetyEvidence.potentiallyIrreversible(false)
                    : SafetyEvidence.verifiedSafe(false);
        StrategicValue strategicValue = strategicValue(result,
                context);
        return Collections.singletonList(new Recommendation(id, title,
                result.getReason(), result.getScore(), result.getConfidence(),
                result.getGuidance(), safety, strategicValue));
    }

    private static StrategicValue strategicValue(
            SlayerDecisionResult result, StrategyContext context)
    {
        StrategicValue.Builder builder =
                StrategicValue.builder()
                        .evidence(Text.get(1977));
        var task = result.getTaskProfile();
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
        if (context != null && context.goal() == GoalType.SLAYER_85)
            builder.unlockValue(1.0);
        if (result.getRecommendedReward() != null)
            builder.unlockValue(1.0).infrastructureValue(0.6)
                    .evidence(Text.get(1978));
        return builder.build();
    }
}
