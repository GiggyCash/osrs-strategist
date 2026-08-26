package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** End-to-end ranking simulations for the final three-card player queue. */
public class GlobalDecisionSimulationTest
{
    @Test
    public void unresolvedUpgradeCannotBuyPrimarySlotWithHugeScore()
    {
        StrategyContext context = context(account(0), GoalType.GEAR_TARGET,
                new PreferenceProfile(), StorageSnapshot.unknown());
        Recommendation skill = skill("skill:defence", Skill.DEFENCE,
                "Train Defence to 80", 40.0, 75, 80, 2);
        Recommendation unresolved = new Recommendation(
                "upgrade:expensive-item", "Buy upgrade", "Needs live price.",
                50_000.0, null, RecommendationConfidence.CHECK_NEEDED,
                0, 0, new RecommendationGuidance(
                        "Buy the item after verification.",
                        "Live price needs validation.",
                        "Grand Exchange.", "Not ready."));

        List<Recommendation> queue = engine().buildPlayerQueue(
                Arrays.asList(unresolved, skill), context);
        assertEquals("skill:defence", queue.get(0).getId());
    }

    @Test
    public void gearGoalLetsActionableUpgradeBeatGenericRawXp()
    {
        StrategyContext context = context(account(0), GoalType.GEAR_TARGET,
                new PreferenceProfile(), StorageSnapshot.unknown());
        Recommendation skill = skill("skill:mining", Skill.MINING,
                "Train Mining to 80", 63.0, 75, 80, 2);
        Recommendation upgrade = readyUpgrade(
                "upgrade:dragon-defender", "Get Dragon defender", 38.0,
                "Permanent melee off-hand upgrade.");

        List<Recommendation> queue = engine().buildPlayerQueue(
                Arrays.asList(skill, upgrade), context);
        assertEquals("upgrade:dragon-defender", queue.get(0).getId());
    }

    @Test
    public void recentTrainingPenaltyCanRotateCloseBalancedChoices()
    {
        PreferenceProfile preferences = new PreferenceProfile();
        preferences.addTemporaryScoreAdjustment(
                "skill:mining", -18.0, 60L * 60L * 1000L);
        StrategyContext context = context(account(0), GoalType.MAX,
                preferences, StorageSnapshot.unknown());

        // The real RecommendationEngine applies this temporary adjustment while
        // producing the skill candidate. The global intelligence layer must not
        // apply it again.
        double miningScore = 50.0
                + preferences.timedScoreAdjustmentFor("skill:mining");
        Recommendation mining = skill("skill:mining", Skill.MINING,
                "Train Mining to 80", miningScore, 70, 80, 2);
        Recommendation fishing = skill("skill:fishing", Skill.FISHING,
                "Train Fishing to 80", 45.0, 70, 80, 2);

        List<Recommendation> queue = engine().buildPlayerQueue(
                Arrays.asList(mining, fishing), context);
        assertEquals("skill:fishing", queue.get(0).getId());
    }

    @Test
    public void uimDeathStorageMakesDangerousGearHuntLoseToSustainableProgress()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(new ItemStackSnapshot(
                        100, "Stored gear", 1)));
        StorageSnapshot storage = new StorageSnapshot(states, contents);
        StrategyContext context = context(account(2), GoalType.GEAR_TARGET,
                new PreferenceProfile(), storage);

        Recommendation fishing = skill("skill:fishing", Skill.FISHING,
                "Train Fishing to 80", 38.0, 70, 80, 2);
        Recommendation dangerous = readyUpgrade(
                "upgrade:bowfa", "Hunt Bowfa seed", 48.0,
                "Run the Corrupted Gauntlet; a dangerous death can threaten UIM death storage.");

        List<Recommendation> queue = engine().buildPlayerQueue(
                Arrays.asList(dangerous, fishing), context);
        assertEquals("skill:fishing", queue.get(0).getId());
    }

    @Test
    public void blockedAndNeedsInfoNeverPopulateEmptyPrimary()
    {
        StrategyContext context = context(account(0), GoalType.MAX,
                new PreferenceProfile(), StorageSnapshot.unknown());
        Recommendation blocked = new Recommendation(
                "quest:blocked", "Blocked quest", "Blocked.", 999.0,
                null, RecommendationConfidence.BLOCKED, 0, 0, null);
        Recommendation unknown = new Recommendation(
                "quest:unknown", "Unknown quest", "Needs info.", 998.0,
                null, RecommendationConfidence.CHECK_NEEDED, 0, 0, null);

        assertTrue(engine().buildPlayerQueue(
                Arrays.asList(blocked, unknown), context).isEmpty());
    }

    @Test
    public void f2pSimulationContainsNoMembersUpgradeCandidate()
    {
        AccountSnapshot f2p = account(0, MembershipStatus.F2P);
        StrategyDataBundle data = StrategyDataBundle.builder(f2p)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
        StrategyContext context = new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.GEAR_TARGET,
                true, false, false, new PreferenceProfile());

        List<StrategyCandidate> upgrades =
                new ProgressionUpgradeCandidateProvider().candidates(context);
        for (StrategyCandidate candidate : upgrades)
        {
            assertFalse(candidate.getId().startsWith("upgrade:fighter-torso"));
            assertFalse(candidate.getId().startsWith("upgrade:dragon-defender"));
            assertFalse(candidate.getId().startsWith("upgrade:fire-cape"));
            assertFalse(candidate.getId().startsWith("upgrade:bowfa"));
            assertFalse(candidate.getId().startsWith("upgrade:barrows-gloves"));
        }
    }

    private static StrategyEngine engine()
    {
        return new StrategyEngine(
                null, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static Recommendation skill(
            String id, Skill skill, String title, double score,
            int current, int target, int setup)
    {
        TrainingMethod method = new TrainingMethod(
                id + ":method", skill, 1, 99,
                title, "Train safely.",
                10, 10, 10, AttentionLevel.LOW,
                20, setup, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method, "simulation", RecommendationConfidence.VERIFIED,
                Collections.emptyList());
        return new Recommendation(
                id, title, "Useful account progress.", score,
                plan, RecommendationConfidence.VERIFIED,
                current, target,
                new RecommendationGuidance(
                        "Do the verified training route.",
                        "Verified: setup is available.",
                        "Safe location.", "Safe route."));
    }

    private static Recommendation readyUpgrade(
            String id, String title, double score, String note)
    {
        return new Recommendation(
                id, title, note, score, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance(
                        "Complete the verified acquisition step.",
                        "Verified: required setup is available.",
                        "Safe verified location.", note));
    }

    private static StrategyContext context(
            AccountSnapshot account,
            GoalType goal,
            PreferenceProfile preferences,
            StorageSnapshot storage)
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .storage(storage)
                .build();
        return new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, goal,
                true, false, false, preferences);
    }

    private static AccountSnapshot account(int typeCode)
    {
        return account(typeCode, MembershipStatus.P2P);
    }

    private static AccountSnapshot account(
            int typeCode, MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 80 : 70;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        return new AccountSnapshot(
                "Simulation", typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                membership, membership == MembershipStatus.P2P ? 1 : 0,
                total, totalXp, levels, xp);
    }
}
