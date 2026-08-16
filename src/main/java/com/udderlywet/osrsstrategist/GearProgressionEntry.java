package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One contextual gear tier. Names stay data-driven until ownership is verified by item IDs. */
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

    public String getId() { return id; }
    public String getContextId() { return contextId; }
    public CombatStyle getStyle() { return style; }
    public GearBudgetTier getTier() { return tier; }
    public List<String> getRecommendedItems() { return recommendedItems; }
    public String getWeaponGuidance() { return weaponGuidance; }
    public String getNote() { return note; }
    public boolean isFreeToPlay() { return freeToPlay; }
    public boolean isSelfSourceFriendly() { return selfSourceFriendly; }
    public boolean isUimFriendly() { return uimFriendly; }
    public boolean isHardcoreSafe() { return hardcoreSafe; }
}
