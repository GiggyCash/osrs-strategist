package com.udderlywet.osrsstrategist;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PvmActivityCatalogTest
{
    @Test
    public void runeLiteBossCatalogProvidesBroadCurrentCoverage()
    {
        PvmActivityCatalog catalog = new PvmActivityCatalog();
        List<PvmActivityDefinition> bosses = catalog.all();
        assertTrue("Expected broad boss coverage", bosses.size() > 30);
        assertNotNull(catalog.byId("pvm:obor"));
        assertNotNull(catalog.byId("pvm:bryophyta"));
        assertNotNull(catalog.byId("pvm:brutus"));
        assertNotNull(catalog.byId("pvm:tombs_of_amascut"));
    }

    @Test
    public void membershipAndRiskMetadataAreConservative()
    {
        PvmActivityCatalog catalog = new PvmActivityCatalog();
        assertTrue(catalog.byId("pvm:obor").isFreeToPlay());
        assertTrue(catalog.byId("pvm:bryophyta").isFreeToPlay());
        assertTrue(catalog.byId("pvm:brutus").isFreeToPlay());
        PvmActivityDefinition scurrius = catalog.byId("pvm:scurrius");
        if (scurrius != null) assertFalse(scurrius.isFreeToPlay());
        PvmActivityDefinition callisto = catalog.byId("pvm:callisto");
        if (callisto != null) assertTrue(callisto.isWilderness());
    }

    @Test
    public void usefulEncounterCorpusHasCuratedFloorsWhileUnknownsStayGeneric()
    {
        PvmActivityCatalog catalog = new PvmActivityCatalog();
        assertTrue(catalog.curatedReadinessProfileCount() >= 10);
        assertTrue(catalog.hasCuratedReadinessProfile("pvm:obor"));
        assertTrue(catalog.hasCuratedReadinessProfile("pvm:vorkath"));
        assertTrue(catalog.hasCuratedReadinessProfile("pvm:tombs_of_amascut"));
        assertFalse(catalog.hasCuratedReadinessProfile("pvm:callisto"));
    }
}
