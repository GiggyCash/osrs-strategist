package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrainingMethodPolicyAccountModeTest
{
    private final TrainingMethodPolicy policy = new TrainingMethodPolicy();

    @Test
    public void hardcoreRejectsWildernessEvenWhenGloballyEnabled()
    {
        TrainingMethod method = method(true);
        TrainingMethodMetadata metadata = metadata(true, true, RiskLevel.HIGH, false);
        assertFalse(policy.isAllowed(data(AccountMode.HARDCORE_IRONMAN,
                MembershipStatus.P2P), method, metadata, true));
    }

    @Test
    public void uimRejectsMethodNotMarkedUimFriendly()
    {
        TrainingMethod method = method(false);
        TrainingMethodMetadata metadata = metadata(false, true, RiskLevel.NONE, false);
        assertFalse(policy.isAllowed(data(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P), method, metadata, false));
    }

    @Test
    public void f2pRejectsMembersOnlyMetadata()
    {
        TrainingMethod method = method(false);
        TrainingMethodMetadata metadata = metadata(true, false, RiskLevel.NONE, false);
        assertFalse(policy.isAllowed(data(AccountMode.MAIN,
                MembershipStatus.F2P), method, metadata, false));
    }

    @Test
    public void safeSelfSourceMethodIsAllowedForIron()
    {
        TrainingMethod method = method(false);
        TrainingMethodMetadata metadata = metadata(true, true, RiskLevel.NONE, false);
        assertTrue(policy.isAllowed(data(AccountMode.IRONMAN,
                MembershipStatus.P2P), method, metadata, false));
    }

    private static TrainingMethod method(boolean wilderness)
    {
        return new TrainingMethod("test", Skill.MINING, 1, 99, "Test", "Test",
                1, 1, 1, AttentionLevel.LOW, 10, 1,
                Collections.emptyList(), RecommendationConfidence.VERIFIED,
                false, wilderness, false);
    }

    private static TrainingMethodMetadata metadata(boolean uim, boolean hardcore,
            RiskLevel risk, boolean f2p)
    {
        return new TrainingMethodMetadata(TrainingIntensity.BALANCED,
                MethodCostTier.FREE, risk, f2p, true, uim, hardcore,
                Collections.emptyList());
    }

    private static StrategyDataBundle data(AccountMode mode, MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 1); xp.put(skill, 0); }
        AccountSnapshot account = new AccountSnapshot("Test", code(mode), mode.name(),
                membership, 0, 1, 0L, levels, xp);
        return StrategyDataBundle.builder(account).build();
    }

    private static int code(AccountMode mode)
    {
        switch (mode)
        {
            case MAIN: return 0;
            case IRONMAN: return 1;
            case ULTIMATE_IRONMAN: return 2;
            case HARDCORE_IRONMAN: return 3;
            case GROUP_IRONMAN: return 4;
            case HARDCORE_GROUP_IRONMAN: return 5;
            case UNRANKED_GROUP_IRONMAN: return 6;
            default: return -1;
        }
    }
}
