package compass;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import net.runelite.client.RuneLite;

/** Supplies the host dependency before plugin/test class initialization, as RuneLite does. */
public abstract class CatalogHostTest
{
    static
    {
        RuneLite.setInjector(Guice.createInjector(new AbstractModule()
        {
            @Override protected void configure()
            {
                bind(Gson.class).toInstance(new Gson());
            }
        }));
    }
}
