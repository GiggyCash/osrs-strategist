package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MainEconomyPlannerTest
{
    private final MainEconomyPlanner planner = new MainEconomyPlanner();

    @Test
    public void affordableTimeSavingPurchaseCanBeRecommended()
    {
        MainPurchaseDecision decision = planner.evaluatePurchase(
                context(100_000L),
                new MainPurchaseCandidate(1, "Material", 100,
                        500L, 2, 30));
        assertEquals(MainPurchaseChoice.BUY, decision.getChoice());
        assertEquals(RecommendationConfidence.VERIFIED,
                decision.getConfidence());
    }

    @Test
    public void insufficientCashDoesNotJumpStraightToSellingGear()
    {
        MainPurchaseDecision decision = planner.evaluatePurchase(
                context(10_000L),
                new MainPurchaseCandidate(1, "Material", 100,
                        500L, 2, 30));
        assertEquals(MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES,
                decision.getChoice());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                decision.getConfidence());
    }

    @Test
    public void protectedItemsAreNeverSaleCandidates()
    {
        ProtectedItemProfile protectedItems = new ProtectedItemProfile();
        protectedItems.protect(42);
        assertFalse(planner.maySuggestSale(42, protectedItems, false));
        assertFalse(planner.maySuggestSale(99, protectedItems, true));
    }

    @Test
    public void unmeasuredPurchaseUsesBroadWealthBands()
    {
        AccountEconomySnapshot economy = new AccountEconomySnapshot(
                100_000L, 100_000L, RecommendationConfidence.VERIFIED);
        assertEquals(MainPurchaseChoice.BUY,
                planner.evaluateUnmeasuredPurchase(economy,
                        new PurchaseCostEstimate(true, 5_000L), true)
                        .getChoice());
        assertEquals(MainPurchaseChoice.SELF_SOURCE,
                planner.evaluateUnmeasuredPurchase(economy,
                        new PurchaseCostEstimate(true, 50_000L), true)
                        .getChoice());
        assertEquals(MainPurchaseChoice.CHECK_NEEDED,
                planner.evaluateUnmeasuredPurchase(economy,
                        PurchaseCostEstimate.unknown(), true).getChoice());
    }

    private static StrategyContext context(long coins)
    {
        StrategyDataBundle data = StrategyDataBundle.builder(main())
                .economy(new AccountEconomySnapshot(
                        coins, coins, RecommendationConfidence.VERIFIED))
                .build();
        return new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot main()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Main", 0, "Main", MembershipStatus.P2P,
                1, 1, 0L, levels, xp);
    }
}
