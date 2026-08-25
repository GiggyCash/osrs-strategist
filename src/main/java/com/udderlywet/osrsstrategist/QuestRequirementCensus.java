package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Quest;

/** Machine-readable quality census over every current RuneLite quest identity. */
public final class QuestRequirementCensus
{
    public static final class Unresolved
    {
        private final String quest;
        private final String field;
        private final String rawEvidence;
        private final String reason;

        private Unresolved(String quest, String field, String rawEvidence,
                String reason)
        {
            this.quest = quest;
            this.field = field;
            this.rawEvidence = rawEvidence;
            this.reason = reason;
        }

        public String getQuest() { return quest; }
        public String getField() { return field; }
        public String getRawEvidence() { return rawEvidence; }
        public String getReason() { return reason; }
    }

    private final int totalQuests;
    private int questsWithItemRequirements;
    private int fullyExecutable;
    private int partiallyExecutable;
    private int rawOnly;
    private int unsupportedExpressions;
    private int explicitCheckExpressions;
    private int parseFailures;
    private int sourceMissingFields;
    private final List<Unresolved> unresolved = new ArrayList<>();
    private final List<String> rawOnlyQuests = new ArrayList<>();

    public QuestRequirementCensus()
    {
        AuthoritativeQuestEnrichmentCatalog catalog =
                new AuthoritativeQuestEnrichmentCatalog();
        QuestItemEvidenceParser parser = new QuestItemEvidenceParser();
        totalQuests = Quest.values().length;
        for (Quest quest : Quest.values())
        {
            AuthoritativeQuestEnrichmentCatalog.Record record =
                    catalog.recordFor(quest.getName());
            if (record == null)
            {
                sourceMissingFields += 5;
                unresolved.add(new Unresolved(quest.getName(), "all",
                        "", "No matching authoritative quest bucket record."));
                continue;
            }
            auditField(quest.getName(), "start", record.getStart(),
                    record.getStartState());
            auditField(quest.getName(), "requirements", record.getRequirements(),
                    record.getRequirementState());
            auditField(quest.getName(), "combat", record.getEnemies(),
                    record.getCombatState());
            auditField(quest.getName(), "rewards", record.getRewards(),
                    record.getRewardState());

            AuthoritativeQuestEnrichmentCatalog.EvidenceState itemState =
                    record.getItemState();
            auditField(quest.getName(), "items", record.getItems(), itemState);
            if (itemState == AuthoritativeQuestEnrichmentCatalog.EvidenceState.NONE
                    || itemState == AuthoritativeQuestEnrichmentCatalog.EvidenceState.NOT_APPLICABLE)
            {
                fullyExecutable++;
                continue;
            }
            if (itemState != AuthoritativeQuestEnrichmentCatalog.EvidenceState.VALUE)
                continue;
            questsWithItemRequirements++;
            QuestItemEvidenceParser.Result result;
            try
            {
                result = parser.parse(record.getItems());
            }
            catch (RuntimeException ex)
            {
                parseFailures++;
                unresolved.add(new Unresolved(quest.getName(), "items",
                        record.getItems(), "Parser exception: "
                                + ex.getClass().getSimpleName()));
                continue;
            }
            // A VALUE field may contain only explicitly optional preparation.
            // Once the parser proves every line non-mandatory, that is the same
            // executable outcome as source NONE: there is no ownership gate.
            explicitCheckExpressions += result.getCheckNeededExpressionCount();
            if (result.isDeterministicallyExecutable())
                fullyExecutable++;
            else if (result.getExpression() != null || result.isFullyExecutable())
                partiallyExecutable++;
            else
            {
                rawOnly++;
                rawOnlyQuests.add(quest.getName());
            }
            for (String raw : result.getUnresolved())
            {
                unsupportedExpressions++;
                unresolved.add(new Unresolved(quest.getName(), "items", raw,
                        "The current expression grammar cannot prove the alternatives, scope, consumption, or quantity safely."));
            }
        }
    }

    private void auditField(String quest, String field, String raw,
            AuthoritativeQuestEnrichmentCatalog.EvidenceState state)
    {
        if (state == AuthoritativeQuestEnrichmentCatalog.EvidenceState.SOURCE_MISSING
                || state == AuthoritativeQuestEnrichmentCatalog.EvidenceState.MISSING)
        {
            sourceMissingFields++;
            unresolved.add(new Unresolved(quest, field, raw,
                    "Authoritative source field is missing."));
        }
        else if (state == AuthoritativeQuestEnrichmentCatalog.EvidenceState.PARSE_FAILURE)
        {
            parseFailures++;
            unresolved.add(new Unresolved(quest, field, raw,
                    "Authoritative source parsing failed."));
        }
        else if (state == AuthoritativeQuestEnrichmentCatalog.EvidenceState.UNSUPPORTED_STRUCTURE)
            unresolved.add(new Unresolved(quest, field, raw,
                    "Authoritative page structure is not supported; no NONE inference was made."));
        else if (state == AuthoritativeQuestEnrichmentCatalog.EvidenceState.UNKNOWN)
            unresolved.add(new Unresolved(quest, field, raw,
                    "Evidence remains explicitly unknown."));
    }

    public int getTotalQuests() { return totalQuests; }
    public int getQuestsWithItemRequirements() { return questsWithItemRequirements; }
    public int getFullyExecutable() { return fullyExecutable; }
    public int getPartiallyExecutable() { return partiallyExecutable; }
    public int getRawOnly() { return rawOnly; }
    public int getUnsupportedExpressions() { return unsupportedExpressions; }
    public int getExplicitCheckExpressions() { return explicitCheckExpressions; }
    public int getParseFailures() { return parseFailures; }
    public int getSourceMissingFields() { return sourceMissingFields; }
    public List<Unresolved> getUnresolved()
    {
        return Collections.unmodifiableList(unresolved);
    }
    public List<String> getRawOnlyQuests()
    {
        return Collections.unmodifiableList(rawOnlyQuests);
    }
}
