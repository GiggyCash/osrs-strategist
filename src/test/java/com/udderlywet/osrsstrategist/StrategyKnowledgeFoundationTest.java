package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StrategyKnowledgeFoundationTest
{
    private final TrainingMethodSelector selector = new TrainingMethodSelector(
            new TrainingMethodDatabase(), null,
            new ExpandedTrainingMethodCatalog(),
            new F2pBaselineMethodCatalog(), new TrainingMethodPolicy());

    @Test
    public void sourceRegistryCoversEveryStableSourceIdWithoutRuntimeNetworking()
    {
        StrategySourceRegistry registry = new StrategySourceRegistry();
        assertEquals(StrategySourceId.values().length, registry.all().size());
        for (StrategySourceId id : StrategySourceId.values())
        {
            StrategySourceDefinition source = registry.get(id);
            assertNotNull(id.name(), source);
            assertNotNull(source.getUrl());
            assertNotNull(source.getReviewedDate());
            assertFalse(source.getUrl().trim().isEmpty());
            assertNotNull(source.getRevision());
            assertFalse(source.getRevision().trim().isEmpty());
            assertFalse(source.getDerivedStrategyFamilies().isEmpty());
        }
        assertEquals("CC BY-NC-SA 3.0",
                registry.get(StrategySourceId.UIM_GENERAL).getLicense());
        assertTrue(registry.get(StrategySourceId.PVM_STRATEGY).getUrl()
                .contains("Guide:Bossing_Ladder"));
    }

    @Test
    public void highImpactIronAndUimMethodsUseDirectSkillGuideSources()
    {
        CuratedTrainingMethod gems = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.CRAFTING).stream()
                .filter(value -> "crafting_gems".equals(
                        value.getMethod().getId()))
                .findFirst().orElseThrow(AssertionError::new);
        MethodStrategyKnowledgeCatalog catalog =
                new MethodStrategyKnowledgeCatalog();

        assertTrue(catalog.profileFor(gems.getMethod(), gems.getMetadata(),
                        AccountMode.IRONMAN).getSources()
                .contains(StrategySourceId.IRONMAN_CRAFTING));

        CuratedTrainingMethod charter = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.CRAFTING).stream()
                .filter(value -> "crafting_charter_glass".equals(
                        value.getMethod().getId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(catalog.profileFor(charter.getMethod(),
                        charter.getMetadata(), AccountMode.ULTIMATE_IRONMAN)
                .getSources().contains(StrategySourceId.UIM_CRAFTING));
    }

    @Test
    public void sharedFootprintsComeFromTypedPropertiesNotMethodIds()
    {
        TrainingMethod agilityWithMiningId = method("mining_named_agility",
                Skill.AGILITY);
        TrainingMethod miningWithAgilityId = method("agility_named_mining",
                Skill.MINING);
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                TrainingIntensity.BALANCED, MethodCostTier.FREE,
                RiskLevel.NONE, true, true, true, true,
                Collections.emptyList());
        MethodStrategyKnowledgeCatalog catalog =
                new MethodStrategyKnowledgeCatalog();

        assertEquals(0, catalog.profileFor(agilityWithMiningId, metadata,
                        AccountMode.ULTIMATE_IRONMAN)
                .getInventoryFootprint().getMinimumPracticalFreeSlots());
        assertEquals(1, catalog.profileFor(miningWithAgilityId, metadata,
                        AccountMode.ULTIMATE_IRONMAN)
                .getInventoryFootprint().getMinimumPracticalFreeSlots());
    }

    @Test
    public void everyExplicitConventionalBankLoopIsExcludedFromUimGeneration()
    {
        MethodStrategyKnowledgeCatalog knowledge =
                new MethodStrategyKnowledgeCatalog();
        List<String> leaked = new ArrayList<>();
        ExpandedTrainingMethodCatalog expanded =
                new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog baseline = new F2pBaselineMethodCatalog();
        for (Skill skill : Skill.values())
        {
            List<CuratedTrainingMethod> methods = new ArrayList<>();
            methods.addAll(expanded.methodsFor(skill));
            methods.addAll(baseline.methodsFor(skill));
            for (CuratedTrainingMethod method : methods)
            {
                String instructions = method.getMethod().getInstructions()
                        .toLowerCase(java.util.Locale.ROOT);
                boolean conventional = instructions.contains("withdraw ")
                        || instructions.contains("bankstanding")
                        || instructions.contains("bank the logs")
                        || instructions.contains("bank each inventory")
                        || instructions.contains("bank immediately")
                        || instructions.contains("use the nearby bank")
                        || instructions.contains("bank/altar route");
                if (conventional && knowledge.profileFor(method.getMethod(),
                        method.getMetadata(), AccountMode.ULTIMATE_IRONMAN)
                        != null)
                    leaked.add(method.getMethod().getId());
            }
        }
        assertTrue("UIM conventional bank profiles: " + leaked,
                leaked.isEmpty());
    }

    @Test
    public void uimGeneratesLocalBronzeRouteBeforeRankingBankedBaseline()
    {
        StrategyDataBundle data = data(2, MembershipStatus.F2P,
                Collections.emptyList());
        TrainingPlan plan = selector.select(data, Skill.SMITHING, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false,
                false);

        assertNotNull(plan);
        assertEquals("smithing_f2p_uim_bronze",
                plan.getMethod().getId());
        assertEquals(MethodBankingBehavior.LOCAL_PROCESSING,
                plan.getStrategyProfile().getBankingBehavior());
        assertTrue(plan.getWhyThisMethod().contains("without conventional banking"));
    }

    @Test
    public void planRelativeFootprintChangesFullInventorySelection()
    {
        List<ItemStackSnapshot> full = new ArrayList<>();
        for (int slot = 0; slot < 28; slot++)
            full.add(new ItemStackSnapshot(10_000 + slot,
                    "Observed item " + slot, 1, slot));

        TrainingPlan emptyInventory = selector.select(
                data(2, MembershipStatus.F2P, Collections.emptyList()),
                Skill.SMITHING, 1, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false, false);
        TrainingPlan fullInventory = selector.select(
                data(2, MembershipStatus.F2P, full), Skill.SMITHING, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false,
                false);

        assertNotNull(emptyInventory);
        assertNull(fullInventory);
    }

    @Test
    public void finalResolvedBankBehaviorIsRejectedForUim()
    {
        TrainingMethod method = new F2pBaselineMethodCatalog()
                .methodsFor(Skill.SMITHING).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(value -> "smithing_f2p_bronze".equals(value.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        TrainingMethodMetadata metadata = TrainingMethodMetadata.legacy(method);
        MethodStrategyProfile profile = new MethodStrategyKnowledgeCatalog()
                .profileFor(method, metadata, AccountMode.MAIN);
        TrainingPlan plan = new TrainingPlan(method, "Main bank loop",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList(), profile);
        Recommendation recommendation = new Recommendation("skill:smithing",
                "Train Smithing to 2", "Cheap first level.", 10.0, plan,
                RecommendationConfidence.VERIFIED, 1, 2,
                new RecommendationGuidance("Smith bronze items.",
                        "Hammer and bronze bars.", "Varrock West anvils.",
                        null, MethodBankingBehavior.CONVENTIONAL_BANK_LOOP),
                CandidateSafetyEvidence.skill(true, Skill.SMITHING));

        StrategyContext uim = context(data(2, MembershipStatus.F2P,
                Collections.emptyList()));
        Recommendation validated = new FinalExecutionPlanValidator()
                .validate(recommendation, uim);
        assertTrue(validated.getSafetyEvidence()
                .isConventionalBankRequired());
        assertFalse(new CandidateSafetyPolicy().isAllowed(validated, uim));
    }

    @Test
    public void liveUimSmithingCardHasMethodAndCoherentNoBankGuidance()
    {
        StrategyDataBundle data = data(2, MembershipStatus.F2P,
                Collections.emptyList());
        Recommendation smithing = new RecommendationEngine(selector)
                .recommendAll(data, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, false, false,
                        new PreferenceProfile()).stream()
                .filter(value -> "skill:smithing".equals(value.getId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertNotNull(smithing.getTrainingPlan().getMethod().getName());
        assertFalse(smithing.getTrainingPlan().getMethod().getName().trim().isEmpty());
        assertNotNull(smithing.getGuidance());
        String execution = (smithing.getGuidance().getAction() + " "
                + smithing.getGuidance().getSupplies() + " "
                + smithing.getGuidance().getLocation()).toLowerCase();
        assertFalse(execution.contains("withdraw"));
        assertFalse(execution.contains("bank, and repeat"));
        assertTrue(execution.contains("lumbridge"));
        assertTrue(smithing.getReason().contains("cheap first Smithing level"));
    }

    private static StrategyContext context(StrategyDataBundle data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
    }

    private static TrainingMethod method(String id, Skill skill)
    {
        return new TrainingMethod(id, skill, 1, 99, "Typed method",
                "Use the typed route.", 1, 1, 1, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(),
                RecommendationConfidence.VERIFIED, false, false, false);
    }

    private static StrategyDataBundle data(int type,
            MembershipStatus membership, List<ItemStackSnapshot> inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, 0);
        }
        levels.put(Skill.SMITHING, 1);
        AccountSnapshot account = new AccountSnapshot("Strategy test",
                8_000L + type, type, AccountMode.fromTypeCode(type).name(),
                membership, 0, 805, 0L, levels, xp);
        return StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(inventory, true))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
    }
}
