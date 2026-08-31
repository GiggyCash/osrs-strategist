package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Account-level simulation harness for beta safety.
 *
 * <p>These scenarios deliberately exercise the real selector and recommendation
 * engine rather than testing one catalog row at a time. They are not intended
 * to prove that every route is globally optimal. They prove the harder safety
 * invariants: membership never leaks, protected builds stay protected, account
 * modes do not receive impossible sourcing advice, and the global strategy
 * queue has access to more than the legacy top-three skill slice.</p>
 */
public class BetaAccountSimulationTest
{
    private final TrainingMethodSelector selector = new TrainingMethodSelector(
            new TrainingMethodDatabase(),
            null,
            new ExpandedTrainingMethodCatalog(),
            new F2pBaselineMethodCatalog(),
            new TrainingMethodPolicy());
    private final RecommendationEngine engine = new RecommendationEngine(selector);

    @Test
    public void f2pMainNeverReceivesMembersSkillOrMethod()
    {
        GameData data = data(account(
                MembershipStatus.F2P, 0, standardLevels(45)));

        List<Recommendation> recommendations = engine.recommendAll(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false,
                false,
                new PreferenceProfile());

        assertFalse(recommendations.isEmpty());
        for (Recommendation recommendation : recommendations)
        {
            TrainingMethod method = requireMethod(recommendation);
            assertTrue("F2P skill leak: " + method.getSkill(),
                    ContentAccessRules.isSkillAvailable(
                            method.getSkill(), MembershipStatus.F2P));
            assertTrue("F2P method leak: " + method.getId(),
                    ContentAccessRules.isMethodAvailable(
                            method, MembershipStatus.F2P));
            assertFalse("Members-only method leaked: " + method.getId(),
                    method.isMembersOnly());
        }
    }

    @Test
    public void unknownMembershipFailsClosedLikeF2p()
    {
        GameData data = data(account(
                MembershipStatus.UNKNOWN, 0, standardLevels(45)));
        List<Recommendation> recommendations = engine.recommendAll(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                false,
                false,
                new PreferenceProfile());

        for (Recommendation recommendation : recommendations)
        {
            TrainingMethod method = requireMethod(recommendation);
            assertTrue("Unknown access leaked a members skill: " + method.getSkill(),
                    ContentAccessRules.isSkillAvailable(
                            method.getSkill(), MembershipStatus.UNKNOWN));
            assertTrue("Unknown access leaked a members method: " + method.getId(),
                    ContentAccessRules.isMethodAvailable(
                            method, MembershipStatus.UNKNOWN));
        }
    }

