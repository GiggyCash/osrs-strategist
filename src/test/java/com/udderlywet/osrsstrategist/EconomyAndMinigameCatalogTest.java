package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
    public void minigameCatalogCoversBroadRepeatableGameContent()
    {
        MinigameCatalog catalog = new MinigameCatalog();
        assertTrue(catalog.all().size() >= 45);

        // Core skilling/progression activities.
        assertNotNull(catalog.byId("wintertodt"));
        assertNotNull(catalog.byId("tempoross"));
        assertNotNull(catalog.byId("guardians-of-the-rift"));
        assertNotNull(catalog.byId("mastering-mixology"));
        assertNotNull(catalog.byId("motherlode-mine"));
        assertNotNull(catalog.byId("shooting-stars"));
        assertNotNull(catalog.byId("blast-furnace"));

        // Combat, hybrid, D&D, and utility activities.
        assertNotNull(catalog.byId("pest-control"));
        assertNotNull(catalog.byId("barbarian-assault"));
        assertNotNull(catalog.byId("the-gauntlet"));
        assertNotNull(catalog.byId("tears-of-guthix"));
        assertNotNull(catalog.byId("managing-miscellania"));
        assertNotNull(catalog.byId("chompy-bird-hunting"));
        assertNotNull(catalog.byId("underwater-agility-thieving"));

        // Sailing-era activities have typed homes too.
        assertNotNull(catalog.byId("barracuda-trials"));
        assertNotNull(catalog.byId("port-tasks"));
        assertNotNull(catalog.byId("shipwreck-salvaging"));
        assertNotNull(catalog.byId("sea-charting"));
    }

    @Test
    public void onlyVerifiedF2pMinigamesAreMarkedFreeToPlay()
    {
        MinigameCatalog catalog = new MinigameCatalog();
        assertTrue(catalog.byId("emirs-arena").isFreeToPlay());
        assertTrue(catalog.byId("clan-wars").isFreeToPlay());
        assertTrue(catalog.byId("castle-wars").isFreeToPlay());
        assertTrue(catalog.byId("last-man-standing").isFreeToPlay());

        assertFalse(catalog.byId("bounty-hunter").isFreeToPlay());
        assertFalse(catalog.byId("wintertodt").isFreeToPlay());
        assertFalse(catalog.byId("tempoross").isFreeToPlay());
    }
}
