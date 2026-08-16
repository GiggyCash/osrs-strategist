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

    /**
     * @return true only when newly learned evidence can affect current strategy.
     */
    public boolean observeCurrentLocation()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return false;
        }

        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return false;
        }

        WorldPoint location = player.getWorldLocation();
        if (location == null)
        {
            return false;
        }

        int regionId = location.getRegionID();
        if (regionId == lastRegionId)
        {
            return false;
        }

        lastRegionId = regionId;

        // Generic region memory is useful later for transport/content discovery,
        // but it does not currently require an immediate recommendation rerank.
        memoryStore.remember("region." + regionId);

        FarmingAccessDefinition farming =
                farmingAccessCatalog.forRegion(regionId);
        return farming != null
                && memoryStore.remember(farming.observationKey());
    }

    public void clearForAccountChange()
    {
        lastRegionId = -1;
        memoryStore.clearCacheForAccountChange();
    }
}
