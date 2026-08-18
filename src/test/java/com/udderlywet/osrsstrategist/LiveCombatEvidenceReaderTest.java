package com.udderlywet.osrsstrategist;

import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LiveCombatEvidenceReaderTest
{
    @Test
    public void loadingStateProducesNoEvidence()
    {
        Client client = client(GameState.LOADING, 0, false);
        assertNull(new LiveCombatEvidenceReader(client).read());
    }

    @Test
    public void loggedInReaderUsesOfficialSpellbookAndPrayerIdentifiers()
    {
        Client client = client(GameState.LOGGED_IN, 7, true);
        CombatEvidenceSnapshot snapshot = new LiveCombatEvidenceReader(client).read();
        assertEquals(7, snapshot.getSpellbookSelector());
        assertTrue(snapshot.getActivePrayers().contains(Prayer.PROTECT_FROM_MELEE));
        assertTrue(snapshot.isRigourUnlocked());
        assertTrue(snapshot.isAuguryUnlocked());
        assertTrue(snapshot.isPreserveUnlocked());
    }

    @Test
    public void transientLoggedInVarStateFailsClosed()
    {
        Client client = (Client) Proxy.newProxyInstance(
                Client.class.getClassLoader(), new Class<?>[]{Client.class},
                (proxy, method, args) ->
                {
                    if (method.getName().equals("getGameState"))
                        return GameState.LOGGED_IN;
                    if (method.getName().equals("getVarbitValue"))
                        throw new IllegalStateException("transient account switch");
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    return null;
                });
        assertNull(new LiveCombatEvidenceReader(client).read());
    }

    private static Client client(GameState state, int spellbook,
            boolean unlocked)
    {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) ->
                {
                    if (method.getName().equals("getGameState")) return state;
                    if (method.getName().equals("getVarbitValue"))
                    {
                        int id = (Integer) args[0];
                        if (id == VarbitID.SPELLBOOK) return spellbook;
                        if (id == Prayer.PROTECT_FROM_MELEE.getVarbit()) return 1;
                        return unlocked ? 1 : 0;
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    return null;
                });
    }
}
