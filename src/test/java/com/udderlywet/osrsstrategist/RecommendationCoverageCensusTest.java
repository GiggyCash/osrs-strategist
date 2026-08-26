package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Executable-coverage census through the same boundary used by DO NEXT.
 * Catalog presence alone is insufficient: guidance and unresolved requirements
 * can still make a selected route ineligible for the player-facing queue.
 */
public class RecommendationCoverageCensusTest
{
    private static final int[] LEVELS = {5, 50, 85};
    private static final StrategyMode[] MODES = StrategyMode.values();
    private static final SessionIntent[] SESSIONS = {
            SessionIntent.QUICK_20_MIN,
            SessionIntent.ONE_HOUR,
            SessionIntent.LONG_SESSION,
            SessionIntent.AFK
    };
    private static final Scenario[] ACCOUNTS = {
            new Scenario("F2P Main", MembershipStatus.F2P, 0),
            new Scenario("P2P Main", MembershipStatus.P2P, 0),
            new Scenario("Ironman", MembershipStatus.P2P, 1),
            new Scenario("GIM", MembershipStatus.P2P, 4),
            new Scenario("UIM", MembershipStatus.P2P, 2),
            new Scenario("HCIM", MembershipStatus.P2P, 3)
    };
    private static final EnumSet<Skill> INDIRECT_ONLY =
            EnumSet.of(Skill.HITPOINTS);

    @Test
    public void representativeStatesAlwaysHaveASafeExecutableRecovery()
    {
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        for (Scenario account : ACCOUNTS)
        {
            StrategyEngine engine = strategyEngine(account.membership);
            for (int level : LEVELS)
            {
                for (StrategyMode mode : MODES)
                {
                    for (SessionIntent session : SESSIONS)
                    {
                        StrategyResult result = engine.evaluate(
                                data(account, level), mode, session,
                                new PreferenceProfile());
                        assertFalse(label(account, level, mode, session),
                                result.getRecommendations().isEmpty());
                        assertTrue(label(account, level, mode, session),
                                policy.canLeadQueue(
                                        result.getRecommendations().get(0)));
                    }
                }
            }
        }
    }

    @Test
    public void censusReportsExecutableSkillCoverage()
    {
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        Map<Skill, Integer> eligible = new EnumMap<>(Skill.class);
        Map<Skill, Integer> executable = new EnumMap<>(Skill.class);
        Map<String, Integer> stateEligible = new LinkedHashMap<>();
        Map<String, Integer> stateExecutable = new LinkedHashMap<>();

        for (Scenario account : ACCOUNTS)
        {
            RecommendationEngine engine = recommendationEngine(
                    account.membership);
            for (int level : LEVELS)
            {
                StrategyDataBundle data = data(account, level);
                String state = account.name + " " + progressionBand(level);
                for (StrategyMode mode : MODES)
                {
                    for (SessionIntent session : SESSIONS)
                    {
                        for (Skill skill : Skill.values())
                        {
                            if (INDIRECT_ONLY.contains(skill)
                                    || level >= 99
                                    || !ContentAccessRules.isSkillAvailable(
                                            skill, account.membership))
                            {
                                continue;
                            }
                            eligible.put(skill,
                                    eligible.getOrDefault(skill, 0) + 1);
                            stateEligible.put(state,
                                    stateEligible.getOrDefault(state, 0) + 1);
                        }
                        for (Recommendation recommendation : engine.recommendAll(
                                data, mode, session, true, false,
                                new PreferenceProfile()))
                        {
                            if (recommendation.getTrainingPlan() == null
                                    || recommendation.getTrainingPlan()
                                            .getMethod() == null)
                            {
                                continue;
                            }
                            Skill skill = recommendation.getTrainingPlan()
                                    .getMethod().getSkill();
                            if (policy.canLeadQueue(recommendation))
                            {
                                executable.put(skill,
                                        executable.getOrDefault(skill, 0) + 1);
                                stateExecutable.put(state,
                                        stateExecutable.getOrDefault(state, 0) + 1);
                            }
                        }
                    }
                }
            }
        }

        for (Skill skill : Skill.values())
        {
            if (INDIRECT_ONLY.contains(skill)) continue;
            int possible = eligible.getOrDefault(skill, 0);
            int ready = executable.getOrDefault(skill, 0);
            if (possible > 0)
            {
                System.out.println("COVERAGE " + skill.getName() + " "
                        + ready + "/" + possible);
            }
        }

        for (Map.Entry<String, Integer> entry : stateEligible.entrySet())
        {
            System.out.println("COVERAGE_STATE " + entry.getKey() + " "
                    + stateExecutable.getOrDefault(entry.getKey(), 0) + "/"
                    + entry.getValue());
        }

        // These are executable-data regression floors, not an excuse to expose
        // vague routes. Known access-dependent weak areas remain visible in the
        // report instead of being papered over with permissive assertions.
        assertTrue(executable.getOrDefault(Skill.MINING, 0) > 0);
        assertTrue(executable.getOrDefault(Skill.WOODCUTTING, 0) > 0);
        assertTrue(executable.getOrDefault(Skill.RUNECRAFT, 0) > 0);
        assertTrue(executable.getOrDefault(Skill.THIEVING, 0).intValue()
                == eligible.getOrDefault(Skill.THIEVING, 0).intValue());
        assertTrue(executable.getOrDefault(Skill.SLAYER, 0).intValue()
                == eligible.getOrDefault(Skill.SLAYER, 0).intValue());
    }

