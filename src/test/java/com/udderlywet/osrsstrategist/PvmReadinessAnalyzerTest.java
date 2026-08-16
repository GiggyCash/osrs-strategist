package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmReadinessAnalyzerTest
{
    private final PvmReadinessAnalyzer analyzer =
            new PvmReadinessAnalyzer(new PvmEncounterCatalog());

    @Test
    public void unlockedHighStatAccountGetsLoadoutCheckForVorkath()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Dragon Slayer II", QuestStatus.COMPLETE);
        PvmReadiness readiness = analyzer.analyze(
                account(0, 85), new QuestSnapshot(quests)).readinessFor("Vorkath");

        assertFalse(readiness.isRealisticallyReady());
        assertTrue(readiness.getMissingRequirements().get(0)
                .startsWith("Verify practical gear"));
    }

    @Test
    public void hardcoreKeepsHighRiskBossBehindSafetyCheck()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Dragon Slayer II", QuestStatus.COMPLETE);
        PvmReadiness readiness = analyzer.analyze(
                account(3, 90), new QuestSnapshot(quests)).readinessFor("Vorkath");

        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Hardcore safety")));
    }

    @Test
    public void wildernessBossAlwaysRequiresRiskPlan()
    {
        PvmReadiness readiness = analyzer.analyze(
                account(0, 90), new QuestSnapshot(new HashMap<>()))
                .readinessFor("King Black Dragon");
        assertTrue(readiness.getMissingRequirements().stream()
                .anyMatch(value -> value.contains("Wilderness")));
    }

    private static AccountSnapshot account(int type, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "PvM", type, "Test", MembershipStatus.P2P, 1,
                2000, 0L, levels, xp);
    }
}
