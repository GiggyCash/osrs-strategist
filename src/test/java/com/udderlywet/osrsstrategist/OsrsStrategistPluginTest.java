package com.udderlywet.osrsstrategist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
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
        assertTrue(plugin.consumeStrategyRefreshPending(10_000L));
        assertFalse(plugin.consumeStrategyRefreshPending(10_001L));
        assertTrue(plugin.consumePohRefreshPending(10_000L));
        assertFalse(plugin.consumePohRefreshPending(10_001L));
    }

    @Test
    public void burstyStrategicEventsRemainPendingUntilRefreshInterval()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        plugin.onVarbitChanged(null);
        assertTrue(plugin.consumeStrategyRefreshPending(10_000L));

        plugin.onVarbitChanged(null);
        assertFalse(plugin.consumeStrategyRefreshPending(11_999L));
        // The throttled event was retained rather than discarded.
        assertTrue(plugin.consumeStrategyRefreshPending(12_000L));
        assertFalse(plugin.consumeStrategyRefreshPending(12_001L));
    }

    @Test
    public void farmingReadsAreBoundedAndClockRegressionDoesNotStallThem()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        assertTrue(plugin.consumeFarmingObservation(10_000L));
        assertFalse(plugin.consumeFarmingObservation(11_999L));
        assertTrue(plugin.consumeFarmingObservation(12_000L));
        assertTrue(plugin.consumeFarmingObservation(9_000L));
    }

    @Test
    public void pohSceneScansAreCoalescedBehindObjectEvidence()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        assertFalse(plugin.consumePohRefreshPending(10_000L));
        plugin.onGameObjectSpawned((GameObjectSpawned) null);
        plugin.onGameObjectSpawned((GameObjectSpawned) null);
        assertTrue(plugin.consumePohRefreshPending(10_000L));
        assertFalse(plugin.consumePohRefreshPending(10_001L));
    }

    @Test
    public void diaryJournalReadsAreCoalescedBehindWidgetEvidence()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        assertFalse(plugin.consumeDiaryRefreshPending());
        WidgetLoaded loaded = new WidgetLoaded();
        loaded.setGroupId(InterfaceID.JOURNALSCROLL);
        plugin.onWidgetLoaded(loaded);
        plugin.onWidgetLoaded(loaded);
        assertTrue(plugin.consumeDiaryRefreshPending());
        assertFalse(plugin.consumeDiaryRefreshPending());
    }

    @Test
    public void onlyStrategicContainersTriggerFullAccountRefresh()
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        plugin.onItemContainerChanged((ItemContainerChanged) null);
        assertFalse(plugin.consumeStrategyRefreshPending(10_000L));
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
