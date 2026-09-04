package compass;

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
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        ImportedQuestItemRequirementCatalog imported =
                new ImportedQuestItemRequirementCatalog();
        totalQuests = Quest.values().length;
        for (Quest quest : Quest.values())
        {
            QuestDefinition record = catalog.definitionFor(quest.getName());
            if (record == null)
            {
                sourceMissingFields += 5;
                unresolved.add(new Unresolved(quest.getName(), "all",
                        "", "No matching authoritative quest bucket record."));
                continue;
            }
            for (String uncertainty : record.getFieldUncertainties())
                auditUncertainty(quest.getName(), uncertainty);
            if (record.getItemRequirements().isEmpty())
            {
                fullyExecutable++;
                continue;
            }
            questsWithItemRequirements++;
            ImportedQuestItemRequirementCatalog.Result result =
                    imported.resultFor(quest.getName());
            if (result == null)
            {
                unsupportedExpressions++;
                unresolved.add(new Unresolved(quest.getName(), "items",
                        record.getItemRequirements().stream()
                                .map(QuestDefinition.QuestItemRequirement::getName)
                                .collect(java.util.stream.Collectors.joining("; ")),
                        "No executable expression is bundled for this quest."));
                continue;
            }
            // A VALUE field may contain only explicitly optional preparation.
            // Once the parser proves every line non-mandatory, that is the same
            // executable outcome as source NONE: there is no ownership gate.
            int checks = countChecks(result.getExpression());
            explicitCheckExpressions += checks;
            if (result.getUnresolved().isEmpty() && checks == 0)
                fullyExecutable++;
            else if (result.getExpression() != null
                    || result.getUnresolved().isEmpty())
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

    private static int countChecks(ItemRule value)
    {
        if (value == null) return 0;
        int count = value.getKind() == ItemRule.Kind.CHECK_NEEDED ? 1 : 0;
        for (ItemRule child : value.getChildren()) count += countChecks(child);
        return count;
    }

    private void auditUncertainty(String quest, String field)
    {
        if (field == null || field.trim().isEmpty()) return;
        if (field.toLowerCase().contains("parse")) parseFailures++;
        if (field.toLowerCase().contains("missing")) sourceMissingFields++;
        unresolved.add(new Unresolved(quest, field, "",
                "The consolidated quest record keeps this field explicitly uncertain."));
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
