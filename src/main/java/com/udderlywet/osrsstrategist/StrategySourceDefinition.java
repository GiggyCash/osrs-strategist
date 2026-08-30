package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Provenance for one reviewed strategic source family. */
public final class StrategySourceDefinition
{
    @Getter
    private final StrategySourceId id;
    @Getter
    private final String url;
    @Getter
    private final String subject;
    @Getter
    private final LocalDate reviewedDate;
    @Getter
    private final String revision;
    @Getter
    private final String license;
    @Getter
    private final List<String> derivedStrategyFamilies;

    public StrategySourceDefinition(StrategySourceId id, String url,
            String subject, LocalDate reviewedDate, String revision,
            String license)
    {
        this(id, url, subject, reviewedDate, revision, license,
                Collections.emptyList());
    }

    public StrategySourceDefinition(StrategySourceId id, String url,
            String subject, LocalDate reviewedDate, String revision,
            String license, List<String> derivedStrategyFamilies)
    {
        this.id = id;
        this.url = url;
        this.subject = subject;
        this.reviewedDate = reviewedDate;
        this.revision = revision;
        this.license = license;
        this.derivedStrategyFamilies = Collections.unmodifiableList(
                derivedStrategyFamilies == null ? new ArrayList<>()
                        : new ArrayList<>(derivedStrategyFamilies));
    }

}