    @Test
    public void preparedAccountCensusSeparatesCatalogGapsFromToolShortfalls()
    {
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        Map<Skill, Integer> eligible = new EnumMap<>(Skill.class);
        Map<Skill, Integer> executable = new EnumMap<>(Skill.class);
        for (Scenario account : ACCOUNTS)
        {
            RecommendationEngine engine = recommendationEngine(
                    account.membership);
            for (int level : LEVELS)
            {
                StrategyDataBundle data = data(account, level, true);
                for (StrategyMode mode : MODES)
                {
                    for (SessionIntent session : SESSIONS)
                    {
                        for (Skill skill : Skill.values())
                        {
                            if (INDIRECT_ONLY.contains(skill)
                                    || !ContentAccessRules.isSkillAvailable(
                                            skill, account.membership)) continue;
                            eligible.put(skill,
                                    eligible.getOrDefault(skill, 0) + 1);
                        }
                        for (Recommendation recommendation : engine.recommendAll(
                                data, mode, session, true, false,
                                new PreferenceProfile()))
                        {
                            if (!policy.canLeadQueue(recommendation)
                                    || recommendation.getTrainingPlan() == null
                                    || recommendation.getTrainingPlan()
                                            .getMethod() == null) continue;
                            Skill skill = recommendation.getTrainingPlan()
                                    .getMethod().getSkill();
                            executable.put(skill,
                                    executable.getOrDefault(skill, 0) + 1);
                        }
                    }
                }
            }
        }

        for (Skill skill : Skill.values())
        {
            int possible = eligible.getOrDefault(skill, 0);
            if (possible == 0) continue;
            System.out.println("COVERAGE_PREPARED " + skill.getName() + " "
                    + executable.getOrDefault(skill, 0) + "/" + possible);
        }
        assertTrue(executable.getOrDefault(Skill.RANGED, 0) > 0);
        assertTrue(executable.getOrDefault(Skill.RUNECRAFT, 0) > 0);
    }

