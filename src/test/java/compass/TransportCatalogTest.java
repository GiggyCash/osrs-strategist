package compass;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransportCatalogTest
{
    @Test
    public void coversEveryHighValueTransportFamilyWithReusableFanOut()
    {
        TransportCatalog catalog = new TransportCatalog();
        Set<TransportCategory> categories = catalog.all().stream()
                .map(TransportDefinition::getCategory).collect(Collectors.toSet());
        assertEquals(EnumSet.allOf(TransportCategory.class), categories);
        assertEquals(41, catalog.all().size());
        assertEquals(catalog.all().size(), catalog.all().stream()
                .map(TransportDefinition::getId).distinct().count());
        for (TransportDefinition definition : catalog.all())
        {
            assertFalse(definition.getName().trim().isEmpty());
            assertTrue(definition.getId(), definition.getFanOut() >= 2);
        }
    }

    @Test
    public void august2026AgilityShortcutsUseLiveLevelsAndDiaryCaveat()
    {
        TransportCatalog catalog = new TransportCatalog();
        assertEquals(83, catalog.get("pollnivneach-west-plateau").getLevel());
        assertEquals(72, catalog.get("water-obelisk-catherby-crossing").getLevel());
        assertTrue(catalog.get("water-obelisk-catherby-crossing")
                .getItemOrAccessCheck().contains("Diary"));
        assertEquals(TransportCategory.AGILITY_SHORTCUT,
                catalog.get("mos-le-harmless-island-stones").getCategory());
    }
}
