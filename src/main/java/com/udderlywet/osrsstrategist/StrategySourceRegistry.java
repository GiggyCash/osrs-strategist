package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/** Local provenance index loaded from the required bundled source catalog. */
@Singleton
public final class StrategySourceRegistry
{
    public static final String WIKI_LICENSE = "CC BY-NC-SA 3.0";
    private static final String RESOURCE = "/content/catalogs/strategy-sources.json";
    private final Map<StrategySourceId, StrategySourceDefinition> sources;

    public StrategySourceRegistry()
    {
        EnumMap<StrategySourceId, StrategySourceDefinition> values =
                new EnumMap<>(StrategySourceId.class);
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            if (record.id == null || record.url == null || record.reviewedDate == null)
                throw new IllegalStateException("Incomplete strategy source in " + RESOURCE);
            StrategySourceDefinition source = new StrategySourceDefinition(record.id,
                    record.url, record.subject, LocalDate.parse(record.reviewedDate),
                    record.revision, record.license, record.derivedStrategyFamilies);
            if (values.put(record.id, source) != null)
                throw new IllegalStateException("Duplicate strategy source: " + record.id);
        }
        sources = Collections.unmodifiableMap(values);
    }

    public StrategySourceDefinition get(StrategySourceId id) { return sources.get(id); }
    public Map<StrategySourceId, StrategySourceDefinition> all() { return sources; }

    private static final class Record
    {
        private StrategySourceId id;
        private String url;
        private String subject;
        private String reviewedDate;
        private String revision;
        private String license;
        private List<String> derivedStrategyFamilies;
    }
}