    @Test
    public void requestedSkillCensusDoesNotCountUnrelatedRecoveryAsCoverage()
    {
        Map<Skill, EnumMap<CoverageClass, Integer>> observed =
                classifiedCensus(false);
        Map<Skill, EnumMap<CoverageClass, Integer>> prepared =
                classifiedCensus(true);

        printClassifiedCensus("REQUESTED", observed);
        printClassifiedCensus("REQUESTED_PREPARED", prepared);

        // A safe fallback remains useful, but it is not requested-skill
        // coverage. These guards make that distinction durable.
        assertTrue(count(observed, Skill.MINING, CoverageClass.PREREQUISITE) > 0);
        assertTrue(count(prepared, Skill.MINING, CoverageClass.DIRECT) > 0);
        assertTrue(count(observed, Skill.RUNECRAFT, CoverageClass.PREREQUISITE) > 0);
        assertTrue(count(prepared, Skill.RUNECRAFT, CoverageClass.DIRECT) > 0);
        for (Skill skill : EnumSet.of(
                Skill.COOKING, Skill.MAGIC, Skill.FISHING, Skill.RUNECRAFT,
                Skill.FARMING, Skill.HUNTER, Skill.CONSTRUCTION))
        {
            assertEquals(skill.getName() + " prepared recovery debt",
                    0, count(prepared, skill, CoverageClass.RECOVERY));
        }
        for (Skill skill : EnumSet.of(
                Skill.COOKING, Skill.MAGIC, Skill.FISHING, Skill.RUNECRAFT,
                Skill.FARMING, Skill.HUNTER))
        {
            assertEquals(skill.getName() + " observed-empty recovery debt",
                    0, count(observed, skill, CoverageClass.RECOVERY));
        }
    }

    @Test
    public void classificationHasAnHonestBlockedState()
    {
        assertEquals(CoverageClass.BLOCKED,
                classify(Skill.COOKING, null, null,
                        new RecommendationActionabilityPolicy()));
        Recommendation recovery = FallbackRecommendationFactory.forState(
                data(ACCOUNTS[0], 5));
        assertEquals(CoverageClass.RECOVERY,
                classify(Skill.COOKING, null, recovery,
                        new RecommendationActionabilityPolicy()));
    }

    @Test
    public void classifiedStateCensusShowsMidgameUimDensity()
    {
        for (boolean prepared : new boolean[]{false, true})
        {
            for (Scenario account : ACCOUNTS)
            {
                RecommendationEngine engine = recommendationEngine(
                        account.membership);
                StrategyEngine strategy = strategyEngine(account.membership);
                for (int level : LEVELS)
                {
                    EnumMap<CoverageClass, Integer> counts =
                            new EnumMap<>(CoverageClass.class);
                    for (StrategyMode mode : MODES)
                    {
                        for (SessionIntent session : SESSIONS)
                        {
                            StrategyDataBundle data = data(
                                    account, level, prepared);
                            List<Recommendation> candidates = engine.recommendAll(
                                    data, mode, session, true, false,
                                    new PreferenceProfile());
                            Recommendation recovery = strategy.evaluate(
                                    data, mode, session,
                                    new PreferenceProfile())
                                    .getRecommendations().get(0);
                            for (Skill skill : Skill.values())
                            {
                                if (INDIRECT_ONLY.contains(skill)
                                        || !ContentAccessRules.isSkillAvailable(
                                                skill, account.membership))
                                    continue;
                                CoverageClass result = classify(skill,
                                        candidateFor(candidates, skill),
                                        recovery,
                                        new RecommendationActionabilityPolicy());
                                counts.merge(result, 1, Integer::sum);
                            }
                        }
                    }
                    System.out.println("COVERAGE_CLASS_STATE "
                            + (prepared ? "prepared" : "observed-empty")
                            + " " + account.name + " "
                            + progressionBand(level)
                            + " DIRECT=" + counts.getOrDefault(
                                    CoverageClass.DIRECT, 0)
                            + " PREREQUISITE=" + counts.getOrDefault(
                                    CoverageClass.PREREQUISITE, 0)
                            + " RECOVERY=" + counts.getOrDefault(
                                    CoverageClass.RECOVERY, 0)
                            + " BLOCKED=" + counts.getOrDefault(
                                    CoverageClass.BLOCKED, 0));
                }
            }
        }
    }

