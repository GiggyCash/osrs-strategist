package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One machine-readable unresolved quest field. */
@RequiredArgsConstructor
public final class QuestUncertaintyEntry
{
    public enum Category
    {
        ITEMS, ITEM_ALTERNATIVES, QUANTITIES, ACCESS, TRANSPORTATION,
        COMBAT, START_LOCATION, REWARDS, XP_REWARDS, IRREVERSIBLE_XP,
        UNLOCKS, QUEST_POINTS, OTHER
    }

    @Getter
    private final String questName;
    @Getter
    private final Category category;
    @Getter
    private final String detail;


}
