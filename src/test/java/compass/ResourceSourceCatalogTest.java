package compass;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceSourceCatalogTest
{
    @Test
    public void broadSourceFamiliesCoverCommonProgressionInputs()
    {
        ResourceSourceCatalog catalog = new ResourceSourceCatalog();
        assertEquals(60, catalog.all().size());
        for (ResourceSourceDefinition source : catalog.all())
            assertFalse(source.getId(), source.getSourceIds().isEmpty());
    }
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

    @Test
    public void selfSourcedBowstringsNameOneCompleteRoute()
    {
        for (AccountMode mode : new AccountMode[]{AccountMode.IRONMAN,
                AccountMode.ULTIMATE_IRONMAN})
        {
            String route = catalog.suggestions("Bow string", mode, false)
                    .get(0);
            assertTrue(mode.name(), route.contains("south of Seers' Village"));
            assertTrue(mode.name(), route.contains("west"));
            assertFalse(mode.name(), route.toLowerCase().contains("nearby"));
        }
    }

    @Test
    public void f2pAndUnknownMembershipUseOnlyExplicitlySafeItemRoutes()
    {
        for (MembershipStatus membership : new MembershipStatus[]{
                MembershipStatus.F2P, MembershipStatus.UNKNOWN})
        {
            List<String> logs = catalog.suggestions("Oak logs",
                    AccountMode.IRONMAN, membership, false);
            assertFalse(logs.isEmpty());
            assertTrue(logs.get(0).contains("F2P tree tier"));
            assertTrue(catalog.match("Oak logs").get(0).getSourceIds()
                    .contains(StrategySourceId.F2P_IRONMAN_GENERAL));

            assertTrue(catalog.suggestions("Teak logs", AccountMode.IRONMAN,
                    membership, false).isEmpty());
            assertTrue(catalog.suggestions("Bow string", AccountMode.IRONMAN,
                    membership, false).isEmpty());
            assertTrue(catalog.suggestions("Pure essence", AccountMode.IRONMAN,
                    membership, false).isEmpty());
        }
    }

    @Test
    public void f2pIronQuestMaterialsHaveConcreteSelfSources()
    {
        assertTrue(catalog.suggestions("Raw beef", AccountMode.IRONMAN,
                MembershipStatus.F2P, false).get(0).contains("F2P cow"));
        assertTrue(catalog.suggestions("Rune essence", AccountMode.IRONMAN,
                MembershipStatus.F2P, false).get(0).contains("Sedridor"));
        assertTrue(catalog.suggestions("Soft clay", AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.F2P, false).get(0).contains("F2P clay"));
    }
}
