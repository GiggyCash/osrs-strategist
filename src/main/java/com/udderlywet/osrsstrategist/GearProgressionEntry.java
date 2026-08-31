package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** One contextual gear tier. Names stay data-driven until ownership is verified by item IDs. */
public final class GearProgressionEntry
{
    @Getter
    private final String id;
    @Getter
    private final String contextId;
    @Getter
    private final CombatStyle style;
    @Getter
    private final GearBudgetTier tier;
    @Getter
    private final List<String> recommendedItems;
    @Getter
    private final String weaponGuidance;
    @Getter
    private final String note;
    @Getter
    private final boolean freeToPlay;
    @Getter
    private final boolean selfSourceFriendly;
    @Getter
    private final boolean uimFriendly;
    @Getter
    private final boolean hardcoreSafe;

    public GearProgressionEntry(
            String id,
            String contextId,
            CombatStyle style,
            GearBudgetTier tier,
            List<String> recommendedItems,
            String weaponGuidance,
            String note,
            boolean freeToPlay,
            boolean selfSourceFriendly,
            boolean uimFriendly,
            boolean hardcoreSafe)
    {
        this.id = id;
        this.contextId = contextId;
        this.style = style;
        this.tier = tier;
        this.recommendedItems = Collections.unmodifiableList(
                recommendedItems == null ? new ArrayList<>()
                        : new ArrayList<>(recommendedItems));
        this.weaponGuidance = weaponGuidance;
        this.note = note;
        this.freeToPlay = freeToPlay;
        this.selfSourceFriendly = selfSourceFriendly;
        this.uimFriendly = uimFriendly;
        this.hardcoreSafe = hardcoreSafe;
    }

}
