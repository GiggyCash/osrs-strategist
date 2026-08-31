package com.udderlywet.osrsstrategist;

import java.util.*;
import lombok.Getter;

/** Pinned Wiki quest-detail evidence. Runtime access is strictly local. */
public final class AuthoritativeQuestEnrichmentCatalog
{
    public static final String PROVENANCE = PlayerText.get("AQEC1");
    private static final String RESOURCE = "/content/quest-enrichment.json";
    private final Map<String, Record> records;

    public AuthoritativeQuestEnrichmentCatalog()
    {
        Map<String, Record> values = new LinkedHashMap<>();
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            record.validate();
            if (values.put(normalize(record.name), record) != null)
                throw new IllegalStateException("Duplicate quest enrichment: " + record.name);
        }
        records = Collections.unmodifiableMap(values);
    }

    public Record recordFor(String name)
    {
        String wikiName = aliases().get(normalize(name));
        return records.get(normalize(wikiName == null ? name : wikiName));
    }
    public Map<String, Record> all() { return records; }
    public boolean hasStrictFieldEvidence()
    {
        for (Record record : records.values())
            if (record.hasLegacyEvidence()) return false;
        return true;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
    private static Map<String, String> aliases()
    {
        Map<String, String> result = new HashMap<>();
        String[][] values = {
                {PlayerText.get("AQEC6"), PlayerText.get("AQEC7")},
                {"Recipe for Disaster - Mountain Dwarf", PlayerText.get("AQEC8")},
                {PlayerText.get("AQEC9"), PlayerText.get("AQEC10")},
                {"Recipe for Disaster - Pirate Pete", PlayerText.get("AQEC11")},
                {"Recipe for Disaster - Lumbridge Guide", PlayerText.get("AQEC12")},
                {"Recipe for Disaster - Evil Dave", "Recipe for Disaster/Freeing Evil Dave"},
                {"Recipe for Disaster - Skrach Uglogwee", PlayerText.get("AQEC13")},
                {"Recipe for Disaster - Sir Amik Varze", PlayerText.get("AQEC14")},
                {"Recipe for Disaster - King Awowogei", PlayerText.get("AQEC15")},
                {"Recipe for Disaster - Culinaromancer", PlayerText.get("AQEC16")},
                {"Vale Totems", "Vale Totems (miniquest)"}
        };
        for (String[] alias : values) result.put(normalize(alias[0]), alias[1]);
        return result;
    }

    public enum EvidenceState
    {
        VALUE, NONE, NOT_APPLICABLE, SOURCE_MISSING, MISSING, PARSE_FAILURE,
        UNSUPPORTED_STRUCTURE, UNKNOWN, LEGACY_NONE;
        public boolean isEvidence()
        {
            return this == VALUE || this == NONE || this == NOT_APPLICABLE
                    || this == LEGACY_NONE;
        }
        public boolean isStrictEvidence()
        {
            return this == VALUE || this == NONE || this == NOT_APPLICABLE;
        }
    }

    @Getter
    public static final class Record
    {
        private String name;
        private String start;
        private EvidenceState startState;
        private String requirements;
        private EvidenceState requirementsState;
        private String items;
        private EvidenceState itemsState;
        private String enemies;
        private EvidenceState combatState;
        private String rewards;
        private EvidenceState rewardsState;

        public EvidenceState getRequirementState() { return requirementsState; }
        public EvidenceState getItemState() { return itemsState; }
        public EvidenceState getRewardState() { return rewardsState; }
        public boolean hasStartEvidence() { return startState.isEvidence(); }
        public boolean hasRequirementEvidence() { return requirementsState.isEvidence(); }
        public boolean hasItemEvidence() { return itemsState.isEvidence(); }
        public boolean hasCombatEvidence() { return combatState.isEvidence(); }
        public boolean hasRewardEvidence() { return rewardsState.isEvidence(); }
        public boolean hasStrictItemEvidence() { return itemsState.isStrictEvidence(); }
        public boolean hasStrictRequirementEvidence() { return requirementsState.isStrictEvidence(); }
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
        private void validate()
        {
            if (name == null || name.trim().isEmpty())
                throw new IllegalStateException("Blank quest enrichment identity");
            validate(start, startState, "start");
            validate(requirements, requirementsState, "requirements");
            validate(items, itemsState, "items");
            validate(enemies, combatState, "combat");
            validate(rewards, rewardsState, "rewards");
        }
        private static void validate(String value, EvidenceState state, String field)
        {
            if (state == null || state == EvidenceState.LEGACY_NONE)
                throw new IllegalStateException("Invalid " + field + " evidence state");
            boolean blank = value == null || value.trim().isEmpty();
            if ((state == EvidenceState.VALUE) == blank
                    || ((state == EvidenceState.NONE || state == EvidenceState.NOT_APPLICABLE
                    || state == EvidenceState.SOURCE_MISSING || state == EvidenceState.PARSE_FAILURE
                    || state == EvidenceState.UNSUPPORTED_STRUCTURE) && !blank))
                throw new IllegalStateException("Value/state mismatch for " + field);
        }
    }
}
