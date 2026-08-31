package com.udderlywet.osrsstrategist;

import java.util.*;
import lombok.Getter;
import net.runelite.api.Skill;

/** Canonical direct quest and skill requirements from the pinned Wiki import. */
public final class AuthoritativeQuestRequirementCatalog
{
    public static final String PROVENANCE = Text.get(55);
    private static final String RESOURCE = "/content/quest-requirements.json";
    private final Map<String, Record> records;

    public AuthoritativeQuestRequirementCatalog()
    {
        Map<String, Record> values = new LinkedHashMap<>();
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            record.freeze();
            String key = normalize(record.name);
            if (key.isEmpty() || values.put(key, record) != null)
                throw new IllegalStateException("Invalid or duplicate quest requirement: " + record.name);
        }
        records = Collections.unmodifiableMap(values);
    }

    public Map<String, Record> all() { return records; }
    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }

    @Getter
    public static final class Record
    {
        private String name;
        private List<String> prerequisites;
        private Map<Skill, Integer> skills;
        private int questPoints;
        private List<String> otherChecks;
        private String startLocation;

        private void freeze()
        {
            prerequisites = immutable(prerequisites);
            otherChecks = immutable(otherChecks);
            EnumMap<Skill, Integer> skillCopy = new EnumMap<>(Skill.class);
            if (skills != null) skillCopy.putAll(skills);
            skills = Collections.unmodifiableMap(skillCopy);
            startLocation = startLocation == null ? "" : startLocation;
        }
        private static List<String> immutable(List<String> values)
        {
            return values == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}
