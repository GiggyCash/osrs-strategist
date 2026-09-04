package compass;

import net.runelite.http.api.RuneLiteAPI;

/** Supplies the test Gson before direct catalog construction initializes statics. */
public final class CatalogGsonTestAgent
{
    private CatalogGsonTestAgent() { }

    public static void premain(String arguments)
    {
        BundledCatalogLoader.injectGson(RuneLiteAPI.GSON);
    }
}
