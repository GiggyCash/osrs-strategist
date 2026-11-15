package compass;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiveItemStateReaderTest
{
    @Test
    public void preservesRuneLiteNotedEvidence()
    {
        Client client = mock(Client.class);
        ItemManager items = mock(ItemManager.class);
        ItemContainer inventory = mock(ItemContainer.class);
        ItemComposition shark = mock(ItemComposition.class);
        ItemComposition notedShark = mock(ItemComposition.class);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        when(inventory.getItems()).thenReturn(new Item[]{
                new Item(385, 1), new Item(386, 5)});
        when(items.getItemComposition(385)).thenReturn(shark);
        when(items.getItemComposition(386)).thenReturn(notedShark);
        when(shark.getName()).thenReturn("Shark");
        when(shark.getNote()).thenReturn(-1);
        when(notedShark.getName()).thenReturn("Shark");
        when(notedShark.getNote()).thenReturn(799);

        ItemsState observed = new LiveItemStateReader(client, items)
                .readInventory();

        assertFalse(observed.getItems().get(0).isNoted());
        assertTrue(observed.getItems().get(1).isNoted());
    }
}
