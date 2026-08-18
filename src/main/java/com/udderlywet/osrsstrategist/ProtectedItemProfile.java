package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-character list of items Compass must never suggest selling, dropping,
 * alching, destroying, or otherwise consuming as an "obsolete" resource.
 *
 * <p>Built-in protection rules for quest/rare/progression items will eventually
 * be layered on top of this explicit player list.</p>
 */
public final class ProtectedItemProfile
{
    private final Set<Integer> protectedItemIds = new HashSet<>();

    public boolean isProtected(int itemId)
    {
        return protectedItemIds.contains(itemId);
    }

    public void protect(int itemId)
    {
        if (itemId >= 0)
        {
            protectedItemIds.add(itemId);
        }
    }

    public void unprotect(int itemId)
    {
        protectedItemIds.remove(itemId);
    }

    public void replaceAll(Set<Integer> itemIds)
    {
        protectedItemIds.clear();

        if (itemIds == null)
        {
            return;
        }

        for (Integer itemId : itemIds)
        {
            if (itemId != null && itemId >= 0)
            {
                protectedItemIds.add(itemId);
            }
        }
    }

    public Set<Integer> snapshot()
    {
        return Collections.unmodifiableSet(
                new HashSet<>(protectedItemIds)
        );
    }
}
