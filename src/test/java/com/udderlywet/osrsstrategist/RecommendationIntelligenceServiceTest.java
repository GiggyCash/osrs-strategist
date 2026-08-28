package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RecommendationIntelligenceServiceTest
{
    private final RecommendationIntelligenceService service =
            new RecommendationIntelligenceService();

    @Test
    public void gearGoalCanBeatHigherRawSkillingScore()
    {
        StrategyContext context = context(
                account(MembershipStatus.P2P, 0, 80),
                GoalType.GEAR_TARGET,
                SessionIntent.PICK_FOR_ME);

        Recommendation skill = recommendation(
                "skill:mining", "Train Mining to 90", 60.0,
                Skill.MINING, 80, 90, 2, 30, AttentionLevel.MODERATE);
        Recommendation gear = new Recommendation(
                "upgrade:dragon-defender",
                "Get a Dragon defender",
                "Permanent melee upgrade.",
                40.0,
                null,
                RecommendationConfidence.VERIFIED,
                0,
                0,
                guidance("Enter the Warriors' Guild and obtain the defender."));

        gear = gear.withGoalProvenance(GoalDependencyProvenance.direct(
                GoalType.GEAR_TARGET, gear.getId(), java.util.Arrays.asList(
                        "Gear target", "Dragon defender")));
        assertTrue(service.rankScore(gear, context)
                > service.rankScore(skill, context));
    }

    @Test
    public void quickSessionPrefersLowSetupMethod()
    {
        StrategyContext context = context(
                account(MembershipStatus.P2P, 0, 70),
                GoalType.MAX,
                SessionIntent.QUICK_20_MIN);

        Recommendation quick = recommendation(
                "skill:fishing", "Train Fishing", 40.0,
                Skill.FISHING, 70, 80, 2, 15, AttentionLevel.LOW);
        Recommendation setupHeavy = recommendation(
                "skill:construction", "Train Construction", 45.0,
                Skill.CONSTRUCTION, 70, 80, 15, 60, AttentionLevel.ACTIVE);

        assertTrue(service.rankScore(quick, context)
                > service.rankScore(setupHeavy, context));
    }

    @Test
    public void uimStronglyPenalizesHighSetupTraining()
    {
        StrategyContext context = context(
                account(MembershipStatus.P2P, 2, 70),
                GoalType.MAX,
                SessionIntent.PICK_FOR_ME);

        Recommendation light = recommendation(
                "skill:fishing", "Train Fishing", 40.0,
                Skill.FISHING, 70, 80, 2, 20, AttentionLevel.LOW);
        Recommendation heavy = recommendation(
                "skill:construction", "Train Construction", 46.0,
                Skill.CONSTRUCTION, 70, 80, 15, 20, AttentionLevel.ACTIVE);

        assertTrue(service.rankScore(light, context)
                > service.rankScore(heavy, context));
    }

    @Test
    public void slayer85GoalDominatesGenericSkillProgress()
    {
        StrategyContext context = context(
                account(MembershipStatus.P2P, 1, 75),
                GoalType.SLAYER_85,
                SessionIntent.LONG_SESSION);

        Recommendation slayer = recommendation(
                "skill:slayer", "Train Slayer to 80", 35.0,
                Skill.SLAYER, 75, 80, 4, 60, AttentionLevel.MODERATE);
        Recommendation mining = recommendation(
                "skill:mining", "Train Mining to 80", 58.0,
                Skill.MINING, 75, 80, 1, 60, AttentionLevel.LOW);

        slayer = new GoalDependencyProvenanceService().attach(slayer, context);
        mining = new GoalDependencyProvenanceService().attach(mining, context);
        assertTrue(service.rankScore(slayer, context)
                > service.rankScore(mining, context));
    }

    @Test
    public void wildernessIsEffectivelyDisqualifiedWhenRiskDisabled()
    {
        StrategyContext context = context(
                account(MembershipStatus.P2P, 0, 85),
                GoalType.GEAR_TARGET,
                SessionIntent.LONG_SESSION);
        Recommendation wilderness = new Recommendation(
                "pvm:revenants",
                "Kill revenants",
                "Fast Wilderness money and drops.",
                999.0,
                null,
                RecommendationConfidence.VERIFIED,
                0,
                0,
                guidance("Enter the Wilderness revenant caves."));
        Recommendation safe = new Recommendation(
                "upgrade:fighter-torso",
                "Get a Fighter torso",
                "Safe account upgrade.",
                25.0,
                null,
                RecommendationConfidence.VERIFIED,
                0,
                0,
                guidance("Play Barbarian Assault."));

        assertTrue(service.rankScore(safe, context)
                > service.rankScore(wilderness, context));
    }

    private static Recommendation recommendation(
            String id,
            String title,
            double score,
            Skill skill,
            int current,
            int target,
            int setupMinutes,
            int minimumMinutes,
            AttentionLevel attention)
    {
        TrainingMethod method = new TrainingMethod(
                id + ":method",
                skill,
                1,
                99,
                title,
                "Use the method.",
                10,
                10,
                10,
                attention,
                minimumMinutes,
                setupMinutes,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED,
                true,
                false,
                false);
        return new Recommendation(
                id,
                title,
                "Account progression.",
                score,
                new TrainingPlan(method, "test",
                        RecommendationConfidence.VERIFIED,
                        Collections.emptyList()),
                RecommendationConfidence.VERIFIED,
                current,
                target,
                guidance("Do the selected training method."));
    }

    private static RecommendationGuidance guidance(String action)
    {
        return new RecommendationGuidance(
                action,
                "Verified: required setup is available.",
                "Safe reachable location.",
                "Account-safe route.");
    }

    private static StrategyContext context(
            AccountSnapshot account,
            GoalType goal,
            SessionIntent intent)
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
        return new StrategyContext(
                data,
                StrategyMode.BALANCED,
                intent,
                QuestTolerance.NORMAL,
                goal,
                true,
                false,
                false,
                new PreferenceProfile());
    }

    private static AccountSnapshot account(
            MembershipStatus membership,
            int typeCode,
            int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int effective = skill == Skill.HITPOINTS ? Math.max(10, level) : level;
            levels.put(skill, effective);
            int skillXp = effective <= 1 ? 0 : Experience.getXpForLevel(effective);
            xp.put(skill, skillXp);
            total += effective;
            totalXp += skillXp;
        }
        return new AccountSnapshot(
                "Intelligence Test",
                typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                total,
                totalXp,
                levels,
                xp);
    }
}
