package compass;

import java.util.Collections;
import java.util.EnumMap;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResourceAcquisitionPlannerTest
{
    @Test
    public void sharedOwnershipIndexCombinesObservedContainers()
    {
        AccountSnapshot account = account();
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(960, "Plank", 4))))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(960, "Plank", 6)), 1L))
                .build();
        ItemIndex items = new ItemIndex(data, false);
        assertTrue(items.usableOwnershipObserved());
        assertEquals(10, items.quantity(960));
    }

    @Test
    public void unobservedBankNeverProvesAResourceShortfall()
    {
        AccountSnapshot account = account();
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
        assertFalse(new ItemIndex(data, false).usableOwnershipObserved());
    }

    private static AccountSnapshot account()
    {
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 70); xp.put(skill, 0); }
        return new AccountSnapshot("Resource test", 1L, 0, "MAIN",
                Membership.P2P, 0, 0, 0L, levels, xp);
    }
}
