package com.udderlywet.osrsstrategist;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GearAcquisitionCatalogTest
{
    @Test
    public void highValueRoutesAreUniqueReviewableAndMultiHop()
    {
        GearAcquisitionCatalog catalog = new GearAcquisitionCatalog();
        assertEquals(41, catalog.all().size());
        Set<String> ids = new HashSet<>();
        int multiHop = 0;
        for (GearAcquisitionRoute route : catalog.all())
        {
            assertTrue(ids.add(route.getId()));
            assertFalse(route.getSteps().isEmpty());
            assertFalse(route.getValueRule().trim().isEmpty());
            assertFalse(route.getProvenance().trim().isEmpty());
            if (route.getSteps().size() > 1) multiHop++;
        }
        assertTrue(multiHop >= 20);
        assertNotNull(catalog.forItem("Bow of faerdhinen"));
        assertNotNull(catalog.forItem("Slayer helmet (i)"));
    }
}
