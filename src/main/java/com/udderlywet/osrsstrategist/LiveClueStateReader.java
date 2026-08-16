package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;

/**
 * Detects owned clue scrolls from containers Strategist has actually observed.
 *
 * <p>A stale/unopened bank is never treated as proof that a clue disappeared.
 * UIM ignores normal bank state entirely. Challenge-scroll style intermediate
 * clue items preserve the previous clue observation rather than resetting it.</p>
 */
@Singleton
public class LiveClueStateReader
{
    public ClueSnapshot read(
            AccountMode mode,
            InventorySnapshot inventory,
            BankSnapshot bank,
            ClueSnapshot previous)
    {
        List<ItemStackSnapshot> visible = new ArrayList<>();
        if (inventory != null) visible.addAll(inventory.getItems());
        if (mode != AccountMode.ULTIMATE_IRONMAN && bank != null)
        {
            visible.addAll(bank.getItems());
        }

        ClueTier bestTier = ClueTier.UNKNOWN;
        boolean clueIntermediateObserved = false;
        for (ItemStackSnapshot item : visible)
        {
            String name = normalize(item.getName());
            if (name.isEmpty()) continue;

            if (isActualClueScroll(name))
            {
                ClueTier tier = ClueTier.fromText(name);
                if (tierPriority(tier) > tierPriority(bestTier))
                {
                    bestTier = tier;
                }
            }
            else if (isIntermediateClueItem(name))
            {
                clueIntermediateObserved = true;
            }
        }

        if (bestTier != ClueTier.UNKNOWN)
        {
            long firstSeen = System.currentTimeMillis();
            if (previous != null
                    && previous.isCluePresent()
                    && ClueTier.fromText(previous.getClueType()) == bestTier)
            {
                firstSeen = previous.getFirstSeenAtMillis();
            }
            return new ClueSnapshot(
                    true,
                    bestTier.name().toLowerCase(Locale.ROOT),
                    firstSeen,
                    RecommendationConfidence.VERIFIED
            );
        }

        if (clueIntermediateObserved && previous != null && previous.isCluePresent())
        {
            return previous;
        }

        // If the bank has not been observed, absence from inventory alone does
        // not prove a non-UIM clue was completed/dropped rather than banked.
        if (mode != AccountMode.ULTIMATE_IRONMAN
                && bank == null
                && previous != null
                && previous.isCluePresent())
        {
            return previous;
        }

        // UIM has no normal bank route. Once neither a clue scroll nor a known
        // intermediate clue item is present in inventory, this observation can
        // be cleared instead of waiting for impossible bank evidence.
        return null;
    }

    private static boolean isActualClueScroll(String name)
    {
        return name.startsWith("clue scroll (")
                || name.startsWith("clue scroll -")
                || name.equals("clue scroll");
    }

    private static boolean isIntermediateClueItem(String name)
    {
        return name.contains("challenge scroll")
                || name.contains("puzzle box")
                || name.contains("light box")
                || name.contains("strange device")
                || name.contains("hot/cold device");
    }

    private static int tierPriority(ClueTier tier)
    {
        switch (tier)
        {
            case MASTER: return 6;
            case ELITE: return 5;
            case HARD: return 4;
            case MEDIUM: return 3;
            case EASY: return 2;
            case BEGINNER: return 1;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
