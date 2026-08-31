package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Quest;

/** Coverage census pinned to RuneLite's maintained quest identity enumeration. */
@Singleton
public class QuestCoverageManifest
{
    public static final String PROVENANCE = PlayerText.get("QCM1");
    private static final Set<String> MINIQUESTS = normalizedSet(
            "Alfred Grimhand's Barcrawl", "Bear Your Soul", "Curse of the Empty Lord",
            "Daddy's Home", "The Enchanted Key", "Enter the Abyss", "Family Pest",
            "The General's Shadow", "Hopespear's Will", "In Search of Knowledge",
            "Into the Tombs", "Lair of Tarn Razorlor", "Mage Arena I", "Mage Arena II",
            "Skippy and the Mogres", "The Frozen Door", "His Faithful Servants",
            "Barbarian Training", "Vale Totems");

    private final List<ContentCoverageEntry> entries;

    public QuestCoverageManifest()
    {
        QuestKnowledgeCatalog knowledge = new QuestKnowledgeCatalog();
        List<ContentCoverageEntry> values = new ArrayList<>();
        for (Quest quest : Quest.values())
        {
            QuestDefinition definition = knowledge.definitionFor(quest.getName());
            boolean structured = definition != null;
            String kind = isMiniquest(quest.getName()) ? "miniquest" : "quest/activity";
            values.add(new ContentCoverageEntry(quest.name(), quest.getName(),
                    structured ? ContentCoverageState.STRUCTURED
                            : ContentCoverageState.CONSERVATIVE_FAIL_CLOSED,
                    structured
                            ? (definition.hasFieldUncertainty()
                                    ? PlayerText.get("QCM2")
                                    : PlayerText.get("QCM3"))
                            : "This " + kind + PlayerText.get("QCM4"),
                    PROVENANCE));
        }
        entries = Collections.unmodifiableList(values);
    }

    public List<ContentCoverageEntry> all() { return entries; }

    public boolean isMiniquest(String name)
    {
        return MINIQUESTS.contains(normalize(name));
    }

    public int miniquestCount()
    {
        int result = 0;
        for (ContentCoverageEntry entry : entries)
            if (isMiniquest(entry.getName())) result++;
        return result;
    }

    private static Set<String> normalizedSet(String... names)
    {
        Set<String> result = new HashSet<>();
        for (String name : Arrays.asList(names)) result.add(normalize(name));
        return Collections.unmodifiableSet(result);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}