    @Test
    public void p2pTransitionRestoresMembersSkillSelection()
    {
        Map<Skill, Integer> levels = standardLevels(60);
        GameData f2p = data(account(
                MembershipStatus.F2P, 0, levels));
        GameData p2p = data(account(
                MembershipStatus.P2P, 0, levels));

        assertNullPlan(selector.select(
                f2p, Skill.SLAYER, 60,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(
                f2p, Skill.SAILING, 60,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));

        assertNotNull(selector.select(
                p2p, Skill.SLAYER, 60,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNotNull(selector.select(
                p2p, Skill.SAILING, 60,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
    }

    @Test
    public void defencePureKeepsDefenceAndPrayerButNeverOffenceOrSlayer()
    {
        Map<Skill, Integer> levels = standardLevels(40);
        levels.put(Skill.ATTACK, 1);
        levels.put(Skill.STRENGTH, 1);
        levels.put(Skill.DEFENCE, 75);
        levels.put(Skill.RANGED, 1);
        levels.put(Skill.MAGIC, 1);
        levels.put(Skill.PRAYER, 43);
        levels.put(Skill.HITPOINTS, 63);

        AccountSnapshot account = account(MembershipStatus.F2P, 0, levels);
        assertEquals(RestrictedBuildType.DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(account));
        GameData data = data(account);

        assertNotNull(selector.select(data, Skill.DEFENCE, 75,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNotNull(selector.select(data, Skill.PRAYER, 43,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(data, Skill.ATTACK, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(data, Skill.STRENGTH, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(data, Skill.RANGED, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(data, Skill.MAGIC, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));
        assertNullPlan(selector.select(data, Skill.SLAYER, 1,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, false));

        for (Recommendation recommendation : engine.recommendAll(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                false, false, new PreferenceProfile()))
        {
            Skill skill = requireMethod(recommendation).getSkill();
            assertFalse(skill == Skill.ATTACK
                    || skill == Skill.STRENGTH
                    || skill == Skill.RANGED
                    || skill == Skill.MAGIC
                    || skill == Skill.SLAYER);
        }
    }

    @Test
    public void oneDefencePureNeverReceivesDefenceTraining()
    {
        Map<Skill, Integer> levels = standardLevels(40);
        levels.put(Skill.ATTACK, 60);
        levels.put(Skill.STRENGTH, 70);
        levels.put(Skill.DEFENCE, 1);
        levels.put(Skill.RANGED, 70);
        levels.put(Skill.MAGIC, 70);
        levels.put(Skill.HITPOINTS, 75);

        AccountSnapshot account = account(MembershipStatus.P2P, 0, levels);
        assertEquals(RestrictedBuildType.ONE_DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(account));
        GameData data = data(account);

        assertNullPlan(selector.select(data, Skill.DEFENCE, 1,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION, false));
        assertNotNull(selector.select(data, Skill.ATTACK, 60,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION, false));

        for (Recommendation recommendation : engine.recommendAll(
                data, StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION,
                false, false, new PreferenceProfile()))
        {
            assertFalse(requireMethod(recommendation).getSkill() == Skill.DEFENCE);
        }
    }

    @Test
    public void levelThreeSkillerNeverReceivesCombatProgression()
    {
        Map<Skill, Integer> levels = standardLevels(40);
        levels.put(Skill.ATTACK, 1);
        levels.put(Skill.STRENGTH, 1);
        levels.put(Skill.DEFENCE, 1);
        levels.put(Skill.RANGED, 1);
        levels.put(Skill.PRAYER, 1);
        levels.put(Skill.MAGIC, 1);
        levels.put(Skill.HITPOINTS, 10);
        levels.put(Skill.SLAYER, 1);

        AccountSnapshot account = account(MembershipStatus.P2P, 0, levels);
        assertEquals(RestrictedBuildType.SKILLER,
                AccountBuildPolicy.effectiveBuild(account));
        GameData data = data(account);

        for (Skill skill : new Skill[]{
                Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED,
                Skill.PRAYER, Skill.MAGIC, Skill.SLAYER})
        {
            assertNullPlan(selector.select(data, skill,
                    account.getSkillLevel(skill),
                    StrategyMode.RELAXED, SessionIntent.AFK, false));
        }
        assertNotNull(selector.select(data, Skill.MINING, 40,
                StrategyMode.RELAXED, SessionIntent.AFK, false));
    }

    @Test
    public void hardcoreCannotSelectWildernessEvenWhenGlobalToggleIsOn()
    {
        GameData data = data(account(
                MembershipStatus.P2P, 3, standardLevels(70)));

        TrainingPlan plan = selector.select(
                data,
                Skill.AGILITY,
                70,
                StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION,
                true);

        assertNotNull(plan);
        assertFalse(plan.getMethod().isWilderness());
    }

    @Test
    public void ironLikeGuidanceNeverTellsPlayerToUseGrandExchange()
    {
        int[] accountTypes = {1, 2, 3, 4, 5, 6};
        for (int accountType : accountTypes)
        {
            GameData data = data(account(
                    MembershipStatus.P2P,
                    accountType,
                    standardLevels(60)));
            List<Recommendation> recommendations = engine.recommendAll(
                    data,
                    StrategyMode.BALANCED,
                    SessionIntent.PICK_FOR_ME,
                    true,
                    false,
                    new PreferenceProfile());

            assertFalse("No recommendations for account type " + accountType,
                    recommendations.isEmpty());
            for (Recommendation recommendation : recommendations)
            {
                Guidance guidance = recommendation.getGuidance();
                if (guidance == null) continue;
                String text = safe(guidance.getAction()) + " "
                        + safe(guidance.getSupplies()) + " "
                        + safe(guidance.getLocation()) + " "
                        + safe(guidance.getNote());
                assertFalse("Iron-like GE leak for " + accountType + ": " + text,
                        text.contains("Grand Exchange"));
            }
        }
    }

    @Test
    public void recommendationEngineKeepsFullPoolForFinalActionabilityPass()
    {
        GameData data = data(account(
                MembershipStatus.P2P, 0, standardLevels(60)));

        List<Recommendation> full = engine.recommendAll(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                true,
                false,
                new PreferenceProfile());
        List<Recommendation> compact = engine.recommend(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                true,
                false,
                new PreferenceProfile());

        assertTrue("Expected the global pool to contain more than three skills",
                full.size() > 3);
        assertTrue(compact.size() <= 3);
        assertEquals(full.get(0).getId(), compact.get(0).getId());
    }

    @Test
    public void broadModeMatrixNeverReturnsBuildOrMembershipIllegalMethod()
    {
        int[] accountTypes = {0, 1, 2, 3, 4, 5, 6};
        StrategyMode[] modes = StrategyMode.values();
        SessionIntent[] sessions = {
                SessionIntent.PICK_FOR_ME,
                SessionIntent.AFK,
                SessionIntent.QUICK_20_MIN,
                SessionIntent.LONG_SESSION
        };

        for (int accountType : accountTypes)
        {
            AccountSnapshot account = account(
                    MembershipStatus.P2P,
                    accountType,
                    standardLevels(70));
            GameData data = data(account);
            for (StrategyMode mode : modes)
            {
                for (SessionIntent session : sessions)
                {
                    for (Skill skill : Skill.values())
                    {
                        if (skill == Skill.HITPOINTS) continue;
                        TrainingPlan plan = selector.select(
                                data,
                                skill,
                                account.getSkillLevel(skill),
                                mode,
                                session,
                                false);
                        if (plan == null) continue;
                        TrainingMethod method = plan.getMethod();
                        assertNotNull(method);
                        assertTrue("Build-illegal method " + method.getId(),
                                AccountBuildPolicy.allowsMethod(account, method));
                        assertTrue("Membership-illegal method " + method.getId(),
                                ContentAccessRules.isMethodAvailable(
                                        method, MembershipStatus.P2P));
                        assertFalse("Wilderness method leaked with risk disabled: "
                                        + method.getId(),
                                method.isWilderness());
                    }
                }
            }
        }
    }

    @Test
    public void strategyEngineRunsMajorAccountStageMatrixEndToEnd()
    {
        StrategyEngine strategyEngine = new StrategyEngine(engine, null, null,
                null, new ActionabilityPolicy(),
                new RecommendationIntelligenceService());
        MembershipStatus[] memberships = {
                MembershipStatus.F2P, MembershipStatus.UNKNOWN,
                MembershipStatus.P2P};
        int[] stages = {1, 35, 65, 90, 98, 99};
        for (MembershipStatus membership : memberships)
        {
            for (int accountType = 0; accountType <= 6; accountType++)
            {
                for (int stage : stages)
                {
                    GameData data = data(account(membership,
                            accountType, standardLevels(stage)));
                    for (StrategyMode mode : StrategyMode.values())
                    {
                        for (SessionIntent session : new SessionIntent[]{
                                SessionIntent.QUICK_20_MIN, SessionIntent.AFK,
                                SessionIntent.PICK_FOR_ME,
                                SessionIntent.LONG_SESSION})
                        {
                            StrategyResult result = strategyEngine.evaluate(data,
                                    mode, session, new PreferenceProfile());
                            assertFalse("DO NEXT missing for " + membership + "/"
                                    + accountType + "/" + stage + "/" + mode + "/"
                                    + session, result.getRecommendations().isEmpty());
                            for (Recommendation recommendation
                                    : result.getRecommendations())
                            {
                                assertFalse(recommendation.getConfidence()
                                        == Confidence.BLOCKED);
                                if (FallbackRecommendationFactory.isFallback(
                                        recommendation)) continue;
                                TrainingMethod method = requireMethod(recommendation);
                                assertTrue(ContentAccessRules.isMethodAvailable(
                                        method, membership));
                                assertTrue(AccountBuildPolicy.allowsMethod(
                                        data.account(), method));
                            }
                        }
                    }
                }
            }
        }
    }

    private static GameData data(AccountSnapshot account)
    {
        return GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
    }

    private static AccountSnapshot account(
            MembershipStatus membership,
            int typeCode,
            Map<Skill, Integer> levels)
    {
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = Math.max(1, levels.getOrDefault(skill, 1));
            int skillXp = level <= 1 ? 0 : Experience.getXpForLevel(level);
            xp.put(skill, skillXp);
            total += level;
            totalXp += skillXp;
        }
        return new AccountSnapshot(
                "Simulation",
                typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                total,
                totalXp,
                levels,
                xp);
    }

    private static Map<Skill, Integer> standardLevels(int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, level);
        // Hitpoints should never be below its real account baseline.
        levels.put(Skill.HITPOINTS, Math.max(10, level));
        return levels;
    }

    private static TrainingMethod requireMethod(Recommendation recommendation)
    {
        assertNotNull(recommendation);
        assertNotNull(recommendation.getTrainingPlan());
        assertNotNull(recommendation.getTrainingPlan().getMethod());
        return recommendation.getTrainingPlan().getMethod();
    }

    private static void assertNullPlan(TrainingPlan plan)
    {
        assertTrue("Expected no legal plan but got "
                        + (plan == null || plan.getMethod() == null
                        ? "unknown" : plan.getMethod().getId()),
                plan == null || plan.getMethod() == null);
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}
