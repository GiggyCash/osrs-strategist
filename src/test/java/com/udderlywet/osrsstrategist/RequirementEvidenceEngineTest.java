package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RequirementEvidenceEngineTest
{
    @Test
    public void earlyFarmingExplainsKnownAccessAndUnfinishedSeedCatalog()
    {
        FarmingAccessEvaluator accessEvaluator =
                new FarmingAccessEvaluator(new FarmingAccessCatalog());
        RequirementEvidenceEngine engine =
                new RequirementEvidenceEngine(accessEvaluator);

        FarmingSnapshot farming = new FarmingSnapshot(
                Collections.singleton("falador"),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        StrategyDataBundle data = StrategyDataBundle
                .builder(account(9))
                .farming(farming)
                .build();

        TrainingMethod method = method("farming_early");
        java.util.List<RequirementCheck> checks =
                engine.evaluate(data, method);

        assertEquals(2, checks.size());
        assertEquals(RequirementState.VERIFIED, checks.get(0).getState());
        assertTrue(checks.get(0).getEvidence().contains("Falador"));
        assertEquals(RequirementState.CHECK_NEEDED, checks.get(1).getState());
        assertTrue(checks.get(1).getEvidence().contains("source is not verified"));
    }

    @Test
    public void herbRunUsesObservedBankAndLeprechaunResources()
    {
        FarmingAccessEvaluator accessEvaluator =
                new FarmingAccessEvaluator(new FarmingAccessCatalog());
        RequirementEvidenceEngine engine =
                new RequirementEvidenceEngine(accessEvaluator);

        Map<String, CapabilityState> tools = new HashMap<>();
        tools.put("rake", CapabilityState.VERIFIED);
        tools.put("dibber", CapabilityState.VERIFIED);
        tools.put("spade", CapabilityState.VERIFIED);
        FarmingSnapshot farming = new FarmingSnapshot(
                Collections.singleton("falador"),
                tools,
                Collections.emptyMap()
        );

        StrategyDataBundle data = StrategyDataBundle
                .builder(account(32))
                .farming(farming)
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(ItemID.RANARR_SEED, "Ranarr seed", 2)
                ), 1L))
                .build();

        java.util.List<RequirementCheck> checks =
                engine.evaluate(data, method("farming_herbs"));

        assertEquals(6, checks.size());
        for (RequirementCheck check : checks)
        {
            assertEquals(check.getLabel(),
                    RequirementState.VERIFIED,
                    check.getState());
        }
    }

    @Test
    public void miningRequiresAnObservedImmediatelyUsablePickaxe()
    {
        RequirementEvidenceEngine engine = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()));
        TrainingMethod mining = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.MINING).stream()
                .filter(candidate -> "mining_mlm".equals(
                        candidate.getMethod().getId()))
                .findFirst().orElseThrow(AssertionError::new).getMethod();

        StrategyDataBundle empty = StrategyDataBundle.builder(account(1))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();
        assertEquals(RequirementState.CHECK_NEEDED,
                engine.evaluate(empty, mining).get(0).getState());

        StrategyDataBundle ready = StrategyDataBundle.builder(account(1))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(ItemID.BRONZE_PICKAXE,
                                "Bronze pickaxe", 1))))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();
        assertEquals(RequirementState.VERIFIED,
                engine.evaluate(ready, mining).get(0).getState());
    }

    @Test
    public void observedNonStandardSpellbookBlocksStandardCombatMagic()
    {
        RequirementEvidenceEngine engine = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()));
        TrainingMethod fireBlast = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.MAGIC).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(method -> "magic_f2p_fire_blast".equals(method.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        StrategyDataBundle data = StrategyDataBundle.builder(account(1))
                .combatEvidence(new CombatEvidenceSnapshot(1,
                        Collections.emptySet(), false, false, false))
                .build();

        java.util.List<RequirementCheck> checks = engine.evaluate(data, fireBlast);
        assertEquals("spellbook:standard", checks.get(0).getId());
        assertEquals(RequirementState.BLOCKED, checks.get(0).getState());
    }

    @Test
    public void unknownSpellbookCannotBecomeVerifiedFromRunesAlone()
    {
        RequirementEvidenceEngine engine = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()));
        TrainingMethod fireBlast = expanded("magic_f2p_fire_blast", Skill.MAGIC);

        java.util.List<RequirementCheck> checks = engine.evaluate(
                StrategyDataBundle.builder(account(1)).build(), fireBlast);

        assertEquals("spellbook:standard", checks.get(0).getId());
        assertEquals(RequirementState.CHECK_NEEDED, checks.get(0).getState());
    }

    @Test
    public void realAccessRulesReplaceSyntheticMinigameUnlocks()
    {
        RequirementEvidenceEngine engine = new RequirementEvidenceEngine(
                new FarmingAccessEvaluator(new FarmingAccessCatalog()));
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Temple of the Eye", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(account(80))
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(java.util.Arrays.asList(
                        new ItemStackSnapshot(ItemID.BRONZE_PICKAXE,
                                "Bronze pickaxe", 1),
                        new ItemStackSnapshot(ItemID.CHISEL, "Chisel", 1)), 1L))
                .build();

        java.util.List<RequirementCheck> gotr = engine.evaluate(data,
                expanded("runecraft_gotr", Skill.RUNECRAFT));
        assertEquals("quest:temple_of_the_eye", gotr.get(0).getId());
        assertEquals(RequirementState.VERIFIED, gotr.get(0).getState());
        assertTrue(gotr.stream().allMatch(check ->
                check.getState() == RequirementState.VERIFIED));

        assertTrue(engine.evaluate(data,
                expanded("fishing_tempoross", Skill.FISHING)).isEmpty());

        java.util.List<RequirementCheck> tithe = engine.evaluate(data,
                expanded("farming_tithe", Skill.FARMING));
        assertTrue(tithe.stream().allMatch(check ->
                check.getId().startsWith("resource:")));
    }

    private static TrainingMethod method(String id)
    {
        return new TrainingMethodDatabase()
                .methodsFor(Skill.FARMING)
                .stream()
                .filter(candidate -> id.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static TrainingMethod expanded(String id, Skill skill)
    {
        return new ExpandedTrainingMethodCatalog().methodsFor(skill).stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(candidate -> id.equals(candidate.getId()))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static AccountSnapshot account(int farmingLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.FARMING ? farmingLevel : 1);
            xp.put(skill, 0);
        }

        return new AccountSnapshot(
                "Tester",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1,
                0L,
                levels,
                xp
        );
    }
}
