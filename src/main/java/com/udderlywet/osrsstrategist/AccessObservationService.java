package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Learns positive access facts from normal gameplay without user prompts.
 *
 * <p>Walking into a known Farming region is proof that this character can reach
 * it. That proof is remembered per character and can satisfy future readiness
 * checks even after RuneLite is restarted.</p>
 */
@Singleton
public class AccessObservationService
{
    private final Client client;
    private final AccountAccessMemoryStore memoryStore;
    private final FarmingAccessCatalog farmingAccessCatalog;
    private int lastRegionId = -1;

    @Inject
    public AccessObservationService(
            Client client,
            AccountAccessMemoryStore memoryStore,
            FarmingAccessCatalog farmingAccessCatalog)
    {
        this.client = client;
        this.memoryStore = memoryStore;
        this.farmingAccessCatalog = farmingAccessCatalog;
    }

    public void observeCurrentLocation()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return;
        }

        WorldPoint location = player.getWorldLocation();
        if (location == null)
        {
            return;
        }

        int regionId = location.getRegionID();
        if (regionId == lastRegionId)
        {
            return;
        }

        lastRegionId = regionId;
        memoryStore.remember("region." + regionId);

        FarmingAccessDefinition farming =
                farmingAccessCatalog.forRegion(regionId);
        if (farming != null)
        {
            memoryStore.remember(farming.observationKey());
        }
    }

    public void clearForAccountChange()
    {
        lastRegionId = -1;
        memoryStore.clearCacheForAccountChange();
    }
}
