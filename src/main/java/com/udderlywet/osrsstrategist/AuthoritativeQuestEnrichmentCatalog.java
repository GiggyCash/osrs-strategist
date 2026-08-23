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

    /**
     * True only when every row came from the evidence-aware snapshot schema.
     * Legacy snapshots remain readable so an update can be staged safely, but
     * callers can distinguish their inferred blank fields from verified NONE.
     */
    public boolean hasStrictFieldEvidence()
    {
        for (Record record : records.values())
            if (record.hasLegacyEvidence()) return false;
        return true;
    }

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
            int lineNumber = 0;
            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] columns = line.split("\\t", -1);
                Record record;
                if (columns.length == 6)
                    record = legacyRecord(columns);
                else if (columns.length == 11)
                    record = evidenceAwareRecord(columns, lineNumber);
                else
                    throw new IllegalStateException("Malformed quest enrichment row at line "
                            + lineNumber + ": expected 6 or 11 columns, found "
                            + columns.length);
                if (record.name.trim().isEmpty())
                    throw new IllegalStateException(
                            "Quest enrichment row has blank name at line " + lineNumber);
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

    private static Record legacyRecord(String[] columns)
    {
        String start = unescape(columns[1]);
        String requirements = unescape(columns[2]);
        String items = unescape(columns[3]);
        String enemies = unescape(columns[4]);
        String rewards = unescape(columns[5]);
        return new Record(unescape(columns[0]), start,
                legacyState(start, false), requirements,
                legacyState(requirements, true), items,
                legacyState(items, true), enemies,
                legacyState(enemies, true), rewards,
                legacyState(rewards, false));
    }

    private static Record evidenceAwareRecord(String[] columns, int lineNumber)
    {
        String name = unescape(columns[0]);
        String start = unescape(columns[1]);
        EvidenceState startState = parseState(columns[2], lineNumber, "start");
        String requirements = unescape(columns[3]);
        EvidenceState requirementsState = parseState(columns[4], lineNumber,
                "requirements");
        String items = unescape(columns[5]);
        EvidenceState itemsState = parseState(columns[6], lineNumber, "items");
        String enemies = unescape(columns[7]);
        EvidenceState combatState = parseState(columns[8], lineNumber, "combat");
        String rewards = unescape(columns[9]);
        EvidenceState rewardsState = parseState(columns[10], lineNumber, "rewards");

        validateState(start, startState, lineNumber, "start");
        validateState(requirements, requirementsState, lineNumber, "requirements");
        validateState(items, itemsState, lineNumber, "items");
        validateState(enemies, combatState, lineNumber, "combat");
        validateState(rewards, rewardsState, lineNumber, "rewards");

        return new Record(name, start, startState, requirements,
                requirementsState, items, itemsState, enemies, combatState,
                rewards, rewardsState);
    }

    private static EvidenceState legacyState(String value,
            boolean blankWasPreviouslyTreatedAsNone)
    {
        if (value != null && !value.trim().isEmpty()) return EvidenceState.VALUE;
        return blankWasPreviouslyTreatedAsNone
                ? EvidenceState.LEGACY_NONE : EvidenceState.MISSING;
    }

    private static EvidenceState parseState(String raw, int lineNumber,
            String field)
    {
        try
        {
            return EvidenceState.valueOf(unescape(raw).trim()
                    .toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalStateException("Invalid " + field
                    + " evidence state at line " + lineNumber + ": " + raw, ex);
        }
    }

    private static void validateState(String value, EvidenceState state,
            int lineNumber, String field)
    {
        boolean blank = value == null || value.trim().isEmpty();
        if (state == EvidenceState.VALUE && blank)
            throw new IllegalStateException("Blank " + field
                    + " marked VALUE at line " + lineNumber);
        if (state == EvidenceState.NONE && !blank)
            throw new IllegalStateException("Non-blank " + field
                    + " marked NONE at line " + lineNumber);
        if ((state == EvidenceState.MISSING || state == EvidenceState.PARSE_FAILURE)
                && !blank)
            throw new IllegalStateException("Non-blank " + field + " marked "
                    + state + " at line " + lineNumber);
        if (state == EvidenceState.LEGACY_NONE)
            throw new IllegalStateException("LEGACY_NONE is reserved for six-column snapshots");
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

    public enum EvidenceState
    {
        VALUE,
        NONE,
        MISSING,
        PARSE_FAILURE,
        LEGACY_NONE;

        public boolean isEvidence()
        {
            return this == VALUE || this == NONE || this == LEGACY_NONE;
        }

        public boolean isStrictEvidence()
        {
            return this == VALUE || this == NONE;
        }
    }

    public static final class Record
    {
        private final String name;
        private final String start;
        private final EvidenceState startState;
        private final String requirements;
        private final EvidenceState requirementsState;
        private final String items;
        private final EvidenceState itemsState;
        private final String enemies;
        private final EvidenceState combatState;
        private final String rewards;
        private final EvidenceState rewardsState;

        private Record(String name, String start, EvidenceState startState,
                String requirements, EvidenceState requirementsState,
                String items, EvidenceState itemsState, String enemies,
                EvidenceState combatState, String rewards,
                EvidenceState rewardsState)
        {
            this.name = name;
            this.start = start;
            this.startState = startState;
            this.requirements = requirements;
            this.requirementsState = requirementsState;
            this.items = items;
            this.itemsState = itemsState;
            this.enemies = enemies;
            this.combatState = combatState;
            this.rewards = rewards;
            this.rewardsState = rewardsState;
        }

        public String getName() { return name; }
        public String getStart() { return start; }
        public String getRequirements() { return requirements; }
        public String getItems() { return items; }
        public String getEnemies() { return enemies; }
        public String getRewards() { return rewards; }
        public EvidenceState getStartState() { return startState; }
        public EvidenceState getRequirementState() { return requirementsState; }
        public EvidenceState getItemState() { return itemsState; }
        public EvidenceState getCombatState() { return combatState; }
        public EvidenceState getRewardState() { return rewardsState; }
        public boolean hasStartEvidence() { return startState.isEvidence(); }
        public boolean hasRequirementEvidence() { return requirementsState.isEvidence(); }
        public boolean hasItemEvidence() { return itemsState.isEvidence(); }
        public boolean hasCombatEvidence() { return combatState.isEvidence(); }
        public boolean hasRewardEvidence() { return rewardsState.isEvidence(); }
        public boolean hasStrictItemEvidence() { return itemsState.isStrictEvidence(); }
        public boolean hasStrictRequirementEvidence()
        {
            return requirementsState.isStrictEvidence();
        }
        public boolean hasStrictCombatEvidence() { return combatState.isStrictEvidence(); }
        public boolean hasStrictRewardEvidence() { return rewardsState.isStrictEvidence(); }
        public boolean hasLegacyEvidence()
        {
            return startState == EvidenceState.LEGACY_NONE
                    || requirementsState == EvidenceState.LEGACY_NONE
                    || itemsState == EvidenceState.LEGACY_NONE
                    || combatState == EvidenceState.LEGACY_NONE
                    || rewardsState == EvidenceState.LEGACY_NONE;
        }
    }
}
