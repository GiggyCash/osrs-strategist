package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Red-team representative accounts through the real skill planning stack. */
public class PlayerFacingScenarioQualityTest
{
    @Test
    public void representativeAccountsAlwaysReceiveAConcreteDoNext()
    {
        List<Scenario> scenarios = new ArrayList<>();
        scenarios.add(new Scenario("fresh F2P Main", MembershipStatus.F2P, 0,
                1, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME));
        scenarios.add(new Scenario("low-level F2P Main", MembershipStatus.F2P, 0,
                29, StrategyMode.BALANCED, SessionIntent.QUICK_20_MIN));
        scenarios.add(new Scenario("fresh P2P Main", MembershipStatus.P2P, 0,
                1, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME));
        scenarios.add(new Scenario("midgame Main", MembershipStatus.P2P, 0,
                60, StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR));
        scenarios.add(new Scenario("fresh Ironman", MembershipStatus.P2P, 1,
                1, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME));
        scenarios.add(new Scenario("midgame Ironman", MembershipStatus.P2P, 1,
                60, StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION));
        scenarios.add(new Scenario("GIM", MembershipStatus.P2P, 4,
                45, StrategyMode.BALANCED, SessionIntent.ONE_HOUR));
        scenarios.add(new Scenario("UIM", MembershipStatus.P2P, 2,
                45, StrategyMode.BALANCED, SessionIntent.QUICK_20_MIN));
        scenarios.add(new Scenario("HCIM", MembershipStatus.P2P, 3,
                45, StrategyMode.RELAXED, SessionIntent.AFK));
        scenarios.add(new Scenario("unknown membership", MembershipStatus.UNKNOWN, 0,
                1, StrategyMode.RELAXED, SessionIntent.AFK));

        StrategyEngine engine = engine();
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        for (Scenario scenario : scenarios)
        {
            StrategyResult result = engine.evaluate(data(scenario),
                    scenario.mode, scenario.session, new PreferenceProfile());
            assertFalse(scenario.name, result.getRecommendations().isEmpty());
            Recommendation top = result.getRecommendations().get(0);
            assertTrue(scenario.name + ": "
                            + RecommendationPresentation.compactText(top),
                    policy.canLeadQueue(top));
            assertNotNull(scenario.name, top.getGuidance());
            assertFalse(scenario.name, top.getGuidance().getLocation().trim().isEmpty());
            String compact = RecommendationPresentation.compactText(top);
            assertTrue(scenario.name, compact.contains("WHERE"));
            assertTrue(scenario.name, compact.contains("DO"));
        }
    }

    private static StrategyEngine engine()
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(
                new TrainingMethodDatabase(),
                new RequirementEvidenceEngine(
                        new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                        new AgilityAccessEvaluator(new AgilityCourseCatalog())),
                new ExpandedTrainingMethodCatalog(),
                new F2pBaselineMethodCatalog(),
                new TrainingMethodPolicy());
        return new StrategyEngine(new RecommendationEngine(selector), null,
                null, null, new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static StrategyDataBundle data(Scenario scenario)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS
                    ? Math.max(10, scenario.level) : scenario.level;
            levels.put(skill, level);
            int skillXp = level <= 1 ? 0 : Experience.getXpForLevel(level);
            xp.put(skill, skillXp);
            total += level;
            totalXp += skillXp;
        }
        AccountSnapshot account = new AccountSnapshot(
                scenario.name, 100L + scenario.type, scenario.type,
                AccountMode.fromTypeCode(scenario.type).name(),
                scenario.membership,
                scenario.membership == MembershipStatus.P2P ? 1 : 0,
                total, totalXp, levels, xp);
        StrategyDataBundle.Builder builder = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()));
        if (scenario.type != 2)
            builder.bank(new BankSnapshot(Collections.emptyList(), 1L));
        return builder.build();
    }

    private static final class Scenario
    {
        private final String name;
        private final MembershipStatus membership;
        private final int type;
        private final int level;
        private final StrategyMode mode;
        private final SessionIntent session;

        private Scenario(String name, MembershipStatus membership, int type,
                int level, StrategyMode mode, SessionIntent session)
        {
            this.name = name;
            this.membership = membership;
            this.type = type;
            this.level = level;
            this.mode = mode;
            this.session = session;
        }
    }
}
