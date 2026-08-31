package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;

/**
 * Converts RuneLite's live quest state into Compass's immutable snapshot.
 *
 * <p>Quest state is direct evidence. Multiple container/stat events can fire in
 * one game tick, so the complete quest scan is cached for that tick instead of
 * repeating the same reads unnecessarily.</p>
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LiveQuestStateReader
{
    private final Client client;
    private int cachedTick = -1;
    private QuestSnapshot cachedSnapshot;

    public QuestSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cachedSnapshot = null;
            return null;
        }

        var tick = client.getTickCount();
        if (cachedSnapshot != null && cachedTick == tick)
        {
            return cachedSnapshot;
        }

        Map<String, QuestStatus> states = new HashMap<>();

        for (Quest quest : Quest.values())
        {
            var state = quest.getState(client);
            states.put(quest.getName(), convert(state));
        }

        cachedSnapshot = new QuestSnapshot(states);
        cachedTick = tick;
        return cachedSnapshot;
    }

    private QuestStatus convert(QuestState state)
    {
        if (state == QuestState.FINISHED)
        {
            return QuestStatus.COMPLETE;
        }
        if (state == QuestState.IN_PROGRESS)
        {
            return QuestStatus.IN_PROGRESS;
        }
        if (state == QuestState.NOT_STARTED)
        {
            return QuestStatus.NOT_STARTED;
        }
        return QuestStatus.UNKNOWN;
    }
}
