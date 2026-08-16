package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LiveClueStateReaderTest
{
    private final LiveClueStateReader reader = new LiveClueStateReader();

    @Test
    public void detectsHighestObservedClueTierAcrossInventoryAndBank()
    {
        InventorySnapshot inventory = new InventorySnapshot(Collections.singletonList(
                new ItemStackSnapshot(1, "Clue scroll (easy)", 1)));
        BankSnapshot bank = new BankSnapshot(Collections.singletonList(
                new ItemStackSnapshot(2, "Clue scroll (hard)", 1)), 1L);

        ClueSnapshot clue = reader.read(AccountMode.MAIN, inventory, bank, null);
        assertTrue(clue.isCluePresent());
        assertEquals("hard", clue.getClueType());
        assertEquals(RecommendationConfidence.VERIFIED, clue.getConfidence());
    }

    @Test
    public void uimIgnoresNormalBankClueState()
    {
        InventorySnapshot inventory = new InventorySnapshot(Collections.emptyList());
        BankSnapshot bank = new BankSnapshot(Collections.singletonList(
                new ItemStackSnapshot(2, "Clue scroll (master)", 1)), 1L);

        assertNull(reader.read(AccountMode.ULTIMATE_IRONMAN,
                inventory, bank, null));
    }

    @Test
    public void unobservedBankDoesNotEraseRememberedClue()
    {
        ClueSnapshot previous = new ClueSnapshot(true, "medium", 1234L,
                RecommendationConfidence.VERIFIED);
        InventorySnapshot inventory = new InventorySnapshot(Collections.emptyList());

        assertSame(previous, reader.read(AccountMode.MAIN,
                inventory, null, previous));
    }

    @Test
    public void observedEmptyBankCanClearCompletedMainClue()
    {
        ClueSnapshot previous = new ClueSnapshot(true, "medium", 1234L,
                RecommendationConfidence.VERIFIED);
        InventorySnapshot inventory = new InventorySnapshot(Collections.emptyList());
        BankSnapshot bank = new BankSnapshot(Collections.emptyList(), 1L);

        assertNull(reader.read(AccountMode.MAIN, inventory, bank, previous));
    }

    @Test
    public void intermediateClueItemPreservesPreviousTierAndAge()
    {
        ClueSnapshot previous = new ClueSnapshot(true, "elite", 1234L,
                RecommendationConfidence.VERIFIED);
        InventorySnapshot inventory = new InventorySnapshot(Arrays.asList(
                new ItemStackSnapshot(3, "Challenge scroll", 1)));

        assertSame(previous, reader.read(AccountMode.ULTIMATE_IRONMAN,
                inventory, null, previous));
    }
}