    @Test
    public void priorityCoverageDebtRemainsVisibleBySelectedMethod()
    {
        EnumSet<Skill> priority = EnumSet.of(
                Skill.COOKING, Skill.MAGIC, Skill.FISHING, Skill.RUNECRAFT,
                Skill.CONSTRUCTION, Skill.FARMING, Skill.HUNTER,
                Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE);
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        for (Scenario account : ACCOUNTS)
        {
            RecommendationEngine engine = recommendationEngine(account.membership);
            for (int level : LEVELS)
            {
                List<Recommendation> candidates = engine.recommendAll(
                        data(account, level), StrategyMode.BALANCED,
                        SessionIntent.ONE_HOUR, true, false,
                        new PreferenceProfile());
                for (Skill skill : priority)
                {
                    if (!ContentAccessRules.isSkillAvailable(
                            skill, account.membership)) continue;
                    Recommendation candidate = candidateFor(candidates, skill);
                    if (candidate == null || !policy.canLeadQueue(candidate))
                    {
                        String method = candidate == null
                                || candidate.getTrainingPlan() == null
                                || candidate.getTrainingPlan().getMethod() == null
                                ? "none"
                                : candidate.getTrainingPlan().getMethod().getId();
                        System.out.println("COVERAGE_DEBT " + account.name
                                + " " + progressionBand(level) + " "
                                + skill.getName() + " method=" + method
                                + " confidence=" + (candidate == null ? "none"
                                        : candidate.getConfidence())
                                + " action=" + (candidate == null
                                        || candidate.getGuidance() == null
                                        ? "none" : candidate.getGuidance().getAction())
                                + " location=" + (candidate == null
                                        || candidate.getGuidance() == null
                                        ? "none" : candidate.getGuidance().getLocation()));
                    }
                }
            }
        }
    }

