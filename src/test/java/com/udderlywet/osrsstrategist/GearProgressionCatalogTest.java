package com.udderlywet.osrsstrategist;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearProgressionCatalogTest
{
    @Test
    public void containsF2pBudgetAndContextualBisLadders()
    {
        GearProgressionCatalog catalog = new GearProgressionCatalog();
        assertTier(catalog.forStyle(CombatStyle.RANGED), GearBudgetTier.F2P);
        assertTier(catalog.forStyle(CombatStyle.RANGED), GearBudgetTier.BUDGET);
        assertTier(catalog.forStyle(CombatStyle.RANGED), GearBudgetTier.BIS);
        assertTier(catalog.forStyle(CombatStyle.MAGIC), GearBudgetTier.BIS);
        assertTier(catalog.forStyle(CombatStyle.MELEE_SLASH), GearBudgetTier.BIS);
        assertFalse(catalog.forContext("raids").isEmpty());
    }

    @Test
    public void bisEntriesExplainEncounterDependence()
    {
        GearProgressionCatalog catalog = new GearProgressionCatalog();
        for (GearProgressionEntry entry : catalog.all())
        {
            if (entry.getTier() != GearBudgetTier.BIS) continue;
            String note = entry.getNote().toLowerCase();
            assertTrue(entry.getId(), note.contains("target")
                    || note.contains("encounter")
                    || note.contains("room")
                    || note.contains("mechanic"));
        }
    }

    private static void assertTier(List<GearProgressionEntry> entries,
            GearBudgetTier tier)
    {
        for (GearProgressionEntry entry : entries)
            if (entry.getTier() == tier) return;
        throw new AssertionError("Missing gear tier " + tier);
    }
}
