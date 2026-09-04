package compass;

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
        assertTier(catalog.filter(v -> v.style == CombatStyle.RANGED), GearBudgetTier.F2P);
        assertTier(catalog.filter(v -> v.style == CombatStyle.RANGED), GearBudgetTier.BUDGET);
        assertTier(catalog.filter(v -> v.style == CombatStyle.RANGED), GearBudgetTier.BIS);
        assertTier(catalog.filter(v -> v.style == CombatStyle.MAGIC), GearBudgetTier.BIS);
        assertTier(catalog.filter(v -> v.style == CombatStyle.MELEE_SLASH), GearBudgetTier.BIS);
        assertFalse(catalog.filter(v -> "raids".equals(v.contextId)).isEmpty());
    }

    @Test
    public void bisEntriesExplainContextDependence()
    {
        GearProgressionCatalog catalog = new GearProgressionCatalog();
        for (GearProgressionEntry entry : catalog.all())
        {
            if (entry.getTier() != GearBudgetTier.BIS) continue;
            String note = entry.getNote().toLowerCase();
            assertTrue(entry.getId(), note.contains("target")
                    || note.contains("encounter")
                    || note.contains("room")
                    || note.contains("mechanic")
                    || note.contains("setup")
                    || note.contains("raid"));
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
