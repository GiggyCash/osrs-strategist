package compass;

import com.google.gson.JsonParseException;
import org.junit.Test;

import static org.junit.Assert.*;

public class BundledCatalogLoaderTest extends CatalogHostTest
{
    @Test
    public void catalogUsesTheHostInjectedGsonAdapters()
    {
        com.google.inject.Injector previous = net.runelite.client.RuneLite.getInjector();
        com.google.gson.Gson hostGson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(String[].class, (com.google.gson.JsonDeserializer<String[]>)
                        (json, type, context) -> new String[] {"host adapter"}).create();
        try
        {
            net.runelite.client.RuneLite.setInjector(com.google.inject.Guice.createInjector(
                    new com.google.inject.AbstractModule()
                    {
                        @Override protected void configure()
                        { bind(com.google.gson.Gson.class).toInstance(hostGson); }
                    }));
            assertArrayEquals(new String[] {"host adapter"},
                    BundledCatalogLoader.array("/content/player-text.json", String[].class));
        }
        finally { net.runelite.client.RuneLite.setInjector(previous); }
    }

    @Test
    public void missingHostCannotSilentlyCreateAPluginOwnedParser()
    {
        com.google.inject.Injector previous = net.runelite.client.RuneLite.getInjector();
        try
        {
            net.runelite.client.RuneLite.setInjector(null);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> BundledCatalogLoader.array("/content/player-text.json", String[].class));
            assertTrue(error.getMessage().contains("host injector"));
        }
        finally { net.runelite.client.RuneLite.setInjector(previous); }
    }

    @Test
    public void rejectsMissingResource()
    {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BundledCatalogLoader.array("/missing-catalog.json", String[].class));
        assertEquals("Missing required bundled catalog: /missing-catalog.json", error.getMessage());
    }

    @Test
    public void rejectsNullCatalog()
    {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BundledCatalogLoader.array("/catalog-tests/null.json", String[].class));
        assertEquals("Empty required bundled catalog: /catalog-tests/null.json", error.getMessage());
    }

    @Test
    public void rejectsNullRecord()
    {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BundledCatalogLoader.array("/catalog-tests/null-record.json", String[].class));
        assertEquals("Null record 1 in /catalog-tests/null-record.json", error.getMessage());
    }

    @Test
    public void rejectsMalformedCatalogAndPreservesCause()
    {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BundledCatalogLoader.array("/catalog-tests/malformed.json", String[].class));
        assertEquals("Malformed required bundled catalog: /catalog-tests/malformed.json", error.getMessage());
        assertTrue(error.getCause() instanceof JsonParseException);
    }
}
