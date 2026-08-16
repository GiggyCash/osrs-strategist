package com.udderlywet.osrsstrategist;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceSourceCatalogTest
{
    private final ResourceSourceCatalog catalog = new ResourceSourceCatalog();

    @Test
    public void commonProgressionResourcesHaveConcreteRoutes()
    {
        for (String item : new String[]{"Oak plank", "Ranarr seed",
                "Ranarr weed", "Nature rune", "Pure essence",
                "Steel bar", "Cannonball", "Molten glass",
                "Bow string", "Dragon bones", "Raw karambwan"})
        {
            assertFalse("Missing route for " + item, catalog.match(item).isEmpty());
        }
    }

    @Test
    public void uimRoutesDoNotTellPlayerToBankResources()
    {
        List<String> routes = catalog.suggestions(
                "Oak plank", AccountMode.ULTIMATE_IRONMAN, false);
        assertFalse(routes.isEmpty());
        for (String route : routes)
            assertFalse(route.toLowerCase().contains("bank the"));
    }

    @Test
    public void hardcoreHidesHighRiskWildernessBoneRoute()
    {
        List<String> safe = catalog.suggestions(
                "Dragon bones", AccountMode.HARDCORE_IRONMAN, true);
        assertFalse(safe.isEmpty());
        for (String route : safe)
            assertFalse(route.toLowerCase().contains("wilderness dragon routes"));
    }

    @Test
    public void wildernessRouteCanBeFilteredGlobally()
    {
        List<String> noWildy = catalog.suggestions(
                "Dragon bones", AccountMode.IRONMAN, false);
        List<String> wildy = catalog.suggestions(
                "Dragon bones", AccountMode.IRONMAN, true);
        assertTrue(wildy.size() >= noWildy.size());
    }
}
