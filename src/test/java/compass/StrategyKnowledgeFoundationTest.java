package compass;

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
            new TrainingMethodCatalog(), null,
            new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
            new UimInventoryResolutionService());

    @Test
    public void sourceRegistryCoversEveryStableSourceIdWithoutRuntimeNetworking()
    {
        StrategySourceRegistry registry = new StrategySourceRegistry();
        assertEquals(Source.values().length, registry.all().size());
        for (Source id : Source.values())
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
                registry.get(Source.UIM_GENERAL).getLicense());
        assertTrue(registry.get(Source.PVM_STRATEGY).getUrl()
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
                .contains(Source.IRONMAN_CRAFTING));

        CuratedTrainingMethod charter = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.CRAFTING).stream()
                .filter(value -> "crafting_charter_glass".equals(
                        value.getMethod().getId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(catalog.profileFor(charter.getMethod(),
                        charter.getMetadata(), AccountMode.ULTIMATE_IRONMAN)
                .getSources().contains(Source.UIM_CRAFTING));
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
        GameData data = data(2, Membership.F2P,
                Collections.emptyList());
        TrainingPlan plan = selector.select(data, Skill.SMITHING, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false,
                false);

        assertNotNull(plan);
        assertEquals("smithing_f2p_uim_bronze",
                plan.getMethod().getId());
        assertEquals(BankingMode.LOCAL_PROCESSING,
                plan.getStrategyProfile().getBankingBehavior());
        assertTrue(plan.getWhyThisMethod().contains("without conventional banking"));
    }

    @Test
    public void planRelativeFootprintChangesFullInventorySelection()
    {
        List<ItemState> full = new ArrayList<>();
        for (int slot = 0; slot < 28; slot++)
            full.add(new ItemState(10_000 + slot,
                    "Observed item " + slot, 1, slot));

        TrainingPlan emptyInventory = selector.select(
                data(2, Membership.F2P, Collections.emptyList()),
                Skill.SMITHING, 1, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false, false);
        TrainingPlan fullInventory = selector.select(
                data(2, Membership.F2P, full), Skill.SMITHING, 1,
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
                Confidence.VERIFIED,
                Collections.emptyList(), profile);
        Recommendation recommendation = new Recommendation("skill:smithing",
                "Train Smithing to 2", "Cheap first level.", 10.0, plan,
                Confidence.VERIFIED, 1, 2,
                new Guidance("Smith bronze items.",
                        "Hammer and bronze bars.", "Varrock West anvils.",
                        null, BankingMode.CONVENTIONAL_BANK_LOOP),
                Safety.skill(true, Skill.SMITHING));

        StrategyContext uim = context(data(2, Membership.F2P,
                Collections.emptyList()));
        assertFalse(new CandidateSafetyPolicy().isAllowed(recommendation, uim));
    }

    @Test
    public void liveUimSmithingCardHasMethodAndCoherentNoBankGuidance()
    {
        GameData data = data(2, Membership.F2P,
                Collections.emptyList());
        Recommendation smithing = TestFixtures.recommendationEngine(selector)
                .recommendAll(data, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, false, false,
                        GoalType.AUTOMATIC, new PreferenceProfile()).stream()
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

    private static StrategyContext context(GameData data)
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
                Confidence.VERIFIED, false, false, false);
    }

    private static GameData data(int type,
            Membership membership, List<ItemState> inventory)
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
        return GameData.builder(account)
                .inventory(new ItemsState(inventory, true))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
    }
}