    @Test
    public void repeatedRequestedSkillRecoveryIsReportedAsCoverageDebt()
    {
        Map<String, Integer> debt = new java.util.TreeMap<>();
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        for (boolean prepared : new boolean[]{false, true})
        {
            for (Scenario account : ACCOUNTS)
            {
                RecommendationEngine engine = recommendationEngine(
                        account.membership);
                StrategyEngine strategy = new StrategyEngine(
                        engine, null, null, null, policy,
                        new RecommendationIntelligenceService());
                for (int level : LEVELS)
                {
                    StrategyDataBundle data = data(account, level, prepared);
                    for (StrategyMode mode : MODES)
                    {
                        for (SessionIntent session : SESSIONS)
                        {
                            List<Recommendation> candidates = engine.recommendAll(
                                    data, mode, session, true, false,
                                    new PreferenceProfile());
                            Recommendation recovery = strategy.evaluate(
                                    data, mode, session,
                                    new PreferenceProfile())
                                    .getRecommendations().get(0);
                            for (Skill skill : Skill.values())
                            {
                                if (INDIRECT_ONLY.contains(skill)
                                        || !ContentAccessRules.isSkillAvailable(
                                                skill, account.membership))
                                    continue;
                                Recommendation candidate = candidateFor(
                                        candidates, skill);
                                if (classify(skill, candidate, recovery, policy)
                                        != CoverageClass.RECOVERY) continue;
                                String method = candidate == null
                                        || candidate.getTrainingPlan() == null
                                        || candidate.getTrainingPlan().getMethod() == null
                                        ? "none"
                                        : candidate.getTrainingPlan().getMethod().getId();
                                String key = (prepared ? "prepared" : "observed-empty")
                                        + " " + skill.getName() + " " + method;
                                debt.merge(key, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Integer> entry : debt.entrySet())
            System.out.println("COVERAGE_DEBT_SUMMARY " + entry.getKey()
                    + "=" + entry.getValue());

        // A prepared, universally legal baseline must not repeatedly recover
        // into an unrelated skill merely because a generic route scored first.
        assertTrue(debt.getOrDefault("prepared Runecraft none", 0) == 0);
        assertTrue(debt.getOrDefault("prepared Hunter none", 0) == 0);
    }

    @Test
    public void concreteBaselinesRespectTheirProgressionBands()
    {
        Scenario p2p = ACCOUNTS[1];
        RecommendationEngine engine = recommendationEngine(p2p.membership);

        assertEquals("magic_f2p_combat", selectedMethod(engine, p2p, 5,
                Skill.MAGIC));
        assertEquals("magic_f2p_fire_bolt", selectedMethod(engine, p2p, 50,
                Skill.MAGIC));
        assertEquals("magic_f2p_fire_blast", selectedMethod(engine, p2p, 85,
                Skill.MAGIC));
        assertEquals("farming_falador_potatoes", selectedMethod(
                engine, p2p, 5, Skill.FARMING));
        assertEquals("farming_falador_watermelons", selectedMethod(
                engine, p2p, 50, Skill.FARMING));
        assertEquals("farming_falador_watermelons", selectedMethod(
                engine, p2p, 85, Skill.FARMING));
        assertEquals("construction_crude_chairs", selectedMethod(
                engine, p2p, 5, Skill.CONSTRUCTION));
        assertEquals("construction_oak_larders", selectedMethod(
                engine, p2p, 50, Skill.CONSTRUCTION));
        assertFalse("construction_crude_chairs".equals(selectedMethod(
                engine, p2p, 85, Skill.CONSTRUCTION)));
    }

    private static String selectedMethod(RecommendationEngine engine,
            Scenario scenario, int level, Skill skill)
    {
        Recommendation candidate = candidateFor(engine.recommendAll(
                data(scenario, level, true), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, true, false,
                new PreferenceProfile()), skill);
        assertTrue(skill.getName() + " candidate missing", candidate != null);
        return candidate.getTrainingPlan().getMethod().getId();
    }

    private static Map<Skill, EnumMap<CoverageClass, Integer>> classifiedCensus(
            boolean prepared)
    {
        Map<Skill, EnumMap<CoverageClass, Integer>> counts =
                new EnumMap<>(Skill.class);
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        for (Scenario account : ACCOUNTS)
        {
            RecommendationEngine recommendationEngine = recommendationEngine(
                    account.membership);
            StrategyEngine strategyEngine = new StrategyEngine(
                    recommendationEngine, null, null, null, policy,
                    new RecommendationIntelligenceService());
            for (int level : LEVELS)
            {
                StrategyDataBundle data = data(account, level, prepared);
                for (StrategyMode mode : MODES)
                {
                    for (SessionIntent session : SESSIONS)
                    {
                        List<Recommendation> skillCandidates =
                                recommendationEngine.recommendAll(
                                        data, mode, session, true, false,
                                        new PreferenceProfile());
                        StrategyResult global = strategyEngine.evaluate(
                                data, mode, session, new PreferenceProfile());
                        Recommendation recovery = global.getRecommendations().isEmpty()
                                ? null : global.getRecommendations().get(0);
                        for (Skill skill : Skill.values())
                        {
                            if (INDIRECT_ONLY.contains(skill)
                                    || !ContentAccessRules.isSkillAvailable(
                                            skill, account.membership))
                            {
                                continue;
                            }
                            Recommendation candidate = candidateFor(
                                    skillCandidates, skill);
                            CoverageClass result = classify(
                                    skill, candidate, recovery, policy);
                            counts.computeIfAbsent(skill,
                                    ignored -> new EnumMap<>(CoverageClass.class))
                                    .merge(result, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return counts;
    }

    private static Recommendation candidateFor(
            List<Recommendation> candidates, Skill skill)
    {
        for (Recommendation candidate : candidates)
        {
            TrainingPlan plan = candidate.getTrainingPlan();
            if (plan != null && plan.getMethod() != null
                    && plan.getMethod().getSkill() == skill)
            {
                return candidate;
            }
        }
        return null;
    }

    private static CoverageClass classify(
            Skill requestedSkill,
            Recommendation candidate,
            Recommendation recovery,
            RecommendationActionabilityPolicy policy)
    {
        if (candidate != null && policy.canLeadQueue(candidate))
        {
            TrainingPlan plan = candidate.getTrainingPlan();
            if (candidate.getConfidence() == RecommendationConfidence.CHECK_NEEDED
                    || hasOutstandingPreparation(plan)
                    || isAcquisitionGuidance(candidate))
            {
                return CoverageClass.PREREQUISITE;
            }
            return CoverageClass.DIRECT;
        }

        if (recovery != null && policy.canLeadQueue(recovery))
        {
            String id = recovery.getId() == null ? "" : recovery.getId();
            if (requestedSkill == Skill.MINING
                    && id.equals("fallback:starter-pickaxe"))
            {
                return CoverageClass.PREREQUISITE;
            }
            if (requestedSkill == Skill.MINING
                    && id.equals("fallback:starter-mining"))
            {
                return CoverageClass.DIRECT;
            }
            if (id.equals("fallback:safe-combat-"
                    + requestedSkill.name().toLowerCase(
                            java.util.Locale.ROOT)))
            {
                return CoverageClass.DIRECT;
            }
            return CoverageClass.RECOVERY;
        }
        return CoverageClass.BLOCKED;
    }

    private static boolean isAcquisitionGuidance(Recommendation recommendation)
    {
        RecommendationGuidance guidance = recommendation == null
                ? null : recommendation.getGuidance();
        String action = guidance == null || guidance.getAction() == null
                ? "" : guidance.getAction().toLowerCase(
                        java.util.Locale.ROOT);
        return action.startsWith("talk to ")
                || action.startsWith("buy ")
                || action.startsWith("get ")
                || action.startsWith("obtain ");
    }

    private static boolean hasOutstandingPreparation(TrainingPlan plan)
    {
        if (plan == null) return false;
        for (RequirementCheck check : plan.getRequirementChecks())
        {
            if (check.getState() == RequirementState.CHECK_NEEDED)
                return true;
        }
        return false;
    }

    private static int count(
            Map<Skill, EnumMap<CoverageClass, Integer>> census,
            Skill skill,
            CoverageClass classification)
    {
        EnumMap<CoverageClass, Integer> counts = census.get(skill);
        return counts == null ? 0 : counts.getOrDefault(classification, 0);
    }

    private static void printClassifiedCensus(
            String prefix,
            Map<Skill, EnumMap<CoverageClass, Integer>> census)
    {
        for (Skill skill : Skill.values())
        {
            if (!census.containsKey(skill)) continue;
            System.out.println(prefix + " " + skill.getName()
                    + " DIRECT=" + count(census, skill, CoverageClass.DIRECT)
                    + " PREREQUISITE=" + count(census, skill,
                            CoverageClass.PREREQUISITE)
                    + " RECOVERY=" + count(census, skill,
                            CoverageClass.RECOVERY)
                    + " BLOCKED=" + count(census, skill,
                            CoverageClass.BLOCKED));
        }
    }

    private enum CoverageClass
    {
        DIRECT,
        PREREQUISITE,
        RECOVERY,
        BLOCKED
    }

    private static String progressionBand(int level)
    {
        if (level < 20) return "early";
        if (level < 70) return "mid";
        return "late";
    }

    private static RecommendationEngine recommendationEngine(
            MembershipStatus membership)
    {
        RuneLiteSkillActionCatalog actions =
                new CensusSkillActionCatalog(membership);
        RecommendationGuidanceService guidance =
                new RecommendationGuidanceService(
                        new AdaptiveMilestoneGuidanceService(
                                actions,
                                new MethodExecutionProfileCatalog(),
                                new SkillingXpModifierService()),
                        new VariableMethodGuidanceService(),
                        new UniversalSkillActionGuidanceService(
                                actions,
                                new UniversalActionRecipeResolver(),
                                new SkillingXpModifierService(),
                                new AccountResourcePlanner()));
        return new RecommendationEngine(new TrainingMethodSelector(
                new TrainingMethodDatabase(),
                new RequirementEvidenceEngine(
                        new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                        new AgilityAccessEvaluator(new AgilityCourseCatalog())),
                new ExpandedTrainingMethodCatalog(),
                new F2pBaselineMethodCatalog(),
                new TrainingMethodPolicy()), guidance);
    }

    private static StrategyEngine strategyEngine(MembershipStatus membership)
    {
        return new StrategyEngine(recommendationEngine(membership), null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    /**
     * The no-ItemManager test adapter keeps RuneLite's maintained names, levels,
     * XP, and icons while supplying the already route-gated account membership.
     * Membership leakage is covered independently by the production policy
     * matrices; this census measures whether concrete rendering survives the
     * final quality boundary.
     */
    private static final class CensusSkillActionCatalog
            extends RuneLiteSkillActionCatalog
    {
        private final MembershipStatus membership;

        private CensusSkillActionCatalog(MembershipStatus membership)
        {
            this.membership = membership;
        }

        @Override
        public List<RuneLiteSkillActionDefinition> actionsFor(Skill skill)
        {
            List<RuneLiteSkillActionDefinition> result = new ArrayList<>();
            for (RuneLiteSkillActionDefinition action : super.actionsFor(skill))
            {
                result.add(new RuneLiteSkillActionDefinition(
                        action.getSkill(), action.getId(), action.getName(),
                        action.getLevel(), action.getXp(), action.getCategory(),
                        membership, action.getItemId()));
            }
            return result;
        }
    }

    private static StrategyDataBundle data(Scenario scenario, int level)
    {
        return data(scenario, level, false);
    }

    private static StrategyDataBundle data(Scenario scenario, int level,
            boolean prepared)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int totalLevel = 0;
        long totalXp = 0;
        for (Skill skill : Skill.values())
        {
            int value = skill == Skill.HITPOINTS ? Math.max(10, level) : level;
            int valueXp = value <= 1 ? 0 : Experience.getXpForLevel(value);
            levels.put(skill, value);
            xp.put(skill, valueXp);
            totalLevel += value;
            totalXp += valueXp;
        }
        AccountSnapshot account = new AccountSnapshot(
                scenario.name, 10_000L + scenario.type, scenario.type,
                AccountMode.fromTypeCode(scenario.type).name(),
                scenario.membership,
                scenario.membership == MembershipStatus.P2P ? 1 : 0,
                totalLevel, totalXp, levels, xp);
        List<ItemStackSnapshot> items = prepared
                ? preparedItems() : Collections.emptyList();
        StrategyDataBundle.Builder builder = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(items))
                .equipment(new EquipmentSnapshot(Collections.emptyList()));
        if (scenario.membership == MembershipStatus.P2P)
        {
            Map<String, CapabilityState> tools = new HashMap<>();
            if (prepared)
            {
                tools.put("rake", CapabilityState.VERIFIED);
                tools.put("dibber", CapabilityState.VERIFIED);
                tools.put("spade", CapabilityState.VERIFIED);
            }
            builder.farming(new FarmingSnapshot(
                    new HashSet<>(Collections.singletonList("falador")),
                    tools, Collections.emptyMap()));
            if (prepared)
            {
                Map<String, CapabilityState> poh = new HashMap<>();
                poh.put("room:parlour", CapabilityState.VERIFIED);
                poh.put("room:kitchen", CapabilityState.VERIFIED);
                builder.poh(new PohSnapshot(CapabilityState.VERIFIED, poh));
            }
        }
        if (scenario.type != 2)
        {
            builder.bank(new BankSnapshot(Collections.emptyList(), 1L));
        }
        return builder.build();
    }

    private static List<ItemStackSnapshot> preparedItems()
    {
        List<ItemStackSnapshot> items = new ArrayList<>();
        items.add(item(ItemID.BRONZE_PICKAXE, "Bronze pickaxe", 1));
        items.add(item(ItemID.BRONZE_AXE, "Bronze axe", 1));
        items.add(item(ItemID.BRONZE_SCIMITAR, "Bronze scimitar", 1));
        items.add(item(ItemID.SHORTBOW, "Shortbow", 1));
        items.add(item(ItemID.BRONZE_ARROW, "Bronze arrow", 10_000));
        items.add(item(ItemID.NET, "Small fishing net", 1));
        items.add(item(ItemID.FLY_FISHING_ROD, "Fly fishing rod", 1));
        items.add(item(ItemID.FEATHER, "Feather", 10_000));
        items.add(item(ItemID.TINDERBOX, "Tinderbox", 1));
        items.add(item(ItemID.HAMMER, "Hammer", 1));
        items.add(item(ItemID.KNIFE, "Knife", 1));
        items.add(item(ItemID.LOGS, "Logs", 10_000));
        items.add(item(ItemID.BONES, "Bones", 10_000));
        items.add(item(ItemID.BRONZE_BAR, "Bronze bar", 10_000));
        items.add(item(ItemID.IRON_BAR, "Iron bar", 10_000));
        items.add(item(ItemID.BLANKRUNE, "Rune essence", 10_000));
        items.add(item(ItemID.AIR_TALISMAN, "Air talisman", 1));
        items.add(item(ItemID.MIND_TALISMAN, "Mind talisman", 1));
        items.add(item(ItemID.WATER_TALISMAN, "Water talisman", 1));
        items.add(item(ItemID.EARTH_TALISMAN, "Earth talisman", 1));
        items.add(item(ItemID.FIRE_TALISMAN, "Fire talisman", 1));
        items.add(item(ItemID.BODY_TALISMAN, "Body talisman", 1));
        items.add(item(ItemID.HUNTING_SNARE, "Bird snare", 1));
        items.add(item(ItemID.AIRRUNE, "Air rune", 10_000));
        items.add(item(ItemID.MINDRUNE, "Mind rune", 10_000));
        items.add(item(ItemID.FIRERUNE, "Fire rune", 100_000));
        items.add(item(ItemID.CHAOSRUNE, "Chaos rune", 10_000));
        items.add(item(ItemID.DEATHRUNE, "Death rune", 10_000));
        items.add(item(ItemID.RAW_SALMON, "Raw salmon", 10_000));
        items.add(item(ItemID.RAW_HERRING, "Raw herring", 10_000));
        items.add(item(ItemID.COINS, "Coins", 10_000));
        items.add(item(ItemID.POTATO_SEED, "Potato seed", 1_000));
        items.add(item(ItemID.WATERMELON_SEED, "Watermelon seed", 1_000));
        items.add(item(ItemID.RAKE, "Rake", 1));
        items.add(item(ItemID.DIBBER, "Seed dibber", 1));
        items.add(item(ItemID.SPADE, "Spade", 1));
        items.add(item(ItemID.WOODPLANK, "Plank", 10_000));
        items.add(item(ItemID.PLANK_OAK, "Oak plank", 10_000));
        items.add(item(ItemID.NAILS, "Steel nails", 10_000));
        items.add(item(ItemID.POH_SAW, "Saw", 1));
        return items;
    }

    private static ItemStackSnapshot item(int id, String name, int quantity)
    {
        return new ItemStackSnapshot(id, name, quantity);
    }

    private static String label(Scenario scenario, int level,
            StrategyMode mode, SessionIntent session)
    {
        return scenario.name + " level " + level + " " + mode + " " + session;
    }

    private static final class Scenario
    {
        private final String name;
        private final MembershipStatus membership;
        private final int type;

        private Scenario(String name, MembershipStatus membership, int type)
        {
            this.name = name;
            this.membership = membership;
            this.type = type;
        }
    }
}
