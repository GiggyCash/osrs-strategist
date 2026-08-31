package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import lombok.Getter;

/** Pinned Wiki quest-detail evidence. Runtime access is strictly local. */
public final class AuthoritativeQuestEnrichmentCatalog
{
    public static final String PROVENANCE = get(39);
    private static final String RESOURCE = "/content/quest-enrichment.json";
    private final Map<String, Record> records;

    public AuthoritativeQuestEnrichmentCatalog()
    {
        Map<String, Record> values = new LinkedHashMap<>();
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            record.validate();
            if (values.put(normalize(record.name), record) != null)
                throw new IllegalStateException(get(1111) + record.name);
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
                {get(51), get(52)},
                {get(1112), get(53)},
                {get(54), get(40)},
                {get(1113), get(41)},
                {get(1114), get(42)},
                {get(1115), get(1116)},
                {get(1117), get(43)},
                {get(1118), get(44)},
                {get(1119), get(45)},
                {get(1120), get(46)},
                {"Vale Totems", get(1121)}
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
                throw new IllegalStateException(get(1122));
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
                throw new IllegalStateException(get(1123) + field);
        }
    }
}
