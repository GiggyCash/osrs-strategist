package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class F2pRunecraftRecommendationTest
{
    @Test
    public void levelOneF2pRunecraftGetsConcreteAirRuneMethod()
    {
        GameData data = GameData.builder(f2pAccount()).build();
        TrainingMethodSelector selector = selector();

        TrainingPlan plan = selector.select(
                data,
                Skill.RUNECRAFT,
                1,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false
        );

        assertNotNull(plan);
        assertNotNull(plan.getMethod());
        assertEquals("runecraft_f2p_air", plan.getMethod().getId());
        assertEquals("Craft air runes", plan.getMethod().getName());
        assertFalse(plan.getRequirementChecks().isEmpty());
        assertTrue(plan.getRequirementChecks().stream()
                .anyMatch(check -> "Rune or pure essence".equals(check.getLabel())));
        assertTrue(plan.getRequirementChecks().stream()
                .anyMatch(check -> "Air talisman or air tiara".equals(check.getLabel())));
    }

    @Test
    public void observedEssenceAndEquippedTiaraMakeAirRunesReady()
    {
        GameData data = GameData.builder(f2pAccount())
                .inventory(new ItemsState(Arrays.asList(
                        new ItemState(ItemID.BLANKRUNE, "Rune essence", 28))))
                .equipment(new ItemsState(Arrays.asList(
                        new ItemState(ItemID.TIARA_AIR, "Air tiara", 1))))
                .build();

        TrainingPlan plan = selector().select(
                data,
                Skill.RUNECRAFT,
                1,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false
        );

        assertNotNull(plan);
        assertEquals("runecraft_f2p_air", plan.getMethod().getId());
        assertEquals(Confidence.VERIFIED, plan.getConfidence());
        assertEquals(2, plan.getRequirementChecks().size());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState() == RequirementState.VERIFIED));
    }

    @Test
    public void p2pLevelNineUsesConcreteEarthRunesAndAcceptsPureEssence()
    {
        AccountSnapshot account = account(MembershipStatus.P2P, 9);
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Arrays.asList(
                        new ItemState(ItemID.BLANKRUNE_HIGH, "Pure essence", 174))))
                .equipment(new ItemsState(Arrays.asList(
                        new ItemState(ItemID.TIARA_EARTH, "Earth tiara", 1))))
                .build();

        TrainingPlan plan = selector().select(
                data,
                Skill.RUNECRAFT,
                9,
                StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME,
                false
        );

        assertNotNull(plan);
        assertNotNull(plan.getMethod());
        assertEquals("runecraft_f2p_earth", plan.getMethod().getId());
        assertEquals("Craft earth runes", plan.getMethod().getName());
        assertTrue(plan.getMethod().getInstructions().contains("Earth Altar"));
        assertTrue(plan.getMethod().getInstructions().contains("Varrock East"));
        assertFalse(plan.getMethod().getName().toLowerCase().contains("most useful"));
        assertEquals(Confidence.VERIFIED, plan.getConfidence());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState() == RequirementState.VERIFIED));
    }

    @Test
    public void gimLevelNineCanUseRecentlyObservedSharedEssenceForEarthRunes()
    {
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 9, 4))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.singletonList(
                        new ItemState(ItemID.TIARA_EARTH,
                                "Earth tiara", 1))))
                .groupStorage(new ItemsState(true,
                        Collections.singletonList(new ItemState(
                                ItemID.BLANKRUNE_HIGH, "Pure essence", 174))))
                .build();

        TrainingPlan plan = selector().select(data, Skill.RUNECRAFT, 9,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR,
                false, true);

        assertNotNull(plan);
        assertEquals("runecraft_f2p_earth", plan.getMethod().getId());
        assertEquals(Confidence.VERIFIED,
                plan.getConfidence());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState()
                        == RequirementState.VERIFIED));
    }

    @Test
    public void skillRecommendationsNeverLeakWithoutConcreteMethods()
    {
        GameData data = GameData.builder(f2pAccount()).build();
        RecommendationEngine engine = new RecommendationEngine(selector());

        java.util.List<Recommendation> recommendations = engine.recommend(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false,
                new PreferenceProfile()
        );

        assertFalse(recommendations.isEmpty());
        for (Recommendation recommendation : recommendations)
        {
            assertNotNull(recommendation.getTrainingPlan());
            assertNotNull(recommendation.getTrainingPlan().getMethod());
            assertFalse(Presentation.compactHtml(recommendation)
                    .contains("Check needed before choosing a method"));
        }
    }

    @Test
    public void unresolvedResourcesRemainNamedOrdinaryPreparation()
    {
        GameData data = GameData.builder(f2pAccount()).build();
        TrainingPlan plan = selector().select(
                data,
                Skill.RUNECRAFT,
                1,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false
        );
        Recommendation recommendation = new Recommendation(
                "skill:runecraft",
                "Train Runecraft to 10",
                "Opens rune options and useful training activities.",
                50.0,
                plan,
                plan.getConfidence(),
                1,
                10
        );

        assertEquals("Craft air runes", plan.getMethod().getName());
        assertTrue(plan.getRequirementChecks().stream().anyMatch(check ->
                "Rune or pure essence".equals(check.getLabel())));
        assertTrue(plan.getRequirementChecks().stream().anyMatch(check ->
                "Air talisman or air tiara".equals(check.getLabel())));
        assertFalse(RequirementActionability.hasHardUnresolvedRequirement(plan));
    }

    @Test
    public void waterRuneRouteNeverSelectsWaterTiaraAsTheAction()
    {
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog()
        {
            @Override
            public java.util.List<ActionDef> actionsFor(
                    Skill skill)
            {
                if (skill != Skill.RUNECRAFT) return Collections.emptyList();
                return Arrays.asList(
                        new ActionDef(Skill.RUNECRAFT,
                                "test:water_tiara", "Water tiara", 1, 50,
                                null, MembershipStatus.F2P),
                        new ActionDef(Skill.RUNECRAFT,
                                "test:water_rune", "Water rune", 5, 5,
                                null, MembershipStatus.F2P));
            }
        };
        AccountSnapshot account = account(MembershipStatus.F2P, 5);
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(ItemID.BLANKRUNE,
                                "Rune essence", 100))))
                .equipment(new ItemsState(Collections.singletonList(
                        new ItemState(ItemID.TIARA_WATER,
                                "Water tiara", 1))))
                .build();
        TrainingPlan plan = selector().select(data, Skill.RUNECRAFT, 5,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false);
        RecommendationGuidanceService guidanceService =
                new RecommendationGuidanceService(
                        new AdaptiveMilestoneGuidanceService(actions,
                                new MethodExecutionProfileCatalog(),
                                new SkillingXpModifierService()),
                        new VariableMethodGuidanceService(),
                        new UniversalSkillActionGuidanceService(actions,
                                new UniversalActionRecipeResolver(),
                                new SkillingXpModifierService(),
                                new AccountResourcePlanner()));

        Guidance guidance = guidanceService.build(
                data, Skill.RUNECRAFT, 5, 10, plan, false);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Water runes"));
        assertFalse(guidance.getAction().contains("Water tiara"));
    }

    private static TrainingMethodSelector selector()
    {
        return new TrainingMethodSelector(
                new TrainingMethodDatabase(),
                new RequirementEvidenceEngine((FarmingAccessEvaluator) null),
                new ExpandedTrainingMethodCatalog(),
                new F2pBaselineMethodCatalog(),
                new TrainingMethodPolicy()
        );
    }

    private static AccountSnapshot f2pAccount()
    {
        return account(MembershipStatus.F2P, 1);
    }

    private static AccountSnapshot account(MembershipStatus membership, int runecraftLevel)
    {
        return account(membership, runecraftLevel, 0);
    }

    private static AccountSnapshot account(MembershipStatus membership,
            int runecraftLevel, int accountType)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.RUNECRAFT, runecraftLevel);
        return new AccountSnapshot(
                membership == MembershipStatus.F2P ? "F2P Test" : "P2P Test",
                accountType,
                AccountMode.fromTypeCode(accountType).name(),
                membership,
                1,
                1,
                0L,
                levels,
                xp
        );
    }
}
