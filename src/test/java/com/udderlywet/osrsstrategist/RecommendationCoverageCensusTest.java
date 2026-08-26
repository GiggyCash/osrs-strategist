package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
