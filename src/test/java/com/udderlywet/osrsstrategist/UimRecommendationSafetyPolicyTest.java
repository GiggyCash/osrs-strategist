package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UimRecommendationSafetyPolicyTest
{
    @Test
    public void normalBankLoopIsRejectedForUimButInventoryLoopIsAllowed()
    {
        CandidateSafetyPolicy policy = new CandidateSafetyPolicy();
        StrategyContext uim = context(2);
        Recommendation banked = recommendation(
                "Mine pay-dirt, bank the ores, and repeat.",
                "Bank near Motherlode Mine.");
        Recommendation carried = recommendation(
                "Mine copper, drop the ore when full, and repeat.",
                "East Lumbridge Swamp mine.");

        assertFalse(policy.isAllowed(banked, uim));
        assertTrue(policy.isAllowed(carried, uim));
        assertTrue(policy.isAllowed(banked, context(0)));
    }

    @Test
    public void siblingBankPhrasingsCannotBypassTheUimBoundary()
    {
        CandidateSafetyPolicy policy = new CandidateSafetyPolicy();
        String[] actions = {
                "Open your bank before starting.",
                "Withdraw bars, smith, bank, repeat.",
                "Cook at Lumbridge and bank upstairs.",
                "Bank or process the herbs.",
                "Prefer banked metal for this method."
        };
        for (String action : actions)
            assertFalse(action, policy.isAllowed(recommendation(
                    action, "Named location."), context(2)));
    }

    private static Recommendation recommendation(String action, String location)
    {
        return new Recommendation("skill:mining", "Train Mining to 50",
                "Reason", 10, null, RecommendationConfidence.VERIFIED,
                45, 50, new RecommendationGuidance(action,
                        "Bronze pickaxe.", location, null),
                CandidateSafetyEvidence.skill(true, Skill.MINING));
    }

    private static StrategyContext context(int type)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 45);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Mode", 100L + type,
                type, AccountMode.fromTypeCode(type).name(),
                MembershipStatus.P2P, 1, 45 * Skill.values().length,
                0L, levels, xp);
        return new StrategyContext(StrategyDataBundle.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
    }
}
