package compass;

import com.google.gson.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Loads required, version-controlled catalog data from the plugin JAR. */
@Singleton
final class BundledCatalogLoader
{
    private final Gson gson;

    @Inject
    BundledCatalogLoader(Gson gson)
    {
        this.gson = java.util.Objects.requireNonNull(gson);
    }

    static <T> T[] array(String resource, Class<T[]> type)
    {
        // RuneLite establishes the host injector before discovering plugin classes.
        // Resolve an injected service on demand; never inject or cache a static Gson.
        var host = RuneLite.getInjector();
        if (host == null)
            throw new IllegalStateException("RuneLite host injector is not available for catalog loading");
        return host.getInstance(BundledCatalogLoader.class).read(resource, type);
    }

    <T> T[] read(String resource, Class<T[]> type)
    {
        // Diagnostics must not initialize Text, which is itself loaded here.
        var stream = BundledCatalogLoader.class.getResourceAsStream(resource);
        if (stream == null)
            throw new IllegalStateException("Missing required bundled catalog: " + resource);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var values = gson.fromJson(reader, type);
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
