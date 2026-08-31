package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Bridges quests, diaries, Combat Achievements, and collection-log state into
 * the shared reasoning pipeline.
 *
 * <p>This module intentionally does not invent exact tasks yet. It only emits
 * signals from snapshots that have actually been observed. Typed game-data
 * definitions will later turn these signals into exact prerequisite chains and
 * opportunistic recommendations.</p>
 */
@Singleton
public class ProgressionStrategyModule implements StrategyModule
{
    @Override
    public String getId()
    {
        return "progression";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        StrategyDataBundle data = context.getData();

        if (data == null)
        {
            return signals;
        }

        QuestSnapshot quests = data.getQuests();
        if (quests != null)
        {
            int complete = 0;
            int known = 0;

            for (Map.Entry<String, QuestStatus> entry
                    : quests.getQuests().entrySet())
            {
                if (entry.getValue() == QuestStatus.COMPLETE)
                {
                    complete++;
                    known++;
                }
                else if (entry.getValue() != QuestStatus.UNKNOWN)
                {
                    known++;
                }
            }

            if (known > 0)
            {
                signals.add(
                        new StrategySignal(
                                "progression:quests",
                                StrategySignalCategory.QUEST,
                                "Quest state observed: " + complete
                                        + " complete of " + known
                                        + " known entries",
                                questWeight(context.getQuestTolerance()),
                                RecommendationConfidence.VERIFIED
                        )
                );
            }
        }

        CombatAchievementSnapshot combatAchievements =
                data.getCombatAchievements();
        if (combatAchievements != null)
        {
            signals.add(
                    new StrategySignal(
                            "progression:combat-achievements",
                            StrategySignalCategory.COMBAT_ACHIEVEMENT,
                            "Combat Achievement points observed: "
                                    + combatAchievements.getEarnedPoints(),
                            1.0,
                            RecommendationConfidence.VERIFIED
                    )
            );
        }

        CollectionLogSnapshot collectionLog = data.getCollectionLog();
        if (collectionLog != null && context.isCollectionistMode())
        {
            signals.add(
                    new StrategySignal(
                            "progression:collection-log",
                            StrategySignalCategory.COLLECTION_LOG,
                            "Collectionist weighting enabled",
                            2.0,
                            RecommendationConfidence.VERIFIED
                    )
            );
        }

        return signals;
    }

    private static double questWeight(QuestTolerance tolerance)
    {
        switch (tolerance)
        {
            case HIGH:
                return 3.0;
            case LOW:
                return -1.0;
            case NORMAL:
            default:
                return 1.0;
        }
    }
}
