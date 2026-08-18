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

    private static TrainingMethod method(String id)
    {
        return new TrainingMethodDatabase()
                .methodsFor(Skill.FARMING)
                .stream()
                .filter(candidate -> id.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(AssertionError::new);
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
