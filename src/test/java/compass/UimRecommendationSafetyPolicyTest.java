package compass;

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
                "Bank near Motherlode Mine.")
                .withSafetyEvidence(Safety
                        .skill(true, Skill.MINING)
                        .requiringConventionalBank());
        Recommendation carried = recommendation(
                "Mine copper, drop the ore when full, and repeat.",
                "East Lumbridge Swamp mine.");

        assertFalse(policy.isAllowed(banked, uim));
        assertTrue(policy.isAllowed(carried, uim));
        assertTrue(policy.isAllowed(banked, context(0)));
    }

    @Test
    public void everyTypedConventionalBankDependencyIsRejectedForUim()
    {
        CandidateSafetyPolicy policy = new CandidateSafetyPolicy();
        Safety[] evidence = {
                Safety.harmless(true),
                Safety.skill(true, Skill.MINING),
                Safety.verifiedSafe(true)
        };
        for (Safety value : evidence)
            assertFalse(policy.isAllowed(recommendation(
                    "Follow the named mining loop.", "East Lumbridge Swamp.")
                    .withSafetyEvidence(
                            value.requiringConventionalBank()), context(2)));
    }

    private static Recommendation recommendation(String action, String location)
    {
        return new Recommendation("skill:mining", "Train Mining to 50",
                "Reason", 10, null, Confidence.VERIFIED,
                45, 50, new Guidance(action,
                        "Bronze pickaxe.", location, null),
                Safety.skill(true, Skill.MINING));
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
                Membership.P2P, 1, 45 * Skill.values().length,
                0L, levels, xp);
        return new StrategyContext(GameData.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
    }
}
