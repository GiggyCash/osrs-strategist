package compass;

import com.google.gson.Gson;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executors;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class ConfigProxyTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void allPublicConfigSignatureTypesAreAccessible()
    {
        for (var method : OsrsStrategistConfig.class.getMethods())
        {
            assertAccessible(method.getReturnType());
            for (Class<?> parameter : method.getParameterTypes()) assertAccessible(parameter);
            for (Class<?> exception : method.getExceptionTypes()) assertAccessible(exception);
        }
    }

    private static void assertAccessible(Class<?> type)
    {
        if (type.isArray()) { assertAccessible(type.getComponentType()); return; }
        if (type.isPrimitive()) return;
        for (Class<?> owner = type; owner != null; owner = owner.getEnclosingClass())
            assertTrue("Config proxy cannot access " + owner.getName(),
                    Modifier.isPublic(owner.getModifiers()));
    }

    @Test
    public void runeLiteProxyReadsEveryEnumDefaultAndRoundTripsStoredNames() throws Exception
    {
        var executor = Executors.newSingleThreadScheduledExecutor();
        try
        {
            // Use the real RuneLite proxy and codecs, without loading a user profile
            // or supplying any network/account services. Reflection is test-only.
            var constructor = ConfigManager.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ConfigManager manager = (ConfigManager) constructor.newInstance(
                    null, executor, null, null, new Gson(), null, null, null);
            var dataField = ConfigManager.class.getDeclaredField("configProfile");
            dataField.setAccessible(true);
            var dataConstructor = dataField.getType().getDeclaredConstructor(java.io.File.class);
            dataConstructor.setAccessible(true);
            Object data = dataConstructor.newInstance(temporary.newFile("config.properties"));
            dataField.set(manager, data);
            var setProperty = dataField.getType().getDeclaredMethod("setProperty", String.class, String.class);
            setProperty.setAccessible(true);
            OsrsStrategistConfig proxy = manager.getConfig(OsrsStrategistConfig.class);
            assertTrue(Proxy.isProxyClass(proxy.getClass()));
            OsrsStrategistConfig defaults = new OsrsStrategistConfig() { };
            var encode = ConfigManager.class.getDeclaredMethod("objectToString", Object.class);
            encode.setAccessible(true);
            var handler = Proxy.getInvocationHandler(proxy);
            var invalidate = handler.getClass().getDeclaredMethod("invalidate");
            invalidate.setAccessible(true);
            int enumMethods = 0;
            for (var method : OsrsStrategistConfig.class.getMethods())
            {
                if (!method.getReturnType().isEnum()) continue;
                enumMethods++;
                assertEquals(method.getName(), method.invoke(defaults), method.invoke(proxy));
                for (Object value : method.getReturnType().getEnumConstants())
                {
                    String stored = (String) encode.invoke(manager, value);
                    assertEquals(((Enum<?>) value).name(), stored);
                    setProperty.invoke(data, OsrsStrategistConfig.GROUP + "."
                            + method.getAnnotation(ConfigItem.class).keyName(), stored);
                    invalidate.invoke(handler);
                    assertSame(value, method.invoke(proxy));
                }
            }
            assertEquals(5, enumMethods);
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}
