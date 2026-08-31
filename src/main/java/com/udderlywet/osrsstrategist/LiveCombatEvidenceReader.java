package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.VarbitID;

/** Reads only official current RuneLite prayer/spellbook identifiers. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LiveCombatEvidenceReader
{
    private final Client client;

    public CombatEvidenceSnapshot read()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
            return null;
        try
        {
            var active = EnumSet.noneOf(Prayer.class);
            for (Prayer prayer : Prayer.values())
                if (client.getVarbitValue(prayer.getVarbit()) > 0) active.add(prayer);
            return new CombatEvidenceSnapshot(
                    client.getVarbitValue(VarbitID.SPELLBOOK), active,
                    client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) > 0,
                    client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) > 0,
                    client.getVarbitValue(VarbitID.PRAYER_PRESERVE_UNLOCKED) > 0);
        }
        catch (RuntimeException transientClientState)
        {
            // Hops/account switches can briefly expose a logged-in state before
            // the backing var client is readable. Unknown is safer than stale data.
            return null;
        }
    }
}
