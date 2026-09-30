package compass;

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Test;

import static org.junit.Assert.*;

public class PluginConstructionTest extends CatalogHostTest
{
    @Test
    public void constructsBeforeGuiceConfigurationOrInjection() throws Exception
    {
        try (URLClassLoader loader = freshPluginClasses())
        {
            // RuneLite constructs the plugin before configuring its injector.
            Class<?> plugin = loader.loadClass("compass.OsrsStrategistPlugin");
            assertNotNull(plugin.getConstructor().newInstance());
            assertBundledData(loader);
        }
    }

    @Test
    public void textCanBeTheFirstInitializedClass() throws Exception
    {
        try (URLClassLoader loader = freshPluginClasses())
        {
            Class.forName("compass.Text", true, loader);
            assertBundledData(loader);
        }
    }

    @Test
    public void policyListsCanBeTheFirstInitializedClass() throws Exception
    {
        try (URLClassLoader loader = freshPluginClasses())
        {
            Class.forName("compass.PolicyLists", true, loader);
            assertBundledData(loader);
        }
    }

    private static void assertBundledData(ClassLoader loader) throws Exception
    {
        Class<?> text = loader.loadClass("compass.Text");
        var get = text.getDeclaredMethod("get", int.class);
        get.setAccessible(true);
        assertEquals("No course has been proven available yet.", get.invoke(null, 0));

        Class<?> policies = loader.loadClass("compass.PolicyLists");
        var data = policies.getDeclaredField("DATA");
        data.setAccessible(true);
        for (String name : new String[] {"one_defence_safe", "level_three_safe",
                "prayer_skiller_extra", "free_to_play_quests", "generic_titles",
                "generic_actions", "generic_locations", "unresolved_supplies"})
        {
            var field = policies.getDeclaredField(name);
            field.setAccessible(true);
            assertTrue(name, ((String[]) field.get(data.get(null))).length > 0);
        }
    }

    @Test
    public void otherCatalogBackedStaticsInitializeWithoutInjection() throws Exception
    {
        for (String name : new String[] {"CombatGuidanceService",
                "VariableMethodGuidanceService", "UniversalActionRecipeResolver",
                "AdaptiveMilestoneGuidanceService", "StrategyEngine", "DiaryTaskCatalog"})
        {
            try (URLClassLoader loader = freshPluginClasses())
            {
                Class.forName("compass." + name, true, loader);
                assertBundledData(loader);
            }
        }
    }

    private static URLClassLoader freshPluginClasses()
    {
        // Isolate plugin statics from other tests and any test-only pre-injection.
        URL classes = OsrsStrategistPlugin.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new URLClassLoader(new URL[] {classes}, PluginConstructionTest.class.getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (!name.startsWith("compass.")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name))
                {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) type = findClass(name);
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        };
    }
}
