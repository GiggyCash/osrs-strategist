package compass;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemObservationBoundaryTest
{
    @Test
    public void nonNullUnknownContainersCannotProveOwnership()
    {
        for (int mode = 0; mode <= 6; mode++)
        {
            ItemIndex unknown = index(mode, ItemsState.unknown(), ItemsState.unknown(), ItemsState.unknown());
            assertFalse(unknown.bankObserved());
            assertFalse(unknown.primaryOwnershipObserved());
            assertFalse(unknown.usableOwnershipObserved());
            assertFalse(unknown.resourceContainersObserved());
            ItemIndex known = index(mode, empty(), empty(), empty());
            assertEquals(mode != 2, known.bankObserved());
            assertTrue(known.primaryOwnershipObserved());
            assertTrue(known.resourceContainersObserved());
        }
    }

    @Test
    public void unobservedPayloadCannotSupplyOrSelectAnItem()
    {
        List<ItemState> payload = Collections.singletonList(new ItemState(1511, "Logs", 100));
        ItemsState unobserved = new ItemsState(false, payload);
        ItemIndex index = index(1, unobserved, unobserved, unobserved);
        assertEquals(0, index.quantity(1511));
        assertEquals(0, index.quantity("Logs"));
        assertEquals(0, index.inventoryQuantity("Logs"));
        assertEquals(0, index.equippedQuantity("Logs"));
        assertNull(index.bestName(name -> 1));
        assertNull(index.bestInventoryName(name -> 1));
        ItemIndex carried = index(1, new ItemsState(payload), empty(), ItemsState.unknown());
        assertEquals(100, carried.quantity(1511));
        assertFalse(carried.resourceContainersObserved());
    }

    @Test
    public void uimResourceEvidenceNeedsInventoryButOwnershipAlsoNeedsEquipment()
    {
        ItemIndex noEquipment = index(2, empty(), ItemsState.unknown(), ItemsState.unknown());
        assertTrue(noEquipment.resourceContainersObserved());
        assertFalse(noEquipment.primaryOwnershipObserved());
        ItemIndex noInventory = index(2, ItemsState.unknown(), empty(), empty());
        assertFalse(noInventory.resourceContainersObserved());
        assertFalse(noInventory.primaryOwnershipObserved());
    }

    private static ItemsState empty() { return new ItemsState(Collections.emptyList()); }

    private static ItemIndex index(int mode, ItemsState inventory, ItemsState equipment, ItemsState bank)
    {
        return new ItemIndex(GameData.builder(MethodIntelligenceTest.account(mode))
                .inventory(inventory).equipment(equipment).bank(bank).build(), false);
    }
}
