package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearAcquisitionResolverTest
{
    @Test
    public void tenRealTargetsTraverseFourOrMoreCrossDomainEdges()
    {
        GearAcquisitionResolver resolver = new GearAcquisitionResolver(
                new GearAcquisitionCatalog(), new QuestKnowledgeCatalog());
        for (String target : Arrays.asList(
                "Bow of faerdhinen", "Crystal armour", "Ava's assembler",
                "Barrows gloves", "Ferocious gloves", "Avernic defender",
                "Bow of faerdhinen", "Crystal armour", "Neitiznot faceguard",
                "Neitiznot faceguard"))
        {
            GearAcquisitionResolution result = resolver.resolve(target, null);
            assertTrue(target + " should expose at least four dependency edges",
                    result.getSteps().size() >= 4);
            assertFalse(target, result.isCyclePrevented());
        }
    }

    @Test
    public void completedQuestEvidenceSkipsSatisfiedEdges()
    {
        java.util.Map<String, QuestStatus> statuses = new java.util.HashMap<>();
        statuses.put("Mourning's End Part II", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .quests(new QuestSnapshot(statuses)).build();
        StrategyContext context = new StrategyContext(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.GEAR_TARGET, false, false,
                new PreferenceProfile());
        GearAcquisitionResolution result = new GearAcquisitionResolver(
                new GearAcquisitionCatalog(), new QuestKnowledgeCatalog())
                .resolve("Bow of faerdhinen", context);
        assertTrue(result.getSteps().stream().noneMatch(step ->
                "Mourning's End Part II".equals(step.getTarget())));
    }
}
