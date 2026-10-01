package compass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises actual startup/shutdown and RuneLite's client-thread queue. */
public class PluginRuntimeLifecycleTest
{
    private final OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
    private final AtomicBoolean onClientThread = new AtomicBoolean();
    private final ClientThread clientThread = new ClientThread();
    private Client client;
    private StrategyDataAssembler assembler;
    private ClientToolbar toolbar;
    private OverlayManager overlays;

    @Before
    public void wire() throws Exception
    {
        for (Field field : OsrsStrategistPlugin.class.getDeclaredFields())
        {
            if (!field.isAnnotationPresent(Inject.class)) continue;
            field.setAccessible(true);
            field.set(plugin, field.getType() == PlanContinuityService.class
                    ? new PlanContinuityService() : mock(field.getType()));
        }
        set(plugin, "config", new OsrsStrategistConfig() { });
        set(plugin, "clientThread", clientThread);
        toolbar = (ClientToolbar) get(plugin, "clientToolbar");
        overlays = (OverlayManager) get(plugin, "overlayManager");
        assembler = (StrategyDataAssembler) get(plugin, "strategyDataAssembler");
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Test player");
        client = mock(Client.class, invocation -> {
            if (invocation.getMethod().getName().equals("isClientThread"))
                return onClientThread.get();
            assertTrue("Live client access outside client thread: "
                    + invocation.getMethod().getName(), onClientThread.get());
            switch (invocation.getMethod().getName())
            {
                case "getGameState": return GameState.LOGGED_IN;
                case "getLocalPlayer": return player;
                default: return RETURNS_DEFAULTS.answer(invocation);
            }
        });
        set(clientThread, "client", client);
        AccountReader reader = new AccountReader(client);
        when(assembler.read()).thenAnswer(invocation -> {
            reader.read(); // Real varbit/varp/player/skill reads enforce the contract.
            return null; // No stable account snapshot: publish the waiting state.
        });
    }

    @Test
    public void enableDisableEnableRegistersSidebarAndOverlaysWithoutOffThreadReads()
            throws Exception
    {
        for (int cycle = 0; cycle < 2; cycle++)
        {
            SwingUtilities.invokeAndWait(plugin::startUp);
            assertNotNull(get(plugin, "panel"));
            NavigationButton navigation = (NavigationButton) get(plugin, "navButton");
            assertEquals("Gielinor Compass", navigation.getTooltip());
            assertSame(get(plugin, "panel"), navigation.getPanel());
            verify(toolbar).addNavigation(navigation);
            verify(assembler, times(cycle)).read();
            drainClientQueue();
            verify(assembler, times(cycle + 1)).read();
            SwingUtilities.invokeAndWait(plugin::shutDown);
            verify(toolbar).removeNavigation(navigation);
            assertNull(get(plugin, "panel"));
        }
        verify(overlays, times(6)).add(any());
        verify(overlays, times(6)).remove(any());
        onClientThread.set(true);
        verify(client, times(2)).getVarbitValue(VarbitID.IRONMAN);
        onClientThread.set(false);
    }

    @Test
    public void queuedReadFromDisabledEnableCannotRunAgainstNextPanel() throws Exception
    {
        SwingUtilities.invokeAndWait(plugin::startUp);
        SwingUtilities.invokeAndWait(plugin::shutDown);
        drainClientQueue();
        verify(assembler, never()).read();
        SwingUtilities.invokeAndWait(plugin::startUp);
        SwingUtilities.invokeAndWait(plugin::shutDown);
        SwingUtilities.invokeAndWait(plugin::startUp);
        drainClientQueue();
        verify(assembler, times(1)).read();
        SwingUtilities.invokeAndWait(plugin::shutDown);
    }

    @Test
    public void configAndSidebarRefreshAlsoScheduleLiveReads() throws Exception
    {
        SwingUtilities.invokeAndWait(plugin::startUp);
        drainClientQueue();
        ConfigChanged event = new ConfigChanged();
        event.setGroup(OsrsStrategistConfig.GROUP);
        event.setKey(CompassConfigKeys.SIDEBAR_TEXT_SIZE);
        SwingUtilities.invokeAndWait(() -> plugin.onConfigChanged(event));
        verify(assembler, times(1)).read();
        drainClientQueue();
        verify(assembler, times(2)).read();
        // The panel refresh callback is this same private entry point.
        Method refresh = OsrsStrategistPlugin.class.getDeclaredMethod("updateAccountPanel");
        refresh.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try { refresh.invoke(plugin); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
        verify(assembler, times(2)).read();
        drainClientQueue();
        verify(assembler, times(3)).read();
        SwingUtilities.invokeAndWait(plugin::shutDown);
    }

    @Test
    public void loggedOutStartupKeepsConfigAndSidebarAvailable() throws Exception
    {
        onClientThread.set(true);
        doReturn(GameState.LOGIN_SCREEN).when(client).getGameState();
        onClientThread.set(false);
        ConfigManager manager = mock(ConfigManager.class);
        OsrsStrategistConfig config = new OsrsStrategistConfig() { };
        when(manager.getConfig(OsrsStrategistConfig.class)).thenReturn(config);
        assertSame(config, plugin.provideConfig(manager));
        SwingUtilities.invokeAndWait(plugin::startUp);
        drainClientQueue();
        assertNotNull(get(plugin, "navButton"));
        onClientThread.set(true);
        verify(client, never()).getVarbitValue(anyInt());
        onClientThread.set(false);
        SwingUtilities.invokeAndWait(plugin::shutDown);
    }

    private void drainClientQueue() throws Exception
    {
        onClientThread.set(true);
        try
        {
            Method drain = ClientThread.class.getDeclaredMethod("invoke");
            drain.setAccessible(true);
            drain.invoke(clientThread);
        }
        finally { onClientThread.set(false); }
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static Object get(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
