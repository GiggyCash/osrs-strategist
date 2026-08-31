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
        assertEquals(Confidence.CHECK_NEEDED,
                result.getConfidence());
        assertTrue(result.getGuidance().getNote().contains("kept unknown"));
    }

    @Test
    public void mortimerLiveChoiceIsResolvedFromTaskAndModifierProperties()
    {
        SlayerSnapshot choice = new SlayerSnapshot(null, 0, "Mortimer", null,
                250, null, 300, 2, 0, null, Arrays.asList(
                        new SlayerTaskOffer("Dust devils", "Slayer XP", 20, false),
                        new SlayerTaskOffer("Hellhounds", "Quantity", 10, false)),
                true, Confidence.VERIFIED);
        StrategyContext context = context(0, choice, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION, GoalType.SLAYER_85, false,
                Collections.emptyList(), null);

        SlayerDecisionResult result = strategist.assess(context);

        assertEquals(SlayerAssignmentState.CHOICE_PENDING,
                result.getAssignmentState());
        assertEquals("Dust devils", result.getRecommendedOffer().getTaskName());
        assertTrue(result.getGuidance().getAction().contains("Slayer XP"));
        Recommendation candidate = new SlayerCandidateProvider()
                .candidates(context).get(0);
        assertEquals("slayer:choose-task", candidate.getId());
        assertEquals("Choose Dust devils from Mortimer", candidate.getTitle());
    }

    @Test
    public void incompleteMortimerChoiceFailsClosedInsteadOfHidingAnOption()
    {
        SlayerSnapshot choice = new SlayerSnapshot(null, 0, "Mortimer", null,
                250, null, 300, 2, 0, null, Arrays.asList(
                        new SlayerTaskOffer("Dust devils", "Slayer XP", 20, false),
                        new SlayerTaskOffer(null, null, 0, false)),
                true, Confidence.VERIFIED);

        SlayerDecisionResult result = strategist.assess(context(0, choice,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false, Collections.emptyList(), null));

        assertEquals(Confidence.CHECK_NEEDED,
                result.getConfidence());
        assertNull(result.getRecommendedOffer());
        assertTrue(result.getGuidance().getAction().contains("Keep Mortimer"));
    }

    @Test
    public void directBossTaskRequiresEncounterReadinessNotGenericSlayerGear()
    {
        SlayerSnapshot task = snapshot("Vorkath", 12, "Duradel", null,
                500, 21, 300, 6, 2);

        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.BALANCED, SessionIntent.LONG_SESSION,
                GoalType.GEAR_TARGET, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertTrue(result.getGuidance().getAction().contains("Vorkath"));
        assertTrue(result.getGuidance().getSupplies().contains("generic Slayer"));
    }

    @Test
    public void mortimerCancellationUsesItsHundredPointEconomy()
    {
        SlayerSnapshot lowPoints = snapshot("Hellhounds", 180, "Mortimer",
                null, 150, null, 300, 2, 2);
        SlayerDecisionResult keep = strategist.assess(context(0, lowPoints,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));
        assertEquals(SlayerTaskDecision.DO, keep.getDecision());

        SlayerSnapshot sustainable = snapshot("Hellhounds", 180, "Mortimer",
                null, 200, null, 300, 2, 2);
        SlayerDecisionResult skip = strategist.assess(context(0, sustainable,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));
        assertEquals(SlayerTaskDecision.SKIP, skip.getDecision());
        assertTrue(skip.getGuidance().getAction().contains("100 Slayer points"));
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
    public void currentSpecialMasterRulesRemainDistinct()
    {
        SlayerMasterCatalog catalog = new SlayerMasterCatalog();
        SlayerMasterProfile spria = catalog.byId("spria");
        SlayerMasterProfile mortimer = catalog.byId("mortimer");

        assertEquals(0, spria.getNormalPoints());
        assertEquals(40, spria.getBlockCost());
        assertEquals(100, mortimer.getCancelCost());
        assertEquals(120, mortimer.getBlockCost());
        StrategyContext eligible = context(0, snapshot(null, 0, null, null,
                0, 0, 300, 6, 0), StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, GoalType.AUTOMATIC, false,
                Collections.emptyList(), null);
        assertTrue(catalog.eligible(eligible).stream()
                .anyMatch(p -> "mortimer".equals(p.getId())));
        assertFalse(catalog.eligible(eligible).stream()
                .anyMatch(p -> "spria".equals(p.getId())));
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
                Confidence.VERIFIED);

        StrategyContext context = context(1, noTask, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION, GoalType.SLAYER_85, false,
                Collections.emptyList(), null);
        SlayerDecisionResult result = strategist.assess(context);

        assertEquals(SlayerReward.BIGGER_AND_BADDER,
                result.getRecommendedReward());
        assertTrue(result.getGuidance().getAction()
                .contains("Bigger and Badder"));
        assertTrue(result.getGuidance().getNote().contains("30-point"));
        Recommendation candidate = new SlayerCandidateProvider()
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
                Confidence.VERIFIED);

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
        assertEquals(Confidence.CHECK_NEEDED,
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
                Collections.singletonList(new ItemState(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())), null));

        assertEquals(SlayerTaskDecision.DO, result.getDecision());
        assertEquals(Confidence.VERIFIED,
                result.getConfidence());
        assertTrue(result.getGuidance().getAction().contains("remaining 140"));
        assertTrue(result.getGuidance().getAction().contains("Abyssal whip"));
        assertFalse(result.getGuidance().getAction().contains("Use any"));
        assertFalse(result.getGuidance().getAction().contains("Choose"));
        assertFalse(result.getGuidance().getAction().contains("owned gear"));
        assertTrue(result.getReason().contains("properties"));
    }

    @Test
    public void uselessNonemptyInventoryCannotVerifyDemandingTaskSupplies()
    {
        SlayerSnapshot task = snapshot("Dust devils", 140, "Duradel",
                null, 500, 21, 300, 6, 2);
        StrategyContext context = context(0, task, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION, GoalType.SLAYER_85, false,
                Collections.singletonList(new ItemState(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())),
                Collections.singletonList(new ItemState(
                        952, "Spade", 1)), null);

        SlayerDecisionResult result = strategist.assess(context);

        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertTrue(result.getReason().contains("no recognised healing"));
        assertFalse(result.getGuidance().getSupplies().contains(
                "Keep the currently carried inventory"));
    }

    @Test
    public void preparationUsesBestObservedFoodAndRejectsIngredientNames()
    {
        SlayerSnapshot task = snapshot("Dust devils", 140, "Duradel",
                null, 500, 21, 300, 6, 2);
        java.util.List<ItemState> bank = Arrays.asList(
                new ItemState(1887, "Cake tin", 1),
                new ItemState(331, "Raw salmon", 20),
                new ItemState(333, "Trout", 10),
                new ItemState(385, "Shark", 5));
        StrategyContext context = contextWithBank(0, task,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                GoalType.SLAYER_85, false,
                Collections.singletonList(new ItemState(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())),
                Collections.singletonList(new ItemState(
                        952, "Spade", 1)), bank, null);

        SlayerDecisionResult result = strategist.assess(context);

        assertEquals(SlayerTaskDecision.PREP_FIRST, result.getDecision());
        assertTrue(result.getGuidance().getAction().contains("withdraw Shark"));
        assertFalse(result.getGuidance().getAction().contains("Cake tin"));
        assertFalse(result.getGuidance().getAction().contains("Raw salmon"));
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
                true, Confidence.VERIFIED,
                Collections.emptyList()));
        SlayerDecisionResult result = strategist.assess(context(1, task,
                StrategyMode.BALANCED, SessionIntent.LONG_SESSION,
                GoalType.GEAR_TARGET, false, Collections.emptyList(),
                new PvmSnapshot(readiness)));

        assertEquals(SlayerTaskDecision.ALTERNATIVE, result.getDecision());
        assertEquals("Cerberus", result.getSelectedAlternativeName());
        assertTrue(result.getGuidance().getAction().contains("Cerberus"));

        readiness.put("pvm:cerberus", new PvmReadiness("pvm:cerberus",
                false, Confidence.CHECK_NEEDED,
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
        Recommendation candidate = new SlayerCandidateProvider()
                .candidates(context(3, task, StrategyMode.EFFICIENT,
                        SessionIntent.LONG_SESSION, GoalType.SLAYER_85,
                        true, Collections.emptyList(), null)).get(0);
        assertEquals("Replace the risky Slayer task", candidate.getTitle());
    }

    @Test
    public void intrinsicallyWildernessTaskIsRejectedWithoutLocationText()
    {
        SlayerSnapshot task = snapshot("Green dragons", 60, "Vannaka",
                null, 500, 21, 300, 6, 1);
        SlayerDecisionResult result = strategist.assess(context(0, task,
                StrategyMode.BALANCED, SessionIntent.LONG_SESSION,
                GoalType.AUTOMATIC, false, Collections.emptyList(), null));

        assertEquals(SlayerTaskDecision.ALTERNATIVE, result.getDecision());
        assertTrue(result.getGuidance().getAction().contains("Do not enter"));
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
                Collections.singletonList(new ItemState(
                        4164, "Facemask", 1,
                        net.runelite.api.EquipmentInventorySlot.HEAD.getSlotIdx())), null);
        Recommendation candidate = new SlayerCandidateProvider()
                .candidates(context).get(0);

        assertEquals("slayer:do-task", candidate.getId());
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(candidate));
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
        GameData data = GameData.builder(f2p)
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
        assertFalse(SlayerPointEconomy.hasSustainableSkipBalance(150, 100));
        assertTrue(SlayerPointEconomy.hasSustainableSkipBalance(200, 100));
    }

    private static StrategyContext context(int typeCode, SlayerSnapshot slayer,
            StrategyMode mode, SessionIntent intent, GoalType goal,
            boolean wilderness, java.util.List<ItemState> items,
            PvmSnapshot pvm)
    {
        return context(typeCode, slayer, mode, intent, goal, wilderness,
                items, Arrays.asList(
                        new ItemState(385, "Shark", 1),
                        new ItemState(385, "Shark", 1),
                        new ItemState(385, "Shark", 1)), pvm);
    }

    private static StrategyContext context(int typeCode, SlayerSnapshot slayer,
            StrategyMode mode, SessionIntent intent, GoalType goal,
            boolean wilderness, java.util.List<ItemState> items,
            java.util.List<ItemState> inventoryItems,
            PvmSnapshot pvm)
    {
        return contextWithBank(typeCode, slayer, mode, intent, goal,
                wilderness, items, inventoryItems, Collections.emptyList(), pvm);
    }

    private static StrategyContext contextWithBank(int typeCode,
            SlayerSnapshot slayer, StrategyMode mode, SessionIntent intent,
            GoalType goal, boolean wilderness,
            java.util.List<ItemState> items,
            java.util.List<ItemState> inventoryItems,
            java.util.List<ItemState> bankItems,
            PvmSnapshot pvm)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Shilo Village", QuestStatus.COMPLETE);
        quests.put("Lost City", QuestStatus.COMPLETE);
        quests.put("Priest in Peril", QuestStatus.COMPLETE);
        quests.put("Fallen From Grace", QuestStatus.IN_PROGRESS);
        java.util.List<ItemState> equipment = new java.util.ArrayList<>();
        equipment.add(new ItemState(4151, "Abyssal whip", 1,
                net.runelite.api.EquipmentInventorySlot.WEAPON.getSlotIdx()));
        equipment.addAll(items);
        GameData data = GameData.builder(account(typeCode))
                .slayer(slayer)
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(bankItems, 1L))
                .inventory(new ItemsState(inventoryItems))
                .equipment(new ItemsState(equipment))
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
                Confidence.VERIFIED);
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
