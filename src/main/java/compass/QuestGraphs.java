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
        var key = Names.words(quest);
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
            if (Names.words(entry.getKey()).equals(key)) return entry.getValue();
        return QuestStatus.UNKNOWN;
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
        if (definition != null) for (String prerequisite : definition.getPrerequisites())
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
        for (String seen : result)
            if (Names.words(seen).equals(Names.words(quest))) return;
        result.add(quest);
        var definition = catalog.definitionFor(quest);
        if (definition != null)
            for (String prerequisite : definition.getPrerequisites())
                collect(catalog, prerequisite, result);
    }
}
