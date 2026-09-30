package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdaptiveMaterialIdentityTest
{
    private final AdaptiveActionSelector selector = new AdaptiveActionSelector();
    private final MethodProfile profile = new MethodExecutionProfileCatalog().forMethod("crafting_gems");
    private final ActionDef sapphire = action("Sapphire", 50);
    private final ActionDef onyx = action("Onyx", 167.5f);

    @Test
    public void reviewedIdsTakePriorityOverNamesAcrossIronModes()
    {
        for (int mode : new int[] {1, 2, 3, 4, 5, 6})
        {
            GameData exact = MethodIntelligenceTest.data(mode, -1,
                    stock(1623, "Different display label"), Collections.emptyList());
            assertSame(sapphire, select(exact, false));
            GameData wrongIdentity = MethodIntelligenceTest.data(mode, -1,
                    stock(999999, "Uncut sapphire"), Collections.emptyList());
            assertSame(onyx, select(wrongIdentity, false));
        }
    }

    @Test
    public void exactIdDoesNotMakeUimBankSuppliesUsable()
    {
        GameData uim = MethodIntelligenceTest.data(2, -1, Collections.emptyList(), stock(1623, "Uncut sapphire"));
        assertSame(onyx, select(uim, false));
        GameData iron = MethodIntelligenceTest.data(1, -1, Collections.emptyList(), stock(1623, "Different label"));
        assertSame(sapphire, select(iron, false));
    }

    @Test
    public void exactGroupSuppliesStillRequireEnabledObservedGroupStorage()
    {
        for (int mode : new int[] {4, 5, 6})
        {
            GameData data = GameData.builder(MethodIntelligenceTest.account(mode))
                    .inventory(new ItemsState(Collections.emptyList(), true))
                    .equipment(new ItemsState(Collections.emptyList(), true))
                    .bank(new ItemsState(Collections.emptyList(), true))
                    .groupStorage(new ItemsState(stock(1623, "Different label"), true)).build();
            assertSame(onyx, select(data, false));
            assertSame(sapphire, select(data, true));
        }
    }

    private ActionDef select(GameData data, boolean groupStorage)
    {
        return selector.select(data, profile, Arrays.asList(sapphire, onyx), 99,
                Membership.P2P, 0, 1000, 1, groupStorage);
    }

    private static List<ItemState> stock(int id, String name)
    {
        return Collections.singletonList(new ItemState(id, name, 100));
    }

    private static ActionDef action(String name, float xp)
    {
        return new ActionDef(Skill.CRAFTING, "test:" + name, name, 1, xp, "test", Membership.P2P);
    }
}
