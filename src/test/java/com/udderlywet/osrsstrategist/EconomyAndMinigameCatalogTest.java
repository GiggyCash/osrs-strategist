package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EconomyAndMinigameCatalogTest
{
    @Test
    public void moneyCatalogHasF2pIronUimAndHighLevelOptions()
    {
        MoneyMakingCatalog catalog = new MoneyMakingCatalog();
        assertTrue(catalog.all().size() >= 20);
        assertTrue(catalog.forAccount(AccountMode.MAIN).size() >= 10);
        assertTrue(catalog.forAccount(AccountMode.IRONMAN).size() >= 10);
        assertTrue(catalog.forAccount(AccountMode.ULTIMATE_IRONMAN).size() >= 8);
        assertTrue(catalog.forAccount(AccountMode.HARDCORE_IRONMAN).size() >= 8);
    }

    @Test
    public void minigameCatalogCoversMajorProgressionActivities()
    {
        MinigameCatalog catalog = new MinigameCatalog();
        assertTrue(catalog.all().size() >= 25);
        assertNotNull(catalog.byId("wintertodt"));
        assertNotNull(catalog.byId("tempoross"));
        assertNotNull(catalog.byId("guardians-of-the-rift"));
        assertNotNull(catalog.byId("pest-control"));
        assertNotNull(catalog.byId("barbarian-assault"));
        assertNotNull(catalog.byId("barracuda-trials"));
    }
}
