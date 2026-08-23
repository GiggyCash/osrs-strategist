package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Executable subset of the pinned authoritative quest item evidence.
 * Complex prose remains visible as explicit verification rather than guessed.
 */
public final class ImportedQuestItemRequirementCatalog
{
    private final Map<String, QuestItemEvidenceParser.Result> requirements;

    public ImportedQuestItemRequirementCatalog()
    {
        AuthoritativeQuestEnrichmentCatalog enrichment =
                new AuthoritativeQuestEnrichmentCatalog();
        QuestItemEvidenceParser parser = new QuestItemEvidenceParser();
        Map<String, QuestItemEvidenceParser.Result> result = new LinkedHashMap<>();
        for (AuthoritativeQuestRequirementCatalog.Record quest
                : new AuthoritativeQuestRequirementCatalog().all().values())
        {
            AuthoritativeQuestEnrichmentCatalog.Record details =
                    enrichment.recordFor(quest.getName());
            if (details == null || !details.hasItemEvidence()) continue;
            result.put(normalize(quest.getName()), parser.parse(details.getItems()));
        }
        requirements = Collections.unmodifiableMap(result);
    }

    public QuestItemEvidenceParser.Result resultFor(String questName)
    {
        return requirements.get(normalize(questName));
    }

    public int questCount() { return requirements.size(); }

    public long fullyExecutableCount()
    {
        return requirements.values().stream()
                .filter(QuestItemEvidenceParser.Result::isFullyExecutable).count();
    }

    public long partiallyExecutableCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() != null
                        && !result.isFullyExecutable()).count();
    }

    public long rawOnlyCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() == null
                        && !result.getUnresolved().isEmpty()).count();
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}
