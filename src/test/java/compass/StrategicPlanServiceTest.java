package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StrategicPlanServiceTest
{
    @Test
    public void provenSkillDependencyBuildsNowThenTargetPlan()
    {
        StrategyContext context = context(52, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        Recommendation fishing = new GoalDependencyProvenanceService().attach(
                skillRecommendation(52, 53), context);

        StrategicPlan plan = new StrategicPlanService().build(
                Collections.singletonList(fishing), context, 1234L);

        assertNotNull(plan);
        assertEquals("skill:fishing:53", plan.getCurrentStep().getId());
        assertEquals(PlanCompletionCondition.Kind.SKILL_LEVEL,
                plan.getCurrentStep().getCompletion().getKind());
        assertTrue(plan.getSteps().stream().anyMatch(step ->
                step.getObjective().equals("Heroes' Quest")
                        && step.getKind() == GoalNodeKind.QUEST));
        assertEquals("Barrows gloves",
                plan.getSteps().get(plan.getSteps().size() - 1).getObjective());
    }

    @Test
    public void unrelatedRecommendationCannotCreateGoalPlan()
    {
        StrategyContext context = context(1, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        Recommendation farming = skillRecommendation(
                Skill.FARMING, 1, 10, "skill:farming");
        assertNull(new StrategicPlanService().build(
                Collections.singletonList(farming), context, 1L));
    }

    @Test
    public void minorRerankRetainsUnfinishedCurrentStep()
    {
        StrategyContext context = context(52, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        Recommendation fishing = new GoalDependencyProvenanceService().attach(
                skillRecommendation(52, 53), context);
        StrategicPlan previous = new StrategicPlanService().build(
                Collections.singletonList(fishing), context, 10L);
        Recommendation lowerScore = copyWithScore(fishing, 1.0);
        StrategicPlan rebuilt = new StrategicPlanService().build(
                Collections.singletonList(lowerScore), context, 20L);

        StrategicPlan result = new PlanContinuityService().reconcile(
                previous, rebuilt, context,
                Collections.singletonList(lowerScore));

        assertEquals(10L, result.getCreatedAtMillis());
        assertEquals("skill:fishing:53", result.getCurrentStep().getId());
    }

    @Test
    public void completedSkillAdvancesToProvenQuestTransition()
    {
        StrategyContext before = context(52, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        Recommendation fishing = new GoalDependencyProvenanceService().attach(
                skillRecommendation(52, 53), before);
        StrategicPlan previous = new StrategicPlanService().build(
                Collections.singletonList(fishing), before, 10L);
        StrategyContext after = context(53, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        StrategicPlan advanced = previous.advanceCompleted(after.data());

        assertFalse(advanced.getCurrentStep().getId()
                .equals("skill:fishing:53"));
        assertEquals(GoalNodeKind.QUEST, advanced.getCurrentStep().getKind());
    }

    @Test
    public void goalMembershipAndAccountChangesInvalidatePlan()
    {
        StrategyContext original = context(52, MembershipStatus.P2P,
                GoalType.BARROWS_GLOVES);
        Recommendation fishing = new GoalDependencyProvenanceService().attach(
                skillRecommendation(52, 53), original);
        StrategicPlan plan = new StrategicPlanService().build(
                Collections.singletonList(fishing), original, 10L);

        assertFalse(plan.matchesContext(context(52, MembershipStatus.F2P,
                GoalType.BARROWS_GLOVES)));
        assertFalse(plan.matchesContext(context(52, MembershipStatus.P2P,
                GoalType.FIRE_CAPE)));
    }

    private static Recommendation copyWithScore(
            Recommendation value, double score)
    {
        return new Recommendation(value.getId(), value.getTitle(),
                value.getReason(), score, value.getTrainingPlan(),
                value.getConfidence(), value.getCurrentLevel(),
                value.getTargetLevel(), value.getGuidance(),
                value.getSafetyEvidence()).withGoalProvenance(
                        value.getGoalProvenance());
    }

    private static Recommendation skillRecommendation(int current, int target)
    {
        return skillRecommendation(Skill.FISHING, current, target,
                "skill:fishing");
    }

    private static Recommendation skillRecommendation(
            Skill skill, int current, int target, String id)
    {
        TrainingMethod method = new TrainingMethod("test-method", skill,
                1, 99, "Test method", "Repeat the method.", 10, 10, 10,
                AttentionLevel.MODERATE, 20, 2, Collections.emptyList(),
                Confidence.VERIFIED, true, false, false);
        TrainingPlan plan = new TrainingPlan(method, "Test plan",
                Confidence.VERIFIED, Collections.emptyList());
        return new Recommendation(id, "Train " + skill.getName() + " to "
                + target, "Proven work.", 50, plan,
                Confidence.VERIFIED, current, target,
                new Guidance("Repeat to the target.",
                        "Bring supplies.", "Named location.", null),
                SafetyEvidence.skill(true, skill));
    }

    private static StrategyContext context(int fishing,
            MembershipStatus membership, GoalType goal)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.FISHING ? fishing
                    : skill == Skill.HITPOINTS ? 10 : 1);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Planner", 42L, 0,
                "MAIN", membership, membership == MembershipStatus.P2P ? 1 : 0,
                levels.size(), 0, levels, xp);
        Map<String, QuestStatus> quests = new LinkedHashMap<>();
        for (Quest quest : Quest.values())
            quests.put(quest.getName(), QuestStatus.NOT_STARTED);
        GameData data = GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .build();
        return new StrategyContext(data, StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, QuestTolerance.LOW, goal,
                false, false, false, new PreferenceProfile());
    }
}
