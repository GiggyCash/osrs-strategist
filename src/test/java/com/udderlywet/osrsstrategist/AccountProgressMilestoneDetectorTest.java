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

    @Test
    public void detectsDiaryTierAndSlayerRewardWithoutRepeatingThem()
    {
        AccountProgressMilestoneDetector detector =
                new AccountProgressMilestoneDetector();
        detector.observe(progressData(false), GoalType.AUTOMATIC, 1L);

        java.util.List<ProgressMilestone> first = detector.observe(
                progressData(true), GoalType.AUTOMATIC, 2L);
        java.util.List<ProgressMilestone> duplicate = detector.observe(
                progressData(true), GoalType.AUTOMATIC, 3L);

        assertEquals(2, first.size());
        assertTrue(first.stream().anyMatch(value -> value.getType()
                == ProgressMilestoneType.DIARY));
        assertTrue(first.stream().anyMatch(value -> value.getType()
                == ProgressMilestoneType.SLAYER_UNLOCK));
        assertTrue(duplicate.isEmpty());
    }

    private static GameData data(long hash, QuestStatus quest,
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
        return GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .transport(new TransportSnapshot(unlocked
                        ? Collections.singleton("spirit-trees")
                        : Collections.emptySet()))
                .storage(new StorageSnapshot(storage))
                .build();
    }

    private static GameData progressData(boolean complete)
    {
        GameData base = data(9L, QuestStatus.NOT_STARTED, false);
        Map<DiaryTier, Boolean> tiers = new EnumMap<>(DiaryTier.class);
        tiers.put(DiaryTier.MEDIUM, complete);
        Map<String, Map<DiaryTier, Boolean>> regions = new HashMap<>();
        regions.put("Varrock", tiers);
        Map<SlayerReward, CapabilityState> rewards =
                new EnumMap<>(SlayerReward.class);
        rewards.put(SlayerReward.BIGGER_AND_BADDER, complete
                ? CapabilityState.VERIFIED : CapabilityState.BLOCKED);
        return GameData.builder(base.account())
                .diaries(new DiarySnapshot(Collections.emptyMap(),
                        Collections.emptyMap(), regions))
                .slayer(new SlayerSnapshot(null, 0, "Nieve", null, 0,
                        10, 100, 2, 0,
                        new SlayerRewardSnapshot(rewards),
                        Confidence.VERIFIED))
                .build();
    }
}
