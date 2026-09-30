package compass;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemsStateQuantityTest
{
    @Test
    public void repeatedRequestedIdsCannotReuseTheSameStack()
    {
        ItemsState items = new ItemsState(Arrays.asList(new ItemState(1, "Input", 3),
                new ItemState(2, "Other", 4)));
        assertEquals(3, items.quantityOf(1, 1));
        assertEquals(7, items.quantityOf(1, 2, 1));
        assertEquals(0, items.quantityOf((int[]) null));
    }

    @Test
    public void allQuantityQueriesClampOverflowAndIgnoreInvalidStacks()
    {
        ItemsState items = new ItemsState(Arrays.asList(null, new ItemState(1, "Input", -1),
                new ItemState(1, "Input", Integer.MAX_VALUE), new ItemState(1, "INPUT", 10)));
        assertEquals(Integer.MAX_VALUE, items.quantityOf(1));
        assertEquals(Integer.MAX_VALUE, items.quantityOf(1, 1));
        assertEquals(Integer.MAX_VALUE, items.quantityNamed("input", "INPUT"));
        assertEquals(Integer.MAX_VALUE, items.quantityWhere(name -> name.equals("input")));
        assertEquals(0, items.quantityWhere(null));
        assertEquals(0, new ItemsState(Collections.singletonList(new ItemState(1, "Input", -10))).quantityOf(1));
    }

    @Test
    public void overflowingObservedOwnershipCannotBecomePreparationShortfall()
    {
        GameData data = MethodIntelligenceTest.data(1, -1, Arrays.asList(
                new ItemState(1511, "Logs", Integer.MAX_VALUE), new ItemState(1511, "Logs", 1)),
                Collections.emptyList());
        MethodPreparation preparation = new MethodPreparationService(null).evaluate(data,
                Collections.singletonList(new MethodInput("Logs", 1511, 10)), false, true, false);
        assertEquals(MethodPreparation.State.READY, preparation.state);
        assertEquals(1, preparation.steps.size());
        assertEquals(MethodPreparation.Kind.CARRIED, preparation.steps.get(0).kind);
        assertEquals(10, preparation.steps.get(0).input.quantity);
    }
}
