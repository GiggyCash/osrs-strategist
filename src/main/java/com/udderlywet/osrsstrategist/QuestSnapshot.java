package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;
import lombok.experimental.Accessors;

public final class QuestSnapshot
{
    @Getter
    @Accessors(fluent = true)
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

}
