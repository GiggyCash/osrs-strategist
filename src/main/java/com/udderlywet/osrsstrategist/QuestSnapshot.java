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
        return quests.getOrDefault(
                questName,
                QuestStatus.UNKNOWN
        );
    }

    public Map<String, QuestStatus> getQuests()
    {
        return quests;
    }
}
