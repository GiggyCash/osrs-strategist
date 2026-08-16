package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/** Reads patch varbits only while the player is in a known Farming region. */
@Singleton
public class FarmingRunObservationService
{
    private final Client client;
    private final FarmingRunCatalog catalog;
    private final FarmingPatchStateDecoder decoder;
    private final FarmingRunStateStore store;

    @Inject
    public FarmingRunObservationService(
            Client client,
            FarmingRunCatalog catalog,
            FarmingPatchStateDecoder decoder,
            FarmingRunStateStore store)
    {
        this.client = client;
        this.catalog = catalog;
        this.decoder = decoder;
        this.store = store;
    }

    public boolean observeCurrentPatches()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        Player player = client.getLocalPlayer();
        if (player == null) return false;
        WorldPoint location = player.getWorldLocation();
        if (location == null) return false;

        boolean changed = false;
        List<FarmingRunPatchDefinition> patches =
                catalog.forRegion(location.getRegionID());
        for (FarmingRunPatchDefinition patch : patches)
        {
            int raw = client.getVarbitValue(patch.getVarbitId());
            FarmingPatchCycleState state = decoder.decode(patch.getKind(), raw);
            if (state != FarmingPatchCycleState.UNKNOWN)
            {
                changed |= store.remember(patch.getId(), state);
            }
        }
        return changed;
    }

    public void clearForAccountChange()
    {
        store.clearCacheForAccountChange();
    }
}
