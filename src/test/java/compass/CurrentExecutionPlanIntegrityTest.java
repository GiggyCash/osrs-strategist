package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CurrentExecutionPlanIntegrityTest
{
    private final RuneLiteSkillActionCatalog fishingActions =
            new RuneLiteSkillActionCatalog()
            {
                @Override
                public List<ActionDef> actionsFor(Skill skill)
                {
                    if (skill != Skill.FISHING) return Collections.emptyList();
                    return Arrays.asList(
                            action("shrimp", "Shrimp", 1, 10),
                            action("anchovies", "Anchovies", 15, 40),
                            action("trout", "Trout", 20, 50),
                            action("salmon", "Salmon", 30, 70));
                }
            };

    @Test
    public void distantFishingTargetStopsAtEveryCurrentExecutionTransition()
    {
        AdaptiveActionSelector resolver =
                new AdaptiveActionSelector(fishingActions,
                        new MethodExecutionProfileCatalog());

        assertEquals(20, resolver.resolve(
                plan(method("fishing_lumbridge_shrimps", 1, 19,
                        "Lumbridge shrimp")), 18, 53));
        assertEquals(30, resolver.resolve(
                plan(method("fishing_f2p_fly", 20, 99,
                        "Fly fishing")), 20, 53));
        assertEquals(53, resolver.resolve(
                plan(method("fishing_f2p_fly", 20, 99,
                        "Fly fishing")), 30, 53));
    }

    @Test
    public void levelEighteenShowsNetFishingAndNoFutureFlyStage()
    {
        GameData data = data(18, 2,
                Arrays.asList(
                        item(ItemID.FLY_FISHING_ROD, "Fly fishing rod", 1),
                        item(ItemID.FEATHER, "Feather", 250)));
        AdaptiveMilestoneGuidanceService service =
                new AdaptiveMilestoneGuidanceService(fishingActions,
                        new MethodExecutionProfileCatalog(),
                        new SkillingXpModifierService());

        Guidance guidance = service.build(data, Skill.FISHING,
                18, 20, plan(method("fishing_lumbridge_shrimps", 1, 19,
                        "Lumbridge shrimp")), true);

        assertTrue(guidance.getAction().contains("small net"));
        assertTrue(guidance.getAction().contains("shrimp and anchovies"));
        assertFalse(guidance.getAction().toLowerCase().contains("salmon"));
        assertFalse(guidance.getSupplies().toLowerCase().contains("feather"));
        assertTrue(guidance.getLocation().contains("Lumbridge Swamp"));
        assertTrue(guidance.getProgress().contains("XP remaining"));
        assertFalse(guidance.getProgress().contains("with Anchovies"));
    }

    @Test
    public void guaranteedF2pFallbackUsesTheSameFishingStages()
    {
        TrainingMethod fallback = new F2pBaselineMethodCatalog()
                .methodsFor(Skill.FISHING).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(value -> "fishing_f2p_fly_baseline".equals(
                        value.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        TrainingPlan fallbackPlan = plan(fallback);
        AdaptiveActionSelector resolver =
                new AdaptiveActionSelector(fishingActions,
                        new MethodExecutionProfileCatalog());

        assertEquals("Fly fishing", fallback.getName());
        assertEquals(30, resolver.resolve(fallbackPlan, 20, 53));

        Guidance guidance =
                new AdaptiveMilestoneGuidanceService(fishingActions,
                        new MethodExecutionProfileCatalog(),
                        new SkillingXpModifierService())
                        .build(data(20, 2, Arrays.asList(
                                item(ItemID.FLY_FISHING_ROD,
                                        "Fly fishing rod", 1),
                                item(ItemID.FEATHER, "Feather", 250))),
                                Skill.FISHING, 20, 30, fallbackPlan, true);
        assertTrue(guidance.getAction().contains("trout"));
        assertFalse(guidance.getAction().toLowerCase().contains("salmon"));
        assertTrue(guidance.getProgress().contains("with Trout"));
        assertTrue(guidance.getSupplies().contains("Feather"));
        assertTrue(guidance.getLocation().contains("Barbarian Village"));
    }

    @Test
    public void finiteUimFeathersAreCurrentStockNotWholePlanReadiness()
    {
        GameData data = data(30, 2,
                Arrays.asList(
                        item(ItemID.FLY_FISHING_ROD, "Fly fishing rod", 1),
                        item(ItemID.FEATHER, "Feather", 250)));
        AdaptiveMilestoneGuidanceService service =
                new AdaptiveMilestoneGuidanceService(fishingActions,
                        new MethodExecutionProfileCatalog(),
                        new SkillingXpModifierService());

        Guidance guidance = service.build(data, Skill.FISHING,
                30, 53, plan(method("fishing_f2p_fly", 20, 99,
                        "Fly fishing")), true);

        assertTrue(guidance.getProgress().contains("Trout and Salmon"));
        assertFalse(guidance.getProgress().contains("with Salmon"));
        assertTrue(guidance.getSupplies().contains("Verified usable: 250 Feather"));
        assertTrue(guidance.getSupplies().contains("Current-stage shortfall"));
        assertTrue(guidance.getSupplies().contains("resupply only"));
        assertTrue(guidance.getSupplies().contains("Gerrant's Fishy Business"));
        assertTrue(guidance.getSupplies().contains("whole distant plan"));
    }

    @Test
    public void finalValidatorRejectsAMethodThatIsNotLegalAtCurrentLevel()
    {
        TrainingPlan future = plan(method("fishing_f2p_fly", 20, 99,
                "Fly fishing")).withCurrentStageTargetLevel(30);
        Recommendation recommendation = new Recommendation(
                "skill:fishing", "Train Fishing to 53", "Goal path", 50,
                future, Confidence.VERIFIED, 18, 53,
                new Guidance("Fly-fish trout.",
                        "Bring a fly fishing rod and feathers.",
                        "Barbarian Village fishing spots.", ""),
                SafetyEvidence.skill(true, Skill.FISHING));

        Recommendation validated = new FinalExecutionPlanValidator().validate(
                recommendation, context(data(18, 2, Collections.emptyList())));
        assertTrue(validated.getSafetyEvidence().hasInvalidCurrentExecution());
        assertFalse(new CandidateSafetyPolicy().isAllowed(validated,
                context(data(18, 2, Collections.emptyList()))));
    }

    @Test
    public void allVisibleConsumersShareTheActiveStageBoundary()
    {
        TrainingPlan current = plan(method("fishing_lumbridge_shrimps", 1, 19,
                "Lumbridge shrimp")).withCurrentStageTargetLevel(20);
        Recommendation recommendation = new Recommendation(
                "skill:fishing", "Train Fishing to 53", "Goal path", 50,
                current, Confidence.VERIFIED, 18, 53,
                new Guidance("Net shrimp and anchovies.",
                        "Bring a small fishing net.",
                        "Lumbridge Swamp fishing spots.", null),
                SafetyEvidence.skill(true, Skill.FISHING));
        StrategyContext strategyContext = context(
                data(18, 2, Collections.emptyList()));
        recommendation = recommendation.withGoalProvenance(
                GoalProvenance.prerequisite(
                        GoalType.BARROWS_GLOVES, recommendation.getId(),
                        Arrays.asList("Barrows gloves", "Heroes' Quest",
                                "Fishing 53")));

        assertEquals(20, recommendation.getCurrentExecutionTargetLevel());
        GuidanceChecklist checklist = new MethodGuidanceService(
                new FarmingRunPlanner(new FarmingRunCatalog()))
                .build(recommendation, null);
        assertEquals("Level 18 → 20", checklist.getProgress());
        StrategicPlan strategicPlan = new StrategicPlanService().build(
                Collections.singletonList(recommendation),
                strategyContext, 1L);
        assertEquals("skill:fishing:20",
                strategicPlan.getCurrentStep().getId());
    }

    @Test
    public void malformedStageCannotFallThroughToTheDistantTarget()
    {
        Recommendation recommendation = new Recommendation(
                "skill:fishing", "Train Fishing to 53", "Goal path", 50,
                plan(method("fishing_lumbridge_shrimps", 1, 19,
                        "Lumbridge shrimp")).withCurrentStageTargetLevel(18),
                Confidence.VERIFIED, 18, 53,
                new Guidance("Net fish.", "Bring a net.",
                        "Lumbridge Swamp fishing spots.", null),
                SafetyEvidence.skill(true, Skill.FISHING));

        assertEquals(18, recommendation.getCurrentExecutionTargetLevel());
        Recommendation validated = new FinalExecutionPlanValidator().validate(
                recommendation, context(data(18, 2, Collections.emptyList())));
        assertTrue(validated.getSafetyEvidence().hasInvalidCurrentExecution());
    }

    @Test
    public void everyCuratedSkillMethodStopsBeforeItsLevelBandEnds()
    {
        ExpandedTrainingMethodCatalog catalog =
                new ExpandedTrainingMethodCatalog();
        AdaptiveActionSelector resolver = new AdaptiveActionSelector();
        EnumSet<Skill> covered = EnumSet.noneOf(Skill.class);
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod curated : catalog.methodsFor(skill))
            {
                TrainingMethod method = curated.getMethod();
                covered.add(skill);
                int current = Math.max(1, method.getMinLevel());
                if (current >= 99) continue;
                int target = resolver.resolve(plan(method), current, 99);
                assertTrue(method.getId(), method.supportsLevel(current));
                assertTrue(method.getId(), target > current);
                assertTrue(method.getId(), target <= 99);
                if (method.getMaxLevel() < 99)
                    assertTrue(method.getId(),
                            target <= method.getMaxLevel() + 1);
            }
        }
        assertEquals(EnumSet.allOf(Skill.class), covered);
    }

    private static ActionDef action(String id, String name,
            int level, float xp)
    {
        return new ActionDef(Skill.FISHING,
                "runelite:fishing:" + id, name, level, xp, null,
                MembershipStatus.F2P, -1);
    }

    private static TrainingPlan plan(TrainingMethod method)
    {
        return new TrainingPlan(method, "Current route",
                Confidence.VERIFIED,
                Collections.emptyList());
    }

    private static TrainingMethod method(String id, int min, int max,
            String name)
    {
        String instructions = id.contains("fly")
                ? "Barbarian Village fishing spots: catch trout and salmon, drop the fish when full, and repeat."
                : "Lumbridge Swamp fishing spots: net shrimp and anchovies, drop the fish when full, and repeat.";
        return new TrainingMethod(id, Skill.FISHING, min, max, name,
                instructions, 10, 10, 10, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(),
                Confidence.VERIFIED);
    }

    private static GameData data(int fishing, int accountType,
            List<ItemState> inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.FISHING ? fishing
                    : skill == Skill.HITPOINTS ? 10 : 1;
            levels.put(skill, level);
            xp.put(skill, level <= 1 ? 0 : Experience.getXpForLevel(level));
        }
        AccountSnapshot account = new AccountSnapshot("Stage test", accountType,
                AccountMode.fromTypeCode(accountType).name(),
                MembershipStatus.F2P, 0, 50, 0L, levels, xp);
        return GameData.builder(account)
                .inventory(new ItemsState(inventory, true))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.BARROWS_GLOVES, false, false, false,
                new PreferenceProfile());
    }

    private static ItemState item(int id, String name, int quantity)
    {
        return new ItemState(id, name, quantity);
    }
}
