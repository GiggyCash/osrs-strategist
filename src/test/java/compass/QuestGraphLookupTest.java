package compass;

import java.util.*;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestGraphLookupTest
{
    @Test
    public void normalizationRetainsPunctuationWhitespaceAndUnicodeRules()
    {
        assertEquals("cook s assistant", Names.words("  Cook\u2019s Assistant! "));
        assertEquals("quest 12", Names.words("QUEST_12\t\n"));
        assertEquals("", Names.words(null));
        assertEquals("", Names.words("\u00e9\u2603!"));
        Random random = new Random(17);
        for (int i = 0; i < 500; i++)
        {
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < 40; j++) value.append((char) random.nextInt(0x3000));
            String text = value.toString();
            String previous = text.trim().toLowerCase(Locale.ROOT).replace('\u2019', '\'')
                    .replaceAll("[^a-z0-9]+", " ").trim();
            assertEquals(previous, Names.words(text));
        }
    }

    @Test
    public void indexedStatusMatchesFormerScanForEveryLiveQuestAndAliases()
    {
        Map<String, QuestStatus> states = new HashMap<>();
        int i = 0;
        for (Quest quest : Quest.values())
            states.put(quest.getName(), QuestStatus.values()[i++ % QuestStatus.values().length]);
        QuestSnapshot snapshot = new QuestSnapshot(states);
        for (String name : states.keySet())
        {
            assertEquals(scan(snapshot, name), snapshot.normalizedStatusOf(name));
            String alias = "  " + name.toUpperCase(Locale.ROOT).replace('\'', '\u2019') + "! ";
            assertEquals(scan(snapshot, alias), snapshot.normalizedStatusOf(alias));
        }
        assertEquals(QuestStatus.UNKNOWN, snapshot.normalizedStatusOf("Unobserved quest"));
        assertEquals(QuestStatus.UNKNOWN, snapshot.normalizedStatusOf(null));
        assertEquals(QuestStatus.UNKNOWN, new QuestSnapshot(null).normalizedStatusOf("Quest"));
        assertEquals(QuestStatus.UNKNOWN, QuestGraphs.status(null, "Quest"));
    }

    @Test
    public void snapshotCopiesStatusesAndKeepsExactLookupAndCollisionSemantics()
    {
        Map<String, QuestStatus> states = new HashMap<>();
        states.put("Quest One", QuestStatus.COMPLETE);
        states.put("quest-one!", null);
        QuestSnapshot original = new QuestSnapshot(states);
        assertEquals(scan(original, "QUEST ONE"), original.normalizedStatusOf("QUEST ONE"));
        // Existing callers of the exact lookup keep their original contract.
        assertEquals(QuestStatus.UNKNOWN, original.statusOf("QUEST ONE"));
        states.clear();
        states.put("Quest One", QuestStatus.NOT_STARTED);
        QuestSnapshot changed = new QuestSnapshot(states);
        assertEquals(scan(original, "Quest One"), original.normalizedStatusOf("Quest One"));
        assertEquals(QuestStatus.NOT_STARTED, changed.normalizedStatusOf("quest one"));
        assertEquals(QuestStatus.COMPLETE, original.statusOf("Quest One"));
    }

    @Test
    public void collectedPrerequisitesMatchReferenceTraversalForEntireCatalog()
    {
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        for (QuestDefinition definition : catalog.all().values())
        {
            Set<String> expected = new LinkedHashSet<>();
            oldCollect(catalog, definition.getName(), expected);
            Set<String> actual = new LinkedHashSet<>();
            QuestGraphs.collect(catalog, definition.getName(), actual);
            assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
        }
    }

    @Test
    public void cyclesSharedPrerequisitesAndSeededAliasesRemainDeduplicated()
    {
        Map<String, QuestDefinition> definitions = new HashMap<>();
        definitions.put("root", quest("Root", "Left", "Right"));
        definitions.put("left", quest("Left", "Shared"));
        definitions.put("right", quest("Right", "SHARED!"));
        definitions.put("shared", quest("Shared", "ROOT!"));
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog()
        {
            @Override public QuestDefinition definitionFor(String name)
            { return definitions.get(Names.words(name)); }
        };
        Set<String> result = new LinkedHashSet<>();
        QuestGraphs.collect(catalog, "Root", result);
        assertEquals(Arrays.asList("Root", "Left", "Shared", "Right"), new ArrayList<>(result));
        Set<String> seeded = new LinkedHashSet<>(Collections.singleton("LEFT!"));
        Set<String> expected = new LinkedHashSet<>(seeded);
        oldCollect(catalog, "Root", expected);
        QuestGraphs.collect(catalog, "Root", seeded);
        assertEquals(new ArrayList<>(expected), new ArrayList<>(seeded));
    }

    @Test
    public void startedPrerequisitesRemainVisibleToGraphTraversal()
    {
        Map<String, QuestDefinition> definitions = new HashMap<>();
        definitions.put("root", startedQuest("Root", "Starter"));
        definitions.put("starter", quest("Starter"));
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog()
        {
            @Override public QuestDefinition definitionFor(String name)
            { return definitions.get(Names.words(name)); }
        };

        assertEquals(Arrays.asList("Root", "Starter"),
                QuestGraphs.path(catalog, "Root", "Starter"));
        Set<String> collected = new LinkedHashSet<>();
        QuestGraphs.collect(catalog, "Root", collected);
        assertEquals(Arrays.asList("Root", "Starter"),
                new ArrayList<>(collected));
    }

    private static QuestStatus scan(QuestSnapshot snapshot, String name)
    {
        for (Map.Entry<String, QuestStatus> entry : snapshot.quests().entrySet())
            if (Names.words(entry.getKey()).equals(Names.words(name))) return entry.getValue();
        return QuestStatus.UNKNOWN;
    }

    private static void oldCollect(QuestKnowledgeCatalog catalog, String name, Set<String> result)
    {
        for (String seen : result) if (Names.words(seen).equals(Names.words(name))) return;
        result.add(name);
        QuestDefinition definition = catalog.definitionFor(name);
        if (definition != null)
        {
            for (String prerequisite : definition.prerequisites)
                oldCollect(catalog, prerequisite, result);
            for (String prerequisite : definition.getStartedPrerequisites())
                oldCollect(catalog, prerequisite, result);
        }
    }

    private static QuestDefinition quest(String name, String... prerequisites)
    {
        return new QuestDefinition(name, false, Arrays.asList(prerequisites),
                Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), null, 0, 0,
                Collections.emptyList(), "Test", Collections.emptyList(),
                Collections.emptyMap(), Collections.emptyList());
    }

    private static QuestDefinition startedQuest(String name,
            String... startedPrerequisites)
    {
        return new QuestDefinition(name, false, Collections.emptyList(),
                Arrays.asList(startedPrerequisites), Collections.emptyMap(),
                Collections.emptyList(), null, 0, 0, Collections.emptyList(),
                "Test", Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList());
    }
}
