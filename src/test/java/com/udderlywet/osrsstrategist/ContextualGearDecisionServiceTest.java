package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class ContextualGearDecisionServiceTest
{
    @Test
    public void assessmentAnswersAllSevenQuestionsWithoutUniversalBisClaim()
    {
        GearProgressionEntry entry = new GearProgressionCatalog()
                .forStyle(CombatStyle.RANGED).stream()
                .filter(value -> value.getTier() == GearBudgetTier.MIDGAME)
                .findFirst().orElseThrow(AssertionError::new);
        StrategyContext context = context(1,
                new ItemsState(Arrays.asList(
                        new ItemState(1, "Crystal body", 1)), 1L));

        ContextualGearAssessment assessment =
                new ContextualGearDecisionService().assess(entry, context);

        assertEquals(GearDecisionKind.values().length, assessment.all().size());
        assertEquals(Confidence.VERIFIED,
                assessment.get(GearDecisionKind.BEST_OWNED).getConfidence());
        assertEquals("Crystal body",
                assessment.get(GearDecisionKind.BEST_OWNED).getValue());
        assertTrue(assessment.get(GearDecisionKind.TARGET_SPECIFIC_BEST)
                .getValue().contains("Bowfa"));
        for (ContextualGearDecision decision : assessment.all().values())
            assertFalse(decision.getValue().trim().isEmpty());
    }

    @Test
    public void unopenedBankDoesNotTurnCompoundSlotProseIntoMissingItems()
    {
        GearProgressionEntry entry = new GearProgressionCatalog()
                .forStyle(CombatStyle.MELEE_SLASH).stream()
                .filter(value -> value.getTier() == GearBudgetTier.MIDGAME)
                .findFirst().orElseThrow(AssertionError::new);
        ContextualGearAssessment assessment =
                new ContextualGearDecisionService().assess(entry,
                        context(0, null));

        assertTrue(assessment.get(GearDecisionKind.BEST_OWNED).getValue()
                .contains("Open the bank"));
        assertEquals(Confidence.CHECK_NEEDED,
                assessment.get(GearDecisionKind.BEST_OWNED).getConfidence());
        assertFalse(ContextualGearDecisionService.isExactOwnershipTarget(
                "Dragon/Avernic defender"));
        assertFalse(ContextualGearDecisionService.isExactOwnershipTarget(
                "Amulet of torture or rancour"));
    }

    @Test
    public void ironAvailableNowUsesSelfSourceAndMainRequiresEconomics()
    {
        GearProgressionEntry entry = new GearProgressionCatalog()
                .forStyle(CombatStyle.MELEE_SLASH).stream()
                .filter(value -> value.getTier() == GearBudgetTier.MIDGAME)
                .findFirst().orElseThrow(AssertionError::new);
        ItemsState observed = new ItemsState(Collections.emptyList(), 1L);
        ContextualGearDecisionService service =
                new ContextualGearDecisionService();

        String iron = service.assess(entry, context(1, observed))
                .get(GearDecisionKind.BEST_AVAILABLE_NOW).getValue();
        String main = service.assess(entry, context(0, observed))
                .get(GearDecisionKind.BEST_AVAILABLE_NOW).getValue();
        assertTrue(iron, iron.contains("self-source"));
        assertTrue(main, main.contains("live price"));
    }

    private static StrategyContext context(int type, ItemsState bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 80);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Gear", 301L, type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P,
                1, 80 * Skill.values().length, 0L, levels, xp);
        GameData.Builder data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .quests(new QuestSnapshot(Collections.emptyMap()));
        if (bank != null) data.bank(bank);
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.GEAR_TARGET, false, false,
                new PreferenceProfile());
    }
}
