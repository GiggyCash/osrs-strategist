package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RecommendationHistoryDocument
{
    private final int schemaVersion;
    private final List<RecommendationHistoryEntry> entries;

    RecommendationHistoryDocument(List<RecommendationHistoryEntry> entries)
    {
        this.schemaVersion = 1;
        this.entries = entries == null
                ? Collections.emptyList()
                : new ArrayList<>(entries);
    }

    int getSchemaVersion() { return schemaVersion; }
    List<RecommendationHistoryEntry> getEntries()
    {
        return entries == null ? Collections.emptyList() : entries;
    }
}
