package compass;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.cluescrolls.ClueScrollService;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PluginInjectionTest extends CatalogHostTest
{
    @Test
    public void pluginDependencyGraphValidatesAfterConstruction()
    {
        // Match RuneLite's order: construct the plugin, then configure its injector.
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        Guice.createInjector(Stage.TOOL, new AbstractModule()
        {
            @Override
            protected void configure()
            {
                // TOOL validates the real Compass graph without starting a game client.
                // Only services supplied by RuneLite are left unprovisioned.
                hostService(Client.class);
                hostService(ConfigManager.class);
                hostService(ItemManager.class);
                hostService(SpriteManager.class);
                hostService(PluginManager.class);
                hostService(ClueScrollService.class);
                hostService(ClientToolbar.class);
                hostService(OverlayManager.class);
                hostService(Gson.class);
                bind(OsrsStrategistPlugin.class).toInstance(plugin);
                install(plugin);
            }

            private <T> void hostService(Class<T> type)
            {
                bind(type).toProvider(() -> {
                    throw new AssertionError("Graph validation must not provision " + type);
                });
            }
        });
    }

    @Test
    public void guiceConstructsAnalyzerWithoutRuntimeAccountState()
    {
        // Exercise actual constructor/member injection, not a direct Java constructor.
        assertNotNull(Guice.createInjector().getInstance(PvmReadinessAnalyzer.class));
    }
}
