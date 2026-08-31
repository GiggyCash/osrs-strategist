package compass;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/** Reads patch varbits only while the player is in a known Farming region. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class FarmingRunObservationService
{
    private final Client client;
    private final FarmingRunCatalog catalog;
    private final FarmingPatchStateDecoder decoder;
    private final FarmingRunStateStore store;

    public boolean observeCurrentPatches()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        var player = client.getLocalPlayer();
        if (player == null) return false;
        var location = player.getWorldLocation();
        if (location == null) return false;

        var changed = false;
        List<FarmingRunPatchDefinition> patches =
                catalog.forRegion(location.getRegionID());
        for (FarmingRunPatchDefinition patch : patches)
        {
            var raw = client.getVarbitValue(patch.getVarbitId());
            var state = decoder.decode(patch.getKind(), raw);
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
