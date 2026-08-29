package com.udderlywet.osrsstrategist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ItemContainerChanged;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class OsrsStrategistPluginTest
{
    @Test
    public void varbitBackedProgressionChangesAreSubscribed()
            throws NoSuchMethodException
    {
        assertTrue(OsrsStrategistPlugin.class
                .getMethod("onVarbitChanged", VarbitChanged.class)
                .isAnnotationPresent(Subscribe.class));

        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        plugin.onVarbitChanged(null);
        plugin.onVarbitChanged(null);
        assertTrue(plugin.consumeVarbitRefreshPending());
        assertFalse(plugin.consumeVarbitRefreshPending());
    }

    @Test
    public void onlyStrategicContainersTriggerFullAccountRefresh()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        plugin.onItemContainerChanged((ItemContainerChanged) null);
        assertFalse(plugin.consumeAccountRefreshPending());
        assertTrue(OsrsStrategistPlugin.isStrategicContainer(
                net.runelite.api.gameval.InventoryID.INV));
        assertTrue(OsrsStrategistPlugin.isStrategicContainer(
                net.runelite.api.gameval.InventoryID.WORN));
        assertTrue(OsrsStrategistPlugin.isStrategicContainer(
                net.runelite.api.gameval.InventoryID.BANK));
        assertTrue(OsrsStrategistPlugin.isStrategicContainer(
                net.runelite.api.gameval.InventoryID.INV_GROUP_TEMP));
        assertFalse(OsrsStrategistPlugin.isStrategicContainer(
                net.runelite.api.gameval.InventoryID.GENERALSHOP1));
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        // RuneLite's varargs signature uses Class<? extends Plugin> and emits
        // an unavoidable generic-array warning at this development launcher.
        ExternalPluginManager.loadBuiltin(OsrsStrategistPlugin.class);
        RuneLite.main(args);
    }
}
