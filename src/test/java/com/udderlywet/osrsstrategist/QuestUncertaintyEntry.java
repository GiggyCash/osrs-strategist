package com.udderlywet.osrsstrategist;


/** One machine-readable unresolved quest field. */
public final class QuestUncertaintyEntry
{
    public enum Category
    {
        ITEMS, ITEM_ALTERNATIVES, QUANTITIES, ACCESS, TRANSPORTATION,
        COMBAT, START_LOCATION, REWARDS, XP_REWARDS, IRREVERSIBLE_XP,
        UNLOCKS, QUEST_POINTS, OTHER
    }

    private final String questName;
    private final Category category;
    private final String detail;

    public QuestUncertaintyEntry(String questName, Category category, String detail)
    {
        this.questName = questName;
        this.category = category;
        this.detail = detail;
    }

    public String getQuestName()
    {
        return questName;
    }

    public Category getCategory()
    {
        return category;
    }

    public String getDetail()
    {
        return detail;
    }

}
