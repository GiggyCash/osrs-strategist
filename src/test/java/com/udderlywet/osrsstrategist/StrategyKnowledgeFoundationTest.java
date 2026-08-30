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
        }
        assertEquals("CC BY-NC-SA 3.0",
                registry.get(StrategySourceId.UIM_GENERAL).getLicense());
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
