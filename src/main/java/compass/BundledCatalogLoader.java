package compass;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Loads required, version-controlled catalog data from the plugin JAR. */
final class BundledCatalogLoader
{
    // Plugin-owned JSON uses plain fields/enums/collections, not client adapters.
    // Text and policy statics are reached before RuneLite configures injection.
    private static final Gson GSON = new Gson();

    private BundledCatalogLoader() {}

    static <T> T[] array(String resource, Class<T[]> type)
    {
        // Diagnostics must not initialize Text, which is itself loaded here.
        var stream = BundledCatalogLoader.class.getResourceAsStream(resource);
        if (stream == null)
            throw new IllegalStateException("Missing required bundled catalog: " + resource);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var values = GSON.fromJson(reader, type);
            if (values == null)
                throw new IllegalStateException("Empty required bundled catalog: " + resource);
            for (int index = 0; index < values.length; index++)
                if (values[index] == null)
                    throw new IllegalStateException("Null record " + index + " in " + resource);
            return values;
        }
        catch (IOException | JsonParseException ex)
        {
            throw new IllegalStateException("Malformed required bundled catalog: " + resource, ex);
        }
    }
}
