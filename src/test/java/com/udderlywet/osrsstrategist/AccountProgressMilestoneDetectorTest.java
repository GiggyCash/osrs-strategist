package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountProgressMilestoneDetectorTest
{
    @Test
    public void firstSnapshotIsBaselineNotAListOfFakeAchievements()
    {
        AccountProgressMilestoneDetector detector =
                new AccountProgressMilestoneDetector();
        assertTrue(detector.observe(data(1L, QuestStatus.COMPLETE, true),
                GoalType.BARROWS_GLOVES, 1L).isEmpty());
    }

    @Test
    public void successiveLiveStateDetectsQuestTransportAndStorage()
    {
        AccountProgressMilestoneDetector detector =
                new AccountProgressMilestoneDetector();
        detector.observe(data(1L, QuestStatus.NOT_STARTED, false),
                GoalType.BARROWS_GLOVES, 1L);
        java.util.List<ProgressMilestone> milestones = detector.observe(
                data(1L, QuestStatus.COMPLETE, true),
                GoalType.BARROWS_GLOVES, 2L);

        assertEquals(3, milestones.size());
        assertTrue(milestones.stream().anyMatch(value ->
                value.getType() == ProgressMilestoneType.QUEST));
        assertTrue(milestones.stream().anyMatch(value ->
                value.getType() == ProgressMilestoneType.TRANSPORT));
        assertTrue(milestones.stream().anyMatch(value ->
                value.getType() == ProgressMilestoneType.INFRASTRUCTURE));
    }

    @Test
    public void accountSwitchRebaselinesWithoutLeakingMilestones()
    {
        AccountProgressMilestoneDetector detector =
                new AccountProgressMilestoneDetector();
        detector.observe(data(1L, QuestStatus.NOT_STARTED, false),
                GoalType.BARROWS_GLOVES, 1L);
        assertTrue(detector.observe(data(2L, QuestStatus.COMPLETE, true),
                GoalType.BARROWS_GLOVES, 2L).isEmpty());
    }

    private static StrategyDataBundle data(long hash, QuestStatus quest,
            boolean unlocked)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Progress", hash, 0,
                "MAIN", MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Cook's Assistant", quest);
        Map<StorageCapability, CapabilityState> storage =
                new EnumMap<>(StorageCapability.class);
        storage.put(StorageCapability.POH_COSTUME_ROOM, unlocked
                ? CapabilityState.VERIFIED : CapabilityState.UNKNOWN);
        return StrategyDataBundle.builder(account)
                .quests(new QuestSnapshot(quests))
                .transport(new TransportSnapshot(unlocked
                        ? Collections.singleton("spirit-trees")
                        : Collections.emptySet()))
                .storage(new StorageSnapshot(storage))
                .build();
    }
}
