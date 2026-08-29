package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SlayerStrategistTest
{
    private final SlayerStrategist strategist = new SlayerStrategist();

    @Test
    public void unknownLiveStateIsNotTreatedAsNoTask()
    {
        SlayerDecisionResult result = strategist.assess(context(0,
                SlayerSnapshot.unknown(), StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, GoalType.SLAYER_85,
                false, Collections.emptyList(), null));

        assertEquals(SlayerAssignmentState.UNKNOWN, result.getAssignmentState());
        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                result.getConfidence());
        assertTrue(result.getGuidance().getNote().contains("kept unknown"));
    }

    @Test
    public void ordinaryNoTaskSelectionUsesMasterProperties()
    {
        SlayerSnapshot noTask = snapshot(null, 0, null, null,
                500, 20, 300, 6, 2);
        SlayerDecisionResult result = strategist.assess(context(0, noTask,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));

        assertEquals(SlayerAssignmentState.NO_TASK, result.getAssignmentState());
        assertNull(result.getDecision());
        assertEquals("duradel", result.getMaster().getId());
        assertTrue(result.getGuidance().getAction().contains("Duradel/Kuradal"));
    }

    @Test
    public void liveLockedRewardCanLeadBeforeTheNextAssignment()
    {
        EnumMap<SlayerReward, CapabilityState> rewardStates =
                new EnumMap<>(SlayerReward.class);
        for (SlayerReward reward : SlayerReward.values())
            rewardStates.put(reward, CapabilityState.BLOCKED);
        SlayerSnapshot noTask = new SlayerSnapshot(null, 0, null, null,
                100, 20, 300, 6, null,
                new SlayerRewardSnapshot(rewardStates),
                RecommendationConfidence.VERIFIED);

        StrategyContext context = context(1, noTask, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION, GoalType.SLAYER_85, false,
                Collections.emptyList(), null);
        SlayerDecisionResult result = strategist.assess(context);

        assertEquals(SlayerReward.BIGGER_AND_BADDER,
                result.getRecommendedReward());
        assertTrue(result.getGuidance().getAction()
                .contains("Bigger and Badder"));
        assertTrue(result.getGuidance().getNote().contains("30-point"));
        StrategyCandidate candidate = new SlayerCandidateProvider()
                .candidates(context).get(0);
        assertEquals("slayer:unlock:bigger-and-badder", candidate.getId());
    }

    @Test
    public void unknownRewardOwnershipDoesNotCreateAPurchaseClaim()
    {
        SlayerSnapshot noTask = snapshot(null, 0, null, null,
                500, 20, 300, 6, null);
        SlayerDecisionResult result = strategist.assess(context(0, noTask,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));

        assertNull(result.getRecommendedReward());
        assertEquals("duradel", result.getMaster().getId());
    }

    @Test
    public void extensionPurchaseRequiresMatchingGoalSessionAndLiveLockState()
    {
        EnumMap<SlayerReward, CapabilityState> rewardStates =
                new EnumMap<>(SlayerReward.class);
        for (SlayerReward reward : SlayerReward.values())
            rewardStates.put(reward, CapabilityState.VERIFIED);
        rewardStates.put(SlayerReward.EXTEND_NECHRYAELS,
                CapabilityState.BLOCKED);
        SlayerSnapshot noTask = new SlayerSnapshot(null, 0, null, null,
                200, 20, 300, 6, null,
                new SlayerRewardSnapshot(rewardStates),
                RecommendationConfidence.VERIFIED);

        SlayerDecisionResult longSession = strategist.assess(context(1,
                noTask, StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));
        assertEquals(SlayerReward.EXTEND_NECHRYAELS,
                longSession.getRecommendedReward());
        assertTrue(longSession.getGuidance().getNote()
                .contains("cancellation reserve"));

        SlayerDecisionResult quickSession = strategist.assess(context(1,
                noTask, StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));
        assertNull(quickSession.getRecommendedReward());
    }

    @Test
    public void milestonePointEconomyMateriallyInfluencesMasterScore()
    {
        SlayerSnapshot noTask = snapshot(null, 0, null, null,
                500, 9, 300, 6, 2);
        SlayerDecisionResult milestone = strategist.assess(context(0, noTask,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));
        SlayerSnapshot ordinaryState = snapshot(null, 0, null, null,
                500, 20, 300, 6, 2);
        SlayerDecisionResult ordinary = strategist.assess(context(0,
                ordinaryState, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, GoalType.AUTOMATIC, false,
                Collections.emptyList(), null));

        assertEquals("duradel", milestone.getMaster().getId());
        assertTrue(milestone.getReason().contains("task 10"));
        assertTrue(milestone.getScore() > ordinary.getScore() + 50.0);
    }

    @Test
    public void mandatoryProtectionShortfallProducesPreparation()
    {
        SlayerSnapshot task = snapshot("Dust devils", 140, "Duradel",
                null, 500, 21, 300, 6, 2);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                result.getConfidence());
        assertTrue(result.getReason().contains("mandatory"));
    }

    @Test
    public void verifiedDustDevilProtectionAllowsDoDecision()
    {
        SlayerSnapshot task = snapshot("Dust devils", 140, "Duradel",
                null, 500, 21, 300, 6, 2);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false,
                Collections.singletonList(new ItemStackSnapshot(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())), null));

        assertEquals(SlayerTaskDecision.DO, result.getDecision());
        assertEquals(RecommendationConfidence.VERIFIED,
                result.getConfidence());
        assertTrue(result.getGuidance().getAction().contains("remaining 140"));
        assertTrue(result.getGuidance().getAction().contains("Abyssal whip"));
        assertTrue(result.getReason().contains("properties"));
    }

    @Test
    public void magicOnlyTaskRejectsObservedMeleeWeaponBeforeDoDecision()
    {
        SlayerSnapshot task = snapshot("Cave krakens", 110, "Duradel",
                null, 500, 21, 300, 6, 2);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertTrue(result.getReason().contains("Magic-only"));
        assertTrue(result.getReason().contains("Abyssal whip"));
    }

    @Test
    public void highWeightLowValueTaskBlocksOnlyWithPointAndSlotEvidence()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 500, 21, 300, 6, 5);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.BLOCK, result.getDecision());
        assertTrue(result.getGuidance().getNote().contains("weight 10"));
        assertTrue(result.getGuidance().getSupplies().contains("100 Slayer points"));
    }

    @Test
    public void fullBlockListFallsBackToSkipInsteadOfClaimingCapacity()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 500, 21, 300, 6, 6);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.SKIP, result.getDecision());
        assertFalse(result.getGuidance().getNote().toLowerCase()
                .contains("free slot"));
    }

    @Test
    public void pointMilestonePreservesOtherwiseWeakTask()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 500, 9, 300, 6, 5);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.DO, result.getDecision());
        assertTrue(result.getReason().contains("milestone"));
    }

    @Test
    public void relaxedAfkIntentCanKeepLowAttentionTaskByProperties()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 500, 21, 300, 6, 5);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.RELAXED, SessionIntent.AFK,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.DO, result.getDecision());
    }

    @Test
    public void bossAlternativeRequiresVerifiedPvmReadiness()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 500, 21, 300, 6, 5);
        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put("pvm:cerberus", new PvmReadiness("pvm:cerberus",
                true, RecommendationConfidence.VERIFIED,
                Collections.emptyList()));
        SlayerDecisionResult result = strategist.assess(context(1, task,
                StrategyMode.BALANCED, SessionIntent.LONG_SESSION,
                GoalType.GEAR_TARGET, false, Collections.emptyList(),
                new PvmSnapshot(readiness)));

        assertEquals(SlayerTaskDecision.ALTERNATIVE, result.getDecision());
        assertEquals("Cerberus", result.getSelectedAlternativeName());
        assertTrue(result.getGuidance().getAction().contains("Cerberus"));

        readiness.put("pvm:cerberus", new PvmReadiness("pvm:cerberus",
                false, RecommendationConfidence.CHECK_NEEDED,
                Arrays.asList("Prayer", "gear")));
        result = strategist.assess(context(1, task,
                StrategyMode.BALANCED, SessionIntent.LONG_SESSION,
                GoalType.GEAR_TARGET, false, Collections.emptyList(),
                new PvmSnapshot(readiness)));
        assertFalse(result.getDecision() == SlayerTaskDecision.ALTERNATIVE);
    }

    @Test
    public void hardcoreWildernessAssignmentIsReplacedBeforeLoadoutSelection()
    {
        SlayerSnapshot task = snapshot("Greater demons", 100, "Krystilia",
                "Wilderness Slayer Cave", 500, 21, 300, 6, 1);
        SlayerDecisionResult result = strategist.assess(context(3, task,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, true, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.ALTERNATIVE, result.getDecision());
        assertNull(result.getSelectedAlternativeName());
        assertTrue(result.getGuidance().getAction().contains("Do not enter"));
        assertTrue(result.getGuidance().getAction().contains("30 Slayer points"));
        StrategyCandidate candidate = new SlayerCandidateProvider()
                .candidates(context(3, task, StrategyMode.EFFICIENT,
                        SessionIntent.LONG_SESSION, GoalType.SLAYER_85,
                        true, Collections.emptyList(), null)).get(0);
        assertEquals("Replace the risky Slayer task", candidate.getTitle());
    }

    @Test
    public void lowPointBalanceDoesNotSpendTheLastSkipOnOrdinaryDislike()
    {
        SlayerSnapshot task = snapshot("Hellhounds", 180, "Duradel",
                null, 30, 21, 300, 6, 6);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.DO, result.getDecision());
    }

    @Test
    public void candidateProviderProducesExecutableQueueAction()
    {
        SlayerSnapshot task = snapshot("Dust devils", 140, "Duradel",
                null, 500, 21, 300, 6, 2);
        StrategyContext context = context(0, task, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION, GoalType.SLAYER_85, false,
                Collections.singletonList(new ItemStackSnapshot(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())), null);
        StrategyCandidate candidate = new SlayerCandidateProvider()
                .candidates(context).get(0);

        assertEquals("slayer:do-task", candidate.getId());
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(candidate.toRecommendation()));
    }

    @Test
    public void f2pAndRestrictedBuildsDoNotProduceSlayerCandidates()
    {
        AccountSnapshot member = account(0);
        AccountSnapshot f2p = new AccountSnapshot(member.getPlayerName(),
                member.getAccountTypeCode(), member.getAccountTypeName(),
                MembershipStatus.F2P, 0, member.getTotalLevel(),
                member.getTotalExperience(), member.getSkillLevels(),
                member.getSkillExperience());
        StrategyDataBundle data = StrategyDataBundle.builder(f2p)
                .slayer(SlayerSnapshot.unknown()).build();
        StrategyContext context = new StrategyContext(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
        assertTrue(new SlayerCandidateProvider().candidates(context).isEmpty());
    }

    @Test
    public void pointRulesCoverInitialAndMilestoneEconomy()
    {
        assertEquals(0, SlayerPointEconomy.pointMultiplier(4));
        assertEquals(1, SlayerPointEconomy.pointMultiplier(5));
        assertEquals(5, SlayerPointEconomy.pointMultiplier(10));
        assertEquals(15, SlayerPointEconomy.pointMultiplier(50));
        assertEquals(50, SlayerPointEconomy.pointMultiplier(1000));
        assertEquals(6, SlayerPointEconomy.blockCapacity(300, false));
        assertEquals(7, SlayerPointEconomy.blockCapacity(300, true));
        assertFalse(SlayerPointEconomy.hasSustainableSkipBalance(30));
        assertTrue(SlayerPointEconomy.hasSustainableSkipBalance(60));
    }

    private static StrategyContext context(int typeCode, SlayerSnapshot slayer,
            StrategyMode mode, SessionIntent intent, GoalType goal,
            boolean wilderness, java.util.List<ItemStackSnapshot> items,
            PvmSnapshot pvm)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Shilo Village", QuestStatus.COMPLETE);
        quests.put("Lost City", QuestStatus.COMPLETE);
        quests.put("Priest in Peril", QuestStatus.COMPLETE);
        java.util.List<ItemStackSnapshot> equipment = new java.util.ArrayList<>();
        equipment.add(new ItemStackSnapshot(4151, "Abyssal whip", 1,
                net.runelite.api.EquipmentInventorySlot.WEAPON.getSlotIdx()));
        equipment.addAll(items);
        StrategyDataBundle data = StrategyDataBundle.builder(account(typeCode))
                .slayer(slayer)
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(equipment))
                .pvm(pvm)
                .build();
        return new StrategyContext(data, mode, intent, QuestTolerance.NORMAL,
                goal, false, false, wilderness, new PreferenceProfile());
    }

    private static SlayerSnapshot snapshot(String task, int remaining,
            String master, String location, int points, Integer streak,
            Integer questPoints, Integer capacity, Integer occupied)
    {
        return new SlayerSnapshot(task, remaining, master, location, points,
                streak, questPoints, capacity, occupied,
                RecommendationConfidence.VERIFIED);
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 99 : 95;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        return new AccountSnapshot("Slayer strategist", typeCode,
                AccountMode.fromTypeCode(typeCode).name(), MembershipStatus.P2P,
                1, total, totalXp, levels, xp);
    }
}
