package com.udderlywet.osrsstrategist;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

/**
 * Converts RuneLite's live quest state into Strategist's immutable snapshot.
 *
 * <p>Quest state is direct evidence. This lets requirement evaluators prove
 * area/content access instead of leaving everything at Check Needed.</p>
 */
@Singleton
public class LiveQuestStateReader
{
    private final Client client;

    @Inject
    public LiveQuestStateReader(Client client)
    {
        this.client = client;
    }

    public QuestSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        Map<String, QuestStatus> states = new HashMap<>();

        for (Quest quest : Quest.values())
        {
            QuestState state = quest.getState(client);
            states.put(quest.getName(), convert(state));
        }

        return new QuestSnapshot(states);
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
