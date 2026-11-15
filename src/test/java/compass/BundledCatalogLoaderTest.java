package compass;

import com.google.gson.JsonParseException;
import org.junit.Test;

import static org.junit.Assert.*;

public class BundledCatalogLoaderTest
{
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
