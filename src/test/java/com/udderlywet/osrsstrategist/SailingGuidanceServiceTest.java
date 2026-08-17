package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SailingGuidanceServiceTest
{
    private final SailingGuidanceService service = new SailingGuidanceService();

    @Test
    public void temporTantrumUsesRepeatableMarlinCompletionMath()
    {
        int currentXp = Experience.getXpForLevel(30);
        AccountSnapshot account = account(30, currentXp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .quests(completed("Pandemonium"))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                30,
                40,
                plan("sailing_barracuda_tantrum"));

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Marlin-rank"));
        assertTrue(guidance.getAction().contains("1,250 XP each"));
        assertTrue(guidance.getSupplies().contains("iron helm"));
        assertTrue(guidance.getSupplies().contains("oak masts"));
        assertTrue(guidance.getNote().contains("one-time rank bonuses"));
    }

    @Test
    public void lockedSailingTellsPlayerToCompletePandemonium()
    {
        AccountSnapshot account = account(1, Experience.getXpForLevel(1));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                1,
                10,
                plan("sailing_charting"));

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Complete Pandemonium"));
    }

    @Test
    public void chartingUsesVerifiedCashForSkiffDecision()
    {
        AccountSnapshot account = account(15, Experience.getXpForLevel(15));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .quests(completed("Pandemonium"))
                .economy(new AccountEconomySnapshot(
                        20000,
                        0,
                        RecommendationConfidence.VERIFIED))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                15,
                20,
                plan("sailing_charting"));

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("15,000-coin skiff"));
        assertTrue(guidance.getSupplies().contains("enough verified cash"));
    }

    private static TrainingPlan plan(String id)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                Skill.SAILING,
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

    private static AccountSnapshot account(int level, int xpValue)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        levels.put(Skill.SAILING, level);
        xp.put(Skill.SAILING, xpValue);
        return new AccountSnapshot(
                "Sailing Test",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
    }

    private static QuestSnapshot completed(String quest)
    {
        Map<String, QuestStatus> states = new HashMap<>();
        states.put(quest, QuestStatus.COMPLETE);
        return new QuestSnapshot(states);
    }
}
