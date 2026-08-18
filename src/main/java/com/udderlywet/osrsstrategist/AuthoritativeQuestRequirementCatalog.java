package com.udderlywet.osrsstrategist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Pinned development-time import of the Wiki's maintained direct quest and
 * skill requirement module. Runtime remains local and never contacts the Wiki.
 */
public final class AuthoritativeQuestRequirementCatalog
{
    public static final String PROVENANCE =
            "OSRS Wiki Module:Questreq/data retrieved 2026-08-18; RuneLite Quest 1.12.35 identity filter";
    private static final String RESOURCE = "/content/quest-requirements.tsv";

    private final Map<String, Record> records;

    public AuthoritativeQuestRequirementCatalog()
    {
        this.records = load();
    }

    public Map<String, Record> all() { return records; }

    private static Map<String, Record> load()
    {
        Map<String, String> runeLiteNames = new LinkedHashMap<>();
        for (Quest quest : Quest.values())
            runeLiteNames.put(normalize(quest.getName()), quest.getName());
        Map<String, String> aliases = aliases();

        Map<String, Record> result = new LinkedHashMap<>();
        InputStream stream = AuthoritativeQuestRequirementCatalog.class
                .getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException(
                "Missing local quest requirement snapshot: " + RESOURCE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] columns = line.split("\\t", -1);
                if (columns.length < 1 || columns.length > 4) throw new IllegalStateException(
                        "Malformed quest requirement row: " + line);
                String canonical = canonical(columns[0], runeLiteNames, aliases);
                if (canonical == null) continue;
                Record record = parse(canonical,
                        columns.length > 1 ? columns[1] : "",
                        columns.length > 2 ? columns[2] : "",
                        columns.length == 4 ? columns[3] : "",
                        runeLiteNames, aliases);
                if (result.put(normalize(canonical), record) != null)
                    throw new IllegalStateException(
                            "Duplicate imported quest requirement: " + canonical);
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to read " + RESOURCE, ex);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Record parse(String name, String quests, String skills,
            String startLocation,
            Map<String, String> runeLiteNames, Map<String, String> aliases)
    {
        List<String> prerequisites = new ArrayList<>();
        for (String prerequisite : split(quests))
        {
            String canonical = canonical(prerequisite, runeLiteNames, aliases);
            prerequisites.add(canonical == null
                    ? prerequisite.replaceFirst("^Started:", "") : canonical);
        }
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        List<String> otherChecks = new ArrayList<>();
        int questPoints = 0;
        for (String token : split(skills))
        {
            int separator = token.lastIndexOf(':');
            if (separator < 1) continue;
            String label = token.substring(0, separator);
            int level = Integer.parseInt(token.substring(separator + 1));
            if ("Quest point".equals(label)) questPoints = level;
            else
            {
                Skill skill = skill(label);
                if (skill == null)
                    otherChecks.add("Verify " + level + " " + label);
                else
                    levels.merge(skill, level, Math::max);
            }
        }
        return new Record(name, prerequisites, levels, questPoints, otherChecks,
                startLocation);
    }

    private static Skill skill(String name)
    {
        if (name == null) return null;
        String key = name.toUpperCase(Locale.ROOT).replace(' ', '_');
        try { return Skill.valueOf(key); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private static List<String> split(String value)
    {
        if (value == null || value.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String token : value.split(";"))
            if (!token.trim().isEmpty()) result.add(token.trim());
        return Collections.unmodifiableList(result);
    }

    private static String canonical(String value,
            Map<String, String> runeLiteNames, Map<String, String> aliases)
    {
        if (value == null) return null;
        String withoutStarted = value.replaceFirst("^Started:", "");
        String aliased = aliases.getOrDefault(normalize(withoutStarted),
                withoutStarted);
        return runeLiteNames.get(normalize(aliased));
    }

    private static Map<String, String> aliases()
    {
        Map<String, String> result = new LinkedHashMap<>();
        alias(result, "Recipe for Disaster/Another Cook's Quest",
                "Recipe for Disaster - Another Cook's Quest");
        alias(result, "Recipe for Disaster/Freeing the Mountain Dwarf",
                "Recipe for Disaster - Mountain Dwarf");
        alias(result, "Recipe for Disaster/Freeing the Goblin generals",
                "Recipe for Disaster - Wartface & Bentnoze");
        alias(result, "Recipe for Disaster/Freeing Pirate Pete",
                "Recipe for Disaster - Pirate Pete");
        alias(result, "Recipe for Disaster/Freeing the Lumbridge Guide",
                "Recipe for Disaster - Lumbridge Guide");
        alias(result, "Recipe for Disaster/Freeing Evil Dave",
                "Recipe for Disaster - Evil Dave");
        alias(result, "Recipe for Disaster/Freeing Skrach Uglogwee",
                "Recipe for Disaster - Skrach Uglogwee");
        alias(result, "Recipe for Disaster/Freeing Sir Amik Varze",
                "Recipe for Disaster - Sir Amik Varze");
        alias(result, "Recipe for Disaster/Freeing King Awowogei",
                "Recipe for Disaster - King Awowogei");
        alias(result, "Recipe for Disaster/Defeating the Culinaromancer",
                "Recipe for Disaster - Culinaromancer");
        return Collections.unmodifiableMap(result);
    }

    private static void alias(Map<String, String> target, String source,
            String canonical)
    {
        target.put(normalize(source), canonical);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static final class Record
    {
        private final String name;
        private final List<String> prerequisites;
        private final Map<Skill, Integer> skills;
        private final int questPoints;
        private final List<String> otherChecks;
        private final String startLocation;

        private Record(String name, List<String> prerequisites,
                Map<Skill, Integer> skills, int questPoints,
                List<String> otherChecks, String startLocation)
        {
            this.name = name;
            this.prerequisites = prerequisites;
            this.skills = Collections.unmodifiableMap(new EnumMap<>(skills));
            this.questPoints = questPoints;
            this.otherChecks = Collections.unmodifiableList(
                    new ArrayList<>(otherChecks));
            this.startLocation = startLocation == null ? "" : startLocation;
        }

        public String getName() { return name; }
        public List<String> getPrerequisites() { return prerequisites; }
        public Map<Skill, Integer> getSkills() { return skills; }
        public int getQuestPoints() { return questPoints; }
        public List<String> getOtherChecks() { return otherChecks; }
        public String getStartLocation() { return startLocation; }
    }
}
