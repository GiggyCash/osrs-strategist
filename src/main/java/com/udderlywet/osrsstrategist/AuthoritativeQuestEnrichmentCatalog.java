package com.udderlywet.osrsstrategist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Pinned Wiki quest-detail evidence. Runtime access is strictly local. */
public final class AuthoritativeQuestEnrichmentCatalog
{
    public static final String PROVENANCE =
            "OSRS Wiki quest bucket and quest reward sections retrieved 2026-08-19";
    private static final String RESOURCE = "/content/quest-enrichment.tsv";
    private final Map<String, Record> records;

    public AuthoritativeQuestEnrichmentCatalog()
    {
        records = load();
    }

    public Record recordFor(String name)
    {
        String key = normalize(name);
        String wikiName = aliases().get(key);
        return records.get(normalize(wikiName == null ? name : wikiName));
    }

    public Map<String, Record> all() { return records; }

    private static Map<String, Record> load()
    {
        Map<String, Record> result = new LinkedHashMap<>();
        InputStream stream = AuthoritativeQuestEnrichmentCatalog.class
                .getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException(
                "Missing local quest enrichment snapshot: " + RESOURCE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] columns = line.split("\t", -1);
                if (columns.length != 6) throw new IllegalStateException(
                        "Malformed quest enrichment row");
                Record record = new Record(unescape(columns[0]),
                        unescape(columns[1]), unescape(columns[2]),
                        unescape(columns[3]), unescape(columns[4]),
                        unescape(columns[5]));
                if (result.put(normalize(record.name), record) != null)
                    throw new IllegalStateException(
                            "Duplicate quest enrichment: " + record.name);
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to read " + RESOURCE, ex);
        }
        return Collections.unmodifiableMap(result);
    }

    private static String unescape(String value)
    {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++)
        {
            char current = value.charAt(index);
            if (escaped)
            {
                result.append(current == 'n' ? '\n' : current == 't' ? '\t' : current);
                escaped = false;
            }
            else if (current == '\\') escaped = true;
            else result.append(current);
        }
        if (escaped) result.append('\\');
        return result.toString();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static Map<String, String> aliases()
    {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(normalize("Recipe for Disaster - Another Cook's Quest"),
                "Recipe for Disaster/Another Cook's Quest");
        result.put(normalize("Recipe for Disaster - Mountain Dwarf"),
                "Recipe for Disaster/Freeing the Mountain Dwarf");
        result.put(normalize("Recipe for Disaster - Wartface & Bentnoze"),
                "Recipe for Disaster/Freeing the Goblin generals");
        result.put(normalize("Recipe for Disaster - Pirate Pete"),
                "Recipe for Disaster/Freeing Pirate Pete");
        result.put(normalize("Recipe for Disaster - Lumbridge Guide"),
                "Recipe for Disaster/Freeing the Lumbridge Guide");
        result.put(normalize("Recipe for Disaster - Evil Dave"),
                "Recipe for Disaster/Freeing Evil Dave");
        result.put(normalize("Recipe for Disaster - Skrach Uglogwee"),
                "Recipe for Disaster/Freeing Skrach Uglogwee");
        result.put(normalize("Recipe for Disaster - Sir Amik Varze"),
                "Recipe for Disaster/Freeing Sir Amik Varze");
        result.put(normalize("Recipe for Disaster - King Awowogei"),
                "Recipe for Disaster/Freeing King Awowogei");
        result.put(normalize("Recipe for Disaster - Culinaromancer"),
                "Recipe for Disaster/Defeating the Culinaromancer");
        return result;
    }

    public static final class Record
    {
        private final String name;
        private final String start;
        private final String requirements;
        private final String items;
        private final String enemies;
        private final String rewards;

        private Record(String name, String start, String requirements,
                String items, String enemies, String rewards)
        {
            this.name = name;
            this.start = start;
            this.requirements = requirements;
            this.items = items;
            this.enemies = enemies;
            this.rewards = rewards;
        }

        public String getName() { return name; }
        public String getStart() { return start; }
        public String getRequirements() { return requirements; }
        public String getItems() { return items; }
        public String getEnemies() { return enemies; }
        public String getRewards() { return rewards; }
        public boolean hasStartEvidence() { return !start.trim().isEmpty(); }
        // Bucket rows preserve explicitly blank Quest details parameters. For
        // these three fields a blank value is authoritative NONE, not UNKNOWN.
        public boolean hasRequirementEvidence() { return true; }
        public boolean hasItemEvidence() { return true; }
        public boolean hasCombatEvidence() { return true; }
        public boolean hasRewardEvidence() { return !rewards.trim().isEmpty(); }
    }
}
