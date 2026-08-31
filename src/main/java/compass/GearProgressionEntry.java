package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** One contextual gear tier. Names stay data-driven until ownership is verified by item IDs. */
@RequiredArgsConstructor
@Getter
public final class GearProgressionEntry
{
    private final String id;
    private final String contextId;
    private final CombatStyle style;
    private final GearBudgetTier tier;
    private final List<String> recommendedItems;
    private final String weaponGuidance;
    private final String note;
    private final boolean freeToPlay;
    private final boolean selfSourceFriendly;
    private final boolean uimFriendly;
    private final boolean hardcoreSafe;


}
