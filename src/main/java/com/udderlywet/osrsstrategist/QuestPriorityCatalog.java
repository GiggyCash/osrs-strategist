package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** High-value quest weighting loaded from the bundled catalog. */
@Singleton
public class QuestPriorityCatalog
{
    private final Map<String, QuestPriority> priorities = new HashMap<>();

    public QuestPriorityCatalog()
    {
        for (QuestPriority priority : BundledCatalogLoader.array(
                Text.get(559), QuestPriority[].class))
            if (priorities.put(normalize(priority.getName()), priority) != null)
                throw new IllegalStateException("Duplicate quest priority " + priority.getName());
    }

    public QuestPriority priorityFor(String questName) { return priorities.get(normalize(questName)); }
    public Map<String, QuestPriority> snapshot() { return Collections.unmodifiableMap(priorities); }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('’', '\'')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static final class QuestPriority
    {
        private String name;
        private double scoreBonus;
        private String reason;
        public String getName() { return name; }
        public double getScoreBonus() { return scoreBonus; }
        public String getReason() { return reason; }
    }
}
