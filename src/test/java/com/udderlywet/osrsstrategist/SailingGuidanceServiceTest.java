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
        GameData data = GameData.builder(account)
                .quests(completed("Pandemonium"))
                .build();

        Guidance guidance = service.build(
                data,
                30,
                40,
                plan("sailing_barracuda_tantrum"));

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Swordfish, Shark, then Marlin"));
        assertTrue(guidance.getAction().contains("1,250 XP each"));
        assertTrue(guidance.getSupplies().contains("iron helm"));
        assertTrue(guidance.getSupplies().contains("oak masts"));
        assertTrue(guidance.getNote().contains("one-time rank bonuses"));
    }

    @Test
    public void courierGivesOneExactStarterLoopAndLogRecovery()
    {
        Guidance guidance = service.build(
                GameData.builder(account(10,
                                Experience.getXpForLevel(10)))
                        .quests(completed("Pandemonium"))
                        .build(),
                10, 15, plan("sailing_courier"));

        assertTrue(guidance.getAction().contains("Port Sarim notice board"));
        assertTrue(guidance.getAction().contains("The Pandemonium"));
        assertTrue(guidance.getSupplies().contains("Junior Jim"));
        assertTrue(guidance.getLocation().contains("loading bay"));
    }

    @Test
    public void lockedSailingTellsPlayerToCompletePandemonium()
    {
        AccountSnapshot account = account(1, Experience.getXpForLevel(1));
        GameData data = GameData.builder(account)
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();

        Guidance guidance = service.build(
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
        GameData data = GameData.builder(account)
                .quests(completed("Pandemonium"))
                .economy(new AccountEconomySnapshot(
                        20000,
                        0,
                        Confidence.VERIFIED))
                .build();

        Guidance guidance = service.build(
                data,
                15,
                20,
                plan("sailing_charting"));

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("15,000-coin skiff"));
        assertTrue(guidance.getSupplies().contains("enough verified cash"));
    }

    @Test
    public void salvagingNamesOneExactSafeBaselineWithoutDroppingItems()
    {
        Guidance guidance = service.build(
                GameData.builder(account(53,
                                Experience.getXpForLevel(53)))
                        .quests(completed("Pandemonium")).build(),
                53, 54, plan("sailing_salvage_small"));

        assertTrue(guidance.getAction().contains("Small shipwreck"));
        assertTrue(guidance.getLocation().contains("Kharidian Sea"));
        assertTrue(guidance.getLocation().contains("The Pandemonium"));
        assertTrue(guidance.getSupplies().contains("no item-dropping"));
        assertTrue(new RecommendationQualityPolicy().isPresentable(
                recommendation(guidance)));
    }

    private static Recommendation recommendation(
            Guidance guidance)
    {
        return new Recommendation("skill:sailing", "Train Sailing to 54",
                "Exact salvage baseline.", 10.0,
                plan("sailing_salvage_small"),
                Confidence.VERIFIED, 53, 54, guidance,
                SafetyEvidence.skill(false, Skill.SAILING));
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
                Confidence.VERIFIED);
        return new TrainingPlan(
                method,
                "test",
                Confidence.VERIFIED,
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
