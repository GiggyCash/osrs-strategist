package com.udderlywet.osrsstrategist;

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
        StrategyDataBundle data = StrategyDataBundle.builder(f2pAccount()).build();
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
        StrategyDataBundle data = StrategyDataBundle.builder(f2pAccount())
                .inventory(new InventorySnapshot(Arrays.asList(
                        new ItemStackSnapshot(ItemID.BLANKRUNE, "Rune essence", 28))))
                .equipment(new EquipmentSnapshot(Arrays.asList(
                        new ItemStackSnapshot(ItemID.TIARA_AIR, "Air tiara", 1))))
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
        assertEquals(RecommendationConfidence.VERIFIED, plan.getConfidence());
        assertEquals(2, plan.getRequirementChecks().size());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState() == RequirementState.VERIFIED));
    }

    @Test
    public void p2pLevelNineUsesConcreteEarthRunesAndAcceptsPureEssence()
    {
        AccountSnapshot account = account(MembershipStatus.P2P, 9);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Arrays.asList(
                        new ItemStackSnapshot(ItemID.BLANKRUNE_HIGH, "Pure essence", 174))))
                .equipment(new EquipmentSnapshot(Arrays.asList(
                        new ItemStackSnapshot(ItemID.TIARA_EARTH, "Earth tiara", 1))))
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
        assertEquals(RecommendationConfidence.VERIFIED, plan.getConfidence());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState() == RequirementState.VERIFIED));
    }

    @Test
    public void gimLevelNineCanUseRecentlyObservedSharedEssenceForEarthRunes()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 9, 4))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(ItemID.TIARA_EARTH,
                                "Earth tiara", 1))))
                .groupStorage(new GroupStorageSnapshot(true,
                        Collections.singletonList(new ItemStackSnapshot(
                                ItemID.BLANKRUNE_HIGH, "Pure essence", 174))))
                .build();

        TrainingPlan plan = selector().select(data, Skill.RUNECRAFT, 9,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR,
                false, true);

        assertNotNull(plan);
        assertEquals("runecraft_f2p_earth", plan.getMethod().getId());
        assertEquals(RecommendationConfidence.VERIFIED,
                plan.getConfidence());
        assertTrue(plan.getRequirementChecks().stream()
                .allMatch(check -> check.getState()
                        == RequirementState.VERIFIED));
    }

    @Test
    public void skillRecommendationsNeverLeakWithoutConcreteMethods()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(f2pAccount()).build();
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
            assertFalse(RecommendationPresentation.compactHtml(recommendation)
                    .contains("Check needed before choosing a method"));
        }
    }

    @Test
    public void unresolvedRequirementsAreNamedInsteadOfVagueFallback()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(f2pAccount()).build();
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

        String compact = RecommendationPresentation.compactHtml(recommendation);
        assertTrue(compact.contains("Craft air runes"));
        assertTrue(compact.contains("NEEDED"));
        assertTrue(compact.contains("Rune or pure essence"));
        assertTrue(compact.contains("Air talisman or air tiara"));
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
