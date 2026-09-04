package compass;

import static compass.Text.get;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

/** Loads required, version-controlled catalog data from the plugin JAR. */
final class BundledCatalogLoader
{
    private static Gson gson;

    private BundledCatalogLoader() {}

    @Inject
    static void injectGson(Gson clientGson)
    {
        gson = clientGson;
    }

    static <T> T[] array(String resource, Class<T[]> type)
    {
        if (gson == null)
            throw new IllegalStateException("Client Gson was not injected");
        var stream = BundledCatalogLoader.class.getResourceAsStream(resource);
        if (stream == null)
            throw new IllegalStateException(get(1125) + resource);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var values = gson.fromJson(reader, type);
            if (values == null)
                throw new IllegalStateException(get(1126) + resource);
            for (int index = 0; index < values.length; index++)
                if (values[index] == null)
                    throw new IllegalStateException("Null record " + index + " in " + resource);
            return values;
        }
        catch (IOException | JsonParseException ex)
        {
            throw new IllegalStateException(get(1127) + resource, ex);
        }
    }
}
