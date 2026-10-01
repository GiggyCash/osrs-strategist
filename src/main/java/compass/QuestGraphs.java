package compass;

import java.util.*;

/** Shared normalized, cycle-safe traversal over bundled quest prerequisites. */
final class QuestGraphs
{
    private QuestGraphs() { }

    static QuestStatus status(StrategyContext context, String quest)
    {
        if (context == null || context.data() == null
                || context.data().quests() == null) return QuestStatus.UNKNOWN;
        return context.data().quests().normalizedStatusOf(quest);
    }

    static List<String> path(QuestKnowledgeCatalog catalog,
            String root, String target)
    {
        return path(catalog, root, target, new HashSet<>());
    }

    private static List<String> path(QuestKnowledgeCatalog catalog,
            String current, String target, Set<String> active)
    {
        if (Names.words(current).equals(Names.words(target)))
            return new ArrayList<>(Collections.singletonList(current));
        var key = Names.words(current);
        if (!active.add(key)) return null;
        var definition = catalog.definitionFor(current);
        if (definition != null) for (String prerequisite : definition.prerequisites)
        {
            List<String> child = path(catalog, prerequisite, target, active);
            if (child != null)
            {
                child.add(0, current);
                active.remove(key);
                return child;
            }
        }
        active.remove(key);
        return null;
    }

    static void collect(QuestKnowledgeCatalog catalog, String quest,
            Set<String> result)
    {
        Set<String> seen = new HashSet<>();
        for (String existing : result) seen.add(Names.words(existing));
        collect(catalog, quest, result, seen);
    }

    private static void collect(QuestKnowledgeCatalog catalog, String quest,
            Set<String> result, Set<String> seen)
    {
        // Normalize each visited node once, rather than rescanning the entire
        // growing result for every prerequisite edge (including cycles).
        if (!seen.add(Names.words(quest))) return;
        result.add(quest);
        var definition = catalog.definitionFor(quest);
        if (definition != null)
            for (String prerequisite : definition.prerequisites)
                collect(catalog, prerequisite, result, seen);
    }
}
