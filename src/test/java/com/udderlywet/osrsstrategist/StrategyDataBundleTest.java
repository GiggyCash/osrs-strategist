package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Ensures unobserved data never gets converted into fake empty snapshots.
 */
public class StrategyDataBundleTest
{
    @Test
    public void builderKeepsUnknownSourcesNull()
    {
        AccountSnapshot account = account();

        StrategyDataBundle bundle =
                StrategyDataBundle.builder(account).build();

        assertSame(account, bundle.getAccount());
        assertNull(bundle.getBank());
        assertNull(bundle.getInventory());
        assertNull(bundle.getGroupStorage());
        assertNull(bundle.getPvm());
    }

    @Test
    public void builderPreservesObservedSources()
    {
        BankSnapshot bank = new BankSnapshot(
                Collections.emptyList(),
                123L
        );
        StorageSnapshot storage = StorageSnapshot.unknown();

        StrategyDataBundle bundle =
                StrategyDataBundle.builder(account())
                        .bank(bank)
                        .storage(storage)
                        .build();

        assertSame(bank, bundle.getBank());
        assertSame(storage, bundle.getStorage());
        assertEquals(123L, bundle.getBank().getCapturedAtMillis());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            experience.put(skill, 0);
        }

        return new AccountSnapshot(
                "Test",
                0,
                "Main",
                24,
                0L,
                levels,
                experience
        );
    }
}
