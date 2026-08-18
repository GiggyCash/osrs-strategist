package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VariableMethodGuidanceServiceTest
{
    private final VariableMethodGuidanceService service =
            new VariableMethodGuidanceService();

    @Test
    public void wintertodtGivesConcreteSetupWithoutFakeKillCount()
    {
        StrategyDataBundle data = data(Skill.FIREMAKING, 60,
                new ItemStackSnapshot(20704, "Bruma torch", 1));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.FIREMAKING,
                60,
                70,
                plan("firemaking_wintertodt", Skill.FIREMAKING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("500 personal points"));
        assertTrue(guidance.getSupplies().contains("four warm items"));
        assertTrue(guidance.getSupplies().contains("Bruma torch"));
        assertFalse(guidance.getAction().matches(".*about [0-9]+ (games|kills).*"));
        assertTrue(guidance.getNote().contains("without inventing a fixed kill count"));
    }

    @Test
    public void temporossUsesObservedHarpoonAndNoFakeGameCount()
    {
        StrategyDataBundle data = data(Skill.FISHING, 70,
                new ItemStackSnapshot(11920, "Dragon harpoon", 1));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.FISHING,
                70,
                80,
                plan("fishing_tempoross", Skill.FISHING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Dragon harpoon"));
        assertFalse(guidance.getAction().matches(".*about [0-9]+ games.*"));
    }

    @Test
    public void foundryExplainsTwentyEightBarCommissionWithoutFakeSwordCount()
    {
        StrategyDataBundle data = data(Skill.SMITHING, 70,
                new ItemStackSnapshot(2359, "Mithril bar", 500));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.SMITHING,
                70,
                80,
                plan("smithing_foundry", Skill.SMITHING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("28 bars"));
        assertTrue(guidance.getSupplies().contains("Mithril bar"));
        assertFalse(guidance.getAction().matches(".*about [0-9]+ .*swords.*"));
    }

    private static StrategyDataBundle data(
            Skill skill,
            int level,
            ItemStackSnapshot observed)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values())
        {
            levels.put(s, 60);
            xp.put(s, Experience.getXpForLevel(60));
        }
        levels.put(skill, level);
        xp.put(skill, Experience.getXpForLevel(level));
        AccountSnapshot account = new AccountSnapshot(
                "Variable Test",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
        return StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(
                        Collections.singletonList(observed), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList());
    }
}
