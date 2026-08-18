package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StrategyDataAssemblerLifecycleTest
{
    @Test
    public void accountIdentitySeparatesCharactersAndModesButNotMembershipTransitions()
    {
        AccountSnapshot mainF2p = account("Alice", 0, MembershipStatus.F2P);
        AccountSnapshot mainP2p = account("Alice", 0, MembershipStatus.P2P);
        AccountSnapshot iron = account("Alice", 1, MembershipStatus.P2P);
        AccountSnapshot other = account("Bob", 0, MembershipStatus.P2P);

        assertEquals(StrategyDataAssembler.accountIdentity(mainF2p),
                StrategyDataAssembler.accountIdentity(mainP2p));
        assertNotEquals(StrategyDataAssembler.accountIdentity(mainP2p),
                StrategyDataAssembler.accountIdentity(iron));
        assertNotEquals(StrategyDataAssembler.accountIdentity(mainP2p),
                StrategyDataAssembler.accountIdentity(other));
    }

    private static AccountSnapshot account(String name, int type,
            MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(name, type, "Test", membership,
                1, 32, 0L, levels, xp);
    }
}
