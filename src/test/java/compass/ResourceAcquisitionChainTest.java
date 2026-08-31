package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceAcquisitionChainTest
{
    private final ResourceAcquisitionPlanner planner =
            new ResourceAcquisitionPlanner(new ResourceSourceCatalog());

    @Test
    public void mainAndIronGetDifferentNonNegativeResourceChains()
    {
        ResourceNeed need = new ResourceNeed(385, "Shark", 10);
        ResourceAcquisitionChain main = planner.planChain(context(account(0),
                Collections.emptyList()), need);
        assertEquals(10, main.getShortfall());
        assertEquals(AcquisitionSource.GRAND_EXCHANGE,
                main.nextStep().getSource());

        ResourceAcquisitionChain iron = planner.planChain(context(account(1),
                Collections.emptyList()), need);
        assertEquals(10, iron.getShortfall());
        assertEquals(AcquisitionSource.SELF_SOURCE,
                iron.nextStep().getSource());
        assertFalse(iron.getSteps().isEmpty());
    }

    @Test
    public void confirmedInventoryEndsChainWithoutNegativeShortfall()
    {
        ResourceNeed need = new ResourceNeed(385, "Shark", 10);
        ResourceAcquisitionChain chain = planner.planChain(context(account(2),
                Collections.singletonList(new ItemState(385, "Shark", 20))), need);
        assertEquals(0, chain.getShortfall());
        assertTrue(chain.nextStep().getAction().contains("inventory"));
    }

    private static StrategyContext context(AccountSnapshot account,
            java.util.List<ItemState> inventory)
    {
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(inventory))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 50); xp.put(skill, 0); }
        return new AccountSnapshot("Resource", typeCode,
                AccountMode.fromTypeCode(typeCode).name(), MembershipStatus.P2P,
                1, 1000, 0L, levels, xp);
    }
}
