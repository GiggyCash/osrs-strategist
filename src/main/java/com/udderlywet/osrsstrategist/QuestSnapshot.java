package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class QuestSnapshot
{
    private final Map<String, QuestStatus> quests;

    public QuestSnapshot(Map<String, QuestStatus> quests)
    {
        this.quests = Collections.unmodifiableMap(
                new HashMap<>(quests)
        );
    }

    public QuestStatus statusOf(String questName)
    {
        if (questName == null) return QuestStatus.UNKNOWN;
        QuestStatus exact = quests.get(questName);
        if (exact != null) return exact;

        // RuneLite supplies canonical display names, but static knowledge can
        // arrive from validated sources with harmless case differences.
        for (Map.Entry<String, QuestStatus> entry : quests.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(questName))
            {
                return entry.getValue();
            }
        }
        return QuestStatus.UNKNOWN;
    }

    public boolean isComplete(String questName)
    {
        return statusOf(questName) == QuestStatus.COMPLETE;
    }

    public Map<String, QuestStatus> getQuests()
    {
        return quests;
    }
}
